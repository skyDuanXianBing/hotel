<template>
  <div class="cleaner-dashboard">
    <!-- 顶部导航栏 -->
    <div class="top-bar">
      <div class="logo">{{ t('stage5.cleaner.common.appName') }}</div>
      <el-select
        v-if="cleanerContexts.length > 1"
        v-model="selectedContextStoreId"
        class="store-switcher"
        size="small"
        @change="handleContextChange"
      >
        <el-option
          v-for="context in cleanerContexts"
          :key="context.storeId"
          :label="context.store.name || `#${context.storeId}`"
          :value="context.storeId"
        />
      </el-select>
      <div class="user-info">
        <span class="username">{{ cleanerUser?.nickname || cleanerUser?.email || t('stage5.cleaner.common.cleaner') }}</span>
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-icon><Setting /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="canSwitchToPms" command="switch-pms">
                {{ t('layout.switchToPmsWorkspace') }}
              </el-dropdown-item>
              <el-dropdown-item command="logout">{{ t('stage5.cleaner.common.logout') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <CleanerInternalTaskPanel ref="internalTaskPanelRef" />

    <!-- 月份选择器 -->
    <div class="month-selector">
      <el-date-picker
        v-model="selectedMonth"
        type="month"
        format="YYYY-MM"
        :placeholder="t('stage5.cleaner.dashboard.monthPlaceholder')"
        @change="handleMonthChange"
      />
    </div>

    <!-- 日历视图 -->
    <div class="calendar-container">
      <!-- 星期标题 -->
      <div class="weekday-header">
        <div v-for="day in weekdays" :key="day" class="weekday-cell">
          {{ day }}
        </div>
      </div>

      <!-- 日期网格 -->
      <div class="calendar-grid">
        <div
          v-for="day in calendarDays"
          :key="day.date"
          class="day-cell"
          :class="{
            'other-month': !day.isCurrentMonth,
            'today': day.isToday,
            'selected': day.date === selectedDate,
          }"
          @click="handleDateClick(day)"
        >
          <div class="day-number">{{ day.day }}</div>
          <div v-if="hasAnyDot(day)" class="day-dots">
            <span v-if="hasStatus(day, 'assigned')" class="dot dot-assigned" />
            <span v-if="hasStatus(day, 'in_progress')" class="dot dot-in-progress" />
            <span v-if="hasStatus(day, 'pending')" class="dot dot-pending" />
            <span v-if="hasStatus(day, 'completed')" class="dot dot-completed" />
          </div>
        </div>
      </div>
    </div>

    <!-- 当日任务抽屉 -->
    <el-drawer
      v-model="dayDrawerVisible"
      :title="dayDrawerTitle"
      direction="btt"
      size="75%"
      :with-header="true"
    >
      <div class="day-drawer">
        <el-tabs v-model="activeDayStatus" stretch class="day-tabs">
          <el-tab-pane name="assigned">
            <template #label>
              <span>{{ t('stage5.cleaner.dashboard.tabs.assigned') }}</span>
              <span v-if="selectedDayStatusCount.assigned > 0" class="tab-count">
                {{ selectedDayStatusCount.assigned }}
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="in_progress">
            <template #label>
              <span>{{ t('stage5.cleaner.dashboard.tabs.inProgress') }}</span>
              <span v-if="selectedDayStatusCount.in_progress > 0" class="tab-count">
                {{ selectedDayStatusCount.in_progress }}
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="pending">
            <template #label>
              <span>{{ t('stage5.cleaner.dashboard.tabs.pending') }}</span>
              <span v-if="selectedDayStatusCount.pending > 0" class="tab-count">
                {{ selectedDayStatusCount.pending }}
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="completed">
            <template #label>
              <span>{{ t('stage5.cleaner.dashboard.tabs.completed') }}</span>
              <span v-if="selectedDayStatusCount.completed > 0" class="tab-count">
                {{ selectedDayStatusCount.completed }}
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>

        <div class="day-task-list">
          <el-empty v-if="filteredSelectedDayTasks.length === 0" :description="t('stage5.common.empty.noTasks')" />
          <div v-else class="task-items">
            <div
              v-for="task in filteredSelectedDayTasks"
              :key="task.id"
              class="day-task-item"
              @click="openTask(task)"
            >
              <div class="task-main">
                <div class="task-room">{{ t('stage5.cleaner.common.room', { room: task.roomNumber || '-' }) }}</div>
                <div class="task-meta">{{ getTaskTypeLabel(task.taskType) }}</div>
              </div>
              <div class="task-action">{{ t('stage5.common.actions.view') }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 空状态 -->
    <div v-if="!loading && totalTasks === 0" class="empty-state-inline">
      <el-icon class="empty-icon"><Document /></el-icon>
      <p class="empty-text">{{ t('stage5.common.empty.noTasks') }}</p>
    </div>

    <!-- 底部统计 -->
    <div class="status-bar">
      <div class="status-item">
        <span class="label">{{ t('stage5.cleaner.dashboard.tabs.assigned') }}</span>
        <span class="count">{{ statusCount.assigned || 0 }}</span>
      </div>
      <div class="status-item">
        <span class="label">{{ t('stage5.cleaner.dashboard.tabs.inProgress') }}</span>
        <span class="count">{{ statusCount.in_progress || 0 }}</span>
      </div>
      <div class="status-item">
        <span class="label">{{ t('stage5.cleaner.dashboard.tabs.pending') }}</span>
        <span class="count">{{ statusCount.pending || 0 }}</span>
      </div>
      <div class="status-item">
        <span class="label">{{ t('stage5.cleaner.dashboard.tabs.completed') }}</span>
        <span class="count">{{ statusCount.completed || 0 }}</span>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <div class="nav-item active">
        <el-icon><Calendar /></el-icon>
        <span>{{ t('stage5.cleaner.dashboard.navTasks') }}</span>
      </div>
      <div class="nav-item">
        <el-icon><Document /></el-icon>
        <span>{{ t('stage5.cleaner.dashboard.navLogout') }}</span>
      </div>
    </div>

    <!-- 任务详情弹窗 -->
    <el-dialog
      v-model="taskDetailVisible"
      :title="t('stage5.cleaner.dashboard.taskDetails')"
      width="90%"
      :close-on-click-modal="false"
    >
      <div v-if="selectedTask" class="task-detail">
        <!-- 基本信息 -->
        <div class="detail-section">
          <h3>{{ t('stage5.cleaner.dashboard.basicInfo') }}</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="t('stage5.cleaner.dashboard.roomNumber')">{{ selectedTask.roomNumber }}</el-descriptions-item>
            <el-descriptions-item :label="t('stage5.cleaner.dashboard.taskDate')">{{ selectedTask.taskDate }}</el-descriptions-item>
            <el-descriptions-item :label="t('stage5.cleaner.dashboard.taskType')">{{ getTaskTypeLabel(selectedTask.taskType) }}</el-descriptions-item>
            <el-descriptions-item :label="t('stage5.cleaner.dashboard.taskStatus')">
              <el-tag :type="getStatusTagType(selectedTask.status)">
                {{ getStatusLabel(selectedTask.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="t('stage5.cleaner.dashboard.cleaner')" v-if="selectedTask.cleanerName">
              {{ selectedTask.cleanerName }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('stage5.cleaner.dashboard.notes')" :span="2" v-if="selectedTask.notes">
              {{ selectedTask.notes }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <!-- 待接受状态：显示接受和拒绝按钮 -->
          <template v-if="selectedTask.status === 'assigned'">
            <el-button type="danger" @click="handleReject" :loading="actionLoading">
              {{ t('stage5.cleaner.dashboard.rejectTask') }}
            </el-button>
            <el-button type="primary" @click="handleAccept" :loading="actionLoading">
              {{ t('stage5.cleaner.dashboard.acceptTask') }}
            </el-button>
          </template>

          <!-- 进行中状态：显示开始和完成按钮 -->
          <template v-else-if="selectedTask.status === 'in_progress'">
            <el-button type="success" @click="handleComplete" :loading="actionLoading">
              {{ t('stage5.cleaner.dashboard.completeCleaning') }}
            </el-button>
          </template>

          <!-- 已完成状态：只显示关闭按钮 -->
          <template v-else-if="selectedTask.status === 'completed'">
            <el-button @click="taskDetailVisible = false">{{ t('stage5.common.actions.close') }}</el-button>
          </template>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Document, Setting } from '@element-plus/icons-vue'
import CleanerInternalTaskPanel from '@/views/cleaner/components/CleanerInternalTaskPanel.vue'
import { logout as logoutApi } from '@/api/auth'
import {
  getCalendarViewData,
  acceptCleaningTask,
  rejectCleaningTask,
  completeCleaningTask,
  type CleaningTaskDTO,
} from '@/api/cleaning'
import {
  clearAllLocalSessions,
  hasCompleteCleanerSession,
  readCleanerContexts,
  readAvailableLoginTargets,
  readCleanerStoreId,
  readCleanerUser,
  switchCleanerContext,
  type CleanerSessionContext,
  type CleanerSessionUser,
} from '@/utils/cleanerSession'

const router = useRouter()
const { t, tm } = useI18n()
const cleanerUser = ref<CleanerSessionUser | null>(null)
const cleanerContexts = ref<CleanerSessionContext[]>([])
const selectedContextStoreId = ref<number | null>(null)
const internalTaskPanelRef = ref<InstanceType<typeof CleanerInternalTaskPanel> | null>(null)
const canSwitchToPms = readAvailableLoginTargets().includes('PMS')
let calendarRequestSeq = 0
let visibilityRefreshAt = 0

const loading = ref(false)
const selectedMonth = ref(new Date())
const selectedDate = ref('')
const totalTasks = ref(0)

// 任务详情弹窗
const taskDetailVisible = ref(false)
const selectedTask = ref<CleaningTaskDTO | null>(null)
const actionLoading = ref(false)

type DayStatusKey = 'assigned' | 'in_progress' | 'pending' | 'completed'

const dayDrawerVisible = ref(false)
const activeDayStatus = ref<DayStatusKey>('assigned')

const weekdays = computed(() => tm('stage5.cleaner.dashboard.weekdays') as string[])

interface CalendarDay {
  date: string
  day: number
  isCurrentMonth: boolean
  isToday: boolean
  tasks?: CleaningTaskDTO[]
}

const calendarDays = ref<CalendarDay[]>([])
const statusCount = reactive<Record<string, number>>({
  pending: 0,
  assigned: 0,
  in_progress: 0,
  completed: 0,
})

const dayDrawerTitle = computed(() => {
  return selectedDate.value
    ? t('stage5.cleaner.dashboard.dateTasksTitle', { date: selectedDate.value })
    : t('stage5.cleaner.common.task')
})

const selectedDayTasks = computed<CleaningTaskDTO[]>(() => {
  if (!selectedDate.value) return []
  return calendarDays.value.find((day) => day.date === selectedDate.value)?.tasks || []
})

const selectedDayStatusCount = computed<Record<DayStatusKey, number>>(() => {
  const counts: Record<DayStatusKey, number> = {
    assigned: 0,
    in_progress: 0,
    pending: 0,
    completed: 0,
  }
  selectedDayTasks.value.forEach((task) => {
    const status = task.status as DayStatusKey
    if (status in counts) {
      counts[status] += 1
    }
  })
  return counts
})

const filteredSelectedDayTasks = computed<CleaningTaskDTO[]>(() => {
  return selectedDayTasks.value.filter((task) => task.status === activeDayStatus.value)
})

const pickDefaultStatus = (tasks: CleaningTaskDTO[]): DayStatusKey => {
  const order: DayStatusKey[] = ['assigned', 'in_progress', 'pending', 'completed']
  const statusSet = new Set(tasks.map((task) => task.status))
  return order.find((status) => statusSet.has(status)) || 'assigned'
}

const hasStatus = (day: CalendarDay, status: DayStatusKey): boolean => {
  return (day.tasks || []).some((task) => task.status === status)
}

const hasAnyDot = (day: CalendarDay): boolean => {
  const statuses: DayStatusKey[] = ['assigned', 'in_progress', 'pending', 'completed']
  return statuses.some((status) => hasStatus(day, status))
}

// 生成日历数据
const generateCalendar = (year: number, month: number) => {
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const firstDayWeek = firstDay.getDay()
  const daysInMonth = lastDay.getDate()

  const days: CalendarDay[] = []
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  // 上个月的日期
  const prevMonthLastDay = new Date(year, month, 0).getDate()
  for (let i = firstDayWeek - 1; i >= 0; i--) {
    const day = prevMonthLastDay - i
    const date = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    days.push({
      date,
      day,
      isCurrentMonth: false,
      isToday: false,
    })
  }

  // 当月日期
  for (let i = 1; i <= daysInMonth; i++) {
    const date = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    days.push({
      date,
      day: i,
      isCurrentMonth: true,
      isToday: date === todayStr,
    })
  }

  // 下个月的日期
  const remainingDays = 42 - days.length
  for (let i = 1; i <= remainingDays; i++) {
    const date = `${year}-${String(month + 2).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    days.push({
      date,
      day: i,
      isCurrentMonth: false,
      isToday: false,
    })
  }

  calendarDays.value = days
}

// 加载日历任务数据
const loadCalendarData = async () => {
  const requestSeq = ++calendarRequestSeq
  const year = selectedMonth.value.getFullYear()
  const month = selectedMonth.value.getMonth()

  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)

  const startDate = `${year}-${String(month + 1).padStart(2, '0')}-01`
  const endDate = `${year}-${String(month + 1).padStart(2, '0')}-${String(lastDay.getDate()).padStart(2, '0')}`

  try {
    loading.value = true
    const response = await getCalendarViewData({
      startDate,
      endDate,
    })

    if (requestSeq !== calendarRequestSeq) return
    if (response.success && response.data) {
      const { tasks, totalCount, statusCount: counts } = response.data

      const tasksByDate: Record<string, CleaningTaskDTO[]> = {}
      Object.values(tasks).forEach((taskArray) => {
        taskArray.forEach((task) => {
          const dateKey = task.taskDate
          if (!tasksByDate[dateKey]) {
            tasksByDate[dateKey] = []
          }
          tasksByDate[dateKey].push(task)
        })
      })

      // 更新统计数据
      totalTasks.value = totalCount
      Object.assign(statusCount, counts)

      // 将任务分配到对应的日期
      calendarDays.value.forEach((day) => {
        const dayTasks = tasksByDate[day.date] || []
        day.tasks = dayTasks
      })
    }
  } catch (error) {
    if (requestSeq !== calendarRequestSeq) return
    console.error('Failed to load cleaner calendar data:', error)
    ElMessage.error(t('stage5.cleaner.dashboard.loadTasksFailed'))
  } finally {
    if (requestSeq === calendarRequestSeq) loading.value = false
  }
}

const resetCalendarData = () => {
  calendarRequestSeq += 1
  loading.value = false
  totalTasks.value = 0
  selectedDate.value = ''
  dayDrawerVisible.value = false
  taskDetailVisible.value = false
  selectedTask.value = null
  Object.assign(statusCount, { pending: 0, assigned: 0, in_progress: 0, completed: 0 })
  const year = selectedMonth.value.getFullYear()
  const month = selectedMonth.value.getMonth()
  generateCalendar(year, month)
}

const handleContextChange = async (storeId: number) => {
  const context = cleanerContexts.value.find((item) => item.storeId === storeId)
  if (!context) return
  try {
    cleanerUser.value = switchCleanerContext(context)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '门店切换失败，请重新登录')
    selectedContextStoreId.value = readCleanerStoreId()
    return
  }
  resetCalendarData()
  internalTaskPanelRef.value?.reset()
  await Promise.allSettled([loadCalendarData(), internalTaskPanelRef.value?.loadTasks()])
}

const refreshWhenVisible = () => {
  if (document.visibilityState !== 'visible') return
  const now = Date.now()
  if (now - visibilityRefreshAt < 30_000) return
  visibilityRefreshAt = now
  void internalTaskPanelRef.value?.loadTasks()
}

const handleMonthChange = () => {
  const year = selectedMonth.value.getFullYear()
  const month = selectedMonth.value.getMonth()
  generateCalendar(year, month)
  selectedDate.value = ''
  dayDrawerVisible.value = false
  loadCalendarData()
}

const handleDateClick = (day: CalendarDay) => {
  if (!day.isCurrentMonth) return
  selectedDate.value = day.date
  activeDayStatus.value = pickDefaultStatus(day.tasks || [])
  dayDrawerVisible.value = true
}

const handleTaskClick = (task: CleaningTaskDTO) => {
  // 显示任务详情弹窗
  selectedTask.value = task
  taskDetailVisible.value = true
}

const openTask = (task: CleaningTaskDTO) => {
  dayDrawerVisible.value = false
  handleTaskClick(task)
}

// 获取任务类型标签
const getTaskTypeLabel = (type: string) => {
  const typeMap: Record<string, string> = {
    daily: t('stage5.cleaner.dashboard.taskTypes.daily'),
    checkout: t('stage5.cleaner.dashboard.taskTypes.checkout'),
    deep: t('stage5.cleaner.dashboard.taskTypes.deep'),
  }
  return typeMap[type] || type
}

// 获取状态标签
const getStatusLabel = (status: string) => {
  const statusMap: Record<string, string> = {
    pending: t('stage5.cleaner.dashboard.status.pending'),
    assigned: t('stage5.cleaner.dashboard.status.assigned'),
    in_progress: t('stage5.cleaner.dashboard.status.inProgress'),
    completed: t('stage5.cleaner.dashboard.status.completed'),
    expired: t('stage5.cleaner.dashboard.status.expired'),
  }
  return statusMap[status] || status
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  const typeMap: Record<string, any> = {
    pending: 'info',
    assigned: 'danger',
    in_progress: 'warning',
    completed: 'success',
    expired: 'info',
  }
  return typeMap[status] || 'info'
}

// 接受任务
const handleAccept = async () => {
  if (!selectedTask.value) return

  try {
    actionLoading.value = true
    const response = await acceptCleaningTask(selectedTask.value.id)

    if (response.success) {
      ElMessage.success(t('stage5.cleaner.dashboard.acceptedTask'))
      taskDetailVisible.value = false
      // 重新加载数据
      await loadCalendarData()
    } else {
      ElMessage.error(response.message || t('stage5.cleaner.dashboard.actionFailed'))
    }
  } catch (error: any) {
    console.error('Failed to accept cleaning task:', error)
    ElMessage.error(error.message || t('stage5.cleaner.dashboard.actionFailed'))
  } finally {
    actionLoading.value = false
  }
}

// 拒绝任务
const handleReject = async () => {
  if (!selectedTask.value) return

  try {
    await ElMessageBox.confirm(t('stage5.cleaner.dashboard.confirmRejectMessage'), t('stage5.cleaner.dashboard.confirmRejectTitle'), {
      confirmButtonText: t('stage5.cleaner.dashboard.confirmButton'),
      cancelButtonText: t('stage5.common.actions.cancel'),
      type: 'warning',
    })

    actionLoading.value = true
    const response = await rejectCleaningTask(selectedTask.value.id)

    if (response.success) {
      ElMessage.success(t('stage5.cleaner.dashboard.rejectedTask'))
      taskDetailVisible.value = false
      // 重新加载数据
      await loadCalendarData()
    } else {
      ElMessage.error(response.message || t('stage5.cleaner.dashboard.actionFailed'))
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to reject cleaning task:', error)
      ElMessage.error(error.message || t('stage5.cleaner.dashboard.actionFailed'))
    }
  } finally {
    actionLoading.value = false
  }
}

// 完成任务
const handleComplete = async () => {
  if (!selectedTask.value) return

  try {
    await ElMessageBox.confirm(t('stage5.cleaner.dashboard.confirmCompleteMessage'), t('stage5.cleaner.dashboard.confirmCompleteTitle'), {
      confirmButtonText: t('stage5.cleaner.dashboard.confirmButton'),
      cancelButtonText: t('stage5.common.actions.cancel'),
      type: 'success',
    })

    actionLoading.value = true
    const response = await completeCleaningTask(selectedTask.value.id)

    if (response.success) {
      ElMessage.success(t('stage5.cleaner.dashboard.completeSuccess'))
      taskDetailVisible.value = false
      // 重新加载数据
      await loadCalendarData()
    } else {
      ElMessage.error(response.message || t('stage5.cleaner.dashboard.actionFailed'))
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to complete cleaning task:', error)
      ElMessage.error(error.message || t('stage5.cleaner.dashboard.actionFailed'))
    }
  } finally {
    actionLoading.value = false
  }
}

const handleCommand = async (command: string) => {
  if (command === 'switch-pms') {
    try {
      await logoutApi()
    } catch {
      // 本地会话仍需清理，避免旧工作区上下文残留。
    } finally {
      clearAllLocalSessions()
      router.replace({ path: '/login', query: { workspace: 'PMS', switch: '1' } })
    }
    return
  }
  if (command === 'logout') {
    clearAllLocalSessions()
    router.push('/login')
    ElMessage.success(t('stage5.cleaner.dashboard.loggedOut'))
  }
}

onMounted(() => {
  if (!hasCompleteCleanerSession()) {
    cleanerUser.value = null
    router.push('/login')
    return
  }
  cleanerUser.value = readCleanerUser()
  cleanerContexts.value = readCleanerContexts()
  selectedContextStoreId.value = readCleanerStoreId()
  const year = selectedMonth.value.getFullYear()
  const month = selectedMonth.value.getMonth()
  generateCalendar(year, month)
  void Promise.allSettled([loadCalendarData(), internalTaskPanelRef.value?.loadTasks()])
  window.addEventListener('focus', refreshWhenVisible)
  document.addEventListener('visibilitychange', refreshWhenVisible)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', refreshWhenVisible)
  document.removeEventListener('visibilitychange', refreshWhenVisible)
})
</script>

<style scoped>
.cleaner-dashboard {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.top-bar {
  background: white;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.logo {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.store-switcher {
  width: min(220px, 38vw);
  margin: 0 auto;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-size: 14px;
  color: #606266;
}

.el-dropdown-link {
  cursor: pointer;
  color: #409eff;
  font-size: 20px;
  display: flex;
  align-items: center;
}

.month-selector {
  padding: 16px 20px;
  background: white;
  margin: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.calendar-container {
  flex: 1;
  margin: 0 20px 20px;
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  overflow-y: auto;
}

.weekday-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.weekday-cell {
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #909399;
  padding: 12px 0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.day-cell {
  aspect-ratio: 1;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
  display: flex;
  flex-direction: column;
}

.day-cell:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.day-cell.other-month {
  opacity: 0.3;
}

.day-cell.today {
  background: #ecf5ff;
  border-color: #409eff;
}

.day-cell.selected {
  background: #409eff;
  border-color: #409eff;
}

.day-cell.selected .day-number {
  color: white;
}

.day-number {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.day-dots {
  margin-top: auto;
  display: flex;
  justify-content: center;
  gap: 6px;
  padding-bottom: 2px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex: 0 0 auto;
}

.dot-assigned {
  background: #f56c6c;
}

.dot-in-progress {
  background: #e6a23c;
}

.dot-pending {
  background: #409eff;
}

.dot-completed {
  background: #67c23a;
}

.day-drawer {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.day-tabs {
  margin-bottom: 8px;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 6px;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 18px;
  color: #fff;
  background: #409eff;
}

.day-task-list {
  flex: 1;
  overflow: auto;
}

.task-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 4px 0 8px;
}

.day-task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
}

.day-task-item:active {
  transform: scale(0.99);
}

.task-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.task-room {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.task-meta {
  font-size: 12px;
  color: #909399;
}

.task-action {
  font-size: 12px;
  color: #409eff;
  flex: 0 0 auto;
  margin-left: 10px;
}

.task-indicator {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}

.task-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 待分配任务 - 蓝色 */
.task-badge.status-pending {
  background: #409eff;
  color: #fff;
}

/* 已分配任务（待接受） - 红色 */
.task-badge.status-assigned {
  background: #f56c6c;
  color: #fff;
}

/* 进行中任务（待打扫） - 橙色 */
.task-badge.status-in_progress {
  background: #e6a23c;
  color: #fff;
}

/* 已完成任务 - 绿色 */
.task-badge.status-completed {
  background: #67c23a;
  color: #fff;
}

.more-tasks {
  font-size: 11px;
  color: #909399;
  text-align: center;
}

.status-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 16px 20px;
  background: white;
  margin: 0 20px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.status-item {
  text-align: center;
}

.status-item .label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.status-item .count {
  display: block;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.bottom-nav {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  background: white;
  border-top: 1px solid #e4e7ed;
  padding: 8px 0;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px;
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}

.nav-item:hover {
  color: #409eff;
}

.nav-item.active {
  color: #409eff;
}

.nav-item :deep(.el-icon) {
  font-size: 24px;
  margin-bottom: 4px;
}

.nav-item span {
  font-size: 12px;
}

.empty-state-inline {
  text-align: center;
  padding: 40px 20px;
  margin: 0 20px;
  background: white;
  border-radius: 8px;
}

.empty-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.task-badge {
  cursor: pointer;
  transition: all 0.2s;
}

.task-badge:hover {
  opacity: 0.8;
  transform: scale(1.05);
}

/* 任务详情弹窗样式 */
.task-detail {
  padding: 12px 0;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

@media (max-width: 480px) {
  .top-bar {
    padding: 12px 14px;
  }

  .month-selector {
    padding: 12px 14px;
    margin: 10px 12px;
  }

  .calendar-container {
    margin: 0 12px 12px;
    padding: 12px;
  }

  .weekday-header {
    gap: 4px;
    margin-bottom: 8px;
  }

  .weekday-cell {
    font-size: 12px;
    padding: 8px 0;
  }

  .calendar-grid {
    gap: 4px;
  }

  .day-cell {
    padding: 4px;
    border-radius: 6px;
  }

  .day-number {
    font-size: 14px;
    margin-bottom: 2px;
  }

  .day-dots {
    gap: 5px;
  }

  .dot {
    width: 6px;
    height: 6px;
  }

  .status-bar {
    margin: 0 12px 12px;
    padding: 12px;
    gap: 8px;
  }
}
</style>
