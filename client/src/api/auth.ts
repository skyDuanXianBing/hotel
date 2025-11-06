import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 用户信息
export interface UserDTO {
  id: number
  email: string
  nickname: string
  avatar?: string
  gender?: 'male' | 'female' | 'private'
  createdAt: string
  updatedAt: string
}

// 更新个人资料请求
export interface UpdateProfileRequest {
  nickname?: string
  gender?: 'male' | 'female' | 'private'
  avatar?: string
}

// 修改密码请求
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

// 登录请求（密码登录）
export interface LoginByPasswordRequest {
  email: string
  password: string
  rememberMe?: boolean
}

// 登录请求（验证码登录）
export interface LoginByCodeRequest {
  email: string
  verificationCode: string
  rememberMe?: boolean
}

// 登录响应
export interface LoginResponse {
  token: string
  user: UserDTO
}

// 注册请求
export interface RegisterRequest {
  email: string
  verificationCode: string
  password: string
}

// 重置密码请求
export interface ResetPasswordRequest {
  email: string
  verificationCode: string
  newPassword: string
}

// 发送验证码请求
export interface SendVerificationCodeRequest {
  email: string
  type: 'login' | 'register' | 'reset_password'
}

/**
 * 密码登录
 */
export const loginByPassword = (data: LoginByPasswordRequest) => {
  return request<ApiResponse<LoginResponse>>({
    url: '/auth/login/password',
    method: 'POST',
    data,
  })
}

/**
 * 验证码登录
 */
export const loginByCode = (data: LoginByCodeRequest) => {
  return request<ApiResponse<LoginResponse>>({
    url: '/auth/login/code',
    method: 'POST',
    data,
  })
}

/**
 * 注册
 */
export const register = (data: RegisterRequest) => {
  return request<ApiResponse<UserDTO>>({
    url: '/auth/register',
    method: 'POST',
    data,
  })
}

/**
 * 重置密码
 */
export const resetPassword = (data: ResetPasswordRequest) => {
  return request<ApiResponse<void>>({
    url: '/auth/reset-password',
    method: 'POST',
    data,
  })
}

/**
 * 发送验证码
 */
export const sendVerificationCode = (data: SendVerificationCodeRequest) => {
  return request<ApiResponse<void>>({
    url: '/auth/send-code',
    method: 'POST',
    data,
  })
}

/**
 * 登出
 */
export const logout = () => {
  return request<ApiResponse<void>>({
    url: '/auth/logout',
    method: 'POST',
  })
}

/**
 * 获取当前用户信息
 */
export const getCurrentUser = () => {
  return request<ApiResponse<UserDTO>>({
    url: '/auth/me',
    method: 'GET',
  })
}

/**
 * 更新个人资料
 */
export const updateProfile = (data: UpdateProfileRequest) => {
  return request<ApiResponse<UserDTO>>({
    url: '/auth/profile',
    method: 'PUT',
    data,
  })
}

/**
 * 修改密码
 */
export const changePassword = (data: ChangePasswordRequest) => {
  return request<ApiResponse<void>>({
    url: '/auth/password',
    method: 'PUT',
    data,
  })
}
