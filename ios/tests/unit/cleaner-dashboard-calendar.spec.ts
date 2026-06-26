import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import type { CleaningTaskDTO } from '@/api/cleaning'
import CleanerDashboardPage from '@/views/cleaner/CleanerDashboardPage.vue'

const apiMocks = vi.hoisted(() => ({
  getCalendarViewData: vi.fn(),
}))

const cleanerSessionMocks = vi.hoisted(() => ({
  readCleanerUser: vi.fn(),
}))

const dateMocks = vi.hoisted(() => ({
  buildBusinessMonthCalendarDates: vi.fn(),
  buildBusinessMonthRange: vi.fn(),
  getStoreCurrentMonthValue: vi.fn(),
  getStoreTodayDate: vi.fn(),
  parseBusinessMonthParts: vi.fn(),
  shiftBusinessMonth: vi.fn(),
}))

const routerMocks = vi.hoisted(() => ({
  push: vi.fn(),
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
    alertController: {
      create: vi.fn(),
    },
    onIonViewWillEnter: (callback: () => unknown) => {
      void callback()
    },
    IonButton: createStub('IonButton', 'button'),
    IonButtons: createStub('IonButtons'),
    IonContent: createStub('IonContent'),
    IonHeader: createStub('IonHeader'),
    IonModal: createStub('IonModal'),
    IonPage: createStub('IonPage'),
    IonRefresher: createStub('IonRefresher'),
    IonRefresherContent: createStub('IonRefresherContent'),
    IonTitle: createStub('IonTitle'),
    IonToolbar: createStub('IonToolbar'),
  }
})

vi.mock('@/api/cleaning', () => ({
  getCalendarViewData: apiMocks.getCalendarViewData,
}))

vi.mock('@/router/guards', () => ({
  buildCleanerTaskDetailPath: (taskId: number) => `/cleaner/tasks/${taskId}`,
  ROUTE_PATHS: {
    login: '/login',
  },
}))

vi.mock('@/utils/accommodation', () => ({
  formatDate: (date: string) => date,
}))

vi.mock('@/utils/cleanerSession', () => ({
  readCleanerUser: cleanerSessionMocks.readCleanerUser,
}))

vi.mock('@/utils/loginSessionResolver', () => ({
  clearAllLoginSessions: vi.fn(),
}))

vi.mock('@/utils/notify', () => ({
  showWarningToast: vi.fn(),
}))

vi.mock('@/utils/request', () => ({
  isHandledRequestError: vi.fn(() => false),
}))

vi.mock('@/utils/storeBusinessDate', () => ({
  buildBusinessMonthCalendarDates: dateMocks.buildBusinessMonthCalendarDates,
  buildBusinessMonthRange: dateMocks.buildBusinessMonthRange,
  getStoreCurrentMonthValue: dateMocks.getStoreCurrentMonthValue,
  getStoreTodayDate: dateMocks.getStoreTodayDate,
  parseBusinessMonthParts: dateMocks.parseBusinessMonthParts,
  shiftBusinessMonth: dateMocks.shiftBusinessMonth,
}))

vi.mock('vue-router', () => ({
  useRouter: () => routerMocks,
}))

function buildTask(overrides: Partial<CleaningTaskDTO> = {}): CleaningTaskDTO {
  return {
    id: 1,
    taskDate: '2026-06-16',
    roomId: 33,
    roomNumber: '101',
    roomType: '大床房',
    roomTypeId: 3,
    taskType: 'checkout',
    status: 'assigned',
    createdAt: '2026-06-16T00:00:00Z',
    updatedAt: '2026-06-16T00:00:00Z',
    ...overrides,
  }
}

describe('CleanerDashboardPage calendar tasks', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    cleanerSessionMocks.readCleanerUser.mockReturnValue({
      userId: 10,
      cleanerId: 20,
      email: 'cleaner@example.com',
      nickname: '保洁员',
      isCleaner: true,
    })
    dateMocks.getStoreCurrentMonthValue.mockReturnValue('2026-06')
    dateMocks.getStoreTodayDate.mockReturnValue('2026-06-16')
    dateMocks.parseBusinessMonthParts.mockReturnValue({ year: 2026, month: 6 })
    dateMocks.buildBusinessMonthRange.mockReturnValue({
      startDate: '2026-06-01',
      endDate: '2026-06-30',
    })
    dateMocks.buildBusinessMonthCalendarDates.mockReturnValue([
      {
        date: '2026-06-16',
        dayNumber: 16,
        isCurrentMonth: true,
      },
      {
        date: '2026-06-17',
        dayNumber: 17,
        isCurrentMonth: true,
      },
    ])
    dateMocks.shiftBusinessMonth.mockImplementation((monthValue: string) => monthValue)
  })

  test('groups backend room-date keys by taskDate for the day modal', async () => {
    apiMocks.getCalendarViewData.mockResolvedValue({
      success: true,
      message: 'ok',
      data: {
        totalCount: 2,
        statusCount: {
          assigned: 1,
          in_progress: 1,
          pending: 0,
          completed: 0,
        },
        tasks: {
          '33_2026-06-16': [
            buildTask({
              id: 1,
              roomId: 33,
              roomNumber: '101',
              status: 'assigned',
            }),
          ],
          '44_2026-06-16': [
            buildTask({
              id: 2,
              roomId: 44,
              roomNumber: '102',
              status: 'in_progress',
            }),
          ],
        },
      },
    })

    const wrapper = mount(CleanerDashboardPage)
    await flushPromises()

    const taskDayButton = wrapper.find('.cleaner-dashboard-page__day-cell')
    expect(taskDayButton.classes()).toContain('has-tasks')

    await taskDayButton.trigger('click')
    await flushPromises()

    const tabTexts = wrapper.findAll('.cleaner-dashboard-page__status-tab').map((tab) => tab.text())
    expect(tabTexts.some((text) => text.includes('待接受') && text.includes('1'))).toBe(true)
    expect(tabTexts.some((text) => text.includes('待打扫') && text.includes('1'))).toBe(true)
    expect(wrapper.findAll('.cleaner-dashboard-page__task-item')).toHaveLength(1)
    expect(wrapper.text()).toContain('101')

    const inProgressTab = wrapper
      .findAll('.cleaner-dashboard-page__status-tab')
      .find((tab) => tab.text().includes('待打扫'))
    expect(inProgressTab).toBeDefined()

    await inProgressTab?.trigger('click')
    expect(wrapper.text()).toContain('102')
  })

  test('keeps an empty task list for dates without grouped tasks', async () => {
    apiMocks.getCalendarViewData.mockResolvedValue({
      success: true,
      message: 'ok',
      data: {
        totalCount: 1,
        statusCount: {
          assigned: 1,
          in_progress: 0,
          pending: 0,
          completed: 0,
        },
        tasks: {
          '33_2026-06-16': [
            buildTask({
              id: 1,
              roomId: 33,
              roomNumber: '101',
              status: 'assigned',
            }),
          ],
        },
      },
    })

    const wrapper = mount(CleanerDashboardPage)
    await flushPromises()

    const dayButtons = wrapper.findAll('.cleaner-dashboard-page__day-cell')
    expect(dayButtons).toHaveLength(2)
    expect(dayButtons[0].classes()).toContain('has-tasks')
    expect(dayButtons[1].classes()).not.toContain('has-tasks')

    await dayButtons[1].trigger('click')
    await flushPromises()

    expect(dayButtons[1].classes()).toContain('is-selected')
    expect(wrapper.findAll('.cleaner-dashboard-page__status-tab')).toHaveLength(0)
    expect(wrapper.findAll('.cleaner-dashboard-page__task-item')).toHaveLength(0)
    expect(wrapper.text()).toContain('2026-06-17 任务')
    expect(wrapper.text()).toContain('当天该状态暂无任务。')
  })

  test('ignores malformed calendar tasks without taskDate', async () => {
    const malformedTask = buildTask({
      id: 3,
      roomId: 55,
      roomNumber: '999',
      status: 'assigned',
    }) as CleaningTaskDTO & { taskDate?: string }
    delete malformedTask.taskDate

    apiMocks.getCalendarViewData.mockResolvedValue({
      success: true,
      message: 'ok',
      data: {
        totalCount: 2,
        statusCount: {
          assigned: 2,
          in_progress: 0,
          pending: 0,
          completed: 0,
        },
        tasks: {
          '33_2026-06-16': [
            buildTask({
              id: 1,
              roomId: 33,
              roomNumber: '101',
              status: 'assigned',
            }),
          ],
          '55_missing': [malformedTask],
        },
      },
    })

    const wrapper = mount(CleanerDashboardPage)
    await flushPromises()

    const dayButtons = wrapper.findAll('.cleaner-dashboard-page__day-cell')
    expect(dayButtons[0].classes()).toContain('has-tasks')
    expect(dayButtons[1].classes()).not.toContain('has-tasks')

    await dayButtons[0].trigger('click')
    await flushPromises()

    expect(wrapper.findAll('.cleaner-dashboard-page__task-item')).toHaveLength(1)
    expect(wrapper.text()).toContain('101')
    expect(wrapper.text()).not.toContain('999')
  })
})
