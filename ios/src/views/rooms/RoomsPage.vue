<template>
  <ion-page>
    <ion-header translucent class="rooms-header">
      <ion-toolbar class="rooms-header__toolbar">
        <div class="rooms-header__bar">
          <div class="rooms-header__actions rooms-header__actions--left">
            <button
              class="rooms-header__icon"
              type="button"
              :aria-label="$t('stage5UiAttributes.48')"
              @click="handleToggleSearchPanel"
            >
              <ion-icon :icon="searchOutline" />
            </button>

            <button
              class="rooms-header__icon"
              type="button"
              :aria-label="$t('stage5UiAttributes.33')"
              :disabled="roomStatusStore.loading || roomTypeCatalogLoading"
              @click="handleManualRefresh"
            >
              <ion-icon :icon="refreshOutline" />
            </button>

            <button
              class="rooms-header__icon"
              type="button"
              :aria-label="$t('accommodation.roomPrice.roomTypeFilter')"
              @click="handleOpenFilterModal"
            >
              <ion-icon :icon="funnelOutline" />
            </button>
          </div>

          <h1 class="rooms-header__title">{{ $t('routes.Rooms') }}</h1>

          <div class="rooms-header__actions rooms-header__actions--right">
            <button
              class="rooms-header__icon"
              type="button"
              :aria-label="$t('stage5UiAttributes.24')"
              @click="presentToolsMenu"
            >
              <ion-icon :icon="menuOutline" />
            </button>
          </div>
        </div>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="rooms-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('stage5UiAttributes.8')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="showSearchPanel" class="rooms-search-panel">
        <div class="rooms-search-panel__bar">
          <div class="rooms-search-panel__field">
            <ion-searchbar
              v-model="searchKeyword"
              :debounce="0"
              class="rooms-searchbar"
              :placeholder="$t('stage5UiAttributes.43')"
            />

            <section v-if="showSearchOverlay" class="rooms-search-popover" aria-live="polite">
              <div class="rooms-search-popover__header">
                <strong>{{ searchOverlayTitle }}</strong>
                <ion-spinner v-if="roomStatusStore.searching" name="crescent" />
              </div>

              <div
                v-if="roomStatusStore.searchResults.length > 0"
                class="rooms-search-popover__list"
              >
                <ReservationSummaryCard
                  v-for="item in roomStatusStore.searchResults"
                  :key="item.id"
                  :reservation="item"
                  @select="handleSelectSearchReservation(item.id)"
                />
              </div>

              <p v-else class="rooms-search-popover__hint">
                {{ searchOverlayHint }}
              </p>
            </section>
          </div>

          <button type="button" class="rooms-search-panel__cancel" @click="handleHideSearchPanel">
            {{ $t('accommodation.common.cancel') }}
          </button>
        </div>
      </section>

      <p v-if="loadNotice" class="rooms-notice">{{ loadNotice }}</p>

      <section class="rooms-board">
        <RoomStatusCalendarGrid
          v-if="roomStatusStore.groupedVisibleRooms.length > 0"
          :days="roomStatusStore.visibleDates"
          :groups="roomStatusStore.groupedVisibleRooms"
          :loading="roomStatusStore.loading"
          :selected-date="roomStatusStore.selectedDate"
          :viewport-days="ROOM_STATUS_VIEWPORT_DAYS"
          @select-date="handleSelectDate"
          @select-reservation="openReservationDetail"
          @open-room-actions="handleOpenGridAction"
          @load-previous-window="handlePreviousWindow"
          @load-next-window="handleNextWindow"
          @go-today="handleGoToday"
        />

        <section v-else class="rooms-empty">
          <h3 class="rooms-empty__title">{{ emptyStateTitle }}</h3>
          <p class="rooms-empty__text">{{ emptyStateDescription }}</p>
          <ion-button size="small" @click="handleOpenRoomTypeSettings">{{ emptyStateActionText }}</ion-button>
        </section>
      </section>

      <button
        v-if="showTodayPill"
        slot="fixed"
        class="rooms-today-pill"
        type="button"
        @click="handleGoToday"
      >
        <ion-icon :icon="locateOutline" />
        <span>{{ $t('stage5VisibleText.150') }}</span>
      </button>

      <ion-fab slot="fixed" vertical="bottom" horizontal="end" class="rooms-fab">
        <ion-fab-button @click="handleFabClick">
          <ion-icon :icon="addOutline" />
        </ion-fab-button>
      </ion-fab>
    </ion-content>

    <ion-modal
      class="rooms-filter-modal"
      :is-open="showFilterModal"
      :initial-breakpoint="0.96"
      :breakpoints="[0, 0.96]"
      @didDismiss="handleFilterModalDismiss"
    >
      <ion-header translucent class="rooms-sheet-header">
        <ion-toolbar class="rooms-sheet-toolbar">
          <ion-title>{{ $t('stage5VisibleText.177') }}</ion-title>
          <ion-button slot="end" fill="clear" @click="handleCloseFilterModal">{{ $t('home.section.close') }}</ion-button>
        </ion-toolbar>
      </ion-header>

      <ion-content class="mobile-page rooms-filter-page">
        <div class="mobile-stack rooms-filter-stack">
          <RoomStatusSummaryCards
            :items="roomStatusStore.summaryCards"
            :loading="roomStatusStore.summaryLoading"
          />

          <RoomTypeSummaryList
            :items="roomStatusStore.roomTypeSummaries"
            :selected-room-types="draftSelectedRoomTypes"
            @reset="handleResetDraftRoomTypeFilter"
            @toggle="handleToggleDraftRoomType"
          />

          <div class="rooms-filter-actions">
            <ion-button fill="outline" @click="handleResetDraftRoomTypeFilter">{{ $t('accommodation.common.reset') }}</ion-button>
            <ion-button @click="handleApplyDraftRoomTypeFilter">{{ $t('accommodation.common.confirm') }}</ion-button>
          </div>
        </div>
      </ion-content>
    </ion-modal>

    <ion-modal
      class="rooms-picker-modal"
      :is-open="showRoomPickerModal"
      @didDismiss="showRoomPickerModal = false"
    >
      <ion-header class="rooms-picker-header">
        <ion-toolbar class="rooms-picker-toolbar">
          <ion-title>{{ $t('stage5VisibleText.237') }}</ion-title>
          <ion-button
            slot="end"
            fill="clear"
            class="rooms-picker__close"
            :aria-label="$t('home.section.close')"
            @click="showRoomPickerModal = false"
          >
            <ion-icon slot="icon-only" :icon="closeOutline" />
          </ion-button>
        </ion-toolbar>
      </ion-header>

      <ion-content class="mobile-page rooms-picker-page">
        <section v-if="roomStatusStore.groupedVisibleRooms.length > 0" class="rooms-picker">
          <header class="rooms-picker__summary" :aria-label="$t('stage5UiAttributes.40')">
            <span class="rooms-picker__summary-pill rooms-picker__summary-pill--accent">
              {{ pickerDateLabel }}
            </span>
            <span class="rooms-picker__summary-pill">{{ pickerRoomCount }}{{ $t('stage5DynamicUi.140') }}</span>
          </header>

          <section
            v-for="group in roomStatusStore.groupedVisibleRooms"
            :key="group.roomType"
            class="rooms-picker__group"
          >
            <div class="rooms-picker__group-header">
              <strong>{{ group.roomType }}</strong>
              <span>{{ group.rooms.length }}{{ $t('settingsStage4.common.unitRooms') }}</span>
            </div>

            <div class="rooms-picker__group-grid">
              <button
                v-for="room in group.rooms"
                :key="room.roomId"
                type="button"
                class="rooms-picker__room"
                :class="`is-${room.focusedBusinessState}`"
                @click="handleRoomPicked(room.roomId, room.focusedDate)"
              >
                <div class="rooms-picker__room-top">
                  <strong class="rooms-picker__room-number">{{ room.roomNumber }}</strong>
                  <span class="rooms-picker__room-arrow">{{ $t('stage5VisibleText.195') }}</span>
                </div>

                <div class="rooms-picker__room-bottom">
                  <span class="rooms-picker__room-state">{{ room.focusedStatusText }}</span>
                </div>
              </button>
            </div>
          </section>
        </section>
        <section v-else class="rooms-picker__empty">
          <h3>{{ $t('stage5VisibleText.200') }}</h3>
          <p>{{ $t('stage5VisibleText.227') }}</p>
        </section>
      </ion-content>
    </ion-modal>

    <ion-action-sheet
      :is-open="showQuickActionSheet"
      :header="$t('stage5UiAttributes.45')"
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
  actionSheetController,
  alertController,
  IonActionSheet,
  IonButton,
  IonContent,
  IonFab,
  IonFabButton,
  IonHeader,
  IonIcon,
  IonModal,
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
  addOutline,
  chevronBackOutline,
  chevronForwardOutline,
  closeOutline,
  funnelOutline,
  gridOutline,
  listOutline,
  locateOutline,
  menuOutline,
  pricetagOutline,
  refreshOutline,
  searchOutline,
  sparklesOutline,
  timeOutline,
} from 'ionicons/icons'
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import BookingFormModal, {
  type BookingFormSubmitPayload,
  type BookingModalMode,
} from '@/components/room-status/BookingFormModal.vue'
import BatchActionModal, {
  type BatchActionMode,
  type BatchActionSubmitPayload,
  type BatchRoomOption,
} from '@/components/room-status/BatchActionModal.vue'
import RoomStatusCalendarGrid from '@/components/room-status/RoomStatusCalendarGrid.vue'
import CloseRoomModal, { type CloseRoomSubmitPayload } from '@/components/room-status/CloseRoomModal.vue'
import ReservationSummaryCard from '@/components/room-status/ReservationSummaryCard.vue'
import RoomStatusSummaryCards from '@/components/room-status/RoomStatusSummaryCards.vue'
import RoomTypeSummaryList from '@/components/room-status/RoomTypeSummaryList.vue'
import { checkCanMoveToOrderBox, moveToOrderBox } from '@/api/orderBox'
import { getAllRoomTypesWithRooms } from '@/api/roomType'
import { createReservation, type CreateReservationRequest } from '@/api/reservation'
import { ROUTE_PATHS } from '@/router/guards'
import { ROOM_STATUS_VIEWPORT_DAYS, useRoomStatusStore } from '@/stores/roomStatus'
import { useStoreStore } from '@/stores/store'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import {
  formatBusinessDateLabel,
  getBusinessDateWeekdayLabel,
  getStoreTodayDate,
} from '@/utils/storeBusinessDate'

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
const { t } = useI18n()

const searchKeyword = ref('')
const showSearchPanel = ref(false)
const showQuickActionSheet = ref(false)
const selectedRoomId = ref(0)
const selectedBusinessDate = ref('')
const showBookingModal = ref(false)
const showCloseRoomModal = ref(false)
const bookingMode = ref<BookingModalMode>('create')
const bookingSubmitting = ref(false)
const loadNotice = ref('')
const showBatchModal = ref(false)
const batchMode = ref<BatchActionMode>('dirty')
const roomTypeCatalogCount = ref(0)
const roomTypeCatalogLoading = ref(true)
const showFilterModal = ref(false)
const draftSelectedRoomTypes = ref<string[]>([])
const showRoomPickerModal = ref(false)
let hasCompletedInitialLoad = false
let searchTimer = 0

const toolEntries = computed<RoomsToolEntry[]>(() => {
  return [
    {
      key: 'room-table',
      title: t('accommodation.layout.roomTable'),
      description: t('stage5Final.rooms.roomTableDescription'),
      path: ROUTE_PATHS.roomsRoomTable,
      icon: gridOutline,
      badge: t('stage5Final.rooms.roomTableBadge'),
    },
    {
      key: 'pricing',
      title: t('accommodation.layout.roomPriceManagement'),
      description: t('stage5Final.rooms.pricingDescription'),
      path: ROUTE_PATHS.roomsPricing,
      icon: pricetagOutline,
      badge: t('stage5Final.rooms.pricingBadge'),
    },
    {
      key: 'pricing-history',
      title: t('accommodation.layout.priceChangeHistory'),
      description: t('stage5Final.rooms.historyDescription'),
      path: ROUTE_PATHS.roomsPricingHistory,
      icon: timeOutline,
      badge: t('stage5Final.rooms.historyBadge'),
    },
    {
      key: 'cleaning-overview',
      title: t('stage5Final.rooms.cleaningOverview'),
      description: t('stage5Final.rooms.cleaningOverviewDescription'),
      path: ROUTE_PATHS.roomsCleaningOverview,
      icon: sparklesOutline,
      badge: t('stage5Final.rooms.cleaningBadge'),
    },
    {
      key: 'cleaning-tasks',
      title: t('accommodation.layout.taskList'),
      description: t('stage5Final.rooms.cleaningTasksDescription'),
      path: ROUTE_PATHS.roomsCleaningTasks,
      icon: listOutline,
      badge: t('stage5Final.rooms.listBadge'),
    },
  ]
})

const selectedRoom = computed(() => {
  if (!selectedRoomId.value) {
    return null
  }

  return roomStatusStore.getRoomListItemById(
    selectedRoomId.value,
    selectedBusinessDate.value || undefined,
  )
})

const todayDateKey = computed(() => {
  return getStoreTodayDate()
})

const showTodayPill = computed(() => {
  return (
    roomStatusStore.selectedDate !== todayDateKey.value ||
    roomStatusStore.visibleFocusDate !== todayDateKey.value
  )
})

const hasSearchKeyword = computed(() => searchKeyword.value.trim().length >= 2)

const showSearchOverlay = computed(() => {
  return (
    showSearchPanel.value &&
    hasSearchKeyword.value &&
    (roomStatusStore.searching || roomStatusStore.hasSearchCompleted)
  )
})

const searchOverlayTitle = computed(() => {
  if (roomStatusStore.searching) {
    return t('roomStatus.roomsPage.search.loadingTitle')
  }

  if (roomStatusStore.searchResults.length > 0) {
    return t('roomStatus.roomsPage.search.resultTitle', {
      count: roomStatusStore.searchResults.length,
    })
  }

  return t('roomStatus.roomsPage.search.emptyTitle')
})

const searchOverlayHint = computed(() => {
  if (roomStatusStore.searching) {
    return t('roomStatus.roomsPage.search.loadingHint')
  }

  if (roomStatusStore.searchResults.length > 0) {
    return t('roomStatus.roomsPage.search.resultHint')
  }

  return t('roomStatus.roomsPage.search.emptyHint')
})

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
    return t('roomStatus.roomsPage.empty.noRoomTypeTitle')
  }

  if (emptyStateMode.value === 'no-room') {
    return t('roomStatus.roomsPage.empty.noRoomTitle')
  }

  return t('roomStatus.roomsPage.empty.filteredTitle')
})

const emptyStateDescription = computed(() => {
  if (emptyStateMode.value === 'no-room-type') {
    return t('roomStatus.roomsPage.empty.noRoomTypeDescription')
  }

  if (emptyStateMode.value === 'no-room') {
    return t('roomStatus.roomsPage.empty.noRoomDescription')
  }

  return t('roomStatus.roomsPage.empty.filteredDescription')
})

const emptyStateActionText = computed(() => {
  if (emptyStateMode.value === 'filtered-empty') {
    return t('roomStatus.roomsPage.empty.resetAction')
  }

  return t('roomStatus.roomsPage.empty.addRoomTypeAction')
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

const pickerRoomCount = computed(() => {
  return roomStatusStore.groupedVisibleRooms.reduce((total, group) => total + group.rooms.length, 0)
})

function formatPickerDateLabel(value: string) {
  if (!value) {
    return ''
  }

  const dateLabel = formatBusinessDateLabel(value, 'month-day-weekday', '')
  if (!dateLabel) {
    return value
  }

  const [monthDay] = dateLabel.split(' ')
  const [month, day] = monthDay.split('-')
  if (!month || !day) {
    return dateLabel
  }

  return t('stage5Final.rooms.dateLabel', {
    month: Number(month),
    day: Number(day),
    weekday: getBusinessDateWeekdayLabel(value, ''),
  })
}

const pickerDateLabel = computed(() => formatPickerDateLabel(roomStatusStore.visibleFocusDate))

const quickActionDescription = computed(() => {
  if (!selectedRoom.value) {
    return ''
  }

  return `${selectedRoom.value.roomType} · ${selectedRoom.value.roomNumber} · ${selectedRoom.value.focusedDate}`
})

const quickActionButtons = computed(() => {
  if (!selectedRoom.value) {
    return [{ text: t('accommodation.common.cancel'), role: 'cancel' }]
  }

  const buttons = [] as Array<Record<string, unknown>>

  if (selectedRoom.value.focusedClosed) {
    buttons.push({
      text: t('iosStage5.roomStatus.openRoom'),
      handler: handleOpenRoom,
    })
  } else if (!selectedRoom.value.reservation) {
    buttons.push({
      text: t('roomStatus.bookingModal.title.create'),
      handler: handleOpenCreateBooking,
    })
    buttons.push({
      text: t('roomStatus.booking.drawerTitle.checkIn'),
      handler: handleOpenCheckInBooking,
    })
    buttons.push({
      text: t('accommodation.roomPrice.closeRoom'),
      handler: handleOpenCloseRoom,
    })
  } else {
    buttons.push({
      text: t('home.stat.arrivals.2'),
      handler: handleOpenQuickReservation,
    })

    if (selectedRoom.value.focusedBusinessState === 'reserved') {
      buttons.push({
        text: t('roomStatus.common.moveToOrderBox'),
        handler: handleMoveSelectedReservationToOrderBox,
      })
    }
  }

  buttons.push({
    text: selectedRoom.value.isDirty ? t('stage5Final.rooms.clean') : t('stage5Final.rooms.dirty'),
    handler: handleToggleDirty,
  })
  buttons.push({
    text: t('roomStatus.detail.channelInfo.roomDetails'),
    handler: handleOpenQuickRoomDetail,
  })
  buttons.push({
    text: t('accommodation.common.cancel'),
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

async function loadPage(force = false) {
  loadNotice.value = ''

  try {
    await roomStatusStore.initialize(force)
  } catch (error) {
    const message = resolveWarningMessage(error, t('roomStatus.store.errors.calendarLoadFailed'))
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
    const message = resolveWarningMessage(error, t('stage5Pattern.refreshFailed'))
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
    const refresher = event.detail as { complete: () => void }
    refresher.complete()
  }
}

async function handleManualRefresh() {
  await refreshPageDataOnEnter()
}

function handleHideSearchPanel() {
  showSearchPanel.value = false
  searchKeyword.value = ''
  roomStatusStore.clearSearchResults()
}

function handleToggleSearchPanel() {
  if (!showSearchPanel.value) {
    showSearchPanel.value = true
    return
  }

  handleHideSearchPanel()
}

async function handleSelectDate(date: string) {
  try {
    await roomStatusStore.setSelectedDate(date)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.operationFailed')))
    }
  }
}

async function handlePreviousWindow() {
  try {
    const shiftDays = Math.max(ROOM_STATUS_VIEWPORT_DAYS, 1)
    await roomStatusStore.shiftWindow(-shiftDays)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.operationFailed')))
    }
  }
}

async function handleNextWindow() {
  try {
    const shiftDays = Math.max(ROOM_STATUS_VIEWPORT_DAYS, 1)
    await roomStatusStore.shiftWindow(shiftDays)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.operationFailed')))
    }
  }
}

async function handleGoToday() {
  try {
    await roomStatusStore.goToday()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.operationFailed')))
    }
  }
}

function syncDraftRoomTypeFilter() {
  draftSelectedRoomTypes.value = [...roomStatusStore.selectedRoomTypes]
}

function handleOpenFilterModal() {
  syncDraftRoomTypeFilter()
  showFilterModal.value = true
}

function handleCloseFilterModal() {
  showFilterModal.value = false
}

function handleFilterModalDismiss() {
  syncDraftRoomTypeFilter()
  showFilterModal.value = false
}

function handleToggleDraftRoomType(roomType: string) {
  if (draftSelectedRoomTypes.value.includes(roomType)) {
    draftSelectedRoomTypes.value = draftSelectedRoomTypes.value.filter((item) => item !== roomType)
    return
  }

  draftSelectedRoomTypes.value = [...draftSelectedRoomTypes.value, roomType]
}

function handleResetDraftRoomTypeFilter() {
  draftSelectedRoomTypes.value = [...roomStatusStore.roomTypes]
}

function handleApplyDraftRoomTypeFilter() {
  roomStatusStore.setSelectedRoomTypes(draftSelectedRoomTypes.value)
  showFilterModal.value = false
}

function openQuickAction(roomId: number, businessDate?: string) {
  selectedRoomId.value = roomId
  selectedBusinessDate.value = businessDate || roomStatusStore.selectedDate
  showQuickActionSheet.value = true
}

function clearSelectedRoomContext() {
  selectedRoomId.value = 0
  selectedBusinessDate.value = ''
}

function handleOpenGridAction(payload: { roomId: number; date: string }) {
  openQuickAction(payload.roomId, payload.date)
}

function handleQuickActionDismiss() {
  showQuickActionSheet.value = false

  if (showBookingModal.value || showCloseRoomModal.value) {
    return
  }

  clearSelectedRoomContext()
}

function handleFabClick() {
  if (roomStatusStore.groupedVisibleRooms.length === 0) {
    showWarningToast(t('stage5Pattern.setup'))
    return
  }

  showRoomPickerModal.value = true
}

function handleRoomPicked(roomId: number, businessDate: string) {
  selectedRoomId.value = roomId
  selectedBusinessDate.value = businessDate || roomStatusStore.selectedDate
  showRoomPickerModal.value = false
  bookingMode.value = 'create'
  showBookingModal.value = true
}

async function presentToolsMenu() {
  const buttons = [
    {
      text: t('stage5Final.rooms.previousFiveDays'),
      icon: chevronBackOutline,
      handler: () => {
        void handlePreviousWindow()
      },
    },
    {
      text: t('stage5Final.rooms.nextFiveDays'),
      icon: chevronForwardOutline,
      handler: () => {
        void handleNextWindow()
      },
    },
    ...toolEntries.value.map((entry) => ({
      text: `${entry.title} · ${entry.badge}`,
      icon: entry.icon,
      handler: () => {
        void handleOpenToolEntry(entry.path)
      },
    })),
    {
      text: t('roomStatus.batchClean.dirty'),
      handler: () => {
        openBatchModal('dirty')
      },
    },
    {
      text: t('roomStatus.batchClean.clean'),
      handler: () => {
        openBatchModal('clean')
      },
    },
    {
      text: t('roomStatus.batchRoom.open'),
      handler: () => {
        openBatchModal('open')
      },
    },
    {
      text: t('roomStatus.batchRoom.close'),
      handler: () => {
        openBatchModal('close')
      },
    },
    {
      text: t('accommodation.common.cancel'),
      role: 'cancel',
    },
  ]

  const actionSheet = await actionSheetController.create({
    header: t('stage5Final.rooms.toolsTitle'),
    subHeader: t('stage5Final.rooms.toolsSubtitle'),
    buttons,
  })

  await actionSheet.present()
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
      showWarningToast(resolveWarningMessage(error, t('roomStatus.closeRoom.messages.openFailed')))
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

  openRoomDetail(selectedRoom.value.roomId, selectedRoom.value.focusedDate)
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
      throw new Error(checkResponse.message || t('order.mobile.messages.eligibilityFailed'))
    }

    if (!checkResponse.data.canMove) {
      showWarningToast(checkResponse.data.reason || t('roomStatus.detail.messages.moveToOrderBoxOnlyConfirmed'))
      return
    }

    const confirmed = await confirmAction(
      t('stage5Final.rooms.moveToOrderBoxTitle'),
      t('stage5Final.rooms.moveToOrderBoxMessage'),
      t('stage5Final.rooms.moveToOrderBoxConfirm'),
    )
    if (!confirmed) {
      return
    }

    const response = await moveToOrderBox({ reservationId })
    if (!response.success) {
      throw new Error(response.message || t('roomStatus.detail.messages.moveToOrderBoxFailed'))
    }

    showSuccessToast(t('roomStatus.detail.messages.moveToOrderBoxSuccess'))
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('roomStatus.detail.messages.moveToOrderBoxFailed')))
    }
  }
}

function handleCloseBookingModal() {
  showBookingModal.value = false
  clearSelectedRoomContext()
}

function handleCloseRoomModalDismiss() {
  showCloseRoomModal.value = false
  clearSelectedRoomContext()
}

async function handleBookingSubmit(payload: BookingFormSubmitPayload) {
  const currentRoom = selectedRoom.value

  if (!currentRoom) {
    showWarningToast(t('roomStatus.roomLock.target.none'))
    return
  }

  if (!payload.guestName || !payload.channelId || !payload.checkInDate || !payload.checkOutDate) {
    showWarningToast(t('iosStage5.roomStatus.completeOrder'))
    return
  }

  const requestData: CreateReservationRequest = {
    guestName: payload.guestName,
    guestPhone: payload.guestPhone,
    roomId: currentRoom.roomId,
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
      bookingMode.value === 'check-in' ? t('iosStage5.roomStatus.checkInCreated') : t('roomStatus.roomsPage.bookingCreated'),
    )
    showBookingModal.value = false
    clearSelectedRoomContext()
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
  const currentRoom = selectedRoom.value

  if (!currentRoom) {
    showWarningToast(t('roomStatus.roomLock.target.none'))
    return
  }

  try {
    await roomStatusStore.closeRooms({
      roomIds: [currentRoom.roomId],
      startDate: payload.startDate,
      endDate: payload.endDate,
      type: payload.type,
      remark: payload.remark,
    })
    showCloseRoomModal.value = false
    clearSelectedRoomContext()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('roomStatus.closeRoom.messages.failed')))
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
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.operationFailed')))
    }
  }
}

async function openRoomDetail(roomId: number, businessDate?: string) {
  await router.push({
    name: 'RoomStatusDetail',
    params: { roomId },
    query: { date: businessDate || roomStatusStore.selectedDate },
  })
}

async function openReservationDetail(reservationId: number) {
  await router.push({
    name: 'RoomReservationDetail',
    params: { reservationId },
  })
}

async function handleSelectSearchReservation(reservationId: number) {
  searchKeyword.value = ''
  showSearchPanel.value = false
  roomStatusStore.clearSearchResults()
  await openReservationDetail(reservationId)
}

async function handleOpenToolEntry(path: string) {
  await router.push(path)
}

async function handleOpenRoomTypeSettings() {
  if (emptyStateMode.value === 'filtered-empty') {
    roomStatusStore.resetRoomTypeFilter()
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
          showWarningToast(resolveWarningMessage(error, t('roomStatus.store.errors.searchFailed')))
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
  --background: #ffffff;
}

.rooms-header {
  background: #ffffff;
}

.rooms-header__toolbar {
  --background: #ffffff;
  --border-width: 0;
  --min-height: 48px;
  padding-top: max(var(--app-safe-top), 0px);
}

.rooms-header__bar {
  position: relative;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  gap: 6px;
  padding: 5px 14px;
}

.rooms-header__title {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -48%);
  padding: 0;
  margin: 0;
  color: var(--ios-pms-header-title-color);
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
  text-align: center;
  line-height: 1;
  pointer-events: none;
  white-space: nowrap;
}

.rooms-header__actions {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  min-width: 27px;
}

.rooms-header__actions--left {
  justify-content: flex-start;
}

.rooms-header__actions--right {
  justify-content: flex-end;
}

.rooms-header__icon {
  appearance: none;
  box-sizing: border-box;
  border: 1px solid #eef0f3;
  background: #ffffff;
  width: 27px;
  height: 27px;
  padding: 0;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #4a4a4a;
  box-shadow:
    0 1px 2px rgba(20, 20, 20, 0.04),
    inset 0 0 0 1px rgba(255, 255, 255, 0.9);
}

.rooms-header__icon:active {
  transform: translateY(1px);
  background: #f7f7f7;
}

.rooms-header__icon[disabled] {
  opacity: 0.5;
  box-shadow: none;
}

.rooms-header__icon ion-icon {
  font-size: 13px;
  stroke-width: 2;
}

.rooms-search-panel {
  padding: 0 34px 10px;
  background: #ffffff;
}

.rooms-search-panel__bar {
  display: flex;
  align-items: center;
  gap: 14px;
}

.rooms-search-panel__field {
  position: relative;
  flex: 1;
  min-width: 0;
}

.rooms-searchbar {
  flex: 1;
  margin: 0;
  padding: 0;
  height: 38px;
  min-height: 38px;
  --background: #f8f8f8;
  --border-radius: 5px;
  --box-shadow: none;
  --color: #333333;
  --icon-color: #8a8a8a;
  --placeholder-color: #6f6f6f;
  --placeholder-opacity: 1;
  --clear-button-color: #8a8a8a;
  --padding-start: 0;
  --padding-end: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --cancel-button-color: #666666;
  border: 0;
  border-radius: 5px;
  background: #f8f8f8;
  box-shadow: none;
}

.rooms-searchbar :deep(.searchbar-input-container) {
  height: 38px;
  min-height: 38px;
  border: 0;
  border-radius: 5px;
  background: #f8f8f8;
  box-shadow: none;
  overflow: hidden;
}

.rooms-searchbar :deep(.searchbar-input) {
  height: 38px;
  min-height: 38px;
  padding-inline-start: 42px !important;
  padding-inline-end: 12px !important;
  color: #333333;
  font-size: 17px;
  font-weight: 400;
  letter-spacing: 0;
  background: transparent;
  box-shadow: none;
}

.rooms-searchbar :deep(.searchbar-search-icon) {
  top: 50%;
  left: 14px;
  width: 20px;
  height: 20px;
  margin-top: 0;
  transform: translateY(-50%);
  color: #8a8a8a;
}

.rooms-search-panel__cancel {
  border: 0;
  background: transparent;
  color: #666666;
  font: inherit;
  font-size: 16px;
  font-weight: 400;
  line-height: 38px;
  white-space: nowrap;
}

.rooms-board {
  margin: 0;
  border-top: 1px solid #e9e9e9;
  background: #ffffff;
}

.rooms-notice {
  margin: 0;
  padding: 10px 16px 12px;
  background: rgba(252, 211, 77, 0.14);
  color: var(--ion-color-warning-shade, #92400e);
  font-size: 13px;
}

.rooms-search-popover {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  right: 0;
  z-index: 18;
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid #ededed;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(20, 20, 20, 0.1);
}

.rooms-search-popover__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #333333;
  font-size: 14px;
  font-weight: 500;
}

.rooms-search-popover__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: min(42vh, 360px);
  overflow-y: auto;
}

.rooms-search-popover__hint {
  margin: 0;
  color: #8a8a8a;
  font-size: 13px;
  line-height: 1.5;
}

.rooms-search-popover__list::-webkit-scrollbar {
  width: 6px;
}

.rooms-search-popover__list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(150, 150, 150, 0.32);
}

.rooms-search-popover__list::-webkit-scrollbar-track {
  background: transparent;
}

.rooms-search-popover :deep(.reservation-card) {
  padding: 12px;
  border: 1px solid #ededed;
  border-radius: 8px;
  box-shadow: none;
}

.rooms-search-popover :deep(.reservation-card__header strong) {
  font-size: 14px;
}

.rooms-search-popover :deep(.reservation-card__header p),
.rooms-search-popover :deep(.reservation-card__body) {
  font-size: 12px;
}

.rooms-search-popover :deep(.reservation-card:active) {
  background: #f8f8f8;
}

.rooms-filter-actions {
  position: sticky;
  bottom: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 0 calc(env(safe-area-inset-bottom, 0px) + 14px);
  background:
    linear-gradient(180deg, rgba(245, 247, 251, 0) 0%, rgba(245, 247, 251, 0.92) 24%, rgba(245, 247, 251, 1) 100%);
}

.rooms-filter-actions ion-button {
  margin: 0;
  min-height: 48px;
  --border-radius: 16px;
  font-weight: 700;
}

.rooms-empty {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 40px 24px 52px;
  align-items: flex-start;
}

.rooms-empty__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--app-heading, #10233f);
}

.rooms-empty__text {
  margin: 0;
  color: var(--app-muted, #64748b);
  font-size: 13px;
}

.rooms-today-pill {
  position: absolute;
  bottom: calc(env(safe-area-inset-bottom, 0px) + 26px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
  appearance: none;
  border: 1px solid rgba(77, 121, 219, 0.14);
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-heading, #10233f);
  font-size: 13px;
  font-weight: 700;
  padding: 10px 18px;
  border-radius: 999px;
  box-shadow: 0 14px 28px rgba(48, 77, 133, 0.14);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.rooms-today-pill ion-icon {
  font-size: 16px;
  color: var(--ion-color-primary);
}

.rooms-fab {
  margin-right: 8px;
  margin-bottom: calc(env(safe-area-inset-bottom, 0px) + 2px);
}

.rooms-fab :deep(ion-fab-button) {
  --background: linear-gradient(180deg, #4a85ff 0%, #2f6df2 100%);
  --box-shadow: 0 20px 40px rgba(52, 116, 246, 0.34);
  opacity: 0.71;
}

.rooms-filter-modal {
  --border-radius: 30px 30px 0 0;
  --box-shadow: 0 -24px 64px rgba(26, 36, 55, 0.18);
  --height: min(820px, 96vh);
}

.rooms-picker-modal {
  --width: 100%;
  --min-width: 100%;
  --max-width: 100%;
  --height: 100%;
  --min-height: 100%;
  --max-height: 100%;
  --border-radius: 0;
  --box-shadow: none;
}

.rooms-picker-modal::part(content) {
  border-radius: 0;
  box-shadow: none;
}

.rooms-sheet-header {
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.rooms-sheet-toolbar {
  --background: rgba(255, 255, 255, 0.9);
  --border-width: 0;
  --min-height: 60px;
  padding: 0 6px;
}

.rooms-sheet-toolbar ion-title {
  color: #14213d;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.rooms-sheet-toolbar ion-button {
  --color: #6d7d96;
  font-size: 13px;
  font-weight: 600;
}

.rooms-picker-header {
  background: #ffffff;
  box-shadow: 0 1px 6px rgba(57, 76, 108, 0.05);
}

.rooms-picker-toolbar {
  --background: #ffffff;
  --border-width: 0;
  --min-height: 56px;
  padding: 0 4px;
}

.rooms-picker-toolbar ion-title {
  padding: 0 52px;
  color: #2d2d2d;
  font-size: 21px;
  font-weight: 500;
  letter-spacing: 0;
  text-align: center;
}

.rooms-picker__close {
  width: 44px;
  height: 44px;
  margin: 0 2px 0 0;
  --color: #858585;
  --padding-start: 0;
  --padding-end: 0;
}

.rooms-picker__close ion-icon {
  font-size: 23px;
}

.rooms-filter-page {
  --padding-top: 14px;
  --padding-start: 16px;
  --padding-end: 16px;
  --padding-bottom: calc(env(safe-area-inset-bottom, 0px) + 18px);
}

.rooms-filter-stack {
  min-height: 100%;
  padding-bottom: calc(env(safe-area-inset-bottom, 0px) + 20px);
}

.rooms-picker-page {
  --background:
    linear-gradient(90deg, #eaf7ff 0%, #f5f8fd 52%, #f2f7ff 100%);
  --padding-top: 10px;
  --padding-start: 15px;
  --padding-end: 15px;
  --padding-bottom: calc(env(safe-area-inset-bottom, 0px) + 48px);
}

.rooms-picker-page::part(scroll) {
  padding-bottom: calc(env(safe-area-inset-bottom, 0px) + 48px);
}

.rooms-picker {
  display: grid;
  gap: 11px;
  min-height: 100%;
  padding-bottom: calc(env(safe-area-inset-bottom, 0px) + 22px);
}

.rooms-picker__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 0 0 1px;
}

.rooms-picker__summary-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(171, 187, 210, 0.16);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.88);
  color: #515151;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0;
}

.rooms-picker__summary-pill--accent {
  border-color: transparent;
  background: #dbeaff;
  color: #3d77b4;
}

.rooms-picker__group {
  display: grid;
  gap: 14px;
  padding: 18px 20px 19px;
  border: 1px solid rgba(190, 204, 223, 0.12);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
    0 3px 5px rgba(94, 116, 150, 0.06),
    0 12px 20px -7px rgba(92, 116, 157, 0.22);
}

.rooms-picker__group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 28px;
}

.rooms-picker__group-header strong {
  min-width: 0;
  color: #2e2e2e;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.rooms-picker__group-header span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  min-width: 39px;
  min-height: 28px;
  padding: 0 8px;
  border: 1px solid rgba(193, 202, 214, 0.24);
  border-radius: 10px;
  background: #ffffff;
  color: #4e4e4e;
  font-size: 13px;
  font-weight: 400;
}

.rooms-picker__group-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(132px, 132px));
  gap: 10px;
}

.rooms-picker__room {
  appearance: none;
  width: 132px;
  min-height: 76px;
  padding: 11px 13px 10px;
  border: 0;
  border-radius: 10px;
  background: #f7f7f8;
  box-shadow: none;
  display: grid;
  align-content: center;
  gap: 8px;
  text-align: left;
  color: #2d2d2d;
  transition:
    transform 0.16s ease,
    background-color 0.16s ease;
}

.rooms-picker__room:active {
  transform: translateY(1px);
  background: #eeeeef;
}

.rooms-picker__room-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.rooms-picker__room-number {
  flex: 1;
  min-width: 0;
  color: #397bbb;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rooms-picker__room-arrow {
  flex-shrink: 0;
  margin-left: auto;
  color: #666666;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.2;
}

.rooms-picker__room-bottom {
  display: flex;
  align-items: center;
}

.rooms-picker__room-state {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-width: 56px;
  min-height: 23px;
  padding: 0 9px;
  border-radius: 6px;
  border: 0;
  background: #e7ebf1;
  color: #68717c;
  font-size: 13px;
  font-weight: 400;
  line-height: 1;
}

.rooms-picker__room.is-available .rooms-picker__room-state {
  background: #cafabc;
  color: #4b9b3e;
}

.rooms-picker__room.is-occupied .rooms-picker__room-state,
.rooms-picker__room.is-reserved .rooms-picker__room-state {
  background: #fed4d5;
  color: #d9797e;
}

.rooms-picker__room.is-dirty .rooms-picker__room-state,
.rooms-picker__room.is-closed .rooms-picker__room-state,
.rooms-picker__room.is-out_of_order .rooms-picker__room-state,
.rooms-picker__room.is-maintenance .rooms-picker__room-state,
.rooms-picker__room.is-retain .rooms-picker__room-state {
  background: #ffe0e2;
  color: #cb646d;
}

.rooms-picker__empty {
  display: grid;
  gap: 8px;
  padding: 52px 8px 24px;
  text-align: center;
}

.rooms-picker__empty h3 {
  margin: 0;
  color: #14213d;
  font-size: 18px;
  font-weight: 800;
}

.rooms-picker__empty p {
  margin: 0;
  color: #6d7d96;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 374px) {
  .rooms-picker__group-grid {
    grid-template-columns: repeat(auto-fill, minmax(122px, 122px));
  }

  .rooms-picker__room {
    width: 122px;
  }

  .rooms-picker__group {
    padding-right: 16px;
    padding-left: 16px;
  }
}
</style>
