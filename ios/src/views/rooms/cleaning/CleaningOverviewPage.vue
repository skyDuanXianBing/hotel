<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.rooms" />
        </ion-buttons>
        <ion-title>保洁概览</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page cleaning-overview-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新保洁概览" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero cleaning-overview-page__hero">
        <p class="mobile-note cleaning-overview-page__eyebrow">住宿保洁</p>
        <h1 class="mobile-title">保洁任务概览</h1>
        <p class="mobile-subtitle">使用房型分组 + 房间卡片 + 7 日任务格替代桌面右键矩阵。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ formatDate(windowStartDate) }} - {{ formatDate(windowEndDate) }}</span>
          <span class="mobile-chip">房间 {{ roomItems.length }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card cleaning-overview-page__toolbar-card">
          <div class="cleaning-overview-page__toolbar-row">
            <ion-button fill="outline" size="small" @click="handleShiftWindow(-7)">上周</ion-button>
            <ion-button fill="outline" size="small" @click="handleGoToday">今天</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftWindow(7)">下周</ion-button>
            <ion-button fill="outline" size="small" @click="handleGenerateTasks">生成任务</ion-button>
          </div>

          <div class="cleaning-overview-page__filter-grid">
            <label class="cleaning-overview-page__field">
              <span>窗口开始</span>
              <input :value="selectedDate" type="date" @input="handleDateInput" />
            </label>

            <label class="cleaning-overview-page__field">
              <span>状态筛选</span>
              <select :value="statusFilter" @change="handleStatusChange">
                <option value="all">全部任务</option>
                <option value="pending">待分配</option>
                <option value="assigned">待清洁</option>
                <option value="in_progress">清洁中</option>
                <option value="completed">已完成</option>
                <option value="expired">已过期</option>
              </select>
            </label>
          </div>

          <p v-if="errorMessage" class="mobile-note cleaning-overview-page__error">{{ errorMessage }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row cleaning-overview-page__section-header">
            <div>
              <h2 class="mobile-section-title">任务窗口</h2>
              <p class="mobile-note">点击任务查看详情；空白未来日期可直接新增任务。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="roomGroups.length > 0" class="mobile-list cleaning-overview-page__group-list">
            <section v-for="group in roomGroups" :key="group.roomType" class="cleaning-overview-page__group-card">
              <div class="cleaning-overview-page__group-header">
                <strong>{{ group.roomType }}</strong>
                <span>{{ group.rooms.length }} 间</span>
              </div>

              <div class="mobile-list cleaning-overview-page__room-list">
                <article v-for="room in group.rooms" :key="room.roomId" class="cleaning-overview-page__room-card">
                  <div class="cleaning-overview-page__room-header">
                    <strong>{{ room.roomNumber }}</strong>
                    <span>{{ room.roomType }}</span>
                  </div>

                  <div class="cleaning-overview-page__date-strip">
                    <article v-for="day in dateWindow" :key="`${room.roomId}-${day.date}`" class="cleaning-overview-page__date-cell">
                      <div class="cleaning-overview-page__date-title">
                        <strong>{{ day.shortLabel }}</strong>
                        <span>{{ day.weekday }}</span>
                      </div>

                      <div class="cleaning-overview-page__task-stack">
                        <button
                          v-for="task in getTasksForCell(room.roomId, day.date)"
                          :key="task.id"
                          class="cleaning-overview-page__task-badge"
                          :class="getCleaningTaskStatusClass(task.status)"
                          type="button"
                          @click="handleOpenTaskDetail(task.id)"
                        >
                          {{ getCleaningTaskStatusText(task.status) }}
                        </button>

                        <button
                          v-if="getTasksForCell(room.roomId, day.date).length <= 0"
                          class="cleaning-overview-page__empty-action"
                          type="button"
                          @click="handleOpenCreateTask(room, day.date)"
                        >
                          {{ day.date < todayDate ? '历史日期' : '新增任务' }}
                        </button>
                      </div>
                    </article>
                  </div>
                </article>
              </div>
            </section>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前门店暂无房间数据，可先在房型设置中补充房间号。</p>
        </section>
      </div>

      <ion-modal :is-open="taskDetailOpen" @didDismiss="handleCloseTaskDetail">
        <ion-header>
          <ion-toolbar>
            <ion-title>任务详情</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseTaskDetail">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page cleaning-overview-page__modal-page">
          <section v-if="selectedTask" class="mobile-card cleaning-overview-page__detail-card">
            <div class="cleaning-overview-page__detail-grid">
              <span>房型 {{ selectedTask.roomType }}</span>
              <span>房间 {{ selectedTask.roomNumber }}</span>
              <span>任务日期 {{ selectedTask.taskDate }}</span>
              <span>任务类型 {{ getCleaningTaskTypeText(selectedTask.taskType) }}</span>
              <span>任务状态 {{ getCleaningTaskStatusText(selectedTask.status) }}</span>
              <span>保洁员 {{ selectedTask.cleanerName || '未分配' }}</span>
              <span>预计时间 {{ selectedTask.estimatedTime || '-' }}</span>
              <span>审批人 {{ selectedTask.approverName || '-' }}</span>
            </div>

            <label v-if="selectedTask.status === 'pending'" class="cleaning-overview-page__field">
              <span>分配保洁员</span>
              <select v-model="selectedCleanerIdText">
                <option value="">请选择保洁员</option>
                <option v-for="cleaner in cleaners" :key="cleaner.id" :value="String(cleaner.id)">
                  {{ cleaner.name }}
                </option>
              </select>
            </label>

            <p v-if="selectedTask.notes" class="mobile-note">备注：{{ selectedTask.notes }}</p>

            <div class="cleaning-overview-page__modal-actions">
              <ion-button fill="outline" @click="handleCloseTaskDetail">关闭</ion-button>
              <ion-button v-if="selectedTask.status === 'pending'" :disabled="submitting" @click="handleAssignTask">
                {{ submitting ? '分配中...' : '分配任务' }}
              </ion-button>
              <ion-button
                v-if="selectedTask.status === 'assigned' || selectedTask.status === 'in_progress'"
                :disabled="submitting"
                @click="handleCompleteTask"
              >
                {{ submitting ? '提交中...' : '完成任务' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="createModalOpen" @didDismiss="handleCloseCreateTask">
        <ion-header>
          <ion-toolbar>
            <ion-title>新增任务</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseCreateTask">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page cleaning-overview-page__modal-page">
          <section class="mobile-card cleaning-overview-page__detail-card">
            <div class="cleaning-overview-page__detail-grid">
              <span>房型 {{ createForm.roomType }}</span>
              <span>房间 {{ createForm.roomNumber }}</span>
              <span>任务日期 {{ createForm.taskDate }}</span>
            </div>

            <div class="cleaning-overview-page__filter-grid">
              <label class="cleaning-overview-page__field">
                <span>任务类型</span>
                <select v-model="createForm.taskType">
                  <option value="">请选择任务类型</option>
                  <option value="checkout">退房</option>
                  <option value="daily">日常清洁</option>
                  <option value="deep">深度清洁</option>
                </select>
              </label>

              <label class="cleaning-overview-page__field">
                <span>保洁员</span>
                <select v-model="createForm.cleanerIdText">
                  <option value="">暂不分配</option>
                  <option v-for="cleaner in cleaners" :key="cleaner.id" :value="String(cleaner.id)">
                    {{ cleaner.name }}
                  </option>
                </select>
              </label>

              <label class="cleaning-overview-page__field">
                <span>开始时间</span>
                <input v-model="createForm.startTime" type="time" />
              </label>

              <label class="cleaning-overview-page__field">
                <span>结束时间</span>
                <input v-model="createForm.endTime" type="time" />
              </label>

              <label class="cleaning-overview-page__field cleaning-overview-page__field--full">
                <span>备注</span>
                <textarea v-model="createForm.notes" rows="4" />
              </label>
            </div>

            <div class="cleaning-overview-page__modal-actions">
              <ion-button fill="outline" @click="handleCloseCreateTask">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSubmitCreateTask">
                {{ submitting ? '提交中...' : '创建任务' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { onIonViewWillEnter } from '@ionic/vue'
import {
  assignCleaningTask,
  completeCleaningTask,
  createCleaningTask,
  generateCleaningTasks,
  getCalendarViewData,
  getCleaners,
  getCleaningTaskById,
  type CleanerDTO,
  type CleaningTaskDTO,
} from '@/api/cleaning'
import { getAllRoomTypesWithRooms, type RoomTypeWithRoomsDTO } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import {
  buildDateWindow,
  buildEstimatedTime,
  formatDate,
  getCleaningTaskStatusClass,
  getCleaningTaskStatusText,
  getCleaningTaskTypeText,
  getTodayDate,
  shiftDate,
} from '@/utils/accommodation'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

interface RoomCalendarItem {
  roomId: number
  roomNumber: string
  roomType: string
}

interface RoomGroup {
  roomType: string
  rooms: RoomCalendarItem[]
}

interface CreateTaskForm {
  roomId: number
  roomNumber: string
  roomType: string
  taskDate: string
  taskType: string
  cleanerIdText: string
  startTime: string
  endTime: string
  notes: string
}

const userStore = useUserStore()

const selectedDate = ref(getTodayDate())
const statusFilter = ref('all')
const roomItems = ref<RoomCalendarItem[]>([])
const cleaners = ref<CleanerDTO[]>([])
const tasks = ref<CleaningTaskDTO[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const taskDetailOpen = ref(false)
const createModalOpen = ref(false)
const selectedTask = ref<CleaningTaskDTO | null>(null)
const selectedCleanerIdText = ref('')
const todayDate = getTodayDate()

const createForm = ref<CreateTaskForm>(createDefaultTaskForm())

const dateWindow = computed(() => {
  return buildDateWindow(selectedDate.value, 7)
})

const windowStartDate = computed(() => {
  return dateWindow.value[0]?.date || selectedDate.value
})

const windowEndDate = computed(() => {
  return dateWindow.value[dateWindow.value.length - 1]?.date || selectedDate.value
})

const roomGroups = computed<RoomGroup[]>(() => {
  const groupMap = new Map<string, RoomCalendarItem[]>()

  for (const item of roomItems.value) {
    const group = groupMap.get(item.roomType) || []
    group.push(item)
    groupMap.set(item.roomType, group)
  }

  const groups: RoomGroup[] = []
  for (const [roomType, rooms] of groupMap.entries()) {
    rooms.sort((left, right) => left.roomNumber.localeCompare(right.roomNumber, 'zh-CN'))
    groups.push({ roomType, rooms })
  }

  groups.sort((left, right) => left.roomType.localeCompare(right.roomType, 'zh-CN'))
  return groups
})

function createDefaultTaskForm(): CreateTaskForm {
  return {
    roomId: 0,
    roomNumber: '',
    roomType: '',
    taskDate: getTodayDate(),
    taskType: '',
    cleanerIdText: '',
    startTime: '10:00',
    endTime: '16:00',
    notes: '',
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function loadRooms() {
  const response = await getAllRoomTypesWithRooms()
  if (!response.success || !response.data) {
    throw new Error(response.message || '加载房间失败')
  }

  const nextItems: RoomCalendarItem[] = []

  for (const roomType of response.data as RoomTypeWithRoomsDTO[]) {
    for (const room of roomType.rooms || []) {
      nextItems.push({
        roomId: room.id,
        roomNumber: room.roomNumber,
        roomType: roomType.name,
      })
    }
  }

  roomItems.value = nextItems
}

async function loadCleaners() {
  const response = await getCleaners()
  if (!response.success || !response.data) {
    throw new Error(response.message || '加载保洁员失败')
  }
  cleaners.value = response.data
}

async function loadTasks() {
  const response = await getCalendarViewData({
    startDate: windowStartDate.value,
    endDate: windowEndDate.value,
    status: statusFilter.value === 'all' ? undefined : statusFilter.value,
  })

  if (!response.success || !response.data) {
    throw new Error(response.message || '加载保洁任务失败')
  }

  const taskList: CleaningTaskDTO[] = []
  for (const taskItems of Object.values(response.data.tasks)) {
    for (const task of taskItems) {
      taskList.push(task)
    }
  }
  tasks.value = taskList
}

async function loadPageData() {
  loading.value = true
  errorMessage.value = ''

  try {
    await Promise.all([loadRooms(), loadCleaners(), loadTasks()])
  } catch (error) {
    const message = resolveWarningMessage(error, '加载保洁概览失败')
    errorMessage.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  } finally {
    loading.value = false
  }
}

function getTasksForCell(roomId: number, date: string) {
  return tasks.value.filter((task) => task.roomId === roomId && task.taskDate === date)
}

async function handleOpenTaskDetail(taskId: number) {
  try {
    const response = await getCleaningTaskById(taskId)
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载任务详情失败')
    }
    selectedTask.value = response.data
  } catch (error) {
    const matchedTask = tasks.value.find((item) => item.id === taskId) || null
    selectedTask.value = matchedTask
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载任务详情失败'))
    }
  }

  selectedCleanerIdText.value = selectedTask.value?.cleanerId ? String(selectedTask.value.cleanerId) : ''
  taskDetailOpen.value = true
}

function handleCloseTaskDetail() {
  taskDetailOpen.value = false
  selectedTask.value = null
  selectedCleanerIdText.value = ''
}

function handleOpenCreateTask(room: RoomCalendarItem, taskDate: string) {
  if (taskDate < todayDate) {
    showWarningToast('历史日期不支持新增任务')
    return
  }

  createForm.value = {
    roomId: room.roomId,
    roomNumber: room.roomNumber,
    roomType: room.roomType,
    taskDate,
    taskType: '',
    cleanerIdText: '',
    startTime: '10:00',
    endTime: '16:00',
    notes: '',
  }
  createModalOpen.value = true
}

function handleCloseCreateTask() {
  createModalOpen.value = false
  createForm.value = createDefaultTaskForm()
}

async function handleSubmitCreateTask() {
  if (!createForm.value.roomId) {
    showWarningToast('未选择房间')
    return
  }
  if (!createForm.value.taskType) {
    showWarningToast('请选择任务类型')
    return
  }

  submitting.value = true
  try {
    const cleanerId = createForm.value.cleanerIdText ? Number(createForm.value.cleanerIdText) : undefined
    const response = await createCleaningTask({
      taskDate: createForm.value.taskDate,
      roomId: createForm.value.roomId,
      taskType: createForm.value.taskType,
      cleanerId,
      estimatedTime: buildEstimatedTime(createForm.value.startTime, createForm.value.endTime),
      notes: createForm.value.notes.trim() || undefined,
    })

    if (!response.success) {
      throw new Error(response.message || '创建任务失败')
    }

    showSuccessToast('任务已创建')
    handleCloseCreateTask()
    await loadTasks()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '创建任务失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleAssignTask() {
  if (!selectedTask.value) {
    return
  }
  if (!selectedCleanerIdText.value) {
    showWarningToast('请选择保洁员')
    return
  }

  submitting.value = true
  try {
    const response = await assignCleaningTask(selectedTask.value.id, Number(selectedCleanerIdText.value))
    if (!response.success) {
      throw new Error(response.message || '分配任务失败')
    }

    showSuccessToast('任务已分配')
    handleCloseTaskDetail()
    await loadTasks()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '分配任务失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleCompleteTask() {
  if (!selectedTask.value) {
    return
  }

  const approverId = userStore.currentUser?.id
  if (!approverId) {
    showWarningToast('未获取到当前审批人信息')
    return
  }

  submitting.value = true
  try {
    const response = await completeCleaningTask(selectedTask.value.id, approverId)
    if (!response.success) {
      throw new Error(response.message || '完成任务失败')
    }

    showSuccessToast('任务已完成')
    handleCloseTaskDetail()
    await loadTasks()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '完成任务失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleGenerateTasks() {
  loading.value = true
  try {
    const response = await generateCleaningTasks(windowStartDate.value, windowEndDate.value)
    if (!response.success || !response.data) {
      throw new Error(response.message || '生成任务失败')
    }

    showSuccessToast(
      `已处理 ${response.data.processedReservations} 条预订，新增 ${response.data.createdCount} 条任务`,
    )
    await loadTasks()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '生成任务失败'))
    }
  } finally {
    loading.value = false
  }
}

async function handleShiftWindow(offsetDays: number) {
  selectedDate.value = shiftDate(selectedDate.value, offsetDays)
  await loadTasks()
}

async function handleGoToday() {
  selectedDate.value = getTodayDate()
  await loadTasks()
}

async function handleDateInput(event: Event) {
  const target = event.target as HTMLInputElement | null
  if (!target?.value) {
    return
  }
  selectedDate.value = target.value
  await loadTasks()
}

async function handleStatusChange(event: Event) {
  const target = event.target as HTMLSelectElement | null
  if (!target) {
    return
  }
  statusFilter.value = target.value
  await loadTasks()
}

async function handleRefresh(event: CustomEvent) {
  await loadPageData()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.cleaning-overview-page {
  display: block;
}

.cleaning-overview-page__hero {
  margin-top: 4px;
}

.cleaning-overview-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.cleaning-overview-page__toolbar-card,
.cleaning-overview-page__detail-card {
  display: grid;
  gap: 14px;
}

.cleaning-overview-page__toolbar-row,
.cleaning-overview-page__modal-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.cleaning-overview-page__filter-grid,
.cleaning-overview-page__detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.cleaning-overview-page__field {
  display: grid;
  gap: 8px;
}

.cleaning-overview-page__field--full {
  grid-column: 1 / -1;
}

.cleaning-overview-page__field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.cleaning-overview-page__field input,
.cleaning-overview-page__field select,
.cleaning-overview-page__field textarea {
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-heading);
  font: inherit;
}

.cleaning-overview-page__field textarea {
  min-height: 96px;
  resize: vertical;
}

.cleaning-overview-page__error {
  color: var(--ion-color-danger);
}

.cleaning-overview-page__section-header {
  align-items: flex-start;
}

.cleaning-overview-page__group-list {
  margin-top: 16px;
}

.cleaning-overview-page__group-card,
.cleaning-overview-page__room-card {
  display: grid;
  gap: 12px;
}

.cleaning-overview-page__group-header,
.cleaning-overview-page__room-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.cleaning-overview-page__group-header span,
.cleaning-overview-page__room-header span {
  color: var(--app-muted);
  font-size: 12px;
}

.cleaning-overview-page__date-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.cleaning-overview-page__date-cell {
  flex: 0 0 132px;
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(16, 35, 63, 0.04);
}

.cleaning-overview-page__date-title {
  display: grid;
  gap: 4px;
}

.cleaning-overview-page__date-title strong {
  color: var(--app-heading);
  font-size: 14px;
}

.cleaning-overview-page__date-title span {
  color: var(--app-muted);
  font-size: 12px;
}

.cleaning-overview-page__task-stack {
  display: grid;
  gap: 8px;
}

.cleaning-overview-page__task-badge,
.cleaning-overview-page__empty-action {
  min-height: 40px;
  padding: 8px 10px;
  border: none;
  border-radius: 14px;
  font: inherit;
  color: #fff;
}

.cleaning-overview-page__task-badge.is-expired {
  background: #94a3b8;
}

.cleaning-overview-page__task-badge.is-pending {
  background: #3b82f6;
}

.cleaning-overview-page__task-badge.is-assigned {
  background: #ef4444;
}

.cleaning-overview-page__task-badge.is-progress {
  background: #f59e0b;
}

.cleaning-overview-page__task-badge.is-completed {
  background: #22c55e;
}

.cleaning-overview-page__empty-action {
  border: 1px dashed rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-muted);
}

.cleaning-overview-page__modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

@media (max-width: 520px) {
  .cleaning-overview-page__filter-grid,
  .cleaning-overview-page__detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
