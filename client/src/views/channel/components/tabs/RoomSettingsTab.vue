<template>
  <div class="room-settings-tab">
    <!-- Header -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-date-picker
          :model-value="startDate"
          type="date"
          :placeholder="t('channel.roomSettings.selectDate')"
          value-format="YYYY-MM-DD"
          style="width: 160px"
          @update:model-value="(val: string) => emit('update:startDate', val)"
        />
      </div>
    </div>

    <!-- Room Settings Table -->
    <div class="table-section">
      <el-table :data="data" border style="width: 100%">
        <el-table-column :label="t('channel.roomSettings.airbnbRoomType')" width="250" fixed="left">
          <template #default="{ row }">
            {{ row.airbnbRoomType }}
          </template>
        </el-table-column>

        <el-table-column :label="t('channel.roomSettings.pmsRoomType')" width="180" fixed="left">
          <template #default="{ row }">
            {{ formatPmsRoomTypeDisplay(row.pmsRoomType) }}
          </template>
        </el-table-column>

        <el-table-column
          v-for="date in dates"
          :key="date.value"
          :prop="'values.' + date.value"
          min-width="80"
        >
          <template #header>
            <div class="date-header">
              <span class="date-day">{{ date.day }}</span>
              <span class="date-weekday">{{ date.weekday }}</span>
            </div>
          </template>
          <template #default="{ row }">
            <span>{{ row.values[date.value] ?? '-' }}</span>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <p class="empty-text">{{ t('channel.roomSettings.empty') }}</p>
          </div>
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { RoomSettingsRow, CalendarDate } from '../../types'
import { useChannelData } from '../../composables/useChannelData'

const { formatPmsRoomTypeDisplay } = useChannelData()
const { t } = useI18n()

defineProps<{
  data: RoomSettingsRow[]
  dates: CalendarDate[]
  startDate: string
}>()

const emit = defineEmits<{
  'update:startDate': [value: string]
}>()
</script>

<style scoped>
.room-settings-tab {
  padding: 16px 0;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.table-section {
  margin-top: 8px;
}

.date-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.4;
}

.date-day {
  font-weight: 600;
  font-size: 13px;
}

.date-weekday {
  font-size: 11px;
  color: #909399;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

.empty-text {
  color: #909399;
  font-size: 14px;
}
</style>
