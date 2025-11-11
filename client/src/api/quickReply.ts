import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 快捷回复DTO
export interface QuickReplyDTO {
  id: number
  userId: number
  title: string
  message: string
  createdAt: string
  updatedAt: string
}

// 创建/更新快捷回复请求
export interface QuickReplyRequest {
  title: string
  message: string
  userId?: number
}

/**
 * 获取所有快捷回复
 */
export const getAllQuickReplies = async (): Promise<ApiResponse<QuickReplyDTO[]>> => {
  return await request.get('/quick-replies')
}

/**
 * 根据ID获取快捷回复详情
 */
export const getQuickReplyById = async (id: number): Promise<ApiResponse<QuickReplyDTO>> => {
  return await request.get(`/quick-replies/${id}`)
}

/**
 * 创建快捷回复
 */
export const createQuickReply = async (
  data: QuickReplyRequest
): Promise<ApiResponse<QuickReplyDTO>> => {
  return await request.post('/quick-replies', data)
}

/**
 * 更新快捷回复
 */
export const updateQuickReply = async (
  id: number,
  data: QuickReplyRequest
): Promise<ApiResponse<QuickReplyDTO>> => {
  return await request.put(`/quick-replies/${id}`, data)
}

/**
 * 删除快捷回复
 */
export const deleteQuickReply = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/quick-replies/${id}`)
}
