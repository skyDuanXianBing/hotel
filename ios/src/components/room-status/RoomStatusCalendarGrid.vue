<template>
  <section class="room-calendar" :class="{ 'is-loading': loading }">
    <div class="room-calendar__sticky-header">
      <div
        ref="headerScrollContainer"
        class="room-calendar__header-scroll"
        @scroll.passive="handleHeaderScroll"
      >
        <div class="room-calendar__inner" :style="{ minWidth: `${totalMinWidth}px` }">
          <div class="room-calendar__row room-calendar__row--header" :style="gridStyle">
            <button
              class="room-calendar__corner"
              type="button"
              aria-label="选择日期"
              @click="openDatePicker"
            >
              <input
                ref="stickyDatePickerInput"
                class="room-calendar__date-input"
                :value="selectedDateValue"
                type="date"
                @change="handleDateInputChange"
              />
              <strong class="room-calendar__corner-date">{{ selectedDateLabel }}</strong>
              <ion-icon :icon="caretDownOutline" class="room-calendar__corner-caret" />
            </button>

            <button
              v-for="day in days"
              :key="`sticky-${day.date}`"
              class="room-calendar__day"
              :class="{
                'is-selected': day.isSelected,
                'is-today': day.isToday,
                'is-weekend': isWeekend(day),
              }"
              type="button"
              @click="$emit('select-date', day.date)"
            >
              <span class="room-calendar__day-number">{{ getDayDateLabel(day) }}</span>
              <strong class="room-calendar__day-weekday">{{ getDayWeekdayLabel(day) }}</strong>
              <span class="room-calendar__day-capacity">{{ getDayCapacityLabel(day) }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div
      ref="scrollContainer"
      class="room-calendar__scroll"
      @scroll.passive="handleBodyScroll"
    >
      <div class="room-calendar__inner" :style="{ minWidth: `${totalMinWidth}px` }">
        <div class="room-calendar__row room-calendar__row--header" :style="gridStyle">
          <button
            class="room-calendar__corner"
            type="button"
            aria-label="选择日期"
            @click="openDatePicker"
          >
            <input
              ref="datePickerInput"
              class="room-calendar__date-input"
              :value="selectedDateValue"
              type="date"
              @change="handleDateInputChange"
            />
            <strong class="room-calendar__corner-date">{{ selectedDateLabel }}</strong>
            <ion-icon :icon="caretDownOutline" class="room-calendar__corner-caret" />
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
            <span class="room-calendar__day-number">{{ getDayDateLabel(day) }}</span>
            <strong class="room-calendar__day-weekday">周{{ day.weekday }}</strong>
            <span class="room-calendar__day-capacity">余 {{ day.availableRooms }}</span>
          </button>
        </div>

        <template v-for="group in groups" :key="group.roomType">
          <div class="room-calendar__row room-calendar__row--group" :style="gridStyle">
            <div class="room-calendar__group-label">
              <span class="room-calendar__group-mark" aria-hidden="true"></span>
              <span class="room-calendar__group-label-text">{{ group.roomType }}</span>
            </div>

            <div
              v-for="day in days"
              :key="`${group.roomType}-${day.date}`"
              class="room-calendar__group-cell"
              :class="{ 'is-selected': day.isSelected, 'is-today': day.isToday }"
            >
              余 {{ getGroupAvailableCount(group, day.date) }}
            </div>
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
                  'is-selected-span': cell.containsSelected,
                  'is-today-span': cell.containsToday,
                  'is-truncated-start': cell.truncatedStart,
                  'is-truncated-end': cell.truncatedEnd,
                }"
                :data-tone="resolveReservationTone(cell.reservation)"
                :style="{ gridColumn: `span ${cell.span}` }"
                type="button"
                @click="$emit('select-reservation', cell.reservation.id)"
              >
                <span class="room-calendar__reservation-topline">
                  {{ cell.reservation.channelName || '自来客' }}
                </span>
                <strong class="room-calendar__reservation-guest">
                  {{ cell.reservation.guestName || '未命名客人' }}
                </strong>
                <span class="room-calendar__reservation-channel">
                  {{ cell.item.statusText }}
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
import { caretDownOutline, notificationsOutline } from 'ionicons/icons'
import { computed, nextTick, ref, watch } from 'vue'
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
      containsSelected: boolean
      containsToday: boolean
      truncatedStart: boolean
      truncatedEnd: boolean
    }

const ROOM_COLUMN_WIDTH = 86
const DAY_MIN_WIDTH = 58

const props = withDefaults(
  defineProps<{
    days: RoomStatusDateItem[]
    groups: RoomStatusRoomGroup[]
    loading: boolean
    viewportDays?: number
  }>(),
  {
    viewportDays: 5,
  },
)

const emit = defineEmits<{
  'select-date': [date: string]
  'select-reservation': [reservationId: number]
  'open-room-actions': [payload: { roomId: number; date: string }]
  'go-today': []
}>()

const datePickerInput = ref<HTMLInputElement | null>(null)
const stickyDatePickerInput = ref<HTMLInputElement | null>(null)
const headerScrollContainer = ref<HTMLDivElement | null>(null)
const scrollContainer = ref<HTMLDivElement | null>(null)
let syncingScroll = false

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

const selectedDay = computed(() => props.days.find((item) => item.isSelected) || props.days[0] || null)

const selectedDateValue = computed(() => selectedDay.value?.date || todayDate.value)

const selectedDateLabel = computed(() => {
  if (!selectedDay.value) {
    return todayDate.value.slice(5)
  }

  return selectedDay.value.label
})

function alignSelectedDateIntoView() {
  const container = scrollContainer.value
  if (!container) {
    return
  }

  const selectedIndex = props.days.findIndex((item) => item.isSelected)
  if (selectedIndex < 0) {
    container.scrollLeft = 0
    return
  }

  const maxStartIndex = Math.max(props.days.length - props.viewportDays, 0)
  const preferredStartIndex = Math.min(Math.max(selectedIndex - 1, 0), maxStartIndex)
  const targetLeft = preferredStartIndex * DAY_MIN_WIDTH

  syncScrollLeft(targetLeft)
}

watch(
  selectedDateValue,
  async () => {
    await nextTick()
    alignSelectedDateIntoView()
  },
  { immediate: true },
)

function openDatePicker() {
  const input = stickyDatePickerInput.value || datePickerInput.value
  if (!input) {
    return
  }

  const pickerInput = input as HTMLInputElement & { showPicker?: () => void }

  if (typeof pickerInput.showPicker === 'function') {
    pickerInput.showPicker()
    return
  }

  input.click()
}

function syncScrollLeft(left: number) {
  if (headerScrollContainer.value) {
    headerScrollContainer.value.scrollLeft = left
  }

  if (scrollContainer.value) {
    scrollContainer.value.scrollLeft = left
  }
}

function syncScrollContainers(source: HTMLDivElement | null, target: HTMLDivElement | null) {
  if (!source || !target || syncingScroll) {
    return
  }

  if (target.scrollLeft === source.scrollLeft) {
    return
  }

  syncingScroll = true
  target.scrollLeft = source.scrollLeft

  requestAnimationFrame(() => {
    syncingScroll = false
  })
}

function handleHeaderScroll() {
  syncScrollContainers(headerScrollContainer.value, scrollContainer.value)
}

function handleBodyScroll() {
  syncScrollContainers(scrollContainer.value, headerScrollContainer.value)
}

function handleDateInputChange(event: Event) {
  const target = event.target as HTMLInputElement | null
  if (!target?.value) {
    return
  }

  emit('select-date', target.value)
}

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
      const segment = timeline.slice(index, index + span)

      cells.push({
        kind: 'reservation',
        startIndex: index,
        span,
        item: current,
        reservation,
        containsSelected: segment.some((item) => item.isSelected),
        containsToday: segment.some((item) => item.isToday),
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

function getDayDateLabel(day: RoomStatusDateItem) {
  const [, date] = day.label.split('-')
  if (!date) {
    return day.label
  }

  return String(Number(date))
}

function isWeekend(day: RoomStatusDateItem) {
  return day.weekday === '六' || day.weekday === '日'
}

function getDayWeekdayLabel(day: RoomStatusDateItem) {
  return `周${day.weekday}`
}

function getDayCapacityLabel(day: RoomStatusDateItem) {
  return `余 ${day.availableRooms}`
}

function getGroupAvailableCount(group: RoomStatusRoomGroup, date: string) {
  let count = 0

  for (const room of group.rooms) {
    const timelineItem = room.timeline.find((item) => item.date === date)
    if (timelineItem?.businessState === 'available') {
      count += 1
    }
  }

  return count
}

function resolveReservationTone(reservation: ReservationDTO) {
  const channelName = reservation.channelName || ''
  const normalized = channelName.toLowerCase()

  if (normalized.includes('airbnb')) {
    return 'coral'
  }
  if (normalized.includes('booking')) {
    return 'indigo'
  }
  if (normalized.includes('agoda')) {
    return 'violet'
  }
  if (normalized.includes('trip') || channelName.includes('携程')) {
    return 'azure'
  }
  if (channelName.includes('美团')) {
    return 'mint'
  }
  if (channelName.includes('抖音')) {
    return 'slate'
  }
  if (channelName.includes('小猪')) {
    return 'rose'
  }

  const fallbackTones = ['sand', 'mint', 'rose', 'azure', 'slate']
  return fallbackTones[reservation.id % fallbackTones.length]
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
    'is-dirty': room.isDirty,
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
  background: rgba(255, 255, 255, 0.76);
}

.room-calendar.is-loading {
  opacity: 0.94;
}

.room-calendar__sticky-header {
  position: sticky;
  top: 0;
  z-index: 8;
}

.room-calendar__header-scroll,
.room-calendar__scroll {
  overflow-x: auto;
  overscroll-behavior-x: contain;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.room-calendar__header-scroll {
  overflow-y: hidden;
}

.room-calendar__scroll {
  overflow-y: visible;
}

.room-calendar__header-scroll::-webkit-scrollbar,
.room-calendar__scroll::-webkit-scrollbar {
  display: none;
}

.room-calendar__inner {
  display: flex;
  flex-direction: column;
}

.room-calendar__row {
  display: grid;
  align-items: stretch;
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid rgba(112, 138, 187, 0.08);
}

.room-calendar__row--header {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  border-bottom: 1px solid rgba(112, 138, 187, 0.1);
  box-shadow: 0 8px 18px rgba(88, 110, 151, 0.04);
}

.room-calendar__scroll > .room-calendar__inner > .room-calendar__row--header {
  display: none;
}

.room-calendar__row--group {
  min-height: 32px;
  background: rgba(244, 247, 253, 0.9);
}

.room-calendar__corner {
  position: sticky;
  left: 0;
  z-index: 10;
  border: none;
  border-right: 1px solid rgba(112, 138, 187, 0.1);
  min-height: 78px;
  padding: 10px 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background:
    linear-gradient(180deg, rgba(252, 254, 255, 0.98), rgba(244, 248, 255, 0.96));
  box-shadow: 10px 0 16px -18px rgba(79, 104, 155, 0.24);
}

.room-calendar__date-input {
  position: absolute;
  inset: auto;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.room-calendar__corner-date {
  color: #15233e;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1;
}

.room-calendar__corner-caret {
  font-size: 11px;
  color: #7b8aa4;
}

.room-calendar__day {
  appearance: none;
  border: none;
  border-right: 1px solid rgba(112, 138, 187, 0.08);
  background: transparent;
  color: var(--app-muted, #64748b);
  padding: 8px 4px 7px;
  display: grid;
  align-items: center;
  justify-items: center;
  gap: 4px;
  min-height: 78px;
}

.room-calendar__day-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(240, 244, 255, 0.78);
  color: #1c2640;
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.room-calendar__day-weekday {
  font-size: 11px;
  font-weight: 600;
  color: #22304b;
  line-height: 1;
}

.room-calendar__day-capacity {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(240, 244, 255, 0.88);
  color: #5f7193;
  font-size: 10px;
  font-weight: 700;
}

.room-calendar__day.is-weekend .room-calendar__day-weekday,
.room-calendar__day.is-weekend .room-calendar__day-capacity {
  color: #ef4444;
}

.room-calendar__day.is-weekend .room-calendar__day-number {
  color: #e64848;
}

.room-calendar__day.is-today .room-calendar__day-number {
  box-shadow:
    0 0 0 6px rgba(63, 124, 255, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.88);
}

.room-calendar__day.is-today .room-calendar__day-weekday,
.room-calendar__day.is-today .room-calendar__day-capacity {
  color: var(--ion-color-primary);
}

.room-calendar__day.is-selected {
  background: rgba(74, 133, 255, 0.07);
}

.room-calendar__day.is-selected .room-calendar__day-number {
  background: linear-gradient(180deg, #4b86ff 0%, #2f6df2 100%);
  color: #ffffff;
  box-shadow: 0 8px 14px rgba(52, 116, 246, 0.2);
}

.room-calendar__day.is-selected .room-calendar__day-weekday {
  color: #2f6df2;
}

.room-calendar__day.is-selected .room-calendar__day-capacity {
  background: rgba(74, 133, 255, 0.12);
  color: #2f6df2;
}

.room-calendar__group-label {
  position: sticky;
  left: 0;
  z-index: 6;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border-right: 1px solid rgba(112, 138, 187, 0.08);
  background: rgba(244, 247, 253, 0.96);
  box-shadow: 10px 0 16px -18px rgba(79, 104, 155, 0.2);
}

.room-calendar__group-mark {
  width: 3px;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(180deg, #4d86ff 0%, #2f6df2 100%);
}

.room-calendar__group-label-text {
  font-size: 12px;
  font-weight: 700;
  color: #3958a8;
}

.room-calendar__group-cell {
  display: grid;
  place-items: center;
  min-height: 32px;
  border-right: 1px solid rgba(112, 138, 187, 0.08);
  color: #73829a;
  font-size: 12px;
  font-weight: 600;
}

.room-calendar__group-cell.is-selected {
  background: rgba(74, 133, 255, 0.08);
  color: #2f6df2;
}

.room-calendar__group-cell.is-today {
  box-shadow: inset 0 2px 0 rgba(76, 132, 255, 0.3);
}

.room-calendar__room-cell {
  position: sticky;
  left: 0;
  z-index: 4;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 255, 0.96));
  border: none;
  border-right: 1px solid rgba(112, 138, 187, 0.1);
  padding: 8px 8px 8px 10px;
  display: grid;
  align-content: center;
  justify-items: start;
  gap: 3px;
  min-height: 62px;
  color: var(--app-heading, #1e293b);
  box-shadow: 10px 0 14px -18px rgba(74, 99, 149, 0.24);
}

.room-calendar__room-cell.is-focused-closed {
  background: rgba(246, 248, 252, 0.98);
}

.room-calendar__room-cell.is-focused-maintenance {
  background: rgba(245, 158, 11, 0.1);
}

.room-calendar__room-cell.is-focused-retain {
  background: rgba(139, 92, 246, 0.1);
}

.room-calendar__room-cell.is-dirty {
  box-shadow:
    inset 0 0 0 1px rgba(245, 158, 11, 0.12),
    10px 0 14px -18px rgba(74, 99, 149, 0.24);
}

.room-calendar__room-number {
  font-size: 12px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: 0;
  word-break: break-word;
}

.room-calendar__room-flags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 4px;
}

.room-calendar__room-close-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 15px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.12);
  color: #d53535;
  font-size: 9px;
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
  font-size: 11px;
  color: #f59e0b;
}

.room-calendar__cell,
.room-calendar__reservation {
  appearance: none;
  border: none;
  border-right: 1px solid rgba(112, 138, 187, 0.08);
  background: #ffffff;
  color: var(--app-heading, #1e293b);
  min-height: 62px;
  padding: 6px;
  display: flex;
  align-items: center;
  text-align: left;
}

.room-calendar__cell {
  justify-content: center;
  color: #9aa5b9;
  font-size: 12px;
  font-weight: 600;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(251, 253, 255, 0.94));
}

.room-calendar__cell.is-today {
  box-shadow: inset 0 2px 0 rgba(76, 132, 255, 0.32);
}

.room-calendar__cell.is-selected {
  background: rgba(74, 133, 255, 0.08);
  box-shadow: inset 0 2px 0 #3f7cff;
}

.room-calendar__cell.is-closed {
  background:
    repeating-linear-gradient(
      -45deg,
      rgba(242, 245, 250, 0.96),
      rgba(242, 245, 250, 0.96) 12px,
      rgba(250, 252, 255, 0.96) 12px,
      rgba(250, 252, 255, 0.96) 24px
    );
  color: #8b97ab;
}

.room-calendar__cell.is-dirty {
  background: repeating-linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.98),
    rgba(255, 255, 255, 0.98) 8px,
    rgba(255, 246, 214, 0.96) 8px,
    rgba(255, 246, 214, 0.96) 16px
  );
}

.room-calendar__cell-content {
  line-height: 1;
}

.room-calendar__reservation {
  position: relative;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 2px;
  margin: 4px 4px 4px 3px;
  padding: 8px 10px 8px 12px;
  background: var(--reservation-bg, #ead9c6);
  color: var(--reservation-text, #ffffff);
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 0 10px 18px rgba(66, 85, 123, 0.14);
}

.room-calendar__reservation::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--reservation-accent, rgba(255, 255, 255, 0.8));
}

.room-calendar__reservation::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), transparent 46%);
  pointer-events: none;
}

.room-calendar__reservation[data-tone='sand'] {
  --reservation-bg: linear-gradient(135deg, #dfc59b 0%, #d9ba89 100%);
  --reservation-accent: #c2904d;
}

.room-calendar__reservation[data-tone='mint'] {
  --reservation-bg: linear-gradient(135deg, #78d9d4 0%, #57cbc8 100%);
  --reservation-accent: #42a8a5;
}

.room-calendar__reservation[data-tone='rose'] {
  --reservation-bg: linear-gradient(135deg, #f4a4b7 0%, #ef92aa 100%);
  --reservation-accent: #d66b86;
}

.room-calendar__reservation[data-tone='azure'] {
  --reservation-bg: linear-gradient(135deg, #9fd7eb 0%, #82c5df 100%);
  --reservation-accent: #4f9ec0;
}

.room-calendar__reservation[data-tone='indigo'] {
  --reservation-bg: linear-gradient(135deg, #82a2e8 0%, #6f92df 100%);
  --reservation-accent: #5073c8;
}

.room-calendar__reservation[data-tone='coral'] {
  --reservation-bg: linear-gradient(135deg, #f3b2a3 0%, #ee9a86 100%);
  --reservation-accent: #df7b62;
}

.room-calendar__reservation[data-tone='violet'] {
  --reservation-bg: linear-gradient(135deg, #b29cf0 0%, #9a84e7 100%);
  --reservation-accent: #7764c6;
}

.room-calendar__reservation[data-tone='slate'] {
  --reservation-bg: linear-gradient(135deg, #737a86 0%, #5e646f 100%);
  --reservation-accent: #3f4652;
}

.room-calendar__reservation.is-truncated-start::before {
  left: -6px;
}

.room-calendar__reservation.is-truncated-end {
  mask-image: linear-gradient(to right, #000 0%, #000 88%, rgba(0, 0, 0, 0.35) 100%);
  -webkit-mask-image: linear-gradient(to right, #000 0%, #000 88%, rgba(0, 0, 0, 0.35) 100%);
}

.room-calendar__reservation.is-selected-span {
  box-shadow:
    0 10px 18px rgba(66, 85, 123, 0.14),
    0 0 0 1px rgba(255, 255, 255, 0.48),
    0 0 0 3px rgba(63, 124, 255, 0.16);
}

.room-calendar__reservation.is-today-span {
  outline: 1px solid rgba(255, 255, 255, 0.34);
}

.room-calendar__reservation-topline {
  position: relative;
  z-index: 1;
  font-size: 9px;
  font-weight: 700;
  line-height: 1.2;
  opacity: 0.88;
}

.room-calendar__reservation-guest {
  position: relative;
  z-index: 1;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.25;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.room-calendar__reservation-channel {
  position: relative;
  z-index: 1;
  font-size: 9px;
  color: rgba(255, 255, 255, 0.84);
  line-height: 1.25;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
