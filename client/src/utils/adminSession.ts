/**
 * 平台管理端（/admin/*）独立会话：复制 cleanerSession 的双 token 先例。
 * 管理员不属于任何门店，adminToken 与 PMS/cleaner 会话完全隔离，
 * 路由守卫与 adminRequest 按 /admin 路径前缀分流。
 */

export const ADMIN_TOKEN_KEY = 'adminToken'
export const ADMIN_PROFILE_KEY = 'adminProfile'

export const ADMIN_PATH_PREFIX = '/admin'
export const ADMIN_LOGIN_PATH = '/admin/login'
export const ADMIN_DASHBOARD_PATH = '/admin/dashboard'

export interface AdminProfile {
  username: string
  role: string
}

/**
 * /admin/login 是公开页，其余 /admin/* 均为管理端工作区。
 */
export const isAdminWorkspacePath = (path: string): boolean => {
  if (path === ADMIN_LOGIN_PATH) {
    return false
  }
  return path === ADMIN_PATH_PREFIX || path.startsWith(`${ADMIN_PATH_PREFIX}/`)
}

export const getAdminToken = (): string => {
  return localStorage.getItem(ADMIN_TOKEN_KEY) || ''
}

export const readAdminProfile = (): AdminProfile | null => {
  const raw = localStorage.getItem(ADMIN_PROFILE_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as Partial<AdminProfile>
    if (typeof parsed.username !== 'string' || typeof parsed.role !== 'string') {
      localStorage.removeItem(ADMIN_PROFILE_KEY)
      return null
    }
    return { username: parsed.username, role: parsed.role }
  } catch {
    localStorage.removeItem(ADMIN_PROFILE_KEY)
    return null
  }
}

export const saveAdminSession = (token: string, profile: AdminProfile): void => {
  localStorage.setItem(ADMIN_TOKEN_KEY, token)
  localStorage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(profile))
}

export const clearAdminSession = (): void => {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(ADMIN_PROFILE_KEY)
}

export const hasCompleteAdminSession = (): boolean => {
  const token = getAdminToken()
  const profile = readAdminProfile()

  if (token && profile) {
    return true
  }

  if (token || profile) {
    clearAdminSession()
  }

  return false
}
