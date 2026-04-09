export const DEFAULT_STORE_TIME_ZONE = 'Asia/Shanghai'

const LOCAL_DATE_TIME_PATTERN =
  /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?$/
const HAS_TIMEZONE_PATTERN = /([zZ]|[+\-]\d{2}:?\d{2})$/

export const resolveStoreTimeZone = (timezone?: string | null): string => {
  const candidate = (timezone || '').trim()
  if (!candidate) {
    return DEFAULT_STORE_TIME_ZONE
  }

  try {
    new Intl.DateTimeFormat('zh-CN', { timeZone: candidate })
    return candidate
  } catch {
    return DEFAULT_STORE_TIME_ZONE
  }
}

export const resolveStoreTimeZoneFromStorage = (): string => {
  const rawStore = localStorage.getItem('currentStore')
  if (!rawStore) {
    return DEFAULT_STORE_TIME_ZONE
  }

  try {
    const parsed = JSON.parse(rawStore) as { timezone?: string }
    return resolveStoreTimeZone(parsed.timezone)
  } catch {
    return DEFAULT_STORE_TIME_ZONE
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
      Number(secondText || '0')
    )
    return new Date(utcTimestamp)
  }

  const fallbackParsed = new Date(text)
  return Number.isNaN(fallbackParsed.getTime()) ? new Date() : fallbackParsed
}

const getFormatterPart = (
  formatter: Intl.DateTimeFormat,
  date: Date,
  partType: 'year' | 'month' | 'day' | 'hour' | 'minute' | 'second'
): string => {
  return formatter.formatToParts(date).find((part) => part.type === partType)?.value || '00'
}

export const formatStoreDateTime = (
  rawValue: string | Date,
  storeTimeZone: string,
  includeSeconds = false
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
