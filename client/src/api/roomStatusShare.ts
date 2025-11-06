import { request } from '@/utils/request'
import type { ApiResponse } from '@/types/room'

// 房态分享相关接口

/**
 * 获取分享列表
 */
export const getRoomStatusShares = async (page: number = 1, pageSize: number = 25): Promise<ApiResponse<any>> => {
  return await request.get(`/room-status-share?page=${page}&pageSize=${pageSize}`)
}

/**
 * 创建分享链接
 */
export const createRoomStatusShare = async (data: any): Promise<ApiResponse<any>> => {
  return await request.post('/room-status-share', data)
}

/**
 * 更新分享链接
 */
export const updateRoomStatusShare = async (id: number, data: any): Promise<ApiResponse<any>> => {
  return await request.put(`/room-status-share/${id}`, data)
}

/**
 * 删除分享链接
 */
export const deleteRoomStatusShare = async (id: number): Promise<ApiResponse<any>> => {
  return await request.delete(`/room-status-share/${id}`)
}

/**
 * 获取公开分享信息（用于分享页面）
 */
export const getPublicRoomStatusShare = async (token: string) => {
  return await request.get<any>(`/room-status-share/public/${token}`)
}

/**
 * 获取分享页面的房态数据
 */
export const getSharedRoomStatusData = async (token: string, startDate?: string, endDate?: string) => {
  const params = new URLSearchParams()
  if (startDate) params.append('startDate', startDate)
  if (endDate) params.append('endDate', endDate)
  const queryString = params.toString()
  return await request.get<any>(`/room-status-share/public/${token}/room-status${queryString ? '?' + queryString : ''}`)
}

/**
 * 获取分享页面的统计数据
 */
export const getSharedStatistics = async (token: string, date?: string) => {
  const params = date ? `?date=${date}` : ''
  return await request.get<any>(`/room-status-share/public/${token}/statistics${params}`)
}