<template>
  <div v-if="visible" class="customer-service-chat">
    <div class="chat-header">
      <div class="header-left">
        <el-icon class="service-icon"><Headset /></el-icon>
        <span class="service-title">{{ t('stage6.components.customerService.title') }}</span>
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

    <div ref="messagesContainer" class="chat-messages">
      <div class="message-list">
        <div v-for="message in messages" :key="message.id" :class="['message-item', message.type]">
          <div class="message-time">{{ message.time }}</div>
          <div class="message-content">
            <div v-if="message.type === 'guest'" class="guest-message-wrapper">
              <div class="guest-message-content">
                {{ message.content }}
              </div>
              <div class="guest-avatar">
                <el-icon class="avatar-icon">
                  {{
                    message.senderName
                      ? message.senderName.charAt(0)
                      : t('stage6.components.customerService.guestAvatarFallback')
                  }}
                </el-icon>
              </div>
            </div>
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
            serviceAvailable && selectedRoomId
              ? t('stage6.components.customerService.placeholderReady')
              : t('stage6.components.customerService.placeholderConnecting')
          "
          :disabled="isLoading"
          @keydown.enter.prevent="sendMessage"
        />
        <el-button
          type="primary"
          size="small"
          class="send-btn"
          :disabled="!inputMessage.trim() || isLoading || !selectedRoomId"
          :loading="isLoading"
          @click="sendMessage"
        >
          {{
            isLoading
              ? t('stage6.common.actions.sending')
              : t('stage6.common.actions.send')
          }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ChatDotSquare,
  Close,
  Download,
  Headset,
  Link,
  Microphone,
  Minus,
  Picture,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  createChatRoom,
  getActiveChatRooms,
  getRoomMessages,
  MessageType,
  pollNewMessages,
  sendRealTimeMessage,
  SenderType,
  type RealTimeChatRequest,
  type RealTimeChatResponse,
} from '../api/realTimeChat'

interface Message {
  id: number
  type: 'staff' | 'guest'
  content: string
  time: string
  senderName?: string
}

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
  minimize: []
}>()

const { t, locale } = useI18n()

const inputMessage = ref('')
const messagesContainer = ref<HTMLElement>()
const messages = ref<Message[]>([])
const isLoading = ref(false)
const serviceAvailable = ref(true)

const selectedRoomId = ref<string>('')
const guestName = ref(t('stage6.components.customerService.defaultGuestName'))
const guestRoomNumber = ref(t('stage6.components.customerService.defaultGuestRoom'))
const pollingInterval = ref<number | null>(null)
const lastPollTime = ref<string>('')

const formatMessageTime = (date: Date) =>
  date.toLocaleTimeString(locale.value, {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })

const close = () => {
  emit('close')
}

const minimize = () => {
  emit('minimize')
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value || !selectedRoomId.value) return

  const messageContent = inputMessage.value.trim()

  const guestMessage: Message = {
    id: Date.now(),
    type: 'guest',
    content: messageContent,
    time: formatMessageTime(new Date()),
    senderName: guestName.value,
  }

  messages.value.push(guestMessage)
  inputMessage.value = ''
  await scrollToBottom()

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
      console.log('Guest message sent:', response.data)
      lastPollTime.value = new Date().toISOString()
      setTimeout(() => pollMessages(), 100)
    } else {
      console.error('Support message send failed:', response.message)
      ElMessage.error(t('stage6.components.customerService.sendFailed'))
    }
  } catch (error) {
    console.error('Failed to send support message:', error)
    ElMessage.error(t('stage6.components.customerService.networkSendFailed'))

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

const createGuestChatRoom = async () => {
  try {
    console.log('Guest is getting or creating chat room...', {
      guestName: guestName.value,
      guestRoomNumber: guestRoomNumber.value,
    })

    const roomsResponse = await getActiveChatRooms()
    if (roomsResponse.success && roomsResponse.data && roomsResponse.data.length > 0) {
      const existingRoom = roomsResponse.data.find(
        (room) =>
          room.guestName === guestName.value && room.guestRoomNumber === guestRoomNumber.value,
      )

      if (existingRoom) {
        selectedRoomId.value = existingRoom.roomId
        console.log('Guest found existing chat room:', { roomId: selectedRoomId.value })

        await loadRoomMessages(selectedRoomId.value)
        lastPollTime.value = new Date().toISOString()
        startPolling()
        return
      }
    }

    console.log('Guest is creating a new chat room...')
    const response = await createChatRoom(guestName.value, guestRoomNumber.value)
    console.log('Create chat room response:', response)

    if (response.success && response.data) {
      selectedRoomId.value = response.data.roomId
      lastPollTime.value = new Date().toISOString()
      console.log('Guest chat room created:', { roomId: selectedRoomId.value })

      await loadRoomMessages(selectedRoomId.value)
      startPolling()
    } else {
      console.error('Failed to create chat room:', response.message)
    }
  } catch (error) {
    console.error('Failed to create chat room:', error)
  }
}

const loadRoomMessages = async (roomId: string) => {
  try {
    const response = await getRoomMessages(roomId)
    if (response.success && response.data) {
      messages.value = response.data.map((msg: RealTimeChatResponse) => ({
        id: msg.messageId,
        type: msg.senderType === SenderType.STAFF ? 'staff' : 'guest',
        content: msg.messageContent,
        time: formatMessageTime(new Date(msg.sentAt)),
        senderName: msg.senderName,
      }))
      await scrollToBottom()
    }
  } catch (error) {
    console.error('Failed to load chat room messages:', error)
  }
}

const pollMessages = async () => {
  if (!selectedRoomId.value) {
    console.log('Guest polling stopped: selectedRoomId is missing')
    return
  }

  console.log('Guest is polling new messages...', {
    roomId: selectedRoomId.value,
    lastPollTime: lastPollTime.value,
  })

  try {
    const response = await pollNewMessages(selectedRoomId.value, lastPollTime.value)

    if (response.success && response.data) {
      console.log('Guest received polling response:', response.data)
      response.data.forEach((msg: RealTimeChatResponse) => {
        console.log('Guest handling message:', msg)
        if (msg.senderType === SenderType.STAFF) {
          const staffMessage: Message = {
            id: msg.messageId,
            type: 'staff',
            content: msg.messageContent,
            time: formatMessageTime(new Date(msg.sentAt)),
            senderName: msg.senderName,
          }

          const exists = messages.value.find((m) => m.id === msg.messageId)
          console.log('Guest message already exists:', exists)
          if (!exists) {
            console.log('Adding support message to guest UI:', staffMessage)
            messages.value.push(staffMessage)
          } else {
            console.log('Message already exists, skipping')
          }
        } else {
          console.log('Skipping guest-owned message')
        }
      })

      if (response.data.length > 0) {
        await scrollToBottom()
        lastPollTime.value = new Date().toISOString()
      }
    }
  } catch (error) {
    console.error('Failed to poll messages:', error)
  }
}

const startPolling = () => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
  }

  console.log('Guest polling started...')
  pollingInterval.value = window.setInterval(() => {
    pollMessages()
  }, 1000)
}

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

const initializeGuestChat = async () => {
  await createGuestChatRoom()
}

watch(
  () => props.visible,
  (newVisible) => {
    if (newVisible) {
      initializeGuestChat()
    } else {
      stopPolling()
    }
  },
  { immediate: true },
)

onMounted(() => {
  scrollToBottom()
})

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

.message-item.guest .message-content {
  justify-content: flex-end;
}

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

@media (max-width: 480px) {
  .customer-service-chat {
    width: 360px;
    height: 480px;
    right: 10px;
    bottom: 10px;
  }
}
</style>
