import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getAllChannels, type ChannelDTO } from '@/api/channel'
import {
  getRegistrationLinkInbox,
  getRegistrationReviewListPage,
  type RegistrationReviewListParams,
} from '@/api/review'
import { sortReviewRecordsByCheckInDate, type ReviewLinkEntry, type ReviewRecord } from '@/constants/reviews'
import { i18n } from '@/locales'
import { showUnhandledRequestWarning } from '@/utils/requestError'

const reviewText = (key: string) => i18n.global.t(`runtime.review.${key}`)
const REVIEW_PAGE_SIZE = 20

export const useReviewStore = defineStore('reviews', () => {
  const records = ref<ReviewRecord[]>([])
  const channels = ref<ChannelDTO[]>([])
  const linkEntries = ref<ReviewLinkEntry[]>([])
  const isLoading = ref(false)
  const isLoadingMore = ref(false)
  const isChannelLoading = ref(false)
  const isLinkLoading = ref(false)
  const loadError = ref('')
  const channelLoadError = ref('')
  const linkLoadError = ref('')
  const hasLoaded = ref(false)
  const hasLoadedChannels = ref(false)
  const currentPage = ref(0)
  const hasMore = ref(false)
  const serverTotalElements = ref(0)
  const serverPendingCount = ref(0)
  const serverApprovedCount = ref(0)
  let lastListParams: RegistrationReviewListParams | undefined
  let listRequestId = 0

  const pendingCount = computed(() => {
    if (serverPendingCount.value > 0 || hasLoaded.value) {
      return serverPendingCount.value
    }
    return records.value.filter((item) => item.status === 'pending').length
  })

  const approvedCount = computed(() => {
    if (serverApprovedCount.value > 0 || hasLoaded.value) {
      return serverApprovedCount.value
    }
    return records.value.filter((item) => item.status === 'approved').length
  })

  const totalCount = computed(() => {
    return serverTotalElements.value || records.value.length
  })

  const refreshStatusCounts = async () => {
    try {
      const [pendingPage, approvedPage] = await Promise.all([
        getRegistrationReviewListPage({ status: 'pending', page: 0, size: 1 }),
        getRegistrationReviewListPage({ status: 'approved', page: 0, size: 1 }),
      ])
      serverPendingCount.value = pendingPage.totalElements
      serverApprovedCount.value = approvedPage.totalElements
    } catch {
      // 角标失败不阻塞列表
    }
  }

  const refreshRecords = async (params?: RegistrationReviewListParams) => {
    const requestId = ++listRequestId
    isLoading.value = true
    loadError.value = ''
    lastListParams = { ...params }

    try {
      const [pageData] = await Promise.all([
        getRegistrationReviewListPage({
          ...params,
          page: 0,
          size: REVIEW_PAGE_SIZE,
        }),
        refreshStatusCounts(),
      ])
      if (requestId !== listRequestId) {
        return false
      }

      records.value = sortReviewRecordsByCheckInDate(pageData.items)
      currentPage.value = pageData.page
      hasMore.value = pageData.hasNext
      serverTotalElements.value = pageData.totalElements
      hasLoaded.value = true
      return true
    } catch (error) {
      if (requestId !== listRequestId) {
        return false
      }
      records.value = []
      hasMore.value = false
      serverTotalElements.value = 0
      loadError.value = showUnhandledRequestWarning(error, reviewText('loadListFailed'))
      hasLoaded.value = true
      return false
    } finally {
      if (requestId === listRequestId) {
        isLoading.value = false
      }
    }
  }

  const loadMoreRecords = async () => {
    if (!hasMore.value || isLoading.value || isLoadingMore.value) {
      return false
    }

    const requestId = listRequestId
    isLoadingMore.value = true
    try {
      const pageData = await getRegistrationReviewListPage({
        ...lastListParams,
        page: currentPage.value + 1,
        size: REVIEW_PAGE_SIZE,
      })
      if (requestId !== listRequestId) {
        return false
      }

      records.value = sortReviewRecordsByCheckInDate([...records.value, ...pageData.items])
      currentPage.value = pageData.page
      hasMore.value = pageData.hasNext
      serverTotalElements.value = pageData.totalElements
      return true
    } catch (error) {
      if (requestId === listRequestId) {
        showUnhandledRequestWarning(error, reviewText('loadListFailed'))
      }
      return false
    } finally {
      if (requestId === listRequestId) {
        isLoadingMore.value = false
      }
    }
  }

  /**
   * 用详情页拿到的最新记录就地更新列表缓存，避免审批后重拉全量列表。
   */
  const syncRecord = (nextRecord: ReviewRecord) => {
    const index = records.value.findIndex((item) => item.formId === nextRecord.formId)
    if (index === -1) {
      return
    }

    const nextRecords = [...records.value]
    nextRecords[index] = nextRecord
    records.value = sortReviewRecordsByCheckInDate(nextRecords)
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
    isLoadingMore,
    isChannelLoading,
    isLinkLoading,
    loadError,
    channelLoadError,
    linkLoadError,
    hasLoaded,
    hasLoadedChannels,
    hasMore,
    totalCount,
    pendingCount,
    approvedCount,
    refreshRecords,
    loadMoreRecords,
    refreshStatusCounts,
    syncRecord,
    refreshChannels,
    refreshLinks,
  }
})
