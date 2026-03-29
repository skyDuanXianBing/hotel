import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { NotificationMessageDTO, NotificationPageResponse } from '@/types/notification'
import type { NotificationSettingDTO, NotificationSettingRequest } from '@/types/settings'

export const getNotificationSettings = (userId: number) => {
  return request<ApiResponse<NotificationSettingDTO>>({
    url: `/notification-settings/user/${userId}`,
    method: 'GET',
  })
}

export const updateNotificationSettings = (userId: number, data: NotificationSettingRequest) => {
  return request<ApiResponse<NotificationSettingDTO>>({
    url: `/notification-settings/user/${userId}`,
    method: 'PUT',
    data,
  })
}

export const getNotificationMessages = (userId: number, page = 0, size = 20) => {
  return request<ApiResponse<NotificationPageResponse<NotificationMessageDTO>>>({
    url: '/notifications',
    method: 'GET',
    params: { userId, page, size },
  })
}

export const getNotificationMessagesByType = (
  userId: number,
  type: string,
  page = 0,
  size = 20,
  isRead?: boolean,
  keyword?: string,
) => {
  const params: Record<string, string | number | boolean> = { userId, page, size }

  if (typeof isRead === 'boolean') {
    params.isRead = isRead
  }

  if (keyword && keyword.trim()) {
    params.keyword = keyword.trim()
  }

  return request<ApiResponse<NotificationPageResponse<NotificationMessageDTO>>>({
    url: `/notifications/type/${type}`,
    method: 'GET',
    params,
  })
}

export const getUnreadNotificationCount = (userId: number) => {
  return request<ApiResponse<number>>({
    url: '/notifications/unread-count',
    method: 'GET',
    params: { userId },
  })
}

export const getUnreadNotificationCountByType = (userId: number, type: string) => {
  return request<ApiResponse<number>>({
    url: `/notifications/unread-count/${type}`,
    method: 'GET',
    params: { userId },
  })
}

export const markNotificationAsRead = (id: number) => {
  return request<ApiResponse<NotificationMessageDTO>>({
    url: `/notifications/${id}/read`,
    method: 'PATCH',
  })
}

export const markAllNotificationsAsRead = (userId: number) => {
  return request<ApiResponse<number>>({
    url: '/notifications/read-all',
    method: 'PATCH',
    params: { userId },
  })
}

export const markAllNotificationsAsReadByType = (userId: number, type: string) => {
  return request<ApiResponse<number>>({
    url: `/notifications/read-all/${type}`,
    method: 'PATCH',
    params: { userId },
  })
}

export const deleteNotificationMessage = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/notifications/${id}`,
    method: 'DELETE',
  })
}
