<template>
  <StatisticsLayout>
    <div class="accommodation-report-container">
      <h2 class="page-title">{{ t('stage5.statistics.reports.accommodationReport') }}</h2>

      <div class="report-filter">
        <el-date-picker
          v-model="reportDateRange"
          type="daterange"
          :start-placeholder="t('stage5.common.date.startDate')"
          :end-placeholder="t('stage5.common.date.endDate')"
          :range-separator="t('stage5.common.date.rangeTo')"
          format="YYYY/MM/DD"
          value-format="YYYY-MM-DD"
          :clearable="false"
        />
      </div>

      <div class="reports-grid">
        <div
          v-for="report in reports"
          :key="report.type"
          class="report-item"
          :class="{ 'is-loading': loadingReport === report.type }"
          @click="downloadReport(report)"
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
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import StatisticsLayout from './StatisticsLayout.vue'
import {
  downloadStatisticsReport,
  getStatisticsReportErrorMessage,
  saveBlobDownload,
} from '@/api/statistics'
import { getStoreTodayYmd, getYmdMonthStart } from '@/utils/storeDateTime'

const { t } = useI18n()
const today = getStoreTodayYmd()
const startDate = ref(getYmdMonthStart(today))
const endDate = ref(today)
const loadingReport = ref('')

type ReportItem = {
  type: string
  labelKey: string
}

const reportDateRange = computed<string[]>({
  get: () => [startDate.value, endDate.value],
  set: (value: string[]) => {
    const [start, end] = value || []
    startDate.value = start || today
    endDate.value = end || today
  },
})

const reports: ReportItem[] = [
  { type: 'room-fees', labelKey: 'stage5.statistics.reports.otherRoomFeeDetail' },
]

const downloadReport = async (report: ReportItem) => {
  try {
    loadingReport.value = report.type
    const download = await downloadStatisticsReport(report.type, {
      startDate: startDate.value,
      endDate: endDate.value,
    })
    saveBlobDownload(download)
  } catch (error) {
    console.error('Failed to download accommodation report:', error)
    const message = await getStatisticsReportErrorMessage(
      error,
      t('stage5.statistics.reports.downloadFailed'),
    )
    ElMessage.error(message)
  } finally {
    loadingReport.value = ''
  }
}
</script>

<style scoped>
.accommodation-report-container {
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

.report-filter {
  display: flex;
  margin-bottom: 24px;
}

.report-filter :deep(.el-date-editor) {
  width: 320px;
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
