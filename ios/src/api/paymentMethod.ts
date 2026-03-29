import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type {
  PaymentMethodDTO,
  PaymentMethodOrderRequest,
  UpsertPaymentMethodRequest,
} from '@/types/settings'

export const getAllPaymentMethods = () => {
  return request<ApiResponse<PaymentMethodDTO[]>>({
    url: '/payment-methods',
    method: 'GET',
  })
}

export const createPaymentMethod = (data: UpsertPaymentMethodRequest) => {
  return request<ApiResponse<PaymentMethodDTO>>({
    url: '/payment-methods',
    method: 'POST',
    data,
  })
}

export const updatePaymentMethod = (id: number, data: UpsertPaymentMethodRequest) => {
  return request<ApiResponse<PaymentMethodDTO>>({
    url: `/payment-methods/${id}`,
    method: 'PUT',
    data,
  })
}

export const updatePaymentMethodEnabled = (id: number, enabled: boolean) => {
  return request<ApiResponse<PaymentMethodDTO>>({
    url: `/payment-methods/${id}/enabled`,
    method: 'PATCH',
    params: { enabled },
  })
}

export const deletePaymentMethod = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/payment-methods/${id}`,
    method: 'DELETE',
  })
}

export const updatePaymentMethodsOrder = (data: PaymentMethodOrderRequest[]) => {
  return request<ApiResponse<PaymentMethodDTO[]>>({
    url: '/payment-methods/order',
    method: 'PUT',
    data,
  })
}
