import { LOCALE_STORAGE_KEY, resolveLocale, type SupportedLocale } from '@/locales'

export const REGISTRATION_LOCALE_STORAGE_KEY = 'registrationLang'

export type PublicRegistrationErrorKey =
  | 'missingToken'
  | 'invalidToken'
  | 'expiredLink'
  | 'invalidOrExpiredLink'
  | 'loadFailed'

const publicRegistrationErrorKeyByMessage: Record<string, PublicRegistrationErrorKey> = {
  缺少token: 'missingToken',
  token格式错误: 'invalidToken',
  token无效: 'invalidOrExpiredLink',
  链接已过期: 'expiredLink',
}

export const normalizeRegistrationLocale = (value?: string | null): SupportedLocale => {
  const raw = String(value || '').trim()
  if (!raw) {
    return resolveLocale(raw)
  }

  const normalized = raw.toLowerCase()
  if (normalized === 'zh') {
    return 'zh-CN'
  }
  if (normalized.startsWith('ko')) {
    return 'en'
  }

  return resolveLocale(raw)
}

export const getInitialRegistrationLocale = (queryLocale?: string | null): SupportedLocale => {
  if (queryLocale) {
    return normalizeRegistrationLocale(queryLocale)
  }

  if (typeof window === 'undefined') {
    return normalizeRegistrationLocale()
  }

  const storedRegistrationLocale = window.localStorage.getItem(REGISTRATION_LOCALE_STORAGE_KEY)
  if (storedRegistrationLocale) {
    return normalizeRegistrationLocale(storedRegistrationLocale)
  }

  const storedAppLocale = window.localStorage.getItem(LOCALE_STORAGE_KEY)
  if (storedAppLocale) {
    return normalizeRegistrationLocale(storedAppLocale)
  }

  const browserLocale = window.navigator.languages?.[0] || window.navigator.language
  return normalizeRegistrationLocale(browserLocale)
}

export const persistRegistrationLocale = (locale: SupportedLocale) => {
  window.localStorage.setItem(REGISTRATION_LOCALE_STORAGE_KEY, locale)
  window.localStorage.setItem(LOCALE_STORAGE_KEY, locale)
}

export const resolvePublicRegistrationErrorKey = (
  message?: string | null,
  fallback: PublicRegistrationErrorKey = 'loadFailed',
): PublicRegistrationErrorKey => {
  const normalizedMessage = String(message || '').trim()
  if (!normalizedMessage) {
    return fallback
  }

  return publicRegistrationErrorKeyByMessage[normalizedMessage] || fallback
}
