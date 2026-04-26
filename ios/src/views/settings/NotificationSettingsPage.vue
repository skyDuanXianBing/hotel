<template>
  <SettingsTogglePage
    :back-href="ROUTE_PATHS.settings"
    title="通知设置"
    hero-eyebrow="通用设置"
    hero-title="通知设置"
  >
    <SettingsSectionCard
      title="订单提醒"
      :loading="loading"
      header-class="settings-page-block__section-header"
    >
      <div class="settings-toggle-field">
        <div>
          <strong>订单弹框</strong>
        </div>
        <ion-toggle v-model="form.orderPopup" />
      </div>

      <div class="settings-toggle-field">
        <div class="settings-toggle-field__content">
          <strong>订单声音</strong>
          <ion-button
            fill="clear"
            size="small"
            class="settings-preview-button"
            :disabled="loading || saving || previewingSound === 'order'"
            @click="handlePreviewSound('order')"
          >
            {{ previewingSound === 'order' ? '试听中...' : '试听提示音' }}
          </ion-button>
        </div>
        <ion-toggle v-model="form.orderSound" />
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard title="聊天提醒">
      <div class="settings-toggle-field">
        <div>
          <strong>聊天弹框</strong>
        </div>
        <ion-toggle v-model="form.chatPopup" />
      </div>

      <div class="settings-toggle-field">
        <div class="settings-toggle-field__content">
          <strong>聊天声音</strong>
          <ion-button
            fill="clear"
            size="small"
            class="settings-preview-button"
            :disabled="loading || saving || previewingSound === 'chat'"
            @click="handlePreviewSound('chat')"
          >
            {{ previewingSound === 'chat' ? '试听中...' : '试听提示音' }}
          </ion-button>
        </div>
        <ion-toggle v-model="form.chatSound" />
      </div>

      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || saving" @click="loadPageData">重置</ion-button>
        <ion-button :disabled="loading || saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存通知设置' }}
        </ion-button>
      </div>

      <p v-if="saveSuccessMessage" class="settings-save-feedback">
        {{ saveSuccessMessage }}
      </p>
    </SettingsSectionCard>
  </SettingsTogglePage>
</template>

<script setup lang="ts">
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

const userStore = useUserStore()
const notificationCenterStore = useNotificationCenterStore()
const PREVIEW_FAILURE_MESSAGE_MAP: Record<NotificationPreviewSoundType, string> = {
  order: '订单提示音试听失败',
  chat: '聊天提示音试听失败',
}

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
    showWarningToast(resolveWarningMessage(error, PREVIEW_FAILURE_MESSAGE_MAP[soundType]))
  } finally {
    previewingSound.value = null
  }
}

async function loadPageData() {
  const userId = userStore.currentUser?.id
  if (!userId) {
    showWarningToast('请先恢复当前用户信息')
    return
  }

  loading.value = true
  saveSuccessMessage.value = ''
  try {
    const response = await getNotificationSettings(userId)
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载通知设置失败')
    }
    form.value = {
      orderPopup: response.data.orderPopup,
      orderSound: response.data.orderSound,
      chatPopup: response.data.chatPopup,
      chatSound: response.data.chatSound,
    }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载通知设置失败'))
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const userId = userStore.currentUser?.id
  if (!userId) {
    showWarningToast('请先恢复当前用户信息')
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
      throw new Error(response.message || '保存通知设置失败')
    }
    notificationCenterStore.applySettingsSnapshot(snapshot)
    saveSuccessMessage.value = '设置已保存'
    showSuccessToast('设置已保存')
  } catch (error) {
    saveSuccessMessage.value = ''
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存通知设置失败'))
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
