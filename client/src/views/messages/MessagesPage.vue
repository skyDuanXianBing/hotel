<template>
  <div class="messages-page">
    <!-- 左侧会话列表 -->
    <div class="conversations-panel">
      <div class="panel-header">
        <h2>收件箱</h2>
        <el-button text size="small" @click="refreshMailboxes">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>

      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索会话"
          :prefix-icon="Search"
          clearable
        />
      </div>

      <div class="conversations-list">
        <div
          v-for="conversation in filteredConversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{ active: activeMailboxId === conversation.id }"
          @click="selectConversation(conversation.id)"
        >
          <div class="conversation-avatar">
            <el-icon><User /></el-icon>
          </div>
          <div class="conversation-info">
            <div class="conversation-header">
              <span class="channel-name">{{ conversation.guestName }}</span>
              <span class="message-time">{{ formatTime(conversation.lastActivity) }}</span>
            </div>
            <div class="last-message">
              房间: {{ conversation.guestRoomNumber || '未分配' }}
            </div>
            <div class="conversation-status">
              <el-tag :type="getStatusType(conversation.status)" size="small">
                {{ getStatusText(conversation.status) }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="conversations.length === 0" class="empty-state">
          <el-icon :size="48" color="#ddd"><ChatDotRound /></el-icon>
          <p>暂无会话</p>
        </div>
      </div>
    </div>

    <!-- 右侧消息面板 -->
    <div class="messages-panel">
      <template v-if="activeConversation">
        <!-- 会话头部 -->
        <div class="messages-header">
          <div class="channel-info">
            <div class="channel-avatar">
              <el-icon><User /></el-icon>
            </div>
            <div class="channel-details">
              <div class="channel-name">{{ activeConversation.guestName }}</div>
              <div class="channel-status">
                房间: {{ activeConversation.guestRoomNumber || '未分配' }} |
                {{ getStatusText(activeConversation.status) }}
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button size="small" @click="closeMailboxDialog">
              <el-icon><Close /></el-icon>
              关闭会话
            </el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="messages-list" ref="messagesListRef">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-item"
            :class="{
              'message-sent': message.senderType === 'STAFF',
              'message-received': message.senderType === 'GUEST'
            }"
          >
            <div class="message-content">
              <div class="message-text">
                {{ message.content }}
                <span v-if="message.senderName && message.senderType === 'STAFF'" class="sender-badge">
                  - {{ message.senderName }}
                </span>
              </div>
              <div class="message-time">{{ formatMessageTime(message.timestamp) }}</div>
            </div>
          </div>

          <!-- 正在输入指示器 -->
          <div v-if="isLoading" class="typing-indicator">
            <div class="typing-dots">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>

        <!-- 消息输入框 -->
        <div class="message-input-area">
          <el-input
            v-model="newMessage"
            type="textarea"
            :rows="3"
            placeholder="输入消息..."
            :disabled="isLoading || activeConversation.status === 'CLOSED'"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="input-actions">
            <el-button
              type="success"
              :icon="MagicStick"
              @click="sendAiReply"
              :loading="isAiReplying"
              :disabled="!lastGuestMessage || isLoading || activeConversation.status === 'CLOSED'"
            >
              AI回复
            </el-button>
            <el-button
              type="primary"
              @click="sendMessage"
              :disabled="!newMessage.trim() || isLoading || activeConversation.status === 'CLOSED'"
              :loading="isLoading"
            >
              发送
            </el-button>
          </div>
        </div>
      </template>

      <!-- 未选择会话时的占位符 -->
      <div v-else class="empty-state">
        <el-icon :size="80" color="#ccc"><ChatDotRound /></el-icon>
        <p>选择一个会话开始聊天</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { Search, ChatDotRound, User, Refresh, Close, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getActiveMailboxes,
  getMailboxMessages,
  pollMailboxMessages,
  sendMailboxMessage,
  closeMailbox,
  type VirtualMailboxDTO,
  type EmailMessageDTO,
  EmailSenderType,
  MailboxStatus
} from '@/api/virtualMailbox'
import { sendChatMessage, type ChatMessageRequest } from '@/api/chat'

interface Message {
  id: number
  senderType: EmailSenderType
  content: string
  timestamp: Date
  senderName?: string
}

// 搜索查询
const searchQuery = ref('')

// 当前选中的会话ID
const activeMailboxId = ref<number | null>(null)

// 新消息输入
const newMessage = ref('')

// 消息列表引用
const messagesListRef = ref<HTMLElement | null>(null)

// 会话数据
const conversations = ref<VirtualMailboxDTO[]>([])

// 消息列表
const messages = ref<Message[]>([])

// 加载状态
const isLoading = ref(false)

// AI回复状态
const isAiReplying = ref(false)
const aiSessionId = ref('')

// 轮询相关
const pollingInterval = ref<number | null>(null)
const lastPollTime = ref<string>('')

// 过滤后的会话列表
const filteredConversations = computed(() => {
  if (!searchQuery.value.trim()) {
    return conversations.value
  }
  const query = searchQuery.value.toLowerCase()
  return conversations.value.filter(
    (conv) =>
      conv.guestName.toLowerCase().includes(query) ||
      conv.guestRoomNumber?.toLowerCase().includes(query)
  )
})

// 当前活动的会话
const activeConversation = computed(() => {
  if (!activeMailboxId.value) return null
  return conversations.value.find((conv) => conv.id === activeMailboxId.value) || null
})

// 最后一条住客消息
const lastGuestMessage = computed(() => {
  const guestMessages = messages.value.filter(m => m.senderType === EmailSenderType.GUEST)
  return guestMessages.length > 0 ? guestMessages[guestMessages.length - 1].content : ''
})

// 获取状态类型
const getStatusType = (status: MailboxStatus) => {
  switch (status) {
    case MailboxStatus.ACTIVE:
      return 'success'
    case MailboxStatus.CLOSED:
      return 'info'
    default:
      return 'info'
  }
}

// 获取状态文本
const getStatusText = (status: MailboxStatus) => {
  switch (status) {
    case MailboxStatus.ACTIVE:
      return '活跃'
    case MailboxStatus.CLOSED:
      return '已关闭'
    default:
      return '未知'
  }
}

// 刷新邮箱列表
const refreshMailboxes = async () => {
  try {
    const response = await getActiveMailboxes()
    if (response.success && response.data) {
      conversations.value = response.data
    }
  } catch (error) {
    console.error('刷新邮箱列表失败:', error)
    ElMessage.error('刷新失败')
  }
}

// 选择会话
const selectConversation = async (mailboxId: number) => {
  activeMailboxId.value = mailboxId
  await loadMailboxMessages(mailboxId)
  lastPollTime.value = new Date().toISOString()
}

// 加载邮箱消息
const loadMailboxMessages = async (mailboxId: number) => {
  try {
    const response = await getMailboxMessages(mailboxId)
    if (response.success && response.data) {
      messages.value = response.data.map((msg: EmailMessageDTO) => ({
        id: msg.id,
        senderType: msg.senderType,
        content: msg.content,
        timestamp: new Date(msg.timestamp),
        senderName: msg.senderName,
      }))
      await scrollToBottom()
    }
  } catch (error) {
    console.error('加载邮箱消息失败:', error)
    ElMessage.error('加载消息失败')
  }
}

// 发送消息
const sendMessage = async () => {
  if (!newMessage.value.trim() || !activeMailboxId.value || isLoading.value) return

  const messageContent = newMessage.value.trim()
  isLoading.value = true

  try {
    const response = await sendMailboxMessage(activeMailboxId.value, {
      content: messageContent,
      senderName: '客服'
    })

    if (response.success && response.data) {
      // 添加到消息列表
      messages.value.push({
        id: response.data.id,
        senderType: response.data.senderType,
        content: response.data.content,
        timestamp: new Date(response.data.timestamp),
        senderName: response.data.senderName,
      })

      newMessage.value = ''
      await scrollToBottom()
      lastPollTime.value = new Date().toISOString()
    } else {
      ElMessage.error('消息发送失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('网络错误，消息发送失败')
  } finally {
    isLoading.value = false
  }
}

// AI回复
const sendAiReply = async () => {
  if (!lastGuestMessage.value || isAiReplying.value || !activeMailboxId.value) return

  isAiReplying.value = true

  try {
    const chatRequest: ChatMessageRequest = {
      message: lastGuestMessage.value,
      sessionId: aiSessionId.value || undefined,
    }

    const response = await sendChatMessage(chatRequest)

    if (response.success && response.data) {
      aiSessionId.value = response.data.sessionId

      // 发送AI回复到虚拟邮箱
      const mailboxResponse = await sendMailboxMessage(activeMailboxId.value, {
        content: response.data.reply,
        senderName: 'AI客服'
      })

      if (mailboxResponse.success && mailboxResponse.data) {
        // 添加到消息列表
        messages.value.push({
          id: mailboxResponse.data.id,
          senderType: mailboxResponse.data.senderType,
          content: response.data.reply,
          timestamp: new Date(mailboxResponse.data.timestamp),
          senderName: 'AI客服',
        })

        await scrollToBottom()
        lastPollTime.value = new Date().toISOString()
        ElMessage.success('AI回复已发送')
      }
    } else {
      ElMessage.error('AI回复生成失败')
    }
  } catch (error) {
    console.error('AI回复失败:', error)
    ElMessage.error('AI服务暂时不可用')
  } finally {
    isAiReplying.value = false
  }
}

// 关闭邮箱
const closeMailboxDialog = async () => {
  if (!activeMailboxId.value) return

  try {
    await ElMessageBox.confirm('确认关闭此会话吗？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const response = await closeMailbox(activeMailboxId.value)
    if (response.success) {
      ElMessage.success('会话已关闭')
      await refreshMailboxes()
      activeMailboxId.value = null
      messages.value = []
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('关闭会话失败:', error)
      ElMessage.error('关闭会话失败')
    }
  }
}

// 轮询新消息
const pollMessages = async () => {
  if (!activeMailboxId.value) return

  try {
    const response = await pollMailboxMessages(activeMailboxId.value, lastPollTime.value)

    if (response.success && response.data && response.data.length > 0) {
      let hasNewMessages = false

      response.data.forEach((msg: EmailMessageDTO) => {
        // 检查消息是否已存在
        const exists = messages.value.find((m) => m.id === msg.id)
        if (!exists) {
          messages.value.push({
            id: msg.id,
            senderType: msg.senderType,
            content: msg.content,
            timestamp: new Date(msg.timestamp),
            senderName: msg.senderName,
          })
          hasNewMessages = true
        }
      })

      if (hasNewMessages) {
        await scrollToBottom()
      }
    }

    // 更新轮询时间
    lastPollTime.value = new Date().toISOString()

    // 刷新会话列表（以更新最后活动时间）
    await refreshMailboxes()
  } catch (error) {
    console.error('轮询消息失败:', error)
  }
}

// 开始轮询
const startPolling = () => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
  }

  pollingInterval.value = window.setInterval(() => {
    pollMessages()
  }, 2000) // 每2秒轮询一次
}

// 停止轮询
const stopPolling = () => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
    pollingInterval.value = null
  }
}

// 滚动到消息列表底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesListRef.value) {
    messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight
  }
}

// 格式化时间 (会话列表)
const formatTime = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const oneDay = 24 * 60 * 60 * 1000

  if (diff < oneDay) {
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes}`
  } else if (diff < 2 * oneDay) {
    return '昨天'
  } else {
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${month}-${day}`
  }
}

// 格式化消息时间
const formatMessageTime = (date: Date) => {
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

// 初始化
const initialize = async () => {
  await refreshMailboxes()

  // 如果有会话，选择第一个
  if (conversations.value.length > 0) {
    await selectConversation(conversations.value[0].id)
  }

  // 开始轮询
  startPolling()
}

// 生命周期
onMounted(() => {
  initialize()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.messages-page {
  display: flex;
  height: calc(100vh - 60px);
  background: #f5f5f5;
}

/* 左侧会话列表 */
.conversations-panel {
  width: 320px;
  background: white;
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
  position: relative;
}

.conversation-item:hover {
  background: #f8f9fa;
}

.conversation-item.active {
  background: #e6f7ff;
}

.conversation-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: white;
  font-size: 20px;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.channel-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.message-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
}

.last-message {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.conversation-status {
  margin-top: 4px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

/* 右侧消息面板 */
.messages-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
}

.messages-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.channel-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.channel-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.channel-details {
  flex: 1;
}

.channel-details .channel-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.channel-status {
  font-size: 12px;
  color: #666;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f8f9fa;
}

.message-item {
  display: flex;
  margin-bottom: 16px;
}

.message-item.message-sent {
  justify-content: flex-end;
}

.message-item.message-received {
  justify-content: flex-start;
}

.message-content {
  max-width: 60%;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;
}

.sender-badge {
  font-size: 12px;
  opacity: 0.8;
  font-style: italic;
}

.message-sent .message-text {
  background: #1890ff;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-received .message-text {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
  border: 1px solid #e8e8e8;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  padding: 0 4px;
}

.message-sent .message-time {
  text-align: right;
}

.message-received .message-time {
  text-align: left;
}

/* 正在输入指示器 */
.typing-indicator {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 16px;
}

.typing-dots {
  background: white;
  padding: 14px 16px;
  border-radius: 16px 16px 16px 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  display: flex;
  gap: 4px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ccc;
  animation: typing 1.4s infinite ease-in-out;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}
.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%,
  80%,
  100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.message-input-area {
  padding: 16px 24px;
  background: white;
  border-top: 1px solid #e8e8e8;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

/* 滚动条样式 */
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
