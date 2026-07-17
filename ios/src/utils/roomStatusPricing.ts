import type { RoomPriceManagementDTO } from '@/api/roomPrice'

export interface RoomStatusDailyPricing {
  price: number
  minStay: number
}

export type RoomStatusDailyPricingMap = Record<string, RoomStatusDailyPricing>
export type RoomStatusPriceSource = 'default' | `plan:${number}`

const buildPricingKey = (roomTypeId: number, pricePlanId: number, date: string) =>
  `${roomTypeId}\u0000${pricePlanId}\u0000${date}`

const normalizePositiveNumber = (value: unknown) => {
  const normalized = Number(value)
  return Number.isFinite(normalized) && normalized > 0 ? normalized : undefined
}

const normalizeMinStay = (value: unknown) => {
  const normalized = Number(value)
  return Number.isInteger(normalized) && normalized > 0 ? normalized : 1
}

export const getRoomStatusPricePlanId = (source: RoomStatusPriceSource) => {
  if (source === 'default') {
    return 0
  }

  const pricePlanId = Number(source.slice('plan:'.length))
  return Number.isInteger(pricePlanId) && pricePlanId > 0 ? pricePlanId : 0
}

export const normalizeRoomStatusPriceSource = (value: unknown): RoomStatusPriceSource => {
  if (value === 'default') {
    return value
  }

  if (typeof value === 'string' && /^plan:[1-9]\d*$/.test(value)) {
    return value as RoomStatusPriceSource
  }

  return 'default'
}

export const formatRoomStatusPrice = (price: unknown) => {
  const normalized = normalizePositiveNumber(price)
  return normalized === undefined ? '' : `¥${normalized.toFixed(2)}`
}

export const buildRoomStatusDailyPricingMap = (
  records: RoomPriceManagementDTO[],
  source: RoomStatusPriceSource,
): RoomStatusDailyPricingMap => {
  const result: RoomStatusDailyPricingMap = {}
  const selectedPricePlanId = getRoomStatusPricePlanId(source)

  for (const record of records) {
    if (!record.roomTypeId || !record.priceDate) {
      continue
    }

    const recordPricePlanId = Number(record.pricePlanId) || 0
    if (recordPricePlanId !== selectedPricePlanId) {
      continue
    }

    const price = normalizePositiveNumber(record.price)
    if (price === undefined) {
      continue
    }

    const key = buildPricingKey(record.roomTypeId, selectedPricePlanId, record.priceDate)
    result[key] = {
      price,
      minStay: normalizeMinStay(record.minStay),
    }
  }

  return result
}

export const getRoomStatusDailyPricing = (
  pricingMap: RoomStatusDailyPricingMap,
  roomTypeId: number,
  date: string,
  source: RoomStatusPriceSource,
  defaultPrice?: number,
) => {
  const pricePlanId = getRoomStatusPricePlanId(source)
  const pricing = pricingMap[buildPricingKey(roomTypeId, pricePlanId, date)]
  if (pricing || source !== 'default') {
    return pricing
  }

  const fallbackPrice = normalizePositiveNumber(defaultPrice)
  if (fallbackPrice === undefined) {
    return undefined
  }

  return {
    price: fallbackPrice,
    minStay: 1,
  }
}
