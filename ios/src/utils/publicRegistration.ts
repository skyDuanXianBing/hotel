import type {
  PublicRegistrationLanguage,
  PublicRegistrationAttachment,
  RegistrationFormStatus,
} from '@/types/publicRegistration'

export const PUBLIC_REGISTRATION_LANGUAGE_KEY = 'registrationLang'

const PUBLIC_REGISTRATION_LANGUAGES: PublicRegistrationLanguage[] = ['en', 'ja', 'zh', 'ko']

export const isPublicRegistrationLanguage = (
  value: unknown,
): value is PublicRegistrationLanguage => {
  if (typeof value !== 'string') {
    return false
  }

  return PUBLIC_REGISTRATION_LANGUAGES.includes(value as PublicRegistrationLanguage)
}

export const detectPublicRegistrationLanguage = (): PublicRegistrationLanguage => {
  if (typeof navigator === 'undefined') {
    return 'en'
  }

  const browserLanguage = navigator.language.toLowerCase()

  if (browserLanguage.startsWith('ja')) {
    return 'ja'
  }

  if (browserLanguage.startsWith('zh')) {
    return 'zh'
  }

  if (browserLanguage.startsWith('ko')) {
    return 'ko'
  }

  return 'en'
}

export const readPublicRegistrationLanguage = () => {
  if (typeof window === 'undefined') {
    return ''
  }

  const storedLanguage = window.localStorage.getItem(PUBLIC_REGISTRATION_LANGUAGE_KEY)
  if (!isPublicRegistrationLanguage(storedLanguage)) {
    return ''
  }

  return storedLanguage
}

export const writePublicRegistrationLanguage = (language: PublicRegistrationLanguage) => {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(PUBLIC_REGISTRATION_LANGUAGE_KEY, language)
}

export const resolvePublicRegistrationLanguage = (queryLanguage: unknown) => {
  if (isPublicRegistrationLanguage(queryLanguage)) {
    writePublicRegistrationLanguage(queryLanguage)
    return queryLanguage
  }

  const storedLanguage = readPublicRegistrationLanguage()
  if (storedLanguage) {
    return storedLanguage
  }

  return detectPublicRegistrationLanguage()
}

export const formatPublicDate = (value?: string | null) => {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const formatPublicDateTime = (value?: string | null) => {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
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
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })

  const imageElement = await new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('图片加载失败'))
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
          reject(new Error('图片压缩失败'))
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
