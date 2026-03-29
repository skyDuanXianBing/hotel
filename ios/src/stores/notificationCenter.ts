import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getMessageThreads } from '@/api/message'
import { getNotificationMessagesByType, getNotificationSettings } from '@/api/notification'
import { buildMessageDetailPath, ROUTE_PATHS } from '@/router/guards'
import type { MessageThreadDTO } from '@/types/message'
import type { NotificationSettingDTO, NotificationSettingRequest } from '@/types/settings'

const ORDER_NOTIFICATION_TYPE = 'ORDER'
const POLL_INTERVAL = 15000
const POPUP_DURATION = 5000

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

  return '住客会话'
}

function buildThreadDetail(thread: MessageThreadDTO) {
  const orderCode = thread.bookingId || thread.threadId || '-'
  return `${thread.channelName} · 订单 ${orderCode}`
}

export const useNotificationCenterStore = defineStore('notificationCenter', () => {
  const items = ref<InAppNotificationItem[]>([])
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
    try {
      if (!activeUserId.value) {
        return
      }

      const popupEnabled = settings.value ? settings.value.orderPopup : true
      if (!popupEnabled) {
        return
      }

      const response = await getNotificationMessagesByType(
        activeUserId.value,
        ORDER_NOTIFICATION_TYPE,
        0,
        10,
        false,
      )

      if (!response.success || !response.data) {
        return
      }

      const content = response.data.content || []

      for (const item of content) {
        const uniqueKey = `order:${item.id}`
        if (shownKeys.has(uniqueKey)) {
          continue
        }

        shownKeys.add(uniqueKey)
        enqueue({
          id: uniqueKey,
          title: item.title || '订单提醒',
          content: item.content || '你有一条新的订单提醒',
          detail: '订单通知',
          targetPath: ROUTE_PATHS.orderNotifications,
          type: 'order',
        })
      }
    } catch {
      return
    }
  }

  const pollMessageNotifications = async () => {
    try {
      const popupEnabled = settings.value ? settings.value.chatPopup : true
      if (!popupEnabled) {
        return
      }

      const response = await getMessageThreads()
      if (!response.success || !response.data) {
        return
      }

      const threads = response.data || []

      for (const thread of threads) {
        if (thread.unreadCount <= 0) {
          continue
        }

        const uniqueKey = `message:${thread.id}:${thread.lastActivity}:${thread.unreadCount}`
        if (shownKeys.has(uniqueKey)) {
          continue
        }

        shownKeys.add(uniqueKey)
        enqueue({
          id: uniqueKey,
          title: buildThreadTitle(thread),
          content: thread.lastMessage || '你有一条新的住客消息',
          detail: buildThreadDetail(thread),
          targetPath: buildMessageDetailPath(thread.id),
          type: 'message',
        })
      }
    } catch {
      return
    }
  }

  const start = async (userId: number) => {
    if (typeof window === 'undefined') {
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
    started,
    enqueue,
    dismiss,
    start,
    stop,
    applySettingsSnapshot,
  }
})
