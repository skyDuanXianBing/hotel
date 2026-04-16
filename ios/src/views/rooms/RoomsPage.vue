<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title class="mobile-toolbar-title">房态</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page rooms-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新房态" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero rooms-hero">
        <p class="mobile-note rooms-hero__eyebrow">房态核心迁移</p>
        <h1 class="mobile-title">{{ storeName }}</h1>
        <p class="mobile-subtitle">
          日期窗口、房型聚合、房间卡片与住宿运营工具已统一收敛到移动端工作台。
        </p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ selectedDateText }}</span>
          <span class="mobile-chip">{{ roomStatusStore.visibleDates.length }} 天窗口</span>
          <span class="mobile-chip">住宿工具 5 项</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card rooms-tools-card">
          <div>
            <h2 class="mobile-section-title">住宿工具</h2>
            <p class="mobile-note">房情表、房价管理、改价记录与保洁任务已补齐到 rooms 工作台。</p>
          </div>

          <div class="rooms-tools-card__list">
            <article v-for="entry in toolEntries" :key="entry.key" class="rooms-tool-entry">
              <div class="rooms-tool-entry__icon">
                <ion-icon :icon="entry.icon" />
              </div>

              <div class="rooms-tool-entry__body">
                <div class="rooms-tool-entry__header">
                  <strong>{{ entry.title }}</strong>
                  <span class="rooms-tool-entry__badge">{{ entry.badge }}</span>
                </div>
                <p>{{ entry.description }}</p>
              </div>

              <ion-button fill="clear" @click="handleOpenToolEntry(entry.path)">进入</ion-button>
            </article>
          </div>
        </section>

        <section class="mobile-card rooms-toolbar-card">
          <ion-searchbar
            v-model="searchKeyword"
            :debounce="0"
            placeholder="房号 / 手机 / 订单号 / 客户"
          />

          <div class="rooms-toolbar-actions">
            <ion-button fill="outline" size="small" @click="openBatchModal('dirty')">批量置脏</ion-button>
            <ion-button fill="outline" size="small" @click="openBatchModal('clean')">批量置净</ion-button>
            <ion-button fill="outline" size="small" @click="openBatchModal('open')">批量开房</ion-button>
            <ion-button fill="outline" size="small" @click="openBatchModal('close')">批量关房</ion-button>
          </div>

          <p v-if="loadNotice" class="mobile-note rooms-toolbar-card__notice">{{ loadNotice }}</p>
        </section>

        <RoomStatusDateStrip
          :days="roomStatusStore.visibleDates"
          @next="handleNextWindow"
          @previous="handlePreviousWindow"
          @select="handleSelectDate"
          @today="handleGoToday"
        />

        <RoomStatusSummaryCards :items="roomStatusStore.summaryCards" :loading="roomStatusStore.summaryLoading" />

        <RoomTypeSummaryList
          :items="roomStatusStore.roomTypeSummaries"
          @reset="handleResetRoomTypeFilter"
          @toggle="handleToggleRoomType"
        />

        <section v-if="showSearchResults" class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">搜索结果</h2>
              <p class="mobile-note">输入 2 个以上字符后展示匹配订单，可直达详情。</p>
            </div>
            <ion-spinner v-if="roomStatusStore.searching" name="crescent" />
          </div>

          <div v-if="roomStatusStore.searchResults.length > 0" class="mobile-list">
            <ReservationSummaryCard
              v-for="item in roomStatusStore.searchResults"
              :key="item.id"
              :reservation="item"
              @select="openReservationDetail(item.id)"
            />
          </div>
          <p v-else class="mobile-note">未找到匹配订单。</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">房间列表</h2>
              <p class="mobile-note">点击房间进入详情，点击订单摘要直达订单页。</p>
            </div>
            <ion-spinner v-if="roomStatusStore.loading" name="crescent" />
          </div>

          <div v-if="roomStatusStore.groupedVisibleRooms.length > 0" class="mobile-list room-groups">
            <section v-for="group in roomStatusStore.groupedVisibleRooms" :key="group.roomType" class="room-group">
              <div class="room-group__header">
                <strong>{{ group.roomType }}</strong>
                <span>{{ group.rooms.length }} 间</span>
              </div>
              <div class="mobile-list">
                <RoomStatusRoomCard
                  v-for="room in group.rooms"
                  :key="room.roomId"
                  :room="room"
                  @quick-action="openQuickAction"
                  @select-date="handleSelectDate"
                  @select-reservation="openReservationDetail"
                  @select-room="openRoomDetail"
                />
              </div>
            </section>
          </div>
          <p v-else-if="showRoomListLoadingHint" class="mobile-note rooms-loading-hint">正在同步最新房态...</p>
          <div v-else class="rooms-empty-state">
            <h3 class="rooms-empty-state__title">{{ emptyStateTitle }}</h3>
            <p class="mobile-note rooms-empty-state__text">{{ emptyStateDescription }}</p>
            <ion-button size="small" @click="handleOpenRoomTypeSettings">{{ emptyStateActionText }}</ion-button>
          </div>
        </section>
      </div>
    </ion-content>

    <ion-action-sheet
      :is-open="showQuickActionSheet"
      header="房间快捷动作"
      :sub-header="quickActionDescription"
      :buttons="quickActionButtons"
      @didDismiss="handleQuickActionDismiss"
    />

    <BookingFormModal
      :is-open="showBookingModal"
      :mode="bookingMode"
      :channels="roomStatusStore.channels"
      :room="selectedRoom"
      :submitting="bookingSubmitting"
      @dismiss="handleCloseBookingModal"
      @submit="handleBookingSubmit"
    />

    <CloseRoomModal
      :is-open="showCloseRoomModal"
      :room="selectedRoom"
      :submitting="roomStatusStore.actionLoading"
      @dismiss="handleCloseRoomModalDismiss"
      @submit="handleCloseRoomSubmit"
    />

    <BatchActionModal
      :is-open="showBatchModal"
      :mode="batchMode"
      :rooms="batchRooms"
      :submitting="roomStatusStore.actionLoading"
      @dismiss="handleBatchModalDismiss"
      @submit="handleBatchSubmit"
    />
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonActionSheet,
  IonButton,
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSearchbar,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import {
  gridOutline,
  listOutline,
  pricetagOutline,
  sparklesOutline,
  timeOutline,
} from 'ionicons/icons'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import BookingFormModal, {
  type BookingFormSubmitPayload,
  type BookingModalMode,
} from '@/components/room-status/BookingFormModal.vue'
import BatchActionModal, {
  type BatchActionMode,
  type BatchRoomOption,
  type BatchActionSubmitPayload,
} from '@/components/room-status/BatchActionModal.vue'
import CloseRoomModal, { type CloseRoomSubmitPayload } from '@/components/room-status/CloseRoomModal.vue'
import ReservationSummaryCard from '@/components/room-status/ReservationSummaryCard.vue'
import RoomStatusDateStrip from '@/components/room-status/RoomStatusDateStrip.vue'
import RoomStatusRoomCard from '@/components/room-status/RoomStatusRoomCard.vue'
import RoomStatusSummaryCards from '@/components/room-status/RoomStatusSummaryCards.vue'
import RoomTypeSummaryList from '@/components/room-status/RoomTypeSummaryList.vue'
import { checkCanMoveToOrderBox, moveToOrderBox } from '@/api/orderBox'
import { getAllRoomTypesWithRooms } from '@/api/roomType'
import { createReservation, type CreateReservationRequest } from '@/api/reservation'
import { ROUTE_PATHS } from '@/router/guards'
import { useRoomStatusStore } from '@/stores/roomStatus'
import { useStoreStore } from '@/stores/store'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

interface RoomsToolEntry {
  key: string
  title: string
  description: string
  path: string
  icon: string
  badge: string
}

const router = useRouter()
const storeStore = useStoreStore()
const roomStatusStore = useRoomStatusStore()

const searchKeyword = ref('')
const showQuickActionSheet = ref(false)
const selectedRoomId = ref(0)
const showBookingModal = ref(false)
const showCloseRoomModal = ref(false)
const bookingMode = ref<BookingModalMode>('create')
const bookingSubmitting = ref(false)
const loadNotice = ref('')
const showBatchModal = ref(false)
const batchMode = ref<BatchActionMode>('dirty')
const roomTypeCatalogCount = ref(0)
const roomTypeCatalogLoading = ref(true)
let hasCompletedInitialLoad = false
let searchTimer = 0

const storeName = computed(() => storeStore.currentStore?.name || '未选择门店')

const toolEntries = computed<RoomsToolEntry[]>(() => {
  return [
    {
      key: 'room-table',
      title: '房情表',
      description: '查看单日统计与远期 7 日房情，快速判断可售、占用与不可售。',
      path: ROUTE_PATHS.roomsRoomTable,
      icon: gridOutline,
      badge: '房情',
    },
    {
      key: 'pricing',
      title: '房价管理',
      description: '按房型与价格计划查看运营价格，并从移动端直接改价。',
      path: ROUTE_PATHS.roomsPricing,
      icon: pricetagOutline,
      badge: '价格',
    },
    {
      key: 'pricing-history',
      title: '改价记录',
      description: '按操作日期、价格日期、房型与价格计划回看变更记录。',
      path: ROUTE_PATHS.roomsPricingHistory,
      icon: timeOutline,
      badge: '记录',
    },
    {
      key: 'cleaning-overview',
      title: '保洁概览',
      description: '用房间卡片视图查看 7 日任务分布，并完成生成、分配与新增。',
      path: ROUTE_PATHS.roomsCleaningOverview,
      icon: sparklesOutline,
      badge: '保洁',
    },
    {
      key: 'cleaning-tasks',
      title: '任务列表',
      description: '按条件筛选保洁任务列表，并在详情弹层内分配或完成任务。',
      path: ROUTE_PATHS.roomsCleaningTasks,
      icon: listOutline,
      badge: '列表',
    },
  ]
})

const selectedRoom = computed(() => {
  if (!selectedRoomId.value) {
    return null
  }
  return roomStatusStore.getRoomListItemById(selectedRoomId.value)
})

const selectedDateText = computed(() => `当前业务日 ${roomStatusStore.selectedDate}`)

const showSearchResults = computed(() => searchKeyword.value.trim().length >= 2)

const emptyStateMode = computed(() => {
  if (roomTypeCatalogCount.value <= 0) {
    return 'no-room-type'
  }

  if (roomStatusStore.roomTypeSummaries.length <= 0) {
    return 'no-room'
  }

  return 'filtered-empty'
})

const emptyStateTitle = computed(() => {
  if (emptyStateMode.value === 'no-room-type') {
    return '当前无房型'
  }

  if (emptyStateMode.value === 'no-room') {
    return '有房型但无房间'
  }

  return '筛选结果为空'
})

const emptyStateDescription = computed(() => {
  if (emptyStateMode.value === 'no-room-type') {
    return '当前还没有配置房型。先添加房型并维护房间号，房态页才会出现可操作房间。'
  }

  if (emptyStateMode.value === 'no-room') {
    return '当前门店有房型但无房间，请到房型设置页继续在房型内补充房间号。'
  }

  return '当前筛选结果为空。可尝试重置房型筛选或下拉刷新后再查看。'
})

const emptyStateActionText = computed(() => {
  if (emptyStateMode.value === 'filtered-empty') {
    return '重置筛选'
  }

  return '添加房型'
})

const showRoomListLoadingHint = computed(() => {
  if (roomStatusStore.groupedVisibleRooms.length > 0) {
    return false
  }

  return roomStatusStore.loading || roomTypeCatalogLoading.value
})

const batchRooms = computed<BatchRoomOption[]>(() => {
  const rooms: BatchRoomOption[] = []

  for (const group of roomStatusStore.groupedVisibleRooms) {
    for (const room of group.rooms) {
      rooms.push({
        roomId: room.roomId,
        roomNumber: room.roomNumber,
        roomType: room.roomType,
      })
    }
  }

  return rooms
})

const quickActionDescription = computed(() => {
  if (!selectedRoom.value) {
    return ''
  }
  return `${selectedRoom.value.roomType} · ${selectedRoom.value.roomNumber} · ${selectedRoom.value.focusedDate}`
})

const quickActionButtons = computed(() => {
  if (!selectedRoom.value) {
    return [{ text: '取消', role: 'cancel' }]
  }

  const buttons = [] as Array<Record<string, unknown>>

  if (selectedRoom.value.focusedClosed) {
    buttons.push({
      text: '开房',
      handler: handleOpenRoom,
    })
  } else if (!selectedRoom.value.reservation) {
    buttons.push({
      text: '快速预订',
      handler: handleOpenCreateBooking,
    })
    buttons.push({
      text: '直接入住',
      handler: handleOpenCheckInBooking,
    })
    buttons.push({
      text: '关房',
      handler: handleOpenCloseRoom,
    })
  } else {
    buttons.push({
      text: '查看订单',
      handler: handleOpenQuickReservation,
    })
    if (selectedRoom.value.focusedBusinessState === 'reserved') {
      buttons.push({
        text: '移入订单盒子',
        handler: handleMoveSelectedReservationToOrderBox,
      })
    }
  }

  buttons.push({
    text: selectedRoom.value.isDirty ? '置净' : '置脏',
    handler: handleToggleDirty,
  })
  buttons.push({
    text: '房间详情',
    handler: handleOpenQuickRoomDetail,
  })
  buttons.push({
    text: '取消',
    role: 'cancel',
  })

  return buttons
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
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

async function loadPage(force = false) {
  loadNotice.value = ''
  try {
    await roomStatusStore.initialize(force)
  } catch (error) {
    const message = resolveWarningMessage(error, '房态加载失败')
    loadNotice.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  }
}

async function loadRoomTypeCatalogCount() {
  roomTypeCatalogLoading.value = true
  try {
    const response = await getAllRoomTypesWithRooms()
    if (!response.success || !response.data) {
      roomTypeCatalogCount.value = 0
      return
    }

    roomTypeCatalogCount.value = response.data.length
  } catch {
    roomTypeCatalogCount.value = 0
  } finally {
    roomTypeCatalogLoading.value = false
  }
}

async function refreshPageDataOnEnter() {
  loadNotice.value = ''
  try {
    await Promise.all([roomStatusStore.refreshAll(true), loadRoomTypeCatalogCount()])
  } catch (error) {
    const message = resolveWarningMessage(error, '房态刷新失败')
    loadNotice.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  }
}

async function handleRefresh(event: CustomEvent) {
  try {
    await refreshPageDataOnEnter()
  } finally {
    event.detail.complete()
  }
}

async function handleSelectDate(date: string) {
  try {
    await roomStatusStore.setSelectedDate(date)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '切换日期失败'))
    }
  }
}

async function handlePreviousWindow() {
  try {
    const shiftDays = Math.max(roomStatusStore.visibleDates.length, 1)
    await roomStatusStore.shiftWindow(-shiftDays)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '切换日期失败'))
    }
  }
}

async function handleNextWindow() {
  try {
    const shiftDays = Math.max(roomStatusStore.visibleDates.length, 1)
    await roomStatusStore.shiftWindow(shiftDays)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '切换日期失败'))
    }
  }
}

async function handleGoToday() {
  try {
    await roomStatusStore.goToday()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '定位今天失败'))
    }
  }
}

function handleToggleRoomType(roomType: string) {
  roomStatusStore.toggleRoomType(roomType)
}

function handleResetRoomTypeFilter() {
  roomStatusStore.resetRoomTypeFilter()
}

function openQuickAction(roomId: number) {
  selectedRoomId.value = roomId
  showQuickActionSheet.value = true
}

function handleQuickActionDismiss() {
  showQuickActionSheet.value = false
}

function handleOpenCreateBooking() {
  bookingMode.value = 'create'
  showBookingModal.value = true
}

function handleOpenCheckInBooking() {
  bookingMode.value = 'check-in'
  showBookingModal.value = true
}

function handleOpenCloseRoom() {
  showCloseRoomModal.value = true
}

async function handleOpenRoom() {
  if (!selectedRoom.value) {
    return
  }

  try {
    await roomStatusStore.openRooms({
      roomIds: [selectedRoom.value.roomId],
      startDate: selectedRoom.value.focusedDate,
      endDate: selectedRoom.value.focusedDate,
    })
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '开房失败'))
    }
  }
}

function handleToggleDirty() {
  if (!selectedRoom.value) {
    return
  }
  roomStatusStore.toggleDirty(selectedRoom.value.roomId)
}

function handleOpenQuickRoomDetail() {
  if (!selectedRoom.value) {
    return
  }
  openRoomDetail(selectedRoom.value.roomId)
}

function handleOpenQuickReservation() {
  if (!selectedRoom.value?.reservation) {
    return
  }
  openReservationDetail(selectedRoom.value.reservation.id)
}

async function handleMoveSelectedReservationToOrderBox() {
  if (!selectedRoom.value?.reservation) {
    return
  }

  const reservationId = selectedRoom.value.reservation.id

  try {
    const checkResponse = await checkCanMoveToOrderBox(reservationId)
    if (!checkResponse.success || !checkResponse.data) {
      throw new Error(checkResponse.message || '校验订单盒子资格失败')
    }

    if (!checkResponse.data.canMove) {
      showWarningToast(checkResponse.data.reason || '只有已预订的房间可以移入订单盒子')
      return
    }

    const confirmed = await confirmAction(
      '移入订单盒子',
      '移入后订单不会实际排房、不占库存，且移出后不会自动恢复原房间。确认继续吗？',
      '确认移入',
    )
    if (!confirmed) {
      return
    }

    const response = await moveToOrderBox({ reservationId })
    if (!response.success) {
      throw new Error(response.message || '移入订单盒子失败')
    }

    showSuccessToast('已移入订单盒子')
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '移入订单盒子失败'))
    }
  }
}

function handleCloseBookingModal() {
  showBookingModal.value = false
}

function handleCloseRoomModalDismiss() {
  showCloseRoomModal.value = false
}

async function handleBookingSubmit(payload: BookingFormSubmitPayload) {
  if (!selectedRoom.value) {
    showWarningToast('未选择房间')
    return
  }
  if (!payload.guestName || !payload.channelId || !payload.checkInDate || !payload.checkOutDate) {
    showWarningToast('请填写完整订单信息')
    return
  }

  const requestData: CreateReservationRequest = {
    guestName: payload.guestName,
    guestPhone: payload.guestPhone,
    roomId: selectedRoom.value.roomId,
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
  if (!selectedRoom.value) {
    return
  }

  try {
    await roomStatusStore.closeRooms({
      roomIds: [selectedRoom.value.roomId],
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

function openBatchModal(mode: BatchActionMode) {
  batchMode.value = mode
  showBatchModal.value = true
}

function handleBatchModalDismiss() {
  showBatchModal.value = false
}

async function handleBatchSubmit(payload: BatchActionSubmitPayload) {
  try {
    if (batchMode.value === 'dirty') {
      roomStatusStore.setDirtyState(payload.roomIds, true)
    }

    if (batchMode.value === 'clean') {
      roomStatusStore.setDirtyState(payload.roomIds, false)
    }

    if (batchMode.value === 'open') {
      await roomStatusStore.openRooms({
        roomIds: payload.roomIds,
        startDate: payload.startDate,
        endDate: payload.endDate,
        weekMode: payload.weekMode,
      })
    }

    if (batchMode.value === 'close') {
      await roomStatusStore.closeRooms({
        roomIds: payload.roomIds,
        startDate: payload.startDate,
        endDate: payload.endDate,
        type: payload.type,
        remark: payload.remark,
        weekMode: payload.weekMode,
      })
    }

    showBatchModal.value = false
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '批量操作失败'))
    }
  }
}

async function openRoomDetail(roomId: number) {
  await router.push({
    name: 'RoomStatusDetail',
    params: { roomId },
    query: { date: roomStatusStore.selectedDate },
  })
}

async function openReservationDetail(reservationId: number) {
  await router.push({
    name: 'RoomReservationDetail',
    params: { reservationId },
  })
}

async function handleOpenToolEntry(path: string) {
  await router.push(path)
}

async function handleOpenRoomTypeSettings() {
  if (emptyStateMode.value === 'filtered-empty') {
    handleResetRoomTypeFilter()
    return
  }

  await router.push(ROUTE_PATHS.settingsRoomTypes)
}

watch(
  () => searchKeyword.value,
  (keyword) => {
    window.clearTimeout(searchTimer)

    if (!keyword || keyword.trim().length < 2) {
      roomStatusStore.clearSearchResults()
      return
    }

    searchTimer = window.setTimeout(async () => {
      try {
        await roomStatusStore.runSearch(keyword)
      } catch (error) {
        if (!isHandledRequestError(error)) {
          showWarningToast(resolveWarningMessage(error, '订单搜索失败'))
        }
      }
    }, 250)
  },
)

watch(
  () => storeStore.currentStore?.id,
  async (nextStoreId, previousStoreId) => {
    if (!nextStoreId || nextStoreId === previousStoreId) {
      return
    }
    await loadPage(true)
  },
)

onMounted(async () => {
  try {
    await Promise.all([loadPage(false), loadRoomTypeCatalogCount()])
  } finally {
    hasCompletedInitialLoad = true
  }
})

onIonViewWillEnter(async () => {
  if (!hasCompletedInitialLoad) {
    return
  }

  await refreshPageDataOnEnter()
})
</script>

<style scoped>
.rooms-page {
  display: block;
}

.rooms-hero {
  margin-top: 4px;
}

.rooms-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.rooms-toolbar-card {
  display: grid;
  gap: 10px;
}

.rooms-tools-card {
  display: grid;
  gap: 14px;
}

.rooms-tools-card__list {
  display: grid;
  gap: 12px;
}

.rooms-tool-entry {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 20px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.rooms-tool-entry__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 16px;
  background: var(--app-primary-soft-strong);
  color: var(--ion-color-primary);
  font-size: 20px;
}

.rooms-tool-entry__header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rooms-tool-entry__header strong,
.rooms-tool-entry__body p {
  margin: 0;
}

.rooms-tool-entry__body p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.rooms-tool-entry__badge {
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 11px;
  font-weight: 600;
}

.rooms-toolbar-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.rooms-toolbar-card__notice {
  color: var(--ion-color-warning);
}

.room-groups {
  gap: 16px;
}

.room-group {
  display: grid;
  gap: 10px;
}

.room-group__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--app-muted);
  font-size: 13px;
}

.rooms-empty-state {
  display: grid;
  gap: 12px;
  justify-items: flex-start;
  padding-top: 16px;
}

.rooms-loading-hint {
  margin: 0;
  padding-top: 16px;
}

.rooms-empty-state__title {
  margin: 0;
  color: var(--app-heading);
  font-size: 16px;
  font-weight: 700;
}

.rooms-empty-state__text {
  margin: 0;
}
</style>
