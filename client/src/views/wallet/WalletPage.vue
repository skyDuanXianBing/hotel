<template>
  <div class="wallet-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>订单钱包</h2>
    </div>

    <!-- 统计卡片区域 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-label">
          余额（元）
          <el-icon class="help-icon"><QuestionFilled /></el-icon>
        </div>
        <div class="stat-value-row">
          <span class="stat-value">¥ {{ walletStats.balance }}</span>
          <el-button type="primary" size="small" @click="handleWithdraw">订单提现</el-button>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-label">提现中（元）</div>
        <div class="stat-value">¥ {{ walletStats.withdrawing }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">
          待入账（元）
          <el-icon class="help-icon"><QuestionFilled /></el-icon>
        </div>
        <div class="stat-value">¥ {{ walletStats.pending }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计提现（元）</div>
        <div class="stat-value">¥ {{ walletStats.totalWithdrawn }}</div>
      </div>
    </div>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="wallet-tabs">
      <!-- 1. 余额变动记录 -->
      <el-tab-pane label="余额变动记录" name="balance">
        <div class="tab-content">
          <!-- 搜索栏 -->
          <div class="search-bar">
            <el-input
              v-model="balanceFilters.searchText"
              placeholder="搜索订单号、流水号、结算单号、备注"
              clearable
              class="search-input"
            >
              <template #suffix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <div class="filter-row">
              <div class="filter-item">
                <span class="filter-label">时间</span>
                <el-date-picker
                  v-model="balanceFilters.dateRange"
                  type="daterange"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  size="small"
                />
              </div>
              <div class="filter-item">
                <span class="filter-label">类型</span>
                <el-select v-model="balanceFilters.type" placeholder="全部" size="small">
                  <el-option label="全部" value="" />
                </el-select>
              </div>
            </div>
            <div class="filter-row">
              <div class="filter-item">
                <span class="filter-label">支付方式</span>
                <el-select v-model="balanceFilters.paymentMethod" placeholder="全部" size="small">
                  <el-option label="全部" value="" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">订单类目</span>
                <el-select v-model="balanceFilters.orderCategory" placeholder="全部" size="small">
                  <el-option label="全部" value="" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">状态</span>
                <el-select v-model="balanceFilters.status" placeholder="全部" size="small">
                  <el-option label="全部" value="" />
                </el-select>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <div></div>
            <el-button type="primary" size="small">导出明细</el-button>
          </div>

          <!-- 数据表格 -->
          <el-table :data="balanceRecords" style="width: 100%" empty-text="暂无数据">
            <el-table-column prop="time" label="时间" width="180" />
            <el-table-column prop="type" label="类型" width="120" />
            <el-table-column prop="amount" label="金额" width="120">
              <template #default="scope">
                <span :class="scope.row.amount > 0 ? 'amount-positive' : 'amount-negative'">
                  {{ scope.row.amount > 0 ? '+' : '' }}¥{{ Math.abs(scope.row.amount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="paymentMethod" label="支付方式" width="120" />
            <el-table-column prop="orderNo" label="关联订单号" width="150" />
            <el-table-column prop="channel" label="订单渠道" width="120" />
            <el-table-column prop="transactionNo" label="流水号" width="180" />
            <el-table-column prop="thirdPartyNo" label="三方流水号">
              <template #header>
                三方流水号
                <el-icon class="help-icon"><QuestionFilled /></el-icon>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页和统计 -->
          <div class="table-footer">
            <div class="footer-summary">
              共计 {{ balancePagination.total }} 条记录，金额合计：<span class="amount-total">¥{{ balanceTotalAmount }}</span>
            </div>
            <el-pagination
              v-model:current-page="balancePagination.current"
              v-model:page-size="balancePagination.size"
              :total="balancePagination.total"
              :page-sizes="[25, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @current-change="handleBalancePageChange"
              @size-change="handleBalanceSizeChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 2. 提现记录 -->
      <el-tab-pane label="提现记录" name="withdraw">
        <div class="tab-content">
          <!-- 搜索栏 -->
          <div class="search-bar">
            <div class="filter-row">
              <div class="filter-item">
                <span class="filter-label">申请时间</span>
                <el-date-picker
                  v-model="withdrawFilters.dateRange"
                  type="daterange"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  size="small"
                />
              </div>
              <div class="filter-item">
                <span class="filter-label">账户类型</span>
                <el-select v-model="withdrawFilters.accountType" placeholder="全部" size="small">
                  <el-option label="全部" value="" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">状态</span>
                <el-select v-model="withdrawFilters.status" placeholder="全部" size="small">
                  <el-option label="全部" value="" />
                </el-select>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <div></div>
            <el-button type="primary" size="small">导出明细</el-button>
          </div>

          <!-- 数据表格 -->
          <el-table :data="withdrawRecords" style="width: 100%" empty-text="暂无数据">
            <el-table-column prop="applyTime" label="申请时间" width="180" />
            <el-table-column prop="amount" label="金额" width="120">
              <template #default="scope">¥{{ scope.row.amount }}</template>
            </el-table-column>
            <el-table-column prop="accountType" label="账户类型" width="120" />
            <el-table-column prop="accountInfo" label="账号信息" width="200" />
            <el-table-column prop="applicant" label="申请人" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="processTime" label="处理时间" width="180" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default>
                <el-button link type="primary" size="small">查看</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页和统计 -->
          <div class="table-footer">
            <div class="footer-summary">
              共计 {{ withdrawPagination.total }} 条记录，金额合计：<span class="amount-total">¥{{ withdrawTotalAmount }}</span>
            </div>
            <el-pagination
              v-model:current-page="withdrawPagination.current"
              v-model:page-size="withdrawPagination.size"
              :total="withdrawPagination.total"
              :page-sizes="[25, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @current-change="handleWithdrawPageChange"
              @size-change="handleWithdrawSizeChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 3. 入账明细 -->
      <el-tab-pane label="入账明细" name="deposit">
        <div class="tab-content">
          <!-- 子标签页 -->
          <el-tabs v-model="depositSubTab" class="deposit-tabs">
            <!-- 已入账订单 -->
            <el-tab-pane label="已入账订单" name="deposited">
              <!-- 搜索栏 -->
              <div class="search-bar">
                <el-input
                  v-model="depositedFilters.searchText"
                  placeholder="搜索订单号、流水号、备注"
                  clearable
                  class="search-input"
                >
                  <template #suffix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <div class="filter-row">
                  <div class="filter-item">
                    <span class="filter-label">发生时间</span>
                    <el-date-picker
                      v-model="depositedFilters.occurDateRange"
                      type="daterange"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      size="small"
                    />
                  </div>
                  <div class="filter-item">
                    <span class="filter-label">类型</span>
                    <el-select v-model="depositedFilters.type" placeholder="全部" size="small">
                      <el-option label="全部" value="" />
                    </el-select>
                  </div>
                </div>
                <div class="filter-row">
                  <div class="filter-item">
                    <span class="filter-label">订单渠道</span>
                    <el-select v-model="depositedFilters.channel" placeholder="全部" size="small">
                      <el-option label="全部" value="" />
                    </el-select>
                  </div>
                  <div class="filter-item">
                    <span class="filter-label">订单类型</span>
                    <el-select v-model="depositedFilters.orderType" placeholder="全部" size="small">
                      <el-option label="全部" value="" />
                    </el-select>
                  </div>
                  <div class="filter-item">
                    <span class="filter-label">提现状态</span>
                    <el-select v-model="depositedFilters.withdrawStatus" placeholder="全部" size="small">
                      <el-option label="全部" value="" />
                    </el-select>
                  </div>
                  <div class="filter-item">
                    <span class="filter-label">支付方式</span>
                    <el-select v-model="depositedFilters.paymentMethod" placeholder="全部" size="small">
                      <el-option label="全部" value="" />
                    </el-select>
                  </div>
                </div>
                <div class="filter-row">
                  <div class="filter-item">
                    <span class="filter-label">提现时间</span>
                    <el-date-picker
                      v-model="depositedFilters.withdrawDateRange"
                      type="daterange"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      size="small"
                    />
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="action-buttons">
                <div></div>
                <el-button type="primary" size="small">导出明细</el-button>
              </div>

              <!-- 数据表格 -->
              <el-table :data="depositedRecords" style="width: 100%" empty-text="暂无数据">
                <el-table-column prop="occurTime" label="发生时间" width="180" />
                <el-table-column prop="type" label="类型" width="100" />
                <el-table-column prop="orderNo" label="订单号" width="180" />
                <el-table-column prop="channel" label="订单渠道" width="120" />
                <el-table-column prop="orderType" label="订单类型" width="120" />
                <el-table-column prop="settlementAmount" label="结算金额" width="120">
                  <template #default="scope">¥{{ scope.row.settlementAmount }}</template>
                </el-table-column>
                <el-table-column prop="withdrawStatus" label="提现状态" width="100">
                  <template #default="scope">
                    <el-tag :type="getWithdrawStatusType(scope.row.withdrawStatus)">
                      {{ scope.row.withdrawStatus }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="withdrawTime" label="提现时间" width="180" />
              </el-table>

              <!-- 分页和统计 -->
              <div class="table-footer">
                <div class="footer-summary">
                  <div>共计 {{ depositedPagination.total }} 条记录，结算金额合计：<span class="amount-total">¥{{ depositedTotalAmount }}</span></div>
                  <div class="fee-summary">
                    支付金额：<span class="fee-amount">¥{{ depositedFees.payment }}</span>，
                    渠道佣金：<span class="fee-amount">¥{{ depositedFees.channelCommission }}</span>，
                    平台服务费：<span class="fee-amount">¥{{ depositedFees.platformService }}</span>，
                    交易手续费：<span class="fee-amount">¥{{ depositedFees.transactionFee }}</span>，
                    分销佣金：<span class="fee-amount">¥{{ depositedFees.distributionCommission }}</span>
                  </div>
                </div>
                <el-pagination
                  v-model:current-page="depositedPagination.current"
                  v-model:page-size="depositedPagination.size"
                  :total="depositedPagination.total"
                  :page-sizes="[25, 50, 100]"
                  layout="total, sizes, prev, pager, next"
                  @current-change="handleDepositedPageChange"
                  @size-change="handleDepositedSizeChange"
                />
              </div>
            </el-tab-pane>

            <!-- 待入账订单 -->
            <el-tab-pane label="待入账订单" name="pending">
              <!-- 搜索栏 -->
              <div class="search-bar">
                <el-input
                  v-model="pendingFilters.searchText"
                  placeholder="搜索订单号、流水号、备注"
                  clearable
                  class="search-input"
                >
                  <template #suffix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <div class="filter-row">
                  <div class="filter-item">
                    <span class="filter-label">订单创建时间</span>
                    <el-date-picker
                      v-model="pendingFilters.createDateRange"
                      type="daterange"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      size="small"
                    />
                  </div>
                  <div class="filter-item">
                    <span class="filter-label">订单渠道</span>
                    <el-select v-model="pendingFilters.channel" placeholder="全部" size="small">
                      <el-option label="全部" value="" />
                    </el-select>
                  </div>
                </div>
                <div class="filter-row">
                  <div class="filter-item">
                    <span class="filter-label">订单类型</span>
                    <el-select v-model="pendingFilters.orderType" placeholder="全部" size="small">
                      <el-option label="全部" value="" />
                    </el-select>
                  </div>
                  <div class="filter-item">
                    <span class="filter-label">支付方式</span>
                    <el-select v-model="pendingFilters.paymentMethod" placeholder="全部" size="small">
                      <el-option label="全部" value="" />
                    </el-select>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="action-buttons">
                <div></div>
                <el-button type="primary" size="small">导出明细</el-button>
              </div>

              <!-- 数据表格 -->
              <el-table :data="pendingRecords" style="width: 100%" empty-text="暂无数据">
                <el-table-column prop="createTime" label="订单创建时间" width="180" />
                <el-table-column prop="orderNo" label="订单号" width="180" />
                <el-table-column prop="channel" label="订单渠道" width="120" />
                <el-table-column prop="orderType" label="订单类型" width="120" />
                <el-table-column prop="pendingAmount" label="待结算金额" width="120">
                  <template #default="scope">¥{{ scope.row.pendingAmount }}</template>
                </el-table-column>
                <el-table-column prop="paymentMethod" label="支付方式" width="120" />
                <el-table-column prop="paymentAmount" label="支付金额" width="120">
                  <template #default="scope">¥{{ scope.row.paymentAmount }}</template>
                </el-table-column>
                <el-table-column prop="refundAmount" label="退款金额" width="120">
                  <template #default="scope">¥{{ scope.row.refundAmount }}</template>
                </el-table-column>
              </el-table>

              <!-- 分页和统计 -->
              <div class="table-footer">
                <div class="footer-summary">
                  <div>共计 {{ pendingPagination.total }} 条记录，待结算金额合计：<span class="amount-total">¥{{ pendingTotalAmount }}</span></div>
                  <div class="fee-summary">
                    支付金额：<span class="fee-amount">¥{{ pendingFees.payment }}</span>，
                    渠道佣金：<span class="fee-amount">¥{{ pendingFees.channelCommission }}</span>，
                    平台服务费：<span class="fee-amount">{{ pendingFees.platformService }}</span>，
                    交易手续费：<span class="fee-amount">¥{{ pendingFees.transactionFee }}</span>，
                    分销佣金：<span class="fee-amount">¥{{ pendingFees.distributionCommission }}</span>
                  </div>
                </div>
                <el-pagination
                  v-model:current-page="pendingPagination.current"
                  v-model:page-size="pendingPagination.size"
                  :total="pendingPagination.total"
                  :page-sizes="[25, 50, 100]"
                  layout="total, sizes, prev, pager, next"
                  @current-change="handlePendingPageChange"
                  @size-change="handlePendingSizeChange"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { QuestionFilled, Search } from '@element-plus/icons-vue'

// 钱包统计数据
const walletStats = ref({
  balance: 0,
  withdrawing: 0,
  pending: 0,
  totalWithdrawn: 0
})

// 活动标签页
const activeTab = ref('balance')
const depositSubTab = ref('deposited')

// 余额变动记录筛选条件
const balanceFilters = ref({
  searchText: '',
  dateRange: null as any,
  type: '',
  paymentMethod: '',
  orderCategory: '',
  status: ''
})

// 提现记录筛选条件
const withdrawFilters = ref({
  dateRange: null as any,
  accountType: '',
  status: ''
})

// 已入账订单筛选条件
const depositedFilters = ref({
  searchText: '',
  occurDateRange: null as any,
  type: '',
  channel: '',
  orderType: '',
  withdrawStatus: '',
  paymentMethod: '',
  withdrawDateRange: null as any
})

// 待入账订单筛选条件
const pendingFilters = ref({
  searchText: '',
  createDateRange: null as any,
  channel: '',
  orderType: '',
  paymentMethod: ''
})

// 数据
const balanceRecords = ref<any[]>([])
const withdrawRecords = ref<any[]>([])
const depositedRecords = ref<any[]>([])
const pendingRecords = ref<any[]>([])

// 分页
const balancePagination = ref({ current: 1, size: 25, total: 0 })
const withdrawPagination = ref({ current: 1, size: 25, total: 0 })
const depositedPagination = ref({ current: 1, size: 25, total: 0 })
const pendingPagination = ref({ current: 1, size: 25, total: 0 })

// 统计金额
const balanceTotalAmount = ref(0)
const withdrawTotalAmount = ref(0)
const depositedTotalAmount = ref(0)
const pendingTotalAmount = ref(0)

const depositedFees = ref({
  payment: 0,
  channelCommission: 0,
  platformService: 0,
  transactionFee: 0,
  distributionCommission: 0
})

const pendingFees = ref({
  payment: 0,
  channelCommission: 0,
  platformService: 0,
  transactionFee: 0,
  distributionCommission: 0
})

// 提现操作
const handleWithdraw = () => {
  ElMessage.success('提现成功')
}

// 分页事件
const handleBalancePageChange = (page: number) => {
  balancePagination.value.current = page
}

const handleBalanceSizeChange = (size: number) => {
  balancePagination.value.size = size
}

const handleWithdrawPageChange = (page: number) => {
  withdrawPagination.value.current = page
}

const handleWithdrawSizeChange = (size: number) => {
  withdrawPagination.value.size = size
}

const handleDepositedPageChange = (page: number) => {
  depositedPagination.value.current = page
}

const handleDepositedSizeChange = (size: number) => {
  depositedPagination.value.size = size
}

const handlePendingPageChange = (page: number) => {
  pendingPagination.value.current = page
}

const handlePendingSizeChange = (size: number) => {
  pendingPagination.value.size = size
}

// 状态类型转换
const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    '待处理': 'warning',
    '处理中': 'info',
    '已完成': 'success',
    '已拒绝': 'danger'
  }
  return typeMap[status] || 'info'
}

const getWithdrawStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    '未提现': 'info',
    '已提现': 'success',
    '提现中': 'warning'
  }
  return typeMap[status] || 'info'
}
</script>

<style scoped>
.wallet-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.help-icon {
  color: #999;
  font-size: 14px;
  cursor: help;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

.stat-value-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 标签页 */
.wallet-tabs {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.tab-content {
  margin-top: 20px;
}

/* 搜索栏 */
.search-bar {
  margin-bottom: 16px;
}

.search-input {
  width: 400px;
  margin-bottom: 12px;
}

.filter-row {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
}

/* 表格相关 */
.amount-positive {
  color: #67c23a;
}

.amount-negative {
  color: #f56c6c;
}

.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.footer-summary {
  font-size: 14px;
  color: #666;
}

.amount-total {
  color: #409eff;
  font-weight: 600;
}

.fee-summary {
  margin-top: 8px;
  font-size: 13px;
  color: #999;
}

.fee-amount {
  color: #409eff;
  font-weight: 500;
}

/* 子标签页 */
.deposit-tabs {
  margin-top: -20px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }

  .search-input {
    width: 100%;
  }

  .filter-row {
    flex-direction: column;
    gap: 12px;
  }

  .filter-item {
    width: 100%;
  }
}
</style>
