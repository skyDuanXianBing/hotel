export interface StoreOption {
  label: string
  labelKey?: string
  value: string
}

export const PROPERTY_TYPE_OPTIONS: StoreOption[] = [
  { label: 'Hotel', labelKey: 'stage6.constants.storeOptions.propertyType.hotel', value: '1' },
  { label: 'Motel', labelKey: 'stage6.constants.storeOptions.propertyType.motel', value: '2' },
  {
    label: 'Vacation Rental',
    labelKey: 'stage6.constants.storeOptions.propertyType.vacationRental',
    value: '3',
  },
]

export const LANGUAGE_OPTIONS: StoreOption[] = [
  { label: 'English（en）', labelKey: 'stage6.constants.storeOptions.language.en', value: 'en' },
  { label: 'Simplified Chinese (zh)', labelKey: 'stage6.constants.storeOptions.language.zh', value: 'zh' },
  { label: 'Japanese (ja)', labelKey: 'stage6.constants.storeOptions.language.ja', value: 'ja' },
  { label: '한국어（ko）', labelKey: 'stage6.constants.storeOptions.language.ko', value: 'ko' },
]

export const COUNTRY_OPTIONS: StoreOption[] = [
  { label: 'China', labelKey: 'stage6.constants.storeOptions.country.china', value: 'China' },
  { label: 'Japan', labelKey: 'stage6.constants.storeOptions.country.japan', value: 'Japan' },
  {
    label: 'South Korea',
    labelKey: 'stage6.constants.storeOptions.country.southKorea',
    value: 'South Korea',
  },
  {
    label: 'United Kingdom',
    labelKey: 'stage6.constants.storeOptions.country.unitedKingdom',
    value: 'United Kingdom',
  },
  { label: 'United States', labelKey: 'stage6.constants.storeOptions.country.usa', value: 'USA' },
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
  { label: 'CNY - Chinese Yuan', labelKey: 'stage6.constants.storeOptions.currency.cny', value: 'CNY' },
  { label: 'JPY - Japanese Yen', labelKey: 'stage6.constants.storeOptions.currency.jpy', value: 'JPY' },
  { label: 'KRW - Korean Won', labelKey: 'stage6.constants.storeOptions.currency.krw', value: 'KRW' },
  { label: 'USD - US Dollar', labelKey: 'stage6.constants.storeOptions.currency.usd', value: 'USD' },
  { label: 'GBP - British Pound', labelKey: 'stage6.constants.storeOptions.currency.gbp', value: 'GBP' },
]

export const getStoreOptionLabel = (
  options: StoreOption[],
  value?: string,
  translate?: (key: string) => string,
): string => {
  if (!value) {
    return '-'
  }

  const option = options.find((item) => item.value === value)
  if (!option) {
    return value
  }

  return option.labelKey && translate ? translate(option.labelKey) : option.label
}
