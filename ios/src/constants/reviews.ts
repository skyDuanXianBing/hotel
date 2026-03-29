export type ReviewStatus = 'draft' | 'pending' | 'approved' | 'rejected'
export type ReviewFilterStatus = 'all' | 'draft' | 'pending' | 'approved' | 'rejected'

export interface ReviewAttachment {
  id: string
  attachmentNumericId: number
  guestId: string
  name: string
  originalName: string
  sizeLabel: string
  typeLabel: string
  type: string
}

export interface ReviewGuest {
  id: string
  sortOrder: number
  name: string
  idType: string
  idNumber: string
  phone: string
  relation: string
  nationality: string
  residenceType: string
  passportNumber: string
  priorStay: string
  nextDestination: string
}

export interface ReviewLinkEntry {
  id: string
  bookingKey: string
  linkUrl: string
  guestName: string
  checkInDate: string
  checkOutDate: string
  roomCount: number
  createdAt: string
}

export interface ReviewHistoryItem {
  id: string
  timestamp: string
  operator: string
  action: string
  note: string
}

export interface ReviewRecord {
  formId: string
  formNumericId: number
  orderNumber: string
  channelOrderNumber: string
  guestName: string
  roomLabel: string
  roomTypeName: string
  roomNumber: string
  channelName: string
  checkInDate: string
  checkOutDate: string
  submittedAt: string
  approvedAt: string
  rejectedAt: string
  updatedAt: string
  status: ReviewStatus
  guests: ReviewGuest[]
  attachments: ReviewAttachment[]
  reviewNote: string
  history: ReviewHistoryItem[]
}

export const REVIEW_STATUS_OPTIONS = [
  { label: '全部状态', value: 'all' },
  { label: '草稿', value: 'draft' },
  { label: '待审查', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
] as const

export const getReviewStatusLabel = (status: ReviewStatus) => {
  if (status === 'draft') {
    return '草稿'
  }

  if (status === 'approved') {
    return '已通过'
  }

  if (status === 'rejected') {
    return '已驳回'
  }

  return '待审查'
}
