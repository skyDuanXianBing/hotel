<template>
  <div class="notification-settings-container">
    <h2 class="page-title">通知功能</h2>

    <div class="notification-list">
      <!-- 订单消息弹框提醒 -->
      <div class="notification-item">
        <div class="notification-content">
          <h3 class="notification-title">订单消息弹框提醒</h3>
          <p class="notification-desc">非系统使用人员操作的订单增、删、变、取消时,有系统弹框提醒</p>
        </div>
        <el-switch v-model="settings.orderPopup" />
      </div>

      <!-- 订单消息声音提醒 -->
      <div class="notification-item">
        <div class="notification-content">
          <h3 class="notification-title">订单消息声音提醒</h3>
          <p class="notification-desc">非系统使用人员操作的订单增、删、变、取消时,有系统声音提醒</p>
        </div>
        <div class="notification-actions">
          <el-button link @click="testOrderSound">
            <el-icon><VideoPlay /></el-icon>
            试听提示音
          </el-button>
          <el-switch v-model="settings.orderSound" />
        </div>
      </div>

      <!-- 聊天信息弹框提醒 -->
      <div class="notification-item">
        <div class="notification-content">
          <h3 class="notification-title">聊天信息弹框提醒</h3>
          <p class="notification-desc">收到新的聊天消息时,有系统弹框提醒</p>
        </div>
        <el-switch v-model="settings.chatPopup" />
      </div>

      <!-- 聊天信息声音提醒 -->
      <div class="notification-item">
        <div class="notification-content">
          <h3 class="notification-title">聊天信息声音提醒</h3>
          <p class="notification-desc">收到新的聊天消息时,有系统声音提醒</p>
        </div>
        <div class="notification-actions">
          <el-button link @click="testChatSound">
            <el-icon><VideoPlay /></el-icon>
            试听提示音
          </el-button>
          <el-switch v-model="settings.chatSound" />
        </div>
      </div>
    </div>

    <!-- 保存按钮 -->
    <div class="save-section">
      <el-button type="primary" @click="handleSave">保存设置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import {
  getNotificationSettings,
  updateNotificationSettings,
} from '@/api/notification'

interface NotificationSettings {
  orderPopup: boolean
  orderSound: boolean
  chatPopup: boolean
  chatSound: boolean
}

const loading = ref(false)
const userStore = useUserStore()
const notificationCenterStore = useNotificationCenterStore()
const settings = ref<NotificationSettings>({
  orderPopup: true,
  orderSound: true,
  chatPopup: true,
  chatSound: true,
})

// 音频对象
let orderAudio: HTMLAudioElement | null = null
let chatAudio: HTMLAudioElement | null = null

const getCurrentUserId = () => userStore.currentUser?.id

// 加载通知设置
const loadSettings = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    ElMessage.warning('未获取到用户信息，无法加载通知设置')
    return
  }
  try {
    loading.value = true
    const response = await getNotificationSettings(userId)
    if (response.success && response.data) {
      settings.value = {
        orderPopup: response.data.orderPopup,
        orderSound: response.data.orderSound,
        chatPopup: response.data.chatPopup,
        chatSound: response.data.chatSound,
      }
    } else {
      ElMessage.error(response.message || '加载通知设置失败')
    }
  } catch (error) {
    console.error('加载通知设置失败:', error)
    ElMessage.error('加载通知设置失败')
  } finally {
    loading.value = false
  }
}

// 播放订单提示音
const testOrderSound = () => {
  try {
    if (!orderAudio) {
      orderAudio = new Audio('/sounds/order-notification.mp3')
    }
    orderAudio.play()
    ElMessage.success('正在播放订单提示音')
  } catch (error) {
    console.error('播放订单提示音失败:', error)
    ElMessage.warning('音频文件未找到,请确保已将音频文件放置在 public/sounds/ 目录下')
  }
}

// 播放聊天提示音
const testChatSound = () => {
  try {
    if (!chatAudio) {
      chatAudio = new Audio('/sounds/chat-notification.mp3')
    }
    chatAudio.play()
    ElMessage.success('正在播放聊天提示音')
  } catch (error) {
    console.error('播放聊天提示音失败:', error)
    ElMessage.warning('音频文件未找到,请确保已将音频文件放置在 public/sounds/ 目录下')
  }
}

// 保存设置
const handleSave = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    ElMessage.warning('未获取到用户信息，无法保存通知设置')
    return
  }
  try {
    loading.value = true
    const snapshot = {
      orderPopup: settings.value.orderPopup,
      orderSound: settings.value.orderSound,
      chatPopup: settings.value.chatPopup,
      chatSound: settings.value.chatSound,
    }
    const response = await updateNotificationSettings(userId, snapshot)

    if (response.success) {
      notificationCenterStore.applySettingsSnapshot(snapshot)
      ElMessage.success('设置已保存')
    } else {
      ElMessage.error(response.message || '保存设置失败')
    }
  } catch (error) {
    console.error('保存设置失败:', error)
    ElMessage.error('保存设置失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (!userStore.currentUser?.id) {
    try {
      await userStore.fetchCurrentUser()
    } catch (error) {
      console.warn('初始化用户信息失败:', error)
    }
  }
  void loadSettings()
})
</script>

<style scoped>
.notification-settings-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 24px 0;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notification-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.notification-content {
  flex: 1;
}

.notification-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin: 0 0 8px 0;
}

.notification-desc {
  font-size: 14px;
  color: #606266;
  margin: 0;
  line-height: 1.5;
}

.notification-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notification-actions .el-button {
  font-size: 14px;
  color: #409eff;
}

.notification-actions .el-icon {
  margin-right: 4px;
}

.save-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e4e7ed;
}
</style>
