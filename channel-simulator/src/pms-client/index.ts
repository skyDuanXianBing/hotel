import axios from 'axios'

import config from '../config'
import type {
  JsonObject,
  PmsApiEnvelope,
  PmsClientResult,
  PmsReadinessData,
  PmsRequestContext,
  PmsMessagingLookupData,
  PmsMessagingLookupQuery,
  PmsReservationLookupData,
  PmsReservationLookupQuery,
  PmsSetupLocalData,
  PmsWebhookEventLookupData,
  PmsWebhookEventLookupQuery,
} from './types'

export type {
  JsonObject,
  PmsChannelSummary,
  PmsClientResult,
  PmsOtaIntegrationSummary,
  PmsPricePlanSummary,
  PmsReadinessData,
  PmsMessagingLookupData,
  PmsMessagingLookupQuery,
  PmsRequestContext,
  PmsReservationLookupData,
  PmsReservationLookupQuery,
  PmsReservationSummary,
  PmsRoomSummary,
  PmsRoomTypeSummary,
  PmsSetupLocalData,
  PmsSetupLocalRoomSummary,
  PmsMessagingMessageSummary,
  PmsMessagingThreadSummary,
  PmsWebhookEventLookupData,
  PmsWebhookEventLookupQuery,
  PmsWebhookEventSummary,
} from './types'

const TEST_SUPPORT_BASE_PATH = '/api/v1/test-support/channel-e2e'
const PMS_REQUEST_TIMEOUT_MS = 15000

function joinUrl(baseUrl: string, requestPath: string): string {
  const left = String(baseUrl || '').replace(/\/+$/, '')
  const right = String(requestPath || '').replace(/^\/+/, '')
  return `${left}/${right}`
}

function hasText(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0
}

function normalizeAuthorization(value: string): string {
  const text = value.trim()
  if (text.toLowerCase().startsWith('bearer ')) {
    return text
  }
  return `Bearer ${text}`
}

function getEffectiveAuthorization(context?: PmsRequestContext): string | null {
  const requestAuthorization = context?.authorization
  if (hasText(requestAuthorization)) {
    return requestAuthorization.trim()
  }
  if (hasText(config.pmsAuth.token)) {
    return normalizeAuthorization(config.pmsAuth.token)
  }
  return null
}

function getEffectiveStoreId(context?: PmsRequestContext): string | null {
  const requestStoreId = context?.storeId
  if (hasText(requestStoreId)) {
    return requestStoreId.trim()
  }
  if (hasText(config.pmsAuth.storeId)) {
    return config.pmsAuth.storeId.trim()
  }
  return null
}

function getConfiguredTestSupportKey(): string | null {
  const key = process.env.CHANNEL_E2E_TEST_SUPPORT_KEY || process.env.TEST_SUPPORT_KEY || ''
  if (hasText(key)) {
    return key.trim()
  }
  return null
}

function getEffectiveTestSupportKey(context?: PmsRequestContext): string | null {
  const requestTestSupportKey = context?.testSupportKey
  if (hasText(requestTestSupportKey)) {
    return requestTestSupportKey.trim()
  }
  const configuredKey = getConfiguredTestSupportKey()
  if (configuredKey) {
    return configuredKey
  }
  return null
}

function getCredentialSource(
  requestValue: unknown,
  envValue: unknown,
): 'request_header' | 'env' | 'missing' {
  if (hasText(requestValue)) {
    return 'request_header'
  }
  if (hasText(envValue)) {
    return 'env'
  }
  return 'missing'
}

function buildHeaders(context?: PmsRequestContext): Record<string, string> {
  const headers: Record<string, string> = {
    Accept: 'application/json',
  }

  const authorization = getEffectiveAuthorization(context)
  const storeId = getEffectiveStoreId(context)
  const testSupportKey = getEffectiveTestSupportKey(context)

  if (authorization) {
    headers.Authorization = authorization
  }

  if (storeId) {
    headers['X-Store-Id'] = storeId
  }

  if (testSupportKey) {
    headers['X-Test-Support-Key'] = testSupportKey
  }

  return headers
}

function buildConfigDiagnostic(context?: PmsRequestContext): JsonObject {
  const storeId = getEffectiveStoreId(context)
  return {
    pmsBaseUrl: config.pmsBaseUrl,
    hasAuthToken: getEffectiveAuthorization(context) !== null,
    authSource: getCredentialSource(context?.authorization, config.pmsAuth.token),
    storeId,
    storeIdSource: getCredentialSource(context?.storeId, config.pmsAuth.storeId),
    hasTestSupportKey: getEffectiveTestSupportKey(context) !== null,
    testSupportKeySource: getCredentialSource(context?.testSupportKey, getConfiguredTestSupportKey()),
  }
}

function toEnvelope<T>(value: unknown): PmsApiEnvelope<T> | null {
  if (!value || typeof value !== 'object') {
    return null
  }
  return value as PmsApiEnvelope<T>
}

function getResponseMessage(value: unknown): string | null {
  if (!value || typeof value !== 'object') {
    return null
  }

  const envelope = value as PmsApiEnvelope<unknown>
  if (hasText(envelope.message)) {
    return envelope.message.trim()
  }

  return null
}

function getResponseErrorCode(value: unknown): string | null {
  if (!value || typeof value !== 'object') {
    return null
  }

  const envelope = value as PmsApiEnvelope<unknown>
  if (hasText(envelope.errorCode)) {
    return envelope.errorCode.trim()
  }
  if (typeof envelope.code === 'number') {
    return String(envelope.code)
  }
  if (hasText(envelope.code)) {
    return envelope.code.trim()
  }

  return null
}

function buildFailureResult<T>(
  statusCode: number | null,
  statusText: string | null,
  responseData: unknown,
  fallbackMessage: string,
  diagnostic: JsonObject,
): PmsClientResult<T> {
  const responseMessage = getResponseMessage(responseData)
  const errorCode = getResponseErrorCode(responseData)
  const statusMessage = statusText && statusText.trim() ? statusText.trim() : null

  return {
    ok: false,
    statusCode,
    statusText: statusMessage,
    data: null,
    envelope: null,
    error: responseMessage || fallbackMessage || statusMessage,
    errorCode,
    diagnostic,
  }
}

function unwrapData<T>(responseData: unknown): T | null {
  const envelope = toEnvelope<T>(responseData)
  if (envelope && Object.prototype.hasOwnProperty.call(envelope, 'data')) {
    return envelope.data ?? null
  }
  return responseData as T
}

function buildQuery(params: Record<string, unknown>): URLSearchParams {
  const query = new URLSearchParams()

  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined) {
      continue
    }
    const text = String(value).trim()
    if (text.length === 0) {
      continue
    }
    query.set(key, text)
  }

  return query
}

async function getJson<T>(
  path: string,
  params: Record<string, unknown> = {},
  context?: PmsRequestContext,
): Promise<PmsClientResult<T>> {
  const query = buildQuery(params)
  const requestPath = query.toString() ? `${path}?${query.toString()}` : path
  const url = joinUrl(config.pmsBaseUrl, requestPath)
  const diagnostic: JsonObject = {
    ...buildConfigDiagnostic(context),
    method: 'GET',
    path: requestPath,
  }

  try {
    const response = await axios.get(url, {
      headers: buildHeaders(context),
      timeout: PMS_REQUEST_TIMEOUT_MS,
      validateStatus: () => true,
    })

    const envelope = toEnvelope<T>(response.data)
    const data = unwrapData<T>(response.data)
    const envelopeSuccess = envelope && typeof envelope.success === 'boolean' ? envelope.success : null
    const ok = response.status >= 200 && response.status < 300 && envelopeSuccess !== false

    if (!ok) {
      return buildFailureResult<T>(
        response.status,
        response.statusText || null,
        response.data,
        `HTTP ${response.status}`,
        diagnostic,
      )
    }

    return {
      ok,
      statusCode: response.status,
      statusText: response.statusText || null,
      data,
      envelope,
      error: null,
      errorCode: null,
      diagnostic,
    }
  } catch (err) {
    let error = err instanceof Error ? err.message : String(err)

    if (axios.isAxiosError(err) && err.response) {
      return buildFailureResult<T>(
        err.response.status,
        err.response.statusText || null,
        err.response.data,
        error,
        diagnostic,
      )
    }

    return buildFailureResult<T>(null, null, null, error, diagnostic)
  }
}

async function postJson<T>(
  path: string,
  body: unknown = {},
  context?: PmsRequestContext,
  extraHeaders: Record<string, string> = {},
): Promise<PmsClientResult<T>> {
  const url = joinUrl(config.pmsBaseUrl, path)
  const diagnostic: JsonObject = {
    ...buildConfigDiagnostic(context),
    method: 'POST',
    path,
  }

  try {
    const response = await axios.post(url, body, {
      headers: {
        ...buildHeaders(context),
        ...extraHeaders,
        'Content-Type': 'application/json',
      },
      timeout: PMS_REQUEST_TIMEOUT_MS,
      validateStatus: () => true,
    })

    const envelope = toEnvelope<T>(response.data)
    const data = unwrapData<T>(response.data)
    const envelopeSuccess = envelope && typeof envelope.success === 'boolean' ? envelope.success : null
    const ok = response.status >= 200 && response.status < 300 && envelopeSuccess !== false

    if (!ok) {
      return buildFailureResult<T>(
        response.status,
        response.statusText || null,
        response.data,
        `HTTP ${response.status}`,
        diagnostic,
      )
    }

    return {
      ok,
      statusCode: response.status,
      statusText: response.statusText || null,
      data,
      envelope,
      error: null,
      errorCode: null,
      diagnostic,
    }
  } catch (err) {
    let error = err instanceof Error ? err.message : String(err)

    if (axios.isAxiosError(err) && err.response) {
      return buildFailureResult<T>(
        err.response.status,
        err.response.statusText || null,
        err.response.data,
        error,
        diagnostic,
      )
    }

    return buildFailureResult<T>(null, null, null, error, diagnostic)
  }
}

export function getPmsConfigDiagnostic(context?: PmsRequestContext): JsonObject {
  return buildConfigDiagnostic(context)
}

export function getPmsReadiness(context?: PmsRequestContext): Promise<PmsClientResult<PmsReadinessData>> {
  return getJson<PmsReadinessData>(`${TEST_SUPPORT_BASE_PATH}/readiness`, {}, context)
}

export function getPmsContext(context?: PmsRequestContext): Promise<PmsClientResult<PmsReadinessData>> {
  return getJson<PmsReadinessData>(`${TEST_SUPPORT_BASE_PATH}/context`, {}, context)
}

export function setupPmsLocalChannelE2E(
  testSupportKey?: string | null,
): Promise<PmsClientResult<PmsSetupLocalData>> {
  return postJson<PmsSetupLocalData>(
    `${TEST_SUPPORT_BASE_PATH}/setup-local`,
    {},
    { testSupportKey },
  )
}

export function lookupPmsReservations(
  query: PmsReservationLookupQuery,
  context?: PmsRequestContext,
): Promise<PmsClientResult<PmsReservationLookupData>> {
  return getJson<PmsReservationLookupData>(`${TEST_SUPPORT_BASE_PATH}/reservations/lookup`, { ...query }, context)
}

export function lookupPmsWebhookEvents(
  query: PmsWebhookEventLookupQuery,
  context?: PmsRequestContext,
): Promise<PmsClientResult<PmsWebhookEventLookupData>> {
  return getJson<PmsWebhookEventLookupData>(`${TEST_SUPPORT_BASE_PATH}/webhook-events`, { ...query }, context)
}

export function lookupPmsMessagingThreads(
  query: PmsMessagingLookupQuery,
  context?: PmsRequestContext,
): Promise<PmsClientResult<PmsMessagingLookupData>> {
  return getJson<PmsMessagingLookupData>(`${TEST_SUPPORT_BASE_PATH}/messaging/threads`, { ...query }, context)
}
