import type { JsonObject, PmsAutoMessageSummary, PmsClientResult, PmsReadinessData } from '../pms-client'

export type E2ERunMode = 'PUSH' | 'PULL'
export type E2EChannelCode = 'BOOKING' | 'AIRBNB'
export type E2ERunScenario = 'NEW' | 'MULTI_ROOM' | 'AIRBNB_NEW'
export type E2ELifecycleStepName = 'new' | 'modification' | 'cancellation'
export type E2ERunStatus = 'CREATED' | 'SENT' | 'FAILED'
export type E2ERunStepStatus = 'SUCCESS' | 'FAILED'

export interface CreateE2ERunRequest {
  mode?: unknown
  scenario?: unknown
  otaCode?: unknown
  channel?: unknown
  roomTypeId?: unknown
  roomId?: unknown
  stayStartDays?: unknown
}

export interface NormalizedCreateE2ERunRequest {
  mode: E2ERunMode
  scenario: E2ERunScenario
  otaCode: 19 | 244
  channel: E2EChannelCode
  roomTypeId: number | null
  roomId: number | null
  stayStartDays: number | null
}

export interface E2ERunGeneratedIds {
  runId: string
  hotelId: string
  reservationId: string
  channelBookingId: string
  roomReservationId: string
  roomReservationIds: string[]
  notifId: string
  otaCode: 19 | 244
  channel: E2EChannelCode
  roomPayloadId: string
  roomPayloadIds: string[]
}

export interface E2ERunStep {
  name: string
  status: E2ERunStepStatus
  at: string
  diagnostic?: JsonObject
}

export interface PendingRegistrationResult {
  notifId: string
  hotelId: string
  registered: boolean
  registeredAt?: string
}

export interface E2ERunRecord {
  runId: string
  status: E2ERunStatus
  createdAt: string
  updatedAt: string
  request: NormalizedCreateE2ERunRequest
  ids: E2ERunGeneratedIds
  payload: JsonObject
  steps: E2ERunStep[]
  pendingRegistration: PendingRegistrationResult | null
  sendResponse: JsonObject | null
  buildDiagnostic: JsonObject
}

export interface BuiltReservation {
  ids: E2ERunGeneratedIds
  reservation: JsonObject
  payload: JsonObject
  diagnostic: JsonObject
}

export interface LifecycleStepResult {
  step: E2ELifecycleStepName
  run: E2ERunRecord
  verifier: E2ERunVerifierResult
}

export interface E2ERunVerifierResult {
  reservationLookup: PmsClientResult<unknown>
  webhookEvents: PmsClientResult<unknown>
  autoMessageSendLogs: PmsClientResult<unknown> | null
  autoMessageMessages: PmsClientResult<unknown> | null
  autoMessageTemplate: PmsAutoMessageSummary | null
}

export interface E2EReadinessResponse {
  simulator: JsonObject
  readiness: PmsClientResult<PmsReadinessData>
  context: PmsClientResult<PmsReadinessData>
  ready: boolean
  missingRequirements: string[]
}

export interface E2EBootstrapResponse {
  token: string
  storeId: number
  suHotelId: string
  roomTypeId: number
  roomId: number
  readiness: E2EReadinessResponse
}
