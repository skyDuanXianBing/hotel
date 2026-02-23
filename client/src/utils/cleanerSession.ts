export const CLEANER_TOKEN_KEY = 'cleanerToken'
export const CLEANER_USER_KEY = 'cleanerUser'
export const CLEANER_STORE_KEY = 'cleanerCurrentStore'

export interface CleanerSessionUser {
  id: number
  email: string
  nickname: string
  avatar?: string
  gender?: 'male' | 'female' | 'private'
  createdAt?: string
  updatedAt?: string
  isCleaner: true
}

export const getCleanerToken = (): string => {
  return localStorage.getItem(CLEANER_TOKEN_KEY) || ''
}

export const readCleanerUser = (): CleanerSessionUser | null => {
  const raw = localStorage.getItem(CLEANER_USER_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as CleanerSessionUser
  } catch {
    localStorage.removeItem(CLEANER_USER_KEY)
    return null
  }
}

export const readCleanerStoreId = (): number | null => {
  const raw = localStorage.getItem(CLEANER_STORE_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as { id?: number }
    return parsed?.id ?? null
  } catch {
    localStorage.removeItem(CLEANER_STORE_KEY)
    return null
  }
}

export const saveCleanerSession = (
  token: string,
  user: CleanerSessionUser,
  storeId?: number
): void => {
  localStorage.setItem(CLEANER_TOKEN_KEY, token)
  localStorage.setItem(CLEANER_USER_KEY, JSON.stringify(user))
  if (storeId) {
    localStorage.setItem(CLEANER_STORE_KEY, JSON.stringify({ id: storeId }))
  } else {
    localStorage.removeItem(CLEANER_STORE_KEY)
  }
}

export const clearCleanerSession = (): void => {
  localStorage.removeItem(CLEANER_TOKEN_KEY)
  localStorage.removeItem(CLEANER_USER_KEY)
  localStorage.removeItem(CLEANER_STORE_KEY)
}
