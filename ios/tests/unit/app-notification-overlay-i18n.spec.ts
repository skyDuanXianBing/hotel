import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h, nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppNotificationOverlay from '@/components/global/AppNotificationOverlay.vue'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { createTestI18n } from './helpers/i18n'

const routerPush = vi.fn()

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    useRouter: () => ({
      push: routerPush,
    }),
  }
})

vi.mock('@ionic/vue', () => ({
  IonIcon: defineComponent({
    name: 'IonIcon',
    setup() {
      return () => h('span')
    },
  }),
}))

describe('AppNotificationOverlay i18n', () => {
  beforeEach(() => {
    window.localStorage.clear()
    routerPush.mockReset()
  })

  it('updates notification labels when the locale changes without remounting', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const testI18n = createTestI18n('zh-CN')
    const wrapper = mount(AppNotificationOverlay, {
      global: {
        plugins: [pinia, testI18n],
      },
    })

    useNotificationCenterStore().enqueue({
      id: 'order:1',
      title: 'Test notification',
      content: 'Test content',
      detail: '',
      targetPath: '/tabs/orders',
      type: 'order',
    })
    await nextTick()

    expect(wrapper.text()).toContain('订单')
    expect(wrapper.get('.notif-banner__close').attributes('aria-label')).toBe('关闭通知')

    testI18n.global.locale.value = 'ja'
    await nextTick()

    expect(wrapper.text()).toContain('予約')
    expect(wrapper.get('.notif-banner__close').attributes('aria-label')).toBe('通知を閉じる')
  })
})
