import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  clearMessageTranslationCache,
  flushMessageTranslationCache,
  getCachedMessageTranslation,
  MESSAGE_TRANSLATION_CACHE_STORAGE_KEY,
  setCachedMessageTranslation,
} from '@/utils/messageTranslationCache'

describe('messageTranslationCache', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    window.localStorage.clear()
    clearMessageTranslationCache()
  })

  afterEach(() => {
    clearMessageTranslationCache()
    vi.useRealTimers()
  })

  it('returns cached translations immediately and persists them in a batch', () => {
    const scope = {
      storeId: 10,
      userId: 20,
      targetLanguage: 'zh-CN',
    }

    setCachedMessageTranslation(scope, 'Hello', '你好')
    setCachedMessageTranslation(scope, 'Thank you', '谢谢')

    expect(getCachedMessageTranslation(scope, 'Hello')).toBe('你好')
    expect(window.localStorage.getItem(MESSAGE_TRANSLATION_CACHE_STORAGE_KEY)).toBeNull()

    vi.runAllTimers()
    const persisted = JSON.parse(
      window.localStorage.getItem(MESSAGE_TRANSLATION_CACHE_STORAGE_KEY) || '{}',
    )
    expect(Object.keys(persisted)).toHaveLength(2)
  })

  it('isolates translations by store, user, and target language', () => {
    const baseScope = {
      storeId: 10,
      userId: 20,
      targetLanguage: 'zh-CN',
    }

    setCachedMessageTranslation(baseScope, 'Hello', '你好')

    expect(
      getCachedMessageTranslation(
        {
          ...baseScope,
          targetLanguage: 'ja',
        },
        'Hello',
      ),
    ).toBe('')
    expect(
      getCachedMessageTranslation(
        {
          ...baseScope,
          storeId: 11,
        },
        'Hello',
      ),
    ).toBe('')
    expect(
      getCachedMessageTranslation(
        {
          ...baseScope,
          userId: 21,
        },
        'Hello',
      ),
    ).toBe('')
  })

  it('flushes pending cache writes explicitly', () => {
    const scope = {
      storeId: 10,
      userId: 20,
      targetLanguage: 'en',
    }

    setCachedMessageTranslation(scope, 'こんにちは', 'Hello')
    flushMessageTranslationCache()

    expect(window.localStorage.getItem(MESSAGE_TRANSLATION_CACHE_STORAGE_KEY)).toContain(
      'Hello',
    )
  })
})
