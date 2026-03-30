<template>
  <article class="order-card" @click="$emit('openDetail')">
    <!-- Row 1: Guest + Status -->
    <div class="order-card__top">
      <h3 class="order-card__guest">{{ reservation.guestName || '未命名客人' }}</h3>
      <span class="order-card__status" :class="`is-${statusColor}`">{{ statusText }}</span>
    </div>

    <!-- Row 2: Key info chips -->
    <div class="order-card__tags">
      <span class="order-card__tag">{{ reservation.channelName || '自来客' }}</span>
      <span class="order-card__tag">{{ assignStatusText }}</span>
      <span class="order-card__tag" :class="`is-${settlementColor}`">{{ settlementText }}</span>
    </div>

    <!-- Row 3: Core data in 2-col grid (only essential fields) -->
    <div class="order-card__info">
      <div class="order-card__field">
        <span>房型/房号</span>
        <strong>{{ roomText }}</strong>
      </div>
      <div class="order-card__field">
        <span>金额</span>
        <strong>{{ amountText }}</strong>
      </div>
      <div class="order-card__field order-card__field--wide">
        <span>入住 / 离店</span>
        <strong>{{ stayText }}</strong>
      </div>
    </div>

    <!-- Row 4: Phone / Notes (collapsed) -->
    <p v-if="reservation.phone" class="order-card__meta">📱 {{ reservation.phone }}</p>
    <p v-if="reservation.notes" class="order-card__meta">📝 {{ reservation.notes }}</p>

    <!-- Exception blocks (only for special tabs) -->
    <div v-if="showUnassignedExceptionBlock" class="order-card__alert order-card__alert--warning">
      <strong>映射异常</strong>
      <span v-if="reservation.otaRoomId">渠道房型ID: {{ reservation.otaRoomId }}</span>
    </div>

    <div v-if="showDeletedRoomExceptionBlock" class="order-card__alert order-card__alert--danger">
      {{ deletedRoomReasonText }}
    </div>

    <div v-if="orderBoxItem" class="order-card__boxmeta">
      移入: {{ movedInAtText }} · {{ orderBoxItem.movedInBy || '-' }}
    </div>

    <!-- Actions -->
    <div class="order-card__actions">
      <button type="button" class="order-card__action-btn order-card__action-btn--primary" @click.stop="$emit('openDetail')">
        查看详情
      </button>
      <button
        v-if="showAssignAction"
        type="button"
        class="order-card__action-btn"
        @click.stop="$emit('assignRoom')"
      >
        {{ assignActionText }}
      </button>
      <button type="button" class="order-card__action-btn" @click.stop="$emit('openActions')">
        更多
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { OrderBoxItem } from '@/api/orderBox'
import type { ReservationDTO } from '@/api/reservation'
import {
  canAssignRoom,
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
  showAssignAction: boolean
}>()

defineEmits<{
  openDetail: []
  assignRoom: []
  openActions: []
}>()

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
const roomText = computed(() => {
  const roomTypeName = props.reservation.roomTypeName || '待排房'
  const roomNumber = props.reservation.roomNumber || '-'
  return `${roomTypeName} / ${roomNumber}`
})
const stayText = computed(() => {
  return `${formatDateLabel(props.reservation.checkInDate)} – ${formatDateLabel(props.reservation.checkOutDate)}`
})
const assignActionText = computed(() => {
  if (props.reservation.roomId && canAssignRoom(props.reservation)) {
    return '编辑排房'
  }
  return '排房'
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
  gap: 10px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--app-border);
  background: var(--app-surface-strong);
  transition: background 0.15s ease;
}

.order-card:active {
  background: var(--app-primary-soft);
}

.order-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.order-card__guest {
  margin: 0;
  color: var(--app-heading);
  font-size: 16px;
  font-weight: 700;
}

.order-card__status {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  background: var(--app-primary-soft-strong);
  color: var(--ion-color-primary);
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
  background: rgba(100, 116, 139, 0.12);
  color: #64748b;
}

.order-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.order-card__tag {
  padding: 3px 8px;
  border-radius: 6px;
  background: var(--app-surface-muted);
  color: var(--app-muted);
  font-size: 11px;
  font-weight: 500;
}

.order-card__tag.is-success {
  background: var(--app-success-soft);
  color: var(--ion-color-success);
}

.order-card__tag.is-warning {
  background: var(--app-warning-soft);
  color: var(--ion-color-warning);
}

.order-card__tag.is-danger {
  background: var(--app-danger-soft);
  color: var(--ion-color-danger);
}

.order-card__info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.order-card__field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.order-card__field--wide {
  grid-column: 1 / -1;
}

.order-card__field span {
  color: var(--app-muted);
  font-size: 11px;
}

.order-card__field strong {
  color: var(--app-heading);
  font-size: 14px;
  font-weight: 600;
  word-break: break-word;
}

.order-card__meta {
  margin: 0;
  padding: 0;
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-card__alert {
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.4;
}

.order-card__alert strong {
  display: block;
  margin-bottom: 4px;
}

.order-card__alert span {
  display: block;
  color: var(--app-muted);
}

.order-card__alert--warning {
  background: var(--app-warning-soft);
  color: var(--ion-color-warning);
}

.order-card__alert--danger {
  background: var(--app-danger-soft);
  color: var(--ion-color-danger);
}

.order-card__boxmeta {
  padding: 8px 12px;
  border-radius: 10px;
  background: var(--app-primary-soft);
  color: var(--app-muted);
  font-size: 12px;
}

.order-card__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 2px;
  border-top: 1px solid var(--app-border);
}

.order-card__action-btn {
  padding: 8px 14px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
  background: transparent;
  color: var(--app-heading);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
}

.order-card__action-btn--primary {
  border-color: rgba(var(--ion-color-primary-rgb), 0.2);
  color: var(--ion-color-primary);
}

.order-card__action-btn:active {
  background: var(--app-primary-soft);
}
</style>
