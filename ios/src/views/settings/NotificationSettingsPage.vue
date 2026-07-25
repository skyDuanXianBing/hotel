<template>
  <SettingsTogglePage
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.entries.notification.0')"
    :hero-eyebrow="$t('settings.groups.general')"
    :hero-title="$t('settings.entries.notification.0')"
  >
    <SettingsSectionCard
      :title="$t('iosStage5.roomStatus.reservationAlerts')"
      :loading="loading"
      header-class="settings-page-block__section-header"
    >
      <div class="settings-toggle-field">
        <div>
          <strong>{{ $t('stage5SourceText.200') }}</strong>
        </div>
        <ion-toggle v-model="form.orderPopup" />
      </div>

      <div class="settings-toggle-field">
        <div class="settings-toggle-field__content">
          <strong>{{ $t('stage5SourceText.199') }}</strong>
          <ion-button
            fill="clear"
            size="small"
            class="settings-preview-button"
            :disabled="loading || saving || previewingSound === 'order'"
            @click="handlePreviewSound('order')"
          >
            {{ previewingSound === 'order' ? $t('stage5DynamicUi.73') : $t('stage5DynamicUi.74') }}
          </ion-button>
        </div>
        <ion-toggle v-model="form.orderSound" />
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard :title="$t('stage5UiAttributes.58')">
      <div class="settings-toggle-field">
        <div>
          <strong>{{ $t('stage5SourceText.183') }}</strong>
        </div>
        <ion-toggle v-model="form.chatPopup" />
      </div>

      <div class="settings-toggle-field">
        <div class="settings-toggle-field__content">
          <strong>{{ $t('stage5SourceText.182') }}</strong>
          <ion-button
            fill="clear"
            size="small"
            class="settings-preview-button"
            :disabled="loading || saving || previewingSound === 'chat'"
            @click="handlePreviewSound('chat')"
          >
            {{ previewingSound === 'chat' ? $t('stage5DynamicUi.73') : $t('stage5DynamicUi.74') }}
          </ion-button>
        </div>
        <ion-toggle v-model="form.chatSound" />
      </div>

      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || saving" @click="loadPageData">{{ $t('accommodation.common.reset') }}</ion-button>
        <ion-button :disabled="loading || saving" @click="handleSave">
          {{ saving ? $t('channel.mobile.common.saving') : $t('stage5DynamicUi.21') }}
        </ion-button>
      </div>

      <p v-if="saveSuccessMessage" class="settings-save-feedback">
        {{ saveSuccessMessage }}
      </p>
    </SettingsSectionCard>
  </SettingsTogglePage>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { IonButton, IonToggle, onIonViewWillEnter } from '@ionic/vue'
import { ref, watch } from 'vue'
import { getNotificationSettings, updateNotificationSettings } from '@/api/notification'
import SettingsSectionCard from '@/components/settings/base/SettingsSectionCard.vue'
import SettingsTogglePage from '@/components/settings/families/SettingsTogglePage.vue'
import { ROUTE_PATHS } from '@/router/guards'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { useUserStore } from '@/stores/user'
import type { NotificationSettingRequest } from '@/types/settings'
import {
  playNotificationPreviewSound,
  type NotificationPreviewSoundType,
} from '@/utils/notificationSound'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const { t } = useI18n()

const userStore = useUserStore()
const notificationCenterStore = useNotificationCenterStore()
const loading = ref(false)
const saving = ref(false)
const previewingSound = ref<NotificationPreviewSoundType | null>(null)
const saveSuccessMessage = ref('')
const form = ref<NotificationSettingRequest>({
  orderPopup: true,
  orderSound: true,
  chatPopup: true,
  chatSound: true,
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function handlePreviewSound(soundType: NotificationPreviewSoundType) {
  if (previewingSound.value) {
    return
  }

  previewingSound.value = soundType

  try {
    await playNotificationPreviewSound(soundType)
  } catch (error) {
    showWarningToast(resolveWarningMessage(error, t('stage5Pattern.operationFailed')))
  } finally {
    previewingSound.value = null
  }
}

async function loadPageData() {
  const userId = userStore.currentUser?.id
  if (!userId) {
    showWarningToast(t('stage5Pattern.setup'))
    return
  }

  loading.value = true
  saveSuccessMessage.value = ''
  try {
    const response = await getNotificationSettings(userId)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5Pattern.loadFailed'))
    }
    form.value = {
      orderPopup: response.data.orderPopup,
      orderSound: response.data.orderSound,
      chatPopup: response.data.chatPopup,
      chatSound: response.data.chatSound,
    }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.loadFailed')))
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const userId = userStore.currentUser?.id
  if (!userId) {
    showWarningToast(t('stage5Pattern.setup'))
    return
  }

  saving.value = true
  saveSuccessMessage.value = ''
  try {
    const snapshot: NotificationSettingRequest = {
      orderPopup: form.value.orderPopup,
      orderSound: form.value.orderSound,
      chatPopup: form.value.chatPopup,
      chatSound: form.value.chatSound,
    }
    const response = await updateNotificationSettings(userId, snapshot)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.saveFailed'))
    }
    notificationCenterStore.applySettingsSnapshot(snapshot)
    saveSuccessMessage.value = t('stage5Pattern.saveCompleted')
    showSuccessToast(t('stage5Pattern.saveCompleted'))
  } catch (error) {
    saveSuccessMessage.value = ''
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    saving.value = false
  }
}

onIonViewWillEnter(async () => {
  await loadPageData()
})

watch(
  form,
  () => {
    saveSuccessMessage.value = ''
  },
  {
    deep: true,
  },
)
</script>

<style scoped>
.settings-save-feedback {
  margin: 12px 0 0;
  color: var(--ion-color-success-shade);
  font-size: 13px;
}
</style>
