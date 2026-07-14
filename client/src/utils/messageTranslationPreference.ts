export const LEGACY_MESSAGE_TRANSLATION_PREFERENCE_KEY = 'messages.translation.settings'
export const MESSAGE_TRANSLATION_PREFERENCE_KEY_PREFIX = 'messages.translation.preference.v2:'
export const MESSAGE_TRANSLATION_LEGACY_PENDING_KEY_PREFIX =
  'messages.translation.legacy-pending.v2:'

export const MESSAGE_TRANSLATION_LANGUAGE_VALUES = ['zh-CN', 'en', 'ja', 'ko'] as const

export type MessageTranslationLanguage = (typeof MESSAGE_TRANSLATION_LANGUAGE_VALUES)[number]

export interface MessageTranslationPreference {
  enabled: boolean
  targetLanguage: MessageTranslationLanguage
}

export interface MessageTranslationPreferenceStorage {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
  removeItem(key: string): void
}

export interface InitialMessageTranslationPreference {
  preference: MessageTranslationPreference
  source: 'user-cache' | 'legacy' | 'default'
  pendingLegacyPreference: MessageTranslationPreference | null
}

export interface ServerMessageTranslationPreference extends MessageTranslationPreference {
  configured: boolean
}

export const DEFAULT_MESSAGE_TRANSLATION_PREFERENCE: MessageTranslationPreference = {
  enabled: false,
  targetLanguage: 'zh-CN',
}

const MESSAGE_TRANSLATION_AI_LANGUAGE_LABELS: Record<MessageTranslationLanguage, string> = {
  'zh-CN': 'Simplified Chinese',
  en: 'English',
  ja: 'Japanese',
  ko: 'Korean',
}

export const isMessageTranslationLanguage = (value: unknown): value is MessageTranslationLanguage =>
  typeof value === 'string' &&
  MESSAGE_TRANSLATION_LANGUAGE_VALUES.includes(value as MessageTranslationLanguage)

export const normalizeMessageTranslationPreference = (
  value: unknown,
): MessageTranslationPreference => {
  if (!value || typeof value !== 'object') {
    return { ...DEFAULT_MESSAGE_TRANSLATION_PREFERENCE }
  }

  const candidate = value as { enabled?: unknown; targetLanguage?: unknown }
  return {
    enabled:
      typeof candidate.enabled === 'boolean'
        ? candidate.enabled
        : DEFAULT_MESSAGE_TRANSLATION_PREFERENCE.enabled,
    targetLanguage: isMessageTranslationLanguage(candidate.targetLanguage)
      ? candidate.targetLanguage
      : DEFAULT_MESSAGE_TRANSLATION_PREFERENCE.targetLanguage,
  }
}

const normalizeUserId = (userId: number | string | null | undefined) => {
  const normalized = String(userId ?? '').trim()
  return normalized || null
}

export const buildMessageTranslationPreferenceCacheKey = (
  userId: number | string | null | undefined,
) => {
  const normalizedUserId = normalizeUserId(userId)
  return normalizedUserId ? `${MESSAGE_TRANSLATION_PREFERENCE_KEY_PREFIX}${normalizedUserId}` : null
}

const buildPendingLegacyPreferenceKey = (
  userId: number | string | null | undefined,
) => {
  const normalizedUserId = normalizeUserId(userId)
  return normalizedUserId
    ? `${MESSAGE_TRANSLATION_LEGACY_PENDING_KEY_PREFIX}${normalizedUserId}`
    : null
}

const readPreference = (
  storage: MessageTranslationPreferenceStorage,
  key: string,
): MessageTranslationPreference | null => {
  try {
    const raw = storage.getItem(key)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') {
      return null
    }
    return normalizeMessageTranslationPreference(parsed)
  } catch {
    return null
  }
}

export const readCachedMessageTranslationPreference = (
  storage: MessageTranslationPreferenceStorage,
  userId: number | string | null | undefined,
) => {
  const key = buildMessageTranslationPreferenceCacheKey(userId)
  return key ? readPreference(storage, key) : null
}

export const persistMessageTranslationPreference = (
  storage: MessageTranslationPreferenceStorage,
  userId: number | string | null | undefined,
  preference: MessageTranslationPreference,
) => {
  const key = buildMessageTranslationPreferenceCacheKey(userId)
  if (!key) {
    return false
  }

  try {
    storage.setItem(key, JSON.stringify(normalizeMessageTranslationPreference(preference)))
    return true
  } catch {
    return false
  }
}

const persistPendingLegacyPreference = (
  storage: MessageTranslationPreferenceStorage,
  userId: number | string | null | undefined,
  preference: MessageTranslationPreference,
) => {
  const key = buildPendingLegacyPreferenceKey(userId)
  if (!key) {
    return false
  }
  try {
    storage.setItem(key, JSON.stringify(normalizeMessageTranslationPreference(preference)))
    return true
  } catch {
    // Local cache failures must not block loading or saving the server preference.
    return false
  }
}

const removeLegacyGlobalPreference = (storage: MessageTranslationPreferenceStorage) => {
  try {
    storage.removeItem(LEGACY_MESSAGE_TRANSLATION_PREFERENCE_KEY)
  } catch {
    // Leave the legacy key untouched if the browser refuses the write.
  }
}

export const clearPendingLegacyMessageTranslationPreference = (
  storage: MessageTranslationPreferenceStorage,
  userId: number | string | null | undefined,
) => {
  const key = buildPendingLegacyPreferenceKey(userId)
  if (!key) {
    return
  }
  try {
    storage.removeItem(key)
  } catch {
    // Local cache failures must not turn a successful server save into an error.
  }
}

export const readInitialMessageTranslationPreference = (
  storage: MessageTranslationPreferenceStorage,
  userId: number | string | null | undefined,
): InitialMessageTranslationPreference => {
  const pendingLegacyPreferenceKey = buildPendingLegacyPreferenceKey(userId)
  const pendingLegacyPreference = pendingLegacyPreferenceKey
    ? readPreference(storage, pendingLegacyPreferenceKey)
    : null
  const cachedPreference = readCachedMessageTranslationPreference(storage, userId)
  if (cachedPreference) {
    return {
      preference: cachedPreference,
      source: 'user-cache',
      pendingLegacyPreference,
    }
  }

  const legacyPreference = readPreference(storage, LEGACY_MESSAGE_TRANSLATION_PREFERENCE_KEY)
  if (legacyPreference) {
    persistMessageTranslationPreference(storage, userId, legacyPreference)
    const pendingCreated = persistPendingLegacyPreference(storage, userId, legacyPreference)
    if (pendingCreated) {
      removeLegacyGlobalPreference(storage)
    }
    return {
      preference: legacyPreference,
      source: 'legacy',
      pendingLegacyPreference: legacyPreference,
    }
  }

  return {
    preference: { ...DEFAULT_MESSAGE_TRANSLATION_PREFERENCE },
    source: 'default',
    pendingLegacyPreference,
  }
}

export const resolveServerMessageTranslationPreference = (
  serverPreference: ServerMessageTranslationPreference,
  pendingLegacyPreference: MessageTranslationPreference | null,
) => ({
  preference: normalizeMessageTranslationPreference(serverPreference),
  pendingLegacyPreference: serverPreference.configured ? null : pendingLegacyPreference,
})

export const resolveMessageTranslationAiLanguageLabel = (language: MessageTranslationLanguage) =>
  MESSAGE_TRANSLATION_AI_LANGUAGE_LABELS[language]
