<template>
  <div class="task-detail-container">
    <!-- 顶部返回 -->
    <div class="header">
      <el-icon class="back-icon" @click="handleBack"><ArrowLeft /></el-icon>
      <span class="title">{{ t('stage5.cleaner.dashboard.taskDetails') }}</span>
    </div>

    <!-- 任务信息 -->
    <div v-if="task" class="task-info">
      <!-- 酒店和房间 -->
      <div class="section">
        <div class="hotel-info">
          <el-icon class="hotel-icon"><OfficeBuilding /></el-icon>
          <span class="hotel-name">{{ task.roomType || t('stage5.cleaner.dashboard.defaultRoomType') }}</span>
        </div>
      </div>

      <!-- 房型 -->
      <div class="section">
        <div class="label">{{ t('stage5.cleaner.dashboard.roomType') }}</div>
        <div class="value">{{ task.roomNumber || '-' }}</div>
      </div>

      <!-- 房间 -->
      <div class="section">
        <div class="label">{{ t('stage5.cleaner.dashboard.roomNumber') }}</div>
        <div class="value">{{ task.roomNumber || '-' }}</div>
      </div>

      <!-- 任务状态 -->
      <div class="section">
        <div class="label">{{ t('stage5.cleaner.dashboard.taskStatus') }}</div>
        <div class="value">
          <el-tag
            :type="getStatusType(task.status)"
            size="large"
          >
            {{ getStatusText(task.status) }}
          </el-tag>
        </div>
      </div>

      <!-- 任务类型 -->
      <div class="section">
        <div class="label">{{ t('stage5.cleaner.dashboard.taskType') }}</div>
        <div class="value">{{ getTaskTypeText(task.taskType) }}</div>
      </div>

      <!-- 任务时间 -->
      <div class="section">
        <div class="label">{{ t('stage5.cleaner.dashboard.taskTime') }}</div>
        <div class="value">{{ task.estimatedTime || '10:00-16:00' }}</div>
      </div>

      <!-- 任务通知 -->
      <div class="section">
        <div class="label">{{ t('stage5.cleaner.dashboard.taskNotice') }}</div>
        <div class="value">-</div>
      </div>

      <!-- 下个订单 -->
      <div class="section">
        <div class="label">{{ t('stage5.cleaner.dashboard.nextOrder') }}</div>
        <div class="value">
          <div v-if="task.notes" class="order-info">
            {{ task.notes }}
          </div>
          <div v-else class="empty-text">-</div>
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="footer-actions">
      <el-button
        v-if="task && task.status === 'assigned'"
        type="danger"
        size="large"
        class="action-button reject-button"
        @click="handleReject"
      >
        {{ t('stage5.cleaner.dashboard.reject') }}
      </el-button>
      <el-button
        v-if="task && task.status === 'assigned'"
        type="primary"
        size="large"
        class="action-button accept-button"
        @click="handleAccept"
      >
        {{ t('stage5.cleaner.dashboard.accept') }}
      </el-button>
      <el-button
        v-if="task && task.status === 'in_progress'"
        type="primary"
        size="large"
        class="action-button complete-button"
        @click="handleComplete"
      >
        {{ t('stage5.cleaner.dashboard.cleaningCompleted') }}
      </el-button>
    </div>

    <!-- Loading -->
    <el-loading v-if="loading" fullscreen />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, OfficeBuilding } from '@element-plus/icons-vue'
import {
  getCleaningTaskById,
  acceptCleaningTask,
  completeCleaningTask,
  rejectCleaningTask,
  type CleaningTaskDTO,
} from '@/api/cleaning'
import { readCleanerUser } from '@/utils/cleanerSession'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const cleanerUser = readCleanerUser()

const loading = ref(false)
const task = ref<CleaningTaskDTO | null>(null)

// 获取任务详情
const loadTaskDetail = async () => {
  const taskId = route.params.id as string
  if (!taskId) {
    ElMessage.error(t('stage5.cleaner.dashboard.invalidTaskId'))
    return
  }

  try {
    loading.value = true
    const response = await getCleaningTaskById(Number(taskId))

    if (response.success && response.data) {
      task.value = response.data
    } else {
      ElMessage.error(response.message || t('stage5.cleaner.dashboard.loadTaskDetailFailed'))
    }
  } catch (error) {
    console.error('Failed to load cleaner task detail:', error)
    ElMessage.error(t('stage5.cleaner.dashboard.loadTaskDetailFailed'))
  } finally {
    loading.value = false
  }
}

// 返回
const handleBack = () => {
  router.back()
}

// 接受任务
const handleAccept = async () => {
  if (!task.value) return

  try {
    loading.value = true
    const response = await acceptCleaningTask(task.value.id)

    if (response.success) {
      ElMessage.success(t('stage5.cleaner.dashboard.acceptedTask'))
      // 更新任务状态
      task.value.status = 'in_progress'
      task.value.startTime = new Date().toISOString()
    } else {
      ElMessage.error(response.message || t('stage5.cleaner.dashboard.actionFailed'))
    }
  } catch (error) {
    console.error('Failed to accept cleaning task:', error)
    ElMessage.error(t('stage5.cleaner.dashboard.actionFailed'))
  } finally {
    loading.value = false
  }
}

// 拒绝任务
const handleReject = async () => {
  if (!task.value) return

  try {
    await ElMessageBox.confirm(t('stage5.cleaner.dashboard.confirmRejectMessage'), t('stage5.cleaner.dashboard.confirmRejectTitle'), {
      confirmButtonText: t('stage5.cleaner.dashboard.confirmButton'),
      cancelButtonText: t('stage5.common.actions.cancel'),
      type: 'warning',
    })

    loading.value = true
    const response = await rejectCleaningTask(task.value.id)

    if (response.success) {
      ElMessage.success(t('stage5.cleaner.dashboard.rejectedTask'))
      router.back()
    } else {
      ElMessage.error(response.message || t('stage5.cleaner.dashboard.actionFailed'))
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to reject cleaning task:', error)
      ElMessage.error(t('stage5.cleaner.dashboard.actionFailed'))
    }
  } finally {
    loading.value = false
  }
}

// 完成打扫
const handleComplete = async () => {
  if (!task.value) return

  try {
    loading.value = true
    const response = await completeCleaningTask(task.value.id)

    if (response.success) {
      ElMessage.success(t('stage5.cleaner.dashboard.completedCleaning'))
      // 更新任务状态
      task.value.status = 'completed'
      task.value.completeTime = new Date().toISOString()

      // 延迟返回
      setTimeout(() => {
        router.back()
      }, 1500)
    } else {
      ElMessage.error(response.message || t('stage5.cleaner.dashboard.actionFailed'))
    }
  } catch (error) {
    console.error('Failed to complete cleaning task:', error)
    ElMessage.error(t('stage5.cleaner.dashboard.actionFailed'))
  } finally {
    loading.value = false
  }
}

// 获取状态文本
const getStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    expired: t('stage5.cleaner.dashboard.status.expired'),
    pending: t('stage5.cleaner.dashboard.status.pending'),
    assigned: t('stage5.cleaner.dashboard.status.assigned'),
    in_progress: t('stage5.cleaner.dashboard.status.inProgress'),
    completed: t('stage5.cleaner.dashboard.status.completed'),
  }
  return statusMap[status] || status
}

// 获取状态类型
const getStatusType = (status: string): string => {
  const typeMap: Record<string, string> = {
    expired: 'info',
    pending: 'info',
    assigned: 'warning',
    in_progress: 'primary',
    completed: 'success',
  }
  return typeMap[status] || 'info'
}

// 获取任务类型文本
const getTaskTypeText = (taskType: string): string => {
  const typeMap: Record<string, string> = {
    checkout: t('stage5.cleaner.dashboard.taskTypes.checkout'),
    daily: t('stage5.cleaner.dashboard.taskTypes.daily'),
    deep: t('stage5.cleaner.dashboard.taskTypes.deep'),
  }
  return typeMap[taskType] || taskType
}

onMounted(() => {
  if (!cleanerUser) {
    ElMessage.error(t('stage5.cleaner.dashboard.sessionExpired'))
    router.replace('/cleaner/login')
    return
  }
  loadTaskDetail()
})
</script>

<style scoped>
.task-detail-container {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.header {
  background: white;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.back-icon {
  font-size: 20px;
  color: #409eff;
  cursor: pointer;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.task-info {
  flex: 1;
  padding: 0 20px 20px;
  overflow-y: auto;
}

.section {
  background: white;
  padding: 16px;
  margin-top: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.section:first-child {
  margin-top: 20px;
}

.hotel-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hotel-icon {
  font-size: 24px;
  color: #409eff;
}

.hotel-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.value {
  font-size: 16px;
  color: #303133;
  font-weight: 500;
}

.order-info {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.empty-text {
  color: #c0c4cc;
}

.footer-actions {
  background: white;
  padding: 16px 20px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  gap: 12px;
}

.action-button {
  flex: 1;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
}

.reject-button {
  background: white;
  color: #f56c6c;
  border: 1px solid #f56c6c;
}

.reject-button:hover {
  background: #fef0f0;
}

.accept-button {
  background: #409eff;
  border: none;
}

.accept-button:hover {
  background: #66b1ff;
}

.complete-button {
  background: #67c23a;
  border: none;
}

.complete-button:hover {
  background: #85ce61;
}
</style>
