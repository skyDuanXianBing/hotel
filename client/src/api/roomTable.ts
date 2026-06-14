import { request } from '@/utils/request'
import type { ApiResponse } from '@/types/room'

const ROOM_TABLE_MONTHLY_TIMEOUT_MS = 30000

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

export const getMonthlyRoomTableData = async (
  startDate: string,
  endDate: string,
  roomTypeId?: number,
): Promise<ApiResponse<RoomTableMonthlyResponse>> => {
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

  return await request.get('/room-table/monthly', {
    params,
    timeout: ROOM_TABLE_MONTHLY_TIMEOUT_MS,
  })
}
