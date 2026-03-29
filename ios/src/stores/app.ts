import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { AppToastColor, AppToastEventDetail } from '@/utils/notify'
import { DEFAULT_TOAST_DURATION } from '@/utils/notify'

export const useAppStore = defineStore('app', () => {
  const isRestoring = ref(false)
  const isReady = ref(false)
  const toastOpen = ref(false)
  const toastMessage = ref('')
  const toastColor = ref<AppToastColor>('primary')
  const toastDuration = ref(DEFAULT_TOAST_DURATION)

  const restoreRuntimeState = async () => {
    if (isRestoring.value || isReady.value) {
      return
    }

    isRestoring.value = true

    try {
      const authStore = useAuthStore()
      const userStore = useUserStore()
      const storeStore = useStoreStore()

      authStore.hydrate()
      userStore.hydrate()
      storeStore.hydrate()

      isReady.value = true
    } finally {
      isRestoring.value = false
    }
  }

  const showToast = (detail: AppToastEventDetail) => {
    if (!detail.message) {
      return
    }

    toastMessage.value = detail.message
    toastColor.value = detail.color ?? 'primary'
    toastDuration.value = detail.duration ?? DEFAULT_TOAST_DURATION
    toastOpen.value = true
  }

  const dismissToast = () => {
    toastOpen.value = false
  }

  return {
    isRestoring,
    isReady,
    toastOpen,
    toastMessage,
    toastColor,
    toastDuration,
    restoreRuntimeState,
    showToast,
    dismissToast,
  }
})
