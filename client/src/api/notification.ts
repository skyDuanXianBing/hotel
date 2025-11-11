import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 通知设置DTO
export interface NotificationSettingDTO {
  id: number
  userId: number
  orderPopup: boolean
  orderSound: boolean
  chatPopup: boolean
  chatSound: boolean
  createdAt: string
  updatedAt: string
}

// 更新通知设置请求
export interface NotificationSettingRequest {
  orderPopup: boolean
  orderSound: boolean
  chatPopup: boolean
  chatSound: boolean
}

/**
 * 获取用户的通知设置
 */
export const getNotificationSettings = async (
  userId: number
): Promise<ApiResponse<NotificationSettingDTO>> => {
  return await request.get(`/notification-settings/user/${userId}`)
}

/**
 * 更新用户的通知设置
 */
export const updateNotificationSettings = async (
  userId: number,
  data: NotificationSettingRequest
): Promise<ApiResponse<NotificationSettingDTO>> => {
  return await request.put(`/notification-settings/user/${userId}`, data)
}

// ==================== 通知消息相关 API ====================

export interface NotificationMessageDTO {
  id: number
  userId: number
  notificationType: string
  title: string
  content: string
  isRead: boolean
  relatedId?: number
  createdAt: string
  readAt?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

/**
 * 分页获取用户的所有通知
 */
export const getNotificationMessages = async (
  userId: number,
  page: number = 0,
  size: number = 25
): Promise<ApiResponse<PageResponse<NotificationMessageDTO>>> => {
  return await request.get('/notifications', {
    params: { userId, page, size },
  })
}

/**
 * 分页获取用户指定类型的通知
 */
export const getNotificationMessagesByType = async (
  userId: number,
  type: string,
  page: number = 0,
  size: number = 25
): Promise<ApiResponse<PageResponse<NotificationMessageDTO>>> => {
  return await request.get(`/notifications/type/${type}`, {
    params: { userId, page, size },
  })
}

/**
 * 获取未读通知数量
 */
export const getUnreadNotificationCount = async (userId: number): Promise<ApiResponse<number>> => {
  return await request.get('/notifications/unread-count', {
    params: { userId },
  })
}

/**
 * 获取指定类型的未读通知数量
 */
export const getUnreadNotificationCountByType = async (
  userId: number,
  type: string
): Promise<ApiResponse<number>> => {
  return await request.get(`/notifications/unread-count/${type}`, {
    params: { userId },
  })
}

/**
 * 创建通知
 */
export const createNotificationMessage = async (
  userId: number,
  data: {
    notificationType: string
    title: string
    content: string
    relatedId?: number
  }
): Promise<ApiResponse<NotificationMessageDTO>> => {
  return await request.post('/notifications', data, {
    params: { userId },
  })
}

/**
 * 标记通知为已读
 */
export const markNotificationAsRead = async (id: number): Promise<ApiResponse<NotificationMessageDTO>> => {
  return await request.patch(`/notifications/${id}/read`)
}

/**
 * 标记所有通知为已读
 */
export const markAllNotificationsAsRead = async (userId: number): Promise<ApiResponse<number>> => {
  return await request.patch('/notifications/read-all', null, {
    params: { userId },
  })
}

/**
 * 标记指定类型的所有通知为已读
 */
export const markAllNotificationsAsReadByType = async (
  userId: number,
  type: string
): Promise<ApiResponse<number>> => {
  return await request.patch(`/notifications/read-all/${type}`, null, {
    params: { userId },
  })
}

/**
 * 删除通知
 */
export const deleteNotificationMessage = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/notifications/${id}`)
}
