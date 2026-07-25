import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { i18n } from '@/locales'
import { useStoreStore } from '@/stores/store'
import type { StoreDTO } from '@/types/store'

const apiMocks = vi.hoisted(() => {
  return {
    createStore: vi.fn(),
    deleteStore: vi.fn(),
    getUserStores: vi.fn(),
  }
})

const storageMocks = vi.hoisted(() => {
  return {
    clearStoreStorage: vi.fn(),
    getStoredCurrentStore: vi.fn(),
    getStoredStores: vi.fn(),
    writeStoredJson: vi.fn(),
  }
})

vi.mock('@/api/store', () => {
  return {
    createStore: apiMocks.createStore,
    deleteStore: apiMocks.deleteStore,
    getUserStores: apiMocks.getUserStores,
  }
})

vi.mock('@/utils/storage', () => {
  return {
    clearStoreStorage: storageMocks.clearStoreStorage,
    CURRENT_STORE_KEY: 'currentStore',
    getStoredCurrentStore: storageMocks.getStoredCurrentStore,
    getStoredStores: storageMocks.getStoredStores,
    STORES_KEY: 'stores',
    writeStoredJson: storageMocks.writeStoredJson,
  }
})

function createStoreDto(id: number, overrides: Partial<StoreDTO> = {}): StoreDTO {
  return {
    id,
    name: `Store ${id}`,
    phone: '+86 1234567890',
    type: '1',
    timezone: 'Asia/Shanghai',
    manager: 'Manager',
    ownerEmail: 'owner@example.com',
    address: 'Address',
    city: 'Shanghai',
    state: '',
    country: 'China',
    currency: 'CNY',
    userRole: 'owner',
    createdAt: '2026-03-22T00:00:00Z',
    updatedAt: '2026-03-22T00:00:00Z',
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'zh-CN'
  vi.clearAllMocks()
  storageMocks.getStoredCurrentStore.mockReturnValue(null)
  storageMocks.getStoredStores.mockReturnValue([])
})

describe('store deleteStore action', () => {
  test('removes the deleted store and clears current store', async () => {
    const storeStore = useStoreStore()
    const firstStore = createStoreDto(1)
    const secondStore = createStoreDto(2)

    storeStore.setStores([firstStore, secondStore])
    storeStore.setCurrentStore(firstStore)
    apiMocks.deleteStore.mockResolvedValue({
      success: true,
      message: '门店删除成功',
      data: null,
    })

    const message = await storeStore.deleteStore(firstStore.id)

    expect(apiMocks.deleteStore).toHaveBeenCalledWith(firstStore.id)
    expect(message).toBe('门店删除成功')
    expect(storeStore.stores).toEqual([secondStore])
    expect(storeStore.currentStore).toBeNull()
  })

  test('throws the delete error and preserves code 953', async () => {
    const storeStore = useStoreStore()

    apiMocks.deleteStore.mockResolvedValue({
      success: false,
      message: '该 Property 仍与渠道存在映射 953',
      data: {
        code: '953',
      },
    })

    try {
      await storeStore.deleteStore(9)
      throw new Error('deleteStore should throw when the API reports failure')
    } catch (error) {
      expect(error).toBeInstanceOf(Error)

      if (error instanceof Error) {
        expect(error.message).toBe('该 Property 仍与渠道存在映射 953')
      }

      expect(Reflect.get(error, 'code')).toBe('953')
    }
  })

  test('normalizes english delete success message to chinese', async () => {
    const storeStore = useStoreStore()

    apiMocks.deleteStore.mockResolvedValue({
      success: true,
      message: 'Delete store success',
      data: null,
    })

    const message = await storeStore.deleteStore(3)

    expect(message).toBe('门店删除成功')
  })
})
