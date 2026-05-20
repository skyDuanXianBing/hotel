<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="defaultBackHref" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ pageTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page reservation-detail-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新订单" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero reservation-detail-hero" v-if="reservation">
        <div class="reservation-detail-hero__topline">
          <p class="mobile-note reservation-detail-hero__eyebrow">订单详情</p>
          <span class="mobile-chip reservation-detail-hero__status">{{ statusText }}</span>
        </div>

        <div class="reservation-detail-hero__headline-row">
          <div class="reservation-detail-hero__headline-copy">
            <h1 class="mobile-title">{{ reservation.guestName }}</h1>
            <p class="mobile-subtitle reservation-detail-hero__orderline">
              {{ reservation.orderNumber }} · {{ reservation.channelName || '自来客' }}
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
              消息
            </ion-button>
          </div>
        </div>

        <div class="reservation-detail-hero__meta-grid">
          <div class="reservation-detail-hero__meta-item">
            <span>入住</span>
            <strong>{{ reservation.checkInDate }}</strong>
          </div>
          <div class="reservation-detail-hero__meta-item">
            <span>离店</span>
            <strong>{{ reservation.checkOutDate }}</strong>
          </div>
          <div class="reservation-detail-hero__meta-item">
            <span>房间</span>
            <strong>{{ reservation.roomTypeName || '待排房' }} {{ reservation.roomNumber || '' }}</strong>
          </div>
          <div class="reservation-detail-hero__meta-item">
            <span>入住人数</span>
            <strong>{{ reservation.adults || 1 }} 成人 · {{ reservation.children || 0 }} 儿童</strong>
          </div>
        </div>

        <div v-if="sourceOrderTab || orderBoxItem" class="mobile-chip-row reservation-detail-hero__chips">
          <span v-if="sourceOrderTab" class="mobile-chip">来源：{{ getOrderTabLabel(sourceOrderTab) }}</span>
          <span v-if="orderBoxItem" class="mobile-chip">已在订单盒子</span>
        </div>
      </section>

      <div class="mobile-stack" v-if="reservation">
        <section class="mobile-card reservation-detail-panel">
          <div v-if="actionLoading" class="detail-actions__busy">
            <ion-spinner name="crescent" />
          </div>

          <div class="detail-actions">
            <ion-button v-if="canCheckIn" class="detail-actions__primary" color="success" @click="handleCheckIn">办理入住</ion-button>
            <ion-button v-else-if="canCheckOut" class="detail-actions__primary" color="warning" @click="handleCheckOut">办理退房</ion-button>
            <ion-button v-if="canEditOrder" fill="outline" class="detail-actions__secondary" @click="showBookingModal = true">修改订单</ion-button>
            <ion-button
              fill="outline"
              class="detail-actions__secondary"
              :class="{ 'detail-actions__secondary--solo': !canEditOrder && !canCheckIn && !canCheckOut }"
              @click="presentMoreActions"
            >
              更多操作
            </ion-button>
          </div>
        </section>

        <section class="mobile-card reservation-detail-content">
          <ion-segment v-model="activeSegment" class="reservation-detail-content__segment">
            <ion-segment-button value="detail">
              <ion-label>详情</ion-label>
            </ion-segment-button>
            <ion-segment-button value="logs">
              <ion-label>日志</ion-label>
            </ion-segment-button>
            <ion-segment-button value="channel">
              <ion-label>渠道</ion-label>
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
                  <span>订单金额</span>
                  <strong>{{ totalAmountText }}</strong>
                </div>
              </div>

              <div class="detail-summary-card__metrics">
                <div class="detail-summary-card__metric">
                  <span>已收款</span>
                  <strong>{{ totalPaymentText }}</strong>
                </div>
                <div class="detail-summary-card__metric">
                  <span>其他消费</span>
                  <strong>{{ totalConsumptionText }}</strong>
                </div>
              </div>
            </article>

            <article class="detail-section">
              <div class="detail-section__head">
                <h3>入住信息</h3>
              </div>

              <div class="detail-fact-list">
                <div class="detail-fact-list__row">
                  <span>房间</span>
                  <strong>{{ reservation.roomTypeName || '待排房' }} {{ reservation.roomNumber || '' }}</strong>
                </div>
                <div class="detail-fact-list__row">
                  <span>入住</span>
                  <strong>{{ reservation.checkInDate }}</strong>
                </div>
                <div class="detail-fact-list__row">
                  <span>离店</span>
                  <strong>{{ reservation.checkOutDate }}</strong>
                </div>
                <div class="detail-fact-list__row">
                  <span>成人 / 儿童</span>
                  <strong>{{ reservation.adults || 1 }} / {{ reservation.children || 0 }}</strong>
                </div>
              </div>
            </article>

            <article class="detail-section">
              <div class="detail-section__head">
                <h3>消费记录</h3>
                <span v-if="consumptions.length > 0" class="detail-section__meta">{{ consumptions.length }} 条</span>
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
                    <strong class="detail-list__amount">{{ formatAmount(item.amount) }}</strong>
                    <ion-button fill="clear" size="small" color="danger" @click="handleDeleteConsumption(item.id)">删除</ion-button>
                  </div>
                </div>
              </div>
              <p v-else class="mobile-note">暂无消费记录。</p>
            </article>

            <article class="detail-section">
              <div class="detail-section__head">
                <h3>收款记录</h3>
                <span v-if="payments.length > 0" class="detail-section__meta">{{ payments.length }} 条</span>
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
                    <strong class="detail-list__amount">{{ formatAmount(item.amount) }}</strong>
                    <ion-button fill="clear" size="small" color="danger" @click="handleDeletePayment(item.id)">删除</ion-button>
                  </div>
                </div>
              </div>
              <p v-else class="mobile-note">暂无收款记录。</p>
            </article>

            <article class="detail-section detail-reminder-card">
              <div class="detail-reminder-card__header">
                <div>
                  <h3>订单提醒</h3>
                  <p>{{ orderReminderDescription }}</p>
                </div>
                <strong class="detail-reminder-card__count">{{ orderReminderCountText }}</strong>
              </div>

              <div class="detail-reminder-card__actions">
                <ion-button fill="outline" size="small" @click="openOrderNotifications">查看提醒</ion-button>
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
                  <p class="mobile-note">操作人：{{ item.operator }}</p>
                  <p v-if="item.content" class="mobile-note">{{ item.content }}</p>
                  <div v-if="item.details && item.details.length > 0" class="log-item__details">
                    <p v-for="detail in item.details" :key="`${item.id}-${detail.label}`" class="mobile-note">
                      {{ detail.label }}：{{ detail.value }}
                    </p>
                  </div>
                </div>
              </div>
              <p v-else class="mobile-note">暂无日志。</p>
            </article>
          </div>

          <div v-else class="reservation-detail-content__body">
            <article class="detail-section">
              <div class="detail-section__head">
                <h2 class="mobile-section-title">渠道信息</h2>
              </div>

              <div class="detail-definition-list">
                <div class="detail-definition-list__row">
                  <span class="detail-definition-list__label">渠道名称</span>
                  <strong class="detail-definition-list__value">{{ channelInfo?.channelName || reservation.channelName || '自来客' }}</strong>
                </div>
                <div v-if="linkedMessageThread" class="detail-definition-list__row">
                  <span class="detail-definition-list__label">关联会话</span>
                  <div class="detail-linked-thread">
                    <strong class="detail-definition-list__value">{{ linkedMessageThreadLabel }}</strong>
                    <p class="mobile-note">{{ linkedMessageThreadMeta }}</p>
                  </div>
                </div>
                <div class="detail-definition-list__row">
                  <span class="detail-definition-list__label">渠道订单号</span>
                  <strong class="detail-definition-list__value">{{ channelInfo?.channelOrderNumber || reservation.channelOrderNumber || '无' }}</strong>
                </div>
                <div class="detail-definition-list__row">
                  <span class="detail-definition-list__label">价格计划</span>
                  <strong class="detail-definition-list__value">{{ channelInfo?.pricePlan || reservation.pricePlan || '无' }}</strong>
                </div>
                <div class="detail-definition-list__row">
                  <span class="detail-definition-list__label">支付方式</span>
                  <strong class="detail-definition-list__value">{{ channelInfo?.paymentMethod || reservation.paymentMethod || '未记录' }}</strong>
                </div>
                <div class="detail-definition-list__row">
                  <span class="detail-definition-list__label">特殊需求</span>
                  <strong class="detail-definition-list__value">{{ channelInfo?.specialRequests || reservation.notes || '无' }}</strong>
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
import { useRoute, useRouter } from 'vue-router'
import BookingFormModal, { type BookingFormSubmitPayload } from '@/components/room-status/BookingFormModal.vue'
import CancelReservationModal from '@/components/room-status/CancelReservationModal.vue'
import {
  formatAmount,
  formatDateTime,
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
import { ROUTE_PATHS, buildMessageDetailPath } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const route = useRoute()
const router = useRouter()
const notificationCenterStore = useNotificationCenterStore()
const roomStatusStore = useRoomStatusStore()
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
  if (!reservation.value) {
    return '订单详情'
  }
  return `${reservation.value.guestName} · 订单详情`
})

const statusText = computed(() => getReservationStatusText(reservation.value?.status))
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
const orderBoxMovedInAtText = computed(() => formatDateTime(orderBoxItem.value?.movedInAt))
const linkedMessageThreadLabel = computed(() => {
  if (!linkedMessageThread.value) {
    return ''
  }

  return (
    linkedMessageThread.value.guestName ||
    linkedMessageThread.value.listingName ||
    linkedMessageThread.value.channelName ||
    `会话 #${linkedMessageThread.value.id}`
  )
})
const linkedMessageThreadMeta = computed(() => {
  if (!linkedMessageThread.value) {
    return ''
  }

  const parts = [
    linkedMessageThread.value.channelName,
    linkedMessageThread.value.closed ? '已关闭' : '进行中',
  ]

  if (linkedMessageThread.value.unreadCount > 0) {
    parts.push(`未读 ${linkedMessageThread.value.unreadCount} 条`)
  }

  return parts.filter(Boolean).join(' · ')
})

const totalAmountText = computed(() => formatAmount(reservation.value?.totalAmount))
const totalConsumptionText = computed(() => formatAmount(totalConsumption.value))
const totalPaymentText = computed(() => formatAmount(totalPayment.value))
const orderReminderCountText = computed(() => `${orderReminderCount.value} 条`)
const orderReminderDescription = computed(() => {
  if (orderReminderNotice.value) {
    return orderReminderNotice.value
  }

  if (orderReminderCount.value > 0) {
    return `有 ${orderReminderCount.value} 条未读订单提醒待处理`
  }

  return '暂无提醒'
})

const remainingPayment = computed(() => {
  return Number(reservation.value?.totalAmount ?? 0) - Number(totalPayment.value || 0) - Number(totalConsumption.value || 0)
})

const remainingPaymentText = computed(() => formatAmount(remainingPayment.value))
const remainingPaymentLabel = computed(() => {
  if (remainingPayment.value > 0) {
    return '还需付款'
  }
  if (remainingPayment.value < 0) {
    return '超收金额'
  }
  return '账目状态'
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
    throw new Error('未获取到当前用户')
  }

  return user.id
}

async function loadOrderReminderCount() {
  orderReminderNotice.value = ''

  try {
    const userId = await ensureUserId()
    const response = await getUnreadNotificationCountByType(userId, ORDER_NOTIFICATION_TYPE)
    if (!response.success) {
      throw new Error(response.message || '加载订单提醒失败')
    }

    orderReminderCount.value = Number(response.data || 0)
  } catch {
    orderReminderCount.value = 0
    orderReminderNotice.value = '提醒数加载失败，请进入提醒页查看'
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
    showWarningToast('订单数据仍在加载，请稍后再试')
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
        text: '取消',
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
      throw new Error(reservationResponse.message || '订单详情加载失败')
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
      showWarningToast(resolveWarningMessage(error, '订单刷新失败'))
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
      throw new Error(response.message || '办理入住失败')
    }
    showSuccessToast('入住办理成功')
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '办理入住失败'))
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
      throw new Error(response.message || '办理退房失败')
    }
    showSuccessToast('退房办理成功')
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '办理退房失败'))
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
    showWarningToast('已取消订单不可修改')
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
      throw new Error(response.message || '修改订单失败')
    }
    showSuccessToast('订单已更新')
    showBookingModal.value = false
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '修改订单失败'))
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
      throw new Error(response.message || '取消预约失败')
    }
    showSuccessToast('订单已取消')
    showCancelModal.value = false
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '取消预约失败'))
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
      throw new Error(checkResponse.message || '校验订单盒子资格失败')
    }

    if (!checkResponse.data.canMove) {
      showWarningToast(checkResponse.data.reason || '只有已预订的房间可以移入订单盒子')
      return
    }

    const confirmed = await confirmAction(
      '移入订单盒子',
      '移入后订单不会实际排房、不占库存，且营业数据不计入统计。确认继续吗？',
      '确认移入',
    )
    if (!confirmed) {
      return
    }

    actionLoading.value = true
    const response = await moveToOrderBox({ reservationId: reservation.value.id })
    if (!response.success) {
      throw new Error(response.message || '移入订单盒子失败')
    }

    showSuccessToast('已移入订单盒子')
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '移入订单盒子失败'))
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleMoveOutOrderBox() {
  if (!reservation.value || !orderBoxItem.value) {
    showWarningToast('未找到订单盒子记录')
    return
  }

  const confirmed = await confirmAction('移出订单盒子', '确认将该订单移出盒子吗？', '确认移出', true)
  if (!confirmed) {
    return
  }

  actionLoading.value = true
  try {
    const response = await moveOutOrderBox({ orderBoxItemId: orderBoxItem.value.id })
    if (!response.success) {
      throw new Error(response.message || '移出订单盒子失败')
    }

    showSuccessToast('已移出订单盒子')
    await loadDetail()
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '移出订单盒子失败'))
    }
  } finally {
    actionLoading.value = false
  }
}

async function confirmDelete(title: string) {
  const alert = await alertController.create({
    header: title,
    message: '删除后不可恢复，是否继续？',
    buttons: [
      {
        text: '取消',
        role: 'cancel',
      },
      {
        text: '删除',
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
  const confirmed = await confirmDelete('删除消费')
  if (!confirmed) {
    return
  }

  actionLoading.value = true
  try {
    const response = await deleteConsumption(consumptionId)
    if (!response.success) {
      throw new Error(response.message || '删除消费失败')
    }
    showSuccessToast('消费已删除')
    await loadDetail()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除消费失败'))
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleDeletePayment(paymentId?: number) {
  if (!paymentId) {
    return
  }
  const confirmed = await confirmDelete('删除收款')
  if (!confirmed) {
    return
  }

  actionLoading.value = true
  try {
    const response = await deletePayment(paymentId)
    if (!response.success) {
      throw new Error(response.message || '删除收款失败')
    }
    showSuccessToast('收款已删除')
    await loadDetail()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除收款失败'))
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
    showWarningToast('当前环境暂不支持打印')
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
      text: '打印',
      handler: () => {
        handlePrint()
      },
    },
    {
      text: '查看客人备注',
      handler: () => {
        void presentReservationNotes()
      },
    },
  ]

  if (isOrderContext.value || orderBoxItem.value) {
    if (orderBoxItem.value) {
      buttons.push({
        text: '移出订单盒子',
        role: 'destructive',
        handler: () => {
          void handleMoveOutOrderBox()
        },
      })
    } else {
      buttons.push({
        text: '移入订单盒子',
        handler: () => {
          void handleMoveToOrderBox()
        },
      })
    }
  }

  if (reservation.value.roomId) {
    buttons.push({
      text: '查看房间',
      handler: () => {
        void openRoomDetail()
      },
    })
  }

  if (canCancelOrder.value) {
    buttons.push({
      text: '取消订单',
      role: 'destructive',
      handler: () => {
        showCancelModal.value = true
      },
    })
  }

  buttons.push({
    text: '取消',
    role: 'cancel',
  })

  const actionSheet = await actionSheetController.create({
    header: reservation.value.guestName || '订单更多操作',
    subHeader: reservation.value.orderNumber,
    buttons,
  })

  await actionSheet.present()
}

async function presentReservationNotes() {
  const notesText = reservationNotesText.value || '暂无客人备注'
  const alert = await alertController.create({
    header: '客人备注',
    message: escapeAlertMessage(notesText),
    buttons: ['知道了'],
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
      showWarningToast(resolveWarningMessage(error, '订单详情加载失败'))
    }
  }
})
</script>

<style scoped>
.reservation-detail-page {
  --background: var(--ios-pms-bg-page-plain);
}

.reservation-detail-page .mobile-stack {
  gap: var(--ios-pms-space-3);
}

.reservation-detail-hero {
  margin-bottom: var(--ios-pms-space-3);
  background: var(--ios-pms-surface-strong);
}

.reservation-detail-hero::before {
  display: none;
}

.reservation-detail-hero__topline,
.detail-section__head,
.log-item__head,
.detail-reminder-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ios-pms-space-3);
}

.reservation-detail-hero__eyebrow {
  color: var(--ios-pms-primary);
  font-weight: var(--ios-pms-weight-bold);
}

.reservation-detail-hero__status {
  flex-shrink: 0;
}

.reservation-detail-hero__headline-row {
  display: block;
}

.reservation-detail-hero__headline-copy {
  min-width: 0;
}

.reservation-detail-hero__orderline {
  margin-top: var(--ios-pms-space-2);
}

.reservation-detail-hero__message-entry {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--ios-pms-space-3);
}

.reservation-detail-hero__message-button {
  margin: 0;
  --border-radius: var(--ios-pms-radius-xl);
  --border-color: rgba(137, 174, 255, 0.38);
  --background: rgba(255, 255, 255, 0.7);
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 4px;
  --padding-bottom: 4px;
  min-height: 32px;
  color: var(--ios-pms-primary);
  font-size: 12px;
  font-weight: var(--ios-pms-weight-bold);
  backdrop-filter: blur(12px);
}

.reservation-detail-hero__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ios-pms-space-3);
  margin-top: var(--ios-pms-space-5);
}

.reservation-detail-hero__meta-item,
.detail-summary-card__hero-copy,
.detail-summary-card__hero-side,
.detail-summary-card__metric,
.detail-key-item {
  min-width: 0;
  display: grid;
  gap: var(--ios-pms-space-1);
}

.reservation-detail-hero__meta-item {
  padding-top: var(--ios-pms-space-3);
  border-top: 1px solid var(--ios-pms-divider);
}

.reservation-detail-hero__meta-item span,
.detail-summary-card__hero span,
.detail-summary-card__metric span,
.detail-key-item span,
.detail-note-row span,
.detail-definition-list__label {
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-body-sm-size);
  line-height: 1.4;
}

.reservation-detail-hero__meta-item strong,
.detail-summary-card__hero-side strong,
.detail-summary-card__metric strong,
.detail-key-item strong,
.detail-definition-list__value {
  color: var(--ios-pms-text-secondary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.4;
}

.reservation-detail-hero__chips {
  margin-top: var(--ios-pms-space-4);
}

.detail-reminder-card__header > div,
.detail-list__main,
.detail-summary-card__hero-copy {
  min-width: 0;
}

.reservation-detail-panel {
  padding: var(--ios-pms-space-4);
}

.detail-actions__busy {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--ios-pms-space-2);
}

.detail-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ios-pms-space-2);
}

.detail-actions__primary {
  grid-column: 1 / -1;
}

.detail-actions__secondary--solo {
  grid-column: 1 / -1;
}

.reservation-detail-content {
  overflow: hidden;
  padding: 0;
}

.reservation-detail-content__segment {
  padding: var(--ios-pms-space-4) var(--ios-pms-space-5) 0;
}

.reservation-detail-content__body {
  display: grid;
  gap: 0;
  padding: var(--ios-pms-space-2) var(--ios-pms-space-5) var(--ios-pms-space-5);
}

.reservation-detail-content__body--detail {
  padding-top: var(--ios-pms-space-3);
}

.detail-section {
  padding: var(--ios-pms-space-4) 0;
}

.detail-section + .detail-section {
  border-top: 1px solid var(--ios-pms-divider);
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
  margin-bottom: var(--ios-pms-space-3);
}

.detail-section__head h2,
.detail-section__head h3,
.detail-list__main strong,
.log-item strong {
  margin: 0;
}

.detail-section__head h2,
.detail-section__head h3 {
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: var(--ios-pms-weight-bold);
}

.detail-section__meta {
  flex-shrink: 0;
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-body-sm-size);
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.4;
  white-space: nowrap;
}

.detail-section--timeline {
  padding-top: var(--ios-pms-space-5);
}

.detail-summary-card {
  gap: var(--ios-pms-space-4);
}

.detail-summary-card__hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--ios-pms-space-3);
  padding-bottom: var(--ios-pms-space-3);
  border-bottom: 1px solid var(--ios-pms-divider);
}

.detail-summary-card__hero-side {
  text-align: right;
}

.detail-summary-card__amount {
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-metric-lg-size);
  font-weight: var(--ios-pms-weight-heavy);
  letter-spacing: -0.03em;
  line-height: 1;
}

.detail-summary-card__metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
}

.detail-summary-card__metric + .detail-summary-card__metric {
  padding-left: var(--ios-pms-space-3);
  margin-left: var(--ios-pms-space-3);
  border-left: 1px solid var(--ios-pms-divider);
}

.detail-fact-list__row {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  align-items: start;
  gap: var(--ios-pms-space-3);
  padding: var(--ios-pms-space-3) 0;
}

.detail-fact-list__row + .detail-fact-list__row {
  border-top: 1px solid var(--ios-pms-divider);
}

.detail-fact-list__row strong {
  color: var(--ios-pms-text-secondary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.45;
}

.detail-fact-list__row p {
  margin: 0;
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.detail-list__item,
.detail-list__actions {
  display: flex;
  justify-content: space-between;
  gap: var(--ios-pms-space-3);
}

.detail-list__item,
.log-item,
.detail-definition-list__row {
  padding: var(--ios-pms-space-3) 0;
}

.detail-list__item + .detail-list__item,
.log-item + .log-item,
.detail-definition-list__row + .detail-definition-list__row {
  border-top: 1px solid var(--ios-pms-divider);
}

.detail-list__item {
  align-items: flex-start;
}

.detail-list__main strong {
  display: block;
  color: var(--ios-pms-text-secondary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.35;
}

.detail-list__main p,
.detail-list__meta,
.log-item p,
.log-item__details p,
.detail-reminder-card p {
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.detail-list__main p,
.detail-list__meta {
  margin: var(--ios-pms-space-1) 0 0;
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
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-heavy);
}

.detail-reminder-card {
  gap: var(--ios-pms-space-3);
}

.detail-reminder-card__header {
  align-items: center;
}

.detail-reminder-card__count {
  flex-shrink: 0;
  color: var(--ios-pms-primary);
  font-size: var(--ios-pms-font-metric-md-size);
  font-weight: var(--ios-pms-weight-heavy);
  white-space: nowrap;
}

.detail-reminder-card__actions {
  display: flex;
  justify-content: flex-end;
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
  background: linear-gradient(180deg, rgba(52, 116, 246, 0.2), var(--ios-pms-divider));
}

.log-item {
  position: relative;
  padding: 0 0 0 var(--ios-pms-space-5);
}

.log-item + .log-item {
  margin-top: var(--ios-pms-space-4);
  padding-top: var(--ios-pms-space-4);
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
  align-items: center;
}

.log-item__head strong {
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.35;
}

.log-item__head .mobile-note {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border: 1px solid var(--ios-pms-border-faint);
  border-radius: var(--ios-pms-radius-pill);
  background: var(--ios-pms-surface-muted);
  flex-shrink: 0;
  white-space: nowrap;
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-note-size);
}

.log-item__details {
  padding-top: var(--ios-pms-space-2);
  gap: var(--ios-pms-space-1);
}

.detail-definition-list__row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  align-items: start;
  gap: var(--ios-pms-space-3);
}

.detail-linked-thread {
  min-width: 0;
}

.detail-linked-thread .mobile-note {
  margin: var(--ios-pms-space-1) 0 0;
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.detail-definition-list__value {
  word-break: break-word;
}

.is-danger {
  color: var(--ion-color-danger);
}

.is-success {
  color: var(--ion-color-success);
}

@media (max-width: 374px) {
  .reservation-detail-hero__meta-grid,
  .detail-summary-card__metrics,
  .detail-actions {
    grid-template-columns: 1fr;
  }

  .reservation-detail-hero__headline-row,
  .reservation-detail-hero__topline,
  .detail-summary-card__hero,
  .detail-list__item,
  .detail-reminder-card__header,
  .log-item__head {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }

  .reservation-detail-hero__message-entry {
    justify-content: flex-start;
  }

  .detail-list__actions,
  .detail-reminder-card__actions {
    justify-items: start;
    justify-content: flex-start;
    text-align: left;
  }

  .log-item {
    padding-left: var(--ios-pms-space-4);
  }

  .detail-definition-list__row {
    grid-template-columns: 1fr;
    gap: var(--ios-pms-space-1);
  }

  .detail-fact-list__row {
    grid-template-columns: 1fr;
    gap: var(--ios-pms-space-1);
  }
}
</style>
