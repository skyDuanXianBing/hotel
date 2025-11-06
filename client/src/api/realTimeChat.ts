import { request } from '../utils/request'

// 发送者类型
export enum SenderType {
  GUEST = 'GUEST',
  STAFF = 'STAFF',
}

// 消息类型
export enum MessageType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  FILE = 'FILE',
  SYSTEM = 'SYSTEM',
}

// 聊天室状态
export enum ChatStatus {
  ACTIVE = 'ACTIVE',
  CLOSED = 'CLOSED',
  WAITING = 'WAITING',
}

// 实时聊天请求
export interface RealTimeChatRequest {
  roomId: string
  message: string
  senderType: SenderType
  senderName?: string
  guestRoomNumber?: string
  messageType?: MessageType
}

// 实时聊天响应
export interface RealTimeChatResponse {
  messageId: number
  roomId: string
  senderType: SenderType
  senderName: string
  messageContent: string
  messageType: MessageType
  sentAt: string
  isRead: boolean
}

// 聊天室信息
export interface ChatRoomInfo {
  roomId: string
  guestName: string
  guestRoomNumber: string
  status: ChatStatus
  lastActivity: string
  createdAt: string
}

// 统一响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

/**
 * 发送实时聊天消息
 */
export const sendRealTimeMessage = (
  data: RealTimeChatRequest,
): Promise<ApiResponse<RealTimeChatResponse>> => {
  return request({
    url: '/realtime-chat/send',
    method: 'POST',
    data,
  })
}

/**
 * 获取聊天室消息历史
 */
export const getRoomMessages = (roomId: string): Promise<ApiResponse<RealTimeChatResponse[]>> => {
  return request({
    url: `/realtime-chat/messages/${roomId}`,
    method: 'GET',
  })
}

/**
 * 轮询新消息
 */
export const pollNewMessages = (
  roomId: string,
  since?: string,
): Promise<ApiResponse<RealTimeChatResponse[]>> => {
  return request({
    url: `/realtime-chat/poll/${roomId}`,
    method: 'GET',
    params: since ? { since } : undefined,
  })
}

/**
 * 获取活跃聊天室列表
 */
export const getActiveChatRooms = (): Promise<ApiResponse<ChatRoomInfo[]>> => {
  return request({
    url: '/realtime-chat/rooms',
    method: 'GET',
  })
}

/**
 * 创建新聊天室
 */
export const createChatRoom = (
  guestName: string,
  guestRoomNumber?: string,
): Promise<ApiResponse<ChatRoomInfo>> => {
  return request({
    url: '/realtime-chat/room/create',
    method: 'POST',
    params: {
      guestName,
      guestRoomNumber,
    },
  })
}

/**
 * 关闭聊天室
 */
export const closeChatRoom = (roomId: string): Promise<ApiResponse<string>> => {
  return request({
    url: `/realtime-chat/room/${roomId}/close`,
    method: 'POST',
  })
}
