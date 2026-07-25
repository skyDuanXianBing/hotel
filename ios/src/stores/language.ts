import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  activateLocale,
  LOCALE_STORAGE_KEY,
  getInitialLocale,
  i18n,
  type SupportedLocale,
} from '@/locales'

export const useLanguageStore = defineStore('language', () => {
  const locale = ref<SupportedLocale>(getInitialLocale())
  const isLoading = ref(false)

  const syncBackButtonText = () => {
    if (typeof window === 'undefined' || typeof document === 'undefined') {
      return
    }

    const backButtonText = i18n.global.t('common.back')
    const ionicConfig = (
      window as Window & {
        Ionic?: {
          config?: {
            set?: (key: string, value: unknown) => void
          }
        }
      }
    ).Ionic?.config

    ionicConfig?.set?.('backButtonText', backButtonText)

    document.querySelectorAll('ion-back-button').forEach((element) => {
      const backButton = element as HTMLElement & { text?: string }
      backButton.text = backButtonText
    })
  }

  const syncLocaleMetadata = (value: SupportedLocale) => {
    if (typeof document !== 'undefined') {
      document.documentElement.lang = value
    }
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(LOCALE_STORAGE_KEY, value)
    }

    syncBackButtonText()
  }

  const setLocale = async (value: SupportedLocale) => {
    if (isLoading.value) {
      return false
    }

    isLoading.value = true
    try {
      await activateLocale(value)
      locale.value = value
      syncLocaleMetadata(value)
      return true
    } catch {
      return false
    } finally {
      isLoading.value = false
    }
  }

  const initialize = async () => {
    await setLocale(locale.value)
  }

  return {
    locale,
    isLoading,
    initialize,
    setLocale,
  }
})
