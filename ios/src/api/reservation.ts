import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface CreateReservationRequest {
  guestName: string
  guestPhone?: string
  guestIdCard?: string
  roomId: number
  groupOrderNo?: string
  channelId: number
  checkInDate: string
  checkOutDate: string
  adults: number
  children?: number
  totalAmount: number
  channelOrderNumber?: string
  notes?: string
  directCheckIn?: boolean
}

export interface ReservationDTO {
  id: number
  orderNumber: string
  groupOrderNo?: string
  channelOrderNumber?: string
  guestName: string
  phone?: string
  guestIdCard?: string
  roomId?: number
  roomNumber?: string
  roomTypeName?: string
  channelId: number
  channelName: string
  checkInDate: string
  checkOutDate: string
  status: string
  adults?: number
  children?: number
  paymentMethod?: string
  commission?: number
  paidAmount?: number
  pricePlan?: string
  createdBy?: string
  notes?: string
  totalAmount?: number
  currentRoomPrice?: number
  createdAt: string
  updatedAt: string
  reservationNotifId?: string
  suReservationId?: string
  otaRoomId?: string
  otaRoomTypeId?: number
  checkinType?: string
}

export interface PagedReservationResponse {
  content: ReservationDTO[]
  currentPage: number
  pageSize: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface ReservationStatistics {
  todayCheckinCount: number
  todayCheckoutCount: number
  todayNewCount: number
  unassignedCount: number
  pendingCount: number
  totalReservations: number
}

export interface ReservationFilters {
  page?: number
  size?: number
  searchKeyword?: string
  channel?: string
  roomType?: string
  checkinType?: string
  status?: string
  paymentStatus?: string
  startDate?: string
  endDate?: string
  orderType?: string
}

export interface ReservationChannelInfoDTO {
  channelName?: string
  channelOrderNumber?: string
  bookingDate?: string
  paymentMethod?: string
  status?: string
  totalAmount?: number
  commission?: number
  otherFees?: number
  roomType?: string
  guestName?: string
  adults?: number
  children?: number
  checkInDate?: string
  checkOutDate?: string
  nights?: number
  pricePlan?: string
  specialRequests?: string
}

export interface OperationLogDetailDTO {
  label: string
  value: string
}

export interface OperationLogDTO {
  id: number
  action: string
  operator: string
  timestamp: string
  type: string
  content?: string
  details?: OperationLogDetailDTO[]
}

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

export interface AssignableRoomTypeDTO {
  id: number
  name: string
  code?: string
  availableRooms: number
}

export interface AssignableRoomDTO {
  id: number
  roomNumber: string
  roomTypeId: number
  roomTypeName: string
}

export interface AssignableRoomsResponse {
  reservationId: number
  checkInDate: string
  checkOutDate: string
  roomTypes: AssignableRoomTypeDTO[]
  rooms: AssignableRoomDTO[]
}

export const createReservation = async (data: CreateReservationRequest) => {
  return request.post<ApiResponse<ReservationDTO>>('/reservations', data)
}

export const updateReservation = async (reservationId: number, data: CreateReservationRequest) => {
  return request.post<ApiResponse<ReservationDTO>>(`/reservations/${reservationId}/update`, data)
}

export const checkInReservation = async (reservationId: number) => {
  return request.post<ApiResponse<ReservationDTO>>(`/reservations/${reservationId}/check-in`)
}

export const checkOutReservation = async (reservationId: number) => {
  return request.post<ApiResponse<ReservationDTO>>(`/reservations/${reservationId}/check-out`)
}

export const cancelReservation = async (reservationId: number) => {
  return request.post<ApiResponse<ReservationDTO>>(`/reservations/${reservationId}/cancel`)
}

export const getReservationById = async (reservationId: number) => {
  return request.get<ApiResponse<ReservationDTO>>(`/reservations/${reservationId}`)
}

export const getReservationByOrderNumber = async (orderNumber: string) => {
  return request.get<ApiResponse<ReservationDTO>>(`/reservations/order/${orderNumber}`)
}

export const getReservationsWithFilters = async (filters: ReservationFilters = {}) => {
  const params: Record<string, string | number | boolean | null | undefined> = {
    ...filters,
  }

  return request.get<ApiResponse<PagedReservationResponse>>('/reservations', {
    params,
  })
}

export const getReservationStatistics = async () => {
  return request.get<ApiResponse<ReservationStatistics>>('/reservations/statistics')
}

export const getReservationsByType = async (type: string) => {
  return request.get<ApiResponse<ReservationDTO[]>>('/reservations/by-type', {
    params: { type },
  })
}

export const searchReservations = async (keyword: string) => {
  return request.get<ApiResponse<ReservationDTO[]>>('/reservations/search', {
    params: { keyword },
  })
}

export const getReservationChannelInfo = async (reservationId: number) => {
  return request.get<ApiResponse<ReservationChannelInfoDTO>>(
    `/reservations/${reservationId}/channel-info`,
  )
}

export const getReservationLogs = async (reservationId: number) => {
  return request.get<ApiResponse<OperationLogDTO[]>>(`/operation-logs/reservation/${reservationId}`)
}

export const getAssignableRooms = async (reservationId: number, roomTypeId?: number) => {
  return request.get<ApiResponse<AssignableRoomsResponse>>(
    `/reservations/${reservationId}/assignable-rooms`,
    {
      params: { roomTypeId },
    },
  )
}

export const assignReservationRoom = async (reservationId: number, roomId: number) => {
  return request.post<ApiResponse<ReservationDTO>>(`/reservations/${reservationId}/assign-room`, {
    roomId,
  })
}

export const getAllChannels = async () => {
  return request.get<ApiResponse<ChannelDTO[]>>('/channels')
}
