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
  | 'viewOrders'
  | 'modifyOrder'
  | 'cancelOrder'
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
    label: '住宿管理',
    sections: [
      {
        title: '房态管理',
        items: [
          {
            key: 'viewRoomStatus',
            label: '查看房态',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_ROOM_STATUS,
          },
          {
            key: 'editRoomStatus',
            label: '修改房态',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.EDIT_ROOM_STATUS,
          },
          {
            key: 'viewRoomInfo',
            label: '查看房情表',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_ROOM_INFO,
          },
        ],
      },
      {
        title: '房价管理',
        items: [
          {
            key: 'viewRoomPrice',
            label: '查看房价',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_ROOM_PRICE,
          },
          {
            key: 'editRoomPrice',
            label: '修改房价',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.EDIT_ROOM_PRICE,
          },
          {
            key: 'viewPriceLog',
            label: '查看改价记录',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_PRICE_LOG,
          },
          {
            key: 'batchChangePrice',
            label: '批量改价',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.BATCH_CHANGE_PRICE,
          },
        ],
      },
      {
        title: '保洁管理',
        items: [
          {
            key: 'taskList',
            label: '查看保洁任务',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.TASK_LIST,
          },
        ],
      },
    ],
  },
  {
    name: 'order',
    label: '订单管理',
    sections: [
      {
        title: '订单管理',
        items: [
          {
            key: 'viewOrders',
            label: '查看订单',
            module: PermissionModule.ORDER,
            action: PermissionAction.VIEW_ORDERS,
          },
          {
            key: 'modifyOrder',
            label: '修改订单',
            module: PermissionModule.ORDER,
            action: PermissionAction.MODIFY_ORDER,
          },
          {
            key: 'cancelOrder',
            label: '取消订单',
            module: PermissionModule.ORDER,
            action: PermissionAction.CANCEL_ORDER,
          },
        ],
      },
    ],
  },
  {
    name: 'channel',
    label: '渠道',
    sections: [
      {
        title: '渠道权限',
        items: [
          {
            key: 'viewChannels',
            label: '查看渠道',
            module: PermissionModule.CHANNEL,
            action: PermissionAction.VIEW_CHANNELS,
          },
          {
            key: 'manageChannels',
            label: '管理渠道',
            module: PermissionModule.CHANNEL,
            action: PermissionAction.MANAGE_CHANNELS,
          },
        ],
      },
    ],
  },
  {
    name: 'statistics',
    label: '统计分析',
    sections: [
      {
        title: '统计分析',
        items: [
          {
            key: 'viewStats',
            label: '查看统计数据',
            module: PermissionModule.STATISTICS,
            action: PermissionAction.VIEW_STATS,
          },
        ],
      },
    ],
  },
  {
    name: 'settings',
    label: '设置',
    sections: [
      {
        title: '设置权限',
        items: [
          {
            key: 'modifyStoreSettings',
            label: '修改门店设置',
            module: PermissionModule.SETTINGS,
            action: PermissionAction.MODIFY_STORE_SETTINGS,
          },
          {
            key: 'manageEmployeeAccounts',
            label: '管理员工账号',
            module: PermissionModule.SETTINGS,
            action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS,
          },
        ],
      },
    ],
  },
  {
    name: 'sensitive',
    label: '敏感权限',
    sections: [
      {
        title: '敏感权限',
        items: [
          {
            key: 'viewFinancialData',
            label: '查看财务数据',
            module: PermissionModule.SENSITIVE,
            action: PermissionAction.VIEW_FINANCIAL_DATA,
          },
          {
            key: 'deleteImportantData',
            label: '删除重要数据',
            module: PermissionModule.SENSITIVE,
            action: PermissionAction.DELETE_IMPORTANT_DATA,
          },
        ],
      },
    ],
  },
]
