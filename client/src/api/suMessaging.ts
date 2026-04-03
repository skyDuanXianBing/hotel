import request from '@/utils/request'

export enum SuMessagingSenderType {
  GUEST = 'GUEST',
  STAFF = 'STAFF',
}

export interface SuMessagingThreadDTO {
  id: number
  channelId: number
  channelName: string
  guestName?: string
  bookingId?: string
  threadId?: string
  listingId?: string
  listingName?: string
  checkInDate?: string
  checkOutDate?: string
  roomTypeName?: string
  lastMessage?: string
  lastActivity: string
  unreadCount: number
  closed: boolean
}

export interface SuMessagingMessageDTO {
  id: number
  threadId: number
  senderType: SuMessagingSenderType
  senderName?: string
  content: string
  deliveryStatus?: 'SENDING' | 'SENT' | 'FAILED'
  timestamp: string
}

export interface SuMessagingSendRequest {
  content: string
  senderName?: string
}

export interface SuMessagingAiSetting {
  autoReplyEnabled: boolean
}

export const getSuThreads = () => {
  return request<SuMessagingThreadDTO[]>({
    url: '/su-messaging/threads',
    method: 'get',
  })
}

export const getSuThreadMessages = (threadId: number) => {
  return request<SuMessagingMessageDTO[]>({
    url: `/su-messaging/threads/${threadId}/messages`,
    method: 'get',
  })
}

export const pollSuThreadMessages = (threadId: number, since: string) => {
  return request<SuMessagingMessageDTO[]>({
    url: `/su-messaging/threads/${threadId}/poll`,
    method: 'get',
    params: { since },
  })
}

export const sendSuThreadMessage = (threadId: number, data: SuMessagingSendRequest) => {
  return request<SuMessagingMessageDTO>({
    url: `/su-messaging/threads/${threadId}/send`,
    method: 'post',
    data,
  })
}

export const getSuMessagingAiSetting = () => {
  return request<SuMessagingAiSetting>({
    url: '/su-messaging/ai-settings',
    method: 'get',
  })
}

export const updateSuMessagingAiSetting = (data: SuMessagingAiSetting) => {
  return request<SuMessagingAiSetting>({
    url: '/su-messaging/ai-settings',
    method: 'put',
    data,
  })
}
