import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, logout as logoutApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useStoreStore } from '@/stores/store'
import type { UserDTO } from '@/types/auth'
import { getStoredUser, writeStoredJson, USER_KEY } from '@/utils/storage'

export const useUserStore = defineStore('user', () => {
  const currentUser = ref<UserDTO | null>(null)
  const loading = ref(false)

  const hydrate = () => {
    currentUser.value = getStoredUser()
  }

  const setUser = (user: UserDTO | null) => {
    currentUser.value = user
    writeStoredJson(USER_KEY, user)
  }

  const fetchCurrentUser = async (force = false) => {
    const authStore = useAuthStore()

    if (!authStore.token) {
      setUser(null)
      return null
    }

    if (!force && currentUser.value) {
      return currentUser.value
    }

    loading.value = true

    try {
      const response = await getCurrentUser()
      if (!response.success || !response.data) {
        throw new Error(response.message || '获取用户信息失败')
      }

      setUser(response.data)
      return response.data
    } catch (error) {
      setUser(null)
      throw error
    } finally {
      loading.value = false
    }
  }

  const logout = async () => {
    const authStore = useAuthStore()
    const storeStore = useStoreStore()

    try {
      await logoutApi()
    } catch {
      // 忽略登出异常，保持本地状态清理
    } finally {
      authStore.clearToken()
      setUser(null)
      storeStore.clearStoreData()
    }
  }

  hydrate()

  return {
    currentUser,
    loading,
    hydrate,
    setUser,
    fetchCurrentUser,
    logout,
  }
})
