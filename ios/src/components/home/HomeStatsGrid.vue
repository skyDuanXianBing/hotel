<template>
  <section class="stats-section mobile-dashboard-surface">
    <div class="stats-section__header">
      <h2 class="stats-section__title">{{ t('home.section.operations') }}</h2>
      <span class="stats-section__tag">{{ t('home.section.realtime') }}</span>
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
          <div class="stats-card__head">
            <span class="stats-card__title">{{ item.title }}</span>
          </div>

          <div class="stats-card__value-row">
            <strong class="stats-card__value">{{ item.value }}</strong>
          </div>
        </button>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonSkeletonText } from '@ionic/vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

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

defineProps<Props>()

const emit = defineEmits<{
  select: [item: HomeStatCardItem]
}>()

const handleSelect = (item: HomeStatCardItem) => {
  emit('select', item)
}
</script>

<style scoped>
.stats-section {
  padding: 18px 16px 16px;
  border-radius: var(--ios-pms-radius-card);
}

.stats-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 9px;
}

.stats-section__title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 20px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.stats-section__tag {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  color: var(--ios-pms-text-soft);
  box-shadow: none;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  row-gap: 6px;
  column-gap: 0;
  padding: 2px 0 0;
}

.stats-card,
.stats-skeleton {
  border: none;
  border-radius: 0;
}

.stats-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  min-height: 61px;
  padding: 5px 2px;
  appearance: none;
  overflow: visible;
  background: transparent;
  box-shadow: none;
  font: inherit;
  text-align: center;
  transition:
    background-color 0.18s ease,
    color 0.18s ease;
}

.stats-card:active {
  background: rgba(79, 123, 217, 0.04);
}

.stats-card:focus-visible {
  outline: none;
  background: rgba(79, 123, 217, 0.04);
}

.stats-card--primary {
  --stats-accent: #4e82ff;
}

.stats-card--warning {
  --stats-accent: #efb44f;
}

.stats-card--success {
  --stats-accent: #56c4a0;
}

.stats-card--secondary {
  --stats-accent: #9099b3;
}

.stats-card__head {
  display: block;
  order: 2;
}

.stats-card__title {
  display: block;
  color: var(--ios-pms-text-muted);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.25;
  text-align: center;
}

.stats-card__swatch {
  display: none;
}

.stats-card__value-row {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 0;
  margin-top: 0;
  margin-bottom: 5px;
}

.stats-card__value {
  color: var(--ios-pms-text-primary);
  font-size: 22px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.stats-card__unit {
  display: none;
}

.stats-card__footer {
  display: none;
}

.stats-card__action {
  display: none;
}

.stats-card__arrow {
  display: none;
}

.stats-skeleton {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 61px;
  padding: 5px 2px;
  background: transparent;
  box-shadow: none;
}

.stats-skeleton__label {
  order: 2;
  width: 52%;
  height: 10px;
}

.stats-skeleton__value {
  width: 40%;
  height: 22px;
  margin-top: 0;
  margin-bottom: 8px;
}

@media (max-width: 374px) {
  .stats-section {
    padding-left: 12px;
    padding-right: 12px;
  }

  .stats-card,
  .stats-skeleton {
    min-height: 57px;
  }

  .stats-card__value {
    font-size: 21px;
  }

  .stats-card__title {
    font-size: 13px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stats-card {
    transition: none;
  }
}
</style>
