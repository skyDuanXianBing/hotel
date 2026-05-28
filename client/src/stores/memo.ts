import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMemo, saveMemo } from '@/api/memo'
import { ElMessage } from 'element-plus'
import { i18n } from '@/locales'

const translate = (key: string) => i18n.global.t(key)

export const useMemoStore = defineStore('memo', () => {
  const memoContent = ref('')
  const loading = ref(false)
  const lastSavedAt = ref<Date | null>(null)
  const autoSaving = ref(false)

  let saveTimer: ReturnType<typeof setTimeout> | null = null

  const loadMemo = async () => {
    loading.value = true
    try {
      const response = await getMemo()
      if (response.success) {
        memoContent.value = response.data || ''
      }
    } catch (error) {
      console.error('Failed to load memo:', error)
    } finally {
      loading.value = false
    }
  }

  const saveImmediately = async () => {
    if (loading.value || autoSaving.value) {
      return
    }

    autoSaving.value = true
    try {
      await saveMemo(memoContent.value)
      lastSavedAt.value = new Date()
    } catch (error) {
      console.error('Failed to save memo:', error)
      ElMessage.error(translate('stage6.common.messages.memoSaveFailed'))
    } finally {
      autoSaving.value = false
    }
  }

  const saveMemoDebounced = (content: string) => {
    memoContent.value = content

    if (saveTimer) {
      clearTimeout(saveTimer)
    }

    saveTimer = setTimeout(async () => {
      await saveImmediately()
    }, 500)
  }

  const getFormattedSaveTime = () => {
    if (!lastSavedAt.value) {
      return ''
    }

    const now = lastSavedAt.value
    const hours = String(now.getHours()).padStart(2, '0')
    const minutes = String(now.getMinutes()).padStart(2, '0')
    const seconds = String(now.getSeconds()).padStart(2, '0')
    return `${hours}:${minutes}:${seconds}`
  }

  return {
    memoContent,
    loading,
    lastSavedAt,
    autoSaving,
    loadMemo,
    saveImmediately,
    saveMemoDebounced,
    getFormattedSaveTime,
  }
})
