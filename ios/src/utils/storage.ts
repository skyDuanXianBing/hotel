import type { UserDTO } from '@/types/auth'
import type { StoreDTO } from '@/types/store'

export const TOKEN_KEY = 'token'
export const USER_KEY = 'user'
export const CURRENT_STORE_KEY = 'currentStore'
export const STORES_KEY = 'stores'

const getBrowserStorage = () => {
  if (typeof window === 'undefined') {
    return null
  }

  return window.localStorage
}

export const readStoredValue = (key: string) => {
  const storage = getBrowserStorage()

  if (!storage) {
    return ''
  }

  return storage.getItem(key) ?? ''
}

export const writeStoredValue = (key: string, value: string | null) => {
  const storage = getBrowserStorage()

  if (!storage) {
    return
  }

  if (!value) {
    storage.removeItem(key)
    return
  }

  storage.setItem(key, value)
}

export const removeStoredValue = (key: string) => {
  const storage = getBrowserStorage()

  if (!storage) {
    return
  }

  storage.removeItem(key)
}

export const readStoredJson = <T>(key: string): T | null => {
  const rawValue = readStoredValue(key)

  if (!rawValue) {
    return null
  }

  try {
    return JSON.parse(rawValue) as T
  } catch {
    removeStoredValue(key)
    return null
  }
}

export const writeStoredJson = <T>(key: string, value: T | null) => {
  if (!value) {
    removeStoredValue(key)
    return
  }

  writeStoredValue(key, JSON.stringify(value))
}

export const getStoredToken = () => readStoredValue(TOKEN_KEY)

export const setStoredToken = (token: string | null) => {
  writeStoredValue(TOKEN_KEY, token)
}

export const getStoredUser = () => readStoredJson<UserDTO>(USER_KEY)

export const getStoredCurrentStore = () => readStoredJson<StoreDTO>(CURRENT_STORE_KEY)

export const getStoredStores = () => {
  return readStoredJson<StoreDTO[]>(STORES_KEY) ?? []
}

export const getStoredCurrentStoreId = () => {
  const store = getStoredCurrentStore()

  if (!store?.id) {
    return ''
  }

  return store.id.toString()
}

export const hasStoredToken = () => Boolean(getStoredToken())

export const hasStoredCurrentStore = () => Boolean(getStoredCurrentStoreId())

export const clearAuthStorage = () => {
  removeStoredValue(TOKEN_KEY)
  removeStoredValue(USER_KEY)
}

export const clearStoreStorage = () => {
  removeStoredValue(CURRENT_STORE_KEY)
  removeStoredValue(STORES_KEY)
}

export const clearSessionStorage = () => {
  clearAuthStorage()
  clearStoreStorage()
}
