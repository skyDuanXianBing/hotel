import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getUserStores,
  getStoreById,
  createStore as createStoreApi,
  updateStore as updateStoreApi,
  addStoreMember,
  removeStoreMember,
  getStoreMembers,
  type StoreDTO,
  type StoreRequest,
  type AddStoreMemberRequest,
  type StoreMember,
} from '@/api/store'

export const useStoreStore = defineStore('store', () => {
  // 当前选中的门店
  const currentStore = ref<StoreDTO | null>(null)

  // 用户的所有门店列表
  const stores = ref<StoreDTO[]>([])

  // 当前门店的成员列表
  const members = ref<StoreMember[]>([])

  // 加载状态
  const loading = ref(false)

  // 从 localStorage 恢复当前门店
  const cachedStore = localStorage.getItem('currentStore')
  if (cachedStore) {
    try {
      currentStore.value = JSON.parse(cachedStore) as StoreDTO
    } catch {
      localStorage.removeItem('currentStore')
    }
  }

  // 从 localStorage 恢复门店列表
  const cachedStores = localStorage.getItem('stores')
  if (cachedStores) {
    try {
      stores.value = JSON.parse(cachedStores) as StoreDTO[]
    } catch {
      localStorage.removeItem('stores')
    }
  }

  /**
   * 设置当前门店
   */
  const setCurrentStore = (store: StoreDTO | null) => {
    currentStore.value = store
    if (store) {
      localStorage.setItem('currentStore', JSON.stringify(store))
    } else {
      localStorage.removeItem('currentStore')
    }
  }

  /**
   * 设置门店列表
   */
  const setStores = (storeList: StoreDTO[]) => {
    stores.value = storeList
    localStorage.setItem('stores', JSON.stringify(storeList))
  }

  /**
   * 获取用户的所有门店
   */
  const fetchUserStores = async (force = false) => {
    if (!force && stores.value.length > 0) {
      return stores.value
    }

    loading.value = true
    try {
      const response = await getUserStores()
      if (!response.success || !response.data) {
        throw new Error(response.message || '获取门店列表失败')
      }
      setStores(response.data)
      return response.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取门店详情
   */
  const fetchStoreById = async (storeId: number) => {
    loading.value = true
    try {
      const response = await getStoreById(storeId)
      if (!response.success || !response.data) {
        throw new Error(response.message || '获取门店详情失败')
      }
      return response.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 创建门店
   */
  const createStore = async (data: StoreRequest) => {
    loading.value = true
    try {
      const response = await createStoreApi(data)
      if (!response.success || !response.data) {
        throw new Error(response.message || '创建门店失败')
      }

      // 将新建的门店添加到列表
      const newStore = response.data
      setStores([...stores.value, newStore])

      // 设置为当前门店
      setCurrentStore(newStore)

      return newStore
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新门店信息
   */
  const updateStore = async (storeId: number, data: StoreRequest) => {
    loading.value = true
    try {
      const response = await updateStoreApi(storeId, data)
      if (!response.success || !response.data) {
        throw new Error(response.message || '更新门店失败')
      }

      const updatedStore = response.data

      // 更新门店列表中的数据
      const index = stores.value.findIndex((s) => s.id === storeId)
      if (index !== -1) {
        stores.value[index] = updatedStore
        setStores([...stores.value])
      }

      // 如果是当前门店,也更新当前门店
      if (currentStore.value?.id === storeId) {
        setCurrentStore(updatedStore)
      }

      return updatedStore
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 邀请成员
   */
  const inviteMember = async (storeId: number, data: AddStoreMemberRequest) => {
    loading.value = true
    try {
      const response = await addStoreMember(storeId, data)
      if (!response.success) {
        throw new Error(response.message || '邀请成员失败')
      }
      return response.message
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 移除成员
   */
  const removeMember = async (storeId: number, userId: number) => {
    loading.value = true
    try {
      const response = await removeStoreMember(storeId, userId)
      if (!response.success) {
        throw new Error(response.message || '移除成员失败')
      }

      // 从成员列表中移除
      members.value = members.value.filter((m) => m.user.id !== userId)

      return response.message
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取门店成员列表
   */
  const fetchStoreMembers = async (storeId: number, force = false) => {
    if (!force && members.value.length > 0) {
      return members.value
    }

    loading.value = true
    try {
      const response = await getStoreMembers(storeId)
      if (!response.success || !response.data) {
        throw new Error(response.message || '获取门店成员失败')
      }
      members.value = response.data
      return response.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 清空门店数据
   */
  const clearStoreData = () => {
    setCurrentStore(null)
    setStores([])
    members.value = []
  }

  // 计算属性:是否有门店
  const hasStores = computed(() => stores.value.length > 0)

  // 计算属性:当前用户在当前门店的角色
  const currentUserRole = computed(() => currentStore.value?.userRole || '')

  // 计算属性:是否是门店所有者
  const isOwner = computed(() => currentUserRole.value === 'owner')

  // 计算属性:是否有管理权限(owner 或 admin)
  const hasAdminPermission = computed(
    () => currentUserRole.value === 'owner' || currentUserRole.value === 'admin'
  )

  return {
    // 状态
    currentStore,
    stores,
    members,
    loading,

    // 计算属性
    hasStores,
    currentUserRole,
    isOwner,
    hasAdminPermission,

    // 方法
    setCurrentStore,
    setStores,
    fetchUserStores,
    fetchStoreById,
    createStore,
    updateStore,
    inviteMember,
    removeMember,
    fetchStoreMembers,
    clearStoreData,
  }
})
