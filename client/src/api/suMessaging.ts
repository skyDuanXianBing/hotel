import request from '@/utils/request'

const SU_MESSAGING_TIMEOUT_MS = 30_000

export enum SuMessagingSenderType {
  GUEST = 'GUEST',
  STAFF = 'STAFF',
}

export type SuMessagingChannelCode = 'AIRBNB' | 'BOOKING'

export type SuMessagingOrderKind =
  | 'INQUIRY'
  | 'REQUESTED'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'COMPLETED'
  | 'UNMATCHED_ORDER'
  | 'UNKNOWN'

export type SuMessagingReservationStatus =
  | 'REQUESTED'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'CHECKED_OUT'
  | 'CANCELLED'
  | 'NO_SHOW'

export type SuMessagingMessageOrderStatus =
  | 'INQUIRY'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'CHECKED_OUT'
  | 'CANCELLED'

export interface SuMessagingThreadDTO {
  id: number
  channelId: number
  channelCode?: SuMessagingChannelCode
  channelName: string
  reservationId?: number | null
  orderKind?: SuMessagingOrderKind
  classificationConfidence?: string
  reservationStatus?: SuMessagingReservationStatus
  bookingFlag?: string
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

export interface SuMessagingThreadPageRequest {
  page?: number
  size?: number
  channel?: SuMessagingChannelCode
  orderKind?: SuMessagingOrderKind
  reservationStatus?: SuMessagingReservationStatus
  orderStatuses?: string
  unread?: boolean
  closed?: boolean
  search?: string
  sort?: string
}

export interface SuMessagingThreadPageDTO {
  items: SuMessagingThreadDTO[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
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

export interface SuMessagingMessagePageRequest {
  limit?: number
  beforeMessageId?: number
  afterMessageId?: number
  markRead?: boolean
}

export interface SuMessagingMessagePageDTO {
  items: SuMessagingMessageDTO[]
  limit: number
  hasMoreBefore: boolean
  nextBeforeMessageId?: number
  hasMoreAfter?: boolean
}

export interface SuMessagingSendRequest {
  content: string
  senderName?: string
}

export interface SuMessagingUnreadSummary {
  totalUnread: number
  unreadThreadCount: number
  byChannel?: Partial<Record<SuMessagingChannelCode, number>>
  byOrderKind?: Partial<Record<SuMessagingOrderKind, number>>
}

export interface SuMessagingAiSetting {
  autoReplyEnabled: boolean
}

const buildParams = (source: Record<string, unknown>) => {
  const params: Record<string, unknown> = {}
  for (const key of Object.keys(source)) {
    const value = source[key]
    if (value === undefined || value === null || value === '') {
      continue
    }
    params[key] = value
  }
  return params
}

export const getSuThreads = () => {
  return request<SuMessagingThreadDTO[]>({
    url: '/su-messaging/threads',
    method: 'get',
    timeout: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const getSuThreadsPage = (params: SuMessagingThreadPageRequest) => {
  return request<SuMessagingThreadPageDTO>({
    url: '/su-messaging/threads/page',
    method: 'get',
    params: buildParams(params as Record<string, unknown>),
    timeout: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const getSuThreadMessages = (threadId: number) => {
  return request<SuMessagingMessageDTO[]>({
    url: `/su-messaging/threads/${threadId}/messages`,
    method: 'get',
    timeout: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const getSuThreadMessagesPage = (
  threadId: number,
  params: SuMessagingMessagePageRequest,
) => {
  return request<SuMessagingMessagePageDTO>({
    url: `/su-messaging/threads/${threadId}/messages/page`,
    method: 'get',
    params: buildParams(params as Record<string, unknown>),
    timeout: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const pollSuThreadMessages = (threadId: number, since: string) => {
  return request<SuMessagingMessageDTO[]>({
    url: `/su-messaging/threads/${threadId}/poll`,
    method: 'get',
    params: { since },
    timeout: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const getSuUnreadSummary = () => {
  return request<SuMessagingUnreadSummary>({
    url: '/su-messaging/unread-summary',
    method: 'get',
    timeout: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const sendSuThreadMessage = (threadId: number, data: SuMessagingSendRequest) => {
  return request<SuMessagingMessageDTO>({
    url: `/su-messaging/threads/${threadId}/send`,
    method: 'post',
    data,
    timeout: SU_MESSAGING_TIMEOUT_MS,
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
