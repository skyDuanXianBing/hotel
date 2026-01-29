import { request } from '@/utils/request'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export type SuWebhookEventStatus = 'RECEIVED' | 'PROCESSING' | 'PROCESSED' | 'FAILED' | 'DEAD'
export type SuWebhookEventType = 'PULL' | 'PUSH'

export interface SuReservationWebhookEventDTO {
  id: number
  storeId: number
  hotelId: string
  reservationNotifId: string
  eventType: SuWebhookEventType
  payloadJson?: string | null
  status: SuWebhookEventStatus
  retryCount: number
  nextRetryAt?: string | null
  lastError?: string | null
  createdAt?: string
  updatedAt?: string
}

export const getSuWebhookEvents = async (params: {
  status?: SuWebhookEventStatus
  size?: number
}): Promise<ApiResponse<SuReservationWebhookEventDTO[]>> => {
  return await request.get('/su/webhook-events', { params })
}

export const processSuWebhookEvents = async (params?: {
  limit?: number
}): Promise<ApiResponse<number>> => {
  return await request.post('/su/webhook-events/process', null, { params })
}

