<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.statistics" />
        </ion-buttons>
        <ion-title class="mobile-toolbar-title">{{ definition.title }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page statistics-report-page">
      <section v-if="definition.showHero !== false" class="mobile-hero statistics-report-page__hero">
        <p class="mobile-note statistics-report-page__eyebrow">{{ definition.eyebrow }}</p>
        <h1 class="mobile-title">{{ definition.title }}</h1>
        <p class="mobile-subtitle">{{ definition.subtitle }}</p>
        <div class="mobile-chip-row">
          <span v-for="chip in definition.chips" :key="chip" class="mobile-chip">{{ chip }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">关键指标</h2>
              <p v-if="definition.metricsDescription !== null" class="mobile-note">
                {{ definition.metricsDescription ?? '聚焦最关键的指标与结论，方便快速浏览。' }}
              </p>
            </div>
          </div>

          <div class="statistics-report-page__metric-grid">
            <article
              v-for="metric in definition.metrics"
              :key="metric.label"
              class="statistics-report-page__metric-card"
            >
              <span class="statistics-report-page__metric-label">{{ metric.label }}</span>
              <strong class="statistics-report-page__metric-value" :class="`is-${metric.tone}`">
                {{ metric.value }}
              </strong>
              <p class="mobile-note">{{ metric.note }}</p>
            </article>
          </div>
        </section>

        <section v-if="definition.showSections !== false" class="mobile-card mobile-list">
          <article
            v-for="section in definition.sections"
            :key="section.title"
            class="statistics-report-page__section-card"
          >
            <h3>{{ section.title }}</h3>
            <p class="mobile-note">{{ section.description }}</p>
            <ul class="mobile-bullet-list">
              <li v-for="bullet in section.bullets" :key="bullet">{{ bullet }}</li>
            </ul>
          </article>
        </section>

        <section v-if="definition.showBoundaryNotes !== false" class="mobile-card">
          <h2 class="mobile-section-title">查看提示</h2>
          <ul class="mobile-bullet-list">
            <li v-for="note in definition.boundaryNotes" :key="note">{{ note }}</li>
          </ul>
        </section>

        <section class="mobile-card statistics-report-page__actions">
          <ion-button expand="block" fill="outline" @click="handleBackToStatistics">返回统计首页</ion-button>
          <ion-button expand="block" @click="handleOpenReviews">前往审核</ion-button>
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
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '@/router/guards'
import type { StatisticsReportDefinition } from '@/constants/statistics'

defineProps<{
  definition: StatisticsReportDefinition
}>()

const router = useRouter()

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
}

.statistics-report-page__metric-value {
  display: block;
  margin-top: 8px;
  color: var(--app-heading);
  font-size: 24px;
  line-height: 1.2;
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
