<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.channels" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ pageTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page channel-detail-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新渠道详情" refreshing-spinner="crescent" />
      </ion-refresher>

      <div v-if="channelView" class="mobile-stack">
        <section class="mobile-card channel-detail-page__header-card">
          <div class="channel-detail-page__brand">
            <div class="channel-detail-page__logo-wrap">
              <img :src="channelView.logoUrl" :alt="channelView.name" class="channel-detail-page__logo" />
            </div>

            <div class="channel-detail-page__brand-copy">
              <h1 class="channel-detail-page__title">{{ channelView.name }}</h1>
            </div>
          </div>

          <div class="channel-detail-page__meta-row">
            <span class="channel-detail-page__meta-pill" :class="statusToneClass">{{ channelView.statusLabel }}</span>
            <span v-if="channelView.propertyId" class="channel-detail-page__meta-pill">
              标识 {{ channelView.propertyId }}
            </span>
            <span class="channel-detail-page__meta-pill">更新 {{ channelView.lastStatusText }}</span>
          </div>
        </section>

        <section class="mobile-card channel-detail-page__section-card">
          <div class="channel-detail-page__primary-action">
            <div class="channel-detail-page__primary-copy">
              <strong>{{ primaryActionTitle }}</strong>
              <p>{{ primaryActionDescription }}</p>
            </div>
            <div class="channel-detail-page__primary-controls">
              <ion-spinner v-if="loading || actionLoading" name="crescent" />
              <ion-button @click="openConnectEntry">{{ primaryActionButtonText }}</ion-button>
            </div>
          </div>

          <div class="channel-detail-page__entry-list">
            <button
              v-if="showMappingEntry"
              type="button"
              class="channel-detail-page__entry"
              @click="openMappingPage"
            >
              <div class="channel-detail-page__entry-copy">
                <strong>{{ groupLabel }}映射</strong>
                <p>{{ mappingEntryDescription }}</p>
              </div>
              <span class="channel-detail-page__entry-arrow">›</span>
            </button>

            <button
              type="button"
              class="channel-detail-page__entry"
              :disabled="!canManageChannels"
              @click="openPriceEditor"
            >
              <div class="channel-detail-page__entry-copy">
                <strong>价格比例</strong>
                <p>{{ priceEntryDescription }}</p>
              </div>
              <span class="channel-detail-page__entry-arrow">›</span>
            </button>

            <button
              v-if="showSyncEntry"
              type="button"
              class="channel-detail-page__entry"
              @click="openSyncPage"
            >
              <div class="channel-detail-page__entry-copy">
                <strong>同步中心</strong>
                <p>进入同步页执行日历同步或全量刷新。</p>
              </div>
              <span class="channel-detail-page__entry-arrow">›</span>
            </button>

            <button
              v-if="channelCapability.supportsInventorySettings"
              type="button"
              class="channel-detail-page__entry"
              @click="openInventoryPage"
            >
              <div class="channel-detail-page__entry-copy">
                <strong>房量设置</strong>
                <p>管理房量同步与预订设置。</p>
              </div>
              <span class="channel-detail-page__entry-arrow">›</span>
            </button>
          </div>
        </section>

        <section v-if="channelView.isConnected" class="mobile-card channel-detail-page__danger-card">
          <h2 class="mobile-section-title">危险操作</h2>
          <button type="button" class="channel-detail-page__danger-action" @click="handleDisconnect">
            <div class="channel-detail-page__entry-copy">
              <strong>断开连接</strong>
              <p>断开后需要重新授权，现有移动端入口将失效。</p>
            </div>
            <span class="channel-detail-page__danger-text">断开</span>
          </button>
        </section>
      </div>

      <section v-else class="mobile-card">
        <p class="mobile-note">{{ loading ? '渠道详情加载中...' : '未找到该渠道配置。' }}</p>
      </section>
    </ion-content>

    <ChannelConnectModal
      :is-open="connectModalOpen"
      :channel-name="channelView?.name || ''"
      @dismiss="connectModalOpen = false"
      @confirm="handleConfirmConnect"
    />

    <ChannelWidgetModal
      :is-open="widgetModalOpen"
      :ota-id="channelView?.id || 0"
      :ota-name="channelView?.name || ''"
      @dismiss="handleWidgetDismiss"
      @error="handleWidgetError"
    />

    <ChannelPriceAdjustmentSheet
      :is-open="priceSheetOpen"
      :editor="selectedAdjustment"
      :submitting="priceSubmitting"
      @dismiss="priceSheetOpen = false"
      @save="handleSavePriceAdjustment"
    />
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
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChannelConnectModal from '@/components/channel/ChannelConnectModal.vue'
import ChannelPriceAdjustmentSheet from '@/components/channel/ChannelPriceAdjustmentSheet.vue'
import ChannelWidgetModal from '@/components/channel/ChannelWidgetModal.vue'
import {
  buildChannelPriceAdjustmentFromOta,
  buildChannelViewModel,
  buildPriceAdjustmentRequest,
  createPriceAdjustmentEditor,
  createPriceAdjustmentEditorFromOta,
  findChannelPriceAdjustment,
  formatAdjustmentSummary,
  getChannelActionCapability,
  getChannelGroupLabel,
  normalizeChannelCode,
  resolveSuChannelId,
  type PriceAdjustmentEditorValue,
} from '@/components/channel/channelUtils'
import {
  disconnectOta,
  getOtaIntegrationById,
  getSuMappingStatus,
  type OtaIntegrationDTO,
  type SuMappingStatusSummary,
} from '@/api/otaIntegration'
import {
  getChannelPriceAdjustments,
  updateChannelPriceAdjustment,
  type ChannelPriceAdjustmentDTO,
} from '@/api/pricelabs'
import { PermissionAction, PermissionModule } from '@/api/role'
import { ROUTE_PATHS } from '@/router/guards'
import {
  resolveChannelWarningMessage,
  sanitizeChannelWarningMessage,
} from '@/utils/channelMessage'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { hasCurrentStorePermission } from '@/utils/permissions'
import { isHandledRequestError } from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const actionLoading = ref(false)
const priceSubmitting = ref(false)
const canManageChannels = ref(false)
const channel = ref<OtaIntegrationDTO | null>(null)
const mappingStatus = ref<SuMappingStatusSummary | null>(null)
const priceAdjustment = ref<ChannelPriceAdjustmentDTO | null>(null)
const connectModalOpen = ref(false)
const widgetModalOpen = ref(false)
const priceSheetOpen = ref(false)
const selectedAdjustment = ref<PriceAdjustmentEditorValue | null>(null)

const otaId = computed(() => Number(route.params.otaId || 0))

const channelView = computed(() => {
  if (!channel.value) {
    return null
  }
  return buildChannelViewModel(channel.value, mappingStatus.value)
})

const pageTitle = computed(() => {
  if (!channelView.value) {
    return '渠道详情'
  }
  return `${channelView.value.name} · 详情`
})

const resolvedPriceAdjustment = computed(() => {
  if (priceAdjustment.value) {
    return priceAdjustment.value
  }

  return buildChannelPriceAdjustmentFromOta(channel.value)
})

const priceSummary = computed(() => {
  if (!resolvedPriceAdjustment.value) {
    return '暂未配置价格比例'
  }
  return formatAdjustmentSummary(resolvedPriceAdjustment.value)
})

const priceEntryDescription = computed(() => {
  return priceSummary.value
})

const channelCapability = computed(() => getChannelActionCapability(channel.value?.code))
const groupLabel = computed(() => getChannelGroupLabel(channel.value?.code))
const showMappingEntry = computed(() => channelCapability.value.supportsSu)
const showSyncEntry = computed(() => {
  if (!channelView.value) {
    return false
  }
  return channelView.value.isConnected && channelCapability.value.supportsSu
})

const primaryActionButtonText = computed(() => {
  if (!channelView.value) {
    return '管理'
  }
  if (!channelView.value.isConnected) {
    return '授权'
  }
  if (channelView.value.mappingReady) {
    return '管理'
  }
  return '继续'
})

const primaryActionTitle = computed(() => {
  if (!channelView.value) {
    return '整理当前渠道'
  }
  if (!channelView.value.isConnected) {
    return '当前尚未授权'
  }
  return '已授权'
})

const primaryActionDescription = computed(() => {
  if (!channelView.value) {
    return '渠道详情加载中，请稍后再试。'
  }
  if (!channelView.value.isConnected) {
    return '先完成授权，再继续映射、价格比例和同步相关操作。'
  }
  if (channelView.value.mappingReady) {
    return '可继续查看配置、调整价格比例，或重新进入连接向导。'
  }
  return '建议优先检查映射状态，补齐房型与价盘后再继续同步。'
})

const mappingEntryDescription = computed(() => {
  if (!mappingStatus.value) {
    return `查看当前${groupLabel.value}映射状态与分组。`
  }

  if (mappingStatus.value.error) {
    return '当前未映射或映射异常'
  }

  const mappedRoomCount = mappingStatus.value.mappedRoomIdCount || 0
  const activeRatePlanCount = mappingStatus.value.activeRatePlanCount || 0
  if (mappedRoomCount === 0 && activeRatePlanCount === 0) {
    return '当前未映射'
  }

  return `房型 ${mappedRoomCount} / 价盘 ${activeRatePlanCount}`
})

const statusToneClass = computed(() => {
  if (!channelView.value) {
    return ''
  }
  if (channelView.value.mappingReady) {
    return 'channel-detail-page__meta-pill--ready'
  }
  if (channelView.value.isConnected) {
    return 'channel-detail-page__meta-pill--partial'
  }
  return 'channel-detail-page__meta-pill--idle'
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  return resolveChannelWarningMessage(error, fallbackMessage)
}

async function loadDetail() {
  if (!otaId.value) {
    return
  }

  loading.value = true

  try {
    const detailResponse = await getOtaIntegrationById(otaId.value)
    if (!detailResponse.success || !detailResponse.data) {
      throw new Error(detailResponse.message || '加载渠道详情失败')
    }

    channel.value = detailResponse.data
    const suChannelId = resolveSuChannelId(detailResponse.data.code)
    priceAdjustment.value = buildChannelPriceAdjustmentFromOta(detailResponse.data)

    const [priceResult, mappingResult] = await Promise.allSettled([
      getChannelPriceAdjustments(),
      suChannelId ? getSuMappingStatus(otaId.value, suChannelId) : Promise.resolve(null),
    ] as const)

    if (priceResult && priceResult.status === 'fulfilled' && priceResult.value.success) {
      const currentChannelCode = normalizeChannelCode(detailResponse.data.code)
      const nextAdjustment = findChannelPriceAdjustment(priceResult.value.data, {
        code: currentChannelCode,
        name: detailResponse.data.name,
      })
      if (nextAdjustment) {
        priceAdjustment.value = nextAdjustment
      }
    }

    if (mappingResult.status === 'fulfilled' && mappingResult.value && mappingResult.value.success) {
      mappingStatus.value = mappingResult.value.data || null
    } else {
      mappingStatus.value = null
    }
  } finally {
    loading.value = false
  }
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

async function loadPageData() {
  await Promise.all([loadDetail(), loadManageChannelsPermission()])
}

function openConnectEntry() {
  if (!channelView.value) {
    return
  }

  if (channelView.value.isConnected) {
    widgetModalOpen.value = true
    return
  }

  connectModalOpen.value = true
}

function handleConfirmConnect() {
  connectModalOpen.value = false
  widgetModalOpen.value = true
}

function handleWidgetDismiss() {
  widgetModalOpen.value = false
  void loadDetail()
}

function handleWidgetError(message: string) {
  showWarningToast(sanitizeChannelWarningMessage(message, '渠道操作异常，请稍后重试'))
}

function openPriceEditor() {
  if (!canManageChannels.value) {
    showWarningToast('您没有权限编辑价格比例')
    return
  }

  if (!channel.value) {
    return
  }

  if (resolvedPriceAdjustment.value) {
    selectedAdjustment.value = createPriceAdjustmentEditor(resolvedPriceAdjustment.value)
  } else {
    selectedAdjustment.value = createPriceAdjustmentEditorFromOta(channel.value)
  }

  if (!selectedAdjustment.value) {
    showWarningToast('价格比例信息加载中，请稍后重试')
    return
  }

  priceSheetOpen.value = true
}

async function handleSavePriceAdjustment(value: PriceAdjustmentEditorValue) {
  if (!canManageChannels.value) {
    showWarningToast('您没有权限保存价格比例')
    return
  }

  priceSubmitting.value = true

  try {
    const response = await updateChannelPriceAdjustment(
      value.channelId,
      buildPriceAdjustmentRequest(value),
    )
    if (!response.success) {
      throw new Error(response.message || '更新价格比例失败')
    }

    if (response.data) {
      priceAdjustment.value = response.data
    }

    showSuccessToast('价格比例已更新')
    priceSheetOpen.value = false
    await loadDetail()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '更新价格比例失败'))
    }
  } finally {
    priceSubmitting.value = false
  }
}

async function handleDisconnect() {
  if (!channelView.value) {
    return
  }

  const alert = await alertController.create({
    header: '断开连接',
    message: `确认断开 ${channelView.value.name} 吗？断开后需要重新授权。`,
    buttons: [
      {
        text: '取消',
        role: 'cancel',
      },
      {
        text: '确认断开',
        role: 'destructive',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  if (result.role !== 'destructive') {
    return
  }

  actionLoading.value = true

  try {
    const response = await disconnectOta(channelView.value.id)
    if (!response.success) {
      throw new Error(response.message || '断开连接失败')
    }

    showSuccessToast('渠道已断开连接')
    await loadDetail()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '断开连接失败'))
    }
  } finally {
    actionLoading.value = false
  }
}

async function openMappingPage() {
  await router.push({
    name: 'ChannelMapping',
    params: { otaId: otaId.value },
  })
}

async function openSyncPage() {
  await router.push({
    name: 'ChannelSync',
    params: { otaId: otaId.value },
  })
}

async function openInventoryPage() {
  await router.push({
    name: 'ChannelInventory',
    params: { otaId: otaId.value },
  })
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadPageData()
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.channel-detail-page {
  --background: var(--ios-pms-bg-page-plain);
}

.channel-detail-page__header-card,
.channel-detail-page__section-card,
.channel-detail-page__danger-card {
  padding: 18px;
}

.channel-detail-page__brand {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 14px;
}

.channel-detail-page__logo-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  border-radius: 20px;
  border: 1px solid var(--app-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.92));
  overflow: hidden;
}

.channel-detail-page__logo {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

.channel-detail-page__brand-copy {
  min-width: 0;
}

.channel-detail-page__title {
  margin: 0;
  color: var(--app-heading);
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.channel-detail-page__meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.channel-detail-page__meta-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(115, 164, 255, 0.06);
  color: var(--ios-pms-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.channel-detail-page__meta-pill--ready {
  background: rgba(23, 166, 115, 0.12);
  color: #17815e;
}

.channel-detail-page__meta-pill--partial {
  background: rgba(255, 186, 56, 0.16);
  color: #996515;
}

.channel-detail-page__meta-pill--idle {
  background: rgba(115, 130, 157, 0.12);
  color: #6c7992;
}

.channel-detail-page__primary-action {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 16px 0;
}

.channel-detail-page__primary-controls {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.channel-detail-page__primary-copy strong,
.channel-detail-page__entry-copy strong {
  display: block;
  margin: 0;
  color: var(--app-heading);
  font-size: 16px;
  font-weight: 700;
}

.channel-detail-page__primary-copy p,
.channel-detail-page__entry-copy p {
  margin: 6px 0 0;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.55;
}

.channel-detail-page__entry-list {
  display: grid;
}

.channel-detail-page__entry,
.channel-detail-page__danger-action {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
}

.channel-detail-page__entry-list .channel-detail-page__entry:first-child,
.channel-detail-page__danger-action {
  border-top: 1px solid var(--app-border);
}

.channel-detail-page__entry + .channel-detail-page__entry {
  border-top: 1px solid var(--app-border);
}

.channel-detail-page__entry:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.channel-detail-page__entry-copy {
  min-width: 0;
}

.channel-detail-page__entry-arrow {
  color: var(--ios-pms-text-disabled);
  font-size: 18px;
}

.channel-detail-page__danger-card {
  background: rgba(255, 255, 255, 0.94);
}

.channel-detail-page__danger-text {
  color: var(--ion-color-danger);
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 360px) {
  .channel-detail-page__primary-action {
    grid-template-columns: minmax(0, 1fr);
  }

  .channel-detail-page__primary-controls {
    justify-content: flex-start;
  }
}
</style>
