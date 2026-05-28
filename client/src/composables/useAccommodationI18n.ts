import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

type SimpleOption<T extends string | number = string | number> = {
  label: string
  value: T
}

const WEEKDAY_VALUES = [1, 2, 3, 4, 5, 6, 7] as const

export const useAccommodationI18n = () => {
  const { t, locale } = useI18n()

  const weekdayOptions = computed<SimpleOption<number>[]>(() => [
    { label: t('accommodation.common.all'), value: 0 },
    ...WEEKDAY_VALUES.map((value) => ({
      label: t(`accommodation.weekdays.short.${value}`),
      value,
    })),
  ])

  const weekdayShortMap = computed<Record<number, string>>(() => ({
    0: t('accommodation.weekdays.dayOfWeek.0'),
    1: t('accommodation.weekdays.dayOfWeek.1'),
    2: t('accommodation.weekdays.dayOfWeek.2'),
    3: t('accommodation.weekdays.dayOfWeek.3'),
    4: t('accommodation.weekdays.dayOfWeek.4'),
    5: t('accommodation.weekdays.dayOfWeek.5'),
    6: t('accommodation.weekdays.dayOfWeek.6'),
  }))

  const weekdayFullMap = computed<Record<number, string>>(() => ({
    0: t('accommodation.weekdays.full.0'),
    1: t('accommodation.weekdays.full.1'),
    2: t('accommodation.weekdays.full.2'),
    3: t('accommodation.weekdays.full.3'),
    4: t('accommodation.weekdays.full.4'),
    5: t('accommodation.weekdays.full.5'),
    6: t('accommodation.weekdays.full.6'),
  }))

  const paymentMethodOptions = computed<SimpleOption<string>[]>(() => [
    { label: t('accommodation.paymentMethods.wechat'), value: 'wechat' },
    { label: t('accommodation.paymentMethods.alipay'), value: 'alipay' },
    { label: t('accommodation.paymentMethods.cash'), value: 'cash' },
  ])

  const onOffOptions = computed<SimpleOption<string>[]>(() => [
    { label: t('accommodation.common.on'), value: 'on' },
    { label: t('accommodation.common.off'), value: 'off' },
  ])

  const roomPriceSettingOptions = computed<SimpleOption<string>[]>(() => [
    { label: t('accommodation.roomPrice.settingType.price'), value: 'price' },
    { label: t('accommodation.roomPrice.settingType.minStay'), value: 'minStay' },
    { label: t('accommodation.roomPrice.settingType.maxStay'), value: 'maxStay' },
    { label: t('accommodation.roomPrice.settingType.closeRoom'), value: 'closeRoom' },
    { label: t('accommodation.roomPrice.settingType.cta'), value: 'cta' },
    { label: t('accommodation.roomPrice.settingType.ctd'), value: 'ctd' },
  ])

  const bulkSettingOptions = computed<SimpleOption<string>[]>(() => [
    { label: t('accommodation.roomPrice.settingType.price'), value: 'price' },
    { label: t('accommodation.roomPrice.settingType.minStay'), value: 'minStay' },
    { label: t('accommodation.roomPrice.settingType.maxStay'), value: 'maxStay' },
  ])

  const closeRoomTypeOptions = computed<SimpleOption<string>[]>(() => [
    { label: t('roomStatus.closeRoom.type.stop'), value: 'stop' },
    { label: t('roomStatus.closeRoom.type.maintenance'), value: 'maintenance' },
    { label: t('roomStatus.closeRoom.type.retain'), value: 'retain' },
  ])

  const batchDateSelectorOptions = computed<SimpleOption<string>[]>(() => [
    { label: t('roomStatus.batchDateSelector.all'), value: 'all' },
    { label: t('roomStatus.batchDateSelector.weekday'), value: 'weekday' },
    { label: t('roomStatus.batchDateSelector.weekend'), value: 'weekend' },
  ])

  const closeRoomTypeLabelMap = computed<Record<string, string>>(() => ({
    stop: t('roomStatus.closeRoom.type.stop'),
    maintenance: t('roomStatus.closeRoom.type.maintenance'),
    retain: t('roomStatus.closeRoom.type.retain'),
  }))

  const batchDateSelectorLabelMap = computed<Record<string, string>>(() => ({
    all: t('roomStatus.batchDateSelector.all'),
    weekday: t('roomStatus.batchDateSelector.weekday'),
    weekend: t('roomStatus.batchDateSelector.weekend'),
  }))

  const getDateLocale = () => {
    switch (locale.value) {
      case 'zh-TW':
        return 'zh-TW'
      case 'en':
        return 'en-US'
      case 'ja':
        return 'ja-JP'
      default:
        return 'zh-CN'
    }
  }

  const formatMonthDay = (value: Date) => {
    const formatter = new Intl.DateTimeFormat(getDateLocale(), {
      month: '2-digit',
      day: '2-digit',
    })

    return formatter.format(value)
  }

  return {
    batchDateSelectorLabelMap,
    batchDateSelectorOptions,
    bulkSettingOptions,
    closeRoomTypeLabelMap,
    closeRoomTypeOptions,
    formatMonthDay,
    onOffOptions,
    paymentMethodOptions,
    roomPriceSettingOptions,
    weekdayFullMap,
    weekdayOptions,
    weekdayShortMap,
  }
}
