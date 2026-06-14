<template>
  <div class="price-history-page">
    <RoomPriceTabs />

    <section class="price-history-surface">
      <div class="history-filter-panel">
        <div class="history-filter-row">
          <div class="filter-group filter-group--date-range">
            <span class="filter-label">{{ t('accommodation.priceHistory.operationDate') }}</span>
            <el-date-picker
              v-model="operateDateStart"
              type="date"
              :placeholder="t('accommodation.common.startDate')"
              format="YYYY/MM/DD"
              value-format="YYYY-MM-DD"
              class="filter-date"
            />
            <span class="date-separator">{{ t('accommodation.common.rangeTo') }}</span>
            <el-date-picker
              v-model="operateDateEnd"
              type="date"
              :placeholder="t('accommodation.common.endDate')"
              format="YYYY/MM/DD"
              value-format="YYYY-MM-DD"
              class="filter-date"
            />
          </div>

          <div class="filter-group filter-group--room-type">
            <span class="filter-label">{{ t('accommodation.priceHistory.localRoomType') }}</span>
            <el-select
              v-model="filters.roomTypeId"
              :placeholder="t('accommodation.priceHistory.allRoomTypes')"
              clearable
              class="filter-select"
            >
              <el-option :label="t('accommodation.priceHistory.allRoomTypes')" :value="null" />
              <el-option
                v-for="roomType in roomTypes"
                :key="roomType.id"
                :label="roomType.name"
                :value="roomType.id"
              />
            </el-select>
          </div>

          <div class="filter-group filter-group--price-plan">
            <span class="filter-label">{{ t('accommodation.priceHistory.pmsPricePlan') }}</span>
            <el-select
              v-model="filters.pricePlanId"
              :placeholder="t('accommodation.priceHistory.allPricePlans')"
              clearable
              class="filter-select"
            >
              <el-option :label="t('accommodation.priceHistory.allPricePlans')" :value="null" />
              <el-option
                v-for="plan in pricePlans"
                :key="plan.id"
                :label="plan.name"
                :value="plan.id"
              />
            </el-select>
          </div>

          <el-button class="toggle-button" type="primary" @click="toggleFilters">
            {{ showAllFilters ? t('accommodation.priceHistory.collapse') : t('accommodation.priceHistory.expand') }}
            <el-icon><ArrowUp v-if="showAllFilters" /><ArrowDown v-else /></el-icon>
          </el-button>
        </div>

        <div v-if="showAllFilters" class="history-filter-row history-filter-row--secondary">
          <div class="filter-group filter-group--date-range">
            <span class="filter-label">{{ t('accommodation.priceHistory.priceDate') }}</span>
            <el-date-picker
              v-model="priceDateStart"
              type="date"
              :placeholder="t('accommodation.common.startDate')"
              format="YYYY/MM/DD"
              value-format="YYYY-MM-DD"
              class="filter-date"
            />
            <span class="date-separator">{{ t('accommodation.common.rangeTo') }}</span>
            <el-date-picker
              v-model="priceDateEnd"
              type="date"
              :placeholder="t('accommodation.common.endDate')"
              format="YYYY/MM/DD"
              value-format="YYYY-MM-DD"
              class="filter-date"
            />
          </div>

          <div class="filter-group filter-group--operator">
            <span class="filter-label">{{ t('accommodation.priceHistory.operator') }}</span>
            <el-select
              v-model="filters.operator"
              :placeholder="t('accommodation.common.all')"
              clearable
              class="filter-select"
            >
              <el-option :label="t('accommodation.common.all')" :value="null" />
              <el-option
                :label="t('accommodation.priceHistory.operatorOptions.system')"
                value="SYSTEM"
              />
            </el-select>
          </div>

          <div class="filter-group filter-group--change-item">
            <span class="filter-label">{{ t('accommodation.priceHistory.changeItem') }}</span>
            <el-select
              v-model="filters.changeType"
              :placeholder="t('accommodation.priceHistory.changeTypeOptions.price')"
              class="filter-select"
            >
              <el-option
                :label="t('accommodation.priceHistory.changeTypeOptions.price')"
                value="PRICE"
              />
            </el-select>
          </div>
        </div>
      </div>

      <div class="history-table-shell">
        <el-table
          :data="tableData"
          border
          v-loading="loading"
          class="history-table"
          :header-cell-style="headerCellStyle"
        >
          <el-table-column
            prop="roomTypeName"
            :label="t('accommodation.priceHistory.columns.roomTypeName')"
            min-width="190"
          />
          <el-table-column
            prop="pricePlanName"
            :label="t('accommodation.priceHistory.columns.pricePlanName')"
            min-width="150"
          />
          <el-table-column
            prop="priceDate"
            :label="t('accommodation.priceHistory.columns.priceDate')"
            min-width="250"
          />
          <el-table-column
            prop="applyDays"
            :label="t('accommodation.priceHistory.columns.applyDays')"
            width="112"
          />
          <el-table-column
            prop="changeType"
            :label="t('accommodation.priceHistory.columns.changeType')"
            width="112"
          />
          <el-table-column
            prop="changeValue"
            :label="t('accommodation.priceHistory.columns.changeValue')"
            width="112"
          >
            <template #default="{ row }">
              <span class="change-value">{{ row.changeValue }}</span>
            </template>
          </el-table-column>
          <el-table-column
            prop="operator"
            :label="t('accommodation.priceHistory.columns.operator')"
            width="112"
          />
          <el-table-column
            prop="operateTime"
            :label="t('accommodation.priceHistory.columns.operateTime')"
            min-width="230"
          />
        </el-table>

        <div class="pagination-container">
          <span class="pagination-total">
            {{ t('accommodation.common.totalCount', { count: pagination.total }) }}
          </span>
          <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 25, 50, 100, 200]"
            :total="pagination.total"
            :pager-count="7"
            layout="sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getAllPricePlans } from '@/api/pricePlan'
import { getPriceChangeHistory, type PriceChangeHistoryDTO } from '@/api/roomPrice'
import { getAllRoomTypes } from '@/api/roomType'
import { useUserStore } from '@/stores/user'
import RoomPriceTabs from './components/RoomPriceTabs.vue'

const userStore = useUserStore()
const { t } = useI18n()

const loading = ref(false)
const showAllFilters = ref(false)
const operateDateStart = ref('')
const operateDateEnd = ref('')
const priceDateStart = ref('')
const priceDateEnd = ref('')

const filters = reactive({
  roomTypeId: null as number | null,
  pricePlanId: null as number | null,
  operator: null as string | null,
  changeType: 'PRICE',
})

const roomTypes = ref<any[]>([])
const pricePlans = ref<any[]>([])
const tableData = ref<PriceChangeHistoryDTO[]>([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const headerCellStyle = {
  background: '#fbfbfb',
  color: '#252525',
  fontSize: '12px',
  fontWeight: '600',
  padding: '0',
}

const loadRoomTypes = async () => {
  try {
    const userId = userStore.currentUser?.id
    if (!userId) return

    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      roomTypes.value = response.data
    }
  } catch (error) {
    console.error('Failed to load room types:', error)
  }
}

const loadPricePlans = async () => {
  try {
    const userId = userStore.currentUser?.id
    if (!userId) return

    const response = await getAllPricePlans(userId)
    if (response && response.data) {
      pricePlans.value = response.data
    }
  } catch (error) {
    console.error('Failed to load price plans:', error)
  }
}

const loadHistory = async () => {
  try {
    loading.value = true
    const response = await getPriceChangeHistory({
      operateDateStart: operateDateStart.value || undefined,
      operateDateEnd: operateDateEnd.value || undefined,
      priceDateStart: priceDateStart.value || undefined,
      priceDateEnd: priceDateEnd.value || undefined,
      roomTypeId: filters.roomTypeId || undefined,
      pricePlanId: filters.pricePlanId ? String(filters.pricePlanId) : undefined,
      operator: filters.operator || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    })

    if (response.success && response.data) {
      tableData.value = response.data.records
      pagination.total = response.data.total
    }
  } catch (error) {
    console.error('Failed to load price change history:', error)
    const loadFailedMessage = t('accommodation.priceHistory.messages.loadFailed')
    ElMessage.error(
      loadFailedMessage === 'accommodation.priceHistory.messages.loadFailed'
        ? t('accommodation.priceHistory.loadFailed')
        : loadFailedMessage,
    )
  } finally {
    loading.value = false
  }
}

const toggleFilters = () => {
  showAllFilters.value = !showAllFilters.value
}

const handleSizeChange = (newSize: number) => {
  pagination.pageSize = newSize
  pagination.pageNum = 1
  loadHistory()
}

const handlePageChange = (newPage: number) => {
  pagination.pageNum = newPage
  loadHistory()
}

watch(
  [
    operateDateStart,
    operateDateEnd,
    priceDateStart,
    priceDateEnd,
    () => filters.roomTypeId,
    () => filters.pricePlanId,
    () => filters.operator,
  ],
  () => {
    pagination.pageNum = 1
    loadHistory()
  },
)

onMounted(() => {
  loadRoomTypes()
  loadPricePlans()
  loadHistory()
})
</script>

<style scoped>
.price-history-page {
  min-height: 100%;
  padding: 4px 0 16px;
  background: #f5f5f5;
  scrollbar-gutter: stable;
}

.price-history-surface {
  margin: 0 24px;
  background: #ffffff;
  border: 1px solid #e3e3e3;
  overflow: hidden;
}

.history-filter-panel {
  --date-filter-width: 138px;
  --select-filter-width: 158px;
  --date-range-column-width: max-content;
  --room-label-width: 92px;
  --plan-label-width: 116px;
  --room-column-width: calc(var(--room-label-width) + var(--select-filter-width) + 7px);
  --plan-column-width: calc(var(--plan-label-width) + var(--select-filter-width) + 7px);
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid #e2e2e2;
  background: #ffffff;
  overflow-x: auto;
}

.history-filter-row {
  display: grid;
  grid-template-columns:
    var(--date-range-column-width)
    var(--room-column-width)
    var(--plan-column-width)
    auto;
  align-items: center;
  column-gap: 14px;
  min-width: 900px;
}

.history-filter-row--secondary {
  grid-template-columns:
    var(--date-range-column-width)
    var(--room-column-width)
    var(--plan-column-width)
    auto;
  padding-top: 2px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.filter-group--date-range {
  gap: 8px;
}

.filter-group--operator {
  grid-column: 2;
}

.filter-group--change-item {
  grid-column: 3;
}

.filter-label {
  flex: 0 0 auto;
  color: #4d4d4d;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  text-align: left;
  white-space: nowrap;
}

.filter-group--room-type .filter-label,
.filter-group--operator .filter-label {
  flex-basis: var(--room-label-width);
  width: var(--room-label-width);
  text-align: right;
}

.filter-group--price-plan .filter-label,
.filter-group--change-item .filter-label {
  flex-basis: var(--plan-label-width);
  width: var(--plan-label-width);
  text-align: right;
}

.filter-date {
  width: var(--date-filter-width);
  flex: 0 0 var(--date-filter-width);
}

.history-filter-panel :deep(.filter-date.el-date-editor),
.history-filter-panel :deep(.filter-date.el-date-editor.el-input),
.history-filter-panel :deep(.filter-date.el-input) {
  width: var(--date-filter-width);
  flex: 0 0 var(--date-filter-width);
}

.filter-select {
  width: var(--select-filter-width);
  flex: 0 0 var(--select-filter-width);
}

.date-separator {
  color: #5f5f5f;
  font-size: 14px;
  line-height: 1;
}

.toggle-button {
  justify-self: start;
  height: 36px;
  min-width: 68px;
  padding: 0 13px;
  border-radius: 4px;
  border-color: #2196f3;
  background: #2196f3;
  font-size: 13px;
  font-weight: 600;
}

.toggle-button :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.history-filter-panel :deep(.el-input__wrapper),
.history-filter-panel :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dddddd inset;
}

.history-filter-panel :deep(.el-input__wrapper:hover),
.history-filter-panel :deep(.el-input__wrapper.is-focus),
.history-filter-panel :deep(.el-select__wrapper:hover),
.history-filter-panel :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #bdbdbd inset;
}

.history-table-shell {
  display: flex;
  flex-direction: column;
  padding: 0;
  background: #ffffff;
}

.history-table {
  width: 100%;
  --el-table-border-color: #e0e0e0;
  --el-table-header-bg-color: #fbfbfb;
  --el-table-row-hover-bg-color: #fafafa;
  color: #303030;
  font-size: 14px;
}

:global(.panel-content:has(.price-history-page)) {
  scrollbar-gutter: stable;
}

:deep(.history-table .el-table__inner-wrapper::before),
:deep(.history-table .el-table__border-left-patch) {
  display: none;
}

:deep(.history-table .el-table__header th.el-table__cell) {
  height: 42px;
  padding: 0;
  border-color: #d8d8d8;
}

:deep(.history-table .el-table__header th.el-table__cell .cell) {
  padding: 0 10px;
  color: #252525;
  line-height: 1.3;
}

:deep(.history-table .el-table__body td.el-table__cell) {
  height: 42px;
  padding: 0;
  border-color: #e5e5e5;
  background: #ffffff;
}

:deep(.history-table .el-table__body td.el-table__cell .cell) {
  padding: 0 10px;
  color: #303030;
  line-height: 1.3;
}

:deep(.history-table .el-table__body tr:nth-child(even) td.el-table__cell) {
  background: #ffffff;
}

.change-value {
  color: #303030;
  font-weight: 500;
}

.pagination-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 58px;
  padding: 10px 18px;
  border-top: 1px solid #ececec;
  background: #ffffff;
}

.pagination-total {
  color: #606266;
  font-size: 14px;
}

.pagination-container :deep(.el-pagination) {
  margin-left: auto;
  gap: 8px;
  --el-pagination-button-width: 30px;
  --el-pagination-button-height: 30px;
  --el-pagination-font-size: 14px;
}

.pagination-container :deep(.el-pagination .el-select) {
  width: 96px;
}

.pagination-container :deep(.el-pagination .el-select .el-select__wrapper) {
  min-height: 30px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.pagination-container :deep(.el-pager) {
  gap: 8px;
}

.pagination-container :deep(.el-pager li),
.pagination-container :deep(.btn-prev),
.pagination-container :deep(.btn-next) {
  min-width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #606266;
  font-weight: 400;
}

.pagination-container :deep(.el-pager li.is-active) {
  border-color: #409eff;
  background: #ffffff;
  color: #409eff;
  font-weight: 500;
}

.pagination-container :deep(.el-pager li.more) {
  border-color: transparent;
}

.pagination-container :deep(.el-pagination__jump) {
  margin-left: 8px;
  color: #606266;
}

.pagination-container :deep(.el-pagination__goto),
.pagination-container :deep(.el-pagination__classifier) {
  color: #606266;
}

.pagination-container :deep(.el-pagination__editor.el-input) {
  width: 56px;
}

.pagination-container :deep(.el-pagination__editor .el-input__wrapper) {
  height: 30px;
  min-height: 30px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

@media (max-width: 1200px) {
  .price-history-surface {
    margin: 0 20px;
  }

  .history-filter-row {
    align-items: flex-start;
  }

  .toggle-button {
    margin-left: 0;
  }
}

@media (max-width: 768px) {
  .price-history-page {
    padding-bottom: 20px;
  }

  .price-history-surface {
    margin: 0 12px;
  }

  .history-filter-row {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    min-width: 0;
  }

  .filter-group,
  .filter-group--date-range {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-date,
  .filter-select,
  .toggle-button {
    width: 100%;
  }

  .pagination-container {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
