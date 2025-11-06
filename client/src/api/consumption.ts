import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 消费记录DTO
export interface ConsumptionDTO {
  id?: number
  reservationId: number
  item: string
  quantity: number
  amount: number
  date: string
  remark?: string
  createdBy?: string
  createdAt?: string
}

// 创建消费记录
export const createConsumption = async (data: ConsumptionDTO): Promise<ApiResponse<ConsumptionDTO>> => {
  return await request.post('/consumptions', data)
}

// 根据预订ID获取消费记录列表
export const getConsumptionsByReservationId = async (
  reservationId: number
): Promise<ApiResponse<ConsumptionDTO[]>> => {
  return await request.get(`/consumptions/reservation/${reservationId}`)
}

// 删除消费记录
export const deleteConsumption = async (id: number): Promise<ApiResponse<string>> => {
  return await request.delete(`/consumptions/${id}`)
}

// 根据预订ID获取总消费金额
export const getTotalConsumption = async (reservationId: number): Promise<ApiResponse<number>> => {
  return await request.get(`/consumptions/reservation/${reservationId}/total`)
}