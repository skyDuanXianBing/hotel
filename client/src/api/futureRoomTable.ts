import { request } from '@/utils/request'
import type { ApiResponse, FutureRoomTableData } from '@/types/room'

/**
 * 获取远期房情表数据
 * @param startDate 开始日期 (YYYY-MM-DD)
 * @param days 天数，默认7天
 * @returns 远期房情表数据
 */
export const getFutureRoomTableData = async (
  startDate: string,
  days: number = 7
): Promise<ApiResponse<FutureRoomTableData>> => {
  return request.get('/future-room-table', {
    params: {
      startDate,
      days
    }
  })
}