import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 收款记录DTO
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

// 创建收款记录
export const createPayment = async (data: PaymentDTO): Promise<ApiResponse<PaymentDTO>> => {
  return await request.post('/payments', data)
}

// 根据预订ID获取收款记录列表
export const getPaymentsByReservationId = async (
  reservationId: number
): Promise<ApiResponse<PaymentDTO[]>> => {
  return await request.get(`/payments/reservation/${reservationId}`)
}

// 删除收款记录
export const deletePayment = async (id: number): Promise<ApiResponse<string>> => {
  return await request.delete(`/payments/${id}`)
}

// 根据预订ID获取总收款金额
export const getTotalPayment = async (reservationId: number): Promise<ApiResponse<number>> => {
  return await request.get(`/payments/reservation/${reservationId}/total`)
}