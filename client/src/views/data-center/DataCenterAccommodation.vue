<template>
  <StatisticsLayout>
    <div class="accommodation-container">
    <!-- 日期筛选 -->
    <div class="filter-section">
      <el-select v-model="dateType" style="width: 100px">
        <el-option :label="t('stage5.common.date.today')" value="today" />
        <el-option :label="t('stage5.common.date.yesterday')" value="yesterday" />
        <el-option :label="t('stage5.common.date.thisWeek')" value="week" />
        <el-option :label="t('stage5.common.date.thisMonth')" value="month" />
      </el-select>
      <el-date-picker
        v-model="startDate"
        type="date"
        :placeholder="t('stage5.common.date.selectDate')"
        format="YYYY/MM/DD"
        value-format="YYYY-MM-DD"
      />
      <span class="date-separator">{{ t('stage5.common.date.rangeTo') }}</span>
      <el-date-picker
        v-model="endDate"
        type="date"
        :placeholder="t('stage5.common.date.selectDate')"
        format="YYYY/MM/DD"
        value-format="YYYY-MM-DD"
      />
    </div>

    <!-- 经营指标统计 -->
    <div class="metrics-section">
      <h3 class="section-title">
        {{ t('stage5.statistics.accommodation.operationalMetrics') }}
        <el-tooltip :content="t('stage5.statistics.common.statsInfo')" placement="right">
          <el-icon class="info-icon"><QuestionFilled /></el-icon>
        </el-tooltip>
      </h3>

      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-icon">
            <el-icon size="32" color="#5b8ff9"><House /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">{{ t('stage5.statistics.accommodation.totalRoomFee') }}</div>
            <div class="metric-value">¥{{ metrics.totalRoomFee.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon">
            <el-icon size="32" color="#667eea"><TrendCharts /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">
              {{ t('stage5.statistics.accommodation.avgDailyRate') }}
              <el-tooltip :content="t('stage5.statistics.accommodation.avgDailyRate')" placement="top">
                <el-icon class="small-info"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div class="metric-value">¥{{ metrics.avgDailyRate.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon">
            <el-icon size="32" color="#67c23a"><SuccessFilled /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">
              {{ t('stage5.statistics.accommodation.occupancyRate') }}
              <el-tooltip :content="t('stage5.statistics.accommodation.occupancyRate')" placement="top">
                <el-icon class="small-info"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div class="metric-value">{{ metrics.occupancyRate }}%</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon">
            <el-icon size="32" color="#f9c94a"><Money /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">
              {{ t('stage5.statistics.accommodation.revPAR') }}
              <el-tooltip :content="t('stage5.statistics.accommodation.revPAR')" placement="top">
                <el-icon class="small-info"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div class="metric-value">¥{{ metrics.revPAR.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon">
            <el-icon size="32" color="#f56c6c"><Calendar /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">
              {{ t('stage5.statistics.accommodation.totalRoomNights') }}
              <el-tooltip :content="t('stage5.statistics.accommodation.roomNights')" placement="top">
                <el-icon class="small-info"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div class="metric-value">{{ metrics.totalRoomNights.toFixed(2) }}</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon">
            <el-icon size="32" color="#e6a23c"><Clock /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">
              {{ t('stage5.statistics.accommodation.avgDailyRoomNights') }}
              <el-tooltip :content="t('stage5.statistics.accommodation.avgDailyRoomNights')" placement="top">
                <el-icon class="small-info"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div class="metric-value">{{ metrics.avgDailyRoomNights.toFixed(2) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 经营指标趋势图 -->
    <div class="trend-section">
      <h3 class="section-title">{{ t('stage5.statistics.accommodation.operationalTrend') }}</h3>

      <div class="trend-tabs">
        <el-button
          v-for="tab in trendTabs"
          :key="tab.key"
          :type="activeTrendTab === tab.key ? 'primary' : 'default'"
          @click="switchTrendTab(tab.key)"
        >
          {{ tab.label }}
        </el-button>
      </div>

      <div ref="lineChartRef" class="trend-chart"></div>
    </div>

    <!-- 数据明细表格 -->
    <div class="table-section">
      <h3 class="section-title">
        {{ t('stage5.statistics.accommodation.dataDetails') }} ({{ dateRangeLabel }})
        <el-button type="primary" style="float: right">{{ t('stage5.common.actions.exportDetails') }}</el-button>
      </h3>

      <div class="table-tabs">
        <el-button
          v-for="tab in tableTabs"
          :key="tab.key"
          :type="activeTableTab === tab.key ? 'primary' : 'default'"
          size="small"
          @click="activeTableTab = tab.key"
        >
          {{ tab.label }}
        </el-button>
      </div>

      <!-- 房费明细表格 -->
      <el-table v-if="activeTableTab === 'room-fee'" :data="roomFeeData" border stripe class="detail-table">
        <el-table-column prop="roomType" :label="t('stage5.common.fields.roomType')" min-width="150" align="center" />
        <el-table-column prop="roomNumber" :label="t('stage5.common.fields.room')" min-width="120" align="center" />
        <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150" align="center">
          <template #default="{ row }">
            <span class="amount-bold">¥{{ row.total.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentDate" :label="currentDateLabel" min-width="150" align="center">
          <template #default="{ row }">
            ¥{{ row.currentDate.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 间夜明细表格 -->
      <el-table v-if="activeTableTab === 'checkin'" :data="checkinData" border stripe class="detail-table">
        <el-table-column prop="roomType" :label="t('stage5.common.fields.roomType')" min-width="150" align="center" />
        <el-table-column prop="roomNumber" :label="t('stage5.common.fields.room')" min-width="120" align="center" />
        <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150" align="center">
          <template #default="{ row }">
            <span class="amount-bold">{{ row.total }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentDate" :label="currentDateLabel" min-width="150" align="center" />
      </el-table>

      <!-- 入住率明细表格 -->
      <el-table v-if="activeTableTab === 'occupancy'" :data="occupancyData" border stripe class="detail-table">
        <el-table-column prop="roomType" :label="t('stage5.common.fields.roomType')" min-width="200" align="center" />
        <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150" align="center">
          <template #default="{ row }">
            <span class="amount-bold">{{ row.total.toFixed(2) }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentDate" :label="currentDateLabel" min-width="150" align="center">
          <template #default="{ row }">{{ row.currentDate.toFixed(2) }}%</template>
        </el-table-column>
      </el-table>

      <!-- RevPAR明细表格 -->
      <el-table v-if="activeTableTab === 'revpar'" :data="revparData" border stripe class="detail-table">
        <el-table-column prop="roomType" :label="t('stage5.common.fields.roomType')" min-width="200" align="center" />
        <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150" align="center">
          <template #default="{ row }">
            <span class="amount-bold">¥{{ row.total.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentDate" :label="currentDateLabel" min-width="150" align="center">
          <template #default="{ row }">
            ¥{{ row.currentDate.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { QuestionFilled, House, TrendCharts, SuccessFilled, Money, Calendar, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import StatisticsLayout from '../statistics/StatisticsLayout.vue'
import {
  getOperationalMetrics,
  type OperationalMetricsDTO,
  type OperationalRoomDetailDTO,
} from '@/api/statistics'
import { getStoreTodayYmd } from '@/utils/storeDateTime'

const { t } = useI18n()
const dateType = ref('today')
const loading = ref(false)

// 自动获取今天的日期
const getTodayDate = () => {
  return getStoreTodayYmd()
}

const startDate = ref(getTodayDate())
const endDate = ref(getTodayDate())

const toNumber = (value: unknown): number => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

// 日期范围标签 - 动态显示选择的日期范围
const dateRangeLabel = computed(() => {
  return t('stage5.common.date.dateRange', { start: startDate.value, end: endDate.value })
})

const currentDateLabel = computed(() => {
  const current = endDate.value || getTodayDate()
  const [year, month, day] = current.split('-').map(item => Number(item))

  if (!year || !month || !day) {
    const today = getTodayDate()
    const [, todayMonth, todayDay] = today.split('-').map(item => Number(item))
    return t('stage5.common.date.monthDay', {
      month: todayMonth,
      day: todayDay,
    })
  }

  return t('stage5.common.date.monthDay', { month, day })
})

// 经营指标数据
const metrics = ref({
  totalRoomFee: 0,
  avgDailyRate: 0,
  occupancyRate: 0,
  revPAR: 0,
  totalRoomNights: 0,
  avgDailyRoomNights: 0,
})

/**
 * 加载经营指标数据
 */
const loadOperationalMetrics = async () => {
  try {
    loading.value = true
    const response = await getOperationalMetrics({
      startDate: startDate.value,
      endDate: endDate.value
    })

    if (response.success && response.data) {
      const data = response.data
      applyOperationalMetricsData(data)
    } else {
      ElMessage.error(response.message || t('stage5.statistics.accommodation.loadMetricsFailed'))
    }
  } catch (error) {
    console.error('Failed to load operational metrics:', error)
    ElMessage.error(t('stage5.statistics.accommodation.loadMetricsFailed'))
  } finally {
    loading.value = false
  }
}

const mapRoomDetailRows = (details: OperationalRoomDetailDTO[] | undefined) => {
  const rows = (details || []).map(item => ({
    roomType: item.roomType || '-',
    roomNumber:
      item.roomNumber === '小计' || item.roomNumber === t('stage5.statistics.accommodation.subtotal')
        ? t('stage5.statistics.accommodation.subtotal')
        : item.roomNumber || '-',
    isSubtotal:
      item.roomNumber === '小计' || item.roomNumber === t('stage5.statistics.accommodation.subtotal'),
    total: toNumber(item.total),
    currentDate: toNumber(item.currentDate),
  }))

  const roomCountByType = new Map<string, number>()
  rows.forEach(row => {
    if (!row.isSubtotal) {
      roomCountByType.set(row.roomType, (roomCountByType.get(row.roomType) || 0) + 1)
    }
  })

  // 单房间房型的小计与明细完全重复，过滤掉该小计行以避免视觉重复。
  return rows.filter(row => {
    if (!row.isSubtotal) {
      return true
    }
    return (roomCountByType.get(row.roomType) || 0) > 1
  })
}

const applyOperationalMetricsData = (data: OperationalMetricsDTO) => {
  metrics.value = {
    totalRoomFee: toNumber(data.totalRoomFee),
    avgDailyRate: toNumber(data.averageDailyRate),
    occupancyRate: toNumber(data.occupancyRate),
    revPAR: toNumber(data.revPAR),
    totalRoomNights: toNumber(data.totalSoldRoomNights),
    avgDailyRoomNights:
      toNumber(data.days) > 0 ? toNumber(data.totalSoldRoomNights) / toNumber(data.days) : 0,
  }

  const dailyTrends = data.dailyTrends || []
  dates.value = dailyTrends.map(item => item.date)
  trendData.value = {
    'room-fee': dailyTrends.map(item => toNumber(item.totalRoomFee)),
    'avg-price': dailyTrends.map(item => toNumber(item.averageDailyRate)),
    'avg-revenue': dailyTrends.map(item => toNumber(item.revPAR)),
    occupancy: dailyTrends.map(item => toNumber(item.roomNights)),
  }

  roomFeeData.value = mapRoomDetailRows(data.roomFeeDetails)
  checkinData.value = mapRoomDetailRows(data.roomNightsDetails)
  occupancyData.value = (data.occupancyDetails || []).map(item => ({
    roomType: item.roomType || '-',
    total: toNumber(item.total),
    currentDate: toNumber(item.currentDate),
  }))
  revparData.value = mapRoomDetailRows(data.revparDetails)

  if (lineChart) {
    updateLineChart(activeTrendTab.value)
  }
}

// 趋势图选项卡
const trendTabs = computed(() => [
  { key: 'room-fee', label: t('stage5.statistics.accommodation.totalRoomFee') },
  { key: 'avg-price', label: t('stage5.statistics.accommodation.avgDailyRate') },
  { key: 'avg-revenue', label: t('stage5.statistics.accommodation.revPAR') },
  { key: 'occupancy', label: t('stage5.statistics.accommodation.roomNights') },
])
const activeTrendTab = ref('room-fee')

// 趋势数据(待从后端获取)
const dates = ref<string[]>([])
const trendData = ref<Record<string, number[]>>({
  'room-fee': [],
  'avg-price': [],
  'avg-revenue': [],
  'occupancy': []
})

// 表格选项卡
const tableTabs = computed(() => [
  { key: 'room-fee', label: t('stage5.statistics.accommodation.roomFeeDetails') },
  { key: 'checkin', label: t('stage5.statistics.accommodation.roomNightDetails') },
  { key: 'occupancy', label: t('stage5.statistics.accommodation.occupancyDetails') },
  { key: 'revpar', label: t('stage5.statistics.accommodation.revparDetails') },
])
const activeTableTab = ref('room-fee')

// 房费明细数据
interface RoomFeeItem {
  roomType: string
  roomNumber: string
  isSubtotal?: boolean
  total: number
  currentDate: number
}

const roomFeeData = ref<RoomFeeItem[]>([])

// 间夜明细数据
interface CheckinItem {
  roomType: string
  roomNumber: string
  isSubtotal?: boolean
  total: number
  currentDate: number
}

const checkinData = ref<CheckinItem[]>([])

// 入住率明细数据
interface OccupancyItem {
  roomType: string
  total: number
  currentDate: number
}

const occupancyData = ref<OccupancyItem[]>([])

// RevPAR明细数据
interface RevPARItem {
  roomType: string
  isSubtotal?: boolean
  total: number
  currentDate: number
}

const revparData = ref<RevPARItem[]>([])

// ECharts实例
const lineChartRef = ref<HTMLDivElement>()
let lineChart: ECharts | null = null

// 初始化折线图
const initLineChart = () => {
  if (!lineChartRef.value) return

  lineChart = echarts.init(lineChartRef.value)
  updateLineChart('room-fee')
}

// 更新折线图数据
const updateLineChart = (tabKey: string) => {
  if (!lineChart) return

  const data = trendData.value[tabKey] || []
  const tabLabel = trendTabs.value.find(tab => tab.key === tabKey)?.label || ''

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const param = params[0]
        let value = param.value
        if (tabKey === 'room-fee' || tabKey === 'avg-price' || tabKey === 'avg-revenue') {
          value = `¥${value.toFixed(2)}`
        } else {
          value = value.toFixed(2)
        }
        return `${param.axisValue}<br/>${param.marker}${tabLabel}: ${value}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates.value,
      boundaryGap: false,
      axisLabel: {
        interval: 0
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => {
          if (tabKey === 'room-fee' || tabKey === 'avg-price' || tabKey === 'avg-revenue') {
            return `¥${value}`
          }
          return value.toString()
        }
      }
    },
    series: [
      {
        name: tabLabel,
        type: 'line',
        smooth: true,
        data: data,
        lineStyle: {
          width: 3
        },
        itemStyle: {
          color: '#5b8ff9'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(91, 143, 249, 0.3)' },
              { offset: 1, color: 'rgba(91, 143, 249, 0.05)' }
            ]
          }
        }
      }
    ]
  }

  lineChart.setOption(option)
}

// 切换趋势图标签
const switchTrendTab = (tabKey: string) => {
  activeTrendTab.value = tabKey
  updateLineChart(tabKey)
}

// 响应式调整
const handleResize = () => {
  lineChart?.resize()
}

// 监听日期变化
watch([startDate, endDate], () => {
  loadOperationalMetrics()
})

onMounted(() => {
  // 加载初始数据
  loadOperationalMetrics()

  initLineChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  lineChart?.dispose()
})
</script>

<style scoped>
.accommodation-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

/* 筛选器 */
.filter-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.date-separator {
  color: #606266;
  font-size: 14px;
}

/* 指标区域 */
.metrics-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-icon {
  color: #909399;
  cursor: help;
}

.small-info {
  font-size: 12px;
  color: #909399;
  cursor: help;
  margin-left: 4px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.metric-card {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
}

.metric-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.metric-icon {
  flex-shrink: 0;
}

.metric-content {
  flex: 1;
}

.metric-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

/* 趋势区域 */
.trend-section {
  margin-bottom: 32px;
}

.trend-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.trend-chart {
  width: 100%;
  height: 350px;
  margin-top: 20px;
}

/* 表格区域 */
.table-section {
  margin-top: 32px;
}

.table-tabs {
  display: flex;
  gap: 8px;
  margin: 16px 0;
}

.detail-table {
  margin-top: 16px;
}

.detail-table :deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.amount-bold {
  font-weight: 600;
  color: #303133;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table .cell) {
  padding: 12px 8px;
}
</style>
