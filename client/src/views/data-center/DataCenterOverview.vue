<template>
  <StatisticsLayout>
    <div class="overview-container">
      <!-- 顶部选项卡 -->
      <div class="tabs-section">
        <el-tabs v-model="activeTab" class="overview-tabs" @tab-change="handleTabChange">
          <el-tab-pane :label="t('stage5.dataCenter.overview.tabs.business')" name="business" />
          <el-tab-pane :label="t('stage5.dataCenter.overview.tabs.revenue')" name="revenue" />
          <el-tab-pane :label="t('stage5.dataCenter.overview.tabs.channel')" name="channel" />
          <el-tab-pane :label="t('stage5.dataCenter.overview.tabs.sales')" name="sales" />
        </el-tabs>
      </div>

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

      <!-- 营业概况内容 -->
      <div v-if="activeTab === 'business'" class="tab-content">
        <!-- 营业概况统计卡片 -->
        <div class="stats-section">
          <div class="stat-card primary">
            <div class="stat-icon">
              <el-icon size="48"><Money /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">{{ t('stage5.statistics.business.totalAccommodationRevenue') }}</div>
              <div class="stat-value">¥{{ totalRevenue.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-content">
              <div class="stat-label">{{ t('stage5.statistics.common.roomFee') }}</div>
              <div class="stat-value">¥{{ roomFee.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-content">
              <div class="stat-label">{{ t('stage5.statistics.common.deposit') }}</div>
              <div class="stat-value">¥{{ deposit.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-content">
              <div class="stat-label">{{ t('stage5.dataCenter.overview.checkoutRefund') }}</div>
              <div class="stat-value">¥{{ checkout.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-content">
              <div class="stat-label">{{ t('stage5.dataCenter.overview.roomService') }}</div>
              <div class="stat-value">¥{{ roomService.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
            </div>
          </div>
        </div>

        <!-- 消费分类分布和住宿消费趋势 -->
        <div class="charts-row">
          <div class="chart-card">
            <h3 class="chart-title">{{ t('stage5.dataCenter.overview.spendCategoryDistribution') }}</h3>
            <div ref="businessPieChart" class="chart-container"></div>
          </div>

          <div class="chart-card">
            <h3 class="chart-title">{{ t('stage5.dataCenter.overview.accommodationSpendTrend') }}</h3>
            <div ref="businessBarChart" class="chart-container"></div>
          </div>
        </div>

        <!-- 住宿消费明细表格 -->
        <div class="table-section">
          <div class="table-header">
            <h3 class="table-title">{{ t('stage5.dataCenter.overview.accommodationSpendDetails') }} ({{ dateRangeLabel }})</h3>
            <el-button type="primary">{{ t('stage5.common.actions.exportDetails') }}</el-button>
          </div>
            <el-table :data="businessDetailData" border stripe class="detail-table">
              <el-table-column prop="category" :label="t('stage5.common.fields.project')" min-width="120" align="center" />
              <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150" align="center">
                <template #default="{ row }">
                  <span class="amount-bold">¥{{ row.total.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-for="column in dateColumns"
                :key="column.prop"
                :prop="column.prop"
                :label="column.label"
                min-width="150"
                align="center"
              >
                <template #default="{ row }">
                  ¥{{ formatMoneyCell(row, column.prop) }}
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
            @click="handleRevenueSubTabChange('payment')"
          >
            {{ t('stage5.statistics.revenue.paymentMethod') }}
          </el-button>
          <el-button
            :type="revenueSubTab === 'category' ? 'primary' : 'default'"
            @click="handleRevenueSubTabChange('category')"
          >
            {{ t('stage5.dataCenter.overview.paymentCategory') }}
          </el-button>
        </div>

        <!-- 支付方式内容 -->
        <div v-if="revenueSubTab === 'payment'">
          <!-- 流水统计卡片 -->
          <div class="revenue-stats">
            <div class="stat-card large">
              <div class="stat-label">{{ t('stage5.dataCenter.overview.totalRevenue') }}</div>
              <div class="stat-value large">¥{{ revenueTotal.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
              <div class="stat-details">
                <span>{{ t('stage5.dataCenter.overview.splitAccount') }}: ¥{{ splitAccount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
                <span>{{ t('stage5.dataCenter.overview.actualReceived') }}: ¥{{ actualReceived.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
              </div>
            </div>

            <div class="revenue-cards">
              <div class="revenue-card">
                <div class="card-label">{{ t('stage5.dataCenter.overview.bookingCollection') }}</div>
                <div class="card-value">¥{{ bookingRevenue.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
              </div>
              <div class="revenue-card">
                <div class="card-label">{{ t('stage5.dataCenter.overview.airbnbCollection') }}</div>
                <div class="card-value">¥{{ airbnbRevenue.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
              </div>
            </div>
          </div>

          <!-- 收款分布和总支出饼图 -->
          <div class="charts-row">
            <div class="chart-card">
              <h3 class="chart-title">{{ t('stage5.dataCenter.overview.collectionDistribution') }}</h3>
              <div ref="revenueDistChart" class="chart-container"></div>
            </div>
            <div class="chart-card">
              <h3 class="chart-title">{{ t('stage5.dataCenter.overview.totalExpense') }}</h3>
              <div ref="expenseChart" class="chart-container"></div>
            </div>
          </div>

          <!-- 流水明细表格 -->
          <div class="table-section">
            <div class="table-header">
              <h3 class="table-title">{{ t('stage5.dataCenter.overview.revenueDetails') }} ({{ dateRangeLabel }})</h3>
              <el-button type="primary">{{ t('stage5.common.actions.exportDetails') }}</el-button>
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
              <el-table-column prop="paymentMethod" :label="t('stage5.statistics.revenue.paymentMethod')" min-width="120" align="center" />
              <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150" align="center">
                <template #default="{ row }">
                  <span class="amount-bold">¥{{ row.total.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-for="column in dateColumns"
                :key="column.prop"
                :prop="column.prop"
                :label="column.label"
                min-width="150"
                align="center"
              >
                <template #default="{ row }">
                  ¥{{ formatMoneyCell(row, column.prop) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- 款项分类内容 -->
        <div v-if="revenueSubTab === 'category'">
          <!-- 流水统计卡片 -->
          <div class="revenue-stats">
            <div class="stat-card large">
              <div class="stat-label">{{ t('stage5.dataCenter.overview.totalRevenue') }}</div>
              <div class="stat-value large">¥{{ categoryRevenue.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
              <div class="stat-details">
                <span>{{ t('stage5.dataCenter.overview.totalCollection') }}: ¥{{ categoryIncome.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
                <span>{{ t('stage5.dataCenter.overview.totalExpense') }}: ¥{{ Math.abs(categoryExpense).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
              </div>
            </div>

            <div class="revenue-cards">
              <div class="revenue-card">
                <div class="card-label">{{ t('stage5.dataCenter.overview.regularRevenue') }}</div>
                <div class="card-value">¥{{ normalRevenue.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
              </div>
              <div class="revenue-card">
                <div class="card-label">{{ t('stage5.dataCenter.overview.notesRevenue') }}</div>
                <div class="card-value">¥{{ notesRevenue.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
              </div>
            </div>
          </div>

          <!-- 收款分布和总支出饼图 -->
          <div class="charts-row">
            <div class="chart-card">
              <h3 class="chart-title">{{ t('stage5.dataCenter.overview.collectionDistribution') }}</h3>
              <div ref="categoryDistChart" class="chart-container"></div>
            </div>
            <div class="chart-card">
              <h3 class="chart-title">{{ t('stage5.dataCenter.overview.totalExpenseDistribution') }}</h3>
              <div ref="categoryExpenseChart" class="chart-container"></div>
            </div>
          </div>

          <!-- 流水明细表格 -->
          <div class="table-section">
            <div class="table-header">
              <h3 class="table-title">{{ t('stage5.dataCenter.overview.revenueDetails') }} ({{ dateRangeLabel }})</h3>
              <el-button type="primary">{{ t('stage5.common.actions.exportDetails') }}</el-button>
            </div>

            <el-table :data="categoryTableData" border stripe class="detail-table">
              <el-table-column prop="paymentMethod" :label="t('stage5.statistics.revenue.paymentMethod')" min-width="120" align="center" />
              <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150" align="center">
                <template #default="{ row }">
                  <span class="amount-bold">¥{{ row.total.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-for="column in dateColumns"
                :key="column.prop"
                :prop="column.prop"
                :label="column.label"
                min-width="150"
                align="center"
              >
                <template #default="{ row }">
                  ¥{{ formatMoneyCell(row, column.prop) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>

      <!-- 渠道汇总内容 -->
      <div v-if="activeTab === 'channel'" class="tab-content">
        <!-- 渠道消费分布和间夜分布 -->
        <div class="charts-row">
          <div class="chart-card">
            <h3 class="chart-title">{{ t('stage5.dataCenter.overview.channelConsumptionDistribution') }} <span class="chart-subtitle">{{ t('stage5.dataCenter.overview.statsConsumption') }}</span></h3>
            <div ref="channelRevenueChart" class="chart-container"></div>
          </div>
          <div class="chart-card">
            <h3 class="chart-title">{{ t('stage5.dataCenter.overview.channelNightsDistribution') }} <span class="chart-subtitle">{{ t('stage5.dataCenter.overview.statsConsumption') }}</span></h3>
            <div ref="channelNightsChart" class="chart-container"></div>
          </div>
        </div>

        <!-- 渠道消费趋势和间夜趋势 -->
        <div class="charts-row">
          <div class="chart-card wide">
            <h3 class="chart-title">{{ t('stage5.dataCenter.overview.channelConsumptionTrend') }} <span class="chart-subtitle">{{ t('stage5.dataCenter.overview.statsConsumption') }}</span></h3>
            <div ref="channelRevenueTrendChart" class="chart-container-large"></div>
          </div>
          <div class="chart-card wide">
            <h3 class="chart-title">{{ t('stage5.dataCenter.overview.channelNightsTrend') }} <span class="chart-subtitle">{{ t('stage5.dataCenter.overview.statsConsumption') }}</span></h3>
            <div ref="channelNightsTrendChart" class="chart-container-large"></div>
          </div>
        </div>

        <!-- 渠道明细表格 -->
        <div class="table-section">
          <div class="table-header">
            <h3 class="table-title">{{ t('stage5.dataCenter.overview.channelDetails') }} ({{ dateRangeLabel }})</h3>
            <el-button type="primary">{{ t('stage5.common.actions.exportDetails') }}</el-button>
          </div>

          <div class="table-tabs">
            <el-button
              v-for="tab in channelTableTabs"
              :key="tab.key"
              :type="channelTableTab === tab.key ? 'primary' : 'default'"
              size="small"
              @click="handleChannelTableTabChange(tab.key)"
            >
              {{ tab.label }}
            </el-button>
          </div>

          <el-table :data="channelTableData" border stripe class="detail-table">
            <el-table-column prop="channel" :label="t('stage5.common.filters.channel')" min-width="120" align="center" />
            <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="150" align="center">
              <template #default="{ row }">
                <span class="amount-bold">{{ row.total }}</span>
              </template>
            </el-table-column>
            <el-table-column
              v-for="column in dateColumns"
              :key="column.prop"
              :prop="column.prop"
              :label="column.label"
              min-width="150"
              align="center"
            >
              <template #default="{ row }">
                {{ row[column.prop] }}
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
            <div class="stat-label">{{ t('stage5.dataCenter.overview.salesTotal') }}</div>
            <div class="stat-value large">¥{{ salesTotal.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
          </div>
        </div>

        <!-- 每日销售额趋势 -->
        <div class="chart-card full-width">
          <h3 class="chart-title">{{ t('stage5.dataCenter.overview.dailySalesStats') }} <span class="chart-subtitle">{{ t('stage5.dataCenter.overview.statsConsumption') }}</span></h3>
          <div ref="salesTrendChart" class="chart-container-xlarge"></div>
        </div>

        <!-- 销售订单明细 -->
        <div class="table-section">
          <div class="table-header">
            <h3 class="table-title">{{ t('stage5.dataCenter.overview.salesOrderDetails') }} ({{ dateRangeLabel }})</h3>
            <el-button type="primary">{{ t('stage5.common.actions.exportDetails') }}</el-button>
          </div>

          <div class="search-section">
            <el-input
              v-model="searchKeyword"
              :placeholder="t('stage5.dataCenter.overview.searchOrders')"
              clearable
              style="max-width: 400px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="searchChannel" :placeholder="t('stage5.common.filters.all')" clearable style="width: 150px">
              <el-option :label="t('stage5.common.filters.all')" value="" />
              <el-option label="Booking.com" value="booking" />
              <el-option label="Airbnb" value="airbnb" />
            </el-select>
            <el-select v-model="searchGuest" :placeholder="t('stage5.common.filters.all')" clearable style="width: 150px">
              <el-option :label="t('stage5.common.filters.all')" value="" />
            </el-select>
          </div>

          <el-button type="default" size="small" style="margin-bottom: 16px">{{ t('stage5.dataCenter.overview.collapse') }}</el-button>

          <el-table :data="salesTableData" border stripe class="detail-table">
            <el-table-column prop="createdAt" :label="t('stage5.dataCenter.overview.createdAt')" min-width="150" align="center" />
            <el-table-column prop="guestName" :label="t('stage5.dataCenter.overview.name')" min-width="100" align="center" />
            <el-table-column prop="orderNumber" :label="t('stage5.dataCenter.overview.orderChannelNumber')" min-width="200" align="center">
              <template #default="{ row }">
                <div>{{ row.orderNumber }}</div>
                <div style="color: #909399; font-size: 12px">{{ row.channelNumber }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="channel" :label="t('stage5.common.filters.channel')" min-width="120" align="center" />
            <el-table-column prop="customerName" :label="t('stage5.dataCenter.overview.customerName')" min-width="120" align="center" />
            <el-table-column prop="phone" :label="t('stage5.dataCenter.overview.phone')" min-width="120" align="center" />
            <el-table-column prop="amount" :label="t('stage5.dataCenter.overview.orderTotalAmount')" min-width="150" align="center">
              <template #default="{ row }">
                <span class="amount-bold">¥{{ row.amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="table-footer">
            <span class="table-info">{{ t('stage5.dataCenter.overview.recordsTotal', { count: 2 }) }}</span>
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
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Money } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import StatisticsLayout from '../statistics/StatisticsLayout.vue'
import {
  getBusinessOverview,
  getRevenueSummary,
  getChannelSummary,
  getSalesSummary,
  type BusinessOverviewDTO,
  type RevenueSummaryDTO,
  type ChannelSummaryDTO,
  type SalesSummaryDTO
} from '@/api/statistics'
import {
  addDaysToYmd,
  formatYmdMonthDay,
  getStoreTodayYmd,
  getYmdMonthStart,
  getYmdWeekStart,
  parseYmdAsUtcDate,
} from '@/utils/storeDateTime'

const { t, locale } = useI18n()

const BUSINESS_CATEGORY_KEYS = {
  roomFee: 'roomFee',
  checkoutRefund: 'checkoutRefund',
  roomService: 'roomService',
  deposit: 'deposit',
} as const

const PAYMENT_METHOD_KEYS = {
  bookingCollection: 'bookingCollection',
  airbnbCollection: 'airbnbCollection',
} as const

const REVENUE_CATEGORY_KEYS = {
  regularRevenue: 'regularRevenue',
  arMismatchRevenue: 'arMismatchRevenue',
  notesRevenue: 'notesRevenue',
} as const

type LocalizedKey = (typeof BUSINESS_CATEGORY_KEYS)[keyof typeof BUSINESS_CATEGORY_KEYS]
  | (typeof PAYMENT_METHOD_KEYS)[keyof typeof PAYMENT_METHOD_KEYS]
  | (typeof REVENUE_CATEGORY_KEYS)[keyof typeof REVENUE_CATEGORY_KEYS]

const BUSINESS_CATEGORY_ALIASES: Record<string, (typeof BUSINESS_CATEGORY_KEYS)[keyof typeof BUSINESS_CATEGORY_KEYS]> = {
  roomfee: BUSINESS_CATEGORY_KEYS.roomFee,
  room_fee: BUSINESS_CATEGORY_KEYS.roomFee,
  '房费': BUSINESS_CATEGORY_KEYS.roomFee,
  '房費': BUSINESS_CATEGORY_KEYS.roomFee,
  '宿泊費': BUSINESS_CATEGORY_KEYS.roomFee,
  deposit: BUSINESS_CATEGORY_KEYS.deposit,
  '押金': BUSINESS_CATEGORY_KEYS.deposit,
  'デポジット': BUSINESS_CATEGORY_KEYS.deposit,
  checkoutrefund: BUSINESS_CATEGORY_KEYS.checkoutRefund,
  checkout_refund: BUSINESS_CATEGORY_KEYS.checkoutRefund,
  checkoutfee: BUSINESS_CATEGORY_KEYS.checkoutRefund,
  checkout_fee: BUSINESS_CATEGORY_KEYS.checkoutRefund,
  '退房金': BUSINESS_CATEGORY_KEYS.checkoutRefund,
  '退房費': BUSINESS_CATEGORY_KEYS.checkoutRefund,
  'チェックアウト返金': BUSINESS_CATEGORY_KEYS.checkoutRefund,
  roomservice: BUSINESS_CATEGORY_KEYS.roomService,
  room_service: BUSINESS_CATEGORY_KEYS.roomService,
  '餐食/客房消费': BUSINESS_CATEGORY_KEYS.roomService,
  '餐食/客房消費': BUSINESS_CATEGORY_KEYS.roomService,
  '客房消费': BUSINESS_CATEGORY_KEYS.roomService,
  '客房消費': BUSINESS_CATEGORY_KEYS.roomService,
  'ルームサービス': BUSINESS_CATEGORY_KEYS.roomService,
}

const PAYMENT_METHOD_ALIASES: Record<string, (typeof PAYMENT_METHOD_KEYS)[keyof typeof PAYMENT_METHOD_KEYS]> = {
  bookingcollection: PAYMENT_METHOD_KEYS.bookingCollection,
  booking_collection: PAYMENT_METHOD_KEYS.bookingCollection,
  booking: PAYMENT_METHOD_KEYS.bookingCollection,
  'booking.com': PAYMENT_METHOD_KEYS.bookingCollection,
  'booking.com收款': PAYMENT_METHOD_KEYS.bookingCollection,
  'booking收款': PAYMENT_METHOD_KEYS.bookingCollection,
  'booking.com入金': PAYMENT_METHOD_KEYS.bookingCollection,
  airbnbcollection: PAYMENT_METHOD_KEYS.airbnbCollection,
  airbnb_collection: PAYMENT_METHOD_KEYS.airbnbCollection,
  airbnb: PAYMENT_METHOD_KEYS.airbnbCollection,
  'airbnb收款': PAYMENT_METHOD_KEYS.airbnbCollection,
  'airbnb入金': PAYMENT_METHOD_KEYS.airbnbCollection,
}

const REVENUE_CATEGORY_ALIASES: Record<string, (typeof REVENUE_CATEGORY_KEYS)[keyof typeof REVENUE_CATEGORY_KEYS]> = {
  regularrevenue: REVENUE_CATEGORY_KEYS.regularRevenue,
  regular_revenue: REVENUE_CATEGORY_KEYS.regularRevenue,
  normalrevenue: REVENUE_CATEGORY_KEYS.regularRevenue,
  normal_revenue: REVENUE_CATEGORY_KEYS.regularRevenue,
  '常规流水': REVENUE_CATEGORY_KEYS.regularRevenue,
  '常規流水': REVENUE_CATEGORY_KEYS.regularRevenue,
  '通常流水': REVENUE_CATEGORY_KEYS.regularRevenue,
  armismatchrevenue: REVENUE_CATEGORY_KEYS.arMismatchRevenue,
  ar_mismatch_revenue: REVENUE_CATEGORY_KEYS.arMismatchRevenue,
  'ar收错流水': REVENUE_CATEGORY_KEYS.arMismatchRevenue,
  'ar收錯流水': REVENUE_CATEGORY_KEYS.arMismatchRevenue,
  notesrevenue: REVENUE_CATEGORY_KEYS.notesRevenue,
  notes_revenue: REVENUE_CATEGORY_KEYS.notesRevenue,
  '记一笔流水': REVENUE_CATEGORY_KEYS.notesRevenue,
  '記一筆流水': REVENUE_CATEGORY_KEYS.notesRevenue,
}

interface DateColumn {
  prop: string
  label: string
  date: string
}

type DynamicAmountRow = Record<string, string | number>

const activeTab = ref('business')
const dateType = ref('today')
const todayYmd = getStoreTodayYmd()
const startDate = ref(todayYmd)
const endDate = ref(todayYmd)
const loading = ref(false)

const dateRangeLabel = computed(() =>
  t('stage5.common.date.dateRange', { start: startDate.value, end: endDate.value }),
)

const normalizeLabel = (value: string) => value.trim().replace(/\s+/g, '').toLowerCase()

const parseDateValue = (value: string) => {
  const parts = value.split('-').map(Number)
  if (parts.length !== 3 || parts.some((part) => Number.isNaN(part))) {
    return null
  }
  return parseYmdAsUtcDate(value)
}

const getDateColumnProp = (date: string) => `date_${date.replace(/-/g, '')}`

const dateColumns = computed<DateColumn[]>(() => {
  const start = parseDateValue(startDate.value)
  const end = parseDateValue(endDate.value)
  if (!start || !end || start.getTime() > end.getTime()) {
    return []
  }

  const columns: DateColumn[] = []
  const current = new Date(start)
  while (current.getTime() <= end.getTime()) {
    const date = current.toISOString().slice(0, 10)
    const { month, day } = formatYmdMonthDay(date)
    columns.push({
      prop: getDateColumnProp(date),
      label: t('stage5.common.date.monthDay', { month: Number(month), day: Number(day) }),
      date,
    })
    current.setUTCDate(current.getUTCDate() + 1)
  }
  return columns
})

const createEmptyDateAmounts = (): DynamicAmountRow => {
  const row: DynamicAmountRow = {}
  dateColumns.value.forEach((column) => {
    row[column.prop] = 0
  })
  return row
}

const createDailyAmountCells = (dailyAmounts: { date: string; amount: number }[] = []) => {
  const row = createEmptyDateAmounts()
  dailyAmounts.forEach((dailyAmount) => {
    const prop = getDateColumnProp(dailyAmount.date)
    if (prop in row) {
      row[prop] = dailyAmount.amount || 0
    }
  })
  return row
}

const createSingleDateAmountCells = (amount: number | string) => {
  const row = createEmptyDateAmounts()
  const firstColumn = dateColumns.value[0]
  if (firstColumn) {
    row[firstColumn.prop] = amount
  }
  return row
}

const formatMoneyCell = (row: DynamicAmountRow, prop: string) => {
  const value = Number(row[prop] || 0)
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const resolveLabelKey = <T extends LocalizedKey>(value: string, aliases: Record<string, T>) => {
  const normalized = normalizeLabel(value || '')
  return aliases[normalized]
}

const translateBusinessCategory = (category: string) => {
  const key = resolveLabelKey(category, BUSINESS_CATEGORY_ALIASES)
  if (key === BUSINESS_CATEGORY_KEYS.roomFee) return t('stage5.statistics.common.roomFee')
  if (key === BUSINESS_CATEGORY_KEYS.checkoutRefund)
    return t('stage5.dataCenter.overview.checkoutRefund')
  if (key === BUSINESS_CATEGORY_KEYS.roomService)
    return t('stage5.dataCenter.overview.roomService')
  if (key === BUSINESS_CATEGORY_KEYS.deposit)
    return t('stage5.statistics.common.deposit')
  return category
}

const translatePaymentMethod = (method: string) => {
  const key = resolveLabelKey(method, PAYMENT_METHOD_ALIASES)
  if (key === PAYMENT_METHOD_KEYS.bookingCollection)
    return t('stage5.dataCenter.overview.bookingCollection')
  if (key === PAYMENT_METHOD_KEYS.airbnbCollection)
    return t('stage5.dataCenter.overview.airbnbCollection')
  return method
}

const translateRevenueCategory = (category: string) => {
  const key = resolveLabelKey(category, REVENUE_CATEGORY_ALIASES)
  if (key === REVENUE_CATEGORY_KEYS.regularRevenue)
    return t('stage5.dataCenter.overview.regularRevenue')
  if (key === REVENUE_CATEGORY_KEYS.arMismatchRevenue)
    return t('stage5.dataCenter.overview.arMismatchRevenue')
  if (key === REVENUE_CATEGORY_KEYS.notesRevenue)
    return t('stage5.dataCenter.overview.notesRevenue')
  return category
}

// 营业概况相关数据
const totalRevenue = ref(181196.45)
const roomFee = ref(154256.45)
const deposit = ref(0.00)
const checkout = ref(0.00)
const roomService = ref(26940.00)

interface BusinessDetailItem {
  category: string
  total: number
  [key: string]: string | number
}

const businessDetailData = ref<BusinessDetailItem[]>([
  {
    category: translateBusinessCategory(BUSINESS_CATEGORY_KEYS.roomFee),
    total: 154256.45,
    ...createSingleDateAmountCells(154256.45),
  },
  {
    category: translateBusinessCategory(BUSINESS_CATEGORY_KEYS.checkoutRefund),
    total: 0.00,
    ...createSingleDateAmountCells(0.00),
  },
  {
    category: translateBusinessCategory(BUSINESS_CATEGORY_KEYS.roomService),
    total: 26940.00,
    ...createSingleDateAmountCells(26940.00),
  },
  {
    category: translateBusinessCategory(BUSINESS_CATEGORY_KEYS.deposit),
    total: 0.00,
    ...createSingleDateAmountCells(0.00),
  },
])

// 流水汇总相关数据
const revenueSubTab = ref('payment')
const revenueTableTab = ref('payment-method')
const revenueTotal = ref(276793.14)
const splitAccount = ref(276793.14)
const actualReceived = ref(0.00)
const bookingRevenue = ref(182126.14)
const airbnbRevenue = ref(94667.00)

const revenueTableTabs = computed(() => [
  { key: 'payment-method', label: t('stage5.statistics.revenue.paymentMethod') },
  { key: 'room-fee', label: t('stage5.dataCenter.overview.roomFeeSource') },
])

const revenueTableData = ref([
  {
    paymentMethod: translatePaymentMethod(PAYMENT_METHOD_KEYS.bookingCollection),
    total: 182126.14,
    ...createSingleDateAmountCells(182126.14),
  },
  {
    paymentMethod: translatePaymentMethod(PAYMENT_METHOD_KEYS.airbnbCollection),
    total: 94667.00,
    ...createSingleDateAmountCells(94667.00),
  },
])

// 款项分类相关数据
const categoryRevenue = ref(390396.66) // 总流水
const categoryIncome = ref(481398.66) // 总收款
const categoryExpense = ref(-91002.00) // 总支出
const normalRevenue = ref(390396.66) // 常规流水
const arRevenue = ref(0.00) // AR收错流水
const notesRevenue = ref(0.00) // 记一笔流水

const categoryTableData = ref([
  {
    paymentMethod: translateRevenueCategory(REVENUE_CATEGORY_KEYS.regularRevenue),
    total: 390396.66,
    ...createSingleDateAmountCells(390396.66),
  },
  {
    paymentMethod: translateRevenueCategory(REVENUE_CATEGORY_KEYS.arMismatchRevenue),
    total: 0.00,
    ...createSingleDateAmountCells(0.00),
  },
  {
    paymentMethod: translateRevenueCategory(REVENUE_CATEGORY_KEYS.notesRevenue),
    total: 0.00,
    ...createSingleDateAmountCells(0.00),
  },
])

// 渠道汇总相关数据
const channelTableTab = ref('channel-fee')
const channelTableTabs = computed(() => [
  { key: 'channel-fee', label: t('stage5.dataCenter.overview.channelConsumptionDistribution') },
  { key: 'channel-nights', label: t('stage5.statistics.channel.nightsDetails') },
])

// 当前显示的表格数据(根据tab切换)
const channelTableData = ref<any[]>([])

// 销售汇总相关数据
const salesTotal = ref(59054.65)
const searchKeyword = ref('')
const searchChannel = ref('')
const searchGuest = ref('')

const salesTableData = ref([
  {
    createdAt: '2025/11/08 14:37:33',
    guestName: '-',
    orderNumber: '1968094745265878529',
    channelNumber: '648615527',
    channel: 'Booking.com',
    customerName: 'HOSHINO ABE',
    phone: '+81 9082460244',
    amount: 20394.65
  },
  {
    createdAt: '2025/11/08 07:16:40',
    guestName: '-',
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
const categoryDistChart = ref<HTMLDivElement>()
const categoryExpenseChart = ref<HTMLDivElement>()
const channelRevenueChart = ref<HTMLDivElement>()
const channelNightsChart = ref<HTMLDivElement>()
const channelRevenueTrendChart = ref<HTMLDivElement>()
const channelNightsTrendChart = ref<HTMLDivElement>()
const salesTrendChart = ref<HTMLDivElement>()

let businessPie: ECharts | null = null
let businessBar: ECharts | null = null
let revenueDist: ECharts | null = null
let expense: ECharts | null = null
let categoryDistChart_instance: ECharts | null = null
let categoryExpenseChart_instance: ECharts | null = null
let channelRevenue: ECharts | null = null
let channelNights: ECharts | null = null
let channelRevenueTrend: ECharts | null = null
let channelNightsTrend: ECharts | null = null
let salesTrend: ECharts | null = null

// ==================== 数据加载函数 ====================

/**
 * 加载营业概况数据
 */
const loadBusinessOverview = async () => {
  // 检查日期参数是否有效
  if (!startDate.value || !endDate.value) {
    console.warn(t('stage5.dataCenter.overview.invalidDateRange'))
    return
  }

  try {
    loading.value = true
    const response = await getBusinessOverview({
      startDate: startDate.value,
      endDate: endDate.value
    })

    if (response.success && response.data) {
      const data = response.data

      // 更新统计卡片数据
      totalRevenue.value = data.totalRevenue
      roomFee.value = data.roomFee
      deposit.value = data.deposit
      checkout.value = data.checkoutFee
      roomService.value = data.roomServiceFee

      // 更新表格数据
      businessDetailData.value = data.consumptionDetails.map(detail => ({
        category: translateBusinessCategory(detail.category),
        total: detail.total,
        ...createDailyAmountCells(detail.dailyAmounts),
      }))

      // 重新初始化图表
      await nextTick()
      initBusinessPieChart(data)
      initBusinessBarChart(data)
    } else {
      ElMessage.error(response.message || t('stage5.dataCenter.overview.loadBusinessFailed'))
    }
  } catch (error) {
    console.error(t('stage5.dataCenter.overview.loadBusinessFailed'), error)
    ElMessage.error(t('stage5.dataCenter.overview.loadBusinessFailed'))
  } finally {
    loading.value = false
  }
}

/**
 * 加载流水汇总数据
 */
const loadRevenueSummary = async () => {
  // 检查日期参数是否有效
  if (!startDate.value || !endDate.value) {
    console.warn(t('stage5.dataCenter.overview.invalidDateRange'))
    return
  }

  try {
    loading.value = true
    const response = await getRevenueSummary({
      startDate: startDate.value,
      endDate: endDate.value
    })

    if (response.success && response.data) {
      const data = response.data

      // 更新统计数据
      revenueTotal.value = data.totalRevenue
      splitAccount.value = data.splitAccount
      actualReceived.value = data.actualReceived

      // 更新表格数据 - 支付方式
      revenueTableData.value = data.paymentMethodStats.map(stat => ({
        paymentMethod: translatePaymentMethod(stat.paymentMethod),
        total: stat.amount,
        ...createSingleDateAmountCells(stat.amount),
      }))

      // 更新表格数据 - 款项分类
      categoryRevenue.value = data.totalRevenue
      categoryIncome.value = data.totalRevenue
      categoryExpense.value = 0
      normalRevenue.value = data.totalRevenue
      notesRevenue.value = 0

      categoryTableData.value = data.categoryStats.map(stat => ({
        paymentMethod: translateRevenueCategory(stat.category),
        total: stat.amount,
        ...createSingleDateAmountCells(stat.amount),
      }))

      // 重新初始化图表
      await nextTick()
      if (revenueSubTab.value === 'payment') {
        initRevenueDistChart(data)
        initExpenseChart()
      } else {
        initCategoryDistChart()
        initCategoryExpenseChart()
      }
    } else {
      ElMessage.error(response.message || t('stage5.dataCenter.overview.loadRevenueFailed'))
    }
  } catch (error) {
    console.error(t('stage5.dataCenter.overview.loadRevenueFailed'), error)
    ElMessage.error(t('stage5.dataCenter.overview.loadRevenueFailed'))
  } finally {
    loading.value = false
  }
}

/**
 * 加载渠道汇总数据
 */
const loadChannelSummary = async () => {
  // 检查日期参数是否有效
  if (!startDate.value || !endDate.value) {
    console.warn(t('stage5.dataCenter.overview.invalidDateRange'))
    return
  }

  try {
    loading.value = true
    const response = await getChannelSummary({
      startDate: startDate.value,
      endDate: endDate.value
    })

    if (response.success && response.data) {
      const data = response.data

      // 更新表格数据
      const channelFeeData = data.channelDetails.map(detail => ({
        channel: detail.channelName,
        total: `¥${(detail.revenue || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`,
        ...createSingleDateAmountCells(
          `¥${(detail.revenue || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`,
        ),
      }))

      const channelNightsData = data.channelDetails.map(detail => ({
        channel: detail.channelName,
        total: detail.roomNights || 0,
        ...createSingleDateAmountCells(detail.roomNights || 0),
      }))

      if (channelTableTab.value === 'channel-fee') {
        channelTableData.value = channelFeeData
      } else {
        channelTableData.value = channelNightsData
      }

      // 重新初始化图表
      await nextTick()
      initChannelRevenueChart(data)
      initChannelNightsChart(data)
      initChannelRevenueTrendChart(data)
      initChannelNightsTrendChart(data)
    } else {
      ElMessage.error(response.message || t('stage5.dataCenter.overview.loadChannelFailed'))
    }
  } catch (error) {
    console.error(t('stage5.dataCenter.overview.loadChannelFailed'), error)
    ElMessage.error(t('stage5.dataCenter.overview.loadChannelFailed'))
  } finally {
    loading.value = false
  }
}

/**
 * 加载销售汇总数据
 */
const loadSalesSummary = async () => {
  // 检查日期参数是否有效
  if (!startDate.value || !endDate.value) {
    console.warn(t('stage5.dataCenter.overview.invalidDateRange'))
    return
  }

  try {
    loading.value = true
    const response = await getSalesSummary({
      startDate: startDate.value,
      endDate: endDate.value,
      keyword: searchKeyword.value || undefined
    })

    if (response.success && response.data) {
      const data = response.data

      // 更新统计数据
      salesTotal.value = data.totalSales

      // 更新表格数据
      salesTableData.value = data.orderDetails.map(order => ({
        createdAt: order.createdAt,
        guestName: order.guestName,
        orderNumber: order.orderNumber,
        channelNumber: order.channelNumber,
        channel: order.channelName,
        customerName: order.customerName,
        phone: order.phone,
        amount: order.amount
      }))

      // 重新初始化图表
      await nextTick()
      initSalesTrendChart(data)
    } else {
      ElMessage.error(response.message || t('stage5.dataCenter.overview.loadSalesFailed'))
    }
  } catch (error) {
    console.error(t('stage5.dataCenter.overview.loadSalesFailed'), error)
    ElMessage.error(t('stage5.dataCenter.overview.loadSalesFailed'))
  } finally {
    loading.value = false
  }
}

/**
 * 根据当前标签页加载对应数据
 */
const loadCurrentTabData = () => {
  if (activeTab.value === 'business') {
    loadBusinessOverview()
  } else if (activeTab.value === 'revenue') {
    loadRevenueSummary()
  } else if (activeTab.value === 'channel') {
    loadChannelSummary()
  } else if (activeTab.value === 'sales') {
    loadSalesSummary()
  }
}

// ==================== 图表初始化函数 ====================

// 初始化营业概况饼图
const initBusinessPieChart = (data?: BusinessOverviewDTO) => {
  if (!businessPieChart.value) return

  // 销毁旧实例避免重复初始化
  if (businessPie) {
    businessPie.dispose()
  }

  businessPie = echarts.init(businessPieChart.value)

  // 使用API数据或默认数据
  const pieChartData = data ? data.categoryDistribution.map(item => ({
    value: item.value,
    name: translateBusinessCategory(item.category),
    percentage: `${item.percentage.toFixed(2)}%`
  })) : [
    { value: 0, name: t('stage5.statistics.common.roomFee'), percentage: '0%' },
    { value: 0, name: t('stage5.statistics.common.deposit'), percentage: '0%' },
    { value: 0, name: t('stage5.dataCenter.overview.checkoutRefund'), percentage: '0%' },
    { value: 0, name: t('stage5.dataCenter.overview.roomService'), percentage: '0%' }
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
        name: t('stage5.dataCenter.overview.spendCategoryDistribution'),
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
const MAX_BUSINESS_BAR_COUNT = 35

const formatBusinessTrendDate = (dateText: string): string => {
  if (!dateText) return ''

  const dateParts = dateText.split('-')
  if (dateParts.length === 3) {
    return `${dateParts[1]}/${dateParts[2]}`
  }

  const date = new Date(dateText)
  if (Number.isNaN(date.getTime())) {
    return dateText
  }

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}/${day}`
}

type BusinessTrendBarItem = {
  label: string
  roomFee: number
  checkoutFee: number
  roomServiceFee: number
  deposit: number
}

const buildBusinessTrendBars = (
  trend: BusinessOverviewDTO['consumptionTrend'],
  maxBarCount = MAX_BUSINESS_BAR_COUNT
): BusinessTrendBarItem[] => {
  if (!trend.length) return []

  const sortedTrend = [...trend].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())

  if (sortedTrend.length <= maxBarCount) {
    return sortedTrend.map((item) => ({
      label: formatBusinessTrendDate(item.date),
      roomFee: item.roomFee || 0,
      checkoutFee: item.checkoutFee || 0,
      roomServiceFee: item.roomServiceFee || 0,
      deposit: item.deposit || 0
    }))
  }

  const bucketSize = Math.ceil(sortedTrend.length / maxBarCount)
  const bars: BusinessTrendBarItem[] = []

  for (let index = 0; index < sortedTrend.length; index += bucketSize) {
    const bucket = sortedTrend.slice(index, index + bucketSize)
    if (!bucket.length) continue

    const startLabel = formatBusinessTrendDate(bucket[0].date)
    const endLabel = formatBusinessTrendDate(bucket[bucket.length - 1].date)
    const label = startLabel === endLabel ? startLabel : `${startLabel}~${endLabel}`

    bars.push({
      label,
      roomFee: bucket.reduce((sum, item) => sum + (item.roomFee || 0), 0),
      checkoutFee: bucket.reduce((sum, item) => sum + (item.checkoutFee || 0), 0),
      roomServiceFee: bucket.reduce((sum, item) => sum + (item.roomServiceFee || 0), 0),
      deposit: bucket.reduce((sum, item) => sum + (item.deposit || 0), 0)
    })
  }

  return bars
}

const initBusinessBarChart = (data?: BusinessOverviewDTO) => {
  if (!businessBarChart.value) return

  // 销毁旧实例避免重复初始化
  if (businessBar) {
    businessBar.dispose()
  }

  businessBar = echarts.init(businessBarChart.value)

  // 直接展示柱图，当天数过多时按日期区间聚合，避免标签拥挤
  const barData = buildBusinessTrendBars(data?.consumptionTrend || [])
  const dates = barData.map(item => item.label)
  const roomFeeData = barData.map(item => item.roomFee)
  const checkoutFeeData = barData.map(item => item.checkoutFee)
  const roomServiceData = barData.map(item => item.roomServiceFee)
  const depositData = barData.map(item => item.deposit)
  const labelRotate = dates.length > 20 ? 45 : dates.length > 12 ? 30 : 0

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        if (!params || !params.length) return ''
        let result = `${params[0].axisValue}<br/>`
        let total = 0
        params.forEach((item: any) => {
          result += `${item.marker}${item.seriesName}: ¥${item.value.toFixed(2)}<br/>`
          total += item.value
        })
        result += `${t('stage5.statistics.common.summary')}: ¥${total.toFixed(2)}`
        return result
      }
    },
    legend: {
      data: [
        t('stage5.statistics.common.roomFee'),
        t('stage5.dataCenter.overview.checkoutRefund'),
        t('stage5.dataCenter.overview.roomService'),
        t('stage5.statistics.common.deposit'),
      ],
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
        rotate: labelRotate
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
        name: t('stage5.statistics.common.roomFee'),
        type: 'bar',
        stack: 'total',
        barMaxWidth: 36,
        data: roomFeeData
      },
      {
        name: t('stage5.dataCenter.overview.checkoutRefund'),
        type: 'bar',
        stack: 'total',
        data: checkoutFeeData
      },
      {
        name: t('stage5.dataCenter.overview.roomService'),
        type: 'bar',
        stack: 'total',
        data: roomServiceData
      },
      {
        name: t('stage5.statistics.common.deposit'),
        type: 'bar',
        stack: 'total',
        data: depositData
      }
    ],
    color: ['#5b8ff9', '#ffd93d', '#f9c94a', '#ff6b6b']
  }

  businessBar.setOption(option)
}

// 初始化流水分布饼图
const initRevenueDistChart = (data?: RevenueSummaryDTO) => {
  if (!revenueDistChart.value) return

  if (revenueDist) revenueDist.dispose()
  revenueDist = echarts.init(revenueDistChart.value)

  // 使用API数据或默认数据
  const chartData = data ? data.paymentMethodStats.map(stat => ({
    value: stat.amount,
    name: translatePaymentMethod(stat.paymentMethod)
  })) : []

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
          [t('stage5.dataCenter.overview.bookingCollection')]: '65.8%',
          [t('stage5.dataCenter.overview.airbnbCollection')]: '34.2%',
        }
        return `${name}  ${percentages[name] || ''}`
      }
    },
    series: [
      {
        name: t('stage5.dataCenter.overview.collectionDistribution'),
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['30%', '50%'],
        data: chartData,
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

  if (expense) expense.dispose()
  expense = echarts.init(expenseChart.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    series: [
      {
        name: t('stage5.dataCenter.overview.totalExpense'),
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: [
          { value: 0, name: t('stage5.dataCenter.overview.totalExpense') }
        ],
        itemStyle: {
          color: '#e5e5e5'
        },
        label: {
          show: true,
          position: 'center',
          formatter: `${t('stage5.dataCenter.overview.totalExpense')}\n¥0.00`,
          fontSize: 16
        }
      }
    ]
  }

  expense.setOption(option)
}

// 初始化款项分类收款分布饼图
const initCategoryDistChart = () => {
  if (!categoryDistChart.value) return

  if (categoryDistChart_instance) categoryDistChart_instance.dispose()
  categoryDistChart_instance = echarts.init(categoryDistChart.value)

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
          [t('stage5.dataCenter.overview.regularRevenue')]: '100%',
        }
        return `${name}  ${percentages[name] || ''}`
      }
    },
    series: [
      {
        name: t('stage5.dataCenter.overview.collectionDistribution'),
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['30%', '50%'],
        data: [
          { value: 481398.66, name: t('stage5.dataCenter.overview.regularRevenue') }
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
    color: ['#5470c6']
  }

  categoryDistChart_instance.setOption(option)
}

// 初始化款项分类总支出饼图
const initCategoryExpenseChart = () => {
  if (!categoryExpenseChart.value) return

  if (categoryExpenseChart_instance) categoryExpenseChart_instance.dispose()
  categoryExpenseChart_instance = echarts.init(categoryExpenseChart.value)

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
          [t('stage5.dataCenter.overview.regularRevenue')]: '100%',
        }
        return `${name}  ${percentages[name] || ''}`
      }
    },
    series: [
      {
        name: t('stage5.dataCenter.overview.totalExpense'),
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['30%', '50%'],
        data: [
          { value: 91002.00, name: t('stage5.dataCenter.overview.regularRevenue') }
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
    color: ['#e5e5e5']
  }

  categoryExpenseChart_instance.setOption(option)
}

// 初始化渠道消费分布饼图
const initChannelRevenueChart = (data?: ChannelSummaryDTO) => {
  if (!channelRevenueChart.value) return

  if (channelRevenue) channelRevenue.dispose()
  channelRevenue = echarts.init(channelRevenueChart.value)

  // 使用API数据或默认数据
  const chartData = data ? data.revenueDistribution.map(item => ({
    value: item.value,
    name: item.channelName
  })) : []

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
        name: t('stage5.dataCenter.overview.channelConsumptionDistribution'),
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['30%', '50%'],
        data: chartData,
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
const initChannelNightsChart = (data?: ChannelSummaryDTO) => {
  if (!channelNightsChart.value) return

  if (channelNights) channelNights.dispose()
  channelNights = echarts.init(channelNightsChart.value)

  // 使用API数据或默认数据
  const chartData = data ? data.nightsDistribution.map(item => ({
    value: item.value,
    name: item.channelName
  })) : []

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
        name: t('stage5.dataCenter.overview.channelNightsDistribution'),
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['30%', '50%'],
        data: chartData,
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
const initChannelRevenueTrendChart = (data?: ChannelSummaryDTO) => {
  if (!channelRevenueTrendChart.value) return

  if (channelRevenueTrend) channelRevenueTrend.dispose()
  channelRevenueTrend = echarts.init(channelRevenueTrendChart.value)

  // 使用API数据或默认数据
  const dates = data ? data.revenueTrend.map(item => item.date) : []
  const seriesData: any[] = []

  if (data && data.revenueTrend.length > 0) {
    // 获取所有渠道名称
    const channelNames = new Set<string>()
    data.revenueTrend.forEach(trend => {
      trend.channels.forEach(ch => channelNames.add(ch.channelName))
    })

    // 为每个渠道创建series
    channelNames.forEach(channelName => {
      seriesData.push({
        name: channelName,
        type: 'line',
        smooth: true,
        data: data.revenueTrend.map(trend => {
          const channelData = trend.channels.find(ch => ch.channelName === channelName)
          return channelData ? channelData.value : 0
        }),
        lineStyle: { width: 2 }
      })
    })
  }

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: Array.from(new Set(seriesData.map(s => s.name))),
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
    series: seriesData,
    color: ['#5470c6', '#fac858']
  }

  channelRevenueTrend.setOption(option)
}

// 初始化渠道间夜趋势柱状图
const initChannelNightsTrendChart = (data?: ChannelSummaryDTO) => {
  if (!channelNightsTrendChart.value) return

  if (channelNightsTrend) channelNightsTrend.dispose()
  channelNightsTrend = echarts.init(channelNightsTrendChart.value)

  // 使用API数据或默认数据
  const dates = data ? data.nightsTrend.map(item => item.date) : []
  const seriesData: any[] = []

  if (data && data.nightsTrend.length > 0) {
    // 获取所有渠道名称
    const channelNames = new Set<string>()
    data.nightsTrend.forEach(trend => {
      trend.channels.forEach(ch => channelNames.add(ch.channelName))
    })

    // 为每个渠道创建series
    channelNames.forEach(channelName => {
      seriesData.push({
        name: channelName,
        type: 'bar',
        stack: 'total',
        data: data.nightsTrend.map(trend => {
          const channelData = trend.channels.find(ch => ch.channelName === channelName)
          return channelData ? channelData.value : 0
        })
      })
    })
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: Array.from(new Set(seriesData.map(s => s.name))),
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
    series: seriesData,
    color: ['#5470c6', '#fac858']
  }

  channelNightsTrend.setOption(option)
}

// 初始化销售趋势折线图
const initSalesTrendChart = (data?: SalesSummaryDTO) => {
  if (!salesTrendChart.value) return

  if (salesTrend) salesTrend.dispose()
  salesTrend = echarts.init(salesTrendChart.value)

  // 使用API数据或默认数据
  const dates = data ? data.dailySalesTrend.map(item => item.date) : []
  const salesData = data ? data.dailySalesTrend.map(item => item.sales) : []

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const param = params[0]
        return `${param.axisValue}<br/>${t('stage5.dataCenter.overview.salesAmount')}: ¥${param.value.toFixed(2)}`
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
        name: t('stage5.dataCenter.overview.salesAmount'),
        type: 'line',
        smooth: true,
        data: salesData,
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

// 流水汇总子标签页切换处理
const handleRevenueSubTabChange = async (tab: string) => {
  revenueSubTab.value = tab
  await nextTick()

  if (tab === 'payment') {
    initRevenueDistChart()
    initExpenseChart()
  } else if (tab === 'category') {
    initCategoryDistChart()
    initCategoryExpenseChart()
  }

  handleResize()
}

// 渠道表格标签切换处理
const handleChannelTableTabChange = (tab: string) => {
  channelTableTab.value = tab
  // 重新加载渠道汇总数据以更新表格
  loadChannelSummary()
}

// 标签页切换处理
const handleTabChange = async () => {
  await nextTick()

  if (activeTab.value === 'business') {
    initBusinessPieChart()
    initBusinessBarChart()
  } else if (activeTab.value === 'revenue') {
    // 根据当前子标签初始化对应图表
    if (revenueSubTab.value === 'payment') {
      initRevenueDistChart()
      initExpenseChart()
    } else if (revenueSubTab.value === 'category') {
      initCategoryDistChart()
      initCategoryExpenseChart()
    }
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
  categoryDistChart_instance?.resize()
  categoryExpenseChart_instance?.resize()
  channelRevenue?.resize()
  channelNights?.resize()
  channelRevenueTrend?.resize()
  channelNightsTrend?.resize()
  salesTrend?.resize()
}

/**
 * 根据日期类型更新日期范围
 */
const updateDateRange = (type: string) => {
  const today = getStoreTodayYmd()

  switch (type) {
    case 'today':
      startDate.value = today
      endDate.value = today
      break
    case 'yesterday':
      const yesterday = addDaysToYmd(today, -1)
      startDate.value = yesterday
      endDate.value = yesterday
      break
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

// 监听日期类型变化
watch(dateType, (newType) => {
  if (newType) {
    updateDateRange(newType)
  }
})

// 监听日期变化
watch([startDate, endDate], () => {
  loadCurrentTabData()
})

// 监听标签页切换
watch(activeTab, () => {
  loadCurrentTabData()
})

// 语言切换后刷新当前页数据和图表文案
watch(locale, () => {
  loadCurrentTabData()
})

onMounted(() => {
  // 初始化日期为今天
  updateDateRange(dateType.value)

  // 加载初始数据
  loadCurrentTabData()

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  businessPie?.dispose()
  businessBar?.dispose()
  revenueDist?.dispose()
  expense?.dispose()
  categoryDistChart_instance?.dispose()
  categoryExpenseChart_instance?.dispose()
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
