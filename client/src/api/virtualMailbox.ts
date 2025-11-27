import request from '@/utils/request'

/**
 * 虚拟邮箱状态枚举
 */
export enum MailboxStatus {
  ACTIVE = 'ACTIVE',
  CLOSED = 'CLOSED'
}

/**
 * 邮件发送者类型枚举
 */
export enum EmailSenderType {
  GUEST = 'GUEST',
  STAFF = 'STAFF',
  SYSTEM = 'SYSTEM'
}

/**
 * 虚拟邮箱DTO
 */
export interface VirtualMailboxDTO {
  id: number
  reservationId: number
  emailAddress: string
  displayName: string
  status: MailboxStatus
  guestName: string
  guestRoomNumber: string
  lastActivity: string
  unreadCount: number
}

/**
 * 邮件消息DTO
 */
export interface EmailMessageDTO {
  id: number
  mailboxId: number
  senderEmail: string
  senderName?: string
  senderType: EmailSenderType
  content: string
  timestamp: string
  isRead: boolean
}

/**
 * 发送消息请求
 */
export interface SendMessageRequest {
  content: string
  senderName?: string
}

/**
 * 获取订单的虚拟邮箱
 */
export const getReservationMailbox = (reservationId: number) => {
  return request<VirtualMailboxDTO>({
    url: `/virtual-mailbox/reservation/${reservationId}`,
    method: 'get'
  })
}

/**
 * 获取所有活跃的虚拟邮箱
 */
export const getActiveMailboxes = () => {
  return request<VirtualMailboxDTO[]>({
    url: '/virtual-mailbox/active',
    method: 'get'
  })
}

/**
 * 获取虚拟邮箱的所有消息
 */
export const getMailboxMessages = (mailboxId: number) => {
  return request<EmailMessageDTO[]>({
    url: `/virtual-mailbox/mailbox/${mailboxId}/messages`,
    method: 'get'
  })
}

/**
 * 轮询新消息
 */
export const pollMailboxMessages = (mailboxId: number, since: string) => {
  return request<EmailMessageDTO[]>({
    url: `/virtual-mailbox/mailbox/${mailboxId}/poll`,
    method: 'get',
    params: { since }
  })
}

/**
 * 发送消息
 */
export const sendMailboxMessage = (mailboxId: number, data: SendMessageRequest) => {
  return request<EmailMessageDTO>({
    url: `/virtual-mailbox/mailbox/${mailboxId}/send`,
    method: 'post',
    data
  })
}

/**
 * 关闭虚拟邮箱
 */
export const closeMailbox = (mailboxId: number) => {
  return request<void>({
    url: `/virtual-mailbox/mailbox/${mailboxId}/close`,
    method: 'post'
  })
}
