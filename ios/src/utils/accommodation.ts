import {
  formatBusinessDateLabel,
  formatStoreDateTime,
  getBusinessDateWeekdayIndex,
  getStoreTodayDate,
  parseBusinessDateParts,
  shiftBusinessDate,
} from '@/utils/storeBusinessDate'
import { i18n } from '@/locales'

const WEEKDAY_KEYS = [
  'sunday',
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday',
  'saturday',
] as const

const CLEANING_TASK_STATUS_TEXT_KEY_MAP: Record<string, string> = {
  expired: 'expired',
  pending: 'pending',
  assigned: 'assigned',
  in_progress: 'inProgress',
  completed: 'completed',
}

const CLEANING_TASK_TYPE_TEXT_KEY_MAP: Record<string, string> = {
  checkout: 'checkout',
  daily: 'daily',
  deep: 'deep',
}

const DEFAULT_ESTIMATED_TIME = {
  startTime: '10:00',
  endTime: '16:00',
} as const

const getDefaultEstimatedTime = () => ({
  startTime: DEFAULT_ESTIMATED_TIME.startTime,
  endTime: DEFAULT_ESTIMATED_TIME.endTime,
})

const accommodationText = (key: string) => i18n.global.t(`runtime.accommodation.${key}`)

const getWeekdayText = (weekdayIndex: number) => {
  const weekdayKey = WEEKDAY_KEYS[weekdayIndex] || WEEKDAY_KEYS[0]
  return accommodationText(`weekday.${weekdayKey}`)
}

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
  return formatBusinessDateLabel(date || getTodayDate(), 'slash-date')
}

export const formatDateWithWeekday = (date: string) => {
  return `${formatDate(date)} ${getWeekdayText(getBusinessDateWeekdayIndex(date))}`
}

export const buildDateWindow = (startDate: string, days: number) => {
  const items: AccommodationDateItem[] = []
  const today = getTodayDate()

  for (let index = 0; index < days; index += 1) {
    const currentDateText = shiftDate(startDate, index)
    const weekdayIndex = getBusinessDateWeekdayIndex(currentDateText)
    const weekday = getWeekdayText(weekdayIndex)
    const shortLabel = formatBusinessDateLabel(currentDateText, 'month-day')

    items.push({
      date: currentDateText,
      label: `${shortLabel} ${weekday}`,
      shortLabel,
      weekday,
      isToday: currentDateText === today,
      isWeekend: weekdayIndex === 0 || weekdayIndex === 6,
    })
  }

  return items
}

export const formatPercent = (value?: number | null) => {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '0%'
  }

  return `${Number(value).toFixed(0)}%`
}

export const formatDateTime = (value?: string) => {
  return formatStoreDateTime(value, 'date-time')
}

export const getCleaningTaskStatusText = (status: string) => {
  const statusKey = CLEANING_TASK_STATUS_TEXT_KEY_MAP[status]
  return statusKey ? accommodationText(`cleaningStatus.${statusKey}`) : status || accommodationText('cleaningStatus.unknown')
}

export const getCleaningTaskTypeText = (taskType: string) => {
  const typeKey = CLEANING_TASK_TYPE_TEXT_KEY_MAP[taskType]
  return typeKey ? accommodationText(`cleaningType.${typeKey}`) : taskType || accommodationText('cleaningType.unset')
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
  return accommodationText('system')
}
