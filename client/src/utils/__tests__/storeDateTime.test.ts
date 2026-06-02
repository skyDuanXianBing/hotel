import {
  addDaysToYmd,
  diffYmdDays,
  FALLBACK_STORE_TIME_ZONE,
  formatBackendDateTime,
  formatReservationTimestamp,
  formatStoreDate,
  formatStoreDateTime,
  getDefaultStoreTimeZone,
  getStoreCurrentMonthYm,
  getStoreDateKey,
  getRecentStoreDateRange,
  getStoreDateRange,
  getYmdRange,
  getYmdWeekdayIndex,
  normalizeYmdInput,
  parseYmdAsUtcDate,
  resolveStoreTimeZone,
} from '../storeDateTime'

const fail = (message: string): never => {
  console.error(`[storeDateTime.test] ${message}`)
  process.exit(1)
}

const assertEquals = (actual: unknown, expected: unknown, message: string): void => {
  if (actual !== expected) {
    fail(`${message}. Expected ${String(expected)}, got ${String(actual)}`)
  }
}

const assertArrayEquals = (actual: string[], expected: string[], message: string): void => {
  if (actual.length !== expected.length) {
    fail(`${message}. Expected length ${expected.length}, got ${actual.length}`)
  }

  for (let index = 0; index < expected.length; index += 1) {
    if (actual[index] !== expected[index]) {
      fail(`${message}. Expected ${expected.join(',')}, got ${actual.join(',')}`)
    }
  }
}

const assertValidFallbackTimeZone = (): void => {
  assertEquals(FALLBACK_STORE_TIME_ZONE, 'Asia/Tokyo', 'fallback store timezone should be Tokyo')
  assertEquals(
    getDefaultStoreTimeZone(),
    'Asia/Tokyo',
    'default timezone should fall back to Tokyo',
  )
  assertEquals(
    resolveStoreTimeZone('bad/timezone'),
    'Asia/Tokyo',
    'invalid store timezone should fall back to Tokyo',
  )
  assertEquals(
    resolveStoreTimeZone(' Asia/Tokyo '),
    'Asia/Tokyo',
    'valid timezone should be trimmed',
  )
  assertEquals(
    formatStoreDate('2026-04-07T15:30:00Z', resolveStoreTimeZone(undefined)),
    '2026-04-08',
    'Tokyo fallback should calculate the store business date',
  )
  assertEquals(
    formatStoreDateTime('2026-05-31T14:42:32', 'Asia/Tokyo', true),
    '2026-05-31 23:42:32',
    'naive UTC timestamps should display in the store timezone',
  )
}

const assertReservationTimestampSourceZoneFormatting = (): void => {
  assertEquals(
    formatReservationTimestamp('2026-05-31T14:42:32', 'Asia/Shanghai', 'Asia/Shanghai', true),
    '2026-05-31 14:42:32',
    'Shanghai storage-zone timestamp should display unchanged for Shanghai stores',
  )
  assertEquals(
    formatReservationTimestamp('2026-05-31T14:42:32', 'Asia/Shanghai', 'Asia/Tokyo', true),
    '2026-05-31 15:42:32',
    'Shanghai storage-zone timestamp should convert to Tokyo store time',
  )
  assertEquals(
    formatReservationTimestamp('2026-05-31T14:42:32', 'UTC', 'Asia/Tokyo', true),
    '2026-05-31 23:42:32',
    'UTC storage-zone timestamp should convert to Tokyo store time',
  )
  assertEquals(
    formatReservationTimestamp('2026-05-31T14:42:32', undefined, 'Asia/Tokyo', true),
    '2026-05-31 15:42:32',
    'missing source zone should fall back to production Shanghai storage semantics',
  )
}

const assertYmdArithmeticUsesUtcDateOnlyMath = (): void => {
  const parsed = parseYmdAsUtcDate('2026-03-08')

  assertEquals(
    parsed.toISOString(),
    '2026-03-08T00:00:00.000Z',
    'YMD parsing should use UTC midnight',
  )
  assertEquals(
    normalizeYmdInput('2026-03-08T23:30:00Z'),
    '2026-03-08',
    'YMD normalization should keep the date key',
  )
  assertEquals(
    addDaysToYmd('2026-03-08', 1),
    '2026-03-09',
    'adding days should ignore local DST jumps',
  )
  assertEquals(addDaysToYmd('2026-01-01', -1), '2025-12-31', 'subtracting days should cross years')
  assertEquals(diffYmdDays('2026-03-08', '2026-03-10'), 2, 'YMD diff should use calendar days')
  assertEquals(getYmdWeekdayIndex('2026-03-08'), 0, 'weekday should be derived from UTC YMD')
  assertArrayEquals(
    getYmdRange('2026-03-08', '2026-03-10'),
    ['2026-03-08', '2026-03-09', '2026-03-10'],
    'YMD range should be inclusive and date-only',
  )
  assertArrayEquals(
    getStoreDateRange('2026-12-30', 4),
    ['2026-12-30', '2026-12-31', '2027-01-01', '2027-01-02'],
    'date range should use date-only arithmetic',
  )

  const recentRange = getRecentStoreDateRange(3, '2027-01-02')
  assertEquals(recentRange.start, '2026-12-31', 'recent range start should be date-only')
  assertEquals(recentRange.end, '2027-01-02', 'recent range end should use provided YMD')
}

const assertStoreDateKeyAndBackendFormatting = (): void => {
  assertEquals(
    getStoreDateKey('2026-04-07T15:30:00Z', 'Asia/Tokyo'),
    '2026-04-08',
    'store date key should use the target store timezone',
  )
  assertEquals(
    getStoreCurrentMonthYm('Asia/Tokyo').length,
    7,
    'store current month helper should return YYYY-MM',
  )
  assertEquals(
    formatBackendDateTime('2026-05-31T14:42:32Z', 'Asia/Tokyo', false),
    '2026-05-31 23:42',
    'backend datetime with timezone should display by store timezone',
  )
  assertEquals(
    formatBackendDateTime('2026-05-31T14:42:32', 'Asia/Tokyo', true),
    '2026-05-31 23:42:32',
    'naive backend datetime should keep UTC compatibility in one helper',
  )
}

assertValidFallbackTimeZone()
assertReservationTimestampSourceZoneFormatting()
assertYmdArithmeticUsesUtcDateOnlyMath()
assertStoreDateKeyAndBackendFormatting()

console.log('[storeDateTime.test] ok: fallback timezone and YMD date-only helpers verified')
