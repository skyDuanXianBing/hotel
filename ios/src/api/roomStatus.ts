import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

const ROOM_STATUS_CALENDAR_TIMEOUT_MS = 30000

export interface ReservationInfoDTO {
  id: number
  guestName: string
  channel: string
  checkIn: string
  checkOut: string
  orderNumber: string
  totalAmount?: number
  groupOrderNo?: string
  notes?: string
  specialRequests?: string
}

export interface DailyRoomStatusDTO {
  date: string
  status: string
  reservation?: ReservationInfoDTO | null
  closed?: boolean
  closeType?: string
  closeRemark?: string
}

export interface CalendarRoomDataDTO {
  roomId: number
  roomNumber: string
  roomType: string
  dailyStatus: DailyRoomStatusDTO[]
}

export interface DateRangeDTO {
  startDate: string
  endDate: string
}

export interface RoomStatusCalendarDTO {
  dateRange: DateRangeDTO
  rooms: CalendarRoomDataDTO[]
}

export interface RoomStatusStatisticsDTO {
  date: string
  todayArrivals: number
  todayDepartures: number
  todayNewOrders: number
  availableRooms: number
  unassignedOrders: number
  pendingOrders: number
}

export interface RoomBlockoutSummaryDTO {
  affectedDays: number
}

export interface CloseRoomBlockoutRequest {
  roomIds: number[]
  startDate: string
  endDate: string
  type: 'stop' | 'maintenance' | 'retain'
  remark?: string
}

export interface OpenRoomBlockoutRequest {
  roomIds: number[]
  startDate: string
  endDate: string
}

export const getRoomStatusCalendar = async (startDate: string, endDate: string) => {
  return request.get<ApiResponse<RoomStatusCalendarDTO>>('/room-status/calendar', {
    params: { startDate, endDate },
    timeoutMs: ROOM_STATUS_CALENDAR_TIMEOUT_MS,
  })
}

export const getRoomStatusStatistics = async (date?: string) => {
  const params = date ? { date } : undefined
  return request.get<ApiResponse<RoomStatusStatisticsDTO>>('/room-status/statistics', {
    params,
  })
}

export const closeRoomBlockouts = async (data: CloseRoomBlockoutRequest) => {
  return request.post<ApiResponse<RoomBlockoutSummaryDTO>>('/room-status/blockouts/close', data)
}

export const openRoomBlockouts = async (data: OpenRoomBlockoutRequest) => {
  return request.post<ApiResponse<RoomBlockoutSummaryDTO>>('/room-status/blockouts/open', data)
}
