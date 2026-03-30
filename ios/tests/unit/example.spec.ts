import router from '@/router'
import { ROUTE_PATHS } from '@/router/guards'
import { describe, expect, test } from 'vitest'

describe('forgot password route', () => {
  test('registers forgot password as a public auth route', () => {
    const forgotPasswordRoute = router.getRoutes().find((route) => route.path === ROUTE_PATHS.forgotPassword)

    expect(forgotPasswordRoute).toBeDefined()
    expect(forgotPasswordRoute?.name).toBe('ForgotPassword')
    expect(forgotPasswordRoute?.meta.publicOnly).toBe(true)
  })
})
