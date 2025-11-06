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
        <el-sub-menu index="room-status">
          <template #title>
            <el-icon><House /></el-icon>
            <span>房态</span>
          </template>
          <el-menu-item index="room-status-calendar">
            <span>房态</span>
          </el-menu-item>
          <el-menu-item index="room-table">
            <span>房情表</span>
          </el-menu-item>
          <el-menu-item index="room-status-share">
            <span>房态分享</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 房价 -->
        <el-sub-menu index="room-price">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>房价</span>
          </template>
          <el-menu-item index="room-price-management">
            <span>房价管理</span>
          </el-menu-item>
          <el-menu-item index="bulk-price-change">
            <span>批量改价</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 早餐套销 -->
        <!-- <el-menu-item index="breakfast-package">
          <el-icon><Coffee /></el-icon>
          <span>早餐套销</span>
        </el-menu-item> -->

        <!-- 房务 -->
        <el-sub-menu index="housekeeping">
          <template #title>
            <el-icon><Service /></el-icon>
            <span>房务</span>
          </template>
          <el-menu-item index="housekeeping-list">
            <span>房务列表</span>
          </el-menu-item>
          <el-menu-item index="housekeeper-list">
            <span>保洁员列表</span>
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
import { House, Coffee, Service, Money } from '@element-plus/icons-vue'

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
    '/accommodation/room-price/management': 'room-price-management',
    '/accommodation/room-price/bulk-change': 'bulk-price-change',
    '/accommodation/room-table': 'room-table',
    '/accommodation/room-status-share': 'room-status-share',
    '/accommodation/room-status-table': 'room-status-table',
    '/accommodation/breakfast-package': 'breakfast-package',
    '/accommodation/housekeeping-list': 'housekeeping-list',
    '/accommodation/housekeeper-list': 'housekeeper-list',
  }

  activeMenuItem.value = routeMap[route.path] || 'room-status-calendar'
}

// 监听路由变化
watch(() => route.path, updateActiveMenuItem, { immediate: true })

// 菜单选择处理
const handleMenuSelect = (index: string) => {
  const routeMap: Record<string, string> = {
    'room-status-calendar': '/accommodation/room-status/calendar',
    'room-price-management': '/accommodation/room-price/management',
    'bulk-price-change': '/accommodation/room-price/bulk-change',
    'room-table': '/accommodation/room-table',
    'room-status-share': '/accommodation/room-status-share',
    'room-status-table': '/accommodation/room-status-table',
    'breakfast-package': '/accommodation/breakfast-package',
    'housekeeping-list': '/accommodation/housekeeping-list',
    'housekeeper-list': '/accommodation/housekeeper-list',
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
  height: 40px;
  line-height: 40px;
  padding-left: 40px;
  font-size: 13px;
  color: #666;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  padding-left: 50px;
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
