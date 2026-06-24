import type { StoreDTO } from '@/types/store'

export interface UserDTO {
  id: number
  email: string
  nickname: string
  avatar?: string
  gender?: 'male' | 'female' | 'private'
  createdAt: string
  updatedAt: string
}

export interface UpdateProfileRequest {
  nickname?: string
  gender?: 'male' | 'female' | 'private'
  avatar?: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface LoginByPasswordRequest {
  email: string
  password: string
  rememberMe?: boolean
}

export interface LoginByCodeRequest {
  email: string
  verificationCode: string
  rememberMe?: boolean
}

export type LoginTarget = 'PMS' | 'CLEANER'

export interface LoginResponse {
  token: string
  user: UserDTO
  stores: StoreDTO[]
  loginTarget: LoginTarget
  cleaner?: CleanerDTO | null
  currentStore?: StoreDTO | null
  targetStoreId?: number | null
}

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

export interface CleanerSessionUser {
  userId: number
  cleanerId: number
  email: string
  nickname: string
  avatar?: string
  gender?: 'male' | 'female' | 'private'
  createdAt?: string
  updatedAt?: string
  isCleaner: true
}

export interface CleanerInvitationInfo {
  id: number
  email: string
  name: string
  token: string
  userId: number
  storeId: number
  status: string
  expiresAt: string
  createdAt: string
  updatedAt: string
}

export interface CleanerInvitationRequest {
  email: string
  name: string
}

export interface CleanerRegistrationRequest {
  token: string
  name: string
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  verificationCode: string
  password: string
}

export interface ResetPasswordRequest {
  email: string
  verificationCode: string
  newPassword: string
}

export interface SendVerificationCodeRequest {
  email: string
  type: 'login' | 'register' | 'reset_password'
}
