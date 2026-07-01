<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
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
const AIRBNB_CHANNEL_CODE = 'AIRBNB'
const AIRBNB_SU_CHANNEL_ID = '244'
const AIRBNB_LISTING_TITLE_LIMIT = 50
const FAILURE_TOOLTIP_STYLE = {
  maxWidth: '520px',
  whiteSpace: 'pre-line',
  wordBreak: 'break-word',
}

const { t, locale } = useI18n()

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
  return t('channel.mappingPriceSettings.fallbackChannel')
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
      drawerError.value = t('channel.mappingPriceSettings.messages.loadFailed')
      rows.value = []
      return
    }

    settings.value = response.data
    rows.value = response.data.rows.map(createDraftRow)
  } catch (error) {
    console.error('加载映射级价格设置失败:', error)
    drawerError.value = t('channel.mappingPriceSettings.messages.loadFailedRetry')
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
    ElMessage.warning(t('channel.mappingPriceSettings.messages.noPermission'))
    return
  }

  if (batchField.value === BATCH_MULTIPLIER) {
    if (batchMultiplier.value === null) {
      ElMessage.warning(t('channel.mappingPriceSettings.messages.batchMultiplierRequired'))
      return
    }
    for (const row of rows.value) {
      row.draftMultiplier = batchMultiplier.value
      refreshRowDirtyState(row)
    }
    ElMessage.success(t('channel.mappingPriceSettings.messages.batchMultiplierApplied'))
    return
  }

  if (batchSurcharge.value === null) {
    ElMessage.warning(t('channel.mappingPriceSettings.messages.batchSurchargeRequired'))
    return
  }
  for (const row of rows.value) {
    row.draftSurcharge = batchSurcharge.value
    refreshRowDirtyState(row)
  }
  ElMessage.success(t('channel.mappingPriceSettings.messages.batchSurchargeApplied'))
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
    ElMessage.success(t('channel.mappingPriceSettings.messages.saveSuccess'))
    return
  }
  if (result.status === 'PARTIAL') {
    ElMessage.warning(t('channel.mappingPriceSettings.messages.savePartial'))
    return
  }
  ElMessage.error(t('channel.mappingPriceSettings.messages.saveFailed'))
}

async function handleSaveAll() {
  if (!props.channel || rows.value.length === 0) {
    return
  }
  if (!props.canSave) {
    ElMessage.warning(t('channel.mappingPriceSettings.messages.noPermission'))
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
      ElMessage.error(t('channel.mappingPriceSettings.messages.saveAllFailed'))
      return
    }

    mergeRowsFromResponse(response.data.rows)
    showSaveResultMessage(response.data)
    emit('saved')
  } catch (error) {
    console.error('保存全部映射行失败:', error)
    ElMessage.error(t('channel.mappingPriceSettings.messages.saveAllFailedRetry'))
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
      ElMessage.error(t('channel.mappingPriceSettings.messages.saveRowFailed'))
      return
    }

    mergeRowsFromResponse(response.data.rows)
    showSaveResultMessage(response.data)
    emit('saved')
  } catch (error) {
    console.error('保存本行映射价格失败:', error)
    ElMessage.error(t('channel.mappingPriceSettings.messages.saveRowFailedRetry'))
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
      ElMessage.error(t('channel.mappingPriceSettings.messages.retryRowFailed'))
      return
    }

    mergeRowsFromResponse(response.data.rows)
    showSaveResultMessage(response.data)
    emit('saved')
  } catch (error) {
    console.error('重试本行映射价格失败:', error)
    ElMessage.error(t('channel.mappingPriceSettings.messages.retryRowFailedRetry'))
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
    ElMessage.info(t('channel.mappingPriceSettings.messages.noFailedRows'))
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
      ElMessage.error(t('channel.mappingPriceSettings.messages.retryFailedRowsFailed'))
      return
    }

    mergeRowsFromResponse(response.data.rows)
    showSaveResultMessage(response.data)
    emit('saved')
  } catch (error) {
    console.error('重试失败映射行失败:', error)
    ElMessage.error(t('channel.mappingPriceSettings.messages.retryFailedRowsFailedRetry'))
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
    return t('channel.mappingPriceSettings.statuses.syncing')
  }
  if (row.dirty) {
    return t('channel.mappingPriceSettings.statuses.unsaved')
  }
  if (row.syncStatus === 'SUCCESS') {
    return t('channel.mappingPriceSettings.statuses.success')
  }
  if (row.syncStatus === 'FAILED') {
    return t('channel.mappingPriceSettings.statuses.failed')
  }
  if (row.syncStatus === 'STALE_MAPPING') {
    return t('channel.mappingPriceSettings.statuses.stale')
  }
  if (row.syncStatus === 'SYNCING') {
    return t('channel.mappingPriceSettings.statuses.syncing')
  }
  return t('channel.mappingPriceSettings.statuses.unsynced')
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
    parts.push(t('channel.mappingPriceSettings.dimension.occupancy', { value: row.occupancy }))
  }
  if (row.applicableNoOfGuest) {
    parts.push(
      t('channel.mappingPriceSettings.dimension.applicableGuest', {
        value: row.applicableNoOfGuest,
      }),
    )
  }
  if (parts.length === 0) {
    return '-'
  }
  return parts.join(' / ')
}

function formatLocalInfo(row: MappingPriceDraftRow): string {
  const parts: string[] = []
  if (row.localRoomId) {
    parts.push(t('channel.mappingPriceSettings.localInfo.roomType', { value: row.localRoomId }))
  }
  if (row.localRatePlanId) {
    parts.push(t('channel.mappingPriceSettings.localInfo.ratePlan', { value: row.localRatePlanId }))
  }
  if (parts.length === 0) {
    return '-'
  }
  return parts.join(' / ')
}

function formatRemoteInfo(row: MappingPriceDraftRow): string {
  const parts: string[] = []
  if (row.listingId) {
    parts.push(t('channel.mappingPriceSettings.remoteInfo.listing', { value: row.listingId }))
  }
  if (row.remoteRoomId) {
    parts.push(t('channel.mappingPriceSettings.remoteInfo.room', { value: row.remoteRoomId }))
  }
  if (row.remoteRatePlanId) {
    parts.push(t('channel.mappingPriceSettings.remoteInfo.ratePlan', { value: row.remoteRatePlanId }))
  }
  if (parts.length === 0) {
    return '-'
  }
  return parts.join(' / ')
}

function isAirbnbRow(row: MappingPriceDraftRow): boolean {
  const rowChannelCode =
    row.channelCode || settings.value?.channelCode || props.channel?.channelCode
  if (rowChannelCode && rowChannelCode.toUpperCase() === AIRBNB_CHANNEL_CODE) {
    return true
  }
  const rowSuChannelId = row.suChannelId || settings.value?.suChannelId
  return rowSuChannelId === AIRBNB_SU_CHANNEL_ID
}

function countCodePoints(value: string): number {
  return Array.from(value).length
}

function resolveAirbnbListingNameLength(row: MappingPriceDraftRow): number | null {
  if (
    typeof row.lastAirbnbListingNameLength === 'number' &&
    Number.isFinite(row.lastAirbnbListingNameLength)
  ) {
    return row.lastAirbnbListingNameLength
  }
  if (row.lastAirbnbListingName) {
    return countCodePoints(row.lastAirbnbListingName)
  }
  return null
}

function formatAirbnbListingTitleEvidence(row: MappingPriceDraftRow): string {
  if (!isAirbnbRow(row)) {
    return ''
  }

  const listingName = row.lastAirbnbListingName?.trim() || ''
  const listingNameLength = resolveAirbnbListingNameLength(row)
  if (!listingName && listingNameLength === null) {
    return ''
  }
  if (listingName && listingNameLength !== null) {
    return t('channel.mappingPriceSettings.failureDetails.airbnbListingTitleWithLength', {
      length: listingNameLength,
      limit: AIRBNB_LISTING_TITLE_LIMIT,
      title: listingName,
    })
  }
  if (listingName) {
    return t('channel.mappingPriceSettings.failureDetails.airbnbListingTitleOnly', {
      title: listingName,
    })
  }
  return t('channel.mappingPriceSettings.failureDetails.airbnbListingTitleLengthOnly', {
    length: listingNameLength,
    limit: AIRBNB_LISTING_TITLE_LIMIT,
  })
}

function formatFailureDetail(row: MappingPriceDraftRow): string {
  if (!row.lastError) {
    return ''
  }
  const parts = [row.lastError]
  const titleEvidence = formatAirbnbListingTitleEvidence(row)
  if (titleEvidence) {
    parts.push(titleEvidence)
  }
  return parts.join('\n')
}

function formatFailureSummary(row: MappingPriceDraftRow): string {
  return formatFailureDetail(row).replace(/\s+/g, ' ')
}

function formatDateTime(value?: string | null): string {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString(locale.value)
}

async function confirmClose(): Promise<boolean> {
  if (savingCount.value > 0) {
    try {
      await ElMessageBox.confirm(
        t('channel.mappingPriceSettings.confirmClose.savingMessage'),
        t('channel.mappingPriceSettings.confirmClose.title'),
        {
          confirmButtonText: t('channel.mappingPriceSettings.confirmClose.close'),
          cancelButtonText: t('channel.mappingPriceSettings.confirmClose.wait'),
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
    await ElMessageBox.confirm(
      t('channel.mappingPriceSettings.confirmClose.dirtyMessage'),
      t('channel.mappingPriceSettings.confirmClose.title'),
      {
        confirmButtonText: t('channel.mappingPriceSettings.confirmClose.discard'),
        cancelButtonText: t('channel.mappingPriceSettings.confirmClose.edit'),
        type: 'warning',
      },
    )
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
        <span class="drawer-title-text">
          {{ t('channel.mappingPriceSettings.title', { channel: channelTitle }) }}
        </span>
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
          <span class="summary-label">{{ t('channel.mappingPriceSettings.summary.rows') }}</span>
          <strong>{{ totalCount }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">{{ t('channel.mappingPriceSettings.summary.synced') }}</span>
          <strong>{{ successCount }}</strong>
        </div>
        <div class="summary-item danger">
          <span class="summary-label">{{ t('channel.mappingPriceSettings.summary.failed') }}</span>
          <strong>{{ failedCount }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">{{ t('channel.mappingPriceSettings.summary.unsynced') }}</span>
          <strong>{{ unsyncedCount }}</strong>
        </div>
        <div class="summary-item warning">
          <span class="summary-label">{{ t('channel.mappingPriceSettings.summary.stale') }}</span>
          <strong>{{ staleCount }}</strong>
        </div>
        <div class="summary-item dirty">
          <span class="summary-label">{{ t('channel.mappingPriceSettings.summary.dirty') }}</span>
          <strong>{{ dirtyCount }}</strong>
        </div>
      </div>

      <div class="toolbar">
        <div class="batch-controls">
          <el-radio-group v-model="batchField" size="small">
            <el-radio-button :label="BATCH_MULTIPLIER">
              {{ t('channel.mappingPriceSettings.fields.multiplier') }}
            </el-radio-button>
            <el-radio-button :label="BATCH_SURCHARGE">
              {{ t('channel.mappingPriceSettings.fields.surcharge') }}
            </el-radio-button>
          </el-radio-group>

          <el-input-number
            v-if="batchField === BATCH_MULTIPLIER"
            v-model="batchMultiplier"
            class="batch-input"
            :min="0"
            :step="NUMBER_STEP"
            :precision="NUMBER_PRECISION"
            controls-position="right"
            :placeholder="t('channel.mappingPriceSettings.fields.multiplier')"
          />
          <el-input-number
            v-else
            v-model="batchSurcharge"
            class="batch-input"
            :step="NUMBER_STEP"
            :precision="NUMBER_PRECISION"
            controls-position="right"
            :placeholder="t('channel.mappingPriceSettings.fields.surcharge')"
          />

          <el-button
            type="primary"
            plain
            :icon="Setting"
            :disabled="!canUseActions || !hasRows"
            @click="handleApplyBatch"
          >
            {{ t('channel.mappingPriceSettings.actions.applyAllRows') }}
          </el-button>
        </div>

        <div class="filter-controls">
          <el-radio-group v-model="filterMode" size="small">
            <el-radio-button :label="FILTER_ALL">
              {{ t('channel.mappingPriceSettings.filters.all') }}
            </el-radio-button>
            <el-radio-button :label="FILTER_FAILED">
              {{ t('channel.mappingPriceSettings.filters.failed') }}
            </el-radio-button>
            <el-radio-button :label="FILTER_DIRTY">
              {{ t('channel.mappingPriceSettings.filters.dirty') }}
            </el-radio-button>
          </el-radio-group>
          <el-button :icon="Refresh" :loading="loading" :disabled="isBusy" @click="loadRows">
            {{ t('channel.mappingPriceSettings.actions.refresh') }}
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
          :empty-text="t('channel.mappingPriceSettings.empty')"
        >
          <el-table-column :label="t('channel.mappingPriceSettings.table.mapping')" min-width="210">
            <template #default="{ row }">
              <div class="mapping-name">
                {{ row.displayName || t('channel.mappingPriceSettings.unnamedMapping') }}
              </div>
              <div class="mapping-meta">
                <span v-if="row.mappingStatus">{{ row.mappingStatus }}</span>
                <span v-if="row.suChannelId">Su {{ row.suChannelId }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column :label="t('channel.mappingPriceSettings.table.local')" min-width="180">
            <template #default="{ row }">
              <span class="line-text">{{ formatLocalInfo(row) }}</span>
            </template>
          </el-table-column>

          <el-table-column :label="t('channel.mappingPriceSettings.table.remote')" min-width="210">
            <template #default="{ row }">
              <span class="line-text">{{ formatRemoteInfo(row) }}</span>
            </template>
          </el-table-column>

          <el-table-column :label="t('channel.mappingPriceSettings.table.dimension')" min-width="150">
            <template #default="{ row }">
              <span class="line-text">{{ formatMappingDimension(row) }}</span>
            </template>
          </el-table-column>

          <el-table-column :label="t('channel.mappingPriceSettings.fields.multiplier')" width="150" align="center">
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

          <el-table-column :label="t('channel.mappingPriceSettings.fields.surcharge')" width="150" align="center">
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

          <el-table-column :label="t('channel.mappingPriceSettings.table.syncStatus')" width="150" align="center">
            <template #default="{ row }">
              <div class="status-cell">
                <el-tag :type="resolveStatusType(row)" effect="light">
                  {{ formatStatus(row) }}
                </el-tag>
                <span v-if="row.retryCount" class="status-note">
                  {{ t('channel.mappingPriceSettings.retryCount', { count: row.retryCount }) }}
                </span>
                <span v-if="row.lastSyncedAt && !row.dirty" class="status-note">
                  {{ formatDateTime(row.lastSyncedAt) }}
                </span>
              </div>
            </template>
          </el-table-column>

          <el-table-column :label="t('channel.mappingPriceSettings.table.failureReason')" min-width="200">
            <template #default="{ row }">
              <el-tooltip
                v-if="formatFailureDetail(row)"
                :content="formatFailureDetail(row)"
                :popper-style="FAILURE_TOOLTIP_STYLE"
                placement="top"
              >
                <span class="error-text">{{ formatFailureSummary(row) }}</span>
              </el-tooltip>
              <span v-else class="muted-text">-</span>
            </template>
          </el-table-column>

          <el-table-column :label="t('channel.mappingPriceSettings.table.actions')" width="190" fixed="right" align="center">
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
                  {{ t('channel.mappingPriceSettings.actions.saveRow') }}
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
                  {{ t('channel.mappingPriceSettings.actions.retry') }}
                </el-button>
                <el-button
                  type="info"
                  link
                  :icon="Close"
                  :disabled="!row.dirty || row.saving"
                  @click="handleResetRowDraft(row)"
                >
                  {{ t('channel.mappingPriceSettings.actions.reset') }}
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
          <span v-if="dirtyCount > 0">
            {{ t('channel.mappingPriceSettings.footer.dirtyRows', { count: dirtyCount }) }}
          </span>
          <span v-else>{{ t('channel.mappingPriceSettings.footer.clean') }}</span>
        </div>
        <div class="footer-actions">
          <el-button @click="requestClose">
            {{ t('channel.mappingPriceSettings.actions.cancel') }}
          </el-button>
          <el-button
            :icon="RefreshRight"
            :loading="retryingFailed"
            :disabled="!canUseActions || failedCount === 0"
            @click="handleRetryFailed"
          >
            {{ t('channel.mappingPriceSettings.actions.retryFailed') }}
          </el-button>
          <el-button
            type="primary"
            :icon="Check"
            :loading="savingAll"
            :disabled="!props.canSave || loading || rows.length === 0"
            @click="handleSaveAll"
          >
            {{ t('channel.mappingPriceSettings.actions.saveAll') }}
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
