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
import { useUserStore } from '@/stores/user'
import {
  appendUniqueWorkbenchItems,
  getAllowedWorkbenchStatuses,
  getWorkbenchStatusLabelKey,
  normalizeWorkbenchStatus,
  resolveWorkbenchTypeCounts,
  toWorkbenchStatusParam,
} from './workbenchPagination'

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
  status: WorkbenchStatusFilter
}

interface ActiveWorkbenchRequest {
  context: WorkbenchRequestContext
  controller: AbortController
  cursor: string | null
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
  overdue: 4,
  expired: 4,
  completed: 5,
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

  if (statusGroup === 'expired' || statusGroup === 'overdue') {
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
  const userStore = useUserStore()
  const loading = ref(false)
  const loadingMore = ref(false)
  const loadError = ref('')
  const loadMoreError = ref('')
  const total = ref(0)
  const nextCursor = ref<string | null>(null)
  const hasMore = ref(false)
  const hasNewTasks = ref(false)
  const resetToken = ref(0)
  const cleanersLoading = ref(false)
  const assigningTaskId = ref<number | null>(null)
  const activeType = ref<WorkbenchTaskTypeFilter>('all')
  const activeStatus = ref<WorkbenchStatusFilter>('all')
  const todayYmd = ref(getStoreTodayYmd())
  const workbenchData = ref<HomeWorkbenchDTO | null>(null)
  const cleanerList = ref<CleanerDTO[]>([])
  const assignSelections = ref<Record<number, number | undefined>>({})
  let activeRequest: ActiveWorkbenchRequest | null = null
  let loadMoreRequest: ActiveWorkbenchRequest | null = null
  let requestGeneration = 0
  const warnedSummaryMismatches = new Set<string>()
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

  const getStatusLabel = (type: WorkbenchTaskTypeFilter, status: string) => {
    if (status === 'all') {
      return t('pages.home.workbench.statuses.all')
    }
    const key = getWorkbenchStatusLabelKey(type, status)
    if (key) return t(key)
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

    const resolvedCounts = resolveWorkbenchTypeCounts(
      workbenchData.value?.typeSummaries,
      total.value,
      activeType.value === 'all' && activeStatus.value === 'all',
    )

    const summaries: WorkbenchTaskTypeSummary[] = []
    for (const type of WORKBENCH_TYPES) {
      const backendSummary = backendSummaries.get(type)
      summaries.push({
        type,
        label: getTypeLabel(type),
        count: resolvedCounts.byType.get(type) || 0,
        connected: backendSummary?.connected ?? false,
      })
    }

    return [
      {
        type: 'all',
        label: getTypeLabel('all'),
        count: resolvedCounts.allCount,
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
    const statuses = new Set<string>(getAllowedWorkbenchStatuses(activeType.value))
    let totalCount = 0

    if (workbenchData.value?.statusSummaries?.length) {
      for (const summary of workbenchData.value.statusSummaries) {
        const status = normalizeStatusGroup(summary.statusGroup)
        if (!statuses.has(status)) continue
        const count = Number(summary.count || 0)
        counts.set(status, count)
        totalCount += count
      }
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
        label: getStatusLabel(activeType.value, 'all'),
        count: totalCount,
      },
    ]

    for (const status of orderedStatuses) {
      summaries.push({
        status,
        label: getStatusLabel(activeType.value, status),
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
      tasks.push(task)
    }
    return tasks
  })

  const loadedCount = computed(() => filteredTasks.value.length)

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
    const status = normalizeWorkbenchStatus(type, activeStatus.value)
    const storeId = storeStore.currentStore?.id || 'none'
    const permissionScope = `${userStore.currentUser?.id || 'anonymous'}:${storeStore.currentUserRole || 'unknown'}`
    return {
      key: `${storeId}:${date}:${type}:${status}:default:${permissionScope}`,
      date,
      type,
      status,
    }
  }

  const isCancellationError = (error: unknown) => {
    if (!error || typeof error !== 'object') {
      return false
    }
    const candidate = error as { code?: string; name?: string }
    return candidate.code === 'ERR_CANCELED' || candidate.name === 'CanceledError'
  }

  const requestParamsFor = (
    context: WorkbenchRequestContext,
    cursor: string | null,
    includeSummaries: boolean,
  ): HomeWorkbenchRequest => {
    const requestParams: HomeWorkbenchRequest = {
      date: context.date,
      size: WORKBENCH_LIMIT,
      includeSummaries,
    }
    if (context.type !== 'all') {
      requestParams.type = context.type
    }
    requestParams.status = toWorkbenchStatusParam(context.type, context.status)
    if (cursor) requestParams.cursor = cursor
    return requestParams
  }

  const responseTotal = (data: HomeWorkbenchDTO) =>
    Number(data.summary?.total ?? data.total ?? data.page?.totalElements ?? 0)

  const normalizeSummaries = (data: HomeWorkbenchDTO) => {
    if (data.summary?.types) data.typeSummaries = data.summary.types
    if (data.summary?.statuses) data.statusSummaries = data.summary.statuses
  }

  const auditTypeSummaryTotal = (
    data: HomeWorkbenchDTO,
    context: WorkbenchRequestContext,
  ) => {
    if (context.type !== 'all' || context.status !== 'all') return
    const queryTotal = responseTotal(data)
    const counts = resolveWorkbenchTypeCounts(data.typeSummaries, queryTotal, true)
    if (!counts.inconsistent) return
    const warningKey = `${context.key}:${queryTotal}:${counts.summaryTotal}`
    if (warnedSummaryMismatches.has(warningKey)) return
    warnedSummaryMismatches.add(warningKey)
    console.warn('[HomeWorkbench] type summary total does not match page total', {
      queryTotal,
      typeSummaryTotal: counts.summaryTotal,
      queryKey: context.key,
    })
  }

  const responseMatchesContext = (
    data: HomeWorkbenchDTO,
    context: WorkbenchRequestContext,
  ) => {
    if (!data.query) return true
    return data.query.type === context.type &&
      normalizeWorkbenchStatus(context.type, data.query.status) === context.status
  }

  const applyPageState = (data: HomeWorkbenchDTO) => {
    total.value = responseTotal(data)
    nextCursor.value = data.page?.nextCursor || null
    hasMore.value = Boolean(data.page?.hasMore && nextCursor.value)
    if (workbenchData.value && workbenchData.value.items?.length && loadedCount.value >= total.value) {
      hasMore.value = false
    }
  }

  const executeWorkbenchRequest = (
    context: WorkbenchRequestContext,
    canScheduleTrailing: boolean,
    preserveItems = false,
  ): Promise<boolean> => {
    const generation = requestGeneration
    const requestParams = requestParamsFor(context, null, true)

    const entry: ActiveWorkbenchRequest = {
      context,
      controller: new AbortController(),
      cursor: null,
      canScheduleTrailing,
      trailingRequested: false,
      promise: Promise.resolve(false),
    }

    loading.value = workbenchData.value === null
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
        const responseIsCurrent = !disposed && generation === requestGeneration &&
          activeRequest === entry && createRequestContext().key === context.key
        if (
          responseIsCurrent &&
          response.success &&
          response.data &&
          responseMatchesContext(response.data, context)
        ) {
          normalizeSummaries(response.data)
          auditTypeSummaryTotal(response.data, context)
          if (preserveItems && loadedCount.value > WORKBENCH_LIMIT) {
            const current = workbenchData.value
            if (current) {
              current.typeSummaries = response.data.typeSummaries
              current.statusSummaries = response.data.statusSummaries
              current.generatedAt = response.data.generatedAt
            }
            total.value = responseTotal(response.data)
            if (loadedCount.value >= total.value) hasMore.value = false
            hasNewTasks.value = true
          } else {
            workbenchData.value = response.data
            applyPageState(response.data)
            hasNewTasks.value = false
          }
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
        if (loadedCount.value > WORKBENCH_LIMIT) {
          hasNewTasks.value = true
          return succeeded
        }
        return executeWorkbenchRequest(createRequestContext(), false, false)
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

    const normalizedStatus = normalizeWorkbenchStatus(activeType.value, activeStatus.value)
    if (normalizedStatus !== activeStatus.value) {
      resetWorkbenchPage()
      activeStatus.value = normalizedStatus
    }
    const context = createRequestContext()
    if (workbenchData.value && todayYmd.value !== context.date) {
      resetWorkbenchPage()
    }
    if (loadMoreRequest?.context.key === context.key && options.markTrailing) {
      loadMoreRequest.trailingRequested = true
      return loadMoreRequest.promise
    }
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

    const preserveItems = Boolean(options.markTrailing && loadedCount.value > WORKBENCH_LIMIT)
    return executeWorkbenchRequest(context, true, preserveItems)
  }

  const reloadWorkbenchData = (options: WorkbenchRefreshOptions = {}) =>
    loadWorkbenchData(options)

  const resetWorkbenchPage = () => {
    requestGeneration += 1
    resetToken.value += 1
    activeRequest?.controller.abort()
    activeRequest = null
    loadMoreRequest?.controller.abort()
    loadMoreRequest = null
    loading.value = false
    loadingMore.value = false
    workbenchData.value = null
    loadError.value = ''
    loadMoreError.value = ''
    total.value = 0
    nextCursor.value = null
    hasMore.value = false
    hasNewTasks.value = false
  }

  const clearWorkbench = () => {
    resetWorkbenchPage()
    cleanerRequestSeq += 1
    cleanersLoading.value = false
    cleanerList.value = []
    assignSelections.value = {}
  }

  const loadWorkbench = async () => {
    await Promise.all([loadCleaners(), loadWorkbenchData()])
  }

  const changeWorkbenchType = async (type: WorkbenchTaskTypeFilter) => {
    if (type === activeType.value) return
    resetWorkbenchPage()
    activeType.value = type
    activeStatus.value = 'all'
    await reloadWorkbenchData()
  }

  const changeWorkbenchStatus = async (status: WorkbenchStatusFilter) => {
    const normalizedStatus = normalizeWorkbenchStatus(activeType.value, status)
    if (normalizedStatus === activeStatus.value) return
    resetWorkbenchPage()
    activeStatus.value = normalizedStatus
    await reloadWorkbenchData()
  }

  const loadMoreWorkbench = (): Promise<boolean> => {
    if (disposed) return Promise.resolve(false)
    const normalizedStatus = normalizeWorkbenchStatus(activeType.value, activeStatus.value)
    if (normalizedStatus !== activeStatus.value) {
      resetWorkbenchPage()
      activeStatus.value = normalizedStatus
      return reloadWorkbenchData()
    }
    if (activeRequest || loading.value || !hasMore.value || !nextCursor.value) {
      return Promise.resolve(false)
    }
    const context = createRequestContext()
    const cursor = nextCursor.value
    if (loadMoreRequest?.context.key === context.key && loadMoreRequest.cursor === cursor) {
      return loadMoreRequest.promise
    }
    loadMoreRequest?.controller.abort()
    const generation = requestGeneration
    const entry: ActiveWorkbenchRequest = {
      context,
      controller: new AbortController(),
      cursor,
      canScheduleTrailing: false,
      trailingRequested: false,
      promise: Promise.resolve(false),
    }
    loadMoreRequest = entry
    loadingMore.value = true
    loadMoreError.value = ''
    entry.promise = (async () => {
      let succeeded = false
      try {
        const response = await request.get<never, ApiResponse<HomeWorkbenchDTO>>(
          '/home/workbench',
          {
            params: requestParamsFor(context, cursor, false),
            signal: entry.controller.signal,
            suppressErrorToast: true,
          },
        )
        const current = !disposed && generation === requestGeneration &&
          loadMoreRequest === entry && createRequestContext().key === context.key
        if (
          current &&
          response.success &&
          response.data &&
          responseMatchesContext(response.data, context) &&
          workbenchData.value
        ) {
          const before = workbenchData.value.items || []
          const appended = appendUniqueWorkbenchItems(before, response.data.items || [])
          workbenchData.value.items = appended
          nextCursor.value = response.data.page?.nextCursor || null
          hasMore.value = Boolean(response.data.page?.hasMore && nextCursor.value)
          if (appended.length >= total.value || (appended.length === before.length && hasMore.value)) {
            hasMore.value = false
          }
          syncAssignSelections()
          succeeded = true
        } else if (current) {
          loadMoreError.value = response.message || t('pages.home.workbench.loadMoreFailed')
        }
      } catch (error) {
        if (!isCancellationError(error) && !disposed && loadMoreRequest === entry) {
          console.error('Failed to load more home workbench tasks:', error)
          loadMoreError.value = t('pages.home.workbench.loadMoreFailed')
        }
      } finally {
        if (loadMoreRequest === entry) {
          loadMoreRequest = null
          loadingMore.value = false
        }
      }
      if (entry.trailingRequested && !disposed && createRequestContext().key === context.key) {
        await executeWorkbenchRequest(createRequestContext(), false, loadedCount.value > WORKBENCH_LIMIT)
      }
      return succeeded
    })()
    return entry.promise
  }

  const refreshFromTop = async () => {
    resetWorkbenchPage()
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
    requestGeneration += 1
    activeRequest?.controller.abort()
    activeRequest = null
    loadMoreRequest?.controller.abort()
    loadMoreRequest = null
    cleanerRequestSeq += 1
    loading.value = false
    loadingMore.value = false
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
    total,
    selectedTypeIsConnected,
    selectedTypeSummary,
    statusSummaries,
    taskTypeSummaries,
    todayYmd,
    assignTask,
    changeWorkbenchType,
    changeWorkbenchStatus,
    clearWorkbench,
    disposeWorkbench,
    loadMoreWorkbench,
    refreshFromTop,
    reloadWorkbenchData,
  }
}
