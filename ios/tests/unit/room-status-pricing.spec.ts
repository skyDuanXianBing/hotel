import { describe, expect, test } from 'vitest'
import type { RoomPriceManagementDTO } from '@/api/roomPrice'
import {
  buildRoomStatusDailyPricingMap,
  getRoomStatusDailyPricing,
} from '@/utils/roomStatusPricing'

const buildRecord = (
  overrides: Partial<RoomPriceManagementDTO> = {},
): RoomPriceManagementDTO => ({
  id: 1,
  roomTypeId: 10,
  roomTypeName: '标准大床房',
  roomTypeCode: 'STD',
  priceDate: '2026-07-16',
  price: 320,
  isWeekend: false,
  isHoliday: false,
  ...overrides,
})

describe('room status daily pricing', () => {
  test('uses the lowest open price for a room type and date', () => {
    const pricingMap = buildRoomStatusDailyPricingMap([
      buildRecord({ id: 1, pricePlanId: 100, price: 420, minStay: 2 }),
      buildRecord({ id: 2, pricePlanId: 101, price: 360, minStay: 1 }),
      buildRecord({ id: 3, pricePlanId: 102, price: 320, closeRoom: true, minStay: 3 }),
    ])

    expect(getRoomStatusDailyPricing(pricingMap, '标准大床房', '2026-07-16')).toEqual({
      price: 360,
      minStay: 1,
    })
  })

  test('keeps an unbound room type price and ignores invalid prices', () => {
    const pricingMap = buildRoomStatusDailyPricingMap([
      buildRecord({ pricePlanId: undefined, price: 298, minStay: 2 }),
      buildRecord({ id: 2, price: 0 }),
      buildRecord({ id: 3, price: Number.NaN }),
    ])

    expect(getRoomStatusDailyPricing(pricingMap, '标准大床房', '2026-07-16')).toEqual({
      price: 298,
      minStay: 2,
    })
  })
})
