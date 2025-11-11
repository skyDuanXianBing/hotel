import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface SortConfigDTO {
  id?: number
  userId: number
  sortType: string
  entityId: number
  sortOrder: number
  createdAt?: string
  updatedAt?: string
}

export interface BatchSortConfigDTO {
  sortType: string
  entityIds: number[]
}

/**
 * 获取指定类型的排序配置列表
 */
export const getSortConfigs = async (
  userId: number,
  sortType: string
): Promise<ApiResponse<SortConfigDTO[]>> => {
  return await request.get('/sort-configs', {
    params: { userId, sortType },
  })
}

/**
 * 获取排序映射(entityId -> sortOrder)
 */
export const getSortOrderMap = async (
  userId: number,
  sortType: string
): Promise<ApiResponse<Record<number, number>>> => {
  return await request.get('/sort-configs/map', {
    params: { userId, sortType },
  })
}

/**
 * 批量更新排序配置
 */
export const updateSortOrders = async (
  userId: number,
  data: BatchSortConfigDTO
): Promise<ApiResponse<void>> => {
  return await request.post('/sort-configs/batch', data, {
    params: { userId },
  })
}

/**
 * 删除指定类型的所有排序配置
 */
export const deleteSortConfigs = async (
  userId: number,
  sortType: string
): Promise<ApiResponse<void>> => {
  return await request.delete('/sort-configs', {
    params: { userId, sortType },
  })
}
