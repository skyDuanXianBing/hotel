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

    <ion-content ref="pageContent" fullscreen class="mobile-page room-table-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('stage5UiAttributes.9')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero room-table-page__hero">
        <p class="mobile-note room-table-page__eyebrow">{{ $t('stage5VisibleText.121') }}</p>
        <h1 class="mobile-title">{{ $t('routes.RoomsRoomTable') }}</h1>
        <p class="mobile-subtitle">{{ $t('stage5VisibleText.218') }}</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ currentDateChipText }}</span>
          <span class="mobile-chip">{{ currentViewChipText }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card room-table-page__toolbar-card">
          <div v-if="activeView !== 'monthly'" class="room-table-page__toolbar-row">
            <ion-button fill="outline" size="small" @click="handleShiftDate(-1)">{{ $t('accommodation.roomPrice.previousDay') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleGoToday">{{ $t('accommodation.common.today') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftDate(1)">{{ $t('accommodation.roomPrice.nextDay') }}</ion-button>
          </div>

          <label v-if="activeView !== 'monthly'" class="room-table-page__date-field">
            <span>{{ $t('stage5VisibleText.111') }}</span>
            <input :value="selectedDate" type="date" @input="handleDateInput" />
          </label>

          <div v-else class="room-table-page__month-switcher">
            <ion-button fill="clear" size="small" class="room-table-page__month-nav" @click="handleShiftMonth(-1)">
              <ion-icon :icon="chevronBackOutline" aria-hidden="true" />
            </ion-button>
            <button
              type="button"
              class="room-table-page__month-current"
              :aria-label="monthlyText.currentMonth"
              @click="handleGoCurrentMonth"
            >
              {{ selectedMonthLabel }}
            </button>
            <ion-button fill="clear" size="small" class="room-table-page__month-nav" @click="handleShiftMonth(1)">
              <ion-icon :icon="chevronForwardOutline" aria-hidden="true" />
            </ion-button>
          </div>

          <ion-segment v-model="activeView" @ionChange="handleViewChange">
            <ion-segment-button value="daily">
              <ion-label>{{ $t('stage5VisibleText.144') }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="monthly">
              <ion-label>{{ monthlyTabLabel }}</ion-label>
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

        <section v-else-if="activeView === 'monthly'" class="mobile-card room-table-page__monthly-section">
          <div class="mobile-inline-row room-table-page__section-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('accommodation.roomTable.monthlyTab') }}</h2>
              <p class="mobile-note">{{ selectedMonthRangeText }}</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="monthlyRoomCards.length > 0" class="room-table-page__monthly-grid">
            <article v-for="room in monthlyRoomCards" :key="room.roomId" class="room-table-page__monthly-card">
              <header class="room-table-page__monthly-card-header">
                <span>{{ room.roomType }}</span>
                <strong>{{ room.roomNumber }}</strong>
              </header>

              <div class="room-table-page__monthly-weekdays">
                <span v-for="weekday in monthlyWeekdayLabels" :key="weekday">{{ weekday }}</span>
              </div>

              <div class="room-table-page__monthly-days">
                <span
                  v-for="(cell, index) in room.cells"
                  :key="cell ? cell.date : `blank-${room.roomId}-${index}`"
                  class="room-table-page__monthly-day"
                  :class="
                    cell
                      ? [`room-table-page__monthly-day--${cell.kind}`, `room-table-page__monthly-day--${cell.segment}`]
                      : 'room-table-page__monthly-day--blank'
                  "
                  :title="cell?.tooltip || ''"
                >
                  {{ cell?.dayLabel || '' }}
                </span>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note room-table-page__monthly-empty">
            {{ monthlyText.empty }}
          </p>
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
  IonIcon,
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
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { onIonViewWillEnter } from '@ionic/vue'
import { chevronBackOutline, chevronForwardOutline } from 'ionicons/icons'
import {
  getFutureRoomTableData,
  getMonthlyRoomTableData,
  getRoomTableStatistics,
  type FutureRoomStatisticsDTO,
  type FutureRoomTypeDataDTO,
  type MonthlyDailyStatusDTO,
  type RoomStatisticsDTO,
  type RoomTableMonthlyResponse,
} from '@/api/roomTable'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { formatDateWithWeekday, formatPercent, getTodayDate, shiftDate } from '@/utils/accommodation'
import { formatMoney as formatBusinessMoney } from '@/utils/formatters'
import { showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

type RoomTableViewMode = 'daily' | 'monthly' | 'future'
type MonthlyDayKind = 'full' | 'available' | 'available-many'
type MonthlyDaySegment = 'single' | 'start' | 'middle' | 'end'

interface MonthlyRoomDay {
  date: string
  dayLabel: string
  kind: MonthlyDayKind
  segment: MonthlyDaySegment
  tooltip: string
}

interface MonthlyRoomCard {
  roomId: number
  roomNumber: string
  roomType: string
  cells: Array<MonthlyRoomDay | null>
}

interface SummaryMetric {
  key: string
  label: string
  value: string | number
}

type IonContentElement = HTMLElement & {
  getScrollElement?: () => Promise<HTMLElement>
  scrollToPoint?: (x: number, y: number, duration?: number) => Promise<void>
}

type IonContentRef = IonContentElement | { $el?: IonContentElement }

const selectedDate = ref(getTodayDate())
const selectedMonthDate = ref(getMonthStart(getTodayDate()))
const pageContent = ref<IonContentRef | null>(null)
const storeStore = useStoreStore()
const { locale, t } = useI18n()
const activeView = ref<RoomTableViewMode>('daily')
const loading = ref(false)
const errorMessage = ref('')
const dailyStatistics = ref<RoomStatisticsDTO[]>([])
const monthlyRoomTableData = ref<RoomTableMonthlyResponse | null>(null)
const futureRoomTypes = ref<FutureRoomTypeDataDTO[]>([])
const futureStatistics = ref<FutureRoomStatisticsDTO[]>([])
const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))

const monthlyText = computed(() => ({
  currentMonth: t('accommodation.roomTable.monthly.currentMonth'),
  full: t('accommodation.roomTable.monthly.full'),
  available: t('accommodation.roomTable.monthly.available'),
  availableMany: t('accommodation.roomTable.monthly.availableMany'),
  empty: t('accommodation.roomTable.monthly.empty'),
}))

const monthlyTabLabel = computed(() => {
  const currentLocale = String(locale.value)
  if (currentLocale.startsWith('ja')) {
    return '月次'
  }
  if (currentLocale.startsWith('en')) {
    return 'Monthly'
  }
  return '月度'
})

const currentViewChipText = computed(() => {
  if (activeView.value === 'monthly') {
    return monthlyTabLabel.value
  }
  return activeView.value === 'daily' ? t('stage5DynamicUi.25') : t('stage5DynamicUi.75')
})

const currentDateChipText = computed(() => {
  if (activeView.value === 'monthly') {
    return selectedMonthLabel.value
  }
  return formatDateWithWeekday(selectedDate.value)
})

const selectedMonthStart = computed(() => getMonthStart(selectedMonthDate.value))
const selectedMonthEnd = computed(() => getMonthEnd(selectedMonthStart.value))
const selectedMonthDates = computed(() => getDateRange(selectedMonthStart.value, selectedMonthEnd.value))

const roomTableDateLocale = computed(() => {
  const currentLocale = String(locale.value)
  if (currentLocale.startsWith('en')) {
    return 'en-US'
  }
  if (currentLocale === 'zh-TW') {
    return 'zh-TW'
  }
  if (currentLocale.startsWith('ja')) {
    return 'ja-JP'
  }
  return 'zh-CN'
})

const selectedMonthLabel = computed(() => {
  const date = parseYmdAsUtcDate(selectedMonthStart.value)
  return new Intl.DateTimeFormat(roomTableDateLocale.value, {
    year: 'numeric',
    month: 'long',
    timeZone: 'UTC',
  }).format(date)
})

const selectedMonthRangeText = computed(() => {
  return `${selectedMonthStart.value.replace(/-/g, '/')} - ${selectedMonthEnd.value.replace(/-/g, '/')}`
})

const monthlyWeekdayLabels = computed(() =>
  [0, 1, 2, 3, 4, 5, 6].map((day) => t(`accommodation.roomTable.monthly.weekdays.${day}`)),
)

const monthlyRoomCards = computed<MonthlyRoomCard[]>(() => {
  const firstWeekday = getYmdWeekdayIndex(selectedMonthStart.value)
  const blankCells = Array.from({ length: firstWeekday }, () => null)

  return (monthlyRoomTableData.value?.rooms || []).map((room) => {
    const dailyStatusByDate = new Map(
      room.dailyStatus.map((dailyStatus) => [dailyStatus.date, dailyStatus]),
    )
    const days = selectedMonthDates.value.map<MonthlyRoomDay>((date) => {
      const dailyStatus = dailyStatusByDate.get(date)
      const dayLabel = String(parseYmdAsUtcDate(date).getUTCDate())

      if (!dailyStatus) {
        return {
          date,
          dayLabel,
          kind: 'full',
          segment: 'single',
          tooltip: monthlyText.value.full,
        }
      }

      const kind = getMonthlyDayKind(dailyStatus)
      return {
        date,
        dayLabel,
        kind,
        segment: 'single',
        tooltip: getMonthlyDayTooltip(dailyStatus, kind),
      }
    })

    const segmentedDays = days.map<MonthlyRoomDay>((day, index) => {
      const weekday = getYmdWeekdayIndex(day.date)
      const previous = index > 0 ? days[index - 1] : null
      const next = index < days.length - 1 ? days[index + 1] : null
      const connectsPrevious = Boolean(previous && previous.kind === day.kind && weekday !== 0)
      const connectsNext = Boolean(next && next.kind === day.kind && weekday !== 6)

      let segment: MonthlyDaySegment = 'single'
      if (connectsPrevious && connectsNext) {
        segment = 'middle'
      } else if (connectsPrevious) {
        segment = 'end'
      } else if (connectsNext) {
        segment = 'start'
      }

      return {
        ...day,
        segment,
      }
    })

    return {
      roomId: room.roomId,
      roomNumber: room.roomNumber,
      roomType: room.roomType,
      cells: [...blankCells, ...segmentedDays],
    }
  })
})

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

function parseYmdAsUtcDate(value: string) {
  const [year = 1970, month = 1, day = 1] = value.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day))
}

function formatUtcDateAsYmd(date: Date) {
  const year = date.getUTCFullYear()
  const month = String(date.getUTCMonth() + 1).padStart(2, '0')
  const day = String(date.getUTCDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getMonthStart(date: string) {
  const parsedDate = parseYmdAsUtcDate(date)
  return formatUtcDateAsYmd(
    new Date(Date.UTC(parsedDate.getUTCFullYear(), parsedDate.getUTCMonth(), 1)),
  )
}

function getMonthEnd(monthStart: string) {
  const parsedDate = parseYmdAsUtcDate(monthStart)
  return formatUtcDateAsYmd(
    new Date(Date.UTC(parsedDate.getUTCFullYear(), parsedDate.getUTCMonth() + 1, 0)),
  )
}

function shiftMonth(monthStart: string, offsetMonths: number) {
  const parsedDate = parseYmdAsUtcDate(monthStart)
  return formatUtcDateAsYmd(
    new Date(Date.UTC(parsedDate.getUTCFullYear(), parsedDate.getUTCMonth() + offsetMonths, 1)),
  )
}

function shiftUtcDate(date: string, offsetDays: number) {
  const parsedDate = parseYmdAsUtcDate(date)
  parsedDate.setUTCDate(parsedDate.getUTCDate() + offsetDays)
  return formatUtcDateAsYmd(parsedDate)
}

function getDateRange(startDate: string, endDate: string) {
  const dates: string[] = []
  let currentDate = startDate

  while (currentDate <= endDate) {
    dates.push(currentDate)
    currentDate = shiftUtcDate(currentDate, 1)
  }

  return dates
}

function getYmdWeekdayIndex(date: string) {
  return parseYmdAsUtcDate(date).getUTCDay()
}

function getMonthlyDayKind(dailyStatus: MonthlyDailyStatusDTO): MonthlyDayKind {
  if (dailyStatus.displayStatus === 'AVAILABLE_MANY') {
    return 'available-many'
  }

  if (dailyStatus.displayStatus === 'AVAILABLE') {
    return 'available'
  }

  return 'full'
}

function getMonthlyDayTooltip(dailyStatus: MonthlyDailyStatusDTO, kind: MonthlyDayKind) {
  if (dailyStatus.closed) {
    return dailyStatus.closeRemark
      ? `${monthlyText.value.full}: ${dailyStatus.closeRemark}`
      : monthlyText.value.full
  }

  if (dailyStatus.reservation) {
    return `${monthlyText.value.full}: ${dailyStatus.reservation.guestName || ''}`.trim()
  }

  if (kind === 'available-many') {
    const availableCount = dailyStatus.roomTypeAvailableRooms || 0
    return `${monthlyText.value.availableMany}: ${availableCount}`
  }

  return kind === 'available' ? monthlyText.value.available : monthlyText.value.full
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function resolvePageContentElement(): IonContentElement | null {
  const content = pageContent.value
  if (!content) {
    return null
  }

  if ('$el' in content && content.$el) {
    return content.$el
  }

  return content as IonContentElement
}

async function getPageScrollTop() {
  const contentElement = resolvePageContentElement()
  const scrollElement = await contentElement?.getScrollElement?.()
  return scrollElement?.scrollTop ?? 0
}

async function restorePageScrollTop(scrollTop: number) {
  await nextTick()

  const contentElement = resolvePageContentElement()
  if (!contentElement) {
    return
  }

  await contentElement.scrollToPoint?.(0, scrollTop, 0)
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

async function loadMonthlyView() {
  monthlyRoomTableData.value = null
  const response = await getMonthlyRoomTableData(selectedMonthStart.value, selectedMonthEnd.value)
  if (!response.success || !response.data) {
    throw new Error(response.message || t('accommodation.roomTable.messages.loadMonthlyFailed'))
  }

  monthlyRoomTableData.value = response.data
}

function getLoadFallbackMessage() {
  if (activeView.value === 'monthly') {
    return t('accommodation.roomTable.messages.loadMonthlyFailed')
  }
  return t('stage5Pattern.loadFailed')
}

async function loadPageData() {
  loading.value = true
  errorMessage.value = ''

  try {
    if (activeView.value === 'daily') {
      await loadDailyView()
      monthlyRoomTableData.value = null
      futureRoomTypes.value = []
      futureStatistics.value = []
    } else if (activeView.value === 'monthly') {
      await loadMonthlyView()
      dailyStatistics.value = []
      futureRoomTypes.value = []
      futureStatistics.value = []
    } else {
      await loadFutureView()
      dailyStatistics.value = []
      monthlyRoomTableData.value = null
    }
  } catch (error) {
    const message = resolveWarningMessage(error, getLoadFallbackMessage())
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
  const previousScrollTop = await getPageScrollTop()

  if (activeView.value === 'monthly') {
    selectedMonthDate.value = getMonthStart(selectedDate.value)
  }

  await loadPageData()
  await restorePageScrollTop(previousScrollTop)
}

async function handleShiftMonth(offsetMonths: number) {
  selectedMonthDate.value = shiftMonth(selectedMonthDate.value, offsetMonths)
  await loadPageData()
}

async function handleGoCurrentMonth() {
  selectedMonthDate.value = getMonthStart(getTodayDate())
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

.room-table-page .mobile-stack,
.room-table-page .mobile-card,
.room-table-page__toolbar-card,
.room-table-page__monthly-grid,
.room-table-page__monthly-card,
.room-table-page__future-list,
.room-table-page__future-card,
.room-table-page__future-strip,
.room-table-page__future-summary,
.room-table-page__future-summary-strip {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
}

.room-table-page__hero {
  margin: 4px 0 14px;
  border-color: rgba(116, 138, 185, 0.08);
  background: rgba(250, 252, 254, 0.76);
  box-shadow: var(--ios-pms-shadow-card);
}

.room-table-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: var(--ios-pms-weight-bold);
}

.room-table-page__toolbar-card {
  display: grid;
  gap: 14px;
  overflow: hidden;
  padding: 16px;
  border-color: rgba(116, 138, 185, 0.08);
  background: rgba(250, 252, 254, 0.78);
  box-shadow: var(--ios-pms-shadow-card);
}

.room-table-page__toolbar-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  min-width: 0;
}

.room-table-page__toolbar-row ion-button {
  min-width: 0;
  min-height: 34px;
  margin: 0;
  --padding-start: 8px;
  --padding-end: 8px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 10px;
  --box-shadow: none;
  --background: rgba(255, 255, 255, 0.84);
  --background-hover: rgba(255, 255, 255, 0.96);
  --background-activated: rgba(245, 248, 255, 0.96);
  --border-color: rgba(130, 143, 165, 0.2);
  --color: var(--ios-pms-text-secondary);
  font-size: 12px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.room-table-page__toolbar-row ion-button::part(native) {
  min-width: 0;
  min-height: 34px;
  border-radius: 10px;
  line-height: 1.2;
}

.room-table-page__date-field {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.room-table-page__date-field span {
  color: var(--ios-pms-text-secondary);
  font-size: 13px;
  font-weight: var(--ios-pms-weight-medium);
}

.room-table-page__date-field input {
  width: 100%;
  min-width: 0;
  min-height: 42px;
  padding: 9px 12px;
  box-sizing: border-box;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: 12px;
  outline: none;
  background: rgba(255, 255, 255, 0.9);
  color: var(--ios-pms-text-primary);
  font: inherit;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    background 160ms ease;
}

.room-table-page__date-field input:focus {
  border-color: rgba(var(--ion-color-primary-rgb), 0.36);
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(var(--ion-color-primary-rgb), 0.08);
}

.room-table-page__month-switcher {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 38px;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.room-table-page__month-nav {
  width: 38px;
  height: 38px;
  min-height: 38px;
  margin: 0;
  --padding-start: 0;
  --padding-end: 0;
  --border-radius: 12px;
  --background: rgba(255, 255, 255, 0.84);
  --background-activated: rgba(245, 248, 255, 0.96);
  --color: var(--ios-pms-primary);
}

.room-table-page__month-nav::part(native) {
  width: 38px;
  height: 38px;
  border: 1px solid rgba(130, 143, 165, 0.16);
  border-radius: 12px;
}

.room-table-page__month-nav ion-icon {
  font-size: 20px;
}

.room-table-page__month-current {
  min-width: 0;
  min-height: 38px;
  padding: 0 12px;
  border: 1px solid rgba(130, 143, 165, 0.16);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--ios-pms-text-primary);
  font: inherit;
  font-size: 15px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0;
  line-height: 1.2;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-table-page__toolbar-card > ion-segment {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  height: 36px;
  min-height: 36px;
  padding: 2px;
  overflow: hidden;
  box-sizing: border-box;
  border: 1px solid rgba(130, 143, 165, 0.14);
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(235, 239, 246, 0.88);
}

.room-table-page__toolbar-card > ion-segment ion-segment-button {
  width: 100%;
  min-width: 0;
  max-width: none;
  height: 32px;
  min-height: 32px;
  margin: 0;
  overflow: hidden;
  --padding-start: 4px;
  --padding-end: 4px;
  --border-radius: var(--ios-pms-radius-pill);
  --color: var(--ios-pms-text-secondary);
  --color-checked: #ffffff;
  --indicator-color: #343436;
  --indicator-box-shadow: none;
  font-size: 14px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.room-table-page__toolbar-card > ion-segment ion-segment-button::part(native) {
  min-width: 0;
  min-height: 32px;
  padding: 0 4px;
  overflow: hidden;
  border-radius: var(--ios-pms-radius-pill);
}

.room-table-page__toolbar-card > ion-segment ion-segment-button::part(indicator) {
  padding: 0;
}

.room-table-page__toolbar-card
  > ion-segment
  ion-segment-button::part(indicator-background) {
  border-radius: var(--ios-pms-radius-pill);
  background: #343436;
  box-shadow: none;
}

.room-table-page__toolbar-card > ion-segment ion-label {
  display: block;
  width: 100%;
  min-width: 0;
  margin: 0;
  overflow: hidden;
  line-height: 1.2;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow-wrap: normal;
}

.room-table-page__error {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid rgba(var(--ion-color-danger-rgb), 0.12);
  border-radius: 10px;
  background: rgba(var(--ion-color-danger-rgb), 0.06);
  color: var(--ion-color-danger);
}

.room-table-page__section-header {
  align-items: flex-start;
  gap: 12px;
}

.room-table-page__section-header > div {
  min-width: 0;
}

.room-table-page .mobile-stack > .mobile-card:not(.room-table-page__toolbar-card) {
  overflow: hidden;
  padding: 16px;
  border-color: rgba(116, 138, 185, 0.08);
  background: rgba(250, 252, 254, 0.78);
  box-shadow: var(--ios-pms-shadow-card);
}

.room-table-page__metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 16px;
}

.room-table-page__metric-card {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 13px;
  border: 1px solid rgba(130, 143, 165, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: none;
}

.room-table-page__metric-card span {
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.35;
}

.room-table-page__metric-card strong {
  color: var(--ios-pms-primary);
  font-size: 19px;
  line-height: 1.1;
}

.room-table-page__metric-card:nth-child(4n + 2) strong {
  color: var(--ion-color-success);
}

.room-table-page__metric-card:nth-child(4n + 3) strong {
  color: var(--ion-color-warning);
}

.room-table-page__metric-card:nth-child(4n + 4) strong {
  color: var(--ios-pms-text-secondary);
}

.room-table-page__statistics-list,
.room-table-page__future-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.room-table-page__statistics-card,
.room-table-page__future-card {
  min-width: 0;
  overflow: hidden;
  padding: 15px;
  border: 1px solid rgba(130, 143, 165, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: none;
}

.room-table-page__statistics-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.room-table-page__statistics-header > div {
  min-width: 0;
}

.room-table-page__statistics-header strong,
.room-table-page__statistics-header p {
  margin: 0;
}

.room-table-page__statistics-header strong {
  color: var(--ios-pms-text-primary);
  font-size: 15px;
  line-height: 1.35;
}

.room-table-page__statistics-header p {
  margin-top: 5px;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.room-table-page__statistics-header span {
  flex-shrink: 0;
  min-height: 28px;
  padding: 5px 9px;
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(var(--ion-color-primary-rgb), 0.08);
  color: var(--ios-pms-primary);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.5;
}

.room-table-page__statistics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
  margin-top: 13px;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
}

.room-table-page__statistics-grid span {
  min-width: 0;
  padding: 7px 8px;
  border-radius: 9px;
  background: rgba(239, 243, 249, 0.74);
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.room-table-page__monthly-section {
  display: grid;
  gap: 14px;
}

.room-table-page__monthly-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(148px, 1fr));
  gap: 12px;
}

.room-table-page__monthly-card {
  display: grid;
  gap: 9px;
  min-width: 0;
  padding: 12px 10px 13px;
  border: 1px solid rgba(130, 143, 165, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
}

.room-table-page__monthly-card-header {
  display: grid;
  gap: 3px;
  min-width: 0;
  text-align: center;
}

.room-table-page__monthly-card-header span {
  min-width: 0;
  overflow: hidden;
  color: var(--ios-pms-text-muted);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-table-page__monthly-card-header strong {
  min-width: 0;
  overflow: hidden;
  color: var(--ios-pms-text-primary);
  font-size: 17px;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-table-page__monthly-weekdays {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0;
  padding: 0 2px 7px;
  border-bottom: 1px solid rgba(130, 143, 165, 0.1);
  color: var(--ios-pms-text-muted);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.2;
  text-align: center;
}

.room-table-page__monthly-weekdays span {
  min-width: 0;
  overflow: hidden;
  text-overflow: clip;
  white-space: nowrap;
}

.room-table-page__monthly-days {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 7px 0;
  min-width: 0;
}

.room-table-page__monthly-day {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  height: 22px;
  margin: 0;
  box-sizing: border-box;
  border-radius: var(--ios-pms-radius-pill);
  color: var(--ios-pms-text-primary);
  font-size: 12px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1;
}

.room-table-page__monthly-day--single {
  border-radius: var(--ios-pms-radius-pill);
}

.room-table-page__monthly-day--start {
  border-radius: var(--ios-pms-radius-pill) 0 0 var(--ios-pms-radius-pill);
}

.room-table-page__monthly-day--middle {
  border-radius: 0;
}

.room-table-page__monthly-day--end {
  border-radius: 0 var(--ios-pms-radius-pill) var(--ios-pms-radius-pill) 0;
}

.room-table-page__monthly-day--full {
  background: #ff7f85;
  color: #6f262a;
}

.room-table-page__monthly-day--available {
  background: #bff2a4;
  color: #34702b;
}

.room-table-page__monthly-day--available-many {
  background: #ffe27a;
  color: #75621b;
}

.room-table-page__monthly-day--blank {
  margin: 0;
  background: transparent;
}

.room-table-page__monthly-empty {
  margin: 0;
  padding: 18px 12px;
  border: 1px solid rgba(130, 143, 165, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.76);
  text-align: center;
}

.room-table-page__future-note {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(130, 143, 165, 0.1);
  border-radius: 10px;
  background: rgba(239, 243, 249, 0.68);
}

.room-table-page__future-strip,
.room-table-page__future-summary-strip {
  display: flex;
  gap: 10px;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  margin-top: 14px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 2px 0 6px;
  overscroll-behavior-inline: contain;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}

.room-table-page__future-strip::-webkit-scrollbar,
.room-table-page__future-summary-strip::-webkit-scrollbar {
  display: none;
}

.room-table-page__future-day,
.room-table-page__future-summary-card {
  flex: 0 0 120px;
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 12px;
  box-sizing: border-box;
  border: 1px solid rgba(130, 143, 165, 0.12);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  box-shadow: none;
}

.room-table-page__future-day strong,
.room-table-page__future-summary-card strong {
  color: var(--ios-pms-text-primary);
  font-size: 14px;
}

.room-table-page__future-summary {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(130, 143, 165, 0.12);
}

.room-table-page__future-summary .mobile-section-title {
  margin-bottom: 0;
}

@media (max-width: 360px) {
  .room-table-page__toolbar-card,
  .room-table-page .mobile-stack > .mobile-card:not(.room-table-page__toolbar-card) {
    padding: 14px;
  }

  .room-table-page__toolbar-row {
    gap: 6px;
  }

  .room-table-page__toolbar-row ion-button {
    --padding-start: 5px;
    --padding-end: 5px;
    font-size: 11px;
  }

  .room-table-page__statistics-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .room-table-page__monthly-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .room-table-page__monthly-card {
    padding-right: 12px;
    padding-left: 12px;
  }
}
</style>
