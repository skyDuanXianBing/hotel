<template>
  <div class="task-detail-container">
    <!-- 顶部返回 -->
    <div class="header">
      <el-icon class="back-icon" @click="handleBack"><ArrowLeft /></el-icon>
      <span class="title">任务详情</span>
    </div>

    <!-- 任务信息 -->
    <div v-if="task" class="task-info">
      <!-- 酒店和房间 -->
      <div class="section">
        <div class="hotel-info">
          <el-icon class="hotel-icon"><OfficeBuilding /></el-icon>
          <span class="hotel-name">{{ task.roomType || '标准房' }}</span>
        </div>
      </div>

      <!-- 房型 -->
      <div class="section">
        <div class="label">房型</div>
        <div class="value">{{ task.roomNumber || '-' }}</div>
      </div>

      <!-- 房间 -->
      <div class="section">
        <div class="label">房间</div>
        <div class="value">{{ task.roomNumber || '-' }}</div>
      </div>

      <!-- 任务状态 -->
      <div class="section">
        <div class="label">任务状态</div>
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
        <div class="label">任务类型</div>
        <div class="value">{{ getTaskTypeText(task.taskType) }}</div>
      </div>

      <!-- 任务时间 -->
      <div class="section">
        <div class="label">任务时间</div>
        <div class="value">{{ task.estimatedTime || '10:00-16:00' }}</div>
      </div>

      <!-- 任务通知 -->
      <div class="section">
        <div class="label">任务通知</div>
        <div class="value">-</div>
      </div>

      <!-- 下个订单 -->
      <div class="section">
        <div class="label">下个订单</div>
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
        拒绝
      </el-button>
      <el-button
        v-if="task && task.status === 'assigned'"
        type="primary"
        size="large"
        class="action-button accept-button"
        @click="handleAccept"
      >
        接受
      </el-button>
      <el-button
        v-if="task && task.status === 'in_progress'"
        type="primary"
        size="large"
        class="action-button complete-button"
        @click="handleComplete"
      >
        打扫完成
      </el-button>
    </div>

    <!-- Loading -->
    <el-loading v-if="loading" fullscreen />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
const cleanerUser = readCleanerUser()

const loading = ref(false)
const task = ref<CleaningTaskDTO | null>(null)

// 获取任务详情
const loadTaskDetail = async () => {
  const taskId = route.params.id as string
  if (!taskId) {
    ElMessage.error('任务ID无效')
    return
  }

  try {
    loading.value = true
    const response = await getCleaningTaskById(Number(taskId))

    if (response.success && response.data) {
      task.value = response.data
    } else {
      ElMessage.error(response.message || '获取任务详情失败')
    }
  } catch (error) {
    console.error('加载任务详情失败:', error)
    ElMessage.error('加载任务详情失败')
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
      ElMessage.success('已接受任务')
      // 更新任务状态
      task.value.status = 'in_progress'
      task.value.startTime = new Date().toISOString()
    } else {
      ElMessage.error(response.message || '接受任务失败')
    }
  } catch (error) {
    console.error('接受任务失败:', error)
    ElMessage.error('接受任务失败')
  } finally {
    loading.value = false
  }
}

// 拒绝任务
const handleReject = async () => {
  if (!task.value) return

  try {
    await ElMessageBox.confirm('确定要拒绝这个任务吗?', '确认拒绝', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    loading.value = true
    const response = await rejectCleaningTask(task.value.id)

    if (response.success) {
      ElMessage.success('已拒绝任务')
      router.back()
    } else {
      ElMessage.error(response.message || '拒绝任务失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('拒绝任务失败:', error)
      ElMessage.error('拒绝任务失败')
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
      ElMessage.success('打扫完成')
      // 更新任务状态
      task.value.status = 'completed'
      task.value.completeTime = new Date().toISOString()

      // 延迟返回
      setTimeout(() => {
        router.back()
      }, 1500)
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    console.error('完成任务失败:', error)
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}

// 获取状态文本
const getStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    expired: '已过期',
    pending: '待分配',
    assigned: '待接受',
    in_progress: '待打扫',
    completed: '已完成',
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
    checkout: '退房清洁',
    daily: '日常清洁',
    deep: '深度清洁',
  }
  return typeMap[taskType] || taskType
}

onMounted(() => {
  if (!cleanerUser) {
    ElMessage.error('登录状态已失效，请重新登录')
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
