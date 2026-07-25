<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-title class="app-page-header__title">{{ t('home.title') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button class="app-page-header__text-btn" fill="clear" @click="goToStoreSelection">
            {{ t('home.switchStore') }}
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page mobile-page--dashboard home-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="t('home.pullToRefresh')" refreshing-spinner="crescent" />
      </ion-refresher>

      <div class="home-shell">
        <section class="home-hero home-surface home-surface--hero mobile-dashboard-surface">
          <div class="home-hero__row">
            <div class="home-hero__content">
              <h1 class="home-hero__name">{{ storeName }}</h1>
              <p class="home-hero__hint">{{ businessHint }}</p>
            </div>
            <span class="home-hero__date">{{ todayLabel }}</span>
          </div>

          <p v-if="showLoadNotice" class="home-hero__notice">{{ loadNotice }}</p>
        </section>

        <div class="mobile-stack home-stack">
          <HomeStatsGrid :items="statCards" :loading="dashboardLoading" @select="handleStatSelect" />

          <HomeQuickActions :items="quickActions" @select="handleQuickActionSelect" />

          <HomeOccupancyCard :items="occupancyData" :loading="dashboardLoading" />

          <div class="home-section home-section--lists">
            <HomeHelpCenter :items="helpItems" @select="handleHelpItemSelect" @more="handleOpenHelpCenterMore" />

            <HomeMemoCard
              v-model="memoValue"
              :auto-saving="memoStore.autoSaving"
              :loading="memoStore.loading"
              :status-text="memoStore.saveStatusText"
            />
          </div>
        </div>
      </div>

      <HomeHelpCenterMoreModal
        :is-open="helpCenterMoreOpen"
        :items="helpItems"
        @dismiss="handleDismissHelpCenterMore"
        @select="handleHelpItemSelect"
        @contact="handleOpenSupportModal"
      />

      <ContactSupportModal :is-open="supportModalOpen" @dismiss="handleDismissSupportModal" />
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import {
  barChartOutline,
  bedOutline,
  receiptOutline,
} from 'ionicons/icons'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { getDailyOccupancy, getHomeStatistics, type DailyOccupancyDTO, type HomeStatisticsDTO } from '@/api/home'
import ContactSupportModal from '@/components/global/ContactSupportModal.vue'
import HomeHelpCenter, { type HomeHelpCenterItem } from '@/components/home/HomeHelpCenter.vue'
import HomeHelpCenterMoreModal from '@/components/home/HomeHelpCenterMoreModal.vue'
import HomeMemoCard from '@/components/home/HomeMemoCard.vue'
import HomeOccupancyCard from '@/components/home/HomeOccupancyCard.vue'
import HomeQuickActions from '@/components/home/HomeQuickActions.vue'
import HomeStatsGrid, { type HomeStatCardItem } from '@/components/home/HomeStatsGrid.vue'
import {
  buildHomeQuickActionItems,
  buildHomeQuickActionCustomizeItem,
  findHomeQuickActionDefinition,
  HOME_QUICK_ACTION_CUSTOMIZE_KEY,
  type HomeQuickActionItem,
} from '@/constants/homeQuickActions'
import { ROUTE_PATHS } from '@/router/guards'
import { useHomeShortcutsStore } from '@/stores/homeShortcuts'
import { useMemoStore } from '@/stores/memo'
import { useStoreStore } from '@/stores/store'
import { showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import {
  formatBusinessDateLabel,
  getStoreTodayDate,
  shiftBusinessDate,
} from '@/utils/storeBusinessDate'

const router = useRouter()
const { t } = useI18n()
const homeShortcutsStore = useHomeShortcutsStore()
const storeStore = useStoreStore()
const memoStore = useMemoStore()

const dashboardLoading = ref(false)
const hasLoadedHomeContent = ref(false)
const occupancyData = ref<DailyOccupancyDTO[]>([])
const homeStatistics = ref<HomeStatisticsDTO | undefined>()
const loadNoticeKeys = ref<string[]>([])
const helpCenterMoreOpen = ref(false)
const supportModalOpen = ref(false)

interface HomeHelpRouteItem extends HomeHelpCenterItem {
  path: string
  query?: Record<string, string>
}

const helpItems = computed<HomeHelpRouteItem[]>(() => [
  {
    key: 'operation-report',
    title: t('home.help.operation.0'),
    description: t('home.help.operation.1'),
    icon: barChartOutline,
    tone: 'primary',
    path: ROUTE_PATHS.statisticsOperationReport,
  },
  {
    key: 'repair-order',
    title: t('home.help.orders.0'),
    description: t('home.help.orders.1'),
    icon: receiptOutline,
    tone: 'warning',
    path: ROUTE_PATHS.orders,
    query: { type: 'pending' },
  },
  {
    key: 'room-status',
    title: t('home.help.roomStatus.0'),
    description: t('home.help.roomStatus.1'),
    icon: bedOutline,
    tone: 'success',
    path: ROUTE_PATHS.rooms,
  },
])

const quickActions = computed<HomeQuickActionItem[]>(() => {
  return [
    ...buildHomeQuickActionItems(homeShortcutsStore.visibleKeys, t),
    buildHomeQuickActionCustomizeItem(t),
  ]
})

const memoValue = computed({
  get: () => memoStore.memoContent,
  set: (value: string) => {
    memoStore.saveMemoDebounced(value)
  },
})

const storeName = computed(() => {
  if (!storeStore.currentStore?.name) {
    return t('home.noStore')
  }

  return storeStore.currentStore.name
})

const todayLabel = computed(() => {
  return t('home.today', { date: formatBusinessDateLabel(getStoreTodayDate(), 'month-day') })
})

const businessHint = computed(() => {
  const pendingCard = findStatCard('pending')
  if (pendingCard && pendingCard.value > 0) {
    return t('home.hint.pending', { count: pendingCard.value })
  }

  const arrivalCard = findStatCard('arrivals')
  if (arrivalCard && arrivalCard.value >= 5) {
    return t('home.hint.arrivals')
  }

  const availableCard = findStatCard('available')
  if (availableCard && availableCard.value <= 3) {
    return t('home.hint.inventory')
  }

  return t('home.hint.default')
})

const showLoadNotice = computed(() => {
  return loadNoticeKeys.value.length > 0
})

const loadNotice = computed(() =>
  loadNoticeKeys.value.map((key) => t(key)).join(t('home.noticeSeparator')),
)

const statCards = computed<HomeStatCardItem[]>(() => buildStatCards(homeStatistics.value))

function buildStatCards(statistics?: HomeStatisticsDTO): HomeStatCardItem[] {
  return [
    {
      key: 'arrivals',
      title: t('home.stat.arrivals.0'),
      value: statistics?.todayArrivals ?? 0,
      description: t('home.stat.arrivals.1'),
      actionLabel: t('home.stat.arrivals.2'),
      tone: 'primary',
    },
    {
      key: 'departures',
      title: t('home.stat.departures.0'),
      value: statistics?.todayDepartures ?? 0,
      description: t('home.stat.departures.1'),
      actionLabel: t('home.stat.departures.2'),
      tone: 'warning',
    },
    {
      key: 'newOrders',
      title: t('home.stat.newOrders.0'),
      value: statistics?.todayNewOrders ?? 0,
      description: t('home.stat.newOrders.1'),
      actionLabel: t('home.stat.newOrders.2'),
      tone: 'secondary',
    },
    {
      key: 'unassigned',
      title: t('home.stat.unassigned.0'),
      value: statistics?.unassignedOrders ?? 0,
      description: t('home.stat.unassigned.1'),
      actionLabel: t('home.stat.unassigned.2'),
      tone: 'warning',
    },
    {
      key: 'available',
      title: t('home.stat.available.0'),
      value: statistics?.availableRooms ?? 0,
      description: t('home.stat.available.1'),
      actionLabel: t('home.stat.available.2'),
      tone: 'success',
    },
    {
      key: 'pending',
      title: t('home.stat.pending.0'),
      value: statistics?.pendingOrders ?? 0,
      description: t('home.stat.pending.1'),
      actionLabel: t('home.stat.pending.2'),
      tone: 'primary',
    },
  ]
}

function findStatCard(key: string) {
  for (const item of statCards.value) {
    if (item.key === key) {
      return item
    }
  }

  return null
}

function resolveOrderQueryType(statKey: string) {
  if (statKey === 'arrivals') {
    return 'today-arrivals'
  }

  if (statKey === 'departures') {
    return 'today-departures'
  }

  if (statKey === 'newOrders') {
    return 'today-new'
  }

  if (statKey === 'unassigned') {
    return 'unassigned'
  }

  if (statKey === 'pending') {
    return 'pending'
  }

  return ''
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

async function loadStatistics() {
  const response = await getHomeStatistics()
  if (!response.success || !response.data) {
    throw new Error(response.message || t('home.error.statistics'))
  }

  homeStatistics.value = response.data
}

async function loadOccupancy() {
  const today = getStoreTodayDate()
  const startDate = shiftBusinessDate(today, -6)

  const response = await getDailyOccupancy({
    startDate,
    endDate: today,
  })

  if (!response.success || !response.data) {
    throw new Error(response.message || t('home.error.occupancy'))
  }

  const nextItems: DailyOccupancyDTO[] = []

  for (const item of response.data) {
    nextItems.push({
      ...item,
      rate: Number(item.rate),
    })
  }

  occupancyData.value = nextItems
}

async function loadHomeContent(forceMemo = false, showLoading = true) {
  if (showLoading) {
    dashboardLoading.value = true
  }

  loadNoticeKeys.value = []

  const [statisticsResult, occupancyResult, memoResult] = await Promise.allSettled([
    loadStatistics(),
    loadOccupancy(),
    memoStore.loadMemo(forceMemo),
  ])

  const warningKeys: string[] = []

  if (statisticsResult.status === 'rejected') {
    homeStatistics.value = undefined
    warningKeys.push('home.error.statisticsSync')
  }

  if (occupancyResult.status === 'rejected') {
    occupancyData.value = []
    warningKeys.push('home.error.occupancySync')
  }

  if (memoResult.status === 'rejected') {
    warningKeys.push('home.error.memo')
  }

  loadNoticeKeys.value = warningKeys

  if (showLoading) {
    dashboardLoading.value = false
  }

  if (!hasLoadedHomeContent.value) {
    hasLoadedHomeContent.value = true
  }
}

async function goToStoreSelection() {
  await router.push(ROUTE_PATHS.storeSelection)
}

async function handleStatSelect(item: HomeStatCardItem) {
  if (item.key === 'available') {
    await router.push(ROUTE_PATHS.rooms)
    return
  }

  const type = resolveOrderQueryType(item.key)
  if (!type) {
    await router.push(ROUTE_PATHS.orders)
    return
  }

  await router.push({
    path: ROUTE_PATHS.orders,
    query: { type },
  })
}

async function handleQuickActionSelect(item: HomeQuickActionItem) {
  if (item.key === HOME_QUICK_ACTION_CUSTOMIZE_KEY) {
    await router.push(ROUTE_PATHS.homeCustomize)
    return
  }

  const matchedAction = findHomeQuickActionDefinition(item.key)
  if (!matchedAction) {
    return
  }

  if (matchedAction.query) {
    await router.push({
      path: matchedAction.path,
      query: matchedAction.query,
    })
    return
  }

  await router.push(matchedAction.path)
}

function findHelpRouteItem(key: string) {
  for (const item of helpItems.value) {
    if (item.key === key) {
      return item
    }
  }

  return null
}

async function handleHelpItemSelect(item: HomeHelpCenterItem) {
  const matchedItem = findHelpRouteItem(item.key)
  if (!matchedItem) {
    return
  }

  helpCenterMoreOpen.value = false

  if (matchedItem.query) {
    await router.push({
      path: matchedItem.path,
      query: matchedItem.query,
    })
    return
  }

  await router.push(matchedItem.path)
}

function handleOpenHelpCenterMore() {
  helpCenterMoreOpen.value = true
}

function handleDismissHelpCenterMore() {
  helpCenterMoreOpen.value = false
}

function handleOpenSupportModal() {
  helpCenterMoreOpen.value = false
  supportModalOpen.value = true
}

function handleDismissSupportModal() {
  supportModalOpen.value = false
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadHomeContent(true, true)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('home.error.refresh')))
    }
  } finally {
    event.detail.complete()
  }
}

watch(
  () => storeStore.currentStore?.id,
  async (nextStoreId, previousStoreId) => {
    if (!nextStoreId) {
      return
    }

    if (previousStoreId === undefined || previousStoreId === nextStoreId) {
      return
    }

    try {
      await loadHomeContent(true, !hasLoadedHomeContent.value)
    } catch (error) {
      if (!isHandledRequestError(error)) {
        showWarningToast(resolveWarningMessage(error, t('home.error.sync')))
      }
    }
  },
)

onIonViewWillEnter(async () => {
  try {
    await loadHomeContent(true, !hasLoadedHomeContent.value)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('home.error.load')))
    }
  }
})
</script>

<style scoped>
.home-page {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
}

ion-header {
  backdrop-filter: blur(14px);
}

ion-header::after {
  display: none;
}

.app-page-header__title {
  font-size: var(--ios-pms-font-title-xl-size);
  font-weight: 500;
  letter-spacing: -0.04em;
}

.app-page-header__text-btn {
  --color: var(--ios-pms-header-control-color);
  font-size: var(--ios-pms-font-body-md-size);
  font-weight: 400;
  letter-spacing: -0.01em;
}

.mobile-toolbar-title {
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-xl-size);
  font-weight: var(--ios-pms-weight-heavy);
  letter-spacing: -0.04em;
}

.home-shell {
  position: relative;
  --home-section-gap: 14px;
  padding:
    14px
    0
    calc(var(--ion-safe-area-bottom, 0px) + 24px);
}

.home-stack,
.home-surface {
  position: relative;
  z-index: 1;
}

.home-stack {
  margin-top: var(--home-section-gap);
  gap: var(--home-section-gap);
}

.home-surface {
  position: relative;
  overflow: hidden;
  border-radius: var(--ios-pms-radius-card-sm);
}

.home-surface::before {
  display: none;
}

.home-surface--hero::after {
  display: none;
}

.home-hero {
  box-sizing: border-box;
  min-height: 120px;
  padding: 20px 16px 18px;
}

.home-hero__row {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: flex-start;
  gap: 8px;
}

.home-hero__content {
  display: contents;
  min-width: 0;
}

.home-hero__name {
  grid-column: 1;
  grid-row: 1;
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-lg-size);
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.2;
  letter-spacing: 0;
}

.home-hero__hint {
  grid-column: 1 / -1;
  grid-row: 2;
  width: 100%;
  max-width: none;
  margin: 14px 0 0;
  color: var(--ios-pms-text-muted);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home-hero__date {
  position: relative;
  z-index: 1;
  grid-column: 2;
  grid-row: 1;
  align-self: center;
  justify-self: end;
  margin-left: 4px;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  color: var(--ios-pms-text-soft);
  box-shadow: none;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0;
  white-space: nowrap;
}

.home-hero__notice {
  position: relative;
  z-index: 1;
  margin: 6px 0 0;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  color: #9a772d;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.4;
}

.home-section {
  display: grid;
  gap: 14px;
}

@media (max-width: 374px) {
  .home-shell {
    --home-section-gap: 12px;
    padding-top: 12px;
  }

  .home-hero {
    min-height: 112px;
    padding: 18px 14px 16px;
  }

  .home-hero__name {
    font-size: 19px;
  }

  .home-hero__hint {
    margin-top: 11px;
    font-size: 13px;
  }

  .home-hero__date {
    font-size: 12px;
  }
}
</style>
