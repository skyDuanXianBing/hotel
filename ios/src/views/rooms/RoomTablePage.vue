<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.rooms" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.RoomsRoomTable') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page room-table-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('stage5UiAttributes.9')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero room-table-page__hero">
        <p class="mobile-note room-table-page__eyebrow">{{ $t('stage5VisibleText.121') }}</p>
        <h1 class="mobile-title">{{ $t('routes.RoomsRoomTable') }}</h1>
        <p class="mobile-subtitle">{{ $t('stage5VisibleText.218') }}</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ formatDateWithWeekday(selectedDate) }}</span>
          <span class="mobile-chip">{{ activeView === 'daily' ? $t('stage5DynamicUi.25') : $t('stage5DynamicUi.75') }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card room-table-page__toolbar-card">
          <div class="room-table-page__toolbar-row">
            <ion-button fill="outline" size="small" @click="handleShiftDate(-1)">{{ $t('accommodation.roomPrice.previousDay') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleGoToday">{{ $t('accommodation.common.today') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftDate(1)">{{ $t('accommodation.roomPrice.nextDay') }}</ion-button>
          </div>

          <label class="room-table-page__date-field">
            <span>{{ $t('stage5VisibleText.111') }}</span>
            <input :value="selectedDate" type="date" @input="handleDateInput" />
          </label>

          <ion-segment v-model="activeView" @ionChange="handleViewChange">
            <ion-segment-button value="daily">
              <ion-label>{{ $t('stage5VisibleText.144') }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="future">
              <ion-label>{{ $t('stage5VisibleText.234') }}</ion-label>
            </ion-segment-button>
          </ion-segment>

          <p v-if="errorMessage" class="mobile-note room-table-page__error">{{ errorMessage }}</p>
        </section>

        <section v-if="activeView === 'daily'" class="mobile-card">
          <div class="mobile-inline-row room-table-page__section-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('stage5VisibleText.145') }}</h2>
              <p class="mobile-note">{{ $t('stage5VisibleText.181') }}</p>
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
                  <p>{{ $t('stage5DynamicUi.113') }} {{ item.totalRooms }} {{ $t('stage5DynamicUi.82') }} {{ item.availableForSale }} {{ $t('stage5DynamicUi.83') }} {{ item.availableRooms }}</p>
                </div>
                <span>{{ formatPercent(item.expectedOccupancyRate) }}</span>
              </div>

              <div class="room-table-page__statistics-grid">
                <span>{{ $t('accommodation.roomTable.columns.occupiedRooms') }} {{ item.occupiedRooms }}</span>
                <span>{{ $t('stage5DynamicUi.86') }} {{ item.occupiedWithoutDeparture }}</span>
                <span>{{ $t('accommodation.roomTable.columns.scheduledArrival') }} {{ item.scheduledArrival }}</span>
                <span>{{ $t('accommodation.roomTable.columns.scheduledDeparture') }} {{ item.scheduledDeparture }}</span>
                <span>{{ $t('roomStatus.closeRoom.type.retain') }} {{ item.reservedRooms }}</span>
                <span>{{ $t('roomStatus.closeRoom.type.maintenance') }} {{ item.maintenanceRooms }}</span>
                <span>{{ $t('roomStatus.closeRoom.type.stop') }} {{ item.outOfOrderRooms }}</span>
                <span>{{ $t('stage5DynamicUi.139') }} {{ item.linkedClosedRooms }}</span>
                <span>{{ $t('accommodation.roomTable.columns.cleanRooms') }} {{ item.cleanRooms }}</span>
                <span>{{ $t('accommodation.roomTable.columns.dirtyRooms') }} {{ item.dirtyRooms }}</span>
                <span>{{ $t('stage5DynamicUi.111') }} {{ item.dailyCancelledRooms }}</span>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">{{ $t('stage5VisibleText.164') }}</p>
        </section>

        <section v-else class="mobile-card">
          <div class="mobile-inline-row room-table-page__section-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('stage5VisibleText.235') }}</h2>
              <p class="mobile-note">{{ $t('stage5VisibleText.208') }}</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <p class="mobile-note room-table-page__future-note">
            {{ $t('stage5VisibleText.226') }}
          </p>

          <div v-if="futureRoomTypes.length > 0" class="mobile-list room-table-page__future-list">
            <article v-for="roomType in futureRoomTypes" :key="roomType.roomTypeName" class="room-table-page__future-card">
              <div class="room-table-page__statistics-header">
                <div>
                  <strong>{{ roomType.roomTypeName }}</strong>
                  <p>{{ $t('stage5DynamicUi.113') }} {{ roomType.totalRooms }}</p>
                </div>
              </div>

              <div class="room-table-page__future-strip">
                <article v-for="dateItem in roomType.dates" :key="`${roomType.roomTypeName}-${dateItem.date}`" class="room-table-page__future-day">
                  <strong>{{ dateItem.date.slice(5) }}</strong>
                  <span>{{ dateItem.dayOfWeek }}</span>
                  <small>{{ $t('accommodation.roomTable.future.available') }} {{ dateItem.available }}</small>
                  <small>{{ $t('accommodation.roomTable.future.occupied') }} {{ dateItem.occupied }}</small>
                  <small>{{ $t('accommodation.roomTable.future.unavailable') }} {{ dateItem.unavailable }}</small>
                </article>
              </div>
            </article>
          </div>

          <section v-if="futureStatistics.length > 0" class="room-table-page__future-summary">
            <h3 class="mobile-section-title">{{ $t('stage5VisibleText.143') }}</h3>
            <div class="room-table-page__future-summary-strip">
              <article v-for="item in futureStatistics" :key="item.date" class="room-table-page__future-summary-card">
                <strong>{{ item.date.slice(5) }}</strong>
                <span>{{ $t('stage5DynamicUi.124') }} {{ item.effectiveRooms }}</span>
                <span>{{ $t('stage5.statistics.common.occupancyRate') }} {{ formatPercent(item.expectedOccupancyRate) }}</span>
                <span>{{ $t('stage5DynamicUi.107') }} {{ formatMoney(item.expectedRoomRevenue) }}</span>
                <span>{{ $t('stage5.statistics.accommodation.totalRoomFee') }} {{ formatMoney(item.expectedTotalRoomFee) }}</span>
                <span>{{ $t('stage5DynamicUi.109') }} {{ formatMoney(item.averageRoomRevenue) }}</span>
              </article>
            </div>
          </section>

          <p v-if="!loading" class="mobile-note">{{ $t('stage5VisibleText.242') }}</p>
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
import { useI18n } from 'vue-i18n'
import { onIonViewWillEnter } from '@ionic/vue'
import {
  getFutureRoomTableData,
  getRoomTableStatistics,
  type FutureRoomStatisticsDTO,
  type FutureRoomTypeDataDTO,
  type RoomStatisticsDTO,
} from '@/api/roomTable'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { formatDateWithWeekday, formatPercent, getTodayDate, shiftDate } from '@/utils/accommodation'
import { formatMoney as formatBusinessMoney } from '@/utils/formatters'
import { showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

type RoomTableViewMode = 'daily' | 'future'

interface SummaryMetric {
  key: string
  label: string
  value: string | number
}

const selectedDate = ref(getTodayDate())
const storeStore = useStoreStore()
const { t } = useI18n()
const activeView = ref<RoomTableViewMode>('daily')
const loading = ref(false)
const errorMessage = ref('')
const dailyStatistics = ref<RoomStatisticsDTO[]>([])
const futureRoomTypes = ref<FutureRoomTypeDataDTO[]>([])
const futureStatistics = ref<FutureRoomStatisticsDTO[]>([])
const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))

const dailySummaryMetrics = computed<SummaryMetric[]>(() => {
  if (dailyStatistics.value.length <= 0) {
    return []
  }

  const total = dailyStatistics.value[dailyStatistics.value.length - 1]
  if (!total) {
    return []
  }

  return [
    { key: 'totalRooms', label: t('roomStatus.roomTable.metrics.totalRooms'), value: total.totalRooms },
    {
      key: 'availableForSale',
      label: t('roomStatus.roomTable.metrics.availableForSale'),
      value: total.availableForSale,
    },
    {
      key: 'availableRooms',
      label: t('roomStatus.roomTable.metrics.availableRooms'),
      value: total.availableRooms,
    },
    {
      key: 'occupiedRooms',
      label: t('roomStatus.roomTable.metrics.occupiedRooms'),
      value: total.occupiedRooms,
    },
    { key: 'cleanRooms', label: t('roomStatus.roomTable.metrics.cleanRooms'), value: total.cleanRooms },
    { key: 'dirtyRooms', label: t('roomStatus.roomTable.metrics.dirtyRooms'), value: total.dirtyRooms },
    {
      key: 'occupancy',
      label: t('roomStatus.roomTable.metrics.occupancy'),
      value: formatPercent(total.expectedOccupancyRate),
    },
  ]
})

function formatMoney(value?: number | null) {
  return formatBusinessMoney(Number(value || 0), currentCurrency.value, {
    maximumFractionDigits: 0,
  }, currentMoneyContext.value)
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function loadDailyView() {
  const response = await getRoomTableStatistics(selectedDate.value)
  if (!response.success || !response.data) {
    throw new Error(response.message || t('stage5Pattern.loadFailed'))
  }

  const nextStatistics = [...response.data.statistics]
  nextStatistics.push({
    ...response.data.total,
    roomTypeName: response.data.total.roomTypeName || t('roomStatus.roomTable.total'),
  })

  dailyStatistics.value = nextStatistics
}

async function loadFutureView() {
  const response = await getFutureRoomTableData(selectedDate.value, 7)
  if (!response.success || !response.data) {
    throw new Error(response.message || t('stage5Pattern.loadFailed'))
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
    const message = resolveWarningMessage(error, t('stage5Pattern.loadFailed'))
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
