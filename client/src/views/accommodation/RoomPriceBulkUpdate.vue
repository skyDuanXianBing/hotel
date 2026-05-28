<template>
  <div class="bulk-update-page">
    <div class="page-tabs">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane :label="t('accommodation.roomPriceBulk.tabCalendar')" name="calendar"></el-tab-pane>
        <el-tab-pane :label="t('accommodation.roomPriceBulk.tabBulkUpdate')" name="bulk-update"></el-tab-pane>
      </el-tabs>
    </div>

    <el-form label-width="110px" class="bulk-form">
      <el-form-item :label="t('accommodation.roomPriceBulk.roomTypeAndPrice')" required class="form-item room-type-select-wrapper">
        <div class="selected-tags-area" @click="toggleTreeDropdown">
          <span v-if="selectedTags.length === 0" class="placeholder-text">{{ t('accommodation.roomPriceBulk.placeholders.select') }}</span>

          <el-tag
            v-for="tag in selectedTags"
            :key="tag.value"
            closable
            @close.stop="removeTag(tag.value)"
            class="selected-tag"
          >
            {{ tag.label }}
          </el-tag>

          <el-icon class="dropdown-arrow" :class="{ 'is-reverse': showTreeDropdown }">
            <ArrowDown />
          </el-icon>
        </div>

        <transition name="dropdown">
          <div v-show="showTreeDropdown" class="tree-select-dropdown">
            <el-input v-model="filterText" :placeholder="t('accommodation.roomPriceBulk.searchRoomTypePrice')" class="filter-input" />
            <el-tree
              ref="treeRef"
              :data="treeData"
              show-checkbox
              node-key="value"
              :default-checked-keys="selectedPlanIds"
              :props="{ children: 'children', label: 'label' }"
              :filter-node-method="filterNode"
              @check="handleTreeCheck"
              class="selection-tree"
            />
            <div class="tree-footer">
              <el-button size="small" @click="clearSelection">{{ t('accommodation.roomPriceBulk.clear') }}</el-button>
              <el-button size="small" type="primary" @click="showTreeDropdown = false">{{ t('accommodation.roomPriceBulk.done') }}</el-button>
            </div>
          </div>
        </transition>
      </el-form-item>

      <el-form-item :label="t('accommodation.common.dateRange')" required class="form-item">
        <div class="date-range-wrapper">
          <el-date-picker
            v-model="startDate"
            type="date"
            :placeholder="t('accommodation.common.startDate')"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            size="default"
          />
          <span class="date-separator">-</span>
          <el-date-picker
            v-model="endDate"
            type="date"
            :placeholder="t('accommodation.common.endDate')"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            size="default"
          />
        </div>
      </el-form-item>

      <el-form-item :label="t('accommodation.common.weekdays')" class="form-item">
        <div class="weekday-selector">
          <el-checkbox-group v-model="selectedWeekdays" class="weekday-group" @change="handleWeekdayChange">
            <el-checkbox v-for="weekday in weekdayOptions" :key="weekday.value" :label="weekday.value">
              {{ weekday.label }}
            </el-checkbox>
          </el-checkbox-group>
          <el-button text type="primary" @click="invertWeekdaySelection">{{ t('accommodation.roomPrice.invertSelection') }}</el-button>
        </div>
      </el-form-item>

      <el-form-item :label="t('accommodation.roomPriceBulk.setting')" required class="form-item">
        <el-select v-model="settingType" :placeholder="t('accommodation.common.select')" style="width: 200px">
          <el-option
            v-for="option in bulkSettingOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item v-if="settingType === 'price'" :label="t('accommodation.roomPriceBulk.batchValue')" class="form-item">
        <div class="value-batch-wrapper">
          <el-radio-group v-model="priceMode" class="price-mode-group">
            <el-radio value="fixed" :label="t('accommodation.roomPriceBulk.fixedValue')" />
            <el-radio value="relative" :label="t('accommodation.roomPriceBulk.basedOnCurrent')" />
          </el-radio-group>

          <div v-if="priceMode === 'relative'" class="relative-price-options">
            <el-radio-group v-model="relativeType" class="relative-type-group">
              <el-radio value="cheaper" :label="t('accommodation.roomPriceBulk.decrease')" />
              <el-radio value="expensive" :label="t('accommodation.roomPriceBulk.increase')" />
            </el-radio-group>
            <el-radio-group v-model="relativeValueMode" class="relative-type-group">
              <el-radio value="amount" :label="t('accommodation.roomPriceBulk.byAmount')" />
              <el-radio value="percent" :label="t('accommodation.roomPriceBulk.byPercent')" />
            </el-radio-group>
          </div>

          <div class="batch-input-row">
            <el-input v-model="batchValue" type="number" :placeholder="valuePlaceholder" style="width: 260px">
              <template #append>{{ batchValueUnit }}</template>
            </el-input>
            <span class="auto-apply-hint">{{ t('accommodation.roomPriceBulk.autoApplyHint') }}</span>
          </div>
        </div>
      </el-form-item>

      <el-form-item v-else :label="t('accommodation.roomPriceBulk.batchValue')" class="form-item">
        <div class="batch-input-row">
          <el-input v-model="batchValue" type="number" :placeholder="valuePlaceholder" style="width: 260px">
            <template #append>{{ batchValueUnit }}</template>
          </el-input>
          <span class="auto-apply-hint">{{ t('accommodation.roomPriceBulk.autoApplyHint') }}</span>
        </div>
      </el-form-item>

      <div class="table-section">
        <div v-if="tableRows.length === 0" class="empty-state">
          <el-icon :size="48" color="#c0c4cc">
            <Document />
          </el-icon>
          <p class="empty-text">{{ t('accommodation.roomPriceBulk.selectRoomTypePriceFirst') }}</p>
        </div>
        <el-table v-else :data="tableRows" border style="width: 100%">
          <el-table-column prop="roomTypeName" :label="t('accommodation.roomPriceBulk.table.roomType')" min-width="180" />
          <el-table-column prop="pricePlanName" :label="t('accommodation.roomPriceBulk.table.pricePlan')" min-width="180" />
          <el-table-column :label="t('accommodation.roomPriceBulk.table.value')" min-width="220">
            <template #default="{ row }">
              <div v-if="showRelativePreviewColumn" class="value-range-preview">
                <template v-if="relativePreviewByKey[row.key]">
                  <div class="range-line">
                    {{ t('accommodation.roomPriceBulk.table.currentRange', {
                      value: formatRangeText(
                        relativePreviewByKey[row.key].currentMin,
                        relativePreviewByKey[row.key].currentMax,
                      )
                    }) }}
                  </div>
                  <div class="range-line range-line-result">
                    {{ t('accommodation.roomPriceBulk.table.resultRange', {
                      value: formatRangeText(
                        relativePreviewByKey[row.key].adjustedMin,
                        relativePreviewByKey[row.key].adjustedMax,
                      )
                    }) }}
                  </div>
                  <div
                    v-for="line in channelRangePreviewByKey[row.key] || []"
                    :key="`${row.key}-${line}`"
                    class="range-line channel-range-line"
                  >
                    {{ line }}
                  </div>
                </template>
                <span v-else class="range-empty">{{ t('accommodation.roomPriceBulk.table.rangeEmpty') }}</span>
              </div>
              <template v-else>
                <el-input v-model="row.value" type="number" :placeholder="valuePlaceholder">
                  <template #append>{{ batchValueUnit }}</template>
                </el-input>
                <div v-if="settingType === 'price'" class="value-range-preview channel-preview-fixed">
                  <div
                    v-for="line in channelRangePreviewByKey[row.key] || []"
                    :key="`${row.key}-${line}`"
                    class="range-line channel-range-line"
                  >
                    {{ line }}
                  </div>
                </div>
              </template>
            </template>
          </el-table-column>
          <el-table-column :label="t('accommodation.roomPriceBulk.table.action')" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeRow(row.key)">{{ t('accommodation.roomPriceBulk.table.remove') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="footer-actions">
        <el-button @click="handleCancel" size="large">{{ t('accommodation.common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave" size="large" :loading="saving">
          {{ t('accommodation.common.save') }}
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowDown, Document } from '@element-plus/icons-vue'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { getPricePlansByRoomType, type RoomTypePricePlanDTO } from '@/api/pricePlan'
import {
  getRoomPriceManagementData,
  updatePriceByPlan,
  type RoomPriceManagementDTO,
  type UpdatePriceByPlanRequest,
} from '@/api/roomPrice'
import {
  getChannelPriceAdjustments,
  type ChannelPriceAdjustmentDTO,
  type PriceAdjustmentType,
} from '@/api/pricelabs'
import { useUserStore } from '@/stores/user'
import { useAccommodationI18n } from '@/composables/useAccommodationI18n'

type TreeNode = {
  value: string
  label: string
  children?: TreeNode[]
}

type LeafMeta = {
  roomTypeId: number
  roomTypeName: string
  roomTypeCode: string
  pricePlanId: number
  pricePlanName: string
}

type TableRow = {
  key: string
  roomTypeId: number
  pricePlanId: number
  roomTypeName: string
  pricePlanName: string
  value: string
}

type RelativeRangePreview = {
  currentMin: number
  currentMax: number
  adjustedMin: number
  adjustedMax: number
}

type ChannelAdjustmentPreview = {
  channelName: string
  adjustmentType: PriceAdjustmentType
  adjustmentValue: number
}

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()
const { weekdayOptions, bulkSettingOptions } = useAccommodationI18n()

const activeTab = ref('bulk-update')
const saving = ref(false)

const showTreeDropdown = ref(false)
const filterText = ref('')
const treeRef = ref()

const treeData = ref<TreeNode[]>([])
const leafMetaByKey = ref<Record<string, LeafMeta>>({})
const selectedPlanIds = ref<string[]>([])

const selectedTags = computed(() => {
  return selectedPlanIds.value.map((key) => {
    const meta = leafMetaByKey.value[key]
    return {
      value: key,
      label: meta ? `${meta.roomTypeName}_${meta.pricePlanName}` : key,
    }
  })
})

const startDate = ref('')
const endDate = ref('')

const selectedWeekdays = ref<number[]>([])
let previousWeekdays: number[] = []
const ALL_WEEKDAYS = [1, 2, 3, 4, 5, 6, 7] as const

const settingType = ref<'price' | 'minStay' | 'maxStay'>('price')

const priceMode = ref<'fixed' | 'relative'>('fixed')
const relativeType = ref<'cheaper' | 'expensive'>('cheaper')
const relativeValueMode = ref<'amount' | 'percent'>('amount')

const batchValue = ref('')

const tableRows = ref<TableRow[]>([])
const rowValueByKey = ref<Record<string, string>>({})
const relativePreviewByKey = ref<Record<string, RelativeRangePreview>>({})
const relativeDateValuesByKey = ref<Record<string, Record<string, number>>>({})
const channelAdjustments = ref<ChannelAdjustmentPreview[]>([])
const PRICE_RATIO_VISIBLE_CHANNEL_CODES = new Set(['AIRBNB', 'BOOKING'])

const AUTO_APPLY_DEBOUNCE_MS = 300
let autoApplyTimer: ReturnType<typeof setTimeout> | null = null
let relativeApplyRequestSeq = 0

const valuePlaceholder = computed(() => {
  return settingType.value === 'price'
    ? t('accommodation.roomPriceBulk.placeholders.inputPrice')
    : t('accommodation.roomPriceBulk.placeholders.inputDays')
})

const showRelativePreviewColumn = computed(() => {
  return settingType.value === 'price' && priceMode.value === 'relative'
})

const batchValueUnit = computed(() => {
  if (settingType.value !== 'price') {
    return t('accommodation.common.dayUnit')
  }
  if (priceMode.value === 'relative' && relativeValueMode.value === 'percent') {
    return '%'
  }
  return t('accommodation.common.jpy')
})

const formatPriceNumber = (value: number) => {
  const rounded = Math.round(value * 100) / 100
  if (Number.isInteger(rounded)) {
    return String(rounded)
  }
  return rounded.toFixed(2).replace(/\.?0+$/, '')
}

const formatRangeText = (min: number, max: number) => {
  const minText = formatPriceNumber(min)
  const maxText = formatPriceNumber(max)
  if (minText === maxText) {
    return `${minText} JPY`
  }
  return `${minText} - ${maxText} JPY`
}

const clampPrice = (value: number) => Math.max(0, Math.round(value * 100) / 100)

const applyChannelAdjustment = (price: number, adjustment: ChannelAdjustmentPreview) => {
  if (adjustment.adjustmentType === 'FIXED') {
    return clampPrice(price + adjustment.adjustmentValue)
  }
  const ratio = 1 + adjustment.adjustmentValue / 100
  return clampPrice(price * ratio)
}

const getRowAdjustedRange = (row: TableRow): { min: number; max: number } | null => {
  if (settingType.value !== 'price') {
    return null
  }

  if (showRelativePreviewColumn.value) {
    const preview = relativePreviewByKey.value[row.key]
    if (!preview) {
      return null
    }
    return { min: preview.adjustedMin, max: preview.adjustedMax }
  }

  const value = Number(row.value)
  if (!Number.isFinite(value) || value <= 0) {
    return null
  }
  const clamped = clampPrice(value)
  return { min: clamped, max: clamped }
}

const channelRangePreviewByKey = computed<Record<string, string[]>>(() => {
  const result: Record<string, string[]> = {}
  if (settingType.value !== 'price' || channelAdjustments.value.length === 0) {
    return result
  }

  tableRows.value.forEach((row) => {
    const range = getRowAdjustedRange(row)
    if (!range) {
      result[row.key] = []
      return
    }

    result[row.key] = channelAdjustments.value.map((adjustment) => {
      const adjustedMin = applyChannelAdjustment(range.min, adjustment)
      const adjustedMax = applyChannelAdjustment(range.max, adjustment)
      const min = Math.min(adjustedMin, adjustedMax)
      const max = Math.max(adjustedMin, adjustedMax)
      return `${adjustment.channelName}: ${formatRangeText(min, max)}`
    })
  })

  return result
})

const toWeekdayValue = (dateText: string): number => {
  const day = new Date(`${dateText}T00:00:00`).getDay()
  return day === 0 ? 7 : day
}

const isDateMatchedByWeekday = (dateText: string): boolean => {
  const days = normalizeWeekdaysPayload()
  if (!days || days.length === 0 || days.includes(0)) {
    return true
  }
  return days.includes(toWeekdayValue(dateText))
}

const calculateAdjustedPrice = (current: number, inputValue: number): number => {
  const delta = relativeValueMode.value === 'percent' ? current * (inputValue / 100) : inputValue
  const adjusted = relativeType.value === 'cheaper' ? current - delta : current + delta
  return Math.max(0, Math.round(adjusted * 100) / 100)
}

const handleTabClick = (tab: any) => {
  if (tab.props.name === 'calendar') {
    router.push('/accommodation/room-price-management')
  }
}

const clearAutoApplyTimer = () => {
  if (autoApplyTimer) {
    clearTimeout(autoApplyTimer)
    autoApplyTimer = null
  }
}

const scheduleAutoApply = () => {
  clearAutoApplyTimer()
  autoApplyTimer = setTimeout(() => {
    void applyBatchValue({ silent: true })
  }, AUTO_APPLY_DEBOUNCE_MS)
}

const toggleTreeDropdown = () => {
  showTreeDropdown.value = !showTreeDropdown.value
}

const filterNode = (value: string, data: any) => {
  if (!value) return true
  return String(data?.label || '').toLowerCase().includes(value.toLowerCase())
}

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

const rebuildTableRows = (keys: string[]) => {
  const rows: TableRow[] = []
  const keep: Record<string, string> = {}
  const previewKeep: Record<string, RelativeRangePreview> = {}
  const dateValuesKeep: Record<string, Record<string, number>> = {}

  for (const key of keys) {
    const meta = leafMetaByKey.value[key]
    if (!meta) continue

    const oldValue = rowValueByKey.value[key]
    const value = oldValue != null ? oldValue : ''
    keep[key] = value

    rows.push({
      key,
      roomTypeId: meta.roomTypeId,
      pricePlanId: meta.pricePlanId,
      roomTypeName: meta.roomTypeName,
      pricePlanName: meta.pricePlanName,
      value,
    })

    if (relativePreviewByKey.value[key]) {
      previewKeep[key] = relativePreviewByKey.value[key]
    }
    if (relativeDateValuesByKey.value[key]) {
      dateValuesKeep[key] = relativeDateValuesByKey.value[key]
    }
  }

  tableRows.value = rows
  rowValueByKey.value = keep
  relativePreviewByKey.value = previewKeep
  relativeDateValuesByKey.value = dateValuesKeep
  scheduleAutoApply()
}

const handleTreeCheck = () => {
  const checkedKeys = (treeRef.value?.getCheckedKeys() as string[]) || []
  const leafKeys = checkedKeys.filter((key) => /^\d+-\d+$/.test(key))
  selectedPlanIds.value = leafKeys
  rebuildTableRows(leafKeys)
}

const removeTag = (key: string) => {
  const idx = selectedPlanIds.value.indexOf(key)
  if (idx < 0) return
  selectedPlanIds.value.splice(idx, 1)
  treeRef.value?.setCheckedKeys(selectedPlanIds.value)
  rebuildTableRows(selectedPlanIds.value)
}

const removeRow = (key: string) => {
  removeTag(key)
}

const clearSelection = () => {
  selectedPlanIds.value = []
  treeRef.value?.setCheckedKeys([])
  rebuildTableRows([])
}

const handleWeekdayChange = (values: number[]) => {
  const hadAll = previousWeekdays.includes(0)
  const hasAll = values.includes(0)

  if (hasAll && !hadAll) {
    selectedWeekdays.value = [0, ...ALL_WEEKDAYS]
  } else if (!hasAll && hadAll) {
    selectedWeekdays.value = []
  } else if (hasAll && hadAll && values.length < previousWeekdays.length) {
    selectedWeekdays.value = values.filter((v) => v !== 0)
  } else if (!hasAll && ALL_WEEKDAYS.every((v) => values.includes(v))) {
    selectedWeekdays.value = [0, ...ALL_WEEKDAYS]
  }

  nextTick(() => {
    previousWeekdays = [...selectedWeekdays.value]
  })
}

const invertWeekdaySelection = () => {
  const explicitSelected = selectedWeekdays.value.filter((value) => value !== 0)
  const inverted = ALL_WEEKDAYS.filter((value) => !explicitSelected.includes(value))
  selectedWeekdays.value = inverted.length === ALL_WEEKDAYS.length ? [0, ...ALL_WEEKDAYS] : inverted
  previousWeekdays = [...selectedWeekdays.value]
}

const applyBatchValue = async ({ silent = false }: { silent?: boolean } = {}) => {
  if (!batchValue.value) {
    relativePreviewByKey.value = {}
    relativeDateValuesByKey.value = {}
    if (!silent) {
    ElMessage.warning(t('accommodation.roomPriceBulk.messages.inputValueFirst'))
    }
    return
  }

  if (settingType.value === 'price') {
    const v = parseFloat(batchValue.value)
    if (Number.isNaN(v) || v <= 0) {
      relativePreviewByKey.value = {}
      relativeDateValuesByKey.value = {}
      if (!silent) {
        ElMessage.warning(t('accommodation.roomPriceBulk.messages.invalidPrice'))
      }
      return
    }

    if (priceMode.value === 'fixed') {
      tableRows.value.forEach((row) => {
        row.value = batchValue.value
        rowValueByKey.value[row.key] = row.value
      })
      relativePreviewByKey.value = {}
      relativeDateValuesByKey.value = {}
      if (!silent) {
        ElMessage.success(t('accommodation.roomPriceBulk.messages.applied'))
      }
      return
    }

    if (!startDate.value || !endDate.value) {
      relativePreviewByKey.value = {}
      relativeDateValuesByKey.value = {}
      if (!silent) {
        ElMessage.warning(t('accommodation.roomPriceBulk.messages.selectDateRangeFirst'))
      }
      return
    }
    if (startDate.value > endDate.value) {
      relativePreviewByKey.value = {}
      relativeDateValuesByKey.value = {}
      if (!silent) {
        ElMessage.warning(t('accommodation.roomPriceBulk.messages.invalidDateRange'))
      }
      return
    }

    try {
      const currentRequestSeq = ++relativeApplyRequestSeq
      const resp = (await getRoomPriceManagementData(startDate.value, endDate.value)) as any
      if (currentRequestSeq !== relativeApplyRequestSeq) {
        return
      }
      const managementRows: RoomPriceManagementDTO[] = resp?.data || []

      const previewMap: Record<string, RelativeRangePreview> = {}
      const dateValuesMap: Record<string, Record<string, number>> = {}

      tableRows.value.forEach((row) => {
        const matched = managementRows.filter((item) => {
          return (
            item.roomTypeId === row.roomTypeId &&
            item.pricePlanId === row.pricePlanId &&
            !!item.priceDate &&
            isDateMatchedByWeekday(item.priceDate)
          )
        })

        if (matched.length === 0) {
          return
        }

        const currentValues: number[] = []
        const adjustedValues: number[] = []
        const valuesByDate: Record<string, number> = {}

        matched.forEach((item) => {
          const current = Number(item.price)
          if (Number.isNaN(current)) {
            return
          }
          const adjusted = calculateAdjustedPrice(current, v)
          currentValues.push(current)
          adjustedValues.push(adjusted)
          valuesByDate[item.priceDate] = adjusted
        })

        if (currentValues.length === 0 || adjustedValues.length === 0) {
          return
        }

        previewMap[row.key] = {
          currentMin: Math.min(...currentValues),
          currentMax: Math.max(...currentValues),
          adjustedMin: Math.min(...adjustedValues),
          adjustedMax: Math.max(...adjustedValues),
        }
        dateValuesMap[row.key] = valuesByDate

        row.value = ''
        rowValueByKey.value[row.key] = ''
      })

      relativePreviewByKey.value = previewMap
      relativeDateValuesByKey.value = dateValuesMap

      if (Object.keys(previewMap).length === 0) {
        if (!silent) {
          ElMessage.warning(t('accommodation.roomPriceBulk.messages.noPriceDataInRange'))
        }
        return
      }

      if (!silent) {
        ElMessage.success(t('accommodation.roomPriceBulk.messages.applied'))
      }
    } catch (error) {
      console.error('获取当前区间价格失败:', error)
      if (!silent) {
        ElMessage.error(t('accommodation.roomPriceBulk.messages.saveFailed'))
      }
    }
    return
  }

  const days = parseInt(batchValue.value, 10)
  if (Number.isNaN(days) || days < 1 || days > 99) {
    if (!silent) {
      ElMessage.warning(t('accommodation.roomPriceBulk.messages.invalidDays'))
    }
    return
  }

  tableRows.value.forEach((row) => {
    row.value = String(days)
    rowValueByKey.value[row.key] = row.value
  })
  if (!silent) {
    ElMessage.success(t('accommodation.roomPriceBulk.messages.applied'))
  }
}

const normalizeWeekdaysPayload = () => {
  const days = selectedWeekdays.value || []
  if (days.length === 0) return undefined
  if (days.includes(0)) return [0]
  return [...days].sort((a, b) => a - b)
}

const validateRowValue = (row: TableRow): string | null => {
  if (showRelativePreviewColumn.value) {
    if (!relativePreviewByKey.value[row.key]) {
      return t('accommodation.roomPriceBulk.validation.relativePreviewMissing', {
        roomType: row.roomTypeName,
        pricePlan: row.pricePlanName,
      })
    }
    const dateValues = relativeDateValuesByKey.value[row.key]
    if (!dateValues || Object.keys(dateValues).length === 0) {
      return t('accommodation.roomPriceBulk.validation.relativeResultMissing', {
        roomType: row.roomTypeName,
        pricePlan: row.pricePlanName,
      })
    }
    return null
  }

  if (!row.value) {
    return t('accommodation.roomPriceBulk.validation.valueRequired', {
      roomType: row.roomTypeName,
      pricePlan: row.pricePlanName,
    })
  }
  if (settingType.value === 'price') {
    const v = parseFloat(row.value)
    if (Number.isNaN(v) || v <= 0) {
      return t('accommodation.roomPriceBulk.validation.invalidPrice', {
        roomType: row.roomTypeName,
        pricePlan: row.pricePlanName,
      })
    }
    return null
  }

  const days = parseInt(row.value, 10)
  if (Number.isNaN(days) || days < 1 || days > 99) {
    return t('accommodation.roomPriceBulk.validation.invalidDays', {
      roomType: row.roomTypeName,
      pricePlan: row.pricePlanName,
    })
  }
  return null
}

const loadChannelAdjustments = async () => {
  try {
    const response = await getChannelPriceAdjustments()
    if (!response.success || !Array.isArray(response.data)) {
      channelAdjustments.value = []
      return
    }

    channelAdjustments.value = response.data
      .filter(
        (item: ChannelPriceAdjustmentDTO) =>
          Boolean(item.channelName && item.channelName.trim()) &&
          PRICE_RATIO_VISIBLE_CHANNEL_CODES.has(String(item.channelCode || '').toUpperCase()),
      )
      .map((item: ChannelPriceAdjustmentDTO) => ({
        channelName: item.channelName.toLowerCase(),
        adjustmentType: item.adjustmentType,
        adjustmentValue: item.adjustmentValue ?? 0,
      }))
  } catch (error) {
    console.error('加载渠道价格比例失败:', error)
    channelAdjustments.value = []
  }
}

const handleCancel = () => {
  router.push('/accommodation/room-price-management')
}

const handleSave = async () => {
  if (selectedPlanIds.value.length === 0 || tableRows.value.length === 0) {
    ElMessage.warning(t('accommodation.roomPriceBulk.messages.selectRoomTypePrice'))
    return
  }
  if (!startDate.value || !endDate.value) {
    ElMessage.warning(t('accommodation.roomPriceBulk.messages.selectDateRange'))
    return
  }
  if (startDate.value > endDate.value) {
    ElMessage.warning(t('accommodation.roomPriceBulk.messages.invalidDateRange'))
    return
  }

  for (const row of tableRows.value) {
    const err = validateRowValue(row)
    if (err) {
      ElMessage.warning(err)
      return
    }
  }

  const operator =
    userStore.currentUser?.nickname ||
    userStore.currentUser?.email ||
    '系统'
  const weekdays = normalizeWeekdaysPayload()
  const errors: string[] = []

  try {
    saving.value = true

    for (const row of tableRows.value) {
      if (showRelativePreviewColumn.value) {
        const dateValues = relativeDateValuesByKey.value[row.key] || {}
        const dates = Object.keys(dateValues).sort((a, b) => a.localeCompare(b))

        for (const date of dates) {
        const req: UpdatePriceByPlanRequest = {
          roomTypeId: row.roomTypeId,
          pricePlanId: row.pricePlanId,
            startDate: date,
            endDate: date,
            applyWeekdaysInRange: true,
            price: dateValues[date],
          }

          try {
            const resp = (await updatePriceByPlan(req, operator)) as any
            if (!resp.success) {
              errors.push(`${row.roomTypeName}/${row.pricePlanName}(${date}): ${resp.message || t('accommodation.roomPriceBulk.messages.saveFailed')}`)
              break
            }
          } catch (e: any) {
            errors.push(`${row.roomTypeName}/${row.pricePlanName}(${date}): ${e?.message || t('accommodation.roomPriceBulk.messages.saveFailed')}`)
            break
          }
        }

        continue
      }

      const req: UpdatePriceByPlanRequest = {
        roomTypeId: row.roomTypeId,
        pricePlanId: row.pricePlanId,
        startDate: startDate.value,
        endDate: endDate.value,
        weekdays,
        applyWeekdaysInRange: true,
      }

      if (settingType.value === 'price') {
        req.price = Number(row.value)
      } else if (settingType.value === 'minStay') {
        req.minStay = Number(row.value)
      } else if (settingType.value === 'maxStay') {
        req.maxStay = Number(row.value)
      }

      try {
        const resp = (await updatePriceByPlan(req, operator)) as any
        if (!resp.success) {
          errors.push(`${row.roomTypeName}/${row.pricePlanName}: ${resp.message || t('accommodation.roomPriceBulk.messages.saveFailed')}`)
        }
      } catch (e: any) {
        errors.push(`${row.roomTypeName}/${row.pricePlanName}: ${e?.message || t('accommodation.roomPriceBulk.messages.saveFailed')}`)
      }
    }

    if (errors.length > 0) {
      console.error('批量更新部分失败:', errors)
      ElMessage.error(errors[0])
      return
    }

    ElMessage.success(t('accommodation.roomPriceBulk.messages.updateSuccess'))
  } finally {
    saving.value = false
  }
}

const loadTreeData = async () => {
  try {
    const roomTypesResp = (await getAllRoomTypes()) as any
    const roomTypes: RoomTypeDTO[] = roomTypesResp.data || []

    const roomTypeNodes: TreeNode[] = []
    const leafMeta: Record<string, LeafMeta> = {}

    await Promise.all(
      roomTypes.map(async (roomType) => {
        const ppResp = (await getPricePlansByRoomType(roomType.id)) as any
        const mappings: RoomTypePricePlanDTO[] = ppResp.data || []

        const planNodes: TreeNode[] = []
        for (const mapping of mappings) {
          const planId = mapping.pricePlan?.id
          if (!planId) continue

          const planName = mapping.pricePlan?.name || String(planId)
          const key = `${roomType.id}-${planId}`

          planNodes.push({ value: key, label: planName })
          leafMeta[key] = {
            roomTypeId: roomType.id,
            roomTypeName: roomType.name,
            roomTypeCode: roomType.code || '',
            pricePlanId: planId,
            pricePlanName: planName,
          }
        }

        if (planNodes.length > 0) {
          roomTypeNodes.push({
            value: `rt-${roomType.id}`,
            label: `${roomType.name}${roomType.code ? ` (${roomType.code})` : ''}`,
            children: planNodes,
          })
        }
      }),
    )

    roomTypeNodes.sort((a, b) => a.label.localeCompare(b.label))
    treeData.value = [{ value: 'all', label: t('accommodation.roomPriceBulk.tree.all'), children: roomTypeNodes }]
    leafMetaByKey.value = leafMeta
  } catch (error) {
    console.error('加载房型&价格计划失败:', error)
    ElMessage.error(t('accommodation.roomPriceBulk.messages.loadTreeFailed'))
  }
}

onMounted(() => {
  loadTreeData()
  loadChannelAdjustments()
})

onUnmounted(() => {
  clearAutoApplyTimer()
})

watch([settingType, priceMode], ([currentSettingType, currentPriceMode]) => {
  if (currentSettingType !== 'price' || currentPriceMode !== 'relative') {
    relativePreviewByKey.value = {}
    relativeDateValuesByKey.value = {}
  }
  scheduleAutoApply()
})

watch(batchValue, () => {
  scheduleAutoApply()
})

watch([startDate, endDate], () => {
  scheduleAutoApply()
})

watch([relativeType, relativeValueMode], () => {
  scheduleAutoApply()
})

watch(
  selectedWeekdays,
  () => {
    scheduleAutoApply()
  },
  { deep: true },
)
</script>

<style scoped>
.bulk-update-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.page-tabs {
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
}

.bulk-form {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.form-item {
  margin-bottom: 24px;
}

.room-type-select-wrapper {
  position: relative;
}

.selected-tags-area {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 8px 40px 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-height: 40px;
  background: #fff;
  cursor: pointer;
  position: relative;
}

.placeholder-text {
  color: #c0c4cc;
  font-size: 14px;
  user-select: none;
}

.dropdown-arrow {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  color: #909399;
  transition: transform 0.3s;
}

.dropdown-arrow.is-reverse {
  transform: translateY(-50%) rotate(180deg);
}

.tree-select-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 1000;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  background: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.tree-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.filter-input {
  margin-bottom: 12px;
}

.selection-tree {
  max-height: 280px;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 8px;
}

.date-range-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-separator {
  color: #606266;
  font-size: 14px;
}

.weekday-group {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.weekday-selector {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.value-batch-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.price-mode-group {
  display: flex;
  gap: 24px;
}

.relative-type-group {
  display: flex;
  gap: 16px;
}

.batch-input-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-apply-hint {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}

.value-range-preview {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #303133;
}

.range-line {
  font-size: 13px;
  line-height: 1.4;
}

.range-line-result {
  color: #409eff;
}

.channel-range-line {
  color: #606266;
}

.channel-preview-fixed {
  margin-top: 8px;
}

.range-empty {
  color: #909399;
  font-size: 13px;
}

.table-section {
  margin-top: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.empty-text {
  margin-top: 16px;
  font-size: 14px;
  color: #909399;
}

.footer-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 16px 24px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}

.dropdown-enter-active {
  transition: all 0.2s ease-out;
}

.dropdown-leave-active {
  transition: all 0.2s ease-in;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
