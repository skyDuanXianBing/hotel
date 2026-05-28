import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { request } from '@/utils/request'
import { i18n } from '@/locales'
import type {
  RoomStatusCalendar,
  DailyRoomStatusData,
  Channel,
  RoomType,
  Room,
  RoomStatus,
  RoomStatusLog,
  ApiResponse,
} from '@/types/room'

const translate = (key: string) => i18n.global.t(key)

export const useRoomStatusStore = defineStore('roomStatus', () => {
  const calendarData = ref<RoomStatusCalendar | null>(null)
  const dailyData = ref<DailyRoomStatusData | null>(null)
  const channels = ref<Channel[]>([])
  const roomTypes = ref<RoomType[]>([])
  const rooms = ref<Room[]>([])
  const statusLogs = ref<RoomStatusLog[]>([])

  const loading = ref(false)
  const error = ref<string | null>(null)

  const activeTab = ref<'calendar' | 'daily' | 'channel'>('calendar')

  const selectedDate = ref<string>(new Date().toISOString().split('T')[0])
  const dateRange = computed(() => {
    if (!calendarData.value) return { startDate: '', endDate: '' }
    return calendarData.value.dateRange
  })

  const fetchCalendarData = async (startDate: string, endDate: string) => {
    try {
      loading.value = true
      error.value = null

      const response: ApiResponse<RoomStatusCalendar> = await request.get(
        `/api/v1/room-status/calendar?startDate=${startDate}&endDate=${endDate}`,
      )

      if (response.success) {
        calendarData.value = response.data
      } else {
        error.value = response.message
      }
    } catch (err) {
      error.value = translate('stage6.common.messages.fetchRoomStatusCalendarFailed')
      console.error('Failed to fetch room status calendar data:', err)
    } finally {
      loading.value = false
    }
  }

  const fetchDailyData = async (date: string) => {
    try {
      loading.value = true
      error.value = null

      const response: ApiResponse<DailyRoomStatusData> = await request.get(
        `/api/v1/room-status/daily?date=${date}`,
      )

      if (response.success) {
        dailyData.value = response.data
      } else {
        error.value = response.message
      }
    } catch (err) {
      error.value = translate('stage6.common.messages.fetchDailyRoomStatusFailed')
      console.error('Failed to fetch daily room status data:', err)
    } finally {
      loading.value = false
    }
  }

  const fetchChannels = async () => {
    try {
      const response: ApiResponse<Channel[]> = await request.get('/api/v1/channels')

      if (response.success) {
        channels.value = response.data
      } else {
        error.value = response.message
      }
    } catch (err) {
      error.value = translate('stage6.common.messages.fetchChannelsFailed')
      console.error('Failed to fetch channel list:', err)
    }
  }

  const fetchRoomTypes = async () => {
    try {
      const response: ApiResponse<RoomType[]> = await request.get('/api/v1/room-types')

      if (response.success) {
        roomTypes.value = response.data
      } else {
        error.value = response.message
      }
    } catch (err) {
      error.value = translate('stage6.common.messages.fetchRoomTypesFailed')
      console.error('Failed to fetch room type list:', err)
    }
  }

  const fetchRooms = async (roomTypeId?: number, date?: string) => {
    try {
      let url = '/api/v1/rooms'
      const params = new URLSearchParams()

      if (roomTypeId) params.append('roomTypeId', roomTypeId.toString())
      if (date) params.append('date', date)

      if (params.toString()) {
        url += '?' + params.toString()
      }

      const response: ApiResponse<Room[]> = await request.get(url)

      if (response.success) {
        rooms.value = response.data
      } else {
        error.value = response.message
      }
    } catch (err) {
      error.value = translate('stage6.common.messages.fetchRoomsFailed')
      console.error('Failed to fetch room list:', err)
    }
  }

  const updateRoomStatus = async (
    roomId: number,
    date: string,
    status: RoomStatus,
    reason?: string,
  ) => {
    try {
      loading.value = true
      error.value = null

      const response: ApiResponse<any> = await request.put(`/api/v1/room-status/${roomId}`, {
        date,
        status,
        reason,
      })

      if (response.success) {
        await fetchDailyData(date)
        if (calendarData.value) {
          await fetchCalendarData(
            calendarData.value.dateRange.startDate,
            calendarData.value.dateRange.endDate,
          )
        }
        return true
      } else {
        error.value = response.message
        return false
      }
    } catch (err) {
      error.value = translate('stage6.common.messages.updateRoomStatusFailed')
      console.error('Failed to update room status:', err)
      return false
    } finally {
      loading.value = false
    }
  }

  const fetchStatusLogs = async (startDate: string, endDate: string) => {
    try {
      const response: ApiResponse<RoomStatusLog[]> = await request.get(
        `/api/v1/room-status/logs?startDate=${startDate}&endDate=${endDate}`,
      )

      if (response.success) {
        statusLogs.value = response.data
      } else {
        error.value = response.message
      }
    } catch (err) {
      error.value = translate('stage6.common.messages.fetchRoomStatusLogsFailed')
      console.error('Failed to fetch room status logs:', err)
    }
  }

  const setSelectedDate = (date: string) => {
    selectedDate.value = date
  }

  const clearError = () => {
    error.value = null
  }

  const setActiveTab = (tab: 'calendar' | 'daily' | 'channel') => {
    activeTab.value = tab
  }

  return {
    calendarData,
    dailyData,
    channels,
    roomTypes,
    rooms,
    statusLogs,
    loading,
    error,
    selectedDate,
    dateRange,
    activeTab,

    fetchCalendarData,
    fetchDailyData,
    fetchChannels,
    fetchRoomTypes,
    fetchRooms,
    updateRoomStatus,
    fetchStatusLogs,
    setSelectedDate,
    clearError,
    setActiveTab,
  }
})
