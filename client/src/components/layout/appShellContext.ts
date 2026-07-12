import type { InjectionKey, ComputedRef } from 'vue'
import type { StoreDTO } from '@/api/store'

export interface AppTopNavItem {
  labelKey: string
  path: string
  activePaths?: string[]
}

export interface AppTopNavProps {
  stores: StoreDTO[]
  currentStore: StoreDTO | null
  navItems: AppTopNavItem[]
  currentPath: string
  displayName: string
  userEmail: string
  userAvatar?: string
  canAccessWallet: boolean
  canAccessOrder: boolean
  chatUnreadCount: number
  formattedChatUnreadCount: string
  inboxUnreadCount: number
  formattedInboxUnreadCount: string
  hasSystemUnread: boolean
  hasOrderUnread: boolean
  canSwitchWorkspace: boolean
}

export interface AppTopNavBindings {
  props: ComputedRef<AppTopNavProps>
  onStoreSelect: (store: StoreDTO) => void
  onManageStores: () => void
  onMenuClick: (path: string) => void
  onWalletClick: () => void
  onInboxClick: () => void
  onSupportChat: () => void
  onSystemNotification: () => void
  onOrderNotification: () => void
  onProfileClick: () => void
  onWorkspaceSwitch: () => void
  onLogout: () => Promise<void> | void
}

export const appTopNavBindingsKey: InjectionKey<AppTopNavBindings> = Symbol('app-top-nav-bindings')
