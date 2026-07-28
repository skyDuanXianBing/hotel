import { beforeEach, describe, expect, test, vi } from 'vitest'
import {
  generateThreadAiReplyDraft,
  getMessageTranslationSetting,
  updateMessageTranslationSetting,
} from '@/api/message'
import { uploadMedia } from '@/api/media'
import { updateReservationSettlementStatus } from '@/api/reservation'
import { approveRegistrationReview, rejectRegistrationReview } from '@/api/review'
import { downloadStatisticsReport, getBusinessSummary } from '@/api/statistics'

const requestMocks = vi.hoisted(() => ({
  call: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  blob: vi.fn(),
}))

vi.mock('@/utils/request', () => {
  const request = Object.assign(requestMocks.call, {
    get: requestMocks.get,
    post: requestMocks.post,
    blob: requestMocks.blob,
  })

  return {
    default: request,
    request,
  }
})

describe('client-synchronized API contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('updates reservation settlement status with the backend payload', async () => {
    requestMocks.post.mockResolvedValue({ success: true, data: { id: 42, settled: true } })

    await updateReservationSettlementStatus(42, true)

    expect(requestMocks.post).toHaveBeenCalledWith('/reservations/42/settlement-status', {
      settled: true,
    })
  })

  test('loads and updates server-backed translation settings', async () => {
    requestMocks.call.mockResolvedValue({ success: true, data: {} })

    await getMessageTranslationSetting()
    await updateMessageTranslationSetting({
      enabled: true,
      targetLanguage: 'ja',
    })

    expect(requestMocks.call).toHaveBeenNthCalledWith(1, {
      url: '/su-messaging/translation-settings',
      method: 'GET',
      suppressErrorToast: true,
    })
    expect(requestMocks.call).toHaveBeenNthCalledWith(2, {
      url: '/su-messaging/translation-settings',
      method: 'PUT',
      data: {
        enabled: true,
        targetLanguage: 'ja',
      },
      suppressErrorToast: true,
    })
  })

  test('uses the dedicated thread AI draft endpoint', async () => {
    requestMocks.call.mockResolvedValue({ success: true, data: { draftReply: 'Welcome' } })
    const payload = {
      reservationId: 9,
      latestGuestMessageId: 101,
      recentMessages: [
        {
          direction: 'GUEST' as const,
          content: 'Can I check in early?',
          sentAt: '2026-07-26T09:00:00',
        },
      ],
      language: 'en',
    }

    await generateThreadAiReplyDraft(7, payload)

    expect(requestMocks.call).toHaveBeenCalledWith({
      url: '/su-messaging/threads/7/ai-reply-draft',
      method: 'POST',
      data: payload,
      timeoutMs: 45000,
      suppressErrorToast: true,
    })
  })

  test('uploads media with the selected scope and file', async () => {
    requestMocks.post.mockResolvedValue({ success: true, data: { url: '/media/test.png' } })
    const file = new File(['image'], 'room.png', { type: 'image/png' })

    await uploadMedia('room-type-mobile', file)

    expect(requestMocks.post).toHaveBeenCalledTimes(1)
    const [url, body] = requestMocks.post.mock.calls[0] as [string, FormData]
    expect(url).toBe('/media/upload')
    expect(body).toBeInstanceOf(FormData)
    expect(body.get('scope')).toBe('room-type-mobile')
    expect(body.get('file')).toBe(file)
  })

  test('sends guest messages with registration approval decisions', async () => {
    const responseData = {
      messageAttempted: true,
      messageLog: { sendStatus: 'SENT' },
    }
    requestMocks.post.mockResolvedValue({
      success: true,
      message: 'ok',
      data: responseData,
    })
    const payload = {
      note: 'Documents checked',
      guestMessage: 'Your registration is approved.',
      senderName: 'Front desk',
    }

    await expect(approveRegistrationReview(18, payload)).resolves.toEqual(responseData)
    await expect(rejectRegistrationReview(19, payload)).resolves.toEqual(responseData)

    expect(requestMocks.post).toHaveBeenNthCalledWith(1, '/registrations/18/approve', payload)
    expect(requestMocks.post).toHaveBeenNthCalledWith(2, '/registrations/19/reject', payload)
  })

  test('requests real business statistics and CSV exports for the selected range', async () => {
    requestMocks.get.mockResolvedValue({ success: true, data: {} })
    requestMocks.blob.mockResolvedValue(new Blob(['report']))
    const params = {
      startDate: '2026-07-20',
      endDate: '2026-07-26',
    }

    await getBusinessSummary(params)
    await downloadStatisticsReport('daily', params)

    expect(requestMocks.get).toHaveBeenCalledWith('/statistics/business/summary', {
      params,
    })
    expect(requestMocks.blob).toHaveBeenCalledWith('/statistics/reports/daily', {
      params,
      timeoutMs: 30000,
      suppressErrorToast: true,
    })
  })
})
