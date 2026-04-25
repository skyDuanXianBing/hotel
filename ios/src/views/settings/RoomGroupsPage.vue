<template>
  <SettingsCrudPage
    :back-href="ROUTE_PATHS.settings"
    title="房间分组"
    hero-eyebrow="住宿设置"
    hero-title="房间分组"
    :chips="[
      { label: `分组 ${groups.length}` },
      { label: `房间 ${rooms.length}` },
    ]"
    toolbar-action-label="新增"
    :show-refresher="true"
    refresher-pulling-text="下拉刷新房间分组"
    section-title="分组列表"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingGroupId ? '编辑房间分组' : '新增房间分组'"
    @toolbar-action="handleCreateGroup"
    @refresh="handleRefresh"
    @dismiss-editor="handleDismissEditor"
  >
    <div v-if="groups.length > 0" class="mobile-list settings-minimal-list">
      <article v-for="group in groups" :key="group.id" class="settings-minimal-card">
        <div class="settings-minimal-card__header">
          <div class="settings-minimal-card__title-group">
            <strong>{{ group.name }}</strong>
            <p class="settings-minimal-card__summary">{{ group.description || formatGroupPreview(group.memberRoomIds) }}</p>
          </div>
          <span class="settings-minimal-card__badge">房间 {{ group.memberRoomIds.length }}</span>
        </div>

        <div v-if="group.description && group.memberRoomIds.length > 0" class="settings-minimal-card__meta">
          <span class="settings-minimal-card__meta-pill">{{ formatGroupPreview(group.memberRoomIds) }}</span>
        </div>

        <div class="settings-minimal-card__actions">
          <ion-button size="small" fill="outline" @click="handleEditGroup(group)">编辑</ion-button>
          <ion-button size="small" color="danger" fill="clear" @click="handleDeleteGroup(group)">删除</ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note">当前暂无房间分组。</p>

    <template #modalContent>
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
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveGroup">
        {{ submitting ? '提交中...' : '保存分组' }}
      </ion-button>
    </template>
  </SettingsCrudPage>
</template>

<script setup lang="ts">
import {
  alertController,
  IonButton,
  IonInput,
  IonSelect,
  IonSelectOption,
  IonTextarea,
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
import SettingsCrudPage from '@/components/settings/families/SettingsCrudPage.vue'
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

function formatGroupPreview(roomIds: number[]) {
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
    return `已关联 ${roomIds.length} 间房`
  }

  const preview = labels.slice(0, 3).join('、')
  if (labels.length > 3) {
    return `房间 ${preview} 等 ${labels.length} 间`
  }
  return `房间 ${preview}`
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
