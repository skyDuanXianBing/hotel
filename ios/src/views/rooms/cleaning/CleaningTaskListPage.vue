<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.roomsCleaningOverview" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.RoomsCleaningTasks') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page cleaning-task-list-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('iosStage5.cleaning.pullTasks')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero cleaning-task-list-page__hero">
        <p class="mobile-note cleaning-task-list-page__eyebrow">{{ $t('iosStage5.cleaning.roomOperations') }}</p>
        <h1 class="mobile-title">{{ $t('routes.RoomsCleaningTasks') }}</h1>
        <p class="mobile-subtitle">{{ $t('iosStage5.cleaning.taskListSubtitle') }}</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ $t('iosStage5.cleaning.resultCount') }} {{ tasks.length }}</span>
          <span class="mobile-chip">{{ $t('iosStage5.cleaning.totalCount') }} {{ total }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card cleaning-task-list-page__filter-card">
          <div class="mobile-inline-row cleaning-task-list-page__section-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('iosStage5.cleaning.filters') }}</h2>
              <p class="mobile-note">{{ $t('iosStage5.cleaning.filterHint') }}</p>
            </div>
            <ion-button fill="clear" size="small" @click="handleToggleAdvancedFilters">
              {{ showAdvancedFilters ? $t('iosStage5.cleaning.collapse') : $t('iosStage5.cleaning.expand') }}
            </ion-button>
          </div>

          <div class="cleaning-task-list-page__filter-grid">
            <label class="cleaning-task-list-page__field cleaning-task-list-page__field--full">
              <span>{{ $t('iosStage5.cleaning.keyword') }}</span>
              <input v-model="filters.search" :placeholder="$t('iosStage5.cleaning.searchPlaceholder')" type="text" />
            </label>
            <label class="cleaning-task-list-page__field">
              <span>{{ $t('iosStage5.cleanerWorkspace.taskType') }}</span>
              <select v-model="filters.taskType">
                <option value="">{{ $t('iosStage5.cleaning.allTaskTypes') }}</option>
                <option value="checkout">{{ $t('iosStage5.cleaning.type.checkout') }}</option>
                <option value="daily">{{ $t('iosStage5.cleaning.type.daily') }}</option>
                <option value="deep">{{ $t('iosStage5.cleaning.type.deep') }}</option>
              </select>
            </label>
            <label class="cleaning-task-list-page__field">
              <span>{{ $t('iosStage5.cleanerWorkspace.taskStatus') }}</span>
              <select v-model="filters.status">
                <option value="">{{ $t('iosStage5.cleaning.allStatuses') }}</option>
                <option value="expired">{{ $t('iosStage5.cleaning.status.expired') }}</option>
                <option value="pending">{{ $t('iosStage5.cleaning.status.pending') }}</option>
                <option value="assigned">{{ $t('iosStage5.cleaning.status.assigned') }}</option>
                <option value="in_progress">{{ $t('iosStage5.cleaning.status.inProgress') }}</option>
                <option value="completed">{{ $t('iosStage5.cleaning.status.completed') }}</option>
              </select>
            </label>
          </div>

          <div v-if="showAdvancedFilters" class="cleaning-task-list-page__filter-grid">
            <label class="cleaning-task-list-page__field">
              <span>{{ $t('iosStage5.cleaning.startDate') }}</span>
              <input v-model="filters.startDate" type="date" />
            </label>
            <label class="cleaning-task-list-page__field">
              <span>{{ $t('iosStage5.cleaning.endDate') }}</span>
              <input v-model="filters.endDate" type="date" />
            </label>
            <label class="cleaning-task-list-page__field cleaning-task-list-page__field--full">
              <span>{{ $t('iosStage5.cleaning.roomType') }}</span>
              <select v-model="filters.roomTypeIdText">
                <option value="">{{ $t('iosStage5.cleaning.allRoomTypes') }}</option>
                <option v-for="roomType in roomTypes" :key="roomType.id" :value="String(roomType.id)">
                  {{ roomType.name }}
                </option>
              </select>
            </label>
          </div>

          <div class="cleaning-task-list-page__actions">
            <ion-button fill="outline" @click="handleResetFilters">{{ $t('stage5.common.actions.reset') }}</ion-button>
            <ion-button @click="handleSearch">{{ $t('stage5.common.actions.query') }}</ion-button>
          </div>

          <p v-if="errorMessage" class="mobile-note cleaning-task-list-page__error">{{ errorMessage }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row cleaning-task-list-page__section-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('iosStage5.cleaning.taskCards') }}</h2>
              <p class="mobile-note">{{ $t('iosStage5.cleaning.taskCardsHint') }}</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="tasks.length > 0" class="mobile-list cleaning-task-list-page__list">
            <article v-for="task in tasks" :key="task.id" class="cleaning-task-list-page__task-card">
              <div class="cleaning-task-list-page__task-header">
                <div>
                  <strong>{{ task.roomType }} · {{ task.roomNumber }}</strong>
                  <p>{{ task.taskDate }} · {{ getTaskTypeLabel(task.taskType) }}</p>
                </div>
                <span :class="getCleaningTaskStatusClass(task.status)">{{ getStatusLabel(task.status) }}</span>
              </div>

              <div class="cleaning-task-list-page__task-grid">
                <span>{{ $t('iosStage5.cleaning.cleaner') }} {{ task.cleanerName || $t('iosStage5.cleaning.unassigned') }}</span>
                <span>{{ $t('iosStage5.cleaning.approver') }} {{ task.approverName || '-' }}</span>
                <span>{{ $t('iosStage5.cleaning.estimatedTime') }} {{ task.estimatedTime || '-' }}</span>
                <span>{{ $t('iosStage5.cleaning.updatedAt') }} {{ formatDateTime(task.updatedAt) }}</span>
              </div>

              <div class="cleaning-task-list-page__task-actions">
                <ion-button fill="outline" size="small" @click="handleOpenDetail(task.id)">{{ $t('iosStage5.cleaning.viewDetails') }}</ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">{{ $t('iosStage5.cleaning.noTasksForFilter') }}</p>

          <div v-if="hasMore" class="cleaning-task-list-page__load-more">
            <ion-button fill="outline" :disabled="loadingMore" @click="handleLoadMore">
              {{ loadingMore ? $t('iosStage5.common.loading') : $t('iosStage5.cleaning.loadMore') }}
            </ion-button>
          </div>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">{{ $t('iosStage5.cleaning.firstReleaseNotes') }}</h2>
          <ul class="mobile-bullet-list">
            <li>{{ $t('iosStage5.cleaning.taskListScope') }}</li>
            <li>{{ $t('iosStage5.cleaning.exportLater') }}</li>
          </ul>
        </section>
      </div>

      <ion-modal :is-open="detailOpen" @didDismiss="handleCloseDetail">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ $t('iosStage5.cleaning.taskDetails') }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseDetail">{{ $t('stage5.common.actions.close') }}</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page cleaning-task-list-page__modal-page">
          <section v-if="selectedTask" class="mobile-card cleaning-task-list-page__detail-card">
            <div class="cleaning-task-list-page__detail-grid">
              <span>{{ $t('iosStage5.cleaning.date') }} {{ selectedTask.taskDate }}</span>
              <span>{{ $t('iosStage5.cleaning.roomType') }} {{ selectedTask.roomType }}</span>
              <span>{{ $t('iosStage5.cleaning.room') }} {{ selectedTask.roomNumber }}</span>
              <span>{{ $t('iosStage5.cleaning.statusLabel') }} {{ getStatusLabel(selectedTask.status) }}</span>
              <span>{{ $t('iosStage5.cleanerWorkspace.taskType') }} {{ getTaskTypeLabel(selectedTask.taskType) }}</span>
              <span>{{ $t('iosStage5.cleaning.estimatedTime') }} {{ selectedTask.estimatedTime || '-' }}</span>
              <span>{{ $t('iosStage5.cleaning.cleaner') }} {{ selectedTask.cleanerName || $t('iosStage5.cleaning.unassigned') }}</span>
              <span>{{ $t('iosStage5.cleaning.approver') }} {{ selectedTask.approverName || '-' }}</span>
              <span>{{ $t('iosStage5.cleaning.createdAt') }} {{ formatDateTime(selectedTask.createdAt) }}</span>
              <span>{{ $t('iosStage5.cleaning.completedAt') }} {{ formatDateTime(selectedTask.completeTime) }}</span>
            </div>

            <label v-if="selectedTask.status === 'pending'" class="cleaning-task-list-page__field">
              <span>{{ $t('iosStage5.cleaning.assignCleaner') }}</span>
              <select v-model="selectedCleanerIdText">
                <option value="">{{ $t('iosStage5.cleaning.selectCleaner') }}</option>
                <option v-for="cleaner in cleaners" :key="cleaner.id" :value="String(cleaner.id)">
                  {{ cleaner.name }}
                </option>
              </select>
            </label>

            <p v-if="selectedTask.notes" class="mobile-note">{{ $t('iosStage5.cleaning.notesLabel') }}{{ selectedTask.notes }}</p>

            <div class="cleaning-task-list-page__actions">
              <ion-button fill="outline" @click="handleCloseDetail">{{ $t('stage5.common.actions.close') }}</ion-button>
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
  getCleaners,
  getCleaningTaskById,
  getCleaningTasks,
  type CleanerDTO,
  type CleaningTaskDTO,
} from '@/api/cleaning'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import {
  formatDateTime,
  getCleaningTaskStatusClass,
  getTodayDate,
  shiftDate,
} from '@/utils/accommodation'
import { CLEANER_STATUS_LABELS, CLEANER_TASK_TYPE_LABELS } from '@/constants/cleaner'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

interface CleaningTaskFilterState {
  search: string
  taskType: string
  status: string
  roomTypeIdText: string
  startDate: string
  endDate: string
}

const DEFAULT_PAGE_SIZE = 20

const userStore = useUserStore()
const { t } = useI18n()

const filters = ref<CleaningTaskFilterState>(createDefaultFilters())
const showAdvancedFilters = ref(false)
const roomTypes = ref<RoomTypeDTO[]>([])
const cleaners = ref<CleanerDTO[]>([])
const tasks = ref<CleaningTaskDTO[]>([])
const total = ref(0)
const page = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const detailOpen = ref(false)
const selectedTask = ref<CleaningTaskDTO | null>(null)
const selectedCleanerIdText = ref('')

const hasMore = computed(() => {
  return tasks.value.length < total.value
})

function createDefaultFilters(): CleaningTaskFilterState {
  return {
    search: '',
    taskType: '',
    status: '',
    roomTypeIdText: '',
    startDate: shiftDate(getTodayDate(), -7),
    endDate: getTodayDate(),
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

async function loadOptions() {
  const [roomTypeResponse, cleanerResponse] = await Promise.all([getAllRoomTypes(), getCleaners()])
  if (!roomTypeResponse.success || !roomTypeResponse.data) {
    throw new Error(roomTypeResponse.message || t('iosStage5.cleaning.roomTypesLoadFailed'))
  }
  if (!cleanerResponse.success || !cleanerResponse.data) {
    throw new Error(cleanerResponse.message || t('iosStage5.cleaning.cleanersLoadFailed'))
  }

  roomTypes.value = roomTypeResponse.data
  cleaners.value = cleanerResponse.data
}

async function loadTasks(reset = true) {
  if (reset) {
    page.value = 0
    loading.value = true
  } else {
    loadingMore.value = true
  }

  errorMessage.value = ''

  try {
    const roomTypeId = filters.value.roomTypeIdText ? Number(filters.value.roomTypeIdText) : undefined
    const response = await getCleaningTasks({
      startDate: filters.value.startDate || undefined,
      endDate: filters.value.endDate || undefined,
      status: filters.value.status || undefined,
      taskType: filters.value.taskType || undefined,
      roomTypeId,
      search: filters.value.search.trim() || undefined,
      page: page.value,
      size: DEFAULT_PAGE_SIZE,
      sortBy: 'taskDate',
      sortDirection: 'DESC',
    })

    if (!response.success || !response.data) {
      throw new Error(response.message || t('iosStage5.cleaning.taskListLoadFailed'))
    }

    total.value = response.data.totalElements
    if (reset) {
      tasks.value = response.data.content
    } else {
      tasks.value = [...tasks.value, ...response.data.content]
    }
  } catch (error) {
    const message = resolveWarningMessage(error, t('iosStage5.cleaning.taskListLoadFailed'))
    errorMessage.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadPageData() {
  loading.value = true
  errorMessage.value = ''

  try {
    await loadOptions()
    await loadTasks(true)
  } catch (error) {
    const message = resolveWarningMessage(error, t('iosStage5.cleaning.taskListLoadFailed'))
    errorMessage.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
    loading.value = false
  }
}

async function handleOpenDetail(taskId: number) {
  try {
    const response = await getCleaningTaskById(taskId)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('iosStage5.cleaning.taskDetailLoadFailed'))
    }
    selectedTask.value = response.data
  } catch (error) {
    selectedTask.value = tasks.value.find((item) => item.id === taskId) || null
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('iosStage5.cleaning.taskDetailLoadFailed')))
    }
  }

  selectedCleanerIdText.value = selectedTask.value?.cleanerId ? String(selectedTask.value.cleanerId) : ''
  detailOpen.value = true
}

function handleCloseDetail() {
  detailOpen.value = false
  selectedTask.value = null
  selectedCleanerIdText.value = ''
}

function handleToggleAdvancedFilters() {
  showAdvancedFilters.value = !showAdvancedFilters.value
}

function handleResetFilters() {
  filters.value = createDefaultFilters()
}

async function handleSearch() {
  await loadTasks(true)
}

async function handleLoadMore() {
  if (!hasMore.value) {
    return
  }
  page.value += 1
  await loadTasks(false)
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
    handleCloseDetail()
    await loadTasks(true)
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
    handleCloseDetail()
    await loadTasks(true)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('iosStage5.cleaning.completeTaskFailed')))
    }
  } finally {
    submitting.value = false
  }
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
.cleaning-task-list-page {
  display: block;
}

.cleaning-task-list-page__hero {
  margin-top: 4px;
}

.cleaning-task-list-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.cleaning-task-list-page__filter-card,
.cleaning-task-list-page__detail-card {
  display: grid;
  gap: 14px;
}

.cleaning-task-list-page__section-header {
  align-items: flex-start;
}

.cleaning-task-list-page__filter-grid,
.cleaning-task-list-page__detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.cleaning-task-list-page__field {
  display: grid;
  gap: 8px;
}

.cleaning-task-list-page__field--full {
  grid-column: 1 / -1;
}

.cleaning-task-list-page__field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.cleaning-task-list-page__field input,
.cleaning-task-list-page__field select {
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-heading);
  font: inherit;
}

.cleaning-task-list-page__actions,
.cleaning-task-list-page__task-actions,
.cleaning-task-list-page__load-more {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.cleaning-task-list-page__error {
  color: var(--ion-color-danger);
}

.cleaning-task-list-page__list {
  margin-top: 16px;
}

.cleaning-task-list-page__task-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.cleaning-task-list-page__task-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.cleaning-task-list-page__task-header strong,
.cleaning-task-list-page__task-header p {
  margin: 0;
}

.cleaning-task-list-page__task-header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.cleaning-task-list-page__task-header span {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.cleaning-task-list-page__task-header span.is-expired {
  background: rgba(148, 163, 184, 0.18);
  color: #64748b;
}

.cleaning-task-list-page__task-header span.is-pending {
  background: rgba(59, 130, 246, 0.14);
  color: #2563eb;
}

.cleaning-task-list-page__task-header span.is-assigned,
.cleaning-task-list-page__task-header span.is-progress {
  background: rgba(245, 158, 11, 0.14);
  color: #d97706;
}

.cleaning-task-list-page__task-header span.is-completed {
  background: rgba(34, 197, 94, 0.14);
  color: #16a34a;
}

.cleaning-task-list-page__task-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  margin-top: 14px;
  color: var(--app-muted);
  font-size: 12px;
}

.cleaning-task-list-page__modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

@media (max-width: 520px) {
  .cleaning-task-list-page__filter-grid,
  .cleaning-task-list-page__detail-grid,
  .cleaning-task-list-page__task-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
