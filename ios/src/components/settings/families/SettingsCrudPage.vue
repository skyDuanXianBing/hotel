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
    content-class="settings-page-block settings-crud-page"
    hero-class="settings-page-block__hero"
    eyebrow-class="settings-page-block__eyebrow"
    @toolbar-action="emit('toolbarAction')"
    @refresh="handleRefresh"
  >
    <SettingsSectionCard
      :title="sectionTitle"
      :note="sectionNote"
      :loading="loading"
      header-class="settings-page-block__section-header"
    >
      <template v-if="slots.sectionHeaderActions" #headerActions>
        <slot name="sectionHeaderActions" />
      </template>

      <slot />

      <template v-if="slots.sectionFooter" #footer>
        <slot name="sectionFooter" />
      </template>
    </SettingsSectionCard>

    <slot name="extraSections" />

    <SettingsEditorModal
      v-if="modalTitle"
      :is-open="modalOpen"
      :title="modalTitle"
      :backdrop-dismiss="modalBackdropDismiss"
      :close-disabled="modalCloseDisabled"
      @close="emit('dismissEditor')"
      @didDismiss="emit('dismissEditor')"
    >
      <slot name="modalContent" />

      <template v-if="slots.modalActions" #actions>
        <slot name="modalActions" />
      </template>
    </SettingsEditorModal>
  </SettingsPageShell>
</template>

<script setup lang="ts">
import { useSlots } from 'vue'
import SettingsEditorModal from '@/components/settings/base/SettingsEditorModal.vue'
import SettingsPageShell from '@/components/settings/base/SettingsPageShell.vue'
import SettingsSectionCard from '@/components/settings/base/SettingsSectionCard.vue'

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
    sectionTitle: string
    sectionNote?: string
    loading?: boolean
    modalOpen?: boolean
    modalTitle?: string
    modalBackdropDismiss?: boolean
    modalCloseDisabled?: boolean
  }>(),
  {
    heroEyebrow: '',
    heroSubtitle: '',
    chips: () => [],
    toolbarActionLabel: '',
    showRefresher: false,
    refresherPullingText: '',
    sectionNote: '',
    loading: false,
    modalOpen: false,
    modalTitle: '',
    modalBackdropDismiss: true,
    modalCloseDisabled: false,
  },
)

const emit = defineEmits<{
  toolbarAction: []
  refresh: [event: CustomEvent]
  dismissEditor: []
}>()

const slots = useSlots()

function handleRefresh(event: CustomEvent) {
  emit('refresh', event)
}
</script>
