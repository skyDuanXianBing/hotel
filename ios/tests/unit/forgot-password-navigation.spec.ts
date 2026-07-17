import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import ForgotPasswordPage from '@/views/auth/ForgotPasswordPage.vue'

const routerMocks = vi.hoisted(() => ({
  back: vi.fn(),
  replace: vi.fn(),
}))

vi.mock('@ionic/vue', async () => {
  const { defineComponent, h } = await import('vue')

  const createStub = (name: string, tagName = 'div') =>
    defineComponent({
      name,
      inheritAttrs: false,
      setup(_, { attrs, slots }) {
        return () => h(tagName, attrs, slots.default ? slots.default() : [])
      },
    })

  return {
    IonButton: createStub('IonButton', 'button'),
    IonContent: createStub('IonContent'),
    IonInput: createStub('IonInput', 'input'),
    IonItem: createStub('IonItem'),
    IonList: createStub('IonList'),
    IonPage: createStub('IonPage'),
    IonSpinner: createStub('IonSpinner'),
  }
})

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {
      email: 'owner@example.com',
    },
  }),
  useRouter: () => routerMocks,
}))

vi.mock('@/api/auth', () => ({
  resetPassword: vi.fn(),
  sendVerificationCode: vi.fn(),
}))

vi.mock('@/router/guards', () => ({
  ROUTE_PATHS: {
    login: '/auth/login',
  },
}))

vi.mock('@/utils/notify', () => ({
  showErrorToast: vi.fn(),
  showSuccessToast: vi.fn(),
  showWarningToast: vi.fn(),
}))

vi.mock('@/utils/request', () => ({
  isHandledRequestError: vi.fn(() => false),
}))

const setHistoryLength = (length: number) => {
  Object.defineProperty(window.history, 'length', {
    configurable: true,
    value: length,
  })
}

describe('ForgotPasswordPage back navigation', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routerMocks.back.mockResolvedValue(undefined)
    routerMocks.replace.mockResolvedValue(undefined)
  })

  test('uses router back so Ionic plays the backward transition', async () => {
    setHistoryLength(2)
    const wrapper = mount(ForgotPasswordPage)

    await wrapper.find('.auth-page-tabs__button--back').trigger('click')
    await flushPromises()

    expect(routerMocks.back).toHaveBeenCalledTimes(1)
    expect(routerMocks.replace).not.toHaveBeenCalled()
  })

  test('falls back to the login route when there is no navigation history', async () => {
    setHistoryLength(1)
    const wrapper = mount(ForgotPasswordPage)

    await wrapper.find('.auth-page-tabs__button--back').trigger('click')
    await flushPromises()

    expect(routerMocks.back).not.toHaveBeenCalled()
    expect(routerMocks.replace).toHaveBeenCalledWith({
      path: '/auth/login',
      query: {
        email: 'owner@example.com',
      },
    })
  })
})
