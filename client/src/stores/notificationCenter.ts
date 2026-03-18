import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getNotificationMessagesByType,
  getNotificationSettings,
  type NotificationMessageDTO,
} from '@/api/notification'
import { createSuMessagingSocket, type SuMessagingRealtimeEvent } from '@/utils/suMessagingSocket'

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
  if (!value) {
    return new Date()
  }
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? new Date() : parsed
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

export const useNotificationCenterStore = defineStore('notificationCenter', () => {
  const popupController = ref<PopupController | null>(null)
  const settings = ref<NotificationSettingsSnapshot>({ ...DEFAULT_SETTINGS })
  const running = ref(false)

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
      if (title && title.includes('取消')) {
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
        console.warn('播放提示音失败:', error)
        return
      }
    }

    try {
      const fallbackAudio = new Audio(fallbackSrc)
      fallbackAudio.preload = 'auto'
      await fallbackAudio.play()
    } catch (error) {
      console.warn('播放提示音失败:', error)
    }
  }

  const emitOrderNotification = (notification: NotificationMessageDTO) => {
    if (settings.value.orderPopup) {
      showPopup({
        id: `order-${notification.id}`,
        type: 'order',
        title: notification.title || '新订单提醒',
        content: normalizeMessageText(notification.content),
        channel: '订单通知',
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
    const content = normalizeMessageText(event.message?.content)
    if (settings.value.chatPopup) {
      showPopup({
        id: `chat-${event.threadId}-${event.message.id}`,
        type: 'message',
        title: '新聊天消息',
        content: content || '您收到一条新的聊天消息',
        channel: '聊天消息',
        guestName: event.message.senderName || '客人',
        orderNumber: event.threadId ? String(event.threadId) : '-',
        time: toDate(event.message.timestamp),
        onClick: onChatClick,
      })
    }

    if (settings.value.chatSound) {
      void playAudio('message')
    }
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
      console.warn('加载通知设置失败，使用默认配置:', error)
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
      console.warn('轮询订单通知失败:', error)
    }
  }

  const clearOrderPolling = () => {
    if (orderPollTimer) {
      window.clearInterval(orderPollTimer)
      orderPollTimer = null
    }
  }

  const handleChatRealtimeEvent = (event: SuMessagingRealtimeEvent) => {
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
        console.warn('解析聊天实时事件失败:', error)
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
    await pollOrderNotifications()
    orderPollTimer = window.setInterval(() => {
      void pollOrderNotifications()
    }, ORDER_POLL_INTERVAL)
    connectChatSocket()
  }

  return {
    settings,
    running,
    start,
    stop,
    loadSettings,
    bindPopupController,
    applySettingsSnapshot,
  }
})
