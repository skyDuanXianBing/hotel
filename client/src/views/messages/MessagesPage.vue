<template>
  <div class="messages-page">
    <!-- 左侧会话列表 -->
    <div class="conversations-panel">
      <div class="panel-header">
        <h2>所有消息</h2>
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
          :class="{ active: activeConversationId === conversation.id }"
          @click="selectConversation(conversation.id)"
        >
          <div class="conversation-avatar">
            <img :src="conversation.channelLogo" :alt="conversation.channelName" />
          </div>
          <div class="conversation-info">
            <div class="conversation-header">
              <span class="channel-name">{{ conversation.channelName }}</span>
              <span class="message-time">{{ formatTime(conversation.lastMessageTime) }}</span>
            </div>
            <div class="last-message">{{ conversation.lastMessage }}</div>
          </div>
          <div v-if="conversation.unreadCount > 0" class="unread-badge">
            {{ conversation.unreadCount }}
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧消息面板 -->
    <div class="messages-panel">
      <template v-if="activeConversation">
        <!-- 会话头部 -->
        <div class="messages-header">
          <div class="channel-info">
            <img :src="activeConversation.channelLogo" :alt="activeConversation.channelName" class="channel-logo" />
            <div class="channel-details">
              <div class="channel-name">{{ activeConversation.channelName }}</div>
              <div class="channel-status">在线</div>
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="messages-list" ref="messagesListRef">
          <div
            v-for="message in activeConversation.messages"
            :key="message.id"
            class="message-item"
            :class="{ 'message-sent': message.sender === 'me', 'message-received': message.sender !== 'me' }"
          >
            <div class="message-content">
              <div class="message-text">{{ message.content }}</div>
              <div class="message-time">{{ formatMessageTime(message.timestamp) }}</div>
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
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="input-actions">
            <el-button type="primary" @click="sendMessage" :disabled="!newMessage.trim()">
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
import { ref, computed, nextTick, onMounted } from 'vue'
import { Search, ChatDotRound } from '@element-plus/icons-vue'

interface Message {
  id: number
  sender: string
  content: string
  timestamp: Date
}

interface Conversation {
  id: number
  channelName: string
  channelLogo: string
  lastMessage: string
  lastMessageTime: Date
  unreadCount: number
  messages: Message[]
}

// 搜索查询
const searchQuery = ref('')

// 当前选中的会话ID
const activeConversationId = ref<number | null>(null)

// 新消息输入
const newMessage = ref('')

// 消息列表引用
const messagesListRef = ref<HTMLElement | null>(null)

// 模拟会话数据
const conversations = ref<Conversation[]>([
  {
    id: 1,
    channelName: 'Booking.com',
    channelLogo: 'https://cf.bstatic.com/static/img/b26logo/booking_logo_knowledge_graph/247454a990efac1952e44dddbf30c58677aa0fd8.png',
    lastMessage: '您好,请问房间还有空余吗?',
    lastMessageTime: new Date(2025, 10, 7, 14, 30),
    unreadCount: 2,
    messages: [
      {
        id: 1,
        sender: 'customer',
        content: '您好,我想预订一个房间',
        timestamp: new Date(2025, 10, 7, 14, 20),
      },
      {
        id: 2,
        sender: 'me',
        content: '您好!请问您需要什么类型的房间?',
        timestamp: new Date(2025, 10, 7, 14, 25),
      },
      {
        id: 3,
        sender: 'customer',
        content: '您好,请问房间还有空余吗?',
        timestamp: new Date(2025, 10, 7, 14, 30),
      },
    ],
  },
  {
    id: 2,
    channelName: 'Airbnb',
    channelLogo: 'https://a0.muscache.com/airbnb/static/logos/belo-200x200-4d851c5b28f61931bf1df28dd15e60ef.png',
    lastMessage: '好的,谢谢!',
    lastMessageTime: new Date(2025, 10, 7, 13, 15),
    unreadCount: 0,
    messages: [
      {
        id: 1,
        sender: 'customer',
        content: '请问可以提前入住吗?',
        timestamp: new Date(2025, 10, 7, 13, 10),
      },
      {
        id: 2,
        sender: 'me',
        content: '可以的,请在下午2点后到店办理入住',
        timestamp: new Date(2025, 10, 7, 13, 12),
      },
      {
        id: 3,
        sender: 'customer',
        content: '好的,谢谢!',
        timestamp: new Date(2025, 10, 7, 13, 15),
      },
    ],
  },
  {
    id: 3,
    channelName: '携程',
    channelLogo: 'https://pages.ctrip.com/commerce/promote/20160/images/logo.png',
    lastMessage: '已为您安排',
    lastMessageTime: new Date(2025, 10, 7, 12, 0),
    unreadCount: 0,
    messages: [
      {
        id: 1,
        sender: 'customer',
        content: '我需要一个安静的房间',
        timestamp: new Date(2025, 10, 7, 11, 50),
      },
      {
        id: 2,
        sender: 'me',
        content: '已为您安排',
        timestamp: new Date(2025, 10, 7, 12, 0),
      },
    ],
  },
  {
    id: 4,
    channelName: '美团',
    channelLogo: 'https://p0.meituan.net/travelcube/dist/images/logo_9d350e4.png',
    lastMessage: '房间很满意',
    lastMessageTime: new Date(2025, 10, 6, 18, 45),
    unreadCount: 0,
    messages: [
      {
        id: 1,
        sender: 'customer',
        content: '房间很满意',
        timestamp: new Date(2025, 10, 6, 18, 45),
      },
    ],
  },
])

// 过滤后的会话列表
const filteredConversations = computed(() => {
  if (!searchQuery.value.trim()) {
    return conversations.value
  }
  const query = searchQuery.value.toLowerCase()
  return conversations.value.filter(
    (conv) =>
      conv.channelName.toLowerCase().includes(query) ||
      conv.lastMessage.toLowerCase().includes(query)
  )
})

// 当前活动的会话
const activeConversation = computed(() => {
  if (activeConversationId.value === null) return null
  return conversations.value.find((conv) => conv.id === activeConversationId.value) || null
})

// 选择会话
const selectConversation = (id: number) => {
  activeConversationId.value = id
  // 标记为已读
  const conversation = conversations.value.find((conv) => conv.id === id)
  if (conversation) {
    conversation.unreadCount = 0
  }
  // 滚动到底部
  nextTick(() => {
    scrollToBottom()
  })
}

// 发送消息
const sendMessage = () => {
  if (!newMessage.value.trim() || !activeConversation.value) return

  const message: Message = {
    id: Date.now(),
    sender: 'me',
    content: newMessage.value.trim(),
    timestamp: new Date(),
  }

  activeConversation.value.messages.push(message)
  activeConversation.value.lastMessage = message.content
  activeConversation.value.lastMessageTime = message.timestamp

  newMessage.value = ''

  // 滚动到底部
  nextTick(() => {
    scrollToBottom()
  })
}

// 滚动到消息列表底部
const scrollToBottom = () => {
  if (messagesListRef.value) {
    messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight
  }
}

// 格式化时间 (会话列表)
const formatTime = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const oneDay = 24 * 60 * 60 * 1000

  if (diff < oneDay) {
    // 今天,显示时间
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes}`
  } else if (diff < 2 * oneDay) {
    // 昨天
    return '昨天'
  } else {
    // 更早,显示日期
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

// 组件挂载时选择第一个会话
onMounted(() => {
  if (conversations.value.length > 0) {
    selectConversation(conversations.value[0].id)
  }
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
  align-items: center;
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
  overflow: hidden;
  flex-shrink: 0;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.conversation-avatar img {
  width: 100%;
  height: 100%;
  object-fit: contain;
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: #ff4d4f;
  color: white;
  font-size: 12px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
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
}

.channel-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.channel-logo {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: contain;
  background: white;
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
  color: #52c41a;
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

.message-input-area {
  padding: 16px 24px;
  background: white;
  border-top: 1px solid #e8e8e8;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.empty-state p {
  margin-top: 16px;
  font-size: 14px;
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
