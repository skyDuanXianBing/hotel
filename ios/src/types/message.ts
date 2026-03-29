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
  lastMessage?: string
  lastActivity: string
  unreadCount: number
  closed: boolean
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

export interface MessageSendRequest {
  content: string
  senderName?: string
}

export interface ChatMessageRequest {
  message: string
  sessionId?: string
  userId?: string
}

export interface ChatMessageResponse {
  reply: string
  timestamp: string
  sessionId: string
  status: string
  errorMessage?: string
  processingTime?: number
}
