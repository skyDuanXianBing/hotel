<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" default-href="/tabs/rooms" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ pageTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page room-detail-page">
      <section class="mobile-hero room-detail-hero">
        <p class="mobile-note room-detail-hero__eyebrow">{{ $t('iosStage5.roomStatus.singleRoom') }}</p>
        <h1 class="mobile-title">{{ room?.roomNumber || $t('routes.RoomStatusDetail') }}</h1>
        <p class="mobile-subtitle">{{ room?.roomType || $t('iosStage5.roomStatus.currentRoom') }} · {{ businessDate }}</p>
        <div class="mobile-chip-row" v-if="room">
          <span class="mobile-chip">{{ room.focusedStatusText }}</span>
          <span class="mobile-chip" v-if="room.isDirty">{{ $t('accommodation.roomTable.columns.dirtyRooms') }}</span>
          <span class="mobile-chip" v-if="room.focusedClosed && room.closeType">{{ closeTypeText }}</span>
        </div>
      </section>

      <div class="mobile-stack" v-if="room">
        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">{{ $t('iosStage5.roomStatus.keyActions') }}</h2>
              <p class="mobile-note">{{ $t('iosStage5.roomStatus.mobileActions') }}</p>
            </div>
          </div>

          <div class="detail-actions">
            <ion-button v-if="!room.focusedClosed && !room.reservation" @click="openBooking('create')">{{ $t('roomStatus.bookingModal.title.create') }}</ion-button>
            <ion-button v-if="!room.focusedClosed && !room.reservation" color="success" @click="openBooking('check-in')">{{ $t('roomStatus.booking.drawerTitle.checkIn') }}</ion-button>
            <ion-button v-if="!room.focusedClosed && !room.reservation" fill="outline" color="danger" @click="showCloseRoomModal = true">{{ $t('accommodation.roomPrice.closeRoom') }}</ion-button>
            <ion-button v-if="room.focusedClosed" color="success" @click="handleOpenRoom">{{ $t('iosStage5.roomStatus.openRoom') }}</ion-button>
            <ion-button fill="outline" color="warning" @click="handleToggleDirty">{{ dirtyActionText }}</ion-button>
            <ion-button v-if="room.reservation" fill="outline" @click="openReservationDetail(room.reservation.id)">{{ $t('roomStatus.detail.channelInfo.orderDetails') }}</ion-button>
          </div>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">{{ $t('iosStage5.roomStatus.shortStayStatus') }}</h2>
          <div class="room-detail-timeline">
            <div
              v-for="item in room.timeline"
              :key="item.date"
              class="room-detail-timeline__item"
              :class="{ 'room-detail-timeline__item--selected': item.isSelected }"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.statusText }}</strong>
            </div>
          </div>
        </section>

        <section class="mobile-card" v-if="room.closeRemark">
          <h2 class="mobile-section-title">{{ $t('iosStage5.roomStatus.closeRoomNote') }}</h2>
          <p class="mobile-note">{{ room.closeRemark }}</p>
        </section>

        <section class="mobile-card" v-if="room.reservation">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">{{ $t('iosStage5.roomStatus.stayInfo') }}</h2>
              <p class="mobile-note">{{ $t('iosStage5.roomStatus.staySummary') }}</p>
            </div>
          </div>

          <ReservationSummaryCard
            :reservation="room.reservation"
            :fallback-room-number="room.roomNumber"
            :fallback-room-type="room.roomType"
            @select="openReservationDetail(room.reservation.id)"
          />
        </section>
      </div>

      <section v-else class="mobile-card">
        <h2 class="mobile-section-title">{{ $t('settingsStage4.roomSort.empty.room') }}</h2>
        <p class="mobile-note">{{ $t('iosStage5.roomStatus.roomOutsideWindow') }}</p>
      </section>
    </ion-content>

    <BookingFormModal
      :is-open="showBookingModal"
      :mode="bookingMode"
      :channels="roomStatusStore.channels"
      :room="room"
      :submitting="bookingSubmitting"
      @dismiss="showBookingModal = false"
      @submit="handleBookingSubmit"
    />

    <CloseRoomModal
      :is-open="showCloseRoomModal"
      :room="room"
      :submitting="roomStatusStore.actionLoading"
      @dismiss="showCloseRoomModal = false"
      @submit="handleCloseRoomSubmit"
    />
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import BookingFormModal, {
  type BookingFormSubmitPayload,
  type BookingModalMode,
} from '@/components/room-status/BookingFormModal.vue'
import CloseRoomModal, { type CloseRoomSubmitPayload } from '@/components/room-status/CloseRoomModal.vue'
import ReservationSummaryCard from '@/components/room-status/ReservationSummaryCard.vue'
import { createReservation, type CreateReservationRequest } from '@/api/reservation'
import { useRoomStatusStore } from '@/stores/roomStatus'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const roomStatusStore = useRoomStatusStore()

const showBookingModal = ref(false)
const showCloseRoomModal = ref(false)
const bookingMode = ref<BookingModalMode>('create')
const bookingSubmitting = ref(false)
let hasCompletedInitialLoad = false

const roomId = computed(() => Number(route.params.roomId || 0))
const businessDate = computed(() => {
  const queryDate = route.query.date
  if (typeof queryDate === 'string' && queryDate) {
    return queryDate
  }
  return roomStatusStore.selectedDate
})

const room = computed(() => roomStatusStore.getRoomListItemById(roomId.value, businessDate.value))

const pageTitle = computed(() => {
  if (!room.value) {
    return t('routes.RoomStatusDetail')
  }
  return `${room.value.roomNumber} · ${t('routes.RoomStatusDetail')}`
})

const dirtyActionText = computed(() => (room.value?.isDirty ? t('iosStage5.roomStatus.setClean') : t('iosStage5.roomStatus.setDirty')))

const closeTypeText = computed(() => {
  if (!room.value?.closeType) {
    return t('roomStatus.store.roomState.outOfOrder')
  }
  if (room.value.closeType === 'maintenance') {
    return t('roomStatus.calendar.cell.maintenance')
  }
  if (room.value.closeType === 'retain') {
    return t('roomStatus.calendar.cell.retain')
  }
  return t('roomStatus.store.roomState.outOfOrder')
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function openBooking(mode: BookingModalMode) {
  bookingMode.value = mode
  showBookingModal.value = true
}

async function handleBookingSubmit(payload: BookingFormSubmitPayload) {
  if (!room.value) {
    showWarningToast(t('iosStage5.roomStatus.roomMissing'))
    return
  }
  if (!payload.guestName || !payload.channelId) {
    showWarningToast(t('iosStage5.roomStatus.completeOrder'))
    return
  }

  const requestData: CreateReservationRequest = {
    guestName: payload.guestName,
    guestPhone: payload.guestPhone,
    roomId: room.value.roomId,
    channelId: payload.channelId,
    checkInDate: payload.checkInDate,
    checkOutDate: payload.checkOutDate,
    adults: payload.adults,
    children: payload.children,
    totalAmount: payload.totalAmount,
    notes: payload.notes,
    directCheckIn: bookingMode.value === 'check-in',
  }

  bookingSubmitting.value = true
  try {
    const response = await createReservation(requestData)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5Pattern.submitFailed'))
    }

    showSuccessToast(
      bookingMode.value === 'check-in'
        ? t('iosStage5.roomStatus.checkInCreated')
        : t('stage5Final.roomStatus.reservationCreated'),
    )
    showBookingModal.value = false
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.submitFailed')))
    }
  } finally {
    bookingSubmitting.value = false
  }
}

async function handleCloseRoomSubmit(payload: CloseRoomSubmitPayload) {
  if (!room.value) {
    return
  }

  try {
    await roomStatusStore.closeRooms({
      roomIds: [room.value.roomId],
      startDate: payload.startDate,
      endDate: payload.endDate,
      type: payload.type,
      remark: payload.remark,
    })
    showCloseRoomModal.value = false
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('roomStatus.closeRoom.messages.failed')))
    }
  }
}

async function handleOpenRoom() {
  if (!room.value) {
    return
  }

  try {
    await roomStatusStore.openRooms({
      roomIds: [room.value.roomId],
      startDate: businessDate.value,
      endDate: businessDate.value,
    })
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('roomStatus.closeRoom.messages.openFailed')))
    }
  }
}

function handleToggleDirty() {
  if (!room.value) {
    return
  }
  roomStatusStore.toggleDirty(room.value.roomId)
}

async function openReservationDetail(reservationId: number) {
  await router.push({
    name: 'RoomReservationDetail',
    params: { reservationId },
  })
}

async function hydrateRoomReservationAmount() {
  try {
    if (roomStatusStore.calendarRooms.length === 0) {
      await roomStatusStore.initialize(true)
    }

    await roomStatusStore.hydrateMissingReservationAmounts(businessDate.value, roomId.value)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('iosStage5.roomStatus.roomDetailsLoadFailed')))
    }
  }
}

onMounted(async () => {
  try {
    await hydrateRoomReservationAmount()
  } finally {
    hasCompletedInitialLoad = true
  }
})

onIonViewWillEnter(async () => {
  if (!hasCompletedInitialLoad) {
    return
  }

  await hydrateRoomReservationAmount()
})
</script>

<style scoped>
.detail-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.room-detail-timeline {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.room-detail-timeline__item {
  padding: 10px 8px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid var(--app-border);
  display: grid;
  gap: 4px;
}

.room-detail-timeline__item span {
  font-size: 11px;
  color: var(--app-muted);
}

.room-detail-timeline__item strong {
  font-size: 12px;
}

.room-detail-timeline__item--selected {
  background: var(--app-primary-soft-strong);
  border-color: var(--app-border-strong);
}

.room-detail-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}
</style>
