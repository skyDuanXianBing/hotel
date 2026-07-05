<template>
  <StatisticsLayout>
    <div class="accommodation-container" v-loading="loading">
      <div class="filter-section">
        <el-select v-model="dateType" class="business-quick-select">
          <el-option :label="t('stage5.common.date.today')" value="today" />
          <el-option :label="t('stage5.common.date.yesterday')" value="yesterday" />
          <el-option :label="t('stage5.common.date.thisWeek')" value="week" />
          <el-option :label="t('stage5.common.date.thisMonth')" value="month" />
        </el-select>
        <el-date-picker
          v-model="businessDateRange"
          class="business-date-range"
          type="daterange"
          :placeholder="t('stage5.common.date.selectDate')"
          :start-placeholder="t('stage5.common.date.selectDate')"
          :end-placeholder="t('stage5.common.date.selectDate')"
          :range-separator="t('stage5.common.date.rangeTo')"
          format="YYYY/MM/DD"
          value-format="YYYY-MM-DD"
          :clearable="false"
        />
      </div>

      <el-alert
        v-if="loadError"
        class="state-alert"
        type="error"
        :title="loadError"
        :closable="false"
        show-icon
      />

      <el-empty
        v-if="showEmpty"
        class="page-empty"
        :description="t('stage5.dataCenter.overview.noData')"
      />

      <div v-if="contentReady" class="metrics-grid">
        <div v-for="card in metricCards" :key="card.key" class="metric-card">
          <div class="metric-copy">
            <div class="metric-label">
              {{ card.label }}
              <el-tooltip v-if="card.tooltip" :content="card.tooltip" placement="top">
                <el-icon class="small-info"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div class="metric-value">{{ card.value }}</div>
          </div>
          <img :src="card.icon" :alt="card.label" class="metric-icon-image" />
        </div>
      </div>

      <section v-if="contentReady" class="trend-section">
        <h3 class="section-title">{{ t('stage5.statistics.accommodation.operationalMetrics') }}</h3>

        <div class="trend-tabs">
          <el-button
            v-for="tab in trendTabs"
            :key="tab.key"
            :class="{ active: activeTrendTab === tab.key }"
            @click="switchTrendTab(tab.key)"
          >
            {{ tab.label }}
          </el-button>
        </div>

        <div ref="lineChartRef" class="trend-chart"></div>
      </section>

      <section v-if="contentReady" class="table-section">
        <div class="table-header">
          <h3 class="section-title table-title">
            {{ t('stage5.statistics.accommodation.dataDetails') }} ({{ dateRangeLabel }})
          </h3>
          <el-button
            type="primary"
            class="export-button"
            :loading="exporting"
            @click="handleExport"
          >
            {{ t('stage5.common.actions.exportDetails') }}
          </el-button>
        </div>

        <div class="table-tabs">
          <el-button
            v-for="tab in tableTabs"
            :key="tab.key"
            :class="{ active: activeTableTab === tab.key }"
            @click="activeTableTab = tab.key"
          >
            {{ tab.label }}
          </el-button>
        </div>

        <el-table v-if="activeTableTab === 'room-fee'" :data="roomFeeData" class="detail-table">
          <el-table-column prop="roomType" :label="t('stage5.common.fields.roomType')" min-width="150" />
          <el-table-column prop="roomNumber" :label="t('stage5.common.fields.room')" min-width="120" />
          <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150">
            <template #default="{ row }">
              <span class="amount-bold">{{ formatCurrency(row.total) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="currentDate" :label="currentDateLabel" min-width="150">
            <template #default="{ row }">
              {{ formatCurrency(row.currentDate) }}
            </template>
          </el-table-column>
        </el-table>

        <el-table v-if="activeTableTab === 'checkin'" :data="checkinData" class="detail-table">
          <el-table-column prop="roomType" :label="t('stage5.common.fields.roomType')" min-width="150" />
          <el-table-column prop="roomNumber" :label="t('stage5.common.fields.room')" min-width="120" />
          <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150">
            <template #default="{ row }">
              <span class="amount-bold">{{ formatNumber(row.total) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="currentDate" :label="currentDateLabel" min-width="150">
            <template #default="{ row }">{{ formatNumber(row.currentDate) }}</template>
          </el-table-column>
        </el-table>

        <el-table v-if="activeTableTab === 'occupancy'" :data="occupancyData" class="detail-table">
          <el-table-column prop="roomType" :label="t('stage5.common.fields.roomType')" min-width="200" />
          <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150">
            <template #default="{ row }">
              <span class="amount-bold">{{ formatPercent(row.total) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="currentDate" :label="currentDateLabel" min-width="150">
            <template #default="{ row }">{{ formatPercent(row.currentDate) }}</template>
          </el-table-column>
        </el-table>

        <el-table v-if="activeTableTab === 'revpar'" :data="revparData" class="detail-table">
          <el-table-column prop="roomType" :label="t('stage5.common.fields.roomType')" min-width="200" />
          <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150">
            <template #default="{ row }">
              <span class="amount-bold">{{ formatCurrency(row.total) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="currentDate" :label="currentDateLabel" min-width="150">
            <template #default="{ row }">
              {{ formatCurrency(row.currentDate) }}
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import StatisticsLayout from '../statistics/StatisticsLayout.vue'
import {
  downloadStatisticsReport,
  getStatisticsReportErrorMessage,
  getOperationalMetrics,
  saveBlobDownload,
  type OperationalMetricsDTO,
  type OperationalRoomDetailDTO,
} from '@/api/statistics'
import { addDaysToYmd, getStoreTodayYmd, getYmdMonthStart, getYmdWeekStart } from '@/utils/storeDateTime'
import businessHomeIcon from '@/assets/icons/statistics/business-home.png'
import businessDepositIcon from '@/assets/icons/statistics/business-deposit.png'
import avgRoomRevenueIcon from '@/assets/icons/statistics/accommodation-avg-room-revenue.png'
import avgDailyRoomNightsIcon from '@/assets/icons/statistics/accommodation-avg-daily-nights.png'
import soldRoomNightsIcon from '@/assets/icons/statistics/accommodation-sold-room-nights.png'
import occupancyRateIcon from '@/assets/icons/statistics/accommodation-occupancy-rate.png'

const { t } = useI18n()
const dateType = ref('today')
const loading = ref(false)
const exporting = ref(false)
const loadError = ref('')

const getTodayDate = () => getStoreTodayYmd()

const startDate = ref(getTodayDate())
const endDate = ref(getTodayDate())

const businessDateRange = computed<string[]>({
  get: () => {
    if (!startDate.value || !endDate.value) return []
    return [startDate.value, endDate.value]
  },
  set: (value: string[]) => {
    const [start, end] = value || []
    startDate.value = start || ''
    endDate.value = end || ''
  },
})

const updateDateRange = (type: string) => {
  const today = getTodayDate()

  switch (type) {
    case 'today':
      startDate.value = today
      endDate.value = today
      break
    case 'yesterday': {
      const yesterday = addDaysToYmd(today, -1)
      startDate.value = yesterday
      endDate.value = yesterday
      break
    }
    case 'week':
      startDate.value = getYmdWeekStart(today)
      endDate.value = today
      break
    case 'month':
      startDate.value = getYmdMonthStart(today)
      endDate.value = today
      break
  }
}

const toNumber = (value: unknown): number => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const formatNumber = (value: number) =>
  value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })

const formatCurrency = (value: number) => `¥${formatNumber(value)}`
const formatPercent = (value: number) => `${formatNumber(value)}%`

const handleExport = async () => {
  if (!startDate.value || !endDate.value) {
    ElMessage.warning(t('stage5.common.messages.pleaseSelectDateRange'))
    return
  }

  try {
    exporting.value = true
    const download = await downloadStatisticsReport('room-fees', {
      startDate: startDate.value,
      endDate: endDate.value,
    })
    saveBlobDownload(download)
  } catch (error) {
    console.error('Failed to export accommodation report:', error)
    const message = await getStatisticsReportErrorMessage(
      error,
      t('stage5.statistics.reports.downloadFailed'),
    )
    ElMessage.error(message)
  } finally {
    exporting.value = false
  }
}

const dateRangeLabel = computed(() => {
  return t('stage5.common.date.dateRange', { start: startDate.value, end: endDate.value })
})

const hasMetricsData = computed(() =>
  metrics.value.totalRoomFee > 0 ||
  metrics.value.totalRoomNights > 0 ||
  dates.value.length > 0 ||
  roomFeeData.value.length > 0 ||
  checkinData.value.length > 0 ||
  occupancyData.value.length > 0 ||
  revparData.value.length > 0,
)

const showEmpty = computed(() => !loading.value && !loadError.value && !hasMetricsData.value)
const contentReady = computed(() => !loadError.value && !showEmpty.value)

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

const metrics = ref({
  totalRoomFee: 0,
  avgDailyRate: 0,
  occupancyRate: 0,
  revPAR: 0,
  totalRoomNights: 0,
  avgDailyRoomNights: 0,
})

const metricCards = computed(() => [
  {
    key: 'total-room-fee',
    label: t('stage5.statistics.accommodation.totalRoomFee'),
    value: formatCurrency(metrics.value.totalRoomFee),
    icon: businessHomeIcon,
  },
  {
    key: 'avg-daily-rate',
    label: t('stage5.statistics.accommodation.avgDailyRate'),
    value: formatCurrency(metrics.value.avgDailyRate),
    icon: businessDepositIcon,
    tooltip: t('stage5.statistics.accommodation.avgDailyRate'),
  },
  {
    key: 'occupancy-rate',
    label: t('stage5.statistics.accommodation.occupancyRate'),
    value: formatPercent(metrics.value.occupancyRate),
    icon: occupancyRateIcon,
    tooltip: t('stage5.statistics.accommodation.occupancyRate'),
  },
  {
    key: 'revpar',
    label: t('stage5.statistics.accommodation.revPAR'),
    value: formatCurrency(metrics.value.revPAR),
    icon: avgRoomRevenueIcon,
    tooltip: t('stage5.statistics.accommodation.revPAR'),
  },
  {
    key: 'total-room-nights',
    label: t('stage5.statistics.accommodation.totalRoomNights'),
    value: formatNumber(metrics.value.totalRoomNights),
    icon: soldRoomNightsIcon,
    tooltip: t('stage5.statistics.accommodation.roomNights'),
  },
  {
    key: 'avg-daily-room-nights',
    label: t('stage5.statistics.accommodation.avgDailyRoomNights'),
    value: formatNumber(metrics.value.avgDailyRoomNights),
    icon: avgDailyRoomNightsIcon,
    tooltip: t('stage5.statistics.accommodation.avgDailyRoomNights'),
  },
])

const loadOperationalMetrics = async () => {
  try {
    loading.value = true
    const response = await getOperationalMetrics({
      startDate: startDate.value,
      endDate: endDate.value,
    })

    if (response.success && response.data) {
      loadError.value = ''
      applyOperationalMetricsData(response.data)
      await nextTick()
      if (!lineChart && lineChartRef.value) {
        initLineChart()
      } else {
        updateLineChart(activeTrendTab.value)
      }
    } else {
      loadError.value = response.message || t('stage5.statistics.accommodation.loadMetricsFailed')
      ElMessage.error(loadError.value)
    }
  } catch (error) {
    loadError.value = t('stage5.statistics.accommodation.loadMetricsFailed')
    console.error('Failed to load operational metrics:', error)
    ElMessage.error(loadError.value)
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

const trendTabs = computed(() => [
  { key: 'room-fee', label: t('stage5.statistics.accommodation.totalRoomFee') },
  { key: 'avg-price', label: t('stage5.statistics.accommodation.avgDailyRate') },
  { key: 'avg-revenue', label: t('stage5.statistics.accommodation.revPAR') },
  { key: 'occupancy', label: t('stage5.statistics.accommodation.roomNights') },
])
const activeTrendTab = ref('room-fee')

const dates = ref<string[]>([])
const trendData = ref<Record<string, number[]>>({
  'room-fee': [],
  'avg-price': [],
  'avg-revenue': [],
  occupancy: [],
})

const tableTabs = computed(() => [
  { key: 'room-fee', label: t('stage5.statistics.accommodation.roomFeeDetails') },
  { key: 'checkin', label: t('stage5.statistics.accommodation.roomNightDetails') },
  { key: 'occupancy', label: t('stage5.statistics.accommodation.occupancyDetails') },
  { key: 'revpar', label: t('stage5.statistics.accommodation.revparDetails') },
])
const activeTableTab = ref('room-fee')

interface RoomFeeItem {
  roomType: string
  roomNumber: string
  isSubtotal?: boolean
  total: number
  currentDate: number
}

const roomFeeData = ref<RoomFeeItem[]>([])

interface CheckinItem {
  roomType: string
  roomNumber: string
  isSubtotal?: boolean
  total: number
  currentDate: number
}

const checkinData = ref<CheckinItem[]>([])

interface OccupancyItem {
  roomType: string
  total: number
  currentDate: number
}

const occupancyData = ref<OccupancyItem[]>([])

interface RevPARItem {
  roomType: string
  roomNumber?: string
  isSubtotal?: boolean
  total: number
  currentDate: number
}

const revparData = ref<RevPARItem[]>([])

const lineChartRef = ref<HTMLDivElement>()
let lineChart: ECharts | null = null

const initLineChart = () => {
  if (!lineChartRef.value) return

  lineChart = echarts.init(lineChartRef.value)
  updateLineChart('room-fee')
}

const createEmptyChartOption = () => ({
  graphic: {
    type: 'text',
    left: 'center',
    top: 'middle',
    style: {
      text: t('stage5.dataCenter.overview.noData'),
      fill: '#8a8f99',
      fontSize: 14,
      fontWeight: 500,
    },
  },
  xAxis: { show: false },
  yAxis: { show: false },
  series: [],
})

const getXAxisLabelInterval = (dateCount: number) => {
  if (dateCount <= 7) return 0
  if (dateCount <= 14) return 1
  if (dateCount <= 31) return 3
  return Math.ceil(dateCount / 8) - 1
}

const updateLineChart = (tabKey: string) => {
  if (!lineChart) return

  const data = trendData.value[tabKey] || []
  if (!dates.value.length || !data.length) {
    lineChart.setOption(createEmptyChartOption(), true)
    return
  }
  const tabLabel = trendTabs.value.find(tab => tab.key === tabKey)?.label || ''
  const isMoney = tabKey === 'room-fee' || tabKey === 'avg-price' || tabKey === 'avg-revenue'
  const xAxisLabelInterval = getXAxisLabelInterval(dates.value.length)
  const showAllSymbols = dates.value.length <= 14

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#d9e7ff',
      borderWidth: 1,
      textStyle: {
        color: '#2f3440',
        fontSize: 12,
      },
      formatter: (params: any) => {
        const param = params[0]
        const value = isMoney ? formatCurrency(Number(param.value)) : formatNumber(Number(param.value))
        return `${param.axisValue}<br/>${param.marker}${tabLabel}: ${value}`
      },
    },
    grid: {
      left: 22,
      right: 22,
      bottom: 34,
      top: 18,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates.value,
      boundaryGap: false,
      axisTick: { show: false },
      axisLine: {
        lineStyle: {
          color: '#aeb6c2',
        },
      },
      axisLabel: {
        interval: xAxisLabelInterval,
        color: '#5f6670',
        fontSize: 12,
        hideOverlap: true,
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#d9dde4',
          type: 'dashed',
        },
      },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#5f6670',
        fontSize: 12,
        formatter: (value: number) => (isMoney ? `¥${value.toLocaleString('zh-CN')}` : value.toString()),
      },
      splitLine: {
        lineStyle: {
          color: '#d9dde4',
          type: 'dashed',
        },
      },
    },
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: 0,
        filterMode: 'none',
        zoomOnMouseWheel: true,
        moveOnMouseMove: true,
        moveOnMouseWheel: false,
      },
    ],
    series: [
      {
        name: tabLabel,
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        showSymbol: showAllSymbols,
        emphasis: {
          focus: 'series',
          scale: true,
        },
        data,
        lineStyle: {
          width: 2,
          color: '#198cff',
        },
        itemStyle: {
          color: '#ffffff',
          borderColor: '#198cff',
          borderWidth: 2,
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(25, 140, 255, 0.28)' },
              { offset: 1, color: 'rgba(25, 140, 255, 0.04)' },
            ],
          },
        },
      },
    ],
  }

  lineChart.setOption(option)
}

const switchTrendTab = (tabKey: string) => {
  activeTrendTab.value = tabKey
  updateLineChart(tabKey)
}

const handleResize = () => {
  lineChart?.resize()
}

watch([startDate, endDate], () => {
  loadOperationalMetrics()
})

watch(dateType, (newType) => {
  if (newType) {
    updateDateRange(newType)
  }
})

onMounted(() => {
  updateDateRange(dateType.value)
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
  min-width: 1218px;
  min-height: 100%;
  padding: 0 0 24px;
  background: transparent;
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 60px;
  margin-bottom: 10px;
  padding: 0 16px;
  background: #ffffff;
  border-radius: 4px;
}

.state-alert {
  margin-bottom: 10px;
}

.page-empty {
  min-height: 320px;
  background: #ffffff;
  border-radius: 4px;
}

.business-quick-select {
  width: 78px;
  flex: 0 0 78px;
}

.business-date-range {
  width: 288px;
  max-width: 288px;
  flex: 0 0 288px;
}

.accommodation-container :deep(.filter-section .el-select__wrapper),
.accommodation-container :deep(.filter-section .el-input__wrapper) {
  min-height: 32px;
  border-radius: 5px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.accommodation-container :deep(.filter-section .el-select__wrapper:hover),
.accommodation-container :deep(.filter-section .el-select__wrapper.is-focused),
.accommodation-container :deep(.filter-section .el-input__wrapper:hover),
.accommodation-container :deep(.filter-section .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #87bdf6 inset;
}

.accommodation-container :deep(.filter-section .el-input__inner),
.accommodation-container :deep(.filter-section .el-select__selected-item) {
  color: #30343b;
  font-size: 13px;
  font-weight: 500;
}

.accommodation-container :deep(.filter-section .el-input__prefix) {
  color: #aeb4bd;
}

.accommodation-container :deep(.filter-section .business-date-range.el-range-editor.el-input__wrapper) {
  width: 288px;
  max-width: 288px;
  flex: 0 0 288px;
  padding: 1px 1px 1px 6px;
  overflow: hidden;
}

.accommodation-container :deep(.filter-section .business-date-range .el-range__icon) {
  margin: 0 9px 0 2px;
  color: #aeb4bd;
  font-size: 14px;
}

.accommodation-container :deep(.filter-section .business-date-range .el-range-input) {
  height: 30px;
  padding: 0 7px;
  background: #fafafa;
  color: #30343b;
  font-size: 13px;
  font-weight: 500;
  line-height: 30px;
}

.accommodation-container :deep(.filter-section .business-date-range .el-range-input:first-child) {
  border-radius: 4px 0 0 4px;
}

.accommodation-container :deep(.filter-section .business-date-range .el-range-input:last-child) {
  border-radius: 0 4px 4px 0;
}

.accommodation-container :deep(.filter-section .business-date-range .el-range-separator) {
  width: 44px;
  height: 30px;
  background: #ffffff;
  color: #777f89;
  font-size: 12px;
  line-height: 30px;
}

.accommodation-container :deep(.filter-section .business-date-range .el-range__close-icon) {
  display: none;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.metric-card {
  display: flex;
  min-height: 82px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 12px 16px;
  background: #ffffff;
  border: none;
  border-radius: 4px;
}

.metric-copy {
  min-width: 0;
}

.metric-label {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  color: #8c8c8c;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.2;
}

.metric-value {
  color: #0b0b0b;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.15;
  white-space: nowrap;
}

.metric-icon-image {
  display: block;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  object-fit: contain;
}

.small-info {
  color: #9ca3ad;
  cursor: help;
  font-size: 13px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #0d0d0d;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.trend-section {
  min-height: 442px;
  margin-bottom: 10px;
  padding: 18px 22px 16px;
  background: #ffffff;
  border-radius: 4px;
}

.trend-tabs,
.table-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trend-tabs {
  margin: 12px 0 14px;
}

.trend-tabs :deep(.el-button),
.table-tabs :deep(.el-button) {
  min-width: 64px;
  height: 32px;
  margin: 0;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #606266;
  font-size: 13px;
  font-weight: 500;
  box-shadow: none;
}

.trend-tabs :deep(.el-button.active),
.trend-tabs :deep(.el-button:hover),
.table-tabs :deep(.el-button.active),
.table-tabs :deep(.el-button:hover) {
  border-color: #1e90f7;
  background: #1e90f7;
  color: #ffffff;
}

.trend-chart {
  width: 100%;
  height: 344px;
}

.table-section {
  min-height: 350px;
  padding: 18px 22px 28px;
  background: #ffffff;
  border-radius: 4px;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.table-title {
  min-width: 0;
}

.export-button {
  min-width: 138px;
  height: 32px;
  border: none;
  border-radius: 4px;
  background: #1e90f7;
  font-size: 13px;
  font-weight: 500;
}

.table-tabs {
  margin-bottom: 10px;
}

.detail-table {
  --el-table-border-color: #e6e6e6;
  --el-table-header-bg-color: #fafafa;
  --el-table-row-hover-bg-color: #f9fbff;
  border: 1px solid #e6e6e6;
  border-bottom: none;
  background: #ffffff;
}

.detail-table :deep(.el-table__inner-wrapper::before) {
  background-color: #e6e6e6;
}

.detail-table :deep(th.el-table__cell) {
  height: 34px;
  background: #fafafa !important;
  border-right: 1px solid #e6e6e6;
  border-bottom: 1px solid #e6e6e6;
  color: #333333;
  font-size: 13px;
  font-weight: 600;
}

.detail-table :deep(td.el-table__cell) {
  height: 52px;
  border-right: 1px solid #e6e6e6;
  border-bottom: 1px solid #e6e6e6;
  color: #333333;
  font-size: 13px;
  font-weight: 400;
}

.detail-table :deep(th.el-table__cell:last-child),
.detail-table :deep(td.el-table__cell:last-child) {
  border-right: none;
}

.detail-table :deep(.cell) {
  padding: 0 10px;
  line-height: 1.35;
}

.amount-bold {
  color: #333333;
  font-weight: 400;
}
</style>
