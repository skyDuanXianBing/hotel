import { beforeEach, describe, expect, test, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const apiMocks = vi.hoisted(() => ({
  getRoomStatusCalendar: vi.fn(),
  getRoomStatusStatistics: vi.fn(),
  getRoomPriceManagementData: vi.fn(),
  getPricePlansByRoomType: vi.fn(),
  getAllRoomTypesWithRooms: vi.fn(),
}))

vi.mock('@/api/roomStatus', () => ({
  closeRoomBlockouts: vi.fn(),
  getRoomStatusCalendar: apiMocks.getRoomStatusCalendar,
  getRoomStatusStatistics: apiMocks.getRoomStatusStatistics,
  openRoomBlockouts: vi.fn(),
}))

vi.mock('@/api/roomGroup', () => ({
  getAllRoomGroups: vi.fn().mockResolvedValue({ success: true, data: [] }),
  getGroupMembers: vi.fn(),
}))

vi.mock('@/api/roomPrice', () => ({
  getRoomPriceManagementData: apiMocks.getRoomPriceManagementData,
}))

vi.mock('@/api/pricePlan', () => ({
  getPricePlansByRoomType: apiMocks.getPricePlansByRoomType,
}))

vi.mock('@/api/roomType', () => ({
  getAllRoomTypesWithRooms: apiMocks.getAllRoomTypesWithRooms,
}))

vi.mock('@/api/reservation', () => ({
  getAllChannels: vi.fn().mockResolvedValue({ success: true, data: [] }),
  getReservationById: vi.fn(),
  searchReservations: vi.fn(),
}))

vi.mock('@/api/sortConfig', () => ({
  getSortOrderMap: vi.fn().mockResolvedValue({ success: true, data: {} }),
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    currentUser: { id: 1 },
  }),
}))

vi.mock('@/utils/notify', () => ({
  showSuccessToast: vi.fn(),
}))

import {
  ROOM_STATUS_PRICE_SOURCE_STORAGE_KEY,
  ROOM_STATUS_PRICE_VISIBLE_STORAGE_KEY,
  useRoomStatusStore,
} from '@/stores/roomStatus'
import { shiftBusinessDate } from '@/utils/storeBusinessDate'

interface CalendarRoomDefinition {
  roomId: number
  roomNumber: string
  roomTypeId: number
  roomType: string
}

const buildDailyStatuses = (startDate: string, endDate: string) => {
  const statuses = []
  let date = startDate

  while (date <= endDate) {
    statuses.push({
      date,
      status: 'AVAILABLE',
      closed: false,
    })
    date = shiftBusinessDate(date, 1)
  }

  return statuses
}

const buildPlanRelation = (pricePlanId: number, name: string) => ({
  id: pricePlanId,
  roomTypeId: 10,
  pricePlanId,
  pricePlan: {
    id: pricePlanId,
    name,
  },
  maxGuests: 2,
})

const createDeferred = <T>() => {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((resolvePromise) => {
    resolve = resolvePromise
  })

  return { promise, resolve }
}

describe('room status price preferences', () => {
  let calendarRoomDefinitions: CalendarRoomDefinition[]

  beforeEach(() => {
    window.localStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
    calendarRoomDefinitions = []
    apiMocks.getRoomStatusCalendar.mockImplementation((startDate: string, endDate: string) => {
      return Promise.resolve({
        success: true,
        data: {
          dateRange: {
            startDate,
            endDate,
          },
          rooms: calendarRoomDefinitions.map((room) => ({
            ...room,
            dailyStatus: buildDailyStatuses(startDate, endDate),
          })),
        },
      })
    })
    apiMocks.getRoomStatusStatistics.mockResolvedValue({
      success: true,
      data: {
        date: '2026-07-17',
        todayArrivals: 0,
        todayDepartures: 0,
        todayNewOrders: 0,
        availableRooms: 0,
        unassignedOrders: 0,
        pendingOrders: 0,
      },
    })
    apiMocks.getRoomPriceManagementData.mockResolvedValue({
      success: true,
      data: [],
    })
    apiMocks.getPricePlansByRoomType.mockResolvedValue({
      success: true,
      data: [],
    })
    apiMocks.getAllRoomTypesWithRooms.mockResolvedValue({
      success: true,
      data: [],
    })
  })

  test('defaults to hidden and does not request management prices while hidden', async () => {
    const store = useRoomStatusStore()

    await store.initialize()

    expect(store.showCellPrice).toBe(false)
    expect(apiMocks.getRoomPriceManagementData).not.toHaveBeenCalled()

    await store.shiftWindow(14)

    expect(apiMocks.getRoomPriceManagementData).not.toHaveBeenCalled()
  })

  test('persists visibility and an explicit price-plan source', async () => {
    const store = useRoomStatusStore()

    await store.initialize()
    await store.setShowCellPrice(true)
    await store.setCellPriceSource('plan:100')

    expect(apiMocks.getRoomPriceManagementData).toHaveBeenCalledTimes(2)
    expect(window.localStorage.getItem(ROOM_STATUS_PRICE_VISIBLE_STORAGE_KEY)).toBe('true')
    expect(window.localStorage.getItem(ROOM_STATUS_PRICE_SOURCE_STORAGE_KEY)).toBe('plan:100')

    setActivePinia(createPinia())
    const restoredStore = useRoomStatusStore()

    expect(restoredStore.showCellPrice).toBe(true)
    expect(restoredStore.cellPriceSource).toBe('plan:100')
  })

  test('retries price-plan options after a failed load and clears them for an empty calendar', async () => {
    calendarRoomDefinitions = [
      {
        roomId: 1,
        roomNumber: '101',
        roomTypeId: 10,
        roomType: '标准房',
      },
    ]
    apiMocks.getPricePlansByRoomType
      .mockRejectedValueOnce(new Error('temporary failure'))
      .mockResolvedValue({
        success: true,
        data: [buildPlanRelation(100, '标准价盘')],
      })
    const store = useRoomStatusStore()

    await store.initialize()
    await store.setShowCellPrice(true)

    expect(store.priceSourceOptions.map((option) => option.value)).toEqual(['default'])

    await store.refreshAll()
    await vi.waitFor(() => {
      expect(store.priceSourceOptions.map((option) => option.value)).toContain('plan:100')
    })
    expect(apiMocks.getPricePlansByRoomType).toHaveBeenCalledTimes(2)

    calendarRoomDefinitions = []
    await store.shiftWindow(14)

    expect(store.priceSourceOptions.map((option) => option.value)).toEqual(['default'])
    expect(store.pricePlanOptionsLoading).toBe(false)
  })

  test('ignores stale price-plan option responses and their loading cleanup', async () => {
    calendarRoomDefinitions = [
      {
        roomId: 1,
        roomNumber: '101',
        roomTypeId: 10,
        roomType: '标准房',
      },
    ]
    const staleOptions = createDeferred<{
      success: true
      data: ReturnType<typeof buildPlanRelation>[]
    }>()
    apiMocks.getPricePlansByRoomType.mockImplementation((roomTypeId: number) => {
      if (roomTypeId === 10) {
        return staleOptions.promise
      }

      return Promise.resolve({
        success: true,
        data: [buildPlanRelation(200, '新价盘')],
      })
    })
    const store = useRoomStatusStore()

    await store.initialize()
    const showPriceTask = store.setShowCellPrice(true)
    await vi.waitFor(() => {
      expect(apiMocks.getPricePlansByRoomType).toHaveBeenCalledWith(10)
    })

    calendarRoomDefinitions = [
      {
        roomId: 2,
        roomNumber: '201',
        roomTypeId: 20,
        roomType: '套房',
      },
    ]
    await store.shiftWindow(14)
    await vi.waitFor(() => {
      expect(store.priceSourceOptions.map((option) => option.value)).toContain('plan:200')
      expect(store.pricePlanOptionsLoading).toBe(false)
    })

    staleOptions.resolve({
      success: true,
      data: [buildPlanRelation(100, '旧价盘')],
    })
    await showPriceTask

    expect(store.priceSourceOptions.map((option) => option.value)).toEqual([
      'default',
      'plan:200',
    ])
    expect(store.pricePlanOptionsLoading).toBe(false)
  })

  test('does not let an older source response overwrite the latest selected source', async () => {
    calendarRoomDefinitions = [
      {
        roomId: 1,
        roomNumber: '101',
        roomTypeId: 10,
        roomType: '标准房',
      },
    ]
    apiMocks.getPricePlansByRoomType.mockResolvedValue({
      success: true,
      data: [buildPlanRelation(100, '标准价盘')],
    })
    const store = useRoomStatusStore()

    await store.initialize()
    const priceDate = store.visibleDates[0].date
    const staleDefaultPrice = createDeferred<{
      success: true
      data: Array<{
        id: number
        roomTypeId: number
        roomTypeName: string
        roomTypeCode: string
        priceDate: string
        price: number
        isWeekend: boolean
        isHoliday: boolean
      }>
    }>()
    apiMocks.getRoomPriceManagementData
      .mockImplementationOnce(() => staleDefaultPrice.promise)
      .mockResolvedValueOnce({
        success: true,
        data: [
          {
            id: 2,
            roomTypeId: 10,
            roomTypeName: '标准房',
            roomTypeCode: 'STD',
            pricePlanId: 100,
            priceDate,
            price: 222.25,
            isWeekend: false,
            isHoliday: false,
          },
        ],
      })

    const showPriceTask = store.setShowCellPrice(true)
    await store.setCellPriceSource('plan:100')

    expect(
      store.visibleRoomItems[0].timeline.find((item) => item.date === priceDate)?.price,
    ).toBe(222.25)

    staleDefaultPrice.resolve({
      success: true,
      data: [
        {
          id: 1,
          roomTypeId: 10,
          roomTypeName: '标准房',
          roomTypeCode: 'STD',
          priceDate,
          price: 111.5,
          isWeekend: false,
          isHoliday: false,
        },
      ],
    })
    await showPriceTask

    expect(store.cellPriceSource).toBe('plan:100')
    expect(
      store.visibleRoomItems[0].timeline.find((item) => item.date === priceDate)?.price,
    ).toBe(222.25)
  })
})
