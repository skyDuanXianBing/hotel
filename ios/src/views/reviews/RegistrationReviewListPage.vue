<template>
  <ion-page>
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
        <p class="mobile-note registration-review-list-page__eyebrow">入住登记审查</p>
        <h1 class="mobile-title">资料核验队列</h1>
        <p class="mobile-subtitle">按状态、渠道与入住日期快速筛选，点击卡片进入详情继续处理审核任务。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">共 {{ reviewStore.totalCount }} 条</span>
          <span class="mobile-chip">待处理 {{ reviewStore.pendingCount }} 条</span>
          <span class="mobile-chip">已通过 {{ reviewStore.approvedCount }} 条</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card registration-review-list-page__filters">
          <div>
            <h2 class="mobile-section-title">筛选条件</h2>
            <p class="mobile-note">可按状态、渠道、入住/退房日期筛选，方便在手机上快速查看。</p>
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
            <ion-input v-model="checkOutDateFilter" type="date" fill="outline" label="退房日期" label-placement="stacked" />
          </div>

          <div class="registration-review-list-page__filter-actions">
            <ion-button size="small" @click="handleApplyFilters">搜索</ion-button>
            <ion-button fill="outline" size="small" @click="handleResetFilters">清空条件</ion-button>
          </div>

          <p v-if="reviewStore.channelLoadError" class="mobile-note">{{ reviewStore.channelLoadError }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">审查列表</h2>
              <p class="mobile-note">集中呈现待审资料要点，方便快速浏览并继续处理。</p>
            </div>
            <div class="registration-review-list-page__header-actions">
              <ion-button fill="outline" size="small" @click="handleOpenLinks">链接列表</ion-button>
              <ion-button fill="outline" size="small" @click="handleReload">刷新</ion-button>
            </div>
          </div>

          <div v-if="reviewStore.isLoading" class="registration-review-list-page__loading">
            <ion-spinner name="crescent" />
            <p class="mobile-note">正在加载审查列表...</p>
          </div>

          <div v-else-if="reviewStore.loadError" class="registration-review-list-page__error-state">
            <p class="mobile-note">{{ reviewStore.loadError }}</p>
            <ion-button fill="outline" size="small" @click="handleReload">重新加载</ion-button>
          </div>

          <div v-else-if="reviewStore.records.length > 0" class="mobile-list registration-review-list-page__list">
            <button
              v-for="record in reviewStore.records"
              :key="record.formId"
              type="button"
              class="registration-review-list-page__item"
              @click="handleOpenDetail(record.formId)"
            >
              <div class="registration-review-list-page__item-header">
                <div>
                  <strong>{{ record.guestName }}</strong>
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
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
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

onIonViewWillEnter(async () => {
  await Promise.all([reviewStore.refreshChannels(), reviewStore.refreshRecords(activeFilters.value)])
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
.registration-review-list-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.registration-review-list-page__filters {
  display: grid;
  gap: 14px;
}

.registration-review-list-page__filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.registration-review-list-page__filter-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.registration-review-list-page__header-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  align-items: flex-start;
}

.registration-review-list-page__item {
  width: 100%;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.86);
  text-align: left;
}

.registration-review-list-page__item-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.registration-review-list-page__item-header p,
.registration-review-list-page__notes p {
  margin: 6px 0 0;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.registration-review-list-page__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
  color: var(--app-muted);
  font-size: 12px;
}

.registration-review-list-page__notes {
  margin-top: 12px;
}

.registration-review-list-page__loading,
.registration-review-list-page__error-state {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 20px 0;
}

.registration-review-list-page__status {
  flex-shrink: 0;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.registration-review-list-page__status.is-pending {
  background: rgba(217, 119, 6, 0.12);
  color: var(--ion-color-warning);
}

.registration-review-list-page__status.is-draft {
  background: rgba(100, 116, 139, 0.12);
  color: var(--ion-color-medium);
}

.registration-review-list-page__status.is-approved {
  background: rgba(15, 159, 110, 0.12);
  color: var(--ion-color-success);
}

.registration-review-list-page__status.is-rejected {
  background: rgba(220, 38, 38, 0.1);
  color: var(--ion-color-danger);
}

.registration-review-list-page__empty {
  padding-top: 10px;
}

@media (max-width: 360px) {
  .registration-review-list-page__filter-grid,
  .registration-review-list-page__meta-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
