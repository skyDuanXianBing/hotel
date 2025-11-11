import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 保洁配置DTO
export interface CleaningConfigDTO {
  id: number
  userId: number
  storeId: number
  enabled: boolean
  stayStartTime: string
  stayEndTime: string
  checkoutStartTime: string
  checkoutEndTime: string
  autoStayTask: boolean
  autoCheckoutTask: boolean
  createdAt: string
  updatedAt: string
}

// 保洁员DTO
export interface CleanerDTO {
  id: number
  userId: number
  storeId: number
  name: string
  email: string
  createdAt: string
  updatedAt: string
}

// 易耗品DTO
export interface CleaningSupplyDTO {
  id: number
  userId: number
  roomType: string
  supplies: string
  createdAt: string
  updatedAt: string
}

// 请求数据类型
export interface CleaningConfigRequest {
  enabled: boolean
  stayStartTime: string
  stayEndTime: string
  checkoutStartTime: string
  checkoutEndTime: string
  autoStayTask: boolean
  autoCheckoutTask: boolean
}

export interface CleanerRequest {
  userId: number
  storeId: number
  name: string
  email: string
}

export interface CleaningSupplyRequest {
  userId: number
  roomType: string
  supplies: string
}

// ==================== 保洁配置 API ====================

/**
 * 根据用户ID获取保洁配置列表
 */
export const getCleaningConfigsByUserId = async (
  userId: number
): Promise<ApiResponse<CleaningConfigDTO[]>> => {
  return await request.get(`/cleaning-configs/user/${userId}`)
}

/**
 * 根据用户ID和门店ID获取或创建保洁配置
 */
export const getOrCreateCleaningConfig = async (
  userId: number,
  storeId: number
): Promise<ApiResponse<CleaningConfigDTO>> => {
  return await request.get(`/cleaning-configs/user/${userId}/store/${storeId}`)
}

/**
 * 更新保洁配置
 */
export const updateCleaningConfig = async (
  id: number,
  data: CleaningConfigRequest
): Promise<ApiResponse<CleaningConfigDTO>> => {
  return await request.put(`/cleaning-configs/${id}`, data)
}

/**
 * 删除保洁配置
 */
export const deleteCleaningConfig = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/cleaning-configs/${id}`)
}

// ==================== 保洁员 API ====================

/**
 * 根据用户ID和门店ID获取保洁员列表
 */
export const getCleanersByUserIdAndStoreId = async (
  userId: number,
  storeId: number
): Promise<ApiResponse<CleanerDTO[]>> => {
  return await request.get(`/cleaners/user/${userId}/store/${storeId}`)
}

/**
 * 根据用户ID获取保洁员列表
 */
export const getCleanersByUserId = async (
  userId: number
): Promise<ApiResponse<CleanerDTO[]>> => {
  return await request.get(`/cleaners/user/${userId}`)
}

/**
 * 创建保洁员
 */
export const createCleaner = async (
  data: CleanerRequest
): Promise<ApiResponse<CleanerDTO>> => {
  return await request.post('/cleaners', data)
}

/**
 * 更新保洁员
 */
export const updateCleaner = async (
  id: number,
  data: CleanerRequest
): Promise<ApiResponse<CleanerDTO>> => {
  return await request.put(`/cleaners/${id}`, data)
}

/**
 * 删除保洁员
 */
export const deleteCleaner = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/cleaners/${id}`)
}

// ==================== 易耗品 API ====================

/**
 * 根据用户ID获取易耗品列表
 */
export const getCleaningSuppliesByUserId = async (
  userId: number
): Promise<ApiResponse<CleaningSupplyDTO[]>> => {
  return await request.get(`/cleaning-supplies/user/${userId}`)
}

/**
 * 创建易耗品
 */
export const createCleaningSupply = async (
  data: CleaningSupplyRequest
): Promise<ApiResponse<CleaningSupplyDTO>> => {
  return await request.post('/cleaning-supplies', data)
}

/**
 * 更新易耗品
 */
export const updateCleaningSupply = async (
  id: number,
  data: CleaningSupplyRequest
): Promise<ApiResponse<CleaningSupplyDTO>> => {
  return await request.put(`/cleaning-supplies/${id}`, data)
}

/**
 * 删除易耗品
 */
export const deleteCleaningSupply = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/cleaning-supplies/${id}`)
}

/**
 * 清空易耗品内容
 */
export const clearCleaningSupply = async (
  id: number
): Promise<ApiResponse<CleaningSupplyDTO>> => {
  return await request.put(`/cleaning-supplies/${id}/clear`)
}
