<template>
  <SettingsPageShell
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.groups.cleaning')"
    :hero-eyebrow="$t('settings.groups.cleaning')"
    :hero-title="$t('stage5UiAttributes.28')"
    :chips="[
      { label: `${$t('accommodation.common.cleaner')} ${cleaners.length}` },
      { label: configForm.enabled ? $t('stage5DynamicUi.77') : $t('stage5DynamicUi.76') },
    ]"
    class="settings-cleaning-page"
    content-class="settings-page-block settings-cleaning-page__content"
    hero-class="settings-page-block__hero settings-cleaning-page__hero"
    eyebrow-class="settings-page-block__eyebrow"
    stack-class="settings-cleaning-page__stack"
  >
    <SettingsSectionCard
      :title="$t('stage5UiAttributes.111')"
      :loading="loading"
      card-class="settings-cleaning-config-card"
      header-class="settings-page-block__section-header"
    >
      <div class="settings-toggle-field settings-cleaning-toggle settings-cleaning-toggle--primary">
        <div>
          <strong>{{ $t('stage5SourceText.33') }}</strong>
        </div>
        <ion-toggle v-model="configForm.enabled" />
      </div>

      <div class="settings-form-grid settings-form-grid--top settings-cleaning-time-grid">
        <label class="settings-form-field settings-cleaning-form-field">
          <span>{{ $t('stage5SourceText.7') }}</span>
          <ion-input v-model="configForm.stayStartTime" fill="outline" placeholder="10:00" />
        </label>
        <label class="settings-form-field settings-cleaning-form-field">
          <span>{{ $t('stage5SourceText.8') }}</span>
          <ion-input v-model="configForm.stayEndTime" fill="outline" placeholder="15:00" />
        </label>
        <label class="settings-form-field settings-cleaning-form-field">
          <span>{{ $t('stage5SourceText.218') }}</span>
          <ion-input v-model="configForm.checkoutStartTime" fill="outline" placeholder="11:00" />
        </label>
        <label class="settings-form-field settings-cleaning-form-field">
          <span>{{ $t('stage5SourceText.219') }}</span>
          <ion-input v-model="configForm.checkoutEndTime" fill="outline" placeholder="17:00" />
        </label>
      </div>

      <div class="settings-cleaning-generation">
        <div class="settings-toggle-field settings-cleaning-toggle">
          <div>
            <strong>{{ $t('stage5SourceText.185') }}</strong>
          </div>
          <ion-toggle v-model="configForm.autoStayTask" />
        </div>

        <div class="settings-toggle-field settings-cleaning-toggle">
          <div>
            <strong>{{ $t('stage5SourceText.186') }}</strong>
          </div>
          <ion-toggle v-model="configForm.autoCheckoutTask" />
        </div>
      </div>

      <div class="settings-form-actions settings-form-actions--section settings-cleaning-config-actions">
        <ion-button fill="outline" :disabled="loading || savingConfig" @click="loadPageData">{{ $t('accommodation.common.reset') }}</ion-button>
        <ion-button :disabled="loading || savingConfig" @click="handleSaveConfig">
          {{ savingConfig ? $t('channel.mobile.common.saving') : $t('stage5DynamicUi.5') }}
        </ion-button>
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard
      :title="$t('accommodation.common.cleaner')"
      card-class="settings-cleaning-cleaners-card"
      header-class="settings-page-block__section-header"
    >
      <template #headerActions>
        <div class="settings-cleaning-section-actions">
          <ion-button size="small" fill="outline" @click="handleOpenSupplies">{{ $t('settingsStage4.cleaningSettings.tabs.supplies') }}</ion-button>
          <ion-button size="small" @click="handleCreateCleaner">{{ $t('stage5SourceText.25') }}</ion-button>
        </div>
      </template>

      <div v-if="cleaners.length > 0" class="mobile-list settings-card-list settings-cleaning-list">
        <article v-for="cleaner in cleaners" :key="cleaner.id" class="settings-card-item settings-cleaning-card-item">
          <div>
            <strong>{{ cleaner.name }}</strong>
            <p>{{ cleaner.email }}</p>
          </div>
          <div class="settings-card-item__actions">
            <ion-button size="small" fill="outline" @click="handleEditCleaner(cleaner)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
            <ion-button size="small" color="danger" fill="clear" @click="handleDeleteCleaner(cleaner)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note">{{ $t('stage5SourceText.75') }}</p>
    </SettingsSectionCard>

    <SettingsEditorModal
      :is-open="editorOpen"
      :title="editingCleanerId ? $t('stage5DynamicUi.61') : $t('stage5SourceText.25')"
      modal-class="settings-cleaner-editor-modal"
      content-class="settings-cleaner-editor-page"
      card-class="settings-cleaner-editor-card"
      @close="handleDismissEditor"
      @didDismiss="handleDismissEditor"
    >
      <div class="settings-form-grid settings-cleaner-editor-form">
        <label class="settings-form-field">
          <span>{{ $t('roomStatus.booking.guestName') }}</span>
          <ion-input v-model="cleanerForm.name" fill="outline" :placeholder="$t('stage5UiAttributes.61')" />
        </label>
        <label class="settings-form-field">
          <span>{{ $t('auth.field.email') }}</span>
          <ion-input v-model="cleanerForm.email" fill="outline" :placeholder="$t('settingsStage4.accountList.placeholders.email')" />
        </label>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleDismissEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
        <ion-button :disabled="submittingCleaner" @click="handleSaveCleaner">
          {{ cleanerSubmitText }}
        </ion-button>
      </template>
    </SettingsEditorModal>
  </SettingsPageShell>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { alertController, IonButton, IonInput, IonToggle, onIonViewWillEnter } from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  deleteCleaner,
  getCleaners,
  getOrCreateCleaningConfig,
  sendCleanerInvitation,
  updateCleaner,
  updateCleaningConfig,
} from '@/api/cleaning'
import SettingsEditorModal from '@/components/settings/base/SettingsEditorModal.vue'
import SettingsPageShell from '@/components/settings/base/SettingsPageShell.vue'
import SettingsSectionCard from '@/components/settings/base/SettingsSectionCard.vue'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { CleanerDTO, CleaningConfigRequest, CleanerRequest } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const { t } = useI18n()

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const router = useRouter()
const storeStore = useStoreStore()
const userStore = useUserStore()

const loading = ref(false)
const savingConfig = ref(false)
const submittingCleaner = ref(false)
const cleaningConfigId = ref<number | null>(null)
const cleaners = ref<CleanerDTO[]>([])
const editorOpen = ref(false)
const editingCleanerId = ref<number | null>(null)
const cleanerForm = ref<{ name: string; email: string }>({ name: '', email: '' })
const cleanerSubmitText = computed(() => {
  if (submittingCleaner.value) {
    return t('settingsResidual.common.submitting')
  }
  if (editingCleanerId.value) {
    return t('settingsResidual.common.saveCleaner')
  }
  return t('settingsResidual.common.sendInvite')
})
const configForm = ref<CleaningConfigRequest>({
  enabled: true,
  stayStartTime: '10:00',
  stayEndTime: '15:00',
  checkoutStartTime: '11:00',
  checkoutEndTime: '17:00',
  autoStayTask: false,
  autoCheckoutTask: true,
})

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
  const userId = userStore.currentUser?.id
  const storeId = storeStore.currentStore?.id
  if (!userId || !storeId) {
    showWarningToast(t('stage5Pattern.setup'))
    return
  }

  loading.value = true
  try {
    const [configResponse, cleanerResponse] = await Promise.all([getOrCreateCleaningConfig(userId, storeId), getCleaners()])
    if (!configResponse.success || !configResponse.data) {
      throw new Error(configResponse.message || t('settingsStage4.cleaningSettings.messages.loadConfigFailed'))
    }
    if (!cleanerResponse.success || !cleanerResponse.data) {
      throw new Error(cleanerResponse.message || t('iosStage5.cleaning.cleanersLoadFailed'))
    }

    cleaningConfigId.value = configResponse.data.id
    configForm.value = {
      enabled: configResponse.data.enabled,
      stayStartTime: configResponse.data.stayStartTime,
      stayEndTime: configResponse.data.stayEndTime,
      checkoutStartTime: configResponse.data.checkoutStartTime,
      checkoutEndTime: configResponse.data.checkoutEndTime,
      autoStayTask: configResponse.data.autoStayTask,
      autoCheckoutTask: configResponse.data.autoCheckoutTask,
    }
    cleaners.value = cleanerResponse.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.loadFailed')))
    }
  } finally {
    loading.value = false
  }
}

async function handleSaveConfig() {
  if (!cleaningConfigId.value) {
    showWarningToast(t('stage5Pattern.unavailable'))
    return
  }

  savingConfig.value = true
  try {
    const response = await updateCleaningConfig(cleaningConfigId.value, configForm.value)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5Pattern.saveFailed'))
    }
    showSuccessToast(t('stage5Pattern.saveCompleted'))
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    savingConfig.value = false
  }
}

function handleCreateCleaner() {
  editingCleanerId.value = null
  cleanerForm.value = { name: '', email: '' }
  editorOpen.value = true
}

function handleEditCleaner(cleaner: CleanerDTO) {
  editingCleanerId.value = cleaner.id
  cleanerForm.value = { name: cleaner.name, email: cleaner.email }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingCleanerId.value = null
  cleanerForm.value = { name: '', email: '' }
}

async function handleSaveCleaner() {
  const userId = userStore.currentUser?.id
  const storeId = storeStore.currentStore?.id
  if (!userId || !storeId) {
    showWarningToast(t('stage5Pattern.setup'))
    return
  }
  if (!cleanerForm.value.name.trim()) {
    showWarningToast(t('stage5UiAttributes.61'))
    return
  }
  if (!cleanerForm.value.email.trim()) {
    showWarningToast(t('stage5Pattern.enter'))
    return
  }
  if (!EMAIL_PATTERN.test(cleanerForm.value.email.trim())) {
    showWarningToast(t('settingsStage4.cleaningSettings.messages.emailInvalid'))
    return
  }

  submittingCleaner.value = true
  try {
    const name = cleanerForm.value.name.trim()
    const email = cleanerForm.value.email.trim()

    if (editingCleanerId.value) {
      const payload: CleanerRequest = {
        userId,
        storeId,
        name,
        email,
      }
      const response = await updateCleaner(editingCleanerId.value, payload)
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.updateFailed'))
      }
      showSuccessToast(t('stage5Pattern.saveCompleted'))
    } else {
      const response = await sendCleanerInvitation({ name, email })
      if (!response.success) {
        throw new Error(response.message || t('settingsStage4.cleaningSettings.messages.invitationFailed'))
      }
      showSuccessToast(t('stage5Pattern.operationCompleted'))
    }

    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      const fallbackMessage = editingCleanerId.value
        ? t('stage5Pattern.saveFailed')
        : t('stage5Pattern.submitFailed')
      showWarningToast(resolveWarningMessage(error, fallbackMessage))
    }
  } finally {
    submittingCleaner.value = false
  }
}

async function handleDeleteCleaner(cleaner: CleanerDTO) {
  const confirmed = await confirmDelete(cleaner.name)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteCleaner(cleaner.id)
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

async function handleOpenSupplies() {
  await router.push(ROUTE_PATHS.settingsCleaningSupplies)
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.settings-cleaning-page :deep(.settings-cleaning-page__content) {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 12px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-cleaning-page :deep(.app-page-header__title) {
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
}

.settings-cleaning-page :deep(.settings-cleaning-page__hero) {
  margin: 0 0 20px;
  padding: 20px 16px 28px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-cleaning-page :deep(.settings-cleaning-page__hero::before) {
  display: none;
}

.settings-cleaning-page :deep(.settings-page-block__eyebrow) {
  display: none;
}

.settings-cleaning-page :deep(.settings-cleaning-page__hero .mobile-title) {
  color: #333333;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-cleaning-page :deep(.settings-cleaning-page__hero .mobile-chip-row) {
  gap: 8px;
  margin-top: 12px;
}

.settings-cleaning-page :deep(.settings-cleaning-page__hero .mobile-chip) {
  min-height: 28px;
  padding: 0 12px;
  border: 0;
  background: rgba(52, 116, 246, 0.1);
  color: var(--ios-pms-primary-strong);
  font-size: 14px;
  font-weight: 400;
}

.settings-cleaning-page :deep(.settings-cleaning-page__stack) {
  gap: 18px;
  margin-top: 0;
  padding-bottom: 6px;
}

.settings-cleaning-page :deep(.settings-cleaning-config-card),
.settings-cleaning-page :deep(.settings-cleaning-cleaners-card) {
  padding: 24px 16px 26px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-cleaning-page :deep(.settings-page-block__section-header) {
  align-items: center;
  min-height: 32px;
}

.settings-cleaning-page :deep(.settings-page-block__section-header .mobile-section-title) {
  margin: 0;
  color: #333333;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-cleaning-page :deep(.settings-page-block__section-header ion-spinner) {
  width: 18px;
  height: 18px;
  color: rgba(var(--ion-color-primary-rgb), 0.78);
}

.settings-cleaning-toggle {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 44px;
  padding: 9px 16px;
  border: 0;
  border-radius: 11px;
  background: rgba(220, 234, 255, 0.92);
  box-shadow: none;
}

.settings-cleaning-toggle--primary {
  margin-top: 22px;
}

.settings-cleaning-toggle strong {
  color: #333333;
  font-size: 17px;
  font-weight: 400;
  line-height: 1.3;
  letter-spacing: 0;
}

.settings-cleaning-toggle ion-toggle {
  width: 46px;
  min-width: 46px;
  height: 24px;
  min-height: 24px;
  contain: layout size style;
  --track-background: #d9d9d9;
  --track-background-checked: linear-gradient(90deg, #81bfff 0%, #017cfe 100%);
  --handle-background: #ffffff;
  --handle-background-checked: #ffffff;
  --handle-width: 24px;
  --handle-height: 24px;
  --handle-spacing: 0;
  --handle-box-shadow: none;
}

.settings-cleaning-toggle ion-toggle::part(track) {
  width: 46px;
  height: 24px;
  border-radius: var(--ios-pms-radius-pill);
}

.settings-cleaning-toggle ion-toggle::part(handle) {
  top: 0;
  width: 24px;
  height: 24px;
  margin: 0;
  border-radius: 50%;
  box-shadow: none;
}

.settings-cleaning-time-grid {
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
  margin-top: 18px;
}

.settings-cleaning-form-field {
  display: grid;
  gap: 7px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  transform: none;
  transition: none;
}

.settings-cleaning-form-field:focus-within {
  border-color: transparent;
  box-shadow: none;
  transform: none;
}

.settings-cleaning-form-field span {
  color: #333333;
  font-size: 18px;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-cleaning-form-field ion-input {
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

.settings-cleaning-form-field ion-input::part(native) {
  color: #333333;
  font-size: 18px;
  font-weight: 400;
  line-height: 1.35;
}

.settings-cleaning-generation {
  display: grid;
  gap: 10px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(130, 143, 165, 0.16);
}

.settings-cleaning-config-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 36px;
  margin-top: 22px;
  padding-top: 0;
  border-top: 0;
}

.settings-cleaning-config-actions ion-button {
  width: 100%;
  min-height: 30px;
  height: 30px;
  margin: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 7px;
  --box-shadow: none;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-cleaning-config-actions ion-button[fill='outline'] {
  --background: rgba(255, 255, 255, 0.96);
  --background-activated: #f7f7f7;
  --border-color: #d9d9d9;
  --border-width: 1px;
  --color: #999999;
}

.settings-cleaning-config-actions ion-button:not([fill='outline']) {
  --background: #1890ff;
  --background-activated: #1683e6;
  --color: #ffffff;
}

.settings-cleaning-section-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.settings-cleaning-section-actions ion-button {
  min-height: 30px;
  height: 30px;
  margin: 0;
  --padding-start: 10px;
  --padding-end: 10px;
  --border-radius: 9px;
  --box-shadow: none;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0;
}

.settings-cleaning-section-actions ion-button[fill='outline'] {
  --background: rgba(255, 255, 255, 0.78);
  --border-color: #d9d9d9;
  --color: #666666;
}

.settings-cleaning-list {
  gap: 14px;
  margin-top: 20px;
}

.settings-cleaning-card-item {
  display: grid;
  gap: 14px;
  padding: 16px 14px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.82);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 6px 16px rgba(77, 98, 145, 0.035);
}

.settings-cleaning-card-item strong {
  color: #333333;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-cleaning-card-item p {
  margin-top: 7px;
  color: #777777;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.35;
}

.settings-cleaning-card-item .settings-card-item__actions {
  gap: 12px;
  margin-top: 0;
  padding-top: 12px;
  border-top: 1px solid #d9d9d9;
}

.settings-cleaning-card-item .settings-card-item__actions ion-button {
  flex: 0 1 108px;
  min-width: 0;
  width: 108px;
  min-height: 30px;
  height: 30px;
  margin: 0;
  --padding-start: 8px;
  --padding-end: 8px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 10px;
  --box-shadow: none;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0;
}

.settings-cleaning-card-item .settings-card-item__actions ion-button[fill='outline'] {
  --background: var(--ios-pms-primary);
  --background-activated: var(--ion-color-primary-shade);
  --border-color: var(--ios-pms-primary);
  --color: #ffffff;
}

.settings-cleaning-card-item .settings-card-item__actions ion-button[color='danger'] {
  --background: rgba(255, 255, 255, 0.72);
  --background-activated: rgba(var(--ion-color-danger-rgb), 0.06);
  --border-color: #d9d9d9;
  --border-style: solid;
  --border-width: 1px;
  --color: var(--ion-color-danger);
}

.settings-cleaning-page :deep(.settings-cleaning-cleaners-card > .mobile-note) {
  margin: 18px 0 0;
  padding: 24px 12px;
  border: 1px dashed rgba(130, 143, 165, 0.24);
  border-radius: var(--ios-pms-radius-input);
  color: var(--ios-pms-text-muted);
  text-align: center;
}

:global(.settings-cleaner-editor-modal) {
  --width: 100%;
  --height: 100%;
  --border-radius: 0;
  --background: #eef6ff;
}

:global(.settings-cleaner-editor-modal ion-header) {
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

:global(.settings-cleaner-editor-modal ion-header::after) {
  display: none;
}

:global(.settings-cleaner-editor-modal ion-toolbar) {
  --background: rgba(255, 255, 255, 0.94);
  --border-color: transparent;
  --min-height: 64px;
  --padding-start: 16px;
  --padding-end: 16px;
}

:global(.settings-cleaner-editor-modal ion-title) {
  color: #333333;
  font-size: 23px;
  font-weight: 400;
  letter-spacing: 0;
}

:global(.settings-cleaner-editor-modal .settings-editor-modal__close-button) {
  min-height: 36px;
  margin: 0;
  --padding-start: 8px;
  --padding-end: 0;
  --background: transparent;
  --background-activated: transparent;
  --box-shadow: none;
  --color: #777777;
  color: #777777;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

:global(ion-content.settings-cleaner-editor-page) {
  --background: #eef6ff;
  --padding-top: 34px;
  --padding-bottom: calc(90px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
  background: #eef6ff;
}

:global(.settings-cleaner-editor-card) {
  width: 100%;
  margin: 0 auto;
  padding: 30px 16px 24px;
  border: 0;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 18px rgba(67, 92, 132, 0.08);
}

:global(.settings-cleaner-editor-form) {
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
}

:global(.settings-cleaner-editor-card .settings-form-field) {
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

:global(.settings-cleaner-editor-card .settings-form-field span) {
  color: #333333;
  font-size: 20px;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
}

:global(.settings-cleaner-editor-card .settings-form-field ion-input) {
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

:global(.settings-cleaner-editor-card .settings-form-field ion-input::part(native)) {
  color: #333333;
  font-size: 18px;
  font-weight: 400;
  line-height: 1.35;
}

:global(.settings-cleaner-editor-card .settings-form-actions) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 36px;
  margin-top: 32px;
  padding-top: 0;
  border-top: 0;
}

:global(.settings-cleaner-editor-card .settings-form-actions ion-button) {
  width: 100%;
  min-height: 30px;
  height: 30px;
  margin: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 7px;
  --box-shadow: none;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

:global(.settings-cleaner-editor-card .settings-form-actions ion-button[fill='outline']) {
  --background: rgba(255, 255, 255, 0.96);
  --background-activated: #f7f7f7;
  --border-color: #d9d9d9;
  --border-width: 1px;
  --color: #999999;
}

:global(.settings-cleaner-editor-card .settings-form-actions ion-button:not([fill='outline'])) {
  --background: #1890ff;
  --background-activated: #1683e6;
  --color: #ffffff;
}

@media (max-width: 374px) {
  .settings-cleaning-page :deep(.settings-cleaning-page__content) {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .settings-cleaning-page :deep(.settings-cleaning-page__hero),
  .settings-cleaning-page :deep(.settings-cleaning-config-card),
  .settings-cleaning-page :deep(.settings-cleaning-cleaners-card) {
    padding-right: 14px;
    padding-left: 14px;
  }

  .settings-cleaning-page :deep(.settings-page-block__section-header .mobile-section-title) {
    font-size: 20px;
  }

  .settings-cleaning-section-actions {
    gap: 5px;
  }

  .settings-cleaning-section-actions ion-button {
    --padding-start: 7px;
    --padding-end: 7px;
    font-size: 11px;
  }

  .settings-cleaning-config-actions {
    gap: 18px;
  }

  .settings-cleaning-card-item {
    padding-right: 12px;
    padding-left: 12px;
  }

  .settings-cleaning-card-item .settings-card-item__actions {
    gap: 8px;
  }

  .settings-cleaning-card-item .settings-card-item__actions ion-button {
    flex-basis: 96px;
    width: 96px;
    font-size: 13px;
  }
}
</style>
