const DEFAULT_API_BASE_URL = '/api/v1'

function trimTrailingSlash(value: string) {
  return value.replace(/\/+$/, '')
}

function normalizeBaseUrl(value?: string) {
  const normalizedValue = value?.trim()
  if (!normalizedValue) {
    return DEFAULT_API_BASE_URL
  }

  return trimTrailingSlash(normalizedValue)
}

function resolvePublicApiBaseUrl(baseUrl: string) {
  return baseUrl.replace(/\/api\/v1$/, '/api')
}

const legacyApiBaseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)
const localApiBaseUrl = normalizeBaseUrl(import.meta.env.VITE_LOCAL_API_BASE_URL || legacyApiBaseUrl)
const cloudApiBaseUrl = normalizeBaseUrl(import.meta.env.VITE_CLOUD_API_BASE_URL || localApiBaseUrl)
const useCloudApi = import.meta.env.VITE_USE_CLOUD_API === 'true'

export const API_BASE_URL = useCloudApi ? cloudApiBaseUrl : localApiBaseUrl
export const PUBLIC_API_BASE_URL = resolvePublicApiBaseUrl(API_BASE_URL)
export const SU_CONFIG_PROXY_BASE = `${API_BASE_URL}/su/config`
export const CLOUD_API_ENABLED = useCloudApi
