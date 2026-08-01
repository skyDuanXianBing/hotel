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

.cleaning-task-list-page .mobile-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.cleaning-task-list-page .mobile-chip {
  min-width: 0;
  min-height: 24px;
  padding: 2px 10px;
  border-color: rgba(var(--ion-color-primary-rgb), 0.1);
  background: rgba(var(--ion-color-primary-rgb), 0.07);
  color: rgba(var(--ion-color-primary-rgb), 0.88);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
  white-space: normal;
  overflow-wrap: anywhere;
}

.cleaning-task-list-page .mobile-stack {
  gap: 18px;
  margin-top: 10px;
  padding-bottom: 4px;
}

.cleaning-task-list-page .mobile-stack > .mobile-card {
  min-width: 0;
  padding: 22px 16px 24px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.cleaning-task-list-page__filter-card,
.cleaning-task-list-page__detail-card {
  display: grid;
  gap: 16px;
}

.cleaning-task-list-page__section-header {
  align-items: flex-start;
}

.cleaning-task-list-page__section-header > div {
  min-width: 0;
}

.cleaning-task-list-page__section-header .mobile-section-title,
.cleaning-task-list-page .mobile-stack > .mobile-card > .mobile-section-title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 22px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
  letter-spacing: 0;
}

.cleaning-task-list-page__section-header .mobile-note {
  margin-top: 5px;
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.cleaning-task-list-page__section-header ion-button,
.cleaning-task-list-page__section-header ion-spinner {
  flex-shrink: 0;
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
  box-sizing: border-box;
  width: 100%;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid rgba(130, 143, 165, 0.24);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.88);
  color: var(--ios-pms-text-primary);
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

.cleaning-task-list-page__actions ion-button,
.cleaning-task-list-page__task-actions ion-button,
.cleaning-task-list-page__load-more ion-button {
  min-height: 29px;
  margin: 0;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 9px;
  --box-shadow: none;
  font-size: 14px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.cleaning-task-list-page__actions ion-button::part(native),
.cleaning-task-list-page__task-actions ion-button::part(native),
.cleaning-task-list-page__load-more ion-button::part(native) {
  min-height: 29px;
  border: 1px solid rgba(130, 143, 165, 0.24);
  border-radius: 9px;
  box-shadow: none;
  line-height: 1.2;
}

.cleaning-task-list-page__actions ion-button[fill='outline'],
.cleaning-task-list-page__task-actions ion-button[fill='outline'],
.cleaning-task-list-page__load-more ion-button[fill='outline'] {
  --background: rgba(255, 255, 255, 0.88);
  --color: var(--ios-pms-primary);
  --border-color: rgba(130, 143, 165, 0.24);
}

.cleaning-task-list-page__error {
  color: var(--ion-color-danger);
}

.cleaning-task-list-page__list {
  margin-top: 21px;
  gap: 17px;
}

.cleaning-task-list-page__task-card {
  min-width: 0;
  padding: 14px 15px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.88);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.88) inset,
    0 8px 18px rgba(77, 98, 145, 0.035);
}

.cleaning-task-list-page__task-header {
  min-width: 0;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.cleaning-task-list-page__task-header > div {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.cleaning-task-list-page__task-header strong,
.cleaning-task-list-page__task-header p {
  margin: 0;
}

.cleaning-task-list-page__task-header strong {
  color: var(--ios-pms-text-primary);
  font-size: 20px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.15;
  letter-spacing: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cleaning-task-list-page__task-header p {
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.35;
  letter-spacing: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cleaning-task-list-page__task-header span {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  margin-top: -2px;
  padding: 0 10px;
  border-radius: var(--ios-pms-radius-pill);
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  white-space: nowrap;
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
  color: var(--ios-pms-text-muted);
  font-size: 12px;
}

.cleaning-task-list-page__task-grid span,
.cleaning-task-list-page__detail-grid span,
.cleaning-task-list-page__detail-card .mobile-note,
.cleaning-task-list-page .mobile-bullet-list li {
  min-width: 0;
  overflow-wrap: anywhere;
  line-height: 1.45;
}

.cleaning-task-list-page .mobile-bullet-list {
  margin-top: 14px;
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

@media (max-width: 374px) {
  .cleaning-task-list-page {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .cleaning-task-list-page .mobile-stack > .mobile-card {
    padding-right: 14px;
    padding-left: 14px;
  }

  .cleaning-task-list-page__section-header .mobile-section-title,
  .cleaning-task-list-page .mobile-stack > .mobile-card > .mobile-section-title {
    font-size: 20px;
  }

  .cleaning-task-list-page__task-card {
    padding: 13px 13px 14px;
  }

  .cleaning-task-list-page__task-header strong {
    font-size: 19px;
  }
}
</style>
