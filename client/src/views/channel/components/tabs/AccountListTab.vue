<template>
  <div class="account-list-tab">
    <el-table :data="accounts" style="width: 100%">
      <el-table-column prop="account" label="账户" min-width="280" />
      <el-table-column prop="accountCode" label="账户编码" min-width="280" />
      <el-table-column label="操作" min-width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link @click="emit('disconnect', row)">断开连接</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <span class="empty-text">无数据</span>
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import type { AirbnbAccount } from '../../types'

defineProps<{
  accounts: AirbnbAccount[]
}>()

const emit = defineEmits<{
  disconnect: [row: AirbnbAccount]
}>()
</script>

<style scoped>
.account-list-tab {
  padding: 16px 0;
}

.empty-text {
  color: #909399;
  font-size: 14px;
}
</style>
