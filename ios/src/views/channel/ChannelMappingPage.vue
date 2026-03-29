<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="defaultBackHref" />
        </ion-buttons>
        <ion-title>{{ pageTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page channel-mapping-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新映射数据" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="channelView" class="mobile-hero channel-mapping-hero">
        <p class="mobile-note channel-mapping-hero__eyebrow">映射管理</p>
        <h1 class="mobile-title">{{ channelView.name }}</h1>
        <p class="mobile-subtitle">已补齐 {{ groupLabel }} 管理层，并支持用移动端深编辑草稿整理 PMS 房型与价盘选择。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ channelView.statusLabel }}</span>
          <span class="mobile-chip">分组 {{ mappingGroups.length }}</span>
          <span class="mobile-chip">价盘 {{ totalRatePlanCount }}</span>
        </div>
      </section>

      <div v-if="channelView" class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">映射摘要</h2>
              <p class="mobile-note">房型 {{ mappingStatus?.mappedRoomIdCount || 0 }}，活跃价盘 {{ mappingStatus?.activeRatePlanCount || 0 }}</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div class="channel-mapping-page__actions">
            <ion-button @click="openConnectEntry">编辑映射</ion-button>
            <ion-button fill="outline" @click="reloadMapping">刷新映射</ion-button>
          </div>

          <p v-if="mappingStatusNotice" class="mobile-note channel-mapping-page__warning">{{ mappingStatusNotice }}</p>
          <p v-if="loadNotice" class="mobile-note channel-mapping-page__warning">{{ loadNotice }}</p>
        </section>

        <section class="mobile-card">
          <ion-segment :value="activeSegment" @ionChange="handleActiveSegmentChange">
            <ion-segment-button value="management">
              <ion-label>{{ groupLabel }}管理</ion-label>
            </ion-segment-button>
            <ion-segment-button value="details">
              <ion-label>映射明细</ion-label>
            </ion-segment-button>
          </ion-segment>
        </section>

        <section class="mobile-card">
          <div class="channel-mapping-page__filters">
            <ion-segment :value="filterMode" @ionChange="handleFilterModeChange">
              <ion-segment-button value="all">
                <ion-label>全部</ion-label>
              </ion-segment-button>
              <ion-segment-button value="active">
                <ion-label>已连接</ion-label>
              </ion-segment-button>
              <ion-segment-button value="problem">
                <ion-label>待处理</ion-label>
              </ion-segment-button>
            </ion-segment>

            <ion-item>
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
        </section>

        <section v-if="activeSegment === 'management'" class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">{{ groupLabel }}管理层</h2>
              <p class="mobile-note">先看分组状态，再按分组进入深编辑，避免在长表里来回查找。</p>
            </div>
          </div>

          <div v-if="filteredGroups.length > 0" class="mobile-list channel-mapping-page__group-list">
            <article v-for="group in filteredGroups" :key="group.id" class="channel-mapping-page__group-card">
              <div class="channel-mapping-page__group-header">
                <div>
                  <h3>{{ group.title }}</h3>
                  <p>{{ groupLabel }}标识：{{ group.hotelKey }}</p>
                </div>
                <ion-badge :color="group.statusColor">{{ group.statusLabel }}</ion-badge>
              </div>

              <div class="channel-mapping-page__group-meta">
                <span>房型标识 {{ group.roomIds.length > 0 ? group.roomIds.join(' / ') : '暂无' }}</span>
                <span>活跃价盘 {{ group.activeRatePlanCount }}</span>
                <span>总价盘 {{ group.ratePlans.length }}</span>
              </div>

              <p v-if="resolveDraftSummary(group.hotelKey)" class="mobile-note channel-mapping-page__draft-text">
                {{ resolveDraftSummary(group.hotelKey) }}
              </p>

              <div class="channel-mapping-page__management-actions">
                <ion-button size="small" fill="outline" @click="viewGroupDetails(group)">
                  查看明细
                </ion-button>
                <ion-button size="small" @click="openEditor(group)">深度编辑</ion-button>
              </div>
            </article>
          </div>

          <p v-else class="mobile-note channel-mapping-page__empty">
            {{ emptyStateText }}
          </p>
        </section>

        <section v-else class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">映射列表</h2>
              <p class="mobile-note">展示 PMS 房型 / 价盘与渠道房型 / 价盘的当前映射关系。</p>
            </div>
          </div>

          <div v-if="filteredGroups.length > 0" class="mobile-list channel-mapping-page__group-list">
            <article v-for="group in filteredGroups" :key="group.id" class="channel-mapping-page__group-card">
              <div class="channel-mapping-page__group-header">
                <div>
                  <h3>{{ group.title }}</h3>
                  <p>房型标识：{{ group.roomIds.length > 0 ? group.roomIds.join(' / ') : '暂无' }}</p>
                </div>
                <ion-badge :color="group.statusColor">{{ group.statusLabel }}</ion-badge>
              </div>

              <div v-if="group.ratePlans.length > 0" class="channel-mapping-page__plans">
                <div v-for="plan in group.ratePlans" :key="plan.id" class="channel-mapping-page__plan-item">
                  <div class="channel-mapping-page__plan-header">
                    <strong>{{ plan.title }}</strong>
                    <ion-badge :color="plan.statusColor">{{ plan.statusLabel }}</ion-badge>
                  </div>
                  <p>PMS 房型：{{ plan.pmsRoomId }} · PMS 价盘：{{ plan.pmsRateId }}</p>
                  <p>渠道房型：{{ plan.channelRoomId }} · 渠道价盘：{{ plan.channelRateId }}</p>
                  <p>价格模型：{{ plan.pricingModel }}</p>
                </div>
              </div>

              <p v-else class="mobile-note">该分组暂无可展示的价盘映射。</p>
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
              <p>当前后端未开放独立保存接口。移动端会先保存本地草稿，并可继续打开 Su 向导完成最终提交。</p>
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
type MappingSegment = 'management' | 'details'

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
const activeSegment = ref<MappingSegment>('management')
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

const emptyStateText = computed(() => {
  if (!channelView.value) {
    return '暂无可展示的映射数据。'
  }
  if (!resolveSuChannelId(channelView.value.code)) {
    return '该渠道当前没有 Su 映射明细接口，首版仅展示连接状态与详情摘要。'
  }
  return '当前筛选条件下没有映射结果，可尝试切换筛选或重新进入编辑映射。'
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

function resolveDraftSummary(hotelKey: string) {
  const draft = mappingDrafts.value[getDraftKey(hotelKey)]
  if (!draft) {
    return ''
  }

  let selectedPlanCount = 0
  for (const item of draft.plans) {
    if (item.selectedPmsRatePlan) {
      selectedPlanCount += 1
    }
  }

  const roomTypeLabel = draft.selectedPmsRoomTypeLabel || '未选 PMS 房型'
  return `本地草稿：${roomTypeLabel}，价盘已选 ${selectedPlanCount} 项，保存于 ${formatDateTime(draft.savedAt)}`
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
  } finally {
    loading.value = false
  }
}

function handleActiveSegmentChange(event: CustomEvent) {
  const nextSegment = event.detail.value as MappingSegment
  activeSegment.value = nextSegment || 'management'
}

function handleFilterModeChange(event: CustomEvent) {
  const nextMode = event.detail.value as MappingFilterMode
  filterMode.value = nextMode || 'all'
}

function handleHotelChange(event: CustomEvent) {
  selectedHotelKey.value = (event.detail.value as string) || 'all'
}

function viewGroupDetails(group: SuHotelMappingView) {
  selectedHotelKey.value = group.hotelKey
  activeSegment.value = 'details'
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
.channel-mapping-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.channel-mapping-page__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.channel-mapping-page__filters,
.channel-mapping-page__group-list,
.channel-mapping-page__plans,
.channel-mapping-page__editor-stack,
.channel-mapping-page__editor-plan-list {
  display: grid;
  gap: 12px;
}

.channel-mapping-page__warning {
  margin-top: 12px;
  color: var(--ion-color-warning);
}

.channel-mapping-page__group-card,
.channel-mapping-page__plan-item,
.channel-mapping-page__editor-plan-item {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--app-border);
}

.channel-mapping-page__group-header,
.channel-mapping-page__plan-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.channel-mapping-page__group-header h3,
.channel-mapping-page__group-header p,
.channel-mapping-page__plan-item p,
.channel-mapping-page__plan-item strong,
.channel-mapping-page__editor-plan-item p,
.channel-mapping-page__editor-plan-item strong,
.channel-mapping-page__editor-hero strong,
.channel-mapping-page__editor-hero p {
  margin: 0;
}

.channel-mapping-page__group-header p,
.channel-mapping-page__plan-item p,
.channel-mapping-page__editor-plan-item p,
.channel-mapping-page__editor-hero p,
.channel-mapping-page__draft-text {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.channel-mapping-page__group-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 12px;
  color: var(--app-muted);
  font-size: 12px;
}

.channel-mapping-page__draft-text,
.channel-mapping-page__management-actions {
  margin-top: 12px;
}

.channel-mapping-page__management-actions,
.channel-mapping-page__editor-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.channel-mapping-page__empty {
  padding-top: 18px;
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
  background: var(--app-primary-soft);
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
</style>
