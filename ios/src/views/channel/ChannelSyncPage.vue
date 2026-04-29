<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="buildChannelDetailPath(otaId)" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ pageTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page channel-sync-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新同步状态" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="channelView" class="mobile-hero channel-sync-hero">
        <h1 class="mobile-title">{{ channelView.name }}</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip channel-sync-hero__chip channel-sync-hero__chip--status">
            {{ channelView.statusLabel }}
          </span>
          <span class="mobile-chip channel-sync-hero__chip channel-sync-hero__chip--metric">
            房型 {{ syncMappedRoomCount }}
          </span>
          <span class="mobile-chip channel-sync-hero__chip channel-sync-hero__chip--metric">
            范围 {{ syncDays }} 天
          </span>
        </div>
      </section>

      <div v-if="channelView" class="mobile-stack">
        <section class="mobile-card channel-sync-page__toolbar-card">
          <div class="channel-sync-page__toolbar-head">
            <div class="channel-sync-page__toolbar-main">
              <strong>{{ syncOverviewText }}</strong>
              <p>先选范围，再执行日历同步或完整刷新</p>
            </div>
            <div class="channel-sync-page__toolbar-side">
              <ion-spinner v-if="loading || actionLoading" name="crescent" />
              <ion-button
                size="small"
                fill="outline"
                class="channel-sync-page__action-secondary"
                @click="reloadSyncPage"
              >
                刷新
              </ion-button>
            </div>
          </div>

          <ion-segment
            class="channel-sync-page__range-segment"
            :value="syncDays"
            @ionChange="handleSyncDaysChange"
          >
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

          <div class="channel-sync-page__actions">
            <ion-button
              class="channel-sync-page__action-primary"
              :disabled="!canRunSync || actionLoading"
              @click="handleCalendarSync"
            >
              {{ actionLoading && runningAction === 'calendar' ? '同步中...' : '日历同步' }}
            </ion-button>
            <ion-button
              fill="outline"
              class="channel-sync-page__action-secondary"
              :disabled="!canRunSync || actionLoading"
              @click="handleFullRefresh"
            >
              {{ actionLoading && runningAction === 'full-refresh' ? '刷新中...' : '全量刷新' }}
            </ion-button>
            <ion-button
              v-if="capability.supportsInventorySettings"
              fill="clear"
              class="channel-sync-page__action-link"
              :disabled="actionLoading"
              @click="openInventoryPage"
            >
              房量设置
            </ion-button>
          </div>

          <p v-if="syncCombinedNotice" class="mobile-note channel-sync-page__warning">
            {{ syncCombinedNotice }}
          </p>
        </section>

        <section class="mobile-card channel-sync-page__result-card">
          <div class="channel-sync-page__section-heading">
            <h2 class="mobile-section-title">执行反馈</h2>
            <p class="mobile-note">{{ syncResultsSummaryText }}</p>
          </div>

          <div v-if="stageResults.length > 0" class="channel-sync-page__result-list">
            <article
              v-for="item in stageResults"
              :key="item.key"
              :class="[
                'channel-sync-page__result-item',
                `channel-sync-page__result-item--${item.statusColor}`,
              ]"
            >
              <div class="channel-sync-page__result-header">
                <strong>{{ item.label }}</strong>
                <ion-badge :color="item.statusColor">{{ item.statusLabel }}</ion-badge>
              </div>
              <p>{{ item.message }}</p>
            </article>
          </div>

          <p v-else class="mobile-note channel-sync-page__empty">
            尚未在当前会话执行同步动作。
          </p>
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
import { buildChannelDetailPath, buildChannelInventoryPath } from '@/router/guards'
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
import { resolveChannelWarningMessage, resolveMappingStatusNotice } from '@/utils/channelMessage'
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
    return '当前渠道没有可用的同步接口，移动端暂不支持直接执行同步。'
  }
  return ''
})
const syncMappedRoomCount = computed(() => Number(mappingStatus.value?.mappedRoomIdCount || 0))
const syncActiveRatePlanCount = computed(() => Number(mappingStatus.value?.activeRatePlanCount || 0))
const syncMappingNotice = computed(() => resolveMappingStatusNotice(mappingStatus.value?.error))
const syncOverviewText = computed(() => {
  if (!channelView.value) {
    return '同步面板待刷新'
  }
  if (!capability.value.supportsSu) {
    return '当前渠道不支持同步动作'
  }
  if (!channelView.value.isConnected) {
    return '完成授权后可执行同步'
  }
  if (syncMappedRoomCount.value === 0 && syncActiveRatePlanCount.value === 0) {
    return `已授权 · 范围 ${syncDays.value} 天`
  }
  return `房型 ${syncMappedRoomCount.value} · 活跃价盘 ${syncActiveRatePlanCount.value} · 范围 ${syncDays.value} 天`
})
const syncCombinedNotice = computed(() => {
  return joinUniqueMessages([
    loadNotice.value,
    syncMappingNotice.value,
    canRunSync.value ? '' : syncBlockedMessage.value,
  ])
})
const syncResultsSummaryText = computed(() => {
  if (actionLoading.value && stageResults.value.length === 0) {
    return '同步执行中，完成后会显示各阶段结果'
  }
  if (stageResults.value.length === 0) {
    return '最近执行的阶段结果会显示在这里'
  }

  let successCount = 0
  let warningCount = 0
  let dangerCount = 0

  for (const item of stageResults.value) {
    if (item.statusColor === 'success') {
      successCount += 1
      continue
    }
    if (item.statusColor === 'warning') {
      warningCount += 1
      continue
    }
    if (item.statusColor === 'danger') {
      dangerCount += 1
    }
  }

  const pieces = [`共 ${stageResults.value.length} 个阶段`]
  if (successCount > 0) {
    pieces.push(`成功 ${successCount}`)
  }
  if (warningCount > 0) {
    pieces.push(`待处理 ${warningCount}`)
  }
  if (dangerCount > 0) {
    pieces.push(`失败 ${dangerCount}`)
  }
  return pieces.join(' · ')
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  return resolveChannelWarningMessage(error, fallbackMessage)
}

function joinUniqueMessages(messages: Array<string | null | undefined>) {
  const result: string[] = []

  for (const message of messages) {
    const normalizedMessage = (message || '').trim()
    if (!normalizedMessage || result.includes(normalizedMessage)) {
      continue
    }
    result.push(normalizedMessage)
  }

  return result.join(' · ')
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
      loadNotice.value = '当前渠道没有 Su 同步能力，无法在移动端执行同步。'
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
      nextResults.push(buildWarningStage('ari', 'ARI 推送', ariResponse.data.error || 'ARI 推送未完成'))
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

async function reloadSyncPage() {
  try {
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '刷新同步状态失败'))
    }
  }
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
.channel-sync-page {
  --background: linear-gradient(180deg, #eef3fb 0%, #f4f7fc 18%, #f8fafd 100%);
}

.channel-sync-hero {
  padding: 20px 20px 18px;
  border-color: rgba(110, 131, 171, 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(251, 253, 255, 0.86));
  box-shadow: 0 16px 28px rgba(22, 38, 70, 0.035);
}

.channel-sync-hero::before {
  background: linear-gradient(135deg, rgba(55, 95, 167, 0.08), transparent 62%);
}

.channel-sync-hero .mobile-title {
  font-size: 24px;
  letter-spacing: -0.04em;
}

.channel-sync-hero .mobile-chip-row {
  gap: 10px;
  margin-top: 16px;
}

.channel-sync-hero__chip {
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  backdrop-filter: blur(10px);
}

.channel-sync-hero__chip--status {
  border-color: rgba(64, 101, 181, 0.14);
  background: rgba(64, 101, 181, 0.08);
  color: #3558a6;
}

.channel-sync-hero__chip--metric {
  border-color: rgba(117, 136, 173, 0.1);
  background: rgba(247, 249, 252, 0.94);
  color: #60708d;
}

.channel-sync-page__toolbar-card,
.channel-sync-page__result-card {
  border-color: rgba(112, 130, 166, 0.1);
  box-shadow: 0 14px 26px rgba(22, 38, 70, 0.035);
}

.channel-sync-page__toolbar-card {
  position: relative;
  overflow: hidden;
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(249, 252, 255, 0.92));
}

.channel-sync-page__toolbar-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 52px;
  background: linear-gradient(90deg, rgba(68, 103, 180, 0.06), transparent 72%);
  pointer-events: none;
}

.channel-sync-page__toolbar-card > * {
  position: relative;
  z-index: 1;
}

.channel-sync-page__toolbar-head,
.channel-sync-page__toolbar-main,
.channel-sync-page__result-list {
  display: grid;
  gap: 12px;
}

.channel-sync-page__toolbar-head {
  gap: 14px;
}

.channel-sync-page__toolbar-main strong,
.channel-sync-page__result-item strong {
  margin: 0;
  color: var(--app-heading);
}

.channel-sync-page__toolbar-main strong {
  display: block;
  font-size: 20px;
  letter-spacing: -0.03em;
}

.channel-sync-page__toolbar-main p,
.channel-sync-page__result-item p {
  margin: 6px 0 0;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.channel-sync-page__toolbar-main p {
  color: var(--ios-pms-text-soft);
  font-size: 12px;
}

.channel-sync-page__toolbar-side,
.channel-sync-page__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.channel-sync-page__toolbar-side {
  justify-content: flex-end;
}

.channel-sync-page__toolbar-side ion-spinner {
  color: var(--ios-pms-primary);
  transform: scale(0.82);
}

.channel-sync-page__range-segment {
  padding: 3px;
  border-color: rgba(111, 130, 168, 0.12);
  border-radius: 16px;
  background: rgba(236, 241, 249, 0.88);
}

.channel-sync-page__range-segment ion-segment-button {
  --border-radius: 13px;
  --indicator-color: rgba(86, 116, 173, 0.16);
  min-height: 38px;
  font-size: 12px;
}

.channel-sync-page__actions {
  margin-top: 16px;
}

.channel-sync-page__actions ion-button {
  min-height: 38px;
  margin: 0;
  --border-radius: 14px;
}

.channel-sync-page__action-primary {
  --background: #234ebd;
  --background-activated: #2046aa;
  --background-hover: #2046aa;
  --box-shadow: 0 10px 20px rgba(35, 78, 189, 0.18);
  --padding-start: 18px;
  --padding-end: 18px;
}

.channel-sync-page__action-secondary {
  --background: rgba(255, 255, 255, 0.72);
  --border-color: rgba(71, 101, 163, 0.18);
  --color: #3f609b;
  --padding-start: 14px;
  --padding-end: 14px;
}

.channel-sync-page__action-link {
  --color: #5573ad;
  --padding-start: 4px;
  --padding-end: 4px;
  font-size: 12px;
  font-weight: 600;
}

.channel-sync-page__warning {
  margin-top: 10px;
  padding: 10px 12px 10px 14px;
  border-left: 3px solid #eb9629;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 248, 238, 0.98), rgba(255, 251, 246, 0.92));
  color: #b16d12;
  font-size: 12px;
  line-height: 1.55;
}

.channel-sync-page__section-heading {
  display: grid;
  gap: 4px;
}

.channel-sync-page__section-heading .mobile-section-title {
  margin-bottom: 0;
}

.channel-sync-page__result-list {
  gap: 10px;
  margin-top: 16px;
}

.channel-sync-page__result-item {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(112, 130, 166, 0.11);
}

.channel-sync-page__result-item--success {
  background: linear-gradient(180deg, rgba(241, 250, 246, 0.98), rgba(248, 252, 249, 0.94));
  border-color: rgba(61, 137, 102, 0.14);
}

.channel-sync-page__result-item--warning {
  background: linear-gradient(180deg, rgba(255, 249, 240, 0.98), rgba(255, 252, 247, 0.94));
  border-color: rgba(222, 149, 44, 0.16);
}

.channel-sync-page__result-item--danger {
  background: linear-gradient(180deg, rgba(255, 245, 245, 0.98), rgba(255, 250, 250, 0.94));
  border-color: rgba(213, 96, 96, 0.14);
}

.channel-sync-page__result-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.channel-sync-page__result-header ion-badge {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.channel-sync-page__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  margin-top: 16px;
  padding: 28px 18px;
  border: 1px dashed rgba(117, 136, 173, 0.16);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.9), rgba(246, 249, 253, 0.76));
  color: var(--ios-pms-text-soft);
  text-align: center;
}

@media (max-width: 480px) {
  .channel-sync-page__toolbar-side {
    justify-content: flex-start;
  }

  .channel-sync-page__actions ion-button {
    flex: 1 1 100%;
  }
}
</style>
