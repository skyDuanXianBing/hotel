import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 自动化消息DTO
export interface AutoMessageDTO {
  id: number
  userId: number
  title: string
  message: string
  automationRule: string
  channel: string
  room: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

// 创建/更新自动化消息请求
export interface AutoMessageRequest {
  title: string
  message: string
  automationRule: string
  channel: string
  room: string
  enabled: boolean
}

/**
 * 获取所有自动化消息
 */
export const getAllAutoMessages = async (): Promise<ApiResponse<AutoMessageDTO[]>> => {
  return await request.get('/auto-messages')
}

/**
 * 根据用户ID获取自动化消息列表
 * @deprecated 使用 getAllAutoMessages() 替代,现在使用门店级数据隔离
 */
export const getAutoMessagesByUserId = async (
  userId: number
): Promise<ApiResponse<AutoMessageDTO[]>> => {
  return await request.get(`/auto-messages/user/${userId}`)
}

/**
 * 根据ID获取自动化消息详情
 */
export const getAutoMessageById = async (
  id: number
): Promise<ApiResponse<AutoMessageDTO>> => {
  return await request.get(`/auto-messages/${id}`)
}

/**
 * 创建自动化消息
 */
export const createAutoMessage = async (
  data: AutoMessageRequest
): Promise<ApiResponse<AutoMessageDTO>> => {
  return await request.post('/auto-messages', data)
}

/**
 * 更新自动化消息
 */
export const updateAutoMessage = async (
  id: number,
  data: AutoMessageRequest
): Promise<ApiResponse<AutoMessageDTO>> => {
  return await request.put(`/auto-messages/${id}`, data)
}

/**
 * 删除自动化消息
 */
export const deleteAutoMessage = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/auto-messages/${id}`)
}

/**
 * 切换自动化消息启用状态
 */
export const toggleAutoMessage = async (
  id: number
): Promise<ApiResponse<AutoMessageDTO>> => {
  return await request.put(`/auto-messages/${id}/toggle`)
}
