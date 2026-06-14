<template>
  <div class="accommodation-shell" :class="{ 'is-sidebar-collapsed': isCollapsed }" :style="shellStyle">
    <aside class="accommodation-sidebar" :class="{ 'is-collapsed': isCollapsed }">
      <button type="button" class="sidebar-toggle" @click="toggleSidebar">
        <span class="sidebar-toggle-mark">
          <el-icon><MenuIcon /></el-icon>
        </span>
        <span v-if="!isCollapsed" class="sidebar-toggle-label">
          {{ t('accommodation.layout.collapseNav') }}
        </span>
        <el-icon v-if="!isCollapsed" class="sidebar-toggle-arrow"><ArrowLeft /></el-icon>
        <el-icon v-else class="sidebar-toggle-arrow"><ArrowRight /></el-icon>
      </button>

      <nav class="sidebar-nav" aria-label="Accommodation navigation">
        <div
          v-for="item in sidebarItems"
          :key="item.id"
          class="sidebar-section"
          :class="{
            'is-active': displayActiveSectionId === item.id,
            'is-standalone': !item.children?.length,
          }"
        >
          <button
            type="button"
            class="sidebar-parent"
            :title="isCollapsed ? t(item.labelKey) : undefined"
            @click="handleParentClick(item)"
          >
            <span class="sidebar-parent-icon">
              <el-icon>
                <component :is="item.icon" />
              </el-icon>
            </span>
            <span v-if="!isCollapsed" class="sidebar-parent-label">{{ t(item.labelKey) }}</span>
            <el-icon
              v-if="item.children?.length && !isCollapsed"
              class="sidebar-parent-arrow"
              :class="{ 'is-expanded': isSectionExpanded(item.id) }"
            >
              <ArrowRight />
            </el-icon>
          </button>

          <div
            v-if="item.children?.length && !isCollapsed && isSectionExpanded(item.id)"
            class="sidebar-children"
          >
            <button
              v-for="child in item.children"
              :key="child.id"
              type="button"
              class="sidebar-child"
              :class="{ 'is-active': activeChildPath === child.path }"
              @click="handleSidebarClick(child.path)"
            >
              <span class="sidebar-child-dot"></span>
              <span class="sidebar-child-label">{{ t(child.labelKey) }}</span>
            </button>
          </div>
        </div>
      </nav>
    </aside>

    <section class="accommodation-panel">
      <header
        class="panel-header"
        :class="{
          'panel-header--room-status': activeSectionId === 'room-status',
          'panel-header--room-table': activeSectionId === 'room-table',
          'panel-header--room-price': activeSectionId === 'room-price',
          'panel-header--cleaning': activeSectionId === 'cleaning',
          'panel-header--sidebar-collapsed': isCollapsed,
        }"
      >
        <AppTopNav
          v-bind="topNavBindings.props.value"
          @store-select="topNavBindings.onStoreSelect"
          @manage-stores="topNavBindings.onManageStores"
          @menu-click="topNavBindings.onMenuClick"
          @wallet-click="topNavBindings.onWalletClick"
          @inbox-click="topNavBindings.onInboxClick"
          @system-notification="topNavBindings.onSystemNotification"
          @order-notification="topNavBindings.onOrderNotification"
          @profile-click="topNavBindings.onProfileClick"
          @logout="topNavBindings.onLogout"
        />
      </header>

      <main class="panel-content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, watch, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft,
  ArrowRight,
  BrushFilled,
  House,
  Menu as MenuIcon,
  Money,
  Tickets,
} from '@element-plus/icons-vue'
import AppTopNav from '@/components/layout/AppTopNav.vue'
import { appTopNavBindingsKey } from '@/components/layout/appShellContext'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const topNavBindings = inject(appTopNavBindingsKey)

if (!topNavBindings) {
  throw new Error('AccommodationLayout requires top navigation bindings')
}

const SIDEBAR_STORAGE_KEY = 'accommodation-sidebar-collapsed'
const COLLAPSED_WIDTH = 84
const EXPANDED_WIDTH = 220

interface SidebarChildItem {
  id: string
  labelKey: string
  path: string
}

interface SidebarItem {
  id: string
  labelKey: string
  path: string
  icon: Component
  children?: SidebarChildItem[]
}

const isCollapsed = ref(
  typeof window === 'undefined'
    ? false
    : localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true',
)
const expandedSectionId = ref('')

const sidebarItems: SidebarItem[] = [
  {
    id: 'room-status',
    labelKey: 'accommodation.layout.roomStatus',
    path: '/accommodation/room-status/calendar',
    icon: House,
    children: [
      {
        id: 'room-status-calendar',
        labelKey: 'roomStatus.layout.calendar',
        path: '/accommodation/room-status/calendar',
      },
      {
        id: 'room-status-channel',
        labelKey: 'roomStatus.common.channel',
        path: '/accommodation/room-status/channel',
      },
    ],
  },
  {
    id: 'room-table',
    labelKey: 'accommodation.layout.roomTable',
    path: '/accommodation/room-table',
    icon: Tickets,
  },
  {
    id: 'room-price',
    labelKey: 'accommodation.layout.roomPriceInventory',
    path: '/accommodation/room-price-management',
    icon: Money,
  },
  {
    id: 'cleaning',
    labelKey: 'accommodation.layout.cleaning',
    path: '/accommodation/cleaning/overview',
    icon: BrushFilled,
  },
]

const activeSectionId = computed(() => {
  const path = route.path

  if (path.startsWith('/accommodation/room-status')) {
    return 'room-status'
  }
  if (path.startsWith('/accommodation/room-table')) {
    return 'room-table'
  }
  if (
    path.startsWith('/accommodation/room-price-management') ||
    path.startsWith('/accommodation/room-price-bulk-update') ||
    path.startsWith('/accommodation/room-price/')
  ) {
    return 'room-price'
  }
  if (
    path.startsWith('/accommodation/cleaning/') ||
    path.startsWith('/accommodation/housekeeping-list') ||
    path.startsWith('/accommodation/housekeeper-list')
  ) {
    return 'cleaning'
  }
  return ''
})

const activeChildPath = computed(() => {
  const path = route.path

  if (path.startsWith('/accommodation/room-status/calendar')) {
    return '/accommodation/room-status/calendar'
  }
  if (path.startsWith('/accommodation/room-status/channel')) {
    return '/accommodation/room-status/channel'
  }
  if (path.startsWith('/accommodation/room-price-management')) {
    return '/accommodation/room-price-management'
  }
  if (path.startsWith('/accommodation/room-price/change-history')) {
    return '/accommodation/room-price/change-history'
  }
  if (path.startsWith('/accommodation/cleaning/overview')) {
    return '/accommodation/cleaning/overview'
  }
  if (path.startsWith('/accommodation/cleaning/task-list')) {
    return '/accommodation/cleaning/task-list'
  }
  return ''
})

const displayActiveSectionId = computed(() => expandedSectionId.value || activeSectionId.value)

const shellStyle = computed(() => ({
  '--sidebar-width': `${isCollapsed.value ? COLLAPSED_WIDTH : EXPANDED_WIDTH}px`,
}))

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const isSectionExpanded = (sectionId: string) => expandedSectionId.value === sectionId

const handleParentClick = (item: SidebarItem) => {
  if (item.children?.length) {
    if (isSectionExpanded(item.id)) {
      expandedSectionId.value = ''
      return
    }

    expandedSectionId.value = item.id
    return
  }

  handleSidebarClick(item.path)
}

const handleSidebarClick = (path: string) => {
  if (path !== route.path) {
    router.push(path)
  }
}

watch(
  isCollapsed,
  (collapsed) => {
    if (typeof window === 'undefined') return
    localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed))
  },
  { immediate: true },
)

watch(
  activeSectionId,
  (sectionId) => {
    if (!sectionId || isCollapsed.value) {
      return
    }

    const targetSection = sidebarItems.find((item) => item.id === sectionId)
    if (targetSection?.children?.length) {
      expandedSectionId.value = sectionId
      return
    }

    expandedSectionId.value = ''
  },
  { immediate: true },
)

watch(
  isCollapsed,
  (collapsed) => {
    if (collapsed) {
      expandedSectionId.value = ''
      return
    }

    const targetSection = sidebarItems.find((item) => item.id === activeSectionId.value)
    if (targetSection?.children?.length) {
      expandedSectionId.value = targetSection.id
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.accommodation-shell {
  --sidebar-width: 84px;
  height: 100%;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  background: #fafafa;
}

.accommodation-sidebar {
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
  border-radius: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
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
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.sidebar-parent:hover {
  background: #f6f7f3;
  color: #20211d;
}

.sidebar-section.is-active > .sidebar-parent,
.sidebar-section.is-standalone.is-active > .sidebar-parent {
  background: #eef5ff;
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

.accommodation-sidebar.is-collapsed .sidebar-toggle {
  justify-content: center;
  padding: 0;
}

.accommodation-sidebar.is-collapsed .sidebar-nav {
  padding-left: 0;
  padding-right: 0;
}

.accommodation-sidebar.is-collapsed .sidebar-parent {
  justify-content: center;
  padding: 0;
}

.accommodation-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 18px 32px 14px;
  background: #f5f5f5;
  overflow: hidden;
}

.panel-header :deep(.top-nav) {
  --nav-center-shift: calc(var(--sidebar-width) / -2);
}

.panel-header--room-status :deep(.top-nav),
.panel-header--room-table :deep(.top-nav),
.panel-header--room-price :deep(.top-nav),
.panel-header--cleaning :deep(.top-nav) {
  --nav-center-shift: calc(-56px + ((var(--sidebar-width) - 84px) / 6));
  --nav-right-shift: -28px;
}

.panel-header--room-status,
.panel-header--room-table,
.panel-header--room-price,
.panel-header--cleaning {
  background: #f5f5f5;
}

.panel-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 24px;
  padding-bottom: 24px;
}

@media (max-width: 1280px) {
  .panel-header {
    padding-left: 24px;
    padding-right: 20px;
  }

  .panel-content {
    padding-right: 20px;
    padding-bottom: 20px;
  }
}
</style>
