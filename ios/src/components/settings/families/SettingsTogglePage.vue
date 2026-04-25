<template>
  <SettingsPageShell
    :back-href="backHref"
    :title="title"
    :hero-eyebrow="heroEyebrow"
    :hero-title="heroTitle"
    :hero-subtitle="heroSubtitle"
    :chips="chips"
    :show-refresher="showRefresher"
    :refresher-pulling-text="refresherPullingText"
    content-class="settings-page-block settings-toggle-page"
    hero-class="settings-page-block__hero"
    eyebrow-class="settings-page-block__eyebrow"
    @refresh="handleRefresh"
  >
    <slot />
  </SettingsPageShell>
</template>

<script setup lang="ts">
import SettingsPageShell from '@/components/settings/base/SettingsPageShell.vue'

interface SettingsChip {
  label: string
  class?: string
}

withDefaults(
  defineProps<{
    backHref: string
    title: string
    heroEyebrow?: string
    heroTitle: string
    heroSubtitle?: string
    chips?: SettingsChip[]
    showRefresher?: boolean
    refresherPullingText?: string
  }>(),
  {
    heroEyebrow: '',
    heroSubtitle: '',
    chips: () => [],
    showRefresher: false,
    refresherPullingText: '',
  },
)

const emit = defineEmits<{
  refresh: [event: CustomEvent]
}>()

function handleRefresh(event: CustomEvent) {
  emit('refresh', event)
}
</script>
