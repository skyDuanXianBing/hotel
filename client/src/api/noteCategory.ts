import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 记一笔分类类型
export type NoteCategoryType = 'income' | 'expense'

// 记一笔分类DTO
export interface NoteCategoryDTO {
  id: number
  storeId: number
  name: string
  type: NoteCategoryType
  displayOrder: number
  createdAt: string
  updatedAt: string
}

// 创建/更新分类请求
export interface NoteCategoryRequest {
  name: string
  type: NoteCategoryType
  displayOrder?: number
}

// 批量更新排序请求
export interface UpdateOrderRequest {
  id: number
  displayOrder: number
}

/**
 * 获取所有分类
 */
export const getAllCategories = async (): Promise<ApiResponse<NoteCategoryDTO[]>> => {
  return await request.get('/note-categories')
}

/**
 * 按类型获取分类
 */
export const getCategoriesByType = async (
  type: NoteCategoryType
): Promise<ApiResponse<NoteCategoryDTO[]>> => {
  return await request.get(`/note-categories/type/${type}`)
}

/**
 * 创建分类
 */
export const createCategory = async (
  data: NoteCategoryRequest
): Promise<ApiResponse<NoteCategoryDTO>> => {
  return await request.post('/note-categories', data)
}

/**
 * 批量创建分类
 */
export const createCategories = async (
  data: NoteCategoryRequest[]
): Promise<ApiResponse<NoteCategoryDTO[]>> => {
  return await request.post('/note-categories/batch', data)
}

/**
 * 更新分类
 */
export const updateCategory = async (
  id: number,
  data: NoteCategoryRequest
): Promise<ApiResponse<NoteCategoryDTO>> => {
  return await request.put(`/note-categories/${id}`, data)
}

/**
 * 删除分类
 */
export const deleteCategory = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/note-categories/${id}`)
}

/**
 * 批量更新排序
 */
export const updateCategoriesOrder = async (
  data: UpdateOrderRequest[]
): Promise<ApiResponse<NoteCategoryDTO[]>> => {
  return await request.put('/note-categories/order', data)
}
