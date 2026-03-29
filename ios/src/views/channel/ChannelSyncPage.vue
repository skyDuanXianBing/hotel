<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="buildChannelDetailPath(otaId)" />
        </ion-buttons>
        <ion-title>{{ pageTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page channel-sync-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新同步状态" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="channelView" class="mobile-hero channel-sync-hero">
        <p class="mobile-note channel-sync-hero__eyebrow">同步中心</p>
        <h1 class="mobile-title">{{ channelView.name }}</h1>
        <p class="mobile-subtitle">在移动端执行日历同步与全量刷新，并查看每个阶段的结果反馈。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ channelView.statusLabel }}</span>
          <span class="mobile-chip">{{ capability.groupLabel }}映射 {{ mappingStatus?.mappedRoomIdCount || 0 }}</span>
          <span class="mobile-chip">范围 {{ syncDays }} 天</span>
        </div>
      </section>

      <div v-if="channelView" class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">同步范围</h2>
              <p class="mobile-note">日历同步与全量刷新都会使用该范围天数。</p>
            </div>
            <ion-spinner v-if="loading || actionLoading" name="crescent" />
          </div>

          <ion-segment :value="syncDays" @ionChange="handleSyncDaysChange">
            <ion-segment-button value="30">
              <ion-label>30 天</ion-label>
            </ion-segment-button>
            <ion-segment-button value="90">
              <ion-label>90 天</ion-label>
            </ion-segment-button>
            <ion-segment-button value="365">
              <ion-label>365 天</ion-label>
            </ion-segment-button>
          </ion-segment>

          <p v-if="loadNotice" class="mobile-note channel-sync-page__warning">{{ loadNotice }}</p>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">执行动作</h2>
          <p class="mobile-note">日历同步优先推送房量与价格；全量刷新会先重拉房型、价盘，再推送 ARI。</p>

          <div class="channel-sync-page__actions">
            <ion-button :disabled="!canRunSync || actionLoading" @click="handleCalendarSync">
              {{ actionLoading && runningAction === 'calendar' ? '同步中...' : '日历同步' }}
            </ion-button>
            <ion-button
              fill="outline"
              :disabled="!canRunSync || actionLoading"
              @click="handleFullRefresh"
            >
              {{ actionLoading && runningAction === 'full-refresh' ? '刷新中...' : '全量刷新' }}
            </ion-button>
            <ion-button
              v-if="capability.supportsInventorySettings"
              fill="outline"
              :disabled="actionLoading"
              @click="openInventoryPage"
            >
              Airbnb 房量设置
            </ion-button>
          </div>

          <p v-if="!canRunSync" class="mobile-note channel-sync-page__warning">
            {{ syncBlockedMessage }}
          </p>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">最近结果</h2>
          <div v-if="stageResults.length > 0" class="channel-sync-page__result-list">
            <article
              v-for="item in stageResults"
              :key="item.key"
              class="channel-sync-page__result-item"
            >
              <div class="channel-sync-page__result-header">
                <strong>{{ item.label }}</strong>
                <ion-badge :color="item.statusColor">{{ item.statusLabel }}</ion-badge>
              </div>
              <p>{{ item.message }}</p>
            </article>
          </div>
          <p v-else class="mobile-note">尚未在当前会话执行同步动作。</p>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">说明</h2>
          <ul class="mobile-bullet-list">
            <li>日历同步：按当前范围依次推送房量与价格。</li>
            <li>全量刷新：按房型、价盘、ARI 顺序执行，阶段失败会逐条展示。</li>
            <li>如当前渠道无 Su 能力或未授权，会明确提示而不是静默失败。</li>
          </ul>
        </section>
      </div>

      <section v-else class="mobile-card">
        <p class="mobile-note">{{ loading ? '同步信息加载中...' : '未找到该渠道配置。' }}</p>
      </section>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonBackButton,
  IonBadge,
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
import {
  buildChannelDetailPath,
  buildChannelInventoryPath,
} from '@/router/guards'
import {
  buildChannelViewModel,
  getChannelActionCapability,
  resolveSuChannelId,
  type ChannelBadgeColor,
} from '@/components/channel/channelUtils'
import {
  getOtaIntegrationById,
  getSuMappingStatus,
  syncSuAri,
  syncSuAvailability,
  syncSuRatePlans,
  syncSuRates,
  syncSuRooms,
  type OtaIntegrationDTO,
  type SuMappingStatusSummary,
} from '@/api/otaIntegration'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

interface SyncStageResult {
  key: string
  label: string
  statusLabel: string
  statusColor: ChannelBadgeColor | 'danger'
  message: string
}

type RunningAction = 'calendar' | 'full-refresh' | ''

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const actionLoading = ref(false)
const channel = ref<OtaIntegrationDTO | null>(null)
const mappingStatus = ref<SuMappingStatusSummary | null>(null)
const loadNotice = ref('')
const syncDays = ref('30')
const runningAction = ref<RunningAction>('')
const stageResults = ref<SyncStageResult[]>([])

const otaId = computed(() => Number(route.params.otaId || 0))
const channelView = computed(() => {
  if (!channel.value) {
    return null
  }
  return buildChannelViewModel(channel.value, mappingStatus.value)
})
const capability = computed(() => getChannelActionCapability(channel.value?.code))
const pageTitle = computed(() => {
  if (!channelView.value) {
    return '渠道同步'
  }
  return `${channelView.value.name} · 同步`
})
const canRunSync = computed(() => {
  if (!channelView.value) {
    return false
  }
  if (!channelView.value.isConnected) {
    return false
  }
  return capability.value.supportsSu
})
const syncBlockedMessage = computed(() => {
  if (!channelView.value) {
    return '未找到该渠道配置。'
  }
  if (!channelView.value.isConnected) {
    return '请先完成授权或重连，再执行同步动作。'
  }
  if (!capability.value.supportsSu) {
    return '当前渠道没有可用的同步接口，移动端暂不支持直接执行日历同步或全量刷新。'
  }
  return ''
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function ensureCanRunSyncAction() {
  if (!canRunSync.value || !channelView.value) {
    showWarningToast(syncBlockedMessage.value || '当前无法执行同步动作')
    return false
  }

  return true
}

function buildSuccessStage(key: string, label: string, message: string): SyncStageResult {
  return {
    key,
    label,
    statusLabel: '成功',
    statusColor: 'success',
    message,
  }
}

function buildWarningStage(key: string, label: string, message: string): SyncStageResult {
  return {
    key,
    label,
    statusLabel: '待处理',
    statusColor: 'warning',
    message,
  }
}

function buildDangerStage(key: string, label: string, message: string): SyncStageResult {
  return {
    key,
    label,
    statusLabel: '失败',
    statusColor: 'danger',
    message,
  }
}

async function loadPage() {
  if (!otaId.value) {
    return
  }

  loading.value = true
  loadNotice.value = ''

  try {
    const detailResponse = await getOtaIntegrationById(otaId.value)
    if (!detailResponse.success || !detailResponse.data) {
      throw new Error(detailResponse.message || '加载渠道同步信息失败')
    }

    channel.value = detailResponse.data
    const nextCapability = getChannelActionCapability(detailResponse.data.code)
    if (!nextCapability.supportsSu) {
      mappingStatus.value = null
      loadNotice.value = '当前渠道没有 Su 同步能力，页面仅保留能力说明。'
      return
    }

    const mappingResponse = await getSuMappingStatus(
      otaId.value,
      resolveSuChannelId(detailResponse.data.code) || undefined,
    )
    if (!mappingResponse.success) {
      mappingStatus.value = null
      loadNotice.value = mappingResponse.message || '映射摘要加载失败'
      return
    }

    mappingStatus.value = mappingResponse.data || null
  } finally {
    loading.value = false
  }
}

function handleSyncDaysChange(event: CustomEvent) {
  syncDays.value = (event.detail.value as string) || '30'
}

function getSyncDaysValue() {
  return Number(syncDays.value || 30)
}

async function confirmFullRefresh() {
  const alert = await alertController.create({
    header: '执行全量刷新',
    message: `将按房型、价盘、ARI 顺序刷新 ${getSyncDaysValue()} 天范围的数据，确认继续吗？`,
    buttons: [
      {
        text: '取消',
        role: 'cancel',
      },
      {
        text: '确认执行',
        role: 'confirm',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'confirm'
}

async function handleCalendarSync() {
  if (!ensureCanRunSyncAction()) {
    return
  }

  const currentChannelView = channelView.value
  if (!currentChannelView) {
    showWarningToast(syncBlockedMessage.value || '当前无法执行同步动作')
    return
  }

  actionLoading.value = true
  runningAction.value = 'calendar'
  stageResults.value = []

  try {
    const days = getSyncDaysValue()
    const nextResults: SyncStageResult[] = []

    const availabilityResponse = await syncSuAvailability(currentChannelView.id, days)
    if (!availabilityResponse.success || !availabilityResponse.data) {
      nextResults.push(
        buildDangerStage('availability', '房量同步', availabilityResponse.message || '房量同步失败'),
      )
    } else if (availabilityResponse.data.availabilitySynced) {
      nextResults.push(
        buildSuccessStage(
          'availability',
          '房量同步',
          `已同步 ${availabilityResponse.data.roomCount} 个房型，范围 ${availabilityResponse.data.days} 天`,
        ),
      )
    } else {
      nextResults.push(
        buildWarningStage(
          'availability',
          '房量同步',
          availabilityResponse.data.availabilityError || '房量同步未完成，请稍后重试',
        ),
      )
    }

    const ratesResponse = await syncSuRates(currentChannelView.id, days)
    if (!ratesResponse.success || !ratesResponse.data) {
      nextResults.push(buildDangerStage('rates', '价格同步', ratesResponse.message || '价格同步失败'))
    } else if (ratesResponse.data.rateSynced) {
      nextResults.push(
        buildSuccessStage(
          'rates',
          '价格同步',
          `已推送 ${ratesResponse.data.combinationsPushed} 个组合，缺价跳过 ${ratesResponse.data.combinationsSkippedNoPrice}`,
        ),
      )
    } else {
      nextResults.push(
        buildWarningStage(
          'rates',
          '价格同步',
          ratesResponse.data.error || '价格同步未完成，请检查价盘映射',
        ),
      )
    }

    stageResults.value = nextResults
    const hasFailure = nextResults.some((item) => item.statusColor !== 'success')
    if (hasFailure) {
      showWarningToast('日历同步已执行，请查看阶段结果')
    } else {
      showSuccessToast('日历同步完成')
    }
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '日历同步失败'))
    }
  } finally {
    actionLoading.value = false
    runningAction.value = ''
  }
}

async function handleFullRefresh() {
  if (!ensureCanRunSyncAction()) {
    return
  }

  const currentChannelView = channelView.value
  if (!currentChannelView) {
    showWarningToast(syncBlockedMessage.value || '当前无法执行同步动作')
    return
  }

  const confirmed = await confirmFullRefresh()
  if (!confirmed) {
    return
  }

  actionLoading.value = true
  runningAction.value = 'full-refresh'
  stageResults.value = []

  try {
    const days = getSyncDaysValue()
    const nextResults: SyncStageResult[] = []

    const roomsResponse = await syncSuRooms(currentChannelView.id)
    if (!roomsResponse.success || !roomsResponse.data) {
      nextResults.push(buildDangerStage('rooms', '房型刷新', roomsResponse.message || '房型刷新失败'))
    } else if (roomsResponse.data.roomsSynced) {
      nextResults.push(
        buildSuccessStage('rooms', '房型刷新', `已刷新 ${roomsResponse.data.roomCount} 个房型`),
      )
    } else {
      nextResults.push(
        buildWarningStage('rooms', '房型刷新', roomsResponse.data.roomsError || '房型刷新未完成'),
      )
    }

    const ratePlansResponse = await syncSuRatePlans(currentChannelView.id)
    if (!ratePlansResponse.success || !ratePlansResponse.data) {
      nextResults.push(
        buildDangerStage('rate-plans', '价盘刷新', ratePlansResponse.message || '价盘刷新失败'),
      )
    } else if (ratePlansResponse.data.ratePlansSynced) {
      nextResults.push(
        buildSuccessStage(
          'rate-plans',
          '价盘刷新',
          `已刷新 ${ratePlansResponse.data.pricePlanCount} 个价盘`,
        ),
      )
    } else {
      nextResults.push(
        buildWarningStage(
          'rate-plans',
          '价盘刷新',
          ratePlansResponse.data.ratePlansError || '价盘刷新未完成',
        ),
      )
    }

    const ariResponse = await syncSuAri(currentChannelView.id, days)
    if (!ariResponse.success || !ariResponse.data) {
      nextResults.push(buildDangerStage('ari', 'ARI 推送', ariResponse.message || 'ARI 推送失败'))
    } else if (ariResponse.data.availabilityPushed || ariResponse.data.ratesPushed) {
      nextResults.push(
        buildSuccessStage(
          'ari',
          'ARI 推送',
          `已处理 ${ariResponse.data.roomCount} 个房型、${ariResponse.data.ratePlanCount} 个价盘，范围 ${ariResponse.data.days} 天`,
        ),
      )
    } else {
      nextResults.push(
        buildWarningStage('ari', 'ARI 推送', ariResponse.data.error || 'ARI 推送未完成'),
      )
    }

    stageResults.value = nextResults
    const hasFailure = nextResults.some((item) => item.statusColor !== 'success')
    if (hasFailure) {
      showWarningToast('全量刷新已执行，请查看阶段结果')
    } else {
      showSuccessToast('全量刷新完成')
    }
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '全量刷新失败'))
    }
  } finally {
    actionLoading.value = false
    runningAction.value = ''
  }
}

async function openInventoryPage() {
  await router.push(buildChannelInventoryPath(otaId.value))
}

async function maybeHandleAutoAction() {
  const rawAction = Array.isArray(route.query.action) ? route.query.action[0] : route.query.action
  if (rawAction !== 'calendar' && rawAction !== 'full-refresh') {
    return
  }

  await router.replace({
    name: 'ChannelSync',
    params: { otaId: otaId.value },
  })

  try {
    if (rawAction === 'calendar') {
      await handleCalendarSync()
      return
    }

    await handleFullRefresh()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '同步动作执行失败'))
    }
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
  await maybeHandleAutoAction()
})
</script>

<style scoped>
.channel-sync-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.channel-sync-page__warning {
  margin-top: 12px;
  color: var(--ion-color-warning);
}

.channel-sync-page__actions,
.channel-sync-page__result-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.channel-sync-page__result-item {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--app-border);
}

.channel-sync-page__result-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.channel-sync-page__result-item strong,
.channel-sync-page__result-item p {
  margin: 0;
}

.channel-sync-page__result-item p {
  margin-top: 8px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}
</style>
