import type { RoomPriceManagementDTO } from '@/api/roomPrice'

export interface RoomStatusDailyPricing {
  price: number
  minStay?: number
}

export type RoomStatusDailyPricingMap = Record<string, RoomStatusDailyPricing>

const buildPricingKey = (roomTypeName: string, date: string) => `${roomTypeName}\u0000${date}`

const normalizePositiveNumber = (value: unknown) => {
  const normalized = Number(value)
  return Number.isFinite(normalized) && normalized > 0 ? normalized : undefined
}

const normalizeMinStay = (value: unknown) => {
  const normalized = Number(value)
  return Number.isInteger(normalized) && normalized > 0 ? normalized : undefined
}

export const buildRoomStatusDailyPricingMap = (
  records: RoomPriceManagementDTO[],
): RoomStatusDailyPricingMap => {
  const result: RoomStatusDailyPricingMap = {}

  for (const record of records) {
    if (!record.roomTypeName || !record.priceDate || record.closeRoom === true) {
      continue
    }

    const price = normalizePositiveNumber(record.price)
    if (price === undefined) {
      continue
    }

    const key = buildPricingKey(record.roomTypeName, record.priceDate)
    const current = result[key]
    const minStay = normalizeMinStay(record.minStay)

    if (
      !current ||
      price < current.price ||
      (price === current.price && current.minStay === undefined && minStay !== undefined)
    ) {
      result[key] = { price, minStay }
    }
  }

  return result
}

export const getRoomStatusDailyPricing = (
  pricingMap: RoomStatusDailyPricingMap,
  roomTypeName: string,
  date: string,
) => {
  return pricingMap[buildPricingKey(roomTypeName, date)]
}
