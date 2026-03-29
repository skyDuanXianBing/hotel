import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getMemo, saveMemo } from '@/api/home'
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

    const hours = String(lastSavedAt.value.getHours()).padStart(2, '0')
    const minutes = String(lastSavedAt.value.getMinutes()).padStart(2, '0')
    const seconds = String(lastSavedAt.value.getSeconds()).padStart(2, '0')

    return `${hours}:${minutes}:${seconds}`
  }

  const saveStatusText = computed(() => {
    if (loading.value) {
      return '加载中'
    }

    if (autoSaving.value) {
      return '自动保存中'
    }

    const formattedTime = getFormattedSaveTime()
    if (formattedTime) {
      return `已保存 ${formattedTime}`
    }

    if (hasLoaded.value) {
      return '输入后自动保存'
    }

    return '等待加载'
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
        throw new Error(response.message || '加载备忘录失败')
      }

      memoContent.value = response.data || ''
      lastSavedContent = memoContent.value
      hasLoaded.value = true
    } catch (error) {
      if (!isHandledRequestError(error)) {
        showErrorToast(error instanceof Error ? error.message : '加载备忘录失败')
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
        throw new Error(response.message || '保存备忘录失败')
      }

      lastSavedAt.value = new Date()
      lastSavedContent = memoContent.value
    } catch (error) {
      if (!isHandledRequestError(error)) {
        showErrorToast(error instanceof Error ? error.message : '保存备忘录失败')
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
