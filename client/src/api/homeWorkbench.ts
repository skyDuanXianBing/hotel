import { request } from '@/utils/request'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export type HomeWorkbenchTaskType = 'cleaning' | 'review' | 'order' | 'message' | 'other'

export interface HomeWorkbenchRequest {
  date: string
  limit?: number
}

export interface HomeWorkbenchTypeSummaryDTO {
  type: HomeWorkbenchTaskType
  count: number
  connected: boolean
}

export interface HomeWorkbenchStatusSummaryDTO {
  statusGroup: string
  count: number
}

export interface HomeWorkbenchMetaItemDTO {
  label?: string
  value?: string | number
}

export type HomeWorkbenchActionDTO =
  | string
  | {
      code?: string
      type?: string
      label?: string
      disabled?: boolean
    }

export interface HomeWorkbenchTargetDTO {
  name?: string
  routeName?: string
  path?: string
  routePath?: string
  params?: Record<string, string | number | boolean | null | undefined>
  query?: Record<string, string | number | boolean | null | undefined>
  reservationId?: string | number | null
  orderNumber?: string | null
  channelOrderNumber?: string | null
  guestName?: string | null
  suThreadId?: string | number | null
}

export interface HomeWorkbenchItemDTO {
  id: string
  type: HomeWorkbenchTaskType
  sourceType?: string
  sourceId?: string | number
  sourceStatus?: string
  statusGroup: string
  priority?: string
  dueAt?: string
  title: string
  subtitle?: string
  metaItems?: Array<string | HomeWorkbenchMetaItemDTO>
  target?: string | HomeWorkbenchTargetDTO | null
  actions?: HomeWorkbenchActionDTO[]
  assigneeId?: number | null
  assigneeName?: string | null
  unreadCount?: number | null
}

export interface HomeWorkbenchDTO {
  businessDate: string
  generatedAt: string
  typeSummaries?: HomeWorkbenchTypeSummaryDTO[]
  statusSummaries?: HomeWorkbenchStatusSummaryDTO[]
  items?: HomeWorkbenchItemDTO[]
}

export const getHomeWorkbench = async (
  params: HomeWorkbenchRequest,
): Promise<ApiResponse<HomeWorkbenchDTO>> => {
  return await request.get('/home/workbench', { params })
}
