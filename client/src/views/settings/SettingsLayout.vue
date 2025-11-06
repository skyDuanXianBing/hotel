<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Setting,
  House,
  Grid,
  Management,
  DataBoard,
  Box,
  Sort,
  Notebook,
  Cpu,
  Shop,
  Bell,
  ChatDotRound,
  MagicStick,
  BrushFilled,
  Tools,
  UserFilled,
  Connection,
  Coin,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const activeCollapse = ref([
  'channel',
  'room',
  'price',
  'finance',
  'finance-settings',
  'store',
  'general',
  'cleaning',
  'auto-checkin',
  'third-party',
])

interface MenuItem {
  key: string
  label: string
  icon?: any
  path?: string
  children?: MenuItem[]
}

const menuItems: MenuItem[] = [
  {
    key: 'store',
    label: '门店设置',
    icon: Shop,
    children: [
      { key: 'store-basic-info', label: '基本信息', path: '/settings/store/basic-info' },
      { key: 'store-details', label: '门店详情', path: '/settings/store/details' },
    ],
  },
  {
    key: 'general',
    label: '通用设置',
    icon: Setting,
    children: [
      { key: 'notification-settings', label: '通知设置', path: '/settings/general/notification' },
      { key: 'channel-settings', label: '渠道设置', path: '/settings/general/channel' },
      { key: 'quick-reply', label: '快捷回复', path: '/settings/general/quick-reply' },
      { key: 'auto-message', label: '自动化消息', path: '/settings/general/auto-message' },
    ],
  },
  {
    key: 'cleaning',
    label: '保洁设置',
    icon: BrushFilled,
    children: [
      { key: 'cleaning-settings', label: '设置', path: '/settings/cleaning/settings' },
      { key: 'cleaning-supplies', label: '易耗品', path: '/settings/cleaning/supplies' },
    ],
  },
  {
    key: 'auto-checkin',
    label: '自动入住',
    icon: UserFilled,
    children: [
      { key: 'auto-checkin-settings', label: '自动入住设置', path: '/settings/auto-checkin/settings' },
    ],
  },
  {
    key: 'third-party',
    label: '第三方集成',
    icon: Connection,
    children: [
      { key: 'pricing-tools', label: '定价工具', path: '/settings/third-party/pricing-tools' },
      { key: 'payment-platforms', label: '支付平台', path: '/settings/third-party/payment-platforms' },
    ],
  },
  {
    key: 'channel',
    label: '渠道管理',
    icon: DataBoard,
    children: [{ key: 'channel-management', label: '渠道设置', path: '/settings/channel-settings' }],
  },
  {
    key: 'room',
    label: '住宿设置',
    icon: House,
    children: [
      { key: 'room-type', label: '房型管理', path: '/settings/room-type' },
      // { key: 'room-status-config', label: '房间分组设置', path: '/settings/room-status-config' },
      // { key: 'room-management', label: '房间管理', path: '/settings/room-management' },
      // { key: 'package-settings', label: '包栋设置', path: '/settings/package-settings' },
      // { key: 'queue-settings', label: '排序设置', path: '/settings/queue-settings' },
      // { key: 'booking-function', label: '订单功能设置', path: '/settings/booking-function' },
      // { key: 'automation', label: '前台自动化', path: '/settings/automation' },
    ],
  },
  // {
  //   key: 'price',
  //   label: '房价设置',
  //   icon: Management,
  //   children: [
  //     { key: 'price-management', label: '房价管理', path: '/settings/price-management' },
  //     { key: 'bulk-price-change', label: '批量改价', path: '/settings/bulk-price-change' },
  //   ],
  // },
  {
    key: 'finance',
    label: '账号管理',
    icon: Grid,
    children: [],
  },
  {
    key: 'finance-settings',
    label: '财务设置',
    icon: Box,
    children: [{ key: 'payment-methods', label: '收款方式', path: '/settings/payment-methods' }],
  },
]

const handleMenuClick = (item: MenuItem) => {
  if (item.path) {
    router.push(item.path)
  }
}

const isActive = (path: string) => {
  return route.path === path
}

const getMenuTitle = () => {
  return route.meta?.title || '设置'
}
</script>

<template>
  <div class="settings-layout">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-section">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>收货导航</el-breadcrumb-item>
        <el-breadcrumb-item>{{ getMenuTitle() }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="settings-content">
      <!-- 左侧菜单 -->
      <aside class="settings-sidebar">
        <el-collapse v-model="activeCollapse" class="settings-menu">
          <el-collapse-item v-for="item in menuItems" :key="item.key" :name="item.key">
            <template #title>
              <div class="menu-title">
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.label }}</span>
              </div>
            </template>

            <div v-if="item.children && item.children.length > 0" class="menu-children">
              <div
                v-for="child in item.children"
                :key="child.key"
                :class="['menu-item', { active: isActive(child.path!) }]"
                @click="handleMenuClick(child)"
              >
                {{ child.label }}
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </aside>

      <!-- 右侧内容区域 -->
      <main class="settings-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.settings-layout {
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

.settings-content {
  flex: 1;
  display: flex;
  min-height: 0;
}

.settings-sidebar {
  width: 200px;
  background: white;
  border-right: 1px solid #e8e8e8;
  overflow-y: auto;
}

.settings-menu {
  border: none;
}

.settings-menu :deep(.el-collapse-item__header) {
  height: auto;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.settings-menu :deep(.el-collapse-item__content) {
  padding: 0;
}

.menu-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.menu-children {
  padding: 8px 0;
}

.menu-item {
  padding: 10px 24px;
  font-size: 13px;
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
}

.settings-main {
  flex: 1;
  background: white;
  margin: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: auto;
}
</style>
