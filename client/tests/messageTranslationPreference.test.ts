import { describe, expect, test } from 'bun:test'
import {
  DEFAULT_MESSAGE_TRANSLATION_PREFERENCE,
  LEGACY_MESSAGE_TRANSLATION_PREFERENCE_KEY,
  buildMessageTranslationPreferenceCacheKey,
  clearPendingLegacyMessageTranslationPreference,
  normalizeMessageTranslationPreference,
  persistMessageTranslationPreference,
  readCachedMessageTranslationPreference,
  readInitialMessageTranslationPreference,
  resolveServerMessageTranslationPreference,
  resolveMessageTranslationAiLanguageLabel,
  type MessageTranslationPreferenceStorage,
} from '../src/utils/messageTranslationPreference'

const createStorage = (initial: Record<string, string> = {}) => {
  const values = new Map(Object.entries(initial))
  const storage: MessageTranslationPreferenceStorage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  }
  return { storage, values }
}

describe('message translation preference', () => {
  test('isolates cached preferences by user id', () => {
    const { storage } = createStorage()

    persistMessageTranslationPreference(storage, 7, { enabled: true, targetLanguage: 'en' })
    persistMessageTranslationPreference(storage, 8, { enabled: false, targetLanguage: 'ja' })

    expect(readCachedMessageTranslationPreference(storage, 7)).toEqual({
      enabled: true,
      targetLanguage: 'en',
    })
    expect(readCachedMessageTranslationPreference(storage, 8)).toEqual({
      enabled: false,
      targetLanguage: 'ja',
    })
    expect(buildMessageTranslationPreferenceCacheKey(null)).toBeNull()
  })

  test('migrates a valid legacy preference into the current user cache', () => {
    const { storage, values } = createStorage({
      [LEGACY_MESSAGE_TRANSLATION_PREFERENCE_KEY]: JSON.stringify({
        enabled: true,
        targetLanguage: 'ko',
      }),
    })

    expect(readInitialMessageTranslationPreference(storage, 12)).toEqual({
      preference: { enabled: true, targetLanguage: 'ko' },
      source: 'legacy',
      pendingLegacyPreference: { enabled: true, targetLanguage: 'ko' },
    })
    expect(values.get(buildMessageTranslationPreferenceCacheKey(12)!)).toBe(
      JSON.stringify({ enabled: true, targetLanguage: 'ko' }),
    )
    expect(values.has(LEGACY_MESSAGE_TRANSLATION_PREFERENCE_KEY)).toBe(false)

    expect(readInitialMessageTranslationPreference(storage, 12)).toEqual({
      preference: { enabled: true, targetLanguage: 'ko' },
      source: 'user-cache',
      pendingLegacyPreference: { enabled: true, targetLanguage: 'ko' },
    })

    clearPendingLegacyMessageTranslationPreference(storage, 12)
    expect(readInitialMessageTranslationPreference(storage, 12).pendingLegacyPreference).toBeNull()
  })

  test('allows only the first user to claim a global legacy preference', () => {
    const { storage } = createStorage({
      [LEGACY_MESSAGE_TRANSLATION_PREFERENCE_KEY]: JSON.stringify({
        enabled: true,
        targetLanguage: 'en',
      }),
    })

    expect(readInitialMessageTranslationPreference(storage, 21).pendingLegacyPreference).toEqual({
      enabled: true,
      targetLanguage: 'en',
    })
    expect(readInitialMessageTranslationPreference(storage, 22)).toEqual({
      preference: DEFAULT_MESSAGE_TRANSLATION_PREFERENCE,
      source: 'default',
      pendingLegacyPreference: null,
    })
  })

  test('configured server defaults always override a pending legacy candidate', () => {
    const pendingLegacyPreference = { enabled: true, targetLanguage: 'ko' as const }

    expect(
      resolveServerMessageTranslationPreference(
        { enabled: false, targetLanguage: 'zh-CN', configured: true },
        pendingLegacyPreference,
      ),
    ).toEqual({
      preference: { enabled: false, targetLanguage: 'zh-CN' },
      pendingLegacyPreference: null,
    })

    expect(
      resolveServerMessageTranslationPreference(
        { enabled: false, targetLanguage: 'zh-CN', configured: false },
        pendingLegacyPreference,
      ).pendingLegacyPreference,
    ).toEqual(pendingLegacyPreference)
  })

  test('prefers the user cache over the legacy global key', () => {
    const userKey = buildMessageTranslationPreferenceCacheKey(12)!
    const { storage } = createStorage({
      [userKey]: JSON.stringify({ enabled: false, targetLanguage: 'ja' }),
      [LEGACY_MESSAGE_TRANSLATION_PREFERENCE_KEY]: JSON.stringify({
        enabled: true,
        targetLanguage: 'en',
      }),
    })

    expect(readInitialMessageTranslationPreference(storage, 12)).toEqual({
      preference: { enabled: false, targetLanguage: 'ja' },
      source: 'user-cache',
      pendingLegacyPreference: null,
    })
  })

  test('falls back safely for missing, damaged, and unsupported values', () => {
    const { storage } = createStorage({
      [LEGACY_MESSAGE_TRANSLATION_PREFERENCE_KEY]: '{damaged',
    })

    expect(readInitialMessageTranslationPreference(storage, 5)).toEqual({
      preference: DEFAULT_MESSAGE_TRANSLATION_PREFERENCE,
      source: 'default',
      pendingLegacyPreference: null,
    })
    expect(normalizeMessageTranslationPreference({ enabled: true, targetLanguage: 'fr' })).toEqual({
      enabled: true,
      targetLanguage: 'zh-CN',
    })
  })

  test('uses stable AI language labels independent of the UI locale', () => {
    expect(resolveMessageTranslationAiLanguageLabel('zh-CN')).toBe('Simplified Chinese')
    expect(resolveMessageTranslationAiLanguageLabel('en')).toBe('English')
    expect(resolveMessageTranslationAiLanguageLabel('ja')).toBe('Japanese')
    expect(resolveMessageTranslationAiLanguageLabel('ko')).toBe('Korean')
  })

  test('bootstraps server settings with a short timeout without blocking message initialization', async () => {
    const apiSource = await Bun.file(
      new URL('../src/api/suMessaging.ts', import.meta.url),
    ).text()
    const pageSource = await Bun.file(
      new URL('../src/views/messages/MessagesPage.vue', import.meta.url),
    ).text()

    expect(apiSource).toContain('SU_MESSAGING_TRANSLATION_SETTING_TIMEOUT_MS = 5_000')
    expect(apiSource).toContain('timeout: SU_MESSAGING_TRANSLATION_SETTING_TIMEOUT_MS')
    expect(pageSource).toContain('translationSettingsReady.value && translationEnabled.value')
    expect(pageSource).toContain('initializeTranslationSettings()\n  void initialize()')
  })
})
