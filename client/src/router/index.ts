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
      meta: { title: '入住登记', requiresAuth: false },
    },
    // Public booking summary (no login)
    {
      path: '/rb/:bookingKey',
      name: 'PublicRegistrationBooking',
      component: () => import('@/views/public/RegistrationBookingPublic.vue'),
      meta: { title: '入住登记', requiresAuth: false },
    },
    // 主布局路由（包含导航栏）
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Home',
          component: () => import('@/views/home/HomePage.vue'),
          meta: { title: '首页', requiresAuth: true },
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
                  meta: { title: '房态', requiresAuth: true },
                },
                {
                  path: 'daily',
                  name: 'RoomStatusDaily',
                  component: () => import('@/views/room-status/RoomStatusDaily.vue'),
                  meta: { title: '房态-单日', requiresAuth: true },
                },
                {
                  path: 'channel',
                  name: 'RoomStatusChannel',
                  component: () => import('@/views/room-status/RoomStatusChannel.vue'),
                  meta: { title: '房态-渠道', requiresAuth: true },
                },
              ],
            },
            {
              path: 'room-price-management',
              name: 'RoomPriceManagement',
              component: () => import('@/views/accommodation/RoomPriceManagement.vue'),
              meta: { title: '房价管理', requiresAuth: true },
            },
            {
              path: 'room-price-bulk-update',
              name: 'RoomPriceBulkUpdate',
              component: () => import('@/views/accommodation/RoomPriceBulkUpdate.vue'),
              meta: { title: '批量更新', requiresAuth: true },
            },
            {
              path: 'room-price/change-history',
              name: 'PriceChangeHistory',
              component: () => import('@/views/accommodation/PriceChangeHistory.vue'),
              meta: { title: '改价记录', requiresAuth: true },
            },
            {
              path: 'room-price/bulk-change',
              name: 'BulkPriceChange',
              component: () => import('@/views/accommodation/BulkPriceChange.vue'),
              meta: { title: '批量改价', requiresAuth: true },
            },
            {
              path: 'room-table',
              name: 'RoomTable',
              component: () => import('@/views/accommodation/RoomTable.vue'),
              meta: { title: '房情表', requiresAuth: true },
            },
            {
              path: 'meals-management',
              name: 'MealsManagement',
              component: () => import('@/views/accommodation/MealsManagement.vue'),
              meta: { title: '餐食', requiresAuth: true },
            },
            {
              path: 'breakfast-package',
              name: 'BreakfastPackage',
              component: () => import('@/views/accommodation/BreakfastPackage.vue'),
              meta: { title: '早餐套销', requiresAuth: true },
            },
            {
              path: 'housekeeping-list',
              name: 'HousekeepingList',
              component: () => import('@/views/accommodation/HousekeepingList.vue'),
              meta: { title: '房务列表', requiresAuth: true },
            },
            {
              path: 'housekeeper-list',
              name: 'HousekeeperList',
              component: () => import('@/views/accommodation/HousekeeperList.vue'),
              meta: { title: '保洁员列表', requiresAuth: true },
            },
            {
              path: 'cleaning/overview',
              name: 'CleaningOverview',
              component: () => import('@/views/accommodation/cleaning/CleaningOverview.vue'),
              meta: { title: '任务概览', requiresAuth: true },
            },
            {
              path: 'cleaning/task-list',
              name: 'CleaningTaskList',
              component: () => import('@/views/accommodation/cleaning/CleaningTaskList.vue'),
              meta: { title: '任务列表', requiresAuth: true },
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
            // 门店设置
            {
              path: 'store/basic-info',
              name: 'StoreBasicInfo',
              component: () => import('@/views/settings/store/StoreBasicInfo.vue'),
              meta: { title: '基本信息', requiresAuth: true },
            },
            {
              path: 'store/details',
              name: 'StoreDetails',
              component: () => import('@/views/settings/store/StoreDetails.vue'),
              meta: { title: '门店详情', requiresAuth: true },
            },
            // 通用设置
            {
              path: 'general/notification',
              name: 'NotificationSettings',
              component: () => import('@/views/settings/general/NotificationSettings.vue'),
              meta: { title: '通知设置', requiresAuth: true },
            },
            {
              path: 'general/channel',
              name: 'GeneralChannelSettings',
              component: () => import('@/views/settings/general/ChannelSettings.vue'),
              meta: { title: '渠道设置', requiresAuth: true },
            },
            {
              path: 'general/quick-reply',
              name: 'QuickReply',
              component: () => import('@/views/settings/general/QuickReply.vue'),
              meta: { title: '快捷回复', requiresAuth: true },
            },
            {
              path: 'general/auto-message',
              name: 'AutoMessage',
              component: () => import('@/views/settings/general/AutoMessage.vue'),
              meta: { title: '自动化消息', requiresAuth: true },
            },
            // 保洁设置
            {
              path: 'cleaning/settings',
              name: 'CleaningSettings',
              component: () => import('@/views/settings/cleaning/CleaningSettings.vue'),
              meta: { title: '设置', requiresAuth: true },
            },
            {
              path: 'cleaning/supplies',
              name: 'CleaningSupplies',
              component: () => import('@/views/settings/cleaning/CleaningSupplies.vue'),
              meta: { title: '易耗品', requiresAuth: true },
            },
            // 自动入住
            {
              path: 'auto-checkin/settings',
              name: 'AutoCheckinSettings',
              component: () => import('@/views/settings/auto-checkin/AutoCheckinSettings.vue'),
              meta: { title: '自动入住设置', requiresAuth: true },
            },
            // 第三方集成
            {
              path: 'third-party/pricing-tools',
              name: 'PricingTools',
              component: () => import('@/views/settings/third-party/PricingTools.vue'),
              meta: { title: '定价工具', requiresAuth: true },
            },
            {
              path: 'third-party/payment-platforms',
              name: 'PaymentPlatforms',
              component: () => import('@/views/settings/third-party/PaymentPlatforms.vue'),
              meta: { title: '支付平台', requiresAuth: true },
            },
            // 原有设置
            {
              path: 'room-type',
              name: 'RoomTypeManagement',
              component: () => import('@/views/settings/room/RoomSettings.vue'),
              meta: { title: '房间设置', requiresAuth: true },
            },
            {
              path: 'room/ownership',
              name: 'RoomOwnership',
              component: () => import('@/views/settings/room/RoomOwnership.vue'),
              meta: { title: '房间归属', requiresAuth: true },
            },
            {
              path: 'room-type/:id/details',
              name: 'RoomTypeDetails',
              component: () => import('@/views/settings/room/RoomTypeDetails.vue'),
              meta: { title: '房型详情', requiresAuth: true },
            },
            // 住宿设置 - 新增页面
            {
              path: 'room/price-plan',
              name: 'PricePlan',
              component: () => import('@/views/settings/room/PricePlan.vue'),
              meta: { title: '价格计划', requiresAuth: true },
            },
            {
              path: 'room/consumption-items',
              name: 'ConsumptionItems',
              component: () => import('@/views/settings/room/ConsumptionItems.vue'),
              meta: { title: '消费项设置', requiresAuth: true },
            },
            {
              path: 'room/room-group',
              name: 'RoomGroup',
              component: () => import('@/views/settings/room/RoomGroup.vue'),
              meta: { title: '房间分组设置', requiresAuth: true },
            },
            {
              path: 'room/room-sort',
              name: 'RoomSort',
              component: () => import('@/views/settings/room/RoomSort.vue'),
              meta: { title: '排序设置', requiresAuth: true },
            },
            {
              path: 'room-status-config',
              name: 'RoomStatusConfig',
              component: () => import('@/views/settings/RoomStatusConfig.vue'),
              meta: { title: '房间分组设置', requiresAuth: true },
            },
            {
              path: 'room-management',
              name: 'RoomManagement',
              component: () => import('@/views/settings/RoomManagement.vue'),
              meta: { title: '房间管理', requiresAuth: true },
            },
            {
              path: 'channel-settings',
              name: 'ChannelSettings',
              component: () => import('@/views/settings/ChannelSettings.vue'),
              meta: { title: '渠道设置', requiresAuth: true },
            },
            {
              path: 'package-settings',
              name: 'PackageSettings',
              component: () => import('@/views/settings/PackageSettings.vue'),
              meta: { title: '包栋设置', requiresAuth: true },
            },
            {
              path: 'queue-settings',
              name: 'QueueSettings',
              component: () => import('@/views/settings/QueueSettings.vue'),
              meta: { title: '排序设置', requiresAuth: true },
            },
            {
              path: 'booking-function',
              name: 'BookingFunction',
              component: () => import('@/views/settings/BookingFunction.vue'),
              meta: { title: '订单功能设置', requiresAuth: true },
            },
            {
              path: 'automation',
              name: 'Automation',
              component: () => import('@/views/settings/Automation.vue'),
              meta: { title: '前台自动化', requiresAuth: true },
            },
            {
              path: 'payment-methods',
              name: 'PaymentMethods',
              component: () => import('@/views/settings/PaymentMethods.vue'),
              meta: { title: '收款方式', requiresAuth: true },
            },
            // 财务管理
            {
              path: 'finance/note-settings',
              name: 'NoteSettings',
              component: () => import('@/views/settings/finance/NoteSettings.vue'),
              meta: { title: '记一笔设置', requiresAuth: true },
            },
            // 账号管理
            {
              path: 'account/account-list',
              name: 'AccountList',
              component: () => import('@/views/settings/account/AccountList.vue'),
              meta: { title: '账号列表', requiresAuth: true },
            },
            {
              path: 'account/role-management',
              name: 'RoleManagement',
              component: () => import('@/views/settings/account/RoleManagement.vue'),
              meta: { title: '角色管理', requiresAuth: true },
            },
          ],
        },
        {
          path: 'channel',
          name: 'Channel',
          component: () => import('@/views/channel/ChannelManagement.vue'),
          meta: { title: '渠道管理', requiresAuth: true },
        },
        {
          path: 'order',
          name: 'Order',
          component: () => import('@/views/order/OrderManagement.vue'),
          meta: { title: '订单管理', requiresAuth: true },
        },
        // 数据中心
        {
          path: 'data-center/overview',
          name: 'DataCenterOverview',
          component: () => import('@/views/data-center/DataCenterOverview.vue'),
          meta: { title: '总览', requiresAuth: true },
        },
        {
          path: 'data-center/accommodation',
          name: 'DataCenterAccommodation',
          component: () => import('@/views/data-center/DataCenterAccommodation.vue'),
          meta: { title: '住宿', requiresAuth: true },
        },
        {
          path: 'data-center/notes',
          name: 'DataCenterNotes',
          component: () => import('@/views/data-center/DataCenterNotes.vue'),
          meta: { title: '记一笔', requiresAuth: true },
        },
        {
          path: 'data-center/registrations',
          name: 'DataCenterRegistrations',
          component: () => import('@/views/data-center/RegistrationReviewList.vue'),
          meta: { title: '人员信息审查', requiresAuth: true },
        },
        {
          path: 'data-center/registrations/:formId',
          name: 'DataCenterRegistrationDetail',
          component: () => import('@/views/data-center/RegistrationReviewDetail.vue'),
          meta: { title: '登记详情', requiresAuth: true },
        },
        {
          path: 'statistics/business-summary',
          name: 'BusinessSummary',
          component: () => import('@/views/statistics/BusinessSummary.vue'),
          meta: { title: '营业汇总', requiresAuth: true },
        },
        {
          path: 'statistics/channel-summary',
          name: 'ChannelSummary',
          component: () => import('@/views/statistics/ChannelSummary.vue'),
          meta: { title: '渠道汇总', requiresAuth: true },
        },
        {
          path: 'statistics/notes-summary',
          name: 'NotesSummary',
          component: () => import('@/views/statistics/NotesSummary.vue'),
          meta: { title: '记一笔汇总', requiresAuth: true },
        },
        {
          path: 'statistics/revenue-summary',
          name: 'RevenueSummary',
          component: () => import('@/views/statistics/RevenueSummary.vue'),
          meta: { title: '流水汇总', requiresAuth: true },
        },
        {
          path: 'statistics/operation-report',
          name: 'OperationReport',
          component: () => import('@/views/statistics/OperationReport.vue'),
          meta: { title: '经营报表', requiresAuth: true },
        },
        {
          path: 'statistics/accommodation-report',
          name: 'AccommodationReport',
          component: () => import('@/views/statistics/AccommodationReport.vue'),
          meta: { title: '住宿报表', requiresAuth: true },
        },
        {
          path: 'statistics/finance-report',
          name: 'FinanceReport',
          component: () => import('@/views/statistics/FinanceReport.vue'),
          meta: { title: '财务报表', requiresAuth: true },
        },
        {
          path: 'statistics',
          redirect: '/statistics/business-summary',
        },
        {
          path: 'wallet',
          name: 'Wallet',
          component: () => import('@/views/wallet/WalletPage.vue'),
          meta: { title: '订单钱包', requiresAuth: true },
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/profile/ProfileCenter.vue'),
          meta: { title: '个人中心', requiresAuth: true },
        },
        {
          path: 'messages',
          name: 'Messages',
          component: () => import('@/views/messages/MessagesPage.vue'),
          meta: { title: '消息', requiresAuth: true },
        },
        {
          path: 'notifications/system',
          name: 'SystemNotifications',
          component: () => import('@/views/notifications/SystemNotifications.vue'),
          meta: { title: '系统通知', requiresAuth: true },
        },
        {
          path: 'notifications/order',
          name: 'OrderNotifications',
          component: () => import('@/views/notifications/OrderNotifications.vue'),
          meta: { title: '订单通知', requiresAuth: true },
        },
        {
          path: 'forbidden',
          name: 'Forbidden',
          component: () => import('@/views/common/ForbiddenPage.vue'),
          meta: { title: '暂无权限', requiresAuth: true },
        },
      ],
    },
    // 空白布局路由（登录、注册等）
    {
      path: '/housekeeping/task',
      name: 'HousekeepingTask',
      component: () => import('@/views/housekeeping/HousekeepingTask.vue'),
      meta: { title: '房务任务', requiresAuth: true },
    },
    {
      path: '/housekeeping/daily-task',
      name: 'DailyTask',
      component: () => import('@/views/housekeeping/DailyTask.vue'),
      meta: { title: '每日任务', requiresAuth: true },
    },
    {
      path: '/housekeeping/statistics',
      name: 'TaskStatistics',
      component: () => import('@/views/housekeeping/TaskStatistics.vue'),
      meta: { title: '任务统计', requiresAuth: true },
    },
    // 门店选择路由
    {
      path: '/store/selection',
      name: 'StoreSelection',
      component: () => import('@/views/store/StoreSelection.vue'),
      meta: { title: '选择门店', requiresAuth: true },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginPage.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterPage.vue'),
      meta: { title: '注册' },
    },
    {
      path: '/cleaner/register',
      name: 'CleanerRegister',
      component: () => import('@/views/cleaner/CleanerRegister.vue'),
      meta: { title: '保洁员注册' },
    },
    {
      path: '/cleaner/login',
      name: 'CleanerLogin',
      component: () => import('@/views/cleaner/CleanerLogin.vue'),
      meta: { title: '保洁员登录' },
    },
    {
      path: '/cleaner/dashboard',
      name: 'CleanerDashboard',
      component: () => import('@/views/cleaner/CleanerDashboard.vue'),
      meta: { title: '保洁工作台', requiresAuth: true },
    },
    {
      path: '/cleaner/task/:id',
      name: 'CleanerTaskDetail',
      component: () => import('@/views/cleaner/TaskDetail.vue'),
      meta: { title: '任务详情', requiresAuth: true },
    },
    {
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('@/views/auth/ForgotPassword.vue'),
      meta: { title: '忘记密码' },
    },
    {
      path: '/legal/terms',
      name: 'TermsOfService',
      component: () => import('@/views/legal/TermsOfService.vue'),
      meta: { title: '用户服务协议' },
    },
    {
      path: '/legal/privacy',
      name: 'PrivacyPolicy',
      component: () => import('@/views/legal/PrivacyPolicy.vue'),
      meta: { title: '隐私政策' },
    },
    {
      path: '/legal/support',
      name: 'TechnicalSupport',
      component: () => import('@/views/legal/TechnicalSupport.vue'),
      meta: { title: '技术支持网站' },
    },
    {
      path: '/share/:token',
      name: 'RoomStatusShareView',
      component: () => import('@/views/share/RoomStatusShareView.vue'),
      meta: { title: '房态分享' },
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
  ['GeneralChannelSettings', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['QuickReply', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
  ['AutoMessage', { requirements: [{ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS }] }],
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

router.getRoutes().forEach((route) => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  const config = routePermissionConfig.get(routeName)
  if (!config) {
    return
  }

  route.meta.requiredPermissions = config.requirements
  route.meta.permissionMatchMode = config.matchMode ?? 'all'
})

// 路由守卫：检查登录状态
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
      // 解析失败，忽略
    }
  }

  const activeToken = isCleanerRoute ? cleanerToken : pmsToken

  // 检查是否有当前选中的门店
  if (currentStoreStr) {
    try {
      const store = JSON.parse(currentStoreStr)
      hasCurrentStore = !!store.id
    } catch {
      // 解析失败,忽略
    }
  }

  // 如果路由需要认证但没有对应端的token，重定向到对应登录页
  if (to.meta.requiresAuth && !activeToken) {
    next(isCleanerRoute ? '/cleaner/login' : '/login')
    return
  }

  if (isCleanerRoute && to.meta.requiresAuth && !hasCleanerSession) {
    next('/cleaner/login')
    return
  }

  // PMS已登录访问管理员登录/注册页，重定向到管理后台
  if ((to.path === '/login' || to.path === '/register') && pmsToken) {
    next('/')
    return
  }

  // 保洁员已登录访问保洁员登录页，重定向到保洁员工作台
  if (to.path === '/cleaner/login' && cleanerToken && hasCleanerSession) {
    next('/cleaner/dashboard')
    return
  }

  // 门店检查 - 管理员访问主要功能时必须先选择门店
  // 排除门店相关页面、登录注册页面和保洁员公开页面
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
    // 已登录且不是保洁员,访问需要认证的页面,但没有选择门店
    // 重定向到门店选择页面
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
