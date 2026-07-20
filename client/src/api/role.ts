import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 权限模块枚举
export enum PermissionModule {
  ACCOMMODATION = 'ACCOMMODATION',
  ORDER = 'ORDER',
  CHANNEL = 'CHANNEL',
  CUSTOMER = 'CUSTOMER',
  STATISTICS = 'STATISTICS',
  REVIEW = 'REVIEW',
  SETTINGS = 'SETTINGS',
  DATA_CENTER = 'DATA_CENTER',
  SENSITIVE = 'SENSITIVE',
}

// 权限操作枚举
export enum PermissionAction {
  // 住宿管理相关
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
  CREATE_INTERNAL_TASK = 'CREATE_INTERNAL_TASK',

  // 订单管理相关
  VIEW_ORDERS = 'VIEW_ORDERS',
  CREATE_ORDER = 'CREATE_ORDER',
  MODIFY_ORDER = 'MODIFY_ORDER',
  DELETE_ORDER = 'DELETE_ORDER',
  CANCEL_ORDER = 'CANCEL_ORDER',

  // 渠道相关
  VIEW_CHANNELS = 'VIEW_CHANNELS',
  MANAGE_CHANNELS = 'MANAGE_CHANNELS',

  // 客户管理相关
  VIEW_CUSTOMERS = 'VIEW_CUSTOMERS',
  MANAGE_CUSTOMERS = 'MANAGE_CUSTOMERS',

  // 统计分析相关
  VIEW_STATS = 'VIEW_STATS',
  EXPORT_STATS = 'EXPORT_STATS',

  // 渠道评价相关
  VIEW = 'VIEW',
  REPLY = 'REPLY',
  REVIEW_GUEST = 'REVIEW_GUEST',
  SYNC = 'SYNC',

  // 设置相关
  VIEW_SETTINGS = 'VIEW_SETTINGS',
  MODIFY_SETTINGS = 'MODIFY_SETTINGS',
  MODIFY_STORE_SETTINGS = 'MODIFY_STORE_SETTINGS',
  MANAGE_EMPLOYEE_ACCOUNTS = 'MANAGE_EMPLOYEE_ACCOUNTS',
  MANAGE_PAYMENT_METHODS = 'MANAGE_PAYMENT_METHODS',

  // 数据中心相关
  VIEW_DATA = 'VIEW_DATA',
  EXPORT_DATA = 'EXPORT_DATA',

  // 敏感权限
  VIEW_FINANCIAL_DATA = 'VIEW_FINANCIAL_DATA',
  DELETE_IMPORTANT_DATA = 'DELETE_IMPORTANT_DATA',
}

// 权限DTO
export interface PermissionDTO {
  id?: number
  module: PermissionModule
  action: PermissionAction
  roomTypeId?: number
  roomTypeName?: string
  allRoomTypes?: boolean
}

// 角色DTO
export interface RoleDTO {
  id: number
  name: string
  description?: string
  isSystem: boolean
  permissions?: PermissionDTO[]
  createdAt: string
  updatedAt: string
}

// 创建角色请求
export interface CreateRoleRequest {
  name: string
  description?: string
}

// 更新角色请求
export interface UpdateRoleRequest {
  name?: string
  description?: string
}

/**
 * 获取所有角色(支持搜索)
 */
export const getAllRoles = async (keyword?: string): Promise<ApiResponse<RoleDTO[]>> => {
  return await request.get('/roles', { params: keyword ? { keyword } : undefined })
}

/**
 * 根据ID获取角色详情(含权限)
 */
export const getRoleById = async (id: number): Promise<ApiResponse<RoleDTO>> => {
  return await request.get(`/roles/${id}`)
}

/**
 * 创建角色
 */
export const createRole = async (data: CreateRoleRequest): Promise<ApiResponse<RoleDTO>> => {
  return await request.post('/roles', data)
}

/**
 * 更新角色
 */
export const updateRole = async (
  id: number,
  data: UpdateRoleRequest
): Promise<ApiResponse<RoleDTO>> => {
  return await request.put(`/roles/${id}`, data)
}

/**
 * 删除角色
 */
export const deleteRole = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/roles/${id}`)
}

/**
 * 获取角色的权限
 */
export const getRolePermissions = async (id: number): Promise<ApiResponse<PermissionDTO[]>> => {
  return await request.get(`/roles/${id}/permissions`)
}

/**
 * 更新角色的权限
 */
export const updateRolePermissions = async (
  id: number,
  permissions: PermissionDTO[]
): Promise<ApiResponse<void>> => {
  return await request.put(`/roles/${id}/permissions`, permissions)
}
