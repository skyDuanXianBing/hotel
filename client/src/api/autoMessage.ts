import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

/** 房间选择类型 */
export type RoomSelectionType = 'ALL_LOCAL' | 'BY_ROOM_TYPE' | 'BY_GROUP' | 'BY_ROOM'

/** 操作类型 */
export type AutoMessageAction = 'BOOKING_CONFIRM' | 'CHECK_IN' | 'CHECK_OUT'

/** 发送时机 */
export type SendTiming =
  | 'IMMEDIATELY'
  | '5_MIN'
  | '10_MIN'
  | '15_MIN'
  | '30_MIN'
  | '1_HOUR'
  | '2_HOUR'
  | '4_HOUR'
  | '8_HOUR'
  | '16_HOUR'
  | '24_HOUR'

// 自动化消息DTO
export interface AutoMessageDTO {
  id: number
  userId: number
  title: string
  message: string
  /** @deprecated 使用 action 替代 */
  automationRule: string
  /** @deprecated 使用 channels 替代 */
  channel: string
  /** 渠道列表（JSON数组格式） */
  channels: string
  /** 过时补发 */
  resendOnExpire: boolean
  /** @deprecated 使用 roomSelectionType 和 roomSelection 替代 */
  room: string
  /** 房间选择类型 */
  roomSelectionType: RoomSelectionType
  /** 房间选择值（JSON数组格式） */
  roomSelection: string
  /** 操作类型 */
  action: AutoMessageAction
  /** 发送时机 */
  sendTiming: SendTiming
  enabled: boolean
  createdAt: string
  updatedAt: string
}

// 创建/更新自动化消息请求
export interface AutoMessageRequest {
  title: string
  message: string
  /** @deprecated 保留兼容性 */
  automationRule?: string
  /** @deprecated 保留兼容性 */
  channel?: string
  /** 渠道列表（JSON数组格式） */
  channels: string
  /** 过时补发 */
  resendOnExpire: boolean
  /** @deprecated 保留兼容性 */
  room?: string
  /** 房间选择类型 */
  roomSelectionType: RoomSelectionType
  /** 房间选择值（JSON数组格式） */
  roomSelection: string
  /** 操作类型 */
  action: AutoMessageAction
  /** 发送时机 */
  sendTiming: SendTiming
  enabled: boolean
}

/**
 * 获取所有自动化消息
 */
export const getAllAutoMessages = async (): Promise<ApiResponse<AutoMessageDTO[]>> => {
  return await request.get('/auto-messages')
}

/**
 * 根据用户ID获取自动化消息列表
 * @deprecated 使用 getAllAutoMessages() 替代,现在使用门店级数据隔离
 */
export const getAutoMessagesByUserId = async (
  userId: number
): Promise<ApiResponse<AutoMessageDTO[]>> => {
  return await request.get(`/auto-messages/user/${userId}`)
}

/**
 * 根据ID获取自动化消息详情
 */
export const getAutoMessageById = async (
  id: number
): Promise<ApiResponse<AutoMessageDTO>> => {
  return await request.get(`/auto-messages/${id}`)
}

/**
 * 创建自动化消息
 */
export const createAutoMessage = async (
  data: AutoMessageRequest
): Promise<ApiResponse<AutoMessageDTO>> => {
  return await request.post('/auto-messages', data)
}

/**
 * 更新自动化消息
 */
export const updateAutoMessage = async (
  id: number,
  data: AutoMessageRequest
): Promise<ApiResponse<AutoMessageDTO>> => {
  return await request.put(`/auto-messages/${id}`, data)
}

/**
 * 删除自动化消息
 */
export const deleteAutoMessage = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/auto-messages/${id}`)
}

/**
 * 切换自动化消息启用状态
 */
export const toggleAutoMessage = async (
  id: number
): Promise<ApiResponse<AutoMessageDTO>> => {
  return await request.put(`/auto-messages/${id}/toggle`)
}
