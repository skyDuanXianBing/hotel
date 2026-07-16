<script setup lang="ts">
import { computed, type Component } from 'vue'
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
} from '@element-plus/icons-vue'
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue'
import WorkspaceSidebar from '@/components/layout/WorkspaceSidebar.vue'
import type { WorkspaceSidebarItem } from '@/components/layout/workspace'
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
      {
        key: 'managed-operation-settlement',
        label: 'managedOperation.title',
        path: '/settings/finance/managed-operation-settlement',
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

const settingsSidebarItems = computed<WorkspaceSidebarItem[]>(() =>
  filteredMenuItems.value.map((item) => ({
    key: item.key,
    label: t(item.label),
    icon: item.icon,
  })),
)

const visibleTabs = computed(() => {
  const children = activeMenuItem.value?.children || []
  return children.length >= 2 ? children : []
})

const shouldShowTabs = computed(() => visibleTabs.value.length > 0)

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

const handleSidebarItemSelect = (key: string) => {
  const item = filteredMenuItems.value.find((menuItem) => menuItem.key === key)
  if (item) {
    handleParentClick(item)
  }
}

const handleTabClick = (child: MenuChildItem) => {
  navigateToPath(child.path)
}

</script>

<template>
  <WorkspaceLayout storage-key="settings-sidebar-collapsed">
    <template #sidebar="{ collapsed, toggleSidebar }">
      <WorkspaceSidebar
        :collapsed="collapsed"
        :items="settingsSidebarItems"
        :active-key="activeParentKey"
        :collapse-label="t('settings.layout.collapseNavigation')"
        aria-label="Settings navigation"
        @toggle="toggleSidebar"
        @item-select="handleSidebarItemSelect"
      />
    </template>

    <div class="settings-panel-content">
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
    </div>
  </WorkspaceLayout>
</template>

<style scoped>
.settings-panel-content {
  min-width: 0;
  min-height: 100%;
  display: flex;
  flex-direction: column;
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

</style>
