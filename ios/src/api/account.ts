import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface AccountRoleDTO {
  id: number
  name: string
  description?: string
  isSystem?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface AccountDTO {
  id: number
  username: string
  name: string
  email: string
  nickname?: string
  avatar?: string
  gender?: string
  isActive: boolean
  roles: AccountRoleDTO[]
  createdAt: string
  updatedAt: string
}

export interface CreateAccountRequest {
  username: string
  name: string
  email: string
  password?: string
  roleIds?: number[]
}

export interface UpdateAccountRequest {
  name?: string
  email?: string
  nickname?: string
  avatar?: string
  gender?: string
  isActive?: boolean
  roleIds?: number[]
}

export interface BatchUpdateStatusRequest {
  accountIds: number[]
  isActive: boolean
}

export interface BatchUpdateRolesRequest {
  accountIds: number[]
  roleIds: number[]
}

export const getAllAccounts = (params?: {
  keyword?: string
  roleId?: number
  isActive?: boolean
}) => {
  return request<ApiResponse<AccountDTO[]>>({
    url: '/accounts',
    method: 'GET',
    params,
  })
}

export const getAccountById = (accountId: number) => {
  return request<ApiResponse<AccountDTO>>({
    url: `/accounts/${accountId}`,
    method: 'GET',
  })
}

export const createAccount = (data: CreateAccountRequest) => {
  return request<ApiResponse<AccountDTO>>({
    url: '/accounts',
    method: 'POST',
    data,
  })
}

export const updateAccount = (accountId: number, data: UpdateAccountRequest) => {
  return request<ApiResponse<AccountDTO>>({
    url: `/accounts/${accountId}`,
    method: 'PUT',
    data,
  })
}

export const deleteAccount = (accountId: number) => {
  return request<ApiResponse<void>>({
    url: `/accounts/${accountId}`,
    method: 'DELETE',
  })
}

export const batchDeleteAccounts = (accountIds: number[]) => {
  return request<ApiResponse<void>>({
    url: '/accounts/batch',
    method: 'DELETE',
    data: accountIds,
  })
}

export const updateAccountStatus = (accountId: number, isActive: boolean) => {
  return request<ApiResponse<AccountDTO>>({
    url: `/accounts/${accountId}/status`,
    method: 'PUT',
    params: { isActive },
  })
}

export const batchUpdateStatus = (data: BatchUpdateStatusRequest) => {
  return request<ApiResponse<void>>({
    url: '/accounts/batch/status',
    method: 'PUT',
    data,
  })
}

export const batchUpdateRoles = (data: BatchUpdateRolesRequest) => {
  return request<ApiResponse<void>>({
    url: '/accounts/batch/roles',
    method: 'PUT',
    data,
  })
}
