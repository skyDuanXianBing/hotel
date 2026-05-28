import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import {
  LOCALE_STORAGE_KEY,
  getInitialLocale,
  i18n,
  type SupportedLocale,
} from '@/locales'

export const useLanguageStore = defineStore('language', () => {
  const locale = ref<SupportedLocale>(getInitialLocale())

  const syncLocale = (value: SupportedLocale) => {
    i18n.global.locale.value = value
    document.documentElement.lang = value
    localStorage.setItem(LOCALE_STORAGE_KEY, value)
  }

  const setLocale = (value: SupportedLocale) => {
    locale.value = value
  }

  watch(locale, syncLocale, { immediate: true })

  return {
    locale,
    setLocale,
  }
})
