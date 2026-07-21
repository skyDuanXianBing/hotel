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

    <ion-content fullscreen class="mobile-page channel-inventory-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('channel.mobile.refreshInventory')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="channelView" class="mobile-hero channel-inventory-hero">
        <h1 class="mobile-title">{{ channelView.name }}</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip channel-inventory-hero__chip channel-inventory-hero__chip--status">
            {{ channelView.statusLabel }}
          </span>
          <span class="mobile-chip channel-inventory-hero__chip channel-inventory-hero__chip--metric">
            {{ $t('channel.mobile.accounts', { count: mappingGroups.length }) }}
          </span>
          <span class="mobile-chip channel-inventory-hero__chip channel-inventory-hero__chip--metric">
            {{ $t('channel.mobile.dayRange', { count: syncDays }) }}
          </span>
        </div>
      </section>

      <div v-if="channelView" class="mobile-stack">
        <section class="mobile-card channel-inventory-page__toolbar-card">
          <div class="channel-inventory-page__toolbar-head">
            <div class="channel-inventory-page__toolbar-main">
              <strong>{{ inventoryOverviewText }}</strong>
              <p>{{ $t('channel.mobile.inventoryDescription') }}</p>
            </div>
            <div class="channel-inventory-page__toolbar-side">
              <ion-spinner v-if="loading || actionLoading || draftSubmitting" name="crescent" />
              <ion-button
                size="small"
                fill="outline"
                class="channel-inventory-page__action-secondary"
                @click="reloadInventory"
              >
                {{ $t('channel.mobile.common.refresh') }}
              </ion-button>
            </div>
          </div>

          <ion-segment
            class="channel-inventory-page__range-segment"
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

          <div class="channel-inventory-page__actions channel-inventory-page__actions--toolbar">
            <ion-button
              class="channel-inventory-page__action-primary"
              :disabled="!canRunInventorySync || actionLoading"
              @click="handleSyncInventory"
            >
              {{ actionLoading ? $t('order.options.loading') : $t('channel.mobile.inventory.sync') }}
            </ion-button>
          </div>

          <p v-if="inventoryCombinedNotice" class="mobile-note channel-inventory-page__warning">
            {{ inventoryCombinedNotice }}
          </p>

          <article
            v-if="lastSyncResult"
            :class="[
              'channel-inventory-page__result-card',
              `channel-inventory-page__result-card--${lastSyncResult.tone}`,
            ]"
          >
            <strong>{{ lastSyncResult.title }}</strong>
            <p>{{ lastSyncResult.message }}</p>
          </article>
        </section>

        <section class="mobile-card channel-inventory-page__list-card">
          <div class="channel-inventory-page__section-heading">
            <h2 class="mobile-section-title">{{ $t('channel.mobile.inventory.accountSummary') }}</h2>
            <p class="mobile-note">{{ inventoryGroupSummaryText }}</p>
          </div>

          <div v-if="mappingGroups.length > 0" class="channel-inventory-page__group-list">
            <article
              v-for="group in mappingGroups"
              :key="group.id"
              class="channel-inventory-page__group-card"
            >
              <div class="channel-inventory-page__group-header">
                <div>
                  <strong>{{ group.title }}</strong>
                  <p>{{ capability.groupLabel }} {{ $t('channel.mobile.common.identifier') }}: {{ group.hotelKey }}</p>
                </div>
                <ion-badge :color="group.statusColor">{{ group.statusLabel }}</ion-badge>
              </div>

              <div class="channel-inventory-page__group-meta">
                <span>{{ $t('channel.mobile.common.roomTypes') }} {{ group.roomIds.length }}</span>
                <span>{{ $t('channel.mobile.common.activeRatePlans') }} {{ group.activeRatePlanCount }}</span>
                <span>{{ $t('channel.mobile.common.totalRatePlans') }} {{ group.ratePlans.length }}</span>
              </div>

              <p v-if="group.roomIds.length > 0" class="channel-inventory-page__group-secondary">
                {{ $t('channel.mobile.common.roomTypeIdentifier') }}：{{ group.roomIds.join(' / ') }}
              </p>
            </article>
          </div>

          <p v-else class="mobile-note channel-inventory-page__empty">
            {{ $t('channel.mobile.inventory.groupEmpty') }}
          </p>
        </section>

        <section class="mobile-card channel-inventory-page__form-card">
          <div class="channel-inventory-page__section-heading">
            <h2 class="mobile-section-title">{{ $t('channel.mobile.inventory.draftTitle') }}</h2>
            <p class="mobile-note">{{ $t('channel.mobile.inventory.draftDescription') }}</p>
          </div>

          <div class="channel-inventory-page__form-grid">
            <label class="channel-inventory-page__field">
              <span>{{ $t('channel.mobile.inventory.advanceBooking') }}</span>
              <ion-select
                v-model="bookingSettingsForm.advanceBookingHours"
                fill="outline"
                interface="action-sheet"
              >
                <ion-select-option v-for="item in ADVANCE_BOOKING_HOURS" :key="item" :value="item">
                  {{ $t('channel.mobile.inventory.hours', { count: item }) }}
                </ion-select-option>
              </ion-select>
            </label>

            <div class="channel-inventory-page__toggle-field">
              <div>
                <strong>{{ $t('channel.mobile.inventory.approvalRequired') }}</strong>
                <p>{{ $t('channel.mobile.inventory.approvalDescription') }}</p>
              </div>
              <ion-toggle v-model="bookingSettingsForm.requireApproval" />
            </div>

            <label class="channel-inventory-page__field">
              <span>{{ $t('channel.mobile.inventory.preparationTime') }}</span>
              <ion-select
                v-model="bookingSettingsForm.preparationNights"
                fill="outline"
                interface="action-sheet"
              >
                <ion-select-option v-for="item in PREPARATION_NIGHTS" :key="item" :value="item">
                  {{
                    item === 0
                      ? $t('channel.mobile.inventory.none')
                      : $t('channel.mobile.inventory.nights', { count: item })
                  }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="channel-inventory-page__field">
              <span>{{ $t('channel.mobile.inventory.bookingWindow') }}</span>
              <ion-select
                v-model="bookingSettingsForm.bookingWindowDays"
                fill="outline"
                interface="action-sheet"
              >
                <ion-select-option v-for="item in BOOKING_WINDOW_DAYS" :key="item" :value="item">
                  {{ $t('channel.mobile.common.rangeDays', { count: item }) }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="channel-inventory-page__field">
              <span>{{ $t('channel.mobile.inventory.checkInStart') }}</span>
              <ion-select
                v-model="bookingSettingsForm.checkInStartTime"
                fill="outline"
                interface="action-sheet"
              >
                <ion-select-option v-for="item in TIME_OPTIONS" :key="item" :value="item">
                  {{ item }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="channel-inventory-page__field">
              <span>{{ $t('channel.mobile.inventory.checkInEnd') }}</span>
              <ion-select
                v-model="bookingSettingsForm.checkInEndTime"
                fill="outline"
                interface="action-sheet"
              >
                <ion-select-option value="">{{ $t('channel.mobile.inventory.unlimited') }}</ion-select-option>
                <ion-select-option v-for="item in TIME_OPTIONS" :key="item" :value="item">
                  {{ item }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="channel-inventory-page__field">
              <span>{{ $t('channel.mobile.inventory.checkOut') }}</span>
              <ion-select
                v-model="bookingSettingsForm.checkOutTime"
                fill="outline"
                interface="action-sheet"
              >
                <ion-select-option v-for="item in TIME_OPTIONS" :key="item" :value="item">
                  {{ item }}
                </ion-select-option>
              </ion-select>
            </label>
          </div>

          <div class="channel-inventory-page__actions">
            <ion-button fill="outline" :disabled="draftSubmitting" @click="handleResetDraft">
              {{ $t('channel.mobile.inventory.resetDraft') }}
            </ion-button>
            <ion-button :disabled="draftSubmitting" @click="handleSaveDraft">
              {{ draftSubmitting ? $t('channel.mobile.common.saving') : $t('channel.mobile.inventory.saveDraft') }}
            </ion-button>
          </div>
        </section>
      </div>

      <section v-else class="mobile-card">
        <p class="mobile-note">
          {{ loading ? $t('channel.mobile.inventory.loading') : $t('channel.mobile.common.notFound') }}
        </p>
      </section>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
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
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToggle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { buildChannelDetailPath } from '@/router/guards'
import {
  buildChannelViewModel,
  getChannelActionCapability,
  parseSuMappings,
  type SuHotelMappingView,
} from '@/components/channel/channelUtils'
import {
  getOtaIntegrationById,
  getSuMappings,
  syncSuAvailability,
  type OtaIntegrationDTO,
  type SuMappingsResponse,
} from '@/api/otaIntegration'
import { resolveChannelWarningMessage } from '@/utils/channelMessage'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

interface BookingSettingsDraft {
  advanceBookingHours: number
  requireApproval: boolean
  preparationNights: number
  bookingWindowDays: number
  checkInStartTime: string
  checkInEndTime: string
  checkOutTime: string
}

interface InventorySyncResult {
  title: string
  message: string
  tone: 'success' | 'warning' | 'danger'
}

const ADVANCE_BOOKING_HOURS = [1, 2, 3, 6, 12, 24, 48]
const PREPARATION_NIGHTS = [0, 1, 2, 3, 5, 7]
const BOOKING_WINDOW_DAYS = [30, 60, 90, 180, 365, 730]
const TIME_OPTIONS = Array.from({ length: 24 }, (_, index) => {
  return `${String(index).padStart(2, '0')}:00`
})
const INVENTORY_DRAFT_STORAGE_KEY = 'ios_airbnb_inventory_drafts_v1'

const route = useRoute()
const { locale, t } = useI18n()

const loading = ref(false)
const actionLoading = ref(false)
const draftSubmitting = ref(false)
const channel = ref<OtaIntegrationDTO | null>(null)
const mappingPayload = ref<SuMappingsResponse | null>(null)
const syncDays = ref('30')
const loadNotice = ref('')
const lastSyncResult = ref<InventorySyncResult | null>(null)
const bookingSettingsForm = ref<BookingSettingsDraft>(createDefaultBookingSettingsDraft())

const otaId = computed(() => Number(route.params.otaId || 0))
const channelView = computed(() => {
  if (!channel.value) {
    return null
  }
  return buildChannelViewModel(channel.value, null)
})
const capability = computed(() => getChannelActionCapability(channel.value?.code))
const pageTitle = computed(() => {
  if (!channelView.value) {
    return t('routes.ChannelInventory')
  }
  return `${channelView.value.name} · ${t('routes.ChannelInventory')}`
})
const mappingGroups = computed(() => {
  locale.value
  return parseSuMappings(mappingPayload.value, channelView.value?.suChannelId || undefined)
})
const canRunInventorySync = computed(() => {
  if (!channelView.value) {
    return false
  }
  if (!channelView.value.isConnected) {
    return false
  }
  return capability.value.supportsInventorySettings
})
const inventoryBlockedMessage = computed(() => {
  if (!channelView.value) {
    return t('channel.mobile.common.notFound')
  }
  if (!channelView.value.isConnected) {
    return t('channel.mobile.inventory.authorizeFirst')
  }
  if (!capability.value.supportsInventorySettings) {
    return t('channel.mobile.inventory.unsupported')
  }
  return ''
})
const inventoryRoomCount = computed(() => {
  return mappingGroups.value.reduce((total, group) => total + group.roomIds.length, 0)
})
const inventoryActiveRatePlanCount = computed(() => {
  return mappingGroups.value.reduce((total, group) => total + group.activeRatePlanCount, 0)
})
const inventoryOverviewText = computed(() => {
  if (!channelView.value) {
    return t('channel.mobile.inventory.pendingRefresh')
  }
  if (!capability.value.supportsInventorySettings) {
    return t('channel.mobile.inventory.unsupportedTitle')
  }
  if (!channelView.value.isConnected) {
    return t('channel.mobile.inventory.authorizeTitle')
  }
  if (mappingGroups.value.length === 0) {
    return t('channel.mobile.inventory.authorizedOverview', { days: syncDays.value })
  }
  return t('channel.mobile.inventory.overview', {
    accounts: mappingGroups.value.length,
    rooms: inventoryRoomCount.value,
    days: syncDays.value,
  })
})
const inventoryGroupSummaryText = computed(() => {
  if (!channelView.value) {
    return t('channel.mobile.inventory.summaryPending')
  }
  if (!capability.value.supportsInventorySettings) {
    return t('channel.mobile.inventory.summaryUnsupported')
  }
  if (!channelView.value.isConnected) {
    return t('channel.mobile.inventory.summaryAuthorize')
  }
  if (mappingGroups.value.length === 0) {
    return t('channel.mobile.inventory.summaryEmpty')
  }
  return t('channel.mobile.inventory.summary', {
    accounts: mappingGroups.value.length,
    rooms: inventoryRoomCount.value,
    ratePlans: inventoryActiveRatePlanCount.value,
  })
})
const inventoryCombinedNotice = computed(() => {
  return joinUniqueMessages([
    loadNotice.value,
    canRunInventorySync.value ? '' : inventoryBlockedMessage.value,
  ])
})

function createDefaultBookingSettingsDraft(): BookingSettingsDraft {
  return {
    advanceBookingHours: 2,
    requireApproval: true,
    preparationNights: 0,
    bookingWindowDays: 365,
    checkInStartTime: '16:00',
    checkInEndTime: '',
    checkOutTime: '10:00',
  }
}

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

function getDraftStorageKey() {
  return String(otaId.value)
}

function readAllDrafts() {
  const rawValue = localStorage.getItem(INVENTORY_DRAFT_STORAGE_KEY)
  if (!rawValue) {
    return {} as Record<string, BookingSettingsDraft>
  }

  try {
    const parsed = JSON.parse(rawValue)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return {} as Record<string, BookingSettingsDraft>
    }
    return parsed as Record<string, BookingSettingsDraft>
  } catch {
    return {} as Record<string, BookingSettingsDraft>
  }
}

function loadDraft() {
  const drafts = readAllDrafts()
  const nextDraft = drafts[getDraftStorageKey()]
  if (!nextDraft) {
    bookingSettingsForm.value = createDefaultBookingSettingsDraft()
    return
  }

  bookingSettingsForm.value = {
    ...createDefaultBookingSettingsDraft(),
    ...nextDraft,
  }
}

function saveDraftToStorage() {
  const drafts = readAllDrafts()
  drafts[getDraftStorageKey()] = {
    ...bookingSettingsForm.value,
  }
  localStorage.setItem(INVENTORY_DRAFT_STORAGE_KEY, JSON.stringify(drafts))
}

function removeDraftFromStorage() {
  const drafts = readAllDrafts()
  delete drafts[getDraftStorageKey()]
  localStorage.setItem(INVENTORY_DRAFT_STORAGE_KEY, JSON.stringify(drafts))
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
      throw new Error(detailResponse.message || t('channel.mobile.inventory.loadFailed'))
    }

    channel.value = detailResponse.data
    loadDraft()

    const nextCapability = getChannelActionCapability(detailResponse.data.code)
    if (!nextCapability.supportsInventorySettings) {
      mappingPayload.value = null
      loadNotice.value = t('channel.mobile.inventory.channelUnsupported')
      return
    }

    if (!channelView.value?.suChannelId) {
      mappingPayload.value = null
      loadNotice.value = t('channel.mobile.inventory.accountNotReady')
      return
    }

    const mappingsResponse = await getSuMappings(otaId.value, channelView.value.suChannelId)
    if (!mappingsResponse.success) {
      mappingPayload.value = null
      loadNotice.value = mappingsResponse.message || t('channel.mobile.inventory.summaryLoadFailed')
      return
    }

    mappingPayload.value = mappingsResponse.data || null
  } finally {
    loading.value = false
  }
}

function handleSyncDaysChange(event: CustomEvent) {
  syncDays.value = (event.detail.value as string) || '30'
}

async function handleSyncInventory() {
  if (!canRunInventorySync.value || !channelView.value) {
    showWarningToast(inventoryBlockedMessage.value || t('channel.mobile.inventory.unavailable'))
    return
  }

  actionLoading.value = true
  try {
    const response = await syncSuAvailability(channelView.value.id, Number(syncDays.value || 30))
    if (!response.success || !response.data) {
      throw new Error(response.message || t('channel.mobile.inventory.syncFailed'))
    }

    if (!response.data.availabilitySynced) {
      const message = response.data.availabilityError || t('channel.mobile.inventory.syncPending')
      lastSyncResult.value = {
        title: t('channel.mobile.inventory.syncPendingTitle'),
        message,
        tone: 'warning',
      }
      showWarningToast(message)
      return
    }

    lastSyncResult.value = {
      title: t('channel.mobile.inventory.syncComplete'),
      message: t('channel.mobile.inventory.syncResult', {
        rooms: response.data.roomCount,
        days: response.data.days,
      }),
      tone: 'success',
    }
    showSuccessToast(t('channel.mobile.inventory.airbnbSyncComplete'))
  } catch (error) {
    if (!isHandledRequestError(error)) {
      const message = resolveWarningMessage(error, t('channel.mobile.inventory.syncFailed'))
      lastSyncResult.value = {
        title: t('channel.mobile.inventory.syncFailed'),
        message,
        tone: 'danger',
      }
      showWarningToast(message)
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleSaveDraft() {
  draftSubmitting.value = true
  try {
    saveDraftToStorage()
    showSuccessToast(t('channel.mobile.inventory.draftSaved'))
  } finally {
    draftSubmitting.value = false
  }
}

function handleResetDraft() {
  bookingSettingsForm.value = createDefaultBookingSettingsDraft()
  removeDraftFromStorage()
  showSuccessToast(t('channel.mobile.inventory.draftReset'))
}

async function reloadInventory() {
  try {
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('channel.mobile.inventory.refreshFailed')))
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
})
</script>

<style scoped>
.channel-inventory-page {
  --background: linear-gradient(180deg, #eef3fb 0%, #f5f8fc 18%, #f9fbfd 100%);
}

.channel-inventory-hero {
  padding: 20px 20px 18px;
  border-color: rgba(110, 131, 171, 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(251, 253, 255, 0.86));
  box-shadow: 0 16px 28px rgba(22, 38, 70, 0.035);
}

.channel-inventory-hero::before {
  background: linear-gradient(135deg, rgba(58, 101, 173, 0.08), transparent 62%);
}

.channel-inventory-hero .mobile-title {
  font-size: 24px;
  letter-spacing: -0.04em;
}

.channel-inventory-hero .mobile-chip-row {
  gap: 10px;
  margin-top: 16px;
}

.channel-inventory-hero__chip {
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

.channel-inventory-hero__chip--status {
  border-color: rgba(64, 101, 181, 0.14);
  background: rgba(64, 101, 181, 0.08);
  color: #3558a6;
}

.channel-inventory-hero__chip--metric {
  border-color: rgba(117, 136, 173, 0.1);
  background: rgba(247, 249, 252, 0.94);
  color: #60708d;
}

.channel-inventory-page__toolbar-card,
.channel-inventory-page__list-card,
.channel-inventory-page__form-card {
  border-color: rgba(112, 130, 166, 0.1);
  box-shadow: 0 14px 26px rgba(22, 38, 70, 0.035);
}

.channel-inventory-page__toolbar-card,
.channel-inventory-page__form-card {
  position: relative;
  overflow: hidden;
}

.channel-inventory-page__toolbar-card {
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(249, 252, 255, 0.92));
}

.channel-inventory-page__toolbar-card::before,
.channel-inventory-page__form-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 52px;
  pointer-events: none;
}

.channel-inventory-page__toolbar-card::before {
  background: linear-gradient(90deg, rgba(68, 103, 180, 0.06), transparent 72%);
}

.channel-inventory-page__form-card::before {
  background: linear-gradient(90deg, rgba(83, 113, 173, 0.05), transparent 70%);
}

.channel-inventory-page__toolbar-card > *,
.channel-inventory-page__form-card > * {
  position: relative;
  z-index: 1;
}

.channel-inventory-page__toolbar-head,
.channel-inventory-page__toolbar-main,
.channel-inventory-page__group-list,
.channel-inventory-page__form-grid {
  display: grid;
  gap: 12px;
}

.channel-inventory-page__toolbar-head {
  gap: 14px;
}

.channel-inventory-page__toolbar-main strong,
.channel-inventory-page__group-card strong,
.channel-inventory-page__result-card strong,
.channel-inventory-page__toggle-field strong {
  margin: 0;
  color: var(--app-heading);
}

.channel-inventory-page__toolbar-main strong {
  display: block;
  font-size: 20px;
  letter-spacing: -0.03em;
}

.channel-inventory-page__toolbar-main p,
.channel-inventory-page__group-card p,
.channel-inventory-page__result-card p,
.channel-inventory-page__toggle-field p {
  margin: 6px 0 0;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.channel-inventory-page__toolbar-main p {
  color: var(--ios-pms-text-soft);
  font-size: 12px;
}

.channel-inventory-page__toolbar-side,
.channel-inventory-page__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.channel-inventory-page__toolbar-side {
  justify-content: flex-end;
}

.channel-inventory-page__toolbar-side ion-spinner {
  color: var(--ios-pms-primary);
  transform: scale(0.82);
}

.channel-inventory-page__range-segment {
  padding: 3px;
  border-color: rgba(111, 130, 168, 0.12);
  border-radius: 16px;
  background: rgba(236, 241, 249, 0.88);
}

.channel-inventory-page__range-segment ion-segment-button {
  --border-radius: 13px;
  --indicator-color: rgba(86, 116, 173, 0.16);
  min-height: 38px;
  font-size: 12px;
}

.channel-inventory-page__range-segment ion-segment-button::part(native),
.channel-inventory-page__actions ion-button::part(native) {
  white-space: normal;
}

.channel-inventory-page__action-primary {
  --background: #234ebd;
  --background-activated: #2046aa;
  --background-hover: #2046aa;
  --box-shadow: 0 10px 20px rgba(35, 78, 189, 0.18);
  --padding-start: 18px;
  --padding-end: 18px;
}

.channel-inventory-page__action-secondary {
  --background: rgba(255, 255, 255, 0.72);
  --border-color: rgba(71, 101, 163, 0.18);
  --color: #3f609b;
  --padding-start: 14px;
  --padding-end: 14px;
}

.channel-inventory-page__actions ion-button {
  min-height: 38px;
  margin: 0;
  --border-radius: 14px;
}

.channel-inventory-page__actions--toolbar ion-button {
  flex: 1 1 auto;
}

.channel-inventory-page__actions--toolbar {
  margin-top: 16px;
}

.channel-inventory-page__warning {
  margin-top: 10px;
  padding: 10px 12px 10px 14px;
  border-left: 3px solid #eb9629;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 248, 238, 0.98), rgba(255, 251, 246, 0.92));
  color: #b16d12;
  font-size: 12px;
  line-height: 1.55;
}

.channel-inventory-page__result-card {
  margin-top: 14px;
  padding: 14px 14px 13px;
  border-radius: 16px;
  border: 1px solid rgba(112, 130, 166, 0.11);
  box-shadow: none;
}

.channel-inventory-page__result-card--success {
  background: linear-gradient(180deg, rgba(241, 250, 246, 0.98), rgba(248, 252, 249, 0.94));
  border-color: rgba(61, 137, 102, 0.14);
}

.channel-inventory-page__result-card--warning {
  background: linear-gradient(180deg, rgba(255, 249, 240, 0.98), rgba(255, 252, 247, 0.94));
  border-color: rgba(222, 149, 44, 0.16);
}

.channel-inventory-page__result-card--danger {
  background: linear-gradient(180deg, rgba(255, 245, 245, 0.98), rgba(255, 250, 250, 0.94));
  border-color: rgba(213, 96, 96, 0.14);
}

.channel-inventory-page__section-heading {
  display: grid;
  gap: 4px;
}

.channel-inventory-page__section-heading .mobile-section-title {
  margin-bottom: 0;
}

.channel-inventory-page__group-list {
  gap: 12px;
  margin-top: 16px;
}

.channel-inventory-page__group-card {
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 254, 0.94));
  border: 1px solid rgba(112, 130, 166, 0.11);
  box-shadow: 0 12px 24px rgba(22, 38, 70, 0.035);
}

.channel-inventory-page__group-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.channel-inventory-page__group-header ion-badge {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  max-width: 45%;
  overflow-wrap: anywhere;
  white-space: normal;
}

.channel-inventory-page__group-header p {
  color: var(--ios-pms-text-soft);
  font-size: 12px;
}

.channel-inventory-page__group-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.channel-inventory-page__group-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(117, 136, 173, 0.09);
  border-radius: 999px;
  background: rgba(245, 248, 252, 0.92);
  color: #61718d;
  font-size: 11px;
  font-weight: 600;
  overflow-wrap: anywhere;
  text-align: center;
  white-space: normal;
}

.channel-inventory-page__group-secondary {
  color: var(--ios-pms-text-soft);
  font-size: 12px;
}

.channel-inventory-page__empty {
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

.channel-inventory-page__form-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(248, 251, 255, 0.92));
}

.channel-inventory-page__form-grid {
  gap: 12px;
  margin-top: 16px;
}

.channel-inventory-page__field,
.channel-inventory-page__toggle-field {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(112, 130, 166, 0.11);
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.96), rgba(246, 249, 253, 0.88));
}

.channel-inventory-page__field {
  display: grid;
  gap: 8px;
}

.channel-inventory-page__field span {
  color: var(--app-heading);
  font-size: 12px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.channel-inventory-page__toggle-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.channel-inventory-page__toggle-field p {
  color: var(--ios-pms-text-soft);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.channel-inventory-page__field ion-select::part(text) {
  overflow-wrap: anywhere;
  white-space: normal;
}

.channel-inventory-page__toggle-field ion-toggle {
  --track-background-checked: #234ebd;
  --handle-background-checked: #ffffff;
}

@media (min-width: 560px) {
  .channel-inventory-page__form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .channel-inventory-page__toggle-field {
    grid-column: span 2;
  }
}

@media (max-width: 480px) {
  .channel-inventory-page__toolbar-side {
    justify-content: flex-start;
  }

  .channel-inventory-page__actions ion-button {
    flex: 1 1 100%;
  }
}
</style>
