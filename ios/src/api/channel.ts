import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { CreateChannelRequest } from '@/types/settings'

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

export const getAllChannels = () => {
  return request.get<ApiResponse<ChannelDTO[]>>('/channels')
}

export const createChannel = (data: CreateChannelRequest) => {
  return request<ApiResponse<ChannelDTO>>({
    url: '/channels',
    method: 'POST',
    data,
  })
}

export const updateChannel = (id: number, data: CreateChannelRequest) => {
  return request<ApiResponse<ChannelDTO>>({
    url: `/channels/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteChannel = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/channels/${id}`,
    method: 'DELETE',
  })
}

export const toggleChannelStatus = (id: number, enabled: boolean) => {
  return request<ApiResponse<ChannelDTO>>({
    url: `/channels/${id}/status`,
    method: 'PATCH',
    data: { enabled },
  })
}

const channelApi = {
  getAllChannels,
  createChannel,
  updateChannel,
  deleteChannel,
  toggleChannelStatus,
}

export default channelApi
