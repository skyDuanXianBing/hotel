import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  assignCleaningTask,
  getCleaners,
  getCleaningTasks,
  type CleanerDTO,
  type CleaningTaskDTO,
} from '@/api/cleaning'
import {
  formatStoreTime,
  getStoreTodayYmd,
  resolveStoreTimeZoneFromStorage,
} from '@/utils/storeDateTime'

export type WorkbenchTaskType = 'cleaning' | 'review' | 'order' | 'message' | 'other'
export type WorkbenchTaskTypeFilter = 'all' | WorkbenchTaskType
export type WorkbenchStatusFilter =
  | 'all'
  | 'pending'
  | 'assigned'
  | 'in_progress'
  | 'completed'
  | 'expired'

export interface WorkbenchTask {
  id: string
  sourceTaskId: number
  type: WorkbenchTaskType
  title: string
  subtitle: string
  status: string
  taskType: string
  taskDate: string
  assigneeId?: number
  assigneeName?: string
  metaItems: string[]
  priority: 'normal' | 'warning' | 'danger' | 'success'
  raw: CleaningTaskDTO
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

const connectedTaskTypes = new Set<WorkbenchTaskTypeFilter>(['all', 'cleaning'])

const statusSortOrder: Record<string, number> = {
  pending: 0,
  assigned: 1,
  in_progress: 2,
  expired: 3,
  completed: 4,
}

const toSimpleTimeRange = (value?: string) => {
  const normalized = (value || '').trim()
  if (!normalized) {
    return ''
  }

  if (/^\d{2}:\d{2}\s*-\s*\d{2}:\d{2}$/.test(normalized)) {
    return normalized.replace(/\s+/g, '')
  }

  const parsed = new Date(normalized)
  if (Number.isNaN(parsed.getTime())) {
    return normalized
  }

  return formatStoreTime(parsed, resolveStoreTimeZoneFromStorage())
}

export const useHomeTaskWorkbench = () => {
  const { t } = useI18n()
  const loading = ref(false)
  const cleanersLoading = ref(false)
  const assigningTaskId = ref<number | null>(null)
  const activeType = ref<WorkbenchTaskTypeFilter>('all')
  const activeStatus = ref<WorkbenchStatusFilter>('all')
  const todayYmd = ref(getStoreTodayYmd())
  const cleaningTasks = ref<CleaningTaskDTO[]>([])
  const cleanerList = ref<CleanerDTO[]>([])
  const assignSelections = ref<Record<number, number | undefined>>({})

  const getTaskTypeLabel = (taskType: string) => {
    const labels: Record<string, string> = {
      checkout: t('pages.home.workbench.cleaningTypes.checkout'),
      daily: t('pages.home.workbench.cleaningTypes.daily'),
      deep: t('pages.home.workbench.cleaningTypes.deep'),
      'accommodation.cleaning.checkout': t('pages.home.workbench.cleaningTypes.checkout'),
      'accommodation.cleaning.daily': t('pages.home.workbench.cleaningTypes.daily'),
      'accommodation.cleaning.deep': t('pages.home.workbench.cleaningTypes.deep'),
    }
    return labels[taskType] || taskType || t('pages.home.workbench.cleaningTypes.unknown')
  }

  const getCleaningTaskTime = (task: CleaningTaskDTO) => {
    if (task.startTime && task.completeTime) {
      const timeZone = resolveStoreTimeZoneFromStorage()
      return `${formatStoreTime(task.startTime, timeZone)}-${formatStoreTime(task.completeTime, timeZone)}`
    }

    return toSimpleTimeRange(task.estimatedTime)
  }

  const getPriority = (status: string): WorkbenchTask['priority'] => {
    if (status === 'expired') {
      return 'danger'
    }
    if (status === 'pending') {
      return 'warning'
    }
    if (status === 'completed') {
      return 'success'
    }
    return 'normal'
  }

  const workbenchTasks = computed<WorkbenchTask[]>(() =>
    cleaningTasks.value.map((task) => {
      const timeText = getCleaningTaskTime(task)
      const assigneeText = task.cleanerName
        ? t('pages.home.workbench.assignedTo', { name: task.cleanerName })
        : t('pages.home.workbench.unassignedEmployee')

      return {
        id: `cleaning-${task.id}`,
        sourceTaskId: task.id,
        type: 'cleaning',
        title: t('pages.home.workbench.cleaningTaskTitle', {
          room: task.roomNumber || '-',
        }),
        subtitle: [task.roomType, getTaskTypeLabel(task.taskType)].filter(Boolean).join(' / '),
        status: task.status,
        taskType: task.taskType,
        taskDate: task.taskDate,
        assigneeId: task.cleanerId,
        assigneeName: task.cleanerName,
        metaItems: [
          timeText ? t('pages.home.workbench.taskTime', { time: timeText }) : '',
          assigneeText,
        ].filter(Boolean),
        priority: getPriority(task.status),
        raw: task,
      }
    })
  )

  const taskTypeSummaries = computed<WorkbenchTaskTypeSummary[]>(() => [
    {
      type: 'all',
      label: t('pages.home.workbench.types.all'),
      count: workbenchTasks.value.length,
      connected: true,
    },
    {
      type: 'cleaning',
      label: t('pages.home.workbench.types.cleaning'),
      count: workbenchTasks.value.length,
      connected: true,
    },
    {
      type: 'review',
      label: t('pages.home.workbench.types.review'),
      count: 0,
      connected: false,
    },
    {
      type: 'order',
      label: t('pages.home.workbench.types.order'),
      count: 0,
      connected: false,
    },
    {
      type: 'message',
      label: t('pages.home.workbench.types.message'),
      count: 0,
      connected: false,
    },
    {
      type: 'other',
      label: t('pages.home.workbench.types.other'),
      count: 0,
      connected: false,
    },
  ])

  const statusSummaries = computed<WorkbenchStatusSummary[]>(() => {
    const countByStatus = workbenchTasks.value.reduce<Record<string, number>>((counts, task) => {
      counts[task.status] = (counts[task.status] || 0) + 1
      return counts
    }, {})

    return [
      {
        status: 'all',
        label: t('pages.home.workbench.statuses.all'),
        count: workbenchTasks.value.length,
      },
      {
        status: 'pending',
        label: t('pages.home.workbench.statuses.pending'),
        count: countByStatus.pending || 0,
      },
      {
        status: 'assigned',
        label: t('pages.home.workbench.statuses.assigned'),
        count: countByStatus.assigned || 0,
      },
      {
        status: 'in_progress',
        label: t('pages.home.workbench.statuses.inProgress'),
        count: countByStatus.in_progress || 0,
      },
      {
        status: 'completed',
        label: t('pages.home.workbench.statuses.completed'),
        count: countByStatus.completed || 0,
      },
      {
        status: 'expired',
        label: t('pages.home.workbench.statuses.expired'),
        count: countByStatus.expired || 0,
      },
    ]
  })

  const selectedTypeSummary = computed(
    () =>
      taskTypeSummaries.value.find((summary) => summary.type === activeType.value) ||
      taskTypeSummaries.value[0]
  )

  const selectedTypeIsConnected = computed(() => connectedTaskTypes.has(activeType.value))

  const filteredTasks = computed(() => {
    if (!selectedTypeIsConnected.value) {
      return []
    }

    return workbenchTasks.value
      .filter((task) => activeType.value === 'all' || task.type === activeType.value)
      .filter((task) => activeStatus.value === 'all' || task.status === activeStatus.value)
      .slice()
      .sort((left, right) => {
        const leftOrder = statusSortOrder[left.status] ?? 99
        const rightOrder = statusSortOrder[right.status] ?? 99
        return leftOrder - rightOrder
      })
  })

  const loadCleaners = async () => {
    cleanersLoading.value = true
    try {
      const response = await getCleaners()
      if (response.success && response.data) {
        cleanerList.value = response.data
      } else {
        ElMessage.error(response.message || t('pages.home.workbench.loadCleanersFailed'))
      }
    } catch (error) {
      console.error('Failed to load cleaners for home workbench:', error)
      ElMessage.error(t('pages.home.workbench.loadCleanersFailed'))
    } finally {
      cleanersLoading.value = false
    }
  }

  const loadCleaningTasks = async () => {
    todayYmd.value = getStoreTodayYmd()
    try {
      const response = await getCleaningTasks({
        startDate: todayYmd.value,
        endDate: todayYmd.value,
        page: 0,
        size: 50,
        sortBy: 'taskDate',
        sortDirection: 'ASC',
      })

      if (response.success && response.data) {
        cleaningTasks.value = response.data.content
        const selections: Record<number, number | undefined> = {}
        response.data.content.forEach((task) => {
          selections[task.id] = task.cleanerId
        })
        assignSelections.value = selections
      } else {
        ElMessage.error(response.message || t('pages.home.workbench.loadTasksFailed'))
      }
    } catch (error) {
      console.error('Failed to load home workbench tasks:', error)
      ElMessage.error(t('pages.home.workbench.loadTasksFailed'))
    }
  }

  const loadWorkbench = async () => {
    loading.value = true
    try {
      await Promise.all([loadCleaners(), loadCleaningTasks()])
    } finally {
      loading.value = false
    }
  }

  const assignTask = async (task: WorkbenchTask) => {
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

      const taskIndex = cleaningTasks.value.findIndex((item) => item.id === task.sourceTaskId)
      if (taskIndex !== -1) {
        cleaningTasks.value.splice(taskIndex, 1, response.data)
      }
      assignSelections.value[task.sourceTaskId] = response.data.cleanerId
      ElMessage.success(t('pages.home.workbench.assignSuccess'))
    } catch (error) {
      console.error('Failed to assign home workbench task:', error)
      ElMessage.error(t('pages.home.workbench.assignFailed'))
    } finally {
      assigningTaskId.value = null
    }
  }

  return {
    activeStatus,
    activeType,
    assignSelections,
    assigningTaskId,
    cleanerList,
    cleanersLoading,
    filteredTasks,
    loadWorkbench,
    loading,
    selectedTypeIsConnected,
    selectedTypeSummary,
    statusSummaries,
    taskTypeSummaries,
    todayYmd,
    assignTask,
  }
}
