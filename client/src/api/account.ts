import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 角色DTO
export interface RoleDTO {
  id: number
  name: string
  description?: string
  isSystem?: boolean
  createdAt?: string
  updatedAt?: string
}

// 账号DTO
export interface AccountDTO {
  id: number
  username: string
  name: string
  email: string
  nickname?: string
  avatar?: string
  gender?: string
  isActive: boolean
  roles: RoleDTO[]
  createdAt: string
  updatedAt: string
}

// 创建账号请求
export interface CreateAccountRequest {
  username: string
  name: string
  email: string
  password?: string
  roleIds?: number[]
}

// 更新账号请求
export interface UpdateAccountRequest {
  name?: string
  email?: string
  nickname?: string
  avatar?: string
  gender?: string
  isActive?: boolean
  roleIds?: number[]
}

// 批量更新状态请求
export interface BatchUpdateStatusRequest {
  accountIds: number[]
  isActive: boolean
}

// 批量调整角色请求
export interface BatchUpdateRolesRequest {
  accountIds: number[]
  roleIds: number[]
}

/**
 * 获取所有账号(支持筛选)
 */
export const getAllAccounts = async (params?: {
  keyword?: string
  roleId?: number
  isActive?: boolean
}): Promise<ApiResponse<AccountDTO[]>> => {
  return await request.get('/accounts', { params })
}

/**
 * 根据ID获取账号详情
 */
export const getAccountById = async (id: number): Promise<ApiResponse<AccountDTO>> => {
  return await request.get(`/accounts/${id}`)
}

/**
 * 创建账号
 */
export const createAccount = async (
  data: CreateAccountRequest
): Promise<ApiResponse<AccountDTO>> => {
  return await request.post('/accounts', data)
}

/**
 * 更新账号
 */
export const updateAccount = async (
  id: number,
  data: UpdateAccountRequest
): Promise<ApiResponse<AccountDTO>> => {
  return await request.put(`/accounts/${id}`, data)
}

/**
 * 删除账号
 */
export const deleteAccount = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/accounts/${id}`)
}

/**
 * 批量删除账号
 */
export const batchDeleteAccounts = async (accountIds: number[]): Promise<ApiResponse<void>> => {
  return await request.delete('/accounts/batch', { data: accountIds })
}

/**
 * 更新账号状态
 */
export const updateAccountStatus = async (
  id: number,
  isActive: boolean
): Promise<ApiResponse<AccountDTO>> => {
  return await request.put(`/accounts/${id}/status`, null, { params: { isActive } })
}

/**
 * 批量更新账号状态
 */
export const batchUpdateStatus = async (
  data: BatchUpdateStatusRequest
): Promise<ApiResponse<void>> => {
  return await request.put('/accounts/batch/status', data)
}

/**
 * 批量调整角色
 */
export const batchUpdateRoles = async (
  data: BatchUpdateRolesRequest
): Promise<ApiResponse<void>> => {
  return await request.put('/accounts/batch/roles', data)
}
