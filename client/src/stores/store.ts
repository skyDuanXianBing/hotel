import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getUserStores,
  getStoreById,
  createStore as createStoreApi,
  updateStore as updateStoreApi,
  deleteStore as deleteStoreApi,
  addStoreMember,
  removeStoreMember,
  getStoreMembers,
  type StoreDTO,
  type StoreRequest,
  type AddStoreMemberRequest,
  type StoreMember,
  type DeleteStoreErrorData,
} from '@/api/store'
import { i18n } from '@/locales'

const translate = (key: string) => i18n.global.t(key)

export const useStoreStore = defineStore('store', () => {
  const currentStore = ref<StoreDTO | null>(null)
  const stores = ref<StoreDTO[]>([])
  const members = ref<StoreMember[]>([])
  const loading = ref(false)

  const cachedStore = localStorage.getItem('currentStore')
  if (cachedStore) {
    try {
      currentStore.value = JSON.parse(cachedStore) as StoreDTO
    } catch {
      localStorage.removeItem('currentStore')
    }
  }

  const cachedStores = localStorage.getItem('stores')
  if (cachedStores) {
    try {
      stores.value = JSON.parse(cachedStores) as StoreDTO[]
    } catch {
      localStorage.removeItem('stores')
    }
  }

  const setCurrentStore = (store: StoreDTO | null) => {
    currentStore.value = store
    if (store) {
      localStorage.setItem('currentStore', JSON.stringify(store))
    } else {
      localStorage.removeItem('currentStore')
    }
  }

  const setStores = (storeList: StoreDTO[]) => {
    stores.value = storeList
    localStorage.setItem('stores', JSON.stringify(storeList))
  }

  const fetchUserStores = async (force = false) => {
    if (!force && stores.value.length > 0) {
      return stores.value
    }

    loading.value = true
    try {
      const response = await getUserStores()
      if (!response.success || !response.data) {
        throw new Error(response.message || translate('stage6.common.messages.fetchStoresFailed'))
      }
      setStores(response.data)
      return response.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchStoreById = async (storeId: number) => {
    loading.value = true
    try {
      const response = await getStoreById(storeId)
      if (!response.success || !response.data) {
        throw new Error(response.message || translate('stage6.common.messages.fetchStoreDetailsFailed'))
      }
      return response.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const createStore = async (data: StoreRequest) => {
    loading.value = true
    try {
      const response = await createStoreApi(data)
      if (!response.success || !response.data) {
        throw new Error(response.message || translate('stage6.common.messages.createStoreFailed'))
      }

      const newStore = response.data
      setStores([...stores.value, newStore])

      setCurrentStore(newStore)

      return { store: newStore, message: response.message }
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const updateStore = async (storeId: number, data: StoreRequest) => {
    loading.value = true
    try {
      const response = await updateStoreApi(storeId, data)
      if (!response.success || !response.data) {
        throw new Error(response.message || translate('stage6.common.messages.updateStoreFailed'))
      }

      const updatedStore = response.data

      const index = stores.value.findIndex((s) => s.id === storeId)
      if (index !== -1) {
        stores.value[index] = updatedStore
        setStores([...stores.value])
      }

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

  const deleteStore = async (storeId: number) => {
    loading.value = true
    try {
      const response = await deleteStoreApi(storeId)
      if (!response.success) {
        const err: any = new Error(response.message || translate('stage6.common.messages.deleteStoreFailed'))
        err.code = (response.data as DeleteStoreErrorData | null)?.code
        throw err
      }

      stores.value = stores.value.filter((s) => s.id !== storeId)
      setStores([...stores.value])

      if (currentStore.value?.id === storeId) {
        setCurrentStore(null)
      }

      return response.message
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const inviteMember = async (storeId: number, data: AddStoreMemberRequest) => {
    loading.value = true
    try {
      const response = await addStoreMember(storeId, data)
      if (!response.success) {
        throw new Error(response.message || translate('stage6.common.messages.inviteMemberFailed'))
      }
      return response.message
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const removeMember = async (storeId: number, userId: number) => {
    loading.value = true
    try {
      const response = await removeStoreMember(storeId, userId)
      if (!response.success) {
        throw new Error(response.message || translate('stage6.common.messages.removeMemberFailed'))
      }

      members.value = members.value.filter((m) => m.user.id !== userId)

      return response.message
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchStoreMembers = async (storeId: number, force = false) => {
    if (!force && members.value.length > 0) {
      return members.value
    }

    loading.value = true
    try {
      const response = await getStoreMembers(storeId)
      if (!response.success || !response.data) {
        throw new Error(response.message || translate('stage6.common.messages.fetchStoreMembersFailed'))
      }
      members.value = response.data
      return response.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const clearStoreData = () => {
    setCurrentStore(null)
    setStores([])
    members.value = []
  }

  const hasStores = computed(() => stores.value.length > 0)

  const currentUserRole = computed(() => currentStore.value?.userRole || '')

  const isOwner = computed(() => currentUserRole.value === 'owner')

  const hasAdminPermission = computed(
    () => currentUserRole.value === 'owner' || currentUserRole.value === 'admin'
  )

  return {
    currentStore,
    stores,
    members,
    loading,

    hasStores,
    currentUserRole,
    isOwner,
    hasAdminPermission,

    setCurrentStore,
    setStores,
    fetchUserStores,
    fetchStoreById,
    createStore,
    updateStore,
    deleteStore,
    inviteMember,
    removeMember,
    fetchStoreMembers,
    clearStoreData,
  }
})
