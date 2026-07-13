import { describe, expect, test } from 'bun:test'
import {
  HoverSummaryCache,
  HoverSummaryRefreshGate,
  chunkReservationIds,
  didAllHoverSummaryBatchesSucceed,
  normalizeReservationIds,
  runWithConcurrency,
} from '../src/views/room-status/hoverSummaryCache'

describe('room status hover summary batching', () => {
  test('deduplicates positive ids in stable order and chunks 401 ids as 200/200/1', () => {
    expect(normalizeReservationIds([3, 1, 3, 0, -1, 2])).toEqual([3, 1, 2])
    expect(chunkReservationIds(Array.from({ length: 401 }, (_, index) => index + 1))).toEqual([
      Array.from({ length: 200 }, (_, index) => index + 1),
      Array.from({ length: 200 }, (_, index) => index + 201),
      [401],
    ])
  })

  test('limits concurrent batches while preserving result order and failures', async () => {
    let active = 0
    let maxActive = 0
    const tasks = Array.from({ length: 5 }, (_, index) => async () => {
      active += 1
      maxActive = Math.max(maxActive, active)
      await Promise.resolve()
      active -= 1
      if (index === 3) throw new Error('batch failed')
      return index
    })
    const results = await runWithConcurrency(tasks, 2)
    expect(maxActive).toBe(2)
    expect(results.map((result) => result.status)).toEqual([
      'fulfilled',
      'fulfilled',
      'fulfilled',
      'rejected',
      'fulfilled',
    ])
    expect(results[4]).toEqual({ status: 'fulfilled', value: 4 })
  })
})

describe('room status hover summary window refresh gate', () => {
  test('marks a window successful only when every non-empty batch succeeds', () => {
    expect(
      didAllHoverSummaryBatchesSucceed([
        { status: 'fulfilled', value: true },
        { status: 'fulfilled', value: true },
        { status: 'rejected', reason: new Error('last chunk failed') },
      ]),
    ).toBe(false)
    expect(
      didAllHoverSummaryBatchesSucceed([
        { status: 'fulfilled', value: true },
        { status: 'fulfilled', value: false },
      ]),
    ).toBe(false)
    expect(
      didAllHoverSummaryBatchesSucceed([
        { status: 'fulfilled', value: true },
        { status: 'fulfilled', value: true },
      ]),
    ).toBe(true)
    expect(didAllHoverSummaryBatchesSucceed([])).toBe(false)
  })

  test('allows one initial attempt, throttles failures, and refreshes the whole window after TTL', () => {
    const gate = new HoverSummaryRefreshGate(60_000, 5_000)
    gate.reset(7)
    expect(gate.canSchedule(7, 1_000)).toBe(true)
    gate.markScheduled(7, 1_000)
    expect(gate.canSchedule(7, 5_999)).toBe(false)
    expect(gate.canSchedule(7, 6_000)).toBe(true)
    gate.markScheduled(7, 6_000)
    gate.markSuccess(7, 6_100)
    expect(gate.canSchedule(7, 66_100)).toBe(false)
    expect(gate.canSchedule(7, 66_101)).toBe(true)
  })

  test('rejects stale generations and reset permits the new window only', () => {
    const gate = new HoverSummaryRefreshGate()
    gate.reset(2)
    gate.markScheduled(2, 100)
    gate.reset(3)
    expect(gate.canSchedule(2, 1_000_000)).toBe(false)
    expect(gate.canSchedule(3, 101)).toBe(true)
  })
})

describe('room status hover summary cache isolation', () => {
  test('keys by store, rejects stale generations, and distinguishes zero from missing', () => {
    const cache = new HoverSummaryCache<{ paidAmount?: number }>(60_000)
    cache.set(1, 9, 4, { paidAmount: 0 }, 1_000)
    cache.set(2, 9, 4, { paidAmount: 25 }, 1_000)

    expect(cache.get(1, 9, 4, 1_001)).toEqual({ paidAmount: 0 })
    expect(cache.get(2, 9, 4, 1_001)).toEqual({ paidAmount: 25 })
    expect(cache.get(1, 9, 5, 1_001)).toBeUndefined()
    expect(cache.get(1, 9, 4, 1_001)).toEqual({ paidAmount: 0 })
  })

  test('expires entries and supports explicit invalidation', () => {
    const cache = new HoverSummaryCache<string>(10)
    cache.set(1, 1, 1, 'fresh', 100)
    expect(cache.get(1, 1, 1, 110)).toBe('fresh')
    expect(cache.get(1, 1, 1, 111)).toBeUndefined()
    cache.set(1, 1, 1, 'again', 120)
    cache.delete(1, 1)
    expect(cache.get(1, 1, 1, 121)).toBeUndefined()
  })
})
