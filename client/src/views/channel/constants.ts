/** 渠道 Logo 映射表 */
export const CHANNEL_LOGO_MAP: Record<string, string> = {
  Agoda: 'https://cdn.worldvectorlogo.com/logos/agoda-1.svg',
  Airbnb: 'https://upload.wikimedia.org/wikipedia/commons/6/69/Airbnb_Logo_B%C3%A9lo.svg',
  'Booking.com': 'https://upload.wikimedia.org/wikipedia/commons/b/be/Booking.com_logo.svg',
  Traveloka: 'https://upload.wikimedia.org/wikipedia/commons/5/5b/Traveloka_logo.svg',
  'Trip.com': 'https://ak-d.tripcdn.com/images/0ww5h12000c6vhxm53B87.png',
  Expedia: 'https://upload.wikimedia.org/wikipedia/commons/5/5b/Expedia_2012_logo.svg',
  'Tiket.com': 'https://www.tiket.com/img/tiket-logo.svg',
  HostelWorld:
    'https://a.hwstatic.com/image/upload/f_auto,q_auto,h_63/v1/propertyimages/0/8914/x5ecdkqgtrzfmcyiykfb',
  TuJia: 'https://pages.c-ctrip.com/hotels/wuhan/tujia-logo.png',
  Neppan: 'https://via.placeholder.com/120x60/FF6B35/FFFFFF?text=Neppan',
}

/** 渠道分类（介绍页） */
export const CHANNEL_CATEGORIES = [
  {
    key: 'domesticOta',
    name: '国内OTA',
    items: ['携程', '去哪儿', '美团', '飞猪', '途牛', '同程', '艺龙'],
  },
  {
    key: 'homestay',
    name: '民宿平台',
    items: ['途家', '小猪民宿', '木鸟民宿', '榛果民宿', 'Airbnb'],
  },
  {
    key: 'internationalOta',
    name: '国际OTA',
    items: ['Booking.com', 'Expedia', 'Agoda', 'Hotels.com', 'Priceline'],
  },
]
