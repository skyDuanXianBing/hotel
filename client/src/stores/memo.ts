import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMemo, saveMemo } from '@/api/memo'
import { ElMessage } from 'element-plus'

export const useMemoStore = defineStore('memo', () => {
  // 备忘录内容
  const memoContent = ref('')
  // 加载状态
  const loading = ref(false)
  // 最后保存时间
  const lastSavedAt = ref<Date | null>(null)
  // 自动保存状态
  const autoSaving = ref(false)

  // 定时器
  let saveTimer: ReturnType<typeof setTimeout> | null = null

  /**
   * 加载备忘录内容
   */
  const loadMemo = async () => {
    loading.value = true
    try {
      const response = await getMemo()
      if (response.success) {
        memoContent.value = response.data || ''
      }
    } catch (error) {
      console.error('加载备忘录失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 立即保存备忘录
   */
  const saveImmediately = async () => {
    if (loading.value || autoSaving.value) {
      return
    }

    autoSaving.value = true
    try {
      await saveMemo(memoContent.value)
      lastSavedAt.value = new Date()
    } catch (error) {
      console.error('保存备忘录失败:', error)
      ElMessage.error('保存备忘录失败')
    } finally {
      autoSaving.value = false
    }
  }

  /**
   * 延迟保存备忘录（防抖）
   */
  const saveMemoDebounced = (content: string) => {
    memoContent.value = content

    // 清除之前的定时器
    if (saveTimer) {
      clearTimeout(saveTimer)
    }

    // 设置新的定时器，500ms后保存
    saveTimer = setTimeout(async () => {
      await saveImmediately()
    }, 500)
  }

  /**
   * 格式化最后保存时间
   */
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
