<template>
  <div class="mapping-tab">
    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-select
          :model-value="connectionStatus"
          placeholder="连接状态"
          style="width: 140px"
          @update:model-value="(val: string) => emit('update:connectionStatus', val)"
        >
          <el-option label="全部" value="all" />
          <el-option label="已连接" value="connected" />
          <el-option label="未连接" value="disconnected" />
        </el-select>
      </div>
      <div class="filter-right">
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
        <el-button @click="emit('importOrders')">导入未来订单</el-button>
        <el-button type="primary" :icon="Refresh" @click="emit('refresh')">
          刷新渠道信息
        </el-button>
      </div>
    </div>

    <!-- Mapping Table -->
    <div class="table-section">
      <el-table
        :data="mappingData"
        :span-method="mappingSpanMethod"
        border
        style="width: 100%"
      >
        <!-- Channel Room Type -->
        <el-table-column label="渠道房型" min-width="200">
          <template #default="{ row }">
            <div>{{ row.channelRoomType }}</div>
            <div class="sub-text">{{ row.channelRoomId }}</div>
          </template>
        </el-table-column>

        <!-- PMS Room Type -->
        <el-table-column label="PMS房型" min-width="200">
          <template #default="{ row }">
            <template v-if="editingRoomId === row.roomGroupId && row.isFirstInGroup">
              <el-select
                :model-value="row.selectedPmsRoom"
                placeholder="选择PMS房型"
                style="width: 100%"
                clearable
                @update:model-value="(val: string | null) => (row.selectedPmsRoom = val)"
              >
                <el-option
                  v-for="opt in pmsRoomOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </template>
            <template v-else>
              {{ formatPmsRoomTypeDisplay(row.pmsRoomType) }}
            </template>
          </template>
        </el-table-column>

        <!-- Channel Price Plan -->
        <el-table-column label="渠道价格计划" min-width="200">
          <template #default="{ row }">
            <div>{{ row.channelPricePlan }}</div>
            <div class="sub-text">{{ row.channelPricePlanId }}</div>
          </template>
        </el-table-column>

        <!-- PMS Price Plan -->
        <el-table-column label="PMS价格计划" min-width="200">
          <template #default="{ row }">
            <template v-if="editingRoomId === row.roomGroupId">
              <el-select
                :model-value="row.selectedPmsPricePlan"
                placeholder="选择价格计划"
                style="width: 100%"
                clearable
                @update:model-value="(val: string | null) => (row.selectedPmsPricePlan = val)"
              >
                <el-option
                  v-for="opt in pmsPricePlanOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </template>
            <template v-else>
              {{ row.pmsPricePlan || '-' }}
            </template>
          </template>
        </el-table-column>

        <!-- Status -->
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <span :class="['status-tag', getMappingStatusClass(row.status)]">
              {{ getMappingStatusText(row.status) }}
            </span>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="editingRoomId === row.roomGroupId && row.isFirstInGroup">
              <el-button type="primary" link @click="emit('save', row.roomGroupId)">
                保存
              </el-button>
              <el-button link @click="emit('cancelEdit')">取消</el-button>
            </template>
            <template v-else-if="row.isFirstInGroup">
              <template v-if="isAirbnb">
                <el-button type="primary" link @click="emit('edit', row)">编辑</el-button>
                <el-button type="danger" link @click="emit('disconnect', row)">
                  断开连接
                </el-button>
              </template>
              <template v-else>
                <el-button type="primary" link @click="emit('manage', row)">管理</el-button>
                <el-button type="danger" link @click="emit('disconnect', row)">
                  断开连接
                </el-button>
              </template>
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
import { Refresh } from '@element-plus/icons-vue'
import type { FlattenedMappingItem, HotelItem, SelectOption } from '../../types'
import { useChannelData } from '../../composables/useChannelData'

const { formatPmsRoomTypeDisplay } = useChannelData()

const props = defineProps<{
  isAirbnb: boolean
  mappingData: FlattenedMappingItem[]
  hotels: HotelItem[]
  selectedHotelId: number | null
  connectionStatus: string
  pmsRoomOptions: SelectOption[]
  pmsPricePlanOptions: SelectOption[]
  editingRoomId: string | null
}>()

const emit = defineEmits<{
  'update:selectedHotelId': [value: number | null]
  'update:connectionStatus': [value: string]
  'update:editingRoomId': [value: string | null]
  refresh: []
  importOrders: []
  edit: [row: FlattenedMappingItem]
  save: [roomGroupId: string]
  cancelEdit: []
  disconnect: [row: FlattenedMappingItem]
  manage: [row: FlattenedMappingItem]
}>()

const mappingSpanMethod = ({
  row,
  columnIndex,
}: {
  row: FlattenedMappingItem
  column: any
  rowIndex: number
  columnIndex: number
}) => {
  if (columnIndex === 0 || columnIndex === 1) {
    if (row.isFirstInGroup) return { rowspan: row.groupRowCount, colspan: 1 }
    return { rowspan: 0, colspan: 0 }
  }
  if (columnIndex === 5) {
    if (row.isFirstInGroup) return { rowspan: row.groupRowCount, colspan: 1 }
    return { rowspan: 0, colspan: 0 }
  }
  return { rowspan: 1, colspan: 1 }
}

const getMappingStatusClass = (status: string) => {
  if (status === 'connected') return 'status-connected'
  if (status === 'disconnected') return 'status-disconnected'
  return 'status-invalid'
}

const getMappingStatusText = (status: string) => {
  if (status === 'connected') return '已连接'
  if (status === 'disconnected') return '未直连'
  return '无效'
}
</script>

<style scoped>
.mapping-tab {
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

.sub-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-connected {
  color: #409eff;
  background-color: #ecf5ff;
}

.status-disconnected {
  color: #f56c6c;
  background-color: #fef0f0;
}

.status-invalid {
  color: #909399;
  background-color: #f4f4f5;
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
