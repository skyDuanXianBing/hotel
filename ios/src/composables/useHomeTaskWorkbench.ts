import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  getHomeWorkbench,
  type HomeWorkbenchDTO,
  type HomeWorkbenchItemDTO,
  type HomeWorkbenchTargetDTO,
  type HomeWorkbenchTaskType,
  type HomeWorkbenchTaskTypeFilter,
} from '@/api/homeWorkbench'
import { assignCleaningTask, getCleaners, type CleanerDTO } from '@/api/cleaning'
import { completeInternalTask } from '@/api/internalTask'
import { getStoreTodayDate } from '@/utils/storeBusinessDate'
import {
  appendUniqueWorkbenchItems,
  formatWorkbenchMetaItem,
  getAllowedWorkbenchStatuses,
  getWorkbenchStatusLabelKey,
  hasWorkbenchAction,
  isWorkbenchRequestCurrent,
  normalizeWorkbenchStatus,
  resolveWorkbenchTypeCounts,
  WORKBENCH_TYPES,
} from '@/utils/homeWorkbench'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const WORKBENCH_PAGE_SIZE = 50

export interface MobileWorkbenchTask {
  id: string
  sourceId?: string | number
  sourceTaskId: number
  type: HomeWorkbenchTaskType
  title: string
  subtitle: string
  sourceStatus: string
  statusGroup: string
  metaItems: string[]
  target?: string | HomeWorkbenchTargetDTO | null
  canAssignCleaner: boolean
  canComplete: boolean
}

export interface MobileWorkbenchTypeSummary {
  type: HomeWorkbenchTaskTypeFilter
  label: string
  count: number
  connected: boolean
}

export interface MobileWorkbenchStatusSummary {
  status: string
  label: string
  count: number
}

const toPositiveInteger = (value?: string | number | null) => {
  const numericValue = Number(value)
  return Number.isInteger(numericValue) && numericValue > 0 ? numericValue : 0
}

export const useHomeTaskWorkbench = () => {
  const { t } = useI18n()
  const activeType = ref<HomeWorkbenchTaskTypeFilter>('all')
  const activeStatus = ref('all')
  const todayYmd = ref(getStoreTodayDate())
  const loading = ref(false)
  const loadingMore = ref(false)
  const cleanersLoading = ref(false)
  const assigningTaskId = ref<number | null>(null)
  const completingTaskId = ref<number | null>(null)
  const loadError = ref('')
  const loadMoreError = ref('')
  const workbenchData = ref<HomeWorkbenchDTO | null>(null)
  const cleanerList = ref<CleanerDTO[]>([])
  const assignSelections = ref<Record<number, number | undefined>>({})
  const total = ref(0)
  const nextCursor = ref<string | null>(null)
  const hasMore = ref(false)
  let activeController: AbortController | null = null
  let loadMoreController: AbortController | null = null
  let queryVersion = 0

  const getTypeLabel = (type: HomeWorkbenchTaskTypeFilter) => {
    return t(`tools.workbench.types.${type}`)
  }

  const getStatusLabel = (
    status: string,
    type: HomeWorkbenchTaskTypeFilter = activeType.value,
  ) => {
    const normalized = normalizeWorkbenchStatus(type, status)
    const key = getWorkbenchStatusLabelKey(type, normalized)
    return key ? t(`tools.workbench.statuses.${key}`) : status.replace(/_/g, ' ')
  }

  const responseTotal = (data: HomeWorkbenchDTO) => {
    return Number(data.summary?.total ?? data.total ?? data.page?.totalElements ?? 0)
  }

  const normalizeResponse = (data: HomeWorkbenchDTO) => {
    if (data.summary?.types) {
      data.typeSummaries = data.summary.types
    }
    if (data.summary?.statuses) {
      data.statusSummaries = data.summary.statuses
    }
  }

  const applyPageState = (data: HomeWorkbenchDTO) => {
    total.value = responseTotal(data)
    nextCursor.value = data.page?.nextCursor || null
    hasMore.value = Boolean(data.page?.hasMore && nextCursor.value)
  }

  const mapTask = (item: HomeWorkbenchItemDTO): MobileWorkbenchTask => {
    const statusGroup = String(item.statusGroup || item.sourceStatus || 'pending').trim()
    const sourceStatus = String(item.sourceStatus || statusGroup).trim()
    const sourceTaskId = toPositiveInteger(item.sourceId)
    const metaItems = (item.metaItems || [])
      .map(formatWorkbenchMetaItem)
      .filter((itemText): itemText is string => Boolean(itemText))

    if (item.type === 'cleaning') {
      const assigneeText = item.assigneeName
        ? t('tools.workbench.assignedTo', { name: item.assigneeName })
        : t('tools.workbench.unassignedEmployee')
      if (!metaItems.includes(assigneeText)) {
        metaItems.push(assigneeText)
      }
    }

    const unreadCount = Number(item.unreadCount || 0)
    if (unreadCount > 0) {
      metaItems.push(t('tools.workbench.unreadMessages', { count: unreadCount }))
    }

    return {
      id: item.id || `${item.type}-${String(item.sourceId || '')}`,
      sourceId: item.sourceId,
      sourceTaskId,
      type: item.type,
      title: item.title || getTypeLabel(item.type),
      subtitle: item.subtitle || '',
      sourceStatus,
      statusGroup,
      metaItems,
      target: item.target,
      canAssignCleaner:
        item.type === 'cleaning' &&
        statusGroup === 'pending' &&
        sourceTaskId > 0 &&
        hasWorkbenchAction(item.actions, 'assign_cleaner'),
      canComplete:
        item.type === 'other' &&
        sourceTaskId > 0 &&
        hasWorkbenchAction(item.actions, 'complete'),
    }
  }

  const tasks = computed(() => (workbenchData.value?.items || []).map(mapTask))

  const typeSummaries = computed<MobileWorkbenchTypeSummary[]>(() => {
    const backendSummaries = new Map(
      (workbenchData.value?.typeSummaries || []).map((summary) => [summary.type, summary]),
    )
    const counts = resolveWorkbenchTypeCounts(
      workbenchData.value?.typeSummaries,
      total.value,
      activeType.value === 'all' && activeStatus.value === 'all',
    )

    return [
      {
        type: 'all',
        label: getTypeLabel('all'),
        count: counts.allCount,
        connected: true,
      },
      ...WORKBENCH_TYPES.map((type) => ({
        type,
        label: getTypeLabel(type),
        count: counts.byType.get(type) || 0,
        connected: backendSummaries.get(type)?.connected ?? false,
      })),
    ]
  })

  const selectedTypeSummary = computed(() => {
    return typeSummaries.value.find((summary) => summary.type === activeType.value) || typeSummaries.value[0]
  })

  const statusSummaries = computed<MobileWorkbenchStatusSummary[]>(() => {
    const counts = new Map<string, number>()
    for (const summary of workbenchData.value?.statusSummaries || []) {
      const normalized = normalizeWorkbenchStatus(activeType.value, summary.statusGroup)
      if (normalized !== 'all') {
        counts.set(normalized, Number(summary.count || 0))
      }
    }

    const statuses = getAllowedWorkbenchStatuses(activeType.value)
    return [
      {
        status: 'all',
        label: getStatusLabel('all'),
        count: [...counts.values()].reduce((sum, count) => sum + count, 0),
      },
      ...statuses.map((status) => ({
        status,
        label: getStatusLabel(status),
        count: counts.get(status) || 0,
      })),
    ]
  })

  const syncAssignSelections = () => {
    const nextSelections: Record<number, number | undefined> = {}
    for (const item of workbenchData.value?.items || []) {
      const sourceTaskId = toPositiveInteger(item.sourceId)
      if (item.type === 'cleaning' && sourceTaskId > 0) {
        nextSelections[sourceTaskId] = item.assigneeId || undefined
      }
    }
    assignSelections.value = nextSelections
  }

  const buildRequestParams = (cursor?: string) => {
    const status = normalizeWorkbenchStatus(activeType.value, activeStatus.value)
    return {
      date: getStoreTodayDate(),
      size: WORKBENCH_PAGE_SIZE,
      type: activeType.value === 'all' ? undefined : activeType.value,
      status: status === 'all' ? undefined : status,
      cursor,
      includeSummaries: !cursor,
    }
  }

  const loadCleaners = async () => {
    cleanersLoading.value = true
    try {
      const response = await getCleaners()
      if (!response.success || !response.data) {
        throw new Error(response.message || t('tools.workbench.loadCleanersFailed'))
      }
      cleanerList.value = response.data
    } catch (error) {
      if (!isHandledRequestError(error)) {
        showWarningToast(
          error instanceof Error ? error.message : t('tools.workbench.loadCleanersFailed'),
        )
      }
    } finally {
      cleanersLoading.value = false
    }
  }

  const loadWorkbench = async () => {
    queryVersion += 1
    const requestVersion = queryVersion
    loadMoreController?.abort()
    loadMoreController = null
    loadingMore.value = false
    activeController?.abort()
    const controller = new AbortController()
    activeController = controller
    loading.value = true
    loadError.value = ''
    todayYmd.value = getStoreTodayDate()

    try {
      const response = await getHomeWorkbench(buildRequestParams(), controller.signal)
      if (!response.success || !response.data) {
        throw new Error(response.message || t('tools.workbench.loadFailed'))
      }
      if (activeController !== controller || requestVersion !== queryVersion) {
        return
      }
      normalizeResponse(response.data)
      workbenchData.value = response.data
      applyPageState(response.data)
      syncAssignSelections()
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      loadError.value = error instanceof Error ? error.message : t('tools.workbench.loadFailed')
      if (!isHandledRequestError(error)) {
        showWarningToast(loadError.value)
      }
    } finally {
      if (activeController === controller) {
        activeController = null
        loading.value = false
      }
    }
  }

  const initialize = async () => {
    await Promise.all([loadCleaners(), loadWorkbench()])
  }

  const changeType = async (type: HomeWorkbenchTaskTypeFilter) => {
    if (activeType.value === type) {
      return
    }
    activeType.value = type
    activeStatus.value = 'all'
    await loadWorkbench()
  }

  const changeStatus = async (status: string) => {
    const normalized = normalizeWorkbenchStatus(activeType.value, status)
    if (activeStatus.value === normalized) {
      return
    }
    activeStatus.value = normalized
    await loadWorkbench()
  }

  const loadMore = async () => {
    if (!hasMore.value || !nextCursor.value || loadingMore.value) {
      return
    }

    const cursor = nextCursor.value
    const requestIdentity = {
      version: queryVersion,
      type: activeType.value,
      status: activeStatus.value,
      cursor,
    }
    const controller = new AbortController()
    loadMoreController = controller
    loadingMore.value = true
    loadMoreError.value = ''
    try {
      const response = await getHomeWorkbench(buildRequestParams(cursor), controller.signal)
      if (!response.success || !response.data || !workbenchData.value) {
        throw new Error(response.message || t('tools.workbench.loadMoreFailed'))
      }
      const currentIdentity = {
        version: queryVersion,
        type: activeType.value,
        status: activeStatus.value,
        cursor: nextCursor.value,
      }
      if (
        loadMoreController !== controller ||
        !isWorkbenchRequestCurrent(requestIdentity, currentIdentity)
      ) {
        return
      }
      workbenchData.value.items = appendUniqueWorkbenchItems(
        workbenchData.value.items,
        response.data.items || [],
      )
      nextCursor.value = response.data.page?.nextCursor || null
      hasMore.value = Boolean(response.data.page?.hasMore && nextCursor.value)
      syncAssignSelections()
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      if (loadMoreController !== controller) {
        return
      }
      loadMoreError.value =
        error instanceof Error ? error.message : t('tools.workbench.loadMoreFailed')
      if (!isHandledRequestError(error)) {
        showWarningToast(loadMoreError.value)
      }
    } finally {
      if (loadMoreController === controller) {
        loadMoreController = null
        loadingMore.value = false
      }
    }
  }

  const assignTask = async (task: MobileWorkbenchTask) => {
    const cleanerId = assignSelections.value[task.sourceTaskId]
    if (!cleanerId) {
      showWarningToast(t('tools.workbench.selectEmployeeFirst'))
      return
    }

    assigningTaskId.value = task.sourceTaskId
    try {
      const response = await assignCleaningTask(task.sourceTaskId, cleanerId)
      if (!response.success || !response.data) {
        throw new Error(response.message || t('tools.workbench.assignFailed'))
      }
      showSuccessToast(t('tools.workbench.assignSuccess'))
      await loadWorkbench()
    } catch (error) {
      if (!isHandledRequestError(error)) {
        showWarningToast(error instanceof Error ? error.message : t('tools.workbench.assignFailed'))
      }
    } finally {
      assigningTaskId.value = null
    }
  }

  const completeTask = async (task: MobileWorkbenchTask) => {
    if (!task.canComplete || completingTaskId.value) {
      return
    }

    completingTaskId.value = task.sourceTaskId
    try {
      const response = await completeInternalTask(task.sourceTaskId)
      if (!response.success || !response.data) {
        throw new Error(response.message || t('tools.workbench.completeFailed'))
      }
      showSuccessToast(t('tools.workbench.completeSuccess'))
      await loadWorkbench()
    } catch (error) {
      if (!isHandledRequestError(error)) {
        showWarningToast(error instanceof Error ? error.message : t('tools.workbench.completeFailed'))
      }
    } finally {
      completingTaskId.value = null
    }
  }

  const dispose = () => {
    queryVersion += 1
    activeController?.abort()
    activeController = null
    loadMoreController?.abort()
    loadMoreController = null
    loadingMore.value = false
  }

  return {
    activeStatus,
    activeType,
    assigningTaskId,
    completingTaskId,
    assignSelections,
    cleanerList,
    cleanersLoading,
    hasMore,
    loadError,
    loadMoreError,
    loading,
    loadingMore,
    selectedTypeSummary,
    statusSummaries,
    tasks,
    todayYmd,
    total,
    typeSummaries,
    assignTask,
    completeTask,
    changeStatus,
    changeType,
    dispose,
    initialize,
    getStatusLabel,
    loadMore,
    loadWorkbench,
  }
}
