import type { ApiResponse } from '@/types/api'
import type {
  PublicRegistrationBookingResponse,
  PublicRegistrationResponse,
  PublicRegistrationSaveRequest,
} from '@/types/publicRegistration'
import publicRequest from '@/utils/publicRequest'

export const getPublicRegistrationBooking = (bookingKey: string, token: string) => {
  return publicRequest.get<ApiResponse<PublicRegistrationBookingResponse>>(
    `/public/registration-booking/${encodeURIComponent(bookingKey)}`,
    {
      params: { t: token },
    },
  )
}

export const setPublicRegistrationRoomGuestCount = (
  bookingKey: string,
  orderNumber: string,
  token: string,
  guestCount: number,
) => {
  const payload: PublicRegistrationSaveRequest = {
    guestCount,
  }

  return publicRequest.put<ApiResponse<PublicRegistrationResponse>>(
    `/public/registration-booking/${encodeURIComponent(bookingKey)}/rooms/${encodeURIComponent(orderNumber)}/guest-count`,
    payload,
    {
      params: { t: token },
    },
  )
}
