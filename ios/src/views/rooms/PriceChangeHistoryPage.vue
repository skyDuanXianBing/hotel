<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.roomsPricing" />
        </ion-buttons>
        <ion-title>改价记录</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page price-history-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新改价记录" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero price-history-page__hero">
        <p class="mobile-note price-history-page__eyebrow">住宿运营</p>
        <h1 class="mobile-title">改价记录</h1>
        <p class="mobile-subtitle">默认展示最近 7 天变更，筛选区按移动端收纳为基础条件与进阶条件。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">最近 {{ records.length }} 条</span>
          <span class="mobile-chip">总数 {{ total }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card price-history-page__filter-card">
          <div class="mobile-inline-row price-history-page__section-header">
            <div>
              <h2 class="mobile-section-title">筛选条件</h2>
              <p class="mobile-note">常用条件常显，进阶条件按需展开。</p>
            </div>
            <ion-button fill="clear" size="small" @click="handleToggleAdvancedFilters">
              {{ showAdvancedFilters ? '收起' : '展开' }}
            </ion-button>
          </div>

          <div class="price-history-page__filter-grid">
            <label class="price-history-page__field">
              <span>操作开始</span>
              <input v-model="filters.operateDateStart" type="date" />
            </label>
            <label class="price-history-page__field">
              <span>操作结束</span>
              <input v-model="filters.operateDateEnd" type="date" />
            </label>
            <label class="price-history-page__field">
              <span>房型</span>
              <select v-model="filters.roomTypeIdText">
                <option value="">全部房型</option>
                <option v-for="roomType in roomTypes" :key="roomType.id" :value="String(roomType.id)">
                  {{ roomType.name }}
                </option>
              </select>
            </label>
            <label class="price-history-page__field">
              <span>价格计划</span>
              <select v-model="filters.pricePlanIdText">
                <option value="">全部计划</option>
                <option v-for="plan in pricePlans" :key="plan.id" :value="String(plan.id)">
                  {{ plan.name }}
                </option>
              </select>
            </label>
          </div>

          <div v-if="showAdvancedFilters" class="price-history-page__filter-grid">
            <label class="price-history-page__field">
              <span>价格开始</span>
              <input v-model="filters.priceDateStart" type="date" />
            </label>
            <label class="price-history-page__field">
              <span>价格结束</span>
              <input v-model="filters.priceDateEnd" type="date" />
            </label>
            <label class="price-history-page__field price-history-page__field--full">
              <span>操作人</span>
              <input v-model="filters.operator" placeholder="输入操作人昵称或邮箱" type="text" />
            </label>
          </div>

          <div class="price-history-page__actions">
            <ion-button fill="outline" @click="handleResetFilters">重置</ion-button>
            <ion-button @click="handleSearch">查询</ion-button>
          </div>

          <p v-if="errorMessage" class="mobile-note price-history-page__error">{{ errorMessage }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row price-history-page__section-header">
            <div>
              <h2 class="mobile-section-title">变更列表</h2>
              <p class="mobile-note">卡片展示房型、计划、价格日期、周几、操作人与变更值。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="records.length > 0" class="mobile-list price-history-page__list">
            <article v-for="record in records" :key="record.id" class="price-history-page__record-card">
              <div class="price-history-page__record-header">
                <div>
                  <strong>{{ record.roomTypeName }}</strong>
                  <p>{{ record.pricePlanName }}</p>
                </div>
                <span>{{ record.changeType }}</span>
              </div>

              <div class="price-history-page__record-grid">
                <span>价格日期 {{ record.priceDate }}</span>
                <span>适用周几 {{ record.applyDays }}</span>
                <span>调整结果 {{ formatCurrency(record.changeValue) }}</span>
                <span>原值 {{ formatCurrency(record.previousValue) }}</span>
                <span>操作人 {{ record.operator || '系统' }}</span>
                <span>操作时间 {{ formatDateTime(record.operateTime) }}</span>
              </div>

              <p v-if="record.notes" class="mobile-note price-history-page__record-note">备注：{{ record.notes }}</p>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前筛选条件下暂无改价记录。</p>

          <div v-if="hasMore" class="price-history-page__load-more">
            <ion-button fill="outline" :disabled="loadingMore" @click="handleLoadMore">
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </ion-button>
          </div>
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
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { onIonViewWillEnter } from '@ionic/vue'
import { getAllPricePlans, type PricePlanDTO } from '@/api/pricePlan'
import {
  getPriceChangeHistory,
  type PriceChangeHistoryDTO,
  type PriceChangeHistoryQueryParams,
} from '@/api/roomPrice'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import { formatCurrency, formatDateTime, getTodayDate, shiftDate } from '@/utils/accommodation'
import { showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

interface FilterState {
  operateDateStart: string
  operateDateEnd: string
  priceDateStart: string
  priceDateEnd: string
  roomTypeIdText: string
  pricePlanIdText: string
  operator: string
}

const DEFAULT_PAGE_SIZE = 20

const userStore = useUserStore()

const roomTypes = ref<RoomTypeDTO[]>([])
const pricePlans = ref<PricePlanDTO[]>([])
const records = ref<PriceChangeHistoryDTO[]>([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')
const showAdvancedFilters = ref(false)
const filters = ref<FilterState>(createDefaultFilters())

const hasMore = computed(() => {
  return records.value.length < total.value
})

function createDefaultFilters(): FilterState {
  return {
    operateDateStart: shiftDate(getTodayDate(), -7),
    operateDateEnd: getTodayDate(),
    priceDateStart: '',
    priceDateEnd: '',
    roomTypeIdText: '',
    pricePlanIdText: '',
    operator: '',
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function buildQueryParams(): PriceChangeHistoryQueryParams {
  const roomTypeId = filters.value.roomTypeIdText ? Number(filters.value.roomTypeIdText) : undefined
  const params: PriceChangeHistoryQueryParams = {
    operateDateStart: filters.value.operateDateStart || undefined,
    operateDateEnd: filters.value.operateDateEnd || undefined,
    priceDateStart: filters.value.priceDateStart || undefined,
    priceDateEnd: filters.value.priceDateEnd || undefined,
    roomTypeId,
    pricePlanId: filters.value.pricePlanIdText || undefined,
    operator: filters.value.operator.trim() || undefined,
    pageNum: pageNum.value,
    pageSize: DEFAULT_PAGE_SIZE,
  }
  return params
}

async function loadOptions() {
  const userId = userStore.currentUser?.id
  const roomTypeResponse = await getAllRoomTypes()

  if (!roomTypeResponse.success || !roomTypeResponse.data) {
    throw new Error(roomTypeResponse.message || '加载房型失败')
  }

  roomTypes.value = roomTypeResponse.data

  if (!userId) {
    pricePlans.value = []
    return
  }

  const pricePlanResponse = await getAllPricePlans(userId)
  if (pricePlanResponse.success && pricePlanResponse.data) {
    pricePlans.value = pricePlanResponse.data
  }
}

async function loadRecords(reset = true) {
  if (reset) {
    loading.value = true
    pageNum.value = 1
  } else {
    loadingMore.value = true
  }

  errorMessage.value = ''

  try {
    const response = await getPriceChangeHistory(buildQueryParams())
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载改价记录失败')
    }

    total.value = response.data.total

    if (reset) {
      records.value = response.data.records
    } else {
      records.value = [...records.value, ...response.data.records]
    }
  } catch (error) {
    const message = resolveWarningMessage(error, '加载改价记录失败')
    errorMessage.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadPageData() {
  loading.value = true
  errorMessage.value = ''

  try {
    await loadOptions()
    await loadRecords(true)
  } catch (error) {
    const message = resolveWarningMessage(error, '加载改价记录失败')
    errorMessage.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
    loading.value = false
  }
}

function handleToggleAdvancedFilters() {
  showAdvancedFilters.value = !showAdvancedFilters.value
}

async function handleSearch() {
  await loadRecords(true)
}

async function handleLoadMore() {
  if (!hasMore.value) {
    return
  }
  pageNum.value += 1
  await loadRecords(false)
}

function handleResetFilters() {
  filters.value = createDefaultFilters()
}

async function handleRefresh(event: CustomEvent) {
  await loadPageData()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.price-history-page {
  display: block;
}

.price-history-page__hero {
  margin-top: 4px;
}

.price-history-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.price-history-page__filter-card {
  display: grid;
  gap: 14px;
}

.price-history-page__section-header {
  align-items: flex-start;
}

.price-history-page__filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.price-history-page__field {
  display: grid;
  gap: 8px;
}

.price-history-page__field--full {
  grid-column: 1 / -1;
}

.price-history-page__field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.price-history-page__field input,
.price-history-page__field select {
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-heading);
  font: inherit;
}

.price-history-page__actions,
.price-history-page__load-more {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.price-history-page__error {
  color: var(--ion-color-danger);
}

.price-history-page__list {
  margin-top: 16px;
}

.price-history-page__record-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.price-history-page__record-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.price-history-page__record-header strong,
.price-history-page__record-header p {
  margin: 0;
}

.price-history-page__record-header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.price-history-page__record-header span {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.price-history-page__record-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  margin-top: 14px;
  color: var(--app-muted);
  font-size: 12px;
}

.price-history-page__record-note {
  margin-top: 12px;
}

@media (max-width: 520px) {
  .price-history-page__filter-grid,
  .price-history-page__record-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
