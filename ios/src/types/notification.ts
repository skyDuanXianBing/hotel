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

export interface NotificationPageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export type NotificationReadFilter = 'all' | 'unread' | 'read'
