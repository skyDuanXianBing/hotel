<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="defaultBackHref" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ pageTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page channel-mapping-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新映射数据" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="channelView" class="mobile-hero channel-mapping-hero">
        <h1 class="mobile-title">{{ channelView.name }}</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip channel-mapping-hero__chip channel-mapping-hero__chip--status">
            {{ channelView.statusLabel }}
          </span>
          <span class="mobile-chip channel-mapping-hero__chip channel-mapping-hero__chip--metric">
            分组 {{ mappingGroups.length }}
          </span>
          <span class="mobile-chip channel-mapping-hero__chip channel-mapping-hero__chip--metric">
            价盘 {{ totalRatePlanCount }}
          </span>
        </div>
      </section>

      <div v-if="channelView" class="mobile-stack">
        <section class="mobile-card channel-mapping-page__toolbar-card">
          <div class="channel-mapping-page__toolbar">
            <div class="channel-mapping-page__toolbar-main">
              <strong>{{ mappingOverviewText }}</strong>
              <p>按{{ groupLabel }}分组查看并继续编辑</p>
            </div>
            <div class="channel-mapping-page__toolbar-actions">
              <ion-spinner v-if="loading" name="crescent" />
              <ion-button size="small" class="channel-mapping-page__action-primary" @click="openConnectEntry">
                编辑映射
              </ion-button>
              <ion-button
                size="small"
                fill="outline"
                class="channel-mapping-page__action-secondary"
                @click="reloadMapping"
              >
                刷新
              </ion-button>
            </div>
          </div>

          <div class="channel-mapping-page__filters">
            <ion-segment class="channel-mapping-page__filter-segment" :value="filterMode" @ionChange="handleFilterModeChange">
              <ion-segment-button value="all">
                <ion-label>全部</ion-label>
              </ion-segment-button>
              <ion-segment-button value="active">
                <ion-label>已映射</ion-label>
              </ion-segment-button>
              <ion-segment-button value="problem">
                <ion-label>待处理</ion-label>
              </ion-segment-button>
            </ion-segment>

            <ion-item v-if="hotelOptions.length > 1" class="channel-mapping-page__filter-item">
              <ion-select
                :value="selectedHotelKey"
                interface="action-sheet"
                :label="groupLabel"
                label-placement="stacked"
                @ionChange="handleHotelChange"
              >
                <ion-select-option value="all">全部分组</ion-select-option>
                <ion-select-option v-for="item in hotelOptions" :key="item" :value="item">
                  {{ item }}
                </ion-select-option>
                </ion-select>
            </ion-item>
          </div>

          <p v-if="combinedNotice" class="mobile-note channel-mapping-page__warning">{{ combinedNotice }}</p>
        </section>

        <section class="mobile-card channel-mapping-page__list-card">
          <div v-if="filteredGroups.length > 0" class="mobile-list channel-mapping-page__group-list">
            <article v-for="group in filteredGroups" :key="group.id" class="channel-mapping-page__group-card">
              <button
                type="button"
                class="channel-mapping-page__group-toggle"
                @click="toggleGroupExpansion(group.hotelKey)"
              >
                <div class="channel-mapping-page__group-header">
                  <div>
                    <h3>{{ group.title }}</h3>
                    <p>{{ groupLabel }}标识：{{ group.hotelKey }}</p>
                  </div>
                  <div class="channel-mapping-page__group-header-side">
                    <ion-badge :color="group.statusColor">{{ group.statusLabel }}</ion-badge>
                    <span
                      class="channel-mapping-page__group-arrow"
                      :class="{ 'channel-mapping-page__group-arrow--expanded': isGroupExpanded(group.hotelKey) }"
                    >
                      ›
                    </span>
                  </div>
                </div>

                <div class="channel-mapping-page__group-meta">
                  <span>房型 {{ group.roomIds.length }}</span>
                  <span>活跃价盘 {{ group.activeRatePlanCount }}</span>
                  <span>总价盘 {{ group.ratePlans.length }}</span>
                  <span
                    v-if="resolveDraftBadge(group.hotelKey)"
                    class="channel-mapping-page__draft-badge"
                  >
                    {{ resolveDraftBadge(group.hotelKey) }}
                  </span>
                </div>
              </button>

              <div class="channel-mapping-page__management-actions">
                <ion-button size="small" fill="outline" @click="toggleGroupExpansion(group.hotelKey)">
                  {{ isGroupExpanded(group.hotelKey) ? '收起明细' : '展开明细' }}
                </ion-button>
                <ion-button size="small" @click="openEditor(group)">深度编辑</ion-button>
              </div>

              <div v-if="isGroupExpanded(group.hotelKey)" class="channel-mapping-page__group-details">
                <p v-if="group.roomIds.length > 0" class="mobile-note channel-mapping-page__group-ids">
                  房型标识：{{ group.roomIds.join(' / ') }}
                </p>

                <div v-if="group.ratePlans.length > 0" class="channel-mapping-page__plans">
                  <div v-for="plan in group.ratePlans" :key="plan.id" class="channel-mapping-page__plan-item">
                    <div class="channel-mapping-page__plan-header">
                      <strong>{{ plan.title }}</strong>
                      <ion-badge :color="plan.statusColor">{{ plan.statusLabel }}</ion-badge>
                    </div>
                    <p>PMS：房型 {{ plan.pmsRoomId }} · 价盘 {{ plan.pmsRateId }}</p>
                    <p>渠道：房型 {{ plan.channelRoomId }} · 价盘 {{ plan.channelRateId }}</p>
                    <p class="channel-mapping-page__plan-secondary">价格模型 {{ plan.pricingModel }}</p>
                  </div>
                </div>

                <p v-else class="mobile-note">该分组暂无可展示的价盘映射。</p>
              </div>
            </article>
          </div>

          <p v-else class="mobile-note channel-mapping-page__empty">
            {{ emptyStateText }}
          </p>
        </section>
      </div>

      <section v-else class="mobile-card">
        <p class="mobile-note">{{ loading ? '映射数据加载中...' : '未找到该渠道配置。' }}</p>
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

    <ion-modal :is-open="editorOpen" @didDismiss="handleEditorDidDismiss">
      <ion-header translucent>
        <ion-toolbar>
          <ion-title>{{ editorGroup ? `${editorGroup.title} · 深度编辑` : '映射深编辑' }}</ion-title>
          <ion-buttons slot="end">
            <ion-button :disabled="editorSubmitting" @click="closeEditor">关闭</ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>

      <ion-content class="mobile-page channel-mapping-page__editor-page">
        <section class="mobile-card">
          <div v-if="editorGroup && editorState">
            <div class="channel-mapping-page__editor-hero">
              <strong>{{ editorGroup.title }}</strong>
              <p>先保存本地草稿，再按需打开向导完成最终提交。</p>
            </div>

            <section v-if="editorLoading" class="channel-mapping-page__editor-loading">
              <ion-spinner name="crescent" />
              <p>正在加载 PMS 房型与价盘选项...</p>
            </section>

            <div v-else class="channel-mapping-page__editor-stack">
              <label class="channel-mapping-page__editor-field">
                <span>PMS 房型</span>
                <ion-select
                  v-model="editorState.selectedPmsRoomType"
                  fill="outline"
                  interface="action-sheet"
                  placeholder="请选择 PMS 房型"
                >
                  <ion-select-option value="">暂不选择</ion-select-option>
                  <ion-select-option v-for="item in roomTypeOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </ion-select-option>
                </ion-select>
              </label>

              <div class="channel-mapping-page__editor-plan-list">
                <article
                  v-for="plan in editorState.plans"
                  :key="plan.id"
                  class="channel-mapping-page__editor-plan-item"
                >
                  <div class="channel-mapping-page__plan-header">
                    <strong>{{ plan.title }}</strong>
                    <ion-badge color="medium">{{ plan.channelRateId }}</ion-badge>
                  </div>
                  <p>渠道房型：{{ plan.channelRoomId }}</p>
                  <p>当前 PMS 价盘：{{ plan.currentPmsRatePlan || '未设置' }}</p>

                  <label class="channel-mapping-page__editor-field">
                    <span>目标 PMS 价盘</span>
                    <ion-select
                      v-model="plan.selectedPmsRatePlan"
                      fill="outline"
                      interface="action-sheet"
                      placeholder="请选择 PMS 价盘"
                    >
                      <ion-select-option value="">暂不选择</ion-select-option>
                      <ion-select-option
                        v-for="item in pricePlanOptions"
                        :key="item.value"
                        :value="item.value"
                      >
                        {{ item.label }}
                      </ion-select-option>
                    </ion-select>
                  </label>
                </article>
              </div>

              <div class="channel-mapping-page__editor-actions">
                <ion-button fill="outline" :disabled="editorSubmitting" @click="closeEditor">取消</ion-button>
                <ion-button fill="outline" :disabled="editorSubmitting" @click="handleSaveEditor(false)">
                  {{ editorSubmitting ? '保存中...' : '保存草稿' }}
                </ion-button>
                <ion-button :disabled="editorSubmitting" @click="handleSaveEditor(true)">
                  保存并打开向导
                </ion-button>
              </div>
            </div>
          </div>
        </section>
      </ion-content>
    </ion-modal>
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
  IonItem,
  IonLabel,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import ChannelConnectModal from '@/components/channel/ChannelConnectModal.vue'
import ChannelWidgetModal from '@/components/channel/ChannelWidgetModal.vue'
import {
  buildChannelViewModel,
  filterSuMappings,
  formatDateTime,
  getChannelGroupLabel,
  parseSuMappings,
  resolveSuChannelId,
  type SuHotelMappingView,
} from '@/components/channel/channelUtils'
import {
  getOtaIntegrationById,
  getSuMappingStatus,
  getSuMappings,
  type OtaIntegrationDTO,
  type SuMappingStatusSummary,
} from '@/api/otaIntegration'
import { getAllPricePlans, type PricePlanDTO } from '@/api/pricePlan'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { useUserStore } from '@/stores/user'
import {
  resolveChannelWarningMessage,
  resolveMappingStatusNotice,
  sanitizeChannelWarningMessage,
} from '@/utils/channelMessage'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

type MappingFilterMode = 'all' | 'active' | 'problem'

interface MappingOption {
  value: string
  label: string
}

interface MappingDraftPlanData {
  id: string
  selectedPmsRatePlan: string
  selectedPmsRatePlanLabel: string
}

interface MappingDraftData {
  selectedPmsRoomType: string
  selectedPmsRoomTypeLabel: string
  plans: MappingDraftPlanData[]
  savedAt: string
}

interface MappingEditorPlanState {
  id: string
  title: string
  channelRoomId: string
  channelRateId: string
  currentPmsRatePlan: string
  selectedPmsRatePlan: string
}

interface MappingEditorState {
  hotelKey: string
  selectedPmsRoomType: string
  plans: MappingEditorPlanState[]
}

const MAPPING_DRAFT_STORAGE_KEY = 'ios_channel_mapping_drafts_v1'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const channel = ref<OtaIntegrationDTO | null>(null)
const mappingStatus = ref<SuMappingStatusSummary | null>(null)
const mappingGroups = ref<SuHotelMappingView[]>([])
const filterMode = ref<MappingFilterMode>('all')
const selectedHotelKey = ref('all')
const expandedGroupKeys = ref<string[]>([])
const connectModalOpen = ref(false)
const widgetModalOpen = ref(false)
const loadNotice = ref('')
const editorOpen = ref(false)
const editorLoading = ref(false)
const editorSubmitting = ref(false)
const editorGroup = ref<SuHotelMappingView | null>(null)
const editorState = ref<MappingEditorState | null>(null)
const roomTypeOptions = ref<MappingOption[]>([])
const pricePlanOptions = ref<MappingOption[]>([])
const mappingDrafts = ref<Record<string, MappingDraftData>>({})

const otaId = computed(() => Number(route.params.otaId || 0))
const defaultBackHref = computed(() => `/tabs/channels/${otaId.value}`)

const channelView = computed(() => {
  if (!channel.value) {
    return null
  }
  return buildChannelViewModel(channel.value, mappingStatus.value)
})

const pageTitle = computed(() => {
  if (!channelView.value) {
    return '映射详情'
  }
  return `${channelView.value.name} · 映射`
})

const groupLabel = computed(() => getChannelGroupLabel(channel.value?.code))
const mappingStatusNotice = computed(() => resolveMappingStatusNotice(mappingStatus.value?.error))
const combinedNotice = computed(() => {
  return [mappingStatusNotice.value, loadNotice.value].filter(Boolean).join('；')
})

const hotelOptions = computed(() => {
  const result: string[] = []
  for (const group of mappingGroups.value) {
    result.push(group.hotelKey)
  }
  return result
})

const filteredGroups = computed(() => {
  return filterSuMappings(mappingGroups.value, selectedHotelKey.value, filterMode.value)
})

const totalRatePlanCount = computed(() => {
  let total = 0
  for (const group of mappingGroups.value) {
    total += group.ratePlans.length
  }
  return total
})

const mappingOverviewText = computed(() => {
  if (!mappingStatus.value) {
    if (mappingGroups.value.length > 0) {
      return `分组 ${mappingGroups.value.length} · 价盘 ${totalRatePlanCount.value}`
    }
    return '映射数据待刷新'
  }

  if (mappingStatus.value.error) {
    return '当前未映射或映射异常'
  }

  const mappedRoomCount = mappingStatus.value.mappedRoomIdCount || 0
  const activeRatePlanCount = mappingStatus.value.activeRatePlanCount || 0
  if (mappedRoomCount === 0 && activeRatePlanCount === 0) {
    return '当前未映射'
  }

  return `房型 ${mappedRoomCount} · 活跃价盘 ${activeRatePlanCount}`
})

const emptyStateText = computed(() => {
  if (!channelView.value) {
    return '暂无可展示的映射数据。'
  }
  if (!resolveSuChannelId(channelView.value.code)) {
    return '该渠道当前不支持映射明细。'
  }
  return '当前筛选下没有映射结果。'
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  return resolveChannelWarningMessage(error, fallbackMessage)
}

function normalizeSelectValue(value: string) {
  if (!value || value === '-') {
    return ''
  }
  return value
}

function getDraftKey(hotelKey: string) {
  return `${otaId.value}:${hotelKey}`
}

function readMappingDrafts() {
  const rawValue = localStorage.getItem(MAPPING_DRAFT_STORAGE_KEY)
  if (!rawValue) {
    return {} as Record<string, MappingDraftData>
  }

  try {
    const parsed = JSON.parse(rawValue)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return {} as Record<string, MappingDraftData>
    }
    return parsed as Record<string, MappingDraftData>
  } catch {
    return {} as Record<string, MappingDraftData>
  }
}

function persistMappingDrafts() {
  localStorage.setItem(MAPPING_DRAFT_STORAGE_KEY, JSON.stringify(mappingDrafts.value))
}

function ensureOption(options: MappingOption[], value: string, fallbackPrefix: string) {
  const normalizedValue = normalizeSelectValue(value)
  if (!normalizedValue) {
    return options
  }

  for (const item of options) {
    if (item.value === normalizedValue) {
      return options
    }
  }

  return [
    {
      value: normalizedValue,
      label: `${fallbackPrefix} ${normalizedValue}`,
    },
    ...options,
  ]
}

function getOptionLabel(options: MappingOption[], value: string, fallback: string) {
  for (const item of options) {
    if (item.value === value) {
      return item.label
    }
  }
  return fallback
}

function formatDraftBadgeTime(value: string) {
  const formatted = formatDateTime(value)
  if (formatted.length >= 16) {
    return formatted.slice(5)
  }
  return formatted
}

function resolveDraftBadge(hotelKey: string) {
  const draft = mappingDrafts.value[getDraftKey(hotelKey)]
  if (!draft) {
    return ''
  }

  return `有草稿 · ${formatDraftBadgeTime(draft.savedAt)}`
}

function createEditorState(group: SuHotelMappingView): MappingEditorState {
  const selectedPmsRoomType = group.ratePlans.length > 0 ? normalizeSelectValue(group.ratePlans[0].pmsRoomId) : ''
  const plans: MappingEditorPlanState[] = []

  for (const plan of group.ratePlans) {
    plans.push({
      id: plan.id,
      title: plan.title,
      channelRoomId: plan.channelRoomId,
      channelRateId: plan.channelRateId,
      currentPmsRatePlan: normalizeSelectValue(plan.pmsRateId),
      selectedPmsRatePlan: normalizeSelectValue(plan.pmsRateId),
    })
  }

  return {
    hotelKey: group.hotelKey,
    selectedPmsRoomType,
    plans,
  }
}

function applyDraftToEditorState(state: MappingEditorState) {
  const draft = mappingDrafts.value[getDraftKey(state.hotelKey)]
  if (!draft) {
    return state
  }

  const nextState: MappingEditorState = {
    ...state,
    selectedPmsRoomType: draft.selectedPmsRoomType || state.selectedPmsRoomType,
    plans: [],
  }

  for (const plan of state.plans) {
    const matchedDraft = draft.plans.find((item) => item.id === plan.id)
    nextState.plans.push({
      ...plan,
      selectedPmsRatePlan: matchedDraft?.selectedPmsRatePlan || plan.selectedPmsRatePlan,
    })
  }

  return nextState
}

async function ensureEditorOptions() {
  if (!editorState.value) {
    return
  }

  editorLoading.value = true

  try {
    let nextRoomTypeOptions = [...roomTypeOptions.value]
    let nextPricePlanOptions = [...pricePlanOptions.value]

    if (nextRoomTypeOptions.length === 0) {
      const roomTypeResponse = await getAllRoomTypes()
      if (!roomTypeResponse.success || !roomTypeResponse.data) {
        throw new Error(roomTypeResponse.message || '加载 PMS 房型失败')
      }

      nextRoomTypeOptions = roomTypeResponse.data.map((item: RoomTypeDTO) => {
        return {
          value: String(item.id),
          label: `${item.name} (${item.code})`,
        }
      })
    }

    if (nextPricePlanOptions.length === 0) {
      const userId = userStore.currentUser?.id
      if (!userId) {
        showWarningToast('未获取到当前用户信息，暂无法加载 PMS 价盘选项')
      } else {
        const pricePlanResponse = await getAllPricePlans(userId)
        if (!pricePlanResponse.success || !pricePlanResponse.data) {
          throw new Error(pricePlanResponse.message || '加载 PMS 价盘失败')
        }

        nextPricePlanOptions = pricePlanResponse.data.map((item: PricePlanDTO) => {
          return {
            value: String(item.id || ''),
            label: item.name,
          }
        })
      }
    }

    nextRoomTypeOptions = ensureOption(nextRoomTypeOptions, editorState.value.selectedPmsRoomType, '当前房型')
    for (const plan of editorState.value.plans) {
      nextPricePlanOptions = ensureOption(nextPricePlanOptions, plan.selectedPmsRatePlan, '当前价盘')
    }

    roomTypeOptions.value = nextRoomTypeOptions
    pricePlanOptions.value = nextPricePlanOptions
  } finally {
    editorLoading.value = false
  }
}

async function loadMappingPage() {
  if (!otaId.value) {
    return
  }

  loading.value = true
  loadNotice.value = ''
  mappingDrafts.value = readMappingDrafts()

  try {
    const detailResponse = await getOtaIntegrationById(otaId.value)
    if (!detailResponse.success || !detailResponse.data) {
      throw new Error(detailResponse.message || '加载渠道详情失败')
    }

    channel.value = detailResponse.data
    const suChannelId = resolveSuChannelId(detailResponse.data.code)
    if (!suChannelId) {
      mappingStatus.value = null
      mappingGroups.value = []
      expandedGroupKeys.value = []
      return
    }

    const [statusResult, mappingsResult] = await Promise.allSettled([
      getSuMappingStatus(otaId.value, suChannelId),
      getSuMappings(otaId.value, suChannelId),
    ] as const)

    if (statusResult.status === 'fulfilled' && statusResult.value.success) {
      mappingStatus.value = statusResult.value.data || null
    } else {
      mappingStatus.value = null
      loadNotice.value = resolveWarningMessage(
        statusResult.status === 'rejected' ? statusResult.reason : statusResult.value.message,
        '映射状态加载失败',
      )
    }

    if (mappingsResult.status === 'fulfilled' && mappingsResult.value.success) {
      mappingGroups.value = parseSuMappings(mappingsResult.value.data || null, suChannelId)
    } else {
      mappingGroups.value = []
      const nextWarning = resolveWarningMessage(
        mappingsResult.status === 'rejected' ? mappingsResult.reason : mappingsResult.value.message,
        '映射结果加载失败',
      )
      loadNotice.value = loadNotice.value ? `${loadNotice.value}；${nextWarning}` : nextWarning
    }

    if (selectedHotelKey.value !== 'all' && !mappingGroups.value.some((item) => item.hotelKey === selectedHotelKey.value)) {
      selectedHotelKey.value = 'all'
    }

    const availableHotelKeys = new Set(mappingGroups.value.map((item) => item.hotelKey))
    expandedGroupKeys.value = expandedGroupKeys.value.filter((item) => availableHotelKeys.has(item))
    if (expandedGroupKeys.value.length === 0 && mappingGroups.value.length === 1) {
      expandedGroupKeys.value = [mappingGroups.value[0].hotelKey]
    }
  } finally {
    loading.value = false
  }
}

function handleFilterModeChange(event: CustomEvent) {
  const nextMode = event.detail.value as MappingFilterMode
  filterMode.value = nextMode || 'all'
}

function handleHotelChange(event: CustomEvent) {
  selectedHotelKey.value = (event.detail.value as string) || 'all'
  if (selectedHotelKey.value !== 'all') {
    expandedGroupKeys.value = Array.from(new Set([...expandedGroupKeys.value, selectedHotelKey.value]))
  }
}

function isGroupExpanded(hotelKey: string) {
  return expandedGroupKeys.value.includes(hotelKey)
}

function toggleGroupExpansion(hotelKey: string) {
  if (isGroupExpanded(hotelKey)) {
    expandedGroupKeys.value = expandedGroupKeys.value.filter((item) => item !== hotelKey)
    return
  }

  expandedGroupKeys.value = [...expandedGroupKeys.value, hotelKey]
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
  void loadMappingPage()
}

function handleWidgetError(message: string) {
  loadNotice.value = sanitizeChannelWarningMessage(message, '映射操作异常，请稍后重试')
}

async function openEditor(group: SuHotelMappingView) {
  editorGroup.value = group
  editorState.value = applyDraftToEditorState(createEditorState(group))
  editorOpen.value = true

  try {
    await ensureEditorOptions()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载编辑选项失败'))
    }
  }
}

function closeEditor() {
  if (editorSubmitting.value) {
    return
  }

  editorOpen.value = false
  editorGroup.value = null
  editorState.value = null
}

function handleEditorDidDismiss() {
  if (editorOpen.value) {
    return
  }

  editorGroup.value = null
  editorState.value = null
}

async function handleSaveEditor(openWidgetAfterSave: boolean) {
  if (!editorState.value || !editorGroup.value) {
    return
  }

  let selectedPlanCount = 0
  for (const plan of editorState.value.plans) {
    if (plan.selectedPmsRatePlan) {
      selectedPlanCount += 1
    }
  }

  if (!editorState.value.selectedPmsRoomType && selectedPlanCount === 0) {
    showWarningToast('请至少选择一个 PMS 房型或价盘后再保存草稿')
    return
  }

  editorSubmitting.value = true

  try {
    const draftKey = getDraftKey(editorState.value.hotelKey)
    const plans: MappingDraftPlanData[] = []
    for (const plan of editorState.value.plans) {
      plans.push({
        id: plan.id,
        selectedPmsRatePlan: plan.selectedPmsRatePlan,
        selectedPmsRatePlanLabel: getOptionLabel(pricePlanOptions.value, plan.selectedPmsRatePlan, plan.selectedPmsRatePlan),
      })
    }

    mappingDrafts.value[draftKey] = {
      selectedPmsRoomType: editorState.value.selectedPmsRoomType,
      selectedPmsRoomTypeLabel: getOptionLabel(
        roomTypeOptions.value,
        editorState.value.selectedPmsRoomType,
        editorState.value.selectedPmsRoomType,
      ),
      plans,
      savedAt: new Date().toISOString(),
    }
    persistMappingDrafts()

    if (openWidgetAfterSave) {
      showSuccessToast('映射草稿已保存，正在打开向导继续提交')
      closeEditor()
      widgetModalOpen.value = true
      return
    }

    showSuccessToast('映射草稿已保存；当前后端仍需通过向导完成最终提交')
    closeEditor()
  } finally {
    editorSubmitting.value = false
  }
}

async function reloadMapping() {
  try {
    await loadMappingPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '刷新映射失败'))
    }
  }
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadMappingPage()
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  await loadMappingPage()
})
</script>

<style scoped>
.channel-mapping-page {
  --background: linear-gradient(180deg, #eef3fb 0%, #f4f7fc 18%, #f8fafd 100%);
}

.channel-mapping-hero {
  padding: 20px 20px 18px;
  border-color: rgba(110, 131, 171, 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(251, 253, 255, 0.84));
  box-shadow: 0 16px 28px rgba(22, 38, 70, 0.035);
}

.channel-mapping-hero::before {
  background: linear-gradient(135deg, rgba(62, 98, 175, 0.08), transparent 62%);
}

.channel-mapping-hero .mobile-title {
  font-size: 24px;
  letter-spacing: -0.04em;
}

.channel-mapping-hero .mobile-chip-row {
  gap: 10px;
  margin-top: 16px;
}

.channel-mapping-hero__chip {
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  backdrop-filter: blur(10px);
}

.channel-mapping-hero__chip--status {
  border-color: rgba(64, 101, 181, 0.14);
  background: rgba(64, 101, 181, 0.08);
  color: #3558a6;
}

.channel-mapping-hero__chip--metric {
  border-color: rgba(117, 136, 173, 0.1);
  background: rgba(247, 249, 252, 0.94);
  color: #60708d;
}

.channel-mapping-page__toolbar-card,
.channel-mapping-page__list-card {
  border-color: rgba(112, 130, 166, 0.1);
  box-shadow: 0 14px 26px rgba(22, 38, 70, 0.035);
}

.channel-mapping-page__toolbar-card {
  position: relative;
  overflow: hidden;
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(249, 252, 255, 0.92));
}

.channel-mapping-page__toolbar-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 52px;
  background: linear-gradient(90deg, rgba(68, 103, 180, 0.06), transparent 72%);
  pointer-events: none;
}

.channel-mapping-page__toolbar-card > * {
  position: relative;
  z-index: 1;
}

.channel-mapping-page__toolbar,
.channel-mapping-page__filters,
.channel-mapping-page__group-list,
.channel-mapping-page__plans,
.channel-mapping-page__editor-stack,
.channel-mapping-page__editor-plan-list {
  display: grid;
  gap: 12px;
}

.channel-mapping-page__toolbar {
  gap: 14px;
}

.channel-mapping-page__toolbar-main strong,
.channel-mapping-page__group-header h3,
.channel-mapping-page__plan-item strong,
.channel-mapping-page__editor-plan-item strong,
.channel-mapping-page__editor-hero strong {
  margin: 0;
  color: var(--app-heading);
}

.channel-mapping-page__toolbar-main strong {
  display: block;
  font-size: 20px;
  letter-spacing: -0.03em;
}

.channel-mapping-page__toolbar-main p,
.channel-mapping-page__group-header p,
.channel-mapping-page__plan-item p,
.channel-mapping-page__editor-plan-item p,
.channel-mapping-page__editor-hero p,
.channel-mapping-page__group-ids {
  margin: 6px 0 0;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.channel-mapping-page__toolbar-main p {
  color: var(--ios-pms-text-soft);
  font-size: 12px;
}

.channel-mapping-page__toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.channel-mapping-page__toolbar-actions ion-spinner {
  color: var(--ios-pms-primary);
  transform: scale(0.82);
}

.channel-mapping-page__action-primary {
  --background: #234ebd;
  --background-activated: #2046aa;
  --background-hover: #2046aa;
  --box-shadow: 0 10px 20px rgba(35, 78, 189, 0.18);
  --padding-start: 16px;
  --padding-end: 16px;
}

.channel-mapping-page__action-secondary {
  --background: rgba(255, 255, 255, 0.72);
  --border-color: rgba(71, 101, 163, 0.18);
  --color: #3f609b;
  --padding-start: 14px;
  --padding-end: 14px;
}

.channel-mapping-page__filters {
  gap: 10px;
  margin-top: 4px;
}

.channel-mapping-page__filter-segment {
  padding: 3px;
  border-color: rgba(111, 130, 168, 0.12);
  border-radius: 16px;
  background: rgba(236, 241, 249, 0.88);
}

.channel-mapping-page__filter-segment ion-segment-button {
  --border-radius: 13px;
  --indicator-color: rgba(86, 116, 173, 0.16);
  min-height: 38px;
  font-size: 12px;
}

.channel-mapping-page__filter-item {
  margin: 0;
  border: 1px solid rgba(112, 130, 166, 0.11);
  border-radius: 16px;
  background: rgba(252, 253, 255, 0.84);
  --padding-start: 4px;
  --inner-padding-end: 4px;
}

.channel-mapping-page__warning {
  margin-top: 10px;
  padding: 10px 12px 10px 14px;
  border-left: 3px solid #eb9629;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 248, 238, 0.98), rgba(255, 251, 246, 0.92));
  color: #b16d12;
  font-size: 12px;
  line-height: 1.55;
}

.channel-mapping-page__group-card,
.channel-mapping-page__plan-item,
.channel-mapping-page__editor-plan-item {
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 254, 0.94));
  border: 1px solid rgba(112, 130, 166, 0.11);
  box-shadow: 0 12px 24px rgba(22, 38, 70, 0.035);
}

.channel-mapping-page__group-toggle {
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
}

.channel-mapping-page__group-header,
.channel-mapping-page__plan-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.channel-mapping-page__group-header-side {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.channel-mapping-page__group-arrow {
  color: var(--ios-pms-text-disabled);
  font-size: 20px;
  line-height: 1;
  transition: transform 0.18s ease;
}

.channel-mapping-page__group-header ion-badge,
.channel-mapping-page__plan-header ion-badge {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.channel-mapping-page__group-header h3 {
  font-size: 16px;
  letter-spacing: -0.02em;
}

.channel-mapping-page__group-header p {
  color: var(--ios-pms-text-soft);
  font-size: 12px;
}

.channel-mapping-page__group-arrow--expanded {
  transform: rotate(90deg);
}

.channel-mapping-page__group-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.channel-mapping-page__group-meta span {
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
}

.channel-mapping-page__draft-badge {
  border-color: rgba(52, 116, 246, 0.14);
  background: rgba(52, 116, 246, 0.09);
  color: #315fc1;
  font-weight: 600;
}

.channel-mapping-page__group-details,
.channel-mapping-page__management-actions {
  margin-top: 12px;
}

.channel-mapping-page__group-details {
  padding-top: 14px;
  border-top: 1px solid rgba(113, 132, 170, 0.11);
}

.channel-mapping-page__management-actions,
.channel-mapping-page__editor-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.channel-mapping-page__management-actions ion-button,
.channel-mapping-page__editor-actions ion-button {
  min-height: 34px;
  --border-radius: 12px;
  font-size: 12px;
}

.channel-mapping-page__group-ids {
  color: var(--ios-pms-text-soft);
  font-size: 12px;
}

.channel-mapping-page__plans {
  gap: 10px;
  margin-top: 10px;
}

.channel-mapping-page__plan-item {
  padding: 14px 14px 13px;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(248, 250, 253, 0.96), rgba(244, 247, 252, 0.88));
  border-color: rgba(114, 132, 170, 0.08);
  box-shadow: none;
}

.channel-mapping-page__plan-item strong {
  font-size: 14px;
}

.channel-mapping-page__plan-item p {
  font-size: 12px;
  line-height: 1.55;
}

.channel-mapping-page__plan-secondary {
  color: var(--ios-pms-text-soft);
  font-size: 11px;
}

.channel-mapping-page__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  padding: 28px 18px;
  border: 1px dashed rgba(117, 136, 173, 0.16);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.9), rgba(246, 249, 253, 0.76));
  color: var(--ios-pms-text-soft);
  text-align: center;
}

.channel-mapping-page__editor-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.channel-mapping-page__editor-hero {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid rgba(112, 130, 166, 0.1);
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.98), rgba(241, 246, 255, 0.9));
}

.channel-mapping-page__editor-loading {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.channel-mapping-page__editor-loading p,
.channel-mapping-page__editor-field span {
  color: var(--app-muted);
}

.channel-mapping-page__editor-field {
  display: grid;
  gap: 8px;
}

.channel-mapping-page__editor-field span {
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 480px) {
  .channel-mapping-page__toolbar-actions {
    justify-content: flex-start;
  }
}
</style>
