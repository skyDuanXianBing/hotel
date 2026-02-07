import { request } from '@/utils/request'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface RegistrationLinkInboxItemDTO {
  id: number
  bookingKey: string
  linkUrl: string
  guestName: string | null
  checkInDate: string | null
  checkOutDate: string | null
  roomCount: number
  createdAt: string
}

export const getRegistrationLinkInbox = async (): Promise<ApiResponse<RegistrationLinkInboxItemDTO[]>> => {
  return await request.get('/registrations/link-inbox')
}

