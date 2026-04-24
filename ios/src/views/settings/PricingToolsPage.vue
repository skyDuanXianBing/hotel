<template>
  <SettingsPageShell
    :back-href="ROUTE_PATHS.settings"
    title="定价工具"
    hero-eyebrow="第三方集成"
    hero-title="PriceLabs"
    content-class="settings-page-block"
    hero-class="settings-page-block__hero"
    eyebrow-class="settings-page-block__eyebrow"
  >
    <SettingsSectionCard
      title="集成状态"
      :loading="loading"
      header-class="settings-page-block__section-header"
    >
      <div class="settings-toggle-field">
        <div>
          <strong>启用 PriceLabs</strong>
          <p>当前状态：{{ integrationForm.isEnabled ? '已启用' : '已停用' }}</p>
        </div>
        <ion-toggle v-model="integrationForm.isEnabled" />
      </div>

      <div class="settings-form-grid settings-form-grid--top">
        <label class="settings-form-field">
          <span>PriceLabs 邮箱</span>
          <ion-input v-model="integrationForm.priceLabsEmail" fill="outline" placeholder="请输入邮箱" />
        </label>
      </div>

      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || savingIntegration" @click="loadPageData">重置</ion-button>
        <ion-button :disabled="loading || savingIntegration" @click="handleSaveIntegration">
          {{ savingIntegration ? '保存中...' : '保存集成设置' }}
        </ion-button>
        <ion-button fill="outline" :disabled="loading || syncing" @click="handleManualSync">
          {{ syncing ? '同步中...' : '立即同步' }}
        </ion-button>
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard
      title="连接关系"
      header-class="settings-page-block__section-header"
    >
      <template #headerActions>
        <ion-button size="small" @click="handleOpenConnectionEditor">新增连接</ion-button>
      </template>

      <div v-if="connections.length > 0" class="mobile-list settings-card-list">
        <article v-for="connection in connections" :key="connection.id" class="settings-card-item">
          <div>
            <strong>{{ connection.roomTypeName }} / {{ connection.pricePlanName }}</strong>
            <p>{{ connection.isEnabled ? '已启用' : '已停用' }} · {{ connection.syncStatus }}</p>
            <p v-if="connection.errorMessage">{{ connection.errorMessage }}</p>
          </div>
          <div class="settings-card-item__actions">
            <ion-button size="small" fill="outline" @click="handleToggleConnection(connection)">
              {{ connection.isEnabled ? '停用' : '启用' }}
            </ion-button>
            <ion-button size="small" color="danger" fill="clear" @click="handleDeleteConnection(connection)">删除</ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note">当前暂无连接关系。</p>
    </SettingsSectionCard>

    <SettingsSectionCard title="渠道价差">
      <div v-if="adjustments.length > 0" class="mobile-list settings-card-list">
        <article v-for="adjustment in adjustments" :key="adjustment.channelId" class="settings-card-item">
          <div>
            <strong>{{ adjustment.channelName }}</strong>
            <p>{{ adjustment.adjustmentType }} · {{ adjustment.adjustmentValue ?? '-' }}</p>
            <p>{{ adjustment.autoSyncPrice ? '自动同步已开' : '自动同步已关' }}</p>
          </div>
          <div class="settings-card-item__actions">
            <ion-button size="small" fill="outline" @click="handleEditAdjustment(adjustment)">编辑</ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note">当前暂无渠道价差配置。</p>
    </SettingsSectionCard>

    <SettingsSectionCard title="最近同步">
      <div v-if="logs.length > 0" class="mobile-list settings-card-list">
        <article v-for="log in logs" :key="log.id" class="settings-card-item">
          <div>
            <strong>{{ log.syncTypeDisplay }}</strong>
            <p>{{ log.statusDisplay }} · {{ log.directionDisplay }}</p>
            <p>{{ log.errorMessage || log.createdAt }}</p>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note">当前暂无同步记录。</p>
    </SettingsSectionCard>

    <SettingsEditorModal
      :is-open="connectionEditorOpen"
      title="新增连接"
      @close="handleCloseConnectionEditor"
      @didDismiss="handleCloseConnectionEditor"
    >
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>房型</span>
          <ion-select v-model="connectionForm.roomTypeId" fill="outline" interface="modal">
            <ion-select-option v-for="roomType in roomTypes" :key="roomType.id" :value="roomType.id">
              {{ roomType.name }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field">
          <span>价格计划</span>
          <ion-select v-model="connectionForm.pricePlanId" fill="outline" interface="modal">
            <ion-select-option v-for="plan in pricePlans" :key="plan.id" :value="plan.id">
              {{ plan.name }}
            </ion-select-option>
          </ion-select>
        </label>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleCloseConnectionEditor">取消</ion-button>
        <ion-button :disabled="submittingConnection" @click="handleSaveConnection">
          {{ submittingConnection ? '提交中...' : '保存连接' }}
        </ion-button>
      </template>
    </SettingsEditorModal>

    <SettingsEditorModal
      :is-open="adjustmentEditorOpen"
      title="编辑渠道价差"
      @close="handleCloseAdjustmentEditor"
      @didDismiss="handleCloseAdjustmentEditor"
    >
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>调整方式</span>
          <ion-select v-model="adjustmentForm.adjustmentType" fill="outline" interface="action-sheet">
            <ion-select-option value="COMMISSION">COMMISSION</ion-select-option>
            <ion-select-option value="FIXED">FIXED</ion-select-option>
            <ion-select-option value="PERCENTAGE">PERCENTAGE</ion-select-option>
          </ion-select>
        </label>
        <label class="settings-form-field">
          <span>调整值</span>
          <ion-input v-model="adjustmentForm.adjustmentValue" fill="outline" inputmode="decimal" placeholder="请输入调整值" />
        </label>
        <div class="settings-toggle-field">
          <div>
            <strong>自动同步价格</strong>
          </div>
          <ion-toggle v-model="adjustmentForm.autoSyncPrice" />
        </div>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleCloseAdjustmentEditor">取消</ion-button>
        <ion-button :disabled="submittingAdjustment" @click="handleSaveAdjustment">
          {{ submittingAdjustment ? '提交中...' : '保存价差' }}
        </ion-button>
      </template>
    </SettingsEditorModal>
  </SettingsPageShell>
</template>

<script setup lang="ts">
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
import { getAllPricePlans, type PricePlanDTO } from '@/api/pricePlan'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import SettingsEditorModal from '@/components/settings/base/SettingsEditorModal.vue'
import SettingsPageShell from '@/components/settings/base/SettingsPageShell.vue'
import SettingsSectionCard from '@/components/settings/base/SettingsSectionCard.vue'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { normalizeOptionalNumber } from '@/utils/settings'

const userStore = useUserStore()

const loading = ref(false)
const savingIntegration = ref(false)
const syncing = ref(false)
const submittingConnection = ref(false)
const submittingAdjustment = ref(false)
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

async function confirmDelete(name: string, title: string) {
  const alert = await alertController.create({
    header: title,
    message: `确认删除 ${name} 吗？`,
    buttons: [
      { text: '取消', role: 'cancel' },
      { text: '确认删除', role: 'destructive' },
    ],
  })
  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadPageData() {
  const userId = userStore.currentUser?.id
  if (!userId) {
    showWarningToast('请先恢复当前用户信息')
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
      showWarningToast(resolveWarningMessage(error, '加载定价工具失败'))
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
      throw new Error(response.message || '保存集成设置失败')
    }
    showSuccessToast('集成设置已保存')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存集成设置失败'))
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
      throw new Error(response.message || '触发同步失败')
    }
    showSuccessToast('同步任务已触发')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '触发同步失败'))
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
    showWarningToast('请选择房型和价格计划')
    return
  }

  submittingConnection.value = true
  try {
    const response = await createConnection(connectionForm.value.roomTypeId, connectionForm.value.pricePlanId)
    if (!response.success) {
      throw new Error(response.message || '创建连接失败')
    }
    showSuccessToast('连接已创建')
    handleCloseConnectionEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '创建连接失败'))
    }
  } finally {
    submittingConnection.value = false
  }
}

async function handleToggleConnection(connection: PriceLabsConnectionDTO) {
  try {
    const response = await updateConnectionStatus(connection.id, !connection.isEnabled)
    if (!response.success) {
      throw new Error(response.message || '更新连接状态失败')
    }
    showSuccessToast(response.data.isEnabled ? '连接已启用' : '连接已停用')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '更新连接状态失败'))
    }
  }
}

async function handleDeleteConnection(connection: PriceLabsConnectionDTO) {
  const confirmed = await confirmDelete(`${connection.roomTypeName} / ${connection.pricePlanName}`, '删除连接')
  if (!confirmed) {
    return
  }
  try {
    const response = await deleteConnection(connection.id)
    if (!response.success) {
      throw new Error(response.message || '删除连接失败')
    }
    showSuccessToast('连接已删除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除连接失败'))
    }
  }
}

function handleEditAdjustment(adjustment: ChannelPriceAdjustmentDTO) {
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
  if (!selectedAdjustmentChannelId.value) {
    return
  }

  const adjustmentValue = normalizeOptionalNumber(adjustmentForm.value.adjustmentValue)
  if (adjustmentValue === null) {
    showWarningToast('请输入有效的调整值')
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
      throw new Error(response.message || '保存价差失败')
    }
    showSuccessToast('渠道价差已保存')
    handleCloseAdjustmentEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存价差失败'))
    }
  } finally {
    submittingAdjustment.value = false
  }
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>
