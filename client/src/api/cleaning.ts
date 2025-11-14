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
 * 获取当前门店的保洁员列表(使用门店上下文)
 */
export const getCleaners = async (): Promise<ApiResponse<CleanerDTO[]>> => {
  return await request.get('/cleaners')
}

/**
 * 根据用户ID和门店ID获取保洁员列表
 * @deprecated 使用 getCleaners() 替代,现在使用门店级数据隔离
 */
export const getCleanersByUserIdAndStoreId = async (
  userId: number,
  storeId: number
): Promise<ApiResponse<CleanerDTO[]>> => {
  return await request.get(`/cleaners/user/${userId}/store/${storeId}`)
}

/**
 * 根据用户ID获取保洁员列表
 * @deprecated 使用 getCleaners() 替代,现在使用门店级数据隔离
 */
export const getCleanersByUserId = async (
  userId: number
): Promise<ApiResponse<CleanerDTO[]>> => {
  return await request.get(`/cleaners/user/${userId}`)
}

/**
 * 根据门店ID获取保洁员列表
 * @deprecated 使用 getCleaners() 替代,现在使用门店级数据隔离
 */
export const getCleanersByStoreId = async (
  storeId: number
): Promise<ApiResponse<CleanerDTO[]>> => {
  return await request.get(`/cleaners/store/${storeId}`)
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
 * 获取易耗品列表(门店级)
 */
export const getAllCleaningSupplies = async (): Promise<ApiResponse<CleaningSupplyDTO[]>> => {
  return await request.get('/cleaning-supplies')
}

/**
 * 根据用户ID获取易耗品列表
 * @deprecated 使用 getAllCleaningSupplies() 替代,现在使用门店级数据隔离
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

// ==================== 保洁任务 API ====================

// 保洁任务DTO
export interface CleaningTaskDTO {
  id: number
  taskDate: string
  roomId: number
  roomNumber: string
  roomType: string
  roomTypeId: number
  taskType: string
  status: string
  cleanerId?: number
  cleanerName?: string
  estimatedTime?: string
  startTime?: string
  completeTime?: string
  approverId?: number
  approverName?: string
  notes?: string
  createdAt: string
  updatedAt: string
}

// 创建保洁任务请求DTO
export interface CleaningTaskCreateDTO {
  taskDate: string
  roomId: number
  taskType: string
  cleanerId?: number
  estimatedTime?: string
  notes?: string
}

// 更新保洁任务请求DTO
export interface CleaningTaskUpdateDTO {
  status?: string
  cleanerId?: number
  estimatedTime?: string
  startTime?: string
  completeTime?: string
  approverId?: number
  notes?: string
}

// 分页响应
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// 日历视图数据
export interface CalendarViewData {
  tasks: Record<string, CleaningTaskDTO[]>
  totalCount: number
  statusCount: Record<string, number>
}

/**
 * 创建保洁任务
 */
export const createCleaningTask = async (
  data: CleaningTaskCreateDTO
): Promise<ApiResponse<CleaningTaskDTO>> => {
  return await request.post('/cleaning-tasks', data)
}

/**
 * 批量创建保洁任务
 */
export const batchCreateCleaningTasks = async (
  data: CleaningTaskCreateDTO[]
): Promise<ApiResponse<CleaningTaskDTO[]>> => {
  return await request.post('/cleaning-tasks/batch', data)
}

/**
 * 更新保洁任务
 */
export const updateCleaningTask = async (
  id: number,
  data: CleaningTaskUpdateDTO
): Promise<ApiResponse<CleaningTaskDTO>> => {
  return await request.put(`/cleaning-tasks/${id}`, data)
}

/**
 * 删除保洁任务
 */
export const deleteCleaningTask = async (id: number): Promise<ApiResponse<string>> => {
  return await request.delete(`/cleaning-tasks/${id}`)
}

/**
 * 获取任务详情
 */
export const getCleaningTaskById = async (
  id: number
): Promise<ApiResponse<CleaningTaskDTO>> => {
  return await request.get(`/cleaning-tasks/${id}`)
}

/**
 * 分页查询任务列表
 */
export const getCleaningTasks = async (params: {
  startDate?: string
  endDate?: string
  status?: string
  taskType?: string
  roomId?: number
  cleanerId?: number
  roomTypeId?: number
  search?: string
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: string
}): Promise<ApiResponse<PageResponse<CleaningTaskDTO>>> => {
  return await request.get('/cleaning-tasks', { params })
}

/**
 * 获取日历视图数据
 */
export const getCalendarViewData = async (params: {
  startDate: string
  endDate: string
  status?: string
}): Promise<ApiResponse<CalendarViewData>> => {
  return await request.get('/cleaning-tasks/calendar', { params })
}

/**
 * 分配任务
 */
export const assignCleaningTask = async (
  id: number,
  cleanerId: number
): Promise<ApiResponse<CleaningTaskDTO>> => {
  return await request.post(`/cleaning-tasks/${id}/assign`, null, {
    params: { cleanerId }
  })
}

/**
 * 保洁员接受任务
 */
export const acceptCleaningTask = async (
  id: number
): Promise<ApiResponse<CleaningTaskDTO>> => {
  return await request.post(`/cleaning-tasks/${id}/accept`)
}

/**
 * 保洁员拒绝任务
 */
export const rejectCleaningTask = async (
  id: number
): Promise<ApiResponse<CleaningTaskDTO>> => {
  return await request.post(`/cleaning-tasks/${id}/reject`)
}

/**
 * 开始任务
 */
export const startCleaningTask = async (
  id: number
): Promise<ApiResponse<CleaningTaskDTO>> => {
  return await request.post(`/cleaning-tasks/${id}/start`)
}

/**
 * 完成任务
 */
export const completeCleaningTask = async (
  id: number,
  approverId: number
): Promise<ApiResponse<CleaningTaskDTO>> => {
  return await request.post(`/cleaning-tasks/${id}/complete`, null, {
    params: { approverId }
  })
}

/**
 * 统计任务状态数量
 */
export const getTaskStatusCount = async (params: {
  startDate: string
  endDate: string
}): Promise<ApiResponse<Record<string, number>>> => {
  return await request.get('/cleaning-tasks/statistics/status', { params })
}

// ==================== 保洁员邀请 API ====================

// 保洁员邀请请求DTO
export interface CleanerInvitationRequest {
  email: string
  name: string
  userId?: number
  storeId?: number
}

// 保洁员注册请求DTO
export interface CleanerRegistrationRequest {
  token: string
  name: string
  email: string
  password: string
}

// 邀请信息DTO
export interface CleanerInvitationInfo {
  id: number
  email: string
  name: string
  token: string
  userId: number
  storeId: number
  status: string
  expiresAt: string
  createdAt: string
  updatedAt: string
}

/**
 * 发送保洁员邀请邮件
 */
export const sendCleanerInvitation = async (
  data: CleanerInvitationRequest
): Promise<ApiResponse<string>> => {
  return await request.post('/cleaner-invitations/send', data)
}

/**
 * 验证邀请token
 */
export const validateInvitationToken = async (
  token: string
): Promise<ApiResponse<CleanerInvitationInfo>> => {
  return await request.get(`/cleaner-invitations/validate/${token}`)
}

/**
 * 保洁员注册
 */
export const registerCleaner = async (
  data: CleanerRegistrationRequest
): Promise<ApiResponse<CleanerDTO>> => {
  return await request.post('/cleaner-invitations/register', data)
}
