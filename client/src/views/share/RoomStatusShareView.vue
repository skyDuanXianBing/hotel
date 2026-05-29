<template>
  <div class="room-status-share-view">
    <!-- 顶部标题区域 -->
    <div class="header-section" v-if="shareData">
      <div class="header-content">
        <h1 class="share-title">{{ shareData.shareTitle }}</h1>
        <div class="date-info">
          <el-icon><Calendar /></el-icon>
          <span>{{ formatCurrentDate() }}</span>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <el-result
        icon="warning"
        :title="errorMessage"
        :sub-title="t('pages.roomStatusShare.errorSubtitle')"
      >
        <template #extra>
          <el-button type="primary" @click="retry">{{ t('pages.roomStatusShare.retry') }}</el-button>
        </template>
      </el-result>
    </div>

    <!-- 房态数据展示 -->
    <div v-else-if="shareData" class="content-section">
      <!-- 统计数据 -->
      <div v-if="shouldShowStatistics" class="statistics-section">
        <div class="statistics-header">
          <h2 class="section-title">{{ t('pages.roomStatusShare.statisticsTitle') }}</h2>
        </div>
        <div class="statistics-grid">
          <div 
            v-for="stat in visibleStatistics" 
            :key="stat.key"
            class="stat-card"
          >
            <div class="stat-label">{{ stat.label }}</div>
            <div class="stat-value">{{ stat.value }}</div>
          </div>
        </div>
      </div>

      <!-- 房态表格 -->
      <div class="room-status-section">
        <div class="section-header">
          <h2 class="section-title">{{ t('pages.roomStatusShare.detailsTitle') }}</h2>
          <div class="date-selector">
            <el-date-picker
              v-model="selectedDate"
              type="date"
              :placeholder="t('pages.roomStatusShare.datePlaceholder')"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="onDateChange"
              class="date-picker"
            />
          </div>
        </div>

        <!-- 房态日历视图 -->
        <div class="room-status-calendar" v-loading="roomStatusLoading">
          <div class="calendar-header">
            <div class="room-type-column">{{ t('pages.roomStatusShare.roomTypeColumn') }}</div>
            <div 
              v-for="date in dateRange" 
              :key="date"
              class="date-column"
              :class="{ 'today': isToday(date) }"
            >
              {{ formatDateHeader(date) }}
            </div>
          </div>

          <div class="calendar-body">
            <div 
              v-for="roomType in filteredRoomTypes" 
              :key="roomType.id"
              class="room-type-section"
            >
              <!-- 房型行 -->
              <div class="room-type-row">
                <div class="room-type-header">{{ roomType.name }}</div>
                <div 
                  v-for="date in dateRange"
                  :key="`${roomType.id}-header-${date}`"
                  class="empty-cell"
                ></div>
              </div>
              
              <!-- 房间行 -->
              <div 
                v-for="room in roomType.rooms"
                :key="room.roomId"
                class="room-row"
              >
                <div class="room-number-cell">
                  {{ room.roomNumber }}
                </div>
                <div 
                  v-for="date in dateRange"
                  :key="`${room.roomId}-${date}`"
                  class="status-cell"
                  :class="[getRoomStatusClass(room, date), { 'clickable-cell': hasReservationForDate(room, date) }]"
                  @click="handleStatusCellClick(room, date)"
                >
                  <div class="status-content">
                    {{ getRoomStatusText(room, date) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 订单信息（如果配置显示）
      <div v-if="shouldShowOrderInfo" class="order-section">
        <div class="section-header">
          <h2 class="section-title">预订信息</h2>
        </div>
        
        <div class="order-list">
          <div 
            v-for="order in visibleOrders"
            :key="order.id"
            class="order-card"
          >
            <div class="order-header">
              <span class="order-id">订单号: {{ order.orderNumber }}</span>
              <span class="order-status" :class="`status-${order.status}`">
                {{ getOrderStatusText(order.status) }}
              </span>
            </div>
            <div class="order-details">
              <div v-if="orderFields.includes('预订人姓名')" class="order-field">
                <span class="field-label">预订人:</span>
                <span class="field-value">{{ formatGuestName(order.guestName) }}</span>
              </div>
              <div v-if="orderFields.includes('渠道企业')" class="order-field">
                <span class="field-label">渠道:</span>
                <span class="field-value">{{ order.channel }}</span>
              </div>
              <div v-if="orderFields.includes('房间信息')" class="order-field">
                <span class="field-label">房间:</span>
                <span class="field-value">{{ order.roomInfo }}</span>
              </div>
              <div v-if="orderFields.includes('房费')" class="order-field">
                <span class="field-label">房费:</span>
                <span class="field-value">¥{{ order.roomFee }}</span>
              </div>
              <div v-if="orderFields.includes('订单金额')" class="order-field">
                <span class="field-label">总金额:</span>
                <span class="field-value">¥{{ order.totalAmount }}</span>
              </div>
              <div v-if="orderFields.includes('订单备注') && order.notes" class="order-field">
                <span class="field-label">备注:</span>
                <span class="field-value">{{ order.notes }}</span>
              </div>
            </div>
          </div>
        </div>
      </div> -->
    </div>

    <!-- 房间详情弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="t('pages.roomStatusShare.dialogTitle')"
      width="600px"
      :before-close="closeRoomDetails"
      class="room-detail-dialog"
    >
      <div v-if="selectedRoom" class="room-detail-content">
        <!-- 基本信息 -->
        <div class="basic-info-section">
          <div class="info-row">
            <span class="info-label">{{ t('pages.roomStatusShare.fields.guestName') }}</span>
            <span class="info-value">{{ formatGuestName(selectedRoom.guestName || 'aa') }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('pages.roomStatusShare.fields.channel') }}</span>
            <span class="info-value">{{ selectedRoom.channel || t('pages.roomStatusShare.fields.fallbackChannel') }}</span>
          </div>
        </div>

        <!-- 房费信息（蓝色背景区域） -->
        <div class="room-fee-section">
          <div class="fee-info">
            <span class="fee-label">{{ t('pages.roomStatusShare.fields.roomFee') }}</span>
            <span class="fee-value">¥{{ selectedRoom.roomFee || '12.00' }}</span>
            <span class="room-count">{{ t('pages.roomStatusShare.fields.roomCount', { count: 1 }) }}</span>
          </div>
        </div>

        <!-- 房间详情 -->
        <div class="room-detail-section">
          <div class="room-info-card">
            <div class="room-header">
              <span class="room-name">{{ selectedRoom.roomType }}-{{ selectedRoom.roomNumber }}</span>
              <span class="status-badge" :class="`status-${selectedRoom.currentStatus?.toLowerCase()}`">
                {{ getStatusText(selectedRoom.currentStatus) }}
              </span>
            </div>
            <div class="room-dates">
              {{ formatDateRange(selectedRoom.checkIn, selectedRoom.checkOut) }}{{ t('pages.roomStatusShare.dateRange.nights', { count: calculateNights(selectedRoom.checkIn, selectedRoom.checkOut) }) }}
            </div>
            <div class="room-fee">
              {{ t('pages.roomStatusShare.fields.roomFee') }}¥{{ selectedRoom.roomFee || '12.00' }}
            </div>
          </div>
        </div>

        <!-- 订单信息 -->
        <div class="order-info-section">
          <div class="info-row">
            <span class="info-label">{{ t('pages.roomStatusShare.fields.totalAmount') }}</span>
            <span class="info-value">¥{{ selectedRoom.totalAmount || '12.00' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('pages.roomStatusShare.fields.orderNotes') }}</span>
            <span class="info-value">{{ selectedRoom.orderNotes || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('pages.roomStatusShare.fields.imageNotes') }}</span>
            <span class="info-value">{{ selectedRoom.imageNotes || '-' }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { getPublicRoomStatusShare, getSharedRoomStatusData, getSharedStatistics } from '@/api/roomStatusShare'
import type { ApiResponse } from '@/types/room'

// 路由参数
const route = useRoute()
const { t, locale } = useI18n()
const shareToken = computed(() => route.params.token as string)

// 响应式数据
const loading = ref(true)
const roomStatusLoading = ref(false)
const error = ref(false)
const rawErrorMessage = ref('')
const shareData = ref<any>(null)
const selectedDate = ref<string>(new Date().toISOString().split('T')[0])
const roomStatusData = ref<any>(null)
const statisticsData = ref<any>(null)
const showDetailDialog = ref(false)
const selectedRoom = ref<any>(null)

const errorMessage = computed(() => resolveShareErrorMessage(rawErrorMessage.value))

// 日期范围 (显示7天)
const dateRange = computed(() => {
  const dates = []
  const startDate = new Date(selectedDate.value)
  
  for (let i = 0; i < 7; i++) {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + i)
    dates.push(date.toISOString().split('T')[0])
  }
  
  return dates
})

// 计算属性
const shouldShowStatistics = computed(() => {
  return shareData.value?.filterItems && shareData.value.filterItems.length > 0
})

const shouldShowOrderInfo = computed(() => {
  return shareData.value?.orderItems && shareData.value.orderItems.length > 0
})

const visibleStatistics = computed(() => {
  if (!shareData.value?.filterItems || !statisticsData.value) return []
  
  const stats = []
  const items = shareData.value.filterItems
  const data = statisticsData.value
  const hasItem = (keys: string[]) => keys.some((key) => items.includes(key))
  
  if (hasItem(['arrivals', '今日预抵', '今日預抵', '本日の到着予定', '全部', 'すべて'])) {
    stats.push({
      key: 'arrivals',
      label: t('pages.roomStatusShare.statusStats.arrivals'),
      value: data.todayArrivals || 0
    })
  }
  
  if (hasItem(['departures', '今日预离', '今日預離', '本日の出発予定', '全部', 'すべて'])) {
    stats.push({
      key: 'departures', 
      label: t('pages.roomStatusShare.statusStats.departures'),
      value: data.todayDepartures || 0
    })
  }
  
  if (hasItem(['new_orders', '今日新办', '今日新辦', '本日の新規', '全部', 'すべて'])) {
    stats.push({
      key: 'new_orders',
      label: t('pages.roomStatusShare.statusStats.newOrders'),
      value: data.todayNewOrders || 0
    })
  }
  
  if (hasItem(['available', '今日可售', '本日の販売可能数', '全部', 'すべて'])) {
    stats.push({
      key: 'available',
      label: t('pages.roomStatusShare.statusStats.available'),
      value: data.availableRooms || 0
    })
  }
  
  if (hasItem(['unassigned', '未排房', '未割り当て', '全部', 'すべて'])) {
    stats.push({
      key: 'unassigned',
      label: t('pages.roomStatusShare.statusStats.unassigned'),
      value: data.unassignedOrders || 0
    })
  }
  
  if (hasItem(['pending', '待处理', '待處理', '対応待ち', '全部', 'すべて'])) {
    stats.push({
      key: 'pending',
      label: t('pages.roomStatusShare.statusStats.pending'),
      value: data.pendingOrders || 0
    })
  }
  
  return stats
})

const orderFields = computed(() => {
  return shareData.value?.orderItems || []
})

const visibleOrders = computed(() => {
  // 模拟订单数据
  return [
    {
      id: 1,
      orderNumber: 'ORD202501001',
      status: 'confirmed',
      guestName: '张三',
      channel: '携程',
      roomInfo: '大床房 - a01',
      roomFee: 300,
      totalAmount: 300,
      notes: '客人要求高层'
    }
  ]
})

const filteredRoomTypes = computed(() => {
  if (!roomStatusData.value?.rooms) return []
  
  // 后端已经根据分享配置过滤了数据，直接按房型分组
  const roomTypeMap = new Map()
  
  roomStatusData.value.rooms.forEach((room: any) => {
    const roomTypeName = room.roomType
    if (!roomTypeMap.has(roomTypeName)) {
      roomTypeMap.set(roomTypeName, {
        id: roomTypeName,
        name: roomTypeName,
        rooms: []
      })
    }
    roomTypeMap.get(roomTypeName).rooms.push(room)
  })
  
  return Array.from(roomTypeMap.values())
})

// 方法
const fetchShareData = async () => {
  try {
    loading.value = true
    rawErrorMessage.value = ''
    
    console.log('获取分享数据，token:', shareToken.value)
    const response: any = await getPublicRoomStatusShare(shareToken.value)
    console.log('分享数据响应:', response)
    
    if (response.success) {
      shareData.value = response.data
      
      // 解析配置数据
      if (shareData.value.filterItems) {
        shareData.value.filterItems = shareData.value.filterItems.split(',')
      }
      if (shareData.value.orderItems) {
        shareData.value.orderItems = shareData.value.orderItems.split(',')
      }
      if (shareData.value.associatedRoomIds) {
        shareData.value.associatedRoomIds = shareData.value.associatedRoomIds
          .split(',').map(Number)
      }
      
      // 获取房态数据和统计数据
      await Promise.all([
        fetchRoomStatusData(),
        fetchStatisticsData()
      ])
    } else {
      error.value = true
      rawErrorMessage.value = response.message || ''
    }
  } catch (err) {
    console.error('获取分享数据失败:', err)
    error.value = true
    rawErrorMessage.value = ''
  } finally {
    loading.value = false
  }
}

const fetchRoomStatusData = async () => {
  try {
    roomStatusLoading.value = true
    
    const startDate = selectedDate.value
    const endDate = new Date(selectedDate.value)
    endDate.setDate(endDate.getDate() + 6)
    const endDateStr = endDate.toISOString().split('T')[0]
    
    const response: any = await getSharedRoomStatusData(
      shareToken.value, 
      startDate, 
      endDateStr
    )
    
    if (response.success) {
      roomStatusData.value = response.data
    } else {
      ElMessage.error(t('pages.roomStatusShare.messages.fetchRoomStatusFailedWithReason', { message: response.message }))
    }
  } catch (err) {
    console.error('获取房态数据失败:', err)
    ElMessage.error(t('pages.roomStatusShare.messages.fetchRoomStatusFailed'))
  } finally {
    roomStatusLoading.value = false
  }
}

const fetchStatisticsData = async () => {
  try {
    const response: any = await getSharedStatistics(shareToken.value, selectedDate.value)
    
    if (response.success) {
      statisticsData.value = response.data
    } else {
      console.error('获取统计数据失败:', response.message)
    }
  } catch (err) {
    console.error('获取统计数据失败:', err)
  }
}

const onDateChange = () => {
  fetchRoomStatusData()
  fetchStatisticsData()
}

const retry = () => {
  error.value = false
  rawErrorMessage.value = ''
  fetchShareData()
}

const containsChineseText = (value: string) => /[\u3400-\u9fff]/.test(value)

const isInvalidShareMessage = (message: string) => {
  return (
    message.includes('分享链接不存在') ||
    message.includes('分享链接无效') ||
    message.includes('分享链接已失效') ||
    message.includes('不存在或已失效') ||
    message.toLowerCase().includes('invalid') ||
    message.toLowerCase().includes('expired')
  )
}

const resolveShareErrorMessage = (message?: string | null) => {
  const rawMessage = String(message || '').trim()
  if (!rawMessage) {
    return t('pages.roomStatusShare.messages.invalidShare')
  }

  if (isInvalidShareMessage(rawMessage)) {
    return t('pages.roomStatusShare.messages.invalidShare')
  }

  if (containsChineseText(rawMessage) && locale.value !== 'zh-CN') {
    return t('pages.roomStatusShare.messages.fetchShareFailed')
  }

  return rawMessage
}

const formatCurrentDate = () => {
  const now = new Date()
  return now.toLocaleDateString(resolveDateLocale(), {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}

const formatDateHeader = (date: string) => {
  const d = new Date(date)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const weekday = new Intl.DateTimeFormat(resolveDateLocale(), { weekday: 'short' }).format(d)
  
  return `${month}-${day.toString().padStart(2, '0')} ${weekday}`
}

const isToday = (date: string) => {
  return date === new Date().toISOString().split('T')[0]
}

const getRoomStatusClass = (room: any, date: string) => {
  const dailyStatus = room.dailyStatus?.find((ds: any) => ds.date === date)
  const status = dailyStatus?.status || 'AVAILABLE'
  const isBlurred = shareData.value?.viewType === 'blurred' && 
                   (status === 'OCCUPIED' || status === 'RESERVED')
  
  return {
    [`status-${status.toLowerCase()}`]: true,
    'blurred': isBlurred
  }
}

const getRoomStatusText = (room: any, date: string) => {
  const dailyStatus = room.dailyStatus?.find((ds: any) => ds.date === date)
  const status = dailyStatus?.status || 'AVAILABLE'
  const reservationInfo = dailyStatus?.reservation
  const isBlurred = shareData.value?.viewType === 'blurred' && 
                   (status === 'OCCUPIED' || status === 'RESERVED')
  
  if (isBlurred) {
    return t('pages.roomStatusShare.roomStatus.masked')
  }
  
  switch (status) {
    case 'AVAILABLE':
      return t('pages.roomStatusShare.roomStatus.available')
    case 'OCCUPIED':
      return reservationInfo?.guestName || t('pages.roomStatusShare.roomStatus.occupied')
    case 'RESERVED':
      return reservationInfo?.guestName || t('pages.roomStatusShare.roomStatus.reserved')
    case 'MAINTENANCE':
      return t('pages.roomStatusShare.roomStatus.maintenance')
    case 'OUT_OF_ORDER':
      return t('pages.roomStatusShare.roomStatus.outOfOrder')
    default:
      return t('pages.roomStatusShare.roomStatus.unknown')
  }
}

const getOrderStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    confirmed: t('pages.roomStatusShare.orderStatus.confirmed'),
    checked_in: t('pages.roomStatusShare.orderStatus.checkedIn'),
    checked_out: t('pages.roomStatusShare.orderStatus.checkedOut'),
    cancelled: t('pages.roomStatusShare.orderStatus.cancelled')
  }
  return statusMap[status] || status
}

const formatGuestName = (name: string) => {
  if (shareData.value?.viewType === 'blurred') {
    return name.charAt(0) + '**'
  }
  return name
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'OCCUPIED': t('pages.roomStatusShare.orderStatus.checkedIn'),
    'RESERVED': t('pages.roomStatusShare.roomStatus.reserved'),
    'MAINTENANCE': t('pages.roomStatusShare.roomStatus.maintenance'),
    'OUT_OF_ORDER': t('pages.roomStatusShare.roomStatus.outOfOrder'),
    'CLEANING': t('pages.roomStatusShare.roomStatus.cleaning'),
    'AVAILABLE': t('pages.roomStatusShare.roomStatus.available')
  }
  return statusMap[status] || t('pages.roomStatusShare.roomStatus.unknown')
}

// 检查特定日期是否有预订信息
const hasReservationForDate = (room: any, date: string) => {
  const dailyStatus = room.dailyStatus?.find((ds: any) => ds.date === date)
  return dailyStatus && dailyStatus.reservation && dailyStatus.status !== 'AVAILABLE'
}

// 处理状态单元格点击
const handleStatusCellClick = (room: any, date: string) => {
  const dailyStatus = room.dailyStatus?.find((ds: any) => ds.date === date)
  
  // 只有非空房（有预订信息）才显示信息
  if (dailyStatus && dailyStatus.reservation) {
    showRoomDetailsForDate(room, date, dailyStatus)
  }
  // 空房点击不做任何操作，不显示任何信息
}

// 显示特定日期的房间详情
const showRoomDetailsForDate = (room: any, date: string, dailyStatus: any) => {
  if (dailyStatus && dailyStatus.reservation) {
    const reservation = dailyStatus.reservation
    selectedRoom.value = {
      ...room,
      ...reservation,
      roomType: room.roomType,
      roomNumber: room.roomNumber,
      checkIn: reservation.checkIn,
      checkOut: reservation.checkOut,
      guestName: reservation.guestName,
      channel: reservation.channel,
      roomFee: '12.00', // 可以从后端API获取实际房费
      totalAmount: '12.00', // 可以从后端API获取实际总金额
      orderNotes: '-',
      imageNotes: '-',
      currentStatus: dailyStatus.status, // 添加当前状态信息
      selectedDate: date // 添加选中的日期信息
    }
    showDetailDialog.value = true
  }
}

const showRoomDetails = (room: any) => {
  // 查找任何有预订信息的状态（已入住、已预订等）
  const reservationStatus = room.dailyStatus?.find((ds: any) => ds.reservation)
  
  if (reservationStatus && reservationStatus.reservation) {
    const reservation = reservationStatus.reservation
    selectedRoom.value = {
      ...room,
      ...reservation,
      roomType: room.roomType,
      roomNumber: room.roomNumber,
      checkIn: reservation.checkIn,
      checkOut: reservation.checkOut,
      guestName: reservation.guestName,
      channel: reservation.channel,
      roomFee: '12.00', // 可以从后端API获取实际房费
      totalAmount: '12.00', // 可以从后端API获取实际总金额
      orderNotes: '-',
      imageNotes: '-',
      currentStatus: reservationStatus.status // 添加当前状态信息
    }
    showDetailDialog.value = true
  }
}

const closeRoomDetails = () => {
  showDetailDialog.value = false
  selectedRoom.value = null
}

const resolveDateLocale = () => {
  switch (locale.value) {
    case 'zh-TW':
      return 'zh-TW'
    case 'en':
      return 'en-US'
    case 'ja':
      return 'ja-JP'
    default:
      return 'zh-CN'
  }
}

const formatDateRange = (checkIn: string, checkOut: string) => {
  if (!checkIn || !checkOut) return ''
  
  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hour = date.getHours().toString().padStart(2, '0')
    const minute = date.getMinutes().toString().padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}`
  }
  
  return `${formatDate(checkIn)}${t('pages.roomStatusShare.dateRange.separator')}${formatDate(checkOut)}`
}

const calculateNights = (checkIn: string, checkOut: string) => {
  if (!checkIn || !checkOut) return 0
  
  const checkInDate = new Date(checkIn)
  const checkOutDate = new Date(checkOut)
  const timeDiff = checkOutDate.getTime() - checkInDate.getTime()
  const daysDiff = Math.ceil(timeDiff / (1000 * 3600 * 24))
  
  return daysDiff
}

onMounted(() => {
  fetchShareData()
})
</script>

<style scoped>
.room-status-share-view {
  min-height: 100vh;
  background: #f5f7fa;
}

.header-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 40px 20px;
  text-align: center;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
}

.share-title {
  font-size: 2.5em;
  margin: 0 0 16px 0;
  font-weight: 300;
}

.date-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 1.1em;
  opacity: 0.9;
}

.loading-container,
.error-container {
  max-width: 800px;
  margin: 40px auto;
  padding: 20px;
}

.content-section {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.section-title {
  font-size: 1.5em;
  color: #2c3e50;
  margin: 0 0 20px 0;
  font-weight: 500;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.date-picker {
  width: 200px;
}

/* 统计数据样式 */
.statistics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  text-align: center;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 2em;
  font-weight: 600;
  color: #2c3e50;
}

/* 房态日历样式 */
.room-status-calendar {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.calendar-header {
  display: grid;
  grid-template-columns: 120px repeat(7, 1fr);
  background: #f8f9fa;
  border-bottom: 2px solid #e9ecef;
}

.room-type-column,
.date-column {
  padding: 16px 12px;
  font-weight: 600;
  text-align: center;
  border-right: 1px solid #e9ecef;
}

.date-column.today {
  background: #e3f2fd;
  color: #1976d2;
}

.calendar-body {
  max-height: 600px;
  overflow-y: auto;
}

.room-type-section {
  border-bottom: 2px solid #e9ecef;
  margin-bottom: 4px;
}

.room-type-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.room-type-row {
  display: grid;
  grid-template-columns: 120px repeat(7, 1fr);
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.room-type-header {
  padding: 16px 12px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  font-size: 16px;
  font-weight: 700;
  color: #2c3e50;
  border-right: 1px solid #e9ecef;
  background: #f1f3f5;
  position: relative;
}

.empty-cell {
  border-right: 1px solid #e9ecef;
}

.room-row {
  display: grid;
  grid-template-columns: 120px repeat(7, 1fr);
  border-bottom: 1px solid #e9ecef;
  transition: all 0.2s ease;
}

/* 可点击的状态单元格样式 */
.status-cell.clickable-cell {
  cursor: pointer;
  position: relative;
}

.status-cell.clickable-cell:hover {
  background: #f8f9fa;
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 10;
}

.status-cell.clickable-cell:hover .status-content {
  background: #007bff;
  color: white;
  transform: scale(1.1);
  box-shadow: 0 2px 4px rgba(0, 123, 255, 0.3);
}

.status-cell.clickable-cell:active {
  transform: scale(1.02);
}

.status-cell.clickable-cell:active .status-content {
  transform: scale(1.05);
}

.room-number-cell {
  padding: 14px 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  border-right: 1px solid #e9ecef;
  background: #ffffff;
  color: #495057;
  border-left: 3px solid transparent;
}


.status-cell {
  padding: 10px 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #e9ecef;
  min-height: 50px;
  background: #ffffff;
  transition: all 0.2s ease;
}

.status-content {
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  min-width: 50px;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.status-available .status-content {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-occupied .status-content {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.status-reserved .status-content {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

.status-maintenance .status-content {
  background: #e2e3e5;
  color: #383d41;
  border: 1px solid #d6d8db;
}

.status-out_of_order .status-content {
  background: #f5c6cb;
  color: #721c24;
  border: 1px solid #f1b0b7;
}

.blurred .status-content {
  background: #f8f9fa;
  color: #6c757d;
  filter: blur(1px);
}

/* 订单信息样式 */
.order-list {
  display: grid;
  gap: 16px;
}

.order-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e9ecef;
}

.order-id {
  font-weight: 600;
  color: #495057;
}

.order-status {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.status-confirmed {
  background: #d4edda;
  color: #155724;
}

.status-checked_in {
  background: #cce5ff;
  color: #004085;
}

.order-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.order-field {
  display: flex;
  gap: 8px;
}

.field-label {
  font-weight: 500;
  color: #666;
  min-width: 60px;
}

.field-value {
  color: #2c3e50;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .calendar-header,
  .room-type-row,
  .room-row {
    grid-template-columns: 100px repeat(7, 1fr);
  }
  
  .content-section {
    padding: 20px 10px;
  }
  
  .share-title {
    font-size: 1.8em;
  }
  
  .statistics-grid {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  }
  
  .status-content {
    font-size: 11px;
    padding: 4px 6px;
    min-width: 40px;
  }
}

/* 房间详情弹窗样式 */
.room-detail-dialog {
  --el-dialog-border-radius: 12px;
}

.room-detail-content {
  padding: 0;
}

.basic-info-section {
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #333;
  font-weight: 600;
}

/* 房费信息（蓝色背景区域） */
.room-fee-section {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  padding: 16px 20px;
  border-radius: 8px;
  margin: 20px 0;
}

.fee-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fee-label {
  font-size: 16px;
  color: #1976d2;
  font-weight: 600;
}

.fee-value {
  font-size: 20px;
  color: #1976d2;
  font-weight: 700;
}

.room-count {
  font-size: 14px;
  color: #1976d2;
  opacity: 0.8;
}

/* 房间详情卡片 */
.room-detail-section {
  margin: 20px 0;
}

.room-info-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  border-left: 4px solid #007bff;
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.room-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  color: white;
}

/* 不同状态的颜色 */
.status-badge.status-occupied {
  background: #dc3545; /* 红色 - 已入住 */
}

.status-badge.status-reserved {
  background: #ffc107; /* 黄色 - 已预订 */
  color: #212529;
}

.status-badge.status-maintenance {
  background: #6c757d; /* 灰色 - 维修中 */
}

.status-badge.status-out_of_order {
  background: #e83e8c; /* 粉色 - 停用 */
}

.status-badge.status-cleaning {
  background: #20c997; /* 青色 - 清洁中 */
}

.status-badge.status-available {
  background: #28a745; /* 绿色 - 空房 */
}

.room-dates {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.room-fee {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

/* 订单信息区域 */
.order-info-section {
  border-top: 1px solid #e9ecef;
  padding-top: 20px;
  margin-top: 20px;
}
</style>
