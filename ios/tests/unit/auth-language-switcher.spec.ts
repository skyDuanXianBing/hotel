import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import AuthLanguageSwitcher from '@/components/auth/AuthLanguageSwitcher.vue'
import { useLanguageStore } from '@/stores/language'
import { createTestI18n } from './helpers/i18n'

const actionSheetMocks = vi.hoisted(() => ({
  create: vi.fn(),
  present: vi.fn(),
}))

vi.mock('@ionic/vue', async () => {
  const { defineComponent, h } = await import('vue')

  return {
    actionSheetController: {
      create: actionSheetMocks.create,
    },
    IonIcon: defineComponent({
      name: 'IonIcon',
      setup() {
        return () => h('span')
      },
    }),
  }
})

describe('AuthLanguageSwitcher', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    window.localStorage.setItem('app_language', 'zh-CN')
    actionSheetMocks.create.mockResolvedValue({
      present: actionSheetMocks.present,
    })
  })

  test('shows the current locale and changes language from the action sheet', async () => {
    const pinia = createPinia()
    const wrapper = mount(AuthLanguageSwitcher, {
      global: {
        plugins: [pinia, createTestI18n()],
      },
    })
    const languageStore = useLanguageStore(pinia)
    const setLocaleSpy = vi.spyOn(languageStore, 'setLocale').mockResolvedValue(true)

    expect(wrapper.get('button').text()).toContain('简中')

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(actionSheetMocks.create).toHaveBeenCalledTimes(1)
    expect(actionSheetMocks.present).toHaveBeenCalledTimes(1)

    const options = actionSheetMocks.create.mock.calls[0][0]
    const languageButtons = options.buttons.slice(0, 4)
    const japaneseButton = languageButtons.find(
      (button: { text: string }) => button.text === '日本語',
    )

    expect(languageButtons).toHaveLength(4)
    expect(languageButtons.find((button: { role?: string }) => button.role === 'selected')?.text).toBe(
      '简体中文',
    )
    expect(japaneseButton).toBeDefined()

    await japaneseButton.handler()

    expect(setLocaleSpy).toHaveBeenCalledWith('ja')
  })
})
