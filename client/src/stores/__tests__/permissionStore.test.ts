import { describe, expect, mock, test } from 'bun:test'
import { createPinia, setActivePinia } from 'pinia'
import { reactive } from 'vue'
import type { PermissionDTO } from '../../api/role'

type PermissionResponse = {
  success: boolean
  message: string
  data: PermissionDTO[]
}

const PermissionModule = {
  ACCOMMODATION: 'ACCOMMODATION',
  ORDER: 'ORDER',
} as const
const PermissionAction = {
  CREATE_INTERNAL_TASK: 'CREATE_INTERNAL_TASK',
  VIEW_ORDERS: 'VIEW_ORDERS',
} as const

const mockStoreStore = reactive<{
  currentStore: { id: number; userRole: string } | null
}>({ currentStore: null })

const permissionRequests = new Map<number, Array<(response: PermissionResponse) => void>>()

mock.module('@/api/store', () => ({
  getMyStorePermissions: (storeId: number) =>
    new Promise<PermissionResponse>((resolve) => {
      const resolvers = permissionRequests.get(storeId) || []
      resolvers.push(resolve)
      permissionRequests.set(storeId, resolvers)
    }),
  getUserStores: async () => ({ success: true, data: [] }),
  getStoreById: async () => ({ success: true, data: null }),
  createStore: async () => ({ success: true, data: null }),
  updateStore: async () => ({ success: true, data: null }),
  deleteStore: async () => ({ success: true, data: null }),
  addStoreMember: async () => ({ success: true, data: null }),
  removeStoreMember: async () => ({ success: true, data: null }),
  getStoreMembers: async () => ({ success: true, data: [] }),
}))

mock.module('@/api/role', () => ({ PermissionModule, PermissionAction }))

mock.module('@/stores/store', () => ({
  useStoreStore: () => mockStoreStore,
}))

mock.module('@/locales', () => ({
  i18n: { global: { t: (key: string) => key } },
}))

const { usePermissionStore } = await import('../permission')

const createStore = (id: number, userRole: string) => ({
  id,
  name: `Hotel ${id}`,
  userRole,
})

const resolvePermissionRequest = (storeId: number, data: PermissionDTO[]) => {
  const resolver = permissionRequests.get(storeId)?.shift()
  if (!resolver) {
    throw new Error(`Missing permission request for store ${storeId}`)
  }
  resolver({ success: true, message: 'ok', data })
}

const internalTaskPermission: PermissionDTO = {
  module: PermissionModule.ACCOMMODATION,
  action: PermissionAction.CREATE_INTERNAL_TASK,
}

const setup = (id: number, role: string) => {
  permissionRequests.clear()
  setActivePinia(createPinia())
  mockStoreStore.currentStore = createStore(id, role)
  return { storeStore: mockStoreStore, permissionStore: usePermissionStore() }
}

describe('permission store internal task permission', () => {
  test('owner 隐式拥有，admin 不再因全权限短路而默认拥有', async () => {
    const owner = setup(1, 'owner').permissionStore
    expect(
      owner.hasPermission(
        PermissionModule.ACCOMMODATION as never,
        PermissionAction.CREATE_INTERNAL_TASK as never
      )
    ).toBe(true)

    const admin = setup(2, 'admin').permissionStore
    expect(
      admin.hasPermission(PermissionModule.ORDER as never, PermissionAction.VIEW_ORDERS as never)
    ).toBe(true)
    expect(
      admin.hasPermission(
        PermissionModule.ACCOMMODATION as never,
        PermissionAction.CREATE_INTERNAL_TASK as never
      )
    ).toBe(false)

    const fetchPromise = admin.fetchCurrentStorePermissions()
    resolvePermissionRequest(2, [internalTaskPermission])
    await fetchPromise
    expect(
      admin.hasPermission(
        PermissionModule.ACCOMMODATION as never,
        PermissionAction.CREATE_INTERNAL_TASK as never
      )
    ).toBe(true)
  })

  test('切店先清权限并丢弃旧门店迟到响应', async () => {
    const { storeStore, permissionStore } = setup(11, 'member')
    const storeARequest = permissionStore.fetchCurrentStorePermissions()

    storeStore.currentStore = createStore(12, 'member')
    expect(permissionStore.permissions).toEqual([])
    const storeBRequest = permissionStore.fetchCurrentStorePermissions()

    resolvePermissionRequest(12, [])
    await storeBRequest
    resolvePermissionRequest(11, [internalTaskPermission])
    await storeARequest

    expect(permissionStore.loadedStoreId).toBe(12)
    expect(permissionStore.permissions).toEqual([])
    expect(
      permissionStore.hasPermission(
        PermissionModule.ACCOMMODATION as never,
        PermissionAction.CREATE_INTERNAL_TASK as never
      )
    ).toBe(false)
  })
})
