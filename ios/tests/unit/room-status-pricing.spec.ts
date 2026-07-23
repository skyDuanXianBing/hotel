import { describe, expect, test } from 'vitest'
import type { RoomPriceManagementDTO } from '@/api/roomPrice'
import { i18n } from '@/locales'
import {
  buildRoomStatusDailyPricingMap,
  formatRoomStatusPrice,
  getRoomStatusDailyPricing,
  normalizeRoomStatusPriceSource,
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
  test('matches the default source by room type id and date without aggregating plan prices', () => {
    const pricingMap = buildRoomStatusDailyPricingMap(
      [
        buildRecord({ id: 1, pricePlanId: 100, price: 420, minStay: 2 }),
        buildRecord({ id: 2, pricePlanId: 101, price: 360, minStay: 1 }),
        buildRecord({ id: 3, pricePlanId: undefined, price: 320, closeRoom: true, minStay: 3 }),
      ],
      'default',
    )

    expect(getRoomStatusDailyPricing(pricingMap, 10, '2026-07-16', 'default')).toEqual({
      price: 320,
      minStay: 3,
    })
  })

  test('matches an explicit price plan and defaults missing min stay to one', () => {
    const pricingMap = buildRoomStatusDailyPricingMap(
      [
        buildRecord({ pricePlanId: undefined, price: 298, minStay: 2 }),
        buildRecord({ id: 2, pricePlanId: 100, price: 420 }),
        buildRecord({ id: 3, pricePlanId: 101, price: 360, minStay: 2 }),
      ],
      'plan:100',
    )

    expect(getRoomStatusDailyPricing(pricingMap, 10, '2026-07-16', 'plan:100')).toEqual({
      price: 420,
      minStay: 1,
    })
  })

  test('isolates room types with the same name by room type id', () => {
    const pricingMap = buildRoomStatusDailyPricingMap(
      [
        buildRecord({ id: 1, roomTypeId: 10, price: 288.5 }),
        buildRecord({ id: 2, roomTypeId: 20, price: 388.75 }),
      ],
      'default',
    )

    expect(getRoomStatusDailyPricing(pricingMap, 10, '2026-07-16', 'default')?.price).toBe(
      288.5,
    )
    expect(getRoomStatusDailyPricing(pricingMap, 20, '2026-07-16', 'default')?.price).toBe(
      388.75,
    )
  })

  test('formats a positive price with the temporary CNY compatibility currency', () => {
    const symbol = String.fromCharCode(0xa5)

    expect(formatRoomStatusPrice(288)).toBe(`${symbol}288.00`)
    expect(formatRoomStatusPrice(288.5, 'JPY')).toBe(`${symbol}288.50`)
    expect(formatRoomStatusPrice(288.567, 'USD')).toBe(`${symbol}288.57`)
    expect(formatRoomStatusPrice(0)).toBe('')
  })

  test('formats CNY prices with the zh-CN currency symbol regardless of app locale', () => {
    const previousLocale = i18n.global.locale.value

    try {
      i18n.global.locale.value = 'zh-TW'
      expect(formatRoomStatusPrice(25000, 'CNY')).toBe('¥25,000.00')

      i18n.global.locale.value = 'en'
      expect(formatRoomStatusPrice(25000, 'CNY')).toBe('¥25,000.00')
    } finally {
      i18n.global.locale.value = previousLocale
    }
  })

  test('falls back to the room type default price only for the default source', () => {
    expect(getRoomStatusDailyPricing({}, 10, '2026-07-16', 'default', 288)).toEqual({
      price: 288,
      minStay: 1,
    })
    expect(getRoomStatusDailyPricing({}, 10, '2026-07-16', 'plan:100', 288)).toBeUndefined()
  })

  test('rejects invalid persisted source values', () => {
    expect(normalizeRoomStatusPriceSource('plan:100')).toBe('plan:100')
    expect(normalizeRoomStatusPriceSource('plan:0')).toBe('default')
    expect(normalizeRoomStatusPriceSource('lowest')).toBe('default')
  })
})
