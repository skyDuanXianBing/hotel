<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="orders-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title class="orders-header__title">{{ $t('routes.Orders') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button class="orders-header__icon-btn" fill="clear" @click="handleToggleSearch">
            <ion-icon :icon="searchOutline" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page orders-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('order.mobile.refresh')" refreshing-spinner="crescent" />
      </ion-refresher>

      <div class="orders-page__shell" :class="{ 'has-search': isSearchVisible }">
        <section v-if="isSearchVisible" class="orders-search-panel">
          <div class="orders-search-panel__bar">
            <ion-searchbar
              v-model="searchKeyword"
              :debounce="0"
              class="orders-searchbar"
              :placeholder="$t('order.mobile.searchPlaceholder')"
            />
            <button type="button" class="orders-search-panel__cancel" @click="handleHideSearch">
              {{ $t('order.mobile.actions.cancel') }}
            </button>
          </div>
        </section>

        <section class="orders-tabs-strip">
          <div class="orders-tabs-strip__scroll">
            <button
              v-for="item in displayTabs"
              :key="item.value"
              type="button"
              class="orders-tabs-strip__item"
              :class="{ 'is-active': item.value === activeTab }"
              @click="handleSelectTab(item.value)"
            >
              {{ item.label }}
            </button>
          </div>
        </section>

        <div class="orders-page__content-bg">
        <section class="orders-filter-row">
          <div class="orders-filter-row__scroll">
            <button
              v-if="showDateFilter"
              type="button"
              class="orders-filter-chip"
              :class="{ 'is-active': activeTab === 'all' ? Boolean(filters.startDate || filters.endDate) : Boolean(filters.operationDate) }"
              @click="openFilterPanel('date')"
            >
              {{ dateFilterLabel }}
            </button>
            <button
              type="button"
              class="orders-filter-chip"
              :class="{ 'is-active': filters.roomType.length > 0 }"
              @click="openFilterPanel('roomType')"
            >
              {{ roomTypeFilterLabel }}
            </button>
            <button
              type="button"
              class="orders-filter-chip"
              :class="{ 'is-active': filters.channel.length > 0 }"
              @click="openFilterPanel('channel')"
            >
              {{ channelFilterLabel }}
            </button>
            <button
              type="button"
              class="orders-filter-chip"
              :class="{ 'is-active': Boolean(filters.status) }"
              @click="openFilterPanel('status')"
            >
              {{ statusFilterLabel }}
            </button>
            <button
              type="button"
              class="orders-filter-chip"
              :class="{ 'is-active': Boolean(filters.paymentStatus) }"
              @click="openFilterPanel('paymentStatus')"
            >
              {{ paymentFilterLabel }}
            </button>
            <button
              type="button"
              class="orders-filter-chip"
              :class="{ 'is-active': Boolean(filters.checkinType) }"
              @click="openFilterPanel('checkinType')"
            >
              {{ checkinTypeFilterLabel }}
            </button>
            <button
              v-if="filterCount > 0 || committedKeyword"
              type="button"
              class="orders-filter-chip orders-filter-chip--ghost"
              @click="handleResetSearchAndFilters"
            >
              {{ $t('order.mobile.clear') }}
            </button>
          </div>
        </section>

        <section class="orders-list-section">
          <div class="orders-list-header">
            <div class="orders-list-header__heading">
              <p class="orders-list-header__summary">
                {{ activeTabLabel }} · {{ $t('order.mobile.resultCount', { count: totalElements }) }}
              </p>
              <div v-if="filterCount > 0 || committedKeyword" class="orders-list-header__tags">
                <span v-if="filterCount > 0" class="orders-list-header__tag">
                  {{ $t('order.mobile.filteredCount', { count: filterCount }) }}
                </span>
                <span v-if="committedKeyword" class="orders-list-header__tag">{{ committedKeyword }}</span>
              </div>
            </div>
            <div class="orders-list-header__status" :class="{ 'is-loading': loading }" aria-hidden="true">
              <ion-spinner name="crescent" class="orders-list-header__spinner" />
            </div>
          </div>

          <p v-if="loadNotice" class="orders-notice-text">{{ loadNotice }}</p>
          <p v-else-if="activeTab === 'order-box'" class="orders-notice-text">
            {{ $t('order.mobile.orderBoxNotice') }}
          </p>
          <p v-else-if="activeTab === 'unassigned'" class="orders-notice-text">
            {{ $t('order.mobile.unassignedNotice') }}
          </p>
          <p v-else-if="activeTab === 'deleted-rooms'" class="orders-notice-text orders-notice-text--warning">
            {{ $t('order.mobile.deletedRoomsNotice') }}
          </p>

          <div v-if="orderCards.length > 0" class="orders-list">
            <OrderListCard
              v-for="item in orderCards"
              :key="item.key"
              :reservation="item.reservation"
              :active-tab="activeTab"
              :order-box-item="item.orderBoxItem"
              :channel-color="item.channelColor"
              :currency="currentCurrency"
              :country="storeStore.currentStore?.country"
              @open-detail="openReservationDetail(item.reservation.id)"
              @assign-room="openAssignRoom(item.reservation)"
              @open-actions="presentReservationActions(item.reservation, item.orderBoxItem)"
            />
          </div>

          <div v-else-if="!loading" class="orders-empty-state">
            <div class="orders-empty-state__illustration" aria-hidden="true">
              <span class="orders-empty-state__box"></span>
            </div>
            <p class="orders-empty">{{ $t('order.mobile.empty') }}</p>
          </div>
        </section>

        <section v-if="usePagedEndpoint && hasMore" class="orders-load-more-card">
          <ion-button
            expand="block"
            fill="clear"
            class="orders-load-more-card__button"
            :disabled="loadingMore"
            @click="handleLoadMoreButton"
          >
            {{ loadingMore ? $t('order.options.loading') : $t('order.mobile.loadMore') }}
          </ion-button>
        </section>
        </div>
      </div>

      <ion-infinite-scroll v-if="usePagedEndpoint && hasMore" @ionInfinite="handleInfiniteLoad">
        <ion-infinite-scroll-content :loading-text="$t('order.mobile.loadMore')" />
      </ion-infinite-scroll>
    </ion-content>

    <OrderFilterModal
      :is-open="isFilterModalOpen"
      :active-tab="activeTab"
      :channels="channelOptions"
      :room-types="roomTypeOptions"
      :initial-filters="filters"
      :initial-panel="activeFilterPanel"
      @dismiss="isFilterModalOpen = false"
      @apply="handleApplyFilters"
      @reset="handleResetFilters"
    />

    <AssignRoomModal
      :is-open="isAssignModalOpen"
      :reservation="selectedReservation"
      :room-types="assignableRoomTypes"
      :rooms="assignableRooms"
      :selected-room-type-id="selectedRoomTypeId"
      :selected-room-id="selectedRoomId"
      :loading="assignLoading"
      :submitting="assignSubmitting"
      @dismiss="handleDismissAssignModal"
      @refresh="refreshAssignableRoomTypes"
      @select-room-type="handleSelectAssignableRoomType"
      @select-room="handleSelectAssignableRoom"
      @submit="submitAssignRoom"
    />
  </ion-page>
</template>

<script setup lang="ts">
import {
  actionSheetController,
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonInfiniteScroll,
  IonInfiniteScrollContent,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSearchbar,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { searchOutline } from 'ionicons/icons'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import AssignRoomModal from '@/components/order/AssignRoomModal.vue'
import OrderFilterModal from '@/components/order/OrderFilterModal.vue'
import OrderListCard from '@/components/order/OrderListCard.vue'
import {
  canAssignRoom,
  canCancelOrder,
  canCheckInOrder,
  canCheckOutOrder,
  createDefaultOrderFilters,
  getOrderTabLabel,
  mapHomeTypeToOrderTab,
  mapOrderTabToApiType,
  matchesReservationSearch,
  ORDER_PRIMARY_TABS,
  ORDER_SECONDARY_TABS,
  type OrderFilterForm,
  type OrderOptionItem,
  type OrderTabValue,
} from '@/components/order/orderUtils'
import {
  checkCanMoveToOrderBox,
  getOrderBoxList,
  moveOutOrderBox,
  moveToOrderBox,
  type OrderBoxItem,
} from '@/api/orderBox'
import {
  assignReservationRoom,
  cancelReservation,
  checkInReservation,
  checkOutReservation,
  getAllChannels,
  getAssignableRooms,
  getReservationsByType,
  getReservationsWithFilters,
  getReservationStatistics,
  type AssignableRoomDTO,
  type AssignableRoomTypeDTO,
  type ReservationDTO,
  type ReservationStatistics,
} from '@/api/reservation'
import { getAllRoomTypes } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { useRoomStatusStore } from '@/stores/roomStatus'
import { useStoreStore } from '@/stores/store'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { getStoreTodayDate } from '@/utils/storeBusinessDate'

const PAGE_SIZE = 10
const SEARCH_DEBOUNCE = 280

const EMPTY_STATISTICS: ReservationStatistics = {
  todayCheckinCount: 0,
  todayCheckoutCount: 0,
  todayNewCount: 0,
  unassignedCount: 0,
  pendingCount: 0,
  totalReservations: 0,
}

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const roomStatusStore = useRoomStatusStore()
const storeStore = useStoreStore()
const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')

type FilterPanelKey = 'date' | 'roomType' | 'channel' | 'status' | 'paymentStatus' | 'checkinType'

const activeTab = ref<OrderTabValue>('all')
const searchKeyword = ref('')
const committedKeyword = ref('')
const filters = ref<OrderFilterForm>(createDefaultOrderFilters())
const statistics = ref<ReservationStatistics>(EMPTY_STATISTICS)
const reservations = ref<ReservationDTO[]>([])
const orderBoxItems = ref<OrderBoxItem[]>([])
const channelOptions = ref<OrderOptionItem[]>([])
const roomTypeOptions = ref<OrderOptionItem[]>([])
const optionsLoaded = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const summaryLoading = ref(false)
const loadNotice = ref('')
const page = ref(0)
const totalPages = ref(1)
const totalElements = ref(0)
const isFilterModalOpen = ref(false)
const isSearchVisible = ref(false)
const isAssignModalOpen = ref(false)
const activeFilterPanel = ref<FilterPanelKey>('date')
const selectedReservation = ref<ReservationDTO | null>(null)
const assignableRoomTypes = ref<AssignableRoomTypeDTO[]>([])
const assignableRooms = ref<AssignableRoomDTO[]>([])
const selectedRoomTypeId = ref<number | null>(null)
const selectedRoomId = ref<number | null>(null)
const assignLoading = ref(false)
const assignSubmitting = ref(false)

let searchTimer = 0

const VALID_ORDER_TABS: OrderTabValue[] = [
  'all',
  'today-checkin',
  'today-checkout',
  'today-new',
  'unassigned',
  'assigned',
  'pending',
  'order-box',
  'deleted-rooms',
]

const filterCount = computed(() => {
  let count = 0
  if (filters.value.channel.length > 0) {
    count += 1
  }
  if (filters.value.roomType.length > 0) {
    count += 1
  }
  if (filters.value.checkinType) {
    count += 1
  }
  if (filters.value.status) {
    count += 1
  }
  if (filters.value.paymentStatus) {
    count += 1
  }
  if (activeTab.value === 'all' && (filters.value.startDate || filters.value.endDate)) {
    count += 1
  }
  if (isTodayOperationTab.value && filters.value.operationDate) {
    count += 1
  }
  return count
})

const isTodayOperationTab = computed(() =>
  ['today-checkin', 'today-checkout', 'today-new'].includes(activeTab.value),
)

const showDateFilter = computed(() => activeTab.value === 'all' || isTodayOperationTab.value)

const todayOperationDateLabel = computed(() => {
  if (activeTab.value === 'today-checkin') {
    return t('order.filters.todayCheckinDate')
  }
  if (activeTab.value === 'today-checkout') {
    return t('order.filters.todayCheckoutDate')
  }
  if (activeTab.value === 'today-new') {
    return t('order.filters.todayNewDate')
  }
  return t('order.filters.operationDate')
})

const usePagedEndpoint = computed(() => {
  if (activeTab.value === 'order-box') {
    return false
  }

  if (activeTab.value === 'all') {
    return true
  }

  if (activeTab.value === 'deleted-rooms') {
    return true
  }

  if (isTodayOperationTab.value) {
    return true
  }

  if (committedKeyword.value.trim()) {
    return true
  }

  if (filterCount.value > 0) {
    return true
  }

  return false
})

const hasMore = computed(() => page.value < totalPages.value - 1)

const activeTabLabel = computed(() => {
  return getOrderTabLabel(activeTab.value)
})

const displayTabs = computed(() =>
  [...ORDER_PRIMARY_TABS, ...ORDER_SECONDARY_TABS].map((item) => ({
    ...item,
    label: t(item.labelKey),
  })),
)

const dateFilterLabel = computed(() => {
  if (activeTab.value === 'all') {
    if (filters.value.startDate && filters.value.endDate) {
      return `${filters.value.startDate.slice(5)} ~ ${filters.value.endDate.slice(5)}`
    }
    if (filters.value.startDate) {
      return filters.value.startDate.slice(5)
    }
    if (filters.value.endDate) {
      return filters.value.endDate.slice(5)
    }
    return t('order.filters.createdAt')
  }

  if (isTodayOperationTab.value) {
    return filters.value.operationDate
      ? filters.value.operationDate.slice(5)
      : todayOperationDateLabel.value
  }

  return t('order.filters.createdAt')
})

const roomTypeFilterLabel = computed(() => {
  if (filters.value.roomType.length === 0) {
    return t('order.filters.roomType')
  }
  const firstMatched = roomTypeOptions.value.find((item) => item.value === filters.value.roomType[0])
  const firstLabel = firstMatched?.label || filters.value.roomType[0] || t('order.filters.roomType')
  if (filters.value.roomType.length === 1) {
    return firstLabel
  }
  return `${firstLabel} +${filters.value.roomType.length - 1}`
})

const channelFilterLabel = computed(() => {
  if (filters.value.channel.length === 0) {
    return t('order.filters.channel')
  }
  const matched = channelOptions.value.find((item) => item.value === filters.value.channel[0])
  const firstLabel = matched?.label || filters.value.channel[0] || t('order.filters.channel')
  if (filters.value.channel.length === 1) {
    return firstLabel
  }
  return `${firstLabel} +${filters.value.channel.length - 1}`
})

const statusFilterLabel = computed(() => {
  if (!filters.value.status) {
    return t('order.filters.stayStatus')
  }
  if (filters.value.status === 'checked-in') {
    return t('order.options.checkedIn')
  }
  if (filters.value.status === 'not-checked-in') {
    return t('order.options.notCheckedIn')
  }
  return t('order.options.checkedOut')
})

const paymentFilterLabel = computed(() => {
  if (!filters.value.paymentStatus) {
    return t('order.table.roomFee')
  }
  return filters.value.paymentStatus === 'paid'
    ? t('order.options.paid')
    : t('order.options.unpaid')
})

const checkinTypeFilterLabel = computed(() => {
  if (!filters.value.checkinType) {
    return t('order.filters.checkinType')
  }
  if (filters.value.checkinType === 'early') {
    return t('order.options.earlyCheckin')
  }
  if (filters.value.checkinType === 'late') {
    return t('order.options.lateCheckin')
  }
  return t('order.options.normalCheckin')
})

const channelColorMap = computed(() => {
  const result = new Map<string, string>()
  for (const item of channelOptions.value) {
    const name = normalizeChannelLookupValue(item.value)
    if (name && item.color) {
      result.set(name, item.color)
    }
  }
  return result
})

const orderCards = computed(() => {
  if (activeTab.value === 'order-box') {
    return orderBoxItems.value.map((item) => ({
      key: `order-box-${item.id}`,
      reservation: item.reservation,
      orderBoxItem: item,
      channelColor: resolveReservationChannelColor(item.reservation),
    }))
  }

  return reservations.value.map((item) => ({
    key: `reservation-${item.id}`,
    reservation: item,
    orderBoxItem: null,
    channelColor: resolveReservationChannelColor(item),
  }))
})

function normalizeChannelLookupValue(value?: string) {
  return (value || '').trim().toLowerCase()
}

function resolveReservationChannelColor(reservation: ReservationDTO) {
  return channelColorMap.value.get(normalizeChannelLookupValue(reservation.channelName)) || ''
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function escapeAlertMessage(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/\n/g, '<br />')
}

async function presentReservationNotes(reservation: ReservationDTO) {
  const notesText = reservation.notes?.trim() || t('order.mobile.notesEmpty')
  const alert = await alertController.create({
    header: t('order.mobile.actions.guestNotes'),
    message: escapeAlertMessage(notesText),
    buttons: [t('order.mobile.understood')],
  })

  await alert.present()
}

function resetPagination() {
  page.value = 0
  totalPages.value = 1
  totalElements.value = 0
}

function normalizeOrderTab(value: unknown): OrderTabValue | null {
  if (typeof value !== 'string') {
    return null
  }
  for (const tab of VALID_ORDER_TABS) {
    if (tab === value) {
      return tab
    }
  }
  return null
}

function normalizeMultiSelectSnapshotValue(value: unknown) {
  if (typeof value === 'string') {
    const trimmed = value.trim()
    return trimmed ? [trimmed] : []
  }

  if (!Array.isArray(value)) {
    return []
  }

  const normalizedValues = new Set<string>()
  for (const item of value) {
    if (typeof item !== 'string') {
      continue
    }
    const trimmed = item.trim()
    if (trimmed) {
      normalizedValues.add(trimmed)
    }
  }
  return [...normalizedValues]
}

function serializeMultiSelectFilterValue(values: string[]) {
  if (values.length === 0) {
    return undefined
  }
  return values.join(',')
}

function parseFiltersSnapshot(snapshot: string) {
  const defaultFilters = createDefaultOrderFilters()
  if (!snapshot) {
    return defaultFilters
  }

  try {
    const parsed = JSON.parse(snapshot) as Record<string, unknown>
    return {
      channel: normalizeMultiSelectSnapshotValue(parsed.channel),
      roomType: normalizeMultiSelectSnapshotValue(parsed.roomType),
      checkinType: typeof parsed.checkinType === 'string' ? parsed.checkinType : '',
      status: typeof parsed.status === 'string' ? parsed.status : '',
      paymentStatus: typeof parsed.paymentStatus === 'string' ? parsed.paymentStatus : '',
      startDate: typeof parsed.startDate === 'string' ? parsed.startDate : '',
      endDate: typeof parsed.endDate === 'string' ? parsed.endDate : '',
      operationDate: typeof parsed.operationDate === 'string' ? parsed.operationDate : '',
    }
  } catch {
    return defaultFilters
  }
}

function serializeFiltersSnapshot(currentFilters: OrderFilterForm) {
  if (
    currentFilters.channel.length === 0 &&
    currentFilters.roomType.length === 0 &&
    !currentFilters.checkinType &&
    !currentFilters.status &&
    !currentFilters.paymentStatus &&
    !currentFilters.startDate &&
    !currentFilters.endDate &&
    !currentFilters.operationDate
  ) {
    return ''
  }

  return JSON.stringify(currentFilters)
}

function buildOrderContextQuery() {
  const query: Record<string, string> = {
    tab: activeTab.value,
  }
  const keyword = committedKeyword.value.trim()
  const filtersSnapshot = serializeFiltersSnapshot(filters.value)

  if (keyword) {
    query.keyword = keyword
  }
  if (filtersSnapshot) {
    query.filtersSnapshot = filtersSnapshot
  }

  return query
}

async function syncOrdersRouteContext() {
  if (route.path !== ROUTE_PATHS.orders) {
    return
  }

  await router.replace({
    path: ROUTE_PATHS.orders,
    query: buildOrderContextQuery(),
  })
}

function applyRouteContextSelection() {
  const queryTab = normalizeOrderTab(route.query.tab)
  const queryKeyword = typeof route.query.keyword === 'string' ? route.query.keyword.trim() : ''
  const queryFiltersSnapshot =
    typeof route.query.filtersSnapshot === 'string' ? route.query.filtersSnapshot : ''

  if (!queryTab && !queryKeyword && !queryFiltersSnapshot) {
    return
  }

  if (queryTab) {
    activeTab.value = queryTab
  }
  searchKeyword.value = queryKeyword
  committedKeyword.value = queryKeyword
  filters.value = parseFiltersSnapshot(queryFiltersSnapshot)
}

function buildOrdersDefaultHref() {
  return router.resolve({
    path: ROUTE_PATHS.orders,
    query: buildOrderContextQuery(),
  }).fullPath
}

function applyRouteTypeSelection() {
  const routeType = typeof route.query.type === 'string' ? route.query.type : ''
  if (!routeType) {
    return
  }

  activeTab.value = mapHomeTypeToOrderTab(routeType)
  searchKeyword.value = ''
  committedKeyword.value = ''
  filters.value = createDefaultOrderFilters()

  if (route.path === ROUTE_PATHS.orders) {
    void router.replace({ path: ROUTE_PATHS.orders })
  }
}

async function loadFilterOptions(force = false) {
  if (optionsLoaded.value && !force) {
    return
  }

  const [channelResponse, roomTypeResponse] = await Promise.all([getAllChannels(), getAllRoomTypes()])

  if (!channelResponse.success || !channelResponse.data) {
    throw new Error(channelResponse.message || t('order.messages.loadChannelFiltersFailed'))
  }
  if (!roomTypeResponse.success || !roomTypeResponse.data) {
    throw new Error(roomTypeResponse.message || t('order.messages.loadRoomTypeFiltersFailed'))
  }

  channelOptions.value = channelResponse.data
    .filter((item) => item.name && item.name.trim().length > 0)
    .map((item) => ({
      label: item.name,
      value: item.name,
      color: item.color,
    }))

  roomTypeOptions.value = roomTypeResponse.data
    .filter((item) => item.name && item.name.trim().length > 0)
    .map((item) => ({
      label: item.name,
      value: item.name,
    }))

  optionsLoaded.value = true
}

async function loadStatistics() {
  summaryLoading.value = true
  try {
    const response = await getReservationStatistics()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5Pattern.loadFailed'))
    }
    statistics.value = response.data
  } finally {
    summaryLoading.value = false
  }
}

function buildPagedFilters(nextPage: number) {
  return {
    page: nextPage,
    size: PAGE_SIZE,
    searchKeyword: committedKeyword.value.trim() || undefined,
    channel: serializeMultiSelectFilterValue(filters.value.channel),
    roomType: serializeMultiSelectFilterValue(filters.value.roomType),
    checkinType: filters.value.checkinType || undefined,
    status: filters.value.status || undefined,
    paymentStatus: filters.value.paymentStatus || undefined,
    startDate: activeTab.value === 'all' ? filters.value.startDate || undefined : undefined,
    endDate: activeTab.value === 'all' ? filters.value.endDate || undefined : undefined,
    orderType: activeTab.value !== 'all' ? activeTab.value : undefined,
    operationDate: isTodayOperationTab.value
      ? filters.value.operationDate || getStoreTodayDate()
      : undefined,
  }
}

async function loadOrderBoxList() {
  const response = await getOrderBoxList()
  if (!response.success || !response.data) {
    throw new Error(response.message || t('order.messages.loadOrderBoxFailed'))
  }

  const localFilters: OrderFilterForm = {
    ...filters.value,
    startDate: '',
    endDate: '',
    operationDate: '',
  }
  const nextItems: OrderBoxItem[] = []
  for (const item of response.data) {
    if (matchesReservationSearch(item.reservation, localFilters, committedKeyword.value)) {
      nextItems.push(item)
    }
  }

  orderBoxItems.value = nextItems
  reservations.value = []
  resetPagination()
  totalElements.value = nextItems.length
}

async function loadOrders(reset = true) {
  if (reset) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    if (activeTab.value === 'order-box') {
      await loadOrderBoxList()
      return
    }

    const apiType = mapOrderTabToApiType(activeTab.value)
    if (!usePagedEndpoint.value && apiType) {
      const response = await getReservationsByType(apiType)
      if (!response.success || !response.data) {
        throw new Error(response.message || t('stage5Pattern.loadFailed'))
      }

      reservations.value = response.data
      orderBoxItems.value = []
      resetPagination()
      totalElements.value = response.data.length
      return
    }

    const nextPage = reset ? 0 : page.value + 1
    const response = await getReservationsWithFilters(buildPagedFilters(nextPage))
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5Pattern.loadFailed'))
    }

    const nextContent = response.data.content || []
    if (reset) {
      reservations.value = nextContent
    } else {
      reservations.value = [...reservations.value, ...nextContent]
    }

    orderBoxItems.value = []
    page.value = Number(response.data.currentPage ?? nextPage)
    totalPages.value = Number(response.data.totalPages ?? 1)
    totalElements.value = Number(response.data.totalElements ?? reservations.value.length)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadPage(forceOptions = false) {
  loadNotice.value = ''

  const tasks = [loadStatistics(), loadOrders(true)]
  if (forceOptions || !optionsLoaded.value) {
    tasks.push(loadFilterOptions(forceOptions))
  }

  const results = await Promise.allSettled(tasks)
  const warnings: string[] = []
  let firstUnhandledError: unknown = null

  for (const result of results) {
    if (result.status === 'rejected') {
      warnings.push(resolveWarningMessage(result.reason, t('order.mobile.messages.syncFailed')))
      if (!firstUnhandledError && !isHandledRequestError(result.reason)) {
        firstUnhandledError = result.reason
      }
    }
  }

  if (warnings.length > 0) {
    loadNotice.value = warnings.join('；')
    if (firstUnhandledError) {
      showWarningToast(resolveWarningMessage(firstUnhandledError, warnings[0]))
    }
  }
}

async function confirmAction(header: string, message: string, confirmText: string, destructive = false) {
  const alert = await alertController.create({
    header,
    message,
    buttons: [
      {
        text: t('order.mobile.actions.cancel'),
        role: 'cancel',
      },
      {
        text: confirmText,
        role: destructive ? 'destructive' : 'confirm',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  if (destructive) {
    return result.role === 'destructive'
  }
  return result.role === 'confirm'
}

async function refreshAfterMutation() {
  const results = await Promise.allSettled([
    loadStatistics(),
    loadOrders(true),
    roomStatusStore.refreshAll(),
  ])
  for (const result of results) {
    if (result.status === 'rejected') {
      throw result.reason
    }
  }
}

async function handleCheckIn(reservation: ReservationDTO) {
  const confirmed = await confirmAction(
    t('order.mobile.actions.checkIn'),
    t('order.mobile.confirmCheckIn', { guest: reservation.guestName }),
    t('order.mobile.actions.confirmCheckIn'),
  )
  if (!confirmed) {
    return
  }

  try {
    const response = await checkInReservation(reservation.id)
    if (!response.success) {
      throw new Error(response.message || t('order.mobile.messages.checkInFailed'))
    }
    showSuccessToast(t('order.mobile.messages.checkInSuccess'))
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.checkInFailed')))
    }
  }
}

async function handleCheckOut(reservation: ReservationDTO) {
  const confirmed = await confirmAction(
    t('order.mobile.actions.checkOut'),
    t('order.mobile.confirmCheckOut', { guest: reservation.guestName }),
    t('order.mobile.actions.confirmCheckOut'),
  )
  if (!confirmed) {
    return
  }

  try {
    const response = await checkOutReservation(reservation.id)
    if (!response.success) {
      throw new Error(response.message || t('order.mobile.messages.checkOutFailed'))
    }
    showSuccessToast(t('order.mobile.messages.checkOutSuccess'))
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.checkOutFailed')))
    }
  }
}

async function handleCancelReservation(reservation: ReservationDTO) {
  const confirmed = await confirmAction(
    t('order.mobile.actions.cancelOrder'),
    t('order.mobile.confirmCancel', { guest: reservation.guestName }),
    t('order.mobile.actions.confirmCancelOrder'),
    true,
  )
  if (!confirmed) {
    return
  }

  try {
    const response = await cancelReservation(reservation.id)
    if (!response.success) {
      throw new Error(response.message || t('order.mobile.messages.cancelFailed'))
    }
    showSuccessToast(t('order.mobile.messages.cancelSuccess'))
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.cancelFailed')))
    }
  }
}

async function handleMoveOutOrderBox(orderBoxItem: OrderBoxItem) {
  const confirmed = await confirmAction(
    t('order.mobile.actions.moveOut'),
    t('order.mobile.confirmMoveOut'),
    t('order.mobile.actions.confirmMoveOut'),
    true,
  )
  if (!confirmed) {
    return
  }

  try {
    const response = await moveOutOrderBox({ orderBoxItemId: orderBoxItem.id })
    if (!response.success) {
      throw new Error(response.message || t('order.mobile.messages.moveOutFailed'))
    }
    showSuccessToast(t('order.mobile.messages.moveOutSuccess'))
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.moveOutFailed')))
    }
  }
}

async function handleMoveToOrderBox(reservation: ReservationDTO) {
  try {
    const checkResponse = await checkCanMoveToOrderBox(reservation.id)
    if (!checkResponse.success || !checkResponse.data) {
      throw new Error(checkResponse.message || t('order.mobile.messages.eligibilityFailed'))
    }

    if (!checkResponse.data.canMove) {
      showWarningToast(
        checkResponse.data.reason || t('order.mobile.messages.onlyBookedCanMove'),
      )
      return
    }

    const confirmed = await confirmAction(
      t('order.mobile.actions.moveIn'),
      t('order.mobile.confirmMoveIn'),
      t('order.mobile.actions.confirmMoveIn'),
    )
    if (!confirmed) {
      return
    }

    const response = await moveToOrderBox({ reservationId: reservation.id })
    if (!response.success) {
      throw new Error(response.message || t('order.mobile.messages.moveInFailed'))
    }

    showSuccessToast(t('order.mobile.messages.moveInSuccess'))
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.moveInFailed')))
    }
  }
}

async function presentReservationActions(reservation: ReservationDTO, orderBoxItem: OrderBoxItem | null) {
  const buttons: Array<Record<string, unknown>> = [
    {
      text: t('order.mobile.actions.viewDetails'),
      handler: () => {
        void openReservationDetail(reservation.id)
      },
    },
    {
      text: t('order.mobile.actions.viewGuestNotes'),
      handler: () => {
        void presentReservationNotes(reservation)
      },
    },
  ]

  if (canAssignRoom(reservation)) {
    buttons.push({
      text: reservation.roomId ? t('order.assignDialog.edit') : t('order.assignDialog.start'),
      handler: () => {
        void openAssignRoom(reservation)
      },
    })
  }

  if (!orderBoxItem) {
    buttons.push({
      text: t('order.mobile.actions.moveIn'),
      handler: () => {
        void handleMoveToOrderBox(reservation)
      },
    })
  }

  if (canCheckInOrder(reservation)) {
    buttons.push({
      text: t('order.mobile.actions.checkIn'),
      handler: () => {
        void handleCheckIn(reservation)
      },
    })
  }

  if (canCheckOutOrder(reservation)) {
    buttons.push({
      text: t('order.mobile.actions.checkOut'),
      handler: () => {
        void handleCheckOut(reservation)
      },
    })
  }

  if (canCancelOrder(reservation)) {
    buttons.push({
      text: t('order.mobile.actions.cancelOrder'),
      role: 'destructive',
      handler: () => {
        void handleCancelReservation(reservation)
      },
    })
  }

  if (orderBoxItem) {
    buttons.push({
      text: t('order.mobile.actions.moveOut'),
      role: 'destructive',
      handler: () => {
        void handleMoveOutOrderBox(orderBoxItem)
      },
    })
  }

  buttons.push({
    text: t('order.mobile.actions.cancel'),
    role: 'cancel',
  })

  const actionSheet = await actionSheetController.create({
    header: reservation.guestName || t('order.mobile.orderActions'),
    subHeader: reservation.orderNumber,
    buttons,
  })

  await actionSheet.present()
}

async function openReservationDetail(reservationId: number) {
  await router.push({
    name: 'OrderReservationDetail',
    params: { reservationId },
    query: {
      defaultHref: buildOrdersDefaultHref(),
      fromTab: activeTab.value,
      keyword: committedKeyword.value.trim(),
      filtersSnapshot: serializeFiltersSnapshot(filters.value),
    },
  })
}

function resetAssignRoomState() {
  assignableRoomTypes.value = []
  assignableRooms.value = []
  selectedRoomTypeId.value = null
  selectedRoomId.value = null
}

function hasAssignedRoomSelection(reservation: ReservationDTO | null) {
  if (!reservation) {
    return false
  }

  if (!reservation.roomId) {
    return false
  }

  if (!reservation.roomTypeName) {
    return false
  }

  return true
}

function isCurrentAssignReservation(reservationId: number) {
  return selectedReservation.value?.id === reservationId
}

function matchAssignableRoomTypeId(
  reservation: ReservationDTO,
  roomTypes: AssignableRoomTypeDTO[],
) {
  if (!reservation.roomTypeName) {
    return null
  }

  for (const roomType of roomTypes) {
    if (roomType.name === reservation.roomTypeName) {
      return roomType.id
    }
  }

  return null
}

function matchAssignableRoomId(reservation: ReservationDTO, rooms: AssignableRoomDTO[]) {
  if (!reservation.roomId) {
    return null
  }

  for (const room of rooms) {
    if (room.id === reservation.roomId) {
      return room.id
    }
  }

  return null
}

async function fetchAssignableRoomTypes(reservationId: number) {
  const response = await getAssignableRooms(reservationId)
  if (!response.success || !response.data) {
    throw new Error(response.message || t('order.messages.loadAssignableRoomTypesFailed'))
  }

  return response.data.roomTypes || []
}

async function fetchAssignableRoomsByType(reservationId: number, roomTypeId: number) {
  const response = await getAssignableRooms(reservationId, roomTypeId)
  if (!response.success || !response.data) {
    throw new Error(response.message || t('order.messages.loadAssignableRoomsFailed'))
  }

  return response.data.rooms || []
}

async function initializeAssignedRoomSelection(reservation: ReservationDTO) {
  const reservationId = reservation.id

  assignLoading.value = true
  try {
    const roomTypes = await fetchAssignableRoomTypes(reservationId)
    if (!isCurrentAssignReservation(reservationId)) {
      return
    }

    assignableRoomTypes.value = roomTypes
    assignableRooms.value = []
    selectedRoomId.value = null

    const matchedRoomTypeId = matchAssignableRoomTypeId(reservation, roomTypes)
    if (!matchedRoomTypeId) {
      selectedRoomTypeId.value = null
      return
    }

    selectedRoomTypeId.value = matchedRoomTypeId

    const rooms = await fetchAssignableRoomsByType(reservationId, matchedRoomTypeId)
    if (!isCurrentAssignReservation(reservationId)) {
      return
    }

    assignableRooms.value = rooms
    selectedRoomId.value = matchAssignableRoomId(reservation, rooms)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(
        resolveWarningMessage(error, t('order.messages.loadAssignableRoomTypesFailed')),
      )
    }
  } finally {
    assignLoading.value = false
  }
}

async function refreshAssignableRoomTypes() {
  if (!selectedReservation.value) {
    return
  }

  if (hasAssignedRoomSelection(selectedReservation.value)) {
    await initializeAssignedRoomSelection(selectedReservation.value)
    return
  }

  assignLoading.value = true
  try {
    assignableRoomTypes.value = await fetchAssignableRoomTypes(selectedReservation.value.id)
    assignableRooms.value = []
    selectedRoomTypeId.value = null
    selectedRoomId.value = null
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(
        resolveWarningMessage(error, t('order.messages.loadAssignableRoomTypesFailed')),
      )
    }
  } finally {
    assignLoading.value = false
  }
}

async function handleSelectAssignableRoomType(roomTypeId: number) {
  if (!selectedReservation.value) {
    return
  }

  if (!roomTypeId) {
    selectedRoomTypeId.value = null
    selectedRoomId.value = null
    assignableRooms.value = []
    return
  }

  selectedRoomTypeId.value = roomTypeId
  selectedRoomId.value = null
  assignLoading.value = true

  try {
    assignableRooms.value = await fetchAssignableRoomsByType(selectedReservation.value.id, roomTypeId)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(
        resolveWarningMessage(error, t('order.messages.loadAssignableRoomsFailed')),
      )
    }
  } finally {
    assignLoading.value = false
  }
}

function handleSelectAssignableRoom(roomId: number) {
  selectedRoomId.value = roomId || null
}

async function openAssignRoom(reservation: ReservationDTO) {
  if (!canAssignRoom(reservation)) {
    showWarningToast(t('order.messages.unsupportedAssignStatus'))
    return
  }

  selectedReservation.value = reservation
  resetAssignRoomState()
  isAssignModalOpen.value = true
  await refreshAssignableRoomTypes()
}

function handleDismissAssignModal() {
  isAssignModalOpen.value = false
  selectedReservation.value = null
  resetAssignRoomState()
}

async function submitAssignRoom() {
  if (!selectedReservation.value || !selectedRoomId.value) {
    return
  }

  assignSubmitting.value = true
  try {
    const response = await assignReservationRoom(selectedReservation.value.id, selectedRoomId.value)
    if (!response.success) {
      throw new Error(response.message || t('order.messages.assignFailed'))
    }
    showSuccessToast(t('order.messages.assignSuccess'))
    handleDismissAssignModal()
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.messages.assignFailed')))
    }
  } finally {
    assignSubmitting.value = false
  }
}

async function handleSelectTab(tab: OrderTabValue) {
  if (activeTab.value === tab) {
    return
  }

  activeTab.value = tab
  resetPagination()
  await syncOrdersRouteContext()

  try {
    await loadOrders(true)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.switchViewFailed')))
    }
  }
}

function handleToggleSearch() {
  isSearchVisible.value = !isSearchVisible.value
  if (!isSearchVisible.value && !committedKeyword.value) {
    searchKeyword.value = ''
  }
}

function handleHideSearch() {
  isSearchVisible.value = false
  if (!committedKeyword.value) {
    searchKeyword.value = ''
  }
}

function openFilterPanel(panel: FilterPanelKey) {
  activeFilterPanel.value = panel
  isFilterModalOpen.value = true
}

async function handleApplyFilters(nextFilters: OrderFilterForm) {
  filters.value = {
    channel: [...nextFilters.channel],
    roomType: [...nextFilters.roomType],
    checkinType: nextFilters.checkinType,
    status: nextFilters.status,
    paymentStatus: nextFilters.paymentStatus,
    startDate: nextFilters.startDate,
    endDate: nextFilters.endDate,
    operationDate: nextFilters.operationDate,
  }
  isFilterModalOpen.value = false
  await syncOrdersRouteContext()

  try {
    await loadOrders(true)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.applyFiltersFailed')))
    }
  }
}

async function handleResetFilters() {
  filters.value = createDefaultOrderFilters()
  isFilterModalOpen.value = false
  await syncOrdersRouteContext()

  try {
    await loadOrders(true)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.resetFiltersFailed')))
    }
  }
}

async function handleResetSearchAndFilters() {
  searchKeyword.value = ''
  committedKeyword.value = ''
  filters.value = createDefaultOrderFilters()
  await syncOrdersRouteContext()

  try {
    await loadOrders(true)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.clearFiltersFailed')))
    }
  }
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadPage(false)
  } finally {
    event.detail.complete()
  }
}

async function handleLoadMoreButton() {
  if (!usePagedEndpoint.value || !hasMore.value || loadingMore.value) {
    return
  }

  try {
    await loadOrders(false)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.loadMoreFailed')))
    }
  }
}

async function handleInfiniteLoad(event: CustomEvent) {
  try {
    await handleLoadMoreButton()
  } finally {
    event.detail.complete()
  }
}

watch(
  () => searchKeyword.value,
  (keyword) => {
    window.clearTimeout(searchTimer)

    const nextKeyword = keyword.trim()
    if (nextKeyword === committedKeyword.value) {
      return
    }

    searchTimer = window.setTimeout(async () => {
      committedKeyword.value = nextKeyword
      await syncOrdersRouteContext()
      try {
        await loadOrders(true)
      } catch (error) {
        if (!isHandledRequestError(error)) {
          showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.searchFailed')))
        }
      }
    }, SEARCH_DEBOUNCE)
  },
)

watch(
  () => storeStore.currentStore?.id,
  async (nextStoreId, previousStoreId) => {
    if (!nextStoreId || nextStoreId === previousStoreId) {
      return
    }
    await loadPage(true)
  },
)

onIonViewWillEnter(async () => {
  applyRouteTypeSelection()
  applyRouteContextSelection()
  await loadPage(false)
})
</script>

<style scoped>
.orders-page {
  display: block;
  --background: #ffffff;
  --padding-start: 0;
  --padding-end: 0;
}

.orders-page :deep(ion-header) {
  backdrop-filter: blur(14px);
}

.orders-page :deep(ion-header::after) {
  display: none;
}

.orders-header__toolbar {
  --background: rgba(255, 255, 255, 0.94);
  --border-width: 0;
  --padding-start: 10px;
  --padding-end: 10px;
}

.orders-header__title {
  color: var(--ios-pms-header-title-color);
  font-size: var(--ios-pms-font-title-xl-size);
  font-weight: 500;
  letter-spacing: -0.04em;
}

.orders-header__icon-btn {
  --padding-start: 0;
  --padding-end: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --color: #141821;
  width: 34px;
  height: 34px;
  margin: 0;
  font-size: 21px;
}

.orders-header__icon-btn ion-icon {
  width: 21px;
  height: 21px;
}

.orders-page__shell {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 2px 0 0;
  background: #ffffff;
}

.orders-page__shell.has-search {
  padding-top: 2px;
}

.orders-page__content-bg {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
  padding: 10px 0 32px;
  background: var(--ios-pms-dashboard-page-background);
}

.orders-search-panel {
  padding: 0 14px 8px;
}

  .orders-search-panel__bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.orders-searchbar {
  flex: 1;
  min-height: 44px;
  margin: 0;
  padding: 0;
  --background: #fafafa;
  --border-radius: 22px;
  --box-shadow: none;
  --color: #666666;
  --icon-color: #666666;
  --placeholder-color: #666666;
  --placeholder-opacity: 0.72;
  --clear-button-color: #666666;
  --padding-start: 0;
  --padding-end: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --cancel-button-color: #8c909b;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.orders-searchbar::part(input) {
  min-height: 44px;
  border: 1px solid #ededed;
  border-radius: 22px;
  background: #fafafa;
  box-shadow: none;
}

.orders-search-panel__cancel {
  flex: 0 0 auto;
  border: 0;
  background: transparent;
  color: #8c909b;
  font: inherit;
  font-size: 16px;
  font-weight: 500;
  white-space: nowrap;
}

.orders-tabs-strip,
.orders-filter-row {
  position: relative;
  --orders-strip-edge: 14px;
  --orders-strip-fade-width: 14px;
}

.orders-tabs-strip::after,
.orders-filter-row::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: var(--orders-strip-fade-width);
  background: linear-gradient(90deg, rgba(247, 250, 255, 0), #f7faff 78%);
  pointer-events: none;
}

.orders-tabs-strip::after {
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), #ffffff 78%);
}

.orders-tabs-strip__scroll,
.orders-filter-row__scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-left: var(--orders-strip-edge);
  padding-right: calc(var(--orders-strip-edge) + var(--orders-strip-fade-width));
  scroll-padding-left: var(--orders-strip-edge);
  scroll-padding-right: calc(var(--orders-strip-edge) + var(--orders-strip-fade-width));
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.orders-tabs-strip__scroll::-webkit-scrollbar,
.orders-filter-row__scroll::-webkit-scrollbar {
  display: none;
}

.orders-tabs-strip__item {
  position: relative;
  flex: 0 0 auto;
  padding: 0 2px 14px;
  border: 0;
  background: transparent;
  color: #8f94a2;
  font: inherit;
  font-size: 17px;
  font-weight: 500;
  white-space: nowrap;
}

.orders-tabs-strip__item.is-active {
  color: #4a98ff;
}

.orders-tabs-strip__item.is-active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 4px;
  border-radius: 999px;
  background: #4a98ff;
}

.orders-filter-chip {
  flex: 0 0 auto;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #e9ebf1;
  border-radius: 13px;
  background: #ffffff;
  color: #5a6170;
  font: inherit;
  font-size: 15px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
  white-space: nowrap;
}

.orders-filter-chip.is-active {
  border-color: #56d0b0;
  box-shadow: inset 0 0 0 1px rgba(86, 208, 176, 0.2);
  color: #3bb596;
}

.orders-filter-chip--ghost {
  border-color: #dce5ff;
  color: #4a98ff;
}

.orders-list-section {
  flex: 1;
  padding: 0 10px 40px;
}

.orders-list-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 0 2px 14px;
}

.orders-list-header__heading {
  min-width: 0;
}

.orders-list-header__summary {
  margin: 0;
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: -0.01em;
  line-height: 1.45;
}

.orders-list-header__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.orders-list-header__tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
  border-radius: 999px;
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
}

.orders-list-header__spinner {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: #4a98ff;
  opacity: 0;
}

.orders-list-header__status {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  min-height: 20px;
  flex-shrink: 0;
}

.orders-list-header__status.is-loading .orders-list-header__spinner {
  opacity: 1;
}

.orders-list {
  display: grid;
  gap: 12px;
}

.orders-notice-text {
  margin: 0;
  padding: 0 2px 14px;
  color: var(--ios-pms-text-soft);
  font-size: 12px;
  line-height: 1.55;
}

.orders-notice-text--warning {
  color: #d99a3e;
}

.orders-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 58vh;
  padding: 28px 0 0;
}

.orders-empty-state__illustration {
  position: relative;
  width: 132px;
  height: 116px;
  margin-bottom: 18px;
}

.orders-empty-state__illustration::before {
  content: '';
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 10px;
  height: 18px;
  border-radius: 50%;
  background: rgba(17, 24, 39, 0.05);
  filter: blur(1px);
}

.orders-empty-state__box {
  position: absolute;
  inset: 0;
  border: 2px solid #eef1f6;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 249, 252, 0.72));
}

.orders-empty-state__box::before,
.orders-empty-state__box::after {
  content: '';
  position: absolute;
  top: -28px;
  width: 54px;
  height: 44px;
  border: 2px solid #eef1f6;
  background: #ffffff;
}

.orders-empty-state__box::before {
  left: 14px;
  border-right: 0;
  transform: skewX(-38deg);
  border-top-left-radius: 12px;
}

.orders-empty-state__box::after {
  right: 14px;
  border-left: 0;
  transform: skewX(38deg);
  border-top-right-radius: 12px;
}

.orders-empty {
  margin: 0;
  color: #1b1e26;
  font-size: 17px;
  font-weight: 500;
  text-align: center;
}

.orders-load-more-card {
  padding: 0 14px 12px;
}

.orders-load-more-card__button {
  --border-radius: 18px;
  --background: #f6f8fc;
  --color: #4a98ff;
  min-height: 46px;
  border-radius: 18px;
  border: 1px solid #edf1f7;
  font-weight: 500;
}
</style>
