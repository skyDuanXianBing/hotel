<template>
  <SettingsCrudPage
    class="settings-quick-replies-page"
    :back-href="ROUTE_PATHS.settings"
    :title="$t('stage5.dataCenter.detail.quickReply')"
    :hero-eyebrow="$t('stage5SourceText.159')"
    :hero-title="$t('stage5.dataCenter.detail.quickReply')"
    :chips="[
      { label: `${$t('stage5VisibleText.216')} ${replies.length}` },
    ]"
    :toolbar-action-label="$t('settingsStage4.roomGroup.addGroup')"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.3')"
    :section-title="$t('stage5UiAttributes.51')"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingReplyId ? $t('stage5DynamicUi.62') : $t('stage5DynamicUi.35')"
    modal-class="settings-quick-reply-editor-modal"
    modal-content-class="settings-quick-reply-editor-page"
    modal-card-class="settings-quick-reply-editor-card"
    :modal-close-text="$t('common.back')"
    modal-close-slot="start"
    @toolbar-action="handleCreateReply"
    @refresh="handleRefresh"
    @dismiss-editor="handleDismissEditor"
  >
    <div v-if="replies.length > 0" class="mobile-list settings-quick-replies-list">
      <article v-for="reply in replies" :key="reply.id" class="settings-quick-reply-card">
        <div class="settings-quick-reply-card__title-group">
          <strong>{{ reply.title }}</strong>
          <div class="settings-quick-reply-card__preview">
            <span>{{ reply.message }}</span>
          </div>
        </div>

        <div class="settings-quick-reply-card__actions">
          <ion-button size="small" fill="solid" @click="handleEditReply(reply)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
          <ion-button
            class="settings-quick-reply-card__delete-button"
            size="small"
            fill="outline"
            @click="handleDeleteReply(reply)"
          >
            {{ $t('roomStatus.roomLock.actions.delete') }}
          </ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note settings-quick-replies-page__empty-state">{{ $t('stage5SourceText.78') }}</p>

    <template #modalContent>
      <div class="settings-form-grid settings-quick-reply-editor-form">
        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.148') }}</span>
          <ion-input v-model="replyForm.title" fill="outline" :placeholder="$t('stage5UiAttributes.85')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5.dataCenter.detail.messageContent') }}</span>
          <ion-textarea v-model="replyForm.message" :rows="1" fill="outline" :placeholder="$t('stage5UiAttributes.69')" />
        </label>

        <div class="settings-variable-panel settings-form-field">
          <span>{{ $t('stage5SourceText.122') }}</span>
          <div class="settings-variable-panel__list">
            <button
              v-for="variable in messageVariables"
              :key="variable.code"
              type="button"
              class="settings-variable-chip"
              @click="handleInsertVariable(variable.code)"
            >
              {{ variable.label }}
            </button>
          </div>
        </div>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleResetReplyForm">{{ $t('accommodation.common.reset') }}</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveReply">
        {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('settingsResidual.common.saveSettings') }}
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
  IonTextarea,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import {
  createQuickReply,
  deleteQuickReply,
  getAllQuickReplies,
  updateQuickReply,
  type QuickReplyDTO,
} from '@/api/quickReply'
import SettingsCrudPage from '@/components/settings/families/SettingsCrudPage.vue'
import { ROUTE_PATHS } from '@/router/guards'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const { t } = useI18n()

interface QuickReplyFormState {
  title: string
  message: string
}

const messageVariables = computed(() => [
  { label: t('settingsResidual.messageVariables.propertyName'), code: '{{property_name}}' },
  { label: t('settingsResidual.messageVariables.guestName'), code: '{{guest_name}}' },
  { label: t('settingsResidual.messageVariables.checkInDate'), code: '{{checkin_date}}' },
  { label: t('settingsResidual.messageVariables.checkOutDate'), code: '{{checkout_date}}' },
  { label: t('settingsResidual.messageVariables.roomTypeName'), code: '{{room_type_name}}' },
  { label: t('settingsResidual.messageVariables.ratePlanName'), code: '{{rate_plan_name}}' },
])

const loading = ref(false)
const submitting = ref(false)
const replies = ref<QuickReplyDTO[]>([])
const editorOpen = ref(false)
const editingReplyId = ref<number | null>(null)
const replyForm = ref<QuickReplyFormState>(createEmptyReplyForm())
const editorInitialForm = ref<QuickReplyFormState>(createEmptyReplyForm())

function createEmptyReplyForm(): QuickReplyFormState {
  return {
    title: '',
    message: '',
  }
}

function cloneReplyForm(form: QuickReplyFormState): QuickReplyFormState {
  return { ...form }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function confirmDelete(title: string) {
  const alert = await alertController.create({
    header: t('settingsResidual.common.confirm'),
    message: t('settingsResidual.common.confirmDelete', { name: title }),
    buttons: [
      {
        text: t('accommodation.common.cancel'),
        role: 'cancel',
      },
      {
        text: t('settingsStage4.roomSettings.messages.deleteTitle'),
        role: 'destructive',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadReplies() {
  loading.value = true
  try {
    const response = await getAllQuickReplies()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5Pattern.loadFailed'))
    }
    replies.value = response.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.loadFailed')))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateReply() {
  editingReplyId.value = null
  const nextForm = createEmptyReplyForm()
  editorInitialForm.value = cloneReplyForm(nextForm)
  replyForm.value = nextForm
  editorOpen.value = true
}

function handleEditReply(reply: QuickReplyDTO) {
  editingReplyId.value = reply.id
  const nextForm = {
    title: reply.title,
    message: reply.message,
  }
  editorInitialForm.value = cloneReplyForm(nextForm)
  replyForm.value = nextForm
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingReplyId.value = null
  replyForm.value = createEmptyReplyForm()
  editorInitialForm.value = createEmptyReplyForm()
}

function handleResetReplyForm() {
  replyForm.value = cloneReplyForm(editorInitialForm.value)
}

function handleInsertVariable(code: string) {
  if (!replyForm.value.message.trim()) {
    replyForm.value.message = code
    return
  }

  replyForm.value.message = `${replyForm.value.message} ${code}`
}

async function handleSaveReply() {
  if (!replyForm.value.title.trim()) {
    showWarningToast(t('stage5UiAttributes.85'))
    return
  }
  if (!replyForm.value.message.trim()) {
    showWarningToast(t('stage5Pattern.enter'))
    return
  }

  submitting.value = true
  try {
    const payload = {
      title: replyForm.value.title.trim(),
      message: replyForm.value.message.trim(),
    }

    if (editingReplyId.value) {
      const response = await updateQuickReply(editingReplyId.value, payload)
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.updateFailed'))
      }
      showSuccessToast(t('stage5Pattern.updateCompleted'))
    } else {
      const response = await createQuickReply(payload)
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.createFailed'))
      }
      showSuccessToast(t('stage5Pattern.createCompleted'))
    }

    handleDismissEditor()
    await loadReplies()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    submitting.value = false
  }
}

async function handleDeleteReply(reply: QuickReplyDTO) {
  const confirmed = await confirmDelete(reply.title)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteQuickReply(reply.id)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.deleteFailed'))
    }
    showSuccessToast(t('stage5Pattern.deleteCompleted'))
    await loadReplies()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.deleteFailed')))
    }
  }
}

async function handleRefresh(event: CustomEvent) {
  await loadReplies()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  await loadReplies()
})
</script>

<style scoped>
.settings-quick-replies-page :deep(.settings-crud-page) {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 12px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-quick-replies-page :deep(.app-page-header__title) {
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
}

.settings-quick-replies-page :deep(.app-page-header__text-btn) {
  --color: #333333;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-quick-replies-page :deep(.settings-page-block__hero) {
  margin: 0 0 20px;
  padding: 20px 16px 28px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-quick-replies-page :deep(.settings-page-block__hero::before) {
  display: none;
}

.settings-quick-replies-page :deep(.settings-page-block__eyebrow) {
  display: none;
}

.settings-quick-replies-page :deep(.settings-page-block__hero .mobile-title) {
  color: #333333;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-quick-replies-page :deep(.settings-page-block__hero .mobile-chip-row) {
  margin-top: 12px;
}

.settings-quick-replies-page :deep(.settings-page-block__hero .mobile-chip) {
  min-height: 28px;
  padding: 0 12px;
  border: 0;
  background: rgba(52, 116, 246, 0.1);
  color: var(--ios-pms-primary-strong);
  font-size: 14px;
  font-weight: 400;
}

.settings-quick-replies-page :deep(.settings-page-shell__stack) {
  gap: 0;
  margin-top: 0;
  padding-bottom: 6px;
}

.settings-quick-replies-page :deep(.settings-page-shell__stack > .mobile-card) {
  padding: 24px 16px 26px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-quick-replies-page :deep(.settings-page-block__section-header) {
  align-items: center;
  min-height: 32px;
}

.settings-quick-replies-page :deep(.settings-page-block__section-header .mobile-section-title) {
  margin: 0;
  color: #333333;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-quick-replies-page :deep(.settings-page-block__section-header ion-spinner) {
  width: 18px;
  height: 18px;
  color: rgba(var(--ion-color-primary-rgb), 0.78);
}

.settings-quick-replies-list {
  gap: 18px;
  margin-top: 20px;
}

.settings-quick-reply-card {
  display: grid;
  gap: 0;
  padding: 16px 14px 22px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-card);
  background: rgba(255, 255, 255, 0.86);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 6px 16px rgba(77, 98, 145, 0.035);
}

.settings-quick-reply-card__title-group {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.settings-quick-reply-card__title-group strong {
  margin: 0;
  color: #333333;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
  word-break: break-word;
}

.settings-quick-reply-card__preview {
  display: flex;
  align-items: center;
  min-height: 36px;
  padding: 4px 12px;
  border: 1px solid #d9d9d9;
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.72);
  color: #777777;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.35;
}

.settings-quick-reply-card__preview span {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
  overflow-wrap: anywhere;
}

.settings-quick-reply-card__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: nowrap;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #d9d9d9;
}

.settings-quick-reply-card__actions ion-button {
  flex: 0 1 108px;
  min-width: 0;
  width: 108px;
  height: 30px;
  margin: 0;
  min-height: 30px;
  --padding-start: 8px;
  --padding-end: 8px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-color: #d9d9d9;
  --border-style: solid;
  --border-width: 1px;
  --border-radius: 10px;
  --box-shadow: none;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0;
}

.settings-quick-reply-card__actions ion-button::part(native) {
  min-height: 30px;
  padding-top: 2px;
  padding-bottom: 2px;
}

.settings-quick-reply-card__actions ion-button[fill='solid'] {
  --background: var(--ios-pms-primary);
  --background-activated: var(--ion-color-primary-shade);
  --border-color: var(--ios-pms-primary);
  --color: #ffffff;
}

.settings-quick-reply-card__delete-button {
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

.settings-quick-reply-card__delete-button::part(native) {
  border-color: #d9d9d9;
  color: #ff0000;
}

.settings-quick-replies-page__empty-state {
  padding-top: 16px;
}

.settings-quick-replies-page :deep(.mobile-note:not(.settings-page-block__eyebrow)) {
  margin: 18px 0 0;
  padding: 24px 12px;
  border: 1px dashed rgba(130, 143, 165, 0.24);
  border-radius: var(--ios-pms-radius-input);
  color: var(--ios-pms-text-muted);
  text-align: center;
}

.settings-variable-panel {
  margin-top: 18px;
  padding: 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.settings-variable-panel h3,
.settings-variable-panel p {
  margin: 0;
}

.settings-variable-panel h3 {
  color: var(--app-heading);
  font-size: 15px;
}

.settings-variable-panel p {
  margin-top: 8px;
}

.settings-variable-panel__list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.settings-variable-chip {
  padding: 8px 12px;
  border: 1px solid var(--app-primary-soft-strong);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

:global(.settings-quick-reply-editor-modal) {
  --width: 100%;
  --height: 100%;
  --border-radius: 0;
  --background: #eef6ff;
}

:global(.settings-quick-reply-editor-modal ion-header) {
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

:global(.settings-quick-reply-editor-modal ion-header::after) {
  display: none;
}

:global(.settings-quick-reply-editor-modal ion-toolbar) {
  --background: rgba(255, 255, 255, 0.94);
  --border-color: transparent;
  --min-height: 64px;
  --padding-start: 16px;
  --padding-end: 16px;
}

:global(.settings-quick-reply-editor-modal ion-title) {
  color: #333333;
  font-size: 23px;
  font-weight: 400;
  letter-spacing: 0;
}

:global(.settings-quick-reply-editor-modal .settings-editor-modal__close-button) {
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

:global(.settings-quick-reply-editor-modal .settings-editor-modal__close-button::part(native)) {
  gap: 6px;
  white-space: normal;
}

:global(.settings-quick-reply-editor-modal .settings-editor-modal__back-icon) {
  display: inline-flex;
  align-items: center;
  color: #777777;
  font-size: 40px;
  font-weight: 300;
  line-height: 0.75;
  transform: translateY(-1px);
}

:global(ion-content.settings-quick-reply-editor-page) {
  --background: #eef6ff;
  --padding-top: 34px;
  --padding-bottom: calc(90px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
  background: #eef6ff;
}

:global(.settings-quick-reply-editor-card) {
  width: 100%;
  margin: 0 auto;
  padding: 30px 16px 24px;
  border: 0;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 18px rgba(67, 92, 132, 0.08);
}

:global(.settings-quick-reply-editor-form) {
  gap: 18px;
}

:global(.settings-quick-reply-editor-card .settings-form-field) {
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

:global(.settings-quick-reply-editor-card .settings-form-field span) {
  color: #333333;
  font-size: 20px;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
}

:global(.settings-quick-reply-editor-card .settings-form-field ion-input),
:global(.settings-quick-reply-editor-card .settings-form-field ion-textarea) {
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

:global(.settings-quick-reply-editor-card .settings-form-field ion-input::part(native)),
:global(.settings-quick-reply-editor-card .settings-form-field ion-textarea::part(native)) {
  color: #333333;
  font-size: 18px;
  font-weight: 400;
  line-height: 1.35;
}

:global(.settings-quick-reply-editor-card .settings-form-field ion-textarea::part(native)) {
  min-height: 24px;
  line-height: 24px;
}

:global(.settings-quick-reply-editor-card .settings-form-field ion-textarea) {
  --padding-top: 10px;
  --padding-bottom: 10px;
}

:global(.settings-quick-reply-editor-card .settings-variable-panel) {
  gap: 12px;
  margin-top: 0;
  padding: 0;
  border-radius: 0;
  background: transparent;
}

:global(.settings-quick-reply-editor-card .settings-variable-panel__list) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
  min-height: 56px;
  margin-top: 0;
  padding: 10px 18px;
  border: 1px solid #d8d8dc;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.62);
}

:global(.settings-quick-reply-editor-card .settings-variable-chip) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  max-width: 100%;
  min-height: 28px;
  height: auto;
  margin: 0;
  padding: 5px 10px;
  border: 0;
  border-radius: var(--ios-pms-radius-pill);
  background: #e1f0ff;
  color: #017cfe;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
  text-align: center;
  white-space: normal;
  overflow-wrap: anywhere;
}

:global(.settings-quick-reply-editor-card .settings-form-actions) {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 12px;
  margin-top: 24px;
  padding-top: 0;
  border-top: 0;
}

:global(.settings-quick-reply-editor-card .settings-form-actions ion-button) {
  width: 100%;
  min-height: 30px;
  height: 30px;
  margin: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --padding-start: 10px;
  --padding-end: 10px;
  --border-radius: 6px;
  --box-shadow: none;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: 0;
}

:global(.settings-quick-reply-editor-card .settings-form-actions ion-button::part(native)) {
  min-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
  white-space: normal;
}

:global(.settings-quick-reply-editor-card .settings-form-actions ion-button[fill='outline']) {
  --background: rgba(255, 255, 255, 0.96);
  --background-activated: #f7f7f7;
  --border-color: rgba(193, 204, 220, 0.95);
  --border-width: 1px;
  --color: #8a96a8;
}

:global(.settings-quick-reply-editor-card .settings-form-actions ion-button:not([fill='outline'])) {
  --background: #2687f7;
  --background-activated: #1f78df;
  --color: #ffffff;
}

@media (max-width: 374px) {
  .settings-quick-replies-page :deep(.settings-crud-page) {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .settings-quick-replies-page :deep(.settings-page-block__hero),
  .settings-quick-replies-page :deep(.settings-page-shell__stack > .mobile-card) {
    padding-right: 14px;
    padding-left: 14px;
  }

  .settings-quick-replies-page :deep(.settings-page-block__section-header .mobile-section-title) {
    font-size: 20px;
  }

  .settings-quick-reply-card {
    padding-right: 12px;
    padding-left: 12px;
  }

  .settings-quick-reply-card__title-group strong {
    font-size: 18px;
  }

  .settings-quick-reply-card__actions {
    gap: 8px;
  }

  .settings-quick-reply-card__actions ion-button {
    flex-basis: 96px;
    width: 96px;
    --padding-start: 6px;
    --padding-end: 6px;
    font-size: 13px;
  }
}
</style>
