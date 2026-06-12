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
  title: string
  content?: string
  type?: string
  severity?: string
  startsAt?: string
  endsAt?: string
  createdAt?: string
  updatedAt?: string
}

export const getHomeAnnouncements = async (
  params: HomeAnnouncementRequest,
): Promise<ApiResponse<HomeAnnouncementDTO[]>> => {
  return await request.get('/announcements/home', { params })
}
