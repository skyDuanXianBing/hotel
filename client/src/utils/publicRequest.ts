import axios, { type AxiosInstance, type AxiosResponse } from 'axios'

function resolvePublicBaseUrl(): string {
  const base = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'
  // Prefer converting /api/v1 -> /api
  const converted = base.replace(/\/api\/v1\/?$/, '/api')
  return converted
}

const publicRequest: AxiosInstance = axios.create({
  baseURL: resolvePublicBaseUrl(),
  timeout: 10000,
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

export default publicRequest
export { publicRequest }
