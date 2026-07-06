<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import AppTopNav from '@/components/layout/AppTopNav.vue'
import { appTopNavBindingsKey } from '@/components/layout/appShellContext'

const props = withDefaults(
  defineProps<{
    storageKey: string
    collapsedWidth?: number
    expandedWidth?: number
    contentPadding?: string
    contentPaddingNarrow?: string
    contentPaddingMobile?: string
  }>(),
  {
    collapsedWidth: 84,
    expandedWidth: 220,
    contentPadding: '0 24px 24px',
    contentPaddingNarrow: '0 20px 20px 24px',
    contentPaddingMobile: '0 12px 20px',
  },
)

defineSlots<{
  sidebar(props: { collapsed: boolean; toggleSidebar: () => void }): unknown
  default(): unknown
}>()

const topNavBindings = inject(appTopNavBindingsKey)

if (!topNavBindings) {
  throw new Error('WorkspaceLayout requires top navigation bindings')
}

const isCollapsed = ref(
  typeof window === 'undefined' ? false : localStorage.getItem(props.storageKey) === 'true',
)

const shellStyle = computed(() => ({
  '--sidebar-width': `${isCollapsed.value ? props.collapsedWidth : props.expandedWidth}px`,
  '--workspace-content-padding': props.contentPadding,
  '--workspace-content-padding-narrow': props.contentPaddingNarrow,
  '--workspace-content-padding-mobile': props.contentPaddingMobile,
}))

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

watch(
  isCollapsed,
  (collapsed) => {
    if (typeof window === 'undefined') return
    localStorage.setItem(props.storageKey, String(collapsed))
  },
  { immediate: true },
)
</script>

<template>
  <div class="workspace-shell" :class="{ 'is-sidebar-collapsed': isCollapsed }" :style="shellStyle">
    <slot name="sidebar" :collapsed="isCollapsed" :toggle-sidebar="toggleSidebar" />

    <section class="workspace-panel">
      <header class="workspace-panel-header">
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
          @logout="topNavBindings.onLogout"
        />
      </header>

      <main class="workspace-content">
        <slot />
      </main>
    </section>
  </div>
</template>

<style scoped>
.workspace-shell {
  --sidebar-width: 84px;
  height: 100%;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  background: #f5f5f5;
}

.workspace-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.workspace-panel-header {
  padding: 18px 32px 14px;
  background: #f5f5f5;
  overflow: hidden;
}

.workspace-panel-header :deep(.top-nav) {
  --nav-center-shift: calc(-56px + ((var(--sidebar-width) - 84px) / 6));
  --nav-right-shift: -28px;
}

.workspace-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding: var(--workspace-content-padding);
  background: #f5f5f5;
}

@media (max-width: 1280px) {
  .workspace-panel-header {
    padding-left: 24px;
    padding-right: 20px;
  }

  .workspace-content {
    padding: var(--workspace-content-padding-narrow);
  }
}

@media (max-width: 768px) {
  .workspace-shell {
    grid-template-columns: 84px minmax(0, 1fr);
  }

  .workspace-content {
    padding: var(--workspace-content-padding-mobile);
  }
}
</style>
