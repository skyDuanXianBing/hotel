import type {
  HomeWorkbenchItemDTO,
  HomeWorkbenchTypeSummaryDTO,
  HomeWorkbenchTaskTypeFilter,
} from '@/api/homeWorkbench'

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

export const getAllowedWorkbenchStatuses = (type: HomeWorkbenchTaskTypeFilter) =>
  allowedStatusesByType[type]

export const normalizeWorkbenchStatus = (
  type: HomeWorkbenchTaskTypeFilter,
  status: string | null | undefined,
) => {
  const rawStatus = String(status || '').trim().toLowerCase()
  const normalized = rawStatus === 'expired' ? 'overdue' : rawStatus
  if (!normalized || normalized === 'all') return 'all'
  return (allowedStatusesByType[type] as readonly string[]).includes(normalized)
    ? normalized
    : 'all'
}

export const toWorkbenchStatusParam = (
  type: HomeWorkbenchTaskTypeFilter,
  status: string | null | undefined,
) => {
  const normalized = normalizeWorkbenchStatus(type, status)
  return normalized === 'all' ? undefined : normalized
}

export const getWorkbenchStatusLabelKey = (
  type: HomeWorkbenchTaskTypeFilter,
  status: string,
) => {
  if (type === 'other') {
    if (status === 'unassigned') return 'pages.home.workbench.internalTasks.unassigned'
    if (status === 'assigned') return 'pages.home.workbench.internalTasks.assigned'
    if (status === 'completed') return 'pages.home.workbench.internalTasks.completed'
  }
  if (type === 'order' && status === 'pending') {
    return 'pages.home.workbench.statuses.orderPending'
  }
  const keys: Record<string, string> = {
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
  const key = keys[status]
  return key ? `pages.home.workbench.statuses.${key}` : undefined
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
    summaryTotal,
    inconsistent: allUnfiltered && queryTotal !== summaryTotal,
  }
}

export const workbenchItemKey = (item: HomeWorkbenchItemDTO) => {
  const id = String(item.id || item.sourceId || '').trim()
  return id ? `${item.type}:${id}` : ''
}

/** Appends without re-sorting; a repeated key keeps its original position and receives fresh data. */
export const appendUniqueWorkbenchItems = (
  current: HomeWorkbenchItemDTO[],
  incoming: HomeWorkbenchItemDTO[],
) => {
  const result = current.slice()
  const indexes = new Map<string, number>()
  result.forEach((item, index) => {
    const key = workbenchItemKey(item)
    if (key) indexes.set(key, index)
  })

  for (const item of incoming) {
    const key = workbenchItemKey(item)
    if (!key) continue
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
