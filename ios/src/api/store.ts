import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { StoreDTO, StoreMember, StoreRequest } from '@/types/store'
import type { StorePolicyDTO } from '@/types/settings'

export interface DeleteStoreErrorData {
  code?: string
}

export interface PermissionDTO {
  id?: number
  module: string
  action: string
  roomTypeId?: number
  roomTypeName?: string
  allRoomTypes?: boolean
}

export interface AddStoreMemberRequest {
  email: string
  role: 'owner' | 'admin' | 'member'
  roleIds?: number[]
  extraPermissions?: PermissionDTO[]
}

export interface UpdateStoreMemberPermissionRequest {
  role?: 'owner' | 'admin' | 'member'
  roleIds?: number[]
  isActive?: boolean
  extraPermissions?: PermissionDTO[]
}

export interface TransferStoreOwnerRequest {
  targetUserId: number
}

export const getUserStores = () => {
  return request<ApiResponse<StoreDTO[]>>({
    url: '/stores',
    method: 'GET',
  })
}

export const getStoreById = (storeId: number) => {
  return request<ApiResponse<StoreDTO>>({
    url: `/stores/${storeId}`,
    method: 'GET',
  })
}

export const createStore = (data: StoreRequest) => {
  return request<ApiResponse<StoreDTO>>({
    url: '/stores',
    method: 'POST',
    data,
  })
}

export const updateStore = (storeId: number, data: StoreRequest) => {
  return request<ApiResponse<StoreDTO>>({
    url: `/stores/${storeId}`,
    method: 'PUT',
    data,
  })
}

export const deleteStore = (storeId: number) => {
  return request<ApiResponse<DeleteStoreErrorData | null>>({
    url: `/stores/${storeId}`,
    method: 'DELETE',
  })
}

export const getStoreMembers = (storeId: number) => {
  return request<ApiResponse<StoreMember[]>>({
    url: `/stores/${storeId}/members`,
    method: 'GET',
  })
}

export const addStoreMember = (storeId: number, data: AddStoreMemberRequest) => {
  return request<ApiResponse<StoreMember>>({
    url: `/stores/${storeId}/members`,
    method: 'POST',
    data,
  })
}

export const getStoreMemberDetail = (storeId: number, memberId: number) => {
  return request<ApiResponse<StoreMember>>({
    url: `/stores/${storeId}/members/${memberId}`,
    method: 'GET',
  })
}

export const updateStoreMemberPermission = (
  storeId: number,
  memberId: number,
  data: UpdateStoreMemberPermissionRequest,
) => {
  return request<ApiResponse<StoreMember>>({
    url: `/stores/${storeId}/members/${memberId}`,
    method: 'PUT',
    data,
  })
}

export const removeStoreMember = (storeId: number, memberId: number) => {
  return request<ApiResponse<void>>({
    url: `/stores/${storeId}/members/${memberId}`,
    method: 'DELETE',
  })
}

export const transferStoreOwner = (storeId: number, data: TransferStoreOwnerRequest) => {
  return request<ApiResponse<void>>({
    url: `/stores/${storeId}/owner/transfer`,
    method: 'POST',
    data,
  })
}

export const getStorePolicy = (storeId: number) => {
  return request<ApiResponse<StorePolicyDTO>>({
    url: `/stores/${storeId}/policy`,
    method: 'GET',
  })
}

export const updateStorePolicy = (storeId: number, data: StorePolicyDTO) => {
  return request<ApiResponse<StorePolicyDTO>>({
    url: `/stores/${storeId}/policy`,
    method: 'PUT',
    data,
  })
}
