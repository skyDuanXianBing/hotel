<template>
  <SettingsCrudPage
    :back-href="ROUTE_PATHS.settings"
    :title="$t('stage5.common.filters.roomGroup')"
    :hero-eyebrow="$t('settings.groups.accommodation')"
    :hero-title="$t('stage5.common.filters.roomGroup')"
    :chips="[
      { label: `${$t('channel.mobile.mapping.groups')} ${groups.length}` },
      { label: `${$t('accommodation.common.room')} ${rooms.length}` },
    ]"
    :toolbar-action-label="$t('settingsStage4.roomGroup.addGroup')"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.10')"
    :section-title="$t('stage5UiAttributes.31')"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingGroupId ? $t('stage5DynamicUi.65') : $t('stage5DynamicUi.37')"
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
          <span class="settings-minimal-card__badge">{{ $t('accommodation.common.room') }} {{ group.memberRoomIds.length }}</span>
        </div>

        <div v-if="group.description && group.memberRoomIds.length > 0" class="settings-minimal-card__meta">
          <span class="settings-minimal-card__meta-pill">{{ formatGroupPreview(group.memberRoomIds) }}</span>
        </div>

        <div class="settings-minimal-card__actions">
          <ion-button size="small" fill="outline" @click="handleEditGroup(group)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
          <ion-button size="small" color="danger" fill="clear" @click="handleDeleteGroup(group)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note">{{ $t('stage5SourceText.79') }}</p>

    <template #modalContent>
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.roomGroup.placeholders.groupName') }}</span>
          <ion-input v-model="groupForm.name" fill="outline" :placeholder="$t('settingsStage4.roomGroup.messages.groupNameRequired')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.20') }}</span>
          <ion-textarea v-model="groupForm.description" :rows="4" fill="outline" :placeholder="$t('stage5UiAttributes.65')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.107') }}</span>
          <ion-select v-model="groupForm.roomIds" fill="outline" interface="modal" multiple>
            <ion-select-option v-for="room in rooms" :key="room.id" :value="room.id">
              {{ room.roomNumber }} · {{ room.roomType.name }}
            </ion-select-option>
          </ion-select>
        </label>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveGroup">
        {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.8') }}
      </ion-button>
    </template>
  </SettingsCrudPage>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()

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
    return t('settingsResidual.common.noRooms')
  }

  const labels: string[] = []
  for (const roomId of roomIds) {
    const matched = rooms.value.find((room) => room.id === roomId)
    if (matched) {
      labels.push(matched.roomNumber)
    }
  }

  if (labels.length === 0) {
    return t('settingsResidual.common.roomsLinked', { count: roomIds.length })
  }

  const preview = labels.slice(0, 3).join('、')
  if (labels.length > 3) {
    return t('settingsResidual.common.roomPreview', {
      value: `${preview} · ${labels.length}`,
    })
  }
  return t('settingsResidual.common.roomPreview', { value: preview })
}

async function confirmDelete(name: string) {
  const alert = await alertController.create({
    header: t('settingsStage4.roomGroup.deleteGroup'),
    message: t('settingsResidual.common.confirmDelete', { name }),
    buttons: [
      { text: t('accommodation.common.cancel'), role: 'cancel' },
      { text: t('settingsStage4.roomSettings.messages.deleteTitle'), role: 'destructive' },
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
      throw new Error(groupResponse.message || t('settingsStage4.roomSort.messages.loadGroupsFailed'))
    }
    if (!roomResponse.success || !roomResponse.data) {
      throw new Error(roomResponse.message || t('settingsStage4.roomSort.messages.loadRoomsFailed'))
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
      showWarningToast(resolveWarningMessage(error, t('settingsStage4.roomGroup.messages.loadGroupsFailed')))
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
    showWarningToast(t('settingsStage4.roomGroup.messages.groupNameRequired'))
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
        throw new Error(response.message || t('stage5Pattern.updateFailed'))
      }
    } else {
      const response = await createRoomGroup({
        name: groupForm.value.name.trim(),
        description: groupForm.value.description.trim(),
      })
      if (!response.success || !response.data?.id) {
        throw new Error(response.message || t('stage5Pattern.createFailed'))
      }
      groupId = Number(response.data.id)
    }

    if (!groupId) {
      throw new Error(t('stage5Pattern.missing'))
    }

    const nextRoomIds = [...groupForm.value.roomIds]
    const roomsToAdd = nextRoomIds.filter((id) => !currentRoomIds.includes(id))
    const roomsToRemove = currentRoomIds.filter((id) => !nextRoomIds.includes(id))

    if (roomsToAdd.length > 0) {
      const addResponse = await addRoomsToGroup(groupId, { roomIds: roomsToAdd })
      if (!addResponse.success) {
        throw new Error(addResponse.message || t('stage5Pattern.createFailed'))
      }
    }

    if (roomsToRemove.length > 0) {
      const removeResponse = await removeRoomsFromGroup(groupId, { roomIds: roomsToRemove })
      if (!removeResponse.success) {
        throw new Error(removeResponse.message || t('stage5Pattern.deleteFailed'))
      }
    }

    showSuccessToast(t('stage5Pattern.saveCompleted'))
    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
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
      throw new Error(response.message || t('stage5Pattern.deleteFailed'))
    }
    showSuccessToast(t('stage5Pattern.deleteCompleted'))
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.deleteFailed')))
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
