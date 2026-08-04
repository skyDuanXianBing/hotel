import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 营业汇总查询参数
export interface BusinessQueryParams {
  startDate: string // YYYY-MM-DD
  endDate: string // YYYY-MM-DD
}

// 每日入住率DTO
export interface DailyOccupancyDTO {
  date: string
  rate: number
  occupiedRooms: number
  totalRooms: number
}

// 获取每日入住率统计
export const getDailyOccupancy = async (
  params: BusinessQueryParams
): Promise<ApiResponse<DailyOccupancyDTO[]>> => {
  return await request.get('/statistics/business/daily-occupancy', { params })
}