import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
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
              path: 'room-price/management',
              name: 'RoomPriceManagement',
              component: () => import('@/views/accommodation/RoomPriceManagement.vue'),
              meta: { title: '房价管理', requiresAuth: true },
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
              path: 'room-status-share',
              name: 'RoomStatusShare',
              component: () => import('@/views/accommodation/RoomStatusShare.vue'),
              meta: { title: '房态分享', requiresAuth: true },
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
              component: () => import('@/views/settings/RoomTypeManagement.vue'),
              meta: { title: '房型管理', requiresAuth: true },
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
          path: 'statistics',
          redirect: '/statistics/business-summary',
        },
        {
          path: 'chat',
          name: 'Chat',
          component: () => import('@/views/chat/ChatPage.vue'),
          meta: { title: '智能客服', requiresAuth: true },
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
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('@/views/auth/ForgotPassword.vue'),
      meta: { title: '忘记密码' },
    },
    {
      path: '/share/:token',
      name: 'RoomStatusShareView',
      component: () => import('@/views/share/RoomStatusShareView.vue'),
      meta: { title: '房态分享' },
    },
  ],
})

// 路由守卫：检查登录状态
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  // 如果路由需要认证但没有token，重定向到登录页
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    // 如果已登录访问登录/注册页，重定向到首页
    next('/')
  } else {
    next()
  }
})

export default router
