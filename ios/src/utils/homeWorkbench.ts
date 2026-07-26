import type {
  HomeWorkbenchActionDTO,
  HomeWorkbenchItemDTO,
  HomeWorkbenchMetaItemDTO,
  HomeWorkbenchTargetDTO,
  HomeWorkbenchTaskType,
  HomeWorkbenchTaskTypeFilter,
  HomeWorkbenchTypeSummaryDTO,
} from '@/api/homeWorkbench'

export const WORKBENCH_TYPES: HomeWorkbenchTaskType[] = [
  'cleaning',
  'review',
  'order',
  'message',
  'other',
]

export const allowedStatusesByType = {
  all: [
    'pending',
    'awaiting_review',
    'awaiting_reply',
    'unassigned',
    'assigned',
    'in_progress',
    'overdue',
    'completed',
  ],
  cleaning: ['pending', 'in_progress', 'overdue'],
  review: ['awaiting_review', 'completed'],
  order: ['pending'],
  message: ['awaiting_reply'],
  other: ['unassigned', 'assigned', 'completed'],
} as const satisfies Record<HomeWorkbenchTaskTypeFilter, readonly string[]>

export const normalizeWorkbenchStatus = (
  type: HomeWorkbenchTaskTypeFilter,
  status?: string | null,
) => {
  const rawStatus = String(status || '').trim().toLowerCase()
  const normalized = rawStatus === 'expired' ? 'overdue' : rawStatus
  if (!normalized || normalized === 'all') {
    return 'all'
  }

  return (allowedStatusesByType[type] as readonly string[]).includes(normalized)
    ? normalized
    : 'all'
}

export const getAllowedWorkbenchStatuses = (type: HomeWorkbenchTaskTypeFilter) => {
  return allowedStatusesByType[type]
}

export const getWorkbenchStatusLabelKey = (
  type: HomeWorkbenchTaskTypeFilter,
  status: string,
) => {
  const normalizedStatus = String(status || '').trim().toLowerCase()
  if (type === 'cleaning' && normalizedStatus === 'pending') {
    return 'cleaningPending'
  }
  if (type === 'order' && normalizedStatus === 'pending') {
    return 'orderPending'
  }

  const keys: Record<string, string> = {
    all: 'all',
    awaiting_review: 'awaitingReview',
    awaiting_reply: 'awaitingReply',
    overdue: 'expired',
    expired: 'expired',
    pending: 'pending',
    unassigned: 'unassigned',
    assigned: 'assigned',
    in_progress: 'inProgress',
    completed: 'completed',
  }
  return keys[normalizedStatus]
}

export const resolveWorkbenchTypeCounts = (
  summaries: HomeWorkbenchTypeSummaryDTO[] | null | undefined,
  queryTotal: number,
  allUnfiltered: boolean,
) => {
  const byType = new Map(summaries?.map((summary) => [summary.type, Number(summary.count || 0)]))
  const summaryTotal = [...byType.values()].reduce((sum, count) => sum + count, 0)

  return {
    byType,
    allCount: allUnfiltered ? queryTotal : summaryTotal,
  }
}

export const formatWorkbenchMetaItem = (item: string | HomeWorkbenchMetaItemDTO) => {
  if (typeof item === 'string') {
    return item.trim()
  }

  const label = String(item.label || '').trim()
  const value = item.value == null ? '' : String(item.value).trim()
  if (label && value) {
    return `${label}: ${value}`
  }
  return value || label
}

export const resolveWorkbenchMessageThreadId = (
  target?: string | HomeWorkbenchTargetDTO | null,
  sourceId?: string | number,
) => {
  const targetRecord = typeof target === 'object' && target ? target : null
  const candidates = [
    targetRecord?.suThreadId,
    targetRecord?.params?.threadId,
    targetRecord?.params?.suThreadId,
    targetRecord?.query?.threadId,
    targetRecord?.query?.suThreadId,
    sourceId,
  ]

  for (const candidate of candidates) {
    const normalized = candidate == null ? '' : String(candidate).trim()
    if (normalized) {
      return normalized
    }
  }

  return null
}

const normalizeActionCode = (action: HomeWorkbenchActionDTO) => {
  if (typeof action === 'string') {
    return action.trim().toLowerCase().replace(/-/g, '_')
  }

  return String(action.code || action.type || '')
    .trim()
    .toLowerCase()
    .replace(/-/g, '_')
}

export const hasWorkbenchAction = (
  actions: HomeWorkbenchActionDTO[] | undefined,
  expectedCode: string,
) => {
  const normalizedExpected = expectedCode.trim().toLowerCase().replace(/-/g, '_')
  return (actions || []).some((action) => normalizeActionCode(action) === normalizedExpected)
}

export const appendUniqueWorkbenchItems = (
  current: HomeWorkbenchItemDTO[],
  incoming: HomeWorkbenchItemDTO[],
) => {
  const result = current.slice()
  const indexes = new Map<string, number>()

  result.forEach((item, index) => {
    indexes.set(`${item.type}:${item.id || item.sourceId || ''}`, index)
  })

  for (const item of incoming) {
    const key = `${item.type}:${item.id || item.sourceId || ''}`
    const existingIndex = indexes.get(key)
    if (existingIndex === undefined) {
      indexes.set(key, result.length)
      result.push(item)
    } else {
      result[existingIndex] = item
    }
  }

  return result
}

export interface WorkbenchRequestIdentity {
  version: number
  type: HomeWorkbenchTaskTypeFilter
  status: string
  cursor: string | null
}

export const isWorkbenchRequestCurrent = (
  request: WorkbenchRequestIdentity,
  current: WorkbenchRequestIdentity,
) => {
  return (
    request.version === current.version &&
    request.type === current.type &&
    request.status === current.status &&
    request.cursor === current.cursor
  )
}
