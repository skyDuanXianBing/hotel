<template>
  <div class="bulk-update-page">
    <div class="page-tabs">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="日历" name="calendar"></el-tab-pane>
        <el-tab-pane label="批量更新" name="bulk-update"></el-tab-pane>
      </el-tabs>
    </div>

    <el-form label-width="110px" class="bulk-form">
      <el-form-item label="房型&价位" required class="form-item room-type-select-wrapper">
        <div class="selected-tags-area" @click="toggleTreeDropdown">
          <span v-if="selectedTags.length === 0" class="placeholder-text">请选择</span>

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
            <el-input v-model="filterText" placeholder="搜索房型/价位" class="filter-input" />
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
              <el-button size="small" @click="clearSelection">清空</el-button>
              <el-button size="small" type="primary" @click="showTreeDropdown = false">完成</el-button>
            </div>
          </div>
        </transition>
      </el-form-item>

      <el-form-item label="日期范围" required class="form-item">
        <div class="date-range-wrapper">
          <el-date-picker
            v-model="startDate"
            type="date"
            placeholder="开始日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            size="default"
          />
          <span class="date-separator">至</span>
          <el-date-picker
            v-model="endDate"
            type="date"
            placeholder="结束日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            size="default"
          />
        </div>
      </el-form-item>

      <el-form-item label="星期" class="form-item">
        <el-checkbox-group v-model="selectedWeekdays" class="weekday-group" @change="handleWeekdayChange">
          <el-checkbox :label="0">全部</el-checkbox>
          <el-checkbox :label="1">周一</el-checkbox>
          <el-checkbox :label="2">周二</el-checkbox>
          <el-checkbox :label="3">周三</el-checkbox>
          <el-checkbox :label="4">周四</el-checkbox>
          <el-checkbox :label="5">周五</el-checkbox>
          <el-checkbox :label="6">周六</el-checkbox>
          <el-checkbox :label="7">周日</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="设置项" required class="form-item">
        <el-select v-model="settingType" placeholder="请选择" style="width: 200px">
          <el-option label="价格" value="price" />
          <el-option label="最小入住天数" value="minStay" />
          <el-option label="最大入住天数" value="maxStay" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="settingType === 'price'" label="批量填值" class="form-item">
        <div class="value-batch-wrapper">
          <el-radio-group v-model="priceMode" class="price-mode-group">
            <el-radio value="fixed" label="固定值" />
            <el-radio value="relative" label="基于当前值" />
          </el-radio-group>

          <div v-if="priceMode === 'relative'" class="relative-price-options">
            <el-radio-group v-model="relativeType" class="relative-type-group">
              <el-radio value="cheaper" label="减" />
              <el-radio value="expensive" label="加" />
            </el-radio-group>
            <el-radio-group v-model="relativeValueMode" class="relative-type-group">
              <el-radio value="amount" label="按金额" />
              <el-radio value="percent" label="按百分比" />
            </el-radio-group>
          </div>

          <div class="batch-input-row">
            <el-input v-model="batchValue" type="number" placeholder="请输入价格" style="width: 260px">
              <template #append>{{ batchValueUnit }}</template>
            </el-input>
            <el-button type="primary" @click="applyBatchValue" style="margin-left: 12px">
              应用到明细
            </el-button>
          </div>
        </div>
      </el-form-item>

      <el-form-item v-else label="批量填值" class="form-item">
        <div class="batch-input-row">
          <el-input v-model="batchValue" type="number" placeholder="请输入天数(1-99)" style="width: 260px">
            <template #append>天</template>
          </el-input>
          <el-button type="primary" @click="applyBatchValue" style="margin-left: 12px">
            应用到明细
          </el-button>
        </div>
      </el-form-item>

      <div class="table-section">
        <div v-if="tableRows.length === 0" class="empty-state">
          <el-icon :size="48" color="#c0c4cc">
            <Document />
          </el-icon>
          <p class="empty-text">请先选择房型&价位</p>
        </div>
        <el-table v-else :data="tableRows" border style="width: 100%">
          <el-table-column prop="roomTypeName" label="房型" min-width="180" />
          <el-table-column prop="pricePlanName" label="价格计划" min-width="180" />
          <el-table-column label="值" min-width="220">
            <template #default="{ row }">
              <div v-if="showRelativePreviewColumn" class="value-range-preview">
                <template v-if="relativePreviewByKey[row.key]">
                  <div class="range-line">
                    当前区间：{{
                      formatRangeText(
                        relativePreviewByKey[row.key].currentMin,
                        relativePreviewByKey[row.key].currentMax,
                      )
                    }}
                  </div>
                  <div class="range-line range-line-result">
                    计算结果：{{
                      formatRangeText(
                        relativePreviewByKey[row.key].adjustedMin,
                        relativePreviewByKey[row.key].adjustedMax,
                      )
                    }}
                  </div>
                </template>
                <span v-else class="range-empty">点击“应用到明细”后显示最小/最大值</span>
              </div>
              <el-input v-else v-model="row.value" type="number" :placeholder="valuePlaceholder">
                <template v-if="settingType === 'price'" #append>JPY</template>
                <template v-else #append>天</template>
              </el-input>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeRow(row.key)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="footer-actions">
        <el-button @click="handleCancel" size="large">取消</el-button>
        <el-button type="primary" @click="handleSave" size="large" :loading="saving">
          保存
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
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
import { useUserStore } from '@/stores/user'

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

const router = useRouter()
const userStore = useUserStore()

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

const valuePlaceholder = computed(() => {
  return settingType.value === 'price' ? '请输入价格' : '请输入天数(1-99)'
})

const showRelativePreviewColumn = computed(() => {
  return settingType.value === 'price' && priceMode.value === 'relative'
})

const batchValueUnit = computed(() => {
  if (settingType.value !== 'price') {
    return 'JPY'
  }
  if (priceMode.value === 'relative' && relativeValueMode.value === 'percent') {
    return '%'
  }
  return 'JPY'
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

const applyBatchValue = async () => {
  if (!batchValue.value) {
    ElMessage.warning('请先输入值')
    return
  }

  if (settingType.value === 'price') {
    const v = parseFloat(batchValue.value)
    if (Number.isNaN(v) || v <= 0) {
      ElMessage.warning('请输入有效价格')
      return
    }

    if (priceMode.value === 'fixed') {
      tableRows.value.forEach((row) => {
        row.value = batchValue.value
        rowValueByKey.value[row.key] = row.value
      })
      relativePreviewByKey.value = {}
      relativeDateValuesByKey.value = {}
      ElMessage.success('已应用到明细')
      return
    }

    if (!startDate.value || !endDate.value) {
      ElMessage.warning('请先选择日期范围')
      return
    }
    if (startDate.value > endDate.value) {
      ElMessage.warning('开始日期不能晚于结束日期')
      return
    }

    try {
      const resp = (await getRoomPriceManagementData(startDate.value, endDate.value)) as any
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
        ElMessage.warning('当前区间未找到可计算的价格数据')
        return
      }

      ElMessage.success('已应用到明细')
    } catch (error) {
      console.error('获取当前区间价格失败:', error)
      ElMessage.error('获取当前区间价格失败')
    }
    return
  }

  const days = parseInt(batchValue.value, 10)
  if (Number.isNaN(days) || days < 1 || days > 99) {
    ElMessage.warning('请输入有效天数（1-99）')
    return
  }

  tableRows.value.forEach((row) => {
    row.value = String(days)
    rowValueByKey.value[row.key] = row.value
  })
  ElMessage.success('已应用到明细')
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
      return `${row.roomTypeName}/${row.pricePlanName}：请先点击应用到明细`
    }
    const dateValues = relativeDateValuesByKey.value[row.key]
    if (!dateValues || Object.keys(dateValues).length === 0) {
      return `${row.roomTypeName}/${row.pricePlanName}：未生成可保存的结果`
    }
    return null
  }

  if (!row.value) return `${row.roomTypeName}/${row.pricePlanName}：请填写值`
  if (settingType.value === 'price') {
    const v = parseFloat(row.value)
    if (Number.isNaN(v) || v <= 0) return `${row.roomTypeName}/${row.pricePlanName}：价格无效`
    return null
  }

  const days = parseInt(row.value, 10)
  if (Number.isNaN(days) || days < 1 || days > 99) {
    return `${row.roomTypeName}/${row.pricePlanName}：天数无效（1-99）`
  }
  return null
}

const handleCancel = () => {
  router.push('/accommodation/room-price-management')
}

const handleSave = async () => {
  if (selectedPlanIds.value.length === 0 || tableRows.value.length === 0) {
    ElMessage.warning('请选择房型&价位')
    return
  }
  if (!startDate.value || !endDate.value) {
    ElMessage.warning('请选择日期范围')
    return
  }
  if (startDate.value > endDate.value) {
    ElMessage.warning('开始日期不能晚于结束日期')
    return
  }

  for (const row of tableRows.value) {
    const err = validateRowValue(row)
    if (err) {
      ElMessage.warning(err)
      return
    }
  }

  const operator = userStore.currentUser?.nickname || userStore.currentUser?.email || '系统'
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
              errors.push(`${row.roomTypeName}/${row.pricePlanName}(${date}): ${resp.message || '保存失败'}`)
              break
            }
          } catch (e: any) {
            errors.push(`${row.roomTypeName}/${row.pricePlanName}(${date}): ${e?.message || '保存失败'}`)
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
          errors.push(`${row.roomTypeName}/${row.pricePlanName}: ${resp.message || '保存失败'}`)
        }
      } catch (e: any) {
        errors.push(`${row.roomTypeName}/${row.pricePlanName}: ${e?.message || '保存失败'}`)
      }
    }

    if (errors.length > 0) {
      console.error('批量更新部分失败:', errors)
      ElMessage.error(errors[0])
      return
    }

    ElMessage.success('批量更新成功')
    router.push('/accommodation/room-price-management')
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
    treeData.value = [{ value: 'all', label: '全部', children: roomTypeNodes }]
    leafMetaByKey.value = leafMeta
  } catch (error) {
    console.error('加载房型&价格计划失败:', error)
    ElMessage.error('加载房型&价格计划失败')
  }
}

onMounted(() => {
  loadTreeData()
})

watch([settingType, priceMode], ([currentSettingType, currentPriceMode]) => {
  if (currentSettingType !== 'price' || currentPriceMode !== 'relative') {
    relativePreviewByKey.value = {}
    relativeDateValuesByKey.value = {}
  }
})
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
