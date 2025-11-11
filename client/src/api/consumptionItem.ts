import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 消费项DTO
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

// 消费项分类DTO
export interface ConsumptionCategoryDTO {
  id?: number
  userId?: number
  name: string
  description?: string
  count?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 获取所有消费项
 */
export const getAllConsumptionItems = async (userId: number): Promise<ApiResponse<ConsumptionItemDTO[]>> => {
  return await request.get('/consumption-items', { params: { userId } })
}

/**
 * 根据ID获取消费项
 */
export const getConsumptionItemById = async (id: number): Promise<ApiResponse<ConsumptionItemDTO>> => {
  return await request.get(`/consumption-items/${id}`)
}

/**
 * 根据分类获取消费项
 */
export const getConsumptionItemsByCategory = async (
  userId: number,
  category: string
): Promise<ApiResponse<ConsumptionItemDTO[]>> => {
  return await request.get(`/consumption-items/category/${category}`, { params: { userId } })
}

/**
 * 根据启用状态获取消费项
 */
export const getConsumptionItemsByEnabled = async (
  userId: number,
  enabled: boolean
): Promise<ApiResponse<ConsumptionItemDTO[]>> => {
  return await request.get(`/consumption-items/enabled/${enabled}`, { params: { userId } })
}

/**
 * 创建消费项
 */
export const createConsumptionItem = async (
  userId: number,
  data: ConsumptionItemDTO
): Promise<ApiResponse<ConsumptionItemDTO>> => {
  return await request.post('/consumption-items', data, { params: { userId } })
}

/**
 * 更新消费项
 */
export const updateConsumptionItem = async (
  id: number,
  data: ConsumptionItemDTO
): Promise<ApiResponse<ConsumptionItemDTO>> => {
  return await request.put(`/consumption-items/${id}`, data)
}

/**
 * 更新消费项启用状态
 */
export const updateConsumptionItemEnabled = async (
  id: number,
  enabled: boolean
): Promise<ApiResponse<ConsumptionItemDTO>> => {
  return await request.patch(`/consumption-items/${id}/enabled`, null, { params: { enabled } })
}

/**
 * 删除消费项
 */
export const deleteConsumptionItem = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/consumption-items/${id}`)
}

/**
 * 获取所有分类
 */
export const getAllConsumptionCategories = async (userId: number): Promise<ApiResponse<ConsumptionCategoryDTO[]>> => {
  return await request.get('/consumption-categories', { params: { userId } })
}

/**
 * 根据ID获取分类
 */
export const getConsumptionCategoryById = async (id: number): Promise<ApiResponse<ConsumptionCategoryDTO>> => {
  return await request.get(`/consumption-categories/${id}`)
}

/**
 * 创建分类
 */
export const createConsumptionCategory = async (
  userId: number,
  data: ConsumptionCategoryDTO
): Promise<ApiResponse<ConsumptionCategoryDTO>> => {
  return await request.post('/consumption-categories', data, { params: { userId } })
}

/**
 * 更新分类
 */
export const updateConsumptionCategory = async (
  id: number,
  data: ConsumptionCategoryDTO
): Promise<ApiResponse<ConsumptionCategoryDTO>> => {
  return await request.put(`/consumption-categories/${id}`, data)
}

/**
 * 删除分类
 */
export const deleteConsumptionCategory = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/consumption-categories/${id}`)
}
