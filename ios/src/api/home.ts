import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface HomeStatisticsDTO {
  date: string
  todayArrivals: number
  todayDepartures: number
  todayNewOrders: number
  availableRooms: number
  unassignedOrders: number
  pendingOrders: number
}

export interface HomeStatisticsQueryParams {
  date?: string
}

export interface BusinessQueryParams {
  startDate: string
  endDate: string
}

export interface DailyOccupancyDTO {
  date: string
  rate: number
  occupiedRooms: number
  totalRooms: number
}

export interface MemoSaveRequest {
  content: string
}

export const getHomeStatistics = (params?: HomeStatisticsQueryParams) => {
  return request<ApiResponse<HomeStatisticsDTO>>({
    url: '/room-status/statistics',
    method: 'GET',
    params: params ? { ...params } : undefined,
  })
}

export const getDailyOccupancy = (params: BusinessQueryParams) => {
  return request<ApiResponse<DailyOccupancyDTO[]>>({
    url: '/statistics/business/daily-occupancy',
    method: 'GET',
    params: { ...params },
  })
}

export const getMemo = () => {
  return request<ApiResponse<string>>({
    url: '/memo',
    method: 'GET',
  })
}

export const saveMemo = (content: string) => {
  const data: MemoSaveRequest = { content }

  return request<ApiResponse<string>>({
    url: '/memo',
    method: 'POST',
    data,
  })
}

const homeApi = {
  getHomeStatistics,
  getDailyOccupancy,
  getMemo,
  saveMemo,
}

export default homeApi
