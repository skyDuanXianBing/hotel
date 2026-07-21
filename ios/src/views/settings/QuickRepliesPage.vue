<template>
  <SettingsCrudPage
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
    @toolbar-action="handleCreateReply"
    @refresh="handleRefresh"
    @dismiss-editor="handleDismissEditor"
  >
    <div v-if="replies.length > 0" class="mobile-list settings-minimal-list settings-quick-replies-list">
      <article v-for="reply in replies" :key="reply.id" class="settings-minimal-card settings-quick-reply-card">
        <div class="settings-minimal-card__title-group">
          <strong>{{ reply.title }}</strong>
          <p class="settings-minimal-card__summary settings-minimal-card__summary--clamp-two">{{ reply.message }}</p>
        </div>

        <div class="settings-minimal-card__actions">
          <ion-button size="small" fill="outline" @click="handleEditReply(reply)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
          <ion-button size="small" color="danger" fill="clear" @click="handleDeleteReply(reply)">
            {{ $t('roomStatus.roomLock.actions.delete') }}
          </ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note settings-quick-replies-page__empty-state">{{ $t('stage5SourceText.78') }}</p>

    <template #modalContent>
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.148') }}</span>
          <ion-input v-model="replyForm.title" fill="outline" :placeholder="$t('stage5UiAttributes.85')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5.dataCenter.detail.messageContent') }}</span>
          <ion-textarea v-model="replyForm.message" :rows="7" fill="outline" :placeholder="$t('stage5UiAttributes.69')" />
        </label>
      </div>

      <div class="settings-variable-panel">
        <h3>{{ $t('stage5SourceText.122') }}</h3>
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
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveReply">
        {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.9') }}
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
import { ref } from 'vue'
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

const messageVariables = [
  { label: 'Property name', code: '{{property_name}}' },
  { label: "Guest's name", code: '{{guest_name}}' },
  { label: 'Check-in date', code: '{{checkin_date}}' },
  { label: 'Checkout date', code: '{{checkout_date}}' },
  { label: 'Room type name', code: '{{room_type_name}}' },
  { label: 'Rate plan name', code: '{{rate_plan_name}}' },
]

const loading = ref(false)
const submitting = ref(false)
const replies = ref<QuickReplyDTO[]>([])
const editorOpen = ref(false)
const editingReplyId = ref<number | null>(null)
const replyForm = ref<QuickReplyFormState>(createEmptyReplyForm())

function createEmptyReplyForm(): QuickReplyFormState {
  return {
    title: '',
    message: '',
  }
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
  replyForm.value = createEmptyReplyForm()
  editorOpen.value = true
}

function handleEditReply(reply: QuickReplyDTO) {
  editingReplyId.value = reply.id
  replyForm.value = {
    title: reply.title,
    message: reply.message,
  }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingReplyId.value = null
  replyForm.value = createEmptyReplyForm()
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
.settings-quick-replies-page__empty-state {
  padding-top: 16px;
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
</style>
