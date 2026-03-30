<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.rooms" />
        </ion-buttons>
        <ion-title>房情表</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page room-table-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新房情" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero room-table-page__hero">
        <p class="mobile-note room-table-page__eyebrow">住宿运营</p>
        <h1 class="mobile-title">房情表</h1>
        <p class="mobile-subtitle">移动端拆为单日统计与远期 7 日视图，避免桌面宽表直接平移。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ formatDateWithWeekday(selectedDate) }}</span>
          <span class="mobile-chip">{{ activeView === 'daily' ? '单日房情' : '远期房情' }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card room-table-page__toolbar-card">
          <div class="room-table-page__toolbar-row">
            <ion-button fill="outline" size="small" @click="handleShiftDate(-1)">前一天</ion-button>
            <ion-button fill="outline" size="small" @click="handleGoToday">今天</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftDate(1)">后一天</ion-button>
          </div>

          <label class="room-table-page__date-field">
            <span>业务日</span>
            <input :value="selectedDate" type="date" @input="handleDateInput" />
          </label>

          <ion-segment v-model="activeView" @ionChange="handleViewChange">
            <ion-segment-button value="daily">
              <ion-label>单日</ion-label>
            </ion-segment-button>
            <ion-segment-button value="future">
              <ion-label>远期</ion-label>
            </ion-segment-button>
          </ion-segment>

          <p v-if="errorMessage" class="mobile-note room-table-page__error">{{ errorMessage }}</p>
        </section>

        <section v-if="activeView === 'daily'" class="mobile-card">
          <div class="mobile-inline-row room-table-page__section-header">
            <div>
              <h2 class="mobile-section-title">单日总览</h2>
              <p class="mobile-note">按房型拆解总房、可售、在住、关房、脏净与入住率。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="dailySummaryMetrics.length > 0" class="room-table-page__metric-grid">
            <article v-for="metric in dailySummaryMetrics" :key="metric.key" class="room-table-page__metric-card">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </article>
          </div>

          <div v-if="dailyStatistics.length > 0" class="mobile-list room-table-page__statistics-list">
            <article v-for="item in dailyStatistics" :key="item.roomTypeName" class="room-table-page__statistics-card">
              <div class="room-table-page__statistics-header">
                <div>
                  <strong>{{ item.roomTypeName }}</strong>
                  <p>总房 {{ item.totalRooms }} · 可售 {{ item.availableForSale }} · 可用 {{ item.availableRooms }}</p>
                </div>
                <span>{{ formatPercent(item.expectedOccupancyRate) }}</span>
              </div>

              <div class="room-table-page__statistics-grid">
                <span>在住 {{ item.occupiedRooms }}</span>
                <span>不含预离 {{ item.occupiedWithoutDeparture }}</span>
                <span>预抵 {{ item.scheduledArrival }}</span>
                <span>预离 {{ item.scheduledDeparture }}</span>
                <span>保留房 {{ item.reservedRooms }}</span>
                <span>维修房 {{ item.maintenanceRooms }}</span>
                <span>停用房 {{ item.outOfOrderRooms }}</span>
                <span>链接关房 {{ item.linkedClosedRooms }}</span>
                <span>净房 {{ item.cleanRooms }}</span>
                <span>脏房 {{ item.dirtyRooms }}</span>
                <span>当日取消 {{ item.dailyCancelledRooms }}</span>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前日期暂无房情统计。</p>
        </section>

        <section v-else class="mobile-card">
          <div class="mobile-inline-row room-table-page__section-header">
            <div>
              <h2 class="mobile-section-title">远期 7 日房情</h2>
              <p class="mobile-note">每张房型卡展示可售、占用、不可售三项核心状态。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <p class="mobile-note room-table-page__future-note">
            说明：可售=可售房间数，占用=已营房间数，不可售=保留+维修+链接关房等不可售房间数。
          </p>

          <div v-if="futureRoomTypes.length > 0" class="mobile-list room-table-page__future-list">
            <article v-for="roomType in futureRoomTypes" :key="roomType.roomTypeName" class="room-table-page__future-card">
              <div class="room-table-page__statistics-header">
                <div>
                  <strong>{{ roomType.roomTypeName }}</strong>
                  <p>总房 {{ roomType.totalRooms }}</p>
                </div>
              </div>

              <div class="room-table-page__future-strip">
                <article v-for="dateItem in roomType.dates" :key="`${roomType.roomTypeName}-${dateItem.date}`" class="room-table-page__future-day">
                  <strong>{{ dateItem.date.slice(5) }}</strong>
                  <span>{{ dateItem.dayOfWeek }}</span>
                  <small>可售 {{ dateItem.available }}</small>
                  <small>占用 {{ dateItem.occupied }}</small>
                  <small>不可售 {{ dateItem.unavailable }}</small>
                </article>
              </div>
            </article>
          </div>

          <section v-if="futureStatistics.length > 0" class="room-table-page__future-summary">
            <h3 class="mobile-section-title">区间统计</h3>
            <div class="room-table-page__future-summary-strip">
              <article v-for="item in futureStatistics" :key="item.date" class="room-table-page__future-summary-card">
                <strong>{{ item.date.slice(5) }}</strong>
                <span>有效客房 {{ item.effectiveRooms }}</span>
                <span>入住率 {{ formatPercent(item.expectedOccupancyRate) }}</span>
                <span>客房收入 {{ formatCurrency(item.expectedRoomRevenue) }}</span>
                <span>总房费 {{ formatCurrency(item.expectedTotalRoomFee) }}</span>
                <span>平均收益 {{ formatCurrency(item.averageRoomRevenue) }}</span>
              </article>
            </div>
          </section>

          <p v-if="!loading" class="mobile-note">首期不做明细导出，结构已为后续导出预留。</p>
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
  IonLabel,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSegment,
  IonSegmentButton,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { onIonViewWillEnter } from '@ionic/vue'
import {
  getFutureRoomTableData,
  getRoomTableStatistics,
  type FutureRoomStatisticsDTO,
  type FutureRoomTypeDataDTO,
  type RoomStatisticsDTO,
} from '@/api/roomTable'
import { ROUTE_PATHS } from '@/router/guards'
import { formatCurrency, formatDateWithWeekday, formatPercent, getTodayDate, shiftDate } from '@/utils/accommodation'
import { showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

type RoomTableViewMode = 'daily' | 'future'

interface SummaryMetric {
  key: string
  label: string
  value: string | number
}

const selectedDate = ref(getTodayDate())
const activeView = ref<RoomTableViewMode>('daily')
const loading = ref(false)
const errorMessage = ref('')
const dailyStatistics = ref<RoomStatisticsDTO[]>([])
const futureRoomTypes = ref<FutureRoomTypeDataDTO[]>([])
const futureStatistics = ref<FutureRoomStatisticsDTO[]>([])

const dailySummaryMetrics = computed<SummaryMetric[]>(() => {
  if (dailyStatistics.value.length <= 0) {
    return []
  }

  const total = dailyStatistics.value[dailyStatistics.value.length - 1]
  if (!total) {
    return []
  }

  return [
    { key: 'totalRooms', label: '总房数', value: total.totalRooms },
    { key: 'availableForSale', label: '可售房', value: total.availableForSale },
    { key: 'availableRooms', label: '可用房', value: total.availableRooms },
    { key: 'occupiedRooms', label: '在住房', value: total.occupiedRooms },
    { key: 'cleanRooms', label: '净房', value: total.cleanRooms },
    { key: 'dirtyRooms', label: '脏房', value: total.dirtyRooms },
    { key: 'occupancy', label: '预计入住率', value: formatPercent(total.expectedOccupancyRate) },
  ]
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function loadDailyView() {
  const response = await getRoomTableStatistics(selectedDate.value)
  if (!response.success || !response.data) {
    throw new Error(response.message || '加载单日房情失败')
  }

  const nextStatistics = [...response.data.statistics]
  nextStatistics.push({
    ...response.data.total,
    roomTypeName: response.data.total.roomTypeName || '合计',
  })

  dailyStatistics.value = nextStatistics
}

async function loadFutureView() {
  const response = await getFutureRoomTableData(selectedDate.value, 7)
  if (!response.success || !response.data) {
    throw new Error(response.message || '加载远期房情失败')
  }

  futureRoomTypes.value = response.data.roomTypes
  futureStatistics.value = response.data.statistics
}

async function loadPageData() {
  loading.value = true
  errorMessage.value = ''

  try {
    if (activeView.value === 'daily') {
      await loadDailyView()
      futureRoomTypes.value = []
      futureStatistics.value = []
    } else {
      await loadFutureView()
      dailyStatistics.value = []
    }
  } catch (error) {
    const message = resolveWarningMessage(error, '加载房情失败')
    errorMessage.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  } finally {
    loading.value = false
  }
}

async function handleShiftDate(offsetDays: number) {
  selectedDate.value = shiftDate(selectedDate.value, offsetDays)
  await loadPageData()
}

async function handleGoToday() {
  selectedDate.value = getTodayDate()
  await loadPageData()
}

async function handleViewChange() {
  await loadPageData()
}

async function handleDateInput(event: Event) {
  const target = event.target as HTMLInputElement | null
  if (!target?.value) {
    return
  }

  selectedDate.value = target.value
  await loadPageData()
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
.room-table-page {
  display: block;
}

.room-table-page__hero {
  margin-top: 4px;
}

.room-table-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.room-table-page__toolbar-card {
  display: grid;
  gap: 12px;
}

.room-table-page__toolbar-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.room-table-page__date-field {
  display: grid;
  gap: 8px;
}

.room-table-page__date-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.room-table-page__date-field input {
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-heading);
  font: inherit;
}

.room-table-page__error {
  color: var(--ion-color-danger);
}

.room-table-page__section-header {
  align-items: flex-start;
}

.room-table-page__metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.room-table-page__metric-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.room-table-page__metric-card span {
  color: var(--app-muted);
  font-size: 12px;
}

.room-table-page__metric-card strong {
  color: var(--app-heading);
  font-size: 18px;
}

.room-table-page__statistics-list,
.room-table-page__future-list {
  margin-top: 16px;
}

.room-table-page__statistics-card,
.room-table-page__future-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.room-table-page__statistics-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.room-table-page__statistics-header strong,
.room-table-page__statistics-header p {
  margin: 0;
}

.room-table-page__statistics-header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.room-table-page__statistics-header span {
  color: var(--ion-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.room-table-page__statistics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  margin-top: 14px;
  color: var(--app-muted);
  font-size: 12px;
}

.room-table-page__future-note {
  margin-top: 12px;
}

.room-table-page__future-strip,
.room-table-page__future-summary-strip {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.room-table-page__future-day,
.room-table-page__future-summary-card {
  flex: 0 0 112px;
  display: grid;
  gap: 4px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(16, 35, 63, 0.04);
  color: var(--app-muted);
  font-size: 12px;
}

.room-table-page__future-day strong,
.room-table-page__future-summary-card strong {
  color: var(--app-heading);
  font-size: 14px;
}

.room-table-page__future-summary {
  margin-top: 18px;
}
</style>
