export const CLEANER_STATUS_ORDER = ['assigned', 'in_progress', 'pending', 'completed'] as const

export type CleanerTaskStatusKey = (typeof CLEANER_STATUS_ORDER)[number]

export const CLEANER_STATUS_LABELS: Record<string, string> = {
  pending: '待分配',
  assigned: '待接受',
  in_progress: '待打扫',
  completed: '已完成',
  expired: '已过期',
}

export const CLEANER_TASK_TYPE_LABELS: Record<string, string> = {
  checkout: '退房清洁',
  daily: '日常清洁',
  deep: '深度清洁',
}
