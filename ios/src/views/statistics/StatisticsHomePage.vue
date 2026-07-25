<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.StatisticsOverview') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page statistics-home-page">
      <section class="mobile-hero statistics-home-page__hero">
        <p class="mobile-note statistics-home-page__eyebrow">{{ $t('statistics.home.eyebrow') }}</p>
        <h1 class="mobile-title">{{ storeName }}</h1>
        <p class="mobile-subtitle">
          {{ $t('statistics.home.subtitle') }}
        </p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ $t('statistics.home.reportCount') }}</span>
          <span class="mobile-chip">
            {{ $t('statistics.home.pendingCount', { count: reviewStore.pendingCount }) }}
          </span>
          <span class="mobile-chip">
            {{ $t('statistics.home.approvedCount', { count: reviewStore.approvedCount }) }}
          </span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <h2 class="mobile-section-title">{{ $t('statistics.home.overviewMetrics') }}</h2>
          <div class="statistics-home-page__metric-grid">
            <article
              v-for="metric in STATISTICS_HOME_METRICS"
              :key="metric.labelKey"
              class="statistics-home-page__metric-card"
            >
              <span class="statistics-home-page__metric-label">{{ t(metric.labelKey) }}</span>
              <strong class="statistics-home-page__metric-value" :class="`is-${metric.tone}`">
                {{ resolveMetricValue(metric) }}
              </strong>
              <p class="mobile-note">{{ t(metric.noteKey) }}</p>
            </article>
          </div>
        </section>

        <section class="mobile-card statistics-home-page__review-card">
          <div>
            <h2 class="mobile-section-title">{{ $t('statistics.home.reviewShortcut') }}</h2>
            <p class="mobile-note">
              {{ $t('statistics.home.reviewDescription') }}
            </p>
          </div>
          <div class="statistics-home-page__review-summary">
            <span class="statistics-home-page__review-pill">
              {{ $t('statistics.home.pendingCount', { count: reviewStore.pendingCount }) }}
            </span>
            <span class="statistics-home-page__review-pill">
              {{ $t('statistics.home.approvedCount', { count: reviewStore.approvedCount }) }}
            </span>
          </div>
          <p v-if="reviewStore.loadError" class="mobile-note">{{ reviewStore.loadError }}</p>
          <ion-button expand="block" @click="handleOpenReviews">{{ $t('statistics.home.openReviews') }}</ion-button>
        </section>

        <section class="mobile-card">
          <ion-segment :value="activeSegment" @ionChange="handleSegmentChange">
            <ion-segment-button value="operation">
              <ion-label>{{ $t('statistics.home.operations') }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="finance">
              <ion-label>{{ $t('statistics.home.finance') }}</ion-label>
            </ion-segment-button>
          </ion-segment>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">{{ $t('statistics.home.reports') }}</h2>
              <p class="mobile-note">
                {{ $t('statistics.home.reportsDescription') }}
              </p>
            </div>
          </div>

          <div class="statistics-home-page__report-list">
            <button
              v-for="report in visibleReports"
              :key="report.key"
              type="button"
              class="statistics-home-page__report-card"
              @click="handleOpenReport(report.path)"
            >
              <div class="statistics-home-page__report-body">
                <div class="statistics-home-page__report-header">
                  <strong>{{ t(report.titleKey) }}</strong>
                  <span class="statistics-home-page__report-badge">{{ t(report.shortTitleKey) }}</span>
                </div>
                <p>{{ t(report.subtitleKey) }}</p>
              </div>
            </button>
          </div>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">{{ $t('statistics.home.focus') }}</h2>
          <ul class="mobile-bullet-list">
            <li>{{ $t('statistics.home.focusCards') }}</li>
            <li>{{ $t('statistics.home.focusReports') }}</li>
            <li>{{ $t('statistics.home.focusReviews') }}</li>
          </ul>
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
  IonLabel,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  STATISTICS_HOME_METRICS,
  STATISTICS_REPORTS,
  type StatisticsMetric,
  type StatisticsReportCategory,
} from '@/constants/statistics'
import { ROUTE_PATHS } from '@/router/guards'
import { useReviewStore } from '@/stores/reviews'
import { useStoreStore } from '@/stores/store'
import { formatMoney } from '@/utils/formatters'

const router = useRouter()
const { t } = useI18n()
const storeStore = useStoreStore()
const reviewStore = useReviewStore()

const activeSegment = ref<StatisticsReportCategory>('operation')

const storeName = computed(() => {
  return storeStore.currentStore?.name || t('statistics.home.titleFallback')
})
const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))

const visibleReports = computed(() => {
  return Object.values(STATISTICS_REPORTS).filter((report) => report.category === activeSegment.value)
})

onIonViewWillEnter(async () => {
  await reviewStore.refreshRecords()
})

function resolveMetricValue(metric: StatisticsMetric) {
  if (metric.dynamicValue === 'pendingReviews') {
    return t('statistics.values.pendingReviews', { count: reviewStore.pendingCount })
  }

  if (typeof metric.currencyValue === 'number') {
    return formatMoney(
      metric.currencyValue,
      currentCurrency.value,
      {
        notation: metric.compactCurrency ? 'compact' : 'standard',
        maximumFractionDigits: metric.compactCurrency ? 1 : 0,
      },
      currentMoneyContext.value,
    )
  }

  return metric.valueKey ? t(metric.valueKey) : ''
}

function handleSegmentChange(event: CustomEvent) {
  const nextValue = event.detail.value as StatisticsReportCategory
  if (!nextValue) {
    return
  }

  activeSegment.value = nextValue
}

async function handleOpenReport(path: string) {
  await router.push(path)
}

async function handleOpenReviews() {
  await router.push(ROUTE_PATHS.reviews)
}
</script>

<style scoped>
.statistics-home-page__eyebrow {
  color: var(--ios-pms-primary);
  font-size: var(--ios-pms-font-body-sm-size);
  font-weight: var(--ios-pms-weight-bold);
}

.statistics-home-page__metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ios-pms-space-3);
  margin-top: var(--ios-pms-space-4);
}

.statistics-home-page__metric-card,
.statistics-home-page__report-card {
  padding: var(--ios-pms-space-4);
  border-radius: var(--ios-pms-radius-card-sm);
  border: 1px solid var(--ios-pms-border-soft);
  background: rgba(255, 255, 255, 0.84);
}

.statistics-home-page__metric-label {
  display: block;
  color: var(--ios-pms-text-muted);
  font-size: var(--ios-pms-font-body-md-size);
  font-weight: var(--ios-pms-weight-medium);
  overflow-wrap: anywhere;
}

.statistics-home-page__metric-value {
  display: block;
  margin-top: var(--ios-pms-space-2);
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-metric-lg-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.statistics-home-page__metric-value.is-primary {
  color: var(--ion-color-primary);
}

.statistics-home-page__metric-value.is-success {
  color: var(--ion-color-success);
}

.statistics-home-page__metric-value.is-warning {
  color: var(--ion-color-warning);
}

.statistics-home-page__metric-value.is-secondary {
  color: var(--ion-color-secondary);
}

.statistics-home-page__review-card {
  display: grid;
  gap: var(--ios-pms-space-3);
}

.statistics-home-page__review-summary {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ios-pms-space-2);
}

.statistics-home-page__review-pill,
.statistics-home-page__report-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 13px;
  border: 1px solid rgba(116, 163, 251, 0.08);
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(115, 164, 255, 0.06);
  color: var(--ios-pms-primary-strong);
  font-size: var(--ios-pms-font-body-sm-size);
  font-weight: var(--ios-pms-weight-bold);
}

.statistics-home-page__report-list {
  display: grid;
  gap: var(--ios-pms-space-3);
  margin-top: var(--ios-pms-space-4);
}

.statistics-home-page__report-card {
  width: 100%;
  border: none;
  border-radius: 0;
  padding: 14px 2px;
  background: transparent;
  box-shadow: none;
  text-align: left;
}

.statistics-home-page__report-card + .statistics-home-page__report-card {
  border-top: 1px solid var(--ios-pms-divider);
}

.statistics-home-page__report-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.statistics-home-page__report-header strong {
  min-width: 0;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.statistics-home-page__report-body p {
  margin: 3px 0 0;
  color: var(--ios-pms-text-muted);
  font-size: var(--ios-pms-font-body-sm-size);
  line-height: 1.45;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

@media (max-width: 360px) {
  .statistics-home-page__metric-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
