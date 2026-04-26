<template>
  <section class="mobile-card order-summary-cards">
    <div class="mobile-inline-row order-summary-cards__header">
      <div>
        <h2 class="mobile-section-title">今日摘要</h2>
        <p class="mobile-note">点击摘要可快速切换到对应订单视图。</p>
      </div>
      <ion-spinner v-if="loading" name="crescent" />
    </div>

    <div class="order-summary-cards__grid">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="order-summary-cards__item"
        :class="[`is-${item.tone}`, { 'is-active': item.key === activeKey }]"
        @click="$emit('select', item.key)"
      >
        <div class="order-summary-cards__item-top">
          <span class="order-summary-cards__title">{{ item.title }}</span>
          <strong class="order-summary-cards__value">{{ item.value }}</strong>
        </div>
        <span class="order-summary-cards__note">{{ item.note }}</span>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonSpinner } from '@ionic/vue'
import type { OrderSummaryCardItem, OrderTabValue } from '@/components/order/orderUtils'

defineProps<{
  items: OrderSummaryCardItem[]
  activeKey: OrderTabValue
  loading: boolean
}>()

defineEmits<{
  select: [key: OrderTabValue]
}>()
</script>

<style scoped>
.order-summary-cards {
  display: grid;
  gap: var(--ios-pms-space-3);
}

.order-summary-cards__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ios-pms-space-2);
  padding-top: 2px;
}

.order-summary-cards__item {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--ios-pms-border-faint);
  border-radius: var(--ios-pms-radius-card-sm);
  background: transparent;
  text-align: left;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.order-summary-cards__item.is-active {
  border-color: rgba(var(--ion-color-primary-rgb), 0.16);
  background: var(--ios-pms-primary-soft);
}

.order-summary-cards__item-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--ios-pms-space-2);
}

.order-summary-cards__title,
.order-summary-cards__note {
  font-size: var(--ios-pms-font-body-sm-size);
}

.order-summary-cards__title {
  min-width: 0;
  color: var(--ios-pms-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-summary-cards__value {
  flex-shrink: 0;
  font-size: var(--ios-pms-font-metric-md-size);
  line-height: 1;
  color: var(--ios-pms-text-primary);
  font-weight: var(--ios-pms-weight-heavy);
}

.order-summary-cards__note {
  color: var(--ios-pms-text-muted);
  line-height: 1.45;
}

.order-summary-cards__item.is-primary .order-summary-cards__value,
.order-summary-cards__item.is-primary .order-summary-cards__note {
  color: var(--ion-color-primary);
}

.order-summary-cards__item.is-warning .order-summary-cards__value,
.order-summary-cards__item.is-warning .order-summary-cards__note {
  color: var(--ion-color-warning);
}

.order-summary-cards__item.is-secondary .order-summary-cards__value,
.order-summary-cards__item.is-secondary .order-summary-cards__note {
  color: var(--ion-color-secondary);
}

.order-summary-cards__item.is-success .order-summary-cards__value,
.order-summary-cards__item.is-success .order-summary-cards__note {
  color: var(--ion-color-success);
}

.order-summary-cards__item.is-danger .order-summary-cards__value,
.order-summary-cards__item.is-danger .order-summary-cards__note {
  color: var(--ion-color-danger);
}

.order-summary-cards__item:active {
  background: var(--ios-pms-primary-soft-active);
}
</style>
