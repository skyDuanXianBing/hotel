import { request } from '@/utils/request'

export type DoorLockProvider = 'SWITCHBOT' | 'TTLOCK'
export type DoorLockStatusSource = 'DEVICE' | 'BOUND_LOCK' | 'UNAVAILABLE'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface DoorLockMaskedCredentials {
  token?: string
  secret?: string
  clientId?: string
  clientSecret?: string
  username?: string
  password?: string
  accessToken?: string
}

export interface DoorLockIntegrationDTO {
  id: number
  provider: DoorLockProvider
  name?: string
  enabled?: boolean
  connectionStatus?: string
  credentialsConfigured?: boolean
  maskedCredentials?: DoorLockMaskedCredentials
  lastTestAt?: string
  lastSyncAt?: string
  tokenExpiresAt?: string
  lastError?: string
  createdAt?: string
  updatedAt?: string
}

export interface DoorLockIntegrationPayload {
  provider: DoorLockProvider
  enabled: boolean
  name?: string
  switchBotToken?: string
  switchBotSecret?: string
  ttLockClientId?: string
  ttLockClientSecret?: string
  ttLockUsername?: string
  ttLockPassword?: string
  ttLockPasswordMd5?: string
}

export interface DoorLockTestResult {
  success: boolean
  message?: string
}

export interface DoorLockDeviceDTO {
  id: number
  integrationId?: number
  provider: DoorLockProvider
  providerDeviceId?: string
  providerLockId?: string
  lockName?: string
  deviceType?: string
  auxiliaryDeviceId?: string
  statusSource?: DoorLockStatusSource
  statusSourceDeviceId?: string
  battery?: number
  lockStatus?: string
  online?: boolean
  lastSyncedAt?: string
  lastStatusAt?: string
  updatedAt?: string
}

export interface DoorLockBindingDTO {
  id: number
  provider: DoorLockProvider
  roomId: number
  roomNumber?: string
  roomTypeId?: number
  roomTypeName?: string
  deviceId: number
  providerLockId?: string
  lockName?: string
  integrationId?: number
  status?: string
  createdAt?: string
  updatedAt?: string
}

export interface DoorLockRoomDTO {
  id?: number
  roomId?: number
  roomNumber: string
  roomName?: string
  roomTypeId?: number
  roomTypeName?: string
  roomType?: {
    id?: number
    name?: string
    code?: string
  }
  binding?: DoorLockBindingDTO | null
  lockBinding?: DoorLockBindingDTO | null
}

export interface DoorLockRoomQuery {
  provider: DoorLockProvider
  roomTypeId?: number
}

export interface DoorLockDeviceQuery {
  provider: DoorLockProvider
}

export interface DoorLockBindingPayload {
  integrationId?: number
  roomId: number
  deviceId?: number
  provider?: DoorLockProvider
  providerLockId?: string
}

const SMART_LOCK_BASE_PATH = '/smart-locks'
const SMART_LOCK_REQUEST_TIMEOUT_MS = 60000
const SMART_LOCK_REQUEST_CONFIG = {
  suppressErrorToast: true,
}
const SMART_LOCK_LONG_REQUEST_CONFIG = {
  ...SMART_LOCK_REQUEST_CONFIG,
  timeout: SMART_LOCK_REQUEST_TIMEOUT_MS,
}

export const getDoorLockIntegrations = async (): Promise<
  ApiResponse<DoorLockIntegrationDTO[]>
> => {
  return await request.get(`${SMART_LOCK_BASE_PATH}/integrations`, SMART_LOCK_REQUEST_CONFIG)
}

export const createDoorLockIntegration = async (
  payload: DoorLockIntegrationPayload,
): Promise<ApiResponse<DoorLockIntegrationDTO>> => {
  return await request.post(`${SMART_LOCK_BASE_PATH}/integrations`, payload, SMART_LOCK_REQUEST_CONFIG)
}

export const updateDoorLockIntegration = async (
  id: number,
  payload: DoorLockIntegrationPayload,
): Promise<ApiResponse<DoorLockIntegrationDTO>> => {
  return await request.put(
    `${SMART_LOCK_BASE_PATH}/integrations/${id}`,
    payload,
    SMART_LOCK_REQUEST_CONFIG,
  )
}

export const testDoorLockIntegration = async (
  id: number,
): Promise<ApiResponse<DoorLockTestResult>> => {
  return await request.post(
    `${SMART_LOCK_BASE_PATH}/integrations/${id}/test`,
    {},
    SMART_LOCK_LONG_REQUEST_CONFIG,
  )
}

export const refreshDoorLockToken = async (
  id: number,
): Promise<ApiResponse<DoorLockIntegrationDTO>> => {
  return await request.post(
    `${SMART_LOCK_BASE_PATH}/integrations/${id}/refresh-token`,
    {},
    SMART_LOCK_LONG_REQUEST_CONFIG,
  )
}

export const syncDoorLockDevices = async (
  id: number,
): Promise<ApiResponse<DoorLockDeviceDTO[]>> => {
  return await request.post(
    `${SMART_LOCK_BASE_PATH}/integrations/${id}/devices/sync`,
    {},
    SMART_LOCK_LONG_REQUEST_CONFIG,
  )
}

export const getDoorLockDevices = async (
  params: DoorLockDeviceQuery,
): Promise<ApiResponse<DoorLockDeviceDTO[]>> => {
  return await request.get(`${SMART_LOCK_BASE_PATH}/devices`, {
    ...SMART_LOCK_REQUEST_CONFIG,
    params,
  })
}

export const getDoorLockRooms = async (
  params: DoorLockRoomQuery,
): Promise<ApiResponse<DoorLockRoomDTO[]>> => {
  return await request.get(`${SMART_LOCK_BASE_PATH}/rooms`, {
    ...SMART_LOCK_REQUEST_CONFIG,
    params,
  })
}

export const saveDoorLockBinding = async (
  payload: DoorLockBindingPayload,
): Promise<ApiResponse<DoorLockBindingDTO>> => {
  return await request.post(`${SMART_LOCK_BASE_PATH}/bindings`, payload, SMART_LOCK_REQUEST_CONFIG)
}

export const deleteDoorLockBinding = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`${SMART_LOCK_BASE_PATH}/bindings/${id}`, SMART_LOCK_REQUEST_CONFIG)
}
