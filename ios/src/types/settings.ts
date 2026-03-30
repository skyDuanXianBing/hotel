import type { ApiResponse } from '@/types/api'

export type SortEntityType = 'ROOM_TYPE' | 'ROOM' | 'GROUP'

export interface RoomDTO {
  id: number
  roomNumber: string
  floor?: number
  status: string
  roomType: {
    id: number
    name: string
    code?: string
    totalRooms?: number
    description?: string
  }
}

export interface RoomGroupDTO {
  id?: number
  name: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface RoomGroupMemberDTO {
  id?: number
  groupId: number
  roomId: number
  createdAt?: string
}

export interface RoomGroupMemberBatchDTO {
  roomIds: number[]
}

export interface SortConfigDTO {
  id?: number
  userId: number
  sortType: SortEntityType
  entityId: number
  sortOrder: number
  createdAt?: string
  updatedAt?: string
}

export interface BatchSortConfigDTO {
  sortType: SortEntityType
  entityIds: number[]
}

export interface PaymentMethodDTO {
  id: number
  storeId: number
  name: string
  displayOrder: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface UpsertPaymentMethodRequest {
  name: string
  enabled?: boolean
}

export interface PaymentMethodOrderRequest {
  id: number
  displayOrder: number
}

export type NoteCategoryType = 'income' | 'expense'

export interface NoteCategoryDTO {
  id: number
  storeId: number
  name: string
  type: NoteCategoryType
  displayOrder: number
  createdAt: string
  updatedAt: string
}

export interface NoteCategoryRequest {
  name: string
  type: NoteCategoryType
  displayOrder?: number
}

export interface NotificationSettingDTO {
  id: number
  userId: number
  orderPopup: boolean
  orderSound: boolean
  chatPopup: boolean
  chatSound: boolean
  createdAt: string
  updatedAt: string
}

export interface NotificationSettingRequest {
  orderPopup: boolean
  orderSound: boolean
  chatPopup: boolean
  chatSound: boolean
}

export interface CreateChannelRequest {
  name: string
  code: string
  type: string
  color: string
  enabled?: boolean
  description?: string
}

export interface CleaningConfigDTO {
  id: number
  userId: number
  storeId: number
  enabled: boolean
  stayStartTime: string
  stayEndTime: string
  checkoutStartTime: string
  checkoutEndTime: string
  autoStayTask: boolean
  autoCheckoutTask: boolean
  createdAt: string
  updatedAt: string
}

export interface CleaningConfigRequest {
  enabled: boolean
  stayStartTime: string
  stayEndTime: string
  checkoutStartTime: string
  checkoutEndTime: string
  autoStayTask: boolean
  autoCheckoutTask: boolean
}

export interface CleanerDTO {
  id: number
  userId: number
  storeId: number
  name: string
  email: string
  createdAt: string
  updatedAt: string
}

export interface CleanerRequest {
  userId: number
  storeId: number
  name: string
  email: string
}

export interface CleaningSupplyDTO {
  id: number
  userId: number
  roomType: string
  supplies: string
  createdAt: string
  updatedAt: string
}

export interface CleaningSupplyRequest {
  roomType: string
  supplies: string
}

export interface StorePolicyDTO {
  id?: number
  storeId?: number
  checkinTime?: string
  checkoutTime?: string
  childPolicy?: string
  smokingPolicy?: string
  petPolicy?: string
  additionalRules?: string
  hotelTerms?: string
}

export interface SelectOption {
  label: string
  value: string
}

export type SettingsApiResponse<T> = ApiResponse<T>
