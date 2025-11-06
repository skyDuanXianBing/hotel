<template>
  <div class="statistics-layout">
    <!-- 统一的侧边栏 -->
    <StatisticsSidebar />
    
    <!-- 主内容区域 -->
    <div class="main-content">
     
      <!-- 子页面内容 -->
      <div class="page-content">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatisticsSidebar from '@/components/StatisticsSidebar.vue'

const router = useRouter()
const route = useRoute()

// 根据当前路由确定激活的Tab
const activeTab = computed(() => {
  const routeName = route.name as string
  switch (routeName) {
    case 'BusinessSummary':
      return 'business-summary'
    case 'ChannelSummary':
      return 'channel-summary'
    default:
      return 'business-summary'
  }
})

// Tab导航处理
const navigateToTab = (tabName: string) => {
  switch (tabName) {
    case 'business-summary':
      router.push('/statistics/business-summary')
      break
    case 'channel-summary':
      router.push('/statistics/channel-summary')
      break
    case 'revenue-summary':
      ElMessage.info('流水汇总功能开发中...')
      break
    case 'customer-type-summary':
      ElMessage.info('客户类型汇总功能开发中...')
      break
    case 'sales-summary':
      ElMessage.info('销售汇总功能开发中...')
      break
  }
}
</script>

<style scoped>
.statistics-layout {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部Tab标签 */
.top-tabs {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 16px 20px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.tab-item {
  font-size: 16px;
  color: #666;
  cursor: pointer;
  padding: 8px 0;
  position: relative;
  transition: color 0.3s;
}

.tab-item:hover {
  color: #409eff;
}

.tab-item.active {
  color: #409eff;
  font-weight: 500;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -17px;
  height: 2px;
  background: #409eff;
}

/* 页面内容区域 */
.page-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .statistics-layout {
    flex-direction: column;
  }

  .top-tabs {
    flex-wrap: wrap;
    gap: 16px;
  }

  .page-content {
    padding: 16px;
  }
}
</style>