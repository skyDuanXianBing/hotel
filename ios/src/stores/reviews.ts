import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getAllChannels, type ChannelDTO } from '@/api/channel'
import { getRegistrationLinkInbox, getRegistrationReviewList, type RegistrationReviewListParams } from '@/api/review'
import { sortReviewRecordsByCheckInDate, type ReviewLinkEntry, type ReviewRecord } from '@/constants/reviews'
import { i18n } from '@/locales'
import { showUnhandledRequestWarning } from '@/utils/requestError'

const reviewText = (key: string) => i18n.global.t(`runtime.review.${key}`)

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
  const hasLoadedChannels = ref(false)

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
      records.value = sortReviewRecordsByCheckInDate(nextRecords)
      hasLoaded.value = true
      return true
    } catch (error) {
      records.value = []
      loadError.value = showUnhandledRequestWarning(error, reviewText('loadListFailed'))
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
        throw new Error(response.message || reviewText('loadChannelsFailed'))
      }

      channels.value = response.data || []
      hasLoadedChannels.value = true
      return true
    } catch (error) {
      channels.value = []
      channelLoadError.value = showUnhandledRequestWarning(error, reviewText('loadChannelsFailed'))
      hasLoadedChannels.value = true
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
      linkLoadError.value = showUnhandledRequestWarning(error, reviewText('loadLinksFailed'))
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
    hasLoadedChannels,
    totalCount,
    pendingCount,
    approvedCount,
    refreshRecords,
    refreshChannels,
    refreshLinks,
  }
})
