import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getStoredToken, setStoredToken } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')

  const hydrate = () => {
    token.value = getStoredToken()
  }

  const setToken = (value: string | null) => {
    token.value = value ?? ''
    setStoredToken(value)
  }

  const clearToken = () => {
    setToken(null)
  }

  const isAuthenticated = computed(() => Boolean(token.value))

  hydrate()

  return {
    token,
    isAuthenticated,
    hydrate,
    setToken,
    clearToken,
  }
})
