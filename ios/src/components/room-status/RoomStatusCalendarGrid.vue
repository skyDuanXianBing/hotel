<template>
  <section class="room-calendar" :class="{ 'is-loading': loading }">
    <div class="room-calendar__scroll">
      <div class="room-calendar__inner" :style="{ minWidth: `${totalMinWidth}px` }">
        <div class="room-calendar__row room-calendar__row--header" :style="gridStyle">
          <button
            class="room-calendar__corner"
            :class="{ 'is-pulsing': !todayInWindow }"
            type="button"
            aria-label="回到今日"
            @click="$emit('go-today')"
          >
            <ion-icon :icon="calendarClearOutline" class="room-calendar__corner-icon" />
            <strong>{{ todayLabel }}</strong>
          </button>

          <button
            v-for="day in days"
            :key="day.date"
            class="room-calendar__day"
            :class="{
              'is-selected': day.isSelected,
              'is-today': day.isToday,
              'is-weekend': day.weekday === '六' || day.weekday === '日',
            }"
            type="button"
            @click="$emit('select-date', day.date)"
          >
            <span class="room-calendar__day-weekday">周{{ day.weekday }}</span>
            <strong class="room-calendar__day-label">{{ day.label }}</strong>
          </button>
        </div>

        <template v-for="group in groups" :key="group.roomType">
          <div class="room-calendar__group-label">
            <span class="room-calendar__group-label-text">| {{ group.roomType }}</span>
          </div>

          <div
            v-for="room in group.rooms"
            :key="room.roomId"
            class="room-calendar__row"
            :style="gridStyle"
          >
            <button
              class="room-calendar__room-cell"
              :class="getRoomCellClass(room)"
              type="button"
              :aria-label="getRoomCellAriaLabel(room)"
              @click="$emit('open-room-actions', { roomId: room.roomId, date: room.focusedDate })"
            >
              <strong class="room-calendar__room-number">{{ room.roomNumber }}</strong>
              <div v-if="room.focusedClosed || room.isDirty" class="room-calendar__room-flags">
                <span
                  v-if="room.focusedClosed"
                  class="room-calendar__room-close-badge"
                  :class="getRoomCloseBadgeClass(room)"
                >
                  {{ getRoomCloseBadgeText(room) }}
                </span>
                <ion-icon
                  v-if="room.isDirty"
                  :icon="notificationsOutline"
                  class="room-calendar__room-flag"
                  aria-label="脏房"
                />
              </div>
            </button>

            <template
              v-for="cell in buildRowCells(room.timeline)"
              :key="`${room.roomId}-${cell.startIndex}`"
            >
              <button
                v-if="cell.kind === 'reservation'"
                class="room-calendar__reservation"
                :class="{
                  'is-truncated-start': cell.truncatedStart,
                  'is-truncated-end': cell.truncatedEnd,
                }"
                :data-channel="resolveChannelAccent(cell.reservation.channelName)"
                :style="{ gridColumn: `span ${cell.span}` }"
                type="button"
                @click="$emit('select-reservation', cell.reservation.id)"
              >
                <strong class="room-calendar__reservation-guest">
                  {{ cell.reservation.guestName || '未命名客人' }}
                </strong>
                <span class="room-calendar__reservation-channel">
                  {{ cell.reservation.channelName || '自来客' }}
                </span>
              </button>

              <button
                v-else
                class="room-calendar__cell"
                :class="getEmptyCellClass(cell.item)"
                type="button"
                @click="$emit('open-room-actions', { roomId: room.roomId, date: cell.item.date })"
              >
                <span class="room-calendar__cell-content">
                  {{ getEmptyCellLabel(cell.item) }}
                </span>
              </button>
            </template>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonIcon } from '@ionic/vue'
import { calendarClearOutline, notificationsOutline } from 'ionicons/icons'
import { computed } from 'vue'
import type {
  RoomStatusDateItem,
  RoomStatusRoomItem,
  RoomTimelineItem,
} from '@/stores/roomStatus'
import type { ReservationDTO } from '@/api/reservation'

interface RoomStatusRoomGroup {
  roomType: string
  rooms: RoomStatusRoomItem[]
}

type RowCell =
  | {
      kind: 'empty'
      startIndex: number
      span: 1
      item: RoomTimelineItem
    }
  | {
      kind: 'reservation'
      startIndex: number
      span: number
      item: RoomTimelineItem
      reservation: ReservationDTO
      truncatedStart: boolean
      truncatedEnd: boolean
    }

const ROOM_COLUMN_WIDTH = 72
const DAY_MIN_WIDTH = 108

const props = defineProps<{
  days: RoomStatusDateItem[]
  groups: RoomStatusRoomGroup[]
  loading: boolean
}>()

defineEmits<{
  'select-date': [date: string]
  'select-reservation': [reservationId: number]
  'open-room-actions': [payload: { roomId: number; date: string }]
  'go-today': []
}>()

const gridStyle = computed(() => ({
  gridTemplateColumns: `${ROOM_COLUMN_WIDTH}px repeat(${props.days.length}, minmax(${DAY_MIN_WIDTH}px, 1fr))`,
}))

const totalMinWidth = computed(() => ROOM_COLUMN_WIDTH + DAY_MIN_WIDTH * props.days.length)

const todayDate = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
})

const todayLabel = computed(() => {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${month}月${day}日`
})

const todayInWindow = computed(() => props.days.some((item) => item.date === todayDate.value))

function buildRowCells(timeline: RoomTimelineItem[]): RowCell[] {
  const cells: RowCell[] = []
  let index = 0

  while (index < timeline.length) {
    const current = timeline[index]

    if (current.reservation) {
      const reservationId = current.reservation.id
      let span = 1

      while (
        index + span < timeline.length &&
        timeline[index + span].reservation?.id === reservationId
      ) {
        span += 1
      }

      const reservation = current.reservation
      const lastItem = timeline[index + span - 1]
      const truncatedStart = Boolean(
        reservation.checkInDate && current.date > reservation.checkInDate,
      )
      const truncatedEnd = Boolean(
        reservation.checkOutDate && lastItem.date < shiftDate(reservation.checkOutDate, -1),
      )

      cells.push({
        kind: 'reservation',
        startIndex: index,
        span,
        item: current,
        reservation,
        truncatedStart,
        truncatedEnd,
      })

      index += span
      continue
    }

    cells.push({
      kind: 'empty',
      startIndex: index,
      span: 1,
      item: current,
    })

    index += 1
  }

  return cells
}

function shiftDate(date: string, amount: number) {
  const next = new Date(date)
  next.setDate(next.getDate() + amount)
  const year = next.getFullYear()
  const month = String(next.getMonth() + 1).padStart(2, '0')
  const day = String(next.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function resolveChannelAccent(channelName?: string) {
  if (!channelName) {
    return 'default'
  }

  const normalized = channelName.toLowerCase()
  if (normalized.includes('airbnb')) {
    return 'airbnb'
  }
  if (normalized.includes('booking')) {
    return 'booking'
  }
  if (normalized.includes('agoda')) {
    return 'agoda'
  }
  if (normalized.includes('trip')) {
    return 'trip'
  }
  return 'default'
}

function getEmptyCellClass(item: RoomTimelineItem) {
  return {
    'is-today': item.isToday,
    'is-selected': item.isSelected,
    'is-available': item.businessState === 'available',
    'is-closed':
      item.businessState === 'closed' ||
      item.businessState === 'out_of_order' ||
      item.businessState === 'maintenance' ||
      item.businessState === 'retain',
    'is-dirty': item.isDirty && item.businessState === 'available',
  }
}

function formatPrice(price?: number) {
  if (price === undefined || price === null || !Number.isFinite(price)) {
    return '—'
  }
  return `¥${Math.round(price)}`
}

function getEmptyCellLabel(item: RoomTimelineItem) {
  if (item.businessState === 'available') {
    return formatPrice(item.price)
  }
  if (item.businessState === 'maintenance') {
    return '维修'
  }
  if (item.businessState === 'retain') {
    return '保留'
  }
  if (item.businessState === 'closed' || item.businessState === 'out_of_order') {
    return '关房'
  }
  return item.statusText
}

function getRoomCloseBadgeText(room: RoomStatusRoomItem) {
  if (room.closeType === 'maintenance') {
    return '维修'
  }

  if (room.closeType === 'retain') {
    return '保留'
  }

  return '关房'
}

function getRoomCloseBadgeClass(room: RoomStatusRoomItem) {
  return {
    'is-maintenance': room.closeType === 'maintenance',
    'is-retain': room.closeType === 'retain',
    'is-closed': room.closeType !== 'maintenance' && room.closeType !== 'retain',
  }
}

function getRoomCellClass(room: RoomStatusRoomItem) {
  return {
    'is-focused-closed': room.focusedClosed,
    'is-focused-maintenance': room.focusedClosed && room.closeType === 'maintenance',
    'is-focused-retain': room.focusedClosed && room.closeType === 'retain',
  }
}

function getRoomCellAriaLabel(room: RoomStatusRoomItem) {
  const labels = [`房间 ${room.roomNumber}`]

  if (room.focusedClosed) {
    labels.push(getRoomCloseBadgeText(room))
  }

  if (room.isDirty) {
    labels.push('脏房')
  }

  return labels.join('，')
}
</script>

<style scoped>
.room-calendar {
  position: relative;
  background: var(--app-surface, #ffffff);
}

.room-calendar.is-loading {
  opacity: 0.92;
}

.room-calendar__scroll {
  overflow-x: auto;
  overflow-y: visible;
  overscroll-behavior-x: contain;
  -webkit-overflow-scrolling: touch;
}

.room-calendar__inner {
  display: flex;
  flex-direction: column;
}

.room-calendar__row {
  display: grid;
  align-items: stretch;
  border-bottom: 1px solid var(--app-border, rgba(15, 23, 42, 0.08));
}

.room-calendar__row--header {
  position: sticky;
  top: 0;
  z-index: 6;
  background: var(--app-surface, #ffffff);
  border-bottom: 1px solid var(--app-border, rgba(15, 23, 42, 0.1));
}

.room-calendar__corner {
  position: sticky;
  left: 0;
  z-index: 7;
  background: var(--app-surface, #ffffff);
  border: none;
  border-right: 1px solid var(--app-border, rgba(15, 23, 42, 0.08));
  padding: 8px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--ion-color-primary);
  min-height: 58px;
}

.room-calendar__corner.is-pulsing::before {
  content: '';
  position: absolute;
  top: 6px;
  left: 50%;
  transform: translateX(-50%);
  width: 18px;
  height: 4px;
  border-radius: 2px;
  background: var(--ion-color-primary);
}

.room-calendar__corner-icon {
  font-size: 16px;
}

.room-calendar__corner strong {
  font-size: 13px;
  font-weight: 700;
  color: var(--app-heading, #1e293b);
  letter-spacing: -0.2px;
}

.room-calendar__day {
  appearance: none;
  border: none;
  border-right: 1px solid var(--app-border, rgba(15, 23, 42, 0.06));
  background: transparent;
  color: var(--app-muted, #64748b);
  padding: 10px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-height: 58px;
}

.room-calendar__day-weekday {
  font-size: 12px;
  color: var(--app-muted, #64748b);
  line-height: 1.1;
}

.room-calendar__day-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-heading, #1e293b);
  line-height: 1.1;
}

.room-calendar__day.is-weekend .room-calendar__day-label,
.room-calendar__day.is-weekend .room-calendar__day-weekday {
  color: #ef4444;
}

.room-calendar__day.is-today .room-calendar__day-label,
.room-calendar__day.is-today .room-calendar__day-weekday {
  color: var(--ion-color-primary);
}

.room-calendar__day.is-selected {
  background: rgba(59, 130, 246, 0.08);
}

.room-calendar__group-label {
  display: flex;
  align-items: center;
  padding: 10px 0 6px;
  min-height: 28px;
}

.room-calendar__group-label-text {
  position: sticky;
  left: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ion-color-primary);
  letter-spacing: 0.2px;
}

.room-calendar__room-cell {
  position: sticky;
  left: 0;
  z-index: 3;
  background: #f5f5f5;
  border: none;
  border-right: 1px solid var(--app-border, rgba(15, 23, 42, 0.08));
  padding: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 72px;
  color: var(--app-heading, #1e293b);
}

.room-calendar__room-cell.is-focused-closed {
  background: #f8fafc;
}

.room-calendar__room-cell.is-focused-maintenance {
  background: rgba(245, 158, 11, 0.12);
}

.room-calendar__room-cell.is-focused-retain {
  background: rgba(139, 92, 246, 0.12);
}

.room-calendar__room-number {
  font-size: 15px;
  font-weight: 600;
  line-height: 1;
}

.room-calendar__room-flags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.room-calendar__room-close-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
}

.room-calendar__room-close-badge.is-maintenance {
  background: rgba(245, 158, 11, 0.18);
  color: #b45309;
}

.room-calendar__room-close-badge.is-retain {
  background: rgba(139, 92, 246, 0.16);
  color: #7c3aed;
}

.room-calendar__room-flag {
  font-size: 14px;
  color: var(--app-muted, #94a3b8);
}

.room-calendar__cell,
.room-calendar__reservation {
  appearance: none;
  border: none;
  border-right: 1px solid var(--app-border, rgba(15, 23, 42, 0.06));
  background: #ffffff;
  color: var(--app-heading, #1e293b);
  min-height: 72px;
  padding: 6px 8px;
  display: flex;
  align-items: center;
  text-align: left;
}

.room-calendar__cell {
  justify-content: center;
  color: var(--app-muted, #94a3b8);
  font-size: 13px;
  font-weight: 500;
}

.room-calendar__cell.is-today {
  box-shadow: inset 0 2px 0 var(--ion-color-primary);
}

.room-calendar__cell.is-selected {
  background: rgba(59, 130, 246, 0.05);
}

.room-calendar__cell.is-closed {
  background: #f1f5f9;
  color: #94a3b8;
}

.room-calendar__cell.is-dirty {
  background: repeating-linear-gradient(
    135deg,
    #ffffff,
    #ffffff 6px,
    #fef3c7 6px,
    #fef3c7 12px
  );
}

.room-calendar__reservation {
  position: relative;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 2px;
  padding: 6px 10px 6px 14px;
  background: #ffe8d4;
  color: #1e293b;
  overflow: hidden;
  border-radius: 0;
}

.room-calendar__reservation::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--ion-color-primary);
}

.room-calendar__reservation[data-channel='airbnb']::before {
  background: #ff5a5f;
}

.room-calendar__reservation[data-channel='booking']::before {
  background: #003580;
}

.room-calendar__reservation[data-channel='agoda']::before {
  background: #d4001a;
}

.room-calendar__reservation[data-channel='trip']::before {
  background: #2681ff;
}

.room-calendar__reservation.is-truncated-start::before {
  left: -4px;
}

.room-calendar__reservation.is-truncated-end {
  background: linear-gradient(to right, #ffe8d4 90%, rgba(255, 232, 212, 0.4));
}

.room-calendar__reservation-guest {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.25;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-calendar__reservation-channel {
  font-size: 11px;
  color: #64748b;
  line-height: 1.25;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
