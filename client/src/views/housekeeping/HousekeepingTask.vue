<template>
  <div class="housekeeping-task">
    <!-- 顶部切换门店 -->
    <header class="task-header">
      <el-button class="switch-store-btn" @click="handleSwitchStore">
        切换门店
        <el-icon class="btn-icon"><DCaret /></el-icon>
      </el-button>
    </header>

    <!-- 任务卡片列表 -->
    <div class="task-container">
      <!-- 每日任务 -->
      <div class="task-card" @click="handleDailyTask">
        <div class="card-icon-wrapper">
          <el-icon class="card-icon" :size="40"><Tickets /></el-icon>
        </div>
        <div class="card-content">
          <h3 class="card-title">每日任务</h3>
          <p class="card-desc">按任务列表的格式查看、</p>
          <p class="card-desc">执行单日保洁</p>
        </div>
      </div>

      <!-- 房务房态 -->
      <div class="task-card" @click="handleRoomStatus">
        <div class="card-icon-wrapper">
          <el-icon class="card-icon" :size="40"><OfficeBuilding /></el-icon>
        </div>
        <div class="card-content">
          <h3 class="card-title">房务房态</h3>
          <p class="card-desc">按实时房态的格式查看、</p>
          <p class="card-desc">执行当日保洁</p>
        </div>
      </div>

      <!-- 任务统计 -->
      <div class="task-card" @click="handleTaskStatistics">
        <div class="card-icon-wrapper">
          <el-icon class="card-icon" :size="40"><PieChart /></el-icon>
        </div>
        <div class="card-content">
          <h3 class="card-title">任务统计</h3>
          <p class="card-desc">查看所有已打扫的保洁任务</p>
        </div>
      </div>
    </div>

    <!-- 底部水印 -->
    <footer class="task-footer">
      <div class="footer-logo">
        <el-icon :size="24"><House /></el-icon>
        <span class="footer-text"> 提供技术支持</span>
      </div>
    </footer>

    <!-- 切换门店对话框 -->
    <el-dialog v-model="showSwitchDialog" title="切换门店" width="90%" class="switch-dialog">
      <el-radio-group v-model="selectedStore" class="store-list">
        <el-radio
          v-for="store in stores"
          :key="store.id"
          :value="store.id"
          class="store-item"
          size="large"
        >
          {{ store.name }}
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="showSwitchDialog = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmSwitch">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Tickets, OfficeBuilding, PieChart, DCaret, House } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 切换门店对话框
const showSwitchDialog = ref(false)
const selectedStore = ref(1)

// 门店列表（模拟数据）
const stores = ref([
  { id: 1, name: '总店' },
  { id: 2, name: '分店A' },
  { id: 3, name: '分店B' },
])

// 切换门店
const handleSwitchStore = () => {
  showSwitchDialog.value = true
}

// 确认切换门店
const handleConfirmSwitch = () => {
  const store = stores.value.find((s) => s.id === selectedStore.value)
  ElMessage.success(`已切换到${store?.name}`)
  showSwitchDialog.value = false
}

// 每日任务
const handleDailyTask = () => {
  router.push('/housekeeping/daily-task')
}

// 房务房态
const handleRoomStatus = () => {
  ElMessage.info('跳转到房务房态页面')
  // router.push('/housekeeping/room-status')
}

// 任务统计
const handleTaskStatistics = () => {
  router.push('/housekeeping/statistics')
}
</script>

<style scoped>
.housekeeping-task {
  min-height: 100vh;
  background: linear-gradient(180deg, #f39c12 0%, #f39c12 180px, #f5f5f5 180px);
  display: flex;
  flex-direction: column;
}

/* 顶部切换门店 */
.task-header {
  padding: 20px;
  text-align: center;
}

.switch-store-btn {
  background: transparent;
  border: 2px solid white;
  color: white;
  font-size: 16px;
  font-weight: 500;
  padding: 10px 30px;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.switch-store-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.btn-icon {
  margin-left: 4px;
}

/* 任务卡片容器 */
.task-container {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}

/* 任务卡片 */
.task-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.task-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.card-icon-wrapper {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #ff9966 0%, #ff6b35 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
}

.card-icon {
  color: white;
}

.card-content {
  flex: 1;
}

.card-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px 0;
}

.card-desc {
  font-size: 14px;
  color: #666;
  margin: 2px 0;
  line-height: 1.6;
}

/* 底部水印 */
.task-footer {
  padding: 30px 20px;
  text-align: center;
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

/* 切换门店对话框 */
.switch-dialog :deep(.el-dialog__header) {
  text-align: center;
  font-weight: bold;
}

.store-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.store-item {
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.store-item:hover {
  border-color: #f39c12;
  background: #fff7e6;
}

/* 响应式设计 - 手机端 */
@media (max-width: 768px) {
  .task-header {
    padding: 16px;
  }

  .switch-store-btn {
    font-size: 15px;
    padding: 8px 24px;
  }

  .task-container {
    padding: 16px;
    gap: 16px;
  }

  .task-card {
    padding: 20px;
    gap: 16px;
  }

  .card-icon-wrapper {
    width: 60px;
    height: 60px;
  }

  .card-icon {
    font-size: 32px;
  }

  .card-title {
    font-size: 18px;
  }

  .card-desc {
    font-size: 13px;
  }

  .task-footer {
    padding: 24px 16px;
  }

  .footer-logo {
    font-size: 13px;
  }
}

/* 超小屏幕 */
@media (max-width: 480px) {
  .housekeeping-task {
    background: linear-gradient(180deg, #f39c12 0%, #f39c12 160px, #f5f5f5 160px);
  }

  .task-header {
    padding: 12px;
  }

  .switch-store-btn {
    font-size: 14px;
    padding: 6px 20px;
  }

  .task-container {
    padding: 12px;
    gap: 12px;
  }

  .task-card {
    padding: 16px;
    gap: 12px;
  }

  .card-icon-wrapper {
    width: 50px;
    height: 50px;
  }

  .card-icon {
    font-size: 28px;
  }

  .card-title {
    font-size: 16px;
    margin-bottom: 6px;
  }

  .card-desc {
    font-size: 12px;
  }

  .task-footer {
    padding: 20px 12px;
  }

  .footer-logo {
    font-size: 12px;
  }

  .footer-logo :deep(.el-icon) {
    font-size: 20px;
  }
}

/* 平板端 */
@media (min-width: 769px) and (max-width: 1024px) {
  .task-container {
    max-width: 800px;
  }

  .task-card {
    padding: 28px;
  }
}

/* 大屏幕 */
@media (min-width: 1025px) {
  .task-container {
    max-width: 1000px;
  }

  .task-card {
    padding: 32px;
  }

  .card-icon-wrapper {
    width: 80px;
    height: 80px;
  }

  .card-icon {
    font-size: 44px;
  }

  .card-title {
    font-size: 22px;
  }

  .card-desc {
    font-size: 15px;
  }
}
</style>
