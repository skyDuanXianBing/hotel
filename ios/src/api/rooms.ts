import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { RoomDTO } from '@/types/settings'

export interface RoomQueryParams {
  roomTypeId?: number
  date?: string
}

export const getRooms = (params?: RoomQueryParams) => {
  return request<ApiResponse<RoomDTO[]>>({
    url: '/rooms',
    method: 'GET',
    params: params as Record<string, string | number | boolean | null | undefined> | undefined,
  })
}

export const getRoomById = (id: number) => {
  return request<ApiResponse<RoomDTO>>({
    url: `/rooms/${id}`,
    method: 'GET',
  })
}

export const roomsApi = Object.freeze({
  getRooms,
  getRoomById,
})

export default roomsApi
