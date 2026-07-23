<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.rooms" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.RoomsCleaningOverview') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page cleaning-overview-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('iosStage5.cleaning.pullOverview')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero cleaning-overview-page__hero">
        <p class="mobile-note cleaning-overview-page__eyebrow">{{ $t('iosStage5.cleaning.roomOperations') }}</p>
        <h1 class="mobile-title">{{ $t('routes.RoomsCleaningOverview') }}</h1>
        <p class="mobile-subtitle">{{ $t('iosStage5.cleaning.overviewSubtitle') }}</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ formatDate(windowStartDate) }} - {{ formatDate(windowEndDate) }}</span>
          <span class="mobile-chip">{{ $t('iosStage5.cleaning.room') }} {{ roomItems.length }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card cleaning-overview-page__toolbar-card">
          <div class="cleaning-overview-page__toolbar-row">
            <ion-button fill="outline" size="small" @click="handleShiftWindow(-7)">{{ $t('iosStage5.cleaning.previousWeek') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleGoToday">{{ $t('iosStage5.cleaning.today') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftWindow(7)">{{ $t('iosStage5.cleaning.nextWeek') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleGenerateTasks">{{ $t('iosStage5.cleaning.generateTasks') }}</ion-button>
          </div>

          <div class="cleaning-overview-page__filter-grid">
            <label class="cleaning-overview-page__field">
              <span>{{ $t('iosStage5.cleaning.windowStart') }}</span>
              <input :value="selectedDate" type="date" @input="handleDateInput" />
            </label>

            <label class="cleaning-overview-page__field">
              <span>{{ $t('iosStage5.cleaning.statusFilter') }}</span>
              <select :value="statusFilter" @change="handleStatusChange">
                <option value="all">{{ $t('iosStage5.cleaning.allTasks') }}</option>
                <option value="pending">{{ $t('iosStage5.cleaning.status.pending') }}</option>
                <option value="assigned">{{ $t('iosStage5.cleaning.status.assigned') }}</option>
                <option value="in_progress">{{ $t('iosStage5.cleaning.status.inProgress') }}</option>
                <option value="completed">{{ $t('iosStage5.cleaning.status.completed') }}</option>
                <option value="expired">{{ $t('iosStage5.cleaning.status.expired') }}</option>
              </select>
            </label>
          </div>

          <p v-if="errorMessage" class="mobile-note cleaning-overview-page__error">{{ errorMessage }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row cleaning-overview-page__section-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('iosStage5.cleaning.taskWindow') }}</h2>
              <p class="mobile-note">{{ $t('iosStage5.cleaning.taskHint') }}</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="roomGroups.length > 0" class="mobile-list cleaning-overview-page__group-list">
            <section v-for="group in roomGroups" :key="group.roomType" class="cleaning-overview-page__group-card">
              <div class="cleaning-overview-page__group-header">
                <strong>{{ group.roomType }}</strong>
                <span>{{ group.rooms.length }} {{ $t('iosStage5.cleaning.roomCountUnit') }}</span>
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
                          {{ getStatusLabel(task.status) }}
                        </button>

                        <button
                          v-if="getTasksForCell(room.roomId, day.date).length <= 0"
                          class="cleaning-overview-page__empty-action"
                          type="button"
                          @click="handleOpenCreateTask(room, day.date)"
                        >
                          {{ day.date < todayDate ? $t('iosStage5.cleaning.historyDate') : $t('iosStage5.cleaning.addTask') }}
                        </button>
                      </div>
                    </article>
                  </div>
                </article>
              </div>
            </section>
          </div>

          <p v-else-if="!loading" class="mobile-note">{{ $t('iosStage5.cleaning.noRoomData') }}</p>
        </section>
      </div>

      <ion-modal :is-open="taskDetailOpen" @didDismiss="handleCloseTaskDetail">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ $t('iosStage5.cleaning.taskDetails') }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseTaskDetail">{{ $t('stage5.common.actions.close') }}</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page cleaning-overview-page__modal-page">
          <section v-if="selectedTask" class="mobile-card cleaning-overview-page__detail-card">
            <div class="cleaning-overview-page__detail-grid">
              <span>{{ $t('iosStage5.cleaning.roomType') }} {{ selectedTask.roomType }}</span>
              <span>{{ $t('iosStage5.cleaning.room') }} {{ selectedTask.roomNumber }}</span>
              <span>{{ $t('iosStage5.cleaning.taskDate') }} {{ selectedTask.taskDate }}</span>
              <span>{{ $t('iosStage5.cleanerWorkspace.taskType') }} {{ getTaskTypeLabel(selectedTask.taskType) }}</span>
              <span>{{ $t('iosStage5.cleanerWorkspace.taskStatus') }} {{ getStatusLabel(selectedTask.status) }}</span>
              <span>{{ $t('iosStage5.cleaning.cleaner') }} {{ selectedTask.cleanerName || $t('iosStage5.cleaning.unassigned') }}</span>
              <span>{{ $t('iosStage5.cleaning.estimatedTime') }} {{ selectedTask.estimatedTime || '-' }}</span>
              <span>{{ $t('iosStage5.cleaning.approver') }} {{ selectedTask.approverName || '-' }}</span>
            </div>

            <label v-if="selectedTask.status === 'pending'" class="cleaning-overview-page__field">
              <span>{{ $t('iosStage5.cleaning.assignCleaner') }}</span>
              <select v-model="selectedCleanerIdText">
                <option value="">{{ $t('iosStage5.cleaning.selectCleaner') }}</option>
                <option v-for="cleaner in cleaners" :key="cleaner.id" :value="String(cleaner.id)">
                  {{ cleaner.name }}
                </option>
              </select>
            </label>

            <p v-if="selectedTask.notes" class="mobile-note">{{ $t('iosStage5.cleaning.notesLabel') }}{{ selectedTask.notes }}</p>

            <div class="cleaning-overview-page__modal-actions">
              <ion-button fill="outline" @click="handleCloseTaskDetail">{{ $t('stage5.common.actions.close') }}</ion-button>
              <ion-button v-if="selectedTask.status === 'pending'" :disabled="submitting" @click="handleAssignTask">
                {{ submitting ? $t('iosStage5.cleaning.assigning') : $t('iosStage5.cleaning.assignTask') }}
              </ion-button>
              <ion-button
                v-if="selectedTask.status === 'assigned' || selectedTask.status === 'in_progress'"
                :disabled="submitting"
                @click="handleCompleteTask"
              >
                {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('iosStage5.cleaning.completeTask') }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="createModalOpen" @didDismiss="handleCloseCreateTask">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ $t('iosStage5.cleaning.addTask') }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseCreateTask">{{ $t('stage5.common.actions.close') }}</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page cleaning-overview-page__modal-page">
          <section class="mobile-card cleaning-overview-page__detail-card">
            <div class="cleaning-overview-page__detail-grid">
              <span>{{ $t('iosStage5.cleaning.roomType') }} {{ createForm.roomType }}</span>
              <span>{{ $t('iosStage5.cleaning.room') }} {{ createForm.roomNumber }}</span>
              <span>{{ $t('iosStage5.cleaning.taskDate') }} {{ createForm.taskDate }}</span>
            </div>

            <div class="cleaning-overview-page__filter-grid">
              <label class="cleaning-overview-page__field">
                <span>{{ $t('iosStage5.cleanerWorkspace.taskType') }}</span>
                <select v-model="createForm.taskType">
                  <option value="">{{ $t('iosStage5.cleaning.selectTaskType') }}</option>
                  <option value="checkout">{{ $t('iosStage5.cleaning.type.checkout') }}</option>
                  <option value="daily">{{ $t('iosStage5.cleaning.type.daily') }}</option>
                  <option value="deep">{{ $t('iosStage5.cleaning.type.deep') }}</option>
                </select>
              </label>

              <label class="cleaning-overview-page__field">
                <span>{{ $t('iosStage5.cleaning.cleaner') }}</span>
                <select v-model="createForm.cleanerIdText">
                  <option value="">{{ $t('iosStage5.cleaning.doNotAssign') }}</option>
                  <option v-for="cleaner in cleaners" :key="cleaner.id" :value="String(cleaner.id)">
                    {{ cleaner.name }}
                  </option>
                </select>
              </label>

              <label class="cleaning-overview-page__field">
                <span>{{ $t('iosStage5.cleaning.startTime') }}</span>
                <input v-model="createForm.startTime" type="time" />
              </label>

              <label class="cleaning-overview-page__field">
                <span>{{ $t('iosStage5.cleaning.endTime') }}</span>
                <input v-model="createForm.endTime" type="time" />
              </label>

              <label class="cleaning-overview-page__field cleaning-overview-page__field--full">
                <span>{{ $t('iosStage5.cleaning.notes') }}</span>
                <textarea v-model="createForm.notes" rows="4" />
              </label>
            </div>

            <div class="cleaning-overview-page__modal-actions">
              <ion-button fill="outline" @click="handleCloseCreateTask">{{ $t('stage5.common.actions.cancel') }}</ion-button>
              <ion-button :disabled="submitting" @click="handleSubmitCreateTask">
                {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('iosStage5.cleaning.createTask') }}
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
import { useI18n } from 'vue-i18n'
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
  getTodayDate,
  shiftDate,
} from '@/utils/accommodation'
import { CLEANER_STATUS_LABELS, CLEANER_TASK_TYPE_LABELS } from '@/constants/cleaner'
import { compareLocalizedText } from '@/utils/formatters'
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
const { t } = useI18n()

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
    rooms.sort((left, right) => compareLocalizedText(left.roomNumber, right.roomNumber))
    groups.push({ roomType, rooms })
  }

  groups.sort((left, right) => compareLocalizedText(left.roomType, right.roomType))
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

function getStatusLabel(status: string) {
  const labelKey = CLEANER_STATUS_LABELS[status]
  return (labelKey ? t(labelKey) : '') || status || t('iosStage5.cleanerWorkspace.unknownStatus')
}

function getTaskTypeLabel(taskType: string) {
  const labelKey = CLEANER_TASK_TYPE_LABELS[taskType]
  return (labelKey ? t(labelKey) : '') || taskType || t('iosStage5.cleanerWorkspace.notSet')
}

async function loadRooms() {
  const response = await getAllRoomTypesWithRooms()
  if (!response.success || !response.data) {
    throw new Error(response.message || t('iosStage5.cleaning.overviewLoadFailed'))
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
    throw new Error(response.message || t('iosStage5.cleaning.cleanersLoadFailed'))
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
    throw new Error(response.message || t('iosStage5.cleaning.tasksLoadFailed'))
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
    const message = resolveWarningMessage(error, t('iosStage5.cleaning.overviewLoadFailed'))
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
      throw new Error(response.message || t('iosStage5.cleaning.taskDetailLoadFailed'))
    }
    selectedTask.value = response.data
  } catch (error) {
    const matchedTask = tasks.value.find((item) => item.id === taskId) || null
    selectedTask.value = matchedTask
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('iosStage5.cleaning.taskDetailLoadFailed')))
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
    showWarningToast(t('iosStage5.cleaning.historyDateNoCreate'))
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
    showWarningToast(t('iosStage5.cleaning.roomMissing'))
    return
  }
  if (!createForm.value.taskType) {
    showWarningToast(t('iosStage5.cleaning.selectTaskType'))
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
      throw new Error(response.message || t('iosStage5.cleaning.createTaskFailed'))
    }

    showSuccessToast(t('iosStage5.cleaning.taskCreated'))
    handleCloseCreateTask()
    await loadTasks()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('iosStage5.cleaning.createTaskFailed')))
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
    showWarningToast(t('iosStage5.cleaning.selectCleanerRequired'))
    return
  }

  submitting.value = true
  try {
    const response = await assignCleaningTask(selectedTask.value.id, Number(selectedCleanerIdText.value))
    if (!response.success) {
      throw new Error(response.message || t('iosStage5.cleaning.assignTaskFailed'))
    }

    showSuccessToast(t('iosStage5.cleaning.taskAssigned'))
    handleCloseTaskDetail()
    await loadTasks()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('iosStage5.cleaning.assignTaskFailed')))
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
    showWarningToast(t('iosStage5.cleaning.approverMissing'))
    return
  }

  submitting.value = true
  try {
    const response = await completeCleaningTask(selectedTask.value.id, approverId)
    if (!response.success) {
      throw new Error(response.message || t('iosStage5.cleaning.completeTaskFailed'))
    }

    showSuccessToast(t('iosStage5.cleanerWorkspace.completed'))
    handleCloseTaskDetail()
    await loadTasks()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('iosStage5.cleaning.completeTaskFailed')))
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
      throw new Error(response.message || t('iosStage5.cleaning.generateTasksFailed'))
    }

    showSuccessToast(
      t('iosStage5.cleaning.generatedSummary', {
        reservations: response.data.processedReservations,
        tasks: response.data.createdCount,
      }),
    )
    await loadTasks()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('iosStage5.cleaning.generateTasksFailed')))
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
