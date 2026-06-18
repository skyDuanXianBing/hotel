import { PermissionAction, PermissionModule, type PermissionDTO } from '@/api/role'
import type { ApiResponse } from '@/types/api'
import request from '@/utils/request'
import { getStoredCurrentStore } from '@/utils/storage'

export interface PermissionRequirement {
  module: PermissionModule
  action: PermissionAction
  roomTypeId?: number
}

let cachedStoreId: number | null = null
let cachedPermissions: PermissionDTO[] = []

function isStoreManagerRole(role?: string) {
  return role === 'owner' || role === 'admin'
}

function getCurrentStoreId() {
  return getStoredCurrentStore()?.id ?? null
}

function hasPermissionInList(
  permissions: PermissionDTO[],
  module: PermissionModule,
  action: PermissionAction,
  roomTypeId?: number,
) {
  for (const permission of permissions) {
    if (permission.module !== module || permission.action !== action) {
      continue
    }

    if (typeof roomTypeId !== 'number') {
      return true
    }

    if (permission.allRoomTypes || permission.roomTypeId === roomTypeId) {
      return true
    }
  }

  return false
}

export function hasCurrentStoreManagerRole() {
  return isStoreManagerRole(getStoredCurrentStore()?.userRole)
}

export async function fetchCurrentStorePermissions(force = false) {
  const currentStore = getStoredCurrentStore()
  if (!currentStore?.id) {
    cachedStoreId = null
    cachedPermissions = []
    return []
  }

  if (isStoreManagerRole(currentStore.userRole)) {
    cachedStoreId = currentStore.id
    cachedPermissions = []
    return []
  }

  if (!force && cachedStoreId === currentStore.id) {
    return cachedPermissions
  }

  const response = await request<ApiResponse<PermissionDTO[]>>({
    url: `/stores/${currentStore.id}/my-permissions`,
    method: 'GET',
  })

  if (!response.success) {
    throw new Error(response.message || '加载当前门店权限失败')
  }

  cachedStoreId = currentStore.id
  cachedPermissions = response.data || []
  return cachedPermissions
}

export async function hasCurrentStorePermission(requirement: PermissionRequirement) {
  if (hasCurrentStoreManagerRole()) {
    return true
  }

  const permissions = await fetchCurrentStorePermissions()
  return hasPermissionInList(
    permissions,
    requirement.module,
    requirement.action,
    requirement.roomTypeId,
  )
}

export function hasCachedCurrentStorePermission(requirement: PermissionRequirement) {
  if (hasCurrentStoreManagerRole()) {
    return true
  }

  const currentStoreId = getCurrentStoreId()
  if (!currentStoreId || cachedStoreId !== currentStoreId) {
    return false
  }

  return hasPermissionInList(
    cachedPermissions,
    requirement.module,
    requirement.action,
    requirement.roomTypeId,
  )
}
