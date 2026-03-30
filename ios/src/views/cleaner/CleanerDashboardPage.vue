<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>保洁员工作台</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleLogout">退出登录</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page cleaner-dashboard-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新任务" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero cleaner-dashboard-page__hero">
        <p class="mobile-note cleaner-dashboard-page__eyebrow">Cleaner Calendar</p>
        <h1 class="mobile-title">{{ cleanerDisplayName }}</h1>
        <p class="mobile-subtitle">按月份查看任务分布，点击日期即可查看当天任务并进入详情页。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ selectedMonthLabel }}</span>
          <span class="mobile-chip">总任务 {{ totalCount }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card cleaner-dashboard-page__toolbar-card">
          <div class="cleaner-dashboard-page__toolbar-row">
            <ion-button fill="outline" size="small" @click="handlePreviousMonth">上月</ion-button>
            <ion-button fill="outline" size="small" @click="handleCurrentMonth">本月</ion-button>
            <ion-button fill="outline" size="small" @click="handleNextMonth">下月</ion-button>
          </div>

          <div class="cleaner-dashboard-page__month-text">{{ selectedMonthLabel }}</div>
          <p v-if="errorMessage" class="mobile-note cleaner-dashboard-page__error">{{ errorMessage }}</p>
        </section>

        <section class="mobile-card cleaner-dashboard-page__calendar-card">
          <div class="cleaner-dashboard-page__weekday-row">
            <span v-for="weekday in WEEKDAY_LABELS" :key="weekday">{{ weekday }}</span>
          </div>

          <div class="cleaner-dashboard-page__calendar-grid">
            <button
              v-for="day in calendarDays"
              :key="day.date"
              class="cleaner-dashboard-page__day-cell"
              :class="{
                'is-other-month': !day.isCurrentMonth,
                'is-today': day.isToday,
                'is-selected': day.date === selectedDate,
              }"
              type="button"
              @click="handleOpenDayTasks(day)"
            >
              <span class="cleaner-dashboard-page__day-number">{{ day.dayNumber }}</span>

              <span class="cleaner-dashboard-page__dot-row">
                <span v-if="hasStatus(day.tasks, 'assigned')" class="cleaner-dashboard-page__dot is-assigned" />
                <span v-if="hasStatus(day.tasks, 'in_progress')" class="cleaner-dashboard-page__dot is-progress" />
                <span v-if="hasStatus(day.tasks, 'pending')" class="cleaner-dashboard-page__dot is-pending" />
                <span v-if="hasStatus(day.tasks, 'completed')" class="cleaner-dashboard-page__dot is-completed" />
              </span>
            </button>
          </div>
        </section>

        <section class="mobile-card cleaner-dashboard-page__stats-card">
          <article class="cleaner-dashboard-page__stat-item">
            <strong>{{ statusCount.assigned }}</strong>
            <span>待接受</span>
          </article>
          <article class="cleaner-dashboard-page__stat-item">
            <strong>{{ statusCount.in_progress }}</strong>
            <span>待打扫</span>
          </article>
          <article class="cleaner-dashboard-page__stat-item">
            <strong>{{ statusCount.pending }}</strong>
            <span>待分配</span>
          </article>
          <article class="cleaner-dashboard-page__stat-item">
            <strong>{{ statusCount.completed }}</strong>
            <span>已完成</span>
          </article>
        </section>
      </div>

      <ion-modal
        :is-open="dayModalOpen"
        :initial-breakpoint="0.8"
        :breakpoints="[0, 0.5, 0.8, 1]"
        @didDismiss="handleCloseDayTasks"
      >
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ selectedDateTitle }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseDayTasks">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page cleaner-dashboard-page__modal-page">
          <section class="mobile-card cleaner-dashboard-page__modal-card">
            <div class="cleaner-dashboard-page__status-tabs">
              <button
                v-for="statusKey in CLEANER_STATUS_ORDER"
                :key="statusKey"
                class="cleaner-dashboard-page__status-tab"
                :class="{
                  'is-active': activeDayStatus === statusKey,
                }"
                type="button"
                @click="handleSwitchDayStatus(statusKey)"
              >
                <span>{{ getStatusLabel(statusKey) }}</span>
                <strong>{{ selectedDayStatusCount[statusKey] }}</strong>
              </button>
            </div>

            <div v-if="filteredSelectedDayTasks.length > 0" class="mobile-list cleaner-dashboard-page__task-list">
              <button
                v-for="task in filteredSelectedDayTasks"
                :key="task.id"
                class="cleaner-dashboard-page__task-item"
                type="button"
                @click="handleOpenTask(task.id)"
              >
                <div>
                  <strong>{{ task.roomNumber || '-' }}</strong>
                  <p>{{ getTaskTypeLabel(task.taskType) }}</p>
                </div>
                <span>{{ getStatusLabel(task.status) }}</span>
              </button>
            </div>

            <p v-else class="mobile-note">当天该状态暂无任务。</p>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { onIonViewWillEnter } from '@ionic/vue'
import { getCalendarViewData, type CleaningTaskDTO } from '@/api/cleaning'
import {
  CLEANER_STATUS_LABELS,
  CLEANER_STATUS_ORDER,
  CLEANER_TASK_TYPE_LABELS,
  type CleanerTaskStatusKey,
} from '@/constants/cleaner'
import { buildCleanerTaskDetailPath, ROUTE_PATHS } from '@/router/guards'
import { showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { clearCleanerSession, readCleanerUser } from '@/utils/cleanerSession'
import { formatDate } from '@/utils/accommodation'
import { useRouter } from 'vue-router'

const WEEKDAY_LABELS = ['日', '一', '二', '三', '四', '五', '六']
const EMPTY_STATUS_COUNT = {
  assigned: 0,
  in_progress: 0,
  pending: 0,
  completed: 0,
}

interface CalendarDayItem {
  date: string
  dayNumber: number
  isCurrentMonth: boolean
  isToday: boolean
  tasks: CleaningTaskDTO[]
}

const router = useRouter()

const cleanerUser = ref(readCleanerUser())
const selectedMonth = ref(buildMonthValue(new Date()))
const selectedDate = ref('')
const calendarDays = ref<CalendarDayItem[]>([])
const totalCount = ref(0)
const dayModalOpen = ref(false)
const activeDayStatus = ref<CleanerTaskStatusKey>('assigned')
const errorMessage = ref('')
const statusCount = ref({
  assigned: 0,
  in_progress: 0,
  pending: 0,
  completed: 0,
})

const cleanerDisplayName = computed(() => {
  if (cleanerUser.value?.nickname) {
    return cleanerUser.value.nickname
  }

  if (cleanerUser.value?.email) {
    return cleanerUser.value.email
  }

  return '保洁员'
})

const selectedMonthLabel = computed(() => {
  const parsedMonth = parseMonthValue(selectedMonth.value)
  return `${parsedMonth.getFullYear()} 年 ${String(parsedMonth.getMonth() + 1).padStart(2, '0')} 月`
})

const selectedDayTasks = computed(() => {
  if (!selectedDate.value) {
    return []
  }

  const matchedDay = calendarDays.value.find((day) => day.date === selectedDate.value)
  if (!matchedDay) {
    return []
  }

  return matchedDay.tasks
})

const selectedDateTitle = computed(() => {
  if (!selectedDate.value) {
    return '当天任务'
  }

  return `${formatDate(selectedDate.value)} 任务`
})

const selectedDayStatusCount = computed<Record<CleanerTaskStatusKey, number>>(() => {
  const nextCount = {
    assigned: 0,
    in_progress: 0,
    pending: 0,
    completed: 0,
  }

  for (const task of selectedDayTasks.value) {
    if (task.status === 'assigned') {
      nextCount.assigned += 1
    }

    if (task.status === 'in_progress') {
      nextCount.in_progress += 1
    }

    if (task.status === 'pending') {
      nextCount.pending += 1
    }

    if (task.status === 'completed') {
      nextCount.completed += 1
    }
  }

  return nextCount
})

const filteredSelectedDayTasks = computed(() => {
  return selectedDayTasks.value.filter((task) => task.status === activeDayStatus.value)
})

function buildMonthValue(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}`
}

function parseMonthValue(value: string) {
  const parts = value.split('-')
  const year = Number(parts[0])
  const month = Number(parts[1])

  if (!year || !month) {
    return new Date()
  }

  return new Date(year, month - 1, 1)
}

function buildDateTextFromDate(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
}

function buildCalendarDays(taskMap: Record<string, CleaningTaskDTO[]>) {
  const parsedMonth = parseMonthValue(selectedMonth.value)
  const year = parsedMonth.getFullYear()
  const monthIndex = parsedMonth.getMonth()
  const firstDay = new Date(year, monthIndex, 1)
  const firstWeekday = firstDay.getDay()
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate()
  const previousMonthDays = new Date(year, monthIndex, 0).getDate()
  const today = new Date()
  const todayMonth = buildMonthValue(today)
  const todayDate = buildDateTextFromDate(today)
  const nextDays: CalendarDayItem[] = []

  for (let index = firstWeekday - 1; index >= 0; index -= 1) {
    const dayNumber = previousMonthDays - index
    const dateValue = new Date(year, monthIndex - 1, dayNumber)
    const date = buildDateTextFromDate(dateValue)
    nextDays.push({
      date,
      dayNumber,
      isCurrentMonth: false,
      isToday: false,
      tasks: taskMap[date] || [],
    })
  }

  for (let dayNumber = 1; dayNumber <= daysInMonth; dayNumber += 1) {
    const dateValue = new Date(year, monthIndex, dayNumber)
    const date = buildDateTextFromDate(dateValue)
    nextDays.push({
      date,
      dayNumber,
      isCurrentMonth: true,
      isToday: selectedMonth.value === todayMonth && date === todayDate,
      tasks: taskMap[date] || [],
    })
  }

  const remainingDays = 42 - nextDays.length
  for (let dayNumber = 1; dayNumber <= remainingDays; dayNumber += 1) {
    const dateValue = new Date(year, monthIndex + 1, dayNumber)
    const date = buildDateTextFromDate(dateValue)
    nextDays.push({
      date,
      dayNumber,
      isCurrentMonth: false,
      isToday: false,
      tasks: taskMap[date] || [],
    })
  }

  calendarDays.value = nextDays
}

function getMonthRange() {
  const parsedMonth = parseMonthValue(selectedMonth.value)
  const year = parsedMonth.getFullYear()
  const monthIndex = parsedMonth.getMonth()
  const lastDate = new Date(year, monthIndex + 1, 0).getDate()

  return {
    startDate: buildDateTextFromDate(new Date(year, monthIndex, 1)),
    endDate: buildDateTextFromDate(new Date(year, monthIndex, lastDate)),
  }
}

function getStatusLabel(status: string) {
  return CLEANER_STATUS_LABELS[status] || status || '未知状态'
}

function getTaskTypeLabel(taskType: string) {
  return CLEANER_TASK_TYPE_LABELS[taskType] || taskType || '未设置'
}

function pickDefaultStatus(tasks: CleaningTaskDTO[]) {
  for (const statusKey of CLEANER_STATUS_ORDER) {
    if (tasks.some((task) => task.status === statusKey)) {
      return statusKey
    }
  }

  return 'assigned'
}

function hasStatus(tasks: CleaningTaskDTO[], status: CleanerTaskStatusKey) {
  return tasks.some((task) => task.status === status)
}

async function loadCalendarData() {
  if (!cleanerUser.value?.id) {
    await router.replace(ROUTE_PATHS.cleanerLogin)
    return
  }

  errorMessage.value = ''

  try {
    const monthRange = getMonthRange()
    const response = await getCalendarViewData({
      startDate: monthRange.startDate,
      endDate: monthRange.endDate,
      cleanerId: cleanerUser.value.id,
    })

    if (!response.success || !response.data) {
      throw new Error(response.message || '加载任务失败')
    }

    totalCount.value = response.data.totalCount || 0
    statusCount.value = {
      assigned: response.data.statusCount.assigned || 0,
      in_progress: response.data.statusCount.in_progress || 0,
      pending: response.data.statusCount.pending || 0,
      completed: response.data.statusCount.completed || 0,
    }
    buildCalendarDays(response.data.tasks || {})

    if (selectedDate.value) {
      const selectedTasks = response.data.tasks[selectedDate.value] || []
      activeDayStatus.value = pickDefaultStatus(selectedTasks)
    }
  } catch (error) {
    statusCount.value = { ...EMPTY_STATUS_COUNT }
    totalCount.value = 0
    buildCalendarDays({})

    const message = error instanceof Error ? error.message : '加载任务失败'
    errorMessage.value = message

    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  }
}

function shiftMonth(offset: number) {
  const parsedMonth = parseMonthValue(selectedMonth.value)
  parsedMonth.setMonth(parsedMonth.getMonth() + offset)
  selectedMonth.value = buildMonthValue(parsedMonth)
}

async function handlePreviousMonth() {
  shiftMonth(-1)
  selectedDate.value = ''
  dayModalOpen.value = false
  await loadCalendarData()
}

async function handleCurrentMonth() {
  selectedMonth.value = buildMonthValue(new Date())
  selectedDate.value = ''
  dayModalOpen.value = false
  await loadCalendarData()
}

async function handleNextMonth() {
  shiftMonth(1)
  selectedDate.value = ''
  dayModalOpen.value = false
  await loadCalendarData()
}

function handleOpenDayTasks(day: CalendarDayItem) {
  if (!day.isCurrentMonth) {
    return
  }

  selectedDate.value = day.date
  activeDayStatus.value = pickDefaultStatus(day.tasks)
  dayModalOpen.value = true
}

function handleCloseDayTasks() {
  dayModalOpen.value = false
}

function handleSwitchDayStatus(statusKey: CleanerTaskStatusKey) {
  activeDayStatus.value = statusKey
}

async function handleOpenTask(taskId: number) {
  dayModalOpen.value = false
  await router.push(buildCleanerTaskDetailPath(taskId))
}

async function handleLogout() {
  clearCleanerSession()
  await router.replace(ROUTE_PATHS.cleanerLogin)
}

async function handleRefresh(event: CustomEvent) {
  await loadCalendarData()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  cleanerUser.value = readCleanerUser()

  if (!cleanerUser.value) {
    await router.replace(ROUTE_PATHS.cleanerLogin)
    return
  }

  await loadCalendarData()
})
</script>

<style scoped>
.cleaner-dashboard-page {
  display: block;
}

.cleaner-dashboard-page__hero {
  margin-top: 4px;
}

.cleaner-dashboard-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.cleaner-dashboard-page__toolbar-card,
.cleaner-dashboard-page__calendar-card,
.cleaner-dashboard-page__modal-card {
  display: grid;
  gap: 14px;
}

.cleaner-dashboard-page__toolbar-row,
.cleaner-dashboard-page__status-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.cleaner-dashboard-page__month-text {
  color: var(--app-heading);
  font-size: 15px;
  font-weight: 700;
}

.cleaner-dashboard-page__error {
  color: var(--ion-color-danger);
}

.cleaner-dashboard-page__weekday-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  text-align: center;
  color: var(--app-muted);
  font-size: 12px;
  font-weight: 600;
}

.cleaner-dashboard-page__calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.cleaner-dashboard-page__day-cell {
  min-height: 72px;
  padding: 8px 4px;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  display: grid;
  align-content: space-between;
}

.cleaner-dashboard-page__day-cell.is-other-month {
  opacity: 0.35;
}

.cleaner-dashboard-page__day-cell.is-today {
  border-color: var(--ion-color-primary);
  background: rgba(var(--ion-color-primary-rgb), 0.08);
}

.cleaner-dashboard-page__day-cell.is-selected {
  border-color: var(--ion-color-primary);
}

.cleaner-dashboard-page__day-number {
  color: var(--app-heading);
  font-size: 14px;
  font-weight: 700;
}

.cleaner-dashboard-page__dot-row {
  min-height: 8px;
  display: flex;
  justify-content: center;
  gap: 5px;
}

.cleaner-dashboard-page__dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
}

.cleaner-dashboard-page__dot.is-assigned {
  background: #ef4444;
}

.cleaner-dashboard-page__dot.is-progress {
  background: #f59e0b;
}

.cleaner-dashboard-page__dot.is-pending {
  background: #3b82f6;
}

.cleaner-dashboard-page__dot.is-completed {
  background: #22c55e;
}

.cleaner-dashboard-page__stats-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.cleaner-dashboard-page__stat-item {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(16, 35, 63, 0.04);
  text-align: center;
}

.cleaner-dashboard-page__stat-item strong {
  color: var(--app-heading);
  font-size: 22px;
}

.cleaner-dashboard-page__stat-item span {
  color: var(--app-muted);
  font-size: 13px;
}

.cleaner-dashboard-page__modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.cleaner-dashboard-page__status-tab,
.cleaner-dashboard-page__task-item {
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.92);
}

.cleaner-dashboard-page__status-tab {
  min-height: 44px;
  padding: 8px 12px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.cleaner-dashboard-page__status-tab.is-active {
  border-color: var(--ion-color-primary);
  color: var(--ion-color-primary);
}

.cleaner-dashboard-page__task-list {
  margin-top: 8px;
}

.cleaner-dashboard-page__task-item {
  width: 100%;
  padding: 14px;
  border-radius: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  text-align: left;
}

.cleaner-dashboard-page__task-item strong,
.cleaner-dashboard-page__task-item p {
  margin: 0;
}

.cleaner-dashboard-page__task-item p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.cleaner-dashboard-page__task-item span {
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 420px) {
  .cleaner-dashboard-page__calendar-grid,
  .cleaner-dashboard-page__weekday-row {
    gap: 4px;
  }

  .cleaner-dashboard-page__day-cell {
    min-height: 64px;
    border-radius: 12px;
  }
}
</style>
