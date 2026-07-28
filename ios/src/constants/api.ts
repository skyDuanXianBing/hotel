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

// 真机生产包的兜底地址：打包时 env 配错（localhost / 相对路径）会导致 App 在
// capacitor://localhost 下所有请求失败，这里强制回退到云端 HTTPS。
const PROD_FALLBACK_API_BASE_URL = 'https://pms.the-host.jp/api/v1'

function isUnreachableFromDevice(baseUrl: string) {
  if (!/^https?:\/\//i.test(baseUrl)) {
    return true
  }

  return /^https?:\/\/(localhost|127\.0\.0\.1|0\.0\.0\.0)([:/]|$)/i.test(baseUrl)
}

function resolveApiBaseUrl(baseUrl: string) {
  if (import.meta.env.PROD && isUnreachableFromDevice(baseUrl)) {
    return PROD_FALLBACK_API_BASE_URL
  }

  return baseUrl
}

const legacyApiBaseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)
const localApiBaseUrl = normalizeBaseUrl(import.meta.env.VITE_LOCAL_API_BASE_URL || legacyApiBaseUrl)
const cloudApiBaseUrl = normalizeBaseUrl(import.meta.env.VITE_CLOUD_API_BASE_URL || localApiBaseUrl)
const useCloudApi = import.meta.env.VITE_USE_CLOUD_API === 'true'

export const API_BASE_URL = resolveApiBaseUrl(useCloudApi ? cloudApiBaseUrl : localApiBaseUrl)
export const PUBLIC_API_BASE_URL = resolvePublicApiBaseUrl(API_BASE_URL)
export const SU_CONFIG_PROXY_BASE = `${API_BASE_URL}/su/config`
export const CLOUD_API_ENABLED = useCloudApi
