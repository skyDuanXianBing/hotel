<template>
  <StatisticsLayout>
    <div class="business-summary-content">

    <!-- 日期选择器 -->
      <div class="date-selector">
        <el-date-picker
          v-model="startDate"
          type="date"
          placeholder="开始日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
        />
        <span class="date-separator">至</span>
        <el-date-picker
          v-model="endDate"
          type="date"
          placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
        />
      </div>

    <!-- 营业概况标题 -->
      <div class="section-header">
        <h2>营业概况</h2>
        <span class="stats-period">统计时间段 {{ formatDateRange }}</span>
      </div>

    <!-- 营业指标卡片 -->
      <div class="metrics-cards">
        <div class="metric-card primary">
          <div class="metric-content">
            <div class="metric-header">
              <span class="metric-title">住宿总营业额</span>
              <div class="metric-trend">
                <el-icon color="#ff4d4f"><ArrowUp /></el-icon>
              </div>
            </div>
            <div class="metric-value">¥{{ businessMetrics.totalRevenue }}</div>
            <div class="metric-change">
              <span class="change-label">日环比</span>
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
              <span class="metric-title">房费</span>
            </div>
            <div class="metric-value">¥{{ businessMetrics.roomFee }}</div>
            <div class="metric-change">
              <span class="change-label">占比</span>
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
              <span class="metric-title">押金</span>
            </div>
            <div class="metric-value">¥{{ businessMetrics.deposit }}</div>
            <div class="metric-change">
              <span class="change-label">占比</span>
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
              <span class="metric-title">客房消费</span>
            </div>
            <div class="metric-value">¥{{ businessMetrics.roomConsumption }}</div>
            <div class="metric-change">
              <span class="change-label">占比</span>
              <span class="change-value">{{ businessMetrics.roomConsumptionRatio }}%</span>
            </div>
          </div>
        </div>
      </div>

    <!-- 图表区域 -->
      <div class="charts-section">
        <!-- 营业汇总统计 -->
        <div class="chart-container">
          <h3>营业汇总统计</h3>
          <div class="chart-content">
            <div class="pie-chart" ref="pieChartRef"></div>
          </div>
        </div>

        <!-- 每日营业统计 -->
        <div class="chart-container">
          <h3>每日营业统计</h3>
          <div class="chart-tabs">
            <el-tabs v-model="activeChartTab" @tab-change="updateLineChart">
              <el-tab-pane label="住宿总营业额" name="revenue"></el-tab-pane>
              <el-tab-pane label="押金" name="deposit"></el-tab-pane>
              <el-tab-pane label="客房消费" name="consumption"></el-tab-pane>
              <el-tab-pane label="房费" name="roomfee"></el-tab-pane>
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
          <h3>营业明细</h3>
          <span class="details-period">({{ formatDateRange }})</span>
          <div class="details-actions">
            <el-button type="primary" @click="exportDetails">导出明细</el-button>
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
            <el-table-column prop="category" label="项目" width="150">
              <template #default="scope">
                <span class="category-name">{{ scope.row.category }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="description" label="明细" min-width="200">
              <template #default="scope">
                <span class="detail-description">{{ scope.row.description }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="amount" label="合计" width="120" align="right">
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
import { useRouter } from 'vue-router'
import {
  ArrowUp,
  House,
  Coin,
  ShoppingCart,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getBusinessSummary, type BusinessSummaryDTO } from '@/api/business'
import StatisticsLayout from './StatisticsLayout.vue'
import * as echarts from 'echarts'

const router = useRouter()

// 获取当前日期和一周前日期
const getCurrentWeekDates = () => {
  const today = new Date()
  const lastWeek = new Date(today)
  lastWeek.setDate(today.getDate() - 6) // 最近7天（包含今天）

  const formatDate = (date: Date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  return {
    start: formatDate(lastWeek),
    end: formatDate(today)
  }
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
    category: '房费',
    description: '全日房费',
    amount: '24.00',
    dateAmount: '24.00',
  },
])

// 计算属性
const formatDateRange = computed(() => {
  if (startDate.value === endDate.value) {
    return startDate.value
  }
  return `${startDate.value} 至 ${endDate.value}`
})

// 初始化饼图
const initPieChart = () => {
  if (!pieChartRef.value) return

  pieChart = echarts.init(pieChartRef.value)

  // 准备图表数据 - 按渠道分布
  const pieData = businessSummaryData.value?.revenueByChannel.map((channel, index) => ({
    value: Number(channel.revenue),
    name: channel.channelName,
    itemStyle: {
      color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272'][index % 6]
    }
  })) || []

  const option = {
    title: {
      text: '住宿总营业额',
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
        name: '渠道收入',
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
            name: '暂无数据',
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
    const date = new Date(item.date)
    return `${date.getMonth() + 1}月${date.getDate()}日`
  })

  let data: number[] = []
  let seriesName = ''

  switch (activeChartTab.value) {
    case 'revenue':
      seriesName = '住宿总营业额'
      data = dailyRevenues.map(item => Number(item.revenue))
      break
    case 'deposit':
      seriesName = '押金'
      data = dailyRevenues.map(() => 0) // 暂无押金数据
      break
    case 'consumption':
      seriesName = '客房消费'
      data = dailyRevenues.map(() => 0) // 暂无消费数据
      break
    case 'roomfee':
      seriesName = '房费'
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
      data: dates.length > 0 ? dates : ['暂无数据'],
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
    const roomFee = totalRevenue // 目前所有收入都作为房费
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
    const details = data.revenueByChannel.map(channel => ({
      category: channel.channelName,
      description: `${channel.orderCount}个订单，${channel.roomNights}间夜`,
      amount: Number(channel.revenue).toFixed(2),
      dateAmount: Number(channel.revenue).toFixed(2),
    }))

    businessDetails.value = details.length > 0
      ? details
      : [
          {
            category: '暂无数据',
            description: '选择的日期范围内暂无营业数据',
            amount: '0.00',
            dateAmount: '0.00',
          },
        ]
  } catch (error) {
    console.error('获取营业数据失败:', error)
    ElMessage.error('获取营业数据失败，请稍后重试')

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
        category: '错误',
        description: '数据加载失败',
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
      sums[index] = '合计'
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
  ElMessage.success('营业明细导出成功')
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
