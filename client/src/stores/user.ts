import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  changePassword as changePasswordApi,
  getCurrentUser,
  logout as logoutApi,
  updateProfile as updateProfileApi,
  type ChangePasswordRequest,
  type UpdateProfileRequest,
  type UserDTO,
} from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const currentUser = ref<UserDTO | null>(null)
  const loading = ref(false)

  const cachedUser = localStorage.getItem('user')
  if (cachedUser) {
    try {
      currentUser.value = JSON.parse(cachedUser) as UserDTO
    } catch {
      localStorage.removeItem('user')
    }
  }

  const setUser = (user: UserDTO | null) => {
    currentUser.value = user
    if (user) {
      localStorage.setItem('user', JSON.stringify(user))
    } else {
      localStorage.removeItem('user')
    }
  }

  const fetchCurrentUser = async (force = false) => {
    const token = localStorage.getItem('token')
    if (!token) {
      setUser(null)
      return null
    }

    if (!force && currentUser.value) {
      return currentUser.value
    }

    loading.value = true
    try {
      const response = (await getCurrentUser()) as any
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

  const updateProfile = async (payload: UpdateProfileRequest) => {
    const response = (await updateProfileApi(payload)) as any
    if (!response.success || !response.data) {
      throw new Error(response.message || '更新个人资料失败')
    }
    setUser(response.data)
    return response.data
  }

  const changePassword = async (payload: ChangePasswordRequest) => {
    const response = (await changePasswordApi(payload)) as any
    if (!response.success) {
      throw new Error(response.message || '修改密码失败')
    }
    return response.message
  }

  const logout = async () => {
    try {
      await logoutApi()
    } catch {
      // 忽略登出接口异常，依然清理本地状态
    } finally {
      localStorage.removeItem('token')
      setUser(null)
    }
  }

  return {
    currentUser,
    loading,
    fetchCurrentUser,
    updateProfile,
    changePassword,
    logout,
    setUser,
  }
})
