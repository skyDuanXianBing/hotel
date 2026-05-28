<template>
  <div class="price-ratio-panel">
    <div class="settings-content">
      <div class="price-ratio-table-container">
        <el-table :data="data" border stripe class="price-ratio-table" v-loading="loading">
          <el-table-column prop="channel" :label="t('channel.priceRatio.channel')" min-width="150" align="center" />
          <el-table-column prop="ratio" :label="t('channel.priceRatio.ratio')" min-width="200" align="center" />
          <el-table-column :label="t('channel.priceRatio.actions')" width="150" align="center">
            <template #default="{ row }">
              <el-button type="text" size="small" @click="$emit('edit', row)">
                {{ t('channel.priceRatio.edit') }}
              </el-button>
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

defineProps<{
  data: PriceRatioItem[]
  loading: boolean
}>()

defineEmits<{
  edit: [row: PriceRatioItem]
}>()

const { t } = useI18n()
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
