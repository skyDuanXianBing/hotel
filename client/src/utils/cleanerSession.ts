export const CLEANER_TOKEN_KEY = 'cleanerToken'
export const CLEANER_USER_KEY = 'cleanerUser'
export const CLEANER_STORE_KEY = 'cleanerCurrentStore'
export const PMS_TOKEN_KEY = 'token'
export const PMS_USER_KEY = 'user'
export const PMS_STORES_KEY = 'stores'
export const PMS_CURRENT_STORE_KEY = 'currentStore'

export type CachedLoginSessionTarget = 'CLEANER' | 'PMS' | null

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
  store?: number | CleanerSessionStore
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
}

export const clearCleanerSession = (): void => {
  localStorage.removeItem(CLEANER_TOKEN_KEY)
  localStorage.removeItem(CLEANER_USER_KEY)
  localStorage.removeItem(CLEANER_STORE_KEY)
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
}
