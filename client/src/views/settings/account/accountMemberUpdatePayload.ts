import type { PermissionDTO } from '@/api/role'
import type { UpdateStoreMemberPermissionRequest } from '@/api/store'

export interface AccountEditSnapshot {
  name: string
  roleIds: number[]
  extraPermissions: PermissionDTO[]
}

const ACCOMMODATION_MODULE = 'ACCOMMODATION' as PermissionDTO['module']
const VIEW_ROOM_STATUS_ACTION = 'VIEW_ROOM_STATUS' as PermissionDTO['action']

function normalizeRoleIds(roleIds: number[]) {
  const uniqueIds: number[] = []
  roleIds.forEach((roleId) => {
    if (!uniqueIds.includes(roleId)) {
      uniqueIds.push(roleId)
    }
  })
  uniqueIds.sort((left, right) => left - right)
  return uniqueIds
}

function areNumberListsEqual(left: number[], right: number[]) {
  if (left.length !== right.length) {
    return false
  }

  for (let index = 0; index < left.length; index += 1) {
    if (left[index] !== right[index]) {
      return false
    }
  }

  return true
}

function buildPermissionCompareKey(permission: PermissionDTO) {
  if (!permission.module || !permission.action) {
    return ''
  }

  const isRoomStatusPermission =
    permission.module === ACCOMMODATION_MODULE &&
    permission.action === VIEW_ROOM_STATUS_ACTION
  if (isRoomStatusPermission) {
    if (permission.allRoomTypes || !permission.roomTypeId || permission.roomTypeId === 0) {
      return `${permission.module}-${permission.action}-0-1`
    }
    return `${permission.module}-${permission.action}-${permission.roomTypeId}-0`
  }

  return `${permission.module}-${permission.action}-0-0`
}

function normalizePermissionKeys(permissions: PermissionDTO[]) {
  const keys: string[] = []
  permissions.forEach((permission) => {
    const key = buildPermissionCompareKey(permission)
    if (key && !keys.includes(key)) {
      keys.push(key)
    }
  })
  keys.sort()
  return keys
}

function arePermissionListsEqual(left: PermissionDTO[], right: PermissionDTO[]) {
  const leftKeys = normalizePermissionKeys(left)
  const rightKeys = normalizePermissionKeys(right)

  if (leftKeys.length !== rightKeys.length) {
    return false
  }

  for (let index = 0; index < leftKeys.length; index += 1) {
    if (leftKeys[index] !== rightKeys[index]) {
      return false
    }
  }

  return true
}

export function buildMemberUpdatePayload(
  snapshot: AccountEditSnapshot | null,
  name: string,
  roleIds: number[],
  extraPermissions: PermissionDTO[]
): UpdateStoreMemberPermissionRequest {
  const payload: UpdateStoreMemberPermissionRequest = {}

  if (!snapshot || name !== snapshot.name) {
    payload.name = name
  }

  const normalizedRoleIds = normalizeRoleIds(roleIds)
  const snapshotRoleIds = snapshot ? normalizeRoleIds(snapshot.roleIds) : []
  if (!snapshot || !areNumberListsEqual(normalizedRoleIds, snapshotRoleIds)) {
    payload.roleIds = roleIds
  }

  const snapshotExtraPermissions = snapshot ? snapshot.extraPermissions : []
  if (!snapshot || !arePermissionListsEqual(extraPermissions, snapshotExtraPermissions)) {
    payload.extraPermissions = extraPermissions
  }

  return payload
}
