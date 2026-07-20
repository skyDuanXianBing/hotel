import { PermissionAction, PermissionModule } from '@/api/role'

export type ExtraPermissionKey =
  | 'viewRoomStatus'
  | 'editRoomStatus'
  | 'viewRoomInfo'
  | 'viewRoomPrice'
  | 'editRoomPrice'
  | 'viewPriceLog'
  | 'batchChangePrice'
  | 'taskList'
  | 'createInternalTask'
  | 'viewOrders'
  | 'modifyOrder'
  | 'cancelOrder'
  | 'viewReviews'
  | 'replyReviews'
  | 'reviewGuests'
  | 'syncReviews'
  | 'viewChannels'
  | 'manageChannels'
  | 'viewStats'
  | 'modifyStoreSettings'
  | 'manageEmployeeAccounts'
  | 'viewFinancialData'
  | 'deleteImportantData'

export interface PermissionItemConfig {
  key: ExtraPermissionKey
  label: string
  module: PermissionModule
  action: PermissionAction
  ownerOnly?: boolean
}

export interface PermissionSectionConfig {
  title: string
  items: PermissionItemConfig[]
}

export interface PermissionTabConfig {
  name: string
  label: string
  sections: PermissionSectionConfig[]
}

export const ACCOUNT_PERMISSION_TABS: PermissionTabConfig[] = [
  {
    name: 'accommodation',
    label: 'settingsStage4.accountPermission.tabs.accommodation',
    sections: [
      {
        title: 'settingsStage4.accountPermission.sections.roomStatus',
        items: [
          {
            key: 'viewRoomStatus',
            label: 'settingsStage4.accountPermission.items.viewRoomStatus',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_ROOM_STATUS,
          },
          {
            key: 'editRoomStatus',
            label: 'settingsStage4.accountPermission.items.editRoomStatus',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.EDIT_ROOM_STATUS,
          },
          {
            key: 'viewRoomInfo',
            label: 'settingsStage4.accountPermission.items.viewRoomInfo',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_ROOM_INFO,
          },
        ],
      },
      {
        title: 'settingsStage4.accountPermission.sections.roomPrice',
        items: [
          {
            key: 'viewRoomPrice',
            label: 'settingsStage4.accountPermission.items.viewRoomPrice',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_ROOM_PRICE,
          },
          {
            key: 'editRoomPrice',
            label: 'settingsStage4.accountPermission.items.editRoomPrice',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.EDIT_ROOM_PRICE,
          },
          {
            key: 'viewPriceLog',
            label: 'settingsStage4.accountPermission.items.viewPriceLog',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_PRICE_LOG,
          },
          {
            key: 'batchChangePrice',
            label: 'settingsStage4.accountPermission.items.batchChangePrice',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.BATCH_CHANGE_PRICE,
          },
        ],
      },
      {
        title: 'settingsStage4.accountPermission.sections.cleaning',
        items: [
          {
            key: 'taskList',
            label: 'settingsStage4.accountPermission.items.taskList',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.TASK_LIST,
          },
        ],
      },
      {
        title: 'settingsStage4.accountPermission.sections.internalTasks',
        items: [
          {
            key: 'createInternalTask',
            label: 'settingsStage4.accountPermission.items.createInternalTask',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.CREATE_INTERNAL_TASK,
            ownerOnly: true,
          },
        ],
      },
    ],
  },
  {
    name: 'order',
    label: 'settingsStage4.accountPermission.tabs.order',
    sections: [
      {
        title: 'settingsStage4.accountPermission.sections.order',
        items: [
          {
            key: 'viewOrders',
            label: 'settingsStage4.accountPermission.items.viewOrders',
            module: PermissionModule.ORDER,
            action: PermissionAction.VIEW_ORDERS,
          },
          {
            key: 'modifyOrder',
            label: 'settingsStage4.accountPermission.items.modifyOrder',
            module: PermissionModule.ORDER,
            action: PermissionAction.MODIFY_ORDER,
          },
          {
            key: 'cancelOrder',
            label: 'settingsStage4.accountPermission.items.cancelOrder',
            module: PermissionModule.ORDER,
            action: PermissionAction.CANCEL_ORDER,
          },
        ],
      },
    ],
  },
  {
    name: 'reviews',
    label: 'settingsStage4.accountPermission.tabs.reviews',
    sections: [
      {
        title: 'settingsStage4.accountPermission.sections.reviews',
        items: [
          {
            key: 'viewReviews',
            label: 'settingsStage4.accountPermission.items.viewReviews',
            module: PermissionModule.REVIEW,
            action: PermissionAction.VIEW,
          },
          {
            key: 'replyReviews',
            label: 'settingsStage4.accountPermission.items.replyReviews',
            module: PermissionModule.REVIEW,
            action: PermissionAction.REPLY,
          },
          {
            key: 'reviewGuests',
            label: 'settingsStage4.accountPermission.items.reviewGuests',
            module: PermissionModule.REVIEW,
            action: PermissionAction.REVIEW_GUEST,
          },
          {
            key: 'syncReviews',
            label: 'settingsStage4.accountPermission.items.syncReviews',
            module: PermissionModule.REVIEW,
            action: PermissionAction.SYNC,
          },
        ],
      },
    ],
  },
  {
    name: 'channel',
    label: 'settingsStage4.accountPermission.tabs.channel',
    sections: [
      {
        title: 'settingsStage4.accountPermission.sections.channel',
        items: [
          {
            key: 'viewChannels',
            label: 'settingsStage4.accountPermission.items.viewChannels',
            module: PermissionModule.CHANNEL,
            action: PermissionAction.VIEW_CHANNELS,
          },
          {
            key: 'manageChannels',
            label: 'settingsStage4.accountPermission.items.manageChannels',
            module: PermissionModule.CHANNEL,
            action: PermissionAction.MANAGE_CHANNELS,
          },
        ],
      },
    ],
  },
  {
    name: 'statistics',
    label: 'settingsStage4.accountPermission.tabs.statistics',
    sections: [
      {
        title: 'settingsStage4.accountPermission.sections.statistics',
        items: [
          {
            key: 'viewStats',
            label: 'settingsStage4.accountPermission.items.viewStats',
            module: PermissionModule.STATISTICS,
            action: PermissionAction.VIEW_STATS,
          },
        ],
      },
    ],
  },
  {
    name: 'settings',
    label: 'settingsStage4.accountPermission.tabs.settings',
    sections: [
      {
        title: 'settingsStage4.accountPermission.sections.settings',
        items: [
          {
            key: 'modifyStoreSettings',
            label: 'settingsStage4.accountPermission.items.modifyStoreSettings',
            module: PermissionModule.SETTINGS,
            action: PermissionAction.MODIFY_STORE_SETTINGS,
          },
          {
            key: 'manageEmployeeAccounts',
            label: 'settingsStage4.accountPermission.items.manageEmployeeAccounts',
            module: PermissionModule.SETTINGS,
            action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS,
          },
        ],
      },
    ],
  },
  {
    name: 'sensitive',
    label: 'settingsStage4.accountPermission.tabs.sensitive',
    sections: [
      {
        title: 'settingsStage4.accountPermission.sections.sensitive',
        items: [
          {
            key: 'viewFinancialData',
            label: 'settingsStage4.accountPermission.items.viewFinancialData',
            module: PermissionModule.SENSITIVE,
            action: PermissionAction.VIEW_FINANCIAL_DATA,
          },
          {
            key: 'deleteImportantData',
            label: 'settingsStage4.accountPermission.items.deleteImportantData',
            module: PermissionModule.SENSITIVE,
            action: PermissionAction.DELETE_IMPORTANT_DATA,
          },
        ],
      },
    ],
  },
]
