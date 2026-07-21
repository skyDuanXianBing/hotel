import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import RegisterPage from '@/views/auth/RegisterPage.vue'
import { createTestI18n } from './helpers/i18n'

const apiMocks = vi.hoisted(() => ({
  register: vi.fn(),
  sendVerificationCode: vi.fn(),
}))

const notifyMocks = vi.hoisted(() => ({
  showErrorToast: vi.fn(),
  showSuccessToast: vi.fn(),
  showWarningToast: vi.fn(),
}))

vi.mock('@ionic/vue', async () => {
  const { defineComponent, h } = await import('vue')

  const createContainerStub = (name: string) =>
    defineComponent({
      name,
      inheritAttrs: false,
      setup(_, { attrs, slots }) {
        return () => h('div', attrs, slots.default ? slots.default() : [])
      },
    })

  const IonButton = defineComponent({
    name: 'IonButton',
    inheritAttrs: false,
    props: {
      disabled: Boolean,
    },
    emits: ['click'],
    setup(props, { attrs, emit, slots }) {
      return () =>
        h(
          'button',
          {
            ...attrs,
            disabled: props.disabled,
            onClick: (event: MouseEvent) => emit('click', event),
          },
          slots.default ? slots.default() : [],
        )
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
    emits: ['ionBlur', 'ionFocus', 'update:modelValue'],
    setup(props, { attrs, emit }) {
      return () =>
        h('input', {
          ...attrs,
          maxlength: props.maxlength,
          value: props.modelValue,
          onBlur: () => emit('ionBlur'),
          onFocus: () => emit('ionFocus'),
          onInput: (event: Event) => {
            emit('update:modelValue', (event.target as HTMLInputElement).value)
          },
        })
    },
  })

  return {
    IonButton,
    IonInput,
    IonItem: createContainerStub('IonItem'),
    IonList: createContainerStub('IonList'),
    IonSpinner: createContainerStub('IonSpinner'),
  }
})

vi.mock('@/api/auth', () => ({
  register: apiMocks.register,
  sendVerificationCode: apiMocks.sendVerificationCode,
}))

vi.mock('@/utils/notify', () => ({
  showErrorToast: notifyMocks.showErrorToast,
  showSuccessToast: notifyMocks.showSuccessToast,
  showWarningToast: notifyMocks.showWarningToast,
}))

vi.mock('@/utils/request', () => ({
  isHandledRequestError: vi.fn(() => false),
}))

const findButtonByText = (wrapper: ReturnType<typeof mount>, text: string) => {
  const button = wrapper.findAll('button').find((item) => item.text() === text)

  if (!button) {
    throw new Error(`Button not found: ${text}`)
  }

  return button
}

const mountRegisterPage = () =>
  mount(RegisterPage, {
    props: {
      initialEmail: 'owner@example.com',
    },
    global: {
      plugins: [createTestI18n()],
    },
  })

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiMocks.register.mockResolvedValue({ success: true, data: {} })
    apiMocks.sendVerificationCode.mockResolvedValue({ success: true })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  test('shows the complete registration form on one page', () => {
    const wrapper = mountRegisterPage()

    expect(wrapper.text()).toContain('邮箱')
    expect(wrapper.text()).toContain('邮箱验证码')
    expect(wrapper.text()).toContain('密码')
    expect(wrapper.text()).toContain('确认密码')
    expect(wrapper.text()).toContain('我已阅读并同意用户协议和隐私协议')
    expect(wrapper.text()).not.toContain('品牌名或姓名')
    expect(wrapper.text()).not.toContain('继续')
    expect(wrapper.find<HTMLInputElement>('input[placeholder="请输入邮箱"]').element.value).toBe(
      'owner@example.com',
    )
  })

  test('sends a registration verification code without changing form steps', async () => {
    const wrapper = mountRegisterPage()

    await findButtonByText(wrapper, '发送验证码').trigger('click')
    await flushPromises()

    expect(apiMocks.sendVerificationCode).toHaveBeenCalledWith({
      email: 'owner@example.com',
      type: 'register',
    })
    expect(notifyMocks.showSuccessToast).toHaveBeenCalledWith('验证码已发送，请查收邮箱')
  })

  test('prevents resending a registration code during the 60 second countdown', async () => {
    vi.useFakeTimers()
    const wrapper = mountRegisterPage()
    const sendButton = findButtonByText(wrapper, '发送验证码')

    await sendButton.trigger('click')
    await flushPromises()

    expect(sendButton.text()).toBe('60s 后可重发')
    expect(sendButton.attributes('disabled')).toBeDefined()

    await vi.advanceTimersByTimeAsync(1000)
    expect(sendButton.text()).toBe('59s 后可重发')

    await vi.advanceTimersByTimeAsync(59_000)
    expect(sendButton.text()).toBe('发送验证码')
    expect(sendButton.attributes('disabled')).toBeUndefined()

    wrapper.unmount()
  })

  test('keeps the register button interactive so incomplete fields show validation feedback', async () => {
    const wrapper = mountRegisterPage()
    const registerButton = findButtonByText(wrapper, '注册')

    expect(registerButton.attributes('disabled')).toBeUndefined()

    await registerButton.trigger('click')

    expect(notifyMocks.showWarningToast).toHaveBeenCalledWith('请输入 6 位邮箱验证码')
    expect(apiMocks.register).not.toHaveBeenCalled()
  })

  test('submits the Web-aligned payload and preserves the original password', async () => {
    const wrapper = mountRegisterPage()

    await wrapper.find('input[placeholder="请输入邮箱验证码"]').setValue('123456')
    await wrapper.find('input[placeholder="请输入 6-20 位密码"]').setValue(' 123456 ')
    await wrapper.find('input[placeholder="请再次输入密码"]').setValue(' 123456 ')
    await wrapper.find('input[type="checkbox"]').setValue(true)
    await findButtonByText(wrapper, '注册').trigger('click')
    await flushPromises()

    expect(apiMocks.register).toHaveBeenCalledWith({
      email: 'owner@example.com',
      verificationCode: '123456',
      password: ' 123456 ',
    })
    expect(wrapper.emitted('registered')).toEqual([['owner@example.com']])
  })

  test('accepts a 6 character password and rejects passwords longer than 20 characters', async () => {
    const wrapper = mountRegisterPage()

    await wrapper.find('input[placeholder="请输入邮箱验证码"]').setValue('123456')
    await wrapper.find('input[type="checkbox"]').setValue(true)

    await wrapper.find('input[placeholder="请输入 6-20 位密码"]').setValue('123456')
    await wrapper.find('input[placeholder="请再次输入密码"]').setValue('123456')
    await findButtonByText(wrapper, '注册').trigger('click')
    await flushPromises()

    expect(apiMocks.register).toHaveBeenCalledWith({
      email: 'owner@example.com',
      verificationCode: '123456',
      password: '123456',
    })

    apiMocks.register.mockClear()
    await wrapper.find('input[placeholder="请输入邮箱验证码"]').setValue('123456')
    await wrapper.find('input[placeholder="请输入 6-20 位密码"]').setValue('123456789012345678901')
    await wrapper
      .find('input[placeholder="请再次输入密码"]')
      .setValue('123456789012345678901')
    await wrapper.find('input[type="checkbox"]').setValue(true)
    await findButtonByText(wrapper, '注册').trigger('click')

    expect(notifyMocks.showWarningToast).toHaveBeenCalledWith('密码需为 6-20 位')
    expect(apiMocks.register).not.toHaveBeenCalled()
  })
})
