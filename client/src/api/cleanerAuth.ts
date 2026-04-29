import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 保洁员信息
export interface CleanerDTO {
  id: number
  userId: number
  storeId: number
  name: string
  email: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

// 保洁员登录请求
export interface CleanerLoginRequest {
  email: string
  password: string
  rememberMe?: boolean
}

// 保洁员登录响应
export interface CleanerLoginResponse {
  token: string
  cleaner: CleanerDTO
}

/**
 * 保洁员密码登录
 */
export const cleanerLoginByPassword = (data: CleanerLoginRequest) => {
  return request<ApiResponse<CleanerLoginResponse>>({
    url: '/auth/cleaner/login/password',
    method: 'POST',
    data,
  })
}
