import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { DEFAULT_LOCALE, LOCALE_STORAGE_KEY, i18n, resolveLocale } from '@/locales'
import { pinia } from '@/stores/pinia'
import {
  CLEANER_STORE_KEY,
  CLEANER_TOKEN_KEY,
  PMS_CURRENT_STORE_KEY,
  PMS_TOKEN_KEY,
  clearAllLocalSessions,
} from '@/utils/cleanerSession'
import { isRequestCancellationError } from '@/utils/requestCancellation'

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

/**
 * 402 升级引导已触发标记（P10 双 toast 修复）：request.ts 命中 402 并决定打开升级引导弹窗时，
 * 在 error 对象上同步挂该标记。调用方 catch 用 {@link isUpgradeGuided} 命中后应跳过通用错误
 * toast（如「保存失败」），避免与升级引导弹窗重复打扰。
 */
export const UPGRADE_GUIDE_SHOWN_KEY = 'upgradeGuideShown'

/** 该错误是否已由 402 升级引导弹窗接管（调用方据此跳过通用错误 toast）。 */
export const isUpgradeGuided = (error: unknown): boolean => {
  if (!error || typeof error !== 'object') {
    return false
  }
  return Boolean((error as Record<string, unknown>)[UPGRADE_GUIDE_SHOWN_KEY])
}

/** 同步打标：弹窗经动态 import 异步打开，而调用方 catch 先于弹窗执行，必须在 reject 前完成标记。 */
const markUpgradeGuided = (error: unknown) => {
  if (error && typeof error === 'object') {
    ;(error as Record<string, unknown>)[UPGRADE_GUIDE_SHOWN_KEY] = true
  }
}

const isTimeoutError = (error: unknown) => {
  if (!axios.isAxiosError(error)) {
    return false
  }
  const message = String(error.message || '').toLowerCase()
  return error.code === 'ECONNABORTED' || message.includes('timeout')
}

/**
 * 请求失败归类（P9 修复）：timeout=超时；network=无响应（断网/CORS/跨域拦截）；
 * server=5xx；unknown=其余（4xx 无业务 message、非 axios 错误等）。
 */
export type RequestFailureKind = 'timeout' | 'network' | 'server' | 'unknown'

export const classifyRequestFailure = (error: unknown): RequestFailureKind => {
  if (isTimeoutError(error)) {
    return 'timeout'
  }
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return 'network'
    }
    if ((error.response.status ?? 0) >= 500) {
      return 'server'
    }
  }
  return 'unknown'
}

const FAILURE_MESSAGE_KEYS: Record<Exclude<RequestFailureKind, 'unknown'>, string> = {
  timeout: 'stage6.common.messages.requestTimeout',
  network: 'stage6.common.messages.networkError',
  server: 'stage6.common.messages.serviceUnavailable',
}

/**
 * 后端业务 message 缺失时的本地化兜底文案（P9 修复）：
 * 超时 → requestTimeout；无响应 → networkError；5xx → serviceUnavailable；其余 → requestFailed。
 * 绝不把 axios 原文（error.message，如 "Request failed with status code 500"）弹给用户。
 * publicRequest.ts 的消费方复用同一映射。
 */
export const resolveRequestFailureMessage = (error: unknown): string => {
  const kind = classifyRequestFailure(error)
  return translate(
    kind === 'unknown' ? 'stage6.common.messages.requestFailed' : FAILURE_MESSAGE_KEYS[kind],
  )
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

const resolveAppLocale = () => {
  if (typeof localStorage === 'undefined') {
    return DEFAULT_LOCALE
  }
  return resolveLocale(localStorage.getItem(LOCALE_STORAGE_KEY))
}

request.interceptors.request.use(
  (config) => {
    const appLocale = resolveAppLocale()
    config.headers['Accept-Language'] = appLocale
    config.headers['X-App-Locale'] = appLocale

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
    if (isRequestCancellationError(error)) {
      return Promise.reject(error)
    }

    const suppressErrorToast = shouldSuppressErrorToast(error)

    if (error.response?.status === 402) {
      // SaaS 权益不足（无订阅 / 无 BOOLEAN 权益 / QUOTA 耗尽 / CAPACITY 超限）：
      // 不 toast 报错，改为触发全局升级引导弹窗；请求仍按失败 reject 给调用方。
      const payload = error.response?.data?.data as
        | { featureCode?: string; limit?: number | null; used?: number | null; reason?: string }
        | undefined
      const serverMessage = error.response?.data?.message
      if (payload?.featureCode) {
        // 先同步打标（调用方 catch 先于弹窗执行），再异步打开升级引导
        markUpgradeGuided(error)
        // 动态引入避免 request.ts ↔ stores/entitlement（经 api/billing）循环依赖
        void import('@/stores/entitlement')
          .then(({ useEntitlementStore }) => {
            useEntitlementStore(pinia).openUpgradeGuide({
              featureCode: String(payload.featureCode),
              limit: payload.limit ?? null,
              used: payload.used ?? null,
              message: typeof serverMessage === 'string' ? serverMessage : undefined,
              reason: typeof payload.reason === 'string' ? payload.reason : null,
            })
          })
          .catch(() => {
            // 引导弹窗加载失败时静默，不影响调用方错误处理
          })
      }
    } else if (error.response?.status === 401) {
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
      // 4xx 业务 message 照常透传；message 缺失时（5xx/网络错误/超时/CORS）
      // 映射为本地化友好文案，绝不把 axios 原文（如 "Request failed with status code 500"）
      // 直接弹给用户。
      const message = sanitizeUserFacingMessage(
        error.response?.data?.message || resolveRequestFailureMessage(error),
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
