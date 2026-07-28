import { describe, expect, it } from 'vitest'
import { canMarkManualSettlement } from '@/components/order/orderUtils'

describe('manual settlement actions', () => {
  it('allows marking orders only from the pending tab', () => {
    expect(canMarkManualSettlement('pending')).toBe(true)
  })

  it('does not expose manual settlement actions in other views', () => {
    expect(canMarkManualSettlement('all')).toBe(false)
    expect(canMarkManualSettlement('today-checkin')).toBe(false)
  })
})
