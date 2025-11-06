import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

/**
 * 获取当前用户的备忘录内容
 */
export const getMemo = async (): Promise<ApiResponse<string>> => {
  return await request.get('/memo')
}

/**
 * 保存当前用户的备忘录内容
 */
export const saveMemo = async (content: string): Promise<ApiResponse<string>> => {
  return await request.post('/memo', { content })
}
