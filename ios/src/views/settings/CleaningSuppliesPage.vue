<template>
  <SettingsPageShell
    :back-href="ROUTE_PATHS.settingsCleaningSettings"
    :title="$t('settingsStage4.cleaningSettings.tabs.supplies')"
    :hero-title="$t('settingsStage4.cleaningSettings.tabs.supplies')"
    :toolbar-action-label="$t('settingsStage4.roomGroup.addGroup')"
    :chips="[
      {
        label: `${$t('settingsStage4.cleaningSettings.tabs.supplies')} ${supplies.length}`,
      },
    ]"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.16')"
    page-class="settings-cleaning-supplies-page"
    content-class="settings-page-block mobile-page--dashboard settings-cleaning-supplies-page__content"
    hero-class="settings-page-block__hero mobile-dashboard-surface settings-cleaning-supplies-page__hero"
    stack-class="settings-cleaning-supplies-page__stack"
    @toolbar-action="handleCreateSupply"
    @refresh="handleRefresh"
  >
    <SettingsSectionCard
      :title="$t('stage5UiAttributes.50')"
      :loading="loading"
      card-class="mobile-dashboard-surface settings-cleaning-supplies-page__list-card"
      header-class="settings-page-block__section-header"
    >
      <div
        v-if="supplies.length > 0"
        class="mobile-list settings-minimal-list settings-cleaning-supplies-list"
      >
        <article
          v-for="supply in supplies"
          :key="supply.id"
          class="settings-minimal-card settings-cleaning-supply-card"
        >
          <div class="settings-minimal-card__header">
            <div class="settings-minimal-card__title-group">
              <strong>{{ supply.roomType }}</strong>
            </div>
            <span
              class="settings-minimal-card__badge"
              :class="
                supply.supplies
                  ? 'settings-minimal-card__badge--success'
                  : 'settings-minimal-card__badge--warning'
              "
            >
              {{ supply.supplies ? $t('stage5DynamicUi.30') : $t('stage5DynamicUi.52') }}
            </span>
          </div>

          <div class="settings-minimal-card__meta">
            <span class="settings-minimal-card__meta-pill">
              {{ supply.supplies || $t('stage5DynamicUi.32') }}
            </span>
          </div>

          <div class="settings-minimal-card__actions">
            <ion-button
              class="settings-cleaning-supply-card__edit-action"
              size="small"
              fill="outline"
              @click="handleEditSupply(supply)"
            >
              {{ $t('accommodation.roomPrice.editTitle') }}
            </ion-button>
            <ion-button
              class="settings-cleaning-supply-card__clear-action"
              size="small"
              fill="outline"
              @click="handleClearSupply(supply)"
            >
              {{ $t('accommodation.roomPriceBulk.clear') }}
            </ion-button>
            <ion-button
              class="settings-cleaning-supply-card__delete-action"
              size="small"
              fill="outline"
              @click="handleDeleteSupply(supply)"
            >
              {{ $t('roomStatus.roomLock.actions.delete') }}
            </ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note settings-cleaning-supplies-page__empty-state">
        {{ $t('stage5SourceText.81') }}
      </p>
    </SettingsSectionCard>

    <SettingsEditorModal
      :is-open="editorOpen"
      :title="
        editingSupplyId
          ? $t('settingsStage4.cleaningSettings.dialog.editSupply')
          : $t('stage5DynamicUi.39')
      "
      @close="handleDismissEditor"
      @didDismiss="handleDismissEditor"
    >
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>{{ $t('accommodation.common.roomType') }}</span>
          <ion-input
            v-model="supplyForm.roomType"
            fill="outline"
            :placeholder="$t('settingsStage4.roomSettings.placeholders.roomTypeName')"
          />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.134') }}</span>
          <ion-textarea
            v-model="supplyForm.supplies"
            :rows="5"
            fill="outline"
            :placeholder="$t('stage5UiAttributes.83')"
          />
        </label>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleDismissEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
        <ion-button :disabled="submitting" @click="handleSaveSupply">
          {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.14') }}
        </ion-button>
      </template>
    </SettingsEditorModal>
  </SettingsPageShell>
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
import SettingsEditorModal from '@/components/settings/base/SettingsEditorModal.vue'
import SettingsPageShell from '@/components/settings/base/SettingsPageShell.vue'
import SettingsSectionCard from '@/components/settings/base/SettingsSectionCard.vue'
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

async function handleRefresh(event: CustomEvent) {
  await loadPageData()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__content) {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 12px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-cleaning-supplies-page :deep(.app-page-header__title) {
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
}

.settings-cleaning-supplies-page :deep(.app-page-header__text-btn) {
  --color: #333333;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__hero) {
  margin: 10px 0 0;
  padding: 18px 16px 20px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__hero::before) {
  display: none;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__hero .mobile-title) {
  margin: 0;
  color: #333333;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.24;
  letter-spacing: 0;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__hero .mobile-chip-row) {
  gap: 7px;
  margin-top: 14px;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__hero .mobile-chip) {
  min-height: 23px;
  padding: 3px 10px;
  border: 0;
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(var(--ion-color-primary-rgb), 0.1);
  color: #1d73ff;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__stack) {
  gap: 0;
  margin-top: 20px;
  padding-bottom: 6px;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__list-card) {
  padding: 24px 16px 22px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-cleaning-supplies-page :deep(.settings-page-block__section-header) {
  align-items: center;
  min-height: 32px;
}

.settings-cleaning-supplies-page
  :deep(.settings-page-block__section-header .mobile-section-title) {
  margin: 0;
  color: #333333;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-cleaning-supplies-page :deep(.settings-page-block__section-header ion-spinner) {
  width: 18px;
  height: 18px;
  color: rgba(var(--ion-color-primary-rgb), 0.78);
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-list) {
  gap: 14px;
  margin-top: 18px;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supply-card) {
  padding: 16px 14px 18px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 6px 16px rgba(77, 98, 145, 0.035);
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supply-card::before) {
  display: none;
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__header) {
  flex-wrap: nowrap;
  align-items: flex-start;
  gap: 12px;
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__title-group) {
  flex: 1;
  gap: 0;
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__title-group strong) {
  color: #333333;
  font-size: 19px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__badge) {
  min-height: 28px;
  padding: 2px 10px;
  border: 0;
  border-radius: var(--ios-pms-radius-pill);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__badge--success) {
  background: rgba(var(--ion-color-success-rgb), 0.74);
  color: #ffffff;
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__badge--warning) {
  background: rgba(var(--ion-color-warning-rgb), 0.13);
  color: var(--ion-color-warning);
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__meta) {
  gap: 8px;
  margin-top: 12px;
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__meta-pill) {
  min-width: 0;
  max-width: 100%;
  min-height: 24px;
  padding: 2px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.78);
  color: #444444;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.35;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__actions) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #d9d9d9;
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__actions ion-button) {
  flex: 0 0 auto;
  width: auto;
  min-width: 0;
  height: 30px;
  min-height: 30px;
  margin: 0;
  --padding-start: 13px;
  --padding-end: 13px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-color: #d9d9d9;
  --border-style: solid;
  --border-width: 1px;
  --border-radius: 11px;
  --box-shadow: none;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0;
}

.settings-cleaning-supplies-page
  :deep(.settings-cleaning-supply-card .settings-minimal-card__actions ion-button::part(native)) {
  min-height: 30px;
  padding-top: 2px;
  padding-bottom: 2px;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supply-card__edit-action) {
  --background: rgba(255, 255, 255, 0.72);
  --background-activated: rgba(52, 116, 246, 0.05);
  --border-color: #d9d9d9;
  --color: #2346ff;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supply-card__clear-action) {
  --background: rgba(255, 255, 255, 0.72);
  --background-activated: rgba(51, 51, 51, 0.05);
  --border-color: #d9d9d9;
  --color: #333333;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supply-card__delete-action) {
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
  color: #ff0000;
}

.settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__empty-state) {
  margin: 18px 0 0;
  padding: 24px 12px;
  border: 1px dashed rgba(130, 143, 165, 0.24);
  border-radius: var(--ios-pms-radius-input);
  color: var(--ios-pms-text-muted);
  text-align: center;
}

@media (max-width: 374px) {
  .settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__content) {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__hero) {
    padding-right: 14px;
    padding-left: 14px;
  }

  .settings-cleaning-supplies-page :deep(.settings-cleaning-supplies-page__list-card) {
    padding-right: 14px;
    padding-left: 14px;
  }

  .settings-cleaning-supplies-page
    :deep(.settings-page-block__section-header .mobile-section-title) {
    font-size: 20px;
  }
}
</style>
