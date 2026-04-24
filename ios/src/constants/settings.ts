import { PermissionAction, PermissionModule } from '@/api/role'
import type { FacilityDTO } from '@/types/store'

export const SETTINGS_SORT_TYPE_OPTIONS = [
  { label: '房型排序', value: 'ROOM_TYPE' },
  { label: '房间排序', value: 'ROOM' },
  { label: '分组排序', value: 'GROUP' },
] as const

export const STORE_FACILITY_OPTIONS: Array<{ label: string; payload: FacilityDTO }> = [
  { label: 'WiFi', payload: { group: 'Facilities', name: 'WiFi' } },
  { label: '停车场', payload: { group: 'Facilities', name: 'Parking' } },
  { label: '电梯', payload: { group: 'Facilities', name: 'Elevator' } },
  { label: '餐厅', payload: { group: 'Dining', name: 'Restaurant' } },
  { label: '酒吧', payload: { group: 'Dining', name: 'Bar' } },
  { label: '前台接待', payload: { group: 'Services', name: 'Front Desk' } },
  { label: '24小时前台', payload: { group: 'Services', name: '24-hour Front Desk' } },
  { label: '行李寄存', payload: { group: 'Services', name: 'Luggage Storage' } },
  { label: '洗衣服务', payload: { group: 'Services', name: 'Laundry Service' } },
  { label: '健身房', payload: { group: 'Facilities', name: 'Gym' } },
  { label: '游泳池', payload: { group: 'Facilities', name: 'Swimming Pool' } },
  { label: '禁止吸烟', payload: { group: 'Policies', name: 'Non-smoking' } },
  { label: '残疾人通道', payload: { group: 'Accessibility', name: 'Accessible Entrance' } },
  { label: '轮椅通道', payload: { group: 'Accessibility', name: 'Wheelchair Access' } },
  { label: '机场班车', payload: { group: 'Transport', name: 'Airport Shuttle' } },
  { label: '多语言员工', payload: { group: 'Services', name: 'Multilingual Staff' } },
] as const

export const CHANNEL_TYPE_OPTIONS = [
  { label: '直连渠道', value: 'DIRECT' },
  { label: 'OTA', value: 'OTA' },
  { label: '线下渠道', value: 'OFFLINE' },
  { label: '企业协议', value: 'CORPORATE' },
] as const

export const CHANNEL_COLOR_OPTIONS = [
  { label: '松石绿', value: '#0f766e' },
  { label: '海岸蓝', value: '#2563eb' },
  { label: '晚霞紫', value: '#9333ea' },
  { label: '琥珀橙', value: '#f59e0b' },
  { label: '珊瑚红', value: '#ef4444' },
  { label: '湖水青', value: '#14b8a6' },
] as const

export const PRICING_TOOL_STATUS_ITEMS = [
  '支持查看 PriceLabs 集成状态、连接关系与渠道价差。',
  '支持启停集成、连接启停、手动同步与价格调整。',
  '复杂日志诊断保留最近同步记录与错误摘要。',
] as const

export interface PermissionItemConfig {
  key: SettingsExtraPermissionKey
  label: string
  module: PermissionModule
  action: PermissionAction
  supportsRoomTypeScope?: boolean
}

export type SettingsExtraPermissionKey =
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

export interface PermissionSectionConfig {
  title: string
  items: PermissionItemConfig[]
}

export interface PermissionTabConfig {
  name: string
  label: string
  sections: PermissionSectionConfig[]
}

export const SETTINGS_PERMISSION_TABS: PermissionTabConfig[] = [
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
            supportsRoomTypeScope: true,
          },
          { key: 'editRoomStatus', label: '修改房态', module: PermissionModule.ACCOMMODATION, action: PermissionAction.EDIT_ROOM_STATUS },
          { key: 'viewRoomInfo', label: '查看房情表', module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_INFO },
        ],
      },
      {
        title: '房价管理',
        items: [
          { key: 'viewRoomPrice', label: '查看房价', module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_PRICE },
          { key: 'editRoomPrice', label: '修改房价', module: PermissionModule.ACCOMMODATION, action: PermissionAction.EDIT_ROOM_PRICE },
          { key: 'viewPriceLog', label: '查看改价记录', module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_PRICE_LOG },
          { key: 'batchChangePrice', label: '批量改价', module: PermissionModule.ACCOMMODATION, action: PermissionAction.BATCH_CHANGE_PRICE },
        ],
      },
      {
        title: '保洁管理',
        items: [
          { key: 'taskList', label: '查看保洁任务', module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST },
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
          { key: 'viewOrders', label: '查看订单', module: PermissionModule.ORDER, action: PermissionAction.VIEW_ORDERS },
          { key: 'modifyOrder', label: '修改订单', module: PermissionModule.ORDER, action: PermissionAction.MODIFY_ORDER },
          { key: 'cancelOrder', label: '取消订单', module: PermissionModule.ORDER, action: PermissionAction.CANCEL_ORDER },
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
          { key: 'viewChannels', label: '查看渠道', module: PermissionModule.CHANNEL, action: PermissionAction.VIEW_CHANNELS },
          { key: 'manageChannels', label: '管理渠道', module: PermissionModule.CHANNEL, action: PermissionAction.MANAGE_CHANNELS },
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
        items: [{ key: 'viewStats', label: '查看统计数据', module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS }],
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
          { key: 'modifyStoreSettings', label: '修改门店设置', module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS },
          { key: 'manageEmployeeAccounts', label: '管理员工账号', module: PermissionModule.SETTINGS, action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS },
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
          { key: 'viewFinancialData', label: '查看财务数据', module: PermissionModule.SENSITIVE, action: PermissionAction.VIEW_FINANCIAL_DATA },
          { key: 'deleteImportantData', label: '删除重要数据', module: PermissionModule.SENSITIVE, action: PermissionAction.DELETE_IMPORTANT_DATA },
        ],
      },
    ],
  },
]
