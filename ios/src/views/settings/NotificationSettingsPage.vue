<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>通知设置</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page-block">
      <section class="mobile-hero settings-page-block__hero">
        <p class="mobile-note settings-page-block__eyebrow">通用设置</p>
        <h1 class="mobile-title">通知设置</h1>
        <p class="mobile-subtitle">覆盖订单 / 聊天的弹框与声音提醒。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-page-block__section-header">
            <div>
              <h2 class="mobile-section-title">订单提醒</h2>
              <p class="mobile-note">用于订单创建、变化时的移动端提醒。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div class="settings-toggle-field">
            <div>
              <strong>订单弹框</strong>
              <p>控制订单类系统提醒弹层。</p>
            </div>
            <ion-toggle v-model="form.orderPopup" />
          </div>

          <div class="settings-toggle-field">
            <div class="settings-toggle-field__content">
              <strong>订单声音</strong>
              <p>控制订单类系统提醒声音。</p>
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
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">聊天提醒</h2>
          <div class="settings-toggle-field">
            <div>
              <strong>聊天弹框</strong>
              <p>控制聊天消息即时提示。</p>
            </div>
            <ion-toggle v-model="form.chatPopup" />
          </div>

          <div class="settings-toggle-field">
            <div class="settings-toggle-field__content">
              <strong>聊天声音</strong>
              <p>控制聊天消息声音提醒。</p>
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
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonSpinner,
  IonTitle,
  IonToggle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref, watch } from 'vue'
import { getNotificationSettings, updateNotificationSettings } from '@/api/notification'
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

.settings-toggle-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.settings-toggle-field__content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.settings-toggle-field + .settings-toggle-field {
  margin-top: 12px;
}

.settings-toggle-field strong,
.settings-toggle-field p {
  margin: 0;
}

.settings-toggle-field p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-preview-button {
  margin-top: 6px;
  --padding-start: 0;
  --padding-end: 0;
  font-size: 13px;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.settings-form-actions--section {
  margin-top: 18px;
}

.settings-save-feedback {
  margin: 12px 0 0;
  color: var(--ion-color-success-shade);
  font-size: 13px;
}
</style>
