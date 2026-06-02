<template>
  <div class="daily-task">
    <header class="task-header">
      <div class="header-left">
        <h2 class="header-title">{{ t('pages.housekeepingDailyTask.title') }}</h2>
        <el-icon class="header-icon"><Calendar /></el-icon>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          :placeholder="t('pages.housekeepingDailyTask.datePlaceholder')"
          class="date-picker"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
        />
      </div>
      <el-button class="refresh-btn" circle @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </header>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        :placeholder="t('pages.housekeepingDailyTask.searchPlaceholder')"
        class="search-input"
        :prefix-icon="Search"
      />
      <el-button class="filter-btn" @click="handleFilter">
        <el-icon><Filter /></el-icon>
        {{ t('pages.housekeepingDailyTask.filter') }}
      </el-button>
    </div>

    <div class="statistics-bar">
      <p class="statistics-text">
        {{
          t('pages.housekeepingDailyTask.statistics.totalCompleted', {
            total: totalTasks,
            completed: completedTasks,
          })
        }}
      </p>
      <p class="statistics-text">
        {{
          t('pages.housekeepingDailyTask.statistics.expiredRemaining', {
            expired: expiredTasks,
            remaining: remainingTasks,
          })
        }}
      </p>
    </div>

    <div class="task-list">
      <div v-for="task in taskList" :key="task.id" class="task-card" @click="handleTaskClick(task)">
        <div class="task-status" :class="task.status">
          {{ getStatusText(task.status) }}
        </div>
        <div class="task-content">
          <h3 class="task-type">{{ getTaskTypeText(task.type) }}</h3>
          <p class="task-room">{{ getRoomTypeText(task.roomType) }}</p>
          <p class="task-room-number">{{ task.roomNumber }}</p>
          <p class="task-time">
            {{ t('pages.housekeepingDailyTask.cleanTime', { time: task.cleanTime || '-' }) }}
          </p>
        </div>
      </div>

      <el-empty
        v-if="taskList.length === 0"
        :description="t('pages.housekeepingDailyTask.empty')"
      />
    </div>

    <footer class="task-footer">
      <div class="footer-logo">
        <el-icon :size="24"><House /></el-icon>
        <span class="footer-text">{{ t('pages.housekeepingDailyTask.footer') }}</span>
      </div>
    </footer>

    <TaskDetailDialog v-model="showTaskDetail" :task="selectedTask" @refresh="handleRefresh" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Calendar, Filter, House, Refresh, Search } from '@element-plus/icons-vue'
import TaskDetailDialog from './components/TaskDetailDialog.vue'
import { getStoreTodayYmd } from '@/utils/storeDateTime'

const { t } = useI18n()

const selectedDate = ref(getStoreTodayYmd())
const searchKeyword = ref('')
const showTaskDetail = ref(false)
const selectedTask = ref<any>(null)

const taskList = ref([
  {
    id: 1,
    type: 'checkoutRoom',
    roomType: 'deluxeDouble',
    roomNumber: 'a01',
    cleanTime: '',
    status: 'expired',
    duration: 0,
    note: '',
  },
])

const totalTasks = computed(() => taskList.value.length)
const completedTasks = computed(
  () => taskList.value.filter((item) => item.status === 'completed').length,
)
const expiredTasks = computed(
  () => taskList.value.filter((item) => item.status === 'expired').length,
)
const remainingTasks = computed(
  () => taskList.value.filter((item) => item.status === 'pending').length,
)

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    pending: t('pages.housekeepingDailyTask.statuses.pending'),
    completed: t('pages.housekeepingDailyTask.statuses.completed'),
    expired: t('pages.housekeepingDailyTask.statuses.expired'),
    cleaning: t('pages.housekeepingDailyTask.statuses.cleaning'),
  }
  return statusMap[status] || status
}

const getTaskTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    checkoutRoom: t('pages.housekeepingDailyTask.taskTypes.checkoutRoom'),
  }
  return typeMap[type] || type
}

const getRoomTypeText = (roomType: string) => {
  const roomTypeMap: Record<string, string> = {
    deluxeDouble: t('pages.housekeepingDailyTask.roomTypes.deluxeDouble'),
  }
  return roomTypeMap[roomType] || roomType
}

const handleRefresh = () => {
  ElMessage.success(t('pages.housekeepingDailyTask.messages.refreshSuccess'))
}

const handleFilter = () => {
  ElMessage.info(t('pages.housekeepingDailyTask.messages.filterPending'))
}

const handleTaskClick = (task: any) => {
  selectedTask.value = task
  showTaskDetail.value = true
}

onMounted(() => {
  console.log('daily task mounted')
})
</script>

<style scoped>
.daily-task {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.task-header {
  background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
}

.header-icon {
  font-size: 20px;
}

.date-picker {
  width: 160px;
}

.date-picker :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: none;
}

.date-picker :deep(.el-input__inner) {
  color: white;
}

.date-picker :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.7);
}

.refresh-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
}

.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.search-bar {
  padding: 16px 20px;
  background: white;
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
}

.filter-btn {
  background: white;
  border: 1px solid #ddd;
}

.statistics-bar {
  background: #f0f0f0;
  padding: 12px 20px;
  text-align: center;
}

.statistics-text {
  font-size: 14px;
  color: #666;
  margin: 4px 0;
}

.task-list {
  flex: 1;
  padding: 16px 20px;
  overflow-y: auto;
}

.task-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.task-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.task-status {
  width: 60px;
  background: #95a5a6;
  color: white;
  font-size: 14px;
  font-weight: bold;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  writing-mode: vertical-rl;
  text-orientation: upright;
  letter-spacing: 4px;
  padding: 8px 0;
  flex-shrink: 0;
}

.task-status.pending {
  background: #3498db;
}

.task-status.completed {
  background: #27ae60;
}

.task-status.expired {
  background: #95a5a6;
}

.task-content {
  flex: 1;
}

.task-type {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px 0;
}

.task-room {
  font-size: 14px;
  color: #666;
  margin: 4px 0;
}

.task-room-number {
  font-size: 14px;
  color: #666;
  margin: 4px 0;
}

.task-time {
  font-size: 13px;
  color: #999;
  margin: 8px 0 0 0;
}

.task-footer {
  padding: 20px;
  text-align: center;
  background: white;
  border-top: 1px solid #f0f0f0;
}

.footer-logo {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #ccc;
  font-size: 14px;
}

.footer-text {
  color: #ccc;
}

@media (max-width: 768px) {
  .task-header {
    padding: 12px 16px;
  }

  .header-title {
    font-size: 16px;
  }

  .date-picker {
    width: 140px;
  }

  .search-bar {
    padding: 12px 16px;
  }

  .statistics-bar {
    padding: 10px 16px;
  }

  .task-list {
    padding: 12px 16px;
  }

  .task-card {
    padding: 12px;
  }

  .task-status {
    width: 50px;
    font-size: 13px;
  }
}
</style>
