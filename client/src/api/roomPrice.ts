import { request } from '@/utils/request'

// 房价数据结构
export interface RoomPriceDTO {
  id?: number
  roomTypeId: number
  roomTypeName: string
  roomTypeCode: string
  priceDate: string
  price: number
  isWeekend: boolean
  isHoliday: boolean
  notes?: string
}

// 更新房价请求
export interface UpdateRoomPriceRequest {
  roomTypeId: number
  startDate: string
  endDate: string
  price: number
  isHoliday?: boolean
  notes?: string
}

// 房价表格显示数据
export interface RoomPriceTableData {
  roomTypeId: number
  roomTypeName: string
  roomTypeCode: string
  [key: string]: any // 动态日期列数据
}

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 获取指定日期范围内的房价数据
export const getRoomPrices = async (
  startDate: string,
  endDate: string,
  roomTypeIds?: number[]
): Promise<ApiResponse<RoomPriceDTO[]>> => {
  const params: any = { startDate, endDate }
  if (roomTypeIds && roomTypeIds.length > 0) {
    params.roomTypeIds = roomTypeIds
  }
  return await request.get('/room-prices', { params })
}

// 获取房型的有效价格
export const getEffectiveRoomPrice = async (
  roomTypeId: number,
  date: string
): Promise<ApiResponse<number>> => {
  return await request.get(`/room-prices/${roomTypeId}/effective`, {
    params: { date }
  })
}

// 获取特定房型和日期的价格
export const getRoomPrice = async (
  roomTypeId: number,
  date: string
): Promise<ApiResponse<RoomPriceDTO>> => {
  return await request.get(`/room-prices/${roomTypeId}`, {
    params: { date }
  })
}

// 获取房型在指定日期范围内的价格
export const getRoomPricesByRange = async (
  roomTypeId: number,
  startDate: string,
  endDate: string
): Promise<ApiResponse<RoomPriceDTO[]>> => {
  return await request.get(`/room-prices/${roomTypeId}/range`, {
    params: { startDate, endDate }
  })
}

// 更新房价
export const updateRoomPrice = async (
  request_data: UpdateRoomPriceRequest
): Promise<ApiResponse<RoomPriceDTO[]>> => {
  return await request.post('/room-prices', request_data)
}

// 批量更新房价
export const batchUpdateRoomPrices = async (
  requests: UpdateRoomPriceRequest[]
): Promise<ApiResponse<RoomPriceDTO[]>> => {
  return await request.post('/room-prices/batch', requests)
}

// 删除房价设置
export const deleteRoomPrice = async (
  roomTypeId: number,
  date: string
): Promise<ApiResponse<string>> => {
  return await request.delete(`/room-prices/${roomTypeId}`, {
    params: { date }
  })
}

// 删除房价设置（日期范围）
export const deleteRoomPriceRange = async (
  roomTypeId: number,
  startDate: string,
  endDate: string
): Promise<ApiResponse<string>> => {
  return await request.delete(`/room-prices/${roomTypeId}/range`, {
    params: { startDate, endDate }
  })
}

// 批量改价请求接口
export interface BulkPriceChangeRequest {
  roomTypeIds: number[]
  dateRanges: Array<{
    startDate: string
    endDate: string
  }>
  weekdays?: number[] // 1=周一, ..., 6=周六, 0=周日
  weekendDifferentiation: boolean
  weekdayPrice: number
  weekendPrice?: number
  notes?: string
}

// 批量改价
export const bulkPriceChange = async (
  requestData: BulkPriceChangeRequest
): Promise<ApiResponse<RoomPriceDTO[]>> => {
  return await request.post('/room-prices/bulk-change', requestData)
}

// 房价管理DTO
export interface RoomPriceManagementDTO {
  id: number
  roomTypeId: number
  roomTypeName: string
  roomTypeCode: string
  pricePlanId?: number
  pricePlanName?: string
  priceDate: string
  price: number
  availableRooms?: number
  minStay?: number
  maxStay?: number
  closeRoom?: boolean
  cta?: boolean
  ctd?: boolean
  isWeekend: boolean
  isHoliday: boolean
  notes?: string
}

// 按价格计划更新价格请求
export interface UpdatePriceByPlanRequest {
  roomTypeId: number
  pricePlanId: number
  startDate: string
  endDate: string
  weekdays?: number[] // 0=全部, 1=周一, 2=周二, ..., 7=周日
  applyWeekdaysInRange?: boolean
  price?: number
  availableRooms?: number
  minStay?: number
  maxStay?: number
  closeRoom?: boolean
  cta?: boolean
  ctd?: boolean
  notes?: string
}

// 改价记录数据结构
export interface PriceChangeHistoryDTO {
  id: number
  roomTypeName: string // 房型名称
  pricePlanName: string // 价格计划名称(如 Standard Rate, Rack Rate)
  priceDate: string // 价格日期范围
  applyDays: string // 适用周几(如 "全部")
  changeType: string // 修改类型(如 "价格")
  changeValue: number // 修改后的价格
  previousValue?: number // 修改前的价格
  operator: string // 操作人
  operateTime: string // 操作时间
  pmsPushTime?: string // PMS推送时间
  notes?: string
}

// 改价记录查询参数
export interface PriceChangeHistoryQueryParams {
  operateDateStart?: string // 操作日期开始
  operateDateEnd?: string // 操作日期结束
  priceDateStart?: string // 价格日期开始
  priceDateEnd?: string // 价格日期结束
  pricePlanId?: string // 价格计划ID(如果"全部"则不传)
  roomTypeId?: number // 房型ID
  operator?: string // 操作人
  pageNum?: number // 页码
  pageSize?: number // 每页数量
}

// 改价记录分页响应
export interface PriceChangeHistoryPageResponse {
  total: number
  records: PriceChangeHistoryDTO[]
  pageNum: number
  pageSize: number
}

// 获取房价管理数据(包含价格计划)
export const getRoomPriceManagementData = async (
  startDate: string,
  endDate: string,
  roomTypeId?: number
): Promise<ApiResponse<RoomPriceManagementDTO[]>> => {
  const params: any = { startDate, endDate }
  if (roomTypeId) params.roomTypeId = roomTypeId
  return await request.get('/room-prices/management', { params })
}

// 按价格计划更新价格
export const updatePriceByPlan = async (
  requestData: UpdatePriceByPlanRequest,
  operator: string
): Promise<ApiResponse<RoomPriceManagementDTO[]>> => {
  return await request.post('/room-prices/update-by-plan', requestData, {
    params: { operator }
  })
}

// 获取改价记录列表
export const getPriceChangeHistory = async (
  params: PriceChangeHistoryQueryParams
): Promise<ApiResponse<PriceChangeHistoryPageResponse>> => {
  return await request.get('/price-change-history', { params })
}

// 构建房价表格数据
export const buildRoomPriceTableData = (
  roomTypes: any[],
  roomPrices: RoomPriceDTO[],
  dateColumns: any[]
): RoomPriceTableData[] => {
  // 创建价格映射
  const priceMap = new Map<string, RoomPriceDTO>()
  roomPrices.forEach(price => {
    const key = `${price.roomTypeId}-${price.priceDate}`
    priceMap.set(key, price)
  })

  return roomTypes.map(roomType => {
    const row: RoomPriceTableData = {
      roomTypeId: roomType.id,
      roomTypeName: roomType.name,
      roomTypeCode: roomType.code
    }

    // 为每个日期列添加价格数据
    dateColumns.forEach(dateCol => {
      const key = `${roomType.id}-${dateCol.date}`
      const roomPrice = priceMap.get(key)

      if (roomPrice) {
        // 有特定价格设置
        row[dateCol.prop] = {
          price: roomPrice.price,
          hasSpecial: true,
          isHoliday: roomPrice.isHoliday,
          notes: roomPrice.notes
        }
      } else {
        // 使用默认价格逻辑
        let defaultPrice = 0
        const isWeekend = dateCol.isHoliday

        if (isWeekend && roomType.weekendPrice) {
          defaultPrice = roomType.weekendPrice
        } else if (!isWeekend && roomType.weekdayPrice) {
          defaultPrice = roomType.weekdayPrice
        } else if (roomType.defaultPrice) {
          defaultPrice = roomType.defaultPrice
        }

        row[dateCol.prop] = {
          price: defaultPrice,
          hasSpecial: false,
          isHoliday: dateCol.isHoliday
        }
      }
    })

    return row
  })
}
