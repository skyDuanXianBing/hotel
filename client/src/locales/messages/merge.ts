import type { LocaleMessageValue, VueMessageType } from 'vue-i18n'

type LocaleMessageRecord = Record<string, LocaleMessageValue<VueMessageType>>
type LocaleMessageSource = Record<string, unknown>

const isPlainRecord = (value: unknown): value is LocaleMessageRecord => {
  return Object.prototype.toString.call(value) === '[object Object]'
}

const mergeRecords = (
  target: LocaleMessageRecord,
  source: LocaleMessageSource,
): LocaleMessageRecord => {
  const merged: LocaleMessageRecord = { ...target }

  for (const [key, sourceValue] of Object.entries(source)) {
    const targetValue = merged[key]
    if (isPlainRecord(targetValue) && isPlainRecord(sourceValue)) {
      merged[key] = mergeRecords(targetValue, sourceValue) as LocaleMessageValue<VueMessageType>
    } else {
      merged[key] = sourceValue as LocaleMessageValue<VueMessageType>
    }
  }

  return merged
}

export const mergeLocaleMessages = (...sources: object[]): LocaleMessageRecord => {
  let result: LocaleMessageRecord = {}

  for (const source of sources) {
    result = mergeRecords(result, source as LocaleMessageSource)
  }

  return result
}
