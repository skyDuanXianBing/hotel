import { mock } from 'bun:test'
import { createPinia, setActivePinia } from 'pinia'

type TestStore = {
  id: number
  name: string
  timezone: string
  userRole: string
}

type LocalStorageMock = {
  getItem: (key: string) => string | null
  setItem: (key: string, value: string) => void
  removeItem: (key: string) => void
  clear: () => void
  key: (index: number) => string | null
  readonly length: number
}

const localStorageData = new Map<string, string>()

let updateStoreResult: TestStore | null = null
let lastUpdateStoreId: number | null = null

const fail = (message: string): never => {
  console.error(`[storeStore.test] ${message}`)
  process.exit(1)
}

const assertEquals = (actual: unknown, expected: unknown, message: string): void => {
  if (actual !== expected) {
    fail(`${message}. Expected ${String(expected)}, got ${String(actual)}`)
  }
}

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

const createStoreFixture = (timezone: string): TestStore => {
  return {
    id: 1,
    name: `Hotel ${timezone}`,
    timezone,
    userRole: 'owner',
  }
}

const getCachedCurrentStore = (): TestStore => {
  const cachedStore = localStorage.getItem('currentStore')
  if (!cachedStore) {
    fail('currentStore should be cached in localStorage')
  }

  return JSON.parse(cachedStore) as TestStore
}

mock.module('@/api/store', () => {
  return {
    getUserStores: async () => ({ success: true, data: [] }),
    getStoreById: async () => ({ success: true, data: null }),
    createStore: async () => ({ success: true, data: null }),
    updateStore: async (storeId: number) => {
      lastUpdateStoreId = storeId
      return { success: true, data: updateStoreResult, message: 'updated' }
    },
    deleteStore: async () => ({ success: true, data: null }),
    addStoreMember: async () => ({ success: true, data: null }),
    removeStoreMember: async () => ({ success: true, data: null }),
    getStoreMembers: async () => ({ success: true, data: [] }),
  }
})

mock.module('@/locales', () => {
  return {
    i18n: {
      global: {
        t: (key: string) => key,
      },
    },
  }
})

installLocalStorage()

const { useStoreStore } = await import('../store')

const createStoreStoreWithCachedCurrentStore = (storeFixture: TestStore) => {
  localStorage.clear()
  localStorage.setItem('currentStore', JSON.stringify(storeFixture))
  localStorage.setItem('stores', JSON.stringify([storeFixture]))
  updateStoreResult = null
  lastUpdateStoreId = null
  setActivePinia(createPinia())
  return useStoreStore()
}

const assertCurrentStoreTimeZone = (actualTimezone: string, message: string): void => {
  const cachedStore = getCachedCurrentStore()
  assertEquals(cachedStore.timezone, actualTimezone, `${message} in localStorage`)
}

const assertSetStoresSyncsCurrentStoreCache = (): void => {
  const shanghaiStore = createStoreFixture('Asia/Shanghai')
  const tokyoStore = createStoreFixture('Asia/Tokyo')
  const storeStore = createStoreStoreWithCachedCurrentStore(shanghaiStore)

  storeStore.setStores([tokyoStore])

  assertEquals(
    storeStore.currentStore?.timezone,
    'Asia/Tokyo',
    'setStores should sync currentStore timezone'
  )
  assertCurrentStoreTimeZone('Asia/Tokyo', 'setStores should sync currentStore timezone')
}

const assertUpdateStoreSyncsCurrentStoreCache = async (): Promise<void> => {
  const shanghaiStore = createStoreFixture('Asia/Shanghai')
  const tokyoStore = createStoreFixture('Asia/Tokyo')
  const storeStore = createStoreStoreWithCachedCurrentStore(shanghaiStore)
  updateStoreResult = tokyoStore

  await storeStore.updateStore(1, {
    name: 'Hotel Asia/Tokyo',
    timezone: 'Asia/Tokyo',
  })

  assertEquals(lastUpdateStoreId, 1, 'updateStore should call API for current store id')
  assertEquals(
    storeStore.currentStore?.timezone,
    'Asia/Tokyo',
    'updateStore should sync currentStore timezone'
  )
  assertCurrentStoreTimeZone('Asia/Tokyo', 'updateStore should sync currentStore timezone')
}

assertSetStoresSyncsCurrentStoreCache()
await assertUpdateStoreSyncsCurrentStoreCache()

console.log('[storeStore.test] ok: currentStore timezone cache sync verified')
