import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { i18n, type SupportedLocale } from '@/locales'
import {
  formatMoney,
  formatDate,
  formatNumber,
  formatPercent,
  getCurrencySymbol,
  normalizeCurrencyCode,
  resolveDisplayCurrencyCode,
  resolveMoneyLocale,
} from '@/utils/formatters'

const locales: SupportedLocale[] = ['zh-CN', 'zh-TW', 'en', 'ja']

describe('localized formatters', () => {
  beforeEach(() => {
    i18n.global.locale.value = 'zh-CN'
  })

  afterEach(() => {
    i18n.global.locale.value = 'zh-CN'
  })

  test('keeps compatibility money formatting identical in every app language', () => {
    const expectedCny = formatMoney(1234.5, 'CNY')
    const expectedUsd = formatMoney(1234.5, 'USD')
    const expectedSymbol = getCurrencySymbol('CNY')

    expect(expectedCny).toBe(`${String.fromCharCode(0xa5)}1,234.50`)
    expect(expectedUsd).toBe(expectedCny)

    for (const locale of locales) {
      i18n.global.locale.value = locale
      expect(formatMoney(1234.5, 'CNY')).toBe(expectedCny)
      expect(formatMoney(1234.5, 'USD')).toBe(expectedUsd)
      expect(getCurrencySymbol('CNY')).toBe(expectedSymbol)
    }
  })

  test('resolves the money locale from business country before currency', () => {
    expect(resolveMoneyLocale('USD', { country: 'Japan' })).toBe('ja-JP')
    expect(resolveMoneyLocale('JPY', { country: 'USA' })).toBe('en-US')
    expect(resolveMoneyLocale('GBP')).toBe('en-GB')
    expect(resolveMoneyLocale('invalid')).toBe('zh-CN')
  })

  test('uses CNY display compatibility without changing the amount', () => {
    const cnyInUsStore = formatMoney(1, 'CNY', {}, { country: 'USA' })
    const usdInJapanStore = formatMoney(1, 'USD', {}, { country: 'Japan' })
    const expected = `${String.fromCharCode(0xa5)}1.00`

    expect(cnyInUsStore).toBe(expected)
    expect(usdInJapanStore).toBe(expected)
  })

  test('keeps multi-currency normalization while resolving display currency to CNY', () => {
    const expectedSymbol = String.fromCharCode(0xa5)
    const expectedAmount = `${expectedSymbol}1,234.50`

    expect(getCurrencySymbol('CNY')).toBe(expectedSymbol)
    expect(getCurrencySymbol('JPY')).toBe(expectedSymbol)
    expect(formatMoney(1234.5, 'JPY')).toBe(expectedAmount)
    expect(formatMoney(1234.5, 'USD')).toBe(expectedAmount)
    expect(normalizeCurrencyCode(' usd ')).toBe('USD')
    expect(normalizeCurrencyCode('invalid')).toBe('CNY')
    expect(resolveDisplayCurrencyCode('USD')).toBe('CNY')
  })

  test('localizes dates, non-amount numbers, and percentages', () => {
    i18n.global.locale.value = 'zh-CN'
    const chineseDate = formatDate(new Date(Date.UTC(2026, 2, 1)), { timeZone: 'UTC' })
    const chineseNumber = formatNumber(1234.5)
    const chinesePercent = formatPercent(12.5, { maximumFractionDigits: 1 })

    i18n.global.locale.value = 'en'
    const englishDate = formatDate(new Date(Date.UTC(2026, 2, 1)), { timeZone: 'UTC' })
    const englishNumber = formatNumber(1234.5)
    const englishPercent = formatPercent(12.5, { maximumFractionDigits: 1 })

    expect(englishDate).not.toBe(chineseDate)
    expect(chineseNumber).toBe(new Intl.NumberFormat('zh-CN').format(1234.5))
    expect(englishNumber).toBe(new Intl.NumberFormat('en-US').format(1234.5))
    expect(englishPercent).toBe('12.5%')
    expect(chinesePercent).toContain('12.5')
  })
})
