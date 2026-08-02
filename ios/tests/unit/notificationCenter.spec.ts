import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useNotificationCenterStore } from '@/stores/notificationCenter'

const apiMocks = vi.hoisted(() => ({
  getNotificationBadgeSummary: vi.fn(),
  getNotificationSettings: vi.fn(),
}))

vi.mock('@/api/notification', () => ({
  getNotificationBadgeSummary: apiMocks.getNotificationBadgeSummary,
  getNotificationSettings: apiMocks.getNotificationSettings,
}))

describe('notificationCenter store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    apiMocks.getNotificationBadgeSummary.mockReset()
    apiMocks.getNotificationSettings.mockReset()
    apiMocks.getNotificationSettings.mockResolvedValue({ success: true, data: null })
  })

  it('does not request badge summary without an authenticated session', async () => {
    window.localStorage.setItem('user', JSON.stringify({ id: 1 }))
    window.localStorage.setItem('currentStore', JSON.stringify({ id: 10 }))

    const notificationCenterStore = useNotificationCenterStore()

    await notificationCenterStore.start(1)

    expect(notificationCenterStore.started).toBe(false)
    expect(apiMocks.getNotificationBadgeSummary).not.toHaveBeenCalled()
  })

  it('syncs unread/pending counts and app badge from badge summary', async () => {
    window.localStorage.setItem('token', 'token-1')
    window.localStorage.setItem('currentStore', JSON.stringify({ id: 10 }))
    apiMocks.getNotificationBadgeSummary.mockResolvedValue({
      success: true,
      data: { unreadMessages: 4, pendingReviews: 3, total: 7 },
    })

    const notificationCenterStore = useNotificationCenterStore()

    await notificationCenterStore.start(1)

    expect(notificationCenterStore.started).toBe(true)
    expect(apiMocks.getNotificationBadgeSummary).toHaveBeenCalled()
    expect(notificationCenterStore.unreadMessageCount).toBe(4)
    expect(notificationCenterStore.pendingReviewCount).toBe(3)

    notificationCenterStore.stop()
  })
})
