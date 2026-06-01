import { beforeEach, describe, expect, test } from 'vitest'
import { CURRENT_STORE_KEY } from '@/utils/storage'
import {
  DEFAULT_BUSINESS_TIME_ZONE,
  buildBusinessDateRange,
  buildBusinessMonthRange,
  getStoreTodayDate,
  isValidBusinessTimeZone,
  shiftBusinessDate,
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

  test('builds month ranges from business month values', () => {
    expect(buildBusinessMonthRange('2026-02')).toEqual({
      startDate: '2026-02-01',
      endDate: '2026-02-28',
    })
  })
})
