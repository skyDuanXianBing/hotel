import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getAllChannels, type ChannelDTO } from '@/api/channel'
import { getRegistrationLinkInbox, getRegistrationReviewList, type RegistrationReviewListParams } from '@/api/review'
import type { ReviewLinkEntry, ReviewRecord } from '@/constants/reviews'
import { showUnhandledRequestWarning } from '@/utils/requestError'

export const useReviewStore = defineStore('reviews', () => {
  const records = ref<ReviewRecord[]>([])
  const channels = ref<ChannelDTO[]>([])
  const linkEntries = ref<ReviewLinkEntry[]>([])
  const isLoading = ref(false)
  const isChannelLoading = ref(false)
  const isLinkLoading = ref(false)
  const loadError = ref('')
  const channelLoadError = ref('')
  const linkLoadError = ref('')
  const hasLoaded = ref(false)

  const pendingCount = computed(() => {
    return records.value.filter((item) => item.status === 'pending').length
  })

  const approvedCount = computed(() => {
    return records.value.filter((item) => item.status === 'approved').length
  })

  const totalCount = computed(() => {
    return records.value.length
  })

  const refreshRecords = async (params?: RegistrationReviewListParams) => {
    isLoading.value = true
    loadError.value = ''

    try {
      const nextRecords = await getRegistrationReviewList(params)
      records.value = nextRecords
      hasLoaded.value = true
      return true
    } catch (error) {
      records.value = []
      loadError.value = showUnhandledRequestWarning(error, '加载审查列表失败')
      hasLoaded.value = true
      return false
    } finally {
      isLoading.value = false
    }
  }

  const refreshChannels = async () => {
    isChannelLoading.value = true
    channelLoadError.value = ''

    try {
      const response = await getAllChannels()

      if (!response.success) {
        throw new Error(response.message || '加载渠道失败')
      }

      channels.value = response.data || []
      return true
    } catch (error) {
      channels.value = []
      channelLoadError.value = showUnhandledRequestWarning(error, '加载渠道失败')
      return false
    } finally {
      isChannelLoading.value = false
    }
  }

  const refreshLinks = async () => {
    isLinkLoading.value = true
    linkLoadError.value = ''

    try {
      linkEntries.value = await getRegistrationLinkInbox()
      return true
    } catch (error) {
      linkEntries.value = []
      linkLoadError.value = showUnhandledRequestWarning(error, '加载链接列表失败')
      return false
    } finally {
      isLinkLoading.value = false
    }
  }

  return {
    records,
    channels,
    linkEntries,
    isLoading,
    isChannelLoading,
    isLinkLoading,
    loadError,
    channelLoadError,
    linkLoadError,
    hasLoaded,
    totalCount,
    pendingCount,
    approvedCount,
    refreshRecords,
    refreshChannels,
    refreshLinks,
  }
})
