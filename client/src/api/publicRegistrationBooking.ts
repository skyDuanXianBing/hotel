import publicRequest from '@/utils/publicRequest'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export type RegistrationFormStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'

export interface PublicRegistrationBookingRoomDTO {
  orderNumber: string
  storeName: string
  roomTypeName: string
  roomNumber: string
  checkInDate: string
  checkOutDate: string
  maxGuests: number
  guestCount: number
  status: RegistrationFormStatus
  lastSavedAt: string | null
  roomRegistrationLink: string
}

export interface PublicRegistrationBookingResponse {
  bookingKey: string
  guestName: string
  checkInDate: string
  checkOutDate: string
  rooms: PublicRegistrationBookingRoomDTO[]
}

export const getPublicRegistrationBooking = async (
  bookingKey: string,
  token: string
): Promise<ApiResponse<PublicRegistrationBookingResponse>> => {
  return await publicRequest.get(`/public/registration-booking/${encodeURIComponent(bookingKey)}`, {
    params: { t: token },
  })
}

export interface PublicRegistrationSaveRequest {
  guestCount?: number
  guests?: unknown[]
}

export interface PublicRegistrationResponse {
  orderNumber: string
  status: RegistrationFormStatus
  maxGuests: number
  guestCount: number
}

export const setRoomGuestCountFromBooking = async (
  bookingKey: string,
  orderNumber: string,
  token: string,
  guestCount: number
): Promise<ApiResponse<PublicRegistrationResponse>> => {
  return await publicRequest.put(
    `/public/registration-booking/${encodeURIComponent(bookingKey)}/rooms/${encodeURIComponent(orderNumber)}/guest-count`,
    { guestCount } satisfies PublicRegistrationSaveRequest,
    { params: { t: token } }
  )
}
