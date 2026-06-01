<template>
  <StatisticsLayout>
    <div class="revenue-summary-content">
      <!-- 日期选择器 -->
      <div class="date-selector">
        <el-select v-model="dateRange" class="date-quick-select" :placeholder="t('stage5.common.date.custom')">
          <el-option :label="t('stage5.common.date.today')" value="today" />
          <el-option :label="t('stage5.common.date.yesterday')" value="yesterday" />
          <el-option :label="t('stage5.common.date.thisWeek')" value="this-week" />
          <el-option :label="t('stage5.common.date.thisMonth')" value="this-month" />
          <el-option :label="t('stage5.common.date.custom')" value="custom" />
        </el-select>
        <el-date-picker
          v-model="startDate"
          type="date"
          :placeholder="t('stage5.common.date.startDate')"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          class="date-picker"
        />
        <span class="date-separator">{{ t('stage5.common.date.rangeTo') }}</span>
        <el-date-picker
          v-model="endDate"
          type="date"
          :placeholder="t('stage5.common.date.endDate')"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          class="date-picker"
        />
      </div>

      <!-- 流水概况 -->
      <div class="overview-section">
        <div class="section-header">
          <h2>{{ t('stage5.statistics.revenue.overview') }}</h2>
          <span class="section-info">{{ t('stage5.statistics.common.statsInfo') }} <el-icon><QuestionFilled /></el-icon></span>
        </div>

        <!-- Tab 切换 -->
        <el-tabs v-model="activeTab" class="overview-tabs">
          <el-tab-pane :label="t('stage5.statistics.revenue.paymentMethod')" name="payment"></el-tab-pane>
          <el-tab-pane :label="t('stage5.statistics.revenue.category')" name="category"></el-tab-pane>
        </el-tabs>

        <!-- 流水卡片 -->
        <div class="revenue-cards">
          <!-- 总流水卡片 -->
          <div class="total-card">
            <div class="card-title">{{ t('stage5.statistics.revenue.totalRevenue') }}</div>
            <div class="card-amount">¥ {{ totalRevenue.toFixed(2) }}</div>
            <div class="card-details">
              <span>{{ t('stage5.statistics.revenue.totalIncome') }} ¥ {{ totalIncome.toFixed(2) }}</span>
              <span>{{ t('stage5.statistics.revenue.totalExpense') }} ¥ {{ totalExpense.toFixed(2) }}</span>
            </div>
          </div>

          <!-- 支付方式卡片列表 -->
          <div
            v-for="payment in paymentMethods"
            :key="payment.name"
            class="payment-card"
          >
            <div class="card-title">{{ payment.name }}</div>
            <div class="card-amount">¥ {{ payment.amount.toFixed(2) }}</div>
          </div>
        </div>
      </div>

      <!-- 收支分布图表 -->
      <div class="distribution-section">
        <div class="chart-row">
          <!-- 总收款分布 -->
          <div class="chart-container">
            <div class="chart-title">{{ t('stage5.statistics.revenue.incomeDistribution') }}</div>
            <div ref="incomeChartRef" class="pie-chart"></div>
          </div>

          <!-- 总支出分布 -->
          <div class="chart-container">
            <div class="chart-title">{{ t('stage5.statistics.revenue.expenseDistribution') }}</div>
            <div ref="expenseChartRef" class="pie-chart"></div>
          </div>
        </div>
      </div>

      <!-- 流水明细 -->
      <div class="details-section">
        <div class="details-header">
          <h3>{{ t('stage5.statistics.revenue.details') }} {{ t('stage5.statistics.common.detailsPeriod', { period: formatDateRange }) }}</h3>
          <el-button type="primary" @click="handleExport">{{ t('stage5.common.actions.exportDetails') }}</el-button>
        </div>

        <div class="details-table">
          <el-table
            :data="revenueDetails"
            style="width: 100%"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
            show-summary
            :summary-method="getSummaries"
            border
          >
            <el-table-column prop="paymentMethod" :label="t('stage5.statistics.revenue.paymentMethod')" width="120" fixed>
              <template #default="{ row }">
                <span class="payment-name">{{ row.paymentMethod }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="total" :label="t('stage5.common.fields.total')" width="120" align="right" fixed>
              <template #default="{ row }">
                <span class="amount-value">¥{{ row.total }}</span>
              </template>
            </el-table-column>

            <el-table-column
              v-for="col in dateColumns"
              :key="col.prop"
              :prop="col.prop"
              :label="col.label"
              width="120"
              align="right"
            >
              <template #default="{ row }">
                <span class="date-amount">¥{{ row[col.prop] }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import StatisticsLayout from './StatisticsLayout.vue'
import { addDaysToYmd, getRecentStoreDateRange } from '@/utils/storeDateTime'

const { t } = useI18n()

const getDefaultDateRange = () => {
  return getRecentStoreDateRange(7)
}

const getDateColumnProp = (ymd: string) => {
  const [year, month, day] = ymd.split('-')
  return `date_${year}${month}${day}`
}

// 日期范围
const defaultDateRange = getDefaultDateRange()
const dateRange = ref('custom')
const startDate = ref(defaultDateRange.start)
const endDate = ref(defaultDateRange.end)

// Tab 切换
const activeTab = ref('payment')

// 总收入、总支出、总流水
const totalIncome = ref(12.0)
const totalExpense = ref(0.0)
const totalRevenue = computed(() => totalIncome.value - totalExpense.value)

// 支付方式数据
const paymentMethods = ref([
  { name: t('stage5.statistics.notes.wechat'), amount: 12.0 },
])

// 收款分布数据
const incomeDistribution = ref([
  { name: t('stage5.statistics.notes.wechat'), value: 12.0 },
])

// 支出分布数据
const expenseDistribution = ref([
  { name: t('stage5.statistics.notes.wechat'), value: 0.0 },
])

// 流水明细数据
const revenueDetails = computed(() => {
  const row: Record<string, string> = {
    paymentMethod: t('stage5.statistics.notes.wechat'),
    total: '12.00',
  }

  dateColumns.value.forEach((column, index) => {
    row[column.prop] = index === dateColumns.value.length - 1 ? '12.00' : '0.00'
  })

  return [row]
})

// 图表实例
const incomeChartRef = ref<HTMLElement>()
const expenseChartRef = ref<HTMLElement>()
let incomeChart: echarts.ECharts | null = null
let expenseChart: echarts.ECharts | null = null

// 动态生成日期列
const dateColumns = computed(() => {
  const columns = []
  let currentDate = startDate.value

  while (currentDate <= endDate.value) {
    const [, monthText, dayText] = currentDate.split('-')
    const month = String(Number(monthText))
    const day = String(Number(dayText))
    columns.push({
      prop: getDateColumnProp(currentDate),
      label: t('stage5.common.date.monthDay', { month, day }),
    })
    currentDate = addDaysToYmd(currentDate, 1)
  }

  return columns
})

// 日期范围格式化
const formatDateRange = computed(() => {
  if (startDate.value === endDate.value) {
    return startDate.value
  }
  return t('stage5.common.date.dateRange', { start: startDate.value, end: endDate.value })
})

// 创建环形图配置
const createPieChartOption = (data: any[], title: string, total: number): EChartsOption => {
  return {
    title: [
      {
        text: title,
        left: '30%',
        top: '43%',
        textAlign: 'center',
        textStyle: {
          fontSize: 14,
          color: '#666',
          fontWeight: 'normal',
        },
      },
      {
        text: `¥ ${total.toFixed(2)}`,
        left: '30%',
        top: '50%',
        textAlign: 'center',
        textStyle: {
          fontSize: 20,
          color: '#333',
          fontWeight: 'bold',
        },
      },
    ],
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: '15%',
      top: 'center',
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 16,
      textStyle: {
        fontSize: 13,
        color: '#333',
        rich: {
          name: {
            width: 60,
          },
          percent: {
            width: 50,
            align: 'right',
          },
        },
      },
      formatter: (name: string) => {
        const item = data.find((d) => d.name === name)
        if (item && total > 0) {
          const percent = ((item.value / total) * 100).toFixed(0)
          return `{name|${name}}{percent|${percent}%}`
        }
        return `{name|${name}}{percent|0%}`
      },
    },
    series: [
      {
        type: 'pie',
        radius: ['50%', '70%'],
        center: ['30%', '50%'],
        avoidLabelOverlap: false,
        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: false,
          },
          scale: true,
          scaleSize: 5,
        },
        labelLine: {
          show: false,
        },
        data: data.filter((item) => item.value > 0),
        color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272'],
      },
    ],
  }
}

// 初始化收款分布图表
const initIncomeChart = () => {
  if (!incomeChartRef.value) return

  incomeChart = echarts.init(incomeChartRef.value)
  const total = incomeDistribution.value.reduce((sum, item) => sum + item.value, 0)
  const option = createPieChartOption(incomeDistribution.value, t('stage5.statistics.revenue.totalIncome'), total)
  incomeChart.setOption(option)
}

// 初始化支出分布图表
const initExpenseChart = () => {
  if (!expenseChartRef.value) return

  expenseChart = echarts.init(expenseChartRef.value)
  const total = expenseDistribution.value.reduce((sum, item) => sum + item.value, 0)
  const option = createPieChartOption(expenseDistribution.value, t('stage5.statistics.revenue.totalExpense'), total)
  expenseChart.setOption(option)
}

// 更新图表
const updateCharts = () => {
  if (incomeChart) {
    const total = incomeDistribution.value.reduce((sum, item) => sum + item.value, 0)
    incomeChart.setOption(createPieChartOption(incomeDistribution.value, t('stage5.statistics.revenue.totalIncome'), total))
  }
  if (expenseChart) {
    const total = expenseDistribution.value.reduce((sum, item) => sum + item.value, 0)
    expenseChart.setOption(createPieChartOption(expenseDistribution.value, t('stage5.statistics.revenue.totalExpense'), total))
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

    const prop = column.property
    if (prop === 'total' || prop.startsWith('date_')) {
      const values = data.map((item: any) => Number(item[prop]))
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
const handleExport = () => {
  ElMessage.info(t('stage5.common.messages.exportComingSoon'))
}

// 加载数据
const loadData = async () => {
  try {
    // TODO: 调用 API 获取数据
    // const response = await getRevenueStatistics({ startDate: startDate.value, endDate: endDate.value })
    ElMessage.success(t('stage5.common.messages.dataLoadSuccess'))
  } catch (error) {
    console.error(t('stage5.common.messages.dataLoadFailed'), error)
    ElMessage.error(t('stage5.common.messages.dataLoadFailed'))
  }
}

// 窗口大小变化处理
const handleResize = () => {
  incomeChart?.resize()
  expenseChart?.resize()
}

// 监听日期变化
watch([startDate, endDate], () => {
  loadData()
  nextTick(() => {
    updateCharts()
  })
})

onMounted(() => {
  loadData()
  nextTick(() => {
    initIncomeChart()
    initExpenseChart()
  })
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  incomeChart?.dispose()
  expenseChart?.dispose()
})
</script>

<style scoped>
.revenue-summary-content {
  padding: 24px;
  background: #f5f5f5;
}

/* 日期选择器 */
.date-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.date-quick-select {
  width: 120px;
}

.date-picker {
  width: 180px;
}

.date-separator {
  font-size: 14px;
  color: #666;
}

/* 流水概况 */
.overview-section {
  background: white;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.section-info {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.overview-tabs {
  margin-bottom: 20px;
}

/* 流水卡片 */
.revenue-cards {
  display: grid;
  grid-template-columns: 2fr repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.total-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  padding: 24px;
  color: white;
}

.payment-card {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 24px;
}

.card-title {
  font-size: 14px;
  margin-bottom: 12px;
  opacity: 0.9;
}

.total-card .card-title {
  color: rgba(255, 255, 255, 0.9);
}

.payment-card .card-title {
  color: #666;
}

.card-amount {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 12px;
}

.total-card .card-amount {
  color: white;
}

.payment-card .card-amount {
  color: #333;
}

.card-details {
  display: flex;
  gap: 24px;
  font-size: 13px;
  opacity: 0.9;
}

/* 收支分布 */
.distribution-section {
  background: white;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 40px;
}

.chart-container {
  display: flex;
  flex-direction: column;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.pie-chart {
  width: 100%;
  height: 350px;
}

/* 流水明细 */
.details-section {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.details-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.details-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.details-table {
  margin-top: 16px;
  overflow-x: auto;
}

.details-table :deep(.el-table) {
  font-size: 13px;
}

.details-table :deep(.el-table__header-wrapper) {
  .cell {
    font-weight: 500;
  }
}

.payment-name {
  font-weight: 500;
  color: #333;
}

.amount-value,
.date-amount {
  font-weight: 500;
  color: #333;
}

/* 表格汇总行样式 */
.details-table :deep(.el-table__footer-wrapper) {
  .cell {
    font-weight: 600;
    color: #333;
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .revenue-cards {
    grid-template-columns: 1fr;
  }

  .chart-row {
    grid-template-columns: 1fr;
  }
}
</style>
