import type { SupportedLocale } from '@/locales'

export type PublicRegistrationLanguage = SupportedLocale

export type RegistrationFormStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'

export type ResidenceType = 'JAPAN' | 'OTHER'

export type RegistrationAttachmentType = 'PASSPORT' | string

export interface PublicRegistrationBookingRoom {
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
  rooms: PublicRegistrationBookingRoom[]
}

export interface PublicRegistrationAttachment {
  id: number
  guestId?: number
  type: RegistrationAttachmentType
  originalName?: string
}

export interface PublicRegistrationGuest {
  id?: number
  sortOrder?: number
  lastName?: string
  firstName?: string
  lastNameKana?: string
  firstNameKana?: string
  gender?: string
  birthday?: string
  nationality?: string
  residenceType?: ResidenceType
  address?: string
  address1?: string
  address2?: string
  city?: string
  state?: string
  country?: string
  phone?: string
  email?: string
  passportNumber?: string
  priorStay?: string
  nextDestination?: string
}

export interface PublicRegistrationResponse {
  formId: number
  orderNumber: string
  bookingKey: string
  status: RegistrationFormStatus
  checkInDate: string
  checkOutDate: string
  guestName: string
  adults: number
  children: number
  maxGuests: number
  guestCount: number
  lastSavedAt?: string | null
  checkInGuideLink?: string | null
  guests: PublicRegistrationGuest[]
  attachments?: PublicRegistrationAttachment[]
}

export interface PublicRegistrationSaveRequest {
  guestCount?: number
  guests?: PublicRegistrationGuest[]
}
