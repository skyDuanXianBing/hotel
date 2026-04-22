<template>
  <ion-page class="registration-review-list-route">
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.statistics" />
        </ion-buttons>
        <ion-title class="mobile-toolbar-title">审查</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page registration-review-list-page">
      <section class="mobile-hero registration-review-list-page__hero">
        <div class="registration-review-list-page__hero-header">
          <div>
            <h1 class="mobile-title">入住登记审查</h1>
          </div>
          <ion-button fill="outline" size="small" class="registration-review-list-page__hero-action" @click="handleOpenLinks">
            链接列表
          </ion-button>
        </div>
        <div class="mobile-chip-row registration-review-list-page__hero-metrics">
          <span class="mobile-chip">共 {{ reviewStore.totalCount }} 条</span>
          <span class="mobile-chip">待处理 {{ reviewStore.pendingCount }} 条</span>
          <span class="mobile-chip">已通过 {{ reviewStore.approvedCount }} 条</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card registration-review-list-page__filters">
          <div class="mobile-inline-row registration-review-list-page__filters-header">
            <div>
              <h2 class="mobile-section-title">筛选条件</h2>
              <p class="mobile-note registration-review-list-page__filters-note">按状态、渠道与入住日期收窄任务范围。</p>
            </div>
            <span class="registration-review-list-page__filter-summary">
              已筛选 {{ Object.keys(activeFilters).length }} 项
            </span>
          </div>

          <div class="registration-review-list-page__filter-grid">
            <ion-select v-model="statusFilter" fill="outline" interface="action-sheet" label="状态" label-placement="stacked">
              <ion-select-option v-for="option in REVIEW_STATUS_OPTIONS" :key="option.value" :value="option.value">
                {{ option.label }}
              </ion-select-option>
            </ion-select>

            <ion-select v-model="channelFilter" fill="outline" interface="action-sheet" label="渠道" label-placement="stacked">
              <ion-select-option v-for="option in channelOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </ion-select-option>
            </ion-select>

            <ion-input v-model="checkInDateFilter" type="date" fill="outline" label="入住日期" label-placement="stacked" />
            <ion-input v-model="checkOutDateFilter" type="date" fill="outline" label="离店日期" label-placement="stacked" />
          </div>

          <div class="registration-review-list-page__filter-actions">
            <ion-button size="small" @click="handleApplyFilters">搜索</ion-button>
            <ion-button fill="outline" size="small" @click="handleResetFilters">清空条件</ion-button>
          </div>

          <p v-if="reviewStore.channelLoadError" class="mobile-note">{{ reviewStore.channelLoadError }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row registration-review-list-page__results-header">
            <div>
              <h2 class="mobile-section-title">审查列表</h2>
              <p class="mobile-note">共 {{ reviewStore.totalCount }} 条，待处理 {{ reviewStore.pendingCount }} 条。</p>
            </div>
            <div class="registration-review-list-page__header-actions">
              <ion-button fill="outline" size="small" @click="handleReload">刷新</ion-button>
            </div>
          </div>

          <div v-if="shouldShowInitialLoading" class="registration-review-list-page__loading">
            <ion-spinner name="crescent" />
            <p class="mobile-note">正在加载审查列表...</p>
          </div>

          <div v-else-if="reviewStore.loadError" class="registration-review-list-page__error-state">
            <p class="mobile-note">{{ reviewStore.loadError }}</p>
            <ion-button fill="outline" size="small" @click="handleReload">重新加载</ion-button>
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
                <div>
                  <strong class="registration-review-list-page__guest-name">{{ record.guestName }}</strong>
                  <p>{{ record.roomLabel }} · {{ record.channelName }}</p>
                </div>
                <span class="registration-review-list-page__status" :class="`is-${record.status}`">
                  {{ getReviewStatusLabel(record.status) }}
                </span>
              </div>

              <div class="registration-review-list-page__meta-grid">
                <span>入住 {{ record.checkInDate }}</span>
                <span>离店 {{ record.checkOutDate }}</span>
                <span>提交 {{ record.submittedAt }}</span>
                <span>更新 {{ record.updatedAt }}</span>
              </div>

              <div class="registration-review-list-page__notes">
                <p>订单号：{{ record.orderNumber || '—' }}</p>
                <p>渠道单号：{{ record.channelOrderNumber || '—' }}</p>
              </div>
            </button>
          </div>

          <p v-else class="mobile-note registration-review-list-page__empty">当前条件下暂无审查记录。</p>
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
  IonInput,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type RegistrationReviewListParams } from '@/api/review'
import { REVIEW_STATUS_OPTIONS, getReviewStatusLabel, type ReviewFilterStatus } from '@/constants/reviews'
import { ROUTE_PATHS } from '@/router/guards'
import { useReviewStore } from '@/stores/reviews'

const router = useRouter()
const reviewStore = useReviewStore()

const statusFilter = ref<ReviewFilterStatus>('all')
const channelFilter = ref('all')
const checkInDateFilter = ref('')
const checkOutDateFilter = ref('')

const channelOptions = computed(() => {
  const options = [{ label: '全部渠道', value: 'all' }]

  for (const channel of reviewStore.channels) {
    options.push({
      label: channel.name,
      value: String(channel.id),
    })
  }

  return options
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
</script>

<style scoped>
.registration-review-list-page__hero {
  display: grid;
  gap: var(--ios-pms-space-3);
  padding-bottom: var(--ios-pms-space-4);
  margin-bottom: var(--ios-pms-space-3);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(249, 251, 255, 0.98));
  box-shadow: 0 10px 24px rgba(77, 98, 145, 0.045);
}

.registration-review-list-route {
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.08), transparent 34%),
    linear-gradient(180deg, #f4f7fc 0%, #eef3fb 100%);
}

.registration-review-list-page {
  --background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.08), transparent 34%),
    linear-gradient(180deg, #f4f7fc 0%, #eef3fb 100%);
  background: var(--background);
}

.registration-review-list-page__hero::before {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0));
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
  min-height: 32px;
}

.registration-review-list-page__hero :deep(.mobile-title) {
  color: var(--ios-pms-text-primary);
  margin: 0;
  line-height: 1.2;
}

.registration-review-list-page__hero-action {
  flex-shrink: 0;
}

.registration-review-list-page__hero-metrics {
  margin-top: 0;
}

.registration-review-list-page__filters {
  display: grid;
  gap: var(--ios-pms-space-4);
}

.registration-review-list-page__filters-header {
  align-items: flex-start;
  padding-bottom: var(--ios-pms-space-2);
  border-bottom: 1px solid var(--ios-pms-divider);
  min-width: 0;
}

.registration-review-list-page__filters-note {
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.registration-review-list-page__filter-summary {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid var(--ios-pms-border-faint);
  border-radius: var(--ios-pms-radius-pill);
  background: var(--ios-pms-surface-muted);
  color: var(--ios-pms-text-muted);
  font-size: var(--ios-pms-font-body-sm-size);
  font-weight: var(--ios-pms-weight-medium);
  white-space: nowrap;
}

.registration-review-list-page__filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
  align-items: start;
  padding-top: 2px;
}

.registration-review-list-page__filter-grid :deep(ion-select),
.registration-review-list-page__filter-grid :deep(ion-input) {
  --border-radius: var(--ios-pms-radius-input);
  --background: rgba(247, 250, 255, 0.72);
  --border-color: var(--ios-pms-border-soft);
  --highlight-color-focused: var(--ios-pms-primary);
  --padding-start: 12px;
  --padding-end: 12px;
  min-height: 74px;
}

.registration-review-list-page__filter-grid :deep(.label-text-wrapper) {
  margin-bottom: 6px;
}

.registration-review-list-page__filter-grid :deep(.label-text) {
  color: var(--ios-pms-text-secondary);
  font-size: var(--ios-pms-font-body-sm-size);
  font-weight: var(--ios-pms-weight-medium);
}

.registration-review-list-page__filter-actions {
  display: flex;
  gap: var(--ios-pms-space-2);
  justify-content: flex-end;
  flex-wrap: wrap;
  padding-top: var(--ios-pms-space-2);
  border-top: 1px solid var(--ios-pms-divider);
}

.registration-review-list-page__results-header {
  margin-bottom: var(--ios-pms-space-2);
  min-width: 0;
}

.registration-review-list-page__header-actions {
  display: flex;
  gap: var(--ios-pms-space-2);
  flex-shrink: 0;
  align-items: center;
}

.registration-review-list-page__item {
  width: 100%;
  padding: var(--ios-pms-space-4) 0;
  border: none;
  border-radius: 0;
  border-top: 1px solid var(--ios-pms-divider);
  background: transparent;
  text-align: left;
  color: inherit;
}

.registration-review-list-page__item:first-child {
  border-top: none;
}

.registration-review-list-page__item.is-pending {
  margin: 0;
  padding: var(--ios-pms-space-4) var(--ios-pms-space-3);
  border-top-color: transparent;
  border-radius: var(--ios-pms-radius-card-sm);
  background: linear-gradient(180deg, rgba(227, 139, 24, 0.08), rgba(255, 255, 255, 0.72));
}

.registration-review-list-page__item.is-pending + .registration-review-list-page__item {
  border-top-color: transparent;
}

.registration-review-list-page__item-header {
  display: flex;
  justify-content: space-between;
  gap: var(--ios-pms-space-3);
  align-items: flex-start;
  min-width: 0;
}

.registration-review-list-page__guest-name {
  display: block;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.3;
}

.registration-review-list-page__item-header p,
.registration-review-list-page__notes p {
  margin: 4px 0 0;
  color: var(--ios-pms-text-muted);
  font-size: var(--ios-pms-font-body-sm-size);
  line-height: 1.6;
}

.registration-review-list-page__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ios-pms-space-2) var(--ios-pms-space-3);
  margin-top: var(--ios-pms-space-3);
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-body-sm-size);
}

.registration-review-list-page__notes {
  margin-top: var(--ios-pms-space-3);
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
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: var(--ios-pms-radius-pill);
  font-size: var(--ios-pms-font-body-sm-size);
  font-weight: var(--ios-pms-weight-bold);
}

.registration-review-list-page__status.is-pending {
  border-color: rgba(227, 139, 24, 0.16);
  background: rgba(227, 139, 24, 0.12);
  color: var(--ion-color-warning);
}

.registration-review-list-page__status.is-draft {
  border-color: rgba(100, 116, 139, 0.12);
  background: rgba(100, 116, 139, 0.08);
  color: var(--ion-color-medium);
}

.registration-review-list-page__status.is-approved {
  border-color: rgba(15, 159, 110, 0.12);
  background: rgba(15, 159, 110, 0.1);
  color: var(--ion-color-success);
}

.registration-review-list-page__status.is-rejected {
  border-color: rgba(220, 38, 38, 0.12);
  background: rgba(220, 38, 38, 0.08);
  color: var(--ion-color-danger);
}

.registration-review-list-page__empty {
  padding-top: var(--ios-pms-space-2);
}

@media (max-width: 360px) {
  .registration-review-list-page__hero-header,
  .registration-review-list-page__filters-header,
  .registration-review-list-page__results-header {
    display: grid;
  }

  .registration-review-list-page__filter-grid,
  .registration-review-list-page__meta-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .registration-review-list-page__filter-summary {
    justify-self: flex-start;
  }
}
</style>
