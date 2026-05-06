import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  PermissionAction,
  PermissionModule,
  type PermissionDTO,
} from '@/api/role'
import { getMyStorePermissions } from '@/api/store'
import { useStoreStore } from '@/stores/store'

export interface PermissionRequirement {
  module: PermissionModule
  action: PermissionAction
  roomTypeId?: number
}

export type PermissionMatchMode = 'all' | 'any'

const buildPermissionKey = (permission: PermissionDTO) =>
  [
    permission.module,
    permission.action,
    permission.roomTypeId ?? 0,
    permission.allRoomTypes ? 1 : 0,
  ].join('|')

export const usePermissionStore = defineStore('permission', () => {
  const storeStore = useStoreStore()

  const permissions = ref<PermissionDTO[]>([])
  const loading = ref(false)
  const loadedStoreId = ref<number | null>(null)

  const currentStoreId = computed(() => storeStore.currentStore?.id ?? null)
  const isStoreManager = computed(() => {
    const role = storeStore.currentStore?.userRole ?? ''
    return role === 'owner' || role === 'admin'
  })
  const permissionKeySet = computed(() => new Set(permissions.value.map(buildPermissionKey)))

  const clearPermissions = () => {
    permissions.value = []
    loadedStoreId.value = null
  }

  const fetchCurrentStorePermissions = async (force = false) => {
    if (!currentStoreId.value) {
      clearPermissions()
      return []
    }

    if (isStoreManager.value) {
      loadedStoreId.value = currentStoreId.value
      permissions.value = []
      return []
    }

    if (!force && loadedStoreId.value === currentStoreId.value) {
      return permissions.value
    }

    loading.value = true
    try {
      const response = await getMyStorePermissions(currentStoreId.value)
      if (!response.success) {
        throw new Error(response.message || '获取当前门店权限失败')
      }
      permissions.value = response.data || []
      loadedStoreId.value = currentStoreId.value
      return permissions.value
    } catch (error) {
      clearPermissions()
      throw error
    } finally {
      loading.value = false
    }
  }

  const hasPermission = (
    module: PermissionModule,
    action: PermissionAction,
    roomTypeId?: number
  ) => {
    if (isStoreManager.value) {
      return true
    }

    if (
      module === PermissionModule.ACCOMMODATION &&
      action === PermissionAction.VIEW_ROOM_STATUS &&
      typeof roomTypeId === 'number'
    ) {
      return permissions.value.some((permission) => {
        if (permission.module !== module || permission.action !== action) {
          return false
        }
        return Boolean(permission.allRoomTypes) || permission.roomTypeId === roomTypeId
      })
    }

    return (
      permissionKeySet.value.has([module, action, 0, 0].join('|')) ||
      permissionKeySet.value.has([module, action, 0, 1].join('|')) ||
      permissions.value.some(
        (permission) => permission.module === module && permission.action === action
      )
    )
  }

  const hasPermissions = (
    requirements: PermissionRequirement[] = [],
    matchMode: PermissionMatchMode = 'all'
  ) => {
    if (!requirements.length) {
      return true
    }

    if (matchMode === 'any') {
      return requirements.some((item) => hasPermission(item.module, item.action, item.roomTypeId))
    }

    return requirements.every((item) => hasPermission(item.module, item.action, item.roomTypeId))
  }

  return {
    permissions,
    loading,
    loadedStoreId,
    isStoreManager,
    fetchCurrentStorePermissions,
    clearPermissions,
    hasPermission,
    hasPermissions,
  }
})
