<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.roomsCleaningOverview" />
        </ion-buttons>
        <ion-title class="app-page-header__title">保洁任务列表</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page cleaning-task-list-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新保洁任务" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero cleaning-task-list-page__hero">
        <p class="mobile-note cleaning-task-list-page__eyebrow">住宿保洁</p>
        <h1 class="mobile-title">保洁任务列表</h1>
        <p class="mobile-subtitle">提供移动端筛选、详情、分配与完成闭环，不首发导出明细。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">结果 {{ tasks.length }}</span>
          <span class="mobile-chip">总量 {{ total }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card cleaning-task-list-page__filter-card">
          <div class="mobile-inline-row cleaning-task-list-page__section-header">
            <div>
              <h2 class="mobile-section-title">筛选条件</h2>
              <p class="mobile-note">支持任务类型、状态、房型、日期区间与关键字。</p>
            </div>
            <ion-button fill="clear" size="small" @click="handleToggleAdvancedFilters">
              {{ showAdvancedFilters ? '收起' : '展开' }}
            </ion-button>
          </div>

          <div class="cleaning-task-list-page__filter-grid">
            <label class="cleaning-task-list-page__field cleaning-task-list-page__field--full">
              <span>关键字</span>
              <input v-model="filters.search" placeholder="搜索房号、保洁员或任务备注" type="text" />
            </label>
            <label class="cleaning-task-list-page__field">
              <span>任务类型</span>
              <select v-model="filters.taskType">
                <option value="">全部任务类型</option>
                <option value="checkout">退房</option>
                <option value="daily">日常清洁</option>
                <option value="deep">深度清洁</option>
              </select>
            </label>
            <label class="cleaning-task-list-page__field">
              <span>任务状态</span>
              <select v-model="filters.status">
                <option value="">全部状态</option>
                <option value="expired">已过期</option>
                <option value="pending">待分配</option>
                <option value="assigned">待清洁</option>
                <option value="in_progress">清洁中</option>
                <option value="completed">已完成</option>
              </select>
            </label>
          </div>

          <div v-if="showAdvancedFilters" class="cleaning-task-list-page__filter-grid">
            <label class="cleaning-task-list-page__field">
              <span>开始日期</span>
              <input v-model="filters.startDate" type="date" />
            </label>
            <label class="cleaning-task-list-page__field">
              <span>结束日期</span>
              <input v-model="filters.endDate" type="date" />
            </label>
            <label class="cleaning-task-list-page__field cleaning-task-list-page__field--full">
              <span>房型</span>
              <select v-model="filters.roomTypeIdText">
                <option value="">全部房型</option>
                <option v-for="roomType in roomTypes" :key="roomType.id" :value="String(roomType.id)">
                  {{ roomType.name }}
                </option>
              </select>
            </label>
          </div>

          <div class="cleaning-task-list-page__actions">
            <ion-button fill="outline" @click="handleResetFilters">重置</ion-button>
            <ion-button @click="handleSearch">查询</ion-button>
          </div>

          <p v-if="errorMessage" class="mobile-note cleaning-task-list-page__error">{{ errorMessage }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row cleaning-task-list-page__section-header">
            <div>
              <h2 class="mobile-section-title">任务卡片</h2>
              <p class="mobile-note">点击“查看详情”可进入全屏弹层并处理分配或完成动作。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="tasks.length > 0" class="mobile-list cleaning-task-list-page__list">
            <article v-for="task in tasks" :key="task.id" class="cleaning-task-list-page__task-card">
              <div class="cleaning-task-list-page__task-header">
                <div>
                  <strong>{{ task.roomType }} · {{ task.roomNumber }}</strong>
                  <p>{{ task.taskDate }} · {{ getCleaningTaskTypeText(task.taskType) }}</p>
                </div>
                <span :class="getCleaningTaskStatusClass(task.status)">{{ getCleaningTaskStatusText(task.status) }}</span>
              </div>

              <div class="cleaning-task-list-page__task-grid">
                <span>保洁员 {{ task.cleanerName || '未分配' }}</span>
                <span>审批人 {{ task.approverName || '-' }}</span>
                <span>预计时间 {{ task.estimatedTime || '-' }}</span>
                <span>更新时间 {{ formatDateTime(task.updatedAt) }}</span>
              </div>

              <div class="cleaning-task-list-page__task-actions">
                <ion-button fill="outline" size="small" @click="handleOpenDetail(task.id)">查看详情</ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前筛选条件下暂无保洁任务。</p>

          <div v-if="hasMore" class="cleaning-task-list-page__load-more">
            <ion-button fill="outline" :disabled="loadingMore" @click="handleLoadMore">
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </ion-button>
          </div>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">首版说明</h2>
          <ul class="mobile-bullet-list">
            <li>列表页优先保证查询、详情、分配、完成任务四个核心动作。</li>
            <li>导出明细暂列为二期增强，不阻塞当前移动端闭环。</li>
          </ul>
        </section>
      </div>

      <ion-modal :is-open="detailOpen" @didDismiss="handleCloseDetail">
        <ion-header>
          <ion-toolbar>
            <ion-title>任务详情</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseDetail">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page cleaning-task-list-page__modal-page">
          <section v-if="selectedTask" class="mobile-card cleaning-task-list-page__detail-card">
            <div class="cleaning-task-list-page__detail-grid">
              <span>日期 {{ selectedTask.taskDate }}</span>
              <span>房型 {{ selectedTask.roomType }}</span>
              <span>房间 {{ selectedTask.roomNumber }}</span>
              <span>状态 {{ getCleaningTaskStatusText(selectedTask.status) }}</span>
              <span>任务类型 {{ getCleaningTaskTypeText(selectedTask.taskType) }}</span>
              <span>预计时间 {{ selectedTask.estimatedTime || '-' }}</span>
              <span>保洁员 {{ selectedTask.cleanerName || '未分配' }}</span>
              <span>审批人 {{ selectedTask.approverName || '-' }}</span>
              <span>创建时间 {{ formatDateTime(selectedTask.createdAt) }}</span>
              <span>完成时间 {{ formatDateTime(selectedTask.completeTime) }}</span>
            </div>

            <label v-if="selectedTask.status === 'pending'" class="cleaning-task-list-page__field">
              <span>分配保洁员</span>
              <select v-model="selectedCleanerIdText">
                <option value="">请选择保洁员</option>
                <option v-for="cleaner in cleaners" :key="cleaner.id" :value="String(cleaner.id)">
                  {{ cleaner.name }}
                </option>
              </select>
            </label>

            <p v-if="selectedTask.notes" class="mobile-note">备注：{{ selectedTask.notes }}</p>

            <div class="cleaning-task-list-page__actions">
              <ion-button fill="outline" @click="handleCloseDetail">关闭</ion-button>
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
  getCleaningTaskStatusText,
  getCleaningTaskTypeText,
  getTodayDate,
  shiftDate,
} from '@/utils/accommodation'
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

async function loadOptions() {
  const [roomTypeResponse, cleanerResponse] = await Promise.all([getAllRoomTypes(), getCleaners()])
  if (!roomTypeResponse.success || !roomTypeResponse.data) {
    throw new Error(roomTypeResponse.message || '加载房型失败')
  }
  if (!cleanerResponse.success || !cleanerResponse.data) {
    throw new Error(cleanerResponse.message || '加载保洁员失败')
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
      throw new Error(response.message || '加载任务列表失败')
    }

    total.value = response.data.totalElements
    if (reset) {
      tasks.value = response.data.content
    } else {
      tasks.value = [...tasks.value, ...response.data.content]
    }
  } catch (error) {
    const message = resolveWarningMessage(error, '加载任务列表失败')
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
    const message = resolveWarningMessage(error, '加载任务列表失败')
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
      throw new Error(response.message || '加载任务详情失败')
    }
    selectedTask.value = response.data
  } catch (error) {
    selectedTask.value = tasks.value.find((item) => item.id === taskId) || null
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载任务详情失败'))
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
    handleCloseDetail()
    await loadTasks(true)
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
    handleCloseDetail()
    await loadTasks(true)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '完成任务失败'))
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
