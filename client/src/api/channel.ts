import { request } from '@/utils/request'

// 渠道接口定义
export interface ChannelDTO {
  id: number
  name: string
  code: string
  type: string
  color: string
  enabled: boolean
  description?: string
  createdAt: string
  updatedAt: string
}

export interface CreateChannelRequest {
  name: string
  code: string
  type: string
  color: string
  enabled?: boolean
  description?: string
}

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 获取所有渠道
export const getAllChannels = async (): Promise<ApiResponse<ChannelDTO[]>> => {
  return await request.get('/channels')
}

// 创建渠道
export const createChannel = async (
  data: CreateChannelRequest,
): Promise<ApiResponse<ChannelDTO>> => {
  return await request.post('/channels', data)
}

// 更新渠道
export const updateChannel = async (
  id: number,
  data: CreateChannelRequest,
): Promise<ApiResponse<ChannelDTO>> => {
  return await request.put(`/channels/${id}`, data)
}

// 删除渠道
export const deleteChannel = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/channels/${id}`)
}

// 启用/禁用渠道
export const toggleChannelStatus = async (
  id: number,
  enabled: boolean,
): Promise<ApiResponse<ChannelDTO>> => {
  return await request.patch(`/channels/${id}/status`, { enabled })
}
