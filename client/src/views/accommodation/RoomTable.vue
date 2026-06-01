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
            {{ t('accommodation.roomTable.dailyTab') }}
          </el-button>
          <el-button 
            :type="activeTab === 'future' ? 'primary' : 'default'"
            :class="activeTab === 'future' ? 'active-tab' : 'inactive-tab'"
            @click="switchTab('future')"
          >
            {{ t('accommodation.roomTable.futureTab') }}
          </el-button>
        </div>
      </div>
      
      <div class="toolbar-right">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          :placeholder="activeTab === 'daily' ? t('accommodation.roomTable.selectDate') : t('accommodation.roomTable.selectStartDate')"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
          class="date-picker"
        />
        <el-button type="primary" :icon="Download" @click="exportData">
          {{ t('accommodation.common.exportDetails') }}
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
        <el-table-column prop="roomTypeName" :label="t('accommodation.roomTable.columns.roomTypeName')" width="120" align="center" />
        <el-table-column prop="totalRooms" :label="t('accommodation.roomTable.columns.totalRooms')" width="120" align="center" />
        
        <el-table-column :label="t('accommodation.roomTable.columns.availableForSale')" width="60" align="center">
          <template #header>
            <span>{{ t('accommodation.roomTable.columns.availableForSale') }}</span>
            <el-tooltip :content="t('accommodation.roomTable.tooltips.availableForSale')" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.availableForSale }}
          </template>
        </el-table-column>

        <el-table-column :label="t('accommodation.roomTable.columns.availableRooms')" width="60" align="center">
          <template #header>
            <span>{{ t('accommodation.roomTable.columns.availableRooms') }}</span>
            <el-tooltip :content="t('accommodation.roomTable.tooltips.availableRooms')" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.availableRooms }}
          </template>
        </el-table-column>

        <el-table-column :label="t('accommodation.roomTable.columns.occupiedRooms')" width="60" align="center">
          <template #header>
            <span>{{ t('accommodation.roomTable.columns.occupiedRooms') }}</span>
            <el-tooltip :content="t('accommodation.roomTable.tooltips.occupiedRooms')" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.occupiedRooms }}
          </template>
        </el-table-column>

        <el-table-column :label="t('accommodation.roomTable.columns.occupiedWithoutDeparture')" width="120" align="center">
          <template #header>
            <span>{{ t('accommodation.roomTable.columns.occupiedWithoutDeparture') }}</span>
            <el-tooltip :content="t('accommodation.roomTable.tooltips.occupiedWithoutDeparture')" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.occupiedWithoutDeparture }}
          </template>
        </el-table-column>

        <el-table-column :label="t('accommodation.roomTable.columns.scheduledDeparture')" width="60" align="center">
          <template #header>
            <span>{{ t('accommodation.roomTable.columns.scheduledDeparture') }}</span>
            <el-tooltip :content="t('accommodation.roomTable.tooltips.scheduledDeparture')" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.scheduledDeparture }}
          </template>
        </el-table-column>

        <el-table-column prop="scheduledArrival" :label="t('accommodation.roomTable.columns.scheduledArrival')" width="60" align="center" />
        <el-table-column prop="reservedRooms" :label="t('accommodation.roomTable.columns.reservedRooms')" width="120" align="center" />
        <el-table-column prop="maintenanceRooms" :label="t('accommodation.roomTable.columns.maintenanceRooms')" width="120" align="center" />
        <el-table-column prop="outOfOrderRooms" :label="t('accommodation.roomTable.columns.outOfOrderRooms')" width="120" align="center" />
        <el-table-column prop="linkedClosedRooms" :label="t('accommodation.roomTable.columns.linkedClosedRooms')" width="120" align="center" />
        <el-table-column prop="cleanRooms" :label="t('accommodation.roomTable.columns.cleanRooms')" width="60" align="center" />
        <el-table-column prop="dirtyRooms" :label="t('accommodation.roomTable.columns.dirtyRooms')" width="60" align="center" />
        
        <el-table-column :label="t('accommodation.roomTable.columns.expectedOccupancyRate')" width="120" align="center">
          <template #header>
            <span>{{ t('accommodation.roomTable.columns.expectedOccupancyRate') }}</span>
            <el-tooltip :content="t('accommodation.roomTable.tooltips.expectedOccupancyRate')" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.expectedOccupancyRate }}%
          </template>
        </el-table-column>

        <el-table-column :label="t('accommodation.roomTable.columns.dailyCancelledRooms')" width="120" align="center">
          <template #header>
            <span>{{ t('accommodation.roomTable.columns.dailyCancelledRooms') }}</span>
            <el-tooltip :content="t('accommodation.roomTable.tooltips.dailyCancelledRooms')" placement="top">
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
          :title="t('accommodation.roomTable.infoBanner')"
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
            :placeholder="t('accommodation.roomTable.selectStartDate')"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="onFutureDateChange"
            class="future-date-picker"
          />
          <span class="date-display" v-if="selectedDate">{{ getFormattedDateWithWeekday(selectedDate) }}</span>
        </div>
        <el-button :icon="ArrowRight" circle @click="nextPeriod" />
      </div>

      <!-- 远期房情主表格 -->
      <div class="future-table-container" v-loading="loading">
        <div class="table-scroll-wrapper">
          <table class="future-table-main" v-if="futureRoomTableData">
            <thead>
              <!-- 第一行：日期标题 -->
              <tr class="date-header-row">
                  <th rowspan="2" class="fixed-header room-type-col">{{ t('accommodation.roomTable.future.roomType') }}</th>
                  <th rowspan="2" class="fixed-header total-rooms-col">{{ t('accommodation.roomTable.future.totalRooms') }}</th>
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
                  <th class="sub-header">{{ t('accommodation.roomTable.future.available') }}</th>
                  <th class="sub-header">{{ t('accommodation.roomTable.future.occupied') }}</th>
                  <th class="sub-header">{{ t('accommodation.roomTable.future.unavailable') }}</th>
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
                <td class="fixed-cell room-type-cell">{{ t('accommodation.roomTable.future.percentageOfTotalRooms') }}</td>
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
                <td class="fixed-cell room-type-cell">{{ t('accommodation.roomTable.future.total') }}</td>
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
              {{ t('accommodation.roomTable.stats.effectiveRooms') }}
              <el-tooltip :content="t('accommodation.roomTable.tooltips.effectiveRooms')" placement="top">
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
              {{ t('accommodation.roomTable.stats.expectedOccupancy') }}
              <el-tooltip :content="t('accommodation.roomTable.tooltips.expectedOccupancy')" placement="top">
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
              {{ t('accommodation.roomTable.stats.expectedRoomRevenue') }}
              <el-tooltip :content="t('accommodation.roomTable.tooltips.expectedRoomRevenue')" placement="top">
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
              {{ t('accommodation.roomTable.stats.expectedTotalRoomFee') }}
              <el-tooltip :content="t('accommodation.roomTable.tooltips.expectedTotalRoomFee')" placement="top">
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
              {{ t('accommodation.roomTable.stats.averageRoomRevenue') }}
              <el-tooltip :content="t('accommodation.roomTable.tooltips.averageRoomRevenue')" placement="top">
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
import { useI18n } from 'vue-i18n'
import { request } from '@/utils/request'
import type { RoomTableData, RoomStatistics, ApiResponse, FutureRoomTableData } from '@/types/room'
import { getFutureRoomTableData } from '@/api/futureRoomTable'
import { useAccommodationI18n } from '@/composables/useAccommodationI18n'
import {
  addDaysToYmd,
  formatYmdMonthDay,
  getStoreTodayYmd,
  getYmdWeekdayIndex,
  parseYmdAsUtcDate,
} from '@/utils/storeDateTime'

// 响应式数据
const { t } = useI18n()
const { weekdayFullMap } = useAccommodationI18n()
const loading = ref(false)
const activeTab = ref<'daily' | 'future'>('daily')
const selectedDate = ref<string>(getStoreTodayYmd())
const roomTableData = ref<RoomTableData | null>(null)
const futureRoomTableData = ref<FutureRoomTableData | null>(null)

// 计算属性 - 表格数据（包含合计行）
const tableData = computed(() => {
  if (!roomTableData.value) return []
  
  const data = [...roomTableData.value.statistics]
  // 添加合计行
  data.push({
    ...roomTableData.value.total,
    roomTypeName: t('accommodation.roomTable.future.total')
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
    ElMessage.error(response.message || t('accommodation.roomTable.messages.loadDailyFailed'))
    }
  } catch (error) {
    console.error('获取房情表数据失败:', error)
    ElMessage.error(t('accommodation.roomTable.messages.loadDailyFailed'))
    
    // 添加模拟数据用于展示
    roomTableData.value = {
      date: date,
      statistics: [
        {
          roomTypeName: t('stage6.components.addRoomDialog.demoRoomTypes.queen'),
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
        roomTypeName: t('accommodation.roomTable.future.total'),
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
      ElMessage.error(response.message || t('accommodation.roomTable.messages.loadFutureFailed'))
    }
  } catch (error) {
    console.error('获取远期房情表数据失败:', error)
    ElMessage.error(t('accommodation.roomTable.messages.loadFutureFailed'))
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
  selectedDate.value = addDaysToYmd(selectedDate.value, -7)
  fetchFutureRoomTableData(selectedDate.value, 7)
}

// 下一个时间段
const nextPeriod = () => {
  selectedDate.value = addDaysToYmd(selectedDate.value, 7)
  fetchFutureRoomTableData(selectedDate.value, 7)
}

// 导出数据
const exportData = () => {
  if (activeTab.value === 'daily') {
    exportDailyRoomTable()
    return
  }
  exportFutureRoomTable()
}

const escapeHtml = (value: string | number) => {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const buildHtmlTable = (
  title: string,
  headers: Array<string | number>,
  rows: Array<Array<string | number>>
) => {
  const headerHtml = headers.map((header) => `<th>${escapeHtml(header)}</th>`).join('')
  const rowHtml = rows
    .map((row) => `<tr>${row.map((cell) => `<td>${escapeHtml(cell)}</td>`).join('')}</tr>`)
    .join('')

  return `
    <div class="sheet-section">
      <h3>${escapeHtml(title)}</h3>
      <table>
        <thead><tr>${headerHtml}</tr></thead>
        <tbody>${rowHtml}</tbody>
      </table>
    </div>
  `
}

const downloadExcelFile = (fileName: string, sections: string[]) => {
  const html = `
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="UTF-8" />
        <style>
          body { font-family: Arial, Helvetica, sans-serif; font-size: 12px; padding: 16px; }
          h3 { margin: 0 0 10px 0; font-size: 14px; }
          .sheet-section { margin-bottom: 20px; }
          table { border-collapse: collapse; width: 100%; margin-bottom: 8px; }
          th, td { border: 1px solid #dcdfe6; padding: 6px 8px; text-align: center; white-space: nowrap; }
          th { background: #f5f7fa; font-weight: 600; }
        </style>
      </head>
      <body>
        ${sections.join('')}
      </body>
    </html>
  `

  const blob = new Blob([`\uFEFF${html}`], {
    type: 'application/vnd.ms-excel;charset=utf-8;',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const exportDailyRoomTable = () => {
  if (!roomTableData.value || tableData.value.length === 0) {
    ElMessage.warning(t('accommodation.roomTable.messages.noDailyExportData'))
    return
  }

  const headers = [
    t('accommodation.roomTable.columns.roomTypeName'),
    t('accommodation.roomTable.columns.totalRooms'),
    t('accommodation.roomTable.columns.availableForSale'),
    t('accommodation.roomTable.columns.availableRooms'),
    t('accommodation.roomTable.columns.occupiedRooms'),
    t('accommodation.roomTable.columns.occupiedWithoutDeparture'),
    t('accommodation.roomTable.columns.scheduledDeparture'),
    t('accommodation.roomTable.columns.scheduledArrival'),
    t('accommodation.roomTable.columns.reservedRooms'),
    t('accommodation.roomTable.columns.maintenanceRooms'),
    t('accommodation.roomTable.columns.outOfOrderRooms'),
    t('accommodation.roomTable.columns.linkedClosedRooms'),
    t('accommodation.roomTable.columns.cleanRooms'),
    t('accommodation.roomTable.columns.dirtyRooms'),
    t('accommodation.roomTable.columns.expectedOccupancyRate'),
    t('accommodation.roomTable.columns.dailyCancelledRooms'),
  ]

  const rows = tableData.value.map((item: RoomStatistics) => [
    item.roomTypeName,
    item.totalRooms,
    item.availableForSale,
    item.availableRooms,
    item.occupiedRooms,
    item.occupiedWithoutDeparture,
    item.scheduledDeparture,
    item.scheduledArrival,
    item.reservedRooms,
    item.maintenanceRooms,
    item.outOfOrderRooms,
    item.linkedClosedRooms,
    item.cleanRooms,
    item.dirtyRooms,
    `${item.expectedOccupancyRate}%`,
    item.dailyCancelledRooms,
  ])

  const section = buildHtmlTable(t('accommodation.roomTable.export.dailyTitle', { date: selectedDate.value }), headers, rows)
  downloadExcelFile(t('accommodation.roomTable.export.dailyFileName', { date: selectedDate.value }), [section])
  ElMessage.success(t('accommodation.roomTable.messages.dailyExportSuccess'))
}

const exportFutureRoomTable = () => {
  if (!futureRoomTableData.value) {
    ElMessage.warning(t('accommodation.roomTable.messages.noFutureExportData'))
    return
  }

  const futureData = futureRoomTableData.value
  const mainHeaders: Array<string | number> = [t('accommodation.roomTable.future.roomType'), t('accommodation.roomTable.future.totalRooms')]
  futureData.total.dates.forEach((date) => {
    mainHeaders.push(`${date.date} ${t('accommodation.roomTable.future.available')}`, `${date.date} ${t('accommodation.roomTable.future.occupied')}`, `${date.date} ${t('accommodation.roomTable.future.unavailable')}`)
  })

  const mainRows: Array<Array<string | number>> = futureData.roomTypes.map((roomType) => {
    const row: Array<string | number> = [roomType.roomTypeName, roomType.totalRooms]
    roomType.dates.forEach((date) => {
      row.push(date.available, date.occupied, date.unavailable)
    })
    return row
  })

  const percentageRow: Array<string | number> = [t('accommodation.roomTable.future.percentageOfTotalRooms'), '-']
  futureData.total.dates.forEach((date) => {
    percentageRow.push(`${date.availableRate}%`, `${date.occupiedRate}%`, `${date.unavailableRate}%`)
  })
  mainRows.push(percentageRow)

  const totalRow: Array<string | number> = [t('accommodation.roomTable.future.total'), futureData.total.totalRooms]
  futureData.total.dates.forEach((date) => {
    totalRow.push(date.available, date.occupied, date.unavailable)
  })
  mainRows.push(totalRow)

  const statsHeaders: Array<string | number> = [t('accommodation.common.item')]
  futureData.statistics.forEach((stat) => {
    statsHeaders.push(stat.date)
  })

  const statsRows: Array<Array<string | number>> = [
    [t('accommodation.roomTable.stats.effectiveRooms'), ...futureData.statistics.map((stat) => stat.effectiveRooms)],
    [t('accommodation.roomTable.stats.expectedOccupancy'), ...futureData.statistics.map((stat) => `${stat.expectedOccupancyRate}%`)],
    [t('accommodation.roomTable.stats.expectedRoomRevenue'), ...futureData.statistics.map((stat) => stat.expectedRoomRevenue)],
    [t('accommodation.roomTable.stats.expectedTotalRoomFee'), ...futureData.statistics.map((stat) => stat.expectedTotalRoomFee)],
    [t('accommodation.roomTable.stats.averageRoomRevenue'), ...futureData.statistics.map((stat) => stat.averageRoomRevenue)],
  ]

  const mainSection = buildHtmlTable(
    t('accommodation.roomTable.export.futureMainTitle', { startDate: futureData.startDate, endDate: futureData.endDate }),
    mainHeaders,
    mainRows
  )
  const statsSection = buildHtmlTable(t('accommodation.roomTable.export.futureStatsTitle'), statsHeaders, statsRows)
  downloadExcelFile(t('accommodation.roomTable.export.futureFileName', { startDate: futureData.startDate, endDate: futureData.endDate }), [
    mainSection,
    statsSection,
  ])
  ElMessage.success(t('accommodation.roomTable.messages.futureExportSuccess'))
}

// 获取格式化的日期和星期
const getFormattedDateWithWeekday = (value: Date | string): string => {
  let ymd: string
  if (typeof value === 'string') {
    ymd = value
  } else {
    ymd = value.toISOString().slice(0, 10)
  }
  
  const date = parseYmdAsUtcDate(ymd)
  const year = date.getUTCFullYear()
  const month = String(date.getUTCMonth() + 1).padStart(2, '0')
  const day = String(date.getUTCDate()).padStart(2, '0')
  
  const weekday = weekdayFullMap.value[getYmdWeekdayIndex(ymd)]
  
  return `${year}-${month}-${day} ${weekday}`
}

// 格式化表格表头日期
const formatDateHeader = (dateStr: string, dayOfWeek: string): string => {
  const today = getStoreTodayYmd()
  const isToday = dateStr === today
  const { month, day } = formatYmdMonthDay(dateStr)
  
  return isToday ? `${month}-${day} ${t('accommodation.common.today')}` : `${month}-${day} ${dayOfWeek}`
}

// 格式化统计区域的日期标题
const formatStatisticDateHeader = (dateStr: string): string => {
  const today = getStoreTodayYmd()
  const isToday = dateStr === today
  const { month, day } = formatYmdMonthDay(dateStr)
  
  // 获取星期几
  const weekday = weekdayFullMap.value[getYmdWeekdayIndex(dateStr)]

  return isToday ? `${month}-${day} ${t('accommodation.common.today')}` : `${month}-${day} ${weekday}`
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
