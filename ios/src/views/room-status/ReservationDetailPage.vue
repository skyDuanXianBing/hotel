<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button
            class="app-page-header__back-btn"
            :default-href="defaultBackHref"
          />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ pageTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page reservation-detail-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('order.mobile.refresh')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero reservation-detail-hero" v-if="reservation">
        <div class="reservation-detail-hero__topline">
          <p class="mobile-note reservation-detail-hero__eyebrow">{{ $t('roomStatus.detail.channelInfo.orderDetails') }}</p>
          <span :class="['mobile-chip', 'reservation-detail-hero__status', `is-${statusColor}`]">
            {{ statusText }}
          </span>
        </div>

        <div class="reservation-detail-hero__headline-row">
          <div class="reservation-detail-hero__headline-copy">
            <h1 class="mobile-title">{{ reservation.guestName }}</h1>
            <p class="mobile-subtitle reservation-detail-hero__orderline">
              <span>{{ reservation.orderNumber }}</span>
              <span>{{ reservation.channelName || $t('roomStatus.common.defaultChannel') }}</span>
            </p>
          </div>

          <div class="reservation-detail-hero__message-entry">
            <ion-button
              fill="outline"
              size="small"
              class="reservation-detail-hero__message-button"
              :disabled="!reservation"
              @click="openReservationMessages"
            >
              {{ $t('home.quick.messages.0') }}
            </ion-button>
          </div>
        </div>

        <div class="reservation-detail-hero__meta-grid">
          <div class="reservation-detail-hero__meta-item">
            <span>{{ $t('roomStatus.action.checkIn') }}</span>
            <strong>{{ reservation.checkInDate }}</strong>
          </div>
          <div class="reservation-detail-hero__meta-item">
            <span>{{ $t('roomStatus.hoverCard.checkOutDate') }}</span>
            <strong>{{ reservation.checkOutDate }}</strong>
          </div>
          <div class="reservation-detail-hero__meta-item">
            <span>{{ $t('accommodation.common.room') }}</span>
            <strong>{{ reservation.roomTypeName || $t('roomStatus.bookingModal.unassignedRoom') }} {{ reservation.roomNumber || '' }}</strong>
          </div>
          <div class="reservation-detail-hero__meta-item">
            <span>{{ $t('order.table.totalGuests') }}</span>
            <strong class="reservation-detail-hero__guest-count">
              <span>{{ reservation.adults || 1 }}{{ $t('stage5VisibleText.172') }}</span>
              <span>{{ reservation.children || 0 }}{{ $t('stage5VisibleText.128') }}</span>
            </strong>
          </div>
        </div>

        <div v-if="sourceOrderTab || orderBoxItem" class="mobile-chip-row reservation-detail-hero__chips">
          <span v-if="sourceOrderTab" class="mobile-chip">{{ $t('stage5DynamicUi.129') }}{{ getOrderTabLabel(sourceOrderTab) }}</span>
          <span v-if="orderBoxItem" class="mobile-chip">{{ $t('stage5VisibleText.157') }}</span>
        </div>
      </section>

      <div class="mobile-stack" v-if="reservation">
        <section class="mobile-card reservation-detail-panel">
          <div v-if="actionLoading" class="detail-actions__busy">
            <ion-spinner name="crescent" />
          </div>

          <div class="detail-actions">
            <ion-button v-if="canCheckIn" class="detail-actions__primary" color="success" @click="handleCheckIn">{{ $t('roomStatus.detail.checkInAction') }}</ion-button>
            <ion-button v-else-if="canCheckOut" class="detail-actions__primary" color="warning" @click="handleCheckOut">{{ $t('roomStatus.detail.checkOutAction') }}</ion-button>
            <ion-button v-if="canEditOrder" fill="outline" class="detail-actions__secondary" @click="showBookingModal = true">{{ $t('roomStatus.common.modifyOrder') }}</ion-button>
            <ion-button
              fill="outline"
              class="detail-actions__secondary"
              :class="{ 'detail-actions__secondary--solo': !canEditOrder && !canCheckIn && !canCheckOut }"
              @click="presentMoreActions"
            >
              {{ $t('roomStatus.common.moreActions') }}
            </ion-button>
          </div>
        </section>

        <section class="mobile-card reservation-detail-content">
          <ion-segment v-model="activeSegment" class="reservation-detail-content__segment">
            <ion-segment-button value="detail">
              <ion-label>{{ $t('accommodation.cleaning.detail') }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="logs">
              <ion-label>{{ $t('iosStage5.roomStatus.logs') }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="channel">
              <ion-label>{{ $t('home.quick.channels.0') }}</ion-label>
            </ion-segment-button>
          </ion-segment>

          <div v-if="activeSegment === 'detail'" class="reservation-detail-content__body reservation-detail-content__body--detail">
            <article class="detail-section detail-summary-card">
              <div class="detail-summary-card__hero">
                <div class="detail-summary-card__hero-copy">
                  <span>{{ remainingPaymentLabel }}</span>
                  <strong :class="['detail-summary-card__amount', remainingPaymentClass]">{{ remainingPaymentText }}</strong>
                </div>
                <div class="detail-summary-card__hero-side">
                  <span>{{ $t('roomStatus.booking.orderAmount') }}</span>
                  <strong>{{ totalAmountText }}</strong>
                </div>
              </div>

              <div class="detail-summary-card__metrics">
                <div class="detail-summary-card__metric">
                  <span>{{ $t('roomStatus.common.paidAmount') }}</span>
                  <strong>{{ totalPaymentText }}</strong>
                </div>
                <div class="detail-summary-card__metric">
                  <span>{{ $t('roomStatus.detail.otherConsumption') }}</span>
                  <strong>{{ totalConsumptionText }}</strong>
                </div>
              </div>
            </article>

            <article class="detail-section">
              <div class="detail-section__head">
                <h3>{{ $t('iosStage5.roomStatus.stayInfo') }}</h3>
              </div>

              <div class="detail-fact-list">
                <div class="detail-fact-list__row">
                  <span>{{ $t('accommodation.common.room') }}</span>
                  <strong>{{ reservation.roomTypeName || $t('roomStatus.bookingModal.unassignedRoom') }} {{ reservation.roomNumber || '' }}</strong>
                </div>
                <div class="detail-fact-list__row">
                  <span>{{ $t('roomStatus.action.checkIn') }}</span>
                  <strong>{{ reservation.checkInDate }}</strong>
                </div>
                <div class="detail-fact-list__row">
                  <span>{{ $t('roomStatus.hoverCard.checkOutDate') }}</span>
                  <strong>{{ reservation.checkOutDate }}</strong>
                </div>
                <div class="detail-fact-list__row">
                  <span>{{ $t('iosStage5.roomStatus.guests') }}</span>
                  <strong>{{ reservation.adults || 1 }}{{ $t('stage5DynamicUi.115') }}{{ reservation.children || 0 }}{{ $t('stage5VisibleText.128') }}</strong>
                </div>
              </div>
            </article>

            <article class="detail-section detail-record-section">
              <div class="detail-section__head">
                <h3>{{ $t('iosStage5.roomStatus.consumptionRecords') }}</h3>
                <span v-if="consumptions.length > 0" class="detail-section__meta">{{ consumptions.length }} {{ $t('stage5DynamicUi.125') }}</span>
              </div>

              <div v-if="consumptions.length > 0" class="detail-list">
                <div v-for="item in consumptions" :key="item.id || `${item.item}-${item.date}`" class="detail-list__item">
                  <div class="detail-list__main">
                    <strong>{{ item.item }}</strong>
                    <div class="detail-list__meta">
                      <span>× {{ item.quantity }}</span>
                      <span>{{ item.date }}</span>
                    </div>
                    <p v-if="item.remark">{{ item.remark }}</p>
                  </div>
                  <div class="detail-list__actions">
                    <strong class="detail-list__amount">
                    {{ formatAmount(item.amount, currentCurrency, currentMoneyContext) }}
                    </strong>
                    <ion-button fill="clear" size="small" color="danger" @click="handleDeleteConsumption(item.id)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
                  </div>
                </div>
              </div>
              <p v-else class="mobile-note">{{ $t('iosStage5.roomStatus.noConsumptionRecords') }}</p>
            </article>

            <article class="detail-section detail-record-section">
              <div class="detail-section__head">
                <h3>{{ $t('iosStage5.roomStatus.paymentRecords') }}</h3>
                <span v-if="payments.length > 0" class="detail-section__meta">{{ payments.length }} {{ $t('stage5DynamicUi.125') }}</span>
              </div>

              <div v-if="payments.length > 0" class="detail-list">
                <div
                  v-for="item in payments"
                  :key="item.id || `${item.type}-${item.date}`"
                  class="detail-list__item"
                >
                  <div class="detail-list__main">
                    <strong>{{ item.type }}</strong>
                    <div class="detail-list__meta">
                      <span>{{ item.paymentMethod }}</span>
                      <span>{{ item.date }}</span>
                    </div>
                    <p v-if="item.remark">{{ item.remark }}</p>
                  </div>
                  <div class="detail-list__actions">
                    <strong class="detail-list__amount">
                    {{ formatAmount(item.amount, currentCurrency, currentMoneyContext) }}
                    </strong>
                    <ion-button fill="clear" size="small" color="danger" @click="handleDeletePayment(item.id)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
                  </div>
                </div>
              </div>
              <p v-else class="mobile-note">{{ $t('iosStage5.roomStatus.noPaymentRecords') }}</p>
            </article>

            <article class="detail-section detail-reminder-card">
              <div class="detail-reminder-card__header">
                <div>
                  <h3>{{ $t('iosStage5.roomStatus.reservationAlerts') }}</h3>
                  <p v-if="orderReminderNotice">{{ orderReminderNotice }}</p>
                  <p v-else-if="orderReminderCount > 0" class="detail-reminder-card__description">
                    <span>{{ $t('stage5VisibleText.201') }}</span>
                    <span class="detail-reminder-card__count">{{ orderReminderCount }}</span>
                    <span>{{ $t('iosStage5.roomStatus.unreadAlerts') }}</span>
                  </p>
                  <p v-else>{{ $t('iosStage5.roomStatus.noAlerts') }}</p>
                </div>
              </div>

              <div class="detail-reminder-card__actions">
                <ion-button fill="outline" size="small" @click="openOrderNotifications">{{ $t('iosStage5.roomStatus.viewAlerts') }}</ion-button>
              </div>
            </article>
          </div>

          <div v-else-if="activeSegment === 'logs'" class="reservation-detail-content__body">
            <article class="detail-section detail-section--timeline">
              <div v-if="logs.length > 0" class="detail-log-list">
                <div v-for="item in logs" :key="item.id" class="log-item">
                  <div class="log-item__head">
                    <strong>{{ item.action }}</strong>
                    <span class="mobile-note">{{ item.timestamp }}</span>
                  </div>
                  <p class="mobile-note">{{ $t('stage5DynamicUi.117') }}{{ item.operator }}</p>
                  <p v-if="item.content" class="mobile-note">{{ item.content }}</p>
                  <div v-if="item.details && item.details.length > 0" class="log-item__details">
                    <p v-for="detail in item.details" :key="`${item.id}-${detail.label}`" class="mobile-note">
                      {{ detail.label }}：{{ detail.value }}
                    </p>
                  </div>
                </div>
              </div>
              <p v-else class="mobile-note">{{ $t('iosStage5.roomStatus.noLogs') }}</p>
            </article>
          </div>

          <div v-else class="reservation-detail-content__body">
            <article class="detail-section">
              <div class="detail-section__head">
                <h2 class="mobile-section-title">{{ $t('roomStatus.detail.tabs.channel') }}</h2>
              </div>

              <div class="detail-definition-list">
                <div class="detail-definition-list__row">
                  <span class="detail-definition-list__label">{{ $t('iosStage5.roomStatus.channelName') }}</span>
                  <strong class="detail-definition-list__value">{{ channelInfo?.channelName || reservation.channelName || $t('roomStatus.common.defaultChannel') }}</strong>
                </div>
                <div v-if="linkedMessageThread" class="detail-definition-list__row">
                  <span class="detail-definition-list__label">{{ $t('iosStage5.roomStatus.linkedConversation') }}</span>
                  <div class="detail-linked-thread">
                    <strong class="detail-definition-list__value">{{ linkedMessageThreadLabel }}</strong>
                    <p class="mobile-note">{{ linkedMessageThreadMeta }}</p>
                  </div>
                </div>
                <div class="detail-definition-list__row">
                  <span class="detail-definition-list__label">{{ $t('roomStatus.detail.channelInfo.channelOrderNumber') }}</span>
                  <strong class="detail-definition-list__value">{{ channelInfo?.channelOrderNumber || reservation.channelOrderNumber || $t('roomStatus.common.none') }}</strong>
                </div>
                <div class="detail-definition-list__row">
                  <span class="detail-definition-list__label">{{ $t('accommodation.roomPriceBulk.table.pricePlan') }}</span>
                  <strong class="detail-definition-list__value">{{ channelInfo?.pricePlan || reservation.pricePlan || $t('roomStatus.common.none') }}</strong>
                </div>
                <div class="detail-definition-list__row">
                  <span class="detail-definition-list__label">{{ $t('roomStatus.common.paymentMethod') }}</span>
                  <strong class="detail-definition-list__value">{{ channelInfo?.paymentMethod || reservation.paymentMethod || $t('iosStage5.roomStatus.recorded') }}</strong>
                </div>
                <div class="detail-definition-list__row">
                  <span class="detail-definition-list__label">{{ $t('roomStatus.detail.channelInfo.specialRequests') }}</span>
                  <strong class="detail-definition-list__value">{{ channelInfo?.specialRequests || reservation.notes || $t('roomStatus.common.none') }}</strong>
                </div>
              </div>
            </article>
          </div>
        </section>
      </div>
    </ion-content>

    <BookingFormModal
      :is-open="showBookingModal"
      :mode="'edit'"
      :channels="roomStatusStore.channels"
      :room="roomContext"
      :reservation="reservation"
      :submitting="actionLoading"
      @dismiss="showBookingModal = false"
      @submit="handleUpdateReservation"
    />

    <CancelReservationModal
      :is-open="showCancelModal"
      :reservation="reservation"
      :submitting="actionLoading"
      @dismiss="showCancelModal = false"
      @submit="handleCancelReservation"
    />
  </ion-page>
</template>

<script setup lang="ts">
import {
  actionSheetController,
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonLabel,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSegment,
  IonSegmentButton,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import BookingFormModal, { type BookingFormSubmitPayload } from '@/components/room-status/BookingFormModal.vue'
import CancelReservationModal from '@/components/room-status/CancelReservationModal.vue'
import {
  formatAmount,
  getReservationStatusColor,
  getReservationStatusText,
  getOrderTabLabel,
  type OrderTabValue,
} from '@/components/order/orderUtils'
import {
  checkCanMoveToOrderBox,
  getOrderBoxList,
  moveOutOrderBox,
  moveToOrderBox,
  type OrderBoxItem,
} from '@/api/orderBox'
import {
  cancelReservation,
  checkInReservation,
  checkOutReservation,
  getReservationById,
  getReservationChannelInfo,
  getReservationLogs,
  updateReservation,
  type ReservationChannelInfoDTO,
  type ReservationDTO,
} from '@/api/reservation'
import { getMessageThreads } from '@/api/message'
import type { MessageThreadDTO } from '@/types/message'
import {
  deleteConsumption,
  getConsumptionsByReservationId,
  getTotalConsumption,
  type ConsumptionDTO,
} from '@/api/consumption'
import {
  deletePayment,
  getPaymentsByReservationId,
  getTotalPayment,
  type PaymentDTO,
} from '@/api/payment'
import { getUnreadNotificationCountByType } from '@/api/notification'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { useRoomStatusStore } from '@/stores/roomStatus'
import { useStoreStore } from '@/stores/store'
import { ROUTE_PATHS, buildMessageDetailPath } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const notificationCenterStore = useNotificationCenterStore()
const roomStatusStore = useRoomStatusStore()
const storeStore = useStoreStore()
const userStore = useUserStore()

const ORDER_NOTIFICATION_TYPE = 'ORDER'

const loading = ref(false)
const actionLoading = ref(false)
const activeSegment = ref('detail')
const reservation = ref<ReservationDTO | null>(null)
const channelInfo = ref<ReservationChannelInfoDTO | null>(null)
const logs = ref<any[]>([])
const consumptions = ref<ConsumptionDTO[]>([])
const payments = ref<PaymentDTO[]>([])
const totalConsumption = ref(0)
const totalPayment = ref(0)
const orderBoxItem = ref<OrderBoxItem | null>(null)
const orderReminderCount = ref(0)
const orderReminderNotice = ref('')
const linkedMessageThread = ref<MessageThreadDTO | null>(null)
const showBookingModal = ref(false)
const showCancelModal = ref(false)

let detailLoadToken = 0

const reservationId = computed(() => Number(route.params.reservationId || 0))
const VALID_ORDER_TABS: OrderTabValue[] = [
  'all',
  'today-checkin',
  'today-checkout',
  'today-new',
  'unassigned',
  'assigned',
  'pending',
  'order-box',
  'deleted-rooms',
]

const defaultBackHref = computed(() => {
  const routeBackHref = route.query.defaultHref
  if (typeof routeBackHref === 'string' && routeBackHref) {
    return routeBackHref
  }

  if (route.path.includes('/tabs/orders/')) {
    return ROUTE_PATHS.orders
  }

  return ROUTE_PATHS.rooms
})

const pageTitle = computed(() => {
  const titleKey = route.path.includes('/tabs/orders/')
    ? 'routes.OrderReservationDetail'
    : 'routes.RoomReservationDetail'
  if (!reservation.value) {
    return t(titleKey)
  }
  return `${reservation.value.guestName} · ${t(titleKey)}`
})

const statusText = computed(() => getReservationStatusText(reservation.value?.status))
const statusColor = computed(() => getReservationStatusColor(reservation.value?.status))
const reservationNotesText = computed(() => {
  const notes = reservation.value?.notes?.trim()
  if (notes) {
    return notes
  }

  const specialRequests = channelInfo.value?.specialRequests?.trim()
  if (specialRequests) {
    return specialRequests
  }

  return ''
})
const sourceOrderTab = computed<OrderTabValue | null>(() => {
  const routeTab = route.query.fromTab
  if (typeof routeTab !== 'string') {
    return null
  }

  for (const tab of VALID_ORDER_TABS) {
    if (tab === routeTab) {
      return tab
    }
  }

  return null
})
const isOrderContext = computed(() => {
  if (sourceOrderTab.value) {
    return true
  }
  return defaultBackHref.value.includes(ROUTE_PATHS.orders)
})
const linkedMessageThreadLabel = computed(() => {
  if (!linkedMessageThread.value) {
    return ''
  }

  return (
    linkedMessageThread.value.guestName ||
    linkedMessageThread.value.listingName ||
    linkedMessageThread.value.channelName ||
    `${t('stage5Final.roomStatus.conversation')} #${linkedMessageThread.value.id}`
  )
})
const linkedMessageThreadMeta = computed(() => {
  if (!linkedMessageThread.value) {
    return ''
  }

  const parts = [
    linkedMessageThread.value.channelName,
    linkedMessageThread.value.closed ? t('messages.tabs.closed') : t('iosStage5.cleaning.status.inProgress'),
  ]

  if (linkedMessageThread.value.unreadCount > 0) {
    parts.push(`${t('messages.tabs.unread')} ${linkedMessageThread.value.unreadCount} ${t('stage5DynamicUi.125')}`)
  }

  return parts.filter(Boolean).join(' · ')
})

const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))
const totalAmountText = computed(() =>
  formatAmount(reservation.value?.totalAmount, currentCurrency.value, currentMoneyContext.value),
)
const totalConsumptionText = computed(() =>
  formatAmount(totalConsumption.value, currentCurrency.value, currentMoneyContext.value),
)
const totalPaymentText = computed(() =>
  formatAmount(totalPayment.value, currentCurrency.value, currentMoneyContext.value),
)
const remainingPayment = computed(() => {
  return Number(reservation.value?.totalAmount ?? 0) - Number(totalPayment.value || 0) - Number(totalConsumption.value || 0)
})

const remainingPaymentText = computed(() =>
  formatAmount(remainingPayment.value, currentCurrency.value, currentMoneyContext.value),
)
const remainingPaymentLabel = computed(() => {
  if (remainingPayment.value > 0) {
    return t('roomStatus.detail.remainingPayment')
  }
  if (remainingPayment.value < 0) {
    return t('iosStage5.roomStatus.overpaid')
  }
  return t('iosStage5.roomStatus.accountBalance')
})

const remainingPaymentClass = computed(() => {
  if (remainingPayment.value > 0) {
    return 'is-danger'
  }
  if (remainingPayment.value < 0) {
    return 'is-success'
  }
  return ''
})

const canCheckIn = computed(() => {
  const status = (reservation.value?.status || '').toUpperCase()
  return status === 'CONFIRMED' || status === 'REQUESTED' || status === 'NEW'
})

const canCheckOut = computed(() => {
  const status = (reservation.value?.status || '').toUpperCase()
  return status === 'CHECKED_IN'
})

const canEditOrder = computed(() => {
  const status = (reservation.value?.status || '').toUpperCase()
  return status !== 'CANCELLED'
})

const roomContext = computed(() => {
  if (!reservation.value?.roomId) {
    return null
  }
  return roomStatusStore.getRoomListItemById(reservation.value.roomId, roomStatusStore.selectedDate)
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function escapeAlertMessage(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/\n/g, '<br />')
}

function normalizeMessageMatchValue(value?: string | null) {
  const normalizedValue = String(value || '')
    .trim()
    .toLowerCase()

  return normalizedValue || null
}

function buildReservationMessageKeys(reservationItem: ReservationDTO) {
  const keys = new Set<string>()
  const rawValues = [
    reservationItem.channelOrderNumber,
    reservationItem.suReservationId,
    reservationItem.reservationNotifId,
    reservationItem.orderNumber,
  ]

  for (const rawValue of rawValues) {
    const normalizedValue = normalizeMessageMatchValue(rawValue)
    if (normalizedValue) {
      keys.add(normalizedValue)
    }
  }

  return Array.from(keys)
}

function normalizeMessageChannelName(value?: string | null) {
  const normalizedValue = String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[\s._-]+/g, '')

  return normalizedValue || null
}

function resolveLinkedMessageThread(
  threads: MessageThreadDTO[],
  reservationItem: ReservationDTO,
) {
  const reservationKeys = buildReservationMessageKeys(reservationItem)
  if (reservationKeys.length === 0) {
    return null
  }

  const reservationChannelName = normalizeMessageChannelName(reservationItem.channelName)
  let bookingIdMatch: MessageThreadDTO | null = null
  let threadIdMatch: MessageThreadDTO | null = null
  let threadIdChannelMatch: MessageThreadDTO | null = null

  for (const thread of threads) {
    const bookingId = normalizeMessageMatchValue(thread.bookingId)
    const externalThreadId = normalizeMessageMatchValue(thread.threadId)
    const channelNameMatches =
      Boolean(reservationChannelName) &&
      normalizeMessageChannelName(thread.channelName) === reservationChannelName
    const hasBookingIdMatch = Boolean(bookingId && reservationKeys.includes(bookingId))
    const hasThreadIdMatch = Boolean(externalThreadId && reservationKeys.includes(externalThreadId))

    if (!hasBookingIdMatch && !hasThreadIdMatch) {
      continue
    }

    if (hasBookingIdMatch && channelNameMatches) {
      return thread
    }

    if (hasThreadIdMatch && channelNameMatches && !threadIdChannelMatch) {
      threadIdChannelMatch = thread
    }

    if (hasBookingIdMatch && !bookingIdMatch) {
      bookingIdMatch = thread
    }

    if (hasThreadIdMatch && !threadIdMatch) {
      threadIdMatch = thread
    }
  }

  if (bookingIdMatch) {
    return bookingIdMatch
  }

  if (threadIdChannelMatch) {
    return threadIdChannelMatch
  }

  return threadIdMatch
}

async function ensureUserId() {
  if (userStore.currentUser?.id) {
    return userStore.currentUser.id
  }

  const user = await userStore.fetchCurrentUser(true)
  if (!user?.id) {
    throw new Error(t('stage5Final.roomStatus.currentUserUnavailable'))
  }

  return user.id
}

async function loadOrderReminderCount() {
  orderReminderNotice.value = ''

  try {
    const userId = await ensureUserId()
    const response = await getUnreadNotificationCountByType(userId, ORDER_NOTIFICATION_TYPE)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.loadFailed'))
    }

    orderReminderCount.value = Number(response.data || 0)
  } catch {
    orderReminderCount.value = 0
    orderReminderNotice.value = t('stage5Final.roomStatus.reminderLoadFailed')
  }
}

async function openOrderNotifications() {
  await router.push(ROUTE_PATHS.orderNotifications)
}

async function openLinkedMessageThread() {
  if (!linkedMessageThread.value) {
    return
  }

  await router.push({
    path: buildMessageDetailPath(linkedMessageThread.value.id),
    query: {
      defaultHref: route.fullPath,
    },
  })
}

function buildReservationMessageQuery(reservationItem: ReservationDTO) {
  const query: Record<string, string> = {
    defaultHref: route.fullPath,
  }

  if (reservationItem.id) {
    query.reservationId = String(reservationItem.id)
  }

  const orderNumber = reservationItem.orderNumber?.trim()
  if (orderNumber) {
    query.orderNumber = orderNumber
  }

  const channelOrderNumber = reservationItem.channelOrderNumber?.trim()
  if (channelOrderNumber) {
    query.channelOrderNumber = channelOrderNumber
  }

  const suReservationId = reservationItem.suReservationId?.trim()
  if (suReservationId) {
    query.suReservationId = suReservationId
  }

  const reservationNotifId = reservationItem.reservationNotifId?.trim()
  if (reservationNotifId) {
    query.reservationNotifId = reservationNotifId
  }

  const guestName = reservationItem.guestName?.trim()
  if (guestName) {
    query.guestName = guestName
  }

  return query
}

async function openReservationMessages() {
  if (linkedMessageThread.value) {
    await openLinkedMessageThread()
    return
  }

  if (!reservation.value) {
    showWarningToast(t('iosStage5.roomStatus.orderDataLoading'))
    return
  }

  await router.push({
    path: ROUTE_PATHS.messages,
    query: buildReservationMessageQuery(reservation.value),
  })
}

function isCurrentDetailRequest(requestToken: number, currentReservationId: number) {
  return requestToken === detailLoadToken && reservationId.value === currentReservationId
}

function syncLinkedMessageThreadFromThreads(
  threads: MessageThreadDTO[],
  reservationItem: ReservationDTO,
  requestToken: number,
) {
  if (!isCurrentDetailRequest(requestToken, reservationItem.id)) {
    return
  }

  linkedMessageThread.value = resolveLinkedMessageThread(threads, reservationItem)
}

async function refreshLinkedMessageThread(
  reservationItem: ReservationDTO,
  requestToken: number,
  prefetchedThreads?: Promise<MessageThreadDTO[] | null>,
) {
  syncLinkedMessageThreadFromThreads(notificationCenterStore.messageThreads, reservationItem, requestToken)

  const nextThreads =
    (await
      (prefetchedThreads ||
        getMessageThreads()
          .then((response) => {
            if (!response.success || !response.data) {
              return null
            }

            return response.data
          })
          .catch(() => null))) || null

  if (!nextThreads) {
    return
  }

  notificationCenterStore.syncMessageThreads(nextThreads)
  syncLinkedMessageThreadFromThreads(nextThreads, reservationItem, requestToken)
}

async function confirmAction(header: string, message: string, confirmText: string, destructive = false) {
  const alert = await alertController.create({
    header,
    message,
    buttons: [
      {
        text: t('accommodation.common.cancel'),
        role: 'cancel',
      },
      {
        text: confirmText,
        role: destructive ? 'destructive' : 'confirm',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  if (destructive) {
    return result.role === 'destructive'
  }
  return result.role === 'confirm'
}

async function loadDetail() {
  if (!reservationId.value) {
    detailLoadToken += 1
    orderBoxItem.value = null
    orderReminderCount.value = 0
    orderReminderNotice.value = ''
    linkedMessageThread.value = null
    return
  }

  const currentReservationId = reservationId.value
  const requestToken = ++detailLoadToken
  const prefetchedThreads = getMessageThreads()
    .then((response) => {
      if (!response.success || !response.data) {
        return null
      }

      return response.data
    })
    .catch(() => null)

  if (reservation.value?.id !== currentReservationId) {
    linkedMessageThread.value = null
  }

  loading.value = true
  try {
    const reservationResponse = await getReservationById(currentReservationId)
    if (!isCurrentDetailRequest(requestToken, currentReservationId)) {
      return
    }
    if (!reservationResponse.success || !reservationResponse.data) {
      throw new Error(reservationResponse.message || t('stage5Pattern.loadFailed'))
    }
    const currentReservation = reservationResponse.data
    reservation.value = currentReservation
    syncLinkedMessageThreadFromThreads(notificationCenterStore.messageThreads, currentReservation, requestToken)
    void refreshLinkedMessageThread(currentReservation, requestToken, prefetchedThreads)

    const [results] = await Promise.all([
      Promise.allSettled([
        getReservationChannelInfo(currentReservationId),
        getReservationLogs(currentReservationId),
        getConsumptionsByReservationId(currentReservationId),
        getPaymentsByReservationId(currentReservationId),
        getTotalConsumption(currentReservationId),
        getTotalPayment(currentReservationId),
        getOrderBoxList(),
      ]),
      loadOrderReminderCount(),
    ])

    if (!isCurrentDetailRequest(requestToken, currentReservationId)) {
      return
    }

    const [
      channelResult,
      logResult,
      consumptionResult,
      paymentResult,
      totalConsumptionResult,
      totalPaymentResult,
      orderBoxResult,
    ] = results

    if (channelResult.status === 'fulfilled' && channelResult.value.success) {
      channelInfo.value = channelResult.value.data || null
    }
    if (logResult.status === 'fulfilled' && logResult.value.success) {
      logs.value = logResult.value.data || []
    }
    if (consumptionResult.status === 'fulfilled' && consumptionResult.value.success) {
      consumptions.value = consumptionResult.value.data || []
    }
    if (paymentResult.status === 'fulfilled' && paymentResult.value.success) {
      payments.value = paymentResult.value.data || []
    }
    if (totalConsumptionResult.status === 'fulfilled' && totalConsumptionResult.value.success) {
      totalConsumption.value = Number(totalConsumptionResult.value.data || 0)
    }
    if (totalPaymentResult.status === 'fulfilled' && totalPaymentResult.value.success) {
      totalPayment.value = Number(totalPaymentResult.value.data || 0)
    }
    if (orderBoxResult.status === 'fulfilled' && orderBoxResult.value.success) {
      orderBoxItem.value =
        orderBoxResult.value.data.find((item) => item.reservation.id === currentReservationId) || null
    } else {
      orderBoxItem.value = null
    }
  } finally {
    if (requestToken === detailLoadToken) {
      loading.value = false
    }
  }
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadDetail()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('iosStage5.roomStatus.orderRefreshFailed')))
    }
  } finally {
    event.detail.complete()
  }
}

async function handleCheckIn() {
  if (!reservation.value) {
    return
  }
  actionLoading.value = true
  try {
    const response = await checkInReservation(reservation.value.id)
    if (!response.success) {
      throw new Error(response.message || t('roomStatus.messages.checkInFailed'))
    }
    showSuccessToast(t('order.mobile.messages.checkInSuccess'))
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('roomStatus.messages.checkInFailed')))
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleCheckOut() {
  if (!reservation.value) {
    return
  }
  actionLoading.value = true
  try {
    const response = await checkOutReservation(reservation.value.id)
    if (!response.success) {
      throw new Error(response.message || t('roomStatus.messages.checkOutFailed'))
    }
    showSuccessToast(t('order.mobile.messages.checkOutSuccess'))
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('roomStatus.messages.checkOutFailed')))
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleUpdateReservation(payload: BookingFormSubmitPayload) {
  if (!reservation.value) {
    return
  }
  if (!canEditOrder.value) {
    showWarningToast(t('iosStage5.roomStatus.orderCannotEdit'))
    showBookingModal.value = false
    return
  }
  actionLoading.value = true
  try {
    const response = await updateReservation(reservation.value.id, {
      guestName: payload.guestName,
      guestPhone: payload.guestPhone,
      roomId: reservation.value.roomId || 0,
      channelId: payload.channelId,
      checkInDate: payload.checkInDate,
      checkOutDate: payload.checkOutDate,
      adults: payload.adults,
      children: payload.children,
      totalAmount: payload.totalAmount,
      channelOrderNumber: reservation.value.channelOrderNumber,
      notes: payload.notes,
    })
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.updateFailed'))
    }
    showSuccessToast(t('iosStage5.roomStatus.orderUpdated'))
    showBookingModal.value = false
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.updateFailed')))
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleCancelReservation() {
  if (!reservation.value) {
    return
  }
  actionLoading.value = true
  try {
    const response = await cancelReservation(reservation.value.id)
    if (!response.success) {
      throw new Error(response.message || t('roomStatus.cancelReservation.messages.failed'))
    }
    showSuccessToast(t('order.mobile.messages.cancelSuccess'))
    showCancelModal.value = false
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('roomStatus.cancelReservation.messages.failed')))
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleMoveToOrderBox() {
  if (!reservation.value) {
    return
  }

  try {
    const checkResponse = await checkCanMoveToOrderBox(reservation.value.id)
    if (!checkResponse.success || !checkResponse.data) {
      throw new Error(checkResponse.message || t('order.mobile.messages.eligibilityFailed'))
    }

    if (!checkResponse.data.canMove) {
      showWarningToast(checkResponse.data.reason || t('roomStatus.detail.messages.moveToOrderBoxOnlyConfirmed'))
      return
    }

    const confirmed = await confirmAction(
      t('roomStatus.common.moveToOrderBox'),
      t('order.mobile.confirmMoveIn'),
      t('order.mobile.actions.confirmMoveIn'),
    )
    if (!confirmed) {
      return
    }

    actionLoading.value = true
    const response = await moveToOrderBox({ reservationId: reservation.value.id })
    if (!response.success) {
      throw new Error(response.message || t('roomStatus.detail.messages.moveToOrderBoxFailed'))
    }

    showSuccessToast(t('roomStatus.detail.messages.moveToOrderBoxSuccess'))
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('roomStatus.detail.messages.moveToOrderBoxFailed')))
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleMoveOutOrderBox() {
  if (!reservation.value || !orderBoxItem.value) {
    showWarningToast(t('stage5Pattern.unavailable'))
    return
  }

  const confirmed = await confirmAction(
    t('order.mobile.actions.moveOut'),
    t('order.mobile.confirmMoveOut'),
    t('order.mobile.actions.confirmMoveOut'),
    true,
  )
  if (!confirmed) {
    return
  }

  actionLoading.value = true
  try {
    const response = await moveOutOrderBox({ orderBoxItemId: orderBoxItem.value.id })
    if (!response.success) {
      throw new Error(response.message || t('order.mobile.messages.moveOutFailed'))
    }

    showSuccessToast(t('order.mobile.messages.moveOutSuccess'))
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('order.mobile.messages.moveOutFailed')))
    }
  } finally {
    actionLoading.value = false
  }
}

async function confirmDelete(title: string) {
  const alert = await alertController.create({
    header: title,
    message: t('stage5Final.roomStatus.deleteIrreversible'),
    buttons: [
      {
        text: t('accommodation.common.cancel'),
        role: 'cancel',
      },
      {
        text: t('roomStatus.roomLock.actions.delete'),
        role: 'destructive',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function handleDeleteConsumption(consumptionId?: number) {
  if (!consumptionId) {
    return
  }
  const confirmed = await confirmDelete(t('stage5Final.roomStatus.deleteConsumption'))
  if (!confirmed) {
    return
  }

  actionLoading.value = true
  try {
    const response = await deleteConsumption(consumptionId)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.deleteFailed'))
    }
    showSuccessToast(t('stage5Pattern.deleteCompleted'))
    await loadDetail()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.deleteFailed')))
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleDeletePayment(paymentId?: number) {
  if (!paymentId) {
    return
  }
  const confirmed = await confirmDelete(t('stage5Final.roomStatus.deletePayment'))
  if (!confirmed) {
    return
  }

  actionLoading.value = true
  try {
    const response = await deletePayment(paymentId)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.deleteFailed'))
    }
    showSuccessToast(t('stage5Pattern.deleteCompleted'))
    await loadDetail()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.deleteFailed')))
    }
  } finally {
    actionLoading.value = false
  }
}

async function openRoomDetail() {
  if (!reservation.value?.roomId) {
    return
  }
  await router.push({
    name: 'RoomStatusDetail',
    params: { roomId: reservation.value.roomId },
    query: { date: reservation.value.checkInDate || roomStatusStore.selectedDate },
  })
}

function handlePrint() {
  if (typeof window.print !== 'function') {
    showWarningToast(t('iosStage5.roomStatus.printUnavailable'))
    return
  }

  window.print()
}

async function presentMoreActions() {
  if (!reservation.value) {
    return
  }

  const buttons: Array<Record<string, unknown>> = [
    {
      text: t('roomStatus.common.print'),
      handler: () => {
        handlePrint()
      },
    },
    {
      text: t('order.mobile.actions.viewGuestNotes'),
      handler: () => {
        void presentReservationNotes()
      },
    },
  ]

  if (isOrderContext.value || orderBoxItem.value) {
    if (orderBoxItem.value) {
      buttons.push({
        text: t('order.mobile.actions.moveOut'),
        role: 'destructive',
        handler: () => {
          void handleMoveOutOrderBox()
        },
      })
    } else {
      buttons.push({
        text: t('roomStatus.common.moveToOrderBox'),
        handler: () => {
          void handleMoveToOrderBox()
        },
      })
    }
  }

  if (reservation.value.roomId) {
    buttons.push({
      text: t('stage5Final.roomStatus.viewRoom'),
      handler: () => {
        void openRoomDetail()
      },
    })
  }

  if (canCancelOrder.value) {
    buttons.push({
      text: t('order.mobile.actions.cancelOrder'),
      role: 'destructive',
      handler: () => {
        showCancelModal.value = true
      },
    })
  }

  buttons.push({
    text: t('accommodation.common.cancel'),
    role: 'cancel',
  })

  const actionSheet = await actionSheetController.create({
    header: reservation.value.guestName || t('iosStage5.roomStatus.orderMoreActions'),
    subHeader: reservation.value.orderNumber,
    buttons,
  })

  await actionSheet.present()
}

async function presentReservationNotes() {
  const notesText = reservationNotesText.value || t('order.mobile.notesEmpty')
  const alert = await alertController.create({
    header: t('order.mobile.actions.guestNotes'),
    message: escapeAlertMessage(notesText),
    buttons: [t('order.mobile.understood')],
  })

  await alert.present()
}

const canCancelOrder = computed(() => {
  const status = (reservation.value?.status || '').toUpperCase()
  return status !== 'CANCELLED' && status !== 'CHECKED_OUT'
})

onMounted(async () => {
  if (roomStatusStore.calendarRooms.length === 0) {
    try {
      await roomStatusStore.initialize(true)
    } catch {
      // ignore here, detail page can still load order data independently
    }
  }

  try {
    await loadDetail()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.loadFailed')))
    }
  }
})
</script>

<style scoped>
.reservation-detail-page {
  --background: #f1f7ff;
  --padding-top: 14px;
  --padding-bottom: calc(32px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.app-page-header__toolbar {
  --min-height: 52px;
  --padding-start: 4px;
  --padding-end: 8px;
}

.app-page-header__title {
  color: var(--ios-pms-header-title-color);
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 0;
}

.app-page-header__back-btn {
  --color: var(--ios-pms-header-control-color);
  font-size: 16px;
  font-weight: 400;
}

.reservation-detail-page .mobile-stack {
  gap: 12px;
}

.reservation-detail-hero {
  margin-bottom: 12px;
  padding: 18px;
  border: 1px solid rgba(198, 207, 218, 0.2);
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(58, 78, 104, 0.08);
}

.reservation-detail-hero::before {
  display: none;
}

.reservation-detail-hero__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.reservation-detail-hero__eyebrow {
  color: #303236;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
}

.reservation-detail-hero__status {
  flex-shrink: 0;
  min-height: 28px;
  padding: 0 12px;
  border: 0;
  background: #f0f5ff;
  color: #2874ff;
  font-size: 13px;
  font-weight: 400;
}

.reservation-detail-hero__status.is-danger {
  background: #ffe9e9;
  color: #ff2d2d;
}

.reservation-detail-hero__status.is-success {
  background: #eaf8f2;
  color: #4fbb91;
}

.reservation-detail-hero__status.is-warning {
  background: #fff4df;
  color: #d68a24;
}

.reservation-detail-hero__status.is-medium {
  background: #f0f1f3;
  color: #7b7f86;
}

.reservation-detail-hero__headline-row {
  display: block;
  margin-top: 14px;
}

.reservation-detail-hero__headline-copy {
  min-width: 0;
  width: 78%;
  max-width: 280px;
}

.reservation-detail-hero .mobile-title {
  display: block;
  max-width: 100%;
  overflow: hidden;
  color: #292b2f;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reservation-detail-hero__orderline {
  display: block;
  margin-top: 16px;
  color: #a1a3a7;
  font-size: 13px;
  line-height: 1.5;
}

.reservation-detail-hero__orderline span {
  display: inline;
  overflow-wrap: anywhere;
}

.reservation-detail-hero__orderline span + span {
  margin-left: 4px;
}

.reservation-detail-hero__message-entry {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.reservation-detail-hero__message-button {
  margin: 0;
  --border-radius: 7px;
  --border-color: #d8e6ff;
  --background: #f2f6ff;
  --background-hover: #ebf2ff;
  --background-activated: #e5eeff;
  --color: #2d74ff;
  --padding-start: 11px;
  --padding-end: 11px;
  min-height: 30px;
  color: #2d74ff;
  font-size: 13px;
  font-weight: 400;
}

.reservation-detail-hero__meta-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(104px, 0.75fr);
  column-gap: 0;
  row-gap: 0;
  margin-top: 16px;
}

.reservation-detail-hero__meta-item,
.detail-summary-card__hero-copy,
.detail-summary-card__hero-side,
.detail-summary-card__metric,
.detail-key-item {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.reservation-detail-hero__meta-item {
  align-content: start;
  padding: 14px 6px 16px;
  border-top: 1px solid #dfe1e5;
}

.reservation-detail-hero__meta-item:nth-child(odd) {
  padding-right: 18px;
}

.reservation-detail-hero__meta-item:nth-child(even) {
  padding-left: 18px;
}

.reservation-detail-hero__meta-item > span,
.detail-summary-card__hero span,
.detail-summary-card__metric span,
.detail-key-item span,
.detail-note-row span,
.detail-definition-list__label {
  color: #777a80;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.4;
}

.reservation-detail-hero__meta-item strong,
.detail-summary-card__hero-side strong,
.detail-summary-card__metric strong,
.detail-key-item strong,
.detail-definition-list__value {
  color: #35373b;
  font-size: 16px;
  font-weight: 400;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.reservation-detail-hero__guest-count {
  display: grid;
  gap: 0;
}

.reservation-detail-hero__guest-count span {
  color: inherit;
  font-size: inherit;
  font-weight: inherit;
  line-height: inherit;
}

.reservation-detail-hero__chips {
  margin-top: 0;
}

.reservation-detail-hero__chips .mobile-chip {
  min-height: 28px;
  padding: 0 10px;
  border: 0;
  background: #f1f5ff;
  color: #3277ff;
  font-size: 13px;
  font-weight: 400;
}

.detail-reminder-card__header > div,
.detail-list__main,
.detail-summary-card__hero-copy {
  min-width: 0;
}

.reservation-detail-panel {
  padding: 16px;
  border: 1px solid rgba(198, 207, 218, 0.18);
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(58, 78, 104, 0.08);
}

.detail-actions__busy {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.detail-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.detail-actions ion-button {
  min-height: 48px;
  margin: 0;
  font-size: 16px;
  font-weight: 400;
}

.detail-actions__primary {
  grid-column: 1 / -1;
  --border-radius: 10px;
  --box-shadow: none;
}

.detail-actions__primary[color='success'] {
  --background: #58bb92;
  --background-hover: #50b189;
  --background-activated: #49a781;
  --color: #ffffff;
}

.detail-actions__primary[color='warning'] {
  --background: #e4a144;
  --background-hover: #d9993f;
  --background-activated: #cf9039;
  --color: #ffffff;
}

.detail-actions__secondary {
  --border-radius: 10px;
  --border-color: #d6d8dc;
  --background: #ffffff;
  --background-hover: #f7f7f8;
  --background-activated: #f2f2f3;
  --color: #383a3e;
  --box-shadow: none;
}

.detail-actions__secondary--solo {
  grid-column: 1 / -1;
}

.reservation-detail-content {
  overflow: visible;
  margin-top: 20px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.reservation-detail-content__segment {
  min-height: 42px;
  margin: 0 0 10px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: none;
}

.reservation-detail-content__segment ion-segment-button {
  --border-radius: 999px;
  --color: #111111;
  --color-checked: #ffffff;
  --indicator-color: #303030;
  --indicator-box-shadow: none;
  min-height: 42px;
  font-size: 16px;
  font-weight: 500;
  text-transform: none;
}

.reservation-detail-content__segment ion-segment-button::part(indicator-background) {
  border-radius: 999px;
}

.reservation-detail-content__body {
  display: grid;
  gap: 0;
  padding: 18px 18px 22px;
  border: 1px solid rgba(198, 207, 218, 0.18);
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(58, 78, 104, 0.08);
}

.reservation-detail-content__body--detail {
  padding-top: 18px;
}

.detail-section {
  min-width: 0;
  padding: 18px 0;
}

.detail-section:first-child {
  padding-top: 2px;
}

.detail-section:last-child {
  padding-bottom: 0;
}

.detail-section + .detail-section {
  border-top: 1px solid #dfe1e5;
}

.detail-summary-card,
.detail-list,
.detail-log-list,
.log-item__details,
.detail-reminder-card,
.detail-definition-list,
.detail-fact-list,
.detail-summary-card__metrics {
  display: grid;
  gap: 0;
}

.detail-section__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.detail-section__head h2,
.detail-section__head h3,
.detail-list__main strong,
.log-item strong {
  margin: 0;
}

.detail-section__head h2,
.detail-section__head h3 {
  color: #111111;
  font-size: 20px;
  font-weight: 500;
  line-height: 1.35;
}

.detail-section__meta {
  flex-shrink: 0;
  color: #8c8f94;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.4;
  white-space: nowrap;
}

.detail-section--timeline {
  padding-top: 4px;
}

.detail-summary-card {
  gap: 0;
}

.detail-summary-card__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(112px, 0.75fr);
  align-items: end;
  gap: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid #dfe1e5;
}

.detail-summary-card__hero-side {
  justify-self: end;
  text-align: left;
}

.detail-summary-card__amount {
  color: #2e3034;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0;
  line-height: 1.15;
  overflow-wrap: anywhere;
}

.detail-summary-card__metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  padding-top: 16px;
}

.detail-summary-card__metric + .detail-summary-card__metric {
  padding-left: 18px;
  margin-left: 0;
  border-left: 0;
}

.detail-fact-list__row {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  align-items: start;
  gap: 12px;
  padding: 7px 0;
}

.detail-fact-list__row + .detail-fact-list__row {
  border-top: 0;
}

.detail-fact-list__row > span {
  color: #7b7d82;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.5;
}

.detail-fact-list__row strong {
  min-width: 0;
  color: #383a3e;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.detail-fact-list__row p {
  margin: 0;
  color: #777a80;
  font-size: 13px;
  line-height: 1.6;
}

.detail-list__item,
.detail-list__actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.detail-list__item,
.log-item,
.detail-definition-list__row {
  padding: 12px 0;
}

.detail-list__item + .detail-list__item,
.log-item + .log-item,
.detail-definition-list__row + .detail-definition-list__row {
  border-top: 1px solid #dfe1e5;
}

.detail-list__item {
  align-items: flex-start;
}

.detail-list__main strong {
  display: block;
  color: #383a3e;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.detail-list__main p,
.detail-list__meta,
.log-item p,
.log-item__details p,
.detail-reminder-card p {
  color: #777a80;
  font-size: 13px;
  line-height: 1.6;
}

.detail-list__main p,
.detail-list__meta {
  margin: 4px 0 0;
}

.detail-list__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0 10px;
}

.detail-list__actions {
  flex-shrink: 0;
  display: grid;
  justify-items: end;
  text-align: right;
}

.detail-list__amount {
  color: #303236;
  font-size: 15px;
  font-weight: 500;
}

.detail-record-section .detail-section__head {
  margin-bottom: 2px;
}

.detail-record-section .detail-section__head h3 {
  color: #7b7d82;
  font-size: 14px;
  font-weight: 400;
}

.detail-record-section > .mobile-note {
  margin: 0;
  color: #383a3e;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.5;
}

.detail-reminder-card {
  gap: 16px;
}

.detail-reminder-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-reminder-card__header h3 {
  margin: 0;
  color: #111111;
  font-size: 20px;
  font-weight: 500;
  line-height: 1.35;
}

.detail-reminder-card__header p {
  margin: 4px 0 0;
  color: #383a3e;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.detail-reminder-card__description {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

.detail-reminder-card__count {
  color: #ff2626;
  font-size: inherit;
  font-weight: 400;
  white-space: nowrap;
}

.detail-reminder-card__actions {
  display: flex;
  justify-content: flex-end;
}

.detail-reminder-card__actions ion-button {
  margin: 0;
  --border-radius: 7px;
  --border-color: #d8e6ff;
  --background: #f2f6ff;
  --background-hover: #ebf2ff;
  --background-activated: #e5eeff;
  --color: #2d74ff;
  min-height: 32px;
  font-size: 13px;
  font-weight: 400;
}

.detail-log-list {
  position: relative;
  padding-left: 20px;
}

.detail-log-list::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 8px;
  bottom: 8px;
  width: 1px;
  background: linear-gradient(180deg, rgba(52, 116, 246, 0.2), #dfe1e5);
}

.log-item {
  position: relative;
  padding: 0 0 0 18px;
}

.log-item + .log-item {
  margin-top: 14px;
  padding-top: 14px;
  border-top: none;
}

.log-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--ios-pms-primary);
  box-shadow: 0 0 0 4px rgba(115, 164, 255, 0.12);
}

.log-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.log-item__head strong {
  color: #303236;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.45;
}

.log-item__head .mobile-note {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border: 1px solid #e8e9ec;
  border-radius: 999px;
  background: #f8f9fb;
  flex-shrink: 0;
  white-space: nowrap;
  color: #8c8f94;
  font-size: 10px;
}

.log-item__details {
  padding-top: 8px;
  gap: 4px;
}

.detail-definition-list__row {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  align-items: start;
  gap: 12px;
}

.detail-linked-thread {
  min-width: 0;
}

.detail-linked-thread .mobile-note {
  margin: 4px 0 0;
  color: #777a80;
  font-size: 13px;
  line-height: 1.6;
}

.detail-definition-list__value {
  word-break: normal;
  overflow-wrap: anywhere;
}

.is-danger {
  color: #ff1717;
}

.is-success {
  color: #4fbb91;
}

@media (max-width: 339px) {
  .reservation-detail-content__body {
    padding-right: 16px;
    padding-left: 16px;
  }

  .detail-summary-card__hero {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .detail-summary-card__hero-side {
    justify-self: start;
  }

  .detail-list__item,
  .log-item__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-list__actions {
    justify-items: start;
    justify-content: flex-start;
    text-align: left;
  }

  .log-item {
    padding-left: 14px;
  }
}
</style>
