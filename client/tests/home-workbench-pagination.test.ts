import { describe, expect, test } from 'bun:test'
import type { HomeWorkbenchItemDTO } from '../src/api/homeWorkbench'
import {
  allowedStatusesByType,
  appendUniqueWorkbenchItems,
  getWorkbenchStatusLabelKey,
  normalizeWorkbenchStatus,
  resolveWorkbenchTypeCounts,
  toWorkbenchStatusParam,
} from '../src/views/home/composables/workbenchPagination'

const task = (id: number, title = `task-${id}`): HomeWorkbenchItemDTO => ({
  id: String(id), type: 'review', statusGroup: 'awaiting_review', title,
})

describe('home workbench labels and backend counts', () => {
  test('resolves the same status key differently by type', () => {
    expect(getWorkbenchStatusLabelKey('all', 'unassigned')).toBe(
      'pages.home.workbench.statuses.unassigned',
    )
    expect(getWorkbenchStatusLabelKey('other', 'unassigned')).toBe(
      'pages.home.workbench.internalTasks.unassigned',
    )
    expect(getWorkbenchStatusLabelKey('other', 'assigned')).toBe(
      'pages.home.workbench.internalTasks.assigned',
    )
    expect(getWorkbenchStatusLabelKey('other', 'completed')).toBe(
      'pages.home.workbench.internalTasks.completed',
    )
    expect(getWorkbenchStatusLabelKey('order', 'pending')).toBe(
      'pages.home.workbench.statuses.orderPending',
    )
  })

  test('uses backend type summaries and page total without loaded-item fallback', () => {
    const summaries = [
      { type: 'cleaning' as const, count: 10, connected: true },
      { type: 'review' as const, count: 584, connected: true },
      { type: 'order' as const, count: 0, connected: true },
      { type: 'message' as const, count: 2, connected: true },
      { type: 'other' as const, count: 0, connected: true },
    ]
    const resolved = resolveWorkbenchTypeCounts(summaries, 596, true)
    expect(resolved.byType.get('review')).toBe(584)
    expect(resolved.byType.get('order')).toBe(0)
    expect(resolved.allCount).toBe(596)
    expect(resolved.inconsistent).toBe(false)
    expect(resolveWorkbenchTypeCounts(summaries, 595, true).inconsistent).toBe(true)
  })
})

describe('home workbench pagination append', () => {
  test('keeps backend order across 51 and 101 items', () => {
    const first = Array.from({ length: 50 }, (_, index) => task(index + 1))
    const second = appendUniqueWorkbenchItems(first, [task(51)])
    const third = appendUniqueWorkbenchItems(
      second,
      Array.from({ length: 50 }, (_, index) => task(index + 52)),
    )
    expect(second.map((item) => item.id)).toEqual(
      Array.from({ length: 51 }, (_, index) => String(index + 1)),
    )
    expect(third).toHaveLength(101)
    expect(third.at(-1)?.id).toBe('101')
  })

  test('deduplicates by type and id without moving an existing item', () => {
    const result = appendUniqueWorkbenchItems([task(1), task(2)], [task(2, 'fresh'), task(3)])
    expect(result.map((item) => item.id)).toEqual(['1', '2', '3'])
    expect(result[1]?.title).toBe('fresh')
  })

  test('ignores malformed entries without a stable key', () => {
    const malformed = {
      type: 'review',
      statusGroup: 'awaiting_review',
      title: 'bad',
    } as HomeWorkbenchItemDTO
    expect(appendUniqueWorkbenchItems([task(1)], [malformed])).toEqual([task(1)])
  })
})

describe('home workbench type/status compatibility', () => {
  test('matches the backend allow-list for every type', () => {
    expect(allowedStatusesByType).toEqual({
      all: [
        'pending',
        'awaiting_review',
        'awaiting_reply',
        'unassigned',
        'assigned',
        'in_progress',
        'overdue',
        'completed',
      ],
      cleaning: ['pending', 'in_progress', 'overdue'],
      review: ['awaiting_review', 'completed'],
      order: ['pending'],
      message: ['awaiting_reply'],
      other: ['unassigned', 'assigned', 'completed'],
    })
  })

  test('all accepts every globally supported status', () => {
    for (const status of allowedStatusesByType.all) {
      expect(normalizeWorkbenchStatus('all', status)).toBe(status)
    }
  })

  test('type switch resets an incompatible old status before a request is built', () => {
    expect(normalizeWorkbenchStatus('message', 'completed')).toBe('all')
    expect(normalizeWorkbenchStatus('review', 'awaiting_reply')).toBe('all')
    expect(normalizeWorkbenchStatus('order', 'unassigned')).toBe('all')
    expect(normalizeWorkbenchStatus('other', 'overdue')).toBe('all')
    expect(normalizeWorkbenchStatus('cleaning', 'awaiting_review')).toBe('all')
    expect(toWorkbenchStatusParam('message', 'completed')).toBeUndefined()
    expect(toWorkbenchStatusParam('order', 'unassigned')).toBeUndefined()
  })

  test('legal zero-count statuses remain selectable', () => {
    expect(normalizeWorkbenchStatus('review', 'completed')).toBe('completed')
    expect(normalizeWorkbenchStatus('other', 'unassigned')).toBe('unassigned')
    expect(normalizeWorkbenchStatus('cleaning', 'overdue')).toBe('overdue')
  })

  test('unknown cached or URL state is normalized to all', () => {
    expect(normalizeWorkbenchStatus('all', 'expired')).toBe('overdue')
    expect(normalizeWorkbenchStatus('cleaning', 'expired')).toBe('overdue')
    expect(normalizeWorkbenchStatus('message', 'expired')).toBe('all')
    expect(normalizeWorkbenchStatus('message', 'unknown')).toBe('all')
    expect(normalizeWorkbenchStatus('order', '')).toBe('all')
  })
})
