<template>
  <section class="room-calendar" :class="{ 'is-loading': loading }">
    <div class="room-calendar__pricing-toolbar">
      <button
        class="room-calendar__price-toggle"
        :class="{ 'is-active': roomStatusStore.showCellPrice }"
        type="button"
        :aria-pressed="roomStatusStore.showCellPrice"
        @click="handleTogglePrice"
      >
        <ion-icon :icon="pricetagOutline" aria-hidden="true" />
        <span>{{ roomStatusStore.showCellPrice ? '隐藏房价' : '显示房价' }}</span>
      </button>

      <label v-if="roomStatusStore.showCellPrice" class="room-calendar__price-source">
        <span class="room-calendar__price-source-label">价格来源</span>
        <select
          class="room-calendar__price-source-select"
          :value="roomStatusStore.cellPriceSource"
          :disabled="roomStatusStore.pricePlanOptionsLoading"
          aria-label="房态价格来源"
          @change="handlePriceSourceChange"
        >
          <option
            v-for="option in roomStatusStore.priceSourceOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </label>
    </div>

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
                'is-focused': day.isFocused,
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
              'is-focused': day.isFocused,
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
              :class="{
                'is-selected': day.isSelected,
                'is-focused': day.isFocused,
                'is-today': day.isToday,
              }"
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
                  'is-focused-span': cell.containsFocused,
                  'is-today-span': cell.containsToday,
                  'is-truncated-start': cell.truncatedStart,
                  'is-truncated-end': cell.truncatedEnd,
                  'is-compact-span': cell.span === 1,
                }"
                :data-tone="resolveReservationTone(cell.reservation)"
                :style="{ gridColumn: `span ${cell.span}` }"
                type="button"
                @click="$emit('select-reservation', cell.reservation.id)"
              >
                <strong class="room-calendar__reservation-guest">
                  {{ cell.reservation.guestName || '未命名客人' }}
                </strong>
                <span class="room-calendar__reservation-channel">
                  {{ getReservationChannelLabel(cell.reservation, cell.span) }}
                </span>
                <span
                  v-if="hasReservationNotes(cell.reservation) && cell.span > 1"
                  class="room-calendar__reservation-note-badge"
                >
                  有备注
                </span>
                <span
                  v-else-if="hasReservationNotes(cell.reservation)"
                  class="room-calendar__reservation-note-dot"
                  aria-label="有备注"
                ></span>
              </button>

              <button
                v-else
                class="room-calendar__cell"
                :class="getEmptyCellClass(cell.item)"
                type="button"
                @click="$emit('open-room-actions', { roomId: room.roomId, date: cell.item.date })"
              >
                <span
                  class="room-calendar__cell-content"
                  :class="{ 'room-calendar__cell-content--available': isAvailableCell(cell.item) }"
                >
                  {{ getEmptyCellLabel(cell.item) }}
                  <span
                    v-if="hasPositivePrice(cell.item)"
                    class="room-calendar__cell-meta"
                  >
                    <ion-icon :icon="moonOutline" aria-hidden="true" />
                    <span>{{ cell.item.minStay ?? 1 }}</span>
                  </span>
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
import {
  caretDownOutline,
  moonOutline,
  notificationsOutline,
  pricetagOutline,
} from 'ionicons/icons'
import { computed, nextTick, ref, watch } from 'vue'
import type {
  RoomStatusDateItem,
  RoomStatusRoomItem,
  RoomTimelineItem,
} from '@/stores/roomStatus'
import { useRoomStatusStore } from '@/stores/roomStatus'
import type { ReservationDTO } from '@/api/reservation'
import {
  formatRoomStatusPrice,
  type RoomStatusPriceSource,
} from '@/utils/roomStatusPricing'
import { getStoreTodayDate, shiftBusinessDate } from '@/utils/storeBusinessDate'

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
      containsFocused: boolean
      containsToday: boolean
      truncatedStart: boolean
      truncatedEnd: boolean
    }

const ROOM_COLUMN_WIDTH = 58
const DAY_MIN_WIDTH = 63
const LOAD_NEXT_WINDOW_THRESHOLD_PX = DAY_MIN_WIDTH
const roomStatusStore = useRoomStatusStore()

const props = withDefaults(
  defineProps<{
    days: RoomStatusDateItem[]
    groups: RoomStatusRoomGroup[]
    loading: boolean
    selectedDate?: string
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
  'load-previous-window': []
  'load-next-window': []
}>()

const datePickerInput = ref<HTMLInputElement | null>(null)
const stickyDatePickerInput = ref<HTMLInputElement | null>(null)
const headerScrollContainer = ref<HTMLDivElement | null>(null)
const scrollContainer = ref<HTMLDivElement | null>(null)
let syncingScroll = false
let hasRequestedPreviousWindow = false
let hasRequestedNextWindow = false
let pendingSelectedDateAlignment = true
let pendingWindowShiftDirection: 'previous' | 'next' | null = null

const gridStyle = computed(() => ({
  gridTemplateColumns: `${ROOM_COLUMN_WIDTH}px repeat(${props.days.length}, minmax(${DAY_MIN_WIDTH}px, 1fr))`,
}))

const totalMinWidth = computed(() => ROOM_COLUMN_WIDTH + DAY_MIN_WIDTH * props.days.length)

const todayDate = computed(() => {
  return getStoreTodayDate()
})

const selectedDay = computed(() => props.days.find((item) => item.isSelected) || null)

const selectedDateValue = computed(() => props.selectedDate || selectedDay.value?.date || todayDate.value)

const selectedDateLabel = computed(() => {
  if (selectedDay.value) {
    return selectedDay.value.label
  }

  return getMonthDayLabel(selectedDateValue.value)
})

const visibleDateKey = computed(() => props.days.map((item) => item.date).join('|'))

function alignSelectedDateIntoView() {
  const container = scrollContainer.value
  if (!container) {
    return false
  }

  const selectedIndex = props.days.findIndex((item) => item.isSelected)
  if (selectedIndex < 0) {
    return false
  }

  const maxStartIndex = Math.max(props.days.length - props.viewportDays, 0)
  const preferredStartIndex = Math.min(Math.max(selectedIndex - 1, 0), maxStartIndex)
  const targetLeft = preferredStartIndex * DAY_MIN_WIDTH

  syncScrollLeft(targetLeft)
  return true
}

function alignWindowAfterDateRangeChange() {
  if (pendingSelectedDateAlignment && alignSelectedDateIntoView()) {
    pendingSelectedDateAlignment = false
    pendingWindowShiftDirection = null
    return
  }

  pendingSelectedDateAlignment = false

  const maxStartIndex = Math.max(props.days.length - props.viewportDays, 0)
  let handoffStartIndex = 0

  if (pendingWindowShiftDirection === 'next') {
    handoffStartIndex = Math.min(Math.max(props.viewportDays - 1, 0), maxStartIndex)
  }

  if (pendingWindowShiftDirection === 'previous') {
    handoffStartIndex = Math.min(Math.max(props.viewportDays, 0), maxStartIndex)
  }

  pendingWindowShiftDirection = null
  syncScrollLeft(handoffStartIndex * DAY_MIN_WIDTH)
}

watch(
  selectedDateValue,
  async () => {
    pendingSelectedDateAlignment = true
    pendingWindowShiftDirection = null
    hasRequestedPreviousWindow = false
    hasRequestedNextWindow = false
    await nextTick()
    if (alignSelectedDateIntoView()) {
      pendingSelectedDateAlignment = false
    }
  },
  { immediate: true },
)

watch(visibleDateKey, async () => {
  hasRequestedPreviousWindow = false
  hasRequestedNextWindow = false
  await nextTick()
  alignWindowAfterDateRangeChange()
})

function getMonthDayLabel(date: string) {
  const [, month, day] = date.split('-')
  if (!month || !day) {
    return date
  }

  return `${month}-${day}`
}

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

function shouldLoadPreviousWindow(container: HTMLDivElement | null) {
  if (!container || props.loading || hasRequestedPreviousWindow) {
    return false
  }

  const maxScrollLeft = container.scrollWidth - container.clientWidth
  if (maxScrollLeft <= 0) {
    return false
  }

  return container.scrollLeft <= LOAD_NEXT_WINDOW_THRESHOLD_PX
}

function requestPreviousWindowIfNeeded(container: HTMLDivElement | null) {
  if (!shouldLoadPreviousWindow(container)) {
    return
  }

  hasRequestedPreviousWindow = true
  pendingWindowShiftDirection = 'previous'
  emit('load-previous-window')
}

function shouldLoadNextWindow(container: HTMLDivElement | null) {
  if (!container || props.loading || hasRequestedNextWindow) {
    return false
  }

  const maxScrollLeft = container.scrollWidth - container.clientWidth
  if (maxScrollLeft <= 0) {
    return false
  }

  return maxScrollLeft - container.scrollLeft <= LOAD_NEXT_WINDOW_THRESHOLD_PX
}

function requestNextWindowIfNeeded(container: HTMLDivElement | null) {
  if (!shouldLoadNextWindow(container)) {
    return
  }

  hasRequestedNextWindow = true
  pendingWindowShiftDirection = 'next'
  emit('load-next-window')
}

function handleHeaderScroll() {
  syncScrollContainers(headerScrollContainer.value, scrollContainer.value)
  requestPreviousWindowIfNeeded(headerScrollContainer.value)
  requestNextWindowIfNeeded(headerScrollContainer.value)
}

function handleBodyScroll() {
  syncScrollContainers(scrollContainer.value, headerScrollContainer.value)
  requestPreviousWindowIfNeeded(scrollContainer.value)
  requestNextWindowIfNeeded(scrollContainer.value)
}

function handleDateInputChange(event: Event) {
  const target = event.target as HTMLInputElement | null
  if (!target?.value) {
    return
  }

  emit('select-date', target.value)
}

function handleTogglePrice() {
  void roomStatusStore.setShowCellPrice(!roomStatusStore.showCellPrice)
}

function handlePriceSourceChange(event: Event) {
  const target = event.target as HTMLSelectElement | null
  if (!target) {
    return
  }

  void roomStatusStore.setCellPriceSource(target.value as RoomStatusPriceSource)
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
        containsFocused: segment.some((item) => item.isFocused),
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
  return shiftBusinessDate(date, amount)
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
    return 'slate'
  }
  if (normalized.includes('tujia') || channelName.includes('途家')) {
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

function getReservationNotesText(reservation: ReservationDTO) {
  const reservationWithRemark = reservation as ReservationDTO & { remark?: string }
  const notesValue = reservation.notes ?? reservationWithRemark.remark ?? ''
  return notesValue.trim()
}

function hasReservationNotes(reservation: ReservationDTO) {
  return getReservationNotesText(reservation).length > 0
}

function getReservationChannelLabel(reservation: ReservationDTO, span: number) {
  const channelName = (reservation.channelName || '自来客').trim()

  if (span > 1) {
    return channelName
  }

  const normalized = channelName.toLowerCase()

  if (normalized.includes('booking')) {
    return 'Bkg'
  }
  if (normalized.includes('airbnb')) {
    return 'Air'
  }
  if (normalized.includes('agoda')) {
    return 'Agd'
  }
  if (normalized.includes('trip') || channelName.includes('携程')) {
    return 'Trip'
  }
  if (normalized.includes('tujia') || channelName.includes('途家')) {
    return '途家'
  }
  if (channelName.includes('美团')) {
    return '美团'
  }
  if (channelName.includes('抖音')) {
    return '抖音'
  }
  if (channelName.includes('小猪')) {
    return '小猪'
  }
  if (channelName.includes('自来客')) {
    return '自'
  }

  const compactLength = /[a-z0-9]/i.test(channelName) ? 4 : 2
  return Array.from(channelName).slice(0, compactLength).join('')
}

function getEmptyCellClass(item: RoomTimelineItem) {
  return {
    'is-today': item.isToday,
    'is-selected': item.isSelected,
    'is-focused': item.isFocused,
    'is-available': item.businessState === 'available',
    'is-closed':
      item.businessState === 'closed' ||
      item.businessState === 'out_of_order' ||
      item.businessState === 'maintenance' ||
      item.businessState === 'retain',
    'is-dirty': item.isDirty && item.businessState === 'available',
  }
}

function isAvailableCell(item: RoomTimelineItem) {
  return item.businessState === 'available'
}

function hasPositivePrice(item: RoomTimelineItem) {
  return (
    isAvailableCell(item) &&
    !item.reservation &&
    !item.isClosed &&
    typeof item.price === 'number' &&
    Number.isFinite(item.price) &&
    item.price > 0
  )
}

function getEmptyCellLabel(item: RoomTimelineItem) {
  if (item.businessState === 'available') {
    return formatRoomStatusPrice(item.price) || item.statusText
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
  background: #ffffff;
  color: #2b2b2b;
}

.room-calendar.is-loading {
  opacity: 0.96;
}

.room-calendar__pricing-toolbar {
  min-height: 44px;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  border-bottom: 1px solid #e7e8eb;
  background: #ffffff;
}

.room-calendar__price-toggle {
  min-height: 32px;
  padding: 0 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border: 1px solid #d9dde5;
  border-radius: 8px;
  background: #ffffff;
  color: #4b5563;
  font-size: 13px;
}

.room-calendar__price-toggle.is-active {
  border-color: #b8d2ff;
  background: #eef6ff;
  color: #287fc5;
}

.room-calendar__price-toggle ion-icon {
  width: 15px;
  height: 15px;
}

.room-calendar__price-source {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.room-calendar__price-source-label {
  color: #6b7280;
  font-size: 12px;
  white-space: nowrap;
}

.room-calendar__price-source-select {
  max-width: 148px;
  min-height: 32px;
  padding: 0 28px 0 9px;
  border: 1px solid #d9dde5;
  border-radius: 8px;
  background: #ffffff;
  color: #333333;
  font-size: 13px;
}

.room-calendar__price-source-select:focus-visible,
.room-calendar__price-toggle:focus-visible {
  outline: 2px solid #8bb7ff;
  outline-offset: 2px;
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
  background: #ffffff;
}

.room-calendar__scroll {
  overflow-y: visible;
  background: #ffffff;
}

.room-calendar__header-scroll::-webkit-scrollbar,
.room-calendar__scroll::-webkit-scrollbar {
  display: none;
}

.room-calendar__inner {
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.room-calendar__row {
  display: grid;
  align-items: stretch;
  box-sizing: border-box;
  background: #ffffff;
  border-bottom: 1px solid #e7e8eb;
}

.room-calendar__row--header {
  height: 70px;
  min-height: 70px;
  max-height: 70px;
  background: #ffffff;
  border-bottom: 1px solid #e4e5e8;
  box-shadow: none;
}

.room-calendar__scroll > .room-calendar__inner > .room-calendar__row--header {
  display: none;
}

.room-calendar__row--group {
  height: 24px;
  min-height: 24px;
  max-height: 24px;
  background: #ffffff;
}

.room-calendar__row:not(.room-calendar__row--header):not(.room-calendar__row--group) {
  height: 48px;
  min-height: 48px;
  max-height: 48px;
}

.room-calendar__corner {
  position: sticky;
  left: 0;
  z-index: 10;
  box-sizing: border-box;
  border: none;
  border-right: 1px solid #e4e5e8;
  height: 70px;
  min-height: 70px;
  max-height: 70px;
  padding: 0 3px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: #ffffff;
  box-shadow: none;
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
  color: #333333;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0;
  line-height: 1;
  white-space: nowrap;
}

.room-calendar__corner-caret {
  flex-shrink: 0;
  font-size: 10px;
  color: #333333;
}

.room-calendar__day {
  appearance: none;
  box-sizing: border-box;
  border: none;
  border-right: 1px solid #e4e5e8;
  background: transparent;
  color: #333333;
  height: 70px;
  min-height: 70px;
  max-height: 70px;
  padding: 4px 2px;
  display: grid;
  align-items: center;
  justify-items: center;
  gap: 3px;
}

.room-calendar__day-number {
  position: relative;
  width: 25px;
  height: 25px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #f5f6fb;
  color: #333333;
  font-size: 14px;
  font-weight: 400;
  line-height: 1;
  box-shadow: none;
}

.room-calendar__day-weekday {
  font-size: 12px;
  font-weight: 400;
  color: #333333;
  line-height: 1;
}

.room-calendar__day-capacity {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #f5f6fb;
  color: #333333;
  font-size: 11px;
  font-weight: 400;
}

.room-calendar__day.is-weekend .room-calendar__day-weekday,
.room-calendar__day.is-weekend .room-calendar__day-capacity {
  color: #c92a54;
}

.room-calendar__day.is-weekend .room-calendar__day-number {
  color: #c92a54;
}

.room-calendar__day.is-today .room-calendar__day-number::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 3px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #287fc5;
  transform: translateX(-50%);
}

.room-calendar__day.is-selected {
  background: #eef6ff;
}

.room-calendar__day.is-selected .room-calendar__day-number {
  background: #2f74ee;
  color: #ffffff;
  box-shadow: none;
}

.room-calendar__day.is-selected.is-today .room-calendar__day-number::after {
  background: #ffffff;
}

.room-calendar__day.is-selected .room-calendar__day-weekday {
  color: #287fc5;
}

.room-calendar__day.is-selected .room-calendar__day-capacity {
  background: #e9f1ff;
  color: #287fc5;
}

.room-calendar__day.is-focused:not(.is-selected) {
  background: #f8fbff;
}

.room-calendar__day.is-focused:not(.is-selected) .room-calendar__day-number {
  box-shadow: inset 0 0 0 1px #b9d5f2;
  color: #287fc5;
}

.room-calendar__day.is-focused:not(.is-selected) .room-calendar__day-weekday,
.room-calendar__day.is-focused:not(.is-selected) .room-calendar__day-capacity {
  color: #287fc5;
}

.room-calendar__group-label {
  position: sticky;
  left: 0;
  z-index: 6;
  box-sizing: border-box;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 0;
  height: 24px;
  min-height: 24px;
  max-height: 24px;
  padding: 0 4px 0 5px;
  border-left: 2px solid #1096d8;
  border-right: 1px solid #e4e5e8;
  background: #ffffff;
  box-shadow: none;
}

.room-calendar__group-mark {
  display: none;
}

.room-calendar__group-label-text {
  min-width: 0;
  max-width: 100%;
  font-size: 10px;
  font-weight: 400;
  line-height: 1.05;
  color: #1096d8;
  overflow: hidden;
  overflow-wrap: normal;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.room-calendar__group-cell {
  box-sizing: border-box;
  display: grid;
  place-items: center;
  height: 24px;
  min-height: 24px;
  max-height: 24px;
  border-right: 1px solid #e4e5e8;
  color: #333333;
  font-size: 12px;
  font-weight: 400;
}

.room-calendar__group-cell.is-selected {
  background: #eef6ff;
  color: #287fc5;
}

.room-calendar__group-cell.is-focused:not(.is-selected) {
  background: #f8fbff;
  color: #287fc5;
}

.room-calendar__group-cell.is-today {
  box-shadow: none;
}

.room-calendar__room-cell {
  position: sticky;
  left: 0;
  z-index: 4;
  box-sizing: border-box;
  min-width: 0;
  background: #ffffff;
  border: none;
  border-right: 1px solid #e4e5e8;
  height: 48px;
  min-height: 48px;
  max-height: 48px;
  padding: 4px 4px 4px 6px;
  display: grid;
  align-content: center;
  justify-items: start;
  gap: 2px;
  color: #333333;
  box-shadow: none;
}

.room-calendar__room-cell.is-focused-closed {
  background: #f7f7f7;
}

.room-calendar__room-cell.is-focused-maintenance {
  background: #fff7e6;
}

.room-calendar__room-cell.is-focused-retain {
  background: #f7f2ff;
}

.room-calendar__room-cell.is-dirty {
  box-shadow: inset 0 0 0 1px rgba(245, 158, 11, 0.12);
}

.room-calendar__room-number {
  max-width: 100%;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  min-height: 12px;
  padding: 0 3px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.12);
  color: #d53535;
  font-size: 8px;
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
  font-size: 9px;
  color: #f59e0b;
}

.room-calendar__cell,
.room-calendar__reservation {
  appearance: none;
  box-sizing: border-box;
  min-width: 0;
  border: none;
  border-right: 1px solid #e4e5e8;
  background: #ffffff;
  color: #333333;
  height: 48px;
  min-height: 48px;
  max-height: 48px;
  padding: 2px;
  display: flex;
  align-items: center;
  text-align: left;
}

.room-calendar__cell {
  justify-content: center;
  color: #333333;
  font-size: 12px;
  font-weight: 400;
  background: #ffffff;
}

.room-calendar__cell.is-today {
  box-shadow: none;
}

.room-calendar__cell.is-selected {
  background: #eef6ff;
  color: #287fc5;
  box-shadow: none;
}

.room-calendar__cell.is-focused:not(.is-selected) {
  background: #f8fbff;
  box-shadow: inset 1px 0 0 rgba(47, 116, 238, 0.08);
}

.room-calendar__cell.is-available {
  color: #399018;
  font-size: 12px;
  background: #f0ffea;
}

.room-calendar__cell.is-available.is-selected {
  color: #639a49;
  background: #f0ffea;
}

.room-calendar__cell.is-closed {
  background: #f7f7f7;
  color: #8d8d8d;
}

.room-calendar__cell.is-dirty {
  background: #fff9e8;
}

.room-calendar__cell-content {
  line-height: 1.1;
  white-space: normal;
}

.room-calendar__cell-content--available {
  display: grid;
  justify-items: center;
  gap: 2px;
  color: #639a49;
  line-height: 1.1;
}

.room-calendar__cell-meta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #639a49;
  font-size: 10px;
  line-height: 1;
}

.room-calendar__cell-meta ion-icon {
  width: 10px;
  height: 10px;
  font-size: 10px;
}

.room-calendar__reservation {
  position: relative;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  height: 44px;
  min-height: 44px;
  max-height: 44px;
  gap: 1px;
  margin: 2px 0;
  padding: 2px 5px 2px 6px;
  background: var(--reservation-bg, #ead9c6);
  color: var(--reservation-text, #ffffff);
  overflow: hidden;
  border-radius: 5px;
  box-shadow: none;
}

.room-calendar__reservation.is-compact-span {
  gap: 1px;
  padding: 2px 4px;
}

.room-calendar__reservation::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.18), transparent 52%);
  pointer-events: none;
}

.room-calendar__reservation[data-tone='sand'] {
  --reservation-bg: #ffc0a8;
  --reservation-pill-bg: #f09a44;
  --reservation-text: #ffffff;
}

.room-calendar__reservation[data-tone='mint'] {
  --reservation-bg: #bfead3;
  --reservation-pill-bg: #22a96a;
  --reservation-text: #ffffff;
}

.room-calendar__reservation[data-tone='rose'] {
  --reservation-bg: #ffc0d1;
  --reservation-pill-bg: #ef4f80;
  --reservation-text: #ffffff;
}

.room-calendar__reservation[data-tone='azure'] {
  --reservation-bg: #69b7f1;
  --reservation-pill-bg: #fa672c;
  --reservation-text: #ffffff;
}

.room-calendar__reservation[data-tone='indigo'] {
  --reservation-bg: #8fb9ff;
  --reservation-pill-bg: #2f74ee;
  --reservation-text: #ffffff;
}

.room-calendar__reservation[data-tone='coral'] {
  --reservation-bg: #ffc0a8;
  --reservation-pill-bg: #ff2f67;
  --reservation-text: #ffffff;
}

.room-calendar__reservation[data-tone='violet'] {
  --reservation-bg: #ffc0a8;
  --reservation-pill-bg: #b01bcf;
  --reservation-text: #ffffff;
}

.room-calendar__reservation[data-tone='slate'] {
  --reservation-bg: #cfcfcf;
  --reservation-pill-bg: #2f74ee;
  --reservation-text: #ffffff;
}

.room-calendar__reservation.is-truncated-start::before {
  display: none;
}

.room-calendar__reservation.is-truncated-end {
  mask-image: linear-gradient(to right, #000 0%, #000 92%, rgba(0, 0, 0, 0.32) 100%);
  -webkit-mask-image: linear-gradient(to right, #000 0%, #000 92%, rgba(0, 0, 0, 0.32) 100%);
}

.room-calendar__reservation.is-selected-span {
  box-shadow: inset 0 0 0 1px rgba(47, 116, 238, 0.28);
}

.room-calendar__reservation.is-focused-span:not(.is-selected-span) {
  box-shadow: inset 0 0 0 1px rgba(47, 116, 238, 0.16);
}

.room-calendar__reservation.is-today-span {
  outline: 0;
}

.room-calendar__reservation-topline {
  position: relative;
  z-index: 1;
  font-size: 11px;
  font-weight: 400;
  line-height: 1.2;
  opacity: 0.9;
}

.room-calendar__reservation-guest {
  position: relative;
  z-index: 1;
  width: 100%;
  font-size: 11px;
  font-weight: 400;
  line-height: 12px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
  white-space: nowrap;
}

.room-calendar__reservation.is-compact-span .room-calendar__reservation-guest {
  display: block;
  white-space: nowrap;
  font-size: 10px;
  line-height: 11px;
}

.room-calendar__reservation-channel {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 12px;
  height: 12px;
  padding: 0 4px;
  border-radius: 3px;
  background: var(--reservation-pill-bg, rgba(255, 255, 255, 0.28));
  color: rgba(255, 255, 255, 0.96);
  font-size: 9px;
  font-weight: 400;
  line-height: 1;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-calendar__reservation.is-compact-span .room-calendar__reservation-channel {
  min-height: 12px;
  height: 12px;
  padding: 0 3px;
  border-radius: 3px;
  font-size: 8px;
  max-width: 100%;
}

.room-calendar__reservation-note-badge {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 12px;
  height: 12px;
  padding: 0 4px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.34);
  color: rgba(255, 255, 255, 0.92);
  font-size: 9px;
  font-weight: 400;
  line-height: 1;
}

.room-calendar__reservation-note-dot {
  position: absolute;
  right: 3px;
  bottom: 3px;
  z-index: 2;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.24);
}
</style>
