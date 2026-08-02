import { afterEach, describe, expect, test } from 'vitest'
import { i18n, loadLocaleMessages, type SupportedLocale } from '@/locales'

const locales: SupportedLocale[] = ['zh-CN', 'zh-TW', 'en', 'ja']

/**
 * 回归：审查详情的模板类消息含有 {{guest_name}} 等后端模板变量，
 * 直接走 t() 会因 vue-i18n 单花括号语法报 "Not allowed nest placeholder" 编译错误，
 * 导致整个审查详情页加载失败（生产构建仅显示错误码 9）。
 * 这些取值点必须经 tm() 获取未编译原文。
 */
describe('registration review i18n templates', () => {
  afterEach(() => {
    i18n.global.locale.value = 'zh-CN'
  })

  test('defaultReviewedInfo keeps raw template variables via tm() in every locale', async () => {
    for (const locale of locales) {
      await loadLocaleMessages(locale)
      const raw = i18n.global.tm('stage5.dataCenter.detail.defaultReviewedInfo', locale)
      expect(typeof raw).toBe('string')
      expect(raw as string).toContain('{{guest_name}}')
    }
  })

  test('other template-bearing detail messages stay readable via tm()', async () => {
    const templateKeys = [
      'defaultApprovedInfo',
      'defaultRejectRequest',
      'defaultReminder',
      'variablesPlaceholder',
    ]
    for (const locale of locales) {
      await loadLocaleMessages(locale)
      for (const key of templateKeys) {
        const raw = i18n.global.tm(`stage5.dataCenter.detail.${key}`, locale)
        expect(typeof raw).toBe('string')
        expect((raw as string).length).toBeGreaterThan(0)
      }
    }
  })

  test('finalize hint messages compile with date param in every locale', async () => {
    for (const locale of locales) {
      await loadLocaleMessages(locale)
      i18n.global.locale.value = locale
      expect(i18n.global.t('stage5.dataCenter.detail.reviewedScheduledHint', { date: '2026-10-01' })).toContain(
        '2026-10-01',
      )
      expect(
        i18n.global.t('stage5.dataCenter.detail.outsideFinalizeWindowHint', { date: '2026-10-01' }),
      ).toContain('2026-10-01')
      expect(() => i18n.global.t('stage5.dataCenter.detail.withinFinalizeWindowHint')).not.toThrow()
      expect(() => i18n.global.t('stage5.dataCenter.detail.reviewedScheduledNoDateHint')).not.toThrow()
      expect(() => i18n.global.t('stage5.common.status.reviewed')).not.toThrow()
    }
  })
})
