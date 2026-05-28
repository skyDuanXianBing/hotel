<template>
  <div class="room-status-channel">
    <div class="header-notice">
      <el-alert
        :title="t('pages.roomStatusChannel.notice')"
        type="info"
        :closable="false"
        show-icon
      />
    </div>

    <div class="toolbar">
      <div class="date-section">
        <label>{{ t('pages.roomStatusChannel.date') }}</label>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          :placeholder="t('pages.roomStatusChannel.datePlaceholder')"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
        />
      </div>

      <div class="room-type-filter">
        <el-select
          v-model="selectedRoomType"
          :placeholder="t('pages.roomStatusChannel.localRoomTypePlaceholder')"
          @change="onRoomTypeChange"
        >
          <el-option :label="t('pages.roomStatusChannel.all')" value="" />
          <el-option
            v-for="roomType in roomTypes"
            :key="roomType.id"
            :label="roomType.name"
            :value="roomType.id"
          />
        </el-select>
      </div>

      <div class="actions">
        <el-button type="primary" @click="loadStatusLogs">
          {{ t('pages.roomStatusChannel.statusLog') }}
        </el-button>
        <el-button type="primary">{{ t('pages.roomStatusChannel.bulkEdit') }}</el-button>
      </div>
    </div>

    <div class="channel-content" v-loading="loading">
      <div class="channel-table">
        <div class="table-header">
          <div class="column-label">{{ t('pages.roomStatusChannel.localRoomType') }}</div>
          <div class="column-label">{{ t('pages.roomStatusChannel.channelRoomType') }}</div>
        </div>

        <div class="table-body">
          <div v-if="channelData.length === 0" class="empty-state">
            <div class="empty-icon">
              <el-icon size="48"><Box /></el-icon>
            </div>
            <div class="empty-text">{{ t('pages.roomStatusChannel.empty') }}</div>
          </div>

          <div v-else class="channel-mapping-list">
            <div v-for="mapping in channelData" :key="mapping.id" class="mapping-row">
              <div class="local-room-type">
                <div class="room-type-info">
                  <div class="room-type-name">{{ mapping.localRoomType.name }}</div>
                  <div class="room-type-details">
                    <span class="total-rooms">
                      {{ t('pages.roomStatusChannel.totalRooms', { count: mapping.localRoomType.totalRooms }) }}
                    </span>
                    <span class="available-rooms">
                      {{ t('pages.roomStatusChannel.availableRooms', { count: mapping.availableRooms }) }}
                    </span>
                  </div>
                </div>
              </div>

              <div class="channel-room-types">
                <div
                  v-for="channelMapping in mapping.channelMappings"
                  :key="channelMapping.channelId"
                  class="channel-mapping"
                >
                  <div class="channel-info">
                    <div class="channel-name">{{ channelMapping.channelName }}</div>
                    <div class="mapping-details">
                      <span class="mapped-name">{{ channelMapping.channelRoomTypeName }}</span>
                      <span class="room-count"
                        >{{ channelMapping.availableCount }}/{{ channelMapping.totalCount }}</span
                      >
                    </div>
                  </div>

                  <div class="channel-actions">
                    <el-button size="small" @click="editChannelMapping(mapping, channelMapping)">
                      {{ t('pages.roomStatusChannel.edit') }}
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showEditDialog"
      :title="t('pages.roomStatusChannel.dialog.title')"
      width="600px"
    >
      <div class="edit-dialog-content">
        <el-form label-width="120px">
          <el-form-item :label="t('pages.roomStatusChannel.dialog.localRoomType')">
            <span>{{ editingMapping?.localRoomType.name }}</span>
          </el-form-item>

          <el-form-item :label="t('pages.roomStatusChannel.dialog.channel')">
            <span>{{ editingChannelMapping?.channelName }}</span>
          </el-form-item>

          <el-form-item :label="t('pages.roomStatusChannel.dialog.channelRoomTypeName')">
            <el-input
              v-model="editForm.channelRoomTypeName"
              :placeholder="t('pages.roomStatusChannel.dialog.channelRoomTypePlaceholder')"
            />
          </el-form-item>

          <el-form-item :label="t('pages.roomStatusChannel.dialog.availableCount')">
            <el-input-number
              v-model="editForm.availableCount"
              :min="0"
              :max="editingMapping?.localRoomType.totalRooms || 0"
              controls-position="right"
            />
          </el-form-item>

          <el-form-item :label="t('pages.roomStatusChannel.dialog.totalCount')">
            <el-input-number
              v-model="editForm.totalCount"
              :min="0"
              :max="editingMapping?.localRoomType.totalRooms || 0"
              controls-position="right"
            />
          </el-form-item>

          <el-form-item :label="t('pages.roomStatusChannel.dialog.price')">
            <el-input-number
              v-model="editForm.price"
              :min="0"
              :precision="2"
              controls-position="right"
            />
            <span style="margin-left: 10px">{{ t('pages.roomStatusChannel.dialog.currencyUnit') }}</span>
          </el-form-item>

          <el-form-item :label="t('pages.roomStatusChannel.dialog.notes')">
            <el-input
              v-model="editForm.notes"
              type="textarea"
              :rows="3"
              :placeholder="t('pages.roomStatusChannel.dialog.notesPlaceholder')"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="showEditDialog = false">
          {{ t('pages.roomStatusChannel.dialog.cancel') }}
        </el-button>
        <el-button type="primary" @click="confirmEdit">
          {{ t('pages.roomStatusChannel.dialog.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showLogsDialog"
      :title="t('pages.roomStatusChannel.logs.title')"
      width="800px"
    >
      <div class="logs-content">
        <el-table :data="statusLogs" stripe>
          <el-table-column prop="roomNumber" :label="t('pages.roomStatusChannel.logs.roomNumber')" width="100" />
          <el-table-column prop="date" :label="t('pages.roomStatusChannel.logs.date')" width="120" />
          <el-table-column prop="oldStatus" :label="t('pages.roomStatusChannel.logs.oldStatus')" width="120">
            <template #default="{ row }">
              <span class="status-tag">{{ getStatusText(row.oldStatus) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="newStatus" :label="t('pages.roomStatusChannel.logs.newStatus')" width="120">
            <template #default="{ row }">
              <span class="status-tag">{{ getStatusText(row.newStatus) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" :label="t('pages.roomStatusChannel.logs.reason')" />
          <el-table-column prop="operatorName" :label="t('pages.roomStatusChannel.logs.operator')" width="100" />
          <el-table-column prop="operatedAt" :label="t('pages.roomStatusChannel.logs.operatedAt')" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.operatedAt) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Box } from '@element-plus/icons-vue'
import { useRoomStatusStore } from '@/stores/roomStatus'
import type { BaseRoomType, RoomStatus } from '@/types/room'

const { t, locale } = useI18n()
const roomStatusStore = useRoomStatusStore()

interface ChannelMapping {
  channelId: number
  channelName: string
  channelRoomTypeName: string
  availableCount: number
  totalCount: number
  price: number
}

interface LocalRoomTypeMapping {
  id: number
  localRoomType: BaseRoomType
  availableRooms: number
  channelMappings: ChannelMapping[]
}

const selectedDate = ref<string>(new Date().toISOString().split('T')[0])
const selectedRoomType = ref<string>('')
const showEditDialog = ref(false)
const showLogsDialog = ref(false)
const editingMapping = ref<LocalRoomTypeMapping | null>(null)
const editingChannelMapping = ref<ChannelMapping | null>(null)

const editForm = ref({
  channelRoomTypeName: '',
  availableCount: 0,
  totalCount: 0,
  price: 0,
  notes: '',
})

const mockRoomTypes = [
  {
    id: 1,
    name: t('pages.roomStatusChannel.roomTypes.deluxeDouble'),
    code: 'DBF',
    totalRooms: 10,
    description: t('pages.roomStatusChannel.roomTypes.deluxeDoubleDesc'),
  },
  {
    id: 2,
    name: t('pages.roomStatusChannel.roomTypes.standardTwin'),
    code: 'BZJ',
    totalRooms: 15,
    description: t('pages.roomStatusChannel.roomTypes.standardTwinDesc'),
  },
  {
    id: 3,
    name: t('pages.roomStatusChannel.roomTypes.suite'),
    code: 'TF',
    totalRooms: 5,
    description: t('pages.roomStatusChannel.roomTypes.suiteDesc'),
  },
]

const mockChannelData: LocalRoomTypeMapping[] = [
  {
    id: 1,
    localRoomType: {
      id: 1,
      name: t('pages.roomStatusChannel.roomTypes.deluxeDouble'),
      code: 'DBF',
      totalRooms: 10,
      description: t('pages.roomStatusChannel.roomTypes.deluxeDoubleDesc'),
    },
    availableRooms: 8,
    channelMappings: [
      {
        channelId: 1,
        channelName: t('pages.roomStatusChannel.channels.ctrip'),
        channelRoomTypeName: t('pages.roomStatusChannel.mappingNames.ctripDeluxeDouble'),
        availableCount: 5,
        totalCount: 8,
        price: 288,
      },
      {
        channelId: 2,
        channelName: t('pages.roomStatusChannel.channels.meituan'),
        channelRoomTypeName: t('pages.roomStatusChannel.mappingNames.meituanDeluxeDouble'),
        availableCount: 3,
        totalCount: 5,
        price: 268,
      },
    ],
  },
  {
    id: 2,
    localRoomType: {
      id: 2,
      name: t('pages.roomStatusChannel.roomTypes.standardTwin'),
      code: 'BZJ',
      totalRooms: 15,
      description: t('pages.roomStatusChannel.roomTypes.standardTwinDesc'),
    },
    availableRooms: 12,
    channelMappings: [
      {
        channelId: 1,
        channelName: t('pages.roomStatusChannel.channels.ctrip'),
        channelRoomTypeName: t('pages.roomStatusChannel.mappingNames.ctripStandardTwin'),
        availableCount: 8,
        totalCount: 12,
        price: 198,
      },
      {
        channelId: 3,
        channelName: t('pages.roomStatusChannel.channels.fliggy'),
        channelRoomTypeName: t('pages.roomStatusChannel.mappingNames.fliggyStandardTwin'),
        availableCount: 4,
        totalCount: 8,
        price: 188,
      },
    ],
  },
  {
    id: 3,
    localRoomType: {
      id: 3,
      name: t('pages.roomStatusChannel.roomTypes.suite'),
      code: 'TF',
      totalRooms: 5,
      description: t('pages.roomStatusChannel.roomTypes.suiteDesc'),
    },
    availableRooms: 4,
    channelMappings: [
      {
        channelId: 1,
        channelName: t('pages.roomStatusChannel.channels.ctrip'),
        channelRoomTypeName: t('pages.roomStatusChannel.mappingNames.ctripSuite'),
        availableCount: 2,
        totalCount: 4,
        price: 588,
      },
    ],
  },
]

const mockStatusLogs = [
  {
    id: 1,
    roomId: 1,
    roomNumber: 'a01',
    date: '2025-09-23',
    oldStatus: 'AVAILABLE',
    newStatus: 'MAINTENANCE',
    reason: t('pages.roomStatusChannel.logReasons.cleaningMaintenance'),
    operatorName: t('pages.roomStatusChannel.operator'),
    operatedAt: '2025-09-23T10:30:00',
  },
  {
    id: 2,
    roomId: 2,
    roomNumber: 'a02',
    date: '2025-09-23',
    oldStatus: 'MAINTENANCE',
    newStatus: 'AVAILABLE',
    reason: t('pages.roomStatusChannel.logReasons.maintenanceCompleted'),
    operatorName: t('pages.roomStatusChannel.operator'),
    operatedAt: '2025-09-23T14:15:00',
  },
]

const channelData = ref<LocalRoomTypeMapping[]>(mockChannelData)
const loading = ref(false)
const roomTypes = ref(mockRoomTypes)
const statusLogs = ref(mockStatusLogs)

const onDateChange = (value: string | null) => {
  if (value) {
    selectedDate.value = value
    loadChannelData()
  }
}

const onRoomTypeChange = () => {
  loadChannelData()
}

const editChannelMapping = (mapping: LocalRoomTypeMapping, channelMapping: ChannelMapping) => {
  editingMapping.value = mapping
  editingChannelMapping.value = channelMapping

  editForm.value = {
    channelRoomTypeName: channelMapping.channelRoomTypeName,
    availableCount: channelMapping.availableCount,
    totalCount: channelMapping.totalCount,
    price: channelMapping.price,
    notes: '',
  }

  showEditDialog.value = true
}

const confirmEdit = async () => {
  try {
    if (editingChannelMapping.value) {
      Object.assign(editingChannelMapping.value, editForm.value)
    }

    ElMessage.success(t('pages.roomStatusChannel.messages.updateSuccess'))
    showEditDialog.value = false
  } catch {
    ElMessage.error(t('pages.roomStatusChannel.messages.updateFailed'))
  }
}

const getStatusText = (status: RoomStatus) => {
  const statusMap: Record<string, string> = {
    AVAILABLE: t('pages.roomStatusChannel.statuses.AVAILABLE'),
    OCCUPIED: t('pages.roomStatusChannel.statuses.OCCUPIED'),
    RESERVED: t('pages.roomStatusChannel.statuses.RESERVED'),
    MAINTENANCE: t('pages.roomStatusChannel.statuses.MAINTENANCE'),
    OUT_OF_ORDER: t('pages.roomStatusChannel.statuses.OUT_OF_ORDER'),
  }
  return statusMap[status] || t('pages.roomStatusChannel.statuses.unknown')
}

const formatDateTime = (dateTime: string) =>
  new Date(dateTime).toLocaleString(locale.value === 'zh-CN' ? 'zh-CN' : locale.value)

const loadChannelData = async () => {
  if (selectedRoomType.value) {
    channelData.value = mockChannelData.filter(
      (item) => item.localRoomType.id.toString() === selectedRoomType.value,
    )
  } else {
    channelData.value = mockChannelData
  }
  console.log('using static channel data')
}

const loadStatusLogs = async () => {
  showLogsDialog.value = true
  console.log('using static status logs')
}

onMounted(async () => {
  void roomStatusStore
  loadChannelData()
})
</script>

<style scoped>
.room-status-channel {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.header-notice {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  padding: 15px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.date-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-section label {
  font-weight: bold;
  color: #333;
}

.room-type-filter {
  flex: 1;
}

.actions {
  display: flex;
  gap: 10px;
}

.channel-content {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.channel-table {
  width: 100%;
}

.table-header {
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.column-label {
  padding: 20px;
  font-weight: bold;
  text-align: center;
  border-right: 1px solid #e9ecef;
}

.column-label:last-child {
  border-right: none;
}

.table-body {
  min-height: 400px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #999;
}

.empty-icon {
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
}

.channel-mapping-list {
  display: flex;
  flex-direction: column;
}

.mapping-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border-bottom: 1px solid #e9ecef;
  min-height: 100px;
}

.local-room-type {
  padding: 20px;
  border-right: 1px solid #e9ecef;
  display: flex;
  align-items: center;
}

.room-type-info {
  width: 100%;
}

.room-type-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.room-type-details {
  display: flex;
  gap: 15px;
  font-size: 14px;
  color: #666;
}

.total-rooms,
.available-rooms {
  display: inline-block;
}

.channel-room-types {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.channel-mapping {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  background: #fafafa;
}

.channel-info {
  flex: 1;
}

.channel-name {
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.mapping-details {
  display: flex;
  gap: 10px;
  font-size: 14px;
  color: #666;
}

.mapped-name {
  font-weight: 500;
}

.room-count {
  background: #e6f7ff;
  color: #1890ff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.channel-actions {
  margin-left: 15px;
}

.edit-dialog-content {
  padding: 10px 0;
}

.logs-content {
  max-height: 500px;
  overflow-y: auto;
}

.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  background: #f0f0f0;
  color: #333;
}
</style>
