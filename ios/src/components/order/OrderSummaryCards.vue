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
        <span class="order-summary-cards__title">{{ item.title }}</span>
        <strong class="order-summary-cards__value">{{ item.value }}</strong>
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
  gap: 14px;
}

.order-summary-cards__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.order-summary-cards__item {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  text-align: left;
}

.order-summary-cards__item.is-active {
  border-color: var(--ion-color-primary);
  box-shadow: 0 10px 24px var(--app-primary-soft-strong);
}

.order-summary-cards__title,
.order-summary-cards__note {
  font-size: 12px;
}

.order-summary-cards__title {
  color: var(--app-muted);
}

.order-summary-cards__value {
  font-size: 24px;
  line-height: 1;
  color: var(--app-heading);
}

.order-summary-cards__note {
  color: var(--app-muted);
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
</style>
