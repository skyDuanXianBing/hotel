import { request } from '@/utils/request'

/**
 * 价格调整类型
 */
export type PriceAdjustmentType = 'PERCENTAGE' | 'FIXED'

/**
 * OTA直连配置接口定义
 */
export interface OtaIntegrationDTO {
  id: number
  name: string
  code: string
  logoUrl: string
  isConnected: boolean
  apiUrl?: string
  propertyId?: string
  enabled: boolean
  priceAdjustmentType?: PriceAdjustmentType
  priceAdjustmentValue?: number
  autoSyncPrice?: boolean
  createdAt: string
  updatedAt: string
}

/**
 * 更新OTA配置请求
 */
export interface UpdateOtaIntegrationRequest {
  apiUrl?: string
  propertyId?: string
  isConnected?: boolean
  enabled?: boolean
}

/**
 * 连接OTA请求
 */
export interface ConnectOtaRequest {
  apiKey: string
  apiSecret: string
}

/**
 * API响应格式
 */
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

/**
 * 获取所有OTA直连配置
 */
export const getAllOtaIntegrations = async (): Promise<ApiResponse<OtaIntegrationDTO[]>> => {
  return await request.get('/ota-integrations')
}

/**
 * 根据ID获取OTA直连配置
 */
export const getOtaIntegrationById = async (id: number): Promise<ApiResponse<OtaIntegrationDTO>> => {
  return await request.get(`/ota-integrations/${id}`)
}

/**
 * 根据渠道代码获取OTA直连配置
 */
export const getOtaIntegrationByCode = async (
  code: string,
): Promise<ApiResponse<OtaIntegrationDTO>> => {
  return await request.get(`/ota-integrations/code/${code}`)
}

/**
 * 更新OTA直连配置
 */
export const updateOtaIntegration = async (
  id: number,
  data: UpdateOtaIntegrationRequest,
): Promise<ApiResponse<OtaIntegrationDTO>> => {
  return await request.put(`/ota-integrations/${id}`, data)
}

/**
 * 连接OTA渠道
 */
export const connectOta = async (
  id: number,
  data: ConnectOtaRequest,
): Promise<ApiResponse<OtaIntegrationDTO>> => {
  return await request.post(`/ota-integrations/${id}/connect`, data)
}

/**
 * 断开OTA渠道连接
 */
export const disconnectOta = async (id: number): Promise<ApiResponse<OtaIntegrationDTO>> => {
  return await request.post(`/ota-integrations/${id}/disconnect`)
}

/**
 * 价格调整请求
 */
export interface PriceAdjustmentRequest {
  priceAdjustmentType: PriceAdjustmentType
  priceAdjustmentValue: number
  autoSyncPrice?: boolean
}

/**
 * 更新价格调整配置
 */
export const updatePriceAdjustment = async (
  id: number,
  data: PriceAdjustmentRequest,
): Promise<ApiResponse<OtaIntegrationDTO>> => {
  return await request.put(`/ota-integrations/${id}/price-adjustment`, data)
}

/**
 * Su Widget Token响应
 */
export interface WidgetTokenResponse {
  tokenId: string
  propertyId: string
  appId: string
  channelCode: string
  scriptUrl: string
  type: string
  language: string
}

/**
 * 获取Su Widget Token
 * 用于加载Su Channel Mapping Widget
 */
export const getSuWidgetToken = async (
  id: number,
): Promise<ApiResponse<WidgetTokenResponse>> => {
  return await request.get(`/ota-integrations/${id}/su-widget-token`)
}

/**
 * Su 房间号列表同步结果
 */
export interface SuRoomSyncSummary {
  roomCount: number
  roomsSynced: boolean
  roomsError: string | null
}

/**
 * 一键推送 PMS 房间号列表到 Su（用于 Widget 映射到具体房间）
 */
export const syncSuRooms = async (id: number): Promise<ApiResponse<SuRoomSyncSummary>> => {
  return await request.post(`/ota-integrations/${id}/su-content/sync-rooms`)
}

export interface SuRatePlanSyncSummary {
  pricePlanCount: number
  ratePlansSynced: boolean
  ratePlansError: string | null
}

/**
 * 一键推送 PMS 价格计划列表到 Su（用于 Widget 费率计划映射）
 */
export const syncSuRatePlans = async (id: number): Promise<ApiResponse<SuRatePlanSyncSummary>> => {
  return await request.post(`/ota-integrations/${id}/su-content/sync-rate-plans`)
}

export interface SuAvailabilitySyncSummary {
  roomCount: number
  days: number
  startDate: string
  endDate: string
  availabilitySynced: boolean
  availabilityError: string | null
}

export interface SuMissingPrice {
  roomId: string
  ratePlanId: string
  missingDays: number
}

export interface SuAriSyncSummary {
  roomCount: number
  ratePlanCount: number
  days: number
  startDate: string
  endDate: string
  availabilityPushed: boolean
  ratesPushed: boolean
  availabilitySegments: number
  rateSegments: number
  requestsPosted: number
  error: string | null
  missingPrices: SuMissingPrice[]
}

export const syncSuAri = async (
  id: number,
  days?: number,
): Promise<ApiResponse<SuAriSyncSummary>> => {
  return await request.post(`/ota-integrations/${id}/su-content/sync-ari`, undefined, {
    params: { days },
  })
}

/**
 * PMS -> Su：推送房间号级别可用性（InventoryControl）
 */
export interface SuMissingRate {
  roomId: string
  rateId: string
  missingDays: number
}

export interface SuRateSyncSummary {
  roomCount: number
  ratePlanCount: number
  combinationsConsidered: number
  combinationsPushed: number
  combinationsSkippedNoPrice: number
  requestsPosted: number
  days: number
  startDate: string
  endDate: string
  rateSynced: boolean
  error: string | null
  missingRates: SuMissingRate[]
}

export const syncSuRates = async (
  id: number,
  days?: number,
): Promise<ApiResponse<SuRateSyncSummary>> => {
  return await request.post(`/ota-integrations/${id}/su-content/sync-rates`, undefined, {
    params: { days },
  })
}

export const syncSuAvailability = async (
  id: number,
  days?: number,
): Promise<ApiResponse<SuAvailabilitySyncSummary>> => {
  return await request.post(`/ota-integrations/${id}/su-content/sync-availability`, undefined, {
    params: { days },
  })
}
