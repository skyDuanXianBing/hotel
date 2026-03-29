import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface ConsumptionItemDTO {
  id?: number
  userId?: number
  category: string
  name: string
  price: number
  enabled: boolean
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface ConsumptionCategoryDTO {
  id?: number
  userId?: number
  name: string
  description?: string
  count?: number
  createdAt?: string
  updatedAt?: string
}

export const getAllConsumptionItems = () => {
  return request<ApiResponse<ConsumptionItemDTO[]>>({
    url: '/consumption-items',
    method: 'GET',
  })
}

export const getConsumptionItemById = (itemId: number) => {
  return request<ApiResponse<ConsumptionItemDTO>>({
    url: `/consumption-items/${itemId}`,
    method: 'GET',
  })
}

export const createConsumptionItem = (data: ConsumptionItemDTO) => {
  return request<ApiResponse<ConsumptionItemDTO>>({
    url: '/consumption-items',
    method: 'POST',
    data,
  })
}

export const updateConsumptionItem = (itemId: number, data: ConsumptionItemDTO) => {
  return request<ApiResponse<ConsumptionItemDTO>>({
    url: `/consumption-items/${itemId}`,
    method: 'PUT',
    data,
  })
}

export const updateConsumptionItemEnabled = (itemId: number, enabled: boolean) => {
  return request<ApiResponse<ConsumptionItemDTO>>({
    url: `/consumption-items/${itemId}/enabled`,
    method: 'PATCH',
    params: { enabled },
  })
}

export const deleteConsumptionItem = (itemId: number) => {
  return request<ApiResponse<void>>({
    url: `/consumption-items/${itemId}`,
    method: 'DELETE',
  })
}

export const getAllConsumptionCategories = () => {
  return request<ApiResponse<ConsumptionCategoryDTO[]>>({
    url: '/consumption-categories',
    method: 'GET',
  })
}

export const createConsumptionCategory = (data: ConsumptionCategoryDTO) => {
  return request<ApiResponse<ConsumptionCategoryDTO>>({
    url: '/consumption-categories',
    method: 'POST',
    data,
  })
}

export const updateConsumptionCategory = (categoryId: number, data: ConsumptionCategoryDTO) => {
  return request<ApiResponse<ConsumptionCategoryDTO>>({
    url: `/consumption-categories/${categoryId}`,
    method: 'PUT',
    data,
  })
}

export const deleteConsumptionCategory = (categoryId: number) => {
  return request<ApiResponse<void>>({
    url: `/consumption-categories/${categoryId}`,
    method: 'DELETE',
  })
}
