import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { RoomGroupDTO, RoomGroupMemberBatchDTO, RoomGroupMemberDTO } from '@/types/settings'

export const getAllRoomGroups = () => {
  return request<ApiResponse<RoomGroupDTO[]>>({
    url: '/room-groups',
    method: 'GET',
  })
}

export const getRoomGroupById = (id: number) => {
  return request<ApiResponse<RoomGroupDTO>>({
    url: `/room-groups/${id}`,
    method: 'GET',
  })
}

export const createRoomGroup = (data: RoomGroupDTO) => {
  return request<ApiResponse<RoomGroupDTO>>({
    url: '/room-groups',
    method: 'POST',
    data,
  })
}

export const updateRoomGroup = (id: number, data: RoomGroupDTO) => {
  return request<ApiResponse<RoomGroupDTO>>({
    url: `/room-groups/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteRoomGroup = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/room-groups/${id}`,
    method: 'DELETE',
  })
}

export const getGroupMembers = (groupId: number) => {
  return request<ApiResponse<RoomGroupMemberDTO[]>>({
    url: `/room-groups/${groupId}/members`,
    method: 'GET',
  })
}

export const addRoomsToGroup = (groupId: number, data: RoomGroupMemberBatchDTO) => {
  return request<ApiResponse<void>>({
    url: `/room-groups/${groupId}/members/batch`,
    method: 'POST',
    data,
  })
}

export const removeRoomsFromGroup = (groupId: number, data: RoomGroupMemberBatchDTO) => {
  return request<ApiResponse<void>>({
    url: `/room-groups/${groupId}/members/batch`,
    method: 'DELETE',
    data,
  })
}
