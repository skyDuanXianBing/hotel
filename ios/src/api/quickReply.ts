import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface QuickReplyDTO {
  id: number
  userId: number
  title: string
  message: string
  createdAt: string
  updatedAt: string
}

export interface QuickReplyRequest {
  title: string
  message: string
}

export const getAllQuickReplies = () => {
  return request<ApiResponse<QuickReplyDTO[]>>({
    url: '/quick-replies',
    method: 'GET',
  })
}

export const getQuickReplyById = (replyId: number) => {
  return request<ApiResponse<QuickReplyDTO>>({
    url: `/quick-replies/${replyId}`,
    method: 'GET',
  })
}

export const createQuickReply = (data: QuickReplyRequest) => {
  return request<ApiResponse<QuickReplyDTO>>({
    url: '/quick-replies',
    method: 'POST',
    data,
  })
}

export const updateQuickReply = (replyId: number, data: QuickReplyRequest) => {
  return request<ApiResponse<QuickReplyDTO>>({
    url: `/quick-replies/${replyId}`,
    method: 'PUT',
    data,
  })
}

export const deleteQuickReply = (replyId: number) => {
  return request<ApiResponse<void>>({
    url: `/quick-replies/${replyId}`,
    method: 'DELETE',
  })
}
