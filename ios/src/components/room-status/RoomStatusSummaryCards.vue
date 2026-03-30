<template>
  <section class="mobile-card summary-card">
    <div class="mobile-inline-row summary-card__header">
      <div>
        <h2 class="mobile-section-title">房态摘要</h2>
        <p class="mobile-note">围绕当前业务日保留预抵、预离、可售与待处理语义。</p>
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
.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid var(--app-border);
  display: grid;
  gap: 6px;
}

.summary-item__title {
  font-size: 12px;
  color: var(--app-muted);
}

.summary-item__value {
  font-size: 24px;
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
