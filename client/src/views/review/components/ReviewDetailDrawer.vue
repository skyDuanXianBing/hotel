<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RefreshRight, WarningFilled } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  REVIEW_ALLOWED_ACTION,
  getReviewById,
  type ReviewActionAudit,
  type SuReviewDetailDTO,
} from '@/api/suReviews'
import {
  formatReviewDate,
  formatReviewDateTime,
  getReplyText,
  getReservationContext,
  getReviewErrorMessage,
  getReviewScore,
  getReviewText,
  hasAllowedReviewAction,
  isAirbnbReview,
  normalizeCategoryRatings,
  normalizeChannelCode,
} from '@/views/review/reviewPresentation'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    reviewId: number | string | null
    refreshToken?: number
  }>(),
  {
    refreshToken: 0,
  },
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'reply', review: SuReviewDetailDTO): void
  (event: 'guest-review', review: SuReviewDetailDTO): void
}>()

const { t, te, locale } = useI18n()
const detail = ref<SuReviewDetailDTO | null>(null)
const loading = ref(false)
const errorMessage = ref('')
let requestSequence = 0

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const reservation = computed(() => (detail.value ? getReservationContext(detail.value) : null))
const score = computed(() => (detail.value ? getReviewScore(detail.value) : null))
const categoryRatings = computed(() => normalizeCategoryRatings(detail.value?.categoryRatings))
const canReply = computed(() => hasAllowedReviewAction(detail.value, REVIEW_ALLOWED_ACTION.REPLY))
const canReviewGuest = computed(
  () =>
    isAirbnbReview(detail.value) &&
    hasAllowedReviewAction(detail.value, REVIEW_ALLOWED_ACTION.GUEST_REVIEW),
)

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

const channelLabel = computed(() => {
  const channelCode = normalizeChannelCode(detail.value?.channelCode)
  const key = `suReviews.channels.${channelCode}`
  return te(key) ? t(key) : detail.value?.channelCode || emptyLabel.value
})

const channelClass = computed(() => {
  const channelCode = normalizeChannelCode(detail.value?.channelCode)
  return channelCode === 'AIRBNB' ? 'is-airbnb' : channelCode === 'BOOKING' ? 'is-booking' : ''
})

const statusTagType = (status?: string | null) => {
  const normalized = String(status || '').toUpperCase()
  if (['COMPLETED', 'CONFIRMED', 'PUBLISHED', 'LINKED'].includes(normalized)) {
    return 'success'
  }
  if (['FAILED', 'UNLINKED', 'AMBIGUOUS'].includes(normalized)) {
    return 'danger'
  }
  if (['PENDING', 'SUBMITTED', 'IN_REVIEW', 'CREATED'].includes(normalized)) {
    return 'warning'
  }
  return 'info'
}

const getAuditTime = (audit: ReviewActionAudit) =>
  audit.confirmedAt || audit.submittedAt || audit.createdAt

const loadDetail = async () => {
  if (!props.reviewId || !props.modelValue) {
    return
  }

  const sequence = ++requestSequence
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getReviewById(props.reviewId)
    if (sequence !== requestSequence) {
      return
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || t('suReviews.errors.detail'))
    }
    detail.value = response.data
  } catch (error) {
    if (sequence !== requestSequence) {
      return
    }
    detail.value = null
    errorMessage.value = getReviewErrorMessage(error, t('suReviews.errors.detail'))
  } finally {
    if (sequence === requestSequence) {
      loading.value = false
    }
  }
}

watch(
  () => [props.modelValue, props.reviewId, props.refreshToken] as const,
  ([open]) => {
    if (open) {
      void loadDetail()
      return
    }
    requestSequence += 1
    detail.value = null
    errorMessage.value = ''
    loading.value = false
  },
  { immediate: true },
)

const handleReply = () => {
  if (detail.value && canReply.value) {
    emit('reply', detail.value)
  }
}

const handleGuestReview = () => {
  if (detail.value && canReviewGuest.value) {
    emit('guest-review', detail.value)
  }
}
</script>

<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="min(720px, 96vw)"
    destroy-on-close
    class="review-detail-drawer"
  >
    <template #header>
      <div class="drawer-heading">
        <div class="drawer-heading-topline">
          <span class="channel-chip" :class="channelClass">{{ channelLabel }}</span>
          <span v-if="detail?.orderNumber" class="drawer-order-number">
            {{ detail.orderNumber }}
          </span>
        </div>
        <h2>{{ t('suReviews.detail.title') }}</h2>
        <p>{{ t('suReviews.detail.subtitle') }}</p>
      </div>
    </template>

    <div v-if="loading" class="drawer-loading" aria-live="polite">
      <el-skeleton :rows="10" animated />
      <span class="sr-only">{{ t('suReviews.detail.loading') }}</span>
    </div>

    <div v-else-if="errorMessage" class="drawer-error">
      <el-alert :title="errorMessage" type="error" :closable="false" show-icon />
      <el-button :icon="RefreshRight" @click="loadDetail">
        {{ t('suReviews.actions.retry') }}
      </el-button>
    </div>

    <div v-else-if="detail" class="drawer-content">
      <section class="detail-section order-section">
        <div class="section-heading">
          <span class="section-kicker">01</span>
          <h3>{{ t('suReviews.detail.orderSection') }}</h3>
        </div>
        <div class="detail-grid">
          <div class="detail-field">
            <span>{{ t('suReviews.labels.orderNumber') }}</span>
            <strong>{{ reservation?.orderNumber || emptyLabel }}</strong>
          </div>
          <div class="detail-field">
            <span>{{ t('suReviews.labels.channelBookingId') }}</span>
            <strong>{{ reservation?.channelOrderNumber || emptyLabel }}</strong>
          </div>
          <div class="detail-field">
            <span>{{ t('suReviews.labels.guestName') }}</span>
            <strong>{{ reservation?.guestName || emptyLabel }}</strong>
          </div>
          <div class="detail-field">
            <span>{{ t('suReviews.labels.reservationStatus') }}</span>
            <el-tag size="small" effect="plain">
              {{ reservation?.status || emptyLabel }}
            </el-tag>
          </div>
          <div class="detail-field detail-field--wide">
            <span>{{ t('suReviews.labels.stayDates') }}</span>
            <strong
              >{{ formatDate(reservation?.checkInDate) }} →
              {{ formatDate(reservation?.checkOutDate) }}</strong
            >
          </div>
          <div class="detail-field detail-field--wide">
            <span>{{ t('suReviews.labels.propertyName') }}</span>
            <strong>{{ detail.propertyName || emptyLabel }}</strong>
          </div>
        </div>
      </section>

      <section class="detail-section">
        <div class="section-heading">
          <span class="section-kicker">02</span>
          <h3>{{ t('suReviews.detail.reviewSection') }}</h3>
        </div>

        <div class="review-meta-row">
          <el-tag :type="statusTagType(detail.reviewStatus)" effect="light">
            {{ translateEnum('reviewStatuses', detail.reviewStatus) }}
          </el-tag>
          <span>{{ translateEnum('reviewTypes', detail.reviewType) }}</span>
          <span v-if="score !== null" class="score-pill">
            {{ t('suReviews.labels.score') }} {{ score }}
          </span>
        </div>

        <div v-if="detail.reviewTitle" class="review-title">{{ detail.reviewTitle }}</div>

        <div class="content-block">
          <span>{{ t('suReviews.labels.reviewText') }}</span>
          <p>{{ getReviewText(detail) || emptyLabel }}</p>
        </div>

        <div v-if="detail.negativeReviewText" class="content-block content-block--negative">
          <span>{{ t('suReviews.labels.negativeReviewText') }}</span>
          <p>{{ detail.negativeReviewText }}</p>
        </div>

        <div v-if="detail.privateFeedback" class="content-block content-block--private">
          <span>{{ t('suReviews.labels.privateFeedback') }}</span>
          <p>{{ detail.privateFeedback }}</p>
        </div>

        <div v-if="categoryRatings.length" class="rating-list">
          <span class="rating-list-title">{{ t('suReviews.labels.categoryRatings') }}</span>
          <div class="rating-list-grid">
            <div v-for="item in categoryRatings" :key="item.category" class="rating-item">
              <span>{{ translateEnum('categories', item.category) }}</span>
              <strong>{{ item.rating }}</strong>
            </div>
          </div>
        </div>

        <div v-if="getReplyText(detail)" class="content-block content-block--reply">
          <span>{{ t('suReviews.labels.replyText') }}</span>
          <p>{{ getReplyText(detail) }}</p>
        </div>
      </section>

      <section class="detail-section">
        <div class="section-heading">
          <span class="section-kicker">03</span>
          <h3>{{ t('suReviews.detail.capabilitySection') }}</h3>
        </div>

        <div class="capability-statuses">
          <div class="status-line">
            <span>{{ t('suReviews.labels.associationStatus') }}</span>
            <el-tag :type="statusTagType(detail.associationStatus)" effect="plain">
              {{ translateEnum('associationStatuses', detail.associationStatus) }}
            </el-tag>
          </div>
          <div class="status-line">
            <span>{{ t('suReviews.labels.lastActionStatus') }}</span>
            <el-tag :type="statusTagType(detail.lastActionStatus)" effect="plain">
              {{ translateEnum('actionStatuses', detail.lastActionStatus) }}
            </el-tag>
          </div>
          <div class="status-line">
            <span>{{ t('suReviews.labels.receivedAt') }}</span>
            <strong>{{ formatDateTime(detail.receivedAt) }}</strong>
          </div>
          <div class="status-line">
            <span>{{ t('suReviews.labels.lastSyncedAt') }}</span>
            <strong>{{ formatDateTime(detail.lastSyncedAt) }}</strong>
          </div>
        </div>

        <el-alert
          v-if="detail.associationReason"
          :title="detail.associationReason"
          type="warning"
          :closable="false"
          show-icon
          class="association-alert"
        >
          <template #icon>
            <el-icon><WarningFilled /></el-icon>
          </template>
        </el-alert>

        <div class="allowed-action-list">
          <el-tag v-if="canReply" type="success" effect="light">
            {{ t('suReviews.eligibility.reply') }}
          </el-tag>
          <el-tag v-if="canReviewGuest" type="success" effect="light">
            {{ t('suReviews.eligibility.guestReview') }}
          </el-tag>
          <el-tag v-if="!canReply && !canReviewGuest" type="info" effect="plain">
            {{ t('suReviews.eligibility.none') }}
          </el-tag>
        </div>

        <div
          v-if="detail.actionReasons && Object.keys(detail.actionReasons).length"
          class="reason-list"
        >
          <p v-for="(reason, action) in detail.actionReasons" :key="action">
            <strong>{{ action }}</strong>
            <span>{{ reason }}</span>
          </p>
        </div>
      </section>

      <section class="detail-section">
        <div class="section-heading">
          <span class="section-kicker">04</span>
          <h3>{{ t('suReviews.detail.auditSection') }}</h3>
        </div>

        <el-timeline v-if="detail.actions?.length" class="audit-timeline">
          <el-timeline-item
            v-for="audit in detail.actions"
            :key="audit.id || `${audit.actionType}-${audit.createdAt}`"
            :type="statusTagType(audit.status)"
            :timestamp="formatDateTime(getAuditTime(audit))"
            placement="top"
          >
            <div class="audit-card">
              <div class="audit-card-heading">
                <strong>{{ audit.actionType || emptyLabel }}</strong>
                <el-tag size="small" :type="statusTagType(audit.status)" effect="plain">
                  {{ translateEnum('actionStatuses', audit.status) }}
                </el-tag>
              </div>
              <p v-if="audit.responseMessage">{{ audit.responseMessage }}</p>
              <p v-if="audit.errorCode" class="audit-error">
                {{ audit.errorCode }}
              </p>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else :description="t('suReviews.detail.noAudit')" :image-size="72" />
      </section>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="visible = false">{{ t('suReviews.actions.close') }}</el-button>
        <el-button v-if="canReply" type="primary" plain @click="handleReply">
          {{ t('suReviews.actions.reply') }}
        </el-button>
        <el-button v-if="canReviewGuest" type="primary" @click="handleGuestReview">
          {{ t('suReviews.actions.guestReview') }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.drawer-heading {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.drawer-heading-topline {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.channel-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 9px;
  border-radius: 999px;
  background: #eef1ee;
  color: #5f6761;
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

.drawer-order-number {
  overflow: hidden;
  color: #848a85;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-heading h2 {
  margin: 0;
  color: #202421;
  font-size: 22px;
  line-height: 1.25;
}

.drawer-heading p {
  margin: 0;
  color: #7b817c;
  font-size: 13px;
}

.drawer-loading,
.drawer-error {
  padding: 8px 2px 24px;
}

.drawer-error {
  display: grid;
  justify-items: start;
  gap: 14px;
}

.drawer-content {
  display: grid;
  gap: 16px;
  padding-bottom: 12px;
}

.detail-section {
  padding: 20px;
  border: 1px solid #e7e9e5;
  border-radius: 16px;
  background: #ffffff;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.section-heading h3 {
  margin: 0;
  color: #292e2a;
  font-size: 15px;
}

.section-kicker {
  color: #2f7cf6;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 24px;
}

.detail-field {
  display: grid;
  align-content: start;
  justify-items: start;
  gap: 5px;
  min-width: 0;
}

.detail-field--wide {
  grid-column: span 2;
}

.detail-field > span,
.content-block > span,
.rating-list-title {
  color: #858b86;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.detail-field strong {
  max-width: 100%;
  overflow-wrap: anywhere;
  color: #333834;
  font-size: 13px;
  font-weight: 650;
}

.review-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 14px;
  color: #737a74;
  font-size: 12px;
}

.score-pill {
  padding: 4px 9px;
  border-radius: 999px;
  background: #fff8df;
  color: #906d00;
  font-weight: 700;
}

.review-title {
  margin-bottom: 10px;
  color: #242925;
  font-size: 17px;
  font-weight: 750;
}

.content-block {
  display: grid;
  gap: 7px;
  margin-top: 10px;
  padding: 14px;
  border-radius: 12px;
  background: #f7f8f5;
}

.content-block p {
  margin: 0;
  color: #424843;
  font-size: 13px;
  line-height: 1.7;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.content-block--negative {
  background: #fff7f4;
}

.content-block--private {
  background: #f5f2ff;
}

.content-block--reply {
  background: #eef6ff;
}

.rating-list {
  display: grid;
  gap: 9px;
  margin-top: 16px;
}

.rating-list-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.rating-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 10px;
  border: 1px solid #ebece8;
  border-radius: 10px;
  color: #6d746e;
  font-size: 12px;
}

.rating-item strong {
  color: #2f7cf6;
}

.capability-statuses {
  display: grid;
  gap: 10px;
}

.status-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
  color: #838984;
  font-size: 12px;
}

.status-line strong {
  color: #4b514c;
  font-size: 12px;
  text-align: right;
}

.association-alert {
  margin-top: 14px;
}

.allowed-action-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.reason-list {
  display: grid;
  gap: 7px;
  margin-top: 12px;
}

.reason-list p {
  display: grid;
  grid-template-columns: minmax(96px, auto) 1fr;
  gap: 10px;
  margin: 0;
  color: #737a74;
  font-size: 12px;
  line-height: 1.5;
}

.reason-list strong {
  color: #4a514b;
}

.audit-timeline {
  padding-top: 4px;
}

.audit-card {
  padding: 12px 14px;
  border: 1px solid #eceeea;
  border-radius: 12px;
  background: #fafbf9;
}

.audit-card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.audit-card p {
  margin: 7px 0 0;
  color: #656c66;
  font-size: 12px;
  line-height: 1.5;
}

.audit-card .audit-error {
  color: #c64236;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 600px) {
  .detail-grid,
  .rating-list-grid {
    grid-template-columns: 1fr;
  }

  .detail-field--wide {
    grid-column: auto;
  }

  .drawer-footer {
    flex-wrap: wrap;
  }
}
</style>
