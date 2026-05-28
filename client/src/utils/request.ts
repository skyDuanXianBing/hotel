import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { i18n } from '@/locales'
import { CLEANER_STORE_KEY, CLEANER_TOKEN_KEY, clearCleanerSession } from '@/utils/cleanerSession'

declare module 'axios' {
  interface AxiosRequestConfig {
    suppressErrorToast?: boolean
  }
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json; charset=UTF-8',
    Accept: 'application/json; charset=UTF-8',
  },
})

const sanitizeUserFacingMessage = (rawMessage: string) => {
  if (!rawMessage) {
    return rawMessage
  }

  return rawMessage
    .replace(/\bSU\b/gi, '')
    .replace(/\s{2,}/g, ' ')
    .trim()
}

const shouldSuppressErrorToast = (error: unknown) => {
  if (!error || typeof error !== 'object') {
    return false
  }
  return Boolean((error as { config?: { suppressErrorToast?: boolean } }).config?.suppressErrorToast)
}

const translate = (key: string) => i18n.global.t(key)

request.interceptors.request.use(
  (config) => {
    const isCleanerRoute =
      typeof window !== 'undefined' && window.location.pathname.startsWith('/cleaner')
    const token = localStorage.getItem(isCleanerRoute ? CLEANER_TOKEN_KEY : 'token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    const currentStore = localStorage.getItem(isCleanerRoute ? CLEANER_STORE_KEY : 'currentStore')
    if (currentStore) {
      try {
        const store = JSON.parse(currentStore)
        if (store?.id) {
          config.headers['X-Store-Id'] = store.id.toString()
        }
      } catch (error) {
        console.error('Failed to parse currentStore:', error)
      }
    }

    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response: AxiosResponse) => response.data,
  (error) => {
    const suppressErrorToast = shouldSuppressErrorToast(error)

    if (error.response?.status === 401) {
      const isCleanerRoute =
        typeof window !== 'undefined' && window.location.pathname.startsWith('/cleaner')
      if (isCleanerRoute) {
        clearCleanerSession()
        window.location.href = '/cleaner/login'
      } else {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
      ElMessage.error(translate('stage6.common.messages.loginExpired'))
    } else if (error.response?.status === 403) {
      const message = sanitizeUserFacingMessage(
        error.response?.data?.message || translate('stage6.common.messages.noPermission'),
      )
      if (!suppressErrorToast) {
        ElMessage.error(message)
      }
    } else {
      const message = sanitizeUserFacingMessage(
        error.response?.data?.message || error.message || translate('stage6.common.messages.requestFailed'),
      )
      if (!suppressErrorToast) {
        ElMessage.error(message)
      }
    }
    return Promise.reject(error)
  },
)

export { request }
export default request
