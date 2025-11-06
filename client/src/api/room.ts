import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 房间信息
export interface RoomDTO {
  id: number
  roomNumber: string
  roomType: {
    id: number
    name: string
    code: string
    totalRooms: number
    description?: string
  }
  floor: number
  status: string
}

// 房间查询参数
export interface RoomQueryParams {
  roomTypeId?: number
  date?: string
}

// 获取房间列表
export const getRooms = async (params?: RoomQueryParams): Promise<ApiResponse<RoomDTO[]>> => {
  return await request.get('/rooms', { params })
}

// 获取房间详情
export const getRoomById = async (id: number): Promise<ApiResponse<RoomDTO>> => {
  return await request.get(`/rooms/${id}`)
}

// 获取房间的房型信息
export const getRoomTypeByRoomId = async (id: number): Promise<ApiResponse<any>> => {
  return await request.get(`/rooms/${id}/room-type`)
}