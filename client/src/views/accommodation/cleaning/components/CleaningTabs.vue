<template>
  <div class="cleaning-tabs-shell">
    <div class="cleaning-tabs" aria-label="Cleaning tabs" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.path"
        type="button"
        class="cleaning-tab"
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
    path: '/accommodation/cleaning/overview',
    labelKey: 'accommodation.layout.taskOverview',
  },
  {
    path: '/accommodation/cleaning/task-list',
    labelKey: 'accommodation.layout.taskList',
  },
]

const isActive = (path: string) => route.path.startsWith(path)

const navigateTo = (path: string) => {
  if (!isActive(path)) {
    router.push(path)
  }
}
</script>

<style scoped>
.cleaning-tabs-shell {
  --cleaning-tabs-center-shift: calc(
    -56px + ((var(--sidebar-width, 84px) - 84px) / 6) + 20px
  );
  display: flex;
  justify-content: center;
  width: 100%;
  padding: 2px 0 0;
  margin: 0 0 18px;
}

.cleaning-tabs {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 2px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 8px rgba(30, 30, 30, 0.04);
  transform: translateX(var(--cleaning-tabs-center-shift));
  transition: transform 0.24s ease;
}

.cleaning-tab {
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

.cleaning-tab:hover {
  background: #f2f2f2;
}

.cleaning-tab.is-active,
.cleaning-tab.is-active:hover {
  background: #111111;
  color: #ffffff;
}
</style>
