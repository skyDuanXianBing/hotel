const WEEKDAY_TEXT = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const CLEANING_TASK_STATUS_TEXT_MAP: Record<string, string> = {
  expired: '已过期',
  pending: '待分配',
  assigned: '待清洁',
  in_progress: '清洁中',
  completed: '已完成',
}

const CLEANING_TASK_TYPE_TEXT_MAP: Record<string, string> = {
  checkout: '退房',
  daily: '日常清洁',
  deep: '深度清洁',
}

const DEFAULT_ESTIMATED_TIME = {
  startTime: '10:00',
  endTime: '16:00',
} as const

const getDefaultEstimatedTime = () => ({
  startTime: DEFAULT_ESTIMATED_TIME.startTime,
  endTime: DEFAULT_ESTIMATED_TIME.endTime,
})

export interface AccommodationDateItem {
  date: string
  label: string
  shortLabel: string
  weekday: string
  isToday: boolean
  isWeekend: boolean
}

const normalizeDate = (value: Date) => {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const getTodayDate = () => {
  return normalizeDate(new Date())
}

export const parseDate = (value: string) => {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return new Date()
  }
  return parsed
}

export const shiftDate = (date: string, offsetDays: number) => {
  const nextDate = parseDate(date)
  nextDate.setDate(nextDate.getDate() + offsetDays)
  return normalizeDate(nextDate)
}

export const formatDate = (date: string) => {
  const parsed = parseDate(date)
  const year = parsed.getFullYear()
  const month = String(parsed.getMonth() + 1).padStart(2, '0')
  const day = String(parsed.getDate()).padStart(2, '0')
  return `${year}/${month}/${day}`
}

export const formatDateWithWeekday = (date: string) => {
  const parsed = parseDate(date)
  return `${formatDate(date)} ${WEEKDAY_TEXT[parsed.getDay()]}`
}

export const buildDateWindow = (startDate: string, days: number) => {
  const items: AccommodationDateItem[] = []
  const today = getTodayDate()
  const baseDate = parseDate(startDate)

  for (let index = 0; index < days; index += 1) {
    const currentDate = new Date(baseDate)
    currentDate.setDate(baseDate.getDate() + index)
    const currentDateText = normalizeDate(currentDate)
    const month = String(currentDate.getMonth() + 1).padStart(2, '0')
    const day = String(currentDate.getDate()).padStart(2, '0')
    const weekday = WEEKDAY_TEXT[currentDate.getDay()]

    items.push({
      date: currentDateText,
      label: `${month}/${day} ${weekday}`,
      shortLabel: `${month}/${day}`,
      weekday,
      isToday: currentDateText === today,
      isWeekend: currentDate.getDay() === 0 || currentDate.getDay() === 6,
    })
  }

  return items
}

export const formatCurrency = (value?: number | null) => {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '¥0'
  }

  return `¥${Number(value).toFixed(0)}`
}

export const formatPercent = (value?: number | null) => {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '0%'
  }

  return `${Number(value).toFixed(0)}%`
}

export const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export const getCleaningTaskStatusText = (status: string) => {
  return CLEANING_TASK_STATUS_TEXT_MAP[status] || status || '未知状态'
}

export const getCleaningTaskTypeText = (taskType: string) => {
  return CLEANING_TASK_TYPE_TEXT_MAP[taskType] || taskType || '未设置'
}

export const getCleaningTaskStatusClass = (status: string) => {
  return {
    'is-expired': status === 'expired',
    'is-pending': status === 'pending',
    'is-assigned': status === 'assigned',
    'is-progress': status === 'in_progress',
    'is-completed': status === 'completed',
  }
}

export const parseEstimatedTime = (estimatedTime?: string) => {
  if (!estimatedTime) {
    return getDefaultEstimatedTime()
  }

  const parts = estimatedTime.split('-')
  if (parts.length === 2) {
    return {
      startTime: parts[0],
      endTime: parts[1],
    }
  }

  return getDefaultEstimatedTime()
}

export const buildEstimatedTime = (startTime: string, endTime: string) => {
  return `${startTime}-${endTime}`
}

export const resolveUserLabel = (nickname?: string, email?: string) => {
  if (nickname && email) {
    return `${nickname} · ${email}`
  }
  if (nickname) {
    return nickname
  }
  if (email) {
    return email
  }
  return '系统'
}
