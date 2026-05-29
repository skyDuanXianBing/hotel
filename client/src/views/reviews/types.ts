export type ReviewOrderChannel = 'Booking.com' | 'Airbnb'

export type ReviewOrderTab = 'pendingGuest' | 'pendingHost' | 'reviewed'

export interface GuestReview {
  rating: number
  content: string
  reviewedAt: string
}

export interface HostReview {
  rating: number
  content: string
  tags: string[]
  reviewedAt: string
}

export interface ReviewOrderRow {
  id: string
  channel: ReviewOrderChannel
  orderNumber: string
  channelOrderNumber: string
  guestName: string
  guestCount: number
  roomName: string
  checkInDate: string
  checkOutDate: string
  orderStatus: 'CHECKED_OUT'
  guestReviewDeadlineDays?: number
  hostReviewDeadlineDays?: number
  guestReview?: GuestReview
  hostReview?: HostReview
}
