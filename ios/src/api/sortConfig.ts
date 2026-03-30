import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { BatchSortConfigDTO, SortConfigDTO, SortEntityType } from '@/types/settings'

export const getSortConfigs = (userId: number, sortType: SortEntityType) => {
  return request<ApiResponse<SortConfigDTO[]>>({
    url: '/sort-configs',
    method: 'GET',
    params: { userId, sortType },
  })
}

export const getSortOrderMap = (userId: number, sortType: SortEntityType) => {
  return request<ApiResponse<Record<number, number>>>({
    url: '/sort-configs/map',
    method: 'GET',
    params: { userId, sortType },
  })
}

export const updateSortOrders = (userId: number, data: BatchSortConfigDTO) => {
  return request<ApiResponse<void>>({
    url: '/sort-configs/batch',
    method: 'POST',
    params: { userId },
    data,
  })
}

export const deleteSortConfigs = (userId: number, sortType: SortEntityType) => {
  return request<ApiResponse<void>>({
    url: '/sort-configs',
    method: 'DELETE',
    params: { userId, sortType },
  })
}
