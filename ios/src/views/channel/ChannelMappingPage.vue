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
        <ion-refresher-content :pulling-text="$t('channel.mobile.refreshMapping')" refreshing-spinner="crescent" />
      </ion-refresher>

      <div v-if="channelView" class="channel-mapping-page__cards">
        <section class="mobile-hero channel-mapping-hero">
          <h1 class="mobile-title">{{ channelView.name }}</h1>
          <div class="mobile-chip-row">
            <span class="mobile-chip channel-mapping-hero__chip channel-mapping-hero__chip--status">
              {{ channelView.statusLabel }}
            </span>
            <span class="mobile-chip channel-mapping-hero__chip channel-mapping-hero__chip--metric">
              {{ $t('channel.mobile.mapping.groups') }} {{ mappingGroups.length }}
            </span>
            <span class="mobile-chip channel-mapping-hero__chip channel-mapping-hero__chip--metric">
              {{ $t('channel.mobile.common.ratePlans') }} {{ totalRatePlanCount }}
            </span>
          </div>
        </section>

        <section class="mobile-card channel-mapping-page__toolbar-card">
          <div class="channel-mapping-page__toolbar">
            <div class="channel-mapping-page__toolbar-main">
              <strong>{{ mappingOverviewText }}</strong>
              <p>{{ $t('channel.mobile.mapping.groupDescription', { group: groupLabel }) }}</p>
            </div>
            <div class="channel-mapping-page__toolbar-actions">
              <ion-spinner v-if="loading" name="crescent" />
              <ion-button size="small" class="channel-mapping-page__action-primary" @click="openConnectEntry">
                {{ $t('channel.mobile.mapping.edit') }}
              </ion-button>
              <ion-button
                size="small"
                fill="outline"
                class="channel-mapping-page__action-secondary"
                @click="reloadMapping"
              >
                {{ $t('channel.mobile.common.refresh') }}
              </ion-button>
            </div>
          </div>

          <div class="channel-mapping-page__filters">
            <ion-segment class="channel-mapping-page__filter-segment" :value="filterMode" @ionChange="handleFilterModeChange">
              <ion-segment-button value="all">
                <ion-label>{{ $t('channel.mobile.mapping.filters.all') }}</ion-label>
              </ion-segment-button>
              <ion-segment-button value="active">
                <ion-label>{{ $t('channel.mobile.mapping.filters.mapped') }}</ion-label>
              </ion-segment-button>
              <ion-segment-button value="problem">
                <ion-label>{{ $t('channel.mobile.common.pending') }}</ion-label>
              </ion-segment-button>
            </ion-segment>

            <div v-if="hotelOptions.length > 0" class="channel-mapping-page__filter-item">
              <span class="channel-mapping-page__filter-label">{{ groupLabel }}</span>
              <span class="channel-mapping-page__filter-value">{{ selectedHotelLabel }}</span>
              <span class="channel-mapping-page__filter-chevron" aria-hidden="true"></span>
              <ion-select
                class="channel-mapping-page__filter-select"
                :value="selectedHotelKey"
                :aria-label="$t('channel.mobile.mapping.filters.selectGroup')"
                interface="action-sheet"
                @ionChange="handleHotelChange"
              >
                <ion-select-option value="all">{{ $t('channel.mobile.mapping.filters.allGroups') }}</ion-select-option>
                <ion-select-option v-for="item in hotelOptions" :key="item" :value="item">
                  {{ item }}
                </ion-select-option>
              </ion-select>
            </div>
          </div>

          <p v-if="combinedNotice" class="mobile-note channel-mapping-page__warning">{{ combinedNotice }}</p>
        </section>

        <section class="channel-mapping-page__list-card">
          <div v-if="filteredGroups.length > 0" class="mobile-list channel-mapping-page__group-list">
            <article v-for="group in filteredGroups" :key="group.id" class="channel-mapping-page__group-card">
              <button
                type="button"
                class="channel-mapping-page__group-toggle"
                @click="toggleGroupExpansion(group.hotelKey)"
              >
                <div class="channel-mapping-page__group-header">
                  <div class="channel-mapping-page__group-copy">
                    <h3>{{ group.title }}</h3>
                    <p>{{ groupLabel }} {{ $t('channel.mobile.common.identifier') }}: {{ group.hotelKey }}</p>
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
                  <span>{{ $t('channel.mobile.common.roomTypes') }} {{ group.roomIds.length }}</span>
                  <span>{{ $t('channel.mobile.common.activeRatePlans') }} {{ group.activeRatePlanCount }}</span>
                  <span>{{ $t('channel.mobile.common.totalRatePlans') }} {{ group.ratePlans.length }}</span>
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
                  {{ isGroupExpanded(group.hotelKey) ? $t('channel.mobile.mapping.collapse') : $t('channel.mobile.mapping.expand') }}
                </ion-button>
                <ion-button size="small" @click="openEditor(group)">{{ $t('channel.mobile.mapping.advancedEdit') }}</ion-button>
              </div>

              <div v-if="isGroupExpanded(group.hotelKey)" class="channel-mapping-page__group-details">
                <p v-if="group.roomIds.length > 0" class="mobile-note channel-mapping-page__group-ids">
                  {{ $t('channel.mobile.common.roomTypeIdentifier') }}：{{ group.roomIds.join(' / ') }}
                </p>

                <div v-if="group.ratePlans.length > 0" class="channel-mapping-page__plans">
                  <div v-for="plan in group.ratePlans" :key="plan.id" class="channel-mapping-page__plan-item">
                    <div class="channel-mapping-page__plan-header">
                      <strong>{{ plan.title }}</strong>
                      <ion-badge :color="plan.statusColor">{{ plan.statusLabel }}</ion-badge>
                    </div>
                    <p>PMS：{{ $t('channel.mobile.common.roomTypes') }} {{ plan.pmsRoomId }} · {{ $t('channel.mobile.common.ratePlans') }} {{ plan.pmsRateId }}</p>
                    <p>{{ $t('channel.mobile.common.channel') }}：{{ $t('channel.mobile.common.roomTypes') }} {{ plan.channelRoomId }} · {{ $t('channel.mobile.common.ratePlans') }} {{ plan.channelRateId }}</p>
                    <p class="channel-mapping-page__plan-secondary">{{ $t('channel.mobile.mapping.priceModel') }} {{ plan.pricingModel }}</p>
                  </div>
                </div>

                <p v-else class="mobile-note">{{ $t('channel.mobile.mapping.noRatePlans') }}</p>
              </div>
            </article>
          </div>

          <p v-else class="mobile-note channel-mapping-page__empty">
            {{ emptyStateText }}
          </p>
        </section>
      </div>

      <section v-else class="mobile-card">
        <p class="mobile-note">
          {{ loading ? $t('channel.mobile.mapping.loading') : $t('channel.mobile.common.notFound') }}
        </p>
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
          <ion-title>
            {{ editorGroup ? `${editorGroup.title} · ${$t('channel.mobile.mapping.advancedEdit')}` : $t('channel.mobile.mapping.editorTitle') }}
          </ion-title>
          <ion-buttons slot="end">
            <ion-button :disabled="editorSubmitting" @click="closeEditor">{{ $t('channel.mobile.common.close') }}</ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>

      <ion-content class="mobile-page channel-mapping-page__editor-page">
        <section class="mobile-card">
          <div v-if="editorGroup && editorState">
            <div class="channel-mapping-page__editor-hero">
              <strong>{{ editorGroup.title }}</strong>
              <p>{{ $t('channel.mobile.mapping.editorDescription') }}</p>
            </div>

            <section v-if="editorLoading" class="channel-mapping-page__editor-loading">
              <ion-spinner name="crescent" />
              <p>{{ $t('channel.mobile.mapping.editorLoading') }}</p>
            </section>

            <div v-else class="channel-mapping-page__editor-stack">
              <label class="channel-mapping-page__editor-field">
                <span>{{ $t('channel.mobile.mapping.pmsRoomType') }}</span>
                <ion-select
                  v-model="editorState.selectedPmsRoomType"
                  fill="outline"
                  interface="action-sheet"
                  :placeholder="$t('channel.mobile.mapping.selectPmsRoomType')"
                >
                  <ion-select-option value="">{{ $t('channel.mobile.mapping.skipSelection') }}</ion-select-option>
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
                  <p>{{ $t('channel.mobile.mapping.channelRoomType') }}：{{ plan.channelRoomId }}</p>
                  <p>{{ $t('channel.mobile.mapping.currentPmsRatePlan') }}：{{ plan.currentPmsRatePlan || $t('channel.mobile.mapping.notSet') }}</p>

                  <label class="channel-mapping-page__editor-field">
                    <span>{{ $t('channel.mobile.mapping.targetPmsRatePlan') }}</span>
                    <ion-select
                      v-model="plan.selectedPmsRatePlan"
                      fill="outline"
                      interface="action-sheet"
                      :placeholder="$t('channel.mobile.mapping.selectPmsRatePlan')"
                    >
                      <ion-select-option value="">{{ $t('channel.mobile.mapping.skipSelection') }}</ion-select-option>
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
                <ion-button fill="outline" :disabled="editorSubmitting" @click="closeEditor">{{ $t('channel.mobile.common.cancel') }}</ion-button>
                <ion-button fill="outline" :disabled="editorSubmitting" @click="handleSaveEditor(false)">
                  {{ editorSubmitting ? $t('channel.mobile.common.saving') : $t('channel.mobile.mapping.saveDraft') }}
                </ion-button>
                <ion-button :disabled="editorSubmitting" @click="handleSaveEditor(true)">
                  {{ $t('channel.mobile.mapping.saveAndOpenWizard') }}
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
import { useI18n } from 'vue-i18n'
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
  type SuMappingsResponse,
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
const { locale, t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const channel = ref<OtaIntegrationDTO | null>(null)
const mappingStatus = ref<SuMappingStatusSummary | null>(null)
const mappingPayload = ref<SuMappingsResponse | null>(null)
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
    return t('routes.ChannelMapping')
  }
  return `${channelView.value.name} · ${t('routes.ChannelMapping')}`
})
const mappingGroups = computed(() => {
  locale.value
  return parseSuMappings(mappingPayload.value, channelView.value?.suChannelId || undefined)
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

const selectedHotelLabel = computed(() => {
  if (selectedHotelKey.value === 'all') {
    return t('channel.mobile.mapping.filters.allGroups')
  }
  return selectedHotelKey.value
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
      return t('channel.mobile.mapping.groupCount', {
        groups: mappingGroups.value.length,
        ratePlans: totalRatePlanCount.value,
      })
    }
    return t('channel.mobile.mapping.pendingRefresh')
  }

  if (mappingStatus.value.error) {
    return t('channel.mobile.mapping.mappingIssue')
  }

  const mappedRoomCount = mappingStatus.value.mappedRoomIdCount || 0
  const activeRatePlanCount = mappingStatus.value.activeRatePlanCount || 0
  if (mappedRoomCount === 0 && activeRatePlanCount === 0) {
    return t('channel.mobile.mapping.notMapped')
  }

  return t('channel.mobile.mapping.overview', {
    rooms: mappedRoomCount,
    ratePlans: activeRatePlanCount,
  })
})

const emptyStateText = computed(() => {
  if (!channelView.value) {
    return t('channel.mobile.mapping.empty')
  }
  if (!resolveSuChannelId(channelView.value.code)) {
    return t('channel.mobile.mapping.unsupported')
  }
  return t('channel.mobile.mapping.filterEmpty')
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

  return t('channel.mobile.mapping.draftBadge', {
    time: formatDraftBadgeTime(draft.savedAt),
  })
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
        throw new Error(roomTypeResponse.message || t('channel.mobile.mapping.roomTypeLoadFailed'))
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
        showWarningToast(t('channel.mobile.mapping.userUnavailable'))
      } else {
        const pricePlanResponse = await getAllPricePlans(userId)
        if (!pricePlanResponse.success || !pricePlanResponse.data) {
          throw new Error(pricePlanResponse.message || t('channel.mobile.mapping.ratePlanLoadFailed'))
        }

        nextPricePlanOptions = pricePlanResponse.data.map((item: PricePlanDTO) => {
          return {
            value: String(item.id || ''),
            label: item.name,
          }
        })
      }
    }

    nextRoomTypeOptions = ensureOption(
      nextRoomTypeOptions,
      editorState.value.selectedPmsRoomType,
      t('channel.mobile.mapping.currentRoomType'),
    )
    for (const plan of editorState.value.plans) {
      nextPricePlanOptions = ensureOption(
        nextPricePlanOptions,
        plan.selectedPmsRatePlan,
        t('channel.mobile.mapping.currentRatePlan'),
      )
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
      throw new Error(detailResponse.message || t('channel.mobile.mapping.detailLoadFailed'))
    }

    channel.value = detailResponse.data
    const suChannelId = resolveSuChannelId(detailResponse.data.code)
    if (!suChannelId) {
      mappingStatus.value = null
      mappingPayload.value = null
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
        t('channel.mobile.mapping.statusLoadFailed'),
      )
    }

    if (mappingsResult.status === 'fulfilled' && mappingsResult.value.success) {
      mappingPayload.value = mappingsResult.value.data || null
    } else {
      mappingPayload.value = null
      const nextWarning = resolveWarningMessage(
        mappingsResult.status === 'rejected' ? mappingsResult.reason : mappingsResult.value.message,
        t('channel.mobile.mapping.resultLoadFailed'),
      )
      loadNotice.value = loadNotice.value ? `${loadNotice.value}；${nextWarning}` : nextWarning
    }

    if (selectedHotelKey.value !== 'all' && !mappingGroups.value.some((item) => item.hotelKey === selectedHotelKey.value)) {
      selectedHotelKey.value = 'all'
    }

    const availableHotelKeys = new Set(mappingGroups.value.map((item) => item.hotelKey))
    expandedGroupKeys.value = expandedGroupKeys.value.filter((item) => availableHotelKeys.has(item))
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
  loadNotice.value = sanitizeChannelWarningMessage(message, t('channel.mobile.mapping.operationFailed'))
}

async function openEditor(group: SuHotelMappingView) {
  editorGroup.value = group
  editorState.value = applyDraftToEditorState(createEditorState(group))
  editorOpen.value = true

  try {
    await ensureEditorOptions()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('channel.mobile.mapping.editorOptionsLoadFailed')))
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
    showWarningToast(t('channel.mobile.mapping.selectionRequired'))
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
      showSuccessToast(t('channel.mobile.mapping.draftSavedOpenWizard'))
      closeEditor()
      widgetModalOpen.value = true
      return
    }

    showSuccessToast(t('channel.mobile.mapping.draftSaved'))
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
      showWarningToast(resolveWarningMessage(error, t('channel.mobile.mapping.refreshFailed')))
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
  --background: linear-gradient(180deg, #edf8ff 0%, #f3f9ff 54%, #f8fbff 100%);
  --padding-top: 10px;
  --padding-start: 22px;
  --padding-end: 20px;
  --padding-bottom: calc(26px + var(--app-safe-bottom));
}

.channel-mapping-page__cards,
.channel-mapping-page__group-list {
  display: grid;
  gap: 10px;
}

.channel-mapping-hero {
  padding: 18px 20px 16px;
  border: 0;
  border-radius: 6px;
  background: #ffffff;
  box-shadow: 0 8px 22px rgba(118, 155, 196, 0.06);
}

.channel-mapping-hero::before {
  display: none;
}

.channel-mapping-hero .mobile-title {
  color: #30323a;
  font-size: 27px;
  line-height: 1.1;
  letter-spacing: 0;
}

.channel-mapping-hero .mobile-chip-row {
  gap: 9px;
  margin-top: 10px;
}

.channel-mapping-hero__chip {
  max-width: 100%;
  min-height: 29px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
  overflow-wrap: anywhere;
  text-align: center;
  white-space: normal;
}

.channel-mapping-hero__chip--status {
  border-color: transparent;
  background: #edf6ff;
  color: #2c73c8;
  box-shadow: 0 4px 12px rgba(44, 115, 200, 0.09);
}

.channel-mapping-hero__chip--metric {
  border-color: #dfe3e8;
  background: #ffffff;
  color: #727985;
}

.channel-mapping-page__toolbar-card {
  position: relative;
  overflow: hidden;
  padding: 20px 18px 30px;
  border: 1px solid rgba(222, 233, 244, 0.78);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 12px 28px rgba(114, 151, 190, 0.1);
}

.channel-mapping-page__toolbar-card::before {
  display: none;
}

.channel-mapping-page__toolbar-card > * {
  position: relative;
  z-index: 1;
}

.channel-mapping-page__toolbar,
.channel-mapping-page__filters,
.channel-mapping-page__plans,
.channel-mapping-page__editor-stack,
.channel-mapping-page__editor-plan-list {
  display: grid;
  gap: 12px;
}

.channel-mapping-page__toolbar {
  gap: 12px;
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
  line-height: 1.25;
  letter-spacing: 0;
}

.channel-mapping-page__toolbar-main p,
.channel-mapping-page__group-header p,
.channel-mapping-page__plan-item p,
.channel-mapping-page__editor-plan-item p,
.channel-mapping-page__editor-hero p,
.channel-mapping-page__group-ids {
  margin: 5px 0 0;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.channel-mapping-page__toolbar-main p {
  color: #9aa3ad;
  font-size: 14px;
  line-height: 1.35;
}

.channel-mapping-page__toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 9px;
  margin-top: 2px;
}

.channel-mapping-page__toolbar-actions ion-spinner {
  color: var(--ios-pms-primary);
  transform: scale(0.82);
}

.channel-mapping-page__action-primary,
.channel-mapping-page__action-secondary,
.channel-mapping-page__management-actions ion-button {
  height: 37px;
  min-height: 37px;
  margin: 0;
  --border-radius: 9px;
  --padding-start: 13px;
  --padding-end: 13px;
  font-size: 15px;
  letter-spacing: 0;
}

.channel-mapping-page__action-primary::part(native),
.channel-mapping-page__action-secondary::part(native),
.channel-mapping-page__management-actions ion-button::part(native) {
  min-height: 37px;
  white-space: normal;
}

.channel-mapping-page__action-primary {
  min-width: 102px;
  --background: #1777ff;
  --background-activated: #1269e5;
  --background-hover: #1269e5;
  --box-shadow: 0 8px 16px rgba(23, 119, 255, 0.18);
}

.channel-mapping-page__action-secondary {
  min-width: 76px;
  --background: #ffffff;
  --border-color: #d9e2ee;
  --color: #236ad8;
  --box-shadow: none;
}

.channel-mapping-page__filters {
  gap: 20px;
  margin-top: 12px;
}

.channel-mapping-page__filter-segment {
  height: 36px;
  padding: 0;
  border: 1px solid #eceef2;
  border-radius: 18px;
  background: #ffffff;
  overflow: hidden;
}

.channel-mapping-page__filter-segment ion-segment-button {
  --border-radius: 18px;
  --color: #111318;
  --color-checked: #ffffff;
  --indicator-box-shadow: none;
  --indicator-color: #303030;
  min-height: 36px;
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.channel-mapping-page__filter-segment ion-segment-button::part(native) {
  min-height: 36px;
  padding: 0;
  white-space: normal;
}

.channel-mapping-page__filter-segment ion-segment-button::part(indicator-background) {
  border-radius: 18px;
  background: #303030;
}

.channel-mapping-page__filter-item {
  position: relative;
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr) 74px;
  align-items: center;
  min-height: 77px;
  padding: 0 17px 0 18px;
  margin: 0;
  border: 1px solid #e7e9ed;
  border-radius: 9px;
  background: #ffffff;
  box-shadow: none;
}

.channel-mapping-page__filter-label {
  grid-column: 1;
  margin: 0;
  color: #666d78;
  font-size: 16px;
  font-weight: 500;
  pointer-events: none;
}

.channel-mapping-page__filter-value {
  grid-column: 2;
  justify-self: center;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  color: #555c67;
  font-size: 18px;
  font-weight: 500;
  line-height: 1.2;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
  pointer-events: none;
}

.channel-mapping-page__filter-chevron {
  position: relative;
  grid-column: 3;
  justify-self: end;
  width: 22px;
  height: 22px;
  pointer-events: none;
}

.channel-mapping-page__filter-chevron::before,
.channel-mapping-page__filter-chevron::after {
  content: '';
  position: absolute;
  right: 5px;
  width: 10px;
  height: 10px;
  border-right: 2px solid #8c949f;
  border-bottom: 2px solid #8c949f;
}

.channel-mapping-page__filter-chevron::before {
  top: calc(50% - 13px);
  transform: rotate(225deg);
}

.channel-mapping-page__filter-chevron::after {
  top: calc(50% + 1px);
  transform: rotate(45deg);
}

.channel-mapping-page__filter-select {
  position: absolute;
  inset: 0;
  width: 100%;
  min-height: 100%;
  --padding-start: 0;
  --padding-end: 0;
  opacity: 0;
}

.channel-mapping-page__filter-select::part(icon) {
  display: none;
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
  padding: 19px 18px 20px;
  border: 1px solid rgba(222, 233, 244, 0.78);
  border-radius: 21px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 12px 28px rgba(114, 151, 190, 0.1);
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
  gap: 10px;
}

.channel-mapping-page__group-copy {
  min-width: 0;
  padding-top: 1px;
}

.channel-mapping-page__group-header-side {
  display: inline-flex;
  align-items: flex-start;
  flex: 0 0 auto;
  gap: 6px;
}

.channel-mapping-page__group-arrow {
  color: #8d949e;
  font-size: 26px;
  line-height: 32px;
  transition: transform 0.18s ease;
}

.channel-mapping-page__group-header ion-badge,
.channel-mapping-page__plan-header ion-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border-radius: 11px;
  font-size: 14px;
  font-weight: 700;
  max-width: 100%;
  overflow-wrap: anywhere;
  white-space: normal;
}

.channel-mapping-page__group-header ion-badge {
  min-width: 78px;
  min-height: 32px;
}

.channel-mapping-page__plan-header ion-badge {
  padding-top: 4px;
  padding-bottom: 4px;
}

.channel-mapping-page__group-header h3 {
  font-size: 16px;
  line-height: 1.32;
  letter-spacing: 0;
}

.channel-mapping-page__group-header p {
  color: #98a0ab;
  font-size: 14px;
  line-height: 1.35;
}

.channel-mapping-page__group-arrow--expanded {
  transform: rotate(90deg);
}

.channel-mapping-page__group-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 20px;
}

.channel-mapping-page__group-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 29px;
  padding: 0 11px;
  border: 1px solid #dfe3e8;
  border-radius: 999px;
  background: #ffffff;
  color: #59626f;
  font-size: 13px;
  font-weight: 500;
  overflow-wrap: anywhere;
  text-align: center;
  white-space: normal;
}

.channel-mapping-page__draft-badge {
  border-color: rgba(52, 116, 246, 0.14);
  background: rgba(52, 116, 246, 0.09);
  color: #315fc1;
  font-weight: 600;
}

.channel-mapping-page__group-details,
.channel-mapping-page__management-actions {
  margin-top: 14px;
}

.channel-mapping-page__group-details {
  padding-top: 14px;
  border-top: 1px solid rgba(113, 132, 170, 0.11);
}

.channel-mapping-page__management-actions,
.channel-mapping-page__editor-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.channel-mapping-page__editor-actions ion-button {
  min-height: 34px;
  --border-radius: 12px;
  font-size: 12px;
}

.channel-mapping-page__management-actions ion-button:first-child {
  min-width: 105px;
  --background: #ffffff;
  --border-color: #d9e2ee;
  --color: #236ad8;
  --box-shadow: none;
}

.channel-mapping-page__management-actions ion-button:last-child {
  min-width: 91px;
  --background: #1777ff;
  --background-activated: #1269e5;
  --background-hover: #1269e5;
  --box-shadow: 0 8px 16px rgba(23, 119, 255, 0.18);
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
  overflow-wrap: anywhere;
}

.channel-mapping-page__editor-field ion-select::part(text) {
  overflow-wrap: anywhere;
  white-space: normal;
}

@media (max-width: 480px) {
  .channel-mapping-page__toolbar-actions {
    justify-content: flex-start;
  }
}
</style>
