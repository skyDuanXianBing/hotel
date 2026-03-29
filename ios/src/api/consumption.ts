import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

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

export const createConsumption = async (data: ConsumptionDTO) => {
  return request.post<ApiResponse<ConsumptionDTO>>('/consumptions', data)
}

export const getConsumptionsByReservationId = async (reservationId: number) => {
  return request.get<ApiResponse<ConsumptionDTO[]>>(`/consumptions/reservation/${reservationId}`)
}

export const deleteConsumption = async (id: number) => {
  return request.delete<ApiResponse<string>>(`/consumptions/${id}`)
}

export const getTotalConsumption = async (reservationId: number) => {
  return request.get<ApiResponse<number>>(`/consumptions/reservation/${reservationId}/total`)
}
