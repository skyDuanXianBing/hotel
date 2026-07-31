import { beforeEach, describe, expect, mock, test } from 'bun:test'
import { createPinia, setActivePinia } from 'pinia'
import { reactive } from 'vue'

// bun test 环境无 localStorage；entitlement store 本身不直接用，
// 但被 mock 的依赖链可能触达，统一兜底。
class MemoryStorage {
  private map = new Map<string, string>()
  getItem(key: string) {
    return this.map.has(key) ? this.map.get(key)! : null
  }
  setItem(key: string, value: string) {
    this.map.set(key, String(value))
  }
  removeItem(key: string) {
    this.map.delete(key)
  }
  clear() {
    this.map.clear()
  }
}
// 其他测试文件以 defineProperty（writable:false）定义 localStorage，必须用同等方式覆盖
Object.defineProperty(globalThis, 'localStorage', {
  value: new MemoryStorage(),
  configurable: true,
  writable: true,
})

const mockStoreStore = reactive<{ currentStore: { id: number } | null }>({ currentStore: null })

let subscriptionResponse: { success: boolean; message: string; data: unknown } = {
  success: true,
  message: 'ok',
  data: null,
}
let shouldThrow = false
let getMySubscriptionCalls = 0

mock.module('@/api/billing', () => ({
  getMySubscription: async () => {
    getMySubscriptionCalls += 1
    if (shouldThrow) {
      throw new Error('network down')
    }
    return subscriptionResponse
  },
  listBillingPackages: async () => ({ success: true, message: 'ok', data: [] }),
  subscribeBillingPackage: async () => ({ success: true, message: 'ok', data: null }),
  SAAS_FEATURE_CODES: {
    INDEPENDENT_WEBSITE: 'independent_website',
    AI_WEBSITE_GEN: 'ai_website_gen',
    ROOM_COUNT: 'room_count',
  },
}))

mock.module('@/stores/store', () => ({
  useStoreStore: () => mockStoreStore,
}))

const { useEntitlementStore } = await import('../entitlement')

const activeSubscription = {
  id: 1,
  packageId: 2,
  packageName: '豪华版',
  pricePaid: 999,
  startTime: '2026-07-01T00:00:00',
  endTime: '2026-08-01T00:00:00',
  status: 'ACTIVE',
  entitlements: [
    { featureCode: 'independent_website', type: 'BOOLEAN', limit: null },
    { featureCode: 'ai_website_gen', type: 'QUOTA', limit: 50 },
    { featureCode: 'room_count', type: 'CAPACITY', limit: 50 },
  ],
  quotas: [
    {
      featureCode: 'ai_website_gen',
      name: 'AI 建站生成次数',
      totalQuota: 50,
      usedQuota: 12,
      remaining: 38,
      periodStart: null,
      periodEnd: null,
    },
  ],
}

const setup = (storeId: number | null) => {
  setActivePinia(createPinia())
  mockStoreStore.currentStore = storeId === null ? null : { id: storeId }
  subscriptionResponse = { success: true, message: 'ok', data: null }
  shouldThrow = false
  getMySubscriptionCalls = 0
  return useEntitlementStore()
}

describe('entitlement store', () => {
  test('拉取失败时 fail-open：hasFeature/hasQuota 一律放行，不锁死前端', async () => {
    const store = setup(1)
    shouldThrow = true

    await store.refresh(true)

    expect(store.loaded).toBe(false)
    expect(store.hasFeature('independent_website')).toBe(true)
    expect(store.hasFeature('unknown_feature')).toBe(true)
    expect(store.hasQuota('ai_website_gen')).toBe(true)
    expect(store.quotaFor('ai_website_gen')).toBeNull()
  })

  test('已加载订阅：按快照判定权益与配额', async () => {
    const store = setup(1)
    subscriptionResponse = { success: true, message: 'ok', data: activeSubscription }

    await store.refresh(true)

    expect(store.loaded).toBe(true)
    expect(store.hasFeature('independent_website')).toBe(true)
    expect(store.hasFeature('room_count')).toBe(true)
    expect(store.hasFeature('advanced_report')).toBe(false)
    expect(store.hasQuota('ai_website_gen')).toBe(true)
    expect(store.quotaFor('ai_website_gen')?.remaining).toBe(38)
  })

  test('配额剩余为 0 时 hasQuota 拦截；剩余为 null（不限）放行', async () => {
    const store = setup(1)
    subscriptionResponse = {
      success: true,
      message: 'ok',
      data: {
        ...activeSubscription,
        quotas: [{ ...activeSubscription.quotas[0], usedQuota: 50, remaining: 0 }],
      },
    }

    await store.refresh(true)
    expect(store.hasQuota('ai_website_gen')).toBe(false)

    subscriptionResponse = {
      success: true,
      message: 'ok',
      data: {
        ...activeSubscription,
        quotas: [
          { ...activeSubscription.quotas[0], totalQuota: null, usedQuota: 999, remaining: null },
        ],
      },
    }
    await store.refresh(true)
    expect(store.hasQuota('ai_website_gen')).toBe(true)
  })

  test('已加载但无订阅：权益一律视为缺失（loaded 后不再 fail-open）', async () => {
    const store = setup(1)
    subscriptionResponse = { success: true, message: 'ok', data: null }

    await store.refresh(true)

    expect(store.loaded).toBe(true)
    expect(store.subscription).toBeNull()
    expect(store.hasFeature('independent_website')).toBe(false)
    expect(store.hasQuota('ai_website_gen')).toBe(false)
  })

  test('同店重复 refresh 走缓存，切店后重新拉取', async () => {
    const store = setup(1)
    subscriptionResponse = { success: true, message: 'ok', data: activeSubscription }

    await store.refresh()
    await store.refresh()
    expect(getMySubscriptionCalls).toBe(1)

    mockStoreStore.currentStore = { id: 2 }
    await store.refresh()
    expect(getMySubscriptionCalls).toBe(2)
    expect(store.loadedStoreId).toBe(2)
  })

  test('无门店时不发请求并保持未加载', async () => {
    const store = setup(null)

    await store.refresh(true)

    expect(getMySubscriptionCalls).toBe(0)
    expect(store.loaded).toBe(false)
    expect(store.hasFeature('independent_website')).toBe(true)
  })

  test('openUpgradeGuide 记录上下文并打开弹窗', async () => {
    const store = setup(1)
    subscriptionResponse = { success: true, message: 'ok', data: activeSubscription }

    store.openUpgradeGuide({
      featureCode: 'ai_website_gen',
      limit: 50,
      used: 50,
      message: 'quota exhausted',
    })

    expect(store.upgradeDialogVisible).toBe(true)
    expect(store.upgradeContext?.featureCode).toBe('ai_website_gen')
    expect(store.upgradeContext?.limit).toBe(50)

    store.closeUpgradeGuide()
    expect(store.upgradeDialogVisible).toBe(false)
  })
})
