import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 营业汇总DTO
export interface BusinessSummaryDTO {
  totalRevenue: number // 总营业收入
  totalOrders: number // 总订单数
  totalRoomNights: number // 总间夜数
  averageRoomRate: number // 平均房价
  occupancyRate: number // 出租率
  revenueByChannel: ChannelRevenue[] // 按渠道统计
  revenueByRoomType: RoomTypeRevenue[] // 按房型统计
  revenueByDate: DailyRevenue[] // 按日期统计
}

// 渠道收入统计
export interface ChannelRevenue {
  channelName: string
  revenue: number
  orderCount: number
  roomNights: number
}

// 房型收入统计
export interface RoomTypeRevenue {
  roomTypeName: string
  revenue: number
  orderCount: number
  roomNights: number
}

// 每日收入统计
export interface DailyRevenue {
  date: string
  revenue: number
  orderCount: number
  roomNights: number
}

// 营业汇总查询参数
export interface BusinessQueryParams {
  startDate: string // YYYY-MM-DD
  endDate: string // YYYY-MM-DD
}

// 获取营业汇总统计
export const getBusinessSummary = async (
  params: BusinessQueryParams
): Promise<ApiResponse<BusinessSummaryDTO>> => {
  return await request.get('/statistics/business/summary', { params })
}

// 每日入住率DTO
export interface DailyOccupancyDTO {
  date: string
  rate: number
  occupiedRooms: number
  totalRooms: number
}

// 获取每日入住率统计
export const getDailyOccupancy = async (
  params: BusinessQueryParams
): Promise<ApiResponse<DailyOccupancyDTO[]>> => {
  return await request.get('/statistics/business/daily-occupancy', { params })
}