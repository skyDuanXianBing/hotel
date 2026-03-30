import type { ApiResponse } from '@/types/api'
import type { CleanerInvitationInfo, CleanerRegistrationRequest } from '@/types/auth'
import type {
  CleanerRequest,
  CleaningConfigDTO,
  CleaningConfigRequest,
  CleaningSupplyDTO,
  CleaningSupplyRequest,
} from '@/types/settings'
import request from '@/utils/request'

export interface CleanerDTO {
  id: number
  userId: number
  storeId: number
  name: string
  email: string
  createdAt: string
  updatedAt: string
}

export interface CleaningTaskDTO {
  id: number
  taskDate: string
  roomId: number
  roomNumber: string
  roomType: string
  roomTypeId: number
  taskType: string
  status: string
  cleanerId?: number
  cleanerName?: string
  estimatedTime?: string
  startTime?: string
  completeTime?: string
  approverId?: number
  approverName?: string
  notes?: string
  reservationId?: number
  source?: string
  createdAt: string
  updatedAt: string
}

export interface CleaningTaskCreateDTO {
  taskDate: string
  roomId: number
  taskType: string
  cleanerId?: number
  estimatedTime?: string
  notes?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface CalendarViewData {
  tasks: Record<string, CleaningTaskDTO[]>
  totalCount: number
  statusCount: Record<string, number>
}

export interface CleaningTaskGenerateResult {
  processedReservations: number
  createdCount: number
  updatedCount: number
  skippedCount: number
}

export interface CleaningTaskListParams {
  startDate?: string
  endDate?: string
  status?: string
  taskType?: string
  roomId?: number
  cleanerId?: number
  roomTypeId?: number
  search?: string
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: string
}

export interface CleaningCalendarParams {
  startDate: string
  endDate: string
  status?: string
  cleanerId?: number
}

export const validateInvitationToken = (token: string) => {
  return request.get<ApiResponse<CleanerInvitationInfo>>(`/cleaner-invitations/validate/${token}`)
}

export const registerCleaner = (data: CleanerRegistrationRequest) => {
  return request.post<ApiResponse<CleanerDTO>>('/cleaner-invitations/register', data)
}

export const getCleaningConfigsByUserId = (userId: number) => {
  return request<ApiResponse<CleaningConfigDTO[]>>({
    url: `/cleaning-configs/user/${userId}`,
    method: 'GET',
  })
}

export const getOrCreateCleaningConfig = (userId: number, storeId: number) => {
  return request<ApiResponse<CleaningConfigDTO>>({
    url: `/cleaning-configs/user/${userId}/store/${storeId}`,
    method: 'GET',
  })
}

export const updateCleaningConfig = (id: number, data: CleaningConfigRequest) => {
  return request<ApiResponse<CleaningConfigDTO>>({
    url: `/cleaning-configs/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteCleaningConfig = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/cleaning-configs/${id}`,
    method: 'DELETE',
  })
}

export const getCleaners = () => {
  return request.get<ApiResponse<CleanerDTO[]>>('/cleaners')
}

export const createCleaner = (data: CleanerRequest) => {
  return request<ApiResponse<CleanerDTO>>({
    url: '/cleaners',
    method: 'POST',
    data,
  })
}

export const updateCleaner = (id: number, data: CleanerRequest) => {
  return request<ApiResponse<CleanerDTO>>({
    url: `/cleaners/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteCleaner = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/cleaners/${id}`,
    method: 'DELETE',
  })
}

export const getAllCleaningSupplies = () => {
  return request<ApiResponse<CleaningSupplyDTO[]>>({
    url: '/cleaning-supplies',
    method: 'GET',
  })
}

export const createCleaningSupply = (data: CleaningSupplyRequest) => {
  return request<ApiResponse<CleaningSupplyDTO>>({
    url: '/cleaning-supplies',
    method: 'POST',
    data,
  })
}

export const updateCleaningSupply = (id: number, data: CleaningSupplyRequest) => {
  return request<ApiResponse<CleaningSupplyDTO>>({
    url: `/cleaning-supplies/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteCleaningSupply = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/cleaning-supplies/${id}`,
    method: 'DELETE',
  })
}

export const clearCleaningSupply = (id: number) => {
  return request<ApiResponse<CleaningSupplyDTO>>({
    url: `/cleaning-supplies/${id}/clear`,
    method: 'PUT',
  })
}

export const getCleaningTasks = (params: CleaningTaskListParams) => {
  return request.get<ApiResponse<PageResponse<CleaningTaskDTO>>>('/cleaning-tasks', {
    params: { ...params },
  })
}

export const getCleaningTaskById = (taskId: number) => {
  return request.get<ApiResponse<CleaningTaskDTO>>(`/cleaning-tasks/${taskId}`)
}

export const getCalendarViewData = (params: CleaningCalendarParams) => {
  return request.get<ApiResponse<CalendarViewData>>('/cleaning-tasks/calendar', {
    params: { ...params },
  })
}

export const generateCleaningTasks = (startDate: string, endDate: string) => {
  return request.post<ApiResponse<CleaningTaskGenerateResult>>('/cleaning-tasks/generate', null, {
    params: { startDate, endDate },
  })
}

export const assignCleaningTask = (taskId: number, cleanerId: number) => {
  return request.post<ApiResponse<CleaningTaskDTO>>(`/cleaning-tasks/${taskId}/assign`, null, {
    params: { cleanerId },
  })
}

export const acceptCleaningTask = (taskId: number) => {
  return request.post<ApiResponse<CleaningTaskDTO>>(`/cleaning-tasks/${taskId}/accept`)
}

export const rejectCleaningTask = (taskId: number) => {
  return request.post<ApiResponse<CleaningTaskDTO>>(`/cleaning-tasks/${taskId}/reject`)
}

export const completeCleaningTask = (taskId: number, approverId: number) => {
  return request.post<ApiResponse<CleaningTaskDTO>>(`/cleaning-tasks/${taskId}/complete`, null, {
    params: { approverId },
  })
}

export const createCleaningTask = (data: CleaningTaskCreateDTO) => {
  return request.post<ApiResponse<CleaningTaskDTO>>('/cleaning-tasks', data)
}
