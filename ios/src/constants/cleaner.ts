export const CLEANER_STATUS_ORDER = ['assigned', 'in_progress', 'pending', 'completed'] as const

export type CleanerTaskStatusKey = (typeof CLEANER_STATUS_ORDER)[number]

export const CLEANER_STATUS_LABELS: Record<string, string> = {
  pending: 'iosStage5.cleaning.status.pending',
  assigned: 'iosStage5.cleaning.status.assigned',
  in_progress: 'iosStage5.cleaning.status.inProgress',
  completed: 'iosStage5.cleaning.status.completed',
  expired: 'iosStage5.cleaning.status.expired',
}

export const CLEANER_TASK_TYPE_LABELS: Record<string, string> = {
  checkout: 'iosStage5.cleaning.type.checkout',
  daily: 'iosStage5.cleaning.type.daily',
  deep: 'iosStage5.cleaning.type.deep',
}
