<template>
  <section class="help-section">
    <div class="help-section__header">
      <h2 class="help-section__title">帮助中心</h2>
      <button type="button" class="help-section__more" @click="handleMore">全部 ›</button>
    </div>

    <div class="help-list">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="help-item"
        @click="handleSelect(item)"
      >
        <div class="help-item__icon" :class="`help-item__icon--${item.tone}`">
          <ion-icon :icon="item.icon" />
        </div>
        <div class="help-item__body">
          <strong>{{ item.title }}</strong>
          <p>{{ item.description }}</p>
        </div>
        <ion-icon :icon="chevronForwardOutline" class="help-item__arrow" />
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonIcon } from '@ionic/vue'
import { chevronForwardOutline } from 'ionicons/icons'

export interface HomeHelpCenterItem {
  key: string
  title: string
  description: string
  icon: string
  tone: 'primary' | 'warning' | 'secondary' | 'success'
}

interface Props {
  items: HomeHelpCenterItem[]
}

defineProps<Props>()

const emit = defineEmits<{
  select: [item: HomeHelpCenterItem]
  more: []
}>()

function handleSelect(item: HomeHelpCenterItem) {
  emit('select', item)
}

function handleMore() {
  emit('more')
}
</script>

<style scoped>
.help-section {
  padding: 18px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
}

.help-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.help-section__title {
  margin: 0;
  color: var(--app-heading);
  font-size: 16px;
  font-weight: 700;
}

.help-section__more {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--ion-color-primary);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
}

.help-list {
  display: grid;
  gap: 8px;
}

.help-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  background: var(--app-surface-strong);
  font: inherit;
  text-align: left;
  transition: background 0.15s ease;
}

.help-item:active {
  background: var(--app-primary-soft);
}

.help-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  font-size: 18px;
}

.help-item__icon--primary {
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
}

.help-item__icon--warning {
  background: var(--app-warning-soft);
  color: var(--ion-color-warning);
}

.help-item__icon--secondary {
  background: var(--app-secondary-soft);
  color: var(--ion-color-secondary);
}

.help-item__icon--success {
  background: var(--app-success-soft);
  color: var(--ion-color-success);
}

.help-item__body {
  flex: 1;
  min-width: 0;
}

.help-item__body strong {
  display: block;
  color: var(--app-heading);
  font-size: 14px;
}

.help-item__body p {
  margin: 2px 0 0;
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.help-item__arrow {
  flex-shrink: 0;
  color: var(--app-muted);
  font-size: 16px;
  opacity: 0.5;
}
</style>
