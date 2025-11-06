import { request } from '@/utils/request'
import type { ReservationDTO } from './reservation'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 订单盒子项
export interface OrderBoxItem {
  id: number
  reservationId: number
  reservation: ReservationDTO
  movedInAt: string
  movedInBy?: string
  notes?: string
}

// 移入订单盒子请求
export interface MoveToOrderBoxRequest {
  reservationId: number
  notes?: string
}

// 移出订单盒子请求
export interface MoveOutOrderBoxRequest {
  orderBoxItemId: number
  notes?: string
}

// 获取订单盒子列表
export const getOrderBoxList = async (): Promise<ApiResponse<OrderBoxItem[]>> => {
  return await request.get('/order-box')
}

// 移入订单盒子
export const moveToOrderBox = async (
  data: MoveToOrderBoxRequest
): Promise<ApiResponse<OrderBoxItem>> => {
  return await request.post('/order-box/move-in', data)
}

// 移出订单盒子
export const moveOutOrderBox = async (
  data: MoveOutOrderBoxRequest
): Promise<ApiResponse<void>> => {
  return await request.post('/order-box/move-out', data)
}

// 获取订单盒子统计
export const getOrderBoxStatistics = async (): Promise<
  ApiResponse<{ count: number }>
> => {
  return await request.get('/order-box/statistics')
}

// 检查订单是否可以移入订单盒子
export const checkCanMoveToOrderBox = async (
  reservationId: number
): Promise<ApiResponse<{ canMove: boolean; reason?: string }>> => {
  return await request.get(`/order-box/check/${reservationId}`)
}