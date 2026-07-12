import { request } from '@/utils/request'

export type InternalTaskStatus = 'UNASSIGNED' | 'ASSIGNED' | 'COMPLETED'

export interface InternalTaskDTO {
  id: number
  title: string
  description?: string | null
  status: InternalTaskStatus
  assigneeUserId?: number | null
  assigneeName?: string | null
  createdByUserId: number
  createdByName?: string | null
  completedByUserId?: number | null
  completedByName?: string | null
  completedAt?: string | null
  archivedAt?: string | null
  createdAt: string
  updatedAt: string
  version: number
  canComplete: boolean
  canManage: boolean
}

export interface InternalTaskPageDTO {
  items: InternalTaskDTO[]
  page: number
  size: number
  totalElements: number
  assignedCount: number
  completedCount: number
}

export interface InternalTaskAssigneeDTO {
  userId: number
  displayName: string
  avatar?: string | null
  baseRole?: string | null
  employeeType?: 'CLEANER' | 'PMS' | 'BOTH' | string
  cleanerId?: number | null
}

export interface InternalTaskCreateRequest {
  title: string
  description?: string
  assigneeUserId?: number
}

export interface InternalTaskAssignRequest {
  assigneeUserId?: number
  version?: number
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface InternalTaskListParams {
  status?: InternalTaskStatus
  page?: number
  size?: number
}

export const getInternalTasks = async (
  params: InternalTaskListParams = {},
): Promise<ApiResponse<InternalTaskPageDTO>> =>
  await request.get('/internal-tasks', { params, suppressErrorToast: true })

export const getMyInternalTasks = async (
  params: Required<Pick<InternalTaskListParams, 'status'>> & InternalTaskListParams,
): Promise<ApiResponse<InternalTaskPageDTO>> =>
  await request.get('/internal-tasks/mine', { params, suppressErrorToast: true })

export const getInternalTaskAssignees = async (): Promise<
  ApiResponse<InternalTaskAssigneeDTO[]>
> => await request.get('/internal-tasks/assignees', { suppressErrorToast: true })

export const createInternalTask = async (
  data: InternalTaskCreateRequest,
): Promise<ApiResponse<InternalTaskDTO>> =>
  await request.post('/internal-tasks', data, { suppressErrorToast: true })

export const assignInternalTask = async (
  id: number,
  data: InternalTaskAssignRequest,
): Promise<ApiResponse<InternalTaskDTO>> =>
  await request.put(`/internal-tasks/${id}/assignee`, data, { suppressErrorToast: true })

export const completeInternalTask = async (id: number): Promise<ApiResponse<InternalTaskDTO>> =>
  await request.post(`/internal-tasks/${id}/complete`, undefined, { suppressErrorToast: true })

export const archiveInternalTask = async (id: number): Promise<ApiResponse<InternalTaskDTO>> =>
  await request.post(`/internal-tasks/${id}/archive`, undefined, { suppressErrorToast: true })
