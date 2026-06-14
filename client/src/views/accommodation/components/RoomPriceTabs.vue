<template>
  <div class="room-price-tabs-shell">
    <div class="room-price-tabs" aria-label="Room price tabs" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.path"
        type="button"
        class="room-price-tab"
        :class="{ 'is-active': isActive(tab.path) }"
        :aria-selected="isActive(tab.path)"
        @click="navigateTo(tab.path)"
      >
        {{ t(tab.labelKey) }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const tabs = [
  {
    path: '/accommodation/room-price-management',
    labelKey: 'accommodation.layout.roomPriceManagement',
  },
  {
    path: '/accommodation/room-price/change-history',
    labelKey: 'accommodation.layout.priceChangeHistory',
  },
]

const isActive = (path: string) => {
  if (path === '/accommodation/room-price-management') {
    return route.path.startsWith('/accommodation/room-price-management')
  }

  return route.path.startsWith(path)
}

const navigateTo = (path: string) => {
  if (route.path !== path) {
    router.push(path)
  }
}
</script>

<style scoped>
.room-price-tabs-shell {
  --room-price-tabs-center-shift: calc(
    -56px + ((var(--sidebar-width, 84px) - 84px) / 6) + 20px
  );
  display: flex;
  justify-content: center;
  padding: 2px 0 16px;
  background: transparent;
}

.room-price-tabs {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 8px rgba(30, 30, 30, 0.04);
  transform: translateX(var(--room-price-tabs-center-shift));
  transition: transform 0.24s ease;
}

.room-price-tab {
  min-width: 82px;
  height: 24px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #252525;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.room-price-tab:hover {
  background: #f2f2f2;
}

.room-price-tab.is-active,
.room-price-tab.is-active:hover {
  background: #111111;
  color: #ffffff;
}
</style>
