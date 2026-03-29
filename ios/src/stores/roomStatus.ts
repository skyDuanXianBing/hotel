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
import {
  getAllChannels,
  getReservationById,
  searchReservations,
  type ChannelDTO,
  type ReservationDTO,
} from '@/api/reservation'
import { showSuccessToast } from '@/utils/notify'

export type BatchWeekMode = 'all' | 'weekday' | 'weekend'

export interface RoomStatusDateItem {
  date: string
  label: string
  weekday: string
  availableRooms: number
  isToday: boolean
  isSelected: boolean
}

export interface RoomTimelineItem {
  date: string
  label: string
  businessState: RoomStatusBusinessState
  statusText: string
  isSelected: boolean
  isToday: boolean
  isClosed: boolean
}

export interface RoomStatusRoomItem {
  roomId: number
  roomNumber: string
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

export type RoomStatusBusinessState =
  | 'available'
  | 'occupied'
  | 'reserved'
  | 'maintenance'
  | 'out_of_order'
  | 'closed'
  | 'unknown'

const WEEKDAY_LABELS = ['日', '一', '二', '三', '四', '五', '六']

export const formatDateKey = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const addDays = (date: string, amount: number) => {
  const nextDate = new Date(date)
  nextDate.setDate(nextDate.getDate() + amount)
  return formatDateKey(nextDate)
}

export const getReservationStatusText = (status?: string) => {
  if (!status) {
    return '未知状态'
  }
  if (status === 'CONFIRMED' || status === 'confirmed') {
    return '已预订'
  }
  if (status === 'CHECKED_IN' || status === 'checked_in') {
    return '已入住'
  }
  if (status === 'CHECKED_OUT' || status === 'checked_out') {
    return '已退房'
  }
  if (status === 'CANCELLED' || status === 'cancelled') {
    return '已取消'
  }
  if (status === 'NO_SHOW' || status === 'no_show') {
    return '未到店'
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

  if (businessState === 'closed' && dailyStatus) {
    if (dailyStatus.closeType === 'maintenance') {
      return '维修房'
    }
    if (dailyStatus.closeType === 'retain') {
      return '保留房'
    }
    return '停用房'
  }

  if (businessState === 'available') {
    return '可售'
  }
  if (businessState === 'occupied') {
    return '已入住'
  }
  if (businessState === 'reserved') {
    return '已预订'
  }
  if (businessState === 'maintenance') {
    return '维修'
  }
  if (businessState === 'out_of_order') {
    return '停用'
  }

  if (!dailyStatus) {
    return '未知'
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
    throw new Error('订单金额必须是有效数字')
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
  const start = new Date(startDate)
  const end = new Date(endDate)
  let rangeStart = ''

  while (start <= end) {
    const day = start.getDay()
    const matched = weekMode === 'weekday' ? day !== 0 && day !== 6 : day === 0 || day === 6
    const currentDate = formatDateKey(start)

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

    start.setDate(start.getDate() + 1)
  }

  if (rangeStart) {
    ranges.push({
      startDate: rangeStart,
      endDate,
    })
  }

  return ranges.map((item) => ({
    startDate: item.startDate,
    endDate: typeof item.endDate === 'string' ? item.endDate : formatDateKey(item.endDate),
  }))
}

export const useRoomStatusStore = defineStore('roomStatus', () => {
  const today = formatDateKey(new Date())
  const selectedDate = ref(today)
  const windowStartDate = ref(addDays(today, -1))
  const calendarRooms = ref<CalendarRoomDataDTO[]>([])
  const statistics = ref<RoomStatusStatisticsDTO | null>(null)
  const channels = ref<ChannelDTO[]>([])
  const selectedRoomTypes = ref<string[]>([])
  const searchResults = ref<ReservationDTO[]>([])
  const dirtyRoomIds = ref<number[]>([])
  const loading = ref(false)
  const summaryLoading = ref(false)
  const searching = ref(false)
  const actionLoading = ref(false)
  const reservationAmountCache = ref<Record<number, number>>({})
  const reservationAmountPending = new Map<number, Promise<void>>()
  const reservationAmountUnavailable = new Set<number>()

  const visibleDates = computed<RoomStatusDateItem[]>(() => {
    const items: RoomStatusDateItem[] = []

    for (let index = 0; index < 5; index += 1) {
      const currentDate = addDays(windowStartDate.value, index)
      const dateObject = new Date(currentDate)
      let availableRooms = 0

      for (const room of calendarRooms.value) {
        const dailyStatus = room.dailyStatus.find((item) => item.date === currentDate)
        if (dailyStatus && !dailyStatus.closed && !dailyStatus.reservation && dailyStatus.status === 'AVAILABLE') {
          availableRooms += 1
        }
      }

      items.push({
        date: currentDate,
        label: `${String(dateObject.getMonth() + 1).padStart(2, '0')}-${String(dateObject.getDate()).padStart(2, '0')}`,
        weekday: WEEKDAY_LABELS[dateObject.getDay()],
        availableRooms,
        isToday: currentDate === today,
        isSelected: currentDate === selectedDate.value,
      })
    }

    return items
  })

  const visibleRange = computed(() => {
    return {
      startDate: addDays(windowStartDate.value, -2),
      endDate: addDays(windowStartDate.value, 11),
    }
  })

  const roomTypes = computed(() => {
    const uniqueRoomTypes = new Set<string>()
    for (const room of calendarRooms.value) {
      uniqueRoomTypes.add(room.roomType)
    }
    return Array.from(uniqueRoomTypes)
  })

  const roomTypeSet = computed(() => new Set(selectedRoomTypes.value))

  const filteredRooms = computed(() => {
    if (roomTypeSet.value.size === 0) {
      return calendarRooms.value
    }

    return calendarRooms.value.filter((room) => roomTypeSet.value.has(room.roomType))
  })

  const visibleRoomItems = computed<RoomStatusRoomItem[]>(() => {
    const items: RoomStatusRoomItem[] = []

    for (const room of filteredRooms.value) {
      const focusedStatus = room.dailyStatus.find((item) => item.date === selectedDate.value)
      if (!focusedStatus) {
        continue
      }

      const timeline: RoomTimelineItem[] = []
      for (const dateItem of visibleDates.value) {
        const dailyStatus = room.dailyStatus.find((item) => item.date === dateItem.date)
        if (!dailyStatus) {
          continue
        }

        timeline.push({
          date: dateItem.date,
          label: dateItem.label,
          businessState: getRoomBusinessState(dailyStatus),
          statusText: getRoomStatusText(dailyStatus),
          isSelected: dateItem.date === selectedDate.value,
          isToday: dateItem.date === today,
          isClosed: Boolean(dailyStatus.closed),
        })
      }

      items.push({
        roomId: room.roomId,
        roomNumber: room.roomNumber,
        roomType: room.roomType,
        focusedDate: selectedDate.value,
        focusedStatus: focusedStatus.status,
        focusedBusinessState: getRoomBusinessState(focusedStatus),
        focusedStatusText: getRoomStatusText(focusedStatus),
        focusedClosed: Boolean(focusedStatus.closed),
        closeType: focusedStatus.closeType || '',
        closeRemark: focusedStatus.closeRemark || '',
        isDirty: dirtyRoomIds.value.includes(room.roomId),
        reservation: buildReservationModel(focusedStatus, reservationAmountCache.value),
        timeline,
      })
    }

    return items.sort((left, right) => {
      if (left.roomType === right.roomType) {
        return left.roomNumber.localeCompare(right.roomNumber, 'zh-CN')
      }
      return left.roomType.localeCompare(right.roomType, 'zh-CN')
    })
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

    for (const roomType of roomTypes.value) {
      const typeRooms = calendarRooms.value.filter((room) => room.roomType === roomType)
      let availableRooms = 0
      let occupiedRooms = 0
      let closedRooms = 0
      let dirtyRooms = 0

      for (const room of typeRooms) {
        const dailyStatus = room.dailyStatus.find((item) => item.date === selectedDate.value)
        if (!dailyStatus) {
          continue
        }

        if (dirtyRoomIds.value.includes(room.roomId)) {
          dirtyRooms += 1
        }

        if (dailyStatus.closed) {
          closedRooms += 1
          continue
        }

        if (dailyStatus.reservation || dailyStatus.status !== 'AVAILABLE') {
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
        title: '今日预抵',
        value: statistics.value?.todayArrivals ?? 0,
        tone: 'primary',
      },
      {
        key: 'departures',
        title: '今日预离',
        value: statistics.value?.todayDepartures ?? 0,
        tone: 'warning',
      },
      {
        key: 'available',
        title: '可售房',
        value: statistics.value?.availableRooms ?? 0,
        tone: 'success',
      },
      {
        key: 'pending',
        title: '待处理',
        value: statistics.value?.pendingOrders ?? 0,
        tone: 'danger',
      },
    ]
  })

  function syncRoomTypeSelection() {
    if (roomTypes.value.length === 0) {
      selectedRoomTypes.value = []
      return
    }

    if (selectedRoomTypes.value.length === 0) {
      selectedRoomTypes.value = [...roomTypes.value]
      return
    }

    const nextSelectedRoomTypes = selectedRoomTypes.value.filter((item) => roomTypes.value.includes(item))
    if (nextSelectedRoomTypes.length === 0) {
      selectedRoomTypes.value = [...roomTypes.value]
      return
    }

    selectedRoomTypes.value = nextSelectedRoomTypes
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
        throw new Error(response.message || '订单金额补全失败')
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

  async function loadCalendar() {
    const response = await getRoomStatusCalendar(visibleRange.value.startDate, visibleRange.value.endDate)
    if (!response.success || !response.data) {
      throw new Error(response.message || '房态加载失败')
    }

    calendarRooms.value = response.data.rooms
    syncRoomTypeSelection()
  }

  async function loadStatistics() {
    const response = await getRoomStatusStatistics(selectedDate.value)
    if (!response.success || !response.data) {
      throw new Error(response.message || '房态摘要加载失败')
    }

    statistics.value = response.data
  }

  async function loadChannels(force = false) {
    if (!force && channels.value.length > 0) {
      return
    }

    const response = await getAllChannels()
    if (!response.success || !response.data) {
      throw new Error(response.message || '渠道列表加载失败')
    }

    channels.value = response.data.filter((item) => item.enabled)
  }

  async function initialize(force = false) {
    if (!force && calendarRooms.value.length > 0 && channels.value.length > 0) {
      await loadStatistics()
      await hydrateMissingReservationAmounts()
      return
    }

    loading.value = true
    try {
      await Promise.all([loadCalendar(), loadStatistics(), loadChannels(force)])
      await hydrateMissingReservationAmounts()
    } finally {
      loading.value = false
    }
  }

  async function refreshAll() {
    loading.value = true
    try {
      const loadTasks = [loadCalendar(), loadStatistics()]
      if (channels.value.length === 0) {
        loadTasks.push(loadChannels())
      }

      await Promise.all(loadTasks)
      await hydrateMissingReservationAmounts()
    } finally {
      loading.value = false
    }
  }

  function ensureDateInWindow(date: string) {
    const visibleDateKeys = visibleDates.value.map((item) => item.date)
    if (visibleDateKeys.includes(date)) {
      return
    }

    windowStartDate.value = addDays(date, -1)
  }

  async function setSelectedDate(date: string) {
    selectedDate.value = date
    ensureDateInWindow(date)
    summaryLoading.value = true
    try {
      await Promise.all([loadCalendar(), loadStatistics()])
      await hydrateMissingReservationAmounts(date)
    } finally {
      summaryLoading.value = false
    }
  }

  async function shiftWindow(days: number) {
    windowStartDate.value = addDays(windowStartDate.value, days)
    selectedDate.value = addDays(selectedDate.value, days)
    await refreshAll()
  }

  async function goToday() {
    selectedDate.value = today
    windowStartDate.value = addDays(today, -1)
    await refreshAll()
  }

  function toggleRoomType(roomType: string) {
    if (selectedRoomTypes.value.includes(roomType)) {
      selectedRoomTypes.value = selectedRoomTypes.value.filter((item) => item !== roomType)
      if (selectedRoomTypes.value.length === 0) {
        selectedRoomTypes.value = [...roomTypes.value]
      }
      return
    }

    selectedRoomTypes.value = [...selectedRoomTypes.value, roomType]
  }

  function resetRoomTypeFilter() {
    selectedRoomTypes.value = [...roomTypes.value]
  }

  async function runSearch(keyword: string) {
    if (!keyword || keyword.trim().length < 2) {
      searchResults.value = []
      return
    }

    searching.value = true
    try {
      const response = await searchReservations(keyword.trim())
      if (!response.success || !response.data) {
        searchResults.value = []
        throw new Error(response.message || '订单搜索失败')
      }

      searchResults.value = response.data
    } finally {
      searching.value = false
    }
  }

  function clearSearchResults() {
    searchResults.value = []
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
    showSuccessToast(dirty ? '已置脏房' : '已置净房')
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
          throw new Error(response.message || '关房失败')
        }

        affectedDays += response.data?.affectedDays ?? 0
      }

      showSuccessToast(`已保存关房设置（${affectedDays} 天）`)
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
          throw new Error(response.message || '开房失败')
        }

        affectedDays += response.data?.affectedDays ?? 0
      }

      showSuccessToast(`已开房（${affectedDays} 天）`)
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

    const focusedDate = businessDate || selectedDate.value
    const focusedStatus = room.dailyStatus.find((item) => item.date === focusedDate)
    if (!focusedStatus) {
      return null
    }

    const timeline: RoomTimelineItem[] = []
    for (const dateItem of visibleDates.value) {
      const dailyStatus = room.dailyStatus.find((item) => item.date === dateItem.date)
      if (!dailyStatus) {
        continue
      }
      timeline.push({
        date: dateItem.date,
        label: dateItem.label,
        businessState: getRoomBusinessState(dailyStatus),
        statusText: getRoomStatusText(dailyStatus),
        isSelected: dateItem.date === focusedDate,
        isToday: dateItem.date === today,
        isClosed: Boolean(dailyStatus.closed),
      })
    }

    return {
      roomId: room.roomId,
      roomNumber: room.roomNumber,
      roomType: room.roomType,
      focusedDate,
      focusedStatus: focusedStatus.status,
      focusedBusinessState: getRoomBusinessState(focusedStatus),
      focusedStatusText: getRoomStatusText(focusedStatus),
      focusedClosed: Boolean(focusedStatus.closed),
      closeType: focusedStatus.closeType || '',
      closeRemark: focusedStatus.closeRemark || '',
      isDirty: dirtyRoomIds.value.includes(room.roomId),
      reservation: buildReservationModel(focusedStatus, reservationAmountCache.value),
      timeline,
    } satisfies RoomStatusRoomItem
  }

  return {
    selectedDate,
    windowStartDate,
    calendarRooms,
    statistics,
    channels,
    selectedRoomTypes,
    searchResults,
    loading,
    summaryLoading,
    searching,
    actionLoading,
    visibleDates,
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
