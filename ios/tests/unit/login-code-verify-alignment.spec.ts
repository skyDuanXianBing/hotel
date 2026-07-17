import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import LoginCodeVerifyPage from '@/views/auth/LoginCodeVerifyPage.vue'

const apiMocks = vi.hoisted(() => ({
  loginByCode: vi.fn(),
  sendVerificationCode: vi.fn(),
}))

const routerMocks = vi.hoisted(() => ({
  back: vi.fn(),
  replace: vi.fn(),
}))

const routeState = vi.hoisted(() => ({
  query: {
    email: 'owner@example.com',
  } as Record<string, string>,
}))

const sessionMocks = vi.hoisted(() => ({
  applyUnifiedLoginResponse: vi.fn(() => ({
    target: 'PMS',
    redirectPath: '/store/selection',
  })),
}))

vi.mock('@ionic/vue', async () => {
  const { defineComponent, h } = await import('vue')

  const createContainerStub = (name: string, tag = 'div') =>
    defineComponent({
      name,
      inheritAttrs: false,
      setup(_, { attrs, slots }) {
        return () => h(tag, attrs, slots.default ? slots.default() : [])
      },
    })

  return {
    IonButton: createContainerStub('IonButton', 'button'),
    IonContent: createContainerStub('IonContent'),
    IonIcon: createContainerStub('IonIcon'),
    IonPage: createContainerStub('IonPage'),
    IonSpinner: createContainerStub('IonSpinner'),
    onIonViewDidEnter: (callback: () => void) => callback(),
  }
})

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => routerMocks,
}))

vi.mock('@/api/auth', () => ({
  loginByCode: apiMocks.loginByCode,
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
  showErrorToast: vi.fn(),
  showSuccessToast: vi.fn(),
  showWarningToast: vi.fn(),
}))

vi.mock('@/utils/request', () => ({
  isHandledRequestError: vi.fn(() => false),
}))

const store = {
  id: 2,
  name: '测试门店',
  phone: '+86 10000000000',
  type: 'hotel',
  timezone: 'Asia/Shanghai',
  manager: '店长',
  ownerEmail: 'owner@example.com',
  address: '测试地址',
  city: '上海',
  state: '上海',
  country: 'China',
  currency: 'CNY',
  userRole: 'member',
  createdAt: '2026-07-17T00:00:00Z',
  updatedAt: '2026-07-17T00:00:00Z',
}

const cleaner = {
  id: 20,
  userId: 10,
  storeId: store.id,
  name: '保洁员',
  email: 'cleaner@example.com',
  isActive: true,
  createdAt: '2026-07-17T00:00:00Z',
  updatedAt: '2026-07-17T00:00:00Z',
}

const buildLoginResponse = (availableLoginTargets: Array<'PMS' | 'CLEANER'> = ['PMS']) => ({
  success: true,
  data: {
    token: 'token',
    user: {
      id: 10,
      email: 'owner@example.com',
      nickname: '业主',
      createdAt: '2026-07-17T00:00:00Z',
      updatedAt: '2026-07-17T00:00:00Z',
    },
    stores: [store],
    loginTarget: 'PMS' as const,
    availableLoginTargets,
    cleanerContexts: [
      {
        cleanerId: cleaner.id,
        storeId: store.id,
        cleaner,
        store,
      },
    ],
  },
})

const enterVerificationCode = async (wrapper: ReturnType<typeof mount>) => {
  const inputs = wrapper.findAll<HTMLInputElement>('.verify-code-input')

  for (const [index, digit] of ['1', '2', '3', '4', '5', '6'].entries()) {
    await inputs[index].setValue(digit)
  }

  await flushPromises()
}

describe('LoginCodeVerifyPage Web alignment', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeState.query = {
      email: 'owner@example.com',
    }
    apiMocks.loginByCode.mockResolvedValue(buildLoginResponse())
    apiMocks.sendVerificationCode.mockResolvedValue({ success: true })
  })

  test('does not claim a hard-coded verification-code expiry and remembers false by default', async () => {
    const wrapper = mount(LoginCodeVerifyPage)

    expect(wrapper.text()).not.toContain('有效期10分钟')
    await enterVerificationCode(wrapper)

    expect(apiMocks.loginByCode).toHaveBeenCalledWith({
      email: 'owner@example.com',
      verificationCode: '123456',
      rememberMe: false,
      preferredLoginTarget: undefined,
    })
  })

  test('selects a workspace from the authorized code-login response without sending the code again', async () => {
    apiMocks.loginByCode.mockResolvedValue(buildLoginResponse(['PMS', 'CLEANER']))
    const wrapper = mount(LoginCodeVerifyPage)

    await enterVerificationCode(wrapper)

    expect(wrapper.find('.workspace-modal-stub').exists()).toBe(true)
    await wrapper.find('.workspace-CLEANER').trigger('click')
    await flushPromises()

    expect(apiMocks.loginByCode).toHaveBeenCalledTimes(1)
    expect(sessionMocks.applyUnifiedLoginResponse).toHaveBeenCalledWith(
      expect.objectContaining({
        loginTarget: 'CLEANER',
        cleaner: expect.objectContaining({
          id: cleaner.id,
        }),
        currentStore: expect.objectContaining({
          id: store.id,
        }),
        targetStoreId: store.id,
      }),
      {
        resetPmsCurrentStore: true,
      },
    )
  })
})
