import { request } from '@/utils/request'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface HomeAnnouncementRequest {
  locale: string
  limit?: number
}

export interface HomeAnnouncementDTO {
  id: string | number
  scope?: string
  storeId?: number | null
  locale?: string | null
  title: string
  content?: string
  type?: string
  severity?: string
  active?: boolean
  sortOrder?: number
  startsAt?: string
  endsAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface AnnouncementManagementDTO {
  id: number
  scope: string
  storeId: number | null
  locale: string | null
  title: string
  content: string
  type: string
  severity: string
  active: boolean
  sortOrder: number
  startsAt: string | null
  endsAt: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface AnnouncementMutationPayload {
  locale: string | null
  title: string
  content: string
  type: string
  severity: string
  active: boolean
  sortOrder: number
  startsAt: string | null
  endsAt: string | null
}

export const getHomeAnnouncements = async (
  params: HomeAnnouncementRequest,
): Promise<ApiResponse<HomeAnnouncementDTO[]>> => {
  return await request.get('/announcements/home', { params })
}

export const listStoreAnnouncements = async (): Promise<
  ApiResponse<AnnouncementManagementDTO[]>
> => {
  return await request.get('/announcements')
}

export const createStoreAnnouncement = async (
  data: AnnouncementMutationPayload,
): Promise<ApiResponse<AnnouncementManagementDTO>> => {
  return await request.post('/announcements', data)
}

export const updateStoreAnnouncement = async (
  id: number,
  data: AnnouncementMutationPayload,
): Promise<ApiResponse<AnnouncementManagementDTO>> => {
  return await request.put(`/announcements/${id}`, data)
}

export const disableStoreAnnouncement = async (
  id: number,
): Promise<ApiResponse<AnnouncementManagementDTO>> => {
  return await request.delete(`/announcements/${id}`)
}
