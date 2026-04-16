import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface PricePlanDTO {
  id?: number
  name: string
  nameEn?: string
  description?: string
  descriptionEn?: string
  minNights: number
  maxNights?: number
  includeMeal: boolean
  derivationType?: string
  basePlanId?: number
  derivationRule?: string
  cancellationPolicy?: string
  cancellationPolicyEn?: string
  createdAt?: string
  updatedAt?: string
}

export interface RoomTypePricePlanDTO {
  id?: number
  roomTypeId?: number
  pricePlanId?: number
  roomType?: {
    id: number
    name: string
    nameEn?: string
  }
  pricePlan?: {
    id: number
    name: string
    nameEn?: string
  }
  mondayPrice?: number
  tuesdayPrice?: number
  wednesdayPrice?: number
  thursdayPrice?: number
  fridayPrice?: number
  saturdayPrice?: number
  sundayPrice?: number
  maxGuests: number
  includedGuests?: number
  extraAdultRate?: number
  extraChildRate?: number
  priceMode?: string
  createdAt?: string
  updatedAt?: string
}

export interface CreatePricePlanRequest {
  name: string
  nameEn?: string
  description?: string
  descriptionEn?: string
  minNights: number
  maxNights?: number
  includeMeal: boolean
  derivationType?: string
  basePlanId?: number
  derivationRule?: string
  cancellationPolicy?: string
  cancellationPolicyEn?: string
}

export interface UpdatePricePlanRequest {
  name: string
  nameEn?: string
  description?: string
  descriptionEn?: string
  minNights: number
  maxNights?: number
  includeMeal: boolean
  derivationType?: string
  basePlanId?: number
  derivationRule?: string
  cancellationPolicy?: string
  cancellationPolicyEn?: string
}

export interface AssignRoomTypePricePlanRequest {
  mondayPrice?: number
  tuesdayPrice?: number
  wednesdayPrice?: number
  thursdayPrice?: number
  fridayPrice?: number
  saturdayPrice?: number
  sundayPrice?: number
  maxGuests: number
  includedGuests?: number
  extraAdultRate?: number
  extraChildRate?: number
  priceMode?: string
  clearFutureOverrides?: boolean
  clearFromDate?: string
}

export const getAllPricePlans = (userId: number) => {
  return request<ApiResponse<PricePlanDTO[]>>({
    url: '/price-plans',
    method: 'GET',
    params: { userId },
  })
}

export const getPricePlanById = (pricePlanId: number, userId: number) => {
  return request<ApiResponse<PricePlanDTO>>({
    url: `/price-plans/${pricePlanId}`,
    method: 'GET',
    params: { userId },
  })
}

export const createPricePlan = (userId: number, data: CreatePricePlanRequest) => {
  return request<ApiResponse<PricePlanDTO>>({
    url: '/price-plans',
    method: 'POST',
    params: { userId },
    data,
  })
}

export const updatePricePlan = (pricePlanId: number, userId: number, data: UpdatePricePlanRequest) => {
  return request<ApiResponse<PricePlanDTO>>({
    url: `/price-plans/${pricePlanId}`,
    method: 'PUT',
    params: { userId },
    data,
  })
}

export const deletePricePlan = (pricePlanId: number, userId: number) => {
  return request<ApiResponse<void>>({
    url: `/price-plans/${pricePlanId}`,
    method: 'DELETE',
    params: { userId },
  })
}

export const forceDeletePricePlan = (pricePlanId: number, userId: number) => {
  return request<ApiResponse<void>>({
    url: `/price-plans/${pricePlanId}/force-delete`,
    method: 'POST',
    params: { userId },
    data: { confirm: true },
  })
}

export const getRoomTypesByPricePlan = (pricePlanId: number) => {
  return request<ApiResponse<RoomTypePricePlanDTO[]>>({
    url: `/price-plans/${pricePlanId}/room-types`,
    method: 'GET',
  })
}

export const getPricePlansByRoomType = (roomTypeId: number) => {
  return request<ApiResponse<RoomTypePricePlanDTO[]>>({
    url: `/price-plans/room-types/${roomTypeId}`,
    method: 'GET',
  })
}

export const assignPricePlanToRoomType = (
  roomTypeId: number,
  pricePlanId: number,
  userId: number,
  data: AssignRoomTypePricePlanRequest,
) => {
  return request<ApiResponse<RoomTypePricePlanDTO>>({
    url: `/price-plans/room-types/${roomTypeId}/assign/${pricePlanId}`,
    method: 'POST',
    params: { userId },
    data,
  })
}

export const updateRoomTypePricePlan = (
  relationId: number,
  userId: number,
  data: AssignRoomTypePricePlanRequest,
) => {
  return request<ApiResponse<RoomTypePricePlanDTO>>({
    url: `/price-plans/room-type-price-plans/${relationId}`,
    method: 'PUT',
    params: { userId },
    data,
  })
}

export const deleteRoomTypePricePlan = (relationId: number, userId: number, clearOverrides = false) => {
  return request<ApiResponse<void>>({
    url: `/price-plans/room-type-price-plans/${relationId}`,
    method: 'DELETE',
    params: { userId, clearOverrides },
  })
}

export const countRoomTypesByPricePlan = (pricePlanId: number) => {
  return request<ApiResponse<number>>({
    url: `/price-plans/${pricePlanId}/room-types/count`,
    method: 'GET',
  })
}
