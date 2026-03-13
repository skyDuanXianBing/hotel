export interface StoreOption {
  label: string
  value: string
}

export const PROPERTY_TYPE_OPTIONS: StoreOption[] = [
  { label: 'Hotel（酒店）', value: '1' },
  { label: 'Motel（汽车旅馆）', value: '2' },
  { label: 'Vacation Rental（度假出租）', value: '3' },
]

export const LANGUAGE_OPTIONS: StoreOption[] = [
  { label: 'English（en）', value: 'en' },
  { label: '简体中文（zh）', value: 'zh' },
  { label: '日本語（ja）', value: 'ja' },
  { label: '한국어（ko）', value: 'ko' },
]

export const COUNTRY_OPTIONS: StoreOption[] = [
  { label: '中国', value: 'China' },
  { label: '日本', value: 'Japan' },
  { label: '韩国', value: 'South Korea' },
  { label: '英国', value: 'United Kingdom' },
  { label: '美国', value: 'USA' },
]

export const PHONE_PREFIX_OPTIONS: StoreOption[] = [
  { label: '+86', value: '+86' },
  { label: '+81', value: '+81' },
  { label: '+82', value: '+82' },
  { label: '+44', value: '+44' },
  { label: '+1', value: '+1' },
]

export const TIMEZONE_OPTIONS: StoreOption[] = [
  { label: 'Asia/Shanghai', value: 'Asia/Shanghai' },
  { label: 'Asia/Tokyo', value: 'Asia/Tokyo' },
  { label: 'Asia/Seoul', value: 'Asia/Seoul' },
  { label: 'Europe/London', value: 'Europe/London' },
  { label: 'America/New_York', value: 'America/New_York' },
]

export const CURRENCY_OPTIONS: StoreOption[] = [
  { label: 'CNY - 人民币', value: 'CNY' },
  { label: 'JPY - 日元', value: 'JPY' },
  { label: 'KRW - 韩元', value: 'KRW' },
  { label: 'USD - 美元', value: 'USD' },
  { label: 'GBP - 英镑', value: 'GBP' },
]

export const getStoreOptionLabel = (options: StoreOption[], value?: string): string => {
  if (!value) {
    return '-'
  }
  return options.find((option) => option.value === value)?.label || value
}
