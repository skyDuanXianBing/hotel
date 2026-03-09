export interface SuMessagingRealtimeEventMessage {
  id: number
  threadId: number
  senderType: 'GUEST' | 'STAFF'
  senderName?: string
  content: string
  deliveryStatus?: 'SENDING' | 'SENT' | 'FAILED'
  timestamp: string
}

export interface SuMessagingRealtimeEvent {
  eventType: 'MESSAGE_CREATED' | 'MESSAGE_UPDATED'
  threadId: number
  message: SuMessagingRealtimeEventMessage
}

const resolveWebSocketUrl = (token: string, storeId: number) => {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const url = new URL(apiBaseUrl, window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = `${url.pathname.replace(/\/$/, '')}/ws/su-messaging`
  url.searchParams.set('token', token)
  url.searchParams.set('storeId', String(storeId))
  return url.toString()
}

export const createSuMessagingSocket = (token: string, storeId: number) => {
  return new WebSocket(resolveWebSocketUrl(token, storeId))
}
