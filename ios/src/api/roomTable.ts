import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

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
