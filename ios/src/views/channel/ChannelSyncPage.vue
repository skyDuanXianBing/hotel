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
        <ion-refresher-content :pulling-text="$t('channel.mobile.refreshSync')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="channelView" class="mobile-hero channel-sync-hero">
        <h1 class="mobile-title">{{ channelView.name }}</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip channel-sync-hero__chip channel-sync-hero__chip--status">
            {{ channelView.statusLabel }}
          </span>
          <span class="mobile-chip channel-sync-hero__chip channel-sync-hero__chip--metric">
            {{ $t('channel.mobile.common.roomTypes') }} {{ syncMappedRoomCount }}
          </span>
          <span class="mobile-chip channel-sync-hero__chip channel-sync-hero__chip--metric">
            {{ $t('channel.mobile.dayRange', { count: syncDays }) }}
          </span>
        </div>
      </section>

      <div v-if="channelView" class="mobile-stack">
        <section class="mobile-card channel-sync-page__toolbar-card">
          <div class="channel-sync-page__toolbar-head">
            <div class="channel-sync-page__toolbar-main">
              <strong>{{ syncOverviewText }}</strong>
              <p>{{ $t('channel.mobile.sync.description') }}</p>
            </div>
            <div class="channel-sync-page__toolbar-side">
              <ion-spinner v-if="loading || actionLoading" name="crescent" />
              <ion-button
                size="small"
                fill="outline"
                class="channel-sync-page__action-secondary"
                @click="reloadSyncPage"
              >
                {{ $t('channel.mobile.common.refresh') }}
              </ion-button>
            </div>
          </div>

          <ion-segment
            class="channel-sync-page__range-segment"
            :value="syncDays"
            @ionChange="handleSyncDaysChange"
          >
            <ion-segment-button value="30">
              <ion-label>{{ $t('channel.mobile.days', { count: 30 }) }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="90">
              <ion-label>{{ $t('channel.mobile.days', { count: 90 }) }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="365">
              <ion-label>{{ $t('channel.mobile.days', { count: 365 }) }}</ion-label>
            </ion-segment-button>
          </ion-segment>

          <div class="channel-sync-page__actions">
            <ion-button
              class="channel-sync-page__action-primary"
              :disabled="!canRunSync || actionLoading"
              @click="handleCalendarSync"
            >
              {{ actionLoading && runningAction === 'calendar' ? $t('channel.mobile.sync.syncing') : $t('channel.mobile.sync.calendar') }}
            </ion-button>
            <ion-button
              fill="outline"
              class="channel-sync-page__action-secondary"
              :disabled="!canRunSync || actionLoading"
              @click="handleFullRefresh"
            >
              {{ actionLoading && runningAction === 'full-refresh' ? $t('channel.mobile.sync.refreshing') : $t('channel.mobile.sync.fullRefresh') }}
            </ion-button>
            <ion-button
              v-if="capability.supportsInventorySettings"
              fill="clear"
              class="channel-sync-page__action-link"
              :disabled="actionLoading"
              @click="openInventoryPage"
            >
              {{ $t('channel.mobile.sync.inventorySettings') }}
            </ion-button>
          </div>

          <p v-if="syncCombinedNotice" class="mobile-note channel-sync-page__warning">
            {{ syncCombinedNotice }}
          </p>
        </section>

        <section class="mobile-card channel-sync-page__result-card">
          <div class="channel-sync-page__section-heading">
            <h2 class="mobile-section-title">{{ $t('channel.mobile.sync.feedback') }}</h2>
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
            {{ $t('channel.mobile.sync.noResults') }}
          </p>
        </section>
      </div>

      <section v-else class="mobile-card">
        <p class="mobile-note">
          {{ loading ? $t('channel.mobile.sync.loading') : $t('channel.mobile.common.notFound') }}
        </p>
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
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()

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
    return t('routes.ChannelSync')
  }
  return `${channelView.value.name} · ${t('routes.ChannelSync')}`
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
    return t('channel.mobile.common.notFound')
  }
  if (!channelView.value.isConnected) {
    return t('channel.mobile.sync.authorizeFirst')
  }
  if (!capability.value.supportsSu) {
    return t('channel.mobile.sync.unsupported')
  }
  return ''
})
const syncMappedRoomCount = computed(() => Number(mappingStatus.value?.mappedRoomIdCount || 0))
const syncActiveRatePlanCount = computed(() => Number(mappingStatus.value?.activeRatePlanCount || 0))
const syncMappingNotice = computed(() => resolveMappingStatusNotice(mappingStatus.value?.error))
const syncOverviewText = computed(() => {
  if (!channelView.value) {
    return t('channel.mobile.sync.pendingRefresh')
  }
  if (!capability.value.supportsSu) {
    return t('channel.mobile.sync.unsupportedTitle')
  }
  if (!channelView.value.isConnected) {
    return t('channel.mobile.sync.authorizeTitle')
  }
  if (syncMappedRoomCount.value === 0 && syncActiveRatePlanCount.value === 0) {
    return t('channel.mobile.sync.authorizedOverview', { days: syncDays.value })
  }
  return t('channel.mobile.sync.overview', {
    rooms: syncMappedRoomCount.value,
    ratePlans: syncActiveRatePlanCount.value,
    days: syncDays.value,
  })
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
    return t('channel.mobile.sync.runningSummary')
  }
  if (stageResults.value.length === 0) {
    return t('channel.mobile.sync.resultSummary')
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

  const pieces = [t('channel.mobile.sync.stageCount', { count: stageResults.value.length })]
  if (successCount > 0) {
    pieces.push(t('channel.mobile.sync.successCount', { count: successCount }))
  }
  if (warningCount > 0) {
    pieces.push(t('channel.mobile.sync.pendingCount', { count: warningCount }))
  }
  if (dangerCount > 0) {
    pieces.push(t('channel.mobile.sync.failedCount', { count: dangerCount }))
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
    showWarningToast(syncBlockedMessage.value || t('channel.mobile.sync.unavailable'))
    return false
  }

  return true
}

function buildSuccessStage(key: string, label: string, message: string): SyncStageResult {
  return {
    key,
    label,
    statusLabel: t('channel.mobile.common.success'),
    statusColor: 'success',
    message,
  }
}

function buildWarningStage(key: string, label: string, message: string): SyncStageResult {
  return {
    key,
    label,
    statusLabel: t('channel.mobile.common.pending'),
    statusColor: 'warning',
    message,
  }
}

function buildDangerStage(key: string, label: string, message: string): SyncStageResult {
  return {
    key,
    label,
    statusLabel: t('channel.mobile.common.failed'),
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
      throw new Error(detailResponse.message || t('channel.mobile.sync.loadFailed'))
    }

    channel.value = detailResponse.data
    const nextCapability = getChannelActionCapability(detailResponse.data.code)
    if (!nextCapability.supportsSu) {
      mappingStatus.value = null
      loadNotice.value = t('channel.mobile.sync.noSuCapability')
      return
    }

    const mappingResponse = await getSuMappingStatus(
      otaId.value,
      resolveSuChannelId(detailResponse.data.code) || undefined,
    )
    if (!mappingResponse.success) {
      mappingStatus.value = null
      loadNotice.value = mappingResponse.message || t('channel.mobile.sync.mappingSummaryFailed')
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
    header: t('channel.mobile.sync.confirmTitle'),
    message: t('channel.mobile.sync.confirmMessage', {
      days: getSyncDaysValue(),
    }),
    buttons: [
      {
        text: t('channel.mobile.common.cancel'),
        role: 'cancel',
      },
      {
        text: t('channel.mobile.sync.confirm'),
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
    showWarningToast(syncBlockedMessage.value || t('channel.mobile.sync.unavailable'))
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
        buildDangerStage(
          'availability',
          t('channel.mobile.sync.inventorySync'),
          availabilityResponse.message || t('channel.mobile.sync.inventorySyncFailed'),
        ),
      )
    } else if (availabilityResponse.data.availabilitySynced) {
      nextResults.push(
        buildSuccessStage(
          'availability',
          t('channel.mobile.sync.inventorySync'),
          t('channel.mobile.sync.inventorySyncResult', {
            rooms: availabilityResponse.data.roomCount,
            days: availabilityResponse.data.days,
          }),
        ),
      )
    } else {
      nextResults.push(
        buildWarningStage(
          'availability',
          t('channel.mobile.sync.inventorySync'),
          availabilityResponse.data.availabilityError || t('channel.mobile.sync.inventorySyncPending'),
        ),
      )
    }

    const ratesResponse = await syncSuRates(currentChannelView.id, days)
    if (!ratesResponse.success || !ratesResponse.data) {
      nextResults.push(
        buildDangerStage('rates', t('channel.mobile.sync.rateSync'), ratesResponse.message || t('channel.mobile.sync.rateSyncFailed')),
      )
    } else if (ratesResponse.data.rateSynced) {
      nextResults.push(
        buildSuccessStage(
          'rates',
          t('channel.mobile.sync.rateSync'),
          t('channel.mobile.sync.rateSyncResult', {
            pushed: ratesResponse.data.combinationsPushed,
            skipped: ratesResponse.data.combinationsSkippedNoPrice,
          }),
        ),
      )
    } else {
      nextResults.push(
        buildWarningStage(
          'rates',
          t('channel.mobile.sync.rateSync'),
          ratesResponse.data.error || t('channel.mobile.sync.rateSyncPending'),
        ),
      )
    }

    stageResults.value = nextResults
    const hasFailure = nextResults.some((item) => item.statusColor !== 'success')
    if (hasFailure) {
      showWarningToast(t('channel.mobile.sync.calendarReview'))
    } else {
      showSuccessToast(t('channel.mobile.sync.calendarComplete'))
    }
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('channel.mobile.sync.calendarFailed')))
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
    showWarningToast(syncBlockedMessage.value || t('channel.mobile.sync.unavailable'))
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
      nextResults.push(
        buildDangerStage('rooms', t('channel.mobile.sync.roomRefresh'), roomsResponse.message || t('channel.mobile.sync.roomRefreshFailed')),
      )
    } else if (roomsResponse.data.roomsSynced) {
      nextResults.push(
        buildSuccessStage(
          'rooms',
          t('channel.mobile.sync.roomRefresh'),
          t('channel.mobile.sync.roomRefreshResult', { count: roomsResponse.data.roomCount }),
        ),
      )
    } else {
      nextResults.push(
        buildWarningStage(
          'rooms',
          t('channel.mobile.sync.roomRefresh'),
          roomsResponse.data.roomsError || t('channel.mobile.sync.roomRefreshPending'),
        ),
      )
    }

    const ratePlansResponse = await syncSuRatePlans(currentChannelView.id)
    if (!ratePlansResponse.success || !ratePlansResponse.data) {
      nextResults.push(
        buildDangerStage(
          'rate-plans',
          t('channel.mobile.sync.ratePlanRefresh'),
          ratePlansResponse.message || t('channel.mobile.sync.ratePlanRefreshFailed'),
        ),
      )
    } else if (ratePlansResponse.data.ratePlansSynced) {
      nextResults.push(
        buildSuccessStage(
          'rate-plans',
          t('channel.mobile.sync.ratePlanRefresh'),
          t('channel.mobile.sync.ratePlanRefreshResult', { count: ratePlansResponse.data.pricePlanCount }),
        ),
      )
    } else {
      nextResults.push(
        buildWarningStage(
          'rate-plans',
          t('channel.mobile.sync.ratePlanRefresh'),
          ratePlansResponse.data.ratePlansError || t('channel.mobile.sync.ratePlanRefreshPending'),
        ),
      )
    }

    const ariResponse = await syncSuAri(currentChannelView.id, days)
    if (!ariResponse.success || !ariResponse.data) {
      nextResults.push(
        buildDangerStage('ari', t('channel.mobile.sync.ariPush'), ariResponse.message || t('channel.mobile.sync.ariPushFailed')),
      )
    } else if (ariResponse.data.availabilityPushed || ariResponse.data.ratesPushed) {
      nextResults.push(
        buildSuccessStage(
          'ari',
          t('channel.mobile.sync.ariPush'),
          t('channel.mobile.sync.ariPushResult', {
            rooms: ariResponse.data.roomCount,
            ratePlans: ariResponse.data.ratePlanCount,
            days: ariResponse.data.days,
          }),
        ),
      )
    } else {
      nextResults.push(
        buildWarningStage('ari', t('channel.mobile.sync.ariPush'), ariResponse.data.error || t('channel.mobile.sync.ariPushPending')),
      )
    }

    stageResults.value = nextResults
    const hasFailure = nextResults.some((item) => item.statusColor !== 'success')
    if (hasFailure) {
      showWarningToast(t('channel.mobile.sync.fullRefreshReview'))
    } else {
      showSuccessToast(t('channel.mobile.sync.fullRefreshComplete'))
    }
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('channel.mobile.sync.fullRefreshFailed')))
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
      showWarningToast(resolveWarningMessage(error, t('channel.mobile.sync.refreshStatusFailed')))
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
      showWarningToast(resolveWarningMessage(error, t('channel.mobile.sync.actionFailed')))
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
  max-width: 100%;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  backdrop-filter: blur(10px);
  overflow-wrap: anywhere;
  text-align: center;
  white-space: normal;
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

.channel-sync-page__range-segment ion-segment-button::part(native),
.channel-sync-page__actions ion-button::part(native) {
  white-space: normal;
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
  max-width: 45%;
  overflow-wrap: anywhere;
  white-space: normal;
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
