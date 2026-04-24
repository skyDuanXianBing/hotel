import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  HOME_QUICK_ACTION_DEFAULT_VISIBLE_KEYS,
  normalizeHomeQuickActionKeys,
} from '@/constants/homeQuickActions'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import { readStoredJson, writeStoredJson } from '@/utils/storage'

const HOME_SHORTCUTS_STORAGE_PREFIX = 'homeShortcuts'

interface HomeShortcutsPreference {
  visibleKeys: string[]
}

const buildHomeShortcutsStorageKey = (userId?: number | string | null, storeId?: number | string | null) => {
  const userScope = userId ? String(userId) : 'anonymous'
  const storeScope = storeId ? String(storeId) : 'default'
  return `${HOME_SHORTCUTS_STORAGE_PREFIX}:${userScope}:${storeScope}`
}

const resolveStoredVisibleKeys = (preference: HomeShortcutsPreference | null) => {
  if (!preference || !Array.isArray(preference.visibleKeys)) {
    return [...HOME_QUICK_ACTION_DEFAULT_VISIBLE_KEYS]
  }

  return normalizeHomeQuickActionKeys(preference.visibleKeys)
}

export const useHomeShortcutsStore = defineStore('homeShortcuts', () => {
  const userStore = useUserStore()
  const storeStore = useStoreStore()
  const storageKey = ref(buildHomeShortcutsStorageKey(userStore.currentUser?.id, storeStore.currentStore?.id))
  const visibleKeys = ref<string[]>([...HOME_QUICK_ACTION_DEFAULT_VISIBLE_KEYS])

  const hydrate = () => {
    storageKey.value = buildHomeShortcutsStorageKey(userStore.currentUser?.id, storeStore.currentStore?.id)
    const storedPreference = readStoredJson<HomeShortcutsPreference>(storageKey.value)
    visibleKeys.value = resolveStoredVisibleKeys(storedPreference)
  }

  const setVisibleKeys = (keys: string[]) => {
    const nextVisibleKeys = normalizeHomeQuickActionKeys(keys)
    visibleKeys.value = nextVisibleKeys
    writeStoredJson<HomeShortcutsPreference>(storageKey.value, {
      visibleKeys: nextVisibleKeys,
    })
  }

  const resetVisibleKeys = () => {
    setVisibleKeys(HOME_QUICK_ACTION_DEFAULT_VISIBLE_KEYS)
  }

  watch([() => userStore.currentUser?.id, () => storeStore.currentStore?.id], hydrate, {
    immediate: true,
  })

  return {
    visibleKeys,
    hydrate,
    setVisibleKeys,
    resetVisibleKeys,
  }
})
