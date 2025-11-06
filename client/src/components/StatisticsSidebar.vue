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
      :default-openeds="['data-center', 'report-center']"
      @select="handleMenuSelect"
    >
      <!-- 数据中心 -->
      

      <!-- 报表中心 -->
      <el-sub-menu index="report-center">
        <template #title>
          <el-icon><Document /></el-icon>
          <span>报表中心</span>
        </template>
        <el-menu-item index="business-summary">营业汇总</el-menu-item>
        <!-- <el-menu-item index="revenue-summary">流水汇总</el-menu-item> -->
        <el-menu-item index="channel-summary">渠道汇总</el-menu-item>
         <el-menu-item index="notes-summary">记一笔汇总</el-menu-item>
        <!-- <el-menu-item index="customer-type-summary">客户类型汇总</el-menu-item>
        <el-menu-item index="sales-summary">销售汇总</el-menu-item>
        <el-menu-item index="revenue-collection">收款汇总</el-menu-item>
        <el-menu-item index="guest-business">客房营业汇总</el-menu-item>
        <el-menu-item index="annual-business">全年营业汇总</el-menu-item>
        <el-menu-item index="expense-detail">消费明细表</el-menu-item>
        <el-menu-item index="payment-bill">扫码缴费水表</el-menu-item> -->
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, TrendCharts, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

// 根据当前路由确定激活的菜单项
const activeMenu = computed(() => {
  const routeName = route.name as string
  switch (routeName) {
    case 'BusinessSummary':
      return 'business-summary'
    case 'ChannelSummary':
      return 'channel-summary'
    case 'NotesSummary':
      return 'notes-summary'
    case 'RevenueSummary':
      return 'revenue-summary'
    default:
      return 'business-summary'
  }
})

// 菜单选择处理
const handleMenuSelect = (index: string) => {
  switch (index) {
    case 'business-summary':
      router.push('/statistics/business-summary')
      break
    case 'channel-summary':
      router.push('/statistics/channel-summary')
      break
    case 'notes-summary':
      router.push('/statistics/notes-summary')
      break
    case 'revenue-summary':
      router.push('/statistics/revenue-summary')
      break
    case 'customer-type-summary':
      ElMessage.info('客户类型汇总功能开发中...')
      break
    case 'sales-summary':
      ElMessage.info('销售汇总功能开发中...')
      break
    default:
      ElMessage.info(`${index} 功能开发中...`)
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