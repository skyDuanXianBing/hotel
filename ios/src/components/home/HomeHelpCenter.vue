<template>
  <section class="help-section">
    <div class="help-section__header">
      <h2 class="help-section__title">帮助中心</h2>
      <button type="button" class="help-section__more" @click="handleMore">全部</button>
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

        <div class="help-item__meta">
          <ion-icon :icon="chevronForwardOutline" class="help-item__arrow" />
        </div>
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
  padding: 18px 16px 22px;
  border: 1px solid var(--ios-pms-border-faint);
  border-radius: var(--ios-pms-radius-card);
  background: rgba(250, 252, 254, 0.7);
  box-shadow: var(--ios-pms-shadow-card);
}

.help-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.help-section__title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 20px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.help-section__more {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 11px;
  border: none;
  border-radius: var(--ios-pms-radius-pill);
  background: #eef5ff;
  color: var(--ios-pms-primary-strong);
  box-shadow: none;
  font: inherit;
  font-size: 13px;
  font-weight: 400;
}

.help-list {
  display: grid;
  gap: 8px;
}

.help-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  width: 100%;
  min-height: 72px;
  padding: 11px 12px;
  border: none;
  border-radius: 7px;
  background: #f7f8fa;
  box-shadow: none;
  font: inherit;
  text-align: left;
  transition:
    background-color 0.18s ease,
    opacity 0.18s ease;
}

.help-item + .help-item {
  border-top: none;
}

.help-item:active {
  background: #f0f4fb;
}

.help-item:focus-visible {
  outline: none;
  background: #f0f4fb;
}

.help-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 7px;
  box-shadow: none;
  font-size: 29px;
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
  min-width: 0;
}

.help-item__body strong {
  display: block;
  color: var(--ios-pms-text-primary);
  font-size: 15px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.3;
}

.help-item__body p {
  margin: 4px 0 0;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.help-item__meta {
  display: flex;
  align-items: center;
  justify-content: center;
  width: auto;
  height: auto;
  border-radius: 0;
  background: transparent;
}

.help-item__arrow {
  color: #858585;
  font-size: 26px;
}

@media (prefers-reduced-motion: reduce) {
  .help-item {
    transition: none;
  }
}
</style>
