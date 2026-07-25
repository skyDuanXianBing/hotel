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
      titleKey: 'routes.Login',
      publicOnly: true,
    },
  },
  {
    path: ROUTE_PATHS.loginCodeVerify,
    name: 'LoginCodeVerify',
    component: () => import('@/views/auth/LoginCodeVerifyPage.vue'),
    meta: {
      titleKey: 'routes.LoginCodeVerify',
      publicOnly: true,
    },
  },
  {
    path: ROUTE_PATHS.register,
    name: 'Register',
    redirect: (to) => ({
      path: ROUTE_PATHS.login,
      query: {
        ...to.query,
        tab: 'register',
      },
    }),
  },
  {
    path: ROUTE_PATHS.forgotPassword,
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPasswordPage.vue'),
    meta: {
      titleKey: 'routes.ForgotPassword',
      publicOnly: true,
    },
  },
  {
    path: ROUTE_PATHS.publicRegistrationBooking,
    alias: '/rb/:bookingKey',
    name: 'PublicRegistrationBooking',
    component: () => import('@/views/public/RegistrationBookingPublicPage.vue'),
    meta: {
      titleKey: 'routes.PublicRegistrationBooking',
      publicAccess: true,
    },
  },
  {
    path: ROUTE_PATHS.publicRegistrationForm,
    alias: '/r/:orderNumber',
    name: 'PublicRegistrationForm',
    component: () => import('@/views/public/RegistrationFormPublicPage.vue'),
    meta: {
      titleKey: 'routes.PublicRegistrationForm',
      publicAccess: true,
    },
  },
  {
    path: ROUTE_PATHS.publicRoomStatusShare,
    alias: '/share/:token',
    name: 'PublicRoomStatusShare',
    component: () => import('@/views/share/RoomStatusSharePublicPage.vue'),
    meta: {
      titleKey: 'routes.PublicRoomStatusShare',
      publicAccess: true,
    },
  },
  {
    path: ROUTE_PATHS.cleanerRegister,
    name: 'CleanerRegister',
    redirect: (to) => ({
      path: ROUTE_PATHS.login,
      query: to.query,
    }),
  },
  {
    path: ROUTE_PATHS.cleanerInviteRegister,
    name: 'CleanerInviteRegister',
    redirect: (to) => ({
      path: ROUTE_PATHS.login,
      query: to.query,
    }),
  },
  {
    path: ROUTE_PATHS.cleanerLogin,
    name: 'CleanerLogin',
    redirect: (to) => ({
      path: ROUTE_PATHS.login,
      query: {
        ...to.query,
        workspace: 'CLEANER',
      },
    }),
  },
  {
    path: ROUTE_PATHS.cleanerDashboard,
    name: 'CleanerDashboard',
    component: () => import('@/views/cleaner/CleanerDashboardPage.vue'),
    meta: {
      titleKey: 'routes.CleanerDashboard',
      requiresCleanerAuth: true,
      requiresCleanerStore: true,
    },
  },
  {
    path: ROUTE_PATHS.cleanerTaskDetail,
    name: 'CleanerTaskDetail',
    component: () => import('@/views/cleaner/CleanerTaskDetailPage.vue'),
    meta: {
      titleKey: 'routes.CleanerTaskDetail',
      requiresCleanerAuth: true,
      requiresCleanerStore: true,
    },
  },
  {
    path: ROUTE_PATHS.storeSelection,
    name: 'StoreSelection',
    component: () => import('@/views/store/StoreSelectionPage.vue'),
    meta: {
      titleKey: 'routes.StoreSelection',
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
          titleKey: 'routes.Home',
          requiresAuth: true,
          requiresStore: true,
          tab: 'home',
        },
      },
      {
        path: 'home/customize',
        name: 'HomeCustomize',
        component: () => import('@/views/home/HomeShortcutManagePage.vue'),
        meta: {
          titleKey: 'routes.HomeCustomize',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms',
        name: 'Rooms',
        component: () => import('@/views/rooms/RoomsPage.vue'),
        meta: {
          titleKey: 'routes.Rooms',
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
          titleKey: 'routes.RoomsRoomTable',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/pricing',
        name: 'RoomsPricing',
        component: () => import('@/views/rooms/RoomPriceManagementPage.vue'),
        meta: {
          titleKey: 'routes.RoomsPricing',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/pricing/history',
        name: 'RoomsPricingHistory',
        component: () => import('@/views/rooms/PriceChangeHistoryPage.vue'),
        meta: {
          titleKey: 'routes.RoomsPricingHistory',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/cleaning/overview',
        name: 'RoomsCleaningOverview',
        component: () => import('@/views/rooms/cleaning/CleaningOverviewPage.vue'),
        meta: {
          titleKey: 'routes.RoomsCleaningOverview',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/cleaning/tasks',
        name: 'RoomsCleaningTasks',
        component: () => import('@/views/rooms/cleaning/CleaningTaskListPage.vue'),
        meta: {
          titleKey: 'routes.RoomsCleaningTasks',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/detail/:roomId',
        name: 'RoomStatusDetail',
        component: () => import('@/views/room-status/RoomStatusDetailPage.vue'),
        meta: {
          titleKey: 'routes.RoomStatusDetail',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'rooms/reservations/:reservationId',
        name: 'RoomReservationDetail',
        component: () => import('@/views/room-status/ReservationDetailPage.vue'),
        meta: {
          titleKey: 'routes.RoomReservationDetail',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/orders/OrdersPage.vue'),
        meta: {
          titleKey: 'routes.Orders',
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
          titleKey: 'routes.OrderReservationDetail',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'channels',
        name: 'Channels',
        component: () => import('@/views/channels/ChannelsPage.vue'),
        meta: {
          titleKey: 'routes.Channels',
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
          titleKey: 'routes.ChannelDetail',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'channels/:otaId/mapping',
        name: 'ChannelMapping',
        component: () => import('@/views/channel/ChannelMappingPage.vue'),
        meta: {
          titleKey: 'routes.ChannelMapping',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'channels/:otaId/sync',
        name: 'ChannelSync',
        component: () => import('@/views/channel/ChannelSyncPage.vue'),
        meta: {
          titleKey: 'routes.ChannelSync',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'channels/:otaId/inventory',
        name: 'ChannelInventory',
        component: () => import('@/views/channel/ChannelInventoryPage.vue'),
        meta: {
          titleKey: 'routes.ChannelInventory',
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
          titleKey: 'routes.StatisticsOverview',
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
          titleKey: 'routes.StatisticsAccommodation',
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
          titleKey: 'routes.StatisticsNotes',
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
          titleKey: 'routes.StatisticsBusinessSummary',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/channel-summary',
        name: 'StatisticsChannelSummary',
        component: () => import('@/views/statistics/ChannelSummaryPage.vue'),
        meta: {
          titleKey: 'routes.StatisticsChannelSummary',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/notes-summary',
        name: 'StatisticsNotesSummary',
        component: () => import('@/views/statistics/NotesSummaryPage.vue'),
        meta: {
          titleKey: 'routes.StatisticsNotesSummary',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/revenue-summary',
        name: 'StatisticsRevenueSummary',
        component: () => import('@/views/statistics/RevenueSummaryPage.vue'),
        meta: {
          titleKey: 'routes.StatisticsRevenueSummary',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/operation-report',
        name: 'StatisticsOperationReport',
        component: () => import('@/views/statistics/OperationReportPage.vue'),
        meta: {
          titleKey: 'routes.StatisticsOperationReport',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/accommodation-report',
        name: 'StatisticsAccommodationReport',
        component: () => import('@/views/statistics/AccommodationReportPage.vue'),
        meta: {
          titleKey: 'routes.StatisticsAccommodationReport',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'statistics/finance-report',
        name: 'StatisticsFinanceReport',
        component: () => import('@/views/statistics/FinanceReportPage.vue'),
        meta: {
          titleKey: 'routes.StatisticsFinanceReport',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'reviews',
        name: 'RegistrationReviews',
        component: () => import('@/views/reviews/RegistrationReviewListPage.vue'),
        meta: {
          titleKey: 'routes.RegistrationReviews',
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
          titleKey: 'routes.RegistrationReviewLinks',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'reviews/:formId',
        name: 'RegistrationReviewDetail',
        component: () => import('@/views/reviews/RegistrationReviewDetailPage.vue'),
        meta: {
          titleKey: 'routes.RegistrationReviewDetail',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/messages/MessagesPage.vue'),
        meta: {
          titleKey: 'routes.Messages',
          requiresAuth: true,
          requiresStore: true,
          tab: 'messages',
        },
      },
      {
        path: 'messages/:threadId',
        name: 'MessageDetail',
        component: () => import('@/views/messages/MessageDetailPage.vue'),
        meta: {
          titleKey: 'routes.MessageDetail',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'notifications/system',
        name: 'SystemNotifications',
        component: () => import('@/views/notifications/SystemNotificationsPage.vue'),
        meta: {
          titleKey: 'routes.SystemNotifications',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'notifications/order',
        name: 'OrderNotifications',
        component: () => import('@/views/notifications/OrderNotificationsPage.vue'),
        meta: {
          titleKey: 'routes.OrderNotifications',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'wallet',
        name: 'Wallet',
        component: () => import('@/views/wallet/WalletPage.vue'),
        meta: {
          titleKey: 'routes.Wallet',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'profile',
        name: 'ProfileCenter',
        component: () => import('@/views/profile/ProfileCenterPage.vue'),
        meta: {
          titleKey: 'routes.ProfileCenter',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsPage.vue'),
        meta: {
          titleKey: 'routes.Settings',
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
          titleKey: 'routes.SettingsStoreProfile',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/store-details',
        name: 'SettingsStoreDetails',
        component: () => import('@/views/settings/StoreDetailsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsStoreDetails',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/store-members',
        name: 'SettingsStoreMembers',
        component: () => import('@/views/settings/StoreMembersPage.vue'),
        meta: {
          titleKey: 'routes.SettingsStoreMembers',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/role-permissions/:roleId',
        name: 'SettingsRolePermissions',
        component: () => import('@/views/settings/RolePermissionsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsRolePermissions',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/room-types',
        name: 'SettingsRoomTypes',
        component: () => import('@/views/settings/RoomTypeSettingsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsRoomTypes',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/room-types/:id/details',
        name: 'SettingsRoomTypeDetail',
        component: () => import('@/views/settings/RoomTypeDetailPage.vue'),
        meta: {
          titleKey: 'routes.SettingsRoomTypeDetail',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/room-groups',
        name: 'SettingsRoomGroups',
        component: () => import('@/views/settings/RoomGroupsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsRoomGroups',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/room-sort',
        name: 'SettingsRoomSort',
        component: () => import('@/views/settings/RoomSortPage.vue'),
        meta: {
          titleKey: 'routes.SettingsRoomSort',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/price-plans',
        name: 'SettingsPricePlans',
        component: () => import('@/views/settings/PricePlansPage.vue'),
        meta: {
          titleKey: 'routes.SettingsPricePlans',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/price-plans/:pricePlanId/rates',
        name: 'SettingsPricePlanRates',
        component: () => import('@/views/settings/PricePlanRatesPage.vue'),
        meta: {
          titleKey: 'routes.SettingsPricePlanRates',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/payment-methods',
        name: 'SettingsPaymentMethods',
        component: () => import('@/views/settings/PaymentMethodsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsPaymentMethods',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/note-settings',
        name: 'SettingsNoteSettings',
        component: () => import('@/views/settings/NoteSettingsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsNoteSettings',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/consumption-items',
        name: 'SettingsConsumptionItems',
        component: () => import('@/views/settings/ConsumptionItemsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsConsumptionItems',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/notification',
        name: 'SettingsNotification',
        component: () => import('@/views/settings/NotificationSettingsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsNotification',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/channel-settings',
        name: 'SettingsChannelSettings',
        component: () => import('@/views/settings/ChannelSettingsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsChannelSettings',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/quick-replies',
        name: 'SettingsQuickReplies',
        component: () => import('@/views/settings/QuickRepliesPage.vue'),
        meta: {
          titleKey: 'routes.SettingsQuickReplies',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/auto-messages',
        name: 'SettingsAutoMessages',
        component: () => import('@/views/settings/AutoMessagesPage.vue'),
        meta: {
          titleKey: 'routes.SettingsAutoMessages',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/cleaning-settings',
        name: 'SettingsCleaningSettings',
        component: () => import('@/views/settings/CleaningSettingsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsCleaningSettings',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/cleaning-supplies',
        name: 'SettingsCleaningSupplies',
        component: () => import('@/views/settings/CleaningSuppliesPage.vue'),
        meta: {
          titleKey: 'routes.SettingsCleaningSupplies',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/pricing-tools',
        name: 'SettingsPricingTools',
        component: () => import('@/views/settings/PricingToolsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsPricingTools',
          requiresAuth: true,
          requiresStore: true,
        },
      },
      {
        path: 'settings/payment-platforms',
        name: 'SettingsPaymentPlatforms',
        component: () => import('@/views/settings/PaymentPlatformsPage.vue'),
        meta: {
          titleKey: 'routes.SettingsPaymentPlatforms',
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
