<template>
  <div class="daily-task">
    <!-- 顶部标题栏 -->
    <header class="task-header">
      <div class="header-left">
        <h2 class="header-title">保洁任务</h2>
        <el-icon class="header-icon"><Calendar /></el-icon>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          class="date-picker"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
        />
      </div>
      <el-button class="refresh-btn" circle @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </header>

    <!-- 搜索和筛选 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索房间号"
        class="search-input"
        :prefix-icon="Search"
      />
      <el-button class="filter-btn" @click="handleFilter">
        <el-icon><Filter /></el-icon>
        筛选
      </el-button>
    </div>

    <!-- 统计信息 -->
    <div class="statistics-bar">
      <p class="statistics-text">
        共有 {{ totalTasks }} 个任务，已打扫 {{ completedTasks }} 个
      </p>
      <p class="statistics-text">
        已过期 {{ expiredTasks }} 个，还剩 {{ remainingTasks }} 个
      </p>
    </div>

    <!-- 任务列表 -->
    <div class="task-list">
      <div
        v-for="task in taskList"
        :key="task.id"
        class="task-card"
        @click="handleTaskClick(task)"
      >
        <div class="task-status" :class="task.status">
          {{ getStatusText(task.status) }}
        </div>
        <div class="task-content">
          <h3 class="task-type">{{ task.type }}</h3>
          <p class="task-room">{{ task.roomType }}</p>
          <p class="task-room-number">{{ task.roomNumber }}</p>
          <p class="task-time">打扫时间：{{ task.cleanTime || '-' }}</p>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="taskList.length === 0" description="暂无任务" />
    </div>

    <!-- 底部水印 -->
    <footer class="task-footer">
      <div class="footer-logo">
        <el-icon :size="24"><House /></el-icon>
        <span class="footer-text">提供技术支持</span>
      </div>
    </footer>

    <!-- 任务详情弹窗 -->
    <TaskDetailDialog
      v-model="showTaskDetail"
      :task="selectedTask"
      @refresh="handleRefresh"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Calendar, Refresh, Search, Filter, House } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TaskDetailDialog from './components/TaskDetailDialog.vue'

// 日期选择
const selectedDate = ref(new Date().toISOString().split('T')[0])

// 搜索关键词
const searchKeyword = ref('')

// 任务详情弹窗
const showTaskDetail = ref(false)
const selectedTask = ref<any>(null)

// 任务列表（模拟数据）
const taskList = ref([
  {
    id: 1,
    type: '退脏房',
    roomType: '大床房',
    roomNumber: 'a01',
    cleanTime: '',
    status: 'expired',
    duration: 0,
    note: '',
  },
])

// 统计数据
const totalTasks = computed(() => taskList.value.length)
const completedTasks = computed(
  () => taskList.value.filter((t) => t.status === 'completed').length
)
const expiredTasks = computed(() => taskList.value.filter((t) => t.status === 'expired').length)
const remainingTasks = computed(() => taskList.value.filter((t) => t.status === 'pending').length)

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    pending: '待打扫',
    completed: '已完成',
    expired: '已过期',
  }
  return statusMap[status] || status
}

// 刷新任务列表
const handleRefresh = () => {
  ElMessage.success('刷新成功')
  // TODO: 调用API刷新任务列表
}

// 筛选
const handleFilter = () => {
  ElMessage.info('筛选功能待实现')
}

// 点击任务卡片
const handleTaskClick = (task: any) => {
  selectedTask.value = task
  showTaskDetail.value = true
}

// 初始化
onMounted(() => {
  // TODO: 加载任务列表数据
})
</script>

<style scoped>
.daily-task {
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
  width: 160px;
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

/* 搜索和筛选 */
.search-bar {
  padding: 16px 20px;
  background: white;
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
}

.filter-btn {
  background: white;
  border: 1px solid #ddd;
}

/* 统计信息 */
.statistics-bar {
  background: #f0f0f0;
  padding: 12px 20px;
  text-align: center;
}

.statistics-text {
  font-size: 14px;
  color: #666;
  margin: 4px 0;
}

/* 任务列表 */
.task-list {
  flex: 1;
  padding: 16px 20px;
  overflow-y: auto;
}

.task-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.task-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.task-status {
  width: 60px;
  background: #95a5a6;
  color: white;
  font-size: 14px;
  font-weight: bold;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  writing-mode: vertical-rl;
  text-orientation: upright;
  letter-spacing: 4px;
  padding: 8px 0;
  flex-shrink: 0;
}

.task-status.pending {
  background: #3498db;
}

.task-status.completed {
  background: #27ae60;
}

.task-status.expired {
  background: #95a5a6;
}

.task-content {
  flex: 1;
}

.task-type {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px 0;
}

.task-room {
  font-size: 14px;
  color: #666;
  margin: 4px 0;
}

.task-room-number {
  font-size: 14px;
  color: #666;
  margin: 4px 0;
}

.task-time {
  font-size: 13px;
  color: #999;
  margin: 8px 0 0 0;
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
    width: 140px;
  }

  .search-bar {
    padding: 12px 16px;
  }

  .statistics-bar {
    padding: 10px 16px;
  }

  .task-list {
    padding: 12px 16px;
  }

  .task-card {
    padding: 12px;
  }

  .task-status {
    width: 50px;
    font-size: 13px;
  }
}
</style>
