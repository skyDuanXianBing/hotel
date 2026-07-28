import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useNotificationCenterStore } from '@/stores/notificationCenter'

const apiMocks = vi.hoisted(() => ({
  getMessageUnreadSummary: vi.fn(),
  getNotificationSettings: vi.fn(),
}))

vi.mock('@/api/message', () => ({
  getMessageUnreadSummary: apiMocks.getMessageUnreadSummary,
}))

vi.mock('@/api/notification', () => ({
  getNotificationSettings: apiMocks.getNotificationSettings,
}))

describe('notificationCenter store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    apiMocks.getMessageUnreadSummary.mockReset()
    apiMocks.getNotificationSettings.mockReset()
    apiMocks.getNotificationSettings.mockResolvedValue({ success: true, data: null })
  })

  it('does not request message unread summary without an authenticated session', async () => {
    window.localStorage.setItem('user', JSON.stringify({ id: 1 }))
    window.localStorage.setItem('currentStore', JSON.stringify({ id: 10 }))

    const notificationCenterStore = useNotificationCenterStore()

    await notificationCenterStore.start(1)

    expect(notificationCenterStore.started).toBe(false)
    expect(apiMocks.getMessageUnreadSummary).not.toHaveBeenCalled()
  })
})
