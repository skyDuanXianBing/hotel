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
              <span class="message-time">{{ formatTime(conversation.lastActivity) }}</span>
            </div>
            <div class="last-message">
              {{ conversation.channelName }} | 订单号: {{ conversation.bookingId || conversation.threadId || '-' }}
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
                订单号: {{ activeConversation.bookingId || activeConversation.threadId || '-' }} |
                {{ getStatusText(activeConversation.closed) }}
              </div>
            </div>
          </div>
        </div>

        <div ref="messagesListRef" class="messages-list">
          <div
            v-for="message in messages"
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
                {{ message.content }}

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
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
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
import { createSuMessagingSocket, type SuMessagingRealtimeEvent } from '@/utils/suMessagingSocket'

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

const searchQuery = ref('')
const activeThreadId = ref<number | null>(null)
const newMessage = ref('')
const messagesListRef = ref<HTMLElement | null>(null)
const conversations = ref<SuMessagingThreadDTO[]>([])
const messages = ref<MessageItem[]>([])
const isSending = ref(false)
const aiAutoReplyEnabled = ref(true)
const isAiSettingSaving = ref(false)

let socket: WebSocket | null = null
let reconnectTimer: number | null = null
let isDestroyed = false

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
  if (trimmed.startsWith('AI') && /[\u00C0-\u00FF]/.test(trimmed)) {
    return 'AI助手'
  }
  if (trimmed.startsWith('AI') && /\uFFFD/.test(trimmed)) {
    return 'AI助手'
  }
  if (trimmed.startsWith('AI') && /[?？]{2,}/.test(trimmed)) {
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

const getCurrentToken = () => localStorage.getItem('token') || ''

const getCurrentStoreId = () => {
  const raw = localStorage.getItem('currentStore')
  if (!raw) {
    return null
  }

  try {
    const store = JSON.parse(raw)
    return typeof store?.id === 'number' ? store.id : Number(store?.id)
  } catch (error) {
    console.error('解析 currentStore 失败:', error)
    return null
  }
}

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
    const response = (await getSuThreads()) as any
    if (response.success && response.data) {
      conversations.value = response.data
      await ensureActiveConversation()
    }
  } catch (error) {
    console.error('刷新会话列表失败:', error)
    ElMessage.error('刷新失败')
  }
}

const loadThreadMessages = async (threadId: number) => {
  try {
    const response = (await getSuThreadMessages(threadId)) as any
    if (response.success && response.data) {
      messages.value = response.data.map(mapMessage)
      await scrollToBottom()
    }
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
    const response = (await getSuMessagingAiSetting()) as any
    if (response.success && response.data) {
      aiAutoReplyEnabled.value = Boolean(response.data.autoReplyEnabled)
    }
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
    const response = (await updateSuMessagingAiSetting(request)) as any
    if (response.success && response.data) {
      aiAutoReplyEnabled.value = Boolean(response.data.autoReplyEnabled)
      ElMessage.success(enabled ? 'AI 自动回复已开启' : 'AI 自动回复已关闭')
      return
    }
    throw new Error(response?.message || '更新失败')
  } catch (error) {
    aiAutoReplyEnabled.value = previousValue
    console.error('更新 AI 自动回复设置失败:', error)
    ElMessage.error('更新 AI 自动回复设置失败')
  } finally {
    isAiSettingSaving.value = false
  }
}

const sendMessage = async () => {
  if (!activeThreadId.value || !newMessage.value.trim() || isSending.value) {
    return
  }

  const content = newMessage.value.trim()
  isSending.value = true

  try {
    const response = (await sendSuThreadMessage(activeThreadId.value, {
      content,
      senderName: '客服',
    })) as any

    if (response.success && response.data) {
      const newMessageItem = mapMessage(response.data)
      const index = messages.value.findIndex((message) => message.id === newMessageItem.id)
      if (index >= 0) {
        messages.value[index] = { ...messages.value[index], ...newMessageItem }
      } else {
        messages.value.push(newMessageItem)
      }
      newMessage.value = ''
      await scrollToBottom()
      await refreshThreads()
      return
    }

    ElMessage.error('消息发送失败')
  } catch (error) {
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
    return false
  }
  messages.value.push(incoming)
  return true
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
    const appended = upsertMessage(mapRealtimeMessage(message))
    if (appended) {
      await scrollToBottom()
    }

    if (message.senderType === SuMessagingSenderType.GUEST && event.eventType === 'MESSAGE_CREATED') {
      await loadThreadMessages(threadId)
    }
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

const formatTime = (dateString: string) => {
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

const initialize = async () => {
  await Promise.all([refreshThreads(), loadAiAutoReplySetting()])
  connectRealtimeSocket()
}

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
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.message-item {
  display: flex;
  align-items: flex-end;
  margin-bottom: 16px;
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
  white-space: pre-wrap;
  word-break: break-word;
}

.sender-badge {
  opacity: 0.8;
  font-style: italic;
}

.message-delivery-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-right: 8px;
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
