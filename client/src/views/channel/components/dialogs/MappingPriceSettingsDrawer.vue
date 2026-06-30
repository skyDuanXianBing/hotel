<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Refresh, RefreshRight, Setting } from '@element-plus/icons-vue'
import {
  getMappingPriceSettings,
  retryMappingPriceSettings,
  saveMappingPriceSettingRow,
  saveMappingPriceSettings,
  type MappingPriceSettingRowDTO,
  type MappingPriceSettingsResponseDTO,
  type MappingPriceSettingsSaveResponseDTO,
  type MappingPriceSyncStatus,
} from '@/api/pricelabs'
import type {
  MappingPriceBatchField,
  MappingPriceDraftRow,
  MappingPriceFilterMode,
  PriceRatioItem,
} from '../../types'

const props = defineProps<{
  modelValue: boolean
  channel: PriceRatioItem | null
  canSave: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
  'dirty-change': [value: boolean]
  'saving-change': [value: boolean]
}>()

const FILTER_ALL: MappingPriceFilterMode = 'ALL'
const FILTER_FAILED: MappingPriceFilterMode = 'FAILED'
const FILTER_DIRTY: MappingPriceFilterMode = 'DIRTY'
const BATCH_MULTIPLIER: MappingPriceBatchField = 'MULTIPLIER'
const BATCH_SURCHARGE: MappingPriceBatchField = 'SURCHARGE'
const NUMBER_STEP = 0.01
const NUMBER_PRECISION = 4
const OPERATION_PREFIX = 'mapping-price'

const loading = ref(false)
const savingAll = ref(false)
const retryingFailed = ref(false)
const closeApprovedByDrawer = ref(false)
const drawerError = ref('')
const settings = ref<MappingPriceSettingsResponseDTO | null>(null)
const rows = ref<MappingPriceDraftRow[]>([])
const filterMode = ref<MappingPriceFilterMode>(FILTER_ALL)
const batchField = ref<MappingPriceBatchField>(BATCH_MULTIPLIER)
const batchMultiplier = ref<number | null>(null)
const batchSurcharge = ref<number | null>(null)

const channelTitle = computed(() => {
  if (settings.value?.channelName) {
    return settings.value.channelName
  }
  if (props.channel?.channel) {
    return props.channel.channel
  }
  return '渠道'
})

const totalCount = computed(() => rows.value.length)
const successCount = computed(() => countRowsByStatus('SUCCESS'))
const failedCount = computed(() => {
  let count = 0
  for (const row of rows.value) {
    if (isRetryableRow(row)) {
      count += 1
    }
  }
  return count
})
const staleCount = computed(() => countRowsByStatus('STALE_MAPPING'))
const unsyncedCount = computed(() => countRowsByStatus('UNSYNCED'))
const dirtyCount = computed(() => {
  let count = 0
  for (const row of rows.value) {
    if (row.dirty) {
      count += 1
    }
  }
  return count
})
const rowSavingCount = computed(() => {
  let count = 0
  for (const row of rows.value) {
    if (row.saving) {
      count += 1
    }
  }
  return count
})
const savingCount = computed(() => {
  let count = rowSavingCount.value
  if (savingAll.value) {
    count += 1
  }
  if (retryingFailed.value) {
    count += 1
  }
  return count
})
const isBusy = computed(() => loading.value || savingAll.value || retryingFailed.value)
const canUseActions = computed(() => props.canSave && !loading.value && savingCount.value === 0)
const hasRows = computed(() => rows.value.length > 0)
const filteredRows = computed(() => {
  if (filterMode.value === FILTER_FAILED) {
    return rows.value.filter(isRetryableRow)
  }
  if (filterMode.value === FILTER_DIRTY) {
    return rows.value.filter((row) => row.dirty)
  }
  return rows.value
})

watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      loadRows()
      return
    }
    resetLocalState()
  },
  { immediate: true },
)

watch(
  () => props.channel?.channelId,
  () => {
    if (props.modelValue) {
      loadRows()
    }
  },
)

watch(
  dirtyCount,
  (count) => {
    emit('dirty-change', count > 0)
  },
  { immediate: true },
)

watch(
  savingCount,
  (count) => {
    emit('saving-change', count > 0)
  },
  { immediate: true },
)

function resetLocalState() {
  loading.value = false
  savingAll.value = false
  retryingFailed.value = false
  closeApprovedByDrawer.value = false
  drawerError.value = ''
  settings.value = null
  rows.value = []
  filterMode.value = FILTER_ALL
  batchField.value = BATCH_MULTIPLIER
  batchMultiplier.value = null
  batchSurcharge.value = null
}

function toNullableNumber(value: number | string | null | undefined): number | null {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const nextValue = Number(value)
  if (!Number.isFinite(nextValue)) {
    return null
  }
  return nextValue
}

function createDraftRow(row: MappingPriceSettingRowDTO): MappingPriceDraftRow {
  const multiplier = toNullableNumber(row.multiplier)
  const surcharge = toNullableNumber(row.surcharge)
  return {
    ...row,
    multiplier,
    surcharge,
    originalMultiplier: multiplier,
    originalSurcharge: surcharge,
    draftMultiplier: multiplier,
    draftSurcharge: surcharge,
    dirty: false,
    saving: false,
  }
}

function countRowsByStatus(status: MappingPriceSyncStatus): number {
  let count = 0
  for (const row of rows.value) {
    if (row.syncStatus === status) {
      count += 1
    }
  }
  return count
}

function buildClientOperationId(action: string): string {
  return `${OPERATION_PREFIX}-${action}-${Date.now()}`
}

async function loadRows() {
  if (!props.channel) {
    resetLocalState()
    return
  }

  loading.value = true
  drawerError.value = ''
  try {
    const response = await getMappingPriceSettings(props.channel.channelId)
    if (!response.success || !response.data) {
      drawerError.value = response.message || '加载映射级价格设置失败'
      rows.value = []
      return
    }

    settings.value = response.data
    rows.value = response.data.rows.map(createDraftRow)
  } catch (error) {
    console.error('加载映射级价格设置失败:', error)
    drawerError.value = '加载映射级价格设置失败，请稍后重试'
    rows.value = []
  } finally {
    loading.value = false
  }
}

function sameDraftValue(left: number | null, right: number | null): boolean {
  if (left === null && right === null) {
    return true
  }
  if (left === null || right === null) {
    return false
  }
  return Number(left) === Number(right)
}

function refreshRowDirtyState(row: MappingPriceDraftRow) {
  const multiplierChanged = !sameDraftValue(row.draftMultiplier, row.originalMultiplier)
  const surchargeChanged = !sameDraftValue(row.draftSurcharge, row.originalSurcharge)
  row.dirty = multiplierChanged || surchargeChanged
}

function handleDraftChange(row: MappingPriceDraftRow) {
  refreshRowDirtyState(row)
}

function handleResetRowDraft(row: MappingPriceDraftRow) {
  row.draftMultiplier = row.originalMultiplier
  row.draftSurcharge = row.originalSurcharge
  row.dirty = false
}

function handleApplyBatch() {
  if (!props.canSave) {
    ElMessage.warning('当前账号没有修改渠道价格比例的权限')
    return
  }

  if (batchField.value === BATCH_MULTIPLIER) {
    if (batchMultiplier.value === null) {
      ElMessage.warning('请先填写要批量应用的倍率')
      return
    }
    for (const row of rows.value) {
      row.draftMultiplier = batchMultiplier.value
      refreshRowDirtyState(row)
    }
    ElMessage.success('已将倍率应用到全部映射行，保存后生效')
    return
  }

  if (batchSurcharge.value === null) {
    ElMessage.warning('请先填写要批量应用的固定加价')
    return
  }
  for (const row of rows.value) {
    row.draftSurcharge = batchSurcharge.value
    refreshRowDirtyState(row)
  }
  ElMessage.success('已将固定加价应用到全部映射行，保存后生效')
}

function toSaveRow(row: MappingPriceDraftRow) {
  return {
    rowKey: row.rowKey,
    multiplier: row.draftMultiplier,
    surcharge: row.draftSurcharge,
  }
}

function setRowsSaving(targetRows: MappingPriceDraftRow[], saving: boolean) {
  for (const row of targetRows) {
    row.saving = saving
  }
}

function mergeRowsFromResponse(savedRows: MappingPriceSettingRowDTO[]) {
  const savedByKey = new Map<string, MappingPriceSettingRowDTO>()
  for (const savedRow of savedRows) {
    savedByKey.set(savedRow.rowKey, savedRow)
  }

  const nextRows: MappingPriceDraftRow[] = []
  for (const currentRow of rows.value) {
    const savedRow = savedByKey.get(currentRow.rowKey)
    if (savedRow) {
      nextRows.push(createDraftRow({ ...currentRow, ...savedRow }))
    } else {
      nextRows.push({ ...currentRow, saving: false })
    }
  }
  rows.value = nextRows
}

function clearRowsSaving() {
  for (const row of rows.value) {
    row.saving = false
  }
}

function showSaveResultMessage(result: MappingPriceSettingsSaveResponseDTO) {
  if (result.status === 'SUCCESS') {
    ElMessage.success(result.message || '映射级价格设置已保存')
    return
  }
  if (result.status === 'PARTIAL') {
    ElMessage.warning(result.message || '部分映射行保存失败，请查看失败原因后重试')
    return
  }
  ElMessage.error(result.message || '映射级价格设置保存失败')
}

async function handleSaveAll() {
  if (!props.channel || rows.value.length === 0) {
    return
  }
  if (!props.canSave) {
    ElMessage.warning('当前账号没有修改渠道价格比例的权限')
    return
  }

  savingAll.value = true
  setRowsSaving(rows.value, true)
  try {
    const response = await saveMappingPriceSettings(props.channel.channelId, {
      clientOperationId: buildClientOperationId('save-all'),
      rows: rows.value.map(toSaveRow),
    })
    if (!response.success || !response.data) {
      ElMessage.error(response.message || '保存全部映射行失败')
      return
    }

    mergeRowsFromResponse(response.data.rows)
    showSaveResultMessage(response.data)
    emit('saved')
  } catch (error) {
    console.error('保存全部映射行失败:', error)
    ElMessage.error('保存全部映射行失败，请稍后重试')
  } finally {
    savingAll.value = false
    clearRowsSaving()
  }
}

async function handleSaveRow(row: MappingPriceDraftRow) {
  if (!props.channel || !props.canSave) {
    return
  }

  row.saving = true
  try {
    const response = await saveMappingPriceSettingRow(
      props.channel.channelId,
      row.rowKey,
      toSaveRow(row),
    )
    if (!response.success || !response.data) {
      ElMessage.error(response.message || '保存本行失败')
      return
    }

    mergeRowsFromResponse(response.data.rows)
    showSaveResultMessage(response.data)
    emit('saved')
  } catch (error) {
    console.error('保存本行映射价格失败:', error)
    ElMessage.error('保存本行失败，请稍后重试')
  } finally {
    row.saving = false
  }
}

async function handleRetryRow(row: MappingPriceDraftRow) {
  if (!props.channel || !props.canSave) {
    return
  }

  row.saving = true
  try {
    const response = await retryMappingPriceSettings(props.channel.channelId, {
      clientOperationId: buildClientOperationId('retry-row'),
      rowKeys: [row.rowKey],
    })
    if (!response.success || !response.data) {
      ElMessage.error(response.message || '重试本行失败')
      return
    }

    mergeRowsFromResponse(response.data.rows)
    showSaveResultMessage(response.data)
    emit('saved')
  } catch (error) {
    console.error('重试本行映射价格失败:', error)
    ElMessage.error('重试本行失败，请稍后重试')
  } finally {
    row.saving = false
  }
}

async function handleRetryFailed() {
  if (!props.channel || !props.canSave) {
    return
  }

  const retryRows = rows.value.filter(isRetryableRow)
  if (retryRows.length === 0) {
    ElMessage.info('当前没有失败的映射行')
    return
  }

  retryingFailed.value = true
  setRowsSaving(retryRows, true)
  try {
    const rowKeys = retryRows.map((row) => row.rowKey)
    const response = await retryMappingPriceSettings(props.channel.channelId, {
      clientOperationId: buildClientOperationId('retry-failed'),
      rowKeys,
    })
    if (!response.success || !response.data) {
      ElMessage.error(response.message || '重试失败映射行失败')
      return
    }

    mergeRowsFromResponse(response.data.rows)
    showSaveResultMessage(response.data)
    emit('saved')
  } catch (error) {
    console.error('重试失败映射行失败:', error)
    ElMessage.error('重试失败映射行失败，请稍后重试')
  } finally {
    retryingFailed.value = false
    setRowsSaving(retryRows, false)
  }
}

function isRetryableRow(row: MappingPriceDraftRow): boolean {
  return row.syncStatus === 'FAILED' || row.syncStatus === 'STALE_MAPPING'
}

function formatStatus(row: MappingPriceDraftRow): string {
  if (row.saving) {
    return '同步中'
  }
  if (row.dirty) {
    return '未保存'
  }
  if (row.syncStatus === 'SUCCESS') {
    return '已同步'
  }
  if (row.syncStatus === 'FAILED') {
    return '同步失败'
  }
  if (row.syncStatus === 'STALE_MAPPING') {
    return '映射已变化'
  }
  if (row.syncStatus === 'SYNCING') {
    return '同步中'
  }
  return '未同步'
}

function resolveStatusType(row: MappingPriceDraftRow) {
  if (row.saving) {
    return 'warning'
  }
  if (row.dirty) {
    return 'info'
  }
  if (row.syncStatus === 'SUCCESS') {
    return 'success'
  }
  if (row.syncStatus === 'FAILED' || row.syncStatus === 'STALE_MAPPING') {
    return 'danger'
  }
  if (row.syncStatus === 'SYNCING') {
    return 'warning'
  }
  return 'info'
}

function formatMappingDimension(row: MappingPriceDraftRow): string {
  const parts: string[] = []
  if (row.occupancy) {
    parts.push(`入住人数 ${row.occupancy}`)
  }
  if (row.applicableNoOfGuest) {
    parts.push(`适用住客 ${row.applicableNoOfGuest}`)
  }
  if (parts.length === 0) {
    return '-'
  }
  return parts.join(' / ')
}

function formatLocalInfo(row: MappingPriceDraftRow): string {
  const parts: string[] = []
  if (row.localRoomId) {
    parts.push(`房型 ${row.localRoomId}`)
  }
  if (row.localRatePlanId) {
    parts.push(`费率 ${row.localRatePlanId}`)
  }
  if (parts.length === 0) {
    return '-'
  }
  return parts.join(' / ')
}

function formatRemoteInfo(row: MappingPriceDraftRow): string {
  const parts: string[] = []
  if (row.listingId) {
    parts.push(`房源 ${row.listingId}`)
  }
  if (row.remoteRoomId) {
    parts.push(`房间 ${row.remoteRoomId}`)
  }
  if (row.remoteRatePlanId) {
    parts.push(`费率 ${row.remoteRatePlanId}`)
  }
  if (parts.length === 0) {
    return '-'
  }
  return parts.join(' / ')
}

function formatDateTime(value?: string | null): string {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

async function confirmClose(): Promise<boolean> {
  if (savingCount.value > 0) {
    try {
      await ElMessageBox.confirm(
        '仍有映射行正在保存，关闭抽屉可能无法看到最新结果。确定要关闭吗？',
        '关闭价格设置',
        {
          confirmButtonText: '关闭',
          cancelButtonText: '继续等待',
          type: 'warning',
        },
      )
      return true
    } catch {
      return false
    }
  }

  if (dirtyCount.value === 0) {
    return true
  }

  try {
    await ElMessageBox.confirm('存在未保存的价格设置，关闭后这些草稿会丢失。确定关闭吗？', '关闭价格设置', {
      confirmButtonText: '放弃草稿',
      cancelButtonText: '继续编辑',
      type: 'warning',
    })
    return true
  } catch {
    return false
  }
}

function handleBeforeClose(done: () => void) {
  confirmClose().then((canClose) => {
    if (canClose) {
      closeApprovedByDrawer.value = true
      done()
    }
  })
}

function handleDrawerModelUpdate(value: boolean) {
  if (value) {
    emit('update:modelValue', true)
    return
  }
  if (closeApprovedByDrawer.value) {
    closeApprovedByDrawer.value = false
    emit('update:modelValue', false)
    return
  }
  requestClose()
}

async function requestClose() {
  const canClose = await confirmClose()
  if (canClose) {
    emit('update:modelValue', false)
  }
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    custom-class="mapping-price-settings-drawer"
    direction="rtl"
    size="min(1120px, 94vw)"
    :before-close="handleBeforeClose"
    @update:model-value="handleDrawerModelUpdate"
  >
    <template #header>
      <div class="drawer-title">
        <span class="drawer-title-text">{{ channelTitle }}价格比例设置</span>
        <el-tag v-if="settings?.channelCode" effect="plain" type="info">
          {{ settings.channelCode }}
        </el-tag>
      </div>
    </template>

    <div class="mapping-price-drawer">
      <el-alert
        v-if="drawerError"
        :title="drawerError"
        type="error"
        show-icon
        :closable="false"
        class="drawer-alert"
      />

      <div class="summary-bar">
        <div class="summary-item">
          <span class="summary-label">映射行</span>
          <strong>{{ totalCount }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">已同步</span>
          <strong>{{ successCount }}</strong>
        </div>
        <div class="summary-item danger">
          <span class="summary-label">失败</span>
          <strong>{{ failedCount }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">未同步</span>
          <strong>{{ unsyncedCount }}</strong>
        </div>
        <div class="summary-item warning">
          <span class="summary-label">映射变化</span>
          <strong>{{ staleCount }}</strong>
        </div>
        <div class="summary-item dirty">
          <span class="summary-label">未保存</span>
          <strong>{{ dirtyCount }}</strong>
        </div>
      </div>

      <div class="toolbar">
        <div class="batch-controls">
          <el-radio-group v-model="batchField" size="small">
            <el-radio-button :label="BATCH_MULTIPLIER">倍率</el-radio-button>
            <el-radio-button :label="BATCH_SURCHARGE">固定加价</el-radio-button>
          </el-radio-group>

          <el-input-number
            v-if="batchField === BATCH_MULTIPLIER"
            v-model="batchMultiplier"
            class="batch-input"
            :min="0"
            :step="NUMBER_STEP"
            :precision="NUMBER_PRECISION"
            controls-position="right"
            placeholder="倍率"
          />
          <el-input-number
            v-else
            v-model="batchSurcharge"
            class="batch-input"
            :step="NUMBER_STEP"
            :precision="NUMBER_PRECISION"
            controls-position="right"
            placeholder="固定加价"
          />

          <el-button
            type="primary"
            plain
            :icon="Setting"
            :disabled="!canUseActions || !hasRows"
            @click="handleApplyBatch"
          >
            应用到全部映射行
          </el-button>
        </div>

        <div class="filter-controls">
          <el-radio-group v-model="filterMode" size="small">
            <el-radio-button :label="FILTER_ALL">全部</el-radio-button>
            <el-radio-button :label="FILTER_FAILED">失败</el-radio-button>
            <el-radio-button :label="FILTER_DIRTY">未保存</el-radio-button>
          </el-radio-group>
          <el-button :icon="Refresh" :loading="loading" :disabled="isBusy" @click="loadRows">
            刷新
          </el-button>
        </div>
      </div>

      <div class="table-scroll-shell">
        <el-table
          v-loading="loading"
          :data="filteredRows"
          border
          row-key="rowKey"
          class="mapping-table"
          empty-text="暂无映射价格设置"
        >
          <el-table-column label="映射" min-width="210">
            <template #default="{ row }">
              <div class="mapping-name">{{ row.displayName || '未命名映射' }}</div>
              <div class="mapping-meta">
                <span v-if="row.mappingStatus">{{ row.mappingStatus }}</span>
                <span v-if="row.suChannelId">Su {{ row.suChannelId }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="本地房型 / 费率" min-width="180">
            <template #default="{ row }">
              <span class="line-text">{{ formatLocalInfo(row) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="远端房源 / 费率" min-width="210">
            <template #default="{ row }">
              <span class="line-text">{{ formatRemoteInfo(row) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="入住维度" min-width="150">
            <template #default="{ row }">
              <span class="line-text">{{ formatMappingDimension(row) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="倍率" width="150" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.draftMultiplier"
                class="price-input"
                :min="0"
                :step="NUMBER_STEP"
                :precision="NUMBER_PRECISION"
                controls-position="right"
                :disabled="!canUseActions || row.saving"
                @change="handleDraftChange(row)"
              />
            </template>
          </el-table-column>

          <el-table-column label="固定加价" width="150" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.draftSurcharge"
                class="price-input"
                :step="NUMBER_STEP"
                :precision="NUMBER_PRECISION"
                controls-position="right"
                :disabled="!canUseActions || row.saving"
                @change="handleDraftChange(row)"
              />
            </template>
          </el-table-column>

          <el-table-column label="同步状态" width="150" align="center">
            <template #default="{ row }">
              <div class="status-cell">
                <el-tag :type="resolveStatusType(row)" effect="light">
                  {{ formatStatus(row) }}
                </el-tag>
                <span v-if="row.retryCount" class="status-note">重试 {{ row.retryCount }} 次</span>
                <span v-if="row.lastSyncedAt && !row.dirty" class="status-note">
                  {{ formatDateTime(row.lastSyncedAt) }}
                </span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="失败原因" min-width="200">
            <template #default="{ row }">
              <el-tooltip v-if="row.lastError" :content="row.lastError" placement="top">
                <span class="error-text">{{ row.lastError }}</span>
              </el-tooltip>
              <span v-else class="muted-text">-</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="190" fixed="right" align="center">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button
                  v-if="row.dirty"
                  type="primary"
                  link
                  :icon="Check"
                  :disabled="!canUseActions"
                  :loading="row.saving"
                  @click="handleSaveRow(row)"
                >
                  保存本行
                </el-button>
                <el-button
                  v-if="isRetryableRow(row) && !row.dirty"
                  type="warning"
                  link
                  :icon="RefreshRight"
                  :disabled="!canUseActions"
                  :loading="row.saving"
                  @click="handleRetryRow(row)"
                >
                  重试
                </el-button>
                <el-button
                  type="info"
                  link
                  :icon="Close"
                  :disabled="!row.dirty || row.saving"
                  @click="handleResetRowDraft(row)"
                >
                  还原
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <div class="footer-status">
          <span v-if="dirtyCount > 0">有 {{ dirtyCount }} 行未保存</span>
          <span v-else>当前没有未保存草稿</span>
        </div>
        <div class="footer-actions">
          <el-button @click="requestClose">取消</el-button>
          <el-button
            :icon="RefreshRight"
            :loading="retryingFailed"
            :disabled="!canUseActions || failedCount === 0"
            @click="handleRetryFailed"
          >
            只重试失败
          </el-button>
          <el-button
            type="primary"
            :icon="Check"
            :loading="savingAll"
            :disabled="!props.canSave || loading || rows.length === 0"
            @click="handleSaveAll"
          >
            保存全部
          </el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
:deep(.mapping-price-settings-drawer) {
  max-width: 100vw;
  overflow: hidden;
}

:deep(.mapping-price-settings-drawer .el-drawer__header) {
  box-sizing: border-box;
  min-width: 0;
}

:deep(.mapping-price-settings-drawer .el-drawer__body) {
  box-sizing: border-box;
  min-width: 0;
  overflow-x: hidden;
}

:deep(.mapping-price-settings-drawer .el-drawer__footer) {
  box-sizing: border-box;
  min-width: 0;
  padding: 0;
}

.drawer-title {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.drawer-title-text {
  flex: 1;
  min-width: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mapping-price-drawer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 100%;
  padding: 0 8px 12px;
  box-sizing: border-box;
}

.drawer-alert {
  flex-shrink: 0;
}

.summary-bar {
  display: grid;
  grid-template-columns: repeat(6, minmax(104px, 1fr));
  gap: 10px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.summary-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.summary-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: #606266;
}

.summary-item strong {
  font-size: 20px;
  line-height: 1.2;
  color: #303133;
}

.summary-item.danger strong {
  color: #f56c6c;
}

.summary-item.warning strong {
  color: #e6a23c;
}

.summary-item.dirty strong {
  color: #409eff;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.batch-controls,
.filter-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.batch-input {
  width: 150px;
}

.table-scroll-shell {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  border-radius: 8px;
  background: #fff;
  box-sizing: border-box;
  -webkit-overflow-scrolling: touch;
}

.mapping-table {
  width: 100%;
  min-width: 1390px;
}

.mapping-table :deep(.cell) {
  overflow-wrap: anywhere;
}

.mapping-name {
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.mapping-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.line-text {
  display: inline-block;
  max-width: 100%;
  color: #303133;
  line-height: 1.5;
}

.price-input {
  width: 126px;
}

.status-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.status-note {
  max-width: 120px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-text {
  display: inline-block;
  max-width: 100%;
  color: #f56c6c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted-text {
  color: #c0c4cc;
}

.row-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px;
  border-top: 1px solid #e4e7ed;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.footer-status {
  min-width: 0;
  font-size: 13px;
  color: #606266;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 720px) {
  :deep(.mapping-price-settings-drawer) {
    width: 100vw !important;
  }

  :deep(.mapping-price-settings-drawer .el-drawer__header) {
    padding: 16px 16px 10px;
    margin-bottom: 12px;
  }

  :deep(.mapping-price-settings-drawer .el-drawer__body) {
    padding: 0 14px 12px;
  }

  .drawer-title {
    gap: 8px;
  }

  .drawer-title-text {
    font-size: 15px;
    line-height: 1.4;
    white-space: normal;
  }

  .mapping-price-drawer {
    gap: 12px;
    padding: 0;
  }

  .summary-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .summary-item {
    padding: 8px 10px;
  }

  .toolbar,
  .drawer-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .batch-controls,
  .filter-controls,
  .footer-actions {
    align-items: stretch;
    flex-direction: column;
    width: 100%;
    min-width: 0;
  }

  .batch-input,
  .footer-actions .el-button {
    width: 100%;
  }

  .filter-controls .el-radio-group,
  .batch-controls .el-radio-group {
    width: 100%;
  }

  .filter-controls :deep(.el-radio-button),
  .batch-controls :deep(.el-radio-button) {
    flex: 1;
  }

  .filter-controls :deep(.el-radio-button__inner),
  .batch-controls :deep(.el-radio-button__inner) {
    width: 100%;
  }

  .table-scroll-shell {
    border: 1px solid #e4e7ed;
  }

  .drawer-footer {
    padding: 10px 14px 14px;
  }
}
</style>
