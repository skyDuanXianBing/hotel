<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.statistics" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ localizedTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page statistics-report-page">
      <section v-if="definition.showHero !== false" class="mobile-hero statistics-report-page__hero">
        <p class="mobile-note statistics-report-page__eyebrow">{{ t(definition.eyebrowKey) }}</p>
        <h1 class="mobile-title">{{ localizedTitle }}</h1>
        <p class="mobile-subtitle">{{ t(definition.subtitleKey) }}</p>
        <div class="mobile-chip-row">
          <span v-for="chipKey in definition.chipKeys" :key="chipKey" class="mobile-chip">
            {{ t(chipKey) }}
          </span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">{{ $t('statistics.report.keyMetrics') }}</h2>
              <p v-if="definition.metricsDescriptionKey !== null" class="mobile-note">
                {{
                  definition.metricsDescriptionKey
                    ? t(definition.metricsDescriptionKey)
                    : t('statistics.report.defaultMetricsDescription')
                }}
              </p>
            </div>
          </div>

          <div class="statistics-report-page__metric-grid">
            <article
              v-for="metric in definition.metrics"
              :key="metric.labelKey"
              class="statistics-report-page__metric-card"
            >
              <span class="statistics-report-page__metric-label">{{ t(metric.labelKey) }}</span>
              <strong class="statistics-report-page__metric-value" :class="`is-${metric.tone}`">
                {{ resolveMetricValue(metric) }}
              </strong>
              <p class="mobile-note">{{ t(metric.noteKey) }}</p>
            </article>
          </div>
        </section>

        <section v-if="definition.showSections !== false" class="mobile-card mobile-list">
          <article
            v-for="section in definition.sections"
            :key="section.titleKey"
            class="statistics-report-page__section-card"
          >
            <h3>{{ t(section.titleKey) }}</h3>
            <p class="mobile-note">{{ t(section.descriptionKey) }}</p>
            <ul class="mobile-bullet-list">
              <li v-for="bulletKey in section.bulletKeys" :key="bulletKey">{{ t(bulletKey) }}</li>
            </ul>
          </article>
        </section>

        <section v-if="definition.showBoundaryNotes !== false" class="mobile-card">
          <h2 class="mobile-section-title">{{ $t('statistics.report.viewTips') }}</h2>
          <ul class="mobile-bullet-list">
            <li v-for="noteKey in definition.boundaryNoteKeys" :key="noteKey">{{ t(noteKey) }}</li>
          </ul>
        </section>

        <section class="mobile-card statistics-report-page__actions">
          <ion-button expand="block" fill="outline" @click="handleBackToStatistics">
            {{ $t('statistics.report.backToStatistics') }}
          </ion-button>
          <ion-button expand="block" @click="handleOpenReviews">{{ $t('statistics.report.openReviews') }}</ion-button>
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
  IonPage,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import type { StatisticsMetric, StatisticsReportDefinition } from '@/constants/statistics'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { formatMoney } from '@/utils/formatters'

const props = defineProps<{
  definition: StatisticsReportDefinition
}>()

const route = useRoute()
const router = useRouter()
const storeStore = useStoreStore()
const { t, te } = useI18n()

const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))
const localizedTitle = computed(() => {
  const titleKey = typeof route.meta.titleKey === 'string' ? route.meta.titleKey : ''
  return titleKey && te(titleKey) ? t(titleKey) : t(props.definition.titleKey)
})

function resolveMetricValue(metric: StatisticsMetric) {
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

async function handleBackToStatistics() {
  await router.push(ROUTE_PATHS.statistics)
}

async function handleOpenReviews() {
  await router.push(ROUTE_PATHS.reviews)
}
</script>

<style scoped>
.statistics-report-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.statistics-report-page__metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.statistics-report-page__metric-card,
.statistics-report-page__section-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.84);
}

.statistics-report-page__metric-label {
  display: block;
  color: var(--app-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.statistics-report-page__metric-value {
  display: block;
  margin-top: 8px;
  color: var(--app-heading);
  font-size: 24px;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.statistics-report-page__metric-value.is-primary {
  color: var(--ion-color-primary);
}

.statistics-report-page__metric-value.is-success {
  color: var(--ion-color-success);
}

.statistics-report-page__metric-value.is-warning {
  color: var(--ion-color-warning);
}

.statistics-report-page__metric-value.is-secondary {
  color: var(--ion-color-secondary);
}

.statistics-report-page__section-card h3 {
  margin: 0 0 8px;
}

.statistics-report-page__actions {
  display: grid;
  gap: 10px;
}

@media (max-width: 360px) {
  .statistics-report-page__metric-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
