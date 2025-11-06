<template>
  <div class="room-status-channel">
    <!-- 头部说明 -->
    <div class="header-notice">
      <el-alert
        title="根据上的数字代表在渠道售卖的房间数量，最多不会超过房型的测试房量。"
        type="info"
        :closable="false"
        show-icon
      />
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="date-section">
        <label>日期</label>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
        />
      </div>

      <div class="room-type-filter">
        <el-select v-model="selectedRoomType" placeholder="本地房型" @change="onRoomTypeChange">
          <el-option label="全部" value="" />
          <el-option
            v-for="roomType in roomTypes"
            :key="roomType.id"
            :label="roomType.name"
            :value="roomType.id"
          />
        </el-select>
      </div>

      <div class="actions">
        <el-button type="primary" @click="loadStatusLogs">房态修改记录</el-button>
        <el-button type="primary">批量修改</el-button>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="channel-content" v-loading="loading">
      <div class="channel-table">
        <!-- 表头 -->
        <div class="table-header">
          <div class="column-label">本地房型</div>
          <div class="column-label">渠道房型</div>
        </div>

        <!-- 表格内容 -->
        <div class="table-body">
          <div v-if="channelData.length === 0" class="empty-state">
            <div class="empty-icon">
              <el-icon size="48"><Box /></el-icon>
            </div>
            <div class="empty-text">暂无数据</div>
          </div>

          <div v-else class="channel-mapping-list">
            <div v-for="mapping in channelData" :key="mapping.id" class="mapping-row">
              <!-- 本地房型列 -->
              <div class="local-room-type">
                <div class="room-type-info">
                  <div class="room-type-name">{{ mapping.localRoomType.name }}</div>
                  <div class="room-type-details">
                    <span class="total-rooms">总房间: {{ mapping.localRoomType.totalRooms }}</span>
                    <span class="available-rooms">可用: {{ mapping.availableRooms }}</span>
                  </div>
                </div>
              </div>

              <!-- 渠道房型列 -->
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
                      编辑
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑渠道映射弹窗 -->
    <el-dialog v-model="showEditDialog" title="编辑渠道房型映射" width="600px">
      <div class="edit-dialog-content">
        <el-form label-width="120px">
          <el-form-item label="本地房型">
            <span>{{ editingMapping?.localRoomType.name }}</span>
          </el-form-item>

          <el-form-item label="渠道">
            <span>{{ editingChannelMapping?.channelName }}</span>
          </el-form-item>

          <el-form-item label="渠道房型名称">
            <el-input v-model="editForm.channelRoomTypeName" placeholder="请输入渠道房型名称" />
          </el-form-item>

          <el-form-item label="可用房间数">
            <el-input-number
              v-model="editForm.availableCount"
              :min="0"
              :max="editingMapping?.localRoomType.totalRooms || 0"
              controls-position="right"
            />
          </el-form-item>

          <el-form-item label="总房间数">
            <el-input-number
              v-model="editForm.totalCount"
              :min="0"
              :max="editingMapping?.localRoomType.totalRooms || 0"
              controls-position="right"
            />
          </el-form-item>

          <el-form-item label="价格">
            <el-input-number
              v-model="editForm.price"
              :min="0"
              :precision="2"
              controls-position="right"
            />
            <span style="margin-left: 10px">元</span>
          </el-form-item>

          <el-form-item label="备注">
            <el-input
              v-model="editForm.notes"
              type="textarea"
              :rows="3"
              placeholder="请输入备注信息"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 房态修改记录弹窗 -->
    <el-dialog v-model="showLogsDialog" title="房态修改记录" width="800px">
      <div class="logs-content">
        <el-table :data="statusLogs" stripe>
          <el-table-column prop="roomNumber" label="房间号" width="100" />
          <el-table-column prop="date" label="日期" width="120" />
          <el-table-column prop="oldStatus" label="原状态" width="100">
            <template #default="{ row }">
              <span class="status-tag">{{ getStatusText(row.oldStatus) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="newStatus" label="新状态" width="100">
            <template #default="{ row }">
              <span class="status-tag">{{ getStatusText(row.newStatus) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" />
          <el-table-column prop="operatorName" label="操作人" width="100" />
          <el-table-column prop="operatedAt" label="操作时间" width="180">
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
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Box } from '@element-plus/icons-vue'
import { useRoomStatusStore } from '@/stores/roomStatus'
import type { BaseRoomType, Channel, RoomStatus, RoomStatusLog } from '@/types/room'

const roomStatusStore = useRoomStatusStore()

// 模拟数据接口
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

// 响应式数据
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

// 静态房型数据
const mockRoomTypes = [
  { id: 1, name: '大床房', code: 'DBF', totalRooms: 10, description: '舒适大床房' },
  { id: 2, name: '标准间', code: 'BZJ', totalRooms: 15, description: '标准双人间' },
  { id: 3, name: '套房', code: 'TF', totalRooms: 5, description: '豪华套房' },
]

// 静态渠道数据
const mockChannelData: LocalRoomTypeMapping[] = [
  {
    id: 1,
    localRoomType: {
      id: 1,
      name: '大床房',
      code: 'DBF',
      totalRooms: 10,
      description: '舒适大床房',
    },
    availableRooms: 8,
    channelMappings: [
      {
        channelId: 1,
        channelName: '携程',
        channelRoomTypeName: '豪华大床房',
        availableCount: 5,
        totalCount: 8,
        price: 288,
      },
      {
        channelId: 2,
        channelName: '美团',
        channelRoomTypeName: '舒适大床间',
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
      name: '标准间',
      code: 'BZJ',
      totalRooms: 15,
      description: '标准双人间',
    },
    availableRooms: 12,
    channelMappings: [
      {
        channelId: 1,
        channelName: '携程',
        channelRoomTypeName: '经济标准间',
        availableCount: 8,
        totalCount: 12,
        price: 198,
      },
      {
        channelId: 3,
        channelName: '飞猪',
        channelRoomTypeName: '标准双人房',
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
      name: '套房',
      code: 'TF',
      totalRooms: 5,
      description: '豪华套房',
    },
    availableRooms: 4,
    channelMappings: [
      {
        channelId: 1,
        channelName: '携程',
        channelRoomTypeName: '豪华套房',
        availableCount: 2,
        totalCount: 4,
        price: 588,
      },
    ],
  },
]

// 静态修改记录数据
const mockStatusLogs = [
  {
    id: 1,
    roomId: 1,
    roomNumber: 'a01',
    date: '2025-09-23',
    oldStatus: 'AVAILABLE',
    newStatus: 'MAINTENANCE',
    reason: '清洁维护',
    operatorName: '管理员',
    operatedAt: '2025-09-23T10:30:00',
  },
  {
    id: 2,
    roomId: 2,
    roomNumber: 'a02',
    date: '2025-09-23',
    oldStatus: 'MAINTENANCE',
    newStatus: 'AVAILABLE',
    reason: '维护完成',
    operatorName: '管理员',
    operatedAt: '2025-09-23T14:15:00',
  },
]

const channelData = ref<LocalRoomTypeMapping[]>(mockChannelData)

// 计算属性
const loading = ref(false)
const roomTypes = ref(mockRoomTypes)
const statusLogs = ref(mockStatusLogs)

// 方法
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
    // 这里应该调用API更新渠道映射
    // await updateChannelMapping(editingChannelMapping.value.channelId, editForm.value)

    // 更新本地数据
    if (editingChannelMapping.value) {
      Object.assign(editingChannelMapping.value, editForm.value)
    }

    ElMessage.success('渠道映射更新成功')
    showEditDialog.value = false
  } catch (error) {
    ElMessage.error('渠道映射更新失败')
  }
}

const getStatusText = (status: RoomStatus) => {
  const statusMap = {
    AVAILABLE: '可用',
    OCCUPIED: '已入住',
    RESERVED: '已预订',
    MAINTENANCE: '维修',
    OUT_OF_ORDER: '停用',
  }
  return statusMap[status] || '未知'
}

const formatDateTime = (dateTime: string) => {
  return new Date(dateTime).toLocaleString('zh-CN')
}

const loadChannelData = async () => {
  // 静态数据，根据选中房型筛选
  if (selectedRoomType.value) {
    channelData.value = mockChannelData.filter(
      (item) => item.localRoomType.id.toString() === selectedRoomType.value,
    )
  } else {
    channelData.value = mockChannelData
  }
  console.log('使用静态渠道数据，无需从服务器加载')
}

const loadStatusLogs = async () => {
  // 静态数据，直接显示弹窗
  showLogsDialog.value = true
  console.log('使用静态修改记录数据')
}

// 生命周期
onMounted(async () => {
  // 静态数据，无需从服务器获取
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
