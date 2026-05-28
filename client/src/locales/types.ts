export const SUPPORTED_LOCALES = ['en', 'zh-CN', 'zh-TW', 'ja'] as const

export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]
