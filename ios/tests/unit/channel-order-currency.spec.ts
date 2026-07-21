import { describe, expect, test } from 'vitest'
import {
  buildPriceAdjustmentRequest,
  createPriceAdjustmentEditor,
} from '@/components/channel/channelUtils'
import { formatAmount } from '@/components/order/orderUtils'
import { i18n } from '@/locales'

describe('channel and order business currency', () => {
  test('formats order amounts with the temporary CNY compatibility currency', () => {
    const previousLocale = i18n.global.locale.value
    const expected = `${String.fromCharCode(0xa5)}1,250.00`

    try {
      i18n.global.locale.value = 'zh-CN'
      expect(formatAmount(1250, 'USD')).toBe(expected)

      i18n.global.locale.value = 'en'
      expect(formatAmount(1250, 'CNY')).toBe(expected)
    } finally {
      i18n.global.locale.value = previousLocale
    }
  })

  test('keeps fixed price adjustment as a stable business value', () => {
    const editor = createPriceAdjustmentEditor({
      channelId: 1,
      channelName: 'Booking.com',
      channelCode: 'BOOKING',
      adjustmentType: 'FIXED',
      adjustmentValue: -25,
      autoSyncPrice: true,
    })

    expect(editor.unit).toBe('FIXED')
    expect(buildPriceAdjustmentRequest(editor)).toEqual({
      adjustmentType: 'FIXED',
      adjustmentValue: -25,
      autoSyncPrice: true,
    })
  })
})
