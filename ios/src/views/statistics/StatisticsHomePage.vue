<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title class="mobile-toolbar-title">统计</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page statistics-home-page">
      <section class="mobile-hero statistics-home-page__hero">
        <p class="mobile-note statistics-home-page__eyebrow">经营与审查入口</p>
        <h1 class="mobile-title">{{ storeName }}</h1>
        <p class="mobile-subtitle">方便负责人在手机上查看经营情况，并继续处理审查任务。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">7 个报表入口</span>
          <span class="mobile-chip">待审查 {{ reviewStore.pendingCount }} 条</span>
          <span class="mobile-chip">已通过 {{ reviewStore.approvedCount }} 条</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <h2 class="mobile-section-title">概览指标</h2>
          <div class="statistics-home-page__metric-grid">
            <article v-for="metric in STATISTICS_HOME_METRICS" :key="metric.label" class="statistics-home-page__metric-card">
              <span class="statistics-home-page__metric-label">{{ metric.label }}</span>
              <strong class="statistics-home-page__metric-value" :class="`is-${metric.tone}`">
                {{ resolveMetricValue(metric) }}
              </strong>
              <p class="mobile-note">{{ metric.note }}</p>
            </article>
          </div>
        </section>

        <section class="mobile-card statistics-home-page__review-card">
          <div>
            <h2 class="mobile-section-title">审查快捷入口</h2>
            <p class="mobile-note">集中处理入住登记、附件核验、资料下载与审核结论。</p>
          </div>
          <div class="statistics-home-page__review-summary">
            <span class="statistics-home-page__review-pill">待处理 {{ reviewStore.pendingCount }} 条</span>
            <span class="statistics-home-page__review-pill">已通过 {{ reviewStore.approvedCount }} 条</span>
          </div>
          <p v-if="reviewStore.loadError" class="mobile-note">{{ reviewStore.loadError }}</p>
          <ion-button expand="block" @click="handleOpenReviews">进入审查</ion-button>
        </section>

        <section class="mobile-card">
          <ion-segment :value="activeSegment" @ionChange="handleSegmentChange">
            <ion-segment-button value="operation">
              <ion-label>经营视图</ion-label>
            </ion-segment-button>
            <ion-segment-button value="finance">
              <ion-label>财务视图</ion-label>
            </ion-segment-button>
          </ion-segment>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">报表入口</h2>
              <p class="mobile-note">按日常使用场景分类，方便快速进入需要查看的报表。</p>
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
                  <strong>{{ report.title }}</strong>
                  <span class="statistics-home-page__report-badge">{{ report.shortTitle }}</span>
                </div>
                <p>{{ report.subtitle }}</p>
              </div>
            </button>
          </div>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">查看重点</h2>
          <ul class="mobile-bullet-list">
            <li>统计页按移动查看习惯整理为清晰的指标卡与摘要。</li>
            <li>可从这里快速进入各类经营、财务与住宿报表。</li>
            <li>审查页支持备注、通过、驳回、附件与历史查看等常用操作。</li>
          </ul>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonHeader, IonLabel, IonPage, IonSegment, IonSegmentButton, IonTitle, IonToolbar, onIonViewWillEnter } from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { STATISTICS_HOME_METRICS, STATISTICS_REPORTS, type StatisticsMetric, type StatisticsReportCategory } from '@/constants/statistics'
import { ROUTE_PATHS } from '@/router/guards'
import { useReviewStore } from '@/stores/reviews'
import { useStoreStore } from '@/stores/store'

const router = useRouter()
const storeStore = useStoreStore()
const reviewStore = useReviewStore()

const activeSegment = ref<StatisticsReportCategory>('operation')

const storeName = computed(() => {
  return storeStore.currentStore?.name || '统计工作台'
})

const visibleReports = computed(() => {
  const result = []

  for (const report of Object.values(STATISTICS_REPORTS)) {
    if (report.category === activeSegment.value) {
      result.push(report)
    }
  }

  return result
})

onIonViewWillEnter(async () => {
  await reviewStore.refreshRecords()
})

function resolveMetricValue(metric: StatisticsMetric) {
  if (metric.label === '待审查') {
    return `${reviewStore.pendingCount} 条`
  }

  return metric.value
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
  color: var(--ion-color-primary);
  font-weight: 700;
}

.statistics-home-page__metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.statistics-home-page__metric-card,
.statistics-home-page__report-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.84);
}

.statistics-home-page__metric-label {
  display: block;
  color: var(--app-muted);
  font-size: 12px;
}

.statistics-home-page__metric-value {
  display: block;
  margin-top: 8px;
  color: var(--app-heading);
  font-size: 24px;
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
  gap: 12px;
}

.statistics-home-page__review-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.statistics-home-page__review-pill,
.statistics-home-page__report-badge {
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.statistics-home-page__report-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.statistics-home-page__report-card {
  width: 100%;
  text-align: left;
}

.statistics-home-page__report-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.statistics-home-page__report-body p {
  margin: 8px 0 0;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
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
