<script setup lang="ts">
import { computed, onMounted, onUnmounted, provide, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import NotificationPopup from '@/components/NotificationPopup.vue'
import CustomerService from '@/components/CustomerService.vue'
import AppTopNav from '@/components/layout/AppTopNav.vue'
import {
  appTopNavBindingsKey,
  type AppTopNavProps,
} from '@/components/layout/appShellContext'
import { PermissionAction, PermissionModule } from '@/api/role'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { useMemoStore } from '@/stores/memo'
import { usePermissionStore } from '@/stores/permission'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { StoreDTO } from '@/api/store'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const userStore = useUserStore()
const memoStore = useMemoStore()
const permissionStore = usePermissionStore()
const storeStore = useStoreStore()
const notificationCenterStore = useNotificationCenterStore()

const stores = ref<StoreDTO[]>([])
const notificationPopupRef = ref<InstanceType<typeof NotificationPopup> | null>(null)
const showCustomerService = ref(false)

const MAX_UNREAD_BADGE_COUNT = 99

const currentStore = computed(() => storeStore.currentStore)
const currentUser = computed(() => userStore.currentUser)
const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.email || '')
const currentUserEmail = computed(() => currentUser.value?.email || '')

interface NavItem {
  labelKey: string
  path: string
}

const normalizePath = (path: string) => path.replace(/\/+$/, '') || '/'

const isAccommodationRoute = computed(() => normalizePath(route.path).startsWith('/accommodation'))
const isRegistrationReviewListRoute = computed(() => route.name === 'DataCenterRegistrations')
const isOrderRoute = computed(() => normalizePath(route.path).startsWith('/order'))
const usesEmbeddedTopNav = computed(
  () => isAccommodationRoute.value || isRegistrationReviewListRoute.value || isOrderRoute.value,
)

const loadStores = async () => {
  try {
    stores.value = await storeStore.fetchUserStores(true)
  } catch (error) {
    console.error('Failed to load store list', error)
  }
}

const handleStoreSelect = (store: StoreDTO) => {
  storeStore.setCurrentStore(store)
  ElMessage.success(t('layout.store.switched', { name: store.name }))
  router.go(0)
}

const goToStoreSelection = () => {
  router.push('/store/selection')
}

const canAccessAccommodation = computed(() =>
  permissionStore.hasPermissions(
    [
      { module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_STATUS },
      { module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_PRICE },
      { module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_INFO },
      { module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST },
    ],
    'any',
  ),
)

const canAccessOrder = computed(() =>
  permissionStore.hasPermission(PermissionModule.ORDER, PermissionAction.VIEW_ORDERS),
)

const canAccessStatistics = computed(() =>
  permissionStore.hasPermission(PermissionModule.STATISTICS, PermissionAction.VIEW_STATS),
)

const canAccessSettings = computed(() =>
  permissionStore.hasPermissions(
    [
      { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      { module: PermissionModule.SETTINGS, action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS },
      { module: PermissionModule.CHANNEL, action: PermissionAction.VIEW_CHANNELS },
    ],
    'any',
  ),
)

const canAccessWallet = computed(() =>
  permissionStore.hasPermission(PermissionModule.SENSITIVE, PermissionAction.VIEW_FINANCIAL_DATA),
)

const navItems = computed<NavItem[]>(() => {
  const items: Array<NavItem & { visible: boolean }> = [
    { labelKey: 'nav.home', path: '/', visible: true },
    { labelKey: 'nav.accommodation', path: '/accommodation', visible: canAccessAccommodation.value },
    { labelKey: 'nav.messages', path: '/messages', visible: true },
    { labelKey: 'nav.order', path: '/order', visible: canAccessOrder.value },
    {
      labelKey: 'nav.statistics',
      path: '/data-center/overview',
      visible: canAccessStatistics.value,
    },
    {
      labelKey: 'nav.review',
      path: '/data-center/registrations',
      visible: canAccessStatistics.value,
    },
    { labelKey: 'nav.settings', path: '/settings', visible: canAccessSettings.value },
  ]

  return items.filter((item) => item.visible)
})

const formatUnreadBadge = (count: number) => {
  if (count > MAX_UNREAD_BADGE_COUNT) {
    return `${MAX_UNREAD_BADGE_COUNT}+`
  }
  return String(count)
}

const chatUnreadCount = computed(() => {
  const count = Number(notificationCenterStore.chatUnreadCount || 0)
  return Number.isFinite(count) && count > 0 ? count : 0
})

const inboxUnreadCount = computed(() => {
  const count = Number(notificationCenterStore.inboxUnreadCount || 0)
  return Number.isFinite(count) && count > 0 ? count : 0
})

const hasSystemUnread = computed(() => notificationCenterStore.systemUnreadCount > 0)
const hasOrderUnread = computed(() => notificationCenterStore.orderUnreadCount > 0)

const formattedChatUnreadCount = computed(() => formatUnreadBadge(chatUnreadCount.value))
const formattedInboxUnreadCount = computed(() => formatUnreadBadge(inboxUnreadCount.value))

const handleMenuClick = (path: string) => {
  router.push(path)
}

const handleWalletClick = () => {
  router.push('/wallet')
}

const handleSystemNotification = () => {
  router.push('/notifications/system')
}

const handleOrderNotification = () => {
  router.push('/notifications/order')
}

const handleInboxClick = () => {
  router.push('/messages')
}

const handleSupportChat = () => {
  showCustomerService.value = true
}

const handleProfileClick = () => {
  router.push('/profile')
}

const handleLogout = async () => {
  try {
    await userStore.logout()
    storeStore.clearStoreData()
    permissionStore.clearPermissions()
    ElMessage.success(t('layout.logoutSuccess'))
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : t('layout.logoutFailed')
    ElMessage.error(message)
  } finally {
    router.replace('/login')
  }
}

const topNavProps = computed<AppTopNavProps>(() => ({
  stores: stores.value,
  currentStore: currentStore.value,
  navItems: navItems.value,
  currentPath: route.path,
  displayName: displayName.value,
  userEmail: currentUserEmail.value,
  userAvatar: currentUser.value?.avatar,
  canAccessWallet: canAccessWallet.value,
  canAccessOrder: canAccessOrder.value,
  chatUnreadCount: chatUnreadCount.value,
  formattedChatUnreadCount: formattedChatUnreadCount.value,
  inboxUnreadCount: inboxUnreadCount.value,
  formattedInboxUnreadCount: formattedInboxUnreadCount.value,
  hasSystemUnread: hasSystemUnread.value,
  hasOrderUnread: hasOrderUnread.value,
}))

provide(appTopNavBindingsKey, {
  props: topNavProps,
  onStoreSelect: handleStoreSelect,
  onManageStores: goToStoreSelection,
  onMenuClick: handleMenuClick,
  onWalletClick: handleWalletClick,
  onInboxClick: handleInboxClick,
  onSupportChat: handleSupportChat,
  onSystemNotification: handleSystemNotification,
  onOrderNotification: handleOrderNotification,
  onProfileClick: handleProfileClick,
  onLogout: handleLogout,
})

onMounted(() => {
  if (localStorage.getItem('token')) {
    userStore.fetchCurrentUser().catch(() => {
      /* auth interceptor handles invalid session */
    })
    memoStore.loadMemo()
    loadStores()
  }
})

watch(
  () => currentStore.value?.id,
  (storeId) => {
    if (!storeId) {
      permissionStore.clearPermissions()
      return
    }

    void permissionStore.fetchCurrentStorePermissions(true).catch(() => {
      /* route guard handles permission failures */
    })
  },
  { immediate: true },
)

watch(
  notificationPopupRef,
  (popup) => {
    notificationCenterStore.bindPopupController(
      popup as unknown as { addNotification: (notification: unknown) => void } | null,
    )
  },
  { immediate: true },
)

watch(
  [() => userStore.currentUser?.id, () => currentStore.value?.id],
  ([userId, storeId]) => {
    if (!userId) {
      notificationCenterStore.stop()
      return
    }

    void notificationCenterStore.start({
      userId,
      storeId: typeof storeId === 'number' ? storeId : undefined,
      onOrderClick: () => router.push('/notifications/order'),
      onChatClick: () => router.push('/messages'),
    })
  },
  { immediate: true },
)

onUnmounted(() => {
  notificationCenterStore.stop()
  notificationCenterStore.bindPopupController(null)
})
</script>

<template>
  <div class="main-layout" :class="{ 'main-layout--embedded-nav': usesEmbeddedTopNav }">
    <template v-if="usesEmbeddedTopNav">
      <router-view />
    </template>
    <template v-else>
      <header class="app-header">
        <AppTopNav
          v-bind="topNavProps"
          @store-select="handleStoreSelect"
          @manage-stores="goToStoreSelection"
          @menu-click="handleMenuClick"
          @wallet-click="handleWalletClick"
          @inbox-click="handleInboxClick"
          @support-chat="handleSupportChat"
          @system-notification="handleSystemNotification"
          @order-notification="handleOrderNotification"
          @profile-click="handleProfileClick"
          @logout="handleLogout"
        />
      </header>

      <main class="layout-main">
        <router-view />
      </main>
    </template>

    <NotificationPopup ref="notificationPopupRef" />
    <CustomerService
      :visible="showCustomerService"
      @close="showCustomerService = false"
      @minimize="showCustomerService = false"
    />
  </div>
</template>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f2;
}

.main-layout--embedded-nav {
  height: 100vh;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  padding: 18px 32px 14px;
  background: #f5f5f2;
}

.layout-main {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

@media (max-width: 1280px) {
  .app-header {
    padding-left: 20px;
    padding-right: 20px;
  }
}
</style>
