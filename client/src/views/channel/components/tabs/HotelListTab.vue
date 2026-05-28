<template>
  <div class="hotel-list-tab">
    <el-table :data="hotels" border style="width: 100%">
      <el-table-column prop="hotelCode" :label="t('channel.hotel.hotelCode')" min-width="180" />
      <el-table-column prop="hotelName" :label="t('channel.hotel.hotelName')" min-width="220" />
      <el-table-column prop="priceMode" :label="t('channel.hotel.priceMode')" min-width="180" />
      <el-table-column prop="status" :label="t('channel.hotel.status')" min-width="120" />
      <el-table-column :label="t('channel.hotel.actions')" min-width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="emit('edit', row)">{{ t('channel.hotel.edit') }}</el-button>
          <el-button type="danger" link @click="emit('disconnect', row)">
            {{ t('channel.hotel.disconnect') }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <svg
            width="120"
            height="120"
            viewBox="0 0 120 120"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M60 110C87.6142 110 110 87.6142 110 60C110 32.3858 87.6142 10 60 10C32.3858 10 10 32.3858 10 60C10 87.6142 32.3858 110 60 110Z"
              fill="#F0F2F5"
            />
            <path
              d="M45 50C48.3137 50 51 47.3137 51 44C51 40.6863 48.3137 38 45 38C41.6863 38 39 40.6863 39 44C39 47.3137 41.6863 50 45 50Z"
              fill="#D9D9D9"
            />
            <path
              d="M75 50C78.3137 50 81 47.3137 81 44C81 40.6863 78.3137 38 75 38C71.6863 38 69 40.6863 69 44C69 47.3137 71.6863 50 75 50Z"
              fill="#D9D9D9"
            />
            <path
              d="M80 68C80 75.732 71.046 82 60 82C48.954 82 40 75.732 40 68"
              stroke="#D9D9D9"
              stroke-width="4"
              stroke-linecap="round"
            />
          </svg>
          <p class="empty-text">{{ t('channel.hotel.empty') }}</p>
        </div>
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { HotelItem } from '../../types'

defineProps<{
  hotels: HotelItem[]
}>()

const emit = defineEmits<{
  edit: [row: HotelItem]
  disconnect: [row: HotelItem]
}>()

const { t } = useI18n()
</script>

<style scoped>
.hotel-list-tab {
  padding: 16px 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

.empty-text {
  margin-top: 16px;
  color: #909399;
  font-size: 14px;
}
</style>
