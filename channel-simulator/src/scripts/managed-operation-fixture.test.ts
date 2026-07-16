import assert from 'node:assert/strict'
import test from 'node:test'

import {
  buildAnomalyArtifacts,
  buildBookingWorkbookModel,
  buildResolvedManifest,
  buildSuPushPayload,
  excelSerial,
  getOrder,
  normalBookingHeaders,
  renderNormalAirbnbCsv,
  reservationVerificationFailures,
  type ManagedOperationBootstrapContext,
} from './managed-operation-fixture'

const context: ManagedOperationBootstrapContext = {
  storeId: 41,
  suHotelId: 'LOCALE2EHOTEL',
  roomTypeId: 73,
  rooms: [
    { id: 101, roomNumber: 'E2E-101', roomTypeId: 73 },
    { id: 102, roomNumber: 'E2E-102', roomTypeId: 73 },
    { id: 103, roomNumber: 'E2E-103', roomTypeId: 73 },
  ],
}

const manifest = buildResolvedManifest('260716123456', context, '2026-07-16T03:00:00.000Z')

test('resolved manifest keeps all identifiers isolated, channel-safe, and room-exact', () => {
  assert.equal(manifest.runPrefix, '260716123456')
  assert.deepEqual(
    manifest.rooms.managed.map((room) => room.roomNumber),
    ['E2E-101', 'E2E-102'],
  )
  assert.equal(manifest.rooms.nonManaged.roomNumber, 'E2E-103')
  assert.equal(manifest.rooms.nonManaged.managed, false)

  const bookingOrders = manifest.orders.filter((order) => order.channel === 'BOOKING')
  const airbnbOrders = manifest.orders.filter((order) => order.channel === 'AIRBNB')
  assert.equal(bookingOrders.every((order) => /^\d+$/.test(order.bookingKey)), true)
  assert.equal(airbnbOrders.every((order) => /^[A-Z0-9]+$/.test(order.bookingKey)), true)
  assert.equal(new Set(manifest.orders.map((order) => order.bookingKey)).size, 7)
  assert.equal(new Set(manifest.orders.map((order) => order.roomReservationId)).size, 7)
  assert.equal(JSON.stringify(manifest).includes('token'), false)
  assert.equal(JSON.stringify(manifest).includes('testSupportKey'), false)
  assert.throws(
    () => buildResolvedManifest('000001', context),
    /must not start with zero/,
  )
})

test('expected result is calculated from the four normal manifest orders and locks Spec totals', () => {
  assert.deepEqual(
    manifest.expected.lines.map((line) => ({
      role: line.role,
      received: line.receivedAmount,
      management: line.managementFee,
      transfer: line.scheduledTransfer,
    })),
    [
      { role: 'B1', received: '18627', management: '1863', transfer: '16764' },
      { role: 'B2', received: '14327', management: '1433', transfer: '12894' },
      { role: 'A1', received: '16527', management: '1653', transfer: '14874' },
      { role: 'A2', received: '19927', management: '1993', transfer: '17934' },
    ],
  )
  assert.deepEqual(manifest.expected.summary, {
    totalReceived: '69408',
    managementFeeNet: '6942',
    cleaningFeeNetUnit: '7273',
    cleaningFeeNetTotal: '29092',
    cleaningTax: '2909',
    managementTax: '694',
    settlementSubtotal: '58863',
    registrationFeeNet: '4000',
    registrationFeeGross: '4400',
    otherDeductionsGross: '1100',
    finalTransfer: '53363',
    invoiceSubtotalNet: '40034',
    invoiceTax: '4003',
    invoiceTotalGross: '44037',
  })
})

test('Booking workbook model uses General numeric cells for reservation keys and Excel dates', () => {
  const workbook = buildBookingWorkbookModel(manifest)
  assert.equal(workbook.sheetName, 'Booking')
  assert.equal(workbook.rows.length, 3)
  assert.deepEqual(
    workbook.rows[0].map((cell) => cell.value),
    [...normalBookingHeaders()],
  )
  assert.deepEqual(workbook.rows[1].slice(0, 3), [
    { type: 'number', value: getOrder(manifest, 'B1').bookingKey },
    { type: 'number', value: '46220' },
    { type: 'number', value: '46222' },
  ])
  assert.deepEqual(workbook.rows[1][8], { type: 'number', value: '46237' })
  assert.equal(excelSerial('2026-07-17'), '46220')
  assert.throws(() => excelSerial('2026-02-30'), /invalid calendar date/)
})

test('Airbnb CSV and every anomaly are derived from resolved manifest values', () => {
  const csv = renderNormalAirbnbCsv(manifest)
  assert.equal(csv.startsWith('\ufeff'), true)
  assert.match(csv, new RegExp(getOrder(manifest, 'A1').bookingKey))
  assert.match(csv, new RegExp(getOrder(manifest, 'A2').bookingKey))
  assert.equal((csv.match(/Payout/g) || []).length, 2)

  const artifacts = buildAnomalyArtifacts(manifest)
  assert.deepEqual(Object.keys(artifacts).sort(), [
    'airbnb-cancelled.csv',
    'booking-date-conflict.csv',
    'booking-duplicate.csv',
    'booking-fractional-yen.csv',
    'booking-month-mismatch.csv',
    'booking-negative-received.csv',
    'booking-non-jpy.csv',
    'booking-non-managed.csv',
    'booking-unmatched.csv',
    'empty.csv',
    'fake-xlsx.xlsx',
    'invalid-format.txt',
    'invalid-stamp.png',
    'over-7mb.csv',
  ])
  assert.match(String(artifacts['booking-duplicate.csv']), new RegExp(getOrder(manifest, 'B1').bookingKey))
  assert.match(String(artifacts['booking-non-managed.csv']), new RegExp(getOrder(manifest, 'NON_MANAGED').bookingKey))
  assert.match(String(artifacts['airbnb-cancelled.csv']), new RegExp(getOrder(manifest, 'CANCELLED').bookingKey))
  const monthMismatch = getOrder(manifest, 'MONTH_MISMATCH')
  assert.equal(monthMismatch.checkInDate, '2026-08-04')
  assert.equal(monthMismatch.checkOutDate, '2026-08-06')
  assert.match(String(artifacts['booking-month-mismatch.csv']), /2026-08-04/)
  assert.match(String(artifacts['booking-month-mismatch.csv']), /2026-08-06/)
  assert.equal(Buffer.byteLength(artifacts['empty.csv']), 0)
  assert.equal(Buffer.byteLength(artifacts['over-7mb.csv']), 7 * 1024 * 1024 + 1)
  assert.match(String(artifacts['fake-xlsx.xlsx']), new RegExp(manifest.runPrefix))
  assert.match(String(artifacts['invalid-stamp.png']), new RegExp(manifest.runPrefix))
})

test('Su PUSH payload is complete, uses exact local room identity, and cancellation reuses the order', () => {
  const b1 = getOrder(manifest, 'B1')
  const b1Payload = buildSuPushPayload(manifest, b1)
  const b1Reservation = (b1Payload.reservations as Array<Record<string, any>>)[0]
  const b1Room = b1Reservation.rooms[0]
  assert.equal(b1Payload.hotelid, manifest.store.suHotelId)
  assert.equal(b1Reservation.hotel_id, manifest.store.suHotelId)
  assert.equal(b1Reservation.affiliation.OTA_Code, '19')
  assert.equal(b1Reservation.channel_booking_id, b1.bookingKey)
  assert.equal(b1Reservation.currencycode, 'JPY')
  assert.equal(b1Room.id, `${manifest.store.roomTypeId}-E2E-101`)
  assert.equal(b1Room.roomreservation_id, b1.roomReservationId)
  assert.equal(b1Room.arrival_date, b1.checkInDate)
  assert.equal(b1Room.departure_date, b1.checkOutDate)
  assert.equal(
    b1Room.price.reduce((sum: number, row: Record<string, string>) => sum + Number(row.priceaftertax), 0),
    b1.grossSales,
  )

  const cancelled = getOrder(manifest, 'CANCELLED')
  const initial = (buildSuPushPayload(manifest, cancelled, 'new').reservations as Array<Record<string, any>>)[0]
  const cancellation = (buildSuPushPayload(manifest, cancelled, 'cancel').reservations as Array<Record<string, any>>)[0]
  assert.equal(cancelled.checkInDate, '2026-07-24')
  assert.equal(cancelled.checkOutDate, '2026-07-26')
  assert.equal(initial.rooms[0].arrival_date, cancelled.checkInDate)
  assert.equal(initial.rooms[0].departure_date, cancelled.checkOutDate)
  assert.equal(cancellation.rooms[0].arrival_date, cancelled.checkInDate)
  assert.equal(cancellation.rooms[0].departure_date, cancelled.checkOutDate)
  assert.equal(initial.id, cancellation.id)
  assert.equal(initial.channel_booking_id, cancellation.channel_booking_id)
  assert.equal(initial.rooms[0].roomreservation_id, cancellation.rooms[0].roomreservation_id)
  assert.notEqual(initial.reservation_notif_id, cancellation.reservation_notif_id)
  assert.equal(initial.status, 'new')
  assert.equal(cancellation.status, 'cancelled')
  assert.equal(cancellation.rooms[0].roomstaystatus, 'cancelled')

  const monthMismatch = getOrder(manifest, 'MONTH_MISMATCH')
  const monthMismatchPayload = (
    buildSuPushPayload(manifest, monthMismatch).reservations as Array<Record<string, any>>
  )[0]
  assert.equal(monthMismatchPayload.rooms[0].arrival_date, monthMismatch.checkInDate)
  assert.equal(monthMismatchPayload.rooms[0].departure_date, monthMismatch.checkOutDate)
})

test('scenario matrix covers every required browser and upload failure family', () => {
  const ids = new Set(manifest.files.scenarios.map((scenario) => scenario.id))
  for (const required of [
    'unmatched',
    'duplicate',
    'date-conflict',
    'non-managed',
    'cancelled',
    'month-mismatch',
    'non-jpy',
    'fractional-yen',
    'negative-received',
    'missing-file',
    'fake-xlsx',
    'empty-file',
    'over-7mb',
    'invalid-format',
    'invalid-stamp',
    'unsaved-settings',
    'stale-preview',
    'missing-invoice-number',
    'missing-receipt-number',
  ]) {
    assert.equal(ids.has(required), true, `missing scenario ${required}`)
  }
})

test('PMS verification requires a room for confirmed orders but accepts cleared room on cancellation', () => {
  const order = getOrder(manifest, 'CANCELLED')
  const base = {
    id: 9001,
    orderNumber: `SU41-${order.bookingKey}`,
    channelOrderNumber: order.bookingKey,
    externalBookingKey: order.bookingKey,
    status: 'CONFIRMED',
    guestName: order.guestName,
    checkInDate: order.checkInDate,
    checkOutDate: order.checkOutDate,
    totalAmount: String(order.grossSales),
    currencyCode: order.currency,
    otaRoomNumber: order.room.roomNumber,
    channel: { code: order.channel },
    room: { id: order.room.id, roomNumber: order.room.roomNumber },
  }

  assert.deepEqual(reservationVerificationFailures(base, order, 'CONFIRMED'), [])
  assert.deepEqual(
    reservationVerificationFailures({ ...base, room: null }, order, 'CONFIRMED'),
    ['room.id', 'room.roomNumber'],
  )
  assert.deepEqual(
    reservationVerificationFailures(
      { ...base, status: 'CANCELLED', room: null },
      order,
      'CANCELLED',
    ),
    [],
  )
  assert.deepEqual(
    reservationVerificationFailures(
      { ...base, status: 'CANCELLED', room: {} },
      order,
      'CANCELLED',
    ),
    [],
  )
  assert.deepEqual(
    reservationVerificationFailures(
      { ...base, status: 'CANCELLED', room: { id: null, roomNumber: null } },
      order,
      'CANCELLED',
    ),
    [],
  )
  assert.deepEqual(
    reservationVerificationFailures(
      {
        ...base,
        status: 'CANCELLED',
        room: { id: order.room.id, roomNumber: null },
      },
      order,
      'CANCELLED',
    ),
    ['room.roomNumber'],
  )
  assert.deepEqual(
    reservationVerificationFailures(
      {
        ...base,
        status: 'CANCELLED',
        room: { id: null, roomNumber: order.room.roomNumber },
      },
      order,
      'CANCELLED',
    ),
    ['room.id'],
  )
  assert.deepEqual(
    reservationVerificationFailures(
      {
        ...base,
        status: 'CANCELLED',
        room: { id: 9999, roomNumber: 'E2E-WRONG' },
      },
      order,
      'CANCELLED',
    ),
    ['room.id', 'room.roomNumber'],
  )
  assert.deepEqual(
    reservationVerificationFailures(
      {
        ...base,
        status: 'CANCELLED',
        room: null,
        otaRoomNumber: 'E2E-WRONG',
      },
      order,
      'CANCELLED',
    ),
    ['otaRoomNumber'],
  )
})
