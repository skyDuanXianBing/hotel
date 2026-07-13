import { describe, expect, test } from 'bun:test'
import {
  buildCalendarWindowKey,
  CalendarWindowBackoffError,
  CalendarWindowCache,
  shiftCalendarWindow,
} from '../src/views/room-status/calendarWindowCache'

describe('room status calendar neighbor ranges', () => {
  test('reuses the exact 30-day navigation shift across month and year boundaries', () => {
    expect(shiftCalendarWindow(['2026-01-30', '2026-02-27'], 30)).toEqual([
      '2026-03-01',
      '2026-03-29',
    ])
    expect(shiftCalendarWindow(['2024-02-01', '2024-02-29'], -30)).toEqual([
      '2024-01-02',
      '2024-01-30',
    ])
    expect(shiftCalendarWindow(['2026-12-15', '2027-01-14'], 30)).toEqual([
      '2027-01-14',
      '2027-02-13',
    ])
  })

  test('isolates user, store, timezone, permission context and exact range', () => {
    const base = { userId: 1, storeId: 2, timezone: 'Asia/Tokyo', permissionContext: 'owner' }
    const key = buildCalendarWindowKey(base, ['2026-07-01', '2026-07-31'])
    expect(buildCalendarWindowKey({ ...base, storeId: 3 }, ['2026-07-01', '2026-07-31'])).not.toBe(key)
    expect(buildCalendarWindowKey({ ...base, timezone: 'UTC' }, ['2026-07-01', '2026-07-31'])).not.toBe(key)
    expect(buildCalendarWindowKey({ ...base, permissionContext: 'limited' }, ['2026-07-01', '2026-07-31'])).not.toBe(key)
    expect(buildCalendarWindowKey(base, ['2026-07-02', '2026-08-01'])).not.toBe(key)
  })
})

describe('room status calendar LRU and request coordination', () => {
  test('expires entries and evicts least recently used windows', () => {
    const cache = new CalendarWindowCache<number>({ maxEntries: 3, ttlMs: 100 })
    cache.set('a', 1, 0)
    cache.set('b', 2, 0)
    cache.set('c', 3, 0)
    expect(cache.get('a', 50)).toBe(1)
    cache.set('d', 4, 50)
    expect(cache.get('b', 50)).toBeUndefined()
    expect(cache.get('a', 101)).toBeUndefined()
    expect(cache.size).toBe(2)
  })

  test('shares a pending request and caches its result', async () => {
    const cache = new CalendarWindowCache<number>({ jitter: () => 0.5 })
    let calls = 0
    let resolve!: (value: number) => void
    const loader = () => {
      calls += 1
      return new Promise<number>((done) => {
        resolve = done
      })
    }
    const first = cache.load('same', loader)
    const second = cache.load('same', loader)
    expect(first).toBe(second)
    expect(calls).toBe(1)
    resolve(7)
    await expect(first).resolves.toBe(7)
    expect(cache.get('same')).toBe(7)
  })

  test('backs failures off exponentially and allows foreground force retry', async () => {
    const cache = new CalendarWindowCache<number>({
      baseBackoffMs: 5_000,
      maxBackoffMs: 60_000,
      jitter: () => 0.5,
    })
    await expect(cache.load('failed', async () => Promise.reject(new Error('no')))).rejects.toThrow('no')
    const retryAt = cache.retryAt('failed')
    expect(retryAt).toBeGreaterThan(Date.now())
    await expect(cache.load('failed', async () => 1)).rejects.toBeInstanceOf(CalendarWindowBackoffError)
    await expect(cache.load('failed', async () => 2, { force: true })).resolves.toBe(2)
  })
})
