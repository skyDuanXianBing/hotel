import request from '@/utils/request'

/**
 * 价格计划接口
 */
export interface PricePlanDTO {
  id?: number
  name: string
  nameEn?: string
  description?: string
  descriptionEn?: string
  minNights: number
  maxNights?: number
  includeMeal: boolean
  derivationType?: string // 'independent' 或 'derived'
  basePlanId?: number
  derivationRule?: string
  cancellationPolicy?: string
  cancellationPolicyEn?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 房型价格计划关联接口
 */
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
  priceMode?: string // 'unified' 或 'multiple'
  createdAt?: string
  updatedAt?: string
}

/**
 * 创建价格计划请求
 */
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

/**
 * 更新价格计划请求
 */
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

/**
 * 分配房型价格计划请求
 */
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
}

/**
 * 获取所有价格计划
 */
export const getAllPricePlans = (userId: number) => {
  return request<PricePlanDTO[]>({
    url: '/price-plans',
    method: 'GET',
    params: { userId },
  })
}

/**
 * 根据ID获取价格计划
 */
export const getPricePlanById = (id: number, userId: number) => {
  return request<PricePlanDTO>({
    url: `/price-plans/${id}`,
    method: 'GET',
    params: { userId },
  })
}

/**
 * 创建价格计划
 */
export const createPricePlan = (userId: number, data: CreatePricePlanRequest) => {
  return request<PricePlanDTO>({
    url: '/price-plans',
    method: 'POST',
    params: { userId },
    data,
  })
}

/**
 * 更新价格计划
 */
export const updatePricePlan = (id: number, userId: number, data: UpdatePricePlanRequest) => {
  return request<PricePlanDTO>({
    url: `/price-plans/${id}`,
    method: 'PUT',
    params: { userId },
    data,
  })
}

/**
 * 删除价格计划
 */
export const deletePricePlan = (id: number, userId: number) => {
  return request<void>({
    url: `/price-plans/${id}`,
    method: 'DELETE',
    params: { userId },
  })
}

/**
 * 彻底删除价格计划（最小改动：仅清理渠道价格记录 channel_prices 后再尝试删除）
 */
export const forceDeletePricePlan = (id: number, userId: number) => {
  return request<void>({
    url: `/price-plans/${id}/force-delete`,
    method: 'POST',
    params: { userId },
    data: { confirm: true },
  })
}

/**
 * 获取价格计划关联的所有房型
 */
export const getRoomTypesByPricePlan = (pricePlanId: number) => {
  return request<RoomTypePricePlanDTO[]>({
    url: `/price-plans/${pricePlanId}/room-types`,
    method: 'GET',
  })
}

/**
 * 获取房型的所有价格计划
 */
export const getPricePlansByRoomType = (roomTypeId: number) => {
  return request<RoomTypePricePlanDTO[]>({
    url: `/price-plans/room-types/${roomTypeId}`,
    method: 'GET',
  })
}

/**
 * 为房型分配价格计划
 */
export const assignPricePlanToRoomType = (
  roomTypeId: number,
  pricePlanId: number,
  userId: number,
  data: AssignRoomTypePricePlanRequest
) => {
  return request<RoomTypePricePlanDTO>({
    url: `/price-plans/room-types/${roomTypeId}/assign/${pricePlanId}`,
    method: 'POST',
    params: { userId },
    data,
  })
}

/**
 * 更新房型价格计划
 */
export const updateRoomTypePricePlan = (
  id: number,
  userId: number,
  data: AssignRoomTypePricePlanRequest
) => {
  return request<RoomTypePricePlanDTO>({
    url: `/price-plans/room-type-price-plans/${id}`,
    method: 'PUT',
    params: { userId },
    data,
  })
}

/**
 * 删除房型价格计划关联
 */
export const deleteRoomTypePricePlan = (id: number, userId: number) => {
  return request<void>({
    url: `/price-plans/room-type-price-plans/${id}`,
    method: 'DELETE',
    params: { userId },
  })
}

/**
 * 统计价格计划关联的房型数量
 */
export const countRoomTypesByPricePlan = (pricePlanId: number) => {
  return request<number>({
    url: `/price-plans/${pricePlanId}/room-types/count`,
    method: 'GET',
  })
}
