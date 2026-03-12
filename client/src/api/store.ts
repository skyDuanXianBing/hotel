import { request } from '@/utils/request'
import type { PermissionDTO } from '@/api/role'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

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
  userRole: string
  createdAt: string
  updatedAt: string
}

export interface StorePolicyDTO {
  id?: number
  storeId: number
  checkinTime: string
  checkoutTime: string
  childPolicy: string
  smokingPolicy: string
  petPolicy: string
  additionalRules: string
  hotelTerms: string
}

export interface StoreRequest {
  name: string
  phone: string
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
}

export interface AddStoreMemberRequest {
  email: string
  role: 'owner' | 'admin' | 'member'
  roleIds?: number[]
  extraPermissions?: PermissionDTO[]
}

export interface UpdateStoreMemberPermissionRequest {
  role?: 'owner' | 'admin' | 'member'
  roleIds?: number[]
  isActive?: boolean
  extraPermissions?: PermissionDTO[]
}

export interface TransferStoreOwnerRequest {
  targetUserId: number
}

export interface RoleDTO {
  id: number
  name: string
  description?: string
  isSystem?: boolean
}

export interface UserSimpleDTO {
  id: number
  username: string
  email: string
  nickname?: string
  avatar?: string
  isActive: boolean
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

export interface DeleteStoreErrorData {
  code?: string
}

export const getUserStores = async (): Promise<ApiResponse<StoreDTO[]>> => {
  return await request.get('/stores')
}

export const getStoreById = async (id: number): Promise<ApiResponse<StoreDTO>> => {
  return await request.get(`/stores/${id}`)
}

export const createStore = async (data: StoreRequest): Promise<ApiResponse<StoreDTO>> => {
  return await request.post('/stores', data)
}

export const updateStore = async (
  id: number,
  data: StoreRequest
): Promise<ApiResponse<StoreDTO>> => {
  return await request.put(`/stores/${id}`, data)
}

export const deleteStore = async (
  id: number
): Promise<ApiResponse<DeleteStoreErrorData | null>> => {
  return await request.delete(`/stores/${id}`)
}

export const addStoreMember = async (
  storeId: number,
  data: AddStoreMemberRequest
): Promise<ApiResponse<StoreUserDTO>> => {
  return await request.post(`/stores/${storeId}/members`, data)
}

export const getStoreMembers = async (
  storeId: number
): Promise<ApiResponse<StoreUserDTO[]>> => {
  return await request.get(`/stores/${storeId}/members`)
}

export const getStoreMemberDetail = async (
  storeId: number,
  userId: number
): Promise<ApiResponse<StoreUserDTO>> => {
  return await request.get(`/stores/${storeId}/members/${userId}`)
}

export const updateStoreMemberPermission = async (
  storeId: number,
  userId: number,
  data: UpdateStoreMemberPermissionRequest
): Promise<ApiResponse<StoreUserDTO>> => {
  return await request.put(`/stores/${storeId}/members/${userId}`, data)
}

export const transferStoreOwner = async (
  storeId: number,
  data: TransferStoreOwnerRequest
): Promise<ApiResponse<void>> => {
  return await request.post(`/stores/${storeId}/owner/transfer`, data)
}

export const removeStoreMember = async (
  storeId: number,
  memberId: number
): Promise<ApiResponse<void>> => {
  return await request.delete(`/stores/${storeId}/members/${memberId}`)
}

export const getStorePolicy = async (
  storeId: number
): Promise<ApiResponse<StorePolicyDTO>> => {
  return await request.get(`/stores/${storeId}/policy`)
}

export const saveStorePolicy = async (
  storeId: number,
  data: Partial<StorePolicyDTO>
): Promise<ApiResponse<StorePolicyDTO>> => {
  return await request.put(`/stores/${storeId}/policy`, data)
}
