import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface PaymentDTO {
  id?: number
  reservationId: number
  type: string
  paymentMethod: string
  amount: number
  date: string
  remark?: string
  createdBy?: string
  createdAt?: string
}

export const createPayment = async (data: PaymentDTO) => {
  return request.post<ApiResponse<PaymentDTO>>('/payments', data)
}

export const getPaymentsByReservationId = async (reservationId: number) => {
  return request.get<ApiResponse<PaymentDTO[]>>(`/payments/reservation/${reservationId}`)
}

export const deletePayment = async (id: number) => {
  return request.delete<ApiResponse<string>>(`/payments/${id}`)
}

export const getTotalPayment = async (reservationId: number) => {
  return request.get<ApiResponse<number>>(`/payments/reservation/${reservationId}/total`)
}
