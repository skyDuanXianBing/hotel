import type { PermissionDTO } from '@/api/role'

export interface LocalizedContentDTO {
  name?: string
  description?: string
}

export interface FacilityDTO {
  group?: string
  name: string
}

export interface StoreDTO {
  id: number
  name: string
  phone: string
  phoneTechType?: string
  type: string
  timezone: string
  manager: string
  ownerEmail: string
  address: string
  city: string
  state: string
  country: string
  currency: string
  suHotelId?: string
  logo?: string
  description?: string
  email?: string
  wechat?: string
  whatsapp?: string
  line?: string
  language?: string
  facilities?: FacilityDTO[]
  desktopPhotoUrls?: string[]
  mobilePhotoUrls?: string[]
  localizedContent?: Record<string, LocalizedContentDTO>
  checkinTime?: string
  checkoutTime?: string
  userRole: string
  createdAt: string
  updatedAt: string
}

export interface StoreRequest {
  name: string
  phone: string
  phoneTechType?: string
  type?: string
  timezone?: string
  manager?: string
  country: string
  city?: string
  state?: string
  address?: string
  currency?: string
  suHotelId?: string
  createSuProperty?: boolean
  ownerEmail?: string
  language?: string
  description?: string
  logo?: string
  email?: string
  wechat?: string
  whatsapp?: string
  line?: string
  facilities?: FacilityDTO[]
  desktopPhotoUrls?: string[]
  mobilePhotoUrls?: string[]
  localizedContent?: Record<string, LocalizedContentDTO>
  checkinTime?: string
  checkoutTime?: string
}

export interface UserSimpleDTO {
  id: number
  username: string
  email: string
  nickname?: string
  avatar?: string
  isActive: boolean
}

export interface RoleDTO {
  id: number
  name: string
  description?: string
  isSystem?: boolean
}

export interface StoreUserDTO {
  id: number
  user: UserSimpleDTO
  role: string
  roles: RoleDTO[]
  extraPermissions?: PermissionDTO[]
  isActive: boolean
  invitedBy?: number
  joinedAt: string
}

export type StoreMember = StoreUserDTO
