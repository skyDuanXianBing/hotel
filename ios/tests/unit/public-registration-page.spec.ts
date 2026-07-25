import { describe, expect, it } from 'vitest'
import { PUBLIC_REGISTRATION_LANGUAGE_OPTIONS } from '@/composables/usePublicRegistrationI18n'

describe('public registration pages', () => {
  it('offers the supported four languages', () => {
    expect(PUBLIC_REGISTRATION_LANGUAGE_OPTIONS.map((item) => item.value)).toEqual([
      'zh-CN',
      'zh-TW',
      'en',
      'ja',
    ])
  })
})
