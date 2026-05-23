import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllOtaIntegrations, getSuMappingStatus, type OtaIntegrationDTO } from '@/api/otaIntegration'
import {
  getChannelPriceAdjustments,
  updateChannelPriceAdjustment,
  type ChannelPriceAdjustmentDTO,
  type PriceAdjustmentType,
} from '@/api/pricelabs'
import { getAllRoomTypes } from '@/api/roomType'

import type {
  ChannelItem,
  PriceRatioItem,
  PriceRatioEdit,
  SelectOption,
  FlattenedMappingItem,
  RoomMappingGroup,
} from '../types'
import { CHANNEL_LOGO_MAP } from '../constants'

/**
 * 渠道管理页面共享业务逻辑
 */
export function useChannelData() {
  // ──────────── OTA 渠道列表 ────────────
  const otaChannels = ref<ChannelItem[]>([])
  const channelsLoading = ref(false)

  const loadChannels = async () => {
    channelsLoading.value = true
    try {
      const response = await getAllOtaIntegrations()
      if (response.success && response.data) {
        const resolveSuChannelId = (code: string): string | null => {
          const normalized = (code || '').trim().toUpperCase()
          if (normalized === 'BOOKING' || normalized === 'BOOKING.COM') return '19'
          if (normalized === 'AIRBNB') return '244'
          return null
        }

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
      ElMessage.error('加载OTA直连渠道数据失败')
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
  const pmsPricePlanOptions = ref<SelectOption[]>([
    { value: 'Rack Rate', label: 'Rack Rate' },
    { value: 'Standard Rate', label: 'Standard Rate' },
  ])

  // ──────────── 价格比例 ────────────
  const priceRatioData = ref<PriceRatioItem[]>([])
  const priceRatioLoading = ref(false)

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
          const adjustmentValue = item.adjustmentValue ?? 0
          let ratio: string

          if (adjustmentValue === 0) {
            ratio = '等同于基本价格'
          } else {
            const unit = item.adjustmentType === 'FIXED' ? '¥' : '%'
            const typeText = adjustmentValue > 0 ? '贵' : '便宜'
            ratio = `${Math.abs(adjustmentValue)} ${unit} 比基准价${typeText}`
          }

          return {
            channelId: item.channelId,
            channel: item.channelName,
            ratio,
            adjustmentType: item.adjustmentType ?? 'PERCENTAGE',
            adjustmentValue: item.adjustmentValue ?? null,
            autoSyncPrice: item.autoSyncPrice ?? true,
          }
        })
      }
    } catch (error) {
      console.error('加载价格比例数据失败:', error)
      ElMessage.error('加载价格比例数据失败')
    } finally {
      priceRatioLoading.value = false
    }
  }

  // ──────────── 价格比例解析 / 格式化 ────────────

  /** 解析现有 ratio 字符串 */
  const parseRatioString = (
    ratio: string,
  ): { type: 'cheaper' | 'expensive'; value: number; unit: '%' | '¥' } => {
    const defaultResult = { type: 'cheaper' as const, value: 0, unit: '%' as const }
    if (ratio === '等同于基本价格') return defaultResult

    const match = ratio.match(/(\d+)\s*([%¥])\s*比基准价(贵|便宜)/)
    if (match) {
      return {
        type: match[3] === '贵' ? 'expensive' : 'cheaper',
        value: parseInt(match[1], 10),
        unit: match[2] as '%' | '¥',
      }
    }
    return defaultResult
  }

  /** 将编辑态转为显示字符串 */
  const formatRatioString = (
    type: 'cheaper' | 'expensive',
    value: number,
    unit: '%' | '¥',
  ): string => {
    if (value === 0) return '等同于基本价格'
    const typeText = type === 'expensive' ? '贵' : '便宜'
    return `${value} ${unit} 比基准价${typeText}`
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
        const newRatio = formatRatioString(adjustmentType, adjustmentValue, adjustmentUnit)
        const index = priceRatioData.value.findIndex((item) => item.channelId === channelId)
        if (index > -1) {
          priceRatioData.value[index] = {
            channelId,
            channel: editData.channel,
            ratio: newRatio,
            adjustmentType: backendAdjustmentType,
            adjustmentValue: backendAdjustmentValue,
            autoSyncPrice,
          }
        }
        ElMessage.success('价格比例已更新')
        return true
      } else {
        ElMessage.error(response.message || '更新失败')
        return false
      }
    } catch (error) {
      console.error('更新价格比例失败:', error)
      ElMessage.error('更新价格比例失败，请重试')
      return false
    }
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

  return {
    // OTA 渠道
    otaChannels,
    channelsLoading,
    loadChannels,
    // PMS
    pmsRoomOptions,
    pmsPricePlanOptions,
    loadPmsRoomOptions,
    // 价格比例
    priceRatioData,
    priceRatioLoading,
    loadPriceRatioData,
    parseRatioString,
    formatRatioString,
    savePriceRatio,
    // 工具
    formatPmsRoomTypeDisplay,
    flattenMappingData,
  }
}
