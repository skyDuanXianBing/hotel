import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export type PriceAdjustmentType = 'COMMISSION' | 'FIXED' | 'PERCENTAGE'

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

export interface ChannelPriceAdjustmentRequest {
  adjustmentType: PriceAdjustmentType
  adjustmentValue: number | null
  autoSyncPrice?: boolean
}

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

export interface PriceLabsSyncLogDTO {
  id: number
  storeId: number
  syncType: string
  direction: string
  status: string
  affectedCount?: number
  errorMessage?: string
  requestData?: string
  responseData?: string
  createdAt: string
  syncTypeDisplay: string
  directionDisplay: string
  statusDisplay: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export const getIntegration = () => {
  return request<ApiResponse<PriceLabsIntegrationDTO>>({
    url: '/pricelabs/integration',
    method: 'GET',
  })
}

export const saveIntegration = (data: Partial<PriceLabsIntegrationDTO>) => {
  return request<ApiResponse<PriceLabsIntegrationDTO>>({
    url: '/pricelabs/integration',
    method: 'POST',
    data,
  })
}

export const toggleIntegration = (enabled: boolean) => {
  return request<ApiResponse<PriceLabsIntegrationDTO>>({
    url: '/pricelabs/integration/toggle',
    method: 'PATCH',
    data: { enabled },
  })
}

export const updateIntegrationConfig = (config: { priceLabsEmail?: string }) => {
  return request<ApiResponse<PriceLabsIntegrationDTO>>({
    url: '/pricelabs/integration/config',
    method: 'PATCH',
    data: config,
  })
}

export const getConnections = () => {
  return request<ApiResponse<PriceLabsConnectionDTO[]>>({
    url: '/pricelabs/connections',
    method: 'GET',
  })
}

export const createConnection = (roomTypeId: number, pricePlanId: number) => {
  return request<ApiResponse<PriceLabsConnectionDTO>>({
    url: '/pricelabs/connections',
    method: 'POST',
    data: { roomTypeId, pricePlanId },
  })
}

export const updateConnectionStatus = (id: number, enabled: boolean) => {
  return request<ApiResponse<PriceLabsConnectionDTO>>({
    url: `/pricelabs/connections/${id}/status`,
    method: 'PATCH',
    data: { enabled },
  })
}

export const deleteConnection = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/pricelabs/connections/${id}`,
    method: 'DELETE',
  })
}

export const batchUpdateChannelPriceAdjustments = (adjustments: ChannelPriceAdjustmentDTO[]) => {
  return request<ApiResponse<ChannelPriceAdjustmentDTO[]>>({
    url: '/pricelabs/channel-adjustments/batch',
    method: 'PUT',
    data: adjustments,
  })
}

export const recalculateChannelPrices = (channelId: number, startDate: string, endDate: string) => {
  return request<ApiResponse<{ affectedCount: number }>>({
    url: `/pricelabs/channel-adjustments/${channelId}/recalculate`,
    method: 'POST',
    params: { startDate, endDate },
  })
}

export const getRecentSyncLogs = (limit = 10) => {
  return request<ApiResponse<PriceLabsSyncLogDTO[]>>({
    url: '/pricelabs/sync-logs/recent',
    method: 'GET',
    params: { limit },
  })
}

export const manualSync = () => {
  return request<ApiResponse<{ message: string }>>({
    url: '/pricelabs/sync/manual',
    method: 'POST',
  })
}

export const syncRoomType = (roomTypeId: number, days?: number) => {
  return request<ApiResponse<{ message: string }>>({
    url: `/pricelabs/sync/room-type/${roomTypeId}`,
    method: 'POST',
    params: { days },
  })
}

export const getChannelPriceAdjustments = () => {
  return request.get<ApiResponse<ChannelPriceAdjustmentDTO[]>>('/pricelabs/channel-adjustments')
}

export const updateChannelPriceAdjustment = (
  channelId: number,
  data: ChannelPriceAdjustmentRequest,
) => {
  return request.put<ApiResponse<ChannelPriceAdjustmentDTO>>(
    `/pricelabs/channel-adjustments/${channelId}`,
    data,
  )
}

const pricelabsApi = {
  getIntegration,
  saveIntegration,
  toggleIntegration,
  updateIntegrationConfig,
  getConnections,
  createConnection,
  updateConnectionStatus,
  deleteConnection,
  getChannelPriceAdjustments,
  updateChannelPriceAdjustment,
  batchUpdateChannelPriceAdjustments,
  recalculateChannelPrices,
  getRecentSyncLogs,
  manualSync,
  syncRoomType,
}

export default pricelabsApi
