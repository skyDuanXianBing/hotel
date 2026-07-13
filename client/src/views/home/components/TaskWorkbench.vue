<template>
  <section class="task-workbench">
    <div class="workbench-header">
      <div class="title-block">
        <h3>{{ t('pages.home.workbench.title') }}</h3>
      </div>
      <el-button
        circle
        class="refresh-btn"
        :icon="Refresh"
        :loading="loading"
        :aria-label="t('pages.home.workbench.refresh')"
        @click="handleManualRefresh"
      />
    </div>

    <div class="date-line">
      <span>{{ t('pages.home.workbench.todayLabel', { date: todayYmd }) }}</span>
      <el-button link type="primary" class="view-all-btn" @click="goToTaskList">
        {{ t('pages.home.workbench.viewAll') }}
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <div class="type-strip" role="tablist" :aria-label="t('pages.home.workbench.typeFilter')">
      <div v-for="summary in taskTypeSummaries" :key="summary.type" class="type-chip-shell">
        <button
          class="type-chip"
          :class="{ active: activeType === summary.type }"
          type="button"
          role="tab"
          :aria-selected="activeType === summary.type"
          @click="handleTypeChange(summary.type)"
        >
          <span class="chip-icon">
            <img :src="getTypeIcon(summary.type)" :alt="summary.label" />
          </span>
          <span class="chip-copy">
            <span class="chip-label">{{ summary.label }}</span>
            <span class="chip-count">
              {{ summary.connected ? summary.count : t('pages.home.workbench.notConnectedShort') }}
            </span>
          </span>
        </button>
        <button
          v-if="summary.type === 'other' && canCreateInternalTasks"
          type="button"
          class="other-add-button"
          :aria-label="t('pages.home.workbench.internalTasks.createTitle')"
          @click.stop="openCreateTask"
        >
          <el-icon><Plus /></el-icon>
        </button>
      </div>
    </div>

    <div
      v-if="selectedTypeIsConnected"
      class="status-strip"
      role="tablist"
      :aria-label="t('pages.home.workbench.statusFilter')"
    >
      <button
        v-for="summary in statusSummaries"
        :key="summary.status"
        class="status-chip"
        :class="{ active: activeStatus === summary.status }"
        type="button"
        role="tab"
        :aria-selected="activeStatus === summary.status"
        @click="handleStatusChange(summary.status)"
      >
        <span>{{ summary.label }}</span>
        <strong>{{ summary.count }}</strong>
      </button>
    </div>

    <div ref="taskListShellRef" v-loading="loading" class="task-list-shell">
      <button
        v-if="hasNewTasks"
        type="button"
        class="new-tasks-notice"
        @click="handleNewTasksRefresh"
      >
        {{ t('pages.home.workbench.newTasksNotice') }}
      </button>
      <el-alert
        v-if="loadError"
        :type="hasWorkbenchData ? 'warning' : 'error'"
        :title="loadError"
        :closable="false"
        show-icon
        class="workbench-load-alert"
      />
      <el-empty
        v-if="!loadError && !selectedTypeIsConnected"
        :description="
          t('pages.home.workbench.futureSourceEmpty', { source: selectedTypeSummary.label })
        "
      />
      <el-empty
        v-else-if="!loadError && !loading && filteredTasks.length === 0"
        :description="t('pages.home.workbench.emptyToday')"
      />

      <div v-else class="task-list-items">
        <div
          v-for="task in filteredTasks"
          :key="`${task.type}:${task.id}`"
          class="task-row"
          :class="[`priority-${task.priority}`, { clickable: canNavigateTask(task) }]"
          :role="canNavigateTask(task) ? 'button' : undefined"
          :tabindex="canNavigateTask(task) ? 0 : undefined"
          @click="goToTask(task)"
          @keydown.enter.prevent="goToTask(task)"
          @keydown.space.prevent="goToTask(task)"
        >
          <div class="task-marker">
            <img :src="getTaskIcon(task.type)" :alt="task.title" />
          </div>

          <div class="task-main">
            <div class="task-copy">
              <h4>{{ task.title }}</h4>
              <p v-if="task.subtitle">{{ task.subtitle }}</p>
            </div>

            <div class="task-meta">
              <span v-for="item in task.metaItems" :key="item">{{ item }}</span>
            </div>

            <span class="task-status" :class="getStatusClass(task.statusGroup)">
              {{ getTaskStatusLabel(task) }}
            </span>

            <el-select
              v-if="task.canAssignCleaner"
              v-model="assignSelections[task.sourceTaskId]"
              :placeholder="t('pages.home.workbench.selectEmployee')"
              :loading="cleanersLoading"
              size="small"
              filterable
              class="assign-select"
              @click.stop
            >
              <el-option
                v-for="cleaner in cleanerList"
                :key="cleaner.id"
                :label="cleaner.name"
                :value="cleaner.id"
              />
            </el-select>
            <el-button
              v-if="task.canAssignCleaner"
              type="primary"
              size="small"
              class="assign-btn"
              :loading="assigningTaskId === task.sourceTaskId"
              @click.stop="assignTask(task)"
            >
              {{ t('pages.home.workbench.assign') }}
            </el-button>
          </div>
        </div>
      </div>

      <div
        v-if="hasWorkbenchData && filteredTasks.length > 0"
        ref="loadMoreSentinelRef"
        class="load-more-sentinel"
        aria-live="polite"
      >
        <span v-if="loadingMore" role="status">
          {{ t('pages.home.workbench.loadingMore') }}
        </span>
        <span v-else-if="loadMoreError" role="alert" class="load-more-error">
          {{ loadMoreError }}
          <button type="button" @click="handleLoadMoreRetry">
            {{ t('pages.home.workbench.retryLoadMore') }}
          </button>
        </span>
        <span v-else-if="!hasMore && total > WORKBENCH_PAGE_SIZE" role="status">
          {{ t('pages.home.workbench.allLoaded', { total }) }}
        </span>
        <span v-else-if="hasMore" role="status">
          {{ t('pages.home.workbench.loadedProgress', { loaded: loadedCount, total }) }}
        </span>
      </div>
    </div>

    <InternalTaskCenter
      ref="internalTaskCenterRef"
      :can-create="canCreateInternalTasks"
      :can-manage="canManageInternalTasks"
      @updated="handleInternalTaskUpdated"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter, type RouteLocationRaw } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowRight, Plus, Refresh } from '@element-plus/icons-vue'
import workbenchAllIcon from '@/assets/home/workbench-all.svg'
import workbenchCleanIcon from '@/assets/home/workbench-clean.svg'
import workbenchMessageIcon from '@/assets/home/workbench-message.svg'
import workbenchOrderIcon from '@/assets/home/workbench-order.svg'
import workbenchOtherIcon from '@/assets/home/workbench-other.svg'
import workbenchReviewIcon from '@/assets/home/workbench-review.svg'
import { useStoreStore } from '@/stores/store'
import { usePermissionStore } from '@/stores/permission'
import { PermissionAction, PermissionModule } from '@/api/role'
import InternalTaskCenter from '@/views/home/components/InternalTaskCenter.vue'
import { getWorkbenchStatusLabelKey } from '@/views/home/composables/workbenchPagination'
import {
  useHomeTaskWorkbench,
  type WorkbenchTask,
  type WorkbenchTaskTarget,
  type WorkbenchTaskType,
  type WorkbenchTaskTypeFilter,
} from '@/views/home/composables/useHomeTaskWorkbench'

const router = useRouter()
const { t } = useI18n()

const {
  activeStatus,
  activeType,
  assignSelections,
  assigningTaskId,
  cleanerList,
  cleanersLoading,
  filteredTasks,
  hasMore,
  hasNewTasks,
  hasWorkbenchData,
  loadWorkbench,
  loading,
  loadingMore,
  loadError,
  loadMoreError,
  loadedCount,
  resetToken,
  selectedTypeIsConnected,
  selectedTypeSummary,
  statusSummaries,
  taskTypeSummaries,
  todayYmd,
  total,
  assignTask,
  changeWorkbenchType,
  changeWorkbenchStatus,
  clearWorkbench,
  disposeWorkbench,
  loadMoreWorkbench,
  refreshFromTop,
  reloadWorkbenchData,
} = useHomeTaskWorkbench()

const storeStore = useStoreStore()
const permissionStore = usePermissionStore()
const canManageInternalTasks = computed(() => storeStore.hasAdminPermission)
const canCreateInternalTasks = computed(() =>
  permissionStore.hasPermission(
    PermissionModule.ACCOMMODATION,
    PermissionAction.CREATE_INTERNAL_TASK
  )
)
const internalTaskCenterRef = ref<InstanceType<typeof InternalTaskCenter> | null>(null)
const taskListShellRef = ref<HTMLElement | null>(null)
const loadMoreSentinelRef = ref<HTMLElement | null>(null)
const WORKBENCH_PAGE_SIZE = 50
let loadMoreObserver: IntersectionObserver | null = null
let listResizeObserver: ResizeObserver | null = null
let refreshTimer: number | null = null
let refreshDebounceTimer: number | null = null
let createdRefreshPromise: Promise<void> | null = null
let createdRefreshSeq = 0
let unmounted = false

const REFRESH_DELAY_MS = 30_000
const REFRESH_EVENT_DEBOUNCE_MS = 350
type WorkbenchRefreshSource = 'manual' | 'poll' | 'event'

const typeIconMap: Record<WorkbenchTaskTypeFilter, string> = {
  all: workbenchAllIcon,
  cleaning: workbenchCleanIcon,
  review: workbenchReviewIcon,
  order: workbenchOrderIcon,
  message: workbenchMessageIcon,
  other: workbenchOtherIcon,
}

const getTypeIcon = (type: WorkbenchTaskTypeFilter) => typeIconMap[type]

const getTaskIcon = (type: WorkbenchTaskType) => typeIconMap[type]

const getTaskStatusLabel = (task: WorkbenchTask) => {
  if (task.type === 'order' && task.statusGroup === 'pending') {
    return task.sourceStatus.toUpperCase() === 'UNASSIGNED'
      ? t('pages.home.workbench.statuses.unassigned')
      : t('pages.home.workbench.statuses.awaitingCheckIn')
  }
  const key = getWorkbenchStatusLabelKey(task.type, task.statusGroup)
  return key ? t(key) : task.statusGroup
}

const allowedRouteNames = new Set([
  'CleaningTaskList',
  'DataCenterRegistrations',
  'DataCenterRegistrationDetail',
  'Order',
  'Messages',
])

const normalizeRouteRecord = (
  source?: Record<string, string | number | boolean | null | undefined>
) => {
  const normalized: Record<string, string> = {}
  if (!source) {
    return normalized
  }

  for (const key of Object.keys(source)) {
    const value = source[key]
    if (value === undefined || value === null || value === '') {
      continue
    }
    normalized[key] = String(value)
  }
  return normalized
}

const getTargetShortcutQuery = (
  target: Exclude<WorkbenchTaskTarget, string | null | undefined>
) => {
  const query: Record<string, string> = {}
  if (target.reservationId) {
    query.reservationId = String(target.reservationId)
  }
  if (target.orderNumber) {
    query.orderNumber = String(target.orderNumber)
  }
  if (target.channelOrderNumber) {
    query.channelOrderNumber = String(target.channelOrderNumber)
  }
  if (target.guestName) {
    query.guestName = String(target.guestName)
  }
  if (target.suThreadId) {
    query.suThreadId = String(target.suThreadId)
  }
  return query
}

const isAllowedTargetPath = (path: string) => {
  if (path === '/accommodation/cleaning/task-list') {
    return true
  }
  if (path === '/data-center/registrations') {
    return true
  }
  if (path.startsWith('/data-center/registrations/')) {
    return true
  }
  if (path === '/order') {
    return true
  }
  if (path === '/messages') {
    return true
  }
  return false
}

const resolveStringTargetRoute = (target: string): RouteLocationRaw | null => {
  const normalized = target.trim()
  if (!normalized) {
    return null
  }
  if (normalized.startsWith('/') && isAllowedTargetPath(normalized)) {
    return { path: normalized }
  }
  if (allowedRouteNames.has(normalized)) {
    return { name: normalized }
  }
  return null
}

const resolveObjectTargetRoute = (
  target: Exclude<WorkbenchTaskTarget, string | null | undefined>
): RouteLocationRaw | null => {
  const routeName = target.routeName || target.name || ''
  const query = {
    ...normalizeRouteRecord(target.query),
    ...getTargetShortcutQuery(target),
  }

  if (routeName && allowedRouteNames.has(routeName)) {
    return {
      name: routeName,
      params: normalizeRouteRecord(target.params),
      query,
    }
  }

  const routePath = target.routePath || target.path || ''
  if (routePath && isAllowedTargetPath(routePath)) {
    return {
      path: routePath,
      query,
    }
  }

  return null
}

const resolveTargetRoute = (target: WorkbenchTaskTarget): RouteLocationRaw | null => {
  if (!target) {
    return null
  }
  if (typeof target === 'string') {
    return resolveStringTargetRoute(target)
  }
  return resolveObjectTargetRoute(target)
}

const resolveOrderListRoute = (statusGroup?: string): RouteLocationRaw => {
  if (statusGroup === 'unassigned') {
    return {
      name: 'Order',
      query: { type: 'unassigned' },
    }
  }
  if (statusGroup === 'pending') {
    return {
      name: 'Order',
      query: { type: 'pending' },
    }
  }
  return { name: 'Order' }
}

const resolveTaskRoute = (task: WorkbenchTask): RouteLocationRaw | null => {
  const targetRoute = resolveTargetRoute(task.target)
  if (targetRoute) {
    return targetRoute
  }

  if (task.type === 'cleaning') {
    return {
      name: 'CleaningTaskList',
      query: {
        date: todayYmd.value,
        status: task.statusGroup,
      },
    }
  }

  if (task.type === 'review') {
    if (task.sourceTaskId) {
      return {
        name: 'DataCenterRegistrationDetail',
        params: { formId: String(task.sourceTaskId) },
      }
    }
    return { name: 'DataCenterRegistrations' }
  }

  if (task.type === 'order') {
    return resolveOrderListRoute(task.statusGroup)
  }

  if (task.type === 'message') {
    const query: Record<string, string> = {}
    if (task.sourceId) {
      query.suThreadId = String(task.sourceId)
    }
    return {
      name: 'Messages',
      query,
    }
  }

  return null
}

const canNavigateTask = (task: WorkbenchTask) =>
  task.type === 'other' || Boolean(resolveTaskRoute(task))

const getStatusClass = (status: string) => {
  return `status-${status.replace(/[^a-zA-Z0-9_-]/g, '_')}`
}

const handleTypeChange = (type: WorkbenchTaskTypeFilter) => {
  taskListShellRef.value?.scrollTo({ top: 0 })
  void changeWorkbenchType(type).finally(rebuildLoadMoreObserver)
}

const handleStatusChange = (status: string) => {
  taskListShellRef.value?.scrollTo({ top: 0 })
  void changeWorkbenchStatus(status).finally(rebuildLoadMoreObserver)
}

const handleNewTasksRefresh = () => {
  taskListShellRef.value?.scrollTo({ top: 0 })
  void refreshFromTop().finally(rebuildLoadMoreObserver)
}

const rebuildLoadMoreObserver = async () => {
  loadMoreObserver?.disconnect()
  loadMoreObserver = null
  await nextTick()
  if (unmounted || !hasMore.value || !loadMoreSentinelRef.value) return
  const shell = taskListShellRef.value
  const scrollsInternally = Boolean(
    shell &&
      getComputedStyle(shell).overflowY !== 'visible' &&
      shell.scrollHeight > shell.clientHeight
  )
  loadMoreObserver = new IntersectionObserver(
    (entries) => {
      if (
        entries.some((entry) => entry.isIntersecting) &&
        hasMore.value &&
        !loadingMore.value &&
        !loadMoreError.value
      ) {
        void loadMoreWorkbench().finally(rebuildLoadMoreObserver)
      }
    },
    { root: scrollsInternally ? shell : null, rootMargin: '0px 0px 400px 0px', threshold: 0 }
  )
  loadMoreObserver.observe(loadMoreSentinelRef.value)
}

const handleLoadMoreRetry = () => {
  void loadMoreWorkbench().finally(rebuildLoadMoreObserver)
}

const navigateToRoute = (route: RouteLocationRaw) => {
  router.push(route).catch((error) => {
    console.error('Failed to navigate from home workbench:', error)
  })
}

const goToTask = (task: WorkbenchTask) => {
  if (task.type === 'other') {
    internalTaskCenterRef.value?.openDrawer()
    return
  }
  const route = resolveTaskRoute(task)
  if (!route) {
    return
  }
  navigateToRoute(route)
}

const goToTaskList = () => {
  if (activeType.value === 'other') {
    internalTaskCenterRef.value?.openDrawer()
    return
  }
  if (activeType.value === 'review') {
    navigateToRoute({ name: 'DataCenterRegistrations' })
    return
  }
  if (activeType.value === 'order') {
    navigateToRoute(resolveOrderListRoute(activeStatus.value))
    return
  }
  if (activeType.value === 'message') {
    navigateToRoute({ name: 'Messages' })
    return
  }
  navigateToRoute({ name: 'CleaningTaskList' })
}

const openCreateTask = () => internalTaskCenterRef.value?.openCreate()

const clearRefreshTimer = () => {
  if (refreshTimer !== null) {
    window.clearTimeout(refreshTimer)
    refreshTimer = null
  }
}

const clearRefreshDebounce = () => {
  if (refreshDebounceTimer !== null) {
    window.clearTimeout(refreshDebounceTimer)
    refreshDebounceTimer = null
  }
}

const scheduleNextRefresh = () => {
  clearRefreshTimer()
  if (unmounted || !storeStore.currentStore?.id) return
  refreshTimer = window.setTimeout(() => {
    refreshTimer = null
    if (document.visibilityState !== 'visible') {
      scheduleNextRefresh()
      return
    }
    if (loading.value) {
      scheduleNextRefresh()
      return
    }
    void performWorkbenchRefresh('poll')
  }, REFRESH_DELAY_MS)
}

const performWorkbenchRefresh = async (source: WorkbenchRefreshSource) => {
  try {
    await reloadWorkbenchData({ markTrailing: source !== 'manual' })
  } finally {
    void rebuildLoadMoreObserver()
    scheduleNextRefresh()
  }
}

const queueWorkbenchRefresh = () => {
  if (unmounted || document.visibilityState !== 'visible') return
  clearRefreshTimer()
  clearRefreshDebounce()
  refreshDebounceTimer = window.setTimeout(() => {
    refreshDebounceTimer = null
    void performWorkbenchRefresh('event')
  }, REFRESH_EVENT_DEBOUNCE_MS)
}

const refreshAfterInternalTaskCreated = () => {
  if (unmounted || createdRefreshPromise) return createdRefreshPromise
  const seq = ++createdRefreshSeq
  clearRefreshTimer()
  clearRefreshDebounce()
  taskListShellRef.value?.scrollTo({ top: 0 })
  createdRefreshPromise = refreshFromTop()
    .finally(() => {
      if (seq !== createdRefreshSeq) return
      void rebuildLoadMoreObserver()
      scheduleNextRefresh()
      createdRefreshPromise = null
    })
  return createdRefreshPromise
}

const handleInternalTaskUpdated = (reason: 'created' | 'updated') => {
  if (reason === 'created') {
    void refreshAfterInternalTaskCreated()
    return
  }
  queueWorkbenchRefresh()
}

const refreshWhenActive = () => {
  if (document.visibilityState === 'visible') {
    queueWorkbenchRefresh()
  }
}

const handleManualRefresh = () => {
  clearRefreshTimer()
  clearRefreshDebounce()
  taskListShellRef.value?.scrollTo({ top: 0 })
  void refreshFromTop().finally(() => {
    void rebuildLoadMoreObserver()
    scheduleNextRefresh()
  })
}

const handleWorkbenchInvalidated = () => queueWorkbenchRefresh()

onMounted(() => {
  void loadWorkbench().finally(() => {
    void rebuildLoadMoreObserver()
    scheduleNextRefresh()
  })
  if (typeof ResizeObserver !== 'undefined') {
    listResizeObserver = new ResizeObserver(() => void rebuildLoadMoreObserver())
    if (taskListShellRef.value) listResizeObserver.observe(taskListShellRef.value)
  }
  window.addEventListener('focus', refreshWhenActive)
  window.addEventListener('workbench-invalidated', handleWorkbenchInvalidated)
  document.addEventListener('visibilitychange', refreshWhenActive)
})

watch(
  () => storeStore.currentStore?.id,
  (nextStoreId, previousStoreId) => {
    if (nextStoreId === previousStoreId) return
    clearRefreshTimer()
    clearRefreshDebounce()
    createdRefreshSeq += 1
    createdRefreshPromise = null
    clearWorkbench()
    internalTaskCenterRef.value?.reset()
    if (nextStoreId) {
      taskListShellRef.value?.scrollTo({ top: 0 })
      void loadWorkbench().finally(() => {
        void rebuildLoadMoreObserver()
        scheduleNextRefresh()
      })
    }
  }
)

watch(resetToken, () => {
  taskListShellRef.value?.scrollTo({ top: 0 })
  void rebuildLoadMoreObserver()
})

onBeforeUnmount(() => {
  unmounted = true
  clearRefreshTimer()
  clearRefreshDebounce()
  createdRefreshSeq += 1
  createdRefreshPromise = null
  loadMoreObserver?.disconnect()
  listResizeObserver?.disconnect()
  loadMoreObserver = null
  listResizeObserver = null
  disposeWorkbench()
  window.removeEventListener('focus', refreshWhenActive)
  window.removeEventListener('workbench-invalidated', handleWorkbenchInvalidated)
  document.removeEventListener('visibilitychange', refreshWhenActive)
})
</script>

<style scoped>
.task-workbench {
  --workbench-border: rgba(15, 23, 42, 0.06);
  --workbench-ink: #111111;
  --workbench-muted: rgba(0, 0, 0, 0.42);
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid var(--workbench-border);
  border-radius: 20px;
  box-shadow:
    0 16px 36px rgba(15, 23, 42, 0.04),
    0 4px 10px rgba(15, 23, 42, 0.03);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  min-width: 0;
  width: 100%;
  height: 100%;
  min-height: 100%;
  padding: 16px 16px 14px;
  overflow: hidden;
}

.workbench-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title-block {
  min-width: 0;
}

.title-block h3 {
  margin: 0;
  color: var(--workbench-ink);
  font-size: 19px;
  font-weight: 800;
  line-height: 1.2;
}

.refresh-btn {
  --el-fill-color-blank: #ffffff;
  --el-border-color: rgba(15, 23, 42, 0.08);
  --el-text-color-regular: rgba(0, 0, 0, 0.34);
  width: 28px;
  height: 28px;
  box-shadow: none;
}

.refresh-btn:hover {
  --el-border-color: rgba(76, 111, 255, 0.28);
  --el-text-color-regular: #4c6fff;
}

.date-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  color: rgba(0, 0, 0, 0.38);
  font-size: 14px;
  line-height: 1.4;
}

.view-all-btn {
  padding-right: 0;
  color: rgba(0, 0, 0, 0.42);
  font-size: 14px;
  font-weight: 500;
}

.view-all-btn:hover {
  color: #4c6fff;
}

.type-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 16px;
}

.type-chip-shell {
  position: relative;
  min-width: 0;
}

.type-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 44px;
  padding: 10px 12px;
  background: #f7f8fb;
  border: 1px solid transparent;
  border-radius: 10px;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
  width: 100%;
}

.other-add-button {
  position: absolute;
  top: 5px;
  right: 5px;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 1px solid rgba(76, 111, 255, 0.3);
  border-radius: 50%;
  background: #fff;
  color: #4c6fff;
  cursor: pointer;
}

.other-add-button:hover,
.other-add-button:focus-visible {
  background: #4c6fff;
  color: #fff;
}

.type-chip:hover {
  transform: translateY(-1px);
  background: #f3f6ff;
}

.type-chip.active {
  background: rgba(76, 111, 255, 0.12);
  border-color: rgba(76, 111, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(76, 111, 255, 0.08);
}

.chip-icon {
  flex: 0 0 24px;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}

.chip-icon img {
  width: 26px;
  height: 26px;
  object-fit: contain;
  display: block;
}

.chip-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chip-label,
.chip-count {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chip-label {
  color: #111111;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.25;
}

.chip-count {
  color: rgba(0, 0, 0, 0.42);
  font-size: 12px;
  line-height: 1.2;
}

.status-strip {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding-bottom: 2px;
  overflow-x: auto;
}

.status-strip::-webkit-scrollbar {
  display: none;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto;
  height: 30px;
  padding: 0 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 999px;
  background: #ffffff;
  color: rgba(0, 0, 0, 0.48);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.status-chip span,
.status-chip strong {
  display: inline-flex;
  align-items: center;
  line-height: 1;
}

.status-chip strong {
  color: #111111;
  font-size: 13px;
  font-weight: 700;
}

.status-chip.active {
  color: #ffffff;
  background: #597ef7;
  border-color: #597ef7;
  box-shadow: none;
}

.status-chip.active strong {
  color: #ffffff;
}

.task-list-shell {
  flex: 1;
  min-height: 0;
  margin-top: 14px;
  padding-top: 2px;
  overflow-y: auto;
  overflow-x: hidden;
}

.task-list-shell :deep(.el-empty) {
  padding: 44px 0;
}

.workbench-load-alert {
  margin-bottom: 12px;
}

.task-list-items {
  display: contents;
}

.new-tasks-notice {
  position: sticky;
  top: 0;
  z-index: 2;
  width: 100%;
  padding: 8px 12px;
  border: 0;
  border-radius: 10px;
  color: #3158d5;
  background: #eef3ff;
  cursor: pointer;
}

.load-more-sentinel {
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--workbench-muted);
  font-size: 13px;
  text-align: center;
}

.load-more-error button {
  margin-left: 8px;
  border: 0;
  color: #3158d5;
  background: transparent;
  cursor: pointer;
  text-decoration: underline;
}

.task-list-shell :deep(.el-empty__description p) {
  color: rgba(0, 0, 0, 0.38);
}

.task-row {
  position: relative;
  display: flex;
  gap: 12px;
  padding: 8px 12px 12px 0;
  margin-bottom: 10px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.task-row.clickable {
  cursor: pointer;
}

.task-row.clickable:focus-visible {
  outline: 2px solid rgba(76, 111, 255, 0.72);
  outline-offset: 2px;
}

.task-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 2px;
  border-radius: 0 999px 999px 0;
  background: #4973fe;
}

.task-marker {
  flex: 0 0 40px;
  width: 40px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 12px;
  overflow: visible;
}

.task-marker img {
  width: 24px;
  height: 24px;
  object-fit: contain;
  display: block;
}

.task-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 64px;
  grid-template-areas:
    'copy copy'
    'meta status'
    'select button';
  align-items: start;
  column-gap: 8px;
  row-gap: 8px;
  flex: 1;
  min-width: 0;
  padding-top: 8px;
  padding-right: 12px;
}

.task-copy {
  grid-area: copy;
  min-width: 0;
}

.task-copy h4,
.task-copy p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-copy h4 {
  margin: 0;
  color: #111111;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
}

.task-copy p {
  margin: 2px 0 0;
  color: rgba(0, 0, 0, 0.42);
  font-size: 13px;
  line-height: 1.4;
}

.task-status {
  grid-area: status;
  justify-self: center;
  align-self: start;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  max-width: 100%;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  text-align: center;
  white-space: nowrap;
}

.task-status.status-pending {
  color: #f80e0e;
  background: rgba(248, 14, 14, 0.05);
}

.task-status.status-awaiting_review,
.task-status.status-awaiting_reply {
  color: #c26a00;
  background: rgba(255, 167, 38, 0.12);
}

.task-status.status-unassigned {
  color: #c26a00;
  background: rgba(255, 167, 38, 0.12);
}

.task-status.status-assigned {
  color: #57b78d;
  background: rgba(236, 255, 247, 0.9);
}

.task-status.status-in_progress {
  color: #b99a3b;
  background: rgba(185, 154, 59, 0.12);
}

.task-status.status-completed {
  color: #959292;
  background: #e9e9e9;
}

.task-status.status-expired {
  color: #111111;
  background: rgba(0, 0, 0, 0.05);
}

.task-status.status-overdue {
  color: #111111;
  background: rgba(0, 0, 0, 0.05);
}

.task-meta {
  grid-area: meta;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  color: rgba(0, 0, 0, 0.42);
  font-size: 13px;
  line-height: 1.5;
  min-width: 0;
}

.assign-select {
  grid-area: select;
  width: 100%;
}

.assign-select :deep(.el-select__wrapper) {
  min-height: 32px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.08) inset;
}

.assign-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px rgba(94, 127, 255, 0.44) inset;
}

.assign-select :deep(.el-select__placeholder) {
  color: rgba(0, 0, 0, 0.28);
}

.assign-btn {
  grid-area: button;
  width: 100%;
  min-width: 56px;
  border-radius: 8px;
  border: 0;
  background: #597ef7;
  box-shadow: none;
}

.assign-btn:hover,
.assign-btn:focus-visible {
  background: #4f73eb;
}

@media (max-width: 768px) {
  .task-workbench {
    padding: 16px 14px 14px;
  }

  .type-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .type-strip {
    grid-template-columns: 1fr;
  }

  .task-main {
    grid-template-columns: 1fr;
    grid-template-areas:
      'copy'
      'meta'
      'status'
      'select'
      'button';
  }

  .date-line {
    flex-direction: column;
    align-items: flex-start;
  }

  .task-status,
  .assign-btn {
    justify-self: start;
    width: auto;
  }
}
</style>
