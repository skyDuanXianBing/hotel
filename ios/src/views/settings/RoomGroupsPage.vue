<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>房间分组</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleCreateGroup">新增</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page-block">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新房间分组" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-page-block__hero">
        <p class="mobile-note settings-page-block__eyebrow">住宿设置</p>
        <h1 class="mobile-title">房间分组</h1>
        <p class="mobile-subtitle">支持新增、编辑、删除分组，并通过多选房间维护成员。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">分组 {{ groups.length }}</span>
          <span class="mobile-chip">房间 {{ rooms.length }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-page-block__section-header">
            <div>
              <h2 class="mobile-section-title">分组列表</h2>
              <p class="mobile-note">移动端保留卡片摘要，不搬运桌面大表格。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="groups.length > 0" class="mobile-list settings-card-list">
            <article v-for="group in groups" :key="group.id" class="settings-card-item">
              <div class="settings-card-item__header">
                <div>
                  <strong>{{ group.name }}</strong>
                  <p>{{ group.description || '未填写分组说明' }}</p>
                </div>
                <span class="settings-card-item__badge">房间 {{ group.memberRoomIds.length }}</span>
              </div>

              <p class="mobile-note">{{ formatGroupMembers(group.memberRoomIds) }}</p>

              <div class="settings-card-item__actions">
                <ion-button size="small" fill="outline" @click="handleEditGroup(group)">编辑</ion-button>
                <ion-button size="small" color="danger" fill="clear" @click="handleDeleteGroup(group)">删除</ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前暂无房间分组。</p>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleDismissEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingGroupId ? '编辑房间分组' : '新增房间分组' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <div class="settings-form-grid">
              <label class="settings-form-field">
                <span>分组名称</span>
                <ion-input v-model="groupForm.name" fill="outline" placeholder="请输入分组名称" />
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>分组说明</span>
                <ion-textarea v-model="groupForm.description" :rows="4" fill="outline" placeholder="请输入分组说明" />
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>房间成员</span>
                <ion-select v-model="groupForm.roomIds" fill="outline" interface="modal" multiple>
                  <ion-select-option v-for="room in rooms" :key="room.id" :value="room.id">
                    {{ room.roomNumber }} · {{ room.roomType.name }}
                  </ion-select-option>
                </ion-select>
              </label>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveGroup">
                {{ submitting ? '提交中...' : '保存分组' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
import {
  addRoomsToGroup,
  createRoomGroup,
  deleteRoomGroup,
  getAllRoomGroups,
  getGroupMembers,
  removeRoomsFromGroup,
  updateRoomGroup,
} from '@/api/roomGroup'
import { getRooms } from '@/api/rooms'
import { ROUTE_PATHS } from '@/router/guards'
import type { RoomDTO, RoomGroupDTO } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

interface RoomGroupView extends RoomGroupDTO {
  id: number
  memberRoomIds: number[]
}

interface RoomGroupFormState {
  name: string
  description: string
  roomIds: number[]
}

const loading = ref(false)
const submitting = ref(false)
const editorOpen = ref(false)
const editingGroupId = ref<number | null>(null)
const groups = ref<RoomGroupView[]>([])
const rooms = ref<RoomDTO[]>([])
const groupForm = ref<RoomGroupFormState>(createEmptyForm())

function createEmptyForm(): RoomGroupFormState {
  return {
    name: '',
    description: '',
    roomIds: [],
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function formatGroupMembers(roomIds: number[]) {
  if (roomIds.length === 0) {
    return '当前分组未分配房间'
  }

  const labels: string[] = []
  for (const roomId of roomIds) {
    const matched = rooms.value.find((room) => room.id === roomId)
    if (matched) {
      labels.push(matched.roomNumber)
    }
  }

  if (labels.length === 0) {
    return '已分配房间，但列表尚未恢复'
  }

  return labels.join('、')
}

async function confirmDelete(name: string) {
  const alert = await alertController.create({
    header: '删除分组',
    message: `确认删除 ${name} 吗？`,
    buttons: [
      { text: '取消', role: 'cancel' },
      { text: '确认删除', role: 'destructive' },
    ],
  })
  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadPageData() {
  loading.value = true
  try {
    const [groupResponse, roomResponse] = await Promise.all([getAllRoomGroups(), getRooms()])
    if (!groupResponse.success || !groupResponse.data) {
      throw new Error(groupResponse.message || '加载分组失败')
    }
    if (!roomResponse.success || !roomResponse.data) {
      throw new Error(roomResponse.message || '加载房间失败')
    }

    rooms.value = roomResponse.data
    const nextGroups: RoomGroupView[] = []
    for (const group of groupResponse.data) {
      const memberResponse = group.id ? await getGroupMembers(group.id) : null
      nextGroups.push({
        ...group,
        id: Number(group.id),
        memberRoomIds: memberResponse?.success && memberResponse.data ? memberResponse.data.map((item) => item.roomId) : [],
      })
    }
    groups.value = nextGroups
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载房间分组失败'))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateGroup() {
  editingGroupId.value = null
  groupForm.value = createEmptyForm()
  editorOpen.value = true
}

function handleEditGroup(group: RoomGroupView) {
  editingGroupId.value = group.id
  groupForm.value = {
    name: group.name,
    description: group.description || '',
    roomIds: [...group.memberRoomIds],
  }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingGroupId.value = null
  groupForm.value = createEmptyForm()
}

async function handleSaveGroup() {
  if (!groupForm.value.name.trim()) {
    showWarningToast('请输入分组名称')
    return
  }

  submitting.value = true
  try {
    let groupId = editingGroupId.value
    let currentRoomIds: number[] = []

    if (groupId) {
      const target = groups.value.find((item) => item.id === groupId)
      currentRoomIds = target?.memberRoomIds || []
      const response = await updateRoomGroup(groupId, {
        name: groupForm.value.name.trim(),
        description: groupForm.value.description.trim(),
      })
      if (!response.success || !response.data) {
        throw new Error(response.message || '更新分组失败')
      }
    } else {
      const response = await createRoomGroup({
        name: groupForm.value.name.trim(),
        description: groupForm.value.description.trim(),
      })
      if (!response.success || !response.data?.id) {
        throw new Error(response.message || '创建分组失败')
      }
      groupId = Number(response.data.id)
    }

    if (!groupId) {
      throw new Error('未获取到分组 ID')
    }

    const nextRoomIds = [...groupForm.value.roomIds]
    const roomsToAdd = nextRoomIds.filter((id) => !currentRoomIds.includes(id))
    const roomsToRemove = currentRoomIds.filter((id) => !nextRoomIds.includes(id))

    if (roomsToAdd.length > 0) {
      const addResponse = await addRoomsToGroup(groupId, { roomIds: roomsToAdd })
      if (!addResponse.success) {
        throw new Error(addResponse.message || '添加房间失败')
      }
    }

    if (roomsToRemove.length > 0) {
      const removeResponse = await removeRoomsFromGroup(groupId, { roomIds: roomsToRemove })
      if (!removeResponse.success) {
        throw new Error(removeResponse.message || '移除房间失败')
      }
    }

    showSuccessToast('房间分组已保存')
    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存房间分组失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleDeleteGroup(group: RoomGroupView) {
  const confirmed = await confirmDelete(group.name)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteRoomGroup(group.id)
    if (!response.success) {
      throw new Error(response.message || '删除分组失败')
    }
    showSuccessToast('房间分组已删除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除分组失败'))
    }
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
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.settings-card-item__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.settings-card-item__header strong,
.settings-card-item__header p {
  margin: 0;
}

.settings-card-item__header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.settings-card-item__badge {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.settings-card-item__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.settings-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-form-grid {
  display: grid;
  gap: 14px;
}

.settings-form-field {
  display: grid;
  gap: 8px;
}

.settings-form-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.settings-form-field--full {
  grid-column: 1 / -1;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}
</style>
