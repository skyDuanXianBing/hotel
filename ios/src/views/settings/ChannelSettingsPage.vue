<template>
  <SettingsCrudPage
    class="settings-channel-settings-page"
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.entries.channelSettings.0')"
    :hero-eyebrow="$t('settings.groups.general')"
    :hero-title="$t('settings.entries.channelSettings.0')"
    :toolbar-action-label="$t('settingsStage4.roomGroup.addGroup')"
    :section-title="$t('channel.sidebar.list')"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingChannelId ? $t('stage5DynamicUi.67') : $t('stage5DynamicUi.40')"
    modal-class="settings-channel-editor-modal"
    modal-content-class="settings-channel-editor-page"
    modal-card-class="settings-channel-editor-card"
    :modal-close-text="$t('common.back')"
    modal-close-slot="start"
    @toolbar-action="handleCreateChannel"
    @dismiss-editor="handleDismissEditor"
  >
    <div v-if="channels.length > 0" class="mobile-list settings-channel-list">
      <article v-for="channel in channels" :key="channel.id" class="settings-channel-card">
        <div class="settings-channel-card__header">
          <div class="settings-channel-card__title-group">
            <div class="settings-channel-card__title-row">
              <span
                class="settings-channel-card__swatch"
                :style="{ backgroundColor: channel.color || CHANNEL_COLOR_OPTIONS[0].value }"
              />
              <strong>{{ channel.name }}</strong>
            </div>
            <p class="settings-channel-card__subtitle">
              {{ resolveChannelTypeLabel(channel.type) }} {{ $t('stage5DynamicUi.80') }} {{ channel.code }}
            </p>
          </div>
          <span
            class="settings-channel-card__status"
            :class="channel.enabled ? 'is-active' : 'is-inactive'"
          >
            {{ channel.enabled ? $t('channel.managementData.statusActive') : $t('stage5DynamicUi.28') }}
          </span>
        </div>

        <p class="settings-channel-card__description">
          {{ channel.description || $t('stage5DynamicUi.49') }}
        </p>

        <div class="settings-channel-card__meta-grid">
          <div class="settings-channel-card__meta-item">
            <span>{{ $t('stage5SourceText.166') }}</span>
            <strong>{{ resolveChannelTypeLabel(channel.type) }}</strong>
          </div>
          <div class="settings-channel-card__meta-item">
            <span>{{ $t('stage5SourceText.164') }}</span>
            <strong>{{ channel.code }}</strong>
          </div>
        </div>

        <div class="settings-channel-card__actions">
          <ion-button size="small" fill="solid" @click="handleEditChannel(channel)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
          <ion-button size="small" fill="solid" @click="handleToggleChannel(channel)">
            {{ channel.enabled ? $t('roomStatus.store.roomState.outOfOrder') : $t('settingsStage4.accountList.status.enabled') }}
          </ion-button>
          <ion-button
            class="settings-channel-card__delete-button"
            size="small"
            fill="outline"
            @click="handleDeleteChannel(channel)"
          >
            {{ $t('roomStatus.roomLock.actions.delete') }}
          </ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note">{{ $t('stage5SourceText.84') }}</p>

    <template #modalContent>
      <div class="settings-form-grid settings-channel-editor-form">
        <label class="settings-form-field">
          <span>{{ $t('iosStage5.roomStatus.channelName') }}</span>
          <ion-input v-model="channelForm.name" fill="outline" :placeholder="$t('stage5UiAttributes.88')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.164') }}</span>
          <ion-input v-model="channelForm.code" fill="outline" :placeholder="$t('stage5UiAttributes.87')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.166') }}</span>
          <ion-select v-model="channelForm.type" fill="outline" interface="action-sheet">
            <ion-select-option v-for="option in CHANNEL_TYPE_OPTIONS" :key="option.value" :value="option.value">
              {{ t(option.labelKey) }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field settings-channel-color-field">
          <span>{{ $t('stage5SourceText.231') }}</span>
          <div class="settings-channel-color-control">
            <span
              class="settings-channel-color-preview__swatch"
              :style="{ backgroundColor: channelForm.color || CHANNEL_COLOR_OPTIONS[0].value }"
            />
            <ion-select
              v-model="channelForm.color"
              fill="outline"
              interface="action-sheet"
              :selected-text="resolveChannelColorLabel(channelForm.color)"
            >
              <ion-select-option v-for="color in CHANNEL_COLOR_OPTIONS" :key="color.value" :value="color.value">
                {{ t(color.labelKey) }}
              </ion-select-option>
            </ion-select>
          </div>
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.206') }}</span>
          <ion-textarea v-model="channelForm.description" :rows="1" fill="outline" placeholder="-" />
        </label>

        <div class="settings-toggle-field settings-channel-editor-toggle">
          <div>
            <strong>{{ $t('stage5SourceText.35') }}</strong>
          </div>
          <ion-toggle v-model="channelForm.enabled" />
        </div>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleResetChannelForm">{{ $t('accommodation.common.reset') }}</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveChannel">
        {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('settingsResidual.common.saveSettings') }}
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
  IonToggle,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { createChannel, deleteChannel, getAllChannels, toggleChannelStatus, updateChannel, type ChannelDTO } from '@/api/channel'
import SettingsCrudPage from '@/components/settings/families/SettingsCrudPage.vue'
import { CHANNEL_COLOR_OPTIONS, CHANNEL_TYPE_OPTIONS } from '@/constants/settings'
import { ROUTE_PATHS } from '@/router/guards'
import type { CreateChannelRequest } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const editorOpen = ref(false)
const editingChannelId = ref<number | null>(null)
const channels = ref<ChannelDTO[]>([])
const channelForm = ref<CreateChannelRequest>(createEmptyForm())
const editorInitialForm = ref<CreateChannelRequest>(createEmptyForm())
const { t } = useI18n()

function createEmptyForm(): CreateChannelRequest {
  return {
    name: '',
    code: '',
    type: 'DIRECT',
    color: '#0f766e',
    enabled: true,
    description: '',
  }
}

function cloneChannelForm(form: CreateChannelRequest): CreateChannelRequest {
  return { ...form }
}

function resolveChannelColorLabel(colorValue?: string) {
  const matched = CHANNEL_COLOR_OPTIONS.find((item) => item.value === colorValue)
  if (matched) {
    return t(matched.labelKey)
  }
  return t('settings.constants.channel.defaultColor')
}

function resolveChannelTypeLabel(typeValue: string) {
  const matched = CHANNEL_TYPE_OPTIONS.find((item) => item.value === typeValue)
  if (matched) {
    return t(matched.labelKey)
  }
  return typeValue || t('settings.constants.channel.unsetType')
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function confirmDelete(name: string) {
  const alert = await alertController.create({
    header: t('settingsResidual.common.confirm'),
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
    const response = await getAllChannels()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5.common.messages.loadChannelsFailed'))
    }
    channels.value = response.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5.common.messages.loadChannelsFailed')))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateChannel() {
  editingChannelId.value = null
  const nextForm = createEmptyForm()
  editorInitialForm.value = cloneChannelForm(nextForm)
  channelForm.value = nextForm
  editorOpen.value = true
}

function handleEditChannel(channel: ChannelDTO) {
  editingChannelId.value = channel.id
  const nextForm = {
    name: channel.name,
    code: channel.code,
    type: channel.type,
    color: channel.color,
    enabled: channel.enabled,
    description: channel.description || '',
  }
  editorInitialForm.value = cloneChannelForm(nextForm)
  channelForm.value = nextForm
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingChannelId.value = null
  channelForm.value = createEmptyForm()
  editorInitialForm.value = createEmptyForm()
}

function handleResetChannelForm() {
  channelForm.value = cloneChannelForm(editorInitialForm.value)
}

async function handleSaveChannel() {
  if (!channelForm.value.name.trim()) {
    showWarningToast(t('stage5UiAttributes.88'))
    return
  }
  if (!channelForm.value.code.trim()) {
    showWarningToast(t('stage5UiAttributes.87'))
    return
  }

  submitting.value = true
  try {
    if (editingChannelId.value) {
      const response = await updateChannel(editingChannelId.value, {
        ...channelForm.value,
        name: channelForm.value.name.trim(),
        code: channelForm.value.code.trim(),
        description: channelForm.value.description?.trim(),
      })
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.updateFailed'))
      }
    } else {
      const response = await createChannel({
        ...channelForm.value,
        name: channelForm.value.name.trim(),
        code: channelForm.value.code.trim(),
        description: channelForm.value.description?.trim(),
      })
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.createFailed'))
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

async function handleToggleChannel(channel: ChannelDTO) {
  try {
    const response = await toggleChannelStatus(channel.id, !channel.enabled)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.updateFailed'))
    }
    showSuccessToast(
      response.data.enabled
        ? t('settings.constants.channel.enabled')
        : t('settings.constants.channel.disabled'),
    )
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.updateFailed')))
    }
  }
}

async function handleDeleteChannel(channel: ChannelDTO) {
  const confirmed = await confirmDelete(channel.name)
  if (!confirmed) {
    return
  }
  try {
    const response = await deleteChannel(channel.id)
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

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.settings-channel-settings-page :deep(.settings-crud-page) {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 12px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-channel-settings-page :deep(.app-page-header__title) {
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
}

.settings-channel-settings-page :deep(.app-page-header__text-btn) {
  --color: #333333;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-channel-settings-page :deep(.settings-page-block__hero) {
  display: none;
}

.settings-channel-settings-page :deep(.settings-page-shell__stack) {
  gap: 0;
  margin-top: 0;
  padding-bottom: 6px;
}

.settings-channel-settings-page :deep(.settings-page-shell__stack > .mobile-card) {
  padding: 18px 16px 24px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-channel-settings-page :deep(.settings-page-block__section-header) {
  align-items: center;
  min-height: 32px;
}

.settings-channel-settings-page :deep(.settings-page-block__section-header .mobile-section-title) {
  margin: 0;
  color: #333333;
  font-size: 22px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-channel-settings-page :deep(.settings-page-block__section-header ion-spinner) {
  width: 18px;
  height: 18px;
  color: rgba(var(--ion-color-primary-rgb), 0.78);
}

.settings-channel-list {
  margin-top: 18px;
  gap: 18px;
}

.settings-channel-card {
  position: relative;
  overflow: hidden;
  padding: 16px 14px 24px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-card);
  background: rgba(255, 255, 255, 0.86);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 6px 16px rgba(77, 98, 145, 0.035);
}

.settings-channel-card::before {
  display: none;
}

.settings-channel-card > * {
  position: relative;
  z-index: 1;
}

.settings-channel-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.settings-channel-card__title-group {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.settings-channel-card__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.settings-channel-card__title-row strong,
.settings-channel-card__subtitle,
.settings-channel-card__description,
.settings-channel-card__meta-item span,
.settings-channel-card__meta-item strong {
  margin: 0;
}

.settings-channel-card__title-row strong {
  color: #333333;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
  word-break: break-word;
}

.settings-channel-card__swatch,
.settings-channel-color-preview__swatch {
  flex: none;
  border-radius: 999px;
  border: 1px solid rgba(22, 35, 59, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.58);
}

.settings-channel-card__swatch {
  width: 10px;
  height: 10px;
}

.settings-channel-card__subtitle {
  display: none;
}

.settings-channel-card__status {
  display: inline-flex;
  flex: none;
  align-items: center;
  min-height: 28px;
  padding: 2px 10px;
  border: 0;
  border-radius: var(--ios-pms-radius-pill);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
  white-space: nowrap;
}

.settings-channel-card__status.is-active {
  background: rgba(var(--ion-color-success-rgb), 0.74);
  color: #ffffff;
}

.settings-channel-card__status.is-inactive {
  background: rgba(217, 119, 6, 0.12);
  color: var(--ion-color-warning);
}

.settings-channel-card__description {
  display: none;
}

.settings-channel-card__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 18px;
}

.settings-channel-card__meta-item {
  display: grid;
  gap: 7px;
  min-width: 0;
  padding: 10px 14px 12px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.68);
}

.settings-channel-card__meta-item span {
  color: #999999;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.35;
  letter-spacing: 0;
}

.settings-channel-card__meta-item strong {
  color: #333333;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: 0;
  word-break: break-word;
}

.settings-channel-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  margin-top: 12px;
  padding-top: 0;
  border-top: 0;
}

.settings-channel-card__actions ion-button {
  flex: 0 1 86px;
  min-width: 0;
  width: 86px;
  height: 28px;
  margin: 0;
  min-height: 28px;
  --padding-start: 8px;
  --padding-end: 8px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-color: #d9d9d9;
  --border-style: solid;
  --border-width: 1px;
  --border-radius: 10px;
  --box-shadow: none;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0;
}

.settings-channel-card__actions ion-button::part(native) {
  min-height: 28px;
  padding-top: 2px;
  padding-bottom: 2px;
}

.settings-channel-card__actions ion-button[fill='solid'] {
  --background: var(--ios-pms-primary);
  --background-activated: var(--ion-color-primary-shade);
  --border-color: #d9d9d9;
  --color: #ffffff;
}

.settings-channel-card__delete-button {
  color: #ff0000;
  --background: rgba(255, 255, 255, 0.72);
  --background-activated: rgba(255, 0, 0, 0.05);
  --background-activated-opacity: 1;
  --background-focused: rgba(255, 0, 0, 0.04);
  --background-hover: rgba(255, 0, 0, 0.03);
  --border-color: #d9d9d9;
  --color: #ff0000;
  --color-activated: #ff0000;
  --color-focused: #ff0000;
  --color-hover: #ff0000;
}

.settings-channel-card__delete-button::part(native) {
  border-color: #d9d9d9;
  color: #ff0000;
}

.settings-channel-color-preview {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  font-weight: var(--ios-pms-weight-medium);
}

.settings-channel-color-preview__swatch {
  width: 16px;
  height: 16px;
}

:global(.settings-channel-editor-modal) {
  --width: 100%;
  --height: 100%;
  --border-radius: 0;
  --background: #eef6ff;
}

:global(.settings-channel-editor-modal ion-header) {
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

:global(.settings-channel-editor-modal ion-header::after) {
  display: none;
}

:global(.settings-channel-editor-modal ion-toolbar) {
  --background: rgba(255, 255, 255, 0.94);
  --border-color: transparent;
  --min-height: 64px;
  --padding-start: 16px;
  --padding-end: 16px;
}

:global(.settings-channel-editor-modal ion-title) {
  color: #333333;
  font-size: 23px;
  font-weight: 400;
  letter-spacing: 0;
}

:global(.settings-channel-editor-modal .settings-editor-modal__close-button) {
  max-width: min(42vw, 150px);
  min-height: 36px;
  margin: 0;
  --padding-start: 0;
  --padding-end: 0;
  --background: transparent;
  --background-activated: transparent;
  --box-shadow: none;
  --color: #777777;
  color: #777777;
  font-size: 20px;
  font-weight: 400;
  letter-spacing: 0;
}

:global(.settings-channel-editor-modal .settings-editor-modal__close-button::part(native)) {
  gap: 6px;
  white-space: normal;
}

:global(.settings-channel-editor-modal .settings-editor-modal__back-icon) {
  display: inline-flex;
  align-items: center;
  color: #777777;
  font-size: 40px;
  font-weight: 300;
  line-height: 0.75;
  transform: translateY(-1px);
}

:global(ion-content.settings-channel-editor-page) {
  --background: #eef6ff;
  --padding-top: 34px;
  --padding-bottom: calc(90px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
  background: #eef6ff;
}

:global(.settings-channel-editor-card) {
  width: 100%;
  margin: 0 auto;
  padding: 30px 16px 24px;
  border: 0;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 18px rgba(67, 92, 132, 0.08);
}

:global(.settings-channel-editor-form) {
  gap: 18px;
}

:global(.settings-channel-editor-card .settings-form-field) {
  display: grid;
  gap: 6px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  transform: none;
  transition: none;
}

:global(.settings-channel-editor-card .settings-form-field span),
:global(.settings-channel-editor-card .settings-channel-editor-toggle strong) {
  color: #333333;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
}

:global(.settings-channel-editor-card .settings-form-field span) {
  font-size: 20px;
}

:global(.settings-channel-editor-card .settings-channel-editor-toggle strong) {
  font-size: 18px;
}

:global(.settings-channel-editor-card .settings-form-field ion-input),
:global(.settings-channel-editor-card .settings-form-field ion-select),
:global(.settings-channel-editor-card .settings-form-field ion-textarea) {
  box-sizing: border-box;
  display: block;
  width: 100%;
  min-height: 44px;
  height: 44px;
  margin: 0;
  overflow: visible;
  border: 1px solid #d8d8dc;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.62);
  box-shadow: none;
  color: #333333;
  font-size: 18px;
  font-weight: 400;
  --background: rgba(255, 255, 255, 0.62);
  --border-color: #d8d8dc;
  --border-radius: 11px;
  --color: #333333;
  --highlight-color-focused: #d8d8dc;
  --highlight-color-valid: #d8d8dc;
  --highlight-color-invalid: var(--ion-color-danger);
  --placeholder-color: #666666;
  --placeholder-opacity: 1;
  --padding-start: 18px;
  --padding-end: 18px;
  --padding-top: 0;
  --padding-bottom: 0;
}

:global(.settings-channel-editor-card .settings-form-field ion-input::part(native)),
:global(.settings-channel-editor-card .settings-form-field ion-textarea::part(native)) {
  color: #333333;
  font-size: 18px;
  font-weight: 400;
  line-height: 1.35;
}

:global(.settings-channel-editor-card .settings-form-field ion-textarea::part(native)) {
  min-height: 44px;
}

:global(.settings-channel-color-control) {
  position: relative;
  display: block;
  width: 100%;
}

:global(.settings-channel-color-control ion-select) {
  --padding-start: 18px;
  --padding-end: 18px;
}

:global(.settings-channel-color-control ion-select::part(text)),
:global(.settings-channel-color-control ion-select::part(placeholder)) {
  margin-left: 30px;
}

:global(.settings-channel-color-control .settings-channel-color-preview__swatch) {
  position: absolute;
  top: 50%;
  left: 18px;
  z-index: 2;
  width: 16px;
  height: 16px;
  transform: translateY(-50%);
}

:global(.settings-channel-editor-toggle) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 41px;
  margin-top: 14px;
  padding: 8px 16px;
  border: 0;
  border-radius: 9px;
  background: #dceaff;
  box-shadow: none;
}

:global(.settings-channel-editor-toggle ion-toggle) {
  width: 44px;
  min-width: 44px;
  height: 22px;
  min-height: 22px;
  contain: layout size style;
  --track-background: #d9d9d9;
  --track-background-checked: linear-gradient(90deg, #81bfff 0%, #017cfe 100%);
  --handle-background: #ffffff;
  --handle-background-checked: #ffffff;
  --handle-width: 22px;
  --handle-height: 22px;
  --handle-spacing: 0;
  --handle-box-shadow: none;
}

:global(.settings-channel-editor-toggle ion-toggle::part(track)) {
  width: 44px;
  height: 22px;
  border-radius: var(--ios-pms-radius-pill);
}

:global(.settings-channel-editor-toggle ion-toggle::part(handle)) {
  top: 0;
  width: 22px;
  height: 22px;
  margin: 0;
  border-radius: 50%;
  box-shadow: none;
}

:global(.settings-channel-editor-card .settings-form-actions) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 36px;
  margin-top: 18px;
  padding-top: 0;
  border-top: 0;
}

:global(.settings-channel-editor-card .settings-form-actions ion-button) {
  width: 100%;
  min-height: 38px;
  height: auto;
  margin: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 6px;
  --box-shadow: none;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

:global(.settings-channel-editor-card .settings-form-actions ion-button::part(native)) {
  min-height: 38px;
  padding-top: 6px;
  padding-bottom: 6px;
  white-space: normal;
}

:global(.settings-channel-editor-card .settings-form-actions ion-button[fill='outline']) {
  --background: rgba(255, 255, 255, 0.96);
  --background-activated: #f7f7f7;
  --border-color: #d9d9d9;
  --border-width: 1px;
  --color: #999999;
}

:global(.settings-channel-editor-card .settings-form-actions ion-button:not([fill='outline'])) {
  --background: #1890ff;
  --background-activated: #1683e6;
  --color: #ffffff;
}

.settings-channel-settings-page :deep(.mobile-note:not(.settings-page-block__eyebrow)) {
  margin: 18px 0 0;
  padding: 24px 12px;
  border: 1px dashed rgba(130, 143, 165, 0.24);
  border-radius: var(--ios-pms-radius-input);
  color: var(--ios-pms-text-muted);
  text-align: center;
}

@media (max-width: 374px) {
  .settings-channel-settings-page :deep(.settings-crud-page) {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .settings-channel-settings-page :deep(.settings-page-shell__stack > .mobile-card) {
    padding-right: 14px;
    padding-left: 14px;
  }

  .settings-channel-settings-page :deep(.settings-page-block__section-header .mobile-section-title) {
    font-size: 20px;
  }

  .settings-channel-card {
    padding-right: 12px;
    padding-left: 12px;
  }

  .settings-channel-card__title-row strong {
    font-size: 18px;
  }

  .settings-channel-card__actions {
    gap: 6px;
  }

  .settings-channel-card__actions ion-button {
    flex-basis: 78px;
    width: 78px;
    --padding-start: 6px;
    --padding-end: 6px;
    font-size: 12px;
  }
}
</style>
