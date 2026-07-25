<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-title class="app-page-header__title">{{ $t('routes.PublicRoomStatusShare') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page public-share-page">
      <section v-if="share" class="mobile-hero public-share-hero">
        <p class="mobile-note public-share-hero__eyebrow">Public Share</p>
        <h1 class="mobile-title">{{ share.shareTitle }}</h1>
        <p class="mobile-subtitle">{{ todayText }}</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ share.viewType === 'blurred' ? $t('stage5DynamicUi.53') : $t('stage5DynamicUi.43') }}</span>
          <span class="mobile-chip">{{ $t('stage5SourceText.1') }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section v-if="loading" class="mobile-card public-state-card">
          <ion-spinner name="crescent" />
          <p class="mobile-note">{{ $t('stage5SourceText.155') }}</p>
        </section>

        <section v-else-if="errorMessage" class="mobile-card public-state-card public-state-card--error">
          <h2 class="mobile-section-title">{{ $t('stage5SourceText.17') }}</h2>
          <p class="mobile-note">{{ errorMessage }}</p>
          <ion-button @click="loadPage">{{ $t('channel.mappingPriceSettings.actions.retry') }}</ion-button>
        </section>

        <template v-else-if="share">
          <section v-if="visibleStatistics.length > 0" class="mobile-card">
            <div class="mobile-inline-row public-share-header-row">
              <div>
                <h2 class="mobile-section-title">{{ $t('stage5SourceText.105') }}</h2>
                <p class="mobile-note">{{ $t('stage5SourceText.113') }}</p>
              </div>
            </div>

            <div class="public-stat-grid">
              <article v-for="stat in visibleStatistics" :key="stat.key" class="public-stat-card">
                <span class="public-stat-card__label">{{ stat.label }}</span>
                <strong class="public-stat-card__value">{{ stat.value }}</strong>
              </article>
            </div>
          </section>

          <section class="mobile-card public-calendar-card">
            <div class="public-calendar-card__toolbar">
              <div>
                <h2 class="mobile-section-title">{{ $t('stage5SourceText.106') }}</h2>
                <p class="mobile-note">{{ $t('stage5SourceText.153') }}</p>
              </div>
              <input v-model="selectedDate" class="public-date-input" type="date" @change="handleDateChange" />
            </div>

            <div class="public-calendar-wrapper">
              <div class="public-calendar-table">
                <div class="public-calendar-header">
                  <div class="public-sticky-column public-room-column">{{ $t('accommodation.common.room') }}</div>
                  <div v-for="dateItem in dateRange" :key="dateItem" class="public-date-column">
                    {{ formatDateHeader(dateItem) }}
                  </div>
                </div>

                <div v-for="group in roomGroups" :key="group.roomType" class="public-room-group">
                  <div class="public-room-group__title">{{ group.roomType }}</div>

                  <div v-for="room in group.rooms" :key="room.roomId" class="public-room-row">
                    <div class="public-sticky-column public-room-column">{{ room.roomNumber }}</div>
                    <button
                      v-for="dateItem in dateRange"
                      :key="`${room.roomId}-${dateItem}`"
                      class="public-status-cell"
                      :class="resolveStatusCellClass(room, dateItem)"
                      type="button"
                      @click="handleOpenReservation(room, dateItem)"
                    >
                      {{ resolveStatusCellText(room, dateItem) }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </template>
      </div>

      <ion-modal :is-open="detailModalOpen" @didDismiss="handleCloseDetailModal">
        <ion-content class="public-share-modal">
          <div v-if="selectedReservation" class="mobile-stack public-share-modal__body">
            <section class="mobile-card">
              <h2 class="mobile-section-title">{{ $t('stage5SourceText.9') }}</h2>
              <div class="public-detail-grid">
                <div>{{ $t('stage5DynamicUi.143') }}{{ formatGuestName(selectedReservation.guestName) }}</div>
                <div>{{ $t('stage5SourceText.168') }}{{ selectedReservation.channel || $t('roomStatus.common.defaultChannel') }}</div>
                <div>{{ $t('stage5DynamicUi.94') }}{{ formatPublicDate(selectedReservation.checkIn) }}</div>
                <div>{{ $t('stage5DynamicUi.138') }}{{ formatPublicDate(selectedReservation.checkOut) }}</div>
                <div>{{ $t('stage5.dataCenter.detail.metaOrderNumber') }}{{ selectedReservation.orderNumber || '-' }}</div>
                <div>{{ $t('roomStatus.hoverCard.notes') }}{{ selectedReservation.notes || selectedReservation.specialRequests || '-' }}</div>
              </div>
            </section>
            <div class="public-action-row">
              <ion-button expand="block" @click="handleCloseDetailModal">{{ $t('home.section.close') }}</ion-button>
            </div>
          </div>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonHeader, IonModal, IonPage, IonSpinner, IonTitle, IonToolbar } from '@ionic/vue'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import {
  getPublicRoomStatusCalendar,
  getPublicRoomStatusShare,
  getPublicRoomStatusStatistics,
} from '@/api/publicRoomStatusShare'
import type {
  PublicRoomStatusCalendar,
  PublicRoomStatusShare,
  PublicRoomStatusStatistics,
  RoomStatusCalendarRoom,
  RoomStatusReservationInfo,
} from '@/types/publicRoomStatusShare'
import { formatPublicDate } from '@/utils/publicRegistration'
import { getIntlLocale } from '@/utils/formatters'
import {
  buildBusinessDateRange,
  getBusinessDateWeekdayIndex,
  parseBusinessDateParts,
} from '@/utils/storeBusinessDate'

const route = useRoute()
const { locale, t } = useI18n()

const loading = ref(false)
const errorMessage = ref('')
const share = ref<PublicRoomStatusShare | null>(null)
const calendar = ref<PublicRoomStatusCalendar | null>(null)
const statistics = ref<PublicRoomStatusStatistics | null>(null)
const selectedDate = ref('')
const detailModalOpen = ref(false)
const selectedReservation = ref<RoomStatusReservationInfo | null>(null)

const shareToken = computed(() => String(route.params.token || ''))
const todayText = computed(() => {
  const parts = parseBusinessDateParts(selectedDate.value)
  if (!parts) {
    return ''
  }

  const weekday = new Intl.DateTimeFormat(getIntlLocale(locale.value), { weekday: 'long' }).format(
    new Date(parts.year, parts.month - 1, parts.day),
  )
  return t('stage5Final.share.fullDate', { ...parts, weekday })
})
const dateRange = computed(() => {
  if (!isBusinessDateValue(selectedDate.value)) {
    return []
  }

  return buildBusinessDateRange(selectedDate.value, 7)
})
const roomGroups = computed(() => {
  const rooms = calendar.value?.rooms || []
  const groupMap = new Map<string, RoomStatusCalendarRoom[]>()

  rooms.forEach((room) => {
    const currentRooms = groupMap.get(room.roomType) || []
    currentRooms.push(room)
    groupMap.set(room.roomType, currentRooms)
  })

  return Array.from(groupMap.entries()).map(([roomType, groupedRooms]) => ({
    roomType,
    rooms: groupedRooms,
  }))
})
const visibleStatistics = computed(() => {
  if (!share.value || !statistics.value) {
    return []
  }

  const filterItems = parseItems(share.value.filterItems)
  const result: Array<{ key: string; label: string; value: number }> = []

  appendStatistic(result, filterItems, 'arrivals', t('stage5Final.share.todayArrivals'), statistics.value.todayArrivals)
  appendStatistic(result, filterItems, 'departures', t('stage5Final.share.todayDepartures'), statistics.value.todayDepartures)
  appendStatistic(result, filterItems, 'new_orders', t('stage5Final.share.todayNewOrders'), statistics.value.todayNewOrders)
  appendStatistic(result, filterItems, 'available', t('stage5Final.share.todayAvailable'), statistics.value.availableRooms)
  appendStatistic(result, filterItems, 'unassigned', t('stage5Final.share.unassigned'), statistics.value.unassignedOrders)
  appendStatistic(result, filterItems, 'pending', t('stage5Final.share.pending'), statistics.value.pendingOrders)

  return result
})

function parseItems(rawValue?: string | null) {
  if (!rawValue) {
    return []
  }

  return rawValue
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function isBusinessDateValue(value?: string | null) {
  return parseBusinessDateParts(String(value || '')) !== null
}

function applyBackendSelectedDate(value?: string | null) {
  if (!isBusinessDateValue(value)) {
    return
  }

  selectedDate.value = String(value)
}

function appendStatistic(
  target: Array<{ key: string; label: string; value: number }>,
  filterItems: string[],
  key: string,
  label: string,
  value?: number,
) {
  const shouldAppend =
    filterItems.length === 0 ||
    filterItems.includes(key) ||
    filterItems.includes(label) ||
    filterItems.includes(t('stage5Final.share.all')) ||
    filterItems.includes('全部')

  if (!shouldAppend) {
    return
  }

  target.push({
    key,
    label,
    value: Number(value || 0),
  })
}

function formatDateHeader(dateValue: string) {
  const currentDate = parseBusinessDateParts(dateValue)
  const month = currentDate?.month || 1
  const day = currentDate?.day || 1
  const weekday = new Intl.DateTimeFormat(getIntlLocale(locale.value), { weekday: 'short' }).format(
    new Date(currentDate?.year || 1970, month - 1, day),
  )
  return t('stage5Final.share.shortDate', { month, day, weekday })
}

function resolveDailyStatus(room: RoomStatusCalendarRoom, dateValue: string) {
  return room.dailyStatus.find((item) => item.date === dateValue)
}

function resolveStatusCellClass(room: RoomStatusCalendarRoom, dateValue: string) {
  const dailyStatus = resolveDailyStatus(room, dateValue)
  const status = String(dailyStatus?.status || 'AVAILABLE').toLowerCase()
  const classNames = [`public-status-cell--${status}`]

  if (share.value?.viewType === 'blurred' && (status === 'occupied' || status === 'reserved')) {
    classNames.push('public-status-cell--blurred')
  }

  if (dailyStatus?.reservation) {
    classNames.push('public-status-cell--clickable')
  }

  return classNames
}

function resolveStatusCellText(room: RoomStatusCalendarRoom, dateValue: string) {
  const dailyStatus = resolveDailyStatus(room, dateValue)
  const status = String(dailyStatus?.status || 'AVAILABLE')

  if (share.value?.viewType === 'blurred' && (status === 'OCCUPIED' || status === 'RESERVED')) {
    return '***'
  }

  if (status === 'AVAILABLE') {
    return t('stage5Final.share.available')
  }

  if (status === 'OCCUPIED') {
    return dailyStatus?.reservation?.guestName || t('stage5Final.share.occupied')
  }

  if (status === 'RESERVED') {
    return dailyStatus?.reservation?.guestName || t('stage5Final.share.reserved')
  }

  if (status === 'MAINTENANCE') {
    return t('stage5Final.share.maintenance')
  }

  if (status === 'OUT_OF_ORDER') {
    return t('stage5Final.share.outOfOrder')
  }

  return status
}

function formatGuestName(guestName?: string) {
  const safeGuestName = String(guestName || '-')
  if (share.value?.viewType !== 'blurred') {
    return safeGuestName
  }

  return `${safeGuestName.slice(0, 1)}**`
}

function handleOpenReservation(room: RoomStatusCalendarRoom, dateValue: string) {
  const dailyStatus = resolveDailyStatus(room, dateValue)
  if (!dailyStatus?.reservation) {
    return
  }

  selectedReservation.value = dailyStatus.reservation
  detailModalOpen.value = true
}

function handleCloseDetailModal() {
  detailModalOpen.value = false
  selectedReservation.value = null
}

async function loadCalendarAndStatistics(shouldSendSelectedDate = true) {
  const requestStartDate =
    shouldSendSelectedDate && isBusinessDateValue(selectedDate.value) ? selectedDate.value : undefined
  const requestDateRange = requestStartDate ? buildBusinessDateRange(requestStartDate, 7) : []
  const requestEndDate = requestDateRange[requestDateRange.length - 1]

  const [calendarResponse, statisticsResponse] = await Promise.all([
    getPublicRoomStatusCalendar(shareToken.value, requestStartDate, requestEndDate),
    getPublicRoomStatusStatistics(shareToken.value, requestStartDate),
  ])

  if (calendarResponse.success && calendarResponse.data) {
    calendar.value = calendarResponse.data
    if (!requestStartDate) {
      applyBackendSelectedDate(calendarResponse.data.dateRange?.startDate)
    }
  }

  if (statisticsResponse.success && statisticsResponse.data) {
    statistics.value = statisticsResponse.data
    if (!requestStartDate && !isBusinessDateValue(selectedDate.value)) {
      applyBackendSelectedDate(statisticsResponse.data.date)
    }
  }
}

async function loadPage() {
  if (!shareToken.value) {
    errorMessage.value = t('stage5Final.share.missingToken')
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const shareResponse = await getPublicRoomStatusShare(shareToken.value)
    if (!shareResponse.success || !shareResponse.data) {
      errorMessage.value = shareResponse.message || t('stage5Final.share.loadInfoFailed')
      return
    }

    share.value = shareResponse.data
    await loadCalendarAndStatistics(false)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('stage5Final.share.loadPageFailed')
  } finally {
    loading.value = false
  }
}

async function handleDateChange() {
  if (!share.value || !isBusinessDateValue(selectedDate.value)) {
    return
  }

  try {
    await loadCalendarAndStatistics()
  } catch {
    return
  }
}

void loadPage()
</script>

<style scoped>
.public-share-page {
  display: block;
}

.public-share-hero {
  margin-top: 4px;
}

.public-share-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.public-state-card {
  display: grid;
  justify-items: center;
  gap: 12px;
  text-align: center;
}

.public-state-card--error {
  align-items: center;
}

.public-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.public-stat-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(16, 35, 63, 0.04);
}

.public-stat-card__label {
  color: var(--app-muted);
  font-size: 12px;
}

.public-stat-card__value {
  color: var(--app-heading);
  font-size: 28px;
}

.public-calendar-card {
  display: grid;
  gap: 14px;
}

.public-calendar-card__toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.public-date-input {
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid var(--app-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--app-heading);
  font: inherit;
}

.public-calendar-wrapper {
  overflow-x: auto;
}

.public-calendar-table {
  min-width: 780px;
}

.public-calendar-header,
.public-room-row {
  display: grid;
  grid-template-columns: 108px repeat(7, minmax(88px, 1fr));
}

.public-calendar-header {
  position: sticky;
  top: 0;
  z-index: 2;
}

.public-room-group {
  margin-top: 14px;
}

.public-room-group__title {
  margin-bottom: 8px;
  color: var(--app-heading);
  font-size: 15px;
  font-weight: 700;
}

.public-room-column,
.public-date-column,
.public-status-cell {
  min-height: 58px;
  padding: 10px 8px;
  border: 1px solid var(--app-border);
}

.public-sticky-column {
  position: sticky;
  left: 0;
  z-index: 1;
  background: var(--app-surface-strong);
}

.public-room-column,
.public-date-column {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 700;
}

.public-status-cell {
  border-radius: 0;
  background: rgba(255, 255, 255, 0.86);
  color: var(--app-heading);
  font: inherit;
  font-size: 12px;
}

.public-status-cell--clickable {
  cursor: pointer;
}

.public-status-cell--available {
  background: #e9f8ef;
  color: #0d7a43;
}

.public-status-cell--occupied {
  background: #feeceb;
  color: #b42318;
}

.public-status-cell--reserved {
  background: #fff7d6;
  color: #b26a00;
}

.public-status-cell--maintenance,
.public-status-cell--out_of_order {
  background: #eceff3;
  color: #475467;
}

.public-status-cell--blurred {
  filter: blur(1px);
}

.public-share-modal__body {
  padding: 16px;
}

.public-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.public-action-row {
  display: flex;
  gap: 10px;
}

@media (max-width: 768px) {
  .public-stat-grid,
  .public-detail-grid {
    grid-template-columns: 1fr;
  }

  .public-calendar-card__toolbar {
    flex-direction: column;
  }
}
</style>
