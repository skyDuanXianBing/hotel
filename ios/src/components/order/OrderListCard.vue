<template>
  <article class="order-card" @click="$emit('openDetail')">
    <div class="order-card__header">
      <div class="order-card__identity">
        <h3 class="order-card__guest">{{ guestName }}</h3>
        <p v-if="shouldShowCheckinType" class="order-card__guest-note">{{ checkinTypeText }}</p>
      </div>

      <div class="order-card__header-side">
        <span class="order-card__status" :class="`is-${statusColor}`">{{ statusText }}</span>
        <button
          type="button"
          class="order-card__more-btn"
          aria-label="更多订单操作"
          @click.stop="$emit('openActions')"
        >
          <ion-icon :icon="ellipsisHorizontal" />
        </button>
      </div>
    </div>

    <div class="order-card__stay-band">
      <span class="order-card__stay-label">入住时段</span>
      <p class="order-card__stay">{{ stayText }}</p>
    </div>

    <div class="order-card__main">
      <div class="order-card__room-block">
        <p class="order-card__room">{{ roomText }}</p>
      </div>
    </div>

    <div v-if="showUnassignedExceptionBlock" class="order-card__alert order-card__alert--warning">
      <strong>映射异常</strong>
      <span v-if="reservation.otaRoomId">渠道房型 ID：{{ reservation.otaRoomId }}</span>
      <span v-else>请尽快核对渠道房型与本地房型的映射关系。</span>
    </div>

    <div v-if="showDeletedRoomExceptionBlock" class="order-card__alert order-card__alert--danger">
      {{ deletedRoomReasonText }}
    </div>

    <div class="order-card__meta-strip">
      <p class="order-card__meta-line">
        <span class="order-card__meta-item">{{ channelName }}</span>
        <span class="order-card__meta-item">{{ assignStatusText }}</span>
        <span class="order-card__meta-item" :class="`is-${settlementColor}`">{{ settlementText }}</span>
      </p>
      <p class="order-card__timestamp">{{ metaTimestamp }}</p>
    </div>

    <p v-if="supportingLine" class="order-card__support-line">
      <span class="order-card__support-label">{{ supportingLine.label }}</span>
      <span class="order-card__support-value">{{ supportingLine.value }}</span>
    </p>

    <div class="order-card__amount-row">
      <p class="order-card__amount">{{ amountText }}</p>
    </div>
  </article>
</template>

<script setup lang="ts">
import { IonIcon } from '@ionic/vue'
import { ellipsisHorizontal } from 'ionicons/icons'
import { computed } from 'vue'
import type { OrderBoxItem } from '@/api/orderBox'
import type { ReservationDTO } from '@/api/reservation'
import {
  formatAmount,
  formatDateLabel,
  formatDateTime,
  getAssignStatusText,
  getCheckinTypeText,
  getDisplayChannelOrderNumber,
  getReservationStatusColor,
  getReservationStatusText,
  getSettlementStatusColor,
  getSettlementStatusText,
} from '@/components/order/orderUtils'

const props = defineProps<{
  reservation: ReservationDTO
  activeTab: string
  orderBoxItem?: OrderBoxItem | null
}>()

defineEmits<{
  openDetail: []
  assignRoom: []
  openActions: []
}>()

const guestName = computed(() => props.reservation.guestName || '未命名客人')
const channelName = computed(() => props.reservation.channelName || '自来客')
const statusText = computed(() => getReservationStatusText(props.reservation.status))
const statusColor = computed(() => getReservationStatusColor(props.reservation.status))
const settlementText = computed(() => getSettlementStatusText(props.reservation))
const settlementColor = computed(() => getSettlementStatusColor(props.reservation))
const assignStatusText = computed(() => getAssignStatusText(props.reservation))
const channelOrderNumber = computed(() => getDisplayChannelOrderNumber(props.reservation))
const amountText = computed(() => formatAmount(props.reservation.totalAmount ?? props.reservation.currentRoomPrice))
const movedInAtText = computed(() => formatDateTime(props.orderBoxItem?.movedInAt))
const createdAtText = computed(() => formatDateTime(props.reservation.createdAt))
const checkinTypeText = computed(() => getCheckinTypeText(props.reservation.checkinType))
const phoneText = computed(() => props.reservation.phone?.trim() || '')
const notesText = computed(() => props.reservation.notes?.trim() || '')
const roomText = computed(() => {
  const roomTypeName = props.reservation.roomTypeName || '待排房型'
  const roomNumber = props.reservation.roomNumber || '-'
  return `${roomTypeName} / ${roomNumber}`
})
const stayText = computed(() => {
  return `${formatDateLabel(props.reservation.checkInDate)} 至 ${formatDateLabel(props.reservation.checkOutDate)}`
})
const shouldShowCheckinType = computed(() => {
  const normalized = (props.reservation.checkinType || '').toLowerCase()
  return normalized === 'early' || normalized === 'late'
})
const metaTimestamp = computed(() => {
  if (!createdAtText.value || createdAtText.value === '-') {
    return '待同步'
  }
  return createdAtText.value
})
const supportingLine = computed(() => {
  if (showUnassignedExceptionBlock.value || showDeletedRoomExceptionBlock.value) {
    return null
  }

  if (notesText.value) {
    return {
      label: '备注',
      value: notesText.value,
    }
  }

  if (props.orderBoxItem) {
    return {
      label: '移入记录',
      value: `${movedInAtText.value} · ${props.orderBoxItem.movedInBy || '-'}`,
    }
  }

  if (channelOrderNumber.value !== '-') {
    return {
      label: '渠道单号',
      value: channelOrderNumber.value,
    }
  }

  if (phoneText.value) {
    return {
      label: '手机号',
      value: phoneText.value,
    }
  }

  return null
})
const showUnassignedExceptionBlock = computed(() => {
  if (props.activeTab !== 'unassigned') {
    return false
  }
  if (props.reservation.otaRoomId) {
    return true
  }
  if (props.reservation.otaRoomTypeId !== undefined && props.reservation.otaRoomTypeId !== null) {
    return true
  }
  if (props.reservation.reservationNotifId) {
    return true
  }
  return false
})
const deletedRoomReasonText = computed(() => {
  if (props.reservation.roomTypeName || props.reservation.roomNumber) {
    return '关联房型或房间已删除，请核对后重新排房。'
  }
  return '房型或房间已删除，请核对异常订单。'
})
const showDeletedRoomExceptionBlock = computed(() => props.activeTab === 'deleted-rooms')
</script>

<style scoped>
.order-card {
  display: grid;
  gap: var(--ios-pms-space-3);
  padding: 16px;
  border: 1px solid var(--ios-pms-border-soft);
  border-radius: var(--ios-pms-radius-card-sm);
  background: linear-gradient(180deg, var(--ios-pms-surface-strong) 0%, rgba(248, 251, 255, 0.92) 100%);
  box-shadow: var(--ios-pms-shadow-card);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease,
    background 0.18s ease;
}

.order-card:active {
  border-color: rgba(var(--ion-color-primary-rgb), 0.14);
  background: linear-gradient(180deg, #ffffff 0%, rgba(243, 248, 255, 0.96) 100%);
  box-shadow: 0 8px 20px rgba(77, 98, 145, 0.06);
  transform: translateY(1px);
}

.order-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.order-card__identity {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.order-card__header-side {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-card__guest,
.order-card__stay,
.order-card__room,
.order-card__amount,
.order-card__timestamp,
.order-card__support-line {
  margin: 0;
}

.order-card__guest {
  color: var(--ios-pms-text-primary);
  font-size: 18px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.2;
  letter-spacing: -0.02em;
  word-break: break-word;
}

.order-card__guest-note {
  margin: 0;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.4;
}

.order-card__status {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: var(--ios-pms-radius-pill);
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
  font-size: 12px;
  font-weight: var(--ios-pms-weight-bold);
  white-space: nowrap;
}

.order-card__status.is-success {
  background: var(--app-success-soft);
  color: var(--ion-color-success);
}

.order-card__status.is-warning {
  background: var(--app-warning-soft);
  color: var(--ion-color-warning);
}

.order-card__status.is-danger {
  background: var(--app-danger-soft);
  color: var(--ion-color-danger);
}

.order-card__status.is-medium {
  background: rgba(116, 138, 185, 0.1);
  color: var(--ios-pms-text-muted);
}

.order-card__more-btn {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid var(--ios-pms-border-faint);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--ios-pms-text-muted);
  font: inherit;
}

.order-card__more-btn ion-icon {
  font-size: 16px;
}

.order-card__more-btn:active {
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
}

.order-card__stay-band {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
  border-radius: 14px;
  background: var(--ios-pms-primary-soft);
}

.order-card__stay-label {
  color: var(--ios-pms-primary-strong);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1;
}

.order-card__stay {
  color: var(--ios-pms-text-primary);
  font-size: 17px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.35;
}

.order-card__main {
  display: block;
}

.order-card__room-block {
  min-width: 0;
}

.order-card__room {
  color: var(--ios-pms-text-secondary);
  font-size: 16px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.45;
  word-break: break-word;
}

.order-card__amount {
  color: var(--ios-pms-text-primary);
  font-size: 22px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1;
  letter-spacing: -0.03em;
}

.order-card__amount-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-top: 2px;
}

.order-card__alert {
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 14px;
  font-size: 12px;
  line-height: 1.5;
}

.order-card__alert strong {
  display: block;
  margin-bottom: 4px;
  font-weight: var(--ios-pms-weight-bold);
}

.order-card__alert span {
  display: block;
  color: var(--ios-pms-text-secondary);
}

.order-card__alert--warning {
  border-color: rgba(var(--ion-color-warning-rgb), 0.14);
  background: rgba(var(--ion-color-warning-rgb), 0.1);
  color: var(--ion-color-warning);
}

.order-card__alert--danger {
  border-color: rgba(var(--ion-color-danger-rgb), 0.14);
  background: rgba(var(--ion-color-danger-rgb), 0.1);
  color: var(--ion-color-danger);
}

.order-card__meta-strip {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: baseline;
  gap: 10px;
  padding-top: 2px;
}

.order-card__meta-line {
  min-width: 0;
  margin: 0;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.45;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-card__meta-item {
  display: inline;
}

.order-card__meta-item + .order-card__meta-item::before {
  content: '·';
  margin: 0 6px;
  color: var(--ios-pms-text-disabled);
}

.order-card__meta-item.is-success {
  color: var(--ion-color-success);
}

.order-card__meta-item.is-warning {
  color: var(--ion-color-warning);
}

.order-card__meta-item.is-danger {
  color: var(--ion-color-danger);
}

.order-card__meta-item.is-medium {
  color: var(--ios-pms-text-muted);
}

.order-card__support-line {
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.45;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.order-card__support-label {
  color: var(--ios-pms-text-soft);
}

.order-card__support-label::after {
  content: '·';
  margin: 0 6px;
  color: var(--ios-pms-text-disabled);
}

.order-card__support-value {
  color: var(--ios-pms-text-secondary);
}

.order-card__timestamp {
  color: var(--ios-pms-text-soft);
  font-size: 11px;
  line-height: 1.3;
  white-space: nowrap;
}
</style>
