import {
  getBusinessDateWeekdayIndex,
  getStoreTodayDate,
  parseBusinessDateParts,
  shiftBusinessDate,
} from '@/utils/storeBusinessDate'

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

export const getTodayDate = () => {
  return getStoreTodayDate()
}

export const shiftDate = (date: string, offsetDays: number) => {
  return shiftBusinessDate(date, offsetDays)
}

export const formatDate = (date: string) => {
  const parsed = parseBusinessDateParts(date) || parseBusinessDateParts(getTodayDate())
  const year = parsed?.year || 1970
  const month = String(parsed?.month || 1).padStart(2, '0')
  const day = String(parsed?.day || 1).padStart(2, '0')
  return `${year}/${month}/${day}`
}

export const formatDateWithWeekday = (date: string) => {
  return `${formatDate(date)} ${WEEKDAY_TEXT[getBusinessDateWeekdayIndex(date)]}`
}

export const buildDateWindow = (startDate: string, days: number) => {
  const items: AccommodationDateItem[] = []
  const today = getTodayDate()

  for (let index = 0; index < days; index += 1) {
    const currentDateText = shiftDate(startDate, index)
    const currentDateParts = parseBusinessDateParts(currentDateText)
    const month = String(currentDateParts?.month || 1).padStart(2, '0')
    const day = String(currentDateParts?.day || 1).padStart(2, '0')
    const weekdayIndex = getBusinessDateWeekdayIndex(currentDateText)
    const weekday = WEEKDAY_TEXT[weekdayIndex]

    items.push({
      date: currentDateText,
      label: `${month}/${day} ${weekday}`,
      shortLabel: `${month}/${day}`,
      weekday,
      isToday: currentDateText === today,
      isWeekend: weekdayIndex === 0 || weekdayIndex === 6,
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
