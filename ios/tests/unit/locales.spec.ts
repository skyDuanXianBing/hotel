import { beforeEach, describe, expect, it } from 'vitest'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'
import en from '@/locales/messages/generated/en.json'
import ja from '@/locales/messages/generated/ja.json'
import zhCN from '@/locales/messages/generated/zh-CN.json'
import zhTW from '@/locales/messages/generated/zh-TW.json'
import { publicRegistrationMessages } from '@/locales/messages/publicRegistration'
import {
  DEFAULT_LOCALE,
  LOCALE_STORAGE_KEY,
  getInitialLocale,
  resolveLocale,
} from '@/locales'

const flattenKeys = (value: unknown, prefix = ''): string[] => {
  if (value === null || typeof value !== 'object') {
    return prefix ? [prefix] : []
  }

  if (Array.isArray(value)) {
    return value.flatMap((item, index) => flattenKeys(item, `${prefix}.${index}`))
  }

  return Object.entries(value as Record<string, unknown>).flatMap(([key, child]) =>
    flattenKeys(child, prefix ? `${prefix}.${key}` : key),
  )
}

const expectSameKeys = (locale: string, value: unknown, baseline: string[]) => {
  const actual = flattenKeys(value).sort()
  const baselineSet = new Set(baseline)
  const actualSet = new Set(actual)

  expect(
    {
      missing: baseline.filter((key) => !actualSet.has(key)),
      extra: actual.filter((key) => !baselineSet.has(key)),
    },
    `${locale} translation keys differ from zh-CN`,
  ).toEqual({
    missing: [],
    extra: [],
  })
}

const flattenEntries = (value: unknown, prefix = ''): Array<[string, unknown]> => {
  if (value === null || typeof value !== 'object') {
    return prefix ? [[prefix, value]] : []
  }

  if (Array.isArray(value)) {
    return value.flatMap((item, index) => flattenEntries(item, `${prefix}.${index}`))
  }

  return Object.entries(value as Record<string, unknown>).flatMap(([key, child]) =>
    flattenEntries(child, prefix ? `${prefix}.${key}` : key),
  )
}

const collectSourceFiles = (target: string): string[] =>
  readdirSync(target).flatMap((entry) => {
    const entryPath = path.join(target, entry)
    return statSync(entryPath).isDirectory()
      ? collectSourceFiles(entryPath)
      : /\.(?:ts|vue)$/u.test(entryPath)
        ? [entryPath]
        : []
  })

describe('locale resources', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('keeps the same translation keys in all four languages', () => {
    const baseline = flattenKeys(zhCN).sort()

    expectSameKeys('zh-TW', zhTW, baseline)
    expectSameKeys('en', en, baseline)
    expectSameKeys('ja', ja, baseline)
  })

  it.each([
    ['zh-CN', zhCN],
    ['zh-TW', zhTW],
    ['en', en],
    ['ja', ja],
  ] as const)('does not contain empty or fallback-like values in %s', (_locale, messages) => {
    const invalidEntries = flattenEntries(messages).filter(([, value]) => {
      if (typeof value !== 'string') {
        return false
      }

      const normalized = value.trim().toLowerCase()
      return normalized === '' || /^(?:missing|undefined|null)$/u.test(normalized)
    })

    expect(invalidEntries).toEqual([])
  })

  it('resolves every statically referenced translation key in the default language', () => {
    const defaultKeys = new Set(flattenKeys(zhCN))
    const publicBookingKeys = new Set(flattenKeys(publicRegistrationMessages['zh-CN'].booking))
    const publicFormKeys = new Set(flattenKeys(publicRegistrationMessages['zh-CN'].form))
    const missingKeys = new Set<string>()
    const staticTranslationCall = /(?:\$t|\bt|i18n\.global\.t)\(\s*['"]([^'"]+)['"]/gu

    for (const file of collectSourceFiles('src')) {
      const source = readFileSync(file, 'utf8')
      const localKeySets = file.endsWith('RegistrationBookingPublicPage.vue')
        ? [publicBookingKeys]
        : file.endsWith('RegistrationFormPublicPage.vue')
          ? [publicFormKeys]
          : file.endsWith('usePublicRegistrationI18n.ts')
            ? [publicBookingKeys, publicFormKeys]
            : []
      for (const match of source.matchAll(staticTranslationCall)) {
        const key = match[1]
        const keyExists = localKeySets.length > 0
          ? localKeySets.some((keySet) => keySet.has(key))
          : defaultKeys.has(key)
        if (!keyExists) {
          missingKeys.add(`${file}:${key}`)
        }
      }
    }

    expect([...missingKeys].sort()).toEqual([])
  })

  it.each([
    ['zh-CN', 'zh-CN'],
    ['zh-Hans-CN', 'zh-CN'],
    ['zh-TW', 'zh-TW'],
    ['zh-Hant-HK', 'zh-TW'],
    ['en-US', 'en'],
    ['ja-JP', 'ja'],
    ['ko-KR', DEFAULT_LOCALE],
  ] as const)('resolves %s to %s', (input, expected) => {
    expect(resolveLocale(input)).toBe(expected)
  })

  it('prefers a stored locale over the browser locale', () => {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, 'ja')
    expect(getInitialLocale()).toBe('ja')
  })

  it('keeps Japanese Home and workspace content localized', () => {
    expect(ja.home.hint.default).toContain('本日')
    expect(ja.home.quick.systemNotifications[0]).toBe('システム通知')
    expect(ja.home.quick.orderNotifications[0]).toBe('予約通知')
    expect(ja.home.quick.systemNotifications[0]).not.toBe(en.home.quick.systemNotifications[0])
    expect(ja.auth.workspace.title).toBe('ワークスペースを選択')
    expect(ja.auth.workspace.cleaner).toBe('清掃ワークスペース')
    expect(ja.auth.action.sendCode).toBe('コード送信')
  })

  it('keeps Traditional Chinese Home and workspace content independent', () => {
    expect(zhTW.home.title).toBe('首頁')
    expect(zhTW.home.quick.systemNotifications[0]).toBe('系統通知')
    expect(zhTW.home.quick.orderNotifications[0]).toBe('訂單通知')
    expect(zhTW.home.hint.default).not.toBe(zhCN.home.hint.default)
    expect(zhTW.auth.workspace.title).toBe('選擇工作台')
    expect(zhTW.auth.workspace.pms).toBe('飯店管理工作台')
  })

  it('uses compact English labels for four-column Home shortcuts', () => {
    expect(en.home.quick.systemNotifications[0]).toBe('System alerts')
    expect(en.home.quick.orderNotifications[0]).toBe('Booking alerts')
  })
})
