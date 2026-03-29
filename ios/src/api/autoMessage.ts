import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export type RoomSelectionType = 'ALL_LOCAL' | 'BY_ROOM_TYPE' | 'BY_GROUP' | 'BY_ROOM'
export type AutoMessageAction = 'BOOKING_CONFIRM' | 'CHECK_IN' | 'CHECK_OUT'
export type SendTiming =
  | 'IMMEDIATELY'
  | '5_MIN'
  | '10_MIN'
  | '15_MIN'
  | '30_MIN'
  | '1_HOUR'
  | '2_HOUR'
  | '4_HOUR'
  | '8_HOUR'
  | '16_HOUR'
  | '24_HOUR'
  | `DAY_${number}_${string}`

export interface AutoMessageDTO {
  id: number
  userId: number
  title: string
  message: string
  automationRule: string
  channel: string
  channels: string
  resendOnExpire: boolean
  room: string
  roomSelectionType: RoomSelectionType
  roomSelection: string
  action: AutoMessageAction
  sendTiming: SendTiming
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface AutoMessageRequest {
  title: string
  message: string
  automationRule?: string
  channel?: string
  channels: string
  resendOnExpire: boolean
  room?: string
  roomSelectionType: RoomSelectionType
  roomSelection: string
  action: AutoMessageAction
  sendTiming: SendTiming
  enabled: boolean
}

export const getAllAutoMessages = () => {
  return request<ApiResponse<AutoMessageDTO[]>>({
    url: '/auto-messages',
    method: 'GET',
  })
}

export const getAutoMessageById = (messageId: number) => {
  return request<ApiResponse<AutoMessageDTO>>({
    url: `/auto-messages/${messageId}`,
    method: 'GET',
  })
}

export const createAutoMessage = (data: AutoMessageRequest) => {
  return request<ApiResponse<AutoMessageDTO>>({
    url: '/auto-messages',
    method: 'POST',
    data,
  })
}

export const updateAutoMessage = (messageId: number, data: AutoMessageRequest) => {
  return request<ApiResponse<AutoMessageDTO>>({
    url: `/auto-messages/${messageId}`,
    method: 'PUT',
    data,
  })
}

export const deleteAutoMessage = (messageId: number) => {
  return request<ApiResponse<void>>({
    url: `/auto-messages/${messageId}`,
    method: 'DELETE',
  })
}

export const toggleAutoMessage = (messageId: number) => {
  return request<ApiResponse<AutoMessageDTO>>({
    url: `/auto-messages/${messageId}/toggle`,
    method: 'PUT',
  })
}
