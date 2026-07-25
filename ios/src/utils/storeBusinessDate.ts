import { getStoredCurrentStore } from '@/utils/storage'
import { getIntlLocale } from '@/utils/formatters'

export const DEFAULT_BUSINESS_TIME_ZONE = 'Asia/Tokyo'

const DATE_PART_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/
const DATETIME_PART_PATTERN = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.\d{1,9})?)?$/
const MONTH_PART_PATTERN = /^(\d{4})-(\d{2})$/
const EXPLICIT_TIME_ZONE_PATTERN = /(Z|[+-]\d{2}:?\d{2})$/i
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

export interface BusinessDateTimeParts extends BusinessDateParts {
  hours: number
  minutes: number
  seconds: number
}

export type StoreDatePreset = 'today' | 'yesterday' | 'week' | 'month'

export interface StoreDatePresetRange {
  startDate: string
  endDate: string
}

export type BusinessDateLabelFormat =
  | 'date'
  | 'slash-date'
  | 'month-day'
  | 'month-day-weekday'
  | 'weekday'

export type StoreDateTimeFormat = 'date-time' | 'month-day-time' | 'time' | 'date' | 'month-day'

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

function formatDateTimeParts(parts: BusinessDateTimeParts, dateTimeSeparator: 'T' | ' ') {
  const date = formatParts(parts)
  const time = [
    String(parts.hours).padStart(2, '0'),
    String(parts.minutes).padStart(2, '0'),
    String(parts.seconds).padStart(2, '0'),
  ].join(':')

  return `${date}${dateTimeSeparator}${time}`
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

export function parseStoreDateTimeParts(value: string): BusinessDateTimeParts | null {
  const match = DATETIME_PART_PATTERN.exec(value.trim())
  if (!match) {
    return null
  }

  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const hours = Number(match[4])
  const minutes = Number(match[5])
  const seconds = Number(match[6] ?? 0)

  if (hours > 23 || minutes > 59 || seconds > 59) {
    return null
  }

  const dateParts = parseBusinessDateParts(formatParts({ year, month, day }))
  if (!dateParts) {
    return null
  }

  return {
    ...dateParts,
    hours,
    minutes,
    seconds,
  }
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

function toValidInstant(value: Date | string) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return null
  }

  return date
}

function getStoreDateTimePartsFromInstant(value: Date | string, storeTimeZone?: string | null) {
  const instant = toValidInstant(value)
  if (!instant) {
    return null
  }

  const timeZone = resolveBusinessTimeZone(storeTimeZone ?? getStoredCurrentStore()?.timezone)
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  })
  const parts = formatter.formatToParts(instant)
  const year = Number(parts.find((part) => part.type === 'year')?.value || 1970)
  const month = Number(parts.find((part) => part.type === 'month')?.value || 1)
  const day = Number(parts.find((part) => part.type === 'day')?.value || 1)
  const hours = Number(parts.find((part) => part.type === 'hour')?.value || 0)
  const minutes = Number(parts.find((part) => part.type === 'minute')?.value || 0)
  const seconds = Number(parts.find((part) => part.type === 'second')?.value || 0)

  return {
    year,
    month,
    day,
    hours,
    minutes,
    seconds,
  }
}

function getStoreDateTimeParts(value: Date | string, storeTimeZone?: string | null) {
  if (value instanceof Date) {
    return getStoreDateTimePartsFromInstant(value, storeTimeZone)
  }

  const trimmedValue = value.trim()
  const localParts = parseStoreDateTimeParts(trimmedValue)
  if (localParts && !EXPLICIT_TIME_ZONE_PATTERN.test(trimmedValue)) {
    return localParts
  }

  const dateParts = parseBusinessDateParts(trimmedValue)
  if (dateParts) {
    return {
      ...dateParts,
      hours: 0,
      minutes: 0,
      seconds: 0,
    }
  }

  return getStoreDateTimePartsFromInstant(trimmedValue, storeTimeZone)
}

function getDateTimeFormatText(
  parts: BusinessDateTimeParts,
  format: StoreDateTimeFormat,
  locale = getIntlLocale(),
) {
  const date = new Date(
    Date.UTC(parts.year, parts.month - 1, parts.day, parts.hours, parts.minutes, parts.seconds),
  )
  const baseOptions: Intl.DateTimeFormatOptions = { timeZone: 'UTC' }

  if (format === 'time') {
    return new Intl.DateTimeFormat(locale, {
      ...baseOptions,
      hour: '2-digit',
      minute: '2-digit',
    }).format(date)
  }

  if (format === 'date') {
    return new Intl.DateTimeFormat(locale, {
      ...baseOptions,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(date)
  }

  if (format === 'month-day') {
    return new Intl.DateTimeFormat(locale, {
      ...baseOptions,
      month: '2-digit',
      day: '2-digit',
    }).format(date)
  }

  if (format === 'month-day-time') {
    return new Intl.DateTimeFormat(locale, {
      ...baseOptions,
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date)
  }

  return new Intl.DateTimeFormat(locale, {
    ...baseOptions,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
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

export function getBusinessDateWeekdayLabel(date: string, prefix = '') {
  const parts = parseBusinessDateParts(normalizeBusinessDate(date))
  if (!parts) {
    return ''
  }

  const weekday = new Intl.DateTimeFormat(getIntlLocale(), {
    weekday: 'short',
    timeZone: 'UTC',
  }).format(toUtcDate(parts))
  return `${prefix}${weekday}`
}

export function diffBusinessDates(startDate: string, endDate: string) {
  const startParts = parseBusinessDateParts(startDate)
  const endParts = parseBusinessDateParts(endDate)
  if (!startParts || !endParts) {
    return 0
  }

  return Math.round((toUtcDate(endParts).getTime() - toUtcDate(startParts).getTime()) / MS_PER_DAY)
}

export function compareBusinessDates(firstDate: string, secondDate: string) {
  const firstParts = parseBusinessDateParts(firstDate)
  const secondParts = parseBusinessDateParts(secondDate)
  if (!firstParts && !secondParts) {
    return 0
  }
  if (!firstParts) {
    return -1
  }
  if (!secondParts) {
    return 1
  }

  return diffBusinessDates(secondDate, firstDate)
}

export function formatBusinessDateLabel(
  value?: string | null,
  format: BusinessDateLabelFormat = 'date',
  fallback = '-',
  locale = getIntlLocale(),
) {
  if (!value) {
    return fallback
  }

  const trimmedValue = value.trim()
  const dateParts = parseBusinessDateParts(trimmedValue) || getStoreDateTimeParts(trimmedValue)
  if (!dateParts) {
    return trimmedValue || fallback
  }

  const date = toUtcDate(dateParts)
  if (format === 'slash-date') {
    return new Intl.DateTimeFormat(locale, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      timeZone: 'UTC',
    }).format(date)
  }

  if (format === 'month-day') {
    return new Intl.DateTimeFormat(locale, {
      month: '2-digit',
      day: '2-digit',
      timeZone: 'UTC',
    }).format(date)
  }

  if (format === 'month-day-weekday') {
    return new Intl.DateTimeFormat(locale, {
      month: '2-digit',
      day: '2-digit',
      weekday: 'short',
      timeZone: 'UTC',
    }).format(date)
  }

  if (format === 'weekday') {
    return getBusinessDateWeekdayLabel(formatParts(dateParts))
  }

  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone: 'UTC',
  }).format(date)
}

export function formatStoreDateTime(
  value?: Date | string | null,
  format: StoreDateTimeFormat = 'date-time',
  fallback = '-',
  storeTimeZone?: string | null,
  locale = getIntlLocale(),
) {
  if (!value) {
    return fallback
  }

  const parts = getStoreDateTimeParts(value, storeTimeZone)
  if (!parts) {
    return typeof value === 'string' ? value : fallback
  }

  return getDateTimeFormatText(parts, format, locale)
}

export function getStoreBusinessDateFromDateTime(
  value?: Date | string | null,
  storeTimeZone?: string | null,
) {
  if (!value) {
    return ''
  }

  const parts = getStoreDateTimeParts(value, storeTimeZone)
  if (!parts) {
    return ''
  }

  return formatParts(parts)
}

export function toStoreDatetimeLocalValue(
  value: Date | string | null = new Date(),
  storeTimeZone?: string | null,
) {
  const parts = getStoreDateTimeParts(value || new Date(), storeTimeZone)
  if (!parts) {
    return ''
  }

  return formatDateTimeParts(parts, 'T').slice(0, 16)
}

export function toStoreServerDatetime(value?: Date | string | null, storeTimeZone?: string | null) {
  if (!value) {
    return ''
  }

  const parts = getStoreDateTimeParts(value, storeTimeZone)
  if (!parts) {
    return ''
  }

  return formatDateTimeParts(parts, ' ')
}

export function compareStoreDateTimes(firstValue?: Date | string | null, secondValue?: Date | string | null) {
  const firstText = toStoreServerDatetime(firstValue)
  const secondText = toStoreServerDatetime(secondValue)
  if (!firstText && !secondText) {
    return 0
  }
  if (!firstText) {
    return -1
  }
  if (!secondText) {
    return 1
  }

  return firstText.localeCompare(secondText)
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

export function getStoreDatePresetRange(
  preset: StoreDatePreset,
  now = new Date(),
  storeTimeZone?: string | null,
): StoreDatePresetRange {
  const today = getStoreTodayDate(now, storeTimeZone)

  if (preset === 'yesterday') {
    const yesterday = shiftBusinessDate(today, -1)
    return {
      startDate: yesterday,
      endDate: yesterday,
    }
  }

  if (preset === 'week') {
    const weekdayIndex = getBusinessDateWeekdayIndex(today)
    const offset = weekdayIndex === 0 ? 6 : weekdayIndex - 1
    return {
      startDate: shiftBusinessDate(today, -offset),
      endDate: today,
    }
  }

  if (preset === 'month') {
    return buildBusinessMonthRange(today.slice(0, 7))
  }

  return {
    startDate: today,
    endDate: today,
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
