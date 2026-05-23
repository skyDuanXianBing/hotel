import { Request, Response } from 'express'
import express from 'express'

import {
  getPmsConfigDiagnostic,
  getPmsContext,
  getPmsReadiness,
  setupPmsLocalChannelE2E,
} from '../pms-client'
import { lookupPmsMessagingThreads, lookupPmsReservations, lookupPmsWebhookEvents } from '../pms-client'
import type { JsonObject, PmsReadinessData, PmsRequestContext } from '../pms-client'
import { registerPendingReservation } from '../state/reservationPendingState'
import {
  appendE2ERunStep,
  getE2ERun,
  markE2ERunStatus,
  saveE2ERun,
  updateE2ERun,
} from '../state/e2eRunState'
import { sendReservationWebhook } from '../webhook-sender'
import { sendMessagingWebhook } from '../webhook-sender'
import { buildDynamicReservation, buildLifecycleReservations, normalizeRunRequest } from './reservationBuilder'
import type {
  BuiltReservation,
  E2EBootstrapResponse,
  E2EReadinessResponse,
  E2ERunRecord,
  E2ERunStep,
  E2ERunVerifierResult,
  LifecycleStepResult,
  NormalizedCreateE2ERunRequest,
  PendingRegistrationResult,
} from './types'

const { v4: uuidv4 } = require('uuid') as { v4: () => string }

const router = express.Router()

const BOOKING_CHANNEL_ID = 19
const AIRBNB_CHANNEL_ID = 244

function nowIso(): string {
  return new Date().toISOString()
}

function buildStep(name: string, status: E2ERunStep['status'], diagnostic?: Record<string, unknown>): E2ERunStep {
  return {
    name,
    status,
    at: nowIso(),
    diagnostic,
  }
}

function collectMissingRequirements(response: E2EReadinessResponse): string[] {
  const missing = new Set<string>()

  if (!response.simulator.hasAuthToken) {
    missing.add('PMS_AUTH_TOKEN')
  }
  if (!response.simulator.storeId) {
    missing.add('PMS_STORE_ID')
  }
  if (!response.simulator.hasTestSupportKey) {
    missing.add('CHANNEL_E2E_TEST_SUPPORT_KEY')
  }
  if (!response.readiness.ok) {
    missing.add('pms_readiness_api')
  }
  if (!response.context.ok) {
    missing.add('pms_context_api')
  }
  for (const item of response.readiness.data?.missingRequirements || []) {
    missing.add(item)
  }
  for (const item of response.context.data?.missingRequirements || []) {
    missing.add(item)
  }

  return Array.from(missing)
}

function getPmsRequestContext(req: Request): PmsRequestContext {
  return {
    authorization: req.get('authorization') || null,
    storeId: req.get('x-store-id') || null,
    testSupportKey: req.get('x-test-support-key') || null,
  }
}

function getBootstrapTestSupportKey(req: Request): string | null {
  const headerValue = req.get('x-test-support-key')
  if (headerValue && headerValue.trim()) {
    return headerValue.trim()
  }

  return null
}

async function buildReadinessResponse(requestContext: PmsRequestContext): Promise<E2EReadinessResponse> {
  const [readiness, pmsContext] = await Promise.all([
    getPmsReadiness(requestContext),
    getPmsContext(requestContext),
  ])
  const response: E2EReadinessResponse = {
    simulator: getPmsConfigDiagnostic(requestContext),
    readiness,
    context: pmsContext,
    ready: false,
    missingRequirements: [],
  }

  response.missingRequirements = collectMissingRequirements(response)
  response.ready =
    response.missingRequirements.length === 0 &&
    readiness.ok &&
    pmsContext.ok &&
    readiness.data?.ready === true &&
    pmsContext.data?.ready === true

  return response
}

function toSendResponseObject(sendResponse: Awaited<ReturnType<typeof sendReservationWebhook>>): Record<string, unknown> {
  return {
    success: sendResponse.success,
    statusCode: sendResponse.statusCode,
    data: sendResponse.data,
    error: sendResponse.error,
  }
}

function buildPullPayload(hotelId: string, notifId: string): Record<string, unknown> {
  return {
    hotelid: hotelId,
    reservation_notif: {
      reservation_notif_id: [notifId],
    },
  }
}

function normalizeMessagingChannel(raw: unknown): 'BOOKING' | 'AIRBNB' {
  const channel = String(raw || '').trim().toUpperCase()
  if (channel === 'BOOKING' || channel === '19') {
    return 'BOOKING'
  }
  if (channel === 'AIRBNB' || channel === '244' || channel === '') {
    return 'AIRBNB'
  }
  throw new Error('channel must be BOOKING or AIRBNB')
}

function buildMessagingPayload(
  runId: string,
  context: PmsReadinessData,
  rawBody: unknown,
): {
  channel: 'BOOKING' | 'AIRBNB'
  ids: JsonObject
  payload: JsonObject
} {
  const body = rawBody && typeof rawBody === 'object' ? (rawBody as JsonObject) : {}
  const channel = normalizeMessagingChannel(body.channel)
  const channelId = channel === 'AIRBNB' ? AIRBNB_CHANNEL_ID : BOOKING_CHANNEL_ID
  const messageId = String(body.messageId || body.externalMessageId || `E2E-MSG-${runId}`)
  const bookingId = String(body.bookingId || (channel === 'AIRBNB' ? `AB-${runId.slice(0, 8)}` : `90${Date.now().toString().slice(-10)}`))
  const threadId = String(body.threadId || (channel === 'AIRBNB' ? `THREAD-${runId.slice(0, 12)}` : bookingId))
  const listingId = String(body.listingId || `LISTING-${context.storeId}`)
  const guestId = String(body.guestId || `GUEST-${runId.slice(0, 8)}`)
  const text = String(body.message || `Local E2E ${channel} guest message ${runId.slice(0, 8)}`)

  const payload: JsonObject = {
    hotelid: context.suHotelId,
    channel_id: String(channelId),
    messageid: messageId,
    message: text,
    bookingid: bookingId,
    threadid: threadId,
    listingid: listingId,
    guestid: guestId,
    bookingflag: 'B',
    user_details: {
      roles: [
        { role: 'owner', user_ids: [22533556] },
        { role: 'guest', user_ids: [311357532] },
      ],
      users: [
        {
          id: 22533556,
          first_name: 'Host',
          location: 'Local E2E',
          preferred_locale: 'ja',
        },
        {
          id: 311357532,
          first_name: channel === 'AIRBNB' ? 'Airbnb Guest' : 'Booking Guest',
          location: 'Local E2E',
          preferred_locale: 'en',
        },
      ],
    },
    booking_details: {
      listing_name: `Local E2E ${channel} Listing`,
      reservation_confirmation_code: bookingId,
      listing_id: listingId,
      listing_id_str: listingId,
      checkin_date: body.checkInDate || null,
      checkout_date: body.checkOutDate || null,
      number_of_guests: 2,
    },
    attachment: [],
  }

  return {
    channel,
    ids: {
      runId,
      hotelId: context.suHotelId,
      channelId,
      bookingId,
      threadId,
      listingId,
      guestId,
      messageId,
    },
    payload,
  }
}

function registerPullReservation(run: E2ERunRecord): PendingRegistrationResult {
  const result = registerPendingReservation({
    notifId: run.ids.notifId,
    hotelId: run.ids.hotelId,
    reservation: run.payload.reservations instanceof Array ? run.payload.reservations[0] : run.payload,
    fixtureName: 'dynamic-e2e',
  })

  return {
    notifId: run.ids.notifId,
    hotelId: run.ids.hotelId,
    registered: Boolean(result),
    registeredAt: result?.registeredAt,
  }
}

async function verifyRun(run: E2ERunRecord, context: PmsRequestContext) {
  const [reservationLookup, webhookEvents] = await Promise.all([
    lookupPmsReservations({
      reservationNotifId: run.ids.notifId,
      suReservationId: run.ids.reservationId,
      channelOrderNumber: run.ids.channelBookingId,
      externalBookingKey: run.ids.channelBookingId,
    }, context),
    lookupPmsWebhookEvents({
      hotelId: run.ids.hotelId,
      reservationNotifId: run.ids.notifId,
      limit: 20,
    }, context),
  ])

  return {
    reservationLookup,
    webhookEvents,
  }
}

function createRunRecord(
  request: NormalizedCreateE2ERunRequest,
  built: BuiltReservation,
  contextResult: Awaited<ReturnType<typeof getPmsContext>>,
): E2ERunRecord {
  const createdAt = nowIso()
  return {
    runId: built.ids.runId,
    status: 'CREATED',
    createdAt,
    updatedAt: createdAt,
    request,
    ids: built.ids,
    payload: built.payload,
    steps: [
      buildStep('pms_context', 'SUCCESS', {
        statusCode: contextResult.statusCode,
      }),
      buildStep('build_reservation', 'SUCCESS', built.diagnostic),
    ],
    pendingRegistration: null,
    sendResponse: null,
    buildDiagnostic: built.diagnostic,
  }
}

async function executeBuiltRun(
  request: NormalizedCreateE2ERunRequest,
  built: BuiltReservation,
  contextResult: Awaited<ReturnType<typeof getPmsContext>>,
  pmsRequestContext: PmsRequestContext,
): Promise<{
  success: boolean
  statusCode: number
  run: E2ERunRecord
  verifier: E2ERunVerifierResult
}> {
  const run = createRunRecord(request, built, contextResult)
  saveE2ERun(run)

  if (request.mode === 'PULL') {
    const pendingRegistration = registerPullReservation(run)
    updateE2ERun(run.runId, { pendingRegistration })
    appendE2ERunStep(
      run.runId,
      buildStep('register_pending_reservation', 'SUCCESS', { ...pendingRegistration }),
    )

    if (!pendingRegistration.registered) {
      markE2ERunStatus(run.runId, 'FAILED')
      const failedRun = getE2ERun(run.runId) || run
      const verifier = await verifyRun(failedRun, pmsRequestContext)
      return {
        success: false,
        statusCode: 500,
        run: failedRun,
        verifier,
      }
    }
  }

  const sendPayload = request.mode === 'PULL' ? buildPullPayload(built.ids.hotelId, built.ids.notifId) : built.payload
  const sendMode = request.mode === 'PULL' ? 'pull' : 'push'
  const sendResponse = await sendReservationWebhook({
    mode: sendMode,
    hotelId: built.ids.hotelId,
    payload: sendPayload,
  })
  const sendResponseObject = toSendResponseObject(sendResponse)
  updateE2ERun(run.runId, { sendResponse: sendResponseObject })
  appendE2ERunStep(
    run.runId,
    buildStep(
      sendResponse.success ? 'send_reservation_webhook' : 'send_reservation_webhook_failed',
      sendResponse.success ? 'SUCCESS' : 'FAILED',
      {
        statusCode: sendResponse.statusCode,
        error: sendResponse.error,
      },
    ),
  )
  markE2ERunStatus(run.runId, sendResponse.success ? 'SENT' : 'FAILED')

  const storedRun = getE2ERun(run.runId)
  if (!storedRun) {
    throw new Error('Run state was not found after send')
  }

  const verifier = await verifyRun(storedRun, pmsRequestContext)
  return {
    success: sendResponse.success,
    statusCode: sendResponse.success ? 201 : 502,
    run: storedRun,
    verifier,
  }
}

async function readinessHandler(req: Request, res: Response): Promise<void> {
  const response = await buildReadinessResponse(getPmsRequestContext(req))
  res.json({
    success: response.ready,
    data: response,
  })
}

async function bootstrapHandler(req: Request, res: Response): Promise<void> {
  const testSupportKey = getBootstrapTestSupportKey(req)
  const diagnostic = getPmsConfigDiagnostic({ testSupportKey })
  if (diagnostic.hasTestSupportKey !== true) {
    res.status(400).json({
      success: false,
      message: '请先在 simulator 后端配置 CHANNEL_E2E_TEST_SUPPORT_KEY，或通过 X-Test-Support-Key 请求头传入本地 E2E 管理密钥。',
      data: {
        diagnostic,
      },
    })
    return
  }

  const setupResult = await setupPmsLocalChannelE2E(testSupportKey)
  if (!setupResult.ok || !setupResult.data) {
    res.status(setupResult.statusCode && setupResult.statusCode >= 400 ? setupResult.statusCode : 502).json({
      success: false,
      message: setupResult.error || 'PMS setup-local request failed',
      data: {
        setup: {
          ok: setupResult.ok,
          statusCode: setupResult.statusCode,
          error: setupResult.error,
          diagnostic: setupResult.diagnostic,
        },
      },
    })
    return
  }

  const setupData = setupResult.data
  const pmsRequestContext: PmsRequestContext = {
    authorization: `Bearer ${setupData.token}`,
    storeId: String(setupData.storeId),
    testSupportKey,
  }
  const readiness = await buildReadinessResponse(pmsRequestContext)
  const response: E2EBootstrapResponse = {
    token: setupData.token,
    storeId: setupData.storeId,
    suHotelId: setupData.suHotelId,
    roomTypeId: setupData.roomTypeId,
    roomId: setupData.roomId,
    readiness,
  }

  res.json({
    success: true,
    data: response,
  })
}

async function createRunHandler(req: Request, res: Response): Promise<void> {
  let normalizedRequest
  try {
    normalizedRequest = normalizeRunRequest(req.body)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    res.status(400).json({
      success: false,
      message,
    })
    return
  }

  const pmsRequestContext = getPmsRequestContext(req)
  const contextResult = await getPmsContext(pmsRequestContext)
  if (!contextResult.ok || !contextResult.data) {
    res.status(502).json({
      success: false,
      message: 'PMS context request failed',
      pmsContext: contextResult,
    })
    return
  }

  const runId = uuidv4()
  let built
  try {
    built = buildDynamicReservation(runId, normalizedRequest, contextResult.data)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    res.status(400).json({
      success: false,
      message,
      pmsContext: contextResult,
    })
    return
  }

  let result
  try {
    result = await executeBuiltRun(normalizedRequest, built, contextResult, pmsRequestContext)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    res.status(500).json({
      success: false,
      message,
    })
    return
  }

  res.status(result.statusCode).json({
    success: result.success,
    data: {
      run: result.run,
      verifier: result.verifier,
    },
  })
}

async function createLifecycleHandler(req: Request, res: Response): Promise<void> {
  let normalizedRequest
  try {
    normalizedRequest = normalizeRunRequest(req.body)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    res.status(400).json({
      success: false,
      message,
    })
    return
  }

  const lifecycleRequest: NormalizedCreateE2ERunRequest = {
    ...normalizedRequest,
    scenario: 'NEW',
    channel: 'BOOKING',
    otaCode: 19,
  }
  const pmsRequestContext = getPmsRequestContext(req)
  const contextResult = await getPmsContext(pmsRequestContext)
  if (!contextResult.ok || !contextResult.data) {
    res.status(502).json({
      success: false,
      message: 'PMS context request failed',
      pmsContext: contextResult,
    })
    return
  }

  const lifecycleRunId = uuidv4()
  let lifecycleSteps
  try {
    lifecycleSteps = buildLifecycleReservations(lifecycleRunId, lifecycleRequest, contextResult.data)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    res.status(400).json({
      success: false,
      message,
      pmsContext: contextResult,
    })
    return
  }

  const results: LifecycleStepResult[] = []
  let success = true
  let statusCode = 201

  for (const lifecycleStep of lifecycleSteps) {
    try {
      const result = await executeBuiltRun(
        lifecycleRequest,
        lifecycleStep.built,
        contextResult,
        pmsRequestContext,
      )
      results.push({
        step: lifecycleStep.step,
        run: result.run,
        verifier: result.verifier,
      })
      if (!result.success) {
        success = false
        statusCode = result.statusCode
        break
      }
    } catch (err) {
      success = false
      statusCode = 500
      const message = err instanceof Error ? err.message : String(err)
      res.status(statusCode).json({
        success: false,
        message,
        data: {
          lifecycleRunId,
          mode: lifecycleRequest.mode,
          steps: results,
        },
      })
      return
    }
  }

  res.status(statusCode).json({
    success,
    data: {
      lifecycleRunId,
      mode: lifecycleRequest.mode,
      channel: lifecycleRequest.channel,
      identityStrategy:
        'new/modification/cancellation reuse reservation.id, channel_booking_id and roomreservation_id; each step uses a unique reservation_notif_id',
      steps: results,
    },
  })
}

async function createMessagingHandler(req: Request, res: Response): Promise<void> {
  const pmsRequestContext = getPmsRequestContext(req)
  const contextResult = await getPmsContext(pmsRequestContext)
  if (!contextResult.ok || !contextResult.data) {
    res.status(502).json({
      success: false,
      message: 'PMS context request failed',
      pmsContext: contextResult,
    })
    return
  }

  const runId = uuidv4()
  let built
  try {
    built = buildMessagingPayload(runId, contextResult.data, req.body)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    res.status(400).json({
      success: false,
      message,
      pmsContext: contextResult,
    })
    return
  }

  const sendResponse = await sendMessagingWebhook({
    hotelId: String(built.ids.hotelId),
    payload: built.payload,
  })
  const messagingLookup = await lookupPmsMessagingThreads(
    {
      channelId: String(built.ids.channelId),
      threadId: String(built.ids.threadId),
      bookingId: String(built.ids.bookingId),
      externalMessageId: String(built.ids.messageId),
      limit: 20,
    },
    pmsRequestContext,
  )
  const totalMatches =
    messagingLookup.data && typeof messagingLookup.data.totalMatches === 'number'
      ? messagingLookup.data.totalMatches
      : 0
  const success = sendResponse.success && messagingLookup.ok && totalMatches > 0

  res.status(success ? 201 : 502).json({
    success,
    data: {
      runId,
      channel: built.channel,
      ids: built.ids,
      payload: built.payload,
      sendResponse: toSendResponseObject(sendResponse),
      verifier: {
        messagingLookup,
      },
    },
  })
}

async function getRunHandler(req: Request, res: Response): Promise<void> {
  const runId = String(req.params.runId || '').trim()
  const run = getE2ERun(runId)
  if (!run) {
    res.status(404).json({
      success: false,
      message: `E2E run not found: ${runId}`,
    })
    return
  }

  const verifier = await verifyRun(run, getPmsRequestContext(req))
  res.json({
    success: true,
    data: {
      run,
      verifier,
    },
  })
}

router.get('/api/e2e/readiness', readinessHandler)
router.post('/api/e2e/bootstrap', bootstrapHandler)
router.post('/api/e2e/runs', createRunHandler)
router.post('/api/e2e/lifecycle', createLifecycleHandler)
router.post('/api/e2e/messaging', createMessagingHandler)
router.get('/api/e2e/runs/:runId', getRunHandler)

export default router
