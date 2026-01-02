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
  timestamp: string
}

export interface SuMessagingSendRequest {
  content: string
  senderName?: string
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

