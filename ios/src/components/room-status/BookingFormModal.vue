<template>
  <ion-modal :is-open="isOpen" @didDismiss="handleDismiss">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ modalTitle }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleDismiss">{{ $t('home.section.close') }}</ion-button>
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
                <span>{{ $t('roomStatus.booking.orderAmount') }}</span>
                <strong>{{ priceText }}</strong>
              </div>
            </div>
            <strong>{{ roomLabel }}</strong>
            <p>{{ bookingSummaryText }}</p>

            <div class="booking-modal__meta-grid">
              <div class="booking-modal__meta-item">
                <span>{{ $t('roomStatus.action.checkIn') }}</span>
                <strong>{{ form.checkInDate || '--' }}</strong>
              </div>
              <div class="booking-modal__meta-item">
                <span>{{ $t('roomStatus.hoverCard.checkOutDate') }}</span>
                <strong>{{ form.checkOutDate || '--' }}</strong>
              </div>
              <div class="booking-modal__meta-item">
                <span>{{ $t('stage5VisibleText.136') }}</span>
                <strong>{{ stayNightsText }}</strong>
              </div>
            </div>
          </section>

          <section class="booking-modal__section">
            <div class="booking-modal__section-head">
              <div>
                <h2>{{ $t('stage5VisibleText.152') }}</h2>
              </div>
            </div>

            <div class="booking-modal__field-grid">
              <label class="booking-field">
                <span>{{ $t('roomStatus.booking.guestName') }}</span>
                <ion-input v-model="form.guestName" fill="outline" :placeholder="$t('roomStatus.booking.messages.guestNameRequired')" />
              </label>
              <label class="booking-field">
                <span>{{ $t('roomStatus.booking.guestPhone') }}</span>
                <ion-input v-model="form.guestPhone" fill="outline" :placeholder="$t('roomStatus.booking.guestPhonePlaceholder')" />
              </label>
              <label class="booking-field booking-field--full">
                <span>{{ $t('home.quick.channels.0') }}</span>
                <ion-select v-model="form.channelId" fill="outline" interface="action-sheet" :placeholder="$t('roomStatus.booking.channelPlaceholder')">
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
                <h2>{{ $t('stage5VisibleText.132') }}</h2>
              </div>
            </div>

            <div class="booking-modal__field-grid">
              <label class="booking-field">
                <span>{{ $t('roomStatus.common.checkInDate') }}</span>
                <ion-input v-model="form.checkInDate" fill="outline" type="date" />
              </label>
              <label class="booking-field">
                <span>{{ $t('roomStatus.common.checkOutDate') }}</span>
                <ion-input v-model="form.checkOutDate" fill="outline" type="date" />
              </label>
              <label class="booking-field">
                <span>{{ $t('stage5VisibleText.172') }}</span>
                <ion-input v-model.number="form.adults" fill="outline" type="number" min="1" />
              </label>
              <label class="booking-field">
                <span>{{ $t('stage5VisibleText.128') }}</span>
                <ion-input v-model.number="form.children" fill="outline" type="number" min="0" />
              </label>
            </div>
          </section>

          <section class="booking-modal__section">
            <div class="booking-modal__section-head">
              <div>
                <h2>{{ $t('stage5VisibleText.231') }}</h2>
              </div>
            </div>

            <div class="booking-modal__field-grid">
              <label class="booking-field booking-field--full">
                <span>{{ $t('roomStatus.booking.orderAmount') }}</span>
                <div class="booking-field__amount">
                  <span class="booking-field__currency">{{ currencyPrefix }}</span>
                  <ion-input v-model.number="form.totalAmount" fill="outline" type="number" min="0" />
                </div>
                <small class="booking-field__assist">{{ priceLoading ? $t('stage5DynamicUi.54') : $t('stage5DynamicUi.27') }}</small>
              </label>
              <label class="booking-field booking-field--full">
                <span>{{ $t('accommodation.common.remarks') }}</span>
                <ion-textarea
                  v-model="form.notes"
                  auto-grow
                  :rows="4"
                  fill="outline"
                  :placeholder="$t('stage5UiAttributes.35')"
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
            <span>{{ $t('stage5VisibleText.169') }}</span>
            <strong>{{ priceText }}</strong>
          </div>
          <div class="booking-modal__footer-actions">
            <ion-button fill="outline" @click="handleDismiss">{{ $t('accommodation.common.cancel') }}</ion-button>
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
import { useI18n } from 'vue-i18n'
import { calculateTotalPriceByDates, getRoomTypeByRoomId, type RoomTypeDTO } from '@/api/roomType'
import type { ChannelDTO, ReservationDTO } from '@/api/reservation'
import { addDays, type RoomStatusRoomItem } from '@/stores/roomStatus'
import { useStoreStore } from '@/stores/store'
import { formatMoney } from '@/utils/formatters'
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

const { t } = useI18n()
const storeStore = useStoreStore()
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
const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))

const modalTitle = computed(() => {
  if (props.mode === 'check-in') {
    return t('roomStatus.bookingModal.title.checkIn')
  }
  if (props.mode === 'edit') {
    return t('roomStatus.bookingModal.title.edit')
  }
  return t('roomStatus.bookingModal.title.create')
})

const submitText = computed(() => {
  if (props.mode === 'check-in') {
    return t('roomStatus.bookingModal.submit.checkIn')
  }
  if (props.mode === 'edit') {
    return t('roomStatus.bookingModal.submit.edit')
  }
  return t('roomStatus.bookingModal.submit.create')
})

const roomLabel = computed(() => {
  if (props.room) {
    return t('roomStatus.bookingModal.roomLabel', {
      roomType: props.room.roomType,
      roomNumber: props.room.roomNumber,
    })
  }
  if (props.reservation?.roomTypeName || props.reservation?.roomNumber) {
    return t('roomStatus.bookingModal.roomLabel', {
      roomType: props.reservation.roomTypeName || t('roomStatus.bookingModal.roomFallback'),
      roomNumber: props.reservation.roomNumber || t('roomStatus.bookingModal.unassignedRoom'),
    })
  }
  return t('roomStatus.bookingModal.noRoomSelected')
})

const priceText = computed(() => {
  return priceLoading.value
    ? t('roomStatus.bookingModal.calculating')
      : formatMoney(
          Number(form.value.totalAmount || 0),
          currentCurrency.value,
          { minimumFractionDigits: 2, maximumFractionDigits: 2 },
          currentMoneyContext.value,
        )
})

const currencyPrefix = computed(() =>
  formatMoney(
    0,
    currentCurrency.value,
    { maximumFractionDigits: 0 },
    currentMoneyContext.value,
  ).replace(/[\d\s.,，。]/g, ''),
)

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
    return t('roomStatus.bookingModal.nights', { count: 0 })
  }

  return t('roomStatus.bookingModal.nights', { count: diffDays })
})

const bookingSummaryText = computed(() => {
  if (!form.value.checkInDate || !form.value.checkOutDate) {
    return t('roomStatus.bookingModal.incompleteDates')
  }

  return t('roomStatus.bookingModal.summary', {
    checkInDate: form.value.checkInDate,
    checkOutDate: form.value.checkOutDate,
    nights: stayNightsText.value,
  })
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
  min-width: 0;
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
  overflow-wrap: anywhere;
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.3;
}

.booking-modal__hero p,
.booking-modal__section-head p {
  overflow-wrap: anywhere;
  margin: var(--ios-pms-space-2) 0 0;
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.booking-modal__amount-chip strong,
.booking-modal__footer-summary strong {
  overflow-wrap: anywhere;
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
  min-width: 0;
  overflow-wrap: anywhere;
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
  line-height: 1;
  pointer-events: none;
  white-space: nowrap;
}

.booking-field__amount ion-input {
  --padding-start: 48px;
}

.booking-field__assist {
  margin-top: -2px;
}

.booking-modal__footer {
  align-items: center;
  padding: 0 16px;
}

.booking-modal__footer-actions {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.booking-modal__footer-actions ion-button {
  min-width: 0;
  white-space: normal;
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
