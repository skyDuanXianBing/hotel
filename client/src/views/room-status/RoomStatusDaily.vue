<template>
  <div class="room-status-daily">
    <!-- 头部工具栏 -->
    <div class="header-toolbar">
      <div class="date-selection">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
        />
      </div>

      <div class="toolbar-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="姓名、手机号、订单号、渠道订单号、房间号、客户"
          :prefix-icon="Search"
          clearable
          style="width: 400px; margin-right: 10px"
        />
        <el-button type="primary">批量查看/净</el-button>
        <el-button type="primary">批量开/关</el-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="daily-content" v-loading="loading">
      <!-- 房型统计卡片 -->
      <div class="room-type-cards">
        <div
          v-for="roomType in dailyData?.roomTypes || []"
          :key="roomType.id"
          class="room-type-card"
        >
          <!-- 房型标题 -->
          <div class="room-type-header">
            <h3 class="room-type-name">{{ roomType.name }} (共{{ roomType.totalRooms }}间)</h3>
            <div class="room-type-stats">
              <span class="stat-item available"
                >可售数/总房数: {{ roomType.availableRooms }}/{{ roomType.totalRooms }}</span
              >
              <span class="stat-item occupied">已入住: {{ roomType.occupiedRooms }}</span>
              <span class="stat-item maintenance">维修: {{ roomType.maintenanceRooms }}</span>
            </div>
          </div>

          <!-- 房间列表 -->
          <div class="rooms-list">
            <div
              v-for="room in roomType.rooms"
              :key="room.id"
              class="room-item"
              :class="`status-${room.status.toLowerCase()}`"
              @click="onRoomClick(room, roomType)"
            >
              <div class="room-info">
                <div class="room-number">{{ room.roomNumber }}</div>
                <div class="room-type-name">{{ roomType.name }}</div>
              </div>

              <div v-if="room.status === RoomStatus.OCCUPIED && room.guestName" class="guest-info">
                <div class="guest-name">{{ room.guestName }}</div>
                <div class="channel-name">{{ room.channel }}</div>
              </div>

              <div class="room-status">
                <span class="status-badge" :class="`badge-${room.status.toLowerCase()}`">
                  {{ getStatusText(room.status) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧信息面板 -->
    <div class="info-panel">
      <!-- 日期选择 -->
      <div class="panel-section">
        <h4>日期选择</h4>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
          style="width: 100%"
        />
      </div>

      <!-- 房间分组 -->
      <div class="panel-section">
        <h4>房间分组 <el-button text @click="toggleGroupCollapse">收起</el-button></h4>

        <div v-if="!groupCollapsed" class="group-stats">
          <div v-for="roomType in dailyData?.roomTypes || []" :key="roomType.id" class="group-item">
            <div class="group-name">{{ roomType.name }}</div>
            <div class="group-count">{{ roomType.availableRooms }}/{{ roomType.totalRooms }}</div>
          </div>

          <div class="group-item">
            <div class="group-name">未分组房间</div>
            <div class="group-count">{{ unassignedRoomsCount }}/{{ unassignedRoomsCount }}</div>
          </div>
        </div>
      </div>

      <!-- 房型筛选 -->
      <div class="panel-section">
        <h4>房型筛选 <el-button text @click="toggleFilterCollapse">收起</el-button></h4>

        <div v-if="!filterCollapsed" class="filter-options">
          <div
            v-for="roomType in dailyData?.roomTypes || []"
            :key="roomType.id"
            class="filter-item"
          >
            <div class="filter-name">{{ roomType.name }}</div>
            <div class="filter-count">{{ roomType.availableRooms }}/{{ roomType.totalRooms }}</div>
          </div>

          <div class="filter-item">
            <div class="filter-name">大厅</div>
            <div class="add-button">
              <el-icon><Plus /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 重置筛选 -->
      <div class="panel-section">
        <el-button @click="resetFilters" style="width: 100%">重置筛选</el-button>
      </div>
    </div>

    <!-- 房间操作弹窗 -->
    <el-dialog v-model="showRoomDialog" title="房间信息" width="500px">
      <div class="room-dialog-content">
        <div class="room-info">
          <p>
            <strong>房间：</strong>{{ selectedRoomInfo?.roomType }} -
            {{ selectedRoomInfo?.roomNumber }}
          </p>
          <p><strong>日期：</strong>{{ selectedDate }}</p>
          <p>
            <strong>当前状态：</strong>{{ getStatusText(selectedRoomInfo?.status || 'AVAILABLE') }}
          </p>
        </div>

        <el-form label-width="80px">
          <el-form-item label="新状态">
            <el-select v-model="newStatus" placeholder="选择新状态">
              <el-option label="可用" :value="RoomStatus.AVAILABLE" />
              <el-option label="已入住" :value="RoomStatus.OCCUPIED" />
              <el-option label="已预订" :value="RoomStatus.RESERVED" />
              <el-option label="维修" :value="RoomStatus.MAINTENANCE" />
              <el-option label="停用" :value="RoomStatus.OUT_OF_ORDER" />
            </el-select>
          </el-form-item>

          <el-form-item label="原因">
            <el-input
              v-model="statusReason"
              type="textarea"
              placeholder="请输入操作原因"
              :rows="3"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="showRoomDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmRoomStatusChange">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { useRoomStatusStore } from '@/stores/roomStatus'
import { RoomStatus } from '@/types/room'
import type { DailyRoomTypeData } from '@/types/room'

const roomStatusStore = useRoomStatusStore()

// 响应式数据
const searchKeyword = ref('')
const selectedDate = ref<string>(new Date().toISOString().split('T')[0])
const showRoomDialog = ref(false)
const selectedRoomInfo = ref<any>(null)
const selectedRoomType = ref<DailyRoomTypeData | null>(null)
const newStatus = ref<RoomStatus>(RoomStatus.AVAILABLE)
const statusReason = ref('')
const groupCollapsed = ref(false)
const filterCollapsed = ref(false)

// 静态数据
const mockDailyData = {
  date: '2025-09-23',
  roomTypes: [
    {
      id: 1,
      name: '大床房',
      totalRooms: 5,
      availableRooms: 3,
      occupiedRooms: 1,
      maintenanceRooms: 1,
      rooms: [
        {
          id: 1,
          roomNumber: 'a01',
          status: RoomStatus.OCCUPIED,
          guestName: '林',
          channel: '自来客',
        },
        {
          id: 2,
          roomNumber: 'a02',
          status: RoomStatus.AVAILABLE,
        },
        {
          id: 3,
          roomNumber: 'a03',
          status: RoomStatus.MAINTENANCE,
        },
        {
          id: 4,
          roomNumber: 'a04',
          status: RoomStatus.AVAILABLE,
        },
        {
          id: 5,
          roomNumber: 'a05',
          status: RoomStatus.AVAILABLE,
        },
      ],
    },
    {
      id: 2,
      name: '标准间',
      totalRooms: 8,
      availableRooms: 6,
      occupiedRooms: 2,
      maintenanceRooms: 0,
      rooms: [
        {
          id: 6,
          roomNumber: 'b01',
          status: RoomStatus.OCCUPIED,
          guestName: '张三',
          channel: '携程',
        },
        {
          id: 7,
          roomNumber: 'b02',
          status: RoomStatus.OCCUPIED,
          guestName: '李四',
          channel: '美团',
        },
        {
          id: 8,
          roomNumber: 'b03',
          status: RoomStatus.AVAILABLE,
        },
        {
          id: 9,
          roomNumber: 'b04',
          status: RoomStatus.AVAILABLE,
        },
        {
          id: 10,
          roomNumber: 'b05',
          status: RoomStatus.AVAILABLE,
        },
        {
          id: 11,
          roomNumber: 'b06',
          status: RoomStatus.AVAILABLE,
        },
        {
          id: 12,
          roomNumber: 'b07',
          status: RoomStatus.AVAILABLE,
        },
        {
          id: 13,
          roomNumber: 'b08',
          status: RoomStatus.AVAILABLE,
        },
      ],
    },
    {
      id: 3,
      name: '套房',
      totalRooms: 3,
      availableRooms: 2,
      occupiedRooms: 0,
      maintenanceRooms: 1,
      rooms: [
        {
          id: 14,
          roomNumber: 'c01',
          status: RoomStatus.OUT_OF_ORDER,
        },
        {
          id: 15,
          roomNumber: 'c02',
          status: RoomStatus.AVAILABLE,
        },
        {
          id: 16,
          roomNumber: 'c03',
          status: RoomStatus.AVAILABLE,
        },
      ],
    },
  ],
}

// 计算属性
const loading = ref(false)
const dailyData = ref(mockDailyData)

const unassignedRoomsCount = computed(() => {
  // 未分组房间数量的计算逻辑
  return 5
})

// 方法
const onDateChange = (value: string | null) => {
  if (value) {
    selectedDate.value = value
    loadDailyData()
  }
}

const onRoomClick = (room: any, roomType: DailyRoomTypeData) => {
  selectedRoomInfo.value = {
    ...room,
    roomType: roomType.name,
  }
  selectedRoomType.value = roomType
  newStatus.value = room.status
  statusReason.value = ''
  showRoomDialog.value = true
}

const confirmRoomStatusChange = async () => {
  if (!selectedRoomInfo.value) return

  // 模拟更新房间状态
  const roomType = dailyData.value.roomTypes.find((rt) =>
    rt.rooms.some((r) => r.id === selectedRoomInfo.value?.id),
  )

  if (roomType) {
    const room = roomType.rooms.find((r) => r.id === selectedRoomInfo.value?.id)
    if (room) {
      const oldStatus = room.status
      room.status = newStatus.value

      // 如果改为非预订状态，清除客人信息
      if (newStatus.value !== RoomStatus.OCCUPIED && newStatus.value !== RoomStatus.RESERVED) {
        room.guestName = undefined
        room.channel = undefined
      }

      // 更新统计数据
      updateRoomTypeStats(roomType, oldStatus, newStatus.value)
    }
  }

  ElMessage.success('房间状态更新成功')
  showRoomDialog.value = false
}

const updateRoomTypeStats = (roomType: any, oldStatus: RoomStatus, newStatus: RoomStatus) => {
  // 减少旧状态计数
  if (oldStatus === RoomStatus.AVAILABLE) roomType.availableRooms--
  else if (oldStatus === RoomStatus.OCCUPIED) roomType.occupiedRooms--
  else if (oldStatus === RoomStatus.MAINTENANCE || oldStatus === RoomStatus.OUT_OF_ORDER)
    roomType.maintenanceRooms--

  // 增加新状态计数
  if (newStatus === RoomStatus.AVAILABLE) roomType.availableRooms++
  else if (newStatus === RoomStatus.OCCUPIED) roomType.occupiedRooms++
  else if (newStatus === RoomStatus.MAINTENANCE || newStatus === RoomStatus.OUT_OF_ORDER)
    roomType.maintenanceRooms++
}

const getStatusText = (status: RoomStatus) => {
  const statusMap = {
    [RoomStatus.AVAILABLE]: '可用',
    [RoomStatus.OCCUPIED]: '已入住',
    [RoomStatus.RESERVED]: '已预订',
    [RoomStatus.MAINTENANCE]: '维修',
    [RoomStatus.OUT_OF_ORDER]: '停用',
  }
  return statusMap[status] || '未知'
}

const toggleGroupCollapse = () => {
  groupCollapsed.value = !groupCollapsed.value
}

const toggleFilterCollapse = () => {
  filterCollapsed.value = !filterCollapsed.value
}

const resetFilters = () => {
  searchKeyword.value = ''
  // 重置其他筛选条件
}

const loadDailyData = async () => {
  // 静态数据，无需加载
  console.log('使用静态数据，无需从服务器加载')
}

// 生命周期
onMounted(() => {
  loadDailyData()
})
</script>

<style scoped>
.room-status-daily {
  display: flex;
  min-height: 100vh;
  background: #f5f5f5;
}

.header-toolbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 300px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: white;
  border-bottom: 1px solid #e9ecef;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.date-selection {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
}

.daily-content {
  flex: 1;
  padding: 80px 20px 20px 20px;
  margin-right: 300px;
}

.room-type-cards {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.room-type-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.room-type-header {
  margin-bottom: 20px;
  border-bottom: 1px solid #e9ecef;
  padding-bottom: 15px;
}

.room-type-name {
  font-size: 18px;
  font-weight: bold;
  margin: 0 0 10px 0;
  color: #333;
}

.room-type-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  font-size: 14px;
  padding: 4px 8px;
  border-radius: 4px;
}

.stat-item.available {
  background: #f6ffed;
  color: #52c41a;
}

.stat-item.occupied {
  background: #fff2e8;
  color: #fa8c16;
}

.stat-item.maintenance {
  background: #fff1f0;
  color: #ff4d4f;
}

.rooms-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.room-item {
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.room-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.room-item.status-available {
  background: #f6ffed;
  border-color: #b7eb8f;
}

.room-item.status-occupied {
  background: #fff2e8;
  border-color: #ffbb96;
}

.room-item.status-reserved {
  background: #e6f7ff;
  border-color: #91d5ff;
}

.room-item.status-maintenance {
  background: #fff1f0;
  border-color: #ffa39e;
}

.room-item.status-out_of_order {
  background: #f6f6f6;
  border-color: #d9d9d9;
}

.room-info {
  margin-bottom: 10px;
}

.room-number {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.room-type-name {
  font-size: 12px;
  color: #666;
  margin: 0;
}

.guest-info {
  margin-bottom: 10px;
}

.guest-name {
  font-weight: bold;
  color: #333;
  margin-bottom: 2px;
}

.channel-name {
  font-size: 12px;
  color: #666;
}

.room-status {
  text-align: center;
}

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.badge-available {
  background: #52c41a;
  color: white;
}

.badge-occupied {
  background: #fa8c16;
  color: white;
}

.badge-reserved {
  background: #1890ff;
  color: white;
}

.badge-maintenance {
  background: #ff4d4f;
  color: white;
}

.badge-out_of_order {
  background: #666;
  color: white;
}

.info-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 300px;
  height: 100vh;
  background: white;
  border-left: 1px solid #e9ecef;
  padding: 20px;
  overflow-y: auto;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
}

.panel-section {
  margin-bottom: 30px;
}

.panel-section h4 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.group-stats,
.filter-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.group-item,
.filter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.group-name,
.filter-name {
  font-size: 14px;
  color: #333;
}

.group-count,
.filter-count {
  font-size: 12px;
  color: #666;
}

.add-button {
  color: #1890ff;
  cursor: pointer;
}

.room-dialog-content {
  padding: 10px 0;
}

.room-dialog-content .room-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 4px;
}

.room-dialog-content .room-info p {
  margin: 5px 0;
  color: #333;
}
</style>
