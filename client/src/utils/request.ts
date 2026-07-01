import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { i18n } from '@/locales'
import {
  CLEANER_STORE_KEY,
  CLEANER_TOKEN_KEY,
  PMS_CURRENT_STORE_KEY,
  PMS_TOKEN_KEY,
  clearAllLocalSessions,
} from '@/utils/cleanerSession'

declare module 'axios' {
  interface AxiosRequestConfig {
    suppressErrorToast?: boolean
  }
}

const DEFAULT_API_TIMEOUT_MS = 60000

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: DEFAULT_API_TIMEOUT_MS,
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

const isTimeoutError = (error: unknown) => {
  if (!axios.isAxiosError(error)) {
    return false
  }
  const message = String(error.message || '').toLowerCase()
  return error.code === 'ECONNABORTED' || message.includes('timeout')
}

const translate = (key: string) => i18n.global.t(key)
const LOGIN_PATH = '/login'
const CLEANER_PATH_PREFIX = '/cleaner'
const CLEANER_REGISTER_PATH = '/cleaner/register'
const CLEANER_LOGIN_PATH = '/cleaner/login'

const isCleanerWorkspacePath = (path: string) => {
  if (!path.startsWith(CLEANER_PATH_PREFIX)) {
    return false
  }
  if (path === CLEANER_LOGIN_PATH || path.startsWith(CLEANER_REGISTER_PATH)) {
    return false
  }
  return true
}

request.interceptors.request.use(
  (config) => {
    const isCleanerRoute =
      typeof window !== 'undefined' && isCleanerWorkspacePath(window.location.pathname)
    const token = localStorage.getItem(isCleanerRoute ? CLEANER_TOKEN_KEY : PMS_TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    const currentStore = localStorage.getItem(
      isCleanerRoute ? CLEANER_STORE_KEY : PMS_CURRENT_STORE_KEY
    )
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
      clearAllLocalSessions()
      window.location.href = LOGIN_PATH
      ElMessage.error(translate('stage6.common.messages.loginExpired'))
    } else if (error.response?.status === 403) {
      const message = sanitizeUserFacingMessage(
        error.response?.data?.message || translate('stage6.common.messages.noPermission'),
      )
      if (!suppressErrorToast) {
        ElMessage.error(message)
      }
    } else {
      const fallbackMessage = isTimeoutError(error)
        ? translate('stage6.common.messages.requestTimeout')
        : error.message || translate('stage6.common.messages.requestFailed')
      const message = sanitizeUserFacingMessage(
        error.response?.data?.message || fallbackMessage,
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
