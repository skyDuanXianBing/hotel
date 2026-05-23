<template>
  <div class="price-ratio-panel">
    <div class="settings-content">
      <div class="price-ratio-table-container">
        <el-table :data="data" border stripe class="price-ratio-table" v-loading="loading">
          <el-table-column prop="channel" label="渠道" min-width="150" align="center" />
          <el-table-column prop="ratio" label="价格比例" min-width="200" align="center" />
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button type="text" size="small" @click="$emit('edit', row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 空状态 -->
        <div v-if="!loading && data.length === 0" class="empty-state">
          <p class="empty-text">暂无渠道价格比例数据，请先在渠道管理中配置渠道</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PriceRatioItem } from '../types'

defineProps<{
  data: PriceRatioItem[]
  loading: boolean
}>()

defineEmits<{
  edit: [row: PriceRatioItem]
}>()
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
