<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title>钱包</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page wallet-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新钱包" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero wallet-hero">
        <p class="mobile-note wallet-hero__eyebrow">钱包迁移</p>
        <h1 class="mobile-title">订单钱包</h1>
        <p class="mobile-subtitle">移动端以查询和提交为主，保留账户、提现和认证三块核心语义。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">余额 ¥{{ formatCurrency(stats.balance) }}</span>
          <span class="mobile-chip">提现中 ¥{{ formatCurrency(stats.withdrawing) }}</span>
          <span class="mobile-chip">待入账 ¥{{ formatCurrency(stats.pending) }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <ion-segment :value="activeSegment" @ionChange="handleSegmentChange">
            <ion-segment-button value="account">
              <ion-label>账户</ion-label>
            </ion-segment-button>
            <ion-segment-button value="withdraw">
              <ion-label>提现</ion-label>
            </ion-segment-button>
            <ion-segment-button value="identity">
              <ion-label>认证</ion-label>
            </ion-segment-button>
          </ion-segment>
        </section>

        <section class="mobile-card wallet-summary-card">
          <div class="wallet-summary-grid">
            <article class="wallet-stat-card">
              <span>可用余额</span>
              <strong>¥{{ formatCurrency(stats.balance) }}</strong>
            </article>
            <article class="wallet-stat-card">
              <span>提现中</span>
              <strong>¥{{ formatCurrency(stats.withdrawing) }}</strong>
            </article>
            <article class="wallet-stat-card">
              <span>待入账</span>
              <strong>¥{{ formatCurrency(stats.pending) }}</strong>
            </article>
            <article class="wallet-stat-card">
              <span>累计提现</span>
              <strong>¥{{ formatCurrency(stats.totalWithdrawn) }}</strong>
            </article>
          </div>

          <p v-if="loadNotice" class="mobile-note wallet-summary-card__notice">{{ loadNotice }}</p>
        </section>

        <section v-if="activeSegment === 'account'" class="mobile-card wallet-account-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">账户流水</h2>
              <p class="mobile-note">按移动端展示清算流水与入账明细，避免桌面大表格。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <ion-segment :value="accountSegment" @ionChange="handleAccountSegmentChange">
            <ion-segment-button value="balance">
              <ion-label>余额流水</ion-label>
            </ion-segment-button>
            <ion-segment-button value="deposit">
              <ion-label>入账明细</ion-label>
            </ion-segment-button>
          </ion-segment>

          <div v-if="accountSegment === 'balance' && balanceRecords.length > 0" class="mobile-list wallet-list">
            <article v-for="item in balanceRecords" :key="item.id" class="wallet-record-card">
              <div class="wallet-record-card__header">
                <strong>{{ item.type || '余额变动' }}</strong>
                <strong>¥{{ formatCurrency(item.amount) }}</strong>
              </div>
              <p>{{ item.channel || '未知渠道' }} · {{ item.paymentMethod || '支付方式待确认' }}</p>
              <p>{{ item.orderNo || item.transactionNo || '无订单号' }} · {{ item.time || '时间未知' }}</p>
            </article>
          </div>

          <div v-if="accountSegment === 'deposit' && depositRecords.length > 0" class="mobile-list wallet-list">
            <article v-for="item in depositRecords" :key="item.id" class="wallet-record-card">
              <div class="wallet-record-card__header">
                <strong>{{ item.type || '入账明细' }}</strong>
                <strong>¥{{ formatCurrency(item.settlementAmount) }}</strong>
              </div>
              <p>{{ item.channel || '未知渠道' }} · {{ item.orderType || '订单类型待确认' }}</p>
              <p>{{ item.orderNo || '无订单号' }} · {{ item.occurTime || '时间未知' }}</p>
            </article>
          </div>

          <div
            v-if="accountSegment === 'balance' && balanceRecords.length === 0 && !loading"
            class="wallet-empty-state"
          >
            <h3>暂无余额流水</h3>
            <p class="mobile-note">
              {{ walletServiceUnavailable ? WALLET_SERVICE_UNAVAILABLE_NOTICE : '当前没有可展示的余额变动记录。' }}
            </p>
          </div>

          <div
            v-if="accountSegment === 'deposit' && depositRecords.length === 0 && !loading"
            class="wallet-empty-state"
          >
            <h3>暂无入账明细</h3>
            <p class="mobile-note">
              {{ walletServiceUnavailable ? WALLET_SERVICE_UNAVAILABLE_NOTICE : '当前没有可展示的入账记录。' }}
            </p>
          </div>
        </section>

        <section v-if="activeSegment === 'withdraw'" class="mobile-card wallet-withdraw-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">提现申请</h2>
              <p class="mobile-note">移动端保留申请入口和历史记录，适合快速提交提现。</p>
            </div>
            <ion-spinner v-if="submittingWithdraw" name="crescent" />
          </div>

          <div class="wallet-withdraw-overview">
            <article class="wallet-withdraw-overview__item">
              <span>当前可提现</span>
              <strong>¥{{ formatCurrency(stats.balance) }}</strong>
            </article>
            <article class="wallet-withdraw-overview__item">
              <span>累计提现</span>
              <strong>¥{{ formatCurrency(stats.totalWithdrawn) }}</strong>
            </article>
          </div>

          <div class="wallet-form-grid">
            <label class="wallet-form-field">
              <span>提现金额</span>
              <ion-input v-model="withdrawForm.amount" fill="outline" inputmode="decimal" placeholder="请输入提现金额" />
            </label>

            <label class="wallet-form-field">
              <span>账户类型</span>
              <ion-select v-model="withdrawForm.accountType" fill="outline" interface="action-sheet">
                <ion-select-option value="bank">银行卡</ion-select-option>
                <ion-select-option value="alipay">支付宝</ion-select-option>
                <ion-select-option value="wechat">微信</ion-select-option>
              </ion-select>
            </label>

            <label class="wallet-form-field wallet-form-field--full">
              <span>到账账户</span>
              <ion-input v-model="withdrawForm.accountInfo" fill="outline" placeholder="请输入银行卡号或收款账号" />
            </label>
          </div>

          <div class="wallet-form-actions">
            <ion-button fill="outline" @click="resetWithdrawForm">重置</ion-button>
            <ion-button :disabled="submittingWithdraw || walletServiceUnavailable" @click="handleSubmitWithdraw">
              {{ submittingWithdraw ? '提交中...' : '提交提现申请' }}
            </ion-button>
          </div>

          <p v-if="walletServiceUnavailable" class="mobile-note">{{ WALLET_WITHDRAW_UNAVAILABLE_NOTICE }}</p>

          <div v-if="withdrawRecords.length > 0" class="mobile-list wallet-list wallet-withdraw-history">
            <article v-for="item in withdrawRecords" :key="item.id" class="wallet-record-card">
              <div class="wallet-record-card__header">
                <strong>{{ item.accountType || '提现记录' }}</strong>
                <strong>¥{{ formatCurrency(item.amount) }}</strong>
              </div>
              <p>{{ item.accountInfo || '账户未记录' }} · {{ item.applicant || '申请人未记录' }}</p>
              <p>{{ item.status || '状态待确认' }} · {{ item.applyTime || '时间未知' }}</p>
            </article>
          </div>

          <div v-else-if="!loading" class="wallet-empty-state">
            <h3>暂无提现记录</h3>
            <p class="mobile-note">
              {{ walletServiceUnavailable ? WALLET_SERVICE_UNAVAILABLE_NOTICE : '提交提现后会在这里展示处理进度。' }}
            </p>
          </div>
        </section>

        <section v-if="activeSegment === 'identity'" class="mobile-card wallet-identity-card">
          <h2 class="mobile-section-title">身份认证</h2>
          <p class="mobile-note">认证模块在 Web 端仍以材料提交为主，iOS 首版先提供状态说明和材料准备清单。</p>

          <div class="wallet-identity-status">
            <span class="wallet-identity-status__badge">待认证</span>
            <p>请先准备收款主体信息、联系人信息和开户地址等资料，再进行正式认证。</p>
          </div>

          <ul class="mobile-bullet-list">
            <li>公司或个人收款主体名称</li>
            <li>联系人姓名与联系电话</li>
            <li>开户地址与收款账户</li>
            <li>若后端开放接口，可在此页继续补齐在线提交</li>
          </ul>

          <section class="wallet-identity-placeholder">
            <h3>当前说明</h3>
            <p class="mobile-note">本次迁移不伪造服务端认证提交接口，先保证移动端可查看状态与操作准备说明。</p>
          </section>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonLabel,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, reactive, ref } from 'vue'
import { getBalanceRecords, getDepositRecords, getWalletStats, getWithdrawRecords, withdrawOrder } from '@/api/wallet'
import { ROUTE_PATHS } from '@/router/guards'
import type { BalanceRecord, DepositRecord, WalletStats, WithdrawRecord } from '@/types/wallet'
import { getRequestErrorStatus, isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const EMPTY_STATS: WalletStats = {
  balance: 0,
  withdrawing: 0,
  pending: 0,
  totalWithdrawn: 0,
}

const WALLET_SERVICE_UNAVAILABLE_NOTICE = '钱包服务暂未开通，当前仅支持查看页面结构。'
const WALLET_WITHDRAW_UNAVAILABLE_NOTICE = '钱包服务暂未开通，暂不支持提现申请。'

type WalletServiceState = 'unknown' | 'available' | 'unavailable'

const loading = ref(false)
const submittingWithdraw = ref(false)
const loadNotice = ref('')
const activeSegment = ref('account')
const accountSegment = ref('balance')
const walletServiceState = ref<WalletServiceState>('unknown')
const hasShownUnavailableNotice = ref(false)
const stats = reactive<WalletStats>({ ...EMPTY_STATS })
const balanceRecords = ref<BalanceRecord[]>([])
const depositRecords = ref<DepositRecord[]>([])
const withdrawRecords = ref<WithdrawRecord[]>([])
const walletServiceUnavailable = computed(() => walletServiceState.value === 'unavailable')

const withdrawForm = reactive({
  amount: '',
  accountType: 'bank',
  accountInfo: '',
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

function formatCurrency(value: number) {
  return Number(value || 0).toFixed(2)
}

function applyEmptyWalletState() {
  Object.assign(stats, EMPTY_STATS)
  balanceRecords.value = []
  depositRecords.value = []
  withdrawRecords.value = []
}

function markWalletServiceAvailable() {
  walletServiceState.value = 'available'
  hasShownUnavailableNotice.value = false
}

function markWalletServiceUnavailable(shouldToast: boolean, toastMessage?: string) {
  applyEmptyWalletState()
  walletServiceState.value = 'unavailable'
  loadNotice.value = WALLET_SERVICE_UNAVAILABLE_NOTICE

  if (shouldToast && !hasShownUnavailableNotice.value) {
    showWarningToast(toastMessage || WALLET_SERVICE_UNAVAILABLE_NOTICE)
    hasShownUnavailableNotice.value = true
  }
}

function isWalletServiceUnavailableError(error: unknown) {
  return getRequestErrorStatus(error) === 404
}

async function loadPage(options: { forceProbe?: boolean } = {}) {
  loading.value = true
  loadNotice.value = ''

  try {
    if (walletServiceState.value === 'unavailable' && !options.forceProbe) {
      applyEmptyWalletState()
      loadNotice.value = WALLET_SERVICE_UNAVAILABLE_NOTICE
      return
    }

    const statsResponse = await getWalletStats({ suppressErrorStatuses: [404] })
    if (!statsResponse.success || !statsResponse.data) {
      throw new Error(statsResponse.message || '钱包统计加载失败')
    }

    markWalletServiceAvailable()
    Object.assign(stats, statsResponse.data)

    const [balanceResult, depositResult, withdrawResult] = await Promise.allSettled([
      getBalanceRecords({ current: 1, size: 20 }, { suppressErrorStatuses: [404] }),
      getDepositRecords({ current: 1, size: 20 }, { suppressErrorStatuses: [404] }),
      getWithdrawRecords({ current: 1, size: 20 }, { suppressErrorStatuses: [404] }),
    ])

    if (balanceResult.status === 'rejected' && isWalletServiceUnavailableError(balanceResult.reason)) {
      markWalletServiceUnavailable(true)
      return
    }

    if (depositResult.status === 'rejected' && isWalletServiceUnavailableError(depositResult.reason)) {
      markWalletServiceUnavailable(true)
      return
    }

    if (withdrawResult.status === 'rejected' && isWalletServiceUnavailableError(withdrawResult.reason)) {
      markWalletServiceUnavailable(true)
      return
    }

    const warnings: string[] = []

    if (balanceResult.status === 'fulfilled') {
      if (balanceResult.value.success && balanceResult.value.data) {
        balanceRecords.value = balanceResult.value.data.records || []
      } else {
        warnings.push(balanceResult.value.message || '余额流水加载失败')
      }
    } else {
      warnings.push(resolveWarningMessage(balanceResult.reason, '余额流水加载失败'))
    }

    if (depositResult.status === 'fulfilled') {
      if (depositResult.value.success && depositResult.value.data) {
        depositRecords.value = depositResult.value.data.records || []
      } else {
        warnings.push(depositResult.value.message || '入账明细加载失败')
      }
    } else {
      warnings.push(resolveWarningMessage(depositResult.reason, '入账明细加载失败'))
    }

    if (withdrawResult.status === 'fulfilled') {
      if (withdrawResult.value.success && withdrawResult.value.data) {
        withdrawRecords.value = withdrawResult.value.data.records || []
      } else {
        warnings.push(withdrawResult.value.message || '提现记录加载失败')
      }
    } else {
      warnings.push(resolveWarningMessage(withdrawResult.reason, '提现记录加载失败'))
    }

    if (warnings.length > 0) {
      loadNotice.value = warnings.join('；')
      showWarningToast(loadNotice.value)
    }
  } catch (error) {
    if (isWalletServiceUnavailableError(error)) {
      markWalletServiceUnavailable(true)
      return
    }

    loadNotice.value = resolveWarningMessage(error, '钱包页面加载失败')
    if (!isHandledRequestError(error)) {
      showWarningToast(loadNotice.value)
    }
  } finally {
    loading.value = false
  }
}

function resetWithdrawForm() {
  withdrawForm.amount = ''
  withdrawForm.accountType = 'bank'
  withdrawForm.accountInfo = ''
}

async function handleSubmitWithdraw() {
  if (walletServiceUnavailable.value) {
    showWarningToast(WALLET_WITHDRAW_UNAVAILABLE_NOTICE)
    return
  }

  const amount = Number(withdrawForm.amount)
  if (!Number.isFinite(amount) || amount <= 0) {
    showWarningToast('请输入有效的提现金额')
    return
  }

  if (!withdrawForm.accountInfo.trim()) {
    showWarningToast('请输入到账账户')
    return
  }

  submittingWithdraw.value = true
  try {
    const response = await withdrawOrder(
      {
        amount,
        accountType: withdrawForm.accountType,
        accountInfo: withdrawForm.accountInfo.trim(),
      },
      { suppressErrorStatuses: [404] },
    )
    if (!response.success) {
      throw new Error(response.message || '提现申请失败')
    }

    showSuccessToast('提现申请已提交')
    resetWithdrawForm()
    await loadPage()
  } catch (error) {
    if (isWalletServiceUnavailableError(error)) {
      markWalletServiceUnavailable(true, WALLET_WITHDRAW_UNAVAILABLE_NOTICE)
      return
    }

    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '提现申请失败'))
    }
  } finally {
    submittingWithdraw.value = false
  }
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadPage({ forceProbe: true })
  } finally {
    event.detail.complete()
  }
}

function handleSegmentChange(event: CustomEvent) {
  activeSegment.value = event.detail.value || 'account'
}

function handleAccountSegmentChange(event: CustomEvent) {
  accountSegment.value = event.detail.value || 'balance'
}

onIonViewWillEnter(async () => {
  await loadPage()
})
</script>

<style scoped>
.wallet-page {
  display: block;
}

.wallet-hero {
  margin-top: 4px;
}

.wallet-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.wallet-summary-card {
  display: grid;
  gap: 12px;
}

.wallet-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.wallet-stat-card,
.wallet-withdraw-overview__item,
.wallet-record-card,
.wallet-identity-placeholder {
  padding: 14px;
  border-radius: 20px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.84);
}

.wallet-stat-card,
.wallet-withdraw-overview__item {
  display: grid;
  gap: 6px;
}

.wallet-stat-card span,
.wallet-withdraw-overview__item span {
  color: var(--app-muted);
  font-size: 13px;
}

.wallet-stat-card strong,
.wallet-withdraw-overview__item strong {
  color: var(--app-heading);
  font-size: 18px;
}

.wallet-summary-card__notice {
  color: var(--ion-color-warning);
}

.wallet-account-card,
.wallet-withdraw-card,
.wallet-identity-card {
  display: grid;
  gap: 14px;
}

.wallet-list {
  margin-top: 4px;
}

.wallet-record-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.wallet-record-card__header strong,
.wallet-record-card p,
.wallet-identity-status p,
.wallet-identity-placeholder h3,
.wallet-identity-placeholder p {
  margin: 0;
}

.wallet-record-card p {
  margin-top: 8px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.wallet-empty-state {
  display: grid;
  gap: 10px;
  justify-items: flex-start;
}

.wallet-empty-state h3,
.wallet-empty-state p {
  margin: 0;
}

.wallet-withdraw-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.wallet-form-grid {
  display: grid;
  gap: 14px;
}

.wallet-form-field {
  display: grid;
  gap: 8px;
}

.wallet-form-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.wallet-form-field--full {
  grid-column: 1 / -1;
}

.wallet-form-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.wallet-withdraw-history {
  margin-top: 2px;
}

.wallet-identity-status {
  padding: 14px;
  border-radius: 20px;
  background: rgba(217, 119, 6, 0.08);
}

.wallet-identity-status__badge {
  display: inline-block;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(217, 119, 6, 0.14);
  color: var(--ion-color-warning);
  font-size: 12px;
  font-weight: 700;
}

.wallet-identity-status p {
  margin-top: 10px;
  color: var(--app-muted);
  line-height: 1.6;
}
</style>
