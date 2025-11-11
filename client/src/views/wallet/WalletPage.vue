<template>
  <div class="wallet-page">
    <!-- 左侧导航栏 -->
    <div class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header" @click="toggleSidebar">
        <el-icon class="sidebar-icon"><Wallet /></el-icon>
        <span v-if="!isCollapsed" class="sidebar-title">收起导航</span>
        <el-icon v-if="!isCollapsed" class="collapse-icon"><ArrowLeft /></el-icon>
        <el-icon v-else class="collapse-icon"><ArrowRight /></el-icon>
      </div>

      <div class="sidebar-menu">
        <div class="menu-section">
          <div
            class="menu-item menu-parent"
            :class="{ active: isWalletMenuActive }"
            @click="toggleWalletMenu"
          >
            <el-icon><Wallet /></el-icon>
            <span v-if="!isCollapsed" class="menu-text">钱包</span>
            <el-icon v-if="!isCollapsed" class="expand-icon">
              <ArrowRight v-if="!isWalletMenuExpanded" />
              <ArrowDown v-else />
            </el-icon>
          </div>

          <!-- 子菜单 -->
          <transition name="submenu">
            <div v-show="isWalletMenuExpanded && !isCollapsed" class="submenu-section">
              <div
                class="menu-item submenu-item"
                :class="{ active: currentMenu === 'account' }"
                @click.stop="handleMenuClick('account')"
              >
                <span class="menu-text">账户</span>
              </div>
              <div
                class="menu-item submenu-item"
                :class="{ active: currentMenu === 'withdraw' }"
                @click.stop="handleMenuClick('withdraw')"
              >
                <span class="menu-text">提现</span>
              </div>
              <div
                class="menu-item submenu-item"
                :class="{ active: currentMenu === 'identity' }"
                @click.stop="handleMenuClick('identity')"
              >
                <span class="menu-text">身份认证</span>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 账户页面:显示资金账户流水 -->
      <div v-if="currentMenu === 'account'" class="account-content">
        <el-tabs v-model="activeTab" class="wallet-tabs">
          <!-- 清算账户流水 -->
          <el-tab-pane label="清算账户流水" name="clearing">
        <div class="tab-content">
          <!-- 筛选条件 -->
          <div class="filter-section">
            <div class="filter-row">
              <span class="filter-label">创建时间</span>
              <el-date-picker
                v-model="clearingFilters.startDate"
                type="datetime"
                placeholder="选择日期时间"
                format="YYYY/MM/DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                size="default"
                class="date-picker"
              />
              <span class="filter-separator">至</span>
              <el-date-picker
                v-model="clearingFilters.endDate"
                type="datetime"
                placeholder="选择日期时间"
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
              <span class="filter-label">类别</span>
              <el-select
                v-model="clearingFilters.category"
                placeholder="全部"
                size="default"
                class="filter-select"
              >
                <el-option label="全部" value="" />
                <el-option label="收入" value="income" />
                <el-option label="支出" value="expense" />
              </el-select>

              <span class="filter-label search-label">搜索</span>
              <el-input
                v-model="clearingFilters.searchText"
                placeholder="搜索JID、交易ID"
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

          <!-- 数据表格 -->
          <el-table
            :data="clearingData"
            border
            style="width: 100%"
            empty-text="暂无数据"
            class="data-table"
          >
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column prop="businessType" label="业务类型" width="120" />
            <el-table-column prop="category" label="类别" width="100" />
            <el-table-column prop="paymentMethod" label="支付方式" width="120" />
            <el-table-column prop="prePayId" label="预付ID" width="150" />
            <el-table-column prop="transactionId" label="交易ID" width="200" />
            <el-table-column prop="currency" label="货币" width="80" />
            <el-table-column prop="amount" label="金额" width="120" align="right">
              <template #default="scope">
                {{ formatAmount(scope.row.amount) }}
              </template>
            </el-table-column>
            <el-table-column prop="availableAmount" label="可用金额" width="120" align="right">
              <template #default="scope">
                {{ formatAmount(scope.row.availableAmount) }}
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
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
      </el-tab-pane>

      <!-- 资金账户流水 -->
      <el-tab-pane label="资金账户流水" name="funds">
        <div class="tab-content">
          <!-- 筛选条件 -->
          <div class="filter-section">
            <div class="filter-row">
              <span class="filter-label">创建时间</span>
              <el-date-picker
                v-model="fundsFilters.startDate"
                type="datetime"
                placeholder="选择日期时间"
                format="YYYY/MM/DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                size="default"
                class="date-picker"
              />
              <span class="filter-separator">至</span>
              <el-date-picker
                v-model="fundsFilters.endDate"
                type="datetime"
                placeholder="选择日期时间"
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
              <span class="filter-label">类别</span>
              <el-select
                v-model="fundsFilters.category"
                placeholder="全部"
                size="default"
                class="filter-select"
              >
                <el-option label="全部" value="" />
                <el-option label="收入" value="income" />
                <el-option label="支出" value="expense" />
              </el-select>

              <span class="filter-label search-label">搜索</span>
              <el-input
                v-model="fundsFilters.searchText"
                placeholder="搜索JID、交易ID"
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

          <!-- 数据表格 -->
          <el-table
            :data="fundsData"
            border
            style="width: 100%"
            empty-text="暂无数据"
            class="data-table"
          >
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column prop="businessType" label="业务类型" width="120" />
            <el-table-column prop="category" label="类别" width="100" />
            <el-table-column prop="transactionId" label="交易ID" width="200" />
            <el-table-column prop="prePayId" label="预付ID" width="150" />
            <el-table-column prop="currency" label="货币" width="80" />
            <el-table-column prop="amount" label="金额" width="120" align="right">
              <template #default="scope">
                {{ formatAmount(scope.row.amount) }}
              </template>
            </el-table-column>
            <el-table-column prop="availableAmount" label="可用金额" width="120" align="right">
              <template #default="scope">
                {{ formatAmount(scope.row.availableAmount) }}
              </template>
            </el-table-column>
            <el-table-column prop="orderCompleteTime" label="订单完成时间" width="180" />
          </el-table>

          <!-- 分页 -->
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
      </el-tab-pane>
    </el-tabs>
      </div>

      <!-- 提现页面 -->
      <div v-else-if="currentMenu === 'withdraw'" class="withdraw-content">
        <el-tabs v-model="withdrawTab" class="wallet-tabs">
          <!-- 可提现标签页 -->
          <el-tab-pane label="可提现" name="available">
            <div class="tab-content">
              <div class="withdraw-card">
                <h2>可提现</h2>
                <div class="withdraw-info">
                  <div class="info-item">
                    <span class="label">订单数</span>
                    <span class="value">0</span>
                  </div>
                  <div class="info-item">
                    <span class="label">提现金额</span>
                    <span class="value">0</span>
                    <el-button type="primary" class="withdraw-btn">提现</el-button>
                  </div>
                </div>

                <div class="filter-section">
                  <div class="filter-row">
                    <span class="filter-label">选择时间</span>
                    <el-select placeholder="选择时间" class="time-select">
                      <el-option label="选择时间" value="" />
                    </el-select>
                    <el-date-picker placeholder="开始时间" class="date-picker" />
                    <span class="filter-separator">至</span>
                    <el-date-picker placeholder="结束时间" class="date-picker" />
                    <el-button type="text">
                      <el-icon><RefreshLeft /></el-icon>
                    </el-button>
                    <span class="filter-label">选择分组</span>
                    <el-select placeholder="请选择" class="group-select">
                      <el-option label="请选择" value="" />
                    </el-select>
                  </div>
                </div>

                <el-table :data="[]" border class="data-table" empty-text="暂无数据">
                  <el-table-column prop="settleTime" label="结算时间" />
                  <el-table-column prop="businessType" label="业务类型" />
                  <el-table-column prop="prePayId" label="预付ID" />
                  <el-table-column prop="currency" label="货币" />
                  <el-table-column prop="settleAmount" label="结算金额" />
                  <el-table-column prop="createTime" label="创建时间" />
                  <el-table-column prop="orderCompleteTime" label="订单完成时间" />
                  <el-table-column prop="groupName" label="分组名称" />
                </el-table>
              </div>
            </div>
          </el-tab-pane>

          <!-- 提现记录标签页 -->
          <el-tab-pane label="提现记录" name="history">
            <div class="tab-content">
              <div class="filter-section">
                <div class="filter-row">
                  <span class="filter-label">时间</span>
                  <el-date-picker placeholder="开始时间" class="date-picker" />
                  <span class="filter-separator">至</span>
                  <el-date-picker placeholder="结束时间" class="date-picker" />
                  <el-button type="text">
                    <el-icon><RefreshLeft /></el-icon>
                  </el-button>
                </div>
                <div class="filter-row">
                  <span class="filter-label">状态</span>
                  <el-select placeholder="All" class="status-select">
                    <el-option label="All" value="" />
                    <el-option label="成功" value="success" />
                    <el-option label="处理中" value="processing" />
                    <el-option label="失败" value="failed" />
                  </el-select>
                  <span class="filter-label search-label">搜索</span>
                  <el-input placeholder="搜索期间ID" clearable class="search-input">
                    <template #prefix>
                      <el-icon><Search /></el-icon>
                    </template>
                  </el-input>
                </div>
              </div>

              <el-table :data="[]" border class="data-table" empty-text="暂无数据">
                <el-table-column prop="withdrawTime" label="提现时间" width="180" />
                <el-table-column prop="paymentId" label="支付ID" width="200" />
                <el-table-column prop="orderCount" label="订单数" width="100" />
                <el-table-column prop="currency" label="货币" width="100" />
                <el-table-column prop="withdrawAmount" label="提现金额" width="120" align="right" />
                <el-table-column prop="withdrawFee" label="提现手续费" width="120" align="right" />
                <el-table-column prop="actualAmount" label="到账金额" width="120" align="right" />
                <el-table-column prop="withdrawStatus" label="提现状态" width="120" />
                <el-table-column prop="refundResult" label="反馈结果" width="150" />
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
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 身份认证页面 -->
      <div v-else-if="currentMenu === 'identity'" class="identity-content">
        <div v-if="!showIdentityForm" class="identity-verify">
          <div class="verify-illustration">
            <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200'%3E%3Ccircle cx='100' cy='100' r='80' fill='%23e6f7ff'/%3E%3Ccircle cx='100' cy='100' r='60' fill='%231890ff' opacity='0.2'/%3E%3Cpath d='M70 100 L90 120 L130 80' stroke='%231890ff' stroke-width='8' fill='none' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E" alt="认证" />
          </div>
          <h2>认证后开始接收款项</h2>
          <el-button type="primary" size="large" @click="showIdentityForm = true">获得认证</el-button>
        </div>

        <div v-else class="identity-form-container">
          <div class="steps">
            <div class="step active">
              <div class="step-number">1</div>
              <div class="step-label">提交</div>
            </div>
            <div class="step-line"></div>
            <div class="step">
              <div class="step-number">2</div>
              <div class="step-label">审核中</div>
            </div>
            <div class="step-line"></div>
            <div class="step">
              <div class="step-number">3</div>
              <div class="step-label">认证成功</div>
            </div>
          </div>

          <el-form :model="identityForm" label-width="100px" class="identity-form">
            <el-form-item label="公司联系人">
              <el-input v-model="identityForm.firstName" placeholder="名字" />
              <el-input v-model="identityForm.lastName" placeholder="姓氏" class="mt-2" />
            </el-form-item>
            <el-form-item label="公司名称">
              <el-input v-model="identityForm.companyName" placeholder="公司名称" />
            </el-form-item>
            <el-form-item label="公司电话">
              <el-input v-model="identityForm.companyPhone" placeholder="公司电话" />
            </el-form-item>
            <el-form-item label="公司地址">
              <el-input v-model="identityForm.detailAddress" placeholder="详细地址" />
              <el-input v-model="identityForm.city" placeholder="城市" class="mt-2" />
              <el-input v-model="identityForm.stateProvince" placeholder="州/省/国家" class="mt-2" />
              <el-input v-model="identityForm.postalCode" placeholder="邮政编码" class="mt-2" />
              <el-input v-model="identityForm.country" placeholder="国家" class="mt-2" />
            </el-form-item>
            <el-form-item>
              <el-button @click="showIdentityForm = false">取消</el-button>
              <el-button type="primary" @click="handleSubmitIdentity">提交</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Search, RefreshLeft, Wallet, ArrowLeft, ArrowRight, ArrowDown } from '@element-plus/icons-vue'

// 侧边栏折叠状态
const isCollapsed = ref(false)

// 钱包菜单展开状态
const isWalletMenuExpanded = ref(true)

// 当前菜单
const currentMenu = ref('account')

// 活动标签页
const activeTab = ref('clearing')

// 提现标签页
const withdrawTab = ref('available')

// 判断钱包菜单是否处于激活状态(任一子菜单被选中)
const isWalletMenuActive = computed(() => {
  return ['account', 'withdraw', 'identity'].includes(currentMenu.value)
})

// 切换侧边栏折叠状态
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

// 切换钱包菜单展开/收起
const toggleWalletMenu = () => {
  isWalletMenuExpanded.value = !isWalletMenuExpanded.value
}

// 身份认证表单显示状态
const showIdentityForm = ref(false)

// 身份认证表单数据
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

// 菜单点击事件
const handleMenuClick = (menu: string) => {
  currentMenu.value = menu
  // TODO: 根据菜单项切换内容
}

// 提交身份认证
const handleSubmitIdentity = () => {
  // TODO: 提交认证信息到后端
  console.log('提交认证信息:', identityForm.value)
  showIdentityForm.value = false
}

// 清算账户流水筛选条件
const clearingFilters = ref({
  startDate: '2025/10/08 00:00:00',
  endDate: '2025/11/08 23:59:59',
  category: '',
  searchText: '',
})

// 资金账户流水筛选条件
const fundsFilters = ref({
  startDate: '2025/10/08 00:00:00',
  endDate: '2025/11/08 23:59:59',
  category: '',
  searchText: '',
})

// 数据
const clearingData = ref<any[]>([])
const fundsData = ref<any[]>([])

// 分页
const clearingPagination = ref({ current: 1, size: 20, total: 0 })
const fundsPagination = ref({ current: 1, size: 20, total: 0 })
const withdrawHistoryPagination = ref({ current: 1, size: 20, total: 0 })

// 重置日期范围
const resetClearingDates = () => {
  const now = new Date()
  const oneMonthAgo = new Date(now)
  oneMonthAgo.setMonth(now.getMonth() - 1)

  clearingFilters.value.startDate = formatDateTime(oneMonthAgo)
  clearingFilters.value.endDate = formatDateTime(now)
}

const resetFundsDates = () => {
  const now = new Date()
  const oneMonthAgo = new Date(now)
  oneMonthAgo.setMonth(now.getMonth() - 1)

  fundsFilters.value.startDate = formatDateTime(oneMonthAgo)
  fundsFilters.value.endDate = formatDateTime(now)
}

// 格式化日期时间
const formatDateTime = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}/${month}/${day} ${hours}:${minutes}:${seconds}`
}

// 格式化金额
const formatAmount = (amount: number) => {
  if (amount === undefined || amount === null) return '0.00'
  return amount.toFixed(2)
}

// 分页事件
const handleClearingPageChange = (page: number) => {
  clearingPagination.value.current = page
  // TODO: 加载数据
}

const handleClearingSizeChange = (size: number) => {
  clearingPagination.value.size = size
  clearingPagination.value.current = 1
  // TODO: 加载数据
}

const handleFundsPageChange = (page: number) => {
  fundsPagination.value.current = page
  // TODO: 加载数据
}

const handleFundsSizeChange = (size: number) => {
  fundsPagination.value.size = size
  fundsPagination.value.current = 1
  // TODO: 加载数据
}

// 组件挂载时初始化
onMounted(() => {
  // TODO: 加载初始数据
})
</script>

<style scoped>
.wallet-page {
  display: flex;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}

/* 左侧边栏 */
.sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  flex-shrink: 0;
  transition: width 0.3s ease;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sidebar.collapsed .sidebar-header {
  padding: 16px 12px;
  justify-content: center;
}

.sidebar-header:hover {
  background-color: #f5f5f5;
}

.sidebar-icon {
  font-size: 18px;
  color: #333;
  flex-shrink: 0;
}

.sidebar-title {
  flex: 1;
  font-size: 14px;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
}

.collapse-icon {
  font-size: 16px;
  color: #999;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.sidebar-menu {
  padding: 8px 0;
}

.menu-section {
  padding: 4px 0;
}

.menu-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 8px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #666;
  font-size: 14px;
  position: relative;
}

.sidebar.collapsed .menu-item {
  padding: 12px 0;
  justify-content: center;
}

.menu-item:hover {
  background-color: #f5f7fa;
  color: #333;
}

.menu-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
  font-weight: 500;
}

.menu-item .el-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.menu-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
}

.menu-char {
  font-size: 14px;
  font-weight: 500;
}

/* 父菜单项样式 */
.menu-parent {
  position: relative;
}

.expand-icon {
  margin-left: auto;
  font-size: 14px;
  transition: transform 0.3s ease;
}

/* 子菜单区域 */
.submenu-section {
  background-color: #fafafa;
  overflow: hidden;
}

.submenu-item {
  padding-left: 48px !important;
  font-size: 13px;
}

.submenu-item:hover {
  background-color: #f0f0f0;
}

.submenu-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

/* 子菜单展开/收起动画 */
.submenu-enter-active,
.submenu-leave-active {
  transition: all 0.3s ease;
}

.submenu-enter-from,
.submenu-leave-to {
  max-height: 0;
  opacity: 0;
}

.submenu-enter-to,
.submenu-leave-from {
  max-height: 200px;
  opacity: 1;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

.account-content,
.withdraw-content,
.identity-content {
  width: 100%;
  height: 100%;
}

/* 提现页面样式 */
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

/* 身份认证页面样式 */
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

.wallet-tabs {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.wallet-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
}

.wallet-tabs :deep(.el-tabs__nav-wrap) {
  padding-top: 0;
}

.wallet-tabs :deep(.el-tabs__item) {
  height: 50px;
  line-height: 50px;
  font-size: 14px;
  padding: 0 20px;
}

.tab-content {
  padding: 20px;
}

/* 筛选条件区域 */
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

/* 数据表格 */
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

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}

.pagination-container :deep(.el-pagination) {
  gap: 8px;
}

/* 响应式 */
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
