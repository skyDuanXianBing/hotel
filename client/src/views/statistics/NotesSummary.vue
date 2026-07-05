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
        <el-button type="primary" class="record-button" @click="showRecordTransaction = true">
          <el-icon><EditPen /></el-icon>
          <span>{{ t('stage6.components.recordTransaction.title') }}</span>
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
              <span v-if="row.voucherCount > 0">{{ row.voucherCount }}</span>
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

    <RecordTransaction v-model="showRecordTransaction" @success="handleRecordSuccess" />
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import RecordTransaction from '@/components/RecordTransaction.vue'
import StatisticsLayout from './StatisticsLayout.vue'
import {
  getNotesList,
  type NoteDTO,
} from '@/api/notes'
import {
  addDaysToYmd,
  getStoreTodayYmd,
  getYmdMonthStart,
  getYmdWeekStart,
} from '@/utils/storeDateTime'

const { t } = useI18n()
const currencySymbol = '\u00a5'

const today = getStoreTodayYmd()
const dateType = ref('today')
const startDate = ref(today)
const endDate = ref(today)
const filterType = ref('all')
const currentPage = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const loadError = ref('')
const showRecordTransaction = ref(false)

const tableData = ref<any[]>([])

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
const hasNotesData = computed(() => tableData.value.length > 0)
const showEmpty = computed(() => !loading.value && !loadError.value && !hasNotesData.value)
const contentReady = computed(() => !loadError.value && !showEmpty.value)

const formatMoney = (value: number) => {
  const normalizedValue = Number(value) || 0
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(normalizedValue)
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

const loadData = async () => {
  try {
    loading.value = true
    loadError.value = ''
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

const handleRecordSuccess = () => {
  void loadData()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
}

watch(dateType, (newType) => {
  if (newType) {
    updateDateRange(newType)
  }
})

watch(filterType, () => {
  currentPage.value = 1
})

onMounted(() => {
  loadData()
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

.record-button {
  height: 32px;
  margin-left: auto;
  padding: 0 16px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.record-button :deep(.el-icon) {
  margin-right: 4px;
  font-size: 14px;
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
  .details-header,
  .table-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
