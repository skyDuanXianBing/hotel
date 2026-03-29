<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>定价工具</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page-block">
      <section class="mobile-hero settings-page-block__hero">
        <p class="mobile-note settings-page-block__eyebrow">第三方集成</p>
        <h1 class="mobile-title">PriceLabs</h1>
        <p class="mobile-subtitle">展示集成状态、连接关系、渠道价差与最近同步记录。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-page-block__section-header">
            <div>
              <h2 class="mobile-section-title">集成状态</h2>
              <p class="mobile-note">邮箱与启停状态可直接在移动端维护。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

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
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row settings-page-block__section-header">
            <div>
              <h2 class="mobile-section-title">连接关系</h2>
              <p class="mobile-note">房型与价格计划一一连接到 PriceLabs listing。</p>
            </div>
            <ion-button size="small" @click="handleOpenConnectionEditor">新增连接</ion-button>
          </div>

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
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">渠道价差</h2>
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
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">最近同步</h2>
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
        </section>
      </div>

      <ion-modal :is-open="connectionEditorOpen" @didDismiss="handleCloseConnectionEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>新增连接</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseConnectionEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
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

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleCloseConnectionEditor">取消</ion-button>
              <ion-button :disabled="submittingConnection" @click="handleSaveConnection">
                {{ submittingConnection ? '提交中...' : '保存连接' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="adjustmentEditorOpen" @didDismiss="handleCloseAdjustmentEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>编辑渠道价差</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseAdjustmentEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
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
                  <p>启用后会自动应用该渠道价差。</p>
                </div>
                <ion-toggle v-model="adjustmentForm.autoSyncPrice" />
              </div>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleCloseAdjustmentEditor">取消</ion-button>
              <ion-button :disabled="submittingAdjustment" @click="handleSaveAdjustment">
                {{ submittingAdjustment ? '提交中...' : '保存价差' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonModal,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToggle,
  IonToolbar,
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

.settings-toggle-field strong,
.settings-toggle-field p {
  margin: 0;
}

.settings-toggle-field p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-form-grid {
  display: grid;
  gap: 14px;
}

.settings-form-grid--top {
  margin-top: 12px;
}

.settings-form-field {
  display: grid;
  gap: 8px;
}

.settings-form-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.settings-card-list {
  margin-top: 16px;
}

.settings-card-item {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.settings-card-item strong,
.settings-card-item p {
  margin: 0;
}

.settings-card-item p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.settings-card-item__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.settings-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.settings-form-actions--section {
  margin-top: 18px;
}
</style>
