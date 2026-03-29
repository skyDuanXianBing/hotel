import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export enum PermissionModule {
  ACCOMMODATION = 'ACCOMMODATION',
  ORDER = 'ORDER',
  CHANNEL = 'CHANNEL',
  CUSTOMER = 'CUSTOMER',
  STATISTICS = 'STATISTICS',
  SETTINGS = 'SETTINGS',
  DATA_CENTER = 'DATA_CENTER',
  SENSITIVE = 'SENSITIVE',
}

export enum PermissionAction {
  VIEW_ROOM_STATUS = 'VIEW_ROOM_STATUS',
  EDIT_ROOM_STATUS = 'EDIT_ROOM_STATUS',
  VIEW_ROOM_OPERATION_LOG = 'VIEW_ROOM_OPERATION_LOG',
  VIEW_ROOM_INFO = 'VIEW_ROOM_INFO',
  ROOM_SHARE = 'ROOM_SHARE',
  VIEW_ROOM_PRICE = 'VIEW_ROOM_PRICE',
  EDIT_ROOM_PRICE = 'EDIT_ROOM_PRICE',
  VIEW_PRICE_LOG = 'VIEW_PRICE_LOG',
  BATCH_CHANGE_PRICE = 'BATCH_CHANGE_PRICE',
  BREAKFAST_PACKAGE = 'BREAKFAST_PACKAGE',
  RESERVATION_CALENDAR = 'RESERVATION_CALENDAR',
  TASK_LIST = 'TASK_LIST',
  VIEW_ORDERS = 'VIEW_ORDERS',
  CREATE_ORDER = 'CREATE_ORDER',
  MODIFY_ORDER = 'MODIFY_ORDER',
  DELETE_ORDER = 'DELETE_ORDER',
  CANCEL_ORDER = 'CANCEL_ORDER',
  VIEW_CHANNELS = 'VIEW_CHANNELS',
  MANAGE_CHANNELS = 'MANAGE_CHANNELS',
  VIEW_CUSTOMERS = 'VIEW_CUSTOMERS',
  MANAGE_CUSTOMERS = 'MANAGE_CUSTOMERS',
  VIEW_STATS = 'VIEW_STATS',
  EXPORT_STATS = 'EXPORT_STATS',
  VIEW_SETTINGS = 'VIEW_SETTINGS',
  MODIFY_SETTINGS = 'MODIFY_SETTINGS',
  MODIFY_STORE_SETTINGS = 'MODIFY_STORE_SETTINGS',
  MANAGE_EMPLOYEE_ACCOUNTS = 'MANAGE_EMPLOYEE_ACCOUNTS',
  MANAGE_PAYMENT_METHODS = 'MANAGE_PAYMENT_METHODS',
  VIEW_DATA = 'VIEW_DATA',
  EXPORT_DATA = 'EXPORT_DATA',
  VIEW_FINANCIAL_DATA = 'VIEW_FINANCIAL_DATA',
  DELETE_IMPORTANT_DATA = 'DELETE_IMPORTANT_DATA',
}

export interface PermissionDTO {
  id?: number
  module: PermissionModule
  action: PermissionAction
  roomTypeId?: number
  roomTypeName?: string
  allRoomTypes?: boolean
}

export interface RoleDTO {
  id: number
  name: string
  description?: string
  isSystem: boolean
  permissions?: PermissionDTO[]
  createdAt: string
  updatedAt: string
}

export interface CreateRoleRequest {
  name: string
  description?: string
}

export interface UpdateRoleRequest {
  name?: string
  description?: string
}

export const getAllRoles = (keyword?: string) => {
  return request<ApiResponse<RoleDTO[]>>({
    url: '/roles',
    method: 'GET',
    params: keyword ? { keyword } : undefined,
  })
}

export const getRoleById = (roleId: number) => {
  return request<ApiResponse<RoleDTO>>({
    url: `/roles/${roleId}`,
    method: 'GET',
  })
}

export const createRole = (data: CreateRoleRequest) => {
  return request<ApiResponse<RoleDTO>>({
    url: '/roles',
    method: 'POST',
    data,
  })
}

export const updateRole = (roleId: number, data: UpdateRoleRequest) => {
  return request<ApiResponse<RoleDTO>>({
    url: `/roles/${roleId}`,
    method: 'PUT',
    data,
  })
}

export const deleteRole = (roleId: number) => {
  return request<ApiResponse<void>>({
    url: `/roles/${roleId}`,
    method: 'DELETE',
  })
}

export const getRolePermissions = (roleId: number) => {
  return request<ApiResponse<PermissionDTO[]>>({
    url: `/roles/${roleId}/permissions`,
    method: 'GET',
  })
}

export const updateRolePermissions = (roleId: number, permissions: PermissionDTO[]) => {
  return request<ApiResponse<void>>({
    url: `/roles/${roleId}/permissions`,
    method: 'PUT',
    data: permissions,
  })
}
