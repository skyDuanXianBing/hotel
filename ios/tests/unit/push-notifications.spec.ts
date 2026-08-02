import { describe, expect, it } from 'vitest'
import { resolvePushTargetPath } from '@/utils/pushNotifications'

describe('pushNotifications.resolvePushTargetPath', () => {
  it('routes chat payload to message detail', () => {
    expect(resolvePushTargetPath({ type: 'chat', threadId: '42' })).toBe('/tabs/messages/42')
  })

  it('falls back to message list when threadId missing', () => {
    expect(resolvePushTargetPath({ type: 'chat' })).toBe('/tabs/messages')
  })

  it('routes order payload to reservation detail', () => {
    expect(resolvePushTargetPath({ type: 'order', reservationId: '123' })).toBe(
      '/tabs/orders/reservations/123',
    )
  })

  it('falls back to order notifications when reservationId missing', () => {
    expect(resolvePushTargetPath({ type: 'order' })).toBe('/tabs/notifications/order')
  })

  it('routes task payload to registration review detail', () => {
    expect(resolvePushTargetPath({ type: 'task', formId: '7' })).toBe('/tabs/reviews/7')
  })

  it('falls back to reviews list when formId missing', () => {
    expect(resolvePushTargetPath({ type: 'task' })).toBe('/tabs/reviews')
  })

  it('returns null for unknown payload type', () => {
    expect(resolvePushTargetPath({ type: 'promo' })).toBeNull()
    expect(resolvePushTargetPath({})).toBeNull()
  })
})
