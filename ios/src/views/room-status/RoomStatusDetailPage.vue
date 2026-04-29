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
        <p class="mobile-note room-detail-hero__eyebrow">单房详情</p>
        <h1 class="mobile-title">{{ room?.roomNumber || '房间详情' }}</h1>
        <p class="mobile-subtitle">{{ room?.roomType || '当前房间' }} · {{ businessDate }}</p>
        <div class="mobile-chip-row" v-if="room">
          <span class="mobile-chip">{{ room.focusedStatusText }}</span>
          <span class="mobile-chip" v-if="room.isDirty">脏房</span>
          <span class="mobile-chip" v-if="room.focusedClosed && room.closeType">{{ closeTypeText }}</span>
        </div>
      </section>

      <div class="mobile-stack" v-if="room">
        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">关键动作</h2>
              <p class="mobile-note">把桌面悬浮操作收敛成移动端按钮流。</p>
            </div>
          </div>

          <div class="detail-actions">
            <ion-button v-if="!room.focusedClosed && !room.reservation" @click="openBooking('create')">快速预订</ion-button>
            <ion-button v-if="!room.focusedClosed && !room.reservation" color="success" @click="openBooking('check-in')">直接入住</ion-button>
            <ion-button v-if="!room.focusedClosed && !room.reservation" fill="outline" color="danger" @click="showCloseRoomModal = true">关房</ion-button>
            <ion-button v-if="room.focusedClosed" color="success" @click="handleOpenRoom">开房</ion-button>
            <ion-button fill="outline" color="warning" @click="handleToggleDirty">{{ dirtyActionText }}</ion-button>
            <ion-button v-if="room.reservation" fill="outline" @click="openReservationDetail(room.reservation.id)">订单详情</ion-button>
          </div>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">短周期房态</h2>
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
          <h2 class="mobile-section-title">关房备注</h2>
          <p class="mobile-note">{{ room.closeRemark }}</p>
        </section>

        <section class="mobile-card" v-if="room.reservation">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">入住信息</h2>
              <p class="mobile-note">展示当前房间的订单摘要、渠道与入离日期。</p>
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
        <h2 class="mobile-section-title">暂无房间数据</h2>
        <p class="mobile-note">当前房间可能不在所选日期窗口内，请返回房态主页重新选择。</p>
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
    return '房间详情'
  }
  return `${room.value.roomNumber} · 房态详情`
})

const dirtyActionText = computed(() => (room.value?.isDirty ? '置净' : '置脏'))

const closeTypeText = computed(() => {
  if (!room.value?.closeType) {
    return '停用'
  }
  if (room.value.closeType === 'maintenance') {
    return '维修'
  }
  if (room.value.closeType === 'retain') {
    return '保留'
  }
  return '停用'
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
    showWarningToast('未找到房间信息')
    return
  }
  if (!payload.guestName || !payload.channelId) {
    showWarningToast('请填写完整订单信息')
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
      throw new Error(response.message || '订单提交失败')
    }

    showSuccessToast(bookingMode.value === 'check-in' ? '入住已创建' : '预订已创建')
    showBookingModal.value = false
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '订单提交失败'))
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
      showWarningToast(resolveWarningMessage(error, '关房失败'))
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
      showWarningToast(resolveWarningMessage(error, '开房失败'))
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
      showWarningToast(resolveWarningMessage(error, '房间详情加载失败'))
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
