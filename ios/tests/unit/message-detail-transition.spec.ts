import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import MessageDetailPage from '@/views/messages/MessageDetailPage.vue'

const apiMocks = vi.hoisted(() => ({
  getMessageThreads: vi.fn(),
  getThreadMessages: vi.fn(),
  pollThreadMessages: vi.fn(),
}))

const ionicLifecycle = vi.hoisted(() => ({
  willEnter: null as null | (() => unknown),
  willLeave: null as null | (() => unknown),
}))

const notificationCenterMocks = vi.hoisted(() => ({
  syncMessageThreads: vi.fn(),
}))

const routerMocks = vi.hoisted(() => ({
  push: vi.fn(),
  route: null as unknown as {
    params: Record<string, string | undefined>
    query: Record<string, string | undefined>
    fullPath: string
  },
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
    onIonViewWillEnter: (callback: () => unknown) => {
      ionicLifecycle.willEnter = callback
    },
    onIonViewWillLeave: (callback: () => unknown) => {
      ionicLifecycle.willLeave = callback
    },
    IonBackButton: createStub('IonBackButton', 'button'),
    IonButton: createStub('IonButton', 'button'),
    IonButtons: createStub('IonButtons'),
    IonContent: createStub('IonContent'),
    IonHeader: createStub('IonHeader'),
    IonIcon: createStub('IonIcon'),
    IonModal: createStub('IonModal'),
    IonPage: createStub('IonPage'),
    IonRefresher: createStub('IonRefresher'),
    IonRefresherContent: createStub('IonRefresherContent'),
    IonSpinner: createStub('IonSpinner'),
    IonTextarea: createStub('IonTextarea', 'textarea'),
    IonTitle: createStub('IonTitle'),
    IonToolbar: createStub('IonToolbar'),
  }
})

vi.mock('vue-router', async () => {
  const { reactive } = await import('vue')
  routerMocks.route = reactive({
    params: {
      threadId: '15',
    },
    query: {},
    fullPath: '/tabs/messages/15',
  })

  return {
    useRoute: () => routerMocks.route,
    useRouter: () => ({
      push: routerMocks.push,
    }),
  }
})

vi.mock('@/api/message', () => ({
  MESSAGE_API_MOCK_ENABLED: true,
  getMessageThreads: apiMocks.getMessageThreads,
  getThreadMessages: apiMocks.getThreadMessages,
  pollThreadMessages: apiMocks.pollThreadMessages,
  sendAiChatMessage: vi.fn(),
  sendThreadMessage: vi.fn(),
  translateThreadMessage: vi.fn(),
}))

vi.mock('@/api/reservation', () => ({
  getReservationsWithFilters: vi.fn(),
}))

vi.mock('@/stores/notificationCenter', () => ({
  useNotificationCenterStore: () => notificationCenterMocks,
}))

vi.mock('@/utils/notify', () => ({
  showSuccessToast: vi.fn(),
  showWarningToast: vi.fn(),
}))

vi.mock('@/utils/request', () => ({
  isHandledRequestError: vi.fn(() => false),
}))

describe('MessageDetailPage transition state', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    routerMocks.route.params.threadId = '15'
    routerMocks.route.query = {}
    routerMocks.route.fullPath = '/tabs/messages/15'

    apiMocks.getMessageThreads.mockResolvedValue({
      success: true,
      message: 'ok',
      data: [
        {
          id: 15,
          channelId: 1,
          channelName: 'Booking.com',
          guestName: 'Alice',
          lastActivity: '2026-07-18T10:00:00Z',
          unreadCount: 0,
          closed: false,
        },
      ],
    })
    apiMocks.getThreadMessages.mockResolvedValue({
      success: true,
      message: 'ok',
      data: [
        {
          id: 1501,
          threadId: 15,
          senderType: 'GUEST',
          senderName: 'Bob',
          content: 'Hello',
          timestamp: '2026-07-18T10:00:00Z',
          deliveryStatus: 'SENT',
        },
      ],
    })
    apiMocks.pollThreadMessages.mockResolvedValue({
      success: true,
      message: 'ok',
      data: [],
    })
  })

  test('keeps the active thread avatar stable while the route changes during leave', async () => {
    const wrapper = mount(MessageDetailPage)

    await ionicLifecycle.willEnter?.()
    await flushPromises()

    const avatar = wrapper.find('.message-row.is-guest .message-row__avatar')
    const initialStyle = avatar.attributes('style')

    expect(avatar.text()).toBe('A')
    expect(initialStyle).toContain('--thread-avatar-start: #9cadff')

    ionicLifecycle.willLeave?.()
    routerMocks.route.params.threadId = undefined
    routerMocks.route.fullPath = '/tabs/messages'
    await flushPromises()

    expect(avatar.text()).toBe('A')
    expect(avatar.attributes('style')).toBe(initialStyle)

    wrapper.unmount()
  })
})
