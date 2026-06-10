<template>
  <div class="home-page">
    <div class="home-shell">
      <div class="main-layout">
        <div ref="leftContentRef" class="left-content">
          <div class="stats-cards card-surface">
            <div class="stat-card clickable" @click="goToOrdersByType('today-arrivals')">
              <div class="stat-title">{{ t('pages.home.stats.checkin') }}</div>
              <div class="stat-value">{{ todayStats.checkin }}</div>
            </div>
            <div class="stat-card clickable" @click="goToOrdersByType('today-departures')">
              <div class="stat-title">{{ t('pages.home.stats.checkout') }}</div>
              <div class="stat-value">{{ todayStats.checkout }}</div>
            </div>
            <div class="stat-card clickable" @click="goToOrdersByType('today-new')">
              <div class="stat-title">{{ t('pages.home.stats.newOrders') }}</div>
              <div class="stat-value">{{ todayStats.newOrders }}</div>
            </div>
            <div class="stat-card clickable" @click="goToOrdersByType('unassigned')">
              <div class="stat-title">{{ t('pages.home.stats.unassigned') }}</div>
              <div class="stat-value">{{ todayStats.unassigned }}</div>
            </div>
            <div class="stat-card clickable available" @click="goToRoomStatus()">
              <div class="stat-title">{{ t('pages.home.stats.available') }}</div>
              <div class="stat-value">{{ todayStats.available }}</div>
            </div>
            <div class="stat-card clickable" @click="goToOrdersByType('pending')">
              <div class="stat-title">{{ t('pages.home.stats.pending') }}</div>
              <div class="stat-value">{{ todayStats.pending }}</div>
            </div>
          </div>

          <div class="chart-section card-surface">
            <div class="chart-header">
              <h3>{{ t('pages.home.occupancyTitle') }}</h3>
            </div>
            <OccupancyChart :data="occupancyData" />
          </div>
        </div>

        <div class="right-content" :style="rightContentStyle">
          <TaskWorkbench />
        </div>
      </div>

      <div class="bottom-section">
        <div class="common-functions card-surface">
          <div class="section-header">
            <h3>{{ t('pages.home.commonFunctionsTitle') }}</h3>
            <el-button link class="more-btn">
              {{ t('pages.home.configureAction') }}
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div class="function-grid">
            <div class="function-card" @click="goToOrders">
              <div class="function-icon">
                <el-icon size="30" color="#ffc100"><Document /></el-icon>
              </div>
              <div class="function-name">{{ t('pages.home.orderCard') }}</div>
            </div>
          </div>
        </div>

        <div class="bulletin-board card-surface">
          <div class="section-header">
            <h3>{{ t('pages.home.bulletinBoardTitle') }}</h3>
            <el-button link class="more-btn">
              {{ t('pages.home.moreAction') }}
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div class="bulletin-list">
            <div
              v-for="item in bulletinItems"
              :key="item.id"
              class="bulletin-item"
              :class="`tone-${item.tone}`"
            >
              <div class="bulletin-badge">{{ item.badge }}</div>
              <div class="bulletin-content">
                <div class="bulletin-title">{{ item.title }}</div>
                <div class="bulletin-desc">{{ item.description }}</div>
              </div>
              <div class="bulletin-date">{{ item.date }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getDailyOccupancy } from '@/api/business'
import { getRoomStatusStatistics } from '@/api/roomStatus'
import OccupancyChart from '@/components/OccupancyChart.vue'
import TaskWorkbench from '@/views/home/components/TaskWorkbench.vue'
import { getRecentStoreDateRange } from '@/utils/storeDateTime'

const router = useRouter()
const { t } = useI18n()
const leftContentRef = ref<HTMLElement>()
const workbenchHeight = ref('auto')
let leftContentResizeObserver: ResizeObserver | null = null

const bulletinItems = computed(() => [
  {
    id: 'releaseDigest',
    tone: 'update',
    badge: t('pages.home.bulletinItems.releaseDigest.badge'),
    title: t('pages.home.bulletinItems.releaseDigest.title'),
    description: t('pages.home.bulletinItems.releaseDigest.description'),
    date: t('pages.home.bulletinItems.releaseDigest.date'),
  },
  {
    id: 'newFeature',
    tone: 'feature',
    badge: t('pages.home.bulletinItems.newFeature.badge'),
    title: t('pages.home.bulletinItems.newFeature.title'),
    description: t('pages.home.bulletinItems.newFeature.description'),
    date: t('pages.home.bulletinItems.newFeature.date'),
  },
  {
    id: 'bugFix',
    tone: 'fix',
    badge: t('pages.home.bulletinItems.bugFix.badge'),
    title: t('pages.home.bulletinItems.bugFix.title'),
    description: t('pages.home.bulletinItems.bugFix.description'),
    date: t('pages.home.bulletinItems.bugFix.date'),
  },
])

const todayStats = ref({
  checkin: 0,
  checkout: 0,
  newOrders: 0,
  unassigned: 0,
  available: 0,
  pending: 0,
})

const loading = ref(false)
const occupancyData = ref<Array<{ date: string; rate: number }>>([])
const rightContentStyle = computed(() => ({
  height: workbenchHeight.value,
}))

const syncWorkbenchHeight = () => {
  if (!leftContentRef.value || typeof window === 'undefined') {
    return
  }

  if (window.innerWidth <= 1280) {
    workbenchHeight.value = 'auto'
    return
  }

  workbenchHeight.value = `${leftContentRef.value.scrollHeight}px`
}

const loadOccupancyData = async () => {
  try {
    const dateRange = getRecentStoreDateRange(7)
    const response = await getDailyOccupancy({
      startDate: dateRange.start,
      endDate: dateRange.end,
    })

    if (response.success && response.data) {
      occupancyData.value = response.data.map((item) => ({
        date: item.date,
        rate: Number(item.rate),
      }))
    }
  } catch (error) {
    console.error('Failed to load occupancy data:', error)
    occupancyData.value = []
  }
}

const goToOrders = () => {
  router.push('/order')
}

const goToOrdersByType = (type: string) => {
  router.push({
    path: '/order',
    query: { type },
  })
}

const goToRoomStatus = () => {
  router.push('/room-status')
}

const fetchRoomStatusStatistics = async () => {
  loading.value = true
  try {
    const response = await getRoomStatusStatistics()
    if (response.success && response.data) {
      const stats = response.data
      todayStats.value = {
        checkin: stats.todayArrivals,
        checkout: stats.todayDepartures,
        newOrders: stats.todayNewOrders,
        unassigned: stats.unassignedOrders,
        available: stats.availableRooms,
        pending: stats.pendingOrders,
      }
    } else {
      ElMessage.error(response.message || t('pages.home.fetchStatisticsFailed'))
    }
  } catch (error) {
    console.error('Failed to load room status statistics:', error)
    ElMessage.error(t('pages.home.fetchStatisticsNetworkFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await nextTick()
  fetchRoomStatusStatistics()
  loadOccupancyData()

  syncWorkbenchHeight()
  leftContentResizeObserver = new ResizeObserver(() => {
    syncWorkbenchHeight()
  })

  if (leftContentRef.value) {
    leftContentResizeObserver.observe(leftContentRef.value)
  }

  window.addEventListener('resize', syncWorkbenchHeight)
})

onBeforeUnmount(() => {
  leftContentResizeObserver?.disconnect()
  window.removeEventListener('resize', syncWorkbenchHeight)
})
</script>

<style scoped>
.home-page {
  --home-card-border: rgba(15, 23, 42, 0.06);
  --home-card-shadow:
    0 16px 36px rgba(15, 23, 42, 0.04),
    0 4px 10px rgba(15, 23, 42, 0.03);
  --home-card-radius: 20px;
  padding: 24px 16px 32px;
  min-height: calc(100vh - 60px);
  background:
    radial-gradient(circle at top left, rgba(60, 195, 223, 0.08), transparent 24%),
    linear-gradient(180deg, #f8f8f9 0%, #f5f6f9 100%);
}

.home-shell {
  width: 100%;
  margin: 0 auto;
}

.card-surface {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid var(--home-card-border);
  border-radius: var(--home-card-radius);
  box-shadow: var(--home-card-shadow);
}

.main-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.9fr) minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.left-content,
.right-content {
  min-width: 0;
}

.left-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.right-content {
  display: flex;
  min-height: 0;
  overflow: hidden;
  width: 100%;
}

.right-content > * {
  flex: 1 1 auto;
  min-width: 0;
  width: 100%;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  padding: 14px 16px;
}

.stat-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 112px;
  padding: 18px 14px;
  text-align: center;
  border-radius: 16px;
  transition:
    transform 0.22s ease,
    background-color 0.22s ease,
    box-shadow 0.22s ease;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card.clickable:hover {
  transform: translateY(-2px);
  background: rgba(96, 165, 250, 0.06);
}

.stat-card.available:hover {
  background: rgba(34, 197, 94, 0.06);
}

.stat-title {
  font-size: 15px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.46);
  letter-spacing: 0.02em;
}

.stat-value {
  font-size: clamp(36px, 2.2vw, 42px);
  font-weight: 700;
  line-height: 1;
  color: #111111;
}

.chart-section {
  padding: 24px 26px 18px;
}

.chart-header {
  margin-bottom: 12px;
}

.chart-header h3,
.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
  color: #111111;
}

.bottom-section {
  display: grid;
  grid-template-columns: minmax(0, 0.96fr) minmax(0, 1.04fr);
  gap: 20px;
  margin-top: 20px;
  align-items: stretch;
}

.common-functions,
.bulletin-board {
  min-width: 0;
  min-height: 316px;
  padding: 22px 22px 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.more-btn {
  color: rgba(0, 0, 0, 0.42);
  font-size: 14px;
  font-weight: 500;
}

.more-btn :deep(.el-icon) {
  margin-left: 2px;
}

.function-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(128px, 140px));
  gap: 16px;
  justify-content: start;
  align-content: start;
}

.function-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
  padding: 8px 10px 10px;
  border-radius: 16px;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    background-color 0.2s ease;
}

.function-card:hover {
  transform: translateY(-2px);
  background: rgba(255, 193, 7, 0.06);
}

.function-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(180deg, #fff8dc 0%, #fff2bf 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.function-name {
  font-size: 15px;
  font-weight: 500;
  color: #151515;
  text-align: left;
}

.bulletin-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bulletin-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) auto;
  align-items: flex-start;
  column-gap: 12px;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.05);
  background: linear-gradient(180deg, #ffffff 0%, #fcfcfe 100%);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.bulletin-item:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.14);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.bulletin-badge {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  align-self: center;
  justify-self: start;
}

.bulletin-item.tone-update .bulletin-badge {
  color: #4c6fff;
  background: #eef2ff;
}

.bulletin-item.tone-feature .bulletin-badge {
  color: #24b36b;
  background: #ecfbf3;
}

.bulletin-item.tone-fix .bulletin-badge {
  color: #d79a2b;
  background: #fff6e6;
}

.bulletin-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.bulletin-title {
  min-width: 0;
  color: #18181b;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
}

.bulletin-date {
  flex: 0 0 auto;
  white-space: nowrap;
  align-self: start;
  padding-top: 1px;
  color: rgba(0, 0, 0, 0.38);
  font-size: 12px;
  line-height: 1.45;
}

.bulletin-desc {
  color: rgba(0, 0, 0, 0.42);
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 1280px) {
  .main-layout {
    grid-template-columns: 1fr;
  }

  .right-content {
    height: auto !important;
    overflow: visible;
  }

  .stats-cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .stat-card::after {
    display: none;
  }
}

@media (min-width: 1281px) and (max-width: 1520px) {
  .stats-cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .stat-card::after {
    display: none;
  }
}

@media (max-width: 1080px) {
  .bottom-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 16px 12px 24px;
  }

  .stats-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding: 12px;
  }

  .chart-section,
  .common-functions,
  .bulletin-board {
    padding: 18px 18px 20px;
  }

  .bulletin-item {
    grid-template-columns: 1fr;
    row-gap: 10px;
  }

  .bulletin-badge,
  .bulletin-date {
    align-self: start;
  }
}

@media (max-width: 480px) {
  .stats-cards,
  .function-grid {
    grid-template-columns: 1fr;
  }
}
</style>
