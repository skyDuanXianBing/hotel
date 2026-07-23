<template>
  <ion-page class="registration-review-list-route">
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.statistics" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.RegistrationReviews') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page mobile-page--dashboard registration-review-list-page">
      <section class="mobile-hero mobile-dashboard-surface registration-review-list-page__hero">
        <div class="registration-review-list-page__hero-header">
          <div>
            <h1 class="mobile-title">{{ $t('routes.RegistrationReviews') }}</h1>
          </div>
          <ion-button size="small" class="registration-review-list-page__hero-action" @click="handleOpenLinks">
            {{ $t('stage5.dataCenter.registrations.linkList') }}
          </ion-button>
        </div>
        <div class="mobile-chip-row registration-review-list-page__hero-metrics">
          <span class="mobile-chip">{{ $t('stage5DynamicUi.95') }} {{ reviewStore.totalCount }} {{ $t('stage5DynamicUi.125') }}</span>
          <span class="mobile-chip">{{ $t('home.stat.pending.0') }} {{ reviewStore.pendingCount }} {{ $t('stage5DynamicUi.125') }}</span>
          <span class="mobile-chip">{{ $t('stage5.common.status.approved') }} {{ reviewStore.approvedCount }} {{ $t('stage5DynamicUi.125') }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card mobile-dashboard-surface registration-review-list-page__filters">
          <div class="mobile-inline-row registration-review-list-page__filters-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('iosStage5.cleaning.filters') }}</h2>
              <p class="mobile-note registration-review-list-page__filters-note">{{ $t('stage5SourceText.119') }}</p>
            </div>
            <span class="registration-review-list-page__filter-summary">
              {{ $t('stage5DynamicUi.108') }} {{ Object.keys(activeFilters).length }} {{ $t('stage5DynamicUi.141') }}
            </span>
          </div>

          <div class="registration-review-list-page__filter-grid">
            <ion-select v-model="statusFilter" fill="outline" interface="action-sheet" :label="$t('accommodation.common.status')" label-placement="stacked">
              <ion-select-option v-for="option in REVIEW_STATUS_OPTIONS" :key="option.value" :value="option.value">
                {{ $t(option.labelKey) }}
              </ion-select-option>
            </ion-select>

            <ion-select v-model="channelFilter" fill="outline" interface="action-sheet" :label="$t('home.quick.channels.0')" label-placement="stacked">
              <ion-select-option v-for="option in channelOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </ion-select-option>
            </ion-select>

            <div class="registration-review-list-page__date-range">
              <div class="registration-review-list-page__date-field">
                <span>{{ checkInDateFilter || $t('roomStatus.common.checkInDate') }}</span>
                <ion-input v-model="checkInDateFilter" type="date" :aria-label="$t('roomStatus.common.checkInDate')" />
              </div>
              <span class="registration-review-list-page__date-separator" aria-hidden="true">{{ $t('accommodation.common.rangeTo') }}</span>
              <div class="registration-review-list-page__date-field">
                <span>{{ checkOutDateFilter || $t('roomStatus.common.checkOutDate') }}</span>
                <ion-input v-model="checkOutDateFilter" type="date" :aria-label="$t('roomStatus.common.checkOutDate')" />
              </div>
            </div>
          </div>

          <div class="registration-review-list-page__filter-actions">
            <ion-button size="small" @click="handleApplyFilters">{{ $t('order.filters.searchPlaceholder') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleResetFilters">{{ $t('stage5SourceText.163') }}</ion-button>
          </div>

          <p v-if="reviewStore.channelLoadError" class="mobile-note">{{ reviewStore.channelLoadError }}</p>
        </section>

        <section class="mobile-card mobile-dashboard-surface registration-review-list-page__results">
          <div class="mobile-inline-row registration-review-list-page__results-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('stage5SourceText.43') }}</h2>
              <p class="mobile-note">{{ $t('stage5DynamicUi.95') }} {{ reviewStore.totalCount }} {{ $t('stage5DynamicUi.127') }} {{ reviewStore.pendingCount }} {{ $t('stage5DynamicUi.126') }}</p>
            </div>
            <div class="registration-review-list-page__header-actions">
              <ion-button
                fill="clear"
                size="small"
                class="registration-review-list-page__reload-button"
                :aria-label="$t('stage5UiAttributes.32')"
                @click="handleReload"
              >
                <ion-icon slot="icon-only" :icon="refreshOutline" />
              </ion-button>
            </div>
          </div>

          <div v-if="shouldShowInitialLoading" class="registration-review-list-page__loading">
            <ion-spinner name="crescent" />
            <p class="mobile-note">{{ $t('stage5SourceText.156') }}</p>
          </div>

          <div v-else-if="reviewStore.loadError" class="registration-review-list-page__error-state">
            <p class="mobile-note">{{ reviewStore.loadError }}</p>
            <ion-button fill="outline" size="small" @click="handleReload">{{ $t('storeSelection.reload') }}</ion-button>
          </div>

          <div v-else-if="reviewStore.records.length > 0" class="mobile-list pms-list registration-review-list-page__list">
            <button
              v-for="record in reviewStore.records"
              :key="record.formId"
              type="button"
              class="registration-review-list-page__item"
              :class="{ 'is-pending': record.status === 'pending' }"
              @click="handleOpenDetail(record.formId)"
            >
              <div class="registration-review-list-page__item-header">
                <div class="registration-review-list-page__item-heading">
                  <strong class="registration-review-list-page__guest-name">{{ record.guestName }}</strong>
                  <div class="registration-review-list-page__item-badges">
                    <span
                      class="registration-review-list-page__channel"
                      :style="resolveChannelBadgeStyle(record.channelName)"
                    >
                      {{ record.channelName }}
                    </span>
                    <span
                      class="registration-review-list-page__room"
                      :class="{
                        'is-unassigned': !record.roomNumber || record.roomNumber === $t('home.stat.unassigned.0'),
                        'is-assigned': record.roomNumber && record.roomNumber !== $t('home.stat.unassigned.0'),
                      }"
                    >
                      {{ record.roomLabel }}
                    </span>
                  </div>
                </div>
                <span class="registration-review-list-page__status" :class="`is-${record.status}`">
                  {{ getReviewStatusLabel(record.status) }}
                </span>
              </div>

              <div class="registration-review-list-page__stay-grid">
                <span>
                  <span class="registration-review-list-page__meta-label">{{ $t('roomStatus.action.checkIn') }}</span>
                  <span class="registration-review-list-page__meta-value">{{ record.checkInDate }}</span>
                </span>
                <span>
                  <span class="registration-review-list-page__meta-label">{{ $t('roomStatus.hoverCard.checkOutDate') }}</span>
                  <span class="registration-review-list-page__meta-value">{{ record.checkOutDate }}</span>
                </span>
              </div>

              <div class="registration-review-list-page__time-grid">
                <span>
                  <span class="registration-review-list-page__meta-label">{{ $t('stage5.common.actions.submit') }}</span>
                  <span class="registration-review-list-page__meta-value">{{ record.submittedAt }}</span>
                </span>
                <span>
                  <span class="registration-review-list-page__meta-label">{{ $t('stage5SourceText.137') }}</span>
                  <span class="registration-review-list-page__meta-value">{{ record.updatedAt }}</span>
                </span>
              </div>

              <div class="registration-review-list-page__notes">
                <p>{{ $t('stage5.dataCenter.detail.metaOrderNumber') }}{{ record.orderNumber || '—' }}</p>
                <p>{{ $t('stage5DynamicUi.132') }}{{ record.channelOrderNumber || '—' }}</p>
              </div>
            </button>
          </div>

          <p v-else class="mobile-note registration-review-list-page__empty">{{ $t('stage5SourceText.89') }}</p>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { refreshOutline } from 'ionicons/icons'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { type RegistrationReviewListParams } from '@/api/review'
import { REVIEW_STATUS_OPTIONS, getReviewStatusLabel, type ReviewFilterStatus } from '@/constants/reviews'
import { ROUTE_PATHS } from '@/router/guards'
import { useReviewStore } from '@/stores/reviews'

const router = useRouter()
const { t } = useI18n()
const reviewStore = useReviewStore()

const statusFilter = ref<ReviewFilterStatus>('all')
const channelFilter = ref('all')
const checkInDateFilter = ref('')
const checkOutDateFilter = ref('')

const DEFAULT_CHANNEL_COLOR = '#2949ff'

const channelOptions = computed(() => {
  const options = [{ label: t('settingsResidual.autoMessages.allChannels'), value: 'all' }]

  for (const channel of reviewStore.channels) {
    options.push({
      label: channel.name,
      value: String(channel.id),
    })
  }

  return options
})

const channelColorMap = computed(() => {
  const result = new Map<string, string>()

  for (const channel of reviewStore.channels) {
    const name = normalizeChannelLookupValue(channel.name)
    const color = normalizeChannelColor(channel.color)

    if (name && color) {
      result.set(name, color)
    }
  }

  return result
})

const activeFilters = computed<RegistrationReviewListParams>(() => {
  const nextFilters: RegistrationReviewListParams = {}

  if (statusFilter.value !== 'all') {
    nextFilters.status = statusFilter.value
  }

  if (channelFilter.value !== 'all') {
    nextFilters.channelId = Number(channelFilter.value)
  }

  if (checkInDateFilter.value) {
    nextFilters.checkInDate = checkInDateFilter.value
  }

  if (checkOutDateFilter.value) {
    nextFilters.checkOutDate = checkOutDateFilter.value
  }

  return nextFilters
})

const shouldShowInitialLoading = computed(() => {
  return reviewStore.isLoading && !reviewStore.hasLoaded && reviewStore.records.length === 0
})

onMounted(async () => {
  const tasks: Promise<unknown>[] = []

  if (!reviewStore.hasLoadedChannels) {
    tasks.push(reviewStore.refreshChannels())
  }

  if (!reviewStore.hasLoaded) {
    tasks.push(reviewStore.refreshRecords(activeFilters.value))
    await Promise.all(tasks)
  } else if (tasks.length > 0) {
    await Promise.all(tasks)
  }
})

async function handleResetFilters() {
  statusFilter.value = 'all'
  channelFilter.value = 'all'
  checkInDateFilter.value = ''
  checkOutDateFilter.value = ''

  await reviewStore.refreshRecords()
}

async function handleApplyFilters() {
  await reviewStore.refreshRecords(activeFilters.value)
}

async function handleReload() {
  await Promise.all([reviewStore.refreshChannels(), reviewStore.refreshRecords(activeFilters.value)])
}

async function handleOpenDetail(formId: string) {
  await router.push({
    name: 'RegistrationReviewDetail',
    params: { formId },
  })
}

async function handleOpenLinks() {
  await router.push({
    name: 'RegistrationReviewLinks',
  })
}

function normalizeChannelLookupValue(value?: string) {
  return (value || '').trim().toLowerCase()
}

function normalizeChannelColor(value?: string) {
  const color = (value || '').trim()
  return /^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/i.test(color) ? color : ''
}

function resolveChannelBadgeStyle(channelName: string) {
  return {
    '--review-channel-color':
      channelColorMap.value.get(normalizeChannelLookupValue(channelName)) || DEFAULT_CHANNEL_COLOR,
  }
}
</script>

<style scoped>
.registration-review-list-route {
  background: var(--ios-pms-dashboard-page-background);
}

.registration-review-list-page {
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 10px;
  --padding-bottom: calc(36px + var(--app-safe-bottom));
  background: var(--background);
}

.registration-review-list-page__hero {
  display: grid;
  gap: 14px;
  padding: 16px 16px 20px;
  margin-bottom: 8px;
  border-radius: 20px;
}

.registration-review-list-page__hero::before {
  display: none;
}

.registration-review-list-page__hero-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ios-pms-space-3);
  min-width: 0;
}

.registration-review-list-page__hero-header > div,
.registration-review-list-page__filters-header > div,
.registration-review-list-page__results-header > div,
.registration-review-list-page__item-header > div {
  min-width: 0;
}

.registration-review-list-page__hero-header > div {
  display: flex;
  align-items: center;
  min-height: 30px;
}

.registration-review-list-page__hero :deep(.mobile-title) {
  margin: 0;
  color: #333333;
  font-size: 24px;
  font-weight: 540;
  line-height: 1.2;
  letter-spacing: 0;
}

.registration-review-list-page__hero-action {
  --background: #218cff;
  --background-activated: #147be9;
  --background-hover: #218cff;
  --border-radius: 7px;
  --box-shadow: none;
  --color: #ffffff;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 0;
  --padding-bottom: 0;
  flex-shrink: 0;
  height: 30px;
  min-height: 30px;
  margin: 0;
  font-size: 15px;
  font-weight: 400;
}

.registration-review-list-page__hero-action::part(native) {
  white-space: nowrap;
}

.registration-review-list-page__hero-metrics {
  gap: 8px;
  margin-top: 0;
}

.registration-review-list-page__hero-metrics .mobile-chip {
  min-height: 26px;
  padding: 0 9px;
  border: 1px solid #d5d5d5;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.72);
  color: #666666;
  font-size: 14px;
  font-weight: 400;
  line-height: 1;
}

.registration-review-list-page :deep(.mobile-stack) {
  gap: 8px;
}

.registration-review-list-page__filters {
  display: grid;
  gap: 16px;
  padding: 16px;
  border-radius: 20px;
}

.registration-review-list-page__filters-header {
  align-items: flex-start;
  min-width: 0;
}

.registration-review-list-page__filters :deep(.mobile-section-title),
.registration-review-list-page__results :deep(.mobile-section-title) {
  margin: 0 0 3px;
  color: #333333;
  font-size: 22px;
  font-weight: 540;
  line-height: 1.25;
  letter-spacing: 0;
}

.registration-review-list-page__filters-note {
  color: #a1a1a1;
  font-size: 13px;
  line-height: 1.45;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.registration-review-list-page__filter-summary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 1 auto;
  min-width: 0;
  max-width: 100%;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid #d9ebff;
  border-radius: 12px;
  background: #eaf5ff;
  color: #287cff;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.registration-review-list-page__filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 8px;
  align-items: start;
}

.registration-review-list-page__filter-grid :deep(ion-select) {
  --background: #ffffff;
  --border-color: #dedede;
  --border-radius: 10px;
  --border-width: 1px;
  --highlight-color-focused: #218cff;
  --padding-start: 14px;
  --padding-end: 14px;
  min-height: 62px;
  color: #666666;
}

.registration-review-list-page__filter-grid :deep(.label-text-wrapper) {
  margin-bottom: 4px;
}

.registration-review-list-page__filter-grid :deep(.label-text) {
  color: #333333;
  font-size: 13px;
  font-weight: 400;
}

.registration-review-list-page__filter-grid :deep(ion-select::part(text)),
.registration-review-list-page__filter-grid :deep(ion-select::part(placeholder)) {
  color: #666666;
  font-size: 16px;
  font-weight: 400;
}

.registration-review-list-page__filter-grid :deep(ion-select::part(icon)) {
  color: #888888;
  opacity: 1;
}

.registration-review-list-page__date-range {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: minmax(0, 1fr) 18px minmax(0, 1fr);
  align-items: center;
  min-height: 48px;
  padding: 0 18px;
  border: 1px solid #dedede;
  border-radius: 24px;
  background: #ffffff;
}

.registration-review-list-page__date-field {
  position: relative;
  display: grid;
  place-items: center;
  min-width: 0;
  min-height: 46px;
  color: #333333;
  font-size: 14px;
  font-weight: 400;
  text-align: center;
}

.registration-review-list-page__date-field:first-child {
  place-items: center end;
  padding-right: 18px;
}

.registration-review-list-page__date-field:last-child {
  place-items: center start;
  padding-left: 18px;
}

.registration-review-list-page__date-field > span {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.registration-review-list-page__date-field :deep(ion-input) {
  position: absolute;
  inset: 0;
  z-index: 1;
  width: 100%;
  min-height: 46px;
  opacity: 0;
  cursor: pointer;
}

.registration-review-list-page__date-separator {
  display: grid;
  place-items: center;
  width: 18px;
  color: #999999;
  font-size: 16px;
  text-align: center;
}

.registration-review-list-page__filter-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
  padding-top: 12px;
  border-top: 1px solid #d9d9d9;
}

.registration-review-list-page__filter-actions :deep(ion-button) {
  --border-radius: 10px;
  --box-shadow: none;
  --padding-top: 0;
  --padding-bottom: 0;
  height: 32px;
  min-height: 32px;
  margin: 0;
  font-size: 15px;
  font-weight: 500;
}

.registration-review-list-page__filter-actions :deep(ion-button::part(native)) {
  min-height: 32px;
  padding-top: 0;
  padding-bottom: 0;
}

.registration-review-list-page__filter-actions :deep(ion-button.button-solid) {
  --background: #2b6cff;
  --background-activated: #1d5be4;
  --padding-start: 14px;
  --padding-end: 14px;
}

.registration-review-list-page__filter-actions :deep(ion-button.button-outline) {
  --background: #ffffff;
  --border-color: #d2d2d2;
  --border-width: 1px;
  --color: #2455ee;
  --padding-start: 13px;
  --padding-end: 13px;
}

.registration-review-list-page__results {
  padding: 16px 16px 24px;
  border-radius: 20px;
}

.registration-review-list-page__results-header {
  align-items: flex-start;
  margin-bottom: 16px;
  min-width: 0;
}

.registration-review-list-page__results-header .mobile-note {
  color: #a1a1a1;
  font-size: 13px;
  line-height: 1.45;
}

.registration-review-list-page__header-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
}

.registration-review-list-page__reload-button {
  --background: rgba(33, 140, 255, 0.08);
  --background-activated: rgba(33, 140, 255, 0.14);
  --border-radius: 10px;
  --box-shadow: none;
  --color: #218cff;
  --padding-start: 0;
  --padding-end: 0;
  width: 34px;
  height: 34px;
  min-height: 34px;
  margin: 0;
}

.registration-review-list-page__reload-button ion-icon {
  font-size: 18px;
}

.registration-review-list-page__list {
  gap: 16px;
}

.registration-review-list-page__item {
  width: 100%;
  padding: 18px 16px 20px;
  border: none;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 16px rgba(75, 95, 130, 0.09);
  text-align: left;
  color: inherit;
  cursor: pointer;
  min-width: 0;
  overflow: hidden;
}

.registration-review-list-page__item.is-pending {
  background: rgba(255, 255, 255, 0.96);
}

.registration-review-list-page__item-header {
  display: flex;
  justify-content: space-between;
  gap: var(--ios-pms-space-3);
  align-items: flex-start;
  min-width: 0;
}

.registration-review-list-page__item-heading {
  min-width: 0;
}

.registration-review-list-page__guest-name {
  display: block;
  overflow: hidden;
  color: #333333;
  font-size: 18px;
  font-weight: 590;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.registration-review-list-page__item-badges {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  margin-top: 5px;
}

.registration-review-list-page__channel,
.registration-review-list-page__room {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 22px;
  padding: 2px 10px;
  border-radius: 5px;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  white-space: nowrap;
}

.registration-review-list-page__channel {
  max-width: 132px;
  overflow: hidden;
  background: var(--review-channel-color, #2949ff);
  color: #ffffff;
  text-overflow: ellipsis;
}

.registration-review-list-page__room {
  max-width: 116px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.registration-review-list-page__room.is-unassigned {
  background: #fff1f3;
  color: #ff2525;
}

.registration-review-list-page__room.is-assigned {
  background: #effbf6;
  color: #43bc8a;
}

.registration-review-list-page__stay-grid,
.registration-review-list-page__time-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 12px;
  margin-top: 14px;
}

.registration-review-list-page__stay-grid > span,
.registration-review-list-page__time-grid > span {
  display: flex;
  align-items: baseline;
  min-width: 0;
}

.registration-review-list-page__stay-grid > span:nth-child(even),
.registration-review-list-page__time-grid > span:nth-child(even) {
  justify-content: flex-end;
}

.registration-review-list-page__meta-label {
  flex-shrink: 0;
  margin-right: 4px;
  color: #777777;
  font-size: 14px;
  font-weight: 400;
}

.registration-review-list-page__meta-value {
  min-width: 0;
  color: #333333;
  font-size: 16px;
  font-weight: 400;
  line-height: 1.35;
  white-space: nowrap;
}

.registration-review-list-page__time-grid .registration-review-list-page__meta-value {
  overflow: hidden;
  overflow-wrap: normal;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.registration-review-list-page__time-grid {
  grid-template-columns: minmax(0, 1fr) max-content;
  column-gap: 8px;
}

.registration-review-list-page__time-grid .registration-review-list-page__meta-label {
  margin-right: 2px;
  font-size: 13px;
}

.registration-review-list-page__time-grid > span:nth-child(even) {
  justify-self: end;
  white-space: nowrap;
}

.registration-review-list-page__time-grid > span:nth-child(even) .registration-review-list-page__meta-value {
  text-align: right;
}

.registration-review-list-page__notes {
  display: grid;
  gap: 7px;
  margin-top: 14px;
}

.registration-review-list-page__notes p {
  margin: 0;
  overflow-wrap: anywhere;
  color: #a0a0a0;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.55;
}

.registration-review-list-page__loading,
.registration-review-list-page__error-state {
  display: grid;
  justify-items: center;
  gap: var(--ios-pms-space-2);
  padding: 20px 0;
}

.registration-review-list-page__status {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 80px;
  min-height: 30px;
  padding: 0 12px;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 400;
}

.registration-review-list-page__status.is-pending {
  background: #fff0db;
  color: #e58a14;
}

.registration-review-list-page__status.is-draft {
  background: #e3e3e3;
  color: #666666;
}

.registration-review-list-page__status.is-approved {
  background: #58bd93;
  color: #ffffff;
}

.registration-review-list-page__status.is-rejected {
  background: #ef7777;
  color: #ffffff;
}

.registration-review-list-page__empty {
  padding: 16px 0;
  color: #999999;
}

@media (max-width: 360px) {
  .registration-review-list-page__hero-header,
  .registration-review-list-page__filters-header,
  .registration-review-list-page__results-header {
    display: grid;
  }

  .registration-review-list-page__filter-grid,
  .registration-review-list-page__stay-grid,
  .registration-review-list-page__time-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .registration-review-list-page__filter-summary {
    justify-self: flex-start;
    font-size: 11px;
  }

  .registration-review-list-page__stay-grid > span:nth-child(even),
  .registration-review-list-page__time-grid > span:nth-child(even) {
    justify-content: flex-start;
  }

  .registration-review-list-page__status {
    min-width: 72px;
    padding: 0 10px;
  }

  .registration-review-list-page__guest-name {
    font-size: 17px;
  }

}
</style>
