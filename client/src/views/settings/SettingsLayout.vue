<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import {
  Setting,
  House,
  Grid,
  Shop,
  BrushFilled,
  Connection,
  Coin,
  ArrowLeft,
  ArrowRight,
  Menu as MenuIcon,
} from '@element-plus/icons-vue'
import { PermissionAction, PermissionModule } from '@/api/role'
import {
  usePermissionStore,
  type PermissionRequirement,
} from '@/stores/permission'

const router = useRouter()
const route = useRoute()
const permissionStore = usePermissionStore()
const { t } = useI18n()

// 侧边栏折叠状态
const isCollapsed = ref(false)

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

// 菜单默认收起状态
const activeCollapse = ref<string[]>([])

interface MenuItem {
  key: string
  label: string
  icon?: any
  path?: string
  requiredPermissions?: PermissionRequirement[]
  children?: MenuItem[]
}

const storeSettingsPermission: PermissionRequirement[] = [
  { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
]

const accountSettingsPermission: PermissionRequirement[] = [
  { module: PermissionModule.SETTINGS, action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS },
]

const menuItems: MenuItem[] = [
  {
    key: 'room',
    label: 'settings.layout.groups.room',
    icon: House,
    children: [
      { key: 'room-type', label: 'settings.layout.items.roomType', path: '/settings/room-type', requiredPermissions: storeSettingsPermission },
      { key: 'price-plan', label: 'settings.layout.items.pricePlan', path: '/settings/room/price-plan', requiredPermissions: storeSettingsPermission },
      { key: 'consumption-items', label: 'settings.layout.items.consumptionItems', path: '/settings/room/consumption-items', requiredPermissions: storeSettingsPermission },
      { key: 'room-group', label: 'settings.layout.items.roomGroup', path: '/settings/room/room-group', requiredPermissions: storeSettingsPermission },
      { key: 'room-sort', label: 'settings.layout.items.roomSort', path: '/settings/room/room-sort', requiredPermissions: storeSettingsPermission },
    ],
  },
  {
    key: 'finance',
    label: 'settings.layout.groups.finance',
    icon: Coin,
    children: [
      { key: 'payment-methods', label: 'settings.layout.items.paymentMethods', path: '/settings/payment-methods', requiredPermissions: storeSettingsPermission },
      { key: 'note-settings', label: 'settings.layout.items.noteSettings', path: '/settings/finance/note-settings', requiredPermissions: storeSettingsPermission },
    ],
  },
  {
    key: 'finance-account',
    label: 'settings.layout.groups.account',
    icon: Grid,
    children: [
      { key: 'account-list', label: 'settings.layout.items.accountList', path: '/settings/account/account-list', requiredPermissions: accountSettingsPermission },
      { key: 'role-management', label: 'settings.layout.items.roleManagement', path: '/settings/account/role-management', requiredPermissions: accountSettingsPermission },
    ],
  },
  {
    key: 'store',
    label: 'settings.layout.groups.store',
    icon: Shop,
    children: [
      { key: 'store-basic-info', label: 'settings.layout.items.storeBasicInfo', path: '/settings/store/basic-info', requiredPermissions: storeSettingsPermission },
      { key: 'store-details', label: 'settings.layout.items.storeDetails', path: '/settings/store/details', requiredPermissions: storeSettingsPermission },
    ],
  },
  {
    key: 'general',
    label: 'settings.layout.groups.general',
    icon: Setting,
    children: [
      { key: 'notification-settings', label: 'settings.layout.items.notificationSettings', path: '/settings/general/notification', requiredPermissions: storeSettingsPermission },
      { key: 'channel-settings', label: 'settings.layout.items.channelSettings', path: '/settings/general/channel', requiredPermissions: storeSettingsPermission },
      { key: 'quick-reply', label: 'settings.layout.items.quickReply', path: '/settings/general/quick-reply', requiredPermissions: storeSettingsPermission },
      { key: 'auto-message', label: 'settings.layout.items.autoMessage', path: '/settings/general/auto-message', requiredPermissions: storeSettingsPermission },
    ],
  },
  {
    key: 'cleaning',
    label: 'settings.layout.groups.cleaning',
    icon: BrushFilled,
    children: [
      { key: 'cleaning-settings', label: 'settings.layout.items.cleaningSettings', path: '/settings/cleaning/settings', requiredPermissions: storeSettingsPermission },
      { key: 'cleaning-supplies', label: 'settings.layout.items.cleaningSupplies', path: '/settings/cleaning/supplies', requiredPermissions: storeSettingsPermission },
    ],
  },
  {
    key: 'third-party',
    label: 'settings.layout.groups.thirdParty',
    icon: Connection,
    children: [
      { key: 'pricing-tools', label: 'settings.layout.items.pricingTools', path: '/settings/third-party/pricing-tools', requiredPermissions: storeSettingsPermission },
      { key: 'payment-platforms', label: 'settings.layout.items.paymentPlatforms', path: '/settings/third-party/payment-platforms', requiredPermissions: storeSettingsPermission },
    ],
  },
]

const filteredMenuItems = computed(() =>
  menuItems
    .map((item) => {
      const children = item.children?.filter(
        (child) =>
          !child.requiredPermissions ||
          permissionStore.hasPermissions(child.requiredPermissions, 'all')
      )

      return {
        ...item,
        children,
      }
    })
    .filter((item) => item.children && item.children.length > 0)
)

const handleMenuClick = (item: MenuItem) => {
  if (item.path) {
    router.push(item.path)
  }
}

const isActive = (path: string) => {
  return route.path === path
}

const getMenuTitle = () => {
  return route.meta?.title || t('settings.layout.fallbackTitle')
}
</script>

<template>
  <div class="settings-layout">
    <!-- 面包屑导航 -->
    <!-- <div class="breadcrumb-section">
      <el-breadcrumb separator="/">
       
      </el-breadcrumb>
    </div> -->

    <div class="settings-content">
      <!-- 左侧菜单 -->
      <aside class="settings-sidebar" :class="{ collapsed: isCollapsed }">
        <!-- 收起导航按钮 -->
        <div class="sidebar-header" @click="toggleSidebar">
          <el-icon class="sidebar-icon"><MenuIcon /></el-icon>
          <span v-if="!isCollapsed" class="sidebar-title">{{ t('settings.layout.collapseNavigation') }}</span>
          <el-icon v-if="!isCollapsed" class="collapse-icon"><ArrowLeft /></el-icon>
          <el-icon v-else class="collapse-icon"><ArrowRight /></el-icon>
        </div>

        <el-collapse v-model="activeCollapse" class="settings-menu">
          <el-collapse-item v-for="item in filteredMenuItems" :key="item.key" :name="item.key">
            <template #title>
              <div class="menu-title">
                <el-icon><component :is="item.icon" /></el-icon>
                <span v-if="!isCollapsed">{{ t(item.label) }}</span>
              </div>
            </template>

            <div v-if="item.children && item.children.length > 0" class="menu-children">
              <div
                v-for="child in item.children"
                :key="child.key"
                :class="['menu-item', { active: isActive(child.path!) }]"
                @click="handleMenuClick(child)"
              >
                {{ t(child.label) }}
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
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
}

.settings-sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid #e8e8e8;
  cursor: pointer;
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

.settings-menu {
  flex: 1;
  border: none;
  overflow-y: auto;
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
