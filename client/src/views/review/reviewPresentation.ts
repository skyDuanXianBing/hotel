import {
  REVIEW_ALLOWED_ACTION,
  REVIEW_CHANNEL,
  type ReviewAllowedAction,
  type ReviewReservationContext,
  type SuReviewDTO,
} from '@/api/suReviews'

const normalizeToken = (value?: string | null) =>
  String(value || '')
    .trim()
    .replace(/[\s-]+/g, '_')
    .toUpperCase()

export const normalizeChannelCode = (value?: string | null) => {
  const normalized = normalizeToken(value)
  if (normalized === 'BOOKING_COM' || normalized === 'BOOKING.COM') {
    return REVIEW_CHANNEL.BOOKING
  }
  return normalized
}

export const isAirbnbReview = (review?: SuReviewDTO | null) =>
  normalizeChannelCode(review?.channelCode) === REVIEW_CHANNEL.AIRBNB

export const isBookingReview = (review?: SuReviewDTO | null) =>
  normalizeChannelCode(review?.channelCode) === REVIEW_CHANNEL.BOOKING

export const hasAllowedReviewAction = (
  review: SuReviewDTO | null | undefined,
  action: ReviewAllowedAction,
) => {
  const normalizedAction = normalizeToken(action)
  const allowed = new Set((review?.allowedActions || []).map((item) => normalizeToken(item)))

  if (normalizedAction === REVIEW_ALLOWED_ACTION.GUEST_REVIEW && !isAirbnbReview(review)) {
    return false
  }

  return allowed.has(normalizedAction)
}

export const getReservationContext = (review: SuReviewDTO): ReviewReservationContext => {
  return {
    id: review.reservationId,
    reservationId: review.reservationId,
    orderNumber: review.orderNumber,
    channelOrderNumber: review.channelBookingId,
    guestName: review.guestName,
    checkInDate: review.checkInDate,
    checkOutDate: review.checkOutDate,
    status: review.reservationStatus,
  }
}

export const getReviewText = (review: SuReviewDTO) => review.reviewText || ''

export const getReplyText = (review: SuReviewDTO) => review.replyText || ''

export const getReviewScore = (review: SuReviewDTO) => review.overallScore ?? null

export const normalizeCategoryRatings = (ratings: Record<string, number> | null | undefined) => {
  if (!ratings) {
    return []
  }

  return Object.entries(ratings)
    .filter(([, rating]) => Number.isFinite(Number(rating)))
    .map(([category, rating]) => ({
      category,
      rating: Number(rating),
    }))
}

export const getIssueReason = (review: SuReviewDTO) =>
  review.associationReason ||
  review.actionReasons?.[REVIEW_ALLOWED_ACTION.REPLY] ||
  review.actionReasons?.[REVIEW_ALLOWED_ACTION.GUEST_REVIEW] ||
  ''

export const getReviewErrorMessage = (error: unknown, fallback: string) => {
  if (!error || typeof error !== 'object') {
    return fallback
  }

  const responseMessage = (
    error as {
      response?: {
        data?: {
          message?: unknown
        }
      }
    }
  ).response?.data?.message

  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage.trim()
  }

  const message = (error as { message?: unknown }).message
  return typeof message === 'string' && message.trim() ? message.trim() : fallback
}

export const formatReviewDateTime = (
  value: string | null | undefined,
  locale: string,
  emptyLabel: string,
) => {
  if (!value) {
    return emptyLabel
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsed)
}

export const formatReviewDate = (
  value: string | null | undefined,
  locale: string,
  emptyLabel: string,
) => {
  if (!value) {
    return emptyLabel
  }

  const dateOnly = /^\d{4}-\d{2}-\d{2}$/.test(value)
  const parsed = new Date(dateOnly ? `${value}T00:00:00` : value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(parsed)
}
