<template>
  <StatisticsLayout>
    <div class="business-summary-content">

    <!-- 日期选择器 -->
      <div class="date-selector">
        <el-date-picker
          v-model="startDate"
          type="date"
          :placeholder="t('stage5.common.date.startDate')"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
        />
        <span class="date-separator">{{ t('stage5.common.date.rangeTo') }}</span>
        <el-date-picker
          v-model="endDate"
          type="date"
          :placeholder="t('stage5.common.date.endDate')"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
        />
      </div>

      <!-- 营业概况标题 -->
      <div class="section-header">
        <h2>{{ t('stage5.statistics.business.title') }}</h2>
        <span class="stats-period">{{ t('stage5.statistics.common.statsPeriod', { period: formatDateRange }) }}</span>
        <span class="price-basis-note">{{ revenuePrecisionNotice }}</span>
      </div>

    <!-- 营业指标卡片 -->
      <div class="metrics-cards">
        <div class="metric-card primary">
          <div class="metric-content">
            <div class="metric-header">
              <span class="metric-title">{{ t('stage5.statistics.business.totalAccommodationRevenue') }}</span>
              <div class="metric-trend">
                <el-icon color="#ff4d4f"><ArrowUp /></el-icon>
              </div>
            </div>
            <div class="metric-value">¥{{ businessMetrics.totalRevenue }}</div>
            <div class="metric-change">
              <span class="change-label">{{ t('stage5.statistics.business.dailyComparison') }}</span>
              <span class="change-value positive">{{ businessMetrics.revenueChange }}%</span>
            </div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-content">
            <div class="metric-header">
              <div class="metric-icon house">
                <el-icon><House /></el-icon>
              </div>
              <span class="metric-title">{{ t('stage5.statistics.common.roomFee') }}</span>
            </div>
            <div class="metric-value">¥{{ businessMetrics.roomFee }}</div>
            <div class="metric-change">
              <span class="change-label">{{ t('stage5.statistics.common.ratio') }}</span>
              <span class="change-value">{{ businessMetrics.roomFeeRatio }}%</span>
            </div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-content">
            <div class="metric-header">
              <div class="metric-icon deposit">
                <el-icon><Coin /></el-icon>
              </div>
              <span class="metric-title">{{ t('stage5.statistics.common.deposit') }}</span>
            </div>
            <div class="metric-value">¥{{ businessMetrics.deposit }}</div>
            <div class="metric-change">
              <span class="change-label">{{ t('stage5.statistics.common.ratio') }}</span>
              <span class="change-value">{{ businessMetrics.depositRatio }}%</span>
            </div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-content">
            <div class="metric-header">
              <div class="metric-icon consumption">
                <el-icon><ShoppingCart /></el-icon>
              </div>
              <span class="metric-title">{{ t('stage5.statistics.common.roomConsumption') }}</span>
            </div>
            <div class="metric-value">¥{{ businessMetrics.roomConsumption }}</div>
            <div class="metric-change">
              <span class="change-label">{{ t('stage5.statistics.common.ratio') }}</span>
              <span class="change-value">{{ businessMetrics.roomConsumptionRatio }}%</span>
            </div>
          </div>
        </div>
      </div>

    <!-- 图表区域 -->
      <div class="charts-section">
        <!-- 营业汇总统计 -->
        <div class="chart-container">
          <h3>{{ t('stage5.statistics.business.summaryChart') }}</h3>
          <div class="chart-content">
            <div class="pie-chart" ref="pieChartRef"></div>
          </div>
        </div>

        <!-- 每日营业统计 -->
        <div class="chart-container">
          <h3>{{ t('stage5.statistics.common.dailyBusinessStats') }}</h3>
          <div class="chart-tabs">
            <el-tabs v-model="activeChartTab" @tab-change="updateLineChart">
              <el-tab-pane :label="t('stage5.statistics.business.totalAccommodationRevenue')" name="revenue"></el-tab-pane>
              <el-tab-pane :label="t('stage5.statistics.common.deposit')" name="deposit"></el-tab-pane>
              <el-tab-pane :label="t('stage5.statistics.common.roomConsumption')" name="consumption"></el-tab-pane>
              <el-tab-pane :label="t('stage5.statistics.common.roomFee')" name="roomfee"></el-tab-pane>
            </el-tabs>
          </div>
          <div class="chart-content">
            <div class="line-chart" ref="lineChartRef"></div>
          </div>
        </div>
      </div>

    <!-- 营业明细 -->
      <div class="business-details">
        <div class="details-header">
          <h3>{{ t('stage5.statistics.business.details') }}</h3>
          <span class="details-period">{{ t('stage5.statistics.common.detailsPeriod', { period: formatDateRange }) }}</span>
          <div class="details-actions">
            <el-button type="primary" @click="exportDetails">{{ t('stage5.common.actions.exportDetails') }}</el-button>
          </div>
        </div>

        <div class="details-table">
          <el-table
            :data="businessDetails"
            style="width: 100%"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
            show-summary
            :summary-method="getSummaries"
          >
            <el-table-column prop="category" :label="t('stage5.common.fields.project')" width="150">
              <template #default="scope">
                <span class="category-name">{{ scope.row.category }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="description" :label="t('stage5.common.fields.details')" min-width="200">
              <template #default="scope">
                <span class="detail-description">{{ scope.row.description }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="amount" :label="t('stage5.common.fields.total')" width="120" align="right">
              <template #default="scope">
                <span class="amount-value">¥{{ scope.row.amount }}</span>
              </template>
            </el-table-column>

            <!-- <el-table-column prop="date" label="09/24" width="120" align="right">
              <template #default="scope">
                <span class="date-amount">¥{{ scope.row.dateAmount }}</span>
              </template>
            </el-table-column> -->
          </el-table>
        </div>
      </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ArrowUp,
  House,
  Coin,
  ShoppingCart,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getBusinessSummary, type BusinessSummaryDTO } from '@/api/statistics'
import StatisticsLayout from './StatisticsLayout.vue'
import * as echarts from 'echarts'
import { formatYmdMonthDay, getRecentStoreDateRange } from '@/utils/storeDateTime'

const { t } = useI18n()

// 获取当前日期和一周前日期
const getCurrentWeekDates = () => {
  return getRecentStoreDateRange(7)
}

const weekDates = getCurrentWeekDates()

// 响应式数据
const startDate = ref(weekDates.start)
const endDate = ref(weekDates.end)
const activeChartTab = ref('revenue')

// ECharts实例
const pieChartRef = ref<HTMLElement>()
const lineChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

// 营业指标数据（基于现有订单数据计算）
const businessMetrics = ref({
  totalRevenue: '24.00',
  revenueChange: '0',
  roomFee: '24.00',
  roomFeeRatio: '100',
  deposit: '0.00',
  depositRatio: '0',
  roomConsumption: '0.00',
  roomConsumptionRatio: '0',
})

// 营业明细数据
const businessDetails = ref([
  {
    category: t('stage5.statistics.common.roomFee'),
    description: t('stage5.statistics.business.fullDayRoomFee'),
    amount: '24.00',
    dateAmount: '24.00',
  },
])

// 计算属性
const formatDateRange = computed(() => {
  if (startDate.value === endDate.value) {
    return startDate.value
  }
  return t('stage5.common.date.dateRange', { start: startDate.value, end: endDate.value })
})

const revenuePrecisionNotice = computed(() => {
  const precision = businessSummaryData.value?.revenuePrecision
  if (!precision) {
    return t('stage5.dataCenter.overview.priceBasisNotice')
  }
  return t('stage5.dataCenter.overview.priceBasisNoticeWithCoverage', {
    exact: precision.exactRoomNights || 0,
    averaged: precision.averagedRoomNights || 0,
  })
})

// 初始化饼图
const initPieChart = () => {
  if (!pieChartRef.value) return

  pieChart = echarts.init(pieChartRef.value)

  // 准备图表数据 - 按渠道分布
  const channelRevenue = businessSummaryData.value?.revenueByChannel || businessSummaryData.value?.topChannels || []
  const pieData = channelRevenue.map((channel, index) => ({
    value: Number(channel.revenue),
    name: channel.channelName,
    itemStyle: {
      color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272'][index % 6]
    }
  })) || []

  const option = {
    title: {
      text: t('stage5.statistics.business.totalAccommodationRevenue'),
      subtext: `¥ ${businessMetrics.value.totalRevenue}`,
      left: 'center',
      top: 'center',
      textStyle: {
        fontSize: 14,
        color: '#666',
        fontWeight: 'normal'
      },
      subtextStyle: {
        fontSize: 20,
        color: '#333',
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: '0%',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: {
        fontSize: 12
      }
    },
    series: [
      {
        name: t('stage5.statistics.common.channelRevenue'),
        type: 'pie',
        radius: ['50%', '80%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: pieData.length > 0 ? pieData : [
          {
            value: 0,
            name: t('stage5.common.empty.noData'),
            itemStyle: { color: '#e0e0e0' }
          }
        ]
      }
    ]
  }

  pieChart.setOption(option)
}

// 初始化折线图
const initLineChart = () => {
  if (!lineChartRef.value) return

  lineChart = echarts.init(lineChartRef.value)
  updateLineChart()
}

// 更新折线图数据
const updateLineChart = () => {
  if (!lineChart) return

  // 从API获取每日收入数据
  const dailyRevenues = businessSummaryData.value?.revenueByDate || []

  // 格式化日期和数据
  const dates = dailyRevenues.map(item => {
    const { month, day } = formatYmdMonthDay(item.date)
    return t('stage5.common.date.monthDay', {
      month: Number(month),
      day: Number(day),
    })
  })

  let data: number[] = []
  let seriesName = ''

  switch (activeChartTab.value) {
    case 'revenue':
      seriesName = t('stage5.statistics.business.totalAccommodationRevenue')
      data = dailyRevenues.map(item => Number(item.revenue))
      break
    case 'deposit':
      seriesName = t('stage5.statistics.common.deposit')
      data = dailyRevenues.map(() => 0) // 暂无押金数据
      break
    case 'consumption':
      seriesName = t('stage5.statistics.common.roomConsumption')
      data = dailyRevenues.map(() => 0) // 暂无消费数据
      break
    case 'roomfee':
      seriesName = t('stage5.statistics.common.roomFee')
      data = dailyRevenues.map(item => Number(item.revenue)) // 所有收入作为房费
      break
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const param = params[0]
        return `${param.axisValue}<br/>${param.seriesName}: ¥${param.value.toFixed(2)}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates.length > 0 ? dates : [t('stage5.common.empty.noData')],
      axisLabel: {
        fontSize: 12,
        color: '#666'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        fontSize: 12,
        color: '#666',
        formatter: '¥{value}'
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    series: [
      {
        name: seriesName,
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: {
          color: '#5470c6'
        },
        lineStyle: {
          color: '#5470c6',
          width: 2
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(84, 112, 198, 0.3)' },
              { offset: 1, color: 'rgba(84, 112, 198, 0.05)' }
            ]
          }
        },
        data: data.length > 0 ? data : [0]
      }
    ]
  }

  lineChart.setOption(option)
}

// 存储API返回的完整数据
const businessSummaryData = ref<BusinessSummaryDTO | null>(null)

// 基于API获取营业汇总数据
const calculateBusinessMetrics = async () => {
  try {
    if (!startDate.value || !endDate.value) {
      return
    }

    // 调用营业汇总API
    const response = await getBusinessSummary({
      startDate: startDate.value,
      endDate: endDate.value
    })

    if (!response.success || !response.data) {
      // 如果没有数据，显示空状态
      businessMetrics.value = {
        totalRevenue: '0.00',
        revenueChange: '0',
        roomFee: '0.00',
        roomFeeRatio: '100',
        deposit: '0.00',
        depositRatio: '0',
        roomConsumption: '0.00',
        roomConsumptionRatio: '0',
      }
      businessDetails.value = []
      businessSummaryData.value = null
      return
    }

    const data = response.data
    businessSummaryData.value = data

    // 映射API数据到页面显示格式
    const totalRevenue = Number(data.totalRevenue)
    const roomFee = totalRevenue
    const deposit = 0 // 暂无押金数据
    const consumption = 0 // 暂无消费数据

    businessMetrics.value = {
      totalRevenue: totalRevenue.toFixed(2),
      revenueChange: '0', // 需要历史数据来计算环比
      roomFee: roomFee.toFixed(2),
      roomFeeRatio: '100',
      deposit: deposit.toFixed(2),
      depositRatio: '0',
      roomConsumption: consumption.toFixed(2),
      roomConsumptionRatio: '0',
    }

    // 更新营业明细数据 - 按渠道分组
    const channelRevenue = data.revenueByChannel || data.topChannels || []
    const details = channelRevenue.map(channel => ({
      category: channel.channelName,
      description: t('stage5.statistics.business.orderRoomNights', {
        orders: channel.orderCount,
        roomNights: channel.roomNights,
      }),
      amount: Number(channel.revenue).toFixed(2),
      dateAmount: Number(channel.revenue).toFixed(2),
    }))

    businessDetails.value = details.length > 0
      ? details
      : [
          {
            category: t('stage5.common.empty.noData'),
            description: t('stage5.statistics.business.noBusinessData'),
            amount: '0.00',
            dateAmount: '0.00',
          },
        ]
  } catch (error) {
    console.error(t('stage5.statistics.business.fetchFailed'), error)
    ElMessage.error(t('stage5.statistics.business.fetchFailed'))

    // 显示错误状态的默认数据
    businessMetrics.value = {
      totalRevenue: '0.00',
      revenueChange: '0',
      roomFee: '0.00',
      roomFeeRatio: '100',
      deposit: '0.00',
      depositRatio: '0',
      roomConsumption: '0.00',
      roomConsumptionRatio: '0',
    }
    businessDetails.value = [
      {
        category: t('stage5.common.messages.dataLoadFailed'),
        description: t('stage5.common.messages.dataLoadFailed'),
        amount: '0.00',
        dateAmount: '0.00',
      },
    ]
    businessSummaryData.value = null
  }
}

// 表格汇总方法
const getSummaries = (param: any) => {
  const { columns, data } = param
  const sums: string[] = []
  columns.forEach((column: any, index: number) => {
    if (index === 0) {
      sums[index] = t('stage5.common.fields.total')
      return
    }
    if (index === 1) {
      sums[index] = ''
      return
    }

    if (column.property === 'amount' || column.property === 'dateAmount') {
      const values = data.map((item: any) => Number(item[column.property]))
      if (!values.every((value: any) => Number.isNaN(value))) {
        const total = values.reduce((prev: number, curr: number) => {
          const value = Number(curr)
          if (!Number.isNaN(value)) {
            return prev + curr
          } else {
            return prev
          }
        }, 0)
        sums[index] = `¥${total.toFixed(2)}`
      } else {
        sums[index] = ''
      }
    } else {
      sums[index] = ''
    }
  })

  return sums
}

// 导出明细
const exportDetails = () => {
  ElMessage.success(t('stage5.statistics.business.exportSuccess'))
  // 这里可以添加实际的导出逻辑
}

// 监听日期变化，重新计算数据
watch(
  [startDate, endDate],
  () => {
    calculateBusinessMetrics()
  },
  { immediate: false },
)

onMounted(() => {
  calculateBusinessMetrics()
  nextTick(() => {
    initPieChart()
    initLineChart()
  })
})

// 监听营业指标变化，更新图表
watch(
  businessMetrics,
  () => {
    nextTick(() => {
      if (pieChart) {
        initPieChart() // 重新初始化饼图以更新数据
      }
      if (lineChart) {
        updateLineChart()
      }
    })
  },
  { deep: true }
)

// 组件卸载时销毁图表
onBeforeUnmount(() => {
  if (pieChart) {
    pieChart.dispose()
  }
  if (lineChart) {
    lineChart.dispose()
  }
})
</script>

<style scoped>
.business-summary-content {
  width: 100%;
}

/* 日期选择器 */
.date-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.date-separator {
  font-size: 14px;
  color: #666;
}

/* 页面标题 */
.section-header {
  display: flex;
  align-items: baseline;
  gap: 16px;
  margin-bottom: 24px;
}

.section-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: #333;
}

.stats-period {
  font-size: 14px;
  color: #666;
}

.price-basis-note {
  max-width: 520px;
  padding: 6px 10px;
  border-left: 3px solid #1e90f7;
  border-radius: 4px;
  background: #f5f9ff;
  color: #42526a;
  font-size: 13px;
  line-height: 1.45;
}

/* 营业指标卡片 */
.metrics-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.metric-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.metric-card.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.metric-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.metric-title {
  font-size: 14px;
  color: inherit;
  opacity: 0.8;
}

.metric-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.metric-icon.house {
  background: linear-gradient(135deg, #4285f4, #34a853);
}

.metric-icon.deposit {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
}

.metric-icon.consumption {
  background: linear-gradient(135deg, #ff9500, #ffb84d);
}

.metric-trend {
  opacity: 0.8;
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  color: inherit;
}

.metric-card:not(.primary) .metric-value {
  color: #333;
}

.metric-change {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.change-label {
  color: inherit;
  opacity: 0.6;
}

.change-value {
  font-weight: 500;
}

.change-value.positive {
  color: #52c41a;
}

.metric-card.primary .change-value {
  color: rgba(255, 255, 255, 0.9);
}

/* 图表区域 */
.charts-section {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 24px;
}

.chart-container {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chart-container h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.chart-tabs {
  margin-bottom: 20px;
}

.chart-content {
  min-height: 300px;
}

/* ECharts图表样式 */
.pie-chart, .line-chart {
  width: 100%;
  height: 300px;
}

/* 营业明细 */
.business-details {
  margin-top: 32px;
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.details-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.details-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.details-period {
  font-size: 14px;
  color: #666;
}

.details-actions {
  margin-left: auto;
}

.details-table {
  margin-top: 16px;
}

.category-name {
  font-weight: 500;
  color: #333;
}

.detail-description {
  color: #666;
}

.amount-value,
.date-amount {
  font-weight: 500;
  color: #333;
}

/* 表格汇总行样式 */
.details-table :deep(.el-table__footer-wrapper) {
  .cell {
    font-weight: 500;
    color: #333;
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .business-summary {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    height: auto;
  }

  .main-content {
    padding: 16px;
  }

  .metrics-cards {
    grid-template-columns: 1fr;
  }

  .date-selector {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
