import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface RoomGroupDTO {
  id?: number
  name: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface RoomGroupMemberDTO {
  id?: number
  groupId: number
  roomId: number
  createdAt?: string
}

export interface RoomGroupMemberBatchDTO {
  roomIds: number[]
}

/**
 * 获取当前门店的所有房间分组
 */
export const getAllRoomGroups = async (): Promise<ApiResponse<RoomGroupDTO[]>> => {
  return await request.get('/room-groups')
}

/**
 * 根据ID获取房间分组
 */
export const getRoomGroupById = async (id: number): Promise<ApiResponse<RoomGroupDTO>> => {
  return await request.get(`/room-groups/${id}`)
}

/**
 * 创建房间分组
 */
export const createRoomGroup = async (
  data: RoomGroupDTO
): Promise<ApiResponse<RoomGroupDTO>> => {
  return await request.post('/room-groups', data)
}

/**
 * 更新房间分组
 */
export const updateRoomGroup = async (
  id: number,
  data: RoomGroupDTO
): Promise<ApiResponse<RoomGroupDTO>> => {
  return await request.put(`/room-groups/${id}`, data)
}

/**
 * 删除房间分组
 */
export const deleteRoomGroup = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/room-groups/${id}`)
}

/**
 * 获取分组的所有房间
 */
export const getGroupMembers = async (
  groupId: number
): Promise<ApiResponse<RoomGroupMemberDTO[]>> => {
  return await request.get(`/room-groups/${groupId}/members`)
}

/**
 * 添加房间到分组
 */
export const addRoomToGroup = async (
  groupId: number,
  roomId: number
): Promise<ApiResponse<RoomGroupMemberDTO>> => {
  return await request.post(`/room-groups/${groupId}/members/${roomId}`)
}

/**
 * 批量添加房间到分组
 */
export const addRoomsToGroup = async (
  groupId: number,
  data: RoomGroupMemberBatchDTO
): Promise<ApiResponse<void>> => {
  return await request.post(`/room-groups/${groupId}/members/batch`, data)
}

/**
 * 从分组中移除房间
 */
export const removeRoomFromGroup = async (
  groupId: number,
  roomId: number
): Promise<ApiResponse<void>> => {
  return await request.delete(`/room-groups/${groupId}/members/${roomId}`)
}

/**
 * 批量从分组中移除房间
 */
export const removeRoomsFromGroup = async (
  groupId: number,
  data: RoomGroupMemberBatchDTO
): Promise<ApiResponse<void>> => {
  return await request.delete(`/room-groups/${groupId}/members/batch`, { data })
}
