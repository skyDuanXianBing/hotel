<template>
  <aside class="statistics-sidebar" :class="{ 'is-collapsed': collapsed }">
    <button type="button" class="sidebar-toggle" @click="emit('toggle')">
      <span class="sidebar-toggle-mark">
        <el-icon><MenuIcon /></el-icon>
      </span>
      <span v-if="!collapsed" class="sidebar-toggle-label">
        {{ t('stage6.components.statisticsSidebar.collapse') }}
      </span>
      <el-icon v-if="!collapsed" class="sidebar-toggle-arrow"><ArrowLeft /></el-icon>
      <el-icon v-else class="sidebar-toggle-arrow"><ArrowRight /></el-icon>
    </button>

    <nav class="sidebar-nav" aria-label="Statistics navigation">
      <div class="sidebar-section is-active">
        <button
          type="button"
          class="sidebar-parent"
          :title="collapsed ? t('stage6.components.statisticsSidebar.dataCenter') : undefined"
        >
          <span class="sidebar-parent-icon">
            <el-icon><TrendCharts /></el-icon>
          </span>
          <span v-if="!collapsed" class="sidebar-parent-label">
            {{ t('stage6.components.statisticsSidebar.dataCenter') }}
          </span>
        </button>

        <div v-if="!collapsed" class="sidebar-children">
          <button
            v-for="item in menuItems"
            :key="item.key"
            type="button"
            class="sidebar-child"
            :class="{ 'is-active': activeMenu === item.key }"
            @click="handleMenuSelect(item.path)"
          >
            <span class="sidebar-child-dot"></span>
            <span class="sidebar-child-label">{{ t(item.labelKey) }}</span>
          </button>
        </div>
      </div>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Menu as MenuIcon, TrendCharts } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle'): void
}>()

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const menuItems = [
  {
    key: 'overview',
    labelKey: 'stage6.components.statisticsSidebar.overview',
    path: '/data-center/overview',
  },
  {
    key: 'accommodation',
    labelKey: 'stage6.components.statisticsSidebar.accommodation',
    path: '/data-center/accommodation',
  },
  {
    key: 'notes',
    labelKey: 'stage6.components.statisticsSidebar.notes',
    path: '/data-center/notes',
  },
]

const activeMenu = computed(() => {
  if (route.path.startsWith('/data-center/accommodation')) {
    return 'accommodation'
  }
  if (route.path.startsWith('/data-center/notes')) {
    return 'notes'
  }
  return 'overview'
})

const handleMenuSelect = (path: string) => {
  if (path !== route.path) {
    router.push(path)
  }
}
</script>

<style scoped>
.statistics-sidebar {
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
  cursor: default;
  text-align: left;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.sidebar-section.is-active > .sidebar-parent {
  color: #2f7cf6;
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

.sidebar-child:hover {
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

.statistics-sidebar.is-collapsed .sidebar-toggle {
  justify-content: center;
  padding: 0;
}

.statistics-sidebar.is-collapsed .sidebar-parent {
  justify-content: center;
  padding: 0;
}
</style>
