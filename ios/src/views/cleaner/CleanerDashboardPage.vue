<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-title class="app-page-header__title">保洁员工作台</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleLogout">退出登录</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page cleaner-dashboard-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新任务" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-card cleaner-dashboard-page__calendar-card">
        <div class="cleaner-dashboard-page__panel-header">
          <div class="cleaner-dashboard-page__panel-copy">
            <p class="cleaner-dashboard-page__eyebrow">保洁任务</p>
            <h1 class="cleaner-dashboard-page__panel-title">{{ cleanerDisplayName }}</h1>
          </div>
          <div class="cleaner-dashboard-page__panel-total">
            <strong>{{ totalCount }}</strong>
            <span>本月任务</span>
          </div>
        </div>

        <div class="cleaner-dashboard-page__month-row">
          <button class="cleaner-dashboard-page__month-nav" type="button" @click="handlePreviousMonth">
            <span aria-hidden="true">‹</span>
          </button>
          <div class="cleaner-dashboard-page__month-copy">
            <span class="cleaner-dashboard-page__month-caption">任务日历</span>
            <strong class="cleaner-dashboard-page__month-value">{{ selectedMonthLabel }}</strong>
          </div>
          <button class="cleaner-dashboard-page__month-nav" type="button" @click="handleNextMonth">
            <span aria-hidden="true">›</span>
          </button>
        </div>

        <div class="cleaner-dashboard-page__meta-row">
          <p class="mobile-note">点击日期查看当天任务。</p>
          <button
            v-if="!isViewingCurrentMonth"
            class="cleaner-dashboard-page__month-reset"
            type="button"
            @click="handleCurrentMonth"
          >
            回到本月
          </button>
        </div>

        <div class="cleaner-dashboard-page__status-strip">
          <span
            v-for="item in statusSummaryItems"
            :key="item.key"
            class="cleaner-dashboard-page__status-pill"
            :class="`is-${item.key}`"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
          </span>
        </div>

        <p v-if="errorMessage" class="mobile-note cleaner-dashboard-page__error">{{ errorMessage }}</p>

        <div class="cleaner-dashboard-page__weekday-row">
          <span v-for="weekday in WEEKDAY_LABELS" :key="weekday">{{ weekday }}</span>
        </div>

        <div class="cleaner-dashboard-page__calendar-grid">
          <button
            v-for="day in calendarDays"
            :key="day.date"
            class="cleaner-dashboard-page__day-cell"
            :class="{
              'has-tasks': day.isCurrentMonth && day.tasks.length > 0,
              'is-other-month': !day.isCurrentMonth,
              'is-today': day.isToday,
              'is-selected': day.date === selectedDate,
            }"
            :disabled="!day.isCurrentMonth"
            type="button"
            @click="handleOpenDayTasks(day)"
          >
            <span class="cleaner-dashboard-page__day-number">{{ day.dayNumber }}</span>

            <span v-if="day.isCurrentMonth" class="cleaner-dashboard-page__dot-row">
              <span v-if="hasStatus(day.tasks, 'assigned')" class="cleaner-dashboard-page__dot is-assigned" />
              <span v-if="hasStatus(day.tasks, 'in_progress')" class="cleaner-dashboard-page__dot is-progress" />
              <span v-if="hasStatus(day.tasks, 'pending')" class="cleaner-dashboard-page__dot is-pending" />
              <span v-if="hasStatus(day.tasks, 'completed')" class="cleaner-dashboard-page__dot is-completed" />
            </span>
          </button>
        </div>
      </section>

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
            <div v-if="dayStatusTabs.length > 1" class="cleaner-dashboard-page__status-tabs">
              <button
                v-for="statusKey in dayStatusTabs"
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
                  <p>
                    {{ getTaskTypeLabel(task.taskType) }}
                    <template v-if="task.estimatedTime"> · {{ task.estimatedTime }}</template>
                  </p>
                </div>
                <span>详情</span>
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
  alertController,
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

const isViewingCurrentMonth = computed(() => {
  return selectedMonth.value === buildMonthValue(new Date())
})

const statusSummaryItems = computed(() => {
  return [
    { key: 'assigned' as CleanerTaskStatusKey, label: '待接受', count: statusCount.value.assigned },
    { key: 'in_progress' as CleanerTaskStatusKey, label: '进行中', count: statusCount.value.in_progress },
    { key: 'pending' as CleanerTaskStatusKey, label: '待分配', count: statusCount.value.pending },
    { key: 'completed' as CleanerTaskStatusKey, label: '已完成', count: statusCount.value.completed },
  ]
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

const dayStatusTabs = computed(() => {
  return CLEANER_STATUS_ORDER.filter((statusKey) => selectedDayStatusCount.value[statusKey] > 0)
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
    await router.replace(ROUTE_PATHS.login)
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

async function confirmLogout() {
  const alert = await alertController.create({
    header: '退出登录',
    message: '确认退出当前保洁账号吗？',
    buttons: [
      {
        text: '取消',
        role: 'cancel',
      },
      {
        text: '确定',
        role: 'destructive',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function handleLogout() {
  const confirmed = await confirmLogout()
  if (!confirmed) {
    return
  }

  clearCleanerSession()
  await router.replace(ROUTE_PATHS.login)
}

async function handleRefresh(event: CustomEvent) {
  await loadCalendarData()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  cleanerUser.value = readCleanerUser()

  if (!cleanerUser.value) {
    await router.replace(ROUTE_PATHS.login)
    return
  }

  await loadCalendarData()
})
</script>

<style scoped>
.cleaner-dashboard-page {
  display: block;
}

.cleaner-dashboard-page__calendar-card {
  display: grid;
  gap: 16px;
  margin-top: 4px;
}

.cleaner-dashboard-page__eyebrow {
  margin: 0 0 6px;
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-note-size);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.cleaner-dashboard-page__panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.cleaner-dashboard-page__panel-copy {
  min-width: 0;
}

.cleaner-dashboard-page__panel-title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 24px;
  font-weight: var(--ios-pms-weight-heavy);
  letter-spacing: -0.04em;
}

.cleaner-dashboard-page__panel-total {
  display: grid;
  justify-items: center;
  gap: 4px;
  min-width: 84px;
  padding: 10px 12px;
  border: 1px solid var(--app-border);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(242, 247, 255, 0.94));
}

.cleaner-dashboard-page__panel-total strong {
  color: var(--ios-pms-text-primary);
  font-size: 24px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1;
}

.cleaner-dashboard-page__panel-total span {
  color: var(--ios-pms-text-muted);
  font-size: var(--ios-pms-font-body-sm-size);
  font-weight: 700;
}

.cleaner-dashboard-page__month-row {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 44px;
  gap: 12px;
  align-items: center;
}

.cleaner-dashboard-page__month-nav {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 14px;
  background: rgba(244, 247, 255, 0.92);
  box-shadow: inset 0 0 0 1px var(--ios-pms-border-faint);
  color: var(--ios-pms-text-primary);
  font-size: 24px;
}

.cleaner-dashboard-page__month-copy {
  display: grid;
  gap: 4px;
  justify-items: center;
  min-width: 0;
  text-align: center;
}

.cleaner-dashboard-page__month-caption {
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-body-sm-size);
  font-weight: 700;
  letter-spacing: 0.06em;
}

.cleaner-dashboard-page__month-value {
  color: var(--ios-pms-text-primary);
  font-size: 18px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: -0.03em;
}

.cleaner-dashboard-page__meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.cleaner-dashboard-page__month-reset {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--ion-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.cleaner-dashboard-page__status-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cleaner-dashboard-page__status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(244, 247, 255, 0.96);
  box-shadow: inset 0 0 0 1px var(--ios-pms-border-faint);
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.cleaner-dashboard-page__status-pill::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
  opacity: 0.95;
}

.cleaner-dashboard-page__status-pill strong {
  color: var(--ios-pms-text-primary);
  font-size: 13px;
}

.cleaner-dashboard-page__status-pill.is-assigned {
  color: var(--ion-color-danger);
}

.cleaner-dashboard-page__status-pill.is-in_progress {
  color: var(--ion-color-warning);
}

.cleaner-dashboard-page__status-pill.is-pending {
  color: var(--ion-color-primary);
}

.cleaner-dashboard-page__status-pill.is-completed {
  color: var(--ion-color-success);
}

.cleaner-dashboard-page__error {
  color: var(--ion-color-danger);
}

.cleaner-dashboard-page__weekday-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
  text-align: center;
  color: var(--ios-pms-text-soft);
  font-size: 11px;
  font-weight: 600;
}

.cleaner-dashboard-page__calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
}

.cleaner-dashboard-page__day-cell {
  min-height: 74px;
  padding: 10px 6px 8px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.92));
  display: grid;
  align-content: space-between;
  box-shadow: inset 0 0 0 1px var(--ios-pms-border-faint);
  transition:
    transform 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.cleaner-dashboard-page__day-cell.has-tasks {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(240, 246, 255, 0.96));
}

.cleaner-dashboard-page__day-cell:active {
  transform: translateY(1px);
}

.cleaner-dashboard-page__day-cell.is-other-month {
  background: rgba(255, 255, 255, 0.42);
  box-shadow: inset 0 0 0 1px rgba(116, 138, 185, 0.05);
}

.cleaner-dashboard-page__day-cell:disabled {
  cursor: default;
}

.cleaner-dashboard-page__day-cell.is-today {
  background: linear-gradient(180deg, rgba(246, 250, 255, 1), rgba(232, 242, 255, 0.98));
  box-shadow: inset 0 0 0 1px rgba(var(--ion-color-primary-rgb), 0.24);
}

.cleaner-dashboard-page__day-cell.is-selected {
  background: linear-gradient(180deg, rgba(249, 251, 255, 1), rgba(232, 241, 255, 1));
  box-shadow:
    inset 0 0 0 1.5px rgba(var(--ion-color-primary-rgb), 0.42),
    0 10px 20px rgba(52, 116, 246, 0.1);
}

.cleaner-dashboard-page__day-number {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  margin: 0 auto 0 0;
  border-radius: 12px;
  color: var(--ios-pms-text-primary);
  font-size: 14px;
  font-weight: 700;
}

.cleaner-dashboard-page__day-cell.is-selected .cleaner-dashboard-page__day-number {
  background: var(--ion-color-primary);
  color: #fff;
}

.cleaner-dashboard-page__day-cell.is-other-month .cleaner-dashboard-page__day-number {
  color: var(--ios-pms-text-disabled);
}

.cleaner-dashboard-page__dot-row {
  min-height: 8px;
  display: flex;
  justify-content: flex-start;
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

.cleaner-dashboard-page__modal-card {
  display: grid;
  gap: 12px;
}

.cleaner-dashboard-page__modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.cleaner-dashboard-page__status-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.cleaner-dashboard-page__status-tab {
  min-height: 40px;
  padding: 8px 12px;
  border: none;
  border-radius: 999px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(244, 247, 255, 0.96);
  box-shadow: inset 0 0 0 1px var(--ios-pms-border-faint);
  color: var(--ios-pms-text-muted);
}

.cleaner-dashboard-page__status-tab strong {
  color: var(--ios-pms-text-primary);
  font-size: 13px;
}

.cleaner-dashboard-page__status-tab.is-active {
  background: rgba(var(--ion-color-primary-rgb), 0.1);
  box-shadow: inset 0 0 0 1px rgba(var(--ion-color-primary-rgb), 0.18);
  color: var(--ion-color-primary);
}

.cleaner-dashboard-page__task-list {
  margin-top: 0;
}

.cleaner-dashboard-page__task-item {
  width: 100%;
  padding: 16px 2px;
  border: none;
  border-radius: 0;
  background: transparent;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  box-shadow: none;
  text-align: left;
}

.cleaner-dashboard-page__task-item + .cleaner-dashboard-page__task-item {
  border-top: 1px solid var(--ios-pms-divider);
}

.cleaner-dashboard-page__task-item strong,
.cleaner-dashboard-page__task-item p {
  margin: 0;
}

.cleaner-dashboard-page__task-item strong {
  color: var(--ios-pms-text-primary);
  font-size: 16px;
  font-weight: var(--ios-pms-weight-bold);
}

.cleaner-dashboard-page__task-item p {
  margin-top: 4px;
  color: var(--ios-pms-text-muted);
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

  .cleaner-dashboard-page__panel-title {
    font-size: 22px;
  }

  .cleaner-dashboard-page__day-cell {
    min-height: 64px;
    border-radius: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cleaner-dashboard-page__day-cell {
    transition: none;
  }
}
</style>
