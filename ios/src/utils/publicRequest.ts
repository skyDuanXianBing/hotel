import type { RequestConfig, ApiResponse } from '@/types/api'
import { sanitizeUserFacingMessage, showErrorToast } from '@/utils/notify'

const PUBLIC_API_BASE_URL = resolvePublicBaseUrl()
const REQUEST_TIMEOUT = 10000
const REQUEST_ERROR_HANDLED_KEY = 'toastHandled'
const REQUEST_ERROR_STATUS_KEY = 'status'

function resolvePublicBaseUrl() {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  return baseUrl.replace(/\/api\/v1\/?$/, '/api')
}

const trimLeadingSlash = (value: string) => value.replace(/^\/+/, '')
const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '')
const isAbsoluteUrl = (value: string) => /^https?:\/\//i.test(value)

const createHeaders = (config: RequestConfig) => {
  const headers = new Headers({
    Accept: 'application/json; charset=UTF-8',
    'Content-Type': 'application/json; charset=UTF-8',
  })

  Object.entries(config.headers ?? {}).forEach(([key, value]) => {
    headers.set(key, value)
  })

  if (config.data instanceof FormData) {
    headers.delete('Content-Type')
  }

  return headers
}

const buildRequestUrl = (url: string, params?: RequestConfig['params']) => {
  let requestUrl = url
  if (!isAbsoluteUrl(url)) {
    requestUrl = `${trimTrailingSlash(PUBLIC_API_BASE_URL)}/${trimLeadingSlash(url)}`
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

const buildHandledError = (message: string, status?: number) => {
  const error = new Error(message)
  Reflect.set(error, REQUEST_ERROR_HANDLED_KEY, true)
  if (typeof status === 'number') {
    Reflect.set(error, REQUEST_ERROR_STATUS_KEY, status)
  }
  return error
}

const executeRequest = async <T>(config: RequestConfig, responseType: 'json' | 'blob'): Promise<T> => {
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => {
    controller.abort()
  }, REQUEST_TIMEOUT)

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

      if (!shouldSuppressErrorToast) {
        showErrorToast(message || '请求失败')
      }

      throw buildHandledError(message || '请求失败', response.status)
    }

    if (responseType === 'blob') {
      return (await response.blob()) as T
    }

    return payload as T
  } catch (error) {
    if (error instanceof Error && Reflect.get(error, REQUEST_ERROR_HANDLED_KEY) === true) {
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

const publicRequest = async <T>(config: RequestConfig): Promise<T> => {
  return executeRequest<T>(config, 'json')
}

publicRequest.get = <T>(url: string, config?: Omit<RequestConfig, 'url' | 'method'>) => {
  return publicRequest<T>({
    ...config,
    url,
    method: 'GET',
  })
}

publicRequest.post = <T>(
  url: string,
  data?: unknown,
  config?: Omit<RequestConfig, 'url' | 'method' | 'data'>,
) => {
  return publicRequest<T>({
    ...config,
    url,
    data,
    method: 'POST',
  })
}

publicRequest.put = <T>(
  url: string,
  data?: unknown,
  config?: Omit<RequestConfig, 'url' | 'method' | 'data'>,
) => {
  return publicRequest<T>({
    ...config,
    url,
    data,
    method: 'PUT',
  })
}

publicRequest.blob = (url: string, config?: Omit<RequestConfig, 'url' | 'method'>) => {
  return executeRequest<Blob>(
    {
      ...config,
      url,
      method: 'GET',
    },
    'blob',
  )
}

export const getPublicRequestBaseUrl = () => trimTrailingSlash(PUBLIC_API_BASE_URL)

export default publicRequest as typeof publicRequest & {
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
  blob: (url: string, config?: Omit<RequestConfig, 'url' | 'method'>) => Promise<Blob>
}
