export const BOOKING_CHANNEL_ID = '19'
export const AIRBNB_CHANNEL_ID = '244'
export const DEFAULT_CHANNEL_ID = BOOKING_CHANNEL_ID
export const KNOWN_CHANNEL_IDS = [BOOKING_CHANNEL_ID, AIRBNB_CHANNEL_ID]
export const DEFAULT_ROOM_ID = 'E2ELOCAL'
export const DEFAULT_RATE_PLAN_ID = 'Local E2E Standard Rate'
export const DEFAULT_HOTEL_ID = 'LOCALE2EHOTEL'
export const DEFAULT_ROOM_NAME = 'Local E2E Standard Room'
export const DEFAULT_RATE_PLAN_NAME = 'Local E2E Standard Rate'
export const LOCAL_BOOKING_ROOM_TYPE_ID = 'LOCAL-19-ROOM'
export const LOCAL_BOOKING_RATE_PLAN_ID = 'LOCAL-19-STD'
export const LOCAL_BOOKING_FLEX_RATE_PLAN_ID = 'LOCAL-19-FLEX'
export const LOCAL_AIRBNB_LISTING_ID = 'LOCAL-244-LISTING'
export const LOCAL_AIRBNB_SECOND_LISTING_ID = 'LOCAL-244-LISTING-DELUXE'
export const LOCAL_AIRBNB_RATE_PLAN_ID = 'LOCAL-244-STD'

const DEFAULT_SCENARIO = 'default-multi'
const MAX_WRITE_HISTORY = 200

export type MappingFailureEndpoint =
  | 'booking-rate-plan-map'
  | 'airbnb-listing-map'
  | 'airbnb-listing-update'

export interface MappingPriceRow {
  rowKey: string
  hotelId: string
  channelId: string
  channelCode: 'BOOKING' | 'AIRBNB'
  channelHotelId: string
  roomId: string
  rateId: string
  channelRoomId?: string
  channelRateId?: string
  listingId?: string
  listingName?: string
  applicableNoOfGuest?: string
  occupancy?: string
  multiplier: string
  surcharge: string
  mappingStatus: string
}

export interface MappingFailureRuleInput {
  rowKey?: string
  endpoint?: MappingFailureEndpoint
  times?: number
  statusCode?: number
  body?: unknown
}

export interface MappingFailureRule {
  rowKey: string
  endpoint: MappingFailureEndpoint
  times: number
  statusCode: number
  body: unknown
}

export interface MappingWriteEvent {
  timestamp: string
  endpoint: MappingFailureEndpoint
  rowKey: string
  hotelId: string
  channelId: string
  multiplier: string
  surcharge: string
  outcome: 'success' | 'injected_failure' | 'not_found' | 'duplicate_failure'
}

interface MappingChannelState {
  scenario: string
  hotelId: string
  channelId: string
  rows: MappingPriceRow[]
}

interface PricingWriteBody {
  applicablenoofguest?: string | number
  multiplier?: string | number
  surcharge?: string | number
}

export interface BookingRatePlanMapWriteBody {
  hotelid?: string | number
  channelid?: string | number
  channelhotelid?: string | number
  roomid?: string | number
  rateid?: string | number
  channelroomid?: string | number
  channelrateid?: string | number
  pricing?: PricingWriteBody[]
}

export interface AirbnbListingMapWriteBody {
  hotelid?: string | number
  channelid?: string | number
  channelhotelid?: string | number
  listingid?: string | number
  roomid?: string | number
  rateid?: string | number
  multiplier?: string | number
  surcharge?: string | number
  occupancy?: string | number
}

export interface AirbnbListingUpdateWriteBody extends AirbnbListingMapWriteBody {
  name?: string | number
}

export interface MappingWriteResult {
  statusCode: number
  body: Record<string, unknown>
}

interface MappingPriceStateResetInput {
  hotelId?: unknown
  channelId?: unknown
  scenario?: unknown
}

interface MappingPriceStateSnapshotInput {
  hotelId?: unknown
  channelId?: unknown
}

const mappingStateByKey = new Map<string, MappingChannelState>()
const failureRulesByKey = new Map<string, MappingFailureRule>()
const writeHistory: MappingWriteEvent[] = []

function normalizeText(value: unknown): string | null {
  if (typeof value !== 'string' && typeof value !== 'number') {
    return null
  }

  const text = String(value).trim()
  if (!text) {
    return null
  }

  return text
}

function resolveKnownChannelId(channelId?: unknown, fallback = DEFAULT_CHANNEL_ID): string {
  const normalizedChannelId = normalizeText(channelId)
  if (normalizedChannelId && KNOWN_CHANNEL_IDS.includes(normalizedChannelId)) {
    return normalizedChannelId
  }

  return fallback
}

function getStateKey(hotelId: string, channelId: string): string {
  return `${hotelId}:${channelId}`
}

function getFailureRuleKey(endpoint: MappingFailureEndpoint, rowKey: string): string {
  return `${endpoint}:${rowKey}`
}

function buildBookingRowKey(input: {
  hotelId: string
  channelHotelId?: string
  roomId?: string
  rateId?: string
  channelRoomId?: string
  channelRateId?: string
  applicableNoOfGuest?: string
}): string {
  return [
    'booking',
    input.hotelId,
    BOOKING_CHANNEL_ID,
    input.channelHotelId || 'missing-channel-hotel',
    input.roomId || 'missing-room',
    input.rateId || 'missing-rate',
    input.channelRoomId || 'missing-channel-room',
    input.channelRateId || 'missing-channel-rate',
    input.applicableNoOfGuest || 'missing-guest',
  ].join(':')
}

function buildAirbnbRowKey(input: {
  hotelId: string
  channelHotelId?: string
  listingId?: string
  roomId?: string
  rateId?: string
  occupancy?: string
}): string {
  return [
    'airbnb',
    input.hotelId,
    AIRBNB_CHANNEL_ID,
    input.channelHotelId || 'missing-channel-hotel',
    input.listingId || 'missing-listing',
    input.roomId || 'missing-room',
    input.rateId || 'missing-rate',
    input.occupancy || 'missing-occupancy',
  ].join(':')
}

function createBookingRow(input: {
  hotelId: string
  channelHotelId: string
  channelRateId?: string
  applicableNoOfGuest?: string
  multiplier?: string
  surcharge?: string
}): MappingPriceRow {
  const row = {
    hotelId: input.hotelId,
    channelId: BOOKING_CHANNEL_ID,
    channelCode: 'BOOKING' as const,
    channelHotelId: input.channelHotelId,
    roomId: DEFAULT_ROOM_ID,
    rateId: DEFAULT_RATE_PLAN_ID,
    channelRoomId: LOCAL_BOOKING_ROOM_TYPE_ID,
    channelRateId: input.channelRateId,
    applicableNoOfGuest: input.applicableNoOfGuest,
    multiplier: input.multiplier || '1',
    surcharge: input.surcharge || '0',
    mappingStatus: 'Active',
  }

  return {
    ...row,
    rowKey: buildBookingRowKey(row),
  }
}

function createAirbnbRow(input: {
  hotelId: string
  channelHotelId: string
  listingId?: string
  listingName?: string
  occupancy?: string
  multiplier?: string
  surcharge?: string
}): MappingPriceRow {
  const listingName = input.listingName || `${input.listingId || 'Local Airbnb'} Listing`
  const row = {
    hotelId: input.hotelId,
    channelId: AIRBNB_CHANNEL_ID,
    channelCode: 'AIRBNB' as const,
    channelHotelId: input.channelHotelId,
    roomId: DEFAULT_ROOM_ID,
    rateId: DEFAULT_RATE_PLAN_ID,
    channelRoomId: input.listingId,
    channelRateId: LOCAL_AIRBNB_RATE_PLAN_ID,
    listingId: input.listingId,
    listingName,
    occupancy: input.occupancy,
    multiplier: input.multiplier || '1',
    surcharge: input.surcharge || '0',
    mappingStatus: 'Active',
  }

  return {
    ...row,
    rowKey: buildAirbnbRowKey(row),
  }
}

function createDefaultRows(hotelId: string, channelId: string): MappingPriceRow[] {
  if (channelId === AIRBNB_CHANNEL_ID) {
    return [
      createAirbnbRow({
        hotelId,
        channelHotelId: `LOCAL-${AIRBNB_CHANNEL_ID}-HOTEL`,
        listingId: LOCAL_AIRBNB_LISTING_ID,
        occupancy: '1',
      }),
      createAirbnbRow({
        hotelId,
        channelHotelId: `LOCAL-${AIRBNB_CHANNEL_ID}-HOTEL`,
        listingId: LOCAL_AIRBNB_LISTING_ID,
        occupancy: '2',
      }),
      createAirbnbRow({
        hotelId,
        channelHotelId: `LOCAL-${AIRBNB_CHANNEL_ID}-HOTEL`,
        listingId: LOCAL_AIRBNB_SECOND_LISTING_ID,
        occupancy: '2',
      }),
    ]
  }

  return [
    createBookingRow({
      hotelId,
      channelHotelId: `LOCAL-${BOOKING_CHANNEL_ID}-HOTEL`,
      channelRateId: LOCAL_BOOKING_RATE_PLAN_ID,
      applicableNoOfGuest: '1',
    }),
    createBookingRow({
      hotelId,
      channelHotelId: `LOCAL-${BOOKING_CHANNEL_ID}-HOTEL`,
      channelRateId: LOCAL_BOOKING_RATE_PLAN_ID,
      applicableNoOfGuest: '2',
    }),
    createBookingRow({
      hotelId,
      channelHotelId: `LOCAL-${BOOKING_CHANNEL_ID}-HOTEL`,
      channelRateId: LOCAL_BOOKING_FLEX_RATE_PLAN_ID,
      applicableNoOfGuest: '2',
    }),
  ]
}

function createScenarioRows(hotelId: string, channelId: string, scenario: string): MappingPriceRow[] {
  if (scenario === 'empty-booking' && channelId === BOOKING_CHANNEL_ID) {
    return []
  }

  if (scenario === 'empty-airbnb' && channelId === AIRBNB_CHANNEL_ID) {
    return []
  }

  if (scenario === 'stale-booking-missing-channelRateId' && channelId === BOOKING_CHANNEL_ID) {
    return [
      createBookingRow({
        hotelId,
        channelHotelId: `LOCAL-${BOOKING_CHANNEL_ID}-HOTEL`,
        applicableNoOfGuest: '2',
      }),
    ]
  }

  if (scenario === 'stale-booking-missing-applicableGuest' && channelId === BOOKING_CHANNEL_ID) {
    return [
      createBookingRow({
        hotelId,
        channelHotelId: `LOCAL-${BOOKING_CHANNEL_ID}-HOTEL`,
        channelRateId: LOCAL_BOOKING_RATE_PLAN_ID,
      }),
    ]
  }

  if (scenario === 'stale-airbnb-missing-listingId' && channelId === AIRBNB_CHANNEL_ID) {
    return [
      createAirbnbRow({
        hotelId,
        channelHotelId: `LOCAL-${AIRBNB_CHANNEL_ID}-HOTEL`,
        occupancy: '2',
      }),
    ]
  }

  if (scenario === 'stale-airbnb-missing-occupancy' && channelId === AIRBNB_CHANNEL_ID) {
    return [
      createAirbnbRow({
        hotelId,
        channelHotelId: `LOCAL-${AIRBNB_CHANNEL_ID}-HOTEL`,
        listingId: LOCAL_AIRBNB_LISTING_ID,
      }),
    ]
  }

  return createDefaultRows(hotelId, channelId)
}

function cloneRow(row: MappingPriceRow): MappingPriceRow {
  return { ...row }
}

function ensureState(hotelId: string, channelId: string): MappingChannelState {
  const stateKey = getStateKey(hotelId, channelId)
  const existingState = mappingStateByKey.get(stateKey)
  if (existingState) {
    return existingState
  }

  const createdState = {
    scenario: DEFAULT_SCENARIO,
    hotelId,
    channelId,
    rows: createDefaultRows(hotelId, channelId),
  }
  mappingStateByKey.set(stateKey, createdState)
  return createdState
}

function getRowsForState(hotelId: string, channelId: string): MappingPriceRow[] {
  return ensureState(hotelId, channelId).rows
}

function findRowByKey(endpoint: MappingFailureEndpoint, rowKey: string): MappingPriceRow | null {
  for (const state of mappingStateByKey.values()) {
    if (
      endpoint === 'booking-rate-plan-map' &&
      state.channelId !== BOOKING_CHANNEL_ID
    ) {
      continue
    }

    if (
      (endpoint === 'airbnb-listing-map' || endpoint === 'airbnb-listing-update') &&
      state.channelId !== AIRBNB_CHANNEL_ID
    ) {
      continue
    }

    for (const row of state.rows) {
      if (row.rowKey === rowKey) {
        return row
      }
    }
  }

  return null
}

function addWriteEvent(event: MappingWriteEvent): void {
  writeHistory.push(event)
  if (writeHistory.length > MAX_WRITE_HISTORY) {
    writeHistory.splice(0, writeHistory.length - MAX_WRITE_HISTORY)
  }
}

function consumeFailureRule(
  endpoint: MappingFailureEndpoint,
  rowKey: string,
): MappingFailureRule | null {
  const failureKey = getFailureRuleKey(endpoint, rowKey)
  const rule = failureRulesByKey.get(failureKey)
  if (!rule) {
    return null
  }

  if (rule.times > 0) {
    rule.times -= 1
    if (rule.times === 0) {
      failureRulesByKey.delete(failureKey)
    }
  }

  return { ...rule }
}

function inferEndpointFromRowKey(rowKey: string): MappingFailureEndpoint | null {
  if (rowKey.startsWith('booking:')) {
    return 'booking-rate-plan-map'
  }

  if (rowKey.startsWith('airbnb:')) {
    return 'airbnb-listing-update'
  }

  return null
}

function buildDefaultFailureBody(rowKey: string): Record<string, unknown> {
  return {
    Status: 'Fail',
    Errors: {
      ShortText: 'Injected mapping row failure',
    },
    RowKey: rowKey,
  }
}

function getFailureRulesSnapshot(): MappingFailureRule[] {
  const rules: MappingFailureRule[] = []
  for (const rule of failureRulesByKey.values()) {
    rules.push({ ...rule })
  }

  return rules
}

function buildPricingObject(row: MappingPriceRow): Record<string, string> {
  const pricing: Record<string, string> = {
    Multiplier: row.multiplier,
    Surcharge: row.surcharge,
  }

  const guestValue = row.applicableNoOfGuest || row.occupancy
  if (guestValue) {
    pricing.ApplicableNoOfGuest = guestValue
  }

  return pricing
}

function buildRatePlanPayload(row: MappingPriceRow): Record<string, unknown> {
  const ratePlan: Record<string, unknown> = {
    RatePlanID: row.rateId,
    PMSRoomID: row.roomId,
    PMSRateID: row.rateId,
    ChannelRoomID: row.channelRoomId,
    ChannelRateID: row.channelRateId,
    MappingStatus: row.mappingStatus,
    Pricing: buildPricingObject(row),
  }

  if (row.channelId === AIRBNB_CHANNEL_ID) {
    ratePlan.ListingID = row.listingId
    ratePlan.Occupancy = row.occupancy
  }

  return ratePlan
}

export function getChannelName(channelId: string): string {
  if (channelId === AIRBNB_CHANNEL_ID) {
    return 'Airbnb'
  }

  return 'Booking.com'
}

export function getChannelRoomTypeId(channelId: string): string {
  if (channelId === AIRBNB_CHANNEL_ID) {
    return LOCAL_AIRBNB_LISTING_ID
  }

  return LOCAL_BOOKING_ROOM_TYPE_ID
}

export function getChannelRatePlanId(channelId: string): string {
  if (channelId === AIRBNB_CHANNEL_ID) {
    return LOCAL_AIRBNB_RATE_PLAN_ID
  }

  return LOCAL_BOOKING_RATE_PLAN_ID
}

export function buildChannelRatePlanCombo(channelId: string, ratePlanId?: string): string {
  const channelRoomTypeId = getChannelRoomTypeId(channelId)
  const channelRatePlanId = ratePlanId || getChannelRatePlanId(channelId)

  return `${channelRoomTypeId}####${channelRatePlanId}`
}

export function buildMappingItems(hotelIdInput: unknown, channelId: string): Record<string, unknown>[] {
  const hotelId = normalizeText(hotelIdInput) || DEFAULT_HOTEL_ID
  const rows = getRowsForState(hotelId, channelId)
  const rowsByChannelHotelId = new Map<string, MappingPriceRow[]>()

  for (const row of rows) {
    const channelHotelRows = rowsByChannelHotelId.get(row.channelHotelId) || []
    channelHotelRows.push(row)
    rowsByChannelHotelId.set(row.channelHotelId, channelHotelRows)
  }

  const items: Record<string, unknown>[] = []
  for (const [channelHotelId, channelHotelRows] of rowsByChannelHotelId) {
    items.push({
      Status: 'Active',
      ChannelID: channelId,
      ChannelHotelID: channelHotelId,
      RoomIDs: [DEFAULT_ROOM_ID],
      Rateplans: channelHotelRows.map((row) => buildRatePlanPayload(row)),
    })
  }

  return items
}

export function resetMappingPriceState(input: MappingPriceStateResetInput = {}) {
  const hotelId = normalizeText(input.hotelId) || DEFAULT_HOTEL_ID
  const channelId = normalizeText(input.channelId)
  const scenario = normalizeText(input.scenario) || DEFAULT_SCENARIO
  const channelIds = channelId && KNOWN_CHANNEL_IDS.includes(channelId) ? [channelId] : KNOWN_CHANNEL_IDS

  mappingStateByKey.clear()
  failureRulesByKey.clear()
  writeHistory.length = 0

  for (const currentChannelId of channelIds) {
    mappingStateByKey.set(getStateKey(hotelId, currentChannelId), {
      scenario,
      hotelId,
      channelId: currentChannelId,
      rows: createScenarioRows(hotelId, currentChannelId, scenario),
    })
  }

  return getMappingPriceStateSnapshot({ hotelId, channelId })
}

export function getMappingPriceStateSnapshot(input: MappingPriceStateSnapshotInput = {}) {
  const hotelId = normalizeText(input.hotelId)
  const channelId = normalizeText(input.channelId)
  const states: MappingChannelState[] = []

  if (hotelId && channelId && KNOWN_CHANNEL_IDS.includes(channelId)) {
    states.push(ensureState(hotelId, channelId))
  } else if (hotelId) {
    for (const currentChannelId of KNOWN_CHANNEL_IDS) {
      states.push(ensureState(hotelId, currentChannelId))
    }
  } else {
    if (mappingStateByKey.size === 0) {
      for (const currentChannelId of KNOWN_CHANNEL_IDS) {
        states.push(ensureState(DEFAULT_HOTEL_ID, currentChannelId))
      }
    } else {
      for (const state of mappingStateByKey.values()) {
        states.push(state)
      }
    }
  }

  const rows: MappingPriceRow[] = []
  const scenarios: Record<string, string> = {}
  for (const state of states) {
    scenarios[getStateKey(state.hotelId, state.channelId)] = state.scenario
    for (const row of state.rows) {
      rows.push(cloneRow(row))
    }
  }

  return {
    success: true,
    data: {
      rowCount: rows.length,
      rows,
      scenarios,
      failureRules: getFailureRulesSnapshot(),
      writeHistory: writeHistory.map((event) => ({ ...event })),
    },
  }
}

export function setMappingPriceFailures(input: {
  failures?: MappingFailureRuleInput[]
  failure?: MappingFailureRuleInput
  rowKey?: string
  endpoint?: MappingFailureEndpoint
  times?: number
  statusCode?: number
  body?: unknown
}) {
  const failureInputs: MappingFailureRuleInput[] = []
  if (Array.isArray(input.failures)) {
    for (const failure of input.failures) {
      failureInputs.push(failure)
    }
  }

  if (input.failure) {
    failureInputs.push(input.failure)
  }

  if (input.rowKey) {
    failureInputs.push({
      rowKey: input.rowKey,
      endpoint: input.endpoint,
      times: input.times,
      statusCode: input.statusCode,
      body: input.body,
    })
  }

  for (const failure of failureInputs) {
    const rowKey = normalizeText(failure.rowKey)
    if (!rowKey) {
      continue
    }

    const endpoint = failure.endpoint || inferEndpointFromRowKey(rowKey)
    if (!endpoint) {
      continue
    }

    const configuredTimes = typeof failure.times === 'number' ? failure.times : 1
    const times = configuredTimes === -1 ? -1 : Math.max(1, configuredTimes)
    const statusCode = typeof failure.statusCode === 'number' ? failure.statusCode : 200
    const body = failure.body || buildDefaultFailureBody(rowKey)
    const rule = {
      rowKey,
      endpoint,
      times,
      statusCode,
      body,
    }

    failureRulesByKey.set(getFailureRuleKey(endpoint, rowKey), rule)
  }

  return {
    success: true,
    data: {
      failureRules: getFailureRulesSnapshot(),
    },
  }
}

export function clearMappingPriceFailures() {
  failureRulesByKey.clear()
  return {
    success: true,
    data: {
      failureRules: getFailureRulesSnapshot(),
    },
  }
}

export function updateBookingRatePlanMap(body: BookingRatePlanMapWriteBody): MappingWriteResult {
  const pricing = Array.isArray(body.pricing) ? body.pricing[0] : null
  const hotelId = normalizeText(body.hotelid) || DEFAULT_HOTEL_ID
  const channelHotelId = normalizeText(body.channelhotelid)
  const roomId = normalizeText(body.roomid)
  const rateId = normalizeText(body.rateid)
  const channelRoomId = normalizeText(body.channelroomid)
  const channelRateId = normalizeText(body.channelrateid)
  const applicableNoOfGuest = normalizeText(pricing?.applicablenoofguest)
  const multiplier = normalizeText(pricing?.multiplier)
  const surcharge = normalizeText(pricing?.surcharge)

  if (!pricing || !multiplier || !surcharge || !applicableNoOfGuest) {
    return {
      statusCode: 400,
      body: {
        Status: 'Fail',
        Errors: {
          ShortText: 'pricing multiplier, surcharge, and applicablenoofguest are required',
        },
      },
    }
  }

  const rowKey = buildBookingRowKey({
    hotelId,
    channelHotelId: channelHotelId || undefined,
    roomId: roomId || undefined,
    rateId: rateId || undefined,
    channelRoomId: channelRoomId || undefined,
    channelRateId: channelRateId || undefined,
    applicableNoOfGuest,
  })
  ensureState(hotelId, BOOKING_CHANNEL_ID)
  const row = findRowByKey('booking-rate-plan-map', rowKey)

  if (!row) {
    addWriteEvent({
      timestamp: new Date().toISOString(),
      endpoint: 'booking-rate-plan-map',
      rowKey,
      hotelId,
      channelId: BOOKING_CHANNEL_ID,
      multiplier,
      surcharge,
      outcome: 'not_found',
    })

    return {
      statusCode: 200,
      body: {
        Status: 'Fail',
        Errors: {
          ShortText: 'Booking mapping row not found',
        },
        RowKey: rowKey,
      },
    }
  }

  const failureRule = consumeFailureRule('booking-rate-plan-map', rowKey)
  if (failureRule) {
    addWriteEvent({
      timestamp: new Date().toISOString(),
      endpoint: 'booking-rate-plan-map',
      rowKey,
      hotelId,
      channelId: BOOKING_CHANNEL_ID,
      multiplier,
      surcharge,
      outcome: 'injected_failure',
    })

    return {
      statusCode: failureRule.statusCode,
      body: failureRule.body as Record<string, unknown>,
    }
  }

  row.multiplier = multiplier
  row.surcharge = surcharge
  addWriteEvent({
    timestamp: new Date().toISOString(),
    endpoint: 'booking-rate-plan-map',
    rowKey,
    hotelId,
    channelId: BOOKING_CHANNEL_ID,
    multiplier,
    surcharge,
    outcome: 'success',
  })

  return {
    statusCode: 200,
    body: {
      Status: 'Success',
      Message: 'Local Booking rate plan mapping updated',
      RowKey: rowKey,
      Data: {
        channelhotelid: body.channelhotelid,
        roomid: body.roomid,
        rateid: body.rateid,
        channelroomid: body.channelroomid,
        channelrateid: body.channelrateid,
        pricing,
      },
    },
  }
}

export function updateAirbnbListingMap(body: AirbnbListingMapWriteBody): MappingWriteResult {
  const hotelId = normalizeText(body.hotelid) || DEFAULT_HOTEL_ID
  const channelHotelId = normalizeText(body.channelhotelid)
  const listingId = normalizeText(body.listingid)
  const roomId = normalizeText(body.roomid)
  const rateId = normalizeText(body.rateid)
  const occupancy = normalizeText(body.occupancy)
  const multiplier = normalizeText(body.multiplier)
  const surcharge = normalizeText(body.surcharge)

  const rowKey = buildAirbnbRowKey({
    hotelId,
    channelHotelId: channelHotelId || undefined,
    listingId: listingId || undefined,
    roomId: roomId || undefined,
    rateId: rateId || undefined,
    occupancy: occupancy || undefined,
  })
  ensureState(hotelId, AIRBNB_CHANNEL_ID)
  const row = findRowByKey('airbnb-listing-map', rowKey)

  if (!row) {
    addWriteEvent({
      timestamp: new Date().toISOString(),
      endpoint: 'airbnb-listing-map',
      rowKey,
      hotelId,
      channelId: AIRBNB_CHANNEL_ID,
      multiplier: multiplier || '',
      surcharge: surcharge || '',
      outcome: 'not_found',
    })

    return {
      statusCode: 200,
      body: {
        Status: 'Fail',
        Errors: {
          ShortText: 'Airbnb mapping row not found',
        },
        RowKey: rowKey,
      },
    }
  }

  addWriteEvent({
    timestamp: new Date().toISOString(),
    endpoint: 'airbnb-listing-map',
    rowKey,
    hotelId,
    channelId: AIRBNB_CHANNEL_ID,
    multiplier: multiplier || '',
    surcharge: surcharge || '',
    outcome: 'duplicate_failure',
  })

  return {
    statusCode: 200,
    body: {
      Status: 'Fail',
      Errors: {
        ShortText: 'Room and rateplan combination already mapped',
      },
      RowKey: rowKey,
    },
  }
}

export function retrieveAirbnbListing(body: AirbnbListingMapWriteBody): MappingWriteResult {
  const hotelId = normalizeText(body.hotelid) || DEFAULT_HOTEL_ID
  const listingId = normalizeText(body.listingid)
  ensureState(hotelId, AIRBNB_CHANNEL_ID)

  let listingRow: MappingPriceRow | null = null
  for (const row of getRowsForState(hotelId, AIRBNB_CHANNEL_ID)) {
    if (row.listingId === listingId) {
      listingRow = row
      break
    }
  }

  if (!listingRow) {
    return {
      statusCode: 200,
      body: {
        Status: 'Fail',
        Errors: {
          ShortText: 'Airbnb listing not found',
        },
      },
    }
  }

  return {
    statusCode: 200,
    body: {
      Status: 'Success',
      Data: {
        listing: {
          hotelid: hotelId,
          channelhotelid: listingRow.channelHotelId,
          listingid: listingRow.listingId,
          roomid: listingRow.roomId,
          rateid: listingRow.rateId,
          name: listingRow.listingName,
          person_capacity: 2,
          room_type_category: 'entire_home',
        },
      },
    },
  }
}

export function updateAirbnbListingUpdate(body: AirbnbListingUpdateWriteBody): MappingWriteResult {
  const hotelId = normalizeText(body.hotelid) || DEFAULT_HOTEL_ID
  const channelHotelId = normalizeText(body.channelhotelid)
  const listingId = normalizeText(body.listingid)
  const listingName = normalizeText(body.name)
  const roomId = normalizeText(body.roomid)
  const rateId = normalizeText(body.rateid)
  const occupancy = normalizeText(body.occupancy)
  const multiplier = normalizeText(body.multiplier)
  const surcharge = normalizeText(body.surcharge)

  const rowKey = buildAirbnbRowKey({
    hotelId,
    channelHotelId: channelHotelId || undefined,
    listingId: listingId || undefined,
    roomId: roomId || undefined,
    rateId: rateId || undefined,
    occupancy: occupancy || undefined,
  })
  ensureState(hotelId, AIRBNB_CHANNEL_ID)
  const row = findRowByKey('airbnb-listing-update', rowKey)

  if (!row) {
    addWriteEvent({
      timestamp: new Date().toISOString(),
      endpoint: 'airbnb-listing-update',
      rowKey,
      hotelId,
      channelId: AIRBNB_CHANNEL_ID,
      multiplier: multiplier || '',
      surcharge: surcharge || '',
      outcome: 'not_found',
    })

    return {
      statusCode: 200,
      body: {
        Status: 'Fail',
        Errors: {
          ShortText: 'Airbnb mapping row not found',
        },
        RowKey: rowKey,
      },
    }
  }

  const failureRule = consumeFailureRule('airbnb-listing-update', rowKey)
  if (failureRule) {
    addWriteEvent({
      timestamp: new Date().toISOString(),
      endpoint: 'airbnb-listing-update',
      rowKey,
      hotelId,
      channelId: AIRBNB_CHANNEL_ID,
      multiplier: multiplier || '',
      surcharge: surcharge || '',
      outcome: 'injected_failure',
    })

    return {
      statusCode: failureRule.statusCode,
      body: failureRule.body as Record<string, unknown>,
    }
  }

  row.listingName = listingName || row.listingName
  row.multiplier = multiplier || '1'
  row.surcharge = surcharge || '0'
  addWriteEvent({
    timestamp: new Date().toISOString(),
    endpoint: 'airbnb-listing-update',
    rowKey,
    hotelId,
    channelId: AIRBNB_CHANNEL_ID,
    multiplier: row.multiplier,
    surcharge: row.surcharge,
    outcome: 'success',
  })

  return {
    statusCode: 200,
    body: {
      Status: 'Success',
      Message: 'Local Airbnb listing updated',
      RowKey: rowKey,
      Data: {
        listing: {
          channelhotelid: body.channelhotelid,
          listingid: body.listingid,
          name: row.listingName,
          roomid: body.roomid,
          rateid: body.rateid,
          multiplier: body.multiplier,
          surcharge: body.surcharge,
          occupancy: body.occupancy,
        },
      },
    },
  }
}
