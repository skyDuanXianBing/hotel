import request from '@/utils/request'
import type { ApiResponse, RequestConfig } from '@/types/api'

export interface RoomPriceManagementDTO {
  id: number
  roomTypeId: number
  roomTypeName: string
  roomTypeCode: string
  pricePlanId?: number
  pricePlanName?: string
  priceDate: string
  price: number
  availableRooms?: number
  minStay?: number
  maxStay?: number
  closeRoom?: boolean
  cta?: boolean
  ctd?: boolean
  priceSource?: string
  priceLabsBasePrice?: number
  priceLabsUpdatedAt?: string
  manualOverride?: boolean
  manualOverrideUntil?: string
  isWeekend: boolean
  isHoliday: boolean
  notes?: string
}

export interface UpdatePriceByPlanRequest {
  roomTypeId: number
  pricePlanId: number
  startDate: string
  endDate: string
  weekdays?: number[]
  applyWeekdaysInRange?: boolean
  price?: number
  availableRooms?: number
  minStay?: number
  maxStay?: number
  closeRoom?: boolean
  cta?: boolean
  ctd?: boolean
  notes?: string
}

export interface BulkPriceChangeRequest {
  roomTypeIds: number[]
  dateRanges: Array<{
    startDate: string
    endDate: string
  }>
  weekdays?: number[]
  weekendDifferentiation: boolean
  weekdayPrice: number
  weekendPrice?: number
  notes?: string
}

export interface PriceChangeHistoryDTO {
  id: number
  roomTypeName: string
  pricePlanName: string
  priceDate: string
  applyDays: string
  changeType: string
  changeValue: number
  previousValue?: number
  operator: string
  operateTime: string
  pmsPushTime?: string
  notes?: string
}

export interface PriceChangeHistoryQueryParams {
  operateDateStart?: string
  operateDateEnd?: string
  priceDateStart?: string
  priceDateEnd?: string
  pricePlanId?: string
  roomTypeId?: number
  operator?: string
  pageNum?: number
  pageSize?: number
}

export interface PriceChangeHistoryPageResponse {
  total: number
  records: PriceChangeHistoryDTO[]
  pageNum: number
  pageSize: number
}

export const getRoomPriceManagementData = (
  startDate: string,
  endDate: string,
  roomTypeId?: number,
  options?: Pick<RequestConfig, 'suppressErrorStatuses'>,
) => {
  const params: {
    startDate: string
    endDate: string
    roomTypeId?: number
  } = { startDate, endDate }

  if (roomTypeId) {
    params.roomTypeId = roomTypeId
  }

  return request.get<ApiResponse<RoomPriceManagementDTO[]>>('/room-prices/management', {
    params,
    ...options,
  })
}

export const updatePriceByPlan = (data: UpdatePriceByPlanRequest, operator: string) => {
  return request.post<ApiResponse<RoomPriceManagementDTO[]>>('/room-prices/update-by-plan', data, {
    params: { operator },
  })
}

export const bulkPriceChange = (data: BulkPriceChangeRequest) => {
  return request.post<ApiResponse<RoomPriceManagementDTO[]>>('/room-prices/bulk-change', data)
}

export const getPriceChangeHistory = (params: PriceChangeHistoryQueryParams) => {
  return request.get<ApiResponse<PriceChangeHistoryPageResponse>>('/price-change-history', {
    params: { ...params },
  })
}
