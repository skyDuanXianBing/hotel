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
  channelId?: number
  roomTypeId?: number
  keyword?: string
  customer?: string
  page?: number
  pageSize?: number
}

export interface StatisticsSourceMetadataDTO {
  metric: string
  sourceType: string
  dateBasis: string
  amountBasis: string
  note?: string
}

export interface StatisticsDataGapDTO {
  metric: string
  reason: string
  requiredSource?: string
  unsupported?: boolean
}

export interface RevenuePrecisionDTO {
  priceBasis?: string
  dateBasis?: string
  taxMode?: string
  currencyCode?: string
  exactRoomNights?: number
  averagedRoomNights?: number
  totalRoomNights?: number
  coverageRate?: number
  residualConflictCount?: number
  residualConflictDetected?: boolean
}

// ==================== 营业汇总统计 ====================
export interface BusinessSummaryDTO {
  totalRevenue: number
  totalOrders: number
  totalRoomNights?: number
  averageRoomRate?: number
  averageOrderValue?: number
  occupancyRate: number
  revenuePrecision?: RevenuePrecisionDTO
  topChannels?: ChannelStat[]
  revenueByChannel?: ChannelStat[]
  revenueByRoomType?: RoomTypeRevenueStat[]
  revenueByDate: DailyRevenueStat[]
}

export interface ChannelStat {
  channelName: string
  revenue: number
  orderCount: number
  roomNights?: number
}

export interface RoomTypeRevenueStat {
  roomTypeName: string
  revenue: number
  orderCount: number
  roomNights: number
}

export interface DailyRevenueStat {
  date: string
  revenue: number
  orderCount: number
  roomNights?: number
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
  notesIncome?: number
  notesExpense?: number
  netRevenue?: number
  revenuePrecision?: RevenuePrecisionDTO
  sourceMetadata?: StatisticsSourceMetadataDTO[]
  dataGaps?: StatisticsDataGapDTO[]
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
  notesIncome?: number
  notesExpense?: number
}

export interface ConsumptionDetail {
  category: string
  total: number
  dailyAmounts: DailyAmount[]
  sourceType?: string
  unsupported?: boolean
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
  totalIncome?: number
  totalExpense?: number
  netIncome?: number
  roomFee?: number
  deposit?: number
  roomServiceFee?: number
  notesIncome?: number
  notesExpense?: number
  paymentRefund?: number
  revenuePrecision?: RevenuePrecisionDTO
  sourceMetadata?: StatisticsSourceMetadataDTO[]
  dataGaps?: StatisticsDataGapDTO[]
  paymentMethodStats: PaymentMethodStat[]
  categoryStats: CategoryStat[]
  incomeDistribution: Distribution[]
  expenseDistribution?: Distribution[]
  dailyRevenues: DailyRevenue[]
}

export interface PaymentMethodStat {
  paymentMethod: string
  amount: number
  percentage: number
  sourceType?: string
  normalizedType?: string
  transactionCount?: number
  dailyAmounts?: DailyAmount[]
}

export interface CategoryStat {
  category: string
  amount: number
  percentage: number
  sourceType?: string
  transactionCount?: number
  dailyAmounts?: DailyAmount[]
}

export interface Distribution {
  name: string
  value: number
  percentage: number
}

export interface DailyRevenue {
  date: string
  amount?: number
  orderCount?: number
  totalRevenue?: number
  roomFee?: number
  splitAccount?: number
  actualReceived?: number
  deposit?: number
  roomServiceFee?: number
  notesIncome?: number
  notesExpense?: number
  paymentRefund?: number
  totalIncome?: number
  totalExpense?: number
  netIncome?: number
  transactionCount?: number
}

// ==================== 渠道汇总统计 ====================
export interface ChannelSummaryDTO {
  totalRevenue: number
  totalRoomNights: number
  revenuePrecision?: RevenuePrecisionDTO
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
  totalRevenue?: number
  totalRoomNights?: number
  dailyValues?: ChannelDetailDailyValue[]
  revenue?: number
  roomNights?: number
  averagePrice?: number
  orderCount?: number
}

export interface ChannelDetailDailyValue {
  date: string
  revenue: number
  roomNights: number
}

// ==================== 销售汇总统计 ====================
export interface SalesSummaryDTO {
  totalSales: number
  totalOrders: number
  page?: number
  pageSize?: number
  totalRecords?: number
  totalPages?: number
  revenuePrecision?: RevenuePrecisionDTO
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
  allocatedAmount?: number
  totalAmount?: number
  roomTypeName?: string
  checkInDate?: string
  checkOutDate?: string
  allocatedRoomNights?: number
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
  revenuePrecision?: RevenuePrecisionDTO
  sourceMetadata?: StatisticsSourceMetadataDTO[]
  dataGaps?: StatisticsDataGapDTO[]
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
  customer?: string
  page?: number
  pageSize?: number
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

export type StatisticsReportType = 'room-fees' | 'transaction-summary' | 'daily' | string

export interface StatisticsReportParams extends DateRangeParams {
  customer?: string
}

export interface StatisticsReportDownload {
  blob: Blob
  fileName: string
}

const REPORT_CONTENT_DISPOSITION_HEADER = 'content-disposition'
const DEFAULT_REPORT_FILE_NAME = 'statistics-report.csv'
const DEFAULT_REPORT_ERROR_MESSAGE = 'Report download failed'

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const isBlob = (value: unknown): value is Blob => {
  return value instanceof Blob
}

const hasResponseHeaderReader = (
  value: unknown,
): value is { getResponseHeader: (name: string) => string | null } => {
  return isRecord(value) && typeof value.getResponseHeader === 'function'
}

const sanitizeDownloadFileName = (fileName: string) => {
  const sanitized = fileName.replace(/[\\/:*?"<>|]/g, '_').trim()
  return sanitized || DEFAULT_REPORT_FILE_NAME
}

const buildDefaultReportFileName = (type: StatisticsReportType, params: StatisticsReportParams) => {
  if (params.startDate && params.endDate) {
    return sanitizeDownloadFileName(`${type}-${params.startDate}-${params.endDate}.csv`)
  }
  return DEFAULT_REPORT_FILE_NAME
}

const decodeHeaderFileName = (value: string) => {
  const trimmed = value.trim().replace(/^["']|["']$/g, '')
  try {
    return decodeURIComponent(trimmed)
  } catch {
    return trimmed
  }
}

const parseContentDispositionFileName = (headerValue: string | undefined) => {
  if (!headerValue) {
    return ''
  }

  const parts = headerValue.split(';').map((part) => part.trim())
  const encodedPart = parts.find((part) => part.toLowerCase().startsWith('filename*='))
  if (encodedPart) {
    const value = encodedPart.substring(encodedPart.indexOf('=') + 1).replace(/^UTF-8''/i, '')
    return sanitizeDownloadFileName(decodeHeaderFileName(value))
  }

  const plainPart = parts.find((part) => part.toLowerCase().startsWith('filename='))
  if (plainPart) {
    const value = plainPart.substring(plainPart.indexOf('=') + 1)
    return sanitizeDownloadFileName(decodeHeaderFileName(value))
  }

  return ''
}

const getHeaderValue = (headers: unknown, headerName: string) => {
  if (!isRecord(headers)) {
    return ''
  }

  const getter = headers.get
  if (typeof getter === 'function') {
    const value = getter.call(headers, headerName)
    if (typeof value === 'string') {
      return value
    }
  }

  const normalizedHeaderName = headerName.toLowerCase()
  for (const [key, value] of Object.entries(headers)) {
    if (key.toLowerCase() !== normalizedHeaderName) {
      continue
    }
    if (Array.isArray(value)) {
      return String(value[0] || '')
    }
    return String(value || '')
  }

  return ''
}

const getProgressResponseHeader = (event: ProgressEvent | undefined, headerName: string) => {
  const candidates: unknown[] = [event?.target, event?.currentTarget]
  for (const candidate of candidates) {
    if (!hasResponseHeaderReader(candidate)) {
      continue
    }
    const value = candidate.getResponseHeader(headerName)
    if (value) {
      return value
    }
  }
  return ''
}

const readBlobText = async (blob: Blob) => {
  try {
    return await blob.text()
  } catch {
    return ''
  }
}

const extractMessageFromPayload = (payload: unknown, fallbackMessage: string): string => {
  if (typeof payload === 'string') {
    const trimmed = payload.trim()
    if (!trimmed) {
      return fallbackMessage
    }

    try {
      const parsed = JSON.parse(trimmed) as unknown
      return extractMessageFromPayload(parsed, trimmed)
    } catch {
      return trimmed
    }
  }

  if (!isRecord(payload)) {
    return fallbackMessage
  }

  const message = payload.message
  if (typeof message === 'string' && message.trim()) {
    return message.trim()
  }

  const error = payload.error
  if (typeof error === 'string' && error.trim()) {
    return error.trim()
  }

  const data = payload.data
  if (isRecord(data) || typeof data === 'string') {
    return extractMessageFromPayload(data, fallbackMessage)
  }

  return fallbackMessage
}

const getBlobErrorMessage = async (blob: Blob, fallbackMessage: string) => {
  const text = await readBlobText(blob)
  return extractMessageFromPayload(text, fallbackMessage)
}

const shouldTreatBlobAsError = (blob: Blob) => {
  const contentType = blob.type.toLowerCase()
  return contentType.includes('json') || contentType.includes('text/plain')
}

export const getStatisticsReportErrorMessage = async (
  error: unknown,
  fallbackMessage = 'Report download failed',
) => {
  if (isRecord(error)) {
    const response = error.response
    if (isRecord(response)) {
      const responseData = response.data
      if (isBlob(responseData)) {
        return await getBlobErrorMessage(responseData, fallbackMessage)
      }
      return extractMessageFromPayload(responseData, fallbackMessage)
    }

    const message = error.message
    if (typeof message === 'string' && message.trim()) {
      if (message.trim() === DEFAULT_REPORT_ERROR_MESSAGE) {
        return fallbackMessage
      }
      return message.trim()
    }
  }

  return fallbackMessage
}

export const saveBlobDownload = (download: StatisticsReportDownload) => {
  const url = window.URL.createObjectURL(download.blob)
  const link = document.createElement('a')
  link.href = url
  link.download = sanitizeDownloadFileName(download.fileName)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * 下载统计 CSV 报表。后端当前支持 room-fees、transaction-summary、daily。
 */
export const downloadStatisticsReport = async (
  type: StatisticsReportType,
  params: StatisticsReportParams,
): Promise<StatisticsReportDownload> => {
  let contentDispositionHeader = ''
  const response = await request.get(`/statistics/reports/${type}`, {
    params,
    responseType: 'blob',
    suppressErrorToast: true,
    headers: {
      Accept: 'text/csv;charset=UTF-8',
    },
    onDownloadProgress: (progressEvent) => {
      const event = (progressEvent as { event?: ProgressEvent }).event
      const headerValue = getProgressResponseHeader(event, REPORT_CONTENT_DISPOSITION_HEADER)
      if (headerValue) {
        contentDispositionHeader = headerValue
      }
    },
  })
  const fallbackFileName = buildDefaultReportFileName(type, params)
  const headerFileName = parseContentDispositionFileName(contentDispositionHeader)

  if (isBlob(response)) {
    if (shouldTreatBlobAsError(response)) {
      throw new Error(await getBlobErrorMessage(response, DEFAULT_REPORT_ERROR_MESSAGE))
    }
    return {
      blob: response,
      fileName: headerFileName || fallbackFileName,
    }
  }

  if (isRecord(response) && isBlob(response.data)) {
    const contentDisposition =
      contentDispositionHeader || getHeaderValue(response.headers, REPORT_CONTENT_DISPOSITION_HEADER)
    const fileName = parseContentDispositionFileName(contentDisposition) || fallbackFileName
    const blob = response.data
    if (shouldTreatBlobAsError(blob)) {
      throw new Error(await getBlobErrorMessage(blob, DEFAULT_REPORT_ERROR_MESSAGE))
    }
    return {
      blob,
      fileName,
    }
  }

  throw new Error(extractMessageFromPayload(response, DEFAULT_REPORT_ERROR_MESSAGE))
}
