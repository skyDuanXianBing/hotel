<template>
  <article class="room-card" @click="$emit('select-room', room.roomId)">
    <div class="room-card__header">
      <div>
        <p class="room-card__eyebrow">{{ room.roomType }}</p>
        <h3>{{ room.roomNumber }}</h3>
      </div>
      <ion-button fill="clear" size="small" @click.stop="$emit('quick-action', room.roomId)">{{ $t('accommodation.common.actions') }}</ion-button>
    </div>

    <div class="room-card__chips">
      <ion-chip :color="focusedStatusColor">{{ room.focusedStatusText }}</ion-chip>
      <ion-chip v-if="room.isDirty" color="warning">{{ $t('accommodation.roomTable.columns.dirtyRooms') }}</ion-chip>
      <ion-chip v-if="room.focusedClosed && room.closeType" :color="closeTypeColor">{{ closeTypeText }}</ion-chip>
    </div>

    <div v-if="room.focusedClosed" class="room-card__closed-info">
      <strong>{{ $t('stage5DynamicUi.96') }}{{ closeTypeText }}</strong>
      <p v-if="room.closeRemark">{{ $t('roomStatus.hoverCard.notes') }}{{ room.closeRemark }}</p>
      <p v-else>{{ $t('stage5VisibleText.163') }}</p>
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
      <p>{{ emptyStateText }}</p>
    </div>

    <div class="room-card__timeline">
      <button
        v-for="item in room.timeline"
        :key="item.date"
        class="timeline-pill"
        :class="{
          'timeline-pill--selected': item.isSelected,
          'timeline-pill--closed': item.businessState === 'closed',
          'timeline-pill--maintenance': item.businessState === 'maintenance',
          'timeline-pill--retain': item.businessState === 'retain',
          'timeline-pill--occupied': item.businessState === 'occupied',
          'timeline-pill--reserved': item.businessState === 'reserved',
          'timeline-pill--available': item.businessState === 'available',
          'timeline-pill--dirty': item.isDirty,
        }"
        type="button"
        @click.stop="$emit('select-date', item.date)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.statusText }}</strong>
        <span v-if="item.isDirty" class="timeline-pill__flag">{{ $t('accommodation.roomTable.columns.dirtyRooms') }}</span>
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { IonButton, IonChip } from '@ionic/vue'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()

const closeTypeText = computed(() => {
  if (props.room.closeType === 'maintenance') {
    return t('roomStatus.store.roomState.maintenanceRoom')
  }
  if (props.room.closeType === 'retain') {
    return t('roomStatus.store.roomState.retainRoom')
  }
  return t('roomStatus.store.roomState.closedRoom')
})

const closeTypeColor = computed(() => {
  if (props.room.closeType === 'maintenance') {
    return 'warning'
  }
  if (props.room.closeType === 'retain') {
    return 'tertiary'
  }
  return 'danger'
})

const focusedStatusColor = computed(() => {
  if (props.room.focusedBusinessState === 'closed') {
    return 'danger'
  }
  if (props.room.focusedBusinessState === 'maintenance') {
    return 'warning'
  }
  if (props.room.focusedBusinessState === 'retain') {
    return 'tertiary'
  }
  if (props.room.focusedBusinessState === 'occupied') {
    return 'success'
  }
  if (props.room.focusedBusinessState === 'reserved') {
    return 'warning'
  }
  if (props.room.isDirty) {
    return 'warning'
  }
  return 'medium'
})

const emptyStateText = computed(() => {
  if (props.room.focusedClosed) {
    return t('roomStatus.roomCard.closedEmpty')
  }

  return t('roomStatus.roomCard.availableEmpty')
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

.room-card__closed-info {
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid rgba(220, 38, 38, 0.12);
  background: rgba(220, 38, 38, 0.06);
  display: grid;
  gap: 6px;
}

.room-card__closed-info strong,
.room-card__closed-info p {
  margin: 0;
}

.room-card__closed-info p {
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.5;
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

.timeline-pill__flag {
  font-size: 10px;
  line-height: 1.2;
  color: var(--ion-color-warning-shade);
}

.timeline-pill--selected {
  border-color: var(--app-border-strong);
  background: var(--app-primary-soft-strong);
}

.timeline-pill--closed {
  background: rgba(220, 38, 38, 0.08);
}

.timeline-pill--maintenance {
  background: rgba(245, 158, 11, 0.12);
}

.timeline-pill--retain {
  background: rgba(168, 85, 247, 0.12);
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

.timeline-pill--dirty {
  border-style: dashed;
}
</style>
