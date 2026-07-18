export interface MessageTranslationCacheScope {
  storeId?: number | string | null
  userId?: number | string | null
  targetLanguage: string
}

interface MessageTranslationCacheEntry {
  sourceText: string
  translatedText: string
  targetLanguage: string
  updatedAt: number
}

type MessageTranslationCache = Record<string, MessageTranslationCacheEntry>

export const MESSAGE_TRANSLATION_CACHE_STORAGE_KEY = 'ios.messages.translation.results.v1'

const CACHE_MAX_ITEMS = 300
const CACHE_TTL_MS = 30 * 24 * 60 * 60 * 1000
const CACHE_WRITE_DELAY_MS = 600

let memoryCache: MessageTranslationCache | null = null
let persistTimer: ReturnType<typeof setTimeout> | null = null

const normalizeScopePart = (value?: number | string | null) => {
  const normalized = String(value ?? '').trim()
  return normalized || 'unknown'
}

const hashText = (value: string) => {
  let hash = 2166136261
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index)
    hash = Math.imul(hash, 16777619)
  }
  return (hash >>> 0).toString(36)
}

const getStorage = () => {
  if (typeof window === 'undefined') {
    return null
  }
  return window.localStorage
}

const pruneCache = (cache: MessageTranslationCache, now = Date.now()) => {
  const validEntries = Object.entries(cache).filter(([, entry]) => {
    return now - entry.updatedAt <= CACHE_TTL_MS
  })
  validEntries.sort(([, first], [, second]) => second.updatedAt - first.updatedAt)
  return Object.fromEntries(validEntries.slice(0, CACHE_MAX_ITEMS))
}

const readCache = () => {
  if (memoryCache) {
    return memoryCache
  }

  const storage = getStorage()
  if (!storage) {
    memoryCache = {}
    return memoryCache
  }

  try {
    const raw = storage.getItem(MESSAGE_TRANSLATION_CACHE_STORAGE_KEY)
    const parsed = raw ? (JSON.parse(raw) as MessageTranslationCache) : {}
    memoryCache = pruneCache(parsed && typeof parsed === 'object' ? parsed : {})
  } catch (error) {
    console.error('读取消息翻译缓存失败:', error)
    memoryCache = {}
  }

  return memoryCache
}

export const flushMessageTranslationCache = () => {
  if (persistTimer) {
    clearTimeout(persistTimer)
    persistTimer = null
  }

  const storage = getStorage()
  if (!storage || !memoryCache) {
    return
  }

  try {
    memoryCache = pruneCache(memoryCache)
    storage.setItem(MESSAGE_TRANSLATION_CACHE_STORAGE_KEY, JSON.stringify(memoryCache))
  } catch (error) {
    console.error('保存消息翻译缓存失败:', error)
  }
}

const schedulePersist = () => {
  if (persistTimer) {
    clearTimeout(persistTimer)
  }
  persistTimer = setTimeout(flushMessageTranslationCache, CACHE_WRITE_DELAY_MS)
}

export const buildMessageTranslationCacheKey = (
  scope: MessageTranslationCacheScope,
  sourceText: string,
) => {
  return [
    normalizeScopePart(scope.storeId),
    normalizeScopePart(scope.userId),
    normalizeScopePart(scope.targetLanguage),
    hashText(sourceText.trim()),
  ].join(':')
}

export const getCachedMessageTranslation = (
  scope: MessageTranslationCacheScope,
  sourceText: string,
) => {
  const normalizedSource = sourceText.trim()
  if (!normalizedSource) {
    return ''
  }

  const cache = readCache()
  const key = buildMessageTranslationCacheKey(scope, normalizedSource)
  const entry = cache[key]
  if (
    !entry ||
    entry.sourceText !== normalizedSource ||
    entry.targetLanguage !== scope.targetLanguage ||
    Date.now() - entry.updatedAt > CACHE_TTL_MS
  ) {
    if (entry) {
      delete cache[key]
      schedulePersist()
    }
    return ''
  }

  return entry.translatedText
}

export const setCachedMessageTranslation = (
  scope: MessageTranslationCacheScope,
  sourceText: string,
  translatedText: string,
) => {
  const normalizedSource = sourceText.trim()
  const normalizedTranslation = translatedText.trim()
  if (!normalizedSource || !normalizedTranslation) {
    return
  }

  const cache = readCache()
  const key = buildMessageTranslationCacheKey(scope, normalizedSource)
  cache[key] = {
    sourceText: normalizedSource,
    translatedText: normalizedTranslation,
    targetLanguage: scope.targetLanguage,
    updatedAt: Date.now(),
  }
  memoryCache = pruneCache(cache)
  schedulePersist()
}

export const clearMessageTranslationCache = () => {
  if (persistTimer) {
    clearTimeout(persistTimer)
    persistTimer = null
  }
  memoryCache = {}
  getStorage()?.removeItem(MESSAGE_TRANSLATION_CACHE_STORAGE_KEY)
}

if (typeof window !== 'undefined') {
  window.addEventListener('pagehide', flushMessageTranslationCache)
}
