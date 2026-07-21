import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { i18n, LOCALE_STORAGE_KEY } from '@/locales'
import { useLanguageStore } from '@/stores/language'

describe('language store', () => {
  beforeEach(() => {
    window.localStorage.clear()
    document.documentElement.lang = ''
    document.body.innerHTML = ''
    i18n.global.locale.value = 'zh-CN'
  })

  afterEach(() => {
    i18n.global.locale.value = 'zh-CN'
  })

  it('restores the saved locale and synchronizes runtime state', async () => {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, 'zh-TW')
    setActivePinia(createPinia())

    const languageStore = useLanguageStore()
    await languageStore.initialize()

    expect(languageStore.locale).toBe('zh-TW')
    expect(i18n.global.locale.value).toBe('zh-TW')
    expect(document.documentElement.lang).toBe('zh-TW')
  })

  it('updates i18n, document language, persistence, and rendered back buttons immediately', async () => {
    window.localStorage.setItem('token', 'existing-token')
    window.localStorage.setItem('currentStore', JSON.stringify({ id: 12 }))

    const backButton = document.createElement('ion-back-button') as HTMLElement & {
      text?: string
    }
    document.body.appendChild(backButton)

    setActivePinia(createPinia())
    const languageStore = useLanguageStore()
    await languageStore.setLocale('ja')

    expect(languageStore.locale).toBe('ja')
    expect(i18n.global.locale.value).toBe('ja')
    expect(document.documentElement.lang).toBe('ja')
    expect(window.localStorage.getItem(LOCALE_STORAGE_KEY)).toBe('ja')
    expect(backButton.text).toBe('戻る')
    expect(window.localStorage.getItem('token')).toBe('existing-token')
    expect(window.localStorage.getItem('currentStore')).toBe(JSON.stringify({ id: 12 }))
  })
})
