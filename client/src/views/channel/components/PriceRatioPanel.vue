<template>
  <div class="price-ratio-panel">
    <div class="settings-content">
      <div class="price-ratio-table-container">
        <el-table :data="data" border stripe class="price-ratio-table" v-loading="loading">
          <el-table-column :label="t('channel.priceRatio.channel')" min-width="170" align="center">
            <template #default="{ row }">
              <div class="channel-cell">
                <span class="channel-name">{{ row.channel }}</span>
                <el-tag v-if="row.channelCode" size="small" effect="plain" type="info">
                  {{ row.channelCode }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="ratio" :label="t('channel.priceRatio.ratio')" min-width="200" align="center" />
          <el-table-column label="Su 同步状态" min-width="180" align="center">
            <template #default="{ row }">
              <el-tooltip :content="formatSyncDetails(row)" placement="top" :show-after="200">
                <el-tag :type="resolveSyncTagType(row)" effect="light" class="sync-status-tag">
                  {{ formatSyncStatus(row) }}
                </el-tag>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column :label="t('channel.priceRatio.actions')" width="150" align="center">
            <template #default="{ row }">
              <el-tooltip
                :content="t('stage6.common.messages.noPermission')"
                :disabled="canManage"
                placement="top"
              >
                <span class="price-ratio-action">
                  <el-button
                    type="text"
                    size="small"
                    :icon="Setting"
                    :disabled="!canManage"
                    @click="handleEdit(row)"
                  >
                    设置价格比例
                  </el-button>
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>

        <!-- 空状态 -->
        <div v-if="!loading && data.length === 0" class="empty-state">
          <p class="empty-text">{{ t('channel.priceRatio.empty') }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Setting } from '@element-plus/icons-vue'
import type { PriceRatioItem } from '../types'

const props = defineProps<{
  data: PriceRatioItem[]
  loading: boolean
  canManage: boolean
}>()

const emit = defineEmits<{
  edit: [row: PriceRatioItem]
}>()

const { t } = useI18n()

function handleEdit(row: PriceRatioItem) {
  if (!props.canManage) {
    return
  }

  emit('edit', row)
}

function formatSyncStatus(row: PriceRatioItem) {
  const sync = row.suMappingSync
  if (!sync) {
    return '保存后同步'
  }
  if (sync.status === 'SUCCESS') {
    return `已同步 ${sync.successCount}/${sync.totalCount}`
  }
  if (sync.status === 'PARTIAL') {
    return `部分失败 ${sync.successCount}/${sync.totalCount}`
  }
  if (sync.status === 'FAILED') {
    return '同步失败'
  }
  return '未同步'
}

function resolveSyncTagType(row: PriceRatioItem) {
  const sync = row.suMappingSync
  if (!sync) {
    return 'info'
  }
  if (sync.status === 'SUCCESS') {
    return 'success'
  }
  if (sync.status === 'PARTIAL') {
    return 'warning'
  }
  if (sync.status === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

function formatSyncDetails(row: PriceRatioItem) {
  const sync = row.suMappingSync
  if (!sync) {
    return '保存价格比例后会同步到 Su 映射价格设置'
  }
  if (!sync.items || sync.items.length === 0) {
    return sync.message || '暂无同步明细'
  }

  const details = sync.items.slice(0, 5).map((item) => {
    const target = item.listingId || item.channelRoomId || item.roomId || '未知映射'
    const message = item.message || item.status
    return `${target}: ${message}`
  })
  if (sync.items.length > details.length) {
    details.push(`另有 ${sync.items.length - details.length} 条结果`)
  }
  return details.join('\n')
}
</script>

<style scoped>
.price-ratio-panel {
  height: 100%;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.settings-content {
  flex: 1;
  padding: 24px;
}

.price-ratio-table-container {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.price-ratio-table {
  width: 100%;
}

.channel-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.channel-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-ratio-action {
  display: inline-flex;
}

.sync-status-tag {
  min-width: 96px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-text {
  font-size: 14px;
  color: #909399;
  line-height: 1.8;
  margin: 0;
}
</style>
