import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { NoteCategoryDTO, NoteCategoryRequest, NoteCategoryType } from '@/types/settings'

export const getAllCategories = () => {
  return request<ApiResponse<NoteCategoryDTO[]>>({
    url: '/note-categories',
    method: 'GET',
  })
}

export const getCategoriesByType = (type: NoteCategoryType) => {
  return request<ApiResponse<NoteCategoryDTO[]>>({
    url: `/note-categories/type/${type}`,
    method: 'GET',
  })
}

export const createCategory = (data: NoteCategoryRequest) => {
  return request<ApiResponse<NoteCategoryDTO>>({
    url: '/note-categories',
    method: 'POST',
    data,
  })
}

export const updateCategory = (id: number, data: NoteCategoryRequest) => {
  return request<ApiResponse<NoteCategoryDTO>>({
    url: `/note-categories/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteCategory = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/note-categories/${id}`,
    method: 'DELETE',
  })
}

export const updateCategoriesOrder = (data: Array<{ id: number; displayOrder: number }>) => {
  return request<ApiResponse<NoteCategoryDTO[]>>({
    url: '/note-categories/order',
    method: 'PUT',
    data,
  })
}
