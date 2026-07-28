<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.SettingsRoomGroups') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button
            class="app-page-header__text-btn settings-room-groups-header-add"
            fill="clear"
            @click="handleCreateGroup"
          >
            <span class="settings-room-groups-header-add__text">{{ $t('settingsStage4.roomGroup.addGroup') }}</span>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page mobile-page--dashboard settings-room-groups-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('stage5UiAttributes.10')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero mobile-dashboard-surface settings-room-groups-hero">
        <p class="mobile-note settings-room-groups-hero__eyebrow">
          {{ $t('settings.groups.accommodation') }}
        </p>
        <h1 class="mobile-title">{{ $t('routes.SettingsRoomGroups') }}</h1>
        <div class="mobile-chip-row settings-room-groups-hero__chips">
          <span class="mobile-chip">{{ $t('channel.mobile.mapping.groups') }} {{ groups.length }}</span>
          <span class="mobile-chip">{{ $t('accommodation.common.room') }} {{ rooms.length }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card mobile-dashboard-surface settings-room-groups-panel">
          <div class="mobile-inline-row settings-room-groups-page__section-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('stage5UiAttributes.31') }}</h2>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="groups.length > 0" class="mobile-list settings-room-groups-list">
            <article v-for="group in groups" :key="group.id" class="settings-room-group-card">
              <div class="settings-room-group-card__header">
                <div class="settings-room-group-card__title-group">
                  <strong>{{ group.name }}</strong>
                  <p class="settings-room-group-card__summary">
                    {{ group.description || formatGroupPreview(group.memberRoomIds) }}
                  </p>
                </div>
                <span class="settings-room-group-card__badge">
                  {{ group.memberRoomIds.length }} {{ $t('settingsStage4.common.unitRooms') }}
                </span>
              </div>

              <div
                v-if="group.description && group.memberRoomIds.length > 0"
                class="settings-room-group-card__meta"
              >
                <span class="settings-room-group-card__meta-pill">
                  {{ formatGroupPreview(group.memberRoomIds) }}
                </span>
              </div>

              <div class="settings-room-group-card__actions">
                <ion-button
                  size="small"
                  fill="outline"
                  class="settings-room-group-card__action"
                  @click="handleEditGroup(group)"
                >
                  {{ $t('accommodation.roomPrice.editTitle') }}
                </ion-button>
                <ion-button
                  size="small"
                  color="danger"
                  fill="clear"
                  class="settings-room-group-card__action settings-room-group-card__action--danger"
                  @click="handleDeleteGroup(group)"
                >
                  {{ $t('roomStatus.roomLock.actions.delete') }}
                </ion-button>
              </div>
            </article>
          </div>

          <div v-else-if="!loading" class="settings-room-groups-page__empty-state">
            <p class="mobile-note settings-room-groups-page__empty-text">
              {{ $t('stage5SourceText.79') }}
            </p>
            <ion-button @click="handleCreateGroup">{{ $t('settingsStage4.roomGroup.addGroup') }}</ion-button>
          </div>
        </section>
      </div>

      <SettingsEditorModal
        :is-open="editorOpen"
        :title="editingGroupId ? $t('stage5DynamicUi.65') : $t('stage5DynamicUi.37')"
        :backdrop-dismiss="!submitting"
        :close-disabled="submitting"
        @close="handleDismissEditor"
        @didDismiss="handleDismissEditor"
      >
        <div class="settings-form-section">
              <div>
                <h2 class="mobile-section-title">{{ $t('accommodation.cleaning.basicInfo') }}</h2>
              </div>

              <div class="settings-form-grid">
                <label class="settings-form-field">
                  <span>{{ $t('settingsStage4.roomGroup.placeholders.groupName') }}</span>
                  <ion-input
                    v-model="groupForm.name"
                    :disabled="submitting"
                    fill="outline"
                    :placeholder="$t('settingsStage4.roomGroup.messages.groupNameRequired')"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>{{ $t('stage5SourceText.20') }}</span>
                  <ion-textarea
                    v-model="groupForm.description"
                    :disabled="submitting"
                    :rows="4"
                    fill="outline"
                    :placeholder="$t('stage5UiAttributes.65')"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>{{ $t('stage5SourceText.107') }}</span>
                  <ion-select
                    v-model="groupForm.roomIds"
                    :disabled="submitting"
                    fill="outline"
                    interface="modal"
                    multiple
                  >
                    <ion-select-option v-for="room in rooms" :key="room.id" :value="room.id">
                      {{ room.roomNumber }} · {{ room.roomType.name }}
                    </ion-select-option>
                  </ion-select>
                </label>
              </div>
        </div>

        <template #actions>
          <ion-button fill="outline" :disabled="submitting" @click="handleDismissEditor">
            {{ $t('accommodation.common.cancel') }}
          </ion-button>
          <ion-button :disabled="submitting" @click="handleSaveGroup">
            {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.8') }}
          </ion-button>
        </template>
      </SettingsEditorModal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
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
import SettingsEditorModal from '@/components/settings/base/SettingsEditorModal.vue'
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
        memberRoomIds:
          memberResponse?.success && memberResponse.data
            ? memberResponse.data.map((item) => item.roomId)
            : [],
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
  try {
    await loadPageData()
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.settings-room-groups-page {
  display: block;
  --background: var(--app-background);
  --padding-top: 12px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
  background: var(--app-background);
}

ion-page > ion-header {
  backdrop-filter: blur(14px);
}

ion-page > ion-header::after {
  display: none;
}

ion-page > ion-header .app-page-header__text-btn {
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 0;
}

ion-page > ion-header .settings-room-groups-header-add {
  font-size: 17px !important;
  font-weight: 500;
  letter-spacing: 0;
}

ion-page > ion-header .settings-room-groups-header-add::part(native) {
  font-size: 17px;
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-room-groups-header-add__text {
  font-size: 17px;
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-room-groups-hero {
  margin-top: 0;
  padding: 17px 16px 21px;
  border-radius: var(--ios-pms-radius-card);
}

.settings-room-groups-hero::before {
  display: none;
}

.settings-room-groups-hero__eyebrow {
  display: none;
}

.settings-room-groups-hero .mobile-title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 22px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-room-groups-hero__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.settings-room-groups-hero__chips .mobile-chip {
  min-width: 0;
  min-height: 24px;
  padding: 2px 10px;
  border-color: rgba(var(--ion-color-primary-rgb), 0.1);
  background: rgba(var(--ion-color-primary-rgb), 0.07);
  color: rgba(var(--ion-color-primary-rgb), 0.88);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
  white-space: normal;
  overflow-wrap: anywhere;
}

.settings-room-groups-page > .mobile-stack {
  gap: 18px;
  margin-top: 10px;
  padding-bottom: 4px;
}

.settings-room-groups-panel {
  padding: 22px 16px 48px;
  border-radius: var(--ios-pms-radius-card);
}

.settings-room-groups-page__section-header {
  align-items: flex-start;
}

.settings-room-groups-page__section-header .mobile-section-title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 22px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-room-groups-page__section-header ion-spinner {
  flex-shrink: 0;
  color: var(--ios-pms-primary);
}

.settings-room-groups-list {
  margin-top: 21px;
  gap: 17px;
}

.settings-room-group-card {
  position: relative;
  overflow: visible;
  padding: 14px 15px 14px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.88);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.88) inset,
    0 8px 18px rgba(77, 98, 145, 0.035);
}

.settings-room-group-card::before {
  display: none;
}

.settings-room-group-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  position: relative;
  z-index: 1;
}

.settings-room-group-card__title-group {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.settings-room-group-card__header strong,
.settings-room-group-card__summary {
  margin: 0;
}

.settings-room-group-card__header strong {
  color: var(--ios-pms-text-primary);
  font-size: 20px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.15;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.settings-room-group-card__summary {
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.35;
  letter-spacing: 0;
}

.settings-room-group-card__badge {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  margin-top: -2px;
  margin-right: -1px;
  min-height: 26px;
  padding: 0 10px;
  border-radius: var(--ios-pms-radius-pill);
  border: 1px solid rgba(130, 143, 165, 0.18);
  background: rgba(255, 255, 255, 0.86);
  color: var(--ios-pms-text-secondary);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
  white-space: nowrap;
}

.settings-room-group-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
  position: relative;
  z-index: 1;
}

.settings-room-group-card__meta-pill {
  display: inline-flex;
  align-items: center;
  min-height: 27px;
  padding: 2px 10px;
  border-radius: var(--ios-pms-radius-pill);
  border: 1px solid rgba(130, 143, 165, 0.22);
  background: rgba(255, 255, 255, 0.84);
  color: var(--ios-pms-text-secondary);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
  white-space: nowrap;
}

.settings-room-group-card__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
  position: relative;
  z-index: 1;
}

.settings-room-group-card__action {
  margin: 0;
  min-height: 29px;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 9px;
  --box-shadow: none;
  font-size: 14px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.settings-room-group-card__action::part(native) {
  min-height: 29px;
  border: 1px solid rgba(130, 143, 165, 0.24);
  border-radius: 9px;
  box-shadow: none;
  line-height: 1.2;
}

.settings-room-group-card__action[fill='outline'] {
  --background: rgba(255, 255, 255, 0.88);
  --color: var(--ios-pms-primary);
  --border-color: rgba(130, 143, 165, 0.24);
}

.settings-room-group-card__action--danger {
  --background: rgba(255, 255, 255, 0.88);
  --color: #ff1f1f;
}

.settings-room-groups-page__empty-state {
  display: grid;
  gap: 12px;
  justify-items: flex-start;
  padding-top: 28px;
}

.settings-room-groups-page__empty-text {
  margin: 0;
}

@media (max-width: 374px) {
  .settings-room-groups-page {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .settings-room-groups-hero,
  .settings-room-groups-panel {
    padding-left: 14px;
    padding-right: 14px;
  }

  .settings-room-group-card {
    padding: 13px 13px 14px;
  }

  .settings-room-group-card__header strong {
    font-size: 19px;
  }

  .settings-room-group-card__action {
    font-size: 13px;
  }
}

.settings-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-editor-card,
.settings-form-section,
.settings-form-grid {
  display: grid;
}

.settings-editor-card {
  gap: 0;
}

.settings-form-section {
  gap: 14px;
}

.settings-form-section + .settings-form-actions {
  border-top: 1px solid var(--app-border);
  margin-top: 12px;
  padding-top: 16px;
}

.settings-form-grid {
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
}
</style>
