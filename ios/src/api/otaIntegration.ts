import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export type PriceAdjustmentType = 'PERCENTAGE' | 'FIXED'

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

export interface WidgetTokenResponse {
  tokenId: string
  propertyId: string
  appId: string
  channelCode: string
  scriptUrl: string
  type: string
  language: string
}

export interface SuRoomSyncSummary {
  roomCount: number
  roomsSynced: boolean
  roomsError: string | null
}

export interface SuRatePlanSyncSummary {
  pricePlanCount: number
  ratePlansSynced: boolean
  ratePlansError: string | null
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

export interface SuAvailabilitySyncSummary {
  roomCount: number
  days: number
  startDate: string
  endDate: string
  availabilitySynced: boolean
  availabilityError: string | null
}

export interface SuMappingStatusSummary {
  channelId: string | null
  mappingReady: boolean
  mappedRoomIdCount: number
  mappedRatePlanCount: number
  activeRatePlanCount: number
  error: string | null
}

export interface SuMappingPricing {
  ApplicableNoOfGuest?: string
  Multiplier?: string
  Surcharge?: string
}

export interface SuMappingRatePlan {
  PMSRoomID?: string
  PMSRateID?: string
  ChannelRoomID?: string
  ChannelRateID?: string
  ChannelMappingName?: string
  MappingStatus?: string
  PricingModel?: string
  Pricing?: SuMappingPricing
  DisableRates?: boolean
  DisableAvailablity?: boolean
}

export interface SuChannelMappingEntry {
  ChannelID?: string
  ChannelHotelID?: string
  Status?: string
  RoomIDs?: string[]
  Rateplans?: SuMappingRatePlan[]
}

export type SuMappingsResponse = Record<string, SuChannelMappingEntry[]> & {
  Status?: string
  Errors?: {
    Code?: string
    ShortText?: string
  }
}

export const getAllOtaIntegrations = () => {
  return request.get<ApiResponse<OtaIntegrationDTO[]>>('/ota-integrations')
}

export const getOtaIntegrationById = (id: number) => {
  return request.get<ApiResponse<OtaIntegrationDTO>>(`/ota-integrations/${id}`)
}

export const getOtaIntegrationByCode = (code: string) => {
  return request.get<ApiResponse<OtaIntegrationDTO>>(`/ota-integrations/code/${code}`)
}

export const disconnectOta = (id: number) => {
  return request.post<ApiResponse<OtaIntegrationDTO>>(`/ota-integrations/${id}/disconnect`)
}

export const getSuWidgetToken = (
  id: number,
  options?: {
    syncContent?: boolean
    language?: 'zn' | 'en'
  },
) => {
  return request.get<ApiResponse<WidgetTokenResponse>>(`/ota-integrations/${id}/su-widget-token`, {
    params: {
      syncContent: options?.syncContent,
      language: options?.language,
    },
  })
}

export const syncSuRooms = (id: number) => {
  return request.post<ApiResponse<SuRoomSyncSummary>>(`/ota-integrations/${id}/su-content/sync-rooms`)
}

export const syncSuRatePlans = (id: number) => {
  return request.post<ApiResponse<SuRatePlanSyncSummary>>(
    `/ota-integrations/${id}/su-content/sync-rate-plans`,
  )
}

export const syncSuAri = (id: number, days?: number) => {
  return request.post<ApiResponse<SuAriSyncSummary>>(
    `/ota-integrations/${id}/su-content/sync-ari`,
    undefined,
    {
      params: { days },
    },
  )
}

export const syncSuRates = (id: number, days?: number) => {
  return request.post<ApiResponse<SuRateSyncSummary>>(
    `/ota-integrations/${id}/su-content/sync-rates`,
    undefined,
    {
      params: { days },
    },
  )
}

export const syncSuAvailability = (id: number, days?: number) => {
  return request.post<ApiResponse<SuAvailabilitySyncSummary>>(
    `/ota-integrations/${id}/su-content/sync-availability`,
    undefined,
    {
      params: { days },
    },
  )
}

export const getSuMappings = (id: number, channelId?: string) => {
  return request.get<ApiResponse<SuMappingsResponse>>(`/ota-integrations/${id}/su-mappings`, {
    params: { channelId },
  })
}

export const getSuMappingStatus = (id: number, channelId?: string) => {
  return request.get<ApiResponse<SuMappingStatusSummary>>(
    `/ota-integrations/${id}/su-mapping-status`,
    {
      params: { channelId },
    },
  )
}

const otaIntegrationApi = {
  getAllOtaIntegrations,
  getOtaIntegrationById,
  getOtaIntegrationByCode,
  disconnectOta,
  getSuWidgetToken,
  syncSuRooms,
  syncSuRatePlans,
  syncSuAri,
  syncSuRates,
  syncSuAvailability,
  getSuMappings,
  getSuMappingStatus,
}

export default otaIntegrationApi
