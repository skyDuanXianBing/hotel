<script setup lang="ts">
import { computed, inject, ref, watch, type Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
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
import AppTopNav from '@/components/layout/AppTopNav.vue'
import { appTopNavBindingsKey } from '@/components/layout/appShellContext'
import { PermissionAction, PermissionModule } from '@/api/role'
import {
  usePermissionStore,
  type PermissionMatchMode,
  type PermissionRequirement,
} from '@/stores/permission'

const router = useRouter()
const route = useRoute()
const permissionStore = usePermissionStore()
const { t } = useI18n()

const topNavBindings = inject(appTopNavBindingsKey)

if (!topNavBindings) {
  throw new Error('SettingsLayout requires top navigation bindings')
}

const SIDEBAR_STORAGE_KEY = 'settings-sidebar-collapsed'
const COLLAPSED_WIDTH = 84
const EXPANDED_WIDTH = 220

interface MenuChildItem {
  key: string
  label: string
  path: string
  activePaths?: string[]
  requiredPermissions?: PermissionRequirement[]
  permissionMatchMode?: PermissionMatchMode
}

interface MenuItem {
  key: string
  label: string
  icon: Component
  children: MenuChildItem[]
}

const storeSettingsPermission: PermissionRequirement[] = [
  { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
]

const accountSettingsPermission: PermissionRequirement[] = [
  { module: PermissionModule.SETTINGS, action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS },
]

const channelSettingsPermission: PermissionRequirement[] = [
  { module: PermissionModule.CHANNEL, action: PermissionAction.VIEW_CHANNELS },
]

const channelPriceRatioPermission: PermissionRequirement[] = [
  { module: PermissionModule.CHANNEL, action: PermissionAction.VIEW_CHANNELS },
  { module: PermissionModule.CHANNEL, action: PermissionAction.MANAGE_CHANNELS },
]

const menuItems: MenuItem[] = [
  {
    key: 'room',
    label: 'settings.layout.groups.room',
    icon: House,
    children: [
      {
        key: 'room-type',
        label: 'settings.layout.items.roomType',
        path: '/settings/room-type',
        activePaths: ['/settings/room-type', '/settings/room/ownership'],
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'price-plan',
        label: 'settings.layout.items.pricePlan',
        path: '/settings/room/price-plan',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'consumption-items',
        label: 'settings.layout.items.consumptionItems',
        path: '/settings/room/consumption-items',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'room-group',
        label: 'settings.layout.items.roomGroup',
        path: '/settings/room/room-group',
        activePaths: ['/settings/room/room-group', '/settings/room-status-config'],
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'room-sort',
        label: 'settings.layout.items.roomSort',
        path: '/settings/room/room-sort',
        activePaths: ['/settings/room/room-sort', '/settings/queue-settings'],
        requiredPermissions: storeSettingsPermission,
      },
    ],
  },
  {
    key: 'finance',
    label: 'settings.layout.groups.finance',
    icon: Coin,
    children: [
      {
        key: 'payment-methods',
        label: 'settings.layout.items.paymentMethods',
        path: '/settings/payment-methods',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'note-settings',
        label: 'settings.layout.items.noteSettings',
        path: '/settings/finance/note-settings',
        requiredPermissions: storeSettingsPermission,
      },
    ],
  },
  {
    key: 'account',
    label: 'settings.layout.groups.account',
    icon: Grid,
    children: [
      {
        key: 'account-list',
        label: 'settings.layout.items.accountList',
        path: '/settings/account/account-list',
        requiredPermissions: accountSettingsPermission,
      },
      {
        key: 'role-management',
        label: 'settings.layout.items.roleManagement',
        path: '/settings/account/role-management',
        requiredPermissions: accountSettingsPermission,
      },
    ],
  },
  {
    key: 'store',
    label: 'settings.layout.groups.store',
    icon: Shop,
    children: [
      {
        key: 'store-basic-info',
        label: 'settings.layout.items.storeBasicInfo',
        path: '/settings/store/basic-info',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'store-details',
        label: 'settings.layout.items.storeDetails',
        path: '/settings/store/details',
        requiredPermissions: storeSettingsPermission,
      },
    ],
  },
  {
    key: 'channel',
    label: 'settings.layout.items.channelSettings',
    icon: Connection,
    children: [
      {
        key: 'channel-list',
        label: 'channel.sidebar.list',
        path: '/settings/channel/list',
        activePaths: ['/settings/channel/list', '/settings/channel-settings'],
        requiredPermissions: channelSettingsPermission,
      },
      {
        key: 'channel-price-ratio',
        label: 'channel.sidebar.priceRatio',
        path: '/settings/channel/price-ratio',
        requiredPermissions: channelPriceRatioPermission,
        permissionMatchMode: 'any',
      },
    ],
  },
  {
    key: 'general',
    label: 'settings.layout.groups.general',
    icon: Setting,
    children: [
      {
        key: 'notification-settings',
        label: 'settings.layout.items.notificationSettings',
        path: '/settings/general/notification',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'announcement-settings',
        label: 'settings.layout.items.announcementSettings',
        path: '/settings/general/announcements',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'channel-settings',
        label: 'settings.layout.items.channelSettings',
        path: '/settings/general/channel',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'quick-reply',
        label: 'settings.layout.items.quickReply',
        path: '/settings/general/quick-reply',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'auto-message',
        label: 'settings.layout.items.autoMessage',
        path: '/settings/general/auto-message',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'ai-message-knowledge',
        label: 'settings.layout.items.aiMessageKnowledge',
        path: '/settings/general/ai-message-knowledge',
        requiredPermissions: storeSettingsPermission,
      },
    ],
  },
  {
    key: 'cleaning',
    label: 'settings.layout.groups.cleaning',
    icon: BrushFilled,
    children: [
      {
        key: 'cleaning-settings',
        label: 'settings.layout.items.cleaningSettings',
        path: '/settings/cleaning/settings',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'cleaning-supplies',
        label: 'settings.layout.items.cleaningSupplies',
        path: '/settings/cleaning/supplies',
        requiredPermissions: storeSettingsPermission,
      },
    ],
  },
  {
    key: 'third-party',
    label: 'settings.layout.groups.thirdParty',
    icon: Connection,
    children: [
      {
        key: 'pricing-tools',
        label: 'settings.layout.items.pricingTools',
        path: '/settings/third-party/pricing-tools',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'payment-platforms',
        label: 'settings.layout.items.paymentPlatforms',
        path: '/settings/third-party/payment-platforms',
        requiredPermissions: storeSettingsPermission,
      },
      {
        key: 'door-locks',
        label: 'settings.layout.items.doorLocks',
        path: '/settings/third-party/door-locks',
        requiredPermissions: storeSettingsPermission,
      },
    ],
  },
]

const isCollapsed = ref(
  typeof window === 'undefined'
    ? false
    : localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true',
)

const shellStyle = computed(() => ({
  '--sidebar-width': `${isCollapsed.value ? COLLAPSED_WIDTH : EXPANDED_WIDTH}px`,
}))

const normalizePath = (path: string) => path.replace(/\/+$/, '') || '/'

const childMatchesRoute = (child: MenuChildItem, routePath = route.path) => {
  const normalizedRoutePath = normalizePath(routePath)
  const activePaths = child.activePaths?.length ? child.activePaths : [child.path]

  return activePaths.some((activePath) => {
    const normalizedActivePath = normalizePath(activePath)
    return (
      normalizedRoutePath === normalizedActivePath ||
      normalizedRoutePath.startsWith(`${normalizedActivePath}/`)
    )
  })
}

const filteredMenuItems = computed(() =>
  menuItems
    .map((item) => {
      const children = item.children.filter(
        (child) =>
          !child.requiredPermissions ||
          permissionStore.hasPermissions(child.requiredPermissions, child.permissionMatchMode ?? 'all'),
      )

      return {
        ...item,
        children,
      }
    })
    .filter((item) => item.children.length > 0),
)

const activeMenuItem = computed(() =>
  filteredMenuItems.value.find((item) => item.children.some((child) => childMatchesRoute(child))),
)

const activeChildItem = computed(() =>
  activeMenuItem.value?.children.find((child) => childMatchesRoute(child)),
)

const activeParentKey = computed(() => activeMenuItem.value?.key || '')

const visibleTabs = computed(() => {
  const children = activeMenuItem.value?.children || []
  return children.length >= 2 ? children : []
})

const shouldShowTabs = computed(() => visibleTabs.value.length > 0)

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const navigateToPath = (path: string) => {
  if (normalizePath(route.path) !== normalizePath(path)) {
    router.push(path)
  }
}

const handleParentClick = (item: MenuItem) => {
  const targetPath = item.children[0]?.path
  if (targetPath) {
    navigateToPath(targetPath)
  }
}

const handleTabClick = (child: MenuChildItem) => {
  navigateToPath(child.path)
}

watch(
  isCollapsed,
  (collapsed) => {
    if (typeof window === 'undefined') return
    localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed))
  },
  { immediate: true },
)
</script>

<template>
  <div class="settings-shell" :class="{ 'is-sidebar-collapsed': isCollapsed }" :style="shellStyle">
    <aside class="settings-sidebar" :class="{ 'is-collapsed': isCollapsed }">
      <button type="button" class="sidebar-toggle" @click="toggleSidebar">
        <span class="sidebar-toggle-mark">
          <el-icon><MenuIcon /></el-icon>
        </span>
        <span v-if="!isCollapsed" class="sidebar-toggle-label">
          {{ t('settings.layout.collapseNavigation') }}
        </span>
        <el-icon v-if="!isCollapsed" class="sidebar-toggle-arrow"><ArrowLeft /></el-icon>
        <el-icon v-else class="sidebar-toggle-arrow"><ArrowRight /></el-icon>
      </button>

      <nav class="sidebar-nav" aria-label="Settings navigation">
        <button
          v-for="item in filteredMenuItems"
          :key="item.key"
          type="button"
          class="sidebar-parent"
          :class="{ 'is-active': activeParentKey === item.key }"
          :title="isCollapsed ? t(item.label) : undefined"
          @click="handleParentClick(item)"
        >
          <span class="sidebar-parent-icon">
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
          </span>
          <span v-if="!isCollapsed" class="sidebar-parent-label">{{ t(item.label) }}</span>
        </button>
      </nav>
    </aside>

    <section class="settings-panel">
      <header class="settings-panel-header">
        <AppTopNav
          v-bind="topNavBindings.props.value"
          @store-select="topNavBindings.onStoreSelect"
          @manage-stores="topNavBindings.onManageStores"
          @menu-click="topNavBindings.onMenuClick"
          @wallet-click="topNavBindings.onWalletClick"
          @inbox-click="topNavBindings.onInboxClick"
          @support-chat="topNavBindings.onSupportChat"
          @system-notification="topNavBindings.onSystemNotification"
          @order-notification="topNavBindings.onOrderNotification"
          @profile-click="topNavBindings.onProfileClick"
          @logout="topNavBindings.onLogout"
        />
      </header>

      <main class="settings-panel-content">
        <div v-if="shouldShowTabs" class="settings-tabs-shell" :aria-label="t(activeMenuItem?.label || '')">
          <div class="settings-tabs" role="tablist">
            <button
              v-for="tab in visibleTabs"
              :key="tab.key"
              type="button"
              class="settings-tab"
              :class="{ 'is-active': activeChildItem?.key === tab.key }"
              :aria-selected="activeChildItem?.key === tab.key"
              @click="handleTabClick(tab)"
            >
              {{ t(tab.label) }}
            </button>
          </div>
        </div>

        <div class="settings-route-surface">
          <router-view />
        </div>
      </main>
    </section>
  </div>
</template>

<style scoped>
.settings-shell {
  --sidebar-width: 84px;
  height: 100%;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  background: #fafafa;
}

.settings-sidebar {
  width: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #ffffff;
  border-right: 1px solid #ecece7;
  transition: width 0.24s ease;
  overflow: hidden;
}

.sidebar-toggle {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  border: none;
  border-bottom: 1px solid #f0f0ea;
  background: #ffffff;
  color: #1f2120;
  cursor: pointer;
}

.sidebar-toggle-mark {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #2f7cf6;
  font-size: 20px;
  flex: 0 0 auto;
}

.sidebar-toggle-label {
  flex: 1;
  min-width: 0;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: #232421;
  white-space: nowrap;
}

.sidebar-toggle-arrow {
  color: #a8ada8;
  font-size: 14px;
  flex: 0 0 auto;
}

.sidebar-nav {
  flex: 1;
  min-height: 0;
  padding: 18px 0 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}

.sidebar-parent {
  position: relative;
  height: 44px;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 0 18px 0 20px;
  border: none;
  border-radius: 0;
  background: transparent;
  color: #5f645f;
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.sidebar-parent:hover {
  background: #f6f7f3;
  color: #20211d;
}

.sidebar-parent.is-active {
  background: #eef5ff;
  color: #2f7cf6;
}

.sidebar-parent-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex: 0 0 auto;
}

.sidebar-parent-label {
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.settings-sidebar.is-collapsed .sidebar-toggle,
.settings-sidebar.is-collapsed .sidebar-parent {
  justify-content: center;
  padding: 0;
}

.settings-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.settings-panel-header {
  padding: 18px 32px 14px;
  background: #f5f5f5;
  overflow: hidden;
}

.settings-panel-header :deep(.top-nav) {
  --nav-center-shift: calc(-56px + ((var(--sidebar-width) - 84px) / 6));
  --nav-right-shift: -28px;
}

.settings-panel-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
  background: #f5f5f5;
  padding: 0 24px 24px;
}

.settings-tabs-shell {
  --settings-tabs-center-shift: calc(-56px + ((var(--sidebar-width, 84px) - 84px) / 6));
  display: flex;
  justify-content: center;
  width: 100%;
  padding: 2px 0 16px;
  background: transparent;
  flex: 0 0 auto;
}

.settings-tabs {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 2px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 8px rgba(30, 30, 30, 0.04);
  transform: translateX(var(--settings-tabs-center-shift));
  transition: transform 0.24s ease;
}

.settings-tab {
  min-width: 82px;
  height: 24px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #252525;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.settings-tab:hover {
  background: #f2f2f2;
}

.settings-tab.is-active,
.settings-tab.is-active:hover {
  background: #111111;
  color: #ffffff;
}

.settings-route-surface {
  flex: 1;
  min-width: 0;
  min-height: 0;
}

@media (max-width: 1280px) {
  .settings-panel-header {
    padding-left: 24px;
    padding-right: 20px;
  }

  .settings-panel-content {
    padding-right: 20px;
    padding-bottom: 20px;
  }
}

@media (max-width: 768px) {
  .settings-shell {
    grid-template-columns: 84px minmax(0, 1fr);
  }

  .settings-sidebar {
    width: 84px;
  }

  .settings-sidebar .sidebar-toggle,
  .settings-sidebar .sidebar-parent {
    justify-content: center;
    padding: 0;
  }

  .settings-panel-content {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>
