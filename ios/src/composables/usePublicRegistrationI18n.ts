import {
  computed,
  onBeforeUnmount,
  ref,
  toValue,
  watch,
  type MaybeRefOrGetter,
} from 'vue'
import { useI18n } from 'vue-i18n'
import { LOCALE_STORAGE_KEY, resolveLocale } from '@/locales'
import {
  publicRegistrationMessages,
  type PublicRegistrationMessageScope,
} from '@/locales/messages/publicRegistration'
import type { PublicRegistrationLanguage } from '@/types/publicRegistration'
import {
  PUBLIC_REGISTRATION_LANGUAGES,
  resolvePublicRegistrationLanguage,
  writePublicRegistrationLanguage,
} from '@/utils/publicRegistration'

const languageLabels: Record<
  PublicRegistrationLanguage,
  { label: string; shortLabel: string }
> = {
  'zh-CN': { label: '简体中文', shortLabel: '简' },
  'zh-TW': { label: '繁體中文', shortLabel: '繁' },
  en: { label: 'English', shortLabel: 'EN' },
  ja: { label: '日本語', shortLabel: 'JA' },
}

export const PUBLIC_REGISTRATION_LANGUAGE_OPTIONS = PUBLIC_REGISTRATION_LANGUAGES.map(
  (value) => ({
    value,
    ...languageLabels[value],
  }),
)

export const usePublicRegistrationI18n = (
  scope: PublicRegistrationMessageScope,
  queryLanguage?: MaybeRefOrGetter<unknown>,
) => {
  const initialLocale = resolvePublicRegistrationLanguage(toValue(queryLanguage))
  const selectedLanguage = ref<PublicRegistrationLanguage>(initialLocale)
  const scopedMessages = Object.fromEntries(
    PUBLIC_REGISTRATION_LANGUAGES.map((locale) => [
      locale,
      publicRegistrationMessages[locale][scope],
    ]),
  )
  const { t, locale } = useI18n({
    useScope: 'local',
    inheritLocale: false,
    locale: initialLocale,
    fallbackLocale: 'zh-CN',
    messages: scopedMessages,
  })

  const syncDocumentLanguage = (value: PublicRegistrationLanguage) => {
    if (typeof document === 'undefined') {
      return
    }

    document.documentElement.lang = value
    document.title = t('routeTitle')
  }

  const setLocale = (value: PublicRegistrationLanguage) => {
    selectedLanguage.value = value
  }

  watch(
    selectedLanguage,
    (value) => {
      locale.value = value
      writePublicRegistrationLanguage(value)
      syncDocumentLanguage(value)
    },
    { immediate: true, flush: 'sync' },
  )

  watch(
    () => toValue(queryLanguage),
    (value) => {
      const nextLocale = resolvePublicRegistrationLanguage(value)
      if (nextLocale !== selectedLanguage.value) {
        selectedLanguage.value = nextLocale
      }
    },
  )

  onBeforeUnmount(() => {
    if (typeof document === 'undefined') {
      return
    }

    const storedAppLocale = typeof window === 'undefined'
      ? null
      : window.localStorage.getItem(LOCALE_STORAGE_KEY)
    const appLocale = resolveLocale(storedAppLocale)
    document.documentElement.lang = appLocale
  })

  return {
    t,
    locale: computed(() => selectedLanguage.value),
    languageOptions: PUBLIC_REGISTRATION_LANGUAGE_OPTIONS,
    setLocale,
  }
}
