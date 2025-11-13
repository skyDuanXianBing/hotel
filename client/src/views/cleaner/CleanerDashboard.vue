<template>
  <div class="cleaner-dashboard">
    <!-- 顶部导航栏 -->
    <div class="top-bar">
      <div class="logo">保洁平台</div>
      <div class="user-info">
        <span class="username">{{ userStore.currentUser?.nickname || userStore.currentUser?.email || '保洁员' }}</span>
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-icon><Setting /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 月份选择器 -->
    <div class="month-selector">
      <el-date-picker
        v-model="selectedMonth"
        type="month"
        format="YYYY-MM"
        placeholder="选择月份"
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
          <div v-if="day.tasks && day.tasks.length > 0" class="task-indicator">
            <div
              v-for="task in day.tasks.slice(0, 3)"
              :key="task.id"
              class="task-badge"
              :class="`status-${task.status}`"
              @click.stop="handleTaskClick(task)"
            >
              {{ task.roomNumber }}
            </div>
            <div v-if="day.tasks.length > 3" class="more-tasks">
              +{{ day.tasks.length - 3 }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && totalTasks === 0" class="empty-state-inline">
      <el-icon class="empty-icon"><Document /></el-icon>
      <p class="empty-text">无任务</p>
    </div>

    <!-- 底部统计 -->
    <div class="status-bar">
      <div class="status-item">
        <span class="label">待接受</span>
        <span class="count">{{ statusCount.assigned || 0 }}</span>
      </div>
      <div class="status-item">
        <span class="label">待打扫</span>
        <span class="count">{{ statusCount.in_progress || 0 }}</span>
      </div>
      <div class="status-item">
        <span class="label">待审核</span>
        <span class="count">{{ statusCount.pending || 0 }}</span>
      </div>
      <div class="status-item">
        <span class="label">已完成</span>
        <span class="count">{{ statusCount.completed || 0 }}</span>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <div class="nav-item active">
        <el-icon><Calendar /></el-icon>
        <span>任务</span>
      </div>
      <div class="nav-item">
        <el-icon><Document /></el-icon>
        <span>登出</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Document, Setting } from '@element-plus/icons-vue'
import { getCalendarViewData, type CleaningTaskDTO } from '@/api/cleaning'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const selectedMonth = ref(new Date())
const selectedDate = ref('')
const totalTasks = ref(0)

const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

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
  const remainingDays = 42 - days.length // 6行 x 7列 = 42个格子
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

    if (response.success && response.data) {
      const { tasks, totalCount, statusCount: counts } = response.data

      // 更新统计数据
      totalTasks.value = totalCount
      Object.assign(statusCount, counts)

      // 将任务分配到对应的日期
      calendarDays.value.forEach((day) => {
        const dayTasks = tasks[day.date] || []
        day.tasks = dayTasks
      })
    }
  } catch (error) {
    console.error('加载日历数据失败:', error)
    ElMessage.error('加载任务数据失败')
  } finally {
    loading.value = false
  }
}

const handleMonthChange = () => {
  const year = selectedMonth.value.getFullYear()
  const month = selectedMonth.value.getMonth()
  generateCalendar(year, month)
  loadCalendarData()
}

const handleDateClick = (day: CalendarDay) => {
  if (!day.isCurrentMonth) return
  selectedDate.value = day.date
}

const handleTaskClick = (task: CleaningTaskDTO) => {
  // 跳转到任务详情页
  router.push(`/cleaner/task/${task.id}`)
}

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/cleaner/login')
    ElMessage.success('已退出登录')
  }
}

onMounted(() => {
  const year = selectedMonth.value.getFullYear()
  const month = selectedMonth.value.getMonth()
  generateCalendar(year, month)
  loadCalendarData()
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

.task-badge.status-pending {
  background: #f0f9ff;
  color: #409eff;
}

.task-badge.status-assigned {
  background: #fef0f0;
  color: #f56c6c;
}

.task-badge.status-in_progress {
  background: #fdf6ec;
  color: #e6a23c;
}

.task-badge.status-completed {
  background: #f0f9ff;
  color: #67c23a;
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
</style>
