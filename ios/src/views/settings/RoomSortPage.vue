<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>排序设置</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page-block">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新排序设置" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-page-block__hero">
        <p class="mobile-note settings-page-block__eyebrow">住宿设置</p>
        <h1 class="mobile-title">排序设置</h1>
        <p class="mobile-subtitle">使用上移 / 下移替代桌面拖拽排序，覆盖房型、房间、分组三类实体。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <ion-segment :value="activeSegment" @ionChange="handleSegmentChange">
            <ion-segment-button value="ROOM_TYPE">
              <ion-label>房型</ion-label>
            </ion-segment-button>
            <ion-segment-button value="ROOM">
              <ion-label>房间</ion-label>
            </ion-segment-button>
            <ion-segment-button value="GROUP">
              <ion-label>分组</ion-label>
            </ion-segment-button>
          </ion-segment>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row settings-page-block__section-header">
            <div>
              <h2 class="mobile-section-title">当前顺序</h2>
              <p class="mobile-note">保存后会写入当前用户的排序配置。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="currentItems.length > 0" class="mobile-list settings-card-list">
            <article v-for="(item, index) in currentItems" :key="item.id" class="settings-card-item settings-card-item--compact">
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ item.description }}</p>
              </div>

              <div class="settings-card-item__actions settings-card-item__actions--vertical">
                <ion-button size="small" fill="outline" :disabled="index === 0" @click="handleMove(index, -1)">上移</ion-button>
                <ion-button size="small" fill="outline" :disabled="index === currentItems.length - 1" @click="handleMove(index, 1)">下移</ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前分类暂无可排序数据。</p>

          <div class="settings-form-actions settings-form-actions--section">
            <ion-button fill="outline" :disabled="loading || saving" @click="loadPageData">重置</ion-button>
            <ion-button :disabled="loading || saving || currentItems.length === 0" @click="handleSaveSortOrder">
              {{ saving ? '保存中...' : '保存顺序' }}
            </ion-button>
          </div>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
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
    return left.name.localeCompare(right.name, 'zh-CN')
  })
  return nextItems
}

async function loadPageData() {
  const userId = userStore.currentUser?.id
  if (!userId) {
    showWarningToast('请先恢复当前用户信息')
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
      throw new Error(roomTypeResponse.message || '加载房型失败')
    }
    if (!roomResponse.success || !roomResponse.data) {
      throw new Error(roomResponse.message || '加载房间失败')
    }
    if (!groupResponse.success || !groupResponse.data) {
      throw new Error(groupResponse.message || '加载分组失败')
    }

    const roomTypeSortMap = roomTypeMapResponse.success && roomTypeMapResponse.data ? roomTypeMapResponse.data : {}
    const roomSortMap = roomMapResponse.success && roomMapResponse.data ? roomMapResponse.data : {}
    const groupSortMap = groupMapResponse.success && groupMapResponse.data ? groupMapResponse.data : {}

    roomTypeItems.value = sortWithOrderMap(
      roomTypeResponse.data.map((item) => ({ id: item.id, name: item.name, description: item.code || '房型代码未设置' })),
      roomTypeSortMap,
    )
    roomItems.value = sortWithOrderMap(
      roomResponse.data.map((item: RoomDTO) => ({ id: item.id, name: item.roomNumber, description: item.roomType.name })),
      roomSortMap,
    )
    groupItems.value = sortWithOrderMap(
      groupResponse.data.map((item: RoomGroupDTO) => ({ id: Number(item.id), name: item.name, description: item.description || '未填写说明' })),
      groupSortMap,
    )
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载排序设置失败'))
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
    showWarningToast('请先恢复当前用户信息')
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
      throw new Error(response.message || '保存排序失败')
    }

    showSuccessToast('排序设置已保存')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存排序失败'))
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
.settings-page-block {
  display: block;
}

.settings-page-block__hero {
  margin-top: 4px;
}

.settings-page-block__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-page-block__section-header {
  align-items: flex-start;
}

.settings-card-list {
  margin-top: 16px;
}

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

.settings-card-item strong,
.settings-card-item p {
  margin: 0;
}

.settings-card-item p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.settings-card-item__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.settings-card-item__actions--vertical {
  flex-direction: column;
  align-items: stretch;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.settings-form-actions--section {
  margin-top: 18px;
}
</style>
