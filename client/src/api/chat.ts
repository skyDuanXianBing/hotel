import { request } from '../utils/request'

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

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

/**
 * 发送聊天消息并获取AI回复
 */
export const sendChatMessage = (
  data: ChatMessageRequest,
): Promise<ApiResponse<ChatMessageResponse>> => {
  return request({
    url: '/chat/message',
    method: 'POST',
    data,
  })
}

/**
 * 获取欢迎消息
 */
export const getWelcomeMessage = (
  sessionId?: string,
): Promise<ApiResponse<ChatMessageResponse>> => {
  return request({
    url: '/chat/welcome',
    method: 'GET',
    params: sessionId ? { sessionId } : undefined,
  })
}

/**
 * 检查聊天服务健康状态
 */
export const checkChatHealth = (): Promise<ApiResponse<Record<string, unknown>>> => {
  return request({
    url: '/chat/health',
    method: 'GET',
  })
}

/**
 * 获取聊天服务信息
 */
export const getChatServiceInfo = (): Promise<ApiResponse<Record<string, string>>> => {
  return request({
    url: '/chat/info',
    method: 'GET',
  })
}
