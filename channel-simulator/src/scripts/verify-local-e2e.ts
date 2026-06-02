import axios from 'axios'

interface StepResult {
  name: string
  ok: boolean
  statusCode?: number
  summary?: unknown
  error?: string
}

const pmsBaseUrl = String(process.env.PMS_BASE_URL || 'http://localhost:8092').replace(/\/+$/, '')
const simulatorBaseUrl = String(process.env.SIMULATOR_BASE_URL || process.env.SIM_BASE_URL || 'http://localhost:4000').replace(/\/+$/, '')
const testSupportKey = String(process.env.CHANNEL_E2E_TEST_SUPPORT_KEY || process.env.TEST_SUPPORT_KEY || 'local-e2e-key')
const simulatorStoreTimeZone = String(process.env.SIMULATOR_STORE_TIME_ZONE || 'Asia/Tokyo')
const simulatorFixedNow = process.env.SIMULATOR_FIXED_NOW ? String(process.env.SIMULATOR_FIXED_NOW) : null
const autoMessageMarker = 'LOCAL_E2E_AUTO_BOOKING_CONFIRM'
const autoMessageAction = 'BOOKING_CONFIRM'
const autoMessageSendTiming = 'IMMEDIATELY'

function joinUrl(baseUrl: string, path: string): string {
  return `${baseUrl}/${path.replace(/^\/+/, '')}`
}

function unwrapData(responseData: any): any {
  if (responseData && typeof responseData === 'object' && Object.prototype.hasOwnProperty.call(responseData, 'data')) {
    return responseData.data
  }
  return responseData
}

function assertCondition(condition: unknown, message: string): void {
  if (!condition) {
    throw new Error(message)
  }
}

function buildE2EHeaders(token: string, storeId: number | string): Record<string, string> {
  return {
    Authorization: `Bearer ${token}`,
    'X-Store-Id': String(storeId),
    'Content-Type': 'application/json',
  }
}

function buildSimulatorHeaders(token: string, storeId: number | string): Record<string, string> {
  return {
    ...buildE2EHeaders(token, storeId),
    'X-Test-Support-Key': testSupportKey,
  }
}

function getReservationMatches(runResponse: any): number {
  return Number(
    runResponse?.verifier?.reservationLookup?.data?.totalMatches ??
      runResponse?.data?.verifier?.reservationLookup?.data?.totalMatches ??
      0,
  )
}

function getWebhookEventMatches(runResponse: any): number {
  return Number(
    runResponse?.verifier?.webhookEvents?.data?.totalMatches ??
      runResponse?.data?.verifier?.webhookEvents?.data?.totalMatches ??
      0,
  )
}

function getMessagingMatches(runResponse: any): number {
  return Number(
    runResponse?.verifier?.messagingLookup?.data?.totalMatches ??
      runResponse?.data?.verifier?.messagingLookup?.data?.totalMatches ??
      0,
  )
}

function getAutoMessageSendLogMatches(runResponse: any): number {
  return Number(
    runResponse?.verifier?.autoMessageSendLogs?.data?.totalMatches ??
      runResponse?.data?.verifier?.autoMessageSendLogs?.data?.totalMatches ??
      0,
  )
}

function getAutoMessageSendLogs(runResponse: any): any[] {
  const logs =
    runResponse?.verifier?.autoMessageSendLogs?.data?.logs ??
    runResponse?.data?.verifier?.autoMessageSendLogs?.data?.logs ??
    []
  if (Array.isArray(logs)) {
    return logs
  }
  return []
}

function selectPreparedAutoMessage(autoMessages: any): any | null {
  if (!Array.isArray(autoMessages)) {
    return null
  }

  for (const item of autoMessages) {
    if (item?.markerPresent !== true) {
      continue
    }
    if (item?.bookingOnly !== true) {
      continue
    }
    if (item?.enabled !== true) {
      continue
    }
    if (item?.action !== autoMessageAction) {
      continue
    }
    if (item?.sendTiming !== autoMessageSendTiming) {
      continue
    }
    return item
  }

  return null
}

function getVerifierAutoMessageTemplate(runResponse: any): any | null {
  const template =
    runResponse?.verifier?.autoMessageTemplate ??
    runResponse?.data?.verifier?.autoMessageTemplate ??
    null
  if (template && typeof template === 'object') {
    return template
  }
  return null
}

function getAutoMessageTemplateId(template: any): number | null {
  const id = Number(template?.id)
  if (Number.isFinite(id) && id > 0) {
    return id
  }
  return null
}

function getExpectedAutoMessageSendLogAction(template: any): string | null {
  const id = getAutoMessageTemplateId(template)
  if (!id) {
    return null
  }
  return `AM:${id}`
}

function describeAutoMessageTemplate(template: any): string {
  const id = getAutoMessageTemplateId(template)
  const templateAction = String(template?.action || 'unknown')
  const sendLogAction = getExpectedAutoMessageSendLogAction(template) || 'missing'
  return `autoMessageId=${id || 'missing'}, templateAction=${templateAction}, expectedSendLogAction=${sendLogAction}`
}

function autoMessageLogMatchesTemplate(log: any, template: any): boolean {
  const expectedId = getAutoMessageTemplateId(template)
  if (!expectedId) {
    return false
  }
  const expectedAction = getExpectedAutoMessageSendLogAction(template)
  if (!expectedAction) {
    return false
  }
  if (Number(log?.autoMessageId) !== expectedId) {
    return false
  }
  if (String(log?.action || '') !== expectedAction) {
    return false
  }
  if (log?.success !== true) {
    return false
  }
  return true
}

function getBoundAutoMessageSendLogMatches(runResponse: any, expectedTemplate: any): number {
  const logs = getAutoMessageSendLogs(runResponse)
  let matches = 0
  for (const log of logs) {
    if (autoMessageLogMatchesTemplate(log, expectedTemplate)) {
      matches += 1
    }
  }
  return matches
}

function hasAutoMessageMarker(runResponse: any): boolean {
  const threads =
    runResponse?.verifier?.autoMessageMessages?.data?.threads ??
    runResponse?.data?.verifier?.autoMessageMessages?.data?.threads ??
    []
  if (!Array.isArray(threads)) {
    return false
  }

  for (const thread of threads) {
    const messages = thread?.messages
    if (!Array.isArray(messages)) {
      continue
    }
    for (const message of messages) {
      const content = String(message?.content || '')
      if (content.includes(autoMessageMarker)) {
        return true
      }
    }
  }

  return false
}

function getFirstMessagingThread(runResponse: any): any {
  const threads =
    runResponse?.verifier?.messagingLookup?.data?.threads ??
    runResponse?.data?.verifier?.messagingLookup?.data?.threads ??
    []
  return Array.isArray(threads) && threads.length > 0 ? threads[0] : null
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

async function runStep(name: string, fn: () => Promise<unknown>): Promise<StepResult> {
  try {
    const summary = await fn()
    const result = { name, ok: true, summary }
    // eslint-disable-next-line no-console
    console.log(`PASS ${name}`)
    return result
  } catch (err) {
    const error = err instanceof Error ? err.message : String(err)
    // eslint-disable-next-line no-console
    console.error(`FAIL ${name}: ${error}`)
    return { name, ok: false, error }
  }
}

async function postSimulator(path: string, body: unknown, headers: Record<string, string>): Promise<any> {
  const response = await axios.post(joinUrl(simulatorBaseUrl, path), body, {
    headers,
    timeout: 60000,
    validateStatus: () => true,
  })
  const data = unwrapData(response.data)
  assertCondition(response.status >= 200 && response.status < 300, `HTTP ${response.status}: ${JSON.stringify(response.data)}`)
  assertCondition(response.data?.success !== false, `response success=false: ${JSON.stringify(response.data)}`)
  return data
}

async function getSimulator(path: string, headers: Record<string, string>): Promise<any> {
  const response = await axios.get(joinUrl(simulatorBaseUrl, path), {
    headers,
    timeout: 60000,
    validateStatus: () => true,
  })
  const data = unwrapData(response.data)
  assertCondition(response.status >= 200 && response.status < 300, `HTTP ${response.status}: ${JSON.stringify(response.data)}`)
  assertCondition(response.data?.success !== false, `response success=false: ${JSON.stringify(response.data)}`)
  return data
}

async function dispatchPmsAutoMessages(headers: Record<string, string>): Promise<void> {
  const response = await axios.post(joinUrl(pmsBaseUrl, '/api/v1/test-support/channel-e2e/auto-messages/dispatch'), {}, {
    headers,
    timeout: 60000,
    validateStatus: () => true,
  })
  assertCondition(
    response.status >= 200 && response.status < 300 && response.data?.success !== false,
    `auto message dispatch failed: HTTP ${response.status}: ${JSON.stringify(response.data)}`,
  )
}

async function waitForBookingAutoMessage(
  runData: any,
  headers: Record<string, string>,
  expectedTemplate: any,
): Promise<any> {
  let current = runData
  const runId = String(runData?.run?.runId || '')
  assertCondition(runId, 'booking run did not return runId')
  assertCondition(getAutoMessageTemplateId(expectedTemplate), `Booking auto message template is not identifiable: ${JSON.stringify(expectedTemplate)}`)

  for (let attempt = 0; attempt < 5; attempt++) {
    if (getBoundAutoMessageSendLogMatches(current, expectedTemplate) > 0 && hasAutoMessageMarker(current)) {
      return current
    }
    await dispatchPmsAutoMessages(headers)
    await sleep(1000)
    current = await getSimulator(`/api/e2e/runs/${runId}`, headers)
  }

  const verifierTemplate = getVerifierAutoMessageTemplate(current)
  if (verifierTemplate) {
    assertCondition(
      getAutoMessageTemplateId(verifierTemplate) === getAutoMessageTemplateId(expectedTemplate),
      `verifier used a different auto message template: expected ${describeAutoMessageTemplate(expectedTemplate)}, got ${describeAutoMessageTemplate(verifierTemplate)}`,
    )
  }
  assertCondition(
    getBoundAutoMessageSendLogMatches(current, expectedTemplate) > 0,
    `auto message send log returned no successful matches for ${describeAutoMessageTemplate(expectedTemplate)}`,
  )
  assertCondition(hasAutoMessageMarker(current), `messaging lookup did not contain ${autoMessageMarker}`)
  return current
}

async function main(): Promise<void> {
  const steps: StepResult[] = []

  const setup = await runStep('pms setup-local', async () => {
    const response = await axios.post(joinUrl(pmsBaseUrl, '/api/v1/test-support/channel-e2e/setup-local'), {}, {
      headers: { 'X-Test-Support-Key': testSupportKey },
      timeout: 60000,
      validateStatus: () => true,
    })
    assertCondition(response.status >= 200 && response.status < 300, `HTTP ${response.status}: ${JSON.stringify(response.data)}`)
    assertCondition(response.data?.success !== false, `setup-local failed: ${JSON.stringify(response.data)}`)
    const data = unwrapData(response.data)
    assertCondition(data?.token, 'setup-local did not return token')
    assertCondition(data?.storeId, 'setup-local did not return storeId')
    assertCondition(data?.suHotelId, 'setup-local did not return suHotelId')
    assertCondition(data?.readiness?.storeTimezone === 'Asia/Tokyo', `setup-local timezone is not Asia/Tokyo: ${data?.readiness?.storeTimezone}`)
    const autoMessages = data?.readiness?.autoMessages
    assertCondition(Array.isArray(autoMessages) && autoMessages.length > 0, 'setup-local readiness did not return autoMessages')
    const autoMessageTemplate = selectPreparedAutoMessage(autoMessages)
    assertCondition(autoMessageTemplate, 'setup-local did not prepare Booking auto message template')
    return {
      storeId: data.storeId,
      suHotelId: data.suHotelId,
      roomTypeId: data.roomTypeId,
      roomId: data.roomId,
      rooms: Array.isArray(data.rooms) ? data.rooms.length : 0,
      storeTimezone: data?.readiness?.storeTimezone,
      autoMessages: Array.isArray(autoMessages) ? autoMessages.length : 0,
      autoMessageTemplate,
      token: data.token,
    }
  })
  steps.push(setup)
  if (!setup.ok || !setup.summary || typeof setup.summary !== 'object') {
    throw new Error('setup-local failed; stop verification')
  }

  const setupSummary = setup.summary as Record<string, any>
  const token = String(setupSummary.token)
  const storeId = setupSummary.storeId
  const expectedAutoMessageTemplate = setupSummary.autoMessageTemplate
  const headers = buildE2EHeaders(token, storeId)
  const simulatorHeaders = buildSimulatorHeaders(token, storeId)

  steps.push(await runStep('simulator readiness', async () => {
    const response = await axios.get(joinUrl(simulatorBaseUrl, '/api/e2e/readiness'), {
      headers: simulatorHeaders,
      timeout: 30000,
      validateStatus: () => true,
    })
    const data = unwrapData(response.data)
    assertCondition(response.status === 200, `HTTP ${response.status}: ${JSON.stringify(response.data)}`)
    assertCondition(response.data?.success === true, `readiness not ready: ${JSON.stringify(response.data)}`)
    const autoMessageTemplate = selectPreparedAutoMessage(data?.context?.data?.autoMessages)
    assertCondition(autoMessageTemplate, 'simulator readiness did not include Booking auto message template')
    assertCondition(
      getAutoMessageTemplateId(autoMessageTemplate) === getAutoMessageTemplateId(expectedAutoMessageTemplate),
      `simulator readiness auto message template mismatch: expected ${describeAutoMessageTemplate(expectedAutoMessageTemplate)}, got ${describeAutoMessageTemplate(autoMessageTemplate)}`,
    )
    return {
      ready: data?.ready,
      missingRequirements: data?.missingRequirements,
      suHotelId: data?.context?.data?.suHotelId,
      storeTimezone: data?.context?.data?.storeTimezone,
      autoMessageTemplate,
    }
  }))

  const runCases = [
    { name: 'booking pull new', body: { mode: 'PULL', channel: 'BOOKING', scenario: 'NEW' } },
    { name: 'booking push multi-room', body: { mode: 'PUSH', channel: 'BOOKING', scenario: 'MULTI_ROOM' } },
    { name: 'airbnb push new', body: { mode: 'PUSH', channel: 'AIRBNB', scenario: 'AIRBNB_NEW' } },
  ]

  for (const runCase of runCases) {
    steps.push(await runStep(runCase.name, async () => {
      let data = await postSimulator('/api/e2e/runs', runCase.body, simulatorHeaders)
      assertCondition(getReservationMatches(data) > 0, 'reservation lookup returned no matches')
      assertCondition(getWebhookEventMatches(data) > 0, 'webhook event lookup returned no matches')
      if (runCase.body.channel === 'BOOKING' && runCase.body.scenario === 'NEW') {
        data = await waitForBookingAutoMessage(data, simulatorHeaders, expectedAutoMessageTemplate)
        assertCondition(
          getBoundAutoMessageSendLogMatches(data, expectedAutoMessageTemplate) > 0,
          `auto message send log returned no successful matches for ${describeAutoMessageTemplate(expectedAutoMessageTemplate)}`,
        )
        assertCondition(hasAutoMessageMarker(data), `messaging lookup did not contain ${autoMessageMarker}`)
      }
      return {
        runId: data?.run?.runId,
        ids: data?.run?.ids,
        reservationMatches: getReservationMatches(data),
        webhookEventMatches: getWebhookEventMatches(data),
        autoMessageSendLogMatches: getAutoMessageSendLogMatches(data),
        boundAutoMessageSendLogMatches: getBoundAutoMessageSendLogMatches(data, expectedAutoMessageTemplate),
        autoMessageMarkerPresent: hasAutoMessageMarker(data),
      }
    }))
  }

  for (const mode of ['PUSH', 'PULL']) {
    steps.push(await runStep(`booking lifecycle ${mode.toLowerCase()}`, async () => {
      const data = await postSimulator('/api/e2e/lifecycle', { mode, channel: 'BOOKING' }, simulatorHeaders)
      const lifecycleSteps = Array.isArray(data?.steps) ? data.steps : []
      assertCondition(lifecycleSteps.length === 3, `expected 3 lifecycle steps, got ${lifecycleSteps.length}`)
      for (const item of lifecycleSteps) {
        assertCondition(getReservationMatches(item) > 0, `lifecycle ${item?.step || 'unknown'} reservation lookup returned no matches`)
        assertCondition(getWebhookEventMatches(item) > 0, `lifecycle ${item?.step || 'unknown'} webhook event lookup returned no matches`)
      }
      return {
        lifecycleRunId: data?.lifecycleRunId,
        mode: data?.mode,
        steps: lifecycleSteps.map((item: any) => ({
          step: item.step,
          runId: item?.run?.runId,
          reservationMatches: getReservationMatches(item),
          webhookEventMatches: getWebhookEventMatches(item),
        })),
      }
    }))
  }

  steps.push(await runStep('airbnb messaging webhook', async () => {
    const data = await postSimulator('/api/e2e/messaging', { channel: 'AIRBNB' }, simulatorHeaders)
    assertCondition(getMessagingMatches(data) > 0, 'messaging lookup returned no matches')
    const thread = getFirstMessagingThread(data)
    assertCondition(thread?.id, 'messaging lookup did not return thread id')
    const replyResponse = await axios.post(
      joinUrl(pmsBaseUrl, `/api/v1/su-messaging/threads/${thread.id}/send`),
      { content: `Local E2E staff reply ${data?.runId}`, senderName: 'Channel E2E' },
      {
        headers,
        timeout: 30000,
        validateStatus: () => true,
      },
    )
    assertCondition(
      replyResponse.status >= 200 && replyResponse.status < 300 && replyResponse.data?.success !== false,
      `staff reply failed: HTTP ${replyResponse.status}: ${JSON.stringify(replyResponse.data)}`,
    )
    const replyData = unwrapData(replyResponse.data)
    assertCondition(replyData?.id, 'staff reply did not return message id')
    return {
      runId: data?.runId,
      ids: data?.ids,
      threadDatabaseId: thread.id,
      staffReplyMessageId: replyData.id,
      messagingMatches: getMessagingMatches(data),
    }
  }))

  const failed = steps.filter((step) => !step.ok)
  // eslint-disable-next-line no-console
  console.log(JSON.stringify({
    ok: failed.length === 0,
    pmsBaseUrl,
    simulatorBaseUrl,
    simulatorTime: {
      storeTimeZone: simulatorStoreTimeZone,
      fixedNow: simulatorFixedNow,
    },
    steps,
  }, null, 2))
  if (failed.length > 0) {
    process.exitCode = 1
  }
}

main().catch((err) => {
  // eslint-disable-next-line no-console
  console.error(err instanceof Error ? err.stack || err.message : String(err))
  process.exitCode = 1
})
