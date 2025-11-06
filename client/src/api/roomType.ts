import { request } from '@/utils/request'

// 房型数据结构
export interface RoomTypeDTO {
  id: number
  name: string
  code: string
  totalRooms: number
  description?: string
  defaultPrice?: number
  weekdayPrice?: number
  weekendPrice?: number
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
    const response: ApiResponse<RoomDTO[]> = await request.get('/rooms')
    if (response.success && response.data) {
      // 查找指定ID的房间
      const room = response.data.find((r: RoomDTO) => r.id === roomId)
      if (room && room.roomType) {
        return {
          success: true,
          message: '获取房型信息成功',
          data: room.roomType
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

// 获取房间当前价格（根据日期判断工作日/周末）
export const getRoomCurrentPrice = (roomType: RoomTypeDTO, date: string): number => {
  const targetDate = new Date(date)
  const dayOfWeek = targetDate.getDay() // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
  
  // 如果是周末（周六=6 或 周日=0），优先使用周末价格
  if (dayOfWeek === 0 || dayOfWeek === 6) {
    return roomType.weekendPrice || roomType.defaultPrice || 0
  }
  
  // 工作日价格
  return roomType.weekdayPrice || roomType.defaultPrice || 0
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