import { createI18n } from 'vue-i18n'
import en from '@/locales/messages/generated/en.json'
import ja from '@/locales/messages/generated/ja.json'
import zhCN from '@/locales/messages/generated/zh-CN.json'
import zhTW from '@/locales/messages/generated/zh-TW.json'
import type { SupportedLocale } from '@/locales'

export const createTestI18n = (locale: SupportedLocale = 'zh-CN') =>
  createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'zh-CN',
    messages: {
      en,
      ja,
      'zh-CN': zhCN,
      'zh-TW': zhTW,
    },
  })
