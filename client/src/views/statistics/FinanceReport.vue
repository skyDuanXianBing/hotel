<template>
  <StatisticsLayout>
    <div class="finance-report-container">
      <h2 class="page-title">{{ t('stage5.statistics.reports.financeReport') }}</h2>

      <div class="reports-grid">
        <div
          v-for="report in reports"
          :key="report.type"
          class="report-item"
          @click="downloadReport(report.type)"
        >
          <div class="report-icon">
            <svg viewBox="0 0 60 80" xmlns="http://www.w3.org/2000/svg">
              <rect x="10" y="0" width="40" height="80" fill="#43A047" rx="2"/>
              <rect x="15" y="15" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="25" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="35" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="45" width="30" height="4" fill="#ffffff" opacity="0.8"/>
            </svg>
          </div>
          <div class="report-name">{{ t(report.labelKey) }}</div>
        </div>
      </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import StatisticsLayout from './StatisticsLayout.vue'

const { t } = useI18n()

const reports = [
  { type: 'transaction-summary', labelKey: 'stage5.statistics.reports.transactionSummary' },
  { type: 'notes-detail', labelKey: 'stage5.statistics.reports.notesDetail' },
]

const downloadReport = (reportType: string) => {
  const report = reports.find((item) => item.type === reportType)
  const name = report ? t(report.labelKey) : reportType
  ElMessage.info(t('stage5.statistics.reports.generating', { name }))

  // TODO: 实现实际的报表下载逻辑
  // 这里可以调用后端API生成Excel报表并下载
}
</script>

<style scoped>
.finance-report-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 32px 0;
}

.reports-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 24px;
  max-width: 800px;
}

.report-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.report-item:hover {
  background: #f5f7fa;
  transform: translateY(-2px);
}

.report-icon {
  width: 60px;
  height: 80px;
}

.report-icon svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.report-name {
  font-size: 14px;
  color: #606266;
  text-align: center;
  word-break: break-word;
}

.report-item:hover .report-name {
  color: #409eff;
}
</style>
