<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ $t('roomStatus.common.cancelReservation') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">{{ $t('home.section.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="cancel-modal__hero">
        <strong>{{ reservationTitle }}</strong>
      </section>

      <ion-list inset>
        <ion-item>
          <ion-select v-model="form.reason" :label="$t('roomStatus.cancelReservation.reason')" label-placement="stacked" interface="action-sheet">
            <ion-select-option value="guest_cancel">{{ $t('roomStatus.cancelReservation.reasons.guest_cancel') }}</ion-select-option>
            <ion-select-option value="room_issue">{{ $t('roomStatus.cancelReservation.reasons.room_issue') }}</ion-select-option>
            <ion-select-option value="system_error">{{ $t('roomStatus.cancelReservation.reasons.system_error') }}</ion-select-option>
            <ion-select-option value="other">{{ $t('roomStatus.payment.bookingTypeOptions.other') }}</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item lines="none">
          <ion-textarea v-model="form.notes" auto-grow :label="$t('accommodation.common.remarks')" label-placement="stacked" :placeholder="$t('settingsStage4.autoCheckin.status.optional')" />
        </ion-item>
      </ion-list>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="modal-footer-actions">
          <ion-button fill="outline" @click="$emit('dismiss')">{{ $t('roomStatus.cancelReservation.back') }}</ion-button>
          <ion-button color="danger" :disabled="submitting" @click="handleSubmit">{{ $t('order.mobile.actions.confirmCancelOrder') }}</ion-button>
        </div>
      </ion-toolbar>
    </ion-footer>
  </ion-modal>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonFooter,
  IonHeader,
  IonItem,
  IonList,
  IonModal,
  IonSelect,
  IonSelectOption,
  IonTextarea,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ReservationDTO } from '@/api/reservation'

export interface CancelReservationSubmitPayload {
  reason: string
  notes: string
}

const props = defineProps<{
  isOpen: boolean
  reservation: ReservationDTO | null
  submitting: boolean
}>()

const { t } = useI18n()

const emit = defineEmits<{
  dismiss: []
  submit: [payload: CancelReservationSubmitPayload]
}>()

const form = ref<CancelReservationSubmitPayload>({
  reason: 'guest_cancel',
  notes: '',
})

const reservationTitle = computed(() => {
  if (!props.reservation) {
    return t('stage5Final.roomStatus.currentOrder')
  }
  return `${props.reservation.guestName} · ${props.reservation.orderNumber}`
})

function resetForm() {
  form.value = {
    reason: 'guest_cancel',
    notes: '',
  }
}

function handleSubmit() {
  emit('submit', {
    reason: form.value.reason,
    notes: form.value.notes.trim(),
  })
}

watch(
  () => props.isOpen,
  (isOpen) => {
    if (isOpen) {
      resetForm()
    }
  },
)
</script>

<style scoped>
.cancel-modal__hero {
  margin: 16px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(220, 38, 38, 0.08);
}

.cancel-modal__hero p {
  margin: 6px 0 0;
  color: var(--app-muted);
}

.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
}
</style>
