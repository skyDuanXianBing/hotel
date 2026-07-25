import type {
  OtaIntegrationDTO,
  SuChannelMappingEntry,
  SuMappingRatePlan,
  SuMappingStatusSummary,
  SuMappingsResponse,
} from '@/api/otaIntegration'
import type {
  ChannelPriceAdjustmentDTO,
  ChannelPriceAdjustmentRequest,
  PriceAdjustmentType,
} from '@/api/pricelabs'
import { i18n } from '@/locales'
import { resolveMappingStatusNotice } from '@/utils/channelMessage'
import {
  compareLocalizedText,
  formatMoney,
  type MoneyDisplayContext,
} from '@/utils/formatters'
import { formatStoreDateTime } from '@/utils/storeBusinessDate'

export type ChannelBadgeColor = 'success' | 'warning' | 'medium' | 'primary'
export type PriceAdjustmentUnit = 'PERCENTAGE' | 'FIXED'
export type PriceAdjustmentDirection = 'cheaper' | 'expensive'

export interface ChannelViewModel extends OtaIntegrationDTO {
  suChannelId: string | null
  mappingStatus: SuMappingStatusSummary | null
  statusLabel: string
  statusDescription: string
  statusColor: ChannelBadgeColor
  actionLabel: string
  actionHint: string
  mappingReady: boolean
  lastStatusText: string
}

export interface PriceAdjustmentEditorValue {
  channelId: number
  channelName: string
  channelCode: string
  direction: PriceAdjustmentDirection
  value: number
  unit: PriceAdjustmentUnit
  autoSyncPrice: boolean
}

export interface SuRatePlanView {
  id: string
  title: string
  mappingStatus: string
  statusLabel: string
  statusColor: ChannelBadgeColor
  pmsRoomId: string
  pmsRateId: string
  channelRoomId: string
  channelRateId: string
  pricingModel: string
}

export interface SuHotelMappingView {
  id: string
  hotelKey: string
  title: string
  statusLabel: string
  statusColor: ChannelBadgeColor
  roomIds: string[]
  ratePlans: SuRatePlanView[]
  activeRatePlanCount: number
}

export interface ChannelActionCapability {
  supportsSu: boolean
  supportsCalendarSync: boolean
  supportsFullRefresh: boolean
  supportsInventorySettings: boolean
  groupLabel: string
}

const PINNED_CHANNEL_NAMES = ['Booking.com', 'Airbnb', 'Agoda', 'Trip.com']

function normalizeCode(code?: string) {
  return (code || '').trim().toUpperCase()
}

function normalizeChannelBusinessKey(value?: string) {
  const normalizedValue = normalizeCode(value).replace(/[^A-Z0-9]/g, '')

  if (normalizedValue === 'BOOKING' || normalizedValue === 'BOOKINGCOM') {
    return 'BOOKING'
  }

  return normalizedValue
}

function normalizeNumber(value: number | null | undefined) {
  return Number(value || 0)
}

function getPinnedOrder(name: string) {
  const index = PINNED_CHANNEL_NAMES.indexOf(name)
  if (index === -1) {
    return PINNED_CHANNEL_NAMES.length + 1
  }
  return index
}

function toDisplayText(value: unknown, fallback: string) {
  if (typeof value !== 'string') {
    return fallback
  }

  const trimmed = value.trim()
  if (!trimmed) {
    return fallback
  }

  return trimmed
}

function getStatusMeta(
  ota: OtaIntegrationDTO,
  mappingStatus: SuMappingStatusSummary | null,
): Pick<
  ChannelViewModel,
  'statusLabel' | 'statusDescription' | 'statusColor' | 'actionLabel' | 'actionHint' | 'mappingReady'
> {
  const isConnected = Boolean(ota.isConnected)
  const mappingReady = Boolean(mappingStatus?.mappingReady)
  const usesSuMapping = Boolean(resolveSuChannelId(ota.code))

  if (mappingReady) {
    return {
      statusLabel: i18n.global.t('channel.list.connected'),
      statusDescription: i18n.global.t('stage5VisibleText.48', {
        rooms: mappingStatus?.mappedRoomIdCount || 0,
        ratePlans: mappingStatus?.activeRatePlanCount || 0,
      }),
      statusColor: 'success',
      actionLabel: i18n.global.t('iosStage5.channel.viewConfiguration'),
      actionHint: i18n.global.t('stage5VisibleText.42'),
      mappingReady: true,
    }
  }

  if (!usesSuMapping && isConnected) {
    return {
      statusLabel: i18n.global.t('iosStage5.channel.authorized'),
      statusDescription: i18n.global.t('stage5VisibleText.41'),
      statusColor: 'primary',
      actionLabel: i18n.global.t('iosStage5.channel.viewConfiguration'),
      actionHint: i18n.global.t('stage5VisibleText.44'),
      mappingReady: false,
    }
  }

  if (isConnected) {
    const localizedErrorMessage = resolveMappingStatusNotice(mappingStatus?.error)
    const errorText = localizedErrorMessage
      ? i18n.global.t('stage5VisibleText.63', { message: localizedErrorMessage })
      : i18n.global.t('stage5VisibleText.40')
    return {
      statusLabel: i18n.global.t('iosStage5.channel.mappingIncomplete'),
      statusDescription: errorText,
      statusColor: 'warning',
      actionLabel: i18n.global.t('iosStage5.channel.continueMapping'),
      actionHint: i18n.global.t('stage5VisibleText.46'),
      mappingReady: false,
    }
  }

  return {
    statusLabel: i18n.global.t('iosStage5.channel.unauthorized'),
    statusDescription: i18n.global.t('stage5VisibleText.45'),
    statusColor: 'medium',
    actionLabel: i18n.global.t('iosStage5.channel.startAuthorization'),
    actionHint: i18n.global.t('stage5VisibleText.43'),
    mappingReady: false,
  }
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return null
  }
  return value as Record<string, unknown>
}

function asStringArray(value: unknown) {
  if (!Array.isArray(value)) {
    return []
  }

  const result: string[] = []
  for (const item of value) {
    if (typeof item === 'string' && item.trim()) {
      result.push(item.trim())
    }
  }
  return result
}

function extractChannelEntries(payload: SuMappingsResponse | null | undefined, channelId?: string) {
  if (!payload) {
    return []
  }

  if (channelId && Array.isArray(payload[channelId])) {
    return payload[channelId] as SuChannelMappingEntry[]
  }

  for (const value of Object.values(payload)) {
    if (Array.isArray(value)) {
      return value as SuChannelMappingEntry[]
    }
  }

  return []
}

function buildRatePlanView(
  plan: SuMappingRatePlan,
  hotelKey: string,
  index: number,
): SuRatePlanView {
  const mappingStatus = toDisplayText(plan.MappingStatus, 'UNKNOWN')
  const isActive = mappingStatus.toUpperCase() === 'ACTIVE'

  return {
    id: `${hotelKey}-${index}-${toDisplayText(plan.ChannelRateID, 'rate')}`,
    title: toDisplayText(plan.ChannelMappingName, i18n.global.t('channel.mappingPriceSettings.unnamedMapping')),
    mappingStatus,
    statusLabel: isActive ? i18n.global.t('channel.mapping.statuses.connected') : i18n.global.t('home.stat.pending.0'),
    statusColor: isActive ? 'success' : 'warning',
    pmsRoomId: toDisplayText(plan.PMSRoomID, '-'),
    pmsRateId: toDisplayText(plan.PMSRateID, '-'),
    channelRoomId: toDisplayText(plan.ChannelRoomID, '-'),
    channelRateId: toDisplayText(plan.ChannelRateID, '-'),
    pricingModel: toDisplayText(plan.PricingModel, '-'),
  }
}

export function resolveSuChannelId(code?: string) {
  const normalized = normalizeChannelCode(code)
  if (normalized === 'BOOKING' || normalized === 'BOOKING.COM') {
    return '19'
  }
  if (normalized === 'AIRBNB') {
    return '244'
  }
  return null
}

export function isAirbnbChannelCode(code?: string) {
  return normalizeChannelCode(code) === 'AIRBNB'
}

export function normalizeChannelCode(code?: string) {
  return normalizeChannelBusinessKey(code)
}

function normalizeChannelName(name?: string) {
  return normalizeChannelBusinessKey(name)
}

export function getChannelGroupLabel(code?: string) {
  if (isAirbnbChannelCode(code)) {
    return i18n.global.t('settingsStage4.pricingTools.columns.account')
  }
  return i18n.global.t('settingsStage4.storeBasic.typeOptions.hotel')
}

export function getChannelActionCapability(code?: string): ChannelActionCapability {
  const supportsSu = Boolean(resolveSuChannelId(code))
  const supportsInventorySettings = isAirbnbChannelCode(code)

  return {
    supportsSu,
    supportsCalendarSync: supportsSu,
    supportsFullRefresh: supportsSu,
    supportsInventorySettings,
    groupLabel: getChannelGroupLabel(code),
  }
}

export function buildChannelViewModel(
  ota: OtaIntegrationDTO,
  mappingStatus: SuMappingStatusSummary | null,
): ChannelViewModel {
  const statusMeta = getStatusMeta(ota, mappingStatus)

  return {
    ...ota,
    suChannelId: resolveSuChannelId(ota.code),
    mappingStatus,
    ...statusMeta,
    lastStatusText: formatDateTime(ota.updatedAt),
  }
}

export function sortChannelViewModels(items: ChannelViewModel[]) {
  return [...items].sort((left, right) => {
    const leftReady = left.mappingReady ? 0 : left.isConnected ? 1 : 2
    const rightReady = right.mappingReady ? 0 : right.isConnected ? 1 : 2

    if (leftReady !== rightReady) {
      return leftReady - rightReady
    }

    const leftPinned = getPinnedOrder(left.name)
    const rightPinned = getPinnedOrder(right.name)
    if (leftPinned !== rightPinned) {
      return leftPinned - rightPinned
    }

    return compareLocalizedText(left.name, right.name)
  })
}

export function formatDateTime(value?: string) {
  if (!value) {
    return i18n.global.t('iosStage5.common.noRecords')
  }

  return formatStoreDateTime(value, 'date-time', value)
}

export function formatAdjustmentSummary(
  item: ChannelPriceAdjustmentDTO,
  currency = 'CNY',
  context: MoneyDisplayContext = {},
) {
  const adjustmentValue = normalizeNumber(item.adjustmentValue)
  if (adjustmentValue === 0) {
    return i18n.global.t('channel.priceRatio.sameAsBase')
  }

  const direction = adjustmentValue > 0 ? i18n.global.t('stage5VisibleText.100') : i18n.global.t('stage5VisibleText.99')
  const isFixed = item.adjustmentType === 'FIXED'
  const value = isFixed
    ? formatMoney(
        Math.abs(adjustmentValue),
        currency,
        { maximumFractionDigits: 2 },
        context,
      )
    : Math.abs(adjustmentValue)
  const unit = isFixed ? '' : '%'
  return i18n.global.t('stage5Final.channel.adjustmentPreview', {
    value,
    unit,
    direction,
  })
}

function resolveOtaPriceAdjustmentType(ota: Pick<OtaIntegrationDTO, 'priceAdjustmentType'>) {
  if (ota.priceAdjustmentType === 'FIXED') {
    return 'FIXED'
  }

  return 'PERCENTAGE'
}

export function hasConfiguredPriceAdjustment(
  ota: Pick<OtaIntegrationDTO, 'priceAdjustmentType' | 'priceAdjustmentValue'> | null | undefined,
) {
  if (!ota) {
    return false
  }

  return ota.priceAdjustmentType !== undefined || ota.priceAdjustmentValue !== undefined
}

export function buildChannelPriceAdjustmentFromOta(
  ota:
    | Pick<
        OtaIntegrationDTO,
        'id' | 'name' | 'code' | 'priceAdjustmentType' | 'priceAdjustmentValue' | 'autoSyncPrice'
      >
    | null
    | undefined,
  options?: {
    includeDefault?: boolean
  },
) {
  if (!ota) {
    return null
  }

  const hasConfiguredValue = hasConfiguredPriceAdjustment(ota)
  if (!hasConfiguredValue && !options?.includeDefault) {
    return null
  }

  return {
    channelId: ota.id,
    channelName: ota.name,
    channelCode: ota.code,
    adjustmentType: resolveOtaPriceAdjustmentType(ota),
    adjustmentValue: hasConfiguredValue ? ota.priceAdjustmentValue ?? 0 : 0,
    autoSyncPrice: ota.autoSyncPrice ?? true,
  } satisfies ChannelPriceAdjustmentDTO
}

export function createPriceAdjustmentEditor(
  item: ChannelPriceAdjustmentDTO,
): PriceAdjustmentEditorValue {
  const adjustmentValue = normalizeNumber(item.adjustmentValue)

  return {
    channelId: item.channelId,
    channelName: item.channelName,
    channelCode: item.channelCode,
    direction: adjustmentValue < 0 ? 'cheaper' : 'expensive',
    value: Math.abs(adjustmentValue),
    unit: item.adjustmentType === 'FIXED' ? 'FIXED' : 'PERCENTAGE',
    autoSyncPrice: item.autoSyncPrice ?? true,
  }
}

export function createPriceAdjustmentEditorFromOta(
  ota:
    | Pick<
        OtaIntegrationDTO,
        'id' | 'name' | 'code' | 'priceAdjustmentType' | 'priceAdjustmentValue' | 'autoSyncPrice'
      >
    | null
    | undefined,
) {
  const adjustment = buildChannelPriceAdjustmentFromOta(ota, { includeDefault: true })
  if (!adjustment) {
    return null
  }

  return createPriceAdjustmentEditor(adjustment)
}

export function findChannelPriceAdjustment(
  adjustments: ChannelPriceAdjustmentDTO[] | null | undefined,
  ota: Pick<OtaIntegrationDTO, 'code' | 'name'> | null | undefined,
) {
  if (!adjustments || adjustments.length === 0 || !ota) {
    return null
  }

  const targetChannelCode = normalizeChannelCode(ota.code)
  for (const item of adjustments) {
    if (normalizeChannelCode(item.channelCode) === targetChannelCode) {
      return item
    }
  }

  const targetChannelName = normalizeChannelName(ota.name)
  for (const item of adjustments) {
    if (normalizeChannelName(item.channelName) === targetChannelName) {
      return item
    }
  }

  return null
}

export function buildPriceAdjustmentRequest(
  editor: PriceAdjustmentEditorValue,
): ChannelPriceAdjustmentRequest {
  let adjustmentType: PriceAdjustmentType = 'PERCENTAGE'
  if (editor.unit === 'FIXED') {
    adjustmentType = 'FIXED'
  }

  let adjustmentValue = editor.value
  if (editor.direction === 'cheaper') {
    adjustmentValue = adjustmentValue * -1
  }

  return {
    adjustmentType,
    adjustmentValue,
    autoSyncPrice: editor.autoSyncPrice,
  }
}

export function parseSuMappings(payload: SuMappingsResponse | null | undefined, channelId?: string) {
  const entries = extractChannelEntries(payload, channelId)
  const groups: SuHotelMappingView[] = []

  for (let index = 0; index < entries.length; index += 1) {
    const entry = entries[index]
    const entryRecord = asRecord(entry)
    if (!entryRecord) {
      continue
    }

    const hotelKey = toDisplayText(entryRecord.ChannelHotelID, `group-${index + 1}`)
    const roomIds = asStringArray(entryRecord.RoomIDs)
    const rawRatePlans = Array.isArray(entryRecord.Rateplans) ? entryRecord.Rateplans : []
    const ratePlans: SuRatePlanView[] = []

    for (let ratePlanIndex = 0; ratePlanIndex < rawRatePlans.length; ratePlanIndex += 1) {
      const ratePlanRecord = asRecord(rawRatePlans[ratePlanIndex])
      if (!ratePlanRecord) {
        continue
      }
      ratePlans.push(buildRatePlanView(ratePlanRecord as SuMappingRatePlan, hotelKey, ratePlanIndex))
    }

    let activeRatePlanCount = 0
    for (const item of ratePlans) {
      if (item.mappingStatus.toUpperCase() === 'ACTIVE') {
        activeRatePlanCount += 1
      }
    }

    const statusValue = toDisplayText(entryRecord.Status, activeRatePlanCount > 0 ? 'Active' : 'Pending')
    const isActive = statusValue.toUpperCase() === 'ACTIVE'

    groups.push({
      id: `${hotelKey}-${index}`,
      hotelKey,
      title: i18n.global.t('stage5VisibleText.84', { id: hotelKey }),
      statusLabel: isActive ? i18n.global.t('channel.mapping.statuses.connected') : i18n.global.t('home.stat.pending.0'),
      statusColor: isActive ? 'success' : 'warning',
      roomIds,
      ratePlans,
      activeRatePlanCount,
    })
  }

  return groups
}

export function filterSuMappings(
  groups: SuHotelMappingView[],
  hotelKey: string,
  filterMode: 'all' | 'active' | 'problem',
) {
  const filteredGroups: SuHotelMappingView[] = []

  for (const group of groups) {
    if (hotelKey !== 'all' && group.hotelKey !== hotelKey) {
      continue
    }

    if (filterMode === 'all') {
      filteredGroups.push(group)
      continue
    }

    const filteredPlans: SuRatePlanView[] = []
    for (const plan of group.ratePlans) {
      const isActive = plan.mappingStatus.toUpperCase() === 'ACTIVE'
      if (filterMode === 'active' && isActive) {
        filteredPlans.push(plan)
      }
      if (filterMode === 'problem' && !isActive) {
        filteredPlans.push(plan)
      }
    }

    if (filteredPlans.length === 0) {
      continue
    }

    filteredGroups.push({
      ...group,
      ratePlans: filteredPlans,
      activeRatePlanCount: filteredPlans.filter((plan) => plan.mappingStatus.toUpperCase() === 'ACTIVE').length,
    })
  }

  return filteredGroups
}
