import { request } from '@/utils/request'
import { getPriceByDate } from '@/utils/priceHelper'

// 房型数据结构
export interface RoomTypeDTO {
  id: number
  name: string
  code: string
  totalRooms: number
  maxGuests?: number
  description?: string
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
  createdAt: string
  updatedAt: string
}

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 获取所有房型
export const getAllRoomTypes = async (): Promise<ApiResponse<RoomTypeDTO[]>> => {
  return await request.get('/room-types')
}

// 获取所有房型(包含房间信息)
export const getAllRoomTypesWithRooms = async (): Promise<ApiResponse<any[]>> => {
  return await request.get('/room-types/with-rooms')
}

// 根据ID获取房型
export const getRoomTypeById = async (id: number): Promise<ApiResponse<RoomTypeDTO>> => {
  return await request.get(`/room-types/${id}`)
}

// 房间数据类型（用于获取房型信息）
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

// 根据房间ID获取房型价格信息
export const getRoomTypeByRoomId = async (roomId: number): Promise<ApiResponse<RoomTypeDTO>> => {
  try {
    // 获取所有房间信息
    const response: ApiResponse<any[]> = await request.get('/rooms')
    if (response.success && response.data) {
      // 查找指定ID的房间
      const room = response.data.find((r: any) => r.id === roomId)
      if (room && room.roomType) {
        // 将后端字段名映射到前端字段名
        const roomType = room.roomType
        const mappedRoomType: RoomTypeDTO = {
          id: roomType.id,
          name: roomType.name,
          code: roomType.code,
          totalRooms: roomType.totalRooms,
          maxGuests: roomType.maxGuests,
          description: roomType.description,
          defaultPrice: roomType.defaultPrice,
          weekdayPrice: roomType.weekdayPrice,
          weekendPrice: roomType.weekendPrice,
          monPrice: roomType.mondayPrice,
          tuePrice: roomType.tuesdayPrice,
          wedPrice: roomType.wednesdayPrice,
          thuPrice: roomType.thursdayPrice,
          friPrice: roomType.fridayPrice,
          satPrice: roomType.saturdayPrice,
          sunPrice: roomType.sundayPrice,
          createdAt: roomType.createdAt,
          updatedAt: roomType.updatedAt
        }

        return {
          success: true,
          message: '获取房型信息成功',
          data: mappedRoomType
        }
      } else {
        return {
          success: false,
          message: '未找到指定房间',
          data: null as any
        }
      }
    }
    return {
      success: false,
      message: response.message || '获取数据失败',
      data: null as any
    }
  } catch (error) {
    console.error('获取房型信息失败:', error)
    return {
      success: false,
      message: '获取房型信息失败',
      data: null as any
    }
  }
}

// 获取房间当前价格（根据日期的星期几获取对应价格）
export const getRoomCurrentPrice = (roomType: RoomTypeDTO, date: string): number => {
  return getPriceByDate(roomType, date)
}

// 获取房型的有效价格（从后端API获取，包含特定日期设置）
export const getEffectiveRoomPrice = async (roomTypeId: number, date: string): Promise<number> => {
  try {
    const response: ApiResponse<number> = await request.get(`/room-prices/${roomTypeId}/effective?date=${date}`)
    if (response.success) {
      return response.data || 0
    }
    return 0
  } catch (error) {
    console.error('获取有效房价失败:', error)
    return 0
  }
}

// 创建房型请求数据
export interface CreateRoomTypeRequest {
  name: string
  code: string
  description: string
  totalRooms: number
  maxGuests: number
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
  roomNumbers?: string[]  // 房间号列表
}

// 创建房型
export const createRoomType = async (data: CreateRoomTypeRequest): Promise<ApiResponse<RoomTypeDTO>> => {
  return await request.post('/room-types', data)
}

// 更新房型
export const updateRoomType = async (id: number, data: Partial<CreateRoomTypeRequest>): Promise<ApiResponse<RoomTypeDTO>> => {
  return await request.put(`/room-types/${id}`, data)
}

// 删除房型
export const deleteRoomType = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/room-types/${id}`)
}
