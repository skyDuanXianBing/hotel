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
    content-class="settings-page-block"
    hero-class="settings-page-block__hero"
    eyebrow-class="settings-page-block__eyebrow"
  >
    <SettingsSectionCard
      :title="$t('stage5UiAttributes.111')"
      :loading="loading"
      header-class="settings-page-block__section-header"
    >
      <div class="settings-toggle-field">
        <div>
          <strong>{{ $t('stage5SourceText.33') }}</strong>
        </div>
        <ion-toggle v-model="configForm.enabled" />
      </div>

      <div class="settings-form-grid settings-form-grid--top">
        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.7') }}</span>
          <ion-input v-model="configForm.stayStartTime" fill="outline" placeholder="10:00" />
        </label>
        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.8') }}</span>
          <ion-input v-model="configForm.stayEndTime" fill="outline" placeholder="15:00" />
        </label>
        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.218') }}</span>
          <ion-input v-model="configForm.checkoutStartTime" fill="outline" placeholder="11:00" />
        </label>
        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.219') }}</span>
          <ion-input v-model="configForm.checkoutEndTime" fill="outline" placeholder="17:00" />
        </label>
      </div>

      <div class="settings-toggle-field settings-toggle-field--top">
        <div>
          <strong>{{ $t('stage5SourceText.185') }}</strong>
        </div>
        <ion-toggle v-model="configForm.autoStayTask" />
      </div>

      <div class="settings-toggle-field settings-toggle-field--top">
        <div>
          <strong>{{ $t('stage5SourceText.186') }}</strong>
        </div>
        <ion-toggle v-model="configForm.autoCheckoutTask" />
      </div>

      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || savingConfig" @click="loadPageData">{{ $t('accommodation.common.reset') }}</ion-button>
        <ion-button :disabled="loading || savingConfig" @click="handleSaveConfig">
          {{ savingConfig ? $t('channel.mobile.common.saving') : $t('stage5DynamicUi.5') }}
        </ion-button>
        <ion-button fill="outline" @click="handleOpenSupplies">{{ $t('settingsStage4.cleaningSettings.tabs.supplies') }}</ion-button>
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard
      :title="$t('accommodation.common.cleaner')"
      header-class="settings-page-block__section-header"
    >
      <template #headerActions>
        <ion-button size="small" @click="handleCreateCleaner">{{ $t('stage5SourceText.25') }}</ion-button>
      </template>

      <div v-if="cleaners.length > 0" class="mobile-list settings-card-list">
        <article v-for="cleaner in cleaners" :key="cleaner.id" class="settings-card-item">
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
      @close="handleDismissEditor"
      @didDismiss="handleDismissEditor"
    >
      <div class="settings-form-grid">
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
