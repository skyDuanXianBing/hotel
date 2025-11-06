import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { request } from '@/utils/request'
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

export const useRoomStatusStore = defineStore('roomStatus', () => {
  // 状态数据
  const calendarData = ref<RoomStatusCalendar | null>(null)
  const dailyData = ref<DailyRoomStatusData | null>(null)
  const channels = ref<Channel[]>([])
  const roomTypes = ref<RoomType[]>([])
  const rooms = ref<Room[]>([])
  const statusLogs = ref<RoomStatusLog[]>([])

  // 加载状态
  const loading = ref(false)
  const error = ref<string | null>(null)

  // UI 状态 - 房态页面活动标签页
  const activeTab = ref<'calendar' | 'daily' | 'channel'>('calendar')

  // 计算属性
  const selectedDate = ref<string>(new Date().toISOString().split('T')[0])
  const dateRange = computed(() => {
    if (!calendarData.value) return { startDate: '', endDate: '' }
    return calendarData.value.dateRange
  })

  // 获取房态日历数据
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
      error.value = '获取房态日历数据失败'
      console.error('获取房态日历数据失败:', err)
    } finally {
      loading.value = false
    }
  }

  // 获取单日房态数据
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
      error.value = '获取单日房态数据失败'
      console.error('获取单日房态数据失败:', err)
    } finally {
      loading.value = false
    }
  }

  // 获取渠道列表
  const fetchChannels = async () => {
    try {
      const response: ApiResponse<Channel[]> = await request.get('/api/v1/channels')

      if (response.success) {
        channels.value = response.data
      } else {
        error.value = response.message
      }
    } catch (err) {
      error.value = '获取渠道列表失败'
      console.error('获取渠道列表失败:', err)
    }
  }

  // 获取房型列表
  const fetchRoomTypes = async () => {
    try {
      const response: ApiResponse<RoomType[]> = await request.get('/api/v1/room-types')

      if (response.success) {
        roomTypes.value = response.data
      } else {
        error.value = response.message
      }
    } catch (err) {
      error.value = '获取房型列表失败'
      console.error('获取房型列表失败:', err)
    }
  }

  // 获取房间列表
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
      error.value = '获取房间列表失败'
      console.error('获取房间列表失败:', err)
    }
  }

  // 更新房间状态
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
        // 重新获取相关数据
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
      error.value = '更新房间状态失败'
      console.error('更新房间状态失败:', err)
      return false
    } finally {
      loading.value = false
    }
  }

  // 获取房态修改记录
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
      error.value = '获取房态修改记录失败'
      console.error('获取房态修改记录失败:', err)
    }
  }

  // 设置选中日期
  const setSelectedDate = (date: string) => {
    selectedDate.value = date
  }

  // 清除错误
  const clearError = () => {
    error.value = null
  }

  // 设置活动标签页
  const setActiveTab = (tab: 'calendar' | 'daily' | 'channel') => {
    activeTab.value = tab
  }

  return {
    // 状态
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

    // 方法
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
