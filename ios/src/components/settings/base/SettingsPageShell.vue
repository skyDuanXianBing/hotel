<template>
  <ion-page :class="pageClass">
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="backHref" />
        </ion-buttons>
        <ion-title>{{ title }}</ion-title>
        <ion-buttons v-if="toolbarActionLabel || slots.toolbarEnd" slot="end">
          <slot name="toolbarEnd">
            <ion-button :disabled="toolbarActionDisabled" @click="emit('toolbarAction')">
              {{ toolbarActionLabel }}
            </ion-button>
          </slot>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page" :class="contentClasses">
      <ion-refresher v-if="showRefresher" slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="refresherPullingText" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero" :class="heroClass">
        <p v-if="heroEyebrow" class="mobile-note" :class="eyebrowClass">
          {{ heroEyebrow }}
        </p>
        <h1 class="mobile-title">{{ heroTitle }}</h1>
        <p v-if="heroSubtitle" class="mobile-subtitle">{{ heroSubtitle }}</p>
        <div v-if="normalizedChips.length > 0" class="mobile-chip-row">
          <span v-for="chip in normalizedChips" :key="chip.label" class="mobile-chip" :class="chip.class">
            {{ chip.label }}
          </span>
        </div>
        <slot name="heroExtra" />
      </section>

      <div class="mobile-stack settings-page-shell__stack" :class="stackClass">
        <slot />
      </div>
    </ion-content>

    <ion-footer v-if="hasBottomActions" class="settings-page-shell__footer">
      <div class="settings-page-shell__footer-inner">
        <slot name="bottomActions" />
      </div>
    </ion-footer>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonFooter,
  IonHeader,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, useSlots } from 'vue'

interface SettingsChip {
  label: string
  class?: string
}

const props = withDefaults(
  defineProps<{
    backHref: string
    title: string
    heroTitle: string
    heroEyebrow?: string
    heroSubtitle?: string
    chips?: SettingsChip[]
    toolbarActionLabel?: string
    toolbarActionDisabled?: boolean
    showRefresher?: boolean
    refresherPullingText?: string
    pageClass?: string
    contentClass?: string
    heroClass?: string
    eyebrowClass?: string
    stackClass?: string
  }>(),
  {
    heroEyebrow: '',
    heroSubtitle: '',
    chips: () => [],
    toolbarActionLabel: '',
    toolbarActionDisabled: false,
    showRefresher: false,
    refresherPullingText: '',
    pageClass: '',
    contentClass: '',
    heroClass: '',
    eyebrowClass: '',
    stackClass: '',
  },
)

const emit = defineEmits<{
  refresh: [event: CustomEvent]
  toolbarAction: []
}>()

const slots = useSlots()

const normalizedChips = computed(() => props.chips.filter((chip) => chip.label))
const hasBottomActions = computed(() => Boolean(slots.bottomActions))
const contentClasses = computed(() => [
  props.contentClass,
  hasBottomActions.value ? 'settings-page-shell__content--with-bottom-actions' : '',
])

function handleRefresh(event: CustomEvent) {
  emit('refresh', event)
}
</script>
