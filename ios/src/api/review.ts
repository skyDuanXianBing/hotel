import type {
  ReviewAttachment,
  ReviewFilterStatus,
  ReviewGuest,
  ReviewHistoryItem,
  ReviewLinkEntry,
  ReviewRecord,
  ReviewStatus,
} from '@/constants/reviews'
import type { ApiResponse } from '@/types/api'
import request from '@/utils/request'

type RegistrationBackendStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'

interface RegistrationListItemDTO {
  formId: number
  orderNumber?: string | null
  channelOrderNumber?: string | null
  channelName?: string | null
  guestName?: string | null
  checkInDate?: string | null
  checkOutDate?: string | null
  status?: RegistrationBackendStatus | null
  submittedAt?: string | null
  updatedAt?: string | null
}

interface RegistrationGuestDTO {
  id?: number | null
  sortOrder?: number | null
  lastName?: string | null
  firstName?: string | null
  nationality?: string | null
  residenceType?: string | null
  phone?: string | null
  passportNumber?: string | null
  priorStay?: string | null
  nextDestination?: string | null
}

interface RegistrationAttachmentDTO {
  id?: number | null
  guestId?: number | null
  type?: string | null
  originalName?: string | null
}

interface RegistrationReviewLogDTO {
  id?: number | null
  action?: string | null
  operatorUserId?: number | null
  operatorName?: string | null
  note?: string | null
  createdAt?: string | null
}

interface RegistrationDetailDTO {
  formId: number
  orderNumber?: string | null
  channelOrderNumber?: string | null
  status?: RegistrationBackendStatus | null
  guestName?: string | null
  channelName?: string | null
  checkInDate?: string | null
  checkOutDate?: string | null
  roomTypeName?: string | null
  roomNumber?: string | null
  submittedAt?: string | null
  approvedAt?: string | null
  rejectedAt?: string | null
  updatedAt?: string | null
  reviewNote?: string | null
  guests?: RegistrationGuestDTO[] | null
  attachments?: RegistrationAttachmentDTO[] | null
  reviewLogs?: RegistrationReviewLogDTO[] | null
}

interface RegistrationLinkInboxItemDTO {
  id?: number | null
  bookingKey?: string | null
  linkUrl?: string | null
  guestName?: string | null
  checkInDate?: string | null
  checkOutDate?: string | null
  roomCount?: number | null
  createdAt?: string | null
}

interface ReviewDecisionRequest {
  note: string
}

export interface RegistrationReviewListParams {
  status?: Exclude<ReviewFilterStatus, 'all'>
  channelId?: number
  checkInDate?: string
  checkOutDate?: string
}

const unwrapApiResponse = <T>(response: ApiResponse<T>, fallbackMessage: string) => {
  if (!response.success) {
    throw new Error(response.message || fallbackMessage)
  }

  return response.data
}

const formatDate = (value?: string | null) => {
  if (!value) {
    return '—'
  }

  if (value.includes('T')) {
    return value.slice(0, 10)
  }

  return value
}

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '—'
  }

  const normalizedValue = value.replace('T', ' ')

  if (normalizedValue.length >= 16) {
    return normalizedValue.slice(0, 16)
  }

  return normalizedValue
}

const mapBackendStatus = (status?: RegistrationBackendStatus | null): ReviewStatus => {
  if (status === 'APPROVED') {
    return 'approved'
  }

  if (status === 'REJECTED') {
    return 'rejected'
  }

  if (status === 'DRAFT') {
    return 'draft'
  }

  return 'pending'
}

const mapFilterStatusToBackend = (status?: Exclude<ReviewFilterStatus, 'all'>) => {
  if (status === 'draft') {
    return 'DRAFT'
  }

  if (status === 'approved') {
    return 'APPROVED'
  }

  if (status === 'rejected') {
    return 'REJECTED'
  }

  if (status === 'pending') {
    return 'SUBMITTED'
  }

  return undefined
}

const buildRoomLabel = (roomTypeName?: string | null, roomNumber?: string | null) => {
  const segments: string[] = []

  if (roomTypeName) {
    segments.push(roomTypeName)
  }

  if (roomNumber) {
    segments.push(roomNumber)
  }

  if (segments.length === 0) {
    return '房间待分配'
  }

  return segments.join(' ')
}

const buildGuestName = (guest: RegistrationGuestDTO) => {
  const segments: string[] = []

  if (guest.lastName) {
    segments.push(guest.lastName)
  }

  if (guest.firstName) {
    segments.push(guest.firstName)
  }

  if (segments.length === 0) {
    return '未命名入住人'
  }

  return segments.join(' ')
}

const buildGuestRelation = (guest: RegistrationGuestDTO, index: number) => {
  const sortOrder = guest.sortOrder ?? index + 1

  if (sortOrder <= 1) {
    return '主住客'
  }

  return `同行人 ${sortOrder}`
}

const buildHistoryAction = (action?: string | null) => {
  if (action === 'APPROVE') {
    return '审核通过'
  }

  if (action === 'REJECT') {
    return '驳回'
  }

  if (action === 'SUBMIT') {
    return '提交审查'
  }

  return '审查更新'
}

const buildHistoryOperator = (log: RegistrationReviewLogDTO) => {
  if (log.operatorName && log.operatorName.trim()) {
    return log.operatorName.trim()
  }

  if (log.operatorUserId) {
    return `用户#${log.operatorUserId}`
  }

  return '系统'
}

const mapGuests = (guests?: RegistrationGuestDTO[] | null): ReviewGuest[] => {
  if (!guests || guests.length === 0) {
    return []
  }

  return guests.map((guest, index) => {
    const sortOrder = guest.sortOrder ?? index + 1
    const idNumber = guest.passportNumber || '未提供'

    return {
      id: String(guest.id ?? `${index + 1}`),
      sortOrder,
      name: buildGuestName(guest),
      idType: 'Passport',
      idNumber,
      phone: guest.phone || '未提供',
      relation: buildGuestRelation(guest, index),
      nationality: guest.nationality || '未提供',
      residenceType: guest.residenceType || '未提供',
      passportNumber: guest.passportNumber || '未提供',
      priorStay: guest.priorStay || '未提供',
      nextDestination: guest.nextDestination || '未提供',
    }
  })
}

const mapAttachments = (attachments?: RegistrationAttachmentDTO[] | null): ReviewAttachment[] => {
  if (!attachments || attachments.length === 0) {
    return []
  }

  return attachments.map((attachment, index) => {
    return {
      id: String(attachment.id ?? `${index + 1}`),
      attachmentNumericId: attachment.id ?? 0,
      guestId: String(attachment.guestId ?? ''),
      name: attachment.originalName || `附件 ${index + 1}`,
      originalName: attachment.originalName || `attachment-${attachment.id ?? index + 1}`,
      sizeLabel: '大小未知',
      typeLabel: attachment.type || '附件',
      type: attachment.type || '附件',
    }
  })
}

const mapLinkInboxItems = (items?: RegistrationLinkInboxItemDTO[] | null): ReviewLinkEntry[] => {
  if (!items || items.length === 0) {
    return []
  }

  return items.map((item, index) => {
    return {
      id: String(item.id ?? `${index + 1}`),
      bookingKey: item.bookingKey || '—',
      linkUrl: item.linkUrl || '',
      guestName: item.guestName || '未命名住客',
      checkInDate: formatDate(item.checkInDate),
      checkOutDate: formatDate(item.checkOutDate),
      roomCount: item.roomCount ?? 0,
      createdAt: formatDateTime(item.createdAt),
    }
  })
}

const mapHistory = (logs?: RegistrationReviewLogDTO[] | null): ReviewHistoryItem[] => {
  if (!logs || logs.length === 0) {
    return []
  }

  return logs.map((log, index) => {
    return {
      id: String(log.id ?? `${index + 1}`),
      timestamp: formatDateTime(log.createdAt),
      operator: buildHistoryOperator(log),
      action: buildHistoryAction(log.action),
      note: log.note?.trim() || '未填写备注',
    }
  })
}

export const mapRegistrationListItem = (item: RegistrationListItemDTO): ReviewRecord => {
  const formId = String(item.formId)

  return {
    formId,
    formNumericId: item.formId,
    orderNumber: item.orderNumber || '',
    channelOrderNumber: item.channelOrderNumber || '',
    guestName: item.guestName || '未命名住客',
    roomLabel: '房间待分配',
    roomTypeName: '',
    roomNumber: '',
    channelName: item.channelName || '渠道待确认',
    checkInDate: formatDate(item.checkInDate),
    checkOutDate: formatDate(item.checkOutDate),
    submittedAt: formatDateTime(item.submittedAt),
    approvedAt: '—',
    rejectedAt: '—',
    updatedAt: formatDateTime(item.updatedAt),
    status: mapBackendStatus(item.status),
    guests: [],
    attachments: [],
    reviewNote: '',
    history: [],
  }
}

export const mapRegistrationDetail = (detail: RegistrationDetailDTO): ReviewRecord => {
  const formId = String(detail.formId)

  return {
    formId,
    formNumericId: detail.formId,
    orderNumber: detail.orderNumber || '',
    channelOrderNumber: detail.channelOrderNumber || '',
    guestName: detail.guestName || '未命名住客',
    roomLabel: buildRoomLabel(detail.roomTypeName, detail.roomNumber),
    roomTypeName: detail.roomTypeName || '房型待确认',
    roomNumber: detail.roomNumber || '未排房',
    channelName: detail.channelName || '渠道待确认',
    checkInDate: formatDate(detail.checkInDate),
    checkOutDate: formatDate(detail.checkOutDate),
    submittedAt: formatDateTime(detail.submittedAt),
    approvedAt: formatDateTime(detail.approvedAt),
    rejectedAt: formatDateTime(detail.rejectedAt),
    updatedAt: formatDateTime(detail.updatedAt),
    status: mapBackendStatus(detail.status),
    guests: mapGuests(detail.guests),
    attachments: mapAttachments(detail.attachments),
    reviewNote: detail.reviewNote || '',
    history: mapHistory(detail.reviewLogs),
  }
}

export const getRegistrationReviewList = async (params?: RegistrationReviewListParams) => {
  const response = await request.get<ApiResponse<RegistrationListItemDTO[]>>('/registrations', {
    params: {
      status: mapFilterStatusToBackend(params?.status),
      channelId: params?.channelId,
      checkInDate: params?.checkInDate,
      checkOutDate: params?.checkOutDate,
    },
  })
  const items = unwrapApiResponse(response, '加载审查列表失败')

  return items.map((item) => mapRegistrationListItem(item))
}

export const getRegistrationLinkInbox = async () => {
  const response = await request.get<ApiResponse<RegistrationLinkInboxItemDTO[]>>('/registrations/link-inbox')
  const items = unwrapApiResponse(response, '加载链接列表失败')

  return mapLinkInboxItems(items)
}

export const getRegistrationReviewDetail = async (formId: number) => {
  const response = await request.get<ApiResponse<RegistrationDetailDTO>>(`/registrations/${formId}`)
  const detail = unwrapApiResponse(response, '加载审查详情失败')

  return mapRegistrationDetail(detail)
}

export const approveRegistrationReview = async (formId: number, note: string) => {
  const response = await request.post<ApiResponse<null>>(`/registrations/${formId}/approve`, {
    note,
  } satisfies ReviewDecisionRequest)

  unwrapApiResponse(response, '审核通过失败')
}

export const rejectRegistrationReview = async (formId: number, note: string) => {
  const response = await request.post<ApiResponse<null>>(`/registrations/${formId}/reject`, {
    note,
  } satisfies ReviewDecisionRequest)

  unwrapApiResponse(response, '驳回失败')
}

export const downloadRegistrationPdf = async (formId: number) => {
  return request.blob(`/registrations/${formId}/pdf`)
}

export const downloadRegistrationAttachment = async (formId: number, attachmentId: number) => {
  return request.blob(`/registrations/${formId}/attachments/${attachmentId}`)
}
