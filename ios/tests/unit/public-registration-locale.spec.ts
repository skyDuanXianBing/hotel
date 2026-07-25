import { createI18n } from 'vue-i18n'
import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import { LOCALE_STORAGE_KEY } from '@/locales'
import { usePublicRegistrationI18n } from '@/composables/usePublicRegistrationI18n'
import { publicRegistrationMessages } from '@/locales/messages/publicRegistration'
import {
  PUBLIC_REGISTRATION_LANGUAGE_KEY,
  formatPublicDate,
  formatPublicDateTime,
  normalizePublicRegistrationLanguage,
  resolvePublicRegistrationErrorKey,
  resolvePublicRegistrationLanguage,
  writePublicRegistrationLanguage,
} from '@/utils/publicRegistration'

const flattenKeys = (value: unknown, prefix = ''): string[] => {
  if (value === null || typeof value !== 'object') {
    return prefix ? [prefix] : []
  }

  return Object.entries(value as Record<string, unknown>).flatMap(([key, child]) =>
    flattenKeys(child, prefix ? `${prefix}.${key}` : key),
  )
}

const flattenValues = (value: unknown): string[] => {
  if (value === null || typeof value !== 'object') {
    return typeof value === 'string' ? [value] : []
  }

  return Object.values(value as Record<string, unknown>).flatMap(flattenValues)
}

describe('public registration locale', () => {
  beforeEach(() => {
    window.localStorage.clear()
    document.documentElement.lang = ''
  })

  it.each([
    ['zh', 'zh-CN'],
    ['zh-Hans-CN', 'zh-CN'],
    ['zh-Hant-HK', 'zh-TW'],
    ['zh-TW', 'zh-TW'],
    ['en-US', 'en'],
    ['ja-JP', 'ja'],
    ['ko-KR', 'en'],
    ['fr-FR', ''],
  ] as const)('normalizes %s to %s', (input, expected) => {
    expect(normalizePublicRegistrationLanguage(input)).toBe(expected)
  })

  it('uses query, registration storage, app storage, then browser language', () => {
    const originalLanguagesDescriptor = Object.getOwnPropertyDescriptor(
      window.navigator,
      'languages',
    )

    window.localStorage.setItem(PUBLIC_REGISTRATION_LANGUAGE_KEY, 'ja')
    window.localStorage.setItem(LOCALE_STORAGE_KEY, 'en')
    expect(resolvePublicRegistrationLanguage('zh-Hant')).toBe('zh-TW')
    expect(resolvePublicRegistrationLanguage('invalid')).toBe('ja')

    window.localStorage.removeItem(PUBLIC_REGISTRATION_LANGUAGE_KEY)
    expect(resolvePublicRegistrationLanguage()).toBe('en')

    window.localStorage.removeItem(LOCALE_STORAGE_KEY)
    Object.defineProperty(window.navigator, 'languages', {
      configurable: true,
      value: ['ja-JP'],
    })
    expect(resolvePublicRegistrationLanguage()).toBe('ja')

    if (originalLanguagesDescriptor) {
      Object.defineProperty(window.navigator, 'languages', originalLanguagesDescriptor)
    } else {
      Reflect.deleteProperty(window.navigator, 'languages')
    }
  })

  it('persists only the public registration preference', () => {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, 'ja')
    writePublicRegistrationLanguage('en')

    expect(window.localStorage.getItem(PUBLIC_REGISTRATION_LANGUAGE_KEY)).toBe('en')
    expect(window.localStorage.getItem(LOCALE_STORAGE_KEY)).toBe('ja')
  })

  it('keeps booking and form keys aligned and non-empty in all four languages', () => {
    const baselineBookingKeys = flattenKeys(publicRegistrationMessages['zh-CN'].booking).sort()
    const baselineFormKeys = flattenKeys(publicRegistrationMessages['zh-CN'].form).sort()

    for (const locale of ['zh-CN', 'zh-TW', 'en', 'ja'] as const) {
      expect(flattenKeys(publicRegistrationMessages[locale].booking).sort()).toEqual(
        baselineBookingKeys,
      )
      expect(flattenKeys(publicRegistrationMessages[locale].form).sort()).toEqual(
        baselineFormKeys,
      )
      expect(
        flattenValues(publicRegistrationMessages[locale]).filter((value) => !value.trim()),
      ).toEqual([])
    }
  })

  it('formats public dates with the selected public locale', () => {
    const englishDate = formatPublicDate('2026-07-21', 'en')
    const chineseDate = formatPublicDate('2026-07-21', 'zh-CN')
    const englishDateTime = formatPublicDateTime(
      '2026-07-21T09:30:00Z',
      'en',
      'Asia/Tokyo',
    )
    const japaneseDateTime = formatPublicDateTime(
      '2026-07-21T09:30:00Z',
      'ja',
      'Asia/Tokyo',
    )

    expect(englishDate).not.toBe(chineseDate)
    expect(englishDateTime).not.toBe(japaneseDateTime)
  })

  it('maps backend and local failures to stable translation keys', () => {
    expect(resolvePublicRegistrationErrorKey('缺少token')).toBe('missingToken')
    expect(resolvePublicRegistrationErrorKey('链接已过期')).toBe('expiredLink')
    expect(resolvePublicRegistrationErrorKey('readImageFailed')).toBe('readImageFailed')
    expect(resolvePublicRegistrationErrorKey('unknown', 'submitFailed')).toBe('submitFailed')
  })

  it('switches page copy immediately without changing the employee app language', async () => {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, 'en')
    const plugin = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en: {} },
    })
    const Host = defineComponent({
      template: '<span>{{ t(\'title\') }}</span>',
      setup() {
        return usePublicRegistrationI18n('form', 'zh-CN')
      },
    })
    const wrapper = mount(Host, {
      global: {
        plugins: [plugin],
      },
    })

    expect(wrapper.text()).toContain('入住登记表')
    expect(document.documentElement.lang).toBe('zh-CN')

    wrapper.vm.setLocale('ja')
    await nextTick()

    expect(wrapper.text()).toContain('宿泊者名簿')
    expect(document.documentElement.lang).toBe('ja')
    expect(window.localStorage.getItem(PUBLIC_REGISTRATION_LANGUAGE_KEY)).toBe('ja')
    expect(window.localStorage.getItem(LOCALE_STORAGE_KEY)).toBe('en')

    wrapper.unmount()
    expect(document.documentElement.lang).toBe('en')
  })
})
