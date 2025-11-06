<template>
  <div class="room-table">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="tab-section">
          <el-button 
            :type="activeTab === 'daily' ? 'primary' : 'default'"
            :class="activeTab === 'daily' ? 'active-tab' : 'inactive-tab'"
            @click="switchTab('daily')"
          >
            单日房情表
          </el-button>
          <el-button 
            :type="activeTab === 'future' ? 'primary' : 'default'"
            :class="activeTab === 'future' ? 'active-tab' : 'inactive-tab'"
            @click="switchTab('future')"
          >
            远期房情表
          </el-button>
        </div>
      </div>
      
      <div class="toolbar-right">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
          class="date-picker"
        />
        <el-button type="primary" :icon="Download" @click="exportData">
          导出明细
        </el-button>
      </div>
    </div>

    <!-- 单日房情统计表格 -->
    <div v-if="activeTab === 'daily'" class="table-container" v-loading="loading">
      <el-table
        :data="tableData"
        stripe
        border
        class="room-statistics-table"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '500' }"
      >
        <el-table-column prop="roomTypeName" label="房型名称" width="120" align="center" />
        <el-table-column prop="totalRooms" label="总房数" width="120" align="center" />
        
        <el-table-column label="可售房" width="60" align="center">
          <template #header>
            <span>可售房</span>
            <el-tooltip content="总房数减去预抵在住不含预离关房" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.availableForSale }}
          </template>
        </el-table-column>

        <el-table-column label="可用房" width="60" align="center">
          <template #header>
            <span>可用房</span>
            <el-tooltip content="总房数减去预离在住不含预离关房" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.availableRooms }}
          </template>
        </el-table-column>

        <el-table-column label="在住" width="60" align="center">
          <template #header>
            <span>在住</span>
            <el-tooltip content="在住不含预离加上预离" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.occupiedRooms }}
          </template>
        </el-table-column>

        <el-table-column label="在住（不含预离）" width="120" align="center">
          <template #header>
            <span>在住（不含预离）</span>
            <el-tooltip content="当前日已入住状态房间数 未来日对应日已入住已预订状态房间数不含预抵" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.occupiedWithoutDeparture }}
          </template>
        </el-table-column>

        <el-table-column label="预离" width="60" align="center">
          <template #header>
            <span>预离</span>
            <el-tooltip content="当前日离店日期为当前日且已入住状态的房间数 未来日离店日期为对应日的所有房间数" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.scheduledDeparture }}
          </template>
        </el-table-column>

        <el-table-column prop="scheduledArrival" label="预抵" width="60" align="center" />
        <el-table-column prop="reservedRooms" label="保留房（关房）" width="120" align="center" />
        <el-table-column prop="maintenanceRooms" label="维修房（关房）" width="120" align="center" />
        <el-table-column prop="outOfOrderRooms" label="停用房（关房）" width="120" align="center" />
        <el-table-column prop="linkedClosedRooms" label="链接关房（关房）" width="120" align="center" />
        <el-table-column prop="cleanRooms" label="净房" width="60" align="center" />
        <el-table-column prop="dirtyRooms" label="脏房" width="60" align="center" />
        
        <el-table-column label="预计入住率" width="120" align="center">
          <template #header>
            <span>预计入住率</span>
            <el-tooltip content="累计出售间夜数除以总可售房间数" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.expectedOccupancyRate }}%
          </template>
        </el-table-column>

        <el-table-column label="当日取消房" width="120" align="center">
          <template #header>
            <span>当日取消房</span>
            <el-tooltip content="即取消的房间数量按订单的营业日统计" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.dailyCancelledRooms }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 远期房情表 -->
    <div v-else-if="activeTab === 'future'" class="future-room-container">
      <!-- 说明提示栏 -->
      <div class="info-banner">
        <el-alert
          title="可售等于可售房间数 占用等于已营房间数 不可售等于保用加维修加链接关房房间数"
          type="info"
          :closable="false"
          show-icon
        />
      </div>

      <!-- 日期导航栏 -->
      <div class="date-navigation">
        <el-button :icon="ArrowLeft" circle @click="previousPeriod" />
        <div class="custom-date-picker">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="onFutureDateChange"
            class="future-date-picker"
          />
          <span class="date-display" v-if="selectedDate">{{ getFormattedDateWithWeekday(selectedDate) }}</span>
        </div>
        <el-button :icon="ArrowRight" circle @click="nextPeriod" />
        <div class="spacer"></div>
        <el-button type="primary" @click="exportData">导出明细</el-button>
      </div>

      <!-- 远期房情主表格 -->
      <div class="future-table-container" v-loading="loading">
        <div class="table-scroll-wrapper">
          <table class="future-table-main" v-if="futureRoomTableData">
            <thead>
              <!-- 第一行：日期标题 -->
              <tr class="date-header-row">
                <th rowspan="2" class="fixed-header room-type-col">房型</th>
                <th rowspan="2" class="fixed-header total-rooms-col">总房数</th>
                <th 
                  v-for="date in futureRoomTableData.total.dates" 
                  :key="date.date"
                  colspan="3" 
                  class="date-header"
                >
                  {{ formatDateHeader(date.date, date.dayOfWeek) }}
                </th>
              </tr>
              <!-- 第二行：子列标题 -->
              <tr class="sub-header-row">
                <template v-for="date in futureRoomTableData.total.dates" :key="`sub-${date.date}`">
                  <th class="sub-header">可售</th>
                  <th class="sub-header">占用</th>
                  <th class="sub-header">不可售</th>
                </template>
              </tr>
            </thead>
            <tbody>
              <!-- 房型数据行 -->
              <tr 
                v-for="roomType in futureRoomTableData.roomTypes" 
                :key="roomType.roomTypeName"
                class="room-type-row"
              >
                <td class="fixed-cell room-type-cell">{{ roomType.roomTypeName }}</td>
                <td class="fixed-cell total-rooms-cell">{{ roomType.totalRooms }}</td>
                <template v-for="date in roomType.dates" :key="`${roomType.roomTypeName}-${date.date}`">
                  <td class="data-cell" :class="{ available: date.available > 0 }">{{ date.available }}</td>
                  <td class="data-cell" :class="{ occupied: date.occupied > 0 }">{{ date.occupied }}</td>
                  <td class="data-cell">{{ date.unavailable }}</td>
                </template>
              </tr>
              <!-- 占总房数的比例行 -->
              <tr class="percentage-row">
                <td class="fixed-cell room-type-cell">占总房数的比例</td>
                <td class="fixed-cell total-rooms-cell">-</td>
                <template v-for="date in futureRoomTableData.total.dates" :key="`percentage-${date.date}`">
                  <td class="data-cell percentage" :class="{ available: date.availableRate > 0 }">
                    {{ date.availableRate }}%
                  </td>
                  <td class="data-cell percentage" :class="{ occupied: date.occupiedRate > 0 }">
                    {{ date.occupiedRate }}%
                  </td>
                  <td class="data-cell percentage">
                    {{ date.unavailableRate }}%
                  </td>
                </template>
              </tr>
              <!-- 合计行 -->
              <tr class="total-row">
                <td class="fixed-cell room-type-cell">合计</td>
                <td class="fixed-cell total-rooms-cell">{{ futureRoomTableData.total.totalRooms }}</td>
                <template v-for="date in futureRoomTableData.total.dates" :key="`total-${date.date}`">
                  <td class="data-cell" :class="{ available: date.available > 0 }">{{ date.available }}</td>
                  <td class="data-cell" :class="{ occupied: date.occupied > 0 }">{{ date.occupied }}</td>
                  <td class="data-cell">{{ date.unavailable }}</td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 底部统计区域 -->
      <div class="future-statistics" v-if="futureRoomTableData">
        <!-- 日期标题行 -->
        <div class="stats-date-header">
          <div class="stat-item"></div>
          <div 
            v-for="statistic in futureRoomTableData.statistics" 
            :key="statistic.date"
            class="date-header-item"
          >
            {{ formatStatisticDateHeader(statistic.date) }}
          </div>
        </div>

        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-label">
              有效客房数
              <el-tooltip content="可用于客人入住的房间总数" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </span>
          </div>
          <div 
            v-for="statistic in futureRoomTableData.statistics" 
            :key="`effective-${statistic.date}`"
            class="stat-value"
          >
            {{ statistic.effectiveRooms }}
          </div>
        </div>

        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-label">
              入住率（预期）
              <el-tooltip content="预期的房间入住率" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </span>
          </div>
          <div 
            v-for="statistic in futureRoomTableData.statistics" 
            :key="`occupancy-${statistic.date}`"
            class="stat-value"
          >
            {{ statistic.expectedOccupancyRate }}%
          </div>
        </div>

        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-label">
              客房收入（预期）
              <el-tooltip content="预期的客房收入" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </span>
          </div>
          <div 
            v-for="statistic in futureRoomTableData.statistics" 
            :key="`revenue-${statistic.date}`"
            class="stat-value"
          >
            {{ statistic.expectedRoomRevenue }}
          </div>
        </div>

        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-label">
              总房费（预期）
              <el-tooltip content="预期的总房费收入" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </span>
          </div>
          <div 
            v-for="statistic in futureRoomTableData.statistics" 
            :key="`total-fee-${statistic.date}`"
            class="stat-value"
          >
            {{ statistic.expectedTotalRoomFee }}
          </div>
        </div>

        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-label">
              平均客房收益（预期）
              <el-tooltip content="预期的平均每间房收益" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </span>
          </div>
          <div 
            v-for="statistic in futureRoomTableData.statistics" 
            :key="`average-${statistic.date}`"
            class="stat-value"
          >
            {{ statistic.averageRoomRevenue }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, InfoFilled, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { request } from '@/utils/request'
import type { RoomTableData, RoomStatistics, ApiResponse, FutureRoomTableData } from '@/types/room'
import { getFutureRoomTableData } from '@/api/futureRoomTable'

// 响应式数据
const loading = ref(false)
const activeTab = ref<'daily' | 'future'>('daily')
const selectedDate = ref<string>(new Date().toISOString().split('T')[0])
const roomTableData = ref<RoomTableData | null>(null)
const futureRoomTableData = ref<FutureRoomTableData | null>(null)

// 计算属性 - 表格数据（包含合计行）
const tableData = computed(() => {
  if (!roomTableData.value) return []
  
  const data = [...roomTableData.value.statistics]
  // 添加合计行
  data.push({
    ...roomTableData.value.total,
    roomTypeName: '合计'
  })
  
  return data
})

// 获取房情表数据
const fetchRoomTableData = async (date: string) => {
  try {
    loading.value = true
    
    const response: ApiResponse<RoomTableData> = await request.get(
      `/room-table/statistics?date=${date}`
    )
    
    if (response.success) {
      roomTableData.value = response.data
    } else {
      ElMessage.error(response.message || '获取房情表数据失败')
    }
  } catch (error) {
    console.error('获取房情表数据失败:', error)
    ElMessage.error('获取房情表数据失败')
    
    // 添加模拟数据用于展示
    roomTableData.value = {
      date: date,
      statistics: [
        {
          roomTypeName: '大床房',
          totalRooms: 1,
          availableForSale: 1,
          availableRooms: 1,
          occupiedRooms: 0,
          occupiedWithoutDeparture: 0,
          scheduledDeparture: 0,
          scheduledArrival: 0,
          reservedRooms: 0,
          maintenanceRooms: 0,
          outOfOrderRooms: 0,
          linkedClosedRooms: 0,
          cleanRooms: 0,
          dirtyRooms: 1,
          expectedOccupancyRate: 0,
          dailyCancelledRooms: 0
        }
      ],
      total: {
        roomTypeName: '合计',
        totalRooms: 1,
        availableForSale: 1,
        availableRooms: 1,
        occupiedRooms: 0,
        occupiedWithoutDeparture: 0,
        scheduledDeparture: 0,
        scheduledArrival: 0,
        reservedRooms: 0,
        maintenanceRooms: 0,
        outOfOrderRooms: 0,
        linkedClosedRooms: 0,
        cleanRooms: 0,
        dirtyRooms: 1,
        expectedOccupancyRate: 0,
        dailyCancelledRooms: 0
      }
    }
  } finally {
    loading.value = false
  }
}

// 日期变化处理
const onDateChange = (date: string) => {
  if (date) {
    fetchRoomTableData(date)
  }
}

// 标签页切换
const switchTab = (tab: 'daily' | 'future') => {
  activeTab.value = tab
  if (tab === 'daily') {
    fetchRoomTableData(selectedDate.value)
  } else {
    fetchFutureRoomTableData(selectedDate.value, 7)
  }
}

// 获取远期房情表数据
const fetchFutureRoomTableData = async (startDate: string, days: number = 7) => {
  try {
    loading.value = true
    
    const response = await getFutureRoomTableData(startDate, days)
    
    if (response.success) {
      futureRoomTableData.value = response.data
    } else {
      ElMessage.error(response.message || '获取远期房情表数据失败')
    }
  } catch (error) {
    console.error('获取远期房情表数据失败:', error)
    ElMessage.error('获取远期房情表数据失败')
  } finally {
    loading.value = false
  }
}

// 远期房情日期变化处理
const onFutureDateChange = (date: string) => {
  if (date) {
    fetchFutureRoomTableData(date, 7)
  }
}

// 上一个时间段
const previousPeriod = () => {
  const current = new Date(selectedDate.value)
  current.setDate(current.getDate() - 7)
  selectedDate.value = current.toISOString().split('T')[0]
  fetchFutureRoomTableData(selectedDate.value, 7)
}

// 下一个时间段
const nextPeriod = () => {
  const current = new Date(selectedDate.value)
  current.setDate(current.getDate() + 7)
  selectedDate.value = current.toISOString().split('T')[0]
  fetchFutureRoomTableData(selectedDate.value, 7)
}

// 导出数据
const exportData = () => {
  ElMessage.info('导出功能正在开发中...')
}

// 获取格式化的日期和星期
const getFormattedDateWithWeekday = (value: Date | string): string => {
  let date: Date
  if (typeof value === 'string') {
    date = new Date(value)
  } else {
    date = value
  }
  
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const weekday = weekdays[date.getDay()]
  
  return `${year}-${month}-${day} ${weekday}`
}

// 格式化表格表头日期
const formatDateHeader = (dateStr: string, dayOfWeek: string): string => {
  const date = new Date(dateStr)
  const today = new Date()
  
  // 检查是否是今天
  const isToday = date.toDateString() === today.toDateString()
  
  // 格式化为 MM-DD 格式
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  
  return isToday ? `${month}-${day} 今天` : `${month}-${day} ${dayOfWeek}`
}

// 格式化统计区域的日期标题
const formatStatisticDateHeader = (dateStr: string): string => {
  const date = new Date(dateStr)
  const today = new Date()
  
  // 检查是否是今天
  const isToday = date.toDateString() === today.toDateString()
  
  // 格式化为 MM-DD 格式
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  
  // 获取星期几
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const weekday = weekdays[date.getDay()]
  
  return isToday ? `${month}-${day} 今天` : `${month}-${day} ${weekday}`
}

// 组件挂载时获取数据
onMounted(() => {
  fetchRoomTableData(selectedDate.value)
})
</script>

<style scoped>
.room-table {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.tab-section {
  display: flex;
  gap: 0;
}

.active-tab {
  background: #409eff;
  border-color: #409eff;
  color: white;
  border-radius: 4px 0 0 4px;
}

.inactive-tab {
  background: #f5f7fa;
  border-color: #dcdfe6;
  color: #606266;
  border-radius: 0 4px 4px 0;
  margin-left: -1px;
}

.inactive-tab:hover {
  background: #ecf5ff;
  color: #409eff;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-picker {
  width: 150px;
}

.table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.room-statistics-table {
  width: 100%;
}

.room-statistics-table :deep(.el-table__header) {
  font-weight: 500;
}

.room-statistics-table :deep(.el-table__row:last-child) {
  font-weight: 600;
  background-color: #f8f9fa;
}

.room-statistics-table :deep(.el-table__row:last-child td) {
  background-color: #f8f9fa !important;
}

.info-icon {
  margin-left: 4px;
  color: #909399;
  cursor: help;
  font-size: 12px;
}

.info-icon:hover {
  color: #409eff;
}

/* 远期房情表样式 */
.future-room-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-banner {
  margin-bottom: 16px;
}

.date-navigation {
  display: flex;
  align-items: center;
  gap: 12px;
  background: white;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.custom-date-picker {
  position: relative;
  display: inline-block;
}

.future-date-picker {
  width: 200px;
}

.date-display {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: white;
  padding: 0 8px;
  color: #606266;
  font-size: 14px;
  pointer-events: none;
  z-index: 1;
}

.custom-date-picker .future-date-picker :deep(.el-input__inner) {
  color: transparent;
  cursor: pointer;
}

.custom-date-picker .future-date-picker :deep(.el-input__inner)::placeholder {
  color: #a8abb2;
}

.spacer {
  flex: 1;
}

.future-table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.future-table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.table-scroll-wrapper {
  overflow-x: auto;
  overflow-y: hidden;
  max-height: 500px;
}

.table-scroll-wrapper::-webkit-scrollbar {
  height: 8px;
}

.table-scroll-wrapper::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.table-scroll-wrapper::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.table-scroll-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 新的表格样式 */
.future-table-main {
  min-width: 1000px; /* 确保表格有足够宽度产生滚动 */
  border-collapse: separate;
  border-spacing: 0;
  font-size: 14px;
  border: 1px solid #e4e7ed;
  position: relative;
}

/* 表头样式 */
.date-header-row th {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
  padding: 8px 12px;
  text-align: center;
  border-bottom: 1px solid #e4e7ed;
  border-right: 1px solid #e4e7ed;
}

.sub-header-row th {
  background: #f8f9fa;
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  padding: 8px 12px;
  text-align: center;
  border-bottom: 1px solid #e4e7ed;
  border-right: 1px solid #e4e7ed;
}

/* 固定列样式 */
.fixed-header {
  position: sticky;
  z-index: 15;
  background: #ecf5ff !important;
  box-shadow: 2px 0 4px rgba(0, 0, 0, 0.1);
}

.room-type-col {
  width: 120px;
  min-width: 120px;
  left: 0;
}

.total-rooms-col {
  width: 120px;
  min-width: 120px;
  left: 120px;
}

/* 固定单元格样式 */
.fixed-cell {
  position: sticky;
  background: white;
  z-index: 10;
  box-shadow: 2px 0 4px rgba(0, 0, 0, 0.05);
  border-right: 1px solid #e4e7ed;
  border-bottom: 1px solid #e4e7ed;
  padding: 8px 12px;
}

.room-type-cell {
  left: 0;
  width: 120px;
  min-width: 120px;
  text-align: center;
  font-weight: 500;
}

.total-rooms-cell {
  left: 120px;
  width: 120px;
  min-width: 120px;
  text-align: center;
}

/* 数据单元格样式 */
.data-cell {
  width: 60px;
  min-width: 60px;
  text-align: center;
  padding: 8px 12px;
  border-bottom: 1px solid #e4e7ed;
  border-right: 1px solid #e4e7ed;
  background: white;
  white-space: nowrap;
}

/* 日期标题样式 */
.date-header {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

.sub-header {
  background: #f8f9fa;
  font-size: 12px;
  width: 60px;
  min-width: 60px;
}

.date-header {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
  position: relative;
}

.sub-header th {
  background: #f8f9fa;
  font-size: 12px;
  padding: 8px 12px;
}

/* 行样式 */
.room-type-row .data-cell {
  font-weight: 500;
}

.percentage-row .fixed-cell {
  background: #fafafa !important;
  font-style: italic;
  font-size: 12px;
}

.percentage-row .data-cell {
  background: #fafafa;
  font-style: italic;
  font-size: 12px;
}

.total-row .fixed-cell {
  background: #f0f9ff !important;
  font-weight: 600;
}

.total-row .data-cell {
  background: #f0f9ff;
  font-weight: 600;
}

.available {
  color: #67c23a;
  font-weight: 600;
}

.occupied {
  color: #f56c6c;
  font-weight: 600;
}

.percentage.available {
  color: #67c23a;
  font-weight: 600;
}

.percentage.occupied {
  color: #f56c6c;
  font-weight: 600;
}

/* 底部统计区域 */
.future-statistics {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.stats-date-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e4e7ed;
}

.date-header-item {
  flex: 1;
  text-align: center;
  padding: 8px 16px;
  font-weight: 600;
  color: #409eff;
  font-size: 14px;
  border-right: 1px solid #e4e7ed;
}

.date-header-item:last-child {
  border-right: none;
}

.stats-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  min-height: 40px;
}

.stats-row:last-child {
  margin-bottom: 0;
}

.stat-item {
  width: 200px;
  flex-shrink: 0;
  text-align: left;
  font-weight: 500;
  color: #606266;
}

.stat-label {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-value {
  flex: 1;
  text-align: center;
  padding: 8px 16px;
  border-right: 1px solid #e4e7ed;
  font-weight: 500;
}

.stat-value:last-child {
  border-right: none;
}
</style>
