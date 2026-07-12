<template>
  <div class="statistics-shell" :class="{ 'is-sidebar-collapsed': isCollapsed }" :style="shellStyle">
    <StatisticsSidebar
      :collapsed="isCollapsed"
      @toggle="toggleSidebar"
    />

    <section class="statistics-panel">
      <header class="statistics-panel-header">
        <AppTopNav
          v-bind="topNavBindings.props.value"
          @store-select="topNavBindings.onStoreSelect"
          @manage-stores="topNavBindings.onManageStores"
          @menu-click="topNavBindings.onMenuClick"
          @wallet-click="topNavBindings.onWalletClick"
          @inbox-click="topNavBindings.onInboxClick"
          @support-chat="topNavBindings.onSupportChat"
          @system-notification="topNavBindings.onSystemNotification"
          @order-notification="topNavBindings.onOrderNotification"
          @profile-click="topNavBindings.onProfileClick"
          @workspace-switch="topNavBindings.onWorkspaceSwitch"
          @logout="topNavBindings.onLogout"
        />
      </header>

      <main class="statistics-content">
        <slot />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import AppTopNav from '@/components/layout/AppTopNav.vue'
import { appTopNavBindingsKey } from '@/components/layout/appShellContext'
import StatisticsSidebar from '@/components/StatisticsSidebar.vue'

const topNavBindings = inject(appTopNavBindingsKey)

if (!topNavBindings) {
  throw new Error('StatisticsLayout requires top navigation bindings')
}

const SIDEBAR_STORAGE_KEY = 'statistics-sidebar-collapsed'
const COLLAPSED_WIDTH = 84
const EXPANDED_WIDTH = 220

const isCollapsed = ref(
  typeof window === 'undefined'
    ? false
    : localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true',
)

const shellStyle = computed(() => ({
  '--sidebar-width': `${isCollapsed.value ? COLLAPSED_WIDTH : EXPANDED_WIDTH}px`,
}))

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

watch(
  isCollapsed,
  (collapsed) => {
    if (typeof window === 'undefined') return
    localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed))
  },
  { immediate: true },
)
</script>

<style scoped>
.statistics-shell {
  --sidebar-width: 84px;
  height: 100vh;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  background: #f5f5f5;
}

.statistics-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.statistics-panel-header {
  padding: 18px 32px 14px;
  background: #f5f5f5;
  overflow: hidden;
}

.statistics-panel-header :deep(.top-nav) {
  --nav-center-shift: calc(-56px + ((var(--sidebar-width) - 84px) / 6));
  --nav-right-shift: -28px;
}

.statistics-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 24px 24px;
  background: #f5f5f5;
}

@media (max-width: 1280px) {
  .statistics-panel-header {
    padding-left: 24px;
    padding-right: 20px;
  }

  .statistics-content {
    padding-right: 20px;
    padding-bottom: 20px;
  }
}

@media (max-width: 768px) {
  .statistics-shell {
    grid-template-columns: 84px minmax(0, 1fr);
  }

  .statistics-content {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>
