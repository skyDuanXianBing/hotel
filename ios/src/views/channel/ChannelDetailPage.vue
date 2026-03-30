<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.channels" />
        </ion-buttons>
        <ion-title>{{ pageTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page channel-detail-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新渠道详情" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="channelView" class="mobile-hero channel-detail-hero">
        <p class="mobile-note channel-detail-hero__eyebrow">渠道详情</p>
        <h1 class="mobile-title">{{ channelView.name }}</h1>
        <p class="mobile-subtitle">{{ channelView.actionHint }}</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ channelView.statusLabel }}</span>
          <span class="mobile-chip">{{ channelView.code }}</span>
          <span class="mobile-chip">{{ channelView.lastStatusText }}</span>
        </div>
      </section>

      <div v-if="channelView" class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">关键操作</h2>
              <p class="mobile-note">授权 / 重连、映射查看、价格比例编辑都集中在这里。</p>
            </div>
            <ion-spinner v-if="loading || actionLoading" name="crescent" />
          </div>

          <div class="channel-detail-page__actions">
            <ion-button @click="openConnectEntry">{{ channelView.actionLabel }}</ion-button>
            <ion-button fill="outline" @click="openMappingPage">查看映射</ion-button>
            <ion-button fill="outline" :disabled="!channelView" @click="openPriceEditor">
              价格比例
            </ion-button>
            <ion-button v-if="channelView.isConnected" color="danger" fill="outline" @click="handleDisconnect">
              断开连接
            </ion-button>
          </div>
        </section>

        <section class="mobile-card">
          <ion-segment :value="activeSegment" @ionChange="handleSegmentChange">
            <ion-segment-button value="overview">
              <ion-label>概览</ion-label>
            </ion-segment-button>
            <ion-segment-button value="settings">
              <ion-label>设置</ion-label>
            </ion-segment-button>
            <ion-segment-button value="notes">
              <ion-label>说明</ion-label>
            </ion-segment-button>
          </ion-segment>
        </section>

        <section v-if="activeSegment === 'overview'" class="mobile-card mobile-list channel-detail-page__summary-list">
          <article class="channel-detail-page__summary-item">
            <strong>连接状态</strong>
            <p>{{ channelView.statusDescription }}</p>
          </article>

          <article class="channel-detail-page__summary-item">
            <strong>渠道属性标识</strong>
            <p>{{ channelView.propertyId || '暂未回填' }}</p>
          </article>

          <article class="channel-detail-page__summary-item">
            <strong>映射摘要</strong>
            <p>
              房型 {{ mappingStatus?.mappedRoomIdCount || 0 }} / 价盘
              {{ mappingStatus?.activeRatePlanCount || 0 }}
            </p>
            <p v-if="mappingStatusNotice">{{ mappingStatusNotice }}</p>
          </article>

          <article class="channel-detail-page__summary-item">
            <strong>价格比例</strong>
            <p>{{ priceSummary }}</p>
          </article>
        </section>

        <section v-else-if="activeSegment === 'settings'" class="mobile-card mobile-list channel-detail-page__summary-list">
          <article class="channel-detail-page__summary-item">
            <strong>同步与刷新</strong>
            <p>已授权且支持同步的渠道可执行日历同步与全量刷新，阶段失败会逐条提示。</p>
            <div class="channel-detail-page__inline-actions">
              <ion-button size="small" :disabled="!canOpenSyncAction" @click="openSyncAction('calendar')">
                日历同步
              </ion-button>
              <ion-button
                size="small"
                fill="outline"
                :disabled="!canOpenSyncAction"
                @click="openSyncAction('full-refresh')"
              >
                全量刷新
              </ion-button>
            </div>
            <p v-if="syncActionBlockedMessage">{{ syncActionBlockedMessage }}</p>
          </article>

          <article class="channel-detail-page__summary-item">
            <strong>{{ groupLabel }}映射管理</strong>
            <p>移动端已补齐 {{ groupLabel }} 管理层，并支持通过深编辑草稿整理 PMS 房型与价盘选择。</p>
            <div class="channel-detail-page__inline-actions">
              <ion-button size="small" fill="outline" @click="openMappingPage">
                进入{{ groupLabel }}管理
              </ion-button>
            </div>
          </article>

          <article v-if="channelCapability.supportsInventorySettings" class="channel-detail-page__summary-item">
            <strong>Airbnb 房量设置</strong>
            <p>已提供房量同步与预订设置草稿页；若后端未开放保存接口，会明确提示当前为降级模式。</p>
            <div class="channel-detail-page__inline-actions">
              <ion-button size="small" fill="outline" @click="openInventoryPage">进入房量设置</ion-button>
            </div>
          </article>

          <article class="channel-detail-page__summary-item">
            <strong>连接历史</strong>
            <p>当前后端尚未提供移动端连接历史接口，首版仅保留说明位，不做静默隐藏。</p>
          </article>
        </section>

        <section v-else class="mobile-card">
          <h2 class="mobile-section-title">首版说明</h2>
          <ul class="mobile-bullet-list">
            <li>已保留授权 / 重连、Su 映射明细查看与价格比例更新主链路。</li>
            <li>已新增同步中心、Airbnb 房量设置承载页与映射管理层。</li>
            <li>若渠道状态长时间未更新，可重新进入授权向导以刷新连接状态。</li>
            <li>对当前后端未开放的保存能力，移动端会给出明确降级提示。</li>
          </ul>
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
  IonLabel,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSegment,
  IonSegmentButton,
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
import { ROUTE_PATHS } from '@/router/guards'
import {
  resolveChannelWarningMessage,
  resolveMappingStatusNotice,
  sanitizeChannelWarningMessage,
} from '@/utils/channelMessage'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

type DetailSegment = 'overview' | 'settings' | 'notes'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const actionLoading = ref(false)
const priceSubmitting = ref(false)
const channel = ref<OtaIntegrationDTO | null>(null)
const mappingStatus = ref<SuMappingStatusSummary | null>(null)
const mappingStatusNotice = ref('')
const priceAdjustment = ref<ChannelPriceAdjustmentDTO | null>(null)
const activeSegment = ref<DetailSegment>('overview')
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
const channelCapability = computed(() => getChannelActionCapability(channel.value?.code))
const groupLabel = computed(() => getChannelGroupLabel(channel.value?.code))
const canOpenSyncAction = computed(() => {
  if (!channelView.value) {
    return false
  }

  return channelView.value.isConnected && channelCapability.value.supportsSu
})
const syncActionBlockedMessage = computed(() => {
  if (!channelView.value) {
    return '渠道详情加载中，请稍后再试。'
  }
  if (!channelView.value.isConnected) {
    return '请先完成授权或重连，再执行同步动作。'
  }
  if (!channelCapability.value.supportsSu) {
    return '当前渠道没有可用同步接口。'
  }
  return ''
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  return resolveChannelWarningMessage(error, fallbackMessage)
}

function syncMappingStatusNotice() {
  mappingStatusNotice.value = resolveMappingStatusNotice(mappingStatus.value?.error)
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

    syncMappingStatusNotice()
  } finally {
    loading.value = false
  }
}

function handleSegmentChange(event: CustomEvent) {
  const nextValue = event.detail.value as DetailSegment
  activeSegment.value = nextValue || 'overview'
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

async function openSyncAction(action: 'calendar' | 'full-refresh') {
  if (!canOpenSyncAction.value) {
    showWarningToast(syncActionBlockedMessage.value || '当前无法执行同步动作')
    return
  }

  await router.push({
    name: 'ChannelSync',
    params: { otaId: otaId.value },
    query: { action },
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
    await loadDetail()
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  await loadDetail()
})
</script>

<style scoped>
.channel-detail-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.channel-detail-page__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.channel-detail-page__summary-list {
  gap: 12px;
}

.channel-detail-page__summary-item {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--app-border);
}

.channel-detail-page__summary-item strong,
.channel-detail-page__summary-item p {
  margin: 0;
}

.channel-detail-page__summary-item p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.channel-detail-page__inline-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
</style>
