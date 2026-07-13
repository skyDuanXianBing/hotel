import { describe, expect, test } from 'bun:test'
import type { PermissionDTO } from '@/api/role'
import { buildMemberUpdatePayload, type AccountEditSnapshot } from '../accountMemberUpdatePayload'

const ACCOMMODATION_MODULE = 'ACCOMMODATION' as PermissionDTO['module']
const ORDER_MODULE = 'ORDER' as PermissionDTO['module']
const VIEW_ROOM_STATUS_ACTION = 'VIEW_ROOM_STATUS' as PermissionDTO['action']
const VIEW_ORDERS_ACTION = 'VIEW_ORDERS' as PermissionDTO['action']
const MODIFY_ORDER_ACTION = 'MODIFY_ORDER' as PermissionDTO['action']
const CREATE_INTERNAL_TASK_ACTION = 'CREATE_INTERNAL_TASK' as PermissionDTO['action']

const createPermission = (
  module: PermissionDTO['module'],
  action: PermissionDTO['action'],
  roomTypeId?: number
): PermissionDTO => {
  const permission: PermissionDTO = { module, action }
  if (roomTypeId !== undefined) {
    permission.roomTypeId = roomTypeId
  }
  return permission
}

const baseExtraPermissions = [
  createPermission(ACCOMMODATION_MODULE, VIEW_ROOM_STATUS_ACTION, 101),
  createPermission(ORDER_MODULE, VIEW_ORDERS_ACTION),
]

const createSnapshot = (): AccountEditSnapshot => ({
  name: 'Alice',
  roleIds: [3, 1],
  extraPermissions: baseExtraPermissions,
})

describe('buildMemberUpdatePayload', () => {
  test('只改姓名时只提交 name', () => {
    const payload = buildMemberUpdatePayload(
      createSnapshot(),
      'Alice Wang',
      [1, 3],
      baseExtraPermissions.slice().reverse()
    )

    expect(payload).toEqual({ name: 'Alice Wang' })
    expect(payload).not.toHaveProperty('roleIds')
    expect(payload).not.toHaveProperty('extraPermissions')
  })

  test('只改权限时提交权限字段且不提交 name', () => {
    const nextExtraPermissions = [
      createPermission(ACCOMMODATION_MODULE, VIEW_ROOM_STATUS_ACTION, 102),
      createPermission(ORDER_MODULE, MODIFY_ORDER_ACTION),
    ]

    const payload = buildMemberUpdatePayload(
      createSnapshot(),
      'Alice',
      [1, 4],
      nextExtraPermissions
    )

    expect(payload).toEqual({
      roleIds: [1, 4],
      extraPermissions: nextExtraPermissions,
    })
    expect(payload).not.toHaveProperty('name')
  })

  test('姓名和权限同时变化时一起提交变化字段', () => {
    const nextExtraPermissions = [createPermission(ORDER_MODULE, MODIFY_ORDER_ACTION)]

    const payload = buildMemberUpdatePayload(
      createSnapshot(),
      'Alice Zhang',
      [2],
      nextExtraPermissions
    )

    expect(payload).toEqual({
      name: 'Alice Zhang',
      roleIds: [2],
      extraPermissions: nextExtraPermissions,
    })
  })

  test('无变化时返回空 payload', () => {
    const payload = buildMemberUpdatePayload(
      createSnapshot(),
      'Alice',
      [1, 3, 3],
      baseExtraPermissions.slice().reverse()
    )

    expect(payload).toEqual({})
  })

  test('只改其他字段时不会误删受保护的创建任务权限', () => {
    const protectedPermission = createPermission(ACCOMMODATION_MODULE, CREATE_INTERNAL_TASK_ACTION)
    const snapshot = createSnapshot()
    snapshot.extraPermissions = [...snapshot.extraPermissions, protectedPermission]
    const nextPermissions = [
      createPermission(ORDER_MODULE, MODIFY_ORDER_ACTION),
      protectedPermission,
    ]

    const payload = buildMemberUpdatePayload(snapshot, 'Alice Wang', [3, 1], nextPermissions)

    expect(payload.name).toBe('Alice Wang')
    expect(payload.extraPermissions).toEqual(nextPermissions)
    expect(payload.extraPermissions).toContainEqual(protectedPermission)
  })
})
