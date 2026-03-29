import type { StoreRequest } from '@/types/store'

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

export const STORE_NAME_MAX_LENGTH = 50
export const STORE_PHONE_MIN_LENGTH = 6
export const STORE_PHONE_MAX_LENGTH = 20

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

export const CREATE_STORE_DEFAULTS: Pick<
  StoreRequest,
  | 'phoneTechType'
  | 'type'
  | 'timezone'
  | 'country'
  | 'currency'
  | 'language'
  | 'checkinTime'
  | 'checkoutTime'
  | 'createSuProperty'
> = {
  phoneTechType: '5',
  type: '1',
  timezone: 'Asia/Shanghai',
  country: 'China',
  currency: 'CNY',
  language: 'en',
  checkinTime: '15:00',
  checkoutTime: '11:00',
  createSuProperty: true,
}

export const STORE_TIME_OPTIONS: StoreOption[] = Array.from({ length: 48 }, (_, index) => {
  const hour = String(Math.floor(index / 2)).padStart(2, '0')
  const minute = index % 2 === 0 ? '00' : '30'
  const value = `${hour}:${minute}`

  return {
    label: value,
    value,
  }
})
