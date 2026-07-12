import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getNotificationMessagesByType,
  getNotificationSettings,
  getSystemUnreadNotificationCount,
  getUnreadNotificationCountByType,
  type NotificationMessageDTO,
} from '@/api/notification'
import { getSuUnreadSummary, type SuMessagingThreadDTO } from '@/api/suMessaging'
import { parseUtcDateTime } from '@/utils/storeDateTime'
import { createSuMessagingSocket, type SuMessagingRealtimeEvent } from '@/utils/suMessagingSocket'
import { i18n } from '@/locales'

type PopupNotificationType = 'order' | 'message'

export interface PopupNotification {
  id: string
  title: string
  time: Date
  type: PopupNotificationType
  content?: string
  channel?: string
  guestName?: string
  orderNumber?: string
  onClick?: () => void
}

interface PopupController {
  addNotification: (notification: PopupNotification) => void
}

interface NotificationSettingsSnapshot {
  orderPopup: boolean
  orderSound: boolean
  chatPopup: boolean
  chatSound: boolean
}

interface NotificationCenterStartOptions {
  userId?: number
  storeId?: number
  onOrderClick?: () => void
  onChatClick?: () => void
}

const ORDER_NOTIFICATION_TYPE = 'ORDER'
const ORDER_POLL_INTERVAL = 10_000
const CHAT_RECONNECT_INTERVAL = 3_000
const translate = (key: string) => i18n.global.t(key)
const DEFAULT_SETTINGS: NotificationSettingsSnapshot = {
  orderPopup: true,
  orderSound: true,
  chatPopup: true,
  chatSound: true,
}

const tryParseUserIdFromStorage = () => {
  const rawUser = localStorage.getItem('user')
  if (!rawUser) {
    return null
  }
  try {
    const user = JSON.parse(rawUser) as { id?: number }
    return typeof user.id === 'number' ? user.id : null
  } catch {
    return null
  }
}

const tryParseStoreIdFromStorage = () => {
  const rawStore = localStorage.getItem('currentStore')
  if (!rawStore) {
    return null
  }
  try {
    const store = JSON.parse(rawStore) as { id?: number }
    return typeof store.id === 'number' ? store.id : null
  } catch {
    return null
  }
}

const getCurrentToken = () => localStorage.getItem('token') || ''

const toDate = (value?: string) => {
  return parseUtcDateTime(value)
}

const normalizeMessageText = (value?: string, maxLength = 120) => {
  if (!value) {
    return ''
  }
  const trimmed = value.trim()
  if (!trimmed) {
    return ''
  }
  if (trimmed.length <= maxLength) {
    return trimmed
  }
  return `${trimmed.slice(0, maxLength)}...`
}

const normalizeUnreadCount = (value: unknown) => {
  const count = Number(value ?? 0)
  if (!Number.isFinite(count) || count <= 0) {
    return 0
  }
  return count
}

export const useNotificationCenterStore = defineStore('notificationCenter', () => {
  const popupController = ref<PopupController | null>(null)
  const settings = ref<NotificationSettingsSnapshot>({ ...DEFAULT_SETTINGS })
  const running = ref(false)
  const chatUnreadCount = ref(0)
  const systemUnreadCount = ref(0)
  const orderUnreadCount = ref(0)
  const inboxUnreadCount = computed(() => systemUnreadCount.value + orderUnreadCount.value)

  let activeUserId: number | null = null
  let activeStoreId: number | null = null
  let onOrderClick: (() => void) | undefined
  let onChatClick: (() => void) | undefined
  let seededOrderIds = new Set<number>()
  let seenChatMessageKeys = new Set<string>()
  let orderPollTimer: number | null = null
  let chatSocket: WebSocket | null = null
  let chatReconnectTimer: number | null = null
  let hasSeededOrderState = false

  const bindPopupController = (controller: PopupController | null) => {
    popupController.value = controller
  }

  const showPopup = (notification: PopupNotification) => {
    if (!popupController.value) {
      return
    }
    popupController.value.addNotification(notification)
  }

  const resolveAudioSrc = (type: PopupNotificationType, title?: string) => {
    if (type === 'order') {
      if (title && /cancel|\u53d6\u6d88|\u30ad\u30e3\u30f3\u30bb\u30eb/i.test(title)) {
        return '/sounds/cancel.mp3'
      }
      return '/sounds/order-notification.mp3'
    }
    return '/sounds/chat-notification.mp3'
  }

  const playAudio = async (type: PopupNotificationType, title?: string) => {
    const primarySrc = resolveAudioSrc(type, title)
    const fallbackSrc = '/sounds/order-notification.mp3'
    const fallbackEnabled = type === 'message'

    try {
      const audio = new Audio(primarySrc)
      audio.preload = 'auto'
      await audio.play()
      return
    } catch (error) {
      if (!fallbackEnabled) {
        console.warn('Failed to play notification sound:', error)
        return
      }
    }

    try {
      const fallbackAudio = new Audio(fallbackSrc)
      fallbackAudio.preload = 'auto'
      await fallbackAudio.play()
    } catch (error) {
      console.warn('Failed to play notification sound:', error)
    }
  }

  const emitOrderNotification = (notification: NotificationMessageDTO) => {
    if (settings.value.orderPopup) {
      showPopup({
        id: `order-${notification.id}`,
        type: 'order',
        title: notification.title || translate('stage6.common.messages.newOrderNotification'),
        content: normalizeMessageText(notification.content),
        channel: translate('stage6.common.messages.orderNotificationChannel'),
        guestName: '-',
        orderNumber: notification.relatedId ? String(notification.relatedId) : '-',
        time: toDate(notification.createdAt),
        onClick: onOrderClick,
      })
    }

    if (settings.value.orderSound) {
      void playAudio('order', notification.title)
    }
  }

  const emitChatNotification = (event: SuMessagingRealtimeEvent) => {
    const message = event.message
    if (!message) return
    const content = normalizeMessageText(message.content)
    if (settings.value.chatPopup) {
      showPopup({
        id: `chat-${event.threadId}-${message.id}`,
        type: 'message',
        title: translate('stage6.common.messages.newChatMessage'),
        content: content || translate('stage6.common.messages.newChatMessageContent'),
        channel: translate('stage6.common.messages.chatMessageChannel'),
        guestName: message.senderName || translate('stage6.common.messages.guestFallback'),
        orderNumber: event.threadId ? String(event.threadId) : '-',
        time: toDate(message.timestamp),
        onClick: onChatClick,
      })
    }

    if (settings.value.chatSound) {
      void playAudio('message')
    }
  }

  const updateChatUnreadCountFromThreads = (threads: SuMessagingThreadDTO[]) => {
    chatUnreadCount.value = threads.reduce((total, thread) => {
      const count = Number(thread.unreadCount ?? 0)
      return total + (Number.isFinite(count) && count > 0 ? count : 0)
    }, 0)
  }

  const refreshChatUnreadCount = async () => {
    if (!activeStoreId || !getCurrentToken()) {
      chatUnreadCount.value = 0
      return
    }

    try {
      const response = (await getSuUnreadSummary()) as {
        success?: boolean
        data?: { totalUnread?: number }
      }
      if (response.success === false) {
        return
      }
      chatUnreadCount.value = normalizeUnreadCount(response.data?.totalUnread)
    } catch (error) {
      console.warn('Failed to refresh chat unread count:', error)
    }
  }

  const refreshSystemUnreadCount = async () => {
    if (!activeUserId) {
      systemUnreadCount.value = 0
      return
    }

    try {
      const response = await getSystemUnreadNotificationCount(activeUserId)
      if (response.success) {
        systemUnreadCount.value = normalizeUnreadCount(response.data)
      }
    } catch (error) {
      console.warn('Failed to refresh system unread count:', error)
    }
  }

  const refreshOrderUnreadCount = async () => {
    if (!activeUserId) {
      orderUnreadCount.value = 0
      return
    }

    try {
      const response = await getUnreadNotificationCountByType(activeUserId, ORDER_NOTIFICATION_TYPE)
      if (response.success) {
        orderUnreadCount.value = normalizeUnreadCount(response.data)
      }
    } catch (error) {
      console.warn('Failed to refresh order unread count:', error)
    }
  }

  const refreshInboxUnreadCounts = async () => {
    await Promise.all([refreshSystemUnreadCount(), refreshOrderUnreadCount()])
  }

  const loadSettings = async () => {
    if (!activeUserId) {
      settings.value = { ...DEFAULT_SETTINGS }
      return
    }
    try {
      const response = await getNotificationSettings(activeUserId)
      if (response.success && response.data) {
        settings.value = {
          orderPopup: response.data.orderPopup,
          orderSound: response.data.orderSound,
          chatPopup: response.data.chatPopup,
          chatSound: response.data.chatSound,
        }
      } else {
        settings.value = { ...DEFAULT_SETTINGS }
      }
    } catch (error) {
      console.warn('Failed to load notification settings; using defaults:', error)
      settings.value = { ...DEFAULT_SETTINGS }
    }
  }

  const pollOrderNotifications = async () => {
    if (!running.value || !activeUserId) {
      return
    }

    try {
      const response = await getNotificationMessagesByType(activeUserId, ORDER_NOTIFICATION_TYPE, 0, 20, false)
      if (!response.success || !response.data?.content) {
        return
      }

      orderUnreadCount.value = normalizeUnreadCount(response.data.totalElements)
      const fetchedList = response.data.content
      if (!hasSeededOrderState) {
        fetchedList.forEach((item) => seededOrderIds.add(item.id))
        hasSeededOrderState = true
        return
      }

      const newItems = fetchedList.filter((item) => !seededOrderIds.has(item.id))
      if (!newItems.length) {
        return
      }

      newItems.forEach((item) => seededOrderIds.add(item.id))
      newItems.slice().reverse().forEach((item) => emitOrderNotification(item))
    } catch (error) {
      console.warn('Failed to poll order notifications:', error)
    }
  }

  const clearOrderPolling = () => {
    if (orderPollTimer) {
      window.clearInterval(orderPollTimer)
      orderPollTimer = null
    }
  }

  const handleChatRealtimeEvent = (event: SuMessagingRealtimeEvent) => {
    if (event.eventType === 'WORKBENCH_INVALIDATED') {
      window.dispatchEvent(
        new CustomEvent('workbench-invalidated', { detail: { resourceType: event.resourceType } }),
      )
      return
    }
    if (event.eventType !== 'MESSAGE_CREATED' && event.eventType !== 'MESSAGE_UPDATED') {
      return
    }
    if (!event.message || event.message.senderType !== 'GUEST') {
      return
    }

    const dedupeKey = `${event.threadId}:${event.message.id}`
    if (seenChatMessageKeys.has(dedupeKey)) {
      return
    }
    seenChatMessageKeys.add(dedupeKey)
    emitChatNotification(event)
    void refreshChatUnreadCount()
  }

  const clearChatReconnect = () => {
    if (chatReconnectTimer) {
      window.clearTimeout(chatReconnectTimer)
      chatReconnectTimer = null
    }
  }

  const closeChatSocket = () => {
    clearChatReconnect()
    if (!chatSocket) {
      return
    }
    chatSocket.onopen = null
    chatSocket.onmessage = null
    chatSocket.onclose = null
    chatSocket.onerror = null
    chatSocket.close()
    chatSocket = null
  }

  const connectChatSocket = () => {
    closeChatSocket()

    const token = getCurrentToken()
    if (!token || !activeStoreId) {
      return
    }

    chatSocket = createSuMessagingSocket(token, activeStoreId)
    chatSocket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as SuMessagingRealtimeEvent
        handleChatRealtimeEvent(payload)
      } catch (error) {
        console.warn('Failed to parse chat realtime event:', error)
      }
    }
    chatSocket.onclose = () => {
      if (!running.value) {
        return
      }
      clearChatReconnect()
      chatReconnectTimer = window.setTimeout(() => {
        connectChatSocket()
      }, CHAT_RECONNECT_INTERVAL)
    }
  }

  const applySettingsSnapshot = (snapshot: NotificationSettingsSnapshot) => {
    settings.value = { ...snapshot }
  }

  const stop = () => {
    running.value = false
    clearOrderPolling()
    closeChatSocket()
    seededOrderIds = new Set<number>()
    seenChatMessageKeys = new Set<string>()
    hasSeededOrderState = false
    chatUnreadCount.value = 0
    systemUnreadCount.value = 0
    orderUnreadCount.value = 0
    activeUserId = null
    activeStoreId = null
    onOrderClick = undefined
    onChatClick = undefined
  }

  const start = async (options: NotificationCenterStartOptions = {}) => {
    stop()

    const userId = options.userId ?? tryParseUserIdFromStorage()
    if (!userId) {
      return
    }

    running.value = true
    activeUserId = userId
    activeStoreId = options.storeId ?? tryParseStoreIdFromStorage()
    onOrderClick = options.onOrderClick
    onChatClick = options.onChatClick

    await loadSettings()
    await refreshInboxUnreadCounts()
    await refreshChatUnreadCount()
    await pollOrderNotifications()
    orderPollTimer = window.setInterval(() => {
      void pollOrderNotifications()
    }, ORDER_POLL_INTERVAL)
    connectChatSocket()
  }

  return {
    settings,
    running,
    chatUnreadCount,
    systemUnreadCount,
    orderUnreadCount,
    inboxUnreadCount,
    start,
    stop,
    loadSettings,
    bindPopupController,
    applySettingsSnapshot,
    refreshChatUnreadCount,
    refreshSystemUnreadCount,
    refreshOrderUnreadCount,
    refreshInboxUnreadCounts,
    updateChatUnreadCountFromThreads,
  }
})
