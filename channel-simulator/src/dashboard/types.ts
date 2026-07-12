export type ViewId = 'readiness' | 'run' | 'lifecycle' | 'scenarios' | 'logs'
export type HttpMethod = 'GET' | 'POST' | 'DELETE'
export type JsonObject = Record<string, unknown>

export interface ApiEnvelope<T> {
  success?: boolean
  message?: string
  data?: T
}

export interface Credentials {
  token: string
  storeId: string
  testSupportKey: string
}

export interface ConfigSummary {
  port?: number
  pmsBaseUrl?: string
  defaultHotelId?: string
  pmsAuth?: {
    hasToken?: boolean
    storeId?: string | null
  }
  testSupportAuth?: {
    hasKey?: boolean
    source?: string | null
  }
  webhookAuth?: JsonObject
  suAuth?: JsonObject
}

export interface ScenarioSummary {
  name: string
  label: string
  description: string
  type: 'reservation' | 'messaging'
  mode: 'pull' | 'push' | null
  batchCount: number | null
}

export interface LogEntry {
  timestamp: string
  type: string
  method: string
  path: string
  requestBody: unknown
  responseBody: unknown
  statusCode: number | null
}

export interface ReadinessParts {
  ready: boolean | null
  missingRequirements: unknown[]
  channels: unknown[]
  roomTypes: unknown[]
  rooms: unknown[]
  suHotelId: string
}

export interface BootstrapData {
  token: string
  storeId: number
  suHotelId: string
  roomTypeId: number
  roomId: number
  cleanerSession: CleanerTestSession
  readiness: unknown
}

export interface CleanerTestSession {
  cleanerToken: string
  storeId: number
  cleanerId: number
  displayName: string
}

export interface RunForm {
  mode: string
  channel: string
  scenario: string
  roomTypeId: string
  roomId: string
  stayStartDays: string
}

export interface MessagingForm {
  channel: string
  message: string
  bookingId: string
  threadId: string
}

export interface LoadingState {
  config: boolean
  bootstrap: boolean
  logs: boolean
  readiness: boolean
  run: boolean
  lifecycle: boolean
  messaging: boolean
  scenarios: boolean
  sendScenario: boolean
}

export interface StatusText {
  credentials: string
  bootstrap: string
  config: string
  logs: string
  readiness: string
  run: string
  lifecycle: string
  messaging: string
  scenarios: string
  sendScenario: string
}

export interface ViewItem {
  id: ViewId
  title: string
  description: string
}
