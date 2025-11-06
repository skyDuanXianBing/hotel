<template>
  <div v-if="visible" class="customer-service-chat">
    <!-- 标题栏 -->
    <div class="chat-header">
      <div class="header-left">
        <el-icon class="service-icon"><Headset /></el-icon>
        <span class="service-title">客服</span>
      </div>
      <div class="header-right">
        <el-button text size="small" class="header-btn">
          <el-icon><Microphone /></el-icon>
        </el-button>
        <el-button text size="small" class="header-btn" @click="minimize">
          <el-icon><Minus /></el-icon>
        </el-button>
        <el-button text size="small" class="header-btn" @click="close">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 聊天消息区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <div class="message-list">
        <div v-for="message in messages" :key="message.id" :class="['message-item', message.type]">
          <div class="message-time">{{ message.time }}</div>
          <div class="message-content">
            <!-- 住户自己发送的消息显示在右边 -->
            <div v-if="message.type === 'guest'" class="guest-message-wrapper">
              <div class="guest-message-content">
                {{ message.content }}
              </div>
              <div class="guest-avatar">
                <el-icon class="avatar-icon">{{
                  message.senderName ? message.senderName.charAt(0) : '客'
                }}</el-icon>
              </div>
            </div>
            <!-- 客服回复的消息显示在左边 -->
            <div v-else class="staff-message">
              <div class="staff-avatar">
                <el-icon class="avatar-icon"><Headset /></el-icon>
              </div>
              <div class="message-bubble">
                {{ message.content }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input">
      <div class="input-tools">
        <el-button text size="small" class="tool-btn">
          <el-icon><ChatDotSquare /></el-icon>
        </el-button>
        <el-button text size="small" class="tool-btn">
          <el-icon><Picture /></el-icon>
        </el-button>
        <el-button text size="small" class="tool-btn">
          <el-icon><Link /></el-icon>
        </el-button>
        <el-button text size="small" class="tool-btn">
          <el-icon><Download /></el-icon>
        </el-button>
      </div>

      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          resize="none"
          :placeholder="
            serviceAvailable && selectedRoomId ? '请输入您的问题...' : '正在连接客服...'
          "
          :disabled="isLoading"
          @keydown.enter.prevent="sendMessage"
        />
        <el-button
          type="primary"
          size="small"
          class="send-btn"
          @click="sendMessage"
          :disabled="!inputMessage.trim() || isLoading || !selectedRoomId"
          :loading="isLoading"
        >
          {{ isLoading ? '发送中' : '发送' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, watch } from 'vue'
import {
  Headset,
  Microphone,
  Minus,
  Close,
  ChatDotSquare,
  Picture,
  Link,
  Download,
} from '@element-plus/icons-vue'
import {
  getActiveChatRooms,
  getRoomMessages,
  sendRealTimeMessage,
  pollNewMessages,
  createChatRoom,
  type ChatRoomInfo,
  type RealTimeChatRequest,
  type RealTimeChatResponse,
  SenderType,
  MessageType,
} from '../api/realTimeChat'
import { ElMessage } from 'element-plus'

interface Message {
  id: number
  type: 'staff' | 'guest'
  content: string
  time: string
  senderName?: string
}

// Props
const props = defineProps<{
  visible: boolean
}>()

// Emits
const emit = defineEmits<{
  close: []
  minimize: []
}>()

// 状态
const inputMessage = ref('')
const messagesContainer = ref<HTMLElement>()
const messages = ref<Message[]>([])
const isLoading = ref(false)
const serviceAvailable = ref(true)

// 聊天室相关状态
const chatRooms = ref<ChatRoomInfo[]>([])
const selectedRoomId = ref<string>('')
const guestName = ref('住户')
const guestRoomNumber = ref('1001')
const pollingInterval = ref<number | null>(null)
const lastPollTime = ref<string>('')

// 方法
const close = () => {
  emit('close')
}

const minimize = () => {
  emit('minimize')
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value || !selectedRoomId.value) return

  const messageContent = inputMessage.value.trim()

  // 立即显示住户消息
  const guestMessage: Message = {
    id: Date.now(),
    type: 'guest',
    content: messageContent,
    time: new Date().toLocaleTimeString('zh-CN', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
    senderName: guestName.value,
  }

  messages.value.push(guestMessage)
  inputMessage.value = ''
  await scrollToBottom()

  // 发送到服务器
  isLoading.value = true

  try {
    const chatRequest: RealTimeChatRequest = {
      roomId: selectedRoomId.value,
      message: messageContent,
      senderType: SenderType.GUEST,
      senderName: guestName.value,
      guestRoomNumber: guestRoomNumber.value,
      messageType: MessageType.TEXT,
    }

    const response = await sendRealTimeMessage(chatRequest)

    if (response.success) {
      console.log('住户消息发送成功:', response.data)
      // 更新最后轮询时间
      lastPollTime.value = new Date().toISOString()
      // 立即轮询一次以获取可能的新消息
      setTimeout(() => pollMessages(), 100)
    } else {
      console.error('客服消息发送失败:', response.message)
      ElMessage.error('消息发送失败')
    }
  } catch (error) {
    console.error('发送客服消息失败:', error)
    ElMessage.error('网络错误，消息发送失败')

    // 检查服务可用性
    if (error instanceof TypeError && error.message.includes('fetch')) {
      serviceAvailable.value = false
      setTimeout(() => {
        serviceAvailable.value = true
      }, 3000)
    }
  } finally {
    isLoading.value = false
  }
}

// 创建或获取住户聊天室
const createGuestChatRoom = async () => {
  try {
    console.log('住户正在获取或创建聊天室...', {
      guestName: guestName.value,
      guestRoomNumber: guestRoomNumber.value,
    })

    // 首先检查是否已有当前住户的活跃聊天室
    const roomsResponse = await getActiveChatRooms()
    if (roomsResponse.success && roomsResponse.data && roomsResponse.data.length > 0) {
      // 查找当前住户的聊天室
      const existingRoom = roomsResponse.data.find(
        (room) =>
          room.guestName === guestName.value && room.guestRoomNumber === guestRoomNumber.value,
      )

      if (existingRoom) {
        selectedRoomId.value = existingRoom.roomId
        console.log('住户找到已存在的聊天室:', { roomId: selectedRoomId.value })

        // 加载该聊天室的消息历史
        await loadRoomMessages(selectedRoomId.value)
        lastPollTime.value = new Date().toISOString()

        // 开始轮询新消息
        startPolling()
        return
      }
    }

    // 如果没有现有聊天室，创建新的
    console.log('住户正在创建新聊天室...')
    const response = await createChatRoom(guestName.value, guestRoomNumber.value)
    console.log('创建聊天室响应:', response)

    if (response.success && response.data) {
      selectedRoomId.value = response.data.roomId
      lastPollTime.value = new Date().toISOString()
      console.log('住户聊天室创建成功:', { roomId: selectedRoomId.value })

      // 加载该聊天室的消息历史
      await loadRoomMessages(selectedRoomId.value)

      // 开始轮询新消息
      startPolling()
    } else {
      console.error('创建聊天室失败:', response.message)
    }
  } catch (error) {
    console.error('创建聊天室失败:', error)
  }
}

// 选择聊天室
const selectChatRoom = async (roomId: string) => {
  selectedRoomId.value = roomId
  lastPollTime.value = new Date().toISOString()

  // 加载该聊天室的消息历史
  await loadRoomMessages(roomId)

  // 开始轮询新消息
  startPolling()
}

// 加载聊天室消息
const loadRoomMessages = async (roomId: string) => {
  try {
    const response = await getRoomMessages(roomId)
    if (response.success && response.data) {
      messages.value = response.data.map((msg: RealTimeChatResponse) => ({
        id: msg.messageId,
        type: msg.senderType === SenderType.STAFF ? 'staff' : 'guest',
        content: msg.messageContent,
        time: new Date(msg.sentAt).toLocaleTimeString('zh-CN', {
          hour12: false,
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
        }),
        senderName: msg.senderName,
      }))
      await scrollToBottom()
    }
  } catch (error) {
    console.error('加载聊天室消息失败:', error)
  }
}

// 轮询新消息
const pollMessages = async () => {
  if (!selectedRoomId.value) {
    console.log('住户轮询停止：没有selectedRoomId')
    return
  }

  console.log('住户轮询新消息中...', {
    roomId: selectedRoomId.value,
    lastPollTime: lastPollTime.value,
  })

  try {
    const response = await pollNewMessages(selectedRoomId.value, lastPollTime.value)

    if (response.success && response.data) {
      console.log('住户收到轮询响应:', response.data)
      response.data.forEach((msg: RealTimeChatResponse) => {
        console.log('住户处理消息:', msg)
        // 只添加不是当前住户发送的消息（即客服消息）
        if (msg.senderType === SenderType.STAFF) {
          const staffMessage: Message = {
            id: msg.messageId,
            type: 'staff',
            content: msg.messageContent,
            time: new Date(msg.sentAt).toLocaleTimeString('zh-CN', {
              hour12: false,
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit',
            }),
            senderName: msg.senderName,
          }

          // 检查是否已经存在，避免重复添加
          const exists = messages.value.find((m) => m.id === msg.messageId)
          console.log('住户消息是否已存在:', exists)
          if (!exists) {
            console.log('添加客服消息到住户界面:', staffMessage)
            messages.value.push(staffMessage)
          } else {
            console.log('消息已存在，跳过')
          }
        } else {
          console.log('跳过住户自己的消息')
        }
      })

      if (response.data.length > 0) {
        await scrollToBottom()
        // 更新最后轮询时间
        lastPollTime.value = new Date().toISOString()
      }
    }
  } catch (error) {
    console.error('轮询消息失败:', error)
  }
}

// 开始轮询
const startPolling = () => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
  }

  console.log('住户开始轮询...')
  pollingInterval.value = window.setInterval(() => {
    pollMessages()
  }, 1000) // 每1秒轮询一次，与客服端保持一致
}

// 停止轮询
const stopPolling = () => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
    pollingInterval.value = null
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 初始化住户聊天界面
const initializeGuestChat = async () => {
  // 创建住户聊天室
  await createGuestChatRoom()
}

// 监听组件显示状态，首次显示时初始化
watch(
  () => props.visible,
  (newVisible) => {
    if (newVisible) {
      initializeGuestChat()
    } else {
      // 隐藏时停止轮询
      stopPolling()
    }
  },
  { immediate: true },
)

// 组件挂载时滚动到底部
onMounted(() => {
  scrollToBottom()
})

// 组件卸载时清理资源
onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.customer-service-chat {
  position: fixed;
  right: 20px;
  bottom: 20px;
  width: 400px;
  height: 520px;
  background: white;
  border-radius: 12px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.12),
    0 4px 16px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  border: 1px solid rgba(0, 0, 0, 0.05);
  overflow: hidden;
  direction: ltr;
  writing-mode: horizontal-tb;
}

/* 标题栏 */
.chat-header {
  background: linear-gradient(135deg, #4f7bff 0%, #3a6aff 100%);
  color: white;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 12px 12px 0 0;
  min-height: 56px;
  box-shadow: 0 2px 8px rgba(79, 123, 255, 0.2);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.service-icon {
  font-size: 18px;
}

.service-title {
  font-size: 16px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-btn {
  color: white !important;
  width: 28px;
  height: 28px;
  border-radius: 4px;
}

.header-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  background: linear-gradient(180deg, #fafbfc 0%, #f8f9fa 100%);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.message-time {
  font-size: 12px;
  color: #999;
  text-align: center;
  margin-bottom: 12px;
  padding: 4px 0;
}

.message-content {
  display: flex;
  width: 100%;
}

/* 住户自己的消息 - 右对齐 */
.message-item.guest .message-content {
  justify-content: flex-end;
}

/* 客服回复的消息 - 左对齐 */
.message-item.staff .message-content {
  justify-content: flex-start;
}

.guest-message-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 85%;
}

.guest-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #34c759;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
  font-size: 14px;
  font-weight: 600;
}

.avatar-icon {
  color: white;
  font-size: 16px;
}

.guest-message-content {
  background: #4f7bff;
  color: white;
  padding: 14px 16px;
  border-radius: 16px 16px 4px 16px;
  font-size: 14px;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(79, 123, 255, 0.2);
  flex: 1;
  word-wrap: break-word;
  word-break: break-word;
  white-space: normal;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
}

.staff-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  max-width: 85%;
}

.staff-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #4f7bff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.message-bubble {
  background: white;
  color: #333;
  padding: 14px 16px;
  border-radius: 16px 16px 16px 4px;
  max-width: 75%;
  font-size: 14px;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  word-wrap: break-word;
  word-break: break-word;
  white-space: normal;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
}

/* 输入区域 */
.chat-input {
  border-top: 1px solid #e8e8e8;
  background: white;
  border-radius: 0 0 12px 12px;
}

.input-tools {
  padding: 10px 16px;
  display: flex;
  gap: 10px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.tool-btn {
  color: #888 !important;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.tool-btn:hover {
  color: #4f7bff !important;
  background: #f0f7ff !important;
  transform: translateY(-1px);
}

.input-area {
  padding: 16px;
  display: flex;
  gap: 12px;
  align-items: flex-end;
  background: white;
}

.input-area :deep(.el-textarea) {
  flex: 1;
}

.input-area :deep(.el-textarea__inner) {
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  padding: 12px 16px;
  transition: border-color 0.2s ease;
  background: #fafafa;
}

.input-area :deep(.el-textarea__inner):focus {
  border-color: #4f7bff;
  background: white;
  box-shadow: 0 0 0 3px rgba(79, 123, 255, 0.1);
}

.input-area :deep(.el-textarea__inner)::placeholder {
  color: #aaa;
}

.send-btn {
  background: #4f7bff;
  border-color: #4f7bff;
  height: 40px;
  padding: 0 24px;
  border-radius: 12px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(79, 123, 255, 0.2);
}

.send-btn:hover {
  background: #3a6aff;
  border-color: #3a6aff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 123, 255, 0.3);
}

.send-btn:disabled {
  background: #e0e0e0;
  border-color: #e0e0e0;
  color: #999;
  transform: none;
  box-shadow: none;
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 动画效果 */
.customer-service-chat {
  animation: slideIn 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

@keyframes slideIn {
  from {
    transform: translateY(30px) scale(0.95);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .customer-service-chat {
    width: 360px;
    height: 480px;
    right: 10px;
    bottom: 10px;
  }
}
</style>
