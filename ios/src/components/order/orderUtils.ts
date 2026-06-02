import type { OrderBoxItem } from '@/api/orderBox'
import type { ReservationDTO } from '@/api/reservation'
import { formatBusinessDateLabel, formatStoreDateTime } from '@/utils/storeBusinessDate'

export type OrderTabValue =
  | 'all'
  | 'today-checkin'
  | 'today-checkout'
  | 'today-new'
  | 'unassigned'
  | 'assigned'
  | 'pending'
  | 'order-box'
  | 'deleted-rooms'

export interface OrderFilterForm {
  channel: string[]
  roomType: string[]
  checkinType: string
  status: string
  paymentStatus: string
  startDate: string
  endDate: string
}

export interface OrderOptionItem {
  label: string
  value: string
}

export interface OrderSummaryCardItem {
  key: OrderTabValue
  title: string
  value: number
  note: string
  tone: 'primary' | 'warning' | 'secondary' | 'success' | 'danger'
}

export const ORDER_PRIMARY_TABS: Array<{ label: string; value: OrderTabValue }> = [
  { label: '全部', value: 'all' },
  { label: '预抵', value: 'today-checkin' },
  { label: '预离', value: 'today-checkout' },
  { label: '新单', value: 'today-new' },
]

export const ORDER_SECONDARY_TABS: Array<{ label: string; value: OrderTabValue }> = [
  { label: '未排房/未映射', value: 'unassigned' },
  { label: '已排房', value: 'assigned' },
  { label: '待处理', value: 'pending' },
  { label: '订单盒子', value: 'order-box' },
  { label: '房型/房间已删除', value: 'deleted-rooms' },
]

export function createDefaultOrderFilters(): OrderFilterForm {
  return {
    channel: [],
    roomType: [],
    checkinType: '',
    status: '',
    paymentStatus: '',
    startDate: '',
    endDate: '',
  }
}

export function getOrderTabLabel(tab: OrderTabValue) {
  if (tab === 'all') {
    return '全部订单'
  }
  if (tab === 'today-checkin') {
    return '今日预抵'
  }
  if (tab === 'today-checkout') {
    return '今日预离'
  }
  if (tab === 'today-new') {
    return '今日新单'
  }
  if (tab === 'unassigned') {
    return '未排房/未映射'
  }
  if (tab === 'assigned') {
    return '已排房'
  }
  if (tab === 'pending') {
    return '待处理'
  }
  if (tab === 'order-box') {
    return '订单盒子'
  }
  return '房型/房间已删除'
}

export function mapHomeTypeToOrderTab(type?: string): OrderTabValue {
  if (type === 'today-arrivals') {
    return 'today-checkin'
  }
  if (type === 'today-departures') {
    return 'today-checkout'
  }
  if (type === 'today-new') {
    return 'today-new'
  }
  if (type === 'unassigned') {
    return 'unassigned'
  }
  if (type === 'assigned') {
    return 'assigned'
  }
  if (type === 'pending') {
    return 'pending'
  }
  if (type === 'order-box') {
    return 'order-box'
  }
  if (type === 'deleted-rooms') {
    return 'deleted-rooms'
  }
  return 'all'
}

export function mapOrderTabToApiType(tab: OrderTabValue): string {
  if (tab === 'today-checkin') {
    return 'today-arrivals'
  }
  if (tab === 'today-checkout') {
    return 'today-departures'
  }
  if (tab === 'today-new') {
    return 'today-new'
  }
  if (tab === 'unassigned') {
    return 'unassigned'
  }
  if (tab === 'assigned') {
    return 'assigned'
  }
  if (tab === 'pending') {
    return 'pending'
  }
  return ''
}

export function buildOrderSummaryCards(statistics: {
  todayCheckinCount?: number
  todayCheckoutCount?: number
  todayNewCount?: number
  unassignedCount?: number
  pendingCount?: number
}): OrderSummaryCardItem[] {
  return [
    {
      key: 'today-checkin',
      title: '今日预抵',
      value: Number(statistics.todayCheckinCount ?? 0),
      note: '优先处理入住准备',
      tone: 'primary',
    },
    {
      key: 'today-checkout',
      title: '今日预离',
      value: Number(statistics.todayCheckoutCount ?? 0),
      note: '关注退房节奏',
      tone: 'warning',
    },
    {
      key: 'today-new',
      title: '今日新单',
      value: Number(statistics.todayNewCount ?? 0),
      note: '查看新增订单',
      tone: 'secondary',
    },
    {
      key: 'unassigned',
      title: '未排房/未映射',
      value: Number(statistics.unassignedCount ?? 0),
      note: '关注排房与映射异常',
      tone: 'danger',
    },
    {
      key: 'pending',
      title: '待处理',
      value: Number(statistics.pendingCount ?? 0),
      note: '集中处理异常项',
      tone: 'success',
    },
  ]
}

export function getDisplayChannelOrderNumber(reservation: ReservationDTO) {
  const channelOrderNumber = reservation.channelOrderNumber?.trim()
  if (channelOrderNumber) {
    return channelOrderNumber
  }

  const orderNumber = reservation.orderNumber?.trim() || ''
  const underscoreIndex = orderNumber.indexOf('_')
  if (underscoreIndex > 0) {
    return orderNumber.slice(0, underscoreIndex)
  }

  return '-'
}

export function getReservationStatusText(status?: string) {
  const normalized = (status || '').toUpperCase()
  if (normalized === 'CONFIRMED' || normalized === 'NEW') {
    return '已确认'
  }
  if (normalized === 'REQUESTED') {
    return '待确认'
  }
  if (normalized === 'CHECKED_IN') {
    return '已入住'
  }
  if (normalized === 'CHECKED_OUT') {
    return '已退房'
  }
  if (normalized === 'CANCELLED') {
    return '已取消'
  }
  return status || '未知状态'
}

export function getReservationStatusColor(status?: string) {
  const normalized = (status || '').toUpperCase()
  if (normalized === 'CHECKED_IN') {
    return 'success'
  }
  if (normalized === 'REQUESTED') {
    return 'warning'
  }
  if (normalized === 'CANCELLED') {
    return 'danger'
  }
  if (normalized === 'CHECKED_OUT') {
    return 'medium'
  }
  return 'primary'
}

export function getCheckinTypeText(checkinType?: string) {
  const normalized = (checkinType || '').toLowerCase()
  if (normalized === 'early') {
    return '提前入住'
  }
  if (normalized === 'late') {
    return '延迟入住'
  }
  return '正常入住'
}

export function canAssignRoom(reservation: ReservationDTO) {
  const normalized = (reservation.status || '').toUpperCase()
  return normalized === 'CONFIRMED' || normalized === 'REQUESTED' || normalized === 'CHECKED_IN'
}

export function canCheckInOrder(reservation: ReservationDTO) {
  const normalized = (reservation.status || '').toUpperCase()
  return normalized === 'CONFIRMED' || normalized === 'REQUESTED' || normalized === 'NEW'
}

export function canCheckOutOrder(reservation: ReservationDTO) {
  return (reservation.status || '').toUpperCase() === 'CHECKED_IN'
}

export function canCancelOrder(reservation: ReservationDTO) {
  const normalized = (reservation.status || '').toUpperCase()
  return normalized !== 'CANCELLED' && normalized !== 'CHECKED_OUT'
}

export function getAssignStatusText(reservation: ReservationDTO) {
  if (!reservation.roomId) {
    return '未排房'
  }
  if (canAssignRoom(reservation)) {
    return '已排房'
  }
  return '已排房(不占房)'
}

export function getAssignStatusColor(reservation: ReservationDTO) {
  if (!reservation.roomId) {
    return 'warning'
  }
  if (canAssignRoom(reservation)) {
    return 'success'
  }
  return 'medium'
}

export function getSettlementStatusText(reservation: ReservationDTO) {
  const paidAmount = Number(reservation.paidAmount ?? 0)
  const totalAmount = Number(reservation.totalAmount ?? 0)
  if (totalAmount > 0 && paidAmount >= totalAmount) {
    return '已结账'
  }
  if (paidAmount > 0 && paidAmount < totalAmount) {
    return '部分结账'
  }
  return '未结账'
}

export function getSettlementStatusColor(reservation: ReservationDTO) {
  const settlementStatus = getSettlementStatusText(reservation)
  if (settlementStatus === '已结账') {
    return 'success'
  }
  if (settlementStatus === '部分结账') {
    return 'warning'
  }
  return 'danger'
}

export function formatAmount(value?: number) {
  const amount = Number(value ?? 0)
  if (!Number.isFinite(amount)) {
    return '¥0.00'
  }
  return `¥${amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

export function formatDateTime(value?: string) {
  return formatStoreDateTime(value)
}

export function formatDateLabel(value?: string) {
  if (!value) {
    return '-'
  }
  const dateText = value.includes('T') ? value.split('T')[0] : value.split(' ')[0]
  return formatBusinessDateLabel(dateText, 'date', value)
}

function includesKeyword(fieldValue: string | undefined, keyword: string) {
  if (!fieldValue) {
    return false
  }
  return fieldValue.toLowerCase().includes(keyword)
}

function matchesStatusFilter(reservation: ReservationDTO, status: string) {
  const normalized = (reservation.status || '').toUpperCase()
  if (status === 'checked-in') {
    return normalized === 'CHECKED_IN'
  }
  if (status === 'checked-out') {
    return normalized === 'CHECKED_OUT'
  }
  if (status === 'not-checked-in') {
    return normalized !== 'CHECKED_IN' && normalized !== 'CHECKED_OUT'
  }
  return true
}

function matchesCheckinTypeFilter(reservation: ReservationDTO, checkinType: string) {
  if (!checkinType) {
    return true
  }
  const normalizedReservationType = (reservation.checkinType || 'normal').toLowerCase()
  return normalizedReservationType === checkinType.toLowerCase()
}

function matchesPaymentFilter(reservation: ReservationDTO, paymentStatus: string) {
  const settlementStatus = getSettlementStatusText(reservation)
  if (paymentStatus === 'paid') {
    return settlementStatus === '已结账'
  }
  if (paymentStatus === 'unpaid') {
    return settlementStatus !== '已结账'
  }
  return true
}

function matchesMultiValueFilter(selectedValues: string[], value?: string) {
  if (selectedValues.length === 0) {
    return true
  }
  if (!value) {
    return false
  }
  return selectedValues.includes(value)
}

function matchesDateRange(reservation: ReservationDTO, startDate: string, endDate: string) {
  const createdDate = formatDateLabel(reservation.createdAt)
  if (!createdDate || createdDate === '-') {
    return false
  }
  if (startDate && createdDate < startDate) {
    return false
  }
  if (endDate && createdDate > endDate) {
    return false
  }
  return true
}

export function matchesReservationSearch(
  reservation: ReservationDTO,
  filters: OrderFilterForm,
  keyword: string,
) {
  const normalizedKeyword = keyword.trim().toLowerCase()
  if (normalizedKeyword) {
    const matched =
      includesKeyword(reservation.orderNumber, normalizedKeyword) ||
      includesKeyword(getDisplayChannelOrderNumber(reservation), normalizedKeyword) ||
      includesKeyword(reservation.roomNumber, normalizedKeyword) ||
      includesKeyword(reservation.guestName, normalizedKeyword) ||
      includesKeyword(reservation.phone, normalizedKeyword)

    if (!matched) {
      return false
    }
  }

  if (!matchesMultiValueFilter(filters.channel, reservation.channelName)) {
    return false
  }
  if (!matchesMultiValueFilter(filters.roomType, reservation.roomTypeName)) {
    return false
  }
  if (filters.checkinType && !matchesCheckinTypeFilter(reservation, filters.checkinType)) {
    return false
  }
  if (filters.status && !matchesStatusFilter(reservation, filters.status)) {
    return false
  }
  if (filters.paymentStatus && !matchesPaymentFilter(reservation, filters.paymentStatus)) {
    return false
  }
  if ((filters.startDate || filters.endDate) && !matchesDateRange(reservation, filters.startDate, filters.endDate)) {
    return false
  }

  return true
}

export function findOrderBoxItemByReservationId(items: OrderBoxItem[], reservationId: number) {
  for (const item of items) {
    if (item.reservation.id === reservationId) {
      return item
    }
  }
  return null
}
