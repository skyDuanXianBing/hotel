import type {
  JsonObject,
  PmsChannelSummary,
  PmsOtaIntegrationSummary,
  PmsPricePlanSummary,
  PmsReadinessData,
  PmsRoomSummary,
  PmsRoomTypeSummary,
} from '../pms-client'
import type {
  BuiltReservation,
  E2EChannelCode,
  E2ELifecycleStepName,
  E2EOtaCode,
  E2ERunGeneratedIds,
  E2ERunScenario,
  NormalizedCreateE2ERunRequest,
} from './types'

const { v4: uuidv4 } = require('uuid') as { v4: () => string }

const BOOKING_OTA_CODE = 19
const AIRBNB_OTA_CODE = 244
const EXPEDIA_OTA_CODE = 9
const TRIP_COM_OTA_CODE = 339
const AGODA_OTA_CODE = 189

interface E2EChannelDefinition {
  otaCode: E2EOtaCode
  pos: string
  source: string
  guest: {
    firstName: string
    lastNamePrefix: string
    emailPrefix: string
    telephone: string
    guestLang: string
  }
  hasMessagingThread: boolean
}

const E2E_CHANNEL_DEFINITIONS: Record<E2EChannelCode, E2EChannelDefinition> = {
  BOOKING: {
    otaCode: BOOKING_OTA_CODE,
    pos: 'Booking.com',
    source: 'booking.com',
    guest: {
      firstName: 'Taro',
      lastNamePrefix: 'Booking',
      emailPrefix: 'booking',
      telephone: '+81-90-0000-0019',
      guestLang: 'ja',
    },
    hasMessagingThread: false,
  },
  AIRBNB: {
    otaCode: AIRBNB_OTA_CODE,
    pos: 'Airbnb',
    source: 'airbnb',
    guest: {
      firstName: 'Alex',
      lastNamePrefix: 'Airbnb',
      emailPrefix: 'airbnb',
      telephone: '+81-90-0000-0244',
      guestLang: 'en',
    },
    hasMessagingThread: true,
  },
  EXPEDIA: {
    otaCode: EXPEDIA_OTA_CODE,
    pos: 'Expedia',
    source: 'expedia',
    guest: {
      firstName: 'Ethan',
      lastNamePrefix: 'Expedia',
      emailPrefix: 'expedia',
      telephone: '+81-90-0000-0009',
      guestLang: 'en',
    },
    hasMessagingThread: true,
  },
  TRIP_COM: {
    otaCode: TRIP_COM_OTA_CODE,
    pos: 'Trip.com',
    source: 'trip.com',
    guest: {
      firstName: 'Wei',
      lastNamePrefix: 'TripCom',
      emailPrefix: 'tripcom',
      telephone: '+81-90-0000-0339',
      guestLang: 'zh',
    },
    hasMessagingThread: false,
  },
  AGODA: {
    otaCode: AGODA_OTA_CODE,
    pos: 'Agoda',
    source: 'agoda',
    guest: {
      firstName: 'Anya',
      lastNamePrefix: 'Agoda',
      emailPrefix: 'agoda',
      telephone: '+81-90-0000-0189',
      guestLang: 'en',
    },
    hasMessagingThread: false,
  },
}

// 部分场景强制绑定渠道（与 AIRBNB_NEW/MULTI_ROOM 现有模式一致）。
const FORCED_SCENARIO_CHANNELS: Partial<Record<E2ERunScenario, E2EChannelCode>> = {
  MULTI_ROOM: 'BOOKING',
  AIRBNB_NEW: 'AIRBNB',
  EXPEDIA_NEW: 'EXPEDIA',
  TRIP_COM_NEW: 'TRIP_COM',
  AGODA_NEW: 'AGODA',
}
const DEFAULT_MIN_STAY_START_DAYS = 7
const DYNAMIC_STAY_WINDOW_DAYS = 180
const MIN_EXPLICIT_STAY_START_DAYS = 1
const MAX_EXPLICIT_STAY_START_DAYS = 365
const DEFAULT_STAY_NIGHTS = 2
const DEFAULT_ADULTS = 2
const DEFAULT_CHILDREN = 0
const DEFAULT_CURRENCY = 'JPY'
const DEFAULT_ROOM_PRICE = 12000
const DEFAULT_TAX_RATE = 0.1
const MAX_SU_ROOM_ID_LENGTH = 20
const DYNAMIC_STAY_TIME_BUCKET_MS = 1000
const DEFAULT_SIMULATOR_STORE_TIME_ZONE = 'Asia/Tokyo'
const MS_PER_DAY = 24 * 60 * 60 * 1000
const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/
const OFFSETLESS_DATE_TIME_PATTERN = /^\d{4}-\d{2}-\d{2}(?:T|\s)\d{2}:\d{2}(?::\d{2}(?:\.\d{1,3})?)?$/

interface SelectedInventory {
  room: PmsRoomSummary
  roomType: PmsRoomTypeSummary
  pricePlan: PmsPricePlanSummary | null
  channel: PmsChannelSummary | null
  otaIntegration: PmsOtaIntegrationSummary | null
}

interface SelectedMultiRoomInventory {
  first: SelectedInventory
  secondRoom: PmsRoomSummary
}

interface BusinessDate {
  year: number
  month: number
  day: number
}

interface StoreDateTimeParts extends BusinessDate {
  hour: number
  minute: number
  second: number
}

interface SimulatorClock {
  now: Date
  timeZone: string
  today: BusinessDate
  fixedNow: string | null
}

export interface BuiltLifecycleReservation {
  step: E2ELifecycleStepName
  built: BuiltReservation
}

let dynamicStaySequence = 0

function normalizeText(value: unknown): string | null {
  if (value === null || value === undefined) {
    return null
  }
  const text = String(value).trim()
  return text.length > 0 ? text : null
}

function getSimulatorStoreTimeZone(): string {
  const timeZone = normalizeText(process.env.SIMULATOR_STORE_TIME_ZONE) || DEFAULT_SIMULATOR_STORE_TIME_ZONE
  try {
    new Intl.DateTimeFormat('en-US', { timeZone }).format(new Date(0))
  } catch {
    throw new Error(`Invalid SIMULATOR_STORE_TIME_ZONE: ${timeZone}`)
  }
  return timeZone
}

function parseSimulatorFixedNow(): { now: Date; fixedNow: string | null } {
  const fixedNow = normalizeText(process.env.SIMULATOR_FIXED_NOW)
  if (!fixedNow) {
    return { now: new Date(), fixedNow: null }
  }

  let normalized = fixedNow
  if (DATE_ONLY_PATTERN.test(fixedNow)) {
    normalized = `${fixedNow}T00:00:00.000Z`
  } else if (OFFSETLESS_DATE_TIME_PATTERN.test(fixedNow)) {
    normalized = `${fixedNow.replace(' ', 'T')}Z`
  }

  const now = new Date(normalized)
  if (Number.isNaN(now.getTime())) {
    throw new Error(`Invalid SIMULATOR_FIXED_NOW: ${fixedNow}`)
  }
  return { now, fixedNow }
}

function getStoreDateTimeParts(instant: Date, timeZone: string): StoreDateTimeParts {
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  })
  const parts = formatter.formatToParts(instant)
  const values: Record<string, number> = {}
  for (const part of parts) {
    if (part.type !== 'literal') {
      values[part.type] = Number(part.value)
    }
  }
  return {
    year: values.year,
    month: values.month,
    day: values.day,
    hour: values.hour,
    minute: values.minute,
    second: values.second,
  }
}

function getSimulatorClock(): SimulatorClock {
  const timeZone = getSimulatorStoreTimeZone()
  const parsed = parseSimulatorFixedNow()
  const parts = getStoreDateTimeParts(parsed.now, timeZone)
  return {
    now: parsed.now,
    timeZone,
    today: {
      year: parts.year,
      month: parts.month,
      day: parts.day,
    },
    fixedNow: parsed.fixedNow,
  }
}

function normalizeNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return null
  }
  return numeric
}

function normalizeStayStartDays(value: unknown): number | null {
  const days = normalizeNumber(value)
  if (days === null) {
    return null
  }
  if (!Number.isInteger(days)) {
    throw new Error('stayStartDays must be an integer')
  }
  if (days < MIN_EXPLICIT_STAY_START_DAYS || days > MAX_EXPLICIT_STAY_START_DAYS) {
    throw new Error(
      `stayStartDays must be between ${MIN_EXPLICIT_STAY_START_DAYS} and ${MAX_EXPLICIT_STAY_START_DAYS}`,
    )
  }
  return days
}

export function normalizeRunRequest(raw: unknown): NormalizedCreateE2ERunRequest {
  const body = raw && typeof raw === 'object' ? (raw as Record<string, unknown>) : {}
  const rawMode = normalizeText(body.mode)?.toUpperCase()
  let mode: 'PUSH' | 'PULL' = 'PUSH'
  if (rawMode === 'PULL') {
    mode = 'PULL'
  } else if (rawMode === 'PUSH' || rawMode === undefined) {
    mode = 'PUSH'
  } else {
    throw new Error('mode must be PUSH or PULL')
  }

  const rawScenario = normalizeText(body.scenario)?.toUpperCase()
  let scenario: E2ERunScenario = 'NEW'
  if (rawScenario === 'MULTI_ROOM') {
    scenario = 'MULTI_ROOM'
  } else if (rawScenario === 'AIRBNB_NEW') {
    scenario = 'AIRBNB_NEW'
  } else if (rawScenario === 'EXPEDIA_NEW') {
    scenario = 'EXPEDIA_NEW'
  } else if (rawScenario === 'TRIP_COM_NEW') {
    scenario = 'TRIP_COM_NEW'
  } else if (rawScenario === 'AGODA_NEW') {
    scenario = 'AGODA_NEW'
  } else if (rawScenario === 'NEW' || rawScenario === undefined) {
    scenario = 'NEW'
  } else {
    throw new Error('scenario must be NEW, MULTI_ROOM, AIRBNB_NEW, EXPEDIA_NEW, TRIP_COM_NEW, or AGODA_NEW')
  }

  const rawChannel = normalizeText(body.channel)?.toUpperCase()
  const rawOtaCode = normalizeNumber(body.otaCode)

  const forcedChannel = FORCED_SCENARIO_CHANNELS[scenario]
  let channel: E2EChannelCode
  if (forcedChannel) {
    const forcedOtaCode = E2E_CHANNEL_DEFINITIONS[forcedChannel].otaCode
    if (rawChannel && rawChannel !== forcedChannel) {
      throw new Error(`scenario ${scenario} requires channel ${forcedChannel}`)
    }
    if (rawOtaCode !== null && rawOtaCode !== forcedOtaCode) {
      throw new Error(`scenario ${scenario} requires otaCode ${forcedOtaCode}`)
    }
    channel = forcedChannel
  } else {
    channel = resolveChannelInput(rawChannel, rawOtaCode)
  }

  const otaCode = E2E_CHANNEL_DEFINITIONS[channel].otaCode
  if (rawOtaCode !== null && rawOtaCode !== otaCode) {
    throw new Error('otaCode must match channel: BOOKING=19, AIRBNB=244, EXPEDIA=9, TRIP_COM=339, AGODA=189')
  }

  return {
    mode,
    scenario,
    otaCode,
    channel,
    roomTypeId: normalizeNumber(body.roomTypeId),
    roomId: normalizeNumber(body.roomId),
    stayStartDays: normalizeStayStartDays(body.stayStartDays),
  }
}

function resolveChannelInput(rawChannel: string | undefined, rawOtaCode: number | null): E2EChannelCode {
  const channelCodes = Object.keys(E2E_CHANNEL_DEFINITIONS) as E2EChannelCode[]
  const byChannel = rawChannel ? channelCodes.find((code) => code === rawChannel) : undefined
  if (rawChannel && !byChannel) {
    throw new Error('channel must be BOOKING, AIRBNB, EXPEDIA, TRIP_COM, or AGODA')
  }

  const byOtaCode =
    rawOtaCode !== null
      ? channelCodes.find((code) => E2E_CHANNEL_DEFINITIONS[code].otaCode === rawOtaCode)
      : undefined
  if (rawOtaCode !== null && !byOtaCode) {
    throw new Error('otaCode must be one of 19, 244, 9, 339, 189')
  }

  if (byChannel && byOtaCode && byChannel !== byOtaCode) {
    throw new Error('otaCode must match channel: BOOKING=19, AIRBNB=244, EXPEDIA=9, TRIP_COM=339, AGODA=189')
  }

  return byChannel || byOtaCode || 'BOOKING'
}

function requireReadinessData(context: PmsReadinessData | null): PmsReadinessData {
  if (!context) {
    throw new Error('PMS context is empty')
  }
  if (!normalizeText(context.suHotelId)) {
    throw new Error('PMS context missing suHotelId')
  }
  if (!Array.isArray(context.roomTypes) || context.roomTypes.length === 0) {
    throw new Error('PMS context has no roomTypes')
  }
  if (!Array.isArray(context.rooms) || context.rooms.length === 0) {
    throw new Error('PMS context has no rooms')
  }
  return context
}

function isEnabledChannel(channel: PmsChannelSummary, channelCode: E2EChannelCode): boolean {
  return (
    normalizeText(channel.code)?.toUpperCase() === channelCode &&
    channel.enabled === true &&
    channel.active === true
  )
}

function isEnabledIntegration(
  integration: PmsOtaIntegrationSummary,
  channelCode: E2EChannelCode,
): boolean {
  return normalizeText(integration.code)?.toUpperCase() === channelCode && integration.enabled === true
}

function findRoomType(context: PmsReadinessData, roomTypeId: number): PmsRoomTypeSummary | null {
  return context.roomTypes.find((item) => item.id === roomTypeId) || null
}

function findPricePlan(
  context: PmsReadinessData,
  channel: PmsChannelSummary | null,
  integration: PmsOtaIntegrationSummary | null,
): PmsPricePlanSummary | null {
  const preferredIds = [channel?.defaultPricePlanId, integration?.defaultPricePlanId]
  for (const preferredId of preferredIds) {
    if (preferredId === null || preferredId === undefined) {
      continue
    }
    const match = context.pricePlans.find((item) => item.id === preferredId)
    if (match) {
      return match
    }
  }
  return context.pricePlans.length > 0 ? context.pricePlans[0] : null
}

function selectInventory(
  context: PmsReadinessData,
  request: NormalizedCreateE2ERunRequest,
): SelectedInventory {
  const channel = context.channels.find((item) => isEnabledChannel(item, request.channel)) || null
  const otaIntegration =
    context.otaIntegrations.find((item) => isEnabledIntegration(item, request.channel)) || null

  let selectedRoom: PmsRoomSummary | null = null
  if (request.roomId !== null) {
    selectedRoom = context.rooms.find((item) => item.id === request.roomId) || null
    if (!selectedRoom) {
      throw new Error(`roomId not found in PMS context: ${request.roomId}`)
    }
  }

  let selectedRoomType: PmsRoomTypeSummary | null = null
  if (request.roomTypeId !== null) {
    selectedRoomType = findRoomType(context, request.roomTypeId)
    if (!selectedRoomType) {
      throw new Error(`roomTypeId not found in PMS context: ${request.roomTypeId}`)
    }
  }

  if (selectedRoom && selectedRoomType && selectedRoom.roomTypeId !== selectedRoomType.id) {
    throw new Error('roomId does not belong to roomTypeId')
  }

  if (!selectedRoom && selectedRoomType) {
    selectedRoom = context.rooms.find((item) => item.roomTypeId === selectedRoomType?.id) || null
    if (!selectedRoom) {
      throw new Error(`no room found for roomTypeId: ${selectedRoomType.id}`)
    }
  }

  if (!selectedRoom) {
    selectedRoom =
      context.rooms.find((item) => item.roomTypeId !== null && normalizeText(item.roomNumber) !== null) ||
      context.rooms[0]
  }

  if (!selectedRoomType) {
    if (selectedRoom.roomTypeId !== null) {
      selectedRoomType = findRoomType(context, selectedRoom.roomTypeId)
    }
    if (!selectedRoomType) {
      selectedRoomType = context.roomTypes[0]
    }
  }

  const pricePlan = findPricePlan(context, channel, otaIntegration)
  return {
    room: selectedRoom,
    roomType: selectedRoomType,
    pricePlan,
    channel,
    otaIntegration,
  }
}

function findUsableRoomsByRoomType(context: PmsReadinessData, roomTypeId: number): PmsRoomSummary[] {
  return context.rooms.filter((room) => room.roomTypeId === roomTypeId && normalizeText(room.roomNumber) !== null)
}

function buildSelectedInventoryForRoom(
  context: PmsReadinessData,
  request: NormalizedCreateE2ERunRequest,
  room: PmsRoomSummary,
  roomType: PmsRoomTypeSummary,
): SelectedInventory {
  const channel = context.channels.find((item) => isEnabledChannel(item, request.channel)) || null
  const otaIntegration =
    context.otaIntegrations.find((item) => isEnabledIntegration(item, request.channel)) || null
  return {
    room,
    roomType,
    pricePlan: findPricePlan(context, channel, otaIntegration),
    channel,
    otaIntegration,
  }
}

function selectMultiRoomInventory(
  context: PmsReadinessData,
  request: NormalizedCreateE2ERunRequest,
  firstSelection: SelectedInventory,
): SelectedMultiRoomInventory {
  let first = firstSelection
  let candidates = findUsableRoomsByRoomType(context, firstSelection.roomType.id)

  if (candidates.length < 2 && request.roomId === null && request.roomTypeId === null) {
    for (const roomType of context.roomTypes) {
      const rooms = findUsableRoomsByRoomType(context, roomType.id)
      if (rooms.length >= 2) {
        first = buildSelectedInventoryForRoom(context, request, rooms[0], roomType)
        candidates = rooms
        break
      }
    }
  }

  const firstRoomId = first.room.id
  const secondRoom = candidates.find((room) => room.id !== firstRoomId) || null
  if (!secondRoom) {
    throw new Error('MULTI_ROOM scenario requires at least two rooms with room numbers in the same roomType')
  }

  return {
    first,
    secondRoom,
  }
}

function pad2(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

function formatBusinessDate(date: BusinessDate): string {
  return `${date.year}-${pad2(date.month)}-${pad2(date.day)}`
}

function addBusinessDays(date: BusinessDate, days: number): BusinessDate {
  const utcTime = Date.UTC(date.year, date.month - 1, date.day) + days * MS_PER_DAY
  const next = new Date(utcTime)
  return {
    year: next.getUTCFullYear(),
    month: next.getUTCMonth() + 1,
    day: next.getUTCDate(),
  }
}

function formatStoreDateTime(instant: Date, timeZone: string): string {
  const parts = getStoreDateTimeParts(instant, timeZone)
  return `${formatBusinessDate(parts)} ${pad2(parts.hour)}:${pad2(parts.minute)}:${pad2(parts.second)}`
}

function hashText(value: string): number {
  let hash = 0
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) >>> 0
  }
  return hash
}

function resolveStayStartDays(
  runId: string,
  request: NormalizedCreateE2ERunRequest,
  now: Date,
): number {
  if (request.stayStartDays !== null) {
    return request.stayStartDays
  }

  const timeBucket = Math.floor(now.getTime() / DYNAMIC_STAY_TIME_BUCKET_MS)
  const sequence = dynamicStaySequence
  dynamicStaySequence = (dynamicStaySequence + 1) % DYNAMIC_STAY_WINDOW_DAYS

  const offset = hashText(`${runId}:${timeBucket}`) + sequence
  return DEFAULT_MIN_STAY_START_DAYS + (offset % DYNAMIC_STAY_WINDOW_DAYS)
}

function toMoney(value: number): string {
  return String(Math.round(value))
}

function buildBookingId(channel: E2EChannelCode, now: Date): string {
  const suffix = Math.floor(100000 + Math.random() * 900000)
  if (channel === 'AIRBNB') {
    return uuidv4().replace(/-/g, '').slice(0, 10).toUpperCase()
  }
  if (channel === 'TRIP_COM') {
    return `TC-${uuidv4().replace(/-/g, '').slice(0, 10).toUpperCase()}`
  }
  if (channel === 'AGODA') {
    return `AG-${uuidv4().replace(/-/g, '').slice(0, 10).toUpperCase()}`
  }
  if (channel === 'EXPEDIA') {
    // Expedia 订单号保持纯数字风格（前缀 70 与 Booking 的 90 区分）
    return `70${suffix}${now.getTime().toString().slice(-4)}`
  }
  return `90${suffix}${now.getTime().toString().slice(-4)}`
}

function buildRoomReservationId(now: Date): string {
  const suffix = Math.floor(100000 + Math.random() * 900000)
  return `${now.getTime()}${suffix}`
}

function buildNotifId(runId: string): string {
  return `E2E-${runId}-${uuidv4()}`
}

function buildRoomPayloadId(room: PmsRoomSummary, roomType: PmsRoomTypeSummary): string {
  const roomNumber = normalizeText(room.roomNumber)
  if (room.roomTypeId !== null && roomNumber) {
    const candidate = `${room.roomTypeId}-${roomNumber}`
    if (candidate.length <= MAX_SU_ROOM_ID_LENGTH) {
      return candidate
    }
  }
  return String(roomType.id)
}

function getRoomPrice(roomType: PmsRoomTypeSummary): number {
  const parsed = Number(roomType.defaultPrice)
  if (Number.isFinite(parsed) && parsed > 0) {
    return Math.round(parsed)
  }
  return DEFAULT_ROOM_PRICE
}

function buildPriceRows(
  arrivalDate: BusinessDate,
  nights: number,
  priceBeforeTax: number,
  tax: number,
  priceAfterTax: number,
  pricePlan: PmsPricePlanSummary | null,
): JsonObject[] {
  const rows: JsonObject[] = []
  const rateId = pricePlan ? String(pricePlan.id) : 'BAR'
  const mealPlan = pricePlan?.includeMeal ? 'Breakfast included' : 'Room only'

  for (let index = 0; index < nights; index += 1) {
    rows.push({
      date: formatBusinessDate(addBusinessDays(arrivalDate, index)),
      rate_id: rateId,
      mealplan_id: rateId,
      mealplan: mealPlan,
      tax: toMoney(tax),
      pricebeforetax: toMoney(priceBeforeTax),
      priceaftertax: toMoney(priceAfterTax),
    })
  }

  return rows
}

function buildGuest(channel: E2EChannelCode, runId: string): JsonObject {
  const profile = E2E_CHANNEL_DEFINITIONS[channel].guest
  return {
    first_name: profile.firstName,
    last_name: `${profile.lastNamePrefix}-${runId.slice(0, 8)}`,
    email: `${profile.emailPrefix}-${runId.slice(0, 8)}@example.com`,
    telephone: profile.telephone,
    countrycode: 'JP',
    city: 'Tokyo',
    address: '1-1 E2E Test Street',
    zip: '100-0001',
    remarks: 'Generated by channel-simulator local E2E run',
    guest_lang: profile.guestLang,
    corporate_booking_detail: {},
    cc_virtual: '0',
  }
}

function cloneJson<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}

function getRooms(reservation: JsonObject): JsonObject[] {
  return reservation.rooms instanceof Array ? (reservation.rooms as JsonObject[]) : []
}

function applyReservationTotals(reservation: JsonObject, rooms: JsonObject[], commissionRate: number): void {
  let totalPrice = 0
  let totalTax = 0
  let totalBeforeTax = 0

  for (const room of rooms) {
    totalPrice += Number(room.totalprice || 0)
    totalTax += Number(room.totaltax || 0)
    totalBeforeTax += Number(room.totalbeforetax || 0)
  }

  reservation.sellamount = toMoney(totalPrice)
  reservation.nettamount = toMoney(totalPrice)
  reservation.totalprice = toMoney(totalPrice)
  reservation.totaltax = toMoney(totalTax)
  reservation.listingbaseprice = toMoney(totalBeforeTax)
  reservation.commissionamount = toMoney(Math.round(totalPrice * commissionRate))
}

function buildAdditionalRoom(
  baseRoom: JsonObject,
  room: PmsRoomSummary,
  roomType: PmsRoomTypeSummary,
  now: Date,
): { payload: JsonObject; roomPayloadId: string; roomReservationId: string } {
  const roomPayloadId = buildRoomPayloadId(room, roomType)
  const roomReservationId = buildRoomReservationId(now)
  const payload = cloneJson(baseRoom)

  payload.info = 'BOOKING local E2E multi-room booking - room 2'
  payload.specialrequest = 'Second room generated by channel-simulator local E2E run'
  payload.guest_name = 'Jiro Booking E2E'
  payload.first_name = 'Jiro'
  payload.last_name = 'Booking E2E'
  payload.id = roomPayloadId
  payload.channel_room_id = roomPayloadId
  payload.roomreservation_id = roomReservationId

  return {
    payload,
    roomPayloadId,
    roomReservationId,
  }
}

function buildLifecycleRelatedReservation(
  base: BuiltReservation,
  runId: string,
  step: E2ELifecycleStepName,
): BuiltReservation {
  const clock = getSimulatorClock()
  const businessToday = formatBusinessDate(clock.today)
  const notifId = buildNotifId(runId)
  const reservation = cloneJson(base.reservation)
  const payload = cloneJson(base.payload)
  const payloadReservations = payload.reservations instanceof Array ? (payload.reservations as JsonObject[]) : []
  payloadReservations[0] = reservation
  payload.reservations = payloadReservations
  reservation.reservation_notif_id = notifId
  reservation.modified_at = businessToday

  const rooms = getRooms(reservation)
  const roomStatus = step === 'cancellation' ? 'cancelled' : 'modified'
  for (const room of rooms) {
    room.roomstaystatus = roomStatus
    if (step === 'modification') {
      room.specialrequest = `${String(room.specialrequest || '')} / lifecycle modification`
      room.eta = '16:00'
    }
  }

  if (step === 'modification') {
    reservation.status = 'modified'
    reservation.processed_at = `${businessToday} 11:00:00`
    reservation.cancelreason = ''
    reservation.cancelamount = ''
  } else {
    reservation.status = 'cancelled'
    reservation.processed_at = `${businessToday} 12:00:00`
    reservation.cancelreason = 'Generated lifecycle cancellation'
    reservation.cancelamount = String(reservation.totalprice || '')
  }

  return {
    ids: {
      ...cloneJson(base.ids),
      runId,
      notifId,
    },
    reservation,
    payload,
    diagnostic: {
      ...cloneJson(base.diagnostic),
      lifecycleStep: step,
      reusedReservationId: base.ids.reservationId,
      reusedChannelBookingId: base.ids.channelBookingId,
      reusedRoomReservationIds: [...base.ids.roomReservationIds],
      previousNotifId: base.ids.notifId,
      simulatorStoreTimeZone: clock.timeZone,
      simulatorFixedNow: clock.fixedNow,
    },
  }
}

export function buildDynamicReservation(
  runId: string,
  request: NormalizedCreateE2ERunRequest,
  rawContext: PmsReadinessData | null,
): BuiltReservation {
  const clock = getSimulatorClock()
  const context = requireReadinessData(rawContext)
  let inventory = selectInventory(context, request)
  let secondRoom: PmsRoomSummary | null = null
  if (request.scenario === 'MULTI_ROOM') {
    const multiRoomInventory = selectMultiRoomInventory(context, request, inventory)
    inventory = multiRoomInventory.first
    secondRoom = multiRoomInventory.secondRoom
  }

  const hotelId = context.suHotelId
  const roomPayloadId = buildRoomPayloadId(inventory.room, inventory.roomType)
  const channelBookingId = buildBookingId(request.channel, clock.now)
  const roomReservationId = buildRoomReservationId(clock.now)
  const notifId = buildNotifId(runId)
  const reservationId = `${channelBookingId}_${hotelId}`

  const stayStartDays = resolveStayStartDays(runId, request, clock.now)
  const arrival = addBusinessDays(clock.today, stayStartDays)
  const departure = addBusinessDays(arrival, DEFAULT_STAY_NIGHTS)
  const bookedAt = formatBusinessDate(clock.today)
  const processedAt = formatStoreDateTime(clock.now, clock.timeZone)
  const priceBeforeTax = getRoomPrice(inventory.roomType)
  const tax = Math.round(priceBeforeTax * DEFAULT_TAX_RATE)
  const priceAfterTax = priceBeforeTax + tax
  const totalBeforeTax = priceBeforeTax * DEFAULT_STAY_NIGHTS
  const totalTax = tax * DEFAULT_STAY_NIGHTS
  const totalPrice = priceAfterTax * DEFAULT_STAY_NIGHTS
  const guest = buildGuest(request.channel, runId)
  const guestName = `${guest.first_name} ${guest.last_name}`
  const channelDefinition = E2E_CHANNEL_DEFINITIONS[request.channel]
  const affiliation = {
    pos: channelDefinition.pos,
    source: channelDefinition.source,
    OTA_Code: String(request.otaCode),
    companyname: '',
  }

  const room: JsonObject = {
    arrival_date: formatBusinessDate(arrival),
    departure_date: formatBusinessDate(departure),
    info: `${request.channel} local E2E single-room booking`,
    facilities: '',
    taxes: [{ name: 'Consumption Tax', value: toMoney(totalTax) }],
    withheldtaxes: [],
    specialrequest: 'Generated by channel-simulator local E2E run',
    eta: '15:00',
    guest_name: guestName,
    first_name: guest.first_name,
    last_name: guest.last_name,
    id: roomPayloadId,
    max_children: String(DEFAULT_CHILDREN),
    numberofguests: String(DEFAULT_ADULTS + DEFAULT_CHILDREN),
    numberofchildren: String(DEFAULT_CHILDREN),
    numberofadults: String(DEFAULT_ADULTS),
    child_age: '',
    roomstaystatus: 'new',
    roomreservation_id: roomReservationId,
    totalbeforetax: toMoney(totalBeforeTax),
    totaltax: toMoney(totalTax),
    totalprice: toMoney(totalPrice),
    price: buildPriceRows(
      arrival,
      DEFAULT_STAY_NIGHTS,
      priceBeforeTax,
      tax,
      priceAfterTax,
      inventory.pricePlan,
    ),
    adults: [],
    addons: [],
    extracomponents: [],
    bed_type: '',
    numberofbeds: '1',
    channel_room_id: roomPayloadId,
    smoking_preference: 'Non-Smoking',
    booking_condition: 'Flexible',
    promotion: '',
    mealplan: inventory.pricePlan?.includeMeal ? 'Breakfast included' : 'Room only',
    cancellation_penalties: [],
  }

  const rooms = [room]
  const roomReservationIds = [roomReservationId]
  const roomPayloadIds = [roomPayloadId]
  if (secondRoom) {
    const additionalRoom = buildAdditionalRoom(room, secondRoom, inventory.roomType, clock.now)
    rooms.push(additionalRoom.payload)
    roomReservationIds.push(additionalRoom.roomReservationId)
    roomPayloadIds.push(additionalRoom.roomPayloadId)
  }

  const reservation: JsonObject = {
    booked_at: bookedAt,
    commissionamount: request.channel === 'AIRBNB' ? '0' : toMoney(Math.round(totalPrice * 0.12)),
    currencycode: DEFAULT_CURRENCY,
    paymenttype: request.channel === 'AIRBNB' ? 'Channel Collect' : 'Hotel Collect',
    hotel_id: hotelId,
    hotel_name: context.storeName || 'PMS Store',
    paymentdue: '',
    customer: guest,
    rooms,
    affiliation,
    chain_id: '',
    external_id: '',
    otadue: '',
    cancelreason: '',
    sellamount: toMoney(totalPrice),
    nettamount: toMoney(totalPrice),
    discount: '',
    confirmationlink: '',
    payment_charge: '',
    booker_genius: 'no',
    smoking_preference: '',
    promotion: '',
    channel_booking_id: channelBookingId,
    thread_id: channelDefinition.hasMessagingThread ? `thread-${runId}` : '',
    guest_id: channelDefinition.hasMessagingThread ? `guest-${runId}` : '',
    numberofpets: '0',
    numberofinfants: '0',
    listingbaseprice: toMoney(totalBeforeTax),
    processed_at: processedAt,
    bookingcharged: 'N',
    amountcharged: '',
    pgtransactionid: '',
    su_payments: { action: '', tokenid: '', pgid: '' },
    deposit: '',
    cvv_available: 'false',
    source: 'Su',
    pg_refid: '',
    pg_type: '',
    cancellation_fee: '',
    vendor_booking_id: '',
    id: reservationId,
    reservation_notif_id: notifId,
    modified_at: bookedAt,
    status: 'new',
    totalprice: toMoney(totalPrice),
    totaltax: toMoney(totalTax),
    cancelamount: '',
    extrafees: [],
    taxes: [],
  }
  applyReservationTotals(reservation, rooms, request.channel === 'AIRBNB' ? 0 : 0.12)

  const ids: E2ERunGeneratedIds = {
    runId,
    hotelId,
    reservationId,
    channelBookingId,
    roomReservationId,
    roomReservationIds,
    notifId,
    otaCode: request.otaCode,
    channel: request.channel,
    roomPayloadId,
    roomPayloadIds,
  }

  return {
    ids,
    reservation,
    payload: {
      hotelid: hotelId,
      reservations: [reservation],
    },
    diagnostic: {
      storeId: context.storeId,
      storeName: context.storeName,
      selectedRoomId: inventory.room.id,
      selectedRoomNumber: inventory.room.roomNumber,
      selectedRoomTypeId: inventory.roomType.id,
      selectedRoomTypeName: inventory.roomType.name,
      selectedPricePlanId: inventory.pricePlan?.id || null,
      selectedChannelId: inventory.channel?.id || null,
      selectedOtaIntegrationId: inventory.otaIntegration?.id || null,
      roomPayloadId,
      roomPayloadIds,
      roomReservationIds,
      scenario: request.scenario,
      stayStartDays,
      stayNights: DEFAULT_STAY_NIGHTS,
      arrivalDate: formatBusinessDate(arrival),
      departureDate: formatBusinessDate(departure),
      simulatorStoreTimeZone: clock.timeZone,
      simulatorFixedNow: clock.fixedNow,
      simulatorBusinessDate: bookedAt,
      simulatorProcessedAt: processedAt,
      roomCount: rooms.length,
    },
  }
}

export function buildLifecycleReservations(
  lifecycleRunId: string,
  request: NormalizedCreateE2ERunRequest,
  rawContext: PmsReadinessData | null,
): BuiltLifecycleReservation[] {
  const normalizedRequest: NormalizedCreateE2ERunRequest = {
    ...request,
    scenario: 'NEW',
    channel: 'BOOKING',
    otaCode: BOOKING_OTA_CODE,
  }
  const newStep = buildDynamicReservation(`${lifecycleRunId}-new`, normalizedRequest, rawContext)
  const modificationStep = buildLifecycleRelatedReservation(
    newStep,
    `${lifecycleRunId}-modification`,
    'modification',
  )
  const cancellationStep = buildLifecycleRelatedReservation(
    modificationStep,
    `${lifecycleRunId}-cancellation`,
    'cancellation',
  )

  return [
    { step: 'new', built: newStep },
    { step: 'modification', built: modificationStep },
    { step: 'cancellation', built: cancellationStep },
  ]
}
