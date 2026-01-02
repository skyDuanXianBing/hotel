import { request } from '@/utils/request'

// ==================== 类型定义 ====================

/**
 * 价格调整类型枚举
 */
export type PriceAdjustmentType = 'COMMISSION' | 'FIXED' | 'PERCENTAGE'

/**
 * 同步类型枚举
 */
export type SyncType = 'LISTING' | 'RATE_PLAN' | 'CALENDAR' | 'RESERVATION' | 'PRICE_UPDATE'

/**
 * 同步方向枚举
 */
export type SyncDirection = 'OUTBOUND' | 'INBOUND'

/**
 * 同步状态枚举
 */
export type SyncStatus = 'SUCCESS' | 'FAILURE' | 'PARTIAL'

/**
 * API 响应格式
 */
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

/**
 * 分页响应格式
 */
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

/**
 * PriceLabs 集成配置 DTO
 */
export interface PriceLabsIntegrationDTO {
  id?: number
  storeId?: number
  isEnabled: boolean
  priceLabsEmail?: string
  syncUrl?: string
  calendarTriggerUrl?: string
  hookUrl?: string
  lastListingSyncAt?: string
  lastPriceSyncAt?: string
  lastReservationSyncAt?: string
  createdAt?: string
  updatedAt?: string
  connectedRoomTypeCount?: number
  totalSyncCount?: number
  successSyncCount?: number
}

/**
 * PriceLabs 连接配置 DTO
 */
export interface PriceLabsConnectionDTO {
  id: number
  storeId: number
  roomTypeId: number
  roomTypeName: string
  pricePlanId: number
  pricePlanName: string
  priceLabsListingId: string
  isEnabled: boolean
  lastSyncAt?: string
  syncStatus: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

/**
 * 渠道价格 DTO
 */
export interface ChannelPriceDTO {
  id: number
  storeId: number
  roomTypeId: number
  roomTypeName: string
  pricePlanId: number
  pricePlanName: string
  channelId: number
  channelName: string
  channelCode: string
  priceDate: string
  basePrice: number
  channelPrice: number
  minStay?: number
  maxStay?: number
  isSyncedToOta: boolean
  otaSyncAt?: string
  priceLabsUpdatedAt?: string
  createdAt: string
  updatedAt: string
}

/**
 * 渠道价格调整 DTO
 */
export interface ChannelPriceAdjustmentDTO {
  channelId: number
  channelName: string
  channelCode: string
  adjustmentType: PriceAdjustmentType
  adjustmentValue: number | null
  autoSyncPrice: boolean
  exampleBasePrice?: number
  exampleChannelPrice?: number
}

/**
 * 同步日志 DTO
 */
export interface PriceLabsSyncLogDTO {
  id: number
  storeId: number
  syncType: SyncType
  direction: SyncDirection
  status: SyncStatus
  affectedCount?: number
  errorMessage?: string
  requestData?: string
  responseData?: string
  createdAt: string
  syncTypeDisplay: string
  directionDisplay: string
  statusDisplay: string
}

/**
 * 渠道价格调整请求
 */
export interface ChannelPriceAdjustmentRequest {
  adjustmentType: PriceAdjustmentType
  adjustmentValue: number | null
  autoSyncPrice?: boolean
}

// ==================== API 接口 ====================

/**
 * 获取 PriceLabs 集成配置
 */
export const getIntegration = async (): Promise<ApiResponse<PriceLabsIntegrationDTO>> => {
  return await request.get('/pricelabs/integration')
}

/**
 * 保存或更新集成配置
 */
export const saveIntegration = async (
  data: Partial<PriceLabsIntegrationDTO>,
): Promise<ApiResponse<PriceLabsIntegrationDTO>> => {
  return await request.post('/pricelabs/integration', data)
}

/**
 * 启用/禁用集成
 */
export const toggleIntegration = async (
  enabled: boolean,
): Promise<ApiResponse<PriceLabsIntegrationDTO>> => {
  return await request.patch('/pricelabs/integration/toggle', { enabled })
}

/**
 * 更新集成配置
 */
export const updateIntegrationConfig = async (
  config: Partial<PriceLabsIntegrationDTO>,
): Promise<ApiResponse<PriceLabsIntegrationDTO>> => {
  return await request.patch('/pricelabs/integration/config', config)
}

// ==================== 连接配置 API ====================

/**
 * 获取所有连接配置
 */
export const getConnections = async (): Promise<ApiResponse<PriceLabsConnectionDTO[]>> => {
  return await request.get('/pricelabs/connections')
}

/**
 * 创建连接配置
 */
export const createConnection = async (
  roomTypeId: number,
  pricePlanId: number,
): Promise<ApiResponse<PriceLabsConnectionDTO>> => {
  return await request.post('/pricelabs/connections', { roomTypeId, pricePlanId })
}

/**
 * 更新连接状态
 */
export const updateConnectionStatus = async (
  id: number,
  enabled: boolean,
): Promise<ApiResponse<PriceLabsConnectionDTO>> => {
  return await request.patch(`/pricelabs/connections/${id}/status`, { enabled })
}

/**
 * 删除连接配置
 */
export const deleteConnection = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/pricelabs/connections/${id}`)
}

// ==================== 渠道价格调整 API ====================

/**
 * 获取所有渠道的价格调整设置
 */
export const getChannelPriceAdjustments = async (): Promise<
  ApiResponse<ChannelPriceAdjustmentDTO[]>
> => {
  return await request.get('/pricelabs/channel-adjustments')
}

/**
 * 更新单个渠道的价格调整设置
 */
export const updateChannelPriceAdjustment = async (
  channelId: number,
  data: ChannelPriceAdjustmentRequest,
): Promise<ApiResponse<ChannelPriceAdjustmentDTO>> => {
  return await request.put(`/pricelabs/channel-adjustments/${channelId}`, data)
}

/**
 * 批量更新渠道价格调整设置
 */
export const batchUpdateChannelPriceAdjustments = async (
  adjustments: ChannelPriceAdjustmentDTO[],
): Promise<ApiResponse<ChannelPriceAdjustmentDTO[]>> => {
  return await request.put('/pricelabs/channel-adjustments/batch', adjustments)
}

/**
 * 重新计算渠道价格
 */
export const recalculateChannelPrices = async (
  channelId: number,
  startDate: string,
  endDate: string,
): Promise<ApiResponse<{ affectedCount: number }>> => {
  return await request.post(
    `/pricelabs/channel-adjustments/${channelId}/recalculate?startDate=${startDate}&endDate=${endDate}`,
  )
}

// ==================== 渠道价格查询 API ====================

/**
 * 获取渠道价格列表
 */
export const getChannelPrices = async (params: {
  roomTypeId?: number
  channelId?: number
  startDate: string
  endDate: string
}): Promise<ApiResponse<ChannelPriceDTO[]>> => {
  const queryParams = new URLSearchParams()
  if (params.roomTypeId) queryParams.append('roomTypeId', params.roomTypeId.toString())
  if (params.channelId) queryParams.append('channelId', params.channelId.toString())
  queryParams.append('startDate', params.startDate)
  queryParams.append('endDate', params.endDate)
  return await request.get(`/pricelabs/prices?${queryParams.toString()}`)
}

/**
 * 获取未同步到 OTA 的价格
 */
export const getUnsyncedPrices = async (
  channelId?: number,
): Promise<ApiResponse<ChannelPriceDTO[]>> => {
  const url = channelId ? `/pricelabs/prices/unsynced?channelId=${channelId}` : '/pricelabs/prices/unsynced'
  return await request.get(url)
}

// ==================== 同步日志 API ====================

/**
 * 获取同步日志（分页）
 */
export const getSyncLogs = async (
  page: number = 0,
  size: number = 20,
): Promise<ApiResponse<PageResponse<PriceLabsSyncLogDTO>>> => {
  return await request.get(`/pricelabs/sync-logs?page=${page}&size=${size}`)
}

/**
 * 获取最近的同步日志
 */
export const getRecentSyncLogs = async (
  limit: number = 10,
): Promise<ApiResponse<PriceLabsSyncLogDTO[]>> => {
  return await request.get(`/pricelabs/sync-logs/recent?limit=${limit}`)
}

// ==================== 手动同步 API ====================

/**
 * 手动触发同步
 */
export const manualSync = async (): Promise<ApiResponse<{ message: string }>> => {
  return await request.post('/pricelabs/sync/manual')
}
