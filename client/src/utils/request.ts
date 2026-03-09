import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { CLEANER_STORE_KEY, CLEANER_TOKEN_KEY, clearCleanerSession } from '@/utils/cleanerSession'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json; charset=UTF-8',
    Accept: 'application/json; charset=UTF-8',
  },
})

// 请求拦截器：附加 token 和当前门店 ID。
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
        console.error('解析 currentStore 失败:', error)
      }
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器：统一处理接口错误。
request.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
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
      ElMessage.error('登录已过期，请重新登录')
    } else if (error.response?.status === 403) {
      const message = error.response?.data?.message || '您没有权限执行此操作'
      ElMessage.error(message)
    } else {
      const message = error.response?.data?.message || error.message || '请求失败'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

export { request }
export default request
