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

    <ion-content fullscreen class="mobile-page channel-inventory-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新房量设置" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="channelView" class="mobile-hero channel-inventory-hero">
        <p class="mobile-note channel-inventory-hero__eyebrow">Airbnb 房量设置</p>
        <h1 class="mobile-title">{{ channelView.name }}</h1>
        <p class="mobile-subtitle">可在手机上完成房量同步，并先整理常用预订设置。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ channelView.statusLabel }}</span>
          <span class="mobile-chip">账号 {{ mappingGroups.length }}</span>
          <span class="mobile-chip">范围 {{ syncDays }} 天</span>
        </div>
      </section>

      <div v-if="channelView" class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">当前能力</h2>
              <p class="mobile-note">当前支持房量同步，预订设置可先保存为草稿，方便稍后继续整理。</p>
            </div>
            <ion-spinner v-if="loading || actionLoading || draftSubmitting" name="crescent" />
          </div>

          <ul class="mobile-bullet-list">
            <li>已支持：按 30 / 90 / 365 天范围执行房量同步。</li>
            <li>已支持：在移动端保存预订设置草稿，避免临时输入丢失。</li>
            <li>预订设置确认后，可按页面指引继续完成后续配置。</li>
          </ul>

          <p v-if="loadNotice" class="mobile-note channel-inventory-page__warning">{{ loadNotice }}</p>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">房量同步</h2>

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

          <div class="channel-inventory-page__actions">
            <ion-button :disabled="!canRunInventorySync || actionLoading" @click="handleSyncInventory">
              {{ actionLoading ? '同步中...' : '同步房量' }}
            </ion-button>
          </div>

          <p v-if="!canRunInventorySync" class="mobile-note channel-inventory-page__warning">
            {{ inventoryBlockedMessage }}
          </p>

          <article v-if="lastSyncResult" class="channel-inventory-page__result-card">
            <strong>{{ lastSyncResult.title }}</strong>
            <p>{{ lastSyncResult.message }}</p>
          </article>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">账号 / 房型摘要</h2>
          <div v-if="mappingGroups.length > 0" class="channel-inventory-page__group-list">
            <article
              v-for="group in mappingGroups"
              :key="group.id"
              class="channel-inventory-page__group-card"
            >
              <div class="channel-inventory-page__group-header">
                <div>
                  <strong>{{ group.title }}</strong>
                  <p>{{ group.roomIds.length > 0 ? group.roomIds.join(' / ') : '暂无房型标识' }}</p>
                </div>
                <ion-badge :color="group.statusColor">{{ group.statusLabel }}</ion-badge>
              </div>
              <p>活跃价盘 {{ group.activeRatePlanCount }} / 总价盘 {{ group.ratePlans.length }}</p>
            </article>
          </div>
          <p v-else class="mobile-note">当前暂无可展示的 Airbnb 映射摘要。</p>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">预订设置草稿</h2>
          <p class="mobile-note">可先整理常用预订规则，保存草稿后方便稍后继续调整。</p>

          <div class="channel-inventory-page__form-grid">
            <label class="channel-inventory-page__field">
              <span>提前预订量</span>
              <ion-select
                v-model="bookingSettingsForm.advanceBookingHours"
                fill="outline"
                interface="action-sheet"
              >
                <ion-select-option v-for="item in ADVANCE_BOOKING_HOURS" :key="item" :value="item">
                  {{ item }} 小时
                </ion-select-option>
              </ion-select>
            </label>

            <div class="channel-inventory-page__toggle-field">
              <div>
                <strong>需人工确认</strong>
                <p>未在可直接接待时间内完成的订单可转为预订申请。</p>
              </div>
              <ion-toggle v-model="bookingSettingsForm.requireApproval" />
            </div>

            <label class="channel-inventory-page__field">
              <span>准备时间</span>
              <ion-select
                v-model="bookingSettingsForm.preparationNights"
                fill="outline"
                interface="action-sheet"
              >
                <ion-select-option v-for="item in PREPARATION_NIGHTS" :key="item" :value="item">
                  {{ item === 0 ? '无' : `${item} 晚` }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="channel-inventory-page__field">
              <span>预订开放期</span>
              <ion-select
                v-model="bookingSettingsForm.bookingWindowDays"
                fill="outline"
                interface="action-sheet"
              >
                <ion-select-option v-for="item in BOOKING_WINDOW_DAYS" :key="item" :value="item">
                  {{ item }} 天
                </ion-select-option>
              </ion-select>
            </label>

            <label class="channel-inventory-page__field">
              <span>入住开始时间</span>
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
              <span>入住结束时间</span>
              <ion-select
                v-model="bookingSettingsForm.checkInEndTime"
                fill="outline"
                interface="action-sheet"
              >
                <ion-select-option value="">不限</ion-select-option>
                <ion-select-option v-for="item in TIME_OPTIONS" :key="item" :value="item">
                  {{ item }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="channel-inventory-page__field">
              <span>离店时间</span>
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
              重置草稿
            </ion-button>
            <ion-button :disabled="draftSubmitting" @click="handleSaveDraft">
              {{ draftSubmitting ? '保存中...' : '保存本地草稿' }}
            </ion-button>
          </div>
        </section>
      </div>

      <section v-else class="mobile-card">
        <p class="mobile-note">{{ loading ? '房量设置加载中...' : '未找到该渠道配置。' }}</p>
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
} from '@/api/otaIntegration'
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
}

const ADVANCE_BOOKING_HOURS = [1, 2, 3, 6, 12, 24, 48]
const PREPARATION_NIGHTS = [0, 1, 2, 3, 5, 7]
const BOOKING_WINDOW_DAYS = [30, 60, 90, 180, 365, 730]
const TIME_OPTIONS = Array.from({ length: 24 }, (_, index) => {
  return `${String(index).padStart(2, '0')}:00`
})
const INVENTORY_DRAFT_STORAGE_KEY = 'ios_airbnb_inventory_drafts_v1'

const route = useRoute()

const loading = ref(false)
const actionLoading = ref(false)
const draftSubmitting = ref(false)
const channel = ref<OtaIntegrationDTO | null>(null)
const mappingGroups = ref<SuHotelMappingView[]>([])
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
    return '房量设置'
  }
  return `${channelView.value.name} · 房量设置`
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
    return '未找到该渠道配置。'
  }
  if (!channelView.value.isConnected) {
    return '请先完成 Airbnb 授权，再执行房量同步。'
  }
  if (!capability.value.supportsInventorySettings) {
    return '当前渠道不是 Airbnb，移动端不展示房量设置。'
  }
  return ''
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
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
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
      throw new Error(detailResponse.message || '加载房量设置失败')
    }

    channel.value = detailResponse.data
    loadDraft()

    const nextCapability = getChannelActionCapability(detailResponse.data.code)
    if (!nextCapability.supportsInventorySettings) {
      mappingGroups.value = []
      loadNotice.value = '当前渠道暂不提供房量设置。'
      return
    }

    if (!channelView.value?.suChannelId) {
      mappingGroups.value = []
      loadNotice.value = '当前账号信息尚未准备完成，暂时无法展示账号摘要。'
      return
    }

    const mappingsResponse = await getSuMappings(otaId.value, channelView.value.suChannelId)
    if (!mappingsResponse.success) {
      mappingGroups.value = []
      loadNotice.value = mappingsResponse.message || '账号摘要加载失败'
      return
    }

    mappingGroups.value = parseSuMappings(mappingsResponse.data || null, channelView.value.suChannelId)
  } finally {
    loading.value = false
  }
}

function handleSyncDaysChange(event: CustomEvent) {
  syncDays.value = (event.detail.value as string) || '30'
}

async function handleSyncInventory() {
  if (!canRunInventorySync.value || !channelView.value) {
    showWarningToast(inventoryBlockedMessage.value || '当前无法执行房量同步')
    return
  }

  actionLoading.value = true
  try {
    const response = await syncSuAvailability(channelView.value.id, Number(syncDays.value || 30))
    if (!response.success || !response.data) {
      throw new Error(response.message || '房量同步失败')
    }

    if (!response.data.availabilitySynced) {
      const message = response.data.availabilityError || '房量同步未完成'
      lastSyncResult.value = {
        title: '房量同步待处理',
        message,
      }
      showWarningToast(message)
      return
    }

    lastSyncResult.value = {
      title: '房量同步成功',
      message: `已同步 ${response.data.roomCount} 个房型，范围 ${response.data.days} 天`,
    }
    showSuccessToast('Airbnb 房量同步完成')
  } catch (error) {
    if (!isHandledRequestError(error)) {
      const message = resolveWarningMessage(error, '房量同步失败')
      lastSyncResult.value = {
        title: '房量同步失败',
        message,
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
    showSuccessToast('预订设置草稿已保存，可稍后继续完善')
  } finally {
    draftSubmitting.value = false
  }
}

function handleResetDraft() {
  bookingSettingsForm.value = createDefaultBookingSettingsDraft()
  removeDraftFromStorage()
  showSuccessToast('已重置为默认草稿')
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
.channel-inventory-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.channel-inventory-page__warning {
  margin-top: 12px;
  color: var(--ion-color-warning);
}

.channel-inventory-page__actions,
.channel-inventory-page__group-list,
.channel-inventory-page__form-grid {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.channel-inventory-page__group-card,
.channel-inventory-page__result-card {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--app-border);
}

.channel-inventory-page__group-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.channel-inventory-page__group-card strong,
.channel-inventory-page__group-card p,
.channel-inventory-page__result-card strong,
.channel-inventory-page__result-card p,
.channel-inventory-page__toggle-field strong,
.channel-inventory-page__toggle-field p {
  margin: 0;
}

.channel-inventory-page__group-card p,
.channel-inventory-page__result-card p,
.channel-inventory-page__toggle-field p {
  margin-top: 8px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.channel-inventory-page__field {
  display: grid;
  gap: 8px;
}

.channel-inventory-page__field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.channel-inventory-page__toggle-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}
</style>
