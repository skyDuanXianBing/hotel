import { SUPPORTED_LOCALES, loadLocaleMessages } from '@/locales'

await Promise.all(SUPPORTED_LOCALES.map((locale) => loadLocaleMessages(locale)))
