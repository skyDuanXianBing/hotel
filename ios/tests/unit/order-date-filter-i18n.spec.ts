import { beforeEach, describe, expect, it } from 'vitest'
import type { ReservationDTO } from '@/api/reservation'
import {
  createDefaultOrderFilters,
  matchesReservationSearch,
} from '@/components/order/orderUtils'

const reservation: ReservationDTO = {
  id: 1,
  orderNumber: 'ORDER-1',
  guestName: 'Guest',
  channelId: 1,
  channelName: 'Direct',
  checkInDate: '2026-07-21',
  checkOutDate: '2026-07-23',
  status: 'CONFIRMED',
  createdAt: '2026-07-20T15:30:00Z',
  updatedAt: '2026-07-20T15:30:00Z',
}

describe('order date filtering', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('compares the reservation check-in date for range filtering', () => {
    const filters = {
      ...createDefaultOrderFilters(),
      startDate: '2026-07-21',
      endDate: '2026-07-21',
    }

    expect(matchesReservationSearch(reservation, filters, '')).toBe(true)
  })

  it('keeps date boundaries inclusive and rejects dates outside the range', () => {
    const filters = createDefaultOrderFilters()

    expect(
      matchesReservationSearch(
        reservation,
        { ...filters, startDate: '2026-07-22' },
        '',
      ),
    ).toBe(false)
    expect(
      matchesReservationSearch(
        reservation,
        { ...filters, endDate: '2026-07-20' },
        '',
      ),
    ).toBe(false)
  })
})
