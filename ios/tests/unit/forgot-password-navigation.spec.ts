import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import ForgotPasswordPage from '@/views/auth/ForgotPasswordPage.vue'
import { createTestI18n } from './helpers/i18n'

const routerMocks = vi.hoisted(() => ({
  back: vi.fn(),
  replace: vi.fn(),
}))

const apiMocks = vi.hoisted(() => ({
  resetPassword: vi.fn(),
  sendVerificationCode: vi.fn(),
}))

const notifyMocks = vi.hoisted(() => ({
  showErrorToast: vi.fn(),
  showSuccessToast: vi.fn(),
  showWarningToast: vi.fn(),
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

  const IonInput = defineComponent({
    name: 'IonInput',
    inheritAttrs: false,
    props: {
      maxlength: [Number, String],
      modelValue: {
        type: [String, Number],
        default: '',
      },
    },
    emits: ['update:modelValue'],
    setup(props, { attrs, emit }) {
      return () =>
        h('input', {
          ...attrs,
          maxlength: props.maxlength,
          value: props.modelValue,
          onInput: (event: Event) => {
            emit('update:modelValue', (event.target as HTMLInputElement).value)
          },
        })
    },
  })

  return {
    IonButton: createStub('IonButton', 'button'),
    IonContent: createStub('IonContent'),
    IonInput,
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
  resetPassword: apiMocks.resetPassword,
  sendVerificationCode: apiMocks.sendVerificationCode,
}))

vi.mock('@/router/guards', () => ({
  ROUTE_PATHS: {
    login: '/auth/login',
  },
}))

vi.mock('@/utils/notify', () => ({
  showErrorToast: notifyMocks.showErrorToast,
  showSuccessToast: notifyMocks.showSuccessToast,
  showWarningToast: notifyMocks.showWarningToast,
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

const mountForgotPasswordPage = () =>
  mount(ForgotPasswordPage, {
    global: {
      plugins: [createTestI18n()],
    },
  })

describe('ForgotPasswordPage back navigation', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routerMocks.back.mockResolvedValue(undefined)
    routerMocks.replace.mockResolvedValue(undefined)
    apiMocks.resetPassword.mockResolvedValue({ success: true })
    apiMocks.sendVerificationCode.mockResolvedValue({ success: true })
  })

  test('uses router back so Ionic plays the backward transition', async () => {
    setHistoryLength(2)
    const wrapper = mountForgotPasswordPage()

    await wrapper.find('.auth-page-tabs__button--back').trigger('click')
    await flushPromises()

    expect(routerMocks.back).toHaveBeenCalledTimes(1)
    expect(routerMocks.replace).not.toHaveBeenCalled()
  })

  test('falls back to the login route when there is no navigation history', async () => {
    setHistoryLength(1)
    const wrapper = mountForgotPasswordPage()

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

  test('removes the local graphic captcha and sends reset code with email only', async () => {
    const wrapper = mountForgotPasswordPage()

    expect(wrapper.text()).not.toContain('图形验证码')
    expect(wrapper.find('input[placeholder="请输入图形验证码"]').exists()).toBe(false)

    const sendButton = wrapper.findAll('button').find((button) => button.text() === '发送验证码')
    expect(sendButton).toBeDefined()

    await sendButton!.trigger('click')
    await flushPromises()

    expect(apiMocks.sendVerificationCode).toHaveBeenCalledWith({
      email: 'owner@example.com',
      type: 'reset_password',
    })
  })

  test('submits a 6-20 character reset password without trimming it', async () => {
    const wrapper = mountForgotPasswordPage()

    await wrapper.find('input[placeholder="请输入验证码"]').setValue('123456')
    await wrapper.find('input[placeholder="请输入 6-20 位新密码"]').setValue(' 123456 ')
    await wrapper.find('input[placeholder="再次输入新密码"]').setValue(' 123456 ')

    const submitButton = wrapper.findAll('button').find((button) => button.text() === '确认')
    expect(submitButton).toBeDefined()

    await submitButton!.trigger('click')
    await flushPromises()

    expect(apiMocks.resetPassword).toHaveBeenCalledWith({
      email: 'owner@example.com',
      verificationCode: '123456',
      newPassword: ' 123456 ',
    })
  })
})
