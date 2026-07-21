import { createI18n } from 'vue-i18n'
import { SUPPORTED_LOCALES, type SupportedLocale } from './types'

export { SUPPORTED_LOCALES, type SupportedLocale } from './types'

export const DEFAULT_LOCALE: SupportedLocale = 'zh-CN'
export const LOCALE_STORAGE_KEY = 'app_language'

type LocaleMessages = Record<string, unknown>

const localeLoaders: Record<SupportedLocale, () => Promise<{ default: LocaleMessages }>> = {
  en: () => import('./messages/generated/en.json'),
  ja: () => import('./messages/generated/ja.json'),
  'zh-CN': () => import('./messages/generated/zh-CN.json'),
  'zh-TW': () => import('./messages/generated/zh-TW.json'),
}
const localeLoadPromises = new Map<SupportedLocale, Promise<void>>()

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
  if (
    normalized.startsWith('zh-tw') ||
    normalized.startsWith('zh-hk') ||
    normalized.startsWith('zh-mo') ||
    normalized.startsWith('zh-hant')
  ) {
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

  return resolveLocale(window.navigator.languages?.[0] || window.navigator.language)
}

export const i18n = createI18n({
  legacy: false,
  locale: DEFAULT_LOCALE,
  fallbackLocale: DEFAULT_LOCALE,
  messages: {},
})

export const isLocaleLoaded = (locale: SupportedLocale) =>
  i18n.global.availableLocales.includes(locale)

export const loadLocaleMessages = async (locale: SupportedLocale) => {
  if (isLocaleLoaded(locale)) {
    return
  }

  const pendingLoad = localeLoadPromises.get(locale)
  if (pendingLoad) {
    return pendingLoad
  }

  const loadPromise = localeLoaders[locale]()
    .then((module) => {
      i18n.global.setLocaleMessage(locale, module.default)
    })
    .finally(() => {
      localeLoadPromises.delete(locale)
    })

  localeLoadPromises.set(locale, loadPromise)
  return loadPromise
}

export const activateLocale = async (locale: SupportedLocale) => {
  await loadLocaleMessages(locale)
  i18n.global.locale.value = locale
}

export const initializeI18n = async () => {
  const locale = getInitialLocale()
  await activateLocale(locale)
  return locale
}
