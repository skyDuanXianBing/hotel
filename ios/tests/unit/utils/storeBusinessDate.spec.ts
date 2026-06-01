import { beforeEach, describe, expect, test } from 'vitest'
import { CURRENT_STORE_KEY } from '@/utils/storage'
import {
  DEFAULT_BUSINESS_TIME_ZONE,
  buildBusinessDateRange,
  buildBusinessMonthRange,
  compareBusinessDates,
  diffBusinessDates,
  formatBusinessDateLabel,
  formatStoreDateTime,
  getBusinessDateWeekdayLabel,
  getStoreDatePresetRange,
  getStoreTodayDate,
  isValidBusinessTimeZone,
  shiftBusinessDate,
  toStoreDatetimeLocalValue,
  toStoreServerDatetime,
} from '@/utils/storeBusinessDate'

const TOKYO_BOUNDARY_INSTANT = new Date('2026-01-01T15:30:00.000Z')

describe('storeBusinessDate', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  test('uses explicit store timezone before the fallback timezone', () => {
    expect(getStoreTodayDate(TOKYO_BOUNDARY_INSTANT, 'Asia/Tokyo')).toBe('2026-01-02')
    expect(getStoreTodayDate(TOKYO_BOUNDARY_INSTANT, 'America/New_York')).toBe('2026-01-01')
  })

  test('reads current store timezone from local storage', () => {
    window.localStorage.setItem(
      CURRENT_STORE_KEY,
      JSON.stringify({
        id: 1,
        name: 'New York Store',
        timezone: 'America/New_York',
      }),
    )

    expect(getStoreTodayDate(TOKYO_BOUNDARY_INSTANT)).toBe('2026-01-01')
  })

  test('falls back to Asia Tokyo for missing or invalid timezone values', () => {
    window.localStorage.setItem(
      CURRENT_STORE_KEY,
      JSON.stringify({
        id: 2,
        name: 'Invalid Store',
        timezone: 'Invalid/Zone',
      }),
    )

    expect(DEFAULT_BUSINESS_TIME_ZONE).toBe('Asia/Tokyo')
    expect(isValidBusinessTimeZone('Invalid/Zone')).toBe(false)
    expect(getStoreTodayDate(TOKYO_BOUNDARY_INSTANT)).toBe('2026-01-02')
  })

  test('shifts date-only business dates without device timezone conversion', () => {
    expect(shiftBusinessDate('2026-03-01', -1)).toBe('2026-02-28')
    expect(buildBusinessDateRange('2026-12-30', 4)).toEqual([
      '2026-12-30',
      '2026-12-31',
      '2027-01-01',
      '2027-01-02',
    ])
  })

  test('formats date-only labels and weekdays without native Date parsing', () => {
    expect(formatBusinessDateLabel('2026-03-01', 'month-day-weekday')).toBe('03-01 周日')
    expect(getBusinessDateWeekdayLabel('2026-03-02', '周')).toBe('周一')
    expect(diffBusinessDates('2026-02-27', '2026-03-02')).toBe(3)
    expect(compareBusinessDates('2026-03-02', '2026-03-01')).toBeGreaterThan(0)
  })

  test('builds preset ranges from the store timezone date', () => {
    expect(getStoreDatePresetRange('today', TOKYO_BOUNDARY_INSTANT, 'Asia/Tokyo')).toEqual({
      startDate: '2026-01-02',
      endDate: '2026-01-02',
    })
    expect(getStoreDatePresetRange('yesterday', TOKYO_BOUNDARY_INSTANT, 'Asia/Tokyo')).toEqual({
      startDate: '2026-01-01',
      endDate: '2026-01-01',
    })
    expect(getStoreDatePresetRange('week', new Date('2026-03-04T01:00:00.000Z'), 'Asia/Tokyo')).toEqual({
      startDate: '2026-03-02',
      endDate: '2026-03-04',
    })
    expect(getStoreDatePresetRange('month', new Date('2026-02-10T01:00:00.000Z'), 'Asia/Tokyo')).toEqual({
      startDate: '2026-02-01',
      endDate: '2026-02-28',
    })
  })

  test('formats instants and store-local datetime values in the store timezone', () => {
    expect(formatStoreDateTime(TOKYO_BOUNDARY_INSTANT, 'date-time', '-', 'Asia/Tokyo')).toBe(
      '2026-01-02 00:30',
    )
    expect(formatStoreDateTime('2026-01-02 00:30:45', 'date-time', '-', 'Asia/Tokyo')).toBe(
      '2026-01-02 00:30',
    )
    expect(formatStoreDateTime('2026-01-02 00:30:45.123', 'date-time', '-', 'Asia/Tokyo')).toBe(
      '2026-01-02 00:30',
    )
    expect(formatStoreDateTime('2026-01-02T00:30', 'month-day-time', '-', 'Asia/Tokyo')).toBe(
      '01-02 00:30',
    )
  })

  test('normalizes datetime-local input and server datetime without device timezone conversion', () => {
    expect(toStoreDatetimeLocalValue(TOKYO_BOUNDARY_INSTANT, 'Asia/Tokyo')).toBe('2026-01-02T00:30')
    expect(toStoreServerDatetime('2026-01-02T00:30', 'Asia/Tokyo')).toBe('2026-01-02 00:30:00')
    expect(toStoreServerDatetime(TOKYO_BOUNDARY_INSTANT, 'Asia/Tokyo')).toBe('2026-01-02 00:30:00')
  })

  test('builds month ranges from business month values', () => {
    expect(buildBusinessMonthRange('2026-02')).toEqual({
      startDate: '2026-02-01',
      endDate: '2026-02-28',
    })
  })
})
