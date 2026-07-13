import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { PermissionAction, PermissionModule, type PermissionDTO } from '@/api/role'
import { getMyStorePermissions } from '@/api/store'
import { useStoreStore } from '@/stores/store'
import { i18n } from '@/locales'

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

const translate = (key: string) => i18n.global.t(key)

export const usePermissionStore = defineStore('permission', () => {
  const storeStore = useStoreStore()

  const permissions = ref<PermissionDTO[]>([])
  const loading = ref(false)
  const loadedStoreId = ref<number | null>(null)
  let requestSeq = 0

  const currentStoreId = computed(() => storeStore.currentStore?.id ?? null)
  const isStoreManager = computed(() => {
    const role = storeStore.currentStore?.userRole ?? ''
    return role === 'owner' || role === 'admin'
  })
  const isStoreOwner = computed(() => storeStore.currentStore?.userRole === 'owner')
  const permissionKeySet = computed(() => new Set(permissions.value.map(buildPermissionKey)))

  const clearPermissions = () => {
    requestSeq += 1
    permissions.value = []
    loadedStoreId.value = null
    loading.value = false
  }

  const fetchCurrentStorePermissions = async (force = false) => {
    if (!currentStoreId.value) {
      clearPermissions()
      return []
    }

    const requestedStoreId = currentStoreId.value

    if (isStoreOwner.value) {
      clearPermissions()
      loadedStoreId.value = requestedStoreId
      permissions.value = []
      return []
    }

    if (!force && loadedStoreId.value === requestedStoreId) {
      return permissions.value
    }

    const seq = ++requestSeq
    permissions.value = []
    loadedStoreId.value = null
    loading.value = true
    try {
      const response = await getMyStorePermissions(requestedStoreId)
      if (seq !== requestSeq || currentStoreId.value !== requestedStoreId) {
        return permissions.value
      }
      if (!response.success) {
        throw new Error(
          response.message || translate('stage6.common.messages.fetchStorePermissionsFailed')
        )
      }
      permissions.value = response.data || []
      loadedStoreId.value = requestedStoreId
      return permissions.value
    } catch (error) {
      if (seq !== requestSeq || currentStoreId.value !== requestedStoreId) {
        return permissions.value
      }
      permissions.value = []
      loadedStoreId.value = null
      throw error
    } finally {
      if (seq === requestSeq) {
        loading.value = false
      }
    }
  }

  const hasPermission = (
    module: PermissionModule,
    action: PermissionAction,
    roomTypeId?: number
  ) => {
    if (
      module === PermissionModule.ACCOMMODATION &&
      action === PermissionAction.CREATE_INTERNAL_TASK
    ) {
      return (
        isStoreOwner.value ||
        permissions.value.some(
          (permission) => permission.module === module && permission.action === action
        )
      )
    }

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

  watch(
    currentStoreId,
    (storeId, previousStoreId) => {
      if (storeId !== previousStoreId) {
        clearPermissions()
      }
    },
    { flush: 'sync' }
  )

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
    isStoreOwner,
    fetchCurrentStorePermissions,
    clearPermissions,
    hasPermission,
    hasPermissions,
  }
})
