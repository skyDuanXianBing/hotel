<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>取消预约</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="cancel-modal__hero">
        <strong>{{ reservationTitle }}</strong>
      </section>

      <ion-list inset>
        <ion-item>
          <ion-select v-model="form.reason" label="取消原因" label-placement="stacked" interface="action-sheet">
            <ion-select-option value="guest_cancel">客人主动取消</ion-select-option>
            <ion-select-option value="room_issue">房间问题</ion-select-option>
            <ion-select-option value="system_error">系统错误</ion-select-option>
            <ion-select-option value="other">其他</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item lines="none">
          <ion-textarea v-model="form.notes" auto-grow label="备注" label-placement="stacked" placeholder="可选" />
        </ion-item>
      </ion-list>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="modal-footer-actions">
          <ion-button fill="outline" @click="$emit('dismiss')">返回</ion-button>
          <ion-button color="danger" :disabled="submitting" @click="handleSubmit">确认取消</ion-button>
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
    return '当前订单'
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
