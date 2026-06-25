import { request } from '@/utils/request'

export type SmartLockProvider = 'SWITCHBOT' | 'TTLOCK'
export type RoomLockOperationAction = 'LOCK' | 'UNLOCK'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface RoomLockOperationContext {
  roomId: number
  roomNumber?: string
  roomType?: string
  date?: string
  reservationId?: number | null
  guestName?: string
  checkInDate?: string
  checkOutDate?: string
}

export interface RoomLockStatusDTO {
  roomId: number
  bindingId?: number
  deviceId?: number
  providerLockId?: string
  lockName?: string
  provider?: SmartLockProvider
  controlAvailable?: boolean
  passcodeAvailable?: boolean
  controlDeviceId?: number
  controlProviderLockId?: string
  controlDeviceName?: string
  controlLockName?: string
  controlProvider?: SmartLockProvider
  controlUnavailableReason?: string
  passcodeDeviceId?: number
  passcodeProviderLockId?: string
  passcodeDeviceName?: string
  passcodeLockName?: string
  passcodeProvider?: SmartLockProvider
  passcodeUnavailableReason?: string
  lockStatus?: string
  online?: boolean
  battery?: number
  lastStatusAt?: string
}

export interface RoomLockConfirmationRequest {
  action: RoomLockOperationAction
  bindingId?: number
  reason?: string
}

export interface RoomLockConfirmationDTO {
  roomId?: number
  bindingId?: number
  action?: RoomLockOperationAction
  confirmToken?: string
  expiresAt?: string
}

export interface RoomLockActionRequest {
  confirm: boolean
  confirmToken?: string
  bindingId?: number
  idempotencyKey: string
  reason?: string
}

export interface RoomLockActionResultDTO {
  id?: number
  taskType?: string
  provider?: SmartLockProvider
  roomId?: number
  bindingId?: number
  passcodeRecordId?: number
  providerTaskId?: string
  status?: string
  resultMessage?: string
  errorMessage?: string
  completedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface RoomLockPasscodeDTO {
  id: number
  roomId?: number
  bindingId?: number
  provider?: SmartLockProvider
  providerLockId?: string
  providerPasscodeId?: string
  providerTaskId?: string
  passcodeName?: string
  passcodeMasked?: string
  oneTimePasscode?: string
  validFrom?: string
  validUntil?: string
  status?: string
  lastError?: string
  createdAt?: string
  updatedAt?: string
}

export interface CreateRoomLockPasscodeRequest {
  passcodeName?: string
  passcode?: string
  validFrom: string
  validUntil: string
  idempotencyKey: string
}

const SMART_LOCK_REQUEST_TIMEOUT_MS = 60000
const SMART_LOCK_REQUEST_CONFIG = {
  suppressErrorToast: true,
  timeout: SMART_LOCK_REQUEST_TIMEOUT_MS,
}

export const getRoomLockStatus = async (
  roomId: number,
): Promise<ApiResponse<RoomLockStatusDTO | null>> => {
  return await request.get(`/smart-locks/rooms/${roomId}/status`, SMART_LOCK_REQUEST_CONFIG)
}

export const refreshRoomLockStatus = async (
  roomId: number,
): Promise<ApiResponse<RoomLockStatusDTO | null>> => {
  return await request.post(
    `/smart-locks/rooms/${roomId}/status/refresh`,
    {},
    SMART_LOCK_REQUEST_CONFIG,
  )
}

export const createRoomLockConfirmation = async (
  roomId: number,
  data: RoomLockConfirmationRequest,
): Promise<ApiResponse<RoomLockConfirmationDTO>> => {
  return await request.post(
    `/smart-locks/rooms/${roomId}/confirmations`,
    data,
    SMART_LOCK_REQUEST_CONFIG,
  )
}

export const unlockRoomLock = async (
  roomId: number,
  data: RoomLockActionRequest,
): Promise<ApiResponse<RoomLockActionResultDTO>> => {
  return await request.post(`/smart-locks/rooms/${roomId}/unlock`, data, SMART_LOCK_REQUEST_CONFIG)
}

export const lockRoomLock = async (
  roomId: number,
  data: RoomLockActionRequest,
): Promise<ApiResponse<RoomLockActionResultDTO>> => {
  return await request.post(`/smart-locks/rooms/${roomId}/lock`, data, SMART_LOCK_REQUEST_CONFIG)
}

export const getRoomLockPasscodes = async (
  roomId: number,
): Promise<ApiResponse<RoomLockPasscodeDTO[]>> => {
  return await request.get(`/smart-locks/rooms/${roomId}/passcodes`, SMART_LOCK_REQUEST_CONFIG)
}

export const createRoomLockPasscode = async (
  roomId: number,
  data: CreateRoomLockPasscodeRequest,
): Promise<ApiResponse<RoomLockPasscodeDTO>> => {
  return await request.post(
    `/smart-locks/rooms/${roomId}/passcodes`,
    data,
    SMART_LOCK_REQUEST_CONFIG,
  )
}

export const deleteRoomLockPasscode = async (
  recordId: number,
): Promise<ApiResponse<RoomLockPasscodeDTO>> => {
  return await request.delete(`/smart-locks/passcodes/${recordId}`, SMART_LOCK_REQUEST_CONFIG)
}
