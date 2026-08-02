import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getNotificationBadgeSummary, getNotificationSettings } from '@/api/notification'
import { i18n } from '@/locales'
import { buildMessageDetailPath } from '@/router/guards'
import { setAppIconBadge } from '@/utils/pushNotifications'
import type { MessageThreadDTO } from '@/types/message'
import type { NotificationSettingDTO, NotificationSettingRequest } from '@/types/settings'
import { getStoredCurrentStoreId, getStoredToken } from '@/utils/storage'

const POLL_INTERVAL = 15000
const POPUP_DURATION = 5000

const notificationText = (key: string, params?: Record<string, unknown>) => {
  const path = `runtime.notification.${key}`
  return params ? i18n.global.t(path, params) : i18n.global.t(path)
}

export interface InAppNotificationItem {
  id: string
  title: string
  content: string
  detail: string
  targetPath: string
  type: 'order' | 'message'
}

type NotificationRuntimeSettings = Pick<
  NotificationSettingDTO,
  'orderPopup' | 'orderSound' | 'chatPopup' | 'chatSound'
>

function buildThreadTitle(thread: MessageThreadDTO) {
  if (thread.guestName) {
    return thread.guestName
  }

  if (thread.listingName) {
    return thread.listingName
  }

  if (thread.channelName) {
    return thread.channelName
  }

  return notificationText('guestThread')
}

function buildThreadDetail(thread: MessageThreadDTO) {
  const orderCode = thread.bookingId || thread.threadId || '-'
  return notificationText('orderDetail', {
    channel: thread.channelName,
    code: orderCode,
  })
}

function hasAuthenticatedNotificationContext() {
  return Boolean(getStoredToken() && getStoredCurrentStoreId())
}

export const useNotificationCenterStore = defineStore('notificationCenter', () => {
  const items = ref<InAppNotificationItem[]>([])
  const unreadMessageCount = ref(0)
  const pendingReviewCount = ref(0)
  const messageThreads = ref<MessageThreadDTO[]>([])
  const activeUserId = ref<number | null>(null)
  const started = ref(false)

  const settings = ref<NotificationRuntimeSettings | null>(null)

  const shownKeys = new Set<string>()
  const dismissTimers = new Map<string, number>()

  let orderTimer = 0
  let messageTimer = 0

  const clearDismissTimer = (id: string) => {
    const timer = dismissTimers.get(id)
    if (!timer) {
      return
    }

    window.clearTimeout(timer)
    dismissTimers.delete(id)
  }

  const dismiss = (id: string) => {
    clearDismissTimer(id)
    items.value = items.value.filter((item) => item.id !== id)
  }

  const enqueue = (item: InAppNotificationItem) => {
    let exists = false

    for (const currentItem of items.value) {
      if (currentItem.id === item.id) {
        exists = true
        break
      }
    }

    if (exists) {
      return
    }

    items.value.push(item)

    const timer = window.setTimeout(() => {
      dismiss(item.id)
    }, POPUP_DURATION)
    dismissTimers.set(item.id, timer)
  }

  const resetRuntimeState = () => {
    for (const timer of dismissTimers.values()) {
      window.clearTimeout(timer)
    }

    dismissTimers.clear()
    shownKeys.clear()
    items.value = []
    unreadMessageCount.value = 0
    pendingReviewCount.value = 0
    messageThreads.value = []
  }

  const syncUnreadMessageCount = (threads: MessageThreadDTO[]) => {
    let nextCount = 0

    for (const thread of threads) {
      const unreadCount = Number(thread.unreadCount) || 0
      if (unreadCount > 0) {
        nextCount += unreadCount
      }
    }

    unreadMessageCount.value = nextCount
  }

  const syncMessageThreads = (threads: MessageThreadDTO[]) => {
    // 只缓存会话摘要供其它页面复用；未读角标由 unread-summary 轮询维护，
    // 分页后的部分列表不能用来重算总未读数
    messageThreads.value = [...threads]
  }

  const stop = () => {
    if (orderTimer) {
      window.clearInterval(orderTimer)
      orderTimer = 0
    }

    if (messageTimer) {
      window.clearInterval(messageTimer)
      messageTimer = 0
    }

    activeUserId.value = null
    started.value = false
    settings.value = null
    resetRuntimeState()
    void setAppIconBadge(0)
  }

  const loadSettings = async (userId: number) => {
    try {
      const response = await getNotificationSettings(userId)
      if (!response.success || !response.data) {
        settings.value = null
        return
      }

      settings.value = {
        orderPopup: response.data.orderPopup,
        orderSound: response.data.orderSound,
        chatPopup: response.data.chatPopup,
        chatSound: response.data.chatSound,
      }
    } catch {
      settings.value = null
      return
    }
  }

  const applySettingsSnapshot = (snapshot: NotificationSettingRequest) => {
    settings.value = {
      orderPopup: snapshot.orderPopup,
      orderSound: snapshot.orderSound,
      chatPopup: snapshot.chatPopup,
      chatSound: snapshot.chatSound,
    }
  }

  const pollOrderNotifications = async () => {
    return
  }

  const pollMessageNotifications = async () => {
    if (!hasAuthenticatedNotificationContext()) {
      stop()
      return
    }

    try {
      // 轮询角标汇总：一次请求同时拿未读聊天数、待审查数和桌面图标角标总数
      const nextResponse = await getNotificationBadgeSummary()
      if (!nextResponse.success || !nextResponse.data) {
        return
      }

      unreadMessageCount.value = Number(nextResponse.data.unreadMessages) || 0
      pendingReviewCount.value = Number(nextResponse.data.pendingReviews) || 0
      void setAppIconBadge(Number(nextResponse.data.total) || 0)
      return
    } catch {
      return
    }
  }

  const start = async (userId: number) => {
    if (typeof window === 'undefined') {
      return
    }

    if (!hasAuthenticatedNotificationContext()) {
      stop()
      return
    }

    if (started.value && activeUserId.value === userId) {
      return
    }

    stop()
    activeUserId.value = userId
    started.value = true

    try {
      await loadSettings(userId)
      await pollOrderNotifications()
      await pollMessageNotifications()
    } catch {
      // first-screen popup polling failure should not block app usage
    }

    orderTimer = window.setInterval(() => {
      void pollOrderNotifications()
    }, POLL_INTERVAL)

    messageTimer = window.setInterval(() => {
      void pollMessageNotifications()
    }, POLL_INTERVAL)
  }

  return {
    items,
    unreadMessageCount,
    pendingReviewCount,
    messageThreads,
    started,
    enqueue,
    dismiss,
    start,
    stop,
    applySettingsSnapshot,
    syncMessageThreads,
    syncUnreadMessageCount,
  }
})
