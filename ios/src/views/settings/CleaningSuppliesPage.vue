<template>
  <SettingsCrudPage
    :back-href="ROUTE_PATHS.settingsCleaningSettings"
    :title="$t('settingsStage4.cleaningSettings.tabs.supplies')"
    :hero-eyebrow="$t('settings.groups.cleaning')"
    :hero-title="$t('settingsStage4.cleaningSettings.tabs.supplies')"
    :toolbar-action-label="$t('settingsStage4.roomGroup.addGroup')"
    :section-title="$t('stage5UiAttributes.50')"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingSupplyId ? $t('settingsStage4.cleaningSettings.dialog.editSupply') : $t('stage5DynamicUi.39')"
    @toolbar-action="handleCreateSupply"
    @dismiss-editor="handleDismissEditor"
  >
    <div v-if="supplies.length > 0" class="mobile-list settings-minimal-list">
      <article v-for="supply in supplies" :key="supply.id" class="settings-minimal-card">
        <div class="settings-minimal-card__header">
          <div class="settings-minimal-card__title-group">
            <strong>{{ supply.roomType }}</strong>
            <p class="settings-minimal-card__summary settings-minimal-card__summary--clamp-two">
              {{ supply.supplies || $t('stage5DynamicUi.32') }}
            </p>
          </div>
          <span
            class="settings-minimal-card__badge"
            :class="supply.supplies ? 'settings-minimal-card__badge--success' : 'settings-minimal-card__badge--warning'"
          >
            {{ supply.supplies ? $t('stage5DynamicUi.30') : $t('stage5DynamicUi.52') }}
          </span>
        </div>

        <div class="settings-minimal-card__actions">
          <ion-button size="small" fill="outline" @click="handleEditSupply(supply)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
          <ion-button size="small" fill="outline" @click="handleClearSupply(supply)">{{ $t('accommodation.roomPriceBulk.clear') }}</ion-button>
          <ion-button size="small" color="danger" fill="clear" @click="handleDeleteSupply(supply)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note">{{ $t('stage5SourceText.81') }}</p>

    <template #modalContent>
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>{{ $t('accommodation.common.roomType') }}</span>
          <ion-input v-model="supplyForm.roomType" fill="outline" :placeholder="$t('settingsStage4.roomSettings.placeholders.roomTypeName')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.134') }}</span>
          <ion-textarea v-model="supplyForm.supplies" :rows="5" fill="outline" :placeholder="$t('stage5UiAttributes.83')" />
        </label>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveSupply">
        {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.14') }}
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
  clearCleaningSupply,
  createCleaningSupply,
  deleteCleaningSupply,
  getAllCleaningSupplies,
  updateCleaningSupply,
} from '@/api/cleaning'
import SettingsCrudPage from '@/components/settings/families/SettingsCrudPage.vue'
import { ROUTE_PATHS } from '@/router/guards'
import type { CleaningSupplyDTO } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const { t } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const editorOpen = ref(false)
const editingSupplyId = ref<number | null>(null)
const supplies = ref<CleaningSupplyDTO[]>([])
const supplyForm = ref({ roomType: '', supplies: '' })

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
      { text: t('auth.action.confirm'), role: 'destructive' },
    ],
  })
  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadPageData() {
  loading.value = true
  try {
    const response = await getAllCleaningSupplies()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5Pattern.loadFailed'))
    }
    supplies.value = response.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.loadFailed')))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateSupply() {
  editingSupplyId.value = null
  supplyForm.value = { roomType: '', supplies: '' }
  editorOpen.value = true
}

function handleEditSupply(supply: CleaningSupplyDTO) {
  editingSupplyId.value = supply.id
  supplyForm.value = { roomType: supply.roomType, supplies: supply.supplies }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingSupplyId.value = null
  supplyForm.value = { roomType: '', supplies: '' }
}

async function handleSaveSupply() {
  if (!supplyForm.value.roomType.trim()) {
    showWarningToast(t('settingsStage4.roomSettings.placeholders.roomTypeName'))
    return
  }

  submitting.value = true
  try {
    if (editingSupplyId.value) {
      const response = await updateCleaningSupply(editingSupplyId.value, {
        roomType: supplyForm.value.roomType.trim(),
        supplies: supplyForm.value.supplies.trim(),
      })
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.updateFailed'))
      }
    } else {
      const response = await createCleaningSupply({
        roomType: supplyForm.value.roomType.trim(),
        supplies: supplyForm.value.supplies.trim(),
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

async function handleClearSupply(supply: CleaningSupplyDTO) {
  const confirmed = await confirmDelete(supply.roomType)
  if (!confirmed) {
    return
  }
  try {
    const response = await clearCleaningSupply(supply.id)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.operationFailed'))
    }
    showSuccessToast(t('stage5Pattern.operationCompleted'))
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.operationFailed')))
    }
  }
}

async function handleDeleteSupply(supply: CleaningSupplyDTO) {
  const confirmed = await confirmDelete(supply.roomType)
  if (!confirmed) {
    return
  }
  try {
    const response = await deleteCleaningSupply(supply.id)
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
