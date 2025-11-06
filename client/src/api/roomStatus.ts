import { request } from '@/utils/request'

// 房态日历相关数据结构
export interface DailyRoomStatusDTO {
  date: string
  status: string
  reservation?: ReservationInfoDTO
}

export interface ReservationInfoDTO {
  id: number
  guestName: string
  channel: string
  checkIn: string
  checkOut: string
  orderNumber: string
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

// 房态统计数据结构
export interface RoomStatusStatisticsDTO {
  date: string
  todayArrivals: number // 今日预抵
  todayDepartures: number // 今日预离
  todayNewOrders: number // 今日新办
  availableRooms: number // 今日可售
  unassignedOrders: number // 未排房
  pendingOrders: number // 待处理
}

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 获取房态日历数据
export const getRoomStatusCalendar = async (
  startDate: string,
  endDate: string,
): Promise<ApiResponse<RoomStatusCalendarDTO>> => {
  return await request.get('/room-status/calendar', {
    params: { startDate, endDate },
  })
}

// 更新房间状态
export const updateRoomStatus = async (
  roomId: number,
  date: string,
  status: string,
  reason?: string,
): Promise<ApiResponse<string>> => {
  return await request.put(`/room-status/${roomId}`, {
    date,
    status,
    reason,
  })
}

// 获取房态统计数据
export const getRoomStatusStatistics = async (
  date?: string,
): Promise<ApiResponse<RoomStatusStatisticsDTO>> => {
  const params = date ? { date } : {}
  return await request.get('/room-status/statistics', { params })
}
