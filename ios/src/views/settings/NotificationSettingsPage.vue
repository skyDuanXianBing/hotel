<template>
  <SettingsTogglePage
    class="settings-notification-page"
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.entries.notification.0')"
    :hero-eyebrow="$t('settings.groups.general')"
    :hero-title="$t('settings.entries.notification.0')"
  >
    <SettingsSectionCard :loading="loading" header-class="settings-page-block__section-header">
      <div class="notification-settings-group">
        <h2 class="notification-settings-group__title">
          {{ $t('iosStage5.roomStatus.reservationAlerts') }}
        </h2>

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
              <img
                class="settings-preview-button__icon"
                src="/settings/notification-preview-play.png"
                alt=""
                aria-hidden="true"
              />
              <span>
                {{ previewingSound === 'order' ? $t('stage5DynamicUi.73') : $t('settingsResidual.common.preview') }}
              </span>
            </ion-button>
          </div>
          <ion-toggle v-model="form.orderSound" />
        </div>
      </div>

      <div class="notification-settings-divider" />

      <div class="notification-settings-group">
        <h2 class="notification-settings-group__title">
          {{ $t('stage5UiAttributes.58') }}
        </h2>

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
              <img
                class="settings-preview-button__icon"
                src="/settings/notification-preview-play.png"
                alt=""
                aria-hidden="true"
              />
              <span>
                {{ previewingSound === 'chat' ? $t('stage5DynamicUi.73') : $t('settingsResidual.common.preview') }}
              </span>
            </ion-button>
          </div>
          <ion-toggle v-model="form.chatSound" />
        </div>
      </div>

      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || saving" @click="loadPageData">{{ $t('accommodation.common.reset') }}</ion-button>
        <ion-button :disabled="loading || saving" @click="handleSave">
          {{ saving ? $t('channel.mobile.common.saving') : $t('settingsResidual.common.saveSettings') }}
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
.settings-notification-page :deep(.settings-toggle-page) {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 10px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  --padding-start: 12px;
  --padding-end: 12px;
}

.settings-notification-page :deep(.app-page-header__title) {
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
}

.settings-notification-page :deep(.settings-page-block__hero) {
  display: none;
}

.settings-notification-page :deep(.settings-page-shell__stack) {
  gap: 14px;
  margin-top: 0;
  padding-bottom: 6px;
}

.settings-notification-page :deep(.settings-page-shell__stack > .mobile-card) {
  padding: 18px 14px 20px;
  border: 1px solid rgba(224, 232, 244, 0.86);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 4px 14px rgba(77, 98, 145, 0.04);
}

.settings-notification-page :deep(.settings-section-card__header) {
  justify-content: flex-end;
  min-height: 0;
  margin: 0;
}

.settings-notification-page :deep(.settings-section-card__header .mobile-section-title) {
  display: none;
}

.settings-notification-page :deep(.settings-section-card__header ion-spinner) {
  width: 18px;
  height: 18px;
  color: rgba(var(--ion-color-primary-rgb), 0.78);
}

.notification-settings-group {
  display: grid;
  gap: 16px;
}

.notification-settings-group__title {
  margin: 0;
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  line-height: 1.25;
  letter-spacing: 0;
}

.notification-settings-divider {
  height: 1px;
  margin: 22px 0;
  background: rgba(217, 217, 217, 0.82);
}

.settings-notification-page :deep(.settings-toggle-field) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 44px;
  padding: 8px 16px 8px 14px;
  border: 1px solid rgba(230, 235, 244, 0.92);
  border-radius: var(--ios-pms-radius-pill);
  background: #ffffff;
  box-shadow: none;
}

.settings-notification-page :deep(.settings-toggle-field + .settings-toggle-field) {
  margin-top: 0;
}

.settings-notification-page :deep(.settings-toggle-field strong) {
  display: block;
  color: #333333;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.35;
  letter-spacing: 0;
}

.settings-notification-page :deep(.settings-toggle-field__content) {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.settings-notification-page :deep(.settings-preview-button) {
  flex: 0 1 auto;
  min-width: 0;
  min-height: 24px;
  height: auto;
  margin: 0;
  --padding-start: 6px;
  --padding-end: 8px;
  --padding-top: 0;
  --padding-bottom: 0;
  --background: #e7f4ff;
  --background-activated: #d9edff;
  --border-radius: var(--ios-pms-radius-pill);
  --box-shadow: none;
  --color: #1890ff;
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-notification-page :deep(.settings-preview-button::part(native)) {
  gap: 4px;
  min-height: 24px;
  padding-top: 3px;
  padding-bottom: 3px;
  white-space: normal;
}

.settings-preview-button__icon {
  display: block;
  width: 12px;
  height: 12px;
  object-fit: contain;
}

.settings-notification-page :deep(.settings-preview-button.button-disabled) {
  opacity: 0.55;
}

.settings-notification-page :deep(ion-toggle) {
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

.settings-notification-page :deep(ion-toggle::part(track)) {
  width: 44px;
  height: 22px;
  border-radius: var(--ios-pms-radius-pill);
}

.settings-notification-page :deep(ion-toggle::part(handle)) {
  top: 0;
  width: 22px;
  height: 22px;
  margin: 0;
  border-radius: 50%;
  box-shadow: none;
}

.settings-notification-page :deep(.settings-form-actions--section) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 32px;
  margin-top: 20px;
  padding-top: 0;
  border-top: 0;
}

.settings-notification-page :deep(.settings-form-actions--section ion-button) {
  width: 100%;
  min-height: 38px;
  height: auto;
  margin: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 6px;
  --box-shadow: none;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-notification-page :deep(.settings-form-actions--section ion-button::part(native)) {
  min-height: 38px;
  padding-top: 6px;
  padding-bottom: 6px;
  white-space: normal;
}

.settings-notification-page :deep(.settings-form-actions--section ion-button:first-child) {
  --background: #ffffff;
  --background-activated: #f7f7f7;
  --border-color: #d9d9d9;
  --border-width: 1px;
  --color: #999999;
}

.settings-notification-page :deep(.settings-form-actions--section ion-button:last-child) {
  --background: #1890ff;
  --background-activated: #1683e6;
  --color: #ffffff;
}

.settings-save-feedback {
  margin: 12px 0 0;
  color: var(--ion-color-success-shade);
  font-size: 13px;
}

@media (max-width: 374px) {
  .settings-notification-page :deep(.settings-toggle-page) {
    --padding-start: 10px;
    --padding-end: 10px;
  }

  .settings-notification-page :deep(.settings-page-shell__stack > .mobile-card) {
    padding-right: 12px;
    padding-left: 12px;
  }

  .notification-settings-group__title {
    font-size: 19px;
  }

  .settings-notification-page :deep(.settings-toggle-field) {
    min-height: 42px;
    padding-right: 14px;
    padding-left: 12px;
  }

  .settings-notification-page :deep(.settings-toggle-field__content) {
    gap: 9px;
  }

  .settings-notification-page :deep(.settings-preview-button) {
    --padding-start: 5px;
    --padding-end: 7px;
    font-size: 10px;
  }

  .settings-notification-page :deep(.settings-form-actions--section) {
    gap: 18px;
  }
}
</style>
