<template>
  <div class="task-statistics">
    <!-- 顶部标题栏 -->
    <header class="task-header">
      <div class="header-left">
        <h2 class="header-title">任务统计</h2>
        <el-icon class="header-icon"><Calendar /></el-icon>
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          placeholder="选择月份"
          class="date-picker"
          format="YYYY-MM"
          value-format="YYYY-MM"
        />
      </div>
      <el-button class="refresh-btn" circle @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </header>

    <!-- 统计汇总 -->
    <div class="summary-bar">
      <p class="summary-text">
        共 {{ totalTasks }} 次保洁，费用 <span class="amount">¥{{ totalAmount.toFixed(2) }}</span> 元
      </p>
    </div>

    <!-- 任务记录列表 -->
    <div class="record-list">
      <div v-for="record in recordList" :key="record.id" class="record-card">
        <div class="record-main">
          <h3 class="record-title">{{ record.type }}</h3>
          <p class="record-room">{{ record.room }}</p>
        </div>
        <div class="record-right">
          <p class="record-date">{{ record.date }}</p>
          <p class="record-amount">¥{{ record.amount.toFixed(2) }}</p>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="recordList.length === 0" description="暂无记录" />
    </div>

    <!-- 底部水印 -->
    <footer class="task-footer">
      <div class="footer-logo">
        <el-icon :size="24"><House /></el-icon>
        <span class="footer-text">提供技术支持</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Calendar, Refresh, House } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 月份选择
const selectedMonth = ref(new Date().toISOString().substring(0, 7))

// 任务记录列表（模拟数据）
const recordList = ref([
  {
    id: 1,
    type: '抹尘',
    room: '大床房-a01',
    date: '2025-10-02',
    amount: 0.0,
  },
])

// 统计数据
const totalTasks = computed(() => recordList.value.length)
const totalAmount = computed(() =>
  recordList.value.reduce((sum, record) => sum + record.amount, 0)
)

// 刷新任务记录
const handleRefresh = () => {
  ElMessage.success('刷新成功')
  // TODO: 调用API刷新任务记录
}

// 初始化
onMounted(() => {
  // TODO: 加载任务记录数据
})
</script>

<style scoped>
.task-statistics {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 顶部标题栏 */
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

/* 统计汇总 */
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

.amount {
  color: #e74c3c;
  font-weight: bold;
  font-size: 16px;
}

/* 任务记录列表 */
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

/* 底部水印 */
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

/* 响应式设计 */
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
