<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ modalTitle }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="assign-modal__hero">
        <strong>{{ reservationLabel }}</strong>
      </section>

      <ion-list inset>
        <ion-item>
          <ion-select
            :model-value="selectedRoomTypeId"
            interface="action-sheet"
            label="房型"
            label-placement="stacked"
            placeholder="请选择房型"
            @ionChange="$emit('selectRoomType', Number($event.detail.value || 0))"
          >
            <ion-select-option v-for="item in roomTypes" :key="item.id" :value="item.id">
              {{ item.name }}（可用 {{ item.availableRooms }}）
            </ion-select-option>
          </ion-select>
        </ion-item>

        <ion-item>
          <ion-select
            :disabled="!selectedRoomTypeId"
            :model-value="selectedRoomId"
            interface="action-sheet"
            label="房间号"
            label-placement="stacked"
            placeholder="请选择房间"
            @ionChange="$emit('selectRoom', Number($event.detail.value || 0))"
          >
            <ion-select-option v-for="item in rooms" :key="item.id" :value="item.id">
              {{ item.roomTypeName }} - {{ item.roomNumber }}
            </ion-select-option>
          </ion-select>
        </ion-item>
      </ion-list>


      <div v-if="loading" class="assign-modal__loading">
        <ion-spinner name="crescent" />
      </div>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="assign-modal__footer">
          <ion-button fill="outline" :disabled="loading" @click="$emit('refresh')">刷新房间</ion-button>
          <ion-button :disabled="submitting || !selectedRoomId" @click="$emit('submit')">
            确认排房
          </ion-button>
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
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed } from 'vue'
import type {
  AssignableRoomDTO,
  AssignableRoomTypeDTO,
  ReservationDTO,
} from '@/api/reservation'

const props = defineProps<{
  isOpen: boolean
  reservation: ReservationDTO | null
  roomTypes: AssignableRoomTypeDTO[]
  rooms: AssignableRoomDTO[]
  selectedRoomTypeId: number | null
  selectedRoomId: number | null
  loading: boolean
  submitting: boolean
}>()

defineEmits<{
  dismiss: []
  refresh: []
  selectRoomType: [roomTypeId: number]
  selectRoom: [roomId: number]
  submit: []
}>()

const modalTitle = computed(() => {
  if (props.reservation?.roomId) {
    return '编辑排房'
  }
  return '进行排房'
})

const reservationLabel = computed(() => {
  if (!props.reservation) {
    return '未选择订单'
  }
  return `${props.reservation.guestName} · ${props.reservation.orderNumber}`
})

const reservationDates = computed(() => {
  if (!props.reservation) {
    return '-'
  }
  return `${props.reservation.checkInDate} 入住 · ${props.reservation.checkOutDate} 离店`
})
</script>

<style scoped>
.assign-modal__hero,
.assign-modal__note {
  margin: 16px;
  padding: 16px;
  border-radius: 20px;
}

.assign-modal__hero {
  background: var(--app-primary-soft);
}

.assign-modal__note {
  background: rgba(16, 35, 63, 0.04);
}

.assign-modal__hero p,
.assign-modal__note p {
  margin: 6px 0 0;
  color: var(--app-muted);
  font-size: 13px;
}

.assign-modal__loading {
  display: flex;
  justify-content: center;
  padding-bottom: 12px;
}

.assign-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
}
</style>
