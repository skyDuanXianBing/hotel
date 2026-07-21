import { i18n, type SupportedLocale } from '@/locales'

const intlLocaleMap: Record<SupportedLocale, string> = {
  en: 'en-US',
  ja: 'ja-JP',
  'zh-CN': 'zh-CN',
  'zh-TW': 'zh-TW',
}

const DEFAULT_CURRENCY = 'CNY'
const DEFAULT_MONEY_LOCALE = 'zh-CN'

export type MoneyDisplayMode = 'CNY_COMPAT' | 'REQUESTED_CURRENCY'

// Keep multi-currency support available while matching the current web client display.
export const MONEY_DISPLAY_MODE: MoneyDisplayMode = 'CNY_COMPAT'

const countryMoneyLocaleMap: Record<string, string> = {
  china: 'zh-CN',
  japan: 'ja-JP',
  'south korea': 'ko-KR',
  'united kingdom': 'en-GB',
  usa: 'en-US',
}

const currencyMoneyLocaleMap: Record<string, string> = {
  CNY: 'zh-CN',
  JPY: 'ja-JP',
  KRW: 'ko-KR',
  USD: 'en-US',
  GBP: 'en-GB',
}

export interface MoneyDisplayContext {
  country?: string | null
  locale?: string | null
}

export type MoneyFormatOptions = Omit<Intl.NumberFormatOptions, 'style' | 'currency'>

export const getIntlLocale = (locale: string = i18n.global.locale.value): string =>
  intlLocaleMap[locale as SupportedLocale] || intlLocaleMap['zh-CN']

export const formatDate = (
  value: Date | string | number,
  options: Intl.DateTimeFormatOptions = {},
  locale = i18n.global.locale.value,
) =>
  new Intl.DateTimeFormat(getIntlLocale(locale), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    ...options,
  }).format(new Date(value))

export const formatDateTime = (
  value: Date | string | number,
  options: Intl.DateTimeFormatOptions = {},
  locale = i18n.global.locale.value,
) =>
  new Intl.DateTimeFormat(getIntlLocale(locale), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    ...options,
  }).format(new Date(value))

export const formatNumber = (
  value: number,
  options: Intl.NumberFormatOptions = {},
  locale = i18n.global.locale.value,
) => new Intl.NumberFormat(getIntlLocale(locale), options).format(value)

export const formatPercent = (
  value: number,
  options: Intl.NumberFormatOptions = {},
  locale = i18n.global.locale.value,
) =>
  new Intl.NumberFormat(getIntlLocale(locale), {
    style: 'percent',
    ...options,
  }).format(value / 100)

export const normalizeCurrencyCode = (currency?: string | null): string => {
  const normalized = String(currency || '')
    .trim()
    .toUpperCase()

  return /^[A-Z]{3}$/u.test(normalized) ? normalized : DEFAULT_CURRENCY
}

export const resolveDisplayCurrencyCode = (currency?: string | null): string => {
  if (MONEY_DISPLAY_MODE === 'CNY_COMPAT') {
    return DEFAULT_CURRENCY
  }
  return normalizeCurrencyCode(currency)
}

const isValidIntlLocale = (locale?: string | null): locale is string => {
  if (!locale) {
    return false
  }

  try {
    new Intl.NumberFormat(locale).format(0)
    return true
  } catch {
    return false
  }
}

export const resolveMoneyLocale = (
  currency?: string | null,
  context: MoneyDisplayContext = {},
): string => {
  if (isValidIntlLocale(context.locale)) {
    return context.locale
  }

  const country = String(context.country || '')
    .trim()
    .toLowerCase()
  if (countryMoneyLocaleMap[country]) {
    return countryMoneyLocaleMap[country]
  }

  const normalizedCurrency = normalizeCurrencyCode(currency)
  return currencyMoneyLocaleMap[normalizedCurrency] || DEFAULT_MONEY_LOCALE
}

export const formatMoney = (
  value: unknown,
  currency = DEFAULT_CURRENCY,
  options: MoneyFormatOptions = {},
  context: MoneyDisplayContext = {},
): string => {
  const normalizedCurrency = resolveDisplayCurrencyCode(currency)
  const numericValue = Number(value)
  const amount = Number.isFinite(numericValue) ? numericValue : 0
  const displayLocale =
    MONEY_DISPLAY_MODE === 'CNY_COMPAT'
      ? DEFAULT_MONEY_LOCALE
      : resolveMoneyLocale(normalizedCurrency, context)

  return new Intl.NumberFormat(displayLocale, {
    currencyDisplay: 'symbol',
    ...options,
    style: 'currency',
    currency: normalizedCurrency,
  }).format(amount)
}

export const getCurrencySymbol = (
  currency: string,
  context: MoneyDisplayContext = {},
): string => {
  const normalizedCurrency = resolveDisplayCurrencyCode(currency)
  const displayLocale =
    MONEY_DISPLAY_MODE === 'CNY_COMPAT'
      ? DEFAULT_MONEY_LOCALE
      : resolveMoneyLocale(normalizedCurrency, context)
  const currencyPart = new Intl.NumberFormat(displayLocale, {
    style: 'currency',
    currency: normalizedCurrency,
    currencyDisplay: 'symbol',
  })
    .formatToParts(0)
    .find((part) => part.type === 'currency')

  return currencyPart?.value || normalizedCurrency
}

export const compareLocalizedText = (
  left: string,
  right: string,
  options: Intl.CollatorOptions = { numeric: true, sensitivity: 'base' },
  locale = i18n.global.locale.value,
) => new Intl.Collator(getIntlLocale(locale), options).compare(left, right)
