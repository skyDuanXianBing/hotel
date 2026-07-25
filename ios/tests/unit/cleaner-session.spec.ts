import { describe, expect, test, beforeEach, afterEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import router from '@/router'
import { i18n } from '@/locales'
import type { LoginResponse, UserDTO } from '@/types/auth'
import type { StoreDTO } from '@/types/store'
import request, { isHandledRequestError } from '@/utils/request'
import {
  CLEANER_STORE_KEY,
  CLEANER_TOKEN_KEY,
  CLEANER_USER_KEY,
  readCleanerStore,
  readCleanerUser,
} from '@/utils/cleanerSession'
import { applyUnifiedLoginResponse } from '@/utils/loginSessionResolver'
import {
  registerRouterGuards,
  resolveDefaultAuthenticatedPath,
  ROUTE_PATHS,
} from '@/router/guards'
import { saveAutoLoginCredentials } from '@/utils/autoLogin'

vi.mock('@/router', async () => {
  const { shallowRef } = await import('vue')
  const currentRoute = shallowRef({
    path: '/',
    fullPath: '/',
    query: {},
    params: {},
    hash: '',
    name: undefined,
    matched: [],
    meta: {},
    redirectedFrom: undefined,
  })

  return {
    default: {
      currentRoute,
      replace: vi.fn(async (path: string) => {
        currentRoute.value = {
          ...currentRoute.value,
          path,
          fullPath: path,
        }
      }),
    },
  }
})

const createCleanerTestRouter = () => {
  const testRouter = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: ROUTE_PATHS.login,
        name: 'Login',
        component: { template: '<div />' },
        meta: { publicOnly: true },
      },
      {
        path: ROUTE_PATHS.cleanerLogin,
        name: 'CleanerLogin',
        redirect: (to) => ({
          path: ROUTE_PATHS.login,
          query: {
            ...to.query,
            workspace: 'CLEANER',
          },
        }),
      },
      {
        path: ROUTE_PATHS.home,
        name: 'Home',
        component: { template: '<div />' },
        meta: { requiresAuth: true, requiresStore: true },
      },
      {
        path: ROUTE_PATHS.cleanerDashboard,
        name: 'CleanerDashboard',
        component: { template: '<div />' },
        meta: { requiresCleanerAuth: true, requiresCleanerStore: true },
      },
    ],
  })
  const disposeGuards = registerRouterGuards(testRouter)

  return {
    router: testRouter,
    disposeGuards,
  }
}

const buildUser = (overrides: Partial<UserDTO> = {}): UserDTO => ({
  id: 10,
  email: 'member@example.com',
  nickname: '前台成员',
  gender: 'private',
  createdAt: '2026-06-24T00:00:00Z',
  updatedAt: '2026-06-24T00:00:00Z',
  ...overrides,
})

const buildStore = (overrides: Partial<StoreDTO> = {}): StoreDTO => ({
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
  createdAt: '2026-06-24T00:00:00Z',
  updatedAt: '2026-06-24T00:00:00Z',
  ...overrides,
})

const createFakeJwt = (expiresAtMs: number) => {
  const header = { alg: 'HS256', typ: 'JWT' }
  const payload = { exp: Math.floor(expiresAtMs / 1000) }

  const encodeJwtSegment = (value: Record<string, unknown>) => {
    return btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
  }

  return `${encodeJwtSegment(header)}.${encodeJwtSegment(payload)}.signature`
}

const buildCleanerPayload = (overrides: Partial<LoginResponse> = {}): LoginResponse => {
  const store = buildStore()

  return {
    token: 'cleaner-token',
    user: buildUser(),
    stores: [store],
    loginTarget: 'CLEANER',
    cleaner: {
      id: 20,
      userId: 10,
      storeId: store.id,
      name: '保洁员',
      email: 'cleaner@example.com',
      isActive: true,
      createdAt: '2026-06-24T00:00:00Z',
      updatedAt: '2026-06-24T00:00:00Z',
    },
    targetStoreId: store.id,
    ...overrides,
  }
}

describe('cleaner session routing', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    i18n.global.locale.value = 'zh-CN'
    router.currentRoute.value = {
      ...router.currentRoute.value,
      path: '/',
      fullPath: '/',
      query: {},
    }
    vi.mocked(router.replace).mockClear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
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

    const { router: isolatedRouter, disposeGuards } = createCleanerTestRouter()

    await isolatedRouter.push(ROUTE_PATHS.home)

    expect(isolatedRouter.currentRoute.value.path).toBe(ROUTE_PATHS.cleanerDashboard)
    disposeGuards()
  })

  test('redirects the legacy cleaner login route to unified login and preserves query', async () => {
    const { router: isolatedRouter, disposeGuards } = createCleanerTestRouter()

    await isolatedRouter.push({
      path: ROUTE_PATHS.cleanerLogin,
      query: {
        email: 'cleaner@example.com',
      },
    })

    expect(isolatedRouter.currentRoute.value.path).toBe(ROUTE_PATHS.login)
    expect(isolatedRouter.currentRoute.value.query.email).toBe('cleaner@example.com')
    expect(isolatedRouter.currentRoute.value.query.workspace).toBe('CLEANER')
    disposeGuards()
  })

  test('redirects incomplete cleaner sessions without a store back to unified login', async () => {
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

    const { router: isolatedRouter, disposeGuards } = createCleanerTestRouter()
    await isolatedRouter.push(ROUTE_PATHS.cleanerDashboard)

    expect(isolatedRouter.currentRoute.value.path).toBe(ROUTE_PATHS.login)
    disposeGuards()
  })

  test('applies PMS login target and clears stale cleaner session', () => {
    const store = buildStore()
    window.localStorage.setItem(CLEANER_TOKEN_KEY, 'stale-cleaner-token')
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
    window.localStorage.setItem(CLEANER_STORE_KEY, JSON.stringify({ id: store.id }))

    const result = applyUnifiedLoginResponse(
      {
        token: 'pms-token',
        user: buildUser(),
        stores: [store],
        loginTarget: 'PMS',
      },
      {
        resetPmsCurrentStore: true,
      },
    )

    expect(result.target).toBe('PMS')
    expect(result.redirectPath).toBe(ROUTE_PATHS.storeSelection)
    expect(window.localStorage.getItem('token')).toBe('pms-token')
    expect(window.localStorage.getItem(CLEANER_TOKEN_KEY)).toBeNull()
    expect(window.localStorage.getItem(CLEANER_USER_KEY)).toBeNull()
    expect(window.localStorage.getItem(CLEANER_STORE_KEY)).toBeNull()
    expect(resolveDefaultAuthenticatedPath()).toBe(ROUTE_PATHS.storeSelection)
  })

  test('applies CLEANER login target and clears stale PMS session', () => {
    const store = buildStore()
    window.localStorage.setItem('token', 'stale-admin-token')
    window.localStorage.setItem('currentStore', JSON.stringify({ id: 1 }))

    const result = applyUnifiedLoginResponse(
      buildCleanerPayload({
        stores: [store],
        currentStore: null,
        targetStoreId: store.id,
      }),
      {
        resetPmsCurrentStore: true,
      },
    )

    expect(result.target).toBe('CLEANER')
    expect(result.redirectPath).toBe(ROUTE_PATHS.cleanerDashboard)
    expect(window.localStorage.getItem('token')).toBeNull()
    expect(window.localStorage.getItem('currentStore')).toBeNull()
    expect(window.localStorage.getItem(CLEANER_TOKEN_KEY)).toBe('cleaner-token')
    expect(readCleanerUser()?.cleanerId).toBe(20)
    expect(readCleanerStore()?.id).toBe(store.id)
    expect(readCleanerStore()?.name).toBe(store.name)
    expect(resolveDefaultAuthenticatedPath()).toBe(ROUTE_PATHS.cleanerDashboard)
  })

  test('rejects incomplete cleaner payloads and does not keep partial sessions', () => {
    window.localStorage.setItem('token', 'stale-admin-token')
    window.localStorage.setItem(CLEANER_TOKEN_KEY, 'stale-cleaner-token')

    expect(() =>
      applyUnifiedLoginResponse(
        buildCleanerPayload({
          stores: [],
          currentStore: null,
          targetStoreId: 2,
        }),
      ),
    ).toThrow('登录响应缺少当前保洁门店信息')

    expect(window.localStorage.getItem('token')).toBeNull()
    expect(window.localStorage.getItem(CLEANER_TOKEN_KEY)).toBeNull()
    expect(window.localStorage.getItem(CLEANER_USER_KEY)).toBeNull()
    expect(window.localStorage.getItem(CLEANER_STORE_KEY)).toBeNull()
  })

  test('rejects cleaner payloads with mismatched target store', () => {
    const mismatchedStore = buildStore({ id: 3 })

    expect(() =>
      applyUnifiedLoginResponse(
        buildCleanerPayload({
          stores: [mismatchedStore],
          currentStore: mismatchedStore,
          targetStoreId: mismatchedStore.id,
        }),
      ),
    ).toThrow('登录响应中的保洁门店与保洁员身份不一致')

    expect(window.localStorage.getItem(CLEANER_TOKEN_KEY)).toBeNull()
  })

  test('stops a stale admin route request when silent reauth restores a cleaner target', async () => {
      const adminStore = buildStore({ id: 1, name: '前台门店' })
      const expiringAdminToken = createFakeJwt(Date.now() + 1_000)
      const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const requestUrl = String(input)

        if (requestUrl.includes('/auth/login/password')) {
          return new Response(
            JSON.stringify({
              success: true,
              data: buildCleanerPayload(),
            }),
            {
              status: 200,
              headers: {
                'Content-Type': 'application/json',
              },
            },
          )
        }

        return new Response(JSON.stringify({ success: true, data: [] }), {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
        })
      })

      vi.stubGlobal('fetch', fetchMock)
      window.localStorage.setItem('token', expiringAdminToken)
      window.localStorage.setItem('currentStore', JSON.stringify(adminStore))

      await saveAutoLoginCredentials({
        email: 'member@example.com',
        password: 'password123',
        token: expiringAdminToken,
        preferredLoginTarget: 'CLEANER',
      })
      router.currentRoute.value = {
        ...router.currentRoute.value,
        path: ROUTE_PATHS.home,
        fullPath: ROUTE_PATHS.home,
      }

      let caughtError: unknown = null
      try {
        await request.get('/rooms')
      } catch (error) {
        caughtError = error
      }

      expect(isHandledRequestError(caughtError)).toBe(true)
      expect(caughtError).toBeInstanceOf(Error)
      expect((caughtError as Error).message).toBe(i18n.global.t('request.sessionChanged'))
      expect(fetchMock).toHaveBeenCalledTimes(1)
      expect(String(fetchMock.mock.calls[0][0])).toContain('/auth/login/password')
      expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual({
        email: 'member@example.com',
        password: 'password123',
        rememberMe: true,
        preferredLoginTarget: 'CLEANER',
      })
      expect(fetchMock.mock.calls.some(([input]) => String(input).includes('/rooms'))).toBe(false)
      expect(router.currentRoute.value.path).toBe(ROUTE_PATHS.cleanerDashboard)
      expect(router.replace).toHaveBeenCalledWith(ROUTE_PATHS.cleanerDashboard)
  })
})
