import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  getAllOtaIntegrations,
  getOtaIntegrationById,
  getSuMappingStatus,
  getSuMappings,
  type OtaIntegrationDTO,
  type SuMappingEntry,
} from '@/api/otaIntegration'
import {
  getChannelPriceAdjustments,
  updateChannelPriceAdjustment,
  type ChannelPriceAdjustmentDTO,
  type PriceAdjustmentType,
} from '@/api/pricelabs'
import { getRooms, type RoomDTO } from '@/api/room'
import { getRoomPriceManagementData, type RoomPriceManagementDTO } from '@/api/roomPrice'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { getCurrentStorePricePlans, type PricePlanDTO } from '@/api/pricePlan'
import { formatYmdMonthDay, getStoreDateRange, getStoreTodayYmd } from '@/utils/storeDateTime'

import type {
  ChannelItem,
  AirbnbAccount,
  CalendarDate,
  CalendarRow,
  HotelItem,
  PriceRatioItem,
  PriceRatioEdit,
  SelectOption,
  FlattenedMappingItem,
  RoomMappingGroup,
  RoomSettingsRow,
} from '../types'
import { CHANNEL_LOGO_MAP } from '../constants'

interface StoreCache {
  id?: number
  name?: string
  suHotelId?: string
}

interface ChannelManagementData {
  hotels: HotelItem[]
  airbnbAccounts: AirbnbAccount[]
  roomMappings: RoomMappingGroup[]
  calendarDates: CalendarDate[]
  calendarData: CalendarRow[]
  roomSettingsDates: CalendarDate[]
  roomSettingsData: RoomSettingsRow[]
  selectedHotelId: number | null
}

const FALLBACK_PRICE_PLANS: SelectOption[] = [
  { value: 'Rack Rate', label: 'Rack Rate' },
  { value: 'Standard Rate', label: 'Standard Rate' },
]

const resolveSuChannelId = (code: string): string | null => {
  const normalized = (code || '').trim().toUpperCase()
  if (normalized === 'BOOKING' || normalized === 'BOOKING.COM') return '19'
  if (normalized === 'AIRBNB') return '244'
  return null
}

const normalizeText = (value: unknown): string =>
  String(value ?? '')
    .trim()
    .toLowerCase()

const readCurrentStore = (): StoreCache | null => {
  if (typeof window === 'undefined') return null
  const raw = window.localStorage.getItem('currentStore')
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoreCache
  } catch {
    return null
  }
}

/**
 * 渠道管理页面共享业务逻辑
 */
export function useChannelData() {
  const { t, locale } = useI18n()

  // ──────────── OTA 渠道列表 ────────────
  const otaChannels = ref<ChannelItem[]>([])
  const channelsLoading = ref(false)

  const loadChannels = async () => {
    channelsLoading.value = true
    try {
      const response = await getAllOtaIntegrations()
      if (response.success && response.data) {
        const mappingReadyById = new Map<number, boolean>()
        await Promise.all(
          response.data.map(async (ota: OtaIntegrationDTO) => {
            const channelId = resolveSuChannelId(ota.code)
            if (!channelId) {
              mappingReadyById.set(ota.id, false)
              return
            }
            try {
              const statusResp = await getSuMappingStatus(ota.id, channelId)
              const ready = Boolean(statusResp.success && statusResp.data?.mappingReady)
              mappingReadyById.set(ota.id, ready)
            } catch {
              mappingReadyById.set(ota.id, false)
            }
          }),
        )

        otaChannels.value = response.data.map((ota: OtaIntegrationDTO) => ({
          id: ota.id,
          name: ota.name,
          code: ota.code,
          logoUrl:
            ota.logoUrl ||
            CHANNEL_LOGO_MAP[ota.name] ||
            'https://via.placeholder.com/120x60/409EFF/FFFFFF?text=' + encodeURIComponent(ota.name),
          connected: mappingReadyById.get(ota.id) === true,
        }))
      }
    } catch (error) {
      console.error('加载OTA直连渠道数据失败:', error)
      ElMessage.error(t('channel.messages.loadOtaFailed'))
    } finally {
      channelsLoading.value = false
    }
  }

  // ──────────── PMS 房型选项 ────────────
  const pmsRoomOptions = ref<SelectOption[]>([])

  const loadPmsRoomOptions = async () => {
    try {
      const resp = await getAllRoomTypes()
      if (!resp.success || !resp.data) return

      const names = resp.data
        .map((rt: any) => (rt?.name ? String(rt.name).trim() : ''))
        .filter((name: string) => !!name)

      pmsRoomOptions.value = Array.from(new Set(names)).map((name) => ({
        value: name,
        label: name,
      }))
    } catch (e) {
      console.error('加载PMS房型列表失败:', e)
    }
  }

  // PMS 价格计划选项
  const pmsPricePlanOptions = ref<SelectOption[]>([...FALLBACK_PRICE_PLANS])

  const loadPmsPricePlanOptions = async () => {
    try {
      const resp = (await getCurrentStorePricePlans()) as any
      const plans = Array.isArray(resp?.data) ? (resp.data as PricePlanDTO[]) : []
      if (plans.length === 0) {
        pmsPricePlanOptions.value = [...FALLBACK_PRICE_PLANS]
        return
      }

      pmsPricePlanOptions.value = plans
        .map((plan) => (plan.name || plan.nameEn || '').trim())
        .filter((name) => !!name)
        .map((name) => ({
          value: name,
          label: name,
        }))
    } catch (e) {
      console.error('加载PMS价格计划列表失败:', e)
      pmsPricePlanOptions.value = [...FALLBACK_PRICE_PLANS]
    }
  }

  // ──────────── 价格比例 ────────────
  const priceRatioData = ref<PriceRatioItem[]>([])
  const priceRatioLoading = ref(false)

  const formatBackendRatioString = (
    adjustmentType: PriceAdjustmentType,
    adjustmentValue: number | null,
  ): string => {
    const value = adjustmentValue ?? 0
    if (value === 0) {
      return t('channel.priceRatio.sameAsBase')
    }

    const unit = adjustmentType === 'FIXED' ? '¥' : '%'
    if (value > 0) {
      return t('channel.priceRatio.moreExpensiveThanBase', {
        value: Math.abs(value),
        unit,
      })
    }

    return t('channel.priceRatio.cheaperThanBase', {
      value: Math.abs(value),
      unit,
    })
  }

  const refreshPriceRatioText = () => {
    priceRatioData.value = priceRatioData.value.map((item) => ({
      ...item,
      ratio: formatBackendRatioString(item.adjustmentType, item.adjustmentValue),
    }))
  }

  watch(locale, refreshPriceRatioText)

  const loadPriceRatioData = async () => {
    priceRatioLoading.value = true
    try {
      const response = await getChannelPriceAdjustments()
      if (response.success && response.data) {
        const supported = response.data.filter(
          (item: ChannelPriceAdjustmentDTO) =>
            item.channelCode === 'AIRBNB' || item.channelCode === 'BOOKING',
        )

        priceRatioData.value = supported.map((item: ChannelPriceAdjustmentDTO) => {
          const adjustmentType = item.adjustmentType ?? 'PERCENTAGE'
          const adjustmentValue = item.adjustmentValue ?? null

          return {
            channelId: item.channelId,
            channelCode: item.channelCode,
            channel: item.channelName,
            ratio: formatBackendRatioString(adjustmentType, adjustmentValue),
            adjustmentType,
            adjustmentValue,
            autoSyncPrice: item.autoSyncPrice ?? true,
            suMappingMultiplier: item.suMappingMultiplier,
            suMappingSurcharge: item.suMappingSurcharge,
            suMappingSync: item.suMappingSync,
          }
        })
      }
    } catch (error) {
      console.error('加载价格比例数据失败:', error)
      ElMessage.error(t('channel.priceRatio.updateFailed'))
    } finally {
      priceRatioLoading.value = false
    }
  }

  // ──────────── 价格比例格式化 ────────────

  /** 将编辑态转为显示字符串 */
  const formatRatioString = (
    type: 'cheaper' | 'expensive',
    value: number,
    unit: '%' | '¥',
  ): string => {
    if (value === 0) {
      return t('channel.priceRatio.sameAsBase')
    }
    if (type === 'expensive') {
      return t('channel.priceRatio.moreExpensiveThanBase', { value, unit })
    }
    return t('channel.priceRatio.cheaperThanBase', { value, unit })
  }

  /** 保存价格比例 */
  const savePriceRatio = async (editData: PriceRatioEdit): Promise<boolean> => {
    const { channelId, adjustmentType, adjustmentValue, adjustmentUnit, autoSyncPrice } = editData

    let backendAdjustmentType: PriceAdjustmentType
    let backendAdjustmentValue: number | null = adjustmentValue

    if (adjustmentValue === 0) {
      backendAdjustmentType = 'PERCENTAGE'
      backendAdjustmentValue = 0
    } else if (adjustmentUnit === '%') {
      backendAdjustmentType = 'PERCENTAGE'
      backendAdjustmentValue = adjustmentType === 'cheaper' ? -adjustmentValue : adjustmentValue
    } else {
      backendAdjustmentType = 'FIXED'
      backendAdjustmentValue = adjustmentType === 'cheaper' ? -adjustmentValue : adjustmentValue
    }

    try {
      const response = await updateChannelPriceAdjustment(channelId, {
        adjustmentType: backendAdjustmentType,
        adjustmentValue: backendAdjustmentValue,
        autoSyncPrice,
      })

      if (response.success) {
        const saved = response.data
        const savedAdjustmentType = saved?.adjustmentType ?? backendAdjustmentType
        const savedAdjustmentValue = saved?.adjustmentValue ?? backendAdjustmentValue
        const newRatio = formatBackendRatioString(savedAdjustmentType, savedAdjustmentValue)
        const index = priceRatioData.value.findIndex((item) => item.channelId === channelId)
        if (index > -1) {
          priceRatioData.value[index] = {
            channelId,
            channelCode: saved?.channelCode || priceRatioData.value[index].channelCode,
            channel: saved?.channelName || editData.channel,
            ratio: newRatio,
            adjustmentType: savedAdjustmentType,
            adjustmentValue: savedAdjustmentValue,
            autoSyncPrice: saved?.autoSyncPrice ?? autoSyncPrice,
            suMappingMultiplier: saved?.suMappingMultiplier,
            suMappingSurcharge: saved?.suMappingSurcharge,
            suMappingSync: saved?.suMappingSync,
          }
        }
        showPriceRatioSyncMessage(saved?.suMappingSync)
        return true
      } else {
        ElMessage.error(t('channel.priceRatio.updateFailed'))
        return false
      }
    } catch (error) {
      console.error('更新价格比例失败:', error)
      ElMessage.error(t('channel.priceRatio.retryFailed'))
      return false
    }
  }

  const showPriceRatioSyncMessage = (syncSummary: ChannelPriceAdjustmentDTO['suMappingSync']) => {
    if (!syncSummary) {
      ElMessage.success(t('channel.priceRatio.updated'))
      return
    }

    if (syncSummary.status === 'SKIPPED') {
      ElMessage.success(t('channel.priceRatio.syncMessages.skipped'))
      return
    }

    if (syncSummary.status === 'SUCCESS') {
      ElMessage.success(t('channel.priceRatio.syncMessages.success'))
      return
    }

    if (syncSummary.status === 'PARTIAL') {
      ElMessage.warning(t('channel.priceRatio.syncMessages.partial'))
      return
    }

    if (syncSummary.status === 'PENDING') {
      ElMessage.warning(t('channel.priceRatio.syncMessages.pending'))
      return
    }

    ElMessage.warning(t('channel.priceRatio.syncMessages.failed'))
  }

  // ──────────── 工具函数 ────────────

  /** 格式化 PMS 房型显示名称（去掉括号内容） */
  const formatPmsRoomTypeDisplay = (value: string | null): string => {
    if (!value) return '-'
    const trimmed = value.trim()
    if (!trimmed) return '-'
    const idx1 = trimmed.indexOf('(')
    const idx2 = trimmed.indexOf('（')
    const idxCandidates = [idx1, idx2].filter((it) => it >= 0)
    if (idxCandidates.length === 0) return trimmed
    const idx = Math.min(...idxCandidates)
    const head = trimmed.slice(0, idx).trim()
    return head || trimmed
  }

  // ──────────── 映射数据扁平化 ────────────

  /** 将分组的映射数据扁平化为表格行 */
  const flattenMappingData = (groups: RoomMappingGroup[]): FlattenedMappingItem[] => {
    const flattened: FlattenedMappingItem[] = []
    groups.forEach((group) => {
      const groupRowCount = group.pricePlans.length
      group.pricePlans.forEach((plan, index) => {
        flattened.push({
          id: plan.id,
          roomGroupId: group.roomGroupId,
          channelRoomType: group.channelRoomType,
          channelRoomId: group.channelRoomId,
          pmsRoomType: group.pmsRoomType,
          selectedPmsRoom: group.selectedPmsRoom,
          channelPricePlan: plan.channelPricePlan,
          channelPricePlanId: plan.channelPricePlanId,
          pmsPricePlan: plan.pmsPricePlan,
          selectedPmsPricePlan: plan.selectedPmsPricePlan,
          status: plan.status,
          isFirstInGroup: index === 0,
          groupRowCount,
        })
      })
    })
    return flattened
  }

  const buildCalendarDates = (startDate: string, days = 8): CalendarDate[] => {
    const weekdayFormatter = new Intl.DateTimeFormat(locale.value, {
      weekday: 'short',
      timeZone: 'UTC',
    })

    return getStoreDateRange(startDate, days).map((value) => {
      const date = new Date(`${value}T00:00:00Z`)
      const { month, day } = formatYmdMonthDay(value)
      return {
        value,
        label: value,
        day: `${Number(month)}/${Number(day)}`,
        weekday: weekdayFormatter.format(date),
      }
    })
  }

  const pickRoomTypeForRoomId = (roomId: string, roomTypes: RoomTypeDTO[]): RoomTypeDTO | null => {
    if (roomTypes.length === 0) return null
    const normalizedRoomId = normalizeText(roomId)
    return (
      roomTypes.find((roomType) => {
        const candidates = [roomType.id, roomType.code, roomType.suRoomType, roomType.name].map(
          normalizeText,
        )
        return candidates.includes(normalizedRoomId)
      }) || null
    )
  }

  const pickPricePlanForRateId = (
    ratePlanId: string,
    pricePlans: PricePlanDTO[],
  ): PricePlanDTO | null => {
    if (pricePlans.length === 0) return null
    const normalizedRatePlanId = normalizeText(ratePlanId)
    if (!normalizedRatePlanId) return null
    return (
      pricePlans.find((plan) => {
        const candidates = [plan.id, plan.name, plan.nameEn].map(normalizeText)
        return candidates.includes(normalizedRatePlanId)
      }) || null
    )
  }

  const buildManagementMappings = (
    mappings: SuMappingEntry[],
    roomTypes: RoomTypeDTO[],
    pricePlans: PricePlanDTO[],
    channelCode: string,
  ): RoomMappingGroup[] => {
    const roomGroups: RoomMappingGroup[] = []

    mappings.forEach((mapping, mappingIndex) => {
      const roomIds = Array.isArray(mapping.RoomIDs) ? mapping.RoomIDs : []
      roomIds.forEach((roomId, roomIndex) => {
        const roomType = pickRoomTypeForRoomId(roomId, roomTypes)

        const ratePlans =
          Array.isArray(mapping.Rateplans) && mapping.Rateplans.length > 0
            ? mapping.Rateplans
            : [
                {
                  RatePlanID: t('channel.managementData.standardRate'),
                  MappingStatus: mapping.Status,
                },
              ]
        const pmsRoomType = roomType?.name || roomType?.code || null
        const channelRoomId = String(
          roomId || roomType?.suRoomType || roomType?.code || roomType?.id || '',
        )
        const channelRoomType = roomType
          ? `${roomType.name || roomType.code || roomType.id} (${channelRoomId})`
          : t('channel.managementData.unmappedChannelRoom', { id: channelRoomId || roomIndex + 1 })

        roomGroups.push({
          roomGroupId: `${channelCode}-${channelRoomId}-${mappingIndex}`,
          channelRoomType,
          channelRoomId,
          pmsRoomType,
          selectedPmsRoom: pmsRoomType,
          pricePlans: ratePlans.map((ratePlan, rateIndex) => {
            const rateId = String(ratePlan.RatePlanID || '')
            const pmsPlan = pickPricePlanForRateId(rateId, pricePlans)
            const pmsPlanName = pmsPlan?.name || pmsPlan?.nameEn || null
            const mappingActive = normalizeText(ratePlan.MappingStatus || mapping.Status) === 'active'
            const status = mappingActive && pmsRoomType && pmsPlanName ? 'connected' : 'disconnected'
            return {
              id: `${channelCode}-${channelRoomId}-${rateId || rateIndex}`,
              channelPricePlan: rateId || t('channel.managementData.standardRate'),
              channelPricePlanId: rateId || '',
              pmsPricePlan: pmsPlanName,
              selectedPmsPricePlan: pmsPlanName,
              status,
            }
          }),
        })
      })
    })

    return roomGroups
  }

  const buildCalendarRows = (
    roomTypes: RoomTypeDTO[],
    priceRows: RoomPriceManagementDTO[],
    dates: CalendarDate[],
  ): CalendarRow[] => {
    const rows: CalendarRow[] = []
    const priceByRoomAndDate = new Map<string, RoomPriceManagementDTO>()
    priceRows.forEach((row) => {
      priceByRoomAndDate.set(`${row.roomTypeId}-${row.priceDate}`, row)
    })

    roomTypes.forEach((roomType) => {
      const roomTypeId = roomType.id
      const roomLabel = `${roomType.name} (${roomType.code || roomTypeId})`
      rows.push({
        id: `room-type-${roomTypeId}-title`,
        label: roomLabel,
        type: 'title',
        roomId: String(roomTypeId),
        values: {},
      })

      const inventoryValues: Record<string, number> = {}
      const priceValues: Record<string, number> = {}
      const minStayValues: Record<string, number | string> = {}
      const maxStayValues: Record<string, number | string> = {}
      const closeRoomValues: Record<string, boolean> = {}
      const ctaValues: Record<string, boolean> = {}
      const ctdValues: Record<string, boolean> = {}

      dates.forEach((date) => {
        const priceRow = priceByRoomAndDate.get(`${roomTypeId}-${date.value}`)
        inventoryValues[date.value] = Number(priceRow?.availableRooms ?? roomType.totalRooms ?? 0)
        priceValues[date.value] = Number(priceRow?.price ?? roomType.defaultPrice ?? 0)
        minStayValues[date.value] = priceRow?.minStay ?? 1
        maxStayValues[date.value] = priceRow?.maxStay ?? 365
        closeRoomValues[date.value] = Boolean(priceRow?.closeRoom)
        ctaValues[date.value] = Boolean(priceRow?.cta)
        ctdValues[date.value] = Boolean(priceRow?.ctd)
      })

      rows.push({
        id: `room-type-${roomTypeId}-inventory`,
        label: t('channel.managementData.calendarRows.inventory'),
        type: 'inventory',
        roomId: String(roomTypeId),
        values: inventoryValues,
      })
      rows.push({
        id: `room-type-${roomTypeId}-price`,
        label: t('channel.managementData.calendarRows.price'),
        type: 'price',
        roomId: String(roomTypeId),
        values: priceValues,
      })
      rows.push({
        id: `room-type-${roomTypeId}-min-stay`,
        label: t('channel.managementData.calendarRows.minStay'),
        type: 'number',
        roomId: String(roomTypeId),
        values: minStayValues,
      })
      rows.push({
        id: `room-type-${roomTypeId}-max-stay`,
        label: t('channel.managementData.calendarRows.maxStay'),
        type: 'number',
        roomId: String(roomTypeId),
        values: maxStayValues,
      })
      rows.push({
        id: `room-type-${roomTypeId}-close-room`,
        label: t('channel.managementData.calendarRows.closeRoom'),
        type: 'checkbox',
        roomId: String(roomTypeId),
        values: closeRoomValues,
      })
      rows.push({
        id: `room-type-${roomTypeId}-cta`,
        label: 'CTA',
        type: 'checkbox',
        roomId: String(roomTypeId),
        values: ctaValues,
      })
      rows.push({
        id: `room-type-${roomTypeId}-ctd`,
        label: 'CTD',
        type: 'checkbox',
        roomId: String(roomTypeId),
        values: ctdValues,
      })
    })

    return rows
  }

  const buildRoomSettingsRows = (
    roomMappings: RoomMappingGroup[],
    priceRows: RoomPriceManagementDTO[],
    dates: CalendarDate[],
  ): RoomSettingsRow[] => {
    const priceByRoomAndDate = new Map<string, RoomPriceManagementDTO>()
    priceRows.forEach((row) => {
      priceByRoomAndDate.set(`${row.roomTypeName}-${row.priceDate}`, row)
    })

    return roomMappings.map((mapping) => {
      const values: Record<string, number | string> = {}
      dates.forEach((date) => {
        const priceRow = priceByRoomAndDate.get(`${mapping.pmsRoomType}-${date.value}`)
        values[date.value] = priceRow?.availableRooms ?? ''
      })
      return {
        id: mapping.roomGroupId,
        airbnbRoomType: mapping.channelRoomType,
        pmsRoomType: mapping.pmsRoomType || '-',
        values,
      }
    })
  }

  const buildConnectedChannelManagementData = async (
    channel: ChannelItem,
    startDate = getStoreTodayYmd(),
  ): Promise<ChannelManagementData> => {
    const channelId = resolveSuChannelId(channel.code)
    const currentStore = readCurrentStore()
    const dates = buildCalendarDates(startDate)
    const endDate = dates[dates.length - 1]?.value || startDate

    const [integrationResp, roomTypesResp, roomsResp, pricePlansResp, priceRowsResp, mappingsResp] =
      await Promise.all([
        getOtaIntegrationById(channel.id).catch(() => null),
        getAllRoomTypes().catch(() => null),
        getRooms().catch(() => null),
        getCurrentStorePricePlans().catch(() => null),
        getRoomPriceManagementData(startDate, endDate).catch(() => null),
        channelId ? getSuMappings(channel.id, channelId).catch(() => null) : Promise.resolve(null),
      ])

    const integration = integrationResp?.success ? integrationResp.data : null
    const roomTypes =
      roomTypesResp?.success && Array.isArray(roomTypesResp.data) ? roomTypesResp.data : []
    const rooms = roomsResp?.success && Array.isArray(roomsResp.data) ? roomsResp.data : []
    const pricePlansPayload = pricePlansResp as any
    const pricePlans =
      pricePlansPayload?.success && Array.isArray(pricePlansPayload.data)
        ? (pricePlansPayload.data as PricePlanDTO[])
        : []
    const priceRows =
      priceRowsResp?.success && Array.isArray(priceRowsResp.data) ? priceRowsResp.data : []
    const mappings =
      channelId && mappingsResp?.success && Array.isArray(mappingsResp.data?.[channelId])
        ? (mappingsResp.data[channelId] as SuMappingEntry[])
        : []

    const propertyId = integration?.propertyId || currentStore?.suHotelId || channel.code
    const storeName = currentStore?.name || propertyId || channel.name

    const hotels: HotelItem[] = [
      {
        id: Number(currentStore?.id || channel.id),
        hotelCode: propertyId || channel.code,
        hotelName: `${storeName} (${t('channel.managementData.localPmsData')})`,
        priceMode: t('channel.managementData.rateModePms'),
        status: channel.connected
          ? t('channel.managementData.statusActive')
          : t('channel.managementData.statusInactive'),
      },
    ]

    const airbnbAccounts: AirbnbAccount[] =
      channel.code.toUpperCase() === 'AIRBNB'
        ? [
            {
              id: channel.id,
              account: `${storeName} ${t('channel.managementData.accountSuffix')} (${t('channel.managementData.localPmsData')})`,
              accountCode: propertyId || channel.code,
            },
          ]
        : []

    const roomMappings = buildManagementMappings(mappings, roomTypes, pricePlans, channel.code)
    const calendarData = buildCalendarRows(roomTypes, priceRows, dates)
    const roomSettingsData =
      channel.code.toUpperCase() === 'AIRBNB'
        ? buildRoomSettingsRows(roomMappings, priceRows, dates)
        : []

    const pricePlanOptions = pricePlans
      .map((plan) => (plan.name || plan.nameEn || '').trim())
      .filter((name) => !!name)
      .map((name) => ({ value: name, label: name }))
    if (pricePlanOptions.length > 0) {
      pmsPricePlanOptions.value = pricePlanOptions
    }

    if (rooms.length > 0) {
      const roomTypeNames = Array.from(
        new Set(
          rooms.map((room: RoomDTO) => room.roomType?.name || '').filter((name: string) => !!name),
        ),
      )
      if (roomTypeNames.length > 0) {
        pmsRoomOptions.value = roomTypeNames.map((name) => ({ value: name, label: name }))
      }
    }

    return {
      hotels,
      airbnbAccounts,
      roomMappings,
      calendarDates: dates,
      calendarData,
      roomSettingsDates: dates,
      roomSettingsData,
      selectedHotelId: hotels[0]?.id || null,
    }
  }

  return {
    // OTA 渠道
    otaChannels,
    channelsLoading,
    loadChannels,
    // PMS
    pmsRoomOptions,
    pmsPricePlanOptions,
    loadPmsRoomOptions,
    loadPmsPricePlanOptions,
    // 价格比例
    priceRatioData,
    priceRatioLoading,
    loadPriceRatioData,
    formatRatioString,
    savePriceRatio,
    // 工具
    formatPmsRoomTypeDisplay,
    flattenMappingData,
    buildConnectedChannelManagementData,
  }
}
