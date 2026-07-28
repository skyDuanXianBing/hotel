<template>
  <ion-modal
    :class="['settings-form-modal', modalClass]"
    :is-open="isOpen"
    :backdrop-dismiss="backdropDismiss"
    @didDismiss="emit('didDismiss')"
  >
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ title }}</ion-title>
        <ion-buttons :slot="closeSlot">
          <ion-button
            class="settings-editor-modal__close-button"
            :disabled="closeDisabled"
            @click="emit('close')"
          >
            <ion-icon
              v-if="closeSlot === 'start'"
              class="settings-editor-modal__back-icon"
              :icon="chevronBackOutline"
              aria-hidden="true"
            />
            <span>
              {{
                closeText ||
                (closeSlot === 'start' ? $t('common.back') : $t('stage5Final.settings.close'))
              }}
            </span>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content
      class="mobile-page settings-modal-page settings-form-modal__page"
      :class="contentClass"
    >
      <section class="mobile-card settings-form-modal__card" :class="cardClass">
        <slot />

        <div
          v-if="slots.actions"
          class="settings-form-actions settings-form-modal__actions"
        >
          <slot name="actions" />
        </div>
      </section>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonModal,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { chevronBackOutline } from 'ionicons/icons'
import { useSlots } from 'vue'

withDefaults(
  defineProps<{
    isOpen: boolean
    title: string
    closeText?: string
    closeSlot?: 'start' | 'end'
    backdropDismiss?: boolean
    closeDisabled?: boolean
    modalClass?: string
    contentClass?: string
    cardClass?: string
  }>(),
  {
    closeText: '',
    closeSlot: 'start',
    backdropDismiss: true,
    closeDisabled: false,
    modalClass: '',
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
