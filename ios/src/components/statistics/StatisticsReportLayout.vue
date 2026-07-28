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
        <section class="mobile-card statistics-report-page__filters">
          <div class="statistics-report-page__date-grid">
            <label>
              <span>{{ $t('stage5.common.date.startDate') }}</span>
              <input v-model="startDate" type="date" :max="endDate || undefined" />
            </label>
            <label>
              <span>{{ $t('stage5.common.date.endDate') }}</span>
              <input v-model="endDate" type="date" :min="startDate || undefined" />
            </label>
          </div>
          <ion-button expand="block" fill="outline" :disabled="loading" @click="loadReport">
            {{ loading ? $t('statistics.report.loading') : $t('stage5.common.actions.refresh') }}
          </ion-button>
        </section>

        <section v-if="loading" class="mobile-card statistics-report-page__status">
          <ion-spinner name="crescent" />
          <p class="mobile-note">{{ $t('statistics.report.loading') }}</p>
        </section>

        <section v-else-if="loadError" class="mobile-card statistics-report-page__status">
          <p class="mobile-note">{{ loadError }}</p>
          <ion-button size="small" fill="outline" @click="loadReport">
            {{ $t('stage5.common.actions.refresh') }}
          </ion-button>
        </section>

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
              v-for="(metric, metricIndex) in definition.metrics"
              :key="metric.labelKey"
              class="statistics-report-page__metric-card"
            >
              <span class="statistics-report-page__metric-label">{{ t(metric.labelKey) }}</span>
              <strong class="statistics-report-page__metric-value" :class="`is-${metric.tone}`">
                {{ resolveMetricValue(metric, metricIndex) }}
              </strong>
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
          <ion-button
            v-if="definition.downloadType"
            expand="block"
            :disabled="downloading || loading"
            @click="handleDownloadReport"
          >
            {{ downloading ? $t('statistics.report.loading') : $t('statistics.report.exportDetails') }}
          </ion-button>
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
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { getNotesList, getNotesStatistics } from '@/api/notes'
import {
  downloadStatisticsReport,
  getBusinessSummary,
  getChannelSummary,
  getOperationalMetrics,
  getRevenueSummary,
} from '@/api/statistics'
import type { StatisticsMetric, StatisticsReportDefinition } from '@/constants/statistics'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { downloadBlobFile } from '@/utils/file'
import { formatMoney, formatNumber, formatPercent } from '@/utils/formatters'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { getStoreDatePresetRange } from '@/utils/storeBusinessDate'

const props = defineProps<{
  definition: StatisticsReportDefinition
}>()

const route = useRoute()
const router = useRouter()
const storeStore = useStoreStore()
const { t, te } = useI18n()

const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))
const defaultDateRange = getStoreDatePresetRange('month')
const startDate = ref(defaultDateRange.startDate)
const endDate = ref(defaultDateRange.endDate)
const metricValues = ref<Array<string | number>>([])
const loading = ref(false)
const downloading = ref(false)
const loadError = ref('')
const localizedTitle = computed(() => {
  const titleKey = typeof route.meta.titleKey === 'string' ? route.meta.titleKey : ''
  return titleKey && te(titleKey) ? t(titleKey) : t(props.definition.titleKey)
})

function resolveMetricValue(metric: StatisticsMetric, metricIndex: number) {
  const value = metricValues.value[metricIndex]
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  if (metric.valueFormat === 'currency') {
    return formatMoney(
      Number(value || 0),
      currentCurrency.value,
      {
        notation: metric.compactCurrency ? 'compact' : 'standard',
        maximumFractionDigits: metric.compactCurrency ? 1 : 0,
      },
      currentMoneyContext.value,
    )
  }

  if (metric.valueFormat === 'percent') {
    return formatPercent(Number(value || 0), { maximumFractionDigits: 1 })
  }

  if (metric.valueFormat === 'number') {
    return formatNumber(Number(value || 0), { maximumFractionDigits: 0 })
  }

  return String(value)
}

function validateDateRange() {
  if (!startDate.value || !endDate.value) {
    showWarningToast(t('stage5.common.messages.pleaseSelectDateRange'))
    return false
  }
  if (startDate.value > endDate.value) {
    showWarningToast(t('stage5.dataCenter.overview.invalidDateRange'))
    return false
  }
  return true
}

function resolveErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return t('stage5.common.messages.dataLoadFailed')
}

async function loadReport() {
  if (!validateDateRange()) {
    return
  }

  loading.value = true
  loadError.value = ''
  const params = {
    startDate: startDate.value,
    endDate: endDate.value,
  }

  try {
    if (props.definition.key === 'business-summary') {
      const response = await getBusinessSummary(params)
      if (!response.success || !response.data) {
        throw new Error(response.message || t('stage5.common.messages.dataLoadFailed'))
      }
      metricValues.value = [
        response.data.totalRevenue,
        response.data.occupancyRate,
        response.data.averageRoomRate ?? response.data.averageOrderValue ?? 0,
        response.data.totalOrders,
      ]
      return
    }

    if (props.definition.key === 'channel-summary') {
      const response = await getChannelSummary(params)
      if (!response.success || !response.data) {
        throw new Error(response.message || t('stage5.common.messages.dataLoadFailed'))
      }
      const topChannel = [...response.data.revenueDistribution].sort(
        (first, second) => second.value - first.value,
      )[0]
      metricValues.value = [
        topChannel?.channelName || '-',
        topChannel?.percentage ?? 0,
        response.data.channelDetails.length,
        response.data.totalRevenue,
      ]
      return
    }

    if (props.definition.key === 'notes-summary') {
      const [statisticsResponse, listResponse] = await Promise.all([
        getNotesStatistics(params),
        getNotesList(params),
      ])
      if (!statisticsResponse.success || !statisticsResponse.data) {
        throw new Error(statisticsResponse.message || t('stage5.common.messages.dataLoadFailed'))
      }
      if (!listResponse.success || !listResponse.data) {
        throw new Error(listResponse.message || t('stage5.common.messages.dataLoadFailed'))
      }
      metricValues.value = [
        listResponse.data.length,
        listResponse.data.filter((item) => item.type === 'expense').length,
        statisticsResponse.data.totalIncome,
        statisticsResponse.data.totalExpense,
      ]
      return
    }

    if (
      props.definition.key === 'revenue-summary' ||
      props.definition.key === 'finance-report'
    ) {
      const response = await getRevenueSummary(params)
      if (!response.success || !response.data) {
        throw new Error(response.message || t('stage5.common.messages.dataLoadFailed'))
      }
      const totalIncome = response.data.totalIncome ?? response.data.totalRevenue
      const totalExpense = response.data.totalExpense ?? 0
      metricValues.value =
        props.definition.key === 'finance-report'
          ? [
              response.data.netIncome ?? totalIncome - totalExpense,
              totalExpense,
              response.data.paymentRefund ?? 0,
              totalIncome,
            ]
          : [
              totalIncome,
              response.data.paymentRefund ?? 0,
              totalExpense,
              response.data.netIncome ?? totalIncome - totalExpense,
            ]
      return
    }

    const response = await getOperationalMetrics(params)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5.common.messages.dataLoadFailed'))
    }
    metricValues.value =
      props.definition.key === 'accommodation-report'
        ? [
            response.data.totalSoldRoomNights,
            response.data.occupancyRate,
            response.data.totalRooms,
            response.data.totalAvailableRoomNights,
          ]
        : [
            response.data.occupancyRate,
            response.data.averageDailyRate,
            response.data.revPAR,
            response.data.totalRooms,
          ]
  } catch (error) {
    loadError.value = resolveErrorMessage(error)
    metricValues.value = []
    if (!isHandledRequestError(error)) {
      showWarningToast(loadError.value)
    }
  } finally {
    loading.value = false
  }
}

async function handleDownloadReport() {
  if (!props.definition.downloadType || !validateDateRange()) {
    return
  }

  downloading.value = true
  try {
    const blob = await downloadStatisticsReport(props.definition.downloadType, {
      startDate: startDate.value,
      endDate: endDate.value,
    })
    downloadBlobFile(
      blob,
      `${props.definition.downloadType}-${startDate.value}-${endDate.value}.csv`,
    )
    showSuccessToast(t('statistics.report.exportSuccess'))
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveErrorMessage(error))
    }
  } finally {
    downloading.value = false
  }
}

async function handleBackToStatistics() {
  await router.push(ROUTE_PATHS.statistics)
}

async function handleOpenReviews() {
  await router.push(ROUTE_PATHS.reviews)
}

onIonViewWillEnter(() => {
  void loadReport()
})
</script>

<style scoped>
.statistics-report-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.statistics-report-page__filters {
  display: grid;
  gap: 14px;
}

.statistics-report-page__date-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.statistics-report-page__date-grid label {
  display: grid;
  gap: 6px;
  min-width: 0;
  color: var(--app-muted);
  font-size: 12px;
  font-weight: 600;
}

.statistics-report-page__date-grid input {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  min-height: 42px;
  padding: 8px 10px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
  outline: none;
  background: #fff;
  color: var(--app-heading);
  font: inherit;
}

.statistics-report-page__status {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding-block: 24px;
  text-align: center;
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
