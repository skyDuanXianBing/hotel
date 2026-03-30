export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export type RequestMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export interface RequestConfig {
  url: string
  method?: RequestMethod
  params?: Record<string, string | number | boolean | null | undefined>
  data?: unknown
  headers?: Record<string, string>
  suppressErrorStatuses?: number[]
}
