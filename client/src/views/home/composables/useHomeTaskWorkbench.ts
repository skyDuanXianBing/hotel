import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { assignCleaningTask, getCleaners, type CleanerDTO } from '@/api/cleaning'
import {
  type ApiResponse,
  type HomeWorkbenchActionDTO,
  type HomeWorkbenchDTO,
  type HomeWorkbenchItemDTO,
  type HomeWorkbenchMetaItemDTO,
  type HomeWorkbenchRequest,
  type HomeWorkbenchTargetDTO,
  type HomeWorkbenchTaskType,
} from '@/api/homeWorkbench'
import { request } from '@/utils/request'
import { getStoreTodayYmd } from '@/utils/storeDateTime'
import { useStoreStore } from '@/stores/store'

const WORKBENCH_LIMIT = 50
const DEFAULT_STATUS_GROUP = 'pending'

interface WorkbenchRefreshOptions {
  /** Marks an event/mutation that happened after the active request started. */
  markTrailing?: boolean
}

interface WorkbenchRequestContext {
  key: string
  date: string
  type: WorkbenchTaskTypeFilter
}

interface ActiveWorkbenchRequest {
  context: WorkbenchRequestContext
  controller: AbortController
  canScheduleTrailing: boolean
  trailingRequested: boolean
  promise: Promise<boolean>
}

const WORKBENCH_TYPES: WorkbenchTaskType[] = ['cleaning', 'review', 'order', 'message', 'other']

const statusSortOrder: Record<string, number> = {
  awaiting_review: 0,
  awaiting_reply: 0,
  pending: 0,
  unassigned: 1,
  assigned: 2,
  in_progress: 3,
  expired: 4,
  completed: 5,
}

const prioritySortOrder: Record<string, number> = {
  danger: 0,
  warning: 1,
  normal: 2,
  success: 3,
}

export type WorkbenchTaskType = HomeWorkbenchTaskType
export type WorkbenchTaskTypeFilter = 'all' | WorkbenchTaskType
export type WorkbenchStatusFilter = 'all' | string
export type WorkbenchTaskPriority = 'normal' | 'warning' | 'danger' | 'success'
export type WorkbenchTaskTarget = string | HomeWorkbenchTargetDTO | null | undefined

export interface WorkbenchTask {
  id: string
  sourceId?: string | number
  sourceTaskId: number
  type: WorkbenchTaskType
  title: string
  subtitle: string
  sourceStatus: string
  statusGroup: string
  assigneeId?: number
  assigneeName?: string
  metaItems: string[]
  priority: WorkbenchTaskPriority
  target?: WorkbenchTaskTarget
  actions: HomeWorkbenchActionDTO[]
  unreadCount?: number
  canAssignCleaner: boolean
}

interface WorkbenchTaskTypeSummary {
  type: WorkbenchTaskTypeFilter
  label: string
  count: number
  connected: boolean
}

interface WorkbenchStatusSummary {
  status: WorkbenchStatusFilter
  label: string
  count: number
}

const normalizeTaskType = (value?: string): WorkbenchTaskType => {
  const normalized = (value || '').trim()
  if (normalized === 'cleaning') {
    return 'cleaning'
  }
  if (normalized === 'review') {
    return 'review'
  }
  if (normalized === 'order') {
    return 'order'
  }
  if (normalized === 'message') {
    return 'message'
  }
  return 'other'
}

const normalizeStatusGroup = (statusGroup?: string, sourceStatus?: string) => {
  const normalizedStatusGroup = (statusGroup || '').trim()
  if (normalizedStatusGroup) {
    return normalizedStatusGroup
  }

  const normalizedSourceStatus = (sourceStatus || '').trim()
  if (normalizedSourceStatus) {
    return normalizedSourceStatus
  }

  return DEFAULT_STATUS_GROUP
}

const toPositiveInteger = (value?: string | number | null) => {
  const numericValue = Number(value)
  if (Number.isInteger(numericValue) && numericValue > 0) {
    return numericValue
  }
  return undefined
}

const normalizePriority = (priority?: string, statusGroup?: string): WorkbenchTaskPriority => {
  const normalizedPriority = (priority || '').trim().toLowerCase()
  if (
    normalizedPriority === 'normal' ||
    normalizedPriority === 'warning' ||
    normalizedPriority === 'danger' ||
    normalizedPriority === 'success'
  ) {
    return normalizedPriority
  }

  if (statusGroup === 'expired') {
    return 'danger'
  }
  if (statusGroup === 'pending') {
    return 'warning'
  }
  if (statusGroup === 'completed') {
    return 'success'
  }
  return 'normal'
}

const normalizeActionCode = (action: HomeWorkbenchActionDTO) => {
  if (typeof action === 'string') {
    return action.trim().toLowerCase().replace(/-/g, '_')
  }

  const rawCode = action.code || action.type || ''
  return rawCode.trim().toLowerCase().replace(/-/g, '_')
}

const hasAction = (actions: HomeWorkbenchActionDTO[], expectedCode: string) => {
  const normalizedExpectedCode = expectedCode.trim().toLowerCase().replace(/-/g, '_')
  for (const action of actions) {
    if (normalizeActionCode(action) === normalizedExpectedCode) {
      return true
    }
  }
  return false
}

const formatUnknownStatus = (status: string) => {
  const normalized = status.trim()
  if (!normalized) {
    return status
  }
  return normalized.replace(/_/g, ' ')
}

const formatMetaItem = (item: string | HomeWorkbenchMetaItemDTO) => {
  if (typeof item === 'string') {
    return item.trim()
  }

  const label = String(item.label || '').trim()
  const value = item.value == null ? '' : String(item.value).trim()
  if (label && value) {
    return `${label}: ${value}`
  }
  if (value) {
    return value
  }
  return label
}

export const useHomeTaskWorkbench = () => {
  const { t } = useI18n()
  const storeStore = useStoreStore()
  const loading = ref(false)
  const loadError = ref('')
  const cleanersLoading = ref(false)
  const assigningTaskId = ref<number | null>(null)
  const activeType = ref<WorkbenchTaskTypeFilter>('all')
  const activeStatus = ref<WorkbenchStatusFilter>('all')
  const todayYmd = ref(getStoreTodayYmd())
  const workbenchData = ref<HomeWorkbenchDTO | null>(null)
  const cleanerList = ref<CleanerDTO[]>([])
  const assignSelections = ref<Record<number, number | undefined>>({})
  let activeRequest: ActiveWorkbenchRequest | null = null
  let cleanerRequestSeq = 0
  let disposed = false

  const getTypeLabel = (type: WorkbenchTaskTypeFilter) => {
    if (type === 'all') {
      return t('pages.home.workbench.types.all')
    }
    if (type === 'cleaning') {
      return t('pages.home.workbench.types.cleaning')
    }
    if (type === 'review') {
      return t('pages.home.workbench.types.review')
    }
    if (type === 'order') {
      return t('pages.home.workbench.types.order')
    }
    if (type === 'message') {
      return t('pages.home.workbench.types.message')
    }
    return t('pages.home.workbench.types.other')
  }

  const getStatusLabel = (status: string) => {
    if (status === 'all') {
      return t('pages.home.workbench.statuses.all')
    }
    if (status === 'pending') {
      return t('pages.home.workbench.statuses.pending')
    }
    if (status === 'awaiting_review') {
      return t('pages.home.workbench.statuses.awaitingReview')
    }
    if (status === 'awaiting_reply') {
      return t('pages.home.workbench.statuses.awaitingReply')
    }
    if (status === 'unassigned') {
      return t('pages.home.workbench.statuses.unassigned')
    }
    if (status === 'assigned') {
      return t('pages.home.workbench.statuses.assigned')
    }
    if (status === 'in_progress') {
      return t('pages.home.workbench.statuses.inProgress')
    }
    if (status === 'completed') {
      return t('pages.home.workbench.statuses.completed')
    }
    if (status === 'expired') {
      return t('pages.home.workbench.statuses.expired')
    }
    return formatUnknownStatus(status)
  }

  const mapWorkbenchItem = (item: HomeWorkbenchItemDTO): WorkbenchTask => {
    const type = normalizeTaskType(item.type)
    const statusGroup = normalizeStatusGroup(item.statusGroup, item.sourceStatus)
    const sourceStatus = (item.sourceStatus || statusGroup).trim()
    const actions = Array.isArray(item.actions) ? item.actions : []
    const sourceTaskId = toPositiveInteger(item.sourceId) || 0
    const canAssignCleaner =
      type === 'cleaning' &&
      statusGroup === 'pending' &&
      sourceTaskId > 0 &&
      hasAction(actions, 'assign_cleaner')

    const metaItems: string[] = []
    const rawMetaItems = Array.isArray(item.metaItems) ? item.metaItems : []
    for (const rawMetaItem of rawMetaItems) {
      const formatted = formatMetaItem(rawMetaItem)
      if (formatted) {
        metaItems.push(formatted)
      }
    }

    if (type === 'cleaning') {
      const assigneeText = item.assigneeName
        ? t('pages.home.workbench.assignedTo', { name: item.assigneeName })
        : t('pages.home.workbench.unassignedEmployee')
      if (!metaItems.includes(assigneeText)) {
        metaItems.push(assigneeText)
      }
    }

    const unreadCount = Number(item.unreadCount || 0)
    if (unreadCount > 0) {
      metaItems.push(t('pages.home.workbench.unreadMessages', { count: unreadCount }))
    }

    return {
      id: item.id || `${type}-${String(item.sourceId || '')}`,
      sourceId: item.sourceId,
      sourceTaskId,
      type,
      title: item.title || getTypeLabel(type),
      subtitle: item.subtitle || '',
      sourceStatus,
      statusGroup,
      assigneeId: item.assigneeId || undefined,
      assigneeName: item.assigneeName || undefined,
      metaItems,
      priority: normalizePriority(item.priority, statusGroup),
      target: item.target,
      actions,
      unreadCount,
      canAssignCleaner,
    }
  }

  const workbenchTasks = computed<WorkbenchTask[]>(() => {
    const items = workbenchData.value?.items || []
    const mappedItems: WorkbenchTask[] = []
    for (const item of items) {
      mappedItems.push(mapWorkbenchItem(item))
    }
    return mappedItems
  })

  const hasWorkbenchData = computed(() => workbenchData.value !== null)

  const taskTypeSummaries = computed<WorkbenchTaskTypeSummary[]>(() => {
    const backendSummaries = new Map<WorkbenchTaskType, { count: number; connected: boolean }>()
    for (const summary of workbenchData.value?.typeSummaries || []) {
      backendSummaries.set(normalizeTaskType(summary.type), {
        count: Number(summary.count || 0),
        connected: Boolean(summary.connected),
      })
    }

    const itemCounts = new Map<WorkbenchTaskType, number>()
    for (const task of workbenchTasks.value) {
      itemCounts.set(task.type, (itemCounts.get(task.type) || 0) + 1)
    }

    const summaries: WorkbenchTaskTypeSummary[] = []
    let totalCount = 0
    for (const type of WORKBENCH_TYPES) {
      const backendSummary = backendSummaries.get(type)
      const itemCount = itemCounts.get(type) || 0
      const count = backendSummary ? backendSummary.count : itemCount
      totalCount += count
      summaries.push({
        type,
        label: getTypeLabel(type),
        count,
        connected: backendSummary ? backendSummary.connected : itemCount > 0,
      })
    }

    return [
      {
        type: 'all',
        label: getTypeLabel('all'),
        count: totalCount,
        connected: true,
      },
      ...summaries,
    ]
  })

  const selectedTypeSummary = computed(() => {
    const matchedSummary = taskTypeSummaries.value.find(
      (summary) => summary.type === activeType.value,
    )
    if (matchedSummary) {
      return matchedSummary
    }
    return taskTypeSummaries.value[0]
  })

  const selectedTypeIsConnected = computed(() => Boolean(selectedTypeSummary.value?.connected))

  const tasksForActiveType = computed(() => {
    if (!selectedTypeIsConnected.value) {
      return []
    }

    if (activeType.value === 'all') {
      return workbenchTasks.value
    }

    const tasks: WorkbenchTask[] = []
    for (const task of workbenchTasks.value) {
      if (task.type === activeType.value) {
        tasks.push(task)
      }
    }
    return tasks
  })

  const statusSummaries = computed<WorkbenchStatusSummary[]>(() => {
    const counts = new Map<string, number>()
    let totalCount = 0

    if (activeType.value === 'all' && workbenchData.value?.statusSummaries?.length) {
      for (const summary of workbenchData.value.statusSummaries) {
        const status = normalizeStatusGroup(summary.statusGroup)
        const count = Number(summary.count || 0)
        counts.set(status, count)
        totalCount += count
      }
    } else {
      for (const task of tasksForActiveType.value) {
        counts.set(task.statusGroup, (counts.get(task.statusGroup) || 0) + 1)
        totalCount += 1
      }
    }

    const statuses = new Set<string>([
      'awaiting_review',
      'awaiting_reply',
      'pending',
      'unassigned',
      'assigned',
      'in_progress',
      'completed',
      'expired',
    ])
    for (const status of counts.keys()) {
      statuses.add(status)
    }

    const orderedStatuses = Array.from(statuses).sort((left, right) => {
      const leftOrder = statusSortOrder[left] ?? 99
      const rightOrder = statusSortOrder[right] ?? 99
      if (leftOrder !== rightOrder) {
        return leftOrder - rightOrder
      }
      return left.localeCompare(right)
    })

    const summaries: WorkbenchStatusSummary[] = [
      {
        status: 'all',
        label: getStatusLabel('all'),
        count: totalCount,
      },
    ]

    for (const status of orderedStatuses) {
      summaries.push({
        status,
        label: getStatusLabel(status),
        count: counts.get(status) || 0,
      })
    }

    return summaries
  })

  const filteredTasks = computed(() => {
    if (!selectedTypeIsConnected.value) {
      return []
    }

    const tasks: WorkbenchTask[] = []
    for (const task of tasksForActiveType.value) {
      if (activeStatus.value !== 'all' && task.statusGroup !== activeStatus.value) {
        continue
      }
      tasks.push(task)
    }

    return tasks.slice().sort((left, right) => {
      const leftStatusOrder = statusSortOrder[left.statusGroup] ?? 99
      const rightStatusOrder = statusSortOrder[right.statusGroup] ?? 99
      if (leftStatusOrder !== rightStatusOrder) {
        return leftStatusOrder - rightStatusOrder
      }

      const leftPriorityOrder = prioritySortOrder[left.priority] ?? 99
      const rightPriorityOrder = prioritySortOrder[right.priority] ?? 99
      return leftPriorityOrder - rightPriorityOrder
    })
  })

  const syncAssignSelections = () => {
    const selections: Record<number, number | undefined> = {}
    for (const task of workbenchTasks.value) {
      if (task.type !== 'cleaning' || task.sourceTaskId <= 0) {
        continue
      }
      selections[task.sourceTaskId] = task.assigneeId
    }
    assignSelections.value = selections
  }

  const loadCleaners = async () => {
    const requestSeq = cleanerRequestSeq + 1
    cleanerRequestSeq = requestSeq
    const requestStoreId = storeStore.currentStore?.id
    cleanersLoading.value = true
    try {
      const response = await getCleaners()
      if (
        requestSeq !== cleanerRequestSeq ||
        requestStoreId !== storeStore.currentStore?.id ||
        disposed
      ) {
        return
      }
      if (response.success && response.data) {
        cleanerList.value = response.data
      } else {
        ElMessage.error(response.message || t('pages.home.workbench.loadCleanersFailed'))
      }
    } catch (error) {
      if (
        requestSeq !== cleanerRequestSeq ||
        requestStoreId !== storeStore.currentStore?.id ||
        disposed
      ) {
        return
      }
      console.error('Failed to load cleaners for home workbench:', error)
      ElMessage.error(t('pages.home.workbench.loadCleanersFailed'))
    } finally {
      if (requestSeq === cleanerRequestSeq) {
        cleanersLoading.value = false
      }
    }
  }

  const createRequestContext = (): WorkbenchRequestContext => {
    const date = getStoreTodayYmd()
    const type = activeType.value
    const storeId = storeStore.currentStore?.id || 'none'
    return {
      key: `${storeId}:${date}:${type}`,
      date,
      type,
    }
  }

  const isCancellationError = (error: unknown) => {
    if (!error || typeof error !== 'object') {
      return false
    }
    const candidate = error as { code?: string; name?: string }
    return candidate.code === 'ERR_CANCELED' || candidate.name === 'CanceledError'
  }

  const executeWorkbenchRequest = (
    context: WorkbenchRequestContext,
    canScheduleTrailing: boolean,
  ): Promise<boolean> => {
    const requestParams: HomeWorkbenchRequest = {
      date: context.date,
      limit: WORKBENCH_LIMIT,
    }
    if (context.type !== 'all') {
      requestParams.type = context.type
    }

    const entry: ActiveWorkbenchRequest = {
      context,
      controller: new AbortController(),
      canScheduleTrailing,
      trailingRequested: false,
      promise: Promise.resolve(false),
    }

    loading.value = true
    todayYmd.value = context.date
    activeRequest = entry

    entry.promise = (async () => {
      let succeeded = false
      try {
        const response = await request.get<never, ApiResponse<HomeWorkbenchDTO>>(
          '/home/workbench',
          {
            params: requestParams,
            signal: entry.controller.signal,
            suppressErrorToast: true,
          },
        )
        const responseIsCurrent =
          !disposed && activeRequest === entry && createRequestContext().key === context.key
        if (responseIsCurrent && response.success && response.data) {
          workbenchData.value = response.data
          loadError.value = ''
          syncAssignSelections()
          succeeded = true
        } else if (responseIsCurrent) {
          loadError.value = response.message || t('pages.home.workbench.loadTasksFailed')
        }
      } catch (error) {
        if (!isCancellationError(error) && !disposed && activeRequest === entry) {
          console.error('Failed to load home workbench tasks:', error)
          loadError.value = t('pages.home.workbench.loadTasksFailed')
        }
      }

      if (activeRequest !== entry) {
        return succeeded
      }

      activeRequest = null
      if (
        !disposed &&
        entry.canScheduleTrailing &&
        entry.trailingRequested &&
        createRequestContext().key === context.key
      ) {
        return executeWorkbenchRequest(createRequestContext(), false)
      }

      loading.value = false
      return succeeded
    })()

    return entry.promise
  }

  const loadWorkbenchData = (
    options: WorkbenchRefreshOptions = {},
  ): Promise<boolean> => {
    if (disposed) {
      return Promise.resolve(false)
    }

    const context = createRequestContext()
    const runningRequest = activeRequest
    if (runningRequest) {
      if (runningRequest.context.key === context.key) {
        if (options.markTrailing && runningRequest.canScheduleTrailing) {
          runningRequest.trailingRequested = true
        }
        return runningRequest.promise
      }

      runningRequest.controller.abort()
    }

    return executeWorkbenchRequest(context, true)
  }

  const reloadWorkbenchData = (options: WorkbenchRefreshOptions = {}) =>
    loadWorkbenchData(options)

  const clearWorkbench = () => {
    activeRequest?.controller.abort()
    activeRequest = null
    cleanerRequestSeq += 1
    loading.value = false
    cleanersLoading.value = false
    workbenchData.value = null
    loadError.value = ''
    cleanerList.value = []
    assignSelections.value = {}
  }

  const loadWorkbench = async () => {
    await Promise.all([loadCleaners(), loadWorkbenchData()])
  }

  const changeWorkbenchType = async (type: WorkbenchTaskTypeFilter) => {
    activeType.value = type
    activeStatus.value = 'all'
    await reloadWorkbenchData()
  }

  const assignTask = async (task: WorkbenchTask) => {
    if (!task.canAssignCleaner || task.sourceTaskId <= 0) {
      return
    }

    const cleanerId = assignSelections.value[task.sourceTaskId]
    if (!cleanerId) {
      ElMessage.warning(t('pages.home.workbench.selectEmployeeFirst'))
      return
    }

    assigningTaskId.value = task.sourceTaskId
    try {
      const response = await assignCleaningTask(task.sourceTaskId, cleanerId)
      if (!response.success || !response.data) {
        ElMessage.error(response.message || t('pages.home.workbench.assignFailed'))
        return
      }

      await reloadWorkbenchData({ markTrailing: true })
      ElMessage.success(t('pages.home.workbench.assignSuccess'))
    } catch (error) {
      console.error('Failed to assign home workbench task:', error)
      ElMessage.error(t('pages.home.workbench.assignFailed'))
    } finally {
      assigningTaskId.value = null
    }
  }

  const disposeWorkbench = () => {
    disposed = true
    activeRequest?.controller.abort()
    activeRequest = null
    cleanerRequestSeq += 1
    loading.value = false
    cleanersLoading.value = false
  }

  return {
    activeStatus,
    activeType,
    assignSelections,
    assigningTaskId,
    cleanerList,
    cleanersLoading,
    filteredTasks,
    hasWorkbenchData,
    loadWorkbench,
    loading,
    loadError,
    selectedTypeIsConnected,
    selectedTypeSummary,
    statusSummaries,
    taskTypeSummaries,
    todayYmd,
    assignTask,
    changeWorkbenchType,
    clearWorkbench,
    disposeWorkbench,
    reloadWorkbenchData,
  }
}
