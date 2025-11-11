<template>
  <Teleport to="body">
    <div class="notification-container">
      <TransitionGroup name="notification">
        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-popup"
          @click="handleNotificationClick(notification)"
        >
          <div class="notification-header">
            <div class="notification-title">
              <el-icon class="title-icon"><BellFilled /></el-icon>
              <span>{{ notification.title }}</span>
            </div>
            <el-button
              link
              class="close-btn"
              @click.stop="closeNotification(notification.id)"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>

          <div class="notification-body">
            <div class="notification-channel">
              <span class="channel-label">渠道:</span>
              <span class="channel-value">{{ notification.channel }}</span>
            </div>
            <div class="notification-info">
              <div class="info-row">
                <span class="info-label">客人:</span>
                <span class="info-value">{{ notification.guestName }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">订单号:</span>
                <span class="info-value">{{ notification.orderNumber }}</span>
              </div>
            </div>
          </div>

          <div class="notification-footer">
            <span class="notification-time">{{ formatTime(notification.time) }}</span>
          </div>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { BellFilled, Close } from '@element-plus/icons-vue'

export interface Notification {
  id: string
  title: string
  channel: string
  guestName: string
  orderNumber: string
  time: Date
  type: 'order' | 'message'
  onClick?: () => void
}

const notifications = ref<Notification[]>([])

// 添加通知
const addNotification = (notification: Notification) => {
  notifications.value.push(notification)

  // 5秒后自动关闭
  setTimeout(() => {
    closeNotification(notification.id)
  }, 5000)
}

// 关闭通知
const closeNotification = (id: string) => {
  const index = notifications.value.findIndex((n) => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

// 点击通知
const handleNotificationClick = (notification: Notification) => {
  if (notification.onClick) {
    notification.onClick()
  }
  closeNotification(notification.id)
}

// 格式化时间
const formatTime = (time: Date) => {
  const hours = time.getHours().toString().padStart(2, '0')
  const minutes = time.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

// 暴露方法供外部调用
defineExpose({
  addNotification,
  closeNotification,
})
</script>

<style scoped>
.notification-container {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 360px;
  pointer-events: none;
}

.notification-popup {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  cursor: pointer;
  pointer-events: auto;
  min-width: 320px;
  transition: all 0.3s ease;
}

.notification-popup:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  transform: translateY(-2px);
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.notification-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.title-icon {
  font-size: 18px;
}

.close-btn {
  color: #fff;
  padding: 4px;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.notification-body {
  padding: 16px;
}

.notification-channel {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.channel-label {
  font-size: 13px;
  color: #909399;
  margin-right: 8px;
}

.channel-value {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.notification-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.info-label {
  color: #909399;
  min-width: 60px;
}

.info-value {
  color: #303133;
  font-weight: 500;
}

.notification-footer {
  padding: 8px 16px;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

/* 动画效果 */
.notification-enter-active {
  animation: slideIn 0.3s ease;
}

.notification-leave-active {
  animation: slideOut 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes slideOut {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(100%);
    opacity: 0;
  }
}
</style>
