import { beforeEach, describe, expect, mock, test } from 'bun:test'
import type { Router } from 'vue-router'
import type { LoginResponse, UserDTO } from '@/api/auth'
import type { StoreDTO } from '@/api/store'
import type { CleanerSessionStore, CleanerSessionUser } from '../cleanerSession'

type LocalStorageMock = {
  getItem: (key: string) => string | null
  setItem: (key: string, value: string) => void
  removeItem: (key: string) => void
  clear: () => void
  key: (index: number) => string | null
  readonly length: number
}

const localStorageData = new Map<string, string>()

const installLocalStorage = (): void => {
  const storage: LocalStorageMock = {
    getItem(key: string): string | null {
      if (localStorageData.has(key)) {
        return localStorageData.get(key) ?? null
      }
      return null
    },
    setItem(key: string, value: string): void {
      localStorageData.set(key, String(value))
    },
    removeItem(key: string): void {
      localStorageData.delete(key)
    },
    clear(): void {
      localStorageData.clear()
    },
    key(index: number): string | null {
      const keys = Array.from(localStorageData.keys())
      return keys[index] ?? null
    },
    get length(): number {
      return localStorageData.size
    },
  }

  Object.defineProperty(globalThis, 'localStorage', {
    value: storage,
    configurable: true,
  })
}

mock.module('@/stores/user', () => {
  return {
    useUserStore: () => {
      return {
        setUser: (user: UserDTO | null): void => {
          if (user) {
            localStorage.setItem(cleanerSession.PMS_USER_KEY, JSON.stringify(user))
          } else {
            localStorage.removeItem(cleanerSession.PMS_USER_KEY)
          }
        },
      }
    },
  }
})

mock.module('@/stores/store', () => {
  return {
    useStoreStore: () => {
      return {
        clearStoreData: (): void => {
          localStorage.removeItem(cleanerSession.PMS_CURRENT_STORE_KEY)
          localStorage.removeItem(cleanerSession.PMS_STORES_KEY)
        },
        setStores: (stores: StoreDTO[]): void => {
          localStorage.setItem(cleanerSession.PMS_STORES_KEY, JSON.stringify(stores))
        },
      }
    },
  }
})

mock.module('@/stores/permission', () => {
  return {
    usePermissionStore: () => {
      return {
        clearPermissions: (): void => undefined,
      }
    },
  }
})

installLocalStorage()

const cleanerSession = await import('../cleanerSession')

mock.module('@/utils/cleanerSession', () => {
  return cleanerSession
})

const { normalizePreferredLoginTarget, resolveLoginTarget } = await import('../loginTargetResolver')

const createRouterStub = (): { router: Router; pushedPaths: string[] } => {
  const pushedPaths: string[] = []
  const router = {
    push: async (path: string): Promise<void> => {
      pushedPaths.push(path)
    },
  } as unknown as Router

  return { router, pushedPaths }
}

const createPmsUser = (): UserDTO => {
  return {
    id: 7,
    email: 'pms@example.test',
    nickname: 'PMS User',
  } as UserDTO
}

const createStore = (): StoreDTO => {
  return {
    id: 3,
    name: 'Main Hotel',
  } as StoreDTO
}

const createCleanerLoginResponse = (): LoginResponse => {
  const store = createStore()

  return {
    token: 'cleaner-token',
    user: createPmsUser(),
    stores: [store],
    loginTarget: 'CLEANER',
    cleaner: {
      id: 11,
      userId: 7,
      storeId: store.id,
      email: 'cleaner@example.test',
      name: 'Cleaner User',
      isActive: true,
    },
    currentStore: store,
    targetStoreId: store.id,
  } as LoginResponse
}

const readJsonItem = <T>(key: string): T => {
  const value = localStorage.getItem(key)
  expect(value).toBeTruthy()
  return JSON.parse(value as string) as T
}

beforeEach(() => {
  localStorage.clear()
})

describe('resolveCachedLoginSessionTarget', () => {
  test('clears workspace availability with all local session state', () => {
    cleanerSession.saveAvailableLoginTargets(['PMS', 'CLEANER'])
    localStorage.setItem(cleanerSession.PMS_TOKEN_KEY, 'token')

    cleanerSession.clearAllLocalSessions()

    expect(cleanerSession.readAvailableLoginTargets()).toEqual([])
    expect(localStorage.getItem(cleanerSession.PMS_TOKEN_KEY)).toBeNull()
  })

  test('prefers complete cleaner session over stale PMS token', () => {
    const store = createStore()

    localStorage.setItem(cleanerSession.PMS_TOKEN_KEY, 'stale-pms-token')
    localStorage.setItem(cleanerSession.PMS_USER_KEY, JSON.stringify(createPmsUser()))
    localStorage.setItem(cleanerSession.CLEANER_TOKEN_KEY, 'cleaner-token')
    localStorage.setItem(
      cleanerSession.CLEANER_USER_KEY,
      JSON.stringify({
        userId: 7,
        cleanerId: 11,
        email: 'cleaner@example.test',
        nickname: 'Cleaner User',
        isCleaner: true,
      })
    )
    localStorage.setItem(cleanerSession.CLEANER_STORE_KEY, JSON.stringify(store))

    expect(cleanerSession.resolveCachedLoginSessionTarget()).toBe('CLEANER')
  })

  test('falls back to PMS token and clears incomplete cleaner session', () => {
    localStorage.setItem(cleanerSession.PMS_TOKEN_KEY, 'pms-token')
    localStorage.setItem(cleanerSession.CLEANER_TOKEN_KEY, 'incomplete-cleaner-token')
    localStorage.setItem(
      cleanerSession.CLEANER_USER_KEY,
      JSON.stringify({
        userId: 7,
        cleanerId: 11,
        email: 'cleaner@example.test',
        nickname: 'Cleaner User',
        isCleaner: true,
      })
    )

    expect(cleanerSession.resolveCachedLoginSessionTarget()).toBe('PMS')
    expect(localStorage.getItem(cleanerSession.CLEANER_TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.CLEANER_USER_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.CLEANER_STORE_KEY)).toBeNull()
  })
})

describe('resolveLoginTarget', () => {
  test('normalizes workspace intent from unified login routes', () => {
    expect(normalizePreferredLoginTarget('cleaner')).toBe('CLEANER')
    expect(normalizePreferredLoginTarget('PMS')).toBe('PMS')
    expect(normalizePreferredLoginTarget('unknown')).toBeUndefined()
  })
  test('PMS target clears cleaner session and routes to store selection', async () => {
    const { router, pushedPaths } = createRouterStub()
    const store = createStore()
    const user = createPmsUser()

    localStorage.setItem(cleanerSession.CLEANER_TOKEN_KEY, 'stale-cleaner-token')
    localStorage.setItem(
      cleanerSession.CLEANER_USER_KEY,
      JSON.stringify({
        userId: 7,
        cleanerId: 11,
        email: 'cleaner@example.test',
        nickname: 'Cleaner User',
        isCleaner: true,
      })
    )
    localStorage.setItem(cleanerSession.CLEANER_STORE_KEY, JSON.stringify(store))

    await resolveLoginTarget(
      {
        token: 'pms-token',
        user,
        stores: [store],
        loginTarget: 'PMS',
        availableLoginTargets: ['PMS', 'CLEANER'],
      } as LoginResponse,
      router
    )

    expect(localStorage.getItem(cleanerSession.PMS_TOKEN_KEY)).toBe('pms-token')
    expect(readJsonItem<UserDTO>(cleanerSession.PMS_USER_KEY)).toEqual(user)
    expect(readJsonItem<StoreDTO[]>(cleanerSession.PMS_STORES_KEY)).toEqual([store])
    expect(localStorage.getItem(cleanerSession.CLEANER_TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.CLEANER_USER_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.CLEANER_STORE_KEY)).toBeNull()
    expect(cleanerSession.readAvailableLoginTargets()).toEqual(['PMS', 'CLEANER'])
    expect(pushedPaths).toEqual(['/store/selection'])
  })

  test('CLEANER target requires complete payload before writing cleaner session', async () => {
    const { router, pushedPaths } = createRouterStub()
    const incompleteResponse = createCleanerLoginResponse()
    incompleteResponse.currentStore = undefined
    incompleteResponse.stores = []

    localStorage.setItem(cleanerSession.PMS_TOKEN_KEY, 'stale-pms-token')
    localStorage.setItem(cleanerSession.PMS_USER_KEY, JSON.stringify(createPmsUser()))

    await expect(resolveLoginTarget(incompleteResponse, router)).rejects.toThrow(
      '保洁账号信息不完整'
    )

    expect(localStorage.getItem(cleanerSession.PMS_TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.PMS_USER_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.CLEANER_TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.CLEANER_USER_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.CLEANER_STORE_KEY)).toBeNull()
    expect(pushedPaths).toEqual([])
  })

  test('CLEANER target writes complete cleaner session and routes to dashboard', async () => {
    const { router, pushedPaths } = createRouterStub()

    localStorage.setItem(cleanerSession.PMS_TOKEN_KEY, 'stale-pms-token')
    localStorage.setItem(cleanerSession.PMS_USER_KEY, JSON.stringify(createPmsUser()))
    localStorage.setItem(cleanerSession.PMS_STORES_KEY, JSON.stringify([createStore()]))

    await resolveLoginTarget(createCleanerLoginResponse(), router)

    const cleanerUser = readJsonItem<CleanerSessionUser>(cleanerSession.CLEANER_USER_KEY)
    const cleanerStore = readJsonItem<CleanerSessionStore>(cleanerSession.CLEANER_STORE_KEY)

    expect(localStorage.getItem(cleanerSession.CLEANER_TOKEN_KEY)).toBe('cleaner-token')
    expect(cleanerUser.cleanerId).toBe(11)
    expect(cleanerUser.userId).toBe(7)
    expect(cleanerUser.isCleaner).toBe(true)
    expect(cleanerStore).toEqual(createStore())
    expect(cleanerSession.readCleanerContexts()).toHaveLength(1)
    expect(cleanerSession.readAvailableLoginTargets()).toEqual(['CLEANER'])
    expect(localStorage.getItem(cleanerSession.PMS_TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.PMS_USER_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.PMS_STORES_KEY)).toBeNull()
    expect(localStorage.getItem(cleanerSession.PMS_CURRENT_STORE_KEY)).toBeNull()
    expect(pushedPaths).toEqual(['/cleaner/dashboard'])
  })

  test('CLEANER target keeps all contexts and supports switching stores', async () => {
    const { router } = createRouterStub()
    const response = createCleanerLoginResponse()
    const secondStore = { ...createStore(), id: 9, name: 'Second Hotel' }
    const secondCleaner = { ...response.cleaner!, id: 19, storeId: secondStore.id }
    response.cleanerContexts = [
      {
        cleanerId: response.cleaner!.id,
        storeId: response.currentStore!.id,
        cleaner: response.cleaner!,
        store: response.currentStore!,
      },
      {
        cleanerId: secondCleaner.id,
        storeId: secondStore.id,
        cleaner: secondCleaner,
        store: secondStore,
      },
    ]
    response.availableLoginTargets = ['PMS', 'CLEANER']

    await resolveLoginTarget(response, router)

    const contexts = cleanerSession.readCleanerContexts()
    expect(contexts).toHaveLength(2)
    const switchedUser = cleanerSession.switchCleanerContext(contexts[1])
    expect(switchedUser.cleanerId).toBe(19)
    expect(cleanerSession.readCleanerStoreId()).toBe(9)
    expect(cleanerSession.readAvailableLoginTargets()).toEqual(['PMS', 'CLEANER'])
  })
})
