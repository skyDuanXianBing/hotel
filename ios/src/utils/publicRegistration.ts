import type {
  PublicRegistrationLanguage,
  PublicRegistrationAttachment,
  RegistrationFormStatus,
} from '@/types/publicRegistration'
import {
  DEFAULT_LOCALE,
  LOCALE_STORAGE_KEY,
  i18n,
  isSupportedLocale,
  resolveLocale,
  type SupportedLocale,
} from '@/locales'
import {
  formatBusinessDateLabel,
  formatStoreDateTime,
} from '@/utils/storeBusinessDate'

export const PUBLIC_REGISTRATION_LANGUAGE_KEY = 'registrationLang'

export const PUBLIC_REGISTRATION_LANGUAGES: PublicRegistrationLanguage[] = [
  'zh-CN',
  'zh-TW',
  'en',
  'ja',
]

export type PublicRegistrationErrorKey =
  | 'missingToken'
  | 'invalidToken'
  | 'expiredLink'
  | 'invalidOrExpiredLink'
  | 'loadFailed'
  | 'saveFailed'
  | 'submitFailed'
  | 'passportUploadFailed'
  | 'readImageFailed'
  | 'loadImageFailed'
  | 'compressFailed'

const publicRegistrationErrorKeyByMessage: Record<string, PublicRegistrationErrorKey> = {
  缺少token: 'missingToken',
  token格式错误: 'invalidToken',
  token无效: 'invalidOrExpiredLink',
  链接已过期: 'expiredLink',
  readImageFailed: 'readImageFailed',
  loadImageFailed: 'loadImageFailed',
  compressFailed: 'compressFailed',
}

const getLanguageValue = (value: unknown) => {
  if (Array.isArray(value)) {
    return value.find((item): item is string => typeof item === 'string') || ''
  }

  return typeof value === 'string' ? value : ''
}

export const normalizePublicRegistrationLanguage = (
  value: unknown,
): PublicRegistrationLanguage | '' => {
  const rawValue = getLanguageValue(value).trim()
  if (!rawValue) {
    return ''
  }

  if (isSupportedLocale(rawValue)) {
    return rawValue
  }

  const normalized = rawValue.toLowerCase()
  if (normalized === 'zh' || normalized.startsWith('zh-hans')) {
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
  if (normalized.startsWith('ko')) {
    return 'en'
  }

  return ''
}

export const isPublicRegistrationLanguage = (
  value: unknown,
): value is PublicRegistrationLanguage => {
  return typeof value === 'string' && isSupportedLocale(value)
}

export const detectPublicRegistrationLanguage = (): PublicRegistrationLanguage => {
  if (typeof navigator === 'undefined') {
    return DEFAULT_LOCALE
  }

  const browserLanguages = navigator.languages?.length
    ? navigator.languages
    : [navigator.language]
  for (const browserLanguage of browserLanguages) {
    const resolvedLanguage = normalizePublicRegistrationLanguage(browserLanguage)
    if (resolvedLanguage) {
      return resolvedLanguage
    }
  }

  return DEFAULT_LOCALE
}

export const readPublicRegistrationLanguage = () => {
  if (typeof window === 'undefined') {
    return ''
  }

  return normalizePublicRegistrationLanguage(
    window.localStorage.getItem(PUBLIC_REGISTRATION_LANGUAGE_KEY),
  )
}

export const writePublicRegistrationLanguage = (language: PublicRegistrationLanguage) => {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(PUBLIC_REGISTRATION_LANGUAGE_KEY, language)
}

export const resolvePublicRegistrationLanguage = (queryLanguage: unknown) => {
  const resolvedQueryLanguage = normalizePublicRegistrationLanguage(queryLanguage)
  if (resolvedQueryLanguage) {
    return resolvedQueryLanguage
  }

  const storedLanguage = readPublicRegistrationLanguage()
  if (storedLanguage) {
    return storedLanguage
  }

  if (typeof window !== 'undefined') {
    const appLanguage = normalizePublicRegistrationLanguage(
      window.localStorage.getItem(LOCALE_STORAGE_KEY),
    )
    if (appLanguage) {
      return appLanguage
    }
  }

  return detectPublicRegistrationLanguage()
}

export const resolvePublicRegistrationErrorKey = (
  message?: string | null,
  fallback: PublicRegistrationErrorKey = 'loadFailed',
) => {
  const normalizedMessage = String(message || '').trim()
  if (!normalizedMessage) {
    return fallback
  }

  return publicRegistrationErrorKeyByMessage[normalizedMessage] || fallback
}

export const formatPublicDate = (
  value?: string | null,
  locale: SupportedLocale = resolveLocale(i18n.global.locale.value),
) => {
  return formatBusinessDateLabel(value, 'date', '-', locale)
}

export const formatPublicDateTime = (
  value?: string | null,
  locale: SupportedLocale = resolveLocale(i18n.global.locale.value),
  storeTimeZone?: string | null,
) => {
  return formatStoreDateTime(value, 'date-time', '-', storeTimeZone, locale)
}

export const resolveRegistrationStatusColor = (status?: RegistrationFormStatus) => {
  if (status === 'APPROVED') {
    return 'success'
  }

  if (status === 'SUBMITTED') {
    return 'warning'
  }

  if (status === 'REJECTED') {
    return 'danger'
  }

  return 'medium'
}

export const normalizeExternalLink = (rawLink?: string | null) => {
  const nextLink = String(rawLink || '').trim()
  if (!nextLink) {
    return ''
  }

  if (/^https?:\/\//i.test(nextLink)) {
    return nextLink
  }

  if (/^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(nextLink)) {
    return ''
  }

  return `https://${nextLink}`
}

export const findLatestPassportAttachment = (
  attachments: PublicRegistrationAttachment[] | undefined,
  guestId?: number,
) => {
  if (!attachments || !guestId) {
    return null
  }

  for (let index = attachments.length - 1; index >= 0; index -= 1) {
    const currentAttachment = attachments[index]
    if (currentAttachment.type === 'PASSPORT' && currentAttachment.guestId === guestId) {
      return currentAttachment
    }
  }

  return null
}

export const downloadBlobFile = (blob: Blob, filename: string) => {
  if (typeof window === 'undefined') {
    return
  }

  const objectUrl = window.URL.createObjectURL(blob)
  const anchorElement = document.createElement('a')
  anchorElement.href = objectUrl
  anchorElement.download = filename
  document.body.appendChild(anchorElement)
  anchorElement.click()
  document.body.removeChild(anchorElement)
  window.URL.revokeObjectURL(objectUrl)
}

export const compressImageIfNeeded = async (file: File) => {
  if (!file.type.startsWith('image/')) {
    return file
  }

  const maxBytes = 900 * 1024
  if (file.size <= maxBytes) {
    return file
  }

  const dataUrl = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('readImageFailed'))
    reader.readAsDataURL(file)
  })

  const imageElement = await new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('loadImageFailed'))
    image.src = dataUrl
  })

  const maxSide = 2000
  const scale = Math.min(1, maxSide / Math.max(imageElement.width || 1, imageElement.height || 1))
  const width = Math.max(1, Math.round((imageElement.width || 1) * scale))
  const height = Math.max(1, Math.round((imageElement.height || 1) * scale))

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height

  const context = canvas.getContext('2d')
  if (!context) {
    return file
  }

  context.drawImage(imageElement, 0, 0, width, height)

  const compressedBlob = await new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error('compressFailed'))
          return
        }

        resolve(blob)
      },
      'image/jpeg',
      0.85,
    )
  })

  const fileName = file.name.replace(/\.(png|webp|jpeg|jpg)$/i, '') || 'passport'
  return new File([compressedBlob], `${fileName}.jpg`, { type: 'image/jpeg' })
}
