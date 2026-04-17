import router from '@/router'
import { ROUTE_PATHS } from '@/router/guards'
import type { ApiResponse, RequestConfig } from '@/types/api'
import {
  clearCleanerSession,
  getCleanerToken,
  readCleanerStoreId,
} from '@/utils/cleanerSession'
import {
  clearSessionStorage,
  getStoredCurrentStoreId,
  getStoredToken,
} from '@/utils/storage'
import { API_BASE_URL } from '@/constants/api'
import { showErrorToast, sanitizeUserFacingMessage } from '@/utils/notify'

const REQUEST_TIMEOUT = 10000
const REQUEST_ERROR_HANDLED_KEY = 'toastHandled'
const REQUEST_ERROR_STATUS_KEY = 'status'

const trimLeadingSlash = (value: string) => value.replace(/^\/+/, '')
const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '')

const isAbsoluteUrl = (value: string) => /^https?:\/\//i.test(value)

const isCleanerPath = (path: string) => path.startsWith('/cleaner')

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

const handleUnauthorized = async () => {
  const activeRoutePath = getActiveRoutePath()
  const useCleanerSession = isCleanerPath(activeRoutePath)

  if (useCleanerSession) {
    clearCleanerSession()
  } else {
    clearSessionStorage()
  }

  showErrorToast('登录已过期，请重新登录')

  const redirectPath = useCleanerSession ? ROUTE_PATHS.cleanerLogin : ROUTE_PATHS.login

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

      if (response.status === 401) {
        await handleUnauthorized()
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
