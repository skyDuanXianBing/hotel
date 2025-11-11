<template>
  <div class="statistics-sidebar">
    <!-- 收起导航 -->
    <div class="collapse-nav" @click="$router.go(-1)">
      <span class="nav-text">收起导航</span>
      <el-icon class="nav-icon"><ArrowLeft /></el-icon>
    </div>

    <!-- 侧边栏菜单 -->
    <el-menu
      class="sidebar-menu"
      :default-active="activeMenu"
      :default-openeds="['data-center']"
      @select="handleMenuSelect"
    >
      <!-- 数据中心 -->
      <el-sub-menu index="data-center">
        <template #title>
          <el-icon><TrendCharts /></el-icon>
          <span>数据中心</span>
        </template>
        <el-menu-item index="data-overview">总览</el-menu-item>
        <el-menu-item index="data-accommodation">住宿</el-menu-item>
        <el-menu-item index="data-notes">记一笔</el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, TrendCharts } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

// 根据当前路由确定激活的菜单项
const activeMenu = computed(() => {
  const routeName = route.name as string
  switch (routeName) {
    // 数据中心
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

// 菜单选择处理
const handleMenuSelect = (index: string) => {
  switch (index) {
    // 数据中心
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
}

/* 收起导航样式 */
.collapse-nav {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  transition: all 0.3s;
}

.collapse-nav:hover {
  background: #f0f2f5;
}

.nav-text {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.nav-icon {
  color: #666;
  font-size: 16px;
}

/* 菜单样式 */
.sidebar-menu {
  flex: 1;
  border-right: none;
  background: #f8f9fa;
  padding: 8px 0;
}

/* 分组标题样式 */
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

/* 子菜单项样式 */
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

/* 图标样式 */
.sidebar-menu :deep(.el-icon) {
  width: 16px;
  height: 16px;
  margin-right: 8px;
}

/* 移除默认边框和阴影 */
.sidebar-menu :deep(.el-menu) {
  border: none;
}

.sidebar-menu :deep(.el-sub-menu__title),
.sidebar-menu :deep(.el-menu-item) {
  border: none !important;
}

/* 展开/收起图标 */
.sidebar-menu :deep(.el-sub-menu__icon-arrow) {
  right: 20px;
  font-size: 12px;
  color: #999;
}
</style>