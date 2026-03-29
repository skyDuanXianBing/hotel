import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type {
  ChangePasswordRequest,
  LoginByCodeRequest,
  LoginByPasswordRequest,
  LoginResponse,
  RegisterRequest,
  ResetPasswordRequest,
  SendVerificationCodeRequest,
  UpdateProfileRequest,
  UserDTO,
} from '@/types/auth'

export const loginByPassword = (data: LoginByPasswordRequest) => {
  return request<ApiResponse<LoginResponse>>({
    url: '/auth/login/password',
    method: 'POST',
    data,
  })
}

export const loginByCode = (data: LoginByCodeRequest) => {
  return request<ApiResponse<LoginResponse>>({
    url: '/auth/login/code',
    method: 'POST',
    data,
  })
}

export const register = (data: RegisterRequest) => {
  return request<ApiResponse<UserDTO>>({
    url: '/auth/register',
    method: 'POST',
    data,
  })
}

export const resetPassword = (data: ResetPasswordRequest) => {
  return request<ApiResponse<void>>({
    url: '/auth/reset-password',
    method: 'POST',
    data,
  })
}

export const sendVerificationCode = (data: SendVerificationCodeRequest) => {
  return request<ApiResponse<void>>({
    url: '/auth/send-code',
    method: 'POST',
    data,
  })
}

export const logout = () => {
  return request<ApiResponse<void>>({
    url: '/auth/logout',
    method: 'POST',
  })
}

export const getCurrentUser = () => {
  return request<ApiResponse<UserDTO>>({
    url: '/auth/me',
    method: 'GET',
  })
}

export const updateProfile = (data: UpdateProfileRequest) => {
  return request<ApiResponse<UserDTO>>({
    url: '/auth/profile',
    method: 'PUT',
    data,
  })
}

export const changePassword = (data: ChangePasswordRequest) => {
  return request<ApiResponse<void>>({
    url: '/auth/password',
    method: 'PUT',
    data,
  })
}
