export enum MessageSenderType {
  GUEST = 'GUEST',
  STAFF = 'STAFF',
}

export interface MessageThreadDTO {
  id: number
  channelId: number
  channelName: string
  guestName?: string
  bookingId?: string
  threadId?: string
  listingId?: string
  listingName?: string
  roomTypeName?: string
  checkInDate?: string
  checkOutDate?: string
  lastMessage?: string
  lastActivity: string
  unreadCount: number
  closed: boolean
}

export interface MessageThreadPageRequest {
  page?: number
  size?: number
  channel?: string
  unread?: boolean
  closed?: boolean
  search?: string
}

export interface MessageThreadPageDTO {
  items: MessageThreadDTO[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

export interface MessageUnreadSummaryDTO {
  totalUnread: number
  unreadThreadCount: number
}

export interface MessageDTO {
  id: number
  threadId: number
  senderType: MessageSenderType
  senderName?: string
  content: string
  deliveryStatus?: 'SENDING' | 'SENT' | 'FAILED'
  timestamp: string
}

export interface MessagePageRequest {
  limit?: number
  beforeMessageId?: number
  afterMessageId?: number
  markRead?: boolean
}

export interface MessagePageDTO {
  items: MessageDTO[]
  limit: number
  hasMoreBefore: boolean
  nextBeforeMessageId?: number
  hasMoreAfter?: boolean
}

export interface MessageSendRequest {
  content: string
  senderName?: string
}

export interface MessageTranslationRequest {
  targetLanguage: string
}

export interface MessageTranslationResponse {
  messageId: number
  targetLanguage: string
  translatedContent: string
  sourceContentHash: string
  status: string
  cached: boolean
  translatedAt: string
}

export interface MessageTranslationSetting {
  enabled: boolean
  targetLanguage: 'zh-CN' | 'en' | 'ja' | 'ko'
  configured?: boolean
}

export interface MessageAiReplyDraftRecentMessage {
  direction: 'GUEST' | 'STAFF'
  content: string
  sentAt?: string
}

export interface MessageAiReplyDraftRequest {
  reservationId?: number
  bookingId?: string
  externalThreadId?: string
  channel?: string
  guestName?: string
  roomId?: number
  roomNumber?: string
  roomTypeId?: number
  roomTypeName?: string
  latestGuestMessageId?: number
  recentMessages?: MessageAiReplyDraftRecentMessage[]
  language?: string
}

export interface MessageAiReplyDraftResponse {
  draftReply?: string
  retrievalStatus?: 'MATCHED' | 'NO_MATCH' | 'PARTIAL' | 'FAILED'
  warnings?: string[]
  matchedKnowledgeCount?: number
  processingTimeMs?: number
}

export interface ChatMessageRequest {
  message: string
  sessionId?: string
  userId?: string
  taskType?: 'DEFAULT' | 'TRANSLATION'
}

export interface ChatMessageResponse {
  reply: string
  timestamp: string
  sessionId: string
  status: string
  errorMessage?: string
  processingTime?: number
}
