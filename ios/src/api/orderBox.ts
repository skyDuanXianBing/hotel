import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { ReservationDTO } from '@/api/reservation'

export interface OrderBoxItem {
  id: number
  reservationId: number
  reservation: ReservationDTO
  movedInAt: string
  movedInBy?: string
  notes?: string
}

export interface MoveToOrderBoxRequest {
  reservationId: number
  notes?: string
}

export interface MoveOutOrderBoxRequest {
  orderBoxItemId: number
  notes?: string
}

export interface OrderBoxStatistics {
  count: number
}

export interface OrderBoxMoveCheckResult {
  canMove: boolean
  reason?: string
}

export const getOrderBoxList = async () => {
  return request.get<ApiResponse<OrderBoxItem[]>>('/order-box')
}

export const moveToOrderBox = async (data: MoveToOrderBoxRequest) => {
  return request.post<ApiResponse<OrderBoxItem>>('/order-box/move-in', data)
}

export const moveOutOrderBox = async (data: MoveOutOrderBoxRequest) => {
  return request.post<ApiResponse<void>>('/order-box/move-out', data)
}

export const getOrderBoxStatistics = async () => {
  return request.get<ApiResponse<OrderBoxStatistics>>('/order-box/statistics')
}

export const checkCanMoveToOrderBox = async (reservationId: number) => {
  return request.get<ApiResponse<OrderBoxMoveCheckResult>>(`/order-box/check/${reservationId}`)
}
