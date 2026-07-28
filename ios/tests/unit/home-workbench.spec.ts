import { describe, expect, it } from 'vitest'
import {
  appendUniqueWorkbenchItems,
  formatWorkbenchMetaItem,
  getWorkbenchStatusLabelKey,
  hasWorkbenchAction,
  isWorkbenchRequestCurrent,
  normalizeWorkbenchStatus,
  resolveWorkbenchMessageThreadId,
  resolveWorkbenchTypeCounts,
} from '@/utils/homeWorkbench'
import type { HomeWorkbenchItemDTO } from '@/api/homeWorkbench'

const createItem = (
  id: string,
  title: string,
  type: HomeWorkbenchItemDTO['type'] = 'cleaning',
): HomeWorkbenchItemDTO => ({
  id,
  type,
  title,
  statusGroup: 'pending',
})

describe('home workbench helpers', () => {
  it('normalizes status aliases and rejects statuses outside the active type', () => {
    expect(normalizeWorkbenchStatus('all', 'expired')).toBe('overdue')
    expect(normalizeWorkbenchStatus('cleaning', 'in_progress')).toBe('in_progress')
    expect(normalizeWorkbenchStatus('order', 'assigned')).toBe('all')
  })

  it('uses task-type-specific labels for pending cleaning and order tasks', () => {
    expect(getWorkbenchStatusLabelKey('cleaning', 'pending')).toBe('cleaningPending')
    expect(getWorkbenchStatusLabelKey('order', 'pending')).toBe('orderPending')
    expect(getWorkbenchStatusLabelKey('all', 'pending')).toBe('pending')
  })

  it('uses the query total for the unfiltered all-task summary', () => {
    const result = resolveWorkbenchTypeCounts(
      [
        { type: 'cleaning', count: 2, connected: true },
        { type: 'order', count: 1, connected: true },
      ],
      5,
      true,
    )

    expect(result.allCount).toBe(5)
    expect(result.byType.get('cleaning')).toBe(2)
  })

  it('updates repeated items in place and appends new task identities', () => {
    const result = appendUniqueWorkbenchItems(
      [createItem('1', 'Old title'), createItem('2', 'Second title')],
      [createItem('1', 'Fresh title'), createItem('3', 'Third title')],
    )

    expect(result.map((item) => item.id)).toEqual(['1', '2', '3'])
    expect(result[0].title).toBe('Fresh title')
  })

  it('formats structured metadata and normalizes action codes', () => {
    expect(formatWorkbenchMetaItem({ label: 'Room', value: '101' })).toBe('Room: 101')
    expect(hasWorkbenchAction([{ code: 'ASSIGN-CLEANER' }], 'assign_cleaner')).toBe(true)
  })

  it('resolves message thread ids from workbench targets before source fallbacks', () => {
    expect(
      resolveWorkbenchMessageThreadId(
        {
          path: '/messages',
          query: { suThreadId: '31' },
        },
        '99',
      ),
    ).toBe('31')
    expect(resolveWorkbenchMessageThreadId('/messages', 42)).toBe('42')
    expect(resolveWorkbenchMessageThreadId({ path: '/messages' })).toBeNull()
  })

  it('rejects stale pagination responses after the active query changes', () => {
    const request = { version: 1, type: 'cleaning' as const, status: 'pending', cursor: 'cursor-1' }

    expect(isWorkbenchRequestCurrent(request, request)).toBe(true)
    expect(
      isWorkbenchRequestCurrent(request, {
        version: 2,
        type: 'review',
        status: 'all',
        cursor: null,
      }),
    ).toBe(false)
  })
})
