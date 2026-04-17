<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title class="mobile-toolbar-title">渠道</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="mobile-page channels-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新渠道状态" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero channels-hero">
        <p class="mobile-note channels-hero__eyebrow">渠道中心</p>
        <h1 class="mobile-title">{{ storeName }}</h1>
        <p class="mobile-subtitle">集中查看 OTA 状态、映射进度、价格比例，以及授权与重连入口。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">已设置 {{ readyCount }}</span>
          <span class="mobile-chip">待补齐 {{ incompleteCount }}</span>
          <span class="mobile-chip">价格规则 {{ priceAdjustments.length }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <ion-segment class="channels-page__segment" :value="activeSegment" @ionChange="handleSegmentChange">
            <ion-segment-button value="overview" @click="handleSegmentButtonClick('overview')">
              <span class="channels-page__segment-text">渠道总览</span>
            </ion-segment-button>
            <ion-segment-button value="pricing" @click="handleSegmentButtonClick('pricing')">
              <span class="channels-page__segment-text">价格比例</span>
            </ion-segment-button>
          </ion-segment>
          <p v-if="loadNotice" class="mobile-note channels-page__notice">{{ loadNotice }}</p>
        </section>

        <section v-if="activeSegment === 'overview'" class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">OTA 列表</h2>
              <p class="mobile-note">高频渠道固定在前部；未授权渠道先看说明，已授权渠道可继续映射或重连。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="channels.length > 0" class="mobile-list channels-page__card-list">
            <ChannelOverviewCard
              v-for="item in channels"
              :key="item.id"
              :item="item"
              @open-detail="openChannelDetail(item.id)"
              @open-connect="openConnectEntry(item)"
            />
          </div>

          <p v-else-if="!loading" class="mobile-note channels-page__empty">当前门店暂无可用渠道配置。</p>
        </section>

        <section v-else class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">价格比例</h2>
              <p class="mobile-note">沿用 Web 正负值语义：正值更贵，负值更便宜。</p>
            </div>
            <ion-spinner v-if="priceLoading" name="crescent" />
          </div>

          <ion-list inset v-if="priceAdjustments.length > 0" class="channels-page__price-list">
            <ion-item v-for="item in priceAdjustments" :key="item.channelId">
              <ion-label>
                <h3>{{ item.channelName }}</h3>
                <p>{{ formatAdjustmentSummary(item) }}</p>
                <p>自动同步：{{ item.autoSyncPrice ? '开启' : '关闭' }}</p>
              </ion-label>
              <ion-button slot="end" fill="outline" size="small" @click="openPriceEditor(item)">
                编辑
              </ion-button>
            </ion-item>
          </ion-list>

          <p v-else-if="!priceLoading" class="mobile-note channels-page__empty">暂无渠道价格比例数据。</p>
        </section>
      </div>
    </ion-content>

    <ChannelConnectModal
      :is-open="connectModalOpen"
      :channel-name="selectedChannel?.name || ''"
      @dismiss="connectModalOpen = false"
      @confirm="handleConfirmConnect"
    />

    <ChannelWidgetModal
      :is-open="widgetModalOpen"
      :ota-id="selectedChannel?.id || 0"
      :ota-name="selectedChannel?.name || ''"
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
  IonButton,
  IonContent,
  IonHeader,
  IonItem,
  IonList,
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
import { useRouter } from 'vue-router'
import ChannelConnectModal from '@/components/channel/ChannelConnectModal.vue'
import ChannelOverviewCard from '@/components/channel/ChannelOverviewCard.vue'
import ChannelPriceAdjustmentSheet from '@/components/channel/ChannelPriceAdjustmentSheet.vue'
import ChannelWidgetModal from '@/components/channel/ChannelWidgetModal.vue'
import {
  buildChannelViewModel,
  buildPriceAdjustmentRequest,
  createPriceAdjustmentEditor,
  formatAdjustmentSummary,
  resolveSuChannelId,
  sortChannelViewModels,
  type ChannelViewModel,
  type PriceAdjustmentEditorValue,
} from '@/components/channel/channelUtils'
import {
  getAllOtaIntegrations,
  getSuMappingStatus,
  type SuMappingStatusSummary,
} from '@/api/otaIntegration'
import {
  getChannelPriceAdjustments,
  updateChannelPriceAdjustment,
  type ChannelPriceAdjustmentDTO,
} from '@/api/pricelabs'
import { useStoreStore } from '@/stores/store'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import {
  resolveChannelWarningMessage,
  sanitizeChannelWarningMessage,
} from '@/utils/channelMessage'
import { isHandledRequestError } from '@/utils/request'

type ChannelSegment = 'overview' | 'pricing'

const SUPPORTED_PRICE_ADJUSTMENT_CHANNEL_CODES = new Set(['AIRBNB', 'BOOKING'])

const router = useRouter()
const storeStore = useStoreStore()

const activeSegment = ref<ChannelSegment>('overview')
const loading = ref(false)
const priceLoading = ref(false)
const priceSubmitting = ref(false)
const channels = ref<ChannelViewModel[]>([])
const priceAdjustments = ref<ChannelPriceAdjustmentDTO[]>([])
const loadNotice = ref('')
const connectModalOpen = ref(false)
const widgetModalOpen = ref(false)
const priceSheetOpen = ref(false)
const selectedChannel = ref<ChannelViewModel | null>(null)
const selectedAdjustment = ref<PriceAdjustmentEditorValue | null>(null)

const storeName = computed(() => storeStore.currentStore?.name || '未选择门店')

const readyCount = computed(() => channels.value.filter((item) => item.mappingReady).length)
const incompleteCount = computed(() => channels.value.filter((item) => item.isConnected && !item.mappingReady).length)
function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  return resolveChannelWarningMessage(error, fallbackMessage)
}

async function loadChannels() {
  loading.value = true

  try {
    const response = await getAllOtaIntegrations()
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载渠道列表失败')
    }

    const mappingStatusById = new Map<number, SuMappingStatusSummary | null>()
    const mappingTasks: Promise<void>[] = []
    const warnings: string[] = []

    for (const item of response.data) {
      const suChannelId = resolveSuChannelId(item.code)
      if (!suChannelId) {
        mappingStatusById.set(item.id, null)
        continue
      }

      const task = getSuMappingStatus(item.id, suChannelId)
        .then((statusResponse) => {
          if (statusResponse.success) {
            mappingStatusById.set(item.id, statusResponse.data || null)
            return
          }

          mappingStatusById.set(item.id, null)
          if (statusResponse.message) {
            warnings.push(sanitizeChannelWarningMessage(statusResponse.message))
          }
        })
        .catch((error) => {
          mappingStatusById.set(item.id, null)
          warnings.push(resolveWarningMessage(error, `${item.name} 映射状态加载失败`))
        })

      mappingTasks.push(task)
    }

    await Promise.all(mappingTasks)

    const nextItems: ChannelViewModel[] = []
    for (const item of response.data) {
      nextItems.push(buildChannelViewModel(item, mappingStatusById.get(item.id) || null))
    }

    channels.value = sortChannelViewModels(nextItems)
    loadNotice.value = warnings.join('；')
  } finally {
    loading.value = false
  }
}

async function loadPriceAdjustments() {
  priceLoading.value = true

  try {
    const response = await getChannelPriceAdjustments()
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载价格比例失败')
    }

    priceAdjustments.value = response.data
      .filter((item) => SUPPORTED_PRICE_ADJUSTMENT_CHANNEL_CODES.has(item.channelCode))
      .sort((left, right) => {
      return left.channelName.localeCompare(right.channelName)
      })
  } finally {
    priceLoading.value = false
  }
}

async function loadPage() {
  try {
    const results = await Promise.allSettled([loadChannels(), loadPriceAdjustments()])
    const warnings: string[] = []

    for (const result of results) {
      if (result.status === 'rejected') {
        warnings.push(resolveWarningMessage(result.reason, '渠道数据加载失败'))
      }
    }

    if (warnings.length > 0) {
      const noticeMessage = warnings.join('；')
      loadNotice.value = noticeMessage
      showWarningToast(noticeMessage)
    }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '渠道数据加载失败'))
    }
  }
}

function handleSegmentChange(event: CustomEvent) {
  const nextSegment = event.detail.value as ChannelSegment
  activeSegment.value = nextSegment || 'overview'
}

function handleSegmentButtonClick(segment: ChannelSegment) {
  activeSegment.value = segment
}

async function openChannelDetail(otaId: number) {
  await router.push({
    name: 'ChannelDetail',
    params: { otaId },
  })
}

function openConnectEntry(item: ChannelViewModel) {
  selectedChannel.value = item
  if (item.isConnected) {
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
  void loadChannels()
}

function handleWidgetError(message: string) {
  loadNotice.value = sanitizeChannelWarningMessage(message, '渠道操作异常，请稍后重试')
}

function openPriceEditor(item: ChannelPriceAdjustmentDTO) {
  selectedAdjustment.value = createPriceAdjustmentEditor(item)
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

    showSuccessToast('价格比例已更新')
    priceSheetOpen.value = false
    await loadPriceAdjustments()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '更新价格比例失败'))
    }
  } finally {
    priceSubmitting.value = false
  }
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadPage()
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  await loadPage()
})
</script>

<style scoped>
.channels-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.channels-page__notice {
  margin-top: 12px;
  color: var(--ion-color-warning);
}

.channels-page__segment {
  margin-top: 4px;
  scroll-margin-top: calc(72px + var(--app-safe-top));
}

.channels-page__segment :deep(ion-segment-button) {
  position: relative;
}

.channels-page__segment-text {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 28px;
}

.channels-page__card-list,
.channels-page__price-list {
  margin-top: 16px;
}

.channels-page__card-list .mobile-note {
  word-break: break-all;
}

.channels-page__empty {
  padding-top: 18px;
}
</style>
