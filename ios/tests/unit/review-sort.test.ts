import { describe, expect, it } from 'vitest'
import {
  sortReviewRecordsByCheckInDate,
  type ReviewRecord,
  type ReviewStatus,
} from '@/constants/reviews'

const createReviewRecord = (
  formId: string,
  checkInDate: string,
  status: ReviewStatus = 'pending',
): ReviewRecord => {
  return {
    formId,
    formNumericId: Number(formId),
    orderNumber: '',
    channelOrderNumber: '',
    guestName: `住客 ${formId}`,
    roomLabel: '房间待分配',
    roomTypeName: '',
    roomNumber: '',
    channelName: '渠道待确认',
    checkInDate,
    checkOutDate: '2026-05-04',
    submittedAt: '2026-04-01 10:00',
    approvedAt: '—',
    rejectedAt: '—',
    updatedAt: '2026-04-01 10:00',
    status,
    guests: [],
    attachments: [],
    reviewNote: '',
    history: [],
  }
}

describe('sortReviewRecordsByCheckInDate', () => {
  it('按入住日期从早到晚排序所有审查状态记录', () => {
    const records = [
      createReviewRecord('3', '2026-05-02', 'approved'),
      createReviewRecord('1', '2026-04-30', 'draft'),
      createReviewRecord('4', '2026-05-03', 'rejected'),
      createReviewRecord('2', '2026-05-01', 'pending'),
    ]

    const sortedRecords = sortReviewRecordsByCheckInDate(records)

    expect(sortedRecords.map((record) => record.checkInDate)).toEqual([
      '2026-04-30',
      '2026-05-01',
      '2026-05-02',
      '2026-05-03',
    ])
  })

  it('同一入住日期保持原有相对顺序', () => {
    const records = [
      createReviewRecord('1', '2026-05-01'),
      createReviewRecord('2', '2026-05-01'),
      createReviewRecord('3', '2026-04-30'),
    ]

    const sortedRecords = sortReviewRecordsByCheckInDate(records)

    expect(sortedRecords.map((record) => record.formId)).toEqual(['3', '1', '2'])
  })

  it('缺失或无效入住日期排在有效日期之后', () => {
    const records = [
      createReviewRecord('1', '—'),
      createReviewRecord('2', '2026-05-01'),
      createReviewRecord('3', ''),
      createReviewRecord('4', '2026-04-30'),
    ]

    const sortedRecords = sortReviewRecordsByCheckInDate(records)

    expect(sortedRecords.map((record) => record.formId)).toEqual(['4', '2', '1', '3'])
  })
})
