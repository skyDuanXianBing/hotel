import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface LocalizedContentDTO {
  name?: string
  description?: string
}

export interface FacilityDTO {
  group?: string
  name: string
}

export interface RoomTypeDTO {
  id: number
  name: string
  code: string
  totalRooms: number
  maxGuests?: number
  maxChildOccupancy?: number
  description?: string
  checkInGuideLink?: string
  suRoomType?: string
  sizeMeasurement?: number
  sizeMeasurementUnit?: string
  defaultPrice?: number
  weekdayPrice?: number
  weekendPrice?: number
  monPrice?: number
  tuePrice?: number
  wedPrice?: number
  thuPrice?: number
  friPrice?: number
  satPrice?: number
  sunPrice?: number
  mondayPrice?: number
  tuesdayPrice?: number
  wednesdayPrice?: number
  thursdayPrice?: number
  fridayPrice?: number
  saturdayPrice?: number
  sundayPrice?: number
  facilities?: FacilityDTO[]
  desktopPhotoUrls?: string[]
  mobilePhotoUrls?: string[]
  localizedContent?: Record<string, LocalizedContentDTO>
  createdAt: string
  updatedAt: string
}

export interface RoomTypeRoomDTO {
  id: number
  roomNumber: string
  smartlockPasscode?: string
  status: string
  floor?: number
  notes?: string
}

export interface RoomTypeWithRoomsDTO extends RoomTypeDTO {
  rooms: RoomTypeRoomDTO[]
}

export interface RoomTypeDeleteBlockReservationDTO {
  orderNumber: string
  status: string
  roomNumber: string
  checkInDate: string
  checkOutDate: string
}

export interface RoomTypeDeleteBlockInfo {
  totalBlockingReservations: number
  sample: RoomTypeDeleteBlockReservationDTO[]
}

export interface CreateRoomTypeRequest {
  name: string
  code: string
  description?: string
  totalRooms: number
  maxGuests: number
  maxChildOccupancy?: number
  checkInGuideLink?: string
  suRoomType?: string
  sizeMeasurement?: number
  sizeMeasurementUnit?: string
  defaultPrice?: number
  weekdayPrice?: number
  weekendPrice?: number
  mondayPrice?: number
  tuesdayPrice?: number
  wednesdayPrice?: number
  thursdayPrice?: number
  fridayPrice?: number
  saturdayPrice?: number
  sundayPrice?: number
  roomNumbers?: string[]
  rooms?: Array<{ roomNumber: string; smartlockPasscode?: string }>
  facilities?: FacilityDTO[]
  desktopPhotoUrls?: string[]
  mobilePhotoUrls?: string[]
  localizedContent?: Record<string, LocalizedContentDTO>
}

const normalizeRoomType = (roomType: RoomTypeDTO): RoomTypeDTO => {
  return {
    ...roomType,
    mondayPrice: roomType.mondayPrice ?? roomType.monPrice,
    tuesdayPrice: roomType.tuesdayPrice ?? roomType.tuePrice,
    wednesdayPrice: roomType.wednesdayPrice ?? roomType.wedPrice,
    thursdayPrice: roomType.thursdayPrice ?? roomType.thuPrice,
    fridayPrice: roomType.fridayPrice ?? roomType.friPrice,
    saturdayPrice: roomType.saturdayPrice ?? roomType.satPrice,
    sundayPrice: roomType.sundayPrice ?? roomType.sunPrice,
    monPrice: roomType.monPrice ?? roomType.mondayPrice,
    tuePrice: roomType.tuePrice ?? roomType.tuesdayPrice,
    wedPrice: roomType.wedPrice ?? roomType.wednesdayPrice,
    thuPrice: roomType.thuPrice ?? roomType.thursdayPrice,
    friPrice: roomType.friPrice ?? roomType.fridayPrice,
    satPrice: roomType.satPrice ?? roomType.saturdayPrice,
    sunPrice: roomType.sunPrice ?? roomType.sundayPrice,
  }
}

const normalizeRoomTypeWithRooms = (roomType: RoomTypeWithRoomsDTO): RoomTypeWithRoomsDTO => {
  const normalizedRooms: RoomTypeRoomDTO[] = []

  for (const room of roomType.rooms || []) {
    normalizedRooms.push({
      ...room,
      roomNumber: room.roomNumber ?? '',
      smartlockPasscode: room.smartlockPasscode ?? undefined,
    })
  }

  return {
    ...normalizeRoomType(roomType),
    rooms: normalizedRooms,
  }
}

export const getAllRoomTypes = async () => {
  const response = await request.get<ApiResponse<RoomTypeDTO[]>>('/room-types')
  if (!response.success || !response.data) {
    return response
  }

  return {
    ...response,
    data: response.data.map((item) => normalizeRoomType(item)),
  }
}

export const getRoomTypeByRoomId = async (roomId: number) => {
  const response = await request.get<ApiResponse<RoomTypeDTO>>(`/rooms/${roomId}/room-type`)
  if (!response.success || !response.data) {
    return response
  }

  return {
    ...response,
    data: normalizeRoomType(response.data),
  }
}

export const getAllRoomTypesWithRooms = async () => {
  const response = await request.get<ApiResponse<RoomTypeWithRoomsDTO[]>>('/room-types/with-rooms')
  if (!response.success || !response.data) {
    return response
  }

  return {
    ...response,
    data: response.data.map((item) => normalizeRoomTypeWithRooms(item)),
  }
}

export const getRoomTypeById = async (id: number) => {
  const response = await request.get<ApiResponse<RoomTypeDTO>>(`/room-types/${id}`)
  if (!response.success || !response.data) {
    return response
  }

  return {
    ...response,
    data: normalizeRoomType(response.data),
  }
}

export const createRoomType = async (data: CreateRoomTypeRequest) => {
  const response = await request.post<ApiResponse<RoomTypeDTO>>('/room-types', data)
  if (!response.success || !response.data) {
    return response
  }

  return {
    ...response,
    data: normalizeRoomType(response.data),
  }
}

export const updateRoomType = async (id: number, data: CreateRoomTypeRequest) => {
  const response = await request.put<ApiResponse<RoomTypeDTO>>(`/room-types/${id}`, data)
  if (!response.success || !response.data) {
    return response
  }

  return {
    ...response,
    data: normalizeRoomType(response.data),
  }
}

export const deleteRoomType = async (id: number) => {
  return await request.delete<ApiResponse<RoomTypeDeleteBlockInfo | null>>(`/room-types/${id}`)
}

export const getRoomCurrentPrice = (roomType: RoomTypeDTO, date: string) => {
  const currentDate = new Date(date)
  if (Number.isNaN(currentDate.getTime())) {
    return Number(roomType.defaultPrice ?? 0)
  }

  const day = currentDate.getDay()
  if (day === 1 && roomType.mondayPrice !== undefined) {
    return Number(roomType.mondayPrice)
  }
  if (day === 2 && roomType.tuesdayPrice !== undefined) {
    return Number(roomType.tuesdayPrice)
  }
  if (day === 3 && roomType.wednesdayPrice !== undefined) {
    return Number(roomType.wednesdayPrice)
  }
  if (day === 4 && roomType.thursdayPrice !== undefined) {
    return Number(roomType.thursdayPrice)
  }
  if (day === 5 && roomType.fridayPrice !== undefined) {
    return Number(roomType.fridayPrice)
  }
  if (day === 6 && roomType.saturdayPrice !== undefined) {
    return Number(roomType.saturdayPrice)
  }
  if (day === 0 && roomType.sundayPrice !== undefined) {
    return Number(roomType.sundayPrice)
  }

  return Number(roomType.defaultPrice ?? roomType.weekdayPrice ?? roomType.weekendPrice ?? 0)
}

export const calculateTotalPriceByDates = (
  roomType: RoomTypeDTO,
  checkInDate: string,
  checkOutDate: string,
) => {
  const start = new Date(checkInDate)
  const end = new Date(checkOutDate)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start >= end) {
    return 0
  }

  const cursor = new Date(start)
  let totalAmount = 0

  while (cursor < end) {
    const currentDate = cursor.toISOString().split('T')[0]
    totalAmount += getRoomCurrentPrice(roomType, currentDate)
    cursor.setDate(cursor.getDate() + 1)
  }

  return Number(totalAmount.toFixed(2))
}
