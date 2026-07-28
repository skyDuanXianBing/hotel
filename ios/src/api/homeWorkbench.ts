import type { ApiResponse } from '@/types/api'
import request from '@/utils/request'

export type HomeWorkbenchTaskType = 'cleaning' | 'review' | 'order' | 'message' | 'other'
export type HomeWorkbenchTaskTypeFilter = 'all' | HomeWorkbenchTaskType

export interface HomeWorkbenchRequest {
  date: string
  size?: number
  type?: HomeWorkbenchTaskType
  status?: string
  cursor?: string
  includeSummaries?: boolean
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
  sourceId?: string | number
  sourceStatus?: string
  statusGroup?: string
  priority?: string
  title?: string
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
  typeSummaries: HomeWorkbenchTypeSummaryDTO[] | null
  statusSummaries: HomeWorkbenchStatusSummaryDTO[] | null
  items: HomeWorkbenchItemDTO[]
  summary?: {
    total?: number
    types?: HomeWorkbenchTypeSummaryDTO[]
    statuses?: HomeWorkbenchStatusSummaryDTO[]
  }
  total?: number
  page?: {
    size: number
    returnedElements: number
    totalElements: number | null
    nextCursor: string | null
    hasMore: boolean
  }
}

export const getHomeWorkbench = (params: HomeWorkbenchRequest, signal?: AbortSignal) => {
  return request.get<ApiResponse<HomeWorkbenchDTO>>('/home/workbench', {
    params: { ...params },
    signal,
    suppressErrorToast: true,
  })
}
