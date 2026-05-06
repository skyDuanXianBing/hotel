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
  reservationStatus: string | null
  createdAt: string
}

export const getRegistrationLinkInbox = async (
  reservationStatus?: string | null
): Promise<ApiResponse<RegistrationLinkInboxItemDTO[]>> => {
  const params = reservationStatus ? { reservationStatus } : undefined
  return await request.get('/registrations/link-inbox', { params })
}
