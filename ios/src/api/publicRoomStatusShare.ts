import type { ApiResponse } from '@/types/api'
import type {
  PublicRoomStatusCalendar,
  PublicRoomStatusShare,
  PublicRoomStatusStatistics,
} from '@/types/publicRoomStatusShare'
import publicRequest from '@/utils/publicRequest'

export const getPublicRoomStatusShare = (shareToken: string) => {
  return publicRequest.get<ApiResponse<PublicRoomStatusShare>>(
    `/v1/room-status-share/public/${encodeURIComponent(shareToken)}`,
  )
}

export const getPublicRoomStatusCalendar = (
  shareToken: string,
  startDate?: string,
  endDate?: string,
) => {
  return publicRequest.get<ApiResponse<PublicRoomStatusCalendar>>(
    `/v1/room-status-share/public/${encodeURIComponent(shareToken)}/room-status`,
    {
      params: {
        startDate,
        endDate,
      },
    },
  )
}

export const getPublicRoomStatusStatistics = (shareToken: string, date?: string) => {
  return publicRequest.get<ApiResponse<PublicRoomStatusStatistics>>(
    `/v1/room-status-share/public/${encodeURIComponent(shareToken)}/statistics`,
    {
      params: {
        date,
      },
    },
  )
}
