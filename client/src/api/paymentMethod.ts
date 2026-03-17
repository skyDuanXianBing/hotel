import { request } from '@/utils/request'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface PaymentMethodDTO {
  id: number
  storeId: number
  name: string
  displayOrder: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface UpsertPaymentMethodRequest {
  name: string
  enabled?: boolean
}

export interface PaymentMethodOrderRequest {
  id: number
  displayOrder: number
}

export const getAllPaymentMethods = async (): Promise<ApiResponse<PaymentMethodDTO[]>> => {
  return await request.get('/payment-methods')
}

export const getEnabledPaymentMethods = async (): Promise<ApiResponse<PaymentMethodDTO[]>> => {
  return await request.get('/payment-methods/enabled')
}

export const createPaymentMethod = async (
  data: UpsertPaymentMethodRequest,
): Promise<ApiResponse<PaymentMethodDTO>> => {
  return await request.post('/payment-methods', data)
}

export const updatePaymentMethod = async (
  id: number,
  data: UpsertPaymentMethodRequest,
): Promise<ApiResponse<PaymentMethodDTO>> => {
  return await request.put(`/payment-methods/${id}`, data)
}

export const updatePaymentMethodEnabled = async (
  id: number,
  enabled: boolean,
): Promise<ApiResponse<PaymentMethodDTO>> => {
  return await request.patch(`/payment-methods/${id}/enabled`, null, {
    params: { enabled },
  })
}

export const deletePaymentMethod = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/payment-methods/${id}`)
}

export const updatePaymentMethodsOrder = async (
  data: PaymentMethodOrderRequest[],
): Promise<ApiResponse<PaymentMethodDTO[]>> => {
  return await request.put('/payment-methods/order', data)
}
