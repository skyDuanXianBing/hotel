<template>
  <div class="calendar-tab">
    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-date-picker
          :model-value="startDate"
          type="date"
          placeholder="选择日期"
          value-format="YYYY-MM-DD"
          style="width: 160px"
          @update:model-value="(val: string) => emit('update:startDate', val)"
        />
        <el-select
          :model-value="roomType"
          placeholder="房型"
          style="width: 160px"
          @update:model-value="(val: string) => emit('update:roomType', val)"
        >
          <el-option label="全部" value="" />
        </el-select>
        <el-select
          :model-value="displayItem"
          placeholder="显示项"
          style="width: 140px"
          @update:model-value="(val: string) => emit('update:displayItem', val)"
        >
          <el-option label="全部" value="" />
          <el-option label="价格" value="price" />
          <el-option label="房量" value="inventory" />
        </el-select>
      </div>
      <div class="filter-right">
        <el-button @click="emit('syncFromCalendar')">从日历同步</el-button>
        <el-button type="primary" @click="emit('fullRefresh')">全量刷新</el-button>
        <el-select
          :model-value="selectedHotelId"
          placeholder="选择酒店"
          style="width: 200px"
          clearable
          @update:model-value="(val: number | null) => emit('update:selectedHotelId', val)"
        >
          <el-option
            v-for="hotel in hotels"
            :key="hotel.id"
            :label="hotel.hotelName"
            :value="hotel.id"
          />
        </el-select>
      </div>
    </div>

    <!-- Calendar Table -->
    <div class="table-section">
      <el-table
        :data="calendarData"
        :span-method="calendarSpanMethod"
        border
        style="width: 100%"
      >
        <el-table-column label="日期" width="120" fixed="left">
          <template #default="{ row }">
            <span :class="{ 'title-label': row.type === 'title' }">{{ row.label }}</span>
          </template>
        </el-table-column>

        <el-table-column
          v-for="date in calendarDates"
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
            <template v-if="row.type === 'title'">
              <!-- Title rows are handled by span-method -->
            </template>
            <template v-else-if="row.type === 'inventory' || row.type === 'number'">
              <span>{{ row.values[date.value] ?? '-' }}</span>
            </template>
            <template v-else-if="row.type === 'price'">
              <span class="price-cell">
                {{ row.values[date.value] != null ? `¥${row.values[date.value]}` : '-' }}
              </span>
            </template>
            <template v-else-if="row.type === 'checkbox'">
              <el-checkbox :model-value="!!row.values[date.value]" disabled />
            </template>
            <template v-else>
              <span>{{ row.values[date.value] ?? '-' }}</span>
            </template>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <p class="empty-text">无数据</p>
          </div>
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CalendarRow, CalendarDate, HotelItem } from '../../types'

const props = defineProps<{
  calendarData: CalendarRow[]
  calendarDates: CalendarDate[]
  hotels: HotelItem[]
  selectedHotelId: number | null
  startDate: string
  roomType: string
  displayItem: string
}>()

const emit = defineEmits<{
  'update:selectedHotelId': [value: number | null]
  'update:startDate': [value: string]
  'update:roomType': [value: string]
  'update:displayItem': [value: string]
  syncFromCalendar: []
  fullRefresh: []
}>()

const calendarSpanMethod = ({
  row,
  columnIndex,
}: {
  row: CalendarRow
  column: any
  rowIndex: number
  columnIndex: number
}) => {
  if (row.type === 'title' && columnIndex === 0) {
    return { rowspan: 1, colspan: props.calendarDates.length + 1 }
  }
  if (row.type === 'title' && columnIndex > 0) {
    return { rowspan: 0, colspan: 0 }
  }
  return { rowspan: 1, colspan: 1 }
}
</script>

<style scoped>
.calendar-tab {
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
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 12px;
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

.title-label {
  font-weight: 600;
  color: #303133;
}

.price-cell {
  color: #409eff;
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
