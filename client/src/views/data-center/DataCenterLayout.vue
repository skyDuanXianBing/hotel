<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { DataAnalysis } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

interface MenuItem {
  key: string
  label: string
  path: string
}

const menuItems = computed<MenuItem[]>(() => [
  { key: 'overview', label: t('stage5.dataCenter.layout.menu.overview'), path: '/data-center/overview' },
  { key: 'accommodation', label: t('stage5.dataCenter.layout.menu.accommodation'), path: '/data-center/accommodation' },
  { key: 'notes', label: t('stage5.dataCenter.layout.menu.notes'), path: '/data-center/notes' },
])

const handleMenuClick = (item: MenuItem) => {
  if (item.path) {
    router.push(item.path)
  }
}

const isActive = (path: string) => {
  return route.path === path
}

const getMenuTitle = () => {
  const current = menuItems.value.find((item) => item.path === route.path)
  return current?.label || t('stage5.dataCenter.layout.dataCenter')
}
</script>

<template>
  <div class="data-center-layout">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-section">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('stage5.dataCenter.layout.dataCenter') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ getMenuTitle() }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="data-center-content">
      <!-- 左侧菜单 -->
      <aside class="data-center-sidebar">
        <div class="sidebar-header">
          <el-icon size="20"><DataAnalysis /></el-icon>
          <span class="sidebar-title">{{ t('stage5.dataCenter.layout.dataAnalysis') }}</span>
        </div>
        <div class="menu-list">
          <div
            v-for="item in menuItems"
            :key="item.key"
            :class="['menu-item', { active: isActive(item.path) }]"
            @click="handleMenuClick(item)"
          >
            {{ item.label }}
          </div>
        </div>
      </aside>

      <!-- 右侧内容区域 -->
      <main class="data-center-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.data-center-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.breadcrumb-section {
  background: white;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
}

.data-center-content {
  flex: 1;
  display: flex;
  min-height: 0;
}

.data-center-sidebar {
  width: 200px;
  background: white;
  border-right: 1px solid #e8e8e8;
  overflow-y: auto;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.menu-list {
  padding: 8px 0;
}

.menu-item {
  padding: 12px 24px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.menu-item:hover {
  background: #f5f5f5;
  color: #1890ff;
}

.menu-item.active {
  background: #e6f7ff;
  color: #1890ff;
  border-left-color: #1890ff;
  font-weight: 500;
}

.data-center-main {
  flex: 1;
  background: white;
  margin: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: auto;
}
</style>
