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

    <ion-content class="mobile-page booking-modal__content">
      <div class="mobile-stack">
        <section class="mobile-card booking-modal__sheet">
          <section class="booking-modal__hero">
            <div class="booking-modal__hero-topline">
              <span class="booking-modal__eyebrow">{{ modalTitle }}</span>
              <div class="booking-modal__amount-chip">
                <span>订单金额</span>
                <strong>{{ priceText }}</strong>
              </div>
            </div>
            <strong>{{ roomLabel }}</strong>
            <p>{{ bookingSummaryText }}</p>

            <div class="booking-modal__meta-grid">
              <div class="booking-modal__meta-item">
                <span>入住</span>
                <strong>{{ form.checkInDate || '--' }}</strong>
              </div>
              <div class="booking-modal__meta-item">
                <span>离店</span>
                <strong>{{ form.checkOutDate || '--' }}</strong>
              </div>
              <div class="booking-modal__meta-item">
                <span>共住</span>
                <strong>{{ stayNightsText }}</strong>
              </div>
            </div>
          </section>

          <section class="booking-modal__section">
            <div class="booking-modal__section-head">
              <div>
                <h2>客人信息</h2>
              </div>
            </div>

            <div class="booking-modal__field-grid">
              <label class="booking-field">
                <span>姓名</span>
                <ion-input v-model="form.guestName" fill="outline" placeholder="请输入客人姓名" />
              </label>
              <label class="booking-field">
                <span>手机</span>
                <ion-input v-model="form.guestPhone" fill="outline" placeholder="请输入手机号" />
              </label>
              <label class="booking-field booking-field--full">
                <span>渠道</span>
                <ion-select v-model="form.channelId" fill="outline" interface="action-sheet" placeholder="请选择渠道">
                  <ion-select-option v-for="item in channels" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </ion-select-option>
                </ion-select>
              </label>
            </div>
          </section>

          <section class="booking-modal__section">
            <div class="booking-modal__section-head">
              <div>
                <h2>入住安排</h2>
              </div>
            </div>

            <div class="booking-modal__field-grid">
              <label class="booking-field">
                <span>入住日期</span>
                <ion-input v-model="form.checkInDate" fill="outline" type="date" />
              </label>
              <label class="booking-field">
                <span>离店日期</span>
                <ion-input v-model="form.checkOutDate" fill="outline" type="date" />
              </label>
              <label class="booking-field">
                <span>成人</span>
                <ion-input v-model.number="form.adults" fill="outline" type="number" min="1" />
              </label>
              <label class="booking-field">
                <span>儿童</span>
                <ion-input v-model.number="form.children" fill="outline" type="number" min="0" />
              </label>
            </div>
          </section>

          <section class="booking-modal__section">
            <div class="booking-modal__section-head">
              <div>
                <h2>费用与备注</h2>
              </div>
            </div>

            <div class="booking-modal__field-grid">
              <label class="booking-field booking-field--full">
                <span>订单金额</span>
                <div class="booking-field__amount">
                  <span class="booking-field__currency">¥</span>
                  <ion-input v-model.number="form.totalAmount" fill="outline" type="number" min="0" />
                </div>
                <small class="booking-field__assist">{{ priceLoading ? '正在根据日期重新计算金额' : '如有特殊价格，可直接在此覆盖' }}</small>
              </label>
              <label class="booking-field booking-field--full">
                <span>备注</span>
                <ion-textarea
                  v-model="form.notes"
                  auto-grow
                  :rows="4"
                  fill="outline"
                  placeholder="可填写特殊需求、渠道号或入住备注"
                />
              </label>
            </div>
          </section>
        </section>
      </div>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="booking-modal__footer">
          <div class="booking-modal__footer-summary">
            <span>当前金额</span>
            <strong>{{ priceText }}</strong>
          </div>
          <div class="booking-modal__footer-actions">
            <ion-button fill="outline" @click="handleDismiss">取消</ion-button>
            <ion-button :disabled="submitting || priceLoading" @click="handleSubmit">
              {{ submitText }}
            </ion-button>
          </div>
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
import { getStoreTodayDate } from '@/utils/storeBusinessDate'

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

const stayNightsText = computed(() => {
  if (!form.value.checkInDate || !form.value.checkOutDate) {
    return '--'
  }

  const start = new Date(form.value.checkInDate)
  const end = new Date(form.value.checkOutDate)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    return '--'
  }

  const diffDays = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays <= 0) {
    return '0 晚'
  }

  return `${diffDays} 晚`
})

const bookingSummaryText = computed(() => {
  if (!form.value.checkInDate || !form.value.checkOutDate) {
    return '请补全入住与离店时间'
  }

  return `${form.value.checkInDate} 入住 · ${form.value.checkOutDate} 离店 · ${stayNightsText.value}`
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
  const baseDate = props.room?.focusedDate || props.reservation?.checkInDate || getStoreTodayDate()
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
  --background: var(--ios-pms-bg-page-plain);
  --padding-bottom: calc(26px + var(--app-safe-bottom));
}

.booking-modal__sheet {
  overflow: hidden;
  padding: 0;
}

.booking-modal__hero {
  padding: var(--ios-pms-space-5);
  background: linear-gradient(180deg, var(--ios-pms-surface-strong), rgba(246, 249, 255, 0.88));
}

.booking-modal__hero-topline,
.booking-modal__footer,
.booking-modal__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ios-pms-space-3);
}

.booking-modal__eyebrow,
.booking-modal__meta-item span,
.booking-field > span,
.booking-modal__amount-chip span,
.booking-modal__footer-summary span,
.booking-field__assist {
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-body-sm-size);
  line-height: 1.4;
}

.booking-modal__eyebrow {
  color: var(--ios-pms-primary);
  font-weight: var(--ios-pms-weight-bold);
}

.booking-modal__amount-chip,
.booking-modal__footer-summary {
  flex-shrink: 0;
  display: grid;
  gap: 2px;
}

.booking-modal__amount-chip {
  min-width: 104px;
  padding: 10px 12px;
  border: 1px solid var(--ios-pms-border-faint);
  border-radius: var(--ios-pms-radius-card-sm);
  background: var(--ios-pms-primary-soft);
  text-align: right;
}

.booking-modal__hero > strong {
  display: block;
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.3;
}

.booking-modal__hero p,
.booking-modal__section-head p {
  margin: var(--ios-pms-space-2) 0 0;
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.booking-modal__amount-chip strong,
.booking-modal__footer-summary strong {
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.2;
}

.booking-modal__meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--ios-pms-space-3);
  margin-top: var(--ios-pms-space-4);
}

.booking-modal__meta-item {
  min-width: 0;
  display: grid;
  gap: var(--ios-pms-space-1);
  padding-top: var(--ios-pms-space-3);
  border-top: 1px solid var(--ios-pms-divider);
}

.booking-modal__meta-item strong {
  color: var(--ios-pms-text-secondary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.4;
}

.booking-modal__section {
  padding: var(--ios-pms-space-5);
}

.booking-modal__section + .booking-modal__section {
  border-top: 1px solid var(--ios-pms-divider);
}

.booking-modal__section-head h2 {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-heavy);
}

.booking-modal__field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ios-pms-space-4) var(--ios-pms-space-3);
  margin-top: var(--ios-pms-space-4);
}

.booking-field {
  min-width: 0;
  display: grid;
  gap: var(--ios-pms-space-2);
}

.booking-field--full {
  grid-column: 1 / -1;
}

.booking-field__amount {
  position: relative;
}

.booking-field__currency {
  position: absolute;
  top: 50%;
  left: 14px;
  z-index: 1;
  transform: translateY(-50%);
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-bold);
  pointer-events: none;
}

.booking-field__amount ion-input {
  --padding-start: 28px;
}

.booking-field__assist {
  margin-top: -2px;
}

.booking-modal__footer {
  align-items: center;
  padding: 0 16px;
}

.booking-modal__footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 374px) {
  .booking-modal__hero-topline,
  .booking-modal__footer,
  .booking-modal__section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .booking-modal__meta-grid,
  .booking-modal__field-grid {
    grid-template-columns: 1fr;
  }

  .booking-modal__amount-chip,
  .booking-modal__footer-summary {
    text-align: left;
  }

  .booking-modal__footer-actions {
    width: 100%;
  }
}
</style>
