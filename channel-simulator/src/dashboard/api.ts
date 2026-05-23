import axios from 'axios'

import type { Credentials, HttpMethod, JsonObject } from './types'

const apiClient = axios.create()
let activeRequestCount = 0

apiClient.interceptors.request.use((config) => {
  activeRequestCount += 1
  return config
})

apiClient.interceptors.response.use(
  (response) => {
    activeRequestCount = Math.max(0, activeRequestCount - 1)
    return response
  },
  (error) => {
    activeRequestCount = Math.max(0, activeRequestCount - 1)
    return Promise.reject(normalizeApiError(error))
  },
)

export function getActiveRequestCount(): number {
  return activeRequestCount
}

export async function apiRequest<T>(
  method: HttpMethod,
  url: string,
  body: unknown,
  includeTestSupportKey: boolean,
  credentials: Credentials,
): Promise<T> {
  const headers = buildHeaders(credentials, includeTestSupportKey)
  const response = await apiClient.request<T>({
    method,
    url,
    headers,
    data: body === null || body === undefined ? undefined : body,
  })

  return response.data
}

function normalizeBearer(value: string): string {
  const token = value.trim()
  if (token.toLowerCase().startsWith('bearer ')) {
    return token
  }
  return `Bearer ${token}`
}

function buildHeaders(
  credentials: Credentials,
  includeTestSupportKey: boolean,
): Record<string, string> {
  const headers: Record<string, string> = {
    Accept: 'application/json',
  }
  const token = credentials.token.trim()
  const storeId = credentials.storeId.trim()
  const testSupportKey = credentials.testSupportKey.trim()

  if (token) {
    headers.Authorization = normalizeBearer(token)
  }
  if (storeId) {
    headers['X-Store-Id'] = storeId
  }
  if (includeTestSupportKey && testSupportKey) {
    headers['X-Test-Support-Key'] = testSupportKey
  }

  return headers
}

function normalizeApiError(error: unknown): Error {
  if (axios.isAxiosError(error)) {
    const message = getMessage(error.response?.data)
    if (message) {
      return new Error(message)
    }
    if (error.response?.status) {
      return new Error(`HTTP ${error.response.status}`)
    }
    if (error.message) {
      return new Error(error.message)
    }
  }

  if (error instanceof Error) {
    return error
  }
  return new Error(String(error))
}

function getMessage(value: unknown): string {
  if (!isObject(value)) {
    return ''
  }
  const raw = value.message || value.error || value.detail
  if (typeof raw === 'string') {
    return raw
  }
  return ''
}

function isObject(value: unknown): value is JsonObject {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}
