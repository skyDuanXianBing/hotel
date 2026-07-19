<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar channel-detail-page__toolbar">
        <ion-buttons slot="start">
          <ion-back-button
            class="app-page-header__back-btn channel-detail-page__back-btn"
            :default-href="ROUTE_PATHS.channels"
          />
        </ion-buttons>
        <ion-title class="app-page-header__title channel-detail-page__toolbar-title">
          {{ pageTitle }}
        </ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page channel-detail-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新渠道详情" refreshing-spinner="crescent" />
      </ion-refresher>

      <div v-if="channelView" class="mobile-stack channel-detail-page__stack">
        <section class="mobile-card channel-detail-page__header-card">
          <div class="channel-detail-page__brand">
            <div class="channel-detail-page__logo-wrap">
              <img :src="channelView.logoUrl" :alt="channelView.name" class="channel-detail-page__logo" />
            </div>

            <div class="channel-detail-page__brand-copy">
              <h1 class="channel-detail-page__title">{{ channelView.name }}</h1>
              <div class="channel-detail-page__meta-row">
                <span class="channel-detail-page__meta-pill" :class="statusToneClass">
                  {{ channelView.statusLabel }}
                </span>
                <span class="channel-detail-page__meta-pill">更新 {{ channelView.lastStatusText }}</span>
              </div>
            </div>
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
.channel-detail-page__toolbar {
  --background: #ffffff;
  --min-height: 54px;
}

.channel-detail-page__back-btn {
  --color: var(--ios-pms-header-control-color);
}

.channel-detail-page__toolbar-title {
  color: var(--ios-pms-header-title-color);
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
}

.channel-detail-page {
  --background: #f3f7fd;
  --padding-top: 8px;
  --padding-start: 14px;
  --padding-end: 14px;
  --padding-bottom: calc(28px + var(--app-safe-bottom));
}

.channel-detail-page__stack {
  gap: 10px;
}

.channel-detail-page__header-card,
.channel-detail-page__section-card,
.channel-detail-page__danger-card {
  margin: 0;
  border: 1px solid rgba(217, 226, 239, 0.28);
  border-radius: 6px;
  background: #ffffff;
  box-shadow: none;
}

.channel-detail-page__header-card {
  --channel-detail-logo-width: clamp(100px, 29vw, 112px);
  padding: 14px 15px;
}

.channel-detail-page__section-card {
  padding: 0 15px;
}

.channel-detail-page__danger-card {
  padding: 16px 15px;
}

.channel-detail-page__brand {
  display: grid;
  grid-template-columns: var(--channel-detail-logo-width) minmax(0, 1fr);
  align-items: center;
  gap: clamp(24px, 13vw, 50px);
}

.channel-detail-page__logo-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--channel-detail-logo-width);
  aspect-ratio: 3 / 2;
  border: 1px solid #d9d9d9;
  border-radius: 5px;
  background: #ffffff;
  overflow: hidden;
}

.channel-detail-page__logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.channel-detail-page__brand-copy {
  min-width: 0;
}

.channel-detail-page__title {
  margin: 0;
  color: #303030;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0;
  line-height: 1.2;
}

.channel-detail-page__meta-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
  margin-top: 6px;
}

.channel-detail-page__meta-pill {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  max-width: 100%;
  padding: 0 8px;
  border-radius: 4px;
  background: #f1f1f1;
  color: #626262;
  font-size: 12px;
  font-weight: 400;
  line-height: 20px;
  white-space: nowrap;
}

.channel-detail-page__meta-pill:last-child {
  min-height: 24px;
  line-height: 24px;
}

.channel-detail-page__meta-pill--ready {
  background: #eaf9f2;
  color: #44b686;
}

.channel-detail-page__meta-pill--partial {
  background: #fff3d8;
  color: #a77119;
}

.channel-detail-page__meta-pill--idle {
  background: #eeeeee;
  color: #777777;
}

.channel-detail-page__primary-action {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
  min-height: 88px;
  padding: 15px 0 13px;
}

.channel-detail-page__primary-controls {
  display: inline-flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 8px;
}

.channel-detail-page__primary-controls ion-spinner {
  width: 20px;
  height: 20px;
  margin-top: 6px;
}

.channel-detail-page__primary-controls ion-button {
  height: 32px;
  min-height: 32px;
  margin: 0;
  --background: #3478f6;
  --border-radius: 8px;
  --box-shadow: none;
  --padding-start: 15px;
  --padding-end: 15px;
  font-size: 15px;
  font-weight: 400;
  letter-spacing: 0;
}

.channel-detail-page__primary-copy strong,
.channel-detail-page__entry-copy strong {
  display: block;
  margin: 0;
  color: #303030;
  font-size: 17px;
  letter-spacing: 0;
  line-height: 1.25;
}

.channel-detail-page__primary-copy strong {
  font-weight: 600;
}

.channel-detail-page__entry-copy strong {
  font-weight: 550;
}

.channel-detail-page__primary-copy p,
.channel-detail-page__entry-copy p {
  margin: 4px 0 0;
  color: #8b8b8b;
  font-size: 14px;
  line-height: 1.45;
  letter-spacing: 0;
}

.channel-detail-page__primary-copy {
  min-width: 0;
}

.channel-detail-page__primary-copy p {
  max-width: 240px;
}

.channel-detail-page__entry-list {
  display: grid;
}

.channel-detail-page__entry,
.channel-detail-page__danger-action {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 75px;
  padding: 13px 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
}

.channel-detail-page__entry-list .channel-detail-page__entry:first-child,
.channel-detail-page__danger-action {
  border-top: 1px solid #dddddd;
}

.channel-detail-page__entry + .channel-detail-page__entry {
  border-top: 1px solid #dddddd;
}

.channel-detail-page__entry:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.channel-detail-page__entry-copy {
  min-width: 0;
}

.channel-detail-page__entry-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: #858585;
  font-size: 34px;
  font-weight: 300;
  line-height: 1;
}

.channel-detail-page__danger-card {
  background: #ffffff;
}

.channel-detail-page__danger-card .mobile-section-title {
  margin-bottom: 4px;
  color: #303030;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0;
}

.channel-detail-page__danger-text {
  color: var(--ion-color-danger);
  font-size: 14px;
  font-weight: 400;
  text-align: right;
}

@media (max-width: 360px) {
  .channel-detail-page {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .channel-detail-page__header-card {
    --channel-detail-logo-width: 96px;
    padding-right: 12px;
    padding-left: 12px;
  }

  .channel-detail-page__brand {
    gap: 20px;
  }

  .channel-detail-page__title {
    font-size: 20px;
  }

  .channel-detail-page__section-card,
  .channel-detail-page__danger-card {
    padding-right: 12px;
    padding-left: 12px;
  }

  .channel-detail-page__primary-action {
    gap: 8px;
  }

  .channel-detail-page__primary-copy strong,
  .channel-detail-page__entry-copy strong {
    font-size: 16px;
  }

  .channel-detail-page__primary-copy p,
  .channel-detail-page__entry-copy p {
    font-size: 13px;
  }
}
</style>
