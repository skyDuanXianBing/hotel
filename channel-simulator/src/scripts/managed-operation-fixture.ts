export type ManagedOperationChannel = 'BOOKING' | 'AIRBNB'

export type ManagedOperationOrderRole =
  | 'B1'
  | 'B2'
  | 'A1'
  | 'A2'
  | 'NON_MANAGED'
  | 'CANCELLED'
  | 'MONTH_MISMATCH'

export interface BootstrapRoom {
  id: number
  roomNumber: string | null
  roomTypeId: number | null
  roomTypeName?: string | null
}

export interface ManagedOperationBootstrapContext {
  storeId: number
  suHotelId: string
  roomTypeId: number
  rooms: BootstrapRoom[]
}

export interface ResolvedFixtureRoom {
  id: number
  roomNumber: string
  roomTypeId: number
  managed: boolean
}

export interface ResolvedFixtureOrder {
  role: ManagedOperationOrderRole
  channel: ManagedOperationChannel
  bookingKey: string
  suReservationId: string
  reservationNotifId: string
  cancellationNotifId: string | null
  roomReservationId: string
  room: ResolvedFixtureRoom
  checkInDate: string
  checkOutDate: string
  guestName: string
  listingName: string
  currency: 'JPY'
  grossSales: number
  otaServiceFee: number
  payoutFee: number
  payoutDate: string
  payoutReference: string
  finalStatus: 'CONFIRMED' | 'CANCELLED'
  normalSettlement: boolean
}

export interface PmsReservationVerificationSnapshot {
  id: number
  orderNumber: string | null
  channelOrderNumber: string | null
  externalBookingKey: string | null
  status: string | null
  guestName: string | null
  checkInDate: string | null
  checkOutDate: string | null
  totalAmount: number | string | null
  currencyCode: string | null
  otaRoomNumber: string | null
  channel: { code?: string | null } | null
  room: { id?: number | null; roomNumber?: string | null } | null
}

export interface ManagedOperationExpectedLine {
  role: 'B1' | 'B2' | 'A1' | 'A2'
  platform: ManagedOperationChannel
  bookingKey: string
  roomNumber: string
  checkInDate: string
  checkOutDate: string
  currency: 'JPY'
  grossSales: string
  otaServiceFee: string
  payoutFee: string
  cleaningFeeNet: string
  receivedAmount: string
  managementFee: string
  scheduledTransfer: string
  expectedStatus: 'INCLUDED'
}

export interface ManagedOperationExpectedResult {
  schemaVersion: 1
  sourceManifest: 'fixture-manifest.json'
  settlementMonth: '2026-07'
  expectedIncludedOrderCount: 4
  expectedStatusCounts: {
    INCLUDED: 4
    PERIOD_EXCLUDED: 0
    UNMATCHED: 0
    AMBIGUOUS: 0
    ROOM_EXCLUDED: 0
    CANCELLED: 0
  }
  lines: ManagedOperationExpectedLine[]
  summary: {
    totalReceived: '69408'
    managementFeeNet: '6942'
    cleaningFeeNetUnit: '7273'
    cleaningFeeNetTotal: '29092'
    cleaningTax: '2909'
    managementTax: '694'
    settlementSubtotal: '58863'
    registrationFeeNet: '4000'
    registrationFeeGross: '4400'
    otherDeductionsGross: '1100'
    finalTransfer: '53363'
    invoiceSubtotalNet: '40034'
    invoiceTax: '4003'
    invoiceTotalGross: '44037'
  }
}

export interface FixtureScenario {
  id: string
  airbnbFile: string | null
  bookingFile: string | null
  stampFile?: string | null
  expectedHttpStatus?: 200 | 400
  expectedLineStatus?:
    | 'UNMATCHED'
    | 'AMBIGUOUS'
    | 'ROOM_EXCLUDED'
    | 'CANCELLED'
    | 'PERIOD_EXCLUDED'
  expectedExportAllowed?: boolean
  expectedErrorContains?: string
  browserOnly?: boolean
}

export interface ResolvedManagedOperationManifest {
  schemaVersion: 1
  generatedAt: string
  runPrefix: string
  store: {
    id: number
    suHotelId: string
    roomTypeId: number
  }
  settlement: {
    settlementMonth: '2026-07'
    managementFeeRate: '0.10'
    taxRate: '0.10'
    cleaningFeeGross: '8000'
    cleaningFeeNet: '7273'
    registrationFeeNetPerRoom: '2000'
    otherDeductions: Array<{ description: string; amountGross: '1100' }>
    invoiceNumber: string
    invoiceDate: '2026-08-01'
    paymentDueDate: '2026-08-15'
    receiptNumber: string
    receiptDate: '2026-08-01'
  }
  rooms: {
    managed: ResolvedFixtureRoom[]
    nonManaged: ResolvedFixtureRoom
  }
  orders: ResolvedFixtureOrder[]
  files: {
    normal: {
      airbnb: 'airbnb-normal.csv'
      booking: 'booking-normal.xlsx'
      expected: 'expected-result.json'
    }
    anomaliesDirectory: 'anomalies'
    scenarios: FixtureScenario[]
  }
  expected: ManagedOperationExpectedResult
}

export type WorkbookCell =
  | { type: 'string'; value: string }
  | { type: 'number'; value: string }

export interface WorkbookModel {
  sheetName: string
  rows: WorkbookCell[][]
}

interface OrderTemplate {
  role: ManagedOperationOrderRole
  channel: ManagedOperationChannel
  bookingSuffix: string
  roomNumber: 'E2E-101' | 'E2E-102' | 'E2E-103'
  checkInDate: string
  checkOutDate: string
  grossSales: number
  otaServiceFee: number
  payoutFee: number
  finalStatus: 'CONFIRMED' | 'CANCELLED'
  normalSettlement: boolean
}

const ORDER_TEMPLATES: readonly OrderTemplate[] = [
  {
    role: 'B1',
    channel: 'BOOKING',
    bookingSuffix: '01',
    roomNumber: 'E2E-101',
    checkInDate: '2026-07-17',
    checkOutDate: '2026-07-19',
    grossSales: 30000,
    otaServiceFee: 3600,
    payoutFee: 500,
    finalStatus: 'CONFIRMED',
    normalSettlement: true,
  },
  {
    role: 'B2',
    channel: 'BOOKING',
    bookingSuffix: '02',
    roomNumber: 'E2E-102',
    checkInDate: '2026-07-17',
    checkOutDate: '2026-07-19',
    grossSales: 25000,
    otaServiceFee: 3000,
    payoutFee: 400,
    finalStatus: 'CONFIRMED',
    normalSettlement: true,
  },
  {
    role: 'A1',
    channel: 'AIRBNB',
    bookingSuffix: 'A1',
    roomNumber: 'E2E-101',
    checkInDate: '2026-07-20',
    checkOutDate: '2026-07-22',
    grossSales: 28000,
    otaServiceFee: 4200,
    payoutFee: 0,
    finalStatus: 'CONFIRMED',
    normalSettlement: true,
  },
  {
    role: 'A2',
    channel: 'AIRBNB',
    bookingSuffix: 'A2',
    roomNumber: 'E2E-102',
    checkInDate: '2026-07-20',
    checkOutDate: '2026-07-22',
    grossSales: 32000,
    otaServiceFee: 4800,
    payoutFee: 0,
    finalStatus: 'CONFIRMED',
    normalSettlement: true,
  },
  {
    role: 'NON_MANAGED',
    channel: 'BOOKING',
    bookingSuffix: '03',
    roomNumber: 'E2E-103',
    checkInDate: '2026-07-23',
    checkOutDate: '2026-07-25',
    grossSales: 20000,
    otaServiceFee: 2400,
    payoutFee: 300,
    finalStatus: 'CONFIRMED',
    normalSettlement: false,
  },
  {
    role: 'CANCELLED',
    channel: 'AIRBNB',
    bookingSuffix: 'AC',
    roomNumber: 'E2E-101',
    checkInDate: '2026-07-24',
    checkOutDate: '2026-07-26',
    grossSales: 22000,
    otaServiceFee: 3300,
    payoutFee: 0,
    finalStatus: 'CANCELLED',
    normalSettlement: false,
  },
  {
    role: 'MONTH_MISMATCH',
    channel: 'BOOKING',
    bookingSuffix: '04',
    roomNumber: 'E2E-102',
    checkInDate: '2026-08-04',
    checkOutDate: '2026-08-06',
    grossSales: 18000,
    otaServiceFee: 1800,
    payoutFee: 300,
    finalStatus: 'CONFIRMED',
    normalSettlement: false,
  },
]

const BOOKING_HEADERS = [
  '予約番号',
  'チェックイン日',
  'Checkout',
  '宿泊者氏名',
  '通貨',
  '金額',
  'コミッション',
  '決済サービスの手数料',
  'お支払い日',
  'お支払い番号',
] as const

const AIRBNB_HEADERS = [
  '日付',
  '入金予定日',
  '種別',
  '確認コード',
  '開始日',
  '終了日',
  'ゲスト',
  'リスティング',
  '通貨',
  'サービス料',
  '総収入',
  '参照コード',
] as const

const NORMAL_ROLES = new Set<ManagedOperationOrderRole>(['B1', 'B2', 'A1', 'A2'])

function assertInteger(value: number, field: string): void {
  if (!Number.isSafeInteger(value)) {
    throw new Error(`${field} must be a safe integer`)
  }
}

function roundHalfUpPositive(numerator: number, denominator: number): number {
  assertInteger(numerator, 'numerator')
  assertInteger(denominator, 'denominator')
  if (numerator < 0 || denominator <= 0) {
    throw new Error('roundHalfUpPositive only accepts a non-negative numerator and positive denominator')
  }
  return Math.floor((numerator * 2 + denominator) / (denominator * 2))
}

function requireRunPrefix(raw: string): string {
  const value = String(raw || '').trim()
  if (!/^[1-9]\d{5,11}$/.test(value)) {
    throw new Error(
      'MANAGED_OPERATION_E2E_RUN_PREFIX must contain 6 to 12 ASCII digits and must not start with zero',
    )
  }
  return value
}

function resolveRoom(context: ManagedOperationBootstrapContext, roomNumber: string): ResolvedFixtureRoom {
  const matches = context.rooms.filter((room) => String(room.roomNumber || '').trim() === roomNumber)
  if (matches.length !== 1) {
    throw new Error(`bootstrap must return exactly one ${roomNumber} room; actual=${matches.length}`)
  }
  const room = matches[0]
  if (!Number.isSafeInteger(room.id) || room.id <= 0) {
    throw new Error(`${roomNumber} has an invalid local room id`)
  }
  if (room.roomTypeId !== context.roomTypeId) {
    throw new Error(`${roomNumber} does not belong to bootstrap roomTypeId=${context.roomTypeId}`)
  }
  return {
    id: room.id,
    roomNumber,
    roomTypeId: context.roomTypeId,
    managed: roomNumber !== 'E2E-103',
  }
}

function bookingKey(runPrefix: string, template: OrderTemplate): string {
  if (template.channel === 'BOOKING') {
    return `${runPrefix}${template.bookingSuffix}`
  }
  return `M${runPrefix}${template.bookingSuffix}`
}

function orderSequence(role: ManagedOperationOrderRole): string {
  const index = ORDER_TEMPLATES.findIndex((template) => template.role === role)
  if (index < 0) {
    throw new Error(`unknown fixture role: ${role}`)
  }
  return String(index + 1).padStart(2, '0')
}

export function getOrder(
  manifest: ResolvedManagedOperationManifest,
  role: ManagedOperationOrderRole,
): ResolvedFixtureOrder {
  const result = manifest.orders.find((order) => order.role === role)
  if (!result) {
    throw new Error(`manifest order not found: ${role}`)
  }
  return result
}

export function buildResolvedManifest(
  rawRunPrefix: string,
  context: ManagedOperationBootstrapContext,
  generatedAt = new Date().toISOString(),
): ResolvedManagedOperationManifest {
  const runPrefix = requireRunPrefix(rawRunPrefix)
  if (!Number.isSafeInteger(context.storeId) || context.storeId <= 0) {
    throw new Error('bootstrap returned an invalid storeId')
  }
  if (!String(context.suHotelId || '').trim()) {
    throw new Error('bootstrap returned an empty suHotelId')
  }
  if (!Number.isSafeInteger(context.roomTypeId) || context.roomTypeId <= 0) {
    throw new Error('bootstrap returned an invalid roomTypeId')
  }

  const rooms = new Map<string, ResolvedFixtureRoom>([
    ['E2E-101', resolveRoom(context, 'E2E-101')],
    ['E2E-102', resolveRoom(context, 'E2E-102')],
    ['E2E-103', resolveRoom(context, 'E2E-103')],
  ])
  const orders = ORDER_TEMPLATES.map((template): ResolvedFixtureOrder => {
    const key = bookingKey(runPrefix, template)
    const sequence = orderSequence(template.role)
    const reservationNotifId = `MO-E2E-${runPrefix}-${template.role}-NEW`
    const room = rooms.get(template.roomNumber)
    if (!room) {
      throw new Error(`resolved room not found: ${template.roomNumber}`)
    }
    return {
      role: template.role,
      channel: template.channel,
      bookingKey: key,
      suReservationId: `${key}_${context.suHotelId}`,
      reservationNotifId,
      cancellationNotifId:
        template.finalStatus === 'CANCELLED'
          ? `MO-E2E-${runPrefix}-${template.role}-CANCEL`
          : null,
      roomReservationId: `${runPrefix}${sequence}`,
      room,
      checkInDate: template.checkInDate,
      checkOutDate: template.checkOutDate,
      guestName: `MO E2E ${template.role} ${runPrefix}`,
      listingName: `${template.roomNumber} Managed Operation Fixture`,
      currency: 'JPY',
      grossSales: template.grossSales,
      otaServiceFee: template.otaServiceFee,
      payoutFee: template.payoutFee,
      payoutDate: '2026-08-03',
      payoutReference: `PAY-${runPrefix}-${template.role}`,
      finalStatus: template.finalStatus,
      normalSettlement: template.normalSettlement,
    }
  })

  const keys = new Set(orders.map((order) => order.bookingKey))
  const roomReservationIds = new Set(orders.map((order) => order.roomReservationId))
  const notificationIds = new Set(
    orders.flatMap((order) =>
      order.cancellationNotifId
        ? [order.reservationNotifId, order.cancellationNotifId]
        : [order.reservationNotifId],
    ),
  )
  if (keys.size !== orders.length || roomReservationIds.size !== orders.length) {
    throw new Error('fixture identifiers are not unique')
  }
  const expectedNotificationCount = orders.length + orders.filter((order) => order.cancellationNotifId).length
  if (notificationIds.size !== expectedNotificationCount) {
    throw new Error('fixture notification ids are not unique')
  }
  for (const order of orders) {
    if (order.channel === 'BOOKING' && !/^\d+$/.test(order.bookingKey)) {
      throw new Error(`Booking key must be numeric: ${order.role}`)
    }
    if (order.channel === 'AIRBNB' && !/^[A-Z0-9]+$/.test(order.bookingKey)) {
      throw new Error(`Airbnb key must be uppercase alphanumeric: ${order.role}`)
    }
  }

  const placeholder = {
    schemaVersion: 1 as const,
    sourceManifest: 'fixture-manifest.json' as const,
    settlementMonth: '2026-07' as const,
    expectedIncludedOrderCount: 4 as const,
    expectedStatusCounts: {
      INCLUDED: 4 as const,
      PERIOD_EXCLUDED: 0 as const,
      UNMATCHED: 0 as const,
      AMBIGUOUS: 0 as const,
      ROOM_EXCLUDED: 0 as const,
      CANCELLED: 0 as const,
    },
    lines: [] as ManagedOperationExpectedLine[],
    summary: {
      totalReceived: '69408' as const,
      managementFeeNet: '6942' as const,
      cleaningFeeNetUnit: '7273' as const,
      cleaningFeeNetTotal: '29092' as const,
      cleaningTax: '2909' as const,
      managementTax: '694' as const,
      settlementSubtotal: '58863' as const,
      registrationFeeNet: '4000' as const,
      registrationFeeGross: '4400' as const,
      otherDeductionsGross: '1100' as const,
      finalTransfer: '53363' as const,
      invoiceSubtotalNet: '40034' as const,
      invoiceTax: '4003' as const,
      invoiceTotalGross: '44037' as const,
    },
  }

  const manifest: ResolvedManagedOperationManifest = {
    schemaVersion: 1,
    generatedAt,
    runPrefix,
    store: {
      id: context.storeId,
      suHotelId: context.suHotelId,
      roomTypeId: context.roomTypeId,
    },
    settlement: {
      settlementMonth: '2026-07',
      managementFeeRate: '0.10',
      taxRate: '0.10',
      cleaningFeeGross: '8000',
      cleaningFeeNet: '7273',
      registrationFeeNetPerRoom: '2000',
      otherDeductions: [{ description: 'Local E2E other deduction', amountGross: '1100' }],
      invoiceNumber: `INV-${runPrefix}`,
      invoiceDate: '2026-08-01',
      paymentDueDate: '2026-08-15',
      receiptNumber: `REC-${runPrefix}`,
      receiptDate: '2026-08-01',
    },
    rooms: {
      managed: [rooms.get('E2E-101')!, rooms.get('E2E-102')!],
      nonManaged: rooms.get('E2E-103')!,
    },
    orders,
    files: {
      normal: {
        airbnb: 'airbnb-normal.csv',
        booking: 'booking-normal.xlsx',
        expected: 'expected-result.json',
      },
      anomaliesDirectory: 'anomalies',
      scenarios: buildScenarioMatrix(),
    },
    expected: placeholder,
  }
  manifest.expected = buildExpectedResult(manifest)
  return manifest
}

function buildScenarioMatrix(): FixtureScenario[] {
  return [
    {
      id: 'unmatched',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/booking-unmatched.csv',
      expectedHttpStatus: 200,
      expectedLineStatus: 'UNMATCHED',
      expectedExportAllowed: false,
    },
    {
      id: 'duplicate',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/booking-duplicate.csv',
      expectedHttpStatus: 200,
      expectedLineStatus: 'AMBIGUOUS',
      expectedExportAllowed: false,
    },
    {
      id: 'date-conflict',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/booking-date-conflict.csv',
      expectedHttpStatus: 200,
      expectedLineStatus: 'UNMATCHED',
      expectedExportAllowed: false,
    },
    {
      id: 'non-managed',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/booking-non-managed.csv',
      expectedHttpStatus: 200,
      expectedLineStatus: 'ROOM_EXCLUDED',
      expectedExportAllowed: true,
    },
    {
      id: 'cancelled',
      airbnbFile: 'anomalies/airbnb-cancelled.csv',
      bookingFile: 'booking-normal.xlsx',
      expectedHttpStatus: 200,
      expectedLineStatus: 'CANCELLED',
      expectedExportAllowed: true,
    },
    {
      id: 'month-mismatch',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/booking-month-mismatch.csv',
      expectedHttpStatus: 200,
      expectedLineStatus: 'PERIOD_EXCLUDED',
      expectedExportAllowed: true,
    },
    {
      id: 'non-jpy',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/booking-non-jpy.csv',
      expectedHttpStatus: 400,
      expectedErrorContains: '币种不是 JPY',
    },
    {
      id: 'fractional-yen',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/booking-fractional-yen.csv',
      expectedHttpStatus: 400,
      expectedErrorContains: '必须是整数日元',
    },
    {
      id: 'negative-received',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/booking-negative-received.csv',
      expectedHttpStatus: 400,
      expectedErrorContains: '受取金为负数',
    },
    {
      id: 'missing-file',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: null,
      browserOnly: true,
    },
    {
      id: 'fake-xlsx',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/fake-xlsx.xlsx',
      expectedHttpStatus: 400,
      expectedErrorContains: 'XLSX',
    },
    {
      id: 'empty-file',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/empty.csv',
      expectedHttpStatus: 400,
      expectedErrorContains: '不能为空',
    },
    {
      id: 'over-7mb',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/over-7mb.csv',
      expectedHttpStatus: 400,
      expectedErrorContains: '7MB',
    },
    {
      id: 'invalid-format',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'anomalies/invalid-format.txt',
      expectedHttpStatus: 400,
      expectedErrorContains: '仅支持 CSV、XLS、XLSX',
    },
    {
      id: 'invalid-stamp',
      airbnbFile: null,
      bookingFile: null,
      stampFile: 'anomalies/invalid-stamp.png',
      expectedHttpStatus: 400,
    },
    {
      id: 'unsaved-settings',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'booking-normal.xlsx',
      browserOnly: true,
    },
    {
      id: 'stale-preview',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'booking-normal.xlsx',
      browserOnly: true,
    },
    {
      id: 'missing-invoice-number',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'booking-normal.xlsx',
      expectedHttpStatus: 400,
      expectedErrorContains: '请款书日期和编号',
    },
    {
      id: 'missing-receipt-number',
      airbnbFile: 'airbnb-normal.csv',
      bookingFile: 'booking-normal.xlsx',
      expectedHttpStatus: 400,
      expectedErrorContains: '收据日期和编号',
    },
  ]
}

export function buildExpectedResult(
  manifest: Pick<ResolvedManagedOperationManifest, 'orders'>,
): ManagedOperationExpectedResult {
  const normal = manifest.orders.filter((order) => NORMAL_ROLES.has(order.role))
  if (normal.length !== 4) {
    throw new Error(`normal settlement must contain 4 orders; actual=${normal.length}`)
  }

  const cleaningFeeNet = roundHalfUpPositive(8000 * 10, 11)
  const lines = normal.map((order): ManagedOperationExpectedLine => {
    const received =
      order.grossSales - order.otaServiceFee - order.payoutFee - cleaningFeeNet
    const managementFee = roundHalfUpPositive(received, 10)
    const scheduledTransfer = roundHalfUpPositive(received * 9, 10)
    return {
      role: order.role as 'B1' | 'B2' | 'A1' | 'A2',
      platform: order.channel,
      bookingKey: order.bookingKey,
      roomNumber: order.room.roomNumber,
      checkInDate: order.checkInDate,
      checkOutDate: order.checkOutDate,
      currency: order.currency,
      grossSales: String(order.grossSales),
      otaServiceFee: String(order.otaServiceFee),
      payoutFee: String(order.payoutFee),
      cleaningFeeNet: String(cleaningFeeNet),
      receivedAmount: String(received),
      managementFee: String(managementFee),
      scheduledTransfer: String(scheduledTransfer),
      expectedStatus: 'INCLUDED',
    }
  })

  const totalReceived = lines.reduce((sum, line) => sum + Number(line.receivedAmount), 0)
  const managementFeeNet = lines.reduce((sum, line) => sum + Number(line.managementFee), 0)
  const cleaningFeeNetTotal = cleaningFeeNet * lines.length
  const cleaningTax = roundHalfUpPositive(cleaningFeeNetTotal, 10)
  const managementTax = roundHalfUpPositive(managementFeeNet, 10)
  const settlementSubtotal = totalReceived - managementFeeNet - cleaningTax - managementTax
  const registrationFeeNet = 2 * 2000
  const registrationFeeGross = roundHalfUpPositive(registrationFeeNet * 11, 10)
  const otherDeductionsGross = 1100
  const finalTransfer = settlementSubtotal - registrationFeeGross - otherDeductionsGross
  const invoiceSubtotalNet = managementFeeNet + cleaningFeeNetTotal + registrationFeeNet
  const invoiceTax = Math.floor(invoiceSubtotalNet / 10)
  const invoiceTotalGross = invoiceSubtotalNet + invoiceTax

  const actual = {
    totalReceived: String(totalReceived),
    managementFeeNet: String(managementFeeNet),
    cleaningFeeNetUnit: String(cleaningFeeNet),
    cleaningFeeNetTotal: String(cleaningFeeNetTotal),
    cleaningTax: String(cleaningTax),
    managementTax: String(managementTax),
    settlementSubtotal: String(settlementSubtotal),
    registrationFeeNet: String(registrationFeeNet),
    registrationFeeGross: String(registrationFeeGross),
    otherDeductionsGross: String(otherDeductionsGross),
    finalTransfer: String(finalTransfer),
    invoiceSubtotalNet: String(invoiceSubtotalNet),
    invoiceTax: String(invoiceTax),
    invoiceTotalGross: String(invoiceTotalGross),
  }
  const locked = {
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
  } as const
  for (const [field, expected] of Object.entries(locked)) {
    if (actual[field as keyof typeof actual] !== expected) {
      throw new Error(
        `fixture formula drift: ${field} expected=${expected} actual=${actual[field as keyof typeof actual]}`,
      )
    }
  }

  return {
    schemaVersion: 1,
    sourceManifest: 'fixture-manifest.json',
    settlementMonth: '2026-07',
    expectedIncludedOrderCount: 4,
    expectedStatusCounts: {
      INCLUDED: 4,
      PERIOD_EXCLUDED: 0,
      UNMATCHED: 0,
      AMBIGUOUS: 0,
      ROOM_EXCLUDED: 0,
      CANCELLED: 0,
    },
    lines,
    summary: locked,
  }
}

export function excelSerial(dateText: string): string {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(dateText)) {
    throw new Error(`invalid ISO date: ${dateText}`)
  }
  const [year, month, day] = dateText.split('-').map(Number)
  const instant = Date.UTC(year, month - 1, day)
  const parsed = new Date(instant)
  if (
    parsed.getUTCFullYear() !== year ||
    parsed.getUTCMonth() !== month - 1 ||
    parsed.getUTCDate() !== day
  ) {
    throw new Error(`invalid calendar date: ${dateText}`)
  }
  const epoch = Date.UTC(1899, 11, 30)
  return String(Math.floor((instant - epoch) / 86400000))
}

function bookingOrders(manifest: ResolvedManagedOperationManifest): ResolvedFixtureOrder[] {
  return manifest.orders.filter(
    (order) => order.normalSettlement && order.channel === 'BOOKING',
  )
}

function airbnbOrders(manifest: ResolvedManagedOperationManifest): ResolvedFixtureOrder[] {
  return manifest.orders.filter(
    (order) => order.normalSettlement && order.channel === 'AIRBNB',
  )
}

export function buildBookingWorkbookModel(
  manifest: ResolvedManagedOperationManifest,
): WorkbookModel {
  const rows: WorkbookCell[][] = [
    BOOKING_HEADERS.map((value) => ({ type: 'string' as const, value })),
  ]
  for (const order of bookingOrders(manifest)) {
    rows.push([
      { type: 'number', value: order.bookingKey },
      { type: 'number', value: excelSerial(order.checkInDate) },
      { type: 'number', value: excelSerial(order.checkOutDate) },
      { type: 'string', value: order.guestName },
      { type: 'string', value: order.currency },
      { type: 'number', value: String(order.grossSales) },
      { type: 'number', value: String(-order.otaServiceFee) },
      { type: 'number', value: String(-order.payoutFee) },
      { type: 'number', value: excelSerial(order.payoutDate) },
      { type: 'string', value: order.payoutReference },
    ])
  }
  return { sheetName: 'Booking', rows }
}

function csvCell(value: unknown): string {
  const text = String(value ?? '')
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

function renderCsv(rows: readonly (readonly unknown[])[], bom = false): string {
  const body = rows.map((row) => row.map(csvCell).join(',')).join('\r\n') + '\r\n'
  return bom ? `\ufeff${body}` : body
}

interface BookingRowOverride {
  bookingKey?: string
  checkInDate?: string
  checkOutDate?: string
  guestName?: string
  currency?: string
  grossSales?: string
  otaServiceFee?: string
  payoutFee?: string
}

function bookingCsvRow(order: ResolvedFixtureOrder, override: BookingRowOverride = {}): string[] {
  return [
    override.bookingKey ?? order.bookingKey,
    override.checkInDate ?? order.checkInDate,
    override.checkOutDate ?? order.checkOutDate,
    override.guestName ?? order.guestName,
    override.currency ?? order.currency,
    override.grossSales ?? String(order.grossSales),
    override.otaServiceFee ?? String(-order.otaServiceFee),
    override.payoutFee ?? String(-order.payoutFee),
    order.payoutDate,
    order.payoutReference,
  ]
}

function renderBookingCsv(rows: string[][]): string {
  return renderCsv([BOOKING_HEADERS, ...rows], true)
}

function airbnbRows(order: ResolvedFixtureOrder): string[][] {
  return [
    [
      order.checkOutDate,
      order.payoutDate,
      'Payout',
      '',
      '',
      '',
      '',
      '',
      '',
      '',
      '',
      order.payoutReference,
    ],
    [
      order.checkOutDate,
      '',
      '予約',
      order.bookingKey,
      order.checkInDate,
      order.checkOutDate,
      order.guestName,
      order.listingName,
      order.currency,
      String(-order.otaServiceFee),
      String(order.grossSales),
      '',
    ],
  ]
}

export function renderNormalAirbnbCsv(manifest: ResolvedManagedOperationManifest): string {
  return renderCsv([AIRBNB_HEADERS, ...airbnbOrders(manifest).flatMap(airbnbRows)], true)
}

export function buildAnomalyArtifacts(
  manifest: ResolvedManagedOperationManifest,
): Record<string, string | Buffer> {
  const b1 = getOrder(manifest, 'B1')
  const nonManaged = getOrder(manifest, 'NON_MANAGED')
  const cancelled = getOrder(manifest, 'CANCELLED')
  const monthMismatch = getOrder(manifest, 'MONTH_MISMATCH')
  const unmatchedKey = `${manifest.runPrefix}99`
  const invalidFormatBody = renderBookingCsv([bookingCsvRow(b1)])
  const overLimitSize = 7 * 1024 * 1024 + 1
  const overLimit = Buffer.alloc(overLimitSize, 'X')
  const overLimitPrefix = Buffer.from(
    renderBookingCsv([
      bookingCsvRow(b1, {
        bookingKey: unmatchedKey,
      }),
    ]),
    'utf8',
  )
  overLimitPrefix.copy(overLimit, 0, 0, Math.min(overLimitPrefix.length, overLimit.length))

  return {
    'booking-unmatched.csv': renderBookingCsv([
      bookingCsvRow(b1, { bookingKey: unmatchedKey }),
    ]),
    'booking-duplicate.csv': renderBookingCsv([
      bookingCsvRow(b1),
      bookingCsvRow(b1),
    ]),
    'booking-date-conflict.csv': renderBookingCsv([
      bookingCsvRow(b1, { checkInDate: '2026-07-18' }),
    ]),
    'booking-non-managed.csv': renderBookingCsv([bookingCsvRow(nonManaged)]),
    'airbnb-cancelled.csv': renderCsv(
      [AIRBNB_HEADERS, ...airbnbRows(cancelled)],
      true,
    ),
    'booking-month-mismatch.csv': renderBookingCsv([bookingCsvRow(monthMismatch)]),
    'booking-non-jpy.csv': renderBookingCsv([
      bookingCsvRow(b1, { currency: 'USD' }),
    ]),
    'booking-fractional-yen.csv': renderBookingCsv([
      bookingCsvRow(b1, { grossSales: '30000.50' }),
    ]),
    'booking-negative-received.csv': renderBookingCsv([
      bookingCsvRow(b1, {
        grossSales: '7000',
        otaServiceFee: '-1000',
        payoutFee: '0',
      }),
    ]),
    'fake-xlsx.xlsx': `NOT-AN-XLSX:${manifest.runPrefix}\n`,
    'empty.csv': Buffer.alloc(0),
    'over-7mb.csv': overLimit,
    'invalid-format.txt': invalidFormatBody,
    'invalid-stamp.png': `NOT-A-PNG:${manifest.runPrefix}\n`,
  }
}

function daysBetween(start: string, end: string): number {
  const startMs = Date.parse(`${start}T00:00:00Z`)
  const endMs = Date.parse(`${end}T00:00:00Z`)
  const result = Math.floor((endMs - startMs) / 86400000)
  if (!Number.isSafeInteger(result) || result <= 0) {
    throw new Error(`invalid stay range: ${start} to ${end}`)
  }
  return result
}

function addIsoDays(start: string, days: number): string {
  const result = new Date(Date.parse(`${start}T00:00:00Z`) + days * 86400000)
  return result.toISOString().slice(0, 10)
}

function buildPriceRows(order: ResolvedFixtureOrder): Record<string, unknown>[] {
  const nights = daysBetween(order.checkInDate, order.checkOutDate)
  const base = Math.floor(order.grossSales / nights)
  let remaining = order.grossSales
  const rows: Record<string, unknown>[] = []
  for (let index = 0; index < nights; index += 1) {
    const amount = index === nights - 1 ? remaining : base
    remaining -= amount
    rows.push({
      date: addIsoDays(order.checkInDate, index),
      rate_id: 'E2E-BAR',
      mealplan_id: 'E2E-BAR',
      mealplan: 'Room only',
      tax: '0',
      pricebeforetax: String(amount),
      priceaftertax: String(amount),
    })
  }
  return rows
}

export function buildSuPushPayload(
  manifest: ResolvedManagedOperationManifest,
  order: ResolvedFixtureOrder,
  phase: 'new' | 'cancel' = 'new',
): Record<string, unknown> {
  if (phase === 'cancel' && !order.cancellationNotifId) {
    throw new Error(`order ${order.role} does not support cancellation phase`)
  }
  const cancelled = phase === 'cancel'
  const reservationNotifId = cancelled
    ? order.cancellationNotifId!
    : order.reservationNotifId
  const roomPayloadId = `${manifest.store.roomTypeId}-${order.room.roomNumber}`
  const names = order.guestName.split(' ')
  const firstName = names.slice(0, -1).join(' ') || order.guestName
  const lastName = names.at(-1) || 'Guest'
  const room = {
    arrival_date: order.checkInDate,
    departure_date: order.checkOutDate,
    info: `${order.channel} managed-operation local E2E fixture`,
    facilities: 'WiFi',
    taxes: [],
    withheldtaxes: [],
    specialrequest: `Managed operation fixture ${manifest.runPrefix}/${order.role}`,
    eta: '15:00',
    guest_name: order.guestName,
    first_name: firstName,
    last_name: lastName,
    id: roomPayloadId,
    max_children: '0',
    numberofguests: '2',
    numberofchildren: '0',
    numberofadults: '2',
    child_age: '',
    roomstaystatus: cancelled ? 'cancelled' : 'new',
    roomreservation_id: order.roomReservationId,
    totalbeforetax: String(order.grossSales),
    totaltax: '0',
    totalprice: String(order.grossSales),
    price: buildPriceRows(order),
    adults: [],
    addons: [],
    extracomponents: [],
    bed_type: 'Double',
    numberofbeds: '1',
    channel_room_id: roomPayloadId,
    smoking_preference: 'Non-Smoking',
    booking_condition: 'Flexible',
    promotion: '',
    mealplan: 'Room only',
    cancellation_penalties: [],
  }
  const reservation = {
    booked_at: '2026-07-01',
    commissionamount: String(order.channel === 'BOOKING' ? order.otaServiceFee : 0),
    currencycode: order.currency,
    paymenttype: order.channel === 'AIRBNB' ? 'Channel Collect' : 'Hotel Collect',
    hotel_id: manifest.store.suHotelId,
    hotel_name: 'Local Channel E2E Hotel',
    paymentdue: '',
    customer: {
      corporate_booking_detail: {},
      address: '1-1 Local E2E Street',
      city: 'Tokyo',
      state: '',
      countrycode: 'JP',
      email: `managed-operation-${manifest.runPrefix}-${order.role.toLowerCase()}@example.test`,
      first_name: firstName,
      last_name: lastName,
      remarks: `Synthetic local fixture ${manifest.runPrefix}/${order.role}`,
      telephone: '+81-90-0000-0000',
      zip: '100-0001',
      guest_lang: order.channel === 'BOOKING' ? 'ja' : 'en',
      cc_virtual: '0',
    },
    rooms: [room],
    affiliation:
      order.channel === 'AIRBNB'
        ? { pos: 'Airbnb', source: 'airbnb', OTA_Code: '244', companyname: '' }
        : { pos: 'Booking.com', source: 'booking.com', OTA_Code: '19', companyname: '' },
    chain_id: '',
    external_id: '',
    otadue: '',
    cancelreason: cancelled ? 'Managed operation local E2E cancellation' : '',
    sellamount: String(order.grossSales),
    nettamount: String(order.grossSales),
    discount: '',
    confirmationlink: '',
    payment_charge: '',
    booker_genius: 'no',
    smoking_preference: '',
    promotion: '',
    channel_booking_id: order.bookingKey,
    thread_id: order.channel === 'AIRBNB' ? `MO-${manifest.runPrefix}-${order.role}` : '',
    guest_id: order.channel === 'AIRBNB' ? `G-${manifest.runPrefix}-${order.role}` : '',
    numberofpets: '0',
    numberofinfants: '0',
    listingbaseprice: String(order.grossSales),
    processed_at: cancelled ? '2026-07-16 12:00:00' : '2026-07-16 10:00:00',
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
    id: order.suReservationId,
    reservation_notif_id: reservationNotifId,
    modified_at: '2026-07-16',
    status: cancelled ? 'cancelled' : 'new',
    totalprice: String(order.grossSales),
    totaltax: '0',
    cancelamount: cancelled ? String(order.grossSales) : '',
    extrafees: [],
    taxes: [],
  }
  return {
    hotelid: manifest.store.suHotelId,
    reservations: [reservation],
  }
}

export function reservationVerificationFailures(
  reservation: PmsReservationVerificationSnapshot,
  order: ResolvedFixtureOrder,
  expectedStatus: 'CONFIRMED' | 'CANCELLED',
): string[] {
  const failures: string[] = []
  if (reservation.channelOrderNumber !== order.bookingKey) failures.push('channelOrderNumber')
  if (reservation.externalBookingKey !== order.bookingKey) failures.push('externalBookingKey')
  if (reservation.checkInDate !== order.checkInDate) failures.push('checkInDate')
  if (reservation.checkOutDate !== order.checkOutDate) failures.push('checkOutDate')
  if (reservation.guestName !== order.guestName) failures.push('guestName')
  if (String(reservation.currencyCode || '').toUpperCase() !== order.currency) failures.push('currencyCode')
  if (String(reservation.channel?.code || '').toUpperCase() !== order.channel) failures.push('channel')
  if (String(reservation.otaRoomNumber || '') !== order.room.roomNumber) failures.push('otaRoomNumber')

  const roomIdentityCleared =
    reservation.room == null ||
    (reservation.room.id == null && reservation.room.roomNumber == null)
  if (expectedStatus === 'CONFIRMED' || !roomIdentityCleared) {
    if (reservation.room?.id !== order.room.id) failures.push('room.id')
    if (reservation.room?.roomNumber !== order.room.roomNumber) failures.push('room.roomNumber')
  }

  if (String(reservation.status || '').toUpperCase() !== expectedStatus) failures.push('status')
  if (Number(reservation.totalAmount) !== order.grossSales) failures.push('totalAmount')
  return failures
}

export function normalBookingHeaders(): readonly string[] {
  return BOOKING_HEADERS
}

export function normalAirbnbHeaders(): readonly string[] {
  return AIRBNB_HEADERS
}
