<template>
  <SettingsPageShell
    page-class="settings-pricing-tools-page"
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.entries.pricingTools.0')"
    :hero-eyebrow="$t('settings.groups.integrations')"
    hero-title="PriceLabs"
    :toolbar-action-label="$t('settingsStage4.pricingTools.actions.addConnection')"
    content-class="settings-page-block"
    hero-class="settings-page-block__hero"
    eyebrow-class="settings-page-block__eyebrow"
    @toolbar-action="handleOpenConnectionEditor"
  >
    <SettingsSectionCard
      :title="$t('settingsStage4.pricingTools.sections.integrationStatus')"
      :loading="loading"
      card-class="settings-pricing-tools__integration-card"
      header-class="settings-page-block__section-header"
    >
      <div class="settings-toggle-field settings-pricing-tools__integration-toggle">
        <div>
          <strong>{{ $t('stage5SourceText.32') }}</strong>
        </div>
        <ion-toggle v-model="integrationForm.isEnabled" />
      </div>

      <div class="settings-form-grid settings-form-grid--top">
        <label class="settings-form-field settings-pricing-tools__email-field">
          <span>{{ $t('settingsStage4.pricingTools.columns.priceLabsEmail') }}</span>
          <ion-input v-model="integrationForm.priceLabsEmail" fill="outline" :placeholder="$t('auth.placeholder.email')" />
        </label>
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard
      :title="$t('stage5UiAttributes.107')"
      card-class="settings-pricing-tools__connections-card"
      header-class="settings-page-block__section-header"
    >
      <template #headerActions>
        <ion-button class="settings-pricing-tools__add-connection" size="small" @click="handleOpenConnectionEditor">
          {{ $t('settingsStage4.pricingTools.actions.addConnection') }}
        </ion-button>
      </template>

      <div v-if="connections.length > 0" class="mobile-list settings-card-list settings-pricing-tools__connection-list">
        <article
          v-for="connection in connections"
          :key="connection.id"
          class="settings-card-item settings-pricing-tools__connection-item"
        >
          <div class="settings-pricing-tools__connection-content">
            <strong>{{ connection.roomTypeName }} / {{ connection.pricePlanName }}</strong>
            <span
              class="settings-pricing-tools__connection-status"
              :class="connection.isEnabled ? 'is-active' : 'is-inactive'"
            >
              {{ connection.isEnabled ? $t('channel.managementData.statusActive') : $t('stage5DynamicUi.28') }}
            </span>
            <p v-if="connection.errorMessage" class="settings-pricing-tools__connection-error">
              {{ connection.errorMessage }}
            </p>
          </div>
          <div class="settings-card-item__actions">
            <ion-button
              class="settings-pricing-tools__connection-action"
              size="small"
              fill="outline"
              @click="handleToggleConnection(connection)"
            >
              {{ connection.isEnabled ? $t('roomStatus.store.roomState.outOfOrder') : $t('settingsStage4.accountList.status.enabled') }}
            </ion-button>
            <ion-button
              class="settings-pricing-tools__connection-action settings-pricing-tools__connection-action--danger"
              size="small"
              fill="outline"
              @click="handleDeleteConnection(connection)"
            >
              {{ $t('roomStatus.roomLock.actions.delete') }}
            </ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note settings-pricing-tools__empty-state">
        {{ $t('stage5SourceText.88') }}
      </p>

      <div class="settings-form-actions settings-form-actions--section settings-pricing-tools__primary-actions">
        <ion-button fill="outline" :disabled="loading || savingIntegration" @click="loadPageData">
          {{ $t('accommodation.common.reset') }}
        </ion-button>
        <ion-button :disabled="loading || savingIntegration" @click="handleSaveIntegration">
          {{ savingIntegration ? $t('channel.mobile.common.saving') : $t('settingsResidual.common.saveSettings') }}
        </ion-button>
        <ion-button fill="outline" :disabled="loading || syncing" @click="handleManualSync">
          {{ syncing ? $t('channel.mobile.sync.syncing') : $t('stage5DynamicUi.59') }}
        </ion-button>
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard
      :title="$t('stage5UiAttributes.55')"
      card-class="settings-pricing-tools__secondary-card"
      header-class="settings-page-block__section-header"
    >
      <p v-if="!canManageChannels" class="mobile-note settings-pricing-tools__permission-note">
        {{ $t('stage5SourceText.160') }}
      </p>

      <div
        v-if="adjustments.length > 0"
        class="mobile-list settings-card-list settings-pricing-tools__secondary-list"
      >
        <article
          v-for="adjustment in adjustments"
          :key="adjustment.channelId"
          class="settings-card-item settings-pricing-tools__secondary-item"
        >
          <div class="settings-pricing-tools__secondary-header">
            <div class="settings-pricing-tools__secondary-title-group">
              <strong>{{ adjustment.channelName }}</strong>
              <p>{{ $t('stage5SourceText.209') }} · {{ formatAdjustmentTypeLabel(adjustment.adjustmentType) }}</p>
            </div>
            <span
              class="settings-pricing-tools__secondary-badge"
              :class="adjustment.autoSyncPrice ? 'is-active' : 'is-inactive'"
            >
              {{ adjustment.autoSyncPrice ? $t('stage5DynamicUi.71') : $t('stage5DynamicUi.70') }}
            </span>
          </div>

          <div class="settings-pricing-tools__secondary-meta">
            <span>
              {{ $t('settingsStage4.pricingTools.fields.adjustmentValue') }}
              {{ adjustment.adjustmentValue ?? '-' }}
            </span>
          </div>

          <div class="settings-card-item__actions settings-pricing-tools__secondary-actions">
            <ion-button
              class="settings-pricing-tools__secondary-action"
              size="small"
              fill="outline"
              :disabled="!canManageChannels"
              @click="handleEditAdjustment(adjustment)"
            >
              {{ $t('accommodation.roomPrice.editTitle') }}
            </ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note settings-pricing-tools__empty-state">
        {{ $t('stage5SourceText.85') }}
      </p>
    </SettingsSectionCard>

    <SettingsSectionCard
      :title="$t('settingsStage4.pricingTools.columns.recentSync')"
      card-class="settings-pricing-tools__secondary-card"
      header-class="settings-page-block__section-header"
    >
      <div
        v-if="logs.length > 0"
        class="mobile-list settings-card-list settings-pricing-tools__secondary-list"
      >
        <article
          v-for="log in logs"
          :key="log.id"
          class="settings-card-item settings-pricing-tools__secondary-item settings-pricing-tools__sync-item"
        >
          <div class="settings-pricing-tools__secondary-header">
            <div class="settings-pricing-tools__secondary-title-group">
              <strong>{{ log.syncTypeDisplay }}</strong>
              <p>{{ log.createdAt }}</p>
            </div>
            <span class="settings-pricing-tools__secondary-badge settings-pricing-tools__sync-status">
              {{ log.statusDisplay }}
            </span>
          </div>

          <div class="settings-pricing-tools__secondary-meta">
            <span>{{ log.directionDisplay }}</span>
          </div>

          <p
            v-if="log.errorMessage"
            class="settings-pricing-tools__sync-message settings-pricing-tools__sync-message--error"
          >
            {{ log.errorMessage }}
          </p>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note settings-pricing-tools__empty-state">
        {{ $t('stage5SourceText.77') }}
      </p>
    </SettingsSectionCard>

    <SettingsEditorModal
      :is-open="connectionEditorOpen"
      :title="$t('settingsStage4.pricingTools.dialog.addConnection')"
      :close-text="$t('common.back')"
      close-slot="start"
      modal-class="settings-pricing-tools-editor-modal"
      content-class="settings-pricing-tools-editor-page"
      card-class="settings-pricing-tools-editor-card"
      @close="handleCloseConnectionEditor"
      @didDismiss="handleCloseConnectionEditor"
    >
      <div class="settings-form-grid settings-pricing-tools-editor-form">
        <label class="settings-form-field">
          <span>{{ $t('accommodation.common.roomType') }}</span>
          <ion-select v-model="connectionForm.roomTypeId" fill="outline" interface="modal">
            <ion-select-option v-for="roomType in roomTypes" :key="roomType.id" :value="roomType.id">
              {{ roomType.name }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field">
          <span>{{ $t('accommodation.roomPriceBulk.table.pricePlan') }}</span>
          <ion-select v-model="connectionForm.pricePlanId" fill="outline" interface="modal">
            <ion-select-option v-for="plan in pricePlans" :key="plan.id" :value="plan.id">
              {{ plan.name }}
            </ion-select-option>
          </ion-select>
        </label>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleCloseConnectionEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
        <ion-button :disabled="submittingConnection" @click="handleSaveConnection">
          {{ submittingConnection ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.20') }}
        </ion-button>
      </template>
    </SettingsEditorModal>

    <SettingsEditorModal
      :is-open="adjustmentEditorOpen"
      :title="$t('stage5UiAttributes.57')"
      :close-text="$t('common.back')"
      close-slot="start"
      modal-class="settings-pricing-tools-editor-modal"
      content-class="settings-pricing-tools-editor-page"
      card-class="settings-pricing-tools-editor-card"
      @close="handleCloseAdjustmentEditor"
      @didDismiss="handleCloseAdjustmentEditor"
    >
      <div class="settings-form-grid settings-pricing-tools-editor-form">
        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.209') }}</span>
          <ion-select
            v-model="adjustmentForm.adjustmentType"
            fill="outline"
            interface="action-sheet"
            :disabled="!canManageChannels"
          >
            <ion-select-option value="COMMISSION">
              {{ $t('settingsStage4.pricingTools.adjustment.commission') }}
            </ion-select-option>
            <ion-select-option value="FIXED">
              {{ $t('settingsStage4.pricingTools.adjustment.fixed') }}
            </ion-select-option>
            <ion-select-option value="PERCENTAGE">
              {{ $t('settingsStage4.pricingTools.adjustment.percentage') }}
            </ion-select-option>
          </ion-select>
        </label>
        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.pricingTools.fields.adjustmentValue') }}</span>
          <ion-input
            v-model="adjustmentForm.adjustmentValue"
            fill="outline"
            inputmode="decimal"
            :placeholder="$t('stage5UiAttributes.96')"
            :disabled="!canManageChannels"
          />
        </label>
        <div class="settings-toggle-field">
          <div>
            <strong>{{ $t('stage5SourceText.184') }}</strong>
          </div>
          <ion-toggle v-model="adjustmentForm.autoSyncPrice" :disabled="!canManageChannels" />
        </div>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleCloseAdjustmentEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
        <ion-button :disabled="submittingAdjustment || !canManageChannels" @click="handleSaveAdjustment">
          {{ submittingAdjustment ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.3') }}
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
  IonSelect,
  IonSelectOption,
  IonToggle,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
import {
  createConnection,
  deleteConnection,
  getChannelPriceAdjustments,
  getConnections,
  getIntegration,
  getRecentSyncLogs,
  manualSync,
  saveIntegration,
  updateChannelPriceAdjustment,
  updateConnectionStatus,
  type ChannelPriceAdjustmentDTO,
  type PriceLabsConnectionDTO,
  type PriceLabsIntegrationDTO,
} from '@/api/pricelabs'
import { PermissionAction, PermissionModule } from '@/api/role'
import { getAllPricePlans, type PricePlanDTO } from '@/api/pricePlan'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import SettingsEditorModal from '@/components/settings/base/SettingsEditorModal.vue'
import SettingsPageShell from '@/components/settings/base/SettingsPageShell.vue'
import SettingsSectionCard from '@/components/settings/base/SettingsSectionCard.vue'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { hasCurrentStorePermission } from '@/utils/permissions'
import { isHandledRequestError } from '@/utils/request'
import { normalizeOptionalNumber } from '@/utils/settings'

const { t } = useI18n()

const userStore = useUserStore()

const loading = ref(false)
const savingIntegration = ref(false)
const syncing = ref(false)
const submittingConnection = ref(false)
const submittingAdjustment = ref(false)
const canManageChannels = ref(false)
const integrationForm = ref<Partial<PriceLabsIntegrationDTO>>({
  isEnabled: false,
  priceLabsEmail: '',
})
const connections = ref<PriceLabsConnectionDTO[]>([])
const adjustments = ref<ChannelPriceAdjustmentDTO[]>([])
const roomTypes = ref<RoomTypeDTO[]>([])
const pricePlans = ref<PricePlanDTO[]>([])
const logs = ref<Array<{ id: number; syncTypeDisplay: string; statusDisplay: string; directionDisplay: string; errorMessage?: string; createdAt: string }>>([])
const connectionEditorOpen = ref(false)
const adjustmentEditorOpen = ref(false)
const connectionForm = ref({ roomTypeId: null as number | null, pricePlanId: null as number | null })
const selectedAdjustmentChannelId = ref<number | null>(null)
const adjustmentForm = ref({ adjustmentType: 'FIXED', adjustmentValue: '', autoSyncPrice: false })

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function formatAdjustmentTypeLabel(value: string) {
  const labelKeys: Record<string, string> = {
    COMMISSION: 'settingsStage4.pricingTools.adjustment.commission',
    FIXED: 'settingsStage4.pricingTools.adjustment.fixed',
    PERCENTAGE: 'settingsStage4.pricingTools.adjustment.percentage',
  }
  const labelKey = labelKeys[value]
  return labelKey ? t(labelKey) : value
}

function ensureCanManageChannels(actionLabel: string) {
  if (canManageChannels.value) {
    return true
  }

  showWarningToast(
    `${t('stage5Pattern.permission')}: ${actionLabel}`,
  )
  return false
}

async function loadManageChannelsPermission() {
  try {
    canManageChannels.value = await hasCurrentStorePermission({
      module: PermissionModule.CHANNEL,
      action: PermissionAction.MANAGE_CHANNELS,
    })
  } catch (error) {
    canManageChannels.value = false
    console.warn('加载渠道管理权限失败', error)
  }
}

async function confirmDelete(name: string, title: string) {
  const alert = await alertController.create({
    header: title,
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
  if (!userId) {
    showWarningToast(t('stage5Pattern.setup'))
    return
  }

  loading.value = true
  try {
    const [integrationResponse, connectionResponse, adjustmentResponse, roomTypeResponse, pricePlanResponse, logResponse] = await Promise.all([
      getIntegration(),
      getConnections(),
      getChannelPriceAdjustments(),
      getAllRoomTypes(),
      getAllPricePlans(userId),
      getRecentSyncLogs(6),
      loadManageChannelsPermission(),
    ])

    if (integrationResponse.success && integrationResponse.data) {
      integrationForm.value = {
        isEnabled: integrationResponse.data.isEnabled,
        priceLabsEmail: integrationResponse.data.priceLabsEmail || '',
      }
    }
    connections.value = connectionResponse.success && connectionResponse.data ? connectionResponse.data : []
    adjustments.value = adjustmentResponse.success && adjustmentResponse.data ? adjustmentResponse.data : []
    roomTypes.value = roomTypeResponse.success && roomTypeResponse.data ? roomTypeResponse.data : []
    pricePlans.value = pricePlanResponse.success && pricePlanResponse.data ? pricePlanResponse.data : []
    logs.value = logResponse.success && logResponse.data ? logResponse.data : []
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.loadFailed')))
    }
  } finally {
    loading.value = false
  }
}

async function handleSaveIntegration() {
  savingIntegration.value = true
  try {
    const response = await saveIntegration({
      isEnabled: Boolean(integrationForm.value.isEnabled),
      priceLabsEmail: integrationForm.value.priceLabsEmail?.trim(),
    })
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
    savingIntegration.value = false
  }
}

async function handleManualSync() {
  syncing.value = true
  try {
    const response = await manualSync()
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.syncFailed'))
    }
    showSuccessToast(t('stage5Pattern.syncCompleted'))
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.syncFailed')))
    }
  } finally {
    syncing.value = false
  }
}

function handleOpenConnectionEditor() {
  connectionForm.value = { roomTypeId: null, pricePlanId: null }
  connectionEditorOpen.value = true
}

function handleCloseConnectionEditor() {
  connectionEditorOpen.value = false
}

async function handleSaveConnection() {
  if (!connectionForm.value.roomTypeId || !connectionForm.value.pricePlanId) {
    showWarningToast(t('stage5Pattern.select'))
    return
  }

  submittingConnection.value = true
  try {
    const response = await createConnection(connectionForm.value.roomTypeId, connectionForm.value.pricePlanId)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.createFailed'))
    }
    showSuccessToast(t('stage5Pattern.createCompleted'))
    handleCloseConnectionEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.createFailed')))
    }
  } finally {
    submittingConnection.value = false
  }
}

async function handleToggleConnection(connection: PriceLabsConnectionDTO) {
  try {
    const response = await updateConnectionStatus(connection.id, !connection.isEnabled)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.updateFailed'))
    }
    showSuccessToast(
      response.data.isEnabled
        ? t('settingsResidual.common.enabled')
        : t('settingsResidual.common.disabled'),
    )
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.updateFailed')))
    }
  }
}

async function handleDeleteConnection(connection: PriceLabsConnectionDTO) {
  const confirmed = await confirmDelete(
    `${connection.roomTypeName} / ${connection.pricePlanName}`,
    t('settingsResidual.common.confirm'),
  )
  if (!confirmed) {
    return
  }
  try {
    const response = await deleteConnection(connection.id)
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

function handleEditAdjustment(adjustment: ChannelPriceAdjustmentDTO) {
  if (!ensureCanManageChannels(t('settingsStage4.pricingTools.dialog.editAdjustment'))) {
    return
  }

  selectedAdjustmentChannelId.value = adjustment.channelId
  adjustmentForm.value = {
    adjustmentType: adjustment.adjustmentType,
    adjustmentValue: adjustment.adjustmentValue === null ? '' : String(adjustment.adjustmentValue),
    autoSyncPrice: adjustment.autoSyncPrice,
  }
  adjustmentEditorOpen.value = true
}

function handleCloseAdjustmentEditor() {
  adjustmentEditorOpen.value = false
  selectedAdjustmentChannelId.value = null
  adjustmentForm.value = { adjustmentType: 'FIXED', adjustmentValue: '', autoSyncPrice: false }
}

async function handleSaveAdjustment() {
  if (!ensureCanManageChannels(t('settingsStage4.pricingTools.dialog.editAdjustment'))) {
    return
  }

  if (!selectedAdjustmentChannelId.value) {
    return
  }

  const adjustmentValue = normalizeOptionalNumber(adjustmentForm.value.adjustmentValue)
  if (adjustmentValue === null) {
    showWarningToast(t('stage5Pattern.enter'))
    return
  }

  submittingAdjustment.value = true
  try {
    const response = await updateChannelPriceAdjustment(selectedAdjustmentChannelId.value, {
      adjustmentType: adjustmentForm.value.adjustmentType as 'COMMISSION' | 'FIXED' | 'PERCENTAGE',
      adjustmentValue: adjustmentValue ?? null,
      autoSyncPrice: adjustmentForm.value.autoSyncPrice,
    })
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.saveFailed'))
    }
    showSuccessToast(t('stage5Pattern.saveCompleted'))
    handleCloseAdjustmentEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    submittingAdjustment.value = false
  }
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.settings-pricing-tools-page :deep(.settings-page-block) {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 10px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-pricing-tools-page :deep(.app-page-header__title) {
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
}

.settings-pricing-tools-page :deep(.app-page-header__text-btn) {
  --color: #333333;
  margin: 0;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-pricing-tools-page :deep(.settings-page-block__hero) {
  margin: 0 0 10px;
  padding: 17px 16px 19px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-pricing-tools-page :deep(.settings-page-block__hero::before) {
  display: none;
}

.settings-pricing-tools-page :deep(.settings-page-block__eyebrow) {
  margin: 0;
  color: #1677ff;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-pricing-tools-page :deep(.settings-page-block__hero .mobile-title) {
  margin: 11px 0 0;
  color: #333333;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-pricing-tools-page :deep(.settings-page-shell__stack) {
  gap: 26px;
  margin-top: 0;
  padding-bottom: 8px;
}

.settings-pricing-tools-page :deep(.settings-page-shell__stack > .mobile-card) {
  padding: 18px 16px 24px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-pricing-tools-page :deep(.settings-page-block__section-header) {
  align-items: center;
  min-height: 32px;
}

.settings-pricing-tools-page :deep(.settings-page-block__section-header .mobile-section-title),
.settings-pricing-tools-page :deep(.settings-pricing-tools__secondary-card .mobile-section-title) {
  margin: 0;
  color: #333333;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-pricing-tools-page :deep(.settings-page-block__section-header ion-spinner) {
  width: 18px;
  height: 18px;
  color: rgba(var(--ion-color-primary-rgb), 0.78);
}

.settings-pricing-tools-page :deep(.settings-pricing-tools__integration-card) {
  padding-bottom: 23px;
}

.settings-pricing-tools__integration-toggle {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 41px;
  margin-top: 18px;
  padding: 8px 16px;
  border: 0;
  border-radius: 9px;
  background: #dceaff;
  box-shadow: none;
}

.settings-pricing-tools__integration-toggle strong {
  color: #333333;
  font-size: 18px;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-pricing-tools__integration-toggle ion-toggle {
  width: 48px;
  min-width: 48px;
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

.settings-pricing-tools__integration-toggle ion-toggle::part(track) {
  width: 48px;
  height: 22px;
  border-radius: var(--ios-pms-radius-pill);
}

.settings-pricing-tools__integration-toggle ion-toggle::part(handle) {
  top: 0;
  width: 22px;
  height: 22px;
  margin: 0;
  border-radius: 50%;
  box-shadow: none;
}

.settings-pricing-tools-page :deep(.settings-form-grid--top) {
  margin-top: 16px;
}

.settings-pricing-tools__email-field {
  display: grid;
  gap: 1px;
  min-height: 62px;
  padding: 10px 10px 8px;
  border: 1px solid #d2d3d8;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.58);
  box-shadow: none;
  transform: none;
  transition: border-color 0.2s ease;
}

.settings-pricing-tools__email-field:focus-within {
  border-color: #9bbef4;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.08);
  transform: none;
}

.settings-pricing-tools__email-field span {
  color: #666666;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-pricing-tools__email-field ion-input {
  min-height: 27px;
  height: 27px;
  margin: 0;
  --background: transparent;
  --border-color: transparent;
  --border-radius: 0;
  --color: #666666;
  --highlight-color-focused: transparent;
  --highlight-color-valid: transparent;
  --highlight-color-invalid: transparent;
  --padding-start: 0;
  --padding-end: 0;
  --padding-top: 0;
  --padding-bottom: 0;
}

.settings-pricing-tools__email-field ion-input::part(native) {
  color: #666666;
  font-size: 18px;
  font-weight: 400;
  line-height: 1.25;
}

.settings-pricing-tools-page :deep(.settings-pricing-tools__connections-card) {
  padding-bottom: 24px;
}

.settings-pricing-tools__add-connection {
  min-width: 100px;
  min-height: 24px;
  height: 24px;
  margin: 0;
  --background: #266eff;
  --background-activated: #1f5fe0;
  --border-radius: 6px;
  --box-shadow: none;
  --color: #ffffff;
  --padding-start: 14px;
  --padding-end: 14px;
  font-size: 15px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-pricing-tools__connection-list {
  gap: 16px;
  margin-top: 18px;
}

.settings-pricing-tools__connection-item {
  overflow: hidden;
  padding: 12px 16px 15px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 5px 12px rgba(77, 98, 145, 0.025);
}

.settings-pricing-tools__connection-content {
  display: grid;
  justify-items: start;
  gap: 10px;
  min-width: 0;
}

.settings-pricing-tools__connection-content strong {
  color: #333333;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.settings-pricing-tools__connection-status {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 9px;
  border-radius: var(--ios-pms-radius-pill);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-pricing-tools__connection-status.is-active {
  background: rgba(var(--ion-color-success-rgb), 0.74);
  color: #ffffff;
}

.settings-pricing-tools__connection-status.is-inactive {
  background: rgba(var(--ion-color-warning-rgb), 0.13);
  color: var(--ion-color-warning);
}

.settings-pricing-tools__connection-error {
  color: var(--ion-color-danger);
  font-size: 12px;
  line-height: 1.35;
}

.settings-pricing-tools__connection-item .settings-card-item__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #d9d9d9;
}

.settings-pricing-tools__connection-action {
  flex: 0 0 auto;
  width: auto;
  min-width: 48px;
  min-height: 30px;
  height: 30px;
  margin: 0;
  --background: rgba(255, 255, 255, 0.78);
  --background-activated: #f7f7f7;
  --border-color: #d9d9d9;
  --border-radius: 10px;
  --border-style: solid;
  --border-width: 1px;
  --box-shadow: none;
  --color: #333333;
  --padding-start: 9px;
  --padding-end: 9px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0;
}

.settings-pricing-tools__connection-action::part(native) {
  min-height: 30px;
}

.settings-pricing-tools__connection-action--danger {
  --background-activated: rgba(255, 0, 0, 0.05);
  --color: #ff0000;
  --color-activated: #ff0000;
  --color-focused: #ff0000;
  --color-hover: #ff0000;
  color: #ff0000;
}

.settings-pricing-tools__empty-state {
  margin: 18px 0 0;
  padding: 24px 12px;
  border: 1px dashed rgba(130, 143, 165, 0.24);
  border-radius: 10px;
  color: var(--ios-pms-text-muted);
  text-align: center;
}

.settings-pricing-tools__primary-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 36px;
  margin-top: 16px;
  padding-top: 0;
  border-top: 0;
}

.settings-pricing-tools__primary-actions ion-button {
  width: 100%;
  min-width: 0;
  min-height: 24px;
  height: 24px;
  margin: 0;
  --padding-start: 5px;
  --padding-end: 5px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 6px;
  --box-shadow: none;
  font-size: 15px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-pricing-tools__primary-actions ion-button[fill='outline'] {
  --background: rgba(255, 255, 255, 0.94);
  --background-activated: #f7f7f7;
  --border-color: #d9d9d9;
  --border-width: 1px;
  --color: #333333;
}

.settings-pricing-tools__primary-actions ion-button:not([fill='outline']) {
  --background: #1890ff;
  --background-activated: #1683e6;
  --color: #ffffff;
}

.settings-pricing-tools__primary-actions ion-button[disabled] {
  opacity: 1;
  --background: rgba(255, 255, 255, 0.82);
  --border-color: #d9d9d9;
  --color: #a8a8a8;
}

.settings-pricing-tools-page :deep(.settings-pricing-tools__secondary-card) {
  padding-bottom: 24px;
}

.settings-pricing-tools__permission-note {
  margin: 16px 0 0;
  padding: 10px 12px;
  border: 1px solid rgba(var(--ion-color-warning-rgb), 0.16);
  border-radius: 10px;
  background: rgba(var(--ion-color-warning-rgb), 0.08);
  color: var(--ion-color-warning);
  font-size: 13px;
  line-height: 1.45;
}

.settings-pricing-tools__secondary-list {
  gap: 14px;
  margin-top: 18px;
}

.settings-pricing-tools__secondary-item {
  position: relative;
  overflow: hidden;
  padding: 15px 14px 16px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 6px 16px rgba(77, 98, 145, 0.035);
}

.settings-pricing-tools__secondary-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.settings-pricing-tools__secondary-title-group {
  display: grid;
  flex: 1;
  gap: 6px;
  min-width: 0;
}

.settings-pricing-tools__secondary-title-group strong,
.settings-pricing-tools__secondary-title-group p {
  margin: 0;
}

.settings-pricing-tools__secondary-title-group strong {
  color: #333333;
  font-size: 19px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.settings-pricing-tools__secondary-title-group p {
  color: #999999;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.4;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.settings-pricing-tools__secondary-badge {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  max-width: 48%;
  min-height: 27px;
  padding: 3px 10px;
  border: 0;
  border-radius: var(--ios-pms-radius-pill);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
  text-align: center;
  white-space: normal;
  overflow-wrap: anywhere;
}

.settings-pricing-tools__secondary-badge.is-active {
  background: rgba(var(--ion-color-success-rgb), 0.74);
  color: #ffffff;
}

.settings-pricing-tools__secondary-badge.is-inactive {
  background: rgba(var(--ion-color-warning-rgb), 0.13);
  color: var(--ion-color-warning);
}

.settings-pricing-tools__sync-status {
  border: 1px solid rgba(var(--ion-color-primary-rgb), 0.12);
  background: rgba(var(--ion-color-primary-rgb), 0.09);
  color: var(--ios-pms-primary);
}

.settings-pricing-tools__secondary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.settings-pricing-tools__secondary-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 27px;
  padding: 3px 10px;
  border: 1px solid rgba(130, 143, 165, 0.24);
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(255, 255, 255, 0.78);
  color: #444444;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.settings-pricing-tools__secondary-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(130, 143, 165, 0.24);
}

.settings-pricing-tools__secondary-action {
  flex: 0 0 auto;
  width: auto;
  min-width: 58px;
  min-height: 29px;
  height: 29px;
  margin: 0;
  --background: rgba(255, 255, 255, 0.78);
  --background-activated: rgba(var(--ion-color-primary-rgb), 0.06);
  --border-color: #d9d9d9;
  --border-radius: 9px;
  --border-style: solid;
  --border-width: 1px;
  --box-shadow: none;
  --color: var(--ios-pms-primary);
  --padding-start: 12px;
  --padding-end: 12px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0;
}

.settings-pricing-tools__secondary-action::part(native) {
  min-height: 29px;
}

.settings-pricing-tools__secondary-action.button-disabled {
  opacity: 0.42;
}

.settings-pricing-tools__sync-message {
  margin: 12px 0 0;
  padding: 9px 10px;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.settings-pricing-tools__sync-message--error {
  border: 1px solid rgba(var(--ion-color-danger-rgb), 0.14);
  background: rgba(var(--ion-color-danger-rgb), 0.07);
  color: var(--ion-color-danger);
}

:global(.settings-pricing-tools-editor-modal) {
  --width: 100%;
  --height: 100%;
  --border-radius: 0;
  --background: #eef6ff;
}

:global(.settings-pricing-tools-editor-modal ion-header) {
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

:global(.settings-pricing-tools-editor-modal ion-header::after) {
  display: none;
}

:global(.settings-pricing-tools-editor-modal ion-toolbar) {
  --background: rgba(255, 255, 255, 0.94);
  --border-color: transparent;
  --min-height: 64px;
  --padding-start: 16px;
  --padding-end: 16px;
}

:global(.settings-pricing-tools-editor-modal ion-title) {
  color: #333333;
  font-size: 23px;
  font-weight: 400;
  letter-spacing: 0;
}

:global(.settings-pricing-tools-editor-modal .settings-editor-modal__close-button) {
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

:global(.settings-pricing-tools-editor-modal .settings-editor-modal__close-button::part(native)) {
  gap: 6px;
  white-space: normal;
}

:global(.settings-pricing-tools-editor-modal .settings-editor-modal__back-icon) {
  display: inline-flex;
  align-items: center;
  color: #777777;
  font-size: 40px;
  font-weight: 300;
  line-height: 0.75;
  transform: translateY(-1px);
}

:global(ion-content.settings-pricing-tools-editor-page) {
  --background: #eef6ff;
  --padding-top: 34px;
  --padding-bottom: calc(90px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
  background: #eef6ff;
}

:global(.settings-pricing-tools-editor-card) {
  width: 100%;
  margin: 0 auto;
  padding: 30px 16px 24px;
  border: 0;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 18px rgba(67, 92, 132, 0.08);
}

:global(.settings-pricing-tools-editor-form) {
  gap: 18px;
}

:global(.settings-pricing-tools-editor-card .settings-form-field) {
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

:global(.settings-pricing-tools-editor-card .settings-form-field span),
:global(.settings-pricing-tools-editor-card .settings-toggle-field strong) {
  color: #333333;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
}

:global(.settings-pricing-tools-editor-card .settings-form-field span) {
  font-size: 20px;
}

:global(.settings-pricing-tools-editor-card .settings-toggle-field strong) {
  font-size: 18px;
}

:global(.settings-pricing-tools-editor-card .settings-form-field ion-input),
:global(.settings-pricing-tools-editor-card .settings-form-field ion-select) {
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

:global(.settings-pricing-tools-editor-card .settings-toggle-field) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 41px;
  margin-top: 14px;
  padding: 8px 16px;
  border: 0;
  border-radius: 9px;
  background: #dceaff;
  box-shadow: none;
}

:global(.settings-pricing-tools-editor-card .settings-toggle-field ion-toggle) {
  width: 44px;
  min-width: 44px;
  height: 22px;
  min-height: 22px;
  --track-background: #d9d9d9;
  --track-background-checked: linear-gradient(90deg, #81bfff 0%, #017cfe 100%);
  --handle-background: #ffffff;
  --handle-background-checked: #ffffff;
  --handle-width: 22px;
  --handle-height: 22px;
  --handle-spacing: 0;
  --handle-box-shadow: none;
}

:global(.settings-pricing-tools-editor-card .settings-form-actions) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 36px;
  margin-top: 18px;
  padding-top: 0;
  border-top: 0;
}

:global(.settings-pricing-tools-editor-card .settings-form-actions ion-button) {
  width: 100%;
  min-height: 38px;
  height: auto;
  margin: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 6px;
  --box-shadow: none;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

:global(.settings-pricing-tools-editor-card .settings-form-actions ion-button::part(native)) {
  min-height: 38px;
  padding-top: 6px;
  padding-bottom: 6px;
  white-space: normal;
}

:global(.settings-pricing-tools-editor-card .settings-form-actions ion-button[fill='outline']) {
  --background: rgba(255, 255, 255, 0.96);
  --background-activated: #f7f7f7;
  --border-color: #d9d9d9;
  --border-width: 1px;
  --color: #999999;
}

:global(.settings-pricing-tools-editor-card .settings-form-actions ion-button:not([fill='outline'])) {
  --background: #1890ff;
  --background-activated: #1683e6;
  --color: #ffffff;
}

@media (max-width: 374px) {
  .settings-pricing-tools-page :deep(.settings-page-block) {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .settings-pricing-tools-page :deep(.settings-page-shell__stack > .mobile-card) {
    padding-right: 14px;
    padding-left: 14px;
  }

  .settings-pricing-tools-page :deep(.settings-page-block__section-header .mobile-section-title),
  .settings-pricing-tools-page :deep(.settings-pricing-tools__secondary-card .mobile-section-title) {
    font-size: 20px;
  }

  .settings-pricing-tools__add-connection {
    min-width: 90px;
    --padding-start: 10px;
    --padding-end: 10px;
    font-size: 14px;
  }

  .settings-pricing-tools__connection-item {
    padding-right: 14px;
    padding-left: 14px;
  }

  .settings-pricing-tools__connection-content strong {
    font-size: 18px;
  }

  .settings-pricing-tools__primary-actions {
    gap: 20px;
  }

  .settings-pricing-tools__primary-actions ion-button {
    font-size: 14px;
  }

  .settings-pricing-tools__secondary-item {
    padding-right: 12px;
    padding-left: 12px;
  }

  .settings-pricing-tools__secondary-header {
    gap: 9px;
  }

  .settings-pricing-tools__secondary-title-group strong {
    font-size: 17px;
  }

  .settings-pricing-tools__secondary-badge {
    max-width: 46%;
    padding-right: 8px;
    padding-left: 8px;
    font-size: 11px;
  }
}
</style>
