<template>
  <div class="mapping-tab">
    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-select
          :model-value="connectionStatus"
          :placeholder="t('channel.mapping.connectionStatus')"
          style="width: 140px"
          @update:model-value="(val: string) => emit('update:connectionStatus', val)"
        >
          <el-option :label="t('channel.mapping.statuses.all')" value="all" />
          <el-option :label="t('channel.mapping.statuses.connected')" value="connected" />
          <el-option :label="t('channel.mapping.statuses.disconnected')" value="disconnected" />
        </el-select>
      </div>
      <div class="filter-right">
        <el-select
          :model-value="selectedHotelId"
          :placeholder="t('channel.mapping.selectHotel')"
          style="width: 200px"
          disabled
          @update:model-value="(val: number | null) => emit('update:selectedHotelId', val)"
        >
          <el-option
            v-for="hotel in hotels"
            :key="hotel.id"
            :label="hotel.hotelName"
            :value="hotel.id"
          />
        </el-select>
        <span class="filter-note">{{ t('channel.mapping.localPmsOnly') }}</span>
        <el-button @click="emit('importOrders')">{{ t('channel.mapping.importFutureOrders') }}</el-button>
        <el-button type="primary" :icon="Refresh" @click="emit('refresh')">
          {{ t('channel.mapping.refreshChannelInfo') }}
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
        <el-table-column :label="t('channel.mapping.channelRoomType')" min-width="200">
          <template #default="{ row }">
            <div>{{ row.channelRoomType }}</div>
            <div class="sub-text">{{ row.channelRoomId }}</div>
          </template>
        </el-table-column>

        <!-- PMS Room Type -->
        <el-table-column :label="t('channel.mapping.pmsRoomType')" min-width="200">
          <template #default="{ row }">
            <template v-if="editingRoomId === row.roomGroupId && row.isFirstInGroup">
              <el-select
                :model-value="row.selectedPmsRoom"
                :placeholder="t('channel.mapping.selectPmsRoomType')"
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
        <el-table-column :label="t('channel.mapping.channelPricePlan')" min-width="200">
          <template #default="{ row }">
            <div>{{ row.channelPricePlan }}</div>
            <div class="sub-text">{{ row.channelPricePlanId }}</div>
          </template>
        </el-table-column>

        <!-- PMS Price Plan -->
        <el-table-column :label="t('channel.mapping.pmsPricePlan')" min-width="200">
          <template #default="{ row }">
            <template v-if="editingRoomId === row.roomGroupId">
              <el-select
                :model-value="row.selectedPmsPricePlan"
                :placeholder="t('channel.mapping.selectPricePlan')"
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
        <el-table-column :label="t('channel.mapping.status')" min-width="100">
          <template #default="{ row }">
            <span :class="['status-tag', getMappingStatusClass(row.status)]">
              {{ getMappingStatusText(row.status) }}
            </span>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column :label="t('channel.mapping.actions')" min-width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="editingRoomId === row.roomGroupId && row.isFirstInGroup">
              <el-tooltip :content="t('channel.messages.channelWriteNotReady')" placement="top">
                <span class="disabled-action">
                  <el-button type="primary" link disabled @click="emit('save', row.roomGroupId)">
                    {{ t('channel.mapping.save') }}
                  </el-button>
                </span>
              </el-tooltip>
              <el-button link @click="emit('cancelEdit')">{{ t('channel.mapping.cancel') }}</el-button>
            </template>
            <template v-else-if="row.isFirstInGroup">
              <template v-if="isAirbnb">
                <el-tooltip :content="t('channel.messages.channelWriteNotReady')" placement="top">
                  <span class="disabled-action">
                    <el-button type="primary" link disabled @click="emit('edit', row)">
                      {{ t('channel.mapping.edit') }}
                    </el-button>
                  </span>
                </el-tooltip>
                <el-tooltip :content="t('channel.messages.channelWriteNotReady')" placement="top">
                  <span class="disabled-action">
                    <el-button type="danger" link disabled @click="emit('disconnect', row)">
                      {{ t('channel.mapping.disconnect') }}
                    </el-button>
                  </span>
                </el-tooltip>
              </template>
              <template v-else>
                <el-tooltip :content="t('channel.messages.channelWriteNotReady')" placement="top">
                  <span class="disabled-action">
                    <el-button type="primary" link disabled @click="emit('manage', row)">
                      {{ t('channel.mapping.manage') }}
                    </el-button>
                  </span>
                </el-tooltip>
                <el-tooltip :content="t('channel.messages.channelWriteNotReady')" placement="top">
                  <span class="disabled-action">
                    <el-button type="danger" link disabled @click="emit('disconnect', row)">
                      {{ t('channel.mapping.disconnect') }}
                    </el-button>
                  </span>
                </el-tooltip>
              </template>
            </template>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <p class="empty-text">{{ t('channel.mapping.empty') }}</p>
          </div>
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Refresh } from '@element-plus/icons-vue'
import type { FlattenedMappingItem, HotelItem, SelectOption } from '../../types'
import { useChannelData } from '../../composables/useChannelData'

const { formatPmsRoomTypeDisplay } = useChannelData()
const { t } = useI18n()

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
  if (status === 'connected') return t('channel.mapping.statuses.connected')
  if (status === 'disconnected') return t('channel.mapping.statuses.disconnected')
  return t('channel.mapping.statuses.invalid')
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
  flex-wrap: wrap;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-bar .el-button {
  white-space: normal;
}

.filter-note {
  color: #909399;
  font-size: 12px;
  line-height: 20px;
  max-width: 220px;
}

.disabled-action {
  display: inline-block;
}

.disabled-action + .disabled-action,
.disabled-action + .el-button {
  margin-left: 12px;
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
