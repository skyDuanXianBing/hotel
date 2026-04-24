<template>
  <ion-modal :is-open="isOpen" :backdrop-dismiss="backdropDismiss" @didDismiss="emit('didDismiss')">
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ title }}</ion-title>
        <ion-buttons slot="end">
          <ion-button :disabled="closeDisabled" @click="emit('close')">
            {{ closeText }}
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="mobile-page settings-modal-page" :class="contentClass">
      <section class="mobile-card" :class="cardClass">
        <slot />

        <div v-if="slots.actions" class="settings-form-actions">
          <slot name="actions" />
        </div>
      </section>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import { IonButton, IonButtons, IonContent, IonHeader, IonModal, IonTitle, IonToolbar } from '@ionic/vue'
import { useSlots } from 'vue'

withDefaults(
  defineProps<{
    isOpen: boolean
    title: string
    closeText?: string
    backdropDismiss?: boolean
    closeDisabled?: boolean
    contentClass?: string
    cardClass?: string
  }>(),
  {
    closeText: '关闭',
    backdropDismiss: true,
    closeDisabled: false,
    contentClass: '',
    cardClass: '',
  },
)

const emit = defineEmits<{
  close: []
  didDismiss: []
}>()

const slots = useSlots()
</script>
