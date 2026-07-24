<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('settings.entries.roomSort.0') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page mobile-page--dashboard room-sort-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('stage5UiAttributes.11')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="room-sort-tabs" :aria-label="$t('settings.entries.roomSort.0')">
        <ion-segment class="room-sort-segment" :value="activeSegment" @ionChange="handleSegmentChange">
          <ion-segment-button value="ROOM_TYPE">
            <ion-label>{{ $t('accommodation.common.roomType') }}</ion-label>
          </ion-segment-button>
          <ion-segment-button value="ROOM">
            <ion-label>{{ $t('accommodation.common.room') }}</ion-label>
          </ion-segment-button>
          <ion-segment-button value="GROUP">
            <ion-label>{{ $t('channel.mobile.mapping.groups') }}</ion-label>
          </ion-segment-button>
        </ion-segment>
      </section>

      <section class="mobile-card mobile-dashboard-surface room-sort-order-card">
        <div class="room-sort-order-card__header">
          <h2 class="room-sort-order-card__title">{{ $t('stage5UiAttributes.41') }}</h2>
          <ion-spinner v-if="loading" name="crescent" />
        </div>

        <div v-if="currentItems.length > 0" class="mobile-list room-sort-list">
          <article v-for="(item, index) in currentItems" :key="item.id" class="room-sort-item">
            <div class="room-sort-item__content">
              <span class="room-sort-item__order">{{ $t('stage5DynamicUi.142') }} {{ index + 1 }}</span>
              <strong>{{ item.name }}</strong>
              <p>{{ item.description }}</p>
            </div>

            <div class="room-sort-item__actions">
              <ion-button size="small" fill="outline" :disabled="index === 0" @click="handleMove(index, -1)">
                {{ $t('stage5SourceText.3') }}
              </ion-button>
              <ion-button size="small" fill="outline" :disabled="index === currentItems.length - 1" @click="handleMove(index, 1)">
                {{ $t('stage5SourceText.4') }}
              </ion-button>
            </div>
          </article>
        </div>

        <p v-else-if="!loading" class="mobile-note room-sort-empty">{{ $t('stage5SourceText.57') }}</p>

        <div class="room-sort-save-actions">
          <ion-button fill="outline" :disabled="loading || saving" @click="loadPageData">{{ $t('accommodation.common.reset') }}</ion-button>
          <ion-button :disabled="loading || saving || currentItems.length === 0" @click="handleSaveSortOrder">
            {{ saving ? $t('channel.mobile.common.saving') : $t('stage5DynamicUi.24') }}
          </ion-button>
        </div>
      </section>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
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
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { getAllRoomGroups } from '@/api/roomGroup'
import { getRooms } from '@/api/rooms'
import { getSortOrderMap, updateSortOrders } from '@/api/sortConfig'
import { getAllRoomTypes } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import type { RoomDTO, RoomGroupDTO, SortEntityType } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { moveArrayItem } from '@/utils/settings'
import { compareLocalizedText } from '@/utils/formatters'

const { t } = useI18n()

interface SortItem {
  id: number
  name: string
  description: string
}

const userStore = useUserStore()

const activeSegment = ref<SortEntityType>('ROOM_TYPE')
const loading = ref(false)
const saving = ref(false)
const roomTypeItems = ref<SortItem[]>([])
const roomItems = ref<SortItem[]>([])
const groupItems = ref<SortItem[]>([])

const currentItems = computed(() => {
  if (activeSegment.value === 'ROOM') {
    return roomItems.value
  }
  if (activeSegment.value === 'GROUP') {
    return groupItems.value
  }
  return roomTypeItems.value
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function sortWithOrderMap<T extends { id: number; name: string }>(items: T[], sortMap: Record<number, number>) {
  const nextItems = [...items]
  nextItems.sort((left, right) => {
    const leftOrder = sortMap[left.id]
    const rightOrder = sortMap[right.id]
    if (typeof leftOrder === 'number' && typeof rightOrder === 'number') {
      return leftOrder - rightOrder
    }
    if (typeof leftOrder === 'number') {
      return -1
    }
    if (typeof rightOrder === 'number') {
      return 1
    }
    return compareLocalizedText(left.name, right.name)
  })
  return nextItems
}

async function loadPageData() {
  const userId = userStore.currentUser?.id
  if (!userId) {
    showWarningToast(t('stage5Pattern.setup'))
    return
  }

  loading.value = true
  try {
    const [roomTypeResponse, roomResponse, groupResponse, roomTypeMapResponse, roomMapResponse, groupMapResponse] = await Promise.all([
      getAllRoomTypes(),
      getRooms(),
      getAllRoomGroups(),
      getSortOrderMap(userId, 'ROOM_TYPE'),
      getSortOrderMap(userId, 'ROOM'),
      getSortOrderMap(userId, 'GROUP'),
    ])

    if (!roomTypeResponse.success || !roomTypeResponse.data) {
      throw new Error(roomTypeResponse.message || t('settingsStage4.roomSort.messages.loadRoomTypesFailed'))
    }
    if (!roomResponse.success || !roomResponse.data) {
      throw new Error(roomResponse.message || t('settingsStage4.roomSort.messages.loadRoomsFailed'))
    }
    if (!groupResponse.success || !groupResponse.data) {
      throw new Error(groupResponse.message || t('settingsStage4.roomSort.messages.loadGroupsFailed'))
    }

    const roomTypeSortMap = roomTypeMapResponse.success && roomTypeMapResponse.data ? roomTypeMapResponse.data : {}
    const roomSortMap = roomMapResponse.success && roomMapResponse.data ? roomMapResponse.data : {}
    const groupSortMap = groupMapResponse.success && groupMapResponse.data ? groupMapResponse.data : {}

    roomTypeItems.value = sortWithOrderMap(
      roomTypeResponse.data.map((item) => ({
        id: item.id,
        name: item.name,
        description: item.code || t('settingsResidual.common.notConfigured'),
      })),
      roomTypeSortMap,
    )
    roomItems.value = sortWithOrderMap(
      roomResponse.data.map((item: RoomDTO) => ({ id: item.id, name: item.roomNumber, description: item.roomType.name })),
      roomSortMap,
    )
    groupItems.value = sortWithOrderMap(
      groupResponse.data.map((item: RoomGroupDTO) => ({
        id: Number(item.id),
        name: item.name,
        description: item.description || t('settingsResidual.common.notConfigured'),
      })),
      groupSortMap,
    )
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.loadFailed')))
    }
  } finally {
    loading.value = false
  }
}

function handleSegmentChange(event: CustomEvent) {
  activeSegment.value = event.detail.value as SortEntityType
}

function handleMove(index: number, delta: number) {
  const nextIndex = index + delta
  if (activeSegment.value === 'ROOM') {
    roomItems.value = moveArrayItem(roomItems.value, index, nextIndex)
    return
  }
  if (activeSegment.value === 'GROUP') {
    groupItems.value = moveArrayItem(groupItems.value, index, nextIndex)
    return
  }
  roomTypeItems.value = moveArrayItem(roomTypeItems.value, index, nextIndex)
}

async function handleSaveSortOrder() {
  const userId = userStore.currentUser?.id
  if (!userId) {
    showWarningToast(t('stage5Pattern.setup'))
    return
  }

  saving.value = true
  try {
    const entityIds = currentItems.value.map((item) => item.id)
    const response = await updateSortOrders(userId, {
      sortType: activeSegment.value,
      entityIds,
    })

    if (!response.success) {
      throw new Error(response.message || t('settingsStage4.roomSort.messages.saveSortFailed'))
    }

    showSuccessToast(t('stage5Pattern.saveCompleted'))
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('settingsStage4.roomSort.messages.saveSortFailed')))
    }
  } finally {
    saving.value = false
  }
}

async function handleRefresh(event: CustomEvent) {
  await loadPageData()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.room-sort-page {
  display: block;
  --background: var(--app-background);
  --padding-top: 20px;
  --padding-bottom: calc(32px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
  background: var(--app-background);
}

.room-sort-tabs {
  margin: 0 0 20px;
}

.room-sort-segment {
  width: 100%;
  height: 34px;
  min-height: 34px;
  padding: 0;
  overflow: hidden;
  border: none;
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.room-sort-segment ion-segment-button {
  --border-radius: var(--ios-pms-radius-pill);
  --color: #050505;
  --color-checked: #ffffff;
  --indicator-color: #343436;
  --indicator-box-shadow: none;
  --padding-start: 6px;
  --padding-end: 6px;
  min-width: 0;
  min-height: 100%;
  height: 100%;
  margin: 0;
  color: #050505;
  font-size: 16px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0;
}

.room-sort-segment ion-segment-button::part(native) {
  min-height: 100%;
  padding: 0 2px;
  border-radius: var(--ios-pms-radius-pill);
}

.room-sort-segment ion-segment-button.segment-button-checked,
.room-sort-segment ion-segment-button.segment-button-checked ion-label,
.room-sort-segment ion-segment-button.segment-button-checked::part(native) {
  color: #ffffff !important;
}

.room-sort-segment ion-segment-button::part(indicator) {
  padding: 0;
}

.room-sort-segment ion-segment-button::part(indicator-background) {
  border-radius: var(--ios-pms-radius-pill);
  background: #343436;
  box-shadow: none;
}

.room-sort-segment ion-label {
  margin: 0;
  line-height: 1.2;
  white-space: normal;
}

.room-sort-order-card {
  display: grid;
  gap: 18px;
  width: 100%;
  box-sizing: border-box;
  padding: 28px 16px 22px;
  border-radius: var(--ios-pms-radius-card);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.room-sort-order-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.room-sort-order-card__title {
  margin: 0;
  color: #30343a;
  font-size: 22px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.22;
  letter-spacing: 0;
}

.room-sort-order-card__header ion-spinner {
  flex-shrink: 0;
  color: var(--ios-pms-primary);
}

.room-sort-list {
  gap: 16px;
}

.room-sort-item {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: start;
  gap: 12px;
  min-height: 96px;
  padding: 16px 86px 16px 16px;
  border: 1px solid rgba(130, 143, 165, 0.18);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.room-sort-item__content {
  min-width: 0;
}

.room-sort-item__order {
  display: block;
  margin: 0 0 8px;
  color: #7d828a;
  font-size: 16px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

.room-sort-item strong {
  display: block;
  min-width: 0;
  margin: 0;
  color: #30343a;
  font-size: 19px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.24;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.room-sort-item p {
  min-width: 0;
  margin: 4px 0 0;
  color: #7f858d;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.room-sort-item__actions {
  position: absolute;
  right: 14px;
  bottom: 16px;
  display: grid;
  gap: 6px;
  justify-items: end;
}

.room-sort-item__actions ion-button {
  width: 58px;
  height: 28px;
  min-height: 28px;
  margin: 0;
  --padding-start: 0;
  --padding-end: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: var(--ios-pms-radius-pill);
  --border-color: rgba(102, 112, 128, 0.22);
  --border-width: 1px;
  --background: rgba(255, 255, 255, 0.74);
  --background-hover: rgba(255, 255, 255, 0.9);
  --background-activated: rgba(52, 116, 246, 0.08);
  --box-shadow: none;
  --color: #2756ff;
  color: #2756ff;
  font-size: 15px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0;
}

.room-sort-item__actions ion-button::part(native) {
  padding: 0;
}

.room-sort-item__actions ion-button.button-disabled {
  opacity: 0.42;
}

.room-sort-empty {
  padding: 10px 0 2px;
  color: var(--ios-pms-text-muted);
  font-size: 14px;
}

.room-sort-save-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 2px;
  padding-top: 16px;
  border-top: 1px solid rgba(130, 143, 165, 0.14);
}

.room-sort-save-actions ion-button {
  min-height: 36px;
  margin: 0;
  --padding-start: 16px;
  --padding-end: 16px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: var(--ios-pms-radius-pill);
  --box-shadow: none;
  font-size: 14px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0;
}

.room-sort-save-actions ion-button[fill='outline'] {
  --background: rgba(255, 255, 255, 0.76);
  --border-color: rgba(52, 116, 246, 0.18);
}

@media (max-width: 374px) {
  .room-sort-page {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .room-sort-segment ion-segment-button {
    font-size: 14px;
  }

  .room-sort-order-card {
    padding: 24px 14px 20px;
  }

  .room-sort-item {
    gap: 10px;
    padding-right: 78px;
    padding-left: 14px;
  }

  .room-sort-item strong {
    font-size: 17px;
  }

  .room-sort-item__actions ion-button {
    width: 54px;
    font-size: 14px;
  }
}
</style>
