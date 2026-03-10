<template>
  <div class="messages-page">
    <div class="conversations-panel">
      <div class="panel-header">
        <h2>收件箱</h2>
        <el-button text size="small" @click="refreshThreads">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>

      <div class="search-bar">
        <el-input v-model="searchQuery" placeholder="搜索会话" :prefix-icon="Search" clearable />
      </div>

      <div class="conversations-list">
        <div
          v-for="conversation in filteredConversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{ active: activeThreadId === conversation.id }"
          @click="selectConversation(conversation.id)"
        >
          <div class="conversation-avatar">
            <el-icon><User /></el-icon>
          </div>
          <div class="conversation-info">
            <div class="conversation-header">
              <span class="channel-name">{{
                conversation.guestName || conversation.listingName || conversation.channelName
              }}</span>
              <span class="message-time">{{ formatConversationTime(conversation.lastActivity) }}</span>
            </div>
            <div class="last-message">
              {{ conversation.channelName }} | 订单号 {{ conversation.bookingId || conversation.threadId || '-' }}
            </div>
            <div class="conversation-status">
              <el-tag :type="getStatusType(conversation.closed)" size="small">
                {{ getStatusText(conversation.closed) }}
              </el-tag>
            </div>
          </div>
        </div>

        <div v-if="conversations.length === 0" class="empty-state">
          <el-icon :size="48" color="#ddd"><ChatDotRound /></el-icon>
          <p>暂无会话</p>
        </div>
      </div>
    </div>

    <div class="messages-panel">
      <template v-if="activeConversation">
        <div class="messages-header">
          <div class="channel-info">
            <div class="channel-avatar">
              <el-icon><User /></el-icon>
            </div>
            <div class="channel-details">
              <div class="channel-name">{{
                activeConversation.guestName || activeConversation.listingName || activeConversation.channelName
              }}</div>
              <div class="channel-status-text">
                渠道: {{ activeConversation.channelName }} |
                订单号 {{ activeConversation.bookingId || activeConversation.threadId || '-' }} |
                {{ getStatusText(activeConversation.closed) }}
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button
              size="small"
              :loading="isResolvingReservation"
              :disabled="!activeConversation"
              @click="openOrderDetail"
            >
              订单明细
            </el-button>
          </div>
        </div>

        <div ref="messagesListRef" class="messages-list">
          <template v-for="group in groupedMessages" :key="group.key">
            <div class="message-date-divider">{{ group.label }}</div>
            <div
              v-for="message in group.items"
              :key="message.id"
              class="message-item"
              :class="{
                'message-sent': message.senderType === SuMessagingSenderType.STAFF,
                'message-received': message.senderType === SuMessagingSenderType.GUEST,
              }"
            >
              <span
                v-if="message.senderType === SuMessagingSenderType.STAFF && message.deliveryStatus === 'SENDING'"
                class="message-delivery-indicator sending"
                aria-label="发送中"
              >
                <el-icon class="spin"><Loading /></el-icon>
              </span>
              <span
                v-else-if="message.senderType === SuMessagingSenderType.STAFF && message.deliveryStatus === 'FAILED'"
                class="message-delivery-indicator failed"
                title="发送失败"
              >
                <el-icon><WarningFilled /></el-icon>
              </span>

              <div class="message-content">
                <div class="message-text">
                  <template
                    v-for="(segment, index) in splitMessageContent(message.content)"
                    :key="`${message.id}-${index}`"
                  >
                    <a
                      v-if="segment.type === 'link'"
                      :href="segment.value"
                      class="message-link"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {{ segment.label }}
                    </a>
                    <span v-else>{{ segment.value }}</span>
                  </template>
                  <span
                    v-if="message.senderName && message.senderType === SuMessagingSenderType.STAFF"
                    class="sender-badge"
                  >
                    - {{ message.senderName }}
                  </span>
                </div>
                <div class="message-time">{{ formatMessageTime(message.timestamp) }}</div>
              </div>
            </div>
          </template>
        </div>

        <div class="message-input-area">
          <el-input
            v-model="newMessage"
            type="textarea"
            :rows="3"
            placeholder="输入消息..."
            :disabled="isSending || activeConversation.closed"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="input-actions">
            <div class="ai-reply-switch">
              <span class="ai-reply-label">AI 自动回复</span>
              <el-switch
                v-model="aiAutoReplyEnabled"
                :disabled="activeConversation.closed || isAiSettingSaving"
                @change="onAiAutoReplyChange"
              />
            </div>
            <el-button
              type="primary"
              @click="sendMessage"
              :disabled="!newMessage.trim() || isSending || activeConversation.closed"
              :loading="isSending"
            >
              发送
            </el-button>
          </div>
        </div>
      </template>

      <div v-else class="empty-state">
        <el-icon :size="80" color="#ccc"><ChatDotRound /></el-icon>
        <p>选择一个会话开始聊天</p>
      </div>
    </div>

    <ReservationDetailDrawer
      v-model="showOrderDetailDrawer"
      :reservation-id="selectedReservationId"
      :active-order-tab="'all'"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ChatDotRound, Loading, Refresh, Search, User, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getSuMessagingAiSetting,
  getSuThreadMessages,
  getSuThreads,
  sendSuThreadMessage,
  updateSuMessagingAiSetting,
  SuMessagingSenderType,
  type SuMessagingAiSetting,
  type SuMessagingMessageDTO,
  type SuMessagingThreadDTO,
} from '@/api/suMessaging'
import { getReservationsWithFilters, type ReservationDTO } from '@/api/reservation'
import { createSuMessagingSocket, type SuMessagingRealtimeEvent } from '@/utils/suMessagingSocket'
import ReservationDetailDrawer from '@/components/reservation/ReservationDetailDrawer.vue'

interface MessageItem {
  id: number
  senderType: SuMessagingSenderType
  content: string
  timestamp: Date
  senderName?: string
  deliveryStatus?: 'SENDING' | 'SENT' | 'FAILED'
}

interface RealtimeMessageItem {
  id: number
  threadId: number
  senderType: 'GUEST' | 'STAFF'
  content: string
  timestamp: string
  senderName?: string
  deliveryStatus?: 'SENDING' | 'SENT' | 'FAILED'
}

interface GroupedMessages {
  key: string
  label: string
  items: MessageItem[]
}

interface MessageSegment {
  type: 'text' | 'link'
  value: string
  label: string
}

interface ApiResponse<T> {
  success?: boolean
  message?: string
  data?: T
}

const searchQuery = ref('')
const activeThreadId = ref<number | null>(null)
const newMessage = ref('')
const messagesListRef = ref<HTMLElement | null>(null)
const conversations = ref<SuMessagingThreadDTO[]>([])
const messages = ref<MessageItem[]>([])
const isSending = ref(false)
const aiAutoReplyEnabled = ref(true)
const isAiSettingSaving = ref(false)
const showOrderDetailDrawer = ref(false)
const selectedReservationId = ref<number | null>(null)
const isResolvingReservation = ref(false)

let socket: WebSocket | null = null
let reconnectTimer: number | null = null
let isDestroyed = false
let localMessageSeed = -1
const reservationIdCache = new Map<string, number | null>()

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const filteredConversations = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) {
    return conversations.value
  }

  return conversations.value.filter((conversation) => {
    return (
      (conversation.guestName || '').toLowerCase().includes(query) ||
      (conversation.listingName || '').toLowerCase().includes(query) ||
      (conversation.bookingId || '').toLowerCase().includes(query) ||
      (conversation.threadId || '').toLowerCase().includes(query)
    )
  })
})

const activeConversation = computed(() => {
  if (!activeThreadId.value) {
    return null
  }
  return conversations.value.find((conversation) => conversation.id === activeThreadId.value) || null
})

const groupedMessages = computed<GroupedMessages[]>(() => {
  const groups = new Map<string, MessageItem[]>()

  for (const message of sortMessagesByTime(messages.value)) {
    const key = getLocalDateKey(message.timestamp)
    const existing = groups.get(key)
    if (existing) {
      existing.push(message)
    } else {
      groups.set(key, [message])
    }
  }

  return Array.from(groups.entries()).map(([key, items]) => ({
    key,
    label: formatDateDividerLabel(key),
    items,
  }))
})

const getStatusType = (closed: boolean) => (closed ? 'info' : 'success')
const getStatusText = (closed: boolean) => (closed ? '已关闭' : '活跃')

const normalizeSenderName = (senderName?: string) => {
  if (!senderName) {
    return senderName
  }
  const trimmed = senderName.trim()
  if (!trimmed) {
    return trimmed
  }

  if (/^AI/i.test(trimmed) && /[\uFFFD\u00C0-\u00FF]/.test(trimmed)) {
    return 'AI助手'
  }

  if (/^AI/i.test(trimmed) && /[åæ]/i.test(trimmed)) {
    return 'AI助手'
  }

  return trimmed
}

const mapMessage = (message: SuMessagingMessageDTO): MessageItem => ({
  id: message.id,
  senderType: message.senderType,
  content: message.content,
  timestamp: new Date(message.timestamp),
  senderName: normalizeSenderName(message.senderName),
  deliveryStatus: message.deliveryStatus,
})

const mapRealtimeMessage = (message: RealtimeMessageItem): MessageItem => ({
  id: message.id,
  senderType: message.senderType as SuMessagingSenderType,
  content: message.content,
  timestamp: new Date(message.timestamp),
  senderName: normalizeSenderName(message.senderName),
  deliveryStatus: message.deliveryStatus,
})

const URL_REGEX = /https?:\/\/[^\s]+/gi
const MAX_LINK_LABEL_LENGTH = 88

const formatLinkLabel = (url: string) => {
  if (url.length <= MAX_LINK_LABEL_LENGTH) {
    return url
  }
  return `${url.slice(0, MAX_LINK_LABEL_LENGTH)}...`
}

const normalizeInlineSpaces = (text: string) => text.replace(/[ \t\u00A0\u3000]{2,}/g, ' ')

const normalizeMessageContent = (content: string) =>
  content
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/[\u00A0\u3000]/g, ' ')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')

const splitMessageContent = (content: string): MessageSegment[] => {
  if (!content) {
    return []
  }

  const normalized = normalizeMessageContent(content)
  const segments: MessageSegment[] = []
  let cursor = 0
  let match = URL_REGEX.exec(normalized)

  while (match) {
    const fullUrl = match[0]
    const start = match.index

    if (start > cursor) {
      const text = normalized.slice(cursor, start)
      if (text) {
        segments.push({
          type: 'text',
          value: normalizeInlineSpaces(text),
          label: normalizeInlineSpaces(text),
        })
      }
    }

    segments.push({
      type: 'link',
      value: fullUrl,
      label: formatLinkLabel(fullUrl),
    })
    cursor = start + fullUrl.length
    match = URL_REGEX.exec(normalized)
  }

  if (cursor < normalized.length) {
    const text = normalized.slice(cursor)
    if (text) {
      segments.push({
        type: 'text',
        value: normalizeInlineSpaces(text),
        label: normalizeInlineSpaces(text),
      })
    }
  }

  return segments
}

const getCurrentToken = () => localStorage.getItem('token') || ''

const getCurrentStoreId = () => {
  const raw = localStorage.getItem('currentStore')
  if (!raw) {
    return null
  }

  try {
    const store = JSON.parse(raw)
    const id = typeof store?.id === 'number' ? store.id : Number(store?.id)
    return Number.isFinite(id) ? id : null
  } catch (error) {
    console.error('解析 currentStore 失败:', error)
    return null
  }
}

const getLocalDateKey = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatDateDividerLabel = (dateKey: string) => {
  const parsed = new Date(`${dateKey}T00:00:00`)
  if (Number.isNaN(parsed.getTime())) {
    return dateKey
  }

  const now = new Date()
  const yearPrefix = parsed.getFullYear() === now.getFullYear() ? '' : `${parsed.getFullYear()}年`
  return `${yearPrefix}${parsed.getMonth() + 1}月${parsed.getDate()}日 ${WEEKDAYS[parsed.getDay()]}`
}

const sortMessagesByTime = (list: MessageItem[]) =>
  [...list].sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime())

const scrollToBottom = async () => {
  await nextTick()
  if (messagesListRef.value) {
    messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight
  }
}

const ensureActiveConversation = async () => {
  if (conversations.value.length === 0) {
    activeThreadId.value = null
    messages.value = []
    return
  }

  const exists = activeThreadId.value
    ? conversations.value.some((conversation) => conversation.id === activeThreadId.value)
    : false

  if (!exists) {
    activeThreadId.value = conversations.value[0].id
    await loadThreadMessages(conversations.value[0].id)
  }
}

const refreshThreads = async () => {
  try {
    const response = (await getSuThreads()) as ApiResponse<SuMessagingThreadDTO[]>
    if (response.success === false) {
      throw new Error(response.message || '刷新失败')
    }

    conversations.value = response.data || []
    await ensureActiveConversation()
  } catch (error) {
    console.error('刷新会话列表失败:', error)
    ElMessage.error('刷新失败')
  }
}

const loadThreadMessages = async (threadId: number) => {
  try {
    const response = (await getSuThreadMessages(threadId)) as ApiResponse<SuMessagingMessageDTO[]>
    if (response.success === false) {
      throw new Error(response.message || '加载消息失败')
    }

    const incoming = (response.data || []).map(mapMessage)
    messages.value = sortMessagesByTime(incoming)
    await scrollToBottom()
  } catch (error) {
    console.error('加载会话消息失败:', error)
    ElMessage.error('加载消息失败')
  }
}

const selectConversation = async (threadId: number) => {
  activeThreadId.value = threadId
  await loadThreadMessages(threadId)
  await refreshThreads()
}

const loadAiAutoReplySetting = async () => {
  try {
    const response = (await getSuMessagingAiSetting()) as ApiResponse<SuMessagingAiSetting>
    if (response.success === false) {
      throw new Error(response.message || '获取 AI 自动回复设置失败')
    }

    aiAutoReplyEnabled.value = Boolean(response.data?.autoReplyEnabled)
  } catch (error) {
    console.error('获取 AI 自动回复设置失败:', error)
    ElMessage.error('获取 AI 自动回复设置失败')
  }
}

const onAiAutoReplyChange = async (enabled: boolean) => {
  const previousValue = !enabled
  isAiSettingSaving.value = true

  try {
    const request: SuMessagingAiSetting = {
      autoReplyEnabled: enabled,
    }
    const response = (await updateSuMessagingAiSetting(request)) as ApiResponse<SuMessagingAiSetting>
    if (response.success === false) {
      throw new Error(response.message || '更新失败')
    }

    aiAutoReplyEnabled.value = Boolean(response.data?.autoReplyEnabled)
    ElMessage.success(enabled ? 'AI 自动回复已开启' : 'AI 自动回复已关闭')
  } catch (error) {
    aiAutoReplyEnabled.value = previousValue
    console.error('更新 AI 自动回复设置失败:', error)
    ElMessage.error('更新 AI 自动回复设置失败')
  } finally {
    isAiSettingSaving.value = false
  }
}

const createOptimisticMessage = (content: string): MessageItem => ({
  id: localMessageSeed--,
  senderType: SuMessagingSenderType.STAFF,
  senderName: '客服',
  content,
  timestamp: new Date(),
  deliveryStatus: 'SENDING',
})

const replaceMessageById = (id: number, incoming: MessageItem) => {
  const index = messages.value.findIndex((message) => message.id === id)
  if (index < 0) {
    messages.value.push(incoming)
  } else {
    messages.value[index] = incoming
  }
  messages.value = sortMessagesByTime(messages.value)
}

const sendMessage = async () => {
  if (!activeThreadId.value || !newMessage.value.trim() || isSending.value) {
    return
  }

  const content = newMessage.value.trim()
  isSending.value = true

  const optimistic = createOptimisticMessage(content)
  messages.value.push(optimistic)
  messages.value = sortMessagesByTime(messages.value)
  newMessage.value = ''
  await scrollToBottom()

  try {
    const response = (await sendSuThreadMessage(activeThreadId.value, {
      content,
      senderName: '客服',
    })) as ApiResponse<SuMessagingMessageDTO>

    if (response.success === false || !response.data) {
      throw new Error(response.message || '消息发送失败')
    }

    replaceMessageById(optimistic.id, mapMessage(response.data))
    await scrollToBottom()
    await refreshThreads()
  } catch (error) {
    const index = messages.value.findIndex((message) => message.id === optimistic.id)
    if (index >= 0) {
      messages.value[index] = {
        ...messages.value[index],
        deliveryStatus: 'FAILED',
      }
    }
    console.error('发送消息失败:', error)
    ElMessage.error('消息发送失败')
  } finally {
    isSending.value = false
  }
}

const upsertMessage = (incoming: MessageItem) => {
  const index = messages.value.findIndex((item) => item.id === incoming.id)
  if (index >= 0) {
    messages.value[index] = { ...messages.value[index], ...incoming }
  } else {
    messages.value.push(incoming)
  }
  messages.value = sortMessagesByTime(messages.value)
}

const handleRealtimeEvent = async (event: SuMessagingRealtimeEvent) => {
  if (event.eventType !== 'MESSAGE_CREATED' && event.eventType !== 'MESSAGE_UPDATED') {
    return
  }

  const { threadId, message } = event
  if (!message) {
    return
  }

  if (threadId === activeThreadId.value) {
    upsertMessage(mapRealtimeMessage(message))
    await scrollToBottom()
  }

  await refreshThreads()
}

const clearReconnectTimer = () => {
  if (reconnectTimer) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

const scheduleReconnect = () => {
  if (isDestroyed) {
    return
  }
  clearReconnectTimer()
  reconnectTimer = window.setTimeout(() => {
    connectRealtimeSocket()
  }, 3000)
}

const closeRealtimeSocket = () => {
  clearReconnectTimer()
  if (socket) {
    socket.onopen = null
    socket.onmessage = null
    socket.onclose = null
    socket.onerror = null
    socket.close()
    socket = null
  }
}

const connectRealtimeSocket = () => {
  closeRealtimeSocket()

  const token = getCurrentToken()
  const storeId = getCurrentStoreId()
  if (!token || !storeId) {
    return
  }

  socket = createSuMessagingSocket(token, storeId)
  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data) as SuMessagingRealtimeEvent
      void handleRealtimeEvent(payload)
    } catch (error) {
      console.error('处理实时消息失败:', error)
    }
  }
  socket.onclose = () => {
    scheduleReconnect()
  }
}

const formatConversationTime = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const oneDay = 24 * 60 * 60 * 1000

  if (diff < oneDay) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }

  if (diff < 2 * oneDay) {
    return '昨天'
  }

  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const formatMessageTime = (date: Date) => {
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const toComparableText = (value?: string | null) => (value || '').trim().toLowerCase()

const resolveConversationLookupKey = (conversation: SuMessagingThreadDTO) => {
  const key = (conversation.bookingId || conversation.threadId || '').trim()
  return key ? `${conversation.channelId}:${key}` : ''
}

const findReservationIdForConversation = async (conversation: SuMessagingThreadDTO) => {
  const lookupKey = resolveConversationLookupKey(conversation)
  if (!lookupKey) {
    return null
  }

  if (reservationIdCache.has(lookupKey)) {
    return reservationIdCache.get(lookupKey) ?? null
  }

  const keyword = (conversation.bookingId || conversation.threadId || '').trim()
  if (!keyword) {
    reservationIdCache.set(lookupKey, null)
    return null
  }

  try {
    const response = await getReservationsWithFilters({ page: 0, size: 100, searchKeyword: keyword })
    if (!response.success) {
      reservationIdCache.set(lookupKey, null)
      return null
    }

    const reservations = response.data?.content || []
    const normalizedKeyword = toComparableText(keyword)

    const exactMatch = reservations.find((item: ReservationDTO) => {
      return (
        toComparableText(item.channelOrderNumber) === normalizedKeyword ||
        toComparableText(item.orderNumber) === normalizedKeyword
      )
    })

    const fuzzyMatch = reservations.find((item: ReservationDTO) => {
      return (
        toComparableText(item.channelOrderNumber).includes(normalizedKeyword) ||
        toComparableText(item.orderNumber).includes(normalizedKeyword)
      )
    })

    const resolved = exactMatch?.id || fuzzyMatch?.id || null
    reservationIdCache.set(lookupKey, resolved)
    return resolved
  } catch (error) {
    console.error('匹配订单失败:', error)
    reservationIdCache.set(lookupKey, null)
    return null
  }
}

const preloadActiveReservationId = async () => {
  const conversation = activeConversation.value
  if (!conversation) {
    selectedReservationId.value = null
    return
  }

  isResolvingReservation.value = true
  try {
    selectedReservationId.value = await findReservationIdForConversation(conversation)
  } finally {
    isResolvingReservation.value = false
  }
}

const openOrderDetail = async () => {
  if (!activeConversation.value) {
    return
  }

  if (!selectedReservationId.value) {
    await preloadActiveReservationId()
  }

  if (!selectedReservationId.value) {
    ElMessage.warning('当前会话未匹配到订单，无法打开订单明细')
    return
  }

  showOrderDetailDrawer.value = true
}

const initialize = async () => {
  await Promise.all([refreshThreads(), loadAiAutoReplySetting()])
  connectRealtimeSocket()
}

watch(
  () => activeThreadId.value,
  () => {
    void preloadActiveReservationId()
  },
)

onMounted(() => {
  void initialize()
})

onUnmounted(() => {
  isDestroyed = true
  closeRealtimeSocket()
})
</script>

<style scoped>
.messages-page {
  display: flex;
  height: calc(100vh - 60px);
  background: #f5f5f5;
}

.conversations-panel {
  width: 320px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.search-bar {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.conversations-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.2s ease;
}

.conversation-item:hover,
.conversation-item.active {
  background: #f8fbff;
}

.conversation-avatar,
.channel-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e6f4ff;
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.conversation-info,
.channel-details {
  min-width: 0;
  flex: 1;
}

.conversation-header,
.channel-info {
  display: flex;
  gap: 12px;
}

.conversation-header {
  justify-content: space-between;
  align-items: center;
}

.channel-name {
  color: #303133;
  font-weight: 600;
}

.last-message,
.channel-status-text {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
}

.conversation-status {
  margin-top: 8px;
}

.message-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.messages-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.messages-header {
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header-actions {
  flex-shrink: 0;
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.message-date-divider {
  margin: 12px 0 20px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.message-item {
  display: flex;
  align-items: flex-end;
  margin-bottom: 16px;
  gap: 8px;
}

.message-sent {
  justify-content: flex-end;
}

.message-received {
  justify-content: flex-start;
}

.message-content {
  max-width: min(75%, 560px);
}

.message-text {
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.6;
  white-space: pre-line;
  overflow-wrap: anywhere;
}

.message-link {
  text-decoration: underline;
  word-break: break-all;
}

.message-sent .message-link {
  color: #fff;
}

.message-received .message-link {
  color: #1677ff;
}

.sender-badge {
  opacity: 0.8;
  font-style: italic;
  display: block;
  margin-top: 6px;
  text-align: right;
}

.message-delivery-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-bottom: 24px;
  font-size: 16px;
  flex-shrink: 0;
}

.message-delivery-indicator.sending {
  color: #8c8c8c;
}

.message-delivery-indicator.failed {
  color: #ff4d4f;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.message-sent .message-text {
  background: #1890ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-received .message-text {
  background: #fff;
  color: #333;
  border: 1px solid #e8e8e8;
  border-bottom-left-radius: 4px;
}

.message-input-area {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.ai-reply-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-reply-label {
  font-size: 13px;
  color: #606266;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  padding: 24px;
  text-align: center;
}

.conversations-list::-webkit-scrollbar,
.messages-list::-webkit-scrollbar {
  width: 6px;
}

.conversations-list::-webkit-scrollbar-thumb,
.messages-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.conversations-list::-webkit-scrollbar-thumb:hover,
.messages-list::-webkit-scrollbar-thumb:hover {
  background: #bbb;
}
</style>
