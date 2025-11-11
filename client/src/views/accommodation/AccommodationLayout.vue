<template>
  <div class="accommodation-container">
    <!-- 左侧边栏 -->
    <aside class="sidebar">
      <el-menu
        :default-active="activeMenuItem"
        class="sidebar-menu"
        @select="handleMenuSelect"
        :collapse="false"
        :unique-opened="true"
      >
        <!-- 房态 -->
        <el-menu-item index="room-status-calendar">
          <el-icon><House /></el-icon>
          <span>房态</span>
        </el-menu-item>

        <!-- 房情表 -->
        <el-menu-item index="room-table">
          <el-icon><Tickets /></el-icon>
          <span>房情表</span>
        </el-menu-item>

        <!-- 房价&房量 -->
        <el-sub-menu index="room-price">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>房价&房量</span>
          </template>
          <el-menu-item index="room-price-management">
            <span>房价管理</span>
          </el-menu-item>
          <el-menu-item index="price-change-history">
            <span>改价记录</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 早餐套销 -->
        <!-- <el-menu-item index="breakfast-package">
          <el-icon><Coffee /></el-icon>
          <span>早餐套销</span>
        </el-menu-item> -->

        <!-- 保洁 -->
        <el-sub-menu index="cleaning">
          <template #title>
            <el-icon><Service /></el-icon>
            <span>保洁</span>
          </template>
          <el-menu-item index="cleaning-overview">
            <span>任务概览</span>
          </el-menu-item>
          <el-menu-item index="cleaning-task-list">
            <span>任务列表</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <!-- 主内容区域 -->
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { House, Service, Money, Tickets } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 活动菜单项
const activeMenuItem = ref('room-status-calendar')

// 根据当前路由设置活动菜单项
const updateActiveMenuItem = () => {
  const routeMap: Record<string, string> = {
    '/accommodation/room-status/calendar': 'room-status-calendar',
    '/accommodation/room-status/daily': 'room-status-calendar',
    '/accommodation/room-status/channel': 'room-status-calendar',
    '/accommodation/room-price-management': 'room-price-management',
    '/accommodation/room-price-bulk-update': 'room-price-management',
    '/accommodation/room-price/change-history': 'price-change-history',
    '/accommodation/room-table': 'room-table',
    '/accommodation/room-status-table': 'room-status-table',
    '/accommodation/cleaning/overview': 'cleaning-overview',
    '/accommodation/cleaning/task-list': 'cleaning-task-list',
  }

  activeMenuItem.value = routeMap[route.path] || 'room-status-calendar'
}

// 监听路由变化
watch(() => route.path, updateActiveMenuItem, { immediate: true })

// 菜单选择处理
const handleMenuSelect = (index: string) => {
  const routeMap: Record<string, string> = {
    'room-status-calendar': '/accommodation/room-status/calendar',
    'room-price-management': '/accommodation/room-price-management',
    'price-change-history': '/accommodation/room-price/change-history',
    'room-table': '/accommodation/room-table',
    'room-status-table': '/accommodation/room-status-table',
    'cleaning-overview': '/accommodation/cleaning/overview',
    'cleaning-task-list': '/accommodation/cleaning/task-list',
  }

  const targetRoute = routeMap[index]
  if (targetRoute && targetRoute !== route.path) {
    router.push(targetRoute)
  }
}
</script>

<style scoped>
.accommodation-container {
  display: flex;
  height: 100%;
  background: #f5f5f5;
}

.sidebar {
  width: 200px;
  background: white;
  border-right: 1px solid #e8e8e8;
  overflow-y: auto;
}

.sidebar-menu {
  height: 100%;
  border-right: none;
}

.sidebar-menu :deep(.el-sub-menu__title) {
  height: 48px;
  line-height: 48px;
  padding-left: 20px;
  font-size: 14px;
  color: #333;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  padding-left: 20px;
  font-size: 14px;
  color: #333;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  height: 40px;
  line-height: 40px;
  padding-left: 50px;
  font-size: 13px;
  color: #666;
  background-color: #fafafa;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: #ecf5ff;
  color: #409eff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #ecf5ff;
  color: #409eff;
  border-right: 2px solid #409eff;
}

.sidebar-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: #409eff;
}

.main-content {
  flex: 1;
  overflow: auto;
  background: #f5f5f5;
}

/* 当侧边栏折叠时的样式调整 */
.sidebar-menu.el-menu--collapse {
  width: 64px;
}

.sidebar-menu.el-menu--collapse :deep(.el-sub-menu__title) {
  padding-left: 20px;
}

.sidebar-menu.el-menu--collapse :deep(.el-menu-item) {
  padding-left: 20px;
}
</style>
