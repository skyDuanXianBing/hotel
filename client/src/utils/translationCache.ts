export interface TranslationCacheScope {
  storeId?: number | string | null
  userId?: number | string | null
  userKey?: string | null
  targetLanguage: string
}

export interface TranslationCacheEntry {
  sourceText: string
  translatedText: string
  targetLanguage: string
  createdAt: number
  updatedAt: number
}

const TRANSLATION_RESULT_CACHE_STORAGE_KEY = 'messages.translation.resultCache.v1'
const TRANSLATION_RESULT_CACHE_MAX_ITEMS = 500
const TRANSLATION_RESULT_CACHE_TTL_MS = 30 * 24 * 60 * 60 * 1000

type TranslationResultCache = Record<string, TranslationCacheEntry>

const normalizeScopePart = (value?: number | string | null) => {
  const normalized = String(value ?? '').trim()
  return normalized || 'unknown'
}

const normalizeSourceText = (sourceText: string) => sourceText.trim()

const hashText = (value: string) => {
  let hash = 2166136261
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index)
    hash = Math.imul(hash, 16777619)
  }
  return (hash >>> 0).toString(36)
}

const readCache = (): TranslationResultCache => {
  const raw = localStorage.getItem(TRANSLATION_RESULT_CACHE_STORAGE_KEY)
  if (!raw) {
    return {}
  }

  try {
    const parsed = JSON.parse(raw) as TranslationResultCache
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (error) {
    console.error('Failed to read translation result cache:', error)
    return {}
  }
}

const persistCache = (cache: TranslationResultCache) => {
  try {
    localStorage.setItem(TRANSLATION_RESULT_CACHE_STORAGE_KEY, JSON.stringify(cache))
  } catch (error) {
    console.error('Failed to persist translation result cache:', error)
  }
}

const pruneCache = (cache: TranslationResultCache, now = Date.now()) => {
  const validEntries = Object.entries(cache).filter(([, entry]) => {
    return now - entry.updatedAt <= TRANSLATION_RESULT_CACHE_TTL_MS
  })

  validEntries.sort(([, first], [, second]) => second.updatedAt - first.updatedAt)

  return Object.fromEntries(validEntries.slice(0, TRANSLATION_RESULT_CACHE_MAX_ITEMS))
}

export const buildTranslationCacheKey = (scope: TranslationCacheScope, sourceText: string) => {
  const normalizedSource = normalizeSourceText(sourceText)
  const storeId = normalizeScopePart(scope.storeId)
  const userId = normalizeScopePart(scope.userId ?? scope.userKey)
  const targetLanguage = normalizeScopePart(scope.targetLanguage)
  return [storeId, userId, targetLanguage, hashText(normalizedSource)].join(':')
}

export const getCachedTranslation = (scope: TranslationCacheScope, sourceText: string) => {
  const normalizedSource = normalizeSourceText(sourceText)
  if (!normalizedSource) {
    return ''
  }

  const now = Date.now()
  const cache = readCache()
  const key = buildTranslationCacheKey(scope, normalizedSource)
  const entry = cache[key]
  if (!entry) {
    return ''
  }

  if (
    entry.sourceText !== normalizedSource ||
    entry.targetLanguage !== scope.targetLanguage ||
    now - entry.updatedAt > TRANSLATION_RESULT_CACHE_TTL_MS
  ) {
    delete cache[key]
    persistCache(cache)
    return ''
  }

  return entry.translatedText
}

export const setCachedTranslation = (
  scope: TranslationCacheScope,
  sourceText: string,
  translatedText: string,
) => {
  const normalizedSource = normalizeSourceText(sourceText)
  const normalizedTranslation = translatedText.trim()
  if (!normalizedSource || !normalizedTranslation) {
    return
  }

  const now = Date.now()
  const cache = pruneCache(readCache(), now)
  const key = buildTranslationCacheKey(scope, normalizedSource)
  const existing = cache[key]

  cache[key] = {
    sourceText: normalizedSource,
    translatedText: normalizedTranslation,
    targetLanguage: scope.targetLanguage,
    createdAt: existing?.createdAt || now,
    updatedAt: now,
  }

  persistCache(pruneCache(cache, now))
}

export const clearExpiredTranslationCache = () => {
  persistCache(pruneCache(readCache()))
}
