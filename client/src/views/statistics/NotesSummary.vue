<template>
  <StatisticsLayout>
    <div class="notes-summary-content" v-loading="loading">
      <div class="date-selector">
        <el-select v-model="dateType" class="business-quick-select">
          <el-option :label="t('stage5.common.date.today')" value="today" />
          <el-option :label="t('stage5.common.date.yesterday')" value="yesterday" />
          <el-option :label="t('stage5.common.date.thisWeek')" value="week" />
          <el-option :label="t('stage5.common.date.thisMonth')" value="month" />
        </el-select>
        <el-date-picker
          v-model="notesDateRange"
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
        <el-button type="primary" class="query-button" @click="handleQuery">
          {{ t('stage5.common.actions.query') }}
        </el-button>
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

      <div v-if="contentReady" class="summary-cards">
        <div v-for="card in summaryCards" :key="card.key" class="summary-card">
          <div class="card-content">
            <div class="card-label">{{ card.label }}</div>
            <div class="card-value">{{ currencySymbol }}{{ formatMoney(card.value) }}</div>
          </div>
          <div class="card-icon">
            <img :src="card.icon" :alt="card.label" />
          </div>
        </div>
      </div>

      <div v-if="contentReady" class="statistics-heading">
        <h2>{{ t('stage5.statistics.notes.incomeExpenseStats') }}</h2>
        <div class="mode-switch">
          <el-button
            v-for="tab in statisticsTabs"
            :key="tab.name"
            :class="{ active: activeTab === tab.name }"
            @click="activeTab = tab.name"
          >
            {{ tab.label }}
          </el-button>
        </div>
      </div>

      <div v-if="contentReady" class="statistics-section">
        <div class="charts-row">
          <div class="chart-wrapper income-chart-card">
            <div class="chart-title">{{ t('stage5.statistics.notes.totalIncome') }}</div>
            <div class="income-arc-chart">
              <div class="income-arc-stage">
                <svg class="income-arc-svg" viewBox="0 0 560 320" aria-hidden="true">
                  <path
                    v-for="segment in incomeGaugeSegments"
                    :key="segment.index"
                    class="income-arc-segment"
                    :d="segment.path"
                    :fill="segment.color"
                  />
                </svg>
                <div class="income-arc-amount">
                  {{ currencySymbol }} {{ formatMoney(activeIncomeTotal) }}
                </div>
                <div class="income-arc-compare">
                  <span class="income-arc-previous">
                    {{ t('stage5.statistics.notes.previousMonth') }}
                    <strong>{{ incomeComparisonPreviousText }}</strong>
                  </span>
                  <span
                    v-if="incomeComparisonChangeText"
                    class="income-arc-growth"
                    :class="`income-arc-growth-${incomeComparison.trend}`"
                  >
                    {{ incomeComparisonChangeText }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="chart-wrapper expense-chart-card">
            <div class="chart-title">{{ t('stage5.statistics.notes.totalExpense') }}</div>
            <div
              v-show="activeTab === 'byProject'"
              ref="expenseProjectChartRef"
              class="chart-canvas expense-donut-chart"
            ></div>
            <div
              v-show="activeTab === 'byPaymentMethod'"
              ref="expensePaymentChartRef"
              class="chart-canvas expense-donut-chart"
            ></div>
          </div>
        </div>
      </div>

      <div v-if="contentReady" class="details-section">
        <div class="details-header">
          <h3>
            {{ t('stage5.statistics.notes.details') }}
            {{ t('stage5.statistics.common.detailsPeriod', { period: formatDateRange }) }}
          </h3>
          <div class="header-actions">
            <el-select
              v-model="filterType"
              class="detail-filter-select"
              :placeholder="t('stage5.common.filters.type')"
            >
              <el-option :label="t('stage5.common.filters.all')" value="all" />
              <el-option :label="t('stage5.statistics.notes.income')" value="income" />
              <el-option :label="t('stage5.statistics.notes.expense')" value="expense" />
            </el-select>
            <el-button
              type="primary"
              class="export-button"
              :loading="exporting"
              @click="handleExport"
            >
              {{ t('stage5.common.actions.exportDetails') }}
            </el-button>
          </div>
        </div>

        <el-table :data="pagedTableData" border class="details-table">
          <el-table-column prop="datetime" :label="t('stage5.common.fields.time')" min-width="160" />
          <el-table-column prop="type" :label="t('stage5.common.fields.type')" width="100">
            <template #default="{ row }">
              <el-tag
                class="type-tag"
                :class="row.type === 'income' ? 'type-tag-income' : 'type-tag-expense'"
                effect="plain"
              >
                {{
                  row.type === 'income'
                    ? t('stage5.statistics.notes.income')
                    : t('stage5.statistics.notes.expense')
                }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="category" :label="t('stage5.common.fields.project')" min-width="100" />
          <el-table-column prop="amount" :label="t('stage5.common.fields.amount')" min-width="150">
            <template #default="{ row }">
              <span
                :class="{
                  'income-amount': row.type === 'income',
                  'expense-amount': row.type === 'expense',
                }"
              >
                {{ currencySymbol }}{{ formatMoney(row.amount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            prop="paymentMethod"
            :label="t('stage5.statistics.notes.paymentReceivedMethod')"
            min-width="120"
          />
          <el-table-column
            prop="roomNumber"
            :label="t('stage5.statistics.notes.relatedRoom')"
            min-width="100"
          />
          <el-table-column
            prop="voucher"
            :label="t('stage5.common.fields.voucher')"
            min-width="150"
          >
            <template #default="{ row }">
              <el-button v-if="row.voucherCount > 0" link type="primary" @click="handleViewVoucher(row)">
                {{ t('stage5.common.actions.view') }}({{ row.voucherCount }})
              </el-button>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column
            prop="notes"
            :label="t('stage5.common.fields.note')"
            min-width="220"
            show-overflow-tooltip
          />
        </el-table>

        <div class="table-footer">
          <div class="footer-stats">
            <span>{{ t('stage5.statistics.notes.recordsTotal', { count: filteredTableData.length }) }}</span>
            <span>
              {{ t('stage5.statistics.notes.netIncome') }}:
              <strong class="net-income-value">{{ currencySymbol }}{{ formatMoney(netIncomeAmount) }}</strong>
            </span>
            <span>
              {{ t('stage5.statistics.notes.totalIncome') }}:
              <strong class="income-value">{{ currencySymbol }}{{ formatMoney(totalIncomeAmount) }}</strong>
            </span>
            <span>
              {{ t('stage5.statistics.notes.totalExpense') }}:
              <strong class="expense-value">{{ currencySymbol }}{{ formatMoney(totalExpenseAmount) }}</strong>
            </span>
          </div>
          <div class="pagination-group">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="filteredTableData.length"
              :page-sizes="[10, 20, 50, 100]"
              layout="sizes, prev, pager, next"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
            <span class="page-total-text">
              {{ t('stage5.statistics.notes.pageTotal', { count: totalPages }) }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import StatisticsLayout from './StatisticsLayout.vue'
import {
  exportNotesReport,
  getNotesStatistics,
  getNotesList,
  type NoteDTO,
  type NoteType,
  type NotesStatisticsDTO,
} from '@/api/notes'
import notesNetIncomeIcon from '@/assets/icons/statistics/notes-net-income.png'
import notesTotalExpenseIcon from '@/assets/icons/statistics/notes-total-expense.png'
import businessDepositIcon from '@/assets/icons/statistics/business-deposit.png'
import {
  addDaysToYmd,
  getStoreTodayYmd,
  getYmdMonthStart,
  getYmdWeekStart,
} from '@/utils/storeDateTime'
import { getStatisticsReportErrorMessage, saveBlobDownload } from '@/api/statistics'

type NotesChartItem = {
  name: string
  value: number
}

type NotesTabName = 'byProject' | 'byPaymentMethod'
type IncomeGaugeSegment = {
  index: number
  path: string
  color: string
}
type IncomeComparisonTrend = 'up' | 'down' | 'flat'
type IncomeComparison = {
  previousAmount: number | null
  changePercent: number | null
  trend: IncomeComparisonTrend
}

const { t } = useI18n()
const currencySymbol = '\u00a5'

const today = getStoreTodayYmd()
const dateType = ref('today')
const startDate = ref(today)
const endDate = ref(today)
const activeTab = ref<NotesTabName>('byProject')
const filterType = ref('all')
const currentPage = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const exporting = ref(false)
const loadError = ref('')

const summaryStats = ref({
  netIncome: 0,
  totalIncome: 0,
  totalExpense: 0,
})

const incomeByProject = ref<NotesChartItem[]>([
  { name: t('stage5.statistics.notes.roomFeeIncome'), value: 0 },
  { name: t('stage5.statistics.notes.depositIncome'), value: 0 },
  { name: t('stage5.statistics.notes.compensationIncome'), value: 0 },
  { name: t('stage5.statistics.notes.otherIncome'), value: 0 },
])

const expenseByProject = ref<NotesChartItem[]>([
  { name: t('stage5.statistics.notes.roomRepair'), value: 0 },
  { name: t('stage5.statistics.notes.cleaningFee'), value: 0 },
  { name: t('stage5.statistics.notes.suppliesPurchase'), value: 0 },
  { name: t('stage5.statistics.notes.otherExpense'), value: 0 },
])

const incomeByPayment = ref<NotesChartItem[]>([
  { name: t('stage5.statistics.notes.cash'), value: 0 },
  { name: t('stage5.statistics.notes.alipay'), value: 0 },
  { name: t('stage5.statistics.notes.wechat'), value: 0 },
  { name: t('stage5.statistics.notes.bankCard'), value: 0 },
])

const expenseByPayment = ref<NotesChartItem[]>([
  { name: t('stage5.statistics.notes.cash'), value: 0 },
  { name: t('stage5.statistics.notes.alipay'), value: 0 },
  { name: t('stage5.statistics.notes.wechat'), value: 0 },
  { name: t('stage5.statistics.notes.bankCard'), value: 0 },
])

const tableData = ref<any[]>([])

let expenseProjectChart: echarts.ECharts | null = null
let expensePaymentChart: echarts.ECharts | null = null

const expenseProjectChartRef = ref<HTMLElement>()
const expensePaymentChartRef = ref<HTMLElement>()

const incomeGaugeSegmentCount = 13
const incomeGaugeActiveSegmentCount = 11
const incomeGaugeEmptyColor = '#d7d7d7'
const incomeGaugePalette = ['#0c82f7', '#168af7', '#3aa0f6', '#6eb9f4', '#bcdcff']

const incomeComparison = ref<IncomeComparison>({
  previousAmount: null,
  changePercent: null,
  trend: 'flat',
})

const notesDateRange = computed<string[]>({
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

const formatDateRange = computed(() => {
  if (startDate.value === endDate.value) {
    return startDate.value
  }
  return t('stage5.common.date.dateRange', { start: startDate.value, end: endDate.value })
})

const statisticsTabs = computed(() => [
  { name: 'byProject' as const, label: t('stage5.statistics.notes.byProject') },
  { name: 'byPaymentMethod' as const, label: t('stage5.statistics.notes.byPaymentMethod') },
])

const summaryCards = computed(() => [
  {
    key: 'netIncome',
    label: t('stage5.statistics.notes.netIncome'),
    value: summaryStats.value.netIncome,
    icon: notesNetIncomeIcon,
  },
  {
    key: 'totalIncome',
    label: t('stage5.statistics.notes.totalIncome'),
    value: summaryStats.value.totalIncome,
    icon: businessDepositIcon,
  },
  {
    key: 'totalExpense',
    label: t('stage5.statistics.notes.totalExpense'),
    value: summaryStats.value.totalExpense,
    icon: notesTotalExpenseIcon,
  },
])

const filteredTableData = computed(() => {
  if (filterType.value === 'all') {
    return tableData.value
  }
  return tableData.value.filter((item) => item.type === filterType.value)
})

const pagedTableData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredTableData.value.slice(start, start + pageSize.value)
})

const totalIncomeAmount = computed(() => {
  return filteredTableData.value
    .filter((item) => item.type === 'income')
    .reduce((sum, item) => sum + item.amount, 0)
})

const totalExpenseAmount = computed(() => {
  return filteredTableData.value
    .filter((item) => item.type === 'expense')
    .reduce((sum, item) => sum + item.amount, 0)
})

const netIncomeAmount = computed(() => totalIncomeAmount.value - totalExpenseAmount.value)
const totalPages = computed(() => Math.ceil(filteredTableData.value.length / pageSize.value))
const hasNotesData = computed(
  () =>
    summaryStats.value.totalIncome > 0 ||
    summaryStats.value.totalExpense > 0 ||
    tableData.value.length > 0,
)
const showEmpty = computed(() => !loading.value && !loadError.value && !hasNotesData.value)
const contentReady = computed(() => !loadError.value && !showEmpty.value)
const activeIncomeData = computed(() =>
  activeTab.value === 'byProject' ? incomeByProject.value : incomeByPayment.value,
)
const activeIncomeTotal = computed(() => getChartTotal(activeIncomeData.value))
const incomeComparisonPreviousText = computed(() =>
  incomeComparison.value.previousAmount === null
    ? t('stage5.statistics.notes.comparisonUnavailable')
    : formatMoney(incomeComparison.value.previousAmount),
)
const incomeComparisonChangeText = computed(() => {
  if (incomeComparison.value.changePercent === null) return ''

  const trendSymbolMap: Record<IncomeComparisonTrend, string> = {
    up: '+',
    down: '-',
    flat: '',
  }

  return `${trendSymbolMap[incomeComparison.value.trend]}${formatPercentValue(
    incomeComparison.value.changePercent,
  )}`
})

const polarToPoint = (centerX: number, centerY: number, radius: number, angle: number) => {
  const radians = (angle * Math.PI) / 180
  return {
    x: centerX + Math.cos(radians) * radius,
    y: centerY + Math.sin(radians) * radius,
  }
}

const createArcSegmentPath = (
  startAngle: number,
  endAngle: number,
  outerRadius: number,
  innerRadius: number,
) => {
  const centerX = 280
  const centerY = 258
  const segmentAngle = endAngle - startAngle
  const outerInset = Math.min(0.55, segmentAngle * 0.06)
  const innerInset = Math.min(2.8, segmentAngle * 0.24)
  const radiusInset = 6
  const outerStart = polarToPoint(centerX, centerY, outerRadius, startAngle + outerInset)
  const outerEnd = polarToPoint(centerX, centerY, outerRadius, endAngle - outerInset)
  const outerEndCorner = polarToPoint(centerX, centerY, outerRadius, endAngle)
  const endOuterSide = polarToPoint(centerX, centerY, outerRadius - radiusInset, endAngle)
  const endInnerSide = polarToPoint(centerX, centerY, innerRadius + radiusInset, endAngle)
  const innerEndCorner = polarToPoint(centerX, centerY, innerRadius, endAngle)
  const innerEnd = polarToPoint(centerX, centerY, innerRadius, endAngle - innerInset)
  const innerStart = polarToPoint(centerX, centerY, innerRadius, startAngle + innerInset)
  const innerStartCorner = polarToPoint(centerX, centerY, innerRadius, startAngle)
  const startInnerSide = polarToPoint(centerX, centerY, innerRadius + radiusInset, startAngle)
  const startOuterSide = polarToPoint(centerX, centerY, outerRadius - radiusInset, startAngle)
  const outerStartCorner = polarToPoint(centerX, centerY, outerRadius, startAngle)
  const largeArc = endAngle - startAngle > 180 ? 1 : 0

  return [
    `M ${outerStart.x.toFixed(2)} ${outerStart.y.toFixed(2)}`,
    `A ${outerRadius} ${outerRadius} 0 ${largeArc} 1 ${outerEnd.x.toFixed(2)} ${outerEnd.y.toFixed(2)}`,
    `Q ${outerEndCorner.x.toFixed(2)} ${outerEndCorner.y.toFixed(2)} ${endOuterSide.x.toFixed(2)} ${endOuterSide.y.toFixed(2)}`,
    `L ${endInnerSide.x.toFixed(2)} ${endInnerSide.y.toFixed(2)}`,
    `Q ${innerEndCorner.x.toFixed(2)} ${innerEndCorner.y.toFixed(2)} ${innerEnd.x.toFixed(2)} ${innerEnd.y.toFixed(2)}`,
    `A ${innerRadius} ${innerRadius} 0 ${largeArc} 0 ${innerStart.x.toFixed(2)} ${innerStart.y.toFixed(2)}`,
    `Q ${innerStartCorner.x.toFixed(2)} ${innerStartCorner.y.toFixed(2)} ${startInnerSide.x.toFixed(2)} ${startInnerSide.y.toFixed(2)}`,
    `L ${startOuterSide.x.toFixed(2)} ${startOuterSide.y.toFixed(2)}`,
    `Q ${outerStartCorner.x.toFixed(2)} ${outerStartCorner.y.toFixed(2)} ${outerStart.x.toFixed(2)} ${outerStart.y.toFixed(2)}`,
    'Z',
  ].join(' ')
}

const incomeGaugeSegments = computed<IncomeGaugeSegment[]>(() => {
  const segmentColors = createIncomeGaugeSegmentColors(activeIncomeData.value)
  const startAngle = 180
  const endAngle = 360
  const gap = 3.4
  const step = (endAngle - startAngle) / incomeGaugeSegmentCount

  return segmentColors.map((color, index) => {
    const segmentStart = startAngle + index * step + gap / 2
    const segmentEnd = startAngle + (index + 1) * step - gap / 2
    return {
      index,
      path: createArcSegmentPath(segmentStart, segmentEnd, 250, 164),
      color,
    }
  })
})

const formatMoney = (value: number) => {
  const normalizedValue = Number(value) || 0
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(normalizedValue)
}

const formatPercentValue = (value: number) =>
  `${Math.abs(Number(value) || 0).toFixed(1)}%`

const getPositiveChartData = (data: NotesChartItem[]) => data.filter((item) => Number(item.value) > 0)

const getChartTotal = (data: NotesChartItem[]) =>
  getPositiveChartData(data).reduce((sum, item) => sum + item.value, 0)

const createIncomeGaugeSegmentColors = (data: NotesChartItem[]) => {
  const visibleData = getPositiveChartData(data)
  const total = getChartTotal(data)

  if (!visibleData.length || total <= 0) {
    return Array.from({ length: incomeGaugeSegmentCount }, () => incomeGaugeEmptyColor)
  }

  const allocations = visibleData.map((item, index) => {
    const exactCount = (Number(item.value) / total) * incomeGaugeActiveSegmentCount
    const count = Math.floor(exactCount)
    return {
      count,
      remainder: exactCount - count,
      color: incomeGaugePalette[index % incomeGaugePalette.length],
    }
  })

  let assignedCount = allocations.reduce((sum, item) => sum + item.count, 0)

  while (assignedCount < incomeGaugeActiveSegmentCount) {
    let targetIndex = 0
    for (let index = 1; index < allocations.length; index += 1) {
      if (allocations[index].remainder > allocations[targetIndex].remainder) {
        targetIndex = index
      }
    }
    allocations[targetIndex].count += 1
    allocations[targetIndex].remainder = -1
    assignedCount += 1
  }

  if (allocations.length <= incomeGaugeActiveSegmentCount) {
    allocations.forEach((allocation) => {
      if (allocation.count > 0) return

      let donorIndex = -1
      allocations.forEach((candidate, index) => {
        if (
          candidate.count > 1 &&
          (donorIndex === -1 || candidate.count > allocations[donorIndex].count)
        ) {
          donorIndex = index
        }
      })

      if (donorIndex >= 0) {
        allocations[donorIndex].count -= 1
        allocation.count = 1
      }
    })
  }

  const colors = allocations.flatMap((allocation) =>
    Array.from({ length: allocation.count }, () => allocation.color),
  )

  const activeColors = colors.slice(0, incomeGaugeActiveSegmentCount)
  const emptySegmentCount = incomeGaugeSegmentCount - activeColors.length

  return [
    ...activeColors,
    ...Array.from({ length: emptySegmentCount }, () => incomeGaugeEmptyColor),
  ]
}

const createDonutChartOption = (data: NotesChartItem[], total: number): EChartsOption => {
  const visibleData = getPositiveChartData(data)
  const hasData = visibleData.length > 0 && total > 0
  const chartData = hasData ? visibleData : [{ name: t('stage5.common.empty.noData'), value: 1 }]

  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) =>
        hasData
          ? `${params.name}<br/>${currencySymbol}${formatMoney(params.value)} (${params.percent}%)`
          : '',
    },
    legend: {
      show: hasData,
      bottom: 6,
      left: 'center',
      itemWidth: 9,
      itemHeight: 9,
      icon: 'circle',
      itemGap: 8,
      textStyle: {
        color: '#555f70',
        fontSize: 13,
      },
    },
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '35%',
        style: {
          text: `${currencySymbol}${formatMoney(total)}`,
          fill: '#050505',
          fontSize: 30,
          fontWeight: 600,
        },
      },
    ],
    series: [
      {
        type: 'pie',
        radius: ['52%', '74%'],
        center: ['50%', '40%'],
        minAngle: hasData ? 8 : 360,
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: '#ffffff',
          borderWidth: 0,
        },
        label: {
          show: hasData,
          formatter: (params: any) =>
            `{name|${params.name}} {percent|${Math.round(params.percent)}%}\n{value|${formatMoney(
              params.value,
            )}}`,
          rich: {
            name: {
              color: '#666d78',
              fontSize: 11,
              lineHeight: 16,
            },
            percent: {
              color: '#5f7df5',
              fontSize: 11,
              fontWeight: 700,
              lineHeight: 16,
            },
            value: {
              color: '#4d8fff',
              fontSize: 11,
              lineHeight: 16,
            },
          },
        },
        labelLine: {
          show: hasData,
          length: 28,
          length2: 32,
          lineStyle: {
            color: '#6690ff',
            width: 1,
          },
        },
        data: chartData,
        color: hasData ? ['#5b79f5', '#dceaff', '#77baf6', '#b8d7fa', '#8fa8ff'] : ['#d7d7d7'],
      },
    ],
  }
}

const initCharts = () => {
  nextTick(() => {
    if (activeTab.value === 'byProject') {
      if (expenseProjectChartRef.value) {
        expenseProjectChart?.dispose()
        expenseProjectChart = echarts.init(expenseProjectChartRef.value)
        const expenseTotal = getChartTotal(expenseByProject.value)
        expenseProjectChart.setOption(createDonutChartOption(expenseByProject.value, expenseTotal))
      }
    } else {
      if (expensePaymentChartRef.value) {
        expensePaymentChart?.dispose()
        expensePaymentChart = echarts.init(expensePaymentChartRef.value)
        const expenseTotal = getChartTotal(expenseByPayment.value)
        expensePaymentChart.setOption(createDonutChartOption(expenseByPayment.value, expenseTotal))
      }
    }
  })
}

const updateCharts = () => {
  if (activeTab.value === 'byProject') {
    const expenseTotal = getChartTotal(expenseByProject.value)
    expenseProjectChart?.setOption(createDonutChartOption(expenseByProject.value, expenseTotal))
  } else {
    const expenseTotal = getChartTotal(expenseByPayment.value)
    expensePaymentChart?.setOption(createDonutChartOption(expenseByPayment.value, expenseTotal))
  }
}

const updateDateRange = (type: string) => {
  const currentToday = getStoreTodayYmd()
  switch (type) {
    case 'today':
      startDate.value = currentToday
      endDate.value = currentToday
      break
    case 'yesterday': {
      const yesterday = addDaysToYmd(currentToday, -1)
      startDate.value = yesterday
      endDate.value = yesterday
      break
    }
    case 'week':
      startDate.value = getYmdWeekStart(currentToday)
      endDate.value = currentToday
      break
    case 'month':
      startDate.value = getYmdMonthStart(currentToday)
      endDate.value = currentToday
      break
    default:
      break
  }
}

const applyNotesStats = (stats: NotesStatisticsDTO) => {
  summaryStats.value = {
    netIncome: stats.netIncome,
    totalIncome: stats.totalIncome,
    totalExpense: stats.totalExpense,
  }

  incomeByProject.value = stats.incomeByProject.map((item) => ({
    name: item.name,
    value: item.value,
  }))

  expenseByProject.value = stats.expenseByProject.map((item) => ({
    name: item.name,
    value: item.value,
  }))

  incomeByPayment.value = stats.incomeByPayment.map((item) => ({
    name: item.name,
    value: item.value,
  }))

  expenseByPayment.value = stats.expenseByPayment.map((item) => ({
    name: item.name,
    value: item.value,
  }))
}

const loadData = async () => {
  try {
    loading.value = true
    loadError.value = ''
    const statsResponse = await getNotesStatistics({
      startDate: startDate.value,
      endDate: endDate.value,
    })

    if (statsResponse.success) {
      applyNotesStats(statsResponse.data)

      updateCharts()
    } else {
      loadError.value = statsResponse.message || t('stage5.statistics.notes.statsLoadFailed')
      ElMessage.error(loadError.value)
      return
    }

    const listResponse = await getNotesList({
      startDate: startDate.value,
      endDate: endDate.value,
    })

    if (listResponse.success) {
      tableData.value = listResponse.data.map((item: NoteDTO) => ({
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
      loadError.value = listResponse.message || t('stage5.statistics.notes.listLoadFailed')
      ElMessage.error(loadError.value)
      return
    }

    currentPage.value = 1
    await nextTick()
    initCharts()
  } catch (error) {
    loadError.value = t('stage5.common.messages.dataLoadFailed')
    console.error(loadError.value, error)
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  if (!startDate.value || !endDate.value) {
    ElMessage.warning(t('stage5.common.messages.pleaseSelectDateRange'))
    return
  }
  loadData()
}

const handleExport = async () => {
  try {
    exporting.value = true
    const exportType: NoteType | undefined =
      filterType.value === 'income' || filterType.value === 'expense'
        ? filterType.value
        : undefined
    const blob = await exportNotesReport({
      startDate: startDate.value,
      endDate: endDate.value,
      type: exportType,
    })
    saveBlobDownload({
      blob,
      fileName: `notes-${startDate.value}-${endDate.value}.csv`,
    })
  } catch (error) {
    console.error('Failed to export notes report:', error)
    const message = await getStatisticsReportErrorMessage(
      error,
      t('stage5.statistics.reports.downloadFailed'),
    )
    ElMessage.error(message)
  } finally {
    exporting.value = false
  }
}

const handleViewVoucher = (row: any) => {
  ElMessage.warning(t('stage5.statistics.notes.voucherDataGap'))
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
}

const handleResize = () => {
  expenseProjectChart?.resize()
  expensePaymentChart?.resize()
}

watch(dateType, (newType) => {
  if (newType) {
    updateDateRange(newType)
  }
})

watch(activeTab, () => {
  expenseProjectChart?.dispose()
  expensePaymentChart?.dispose()

  expenseProjectChart = null
  expensePaymentChart = null

  initCharts()
})

watch(filterType, () => {
  currentPage.value = 1
})

onMounted(() => {
  loadData()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  expenseProjectChart?.dispose()
  expensePaymentChart?.dispose()
})
</script>

<style scoped>
.notes-summary-content {
  min-height: 100%;
  padding: 0;
  background: #f5f5f5;
}

.date-selector {
  display: flex;
  min-height: 62px;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;
  padding: 14px 18px;
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

.query-button {
  width: 48px;
  height: 32px;
  margin-left: 14px;
  padding: 0;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.state-alert {
  margin-bottom: 10px;
}

.page-empty {
  min-height: 320px;
  margin-bottom: 10px;
  background: #ffffff;
  border-radius: 4px;
}

.notes-summary-content :deep(.date-selector .el-select__wrapper),
.notes-summary-content :deep(.date-selector .el-input__wrapper) {
  min-height: 32px;
  border-radius: 5px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.notes-summary-content :deep(.date-selector .el-select__wrapper:hover),
.notes-summary-content :deep(.date-selector .el-select__wrapper.is-focused),
.notes-summary-content :deep(.date-selector .el-input__wrapper:hover),
.notes-summary-content :deep(.date-selector .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #87bdf6 inset;
}

.notes-summary-content :deep(.date-selector .business-date-range.el-range-editor.el-input__wrapper) {
  width: 288px;
  max-width: 288px;
  flex: 0 0 288px;
  padding: 1px 1px 1px 6px;
  overflow: hidden;
}

.notes-summary-content :deep(.date-selector .business-date-range .el-range__icon) {
  margin: 0 9px 0 2px;
  color: #aeb4bd;
  font-size: 14px;
}

.notes-summary-content :deep(.date-selector .business-date-range .el-range-input) {
  height: 30px;
  padding: 0 7px;
  background: #fafafa;
  color: #30343b;
  font-size: 13px;
  font-weight: 500;
  line-height: 30px;
}

.notes-summary-content :deep(.date-selector .business-date-range .el-range-input:first-child) {
  border-radius: 4px 0 0 4px;
}

.notes-summary-content :deep(.date-selector .business-date-range .el-range-input:last-child) {
  border-radius: 0 4px 4px 0;
}

.notes-summary-content :deep(.date-selector .business-date-range .el-range-separator) {
  width: 44px;
  height: 30px;
  background: #ffffff;
  color: #777f89;
  font-size: 12px;
  line-height: 30px;
}

.notes-summary-content :deep(.date-selector .business-date-range .el-range__close-icon) {
  display: none;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.summary-card {
  display: flex;
  min-height: 84px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 12px 16px 18px;
  background: #ffffff;
  border-radius: 4px;
}

.card-content {
  min-width: 0;
}

.card-label {
  margin-bottom: 8px;
  color: #8a8f99;
  font-size: 14px;
  line-height: 1.2;
}

.card-value {
  color: #050505;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}

.card-icon {
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
}

.card-icon img {
  display: block;
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.statistics-heading {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 10px;
}

.statistics-heading h2 {
  margin: 0;
  color: #050505;
  font-size: 24px;
  font-weight: 500;
  line-height: 32px;
}

.mode-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-switch :deep(.el-button) {
  min-width: 61px;
  height: 32px;
  margin: 0;
  padding: 0 13px;
  border-color: #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #777f89;
  font-size: 13px;
  font-weight: 500;
  box-shadow: none;
}

.mode-switch :deep(.el-button.active),
.mode-switch :deep(.el-button:hover) {
  border-color: #1e90ff;
  background: #1e90ff;
  color: #ffffff;
}

.statistics-section {
  margin-bottom: 24px;
}

.charts-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}

.chart-wrapper {
  min-height: 374px;
  background: #ffffff;
  border-radius: 0;
}

.chart-title {
  margin: 22px 20px 0;
  color: #050505;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.chart-canvas {
  width: 100%;
  height: 316px;
}

.income-arc-chart {
  display: flex;
  height: 316px;
  align-items: center;
  justify-content: center;
  padding-top: 0;
}

.income-arc-stage {
  position: relative;
  width: min(100%, 520px);
  height: 306px;
}

.income-arc-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 296px;
  overflow: visible;
}

.income-arc-segment {
  shape-rendering: geometricPrecision;
}

.income-arc-amount {
  position: absolute;
  left: 50%;
  top: 160px;
  width: max-content;
  transform: translateX(-50%);
  color: #050505;
  font-size: 30px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}

.income-arc-compare {
  position: absolute;
  left: 50%;
  top: 238px;
  display: flex;
  align-items: center;
  gap: 28px;
  transform: translateX(-50%);
  color: #30343b;
  font-size: 17px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}

.income-arc-previous strong {
  margin-left: 10px;
  font-weight: 500;
}

.income-arc-growth {
  display: inline-flex;
  height: 26px;
  align-items: center;
  padding: 0 9px;
  border-radius: 4px;
  background: #c6ccd4;
  color: #ffffff;
  font-size: 18px;
  font-weight: 500;
}

.income-arc-growth-up {
  background: #69b7ff;
}

.income-arc-growth-down {
  background: #ff7a7a;
}

.income-arc-growth-flat {
  background: #c6ccd4;
}

.expense-donut-chart {
  height: 316px;
}

.details-section {
  background: #ffffff;
  border-radius: 0;
  padding: 0 20px 18px;
}

.details-header {
  display: flex;
  min-height: 78px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.details-header h3 {
  margin: 0;
  color: #050505;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.detail-filter-select {
  width: 123px;
}

.export-button {
  width: 140px;
  height: 32px;
  padding: 0;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.details-section :deep(.header-actions .el-select__wrapper) {
  min-height: 32px;
  border-radius: 5px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.details-table {
  --el-table-border-color: #e6e6e6;
  --el-table-header-bg-color: #fafafa;
  --el-table-row-hover-bg-color: #f9fbff;
  width: 100%;
  margin-bottom: 0;
  color: #424850;
  font-size: 13px;
}

.details-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.details-table :deep(th.el-table__cell) {
  height: 36px;
  padding: 0;
  background: #fafafa;
  color: #6f7782;
  font-size: 14px;
  font-weight: 600;
}

.details-table :deep(td.el-table__cell) {
  height: 52px;
  padding: 0;
  color: #3f4650;
  font-size: 13px;
}

.details-table :deep(.cell) {
  padding: 0 12px;
  line-height: 1.3;
}

.details-table :deep(.type-tag) {
  min-width: 44px;
  height: 22px;
  padding: 0 10px;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 500;
  line-height: 22px;
}

.details-table :deep(.type-tag-expense) {
  background: #ffd3d7;
  color: #ff5a68;
}

.details-table :deep(.type-tag-income) {
  background: #dff7ea;
  color: #25b26b;
}

.income-amount {
  color: #3f4650 !important;
  font-weight: 500;
}

.expense-amount {
  color: #3f4650 !important;
  font-weight: 500;
}

.table-footer {
  display: flex;
  min-height: 70px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-top: 18px;
}

.footer-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  color: #777f89;
  font-size: 14px;
  line-height: 32px;
}

.net-income-value {
  color: #50c18b;
  font-weight: 500;
}

.income-value {
  color: #168bf8;
  font-weight: 500;
}

.expense-value {
  color: #ff0000;
  font-weight: 500;
}

.table-footer :deep(.el-pagination) {
  flex: 0 0 auto;
}

.pagination-group {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  margin-left: auto;
}

.table-footer :deep(.el-pagination .el-select) {
  width: 112px;
}

.table-footer :deep(.el-pagination .el-select__wrapper) {
  min-height: 26px;
  border-radius: 5px;
}

.table-footer :deep(.el-pager li),
.table-footer :deep(.btn-prev),
.table-footer :deep(.btn-next) {
  min-width: 28px;
  height: 28px;
  font-size: 13px;
}

.page-total-text {
  color: #333333;
  font-size: 12px;
  line-height: 28px;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .summary-cards,
  .charts-row {
    grid-template-columns: 1fr;
  }

  .details-header,
  .table-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
