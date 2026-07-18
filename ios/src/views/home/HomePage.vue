<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-title class="app-page-header__title">首页</ion-title>
        <ion-buttons slot="end">
          <ion-button class="app-page-header__text-btn" fill="clear" @click="goToStoreSelection">切换门店</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page mobile-page--dashboard home-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新" refreshing-spinner="crescent" />
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
  findHomeQuickActionDefinition,
  HOME_QUICK_ACTION_CUSTOMIZE_ITEM,
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
const homeShortcutsStore = useHomeShortcutsStore()
const storeStore = useStoreStore()
const memoStore = useMemoStore()

const dashboardLoading = ref(false)
const hasLoadedHomeContent = ref(false)
const occupancyData = ref<DailyOccupancyDTO[]>([])
const statCards = ref<HomeStatCardItem[]>(buildStatCards())
const loadNotice = ref('')
const helpCenterMoreOpen = ref(false)
const supportModalOpen = ref(false)

interface HomeHelpRouteItem extends HomeHelpCenterItem {
  path: string
  query?: Record<string, string>
}

const helpItems: HomeHelpRouteItem[] = [
  {
    key: 'operation-report',
    title: '营业统报表',
    description: '如何生成营业统计报表数据？',
    icon: barChartOutline,
    tone: 'primary',
    path: ROUTE_PATHS.statisticsOperationReport,
  },
  {
    key: 'repair-order',
    title: '订单操作',
    description: '如何修复在住订单营业？',
    icon: receiptOutline,
    tone: 'warning',
    path: ROUTE_PATHS.orders,
    query: { type: 'pending' },
  },
  {
    key: 'room-status',
    title: '房态',
    description: '如何房间单历房态设置的房型？',
    icon: bedOutline,
    tone: 'success',
    path: ROUTE_PATHS.rooms,
  },
]

/*
const legacyQuickActions: HomeQuickActionItem[] = [
  {
    key: 'orders',
    title: '住宿订单',
    description: '快速进入订单页，继续处理预抵、预离与待处理订单。',
    icon: receiptOutline,
    tone: 'warning',
  },
  {
    key: 'rooms',
    title: '房态',
    description: '查看今日可售与房间状态，衔接房态核心操作。',
    icon: bedOutline,
    tone: 'primary',
  },
  {
    key: 'channels',
    title: '渠道',
    description: '跳到渠道管理，查看连接、映射和后续操作入口。',
    icon: gitNetworkOutline,
    tone: 'secondary',
  },
  {
    key: 'statistics',
    title: '统计',
    description: '进入统计工作台，查看经营概况、报表和数据中心。',
    icon: barChartOutline,
    tone: 'primary',
  },
  {
    key: 'messages',
    title: '消息',
    description: '查看住客会话、未读消息与待处理聊天。',
    icon: chatbubblesOutline,
    tone: 'primary',
  },
  {
    key: 'system-notifications',
    title: '系统通知',
    description: '查看系统、保洁与任务相关通知。',
    icon: notificationsOutline,
    tone: 'warning',
  },
  {
    key: 'order-notifications',
    title: '订单通知',
    description: '集中处理订单类提醒与未读通知。',
    icon: receiptOutline,
    tone: 'secondary',
  },
  {
    key: 'wallet',
    title: '钱包',
    description: '查看余额、流水、提现记录与认证说明。',
    icon: walletOutline,
    tone: 'success',
  },
  {
    key: 'profile',
    title: '个人中心',
    description: '更新昵称、头像、性别并修改密码。',
    icon: personCircleOutline,
    tone: 'primary',
  },
  {
    key: 'settings',
    title: '设置',
    description: '继续进入门店、账号与业务配置的移动端入口。',
    icon: settingsOutline,
    tone: 'success',
  },
]
*/

const quickActions = computed<HomeQuickActionItem[]>(() => {
  return [...buildHomeQuickActionItems(homeShortcutsStore.visibleKeys), HOME_QUICK_ACTION_CUSTOMIZE_ITEM]
})

const memoValue = computed({
  get: () => memoStore.memoContent,
  set: (value: string) => {
    memoStore.saveMemoDebounced(value)
  },
})

const storeName = computed(() => {
  if (!storeStore.currentStore?.name) {
    return '未选择门店'
  }

  return storeStore.currentStore.name
})

const todayLabel = computed(() => {
  return `今日 ${formatBusinessDateLabel(getStoreTodayDate(), 'month-day')}`
})

const businessHint = computed(() => {
  const pendingCard = findStatCard('pending')
  if (pendingCard && pendingCard.value > 0) {
    return `当前有 ${pendingCard.value} 条待处理订单，建议优先处理异常或未完成事项。`
  }

  const arrivalCard = findStatCard('arrivals')
  if (arrivalCard && arrivalCard.value >= 5) {
    return `今日预抵较多，建议先检查排房与入住准备情况。`
  }

  const availableCard = findStatCard('available')
  if (availableCard && availableCard.value <= 3) {
    return `今日可售房量偏紧，请留意房态与订单节奏。`
  }

  return '首页已同步今日统计、入住率趋势与备忘录，适合开班后快速查看。'
})

const showLoadNotice = computed(() => {
  return Boolean(loadNotice.value)
})

function buildStatCards(statistics?: HomeStatisticsDTO): HomeStatCardItem[] {
  return [
    {
      key: 'arrivals',
      title: '今日预抵',
      value: statistics?.todayArrivals ?? 0,
      description: '进入订单页查看今日待入住订单。',
      actionLabel: '查看订单',
      tone: 'primary',
    },
    {
      key: 'departures',
      title: '今日预离',
      value: statistics?.todayDepartures ?? 0,
      description: '进入订单页查看今日待离店订单。',
      actionLabel: '查看订单',
      tone: 'warning',
    },
    {
      key: 'newOrders',
      title: '今日新办',
      value: statistics?.todayNewOrders ?? 0,
      description: '查看今日新增订单与处理进度。',
      actionLabel: '查看订单',
      tone: 'secondary',
    },
    {
      key: 'unassigned',
      title: '未排房',
      value: statistics?.unassignedOrders ?? 0,
      description: '查看尚未分配房间的订单。',
      actionLabel: '查看订单',
      tone: 'warning',
    },
    {
      key: 'available',
      title: '今日可售',
      value: statistics?.availableRooms ?? 0,
      description: '直接跳到房态页查看今日可售房量。',
      actionLabel: '查看房态',
      tone: 'success',
    },
    {
      key: 'pending',
      title: '待处理',
      value: statistics?.pendingOrders ?? 0,
      description: '集中查看仍需跟进的订单事项。',
      actionLabel: '查看订单',
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
    throw new Error(response.message || '加载今日统计失败')
  }

  statCards.value = buildStatCards(response.data)
}

async function loadOccupancy() {
  const today = getStoreTodayDate()
  const startDate = shiftBusinessDate(today, -6)

  const response = await getDailyOccupancy({
    startDate,
    endDate: today,
  })

  if (!response.success || !response.data) {
    throw new Error(response.message || '加载近 7 天入住率失败')
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

  loadNotice.value = ''

  const [statisticsResult, occupancyResult, memoResult] = await Promise.allSettled([
    loadStatistics(),
    loadOccupancy(),
    memoStore.loadMemo(forceMemo),
  ])

  const warnings: string[] = []

  if (statisticsResult.status === 'rejected') {
    statCards.value = buildStatCards()
    warnings.push(resolveWarningMessage(statisticsResult.reason, '今日统计同步失败'))
  }

  if (occupancyResult.status === 'rejected') {
    occupancyData.value = []
    warnings.push(resolveWarningMessage(occupancyResult.reason, '近 7 天入住率同步失败'))
  }

  if (memoResult.status === 'rejected') {
    warnings.push(resolveWarningMessage(memoResult.reason, '备忘录加载失败'))
  }

  if (warnings.length > 0) {
    loadNotice.value = warnings.join('；')
  }

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
  if (item.key === HOME_QUICK_ACTION_CUSTOMIZE_ITEM.key) {
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
  for (const item of helpItems) {
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
      showWarningToast(resolveWarningMessage(error, '首页刷新失败'))
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
        showWarningToast(resolveWarningMessage(error, '首页同步失败'))
      }
    }
  },
)

onIonViewWillEnter(async () => {
  try {
    await loadHomeContent(true, !hasLoadedHomeContent.value)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '首页加载失败'))
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
