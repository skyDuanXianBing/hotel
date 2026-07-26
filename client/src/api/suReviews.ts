import { request } from '@/utils/request'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export const REVIEW_CHANNEL = {
  AIRBNB: 'AIRBNB',
  BOOKING: 'BOOKING',
} as const

export type ReviewChannelCode = (typeof REVIEW_CHANNEL)[keyof typeof REVIEW_CHANNEL] | string

export const REVIEW_ALLOWED_ACTION = {
  REPLY: 'REPLY',
  GUEST_REVIEW: 'GUEST_REVIEW',
  SYNC: 'SYNC',
} as const

export type ReviewAllowedAction =
  | (typeof REVIEW_ALLOWED_ACTION)[keyof typeof REVIEW_ALLOWED_ACTION]
  | string

export const REVIEW_CENTER_TAB = {
  ALL: 'ALL',
  PENDING_REPLY: 'PENDING_REPLY',
  PENDING_GUEST_REVIEW: 'PENDING_GUEST_REVIEW',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  UNLINKED: 'UNLINKED',
} as const

export type ReviewCenterTab = (typeof REVIEW_CENTER_TAB)[keyof typeof REVIEW_CENTER_TAB]

export interface ReviewReservationContext {
  id?: number | null
  reservationId?: number | null
  orderNumber?: string | null
  channelOrderNumber?: string | null
  guestName?: string | null
  roomNumber?: string | null
  roomTypeName?: string | null
  checkInDate?: string | null
  checkOutDate?: string | null
  status?: string | null
}

export interface SuReviewDTO {
  id: number | string
  reservationId?: number | null
  orderNumber?: string | null
  channelBookingId?: string | null
  guestName?: string | null
  checkInDate?: string | null
  checkOutDate?: string | null
  reservationStatus?: string | null
  channelCode: ReviewChannelCode
  suChannelId?: string | number | null
  propertyName?: string | null
  reviewType?: string | null
  reviewStatus?: string | null
  associationStatus?: string | null
  associationReason?: string | null
  lastActionStatus?: string | null
  reviewTitle?: string | null
  reviewText?: string | null
  negativeReviewText?: string | null
  overallScore?: number | null
  replyText?: string | null
  receivedAt?: string | null
  lastSyncedAt?: string | null
  allowedActions?: ReviewAllowedAction[] | null
  actionReasons?: Record<string, string> | null
}

export interface ReviewActionAudit {
  id?: number | string
  actionType?: string | null
  status?: string | null
  operatorUserId?: number | null
  responseMessage?: string | null
  errorCode?: string | null
  submittedAt?: string | null
  confirmedAt?: string | null
  createdAt?: string | null
}

export interface SuReviewDetailDTO extends SuReviewDTO {
  categoryRatings?: Record<string, number> | null
  privateFeedback?: string | null
  actions?: ReviewActionAudit[] | null
}

export interface ReviewPageResponse {
  items: SuReviewDTO[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  allowedActions: ReviewAllowedAction[]
  actionReasons: Record<string, string>
}

export interface ReviewListParams {
  tab?: ReviewCenterTab
  channel?: string
  reservationId?: number
  search?: string
  page?: number
  size?: number
}

export interface ReviewReplyRequest {
  reviewReply: string
  idempotencyKey: string
}

export type GuestReviewCategory = 'cleanliness' | 'communication' | 'respect_house_rules'

export interface GuestReviewCategoryInput {
  category: GuestReviewCategory
  rating: number
  comment?: string
  reviewCategoryTags?: string[]
}

export interface GuestReviewRequest {
  idempotencyKey: string
  confirmed: true
  isRevieweeRecommended: boolean
  publicReview: string
  privateFeedback?: string
  categoryRatings: GuestReviewCategoryInput[]
}

export interface ReviewActionResult {
  actionId?: number | string | null
  actionType?: string | null
  status?: string | null
  message?: string | null
  submittedAt?: string | null
  review?: SuReviewDetailDTO | null
}

export interface ReviewSyncResult {
  success: boolean
  fetched: number
  created: number
  updated: number
  unlinked: number
  message?: string | null
  syncedAt?: string | null
}

export const getReviews = async (
  params: ReviewListParams = {},
): Promise<ApiResponse<ReviewPageResponse>> => {
  return await request.get('/reviews', {
    params,
    suppressErrorToast: true,
  })
}

export const getReviewById = async (
  reviewId: number | string,
): Promise<ApiResponse<SuReviewDetailDTO>> => {
  return await request.get(`/reviews/${reviewId}`, {
    suppressErrorToast: true,
  })
}

export const syncReviews = async (): Promise<ApiResponse<ReviewSyncResult>> => {
  return await request.post('/reviews/sync', undefined, {
    suppressErrorToast: true,
  })
}

export const replyToReview = async (
  reviewId: number | string,
  data: ReviewReplyRequest,
): Promise<ApiResponse<ReviewActionResult>> => {
  return await request.post(`/reviews/${reviewId}/reply`, data, {
    suppressErrorToast: true,
  })
}

export const reviewGuest = async (
  reviewId: number | string,
  data: GuestReviewRequest,
): Promise<ApiResponse<ReviewActionResult>> => {
  return await request.post(`/reviews/${reviewId}/guest-review`, data, {
    suppressErrorToast: true,
  })
}
