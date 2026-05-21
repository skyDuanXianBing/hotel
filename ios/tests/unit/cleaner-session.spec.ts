import { describe, expect, test, beforeEach } from 'vitest'
import router from '@/router'
import { CLEANER_STORE_KEY, CLEANER_TOKEN_KEY, CLEANER_USER_KEY, readCleanerUser } from '@/utils/cleanerSession'
import { resolveDefaultAuthenticatedPath, ROUTE_PATHS } from '@/router/guards'

describe('cleaner session routing', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  test('clears malformed cleaner sessions instead of accepting a token alone', () => {
    window.localStorage.setItem(CLEANER_TOKEN_KEY, 'cleaner-token')
    window.localStorage.setItem(
      CLEANER_USER_KEY,
      JSON.stringify({
        id: 9,
        email: 'cleaner@example.com',
        isCleaner: true,
      }),
    )
    window.localStorage.setItem(CLEANER_STORE_KEY, JSON.stringify({ id: 2 }))

    expect(readCleanerUser()).toBeNull()
    expect(window.localStorage.getItem(CLEANER_TOKEN_KEY)).toBeNull()
    expect(window.localStorage.getItem(CLEANER_USER_KEY)).toBeNull()
    expect(window.localStorage.getItem(CLEANER_STORE_KEY)).toBeNull()
  })

  test('prefers a valid cleaner session over a stale admin session for default routing', () => {
    window.localStorage.setItem('token', 'admin-token')
    window.localStorage.setItem('currentStore', JSON.stringify({ id: 1 }))
    window.localStorage.setItem(CLEANER_TOKEN_KEY, 'cleaner-token')
    window.localStorage.setItem(
      CLEANER_USER_KEY,
      JSON.stringify({
        userId: 10,
        cleanerId: 20,
        email: 'cleaner@example.com',
        nickname: '保洁员',
        isCleaner: true,
      }),
    )
    window.localStorage.setItem(CLEANER_STORE_KEY, JSON.stringify({ id: 2 }))

    expect(resolveDefaultAuthenticatedPath()).toBe(ROUTE_PATHS.cleanerDashboard)
  })

  test('keeps an active cleaner session out of admin routes', async () => {
    window.localStorage.setItem('token', 'admin-token')
    window.localStorage.setItem('currentStore', JSON.stringify({ id: 1 }))
    window.localStorage.setItem(CLEANER_TOKEN_KEY, 'cleaner-token')
    window.localStorage.setItem(
      CLEANER_USER_KEY,
      JSON.stringify({
        userId: 10,
        cleanerId: 20,
        email: 'cleaner@example.com',
        nickname: '保洁员',
        isCleaner: true,
      }),
    )
    window.localStorage.setItem(CLEANER_STORE_KEY, JSON.stringify({ id: 2 }))

    await router.push(ROUTE_PATHS.home)

    expect(router.currentRoute.value.path).toBe(ROUTE_PATHS.cleanerDashboard)
  })
})
