import { getStoredCurrentStore } from '@/utils/storage'

export const DEFAULT_BUSINESS_TIME_ZONE = 'Asia/Tokyo'

const DATE_PART_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/
const MONTH_PART_PATTERN = /^(\d{4})-(\d{2})$/
const DAYS_PER_WEEK = 7
const MONTH_GRID_DAYS = 42
const MS_PER_DAY = 24 * 60 * 60 * 1000

export interface BusinessDateParts {
  year: number
  month: number
  day: number
}

export interface BusinessMonthParts {
  year: number
  month: number
}

export interface BusinessMonthCalendarDate {
  date: string
  dayNumber: number
  isCurrentMonth: boolean
}

function normalizeTimeZone(value?: string | null) {
  if (typeof value !== 'string') {
    return ''
  }

  return value.trim()
}

export function isValidBusinessTimeZone(value?: string | null) {
  const timeZone = normalizeTimeZone(value)
  if (!timeZone) {
    return false
  }

  try {
    new Intl.DateTimeFormat('en-US', { timeZone }).format(new Date(0))
    return true
  } catch {
    return false
  }
}

export function resolveBusinessTimeZone(storeTimeZone?: string | null) {
  const timeZone = normalizeTimeZone(storeTimeZone)
  if (isValidBusinessTimeZone(timeZone)) {
    return timeZone
  }

  return DEFAULT_BUSINESS_TIME_ZONE
}

export function getCurrentStoreBusinessTimeZone() {
  const currentStore = getStoredCurrentStore()
  return resolveBusinessTimeZone(currentStore?.timezone)
}

export function getStoreTodayDate(now = new Date(), storeTimeZone?: string | null) {
  const timeZone = resolveBusinessTimeZone(storeTimeZone ?? getStoredCurrentStore()?.timezone)
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })

  const parts = formatter.formatToParts(now)
  const year = parts.find((part) => part.type === 'year')?.value || '1970'
  const month = parts.find((part) => part.type === 'month')?.value || '01'
  const day = parts.find((part) => part.type === 'day')?.value || '01'

  return `${year}-${month}-${day}`
}

function formatParts(parts: BusinessDateParts) {
  return [
    String(parts.year).padStart(4, '0'),
    String(parts.month).padStart(2, '0'),
    String(parts.day).padStart(2, '0'),
  ].join('-')
}

function formatMonthParts(parts: BusinessMonthParts) {
  return `${String(parts.year).padStart(4, '0')}-${String(parts.month).padStart(2, '0')}`
}

export function parseBusinessDateParts(value: string): BusinessDateParts | null {
  const match = DATE_PART_PATTERN.exec(value)
  if (!match) {
    return null
  }

  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const utcDate = new Date(Date.UTC(year, month - 1, day))

  if (
    utcDate.getUTCFullYear() !== year ||
    utcDate.getUTCMonth() !== month - 1 ||
    utcDate.getUTCDate() !== day
  ) {
    return null
  }

  return { year, month, day }
}

export function parseBusinessMonthParts(value: string): BusinessMonthParts | null {
  const match = MONTH_PART_PATTERN.exec(value)
  if (!match) {
    return null
  }

  const year = Number(match[1])
  const month = Number(match[2])
  if (!year || month < 1 || month > 12) {
    return null
  }

  return { year, month }
}

function normalizeBusinessDate(value: string) {
  return parseBusinessDateParts(value) ? value : getStoreTodayDate()
}

function toUtcDate(parts: BusinessDateParts) {
  return new Date(Date.UTC(parts.year, parts.month - 1, parts.day))
}

export function shiftBusinessDate(date: string, offsetDays: number) {
  const normalizedDate = normalizeBusinessDate(date)
  const parts = parseBusinessDateParts(normalizedDate)
  if (!parts) {
    return getStoreTodayDate()
  }

  const timestamp = toUtcDate(parts).getTime() + offsetDays * MS_PER_DAY
  const shiftedDate = new Date(timestamp)

  return formatParts({
    year: shiftedDate.getUTCFullYear(),
    month: shiftedDate.getUTCMonth() + 1,
    day: shiftedDate.getUTCDate(),
  })
}

export function getBusinessDateWeekdayIndex(date: string) {
  const normalizedDate = normalizeBusinessDate(date)
  const parts = parseBusinessDateParts(normalizedDate)
  if (!parts) {
    return 0
  }

  return toUtcDate(parts).getUTCDay()
}

export function buildBusinessDateRange(startDate: string, days: number) {
  const dates: string[] = []
  const totalDays = Math.max(0, Math.floor(days))

  for (let index = 0; index < totalDays; index += 1) {
    dates.push(shiftBusinessDate(startDate, index))
  }

  return dates
}

export function getStoreCurrentMonthValue(now = new Date(), storeTimeZone?: string | null) {
  return getStoreTodayDate(now, storeTimeZone).slice(0, 7)
}

export function shiftBusinessMonth(monthValue: string, offsetMonths: number) {
  const parts = parseBusinessMonthParts(monthValue)
  const baseParts = parts || parseBusinessMonthParts(getStoreCurrentMonthValue())
  if (!baseParts) {
    return getStoreCurrentMonthValue()
  }

  const shiftedDate = new Date(Date.UTC(baseParts.year, baseParts.month - 1 + offsetMonths, 1))
  return formatMonthParts({
    year: shiftedDate.getUTCFullYear(),
    month: shiftedDate.getUTCMonth() + 1,
  })
}

export function buildBusinessMonthRange(monthValue: string) {
  const parts = parseBusinessMonthParts(monthValue) || parseBusinessMonthParts(getStoreCurrentMonthValue())
  if (!parts) {
    const today = getStoreTodayDate()
    return { startDate: today, endDate: today }
  }

  const lastDay = new Date(Date.UTC(parts.year, parts.month, 0)).getUTCDate()

  return {
    startDate: formatParts({ year: parts.year, month: parts.month, day: 1 }),
    endDate: formatParts({ year: parts.year, month: parts.month, day: lastDay }),
  }
}

export function buildBusinessMonthCalendarDates(monthValue: string): BusinessMonthCalendarDate[] {
  const parts = parseBusinessMonthParts(monthValue) || parseBusinessMonthParts(getStoreCurrentMonthValue())
  if (!parts) {
    const today = getStoreTodayDate()
    const todayParts = parseBusinessDateParts(today)
    return [
      {
        date: today,
        dayNumber: todayParts?.day || 1,
        isCurrentMonth: true,
      },
    ]
  }

  const firstDate = new Date(Date.UTC(parts.year, parts.month - 1, 1))
  const firstWeekday = firstDate.getUTCDay()
  const gridStartTimestamp = firstDate.getTime() - firstWeekday * MS_PER_DAY
  const dates: BusinessMonthCalendarDate[] = []

  for (let index = 0; index < MONTH_GRID_DAYS; index += 1) {
    const currentDate = new Date(gridStartTimestamp + index * MS_PER_DAY)
    const currentMonth = currentDate.getUTCMonth() + 1
    dates.push({
      date: formatParts({
        year: currentDate.getUTCFullYear(),
        month: currentMonth,
        day: currentDate.getUTCDate(),
      }),
      dayNumber: currentDate.getUTCDate(),
      isCurrentMonth: currentMonth === parts.month,
    })
  }

  if (dates.length % DAYS_PER_WEEK !== 0) {
    return dates.slice(0, dates.length - (dates.length % DAYS_PER_WEEK))
  }

  return dates
}
