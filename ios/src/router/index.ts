import { createRouter, createWebHistory } from '@ionic/vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { registerRouterGuards, resolveDefaultAuthenticatedPath, ROUTE_PATHS } from '@/router/guards'
import '@/router/types'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: () => resolveDefaultAuthenticatedPath(),
  },
  {
    path: ROUTE_PATHS.login,
    name: 'Login',
    component: () => import('@/views/auth/LoginPage.vue'),
    meta: {
      title: '鐧诲綍',
      publicOnly: true,
    },
  },
  {
    path: ROUTE_PATHS.loginCodeVerify,
    name: 'LoginCodeVerify',
    component: () => import('@/views/auth/LoginCodeVerifyPage.vue'),
    meta: {
      title: '邮箱验证码登录',
      publicOnly: true,
    },
  },
  {
    path: ROUTE_PATHS.register,
    name: 'Register',
    component: () => import('@/views/auth/RegisterPage.vue'),
    meta: {
      title: '娉ㄥ唽',
      publicOnly: true,
    },
  },
  {
    path: ROUTE_PATHS.forgotPassword,
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPasswordPage.vue'),
    meta: {
      title: '蹇樿瀵嗙爜',
      publicOnly: true,
    },
  },
  {
    path: ROUTE_PATHS.publicRegistrationBooking,
    alias: '/rb/:bookingKey',
    name: 'PublicRegistrationBooking',
    component: () => import('@/views/public/RegistrationBookingPublicPage.vue'),
    meta: {
      title: '鍏紑鐧昏鍏ュ彛',
      publicAccess: true,
    },
  },
  {
    path: ROUTE_PATHS.publicRegistrationForm,
    alias: '/r/:orderNumber',
    name: 'PublicRegistrationForm',
    component: () => import('@/views/public/RegistrationFormPublicPage.vue'),
    meta: {
      title: '鍏紑鍏ヤ綇鐧昏',
      publicAccess: true,
    },
  },
  {
    path: ROUTE_PATHS.publicRoomStatusShare,
    alias: '/share/:token',
    name: 'PublicRoomStatusShare',
    component: () => import('@/views/share/RoomStatusSharePublicPage.vue'),
    meta: {
      title: 'Room Status Share',
      publicAccess: true,
    },
  },
  {
    path: ROUTE_PATHS.cleanerRegister,
    name: 'CleanerRegister',
    component: () => import('@/views/cleaner/CleanerRegisterPage.vue'),
    meta: {
      title: 'Cleaner Register',
      cleanerPublicOnly: true,
    },
  },
  {
    path: ROUTE_PATHS.cleanerLogin,
    name: 'CleanerLogin',
    component: () => import('@/views/cleaner/CleanerLoginPage.vue'),
    meta: {
      title: 'Cleaner Login',
      cleanerPublicOnly: true,
    },
  },
  {
    path: ROUTE_PATHS.cleanerDashboard,
    name: 'CleanerDashboard',
    component: () => import('@/views/cleaner/CleanerDashboardPage.vue'),
    meta: {
      title: '淇濇磥鍛樺伐浣滃彴',
      requiresCleanerAuth: true,
      requiresCleanerStore: true,
    },
  },
  {
    path: ROUTE_PATHS.cleanerTaskDetail,
    name: 'CleanerTaskDetail',
    component: () => import('@/views/cleaner/CleanerTaskDetailPage.vue'),
    meta: {
      title: '浠诲姟璇︽儏',
      requiresCleanerAuth: true,
      requiresCleanerStore: true,
    },
  },
  {
    path: ROUTE_PATHS.storeSelection,
    name: 'StoreSelection',
    component: () => import('@/views/store/StoreSelectionPage.vue'),
    meta: {
      title: '閫夋嫨闂ㄥ簵',
      requiresAuth: true,
    },
  },
  {
    path: ROUTE_PATHS.tabsRoot,
    component: () => import('@/views/shell/AppTabsShell.vue'),
    redirect: ROUTE_PATHS.home,
    meta: {
      requiresAuth: true,
      requiresStore: true,
    },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/HomePage.vue'),
        meta: {
          title: '棣栭〉',
          requiresAuth: true,
          requiresStore: true,
          tab: 'home',
        },
      },
      {
        path: 'rooms',
        name: 'Rooms',
        component: () => import('@/views/rooms/RoomsPage.vue'),
        meta: {
          title: 'Rooms',
          requiresAuth: true,
          requiresStore: true,
          tab: 'rooms',
        },
      },
      {
        path: 'rooms/room-table',
        name: 'RoomsRoomTable',
        component: () => import('@/views/rooms/RoomTablePage.vue'),
        meta: {
          title: 'Room Table',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/pricing',
        name: 'RoomsPricing',
        component: () => import('@/views/rooms/RoomPriceManagementPage.vue'),
        meta: {
          title: '鎴夸环绠＄悊',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/pricing/history',
        name: 'RoomsPricingHistory',
        component: () => import('@/views/rooms/PriceChangeHistoryPage.vue'),
        meta: {
          title: '鏀逛环璁板綍',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/cleaning/overview',
        name: 'RoomsCleaningOverview',
        component: () => import('@/views/rooms/cleaning/CleaningOverviewPage.vue'),
        meta: {
          title: '淇濇磥姒傝',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/cleaning/tasks',
        name: 'RoomsCleaningTasks',
        component: () => import('@/views/rooms/cleaning/CleaningTaskListPage.vue'),
        meta: {
          title: '淇濇磥浠诲姟鍒楄〃',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/detail/:roomId',
        name: 'RoomStatusDetail',
        component: () => import('@/views/room-status/RoomStatusDetailPage.vue'),
        meta: {
          title: '鎴块棿璇︽儏',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/reservations/:reservationId',
        name: 'RoomReservationDetail',
        component: () => import('@/views/room-status/ReservationDetailPage.vue'),
        meta: {
          title: '璁㈠崟璇︽儏',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/orders/OrdersPage.vue'),
        meta: {
          title: '璁㈠崟',
          requiresAuth: true,
          requiresStore: true,
          tab: 'orders',
        },
      },
      {
        path: 'orders/reservations/:reservationId',
        name: 'OrderReservationDetail',
        component: () => import('@/views/room-status/ReservationDetailPage.vue'),
        meta: {
          title: '璁㈠崟璇︽儏',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'channels',
        name: 'Channels',
        component: () => import('@/views/channels/ChannelsPage.vue'),
        meta: {
          title: '娓犻亾',
          requiresAuth: true,
          requiresStore: true,
          tab: 'channels',
        },
      },
      {
        path: 'channels/:otaId',
        name: 'ChannelDetail',
        component: () => import('@/views/channel/ChannelDetailPage.vue'),
        meta: {
          title: '娓犻亾璇︽儏',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'channels/:otaId/mapping',
        name: 'ChannelMapping',
        component: () => import('@/views/channel/ChannelMappingPage.vue'),
        meta: {
          title: '娓犻亾鏄犲皠',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'channels/:otaId/sync',
        name: 'ChannelSync',
        component: () => import('@/views/channel/ChannelSyncPage.vue'),
        meta: {
          title: '娓犻亾鍚屾',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'channels/:otaId/inventory',
        name: 'ChannelInventory',
        component: () => import('@/views/channel/ChannelInventoryPage.vue'),
        meta: {
          title: '鎴块噺璁剧疆',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics',
        redirect: ROUTE_PATHS.statisticsOverview,
      },
      {
        path: 'statistics/overview',
        name: 'StatisticsOverview',
        component: () => import('@/views/statistics/DataCenterPage.vue'),
        meta: {
          title: '鏁版嵁涓績',
          requiresAuth: true,
          requiresStore: true,
          tab: 'statistics',
        },
      },
      {
        path: 'statistics/accommodation',
        name: 'StatisticsAccommodation',
        component: () => import('@/views/statistics/DataCenterPage.vue'),
        meta: {
          title: '浣忓',
          requiresAuth: true,
          requiresStore: true,
          tab: 'statistics',
        },
      },
      {
        path: 'statistics/notes',
        name: 'StatisticsNotes',
        component: () => import('@/views/statistics/DataCenterPage.vue'),
        meta: {
          title: 'Data Center',
          requiresAuth: true,
          requiresStore: true,
          tab: 'statistics',
        },
      },
      {
        path: 'statistics/business-summary',
        name: 'StatisticsBusinessSummary',
        component: () => import('@/views/statistics/BusinessSummaryPage.vue'),
        meta: {
          title: 'Business Summary',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/channel-summary',
        name: 'StatisticsChannelSummary',
        component: () => import('@/views/statistics/ChannelSummaryPage.vue'),
        meta: {
          title: 'Channel Summary',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/notes-summary',
        name: 'StatisticsNotesSummary',
        component: () => import('@/views/statistics/NotesSummaryPage.vue'),
        meta: {
          title: 'Notes Summary',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/revenue-summary',
        name: 'StatisticsRevenueSummary',
        component: () => import('@/views/statistics/RevenueSummaryPage.vue'),
        meta: {
          title: 'Revenue Summary',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/operation-report',
        name: 'StatisticsOperationReport',
        component: () => import('@/views/statistics/OperationReportPage.vue'),
        meta: {
          title: '缁忚惀鎶ヨ〃',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/accommodation-report',
        name: 'StatisticsAccommodationReport',
        component: () => import('@/views/statistics/AccommodationReportPage.vue'),
        meta: {
          title: '浣忓鎶ヨ〃',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/finance-report',
        name: 'StatisticsFinanceReport',
        component: () => import('@/views/statistics/FinanceReportPage.vue'),
        meta: {
          title: '璐㈠姟鎶ヨ〃',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'reviews',
        name: 'RegistrationReviews',
        component: () => import('@/views/reviews/RegistrationReviewListPage.vue'),
        meta: {
          title: '瀹℃煡',
          requiresAuth: true,
          requiresStore: true,
          tab: 'reviews',
        },
      },
      {
        path: 'reviews/links',
        name: 'RegistrationReviewLinks',
        component: () => import('@/views/reviews/RegistrationReviewLinksPage.vue'),
        meta: {
          title: '閾炬帴鍒楄〃',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'reviews/:formId',
        name: 'RegistrationReviewDetail',
        component: () => import('@/views/reviews/RegistrationReviewDetailPage.vue'),
        meta: {
          title: '瀹℃煡璇︽儏',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/messages/MessagesPage.vue'),
        meta: {
          title: '娑堟伅',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'messages/:threadId',
        name: 'MessageDetail',
        component: () => import('@/views/messages/MessageDetailPage.vue'),
        meta: {
          title: '娑堟伅璇︽儏',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'notifications/system',
        name: 'SystemNotifications',
        component: () => import('@/views/notifications/SystemNotificationsPage.vue'),
        meta: {
          title: '绯荤粺閫氱煡',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'notifications/order',
        name: 'OrderNotifications',
        component: () => import('@/views/notifications/OrderNotificationsPage.vue'),
        meta: {
          title: '璁㈠崟閫氱煡',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'wallet',
        name: 'Wallet',
        component: () => import('@/views/wallet/WalletPage.vue'),
        meta: {
          title: '閽卞寘',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'profile',
        name: 'ProfileCenter',
        component: () => import('@/views/profile/ProfileCenterPage.vue'),
        meta: {
          title: '涓汉涓績',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsPage.vue'),
        meta: {
          title: '璁剧疆',
          requiresAuth: true,
          requiresStore: true,
          tab: 'settings',
        },
      },
      {
        path: 'settings/store-profile',
        name: 'SettingsStoreProfile',
        component: () => import('@/views/settings/StoreProfilePage.vue'),
        meta: {
          title: '闂ㄥ簵璧勬枡',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/store-details',
        name: 'SettingsStoreDetails',
        component: () => import('@/views/settings/StoreDetailsPage.vue'),
        meta: {
          title: '闂ㄥ簵璇︽儏',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/store-members',
        name: 'SettingsStoreMembers',
        component: () => import('@/views/settings/StoreMembersPage.vue'),
        meta: {
          title: '闂ㄥ簵鎴愬憳',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/role-permissions/:roleId',
        name: 'SettingsRolePermissions',
        component: () => import('@/views/settings/RolePermissionsPage.vue'),
        meta: {
          title: '瑙掕壊鏉冮檺',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/room-types',
        name: 'SettingsRoomTypes',
        component: () => import('@/views/settings/RoomTypeSettingsPage.vue'),
        meta: {
          title: '鎴垮瀷璁剧疆',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/room-types/:id/details',
        name: 'SettingsRoomTypeDetail',
        component: () => import('@/views/settings/RoomTypeDetailPage.vue'),
        meta: {
          title: '鎴垮瀷璇︽儏',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/room-groups',
        name: 'SettingsRoomGroups',
        component: () => import('@/views/settings/RoomGroupsPage.vue'),
        meta: {
          title: '鎴块棿鍒嗙粍',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/room-sort',
        name: 'SettingsRoomSort',
        component: () => import('@/views/settings/RoomSortPage.vue'),
        meta: {
          title: '鎺掑簭璁剧疆',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/price-plans',
        name: 'SettingsPricePlans',
        component: () => import('@/views/settings/PricePlansPage.vue'),
        meta: {
          title: '浠锋牸璁″垝',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/price-plans/:pricePlanId/rates',
        name: 'SettingsPricePlanRates',
        component: () => import('@/views/settings/PricePlanRatesPage.vue'),
        meta: {
          title: '鎴垮瀷浠锋牸',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/payment-methods',
        name: 'SettingsPaymentMethods',
        component: () => import('@/views/settings/PaymentMethodsPage.vue'),
        meta: {
          title: '鏀舵鏂瑰紡',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/note-settings',
        name: 'SettingsNoteSettings',
        component: () => import('@/views/settings/NoteSettingsPage.vue'),
        meta: {
          title: 'Note Settings',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/consumption-items',
        name: 'SettingsConsumptionItems',
        component: () => import('@/views/settings/ConsumptionItemsPage.vue'),
        meta: {
          title: 'Consumption Items',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/notification',
        name: 'SettingsNotification',
        component: () => import('@/views/settings/NotificationSettingsPage.vue'),
        meta: {
          title: '閫氱煡璁剧疆',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/channel-settings',
        name: 'SettingsChannelSettings',
        component: () => import('@/views/settings/ChannelSettingsPage.vue'),
        meta: {
          title: '娓犻亾璁剧疆',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/quick-replies',
        name: 'SettingsQuickReplies',
        component: () => import('@/views/settings/QuickRepliesPage.vue'),
        meta: {
          title: '蹇嵎鍥炲',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/auto-messages',
        name: 'SettingsAutoMessages',
        component: () => import('@/views/settings/AutoMessagesPage.vue'),
        meta: {
          title: '鑷姩娑堟伅',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/cleaning-settings',
        name: 'SettingsCleaningSettings',
        component: () => import('@/views/settings/CleaningSettingsPage.vue'),
        meta: {
          title: '淇濇磥璁剧疆',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/cleaning-supplies',
        name: 'SettingsCleaningSupplies',
        component: () => import('@/views/settings/CleaningSuppliesPage.vue'),
        meta: {
          title: '鏄撹€楀搧',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/pricing-tools',
        name: 'SettingsPricingTools',
        component: () => import('@/views/settings/PricingToolsPage.vue'),
        meta: {
          title: '瀹氫环宸ュ叿',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/payment-platforms',
        name: 'SettingsPaymentPlatforms',
        component: () => import('@/views/settings/PaymentPlatformsPage.vue'),
        meta: {
          title: '鏀粯骞冲彴',
          requiresAuth: true,
          requiresStore: true,
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: () => resolveDefaultAuthenticatedPath(),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

registerRouterGuards(router)

export default router

