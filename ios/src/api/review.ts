import type {
  ReviewAttachment,
  ReviewFilterStatus,
  ReviewGuest,
  ReviewHistoryItem,
  ReviewLinkEntry,
  ReviewRecord,
  ReviewStatus,
} from '@/constants/reviews'
import { i18n } from '@/locales'
import type { ApiResponse } from '@/types/api'
import request from '@/utils/request'
import {
  formatBusinessDateLabel,
  formatStoreDateTime,
} from '@/utils/storeBusinessDate'

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
  birthday?: string | null
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

const PUBLIC_REGISTRATION_BOOKING_PATHS = ['/rb', '/public/registration-booking'] as const

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
  return formatBusinessDateLabel(value, 'date', '—')
}

const formatDateTime = (value?: string | null) => {
  return formatStoreDateTime(value, 'date-time', '—')
}

const reviewText = (key: string, params?: Record<string, unknown>) => {
  const path = `runtime.review.${key}`
  return params ? i18n.global.t(path, params) : i18n.global.t(path)
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

const trimSlashes = (value: string) => {
  return value.replace(/^\/+|\/+$/g, '')
}

const buildConfiguredBasePath = () => {
  const baseUrl = import.meta.env.BASE_URL || '/'
  const trimmedBaseUrl = baseUrl.trim()

  if (!trimmedBaseUrl || trimmedBaseUrl === '/') {
    return ''
  }

  const normalizedBaseUrl = trimSlashes(trimmedBaseUrl)
  if (!normalizedBaseUrl) {
    return ''
  }

  return `/${normalizedBaseUrl}`
}

const stripConfiguredBasePath = (pathname: string) => {
  const basePath = buildConfiguredBasePath()
  if (!basePath) {
    return pathname
  }

  if (pathname === basePath) {
    return '/'
  }

  if (pathname.startsWith(`${basePath}/`)) {
    return pathname.slice(basePath.length)
  }

  return pathname
}

const isPublicRegistrationBookingPath = (pathname: string) => {
  const routePath = stripConfiguredBasePath(pathname)

  for (const publicPath of PUBLIC_REGISTRATION_BOOKING_PATHS) {
    if (routePath === publicPath || routePath.startsWith(`${publicPath}/`)) {
      return true
    }
  }

  return false
}

const buildFrontendPublicPath = (pathname: string) => {
  const basePath = buildConfiguredBasePath()
  const routePath = stripConfiguredBasePath(pathname)

  if (!basePath) {
    return routePath
  }

  return `${basePath}${routePath}`
}

const normalizePublicRegistrationLinkUrl = (linkUrl?: string | null) => {
  const normalizedLinkUrl = linkUrl?.trim()
  if (!normalizedLinkUrl) {
    return ''
  }

  if (typeof window === 'undefined' || !window.location.origin) {
    return normalizedLinkUrl
  }

  try {
    const parsedUrl = new URL(normalizedLinkUrl, window.location.origin)
    if (!isPublicRegistrationBookingPath(parsedUrl.pathname)) {
      return normalizedLinkUrl
    }

    return `${window.location.origin}${buildFrontendPublicPath(parsedUrl.pathname)}${parsedUrl.search}${parsedUrl.hash}`
  } catch {
    return normalizedLinkUrl
  }
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
    return reviewText('roomPending')
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
    return reviewText('unnamedResident')
  }

  return segments.join(' ')
}

const buildGuestRelation = (guest: RegistrationGuestDTO, index: number) => {
  const sortOrder = guest.sortOrder ?? index + 1

  if (sortOrder <= 1) {
    return reviewText('mainGuest')
  }

  return reviewText('companion', { order: sortOrder })
}

const buildHistoryAction = (action?: string | null) => {
  if (action === 'APPROVE') {
    return reviewText('actionApprove')
  }

  if (action === 'REJECT') {
    return reviewText('actionReject')
  }

  if (action === 'SUBMIT') {
    return reviewText('actionSubmit')
  }

  return reviewText('actionUpdate')
}

const buildHistoryOperator = (log: RegistrationReviewLogDTO) => {
  if (log.operatorName && log.operatorName.trim()) {
    return log.operatorName.trim()
  }

  if (log.operatorUserId) {
    return reviewText('operatorUser', { id: log.operatorUserId })
  }

  return reviewText('system')
}

const mapGuests = (guests?: RegistrationGuestDTO[] | null): ReviewGuest[] => {
  if (!guests || guests.length === 0) {
    return []
  }

  return guests.map((guest, index) => {
    const sortOrder = guest.sortOrder ?? index + 1
    const idNumber = guest.passportNumber || reviewText('notProvided')

    return {
      id: String(guest.id ?? `${index + 1}`),
      sortOrder,
      name: buildGuestName(guest),
      firstName: guest.firstName || '',
      lastName: guest.lastName || '',
      birthday: guest.birthday ? formatDate(guest.birthday) : '',
      idType: reviewText('passport'),
      idNumber,
      phone: guest.phone || reviewText('notProvided'),
      relation: buildGuestRelation(guest, index),
      nationality: guest.nationality || reviewText('notProvided'),
      residenceType: guest.residenceType || reviewText('notProvided'),
      passportNumber: guest.passportNumber || reviewText('notProvided'),
      priorStay: guest.priorStay || reviewText('notProvided'),
      nextDestination: guest.nextDestination || reviewText('notProvided'),
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
      name: attachment.originalName || reviewText('attachmentName', { index: index + 1 }),
      originalName: attachment.originalName || `attachment-${attachment.id ?? index + 1}`,
      sizeLabel: reviewText('unknownSize'),
      typeLabel: attachment.type || reviewText('attachment'),
      type: attachment.type || reviewText('attachment'),
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
      linkUrl: normalizePublicRegistrationLinkUrl(item.linkUrl),
      guestName: item.guestName || reviewText('unnamedGuest'),
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
      note: log.note?.trim() || reviewText('noteEmpty'),
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
    guestName: item.guestName || reviewText('unnamedGuest'),
    roomLabel: reviewText('roomPending'),
    roomTypeName: '',
    roomNumber: '',
    channelName: item.channelName || reviewText('channelPending'),
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
    guestName: detail.guestName || reviewText('unnamedGuest'),
    roomLabel: buildRoomLabel(detail.roomTypeName, detail.roomNumber),
    roomTypeName: detail.roomTypeName || reviewText('roomTypePending'),
    roomNumber: detail.roomNumber || reviewText('roomUnassigned'),
    channelName: detail.channelName || reviewText('channelPending'),
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
  const items = unwrapApiResponse(response, reviewText('loadListFailed'))

  return items.map((item) => mapRegistrationListItem(item))
}

export const getRegistrationLinkInbox = async () => {
  const response = await request.get<ApiResponse<RegistrationLinkInboxItemDTO[]>>('/registrations/link-inbox')
  const items = unwrapApiResponse(response, reviewText('loadLinksFailed'))

  return mapLinkInboxItems(items)
}

export const getRegistrationReviewDetail = async (formId: number) => {
  const response = await request.get<ApiResponse<RegistrationDetailDTO>>(`/registrations/${formId}`)
  const detail = unwrapApiResponse(response, reviewText('loadDetailFailed'))

  return mapRegistrationDetail(detail)
}

export const approveRegistrationReview = async (formId: number, note: string) => {
  const response = await request.post<ApiResponse<null>>(`/registrations/${formId}/approve`, {
    note,
  } satisfies ReviewDecisionRequest)

  unwrapApiResponse(response, reviewText('approveFailed'))
}

export const rejectRegistrationReview = async (formId: number, note: string) => {
  const response = await request.post<ApiResponse<null>>(`/registrations/${formId}/reject`, {
    note,
  } satisfies ReviewDecisionRequest)

  unwrapApiResponse(response, reviewText('rejectFailed'))
}

export const downloadRegistrationPdf = async (formId: number) => {
  return request.blob(`/registrations/${formId}/pdf`)
}

export const downloadRegistrationAttachment = async (formId: number, attachmentId: number) => {
  return request.blob(`/registrations/${formId}/attachments/${attachmentId}`)
}
