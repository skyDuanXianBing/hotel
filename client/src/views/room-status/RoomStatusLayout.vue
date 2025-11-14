<template>
  <div class="room-status-layout">
    <!-- 标签页导航 -->
    <div class="tab-navigation">
      <el-tabs :model-value="activeTab" @tab-change="handleTabChange" class="room-status-tabs">
        <el-tab-pane label="日历" name="calendar" />
        <!-- <el-tab-pane label="单日" name="daily" /> -->
        <!-- <el-tab-pane label="渠道" name="channel" /> -->
      </el-tabs>
    </div>

    <!-- 子页面内容 -->
    <div class="content-area">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRoomStatusStore } from '@/stores/roomStatus'

const route = useRoute()
const router = useRouter()
const roomStatusStore = useRoomStatusStore()

// 使用 store 中的状态
const { activeTab, setActiveTab } = roomStatusStore

// 根据路由设置当前标签页
const updateActiveTabFromRoute = () => {
  const routeTabMap: Record<string, string> = {
    '/accommodation/room-status/calendar': 'calendar',
    '/accommodation/room-status/daily': 'daily',
    '/accommodation/room-status/channel': 'channel',
  }

  const newTab = routeTabMap[route.path] || 'calendar'
  setActiveTab(newTab as 'calendar' | 'daily' | 'channel')
}

// 监听路由变化，自动更新标签页
watch(() => route.path, updateActiveTabFromRoute, { immediate: true })

// 标签页切换处理
const handleTabChange = (tabName: string) => {
  // 更新 store 中的状态
  setActiveTab(tabName as 'calendar' | 'daily' | 'channel')
  
  const routeMap: Record<string, string> = {
    calendar: '/accommodation/room-status/calendar',
    daily: '/accommodation/room-status/daily',
    channel: '/accommodation/room-status/channel',
  }

  const targetRoute = routeMap[tabName]
  if (targetRoute && targetRoute !== route.path) {
    router.push(targetRoute)
  }
}
</script>

<style scoped>
.room-status-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.tab-navigation {
  background: white;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 20px;
}

.room-status-tabs {
  height: 48px;
}

.room-status-tabs :deep(.el-tabs__header) {
  margin: 0;
  border-bottom: none;
}

.room-status-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.room-status-tabs :deep(.el-tabs__item) {
  height: 48px;
  line-height: 48px;
  padding: 0 20px;
  font-size: 14px;
  color: #666;
  border: none;
}

.room-status-tabs :deep(.el-tabs__item.is-active) {
  color: #409eff;
  border-bottom: 2px solid #409eff;
}

.room-status-tabs :deep(.el-tabs__item:hover) {
  color: #409eff;
}

.room-status-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.content-area {
  flex: 1;
  overflow: auto;
}
</style>