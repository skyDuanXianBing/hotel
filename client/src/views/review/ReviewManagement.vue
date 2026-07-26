<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { TabsPaneContext } from 'element-plus'
import { ElMessage } from 'element-plus'
import { ChatLineSquare, Refresh, Search, View } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue'
import WorkspaceSidebar from '@/components/layout/WorkspaceSidebar.vue'
import type { WorkspaceSidebarItem } from '@/components/layout/workspace'
import {
  REVIEW_ALLOWED_ACTION,
  REVIEW_CENTER_TAB,
  getReviewById,
  getReviews,
  syncReviews,
  type ReviewAllowedAction,
  type ReviewCenterTab,
  type SuReviewDTO,
  type SuReviewDetailDTO,
} from '@/api/suReviews'
import { useStoreStore } from '@/stores/store'
import GuestReviewDialog from '@/views/review/components/GuestReviewDialog.vue'
import ReplyReviewDialog from '@/views/review/components/ReplyReviewDialog.vue'
import ReviewDetailDrawer from '@/views/review/components/ReviewDetailDrawer.vue'
import {
  formatReviewDate,
  formatReviewDateTime,
  getIssueReason,
  getReservationContext,
  getReviewErrorMessage,
  getReviewScore,
  getReviewText,
  hasAllowedReviewAction,
  normalizeChannelCode,
} from '@/views/review/reviewPresentation'

const { t, te, locale } = useI18n()
const storeStore = useStoreStore()

const tabOrder: ReviewCenterTab[] = [
  REVIEW_CENTER_TAB.ALL,
  REVIEW_CENTER_TAB.PENDING_REPLY,
  REVIEW_CENTER_TAB.PENDING_GUEST_REVIEW,
  REVIEW_CENTER_TAB.PROCESSING,
  REVIEW_CENTER_TAB.COMPLETED,
  REVIEW_CENTER_TAB.UNLINKED,
]

const activeTab = ref<ReviewCenterTab>(REVIEW_CENTER_TAB.ALL)
const filters = reactive({
  channel: '',
  searchInput: '',
  search: '',
})
const rows = ref<SuReviewDTO[]>([])
const loading = ref(false)
const syncing = ref(false)
const listError = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)
const listAllowedActions = ref<ReviewAllowedAction[]>([])
const listActionReasons = ref<Record<string, string>>({})
const selectedReviewId = ref<number | string | null>(null)
const detailDrawerVisible = ref(false)
const detailRefreshToken = ref(0)
const actionReview = ref<SuReviewDetailDTO | null>(null)
const replyDialogVisible = ref(false)
const guestReviewDialogVisible = ref(false)
const actionLoadingKey = ref('')
let requestSequence = 0

const currentStoreId = computed(() => storeStore.currentStore?.id ?? null)
const canSync = computed(() =>
  listAllowedActions.value.some(
    (action) => String(action).toUpperCase() === REVIEW_ALLOWED_ACTION.SYNC,
  ),
)
const syncUnavailableReason = computed(
  () => listActionReasons.value[REVIEW_ALLOWED_ACTION.SYNC] || '',
)

const reviewSidebarItems = computed<WorkspaceSidebarItem[]>(() => [
  {
    key: 'ota-review-center',
    label: t('suReviews.sidebar.center'),
    icon: ChatLineSquare,
  },
])

const emptyLabel = computed(() => t('suReviews.labels.notProvided'))

const formatDate = (value?: string | null) =>
  formatReviewDate(value, String(locale.value), emptyLabel.value)

const formatDateTime = (value?: string | null) =>
  formatReviewDateTime(value, String(locale.value), emptyLabel.value)

const translateEnum = (scope: string, value?: string | null) => {
  if (!value) {
    return emptyLabel.value
  }
  const normalized = value
    .trim()
    .replace(/[\s-]+/g, '_')
    .toUpperCase()
  const keys = [
    `suReviews.${scope}.${value}`,
    `suReviews.${scope}.${normalized}`,
    `suReviews.${scope}.${normalized.toLowerCase()}`,
  ]
  const key = keys.find((candidate) => te(candidate))
  return key ? t(key) : value
}

const getChannelLabel = (row: SuReviewDTO) => {
  const channelCode = normalizeChannelCode(row.channelCode)
  const key = `suReviews.channels.${channelCode}`
  return te(key) ? t(key) : row.channelCode || emptyLabel.value
}

const getChannelClass = (row: SuReviewDTO) => {
  const channelCode = normalizeChannelCode(row.channelCode)
  if (channelCode === 'AIRBNB') {
    return 'is-airbnb'
  }
  if (channelCode === 'BOOKING') {
    return 'is-booking'
  }
  return ''
}

const statusTagType = (status?: string | null) => {
  const normalized = String(status || '').toUpperCase()
  if (['COMPLETED', 'CONFIRMED', 'PUBLISHED', 'LINKED'].includes(normalized)) {
    return 'success' as const
  }
  if (['FAILED', 'UNLINKED', 'AMBIGUOUS'].includes(normalized)) {
    return 'danger' as const
  }
  if (['PENDING', 'SUBMITTED', 'IN_REVIEW', 'CREATED'].includes(normalized)) {
    return 'warning' as const
  }
  return 'info' as const
}

const canReply = (row: SuReviewDTO) => hasAllowedReviewAction(row, REVIEW_ALLOWED_ACTION.REPLY)

const canReviewGuest = (row: SuReviewDTO) =>
  hasAllowedReviewAction(row, REVIEW_ALLOWED_ACTION.GUEST_REVIEW)

const hasRowAction = (row: SuReviewDTO) => canReply(row) || canReviewGuest(row)

const getPrimaryActionReason = (row: SuReviewDTO) => {
  const issueReason = getIssueReason(row)
  if (issueReason) {
    return issueReason
  }
  const reasons = Object.values(row.actionReasons || {}).filter(Boolean)
  return reasons[0] || ''
}

const loadReviews = async () => {
  if (!currentStoreId.value) {
    rows.value = []
    totalElements.value = 0
    totalPages.value = 0
    listAllowedActions.value = []
    listActionReasons.value = {}
    return
  }

  const requestedStoreId = currentStoreId.value
  const sequence = ++requestSequence
  loading.value = true
  listError.value = ''

  try {
    const response = await getReviews({
      page: currentPage.value - 1,
      size: pageSize.value,
      tab: activeTab.value,
      ...(filters.channel ? { channel: filters.channel } : {}),
      ...(filters.search ? { search: filters.search } : {}),
    })

    if (sequence !== requestSequence || currentStoreId.value !== requestedStoreId) {
      return
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || t('suReviews.errors.list'))
    }

    rows.value = response.data.items || []
    totalElements.value = Number(response.data.totalElements || 0)
    totalPages.value = Number(response.data.totalPages || 0)
    listAllowedActions.value = response.data.allowedActions || []
    listActionReasons.value = response.data.actionReasons || {}

    const responsePage = Number(response.data.page)
    if (Number.isFinite(responsePage) && responsePage + 1 !== currentPage.value) {
      currentPage.value = responsePage + 1
    }
  } catch (error) {
    if (sequence !== requestSequence || currentStoreId.value !== requestedStoreId) {
      return
    }
    rows.value = []
    totalElements.value = 0
    totalPages.value = 0
    listAllowedActions.value = []
    listActionReasons.value = {}
    listError.value = getReviewErrorMessage(error, t('suReviews.errors.list'))
  } finally {
    if (sequence === requestSequence) {
      loading.value = false
    }
  }
}

watch(
  currentStoreId,
  (storeId, previousStoreId) => {
    requestSequence += 1
    rows.value = []
    totalElements.value = 0
    totalPages.value = 0
    listAllowedActions.value = []
    listActionReasons.value = {}
    listError.value = ''
    selectedReviewId.value = null
    detailDrawerVisible.value = false
    replyDialogVisible.value = false
    guestReviewDialogVisible.value = false
    actionReview.value = null
    currentPage.value = 1

    if (storeId && storeId !== previousStoreId) {
      void loadReviews()
    }
  },
  { immediate: true },
)

const handleTabChange = (tab: TabsPaneContext) => {
  const nextTab = String(tab.paneName) as ReviewCenterTab
  if (!tabOrder.includes(nextTab)) {
    return
  }
  activeTab.value = nextTab
  currentPage.value = 1
  void loadReviews()
}

const handleStatusFilterChange = () => {
  currentPage.value = 1
  void loadReviews()
}

const handleChannelChange = () => {
  currentPage.value = 1
  void loadReviews()
}

const handleSearch = () => {
  filters.search = filters.searchInput.trim()
  currentPage.value = 1
  void loadReviews()
}

const resetFilters = () => {
  activeTab.value = REVIEW_CENTER_TAB.ALL
  filters.channel = ''
  filters.searchInput = ''
  filters.search = ''
  currentPage.value = 1
  void loadReviews()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  void loadReviews()
}

const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  void loadReviews()
}

const handleSync = async () => {
  if (!canSync.value) {
    ElMessage.warning(
      syncUnavailableReason.value
        ? t('suReviews.sync.unavailableReason', { reason: syncUnavailableReason.value })
        : t('suReviews.sync.unavailable'),
    )
    return
  }

  syncing.value = true
  try {
    const response = await syncReviews()
    if (!response.success || !response.data?.success) {
      throw new Error(response.data?.message || response.message || t('suReviews.errors.sync'))
    }
    ElMessage.success(response.data.message || response.message || t('suReviews.sync.accepted'))
    currentPage.value = 1
    await loadReviews()
  } catch (error) {
    ElMessage.error(getReviewErrorMessage(error, t('suReviews.errors.sync')))
  } finally {
    syncing.value = false
  }
}

const openDetail = (row: SuReviewDTO) => {
  selectedReviewId.value = row.id
  detailDrawerVisible.value = true
}

const actionKey = (row: SuReviewDTO, action: ReviewAllowedAction) => `${row.id}:${action}`

const openRowAction = async (row: SuReviewDTO, action: ReviewAllowedAction) => {
  if (!hasAllowedReviewAction(row, action)) {
    return
  }

  const key = actionKey(row, action)
  actionLoadingKey.value = key
  try {
    const response = await getReviewById(row.id)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('suReviews.errors.detail'))
    }

    const latest = response.data
    if (!hasAllowedReviewAction(latest, action)) {
      const reason = latest.actionReasons?.[String(action)] || t('suReviews.eligibility.none')
      ElMessage.warning(reason)
      await loadReviews()
      return
    }

    actionReview.value = latest
    if (action === REVIEW_ALLOWED_ACTION.REPLY) {
      replyDialogVisible.value = true
      return
    }
    if (action === REVIEW_ALLOWED_ACTION.GUEST_REVIEW) {
      guestReviewDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error(getReviewErrorMessage(error, t('suReviews.errors.detail')))
  } finally {
    if (actionLoadingKey.value === key) {
      actionLoadingKey.value = ''
    }
  }
}

const openReplyFromDrawer = (review: SuReviewDetailDTO) => {
  actionReview.value = review
  replyDialogVisible.value = true
}

const openGuestReviewFromDrawer = (review: SuReviewDetailDTO) => {
  actionReview.value = review
  guestReviewDialogVisible.value = true
}

const handleActionSubmitted = async () => {
  detailRefreshToken.value += 1
  await loadReviews()
}
</script>

<template>
  <WorkspaceLayout
    storage-key="ota-review-sidebar-collapsed"
    content-padding="0 24px 24px"
    content-padding-narrow="0 20px 20px"
    content-padding-mobile="0 12px 20px"
  >
    <template #sidebar="{ collapsed, toggleSidebar }">
      <WorkspaceSidebar
        :collapsed="collapsed"
        :items="reviewSidebarItems"
        active-key="ota-review-center"
        :collapse-label="t('suReviews.sidebar.collapse')"
        :aria-label="t('suReviews.sidebar.aria')"
        @toggle="toggleSidebar"
      />
    </template>

    <div class="review-center-page">
      <section class="review-hero">
        <div class="hero-copy">
          <span class="hero-eyebrow">{{ t('suReviews.page.eyebrow') }}</span>
          <h1>{{ t('suReviews.page.title') }}</h1>
          <p>{{ t('suReviews.page.description') }}</p>
          <div class="security-note">
            <span class="security-dot"></span>
            <span>{{ t('suReviews.page.securityNote') }}</span>
          </div>
        </div>

        <div class="hero-actions">
          <el-tooltip
            v-if="canSync"
            :content="syncUnavailableReason || t('suReviews.actions.sync')"
            placement="bottom"
          >
            <el-button type="primary" :icon="Refresh" :loading="syncing" @click="handleSync">
              {{ t('suReviews.actions.sync') }}
            </el-button>
          </el-tooltip>
          <el-button :icon="Refresh" :loading="loading" @click="loadReviews">
            {{ t('suReviews.actions.refresh') }}
          </el-button>
        </div>
      </section>

      <section class="review-workspace">
        <div class="status-tabs">
          <el-tabs v-model="activeTab" @tab-click="handleTabChange">
            <el-tab-pane
              v-for="tab in tabOrder"
              :key="tab"
              :name="tab"
              :label="t(`suReviews.tabs.${tab}`)"
            />
          </el-tabs>
        </div>

        <div class="filter-bar" role="search">
          <div class="filter-field">
            <label for="review-channel-filter">{{ t('suReviews.filters.channel') }}</label>
            <el-select
              id="review-channel-filter"
              v-model="filters.channel"
              :placeholder="t('suReviews.filters.allChannels')"
              clearable
              @change="handleChannelChange"
            >
              <el-option :label="t('suReviews.filters.allChannels')" value="" />
              <el-option :label="t('suReviews.channels.AIRBNB')" value="AIRBNB" />
              <el-option :label="t('suReviews.channels.BOOKING')" value="BOOKING" />
            </el-select>
          </div>

          <div class="filter-field">
            <label for="review-status-filter">{{ t('suReviews.filters.status') }}</label>
            <el-select
              id="review-status-filter"
              v-model="activeTab"
              @change="handleStatusFilterChange"
            >
              <el-option
                v-for="tab in tabOrder"
                :key="tab"
                :label="t(`suReviews.tabs.${tab}`)"
                :value="tab"
              />
            </el-select>
          </div>

          <div class="filter-field filter-field--search">
            <label for="review-search-input">{{ t('suReviews.filters.search') }}</label>
            <el-input
              id="review-search-input"
              v-model="filters.searchInput"
              clearable
              :prefix-icon="Search"
              :placeholder="t('suReviews.filters.searchPlaceholder')"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </div>

          <div class="filter-actions">
            <el-button type="primary" :icon="Search" @click="handleSearch">
              {{ t('suReviews.actions.search') }}
            </el-button>
            <el-button @click="resetFilters">{{ t('suReviews.actions.reset') }}</el-button>
          </div>
        </div>

        <el-alert
          v-if="listError"
          :title="listError"
          type="error"
          :closable="false"
          show-icon
          class="list-error"
        >
          <template #default>
            <el-button size="small" @click="loadReviews">
              {{ t('suReviews.actions.retry') }}
            </el-button>
          </template>
        </el-alert>

        <div class="table-header">
          <div>
            <span class="table-kicker">{{ t(`suReviews.tabs.${activeTab}`) }}</span>
            <strong>{{ totalElements }}</strong>
          </div>
          <span v-if="!canSync && syncUnavailableReason" class="sync-reason">
            {{ t('suReviews.sync.unavailableReason', { reason: syncUnavailableReason }) }}
          </span>
        </div>

        <div class="table-surface">
          <el-table
            v-loading="loading"
            :data="rows"
            row-key="id"
            class="review-table"
            @row-click="openDetail"
          >
            <el-table-column :label="t('suReviews.table.channel')" width="142">
              <template #default="{ row }">
                <div class="channel-cell">
                  <span class="channel-chip" :class="getChannelClass(row)">
                    {{ getChannelLabel(row) }}
                  </span>
                  <span>{{ row.propertyName || emptyLabel }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column :label="t('suReviews.table.order')" min-width="210">
              <template #default="{ row }">
                <div class="order-cell">
                  <strong>{{ row.orderNumber || emptyLabel }}</strong>
                  <span>{{ row.guestName || emptyLabel }}</span>
                  <small>{{ row.channelBookingId || emptyLabel }}</small>
                </div>
              </template>
            </el-table-column>

            <el-table-column :label="t('suReviews.table.stay')" min-width="178">
              <template #default="{ row }">
                <div class="stay-cell">
                  <span>{{ formatDate(row.checkInDate) }}</span>
                  <span class="stay-arrow">→</span>
                  <span>{{ formatDate(row.checkOutDate) }}</span>
                  <el-tag size="small" effect="plain">
                    {{ row.reservationStatus || emptyLabel }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>

            <el-table-column :label="t('suReviews.table.review')" min-width="280">
              <template #default="{ row }">
                <div class="review-summary-cell">
                  <div class="review-summary-heading">
                    <strong>{{
                      row.reviewTitle || translateEnum('reviewTypes', row.reviewType)
                    }}</strong>
                    <span v-if="getReviewScore(row) !== null" class="score-chip">
                      {{ getReviewScore(row) }}
                    </span>
                  </div>
                  <p>{{ getReviewText(row) || emptyLabel }}</p>
                  <small v-if="row.negativeReviewText">{{ row.negativeReviewText }}</small>
                </div>
              </template>
            </el-table-column>

            <el-table-column :label="t('suReviews.table.eligibility')" min-width="184">
              <template #default="{ row }">
                <div class="eligibility-cell">
                  <el-tag v-if="canReply(row)" type="success" effect="light" size="small">
                    {{ t('suReviews.eligibility.reply') }}
                  </el-tag>
                  <el-tag v-if="canReviewGuest(row)" type="success" effect="light" size="small">
                    {{ t('suReviews.eligibility.guestReview') }}
                  </el-tag>
                  <el-tag v-if="!hasRowAction(row)" type="info" effect="plain" size="small">
                    {{ t('suReviews.eligibility.none') }}
                  </el-tag>
                  <small v-if="getPrimaryActionReason(row)">
                    {{ getPrimaryActionReason(row) }}
                  </small>
                </div>
              </template>
            </el-table-column>

            <el-table-column :label="t('suReviews.table.status')" min-width="154">
              <template #default="{ row }">
                <div class="status-cell">
                  <el-tag :type="statusTagType(row.reviewStatus)" effect="plain" size="small">
                    {{ translateEnum('reviewStatuses', row.reviewStatus) }}
                  </el-tag>
                  <span>{{ translateEnum('associationStatuses', row.associationStatus) }}</span>
                  <small v-if="row.lastActionStatus">
                    {{ translateEnum('actionStatuses', row.lastActionStatus) }}
                  </small>
                </div>
              </template>
            </el-table-column>

            <el-table-column :label="t('suReviews.table.syncedAt')" width="142">
              <template #default="{ row }">
                <span class="synced-at">{{ formatDateTime(row.lastSyncedAt) }}</span>
              </template>
            </el-table-column>

            <el-table-column
              :label="t('suReviews.table.actions')"
              fixed="right"
              width="248"
              align="right"
            >
              <template #default="{ row }">
                <div class="row-actions" @click.stop>
                  <el-tooltip :content="t('suReviews.actions.view')" placement="top">
                    <el-button
                      circle
                      :icon="View"
                      :aria-label="t('suReviews.actions.view')"
                      @click="openDetail(row)"
                    />
                  </el-tooltip>
                  <el-button
                    v-if="canReply(row)"
                    type="primary"
                    plain
                    size="small"
                    :loading="actionLoadingKey === actionKey(row, REVIEW_ALLOWED_ACTION.REPLY)"
                    @click="openRowAction(row, REVIEW_ALLOWED_ACTION.REPLY)"
                  >
                    {{ t('suReviews.actions.reply') }}
                  </el-button>
                  <el-button
                    v-if="canReviewGuest(row)"
                    type="primary"
                    size="small"
                    :loading="
                      actionLoadingKey === actionKey(row, REVIEW_ALLOWED_ACTION.GUEST_REVIEW)
                    "
                    @click="openRowAction(row, REVIEW_ALLOWED_ACTION.GUEST_REVIEW)"
                  >
                    {{ t('suReviews.actions.guestReview') }}
                  </el-button>
                </div>
              </template>
            </el-table-column>

            <template #empty>
              <div class="empty-state">
                <el-empty :description="t('suReviews.empty.title')" :image-size="92" />
                <p>{{ t('suReviews.empty.description') }}</p>
              </div>
            </template>
          </el-table>

          <div v-if="totalElements > 0" class="pagination-bar">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="[20, 50, 100]"
              :total="totalElements"
              @current-change="handlePageChange"
              @size-change="handlePageSizeChange"
            />
          </div>
        </div>
      </section>
    </div>

    <ReviewDetailDrawer
      v-model="detailDrawerVisible"
      :review-id="selectedReviewId"
      :refresh-token="detailRefreshToken"
      @reply="openReplyFromDrawer"
      @guest-review="openGuestReviewFromDrawer"
    />
    <ReplyReviewDialog
      v-model="replyDialogVisible"
      :review="actionReview"
      @submitted="handleActionSubmitted"
    />
    <GuestReviewDialog
      v-model="guestReviewDialogVisible"
      :review="actionReview"
      @submitted="handleActionSubmitted"
    />
  </WorkspaceLayout>
</template>

<style scoped>
.review-center-page {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.review-hero {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  overflow: hidden;
  padding: 28px 30px;
  border: 1px solid #e6e9e3;
  border-radius: 20px;
  background:
    radial-gradient(circle at 88% 20%, rgb(47 124 246 / 13%), transparent 28%),
    linear-gradient(135deg, #ffffff 0%, #fbfcf8 64%, #f1f6ff 100%);
}

.review-hero::after {
  position: absolute;
  right: -38px;
  bottom: -74px;
  width: 190px;
  height: 190px;
  border: 24px solid rgb(227 28 95 / 6%);
  border-radius: 50%;
  content: '';
  pointer-events: none;
}

.hero-copy {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 7px;
  max-width: 780px;
}

.hero-eyebrow {
  color: #2f7cf6;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0;
  color: #1d211e;
  font-size: clamp(24px, 2.4vw, 34px);
  line-height: 1.2;
}

.hero-copy > p {
  max-width: 690px;
  margin: 0;
  color: #606762;
  font-size: 14px;
  line-height: 1.65;
}

.security-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 5px;
  color: #7c827d;
  font-size: 12px;
  line-height: 1.5;
}

.security-dot {
  width: 7px;
  height: 7px;
  margin-top: 5px;
  border-radius: 50%;
  background: #65a30d;
  box-shadow: 0 0 0 4px rgb(101 163 13 / 12%);
  flex: 0 0 auto;
}

.hero-actions {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.review-workspace {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e6e8e3;
  border-radius: 18px;
  background: #ffffff;
}

.status-tabs {
  padding: 0 24px;
  border-bottom: 1px solid #eceee9;
}

.status-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.status-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.status-tabs :deep(.el-tabs__item) {
  height: 54px;
  color: #666d67;
  font-weight: 650;
}

.status-tabs :deep(.el-tabs__item.is-active) {
  color: #2f7cf6;
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(150px, 0.8fr) minmax(190px, 1fr) minmax(280px, 2fr) auto;
  align-items: end;
  gap: 14px;
  padding: 18px 24px;
  border-bottom: 1px solid #eff0ec;
  background: #fafbf9;
}

.filter-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.filter-field label {
  color: #707772;
  font-size: 11px;
  font-weight: 700;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.list-error {
  margin: 16px 24px 0;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 24px 10px;
}

.table-header > div {
  display: flex;
  align-items: baseline;
  gap: 9px;
}

.table-kicker {
  color: #3a403b;
  font-size: 14px;
  font-weight: 750;
}

.table-header strong {
  color: #2f7cf6;
  font-size: 13px;
}

.sync-reason {
  max-width: 520px;
  color: #8a7163;
  font-size: 11px;
  line-height: 1.4;
  text-align: right;
}

.table-surface {
  min-width: 0;
  padding: 0 16px 18px;
}

.review-table {
  --el-table-border-color: #eceee9;
  --el-table-header-bg-color: #f7f8f5;
  --el-table-row-hover-bg-color: #f8fbff;
  width: 100%;
}

.review-table :deep(.el-table__row) {
  cursor: pointer;
}

.review-table :deep(.el-table__header th) {
  height: 46px;
  color: #6d746e;
  font-size: 11px;
  font-weight: 750;
}

.review-table :deep(.el-table__cell) {
  padding-top: 13px;
  padding-bottom: 13px;
}

.channel-cell,
.order-cell,
.stay-cell,
.review-summary-cell,
.eligibility-cell,
.status-cell {
  display: grid;
  align-content: center;
  justify-items: start;
  gap: 5px;
  min-width: 0;
}

.channel-cell > span:last-child,
.order-cell span,
.order-cell small,
.eligibility-cell small,
.status-cell span,
.status-cell small,
.synced-at {
  max-width: 100%;
  overflow: hidden;
  color: #858b86;
  font-size: 11px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.channel-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 9px;
  border-radius: 999px;
  background: #edf0ed;
  color: #5d655f;
  font-size: 11px;
  font-weight: 800;
}

.channel-chip.is-airbnb {
  background: #fff0f3;
  color: #e31c5f;
}

.channel-chip.is-booking {
  background: #edf4ff;
  color: #003b95;
}

.order-cell strong {
  max-width: 100%;
  overflow: hidden;
  color: #2d332e;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-cell span {
  color: #535a54;
  font-size: 12px;
}

.stay-cell {
  grid-template-columns: auto auto auto;
  align-items: center;
}

.stay-cell .el-tag {
  grid-column: 1 / -1;
}

.stay-cell > span {
  color: #505751;
  font-size: 11px;
}

.stay-arrow {
  color: #a7aca8 !important;
}

.review-summary-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.review-summary-heading strong {
  min-width: 0;
  overflow: hidden;
  color: #363c37;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.score-chip {
  flex: 0 0 auto;
  min-width: 28px;
  padding: 3px 7px;
  border-radius: 999px;
  background: #fff7dc;
  color: #876600;
  font-size: 11px;
  font-weight: 800;
  text-align: center;
}

.review-summary-cell p,
.review-summary-cell small {
  display: -webkit-box;
  max-width: 100%;
  margin: 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  text-overflow: ellipsis;
}

.review-summary-cell p {
  color: #687069;
  font-size: 12px;
  line-height: 1.45;
  -webkit-line-clamp: 2;
}

.review-summary-cell small {
  color: #b15b4b;
  font-size: 11px;
  line-height: 1.35;
  -webkit-line-clamp: 1;
}

.eligibility-cell {
  align-content: start;
}

.eligibility-cell small {
  display: -webkit-box;
  margin-top: 2px;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.status-cell {
  align-content: start;
}

.row-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.row-actions .el-button + .el-button {
  margin-left: 0;
}

.empty-state {
  display: grid;
  justify-items: center;
  padding: 36px 0 46px;
}

.empty-state p {
  max-width: 480px;
  margin: -14px 0 0;
  color: #8a908b;
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 18px 8px 0;
}

@media (max-width: 1180px) {
  .filter-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-field--search {
    grid-column: span 2;
  }

  .filter-actions {
    grid-column: span 2;
    justify-content: flex-end;
  }
}

@media (max-width: 760px) {
  .review-hero {
    flex-direction: column;
    padding: 22px 20px;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .filter-bar {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .filter-field--search,
  .filter-actions {
    grid-column: auto;
  }

  .filter-actions {
    justify-content: stretch;
  }

  .filter-actions .el-button {
    flex: 1;
  }

  .status-tabs,
  .table-header {
    padding-left: 16px;
    padding-right: 16px;
  }

  .table-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .sync-reason {
    text-align: left;
  }

  .pagination-bar {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
