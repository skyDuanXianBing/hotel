<template>
  <StatisticsLayout>
    <div class="overview-container" v-loading="loading">
      <!-- 顶部选项卡 -->
      <div class="tabs-section">
        <el-tabs v-model="activeTab" class="overview-tabs" @tab-change="handleTabChange">
          <el-tab-pane :label="t('stage5.dataCenter.overview.tabs.business')" name="business" />
          <el-tab-pane
            :label="t('stage5.dataCenter.overview.tabs.revenue')"
            name="revenue"
            :disabled="!canViewRevenue"
          />
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

      <el-alert
        v-if="activeTabError"
        class="tab-state-alert"
        type="error"
        :title="activeTabError"
        :closable="false"
        show-icon
      />

      <el-empty
        v-if="showActiveEmpty"
        class="tab-empty"
        :description="t('stage5.dataCenter.overview.noData')"
      />

      <!-- 营业概况内容 -->
      <div v-if="activeTab === 'business' && !showActiveEmpty && !activeTabError" class="tab-content">
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
            </div>
          </div>
        </div>

        <!-- 消费分类分布和住宿消费趋势 -->
        <div class="charts-row business-charts-row">
          <div class="chart-card trend-card">
            <div class="chart-card-header">
              <h3 class="chart-title">营业额趋势</h3>
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
            <el-button
              type="primary"
              class="business-export-button"
              :loading="exportingReport === 'daily'"
              @click="handleOverviewExport('daily')"
            >
              {{ t('stage5.common.actions.exportDetails') }}
            </el-button>
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
      <div v-if="activeTab === 'revenue' && !showActiveEmpty && !activeTabError" class="tab-content">
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
                <div class="revenue-card-label">{{ secondaryRevenueCards[0].label }}</div>
                <div class="revenue-card-icon blue">
                  <img :src="businessDepositIcon" :alt="secondaryRevenueCards[0].label" />
                </div>
              </div>
              <div class="revenue-card-value dark">¥{{ formatMoney(secondaryRevenueCards[0].value) }}</div>
            </div>

            <div class="revenue-summary-card booking-card">
              <div class="revenue-card-head">
                <div class="revenue-card-label">{{ secondaryRevenueCards[1].label }}</div>
                <div class="revenue-card-icon blue">
                  <img :src="businessDepositIcon" :alt="secondaryRevenueCards[1].label" />
                </div>
              </div>
              <div class="revenue-card-value dark">¥{{ formatMoney(secondaryRevenueCards[1].value) }}</div>
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
                <el-button
                  type="primary"
                  class="business-export-button"
                  :loading="exportingReport === 'transaction-summary'"
                  @click="handleOverviewExport('transaction-summary')"
                >
                  {{ t('stage5.common.actions.exportDetails') }}
                </el-button>
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
                <div class="revenue-card-label">{{ t('stage5.dataCenter.overview.netIncome') }}</div>
                <div class="revenue-card-icon blue">
                  <img :src="businessCartIcon" :alt="t('stage5.dataCenter.overview.netIncome')" />
                </div>
              </div>
              <div class="revenue-card-value dark">¥{{ formatMoney(revenueNetIncome) }}</div>
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
                <el-button
                  type="primary"
                  class="business-export-button"
                  :loading="exportingReport === 'transaction-summary'"
                  @click="handleOverviewExport('transaction-summary')"
                >
                  {{ t('stage5.common.actions.exportDetails') }}
                </el-button>
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
      <div v-if="activeTab === 'channel' && !showActiveEmpty && !activeTabError" class="tab-content">
        <!-- 渠道消费分布和间夜分布 -->
        <div class="charts-row channel-charts-row">
          <div class="chart-card channel-chart-card channel-donut-card">
            <h3 class="chart-title channel-chart-title">{{ t('stage5.dataCenter.overview.channelConsumptionDistribution') }}</h3>
            <div ref="channelRevenueChart" class="chart-container channel-donut-chart"></div>
          </div>
          <div class="chart-card channel-chart-card channel-donut-card">
            <h3 class="chart-title channel-chart-title">{{ t('stage5.dataCenter.overview.channelNightsDistribution') }}</h3>
            <div ref="channelNightsChart" class="chart-container channel-donut-chart"></div>
          </div>
        </div>

        <!-- 渠道消费趋势和间夜趋势 -->
        <div class="charts-row channel-charts-row">
          <div class="chart-card wide channel-chart-card channel-trend-card">
            <h3 class="chart-title channel-chart-title">{{ t('stage5.dataCenter.overview.channelConsumptionTrend') }}</h3>
            <div ref="channelRevenueTrendChart" class="chart-container-large channel-trend-chart"></div>
          </div>
          <div class="chart-card wide channel-chart-card channel-trend-card">
            <h3 class="chart-title channel-chart-title">{{ t('stage5.dataCenter.overview.channelNightsTrend') }}</h3>
            <div ref="channelNightsTrendChart" class="chart-container-large channel-trend-chart"></div>
          </div>
        </div>

        <!-- 渠道明细表格 -->
        <div class="table-section channel-table-section">
          <div class="table-header channel-table-header">
            <h3 class="table-title">{{ t('stage5.dataCenter.overview.channelDetails') }} ({{ dateRangeLabel }})</h3>
            <div class="table-tabs channel-table-tabs">
              <el-button
                v-for="tab in channelTableTabs"
                :key="tab.key"
                :class="{ active: channelTableTab === tab.key }"
                @click="handleChannelTableTabChange(tab.key)"
              >
                {{ tab.label }}
              </el-button>
            </div>
          </div>

          <el-table :data="channelTableData" class="detail-table business-detail-table channel-detail-table">
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
                {{ formatChannelCell(row, column.prop) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 销售汇总内容 -->
      <div v-if="activeTab === 'sales' && !showActiveEmpty && !activeTabError" class="tab-content">
        <!-- 销售额卡片 -->
        <div class="sales-stats">
          <div class="sales-summary-card">
            <div class="sales-card-head">
              <div class="sales-card-label">{{ t('stage5.dataCenter.overview.salesTotal') }}</div>
              <div class="sales-card-icon">
                <img :src="businessCartIcon" :alt="t('stage5.dataCenter.overview.salesTotal')" />
              </div>
            </div>
            <div class="sales-card-value">¥{{ salesTotal.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
          </div>
        </div>

        <!-- 每日销售额趋势 -->
        <div class="chart-card full-width sales-trend-card">
          <h3 class="chart-title sales-chart-title">{{ t('stage5.dataCenter.overview.dailySalesStats') }} <span class="chart-subtitle">{{ t('stage5.dataCenter.overview.statsConsumption') }}</span></h3>
          <div ref="salesTrendChart" class="chart-container-xlarge sales-trend-chart"></div>
        </div>

        <!-- 销售订单明细 -->
        <div class="table-section sales-table-section">
          <div class="table-header sales-table-header">
            <h3 class="table-title">{{ t('stage5.dataCenter.overview.salesOrderDetails') }} ({{ dateRangeLabel }})</h3>
            <el-button
              type="primary"
              class="business-export-button sales-export-button"
              :loading="exportingReport === 'room-fees'"
              @click="handleOverviewExport('room-fees')"
            >
              {{ t('stage5.common.actions.exportDetails') }}
            </el-button>
          </div>

          <div class="search-section sales-search-section">
            <el-input
              v-model="searchKeyword"
              class="sales-search-input"
              :placeholder="t('stage5.dataCenter.overview.searchOrders')"
              clearable
              @keyup.enter="handleSalesSearch"
              @clear="handleSalesSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select
              v-model="searchChannel"
              class="sales-filter-select"
              :placeholder="t('stage5.common.filters.all')"
              clearable
              @change="handleSalesSearch"
            >
              <el-option :label="t('stage5.common.filters.all')" value="" />
              <el-option
                v-for="channel in salesChannelOptions"
                :key="channel.id"
                :label="channel.name"
                :value="channel.id"
              />
            </el-select>
            <el-input
              v-model="searchGuest"
              class="sales-filter-select"
              :placeholder="t('stage5.dataCenter.overview.customerFilterPlaceholder')"
              clearable
              @keyup.enter="handleSalesSearch"
              @clear="handleSalesSearch"
            >
            </el-input>
          </div>

          <el-table :data="salesTableData" class="detail-table sales-detail-table">
            <el-table-column prop="createdAt" :label="t('stage5.dataCenter.overview.createdAt')" min-width="150" align="center" />
            <el-table-column prop="guestName" :label="t('stage5.dataCenter.overview.name')" min-width="100" align="center" />
            <el-table-column prop="orderNumber" :label="t('stage5.dataCenter.overview.orderChannelNumber')" min-width="200" align="center">
              <template #default="{ row }">
                <div class="sales-order-number">{{ row.orderNumber }}</div>
                <div class="sales-channel-number">{{ row.channelNumber }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="channel" :label="t('stage5.common.filters.channel')" min-width="120" align="center" />
            <el-table-column prop="customerName" :label="t('stage5.dataCenter.overview.customerName')" min-width="120" align="center" />
            <el-table-column prop="phone" :label="t('stage5.dataCenter.overview.phone')" min-width="120" align="center" />
            <el-table-column prop="allocatedAmount" :label="t('stage5.dataCenter.overview.allocatedAmount')" min-width="170" align="center">
              <template #default="{ row }">
                <span class="amount-bold">¥{{ formatMoney(row.allocatedAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="totalAmount" :label="t('stage5.dataCenter.overview.orderTotalAmount')" min-width="150" align="center">
              <template #default="{ row }">
                <span class="amount-bold">¥{{ formatMoney(row.totalAmount) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="table-footer sales-table-footer">
            <span class="table-info">{{ t('stage5.dataCenter.overview.recordsTotal', { count: salesTableTotal }) }}</span>
            <el-pagination
              small
              layout="prev, pager, next"
              :total="salesTableTotal"
              :page-size="salesPageSize"
              v-model:current-page="salesCurrentPage"
              @current-change="handleSalesPageChange"
            />
            <span class="total-amount">¥{{ salesTotal.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
        </div>
      </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import StatisticsLayout from '../statistics/StatisticsLayout.vue'
import businessCartIcon from '@/assets/icons/statistics/business-cart.png'
import businessCustomerIcon from '@/assets/icons/statistics/business-customer.png'
import businessDepositIcon from '@/assets/icons/statistics/business-deposit.png'
import businessHomeIcon from '@/assets/icons/statistics/business-home.png'
import {
  downloadStatisticsReport,
  getStatisticsReportErrorMessage,
  getBusinessOverview,
  getRevenueSummary,
  getChannelSummary,
  getSalesSummary,
  saveBlobDownload,
  type BusinessOverviewDTO,
  type RevenueSummaryDTO,
  type ChannelSummaryDTO,
  type SalesSummaryDTO,
  type StatisticsReportType,
} from '@/api/statistics'
import { PermissionAction, PermissionModule } from '@/api/role'
import { getAllChannels } from '@/api/channel'
import type { ChannelDTO } from '@/api/channel'
import { usePermissionStore } from '@/stores/permission'
import {
  addDaysToYmd,
  formatYmdMonthDay,
  getStoreTodayYmd,
  getYmdMonthStart,
  getYmdWeekStart,
  parseYmdAsUtcDate,
} from '@/utils/storeDateTime'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

type OverviewTab = 'business' | 'revenue' | 'channel' | 'sales'

const OVERVIEW_TABS: OverviewTab[] = ['business', 'revenue', 'channel', 'sales']

const BUSINESS_CATEGORY_KEYS = {
  roomFee: 'roomFee',
  checkoutRefund: 'checkoutRefund',
  roomService: 'roomService',
  deposit: 'deposit',
  notesIncome: 'notesIncome',
  notesExpense: 'notesExpense',
  netRevenue: 'netRevenue',
} as const

const PAYMENT_METHOD_KEYS = {
  bookingCollection: 'bookingCollection',
  airbnbCollection: 'airbnbCollection',
} as const

const REVENUE_CATEGORY_KEYS = {
  regularRevenue: 'regularRevenue',
  arMismatchRevenue: 'arMismatchRevenue',
  notesRevenue: 'notesRevenue',
  roomFee: 'roomFee',
  deposit: 'deposit',
  roomService: 'roomService',
  notesIncome: 'notesIncome',
  notesExpense: 'notesExpense',
  paymentRefund: 'paymentRefund',
  netIncome: 'netIncome',
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
  notesincome: BUSINESS_CATEGORY_KEYS.notesIncome,
  notes_income: BUSINESS_CATEGORY_KEYS.notesIncome,
  '记一笔收入': BUSINESS_CATEGORY_KEYS.notesIncome,
  '記一筆收入': BUSINESS_CATEGORY_KEYS.notesIncome,
  notesexpense: BUSINESS_CATEGORY_KEYS.notesExpense,
  notes_expense: BUSINESS_CATEGORY_KEYS.notesExpense,
  '记一笔支出': BUSINESS_CATEGORY_KEYS.notesExpense,
  '記一筆支出': BUSINESS_CATEGORY_KEYS.notesExpense,
  netrevenue: BUSINESS_CATEGORY_KEYS.netRevenue,
  net_revenue: BUSINESS_CATEGORY_KEYS.netRevenue,
  '净收入': BUSINESS_CATEGORY_KEYS.netRevenue,
  '純収入': BUSINESS_CATEGORY_KEYS.netRevenue,
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
  roomfee: REVENUE_CATEGORY_KEYS.roomFee,
  room_fee: REVENUE_CATEGORY_KEYS.roomFee,
  '税后房费': REVENUE_CATEGORY_KEYS.roomFee,
  '房费': REVENUE_CATEGORY_KEYS.roomFee,
  deposit: REVENUE_CATEGORY_KEYS.deposit,
  '押金': REVENUE_CATEGORY_KEYS.deposit,
  roomservice: REVENUE_CATEGORY_KEYS.roomService,
  room_service: REVENUE_CATEGORY_KEYS.roomService,
  '客房消费': REVENUE_CATEGORY_KEYS.roomService,
  notesincome: REVENUE_CATEGORY_KEYS.notesIncome,
  notes_income: REVENUE_CATEGORY_KEYS.notesIncome,
  '记一笔收入': REVENUE_CATEGORY_KEYS.notesIncome,
  notesexpense: REVENUE_CATEGORY_KEYS.notesExpense,
  notes_expense: REVENUE_CATEGORY_KEYS.notesExpense,
  '记一笔支出': REVENUE_CATEGORY_KEYS.notesExpense,
  paymentrefund: REVENUE_CATEGORY_KEYS.paymentRefund,
  payment_refund: REVENUE_CATEGORY_KEYS.paymentRefund,
  refund: REVENUE_CATEGORY_KEYS.paymentRefund,
  '退款': REVENUE_CATEGORY_KEYS.paymentRefund,
  netincome: REVENUE_CATEGORY_KEYS.netIncome,
  net_income: REVENUE_CATEGORY_KEYS.netIncome,
  '净收入': REVENUE_CATEGORY_KEYS.netIncome,
}

interface DateColumn {
  prop: string
  label: string
  date: string
}

type DynamicAmountRow = Record<string, string | number>

type SalesTableRow = {
  createdAt: string
  guestName: string
  orderNumber: string
  channelNumber: string
  channel: string
  customerName: string
  phone: string
  amount: number
  allocatedAmount: number
  totalAmount: number
}

const canViewRevenue = computed(() =>
  permissionStore.hasPermission(PermissionModule.SENSITIVE, PermissionAction.VIEW_FINANCIAL_DATA),
)

const resolveTabFromQuery = (): OverviewTab => {
  const queryTab = String(route.query.tab || '')
  if (OVERVIEW_TABS.includes(queryTab as OverviewTab)) {
    return queryTab as OverviewTab
  }
  return 'business'
}

const activeTab = ref<OverviewTab>(resolveTabFromQuery())
const dateType = ref('today')
const todayYmd = getStoreTodayYmd()
const startDate = ref(todayYmd)
const endDate = ref(todayYmd)
const loading = ref(false)
const tabErrors = ref<Record<OverviewTab, string>>({
  business: '',
  revenue: '',
  channel: '',
  sales: '',
})
const tabHasData = ref<Record<OverviewTab, boolean>>({
  business: false,
  revenue: false,
  channel: false,
  sales: false,
})
const exportingReport = ref<StatisticsReportType | ''>('')

const activeTabError = computed(() => tabErrors.value[activeTab.value] || '')
const showActiveEmpty = computed(
  () => !loading.value && !activeTabError.value && !tabHasData.value[activeTab.value],
)

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
let channelColorLoadPromise: Promise<void> | null = null

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

const setSalesChannelOptions = (channels: ChannelDTO[]) => {
  salesChannelOptions.value = channels.filter((channel) => channel.enabled !== false)
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

const getChannelColors = (names: string[]) => names.map((name, index) => getChannelColor(name, index))

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

const createChannelDailyCells = (
  dailyValues: { date: string; revenue?: number; roomNights?: number }[] = [],
  key: 'revenue' | 'roomNights',
) => {
  const row = createEmptyDateAmounts()
  dailyValues.forEach((dailyValue) => {
    const prop = getDateColumnProp(dailyValue.date)
    if (prop in row) {
      row[prop] = dailyValue[key] || 0
    }
  })
  return row
}

const formatMoneyCell = (row: DynamicAmountRow, prop: string) => {
  const value = Number(row[prop] || 0)
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const formatMoney = (value: number | string) =>
  Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })

const formatChannelCell = (row: DynamicAmountRow, prop: string) => {
  const value = toNumber(row[prop])
  if (channelTableTab.value === 'channel-fee') {
    return `¥${formatMoney(value)}`
  }
  return value.toLocaleString('zh-CN')
}

const toNumber = (value: unknown) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const createDailyRevenueCells = (
  dailyRows: RevenueSummaryDTO['dailyRevenues'] = [],
  key: keyof RevenueSummaryDTO['dailyRevenues'][number],
) => {
  const row = createEmptyDateAmounts()
  dailyRows.forEach((dailyRow) => {
    const prop = getDateColumnProp(dailyRow.date)
    if (prop in row) {
      row[prop] = toNumber(dailyRow[key])
    }
  })
  return row
}

const sumDailyRevenueField = (
  dailyRows: RevenueSummaryDTO['dailyRevenues'] = [],
  key: keyof RevenueSummaryDTO['dailyRevenues'][number],
) => dailyRows.reduce((sum, dailyRow) => sum + toNumber(dailyRow[key]), 0)

const buildReportParams = () => ({
  startDate: startDate.value,
  endDate: endDate.value,
  keyword: searchKeyword.value || undefined,
  channelId: searchChannel.value || undefined,
  customer: searchGuest.value || undefined,
})

const handleOverviewExport = async (type: StatisticsReportType) => {
  if (!startDate.value || !endDate.value) {
    ElMessage.warning(t('stage5.common.messages.pleaseSelectDateRange'))
    return
  }

  try {
    exportingReport.value = type
    const download = await downloadStatisticsReport(type, buildReportParams())
    saveBlobDownload(download)
  } catch (error) {
    console.error('Failed to download statistics report:', error)
    const message = await getStatisticsReportErrorMessage(
      error,
      t('stage5.statistics.reports.downloadFailed'),
    )
    ElMessage.error(message)
  } finally {
    exportingReport.value = ''
  }
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
  if (key === BUSINESS_CATEGORY_KEYS.notesIncome)
    return t('stage5.dataCenter.overview.notesIncome')
  if (key === BUSINESS_CATEGORY_KEYS.notesExpense)
    return t('stage5.dataCenter.overview.notesExpense')
  if (key === BUSINESS_CATEGORY_KEYS.netRevenue)
    return t('stage5.dataCenter.overview.netRevenue')
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
  if (key === REVENUE_CATEGORY_KEYS.roomFee)
    return t('stage5.dataCenter.overview.taxIncludedRoomFee')
  if (key === REVENUE_CATEGORY_KEYS.deposit)
    return t('stage5.statistics.common.deposit')
  if (key === REVENUE_CATEGORY_KEYS.roomService)
    return t('stage5.dataCenter.overview.roomService')
  if (key === REVENUE_CATEGORY_KEYS.notesIncome)
    return t('stage5.dataCenter.overview.notesIncome')
  if (key === REVENUE_CATEGORY_KEYS.notesExpense)
    return t('stage5.dataCenter.overview.notesExpense')
  if (key === REVENUE_CATEGORY_KEYS.paymentRefund)
    return t('stage5.dataCenter.overview.paymentRefund')
  if (key === REVENUE_CATEGORY_KEYS.netIncome)
    return t('stage5.dataCenter.overview.netIncome')
  return category
}

// 营业概况相关数据
const totalRevenue = ref(0)
const roomFee = ref(0)
const deposit = ref(0)
const roomService = ref(0)
const businessNetRevenue = ref(0)

interface BusinessDetailItem {
  category: string
  total: number
  [key: string]: string | number
}

interface RevenueDetailItem {
  paymentMethod: string
  total: number
  [key: string]: string | number
}

const businessMetricCards = computed(() => [
  {
    key: 'totalRevenue',
    label: t('stage5.statistics.business.totalAccommodationRevenue'),
    value: totalRevenue.value,
    icon: businessCartIcon,
    primary: true,
  },
  {
    key: 'roomFee',
    label: t('stage5.statistics.common.roomFee'),
    value: roomFee.value,
    icon: businessHomeIcon,
    primary: false,
  },
  {
    key: 'deposit',
    label: t('stage5.statistics.common.deposit'),
    value: deposit.value,
    icon: businessDepositIcon,
    primary: false,
  },
  {
    key: 'roomService',
    label: t('stage5.dataCenter.overview.roomService'),
    value: roomService.value,
    icon: businessCustomerIcon,
    primary: false,
  },
  {
    key: 'netRevenue',
    label: t('stage5.dataCenter.overview.netRevenue'),
    value: businessNetRevenue.value,
    icon: businessDepositIcon,
    primary: false,
  },
])

const businessDetailData = ref<BusinessDetailItem[]>([])

const isVisibleBusinessCategory = (category: string) => {
  const key = resolveLabelKey(category, BUSINESS_CATEGORY_ALIASES)
  return (
    key !== BUSINESS_CATEGORY_KEYS.checkoutRefund &&
    key !== BUSINESS_CATEGORY_KEYS.notesIncome &&
    key !== BUSINESS_CATEGORY_KEYS.notesExpense
  )
}

// 流水汇总相关数据
const revenueSubTab = ref('payment')
const revenueTableTab = ref('payment-method')
const revenueTotal = ref(0)
const splitAccount = ref(0)
const actualReceived = ref(0)
const revenueTotalIncome = ref(0)
const revenueTotalExpense = ref(0)
const revenueNetIncome = ref(0)
const revenuePaymentStats = ref<RevenueSummaryDTO['paymentMethodStats']>([])
const revenueDailyRows = ref<RevenueSummaryDTO['dailyRevenues']>([])
const latestRevenueSummary = ref<RevenueSummaryDTO | null>(null)

const secondaryRevenueCards = computed(() => {
  const sortedStats = [...revenuePaymentStats.value]
    .filter((item) => Number(item.amount || 0) > 0)
    .sort((first, second) => Number(second.amount || 0) - Number(first.amount || 0))

  const first = sortedStats[0]
  const second = sortedStats[1]

  return [
    {
      label: first ? translatePaymentMethod(first.paymentMethod) : t('stage5.dataCenter.overview.actualReceived'),
      value: first ? first.amount : actualReceived.value,
    },
    {
      label: second ? translatePaymentMethod(second.paymentMethod) : t('stage5.dataCenter.overview.splitAccount'),
      value: second ? second.amount : splitAccount.value,
    },
  ]
})

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

const revenueTableData = ref<RevenueDetailItem[]>([])

// 款项分类相关数据
const categoryRevenue = ref(0)
const categoryIncome = ref(0)
const categoryExpense = ref(0)
const normalRevenue = ref(0)
const arRevenue = ref(0)

const categoryTableData = ref<RevenueDetailItem[]>([])

// 渠道汇总相关数据
const channelTableTab = ref('channel-fee')
const channelTableTabs = computed(() => [
  { key: 'channel-fee', label: t('stage5.statistics.channel.consumptionDetails') },
  { key: 'channel-nights', label: t('stage5.statistics.channel.nightsDetails') },
])

// 当前显示的表格数据(根据tab切换)
const channelTableData = ref<any[]>([])

// 销售汇总相关数据
const salesTotal = ref(0)
const searchKeyword = ref('')
const searchChannel = ref<number | ''>('')
const searchGuest = ref('')
const salesChannelOptions = ref<ChannelDTO[]>([])
const salesTableData = ref<SalesTableRow[]>([])
const salesCurrentPage = ref(1)
const salesPageSize = 10
const salesTableTotal = ref(0)

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

const setTabSuccess = (tab: OverviewTab, hasData: boolean) => {
  tabErrors.value[tab] = ''
  tabHasData.value[tab] = hasData
}

const setTabError = (tab: OverviewTab, message: string) => {
  tabErrors.value[tab] = message
  tabHasData.value[tab] = false
}

const hasPositiveValue = (values: number[]) => values.some((value) => Math.abs(toNumber(value)) > 0)

const hasBusinessData = (data: BusinessOverviewDTO) =>
  hasPositiveValue([
    data.totalRevenue,
    data.roomFee,
    data.deposit,
    data.roomServiceFee,
    data.netRevenue || 0,
  ]) ||
  Boolean(
    (data.categoryDistribution || []).some(
      (item) => isVisibleBusinessCategory(item.category) && Math.abs(toNumber(item.value)) > 0,
    ),
  ) ||
  Boolean(
    (data.consumptionTrend || []).some((item) =>
      hasPositiveValue([item.roomFee, item.deposit, item.roomServiceFee]),
    ),
  ) ||
  Boolean((data.consumptionDetails || []).some((detail) => isVisibleBusinessCategory(detail.category)))

const hasRevenueData = (data: RevenueSummaryDTO) =>
  hasPositiveValue([
    data.totalRevenue,
    data.totalIncome || 0,
    data.totalExpense || 0,
    data.netIncome || 0,
    data.splitAccount,
    data.actualReceived,
  ]) ||
  Boolean(data.paymentMethodStats?.length) ||
  Boolean(data.categoryStats?.length) ||
  Boolean(data.dailyRevenues?.length)

const hasChannelData = (data: ChannelSummaryDTO) =>
  hasPositiveValue([data.totalRevenue || 0, data.totalRoomNights || 0]) ||
  Boolean(data.revenueDistribution?.length) ||
  Boolean(data.nightsDistribution?.length) ||
  Boolean(data.channelDetails?.length)

const hasSalesData = (data: SalesSummaryDTO) =>
  hasPositiveValue([data.totalSales || 0]) ||
  Boolean(data.totalOrders) ||
  Boolean(data.totalRecords) ||
  Boolean(data.orderDetails?.length)

const loadChannelColorOverrides = async () => {
  if (!channelColorLoadPromise) {
    channelColorLoadPromise = (async () => {
      try {
        const response = await getAllChannels()
        if (response.success && Array.isArray(response.data)) {
          buildChannelColorOverrides(response.data)
          setSalesChannelOptions(response.data)
        }
      } catch (error) {
        console.warn('Failed to load channel colors for statistics charts', error)
      }
    })()
  }

  await channelColorLoadPromise
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
      totalRevenue.value = toNumber(data.totalRevenue)
      roomFee.value = toNumber(data.roomFee)
      deposit.value = toNumber(data.deposit)
      roomService.value = toNumber(data.roomServiceFee)
      businessNetRevenue.value = toNumber(data.netRevenue)

      // 更新表格数据
      businessDetailData.value = (data.consumptionDetails || [])
        .filter((detail) => isVisibleBusinessCategory(detail.category))
        .map(detail => ({
          category: translateBusinessCategory(detail.category),
          total: toNumber(detail.total),
          ...createDailyAmountCells(detail.dailyAmounts || []),
        }))
      setTabSuccess('business', hasBusinessData(data))

      // 重新初始化图表
      await nextTick()
      initBusinessPieChart(data)
      initBusinessBarChart(data)
    } else {
      const message = response.message || t('stage5.dataCenter.overview.loadBusinessFailed')
      setTabError('business', message)
      ElMessage.error(message)
    }
  } catch (error) {
    const message = t('stage5.dataCenter.overview.loadBusinessFailed')
    setTabError('business', message)
    console.error(message, error)
    ElMessage.error(message)
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
      latestRevenueSummary.value = data
      const dailyRevenues = data.dailyRevenues || []
      revenueDailyRows.value = dailyRevenues
      revenuePaymentStats.value = data.paymentMethodStats || []

      // 更新统计数据
      revenueTotal.value = toNumber(data.totalRevenue)
      splitAccount.value = toNumber(data.splitAccount)
      actualReceived.value = toNumber(data.actualReceived)
      revenueTotalIncome.value = toNumber(data.totalIncome)
      revenueTotalExpense.value = toNumber(data.totalExpense)
      revenueNetIncome.value = toNumber(data.netIncome)

      // 更新表格数据 - 支付方式
      const paymentRows: RevenueDetailItem[] = [
        {
          paymentMethod: t('stage5.statistics.revenue.totalIncome'),
          total: revenueTotalIncome.value || sumDailyRevenueField(dailyRevenues, 'totalIncome'),
          ...createDailyRevenueCells(dailyRevenues, 'totalIncome'),
        },
        {
          paymentMethod: t('stage5.dataCenter.overview.splitAccount'),
          total: splitAccount.value || sumDailyRevenueField(dailyRevenues, 'splitAccount'),
          ...createDailyRevenueCells(dailyRevenues, 'splitAccount'),
        },
        {
          paymentMethod: t('stage5.dataCenter.overview.actualReceived'),
          total: actualReceived.value || sumDailyRevenueField(dailyRevenues, 'actualReceived'),
          ...createDailyRevenueCells(dailyRevenues, 'actualReceived'),
        },
        {
          paymentMethod: t('stage5.dataCenter.overview.paymentRefund'),
          total: toNumber(data.paymentRefund) || sumDailyRevenueField(dailyRevenues, 'paymentRefund'),
          ...createDailyRevenueCells(dailyRevenues, 'paymentRefund'),
        },
      ]
      revenueTableData.value = paymentRows.filter((row) =>
        Math.abs(toNumber(row.total)) > 0 ||
        dateColumns.value.some((column) => Math.abs(toNumber(row[column.prop])) > 0),
      )

      // 更新表格数据 - 款项分类
      categoryRevenue.value = toNumber(data.totalRevenue)
      categoryIncome.value = revenueTotalIncome.value || categoryRevenue.value
      categoryExpense.value = revenueTotalExpense.value
      normalRevenue.value =
        toNumber(data.roomFee) + toNumber(data.deposit) + toNumber(data.roomServiceFee)
      arRevenue.value = resolveCategoryAmount(data.categoryStats || [], REVENUE_CATEGORY_KEYS.arMismatchRevenue)

      const categoryRows: RevenueDetailItem[] = [
        {
          paymentMethod: t('stage5.dataCenter.overview.taxIncludedRoomFee'),
          total: toNumber(data.roomFee) || sumDailyRevenueField(dailyRevenues, 'roomFee'),
          ...createDailyRevenueCells(dailyRevenues, 'roomFee'),
        },
        {
          paymentMethod: t('stage5.statistics.common.deposit'),
          total: toNumber(data.deposit) || sumDailyRevenueField(dailyRevenues, 'deposit'),
          ...createDailyRevenueCells(dailyRevenues, 'deposit'),
        },
        {
          paymentMethod: t('stage5.dataCenter.overview.roomService'),
          total: toNumber(data.roomServiceFee) || sumDailyRevenueField(dailyRevenues, 'roomServiceFee'),
          ...createDailyRevenueCells(dailyRevenues, 'roomServiceFee'),
        },
        {
          paymentMethod: t('stage5.dataCenter.overview.paymentRefund'),
          total: toNumber(data.paymentRefund) || sumDailyRevenueField(dailyRevenues, 'paymentRefund'),
          ...createDailyRevenueCells(dailyRevenues, 'paymentRefund'),
        },
        {
          paymentMethod: t('stage5.dataCenter.overview.netIncome'),
          total: revenueNetIncome.value || sumDailyRevenueField(dailyRevenues, 'netIncome'),
          ...createDailyRevenueCells(dailyRevenues, 'netIncome'),
        },
      ]
      categoryTableData.value = categoryRows.filter((row) =>
        Math.abs(toNumber(row.total)) > 0 ||
        dateColumns.value.some((column) => Math.abs(toNumber(row[column.prop])) > 0),
      )

      setTabSuccess('revenue', hasRevenueData(data))

      // 重新初始化图表
      await nextTick()
      if (revenueSubTab.value === 'payment') {
        initRevenueDistChart(data)
        initExpenseChart(data)
      } else {
        initCategoryDistChart(data)
        initCategoryExpenseChart(data)
      }
    } else {
      const message = response.message || t('stage5.dataCenter.overview.loadRevenueFailed')
      setTabError('revenue', message)
      ElMessage.error(message)
    }
  } catch (error) {
    const message = t('stage5.dataCenter.overview.loadRevenueFailed')
    setTabError('revenue', message)
    console.error(message, error)
    ElMessage.error(message)
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
    await loadChannelColorOverrides()
    const response = await getChannelSummary({
      startDate: startDate.value,
      endDate: endDate.value
    })

    if (response.success && response.data) {
      const data = response.data

      // 更新表格数据
      const channelFeeData = data.channelDetails.map((detail) => {
        const totalRevenue = detail.totalRevenue ?? detail.revenue ?? 0
        return {
          channel: detail.channelName,
          total: `¥${formatMoney(totalRevenue)}`,
          ...createChannelDailyCells(detail.dailyValues || [], 'revenue'),
        }
      })

      const channelNightsData = data.channelDetails.map((detail) => {
        const totalRoomNights = detail.totalRoomNights ?? detail.roomNights ?? 0
        return {
          channel: detail.channelName,
          total: totalRoomNights,
          ...createChannelDailyCells(detail.dailyValues || [], 'roomNights'),
        }
      })

      if (channelTableTab.value === 'channel-fee') {
        channelTableData.value = channelFeeData
      } else {
        channelTableData.value = channelNightsData
      }
      setTabSuccess('channel', hasChannelData(data))

      // 重新初始化图表
      await nextTick()
      initChannelRevenueChart(data)
      initChannelNightsChart(data)
      initChannelRevenueTrendChart(data)
      initChannelNightsTrendChart(data)
    } else {
      const message = response.message || t('stage5.dataCenter.overview.loadChannelFailed')
      setTabError('channel', message)
      ElMessage.error(message)
    }
  } catch (error) {
    const message = t('stage5.dataCenter.overview.loadChannelFailed')
    setTabError('channel', message)
    console.error(message, error)
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

/**
 * 加载销售汇总数据
 */
const loadSalesSummary = async (resetPage = false) => {
  // 检查日期参数是否有效
  if (!startDate.value || !endDate.value) {
    console.warn(t('stage5.dataCenter.overview.invalidDateRange'))
    return
  }

  try {
    loading.value = true
    if (resetPage) {
      salesCurrentPage.value = 1
    }
    await loadChannelColorOverrides()
    const response = await getSalesSummary({
      startDate: startDate.value,
      endDate: endDate.value,
      keyword: searchKeyword.value || undefined,
      channelId: searchChannel.value || undefined,
      customer: searchGuest.value || undefined,
      page: salesCurrentPage.value,
      pageSize: salesPageSize,
    })

    if (response.success && response.data) {
      const data = response.data

      // 更新统计数据
      salesTotal.value = toNumber(data.totalSales)
      const orderDetails = data.orderDetails || []
      salesTableTotal.value = data.totalRecords ?? data.totalOrders ?? orderDetails.length

      // 更新表格数据
      salesTableData.value = orderDetails.map(order => ({
        createdAt: order.createdAt,
        guestName: order.guestName,
        orderNumber: order.orderNumber,
        channelNumber: order.channelNumber,
        channel: order.channelName,
        customerName: order.customerName,
        phone: order.phone,
        amount: toNumber(order.amount),
        allocatedAmount: toNumber(order.allocatedAmount ?? order.amount),
        totalAmount: toNumber(order.totalAmount ?? order.amount),
      }))
      if (data.page && data.page !== salesCurrentPage.value) {
        salesCurrentPage.value = data.page
      }
      setTabSuccess('sales', hasSalesData(data))

      // 重新初始化图表
      await nextTick()
      initSalesTrendChart(data)
    } else {
      const message = response.message || t('stage5.dataCenter.overview.loadSalesFailed')
      setTabError('sales', message)
      ElMessage.error(message)
    }
  } catch (error) {
    const message = t('stage5.dataCenter.overview.loadSalesFailed')
    setTabError('sales', message)
    console.error(message, error)
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

/**
 * 根据当前标签页加载对应数据
 */
const loadCurrentTabData = () => {
  if (activeTab.value === 'revenue' && !canViewRevenue.value) {
    activeTab.value = 'business'
    ElMessage.warning(t('stage5.dataCenter.overview.revenuePermissionRequired'))
    return
  }
  if (activeTab.value === 'business') {
    loadBusinessOverview()
  } else if (activeTab.value === 'revenue') {
    loadRevenueSummary()
  } else if (activeTab.value === 'channel') {
    loadChannelSummary()
  } else if (activeTab.value === 'sales') {
    loadSalesSummary(true)
  }
}

const syncTabQuery = () => {
  if (route.query.tab === activeTab.value) {
    return
  }
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      tab: activeTab.value,
    },
  })
}

// ==================== 图表初始化函数 ====================

const createEmptyChartOption = (message = t('stage5.dataCenter.overview.noData')) => ({
  graphic: {
    type: 'text',
    left: 'center',
    top: 'middle',
    style: {
      text: message,
      fill: '#8a8f99',
      fontSize: 14,
      fontWeight: 500,
    },
  },
  xAxis: { show: false },
  yAxis: { show: false },
  series: [],
})

// 初始化营业概况饼图
const initBusinessPieChart = (data?: BusinessOverviewDTO) => {
  if (!businessPieChart.value) return

  // 销毁旧实例避免重复初始化
  if (businessPie) {
    businessPie.dispose()
  }

  businessPie = echarts.init(businessPieChart.value)

  const pieChartData = (data?.categoryDistribution || [])
    .filter((item) => isVisibleBusinessCategory(item.category))
    .map((item) => ({
      value: toNumber(item.value),
      name: translateBusinessCategory(item.category),
    }))
    .filter((item) => item.value > 0)
  if (!pieChartData.length) {
    businessPie.setOption(createEmptyChartOption())
    return
  }
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
  roomServiceFee: number
  deposit: number
}

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
      roomServiceFee: item.roomServiceFee || 0,
      deposit: item.deposit || 0,
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
      roomServiceFee: bucket.reduce((sum, item) => sum + (item.roomServiceFee || 0), 0),
      deposit: bucket.reduce((sum, item) => sum + (item.deposit || 0), 0),
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

  const barData = buildBusinessTrendBars(data?.consumptionTrend || [])
  if (!barData.length) {
    businessBar.setOption(createEmptyChartOption())
    return
  }
  const dates = barData.map((item) => item.label)
  const revenueData = barData.map(
    (item) =>
      item.roomFee +
      item.roomServiceFee +
      item.deposit,
  )
  const maxRevenue = Math.max(...revenueData, 1)
  const yMax = Math.ceil(maxRevenue / 10000) * 10000
  const xAxisLabelInterval = Math.max(0, Math.ceil(dates.length / 8) - 1)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter: (params: any) => {
        if (!params || !params.length) return ''
        const revenueItem = params.find((item: any) => item.seriesName === t('stage5.dataCenter.overview.netRevenue'))
        return [
          `<strong>${params[0].axisValue}</strong>`,
          `${revenueItem?.marker || ''}${t('stage5.dataCenter.overview.netRevenue')}&nbsp;&nbsp;¥${formatMoney(revenueItem?.value || 0)}`,
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
      data: [t('stage5.dataCenter.overview.netRevenue')],
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
        interval: (index: number) =>
          index === 0 || index === dates.length - 1 || index % (xAxisLabelInterval + 1) === 0,
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
        name: t('stage5.dataCenter.overview.netRevenue'),
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
    color: ['#5ea8f4'],
  }

  businessBar.setOption(option)
}

// 初始化流水分布饼图
const initRevenueDistChart = (data?: RevenueSummaryDTO) => {
  if (!revenueDistChart.value) return

  if (revenueDist) revenueDist.dispose()
  revenueDist = echarts.init(revenueDistChart.value)

  const chartData = (data?.paymentMethodStats || [])
    .map((stat) => ({
      value: toNumber(stat.amount),
      name: translatePaymentMethod(stat.paymentMethod),
    }))
    .filter((item) => Number(item.value) > 0)
  if (!chartData.length) {
    revenueDist.setOption(createEmptyChartOption())
    return
  }
  const visibleChartData = chartData
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
const initExpenseChart = (data?: RevenueSummaryDTO) => {
  if (!expenseChart.value) return

  if (expense) expense.dispose()
  expense = echarts.init(expenseChart.value)

  const chartData = (data?.expenseDistribution || [])
    .map((item) => ({
      value: toNumber(item.value),
      name: item.name,
    }))
    .filter((item) => item.value > 0)
  if (!chartData.length) {
    expense.setOption(createEmptyChartOption())
    return
  }
  const total = chartData.reduce((sum, item) => sum + item.value, 0)

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
        text: formatMoney(total),
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
        data: chartData,
        itemStyle: {
          color: '#dcecff',
        },
        label: {
          show: true,
          formatter: (params: any) => `${params.percent}% ${params.name}\n¥${formatMoney(params.value)}`,
        },
      },
    ],
  }

  expense.setOption(option)
}

// 初始化款项分类收款分布饼图
const initCategoryDistChart = (data?: RevenueSummaryDTO) => {
  if (!categoryDistChart.value) return

  if (categoryDistChart_instance) categoryDistChart_instance.dispose()
  categoryDistChart_instance = echarts.init(categoryDistChart.value)

  const chartData = (data?.incomeDistribution || [])
    .map((row) => ({
      value: toNumber(row.value),
      name: row.name,
    }))
    .filter((item) => item.value > 0)
  if (!chartData.length) {
    categoryDistChart_instance.setOption(createEmptyChartOption())
    return
  }
  const visibleChartData = chartData
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
const initCategoryExpenseChart = (data?: RevenueSummaryDTO) => {
  if (!categoryExpenseChart.value) return

  if (categoryExpenseChart_instance) categoryExpenseChart_instance.dispose()
  categoryExpenseChart_instance = echarts.init(categoryExpenseChart.value)

  const chartData = (data?.expenseDistribution || [])
    .map((item) => ({
      value: toNumber(item.value),
      name: item.name,
    }))
    .filter((item) => item.value > 0)
  if (!chartData.length) {
    categoryExpenseChart_instance.setOption(createEmptyChartOption())
    return
  }
  const total = chartData.reduce((sum, item) => sum + item.value, 0)

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
        text: formatMoney(total),
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
        data: chartData,
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

type ChannelChartItem = {
  value: number
  name: string
  percentage?: number
}

const formatChannelChartDate = (dateText: string) => {
  if (!dateText) return ''
  const parts = dateText.split('-')
  if (parts.length === 3) {
    return `${Number(parts[1])}/${Number(parts[2])}`
  }
  return dateText
}

const createChannelDonutOption = (
  title: string,
  chartData: ChannelChartItem[],
  valueFormatter: (value: number) => string,
) => {
  const visibleChartData = chartData.length ? chartData : [{ value: 0, name: title, percentage: 0 }]
  const colorNames = visibleChartData.map((item) => item.name)
  const colors = getChannelColors(colorNames)

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: '#ffffff',
      borderColor: '#eeeeee',
      borderWidth: 1,
      padding: [10, 12],
      textStyle: {
        color: '#111111',
        fontSize: 12,
      },
      formatter: (params: any) =>
        `${params.name}<br/>${valueFormatter(Number(params.value || 0))} (${params.percent}%)`,
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 14,
      textStyle: {
        color: '#555555',
        fontSize: 12,
      },
      data: colorNames,
    },
    series: [
      {
        name: title,
        type: 'pie',
        radius: ['52%', '74%'],
        center: ['50%', '44%'],
        minAngle: 3,
        avoidLabelOverlap: true,
        data: visibleChartData.map((item, index) => ({
          ...item,
          itemStyle: {
            color: colors[index],
          },
        })),
        itemStyle: {
          borderRadius: 0,
          borderColor: '#ffffff',
          borderWidth: 0,
        },
        label: {
          show: true,
          formatter: (params: any) => `${params.percent}%  ${params.name}\n${valueFormatter(Number(params.value || 0))}`,
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
}

const getChannelTrendNames = (trend: ChannelSummaryDTO['revenueTrend']) => {
  const names = new Set<string>()
  trend.forEach((item) => {
    item.channels.forEach((channel) => names.add(channel.channelName))
  })
  return Array.from(names)
}

const hexToRgba = (hex: string, alpha: number) => {
  const normalized = hex.replace('#', '')
  if (!/^[0-9a-f]{6}$/i.test(normalized)) return `rgba(30, 144, 247, ${alpha})`
  const value = Number.parseInt(normalized, 16)
  const red = (value >> 16) & 255
  const green = (value >> 8) & 255
  const blue = value & 255
  return `rgba(${red}, ${green}, ${blue}, ${alpha})`
}

const createChannelTrendOption = (
  trend: ChannelSummaryDTO['revenueTrend'],
  valueFormatter?: (value: number) => string,
) => {
  const dates = trend.map((item) => formatChannelChartDate(item.date))
  const rawDates = trend.map((item) => item.date)
  const channelNames = getChannelTrendNames(trend)
  const colors = getChannelColors(channelNames)
  const seriesData = channelNames.map((channelName, index) => {
    const color = colors[index]
    return {
      name: channelName,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      showSymbol: true,
      lineStyle: {
        width: 2,
        color,
      },
      itemStyle: {
        color: '#ffffff',
        borderColor: color,
        borderWidth: 2,
      },
      areaStyle: {
        opacity: 0.22,
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: hexToRgba(color, 0.34) },
            { offset: 1, color: hexToRgba(color, 0.06) },
          ],
        },
      },
      emphasis: {
        focus: 'series',
      },
      data: trend.map((item) => {
        const channelData = item.channels.find((channel) => channel.channelName === channelName)
        return channelData ? channelData.value : 0
      }),
    }
  })

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#ffffff',
      borderColor: '#eeeeee',
      borderWidth: 1,
      padding: [10, 12],
      textStyle: {
        color: '#111111',
        fontSize: 12,
      },
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: '#d8d8d8',
          type: 'dashed',
        },
      },
      formatter: (params: any[]) => {
        if (!params?.length) return ''
        const dateText = rawDates[params[0].dataIndex] || params[0].axisValue
        const rows = params.map((param) => {
          const value = Number(param.value || 0)
          const text = valueFormatter ? valueFormatter(value) : String(value)
          return `${param.marker}${param.seriesName}&nbsp;&nbsp;${text}`
        })
        return [`<strong>${dateText}</strong>`, ...rows].join('<br/>')
      },
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 9,
      itemHeight: 9,
      itemGap: 18,
      textStyle: {
        color: '#555555',
        fontSize: 12,
      },
      data: channelNames,
    },
    grid: {
      left: 48,
      right: 20,
      bottom: 54,
      top: 24,
      containLabel: false,
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisTick: {
        show: false,
      },
      axisLine: {
        lineStyle: {
          color: '#d8d8d8',
        },
      },
      axisLabel: {
        color: '#5c5c5c',
        fontSize: 12,
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#dedede',
          type: 'dashed',
        },
      },
    },
    yAxis: {
      type: 'value',
      min: 0,
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: '#5c5c5c',
        fontSize: 12,
        formatter: (value: number) => (valueFormatter ? valueFormatter(value).replace('¥', '') : value),
      },
      splitLine: {
        lineStyle: {
          color: '#dedede',
          type: 'dashed',
        },
      },
    },
    series: seriesData,
    color: colors,
  }
}

// 初始化渠道消费分布饼图
const initChannelRevenueChart = (data?: ChannelSummaryDTO) => {
  if (!channelRevenueChart.value) return

  if (channelRevenue) channelRevenue.dispose()
  channelRevenue = echarts.init(channelRevenueChart.value)

  const chartData = data ? data.revenueDistribution.map((item) => ({
    value: item.value,
    name: item.channelName,
    percentage: item.percentage,
  })) : []

  if (!chartData.filter((item) => toNumber(item.value) > 0).length) {
    channelRevenue.setOption(createEmptyChartOption())
    return
  }

  const option = createChannelDonutOption(
    t('stage5.dataCenter.overview.channelConsumptionDistribution'),
    chartData,
    (value) => `¥${formatMoney(value)}`,
  )

  channelRevenue.setOption(option)
}

// 初始化渠道间夜分布饼图
const initChannelNightsChart = (data?: ChannelSummaryDTO) => {
  if (!channelNightsChart.value) return

  if (channelNights) channelNights.dispose()
  channelNights = echarts.init(channelNightsChart.value)

  const chartData = data ? data.nightsDistribution.map((item) => ({
    value: item.value,
    name: item.channelName,
    percentage: item.percentage,
  })) : []

  if (!chartData.filter((item) => toNumber(item.value) > 0).length) {
    channelNights.setOption(createEmptyChartOption())
    return
  }

  const option = createChannelDonutOption(
    t('stage5.dataCenter.overview.channelNightsDistribution'),
    chartData,
    (value) => String(value),
  )

  channelNights.setOption(option)
}

// 初始化渠道消费趋势折线图
const initChannelRevenueTrendChart = (data?: ChannelSummaryDTO) => {
  if (!channelRevenueTrendChart.value) return

  if (channelRevenueTrend) channelRevenueTrend.dispose()
  channelRevenueTrend = echarts.init(channelRevenueTrendChart.value)

  const trend = data?.revenueTrend || []
  if (!trend.length) {
    channelRevenueTrend.setOption(createEmptyChartOption())
    return
  }
  const option = createChannelTrendOption(trend, (value) => `¥${formatMoney(value)}`)

  channelRevenueTrend.setOption(option)
}

// 初始化渠道间夜趋势图
const initChannelNightsTrendChart = (data?: ChannelSummaryDTO) => {
  if (!channelNightsTrendChart.value) return

  if (channelNightsTrend) channelNightsTrend.dispose()
  channelNightsTrend = echarts.init(channelNightsTrendChart.value)

  const trend = data?.nightsTrend || []
  if (!trend.length) {
    channelNightsTrend.setOption(createEmptyChartOption())
    return
  }
  const option = createChannelTrendOption(trend)

  channelNightsTrend.setOption(option)
}

// 初始化销售趋势折线图
const initSalesTrendChart = (data?: SalesSummaryDTO) => {
  if (!salesTrendChart.value) return

  if (salesTrend) salesTrend.dispose()
  salesTrend = echarts.init(salesTrendChart.value)

  // 使用API数据或默认数据
  const dates = data ? (data.dailySalesTrend || []).map(item => item.date) : []
  const salesData = data ? (data.dailySalesTrend || []).map(item => item.sales) : []
  if (!dates.length) {
    salesTrend.setOption(createEmptyChartOption())
    return
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#ffffff',
      borderColor: '#eeeeee',
      borderWidth: 1,
      padding: [10, 12],
      textStyle: {
        color: '#111111',
        fontSize: 12,
      },
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: '#d6dce4',
          type: 'dashed',
        },
      },
      formatter: (params: any) => {
        const param = params[0]
        return `${param.axisValue}<br/>${t('stage5.dataCenter.overview.salesAmount')}: ¥${param.value.toFixed(2)}`
      },
    },
    grid: {
      left: 64,
      right: 16,
      bottom: 44,
      top: 16,
      containLabel: false,
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisTick: {
        show: false,
      },
      axisLine: {
        lineStyle: {
          color: '#aeb6c2',
        },
      },
      axisLabel: {
        color: '#5c5c5c',
        fontSize: 12,
        margin: 10,
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#d8d8d8',
          type: 'dashed',
        },
      },
    },
    yAxis: {
      type: 'value',
      min: 0,
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: '#5c5c5c',
        fontSize: 12,
        formatter: (value: number) => `¥${Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })}`,
      },
      splitLine: {
        lineStyle: {
          color: '#d8d8d8',
          type: 'dashed',
        },
      },
    },
    series: [
      {
        name: t('stage5.dataCenter.overview.salesAmount'),
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        showSymbol: true,
        data: salesData,
        lineStyle: {
          width: 2,
          color: '#168bf8',
        },
        itemStyle: {
          color: '#ffffff',
          borderColor: '#168bf8',
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
              { offset: 0, color: 'rgba(22, 139, 248, 0.28)' },
              { offset: 1, color: 'rgba(22, 139, 248, 0.05)' },
            ],
          },
        },
      },
    ],
  }

  salesTrend.setOption(option)
}

// 流水汇总子标签页切换处理
const handleRevenueSubTabChange = async (tab: string) => {
  revenueSubTab.value = tab
  await nextTick()

  if (tab === 'payment') {
    initRevenueDistChart(latestRevenueSummary.value || undefined)
    initExpenseChart(latestRevenueSummary.value || undefined)
  } else if (tab === 'category') {
    initCategoryDistChart(latestRevenueSummary.value || undefined)
    initCategoryExpenseChart(latestRevenueSummary.value || undefined)
  }

  handleResize()
}

// 渠道表格标签切换处理
const handleChannelTableTabChange = (tab: string) => {
  channelTableTab.value = tab
  // 重新加载渠道汇总数据以更新表格
  loadChannelSummary()
}

let salesSearchTimer: ReturnType<typeof setTimeout> | null = null

const scheduleSalesSearch = () => {
  if (activeTab.value !== 'sales') return
  if (salesSearchTimer) {
    clearTimeout(salesSearchTimer)
  }
  salesSearchTimer = setTimeout(() => {
    loadSalesSummary(true)
    salesSearchTimer = null
  }, 400)
}

const handleSalesSearch = () => {
  if (activeTab.value !== 'sales') return
  if (salesSearchTimer) {
    clearTimeout(salesSearchTimer)
    salesSearchTimer = null
  }
  loadSalesSummary(true)
}

const handleSalesPageChange = (page: number) => {
  salesCurrentPage.value = page
  loadSalesSummary(false)
}

// 标签页切换处理
const handleTabChange = async () => {
  if (activeTab.value === 'revenue' && !canViewRevenue.value) {
    activeTab.value = 'business'
    ElMessage.warning(t('stage5.dataCenter.overview.revenuePermissionRequired'))
    return
  }
  syncTabQuery()
  await nextTick()

  if (activeTab.value === 'business') {
    initBusinessPieChart()
    initBusinessBarChart()
  } else if (activeTab.value === 'revenue') {
    // 根据当前子标签初始化对应图表
    if (revenueSubTab.value === 'payment') {
      initRevenueDistChart(latestRevenueSummary.value || undefined)
      initExpenseChart(latestRevenueSummary.value || undefined)
    } else if (revenueSubTab.value === 'category') {
      initCategoryDistChart(latestRevenueSummary.value || undefined)
      initCategoryExpenseChart(latestRevenueSummary.value || undefined)
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
  syncTabQuery()
  loadCurrentTabData()
})

watch(
  () => route.query.tab,
  () => {
    const nextTab = resolveTabFromQuery()
    if (nextTab === 'revenue' && !canViewRevenue.value) {
      activeTab.value = 'business'
      ElMessage.warning(t('stage5.dataCenter.overview.revenuePermissionRequired'))
      return
    }
    if (nextTab !== activeTab.value) {
      activeTab.value = nextTab
    }
  },
)

watch(canViewRevenue, (allowed) => {
  if (!allowed && activeTab.value === 'revenue') {
    activeTab.value = 'business'
    ElMessage.warning(t('stage5.dataCenter.overview.revenuePermissionRequired'))
  }
})

// 语言切换后刷新当前页数据和图表文案
watch(locale, () => {
  loadCurrentTabData()
})

watch(searchKeyword, () => {
  scheduleSalesSearch()
})

watch(searchGuest, () => {
  scheduleSalesSearch()
})

onMounted(() => {
  if (activeTab.value === 'revenue' && !canViewRevenue.value) {
    activeTab.value = 'business'
  }
  // 初始化日期为今天
  updateDateRange(dateType.value)

  // 加载初始数据
  loadCurrentTabData()
  loadChannelColorOverrides()

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (salesSearchTimer) {
    clearTimeout(salesSearchTimer)
  }
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

.tab-state-alert {
  margin-bottom: 10px;
}

.tab-empty {
  min-height: 320px;
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
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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

.channel-charts-row {
  gap: 10px;
  margin-bottom: 10px;
}

.channel-chart-card {
  min-height: 430px;
  padding: 20px 22px 16px;
  background: #ffffff;
  border: none;
  border-radius: 4px;
}

.channel-donut-card {
  min-height: 430px;
}

.channel-trend-card {
  min-height: 430px;
}

.channel-chart-title {
  margin: 0 0 14px;
  color: #0f0f0f;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.channel-donut-chart {
  height: 352px;
}

.channel-trend-chart {
  height: 352px;
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
  margin-bottom: 10px;
}

.sales-summary-card {
  display: flex;
  width: 420px;
  min-height: 132px;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
  padding: 22px 20px 20px;
  overflow: hidden;
  border-radius: 4px;
  background: linear-gradient(135deg, #168bf8 0%, #68b8ff 100%);
  color: #ffffff;
}

.sales-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.sales-card-label {
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.sales-card-icon {
  display: inline-flex;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
}

.sales-card-icon img {
  display: block;
  width: 31px;
  height: 31px;
  object-fit: contain;
}

.sales-card-value {
  color: #ffffff;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
}

.chart-card.full-width.sales-trend-card {
  min-height: 430px;
  margin-bottom: 10px;
  padding: 16px 22px 18px;
  border: none;
  border-radius: 0;
  background: #ffffff;
}

.sales-chart-title {
  margin: 0 0 10px;
  color: #0f0f0f;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.sales-chart-title .chart-subtitle {
  color: #909399;
  font-size: 12px;
  font-weight: 400;
  margin-left: 8px;
}

.sales-trend-chart {
  height: 350px;
}

.sales-table-section {
  min-height: 350px;
  margin-top: 0;
  padding: 18px 22px 24px;
  background: #ffffff;
  border-radius: 4px;
}

.sales-table-header {
  align-items: center;
  margin-bottom: 10px;
}

.sales-table-section .table-title {
  color: #0d0d0d;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.sales-export-button {
  min-width: 138px;
}

.sales-search-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 10px;
}

.sales-search-input {
  width: 330px;
  flex: 0 0 330px;
}

.sales-filter-select {
  width: 190px;
  flex: 0 0 190px;
}

.sales-search-section :deep(.el-input__wrapper),
.sales-search-section :deep(.el-select__wrapper) {
  min-height: 32px;
  border-radius: 5px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.sales-search-section :deep(.el-input__wrapper:hover),
.sales-search-section :deep(.el-input__wrapper.is-focus),
.sales-search-section :deep(.el-select__wrapper:hover),
.sales-search-section :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #87bdf6 inset;
}

.sales-search-section :deep(.el-input__inner),
.sales-search-section :deep(.el-select__selected-item) {
  color: #5f6670;
  font-size: 13px;
  font-weight: 400;
}

.sales-detail-table {
  --el-table-border-color: #e6e6e6;
  --el-table-header-bg-color: #fafafa;
  --el-table-row-hover-bg-color: #f9fbff;
  border: 1px solid #e6e6e6;
  border-bottom: none;
  background: #ffffff;
}

.sales-detail-table :deep(.el-table__inner-wrapper::before) {
  background-color: #e6e6e6;
}

.sales-detail-table :deep(th.el-table__cell) {
  height: 34px;
  background: #fafafa !important;
  border-right: 1px solid #e6e6e6;
  border-bottom: 1px solid #e6e6e6;
  color: #333333;
  font-size: 13px;
  font-weight: 600;
}

.sales-detail-table :deep(td.el-table__cell) {
  height: 52px;
  border-right: 1px solid #e6e6e6;
  border-bottom: 1px solid #e6e6e6;
  color: #333333;
  font-size: 13px;
  font-weight: 400;
}

.sales-detail-table :deep(th.el-table__cell:last-child),
.sales-detail-table :deep(td.el-table__cell:last-child) {
  border-right: none;
}

.sales-detail-table :deep(.cell) {
  padding: 0 10px;
  line-height: 1.35;
}

.sales-order-number {
  color: #333333;
  font-size: 13px;
  font-weight: 400;
}

.sales-channel-number {
  margin-top: 3px;
  color: #333333;
  font-size: 13px;
  font-weight: 400;
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

.channel-table-section {
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

.channel-table-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  column-gap: 16px;
  margin-bottom: 28px;
}

.channel-table-section .table-title {
  color: #0d0d0d;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
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

.channel-table-tabs {
  justify-self: center;
  gap: 10px;
  margin-bottom: 0;
}

.channel-table-tabs :deep(.el-button) {
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

.channel-table-tabs :deep(.el-button.active),
.channel-table-tabs :deep(.el-button:hover) {
  border-color: #1e90f7;
  background: #1e90f7;
  color: #ffffff;
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

.channel-detail-table {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: #fafafa;
  --el-table-row-hover-bg-color: #f9fbff;
}

.channel-detail-table :deep(.el-table__inner-wrapper::before),
.channel-detail-table :deep(.el-table__border-left-patch) {
  display: none;
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

.sales-table-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  column-gap: 16px;
}

.sales-table-footer :deep(.el-pagination) {
  justify-self: center;
}

.sales-table-footer .total-amount {
  justify-self: end;
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
