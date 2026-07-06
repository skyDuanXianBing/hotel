<script setup lang="ts">
import { computed } from 'vue'
import type { WorkspaceTabItem } from '@/components/layout/workspace'

const props = withDefaults(
  defineProps<{
    modelValue: string
    tabs: WorkspaceTabItem[]
    ariaLabel?: string
    centerShiftOffset?: number
  }>(),
  {
    ariaLabel: 'Workspace tabs',
    centerShiftOffset: 0,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'tab-change', value: string): void
}>()

const tabsStyle = computed(() => ({
  '--workspace-tabs-extra-shift': `${props.centerShiftOffset}px`,
}))

const handleTabClick = (tab: WorkspaceTabItem) => {
  if (tab.disabled || tab.name === props.modelValue) {
    return
  }

  emit('update:modelValue', tab.name)
  emit('tab-change', tab.name)
}
</script>

<template>
  <div class="workspace-tabs-shell" :style="tabsStyle">
    <div class="workspace-tabs" :aria-label="ariaLabel" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.name"
        type="button"
        class="workspace-tab"
        :class="{ 'is-active': modelValue === tab.name }"
        :aria-selected="modelValue === tab.name"
        :disabled="tab.disabled"
        role="tab"
        @click="handleTabClick(tab)"
      >
        <span>{{ tab.label }}</span>
        <span v-if="tab.badge" class="workspace-tab-badge">{{ tab.badge }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.workspace-tabs-shell {
  --workspace-tabs-extra-shift: 0px;
  --workspace-tabs-center-shift: calc(
    -56px + ((var(--sidebar-width, 84px) - 84px) / 6) + var(--workspace-tabs-extra-shift)
  );
  display: flex;
  justify-content: center;
  width: 100%;
  padding: 2px 0 16px;
  background: transparent;
  flex: 0 0 auto;
}

.workspace-tabs {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 2px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 8px rgba(30, 30, 30, 0.04);
  transform: translateX(var(--workspace-tabs-center-shift));
  transition: transform 0.24s ease;
}

.workspace-tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
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
    color 0.2s ease,
    opacity 0.2s ease;
}

.workspace-tab:hover:not(:disabled) {
  background: #f2f2f2;
}

.workspace-tab.is-active,
.workspace-tab.is-active:hover {
  background: #111111;
  color: #ffffff;
}

.workspace-tab:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.workspace-tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 14px;
  height: 14px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ff6267;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  line-height: 14px;
}
</style>
