<template>
  <div class="task-statistics">
    <header class="task-header">
      <div class="header-left">
        <h2 class="header-title">{{ t('pages.housekeepingStatistics.title') }}</h2>
        <el-icon class="header-icon"><Calendar /></el-icon>
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          :placeholder="t('pages.housekeepingStatistics.monthPlaceholder')"
          class="date-picker"
          format="YYYY-MM"
          value-format="YYYY-MM"
        />
      </div>
      <el-button class="refresh-btn" circle @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </header>

    <div class="summary-bar">
      <p class="summary-text">
        {{
          t('pages.housekeepingStatistics.summary', {
            count: totalTasks,
            amount: formatAmount(totalAmount),
          })
        }}
      </p>
    </div>

    <div class="record-list">
      <div v-for="record in recordList" :key="record.id" class="record-card">
        <div class="record-main">
          <h3 class="record-title">{{ getRecordType(record.type) }}</h3>
          <p class="record-room">{{ getRecordRoom(record.room) }}</p>
        </div>
        <div class="record-right">
          <p class="record-date">{{ record.date }}</p>
          <p class="record-amount">{{ formatAmount(record.amount) }}</p>
        </div>
      </div>

      <el-empty
        v-if="recordList.length === 0"
        :description="t('pages.housekeepingStatistics.empty')"
      />
    </div>

    <footer class="task-footer">
      <div class="footer-logo">
        <el-icon :size="24"><House /></el-icon>
        <span class="footer-text">{{ t('pages.housekeepingStatistics.footer') }}</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Calendar, House, Refresh } from '@element-plus/icons-vue'
import { getStoreCurrentMonthYm } from '@/utils/storeDateTime'

const { t } = useI18n()

const selectedMonth = ref(getStoreCurrentMonthYm())

const recordList = ref([
  {
    id: 1,
    type: 'dusting',
    room: 'deluxeDoubleRoomA01',
    date: '2025-10-02',
    amount: 0,
  },
])

const totalTasks = computed(() => recordList.value.length)
const totalAmount = computed(() => recordList.value.reduce((sum, record) => sum + record.amount, 0))

const handleRefresh = () => {
  ElMessage.success(t('pages.housekeepingStatistics.messages.refreshSuccess'))
}

const formatAmount = (amount: number) =>
  t('pages.housekeepingStatistics.amount.currency', { value: amount.toFixed(2) })

const getRecordType = (type: string) => {
  const typeMap: Record<string, string> = {
    dusting: t('pages.housekeepingStatistics.records.dusting'),
  }
  return typeMap[type] || type
}

const getRecordRoom = (room: string) => {
  const roomMap: Record<string, string> = {
    deluxeDoubleRoomA01: t('pages.housekeepingStatistics.records.deluxeDoubleRoomA01'),
  }
  return roomMap[room] || room
}

onMounted(() => {
  console.log('task statistics mounted')
})
</script>

<style scoped>
.task-statistics {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.task-header {
  background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
}

.header-icon {
  font-size: 20px;
}

.date-picker {
  width: 140px;
}

.date-picker :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: none;
}

.date-picker :deep(.el-input__inner) {
  color: white;
}

.date-picker :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.7);
}

.refresh-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
}

.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.summary-bar {
  background: #f0f0f0;
  padding: 16px 20px;
  text-align: center;
}

.summary-text {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.record-list {
  flex: 1;
  padding: 16px 20px;
  overflow-y: auto;
}

.record-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.record-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.record-main {
  flex: 1;
}

.record-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px 0;
}

.record-room {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.record-right {
  text-align: right;
}

.record-date {
  font-size: 14px;
  color: #666;
  margin: 0 0 8px 0;
}

.record-amount {
  font-size: 16px;
  font-weight: bold;
  color: #e74c3c;
  margin: 0;
}

.task-footer {
  padding: 20px;
  text-align: center;
  background: white;
  border-top: 1px solid #f0f0f0;
}

.footer-logo {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #ccc;
  font-size: 14px;
}

.footer-text {
  color: #ccc;
}

@media (max-width: 768px) {
  .task-header {
    padding: 12px 16px;
  }

  .header-title {
    font-size: 16px;
  }

  .date-picker {
    width: 120px;
  }

  .summary-bar {
    padding: 12px 16px;
  }

  .record-list {
    padding: 12px 16px;
  }

  .record-card {
    padding: 12px;
  }

  .record-title {
    font-size: 15px;
  }

  .record-room {
    font-size: 13px;
  }

  .record-date {
    font-size: 13px;
  }

  .record-amount {
    font-size: 15px;
  }
}
</style>
