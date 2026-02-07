// 房间状态枚举
export enum RoomStatus {
  AVAILABLE = 'AVAILABLE', // 可用
  OCCUPIED = 'OCCUPIED', // 已入住
  RESERVED = 'RESERVED', // 已预订
  MAINTENANCE = 'MAINTENANCE', // 维修
  OUT_OF_ORDER = 'OUT_OF_ORDER', // 停用
}

// 渠道类型枚举
export enum ChannelType {
  DIRECT = 'DIRECT', // 直销
  OTA = 'OTA', // 在线旅行社
  TRAVEL_AGENCY = 'TRAVEL_AGENCY', // 旅行社
  CORPORATE = 'CORPORATE', // 企业客户
}

// 预订状态枚举
export enum ReservationStatus {
  CONFIRMED = 'CONFIRMED', // 已确认
  CHECKED_IN = 'CHECKED_IN', // 已入住
  CHECKED_OUT = 'CHECKED_OUT', // 已退房
  CANCELLED = 'CANCELLED', // 已取消
  NO_SHOW = 'NO_SHOW', // 未到
}

// 房型类别枚举
export enum RoomTypeCategory {
  SUITE = 'suite', // 套房
  STANDARD = 'standard', // 营位
  LUXURY = 'luxury', // 账篷
}

// 包含早餐枚举
export enum BreakfastOption {
  NONE = 'none', // 无早餐
  SINGLE = 'single', // 单早
  DOUBLE = 'double', // 双早
}

// 接受设置枚举
export enum AcceptanceSetting {
  MANUAL = 'manual', // 手动接单
  AUTO = 'auto', // 自动接单
}

// 取消政策枚举
export enum CancellationPolicy {
  CONFIRMED_NO_CANCEL = 'confirmed_no_cancel', // 一经确认，不可取消修改
  FREE_CANCEL_BEFORE_CHECKIN = 'free_cancel_before_checkin', // 入住日前可免费取消
}

// 床型信息接口
export interface BedInfo {
  type: string // 床型（如：大床）
  size: string // 尺寸（如：2×1.8米）
  count: number // 数量
}

// 户型接口
export interface RoomLayout {
  bedroom: number // 卧室数量
  bathroom: number // 卫生间数量
  livingRoom: number // 客厅数量
  kitchen: number // 厨房数量
  study: number // 书房数量
  balcony: number // 阳台数量
}

// 房间设施接口
export interface Amenity {
  id: string
  name: string
  category: string // 设施分类（如：核心设施）
}

// 房型标签接口
export interface RoomTypeTag {
  id: string
  name: string
  color?: string
}

// 销售设置接口
export interface SaleSettings {
  includeBreakfast: BreakfastOption
  latestCheckInTime: string // 24:00 格式
  acceptanceSetting: AcceptanceSetting
  cancellationPolicy: CancellationPolicy
  freeCancelDays?: number // 入住前几天可免费取消
}

// 基础房型接口（向后兼容）
export interface BaseRoomType {
  id: number
  name: string
  code: string
  totalRooms: number
  description?: string
  defaultPrice?: number
  weekdayPrice?: number
  weekendPrice?: number
  roomNumbers?: string[]
}

// 完整房型接口（新增功能）
export interface RoomType extends BaseRoomType {
  externalName: string // 对外名称
  defaultPrice: number
  weekdayPrice?: number
  weekendPrice?: number

  // 基本信息
  category: RoomTypeCategory
  area: number // 面积（平方米）
  capacity: number // 可住人数
  roomLayout: RoomLayout // 户型
  bedInfo: BedInfo[] // 床型信息

  // 图片
  images: string[] // 房型图片列表

  // 设施和标签
  amenities: Amenity[] // 设施列表
  tags: RoomTypeTag[] // 标签列表

  // 销售设置
  saleSettings: SaleSettings

  // 相关说明
  description_rich?: string // 富文本相关说明

  // 时间戳
  createdAt?: string
  updatedAt?: string
}

// 房间接口
export interface Room {
  id: number
  roomNumber: string
  roomType: BaseRoomType
  floor: number
  status: RoomStatus
}

// 预订信息接口
export interface Reservation {
  id: number
  guestName: string
  phone?: string
  channel: string
  channelType?: ChannelType
  checkIn: string
  checkOut: string
  orderNumber: string
  adults: number
  children: number
  totalAmount: number
  status: ReservationStatus
  notes?: string
}

// 日常房态接口
export interface DailyRoomStatus {
  date: string
  status: RoomStatus
  reservation?: Reservation | null
  closed?: boolean
  closeType?: string
  closeRemark?: string
}

// 房态日历房间数据接口
export interface CalendarRoomData {
  roomId: number
  roomNumber: string
  roomType: string
  dailyStatus: DailyRoomStatus[]
}

// 房态日历数据接口
export interface RoomStatusCalendar {
  dateRange: {
    startDate: string
    endDate: string
  }
  rooms: CalendarRoomData[]
}

// 单日房态房型数据接口
export interface DailyRoomTypeData {
  id: number
  name: string
  totalRooms: number
  availableRooms: number
  occupiedRooms: number
  maintenanceRooms: number
  rooms: Array<{
    id: number
    roomNumber: string
    status: RoomStatus
    guestName?: string
    channel?: string
  }>
}

// 单日房态数据接口
export interface DailyRoomStatusData {
  date: string
  roomTypes: DailyRoomTypeData[]
}

// 渠道接口
export interface Channel {
  id: number
  name: string
  code: string
  type: ChannelType
}

// 房态修改记录接口
export interface RoomStatusLog {
  id: number
  roomId: number
  roomNumber: string
  date: string
  oldStatus: RoomStatus
  newStatus: RoomStatus
  reason?: string
  operatorName: string
  operatedAt: string
}

// 房情表统计数据接口
export interface RoomStatistics {
  roomTypeName: string // 房型名称
  totalRooms: number // 总房数
  availableForSale: number // 可售房
  availableRooms: number // 可用房
  occupiedRooms: number // 在住
  occupiedWithoutDeparture: number // 在住（不含预离）
  scheduledDeparture: number // 预离
  scheduledArrival: number // 预抵
  reservedRooms: number // 保留房（关房）
  maintenanceRooms: number // 维修房（关房）
  outOfOrderRooms: number // 停用房（关房）
  linkedClosedRooms: number // 链接关房（关房）
  cleanRooms: number // 净房
  dirtyRooms: number // 脏房
  expectedOccupancyRate: number // 预计入住率（百分比）
  dailyCancelledRooms: number // 当日取消房
}

// 房情表数据接口
export interface RoomTableData {
  date: string // 统计日期
  statistics: RoomStatistics[] // 各房型统计
  total: RoomStatistics // 合计数据
}

// 远期房情日期数据接口
export interface FutureDateRoomData {
  date: string // 日期 (YYYY-MM-DD)
  dayOfWeek: string // 星期几
  available: number // 可售
  occupied: number // 占用
  unavailable: number // 不可售
  availableRate: number // 可售比例 (%)
  occupiedRate: number // 占用比例 (%)
  unavailableRate: number // 不可售比例 (%)
}

// 远期房情房型数据接口
export interface FutureRoomTypeData {
  roomTypeName: string // 房型名称
  totalRooms: number // 总房数
  dates: FutureDateRoomData[] // 每日数据
}

// 远期房情底部统计数据接口
export interface FutureRoomStatistics {
  date: string // 日期
  effectiveRooms: number // 有效客房数
  expectedOccupancyRate: number // 入住率（预期）%
  expectedRoomRevenue: number // 客房收入（预期）
  expectedTotalRoomFee: number // 总房费（预期）
  averageRoomRevenue: number // 平均客房收益（预期）
}

// 远期房情表数据接口
export interface FutureRoomTableData {
  startDate: string // 开始日期
  endDate: string // 结束日期
  roomTypes: FutureRoomTypeData[] // 各房型数据
  total: FutureRoomTypeData // 合计数据
  statistics: FutureRoomStatistics[] // 底部统计数据
}

// 房态分享配置接口
export interface RoomStatusShareConfig {
  shareTitle: string // 分享标题
  viewRoomStatus: boolean // 查看房间状态开关
  queryMethod: boolean // 查询房间方式：true=查看房态，false=按时间查看
  viewType: 'normal' | 'blurred' // 查看方式：normal=正常查看，blurred=模糊查看
  queryMode: 'enabled' | 'disabled' // 开关房权限：enabled=启用，disabled=关闭
  filterItems: string[] // 统计数据
  orderItems: string[] // 订单数据
  associatedRooms: number[] // 关联房间ID列表
}

// 房态分享数据接口
export interface RoomStatusShare {
  id: number // 分享ID
  shareTitle: string // 分享标题
  roomCount: number // 房间数
  roomNumbers: string // 房间号（多个用逗号分隔）
  shareLink: string // 分享链接
  config: RoomStatusShareConfig // 分享配置
  createdAt: string // 创建时间
  updatedAt: string // 更新时间
  isActive: boolean // 是否启用
}

// 房态分享列表数据接口
export interface RoomStatusShareListData {
  shares: RoomStatusShare[] // 分享列表
  total: number // 总数
  page: number // 当前页
  pageSize: number // 每页条数
}

// 保洁状态枚举
export enum CleaningStatus {
  NOT_STARTED = 'not_started', // 未开始
  IN_PROGRESS = 'in_progress', // 进行中
  COMPLETED = 'completed', // 已完成
}

// 查检状态枚举
export enum InspectionStatus {
  PENDING = 'pending', // 待查检
  INSPECTED = 'inspected', // 已查检
  FAILED = 'failed', // 不合格
}

// 保洁员接口
export interface Housekeeper {
  id: number
  name: string
  phone: string
  associatedRooms: string // 关联房间
  wechatNickname?: string // 微信昵称
  isActive: boolean // 是否启用
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间
}

// 保洁员表单接口
export interface HousekeeperForm {
  name: string
  phone: string
  associatedRooms: string
  wechatNickname: string
  isActive: boolean
}

// 保洁员列表数据接口
export interface HousekeeperListData {
  housekeepers: Housekeeper[]
  total: number
  page: number
  pageSize: number
}

// 房务任务接口
export interface HousekeepingTask {
  id: number
  timeSlot: string // 时段
  roomType: string // 房型
  roomNumber: string // 房间号
  roomGroup: string // 房间分组
  checkoutTime: string // 退房时间
  housekeeper: string // 保洁员姓名
  housekeeperId?: number // 保洁员ID
  cleaningStatus: CleaningStatus // 保洁状态
  inspectionStatus: InspectionStatus // 查检状态
  cleaningType: string // 房扫类型
  cost: number // 费用
  startTime?: string // 开始时间
  endTime?: string // 结束时间
  notes?: string // 备注
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间
}

// 房务列表数据接口
export interface HousekeepingListData {
  tasks: HousekeepingTask[] // 任务列表
  total: number // 总数
  page: number // 当前页
  pageSize: number // 每页条数
}

// 房务筛选条件接口
export interface HousekeepingFilters {
  cleaningStatus?: CleaningStatus | string // 保洁状态
  roomType?: string // 房型
  roomGroup?: string // 房间分组
  inspectionStatus?: InspectionStatus | string // 查检状态
  housekeeperId?: number // 保洁员ID
  dateRange?: [string, string] // 日期范围
}

// API响应接口
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}
