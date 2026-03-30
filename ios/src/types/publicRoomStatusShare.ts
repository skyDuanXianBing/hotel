export interface PublicRoomStatusShare {
  id: number
  shareTitle: string
  shareToken: string
  shareLink: string
  viewRoomStatus: boolean
  queryMethod: boolean
  viewType: string
  queryMode: string
  filterItems?: string | null
  orderItems?: string | null
  associatedRoomIds?: string | null
  userId?: number
  storeId?: number
  isActive?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface RoomStatusReservationInfo {
  id: number
  guestName: string
  channel?: string
  checkIn: string
  checkOut: string
  orderNumber?: string
  groupOrderNo?: string
  notes?: string
  specialRequests?: string
}

export interface RoomStatusDailyStatus {
  date: string
  status: string
  reservation?: RoomStatusReservationInfo | null
  closed?: boolean
  closeType?: string | null
  closeRemark?: string | null
}

export interface RoomStatusCalendarRoom {
  roomId: number
  roomNumber: string
  roomType: string
  dailyStatus: RoomStatusDailyStatus[]
}

export interface RoomStatusCalendarDateRange {
  startDate: string
  endDate: string
}

export interface PublicRoomStatusCalendar {
  dateRange?: RoomStatusCalendarDateRange
  rooms: RoomStatusCalendarRoom[]
}

export interface PublicRoomStatusStatistics {
  date: string
  todayArrivals: number
  todayDepartures: number
  todayNewOrders: number
  availableRooms: number
  unassignedOrders: number
  pendingOrders: number
}
