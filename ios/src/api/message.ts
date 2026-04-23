import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type {
  ChatMessageRequest,
  ChatMessageResponse,
  MessageDTO,
  MessageSendRequest,
  MessageThreadDTO,
} from '@/types/message'

export const getMessageThreads = () => {
  return request<ApiResponse<MessageThreadDTO[]>>({
    url: '/su-messaging/threads',
    method: 'GET',
  })
}

export const getThreadMessages = (threadId: number) => {
  return request<ApiResponse<MessageDTO[]>>({
    url: `/su-messaging/threads/${threadId}/messages`,
    method: 'GET',
  })
}

export const pollThreadMessages = (threadId: number, since: string) => {
  return request<ApiResponse<MessageDTO[]>>({
    url: `/su-messaging/threads/${threadId}/poll`,
    method: 'GET',
    params: { since },
    suppressErrorStatuses: [400, 403, 404],
  })
}

export const sendThreadMessage = (threadId: number, data: MessageSendRequest) => {
  return request<ApiResponse<MessageDTO>>({
    url: `/su-messaging/threads/${threadId}/send`,
    method: 'POST',
    data,
  })
}

export const sendAiChatMessage = (data: ChatMessageRequest) => {
  return request<ApiResponse<ChatMessageResponse>>({
    url: '/chat/message',
    method: 'POST',
    data,
  })
}
