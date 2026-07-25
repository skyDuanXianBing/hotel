import { PermissionAction, PermissionModule, type PermissionDTO } from '@/api/role'
import { i18n } from '@/locales'

export interface PermissionRoomScope {
  allRoomTypes: boolean
  roomTypeIds: number[]
}

export const moveArrayItem = <T>(list: T[], fromIndex: number, toIndex: number) => {
  if (fromIndex < 0 || toIndex < 0 || fromIndex >= list.length || toIndex >= list.length) {
    return [...list]
  }

  const nextList = [...list]
  const [currentItem] = nextList.splice(fromIndex, 1)
  nextList.splice(toIndex, 0, currentItem)
  return nextList
}

export const normalizeTextList = (rawValue: string) => {
  const values = rawValue
    .split(/[\n,]/)
    .map((item) => item.trim())
    .filter(Boolean)

  return [...new Set(values)]
}

export const formatTextList = (values?: string[]) => {
  if (!values || values.length === 0) {
    return ''
  }

  return values.join('\n')
}

export const parseJsonNumberList = (rawValue: string) => {
  if (!rawValue) {
    return []
  }

  try {
    const parsed = JSON.parse(rawValue) as unknown[]
    const values: number[] = []
    for (const item of parsed) {
      const value = Number(item)
      if (Number.isFinite(value)) {
        values.push(value)
      }
    }
    return values
  } catch {
    return []
  }
}

export const formatDateTimeText = (rawValue?: string) => {
  if (!rawValue) {
    return i18n.global.t('runtime.settings.unset')
  }

  const date = new Date(rawValue)
  if (Number.isNaN(date.getTime())) {
    return rawValue
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

export const formatDateText = (rawValue?: string) => {
  if (!rawValue) {
    return i18n.global.t('runtime.settings.unset')
  }

  const date = new Date(rawValue)
  if (Number.isNaN(date.getTime())) {
    return rawValue
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const normalizeOptionalNumber = (rawValue: string) => {
  const normalizedValue = rawValue.trim()
  if (!normalizedValue) {
    return undefined
  }

  const value = Number(normalizedValue)
  if (!Number.isFinite(value)) {
    return null
  }

  return value
}

export const normalizeTimeText = (rawValue: string) => {
  const matched = rawValue.trim().match(/^([01]\d|2[0-3]):([0-5]\d)(?::[0-5]\d)?$/)
  if (!matched) {
    return ''
  }

  return `${matched[1]}:${matched[2]}`
}

export const formatPermissionSummary = (count: number) => {
  if (count <= 0) {
    return i18n.global.t('runtime.settings.permissionsNone')
  }

  return i18n.global.t('runtime.settings.permissionsSelected', { count })
}

export const isRoomTypeScopedPermission = (module: string, action: string) => {
  return module === PermissionModule.ACCOMMODATION && action === PermissionAction.VIEW_ROOM_STATUS
}

export const resolveRoomScope = (permissions: PermissionDTO[]): PermissionRoomScope => {
  const roomTypeIds = new Set<number>()

  for (const permission of permissions) {
    if (!isRoomTypeScopedPermission(permission.module, permission.action)) {
      continue
    }

    if (permission.allRoomTypes || !permission.roomTypeId || permission.roomTypeId === 0) {
      return {
        allRoomTypes: true,
        roomTypeIds: [],
      }
    }

    roomTypeIds.add(permission.roomTypeId)
  }

  return {
    allRoomTypes: false,
    roomTypeIds: Array.from(roomTypeIds),
  }
}
