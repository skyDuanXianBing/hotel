import { beforeEach, describe, expect, test } from 'bun:test'

// bun test 环境无 localStorage，用内存实现替代
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

const storage = new MemoryStorage()
// 其他测试文件以 defineProperty（writable:false）定义 localStorage，必须用同等方式覆盖
Object.defineProperty(globalThis, 'localStorage', {
  value: storage,
  configurable: true,
  writable: true,
})

const {
  ADMIN_LOGIN_PATH,
  clearAdminSession,
  getAdminToken,
  hasCompleteAdminSession,
  isAdminWorkspacePath,
  readAdminProfile,
  saveAdminSession,
} = await import('../adminSession')

beforeEach(() => {
  storage.clear()
})

describe('adminSession', () => {
  test('保存/读取/清除完整会话', () => {
    expect(hasCompleteAdminSession()).toBe(false)

    saveAdminSession('token-abc', { username: 'admin', role: 'SUPER' })
    expect(getAdminToken()).toBe('token-abc')
    expect(readAdminProfile()).toEqual({ username: 'admin', role: 'SUPER' })
    expect(hasCompleteAdminSession()).toBe(true)

    clearAdminSession()
    expect(getAdminToken()).toBe('')
    expect(readAdminProfile()).toBeNull()
    expect(hasCompleteAdminSession()).toBe(false)
  })

  test('残缺会话（仅 token 或仅 profile）被判定为不完整并自动清理', () => {
    storage.setItem('adminToken', 'token-only')
    expect(hasCompleteAdminSession()).toBe(false)
    expect(getAdminToken()).toBe('')

    storage.setItem('adminProfile', JSON.stringify({ username: 'admin', role: 'SUPER' }))
    expect(hasCompleteAdminSession()).toBe(false)
    expect(storage.getItem('adminProfile')).toBeNull()
  })

  test('损坏的 profile JSON 读取为 null', () => {
    storage.setItem('adminProfile', '{broken')
    expect(readAdminProfile()).toBeNull()
    expect(storage.getItem('adminProfile')).toBeNull()
  })

  test('工作区路径判定：/admin/login 公开，其余 /admin/* 为工作区', () => {
    expect(isAdminWorkspacePath(ADMIN_LOGIN_PATH)).toBe(false)
    expect(isAdminWorkspacePath('/admin')).toBe(true)
    expect(isAdminWorkspacePath('/admin/dashboard')).toBe(true)
    expect(isAdminWorkspacePath('/admin/packages')).toBe(true)
    expect(isAdminWorkspacePath('/login')).toBe(false)
    expect(isAdminWorkspacePath('/settings/package-settings')).toBe(false)
    expect(isAdminWorkspacePath('/administrator')).toBe(false)
  })
})
