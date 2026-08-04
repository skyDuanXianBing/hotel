import { createRouter, createWebHistory } from 'vue-router'
import { PermissionAction, PermissionModule } from '@/api/role'
import { pinia } from '@/stores/pinia'
import {
  usePermissionStore,
  type PermissionMatchMode,
  type PermissionRequirement,
} from '@/stores/permission'
import { useEntitlementStore } from '@/stores/entitlement'
import {
  CLEANER_TOKEN_KEY,
  PMS_CURRENT_STORE_KEY,
  PMS_TOKEN_KEY,
  clearCleanerSession,
  hasCompleteCleanerSession,
  resolveCachedLoginSessionTarget,
} from '@/utils/cleanerSession'
import {
  ADMIN_DASHBOARD_PATH,
  ADMIN_LOGIN_PATH,
  hasCompleteAdminSession,
  isAdminWorkspacePath,
} from '@/utils/adminSession'
import { SAAS_FEATURE_CODES } from '@/api/billing'

const LOGIN_PATH = '/login'
const REGISTER_PATH = '/register'
const FORGOT_PASSWORD_PATH = '/forgot-password'
const STORE_SELECTION_PATH = '/store/selection'
const CLEANER_PATH_PREFIX = '/cleaner'
const CLEANER_LOGIN_PATH = '/cleaner/login'
const CLEANER_REGISTER_PATH = '/cleaner/register'
const CLEANER_DASHBOARD_PATH = '/cleaner/dashboard'

const isCleanerWorkspacePath = (path: string) => {
  if (!path.startsWith(CLEANER_PATH_PREFIX)) {
    return false
  }
  if (path === CLEANER_LOGIN_PATH || path.startsWith(CLEANER_REGISTER_PATH)) {
    return false
  }
  return true
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // Public registration form (no login)
    {
      path: '/r/:orderNumber',
      name: 'PublicRegistration',
      component: () => import('@/views/public/RegistrationFormPublic.vue'),
      meta: { title: 'Check-in Registration', requiresAuth: false },
    },
    // Public booking summary (no login)
    {
      path: '/rb/:bookingKey',
      name: 'PublicRegistrationBooking',
      component: () => import('@/views/public/RegistrationBookingPublic.vue'),
      meta: { title: 'Check-in Registration', requiresAuth: false },
    },
    // Public independent booking site (no login or back-office layout)
    {
      path: '/stay/:slug',
      name: 'IndependentSitePublic',
      component: () => import('@/views/public/IndependentSitePublic.vue'),
      meta: { title: 'Book your stay', requiresAuth: false },
    },
    // Public independent booking site sub-pages (same component, multi-page support)
    {
      path: '/stay/:slug/p/:pagePath+',
      name: 'IndependentSitePublicPage',
      component: () => import('@/views/public/IndependentSitePublic.vue'),
      meta: { title: 'Book your stay', requiresAuth: false },
    },
    // Main layout routes with navigation
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Home',
          component: () => import('@/views/home/HomePage.vue'),
          meta: { title: 'Home', requiresAuth: true },
        },
        {
          path: 'accommodation',
          name: 'Accommodation',
          component: () => import('@/views/accommodation/AccommodationLayout.vue'),
          redirect: '/accommodation/room-status/calendar',
          meta: { requiresAuth: true },
          children: [
            {
              path: 'room-status',
              component: () => import('@/views/room-status/RoomStatusLayout.vue'),
              redirect: '/accommodation/room-status/calendar',
              meta: { requiresAuth: true },
              children: [
                {
                  path: 'calendar',
                  name: 'RoomStatusCalendar',
                  component: () => import('@/views/room-status/RoomStatusCalendar.vue'),
                  meta: { title: 'Room Status', requiresAuth: true },
                },
                {
                  path: 'daily',
                  name: 'RoomStatusDaily',
                  component: () => import('@/views/room-status/RoomStatusDaily.vue'),
                  meta: { title: 'Daily Room Status', requiresAuth: true },
                },
                {
                  path: 'channel',
                  name: 'RoomStatusChannel',
                  component: () => import('@/views/room-status/RoomStatusChannel.vue'),
                  meta: { title: 'Channel Room Status', requiresAuth: true },
                },
              ],
            },
            {
              path: 'room-price-management',
              name: 'RoomPriceManagement',
              component: () => import('@/views/accommodation/RoomPriceManagement.vue'),
              meta: { title: 'Room Rate Management', requiresAuth: true },
            },
            {
              path: 'room-price-bulk-update',
              name: 'RoomPriceBulkUpdate',
              component: () => import('@/views/accommodation/RoomPriceBulkUpdate.vue'),
              meta: { title: 'Bulk Update', requiresAuth: true },
            },
            {
              path: 'room-price/change-history',
              name: 'PriceChangeHistory',
              component: () => import('@/views/accommodation/PriceChangeHistory.vue'),
              meta: { title: 'Rate Change History', requiresAuth: true },
            },
            {
              path: 'room-table',
              name: 'RoomTable',
              component: () => import('@/views/accommodation/RoomTable.vue'),
              meta: { title: 'Room Overview', requiresAuth: true },
            },
            {
              path: 'housekeeping-list',
              name: 'HousekeepingList',
              component: () => import('@/views/accommodation/HousekeepingList.vue'),
              meta: { title: 'Housekeeping List', requiresAuth: true },
            },
            {
              path: 'housekeeper-list',
              name: 'HousekeeperList',
              component: () => import('@/views/accommodation/HousekeeperList.vue'),
              meta: { title: 'Cleaner List', requiresAuth: true },
            },
            {
              path: 'cleaning/overview',
              name: 'CleaningOverview',
              component: () => import('@/views/accommodation/cleaning/CleaningOverview.vue'),
              meta: { title: 'Task Overview', requiresAuth: true },
            },
            {
              path: 'cleaning/task-list',
              name: 'CleaningTaskList',
              component: () => import('@/views/accommodation/cleaning/CleaningTaskList.vue'),
              meta: { title: 'Task List', requiresAuth: true },
            },
          ],
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/settings/SettingsLayout.vue'),
          redirect: '/settings/room-type',
          meta: { requiresAuth: true },
          children: [
            // Store settings
            {
              path: 'store/basic-info',
              name: 'StoreBasicInfo',
              component: () => import('@/views/settings/store/StoreBasicInfo.vue'),
              meta: { title: 'Basic Info', requiresAuth: true },
            },
            {
              path: 'store/details',
              name: 'StoreDetails',
              component: () => import('@/views/settings/store/StoreDetails.vue'),
              meta: { title: 'Store Details', requiresAuth: true },
            },
            // General settings
            {
              path: 'general/notification',
              name: 'NotificationSettings',
              component: () => import('@/views/settings/general/NotificationSettings.vue'),
              meta: { title: 'Notification Settings', requiresAuth: true },
            },
            {
              path: 'general/announcements',
              name: 'AnnouncementSettings',
              component: () => import('@/views/settings/general/AnnouncementSettings.vue'),
              meta: { title: 'Announcement Management', requiresAuth: true },
            },
            {
              path: 'general/channel',
              name: 'GeneralChannelSettings',
              component: () => import('@/views/settings/general/ChannelSettings.vue'),
              meta: { title: 'Channel Settings', requiresAuth: true },
            },
            {
              path: 'general/quick-reply',
              name: 'QuickReply',
              component: () => import('@/views/settings/general/QuickReply.vue'),
              meta: { title: 'Quick Replies', requiresAuth: true },
            },
            {
              path: 'general/auto-message',
              name: 'AutoMessage',
              component: () => import('@/views/settings/general/AutoMessage.vue'),
              meta: { title: 'Automation Messages', requiresAuth: true },
            },
            {
              path: 'general/ai-message-knowledge',
              name: 'AiMessageKnowledge',
              component: () => import('@/views/settings/general/AiMessageKnowledge.vue'),
              meta: { title: 'AI Message Knowledge Base', requiresAuth: true },
            },
            // Cleaning settings
            {
              path: 'cleaning/settings',
              name: 'CleaningSettings',
              component: () => import('@/views/settings/cleaning/CleaningSettings.vue'),
              meta: { title: 'Settings', requiresAuth: true },
            },
            {
              path: 'cleaning/supplies',
              name: 'CleaningSupplies',
              component: () => import('@/views/settings/cleaning/CleaningSupplies.vue'),
              meta: { title: 'Supplies', requiresAuth: true },
            },
            // Auto check-in
            {
              path: 'auto-checkin/settings',
              name: 'AutoCheckinSettings',
              component: () => import('@/views/settings/auto-checkin/AutoCheckinSettings.vue'),
              meta: { title: 'Auto Check-in Settings', requiresAuth: true },
            },
            // Third-party integrations
            {
              path: 'third-party/pricing-tools',
              name: 'PricingTools',
              component: () => import('@/views/settings/third-party/PricingTools.vue'),
              meta: { title: 'Pricing Tools', requiresAuth: true },
            },
            {
              path: 'third-party/payment-platforms',
              name: 'PaymentPlatforms',
              component: () => import('@/views/settings/third-party/PaymentPlatforms.vue'),
              meta: { title: 'Payment Platforms', requiresAuth: true },
            },
            {
              path: 'third-party/door-locks',
              name: 'DoorLocks',
              component: () => import('@/views/settings/third-party/DoorLocks.vue'),
              meta: { title: 'Door Locks', requiresAuth: true },
            },
            // Legacy settings
            {
              path: 'room-type',
              name: 'RoomTypeManagement',
              component: () => import('@/views/settings/room/RoomSettings.vue'),
              meta: { title: 'Room Settings', requiresAuth: true },
            },
            {
              path: 'room/ownership',
              name: 'RoomOwnership',
              component: () => import('@/views/settings/room/RoomOwnership.vue'),
              meta: { title: 'Room Ownership', requiresAuth: true },
            },
            {
              path: 'room-type/:id/details',
              name: 'RoomTypeDetails',
              component: () => import('@/views/settings/room/RoomTypeDetails.vue'),
              meta: { title: 'Room Type Details', requiresAuth: true },
            },
            // Accommodation settings
            {
              path: 'room/price-plan',
              name: 'PricePlan',
              component: () => import('@/views/settings/room/PricePlan.vue'),
              meta: { title: 'Price Plan', requiresAuth: true },
            },
            {
              path: 'room/consumption-items',
              name: 'ConsumptionItems',
              component: () => import('@/views/settings/room/ConsumptionItems.vue'),
              meta: { title: 'Consumption Item Settings', requiresAuth: true },
            },
            {
              path: 'room/room-group',
              name: 'RoomGroup',
              component: () => import('@/views/settings/room/RoomGroup.vue'),
              meta: { title: 'Room Group Settings', requiresAuth: true },
            },
            {
              path: 'room/room-sort',
              name: 'RoomSort',
              component: () => import('@/views/settings/room/RoomSort.vue'),
              meta: { title: 'Sort Settings', requiresAuth: true },
            },
            {
              path: 'room-status-config',
              name: 'RoomStatusConfig',
              component: () => import('@/views/settings/RoomStatusConfig.vue'),
              meta: { title: 'Room Group Settings', requiresAuth: true },
            },
            {
              path: 'room-management',
              name: 'RoomManagement',
              component: () => import('@/views/settings/RoomManagement.vue'),
              meta: { title: 'Room Management', requiresAuth: true },
            },
            {
              path: 'channel-settings',
              name: 'ChannelSettings',
              component: () => import('@/views/settings/ChannelSettings.vue'),
              meta: { title: 'Channel Settings', requiresAuth: true },
            },
            {
              path: 'channel/list',
              name: 'ChannelListSettings',
              component: () => import('@/views/channel/ChannelManagement.vue'),
              meta: { title: 'Channel List', requiresAuth: true },
            },
            {
              path: 'channel/price-ratio',
              name: 'ChannelPriceRatioSettings',
              component: () => import('@/views/channel/ChannelManagement.vue'),
              meta: { title: 'Price Ratio', requiresAuth: true },
            },
            {
              path: 'independent-site',
              name: 'IndependentSiteSettings',
              component: () => import('@/views/settings/independent-site/IndependentSiteList.vue'),
              meta: {
                title: '独立站',
                requiresAuth: true,
                requiredFeatures: [SAAS_FEATURE_CODES.INDEPENDENT_WEBSITE],
              },
            },
            {
              path: 'independent-site/:id',
              name: 'IndependentSiteDetail',
              component: () =>
                import('@/views/settings/independent-site/IndependentSiteDetail.vue'),
              meta: {
                title: '独立站',
                requiresAuth: true,
                requiredFeatures: [SAAS_FEATURE_CODES.INDEPENDENT_WEBSITE],
              },
            },
            {
              path: 'package-settings',
              name: 'PackageSettings',
              component: () => import('@/views/settings/PackageSettings.vue'),
              meta: { title: 'Package Settings', requiresAuth: true },
            },
            {
              path: 'queue-settings',
              name: 'QueueSettings',
              component: () => import('@/views/settings/QueueSettings.vue'),
              meta: { title: 'Sort Settings', requiresAuth: true },
            },
            {
              path: 'booking-function',
              name: 'BookingFunction',
              component: () => import('@/views/settings/BookingFunction.vue'),
              meta: { title: 'Booking Function Settings', requiresAuth: true },
            },
            {
              path: 'automation',
              name: 'Automation',
              component: () => import('@/views/settings/Automation.vue'),
              meta: { title: 'Front Desk Automation', requiresAuth: true },
            },
            {
              path: 'payment-methods',
              name: 'PaymentMethods',
              component: () => import('@/views/settings/PaymentMethods.vue'),
              meta: { title: 'Payment Methods', requiresAuth: true },
            },
            // Finance management
            {
              path: 'finance/note-settings',
              name: 'NoteSettings',
              component: () => import('@/views/settings/finance/NoteSettings.vue'),
              meta: { title: 'Record Settings', requiresAuth: true },
            },
            {
              path: 'finance/managed-operation-settlement',
              name: 'ManagedOperationSettlement',
              component: () => import('@/views/settings/finance/ManagedOperationSettlement.vue'),
              meta: { title: 'Managed Operation Settlement', requiresAuth: true },
            },
            // Account management
            {
              path: 'account/account-list',
              name: 'AccountList',
              component: () => import('@/views/settings/account/AccountList.vue'),
              meta: { title: 'Account List', requiresAuth: true },
            },
            {
              path: 'account/role-management',
              name: 'RoleManagement',
              component: () => import('@/views/settings/account/RoleManagement.vue'),
              meta: { title: 'Role Management', requiresAuth: true },
            },
          ],
        },
        {
          path: 'channel',
          redirect: '/settings/channel/list',
        },
        {
          path: 'order',
          name: 'Order',
          component: () => import('@/views/order/OrderManagement.vue'),
          meta: { title: 'Order Management', requiresAuth: true },
        },
        {
          path: 'reviews',
          name: 'OtaReviews',
          component: () => import('@/views/review/ReviewManagement.vue'),
          meta: { title: 'Review Center', requiresAuth: true },
        },
        // Data center
        {
          path: 'data-center/overview',
          name: 'DataCenterOverview',
          component: () => import('@/views/data-center/DataCenterOverview.vue'),
          meta: { title: 'Overview', requiresAuth: true },
        },
        {
          path: 'data-center/accommodation',
          name: 'DataCenterAccommodation',
          component: () => import('@/views/data-center/DataCenterAccommodation.vue'),
          meta: { title: 'Accommodation', requiresAuth: true },
        },
        {
          path: 'data-center/notes',
          name: 'DataCenterNotes',
          component: () => import('@/views/data-center/DataCenterNotes.vue'),
          meta: { title: 'Record Transaction', requiresAuth: true },
        },
        {
          path: 'data-center/registrations',
          name: 'DataCenterRegistrations',
          component: () => import('@/views/data-center/RegistrationReviewList.vue'),
          meta: { title: 'Guest Information Review', requiresAuth: true },
        },
        {
          path: 'data-center/registrations/:formId',
          name: 'DataCenterRegistrationDetail',
          component: () => import('@/views/data-center/RegistrationReviewDetail.vue'),
          meta: { title: 'Registration Details', requiresAuth: true },
        },
        {
          path: 'statistics/business-summary',
          name: 'BusinessSummary',
          redirect: { path: '/data-center/overview', query: { tab: 'business' } },
          meta: { title: 'Business Summary', requiresAuth: true },
        },
        {
          path: 'statistics/channel-summary',
          name: 'ChannelSummary',
          redirect: { path: '/data-center/overview', query: { tab: 'channel' } },
          meta: { title: 'Channel Summary', requiresAuth: true },
        },
        {
          path: 'statistics/notes-summary',
          name: 'NotesSummary',
          redirect: '/data-center/notes',
          meta: { title: 'Record Summary', requiresAuth: true },
        },
        {
          path: 'statistics/revenue-summary',
          name: 'RevenueSummary',
          component: () => import('@/views/statistics/RevenueSummary.vue'),
          meta: { title: 'Revenue Summary', requiresAuth: true },
        },
        {
          path: 'statistics/operation-report',
          name: 'OperationReport',
          component: () => import('@/views/statistics/OperationReport.vue'),
          meta: { title: 'Operation Report', requiresAuth: true },
        },
        {
          path: 'statistics/accommodation-report',
          name: 'AccommodationReport',
          component: () => import('@/views/statistics/AccommodationReport.vue'),
          meta: { title: 'Accommodation Report', requiresAuth: true },
        },
        {
          path: 'statistics/finance-report',
          name: 'FinanceReport',
          component: () => import('@/views/statistics/FinanceReport.vue'),
          meta: { title: 'Finance Report', requiresAuth: true },
        },
        {
          path: 'statistics',
          redirect: { path: '/data-center/overview', query: { tab: 'business' } },
        },
        {
          path: 'wallet',
          name: 'Wallet',
          component: () => import('@/views/wallet/WalletPage.vue'),
          meta: { title: 'Order Wallet', requiresAuth: true },
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/profile/ProfileCenter.vue'),
          meta: { title: 'Profile', requiresAuth: true },
        },
        {
          path: 'messages',
          name: 'Messages',
          component: () => import('@/views/messages/MessagesPage.vue'),
          meta: { title: 'Messages', requiresAuth: true },
        },
        {
          path: 'notifications/system',
          name: 'SystemNotifications',
          component: () => import('@/views/notifications/SystemNotifications.vue'),
          meta: { title: 'System Notifications', requiresAuth: true },
        },
        {
          path: 'notifications/order',
          name: 'OrderNotifications',
          component: () => import('@/views/notifications/OrderNotifications.vue'),
          meta: { title: 'Order Notifications', requiresAuth: true },
        },
        {
          path: 'forbidden',
          name: 'Forbidden',
          component: () => import('@/views/common/ForbiddenPage.vue'),
          meta: { title: 'Forbidden', requiresAuth: true },
        },
      ],
    },
    // Routes outside the main layout
    {
      path: '/housekeeping/task',
      name: 'HousekeepingTask',
      component: () => import('@/views/housekeeping/HousekeepingTask.vue'),
      meta: { title: 'Housekeeping Task', requiresAuth: true },
    },
    {
      path: '/housekeeping/daily-task',
      name: 'DailyTask',
      component: () => import('@/views/housekeeping/DailyTask.vue'),
      meta: { title: 'Daily Task', requiresAuth: true },
    },
    {
      path: '/housekeeping/statistics',
      name: 'TaskStatistics',
      component: () => import('@/views/housekeeping/TaskStatistics.vue'),
      meta: { title: 'Task Statistics', requiresAuth: true },
    },
    // Store selection route
    {
      path: '/store/selection',
      name: 'StoreSelection',
      component: () => import('@/views/store/StoreSelection.vue'),
      meta: { title: 'Select Store', requiresAuth: true },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginPage.vue'),
      meta: { title: 'Login' },
    },
    {
      path: '/register',
      alias: '/auth/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterPage.vue'),
      meta: { title: 'Register' },
    },
    {
      path: '/cleaner/register',
      name: 'CleanerRegister',
      component: () => import('@/views/cleaner/CleanerRegister.vue'),
      meta: { title: 'Cleaner Register' },
    },
    {
      path: CLEANER_LOGIN_PATH,
      redirect: (to) => ({
        path: LOGIN_PATH,
        query: { ...to.query, workspace: 'CLEANER' },
      }),
    },
    {
      path: '/cleaner/dashboard',
      name: 'CleanerDashboard',
      component: () => import('@/views/cleaner/CleanerDashboard.vue'),
      meta: { title: 'Cleaner Dashboard', requiresAuth: true },
    },
    {
      path: '/cleaner/task/:id',
      name: 'CleanerTaskDetail',
      component: () => import('@/views/cleaner/TaskDetail.vue'),
      meta: { title: 'Task Details', requiresAuth: true },
    },
    {
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('@/views/auth/ForgotPassword.vue'),
      meta: { title: 'Forgot Password' },
    },
    {
      path: '/legal/terms',
      name: 'TermsOfService',
      component: () => import('@/views/legal/TermsOfService.vue'),
      meta: { title: 'Terms of Service' },
    },
    {
      path: '/legal/privacy',
      name: 'PrivacyPolicy',
      component: () => import('@/views/legal/PrivacyPolicy.vue'),
      meta: { title: 'Privacy Policy' },
    },
    {
      path: '/legal/support',
      name: 'TechnicalSupport',
      component: () => import('@/views/legal/TechnicalSupport.vue'),
      meta: { title: 'Technical Support' },
    },
    {
      path: '/share/:token',
      name: 'RoomStatusShareView',
      component: () => import('@/views/share/RoomStatusShareView.vue'),
      meta: { title: 'Room Status Share' },
    },
    // SaaS 平台管理端：公开登录页 + AdminLayout 工作区（adminToken 独立会话，
    // 守卫在最前面的 admin 分支处理，跳过门店守卫与 RBAC）
    {
      path: ADMIN_LOGIN_PATH,
      name: 'AdminLogin',
      component: () => import('@/views/admin/AdminLogin.vue'),
      meta: { title: 'Admin Login', workspace: 'admin' },
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: ADMIN_DASHBOARD_PATH,
      meta: { workspace: 'admin' },
      children: [
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('@/views/admin/AdminDashboard.vue'),
          meta: { title: 'Dashboard', workspace: 'admin' },
        },
        {
          path: 'packages',
          name: 'AdminPackages',
          component: () => import('@/views/admin/AdminPackages.vue'),
          meta: { title: 'Packages', workspace: 'admin' },
        },
        {
          path: 'features',
          name: 'AdminFeatures',
          component: () => import('@/views/admin/AdminFeatures.vue'),
          meta: { title: 'Features', workspace: 'admin' },
        },
        {
          path: 'subscriptions',
          name: 'AdminSubscriptions',
          component: () => import('@/views/admin/AdminSubscriptions.vue'),
          meta: { title: 'Subscriptions', workspace: 'admin' },
        },
        {
          path: 'quota',
          name: 'AdminQuotaAdjust',
          component: () => import('@/views/admin/AdminQuotaAdjust.vue'),
          meta: { title: 'Quota Adjust', workspace: 'admin' },
        },
      ],
    },
  ],
})

const routePermissionConfig = new Map<
  string,
  { requirements: PermissionRequirement[]; matchMode?: PermissionMatchMode }
>([
  [
    'RoomStatusCalendar',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_STATUS },
      ],
    },
  ],
  [
    'RoomStatusDaily',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_STATUS },
      ],
    },
  ],
  [
    'RoomStatusChannel',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_STATUS },
      ],
    },
  ],
  [
    'RoomPriceManagement',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_PRICE },
      ],
    },
  ],
  [
    'RoomPriceBulkUpdate',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.BATCH_CHANGE_PRICE },
      ],
    },
  ],
  [
    'PriceChangeHistory',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_PRICE_LOG },
      ],
    },
  ],
  [
    'RoomTable',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_INFO },
      ],
    },
  ],
  [
    'HousekeepingList',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST },
      ],
    },
  ],
  [
    'HousekeeperList',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST },
      ],
    },
  ],
  [
    'CleaningOverview',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST },
      ],
    },
  ],
  [
    'CleaningTaskList',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST },
      ],
    },
  ],
  [
    'HousekeepingTask',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST },
      ],
    },
  ],
  [
    'DailyTask',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST },
      ],
    },
  ],
  [
    'TaskStatistics',
    {
      requirements: [
        { module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST },
      ],
    },
  ],
  [
    'ChannelListSettings',
    {
      requirements: [{ module: PermissionModule.CHANNEL, action: PermissionAction.VIEW_CHANNELS }],
    },
  ],
  [
    'ChannelPriceRatioSettings',
    {
      requirements: [
        { module: PermissionModule.CHANNEL, action: PermissionAction.VIEW_CHANNELS },
        { module: PermissionModule.CHANNEL, action: PermissionAction.MANAGE_CHANNELS },
      ],
      matchMode: 'any',
    },
  ],
  [
    'IndependentSiteSettings',
    {
      requirements: [
        { module: PermissionModule.CHANNEL, action: PermissionAction.VIEW_CHANNELS },
        { module: PermissionModule.CHANNEL, action: PermissionAction.MANAGE_CHANNELS },
      ],
    },
  ],
  [
    'IndependentSiteDetail',
    {
      requirements: [
        { module: PermissionModule.CHANNEL, action: PermissionAction.VIEW_CHANNELS },
        { module: PermissionModule.CHANNEL, action: PermissionAction.MANAGE_CHANNELS },
      ],
    },
  ],
  [
    'Order',
    { requirements: [{ module: PermissionModule.ORDER, action: PermissionAction.VIEW_ORDERS }] },
  ],
  [
    'OrderNotifications',
    { requirements: [{ module: PermissionModule.ORDER, action: PermissionAction.VIEW_ORDERS }] },
  ],
  [
    'OtaReviews',
    { requirements: [{ module: PermissionModule.REVIEW, action: PermissionAction.VIEW }] },
  ],
  [
    'DataCenterOverview',
    {
      requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
    },
  ],
  [
    'DataCenterAccommodation',
    {
      requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
    },
  ],
  [
    'DataCenterNotes',
    {
      requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
    },
  ],
  [
    'DataCenterRegistrations',
    {
      requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
    },
  ],
  [
    'DataCenterRegistrationDetail',
    {
      requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
    },
  ],
  [
    'BusinessSummary',
    {
      requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
    },
  ],
  [
    'ChannelSummary',
    {
      requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
    },
  ],
  [
    'NotesSummary',
    {
      requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
    },
  ],
  [
    'OperationReport',
    {
      requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
    },
  ],
  [
    'AccommodationReport',
    {
      requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
    },
  ],
  [
    'RevenueSummary',
    {
      requirements: [
        { module: PermissionModule.SENSITIVE, action: PermissionAction.VIEW_FINANCIAL_DATA },
      ],
    },
  ],
  [
    'FinanceReport',
    {
      requirements: [
        { module: PermissionModule.SENSITIVE, action: PermissionAction.VIEW_FINANCIAL_DATA },
      ],
    },
  ],
  [
    'Wallet',
    {
      requirements: [
        { module: PermissionModule.SENSITIVE, action: PermissionAction.VIEW_FINANCIAL_DATA },
      ],
    },
  ],
  [
    'StoreBasicInfo',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'StoreDetails',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'NotificationSettings',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'AnnouncementSettings',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'GeneralChannelSettings',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'QuickReply',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'AutoMessage',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'AiMessageKnowledge',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'CleaningSettings',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'CleaningSupplies',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'AutoCheckinSettings',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'PricingTools',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'PaymentPlatforms',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'DoorLocks',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'RoomTypeManagement',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'RoomOwnership',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'RoomTypeDetails',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'PricePlan',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'ConsumptionItems',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'RoomGroup',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'RoomSort',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'RoomStatusConfig',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'RoomManagement',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'ChannelSettings',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'PackageSettings',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'QueueSettings',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'BookingFunction',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'Automation',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'PaymentMethods',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'NoteSettings',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'ManagedOperationSettlement',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
      ],
    },
  ],
  [
    'AccountList',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS },
      ],
    },
  ],
  [
    'RoleManagement',
    {
      requirements: [
        { module: PermissionModule.SETTINGS, action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS },
      ],
    },
  ],
])

const routeTitleKeyByName = new Map<string, string>([
  ['PublicRegistration', 'routeTitles.checkinRegistration'],
  ['PublicRegistrationBooking', 'routeTitles.checkinRegistration'],
  ['Home', 'routeTitles.home'],
  ['RoomStatusCalendar', 'routeTitles.roomStatus'],
  ['RoomStatusDaily', 'routeTitles.roomStatusDaily'],
  ['RoomStatusChannel', 'routeTitles.roomStatusChannel'],
  ['RoomPriceManagement', 'routeTitles.roomPriceManagement'],
  ['RoomPriceBulkUpdate', 'routeTitles.bulkUpdate'],
  ['PriceChangeHistory', 'routeTitles.priceChangeHistory'],
  ['RoomTable', 'routeTitles.roomOverview'],
  ['HousekeepingList', 'routeTitles.housekeepingList'],
  ['HousekeeperList', 'routeTitles.cleanerList'],
  ['CleaningOverview', 'routeTitles.taskOverview'],
  ['CleaningTaskList', 'routeTitles.taskList'],
  ['StoreBasicInfo', 'routeTitles.basicInfo'],
  ['StoreDetails', 'routeTitles.storeDetails'],
  ['NotificationSettings', 'routeTitles.notificationSettings'],
  ['AnnouncementSettings', 'routeTitles.announcementSettings'],
  ['GeneralChannelSettings', 'routeTitles.channelSettings'],
  ['QuickReply', 'routeTitles.quickReply'],
  ['AutoMessage', 'routeTitles.automationMessages'],
  ['AiMessageKnowledge', 'routeTitles.aiMessageKnowledge'],
  ['CleaningSettings', 'routeTitles.settings'],
  ['CleaningSupplies', 'routeTitles.supplies'],
  ['AutoCheckinSettings', 'routeTitles.autoCheckinSettings'],
  ['PricingTools', 'routeTitles.pricingTools'],
  ['PaymentPlatforms', 'routeTitles.paymentPlatforms'],
  ['DoorLocks', 'routeTitles.doorLocks'],
  ['RoomTypeManagement', 'routeTitles.roomSettings'],
  ['RoomOwnership', 'routeTitles.roomOwnership'],
  ['RoomTypeDetails', 'routeTitles.roomTypeDetails'],
  ['PricePlan', 'routeTitles.pricePlan'],
  ['ConsumptionItems', 'routeTitles.consumptionItemSettings'],
  ['RoomGroup', 'routeTitles.roomGroupSettings'],
  ['RoomSort', 'routeTitles.sortSettings'],
  ['RoomStatusConfig', 'routeTitles.roomGroupSettings'],
  ['RoomManagement', 'routeTitles.roomManagement'],
  ['ChannelSettings', 'routeTitles.channelSettings'],
  ['PackageSettings', 'routeTitles.packageSettings'],
  ['QueueSettings', 'routeTitles.sortSettings'],
  ['BookingFunction', 'routeTitles.bookingFunctionSettings'],
  ['Automation', 'routeTitles.frontDeskAutomation'],
  ['PaymentMethods', 'routeTitles.paymentMethods'],
  ['NoteSettings', 'routeTitles.recordSettings'],
  ['ManagedOperationSettlement', 'routeTitles.managedOperationSettlement'],
  ['AccountList', 'routeTitles.accountList'],
  ['RoleManagement', 'routeTitles.roleManagement'],
  ['ChannelListSettings', 'routeTitles.channelManagement'],
  ['ChannelPriceRatioSettings', 'routeTitles.channelManagement'],
  ['Order', 'routeTitles.orderManagement'],
  ['OtaReviews', 'routeTitles.otaReviews'],
  ['DataCenterOverview', 'routeTitles.overview'],
  ['DataCenterAccommodation', 'routeTitles.accommodation'],
  ['DataCenterNotes', 'routeTitles.recordTransaction'],
  ['DataCenterRegistrations', 'routeTitles.guestInformationReview'],
  ['DataCenterRegistrationDetail', 'routeTitles.registrationDetails'],
  ['BusinessSummary', 'routeTitles.businessSummary'],
  ['ChannelSummary', 'routeTitles.channelSummary'],
  ['NotesSummary', 'routeTitles.recordSummary'],
  ['RevenueSummary', 'routeTitles.revenueSummary'],
  ['OperationReport', 'routeTitles.operationReport'],
  ['AccommodationReport', 'routeTitles.accommodationReport'],
  ['FinanceReport', 'routeTitles.financeReport'],
  ['Wallet', 'routeTitles.orderWallet'],
  ['Profile', 'routeTitles.profile'],
  ['Messages', 'routeTitles.messages'],
  ['SystemNotifications', 'routeTitles.systemNotifications'],
  ['OrderNotifications', 'routeTitles.orderNotifications'],
  ['Forbidden', 'routeTitles.forbidden'],
  ['HousekeepingTask', 'routeTitles.housekeepingTask'],
  ['DailyTask', 'routeTitles.dailyTask'],
  ['TaskStatistics', 'routeTitles.taskStatistics'],
  ['StoreSelection', 'routeTitles.storeSelection'],
  ['Login', 'routeTitles.login'],
  ['Register', 'routeTitles.register'],
  ['CleanerRegister', 'routeTitles.cleanerRegister'],
  ['CleanerDashboard', 'routeTitles.cleanerDashboard'],
  ['CleanerTaskDetail', 'routeTitles.taskDetails'],
  ['ForgotPassword', 'routeTitles.forgotPassword'],
  ['TermsOfService', 'routeTitles.termsOfService'],
  ['PrivacyPolicy', 'routeTitles.privacyPolicy'],
  ['TechnicalSupport', 'routeTitles.technicalSupport'],
  ['RoomStatusShareView', 'routeTitles.roomStatusShare'],
  ['AdminLogin', 'admin.routeTitles.login'],
  ['AdminDashboard', 'admin.routeTitles.dashboard'],
  ['AdminPackages', 'admin.routeTitles.packages'],
  ['AdminFeatures', 'admin.routeTitles.features'],
  ['AdminSubscriptions', 'admin.routeTitles.subscriptions'],
  ['AdminQuotaAdjust', 'admin.routeTitles.quota'],
])

router.getRoutes().forEach((route) => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  const config = routePermissionConfig.get(routeName)
  if (!config) {
    return
  }

  route.meta.requiredPermissions = config.requirements
  route.meta.permissionMatchMode = config.matchMode ?? 'all'
})

router.getRoutes().forEach((route) => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  const titleKey = routeTitleKeyByName.get(routeName)
  if (titleKey) {
    route.meta.titleKey = titleKey
  }
})

// Route guard: validate session state
router.beforeEach(async (to, from, next) => {
  // 平台管理端分流（必须在 PMS/cleaner token 检查之前）：admin 会话独立于门店体系，
  // 管理员不属于任何门店，跳过门店守卫与 RBAC。
  if (isAdminWorkspacePath(to.path)) {
    if (hasCompleteAdminSession()) {
      next()
    } else {
      next({ path: ADMIN_LOGIN_PATH, query: { redirect: to.fullPath } })
    }
    return
  }
  if (to.path === ADMIN_LOGIN_PATH) {
    if (hasCompleteAdminSession()) {
      next(ADMIN_DASHBOARD_PATH)
      return
    }
    next()
    return
  }

  const isCleanerRoute = isCleanerWorkspacePath(to.path)
  const pmsToken = localStorage.getItem(PMS_TOKEN_KEY)
  const cleanerToken = localStorage.getItem(CLEANER_TOKEN_KEY)
  const currentStoreStr = localStorage.getItem(PMS_CURRENT_STORE_KEY)
  let hasCurrentStore = false
  const loginSessionTarget = to.path === LOGIN_PATH ? resolveCachedLoginSessionTarget() : null
  const hasCleanerSession =
    loginSessionTarget === 'CLEANER' || (to.path !== LOGIN_PATH && hasCompleteCleanerSession())

  const activeToken = isCleanerRoute ? cleanerToken : pmsToken

  // Check whether a store is selected
  if (currentStoreStr) {
    try {
      const store = JSON.parse(currentStoreStr)
      hasCurrentStore = !!store.id
    } catch {
      // Ignore invalid cached store data
    }
  }

  // Redirect authenticated routes without the matching token to login
  if (to.meta.requiresAuth && !activeToken) {
    if (isCleanerRoute) {
      clearCleanerSession()
    }
    next(LOGIN_PATH)
    return
  }

  if (isCleanerRoute && to.meta.requiresAuth && !hasCleanerSession) {
    clearCleanerSession()
    next(LOGIN_PATH)
    return
  }

  // Redirect signed-in cleaners away from the unified login page
  if (to.path === LOGIN_PATH && loginSessionTarget === 'CLEANER') {
    next(CLEANER_DASHBOARD_PATH)
    return
  }

  // Redirect signed-in PMS users away from auth pages
  if (
    (to.path === LOGIN_PATH && loginSessionTarget === 'PMS') ||
    (to.path === REGISTER_PATH && pmsToken)
  ) {
    next('/')
    return
  }

  // Store guard: PMS users must select a store before main features
  // Exclude store selection, auth pages, and public cleaner pages
  const storeRelatedPaths = [
    STORE_SELECTION_PATH,
    LOGIN_PATH,
    REGISTER_PATH,
    FORGOT_PASSWORD_PATH,
    CLEANER_REGISTER_PATH,
  ]
  const isStoreRelatedPath = storeRelatedPaths.some(
    (path) => to.path === path || to.path.startsWith(path),
  )

  if (
    pmsToken &&
    !isCleanerRoute &&
    to.meta.requiresAuth &&
    !isStoreRelatedPath &&
    !hasCurrentStore
  ) {
    // Signed-in PMS user is accessing an authenticated page without a selected store
    // Redirect to store selection
    next('/store/selection')
    return
  }

  if (!isCleanerRoute && to.name !== 'Forbidden' && hasCurrentStore) {
    const requiredPermissions = to.meta.requiredPermissions as PermissionRequirement[] | undefined
    const permissionMatchMode =
      (to.meta.permissionMatchMode as PermissionMatchMode | undefined) || 'all'

    if (requiredPermissions?.length) {
      const permissionStore = usePermissionStore(pinia)
      try {
        await permissionStore.fetchCurrentStorePermissions()
      } catch {
        next({
          name: 'Forbidden',
          query: { from: to.fullPath },
        })
        return
      }

      if (!permissionStore.hasPermissions(requiredPermissions, permissionMatchMode)) {
        next({
          name: 'Forbidden',
          query: { from: to.fullPath },
        })
        return
      }
    }
  }

  // SaaS 权益门禁（RBAC 之后）：meta.requiredFeatures 校验套餐权益。
  // entitlement store 拉取失败时 fail-open 不拦截，真正的拦截由后端 402 兜底并走升级引导。
  if (!isCleanerRoute && hasCurrentStore) {
    const requiredFeatures = to.meta.requiredFeatures as string[] | undefined
    if (requiredFeatures?.length) {
      const entitlementStore = useEntitlementStore(pinia)
      await entitlementStore.refresh()
      const missingFeature = requiredFeatures.find(
        (featureCode) => !entitlementStore.hasFeature(featureCode),
      )
      if (missingFeature) {
        // P9：补传 reason——无订阅门店显示「先购买套餐」专属文案，有订阅走功能未包含引导
        entitlementStore.openUpgradeGuide({
          featureCode: missingFeature,
          reason: entitlementStore.subscription ? 'NOT_INCLUDED' : 'NO_SUBSCRIPTION',
        })
        next({ name: 'PackageSettings', query: { from: to.fullPath } })
        return
      }
    }
  }

  next()
})

export default router
