import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json; charset=UTF-8',
    Accept: 'application/json; charset=UTF-8',
  },
})

// 请求拦截器：添加token和storeId
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加当前门店ID到请求头
    const currentStore = localStorage.getItem('currentStore')
    if (currentStore) {
      try {
        const store = JSON.parse(currentStore)
        if (store?.id) {
          config.headers['X-Store-Id'] = store.id.toString()
        }
      } catch (e) {
        console.error('解析currentStore失败:', e)
      }
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器：处理响应和错误
request.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    // 处理401未授权错误
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
      ElMessage.error('登录已过期，请重新登录')
    }
    // 处理403权限不足错误
    else if (error.response?.status === 403) {
      const message = error.response?.data?.message || '您没有权限执行此操作'
      ElMessage.error(message)
    }
    // 处理其他错误
    else {
      const message = error.response?.data?.message || error.message || '请求失败'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

export { request }
export default request
