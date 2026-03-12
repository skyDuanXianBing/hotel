import type { FacilityDTO } from '@/api/roomType'

export interface RoomFacilityItem {
  key: string
  label: string
  payload: FacilityDTO
}

export interface RoomFacilitySection {
  title: string
  items: RoomFacilityItem[]
}

export const STORE_FACILITY_OPTIONS: Array<{ label: string; payload: FacilityDTO }> = [
  { label: 'WiFi', payload: { group: 'Facilities', name: 'WiFi' } },
  { label: '停车场', payload: { group: 'Facilities', name: 'Parking' } },
  { label: '电梯', payload: { group: 'Facilities', name: 'Elevator' } },
  { label: '行李寄存', payload: { group: 'Services', name: 'Luggage Storage' } },
  { label: '健身房', payload: { group: 'Facilities', name: 'Gym' } },
  { label: '游泳池', payload: { group: 'Facilities', name: 'Swimming Pool' } },
  { label: '室外游泳区', payload: { group: 'Facilities', name: 'Outdoor Pool' } },
  { label: '餐厅', payload: { group: 'Dining', name: 'Restaurant' } },
  { label: '休息室', payload: { group: 'Facilities', name: 'Lounge' } },
  { label: '机场班车', payload: { group: 'Transport', name: 'Airport Shuttle' } },
  { label: '酒吧', payload: { group: 'Dining', name: 'Bar' } },
  { label: '前台接待', payload: { group: 'Services', name: 'Front Desk' } },
  { label: '会议室', payload: { group: 'Business', name: 'Meeting Room' } },
  { label: '洗衣服务', payload: { group: 'Services', name: 'Laundry Service' } },
  { label: '桑拿', payload: { group: 'Wellness', name: 'Sauna' } },
  { label: '水疗中心', payload: { group: 'Wellness', name: 'Spa' } },
  { label: '礼宾服务', payload: { group: 'Services', name: 'Concierge' } },
  { label: '自动取款机', payload: { group: 'Facilities', name: 'ATM' } },
  { label: '外币兑换', payload: { group: 'Services', name: 'Currency Exchange' } },
  { label: '提供早餐', payload: { group: 'Dining', name: 'Breakfast Available' } },
  { label: '礼品店', payload: { group: 'Facilities', name: 'Gift Shop' } },
  { label: '打印机', payload: { group: 'Business', name: 'Printer' } },
  { label: '客房服务', payload: { group: 'Services', name: 'Room Service' } },
  { label: '加床/婴儿床', payload: { group: 'Family', name: 'Extra Bed / Crib' } },
  { label: '24小时前台', payload: { group: 'Services', name: '24-hour Front Desk' } },
  { label: '禁止吸烟', payload: { group: 'Policies', name: 'Non-smoking' } },
  { label: '残疾人通道', payload: { group: 'Accessibility', name: 'Accessible Entrance' } },
  { label: '轮椅通道', payload: { group: 'Accessibility', name: 'Wheelchair Access' } },
  { label: '司机服务', payload: { group: 'Transport', name: 'Driver Service' } },
  { label: '私人庭院', payload: { group: 'Facilities', name: 'Private Courtyard' } },
  { label: '多语言员工', payload: { group: 'Services', name: 'Multilingual Staff' } },
]

export const ROOM_FACILITY_SECTIONS: RoomFacilitySection[] = [
  {
    title: '热门设施',
    items: [
      { key: 'airConditioning', label: '空调', payload: { group: 'Popular', name: 'Air Conditioning' } },
      { key: 'heating', label: '暖气', payload: { group: 'Popular', name: 'Heating' } },
      { key: 'noSmoking', label: '禁止吸烟', payload: { group: 'Popular', name: 'Non-smoking' } },
      { key: 'tv', label: '电视', payload: { group: 'Popular', name: 'TV' } },
      { key: 'wifi', label: 'WiFi', payload: { group: 'Popular', name: 'WiFi' } },
    ],
  },
  {
    title: '浴室设施',
    items: [
      { key: 'bathtub', label: '浴缸', payload: { group: 'Bathroom', name: 'Bathtub' } },
      { key: 'toiletries', label: '清洁用品', payload: { group: 'Bathroom', name: 'Cleaning Products' } },
      { key: 'towel', label: '毛巾', payload: { group: 'Bathroom', name: 'Towels' } },
      { key: 'shower', label: '淋浴器', payload: { group: 'Bathroom', name: 'Shower' } },
      { key: 'hairDryer', label: '吹风机', payload: { group: 'Bathroom', name: 'Hair Dryer' } },
      { key: 'slippers', label: '拖鞋', payload: { group: 'Bathroom', name: 'Slippers' } },
      { key: 'separateShower', label: '独立淋浴和浴缸', payload: { group: 'Bathroom', name: 'Separate Shower and Bathtub' } },
      { key: 'toiletries2', label: '洗漱用品', payload: { group: 'Bathroom', name: 'Toiletries' } },
    ],
  },
  {
    title: '餐饮设施',
    items: [
      { key: 'teaCoffee', label: '茶/咖啡设施', payload: { group: 'Dining', name: 'Tea/Coffee Maker' } },
      { key: 'tableware', label: '餐具', payload: { group: 'Dining', name: 'Tableware' } },
      { key: 'dinnerware', label: '餐具和器皿', payload: { group: 'Dining', name: 'Dinnerware' } },
      { key: 'iceBox', label: '冰箱', payload: { group: 'Dining', name: 'Refrigerator' } },
      { key: 'grill', label: '烤箱', payload: { group: 'Dining', name: 'Oven' } },
      { key: 'wine', label: '酒杯', payload: { group: 'Dining', name: 'Wine Glasses' } },
      { key: 'service24h', label: '24小时客房服务', payload: { group: 'Dining', name: '24-hour Room Service' } },
      { key: 'hotWater', label: '电热水壶', payload: { group: 'Dining', name: 'Electric Kettle' } },
      { key: 'microwave', label: '迷你吧', payload: { group: 'Dining', name: 'Minibar' } },
      { key: 'coffeeService', label: '咖啡和茶设施', payload: { group: 'Dining', name: 'Coffee and Tea Facilities' } },
      { key: 'stove', label: '炉灶', payload: { group: 'Dining', name: 'Stove' } },
      { key: 'cutlery', label: '卫星频道', payload: { group: 'Dining', name: 'Cutlery' } },
    ],
  },
  {
    title: '娱乐设施',
    items: [
      { key: 'newspaper', label: '报纸', payload: { group: 'Entertainment', name: 'Newspaper' } },
      { key: 'cableTv', label: '付费电视', payload: { group: 'Entertainment', name: 'Pay TV' } },
      { key: 'satellite', label: '卫星频道', payload: { group: 'Entertainment', name: 'Satellite Channels' } },
      { key: 'phone', label: '电话', payload: { group: 'Entertainment', name: 'Telephone' } },
      { key: 'radio', label: '收音机', payload: { group: 'Entertainment', name: 'Radio' } },
    ],
  },
  {
    title: '更多设施',
    items: [
      { key: 'wardrobe', label: '衣物储存', payload: { group: 'More', name: 'Wardrobe' } },
      { key: 'dryer', label: '烘干机', payload: { group: 'More', name: 'Dryer' } },
      { key: 'rack', label: '衣架', payload: { group: 'More', name: 'Clothes Rack' } },
      { key: 'mosquito', label: '蚊帐', payload: { group: 'More', name: 'Mosquito Net' } },
      { key: 'washer', label: '洗衣机', payload: { group: 'More', name: 'Washing Machine' } },
      { key: 'curtain', label: '遮光窗帘', payload: { group: 'More', name: 'Blackout Curtains' } },
      { key: 'toys', label: '提供婴儿床', payload: { group: 'Family', name: 'Crib Available' } },
      { key: 'board', label: '婴斗和婴衣板', payload: { group: 'Family', name: 'Baby Bath and High Chair' } },
    ],
  },
]
