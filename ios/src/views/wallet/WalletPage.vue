<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.Wallet') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page wallet-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('iosStage5.wallet.pullToRefresh')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero wallet-hero">
        <p class="mobile-note wallet-hero__eyebrow">{{ $t('iosStage5.wallet.eyebrow') }}</p>
        <h1 class="mobile-title">{{ $t('routes.Wallet') }}</h1>
        <p class="mobile-subtitle">{{ $t('iosStage5.wallet.subtitle') }}</p>
        <div class="mobile-chip-row">
        <span class="mobile-chip">{{ $t('iosStage5.wallet.balance') }} {{ formatWalletMoney(stats.balance) }}</span>
        <span class="mobile-chip">{{ $t('iosStage5.wallet.withdrawing') }} {{ formatWalletMoney(stats.withdrawing) }}</span>
        <span class="mobile-chip">{{ $t('iosStage5.wallet.pendingDeposit') }} {{ formatWalletMoney(stats.pending) }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <ion-segment :value="activeSegment" @ionChange="handleSegmentChange">
            <ion-segment-button value="account">
              <ion-label>{{ $t('iosStage5.wallet.account') }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="withdraw">
              <ion-label>{{ $t('iosStage5.wallet.withdraw') }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="identity">
              <ion-label>{{ $t('iosStage5.wallet.verification') }}</ion-label>
            </ion-segment-button>
          </ion-segment>
        </section>

        <section class="mobile-card wallet-summary-card">
          <div class="wallet-summary-grid">
            <article class="wallet-stat-card">
              <span>{{ $t('iosStage5.wallet.availableBalance') }}</span>
              <strong>{{ formatWalletMoney(stats.balance) }}</strong>
            </article>
            <article class="wallet-stat-card">
              <span>{{ $t('iosStage5.wallet.withdrawing') }}</span>
              <strong>{{ formatWalletMoney(stats.withdrawing) }}</strong>
            </article>
            <article class="wallet-stat-card">
              <span>{{ $t('iosStage5.wallet.pendingDeposit') }}</span>
              <strong>{{ formatWalletMoney(stats.pending) }}</strong>
            </article>
            <article class="wallet-stat-card">
              <span>{{ $t('iosStage5.wallet.totalWithdrawn') }}</span>
              <strong>{{ formatWalletMoney(stats.totalWithdrawn) }}</strong>
            </article>
          </div>

          <p v-if="loadNotice" class="mobile-note wallet-summary-card__notice">{{ loadNotice }}</p>
        </section>

        <section v-if="activeSegment === 'account'" class="mobile-card wallet-account-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">{{ $t('iosStage5.wallet.accountActivity') }}</h2>
              <p class="mobile-note">{{ $t('iosStage5.wallet.activityDescription') }}</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <ion-segment :value="accountSegment" @ionChange="handleAccountSegmentChange">
            <ion-segment-button value="balance">
              <ion-label>{{ $t('iosStage5.wallet.balanceActivity') }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="deposit">
              <ion-label>{{ $t('iosStage5.wallet.depositDetails') }}</ion-label>
            </ion-segment-button>
          </ion-segment>

          <div v-if="accountSegment === 'balance' && balanceRecords.length > 0" class="mobile-list wallet-list">
            <article v-for="item in balanceRecords" :key="item.id" class="wallet-record-card">
              <div class="wallet-record-card__header">
                <strong>{{ item.type || $t('iosStage5.wallet.balanceChange') }}</strong>
                    <strong>{{ formatWalletMoney(item.amount) }}</strong>
              </div>
              <p>{{ item.channel || $t('iosStage5.wallet.unknownChannel') }} · {{ item.paymentMethod || $t('iosStage5.wallet.paymentPending') }}</p>
              <p>{{ item.orderNo || item.transactionNo || $t('iosStage5.wallet.noOrderNumber') }} · {{ item.time || $t('iosStage5.wallet.unknownTime') }}</p>
            </article>
          </div>

          <div v-if="accountSegment === 'deposit' && depositRecords.length > 0" class="mobile-list wallet-list">
            <article v-for="item in depositRecords" :key="item.id" class="wallet-record-card">
              <div class="wallet-record-card__header">
                <strong>{{ item.type || $t('iosStage5.wallet.depositDetails') }}</strong>
                    <strong>{{ formatWalletMoney(item.settlementAmount) }}</strong>
              </div>
              <p>{{ item.channel || $t('iosStage5.wallet.unknownChannel') }} · {{ item.orderType || $t('iosStage5.wallet.orderTypePending') }}</p>
              <p>{{ item.orderNo || $t('iosStage5.wallet.noOrderNumber') }} · {{ item.occurTime || $t('iosStage5.wallet.unknownTime') }}</p>
            </article>
          </div>

          <div
            v-if="accountSegment === 'balance' && balanceRecords.length === 0 && !loading"
            class="wallet-empty-state"
          >
            <h3>{{ $t('iosStage5.wallet.noBalanceActivity') }}</h3>
            <p class="mobile-note">
              {{ walletServiceUnavailable ? walletServiceUnavailableNotice : $t('iosStage5.wallet.noBalanceDescription') }}
            </p>
          </div>

          <div
            v-if="accountSegment === 'deposit' && depositRecords.length === 0 && !loading"
            class="wallet-empty-state"
          >
            <h3>{{ $t('iosStage5.wallet.noDepositDetails') }}</h3>
            <p class="mobile-note">
              {{ walletServiceUnavailable ? walletServiceUnavailableNotice : $t('iosStage5.wallet.noDepositDescription') }}
            </p>
          </div>
        </section>

        <section v-if="activeSegment === 'withdraw'" class="mobile-card wallet-withdraw-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">{{ $t('iosStage5.wallet.withdrawRequest') }}</h2>
              <p class="mobile-note">{{ $t('iosStage5.wallet.withdrawDescription') }}</p>
            </div>
            <ion-spinner v-if="submittingWithdraw" name="crescent" />
          </div>

          <div class="wallet-withdraw-overview">
            <article class="wallet-withdraw-overview__item">
              <span>{{ $t('iosStage5.wallet.currentlyAvailable') }}</span>
              <strong>{{ formatWalletMoney(stats.balance) }}</strong>
            </article>
            <article class="wallet-withdraw-overview__item">
              <span>{{ $t('iosStage5.wallet.totalWithdrawn') }}</span>
              <strong>{{ formatWalletMoney(stats.totalWithdrawn) }}</strong>
            </article>
          </div>

          <div class="wallet-form-grid">
            <label class="wallet-form-field">
              <span>{{ $t('iosStage5.wallet.withdrawAmount') }}</span>
              <ion-input v-model="withdrawForm.amount" fill="outline" inputmode="decimal" :placeholder="$t('iosStage5.wallet.withdrawAmountPlaceholder')" />
            </label>

            <label class="wallet-form-field">
              <span>{{ $t('iosStage5.wallet.accountType') }}</span>
              <ion-select v-model="withdrawForm.accountType" fill="outline" interface="action-sheet">
                <ion-select-option value="bank">{{ $t('roomStatus.payment.methodOptions.bankCard') }}</ion-select-option>
                <ion-select-option value="alipay">{{ $t('accommodation.paymentMethods.alipay') }}</ion-select-option>
                <ion-select-option value="wechat">{{ $t('accommodation.paymentMethods.wechat') }}</ion-select-option>
              </ion-select>
            </label>

            <label class="wallet-form-field wallet-form-field--full">
              <span>{{ $t('iosStage5.wallet.receivingAccount') }}</span>
              <ion-input v-model="withdrawForm.accountInfo" fill="outline" :placeholder="$t('iosStage5.wallet.receivingAccountPlaceholder')" />
            </label>
          </div>

          <div class="wallet-form-actions">
            <ion-button fill="outline" @click="resetWithdrawForm">{{ $t('accommodation.common.reset') }}</ion-button>
            <ion-button :disabled="submittingWithdraw || walletServiceUnavailable" @click="handleSubmitWithdraw">
              {{ submittingWithdraw ? $t('iosStage5.cleaning.submitting') : $t('iosStage5.wallet.submitRequest') }}
            </ion-button>
          </div>

          <p v-if="walletServiceUnavailable" class="mobile-note">{{ walletWithdrawUnavailableNotice }}</p>

          <div v-if="withdrawRecords.length > 0" class="mobile-list wallet-list wallet-withdraw-history">
            <article v-for="item in withdrawRecords" :key="item.id" class="wallet-record-card">
              <div class="wallet-record-card__header">
                <strong>{{ item.accountType || $t('iosStage5.wallet.withdrawRecord') }}</strong>
                    <strong>{{ formatWalletMoney(item.amount) }}</strong>
              </div>
              <p>{{ item.accountInfo || $t('iosStage5.wallet.accountNotRecorded') }} · {{ item.applicant || $t('iosStage5.wallet.applicantNotRecorded') }}</p>
              <p>{{ item.status || $t('iosStage5.wallet.statusPending') }} · {{ item.applyTime || $t('iosStage5.wallet.unknownTime') }}</p>
            </article>
          </div>

          <div v-else-if="!loading" class="wallet-empty-state">
            <h3>{{ $t('iosStage5.wallet.noWithdrawRecords') }}</h3>
            <p class="mobile-note">
              {{ walletServiceUnavailable ? walletServiceUnavailableNotice : $t('iosStage5.wallet.noWithdrawDescription') }}
            </p>
          </div>
        </section>

        <section v-if="activeSegment === 'identity'" class="mobile-card wallet-identity-card">
          <h2 class="mobile-section-title">{{ $t('iosStage5.wallet.identityVerification') }}</h2>
          <p class="mobile-note">{{ $t('iosStage5.wallet.identityDescription') }}</p>

          <div class="wallet-identity-status">
            <span class="wallet-identity-status__badge">{{ $t('iosStage5.wallet.verificationPending') }}</span>
            <p>{{ $t('iosStage5.wallet.prepareMaterials') }}</p>
          </div>

          <ul class="mobile-bullet-list">
            <li>{{ $t('iosStage5.wallet.recipientName') }}</li>
            <li>{{ $t('iosStage5.wallet.contactDetails') }}</li>
            <li>{{ $t('iosStage5.wallet.bankAndAccount') }}</li>
            <li>{{ $t('iosStage5.wallet.onlineSubmissionNote') }}</li>
          </ul>

          <section class="wallet-identity-placeholder">
            <h3>{{ $t('iosStage5.wallet.currentNotice') }}</h3>
            <p class="mobile-note">{{ $t('iosStage5.wallet.noFakeApi') }}</p>
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
import { useI18n } from 'vue-i18n'
import { getBalanceRecords, getDepositRecords, getWalletStats, getWithdrawRecords, withdrawOrder } from '@/api/wallet'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import type { BalanceRecord, DepositRecord, WalletStats, WithdrawRecord } from '@/types/wallet'
import { formatMoney } from '@/utils/formatters'
import { getRequestErrorStatus, isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const EMPTY_STATS: WalletStats = {
  balance: 0,
  withdrawing: 0,
  pending: 0,
  totalWithdrawn: 0,
}

const { t } = useI18n()
const storeStore = useStoreStore()
const walletServiceUnavailableNotice = computed(() => t('iosStage5.wallet.serviceUnavailable'))
const walletWithdrawUnavailableNotice = computed(() => t('iosStage5.wallet.withdrawUnavailable'))

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

function formatWalletMoney(value: number) {
  return formatMoney(
    value,
    storeStore.currentStore?.currency || 'CNY',
    {},
    { country: storeStore.currentStore?.country },
  )
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
  loadNotice.value = walletServiceUnavailableNotice.value

  if (shouldToast && !hasShownUnavailableNotice.value) {
    showWarningToast(toastMessage || walletServiceUnavailableNotice.value)
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
      loadNotice.value = walletServiceUnavailableNotice.value
      return
    }

    const statsResponse = await getWalletStats({ suppressErrorStatuses: [404] })
    if (!statsResponse.success || !statsResponse.data) {
      throw new Error(statsResponse.message || t('iosStage5.wallet.statsLoadFailed'))
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
        warnings.push(balanceResult.value.message || t('iosStage5.wallet.balanceLoadFailed'))
      }
    } else {
      warnings.push(resolveWarningMessage(balanceResult.reason, t('iosStage5.wallet.balanceLoadFailed')))
    }

    if (depositResult.status === 'fulfilled') {
      if (depositResult.value.success && depositResult.value.data) {
        depositRecords.value = depositResult.value.data.records || []
      } else {
        warnings.push(depositResult.value.message || t('iosStage5.wallet.depositLoadFailed'))
      }
    } else {
      warnings.push(resolveWarningMessage(depositResult.reason, t('iosStage5.wallet.depositLoadFailed')))
    }

    if (withdrawResult.status === 'fulfilled') {
      if (withdrawResult.value.success && withdrawResult.value.data) {
        withdrawRecords.value = withdrawResult.value.data.records || []
      } else {
        warnings.push(withdrawResult.value.message || t('iosStage5.wallet.withdrawLoadFailed'))
      }
    } else {
      warnings.push(resolveWarningMessage(withdrawResult.reason, t('iosStage5.wallet.withdrawLoadFailed')))
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

    loadNotice.value = resolveWarningMessage(error, t('iosStage5.wallet.pageLoadFailed'))
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
    showWarningToast(walletWithdrawUnavailableNotice.value)
    return
  }

  const amount = Number(withdrawForm.amount)
  if (!Number.isFinite(amount) || amount <= 0) {
    showWarningToast(t('iosStage5.wallet.invalidAmount'))
    return
  }

  if (!withdrawForm.accountInfo.trim()) {
    showWarningToast(t('iosStage5.wallet.accountRequired'))
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
      throw new Error(response.message || t('iosStage5.wallet.requestFailed'))
    }

    showSuccessToast(t('iosStage5.wallet.requestSubmitted'))
    resetWithdrawForm()
    await loadPage()
  } catch (error) {
    if (isWalletServiceUnavailableError(error)) {
      markWalletServiceUnavailable(true, walletWithdrawUnavailableNotice.value)
      return
    }

    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('iosStage5.wallet.requestFailed')))
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
