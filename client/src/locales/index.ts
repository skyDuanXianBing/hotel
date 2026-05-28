import { createI18n } from 'vue-i18n'
import type { Language } from 'element-plus/es/locale'
import elementEn from 'element-plus/es/locale/lang/en'
import elementJa from 'element-plus/es/locale/lang/ja'
import elementZhCn from 'element-plus/es/locale/lang/zh-cn'
import elementZhTw from 'element-plus/es/locale/lang/zh-tw'
import en from './messages/en'
import ja from './messages/ja'
import zhCN from './messages/zh-CN'
import zhTW from './messages/zh-TW'
import { SUPPORTED_LOCALES, type SupportedLocale } from './types'

export { SUPPORTED_LOCALES, type SupportedLocale } from './types'

export const DEFAULT_LOCALE: SupportedLocale = 'zh-CN'
export const LOCALE_STORAGE_KEY = 'app_language'

const messages = {
  en,
  ja,
  'zh-CN': zhCN,
  'zh-TW': zhTW,
} as const

const elementPlusLocales: Record<SupportedLocale, Language> = {
  en: elementEn,
  ja: elementJa,
  'zh-CN': elementZhCn,
  'zh-TW': elementZhTw,
}

export const isSupportedLocale = (value: string): value is SupportedLocale =>
  SUPPORTED_LOCALES.includes(value as SupportedLocale)

export const resolveLocale = (value?: string | null): SupportedLocale => {
  if (!value) {
    return DEFAULT_LOCALE
  }

  if (isSupportedLocale(value)) {
    return value
  }

  const normalized = value.toLowerCase()
  if (normalized.startsWith('zh-tw') || normalized.startsWith('zh-hk') || normalized.startsWith('zh-hant')) {
    return 'zh-TW'
  }
  if (normalized.startsWith('zh')) {
    return 'zh-CN'
  }
  if (normalized.startsWith('ja')) {
    return 'ja'
  }
  if (normalized.startsWith('en')) {
    return 'en'
  }

  return DEFAULT_LOCALE
}

export const getInitialLocale = (): SupportedLocale => {
  if (typeof window === 'undefined') {
    return DEFAULT_LOCALE
  }

  const storedLocale = window.localStorage.getItem(LOCALE_STORAGE_KEY)
  if (storedLocale) {
    return resolveLocale(storedLocale)
  }

  const browserLocale = window.navigator.languages?.[0] || window.navigator.language
  return resolveLocale(browserLocale)
}

export const getElementPlusLocale = (locale: SupportedLocale) => elementPlusLocales[locale]

export const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages,
})

export type MessageSchema = typeof zhCN
