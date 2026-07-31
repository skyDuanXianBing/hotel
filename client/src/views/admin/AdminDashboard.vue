<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Coin, DataAnalysis, Goods, OfficeBuilding } from '@element-plus/icons-vue'
import { getAdminDashboard, type AdminDashboardResponse } from '@/api/admin'
import { getAdminErrorMessage } from '@/utils/adminRequest'

const { t } = useI18n()

const loading = ref(true)
const dashboard = ref<AdminDashboardResponse | null>(null)

const loadDashboard = async () => {
  loading.value = true
  try {
    const response = await getAdminDashboard()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('admin.common.loadFailed'))
    }
    dashboard.value = response.data
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error))
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div v-loading="loading" class="admin-dashboard">
    <div class="page-header">
      <h2>{{ t('admin.dashboard.title') }}</h2>
      <p class="page-description">{{ t('admin.dashboard.description') }}</p>
    </div>

    <template v-if="dashboard">
      <div class="stat-cards">
        <el-card class="stat-card">
          <div class="stat-icon stat-icon--blue">
            <el-icon :size="22"><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">{{ t('admin.dashboard.totalStores') }}</p>
            <p class="stat-value">{{ dashboard.totalStores }}</p>
          </div>
        </el-card>

        <el-card class="stat-card">
          <div class="stat-icon stat-icon--green">
            <el-icon :size="22"><DataAnalysis /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">{{ t('admin.dashboard.activeSubscriptions') }}</p>
            <p class="stat-value">{{ dashboard.activeSubscriptions }}</p>
          </div>
        </el-card>

        <el-card class="stat-card">
          <div class="stat-icon stat-icon--orange">
            <el-icon :size="22"><Coin /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">{{ t('admin.dashboard.last30DaysOrderAmount') }}</p>
            <p class="stat-value">${{ dashboard.last30DaysOrderAmount }}</p>
          </div>
        </el-card>

        <el-card class="stat-card">
          <div class="stat-icon stat-icon--purple">
            <el-icon :size="22"><Goods /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">{{ t('admin.dashboard.aiQuotaUsedTotal') }}</p>
            <p class="stat-value">{{ dashboard.aiQuotaUsedTotal ?? '-' }}</p>
          </div>
        </el-card>
      </div>

      <el-card class="distribution-card">
        <template #header>
          <span>{{ t('admin.dashboard.packageDistribution') }}</span>
        </template>
        <el-table :data="dashboard.packageSubscriptionCounts" size="small">
          <el-table-column prop="packageName" :label="t('admin.dashboard.packageName')" />
          <el-table-column
            prop="count"
            :label="t('admin.dashboard.subscriptionCount')"
            width="160"
            align="right"
          />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.admin-dashboard {
  min-height: 200px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 6px;
  color: #303133;
  font-size: 18px;
}

.page-description {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  color: #fff;
}

.stat-icon--blue {
  background: #409eff;
}

.stat-icon--green {
  background: #67c23a;
}

.stat-icon--orange {
  background: #e6a23c;
}

.stat-icon--purple {
  background: #9254de;
}

.stat-label {
  margin: 0 0 4px;
  color: #909399;
  font-size: 12px;
}

.stat-value {
  margin: 0;
  color: #303133;
  font-size: 22px;
  font-weight: 700;
}

.distribution-card {
  margin-top: 8px;
}
</style>
