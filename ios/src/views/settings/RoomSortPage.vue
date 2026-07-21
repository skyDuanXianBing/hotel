<template>
  <SettingsSortablePage
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.entries.roomSort.0')"
    :hero-eyebrow="$t('settings.groups.accommodation')"
    :hero-title="$t('settings.entries.roomSort.0')"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.11')"
    :section-title="$t('stage5UiAttributes.41')"
    :loading="loading"
    @refresh="handleRefresh"
  >
    <template #controls>
      <ion-segment :value="activeSegment" @ionChange="handleSegmentChange">
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
    </template>

    <div v-if="currentItems.length > 0" class="mobile-list settings-card-list">
      <article v-for="(item, index) in currentItems" :key="item.id" class="settings-card-item settings-card-item--compact">
        <div>
          <strong>{{ item.name }}</strong>
          <p>{{ item.description }}</p>
        </div>

        <div class="settings-card-item__actions settings-card-item__actions--vertical">
          <ion-button size="small" fill="outline" :disabled="index === 0" @click="handleMove(index, -1)">{{ $t('stage5SourceText.3') }}</ion-button>
          <ion-button size="small" fill="outline" :disabled="index === currentItems.length - 1" @click="handleMove(index, 1)">{{ $t('stage5SourceText.4') }}</ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note">{{ $t('stage5SourceText.57') }}</p>

    <template #sectionFooter>
      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || saving" @click="loadPageData">{{ $t('accommodation.common.reset') }}</ion-button>
        <ion-button :disabled="loading || saving || currentItems.length === 0" @click="handleSaveSortOrder">
          {{ saving ? $t('channel.mobile.common.saving') : $t('stage5DynamicUi.24') }}
        </ion-button>
      </div>
    </template>
  </SettingsSortablePage>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  IonButton,
  IonLabel,
  IonSegment,
  IonSegmentButton,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { getAllRoomGroups } from '@/api/roomGroup'
import { getRooms } from '@/api/rooms'
import { getSortOrderMap, updateSortOrders } from '@/api/sortConfig'
import { getAllRoomTypes } from '@/api/roomType'
import SettingsSortablePage from '@/components/settings/families/SettingsSortablePage.vue'
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
.settings-card-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.settings-card-item__actions--vertical {
  flex-direction: column;
  align-items: stretch;
}
</style>
