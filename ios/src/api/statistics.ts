import type { ApiResponse } from '@/types/api'
import request from '@/utils/request'

export interface DateRangeParams {
  startDate: string
  endDate: string
}

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
  orderCount: number | null
}

interface RawChannelDetail {
  channelName?: string | null
  revenue?: number | null
  roomNights?: number | null
  averagePrice?: number | null
  orderCount?: number | null
  totalRevenue?: number | null
  totalRoomNights?: number | null
}

interface RawChannelSummaryDTO {
  totalRevenue?: number | null
  totalRoomNights?: number | null
  revenueDistribution?: ChannelDistribution[] | null
  nightsDistribution?: ChannelDistribution[] | null
  revenueTrend?: ChannelTrend[] | null
  nightsTrend?: ChannelTrend[] | null
  channelDetails?: RawChannelDetail[] | null
}

export interface SalesSummaryQueryParams extends DateRangeParams {
  keyword?: string
  channelId?: number
}

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

export interface OperationalMetricsDTO {
  totalRoomFee: number
  averageDailyRate: number
  occupancyRate: number
  revPAR: number
  totalSoldRoomNights: number
  totalAvailableRoomNights: number
  totalRooms: number
  days: number
  dailyTrends: DailyMetricsDTO[]
  roomFeeDetails: RoomDetailDTO[]
  roomNightsDetails: RoomDetailDTO[]
  occupancyDetails: RoomDetailDTO[]
  revparDetails: RoomDetailDTO[]
}

export interface DailyMetricsDTO {
  date: string
  totalRoomFee: number
  averageDailyRate: number
  revPAR: number
  roomNights: number
  occupancyRate: number
}

export interface RoomDetailDTO {
  roomType: string
  roomNumber: string
  total: number
  currentDate: number
}

interface RawOperationalMetricsDTO {
  totalRoomFee?: number | null
  averageDailyRate?: number | null
  occupancyRate?: number | null
  revPAR?: number | null
  totalSoldRoomNights?: number | null
  totalAvailableRoomNights?: number | null
  totalRooms?: number | null
  days?: number | null
  dailyTrends?: DailyMetricsDTO[] | null
  roomFeeDetails?: RoomDetailDTO[] | null
  roomNightsDetails?: RoomDetailDTO[] | null
  occupancyDetails?: RoomDetailDTO[] | null
  revparDetails?: RoomDetailDTO[] | null
}

function toNumber(value: unknown) {
  const nextValue = Number(value)
  if (Number.isFinite(nextValue)) {
    return nextValue
  }

  return 0
}

function normalizeChannelDistribution(item: ChannelDistribution): ChannelDistribution {
  return {
    channelName: item.channelName || '未知渠道',
    value: toNumber(item.value),
    percentage: toNumber(item.percentage),
  }
}

function normalizeChannelTrendItem(item: ChannelTrend): ChannelTrend {
  const nextChannels = Array.isArray(item.channels)
    ? item.channels.map((channel) => {
        return {
          channelName: channel.channelName || '未知渠道',
          value: toNumber(channel.value),
        }
      })
    : []

  return {
    date: item.date || '',
    channels: nextChannels,
  }
}

function normalizeChannelDetail(item: RawChannelDetail): ChannelDetail {
  const revenue = toNumber(item.revenue ?? item.totalRevenue)
  const roomNights = toNumber(item.roomNights ?? item.totalRoomNights)

  let averagePrice = 0
  if (item.averagePrice !== undefined && item.averagePrice !== null) {
    averagePrice = toNumber(item.averagePrice)
  } else if (roomNights > 0) {
    averagePrice = revenue / roomNights
  }

  let orderCount: number | null = null
  if (item.orderCount !== undefined && item.orderCount !== null) {
    orderCount = toNumber(item.orderCount)
  }

  return {
    channelName: item.channelName || '未知渠道',
    revenue,
    roomNights,
    averagePrice,
    orderCount,
  }
}

function normalizeChannelSummary(data?: RawChannelSummaryDTO | null): ChannelSummaryDTO {
  const revenueDistribution = Array.isArray(data?.revenueDistribution)
    ? data.revenueDistribution.map((item) => normalizeChannelDistribution(item))
    : []
  const nightsDistribution = Array.isArray(data?.nightsDistribution)
    ? data.nightsDistribution.map((item) => normalizeChannelDistribution(item))
    : []
  const revenueTrend = Array.isArray(data?.revenueTrend)
    ? data.revenueTrend.map((item) => normalizeChannelTrendItem(item))
    : []
  const nightsTrend = Array.isArray(data?.nightsTrend)
    ? data.nightsTrend.map((item) => normalizeChannelTrendItem(item))
    : []
  const channelDetails = Array.isArray(data?.channelDetails)
    ? data.channelDetails.map((item) => normalizeChannelDetail(item))
    : []

  return {
    totalRevenue: toNumber(data?.totalRevenue),
    totalRoomNights: toNumber(data?.totalRoomNights),
    revenueDistribution,
    nightsDistribution,
    revenueTrend,
    nightsTrend,
    channelDetails,
  }
}

function normalizeDailyMetricsItem(item: DailyMetricsDTO): DailyMetricsDTO {
  return {
    date: item.date || '',
    totalRoomFee: toNumber(item.totalRoomFee),
    averageDailyRate: toNumber(item.averageDailyRate),
    revPAR: toNumber(item.revPAR),
    roomNights: toNumber(item.roomNights),
    occupancyRate: toNumber(item.occupancyRate),
  }
}

function normalizeRoomDetailItem(item: RoomDetailDTO): RoomDetailDTO {
  return {
    roomType: item.roomType || '未知房型',
    roomNumber: item.roomNumber || '-',
    total: toNumber(item.total),
    currentDate: toNumber(item.currentDate),
  }
}

function normalizeOperationalMetrics(data?: RawOperationalMetricsDTO | null): OperationalMetricsDTO {
  const dailyTrends = Array.isArray(data?.dailyTrends)
    ? data.dailyTrends.map((item) => normalizeDailyMetricsItem(item))
    : []
  const roomFeeDetails = Array.isArray(data?.roomFeeDetails)
    ? data.roomFeeDetails.map((item) => normalizeRoomDetailItem(item))
    : []
  const roomNightsDetails = Array.isArray(data?.roomNightsDetails)
    ? data.roomNightsDetails.map((item) => normalizeRoomDetailItem(item))
    : []
  const occupancyDetails = Array.isArray(data?.occupancyDetails)
    ? data.occupancyDetails.map((item) => normalizeRoomDetailItem(item))
    : []
  const revparDetails = Array.isArray(data?.revparDetails)
    ? data.revparDetails.map((item) => normalizeRoomDetailItem(item))
    : []

  return {
    totalRoomFee: toNumber(data?.totalRoomFee),
    averageDailyRate: toNumber(data?.averageDailyRate),
    occupancyRate: toNumber(data?.occupancyRate),
    revPAR: toNumber(data?.revPAR),
    totalSoldRoomNights: toNumber(data?.totalSoldRoomNights),
    totalAvailableRoomNights: toNumber(data?.totalAvailableRoomNights),
    totalRooms: toNumber(data?.totalRooms),
    days: toNumber(data?.days),
    dailyTrends,
    roomFeeDetails,
    roomNightsDetails,
    occupancyDetails,
    revparDetails,
  }
}

function normalizeApiResponse<TRawData, TData>(
  response: ApiResponse<TRawData>,
  normalizeData: (data?: TRawData | null) => TData,
): ApiResponse<TData> {
  return {
    ...response,
    data: normalizeData(response.data),
  }
}

export const getBusinessOverview = (params: DateRangeParams) => {
  return request.get<ApiResponse<BusinessOverviewDTO>>('/statistics/business/overview', {
    params: { ...params },
  })
}

export const getRevenueSummary = (params: DateRangeParams) => {
  return request.get<ApiResponse<RevenueSummaryDTO>>('/statistics/business/revenue-summary', {
    params: { ...params },
  })
}

export const getChannelSummary = async (params: DateRangeParams) => {
  const response = await request.get<ApiResponse<RawChannelSummaryDTO>>('/statistics/business/channel-summary', {
    params: { ...params },
  })

  return normalizeApiResponse(response, normalizeChannelSummary)
}

export const getSalesSummary = (params: SalesSummaryQueryParams) => {
  return request.get<ApiResponse<SalesSummaryDTO>>('/statistics/business/sales-summary', {
    params: { ...params },
  })
}

export const getOperationalMetrics = async (params: DateRangeParams) => {
  const response = await request.get<ApiResponse<RawOperationalMetricsDTO>>('/statistics/business/operational-metrics', {
    params: { ...params },
  })

  return normalizeApiResponse(response, normalizeOperationalMetrics)
}

const statisticsApi = {
  getBusinessOverview,
  getRevenueSummary,
  getChannelSummary,
  getSalesSummary,
  getOperationalMetrics,
}

export default statisticsApi
