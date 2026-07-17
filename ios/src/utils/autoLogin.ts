import type { LoginTarget } from '@/types/auth'
import { readStoredValue, removeStoredValue, writeStoredValue } from '@/utils/storage'

const AUTO_LOGIN_STORAGE_KEY = 'adminAutoLogin'
const AUTO_LOGIN_VERSION = 1
const AUTO_LOGIN_SECRET = 'the-host-ios-auto-login-v1'
const AUTO_LOGIN_IV_LENGTH = 12
const AUTO_LOGIN_RENEW_WINDOW_MS = 5 * 60 * 1000
const AUTO_LOGIN_ACTIVITY_THROTTLE_MS = 60 * 1000
const AUTO_LOGIN_MAX_IDLE_MS = 24 * 60 * 60 * 1000

interface AutoLoginCredentialsPayload {
  email: string
  password: string
  preferredLoginTarget?: LoginTarget
}

interface StoredAutoLoginEnvelope {
  version: number
  ciphertext: string
  iv: string
  lastAuthenticatedAt: number
  lastActiveAt: number
  tokenExpiresAt: number | null
}

interface SaveAutoLoginCredentialsParams {
  email: string
  password: string
  token: string
  preferredLoginTarget?: LoginTarget
}

const textEncoder = new TextEncoder()
const textDecoder = new TextDecoder()

const getCrypto = () => {
  const webCrypto = globalThis.crypto

  if (!webCrypto?.subtle) {
    throw new Error('当前设备不支持自动续登加密能力')
  }

  return webCrypto
}

const toBase64 = (value: Uint8Array) => {
  let binary = ''

  value.forEach((item) => {
    binary += String.fromCharCode(item)
  })

  return btoa(binary)
}

const fromBase64 = (value: string) => {
  const binary = atob(value)
  const bytes = new Uint8Array(binary.length)

  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }

  return bytes
}

const decodeBase64Url = (value: string) => {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const paddingLength = (4 - (normalized.length % 4)) % 4
  return `${normalized}${'='.repeat(paddingLength)}`
}

const isLoginTarget = (value: unknown): value is LoginTarget => {
  return value === 'PMS' || value === 'CLEANER'
}

const getAutoLoginKey = async () => {
  const crypto = getCrypto()
  const digest = await crypto.subtle.digest('SHA-256', textEncoder.encode(AUTO_LOGIN_SECRET))

  return crypto.subtle.importKey('raw', digest, { name: 'AES-GCM' }, false, ['encrypt', 'decrypt'])
}

const encryptCredentials = async (payload: AutoLoginCredentialsPayload) => {
  const crypto = getCrypto()
  const iv = crypto.getRandomValues(new Uint8Array(AUTO_LOGIN_IV_LENGTH))
  const key = await getAutoLoginKey()
  const plaintext = textEncoder.encode(JSON.stringify(payload))
  const encrypted = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, plaintext)

  return {
    ciphertext: toBase64(new Uint8Array(encrypted)),
    iv: toBase64(iv),
  }
}

const decryptCredentials = async (envelope: StoredAutoLoginEnvelope) => {
  const key = await getAutoLoginKey()
  const iv = fromBase64(envelope.iv)
  const ciphertext = fromBase64(envelope.ciphertext)
  const decrypted = await getCrypto().subtle.decrypt({ name: 'AES-GCM', iv }, key, ciphertext)
  const payload = JSON.parse(textDecoder.decode(decrypted)) as Partial<AutoLoginCredentialsPayload>

  if (typeof payload.email !== 'string' || typeof payload.password !== 'string') {
    throw new Error('自动续登凭证格式无效')
  }

  const email = payload.email.trim()
  const password = payload.password

  if (!email || !password) {
    throw new Error('自动续登凭证内容为空')
  }

  return {
    email,
    password,
    ...(isLoginTarget(payload.preferredLoginTarget)
      ? { preferredLoginTarget: payload.preferredLoginTarget }
      : {}),
  }
}

const readAutoLoginEnvelope = () => {
  const rawValue = readStoredValue(AUTO_LOGIN_STORAGE_KEY)

  if (!rawValue) {
    return null
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<StoredAutoLoginEnvelope>

    if (
      parsed.version !== AUTO_LOGIN_VERSION ||
      typeof parsed.ciphertext !== 'string' ||
      typeof parsed.iv !== 'string' ||
      typeof parsed.lastAuthenticatedAt !== 'number' ||
      typeof parsed.lastActiveAt !== 'number'
    ) {
      clearAutoLoginCredentials()
      return null
    }

    return {
      version: parsed.version,
      ciphertext: parsed.ciphertext,
      iv: parsed.iv,
      lastAuthenticatedAt: parsed.lastAuthenticatedAt,
      lastActiveAt: parsed.lastActiveAt,
      tokenExpiresAt: typeof parsed.tokenExpiresAt === 'number' ? parsed.tokenExpiresAt : null,
    } satisfies StoredAutoLoginEnvelope
  } catch {
    clearAutoLoginCredentials()
    return null
  }
}

const writeAutoLoginEnvelope = (envelope: StoredAutoLoginEnvelope) => {
  writeStoredValue(AUTO_LOGIN_STORAGE_KEY, JSON.stringify(envelope))
}

const isWithinAutoLoginWindow = (lastActiveAt: number, now: number) => {
  return now - lastActiveAt <= AUTO_LOGIN_MAX_IDLE_MS
}

export const resolveTokenExpiresAt = (token: string) => {
  if (!token) {
    return null
  }

  const segments = token.split('.')
  if (segments.length < 2) {
    return null
  }

  try {
    const payloadText = atob(decodeBase64Url(segments[1]))
    const payload = JSON.parse(payloadText) as { exp?: number }

    if (typeof payload.exp !== 'number' || !Number.isFinite(payload.exp)) {
      return null
    }

    return payload.exp * 1000
  } catch {
    return null
  }
}

export const shouldRenewTokenSoon = (token: string, now = Date.now()) => {
  const expiresAt = resolveTokenExpiresAt(token)

  if (expiresAt === null) {
    return false
  }

  return expiresAt <= now + AUTO_LOGIN_RENEW_WINDOW_MS
}

export const hasStoredAutoLoginCredentials = () => {
  return readAutoLoginEnvelope() !== null
}

export const clearAutoLoginCredentials = () => {
  removeStoredValue(AUTO_LOGIN_STORAGE_KEY)
}

export const saveAutoLoginCredentials = async ({
  email,
  password,
  token,
  preferredLoginTarget,
}: SaveAutoLoginCredentialsParams) => {
  const normalizedEmail = email.trim()

  if (!normalizedEmail || !password) {
    clearAutoLoginCredentials()
    return
  }

  const now = Date.now()
  const encrypted = await encryptCredentials({
    email: normalizedEmail,
    password,
    ...(isLoginTarget(preferredLoginTarget) ? { preferredLoginTarget } : {}),
  })

  writeAutoLoginEnvelope({
    version: AUTO_LOGIN_VERSION,
    ciphertext: encrypted.ciphertext,
    iv: encrypted.iv,
    lastAuthenticatedAt: now,
    lastActiveAt: now,
    tokenExpiresAt: resolveTokenExpiresAt(token),
  })
}

export const loadRenewableAutoLoginCredentials = async (now = Date.now()) => {
  const envelope = readAutoLoginEnvelope()

  if (!envelope) {
    return null
  }

  if (!isWithinAutoLoginWindow(envelope.lastActiveAt, now)) {
    clearAutoLoginCredentials()
    return null
  }

  try {
    return await decryptCredentials(envelope)
  } catch {
    clearAutoLoginCredentials()
    return null
  }
}

export const syncAutoLoginToken = async (token: string) => {
  const envelope = readAutoLoginEnvelope()

  if (!envelope) {
    return
  }

  const now = Date.now()

  writeAutoLoginEnvelope({
    ...envelope,
    lastAuthenticatedAt: now,
    lastActiveAt: now,
    tokenExpiresAt: resolveTokenExpiresAt(token),
  })
}

export const touchAutoLoginActivity = async (now = Date.now()) => {
  const envelope = readAutoLoginEnvelope()

  if (!envelope) {
    return
  }

  if (!isWithinAutoLoginWindow(envelope.lastActiveAt, now)) {
    clearAutoLoginCredentials()
    return
  }

  if (now - envelope.lastActiveAt < AUTO_LOGIN_ACTIVITY_THROTTLE_MS) {
    return
  }

  writeAutoLoginEnvelope({
    ...envelope,
    lastActiveAt: now,
  })
}

export const isAutoLoginWindowActive = (now = Date.now()) => {
  const envelope = readAutoLoginEnvelope()

  if (!envelope) {
    return false
  }

  return isWithinAutoLoginWindow(envelope.lastActiveAt, now)
}

export const AUTO_LOGIN_TEST_CONSTANTS = {
  AUTO_LOGIN_MAX_IDLE_MS,
  AUTO_LOGIN_RENEW_WINDOW_MS,
}
