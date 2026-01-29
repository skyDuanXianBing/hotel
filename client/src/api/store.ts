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
  suHotelId?: string
  logo?: string
  description?: string
  email?: string
  wechat?: string
  whatsapp?: string
  line?: string
  language?: string
  userRole: string // owner, admin, member
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
  phone: string
  type?: string
  timezone?: string
  manager?: string
  country: string
  city?: string
  state?: string
  address?: string
  currency?: string
  suHotelId?: string
  createSuProperty?: boolean
  ownerEmail?: string
  language?: string
  description?: string
  logo?: string
  email?: string
  wechat?: string
  whatsapp?: string
  line?: string
}

// 添加成员请求
export interface AddStoreMemberRequest {
  email: string
  role: 'owner' | 'admin' | 'member'
  roleIds?: number[] // 权限角色ID列表
}

// 更新成员权限请求
export interface UpdateStoreMemberPermissionRequest {
  role?: 'owner' | 'admin' | 'member'
  roleIds?: number[]
  isActive?: boolean
}

// 角色信息
export interface RoleDTO {
  id: number
  name: string
  description?: string
  isSystem?: boolean
}

// 用户简化信息
export interface UserSimpleDTO {
  id: number
  username: string
  email: string
  nickname?: string
  avatar?: string
  isActive: boolean
}

// 门店成员详细信息（匹配后端StoreUserDTO）
export interface StoreUserDTO {
  id: number
  user: UserSimpleDTO
  role: string // 基础角色: owner, admin, member
  roles: RoleDTO[] // 权限角色列表
  isActive: boolean
  invitedBy?: number
  joinedAt: string
}

// 保留旧的接口以兼容现有代码
export type StoreMember = StoreUserDTO

/**
 * 获取当前用户的所有门店
 */
export const getUserStores = async (): Promise<ApiResponse<StoreDTO[]>> => {
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
 * 添加门店成员（支持权限角色）
 */
export const addStoreMember = async (
  storeId: number,
  data: AddStoreMemberRequest
): Promise<ApiResponse<StoreUserDTO>> => {
  return await request.post(`/stores/${storeId}/members`, data)
}

/**
 * 获取门店成员列表
 */
export const getStoreMembers = async (
  storeId: number
): Promise<ApiResponse<StoreUserDTO[]>> => {
  return await request.get(`/stores/${storeId}/members`)
}

/**
 * 获取门店成员详情
 */
export const getStoreMemberDetail = async (
  storeId: number,
  userId: number
): Promise<ApiResponse<StoreUserDTO>> => {
  return await request.get(`/stores/${storeId}/members/${userId}`)
}

/**
 * 更新门店成员权限
 */
export const updateStoreMemberPermission = async (
  storeId: number,
  userId: number,
  data: UpdateStoreMemberPermissionRequest
): Promise<ApiResponse<StoreUserDTO>> => {
  return await request.put(`/stores/${storeId}/members/${userId}`, data)
}

/**
 * 移除门店成员
 */
export const removeStoreMember = async (
  storeId: number,
  memberId: number
): Promise<ApiResponse<void>> => {
  return await request.delete(`/stores/${storeId}/members/${memberId}`)
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
