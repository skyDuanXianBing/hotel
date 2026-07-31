import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { classifyRequestFailure, resolveRequestFailureMessage } from '@/utils/request'

function resolvePublicBaseUrl(): string {
  const base = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'
  // Prefer converting /api/v1 -> /api
  const converted = base.replace(/\/api\/v1\/?$/, '/api')
  return converted
}

const DEFAULT_API_TIMEOUT_MS = 60000

const publicRequest: AxiosInstance = axios.create({
  baseURL: resolvePublicBaseUrl(),
  timeout: DEFAULT_API_TIMEOUT_MS,
  headers: {
    'Content-Type': 'application/json; charset=UTF-8',
    Accept: 'application/json; charset=UTF-8',
  },
})

publicRequest.interceptors.request.use((config) => {
  if (config.data instanceof FormData) {
    if (config.headers) {
      const headersAny = config.headers as any
      if (typeof headersAny.delete === 'function') {
        headersAny.delete('Content-Type')
        headersAny.delete('content-type')
      } else {
        delete headersAny['Content-Type']
        delete headersAny['content-type']
      }
    }
  }
  return config
})

publicRequest.interceptors.response.use(
  (response: AxiosResponse) => response.data,
  (error) => Promise.reject(error),
)

/**
 * 公开独立站交易端点 403 = 门店权益失效暂停接单（P9 契约：message='该店铺暂停接单'）。
 * 前端据此切换维护态，展示本地化「店铺维护中」提示，而非透传后端固定中文 message。
 */
export const isStoreClosedError = (error: unknown): boolean => {
  if (!error || typeof error !== 'object') {
    return false
  }
  const status = (error as { response?: { status?: unknown } }).response?.status
  return status === 403
}

/**
 * 公共（免登录）页面错误文案提取（P9 修复）：
 * error.response?.data?.message（后端业务 message）照常透传；缺失时（5xx/网络错误/超时/CORS）
 * 映射为本地化友好文案，绝不把 axios 原文（"Request failed with status code 500" 等）
 * 直接弹给用户。fallback 为调用方提供的本地化操作级文案（如 t('uploadFailed')），
 * 在无法归类（4xx 无业务 message 等）时优先使用，信息更具体。
 */
export const getPublicErrorMessage = (error: unknown, fallback: string): string => {
  if (error && typeof error === 'object') {
    const responseMessage = (error as { response?: { data?: { message?: unknown } } }).response
      ?.data?.message
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      return responseMessage
    }
  }
  if (axios.isAxiosError(error)) {
    // 超时/断网/5xx 用专属文案；其余（如 4xx 无 message）用调用方操作级文案
    return classifyRequestFailure(error) === 'unknown'
      ? fallback
      : resolveRequestFailureMessage(error)
  }
  // 调用方包装过的业务错误（throw new Error(resp.message)）：message 即本地化文案，直接透传
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

export default publicRequest
export { publicRequest }
