<template>
  <SettingsPageShell
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.entries.pricingTools.0')"
    :hero-eyebrow="$t('settings.groups.integrations')"
    hero-title="PriceLabs"
    content-class="settings-page-block"
    hero-class="settings-page-block__hero"
    eyebrow-class="settings-page-block__eyebrow"
  >
    <SettingsSectionCard
      :title="$t('settingsStage4.pricingTools.sections.integrationStatus')"
      :loading="loading"
      header-class="settings-page-block__section-header"
    >
      <div class="settings-toggle-field">
        <div>
          <strong>{{ $t('stage5SourceText.32') }}</strong>
          <p>{{ $t('stage5DynamicUi.110') }}{{ integrationForm.isEnabled ? $t('channel.managementData.statusActive') : $t('stage5DynamicUi.28') }}</p>
        </div>
        <ion-toggle v-model="integrationForm.isEnabled" />
      </div>

      <div class="settings-form-grid settings-form-grid--top">
        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.pricingTools.columns.priceLabsEmail') }}</span>
          <ion-input v-model="integrationForm.priceLabsEmail" fill="outline" :placeholder="$t('auth.placeholder.email')" />
        </label>
      </div>

      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || savingIntegration" @click="loadPageData">{{ $t('accommodation.common.reset') }}</ion-button>
        <ion-button :disabled="loading || savingIntegration" @click="handleSaveIntegration">
          {{ savingIntegration ? $t('channel.mobile.common.saving') : $t('stage5DynamicUi.23') }}
        </ion-button>
        <ion-button fill="outline" :disabled="loading || syncing" @click="handleManualSync">
          {{ syncing ? $t('channel.mobile.sync.syncing') : $t('stage5DynamicUi.59') }}
        </ion-button>
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard
      :title="$t('stage5UiAttributes.107')"
      header-class="settings-page-block__section-header"
    >
      <template #headerActions>
        <ion-button size="small" @click="handleOpenConnectionEditor">{{ $t('stage5SourceText.130') }}</ion-button>
      </template>

      <div v-if="connections.length > 0" class="mobile-list settings-card-list">
        <article v-for="connection in connections" :key="connection.id" class="settings-card-item">
          <div>
            <strong>{{ connection.roomTypeName }} / {{ connection.pricePlanName }}</strong>
            <p>{{ connection.isEnabled ? $t('channel.managementData.statusActive') : $t('stage5DynamicUi.28') }} · {{ connection.syncStatus }}</p>
            <p v-if="connection.errorMessage">{{ connection.errorMessage }}</p>
          </div>
          <div class="settings-card-item__actions">
            <ion-button size="small" fill="outline" @click="handleToggleConnection(connection)">
              {{ connection.isEnabled ? $t('roomStatus.store.roomState.outOfOrder') : $t('settingsStage4.accountList.status.enabled') }}
            </ion-button>
            <ion-button size="small" color="danger" fill="clear" @click="handleDeleteConnection(connection)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note">{{ $t('stage5SourceText.88') }}</p>
    </SettingsSectionCard>

    <SettingsSectionCard :title="$t('stage5UiAttributes.55')">
      <p v-if="!canManageChannels" class="mobile-note">{{ $t('stage5SourceText.160') }}</p>

      <div v-if="adjustments.length > 0" class="mobile-list settings-card-list">
        <article v-for="adjustment in adjustments" :key="adjustment.channelId" class="settings-card-item">
          <div>
            <strong>{{ adjustment.channelName }}</strong>
            <p>{{ adjustment.adjustmentType }} · {{ adjustment.adjustmentValue ?? '-' }}</p>
            <p>{{ adjustment.autoSyncPrice ? $t('stage5DynamicUi.71') : $t('stage5DynamicUi.70') }}</p>
          </div>
          <div class="settings-card-item__actions">
            <ion-button
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

      <p v-else-if="!loading" class="mobile-note">{{ $t('stage5SourceText.85') }}</p>
    </SettingsSectionCard>

    <SettingsSectionCard :title="$t('settingsStage4.pricingTools.columns.recentSync')">
      <div v-if="logs.length > 0" class="mobile-list settings-card-list">
        <article v-for="log in logs" :key="log.id" class="settings-card-item">
          <div>
            <strong>{{ log.syncTypeDisplay }}</strong>
            <p>{{ log.statusDisplay }} · {{ log.directionDisplay }}</p>
            <p>{{ log.errorMessage || log.createdAt }}</p>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note">{{ $t('stage5SourceText.77') }}</p>
    </SettingsSectionCard>

    <SettingsEditorModal
      :is-open="connectionEditorOpen"
      :title="$t('stage5SourceText.130')"
      @close="handleCloseConnectionEditor"
      @didDismiss="handleCloseConnectionEditor"
    >
      <div class="settings-form-grid">
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
      @close="handleCloseAdjustmentEditor"
      @didDismiss="handleCloseAdjustmentEditor"
    >
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.209') }}</span>
          <ion-select
            v-model="adjustmentForm.adjustmentType"
            fill="outline"
            interface="action-sheet"
            :disabled="!canManageChannels"
          >
            <ion-select-option value="COMMISSION">COMMISSION</ion-select-option>
            <ion-select-option value="FIXED">FIXED</ion-select-option>
            <ion-select-option value="PERCENTAGE">PERCENTAGE</ion-select-option>
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
