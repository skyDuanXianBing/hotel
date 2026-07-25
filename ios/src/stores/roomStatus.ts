import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  closeRoomBlockouts,
  getRoomStatusCalendar,
  getRoomStatusStatistics,
  openRoomBlockouts,
  type CalendarRoomDataDTO,
  type DailyRoomStatusDTO,
  type RoomStatusStatisticsDTO,
} from '@/api/roomStatus'
import { getAllRoomGroups, getGroupMembers } from '@/api/roomGroup'
import { getRoomPriceManagementData } from '@/api/roomPrice'
import { getPricePlansByRoomType } from '@/api/pricePlan'
import { getAllRoomTypesWithRooms } from '@/api/roomType'
import {
  getAllChannels,
  getReservationById,
  searchReservations,
  type ChannelDTO,
  type ReservationDTO,
} from '@/api/reservation'
import { getSortOrderMap } from '@/api/sortConfig'
import { useUserStore } from '@/stores/user'
import { i18n } from '@/locales'
import type { RoomGroupDTO, RoomGroupMemberDTO } from '@/types/settings'
import { compareLocalizedText } from '@/utils/formatters'
import { showSuccessToast } from '@/utils/notify'
import {
  buildRoomStatusDailyPricingMap,
  getRoomStatusDailyPricing,
  normalizeRoomStatusPriceSource,
  type RoomStatusDailyPricingMap,
  type RoomStatusPriceSource,
} from '@/utils/roomStatusPricing'
import {
  getBusinessDateWeekdayIndex,
  getStoreTodayDate,
  parseBusinessDateParts,
  shiftBusinessDate,
} from '@/utils/storeBusinessDate'

export type BatchWeekMode = 'all' | 'weekday' | 'weekend'

export interface RoomStatusDateItem {
  date: string
  label: string
  weekday: string
  availableRooms: number
  isToday: boolean
  isSelected: boolean
  isFocused: boolean
}

export interface RoomTimelineItem {
  date: string
  label: string
  businessState: RoomStatusBusinessState
  statusText: string
  isSelected: boolean
  isFocused: boolean
  isToday: boolean
  isClosed: boolean
  isDirty: boolean
  closeType: string
  closeRemark: string
  reservation: ReservationDTO | null
  price?: number
  minStay?: number
}

export interface RoomStatusRoomItem {
  roomId: number
  roomNumber: string
  roomTypeId: number
  roomType: string
  focusedDate: string
  focusedStatus: string
  focusedBusinessState: RoomStatusBusinessState
  focusedStatusText: string
  focusedClosed: boolean
  closeType: string
  closeRemark: string
  isDirty: boolean
  reservation: ReservationDTO | null
  timeline: RoomTimelineItem[]
}

export interface RoomTypeSummaryItem {
  roomType: string
  totalRooms: number
  availableRooms: number
  occupiedRooms: number
  closedRooms: number
  dirtyRooms: number
  selected: boolean
}

export interface SummaryCardItem {
  key: string
  title: string
  value: number
  tone: 'primary' | 'success' | 'warning' | 'danger'
}

export interface RoomStatusPriceSourceOption {
  value: RoomStatusPriceSource
  label: string
}

export type RoomStatusBusinessState =
  | 'available'
  | 'occupied'
  | 'reserved'
  | 'retain'
  | 'maintenance'
  | 'out_of_order'
  | 'closed'
  | 'dirty'
  | 'unknown'

const WEEKDAY_KEYS = [
  'accommodation.weekdays.dayOfWeek.0',
  'accommodation.weekdays.dayOfWeek.1',
  'accommodation.weekdays.dayOfWeek.2',
  'accommodation.weekdays.dayOfWeek.3',
  'accommodation.weekdays.dayOfWeek.4',
  'accommodation.weekdays.dayOfWeek.5',
  'accommodation.weekdays.dayOfWeek.6',
] as const
const DEFAULT_SORT_ORDER = Number.MAX_SAFE_INTEGER
export const ROOM_STATUS_VIEWPORT_DAYS = 5
const DATE_WINDOW_SIZE = 14
const SELECTED_DATE_OFFSET = 5
const CALENDAR_RANGE_BUFFER_BEFORE = 2
const CALENDAR_RANGE_BUFFER_AFTER = 2
export const ROOM_STATUS_PRICE_VISIBLE_STORAGE_KEY = 'ios-room-status.show-cell-price'
export const ROOM_STATUS_PRICE_SOURCE_STORAGE_KEY = 'ios-room-status.cell-price-source'
const NATURAL_COMPARE_OPTIONS: Intl.CollatorOptions = {
  numeric: true,
  sensitivity: 'base',
}

interface CalendarWindowData {
  rooms: CalendarRoomDataDTO[]
  pricingMapPromise: Promise<RoomStatusDailyPricingMap>
  pricingRequestId: number
  targetWindowStartDate: string
}

const readStoredPriceVisibility = () => {
  if (typeof window === 'undefined') {
    return false
  }

  return window.localStorage.getItem(ROOM_STATUS_PRICE_VISIBLE_STORAGE_KEY) === 'true'
}

const readStoredPriceSource = () => {
  if (typeof window === 'undefined') {
    return 'default' as RoomStatusPriceSource
  }

  return normalizeRoomStatusPriceSource(
    window.localStorage.getItem(ROOM_STATUS_PRICE_SOURCE_STORAGE_KEY),
  )
}

const persistPricePreference = (key: string, value: string) => {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(key, value)
  }
}

const getTodayDateKey = () => getStoreTodayDate()
const t = (key: string, params?: Record<string, unknown>) => i18n.global.t(key, params ?? {})

export const formatDateKey = (date: Date) => {
  const year = date.getUTCFullYear()
  const month = String(date.getUTCMonth() + 1).padStart(2, '0')
  const day = String(date.getUTCDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const addDays = (date: string, amount: number) => {
  return shiftBusinessDate(date, amount)
}

export const getReservationStatusText = (status?: string) => {
  if (!status) {
    return t('roomStatus.store.reservationStatus.unknown')
  }
  if (status === 'CONFIRMED' || status === 'confirmed') {
    return t('roomStatus.store.reservationStatus.confirmed')
  }
  if (status === 'CHECKED_IN' || status === 'checked_in') {
    return t('roomStatus.store.reservationStatus.checkedIn')
  }
  if (status === 'CHECKED_OUT' || status === 'checked_out') {
    return t('roomStatus.store.reservationStatus.checkedOut')
  }
  if (status === 'CANCELLED' || status === 'cancelled') {
    return t('roomStatus.store.reservationStatus.cancelled')
  }
  if (status === 'NO_SHOW' || status === 'no_show') {
    return t('roomStatus.store.reservationStatus.noShow')
  }
  return status
}

export const getRoomBusinessState = (
  dailyStatus?: DailyRoomStatusDTO | null,
): RoomStatusBusinessState => {
  if (!dailyStatus) {
    return 'unknown'
  }

  if (dailyStatus.closed) {
    if (dailyStatus.closeType === 'maintenance') {
      return 'maintenance'
    }
    if (dailyStatus.closeType === 'retain') {
      return 'retain'
    }
    return 'closed'
  }

  const normalizedStatus = (dailyStatus.status || '').toUpperCase()

  if (normalizedStatus === 'OCCUPIED') {
    return 'occupied'
  }
  if (normalizedStatus === 'RESERVED') {
    return 'reserved'
  }
  if (normalizedStatus === 'AVAILABLE') {
    return 'available'
  }
  if (normalizedStatus === 'MAINTENANCE') {
    return 'maintenance'
  }
  if (normalizedStatus === 'DIRTY') {
    return 'dirty'
  }
  if (normalizedStatus === 'OUT_OF_ORDER') {
    return 'out_of_order'
  }

  if (dailyStatus.reservation) {
    return 'reserved'
  }

  return 'unknown'
}

export const getRoomStatusText = (dailyStatus?: DailyRoomStatusDTO | null) => {
  const businessState = getRoomBusinessState(dailyStatus)

  if (businessState === 'maintenance') {
    return t('roomStatus.store.roomState.maintenanceRoom')
  }
  if (businessState === 'retain') {
    return t('roomStatus.store.roomState.retainRoom')
  }
  if (businessState === 'closed' && dailyStatus) {
    return t('roomStatus.store.roomState.closedRoom')
  }

  if (businessState === 'available') {
    return t('roomStatus.store.roomState.available')
  }
  if (businessState === 'occupied') {
    return t('roomStatus.store.roomState.occupied')
  }
  if (businessState === 'reserved') {
    return t('roomStatus.store.roomState.reserved')
  }
  if (businessState === 'out_of_order') {
    return t('roomStatus.store.roomState.outOfOrder')
  }
  if (businessState === 'dirty') {
    return t('roomStatus.store.roomState.dirty')
  }

  if (!dailyStatus) {
    return t('roomStatus.store.roomState.unknown')
  }

  return dailyStatus.status
}

function getReservationStatusFromDailyStatus(dailyStatus: DailyRoomStatusDTO) {
  const businessState = getRoomBusinessState(dailyStatus)

  if (businessState === 'occupied') {
    return 'CHECKED_IN'
  }

  if (businessState === 'reserved') {
    return 'CONFIRMED'
  }

  return 'CONFIRMED'
}

function normalizeReservationAmount(totalAmount: unknown) {
  if (totalAmount === null || totalAmount === undefined) {
    return undefined
  }

  if (typeof totalAmount !== 'number' || !Number.isFinite(totalAmount)) {
    throw new Error(t('roomStatus.store.errors.invalidReservationAmount'))
  }

  return totalAmount
}

function hasReservationAmount(
  reservationAmountCache: Record<number, number>,
  reservationId: number,
) {
  return Object.prototype.hasOwnProperty.call(reservationAmountCache, reservationId)
}

function resolveReservationTotalAmount(
  reservation: NonNullable<DailyRoomStatusDTO['reservation']>,
  reservationAmountCache: Record<number, number>,
) {
  if (reservation.totalAmount !== null && reservation.totalAmount !== undefined) {
    return normalizeReservationAmount(reservation.totalAmount)
  }

  if (hasReservationAmount(reservationAmountCache, reservation.id)) {
    return normalizeReservationAmount(reservationAmountCache[reservation.id])
  }

  return undefined
}

function buildReservationModel(
  dailyStatus?: DailyRoomStatusDTO | null,
  reservationAmountCache: Record<number, number> = {},
) {
  if (!dailyStatus?.reservation) {
    return null
  }

  const reservation = dailyStatus.reservation
  const totalAmount = resolveReservationTotalAmount(reservation, reservationAmountCache)

  return {
    id: reservation.id,
    orderNumber: reservation.orderNumber,
    guestName: reservation.guestName,
    channelId: 0,
    channelName: reservation.channel,
    checkInDate: reservation.checkIn,
    checkOutDate: reservation.checkOut,
    status: getReservationStatusFromDailyStatus(dailyStatus),
    createdAt: '',
    updatedAt: '',
    groupOrderNo: reservation.groupOrderNo,
    notes: reservation.notes || reservation.specialRequests,
    totalAmount,
  } as ReservationDTO
}

function buildWeekModeRanges(startDate: string, endDate: string, weekMode: BatchWeekMode) {
  if (weekMode === 'all') {
    return [{ startDate, endDate }]
  }

  const ranges: Array<{ startDate: string; endDate: string }> = []
  let currentDate = startDate
  let rangeStart = ''

  while (currentDate <= endDate) {
    const day = getBusinessDateWeekdayIndex(currentDate)
    const matched = weekMode === 'weekday' ? day !== 0 && day !== 6 : day === 0 || day === 6

    if (matched && !rangeStart) {
      rangeStart = currentDate
    }

    if (!matched && rangeStart) {
      ranges.push({
        startDate: rangeStart,
        endDate: addDays(currentDate, -1),
      })
      rangeStart = ''
    }

    currentDate = addDays(currentDate, 1)
  }

  if (rangeStart) {
    ranges.push({
      startDate: rangeStart,
      endDate,
    })
  }

  return ranges.map((item) => ({
    startDate: item.startDate,
    endDate: item.endDate,
  }))
}

export const useRoomStatusStore = defineStore('roomStatus', () => {
  const userStore = useUserStore()
  const today = getTodayDateKey()
  const selectedDate = ref(today)
  const browsingDate = ref(today)
  const windowStartDate = ref(addDays(today, -SELECTED_DATE_OFFSET))
  const calendarRooms = ref<CalendarRoomDataDTO[]>([])
  const statistics = ref<RoomStatusStatisticsDTO | null>(null)
  const channels = ref<ChannelDTO[]>([])
  const selectedRoomTypes = ref<string[]>([])
  const searchResults = ref<ReservationDTO[]>([])
  const hasSearchCompleted = ref(false)
  const dirtyRoomIds = ref<number[]>([])
  const loading = ref(false)
  const summaryLoading = ref(false)
  const searching = ref(false)
  const actionLoading = ref(false)
  const reservationAmountCache = ref<Record<number, number>>({})
  const reservationAmountPending = new Map<number, Promise<void>>()
  const reservationAmountUnavailable = new Set<number>()
  let latestSearchRequestId = 0
  let latestPricingRequestId = 0
  let hasInitializedRoomTypeSelection = false
  const roomTypeSortOrderMap = ref<Record<string, number>>({})
  const roomTypeDailyPricingMap = ref<RoomStatusDailyPricingMap>({})
  const roomTypeDefaultPriceMap = ref<Record<number, number>>({})
  const showCellPrice = ref(readStoredPriceVisibility())
  const cellPriceSource = ref<RoomStatusPriceSource>(readStoredPriceSource())
  const mappedPricePlanOptions = ref<RoomStatusPriceSourceOption[]>([])
  const pricePlanOptionsLoading = ref(false)
  let loadedPricePlanRoomTypeSignature = ''
  let pricePlanOptionsGeneration = 0
  const roomSortOrderMap = ref<Record<number, number>>({})
  const roomToGroupSortOrderMap = ref<Map<number, number>>(new Map())
  const sortContextUserId = ref(0)
  const sortContextLoaded = ref(false)

  function isDateInVisibleWindow(date: string, targetWindowStartDate = windowStartDate.value) {
    const lastVisibleDate = addDays(targetWindowStartDate, DATE_WINDOW_SIZE - 1)
    return date >= targetWindowStartDate && date <= lastVisibleDate
  }

  const visibleFocusDate = computed(() => {
    if (isDateInVisibleWindow(selectedDate.value)) {
      return selectedDate.value
    }

    if (isDateInVisibleWindow(browsingDate.value)) {
      return browsingDate.value
    }

    return windowStartDate.value
  })

  const visibleDates = computed<RoomStatusDateItem[]>(() => {
    const items: RoomStatusDateItem[] = []
    const currentToday = getTodayDateKey()

    for (let index = 0; index < DATE_WINDOW_SIZE; index += 1) {
      const currentDate = addDays(windowStartDate.value, index)
      const dateParts = parseBusinessDateParts(currentDate)
      const month = dateParts ? dateParts.month : 1
      const day = dateParts ? dateParts.day : 1
      const weekdayIndex = getBusinessDateWeekdayIndex(currentDate)
      let availableRooms = 0

      for (const room of calendarRooms.value) {
        const dailyStatus = room.dailyStatus.find((item) => item.date === currentDate)
        if (getRoomBusinessState(dailyStatus) === 'available') {
          availableRooms += 1
        }
      }

      items.push({
        date: currentDate,
        label: `${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`,
        weekday: t(WEEKDAY_KEYS[weekdayIndex]),
        availableRooms,
        isToday: currentDate === currentToday,
        isSelected: currentDate === selectedDate.value,
        isFocused: currentDate === visibleFocusDate.value,
      })
    }

    return items
  })

  function getVisibleRangeForWindowStart(targetWindowStartDate: string) {
    return {
      startDate: addDays(targetWindowStartDate, -CALENDAR_RANGE_BUFFER_BEFORE),
      endDate: addDays(
        targetWindowStartDate,
        DATE_WINDOW_SIZE - 1 + CALENDAR_RANGE_BUFFER_AFTER,
      ),
    }
  }

  const getRoomGroupSortOrder = (roomId: number) => {
    return roomToGroupSortOrderMap.value.get(roomId) ?? DEFAULT_SORT_ORDER
  }

  const getRoomTypeSortOrder = (roomType: string) => {
    return roomTypeSortOrderMap.value[roomType] ?? DEFAULT_SORT_ORDER
  }

  const getRoomSortOrder = (roomId: number) => {
    return roomSortOrderMap.value[roomId] ?? DEFAULT_SORT_ORDER
  }

  const priceSourceOptions = computed<RoomStatusPriceSourceOption[]>(() => {
    const options: RoomStatusPriceSourceOption[] = [
      {
        value: 'default',
        label: t('roomStatus.store.priceSource.default'),
      },
      ...mappedPricePlanOptions.value,
    ]

    if (
      cellPriceSource.value !== 'default' &&
      !options.some((item) => item.value === cellPriceSource.value)
    ) {
      options.push({
        value: cellPriceSource.value,
        label: t('roomStatus.store.priceSource.plan', {
          id: cellPriceSource.value.slice('plan:'.length),
        }),
      })
    }

    return options
  })

  const getRoomTypePricing = (roomTypeId: number, date: string) => {
    if (!showCellPrice.value) {
      return undefined
    }

    return getRoomStatusDailyPricing(
      roomTypeDailyPricingMap.value,
      roomTypeId,
      date,
      cellPriceSource.value,
      roomTypeDefaultPriceMap.value[roomTypeId],
    )
  }

  const compareCalendarRooms = (left: CalendarRoomDataDTO, right: CalendarRoomDataDTO) => {
    const groupOrderDiff = getRoomGroupSortOrder(left.roomId) - getRoomGroupSortOrder(right.roomId)
    if (groupOrderDiff !== 0) {
      return groupOrderDiff
    }

    const roomTypeOrderDiff = getRoomTypeSortOrder(left.roomType) - getRoomTypeSortOrder(right.roomType)
    if (roomTypeOrderDiff !== 0) {
      return roomTypeOrderDiff
    }

    const roomOrderDiff = getRoomSortOrder(left.roomId) - getRoomSortOrder(right.roomId)
    if (roomOrderDiff !== 0) {
      return roomOrderDiff
    }

    const roomTypeNameDiff = compareLocalizedText(left.roomType, right.roomType, NATURAL_COMPARE_OPTIONS)
    if (roomTypeNameDiff !== 0) {
      return roomTypeNameDiff
    }

    return compareLocalizedText(left.roomNumber, right.roomNumber, NATURAL_COMPARE_OPTIONS)
  }

  const sortedCalendarRooms = computed(() => {
    return [...calendarRooms.value].sort(compareCalendarRooms)
  })

  const roomTypes = computed(() => {
    const uniqueRoomTypes = new Set<string>()
    for (const room of sortedCalendarRooms.value) {
      uniqueRoomTypes.add(room.roomType)
    }
    return Array.from(uniqueRoomTypes)
  })

  const roomTypeSet = computed(() => new Set(selectedRoomTypes.value))

  const filteredRooms = computed(() => {
    if (roomTypeSet.value.size === 0) {
      return sortedCalendarRooms.value
    }

    return sortedCalendarRooms.value.filter((room) => roomTypeSet.value.has(room.roomType))
  })

  const visibleRoomItems = computed<RoomStatusRoomItem[]>(() => {
    const items: RoomStatusRoomItem[] = []
    const focusedDate = visibleFocusDate.value

    for (const room of filteredRooms.value) {
      const focusedStatus = room.dailyStatus.find((item) => item.date === focusedDate)
      if (!focusedStatus) {
        continue
      }

      const timeline: RoomTimelineItem[] = []
      const roomIsDirty = dirtyRoomIds.value.includes(room.roomId)
      const currentToday = getTodayDateKey()

      for (const dateItem of visibleDates.value) {
        const dailyStatus = room.dailyStatus.find((item) => item.date === dateItem.date)
        if (!dailyStatus) {
          continue
        }
        const businessState = getRoomBusinessState(dailyStatus)
        const reservation = buildReservationModel(dailyStatus, reservationAmountCache.value)
        const pricing =
          businessState === 'available' && !dailyStatus.closed && !reservation
            ? getRoomTypePricing(room.roomTypeId, dateItem.date)
            : undefined

        timeline.push({
          date: dateItem.date,
          label: dateItem.label,
          businessState,
          statusText: getRoomStatusText(dailyStatus),
          isSelected: dateItem.date === selectedDate.value,
          isFocused: dateItem.date === focusedDate,
          isToday: dateItem.date === currentToday,
          isClosed: Boolean(dailyStatus.closed),
          isDirty: roomIsDirty,
          closeType: dailyStatus.closeType || '',
          closeRemark: dailyStatus.closeRemark || '',
          reservation,
          price: pricing?.price,
          minStay: pricing?.minStay,
        })
      }

      items.push({
        roomId: room.roomId,
        roomNumber: room.roomNumber,
        roomTypeId: room.roomTypeId,
        roomType: room.roomType,
        focusedDate,
        focusedStatus: focusedStatus.status,
        focusedBusinessState: getRoomBusinessState(focusedStatus),
        focusedStatusText: getRoomStatusText(focusedStatus),
        focusedClosed: Boolean(focusedStatus.closed),
        closeType: focusedStatus.closeType || '',
        closeRemark: focusedStatus.closeRemark || '',
        isDirty: roomIsDirty,
        reservation: buildReservationModel(focusedStatus, reservationAmountCache.value),
        timeline,
      })
    }

    return items
  })

  const groupedVisibleRooms = computed(() => {
    const roomMap = new Map<string, RoomStatusRoomItem[]>()

    for (const room of visibleRoomItems.value) {
      const group = roomMap.get(room.roomType)
      if (group) {
        group.push(room)
      } else {
        roomMap.set(room.roomType, [room])
      }
    }

    return Array.from(roomMap.entries()).map(([roomType, rooms]) => ({ roomType, rooms }))
  })

  const roomTypeSummaries = computed<RoomTypeSummaryItem[]>(() => {
    const summaries: RoomTypeSummaryItem[] = []
    const focusedDate = visibleFocusDate.value

    for (const roomType of roomTypes.value) {
      const typeRooms = calendarRooms.value.filter((room) => room.roomType === roomType)
      let availableRooms = 0
      let occupiedRooms = 0
      let closedRooms = 0
      let dirtyRooms = 0

      for (const room of typeRooms) {
        const dailyStatus = room.dailyStatus.find((item) => item.date === focusedDate)
        if (!dailyStatus) {
          continue
        }

        const businessState = getRoomBusinessState(dailyStatus)

        if (dirtyRoomIds.value.includes(room.roomId)) {
          dirtyRooms += 1
        }

        if (
          businessState === 'closed' ||
          businessState === 'maintenance' ||
          businessState === 'retain' ||
          businessState === 'out_of_order'
        ) {
          closedRooms += 1
          continue
        }

        if (businessState === 'occupied' || businessState === 'reserved') {
          occupiedRooms += 1
          continue
        }

        availableRooms += 1
      }

      summaries.push({
        roomType,
        totalRooms: typeRooms.length,
        availableRooms,
        occupiedRooms,
        closedRooms,
        dirtyRooms,
        selected: selectedRoomTypes.value.includes(roomType),
      })
    }

    return summaries
  })

  const summaryCards = computed<SummaryCardItem[]>(() => {
    return [
      {
        key: 'arrivals',
        title: t('roomStatus.store.summary.arrivals'),
        value: statistics.value?.todayArrivals ?? 0,
        tone: 'primary',
      },
      {
        key: 'departures',
        title: t('roomStatus.store.summary.departures'),
        value: statistics.value?.todayDepartures ?? 0,
        tone: 'warning',
      },
      {
        key: 'available',
        title: t('roomStatus.store.summary.available'),
        value: statistics.value?.availableRooms ?? 0,
        tone: 'success',
      },
      {
        key: 'pending',
        title: t('roomStatus.store.summary.pending'),
        value: statistics.value?.pendingOrders ?? 0,
        tone: 'danger',
      },
    ]
  })

  function syncRoomTypeSelection() {
    if (roomTypes.value.length === 0) {
      selectedRoomTypes.value = []
      hasInitializedRoomTypeSelection = false
      return
    }

    const nextSelectedRoomTypes = selectedRoomTypes.value.filter((item) => roomTypes.value.includes(item))

    if (!hasInitializedRoomTypeSelection) {
      hasInitializedRoomTypeSelection = true

      if (nextSelectedRoomTypes.length > 0) {
        selectedRoomTypes.value = nextSelectedRoomTypes
        return
      }

      selectedRoomTypes.value = [...roomTypes.value]
      return
    }

    selectedRoomTypes.value = nextSelectedRoomTypes
  }

  function setSelectedRoomTypes(nextRoomTypes: string[]) {
    const validRoomTypes = Array.from(
      new Set(nextRoomTypes.filter((item) => roomTypes.value.includes(item))),
    )
    selectedRoomTypes.value = validRoomTypes
    hasInitializedRoomTypeSelection = true
  }

  function setCachedReservationAmount(reservationId: number, totalAmount: number) {
    reservationAmountCache.value = {
      ...reservationAmountCache.value,
      [reservationId]: totalAmount,
    }
    reservationAmountUnavailable.delete(reservationId)
  }

  function collectMissingReservationIds(targetDate: string, roomId?: number) {
    const reservationIds = new Set<number>()
    const rooms = roomId
      ? calendarRooms.value.filter((item) => item.roomId === roomId)
      : filteredRooms.value

    for (const room of rooms) {
      const dailyStatus = room.dailyStatus.find((item) => item.date === targetDate)
      if (!dailyStatus?.reservation?.id) {
        continue
      }

      const reservationId = dailyStatus.reservation.id
      if (dailyStatus.reservation.totalAmount !== null && dailyStatus.reservation.totalAmount !== undefined) {
        continue
      }
      if (hasReservationAmount(reservationAmountCache.value, reservationId)) {
        continue
      }
      if (reservationAmountUnavailable.has(reservationId)) {
        continue
      }

      reservationIds.add(reservationId)
    }

    return Array.from(reservationIds)
  }

  async function hydrateReservationAmount(reservationId: number) {
    if (hasReservationAmount(reservationAmountCache.value, reservationId)) {
      return
    }
    if (reservationAmountUnavailable.has(reservationId)) {
      return
    }

    const currentTask = reservationAmountPending.get(reservationId)
    if (currentTask) {
      await currentTask
      return
    }

    const task = (async () => {
      const response = await getReservationById(reservationId)
      if (!response.success || !response.data) {
      throw new Error(response.message || t('roomStatus.store.errors.reservationAmountFailed'))
      }

      const totalAmount = normalizeReservationAmount(response.data.totalAmount)
      if (totalAmount === undefined) {
        reservationAmountUnavailable.add(reservationId)
        return
      }

      setCachedReservationAmount(reservationId, totalAmount)
    })().finally(() => {
      reservationAmountPending.delete(reservationId)
    })

    reservationAmountPending.set(reservationId, task)
    await task
  }

  async function hydrateMissingReservationAmounts(targetDate = selectedDate.value, roomId?: number) {
    const reservationIds = collectMissingReservationIds(targetDate, roomId)
    if (reservationIds.length === 0) {
      return
    }

    await Promise.all(reservationIds.map((reservationId) => hydrateReservationAmount(reservationId)))
  }

  function resetSortContext() {
    roomTypeSortOrderMap.value = {}
    roomTypeDefaultPriceMap.value = {}
    roomSortOrderMap.value = {}
    roomToGroupSortOrderMap.value = new Map()
  }

  async function loadSortContext(force = false) {
    const userId = userStore.currentUser?.id

    if (!userId) {
      sortContextUserId.value = 0
      sortContextLoaded.value = true
      resetSortContext()
      return
    }

    if (!force && sortContextLoaded.value && sortContextUserId.value === userId) {
      return
    }

    try {
      const [roomTypeSortResult, roomSortResult, roomGroupSortResult, roomTypesResult, roomGroupsResult] =
        await Promise.allSettled([
          getSortOrderMap(userId, 'ROOM_TYPE'),
          getSortOrderMap(userId, 'ROOM'),
          getSortOrderMap(userId, 'GROUP'),
          getAllRoomTypesWithRooms(),
          getAllRoomGroups(),
        ])

      if (roomTypeSortResult.status === 'rejected') {
        console.error('加载房型排序配置失败', roomTypeSortResult.reason)
      }
      if (roomSortResult.status === 'rejected') {
        console.error('加载房间排序配置失败', roomSortResult.reason)
      }
      if (roomGroupSortResult.status === 'rejected') {
        console.error('加载分组排序配置失败', roomGroupSortResult.reason)
      }
      if (roomTypesResult.status === 'rejected') {
        console.error('加载房型列表失败', roomTypesResult.reason)
      }
      if (roomGroupsResult.status === 'rejected') {
        console.error('加载房间分组失败', roomGroupsResult.reason)
      }

      roomSortOrderMap.value =
        roomSortResult.status === 'fulfilled' && roomSortResult.value.success && roomSortResult.value.data
          ? roomSortResult.value.data
          : {}

      const nextRoomTypeSortOrderMap: Record<string, number> = {}
      const rawRoomTypeSortOrderMap =
        roomTypeSortResult.status === 'fulfilled' &&
        roomTypeSortResult.value.success &&
        roomTypeSortResult.value.data
          ? roomTypeSortResult.value.data
          : {}

      if (
        roomTypesResult.status === 'fulfilled' &&
        roomTypesResult.value.success &&
        Array.isArray(roomTypesResult.value.data)
      ) {
        const nextRoomTypeDefaultPriceMap: Record<number, number> = {}

        for (const roomType of roomTypesResult.value.data) {
          nextRoomTypeSortOrderMap[roomType.name] =
            rawRoomTypeSortOrderMap[roomType.id] ?? DEFAULT_SORT_ORDER

          const defaultPrice = Number(roomType.defaultPrice)
          if (Number.isFinite(defaultPrice) && defaultPrice > 0) {
            nextRoomTypeDefaultPriceMap[roomType.id] = defaultPrice
          }
        }

        roomTypeDefaultPriceMap.value = nextRoomTypeDefaultPriceMap
      } else {
        roomTypeDefaultPriceMap.value = {}
      }

      roomTypeSortOrderMap.value = nextRoomTypeSortOrderMap

      const roomGroupSortOrderMap =
        roomGroupSortResult.status === 'fulfilled' &&
        roomGroupSortResult.value.success &&
        roomGroupSortResult.value.data
          ? roomGroupSortResult.value.data
          : {}
      const nextRoomToGroupSortOrderMap = new Map<number, number>()

      if (
        roomGroupsResult.status === 'fulfilled' &&
        roomGroupsResult.value.success &&
        Array.isArray(roomGroupsResult.value.data)
      ) {
        const roomGroups = roomGroupsResult.value.data.filter(
          (group: RoomGroupDTO) => typeof group.id === 'number',
        ) as Array<RoomGroupDTO & { id: number }>

        const memberResponses = await Promise.all(
          roomGroups.map(async (group) => {
            try {
              const response = await getGroupMembers(group.id)
              if (!response.success || !Array.isArray(response.data)) {
                return [] as RoomGroupMemberDTO[]
              }
              return response.data
            } catch (error) {
              console.error(`加载房间分组成员失败，groupId=${group.id}`, error)
              return [] as RoomGroupMemberDTO[]
            }
          }),
        )

        for (let index = 0; index < roomGroups.length; index += 1) {
          const group = roomGroups[index]
          const members = memberResponses[index]
          const groupSortOrder = roomGroupSortOrderMap[group.id] ?? DEFAULT_SORT_ORDER

          for (const member of members) {
            const previousSortOrder = nextRoomToGroupSortOrderMap.get(member.roomId)
            if (previousSortOrder === undefined || groupSortOrder < previousSortOrder) {
              nextRoomToGroupSortOrderMap.set(member.roomId, groupSortOrder)
            }
          }
        }
      }

      roomToGroupSortOrderMap.value = nextRoomToGroupSortOrderMap
    } catch (error) {
      console.error('加载房态排序上下文失败', error)
      resetSortContext()
    } finally {
      sortContextUserId.value = userId
      sortContextLoaded.value = true
    }
  }

  async function fetchCalendarRooms(targetWindowStartDate = windowStartDate.value) {
    const targetRange = getVisibleRangeForWindowStart(targetWindowStartDate)
    const response = await getRoomStatusCalendar(targetRange.startDate, targetRange.endDate)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('roomStatus.store.errors.calendarLoadFailed'))
    }

    return response.data.rooms
  }

  async function fetchRoomStatusPricing(
    targetWindowStartDate = windowStartDate.value,
    source = cellPriceSource.value,
  ): Promise<RoomStatusDailyPricingMap> {
    const targetRange = getVisibleRangeForWindowStart(targetWindowStartDate)

    try {
      const response = await getRoomPriceManagementData(
        targetRange.startDate,
        targetRange.endDate,
        undefined,
        { suppressErrorStatuses: [403] },
      )

      if (!response.success || !Array.isArray(response.data)) {
        return {}
      }

      return buildRoomStatusDailyPricingMap(response.data, source)
    } catch (error) {
      console.warn('房态当日价格加载失败，已降级为状态展示', error)
      return {}
    }
  }

  async function fetchCalendarWindow(
    targetWindowStartDate = windowStartDate.value,
  ): Promise<CalendarWindowData> {
    latestPricingRequestId += 1
    const pricingRequestId = latestPricingRequestId
    const pricingMapPromise = showCellPrice.value
      ? fetchRoomStatusPricing(targetWindowStartDate, cellPriceSource.value)
      : Promise.resolve({})

    try {
      const rooms = await fetchCalendarRooms(targetWindowStartDate)
      return {
        rooms,
        pricingMapPromise,
        pricingRequestId,
        targetWindowStartDate,
      }
    } catch (error) {
      if (pricingRequestId === latestPricingRequestId) {
        latestPricingRequestId += 1
      }
      throw error
    }
  }

  function applyCalendarWindow({
    rooms,
    pricingMapPromise,
    pricingRequestId,
    targetWindowStartDate,
  }: CalendarWindowData) {
    calendarRooms.value = rooms
    roomTypeDailyPricingMap.value = {}
    syncRoomTypeSelection()

    if (showCellPrice.value) {
      void ensurePricePlanOptions()
    }

    if (showCellPrice.value && pricingRequestId !== latestPricingRequestId) {
      void reloadVisiblePricing()
      return
    }

    void pricingMapPromise.then((pricingMap) => {
      if (
        pricingRequestId !== latestPricingRequestId ||
        targetWindowStartDate !== windowStartDate.value
      ) {
        return
      }

      roomTypeDailyPricingMap.value = pricingMap
    })
  }

  function getCalendarRoomTypeIds() {
    return Array.from(
      new Set(
        calendarRooms.value
          .map((room) => Number(room.roomTypeId))
          .filter((roomTypeId) => Number.isInteger(roomTypeId) && roomTypeId > 0),
      ),
    ).sort((left, right) => left - right)
  }

  async function ensurePricePlanOptions(force = false) {
    const roomTypeIds = getCalendarRoomTypeIds()
    const roomTypeSignature = roomTypeIds.join(',')

    if (roomTypeIds.length === 0) {
      pricePlanOptionsGeneration += 1
      mappedPricePlanOptions.value = []
      loadedPricePlanRoomTypeSignature = ''
      pricePlanOptionsLoading.value = false
      return
    }

    if (!force && loadedPricePlanRoomTypeSignature === roomTypeSignature) {
      return
    }

    pricePlanOptionsGeneration += 1
    const generation = pricePlanOptionsGeneration
    if (loadedPricePlanRoomTypeSignature !== roomTypeSignature) {
      mappedPricePlanOptions.value = []
      loadedPricePlanRoomTypeSignature = ''
    }
    pricePlanOptionsLoading.value = true

    try {
      const responses = await Promise.allSettled(
        roomTypeIds.map((roomTypeId) => getPricePlansByRoomType(roomTypeId)),
      )

      if (
        generation !== pricePlanOptionsGeneration ||
        roomTypeSignature !== getCalendarRoomTypeIds().join(',')
      ) {
        return
      }

      const planNames = new Map<number, string>()
      let successfulResponseCount = 0

      for (const response of responses) {
        if (
          response.status !== 'fulfilled' ||
          !response.value.success ||
          !Array.isArray(response.value.data)
        ) {
          continue
        }

        successfulResponseCount += 1

        for (const relation of response.value.data) {
          const pricePlanId = Number(relation.pricePlanId ?? relation.pricePlan?.id)
          if (!Number.isInteger(pricePlanId) || pricePlanId <= 0) {
            continue
          }

          planNames.set(
            pricePlanId,
            relation.pricePlan?.name?.trim() ||
              t('roomStatus.store.priceSource.plan', { id: pricePlanId }),
          )
        }
      }

      if (successfulResponseCount === 0) {
        loadedPricePlanRoomTypeSignature = ''
        return
      }

      mappedPricePlanOptions.value = Array.from(planNames.entries())
        .sort((left, right) => compareLocalizedText(left[1], right[1]))
        .map(([pricePlanId, label]) => ({
          value: `plan:${pricePlanId}` as RoomStatusPriceSource,
          label,
        }))
      loadedPricePlanRoomTypeSignature =
        successfulResponseCount === responses.length ? roomTypeSignature : ''
    } catch (error) {
      console.warn('房态价盘选项加载失败', error)
    } finally {
      if (generation === pricePlanOptionsGeneration) {
        pricePlanOptionsLoading.value = false
      }
    }
  }

  async function reloadVisiblePricing() {
    latestPricingRequestId += 1
    const pricingRequestId = latestPricingRequestId
    const targetWindowStartDate = windowStartDate.value
    const source = cellPriceSource.value
    roomTypeDailyPricingMap.value = {}

    if (!showCellPrice.value) {
      return
    }

    const pricingMap = await fetchRoomStatusPricing(targetWindowStartDate, source)
    if (
      pricingRequestId !== latestPricingRequestId ||
      targetWindowStartDate !== windowStartDate.value ||
      source !== cellPriceSource.value ||
      !showCellPrice.value
    ) {
      return
    }

    roomTypeDailyPricingMap.value = pricingMap
  }

  async function setShowCellPrice(visible: boolean) {
    if (showCellPrice.value === visible) {
      return
    }

    showCellPrice.value = visible
    persistPricePreference(ROOM_STATUS_PRICE_VISIBLE_STORAGE_KEY, String(visible))
    latestPricingRequestId += 1
    roomTypeDailyPricingMap.value = {}

    if (!visible) {
      return
    }

    await Promise.all([ensurePricePlanOptions(), reloadVisiblePricing()])
  }

  async function setCellPriceSource(source: RoomStatusPriceSource) {
    const normalizedSource = normalizeRoomStatusPriceSource(source)
    if (cellPriceSource.value === normalizedSource) {
      return
    }

    cellPriceSource.value = normalizedSource
    persistPricePreference(ROOM_STATUS_PRICE_SOURCE_STORAGE_KEY, normalizedSource)

    if (showCellPrice.value) {
      await reloadVisiblePricing()
    }
  }

  async function loadCalendar() {
    applyCalendarWindow(await fetchCalendarWindow())
  }

  async function loadStatistics() {
    const response = await getRoomStatusStatistics(selectedDate.value)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('roomStatus.store.errors.statisticsLoadFailed'))
    }

    statistics.value = response.data
  }

  async function loadChannels(force = false) {
    if (!force && channels.value.length > 0) {
      return
    }

    const response = await getAllChannels()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('roomStatus.store.errors.channelsLoadFailed'))
    }

    channels.value = response.data.filter((item) => item.enabled)
  }

  async function initialize(force = false) {
    if (!force && calendarRooms.value.length > 0 && channels.value.length > 0) {
      await loadSortContext(false)
      await loadStatistics()
      await hydrateMissingReservationAmounts()
      return
    }

    loading.value = true
    try {
      await Promise.all([loadCalendar(), loadStatistics(), loadChannels(force), loadSortContext(force)])
      await hydrateMissingReservationAmounts()
    } finally {
      loading.value = false
    }
  }

  async function refreshAll(reloadSortContext = false) {
    loading.value = true
    try {
      const loadTasks = [loadCalendar(), loadStatistics()]
      if (channels.value.length === 0) {
        loadTasks.push(loadChannels())
      }
      if (reloadSortContext) {
        loadTasks.push(loadSortContext(true))
      }

      await Promise.all(loadTasks)
      await hydrateMissingReservationAmounts()
    } finally {
      loading.value = false
    }
  }

  function resolveWindowStartForDate(date: string) {
    if (isDateInVisibleWindow(date)) {
      return windowStartDate.value
    }

    return addDays(date, -SELECTED_DATE_OFFSET)
  }

  function resolveBrowsingDateForWindow(targetWindowStartDate: string) {
    return addDays(
      targetWindowStartDate,
      Math.min(SELECTED_DATE_OFFSET, DATE_WINDOW_SIZE - 1),
    )
  }

  async function setSelectedDate(date: string) {
    const nextWindowStartDate = resolveWindowStartForDate(date)
    const shouldMoveWindow = nextWindowStartDate !== windowStartDate.value
    selectedDate.value = date
    browsingDate.value = date
    summaryLoading.value = true
    try {
      const calendarTask = shouldMoveWindow
        ? fetchCalendarWindow(nextWindowStartDate)
        : fetchCalendarWindow()
      const [calendarWindow] = await Promise.all([calendarTask, loadStatistics()])
      if (shouldMoveWindow) {
        windowStartDate.value = nextWindowStartDate
      }
      applyCalendarWindow(calendarWindow)
      await hydrateMissingReservationAmounts(date)
    } finally {
      summaryLoading.value = false
    }
  }

  async function shiftWindow(days: number) {
    const nextWindowStartDate = addDays(windowStartDate.value, days)
    loading.value = true
    try {
      const calendarWindow = await fetchCalendarWindow(nextWindowStartDate)
      windowStartDate.value = nextWindowStartDate
      browsingDate.value = resolveBrowsingDateForWindow(nextWindowStartDate)
      applyCalendarWindow(calendarWindow)
      await hydrateMissingReservationAmounts(visibleFocusDate.value)
    } finally {
      loading.value = false
    }
  }

  async function goToday() {
    const currentToday = getTodayDateKey()
    selectedDate.value = currentToday
    browsingDate.value = currentToday
    windowStartDate.value = addDays(currentToday, -SELECTED_DATE_OFFSET)
    await refreshAll()
  }

  function toggleRoomType(roomType: string) {
    if (selectedRoomTypes.value.includes(roomType)) {
      selectedRoomTypes.value = selectedRoomTypes.value.filter((item) => item !== roomType)
      hasInitializedRoomTypeSelection = true
      return
    }

    selectedRoomTypes.value = [...selectedRoomTypes.value, roomType]
    hasInitializedRoomTypeSelection = true
  }

  function resetRoomTypeFilter() {
    selectedRoomTypes.value = [...roomTypes.value]
    hasInitializedRoomTypeSelection = true
  }

  async function runSearch(keyword: string) {
    const normalizedKeyword = keyword.trim()

    if (!normalizedKeyword || normalizedKeyword.length < 2) {
      clearSearchResults()
      return
    }

    latestSearchRequestId += 1
    const requestId = latestSearchRequestId
    searching.value = true
    hasSearchCompleted.value = false
    searchResults.value = []

    try {
      const response = await searchReservations(normalizedKeyword)
      if (requestId !== latestSearchRequestId) {
        return
      }

      if (!response.success || !response.data) {
        searchResults.value = []
        hasSearchCompleted.value = true
        throw new Error(response.message || t('roomStatus.store.errors.searchFailed'))
      }

      searchResults.value = response.data
      hasSearchCompleted.value = true
    } finally {
      if (requestId === latestSearchRequestId) {
        searching.value = false
      }
    }
  }

  function clearSearchResults() {
    latestSearchRequestId += 1
    searchResults.value = []
    hasSearchCompleted.value = false
    searching.value = false
  }

  function setDirtyState(roomIds: number[], dirty: boolean) {
    const nextRoomIds = new Set(dirtyRoomIds.value)
    for (const roomId of roomIds) {
      if (dirty) {
        nextRoomIds.add(roomId)
      } else {
        nextRoomIds.delete(roomId)
      }
    }
    dirtyRoomIds.value = Array.from(nextRoomIds)
    showSuccessToast(
      dirty ? t('roomStatus.store.toast.setDirty') : t('roomStatus.store.toast.setClean'),
    )
  }

  function toggleDirty(roomId: number) {
    const isDirty = dirtyRoomIds.value.includes(roomId)
    setDirtyState([roomId], !isDirty)
  }

  async function closeRooms(payload: {
    roomIds: number[]
    startDate: string
    endDate: string
    type: 'stop' | 'maintenance' | 'retain'
    remark?: string
    weekMode?: BatchWeekMode
  }) {
    const ranges = buildWeekModeRanges(payload.startDate, payload.endDate, payload.weekMode || 'all')
    actionLoading.value = true

    try {
      let affectedDays = 0

      for (const range of ranges) {
        const response = await closeRoomBlockouts({
          roomIds: payload.roomIds,
          startDate: range.startDate,
          endDate: range.endDate,
          type: payload.type,
          remark: payload.remark,
        })

        if (!response.success) {
          throw new Error(response.message || t('roomStatus.store.errors.closeFailed'))
        }

        affectedDays += response.data?.affectedDays ?? 0
      }

      showSuccessToast(t('roomStatus.store.toast.closeSaved', { count: affectedDays }))
      await refreshAll()
    } finally {
      actionLoading.value = false
    }
  }

  async function openRooms(payload: {
    roomIds: number[]
    startDate: string
    endDate: string
    weekMode?: BatchWeekMode
  }) {
    const ranges = buildWeekModeRanges(payload.startDate, payload.endDate, payload.weekMode || 'all')
    actionLoading.value = true

    try {
      let affectedDays = 0

      for (const range of ranges) {
        const response = await openRoomBlockouts({
          roomIds: payload.roomIds,
          startDate: range.startDate,
          endDate: range.endDate,
        })

        if (!response.success) {
          throw new Error(response.message || t('roomStatus.store.errors.openFailed'))
        }

        affectedDays += response.data?.affectedDays ?? 0
      }

      showSuccessToast(t('roomStatus.store.toast.opened', { count: affectedDays }))
      await refreshAll()
    } finally {
      actionLoading.value = false
    }
  }

  function getRoomListItemById(roomId: number, businessDate?: string) {
    const room = calendarRooms.value.find((item) => item.roomId === roomId)
    if (!room) {
      return null
    }

    const focusedDate = businessDate || visibleFocusDate.value
    const focusedStatus = room.dailyStatus.find((item) => item.date === focusedDate)
    if (!focusedStatus) {
      return null
    }

    const timeline: RoomTimelineItem[] = []
    const roomIsDirty = dirtyRoomIds.value.includes(room.roomId)
    const currentToday = getTodayDateKey()

    for (const dateItem of visibleDates.value) {
      const dailyStatus = room.dailyStatus.find((item) => item.date === dateItem.date)
      if (!dailyStatus) {
        continue
      }
      const businessState = getRoomBusinessState(dailyStatus)
      const reservation = buildReservationModel(dailyStatus, reservationAmountCache.value)
      const pricing =
        businessState === 'available' && !dailyStatus.closed && !reservation
          ? getRoomTypePricing(room.roomTypeId, dateItem.date)
          : undefined

      timeline.push({
        date: dateItem.date,
        label: dateItem.label,
        businessState,
        statusText: getRoomStatusText(dailyStatus),
        isSelected: dateItem.date === selectedDate.value,
        isFocused: dateItem.date === focusedDate,
        isToday: dateItem.date === currentToday,
        isClosed: Boolean(dailyStatus.closed),
        isDirty: roomIsDirty,
        closeType: dailyStatus.closeType || '',
        closeRemark: dailyStatus.closeRemark || '',
        reservation,
        price: pricing?.price,
        minStay: pricing?.minStay,
      })
    }

    return {
      roomId: room.roomId,
      roomNumber: room.roomNumber,
      roomTypeId: room.roomTypeId,
      roomType: room.roomType,
      focusedDate,
      focusedStatus: focusedStatus.status,
      focusedBusinessState: getRoomBusinessState(focusedStatus),
      focusedStatusText: getRoomStatusText(focusedStatus),
      focusedClosed: Boolean(focusedStatus.closed),
      closeType: focusedStatus.closeType || '',
      closeRemark: focusedStatus.closeRemark || '',
      isDirty: roomIsDirty,
      reservation: buildReservationModel(focusedStatus, reservationAmountCache.value),
      timeline,
    } satisfies RoomStatusRoomItem
  }

  return {
    selectedDate,
    browsingDate,
    windowStartDate,
    calendarRooms,
    statistics,
    channels,
    selectedRoomTypes,
    searchResults,
    hasSearchCompleted,
    loading,
    summaryLoading,
    searching,
    actionLoading,
    showCellPrice,
    cellPriceSource,
    priceSourceOptions,
    pricePlanOptionsLoading,
    visibleDates,
    visibleFocusDate,
    roomTypes,
    visibleRoomItems,
    groupedVisibleRooms,
    roomTypeSummaries,
    summaryCards,
    initialize,
    refreshAll,
    setSelectedDate,
    shiftWindow,
    goToday,
    toggleRoomType,
    resetRoomTypeFilter,
    setSelectedRoomTypes,
    setShowCellPrice,
    setCellPriceSource,
    runSearch,
    clearSearchResults,
    toggleDirty,
    setDirtyState,
    closeRooms,
    openRooms,
    hydrateMissingReservationAmounts,
    getRoomListItemById,
  }
})
