import { createRouter, createWebHistory } from 'vue-router'
import { PermissionAction, PermissionModule } from '@/api/role'
import { pinia } from '@/stores/pinia'
import {
  usePermissionStore,
  type PermissionMatchMode,
  type PermissionRequirement,
} from '@/stores/permission'

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
              path: 'room-price/bulk-change',
              name: 'BulkPriceChange',
              component: () => import('@/views/accommodation/BulkPriceChange.vue'),
              meta: { title: 'Bulk Rate Change', requiresAuth: true },
            },
            {
              path: 'room-table',
              name: 'RoomTable',
              component: () => import('@/views/accommodation/RoomTable.vue'),
              meta: { title: 'Room Overview', requiresAuth: true },
            },
            {
              path: 'meals-management',
              name: 'MealsManagement',
              component: () => import('@/views/accommodation/MealsManagement.vue'),
              meta: { title: 'Meals', requiresAuth: true },
            },
            {
              path: 'breakfast-package',
              name: 'BreakfastPackage',
              component: () => import('@/views/accommodation/BreakfastPackage.vue'),
              meta: { title: 'Breakfast Bundle', requiresAuth: true },
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
            // Quick tools
            {
              path: 'tools',
              name: 'QuickTools',
              component: () => import('@/views/settings/QuickTools.vue'),
              meta: { title: 'Quick Tools', requiresAuth: true },
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
          name: 'Channel',
          component: () => import('@/views/channel/ChannelManagement.vue'),
          meta: { title: 'Channel Management', requiresAuth: true },
        },
        {
          path: 'order',
          name: 'Order',
          component: () => import('@/views/order/OrderManagement.vue'),
          meta: { title: 'Order Management', requiresAuth: true },
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
          component: () => import('@/views/statistics/BusinessSummary.vue'),
          meta: { title: 'Business Summary', requiresAuth: true },
        },
        {
          path: 'statistics/channel-summary',
          name: 'ChannelSummary',
          component: () => import('@/views/statistics/ChannelSummary.vue'),
          meta: { title: 'Channel Summary', requiresAuth: true },
        },
        {
          path: 'statistics/notes-summary',
          name: 'NotesSummary',
          component: () => import('@/views/statistics/NotesSummary.vue'),
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
          redirect: '/statistics/business-summary',
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
      path: '/cleaner/login',
      name: 'CleanerLogin',
      component: () => import('@/views/cleaner/CleanerLogin.vue'),
      meta: { title: 'Cleaner Login' },
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
  ],
})

const routePermissionConfig = new Map<
  string,
  { requirements: PermissionRequirement[]; matchMode?: PermissionMatchMode }
>([
  ['RoomStatusCalendar', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_STATUS }] }],
  ['RoomStatusDaily', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_STATUS }] }],
  ['RoomStatusChannel', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_STATUS }] }],
  ['RoomPriceManagement', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_PRICE }] }],
  ['RoomPriceBulkUpdate', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.BATCH_CHANGE_PRICE }] }],
  ['PriceChangeHistory', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_PRICE_LOG }] }],
  ['BulkPriceChange', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.BATCH_CHANGE_PRICE }] }],
  ['RoomTable', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_INFO }] }],
  ['HousekeepingList', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST }] }],
  ['HousekeeperList', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST }] }],
  ['CleaningOverview', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST }] }],
  ['CleaningTaskList', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST }] }],
  ['HousekeepingTask', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST }] }],
  ['DailyTask', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST }] }],
  ['TaskStatistics', { requirements: [{ module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST }] }],
  ['Channel', { requirements: [{ module: PermissionModule.CHANNEL, action: PermissionAction.VIEW_CHANNELS }] }],
  ['Order', { requirements: [{ module: PermissionModule.ORDER, action: PermissionAction.VIEW_ORDERS }] }],
  ['OrderNotifications', { requirements: [{ module: PermissionModule.ORDER, action: PermissionAction.VIEW_ORDERS }] }],
  ['DataCenterOverview', { requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }] }],
  ['DataCenterAccommodation', { requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }] }],
  ['DataCenterNotes', { requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }] }],
  ['DataCenterRegistrations', { requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }] }],
  ['DataCenterRegistrationDetail', { requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }] }],
  ['BusinessSummary', { requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }] }],
  ['ChannelSummary', { requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }] }],
  ['NotesSummary', { requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }] }],
  ['OperationReport', { requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }] }],
  ['AccommodationReport', { requirements: [{ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }] }],
  ['RevenueSummary', { requirements: [{ module: PermissionModule.SENSITIVE, action: PermissionAction.VIEW_FINANCIAL_DATA }] }],
  ['FinanceReport', { requirements: [{ module: PermissionModule.SENSITIVE, action: PermissionAction.VIEW_FINANCIAL_DATA }] }],
  ['Wallet', { requirements: [{ module: PermissionModule.SENSITIVE, action: PermissionAction.VIEW_FINANCIAL_DATA }] }],
  ['StoreBasicInfo', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['StoreDetails', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['NotificationSettings', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['AnnouncementSettings', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['GeneralChannelSettings', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['QuickReply', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['AutoMessage', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['AiMessageKnowledge', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['QuickTools', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['CleaningSettings', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['CleaningSupplies', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['AutoCheckinSettings', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['PricingTools', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['PaymentPlatforms', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['RoomTypeManagement', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['RoomOwnership', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['RoomTypeDetails', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['PricePlan', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['ConsumptionItems', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['RoomGroup', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['RoomSort', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['RoomStatusConfig', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['RoomManagement', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['ChannelSettings', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['PackageSettings', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['QueueSettings', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['BookingFunction', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['Automation', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['PaymentMethods', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['NoteSettings', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['AccountList', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS }] }],
  ['RoleManagement', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS }] }],
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
  ['BulkPriceChange', 'routeTitles.bulkPriceChange'],
  ['RoomTable', 'routeTitles.roomOverview'],
  ['MealsManagement', 'routeTitles.meals'],
  ['BreakfastPackage', 'routeTitles.breakfastBundle'],
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
  ['QuickTools', 'settings.layout.items.quickTools'],
  ['CleaningSettings', 'routeTitles.settings'],
  ['CleaningSupplies', 'routeTitles.supplies'],
  ['AutoCheckinSettings', 'routeTitles.autoCheckinSettings'],
  ['PricingTools', 'routeTitles.pricingTools'],
  ['PaymentPlatforms', 'routeTitles.paymentPlatforms'],
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
  ['AccountList', 'routeTitles.accountList'],
  ['RoleManagement', 'routeTitles.roleManagement'],
  ['Channel', 'routeTitles.channelManagement'],
  ['Order', 'routeTitles.orderManagement'],
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
  ['CleanerLogin', 'routeTitles.cleanerLogin'],
  ['CleanerDashboard', 'routeTitles.cleanerDashboard'],
  ['CleanerTaskDetail', 'routeTitles.taskDetails'],
  ['ForgotPassword', 'routeTitles.forgotPassword'],
  ['TermsOfService', 'routeTitles.termsOfService'],
  ['PrivacyPolicy', 'routeTitles.privacyPolicy'],
  ['TechnicalSupport', 'routeTitles.technicalSupport'],
  ['RoomStatusShareView', 'routeTitles.roomStatusShare'],
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
  const isCleanerRoute = to.path.startsWith('/cleaner')
  const pmsToken = localStorage.getItem('token')
  const cleanerToken = localStorage.getItem('cleanerToken')
  const cleanerUserStr = localStorage.getItem('cleanerUser')
  const currentStoreStr = localStorage.getItem('currentStore')
  let hasCurrentStore = false
  let hasCleanerSession = false

  if (cleanerToken && cleanerUserStr) {
    try {
      const cleanerUser = JSON.parse(cleanerUserStr)
      hasCleanerSession = cleanerUser?.isCleaner === true
    } catch {
      // Ignore invalid cached session data
    }
  }

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
    next(isCleanerRoute ? '/cleaner/login' : '/login')
    return
  }

  if (isCleanerRoute && to.meta.requiresAuth && !hasCleanerSession) {
    next('/cleaner/login')
    return
  }

  // Redirect signed-in PMS users away from auth pages
  if ((to.path === '/login' || to.path === '/register') && pmsToken) {
    next('/')
    return
  }

  // Redirect signed-in cleaners away from cleaner login
  if (to.path === '/cleaner/login' && cleanerToken && hasCleanerSession) {
    next('/cleaner/dashboard')
    return
  }

  // Store guard: PMS users must select a store before main features
  // Exclude store selection, auth pages, and public cleaner pages
  const storeRelatedPaths = [
    '/store/selection',
    '/login',
    '/register',
    '/forgot-password',
    '/cleaner/login',
    '/cleaner/register',
  ]
  const isStoreRelatedPath = storeRelatedPaths.some(
    (path) => to.path === path || to.path.startsWith(path)
  )

  if (pmsToken && !isCleanerRoute && to.meta.requiresAuth && !isStoreRelatedPath && !hasCurrentStore) {
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

  next()
})

export default router
