<template>
  <div class="account-list-tab">
    <el-table :data="accounts" style="width: 100%">
      <el-table-column prop="account" :label="t('channel.account.account')" min-width="280" />
      <el-table-column prop="accountCode" :label="t('channel.account.accountCode')" min-width="280" />
      <el-table-column :label="t('channel.account.actions')" min-width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link @click="emit('disconnect', row)">
            {{ t('channel.account.disconnect') }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <span class="empty-text">{{ t('channel.account.empty') }}</span>
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { AirbnbAccount } from '../../types'

defineProps<{
  accounts: AirbnbAccount[]
}>()

const emit = defineEmits<{
  disconnect: [row: AirbnbAccount]
}>()

const { t } = useI18n()
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
