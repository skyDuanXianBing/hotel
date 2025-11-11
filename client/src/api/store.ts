import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 门店基本信息DTO
export interface StoreDTO {
  id: number
  name: string
  phone: string
  type: string
  timezone: string
  manager: string
  ownerEmail: string
  address: string
  city: string
  state: string
  country: string
  currency: string
  logo?: string
  description?: string
  email?: string
  wechat?: string
  whatsapp?: string
  line?: string
  language?: string
  createdAt: string
  updatedAt: string
}

// 门店政策DTO
export interface StorePolicyDTO {
  id?: number
  storeId: number
  checkinTime: string
  checkoutTime: string
  childPolicy: string
  smokingPolicy: string
  petPolicy: string
  additionalRules: string
  hotelTerms: string
}

// 创建/更新门店请求
export interface StoreRequest {
  name: string
  phone?: string
  type?: string
  timezone?: string
  manager?: string
  ownerEmail?: string
  address?: string
  city?: string
  state?: string
  country?: string
  currency?: string
  logo?: string
  description?: string
  email?: string
  wechat?: string
  whatsapp?: string
  line?: string
  language?: string
}

/**
 * 获取所有门店
 */
export const getAllStores = async (): Promise<ApiResponse<StoreDTO[]>> => {
  return await request.get('/stores')
}

/**
 * 根据ID获取门店详情
 */
export const getStoreById = async (id: number): Promise<ApiResponse<StoreDTO>> => {
  return await request.get(`/stores/${id}`)
}

/**
 * 创建门店
 */
export const createStore = async (data: StoreRequest): Promise<ApiResponse<StoreDTO>> => {
  return await request.post('/stores', data)
}

/**
 * 更新门店
 */
export const updateStore = async (
  id: number,
  data: StoreRequest
): Promise<ApiResponse<StoreDTO>> => {
  return await request.put(`/stores/${id}`, data)
}

/**
 * 删除门店
 */
export const deleteStore = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/stores/${id}`)
}

/**
 * 获取门店政策
 */
export const getStorePolicy = async (storeId: number): Promise<ApiResponse<StorePolicyDTO>> => {
  return await request.get(`/stores/${storeId}/policy`)
}

/**
 * 更新或创建门店政策
 */
export const saveStorePolicy = async (
  storeId: number,
  data: Partial<StorePolicyDTO>
): Promise<ApiResponse<StorePolicyDTO>> => {
  return await request.put(`/stores/${storeId}/policy`, data)
}
