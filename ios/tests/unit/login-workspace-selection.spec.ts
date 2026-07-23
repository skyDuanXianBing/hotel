import { beforeEach, describe, expect, test } from 'vitest'
import { i18n } from '@/locales'
import type { CleanerDTO, LoginResponse, UserDTO } from '@/types/auth'
import type { StoreDTO } from '@/types/store'
import {
  normalizeAvailableLoginTargets,
  normalizePreferredLoginTarget,
  selectLoginTargetFromAuthorizedResponse,
} from '@/utils/loginSessionResolver'

const buildUser = (): UserDTO => ({
  id: 10,
  email: 'member@example.com',
  nickname: '成员',
  createdAt: '2026-07-17T00:00:00Z',
  updatedAt: '2026-07-17T00:00:00Z',
})

const buildStore = (): StoreDTO => ({
  id: 2,
  name: '测试门店',
  phone: '+86 10000000000',
  type: 'hotel',
  timezone: 'Asia/Shanghai',
  manager: '店长',
  ownerEmail: 'owner@example.com',
  address: '测试地址',
  city: '上海',
  state: '上海',
  country: 'China',
  currency: 'CNY',
  userRole: 'member',
  createdAt: '2026-07-17T00:00:00Z',
  updatedAt: '2026-07-17T00:00:00Z',
})

const buildCleaner = (storeId: number): CleanerDTO => ({
  id: 20,
  userId: 10,
  storeId,
  name: '保洁员',
  email: 'cleaner@example.com',
  isActive: true,
  createdAt: '2026-07-17T00:00:00Z',
  updatedAt: '2026-07-17T00:00:00Z',
})

const buildMultiWorkspaceResponse = (): LoginResponse => {
  const store = buildStore()
  const cleaner = buildCleaner(store.id)

  return {
    token: 'token',
    user: buildUser(),
    stores: [store],
    loginTarget: 'PMS',
    availableLoginTargets: ['PMS', 'CLEANER', 'PMS'],
    cleanerContexts: [
      {
        cleanerId: cleaner.id,
        storeId: store.id,
        cleaner,
        store,
      },
    ],
  }
}

describe('login workspace selection', () => {
  beforeEach(() => {
    i18n.global.locale.value = 'zh-CN'
  })

  test('normalizes route workspace intent and ignores unknown values', () => {
    expect(normalizePreferredLoginTarget(' cleaner ')).toBe('CLEANER')
    expect(normalizePreferredLoginTarget('PMS')).toBe('PMS')
    expect(normalizePreferredLoginTarget('admin')).toBeUndefined()
  })

  test('deduplicates the workspaces returned by the backend', () => {
    const response = buildMultiWorkspaceResponse()

    expect(normalizeAvailableLoginTargets(response)).toEqual(['PMS', 'CLEANER'])
  })

  test('selects PMS from an already authorized code-login response', () => {
    const response = buildMultiWorkspaceResponse()
    const selected = selectLoginTargetFromAuthorizedResponse(
      {
        ...response,
        cleaner: buildCleaner(2),
        currentStore: buildStore(),
        targetStoreId: 2,
      },
      'PMS',
    )

    expect(selected.loginTarget).toBe('PMS')
    expect(selected.cleaner).toBeNull()
    expect(selected.currentStore).toBeNull()
    expect(selected.targetStoreId).toBeNull()
  })

  test('selects CLEANER using the matching cleaner context', () => {
    const response = buildMultiWorkspaceResponse()
    const selected = selectLoginTargetFromAuthorizedResponse(response, 'CLEANER')

    expect(selected.loginTarget).toBe('CLEANER')
    expect(selected.cleaner?.id).toBe(20)
    expect(selected.currentStore?.id).toBe(2)
    expect(selected.targetStoreId).toBe(2)
  })

  test('rejects unavailable or incomplete cleaner workspaces', () => {
    const response = buildMultiWorkspaceResponse()

    expect(() =>
      selectLoginTargetFromAuthorizedResponse(
        {
          ...response,
          availableLoginTargets: ['PMS'],
        },
        'CLEANER',
      ),
    ).toThrow('当前账号无法进入所选工作台')

    expect(() =>
      selectLoginTargetFromAuthorizedResponse(
        {
          ...response,
          cleanerContexts: [],
        },
        'CLEANER',
      ),
    ).toThrow('登录响应缺少可用的保洁工作台信息')
  })

  test('localizes workspace validation errors using the current app language', () => {
    i18n.global.locale.value = 'en'
    const response = buildMultiWorkspaceResponse()

    expect(() =>
      selectLoginTargetFromAuthorizedResponse(
        {
          ...response,
          availableLoginTargets: ['PMS'],
        },
        'CLEANER',
      ),
    ).toThrow('This account cannot enter the selected workspace')
  })
})
