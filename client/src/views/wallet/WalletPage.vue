<template>
  <WorkspaceLayout storage-key="wallet-sidebar-collapsed">
    <template #sidebar="{ collapsed, toggleSidebar }">
      <WorkspaceSidebar
        :collapsed="collapsed"
        :items="walletSidebarItems"
        :active-key="currentMenu"
        :expanded-keys="expandedSidebarKeys"
        :collapse-label="t('pages.wallet.sidebar.collapse')"
        :aria-label="t('pages.wallet.sidebar.wallet')"
        @toggle="toggleSidebar"
        @parent-click="handleSidebarParentClick"
        @item-select="handleMenuClick"
      />
    </template>

    <div class="wallet-page">
      <div v-if="currentMenu === 'account'" class="account-content">
        <WorkspaceTabs
          v-model="activeTab"
          :tabs="accountTabs"
          :aria-label="t('pages.wallet.sidebar.account')"
        />
        <div class="wallet-tab-panel">
          <template v-if="activeTab === 'clearing'">
            <div class="tab-content">
              <div class="filter-section">
                <div class="filter-row">
                  <span class="filter-label">{{ t('pages.wallet.account.filters.createdTime') }}</span>
                  <el-date-picker
                    v-model="clearingFilters.startDate"
                    type="datetime"
                    :placeholder="t('pages.wallet.account.filters.dateTimePlaceholder')"
                    format="YYYY/MM/DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    size="default"
                    class="date-picker"
                  />
                  <span class="filter-separator">{{ t('pages.wallet.account.filters.to') }}</span>
                  <el-date-picker
                    v-model="clearingFilters.endDate"
                    type="datetime"
                    :placeholder="t('pages.wallet.account.filters.dateTimePlaceholder')"
                    format="YYYY/MM/DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    size="default"
                    class="date-picker"
                  />
                  <el-button type="text" @click="resetClearingDates">
                    <el-icon><RefreshLeft /></el-icon>
                  </el-button>
                </div>

                <div class="filter-row">
                  <span class="filter-label">{{ t('pages.wallet.account.filters.category') }}</span>
                  <el-select
                    v-model="clearingFilters.category"
                    :placeholder="t('pages.wallet.account.filters.all')"
                    size="default"
                    class="filter-select"
                  >
                    <el-option :label="t('pages.wallet.account.filters.all')" value="" />
                    <el-option :label="t('pages.wallet.account.filters.income')" value="income" />
                    <el-option :label="t('pages.wallet.account.filters.expense')" value="expense" />
                  </el-select>

                  <span class="filter-label search-label">{{ t('pages.wallet.account.filters.search') }}</span>
                  <el-input
                    v-model="clearingFilters.searchText"
                    :placeholder="t('pages.wallet.account.filters.searchPlaceholder')"
                    clearable
                    size="default"
                    class="search-input"
                  >
                    <template #prefix>
                      <el-icon><Search /></el-icon>
                    </template>
                  </el-input>
                </div>
              </div>

              <el-table
                :data="clearingData"
                border
                style="width: 100%"
                :empty-text="t('pages.wallet.common.empty')"
                class="data-table"
              >
                <el-table-column prop="createTime" :label="t('pages.wallet.account.columns.createdTime')" width="180" />
                <el-table-column prop="businessType" :label="t('pages.wallet.account.columns.businessType')" width="120" />
                <el-table-column prop="category" :label="t('pages.wallet.account.columns.category')" width="100" />
                <el-table-column prop="paymentMethod" :label="t('pages.wallet.account.columns.paymentMethod')" width="120" />
                <el-table-column prop="prePayId" :label="t('pages.wallet.account.columns.prePayId')" width="150" />
                <el-table-column prop="transactionId" :label="t('pages.wallet.account.columns.transactionId')" width="200" />
                <el-table-column prop="currency" :label="t('pages.wallet.account.columns.currency')" width="80" />
                <el-table-column prop="amount" :label="t('pages.wallet.account.columns.amount')" width="120" align="right">
                  <template #default="scope">
                    {{ formatAmount(scope.row.amount) }}
                  </template>
                </el-table-column>
                <el-table-column
                  prop="availableAmount"
                  :label="t('pages.wallet.account.columns.availableAmount')"
                  width="120"
                  align="right"
                >
                  <template #default="scope">
                    {{ formatAmount(scope.row.availableAmount) }}
                  </template>
                </el-table-column>
              </el-table>

              <div class="pagination-container">
                <el-pagination
                  v-model:current-page="clearingPagination.current"
                  v-model:page-size="clearingPagination.size"
                  :total="clearingPagination.total"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                  @current-change="handleClearingPageChange"
                  @size-change="handleClearingSizeChange"
                />
              </div>
            </div>
          </template>

          <template v-else-if="activeTab === 'funds'">
            <div class="tab-content">
              <div class="filter-section">
                <div class="filter-row">
                  <span class="filter-label">{{ t('pages.wallet.account.filters.createdTime') }}</span>
                  <el-date-picker
                    v-model="fundsFilters.startDate"
                    type="datetime"
                    :placeholder="t('pages.wallet.account.filters.dateTimePlaceholder')"
                    format="YYYY/MM/DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    size="default"
                    class="date-picker"
                  />
                  <span class="filter-separator">{{ t('pages.wallet.account.filters.to') }}</span>
                  <el-date-picker
                    v-model="fundsFilters.endDate"
                    type="datetime"
                    :placeholder="t('pages.wallet.account.filters.dateTimePlaceholder')"
                    format="YYYY/MM/DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    size="default"
                    class="date-picker"
                  />
                  <el-button type="text" @click="resetFundsDates">
                    <el-icon><RefreshLeft /></el-icon>
                  </el-button>
                </div>

                <div class="filter-row">
                  <span class="filter-label">{{ t('pages.wallet.account.filters.category') }}</span>
                  <el-select
                    v-model="fundsFilters.category"
                    :placeholder="t('pages.wallet.account.filters.all')"
                    size="default"
                    class="filter-select"
                  >
                    <el-option :label="t('pages.wallet.account.filters.all')" value="" />
                    <el-option :label="t('pages.wallet.account.filters.income')" value="income" />
                    <el-option :label="t('pages.wallet.account.filters.expense')" value="expense" />
                  </el-select>

                  <span class="filter-label search-label">{{ t('pages.wallet.account.filters.search') }}</span>
                  <el-input
                    v-model="fundsFilters.searchText"
                    :placeholder="t('pages.wallet.account.filters.searchPlaceholder')"
                    clearable
                    size="default"
                    class="search-input"
                  >
                    <template #prefix>
                      <el-icon><Search /></el-icon>
                    </template>
                  </el-input>
                </div>
              </div>

              <el-table
                :data="fundsData"
                border
                style="width: 100%"
                :empty-text="t('pages.wallet.common.empty')"
                class="data-table"
              >
                <el-table-column prop="createTime" :label="t('pages.wallet.account.columns.createdTime')" width="180" />
                <el-table-column prop="businessType" :label="t('pages.wallet.account.columns.businessType')" width="120" />
                <el-table-column prop="category" :label="t('pages.wallet.account.columns.category')" width="100" />
                <el-table-column prop="transactionId" :label="t('pages.wallet.account.columns.transactionId')" width="200" />
                <el-table-column prop="prePayId" :label="t('pages.wallet.account.columns.prePayId')" width="150" />
                <el-table-column prop="currency" :label="t('pages.wallet.account.columns.currency')" width="80" />
                <el-table-column prop="amount" :label="t('pages.wallet.account.columns.amount')" width="120" align="right">
                  <template #default="scope">
                    {{ formatAmount(scope.row.amount) }}
                  </template>
                </el-table-column>
                <el-table-column
                  prop="availableAmount"
                  :label="t('pages.wallet.account.columns.availableAmount')"
                  width="120"
                  align="right"
                >
                  <template #default="scope">
                    {{ formatAmount(scope.row.availableAmount) }}
                  </template>
                </el-table-column>
                <el-table-column
                  prop="orderCompleteTime"
                  :label="t('pages.wallet.account.columns.orderCompleteTime')"
                  width="180"
                />
              </el-table>

              <div class="pagination-container">
                <el-pagination
                  v-model:current-page="fundsPagination.current"
                  v-model:page-size="fundsPagination.size"
                  :total="fundsPagination.total"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                  @current-change="handleFundsPageChange"
                  @size-change="handleFundsSizeChange"
                />
              </div>
            </div>
          </template>
        </div>
      </div>

      <div v-else-if="currentMenu === 'withdraw'" class="withdraw-content">
        <WorkspaceTabs
          v-model="withdrawTab"
          :tabs="withdrawTabs"
          :aria-label="t('pages.wallet.sidebar.withdraw')"
        />
        <div class="wallet-tab-panel">
          <template v-if="withdrawTab === 'available'">
            <div class="tab-content">
              <div class="withdraw-card">
                <h2>{{ t('pages.wallet.withdraw.availableTitle') }}</h2>
                <div class="withdraw-info">
                  <div class="info-item">
                    <span class="label">{{ t('pages.wallet.withdraw.orderCount') }}</span>
                    <span class="value">0</span>
                  </div>
                  <div class="info-item">
                    <span class="label">{{ t('pages.wallet.withdraw.withdrawAmount') }}</span>
                    <span class="value">0</span>
                    <el-button type="primary" class="withdraw-btn">
                      {{ t('pages.wallet.withdraw.withdrawAction') }}
                    </el-button>
                  </div>
                </div>

                <div class="filter-section">
                  <div class="filter-row">
                    <span class="filter-label">{{ t('pages.wallet.withdraw.selectTime') }}</span>
                    <el-select :placeholder="t('pages.wallet.withdraw.selectTimePlaceholder')" class="time-select">
                      <el-option
                        :label="t('pages.wallet.withdraw.selectTimePlaceholder')"
                        value=""
                      />
                    </el-select>
                    <el-date-picker :placeholder="t('pages.wallet.withdraw.startTime')" class="date-picker" />
                    <span class="filter-separator">{{ t('pages.wallet.account.filters.to') }}</span>
                    <el-date-picker :placeholder="t('pages.wallet.withdraw.endTime')" class="date-picker" />
                    <el-button type="text">
                      <el-icon><RefreshLeft /></el-icon>
                    </el-button>
                    <span class="filter-label">{{ t('pages.wallet.withdraw.selectGroup') }}</span>
                    <el-select :placeholder="t('pages.wallet.withdraw.selectPlaceholder')" class="group-select">
                      <el-option :label="t('pages.wallet.withdraw.selectPlaceholder')" value="" />
                    </el-select>
                  </div>
                </div>

                <el-table :data="[]" border class="data-table" :empty-text="t('pages.wallet.common.empty')">
                  <el-table-column prop="settleTime" :label="t('pages.wallet.withdraw.columns.settleTime')" />
                  <el-table-column prop="businessType" :label="t('pages.wallet.withdraw.columns.businessType')" />
                  <el-table-column prop="prePayId" :label="t('pages.wallet.withdraw.columns.prePayId')" />
                  <el-table-column prop="currency" :label="t('pages.wallet.withdraw.columns.currency')" />
                  <el-table-column prop="settleAmount" :label="t('pages.wallet.withdraw.columns.settleAmount')" />
                  <el-table-column prop="createTime" :label="t('pages.wallet.withdraw.columns.createTime')" />
                  <el-table-column
                    prop="orderCompleteTime"
                    :label="t('pages.wallet.withdraw.columns.orderCompleteTime')"
                  />
                  <el-table-column prop="groupName" :label="t('pages.wallet.withdraw.columns.groupName')" />
                </el-table>
              </div>
            </div>
          </template>

          <template v-else-if="withdrawTab === 'history'">
            <div class="tab-content">
              <div class="filter-section">
                <div class="filter-row">
                  <span class="filter-label">{{ t('pages.wallet.withdraw.selectTime') }}</span>
                  <el-date-picker :placeholder="t('pages.wallet.withdraw.startTime')" class="date-picker" />
                  <span class="filter-separator">{{ t('pages.wallet.account.filters.to') }}</span>
                  <el-date-picker :placeholder="t('pages.wallet.withdraw.endTime')" class="date-picker" />
                  <el-button type="text">
                    <el-icon><RefreshLeft /></el-icon>
                  </el-button>
                </div>
                <div class="filter-row">
                  <span class="filter-label">{{ t('pages.wallet.withdraw.status') }}</span>
                  <el-select :placeholder="t('pages.wallet.withdraw.statusAll')" class="status-select">
                    <el-option :label="t('pages.wallet.withdraw.statusAll')" value="" />
                    <el-option :label="t('pages.wallet.withdraw.statusSuccess')" value="success" />
                    <el-option :label="t('pages.wallet.withdraw.statusProcessing')" value="processing" />
                    <el-option :label="t('pages.wallet.withdraw.statusFailed')" value="failed" />
                  </el-select>
                  <span class="filter-label search-label">{{ t('pages.wallet.withdraw.search') }}</span>
                  <el-input
                    :placeholder="t('pages.wallet.withdraw.searchPeriodPlaceholder')"
                    clearable
                    class="search-input"
                  >
                    <template #prefix>
                      <el-icon><Search /></el-icon>
                    </template>
                  </el-input>
                </div>
              </div>

              <el-table :data="[]" border class="data-table" :empty-text="t('pages.wallet.common.empty')">
                <el-table-column prop="withdrawTime" :label="t('pages.wallet.withdraw.columns.withdrawTime')" width="180" />
                <el-table-column prop="paymentId" :label="t('pages.wallet.withdraw.columns.paymentId')" width="200" />
                <el-table-column prop="orderCount" :label="t('pages.wallet.withdraw.columns.orderCount')" width="100" />
                <el-table-column prop="currency" :label="t('pages.wallet.withdraw.columns.currency')" width="100" />
                <el-table-column prop="withdrawAmount" :label="t('pages.wallet.withdraw.columns.withdrawAmount')" width="120" align="right" />
                <el-table-column prop="withdrawFee" :label="t('pages.wallet.withdraw.columns.withdrawFee')" width="120" align="right" />
                <el-table-column prop="actualAmount" :label="t('pages.wallet.withdraw.columns.actualAmount')" width="120" align="right" />
                <el-table-column prop="withdrawStatus" :label="t('pages.wallet.withdraw.columns.withdrawStatus')" width="120" />
                <el-table-column prop="refundResult" :label="t('pages.wallet.withdraw.columns.refundResult')" width="150" />
              </el-table>

              <div class="pagination-container">
                <el-pagination
                  v-model:current-page="withdrawHistoryPagination.current"
                  v-model:page-size="withdrawHistoryPagination.size"
                  :total="withdrawHistoryPagination.total"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                />
              </div>
            </div>
          </template>
        </div>
      </div>

      <div v-else-if="currentMenu === 'identity'" class="identity-content">
        <div v-if="!showIdentityForm" class="identity-verify">
          <div class="verify-illustration">
            <img
              src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200'%3E%3Ccircle cx='100' cy='100' r='80' fill='%23e6f7ff'/%3E%3Ccircle cx='100' cy='100' r='60' fill='%231890ff' opacity='0.2'/%3E%3Cpath d='M70 100 L90 120 L130 80' stroke='%231890ff' stroke-width='8' fill='none' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E"
              :alt="t('pages.wallet.identity.alt')"
            />
          </div>
          <h2>{{ t('pages.wallet.identity.title') }}</h2>
          <el-button type="primary" size="large" @click="showIdentityForm = true">
            {{ t('pages.wallet.identity.startAction') }}
          </el-button>
        </div>

        <div v-else class="identity-form-container">
          <div class="steps">
            <div class="step active">
              <div class="step-number">1</div>
              <div class="step-label">{{ t('pages.wallet.identity.steps.submit') }}</div>
            </div>
            <div class="step-line"></div>
            <div class="step">
              <div class="step-number">2</div>
              <div class="step-label">{{ t('pages.wallet.identity.steps.reviewing') }}</div>
            </div>
            <div class="step-line"></div>
            <div class="step">
              <div class="step-number">3</div>
              <div class="step-label">{{ t('pages.wallet.identity.steps.success') }}</div>
            </div>
          </div>

          <el-form :model="identityForm" label-width="100px" class="identity-form">
            <el-form-item :label="t('pages.wallet.identity.labels.contact')">
              <el-input
                v-model="identityForm.firstName"
                :placeholder="t('pages.wallet.identity.placeholders.firstName')"
              />
              <el-input
                v-model="identityForm.lastName"
                :placeholder="t('pages.wallet.identity.placeholders.lastName')"
                class="mt-2"
              />
            </el-form-item>
            <el-form-item :label="t('pages.wallet.identity.labels.companyName')">
              <el-input
                v-model="identityForm.companyName"
                :placeholder="t('pages.wallet.identity.placeholders.companyName')"
              />
            </el-form-item>
            <el-form-item :label="t('pages.wallet.identity.labels.companyPhone')">
              <el-input
                v-model="identityForm.companyPhone"
                :placeholder="t('pages.wallet.identity.placeholders.companyPhone')"
              />
            </el-form-item>
            <el-form-item :label="t('pages.wallet.identity.labels.companyAddress')">
              <el-input
                v-model="identityForm.detailAddress"
                :placeholder="t('pages.wallet.identity.placeholders.detailAddress')"
              />
              <el-input
                v-model="identityForm.city"
                :placeholder="t('pages.wallet.identity.placeholders.city')"
                class="mt-2"
              />
              <el-input
                v-model="identityForm.stateProvince"
                :placeholder="t('pages.wallet.identity.placeholders.stateProvince')"
                class="mt-2"
              />
              <el-input
                v-model="identityForm.postalCode"
                :placeholder="t('pages.wallet.identity.placeholders.postalCode')"
                class="mt-2"
              />
              <el-input
                v-model="identityForm.country"
                :placeholder="t('pages.wallet.identity.placeholders.country')"
                class="mt-2"
              />
            </el-form-item>
            <el-form-item>
              <el-button @click="showIdentityForm = false">
                {{ t('pages.wallet.identity.cancel') }}
              </el-button>
              <el-button type="primary" @click="handleSubmitIdentity">
                {{ t('pages.wallet.identity.submit') }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshLeft, Search, Wallet } from '@element-plus/icons-vue'
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue'
import WorkspaceSidebar from '@/components/layout/WorkspaceSidebar.vue'
import WorkspaceTabs from '@/components/layout/WorkspaceTabs.vue'
import type { WorkspaceSidebarItem, WorkspaceTabItem } from '@/components/layout/workspace'

const { t } = useI18n()

const currentMenu = ref('account')
const activeTab = ref('clearing')
const withdrawTab = ref('available')
const expandedSidebarKeys = ref(['wallet'])

const walletSidebarItems = computed<WorkspaceSidebarItem[]>(() => [
  {
    key: 'wallet',
    label: t('pages.wallet.sidebar.wallet'),
    icon: Wallet,
    children: [
      { key: 'account', label: t('pages.wallet.sidebar.account') },
      { key: 'withdraw', label: t('pages.wallet.sidebar.withdraw') },
      { key: 'identity', label: t('pages.wallet.sidebar.identity') },
    ],
  },
])

const accountTabs = computed<WorkspaceTabItem[]>(() => [
  { name: 'clearing', label: t('pages.wallet.account.clearingTab') },
  { name: 'funds', label: t('pages.wallet.account.fundsTab') },
])

const withdrawTabs = computed<WorkspaceTabItem[]>(() => [
  { name: 'available', label: t('pages.wallet.withdraw.availableTab') },
  { name: 'history', label: t('pages.wallet.withdraw.historyTab') },
])

const handleSidebarParentClick = (key: string) => {
  if (key !== 'wallet') {
    return
  }

  expandedSidebarKeys.value = expandedSidebarKeys.value.includes(key) ? [] : [key]
}

const showIdentityForm = ref(false)

const identityForm = ref({
  firstName: '',
  lastName: '',
  companyName: '',
  companyPhone: '',
  detailAddress: '',
  city: '',
  stateProvince: '',
  postalCode: '',
  country: '',
})

const handleMenuClick = (menu: string) => {
  currentMenu.value = menu
}

const handleSubmitIdentity = () => {
  console.log('submit identity info:', identityForm.value)
  showIdentityForm.value = false
}

const formatDateTime = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const getDefaultDateTimeRange = () => {
  const now = new Date()
  const oneMonthAgo = new Date(now)
  oneMonthAgo.setMonth(now.getMonth() - 1)
  return {
    startDate: formatDateTime(oneMonthAgo),
    endDate: formatDateTime(now),
  }
}

const defaultDateTimeRange = getDefaultDateTimeRange()

const clearingFilters = ref({
  startDate: defaultDateTimeRange.startDate,
  endDate: defaultDateTimeRange.endDate,
  category: '',
  searchText: '',
})

const fundsFilters = ref({
  startDate: defaultDateTimeRange.startDate,
  endDate: defaultDateTimeRange.endDate,
  category: '',
  searchText: '',
})

const clearingData = ref<any[]>([])
const fundsData = ref<any[]>([])

const clearingPagination = ref({ current: 1, size: 20, total: 0 })
const fundsPagination = ref({ current: 1, size: 20, total: 0 })
const withdrawHistoryPagination = ref({ current: 1, size: 20, total: 0 })

const resetClearingDates = () => {
  const range = getDefaultDateTimeRange()
  clearingFilters.value.startDate = range.startDate
  clearingFilters.value.endDate = range.endDate
}

const resetFundsDates = () => {
  const range = getDefaultDateTimeRange()
  fundsFilters.value.startDate = range.startDate
  fundsFilters.value.endDate = range.endDate
}

const formatAmount = (amount: number) => {
  if (amount === undefined || amount === null) return '0.00'
  return amount.toFixed(2)
}

const handleClearingPageChange = (page: number) => {
  clearingPagination.value.current = page
}

const handleClearingSizeChange = (size: number) => {
  clearingPagination.value.size = size
  clearingPagination.value.current = 1
}

const handleFundsPageChange = (page: number) => {
  fundsPagination.value.current = page
}

const handleFundsSizeChange = (size: number) => {
  fundsPagination.value.size = size
  fundsPagination.value.current = 1
}

onMounted(() => {
  console.log('wallet page mounted')
})
</script>

<style scoped>
.wallet-page {
  min-width: 1120px;
  min-height: 100%;
  background: transparent;
}

.account-content,
.withdraw-content,
.identity-content {
  width: 100%;
  height: 100%;
}

.withdraw-card {
  background: #fff;
  border-radius: 4px;
  padding: 20px;
}

.withdraw-card h2 {
  font-size: 18px;
  color: #333;
  margin: 0 0 20px 0;
  font-weight: 500;
}

.withdraw-info {
  display: flex;
  gap: 40px;
  margin-bottom: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

.withdraw-info .info-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.withdraw-info .label {
  font-size: 14px;
  color: #666;
}

.withdraw-info .value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.withdraw-btn {
  margin-left: 20px;
}

.time-select,
.group-select {
  width: 150px;
}

.identity-verify {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: #fff;
  border-radius: 4px;
  padding: 40px;
}

.verify-illustration {
  width: 200px;
  height: 200px;
  margin-bottom: 30px;
}

.verify-illustration img {
  width: 100%;
  height: 100%;
}

.identity-verify h2 {
  font-size: 20px;
  color: #333;
  margin: 0 0 30px 0;
  font-weight: 500;
}

.identity-form-container {
  background: #fff;
  border-radius: 4px;
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
}

.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e8e8e8;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
}

.step.active .step-number {
  background: #1890ff;
  color: #fff;
}

.step-label {
  font-size: 14px;
  color: #666;
}

.step.active .step-label {
  color: #1890ff;
  font-weight: 500;
}

.step-line {
  width: 120px;
  height: 2px;
  background: #e8e8e8;
  margin: 0 20px;
  margin-bottom: 28px;
}

.identity-form {
  margin-top: 20px;
}

.identity-form .mt-2 {
  margin-top: 8px;
}

.wallet-tab-panel {
  background: #fff;
  border-radius: 4px;
}

.tab-content {
  padding: 0 0 20px;
}

.filter-section {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 16px;
  border: 1px solid #e8e8e8;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-row:last-child {
  margin-bottom: 0;
}

.filter-label {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  min-width: 70px;
}

.search-label {
  margin-left: 24px;
}

.filter-separator {
  font-size: 14px;
  color: #666;
  margin: 0 4px;
}

.date-picker {
  width: 200px;
}

.filter-select {
  width: 120px;
}

.search-input {
  width: 240px;
}

.data-table {
  margin-bottom: 16px;
}

.data-table :deep(.el-table__header th) {
  background: #fafafa;
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.data-table :deep(.el-table__row) {
  font-size: 13px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}

.pagination-container :deep(.el-pagination) {
  gap: 8px;
}

@media (max-width: 1200px) {
  .date-picker {
    width: 180px;
  }

  .search-input {
    width: 200px;
  }
}

@media (max-width: 768px) {
  .wallet-page {
    padding: 12px;
  }

  .tab-content {
    padding: 12px;
  }

  .filter-section {
    padding: 12px;
  }

  .filter-row {
    flex-wrap: wrap;
  }

  .date-picker,
  .filter-select,
  .search-input {
    width: 100%;
  }

  .search-label {
    margin-left: 0;
  }
}
</style>
