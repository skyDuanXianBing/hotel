<template>
  <section class="quick-section mobile-dashboard-surface">
    <div class="quick-grid">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="quick-item"
        @click="handleSelect(item)"
      >
        <div class="quick-item__icon-shell">
          <img
            class="quick-item__icon"
            :src="item.iconSrc"
            alt=""
            draggable="false"
          />
        </div>
        <span class="quick-item__label">{{ item.title }}</span>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
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
  padding: 11px 18px 10px;
  border-radius: var(--ios-pms-radius-card);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  row-gap: 6px;
  column-gap: 8px;
}

.quick-item {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 2px;
  width: 100%;
  min-width: 0;
  min-height: 72px;
  padding: 2px 2px 1px;
  border: none;
  border-radius: var(--ios-pms-radius-icon);
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
  width: 38px;
  height: 38px;
  border-radius: var(--ios-pms-radius-icon);
  background: transparent;
}

.quick-item__icon {
  display: block;
  width: 34px;
  height: 34px;
  object-fit: contain;
  user-select: none;
  -webkit-user-drag: none;
}

.quick-item__label {
  display: -webkit-box;
  min-height: 34px;
  overflow: hidden;
  color: var(--ios-pms-text-secondary);
  font-size: 14px;
  font-weight: 400;
  text-align: center;
  line-height: 1.2;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

@media (max-width: 374px) {
  .quick-grid {
    row-gap: 5px;
    column-gap: 6px;
  }

  .quick-item {
    min-height: 68px;
  }

  .quick-item__icon-shell {
    width: 35px;
    height: 35px;
  }

  .quick-item__icon {
    width: 31px;
    height: 31px;
  }

  .quick-item__label {
    font-size: 12px;
    min-height: 29px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .quick-item {
    transition: none;
  }
}
</style>
