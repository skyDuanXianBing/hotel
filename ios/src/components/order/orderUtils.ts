import type { OrderBoxItem } from '@/api/orderBox'
import type { ReservationDTO } from '@/api/reservation'
import { i18n } from '@/locales'
import { formatMoney, type MoneyDisplayContext } from '@/utils/formatters'
import {
  formatBusinessDateLabel,
  formatStoreDateTime,
} from '@/utils/storeBusinessDate'

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
  operationDate: string
}

export interface OrderOptionItem {
  label: string
  value: string
  color?: string
}

export interface OrderSummaryCardItem {
  key: OrderTabValue
  title: string
  value: number
  note: string
  tone: 'primary' | 'warning' | 'secondary' | 'success' | 'danger'
}

export const ORDER_PRIMARY_TABS: Array<{ labelKey: string; value: OrderTabValue }> = [
  { labelKey: 'order.tabs.all', value: 'all' },
  { labelKey: 'order.tabs.todayCheckin', value: 'today-checkin' },
  { labelKey: 'order.tabs.todayCheckout', value: 'today-checkout' },
  { labelKey: 'order.tabs.todayNew', value: 'today-new' },
]

export const ORDER_SECONDARY_TABS: Array<{ labelKey: string; value: OrderTabValue }> = [
  { labelKey: 'order.tabs.unassigned', value: 'unassigned' },
  { labelKey: 'order.tabs.assigned', value: 'assigned' },
  { labelKey: 'order.tabs.pending', value: 'pending' },
  { labelKey: 'order.tabs.orderBox', value: 'order-box' },
  { labelKey: 'order.tabs.deletedRooms', value: 'deleted-rooms' },
]

const t = (key: string, params?: Record<string, unknown>) =>
  params ? i18n.global.t(key, params) : i18n.global.t(key)

export function createDefaultOrderFilters(): OrderFilterForm {
  return {
    channel: [],
    roomType: [],
    checkinType: '',
    status: '',
    paymentStatus: '',
    startDate: '',
    endDate: '',
    operationDate: '',
  }
}

export function getOrderTabLabel(tab: OrderTabValue) {
  if (tab === 'all') {
    return t('order.tabs.all')
  }
  if (tab === 'today-checkin') {
    return t('order.tabs.todayCheckin')
  }
  if (tab === 'today-checkout') {
    return t('order.tabs.todayCheckout')
  }
  if (tab === 'today-new') {
    return t('order.tabs.todayNew')
  }
  if (tab === 'unassigned') {
    return t('order.tabs.unassigned')
  }
  if (tab === 'assigned') {
    return t('order.tabs.assigned')
  }
  if (tab === 'pending') {
    return t('order.tabs.pending')
  }
  if (tab === 'order-box') {
    return t('order.tabs.orderBox')
  }
  return t('order.tabs.deletedRooms')
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
      title: t('order.tabs.todayCheckin'),
      value: Number(statistics.todayCheckinCount ?? 0),
      note: t('order.mobile.summary.prepareCheckin'),
      tone: 'primary',
    },
    {
      key: 'today-checkout',
      title: t('order.tabs.todayCheckout'),
      value: Number(statistics.todayCheckoutCount ?? 0),
      note: t('order.mobile.summary.watchCheckout'),
      tone: 'warning',
    },
    {
      key: 'today-new',
      title: t('order.tabs.todayNew'),
      value: Number(statistics.todayNewCount ?? 0),
      note: t('order.mobile.summary.viewNewOrders'),
      tone: 'secondary',
    },
    {
      key: 'unassigned',
      title: t('order.tabs.unassigned'),
      value: Number(statistics.unassignedCount ?? 0),
      note: t('order.mobile.summary.watchAssignment'),
      tone: 'danger',
    },
    {
      key: 'pending',
      title: t('order.tabs.pending'),
      value: Number(statistics.pendingCount ?? 0),
      note: t('order.mobile.summary.handleExceptions'),
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
    return t('order.status.confirmed')
  }
  if (normalized === 'REQUESTED') {
    return t('order.status.requested')
  }
  if (normalized === 'CHECKED_IN') {
    return t('order.status.checkedIn')
  }
  if (normalized === 'CHECKED_OUT') {
    return t('order.status.checkedOut')
  }
  if (normalized === 'CANCELLED') {
    return t('order.status.cancelled')
  }
  return status || t('order.mobile.unknownStatus')
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
    return t('order.options.earlyCheckin')
  }
  if (normalized === 'late') {
    return t('order.options.lateCheckin')
  }
  return t('order.options.normalCheckin')
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
    return t('order.assignStatus.unassigned')
  }
  if (canAssignRoom(reservation)) {
    return t('order.assignStatus.assigned')
  }
  return t('order.assignStatus.assignedNoInventory')
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
    return t('order.settlement.settled')
  }
  if (paidAmount > 0 && paidAmount < totalAmount) {
    return t('order.settlement.partiallySettled')
  }
  return t('order.settlement.unsettled')
}

export function getSettlementStatusColor(reservation: ReservationDTO) {
  const paidAmount = Number(reservation.paidAmount ?? 0)
  const totalAmount = Number(reservation.totalAmount ?? 0)
  if (totalAmount > 0 && paidAmount >= totalAmount) {
    return 'success'
  }
  if (paidAmount > 0 && paidAmount < totalAmount) {
    return 'warning'
  }
  return 'danger'
}

export function formatAmount(
  value?: number,
  currency = 'CNY',
  context: MoneyDisplayContext = {},
) {
  const amount = Number(value ?? 0)
  if (!Number.isFinite(amount)) {
    return formatMoney(
      0,
      currency,
      { minimumFractionDigits: 2, maximumFractionDigits: 2 },
      context,
    )
  }

  return formatMoney(
    amount,
    currency,
    { minimumFractionDigits: 2, maximumFractionDigits: 2 },
    context,
  )
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
  const paidAmount = Number(reservation.paidAmount ?? 0)
  const totalAmount = Number(reservation.totalAmount ?? 0)
  const isSettled = totalAmount > 0 && paidAmount >= totalAmount
  if (paymentStatus === 'paid') {
    return isSettled
  }
  if (paymentStatus === 'unpaid') {
    return !isSettled
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
  const checkInDate = reservation.checkInDate?.includes('T')
    ? reservation.checkInDate.split('T')[0]
    : reservation.checkInDate
  if (!checkInDate) {
    return false
  }
  if (startDate && checkInDate < startDate) {
    return false
  }
  if (endDate && checkInDate > endDate) {
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
