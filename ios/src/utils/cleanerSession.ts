import type { CleanerSessionUser } from '@/types/auth'
import {
  readStoredJson,
  readStoredValue,
  removeStoredValue,
  writeStoredJson,
  writeStoredValue,
} from '@/utils/storage'

export const CLEANER_TOKEN_KEY = 'cleanerToken'
export const CLEANER_USER_KEY = 'cleanerUser'
export const CLEANER_STORE_KEY = 'cleanerCurrentStore'

interface CleanerStoreSession {
  id: number
}

export const getCleanerToken = () => {
  return readStoredValue(CLEANER_TOKEN_KEY)
}

export const hasCleanerToken = () => {
  return Boolean(getCleanerToken())
}

export const readCleanerUser = () => {
  return readStoredJson<CleanerSessionUser>(CLEANER_USER_KEY)
}

export const readCleanerStore = () => {
  return readStoredJson<CleanerStoreSession>(CLEANER_STORE_KEY)
}

export const readCleanerStoreId = () => {
  const cleanerStore = readCleanerStore()

  if (!cleanerStore?.id) {
    return ''
  }

  return String(cleanerStore.id)
}

export const hasCleanerStore = () => {
  return Boolean(readCleanerStoreId())
}

export const saveCleanerSession = (
  token: string,
  user: CleanerSessionUser,
  storeId?: number | null,
) => {
  writeStoredValue(CLEANER_TOKEN_KEY, token)
  writeStoredJson(CLEANER_USER_KEY, user)

  if (storeId) {
    writeStoredJson(CLEANER_STORE_KEY, { id: storeId })
    return
  }

  removeStoredValue(CLEANER_STORE_KEY)
}

export const clearCleanerSession = () => {
  removeStoredValue(CLEANER_TOKEN_KEY)
  removeStoredValue(CLEANER_USER_KEY)
  removeStoredValue(CLEANER_STORE_KEY)
}
