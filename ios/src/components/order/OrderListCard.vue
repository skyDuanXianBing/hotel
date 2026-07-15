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
      <p class="order-card__badge-row">
        <span
          class="order-card__meta-badge order-card__meta-badge--channel"
          :style="channelBadgeStyle"
        >
          {{ channelName }}
        </span>
        <span class="order-card__meta-badge" :class="`is-${assignStatusColor}`">
          {{ assignStatusText }}
        </span>
        <span class="order-card__meta-badge" :class="`is-${settlementColor}`">
          {{ settlementText }}
        </span>
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
  getAssignStatusColor,
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
  channelColor?: string
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
const assignStatusColor = computed(() => {
  const color = getAssignStatusColor(props.reservation)
  return color === 'warning' ? 'danger' : color
})
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

const channelBadgeStyle = computed(() => ({
  '--order-card-channel-color': resolveChannelBadgeColor(channelName.value, props.channelColor),
}))

function resolveChannelBadgeColor(channel: string, configuredColor?: string) {
  const trimmedColor = (configuredColor || '').trim()
  if (/^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/i.test(trimmedColor)) {
    return trimmedColor
  }

  const normalizedChannel = channel.trim().toLowerCase().replace(/[^a-z0-9]/g, '')
  if (normalizedChannel.includes('airbnb')) {
    return '#ff5a6d'
  }
  if (normalizedChannel.includes('tripcom') || normalizedChannel.includes('ctrip')) {
    return '#2f5dff'
  }
  if (normalizedChannel.includes('agoda')) {
    return '#ca28d9'
  }
  if (normalizedChannel.includes('booking')) {
    return '#003b95'
  }
  return '#3474f6'
}
</script>

<style scoped>
.order-card {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid rgba(97, 124, 177, 0.06);
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(77, 98, 145, 0.08);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease,
    background 0.18s ease;
}

.order-card:active {
  border-color: rgba(var(--ion-color-primary-rgb), 0.14);
  background: #fbfdff;
  box-shadow: 0 6px 16px rgba(77, 98, 145, 0.08);
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
  letter-spacing: 0;
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
  min-height: 22px;
  padding: 0 8px;
  border-radius: 7px;
  background: #e9f5ff;
  color: #3474f6;
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
  white-space: nowrap;
}

.order-card__status.is-success {
  background: #e9fff7;
  color: #15a779;
}

.order-card__status.is-warning {
  background: #fff7e8;
  color: #d18416;
}

.order-card__status.is-danger {
  background: #fff0f2;
  color: #e25161;
}

.order-card__status.is-medium {
  background: #eef2f7;
  color: #74829a;
}

.order-card__more-btn {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #a5adba;
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
  padding: 9px 12px 10px;
  border: 0;
  border-radius: 8px;
  background: #eef6ff;
}

.order-card__stay-label {
  color: var(--ios-pms-primary-strong);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1;
}

.order-card__stay {
  color: var(--ios-pms-text-primary);
  font-size: 16px;
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
  font-size: 14px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.45;
  word-break: break-word;
}

.order-card__amount {
  color: var(--ios-pms-text-primary);
  font-size: 22px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1;
  letter-spacing: 0;
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
  align-items: center;
  gap: 10px;
  padding-top: 0;
}

.order-card__badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
}

.order-card__meta-badge {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 16px;
  max-width: 88px;
  padding: 0 6px;
  border-radius: 3px;
  background: #eef2f7;
  color: #74829a;
  font-size: 9px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-card__meta-badge--channel {
  max-width: 96px;
  background: var(--order-card-channel-color, #3474f6);
  color: #ffffff;
}

.order-card__meta-badge.is-success {
  background: #e9fff7;
  color: #15a779;
}

.order-card__meta-badge.is-warning {
  background: #fff7e8;
  color: #d18416;
}

.order-card__meta-badge.is-danger {
  background: #fff0f2;
  color: #e25161;
}

.order-card__meta-badge.is-medium {
  background: #eef2f7;
  color: #74829a;
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
  color: #a6afbd;
  font-size: 11px;
  line-height: 1.3;
  white-space: nowrap;
}
</style>
