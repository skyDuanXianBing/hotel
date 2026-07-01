import assert from 'node:assert/strict'
import axios from 'axios'

import {
  AIRBNB_CHANNEL_ID,
  BOOKING_CHANNEL_ID,
  DEFAULT_HOTEL_ID,
  LOCAL_AIRBNB_LISTING_ID,
  LOCAL_AIRBNB_SECOND_LISTING_ID,
  LOCAL_BOOKING_FLEX_RATE_PLAN_ID,
  LOCAL_BOOKING_RATE_PLAN_ID,
  type MappingFailureEndpoint,
} from '../mock-su-api/mappingPriceState'

const DEFAULT_BASE_URL = 'http://localhost:4000'
const SIMULATOR_BASE_URL = process.env.SIMULATOR_BASE_URL || DEFAULT_BASE_URL
const HOTEL_ID = process.env.SIMULATOR_MAPPING_HOTEL_ID || DEFAULT_HOTEL_ID
const AUTH_HEADERS = {
  Authorization: 'Bearer mock-access-token-mapping-price-verifier',
}

interface MappingPriceRow {
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
}

interface MappingWriteEvent {
  endpoint: MappingFailureEndpoint
  rowKey: string
  outcome: 'success' | 'injected_failure' | 'not_found' | 'duplicate_failure'
}

interface MappingStateSnapshot {
  success: boolean
  data: {
    rowCount: number
    rows: MappingPriceRow[]
    failureRules: Array<{ rowKey: string; endpoint: MappingFailureEndpoint; times: number }>
    writeHistory: MappingWriteEvent[]
  }
}

interface SuMappingsResponse {
  Status: string
  [BOOKING_CHANNEL_ID]?: MappingItem[]
  [AIRBNB_CHANNEL_ID]?: MappingItem[]
}

interface MappingItem {
  Rateplans?: MappingRatePlan[]
}

interface MappingRatePlan {
  ChannelRateID?: string
  ListingID?: string
  Occupancy?: string
  Pricing?: {
    ApplicableNoOfGuest?: string
    Multiplier?: string
    Surcharge?: string
  }
}

interface LogResponse {
  success: boolean
  data: Array<{
    path: string
    requestBody: unknown
    responseBody: unknown
  }>
}

const http = axios.create({
  baseURL: SIMULATOR_BASE_URL,
  timeout: 5000,
  validateStatus: () => true,
})

async function assertSimulatorAvailable(): Promise<void> {
  try {
    const response = await http.get('/api/config')
    if (response.status >= 200 && response.status < 300) {
      return
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    throw new Error(
      `Channel simulator is not reachable at ${SIMULATOR_BASE_URL}: ${message}. Start it separately before running this verifier.`,
    )
  }

  throw new Error(
    `Channel simulator is not reachable at ${SIMULATOR_BASE_URL}. Start it separately before running this verifier.`,
  )
}

async function resetState(): Promise<MappingStateSnapshot> {
  const response = await http.post('/api/test/channel-mapping-price-settings/reset', {
    hotelId: HOTEL_ID,
    scenario: 'default-multi',
  })
  assert.equal(response.status, 200)
  return response.data as MappingStateSnapshot
}

async function readState(): Promise<MappingStateSnapshot> {
  const response = await http.get('/api/test/channel-mapping-price-settings/state', {
    params: { hotelId: HOTEL_ID },
  })
  assert.equal(response.status, 200)
  return response.data as MappingStateSnapshot
}

async function readMappings(): Promise<SuMappingsResponse> {
  const response = await http.post(
    '/SUAPI/jservice/mappings',
    { hotelid: HOTEL_ID },
    { headers: AUTH_HEADERS },
  )
  assert.equal(response.status, 200)
  return response.data as SuMappingsResponse
}

function getRows(snapshot: MappingStateSnapshot, channelId: string): MappingPriceRow[] {
  const rows: MappingPriceRow[] = []
  for (const row of snapshot.data.rows) {
    if (row.channelId === channelId) {
      rows.push(row)
    }
  }

  return rows
}

function findRequiredRow(
  rows: MappingPriceRow[],
  description: string,
  predicate: (row: MappingPriceRow) => boolean,
): MappingPriceRow {
  for (const row of rows) {
    if (predicate(row)) {
      return row
    }
  }

  throw new Error(`Expected mapping row was not found: ${description}`)
}

function assertDefaultState(snapshot: MappingStateSnapshot): void {
  assert.equal(snapshot.success, true)
  assert.equal(snapshot.data.rowCount, 6)

  const bookingRows = getRows(snapshot, BOOKING_CHANNEL_ID)
  const airbnbRows = getRows(snapshot, AIRBNB_CHANNEL_ID)
  assert.equal(bookingRows.length, 3)
  assert.equal(airbnbRows.length, 3)

  findRequiredRow(
    bookingRows,
    'Booking STD guest 1',
    (row) => row.channelRateId === LOCAL_BOOKING_RATE_PLAN_ID && row.applicableNoOfGuest === '1',
  )
  findRequiredRow(
    bookingRows,
    'Booking STD guest 2',
    (row) => row.channelRateId === LOCAL_BOOKING_RATE_PLAN_ID && row.applicableNoOfGuest === '2',
  )
  findRequiredRow(
    bookingRows,
    'Booking FLEX guest 2',
    (row) => row.channelRateId === LOCAL_BOOKING_FLEX_RATE_PLAN_ID && row.applicableNoOfGuest === '2',
  )
  findRequiredRow(
    airbnbRows,
    'Airbnb primary listing occupancy 1',
    (row) => row.listingId === LOCAL_AIRBNB_LISTING_ID && row.occupancy === '1',
  )
  findRequiredRow(
    airbnbRows,
    'Airbnb primary listing occupancy 2',
    (row) => row.listingId === LOCAL_AIRBNB_LISTING_ID && row.occupancy === '2',
  )
  findRequiredRow(
    airbnbRows,
    'Airbnb second listing occupancy 2',
    (row) => row.listingId === LOCAL_AIRBNB_SECOND_LISTING_ID && row.occupancy === '2',
  )
}

function assertMappingsResponse(mappings: SuMappingsResponse): void {
  assert.equal(mappings.Status, 'Success')
  const bookingItems = mappings[BOOKING_CHANNEL_ID] || []
  const airbnbItems = mappings[AIRBNB_CHANNEL_ID] || []
  assert.equal(bookingItems.length, 1)
  assert.equal(airbnbItems.length, 1)

  const bookingRatePlans = bookingItems[0].Rateplans || []
  const airbnbRatePlans = airbnbItems[0].Rateplans || []
  assert.equal(bookingRatePlans.length, 3)
  assert.equal(airbnbRatePlans.length, 3)

  const bookingGuestValues = new Set<string>()
  for (const ratePlan of bookingRatePlans) {
    if (ratePlan.Pricing?.ApplicableNoOfGuest) {
      bookingGuestValues.add(ratePlan.Pricing.ApplicableNoOfGuest)
    }
  }
  assert.equal(bookingGuestValues.has('1'), true)
  assert.equal(bookingGuestValues.has('2'), true)

  const airbnbListingValues = new Set<string>()
  const airbnbOccupancyValues = new Set<string>()
  for (const ratePlan of airbnbRatePlans) {
    if (ratePlan.ListingID) {
      airbnbListingValues.add(ratePlan.ListingID)
    }
    if (ratePlan.Occupancy) {
      airbnbOccupancyValues.add(ratePlan.Occupancy)
    }
  }
  assert.equal(airbnbListingValues.has(LOCAL_AIRBNB_LISTING_ID), true)
  assert.equal(airbnbListingValues.has(LOCAL_AIRBNB_SECOND_LISTING_ID), true)
  assert.equal(airbnbOccupancyValues.has('1'), true)
  assert.equal(airbnbOccupancyValues.has('2'), true)
}

async function writeBookingRow(
  row: MappingPriceRow,
  multiplier: string,
  surcharge: string,
): Promise<Record<string, unknown>> {
  const response = await http.post(
    '/SUAPI/jservice/OTA_RatePlanMap',
    {
      hotelid: HOTEL_ID,
      channelhotelid: row.channelHotelId,
      roomid: row.roomId,
      rateid: row.rateId,
      channelroomid: row.channelRoomId,
      channelrateid: row.channelRateId,
      pricing: [
        {
          applicablenoofguest: row.applicableNoOfGuest,
          multiplier,
          surcharge,
        },
      ],
    },
    { headers: AUTH_HEADERS },
  )
  assert.equal(response.status, 200)
  return response.data as Record<string, unknown>
}

async function mapAirbnbRow(
  row: MappingPriceRow,
  multiplier: string,
  surcharge: string,
): Promise<Record<string, unknown>> {
  const response = await http.post(
    '/SUAPI/jservice/airbnb/listing/map',
    {
      hotelid: HOTEL_ID,
      channelhotelid: row.channelHotelId,
      listingid: row.listingId,
      roomid: row.roomId,
      rateid: row.rateId,
      multiplier,
      surcharge,
      occupancy: row.occupancy,
    },
    { headers: AUTH_HEADERS },
  )
  assert.equal(response.status, 200)
  return response.data as Record<string, unknown>
}

async function updateAirbnbRow(
  row: MappingPriceRow,
  multiplier: string,
  surcharge: string,
): Promise<Record<string, unknown>> {
  const response = await http.post(
    '/SUAPI/jservice/airbnb/listing/update',
    {
      hotelid: HOTEL_ID,
      channelhotelid: row.channelHotelId,
      listingid: row.listingId,
      name: row.listingName || `${row.listingId} Listing`,
      roomid: row.roomId,
      rateid: row.rateId,
      multiplier,
      surcharge,
      occupancy: row.occupancy,
    },
    { headers: AUTH_HEADERS },
  )
  assert.equal(response.status, 200)
  return response.data as Record<string, unknown>
}

async function updateAirbnbRowWithUnknownField(row: MappingPriceRow): Promise<Record<string, unknown>> {
  const response = await http.post(
    '/SUAPI/jservice/airbnb/listing/update',
    {
      hotelid: HOTEL_ID,
      channelhotelid: row.channelHotelId,
      listingid: row.listingId,
      name: row.listingName || `${row.listingId} Listing`,
      roomid: row.roomId,
      rateid: row.rateId,
      multiplier: '0.9',
      surcharge: '1',
      occupancy: row.occupancy,
      retrieve_only_field: 'should fail',
    },
    { headers: AUTH_HEADERS },
  )
  assert.equal(response.status, 400)
  return response.data as Record<string, unknown>
}

async function updateAirbnbRowWithIncompleteCheckIn(row: MappingPriceRow): Promise<Record<string, unknown>> {
  const response = await http.post(
    '/SUAPI/jservice/airbnb/listing/update',
    {
      hotelid: HOTEL_ID,
      channelhotelid: row.channelHotelId,
      listingid: row.listingId,
      name: row.listingName || `${row.listingId} Listing`,
      roomid: row.roomId,
      rateid: row.rateId,
      multiplier: '0.9',
      surcharge: '1',
      occupancy: row.occupancy,
      check_in_option: {
        category: 'self_check_in',
      },
    },
    { headers: AUTH_HEADERS },
  )
  assert.equal(response.status, 200)
  return response.data as Record<string, unknown>
}

async function injectFailure(row: MappingPriceRow, endpoint: MappingFailureEndpoint): Promise<void> {
  const response = await http.post('/api/test/channel-mapping-price-settings/failures', {
    failures: [
      {
        rowKey: row.rowKey,
        endpoint,
        times: 1,
        statusCode: 200,
        body: {
          Status: 'Fail',
          Errors: {
            ShortText: `Injected failure for ${row.rowKey}`,
          },
        },
      },
    ],
  })
  assert.equal(response.status, 200)
}

function assertRowValue(
  snapshot: MappingStateSnapshot,
  rowKey: string,
  multiplier: string,
  surcharge: string,
): void {
  const row = findRequiredRow(snapshot.data.rows, rowKey, (candidate) => candidate.rowKey === rowKey)
  assert.equal(row.multiplier, multiplier)
  assert.equal(row.surcharge, surcharge)
}

async function assertLogsContainExpectedPaths(): Promise<void> {
  const response = await http.get('/api/logs')
  assert.equal(response.status, 200)
  const logs = response.data as LogResponse
  assert.equal(logs.success, true)

  const paths = new Set<string>()
  for (const entry of logs.data) {
    paths.add(entry.path)
  }

  assert.equal(paths.has('/SUAPI/jservice/mappings'), true)
  assert.equal(paths.has('/SUAPI/jservice/OTA_RatePlanMap'), true)
  assert.equal(paths.has('/SUAPI/jservice/airbnb/listing/map'), true)
  assert.equal(paths.has('/SUAPI/jservice/airbnb/listing/update'), true)
}

async function main(): Promise<void> {
  await assertSimulatorAvailable()

  const resetSnapshot = await resetState()
  assertDefaultState(resetSnapshot)

  const mappings = await readMappings()
  assertMappingsResponse(mappings)

  const initialSnapshot = await readState()
  const bookingRows = getRows(initialSnapshot, BOOKING_CHANNEL_ID)
  const airbnbRows = getRows(initialSnapshot, AIRBNB_CHANNEL_ID)
  const bookingSuccessRow = findRequiredRow(
    bookingRows,
    'Booking STD guest 1 success row',
    (row) => row.channelRateId === LOCAL_BOOKING_RATE_PLAN_ID && row.applicableNoOfGuest === '1',
  )
  const bookingFailureRow = findRequiredRow(
    bookingRows,
    'Booking FLEX guest 2 failure row',
    (row) => row.channelRateId === LOCAL_BOOKING_FLEX_RATE_PLAN_ID && row.applicableNoOfGuest === '2',
  )
  const airbnbSuccessRow = findRequiredRow(
    airbnbRows,
    'Airbnb primary listing occupancy 1 success row',
    (row) => row.listingId === LOCAL_AIRBNB_LISTING_ID && row.occupancy === '1',
  )
  const airbnbFailureRow = findRequiredRow(
    airbnbRows,
    'Airbnb second listing occupancy 2 failure row',
    (row) => row.listingId === LOCAL_AIRBNB_SECOND_LISTING_ID && row.occupancy === '2',
  )

  const bookingSuccess = await writeBookingRow(bookingSuccessRow, '1.25', '5')
  assert.equal(bookingSuccess.Status, 'Success')
  assertRowValue(await readState(), bookingSuccessRow.rowKey, '1.25', '5')

  await injectFailure(bookingFailureRow, 'booking-rate-plan-map')
  const bookingInjectedFailure = await writeBookingRow(bookingFailureRow, '1.5', '7')
  assert.equal(bookingInjectedFailure.Status, 'Fail')
  assertRowValue(await readState(), bookingFailureRow.rowKey, '1', '0')

  const bookingRetry = await writeBookingRow(bookingFailureRow, '1.5', '7')
  assert.equal(bookingRetry.Status, 'Success')
  assertRowValue(await readState(), bookingFailureRow.rowKey, '1.5', '7')

  const duplicateMap = await mapAirbnbRow(airbnbSuccessRow, '0.95', '2')
  assert.equal(duplicateMap.Status, 'Fail')
  assert.deepEqual(duplicateMap.Errors, {
    ShortText: 'Room and rateplan combination already mapped',
  })
  assertRowValue(await readState(), airbnbSuccessRow.rowKey, '1', '0')

  const unknownFieldFailure = await updateAirbnbRowWithUnknownField(airbnbSuccessRow)
  assert.equal(unknownFieldFailure.Status, 'Fail')
  assert.equal(unknownFieldFailure.Message, 'Unable to process JSON')
  assertRowValue(await readState(), airbnbSuccessRow.rowKey, '1', '0')

  const incompleteCheckInFailure = await updateAirbnbRowWithIncompleteCheckIn(airbnbSuccessRow)
  assert.equal(incompleteCheckInFailure.Status, 'Fail')
  assert.deepEqual(incompleteCheckInFailure.Errors, {
    ShortText: 'check_in_option.category and check_in_option.instruction are required',
  })
  assertRowValue(await readState(), airbnbSuccessRow.rowKey, '1', '0')

  const airbnbSuccess = await updateAirbnbRow(airbnbSuccessRow, '0.95', '2')
  assert.equal(airbnbSuccess.Status, 'Success')
  assertRowValue(await readState(), airbnbSuccessRow.rowKey, '0.95', '2')

  await injectFailure(airbnbFailureRow, 'airbnb-listing-update')
  const airbnbInjectedFailure = await updateAirbnbRow(airbnbFailureRow, '1.1', '3')
  assert.equal(airbnbInjectedFailure.Status, 'Fail')
  assertRowValue(await readState(), airbnbFailureRow.rowKey, '1', '0')

  const airbnbRetry = await updateAirbnbRow(airbnbFailureRow, '1.1', '3')
  assert.equal(airbnbRetry.Status, 'Success')
  const finalSnapshot = await readState()
  assertRowValue(finalSnapshot, airbnbFailureRow.rowKey, '1.1', '3')
  assert.equal(finalSnapshot.data.failureRules.length, 0)
  assert.equal(
    finalSnapshot.data.writeHistory.some(
      (event) => event.endpoint === 'airbnb-listing-map' && event.outcome === 'success',
    ),
    false,
  )
  assert.equal(
    finalSnapshot.data.writeHistory.some(
      (event) => event.endpoint === 'airbnb-listing-update' && event.outcome === 'success',
    ),
    true,
  )

  await assertLogsContainExpectedPaths()

  // eslint-disable-next-line no-console
  console.log('channel mapping price settings verification passed')
}

main().catch((err) => {
  const message = err instanceof Error ? err.message : String(err)
  // eslint-disable-next-line no-console
  console.error(message)
  process.exitCode = 1
})
