import type { Router } from 'vue-router'
import { hasCleanerSession, hasCleanerStore } from '@/utils/cleanerSession'
import { hasStoredCurrentStore, hasStoredToken } from '@/utils/storage'

const APP_TITLE = '房东智控中心（THE HOST HUB）'

export const ROUTE_PATHS = {
  login: '/auth/login',
  loginCodeVerify: '/auth/login/code-verify',
  register: '/auth/register',
  forgotPassword: '/auth/forgot-password',
  publicRegistrationBooking: '/public/registration-booking/:bookingKey',
  publicRegistrationForm: '/public/registration/:orderNumber',
  publicRoomStatusShare: '/public/share/:token',
  cleanerLogin: '/cleaner/login',
  cleanerRegister: '/cleaner/register',
  cleanerInviteRegister: '/cleaner/invite-register',
  cleanerDashboard: '/cleaner/dashboard',
  cleanerTaskDetail: '/cleaner/task/:id',
  storeSelection: '/store/selection',
  tabsRoot: '/tabs',
  home: '/tabs/home',
  homeCustomize: '/tabs/home/customize',
  rooms: '/tabs/rooms',
  roomsRoomTable: '/tabs/rooms/room-table',
  roomsPricing: '/tabs/rooms/pricing',
  roomsPricingHistory: '/tabs/rooms/pricing/history',
  roomsCleaningOverview: '/tabs/rooms/cleaning/overview',
  roomsCleaningTasks: '/tabs/rooms/cleaning/tasks',
  orders: '/tabs/orders',
  channels: '/tabs/channels',
  channelDetail: '/tabs/channels/:otaId',
  channelMapping: '/tabs/channels/:otaId/mapping',
  channelSync: '/tabs/channels/:otaId/sync',
  channelInventory: '/tabs/channels/:otaId/inventory',
  statistics: '/tabs/statistics',
  statisticsOverview: '/tabs/statistics/overview',
  statisticsAccommodation: '/tabs/statistics/accommodation',
  statisticsNotes: '/tabs/statistics/notes',
  statisticsBusinessSummary: '/tabs/statistics/business-summary',
  statisticsChannelSummary: '/tabs/statistics/channel-summary',
  statisticsNotesSummary: '/tabs/statistics/notes-summary',
  statisticsRevenueSummary: '/tabs/statistics/revenue-summary',
  statisticsOperationReport: '/tabs/statistics/operation-report',
  statisticsAccommodationReport: '/tabs/statistics/accommodation-report',
  statisticsFinanceReport: '/tabs/statistics/finance-report',
  reviews: '/tabs/reviews',
  reviewsLinks: '/tabs/reviews/links',
  reviewsDetail: '/tabs/reviews/:formId',
  messages: '/tabs/messages',
  messageDetail: '/tabs/messages/:threadId',
  systemNotifications: '/tabs/notifications/system',
  orderNotifications: '/tabs/notifications/order',
  wallet: '/tabs/wallet',
  profile: '/tabs/profile',
  settings: '/tabs/settings',
  settingsStoreProfile: '/tabs/settings/store-profile',
  settingsStoreDetails: '/tabs/settings/store-details',
  settingsStoreMembers: '/tabs/settings/store-members',
  settingsRoomTypes: '/tabs/settings/room-types',
  settingsRoomTypesDetail: '/tabs/settings/room-types/:id/details',
  settingsRoomGroups: '/tabs/settings/room-groups',
  settingsRoomSort: '/tabs/settings/room-sort',
  settingsPricePlans: '/tabs/settings/price-plans',
  settingsPricePlanRates: '/tabs/settings/price-plans/:pricePlanId/rates',
  settingsConsumptionItems: '/tabs/settings/consumption-items',
  settingsPaymentMethods: '/tabs/settings/payment-methods',
  settingsNoteSettings: '/tabs/settings/note-settings',
  settingsNotification: '/tabs/settings/notification',
  settingsChannelSettings: '/tabs/settings/channel-settings',
  settingsQuickReplies: '/tabs/settings/quick-replies',
  settingsAutoMessages: '/tabs/settings/auto-messages',
  settingsCleaningSettings: '/tabs/settings/cleaning-settings',
  settingsCleaningSupplies: '/tabs/settings/cleaning-supplies',
  settingsRolePermissions: '/tabs/settings/role-permissions/:roleId',
  settingsPricingTools: '/tabs/settings/pricing-tools',
  settingsPaymentPlatforms: '/tabs/settings/payment-platforms',
} as const

export const buildSettingsRoomTypeDetailPath = (id: number | string) => {
  return ROUTE_PATHS.settingsRoomTypesDetail.replace(':id', String(id))
}

export const buildChannelDetailPath = (otaId: number | string) => {
  return ROUTE_PATHS.channelDetail.replace(':otaId', String(otaId))
}

export const buildSettingsRolePermissionsPath = (roleId: number | string) => {
  return ROUTE_PATHS.settingsRolePermissions.replace(':roleId', String(roleId))
}

export const buildSettingsPricePlanRatesPath = (pricePlanId: number | string) => {
  return ROUTE_PATHS.settingsPricePlanRates.replace(':pricePlanId', String(pricePlanId))
}

export const buildChannelMappingPath = (otaId: number | string) => {
  return ROUTE_PATHS.channelMapping.replace(':otaId', String(otaId))
}

export const buildChannelSyncPath = (otaId: number | string) => {
  return ROUTE_PATHS.channelSync.replace(':otaId', String(otaId))
}

export const buildChannelInventoryPath = (otaId: number | string) => {
  return ROUTE_PATHS.channelInventory.replace(':otaId', String(otaId))
}

export const buildMessageDetailPath = (threadId: number | string) => {
  return ROUTE_PATHS.messageDetail.replace(':threadId', String(threadId))
}

export const buildCleanerTaskDetailPath = (taskId: number | string) => {
  return ROUTE_PATHS.cleanerTaskDetail.replace(':id', String(taskId))
}

export const buildPublicRegistrationBookingPath = (bookingKey: string) => {
  return ROUTE_PATHS.publicRegistrationBooking.replace(':bookingKey', String(bookingKey))
}

export const buildPublicRegistrationFormPath = (orderNumber: string) => {
  return ROUTE_PATHS.publicRegistrationForm.replace(':orderNumber', String(orderNumber))
}

export const buildPublicRoomStatusSharePath = (token: string) => {
  return ROUTE_PATHS.publicRoomStatusShare.replace(':token', String(token))
}

export const resolveDefaultAuthenticatedPath = () => {
  if (hasCleanerSession() && hasCleanerStore()) {
    return ROUTE_PATHS.cleanerDashboard
  }

  if (hasStoredToken()) {
    if (!hasStoredCurrentStore()) {
      return ROUTE_PATHS.storeSelection
    }

    return ROUTE_PATHS.home
  }
  return ROUTE_PATHS.login
}

const buildDocumentTitle = (title?: string) => {
  if (!title) {
    return APP_TITLE
  }

  if (title === APP_TITLE) {
    return APP_TITLE
  }

  return `${title} - ${APP_TITLE}`
}

const hasRouteParamValue = (param: unknown) => {
  if (Array.isArray(param)) {
    return Boolean(param[0])
  }

  return Boolean(param)
}

export const registerRouterGuards = (router: Router) => {
  router.beforeEach((to) => {
    const hasAdminToken = hasStoredToken()
    const hasAdminStore = hasStoredCurrentStore()
    const hasCleanerLoginSession = hasCleanerSession()
    const hasCleanerCurrentStore = hasCleanerStore()
    const requiresAdminAuth = to.matched.some((record) => record.meta.requiresAuth)
    const requiresAdminStore = to.matched.some((record) => record.meta.requiresStore)
    const adminPublicOnly = to.matched.some((record) => record.meta.publicOnly)
    const publicAccess = to.matched.some((record) => record.meta.publicAccess)
    const requiresCleanerAuth = to.matched.some((record) => record.meta.requiresCleanerAuth)
    const requiresCleanerStore = to.matched.some((record) => record.meta.requiresCleanerStore)
    const cleanerPublicOnly = to.matched.some((record) => record.meta.cleanerPublicOnly)

    if (to.path === '/') {
      return resolveDefaultAuthenticatedPath()
    }

    if (publicAccess) {
      return true
    }

    if (cleanerPublicOnly && hasCleanerLoginSession) {
      return ROUTE_PATHS.cleanerDashboard
    }

    if (adminPublicOnly && hasCleanerLoginSession) {
      return ROUTE_PATHS.cleanerDashboard
    }

    if (requiresAdminAuth && hasCleanerLoginSession && !requiresCleanerAuth) {
      return ROUTE_PATHS.cleanerDashboard
    }

    if (requiresCleanerAuth && !hasCleanerLoginSession) {
      return ROUTE_PATHS.cleanerLogin
    }

    if (requiresCleanerStore && !hasCleanerCurrentStore) {
      return ROUTE_PATHS.cleanerLogin
    }

    if (adminPublicOnly && hasAdminToken) {
      return resolveDefaultAuthenticatedPath()
    }

    if (requiresAdminAuth && !hasAdminToken) {
      return ROUTE_PATHS.login
    }

    if (requiresAdminStore && !hasAdminStore) {
      return ROUTE_PATHS.storeSelection
    }

    if (to.name === 'CleanerTaskDetail' && !hasRouteParamValue(to.params.id)) {
      return ROUTE_PATHS.cleanerDashboard
    }

    if (to.name === 'RegistrationReviewDetail' && !hasRouteParamValue(to.params.formId)) {
      return ROUTE_PATHS.reviews
    }

    if (to.name === 'SettingsRoomTypeDetail' && !hasRouteParamValue(to.params.id)) {
      return ROUTE_PATHS.settingsRoomTypes
    }

    if (to.name === 'SettingsRolePermissions' && !hasRouteParamValue(to.params.roleId)) {
      return ROUTE_PATHS.settingsStoreMembers
    }

    if (to.name === 'SettingsPricePlanRates' && !hasRouteParamValue(to.params.pricePlanId)) {
      return ROUTE_PATHS.settingsPricePlans
    }

    if (to.name === 'MessageDetail' && !hasRouteParamValue(to.params.threadId)) {
      return ROUTE_PATHS.messages
    }

    return true
  })

  router.afterEach((to) => {
    if (typeof document === 'undefined') {
      return
    }

    document.title = buildDocumentTitle(to.meta.title)
  })
}
