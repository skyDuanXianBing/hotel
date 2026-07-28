import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export type InternalTaskStatus = 'UNASSIGNED' | 'ASSIGNED' | 'COMPLETED'

export interface InternalTaskDTO {
  id: number
  title: string
  description?: string | null
  status: InternalTaskStatus
  assigneeUserId?: number | null
  assigneeName?: string | null
  createdByUserId?: number | null
  createdByName?: string | null
  completedByUserId?: number | null
  completedByName?: string | null
  completedAt?: string | null
  archivedAt?: string | null
  createdAt: string
  updatedAt: string
  version?: number | null
  canComplete: boolean
  canManage: boolean
}

export const completeInternalTask = (taskId: number) => {
  return request.post<ApiResponse<InternalTaskDTO>>(`/internal-tasks/${taskId}/complete`)
}
