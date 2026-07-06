<template>
  <WorkspaceLayout
    storage-key="accommodation-sidebar-collapsed"
    content-padding="0 24px 24px 0"
    content-padding-narrow="0 20px 20px 0"
    content-padding-mobile="0 20px 20px 0"
  >
    <template #sidebar="{ collapsed, toggleSidebar }">
      <WorkspaceSidebar
        :collapsed="collapsed"
        :items="workspaceSidebarItems"
        :active-key="activeSidebarKey"
        :expanded-keys="expandedSidebarKeys"
        :collapse-label="t('accommodation.layout.collapseNav')"
        aria-label="Accommodation navigation"
        @toggle="handleSidebarToggle(toggleSidebar, collapsed)"
        @parent-click="handleWorkspaceParentClick"
        @item-select="handleWorkspaceItemSelect"
      />
    </template>

    <router-view />
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, ref, watch, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  BrushFilled,
  House,
  Money,
  Tickets,
} from '@element-plus/icons-vue'
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue'
import WorkspaceSidebar from '@/components/layout/WorkspaceSidebar.vue'
import type { WorkspaceSidebarItem } from '@/components/layout/workspace'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

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

const workspaceSidebarItems = computed<WorkspaceSidebarItem[]>(() =>
  sidebarItems.map((item) => ({
    key: item.id,
    label: t(item.labelKey),
    icon: item.icon,
    children: item.children?.map((child) => ({
      key: child.path,
      label: t(child.labelKey),
    })),
  })),
)

const expandedSidebarKeys = computed(() => (expandedSectionId.value ? [expandedSectionId.value] : []))

const activeSidebarKey = computed(() => {
  if (expandedSectionId.value && expandedSectionId.value !== activeSectionId.value) {
    return expandedSectionId.value
  }

  const activeSection = sidebarItems.find((item) => item.id === activeSectionId.value)
  if (activeSection?.children?.length) {
    return activeChildPath.value || activeSectionId.value
  }

  return activeSectionId.value
})

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

const restoreActiveExpandedSection = () => {
  const targetSection = sidebarItems.find((item) => item.id === activeSectionId.value)
  expandedSectionId.value = targetSection?.children?.length ? targetSection.id : ''
}

const handleWorkspaceParentClick = (key: string) => {
  const item = sidebarItems.find((sidebarItem) => sidebarItem.id === key)
  if (item) {
    handleParentClick(item)
  }
}

const handleWorkspaceItemSelect = (key: string) => {
  const parentItem = sidebarItems.find((item) => item.id === key)
  if (parentItem) {
    handleSidebarClick(parentItem.path)
    return
  }

  handleSidebarClick(key)
}

const handleSidebarToggle = (toggleSidebar: () => void, collapsed: boolean) => {
  if (collapsed) {
    restoreActiveExpandedSection()
  } else {
    expandedSectionId.value = ''
  }

  toggleSidebar()
}

watch(
  activeSectionId,
  (sectionId) => {
    if (!sectionId) {
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
</script>
