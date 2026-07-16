import axios from 'axios'
import { spawn } from 'node:child_process'
import { constants as fsConstants } from 'node:fs'
import { access, mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'

import {
  buildAnomalyArtifacts,
  buildBookingWorkbookModel,
  buildResolvedManifest,
  buildSuPushPayload,
  renderNormalAirbnbCsv,
  reservationVerificationFailures,
  type BootstrapRoom,
  type ManagedOperationBootstrapContext,
  type PmsReservationVerificationSnapshot,
  type ResolvedFixtureOrder,
  type ResolvedManagedOperationManifest,
  type WorkbookModel,
} from './managed-operation-fixture'
import { redactSensitiveText } from './outputRedaction'

interface RuntimeConfig {
  pmsBaseUrl: string
  simulatorBaseUrl: string
  testSupportKey: string
  outputDirectory: string
  runPrefix: string
  pythonCommand: string
  allowOverwrite: boolean
}

interface BootstrapSession {
  token: string
  manifestContext: ManagedOperationBootstrapContext
}

interface SendSummary {
  role: string
  phase: 'new' | 'cancel'
  channel: string
  bookingKey: string
  reservationNotifId: string
  hotelId: string
  roomNumber: string
  roomPayloadId: string
  checkInDate: string
  checkOutDate: string
  currency: string
  payloadStatus: string
  simulatorStatusCode: number
  pmsStatusCode: number
  acknowledged: boolean
  lookupReservationId: number
  lookupOrderNumber: string | null
  lookupStatus: string | null
  verified: true
}

const LOOKUP_PATH = '/api/v1/test-support/channel-e2e/reservations/lookup'
const BOOTSTRAP_PATH = '/api/e2e/bootstrap'
const SEND_PATH = '/api/webhooks/reservation/send'

function trimBaseUrl(value: string): string {
  return value.replace(/\/+$/, '')
}

function assertLoopbackUrl(raw: string, field: string): string {
  let parsed: URL
  try {
    parsed = new URL(raw)
  } catch {
    throw new Error(`${field} must be a valid URL`)
  }
  const hostname = parsed.hostname.toLowerCase()
  if (!['localhost', '127.0.0.1', '::1'].includes(hostname)) {
    throw new Error(`${field} must point to localhost; external endpoints are forbidden`)
  }
  if (!['http:', 'https:'].includes(parsed.protocol)) {
    throw new Error(`${field} must use http or https`)
  }
  return trimBaseUrl(parsed.toString())
}

function defaultRunPrefix(now = new Date()): string {
  const values = [
    String(now.getFullYear()).slice(-2),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
    String(now.getHours()).padStart(2, '0'),
    String(now.getMinutes()).padStart(2, '0'),
    String(now.getSeconds()).padStart(2, '0'),
  ]
  return values.join('')
}

function readRuntimeConfig(): RuntimeConfig {
  const outputDirectoryRaw = String(process.env.MANAGED_OPERATION_E2E_OUTPUT_DIR || '').trim()
  if (!outputDirectoryRaw) {
    throw new Error('MANAGED_OPERATION_E2E_OUTPUT_DIR is required')
  }
  const testSupportKey = String(
    process.env.CHANNEL_E2E_TEST_SUPPORT_KEY || process.env.TEST_SUPPORT_KEY || '',
  ).trim()
  if (!testSupportKey) {
    throw new Error('CHANNEL_E2E_TEST_SUPPORT_KEY is required')
  }
  const runPrefix = String(
    process.env.MANAGED_OPERATION_E2E_RUN_PREFIX || defaultRunPrefix(),
  ).trim()
  if (!/^[1-9]\d{5,11}$/.test(runPrefix)) {
    throw new Error(
      'MANAGED_OPERATION_E2E_RUN_PREFIX must contain 6 to 12 ASCII digits and must not start with zero',
    )
  }
  return {
    pmsBaseUrl: assertLoopbackUrl(
      String(process.env.PMS_BASE_URL || 'http://localhost:8092'),
      'PMS_BASE_URL',
    ),
    simulatorBaseUrl: assertLoopbackUrl(
      String(process.env.SIMULATOR_BASE_URL || process.env.SIM_BASE_URL || 'http://localhost:4000'),
      'SIMULATOR_BASE_URL',
    ),
    testSupportKey,
    outputDirectory: path.resolve(outputDirectoryRaw),
    runPrefix,
    pythonCommand: String(process.env.MANAGED_OPERATION_E2E_PYTHON || 'python3').trim(),
    allowOverwrite: process.env.MANAGED_OPERATION_E2E_ALLOW_OVERWRITE === 'true',
  }
}

function requireObject(value: unknown, field: string): Record<string, any> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`${field} is missing or is not an object`)
  }
  return value as Record<string, any>
}

function requirePositiveInteger(value: unknown, field: string): number {
  const numeric = Number(value)
  if (!Number.isSafeInteger(numeric) || numeric <= 0) {
    throw new Error(`${field} is missing or invalid`)
  }
  return numeric
}

function safeResponseMessage(value: unknown): string {
  if (!value || typeof value !== 'object') {
    return 'no response message'
  }
  const message = String((value as Record<string, unknown>).message || '').trim()
  return message ? redactSensitiveText(message) : 'no response message'
}

async function bootstrap(config: RuntimeConfig): Promise<BootstrapSession> {
  const response = await axios.post(`${config.simulatorBaseUrl}${BOOTSTRAP_PATH}`, {}, {
    headers: { 'X-Test-Support-Key': config.testSupportKey },
    timeout: 60000,
    validateStatus: () => true,
  })
  if (response.status < 200 || response.status >= 300 || response.data?.success !== true) {
    throw new Error(
      `simulator bootstrap failed: HTTP ${response.status}; ${safeResponseMessage(response.data)}`,
    )
  }
  const data = requireObject(response.data?.data, 'bootstrap.data')
  const readiness = requireObject(data.readiness, 'bootstrap.data.readiness')
  if (readiness.ready !== true) {
    throw new Error('simulator bootstrap readiness is false')
  }
  const contextResult = requireObject(readiness.context, 'bootstrap.data.readiness.context')
  if (contextResult.ok !== true) {
    throw new Error('PMS test-support context is not ready')
  }
  const contextData = requireObject(
    contextResult.data,
    'bootstrap.data.readiness.context.data',
  )
  const rooms = contextData.rooms
  if (!Array.isArray(rooms)) {
    throw new Error('bootstrap context did not return rooms')
  }
  const token = String(data.token || '').trim()
  if (!token) {
    throw new Error('bootstrap did not return a local authentication token')
  }
  return {
    token,
    manifestContext: {
      storeId: requirePositiveInteger(data.storeId, 'bootstrap storeId'),
      suHotelId: String(data.suHotelId || '').trim(),
      roomTypeId: requirePositiveInteger(data.roomTypeId, 'bootstrap roomTypeId'),
      rooms: rooms as BootstrapRoom[],
    },
  }
}

function authenticationHeaders(
  config: RuntimeConfig,
  session: BootstrapSession,
): Record<string, string> {
  return {
    Authorization: `Bearer ${session.token}`,
    'X-Store-Id': String(session.manifestContext.storeId),
    'X-Test-Support-Key': config.testSupportKey,
    Accept: 'application/json',
  }
}

async function lookupReservations(
  config: RuntimeConfig,
  session: BootstrapSession,
  bookingKey: string,
): Promise<{ totalMatches: number; reservations: PmsReservationVerificationSnapshot[] }> {
  const response = await axios.get(`${config.pmsBaseUrl}${LOOKUP_PATH}`, {
    headers: authenticationHeaders(config, session),
    params: { channelOrderNumber: bookingKey },
    timeout: 30000,
    validateStatus: () => true,
  })
  if (response.status < 200 || response.status >= 300 || response.data?.success === false) {
    throw new Error(
      `PMS reservation lookup failed for ${bookingKey}: HTTP ${response.status}; ${safeResponseMessage(response.data)}`,
    )
  }
  const data = requireObject(response.data?.data, 'reservation lookup data')
  const totalMatches = Number(data.totalMatches)
  const reservations = Array.isArray(data.reservations)
    ? (data.reservations as PmsReservationVerificationSnapshot[])
    : []
  if (!Number.isSafeInteger(totalMatches) || totalMatches < 0) {
    throw new Error(`PMS reservation lookup returned an invalid count for ${bookingKey}`)
  }
  return { totalMatches, reservations }
}

async function ensureBookingKeysUnused(
  config: RuntimeConfig,
  session: BootstrapSession,
  manifest: ResolvedManagedOperationManifest,
): Promise<void> {
  for (const order of manifest.orders) {
    const lookup = await lookupReservations(config, session, order.bookingKey)
    if (lookup.totalMatches !== 0) {
      throw new Error(
        `run prefix is not isolated: booking key ${order.bookingKey} already has ${lookup.totalMatches} local match(es)`,
      )
    }
  }
}

function getPayloadReservation(payload: Record<string, unknown>): Record<string, any> {
  const reservations = payload.reservations
  if (!Array.isArray(reservations) || reservations.length !== 1) {
    throw new Error('generated Su payload must contain exactly one reservation')
  }
  return requireObject(reservations[0], 'generated reservation')
}

async function sendReservation(
  config: RuntimeConfig,
  manifest: ResolvedManagedOperationManifest,
  order: ResolvedFixtureOrder,
  phase: 'new' | 'cancel',
): Promise<{
  simulatorStatusCode: number
  pmsStatusCode: number
  reservationNotifId: string
  acknowledged: boolean
}> {
  const payload = buildSuPushPayload(manifest, order, phase)
  const reservation = getPayloadReservation(payload)
  const reservationNotifId = String(reservation.reservation_notif_id || '')
  const response = await axios.post(
    `${config.simulatorBaseUrl}${SEND_PATH}`,
    { mode: 'push', hotelId: manifest.store.suHotelId, payload },
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: 60000,
      validateStatus: () => true,
    },
  )
  if (response.status < 200 || response.status >= 300 || response.data?.success !== true) {
    throw new Error(
      `simulator send failed for ${order.role}/${phase}: HTTP ${response.status}; ${safeResponseMessage(response.data)}`,
    )
  }
  const result = requireObject(response.data?.result, 'simulator send result')
  const pmsStatusCode = Number(result.statusCode)
  if (
    result.success !== true ||
    !Number.isSafeInteger(pmsStatusCode) ||
    pmsStatusCode < 200 ||
    pmsStatusCode >= 300
  ) {
    throw new Error(
      `simulator downstream send failed for ${order.role}/${phase}: statusCode=${String(result.statusCode)}`,
    )
  }
  const downstream = requireObject(result.data, 'simulator downstream response')
  if (String(downstream.Status || '').toLowerCase() === 'fail') {
    throw new Error(`PMS rejected Su payload for ${order.role}/${phase}`)
  }
  const ackIds = downstream.reservation_notif?.reservation_notif_id
  const acknowledged =
    Array.isArray(ackIds) && ackIds.map(String).includes(reservationNotifId)
  if (!acknowledged) {
    throw new Error(
      `PMS response did not acknowledge reservation_notif_id for ${order.role}/${phase}`,
    )
  }
  return {
    simulatorStatusCode: response.status,
    pmsStatusCode,
    reservationNotifId,
    acknowledged,
  }
}

function assertReservationMatches(
  reservation: PmsReservationVerificationSnapshot,
  order: ResolvedFixtureOrder,
  expectedStatus: 'CONFIRMED' | 'CANCELLED',
): void {
  const failures = reservationVerificationFailures(reservation, order, expectedStatus)
  if (failures.length > 0) {
    throw new Error(
      `PMS reservation verification failed for ${order.role}: ${failures.join(', ')}`,
    )
  }
}

async function waitForVerifiedReservation(
  config: RuntimeConfig,
  session: BootstrapSession,
  order: ResolvedFixtureOrder,
  expectedStatus: 'CONFIRMED' | 'CANCELLED',
): Promise<PmsReservationVerificationSnapshot> {
  let lastCount = 0
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const lookup = await lookupReservations(config, session, order.bookingKey)
    lastCount = lookup.totalMatches
    if (lookup.totalMatches === 1 && lookup.reservations.length === 1) {
      const reservation = lookup.reservations[0]
      try {
        assertReservationMatches(reservation, order, expectedStatus)
        return reservation
      } catch (error) {
        if (attempt === 9) throw error
      }
    } else if (lookup.totalMatches > 1) {
      throw new Error(
        `PMS lookup for ${order.bookingKey} is ambiguous: ${lookup.totalMatches} matches`,
      )
    }
    await new Promise((resolve) => setTimeout(resolve, 250))
  }
  throw new Error(
    `PMS reservation did not become verifiable for ${order.role}; totalMatches=${lastCount}`,
  )
}

function summaryForSend(
  manifest: ResolvedManagedOperationManifest,
  order: ResolvedFixtureOrder,
  phase: 'new' | 'cancel',
  send: Awaited<ReturnType<typeof sendReservation>>,
  reservation: PmsReservationVerificationSnapshot,
): SendSummary {
  return {
    role: order.role,
    phase,
    channel: order.channel,
    bookingKey: order.bookingKey,
    reservationNotifId: send.reservationNotifId,
    hotelId: manifest.store.suHotelId,
    roomNumber: order.room.roomNumber,
    roomPayloadId: `${manifest.store.roomTypeId}-${order.room.roomNumber}`,
    checkInDate: order.checkInDate,
    checkOutDate: order.checkOutDate,
    currency: order.currency,
    payloadStatus: phase === 'cancel' ? 'cancelled' : 'new',
    simulatorStatusCode: send.simulatorStatusCode,
    pmsStatusCode: send.pmsStatusCode,
    acknowledged: send.acknowledged,
    lookupReservationId: reservation.id,
    lookupOrderNumber: reservation.orderNumber,
    lookupStatus: reservation.status,
    verified: true,
  }
}

async function sendAndVerifyOrders(
  config: RuntimeConfig,
  session: BootstrapSession,
  manifest: ResolvedManagedOperationManifest,
): Promise<SendSummary[]> {
  const summaries: SendSummary[] = []
  for (const order of manifest.orders) {
    const initialSend = await sendReservation(config, manifest, order, 'new')
    const initialReservation = await waitForVerifiedReservation(
      config,
      session,
      order,
      'CONFIRMED',
    )
    summaries.push(summaryForSend(manifest, order, 'new', initialSend, initialReservation))

    if (order.finalStatus === 'CANCELLED') {
      const cancellationSend = await sendReservation(config, manifest, order, 'cancel')
      const cancelledReservation = await waitForVerifiedReservation(
        config,
        session,
        order,
        'CANCELLED',
      )
      summaries.push(
        summaryForSend(manifest, order, 'cancel', cancellationSend, cancelledReservation),
      )
    }
  }
  return summaries
}

async function pathExists(filePath: string): Promise<boolean> {
  try {
    await access(filePath, fsConstants.F_OK)
    return true
  } catch {
    return false
  }
}

async function ensureTargetsAvailable(
  config: RuntimeConfig,
  relativePaths: string[],
): Promise<void> {
  if (config.allowOverwrite) return
  for (const relativePath of relativePaths) {
    const target = path.join(config.outputDirectory, relativePath)
    if (await pathExists(target)) {
      throw new Error(
        `fixture artifact already exists: ${target}; use a new output directory or explicitly set MANAGED_OPERATION_E2E_ALLOW_OVERWRITE=true`,
      )
    }
  }
}

async function writeArtifact(
  config: RuntimeConfig,
  relativePath: string,
  content: string | Buffer,
): Promise<void> {
  const target = path.join(config.outputDirectory, relativePath)
  await mkdir(path.dirname(target), { recursive: true })
  await writeFile(target, content, config.allowOverwrite ? undefined : { flag: 'wx' })
}

async function writeXlsx(
  config: RuntimeConfig,
  relativePath: string,
  model: WorkbookModel,
): Promise<void> {
  const target = path.join(config.outputDirectory, relativePath)
  await mkdir(path.dirname(target), { recursive: true })
  const helper = path.join(__dirname, 'managed-operation-xlsx.py')
  await new Promise<void>((resolve, reject) => {
    const child = spawn(config.pythonCommand, [helper, target], {
      stdio: ['pipe', 'ignore', 'pipe'],
    })
    let stderr = ''
    child.stderr.setEncoding('utf8')
    child.stderr.on('data', (chunk: string) => {
      if (stderr.length < 4000) stderr += chunk
    })
    child.on('error', (error) => {
      reject(
        new Error(
          `cannot run ${config.pythonCommand}; Python 3 is required for standard-library XLSX packaging: ${error.message}`,
        ),
      )
    })
    child.on('close', (code) => {
      if (code === 0) {
        resolve()
        return
      }
      reject(
        new Error(
          `standard-library XLSX writer failed with exit code ${String(code)}: ${redactSensitiveText(stderr.trim())}`,
        ),
      )
    })
    child.stdin.end(JSON.stringify(model))
  })
}

async function writeFixtureArtifacts(
  config: RuntimeConfig,
  manifest: ResolvedManagedOperationManifest,
): Promise<void> {
  const anomalyArtifacts = buildAnomalyArtifacts(manifest)
  const relativePaths = [
    'fixture-manifest.json',
    manifest.files.normal.airbnb,
    manifest.files.normal.booking,
    manifest.files.normal.expected,
    'simulator-payload-summary.json',
    ...Object.keys(anomalyArtifacts).map(
      (name) => `${manifest.files.anomaliesDirectory}/${name}`,
    ),
  ]
  await ensureTargetsAvailable(config, relativePaths)
  await mkdir(config.outputDirectory, { recursive: true })

  await writeXlsx(config, manifest.files.normal.booking, buildBookingWorkbookModel(manifest))
  await writeArtifact(
    config,
    'fixture-manifest.json',
    `${JSON.stringify(manifest, null, 2)}\n`,
  )
  await writeArtifact(config, manifest.files.normal.airbnb, renderNormalAirbnbCsv(manifest))
  await writeArtifact(
    config,
    manifest.files.normal.expected,
    `${JSON.stringify(manifest.expected, null, 2)}\n`,
  )
  for (const [name, content] of Object.entries(anomalyArtifacts)) {
    await writeArtifact(config, `${manifest.files.anomaliesDirectory}/${name}`, content)
  }
}

async function writePayloadSummary(
  config: RuntimeConfig,
  manifest: ResolvedManagedOperationManifest,
  sends: SendSummary[],
): Promise<void> {
  const summary = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    sourceManifest: 'fixture-manifest.json',
    runPrefix: manifest.runPrefix,
    store: manifest.store,
    logicalOrderCount: manifest.orders.length,
    webhookSendCount: sends.length,
    allAcknowledged: sends.every((item) => item.acknowledged),
    allLookupsVerified: sends.every((item) => item.verified),
    sends,
  }
  await writeArtifact(
    config,
    'simulator-payload-summary.json',
    `${JSON.stringify(summary, null, 2)}\n`,
  )
}

async function main(): Promise<void> {
  const config = readRuntimeConfig()
  const session = await bootstrap(config)
  const manifest = buildResolvedManifest(config.runPrefix, session.manifestContext)

  await writeFixtureArtifacts(config, manifest)
  await ensureBookingKeysUnused(config, session, manifest)
  const sends = await sendAndVerifyOrders(config, session, manifest)
  await writePayloadSummary(config, manifest, sends)

  // Never print the bootstrap token or test-support key.
  // eslint-disable-next-line no-console
  console.log(
    `PASS managed-operation fixture: storeId=${manifest.store.id}, runPrefix=${manifest.runPrefix}, orders=${manifest.orders.length}, sends=${sends.length}, output=${config.outputDirectory}`,
  )
}

main().catch((error) => {
  const message = redactSensitiveText(error instanceof Error ? error.message : String(error))
  // eslint-disable-next-line no-console
  console.error(`FAIL managed-operation fixture: ${message}`)
  process.exitCode = 1
})
