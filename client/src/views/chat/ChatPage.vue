<template>
  <div class="chat-page">
    <!-- 页面头部 -->
    <div class="chat-header">
      <div class="header-content">
        <div class="brand-info">
          <el-icon class="brand-icon"><Headset /></el-icon>
          <h1 class="brand-title">乐迪酒店管理系统</h1>
          <span class="brand-subtitle">住户聊天</span>
        </div>
        <div class="header-actions">
          <div class="ai-toggle">
            <el-switch
              v-model="autoReplyEnabled"
              :disabled="isAiReplying"
              active-text="AI自动回复"
              size="small"
            />
            <span v-if="isAiReplying" class="ai-status">AI回复中...</span>
          </div>
          <el-button type="primary" size="small" @click="clearChat">
            <el-icon><Delete /></el-icon>
            清空对话
          </el-button>
          <el-button size="small" @click="goHome">
            <el-icon><House /></el-icon>
            返回主页
          </el-button>
        </div>
      </div>
    </div>

    <!-- 聊天内容区域 -->
    <div class="chat-container">
      <!-- 左侧会话列表 -->
      <aside class="conversation-sidebar">
        <div class="sidebar-header">
          <h3 class="sidebar-title">会话列表</h3>
          <div class="sidebar-actions">
            <el-button text size="small">一键已读</el-button>
            <el-button text size="small">设置</el-button>
          </div>
        </div>

        <div class="search-box">
          <el-input
            placeholder="搜索客人昵称"
            size="small"
            clearable
          >
            <template #suffix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="filter-bar">
          <el-select v-model="channelFilter" placeholder="全部渠道" size="small">
            <el-option label="全部渠道" value=""></el-option>
            <el-option
              v-for="channel in channelList"
              :key="channel.id"
              :label="channel.name"
              :value="channel.code"
            ></el-option>
          </el-select>
          <el-select v-model="statusFilter" placeholder="全部状态" size="small">
            <el-option label="全部状态" value=""></el-option>
            <el-option label="在线" value="online"></el-option>
            <el-option label="离线" value="offline"></el-option>
          </el-select>
        </div>

        <div class="conversation-list">
          <div class="empty-state">
            <el-icon class="empty-icon"><ChatDotSquare /></el-icon>
            <p class="empty-text">暂无会话列表</p>
          </div>
        </div>
      </aside>

      <div class="chat-main">
        <!-- 消息列表 -->
        <div class="messages-area" ref="messagesContainer">
          <div class="welcome-message" v-if="messages.length === 0">
            <div class="welcome-avatar">
              <el-icon><Headset /></el-icon>
            </div>
            <div class="welcome-content">
              <h3>欢迎使用住户聊天服务</h3>
              <p>您可以通过此平台与酒店客服实时沟通：</p>
              <ul class="feature-list">
                <li>
                  <el-icon><Calendar /></el-icon> 房间服务咨询
                </li>
                <li>
                  <el-icon><Monitor /></el-icon> 设施问题报修
                </li>
                <li>
                  <el-icon><Money /></el-icon> 账单查询和结算
                </li>
                <li>
                  <el-icon><Connection /></el-icon> 投诉和建议
                </li>
                <li>
                  <el-icon><DataAnalysis /></el-icon> 其他服务需求
                </li>
              </ul>
              <p class="welcome-tip">请输入您的问题，客服人员会及时为您解答！</p>
            </div>
          </div>

          <div class="message-list">
            <div
              v-for="message in messages"
              :key="message.id"
              :class="['message-item', message.type]"
            >
              <div class="message-time">{{ message.time }}</div>
              <div class="message-content">
                <!-- 住户消息显示在左边 -->
                <div v-if="message.type === 'guest'" class="guest-message-wrapper">
                  <div class="guest-avatar">
                    <el-icon class="avatar-icon"><User /></el-icon>
                  </div>
                  <div class="guest-message-content">
                    {{ message.content }}
                  </div>
                </div>
                <!-- 客服消息显示在右边 -->
                <div v-else class="staff-message">
                  <div class="message-bubble">
                    {{ message.content }}
                  </div>
                  <div class="staff-avatar">
                    <el-icon class="avatar-icon"><Headset /></el-icon>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 正在输入指示器 -->
          <div v-if="isLoading" class="typing-indicator">
            <div class="staff-avatar">
              <el-icon class="avatar-icon"><Headset /></el-icon>
            </div>
            <div class="typing-dots">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <div class="input-container">
            <div class="input-tools">
              <el-button text size="small" class="tool-btn" title="表情">
                <el-icon><ChatDotSquare /></el-icon>
              </el-button>
              <el-button text size="small" class="tool-btn" title="图片">
                <el-icon><Picture /></el-icon>
              </el-button>
              <el-button text size="small" class="tool-btn" title="文件">
                <el-icon><Document /></el-icon>
              </el-button>
            </div>

            <div class="input-box">
              <el-input
                v-model="inputMessage"
                type="textarea"
                :rows="3"
                resize="none"
                :placeholder="
                  serviceAvailable ? '请输入您的问题...' : '聊天服务暂时不可用，请稍后再试'
                "
                :disabled="isLoading"
                @keydown.enter.prevent="handleEnterKey"
                @keydown.ctrl.enter="insertNewline"
                class="message-input"
              />
              <el-button
                type="primary"
                size="large"
                class="send-btn"
                @click="sendMessage"
                :disabled="!inputMessage.trim() || isLoading"
                :loading="isLoading"
              >
                <el-icon><Promotion /></el-icon>
              </el-button>
            </div>

            <div class="input-footer">
              <span class="input-tip">按 Enter 发送，Ctrl+Enter 换行</span>
              <span class="service-status" :class="{ offline: !serviceAvailable }">
                <el-icon><CircleCheck v-if="serviceAvailable" /><CircleClose v-else /></el-icon>
                {{ serviceAvailable ? '服务正常' : '服务离线' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Headset,
  User,
  Delete,
  House,
  Calendar,
  Monitor,
  Money,
  Connection,
  DataAnalysis,
  ChatDotSquare,
  Picture,
  Document,
  Promotion,
  CircleCheck,
  CircleClose,
  Search,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  sendRealTimeMessage,
  getRoomMessages,
  pollNewMessages,
  createChatRoom,
  getActiveChatRooms,
  type RealTimeChatRequest,
  type RealTimeChatResponse,
  SenderType,
  MessageType,
} from '../../api/realTimeChat'
import { sendChatMessage, type ChatMessageRequest, type ChatMessageResponse } from '../../api/chat'
import { getAllChannels, type ChannelDTO } from '../../api/channel'

interface Message {
  id: number
  type: 'staff' | 'guest'
  content: string
  time: string
  senderName?: string
}

// 路由
const router = useRouter()

// 状态
const inputMessage = ref('')
const messagesContainer = ref<HTMLElement>()
const messages = ref<Message[]>([])
const roomId = ref<string>('')
const isLoading = ref(false)
const serviceAvailable = ref(true)
const staffName = ref('客服')
const selectedRoomId = ref('')
const pollingInterval = ref<number | null>(null)
const lastPollTime = ref<string>('')

// AI自动回复配置
const autoReplyEnabled = ref(true) // 默认启用AI自动回复
const aiSessionId = ref('')
const isAiReplying = ref(false)

// 会话列表筛选
const channelFilter = ref('')
const statusFilter = ref('')
const channelList = ref<ChannelDTO[]>([])

// 加载渠道列表
const loadChannels = async () => {
  try {
    const response = await getAllChannels()
    if (response.success && response.data) {
      channelList.value = response.data.filter((channel) => channel.enabled)
    }
  } catch (error) {
    console.error('加载渠道列表失败:', error)
  }
}

// 方法
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value || !roomId.value) return

  const messageContent = inputMessage.value.trim()

  // 立即显示客服消息
  const staffMessage: Message = {
    id: Date.now(),
    type: 'staff',
    content: messageContent,
    time: new Date().toLocaleTimeString('zh-CN', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
    senderName: staffName.value,
  }

  messages.value.push(staffMessage)
  inputMessage.value = ''
  await scrollToBottom()

  // 发送到服务器
  isLoading.value = true

  try {
    const chatRequest: RealTimeChatRequest = {
      roomId: roomId.value,
      message: messageContent,
      senderType: SenderType.STAFF,
      senderName: staffName.value,
      messageType: MessageType.TEXT,
    }

    const response = await sendRealTimeMessage(chatRequest)

    if (response.success) {
      console.log('消息发送成功:', response.data)
      // 更新最后轮询时间
      lastPollTime.value = new Date().toISOString()
      // 立即轮询一次以获取可能的新消息
      setTimeout(() => pollMessages(), 100)
    } else {
      console.error('消息发送失败:', response.message)
      ElMessage.error('消息发送失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
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

const addStaffMessage = async (content: string, senderName: string = '客服') => {
  const staffMessage: Message = {
    id: Date.now() + Math.random(),
    type: 'staff',
    content,
    time: new Date().toLocaleTimeString('zh-CN', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
    senderName,
  }

  messages.value.push(staffMessage)
  await scrollToBottom()
}

// AI自动回复函数
const generateAIReply = async (guestMessage: string) => {
  if (!autoReplyEnabled.value || isAiReplying.value) {
    return
  }

  console.log('客服页面：准备生成AI回复，住户消息:', guestMessage)
  isAiReplying.value = true

  try {
    const chatRequest: ChatMessageRequest = {
      message: guestMessage,
      sessionId: aiSessionId.value || undefined,
    }

    console.log('客服页面：调用AI聊天接口...', chatRequest)
    const response = await sendChatMessage(chatRequest)

    if (response.success && response.data) {
      // 更新AI会话ID
      aiSessionId.value = response.data.sessionId

      console.log('客服页面：AI回复生成成功:', response.data.reply)

      // 发送AI回复到实时聊天
      const aiReplyRequest: RealTimeChatRequest = {
        roomId: roomId.value,
        message: response.data.reply,
        senderType: SenderType.STAFF,
        senderName: 'AI客服',
        messageType: MessageType.TEXT,
      }

      // 立即显示AI回复
      const aiMessage: Message = {
        id: Date.now(),
        type: 'staff',
        content: response.data.reply,
        time: new Date().toLocaleTimeString('zh-CN', {
          hour12: false,
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
        }),
        senderName: 'AI客服',
      }

      messages.value.push(aiMessage)
      await scrollToBottom()

      // 发送到实时聊天系统
      const realtimeResponse = await sendRealTimeMessage(aiReplyRequest)
      if (realtimeResponse.success) {
        console.log('客服页面：AI回复已发送到实时聊天系统')
      } else {
        console.error('客服页面：AI回复发送失败:', realtimeResponse.message)
      }
    } else {
      console.error('客服页面：AI回复生成失败:', response.message)
      ElMessage.error('AI回复生成失败')
    }
  } catch (error) {
    console.error('客服页面：AI回复生成异常:', error)
    ElMessage.error('AI服务暂时不可用')
  } finally {
    isAiReplying.value = false
  }
}

// 轮询新消息
const pollMessages = async () => {
  try {
    // 首先检查是否有更新的活跃聊天室
    const roomsResponse = await getActiveChatRooms()
    if (roomsResponse.success && roomsResponse.data && roomsResponse.data.length > 0) {
      const latestRoom = roomsResponse.data[0] // 获取最新的活跃聊天室

      // 如果发现新的聊天室，切换到新聊天室
      if (!roomId.value || roomId.value !== latestRoom.roomId) {
        console.log('客服页面：发现新的活跃聊天室，切换到:', latestRoom)
        roomId.value = latestRoom.roomId
        selectedRoomId.value = latestRoom.roomId

        // 加载新聊天室的消息历史
        await loadRoomMessages(roomId.value)
        lastPollTime.value = new Date().toISOString()

        ElMessage.success(`已切换到新聊天室：${latestRoom.guestName}`)
        return // 本次轮询结束，下次轮询会处理新消息
      }
    } else if (!roomId.value) {
      // 没有活跃聊天室且当前也没有roomId
      console.log('客服页面：暂无活跃聊天室，继续轮询等待新聊天室...')
      return
    }

    console.log('客服页面：轮询新消息中...', {
      roomId: roomId.value,
      lastPollTime: lastPollTime.value,
    })

    const response = await pollNewMessages(roomId.value, lastPollTime.value)

    if (response.success && response.data && response.data.length > 0) {
      console.log('客服页面：收到轮询响应:', response.data)
      let hasNewMessages = false

      response.data.forEach(async (msg: RealTimeChatResponse) => {
        console.log('客服页面：处理消息:', msg)
        // 只添加不是当前客服发送的消息（即住户消息）
        if (msg.senderType === SenderType.GUEST) {
          // 检查是否已存在该消息，避免重复添加
          const exists = messages.value.find((m) => m.id === msg.messageId)
          if (!exists) {
            const guestMessage: Message = {
              id: msg.messageId,
              type: 'guest',
              content: msg.messageContent,
              time: new Date(msg.sentAt).toLocaleTimeString('zh-CN', {
                hour12: false,
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
              }),
              senderName: msg.senderName,
            }

            console.log('客服页面：添加住户新消息:', guestMessage)
            messages.value.push(guestMessage)
            hasNewMessages = true

            // 触发AI自动回复
            if (autoReplyEnabled.value) {
              console.log('客服页面：触发AI自动回复，住户消息:', msg.messageContent)
              // 使用setTimeout避免阻塞当前循环
              setTimeout(() => {
                generateAIReply(msg.messageContent)
              }, 1000) // 延迟1秒回复，显得更自然
            }
          } else {
            console.log('客服页面：消息已存在，跳过 - ID:', msg.messageId)
          }
        } else {
          console.log('客服页面：跳过客服自己的消息')
        }
      })

      if (hasNewMessages) {
        await scrollToBottom()
      }

      // 无论是否有新消息，都更新轮询时间为当前时间
      lastPollTime.value = new Date().toISOString()
      console.log('客服页面：更新最后轮询时间:', lastPollTime.value)
    } else {
      console.log('客服页面：轮询未获取到新消息')
      // 即使没有新消息，也要更新轮询时间，避免重复获取相同的旧消息
      lastPollTime.value = new Date().toISOString()
    }
  } catch (error) {
    console.error('客服页面：轮询消息失败:', error)
    // 网络错误时不影响下次轮询
  }
}

// 开始轮询
const startPolling = () => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
  }

  console.log('客服页面：开始轮询...')
  pollingInterval.value = window.setInterval(() => {
    pollMessages()
  }, 1000) // 每1秒轮询一次，提高性能
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

const handleEnterKey = (event: KeyboardEvent) => {
  if (!event.ctrlKey) {
    sendMessage()
  }
}

const insertNewline = () => {
  inputMessage.value += '\n'
}

const clearChat = () => {
  messages.value = []
  ElMessage.success('对话已清空')
}

const goHome = () => {
  router.push('/')
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

// 初始化聊天室
const initializeChat = async () => {
  try {
    console.log('客服页面：正在初始化聊天室...')

    // 加载活跃聊天室
    const response = await getActiveChatRooms()
    if (response.success && response.data && response.data.length > 0) {
      // 选择第一个活跃的聊天室
      const firstRoom = response.data[0]
      roomId.value = firstRoom.roomId
      selectedRoomId.value = firstRoom.roomId
      console.log('客服页面：加载聊天室成功:', firstRoom)

      // 加载消息历史
      await loadRoomMessages(roomId.value)
      lastPollTime.value = new Date().toISOString()

      ElMessage.success(`已连接到聊天室：${firstRoom.guestName}`)
    } else {
      console.log('客服页面：暂无活跃聊天室，启动轮询监听新聊天室')
      ElMessage.info('等待住户消息...')
      lastPollTime.value = new Date().toISOString()
    }

    // 无论是否有现有聊天室，都要启动轮询以监听新消息和新聊天室
    startPolling()
  } catch (error) {
    console.error('客服页面：初始化失败:', error)
    ElMessage.error('连接聊天系统失败')
    serviceAvailable.value = false

    // 即使初始化失败，也要启动轮询尝试恢复连接
    setTimeout(() => {
      serviceAvailable.value = true
      startPolling()
    }, 3000)
  }
}

// 生命周期
onMounted(() => {
  loadChannels()
  initializeChat()
  // 设置页面标题
  document.title = '乐迪酒店管理系统 - 客服'
})

onUnmounted(() => {
  // 停止轮询
  stopPolling()
  // 恢复页面标题
  document.title = '乐迪酒店管理系统'
})
</script>

<style scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 页面头部 */
.chat-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 16px 24px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.brand-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-icon {
  font-size: 32px;
  color: #4f7bff;
}

.brand-title {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.brand-subtitle {
  background: linear-gradient(45deg, #4f7bff, #667eea);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-size: 14px;
  font-weight: 500;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.ai-status {
  font-size: 12px;
  color: #4f7bff;
  font-weight: 500;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

/* 聊天容器 */
.chat-container {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 20px;
  overflow: hidden;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

/* 会话列表侧边栏 */
.conversation-sidebar {
  width: 320px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.sidebar-actions {
  display: flex;
  gap: 8px;
}

.sidebar-actions .el-button {
  color: #666;
  font-size: 13px;
}

.search-box {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.search-box .el-input {
  width: 100%;
}

.filter-bar {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  gap: 8px;
}

.filter-bar .el-select {
  flex: 1;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  color: #ddd;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.chat-main {
  flex: 1;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 消息区域 */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: linear-gradient(180deg, #fafbfc 0%, #f8f9fa 100%);
}

.welcome-message {
  text-align: center;
  padding: 40px 20px;
}

.welcome-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(45deg, #4f7bff, #667eea);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.welcome-avatar .el-icon {
  font-size: 28px;
  color: white;
}

.welcome-content h3 {
  color: #2c3e50;
  font-size: 20px;
  margin: 0 0 16px;
}

.welcome-content p {
  color: #666;
  margin: 0 0 16px;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0 0 20px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(79, 123, 255, 0.1);
  border-radius: 8px;
  color: #4f7bff;
  font-size: 14px;
}

.welcome-tip {
  color: #888;
  font-style: italic;
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

.message-item.system .message-content {
  justify-content: flex-start;
}

.message-item.user .message-content {
  justify-content: flex-end;
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

/* 住户消息容器 - 左对齐 */
.message-item.guest .message-content {
  justify-content: flex-start;
}

/* 客服消息容器 - 右对齐 */
.message-item.staff .message-content {
  justify-content: flex-end;
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
}

.avatar-icon {
  color: white;
  font-size: 16px;
}

.guest-message-content {
  background: white;
  padding: 14px 16px;
  border-radius: 16px 16px 16px 4px;
  font-size: 14px;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  flex: 1;
  word-wrap: break-word;
  word-break: break-word;
  white-space: normal;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
}

.staff-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
}

.message-bubble {
  background: #4f7bff;
  color: white;
  padding: 14px 16px;
  border-radius: 16px 16px 4px 16px;
  max-width: 75%;
  font-size: 14px;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(79, 123, 255, 0.2);
  word-wrap: break-word;
  word-break: break-word;
  white-space: normal;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
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

/* 输入区域 */
.input-area {
  background: white;
  border-top: 1px solid #e8e8e8;
  padding: 16px;
}

.input-container {
  max-width: 100%;
}

.input-tools {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
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
}

.input-box {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 8px;
}

.message-input :deep(.el-textarea__inner) {
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  padding: 12px 16px;
  transition: border-color 0.2s ease;
  background: #fafafa;
}

.message-input :deep(.el-textarea__inner):focus {
  border-color: #4f7bff;
  background: white;
  box-shadow: 0 0 0 3px rgba(79, 123, 255, 0.1);
}

.send-btn {
  background: #4f7bff;
  border-color: #4f7bff;
  height: 48px;
  width: 48px;
  border-radius: 12px;
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

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #888;
}

.service-status {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #34c759;
}

.service-status.offline {
  color: #ff3b30;
}

/* 滚动条样式 */
.messages-area::-webkit-scrollbar {
  width: 6px;
}

.messages-area::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 3px;
}

.messages-area::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.messages-area::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-container {
    padding: 10px;
  }

  .chat-main {
    max-width: 100%;
    border-radius: 12px;
  }

  .header-content {
    padding: 0 16px;
  }

  .brand-title {
    font-size: 20px;
  }

  .feature-list {
    grid-template-columns: 1fr;
  }
}
</style>
