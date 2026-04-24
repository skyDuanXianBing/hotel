import {
  barChartOutline,
  bedOutline,
  chatbubblesOutline,
  clipboardOutline,
  gitNetworkOutline,
  notificationsOutline,
  optionsOutline,
  personCircleOutline,
  receiptOutline,
  settingsOutline,
  walletOutline,
} from 'ionicons/icons'
import { ROUTE_PATHS } from '@/router/guards'

export type HomeQuickActionTone = 'primary' | 'warning' | 'secondary' | 'success'

export interface HomeQuickActionItem {
  key: string
  title: string
  description: string
  icon: string
  tone: HomeQuickActionTone
}

export interface HomeQuickActionDefinition extends HomeQuickActionItem {
  path: string
  query?: Record<string, string>
  defaultVisible: boolean
}

export const HOME_QUICK_ACTION_CUSTOMIZE_KEY = 'customize'

export const HOME_QUICK_ACTION_DEFINITIONS: HomeQuickActionDefinition[] = [
  {
    key: 'orders',
    title: '订单',
    description: '快速进入订单页，继续处理预抵、预离与待处理订单。',
    icon: receiptOutline,
    tone: 'warning',
    path: ROUTE_PATHS.orders,
    defaultVisible: true,
  },
  {
    key: 'rooms',
    title: '房态',
    description: '查看今日可售与房间状态，衔接房态核心操作。',
    icon: bedOutline,
    tone: 'primary',
    path: ROUTE_PATHS.rooms,
    defaultVisible: true,
  },
  {
    key: 'channels',
    title: '渠道',
    description: '跳到渠道管理，查看连接、映射和后续操作入口。',
    icon: gitNetworkOutline,
    tone: 'secondary',
    path: ROUTE_PATHS.channels,
    defaultVisible: true,
  },
  {
    key: 'statistics',
    title: '统计',
    description: '进入统计工作台，查看经营概况、报表和数据中心。',
    icon: barChartOutline,
    tone: 'primary',
    path: ROUTE_PATHS.statistics,
    defaultVisible: true,
  },
  {
    key: 'reviews',
    title: '审查',
    description: '进入审查列表，处理入住登记与审核相关任务。',
    icon: clipboardOutline,
    tone: 'secondary',
    path: ROUTE_PATHS.reviews,
    defaultVisible: true,
  },
  {
    key: 'messages',
    title: '消息',
    description: '查看住客会话、未读消息与待处理聊天。',
    icon: chatbubblesOutline,
    tone: 'primary',
    path: ROUTE_PATHS.messages,
    defaultVisible: true,
  },
  {
    key: 'system-notifications',
    title: '系统通知',
    description: '查看系统、保洁与任务相关通知。',
    icon: notificationsOutline,
    tone: 'warning',
    path: ROUTE_PATHS.systemNotifications,
    defaultVisible: true,
  },
  {
    key: 'order-notifications',
    title: '订单通知',
    description: '集中处理订单类提醒与未读通知。',
    icon: receiptOutline,
    tone: 'secondary',
    path: ROUTE_PATHS.orderNotifications,
    defaultVisible: true,
  },
  {
    key: 'wallet',
    title: '钱包',
    description: '查看余额、流水、提现记录与认证说明。',
    icon: walletOutline,
    tone: 'success',
    path: ROUTE_PATHS.wallet,
    defaultVisible: true,
  },
  {
    key: 'profile',
    title: '个人中心',
    description: '更新昵称、头像、性别并修改密码。',
    icon: personCircleOutline,
    tone: 'primary',
    path: ROUTE_PATHS.profile,
    defaultVisible: true,
  },
  {
    key: 'settings',
    title: '设置',
    description: '继续进入门店、账户与业务配置的移动端入口。',
    icon: settingsOutline,
    tone: 'success',
    path: ROUTE_PATHS.settings,
    defaultVisible: true,
  },
]

export const HOME_QUICK_ACTION_CUSTOMIZE_ITEM: HomeQuickActionItem = {
  key: HOME_QUICK_ACTION_CUSTOMIZE_KEY,
  title: '定制',
  description: '个性化调整首页快捷方式的显示与隐藏。',
  icon: optionsOutline,
  tone: 'secondary',
}

export const HOME_QUICK_ACTION_DEFAULT_VISIBLE_KEYS = HOME_QUICK_ACTION_DEFINITIONS.filter((item) => item.defaultVisible).map(
  (item) => item.key,
)

export const normalizeHomeQuickActionKeys = (keys: string[]) => {
  const keySet = new Set(keys.filter((item) => typeof item === 'string'))
  return HOME_QUICK_ACTION_DEFINITIONS.filter((item) => keySet.has(item.key)).map((item) => item.key)
}

export const buildHomeQuickActionItems = (keys: string[]) => {
  const keySet = new Set(normalizeHomeQuickActionKeys(keys))
  return HOME_QUICK_ACTION_DEFINITIONS.filter((item) => keySet.has(item.key)).map(({ path, query, defaultVisible, ...action }) => action)
}

export const findHomeQuickActionDefinition = (key: string) => {
  return HOME_QUICK_ACTION_DEFINITIONS.find((item) => item.key === key) ?? null
}
