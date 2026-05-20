import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useNotificationCenterStore } from '@/stores/notificationCenter'

const apiMocks = vi.hoisted(() => ({
  getMessageThreads: vi.fn(),
  getNotificationSettings: vi.fn(),
}))

vi.mock('@/api/message', () => ({
  getMessageThreads: apiMocks.getMessageThreads,
}))

vi.mock('@/api/notification', () => ({
  getNotificationSettings: apiMocks.getNotificationSettings,
}))

describe('notificationCenter store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    apiMocks.getMessageThreads.mockReset()
    apiMocks.getNotificationSettings.mockReset()
    apiMocks.getNotificationSettings.mockResolvedValue({ success: true, data: null })
  })

  it('does not request message threads without an authenticated session', async () => {
    window.localStorage.setItem('user', JSON.stringify({ id: 1 }))
    window.localStorage.setItem('currentStore', JSON.stringify({ id: 10 }))

    const notificationCenterStore = useNotificationCenterStore()

    await notificationCenterStore.start(1)

    expect(notificationCenterStore.started).toBe(false)
    expect(apiMocks.getMessageThreads).not.toHaveBeenCalled()
  })
})
