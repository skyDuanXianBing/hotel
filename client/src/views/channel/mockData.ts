import type {
  HotelItem,
  RoomMappingGroup,
  CalendarDate,
  CalendarRow,
  RoomSettingsRow,
} from './types'

/** 渠道 Logo 映射表 */
export const CHANNEL_LOGO_MAP: Record<string, string> = {
  'Agoda': 'https://cdn.worldvectorlogo.com/logos/agoda-1.svg',
  'Airbnb': 'https://upload.wikimedia.org/wikipedia/commons/6/69/Airbnb_Logo_B%C3%A9lo.svg',
  'Booking.com': 'https://upload.wikimedia.org/wikipedia/commons/b/be/Booking.com_logo.svg',
  'Traveloka': 'https://upload.wikimedia.org/wikipedia/commons/5/5b/Traveloka_logo.svg',
  'Trip.com': 'https://ak-d.tripcdn.com/images/0ww5h12000c6vhxm53B87.png',
  'Expedia': 'https://upload.wikimedia.org/wikipedia/commons/5/5b/Expedia_2012_logo.svg',
  'Tiket.com': 'https://www.tiket.com/img/tiket-logo.svg',
  'HostelWorld':
    'https://a.hwstatic.com/image/upload/f_auto,q_auto,h_63/v1/propertyimages/0/8914/x5ecdkqgtrzfmcyiykfb',
  'TuJia': 'https://pages.c-ctrip.com/hotels/wuhan/tujia-logo.png',
  'Neppan': 'https://via.placeholder.com/120x60/FF6B35/FFFFFF?text=Neppan',
}

/** 渠道分类（介绍页） */
export const CHANNEL_CATEGORIES = [
  {
    name: '国内OTA',
    items: ['携程', '去哪儿', '美团', '飞猪', '途牛', '同程', '艺龙'],
  },
  {
    name: '民宿平台',
    items: ['途家', '小猪民宿', '木鸟民宿', '榛果民宿', 'Airbnb'],
  },
  {
    name: '国际OTA',
    items: ['Booking.com', 'Expedia', 'Agoda', 'Hotels.com', 'Priceline'],
  },
]

/** 生成日历日期列表 */
export const generateCalendarDates = (startDate: string, days: number = 10): CalendarDate[] => {
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const dates: CalendarDate[] = []
  const start = new Date(startDate)

  for (let i = 0; i < days; i++) {
    const date = new Date(start)
    date.setDate(start.getDate() + i)
    const month = date.getMonth() + 1
    const day = date.getDate()
    dates.push({
      value: date.toISOString().split('T')[0],
      label: `${month}月${day}日`,
      day: `${month}月${day.toString().padStart(2, '0')}日`,
      weekday: weekdays[date.getDay()],
    })
  }
  return dates
}

/** 模拟酒店数据生成器（根据渠道不同生成不同的数据） */
export const generateMockHotelData = (channelName: string): HotelItem[] => {
  const baseId = Date.now()

  const mockDataByChannel: Record<string, HotelItem[]> = {
    'Booking.com': [
      {
        id: baseId,
        hotelCode: '14540318',
        hotelName: 'Tanpopo Inn',
        priceMode: 'Occupancy Based Price',
        status: 'Active',
      },
      {
        id: baseId + 1,
        hotelCode: '14844797',
        hotelName: '楽途ホテル 東十条',
        priceMode: 'Occupancy Based Price',
        status: 'Active',
      },
      {
        id: baseId + 2,
        hotelCode: '14850604',
        hotelName: '楽途ホテル 池袋',
        priceMode: 'Occupancy Based Price',
        status: 'Active',
      },
    ],
    'Agoda': [
      {
        id: baseId,
        hotelCode: 'AGD001',
        hotelName: 'Tokyo Central Hotel',
        priceMode: 'Per Night',
        status: 'Active',
      },
      {
        id: baseId + 1,
        hotelCode: 'AGD002',
        hotelName: 'Osaka Bay Resort',
        priceMode: 'Per Night',
        status: 'Active',
      },
    ],
    'Airbnb': [
      {
        id: baseId,
        hotelCode: 'ABB001',
        hotelName: 'Cozy Tokyo Apartment',
        priceMode: 'Per Night',
        status: 'Active',
      },
      {
        id: baseId + 1,
        hotelCode: 'ABB002',
        hotelName: 'Shibuya Modern Studio',
        priceMode: 'Per Night',
        status: 'Active',
      },
    ],
    'Trip.com': [
      {
        id: baseId,
        hotelCode: 'TRIP001',
        hotelName: '東京スカイホテル',
        priceMode: 'Room Rate',
        status: 'Active',
      },
    ],
    'Expedia': [
      {
        id: baseId,
        hotelCode: 'EXP001',
        hotelName: 'Grand Tokyo Hotel',
        priceMode: 'Per Room Per Night',
        status: 'Active',
      },
    ],
  }

  return (
    mockDataByChannel[channelName] || [
      {
        id: baseId,
        hotelCode: `${channelName.substring(0, 3).toUpperCase()}001`,
        hotelName: `${channelName} Hotel 1`,
        priceMode: 'Standard',
        status: 'Active',
      },
    ]
  )
}

/** 生成映射模拟数据 */
export const generateMockMappingData = (hotelName: string): RoomMappingGroup[] => {
  return [
    {
      roomGroupId: 'room_1454031803',
      channelRoomType: 'Economy Double Room',
      channelRoomId: '1454031803',
      pmsRoomType: `${hotelName}103`,
      selectedPmsRoom: `${hotelName}103`,
      pricePlans: [
        {
          id: 'plan_57653133',
          channelPricePlan: 'Fully flexible(non-manageable)',
          channelPricePlanId: '57653133',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'invalid',
        },
        {
          id: 'plan_57652901',
          channelPricePlan: 'Non-refundable(non-manageable)',
          channelPricePlanId: '57652901',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'invalid',
        },
        {
          id: 'plan_56459960',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: '56459960',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
        {
          id: 'plan_57593856',
          channelPricePlan: 'Weekly rate(non-manageable)',
          channelPricePlanId: '57593856',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'disconnected',
        },
        {
          id: 'plan_57715884',
          channelPricePlan: 'Fully flexible(non-manageable)',
          channelPricePlanId: '57715884',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'disconnected',
        },
        {
          id: 'plan_57715921',
          channelPricePlan: 'Weekly rate(non-manageable)',
          channelPricePlanId: '57715921',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'disconnected',
        },
        {
          id: 'plan_57716084',
          channelPricePlan: 'Early booker rate plan (90+ days)(non-manageable)',
          channelPricePlanId: '57716084',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'disconnected',
        },
        {
          id: 'plan_57715907',
          channelPricePlan: 'Non-refundable(non-manageable)',
          channelPricePlanId: '57715907',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'disconnected',
        },
      ],
    },
    {
      roomGroupId: 'room_1454031804',
      channelRoomType: 'Standard Double Room',
      channelRoomId: '1454031804',
      pmsRoomType: `${hotelName}104`,
      selectedPmsRoom: `${hotelName}104`,
      pricePlans: [
        {
          id: 'plan_56459961',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: '56459961',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
      ],
    },
  ]
}

/** 生成日历模拟数据 */
export const generateMockCalendarData = (
  hotelName: string,
  dates: CalendarDate[],
): CalendarRow[] => {
  const rooms = [`${hotelName}103`, `${hotelName}104`]
  const rows: CalendarRow[] = []

  rooms.forEach((room, roomIndex) => {
    const roomId = `room_${roomIndex}`
    // 房型标题行
    rows.push({
      id: `${roomId}_title`,
      label: `${room}(Economy Double Room-145403180${3 + roomIndex})`,
      type: 'title',
      roomId,
      values: {},
    })
    // 房量行
    const inventoryValues: Record<string, number> = {}
    dates.forEach((d) => {
      inventoryValues[d.value] = 0
    })
    rows.push({
      id: `${roomId}_inventory`,
      label: '房量',
      type: 'inventory',
      roomId,
      values: inventoryValues,
    })
    // Standard Rate 标题
    rows.push({
      id: `${roomId}_rate_title`,
      label: '▼ Standard Rate',
      type: 'title',
      roomId,
      values: {},
    })
    // 价格行
    const priceValues: Record<string, number> = {}
    dates.forEach((d) => {
      const basePrice = 11420.2
      const dayOfWeek = new Date(d.value).getDay()
      priceValues[d.value] = dayOfWeek === 0 || dayOfWeek === 6 ? basePrice * 1.1 : basePrice
    })
    rows.push({
      id: `${roomId}_price`,
      label: '价格',
      type: 'price',
      roomId,
      values: priceValues,
    })
    // 2人价格
    const price2Values: Record<string, string> = {}
    dates.forEach((d) => {
      price2Values[d.value] = '-'
    })
    rows.push({
      id: `${roomId}_price2`,
      label: '2人价格',
      type: 'number',
      roomId,
      values: price2Values,
    })
    // 最小入住天数
    const minStayValues: Record<string, number> = {}
    dates.forEach((d) => {
      minStayValues[d.value] = 1
    })
    rows.push({
      id: `${roomId}_minstay`,
      label: '最小入住天数',
      type: 'number',
      roomId,
      values: minStayValues,
    })
    // 最大入住天数
    const maxStayValues: Record<string, number> = {}
    dates.forEach((d) => {
      maxStayValues[d.value] = 365
    })
    rows.push({
      id: `${roomId}_maxstay`,
      label: '最大入住天数',
      type: 'number',
      roomId,
      values: maxStayValues,
    })
    // 关房
    const closeValues: Record<string, boolean> = {}
    dates.forEach((d) => {
      closeValues[d.value] = false
    })
    rows.push({
      id: `${roomId}_close`,
      label: '关房',
      type: 'checkbox',
      roomId,
      values: closeValues,
    })
    // CTA
    const ctaValues: Record<string, boolean> = {}
    dates.forEach((d) => {
      ctaValues[d.value] = false
    })
    rows.push({
      id: `${roomId}_cta`,
      label: 'CTA',
      type: 'checkbox',
      roomId,
      values: ctaValues,
    })
    // CTD
    const ctdValues: Record<string, boolean> = {}
    dates.forEach((d) => {
      ctdValues[d.value] = false
    })
    rows.push({
      id: `${roomId}_ctd`,
      label: 'CTD',
      type: 'checkbox',
      roomId,
      values: ctdValues,
    })
  })

  return rows
}

/** 生成 Airbnb 映射数据 */
export const generateAirbnbMappingData = (): RoomMappingGroup[] => {
  return [
    {
      roomGroupId: 'airbnb_room_1',
      channelRoomType:
        '串十条丁目2F(LS012)  higashijujo 2F ・《Rakuto Higashijujo 2F》Good Access to Ikebukuro',
      channelRoomId: '115771838797582817A',
      pmsRoomType: '楽途ホテル  串十条2F',
      selectedPmsRoom: '楽途ホテル  串十条2F',
      pricePlans: [
        {
          id: 'airbnb_plan_1',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: 'STANDARD',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
      ],
    },
    {
      roomGroupId: 'airbnb_room_2',
      channelRoomType:
        '串十条丁目3・4F(LS013)  higashijujo1 3-4F ・《Rakuto Higashijujo 2F》Good Access to Ikebukuro',
      channelRoomId: '116520872962299543I',
      pmsRoomType: '楽途ホテル  串十条3/4F',
      selectedPmsRoom: '楽途ホテル  串十条3/4F',
      pricePlans: [
        {
          id: 'airbnb_plan_2',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: 'STANDARD',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
      ],
    },
    {
      roomGroupId: 'airbnb_room_3',
      channelRoomType: '楽途ホテル  池袋・《Rakuto Hotel Ikebukuro》Direct to Shinjuku',
      channelRoomId: '131261457580594907',
      pmsRoomType: '楽途ホテル  池袋201',
      selectedPmsRoom: '楽途ホテル  池袋201',
      pricePlans: [
        {
          id: 'airbnb_plan_3',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: 'STANDARD',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
      ],
    },
    {
      roomGroupId: 'airbnb_room_4',
      channelRoomType:
        '串十条丁目1F  higashi jujo 1 1F ・《Rakuto Higashijujo 1F》Good Access to Ikebukuro',
      channelRoomId: '141957367786915011A',
      pmsRoomType: '楽途ホテル  串十条1F',
      selectedPmsRoom: '楽途ホテル  串十条1F',
      pricePlans: [
        {
          id: 'airbnb_plan_4',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: 'STANDARD',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
      ],
    },
  ]
}

/** Airbnb 房型列表（用于房量设置 Tab） */
const AIRBNB_ROOM_LIST = [
  {
    airbnbRoomType:
      '串十条丁目2F(LS012)  higashijujo 2F ・《Rakuto Higashijujo 2F》Good Access to Ikebukuro (115771838797582817A)',
    pmsRoomType: '楽途ホテル  串十条2F',
  },
  {
    airbnbRoomType:
      '串十条丁目3・4F(LS013)  higashijujo1 3-4F ・《Rakuto Higashijujo 2F》Good Access to Ikebukuro (116520872962299543I)',
    pmsRoomType: '楽途ホテル  串十条3/4F',
  },
  {
    airbnbRoomType:
      '楽途ホテル  池袋・《Rakuto Hotel Ikebukuro》Direct to Shinjuku (131261457580594907)',
    pmsRoomType: '楽途ホテル  池袋201',
  },
  {
    airbnbRoomType:
      '串十条丁目1F  higashi jujo 1 1F ・《Rakuto Higashijujo 1F》Good Access to Ikebukuro (141957367786915011A)',
    pmsRoomType: '楽途ホテル  串十条1F',
  },
]

/** 生成 Airbnb 房量设置数据 */
export const generateRoomSettingsRows = (dates: CalendarDate[]): RoomSettingsRow[] => {
  return AIRBNB_ROOM_LIST.map((room, index) => {
    const values: Record<string, any> = {}
    dates.forEach((date) => {
      values[date.value] = ''
    })
    return {
      id: `room_${index}`,
      airbnbRoomType: room.airbnbRoomType,
      pmsRoomType: room.pmsRoomType,
      values,
    }
  })
}
