import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import LoginPage from '@/views/auth/LoginPage.vue'

const apiMocks = vi.hoisted(() => ({
  loginByPassword: vi.fn(),
  sendVerificationCode: vi.fn(),
}))

const routerMocks = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
}))

const routeState = vi.hoisted(() => ({
  query: {} as Record<string, string>,
}))

const sessionMocks = vi.hoisted(() => ({
  applyUnifiedLoginResponse: vi.fn(() => ({
    target: 'PMS',
    redirectPath: '/store/selection',
  })),
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
          value: props.modelValue,
          onInput: (event: Event) => {
            emit('update:modelValue', (event.target as HTMLInputElement).value)
          },
        })
    },
  })

  return {
    IonButton,
    IonContent: createContainerStub('IonContent'),
    IonInput,
    IonItem: createContainerStub('IonItem'),
    IonLabel: createContainerStub('IonLabel'),
    IonList: createContainerStub('IonList'),
    IonPage: createContainerStub('IonPage'),
    IonSegment: createContainerStub('IonSegment'),
    IonSegmentButton: createContainerStub('IonSegmentButton'),
    IonSpinner: createContainerStub('IonSpinner'),
  }
})

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => routerMocks,
}))

vi.mock('@/api/auth', () => ({
  loginByPassword: apiMocks.loginByPassword,
  sendVerificationCode: apiMocks.sendVerificationCode,
}))

vi.mock('@/components/auth/AuthBrandHeader.vue', async () => {
  const { defineComponent, h } = await import('vue')
  return {
    default: defineComponent({
      name: 'AuthBrandHeader',
      setup() {
        return () => h('header')
      },
    }),
  }
})

vi.mock('@/views/auth/RegisterPage.vue', async () => {
  const { defineComponent, h } = await import('vue')
  return {
    default: defineComponent({
      name: 'RegisterPage',
      setup() {
        return () => h('div')
      },
    }),
  }
})

vi.mock('@/views/auth/WorkspaceSelectionModal.vue', async () => {
  const { defineComponent, h } = await import('vue')
  return {
    default: defineComponent({
      name: 'WorkspaceSelectionModal',
      props: {
        open: Boolean,
        targets: {
          type: Array,
          default: () => [],
        },
      },
      emits: ['select', 'dismiss'],
      setup(props, { emit }) {
        return () =>
          props.open
            ? h(
                'div',
                { class: 'workspace-modal-stub' },
                (props.targets as string[]).map((target) =>
                  h(
                    'button',
                    {
                      class: `workspace-${target}`,
                      onClick: () => emit('select', target),
                    },
                    target,
                  ),
                ),
              )
            : null
      },
    }),
  }
})

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ hydrate: vi.fn() }),
}))

vi.mock('@/stores/store', () => ({
  useStoreStore: () => ({ hydrate: vi.fn() }),
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({ hydrate: vi.fn() }),
}))

vi.mock('@/utils/autoLogin', () => ({
  clearAutoLoginCredentials: vi.fn(),
  saveAutoLoginCredentials: vi.fn(),
}))

vi.mock('@/utils/loginSessionResolver', async () => {
  const actual = await vi.importActual<typeof import('@/utils/loginSessionResolver')>(
    '@/utils/loginSessionResolver',
  )
  return {
    ...actual,
    applyUnifiedLoginResponse: sessionMocks.applyUnifiedLoginResponse,
  }
})

vi.mock('@/utils/notify', () => ({
  showErrorToast: notifyMocks.showErrorToast,
  showSuccessToast: notifyMocks.showSuccessToast,
  showWarningToast: notifyMocks.showWarningToast,
}))

vi.mock('@/utils/request', () => ({
  getRequestErrorStatus: vi.fn(() => null),
  isHandledRequestError: vi.fn(() => false),
}))

const buildPmsLoginResponse = (availableLoginTargets: Array<'PMS' | 'CLEANER'> = ['PMS']) => ({
  success: true,
  data: {
    token: 'token',
    user: {
      id: 1,
      email: 'owner@example.com',
      nickname: '业主',
      createdAt: '2026-07-17T00:00:00Z',
      updatedAt: '2026-07-17T00:00:00Z',
    },
    stores: [],
    loginTarget: 'PMS' as const,
    availableLoginTargets,
  },
})

const findButtonByText = (wrapper: ReturnType<typeof mount>, text: string) => {
  const button = wrapper.findAll('button').find((item) => item.text() === text)

  if (!button) {
    throw new Error(`Button not found: ${text}`)
  }

  return button
}

describe('LoginPage Web alignment', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeState.query = {}
    apiMocks.loginByPassword.mockResolvedValue(buildPmsLoginResponse())
    apiMocks.sendVerificationCode.mockResolvedValue({ success: true })
  })

  test('shows no graphic captcha and leaves remember/agreement unchecked by default', () => {
    const wrapper = mount(LoginPage)
    const checkboxes = wrapper.findAll<HTMLInputElement>('input[type="checkbox"]')

    expect(wrapper.text()).not.toContain('图形验证码')
    expect(checkboxes).toHaveLength(2)
    expect(checkboxes[0].element.checked).toBe(false)
    expect(checkboxes[1].element.checked).toBe(false)
  })

  test('submits the original password without trimming it', async () => {
    const wrapper = mount(LoginPage)

    await wrapper.find('input[placeholder="请输入邮箱"]').setValue('owner@example.com')
    await wrapper.find('input[placeholder="请输入密码"]').setValue(' 123456 ')
    await wrapper.findAll('input[type="checkbox"]')[1].setValue(true)
    await findButtonByText(wrapper, '登录').trigger('click')
    await flushPromises()

    expect(apiMocks.loginByPassword).toHaveBeenCalledWith({
      email: 'owner@example.com',
      password: ' 123456 ',
      rememberMe: false,
    })
  })

  test('asks for a workspace and re-authenticates password login with the selected target', async () => {
    apiMocks.loginByPassword
      .mockResolvedValueOnce(buildPmsLoginResponse(['PMS', 'CLEANER']))
      .mockResolvedValueOnce(buildPmsLoginResponse(['PMS', 'CLEANER']))
    const wrapper = mount(LoginPage)

    await wrapper.find('input[placeholder="请输入邮箱"]').setValue('owner@example.com')
    await wrapper.find('input[placeholder="请输入密码"]').setValue('123456')
    await wrapper.findAll('input[type="checkbox"]')[1].setValue(true)
    await findButtonByText(wrapper, '登录').trigger('click')
    await flushPromises()

    expect(wrapper.find('.workspace-modal-stub').exists()).toBe(true)
    await wrapper.find('.workspace-CLEANER').trigger('click')
    await flushPromises()

    expect(apiMocks.loginByPassword).toHaveBeenNthCalledWith(2, {
      email: 'owner@example.com',
      password: '123456',
      rememberMe: false,
      preferredLoginTarget: 'CLEANER',
    })
  })

  test('sends code without a captcha and preserves the false remember choice', async () => {
    const wrapper = mount(LoginPage)

    await wrapper.find('input[placeholder="请输入邮箱"]').setValue('owner@example.com')
    await wrapper.findAll('input[type="checkbox"]')[1].setValue(true)
    wrapper.findComponent({ name: 'IonSegment' }).vm.$emit('ionChange', {
      detail: {
        value: 'code',
      },
    })
    await flushPromises()
    await findButtonByText(wrapper, '发送验证码').trigger('click')
    await flushPromises()

    expect(apiMocks.sendVerificationCode).toHaveBeenCalledWith({
      email: 'owner@example.com',
      type: 'login',
    })
    expect(routerMocks.push).toHaveBeenCalledWith({
      path: '/auth/login/code-verify',
      query: {
        email: 'owner@example.com',
        rememberMe: '0',
      },
    })
  })
})
