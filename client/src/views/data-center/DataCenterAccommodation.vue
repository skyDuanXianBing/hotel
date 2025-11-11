<template>
  <StatisticsLayout>
    <div class="accommodation-container">
    <!-- 日期筛选 -->
    <div class="filter-section">
      <el-select v-model="dateType" style="width: 100px">
        <el-option label="今天" value="today" />
        <el-option label="昨天" value="yesterday" />
        <el-option label="本周" value="week" />
        <el-option label="本月" value="month" />
      </el-select>
      <el-date-picker
        v-model="startDate"
        type="date"
        placeholder="选择日期"
        format="YYYY/MM/DD"
        value-format="YYYY/MM/DD"
      />
      <span class="date-separator">至</span>
      <el-date-picker
        v-model="endDate"
        type="date"
        placeholder="选择日期"
        format="YYYY/MM/DD"
        value-format="YYYY/MM/DD"
      />
    </div>

    <!-- 经营指标统计 -->
    <div class="metrics-section">
      <h3 class="section-title">
        经营指标
        <el-tooltip content="统计说明" placement="right">
          <el-icon class="info-icon"><QuestionFilled /></el-icon>
        </el-tooltip>
      </h3>

      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-icon">
            <el-icon size="32" color="#5b8ff9"><House /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">总房费</div>
            <div class="metric-value">¥{{ metrics.totalRoomFee.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon">
            <el-icon size="32" color="#667eea"><TrendCharts /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">
              平均房价
              <el-tooltip content="ADR" placement="top">
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
              入住率
              <el-tooltip content="Occ" placement="top">
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
              平均每房收益
              <el-tooltip content="RevPAR" placement="top">
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
              累计出售间夜数
              <el-tooltip content="间夜数" placement="top">
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
              平均每日间夜数
              <el-tooltip content="平均每日间夜数" placement="top">
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
      <h3 class="section-title">经营指标趋势</h3>

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
        数据明细 ({{ dateRangeLabel }})
        <el-button type="primary" style="float: right">导出明细</el-button>
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
        <el-table-column prop="roomType" label="房型" min-width="150" align="center" />
        <el-table-column prop="roomNumber" label="房间" min-width="120" align="center" />
        <el-table-column prop="total" label="合计" min-width="150" align="center">
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
        <el-table-column prop="roomType" label="房型" min-width="150" align="center" />
        <el-table-column prop="roomNumber" label="房间" min-width="120" align="center" />
        <el-table-column prop="total" label="合计" min-width="150" align="center">
          <template #default="{ row }">
            <span class="amount-bold">{{ row.total }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentDate" :label="currentDateLabel" min-width="150" align="center" />
      </el-table>

      <!-- 入住率明细表格 -->
      <el-table v-if="activeTableTab === 'occupancy'" :data="occupancyData" border stripe class="detail-table">
        <el-table-column prop="roomType" label="房型" min-width="200" align="center" />
        <el-table-column prop="total" label="合计" min-width="150" align="center">
          <template #default="{ row }">
            <span class="amount-bold">{{ row.total }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentDate" :label="currentDateLabel" min-width="150" align="center" />
      </el-table>

      <!-- RevPAR明细表格 -->
      <el-table v-if="activeTableTab === 'revpar'" :data="revparData" border stripe class="detail-table">
        <el-table-column prop="roomType" label="房型" min-width="200" align="center" />
        <el-table-column prop="total" label="合计" min-width="150" align="center">
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { QuestionFilled, House, TrendCharts, SuccessFilled, Money, Calendar, Clock } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import StatisticsLayout from '../statistics/StatisticsLayout.vue'

const dateType = ref('today')

// 自动获取今天的日期
const getTodayDate = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${year}/${month}/${day}`
}

const startDate = ref(getTodayDate())
const endDate = ref(getTodayDate())

// 日期范围标签 - 动态显示选择的日期范围
const dateRangeLabel = computed(() => {
  return `${startDate.value} 至 ${endDate.value}`
})

// 经营指标数据
const metrics = ref({
  totalRoomFee: 154256.45,
  avgDailyRate: 12854.70,
  occupancyRate: 100.00,
  revPAR: 12854.70,
  totalRoomNights: 12.00,
  avgDailyRoomNights: 12.00,
})

// 趋势图选项卡
const trendTabs = [
  { key: 'room-fee', label: '总房费' },
  { key: 'avg-price', label: '平均房价' },
  { key: 'avg-revenue', label: '平均每房收益' },
  { key: 'occupancy', label: '间夜数' },
]
const activeTrendTab = ref('room-fee')

// 日期数据
const dates = ['11月2日', '11月3日', '11月4日', '11月5日', '11月6日', '11月7日', '11月8日']

// 趋势数据
const trendData: Record<string, number[]> = {
  'room-fee': [18000, 19500, 21000, 22000, 20500, 18500, 17500],
  'avg-price': [380, 390, 420, 440, 410, 370, 350],
  'avg-revenue': [320, 330, 350, 370, 345, 310, 295],
  'occupancy': [47, 50, 50, 50, 50, 50, 50]
}

// 表格选项卡
const tableTabs = [
  { key: 'room-fee', label: '房费明细' },
  { key: 'checkin', label: '间夜明细' },
  { key: 'occupancy', label: '入住率明细' },
  { key: 'revpar', label: 'RevPAR明细' },
]
const activeTableTab = ref('room-fee')

// 当前日期标签 - 动态获取今天的日期
const currentDateLabel = (() => {
  const today = new Date()
  const month = today.getMonth() + 1
  const day = today.getDate()
  return `${month}月${day}日`
})()

// 房费明细数据
interface RoomFeeItem {
  roomType: string
  roomNumber: string
  total: number
  currentDate: number
}

const roomFeeData = ref<RoomFeeItem[]>([
  { roomType: 'Tanpopo Inn204', roomNumber: '204', total: 8672.00, currentDate: 8672.00 },
  { roomType: 'Tanpopo Inn204', roomNumber: '未排房', total: 0.00, currentDate: 0.00 },
  { roomType: '', roomNumber: '合计', total: 8672.00, currentDate: 8672.00 },
  { roomType: 'Tanpopo Inn104', roomNumber: '104', total: 6916.00, currentDate: 6916.00 },
  { roomType: 'Tanpopo Inn104', roomNumber: '未排房', total: 0.00, currentDate: 0.00 },
  { roomType: '', roomNumber: '合计', total: 6916.00, currentDate: 6916.00 },
  { roomType: '美途ホテル　東十条1F', roomNumber: '101', total: 19117.00, currentDate: 19117.00 },
  { roomType: '美途ホテル　東十条1F', roomNumber: '未排房', total: 0.00, currentDate: 0.00 },
  { roomType: '', roomNumber: '合计', total: 19117.00, currentDate: 19117.00 },
  { roomType: '美途ホテル　池袋403', roomNumber: '403', total: 9063.35, currentDate: 9063.35 },
])

// 间夜明细数据
interface CheckinItem {
  roomType: string
  roomNumber: string
  total: number
  currentDate: number
}

const checkinData = ref<CheckinItem[]>([
  { roomType: 'Tanpopo Inn204', roomNumber: '204', total: 1, currentDate: 1 },
  { roomType: 'Tanpopo Inn204', roomNumber: '未排房', total: 0, currentDate: 0 },
  { roomType: '', roomNumber: '合计', total: 1, currentDate: 1 },
  { roomType: 'Tanpopo Inn104', roomNumber: '104', total: 1, currentDate: 1 },
  { roomType: 'Tanpopo Inn104', roomNumber: '未排房', total: 0, currentDate: 0 },
  { roomType: '', roomNumber: '合计', total: 1, currentDate: 1 },
  { roomType: '美途ホテル　東十条1F', roomNumber: '101', total: 1, currentDate: 1 },
  { roomType: '美途ホテル　東十条1F', roomNumber: '未排房', total: 0, currentDate: 0 },
  { roomType: '', roomNumber: '合计', total: 1, currentDate: 1 },
  { roomType: '美途ホテル　池袋403', roomNumber: '403', total: 1, currentDate: 1 },
])

// 入住率明细数据
interface OccupancyItem {
  roomType: string
  total: string
  currentDate: string
}

const occupancyData = ref<OccupancyItem[]>([
  { roomType: 'Tanpopo Inn204', total: '100%', currentDate: '100%' },
  { roomType: 'Tanpopo Inn104', total: '100%', currentDate: '100%' },
  { roomType: '美途ホテル　東十条1F', total: '100%', currentDate: '100%' },
  { roomType: '美途ホテル　池袋403', total: '100%', currentDate: '100%' },
  { roomType: 'Tanpopo Inn103', total: '100%', currentDate: '100%' },
  { roomType: '美途ホテル　池袋201', total: '100%', currentDate: '100%' },
  { roomType: 'Tanpopo Inn303', total: '100%', currentDate: '100%' },
  { roomType: '美途ホテル　東十条3/4F', total: '100%', currentDate: '100%' },
  { roomType: 'Tanpopo Inn301', total: '100%', currentDate: '100%' },
  { roomType: '美途ホテル　東十条2F', total: '100%', currentDate: '100%' },
  { roomType: 'Tanpopo Inn304', total: '100%', currentDate: '100%' },
  { roomType: '美途ホテル　池袋401', total: '100%', currentDate: '100%' },
])

// RevPAR明细数据
interface RevPARItem {
  roomType: string
  total: number
  currentDate: number
}

const revparData = ref<RevPARItem[]>([
  { roomType: 'Tanpopo Inn204', total: 8672.00, currentDate: 8672.00 },
  { roomType: 'Tanpopo Inn104', total: 6916.00, currentDate: 6916.00 },
  { roomType: '美途ホテル　東十条1F', total: 19117.00, currentDate: 19117.00 },
  { roomType: '美途ホテル　池袋403', total: 9063.35, currentDate: 9063.35 },
  { roomType: 'Tanpopo Inn103', total: 8261.00, currentDate: 8261.00 },
  { roomType: '美途ホテル　池袋201', total: 20944.00, currentDate: 20944.00 },
  { roomType: 'Tanpopo Inn303', total: 9833.00, currentDate: 9833.00 },
  { roomType: '美途ホテル　東十条3/4F', total: 19124.00, currentDate: 19124.00 },
  { roomType: 'Tanpopo Inn301', total: 4239.00, currentDate: 4239.00 },
])

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

  const data = trendData[tabKey]
  const tabLabel = trendTabs.find(t => t.key === tabKey)?.label || ''

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
      data: dates,
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

onMounted(() => {
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
