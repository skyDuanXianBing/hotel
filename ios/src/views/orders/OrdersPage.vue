<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title class="mobile-toolbar-title">订单</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="isFilterModalOpen = true">筛选</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page orders-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新订单" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="orders-hero">
        <div class="orders-hero__row">
          <h1 class="orders-hero__title">订单管理</h1>
          <span class="orders-hero__badge">{{ totalElements }} 单</span>
        </div>
        <div v-if="filterCount > 0 || committedKeyword" class="orders-hero__filters">
          <span v-if="filterCount > 0" class="orders-hero__filter-tag">筛选 {{ filterCount }} 项</span>
          <span v-if="committedKeyword" class="orders-hero__filter-tag">{{ committedKeyword }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <OrderSummaryCards
          :items="summaryCards"
          :active-key="activeTab"
          :loading="summaryLoading"
          @select="handleSelectTab"
        />

        <section class="orders-toolbar">
          <ion-searchbar
            v-model="searchKeyword"
            :debounce="0"
            class="orders-searchbar"
            placeholder="搜索订单号/房号/姓名/手机号"
          />

          <ion-segment :value="activeTab" @ionChange="handlePrimaryTabChange">
            <ion-segment-button v-for="item in primaryTabs" :key="item.value" :value="item.value">
              <ion-label>{{ item.label }}</ion-label>
            </ion-segment-button>
          </ion-segment>

          <div v-if="secondaryTabs.length > 0" class="orders-secondary-tabs">
            <button
              v-for="item in secondaryTabs"
              :key="item.value"
              type="button"
              class="orders-tab-pill"
              :class="{ 'is-active': item.value === activeTab }"
              @click="handleSelectTab(item.value)"
            >
              {{ item.label }}
            </button>
          </div>

          <div class="orders-actions">
            <ion-button fill="outline" size="small" @click="isFilterModalOpen = true">
              筛选{{ filterCount > 0 ? `(${filterCount})` : '' }}
            </ion-button>
            <ion-button fill="outline" size="small" @click="handleResetSearchAndFilters">清空</ion-button>
          </div>

          <p v-if="loadNotice" class="orders-notice-text">{{ loadNotice }}</p>
        </section>

        <div v-if="activeTab === 'order-box'" class="orders-tip">
          订单盒子以 reservation 为数据源，可从详情执行移入/移出。
        </div>

        <div v-if="activeTab === 'unassigned'" class="orders-tip">
          包含未排房订单与渠道映射异常订单。
        </div>

        <div v-if="activeTab === 'deleted-rooms'" class="orders-tip orders-tip--warning">
          关联房型或房间已删除，请核对后重新排房。
        </div>

        <section class="orders-list-section">
          <div class="orders-list-header">
            <h2 class="orders-list-header__title">订单列表</h2>
            <ion-spinner v-if="loading" name="crescent" class="orders-list-header__spinner" />
          </div>

          <div v-if="orderCards.length > 0" class="orders-list">
            <OrderListCard
              v-for="item in orderCards"
              :key="item.key"
              :reservation="item.reservation"
              :active-tab="activeTab"
              :order-box-item="item.orderBoxItem"
              :show-assign-action="activeTab !== 'order-box' && canAssignRoom(item.reservation)"
              @open-detail="openReservationDetail(item.reservation.id)"
              @assign-room="openAssignRoom(item.reservation)"
              @open-actions="presentReservationActions(item.reservation, item.orderBoxItem)"
            />
          </div>

          <p v-else-if="!loading" class="orders-empty">当前条件下暂无订单</p>
        </section>

        <section v-if="usePagedEndpoint && hasMore" class="mobile-card orders-load-more-card">
          <ion-button expand="block" fill="outline" :disabled="loadingMore" @click="handleLoadMoreButton">
            {{ loadingMore ? '加载中...' : '加载更多订单' }}
          </ion-button>
        </section>
      </div>

      <ion-infinite-scroll v-if="usePagedEndpoint && hasMore" @ionInfinite="handleInfiniteLoad">
        <ion-infinite-scroll-content loading-text="加载更多订单" />
      </ion-infinite-scroll>
    </ion-content>

    <OrderFilterModal
      :is-open="isFilterModalOpen"
      :active-tab="activeTab"
      :channels="channelOptions"
      :room-types="roomTypeOptions"
      :initial-filters="filters"
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
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInfiniteScroll,
  IonInfiniteScrollContent,
  IonLabel,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSearchbar,
  IonSegment,
  IonSegmentButton,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AssignRoomModal from '@/components/order/AssignRoomModal.vue'
import OrderFilterModal from '@/components/order/OrderFilterModal.vue'
import OrderListCard from '@/components/order/OrderListCard.vue'
import OrderSummaryCards from '@/components/order/OrderSummaryCards.vue'
import {
  canAssignRoom,
  canCancelOrder,
  canCheckInOrder,
  canCheckOutOrder,
  createDefaultOrderFilters,
  buildOrderSummaryCards,
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
const roomStatusStore = useRoomStatusStore()
const storeStore = useStoreStore()

const primaryTabs = ORDER_PRIMARY_TABS
const secondaryTabs = ORDER_SECONDARY_TABS

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
const isAssignModalOpen = ref(false)
const selectedReservation = ref<ReservationDTO | null>(null)
const assignableRoomTypes = ref<AssignableRoomTypeDTO[]>([])
const assignableRooms = ref<AssignableRoomDTO[]>([])
const selectedRoomTypeId = ref<number | null>(null)
const selectedRoomId = ref<number | null>(null)
const assignLoading = ref(false)
const assignSubmitting = ref(false)

let searchTimer = 0

const storeName = computed(() => storeStore.currentStore?.name || '未选择门店')
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
  if (filters.value.channel) {
    count += 1
  }
  if (filters.value.roomType) {
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
  if (filters.value.startDate || filters.value.endDate) {
    count += 1
  }
  return count
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

const summaryCards = computed(() => buildOrderSummaryCards(statistics.value))

const orderCards = computed(() => {
  if (activeTab.value === 'order-box') {
    return orderBoxItems.value.map((item) => ({
      key: `order-box-${item.id}`,
      reservation: item.reservation,
      orderBoxItem: item,
    }))
  }

  return reservations.value.map((item) => ({
    key: `reservation-${item.id}`,
    reservation: item,
    orderBoxItem: null,
  }))
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
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

function parseFiltersSnapshot(snapshot: string) {
  const defaultFilters = createDefaultOrderFilters()
  if (!snapshot) {
    return defaultFilters
  }

  try {
    const parsed = JSON.parse(snapshot) as Partial<OrderFilterForm>
    return {
      channel: typeof parsed.channel === 'string' ? parsed.channel : '',
      roomType: typeof parsed.roomType === 'string' ? parsed.roomType : '',
      checkinType: typeof parsed.checkinType === 'string' ? parsed.checkinType : '',
      status: typeof parsed.status === 'string' ? parsed.status : '',
      paymentStatus: typeof parsed.paymentStatus === 'string' ? parsed.paymentStatus : '',
      startDate: typeof parsed.startDate === 'string' ? parsed.startDate : '',
      endDate: typeof parsed.endDate === 'string' ? parsed.endDate : '',
    }
  } catch {
    return defaultFilters
  }
}

function serializeFiltersSnapshot(currentFilters: OrderFilterForm) {
  if (
    !currentFilters.channel &&
    !currentFilters.roomType &&
    !currentFilters.checkinType &&
    !currentFilters.status &&
    !currentFilters.paymentStatus &&
    !currentFilters.startDate &&
    !currentFilters.endDate
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
    throw new Error(channelResponse.message || '加载渠道筛选项失败')
  }
  if (!roomTypeResponse.success || !roomTypeResponse.data) {
    throw new Error(roomTypeResponse.message || '加载房型筛选项失败')
  }

  channelOptions.value = channelResponse.data
    .filter((item) => item.name && item.name.trim().length > 0)
    .map((item) => ({
      label: item.name,
      value: item.name,
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
      throw new Error(response.message || '加载订单统计失败')
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
    channel: filters.value.channel || undefined,
    roomType: filters.value.roomType || undefined,
    checkinType: filters.value.checkinType || undefined,
    status: filters.value.status || undefined,
    paymentStatus: filters.value.paymentStatus || undefined,
    startDate: filters.value.startDate || undefined,
    endDate: filters.value.endDate || undefined,
    orderType: activeTab.value !== 'all' ? activeTab.value : undefined,
  }
}

async function loadOrderBoxList() {
  const response = await getOrderBoxList()
  if (!response.success || !response.data) {
    throw new Error(response.message || '加载订单盒子失败')
  }

  const nextItems: OrderBoxItem[] = []
  for (const item of response.data) {
    if (matchesReservationSearch(item.reservation, filters.value, committedKeyword.value)) {
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
        throw new Error(response.message || '加载订单列表失败')
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
      throw new Error(response.message || '加载订单列表失败')
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
      warnings.push(resolveWarningMessage(result.reason, '订单数据同步失败'))
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
        text: '取消',
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
  const confirmed = await confirmAction('办理入住', `确认办理 ${reservation.guestName} 的入住吗？`, '确认入住')
  if (!confirmed) {
    return
  }

  try {
    const response = await checkInReservation(reservation.id)
    if (!response.success) {
      throw new Error(response.message || '办理入住失败')
    }
    showSuccessToast('入住办理成功')
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '办理入住失败'))
    }
  }
}

async function handleCheckOut(reservation: ReservationDTO) {
  const confirmed = await confirmAction('办理退房', `确认办理 ${reservation.guestName} 的退房吗？`, '确认退房')
  if (!confirmed) {
    return
  }

  try {
    const response = await checkOutReservation(reservation.id)
    if (!response.success) {
      throw new Error(response.message || '办理退房失败')
    }
    showSuccessToast('退房办理成功')
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '办理退房失败'))
    }
  }
}

async function handleCancelReservation(reservation: ReservationDTO) {
  const confirmed = await confirmAction('取消订单', `确认取消 ${reservation.guestName} 的订单吗？`, '确认取消', true)
  if (!confirmed) {
    return
  }

  try {
    const response = await cancelReservation(reservation.id)
    if (!response.success) {
      throw new Error(response.message || '取消订单失败')
    }
    showSuccessToast('订单已取消')
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '取消订单失败'))
    }
  }
}

async function handleMoveOutOrderBox(orderBoxItem: OrderBoxItem) {
  const confirmed = await confirmAction('移出订单盒子', '确认将该订单移出盒子吗？', '确认移出', true)
  if (!confirmed) {
    return
  }

  try {
    const response = await moveOutOrderBox({ orderBoxItemId: orderBoxItem.id })
    if (!response.success) {
      throw new Error(response.message || '移出订单盒子失败')
    }
    showSuccessToast('已移出订单盒子')
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '移出订单盒子失败'))
    }
  }
}

async function handleMoveToOrderBox(reservation: ReservationDTO) {
  try {
    const checkResponse = await checkCanMoveToOrderBox(reservation.id)
    if (!checkResponse.success || !checkResponse.data) {
      throw new Error(checkResponse.message || '校验订单盒子资格失败')
    }

    if (!checkResponse.data.canMove) {
      showWarningToast(checkResponse.data.reason || '只有已预订的房间可以移入订单盒子')
      return
    }

    const confirmed = await confirmAction(
      '移入订单盒子',
      '移入后订单不会实际排房、不占库存，且营业数据不计入统计。确认继续吗？',
      '确认移入',
    )
    if (!confirmed) {
      return
    }

    const response = await moveToOrderBox({ reservationId: reservation.id })
    if (!response.success) {
      throw new Error(response.message || '移入订单盒子失败')
    }

    showSuccessToast('已移入订单盒子')
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '移入订单盒子失败'))
    }
  }
}

async function presentReservationActions(reservation: ReservationDTO, orderBoxItem: OrderBoxItem | null) {
  const buttons: Array<Record<string, unknown>> = [
    {
      text: '查看详情',
      handler: () => {
        void openReservationDetail(reservation.id)
      },
    },
  ]

  if (canAssignRoom(reservation)) {
    buttons.push({
      text: reservation.roomId ? '编辑排房' : '进行排房',
      handler: () => {
        void openAssignRoom(reservation)
      },
    })
  }

  if (!orderBoxItem) {
    buttons.push({
      text: '移入订单盒子',
      handler: () => {
        void handleMoveToOrderBox(reservation)
      },
    })
  }

  if (canCheckInOrder(reservation)) {
    buttons.push({
      text: '办理入住',
      handler: () => {
        void handleCheckIn(reservation)
      },
    })
  }

  if (canCheckOutOrder(reservation)) {
    buttons.push({
      text: '办理退房',
      handler: () => {
        void handleCheckOut(reservation)
      },
    })
  }

  if (canCancelOrder(reservation)) {
    buttons.push({
      text: '取消订单',
      role: 'destructive',
      handler: () => {
        void handleCancelReservation(reservation)
      },
    })
  }

  if (orderBoxItem) {
    buttons.push({
      text: '移出订单盒子',
      role: 'destructive',
      handler: () => {
        void handleMoveOutOrderBox(orderBoxItem)
      },
    })
  }

  buttons.push({
    text: '取消',
    role: 'cancel',
  })

  const actionSheet = await actionSheetController.create({
    header: reservation.guestName || '订单操作',
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
    throw new Error(response.message || '加载可用房型失败')
  }

  return response.data.roomTypes || []
}

async function fetchAssignableRoomsByType(reservationId: number, roomTypeId: number) {
  const response = await getAssignableRooms(reservationId, roomTypeId)
  if (!response.success || !response.data) {
    throw new Error(response.message || '加载可用房间失败')
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
      showWarningToast(resolveWarningMessage(error, '加载可用房型失败'))
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
      showWarningToast(resolveWarningMessage(error, '加载可用房型失败'))
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
      showWarningToast(resolveWarningMessage(error, '加载可用房间失败'))
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
    showWarningToast('当前订单状态不支持排房')
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
      throw new Error(response.message || '排房失败')
    }
    showSuccessToast('排房成功')
    handleDismissAssignModal()
    await refreshAfterMutation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '排房失败'))
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
      showWarningToast(resolveWarningMessage(error, '切换订单视图失败'))
    }
  }
}

async function handlePrimaryTabChange(event: CustomEvent) {
  const nextTab = event.detail.value as OrderTabValue
  if (!nextTab) {
    return
  }
  await handleSelectTab(nextTab)
}

async function handleApplyFilters(nextFilters: OrderFilterForm) {
  filters.value = {
    channel: nextFilters.channel,
    roomType: nextFilters.roomType,
    checkinType: nextFilters.checkinType,
    status: nextFilters.status,
    paymentStatus: nextFilters.paymentStatus,
    startDate: nextFilters.startDate,
    endDate: nextFilters.endDate,
  }
  isFilterModalOpen.value = false
  await syncOrdersRouteContext()

  try {
    await loadOrders(true)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '应用筛选失败'))
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
      showWarningToast(resolveWarningMessage(error, '重置筛选失败'))
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
      showWarningToast(resolveWarningMessage(error, '清空条件失败'))
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
      showWarningToast(resolveWarningMessage(error, '加载更多订单失败'))
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
          showWarningToast(resolveWarningMessage(error, '订单搜索失败'))
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
}

.orders-hero {
  padding: 18px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(var(--ion-color-primary-rgb), 0.08), transparent 60%),
    var(--app-surface-strong);
  box-shadow: var(--app-shadow);
}

.orders-hero__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.orders-hero__title {
  margin: 0;
  color: var(--app-heading);
  font-size: 20px;
  font-weight: 700;
}

.orders-hero__badge {
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.orders-hero__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.orders-hero__filter-tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--app-primary-soft-strong);
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.orders-toolbar {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
}

.orders-searchbar {
  --border-radius: 14px;
  --background: var(--app-surface-muted);
}

.orders-secondary-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.orders-tab-pill {
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid var(--app-border);
  background: var(--app-surface-strong);
  color: var(--app-muted);
  font-size: 13px;
  font-weight: 600;
}

.orders-tab-pill.is-active {
  border-color: var(--ion-color-primary);
  background: var(--app-primary-soft-strong);
  color: var(--ion-color-primary);
}

.orders-actions {
  display: flex;
  gap: 8px;
}

.orders-notice-text {
  margin: 0;
  color: var(--ion-color-warning);
  font-size: 12px;
}

.orders-tip {
  padding: 12px 16px;
  border-radius: 14px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 13px;
  line-height: 1.5;
}

.orders-tip--warning {
  background: var(--app-warning-soft);
  color: var(--ion-color-warning);
}

.orders-list-section {
  padding: 16px;
  padding-bottom: 80px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
}

.orders-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.orders-list-header__title {
  margin: 0;
  color: var(--app-heading);
  font-size: 16px;
  font-weight: 700;
}

.orders-list-header__spinner {
  color: var(--ion-color-primary);
}

.orders-list {
  display: grid;
  gap: 10px;
}

.orders-empty {
  margin: 0;
  padding: 24px 0;
  color: var(--app-muted);
  font-size: 13px;
  text-align: center;
}

.orders-load-more-card {
  padding-top: 12px;
}
</style>
