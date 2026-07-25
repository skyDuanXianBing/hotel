<template>
  <button class="reservation-card" type="button" @click.stop="$emit('select')">
    <div class="reservation-card__header">
      <div>
        <strong>{{ reservation.guestName }}</strong>
        <p>{{ reservation.orderNumber }}</p>
      </div>
      <ion-chip color="primary">{{ reservation.channelName || $t('roomStatus.common.defaultChannel') }}</ion-chip>
    </div>

    <div class="reservation-card__body">
      <span>{{ reservation.checkInDate }} {{ $t('roomStatus.action.checkIn') }}</span>
      <span>{{ reservation.checkOutDate }} {{ $t('roomStatus.hoverCard.checkOutDate') }}</span>
      <span>{{ reservation.roomTypeName || fallbackRoomType }} {{ reservation.roomNumber || fallbackRoomNumber }}</span>
    </div>

    <div class="reservation-card__footer">
      <ion-chip :color="statusColor">{{ statusText }}</ion-chip>
      <strong>{{ moneyText }}</strong>
    </div>
  </button>
</template>

<script setup lang="ts">
import { IonChip } from '@ionic/vue'
import { computed } from 'vue'
import type { ReservationDTO } from '@/api/reservation'
import { getReservationStatusText } from '@/stores/roomStatus'
import { useStoreStore } from '@/stores/store'
import { formatMoney } from '@/utils/formatters'

const props = defineProps<{
  reservation: ReservationDTO
  fallbackRoomType?: string
  fallbackRoomNumber?: string
}>()

defineEmits<{
  select: []
}>()

const storeStore = useStoreStore()
const statusText = computed(() => getReservationStatusText(props.reservation.status))
const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))

const statusColor = computed(() => {
  if (props.reservation.status === 'CHECKED_IN' || props.reservation.status === 'checked_in') {
    return 'success'
  }
  if (props.reservation.status === 'CANCELLED' || props.reservation.status === 'cancelled') {
    return 'danger'
  }
  if (props.reservation.status === 'CHECKED_OUT' || props.reservation.status === 'checked_out') {
    return 'medium'
  }
  return 'warning'
})

const moneyText = computed(() => {
  if (props.reservation.totalAmount === null || props.reservation.totalAmount === undefined) {
    return formatMoney(
      0,
      currentCurrency.value,
      { minimumFractionDigits: 2, maximumFractionDigits: 2 },
      currentMoneyContext.value,
    )
  }

  if (typeof props.reservation.totalAmount !== 'number' || !Number.isFinite(props.reservation.totalAmount)) {
    throw new Error('Reservation amount must be a valid number')
  }

  return formatMoney(
    props.reservation.totalAmount,
    currentCurrency.value,
    { minimumFractionDigits: 2, maximumFractionDigits: 2 },
    currentMoneyContext.value,
  )
})
</script>

<style scoped>
.reservation-card {
  appearance: none;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.92);
  color: inherit;
  border-radius: 18px;
  width: 100%;
  padding: 14px;
  text-align: left;
  display: grid;
  gap: 10px;
}

.reservation-card__header,
.reservation-card__footer {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.reservation-card__header p,
.reservation-card__body {
  margin: 0;
  color: var(--app-muted);
  font-size: 12px;
}

.reservation-card__body {
  display: grid;
  gap: 4px;
  min-width: 0;
  overflow-wrap: anywhere;
}
</style>
