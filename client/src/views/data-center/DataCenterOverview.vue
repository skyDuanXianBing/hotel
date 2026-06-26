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
        <div v-if="activeTab === 'revenue'" class="revenue-filter-tabs">
          <el-button
            :class="{ active: revenueSubTab === 'payment' }"
            @click="handleRevenueSubTabChange('payment')"
          >
            {{ t('stage5.statistics.revenue.paymentMethod') }}
          </el-button>
          <el-button
            :class="{ active: revenueSubTab === 'category' }"
            @click="handleRevenueSubTabChange('category')"
          >
            {{ t('stage5.dataCenter.overview.paymentCategory') }}
          </el-button>
        </div>
      </div>

      <!-- 营业概况内容 -->
      <div v-if="activeTab === 'business'" class="tab-content">
        <!-- 营业概况统计卡片 -->
        <div class="stats-section">
          <div
            v-for="card in businessMetricCards"
            :key="card.key"
            class="stat-card business-stat-card"
            :class="{ primary: card.primary }"
          >
            <div class="stat-card-head">
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-icon">
                <img :src="card.icon" :alt="card.label" class="stat-icon-image" />
              </div>
            </div>
            <div class="stat-value-row">
              <div class="stat-value">¥{{ formatMoney(card.value) }}</div>
              <span class="stat-change" :class="card.changeDirection">
                <span class="stat-change-icon" aria-hidden="true">{{ card.changeIcon }}</span>
                {{ Math.abs(card.changeRate) }}%
              </span>
            </div>
            <div class="stat-previous">
              <span>上个月</span>
              <strong>{{ card.previousText }}</strong>
            </div>
          </div>
        </div>

        <!-- 消费分类分布和住宿消费趋势 -->
        <div class="charts-row business-charts-row">
          <div class="chart-card trend-card">
            <div class="chart-card-header">
              <h3 class="chart-title">营业额趋势</h3>
              <el-select v-model="businessTrendRange" class="business-chart-select">
                <el-option label="本月" value="month" />
                <el-option label="本周" value="week" />
                <el-option label="今天" value="today" />
              </el-select>
            </div>
            <div ref="businessBarChart" class="chart-container business-trend-chart"></div>
          </div>

          <div class="chart-card distribution-card">
            <h3 class="chart-title">消费分布</h3>
            <div ref="businessPieChart" class="chart-container business-distribution-chart"></div>
          </div>
        </div>

        <!-- 住宿消费明细表格 -->
        <div class="table-section business-table-section">
          <div class="table-header">
            <h3 class="table-title">{{ t('stage5.dataCenter.overview.accommodationSpendDetails') }} ({{ dateRangeLabel }})</h3>
            <el-button type="primary" class="business-export-button">{{ t('stage5.common.actions.exportDetails') }}</el-button>
          </div>
          <el-table :data="businessDetailData" class="detail-table business-detail-table">
            <el-table-column prop="category" :label="t('stage5.common.fields.project')" min-width="140" align="center" />
            <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="140" align="center">
              <template #default="{ row }">¥{{ formatMoney(row.total) }}</template>
            </el-table-column>
            <el-table-column
              v-for="column in dateColumns"
              :key="column.prop"
              :prop="column.prop"
              :label="column.label"
              min-width="140"
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
        <!-- 支付方式内容 -->
        <div v-if="revenueSubTab === 'payment'">
          <!-- 流水统计卡片 -->
          <div class="revenue-stats">
            <div class="revenue-summary-card primary">
              <div class="revenue-card-head">
                <div class="revenue-card-label">{{ t('stage5.dataCenter.overview.totalRevenue') }}</div>
                <div class="revenue-card-icon">
                  <img :src="businessCartIcon" :alt="t('stage5.dataCenter.overview.totalRevenue')" />
                </div>
              </div>
              <div class="revenue-card-value">¥{{ formatMoney(revenueTotal) }}</div>
              <div class="revenue-card-details">
                <span>{{ t('stage5.dataCenter.overview.splitAccount') }} ¥{{ formatMoney(splitAccount) }}</span>
                <span>{{ t('stage5.dataCenter.overview.actualReceived') }} ¥{{ formatMoney(actualReceived) }}</span>
              </div>
            </div>

            <div class="revenue-summary-card airbnb-card">
              <div class="revenue-card-head">
                <div class="revenue-card-label">{{ t('stage5.dataCenter.overview.airbnbCollection') }}</div>
                <div class="revenue-card-icon blue">
                  <img :src="businessCartIcon" :alt="t('stage5.dataCenter.overview.airbnbCollection')" />
                </div>
              </div>
              <div class="revenue-card-value dark">¥{{ formatMoney(airbnbRevenue) }}</div>
            </div>

            <div class="revenue-summary-card booking-card">
              <div class="revenue-card-head">
                <div class="revenue-card-label">{{ t('stage5.dataCenter.overview.bookingCollection') }}</div>
                <div class="revenue-card-icon blue">
                  <img :src="businessCartIcon" :alt="t('stage5.dataCenter.overview.bookingCollection')" />
                </div>
              </div>
              <div class="revenue-card-value dark">¥{{ formatMoney(bookingRevenue) }}</div>
            </div>
          </div>

          <!-- 收款分布和总支出饼图 -->
          <div class="charts-row revenue-charts-row">
            <div class="chart-card revenue-chart-card">
              <h3 class="chart-title">{{ t('stage5.dataCenter.overview.collectionDistribution') }}</h3>
              <div ref="revenueDistChart" class="chart-container revenue-donut-chart"></div>
            </div>
            <div class="chart-card revenue-chart-card">
              <h3 class="chart-title">{{ t('stage5.dataCenter.overview.totalExpense') }}</h3>
              <div ref="expenseChart" class="chart-container revenue-donut-chart"></div>
            </div>
          </div>

          <!-- 流水明细表格 -->
          <div class="table-section revenue-table-section">
            <div class="table-header">
              <h3 class="table-title">{{ t('stage5.dataCenter.overview.revenueDetails') }} ({{ dateRangeLabel }})</h3>
              <div class="revenue-table-actions">
                <div class="table-tabs revenue-table-tabs">
                  <el-button
                    v-for="tab in revenueTableTabs"
                    :key="tab.key"
                    :class="{ active: revenueTableTab === tab.key }"
                    @click="revenueTableTab = tab.key"
                  >
                    {{ tab.label }}
                  </el-button>
                </div>
                <el-button type="primary" class="business-export-button">{{ t('stage5.common.actions.exportDetails') }}</el-button>
              </div>
            </div>

            <el-table :data="revenueTableData" class="detail-table business-detail-table revenue-detail-table">
              <el-table-column prop="paymentMethod" :label="t('stage5.statistics.revenue.paymentMethod')" min-width="140" align="center" />
              <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="140" align="center">
                <template #default="{ row }">
                  <span class="amount-bold">¥{{ formatMoney(row.total) }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-for="column in dateColumns"
                :key="column.prop"
                :prop="column.prop"
                :label="column.label"
                min-width="140"
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
            <div class="revenue-summary-card primary">
              <div class="revenue-card-head">
                <div class="revenue-card-label">{{ t('stage5.dataCenter.overview.totalRevenue') }}</div>
                <div class="revenue-card-icon">
                  <img :src="businessCartIcon" :alt="t('stage5.dataCenter.overview.totalRevenue')" />
                </div>
              </div>
              <div class="revenue-card-value">¥{{ formatMoney(categoryRevenue) }}</div>
              <div class="revenue-card-details">
                <span>{{ t('stage5.dataCenter.overview.totalCollection') }} ¥{{ formatMoney(categoryIncome) }}</span>
                <span>{{ t('stage5.dataCenter.overview.totalExpense') }} ¥{{ formatMoney(Math.abs(categoryExpense)) }}</span>
              </div>
            </div>

            <div class="revenue-summary-card">
              <div class="revenue-card-head">
                <div class="revenue-card-label">{{ t('stage5.dataCenter.overview.regularRevenue') }}</div>
                <div class="revenue-card-icon blue">
                  <img :src="businessCartIcon" :alt="t('stage5.dataCenter.overview.regularRevenue')" />
                </div>
              </div>
              <div class="revenue-card-value dark">¥{{ formatMoney(normalRevenue) }}</div>
            </div>

            <div class="revenue-summary-card">
              <div class="revenue-card-head">
                <div class="revenue-card-label">{{ t('stage5.dataCenter.overview.notesRevenue') }}</div>
                <div class="revenue-card-icon blue">
                  <img :src="businessCartIcon" :alt="t('stage5.dataCenter.overview.notesRevenue')" />
                </div>
              </div>
              <div class="revenue-card-value dark">¥{{ formatMoney(notesRevenue) }}</div>
            </div>
          </div>

          <!-- 收款分布和总支出饼图 -->
          <div class="charts-row revenue-charts-row">
            <div class="chart-card revenue-chart-card">
              <h3 class="chart-title">{{ t('stage5.dataCenter.overview.collectionDistribution') }}</h3>
              <div ref="categoryDistChart" class="chart-container revenue-donut-chart"></div>
            </div>
            <div class="chart-card revenue-chart-card">
              <h3 class="chart-title">{{ t('stage5.dataCenter.overview.totalExpenseDistribution') }}</h3>
              <div ref="categoryExpenseChart" class="chart-container revenue-donut-chart"></div>
            </div>
          </div>

          <!-- 流水明细表格 -->
          <div class="table-section revenue-table-section">
            <div class="table-header">
              <h3 class="table-title">{{ t('stage5.dataCenter.overview.revenueDetails') }} ({{ dateRangeLabel }})</h3>
              <div class="revenue-table-actions">
                <div class="table-tabs revenue-table-tabs">
                  <el-button class="active">{{ t('stage5.dataCenter.overview.paymentCategory') }}</el-button>
                </div>
                <el-button type="primary" class="business-export-button">{{ t('stage5.common.actions.exportDetails') }}</el-button>
              </div>
            </div>

            <el-table :data="categoryTableData" class="detail-table business-detail-table revenue-detail-table">
              <el-table-column prop="paymentMethod" :label="t('stage5.statistics.revenue.paymentMethod')" min-width="140" align="center" />
              <el-table-column prop="total" :label="t('stage5.common.fields.total')" min-width="140" align="center">
                <template #default="{ row }">
                  <span class="amount-bold">¥{{ formatMoney(row.total) }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-for="column in dateColumns"
                :key="column.prop"
                :prop="column.prop"
                :label="column.label"
                min-width="140"
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
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import StatisticsLayout from '../statistics/StatisticsLayout.vue'
import businessCartIcon from '@/assets/icons/statistics/business-cart.png'
import businessCheckoutIcon from '@/assets/icons/statistics/business-checkout.png'
import businessCustomerIcon from '@/assets/icons/statistics/business-customer.png'
import businessDepositIcon from '@/assets/icons/statistics/business-deposit.png'
import businessHomeIcon from '@/assets/icons/statistics/business-home.png'
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
import { getAllChannels } from '@/api/channel'
import type { ChannelDTO } from '@/api/channel'
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
const businessTrendRange = ref('month')

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

const dateRangeLabel = computed(() =>
  t('stage5.common.date.dateRange', { start: startDate.value, end: endDate.value }),
)

const normalizeLabel = (value: string) => value.trim().replace(/\s+/g, '').toLowerCase()

const CHANNEL_FALLBACK_COLORS: Record<string, string> = {
  airbnb: '#ff385c',
  booking: '#003b95',
  bookingcom: '#003b95',
  agoda: '#bb2bd9',
  trip: '#1f66ff',
  tripcom: '#1f66ff',
  tujia: '#f16a2f',
  途家: '#f16a2f',
}

const normalizeChannelColorKey = (name: string) =>
  normalizeLabel(name).replace(/\.com/g, 'com').replace(/[^a-z0-9\u4e00-\u9fa5]/g, '')

const channelColorOverrides = ref(new Map<string, string>())

const addChannelColorOverride = (map: Map<string, string>, value: string | undefined, color: string) => {
  if (!value || !color) return
  const normalized = normalizeChannelColorKey(value)
  if (normalized) map.set(normalized, color)
}

const buildChannelColorOverrides = (channels: ChannelDTO[]) => {
  const map = new Map<string, string>()
  channels.forEach((channel) => {
    if (!channel.color) return
    addChannelColorOverride(map, channel.name, channel.color)
    addChannelColorOverride(map, channel.code, channel.color)
  })
  channelColorOverrides.value = map
}

const getChannelColor = (name: string, index = 0) => {
  const normalized = normalizeChannelColorKey(name)
  const configuredKey = Array.from(channelColorOverrides.value.keys()).find(
    (key) => normalized.includes(key) || key.includes(normalized),
  )
  if (configuredKey) return channelColorOverrides.value.get(configuredKey) as string

  const matchedKey = Object.keys(CHANNEL_FALLBACK_COLORS).find((key) => normalized.includes(key))
  const fallbackPalette = ['#168bf8', '#ff385c', '#bb2bd9', '#1f66ff', '#f16a2f', '#8ec5ff']
  return matchedKey ? CHANNEL_FALLBACK_COLORS[matchedKey] : fallbackPalette[index % fallbackPalette.length]
}

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

const formatMoney = (value: number | string) =>
  Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })

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

type BusinessMetricKey = 'totalRevenue' | 'roomFee' | 'deposit' | 'checkout' | 'roomService'

const BUSINESS_METRIC_COMPARISON: Record<
  BusinessMetricKey,
  { previousMonthAmount: number; changeRate: number }
> = {
  // TODO(backend): replace with previous-period metrics when the statistics API exposes them.
  totalRevenue: { previousMonthAmount: 234568.33, changeRate: 14.9 },
  roomFee: { previousMonthAmount: 89, changeRate: 14.9 },
  deposit: { previousMonthAmount: 89, changeRate: 14.9 },
  checkout: { previousMonthAmount: 89, changeRate: 14.9 },
  roomService: { previousMonthAmount: 89, changeRate: 14.9 },
}

const getChangeDirection = (changeRate: number) => (changeRate >= 0 ? 'up' : 'down')
const getChangeIcon = (changeRate: number) => (changeRate >= 0 ? '↑' : '↓')

const businessMetricCards = computed(() => [
  {
    key: 'totalRevenue',
    label: t('stage5.statistics.business.totalAccommodationRevenue'),
    value: totalRevenue.value,
    icon: businessCartIcon,
    primary: true,
    changeRate: BUSINESS_METRIC_COMPARISON.totalRevenue.changeRate,
    changeDirection: getChangeDirection(BUSINESS_METRIC_COMPARISON.totalRevenue.changeRate),
    changeIcon: getChangeIcon(BUSINESS_METRIC_COMPARISON.totalRevenue.changeRate),
    previousText: `¥${formatMoney(BUSINESS_METRIC_COMPARISON.totalRevenue.previousMonthAmount)}`,
  },
  {
    key: 'roomFee',
    label: t('stage5.statistics.common.roomFee'),
    value: roomFee.value,
    icon: businessHomeIcon,
    primary: false,
    changeRate: BUSINESS_METRIC_COMPARISON.roomFee.changeRate,
    changeDirection: getChangeDirection(BUSINESS_METRIC_COMPARISON.roomFee.changeRate),
    changeIcon: getChangeIcon(BUSINESS_METRIC_COMPARISON.roomFee.changeRate),
    previousText: formatMoney(BUSINESS_METRIC_COMPARISON.roomFee.previousMonthAmount),
  },
  {
    key: 'deposit',
    label: t('stage5.statistics.common.deposit'),
    value: deposit.value,
    icon: businessDepositIcon,
    primary: false,
    changeRate: BUSINESS_METRIC_COMPARISON.deposit.changeRate,
    changeDirection: getChangeDirection(BUSINESS_METRIC_COMPARISON.deposit.changeRate),
    changeIcon: getChangeIcon(BUSINESS_METRIC_COMPARISON.deposit.changeRate),
    previousText: formatMoney(BUSINESS_METRIC_COMPARISON.deposit.previousMonthAmount),
  },
  {
    key: 'checkout',
    label: t('stage5.dataCenter.overview.checkoutRefund'),
    value: checkout.value,
    icon: businessCheckoutIcon,
    primary: false,
    changeRate: BUSINESS_METRIC_COMPARISON.checkout.changeRate,
    changeDirection: getChangeDirection(BUSINESS_METRIC_COMPARISON.checkout.changeRate),
    changeIcon: getChangeIcon(BUSINESS_METRIC_COMPARISON.checkout.changeRate),
    previousText: formatMoney(BUSINESS_METRIC_COMPARISON.checkout.previousMonthAmount),
  },
  {
    key: 'roomService',
    label: t('stage5.dataCenter.overview.roomService'),
    value: roomService.value,
    icon: businessCustomerIcon,
    primary: false,
    changeRate: BUSINESS_METRIC_COMPARISON.roomService.changeRate,
    changeDirection: getChangeDirection(BUSINESS_METRIC_COMPARISON.roomService.changeRate),
    changeIcon: getChangeIcon(BUSINESS_METRIC_COMPARISON.roomService.changeRate),
    previousText: formatMoney(BUSINESS_METRIC_COMPARISON.roomService.previousMonthAmount),
  },
])

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

const resolvePaymentAmount = (
  stats: RevenueSummaryDTO['paymentMethodStats'],
  methodKey: (typeof PAYMENT_METHOD_KEYS)[keyof typeof PAYMENT_METHOD_KEYS],
) => {
  const targetLabel = translatePaymentMethod(methodKey)
  const targetKey = normalizeLabel(targetLabel)
  const stat = stats.find((item) => {
    const translated = translatePaymentMethod(item.paymentMethod)
    return normalizeLabel(translated) === targetKey || resolveLabelKey(item.paymentMethod, PAYMENT_METHOD_ALIASES) === methodKey
  })
  return stat?.amount || 0
}

const resolveCategoryAmount = (
  stats: RevenueSummaryDTO['categoryStats'],
  categoryKey: (typeof REVENUE_CATEGORY_KEYS)[keyof typeof REVENUE_CATEGORY_KEYS],
) => {
  const targetLabel = translateRevenueCategory(categoryKey)
  const targetKey = normalizeLabel(targetLabel)
  const stat = stats.find((item) => {
    const translated = translateRevenueCategory(item.category)
    return normalizeLabel(translated) === targetKey || resolveLabelKey(item.category, REVENUE_CATEGORY_ALIASES) === categoryKey
  })
  return stat?.amount || 0
}

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

const loadChannelColorOverrides = async () => {
  try {
    const response = await getAllChannels()
    if (response.success && Array.isArray(response.data)) {
      buildChannelColorOverrides(response.data)

      if (activeTab.value === 'revenue' && revenueSubTab.value === 'payment') {
        await nextTick()
        initRevenueDistChart()
      }
    }
  } catch (error) {
    console.warn('Failed to load channel colors for revenue chart', error)
  }
}

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
      bookingRevenue.value = resolvePaymentAmount(
        data.paymentMethodStats,
        PAYMENT_METHOD_KEYS.bookingCollection,
      )
      airbnbRevenue.value = resolvePaymentAmount(
        data.paymentMethodStats,
        PAYMENT_METHOD_KEYS.airbnbCollection,
      )

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
      normalRevenue.value =
        resolveCategoryAmount(data.categoryStats, REVENUE_CATEGORY_KEYS.regularRevenue) ||
        data.totalRevenue
      arRevenue.value = resolveCategoryAmount(data.categoryStats, REVENUE_CATEGORY_KEYS.arMismatchRevenue)
      notesRevenue.value = resolveCategoryAmount(data.categoryStats, REVENUE_CATEGORY_KEYS.notesRevenue)

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

  const fallbackPieData = [
    { value: roomFee.value || 190.65, name: t('stage5.statistics.common.roomFee') },
    { value: checkout.value || 190.65, name: t('stage5.dataCenter.overview.checkoutRefund') },
    { value: deposit.value || 190.65, name: t('stage5.statistics.common.deposit') },
    { value: roomService.value || 190.65, name: t('stage5.dataCenter.overview.roomService') },
  ]
  const pieChartData = data?.categoryDistribution?.length
    ? data.categoryDistribution.map((item) => ({
        value: item.value,
        name: translateBusinessCategory(item.category),
      }))
    : fallbackPieData
  const pieTotal = pieChartData.reduce((sum, item) => sum + Number(item.value || 0), 0)
  const pieColors = ['#5a7df6', '#ffa59a', '#9d7df8', '#ffe7b5']

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: '#ffffff',
      borderColor: '#eeeeee',
      borderWidth: 1,
      textStyle: {
        color: '#111111',
        fontSize: 12,
      },
      formatter: (params: any) => `${params.name}<br/>¥${formatMoney(params.value)} (${params.percent}%)`,
    },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 10,
      itemHeight: 10,
      icon: 'circle',
      itemGap: 14,
      textStyle: {
        color: '#333333',
        fontSize: 12,
      },
      data: pieChartData.map((item) => item.name),
    },
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '48%',
        style: {
          text: `¥${formatMoney(pieTotal)}`,
          fill: '#111111',
          fontSize: 24,
          fontWeight: 800,
          textAlign: 'center',
        },
      },
    ],
    grid: {
      top: 0,
      bottom: 0,
    },
    series: [
      {
        name: '消费分布',
        type: 'pie',
        radius: ['56%', '76%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 0,
          borderColor: '#fff',
          borderWidth: 0,
        },
        label: {
          show: true,
          formatter: (params: any) => `${params.percent}%  ${params.name}\n¥${formatMoney(params.value)}`,
          color: 'inherit',
          fontSize: 12,
          lineHeight: 18,
        },
        labelLine: {
          show: true,
          length: 16,
          length2: 56,
          lineStyle: {
            width: 1,
          },
        },
        data: pieChartData,
      },
    ],
    color: pieColors,
  }

  businessPie.setOption(option)
}

// 初始化营业概况柱状图
const MAX_BUSINESS_BAR_COUNT = 35

type BusinessTrendBarItem = {
  label: string
  roomFee: number
  checkoutFee: number
  roomServiceFee: number
  deposit: number
}

const MOCK_BUSINESS_TREND_BARS: BusinessTrendBarItem[] = [
  // TODO(backend): remove once the API reliably returns monthly trend data for the selected period.
  { label: '5月', roomFee: 19000, checkoutFee: 0, roomServiceFee: 0, deposit: 0 },
  { label: '6月', roomFee: 10500, checkoutFee: 0, roomServiceFee: 0, deposit: 0 },
  { label: '7月', roomFee: 12200, checkoutFee: 0, roomServiceFee: 0, deposit: 0 },
  { label: '8月', roomFee: 16000, checkoutFee: 0, roomServiceFee: 0, deposit: 0 },
  { label: '9月', roomFee: 28000, checkoutFee: 0, roomServiceFee: 0, deposit: 0 },
  { label: '10月', roomFee: 0, checkoutFee: 0, roomServiceFee: 0, deposit: 0 },
  { label: '11月', roomFee: 0, checkoutFee: 0, roomServiceFee: 0, deposit: 0 },
  { label: '12月', roomFee: 0, checkoutFee: 0, roomServiceFee: 0, deposit: 0 },
  { label: '1月', roomFee: 0, checkoutFee: 0, roomServiceFee: 0, deposit: 0 },
]

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

  const apiBars = buildBusinessTrendBars(data?.consumptionTrend || [])
  const barData = apiBars.length ? apiBars : MOCK_BUSINESS_TREND_BARS
  const dates = barData.map((item) => item.label)
  const revenueData = barData.map(
    (item) => item.roomFee + item.checkoutFee + item.roomServiceFee + item.deposit,
  )
  const maxRevenue = Math.max(...revenueData, 40000)
  const yMax = Math.ceil(maxRevenue / 10000) * 10000
  const salesBackgroundData = dates.map(() => yMax)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter: (params: any) => {
        if (!params || !params.length) return ''
        const revenueItem = params.find((item: any) => item.seriesName === '总营业额')
        return [
          `<strong>${params[0].axisValue}</strong>`,
          `${revenueItem?.marker || ''}总销售&nbsp;&nbsp;&nbsp;&nbsp;${apiBars.length ? '-' : '440'}`,
          `${revenueItem?.marker || ''}总营业额&nbsp;&nbsp;¥${formatMoney(revenueItem?.value || 0)}`,
        ].join('<br/>')
      },
      backgroundColor: '#ffffff',
      borderColor: '#e6e6e6',
      borderWidth: 1,
      padding: [12, 14],
      textStyle: {
        color: '#101010',
        fontSize: 12,
      },
      extraCssText: 'box-shadow:0 6px 18px rgba(0,0,0,0.18);border-radius:4px;',
    },
    legend: {
      data: ['总销售', '总营业额'],
      bottom: 0,
      left: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: {
        color: '#555555',
        fontSize: 12,
      },
    },
    grid: {
      left: 48,
      right: 18,
      bottom: 42,
      top: 24,
      containLabel: false,
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisTick: {
        show: false,
      },
      axisLine: {
        lineStyle: {
          color: '#d8d8d8',
        },
      },
      axisLabel: {
        interval: 0,
        color: '#5c5c5c',
        fontSize: 12,
      },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: yMax,
      splitNumber: 5,
      axisLabel: {
        color: '#5c5c5c',
        fontSize: 12,
        formatter: (value: number) => `${value / 1000}K`,
      },
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      splitLine: {
        lineStyle: {
          color: '#dedede',
          type: 'dashed',
        },
      },
    },
    series: [
      {
        // TODO(backend): replace this visual background with a real total-sales series when available.
        name: '总销售',
        type: 'bar',
        barWidth: 34,
        barGap: '-100%',
        data: salesBackgroundData,
        itemStyle: {
          color: '#f0f0f0',
          borderRadius: [5, 5, 0, 0],
        },
        emphasis: {
          disabled: true,
        },
        tooltip: {
          show: false,
        },
      },
      {
        name: '总营业额',
        type: 'bar',
        barWidth: 34,
        data: revenueData,
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: (params: any) =>
            params.dataIndex === revenueData.indexOf(Math.max(...revenueData))
              ? '#5ea8f4'
              : '#e8e8e8',
        },
      },
    ],
    color: ['#efefef', '#5ea8f4'],
  }

  businessBar.setOption(option)
}

// 初始化流水分布饼图
const initRevenueDistChart = (data?: RevenueSummaryDTO) => {
  if (!revenueDistChart.value) return

  if (revenueDist) revenueDist.dispose()
  revenueDist = echarts.init(revenueDistChart.value)

  const chartData = (
    data?.paymentMethodStats?.length
      ? data.paymentMethodStats.map((stat) => ({
          value: stat.amount,
          name: translatePaymentMethod(stat.paymentMethod),
        }))
      : revenueTableData.value.map((row) => ({
          value: Number(row.total || 0),
          name: String(row.paymentMethod),
        }))
  ).filter((item) => Number(item.value) > 0)
  const visibleChartData = chartData.length
    ? chartData
    : [{ value: 0, name: t('stage5.dataCenter.overview.collectionDistribution') }]
  const total = visibleChartData.reduce((sum, item) => sum + Number(item.value || 0), 0)

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: '#ffffff',
      borderColor: '#eeeeee',
      borderWidth: 1,
      textStyle: {
        color: '#111111',
        fontSize: 12,
      },
      formatter: (params: any) => `${params.name}<br/>¥${formatMoney(params.value)} (${params.percent}%)`,
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 16,
      textStyle: {
        color: '#555555',
        fontSize: 12,
      },
    },
    graphic: {
      type: 'text',
      left: 'center',
      top: '48%',
      style: {
        text: formatMoney(total),
        fill: '#111111',
        fontSize: 24,
        fontWeight: 700,
        textAlign: 'center',
      },
    },
    series: [
      {
        name: t('stage5.dataCenter.overview.collectionDistribution'),
        type: 'pie',
        radius: ['56%', '74%'],
        center: ['50%', '45%'],
        data: visibleChartData.map((item, index) => ({
          ...item,
          itemStyle: {
            color: getChannelColor(item.name, index),
          },
        })),
        itemStyle: {
          borderRadius: 0,
          borderColor: '#fff',
          borderWidth: 0,
        },
        label: {
          show: true,
          formatter: (params: any) => `${params.percent}% ${params.name}\n¥${formatMoney(params.value)}`,
          color: 'inherit',
          fontSize: 12,
          lineHeight: 18,
        },
        labelLine: {
          show: true,
          length: 18,
          length2: 70,
          lineStyle: {
            width: 1,
          },
        },
      },
    ],
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
      formatter: (params: any) => `${params.name}<br/>¥${formatMoney(params.value)} (${params.percent}%)`,
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: {
        color: '#666666',
        fontSize: 12,
      },
    },
    graphic: {
      type: 'text',
      left: 'center',
      top: '48%',
      style: {
        text: '0.00',
        fill: '#111111',
        fontSize: 24,
        fontWeight: 700,
        textAlign: 'center',
      },
    },
    series: [
      {
        name: t('stage5.dataCenter.overview.totalExpense'),
        type: 'pie',
        radius: ['56%', '74%'],
        center: ['50%', '45%'],
        data: [
          { value: 0, name: t('stage5.dataCenter.overview.totalExpense') }
        ],
        itemStyle: {
          color: '#dcecff',
        },
        label: {
          show: false,
        },
      },
    ],
  }

  expense.setOption(option)
}

// 初始化款项分类收款分布饼图
const initCategoryDistChart = () => {
  if (!categoryDistChart.value) return

  if (categoryDistChart_instance) categoryDistChart_instance.dispose()
  categoryDistChart_instance = echarts.init(categoryDistChart.value)

  const chartData = categoryTableData.value
    .map((row) => ({
      value: Number(row.total || 0),
      name: String(row.paymentMethod),
    }))
    .filter((item) => item.value > 0)
  const visibleChartData = chartData.length
    ? chartData
    : [{ value: 0, name: t('stage5.dataCenter.overview.collectionDistribution') }]
  const total = visibleChartData.reduce((sum, item) => sum + Number(item.value || 0), 0)

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: '#ffffff',
      borderColor: '#eeeeee',
      borderWidth: 1,
      textStyle: {
        color: '#111111',
        fontSize: 12,
      },
      formatter: (params: any) => `${params.name}<br/>¥${formatMoney(params.value)} (${params.percent}%)`,
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 16,
      textStyle: {
        color: '#555555',
        fontSize: 12,
      },
    },
    graphic: {
      type: 'text',
      left: 'center',
      top: '48%',
      style: {
        text: formatMoney(total),
        fill: '#111111',
        fontSize: 24,
        fontWeight: 700,
        textAlign: 'center',
      },
    },
    series: [
      {
        name: t('stage5.dataCenter.overview.collectionDistribution'),
        type: 'pie',
        radius: ['56%', '74%'],
        center: ['50%', '45%'],
        data: visibleChartData,
        itemStyle: {
          borderRadius: 0,
          borderColor: '#fff',
          borderWidth: 0,
        },
        label: {
          show: true,
          formatter: (params: any) => `${params.percent}% ${params.name}\n¥${formatMoney(params.value)}`,
          color: '#168bf8',
          fontSize: 12,
          lineHeight: 18,
        },
        labelLine: {
          show: true,
          length: 18,
          length2: 70,
          lineStyle: {
            width: 1,
          },
        },
      },
    ],
    color: ['#168bf8'],
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
      formatter: (params: any) => `${params.name}<br/>¥${formatMoney(params.value)} (${params.percent}%)`,
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 16,
      textStyle: {
        color: '#555555',
        fontSize: 12,
      },
    },
    graphic: {
      type: 'text',
      left: 'center',
      top: '48%',
      style: {
        text: formatMoney(Math.abs(categoryExpense.value)),
        fill: '#111111',
        fontSize: 24,
        fontWeight: 700,
        textAlign: 'center',
      },
    },
    series: [
      {
        name: t('stage5.dataCenter.overview.totalExpense'),
        type: 'pie',
        radius: ['56%', '74%'],
        center: ['50%', '45%'],
        data: [
          { value: Math.abs(categoryExpense.value), name: t('stage5.dataCenter.overview.regularRevenue') }
        ],
        itemStyle: {
          borderRadius: 0,
          borderColor: '#fff',
          borderWidth: 0,
        },
        label: {
          show: true,
          formatter: (params: any) => `${params.percent}% ${params.name}\n¥${formatMoney(params.value)}`,
          color: '#5a7df6',
          fontSize: 12,
          lineHeight: 18,
        },
        labelLine: {
          show: true,
          length: 18,
          length2: 70,
          lineStyle: {
            width: 1,
          },
        },
      },
    ],
    color: ['#dcecff'],
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
  loadChannelColorOverrides()

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
  min-width: 1218px;
  min-height: 100%;
  padding: 0 0 24px;
  background: transparent;
}

.tabs-section {
  --overview-tabs-center-shift: calc(
    -56px + ((var(--sidebar-width, 84px) - 84px) / 6) + 4px
  );
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 1218px;
  padding: 2px 0 16px;
  background: transparent;
  border-bottom: none;
}

.overview-tabs {
  display: flex;
  justify-content: center;
  width: max-content;
  transform: translateX(var(--overview-tabs-center-shift));
  transition: transform 0.24s ease;
}

.overview-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.overview-tabs :deep(.el-tabs__nav-wrap) {
  overflow: visible;
}

.overview-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.overview-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.overview-tabs :deep(.el-tabs__nav-scroll) {
  display: flex;
  justify-content: center;
  overflow: visible;
}

.overview-tabs :deep(.el-tabs__nav) {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  float: none;
  height: 28px;
  padding: 2px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 8px rgba(30, 30, 30, 0.04);
}

.overview-tabs :deep(.el-tabs__item) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 78px;
  height: 24px;
  padding: 0 12px !important;
  border-radius: 999px;
  color: #252525;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.overview-tabs :deep(.el-tabs__item:hover:not(.is-active)) {
  background: #f2f2f2;
  color: #111111;
}

.overview-tabs :deep(.el-tabs__item.is-active),
.overview-tabs :deep(.el-tabs__item.is-active:hover) {
  background: #111111;
  color: #ffffff;
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

.date-separator {
  color: #606266;
  font-size: 14px;
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

.overview-container :deep(.filter-section .el-select__wrapper),
.overview-container :deep(.filter-section .el-input__wrapper) {
  min-height: 32px;
  border-radius: 5px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.overview-container :deep(.filter-section .el-select__wrapper:hover),
.overview-container :deep(.filter-section .el-select__wrapper.is-focused),
.overview-container :deep(.filter-section .el-input__wrapper:hover),
.overview-container :deep(.filter-section .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #87bdf6 inset;
}

.overview-container :deep(.filter-section .business-date-range.el-range-editor.el-input__wrapper) {
  width: 288px;
  max-width: 288px;
  flex: 0 0 288px;
  padding: 1px 1px 1px 6px;
  overflow: hidden;
}

.overview-container :deep(.filter-section .business-date-range .el-range__icon) {
  margin: 0 9px 0 2px;
  color: #aeb4bd;
  font-size: 14px;
}

.overview-container :deep(.filter-section .business-date-range .el-range-input) {
  height: 30px;
  padding: 0 7px;
  background: #fafafa;
  color: #30343b;
  font-size: 13px;
  font-weight: 500;
  line-height: 30px;
}

.overview-container :deep(.filter-section .business-date-range .el-range-input:first-child) {
  border-radius: 4px 0 0 4px;
}

.overview-container :deep(.filter-section .business-date-range .el-range-input:last-child) {
  border-radius: 0 4px 4px 0;
}

.overview-container :deep(.filter-section .business-date-range .el-range-separator) {
  width: 44px;
  height: 30px;
  background: #ffffff;
  color: #777f89;
  font-size: 12px;
  line-height: 30px;
}

.overview-container :deep(.filter-section .business-date-range .el-range__close-icon) {
  display: none;
}

/* 标签页内容 */
.tab-content {
  margin-top: 0;
}

/* 营业概况样式 */
.stats-section {
  display: grid;
  grid-template-columns: 1.25fr repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 10px;
}

.business-stat-card {
  display: flex;
  min-height: 154px;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 20px 18px;
  background: #ffffff;
  border: none;
  border-radius: 4px;
  box-shadow: none;
}

.business-stat-card.primary {
  background: linear-gradient(135deg, #168bf8 0%, #67b5ff 100%);
  color: white;
}

.stat-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.stat-icon {
  display: inline-flex;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  align-items: center;
  justify-content: center;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.business-stat-card.primary .stat-icon {
  background: transparent;
  box-shadow: none;
}

.stat-icon-image {
  display: block;
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.business-stat-card .stat-label {
  color: #111111;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.business-stat-card.primary .stat-label {
  color: #ffffff;
}

.stat-value-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.business-stat-card .stat-value {
  min-width: 0;
  color: #050505;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
}

.business-stat-card.primary .stat-value {
  color: #ffffff;
}

.stat-change {
  display: inline-flex;
  height: 20px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 0 7px 0 6px;
  border-radius: 4px;
  background: #69b7ff;
  color: #ffffff;
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
}

.stat-change-icon {
  display: inline-block;
  transform: translateY(-1px);
  font-size: 17px;
  font-weight: 400;
  line-height: 1;
}

.stat-previous {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  color: #0a0a0a;
  font-size: 12px;
  line-height: 1;
}

.business-stat-card.primary .stat-previous {
  color: rgba(255, 255, 255, 0.9);
}

.stat-previous strong {
  color: inherit;
  font-weight: 700;
}

/* 流水汇总样式 */
.revenue-filter-tabs {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: 8px;
}

.revenue-filter-tabs :deep(.el-button) {
  min-width: 88px;
  height: 32px;
  margin: 0;
  border: 1px solid #e2e5ea;
  border-radius: 4px;
  background: #ffffff;
  color: #8b8f97;
  font-size: 13px;
  font-weight: 500;
  box-shadow: none;
}

.revenue-filter-tabs :deep(.el-button.active),
.revenue-filter-tabs :deep(.el-button:hover) {
  border-color: #1e90f7;
  background: #1e90f7;
  color: #ffffff;
}

.revenue-stats {
  display: grid;
  grid-template-columns: minmax(360px, 420px) minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
  margin-bottom: 10px;
}

.revenue-summary-card {
  position: relative;
  display: flex;
  min-height: 154px;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  padding: 28px 22px 22px;
  overflow: hidden;
  background: #ffffff;
  border: none;
  border-radius: 4px;
}

.revenue-summary-card.primary {
  justify-content: space-between;
  background: linear-gradient(135deg, #168bf8 0%, #68b8ff 100%);
  color: #ffffff;
}

.revenue-summary-card:not(.primary)::before {
  position: absolute;
  top: 0;
  left: 0;
  width: 1px;
  height: 100%;
  background: #168bf8;
  content: '';
}

.revenue-summary-card.airbnb-card::before {
  background: #ff385c;
}

.revenue-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.revenue-card-label {
  color: #333333;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.revenue-summary-card.primary .revenue-card-label {
  color: #ffffff;
}

.revenue-card-icon {
  display: inline-flex;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
}

.revenue-card-icon.blue {
  background: #168bf8;
}

.revenue-card-icon img {
  display: block;
  width: 31px;
  height: 31px;
  object-fit: contain;
}

.revenue-card-value {
  color: #ffffff;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
}

.revenue-card-value.dark {
  color: #0d0d0d;
  font-size: 24px;
}

.revenue-card-details {
  display: flex;
  align-items: center;
  gap: 24px;
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}

/* 图表区域 */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
}

.chart-card {
  background: #ffffff;
  border: none;
  border-radius: 4px;
  padding: 20px 22px 16px;
}

.business-charts-row {
  gap: 10px;
  margin-bottom: 10px;
}

.trend-card {
  min-height: 405px;
  background: #ffffff;
  border: none;
  border-radius: 4px;
  padding: 20px 22px 16px;
}

.distribution-card {
  min-height: 444px;
  background: #ffffff;
  border: none;
  border-radius: 4px;
  padding: 20px 22px 16px;
}

.revenue-chart-card {
  min-height: 470px;
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

.trend-card .chart-title,
.distribution-card .chart-title,
.revenue-chart-card .chart-title {
  margin: 0 0 14px 0;
  color: #0f0f0f;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.chart-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.business-chart-select {
  width: 72px;
  flex: 0 0 72px;
}

.business-chart-select :deep(.el-select__wrapper) {
  min-height: 30px;
  border-radius: 4px;
  background: #fafafa;
  box-shadow: none;
}

.business-chart-select :deep(.el-select__selected-item) {
  color: #121212;
  font-size: 13px;
  font-weight: 500;
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

.business-trend-chart,
.business-distribution-chart {
  height: 370px;
}

.revenue-donut-chart {
  height: 392px;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 8px;
  padding: 32px;
}

.sales-stats .stat-label {
  font-size: 14px;
  margin-bottom: 12px;
  opacity: 0.9;
}

.sales-stats .stat-value.large {
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 16px;
}

.sales-stats .stat-card.large {
  max-width: 500px;
}

/* 表格区域 */
.table-section {
  margin-top: 0;
}

.business-table-section {
  min-height: 350px;
  margin-top: 0;
  padding: 18px 22px 28px;
  background: #ffffff;
  border-radius: 4px;
}

.revenue-table-section {
  min-height: 350px;
  margin-top: 0;
  padding: 18px 22px 28px;
  background: #ffffff;
  border-radius: 4px;
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

.business-table-section .table-header {
  margin-bottom: 28px;
}

.business-table-section .table-title,
.revenue-table-section .table-title {
  color: #0d0d0d;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.revenue-table-section .table-header {
  align-items: center;
  margin-bottom: 28px;
}

.revenue-table-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.business-export-button {
  min-width: 138px;
  height: 32px;
  border: none;
  border-radius: 4px;
  background: #1e90f7;
  font-size: 13px;
  font-weight: 500;
}

.table-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.revenue-table-tabs {
  gap: 10px;
  margin-bottom: 0;
}

.revenue-table-tabs :deep(.el-button) {
  min-width: 88px;
  height: 32px;
  margin: 0;
  border: 1px solid #e2e5ea;
  border-radius: 4px;
  background: #ffffff;
  color: #8b8f97;
  font-size: 13px;
  font-weight: 500;
  box-shadow: none;
}

.revenue-table-tabs :deep(.el-button.active),
.revenue-table-tabs :deep(.el-button:hover) {
  border-color: #1e90f7;
  background: #1e90f7;
  color: #ffffff;
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

.business-detail-table {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: #fafafa;
  --el-table-row-hover-bg-color: #f9fbff;
  background: #ffffff;
}

.business-detail-table :deep(.el-table__inner-wrapper::before),
.business-detail-table :deep(.el-table__border-left-patch) {
  display: none;
}

.business-detail-table :deep(th.el-table__cell) {
  height: 30px;
  background: #fafafa !important;
  border-bottom: none;
  color: #181818;
  font-size: 13px;
  font-weight: 600;
}

.business-detail-table :deep(td.el-table__cell) {
  height: 50px;
  border-bottom: none;
  color: #080808;
  font-size: 13px;
  font-weight: 500;
}

.business-detail-table :deep(.cell) {
  padding: 0 8px;
  line-height: 1.4;
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
