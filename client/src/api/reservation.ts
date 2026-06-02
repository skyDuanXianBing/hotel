import { request } from '@/utils/request'

// 预订相关API接口定义
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
  pricePlan?: string
  channelOrderNumber?: string
  notes?: string
  directCheckIn?: boolean // 是否直接入住
}

export interface ReservationDTO {
  id: number
  orderNumber: string
  groupOrderNo?: string
  channelOrderNumber?: string
  guestName: string
  phone?: string
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
  settled?: boolean
  pricePlan?: string
  createdBy?: string
  notes?: string
  totalAmount?: number
  currentRoomPrice?: number // 当前房型价格
  createdAt: string
  updatedAt: string
  reservationTimestampStorageZone?: string
  reservationNotifId?: string
  suReservationId?: string
  otaRoomId?: string
  otaRoomTypeId?: number
}

export interface BatchCreateReservationRequest {
  groupOrderNo?: string
  reservations: CreateReservationRequest[]
}

export interface BatchCreateReservationResponse {
  groupOrderNo: string
  reservationCount: number
  reservations: ReservationDTO[]
}

// 分页响应格式
export interface PagedReservationResponse {
  content: ReservationDTO[]
  currentPage: number
  pageSize: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// 预订统计信息
export interface ReservationStatistics {
  todayCheckinCount: number
  todayCheckoutCount: number
  todayNewCount: number
  unassignedCount: number
  pendingCount: number
  totalReservations: number
}

// 筛选条件接口
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
  operationDate?: string
}

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface UpdateReservationSettlementStatusRequest {
  settled: boolean
}

// 创建预订
export const createReservation = async (
  data: CreateReservationRequest,
): Promise<ApiResponse<ReservationDTO>> => {
  return await request.post('/reservations', data)
}

export const createBatchReservations = async (
  data: BatchCreateReservationRequest,
): Promise<ApiResponse<BatchCreateReservationResponse>> => {
  return await request.post('/reservations/batch', data)
}

// 办理入住
export const checkInReservation = async (
  reservationId: number,
): Promise<ApiResponse<ReservationDTO>> => {
  return await request.post(`/reservations/${reservationId}/check-in`)
}

// 办理退房
export const checkOutReservation = async (
  reservationId: number,
): Promise<ApiResponse<ReservationDTO>> => {
  return await request.post(`/reservations/${reservationId}/check-out`)
}

// 取消预订
export const cancelReservation = async (
  reservationId: number,
): Promise<ApiResponse<ReservationDTO>> => {
  return await request.post(`/reservations/${reservationId}/cancel`)
}

// 根据ID获取预订详情
export const getReservationById = async (
  reservationId: number,
): Promise<ApiResponse<ReservationDTO>> => {
  return await request.get(`/reservations/${reservationId}`)
}

// 根据订单号获取预订
export const getReservationByOrderNumber = async (
  orderNumber: string,
): Promise<ApiResponse<ReservationDTO>> => {
  return await request.get(`/reservations/order/${orderNumber}`)
}

// 根据日期范围获取预订
export const getReservationsByDateRange = async (
  startDate: string,
  endDate: string,
): Promise<ApiResponse<ReservationDTO[]>> => {
  return await request.get('/reservations/date-range', {
    params: { startDate, endDate },
  })
}

// 根据房间获取预订
export const getReservationsByRoom = async (
  roomId: number,
): Promise<ApiResponse<ReservationDTO[]>> => {
  return await request.get(`/reservations/room/${roomId}`)
}

// 搜索预订
export const searchReservations = async (
  keyword: string,
): Promise<ApiResponse<ReservationDTO[]>> => {
  return await request.get('/reservations/search', {
    params: { keyword },
  })
}

// 获取今日入住
export const getTodayCheckIns = async (): Promise<ApiResponse<ReservationDTO[]>> => {
  return await request.get('/reservations/today/check-ins')
}

// 获取今日退房
export const getTodayCheckOuts = async (): Promise<ApiResponse<ReservationDTO[]>> => {
  return await request.get('/reservations/today/check-outs')
}

// 更新预订
export const updateReservation = async (
  reservationId: number,
  data: CreateReservationRequest,
): Promise<ApiResponse<ReservationDTO>> => {
  return await request.post(`/reservations/${reservationId}/update`, data)
}

// 获取带分页和筛选的订单列表
export const getReservationsWithFilters = async (
  filters: ReservationFilters = {},
): Promise<ApiResponse<PagedReservationResponse>> => {
  const params = new URLSearchParams()

  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params.append(key, value.toString())
    }
  })

  const queryString = params.toString()
  const url = `/reservations${queryString ? `?${queryString}` : ''}`

  return await request.get(url)
}

// 获取订单统计信息
export const getReservationStatistics = async (): Promise<ApiResponse<ReservationStatistics>> => {
  return await request.get('/reservations/statistics')
}

// 获取今日新增订单
export const getTodayNewReservations = async (): Promise<ApiResponse<ReservationDTO[]>> => {
  return await request.get('/reservations/today/new')
}

// 获取未排房订单
export const getUnassignedReservations = async (): Promise<ApiResponse<ReservationDTO[]>> => {
  return await request.get('/reservations/unassigned')
}

// 获取待处理订单
export const getPendingReservations = async (): Promise<ApiResponse<ReservationDTO[]>> => {
  return await request.get('/reservations/pending')
}

// 根据类型获取订单列表
export const getReservationsByType = async (
  type: string,
): Promise<ApiResponse<ReservationDTO[]>> => {
  return await request.get('/reservations/by-type', {
    params: { type },
  })
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

export const getReservationChannelInfo = async (
  reservationId: number,
): Promise<ApiResponse<ReservationChannelInfoDTO>> => {
  return await request.get(`/reservations/${reservationId}/channel-info`)
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

export const getAssignableRooms = async (
  reservationId: number,
  roomTypeId?: number,
): Promise<ApiResponse<AssignableRoomsResponse>> => {
  return await request.get(`/reservations/${reservationId}/assignable-rooms`, {
    params: {
      roomTypeId,
    },
  })
}

export const assignReservationRoom = async (
  reservationId: number,
  roomId: number,
): Promise<ApiResponse<ReservationDTO>> => {
  return await request.post(`/reservations/${reservationId}/assign-room`, {
    roomId,
  })
}

export const updateReservationSettlementStatus = async (
  reservationId: number,
  data: UpdateReservationSettlementStatusRequest,
): Promise<ApiResponse<ReservationDTO>> => {
  return await request.post(`/reservations/${reservationId}/settlement-status`, data)
}
