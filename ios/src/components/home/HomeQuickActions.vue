<template>
  <section class="quick-section">
    <div class="quick-grid">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="quick-item"
        @click="handleSelect(item)"
      >
        <div class="quick-item__icon-shell">
          <div class="quick-item__icon" :class="`quick-item__icon--${item.tone}`">
            <ion-icon :icon="item.icon" />
          </div>
        </div>
        <span class="quick-item__label">{{ item.title }}</span>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonIcon } from '@ionic/vue'
import type { HomeQuickActionItem } from '@/constants/homeQuickActions'

interface Props {
  items: HomeQuickActionItem[]
}

defineProps<Props>()

const emit = defineEmits<{
  select: [item: HomeQuickActionItem]
}>()

const handleSelect = (item: HomeQuickActionItem) => {
  emit('select', item)
}
</script>

<style scoped>
.quick-section {
  padding: 14px 18px 12px;
  border: 1px solid var(--ios-pms-border-soft);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-surface);
  box-shadow: var(--ios-pms-shadow-card-strong);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px 12px;
}

.quick-item {
  display: grid;
  justify-items: center;
  gap: 5px;
  width: 100%;
  padding: 6px 6px 4px;
  border: none;
  border-radius: 16px;
  background: transparent;
  box-shadow: none;
  font: inherit;
  transition:
    transform 0.18s ease,
    background-color 0.18s ease;
}

.quick-item:active {
  transform: scale(0.985);
  background: rgba(79, 123, 217, 0.05);
}

.quick-item:focus-visible {
  outline: none;
  background: rgba(79, 123, 217, 0.05);
}

.quick-item__icon-shell {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: transparent;
}

.quick-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 14px;
  box-shadow: none;
  font-size: 22px;
}

.quick-item__icon--primary {
  background: transparent;
  color: var(--ion-color-primary);
}

.quick-item__icon--warning {
  background: transparent;
  color: var(--ion-color-warning);
}

.quick-item__icon--secondary {
  background: transparent;
  color: var(--ion-color-secondary);
}

.quick-item__icon--success {
  background: transparent;
  color: var(--ion-color-success);
}

.quick-item__label {
  color: var(--ios-pms-text-secondary);
  font-size: var(--ios-pms-font-body-md-size);
  font-weight: var(--ios-pms-weight-bold);
  text-align: center;
  line-height: 1.15;
}

@media (max-width: 374px) {
  .quick-grid {
    gap: 7px 10px;
  }

  .quick-item {
    padding-left: 4px;
    padding-right: 4px;
    padding-top: 5px;
    padding-bottom: 3px;
  }

  .quick-item__icon-shell,
  .quick-item__icon {
    width: 38px;
    height: 38px;
  }

  .quick-item__icon {
    font-size: 20px;
  }

  .quick-item__label {
    font-size: 11px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .quick-item {
    transition: none;
  }
}
</style>
