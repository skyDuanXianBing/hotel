<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar channels-page__toolbar">
        <ion-buttons slot="start">
          <ion-back-button
            class="app-page-header__back-btn channels-page__back-btn"
            :default-href="ROUTE_PATHS.home"
          />
        </ion-buttons>
        <ion-title class="app-page-header__title channels-page__title">渠道</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="mobile-page channels-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新渠道状态" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="loadNotice" class="channels-page__notice">
        <p class="mobile-note channels-page__notice-text">{{ loadNotice }}</p>
      </section>

      <div v-if="channels.length > 0" class="mobile-list channels-page__card-list">
        <ChannelOverviewCard
          v-for="item in channels"
          :key="item.id"
          :item="item"
          @open-detail="openChannelDetail(item.id)"
          @open-connect="openConnectEntry(item)"
        />
      </div>

      <section v-else-if="loading" class="channels-page__loading">
        <ion-spinner name="crescent" />
        <p class="mobile-note">正在同步渠道状态…</p>
      </section>

      <p v-else class="mobile-note channels-page__empty">当前门店暂无可用渠道配置。</p>
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
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import ChannelConnectModal from '@/components/channel/ChannelConnectModal.vue'
import ChannelOverviewCard from '@/components/channel/ChannelOverviewCard.vue'
import ChannelWidgetModal from '@/components/channel/ChannelWidgetModal.vue'
import {
  buildChannelViewModel,
  resolveSuChannelId,
  sortChannelViewModels,
  type ChannelViewModel,
} from '@/components/channel/channelUtils'
import {
  getAllOtaIntegrations,
  getSuMappingStatus,
  type SuMappingStatusSummary,
} from '@/api/otaIntegration'
import { ROUTE_PATHS } from '@/router/guards'
import { showWarningToast } from '@/utils/notify'
import {
  resolveChannelWarningMessage,
  sanitizeChannelWarningMessage,
} from '@/utils/channelMessage'
import { isHandledRequestError } from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const channels = ref<ChannelViewModel[]>([])
const loadNotice = ref('')
const connectModalOpen = ref(false)
const widgetModalOpen = ref(false)
const selectedChannel = ref<ChannelViewModel | null>(null)
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

async function loadPage() {
  try {
    await loadChannels()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '渠道数据加载失败'))
    }
  }
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
.channels-page__toolbar {
  --background: #ffffff;
  --min-height: 54px;
}

.channels-page__back-btn {
  --color: var(--ios-pms-header-control-color);
}

.channels-page__title {
  color: var(--ios-pms-header-title-color);
  font-size: 21px;
  font-weight: 500;
  letter-spacing: 0;
}

.channels-page {
  --background: #f3f7fd;
  --padding-top: 8px;
  --padding-start: 14px;
  --padding-end: 14px;
  --padding-bottom: calc(28px + var(--app-safe-bottom));
}

.channels-page__notice {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid rgba(227, 139, 24, 0.16);
  border-radius: 18px;
  background: rgba(255, 249, 239, 0.92);
}

.channels-page__notice-text {
  color: #99661c;
}

.channels-page__card-list {
  display: grid;
  gap: 10px;
}

.channels-page__loading {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 72px 0 32px;
}

.channels-page__empty {
  padding: 72px 4px 0;
  text-align: center;
}
</style>
