<template>
  <StatisticsLayout>
    <div class="notes-summary-content">
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
        <el-button type="primary" @click="handleQuery">查询</el-button>
      </div>

      <!-- 记一笔概况 -->
      <div class="section-header">
        <h2>记一笔概况</h2>
      </div>

      <!-- 统计卡片 -->
      <div class="summary-cards">
        <div class="summary-card net-income">
          <div class="card-icon">
            <el-icon><Money /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-label">净收入</div>
            <div class="card-value">¥{{ summaryStats.netIncome.toFixed(2) }}</div>
          </div>
        </div>

        <div class="summary-card total-income">
          <div class="card-icon">
            <el-icon><Wallet /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-label">总收入</div>
            <div class="card-value">¥{{ summaryStats.totalIncome.toFixed(2) }}</div>
          </div>
        </div>

        <div class="summary-card total-expense">
          <div class="card-icon">
            <el-icon><ShoppingCart /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-label">总支出</div>
            <div class="card-value">¥{{ summaryStats.totalExpense.toFixed(2) }}</div>
          </div>
        </div>
      </div>

      <!-- 记一笔收支统计 -->
      <div class="statistics-section">
        <div class="section-title">记一笔收支统计</div>

        <el-tabs v-model="activeTab" class="statistics-tabs">
          <!-- 按项目 -->
          <el-tab-pane label="按项目" name="byProject">
            <div class="charts-row">
              <div class="chart-wrapper">
                <div class="chart-title">总收入</div>
                <div ref="incomeProjectChartRef" class="pie-chart"></div>
              </div>
              <div class="chart-wrapper">
                <div class="chart-title">总支出</div>
                <div ref="expenseProjectChartRef" class="pie-chart"></div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 按支付方式 -->
          <el-tab-pane label="按支付方式" name="byPaymentMethod">
            <div class="charts-row">
              <div class="chart-wrapper">
                <div class="chart-title">总收入</div>
                <div ref="incomePaymentChartRef" class="pie-chart"></div>
              </div>
              <div class="chart-wrapper">
                <div class="chart-title">总支出</div>
                <div ref="expensePaymentChartRef" class="pie-chart"></div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 记一笔明细 -->
      <div class="details-section">
        <div class="details-header">
          <h3>记一笔明细 ({{ formatDateRange }})</h3>
          <div class="header-actions">
            <el-select v-model="filterType" placeholder="类型" style="width: 120px">
              <el-option label="全部" value="all" />
              <el-option label="收入" value="income" />
              <el-option label="支出" value="expense" />
            </el-select>
            <el-button type="primary" @click="handleExport">导出报表</el-button>
          </div>
        </div>

        <!-- 数据表格 -->
        <el-table :data="filteredTableData" border stripe class="details-table">
          <el-table-column prop="datetime" label="时间" width="180" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 'income' ? 'success' : 'danger'">
                {{ row.type === 'income' ? '收入' : '支出' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="项目" width="150" />
          <el-table-column prop="amount" label="金额" width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'income-amount': row.type === 'income', 'expense-amount': row.type === 'expense' }">
                {{ row.type === 'income' ? '+' : '-' }}¥{{ row.amount.toFixed(2) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="paymentMethod" label="收款方式" width="120" />
          <el-table-column prop="roomNumber" label="关联房间" width="100" />
          <el-table-column prop="voucher" label="凭证" width="80" align="center">
            <template #default="{ row }">
              <el-button v-if="row.voucherCount > 0" link type="primary" @click="handleViewVoucher(row)">
                查看({{ row.voucherCount }})
              </el-button>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="notes" label="备注" min-width="150" show-overflow-tooltip />
        </el-table>

        <!-- 分页和统计 -->
        <div class="table-footer">
          <div class="footer-stats">
            共计{{ filteredTableData.length }}条记录，净收入：
            <span class="net-income-value">¥{{ netIncomeAmount.toFixed(2) }}</span> | 总收入：
            <span class="income-value">¥{{ totalIncomeAmount.toFixed(2) }}</span> | 总支出：
            <span class="expense-value">¥{{ totalExpenseAmount.toFixed(2) }}</span>
          </div>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="filteredTableData.length"
            :page-sizes="[10, 20, 50, 100]"
            layout="prev, pager, next, sizes"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Money, Wallet, ShoppingCart } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import StatisticsLayout from '../statistics/StatisticsLayout.vue'
import { getNotesStatistics, getNotesList } from '@/api/notes'

// 日期范围
const today = new Date().toISOString().split('T')[0]
const startDate = ref(today)
const endDate = ref(today)

// Tab 切换
const activeTab = ref('byProject')

// 表格筛选
const filterType = ref('all')
const currentPage = ref(1)
const pageSize = ref(20)

// 汇总统计数据
const summaryStats = ref({
  netIncome: 0,
  totalIncome: 0,
  totalExpense: 0,
})

// 图表数据
const incomeByProject = ref([
  { name: '房费收入', value: 0 },
  { name: '押金收入', value: 0 },
  { name: '赔偿收入', value: 0 },
  { name: '其他收入', value: 0 },
])

const expenseByProject = ref([
  { name: '房间维修', value: 0 },
  { name: '清洁费用', value: 0 },
  { name: '用品采购', value: 0 },
  { name: '其他支出', value: 0 },
])

const incomeByPayment = ref([
  { name: '现金', value: 0 },
  { name: '支付宝', value: 0 },
  { name: '微信', value: 0 },
  { name: '银行卡', value: 0 },
])

const expenseByPayment = ref([
  { name: '现金', value: 0 },
  { name: '支付宝', value: 0 },
  { name: '微信', value: 0 },
  { name: '银行卡', value: 0 },
])

// 明细数据
const tableData = ref<any[]>([])

// 图表实例
let incomeProjectChart: echarts.ECharts | null = null
let expenseProjectChart: echarts.ECharts | null = null
let incomePaymentChart: echarts.ECharts | null = null
let expensePaymentChart: echarts.ECharts | null = null

const incomeProjectChartRef = ref<HTMLElement>()
const expenseProjectChartRef = ref<HTMLElement>()
const incomePaymentChartRef = ref<HTMLElement>()
const expensePaymentChartRef = ref<HTMLElement>()

// 日期范围格式化
const formatDateRange = computed(() => {
  if (startDate.value === endDate.value) {
    return startDate.value
  }
  return `${startDate.value} 至 ${endDate.value}`
})

// 筛选后的表格数据
const filteredTableData = computed(() => {
  if (filterType.value === 'all') {
    return tableData.value
  }
  return tableData.value.filter((item) => item.type === filterType.value)
})

// 净收入
const netIncomeAmount = computed(() => {
  return totalIncomeAmount.value - totalExpenseAmount.value
})

// 总收入
const totalIncomeAmount = computed(() => {
  return filteredTableData.value
    .filter((item) => item.type === 'income')
    .reduce((sum, item) => sum + item.amount, 0)
})

// 总支出
const totalExpenseAmount = computed(() => {
  return filteredTableData.value
    .filter((item) => item.type === 'expense')
    .reduce((sum, item) => sum + item.amount, 0)
})

// 创建环形图配置
const createPieChartOption = (data: any[], total: number, type: 'income' | 'expense'): EChartsOption => {
  const filteredData = data.filter((item) => item.value > 0)

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
    },
    legend: {
      show: true,
      orient: 'vertical',
      right: '10%',
      top: 'center',
      itemGap: 16,
      itemWidth: 12,
      itemHeight: 12,
      formatter: (name: string) => {
        const item = filteredData.find((d) => d.name === name)
        if (item) {
          const percentage = total > 0 ? ((item.value / total) * 100).toFixed(0) : 0
          return `${name}  ${percentage}%`
        }
        return name
      },
      textStyle: {
        fontSize: 14,
        color: '#666',
      },
    },
    series: [
      {
        type: 'pie',
        radius: ['55%', '75%'],
        center: ['30%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
        },
        emphasis: {
          scale: true,
          scaleSize: 10,
        },
        labelLine: {
          show: false,
        },
        data: filteredData,
        color: ['#4E7CFF', '#53D769', '#FFB946', '#FF6B6B', '#A78BFA', '#60A5FA'],
      },
      {
        type: 'pie',
        radius: ['0%', '45%'],
        center: ['30%', '50%'],
        silent: true,
        label: {
          show: true,
          position: 'center',
          formatter: () => {
            return `{title|${type === 'income' ? '总收入' : '总支出'}}\n{value|¥${total.toFixed(2)}}`
          },
          rich: {
            title: {
              fontSize: 14,
              color: '#999',
              lineHeight: 24,
            },
            value: {
              fontSize: 20,
              fontWeight: 'bold',
              color: '#333',
              lineHeight: 30,
            },
          },
        },
        labelLine: {
          show: false,
        },
        data: [{ value: 1, itemStyle: { color: 'transparent' } }],
      },
    ],
  }
}

// 初始化图表
const initCharts = () => {
  if (activeTab.value === 'byProject') {
    nextTick(() => {
      if (incomeProjectChartRef.value) {
        incomeProjectChart = echarts.init(incomeProjectChartRef.value)
        const incomeTotal = incomeByProject.value.reduce((sum, item) => sum + item.value, 0)
        incomeProjectChart.setOption(createPieChartOption(incomeByProject.value, incomeTotal, 'income'))
      }

      if (expenseProjectChartRef.value) {
        expenseProjectChart = echarts.init(expenseProjectChartRef.value)
        const expenseTotal = expenseByProject.value.reduce((sum, item) => sum + item.value, 0)
        expenseProjectChart.setOption(createPieChartOption(expenseByProject.value, expenseTotal, 'expense'))
      }
    })
  } else {
    nextTick(() => {
      if (incomePaymentChartRef.value) {
        incomePaymentChart = echarts.init(incomePaymentChartRef.value)
        const incomeTotal = incomeByPayment.value.reduce((sum, item) => sum + item.value, 0)
        incomePaymentChart.setOption(createPieChartOption(incomeByPayment.value, incomeTotal, 'income'))
      }

      if (expensePaymentChartRef.value) {
        expensePaymentChart = echarts.init(expensePaymentChartRef.value)
        const expenseTotal = expenseByPayment.value.reduce((sum, item) => sum + item.value, 0)
        expensePaymentChart.setOption(createPieChartOption(expenseByPayment.value, expenseTotal, 'expense'))
      }
    })
  }
}

// 更新图表
const updateCharts = () => {
  if (activeTab.value === 'byProject') {
    const incomeTotal = incomeByProject.value.reduce((sum, item) => sum + item.value, 0)
    const expenseTotal = expenseByProject.value.reduce((sum, item) => sum + item.value, 0)
    incomeProjectChart?.setOption(createPieChartOption(incomeByProject.value, incomeTotal, 'income'))
    expenseProjectChart?.setOption(createPieChartOption(expenseByProject.value, expenseTotal, 'expense'))
  } else {
    const incomeTotal = incomeByPayment.value.reduce((sum, item) => sum + item.value, 0)
    const expenseTotal = expenseByPayment.value.reduce((sum, item) => sum + item.value, 0)
    incomePaymentChart?.setOption(createPieChartOption(incomeByPayment.value, incomeTotal, 'income'))
    expensePaymentChart?.setOption(createPieChartOption(expenseByPayment.value, expenseTotal, 'expense'))
  }
}

// 加载数据
const loadData = async () => {
  try {
    // 获取统计数据
    const statsResponse = await getNotesStatistics({
      startDate: startDate.value,
      endDate: endDate.value,
    })

    if (statsResponse.success) {
      const stats = statsResponse.data
      summaryStats.value = {
        netIncome: stats.netIncome,
        totalIncome: stats.totalIncome,
        totalExpense: stats.totalExpense,
      }

      // 更新按项目分类的数据
      incomeByProject.value = stats.incomeByProject.map((item) => ({
        name: item.name,
        value: item.value,
      }))

      expenseByProject.value = stats.expenseByProject.map((item) => ({
        name: item.name,
        value: item.value,
      }))

      // 更新按支付方式的数据
      incomeByPayment.value = stats.incomeByPayment.map((item) => ({
        name: item.name,
        value: item.value,
      }))

      expenseByPayment.value = stats.expenseByPayment.map((item) => ({
        name: item.name,
        value: item.value,
      }))

      // 更新图表
      updateCharts()
    } else {
      ElMessage.error(statsResponse.message || '获取统计数据失败')
    }

    // 获取明细列表
    const listResponse = await getNotesList({
      startDate: startDate.value,
      endDate: endDate.value,
    })

    if (listResponse.success) {
      tableData.value = listResponse.data.map((item) => ({
        datetime: item.datetime,
        type: item.type,
        category: item.category,
        amount: item.amount,
        paymentMethod: item.paymentMethod,
        roomNumber: item.roomNumber || '-',
        voucherCount: item.voucherCount,
        notes: item.notes || '-',
      }))
    } else {
      ElMessage.error(listResponse.message || '获取明细列表失败')
    }

    ElMessage.success('数据加载成功')
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  }
}

// 查询
const handleQuery = () => {
  if (!startDate.value || !endDate.value) {
    ElMessage.warning('请选择日期范围')
    return
  }
  loadData()
}

// 导出报表
const handleExport = () => {
  ElMessage.info('导出报表功能开发中...')
}

// 查看凭证
const handleViewVoucher = (row: any) => {
  ElMessage.info('查看凭证功能开发中...')
}

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
}

// 窗口大小变化处理
const handleResize = () => {
  incomeProjectChart?.resize()
  expenseProjectChart?.resize()
  incomePaymentChart?.resize()
  expensePaymentChart?.resize()
}

// 监听 Tab 切换
watch(activeTab, () => {
  // 销毁旧图表
  incomeProjectChart?.dispose()
  expenseProjectChart?.dispose()
  incomePaymentChart?.dispose()
  expensePaymentChart?.dispose()

  incomeProjectChart = null
  expenseProjectChart = null
  incomePaymentChart = null
  expensePaymentChart = null

  // 重新初始化图表
  initCharts()
})

onMounted(() => {
  loadData()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  incomeProjectChart?.dispose()
  expenseProjectChart?.dispose()
  incomePaymentChart?.dispose()
  expensePaymentChart?.dispose()
})
</script>

<style scoped>
.notes-summary-content {
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

.date-separator {
  color: #666;
  font-size: 14px;
}

/* 标题 */
.section-header {
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

/* 统计卡片 */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.card-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 28px;
}

.summary-card.net-income .card-icon {
  background: #e8f5e9;
  color: #4caf50;
}

.summary-card.total-income .card-icon {
  background: #e3f2fd;
  color: #2196f3;
}

.summary-card.total-expense .card-icon {
  background: #ffebee;
  color: #f44336;
}

.card-content {
  flex: 1;
}

.card-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.card-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

/* 统计区域 */
.statistics-section {
  background: white;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.statistics-tabs {
  margin-top: 20px;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 40px;
  padding: 20px 0;
}

.chart-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chart-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 20px;
}

.pie-chart {
  width: 100%;
  height: 300px;
}

/* 明细区域 */
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

.header-actions {
  display: flex;
  gap: 12px;
}

.details-table {
  margin-bottom: 16px;
}

.income-amount {
  color: #52c41a;
  font-weight: 500;
}

.expense-amount {
  color: #ff4d4f;
  font-weight: 500;
}

/* 表格底部 */
.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.footer-stats {
  font-size: 14px;
  color: #666;
}

.net-income-value {
  color: #52c41a;
  font-weight: 600;
  font-size: 16px;
}

.income-value {
  color: #1890ff;
  font-weight: 600;
}

.expense-value {
  color: #ff4d4f;
  font-weight: 600;
}
</style>