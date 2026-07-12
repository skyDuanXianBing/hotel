export const CLEANER_TOKEN_KEY = 'cleanerToken'
export const CLEANER_USER_KEY = 'cleanerUser'
export const CLEANER_STORE_KEY = 'cleanerCurrentStore'
export const CLEANER_CONTEXTS_KEY = 'cleanerContexts'
export const AVAILABLE_LOGIN_TARGETS_KEY = 'availableLoginTargets'
export const PMS_TOKEN_KEY = 'token'
export const PMS_USER_KEY = 'user'
export const PMS_STORES_KEY = 'stores'
export const PMS_CURRENT_STORE_KEY = 'currentStore'

export type CachedLoginSessionTarget = 'CLEANER' | 'PMS' | null
export type AvailableLoginTarget = 'CLEANER' | 'PMS'

export const readAvailableLoginTargets = (): AvailableLoginTarget[] => {
  const raw = localStorage.getItem(AVAILABLE_LOGIN_TARGETS_KEY)
  if (!raw) return []
  try {
    const values = JSON.parse(raw) as unknown[]
    if (!Array.isArray(values)) return []
    return values.filter(
      (value): value is AvailableLoginTarget => value === 'PMS' || value === 'CLEANER',
    )
  } catch {
    return []
  }
}

export const saveAvailableLoginTargets = (targets: AvailableLoginTarget[]): void => {
  const normalized = Array.from(new Set(targets.filter((target) => target === 'PMS' || target === 'CLEANER')))
  localStorage.setItem(AVAILABLE_LOGIN_TARGETS_KEY, JSON.stringify(normalized))
}

export interface CleanerSessionUser {
  userId: number
  cleanerId: number
  email: string
  nickname: string
  avatar?: string
  gender?: 'male' | 'female' | 'private'
  createdAt?: string
  updatedAt?: string
  isCleaner: true
}

export interface CleanerSessionStore {
  id: number
  name?: string
  [key: string]: unknown
}

export interface CleanerSessionContext {
  cleanerId: number
  storeId: number
  cleaner: {
    id: number
    userId: number
    storeId: number
    name: string
    email: string
    isActive: boolean
    createdAt?: string
    updatedAt?: string
  }
  store: CleanerSessionStore
}

export const readCleanerContexts = (): CleanerSessionContext[] => {
  const raw = localStorage.getItem(CLEANER_CONTEXTS_KEY)
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as CleanerSessionContext[]
    if (!Array.isArray(parsed)) return []
    return parsed.filter(
      (context) =>
        typeof context.cleanerId === 'number' &&
        typeof context.storeId === 'number' &&
        context.cleaner?.id === context.cleanerId &&
        context.store?.id === context.storeId,
    )
  } catch {
    return []
  }
}

export const getCleanerToken = (): string => {
  return localStorage.getItem(CLEANER_TOKEN_KEY) || ''
}

export const readCleanerUser = (): CleanerSessionUser | null => {
  const raw = localStorage.getItem(CLEANER_USER_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as Partial<CleanerSessionUser>
    if (
      typeof parsed.userId !== 'number' ||
      typeof parsed.cleanerId !== 'number' ||
      parsed.isCleaner !== true
    ) {
      clearCleanerSession()
      return null
    }
    return parsed as CleanerSessionUser
  } catch {
    clearCleanerSession()
    return null
  }
}

export const readCleanerStoreId = (): number | null => {
  return readCleanerStore()?.id ?? null
}

export const readCleanerStore = (): CleanerSessionStore | null => {
  const raw = localStorage.getItem(CLEANER_STORE_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as Partial<CleanerSessionStore>
    if (typeof parsed.id !== 'number') {
      localStorage.removeItem(CLEANER_STORE_KEY)
      return null
    }
    return parsed as CleanerSessionStore
  } catch {
    localStorage.removeItem(CLEANER_STORE_KEY)
    return null
  }
}

export const saveCleanerSession = (
  token: string,
  user: CleanerSessionUser,
  store?: number | CleanerSessionStore,
  contexts: CleanerSessionContext[] = [],
): void => {
  localStorage.setItem(CLEANER_TOKEN_KEY, token)
  localStorage.setItem(CLEANER_USER_KEY, JSON.stringify(user))
  if (typeof store === 'number') {
    localStorage.setItem(CLEANER_STORE_KEY, JSON.stringify({ id: store }))
  } else if (store?.id) {
    localStorage.setItem(CLEANER_STORE_KEY, JSON.stringify(store))
  } else {
    localStorage.removeItem(CLEANER_STORE_KEY)
  }
  localStorage.setItem(CLEANER_CONTEXTS_KEY, JSON.stringify(contexts))
}

export const switchCleanerContext = (context: CleanerSessionContext): CleanerSessionUser => {
  const currentUser = readCleanerUser()
  if (!currentUser || context.cleaner.userId !== currentUser.userId) {
    throw new Error('保洁门店上下文无效，请重新登录')
  }
  const nextUser: CleanerSessionUser = {
    ...currentUser,
    cleanerId: context.cleanerId,
    email: context.cleaner.email,
    nickname: context.cleaner.name || context.cleaner.email,
  }
  localStorage.setItem(CLEANER_USER_KEY, JSON.stringify(nextUser))
  localStorage.setItem(CLEANER_STORE_KEY, JSON.stringify(context.store))
  return nextUser
}

export const clearCleanerSession = (): void => {
  localStorage.removeItem(CLEANER_TOKEN_KEY)
  localStorage.removeItem(CLEANER_USER_KEY)
  localStorage.removeItem(CLEANER_STORE_KEY)
  localStorage.removeItem(CLEANER_CONTEXTS_KEY)
}

export const hasCompleteCleanerSession = (): boolean => {
  const token = getCleanerToken()
  const user = readCleanerUser()
  const storeId = readCleanerStoreId()

  if (token && user && storeId) {
    return true
  }

  if (token || user || storeId) {
    clearCleanerSession()
  }

  return false
}

export const resolveCachedLoginSessionTarget = (): CachedLoginSessionTarget => {
  if (hasCompleteCleanerSession()) {
    return 'CLEANER'
  }

  if (localStorage.getItem(PMS_TOKEN_KEY)) {
    return 'PMS'
  }

  return null
}

export const clearPmsSessionStorage = (): void => {
  localStorage.removeItem(PMS_TOKEN_KEY)
  localStorage.removeItem(PMS_USER_KEY)
  localStorage.removeItem(PMS_STORES_KEY)
  localStorage.removeItem(PMS_CURRENT_STORE_KEY)
}

export const clearAllLocalSessions = (): void => {
  clearPmsSessionStorage()
  clearCleanerSession()
  localStorage.removeItem(AVAILABLE_LOGIN_TARGETS_KEY)
}
