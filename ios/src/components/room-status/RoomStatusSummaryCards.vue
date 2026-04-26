<template>
  <section class="mobile-card summary-card">
    <div class="mobile-inline-row summary-card__header">
      <div>
        <h2 class="mobile-section-title">房态摘要</h2>
      </div>
      <ion-spinner v-if="loading" name="crescent" />
    </div>

    <div class="summary-grid">
      <article v-for="item in items" :key="item.key" class="summary-item">
        <span class="summary-item__title">{{ item.title }}</span>
        <strong :class="`summary-item__value summary-item__value--${item.tone}`">{{ item.value }}</strong>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonSpinner } from '@ionic/vue'
import type { SummaryCardItem } from '@/stores/roomStatus'

defineProps<{
  items: SummaryCardItem[]
  loading: boolean
}>()
</script>

<style scoped>
.summary-card {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 250, 255, 0.94));
}

.summary-card__header {
  margin-bottom: 14px;
}

.summary-card__header h2 {
  margin: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 16px;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 251, 255, 0.92));
  border: 1px solid rgba(112, 138, 187, 0.12);
  display: grid;
  gap: 8px;
  box-shadow: 0 14px 28px rgba(83, 107, 152, 0.05);
}

.summary-item__title {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-muted);
}

.summary-item__value {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1;
}

.summary-item__value--primary {
  color: var(--ion-color-primary);
}

.summary-item__value--success {
  color: var(--ion-color-success);
}

.summary-item__value--warning {
  color: var(--ion-color-warning);
}

.summary-item__value--danger {
  color: var(--ion-color-danger);
}
</style>
