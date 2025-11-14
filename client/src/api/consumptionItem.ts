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
 * 获取所有消费项（门店级架构，自动从X-Store-Id请求头获取门店）
 */
export const getAllConsumptionItems = async (): Promise<ApiResponse<ConsumptionItemDTO[]>> => {
  return await request.get('/consumption-items')
}

/**
 * 根据ID获取消费项
 */
export const getConsumptionItemById = async (id: number): Promise<ApiResponse<ConsumptionItemDTO>> => {
  return await request.get(`/consumption-items/${id}`)
}

/**
 * 根据分类获取消费项（门店级架构）
 */
export const getConsumptionItemsByCategory = async (
  category: string
): Promise<ApiResponse<ConsumptionItemDTO[]>> => {
  return await request.get(`/consumption-items/category/${category}`)
}

/**
 * 根据启用状态获取消费项（门店级架构）
 */
export const getConsumptionItemsByEnabled = async (
  enabled: boolean
): Promise<ApiResponse<ConsumptionItemDTO[]>> => {
  return await request.get(`/consumption-items/enabled/${enabled}`)
}

/**
 * 创建消费项（门店级架构，storeId由后端自动注入）
 */
export const createConsumptionItem = async (
  data: ConsumptionItemDTO
): Promise<ApiResponse<ConsumptionItemDTO>> => {
  return await request.post('/consumption-items', data)
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
 * 获取所有分类（门店级架构）
 */
export const getAllConsumptionCategories = async (): Promise<ApiResponse<ConsumptionCategoryDTO[]>> => {
  return await request.get('/consumption-categories')
}

/**
 * 根据ID获取分类
 */
export const getConsumptionCategoryById = async (id: number): Promise<ApiResponse<ConsumptionCategoryDTO>> => {
  return await request.get(`/consumption-categories/${id}`)
}

/**
 * 创建分类（门店级架构）
 */
export const createConsumptionCategory = async (
  data: ConsumptionCategoryDTO
): Promise<ApiResponse<ConsumptionCategoryDTO>> => {
  return await request.post('/consumption-categories', data)
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
