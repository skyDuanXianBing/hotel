<template>
  <div class="home-page">
    <!-- 主要内容区域：左边两格右边一格 -->
    <div class="main-layout">
      <!-- 左侧内容区域 -->
      <div class="left-content">
        <!-- 统计卡片区域 -->
        <div class="stats-cards">
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
          <div class="stat-card available" @click="goToRoomStatus()">
            <div class="stat-title">{{ t('pages.home.stats.available') }}</div>
            <div class="stat-value">{{ todayStats.available }}</div>
          </div>
          <div class="stat-card clickable" @click="goToOrdersByType('pending')">
            <div class="stat-title">{{ t('pages.home.stats.pending') }}</div>
            <div class="stat-value">{{ todayStats.pending }}</div>
          </div>
        </div>

        <!-- 近7天入住率图表 -->
        <div class="chart-section">
          <div class="chart-header">
            <h3>{{ t('pages.home.occupancyTitle') }}</h3>
          </div>
          <OccupancyChart :data="occupancyData" />
        </div>
      </div>

      <!-- 右侧工作台 / 今日任务列表 -->
      <div class="right-content">
        <TaskWorkbench />
      </div>
    </div>

    <!-- 底部功能区域 -->
    <div class="bottom-section">
      <!-- 常用功能 -->
      <div class="common-functions">
        <div class="section-header">
          <h3>{{ t('pages.home.commonFunctionsTitle') }}</h3>
          <el-button link class="more-btn">
            {{ t('pages.home.configureAction') }} <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="function-grid">
          <div class="function-card" @click="goToOrders">
            <div class="function-icon">
              <el-icon size="32" color="#ffd900"><Document /></el-icon>
            </div>
            <div class="function-name">{{ t('pages.home.orderCard') }}</div>
          </div>
          <!-- 可以添加更多功能卡片 -->
        </div>
      </div>

      <!-- 帮助中心 -->
      <div class="help-center">
        <div class="section-header">
          <h3>{{ t('pages.home.helpCenterTitle') }}</h3>
          <el-button link class="more-btn">
            {{ t('pages.home.moreAction') }} <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="help-grid">
          <div class="help-card">
            <div class="help-title">{{ t('pages.home.helpArticles.businessReport.title') }}</div>
            <div class="help-desc">{{ t('pages.home.helpArticles.businessReport.description') }}</div>
          </div>
          <div class="help-card">
            <div class="help-title">{{ t('pages.home.helpArticles.inHouseOrder.title') }}</div>
            <div class="help-desc">{{ t('pages.home.helpArticles.inHouseOrder.description') }}</div>
          </div>
          <div class="help-card">
            <div class="help-title">{{ t('pages.home.helpArticles.roomStatus.title') }}</div>
            <div class="help-desc">{{ t('pages.home.helpArticles.roomStatus.description') }}</div>
          </div>
          <div class="help-card">
            <div class="help-title">{{ t('pages.home.helpArticles.orderFeatures.title') }}</div>
            <div class="help-desc">{{ t('pages.home.helpArticles.orderFeatures.description') }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document, ArrowRight } from '@element-plus/icons-vue'
import { getRoomStatusStatistics, type RoomStatusStatisticsDTO } from '@/api/roomStatus'
import { getDailyOccupancy } from '@/api/business'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import OccupancyChart from '@/components/OccupancyChart.vue'
import TaskWorkbench from '@/views/home/components/TaskWorkbench.vue'

const router = useRouter()
const { t } = useI18n()

// 今日统计数据
const todayStats = ref({
  checkin: 0, // 今日预抵
  checkout: 0, // 今日预离
  newOrders: 0, // 今日新办
  unassigned: 0, // 未排房
  available: 0, // 今日可售
  pending: 0, // 待处理
})

// 加载状态
const loading = ref(false)

// 近7天入住率数据
const occupancyData = ref<Array<{ date: string; rate: number }>>([])

// 加载入住率数据
const loadOccupancyData = async () => {
  try {
    // 计算近7天日期范围
    const today = new Date()
    const sevenDaysAgo = new Date(today)
    sevenDaysAgo.setDate(today.getDate() - 6) // 包含今天共7天

    const formatDate = (date: Date) => {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }

    const response = await getDailyOccupancy({
      startDate: formatDate(sevenDaysAgo),
      endDate: formatDate(today),
    })

    if (response.success && response.data) {
      occupancyData.value = response.data.map((item) => ({
        date: item.date,
        rate: Number(item.rate),
      }))
    }
  } catch (error) {
    console.error('加载入住率数据失败:', error)
    // 失败时使用空数据
    occupancyData.value = []
  }
}

// 方法
const goToOrders = () => {
  router.push('/order')
}

// 根据类型跳转到订单页面
const goToOrdersByType = (type: string) => {
  // 跳转到订单页面，并传递过滤类型参数
  router.push({
    path: '/order',
    query: { type },
  })
}

// 跳转到房态管理页面
const goToRoomStatus = () => {
  router.push('/room-status')
}

// 获取房态统计数据
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
    console.error('获取统计数据错误:', error)
    ElMessage.error(t('pages.home.fetchStatisticsNetworkFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchRoomStatusStatistics()
  loadOccupancyData()
})
</script>

<style scoped>
.home-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}

/* 主布局 */
.main-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

/* 左侧内容区域 */
.left-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-card {
  background: transparent;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  transition:
    transform 0.2s ease,
    background-color 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card.clickable:hover {
  background: rgba(64, 133, 244, 0.05);
}

.stat-card.available {
  cursor: pointer;
}

.stat-card.available:hover {
  background: rgba(76, 175, 80, 0.05);
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
}

/* 右侧内容区域 */
.right-content {
  display: flex;
  flex-direction: column;
}

/* 图表区域 */
.chart-section {
  background: white;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chart-header {
  margin-bottom: 20px;
}

.chart-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

/* 底部功能区域 */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.common-functions,
.help-center {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.more-btn {
  color: #666;
  font-size: 14px;
}

/* 常用功能 */
.function-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}

.function-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.function-card:hover {
  background: #f8f9fa;
}

.function-icon {
  width: 60px;
  height: 60px;
  background: #fff3e0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.function-name {
  font-size: 14px;
  color: #333;
  text-align: center;
}

/* 帮助中心 */
.help-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.help-card {
  padding: 16px;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.help-card:hover {
  background: #f0f0f0;
}

.help-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.help-desc {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-layout {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .stats-cards {
    grid-template-columns: repeat(3, 1fr);
  }

  .bottom-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 16px;
  }

  .main-layout {
    gap: 12px;
  }

  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .help-grid {
    grid-template-columns: 1fr;
  }

  .function-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
