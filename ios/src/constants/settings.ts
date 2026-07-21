import { PermissionAction, PermissionModule } from '@/api/role'
import type { FacilityDTO } from '@/types/store'

export const SETTINGS_SORT_TYPE_OPTIONS = [
  { labelKey: 'settings.constants.sortType.roomType', value: 'ROOM_TYPE' },
  { labelKey: 'settings.constants.sortType.room', value: 'ROOM' },
  { labelKey: 'settings.constants.sortType.group', value: 'GROUP' },
] as const

export const STORE_FACILITY_OPTIONS: Array<{ key: string; labelKey: string; payload: FacilityDTO }> = [
  { key: 'wifi', labelKey: 'settings.constants.facility.wifi', payload: { group: 'Facilities', name: 'WiFi' } },
  {
    key: 'parking',
    labelKey: 'settings.constants.facility.parking',
    payload: { group: 'Facilities', name: 'Parking' },
  },
  {
    key: 'elevator',
    labelKey: 'settings.constants.facility.elevator',
    payload: { group: 'Facilities', name: 'Elevator' },
  },
  {
    key: 'restaurant',
    labelKey: 'settings.constants.facility.restaurant',
    payload: { group: 'Dining', name: 'Restaurant' },
  },
  { key: 'bar', labelKey: 'settings.constants.facility.bar', payload: { group: 'Dining', name: 'Bar' } },
  {
    key: 'frontDesk',
    labelKey: 'settings.constants.facility.frontDesk',
    payload: { group: 'Services', name: 'Front Desk' },
  },
  {
    key: 'frontDesk24h',
    labelKey: 'settings.constants.facility.frontDesk24h',
    payload: { group: 'Services', name: '24-hour Front Desk' },
  },
  {
    key: 'luggageStorage',
    labelKey: 'settings.constants.facility.luggageStorage',
    payload: { group: 'Services', name: 'Luggage Storage' },
  },
  {
    key: 'laundryService',
    labelKey: 'settings.constants.facility.laundryService',
    payload: { group: 'Services', name: 'Laundry Service' },
  },
  { key: 'gym', labelKey: 'settings.constants.facility.gym', payload: { group: 'Facilities', name: 'Gym' } },
  {
    key: 'swimmingPool',
    labelKey: 'settings.constants.facility.swimmingPool',
    payload: { group: 'Facilities', name: 'Swimming Pool' },
  },
  {
    key: 'nonSmoking',
    labelKey: 'settings.constants.facility.nonSmoking',
    payload: { group: 'Policies', name: 'Non-smoking' },
  },
  {
    key: 'accessibleEntrance',
    labelKey: 'settings.constants.facility.accessibleEntrance',
    payload: { group: 'Accessibility', name: 'Accessible Entrance' },
  },
  {
    key: 'wheelchairAccess',
    labelKey: 'settings.constants.facility.wheelchairAccess',
    payload: { group: 'Accessibility', name: 'Wheelchair Access' },
  },
  {
    key: 'airportShuttle',
    labelKey: 'settings.constants.facility.airportShuttle',
    payload: { group: 'Transport', name: 'Airport Shuttle' },
  },
  {
    key: 'multilingualStaff',
    labelKey: 'settings.constants.facility.multilingualStaff',
    payload: { group: 'Services', name: 'Multilingual Staff' },
  },
] as const

export const CHANNEL_TYPE_OPTIONS = [
  { labelKey: 'settings.constants.channelType.direct', value: 'DIRECT' },
  { labelKey: 'settings.constants.channelType.ota', value: 'OTA' },
  { labelKey: 'settings.constants.channelType.offline', value: 'OFFLINE' },
  { labelKey: 'settings.constants.channelType.corporate', value: 'CORPORATE' },
] as const

export const CHANNEL_COLOR_OPTIONS = [
  { labelKey: 'settings.constants.channelColor.teal', value: '#0f766e' },
  { labelKey: 'settings.constants.channelColor.blue', value: '#2563eb' },
  { labelKey: 'settings.constants.channelColor.purple', value: '#9333ea' },
  { labelKey: 'settings.constants.channelColor.amber', value: '#f59e0b' },
  { labelKey: 'settings.constants.channelColor.coral', value: '#ef4444' },
  { labelKey: 'settings.constants.channelColor.aqua', value: '#14b8a6' },
] as const

export const PRICING_TOOL_STATUS_ITEMS = [
  'stage5Final.settings.pricingToolStatus.0',
  'stage5Final.settings.pricingToolStatus.1',
  'stage5Final.settings.pricingToolStatus.2',
] as const

export interface PermissionItemConfig {
  key: SettingsExtraPermissionKey
  labelKey: string
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
  titleKey: string
  items: PermissionItemConfig[]
}

export interface PermissionTabConfig {
  name: string
  labelKey: string
  sections: PermissionSectionConfig[]
}

export const SETTINGS_PERMISSION_TABS: PermissionTabConfig[] = [
  {
    name: 'accommodation',
    labelKey: 'settings.constants.permission.tabs.accommodation',
    sections: [
      {
        titleKey: 'settings.constants.permission.sections.roomStatus',
        items: [
          {
            key: 'viewRoomStatus',
            labelKey: 'settings.constants.permission.items.viewRoomStatus',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_ROOM_STATUS,
            supportsRoomTypeScope: true,
          },
          {
            key: 'editRoomStatus',
            labelKey: 'settings.constants.permission.items.editRoomStatus',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.EDIT_ROOM_STATUS,
          },
          {
            key: 'viewRoomInfo',
            labelKey: 'settings.constants.permission.items.viewRoomInfo',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_ROOM_INFO,
          },
        ],
      },
      {
        titleKey: 'settings.constants.permission.sections.roomPrice',
        items: [
          {
            key: 'viewRoomPrice',
            labelKey: 'settings.constants.permission.items.viewRoomPrice',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_ROOM_PRICE,
          },
          {
            key: 'editRoomPrice',
            labelKey: 'settings.constants.permission.items.editRoomPrice',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.EDIT_ROOM_PRICE,
          },
          {
            key: 'viewPriceLog',
            labelKey: 'settings.constants.permission.items.viewPriceLog',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_PRICE_LOG,
          },
          {
            key: 'batchChangePrice',
            labelKey: 'settings.constants.permission.items.batchChangePrice',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.BATCH_CHANGE_PRICE,
          },
        ],
      },
      {
        titleKey: 'settings.constants.permission.sections.housekeeping',
        items: [
          {
            key: 'taskList',
            labelKey: 'settings.constants.permission.items.taskList',
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.TASK_LIST,
          },
        ],
      },
    ],
  },
  {
    name: 'order',
    labelKey: 'settings.constants.permission.tabs.order',
    sections: [
      {
        titleKey: 'settings.constants.permission.sections.order',
        items: [
          {
            key: 'viewOrders',
            labelKey: 'settings.constants.permission.items.viewOrders',
            module: PermissionModule.ORDER,
            action: PermissionAction.VIEW_ORDERS,
          },
          {
            key: 'modifyOrder',
            labelKey: 'settings.constants.permission.items.modifyOrder',
            module: PermissionModule.ORDER,
            action: PermissionAction.MODIFY_ORDER,
          },
          {
            key: 'cancelOrder',
            labelKey: 'settings.constants.permission.items.cancelOrder',
            module: PermissionModule.ORDER,
            action: PermissionAction.CANCEL_ORDER,
          },
        ],
      },
    ],
  },
  {
    name: 'channel',
    labelKey: 'settings.constants.permission.tabs.channel',
    sections: [
      {
        titleKey: 'settings.constants.permission.sections.channel',
        items: [
          {
            key: 'viewChannels',
            labelKey: 'settings.constants.permission.items.viewChannels',
            module: PermissionModule.CHANNEL,
            action: PermissionAction.VIEW_CHANNELS,
          },
          {
            key: 'manageChannels',
            labelKey: 'settings.constants.permission.items.manageChannels',
            module: PermissionModule.CHANNEL,
            action: PermissionAction.MANAGE_CHANNELS,
          },
        ],
      },
    ],
  },
  {
    name: 'statistics',
    labelKey: 'settings.constants.permission.tabs.statistics',
    sections: [
      {
        titleKey: 'settings.constants.permission.sections.statistics',
        items: [
          {
            key: 'viewStats',
            labelKey: 'settings.constants.permission.items.viewStats',
            module: PermissionModule.STATISTICS,
            action: PermissionAction.VIEW_STATS,
          },
        ],
      },
    ],
  },
  {
    name: 'settings',
    labelKey: 'settings.constants.permission.tabs.settings',
    sections: [
      {
        titleKey: 'settings.constants.permission.sections.settings',
        items: [
          {
            key: 'modifyStoreSettings',
            labelKey: 'settings.constants.permission.items.modifyStoreSettings',
            module: PermissionModule.SETTINGS,
            action: PermissionAction.MODIFY_STORE_SETTINGS,
          },
          {
            key: 'manageEmployeeAccounts',
            labelKey: 'settings.constants.permission.items.manageEmployeeAccounts',
            module: PermissionModule.SETTINGS,
            action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS,
          },
        ],
      },
    ],
  },
  {
    name: 'sensitive',
    labelKey: 'settings.constants.permission.tabs.sensitive',
    sections: [
      {
        titleKey: 'settings.constants.permission.sections.sensitive',
        items: [
          {
            key: 'viewFinancialData',
            labelKey: 'settings.constants.permission.items.viewFinancialData',
            module: PermissionModule.SENSITIVE,
            action: PermissionAction.VIEW_FINANCIAL_DATA,
          },
          {
            key: 'deleteImportantData',
            labelKey: 'settings.constants.permission.items.deleteImportantData',
            module: PermissionModule.SENSITIVE,
            action: PermissionAction.DELETE_IMPORTANT_DATA,
          },
        ],
      },
    ],
  },
]
