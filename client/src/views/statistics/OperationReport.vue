<template>
  <StatisticsLayout>
    <div class="operation-report-container">
      <h2 class="page-title">经营报表</h2>

      <!-- 报表列表网格 -->
      <div class="reports-grid">
        <!-- 台账明细表 -->
        <div class="report-item" @click="downloadReport('ledger')">
          <div class="report-icon">
            <svg viewBox="0 0 60 80" xmlns="http://www.w3.org/2000/svg">
              <rect x="10" y="0" width="40" height="80" fill="#43A047" rx="2"/>
              <rect x="15" y="15" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="25" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="35" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="45" width="30" height="4" fill="#ffffff" opacity="0.8"/>
            </svg>
          </div>
          <div class="report-name">台账明细表</div>
        </div>

        <!-- 经理日报表 -->
        <div class="report-item" @click="downloadReport('daily')">
          <div class="report-icon">
            <svg viewBox="0 0 60 80" xmlns="http://www.w3.org/2000/svg">
              <rect x="10" y="0" width="40" height="80" fill="#43A047" rx="2"/>
              <rect x="15" y="15" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="25" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="35" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="45" width="30" height="4" fill="#ffffff" opacity="0.8"/>
            </svg>
          </div>
          <div class="report-name">经理日报表</div>
        </div>

        <!-- 权责物资业报表 -->
        <div class="report-item" @click="downloadReport('materials')">
          <div class="report-icon">
            <svg viewBox="0 0 60 80" xmlns="http://www.w3.org/2000/svg">
              <rect x="10" y="0" width="40" height="80" fill="#43A047" rx="2"/>
              <rect x="15" y="15" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="25" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="35" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="45" width="30" height="4" fill="#ffffff" opacity="0.8"/>
            </svg>
          </div>
          <div class="report-name">权责物资业报表</div>
        </div>

        <!-- 兵器类住房明细表 -->
        <div class="report-item" @click="downloadReport('housing')">
          <div class="report-icon">
            <svg viewBox="0 0 60 80" xmlns="http://www.w3.org/2000/svg">
              <rect x="10" y="0" width="40" height="80" fill="#43A047" rx="2"/>
              <rect x="15" y="15" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="25" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="35" width="30" height="4" fill="#ffffff" opacity="0.8"/>
              <rect x="15" y="45" width="30" height="4" fill="#ffffff" opacity="0.8"/>
            </svg>
          </div>
          <div class="report-name">兵器类住房明细表</div>
        </div>
      </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import StatisticsLayout from './StatisticsLayout.vue'

// 下载报表
const downloadReport = (reportType: string) => {
  const reportNames: Record<string, string> = {
    ledger: '台账明细表',
    daily: '经理日报表',
    materials: '权责物资业报表',
    housing: '兵器类住房明细表'
  }

  ElMessage.info(`正在生成${reportNames[reportType]}...`)

  // TODO: 实现实际的报表下载逻辑
  // 这里可以调用后端API生成Excel报表并下载
}
</script>

<style scoped>
.operation-report-container {
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
