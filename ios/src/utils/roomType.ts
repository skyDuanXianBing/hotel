import type {
  CreateRoomTypeRequest,
  FacilityDTO,
  LocalizedContentDTO,
  RoomTypeDTO,
  RoomTypeWithRoomsDTO,
} from '@/api/roomType'
import { i18n } from '@/locales'
import {
  formatMoney,
  type MoneyDisplayContext,
} from '@/utils/formatters'

export const ROOM_TYPE_DETAIL_LOCALE = 'zh-CN'

export const ROOM_TYPE_SIZE_UNIT_OPTIONS = [
  {
    labelKey: 'runtime.roomType.sizeUnit.sqm',
    value: 'sqm',
  },
  {
    labelKey: 'runtime.roomType.sizeUnit.sqft',
    value: 'sqft',
  },
] as const

export const ROOM_TYPE_DAILY_PRICE_FIELDS = [
  { key: 'mondayPrice', labelKey: 'runtime.roomType.weekdayPrice.monday' },
  { key: 'tuesdayPrice', labelKey: 'runtime.roomType.weekdayPrice.tuesday' },
  { key: 'wednesdayPrice', labelKey: 'runtime.roomType.weekdayPrice.wednesday' },
  { key: 'thursdayPrice', labelKey: 'runtime.roomType.weekdayPrice.thursday' },
  { key: 'fridayPrice', labelKey: 'runtime.roomType.weekdayPrice.friday' },
  { key: 'saturdayPrice', labelKey: 'runtime.roomType.weekdayPrice.saturday' },
  { key: 'sundayPrice', labelKey: 'runtime.roomType.weekdayPrice.sunday' },
] as const

type DailyPriceKey = (typeof ROOM_TYPE_DAILY_PRICE_FIELDS)[number]['key']

export interface RoomTypeMoneyOptions {
  currency?: string
  context?: MoneyDisplayContext
}

const roomTypeText = (key: string, params?: Record<string, unknown>) => {
  const path = `runtime.roomType.${key}`
  return params ? i18n.global.t(path, params) : i18n.global.t(path)
}

export function extractRoomEntries(roomType: Pick<RoomTypeWithRoomsDTO, 'rooms'>) {
  const rooms: Array<{ roomNumber: string; smartlockPasscode?: string }> = []

  for (const room of roomType.rooms || []) {
    const roomNumber = room.roomNumber?.trim() || ''
    if (!roomNumber) {
      continue
    }

    const smartlockPasscode = room.smartlockPasscode?.trim()
    rooms.push({
      roomNumber,
      smartlockPasscode: smartlockPasscode || undefined,
    })
  }

  return rooms
}

export function extractRoomNumbers(roomType: Pick<RoomTypeWithRoomsDTO, 'rooms'>) {
  const roomNumbers: string[] = []

  for (const room of extractRoomEntries(roomType)) {
    roomNumbers.push(room.roomNumber)
  }

  return roomNumbers
}

function resolveLocaleContent(roomType: Pick<RoomTypeDTO, 'localizedContent'>) {
  const localizedContent = roomType.localizedContent || {}
  const preferredContent = localizedContent[ROOM_TYPE_DETAIL_LOCALE]
  if (preferredContent) {
    return preferredContent
  }

  const localizedEntries = Object.values(localizedContent)
  if (localizedEntries.length > 0) {
    return localizedEntries[0]
  }

  return undefined
}

export function resolveRoomTypeShortName(roomType: Pick<RoomTypeDTO, 'description' | 'code'>) {
  const description = roomType.description?.trim()
  if (description) {
    return description
  }

  return roomType.code
}

export function resolveRoomTypeLongDescription(
  roomType: Pick<RoomTypeDTO, 'localizedContent' | 'description'>,
) {
  const localeContent = resolveLocaleContent(roomType)
  const localizedDescription = localeContent?.description?.trim()
  if (localizedDescription) {
    return localizedDescription
  }

  return ''
}

export function buildRoomTypeCode(shortName: string, currentCode?: string) {
  const trimmedCurrentCode = currentCode?.trim()
  if (trimmedCurrentCode) {
    return trimmedCurrentCode
  }

  const normalizedShortName = shortName.trim().replace(/\s+/g, '')
  if (!normalizedShortName) {
    return ''
  }

  return normalizedShortName.slice(0, 12).toUpperCase()
}

export function formatPriceValue(value?: number, options: RoomTypeMoneyOptions = {}) {
  if (value === undefined || value === null) {
    return roomTypeText('priceUnset')
  }

  const amount = Number(value)
  if (!Number.isFinite(amount)) {
    return roomTypeText('priceUnset')
  }

  return formatMoney(
    amount,
    options.currency || 'CNY',
    { minimumFractionDigits: 0, maximumFractionDigits: 2 },
    options.context,
  )
}

export function getBasePriceText(roomType: RoomTypeDTO, options: RoomTypeMoneyOptions = {}) {
  if (roomType.defaultPrice !== undefined && roomType.defaultPrice !== null) {
    return roomTypeText('defaultPrice', {
      price: formatPriceValue(roomType.defaultPrice, options),
    })
  }

  if (roomType.weekdayPrice !== undefined || roomType.weekendPrice !== undefined) {
    return roomTypeText('weekdayWeekendPrice', {
      weekday: formatPriceValue(roomType.weekdayPrice, options),
      weekend: formatPriceValue(roomType.weekendPrice, options),
    })
  }

  const mondayPrice = roomType.mondayPrice
  if (mondayPrice !== undefined && mondayPrice !== null) {
    return roomTypeText('weeklyStartPrice', { price: formatPriceValue(mondayPrice, options) })
  }

  return roomTypeText('defaultPriceUnset')
}

export function buildRoomTypePriceSummary(
  roomType: RoomTypeDTO,
  options: RoomTypeMoneyOptions = {},
) {
  const priceHighlights: string[] = []

  if (roomType.defaultPrice !== undefined && roomType.defaultPrice !== null) {
    priceHighlights.push(
      roomTypeText('defaultPriceShort', {
        price: formatPriceValue(roomType.defaultPrice, options),
      }),
    )
  }

  for (const field of ROOM_TYPE_DAILY_PRICE_FIELDS) {
    const priceValue = roomType[field.key]
    if (priceValue === undefined || priceValue === null) {
      continue
    }
    priceHighlights.push(
      `${i18n.global.t(field.labelKey)} ${formatPriceValue(priceValue, options)}`,
    )
  }

  if (priceHighlights.length === 0) {
    return roomTypeText('noDefaultOrWeeklyPrice')
  }

  return priceHighlights.join(' · ')
}

export function normalizeHttpUrl(rawValue?: string) {
  const trimmedValue = (rawValue || '').trim()
  if (!trimmedValue) {
    return undefined
  }

  let normalizedValue = trimmedValue
  if (!/^https?:\/\//i.test(normalizedValue)) {
    if (/^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(normalizedValue)) {
      return null
    }
    normalizedValue = `https://${normalizedValue}`
  }

  try {
    const url = new URL(normalizedValue)
    if (url.protocol !== 'http:' && url.protocol !== 'https:') {
      return null
    }
    return url.toString()
  } catch {
    return null
  }
}

function splitTextList(rawValue: string) {
  return rawValue
    .split(/\n|,|，/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
}

export function normalizeUrlTextList(rawValue: string) {
  const rawItems = splitTextList(rawValue)
  const normalizedUrls: string[] = []

  for (const item of rawItems) {
    const normalizedUrl = normalizeHttpUrl(item)
    if (!normalizedUrl) {
      return {
        invalidValue: item,
        urls: [] as string[],
      }
    }
    normalizedUrls.push(normalizedUrl)
  }

  return {
    invalidValue: '',
    urls: normalizedUrls,
  }
}

export function formatUrlTextList(urls?: string[]) {
  if (!urls || urls.length === 0) {
    return ''
  }
  return urls.join('\n')
}

function formatFacilityLabel(facility: FacilityDTO) {
  const group = facility.group?.trim()
  const name = facility.name?.trim()
  if (!name) {
    return ''
  }

  if (!group) {
    return name
  }

  return `${group}: ${name}`
}

export function formatFacilitiesText(facilities?: FacilityDTO[]) {
  if (!facilities || facilities.length === 0) {
    return ''
  }

  const labels: string[] = []
  for (const facility of facilities) {
    const label = formatFacilityLabel(facility)
    if (label) {
      labels.push(label)
    }
  }

  return labels.join('\n')
}

export function parseFacilitiesText(rawValue: string) {
  const items = splitTextList(rawValue)
  const facilities: FacilityDTO[] = []

  for (const item of items) {
    const colonIndex = item.indexOf(':')
    if (colonIndex > 0) {
      const group = item.slice(0, colonIndex).trim()
      const name = item.slice(colonIndex + 1).trim()
      if (name) {
        facilities.push({
          group: group || undefined,
          name,
        })
      }
      continue
    }

    facilities.push({
      name: item,
    })
  }

  return facilities
}

export function buildLocalizedContent(
  existingContent: Record<string, LocalizedContentDTO> | undefined,
  name: string,
  description: string,
) {
  const localizedContent: Record<string, LocalizedContentDTO> = {
    ...(existingContent || {}),
  }

  localizedContent[ROOM_TYPE_DETAIL_LOCALE] = {
    ...(localizedContent[ROOM_TYPE_DETAIL_LOCALE] || {}),
    name,
    description: description || undefined,
  }

  return localizedContent
}

export function buildExistingRoomTypePayload(roomType: RoomTypeWithRoomsDTO): CreateRoomTypeRequest {
  const rooms = extractRoomEntries(roomType)

  return {
    name: roomType.name,
    code: roomType.code,
    description: roomType.description,
    totalRooms: roomType.totalRooms,
    maxGuests: roomType.maxGuests || 1,
    maxChildOccupancy: roomType.maxChildOccupancy,
    roomTypeAddress: roomType.roomTypeAddress,
    nearbyStation: roomType.nearbyStation,
    checkInGuideLink: roomType.checkInGuideLink,
    suRoomType: roomType.suRoomType,
    sizeMeasurement: roomType.sizeMeasurement,
    sizeMeasurementUnit: roomType.sizeMeasurementUnit,
    defaultPrice: roomType.defaultPrice,
    weekdayPrice: roomType.weekdayPrice,
    weekendPrice: roomType.weekendPrice,
    mondayPrice: roomType.mondayPrice,
    tuesdayPrice: roomType.tuesdayPrice,
    wednesdayPrice: roomType.wednesdayPrice,
    thursdayPrice: roomType.thursdayPrice,
    fridayPrice: roomType.fridayPrice,
    saturdayPrice: roomType.saturdayPrice,
    sundayPrice: roomType.sundayPrice,
    roomNumbers: rooms.map((room) => room.roomNumber),
    rooms,
    facilities: roomType.facilities,
    desktopPhotoUrls: roomType.desktopPhotoUrls,
    mobilePhotoUrls: roomType.mobilePhotoUrls,
    localizedContent: roomType.localizedContent,
  }
}

export function resolveDailyPrice(roomType: RoomTypeDTO, priceKey: DailyPriceKey) {
  return roomType[priceKey]
}

export function mergeRoomTypePhotoUrls(roomType: Pick<RoomTypeDTO, 'desktopPhotoUrls' | 'mobilePhotoUrls'>) {
  const photoUrlSet = new Set<string>()

  for (const url of roomType.desktopPhotoUrls || []) {
    if (url) {
      photoUrlSet.add(url)
    }
  }

  for (const url of roomType.mobilePhotoUrls || []) {
    if (url) {
      photoUrlSet.add(url)
    }
  }

  return Array.from(photoUrlSet)
}

export function buildRoomTypeLocationSummary(
  roomType: Pick<RoomTypeDTO, 'roomTypeAddress' | 'nearbyStation'>,
) {
  const labels: string[] = []

  const roomTypeAddress = roomType.roomTypeAddress?.trim()
  if (roomTypeAddress) {
    labels.push(roomTypeText('address', { value: roomTypeAddress }))
  }

  const nearbyStation = roomType.nearbyStation?.trim()
  if (nearbyStation) {
    labels.push(roomTypeText('nearbyStation', { value: nearbyStation }))
  }

  return labels.join(' · ')
}
