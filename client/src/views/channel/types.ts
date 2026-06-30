import type {
  ChannelMappingMultiplierSyncSummary,
  MappingPriceSettingRowDTO,
  PriceAdjustmentType,
} from '@/api/pricelabs'

/** OTA 渠道卡片数据 */
export interface ChannelItem {
  id: number
  name: string
  code: string
  logoUrl: string
  connected: boolean
}

/** 酒店列表条目 */
export interface HotelItem {
  id: number
  hotelCode: string
  hotelName: string
  priceMode: string
  status: string
}

/** Airbnb 帐户 */
export interface AirbnbAccount {
  id: number
  account: string
  accountCode: string
}

/** 价格计划映射项 */
export interface PricePlanMapping {
  id: string
  channelPricePlan: string
  channelPricePlanId: string
  pmsPricePlan: string | null
  selectedPmsPricePlan: string | null
  /** 已连接、未直连、无效 */
  status: 'connected' | 'disconnected' | 'invalid'
}

/** 房型映射组 */
export interface RoomMappingGroup {
  roomGroupId: string
  channelRoomType: string
  channelRoomId: string
  pmsRoomType: string | null
  selectedPmsRoom: string | null
  pricePlans: PricePlanMapping[]
}

/** 扁平化后的映射数据项（用于表格展示） */
export interface FlattenedMappingItem {
  id: string
  roomGroupId: string
  channelRoomType: string
  channelRoomId: string
  pmsRoomType: string | null
  selectedPmsRoom: string | null
  channelPricePlan: string
  channelPricePlanId: string
  pmsPricePlan: string | null
  selectedPmsPricePlan: string | null
  status: 'connected' | 'disconnected' | 'invalid'
  isFirstInGroup: boolean
  groupRowCount: number
}

/** 预定设置表单 */
export interface BookingSettings {
  /** 提前预订量（小时） */
  advanceBookingHours: number
  /** 是否需要申请 */
  requireApproval: boolean
  /** 准备时间（晚） */
  preparationNights: number
  /** 预订开放期（天） */
  bookingWindowDays: number
  /** 入住开始时间 */
  checkInStartTime: string
  /** 入住结束时间（不限用空字符串） */
  checkInEndTime: string
  /** 离店时间 */
  checkOutTime: string
}

/** 价格比例表格行 */
export interface PriceRatioItem {
  channelId: number
  channelCode: string
  channel: string
  ratio: string
  adjustmentType: PriceAdjustmentType
  adjustmentValue: number | null
  autoSyncPrice: boolean
  suMappingMultiplier?: number
  suMappingSurcharge?: number
  suMappingSync?: ChannelMappingMultiplierSyncSummary
}

/** 价格比例编辑态 */
export interface PriceRatioEdit {
  channelId: number
  channel: string
  ratio: string
  adjustmentType: 'cheaper' | 'expensive'
  adjustmentValue: number
  adjustmentUnit: '%' | '¥'
  autoSyncPrice: boolean
  backendAdjustmentType: PriceAdjustmentType
}

export type MappingPriceFilterMode = 'ALL' | 'FAILED' | 'DIRTY'

export type MappingPriceBatchField = 'MULTIPLIER' | 'SURCHARGE'

export interface MappingPriceDraftRow extends MappingPriceSettingRowDTO {
  originalMultiplier: number | null
  originalSurcharge: number | null
  draftMultiplier: number | null
  draftSurcharge: number | null
  dirty: boolean
  saving: boolean
}

/** 日历日期列 */
export interface CalendarDate {
  value: string
  label: string
  day: string
  weekday: string
}

/** 日历表格行 */
export interface CalendarRow {
  id: string
  label: string
  type: 'title' | 'inventory' | 'price' | 'number' | 'checkbox'
  roomId?: string
  values: Record<string, any>
}

/** Airbnb 房量设置行 */
export interface RoomSettingsRow {
  id: string
  airbnbRoomType: string
  pmsRoomType: string
  values: Record<string, any>
}

/** 下拉选项 */
export interface SelectOption {
  value: string
  label: string
}
