<template>
  <SettingsPageShell
    :back-href="backHref"
    :title="title"
    :hero-eyebrow="heroEyebrow"
    :hero-title="heroTitle"
    :hero-subtitle="heroSubtitle"
    :chips="chips"
    :toolbar-action-label="toolbarActionLabel"
    :show-refresher="showRefresher"
    :refresher-pulling-text="refresherPullingText"
    content-class="settings-detail-page settings-detail-form-page"
    hero-class="settings-detail-hero"
    eyebrow-class="settings-detail-hero__eyebrow"
    @toolbar-action="emit('toolbarAction')"
    @refresh="handleRefresh"
  >
    <template v-if="$slots.heroExtra" #heroExtra>
      <slot name="heroExtra" />
    </template>
    <slot />
    <template v-if="$slots.bottomActions" #bottomActions>
      <slot name="bottomActions" />
    </template>
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
    toolbarActionLabel?: string
    showRefresher?: boolean
    refresherPullingText?: string
  }>(),
  {
    heroEyebrow: '',
    heroSubtitle: '',
    chips: () => [],
    toolbarActionLabel: '',
    showRefresher: false,
    refresherPullingText: '',
  },
)

const emit = defineEmits<{
  refresh: [event: CustomEvent]
  toolbarAction: []
}>()

function handleRefresh(event: CustomEvent) {
  emit('refresh', event)
}
</script>
