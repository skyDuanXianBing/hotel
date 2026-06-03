<template>
  <div class="reviews-page">
    <section class="page-header">
      <div>
        <h1>{{ t('pages.reviews.title') }}</h1>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="review-tabs">
      <el-tab-pane
        :label="t('pages.reviews.tabs.pendingGuest', { count: pendingGuestCount })"
        name="pendingGuest"
      />
      <el-tab-pane
        :label="t('pages.reviews.tabs.pendingHost', { count: pendingHostCount })"
        name="pendingHost"
      />
      <el-tab-pane
        :label="t('pages.reviews.tabs.reviewed', { count: reviewedCount })"
        name="reviewed"
      />
    </el-tabs>

    <section class="filter-panel">
      <el-input
        v-model="keyword"
        class="keyword-input"
        clearable
        :placeholder="t('pages.reviews.filters.keyword')"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-button-group>
        <el-button
          :type="quickRange === 'recent30' ? 'primary' : 'default'"
          @click="setQuickRange('recent30')"
        >
          {{ t('pages.reviews.filters.recent30') }}
        </el-button>
        <el-button
          :type="quickRange === 'checkout' ? 'primary' : 'default'"
          @click="setQuickRange('checkout')"
        >
          {{ t('pages.reviews.filters.checkoutTime') }}
        </el-button>
      </el-button-group>

      <el-date-picker
        v-model="dateRange"
        class="date-filter"
        type="daterange"
        value-format="YYYY-MM-DD"
        :range-separator="t('pages.reviews.filters.dateSeparator')"
        :start-placeholder="t('pages.reviews.filters.startDate')"
        :end-placeholder="t('pages.reviews.filters.endDate')"
      />

      <el-select v-model="roomFilter" class="room-filter" clearable :placeholder="t('pages.reviews.filters.room')">
        <el-option :label="t('pages.reviews.filters.allRooms')" value="all" />
        <el-option v-for="room in roomOptions" :key="room" :label="room" :value="room" />
      </el-select>

      <el-select
        v-model="guestRatingFilter"
        class="rating-filter"
        clearable
        :placeholder="t('pages.reviews.filters.guestRating')"
      >
        <el-option :label="t('pages.reviews.filters.anyRating')" :value="0" />
        <el-option :label="t('pages.reviews.filters.ratingFive')" :value="5" />
        <el-option :label="t('pages.reviews.filters.ratingFour')" :value="4" />
      </el-select>

      <el-button class="reset-button" @click="resetFilters">
        <el-icon><Refresh /></el-icon>
        <span>{{ t('pages.reviews.filters.reset') }}</span>
      </el-button>
    </section>

    <section class="table-panel">
      <el-table
        :data="pagedOrders"
        :row-key="getRowKey"
        class="reviews-table"
        empty-text="-"
      >
        <el-table-column :label="t('pages.reviews.columns.guest')" min-width="190">
          <template #default="{ row }">
            <div class="guest-cell">
              <div class="guest-line">
                <span class="channel-mark" :class="getChannelClass(row.channel)">
                  {{ getChannelInitial(row.channel) }}
                </span>
                <strong>{{ row.guestName }}</strong>
              </div>
              <small>{{ t('pages.reviews.guestCount', { count: row.guestCount }) }}</small>
              <small>{{ row.channelOrderNumber }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('pages.reviews.columns.room')" min-width="180">
          <template #default="{ row }">
            <div class="room-cell">
              <strong>{{ row.roomName }}</strong>
              <small>{{ row.orderNumber }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('pages.reviews.columns.stay')" min-width="150">
          <template #default="{ row }">
            <div class="stay-cell">
              <strong>{{ formatStayRange(row.checkInDate, row.checkOutDate) }}</strong>
              <small>{{ t('pages.reviews.orderStatus.checkedOut') }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('pages.reviews.columns.guestReview')" min-width="300">
          <template #default="{ row }">
            <div v-if="row.guestReview" class="review-content">
              <div class="rating-row">
                <el-rate :model-value="row.guestReview.rating" disabled size="small" />
                <span>{{ formatDate(row.guestReview.reviewedAt) }}</span>
              </div>
              <p>{{ row.guestReview.content }}</p>
            </div>
            <div v-else class="pending-content">
              <strong>{{ t('pages.reviews.empty.guestNotReviewed') }}</strong>
              <span>{{ formatDeadline(row.guestReviewDeadlineDays) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('pages.reviews.columns.hostReview')" min-width="320">
          <template #default="{ row }">
            <div v-if="row.hostReview" class="review-content">
              <div class="rating-row">
                <el-rate :model-value="row.hostReview.rating" disabled size="small" />
                <span>{{ formatDate(row.hostReview.reviewedAt) }}</span>
              </div>
              <p>{{ row.hostReview.content }}</p>
            </div>
            <div v-else class="host-pending">
              <div class="pending-content">
                <strong>{{ t('pages.reviews.empty.hostNotReviewed') }}</strong>
                <span>{{ formatDeadline(row.hostReviewDeadlineDays) }}</span>
              </div>
              <el-button type="primary" size="small" @click="openHostReviewDialog(row)">
                {{ t('pages.reviews.actions.reviewGuest') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <span>{{ t('pages.reviews.pagination.total', { count: filteredOrders.length }) }}</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          layout="sizes, prev, pager, next"
          :page-sizes="[5, 10, 20]"
          :total="filteredOrders.length"
          background
        />
      </div>
    </section>

    <el-dialog
      v-model="hostReviewDialogVisible"
      :title="t('pages.reviews.dialog.title')"
      width="560px"
      destroy-on-close
    >
      <div v-if="activeOrder" class="dialog-order">
        <strong>{{ activeOrder.guestName }}</strong>
        <span>{{ activeOrder.roomName }} · {{ formatStayRange(activeOrder.checkInDate, activeOrder.checkOutDate) }}</span>
      </div>

      <el-form label-position="top" class="host-review-form">
        <el-form-item :label="t('pages.reviews.dialog.rating')">
          <el-rate v-model="hostReviewForm.rating" />
        </el-form-item>
        <el-form-item :label="t('pages.reviews.dialog.tags')">
          <el-checkbox-group v-model="hostReviewForm.tags">
            <el-checkbox-button
              v-for="tag in hostReviewTagOptions"
              :key="tag.value"
              :label="tag.value"
            >
              {{ tag.label }}
            </el-checkbox-button>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item :label="t('pages.reviews.dialog.content')">
          <el-input
            v-model="hostReviewForm.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            :placeholder="t('pages.reviews.dialog.placeholder')"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="hostReviewDialogVisible = false">{{ t('common.close') }}</el-button>
        <el-button type="primary" @click="submitHostReview">
          {{ t('pages.reviews.dialog.submit') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { mockReviewOrders } from './mockData'
import type { ReviewOrderChannel, ReviewOrderRow, ReviewOrderTab } from './types'

const { t, locale } = useI18n()

// Frontend-only mock data until real review order APIs are available.
const reviewOrders = ref<ReviewOrderRow[]>(mockReviewOrders.map((order) => ({ ...order })))
const activeTab = ref<ReviewOrderTab>('pendingGuest')
const keyword = ref('')
const quickRange = ref<'recent30' | 'checkout' | ''>('recent30')
const dateRange = ref<[string, string] | null>(null)
const roomFilter = ref('all')
const guestRatingFilter = ref<number | null>(0)
const page = ref(1)
const pageSize = ref(10)
const hostReviewDialogVisible = ref(false)
const activeOrder = ref<ReviewOrderRow | null>(null)

const hostReviewForm = reactive({
  rating: 5,
  tags: [] as string[],
  content: '',
})

const hostReviewTagOptions = computed(() => [
  { value: 'clean', label: t('pages.reviews.hostTags.clean') },
  { value: 'communicative', label: t('pages.reviews.hostTags.communicative') },
  { value: 'rules', label: t('pages.reviews.hostTags.rules') },
  { value: 'welcomeBack', label: t('pages.reviews.hostTags.welcomeBack') },
])

const pendingGuestCount = computed(() => reviewOrders.value.filter((order) => !order.guestReview).length)
const pendingHostCount = computed(() => reviewOrders.value.filter((order) => !order.hostReview).length)
const reviewedCount = computed(() => reviewOrders.value.filter((order) => order.hostReview).length)

const roomOptions = computed(() =>
  Array.from(new Set(reviewOrders.value.map((order) => order.roomName)))
)

const tabOrders = computed(() => {
  switch (activeTab.value) {
    case 'pendingGuest':
      return reviewOrders.value.filter((order) => !order.guestReview)
    case 'pendingHost':
      return reviewOrders.value.filter((order) => !order.hostReview)
    case 'reviewed':
      return reviewOrders.value.filter((order) => order.hostReview)
    default:
      return reviewOrders.value
  }
})

const filteredOrders = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  const minimumRating = guestRatingFilter.value || 0

  return tabOrders.value.filter((order) => {
    const content = [
      order.orderNumber,
      order.channelOrderNumber,
      order.guestName,
      order.roomName,
      order.guestReview?.content,
      order.hostReview?.content,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()

    const matchesKeyword = !normalizedKeyword || content.includes(normalizedKeyword)
    const matchesRoom = roomFilter.value === 'all' || !roomFilter.value || order.roomName === roomFilter.value
    const matchesRating = !minimumRating || (order.guestReview?.rating ?? 0) >= minimumRating
    const matchesDate =
      !dateRange.value ||
      (order.checkOutDate >= dateRange.value[0] && order.checkOutDate <= dateRange.value[1])

    return matchesKeyword && matchesRoom && matchesRating && matchesDate
  })
})

const pagedOrders = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredOrders.value.slice(start, start + pageSize.value)
})

const getRowKey = (order: ReviewOrderRow) => order.id

const getChannelInitial = (channel: ReviewOrderChannel) => {
  if (channel === 'Booking.com') {
    return 'B.'
  }
  return 'A'
}

const getChannelClass = (channel: ReviewOrderChannel) =>
  channel === 'Booking.com' ? 'booking' : 'airbnb'

const formatDate = (date: string) => {
  const parsedDate = new Date(`${date}T00:00:00`)
  return new Intl.DateTimeFormat(locale.value, {
    month: '2-digit',
    day: '2-digit',
  }).format(parsedDate)
}

const formatStayRange = (checkInDate: string, checkOutDate: string) =>
  `${formatDate(checkInDate)} - ${formatDate(checkOutDate)}`

const formatDeadline = (days?: number) => {
  if (days == null) {
    return t('pages.reviews.deadline.none')
  }
  if (days <= 0) {
    return t('pages.reviews.deadline.today')
  }
  return t('pages.reviews.deadline.days', { count: days })
}

const setQuickRange = (range: 'recent30' | 'checkout') => {
  quickRange.value = range
  if (range === 'recent30') {
    dateRange.value = ['2026-05-01', '2026-05-30']
  }
}

const resetFilters = () => {
  keyword.value = ''
  quickRange.value = 'recent30'
  dateRange.value = null
  roomFilter.value = 'all'
  guestRatingFilter.value = 0
  page.value = 1
}

const openHostReviewDialog = (order: ReviewOrderRow) => {
  activeOrder.value = order
  hostReviewForm.rating = 5
  hostReviewForm.tags = []
  hostReviewForm.content = ''
  hostReviewDialogVisible.value = true
}

const submitHostReview = () => {
  if (!activeOrder.value) {
    return
  }
  if (!hostReviewForm.rating) {
    ElMessage.warning(t('pages.reviews.dialog.ratingRequired'))
    return
  }
  if (!hostReviewForm.content.trim()) {
    ElMessage.warning(t('pages.reviews.dialog.contentRequired'))
    return
  }

  const targetIndex = reviewOrders.value.findIndex((order) => order.id === activeOrder.value?.id)
  if (targetIndex >= 0) {
    reviewOrders.value[targetIndex] = {
      ...reviewOrders.value[targetIndex],
      hostReview: {
        rating: hostReviewForm.rating,
        tags: [...hostReviewForm.tags],
        content: hostReviewForm.content.trim(),
        reviewedAt: '2026-05-30',
      },
    }
  }

  hostReviewDialogVisible.value = false
  ElMessage.success(t('pages.reviews.dialog.submitSuccess'))
}

watch([activeTab, keyword, dateRange, roomFilter, guestRatingFilter], () => {
  page.value = 1
})
</script>

<style scoped>
.reviews-page {
  min-height: calc(100vh - 60px);
  padding: 24px;
  background: #f7f8fa;
  color: #1f2937;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.3;
  font-weight: 700;
  color: #111827;
}

.review-tabs {
  padding: 0 4px;
  margin-bottom: 12px;
  background: transparent;
}

.review-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.filter-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 12px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.keyword-input {
  width: 260px;
}

.date-filter {
  width: 280px;
}

.room-filter {
  width: 180px;
}

.rating-filter {
  width: 150px;
}

.reset-button {
  min-width: 96px;
}

.table-panel {
  padding: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.reviews-table {
  width: 100%;
}

.reviews-table :deep(.el-table__header th) {
  height: 48px;
  background: #f8fafc;
  color: #6b7280;
  font-weight: 600;
}

.reviews-table :deep(.el-table__row td) {
  padding: 18px 0;
}

.guest-cell,
.room-cell,
.stay-cell,
.review-content,
.pending-content,
.host-pending {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.guest-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.channel-mark {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
}

.channel-mark.booking {
  color: #003b95;
  background: #e6f0ff;
}

.channel-mark.airbnb {
  color: #ff385c;
  background: #fff0f3;
}

.guest-cell strong,
.room-cell strong,
.stay-cell strong,
.pending-content strong {
  color: #172033;
  font-weight: 650;
}

.guest-cell small,
.room-cell small,
.stay-cell small,
.pending-content span,
.rating-row span {
  color: #6b7280;
  line-height: 1.45;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-content p {
  max-width: 560px;
  margin: 0;
  color: #1f2937;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.host-pending {
  align-items: flex-start;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-top: 1px solid #e5e7eb;
  color: #6b7280;
  font-size: 13px;
}

.dialog-order {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  margin-bottom: 16px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.dialog-order span {
  color: #6b7280;
}

.host-review-form :deep(.el-checkbox-button__inner) {
  margin-bottom: 8px;
}

@media (max-width: 980px) {
  .keyword-input,
  .date-filter,
  .room-filter,
  .rating-filter {
    width: calc(50% - 5px);
  }
}

@media (max-width: 720px) {
  .reviews-page {
    padding: 16px;
  }

  .page-header,
  .pagination-row {
    flex-direction: column;
    align-items: stretch;
  }

  .keyword-input,
  .date-filter,
  .room-filter,
  .rating-filter,
  .reset-button {
    width: 100%;
  }
}
</style>
