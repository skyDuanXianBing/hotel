<template>
  <div class="statistics-sidebar" :class="{ collapsed: isCollapsed }">
    <!-- Collapse navigation button -->
    <div class="sidebar-header" @click="toggleSidebar">
      <el-icon class="sidebar-icon"><MenuIcon /></el-icon>
      <span v-if="!isCollapsed" class="sidebar-title">{{ t('stage6.components.statisticsSidebar.collapse') }}</span>
      <el-icon v-if="!isCollapsed" class="collapse-icon"><ArrowLeft /></el-icon>
      <el-icon v-else class="collapse-icon"><ArrowRight /></el-icon>
    </div>

    <!-- Sidebar menu -->
    <el-menu
      class="sidebar-menu"
      :default-active="activeMenu"
      :default-openeds="['data-center']"
      :collapse="isCollapsed"
      @select="handleMenuSelect"
    >
      <!-- Data center -->
      <el-sub-menu index="data-center">
        <template #title>
          <el-icon><TrendCharts /></el-icon>
          <span>{{ t('stage6.components.statisticsSidebar.dataCenter') }}</span>
        </template>
        <el-menu-item index="data-overview">{{ t('stage6.components.statisticsSidebar.overview') }}</el-menu-item>
        <el-menu-item index="data-accommodation">{{ t('stage6.components.statisticsSidebar.accommodation') }}</el-menu-item>
        <el-menu-item index="data-notes">{{ t('stage6.components.statisticsSidebar.notes') }}</el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight, TrendCharts, Menu as MenuIcon } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

// Sidebar collapsed state
const isCollapsed = ref(false)

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

// Determine the active menu item from the current route
const activeMenu = computed(() => {
  const routeName = route.name as string
  switch (routeName) {
    // Data center
    case 'DataCenterOverview':
      return 'data-overview'
    case 'DataCenterAccommodation':
      return 'data-accommodation'
    case 'DataCenterNotes':
      return 'data-notes'
    default:
      return 'data-overview'
  }
})

// Handle menu selection
const handleMenuSelect = (index: string) => {
  switch (index) {
    // Data center
    case 'data-overview':
      router.push('/data-center/overview')
      break
    case 'data-accommodation':
      router.push('/data-center/accommodation')
      break
    case 'data-notes':
      router.push('/data-center/notes')
      break
  }
}
</script>

<style scoped>
.statistics-sidebar {
  width: 200px;
  background: #f8f9fa;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: width 0.3s ease;
}

.statistics-sidebar.collapsed {
  width: 64px;
}

/* Collapse navigation styles */
.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid #e8e8e8;
  cursor: pointer;
  background: white;
  transition: background-color 0.3s;
  flex-shrink: 0;
}

.sidebar-header:hover {
  background-color: #f5f5f5;
}

.sidebar-icon {
  font-size: 20px;
  color: #1890ff;
  flex-shrink: 0;
}

.sidebar-title {
  flex: 1;
  margin-left: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.collapse-icon {
  font-size: 16px;
  color: #999;
}

/* Menu styles */
.sidebar-menu {
  flex: 1;
  border-right: none;
  background: #f8f9fa;
  padding: 8px 0;
}

/* Group title styles */
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  padding: 0 20px !important;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  border-radius: 0;
}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

.sidebar-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: #409eff;
}

/* Submenu item styles */
.sidebar-menu :deep(.el-menu-item) {
  height: 36px;
  line-height: 36px;
  padding: 0 40px !important;
  background: transparent;
  font-size: 13px;
  color: #666;
  border-radius: 0;
  margin: 2px 0;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #e6f4ff;
  color: #409eff;
  font-weight: 500;
}

/* Icon styles */
.sidebar-menu :deep(.el-icon) {
  width: 16px;
  height: 16px;
  margin-right: 8px;
}

/* Remove default border and shadow */
.sidebar-menu :deep(.el-menu) {
  border: none;
}

.sidebar-menu :deep(.el-sub-menu__title),
.sidebar-menu :deep(.el-menu-item) {
  border: none !important;
}

/* Expand/collapse icon */
.sidebar-menu :deep(.el-sub-menu__icon-arrow) {
  right: 20px;
  font-size: 12px;
  color: #999;
}
</style>
