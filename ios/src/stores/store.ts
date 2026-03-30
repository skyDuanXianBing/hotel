import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createStore as createStoreRequest,
  deleteStore as deleteStoreRequest,
  type DeleteStoreErrorData,
  getUserStores,
} from '@/api/store'
import type { StoreDTO, StoreRequest } from '@/types/store'
import {
  clearStoreStorage,
  CURRENT_STORE_KEY,
  getStoredCurrentStore,
  getStoredStores,
  STORES_KEY,
  writeStoredJson,
} from '@/utils/storage'

const DELETE_STORE_SUCCESS_MESSAGE = '门店删除成功'

export const useStoreStore = defineStore('store', () => {
  const currentStore = ref<StoreDTO | null>(null)
  const stores = ref<StoreDTO[]>([])
  const loading = ref(false)

  const hydrate = () => {
    currentStore.value = getStoredCurrentStore()
    stores.value = getStoredStores()
  }

  const setCurrentStore = (store: StoreDTO | null) => {
    currentStore.value = store
    writeStoredJson(CURRENT_STORE_KEY, store)
  }

  const setStores = (storeList: StoreDTO[]) => {
    stores.value = [...storeList]
    writeStoredJson(STORES_KEY, stores.value)
  }

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
    } finally {
      loading.value = false
    }
  }

  const createStore = async (payload: StoreRequest) => {
    const response = await createStoreRequest(payload)
    if (!response.success || !response.data) {
      throw new Error(response.message || '创建门店失败')
    }

    let nextStores: StoreDTO[] = []

    try {
      nextStores = await fetchUserStores(true)
    } catch {
      nextStores = stores.value.filter((store) => store.id !== response.data.id)
      nextStores.unshift(response.data)
      setStores(nextStores)
    }

    const createdStore = nextStores.find((store) => store.id === response.data.id) ?? response.data

    if (!nextStores.some((store) => store.id === createdStore.id)) {
      nextStores = [createdStore, ...nextStores]
      setStores(nextStores)
    }

    setCurrentStore(createdStore)

    return {
      store: createdStore,
      message: response.message || '门店创建成功',
    }
  }

  const deleteStore = async (storeId: number) => {
    const response = await deleteStoreRequest(storeId)
    if (!response.success) {
      const error = new Error(response.message || '删除门店失败')
      const errorCode = (response.data as DeleteStoreErrorData | null)?.code

      if (errorCode) {
        Reflect.set(error, 'code', errorCode)
      }

      throw error
    }

    const nextStores = stores.value.filter((store) => store.id !== storeId)
    setStores(nextStores)

    if (currentStore.value?.id === storeId) {
      setCurrentStore(null)
    }

    if (!response.message || response.message === 'Delete store success') {
      return DELETE_STORE_SUCCESS_MESSAGE
    }

    return response.message
  }

  const clearStoreData = () => {
    currentStore.value = null
    stores.value = []
    clearStoreStorage()
  }

  const hasStores = computed(() => stores.value.length > 0)

  hydrate()

  return {
    currentStore,
    stores,
    loading,
    hasStores,
    hydrate,
    setCurrentStore,
    setStores,
    fetchUserStores,
    createStore,
    deleteStore,
    clearStoreData,
  }
})
