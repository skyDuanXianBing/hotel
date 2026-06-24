import router from '@/router'
import { resolveDefaultAuthenticatedPath, ROUTE_PATHS } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse, RequestConfig } from '@/types/api'
import {
  clearAutoLoginCredentials,
  hasStoredAutoLoginCredentials,
  loadRenewableAutoLoginCredentials,
  saveAutoLoginCredentials,
  shouldRenewTokenSoon,
  touchAutoLoginActivity,
} from '@/utils/autoLogin'
import { getCleanerToken, readCleanerStoreId } from '@/utils/cleanerSession'
import {
  applyUnifiedLoginResponse,
  clearAllLoginSessions,
  type UnifiedLoginSessionResult,
} from '@/utils/loginSessionResolver'
import {
  getStoredCurrentStoreId,
  getStoredToken,
} from '@/utils/storage'
import { API_BASE_URL } from '@/constants/api'
import type { LoginByPasswordRequest, LoginResponse } from '@/types/auth'
import { showErrorToast, sanitizeUserFacingMessage } from '@/utils/notify'

const REQUEST_TIMEOUT = 10000
const REQUEST_ERROR_HANDLED_KEY = 'toastHandled'
const REQUEST_ERROR_STATUS_KEY = 'status'
const AUTHENTICATION_FREE_PATHS = [
  '/auth/login/password',
  '/auth/login/code',
  '/auth/register',
  '/auth/send-code',
  '/auth/reset-password',
]
const PUBLIC_ROUTE_PREFIXES = ['/public/']

let silentReauthPromise: Promise<UnifiedLoginSessionResult | null> | null = null

const trimLeadingSlash = (value: string) => value.replace(/^\/+/, '')
const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '')

const isAbsoluteUrl = (value: string) => /^https?:\/\//i.test(value)

const isCleanerPath = (path: string) => path.startsWith('/cleaner')

const pathMatchesPrefix = (path: string, prefix: string) => {
  if (path === prefix) {
    return true
  }

  if (prefix.endsWith('/')) {
    return path.startsWith(prefix)
  }

  return path.startsWith(`${prefix}/`)
}

const isPublicRoutePath = (path: string) => {
  return PUBLIC_ROUTE_PREFIXES.some((prefix) => pathMatchesPrefix(path, prefix))
}

const isAdminPublicRoutePath = (path: string) => {
  if (!path) {
    return false
  }

  return (
    path === ROUTE_PATHS.login ||
    path === ROUTE_PATHS.loginCodeVerify ||
    path === ROUTE_PATHS.register ||
    path === ROUTE_PATHS.forgotPassword ||
    isPublicRoutePath(path)
  )
}

const normalizeApiPath = (url: string) => {
  if (!url) {
    return ''
  }

  try {
    const parsedUrl = isAbsoluteUrl(url) ? new URL(url) : new URL(buildRequestUrl(url), window.location.origin)
    const basePath = new URL(trimTrailingSlash(API_BASE_URL), window.location.origin).pathname
    const requestPath = parsedUrl.pathname

    if (requestPath.startsWith(basePath)) {
      const normalized = requestPath.slice(basePath.length)
      return normalized.startsWith('/') ? normalized : `/${normalized}`
    }

    return requestPath
  } catch {
    if (url.startsWith('http')) {
      return url
    }

    return url.startsWith('/') ? url : `/${url}`
  }
}

const isAuthenticationFreeRequest = (url: string) => {
  const normalizedPath = normalizeApiPath(url)

  return AUTHENTICATION_FREE_PATHS.some((path) => pathMatchesPrefix(normalizedPath, path))
}

const shouldUseAutoReauthForRoute = (path: string) => {
  return Boolean(path) && !isAdminPublicRoutePath(path)
}

const getActiveRoutePath = () => {
  const currentPath = router.currentRoute.value.path
  if (currentPath) {
    return currentPath
  }

  if (typeof window !== 'undefined') {
    return window.location.pathname
  }

  return ''
}

const getStoredTokenForRoute = (path: string) => {
  if (isCleanerPath(path)) {
    return getCleanerToken()
  }

  return getStoredToken()
}

const handleRecoveredUnifiedSessionRedirect = async (publicOnly = true) => {
  const currentPath = router.currentRoute.value.path

  if (publicOnly && !isAdminPublicRoutePath(currentPath)) {
    return
  }

  const redirectPath = resolveDefaultAuthenticatedPath()

  if (redirectPath !== currentPath) {
    await router.replace(redirectPath)
  }
}

const getHandledRequestErrorStatus = (error: unknown) => {
  if (!(error instanceof Error)) {
    return null
  }

  const status = Reflect.get(error, REQUEST_ERROR_STATUS_KEY)
  return typeof status === 'number' ? status : null
}

const performSilentUnifiedLogin = async (redirectAfterSuccess: boolean) => {
  if (silentReauthPromise) {
    return silentReauthPromise
  }

  silentReauthPromise = (async () => {
    const credentials = await loadRenewableAutoLoginCredentials()

    if (!credentials) {
      return null
    }

    const payload: LoginByPasswordRequest = {
      email: credentials.email,
      password: credentials.password,
      rememberMe: true,
    }

    try {
      const response = await executeRequest<ApiResponse<LoginResponse>>(
        {
          url: '/auth/login/password',
          method: 'POST',
          data: payload,
          suppressErrorStatuses: [400, 401, 403],
          skipAutoReauth: true,
          skipUnauthorizedHandling: true,
        },
        'json',
      )

      if (!response.success || !response.data?.token) {
        clearAutoLoginCredentials()
        return null
      }

      let sessionResult: UnifiedLoginSessionResult

      try {
        sessionResult = applyUnifiedLoginResponse(response.data, {
          resetPmsCurrentStore: false,
        })
      } catch (error) {
        clearAutoLoginCredentials()
        throw error
      }

      useAuthStore().hydrate()

      try {
        await saveAutoLoginCredentials({
          email: credentials.email,
          password: credentials.password,
          token: response.data.token,
        })
      } catch {
        clearAutoLoginCredentials()
      }

      if (redirectAfterSuccess) {
        await handleRecoveredUnifiedSessionRedirect()
      }

      return sessionResult
    } catch (error) {
      const status = getHandledRequestErrorStatus(error)

      if (status === 400 || status === 401 || status === 403) {
        clearAutoLoginCredentials()
      }

      return null
    } finally {
      silentReauthPromise = null
    }
  })()

  return silentReauthPromise
}

const restoredSessionMatchesRoute = (sessionResult: UnifiedLoginSessionResult, routePath: string) => {
  if (isCleanerPath(routePath)) {
    return sessionResult.target === 'CLEANER'
  }

  return sessionResult.target === 'PMS'
}

const handleRestoredSessionRouteMismatch = async (status?: number) => {
  await handleRecoveredUnifiedSessionRedirect(false)
  throw buildHandledError('登录身份已更新，请重新打开页面', status)
}

const ensureUnifiedSessionIfNeeded = async (force = false, validateRestoredTarget = false) => {
  const activeRoutePath = getActiveRoutePath()
  const token = getStoredTokenForRoute(activeRoutePath)

  if (!force && !shouldUseAutoReauthForRoute(activeRoutePath)) {
    if (token) {
      await touchAutoLoginActivity()
    }
    return Boolean(token)
  }

  if (!hasStoredAutoLoginCredentials()) {
    if (token) {
      await touchAutoLoginActivity()
    }
    return Boolean(token)
  }

  if (token) {
    await touchAutoLoginActivity()

    if (!shouldRenewTokenSoon(token)) {
      return true
    }
  }

  const sessionResult = await performSilentUnifiedLogin(force)
  if (
    sessionResult &&
    validateRestoredTarget &&
    !restoredSessionMatchesRoute(sessionResult, activeRoutePath)
  ) {
    await handleRestoredSessionRouteMismatch()
  }

  return Boolean(sessionResult)
}

export const restoreUnifiedSessionIfNeeded = async () => {
  return ensureUnifiedSessionIfNeeded(true)
}

const buildRequestUrl = (url: string, params?: RequestConfig['params']) => {
  let requestUrl = url

  if (!isAbsoluteUrl(url)) {
    requestUrl = `${trimTrailingSlash(API_BASE_URL)}/${trimLeadingSlash(url)}`
  }

  if (!params) {
    return requestUrl
  }

  const searchParams = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }

    searchParams.set(key, String(value))
  })

  const queryString = searchParams.toString()

  if (!queryString) {
    return requestUrl
  }

  return `${requestUrl}?${queryString}`
}

const createHeaders = (config: RequestConfig) => {
  const headers = new Headers({
    Accept: 'application/json; charset=UTF-8',
    'Content-Type': 'application/json; charset=UTF-8',
  })

  Object.entries(config.headers ?? {}).forEach(([key, value]) => {
    headers.set(key, value)
  })

  const activeRoutePath = getActiveRoutePath()
  const useCleanerSession = isCleanerPath(activeRoutePath)
  const token = useCleanerSession ? getCleanerToken() : getStoredToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const currentStoreId = useCleanerSession ? readCleanerStoreId() : getStoredCurrentStoreId()
  if (currentStoreId) {
    headers.set('X-Store-Id', currentStoreId)
  }

  if (config.data instanceof FormData) {
    headers.delete('Content-Type')
  }

  return headers
}

const buildBody = (data: unknown) => {
  if (data === undefined || data === null) {
    return undefined
  }

  if (data instanceof FormData) {
    return data
  }

  return JSON.stringify(data)
}

const readResponseData = async (response: Response) => {
  const text = await response.text()

  if (!text) {
    return null
  }

  try {
    return JSON.parse(text) as ApiResponse<unknown> | Record<string, unknown>
  } catch {
    return { message: text }
  }
}

const resolveErrorMessage = (payload: unknown, fallbackMessage: string) => {
  if (!payload || typeof payload !== 'object') {
    return sanitizeUserFacingMessage(fallbackMessage)
  }

  const message = Reflect.get(payload, 'message')
  if (typeof message === 'string' && message) {
    return sanitizeUserFacingMessage(message)
  }

  return sanitizeUserFacingMessage(fallbackMessage)
}

const handleUnauthorized = async (clearAutoLogin = false) => {
  clearAllLoginSessions({
    clearAutoLogin,
  })
  useAuthStore().clearToken()

  showErrorToast('登录已过期，请重新登录')

  const redirectPath = ROUTE_PATHS.login

  if (router.currentRoute.value.path !== redirectPath) {
    await router.replace(redirectPath)
  }
}

const buildHandledError = (message: string, status?: number) => {
  const error = new Error(message)
  Reflect.set(error, REQUEST_ERROR_HANDLED_KEY, true)
  if (typeof status === 'number') {
    Reflect.set(error, REQUEST_ERROR_STATUS_KEY, status)
  }
  return error
}

export const isHandledRequestError = (error: unknown) => {
  if (!(error instanceof Error)) {
    return false
  }

  return Reflect.get(error, REQUEST_ERROR_HANDLED_KEY) === true
}

export const getRequestErrorStatus = (error: unknown) => {
  if (!(error instanceof Error)) {
    return null
  }

  const status = Reflect.get(error, REQUEST_ERROR_STATUS_KEY)
  if (typeof status !== 'number') {
    return null
  }

  return status
}

const executeRequest = async <T>(config: RequestConfig, responseType: 'json' | 'blob'): Promise<T> => {
  const controller = new AbortController()
  const timeoutMs = config.timeoutMs ?? REQUEST_TIMEOUT
  const timeoutId = window.setTimeout(() => {
    controller.abort()
  }, timeoutMs)

  try {
    const activeRoutePath = getActiveRoutePath()
    const shouldAttemptUnifiedSessionRestore =
      !config.skipAutoReauth &&
      !isAuthenticationFreeRequest(config.url) &&
      shouldUseAutoReauthForRoute(activeRoutePath)

    if (shouldAttemptUnifiedSessionRestore) {
      await ensureUnifiedSessionIfNeeded(false, true)
    }

    const response = await fetch(buildRequestUrl(config.url, config.params), {
      method: config.method ?? 'GET',
      headers: createHeaders(config),
      body: buildBody(config.data),
      signal: controller.signal,
    })

    let payload: ApiResponse<unknown> | Record<string, unknown> | null = null

    if (!response.ok || responseType === 'json') {
      payload = await readResponseData(response.clone())
    }

    if (!response.ok) {
      const message = resolveErrorMessage(payload, response.statusText || '请求失败')
      const shouldSuppressErrorToast = config.suppressErrorStatuses?.includes(response.status) === true
      const canRecoverUnauthorized =
        response.status === 401 &&
        !config.skipAutoReauth &&
        !config.skipUnauthorizedHandling &&
        !config.retriedAfterReauth &&
        !isAuthenticationFreeRequest(config.url) &&
        shouldUseAutoReauthForRoute(activeRoutePath)

      if (canRecoverUnauthorized) {
        const restored = await performSilentUnifiedLogin(false)

        if (restored) {
          if (!restoredSessionMatchesRoute(restored, activeRoutePath)) {
            await handleRestoredSessionRouteMismatch(response.status)
          }

          return executeRequest<T>(
            {
              ...config,
              retriedAfterReauth: true,
            },
            responseType,
          )
        }
      }

      if (response.status === 401 && !config.skipUnauthorizedHandling) {
        await handleUnauthorized(canRecoverUnauthorized)
      } else if (!shouldSuppressErrorToast) {
        if (response.status === 403) {
          showErrorToast(message || '您没有权限执行此操作')
        } else {
          showErrorToast(message || '请求失败')
        }
      }

      throw buildHandledError(message || '请求失败', response.status)
    }

    if (responseType === 'blob') {
      return (await response.blob()) as T
    }

    return payload as T
  } catch (error) {
    if (isHandledRequestError(error)) {
      throw error
    }

    if (error instanceof DOMException && error.name === 'AbortError') {
      showErrorToast('请求超时，请稍后重试')
      throw buildHandledError('请求超时，请稍后重试')
    }

    const message =
      error instanceof Error && error.message ? sanitizeUserFacingMessage(error.message) : '网络异常，请稍后重试'

    showErrorToast(message)
    throw buildHandledError(message)
  } finally {
    window.clearTimeout(timeoutId)
  }
}

const request = async <T>(config: RequestConfig): Promise<T> => {
  return executeRequest<T>(config, 'json')
}

request.get = <T>(url: string, config?: Omit<RequestConfig, 'url' | 'method'>) => {
  return request<T>({
    ...config,
    url,
    method: 'GET',
  })
}

request.post = <T>(url: string, data?: unknown, config?: Omit<RequestConfig, 'url' | 'method' | 'data'>) => {
  return request<T>({
    ...config,
    url,
    data,
    method: 'POST',
  })
}

request.put = <T>(url: string, data?: unknown, config?: Omit<RequestConfig, 'url' | 'method' | 'data'>) => {
  return request<T>({
    ...config,
    url,
    data,
    method: 'PUT',
  })
}

request.delete = <T>(url: string, config?: Omit<RequestConfig, 'url' | 'method'>) => {
  return request<T>({
    ...config,
    url,
    method: 'DELETE',
  })
}

request.blob = (url: string, config?: Omit<RequestConfig, 'url' | 'method'>) => {
  return executeRequest<Blob>(
    {
      ...config,
      url,
      method: 'GET',
    },
    'blob',
  )
}

export { request }
export default request as typeof request & {
  get: <T>(url: string, config?: Omit<RequestConfig, 'url' | 'method'>) => Promise<T>
  post: <T>(
    url: string,
    data?: unknown,
    config?: Omit<RequestConfig, 'url' | 'method' | 'data'>,
  ) => Promise<T>
  put: <T>(
    url: string,
    data?: unknown,
    config?: Omit<RequestConfig, 'url' | 'method' | 'data'>,
  ) => Promise<T>
  delete: <T>(url: string, config?: Omit<RequestConfig, 'url' | 'method'>) => Promise<T>
  blob: (url: string, config?: Omit<RequestConfig, 'url' | 'method'>) => Promise<Blob>
}
