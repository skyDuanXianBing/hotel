export const FALLBACK_STORE_TIME_ZONE = 'Asia/Tokyo'
export const DEFAULT_RESERVATION_TIMESTAMP_SOURCE_ZONE = 'Asia/Shanghai'

const LOCAL_DATE_TIME_PATTERN =
  /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?$/
const HAS_TIMEZONE_PATTERN = /([zZ]|[+\-]\d{2}:?\d{2})$/
const YMD_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/
const YMD_PREFIX_PATTERN = /^(\d{4})-(\d{2})-(\d{2})/
const DAYS_PER_WEEK = 7
const WEEK_START_OFFSET = 0
const MS_PER_DAY = 24 * 60 * 60 * 1000

const isValidTimeZone = (timezone: string): boolean => {
  try {
    new Intl.DateTimeFormat('en-US', { timeZone: timezone })
    return true
  } catch {
    return false
  }
}

const normalizeTimeZone = (timezone?: string | null): string | null => {
  const candidate = (timezone || '').trim()
  if (!candidate) {
    return null
  }
  return isValidTimeZone(candidate) ? candidate : null
}

export const getDefaultStoreTimeZone = (): string => {
  return normalizeTimeZone(import.meta.env.VITE_DEFAULT_STORE_TIME_ZONE) || FALLBACK_STORE_TIME_ZONE
}

export const DEFAULT_STORE_TIME_ZONE = getDefaultStoreTimeZone()

export const resolveStoreTimeZone = (timezone?: string | null): string => {
  return normalizeTimeZone(timezone) || getDefaultStoreTimeZone()
}

export const resolveStoreTimeZoneFromStorage = (): string => {
  if (typeof localStorage === 'undefined') {
    return getDefaultStoreTimeZone()
  }

  const rawStore = localStorage.getItem('currentStore')
  if (!rawStore) {
    return getDefaultStoreTimeZone()
  }

  try {
    const parsed = JSON.parse(rawStore) as { timezone?: string }
    return resolveStoreTimeZone(parsed.timezone)
  } catch {
    return getDefaultStoreTimeZone()
  }
}

export const parseUtcDateTime = (rawValue?: string | Date): Date => {
  if (rawValue instanceof Date) {
    return rawValue
  }

  const normalized = (rawValue || '').trim()
  if (!normalized) {
    return new Date()
  }

  const text = normalized.includes('T') ? normalized : normalized.replace(' ', 'T')

  if (HAS_TIMEZONE_PATTERN.test(text)) {
    const parsedWithTimezone = new Date(text)
    if (!Number.isNaN(parsedWithTimezone.getTime())) {
      return parsedWithTimezone
    }
  }

  const matched = text.match(LOCAL_DATE_TIME_PATTERN)
  if (matched) {
    const [, yearText, monthText, dayText, hourText, minuteText, secondText] = matched
    const utcTimestamp = Date.UTC(
      Number(yearText),
      Number(monthText) - 1,
      Number(dayText),
      Number(hourText),
      Number(minuteText),
      Number(secondText || '0'),
    )
    return new Date(utcTimestamp)
  }

  const fallbackParsed = new Date(text)
  return Number.isNaN(fallbackParsed.getTime()) ? new Date() : fallbackParsed
}

const resolveReservationTimestampSourceZone = (timezone?: string | null): string => {
  return normalizeTimeZone(timezone) || DEFAULT_RESERVATION_TIMESTAMP_SOURCE_ZONE
}

const getTimeZoneOffsetMs = (date: Date, timeZone: string): number => {
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
    hourCycle: 'h23',
  })
  const parts = formatter.formatToParts(date)
  const valueByType: Record<string, string> = {}

  for (const part of parts) {
    if (part.type !== 'literal') {
      valueByType[part.type] = part.value
    }
  }

  const zonedAsUtc = Date.UTC(
    Number(valueByType.year),
    Number(valueByType.month) - 1,
    Number(valueByType.day),
    Number(valueByType.hour),
    Number(valueByType.minute),
    Number(valueByType.second),
  )
  return zonedAsUtc - date.getTime()
}

const parseSourceZoneDateTime = (
  rawValue?: string | Date,
  sourceTimeZone?: string | null,
): Date => {
  if (rawValue instanceof Date) {
    return rawValue
  }

  const normalized = (rawValue || '').trim()
  if (!normalized) {
    return new Date()
  }

  const text = normalized.includes('T') ? normalized : normalized.replace(' ', 'T')
  if (HAS_TIMEZONE_PATTERN.test(text)) {
    const parsedWithTimezone = new Date(text)
    if (!Number.isNaN(parsedWithTimezone.getTime())) {
      return parsedWithTimezone
    }
  }

  const matched = text.match(LOCAL_DATE_TIME_PATTERN)
  if (!matched) {
    return parseUtcDateTime(rawValue)
  }

  const [, yearText, monthText, dayText, hourText, minuteText, secondText] = matched
  const sourceZone = resolveReservationTimestampSourceZone(sourceTimeZone)
  const utcGuess = Date.UTC(
    Number(yearText),
    Number(monthText) - 1,
    Number(dayText),
    Number(hourText),
    Number(minuteText),
    Number(secondText || '0'),
  )
  const firstOffset = getTimeZoneOffsetMs(new Date(utcGuess), sourceZone)
  const firstInstant = new Date(utcGuess - firstOffset)
  const secondOffset = getTimeZoneOffsetMs(firstInstant, sourceZone)

  if (secondOffset !== firstOffset) {
    return new Date(utcGuess - secondOffset)
  }
  return firstInstant
}

const getFormatterPart = (
  formatter: Intl.DateTimeFormat,
  date: Date,
  partType: 'year' | 'month' | 'day' | 'hour' | 'minute' | 'second',
): string => {
  return formatter.formatToParts(date).find((part) => part.type === partType)?.value || '00'
}

export const formatStoreDate = (rawValue: string | Date, storeTimeZone: string): string => {
  const date = parseUtcDateTime(rawValue)
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    timeZone: resolveStoreTimeZone(storeTimeZone),
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })

  const year = getFormatterPart(formatter, date, 'year')
  const month = getFormatterPart(formatter, date, 'month')
  const day = getFormatterPart(formatter, date, 'day')
  return `${year}-${month}-${day}`
}

export const getStoreTodayYmd = (storeTimeZone?: string): string => {
  const timeZone = storeTimeZone
    ? resolveStoreTimeZone(storeTimeZone)
    : resolveStoreTimeZoneFromStorage()
  return formatStoreDate(new Date(), timeZone)
}

export const getDefaultStoreTodayYmd = (): string => {
  return formatStoreDate(new Date(), getDefaultStoreTimeZone())
}

export const normalizeYmdInput = (
  rawValue?: string | Date | null,
  fallbackYmd = getStoreTodayYmd(),
): string => {
  if (rawValue instanceof Date) {
    return formatStoreDate(rawValue, resolveStoreTimeZoneFromStorage())
  }

  const normalized = (rawValue || '').trim()
  const matched = normalized.match(YMD_PREFIX_PATTERN)
  if (matched) {
    return `${matched[1]}-${matched[2]}-${matched[3]}`
  }

  const fallback = (fallbackYmd || '').trim()
  const fallbackMatched = fallback.match(YMD_PREFIX_PATTERN)
  if (fallbackMatched) {
    return `${fallbackMatched[1]}-${fallbackMatched[2]}-${fallbackMatched[3]}`
  }

  return fallback
}

export const getStoreCurrentMonthYm = (storeTimeZone?: string): string => {
  return getStoreTodayYmd(storeTimeZone).slice(0, 7)
}

const parseYmdParts = (ymd: string): { year: number; month: number; day: number } => {
  const matched = ymd.match(YMD_PATTERN)
  if (!matched) {
    const today = normalizeYmdInput(ymd)
    const fallbackMatched = today.match(YMD_PATTERN)
    if (!fallbackMatched) {
      return { year: 1970, month: 1, day: 1 }
    }
    return {
      year: Number(fallbackMatched[1]),
      month: Number(fallbackMatched[2]),
      day: Number(fallbackMatched[3]),
    }
  }

  return {
    year: Number(matched[1]),
    month: Number(matched[2]),
    day: Number(matched[3]),
  }
}

const formatUtcDateYmd = (date: Date): string => {
  const year = date.getUTCFullYear()
  const month = String(date.getUTCMonth() + 1).padStart(2, '0')
  const day = String(date.getUTCDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatLocalDateTime = (
  year: string,
  month: string,
  day: string,
  hour: string,
  minute: string,
  second: string,
): string => {
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}

export const parseYmdAsUtcDate = (ymd: string): Date => {
  const { year, month, day } = parseYmdParts(ymd)
  return new Date(Date.UTC(year, month - 1, day))
}

export const addDaysToYmd = (ymd: string, offsetDays: number): string => {
  const date = parseYmdAsUtcDate(normalizeYmdInput(ymd))
  date.setUTCDate(date.getUTCDate() + offsetDays)
  return formatUtcDateYmd(date)
}

export const getStoreLocalDateTime = (storeTimeZone?: string, date = new Date()): string => {
  const timeZone = storeTimeZone
    ? resolveStoreTimeZone(storeTimeZone)
    : resolveStoreTimeZoneFromStorage()
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
    hourCycle: 'h23',
  })

  const year = getFormatterPart(formatter, date, 'year')
  const month = getFormatterPart(formatter, date, 'month')
  const day = getFormatterPart(formatter, date, 'day')
  const hour = getFormatterPart(formatter, date, 'hour')
  const minute = getFormatterPart(formatter, date, 'minute')
  const second = getFormatterPart(formatter, date, 'second')
  return formatLocalDateTime(year, month, day, hour, minute, second)
}

export const addDaysToLocalDateTime = (localDateTime: string, offsetDays: number): string => {
  const matched = localDateTime.trim().match(LOCAL_DATE_TIME_PATTERN)
  if (!matched) {
    const fallbackLocalDateTime = getStoreLocalDateTime()
    return addDaysToLocalDateTime(fallbackLocalDateTime, offsetDays)
  }

  const [, year, month, day, hour, minute, secondText] = matched
  const nextYmd = addDaysToYmd(`${year}-${month}-${day}`, offsetDays)
  return `${nextYmd}T${hour}:${minute}:${secondText || '00'}`
}

export const addCalendarMonthsToYmd = (ymd: string, offsetMonths: number): string => {
  const { year, month, day } = parseYmdParts(ymd)
  const date = new Date(Date.UTC(year, month - 1 + offsetMonths, day))
  return formatUtcDateYmd(date)
}

export const getYmdMonthStart = (ymd: string): string => {
  const { year, month } = parseYmdParts(ymd)
  return `${year}-${String(month).padStart(2, '0')}-01`
}

export const getYmdWeekStart = (ymd: string): string => {
  const date = parseYmdAsUtcDate(ymd)
  const weekday = date.getUTCDay()
  return addDaysToYmd(ymd, WEEK_START_OFFSET - weekday)
}

export const getYmdWeekdayIndex = (ymd: string): number => {
  return parseYmdAsUtcDate(normalizeYmdInput(ymd)).getUTCDay()
}

export const diffYmdDays = (startYmd: string, endYmd: string): number => {
  const start = parseYmdAsUtcDate(normalizeYmdInput(startYmd))
  const end = parseYmdAsUtcDate(normalizeYmdInput(endYmd))
  return Math.round((end.getTime() - start.getTime()) / MS_PER_DAY)
}

export const getYmdRange = (startYmd: string, endYmd: string): string[] => {
  const start = normalizeYmdInput(startYmd)
  const end = normalizeYmdInput(endYmd)
  const dayDiff = diffYmdDays(start, end)
  if (dayDiff < 0) {
    return []
  }

  const dates: string[] = []
  for (let offset = 0; offset <= dayDiff; offset += 1) {
    dates.push(addDaysToYmd(start, offset))
  }
  return dates
}

export const getRecentStoreDateRange = (days = DAYS_PER_WEEK, endYmd = getStoreTodayYmd()) => {
  return {
    start: addDaysToYmd(endYmd, -(days - 1)),
    end: endYmd,
  }
}

export const getStoreDateRange = (startYmd: string, days: number): string[] => {
  const dates: string[] = []
  for (let index = 0; index < days; index += 1) {
    dates.push(addDaysToYmd(startYmd, index))
  }
  return dates
}

export const getStoreDateKey = (rawValue: string | Date, storeTimeZone?: string): string => {
  const timeZone = storeTimeZone
    ? resolveStoreTimeZone(storeTimeZone)
    : resolveStoreTimeZoneFromStorage()
  return formatStoreDate(rawValue, timeZone)
}

export const formatYmdMonthDay = (ymd: string): { month: string; day: string } => {
  const { month, day } = parseYmdParts(ymd)
  return {
    month: String(month).padStart(2, '0'),
    day: String(day).padStart(2, '0'),
  }
}

export const formatStoreDateTime = (
  rawValue: string | Date,
  storeTimeZone: string,
  includeSeconds = false,
): string => {
  const date = parseUtcDateTime(rawValue)
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    timeZone: resolveStoreTimeZone(storeTimeZone),
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: includeSeconds ? '2-digit' : undefined,
    hour12: false,
  })

  const year = getFormatterPart(formatter, date, 'year')
  const month = getFormatterPart(formatter, date, 'month')
  const day = getFormatterPart(formatter, date, 'day')
  const hour = getFormatterPart(formatter, date, 'hour')
  const minute = getFormatterPart(formatter, date, 'minute')

  if (!includeSeconds) {
    return `${year}-${month}-${day} ${hour}:${minute}`
  }

  const second = getFormatterPart(formatter, date, 'second')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

export const formatBackendDateTime = (
  rawValue?: string | Date | null,
  storeTimeZone?: string | null,
  includeSeconds = false,
): string => {
  if (!rawValue) {
    return '-'
  }
  const timeZone = storeTimeZone
    ? resolveStoreTimeZone(storeTimeZone)
    : resolveStoreTimeZoneFromStorage()
  return formatStoreDateTime(rawValue, timeZone, includeSeconds)
}

export const formatReservationTimestamp = (
  rawValue: string | Date,
  sourceTimeZone: string | null | undefined,
  storeTimeZone: string,
  includeSeconds = false,
): string => {
  const date = parseSourceZoneDateTime(rawValue, sourceTimeZone)
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    timeZone: resolveStoreTimeZone(storeTimeZone),
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: includeSeconds ? '2-digit' : undefined,
    hour12: false,
  })

  const year = getFormatterPart(formatter, date, 'year')
  const month = getFormatterPart(formatter, date, 'month')
  const day = getFormatterPart(formatter, date, 'day')
  const hour = getFormatterPart(formatter, date, 'hour')
  const minute = getFormatterPart(formatter, date, 'minute')

  if (!includeSeconds) {
    return `${year}-${month}-${day} ${hour}:${minute}`
  }

  const second = getFormatterPart(formatter, date, 'second')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

export const formatStoreTime = (rawValue: string | Date, storeTimeZone: string): string => {
  const date = parseUtcDateTime(rawValue)
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    timeZone: resolveStoreTimeZone(storeTimeZone),
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })

  const hour = getFormatterPart(formatter, date, 'hour')
  const minute = getFormatterPart(formatter, date, 'minute')
  return `${hour}:${minute}`
}
