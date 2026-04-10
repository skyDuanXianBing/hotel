import { request } from '@/utils/request'
import { getPriceByDate } from '@/utils/priceHelper'

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
  roomTypeAddress?: string
  nearbyStation?: string
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

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface RoomTypeDeleteBlockInfo {
  totalBlockingReservations: number
  sample: Array<{
    orderNumber: string
    status: string
    roomNumber: string
    checkInDate: string
    checkOutDate: string
  }>
}

export interface CreateRoomTypeRequest {
  name: string
  code: string
  description: string
  totalRooms: number
  maxGuests: number
  maxChildOccupancy?: number
  roomTypeAddress?: string
  nearbyStation?: string
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

interface RoomDTO {
  id: number
  roomNumber: string
  roomType: RoomTypeDTO
  floor?: number
  status: string
  notes?: string
  createdAt: string
  updatedAt: string
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

export const getAllRoomTypes = async (): Promise<ApiResponse<RoomTypeDTO[]>> => {
  const response: ApiResponse<RoomTypeDTO[]> = await request.get('/room-types')
  return response.success && response.data
    ? { ...response, data: response.data.map(normalizeRoomType) }
    : response
}

export const getAllRoomTypesWithRooms = async (): Promise<ApiResponse<any[]>> => {
  return await request.get('/room-types/with-rooms')
}

export const getRoomTypeById = async (id: number): Promise<ApiResponse<RoomTypeDTO>> => {
  const response: ApiResponse<RoomTypeDTO> = await request.get(`/room-types/${id}`)
  return response.success && response.data
    ? { ...response, data: normalizeRoomType(response.data) }
    : response
}

export const getRoomTypeByRoomId = async (roomId: number): Promise<ApiResponse<RoomTypeDTO>> => {
  try {
    const response: ApiResponse<RoomDTO[]> = await request.get('/rooms')
    if (!response.success || !response.data) {
      return {
        success: false,
        message: response.message || '获取房型信息失败',
        data: null as never,
      }
    }

    const room = response.data.find((item) => item.id === roomId)
    if (!room?.roomType) {
      return {
        success: false,
        message: '未找到指定房间',
        data: null as never,
      }
    }

    return {
      success: true,
      message: '获取房型信息成功',
      data: normalizeRoomType(room.roomType),
    }
  } catch (error) {
    console.error('获取房型信息失败:', error)
    return {
      success: false,
      message: '获取房型信息失败',
      data: null as never,
    }
  }
}

export const getRoomCurrentPrice = (roomType: RoomTypeDTO, date: string): number => {
  return getPriceByDate(roomType, date)
}

export const getEffectiveRoomPrice = async (roomTypeId: number, date: string): Promise<number> => {
  try {
    const response: ApiResponse<number> = await request.get(
      `/room-prices/${roomTypeId}/effective?date=${date}`
    )
    if (response.success) {
      return response.data || 0
    }
    return 0
  } catch (error) {
    console.error('获取有效房价失败:', error)
    return 0
  }
}

export const createRoomType = async (
  data: CreateRoomTypeRequest
): Promise<ApiResponse<RoomTypeDTO>> => {
  const response: ApiResponse<RoomTypeDTO> = await request.post('/room-types', data)
  return response.success && response.data
    ? { ...response, data: normalizeRoomType(response.data) }
    : response
}

export const updateRoomType = async (
  id: number,
  data: Partial<CreateRoomTypeRequest>
): Promise<ApiResponse<RoomTypeDTO>> => {
  const response: ApiResponse<RoomTypeDTO> = await request.put(`/room-types/${id}`, data)
  return response.success && response.data
    ? { ...response, data: normalizeRoomType(response.data) }
    : response
}

export const deleteRoomType = async (
  id: number
): Promise<ApiResponse<RoomTypeDeleteBlockInfo | null>> => {
  return await request.delete(`/room-types/${id}`)
}
