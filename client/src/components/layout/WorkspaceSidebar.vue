<script setup lang="ts">
import { computed } from 'vue'
import { ArrowLeft, ArrowRight, Menu as MenuIcon } from '@element-plus/icons-vue'
import type { WorkspaceSidebarChildItem, WorkspaceSidebarItem } from '@/components/layout/workspace'

const props = withDefaults(
  defineProps<{
    collapsed: boolean
    items: WorkspaceSidebarItem[]
    activeKey: string
    expandedKeys?: string[]
    collapseLabel: string
    ariaLabel?: string
  }>(),
  {
    expandedKeys: () => [],
    ariaLabel: 'Workspace navigation',
  },
)

const emit = defineEmits<{
  (e: 'toggle'): void
  (e: 'parent-click', key: string): void
  (e: 'item-select', key: string): void
}>()

const expandedKeySet = computed(() => new Set(props.expandedKeys))

const isItemActive = (item: WorkspaceSidebarItem) =>
  props.activeKey === item.key || Boolean(item.children?.some((child) => child.key === props.activeKey))

const isItemExpanded = (item: WorkspaceSidebarItem) => expandedKeySet.value.has(item.key)

const handleParentClick = (item: WorkspaceSidebarItem) => {
  if (item.disabled) {
    return
  }

  if (item.children?.length) {
    emit('parent-click', item.key)
    return
  }

  emit('item-select', item.key)
}

const handleChildClick = (child: WorkspaceSidebarChildItem) => {
  if (child.disabled) {
    return
  }

  emit('item-select', child.key)
}
</script>

<template>
  <aside class="workspace-sidebar" :class="{ 'is-collapsed': collapsed }">
    <button type="button" class="sidebar-toggle" @click="emit('toggle')">
      <span class="sidebar-toggle-mark">
        <el-icon><MenuIcon /></el-icon>
      </span>
      <span v-if="!collapsed" class="sidebar-toggle-label">
        {{ collapseLabel }}
      </span>
      <el-icon v-if="!collapsed" class="sidebar-toggle-arrow"><ArrowLeft /></el-icon>
      <el-icon v-else class="sidebar-toggle-arrow"><ArrowRight /></el-icon>
    </button>

    <nav class="sidebar-nav" :aria-label="ariaLabel">
      <div
        v-for="item in items"
        :key="item.key"
        class="sidebar-section"
        :class="{
          'is-active': isItemActive(item),
          'is-standalone': !item.children?.length,
          'is-disabled': item.disabled,
        }"
      >
        <button
          type="button"
          class="sidebar-parent"
          :disabled="item.disabled"
          :title="collapsed ? item.label : undefined"
          @click="handleParentClick(item)"
        >
          <span class="sidebar-parent-icon">
            <el-icon v-if="item.icon">
              <component :is="item.icon" />
            </el-icon>
          </span>
          <span v-if="!collapsed" class="sidebar-parent-label">{{ item.label }}</span>
          <el-icon
            v-if="item.children?.length && !collapsed"
            class="sidebar-parent-arrow"
            :class="{ 'is-expanded': isItemExpanded(item) }"
          >
            <ArrowRight />
          </el-icon>
        </button>

        <div
          v-if="item.children?.length && !collapsed && isItemExpanded(item)"
          class="sidebar-children"
        >
          <button
            v-for="child in item.children"
            :key="child.key"
            type="button"
            class="sidebar-child"
            :class="{ 'is-active': activeKey === child.key, 'is-disabled': child.disabled }"
            :disabled="child.disabled"
            @click="handleChildClick(child)"
          >
            <span class="sidebar-child-dot"></span>
            <span class="sidebar-child-label">{{ child.label }}</span>
          </button>
        </div>
      </div>
    </nav>
  </aside>
</template>

<style scoped>
.workspace-sidebar {
  width: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #ffffff;
  border-right: 1px solid #ecece7;
  transition: width 0.24s ease;
  overflow: hidden;
}

.sidebar-toggle {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  border: none;
  border-bottom: 1px solid #f0f0ea;
  background: #ffffff;
  color: #1f2120;
  cursor: pointer;
}

.sidebar-toggle-mark {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #2f7cf6;
  font-size: 20px;
  flex: 0 0 auto;
}

.sidebar-toggle-label {
  flex: 1;
  min-width: 0;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: #232421;
  white-space: nowrap;
}

.sidebar-toggle-arrow {
  color: #a8ada8;
  font-size: 14px;
  flex: 0 0 auto;
}

.sidebar-nav {
  flex: 1;
  min-height: 0;
  padding: 18px 0 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sidebar-parent {
  position: relative;
  height: 44px;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 0 18px 0 20px;
  border: none;
  border-radius: 0;
  background: transparent;
  color: #5f645f;
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.sidebar-parent:hover:not(:disabled) {
  background: #f6f7f3;
  color: #20211d;
}

.sidebar-section.is-active > .sidebar-parent,
.sidebar-section.is-standalone.is-active > .sidebar-parent {
  background: #eef5ff;
  color: #2f7cf6;
}

.sidebar-parent:disabled,
.sidebar-child:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.sidebar-parent-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex: 0 0 auto;
}

.sidebar-parent-label {
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-parent-arrow {
  margin-left: auto;
  font-size: 14px;
  color: #a8ada8;
  transition:
    transform 0.2s ease,
    color 0.2s ease;
}

.sidebar-parent-arrow.is-expanded {
  transform: rotate(90deg);
  color: currentColor;
}

.sidebar-children {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 0;
}

.sidebar-child {
  height: 36px;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 0 18px 0 48px;
  border: none;
  border-radius: 0;
  background: transparent;
  color: #717572;
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.sidebar-child:hover:not(:disabled) {
  background: #f7f8f4;
  color: #232421;
}

.sidebar-child.is-active {
  background: #f1f6ff;
  color: #2f7cf6;
}

.sidebar-child-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: currentColor;
  opacity: 0.45;
  flex: 0 0 auto;
}

.sidebar-child.is-active .sidebar-child-dot {
  opacity: 1;
}

.sidebar-child-label {
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.workspace-sidebar.is-collapsed .sidebar-toggle,
.workspace-sidebar.is-collapsed .sidebar-parent {
  justify-content: center;
  padding: 0;
}

.workspace-sidebar.is-collapsed .sidebar-nav {
  padding-left: 0;
  padding-right: 0;
}

@media (max-width: 768px) {
  .workspace-sidebar {
    width: 84px;
  }

  .workspace-sidebar .sidebar-toggle,
  .workspace-sidebar .sidebar-parent {
    justify-content: center;
    padding: 0;
  }
}
</style>
