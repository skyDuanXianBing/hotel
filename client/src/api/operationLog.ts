import { request } from '@/utils/request'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface OperationLogDetailDTO {
  label: string
  value: string
}

export interface OperationLogDTO {
  id: number
  action: string
  operator: string
  timestamp: string
  type: 'order' | 'billing'
  content?: string
  details?: OperationLogDetailDTO[]
}

export const getOperationLogsByReservationId = async (
  reservationId: number,
): Promise<ApiResponse<OperationLogDTO[]>> => {
  return await request.get(`/operation-logs/reservation/${reservationId}`)
}

