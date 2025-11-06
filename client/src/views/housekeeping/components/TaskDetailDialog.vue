<template>
  <el-dialog
    v-model="dialogVisible"
    :show-close="false"
    width="90%"
    class="task-detail-dialog"
    :fullscreen="isMobile"
  >
    <template #header>
      <div class="dialog-header">
        <h2 class="dialog-title">{{ task?.type || '抹尘' }}</h2>
      </div>
    </template>

    <div class="dialog-content">
      <!-- 房间信息 -->
      <div class="room-info">
        <p class="room-type">{{ task?.roomType || '大床房' }}</p>
        <p class="room-number">{{ task?.roomNumber || 'a01' }}</p>
      </div>

      <!-- 状态圆环 -->
      <div class="status-circle">
        <div class="circle-outer" :class="task?.status">
          <div class="circle-inner">
            <div class="status-content">
              <p class="status-text" :class="task?.status">
                {{ getStatusText(task?.status) }}
              </p>
              <p class="duration-text">总时长：{{ formatDuration(task?.duration) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 打扫时间 -->
      <div class="detail-item">
        <span class="detail-label">打扫时间</span>
        <span class="detail-value">{{ task?.cleanTime || '-' }}</span>
      </div>

      <!-- 备注 -->
      <div class="detail-item">
        <span class="detail-label">备注</span>
        <span class="detail-value">{{ task?.note || '-' }}</span>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons" v-if="task?.status === 'pending'">
        <el-button type="success" size="large" class="action-btn" @click="handleStartClean">
          开始打扫
        </el-button>
      </div>

      <div class="action-buttons" v-if="task?.status === 'cleaning'">
        <el-button type="primary" size="large" class="action-btn" @click="handleFinishClean">
          完成打扫
        </el-button>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { ElMessage } from 'element-plus'

interface Task {
  id: number
  type: string
  roomType: string
  roomNumber: string
  cleanTime: string
  status: string
  duration: number
  note: string
}

interface Props {
  modelValue: boolean
  task: Task | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'refresh'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

// 检测是否为移动端
const isMobile = ref(window.innerWidth <= 768)

// 获取状态文本
const getStatusText = (status?: string) => {
  const statusMap: Record<string, string> = {
    pending: '待打扫',
    cleaning: '打扫中',
    completed: '已打扫',
    expired: '已过期',
  }
  return statusMap[status || ''] || '待打扫'
}

// 格式化时长
const formatDuration = (seconds?: number) => {
  if (!seconds) return '00:00:00'

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

// 开始打扫
const handleStartClean = () => {
  ElMessage.success('开始打扫')
  // TODO: 调用API开始计时
  emit('refresh')
  handleClose()
}

// 完成打扫
const handleFinishClean = () => {
  ElMessage.success('打扫完成')
  // TODO: 调用API完成任务
  emit('refresh')
  handleClose()
}

// 关闭弹窗
const handleClose = () => {
  dialogVisible.value = false
}

// 监听窗口大小变化
watch(
  () => window.innerWidth,
  (width) => {
    isMobile.value = width <= 768
  }
)
</script>

<style scoped>
.task-detail-dialog :deep(.el-dialog__header) {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.task-detail-dialog :deep(.el-dialog__body) {
  padding: 30px 20px;
}

.task-detail-dialog :deep(.el-dialog__footer) {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
}

.dialog-header {
  text-align: center;
}

.dialog-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.dialog-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

/* 房间信息 */
.room-info {
  text-align: center;
}

.room-type {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin: 0 0 8px 0;
}

.room-number {
  font-size: 16px;
  color: #666;
  margin: 0;
}

/* 状态圆环 */
.status-circle {
  width: 100%;
  max-width: 300px;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 20px 0;
}

.circle-outer {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 12px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.circle-outer.pending {
  border-color: #e0e0e0;
}

.circle-outer.cleaning {
  border-color: #3498db;
}

.circle-outer.completed {
  border-color: #a8d5a8;
}

.circle-outer.expired {
  border-color: #95a5a6;
}

.circle-inner {
  width: 85%;
  height: 85%;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.status-content {
  text-align: center;
}

.status-text {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0 0 12px 0;
}

.status-text.completed {
  color: #27ae60;
}

.status-text.cleaning {
  color: #3498db;
}

.status-text.expired {
  color: #95a5a6;
}

.duration-text {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* 详情项 */
.detail-item {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item:last-of-type {
  border-bottom: none;
}

.detail-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: #333;
}

/* 操作按钮 */
.action-buttons {
  width: 100%;
  margin-top: 20px;
}

.action-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dialog-title {
    font-size: 20px;
  }

  .room-type {
    font-size: 16px;
  }

  .room-number {
    font-size: 14px;
  }

  .status-circle {
    max-width: 250px;
  }

  .status-text {
    font-size: 24px;
  }

  .duration-text {
    font-size: 13px;
  }

  .circle-outer {
    border-width: 10px;
  }
}
</style>
