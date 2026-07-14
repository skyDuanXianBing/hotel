import request from '@/utils/request'

const SU_MESSAGING_TIMEOUT_MS = 60_000
const SU_MESSAGING_AI_DRAFT_TIMEOUT_MS = 60_000
const SU_MESSAGING_TRANSLATION_TIMEOUT_MS = 60_000
const SU_MESSAGING_TRANSLATION_SETTING_TIMEOUT_MS = 5_000

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
  attachments?: SuMessagingAttachmentDTO[]
}

export interface SuMessagingAttachmentDTO {
  id: number
  mimeType?: string
  fileName?: string
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

export type SuMessagingTranslationTargetLanguage = 'zh-CN' | 'en' | 'ja' | 'ko'

export interface SuMessagingTranslationSetting {
  enabled: boolean
  targetLanguage: SuMessagingTranslationTargetLanguage
  configured: boolean
}

export type SuMessagingTranslationSettingUpdate = Pick<
  SuMessagingTranslationSetting,
  'enabled' | 'targetLanguage'
>

export interface SuMessageTranslationRequest {
  targetLanguage: string
}

export interface SuMessageTranslationResponse {
  messageId: number
  targetLanguage: string
  translatedContent: string
  sourceContentHash: string
  status: string
  cached: boolean
  translatedAt?: string | null
}

export interface SuMessageTranslationOptions {
  timeoutMs?: number
  suppressErrorToast?: boolean
}

export type SuMessagingAiReplyDraftRetrievalStatus =
  | 'MATCHED'
  | 'NO_MATCH'
  | 'PARTIAL'
  | 'FAILED'

export interface SuMessagingAiReplyDraftRecentMessage {
  direction: 'GUEST' | 'STAFF'
  content: string
  sentAt?: string
}

export interface SuMessagingAiReplyDraftRequest {
  reservationId?: number
  bookingId?: string
  externalThreadId?: string
  channel?: SuMessagingChannelCode
  guestName?: string
  roomId?: number
  roomNumber?: string
  roomTypeId?: number
  roomTypeName?: string
  latestGuestMessageId?: number
  recentMessages?: SuMessagingAiReplyDraftRecentMessage[]
  language?: string
}

export interface SuMessagingAiReplyDraftResponse {
  draftReply?: string
  retrievalStatus?: SuMessagingAiReplyDraftRetrievalStatus
  warnings?: string[]
}

export interface SuMessagingAiReplyDraftOptions {
  timeoutMs?: number
  suppressErrorToast?: boolean
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

export const sendSuThreadAttachment = (threadId: number, file: File, senderName?: string) => {
  const data = new FormData()
  data.append('file', file)
  if (senderName?.trim()) {
    data.append('senderName', senderName.trim())
  }
  return request<SuMessagingMessageDTO>({
    url: `/su-messaging/threads/${threadId}/attachments`,
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const getSuMessageAttachment = (
  threadId: number,
  messageId: number,
  attachmentId: number,
) => {
  return request<Blob>({
    url: `/su-messaging/threads/${threadId}/messages/${messageId}/attachments/${attachmentId}`,
    method: 'get',
    responseType: 'blob',
    timeout: SU_MESSAGING_TIMEOUT_MS,
    suppressErrorToast: true,
  })
}

export const translateSuThreadMessage = (
  threadId: number,
  messageId: number,
  data: SuMessageTranslationRequest,
  options: SuMessageTranslationOptions = {},
) => {
  return request<SuMessageTranslationResponse>({
    url: `/su-messaging/threads/${threadId}/messages/${messageId}/translation`,
    method: 'post',
    data,
    timeout: options.timeoutMs || SU_MESSAGING_TRANSLATION_TIMEOUT_MS,
    suppressErrorToast: options.suppressErrorToast,
  })
}

export const generateSuMessagingAiReplyDraft = (
  threadId: number,
  data: SuMessagingAiReplyDraftRequest,
  options: SuMessagingAiReplyDraftOptions = {},
) => {
  return request<SuMessagingAiReplyDraftResponse>({
    url: `/su-messaging/threads/${threadId}/ai-reply-draft`,
    method: 'post',
    data,
    timeout: options.timeoutMs || SU_MESSAGING_AI_DRAFT_TIMEOUT_MS,
    suppressErrorToast: options.suppressErrorToast,
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

export const getSuMessagingTranslationSetting = () => {
  return request<SuMessagingTranslationSetting>({
    url: '/su-messaging/translation-settings',
    method: 'get',
    timeout: SU_MESSAGING_TRANSLATION_SETTING_TIMEOUT_MS,
    suppressErrorToast: true,
  })
}

export const updateSuMessagingTranslationSetting = (data: SuMessagingTranslationSettingUpdate) => {
  return request<SuMessagingTranslationSetting>({
    url: '/su-messaging/translation-settings',
    method: 'put',
    data,
    suppressErrorToast: true,
  })
}
