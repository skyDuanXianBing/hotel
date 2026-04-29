import request from '@/utils/request'
import {
  getMockMessageThreads,
  getMockThreadMessages,
  MESSAGE_API_MOCK_ENABLED,
  pollMockThreadMessages,
  sendMockThreadMessage,
} from '@/mocks/message'
import type { ApiResponse } from '@/types/api'
import type {
  ChatMessageRequest,
  ChatMessageResponse,
  MessageDTO,
  MessageSendRequest,
  MessageThreadDTO,
} from '@/types/message'

export { MESSAGE_API_MOCK_ENABLED }

export const getMessageThreads = () => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return getMockMessageThreads()
  }

  return request<ApiResponse<MessageThreadDTO[]>>({
    url: '/su-messaging/threads',
    method: 'GET',
  })
}

export const getThreadMessages = (threadId: number) => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return getMockThreadMessages(threadId)
  }

  return request<ApiResponse<MessageDTO[]>>({
    url: `/su-messaging/threads/${threadId}/messages`,
    method: 'GET',
  })
}

export const pollThreadMessages = (threadId: number, since: string) => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return pollMockThreadMessages(threadId, since)
  }

  return request<ApiResponse<MessageDTO[]>>({
    url: `/su-messaging/threads/${threadId}/poll`,
    method: 'GET',
    params: { since },
    suppressErrorStatuses: [400, 403, 404],
  })
}

export const sendThreadMessage = (threadId: number, data: MessageSendRequest) => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return sendMockThreadMessage(threadId, data)
  }

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
