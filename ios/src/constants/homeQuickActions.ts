import { ROUTE_PATHS } from '@/router/guards'

export type HomeQuickActionTone = 'primary' | 'warning' | 'secondary' | 'success'

export interface HomeQuickActionItem {
  key: string
  title: string
  description: string
  iconSrc: string
  tone: HomeQuickActionTone
}

export interface HomeQuickActionDefinition {
  key: string
  titleKey: string
  descriptionKey: string
  iconSrc: string
  tone: HomeQuickActionTone
  path: string
  query?: Record<string, string>
  defaultVisible: boolean
}

export const HOME_QUICK_ACTION_CUSTOMIZE_KEY = 'customize'

export const HOME_QUICK_ACTION_DEFINITIONS: HomeQuickActionDefinition[] = [
  {
    key: 'orders',
    titleKey: 'home.quick.orders.0',
    descriptionKey: 'home.quick.orders.1',
    iconSrc: '/home-shortcuts/orders.png',
    tone: 'warning',
    path: ROUTE_PATHS.orders,
    defaultVisible: true,
  },
  {
    key: 'rooms',
    titleKey: 'home.quick.rooms.0',
    descriptionKey: 'home.quick.rooms.1',
    iconSrc: '/home-shortcuts/rooms.png',
    tone: 'primary',
    path: ROUTE_PATHS.rooms,
    defaultVisible: true,
  },
  {
    key: 'channels',
    titleKey: 'home.quick.channels.0',
    descriptionKey: 'home.quick.channels.1',
    iconSrc: '/home-shortcuts/channels.png',
    tone: 'secondary',
    path: ROUTE_PATHS.channels,
    defaultVisible: true,
  },
  {
    key: 'statistics',
    titleKey: 'home.quick.statistics.0',
    descriptionKey: 'home.quick.statistics.1',
    iconSrc: '/home-shortcuts/statistics.png',
    tone: 'primary',
    path: ROUTE_PATHS.statistics,
    defaultVisible: true,
  },
  {
    key: 'reviews',
    titleKey: 'home.quick.reviews.0',
    descriptionKey: 'home.quick.reviews.1',
    iconSrc: '/home-shortcuts/reviews.png',
    tone: 'secondary',
    path: ROUTE_PATHS.reviews,
    defaultVisible: true,
  },
  {
    key: 'messages',
    titleKey: 'home.quick.messages.0',
    descriptionKey: 'home.quick.messages.1',
    iconSrc: '/home-shortcuts/messages.png',
    tone: 'primary',
    path: ROUTE_PATHS.messages,
    defaultVisible: true,
  },
  {
    key: 'system-notifications',
    titleKey: 'home.quick.systemNotifications.0',
    descriptionKey: 'home.quick.systemNotifications.1',
    iconSrc: '/home-shortcuts/system-notifications.png',
    tone: 'warning',
    path: ROUTE_PATHS.systemNotifications,
    defaultVisible: true,
  },
  {
    key: 'order-notifications',
    titleKey: 'home.quick.orderNotifications.0',
    descriptionKey: 'home.quick.orderNotifications.1',
    iconSrc: '/home-shortcuts/order-notifications.png',
    tone: 'secondary',
    path: ROUTE_PATHS.orderNotifications,
    defaultVisible: true,
  },
  {
    key: 'wallet',
    titleKey: 'home.quick.wallet.0',
    descriptionKey: 'home.quick.wallet.1',
    iconSrc: '/home-shortcuts/wallet.png',
    tone: 'success',
    path: ROUTE_PATHS.wallet,
    defaultVisible: true,
  },
  {
    key: 'profile',
    titleKey: 'home.quick.profile.0',
    descriptionKey: 'home.quick.profile.1',
    iconSrc: '/home-shortcuts/profile.png',
    tone: 'primary',
    path: ROUTE_PATHS.profile,
    defaultVisible: true,
  },
  {
    key: 'settings',
    titleKey: 'home.quick.settings.0',
    descriptionKey: 'home.quick.settings.1',
    iconSrc: '/home-shortcuts/settings.png',
    tone: 'success',
    path: ROUTE_PATHS.settings,
    defaultVisible: true,
  },
]

export const HOME_QUICK_ACTION_DEFAULT_VISIBLE_KEYS = HOME_QUICK_ACTION_DEFINITIONS.filter((item) => item.defaultVisible).map(
  (item) => item.key,
)

export const normalizeHomeQuickActionKeys = (keys: string[]) => {
  const keySet = new Set(keys.filter((item) => typeof item === 'string'))
  return HOME_QUICK_ACTION_DEFINITIONS.filter((item) => keySet.has(item.key)).map((item) => item.key)
}

type Translate = (key: string) => string

export const localizeHomeQuickAction = (
  definition: HomeQuickActionDefinition,
  translate: Translate,
): HomeQuickActionItem => ({
  key: definition.key,
  title: translate(definition.titleKey),
  description: translate(definition.descriptionKey),
  iconSrc: definition.iconSrc,
  tone: definition.tone,
})

export const buildHomeQuickActionItems = (keys: string[], translate: Translate) => {
  const keySet = new Set(normalizeHomeQuickActionKeys(keys))
  return HOME_QUICK_ACTION_DEFINITIONS.filter((item) => keySet.has(item.key)).map((item) =>
    localizeHomeQuickAction(item, translate),
  )
}

export const buildHomeQuickActionCustomizeItem = (translate: Translate): HomeQuickActionItem => {
  return {
    key: HOME_QUICK_ACTION_CUSTOMIZE_KEY,
    title: translate('home.quick.customize.0'),
    description: translate('home.quick.customize.1'),
    iconSrc: '/home-shortcuts/customize.png',
    tone: 'secondary',
  }
}

export const findHomeQuickActionDefinition = (key: string) => {
  return HOME_QUICK_ACTION_DEFINITIONS.find((item) => item.key === key) ?? null
}
