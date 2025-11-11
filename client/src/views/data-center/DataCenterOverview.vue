<template>
  <StatisticsLayout>
    <div class="overview-container">
      <!-- 顶部选项卡 -->
      <div class="tabs-section">
        <el-tabs v-model="activeTab" class="overview-tabs" @tab-change="handleTabChange">
          <el-tab-pane label="营业概况" name="business" />
          <el-tab-pane label="流水汇总" name="revenue" />
          <el-tab-pane label="渠道汇总" name="channel" />
          <el-tab-pane label="销售汇总" name="sales" />
        </el-tabs>
      </div>

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

      <!-- 营业概况内容 -->
      <div v-if="activeTab === 'business'" class="tab-content">
        <!-- 营业概况统计卡片 -->
        <div class="stats-section">
          <div class="stat-card primary">
            <div class="stat-icon">
              <el-icon size="48"><Money /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">住宿总营业额</div>
              <div class="stat-value">¥{{ totalRevenue.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-content">
              <div class="stat-label">房费</div>
              <div class="stat-value">¥{{ roomFee.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-content">
              <div class="stat-label">押金</div>
              <div class="stat-value">¥{{ deposit.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-content">
              <div class="stat-label">退房金</div>
              <div class="stat-value">¥{{ checkout.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-content">
              <div class="stat-label">餐食/客房消费</div>
              <div class="stat-value">¥{{ roomService.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>
        </div>

        <!-- 消费分类分布和住宿消费趋势 -->
        <div class="charts-row">
          <div class="chart-card">
            <h3 class="chart-title">消费分类分布</h3>
            <div ref="businessPieChart" class="chart-container"></div>
          </div>

          <div class="chart-card">
            <h3 class="chart-title">住宿消费趋势</h3>
            <div ref="businessBarChart" class="chart-container"></div>
          </div>
        </div>

        <!-- 住宿消费明细表格 -->
        <div class="table-section">
          <div class="table-header">
            <h3 class="table-title">住宿消费明细 ({{ startDate }} 至 {{ endDate }})</h3>
            <el-button type="primary">导出明细</el-button>
          </div>
          <el-table :data="businessDetailData" border stripe class="detail-table">
            <el-table-column prop="category" label="项目" min-width="120" align="center" />
            <el-table-column prop="total" label="合计" min-width="150" align="center">
              <template #default="{ row }">
                <span class="amount-bold">¥{{ row.total.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nov8" label="11月8日" min-width="150" align="center">
              <template #default="{ row }">
                ¥{{ row.nov8.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 流水汇总内容 -->
      <div v-if="activeTab === 'revenue'" class="tab-content">
        <!-- 流水概况标签页 -->
        <div class="revenue-tabs">
          <el-button
            :type="revenueSubTab === 'payment' ? 'primary' : 'default'"
            @click="revenueSubTab = 'payment'"
          >
            支付方式
          </el-button>
          <el-button
            :type="revenueSubTab === 'category' ? 'primary' : 'default'"
            @click="revenueSubTab = 'category'"
          >
            款项分类
          </el-button>
        </div>

        <!-- 流水统计卡片 -->
        <div class="revenue-stats">
          <div class="stat-card large">
            <div class="stat-label">总流水</div>
            <div class="stat-value large">¥{{ revenueTotal.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            <div class="stat-details">
              <span>分账款: ¥{{ splitAccount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
              <span>实收款: ¥{{ actualReceived.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
            </div>
          </div>

          <div class="revenue-cards">
            <div class="revenue-card">
              <div class="card-label">Booking代收</div>
              <div class="card-value">¥{{ bookingRevenue.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
            <div class="revenue-card">
              <div class="card-label">Airbnb代收</div>
              <div class="card-value">¥{{ airbnbRevenue.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>
        </div>

        <!-- 收款分布和总支出饼图 -->
        <div class="charts-row">
          <div class="chart-card">
            <h3 class="chart-title">收款分布</h3>
            <div ref="revenueDistChart" class="chart-container"></div>
          </div>
          <div class="chart-card">
            <h3 class="chart-title">总支出</h3>
            <div ref="expenseChart" class="chart-container"></div>
          </div>
        </div>

        <!-- 流水明细表格 -->
        <div class="table-section">
          <div class="table-header">
            <h3 class="table-title">流水明细 ({{ startDate }} 至 {{ endDate }})</h3>
            <el-button type="primary">导出明细</el-button>
          </div>

          <div class="table-tabs">
            <el-button
              v-for="tab in revenueTableTabs"
              :key="tab.key"
              :type="revenueTableTab === tab.key ? 'primary' : 'default'"
              size="small"
              @click="revenueTableTab = tab.key"
            >
              {{ tab.label }}
            </el-button>
          </div>

          <el-table :data="revenueTableData" border stripe class="detail-table">
            <el-table-column prop="paymentMethod" label="支付方式" min-width="120" align="center" />
            <el-table-column prop="total" label="合计" min-width="150" align="center">
              <template #default="{ row }">
                <span class="amount-bold">¥{{ row.total.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nov8" label="11月8日" min-width="150" align="center">
              <template #default="{ row }">
                ¥{{ row.nov8.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 渠道汇总内容 -->
      <div v-if="activeTab === 'channel'" class="tab-content">
        <!-- 渠道消费分布和间夜分布 -->
        <div class="charts-row">
          <div class="chart-card">
            <h3 class="chart-title">渠道消费分布 <span class="chart-subtitle">统计消费</span></h3>
            <div ref="channelRevenueChart" class="chart-container"></div>
          </div>
          <div class="chart-card">
            <h3 class="chart-title">渠道间夜分布 <span class="chart-subtitle">统计消费</span></h3>
            <div ref="channelNightsChart" class="chart-container"></div>
          </div>
        </div>

        <!-- 渠道消费趋势和间夜趋势 -->
        <div class="charts-row">
          <div class="chart-card wide">
            <h3 class="chart-title">渠道消费趋势 <span class="chart-subtitle">统计消费</span></h3>
            <div ref="channelRevenueTrendChart" class="chart-container-large"></div>
          </div>
          <div class="chart-card wide">
            <h3 class="chart-title">渠道间夜趋势 <span class="chart-subtitle">统计消费</span></h3>
            <div ref="channelNightsTrendChart" class="chart-container-large"></div>
          </div>
        </div>

        <!-- 渠道明细表格 -->
        <div class="table-section">
          <div class="table-header">
            <h3 class="table-title">渠道明细 ({{ startDate }} 至 {{ endDate }})</h3>
            <el-button type="primary">导出明细</el-button>
          </div>

          <div class="table-tabs">
            <el-button
              v-for="tab in channelTableTabs"
              :key="tab.key"
              :type="channelTableTab === tab.key ? 'primary' : 'default'"
              size="small"
              @click="channelTableTab = tab.key"
            >
              {{ tab.label }}
            </el-button>
          </div>

          <el-table :data="channelTableData" border stripe class="detail-table">
            <el-table-column prop="channel" label="渠道" min-width="120" align="center" />
            <el-table-column prop="total" label="合计" min-width="150" align="center">
              <template #default="{ row }">
                <span class="amount-bold">{{ row.total }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nov8" label="11月8日" min-width="150" align="center">
              <template #default="{ row }">
                {{ row.nov8 }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 销售汇总内容 -->
      <div v-if="activeTab === 'sales'" class="tab-content">
        <!-- 销售额卡片 -->
        <div class="sales-stats">
          <div class="stat-card large">
            <div class="stat-label">总销售额</div>
            <div class="stat-value large">¥{{ salesTotal.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
          </div>
        </div>

        <!-- 每日销售额趋势 -->
        <div class="chart-card full-width">
          <h3 class="chart-title">每日销售额统计 <span class="chart-subtitle">统计消费</span></h3>
          <div ref="salesTrendChart" class="chart-container-xlarge"></div>
        </div>

        <!-- 销售订单明细 -->
        <div class="table-section">
          <div class="table-header">
            <h3 class="table-title">销售订单明细 ({{ startDate }} 至 {{ endDate }})</h3>
            <el-button type="primary">导出明细</el-button>
          </div>

          <div class="search-section">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索订单号、渠道号、客户名号、手机号"
              clearable
              style="max-width: 400px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="searchChannel" placeholder="全部" clearable style="width: 150px">
              <el-option label="全部" value="" />
              <el-option label="Booking.com" value="booking" />
              <el-option label="Airbnb" value="airbnb" />
            </el-select>
            <el-select v-model="searchGuest" placeholder="全部" clearable style="width: 150px">
              <el-option label="全部" value="" />
            </el-select>
          </div>

          <el-button type="default" size="small" style="margin-bottom: 16px">收起</el-button>

          <el-table :data="salesTableData" border stripe class="detail-table">
            <el-table-column prop="createdAt" label="创建时间" min-width="150" align="center" />
            <el-table-column prop="guestName" label="姓名" min-width="100" align="center" />
            <el-table-column prop="orderNumber" label="订单号/渠道号" min-width="200" align="center">
              <template #default="{ row }">
                <div>{{ row.orderNumber }}</div>
                <div style="color: #909399; font-size: 12px">{{ row.channelNumber }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="channel" label="渠道" min-width="120" align="center" />
            <el-table-column prop="customerName" label="客户名" min-width="120" align="center" />
            <el-table-column prop="phone" label="手机号" min-width="120" align="center" />
            <el-table-column prop="amount" label="订单总金额" min-width="150" align="center">
              <template #default="{ row }">
                <span class="amount-bold">¥{{ row.amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="table-footer">
            <span class="table-info">共 2 条</span>
            <span class="total-amount">¥{{ salesTotal.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
            <el-pagination
              small
              layout="prev, pager, next"
              :total="2"
              :page-size="10"
              :current-page="1"
            />
          </div>
        </div>
      </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Search, Money } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import StatisticsLayout from '../statistics/StatisticsLayout.vue'

const activeTab = ref('business')
const dateType = ref('today')
const startDate = ref('2025/11/08')
const endDate = ref('2025/11/08')

// 营业概况相关数据
const totalRevenue = ref(181196.45)
const roomFee = ref(154256.45)
const deposit = ref(0.00)
const checkout = ref(0.00)
const roomService = ref(26940.00)

interface BusinessDetailItem {
  category: string
  total: number
  nov8: number
}

const businessDetailData = ref<BusinessDetailItem[]>([
  { category: '房费', total: 154256.45, nov8: 154256.45 },
  { category: '退房金', total: 0.00, nov8: 0.00 },
  { category: '餐食/客房消费', total: 26940.00, nov8: 26940.00 },
  { category: '押金', total: 0.00, nov8: 0.00 },
])

// 流水汇总相关数据
const revenueSubTab = ref('payment')
const revenueTableTab = ref('payment-method')
const revenueTotal = ref(276793.14)
const splitAccount = ref(276793.14)
const actualReceived = ref(0.00)
const bookingRevenue = ref(182126.14)
const airbnbRevenue = ref(94667.00)

const revenueTableTabs = [
  { key: 'payment-method', label: '支付方式' },
  { key: 'room-fee', label: '房费来源' }
]

const revenueTableData = ref([
  { paymentMethod: 'Booking代收', total: 182126.14, nov8: 182126.14 },
  { paymentMethod: 'Airbnb代收', total: 94667.00, nov8: 94667.00 }
])

// 渠道汇总相关数据
const channelTableTab = ref('channel-fee')
const channelTableTabs = [
  { key: 'channel-fee', label: '渠道消费' },
  { key: 'channel-nights', label: '间夜明细' }
]

const channelTableData = ref([
  { channel: 'Airbnb', total: '¥97,981.00', nov8: '¥97,981.00' },
  { channel: 'Booking.com', total: '¥83,215.45', nov8: '¥83,215.45' }
])

// 销售汇总相关数据
const salesTotal = ref(59054.65)
const searchKeyword = ref('')
const searchChannel = ref('')
const searchGuest = ref('')

const salesTableData = ref([
  {
    createdAt: '2025/11/08 14:37:33',
    guestName: '系统',
    orderNumber: '1968094745265878529',
    channelNumber: '648615527',
    channel: 'Booking.com',
    customerName: 'HOSHINO ABE',
    phone: '+81 9082460244',
    amount: 20394.65
  },
  {
    createdAt: '2025/11/08 07:16:40',
    guestName: '系统',
    orderNumber: '1968094742920252902',
    channelNumber: 'HM4DRVABE3',
    channel: 'Airbnb',
    customerName: 'Judy An',
    phone: '61430533463',
    amount: 38660.00
  }
])

// ECharts实例
const businessPieChart = ref<HTMLDivElement>()
const businessBarChart = ref<HTMLDivElement>()
const revenueDistChart = ref<HTMLDivElement>()
const expenseChart = ref<HTMLDivElement>()
const channelRevenueChart = ref<HTMLDivElement>()
const channelNightsChart = ref<HTMLDivElement>()
const channelRevenueTrendChart = ref<HTMLDivElement>()
const channelNightsTrendChart = ref<HTMLDivElement>()
const salesTrendChart = ref<HTMLDivElement>()

let businessPie: ECharts | null = null
let businessBar: ECharts | null = null
let revenueDist: ECharts | null = null
let expense: ECharts | null = null
let channelRevenue: ECharts | null = null
let channelNights: ECharts | null = null
let channelRevenueTrend: ECharts | null = null
let channelNightsTrend: ECharts | null = null
let salesTrend: ECharts | null = null

// 初始化营业概况饼图
const initBusinessPieChart = () => {
  if (!businessPieChart.value) return

  businessPie = echarts.init(businessPieChart.value)

  const pieChartData = [
    { value: 154256.45, name: '房费', percentage: '85.13%' },
    { value: 0.00, name: '押金', percentage: '0%' },
    { value: 0.00, name: '退房金', percentage: '0%' },
    { value: 26940.00, name: '餐食/客房消费', percentage: '14.87%' }
  ]

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center',
      formatter: (name: string) => {
        const data = pieChartData.find((item) => item.name === name)
        return `${name}  ${data?.percentage || '0%'}`
      }
    },
    series: [
      {
        name: '消费分类',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['30%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: pieChartData
      }
    ],
    color: ['#5b8ff9', '#ff6b6b', '#ffd93d', '#f9c94a']
  }

  businessPie.setOption(option)
}

// 初始化营业概况柱状图
const initBusinessBarChart = () => {
  if (!businessBarChart.value) return

  businessBar = echarts.init(businessBarChart.value)

  const dates = ['11月2日', '11月3日', '11月4日', '11月5日', '11月6日', '11月7日', '11月8日', '11月9日']

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        let result = params[0].axisValue + '<br/>'
        let total = 0
        params.forEach((item: any) => {
          result += `${item.marker}${item.seriesName}: ¥${item.value.toFixed(2)}<br/>`
          total += item.value
        })
        result += `合计: ¥${total.toFixed(2)}`
        return result
      }
    },
    legend: {
      data: ['房费', '退房金', '餐食/客房消费', '押金'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '¥{value}'
      }
    },
    series: [
      {
        name: '房费',
        type: 'bar',
        stack: 'total',
        data: [18000, 19500, 21000, 22000, 20500, 18500, 17500, 18000]
      },
      {
        name: '退房金',
        type: 'bar',
        stack: 'total',
        data: [0, 0, 0, 0, 0, 0, 0, 0]
      },
      {
        name: '餐食/客房消费',
        type: 'bar',
        stack: 'total',
        data: [3200, 3500, 3800, 4000, 3700, 3300, 3100, 3200]
      },
      {
        name: '押金',
        type: 'bar',
        stack: 'total',
        data: [0, 0, 0, 0, 0, 0, 0, 0]
      }
    ],
    color: ['#5b8ff9', '#ffd93d', '#f9c94a', '#ff6b6b']
  }

  businessBar.setOption(option)
}

// 初始化流水分布饼图
const initRevenueDistChart = () => {
  if (!revenueDistChart.value) return

  revenueDist = echarts.init(revenueDistChart.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      formatter: (name: string) => {
        const percentages: Record<string, string> = {
          'Booking代收': '65.8%',
          'Airbnb代收': '34.2%'
        }
        return `${name}  ${percentages[name] || ''}`
      }
    },
    series: [
      {
        name: '收款分布',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['30%', '50%'],
        data: [
          { value: 182126.14, name: 'Booking代收' },
          { value: 94667.00, name: 'Airbnb代收' }
        ],
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        }
      }
    ],
    color: ['#5470c6', '#fac858']
  }

  revenueDist.setOption(option)
}

// 初始化总支出饼图
const initExpenseChart = () => {
  if (!expenseChart.value) return

  expense = echarts.init(expenseChart.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    series: [
      {
        name: '总支出',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: [
          { value: 0, name: '总支出' }
        ],
        itemStyle: {
          color: '#e5e5e5'
        },
        label: {
          show: true,
          position: 'center',
          formatter: '总支出\n¥0.00',
          fontSize: 16
        }
      }
    ]
  }

  expense.setOption(option)
}

// 初始化渠道消费分布饼图
const initChannelRevenueChart = () => {
  if (!channelRevenueChart.value) return

  channelRevenue = echarts.init(channelRevenueChart.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      formatter: (name: string) => {
        const percentages: Record<string, string> = {
          'Airbnb': '54.07%',
          'Booking.com': '45.93%'
        }
        return `${name}  ${percentages[name] || ''}`
      }
    },
    series: [
      {
        name: '渠道消费',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['30%', '50%'],
        data: [
          { value: 97981.00, name: 'Airbnb' },
          { value: 83215.45, name: 'Booking.com' }
        ],
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        }
      }
    ],
    color: ['#5470c6', '#fac858']
  }

  channelRevenue.setOption(option)
}

// 初始化渠道间夜分布饼图
const initChannelNightsChart = () => {
  if (!channelNightsChart.value) return

  channelNights = echarts.init(channelNightsChart.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      formatter: (name: string) => {
        const percentages: Record<string, string> = {
          'Booking.com': '66.67%',
          'Airbnb': '33.33%'
        }
        return `${name}  ${percentages[name] || ''}`
      }
    },
    series: [
      {
        name: '间夜(数)',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['30%', '50%'],
        data: [
          { value: 8, name: 'Booking.com' },
          { value: 4, name: 'Airbnb' }
        ],
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        }
      }
    ],
    color: ['#5470c6', '#fac858']
  }

  channelNights.setOption(option)
}

// 初始化渠道消费趋势折线图
const initChannelRevenueTrendChart = () => {
  if (!channelRevenueTrendChart.value) return

  channelRevenueTrend = echarts.init(channelRevenueTrendChart.value)

  const dates = ['11月2日', '11月3日', '11月4日', '11月5日', '11月6日', '11月7日', '11月8日']

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['Airbnb', 'Booking.com'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '¥{value}'
      }
    },
    series: [
      {
        name: 'Airbnb',
        type: 'line',
        smooth: true,
        data: [30000, 25000, 28000, 35000, 40000, 45000, 50000],
        lineStyle: { width: 2 }
      },
      {
        name: 'Booking.com',
        type: 'line',
        smooth: true,
        data: [50000, 55000, 60000, 65000, 70000, 75000, 80000],
        lineStyle: { width: 2 }
      }
    ],
    color: ['#5470c6', '#fac858']
  }

  channelRevenueTrend.setOption(option)
}

// 初始化渠道间夜趋势柱状图
const initChannelNightsTrendChart = () => {
  if (!channelNightsTrendChart.value) return

  channelNightsTrend = echarts.init(channelNightsTrendChart.value)

  const dates = ['11月2日', '11月3日', '11月4日', '11月5日', '11月6日', '11月7日', '11月8日']

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['booking.com', 'Airbnb'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: 'booking.com',
        type: 'bar',
        stack: 'total',
        data: [4, 2, 1, 2, 4, 6, 4]
      },
      {
        name: 'Airbnb',
        type: 'bar',
        stack: 'total',
        data: [5, 6, 7, 6, 5, 4, 5]
      }
    ],
    color: ['#5470c6', '#fac858']
  }

  channelNightsTrend.setOption(option)
}

// 初始化销售趋势折线图
const initSalesTrendChart = () => {
  if (!salesTrendChart.value) return

  salesTrend = echarts.init(salesTrendChart.value)

  const dates = ['11月2日', '11月3日', '11月4日', '11月5日', '11月6日', '11月7日', '11月8日']

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const param = params[0]
        return `${param.axisValue}<br/>销售额: ¥${param.value.toFixed(2)}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '¥{value}'
      }
    },
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        data: [450000, 410000, 530000, 520000, 500000, 380000, 150000],
        lineStyle: {
          width: 3,
          color: '#5470c6'
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
        }
      }
    ]
  }

  salesTrend.setOption(option)
}

// 标签页切换处理
const handleTabChange = async () => {
  await nextTick()

  if (activeTab.value === 'business') {
    initBusinessPieChart()
    initBusinessBarChart()
  } else if (activeTab.value === 'revenue') {
    initRevenueDistChart()
    initExpenseChart()
  } else if (activeTab.value === 'channel') {
    initChannelRevenueChart()
    initChannelNightsChart()
    initChannelRevenueTrendChart()
    initChannelNightsTrendChart()
  } else if (activeTab.value === 'sales') {
    initSalesTrendChart()
  }

  handleResize()
}

// 响应式调整
const handleResize = () => {
  businessPie?.resize()
  businessBar?.resize()
  revenueDist?.resize()
  expense?.resize()
  channelRevenue?.resize()
  channelNights?.resize()
  channelRevenueTrend?.resize()
  channelNightsTrend?.resize()
  salesTrend?.resize()
}

onMounted(() => {
  // 默认初始化营业概况图表
  nextTick(() => {
    initBusinessPieChart()
    initBusinessBarChart()
  })

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  businessPie?.dispose()
  businessBar?.dispose()
  revenueDist?.dispose()
  expense?.dispose()
  channelRevenue?.dispose()
  channelNights?.dispose()
  channelRevenueTrend?.dispose()
  channelNightsTrend?.dispose()
  salesTrend?.dispose()
})
</script>

<style scoped>
.overview-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

/* 选项卡 */
.tabs-section {
  margin-bottom: 20px;
}

.overview-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
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

/* 标签页内容 */
.tab-content {
  margin-top: 24px;
}

/* 营业概况样式 */
.stats-section {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  grid-column: span 1;
}

.stat-icon {
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-card .stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-card.primary .stat-label {
  color: rgba(255, 255, 255, 0.9);
}

.stat-card .stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.stat-card.primary .stat-value {
  color: white;
}

/* 流水汇总样式 */
.revenue-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.revenue-stats {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

.stat-card.large {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 8px;
  padding: 32px;
}

.stat-label {
  font-size: 14px;
  margin-bottom: 12px;
  opacity: 0.9;
}

.stat-value.large {
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 16px;
}

.stat-details {
  display: flex;
  gap: 24px;
  font-size: 14px;
  opacity: 0.9;
}

.revenue-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.revenue-card {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
}

.card-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.card-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

/* 图表区域 */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

.chart-card {
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
}

.chart-card.wide {
  grid-column: span 1;
}

.chart-card.full-width {
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 32px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
}

.chart-subtitle {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
  margin-left: 8px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

.chart-container-large {
  width: 100%;
  height: 350px;
}

.chart-container-xlarge {
  width: 100%;
  height: 400px;
}

/* 销售汇总样式 */
.sales-stats {
  margin-bottom: 32px;
}

.sales-stats .stat-card.large {
  max-width: 500px;
}

/* 表格区域 */
.table-section {
  margin-top: 32px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.table-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.search-section {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
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

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding: 12px 0;
}

.table-info {
  font-size: 14px;
  color: #606266;
}

.total-amount {
  font-size: 16px;
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
