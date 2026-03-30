<template>
  <article class="room-card" @click="$emit('select-room', room.roomId)">
    <div class="room-card__header">
      <div>
        <p class="room-card__eyebrow">{{ room.roomType }}</p>
        <h3>{{ room.roomNumber }}</h3>
      </div>
      <ion-button fill="clear" size="small" @click.stop="$emit('quick-action', room.roomId)">操作</ion-button>
    </div>

    <div class="room-card__chips">
      <ion-chip :color="focusedStatusColor">{{ room.focusedStatusText }}</ion-chip>
      <ion-chip v-if="room.isDirty" color="warning">脏房</ion-chip>
      <ion-chip v-if="room.focusedClosed && room.closeType">{{ closeTypeText }}</ion-chip>
    </div>

    <ReservationSummaryCard
      v-if="room.reservation"
      :reservation="room.reservation"
      :fallback-room-number="room.roomNumber"
      :fallback-room-type="room.roomType"
      @select="$emit('select-reservation', room.reservation.id)"
    />
    <div v-else class="room-card__empty">
      <strong>{{ room.focusedDate }}</strong>
      <p>当前房间暂无订单，可直接发起预订、直接入住或关房。</p>
    </div>

    <div class="room-card__timeline">
      <button
        v-for="item in room.timeline"
        :key="item.date"
        class="timeline-pill"
        :class="{
          'timeline-pill--selected': item.isSelected,
          'timeline-pill--closed': item.businessState === 'closed',
          'timeline-pill--occupied': item.businessState === 'occupied',
          'timeline-pill--reserved': item.businessState === 'reserved',
          'timeline-pill--available': item.businessState === 'available',
        }"
        type="button"
        @click.stop="$emit('select-date', item.date)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.statusText }}</strong>
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { IonButton, IonChip } from '@ionic/vue'
import { computed } from 'vue'
import ReservationSummaryCard from '@/components/room-status/ReservationSummaryCard.vue'
import type { RoomStatusRoomItem } from '@/stores/roomStatus'

const props = defineProps<{
  room: RoomStatusRoomItem
}>()

defineEmits<{
  'select-room': [roomId: number]
  'select-reservation': [reservationId: number]
  'quick-action': [roomId: number]
  'select-date': [date: string]
}>()

const closeTypeText = computed(() => {
  if (props.room.closeType === 'maintenance') {
    return '维修'
  }
  if (props.room.closeType === 'retain') {
    return '保留'
  }
  return '停用'
})

const focusedStatusColor = computed(() => {
  if (props.room.focusedBusinessState === 'closed') {
    return 'danger'
  }
  if (props.room.focusedBusinessState === 'occupied') {
    return 'success'
  }
  if (props.room.focusedBusinessState === 'reserved') {
    return 'warning'
  }
  return 'medium'
})
</script>

<style scoped>
.room-card {
  border: 1px solid var(--app-border);
  border-radius: 22px;
  background: var(--app-surface);
  padding: 16px;
  display: grid;
  gap: 12px;
  box-shadow: var(--app-shadow);
}

.room-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.room-card__eyebrow,
.room-card__empty p {
  margin: 0;
  color: var(--app-muted);
  font-size: 12px;
}

.room-card__header h3,
.room-card__empty strong {
  margin: 4px 0 0;
  font-size: 22px;
}

.room-card__chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.room-card__empty {
  padding: 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.room-card__timeline {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.timeline-pill {
  appearance: none;
  border: 1px solid var(--app-border);
  border-radius: 14px;
  background: var(--app-surface-muted);
  color: inherit;
  min-height: 56px;
  padding: 6px 4px;
  display: grid;
  gap: 2px;
  text-align: center;
  overflow: hidden;
}

.timeline-pill span {
  font-size: 11px;
  color: var(--app-muted);
  line-height: 1.2;
}

.timeline-pill strong {
  font-size: 11px;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-pill--selected {
  border-color: var(--app-border-strong);
  background: var(--app-primary-soft-strong);
}

.timeline-pill--closed {
  background: rgba(220, 38, 38, 0.08);
}

.timeline-pill--occupied {
  background: rgba(34, 197, 94, 0.12);
}

.timeline-pill--reserved {
  background: rgba(217, 119, 6, 0.1);
}

.timeline-pill--available {
  background: rgba(59, 130, 246, 0.08);
}
</style>
