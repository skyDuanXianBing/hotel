<template>
  <ion-modal :is-open="isOpen" @didDismiss="handleDismiss">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ modalTitle }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleDismiss">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="booking-modal__content">
      <section class="booking-modal__hero">
        <strong>{{ roomLabel }}</strong>
        <p>{{ form.checkInDate }} 入住 · {{ form.checkOutDate }} 离店</p>
      </section>

      <ion-list inset>
        <ion-item>
          <ion-input v-model="form.guestName" label="姓名" label-placement="stacked" placeholder="请输入客人姓名" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.guestPhone" label="手机" label-placement="stacked" placeholder="请输入手机号" />
        </ion-item>
        <ion-item>
          <ion-select
            v-model="form.channelId"
            interface="action-sheet"
            label="渠道"
            label-placement="stacked"
            placeholder="请选择渠道"
          >
            <ion-select-option v-for="item in channels" :key="item.id" :value="item.id">
              {{ item.name }}
            </ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item>
          <ion-input v-model="form.checkInDate" type="date" label="入住日期" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.checkOutDate" type="date" label="离店日期" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model.number="form.adults" type="number" min="1" label="成人" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model.number="form.children" type="number" min="0" label="儿童" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input
            v-model.number="form.totalAmount"
            type="number"
            min="0"
            label="订单金额"
            label-placement="stacked"
          />
        </ion-item>
        <ion-item lines="none">
          <ion-textarea
            v-model="form.notes"
            auto-grow
            label="备注"
            label-placement="stacked"
            placeholder="可填写特殊需求、渠道号或入住备注"
          />
        </ion-item>
      </ion-list>

    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="booking-modal__footer">
          <ion-button fill="outline" @click="handleDismiss">取消</ion-button>
          <ion-button :disabled="submitting || priceLoading" @click="handleSubmit">
            {{ submitText }}
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
  IonInput,
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
import { calculateTotalPriceByDates, getRoomTypeByRoomId, type RoomTypeDTO } from '@/api/roomType'
import type { ChannelDTO, ReservationDTO } from '@/api/reservation'
import { addDays, type RoomStatusRoomItem } from '@/stores/roomStatus'

export type BookingModalMode = 'create' | 'check-in' | 'edit'

export interface BookingFormSubmitPayload {
  guestName: string
  guestPhone: string
  channelId: number
  checkInDate: string
  checkOutDate: string
  adults: number
  children: number
  totalAmount: number
  notes: string
}

const props = defineProps<{
  isOpen: boolean
  mode: BookingModalMode
  channels: ChannelDTO[]
  room: RoomStatusRoomItem | null
  reservation?: ReservationDTO | null
  submitting: boolean
}>()

const emit = defineEmits<{
  dismiss: []
  submit: [payload: BookingFormSubmitPayload]
}>()

const form = ref({
  guestName: '',
  guestPhone: '',
  channelId: 0,
  checkInDate: '',
  checkOutDate: '',
  adults: 1,
  children: 0,
  totalAmount: 0,
  notes: '',
})
const roomType = ref<RoomTypeDTO | null>(null)
const priceLoading = ref(false)

const modalTitle = computed(() => {
  if (props.mode === 'check-in') {
    return '直接入住'
  }
  if (props.mode === 'edit') {
    return '修改订单'
  }
  return '快速预订'
})

const submitText = computed(() => {
  if (props.mode === 'check-in') {
    return '确认入住'
  }
  if (props.mode === 'edit') {
    return '保存修改'
  }
  return '提交预订'
})

const roomLabel = computed(() => {
  if (props.room) {
    return `${props.room.roomType} · ${props.room.roomNumber}`
  }
  if (props.reservation?.roomTypeName || props.reservation?.roomNumber) {
    return `${props.reservation.roomTypeName || '房间'} · ${props.reservation.roomNumber || '待排房'}`
  }
  return '未选择房间'
})

const priceText = computed(() => {
  return priceLoading.value ? '计算中...' : `¥${Number(form.value.totalAmount || 0).toFixed(2)}`
})

function getRoomId() {
  if (props.room?.roomId) {
    return props.room.roomId
  }
  if (props.reservation?.roomId) {
    return props.reservation.roomId
  }
  return 0
}

async function loadRoomType() {
  const roomId = getRoomId()
  if (!roomId) {
    roomType.value = null
    return
  }

  priceLoading.value = true
  try {
    const response = await getRoomTypeByRoomId(roomId)
    if (response.success && response.data) {
      roomType.value = response.data
      updatePrice()
    }
  } finally {
    priceLoading.value = false
  }
}

function updatePrice() {
  if (!roomType.value) {
    return
  }

  form.value.totalAmount = calculateTotalPriceByDates(
    roomType.value,
    form.value.checkInDate,
    form.value.checkOutDate,
  )
}

function buildDefaultForm() {
  const channelId = props.channels[0]?.id || 0
  const baseDate = props.room?.focusedDate || props.reservation?.checkInDate || new Date().toISOString().split('T')[0]
  return {
    guestName: props.reservation?.guestName || '',
    guestPhone: props.reservation?.phone || '',
    channelId: props.reservation?.channelId || channelId,
    checkInDate: props.reservation?.checkInDate || baseDate,
    checkOutDate: props.reservation?.checkOutDate || addDays(baseDate, 1),
    adults: props.reservation?.adults || 1,
    children: props.reservation?.children || 0,
    totalAmount: Number(props.reservation?.totalAmount ?? 0),
    notes: props.reservation?.notes || '',
  }
}

function resetForm() {
  form.value = buildDefaultForm()
  void loadRoomType()
}

function handleDismiss() {
  emit('dismiss')
}

function handleSubmit() {
  emit('submit', {
    guestName: form.value.guestName.trim(),
    guestPhone: form.value.guestPhone.trim(),
    channelId: Number(form.value.channelId),
    checkInDate: form.value.checkInDate,
    checkOutDate: form.value.checkOutDate,
    adults: Number(form.value.adults || 1),
    children: Number(form.value.children || 0),
    totalAmount: Number(form.value.totalAmount || 0),
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

watch(
  () => [form.value.checkInDate, form.value.checkOutDate],
  () => {
    if (props.isOpen) {
      updatePrice()
    }
  },
)
</script>

<style scoped>
.booking-modal__content {
  --padding-bottom: 24px;
}

.booking-modal__hero {
  margin: 16px;
  padding: 16px;
  border-radius: 20px;
  background: var(--app-primary-soft);
}

.booking-modal__hero p,
.booking-modal__note p {
  margin: 6px 0 0;
  color: var(--app-muted);
  font-size: 13px;
}

.booking-modal__note {
  margin: 0 16px;
}

.booking-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
}
</style>
