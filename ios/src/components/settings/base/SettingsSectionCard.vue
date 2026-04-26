<template>
  <section class="mobile-card" :class="cardClass">
    <div v-if="showHeader" class="mobile-inline-row settings-section-card__header" :class="headerClass">
      <div v-if="title || note">
        <h2 v-if="title" class="mobile-section-title">{{ title }}</h2>
        <p v-if="note" class="mobile-note">{{ note }}</p>
      </div>
      <slot name="headerActions">
        <ion-spinner v-if="loading" name="crescent" />
      </slot>
    </div>

    <slot />

    <div v-if="slots.footer" class="settings-section-card__footer">
      <slot name="footer" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonSpinner } from '@ionic/vue'
import { computed, useSlots } from 'vue'

const props = withDefaults(
  defineProps<{
    title?: string
    note?: string
    loading?: boolean
    cardClass?: string
    headerClass?: string
  }>(),
  {
    title: '',
    note: '',
    loading: false,
    cardClass: '',
    headerClass: '',
  },
)

const slots = useSlots()

const showHeader = computed(() => {
  return Boolean(props.title || props.note || props.loading || slots.headerActions)
})
</script>
