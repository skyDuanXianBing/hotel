<template>
  <div class="price-ratio-panel">
    <div class="settings-content">
      <div class="price-ratio-table-container">
        <el-table :data="data" border stripe class="price-ratio-table" v-loading="loading">
          <el-table-column prop="channel" :label="t('channel.priceRatio.channel')" min-width="150" align="center" />
          <el-table-column prop="ratio" :label="t('channel.priceRatio.ratio')" min-width="200" align="center" />
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
                    :disabled="!canManage"
                    @click="handleEdit(row)"
                  >
                    {{ t('channel.priceRatio.edit') }}
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

.price-ratio-action {
  display: inline-flex;
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
