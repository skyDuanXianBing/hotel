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

function getFirstMessagingThread(runResponse: any): any {
  const threads =
    runResponse?.verifier?.messagingLookup?.data?.threads ??
    runResponse?.data?.verifier?.messagingLookup?.data?.threads ??
    []
  return Array.isArray(threads) && threads.length > 0 ? threads[0] : null
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
    return {
      storeId: data.storeId,
      suHotelId: data.suHotelId,
      roomTypeId: data.roomTypeId,
      roomId: data.roomId,
      rooms: Array.isArray(data.rooms) ? data.rooms.length : 0,
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
    return {
      ready: data?.ready,
      missingRequirements: data?.missingRequirements,
      suHotelId: data?.context?.data?.suHotelId,
    }
  }))

  const runCases = [
    { name: 'booking pull new', body: { mode: 'PULL', channel: 'BOOKING', scenario: 'NEW' } },
    { name: 'booking push multi-room', body: { mode: 'PUSH', channel: 'BOOKING', scenario: 'MULTI_ROOM' } },
    { name: 'airbnb push new', body: { mode: 'PUSH', channel: 'AIRBNB', scenario: 'AIRBNB_NEW' } },
  ]

  for (const runCase of runCases) {
    steps.push(await runStep(runCase.name, async () => {
      const data = await postSimulator('/api/e2e/runs', runCase.body, simulatorHeaders)
      assertCondition(getReservationMatches(data) > 0, 'reservation lookup returned no matches')
      assertCondition(getWebhookEventMatches(data) > 0, 'webhook event lookup returned no matches')
      return {
        runId: data?.run?.runId,
        ids: data?.run?.ids,
        reservationMatches: getReservationMatches(data),
        webhookEventMatches: getWebhookEventMatches(data),
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
  console.log(JSON.stringify({ ok: failed.length === 0, pmsBaseUrl, simulatorBaseUrl, steps }, null, 2))
  if (failed.length > 0) {
    process.exitCode = 1
  }
}

main().catch((err) => {
  // eslint-disable-next-line no-console
  console.error(err instanceof Error ? err.stack || err.message : String(err))
  process.exitCode = 1
})
