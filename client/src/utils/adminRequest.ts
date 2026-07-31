import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { DEFAULT_LOCALE, LOCALE_STORAGE_KEY, i18n, resolveLocale } from '@/locales'
import {
  ADMIN_LOGIN_PATH,
  clearAdminSession,
  getAdminToken,
  isAdminWorkspacePath,
} from '@/utils/adminSession'
import { isRequestCancellationError } from '@/utils/requestCancellation'

const DEFAULT_API_TIMEOUT_MS = 60000

/**
 * 管理端 API 挂在 /api/admin/**，而租户 axios 实例 baseURL 是 /api/v1。
 * 参照 publicRequest 的先例，把 baseURL 中的 /api/v1 改写为 /api/admin。
 */
function resolveAdminBaseUrl(): string {
  const base = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'
  return base.replace(/\/api\/v1\/?$/, '/api/admin')
}

const adminRequest: AxiosInstance = axios.create({
  baseURL: resolveAdminBaseUrl(),
  timeout: DEFAULT_API_TIMEOUT_MS,
  headers: {
    'Content-Type': 'application/json; charset=UTF-8',
    Accept: 'application/json; charset=UTF-8',
  },
})

adminRequest.interceptors.request.use((config) => {
  const appLocale =
    typeof localStorage === 'undefined'
      ? DEFAULT_LOCALE
      : resolveLocale(localStorage.getItem(LOCALE_STORAGE_KEY))
  config.headers['Accept-Language'] = appLocale
  config.headers['X-App-Locale'] = appLocale

  const token = getAdminToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

adminRequest.interceptors.response.use(
  (response: AxiosResponse) => response.data,
  (error) => {
    if (isRequestCancellationError(error)) {
      return Promise.reject(error)
    }

    // 管理端 401：仅清 admin 会话并跳管理端登录页，绝不动 PMS/cleaner 会话。
    if (error.response?.status === 401) {
      clearAdminSession()
      if (typeof window !== 'undefined' && isAdminWorkspacePath(window.location.pathname)) {
        window.location.href = ADMIN_LOGIN_PATH
      }
    }
    // 其余错误原样 reject，由页面提取 ApiResponse.message 展示。
    return Promise.reject(error)
  },
)

const isTimeoutError = (error: unknown) => {
  if (!axios.isAxiosError(error)) {
    return false
  }
  const message = String(error.message || '').toLowerCase()
  return error.code === 'ECONNABORTED' || message.includes('timeout')
}

/**
 * 后端业务 message 缺失时的本地化兜底（P9 修复）：超时 → requestTimeout；
 * 无响应（断网/CORS/跨域拦截）→ networkError；5xx → serviceUnavailable；其余 → fallbackKey。
 * 绝不把 axios 原文（error.message，如 "Request failed with status code 500"）弹给用户。
 */
const resolveAdminFailureMessage = (error: unknown, fallbackKey: string) => {
  if (isTimeoutError(error)) {
    return i18n.global.t('admin.common.requestTimeout')
  }
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return i18n.global.t('admin.common.networkError')
    }
    if ((error.response.status ?? 0) >= 500) {
      return i18n.global.t('admin.common.serviceUnavailable')
    }
  }
  return i18n.global.t(fallbackKey)
}

/**
 * 从 axios 错误中提取用户可读文案，供管理端页面 toast。
 *
 * P9 修复：error.response?.data?.message（4xx 等业务 message）照常透传；
 * message 缺失时（5xx/网络错误/超时/CORS）映射为本地化友好文案，绝不把 axios 原文
 * （error.message，如 "Request failed with status code 500" / "Network Error"）弹给用户。
 */
export const getAdminErrorMessage = (error: unknown, fallbackKey = 'admin.common.loadFailed') => {
  if (error && typeof error === 'object') {
    const responseMessage = (error as { response?: { data?: { message?: unknown } } }).response
      ?.data?.message
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      return responseMessage
    }
  }
  if (axios.isAxiosError(error)) {
    return resolveAdminFailureMessage(error, fallbackKey)
  }
  // 非 axios 错误（如页面自行 throw new Error(response.message)）：保留其业务文案，
  // 但拦截 axios 原文形态的英文串（历史调用点会把 axios error.message 包进 Error 再抛出）。
  if (error instanceof Error && error.message) {
    if (/^(Request failed with status code|Network Error)/i.test(error.message)) {
      return i18n.global.t('admin.common.serviceUnavailable')
    }
    return error.message
  }
  return i18n.global.t(fallbackKey)
}

export { adminRequest }
export default adminRequest
