import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getMemo, saveMemo } from '@/api/home'
import { i18n } from '@/locales'
import { formatDateTime } from '@/utils/formatters'
import { showErrorToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const AUTO_SAVE_DELAY = 500

export const useMemoStore = defineStore('memo', () => {
  const memoContent = ref('')
  const loading = ref(false)
  const autoSaving = ref(false)
  const lastSavedAt = ref<Date | null>(null)
  const hasLoaded = ref(false)

  let saveTimer: ReturnType<typeof setTimeout> | null = null
  let queuedSave = false
  let lastSavedContent = ''

  const clearSaveTimer = () => {
    if (saveTimer === null) {
      return
    }

    clearTimeout(saveTimer)
    saveTimer = null
  }

  const getFormattedSaveTime = () => {
    if (!lastSavedAt.value) {
      return ''
    }

    return formatDateTime(lastSavedAt.value, {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      year: undefined,
      month: undefined,
      day: undefined,
    })
  }

  const saveStatusText = computed(() => {
    const translate = i18n.global.t

    if (loading.value) {
      return translate('home.memoStatus.loading')
    }

    if (autoSaving.value) {
      return translate('home.memoStatus.saving')
    }

    const formattedTime = getFormattedSaveTime()
    if (formattedTime) {
      return translate('home.memoStatus.saved', { time: formattedTime })
    }

    if (hasLoaded.value) {
      return translate('home.memoStatus.ready')
    }

    return translate('home.memoStatus.waiting')
  })

  const loadMemo = async (force = false) => {
    if (!force && (loading.value || hasLoaded.value)) {
      return
    }

    clearSaveTimer()
    loading.value = true

    try {
      const response = await getMemo()
      if (!response.success) {
        throw new Error(response.message || i18n.global.t('home.error.memo'))
      }

      memoContent.value = response.data || ''
      lastSavedContent = memoContent.value
      hasLoaded.value = true
    } catch (error) {
      if (!isHandledRequestError(error)) {
        showErrorToast(error instanceof Error ? error.message : i18n.global.t('home.error.memo'))
      }

      throw error
    } finally {
      loading.value = false
    }
  }

  const saveImmediately = async () => {
    if (loading.value) {
      return
    }

    if (autoSaving.value) {
      queuedSave = true
      return
    }

    autoSaving.value = true
    queuedSave = false

    try {
      const response = await saveMemo(memoContent.value)
      if (!response.success) {
        throw new Error(response.message || i18n.global.t('home.error.memoSave'))
      }

      lastSavedAt.value = new Date()
      lastSavedContent = memoContent.value
    } catch (error) {
      if (!isHandledRequestError(error)) {
        showErrorToast(error instanceof Error ? error.message : i18n.global.t('home.error.memoSave'))
      }

      throw error
    } finally {
      autoSaving.value = false
    }

    if (queuedSave && memoContent.value !== lastSavedContent) {
      queuedSave = false
      await saveImmediately()
    }
  }

  const saveMemoDebounced = (content: string) => {
    memoContent.value = content
    clearSaveTimer()

    saveTimer = setTimeout(() => {
      void saveImmediately()
    }, AUTO_SAVE_DELAY)
  }

  return {
    memoContent,
    loading,
    autoSaving,
    lastSavedAt,
    hasLoaded,
    saveStatusText,
    loadMemo,
    saveImmediately,
    saveMemoDebounced,
    getFormattedSaveTime,
  }
})
