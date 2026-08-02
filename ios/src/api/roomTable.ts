import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

const ROOM_TABLE_MONTHLY_TIMEOUT_MS = 60000

export type MonthlyRoomDisplayStatus = 'FULL' | 'AVAILABLE' | 'AVAILABLE_MANY'

export interface MonthlyRoomReservationInfo {
  id: number
  guestName: string
  channel: string
  checkIn: string
  checkOut: string
  orderNumber: string
  status?: string
  totalAmount?: number
  groupOrderNo?: string
  notes?: string
  specialRequests?: string
}

export interface MonthlyDailyStatusDTO {
  date: string
  status: string
  displayStatus: MonthlyRoomDisplayStatus
  sellable: boolean
  blockedReason?: string
  reservation?: MonthlyRoomReservationInfo
  closed?: boolean
  closeType?: string
  closeRemark?: string
  roomTypeAvailableRooms?: number
  closeRoom?: boolean
  cta?: boolean
  ctd?: boolean
}

export interface MonthlyRoomDataDTO {
  roomId: number
  roomNumber: string
  roomTypeId: number
  roomType: string
  dailyStatus: MonthlyDailyStatusDTO[]
}

export interface MonthlyRoomTypeSummaryDTO {
  roomTypeId: number
  roomTypeName: string
  date: string
  totalRooms: number
  physicalSellableRooms: number
  assignedOccupiedRooms: number
  blockoutRooms: number
  staticUnavailableRooms: number
  unassignedOccupiedRooms: number
  inventoryLimit?: number
  effectiveAvailableRooms: number
  closeRoom: boolean
  cta: boolean
  ctd: boolean
}

export interface RoomTableMonthlyResponse {
  startDate: string
  endDate: string
  rooms: MonthlyRoomDataDTO[]
  roomTypeSummaries: MonthlyRoomTypeSummaryDTO[]
}

export interface RoomStatisticsDTO {
  roomTypeName: string
  totalRooms: number
  availableForSale: number
  availableRooms: number
  occupiedRooms: number
  occupiedWithoutDeparture: number
  scheduledDeparture: number
  scheduledArrival: number
  reservedRooms: number
  maintenanceRooms: number
  outOfOrderRooms: number
  linkedClosedRooms: number
  cleanRooms: number
  dirtyRooms: number
  expectedOccupancyRate: number
  dailyCancelledRooms: number
}

export interface RoomTableDataDTO {
  date: string
  statistics: RoomStatisticsDTO[]
  total: RoomStatisticsDTO
}

export interface FutureDateRoomDataDTO {
  date: string
  dayOfWeek: string
  available: number
  occupied: number
  unavailable: number
  availableRate: number
  occupiedRate: number
  unavailableRate: number
}

export interface FutureRoomTypeDataDTO {
  roomTypeName: string
  totalRooms: number
  dates: FutureDateRoomDataDTO[]
}

export interface FutureRoomStatisticsDTO {
  date: string
  effectiveRooms: number
  expectedOccupancyRate: number
  expectedRoomRevenue: number
  expectedTotalRoomFee: number
  averageRoomRevenue: number
}

export interface FutureRoomTableDataDTO {
  startDate: string
  endDate: string
  roomTypes: FutureRoomTypeDataDTO[]
  total: FutureRoomTypeDataDTO
  statistics: FutureRoomStatisticsDTO[]
}

export const getRoomTableStatistics = (date: string) => {
  return request.get<ApiResponse<RoomTableDataDTO>>('/room-table/statistics', {
    params: { date },
  })
}

export const getFutureRoomTableData = (startDate: string, days = 7) => {
  return request.get<ApiResponse<FutureRoomTableDataDTO>>('/future-room-table', {
    params: { startDate, days },
  })
}

export const getMonthlyRoomTableData = (
  startDate: string,
  endDate: string,
  roomTypeId?: number,
) => {
  const params: {
    startDate: string
    endDate: string
    roomTypeId?: number
  } = {
    startDate,
    endDate,
  }

  if (roomTypeId) {
    params.roomTypeId = roomTypeId
  }

  return request.get<ApiResponse<RoomTableMonthlyResponse>>('/room-table/monthly', {
    params,
    timeoutMs: ROOM_TABLE_MONTHLY_TIMEOUT_MS,
  })
}
