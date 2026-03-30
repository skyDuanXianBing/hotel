<template>
  <section class="stats-section">
    <div class="stats-section__header">
      <h2 class="stats-section__title">今日经营</h2>
      <span class="stats-section__tag">实时</span>
    </div>

    <div class="stats-grid" :class="{ 'stats-grid--loading': loading }">
      <template v-if="loading">
        <div v-for="index in 6" :key="index" class="stats-skeleton">
          <ion-skeleton-text animated class="stats-skeleton__label" />
          <ion-skeleton-text animated class="stats-skeleton__value" />
        </div>
      </template>

      <template v-else>
        <button
          v-for="item in items"
          :key="item.key"
          type="button"
          class="stats-card"
          :class="`stats-card--${item.tone}`"
          @click="handleSelect(item)"
        >
          <span class="stats-card__title">{{ item.title }}</span>
          <div class="stats-card__value-row">
            <strong class="stats-card__value">{{ item.value }}</strong>
            <span class="stats-card__unit">间/单</span>
          </div>
          <span class="stats-card__action">{{ item.actionLabel }} ›</span>
        </button>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonSkeletonText } from '@ionic/vue'

export interface HomeStatCardItem {
  key: string
  title: string
  value: number
  description: string
  actionLabel: string
  tone: 'primary' | 'warning' | 'success' | 'secondary'
}

interface Props {
  items: HomeStatCardItem[]
  loading: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [item: HomeStatCardItem]
}>()

const handleSelect = (item: HomeStatCardItem) => {
  emit('select', item)
}
</script>

<style scoped>
.stats-section {
  padding: 18px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
}

.stats-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.stats-section__title {
  margin: 0;
  color: var(--app-heading);
  font-size: 16px;
  font-weight: 700;
}

.stats-section__tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 11px;
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.stats-card,
.stats-skeleton {
  border: 1px solid var(--app-border);
  border-radius: 16px;
}

.stats-card {
  width: 100%;
  padding: 14px 12px;
  appearance: none;
  background: var(--app-surface-strong);
  font: inherit;
  text-align: left;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.stats-card:active {
  transform: scale(0.97);
}

.stats-card--primary {
  border-color: rgba(var(--ion-color-primary-rgb), 0.12);
  background: linear-gradient(160deg, var(--app-primary-soft), var(--app-surface-strong));
}

.stats-card--warning {
  border-color: rgba(var(--ion-color-warning-rgb), 0.12);
  background: linear-gradient(160deg, var(--app-warning-soft), var(--app-surface-strong));
}

.stats-card--success {
  border-color: rgba(var(--ion-color-success-rgb), 0.12);
  background: linear-gradient(160deg, var(--app-success-soft), var(--app-surface-strong));
}

.stats-card--secondary {
  border-color: rgba(var(--ion-color-secondary-rgb), 0.08);
  background: linear-gradient(160deg, var(--app-secondary-soft), var(--app-surface-strong));
}

.stats-card__title {
  display: block;
  color: var(--app-muted);
  font-size: 12px;
  font-weight: 600;
}

.stats-card__value-row {
  display: flex;
  align-items: baseline;
  gap: 3px;
  margin-top: 8px;
}

.stats-card__value {
  color: var(--app-heading);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
}

.stats-card__unit {
  color: var(--app-muted);
  font-size: 11px;
}

.stats-card__action {
  display: block;
  margin-top: 8px;
  color: var(--ion-color-primary);
  font-size: 11px;
  font-weight: 600;
}

.stats-skeleton {
  padding: 14px 12px;
  background: var(--app-surface);
}

.stats-skeleton__label {
  width: 56%;
  height: 12px;
}

.stats-skeleton__value {
  width: 48%;
  height: 26px;
  margin-top: 8px;
}

@media (max-width: 374px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
