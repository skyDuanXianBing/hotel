import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 查询参数
export interface DateRangeParams {
  startDate: string
  endDate: string
}

// ==================== 营业汇总统计 ====================
export interface BusinessSummaryDTO {
  totalRevenue: number
  totalOrders: number
  averageOrderValue: number
  occupancyRate: number
  topChannels: ChannelStat[]
  revenueByDate: DailyRevenueStat[]
}

export interface ChannelStat {
  channelName: string
  revenue: number
  orderCount: number
}

export interface DailyRevenueStat {
  date: string
  revenue: number
  orderCount: number
}

// ==================== 每日入住率统计 ====================
export interface DailyOccupancyDTO {
  date: string
  occupancyRate: number
  occupiedRooms: number
  totalRooms: number
}

// ==================== 营业概况统计 ====================
export interface BusinessOverviewDTO {
  totalRevenue: number
  roomFee: number
  deposit: number
  checkoutFee: number
  roomServiceFee: number
  categoryDistribution: CategoryDistribution[]
  consumptionTrend: DailyConsumption[]
  consumptionDetails: ConsumptionDetail[]
}

export interface CategoryDistribution {
  category: string
  value: number
  percentage: number
}

export interface DailyConsumption {
  date: string
  roomFee: number
  deposit: number
  checkoutFee: number
  roomServiceFee: number
}

export interface ConsumptionDetail {
  category: string
  total: number
  dailyAmounts: DailyAmount[]
}

export interface DailyAmount {
  date: string
  amount: number
}

// ==================== 流水汇总统计 ====================
export interface RevenueSummaryDTO {
  totalRevenue: number
  splitAccount: number
  actualReceived: number
  paymentMethodStats: PaymentMethodStat[]
  categoryStats: CategoryStat[]
  incomeDistribution: Distribution[]
  dailyRevenues: DailyRevenue[]
}

export interface PaymentMethodStat {
  paymentMethod: string
  amount: number
  percentage: number
}

export interface CategoryStat {
  category: string
  amount: number
  percentage: number
}

export interface Distribution {
  name: string
  value: number
  percentage: number
}

export interface DailyRevenue {
  date: string
  totalRevenue: number
  splitAccount: number
  actualReceived: number
}

// ==================== 渠道汇总统计 ====================
export interface ChannelSummaryDTO {
  totalRevenue: number
  totalRoomNights: number
  revenueDistribution: ChannelDistribution[]
  nightsDistribution: ChannelDistribution[]
  revenueTrend: ChannelTrend[]
  nightsTrend: ChannelTrend[]
  channelDetails: ChannelDetail[]
}

export interface ChannelDistribution {
  channelName: string
  value: number
  percentage: number
}

export interface ChannelTrend {
  date: string
  channels: ChannelDailyData[]
}

export interface ChannelDailyData {
  channelName: string
  value: number
}

export interface ChannelDetail {
  channelName: string
  revenue: number
  roomNights: number
  averagePrice: number
  orderCount: number
}

// ==================== 销售汇总统计 ====================
export interface SalesSummaryDTO {
  totalSales: number
  totalOrders: number
  dailySalesTrend: DailySales[]
  orderDetails: SalesOrderDetail[]
}

export interface DailySales {
  date: string
  sales: number
  orderCount: number
}

export interface SalesOrderDetail {
  id: number
  orderNumber: string
  channelNumber: string
  createdAt: string
  guestName: string
  channelName: string
  customerName: string
  phone: string
  amount: number
}

// ==================== 经营指标统计 ====================
export interface OperationalMetricsDTO {
  totalRoomFee: number
  averageDailyRate: number // ADR
  occupancyRate: number // Occ
  revPAR: number // RevPAR
  totalSoldRoomNights: number
  totalAvailableRoomNights: number
  totalRooms: number
  days: number
  dailyTrends?: DailyMetricsDTO[]
  roomFeeDetails?: OperationalRoomDetailDTO[]
  roomNightsDetails?: OperationalRoomDetailDTO[]
  occupancyDetails?: OperationalRoomDetailDTO[]
  revparDetails?: OperationalRoomDetailDTO[]
}

export interface DailyMetricsDTO {
  date: string
  totalRoomFee: number
  averageDailyRate: number
  revPAR: number
  roomNights: number
  occupancyRate: number
}

export interface OperationalRoomDetailDTO {
  roomType: string
  roomNumber: string
  total: number
  currentDate: number
}

// ==================== API 调用方法 ====================

/**
 * 获取营业汇总统计
 */
export const getBusinessSummary = async (
  params: DateRangeParams
): Promise<ApiResponse<BusinessSummaryDTO>> => {
  return await request.get('/statistics/business/summary', { params })
}

/**
 * 获取每日入住率统计
 */
export const getDailyOccupancy = async (
  params: DateRangeParams
): Promise<ApiResponse<DailyOccupancyDTO[]>> => {
  return await request.get('/statistics/business/daily-occupancy', { params })
}

/**
 * 获取营业概况详细统计
 */
export const getBusinessOverview = async (
  params: DateRangeParams
): Promise<ApiResponse<BusinessOverviewDTO>> => {
  return await request.get('/statistics/business/overview', { params })
}

/**
 * 获取流水汇总统计
 */
export const getRevenueSummary = async (
  params: DateRangeParams
): Promise<ApiResponse<RevenueSummaryDTO>> => {
  return await request.get('/statistics/business/revenue-summary', { params })
}

/**
 * 获取渠道汇总统计
 */
export const getChannelSummary = async (
  params: DateRangeParams
): Promise<ApiResponse<ChannelSummaryDTO>> => {
  return await request.get('/statistics/business/channel-summary', { params })
}

/**
 * 获取销售汇总统计
 */
export const getSalesSummary = async (params: {
  startDate: string
  endDate: string
  keyword?: string
  channelId?: number
}): Promise<ApiResponse<SalesSummaryDTO>> => {
  return await request.get('/statistics/business/sales-summary', { params })
}

/**
 * 获取经营指标统计
 */
export const getOperationalMetrics = async (
  params: DateRangeParams
): Promise<ApiResponse<OperationalMetricsDTO>> => {
  return await request.get('/statistics/business/operational-metrics', { params })
}
