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
  firstName: string
  lastName: string
  birthday: string
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

const INVALID_CHECK_IN_DATE_ORDER = Number.MAX_SAFE_INTEGER

const parseCheckInDateOrder = (value: string) => {
  const dateParts = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(value)

  if (!dateParts) {
    return INVALID_CHECK_IN_DATE_ORDER
  }

  const year = Number(dateParts[1])
  const month = Number(dateParts[2])
  const day = Number(dateParts[3])

  if (month < 1 || month > 12 || day < 1 || day > 31) {
    return INVALID_CHECK_IN_DATE_ORDER
  }

  return year * 10000 + month * 100 + day
}

export const sortReviewRecordsByCheckInDate = (records: ReviewRecord[]) => {
  return records
    .map((record, index) => ({ record, index }))
    .sort((left, right) => {
      const leftOrder = parseCheckInDateOrder(left.record.checkInDate)
      const rightOrder = parseCheckInDateOrder(right.record.checkInDate)

      if (leftOrder !== rightOrder) {
        return leftOrder - rightOrder
      }

      return left.index - right.index
    })
    .map((item) => item.record)
}

export const REVIEW_STATUS_OPTIONS = [
  { labelKey: 'iosStage5.cleaning.allStatuses', value: 'all' },
  { labelKey: 'stage5.common.status.draft', value: 'draft' },
  { labelKey: 'stage5.common.status.pending', value: 'pending' },
  { labelKey: 'stage5.common.status.approved', value: 'approved' },
  { labelKey: 'stage5.common.status.rejected', value: 'rejected' },
] as const

export const getReviewStatusLabel = (status: ReviewStatus) => {
  if (status === 'draft') {
    return i18n.global.t('stage5.common.status.draft')
  }

  if (status === 'approved') {
    return i18n.global.t('stage5.common.status.approved')
  }

  if (status === 'rejected') {
    return i18n.global.t('stage5.common.status.rejected')
  }

  return i18n.global.t('stage5.common.status.pending')
}
import { i18n } from '@/locales'
