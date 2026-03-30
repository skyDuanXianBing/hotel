<template>
  <section class="quick-section">
    <h2 class="quick-section__title">快捷入口</h2>

    <div class="quick-grid">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="quick-item"
        @click="handleSelect(item)"
      >
        <div class="quick-item__icon" :class="`quick-item__icon--${item.tone}`">
          <ion-icon :icon="item.icon" />
        </div>
        <span class="quick-item__label">{{ item.title }}</span>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonIcon } from '@ionic/vue'

export interface HomeQuickActionItem {
  key: string
  title: string
  description: string
  icon: string
  tone: 'primary' | 'warning' | 'secondary' | 'success'
}

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
  padding: 18px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
}

.quick-section__title {
  margin: 0 0 14px;
  color: var(--app-heading);
  font-size: 16px;
  font-weight: 700;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 4px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 4px;
  border: none;
  border-radius: 14px;
  background: transparent;
  font: inherit;
  transition: background 0.15s ease;
}

.quick-item:active {
  background: var(--app-primary-soft);
}

.quick-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  font-size: 22px;
}

.quick-item__icon--primary {
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
}

.quick-item__icon--warning {
  background: var(--app-warning-soft);
  color: var(--ion-color-warning);
}

.quick-item__icon--secondary {
  background: var(--app-secondary-soft);
  color: var(--ion-color-secondary);
}

.quick-item__icon--success {
  background: var(--app-success-soft);
  color: var(--ion-color-success);
}

.quick-item__label {
  color: var(--app-heading);
  font-size: 12px;
  font-weight: 500;
  text-align: center;
  line-height: 1.3;
}

@media (max-width: 374px) {
  .quick-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
