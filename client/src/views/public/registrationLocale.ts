import { LOCALE_STORAGE_KEY, type SupportedLocale } from '@/locales'

export const REGISTRATION_LOCALE_STORAGE_KEY = 'registrationLang'
export const PUBLIC_REGISTRATION_DEFAULT_LOCALE: SupportedLocale = 'en'

export type PublicRegistrationLocaleSource =
  | 'query'
  | 'backendHint'
  | 'registrationCache'
  | 'appCache'
  | 'guestName'
  | 'browser'
  | 'default'

export type PublicRegistrationLocaleHint = {
  // Reserved for future backend fields such as OTA country, phone country code, or guest region.
  country?: string | null
  countryCode?: string | null
  region?: string | null
  phoneCountryCode?: string | null
  phone?: string | null
  nationality?: string | null
}

export type PublicRegistrationLocaleContext = {
  queryLocale?: string | null
  backendHint?: PublicRegistrationLocaleHint | null
  storedRegistrationLocale?: string | null
  storedAppLocale?: string | null
  guestName?: string | null
  browserLocales?: string[]
}

export type PublicRegistrationLocaleResolution = {
  locale: SupportedLocale
  source: PublicRegistrationLocaleSource
}

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

const hasValue = (value?: string | null): value is string => String(value || '').trim().length > 0

export const normalizeRegistrationLocale = (value?: string | null): SupportedLocale => {
  const raw = String(value || '').trim()
  if (!raw) {
    return PUBLIC_REGISTRATION_DEFAULT_LOCALE
  }

  const normalized = raw.toLowerCase()
  if (normalized === 'zh') {
    return 'zh-CN'
  }
  if (normalized.startsWith('ko')) {
    return 'en'
  }
  if (
    normalized.startsWith('zh-cn') ||
    normalized.startsWith('zh-sg') ||
    normalized.startsWith('zh-hans')
  ) {
    return 'zh-CN'
  }
  if (
    normalized.startsWith('zh-tw') ||
    normalized.startsWith('zh-hk') ||
    normalized.startsWith('zh-mo') ||
    normalized.startsWith('zh-hant')
  ) {
    return 'zh-TW'
  }
  if (normalized.startsWith('ja')) {
    return 'ja'
  }
  if (normalized.startsWith('en')) {
    return 'en'
  }

  return PUBLIC_REGISTRATION_DEFAULT_LOCALE
}

const resolveExplicitLocale = (value?: string | null): SupportedLocale | null => {
  if (!hasValue(value)) {
    return null
  }

  return normalizeRegistrationLocale(value)
}

const normalizeHintText = (value?: string | null): string => String(value || '').trim().toLowerCase()

const resolveBackendHintLocale = (
  hint?: PublicRegistrationLocaleHint | null,
): SupportedLocale | null => {
  if (!hint) {
    return null
  }

  const values = [
    hint.countryCode,
    hint.region,
    hint.country,
    hint.phoneCountryCode,
    hint.nationality,
  ].map(normalizeHintText)
  const phone = normalizeHintText(hint.phone).replace(/[^\d+]/g, '')

  // These mappings are intentionally isolated so future backend region fields can replace them.
  if (
    values.some((value) =>
      [
        'tw',
        'twn',
        'taiwan',
        '台湾',
        '臺灣',
        'hk',
        'hkg',
        'hong kong',
        '香港',
        'mo',
        'macau',
        'macao',
        '澳门',
        '澳門',
        '+886',
        '886',
        '+852',
        '852',
        '+853',
        '853',
      ].includes(value),
    ) ||
    phone.startsWith('+886') ||
    phone.startsWith('+852') ||
    phone.startsWith('+853')
  ) {
    return 'zh-TW'
  }
  if (
    values.some((value) =>
      ['cn', 'chn', 'china', 'mainland china', '中国', '中國', '中国大陆', '中國大陸', '+86', '86'].includes(value),
    ) ||
    phone.startsWith('+86')
  ) {
    return 'zh-CN'
  }
  if (
    values.some((value) => ['jp', 'jpn', 'japan', '日本', '+81', '81'].includes(value)) ||
    phone.startsWith('+81')
  ) {
    return 'ja'
  }

  return null
}

const resolveGuestNameLocale = (guestName?: string | null): SupportedLocale | null => {
  const name = String(guestName || '').trim()
  if (!name) {
    return null
  }

  if (/[\u3040-\u30ff]/.test(name)) {
    return 'ja'
  }
  if (/[\uac00-\ud7af]/.test(name)) {
    return 'en'
  }
  if (/[臺灣灣體廣門們國國內內區區號號飯館館劃劃後後與與聯絡護證鄉鄰縣縣]/.test(name)) {
    return 'zh-TW'
  }
  if (/[台湾体广门们国内区号饭馆划后与联络护证乡邻县]/.test(name)) {
    return 'zh-CN'
  }

  return null
}

const getBrowserLocales = (): string[] => {
  if (typeof window === 'undefined') {
    return []
  }

  const languages = Array.isArray(window.navigator.languages) ? window.navigator.languages : []
  return [...languages, window.navigator.language].filter(hasValue)
}

const getStoredRegistrationLocale = (): string | null => {
  if (typeof window === 'undefined') {
    return null
  }

  return window.localStorage.getItem(REGISTRATION_LOCALE_STORAGE_KEY)
}

const getStoredAppLocale = (): string | null => {
  if (typeof window === 'undefined') {
    return null
  }

  return window.localStorage.getItem(LOCALE_STORAGE_KEY)
}

export const resolvePublicRegistrationLocale = (
  context: PublicRegistrationLocaleContext = {},
): PublicRegistrationLocaleResolution => {
  const explicitQueryLocale = resolveExplicitLocale(context.queryLocale)
  if (explicitQueryLocale) {
    return { locale: explicitQueryLocale, source: 'query' }
  }

  const backendHintLocale = resolveBackendHintLocale(context.backendHint)
  if (backendHintLocale) {
    return { locale: backendHintLocale, source: 'backendHint' }
  }

  const storedRegistrationLocale =
    context.storedRegistrationLocale === undefined
      ? getStoredRegistrationLocale()
      : context.storedRegistrationLocale
  const registrationCacheLocale = resolveExplicitLocale(storedRegistrationLocale)
  if (registrationCacheLocale) {
    return { locale: registrationCacheLocale, source: 'registrationCache' }
  }

  const storedAppLocale =
    context.storedAppLocale === undefined ? getStoredAppLocale() : context.storedAppLocale
  const appCacheLocale = resolveExplicitLocale(storedAppLocale)
  if (appCacheLocale) {
    return { locale: appCacheLocale, source: 'appCache' }
  }

  const guestNameLocale = resolveGuestNameLocale(context.guestName)
  if (guestNameLocale) {
    return { locale: guestNameLocale, source: 'guestName' }
  }

  const browserLocales = context.browserLocales === undefined ? getBrowserLocales() : context.browserLocales
  const browserLocale = browserLocales
    .map(resolveExplicitLocale)
    .find((value): value is SupportedLocale => !!value)
  if (browserLocale) {
    return { locale: browserLocale, source: 'browser' }
  }

  return { locale: PUBLIC_REGISTRATION_DEFAULT_LOCALE, source: 'default' }
}

export const getInitialRegistrationLocale = (queryLocale?: string | null): SupportedLocale => {
  return resolvePublicRegistrationLocale({ queryLocale }).locale
}

export const shouldPersistRegistrationLocale = (
  resolution: PublicRegistrationLocaleResolution,
): boolean => {
  return resolution.source !== 'browser' && resolution.source !== 'default'
}

export const shouldApplyLoadedRegistrationLocale = (
  currentSource: PublicRegistrationLocaleSource,
  resolution: PublicRegistrationLocaleResolution,
): boolean => {
  if (resolution.source === 'backendHint') {
    return currentSource !== 'query'
  }

  if (resolution.source === 'guestName') {
    return currentSource === 'browser' || currentSource === 'default'
  }

  return false
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
