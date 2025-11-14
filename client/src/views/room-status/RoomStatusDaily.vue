<template>
  <div class="room-status-daily">
    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 顶部工具栏 -->
      <div class="header-toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="姓名、邮箱、手机号、订单号、备注"
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
        <div class="toolbar-actions">
          <el-button>批量算脏/净</el-button>
          <el-button>批量开/关房</el-button>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-label">今日预抵</div>
          <div class="stat-value">{{ todayStats.checkIn }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">今日预离</div>
          <div class="stat-value">{{ todayStats.checkOut }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">今日在住</div>
          <div class="stat-value">{{ todayStats.occupied }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">未排房</div>
          <div class="stat-value">{{ todayStats.unassigned }}</div>
        </div>
      </div>

      <!-- 房型分组列表 -->
      <div class="room-types-list">
        <div
          v-for="roomType in filteredRoomTypes"
          :key="roomType.id"
          class="room-type-section"
        >
          <!-- 房型标题 -->
          <div class="room-type-header">
            <h3 class="room-type-title">{{ roomType.name }} (共{{ roomType.totalRooms }}间)</h3>
          </div>

          <!-- 房间卡片列表 -->
          <div class="rooms-grid">
            <div
              v-for="room in roomType.rooms"
              :key="room.id"
              class="room-card"
              :class="getRoomStatusClass(room)"
              @click="handleRoomClick(room)"
            >
              <!-- 房间号和数量 -->
              <div class="room-header">
                <span class="room-number">{{ room.roomNumber }}</span>
                <span v-if="room.quantity" class="room-quantity">{{ room.quantity }}</span>
                <el-icon v-if="room.hasIcon" class="room-icon">
                  <House />
                </el-icon>
              </div>

              <!-- 房型名称 -->
              <div class="room-type-name">{{ roomType.name }}</div>

              <!-- 客人信息 -->
              <div v-if="room.status === 'occupied'" class="guest-info">
                <div class="status-label">{{ room.statusLabel }}</div>
                <div class="guest-name">{{ room.guestName }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧筛选面板 -->
    <div class="sidebar-panel">
      <!-- 日期选择 -->
      <div class="panel-section">
        <h4 class="panel-title">日期选择</h4>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY/MM/DD"
          value-format="YYYY-MM-DD"
          style="width: 100%"
          @change="handleDateChange"
        />
      </div>

      <!-- 房间分组 -->
      <div class="panel-section">
        <div class="panel-header">
          <h4 class="panel-title">房间分组</h4>
          <el-button link @click="toggleRoomGroupCollapse">
            {{ roomGroupCollapsed ? '展开' : '收起' }}
          </el-button>
        </div>
        <div v-show="!roomGroupCollapsed" class="panel-content">
          <div class="group-stats">
            <div class="stat-row">
              <span>可售数/总房数:</span>
              <span class="stat-number">{{ totalAvailable }}/{{ totalRooms }}</span>
            </div>
          </div>
          <div class="group-item">
            <span>未分组房间</span>
            <span class="group-count">{{ unassignedCount }}/{{ totalRooms }}</span>
          </div>
        </div>
      </div>

      <!-- 房型筛选 -->
      <div class="panel-section">
        <div class="panel-header">
          <h4 class="panel-title">房型筛选</h4>
          <el-button link @click="toggleRoomTypeCollapse">
            {{ roomTypeCollapsed ? '展开' : '收起' }}
          </el-button>
        </div>
        <div v-show="!roomTypeCollapsed" class="panel-content">
          <div class="filter-stats">
            <div class="stat-row">
              <span>可售数/总房数:</span>
              <span class="stat-number">{{ totalAvailable }}/{{ totalRooms }}</span>
            </div>
          </div>
          <div
            v-for="roomType in roomTypes"
            :key="roomType.id"
            class="filter-item"
            :class="{ active: selectedRoomTypeId === roomType.id }"
            @click="selectRoomType(roomType.id)"
          >
            <span>{{ roomType.name }}</span>
            <span class="filter-count">{{ roomType.availableRooms }}/{{ roomType.totalRooms }}</span>
          </div>

          <!-- 添加按钮 -->
          <div class="add-section">
            <el-button class="add-button" @click="handleAddRoomType">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 重置筛选 -->
      <div class="panel-section">
        <el-button style="width: 100%" @click="resetFilters">重置筛选</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search, Plus, House } from '@element-plus/icons-vue'

// 响应式数据
const searchKeyword = ref('')
const selectedDate = ref(new Date().toISOString().split('T')[0])
const selectedRoomTypeId = ref<number | null>(null)
const roomGroupCollapsed = ref(false)
const roomTypeCollapsed = ref(false)

// 统计数据
const todayStats = ref({
  checkIn: 1,
  checkOut: 0,
  occupied: 0,
  unassigned: 0
})

// 房型数据
const roomTypes = ref([
  {
    id: 1,
    name: '大床房',
    totalRooms: 2,
    availableRooms: 1,
    rooms: [
      {
        id: 11,
        roomNumber: '11',
        quantity: 1,
        hasIcon: true,
        status: 'occupied',
        statusLabel: '里',
        guestName: '自来客'
      },
      {
        id: 13,
        roomNumber: '13',
        quantity: 1,
        hasIcon: false,
        status: 'available',
        statusLabel: '',
        guestName: ''
      }
    ]
  },
  {
    id: 2,
    name: '双人',
    totalRooms: 1,
    availableRooms: 1,
    rooms: [
      {
        id: 111,
        roomNumber: '111',
        quantity: 1,
        hasIcon: false,
        status: 'available',
        statusLabel: '',
        guestName: ''
      }
    ]
  }
])

// 计算属性
const totalRooms = computed(() => {
  return roomTypes.value.reduce((sum, rt) => sum + rt.totalRooms, 0)
})

const totalAvailable = computed(() => {
  return roomTypes.value.reduce((sum, rt) => sum + rt.availableRooms, 0)
})

const unassignedCount = computed(() => {
  return roomTypes.value.reduce((sum, rt) => sum + rt.totalRooms, 0)
})

const filteredRoomTypes = computed(() => {
  if (selectedRoomTypeId.value) {
    return roomTypes.value.filter(rt => rt.id === selectedRoomTypeId.value)
  }
  return roomTypes.value
})

// 方法
const getRoomStatusClass = (room: any) => {
  if (room.status === 'occupied') return 'status-occupied'
  return 'status-available'
}

const handleRoomClick = (room: any) => {
  console.log('点击房间:', room)
  // 这里可以打开房间详情弹窗
}

const handleDateChange = (value: string) => {
  console.log('日期改变:', value)
  // 重新加载当天的数据
}

const toggleRoomGroupCollapse = () => {
  roomGroupCollapsed.value = !roomGroupCollapsed.value
}

const toggleRoomTypeCollapse = () => {
  roomTypeCollapsed.value = !roomTypeCollapsed.value
}

const selectRoomType = (id: number) => {
  if (selectedRoomTypeId.value === id) {
    selectedRoomTypeId.value = null
  } else {
    selectedRoomTypeId.value = id
  }
}

const handleAddRoomType = () => {
  console.log('添加房型')
}

const resetFilters = () => {
  searchKeyword.value = ''
  selectedRoomTypeId.value = null
}

// 生命周期
onMounted(() => {
  // 初始化数据
  console.log('单日视图已加载')
})
</script>

<style scoped>
.room-status-daily {
  display: flex;
  min-height: 100vh;
  background: #f5f5f5;
}

.main-content {
  flex: 1;
  padding: 20px;
  margin-right: 340px;
}

.header-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.search-input {
  flex: 1;
  max-width: 600px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  color: #409eff;
}

.room-types-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.room-type-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.room-type-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.room-type-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.rooms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.room-card {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
  background: white;
}

.room-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.room-card.status-occupied {
  background: #fff7e6;
  border-color: #ffd591;
}

.room-card.status-available {
  background: #f0f0f0;
  border-color: #d9d9d9;
}

.room-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.room-number {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.room-quantity {
  font-size: 14px;
  color: #666;
}

.room-icon {
  margin-left: auto;
  color: #fa8c16;
}

.room-type-name {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.guest-info {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e8e8e8;
}

.status-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.guest-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.sidebar-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 320px;
  height: 100vh;
  background: white;
  border-left: 1px solid #e8e8e8;
  padding: 20px;
  overflow-y: auto;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.05);
}

.panel-section {
  margin-bottom: 24px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.panel-title {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.panel-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.group-stats,
.filter-stats {
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 12px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #666;
}

.stat-number {
  font-weight: 600;
  color: #333;
}

.group-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 13px;
}

.group-count {
  color: #666;
}

.filter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-item:hover {
  background: #e6f7ff;
}

.filter-item.active {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
}

.filter-count {
  color: #666;
  font-size: 12px;
}

.add-section {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.add-button {
  width: 100%;
  border: 1px dashed #d9d9d9;
  color: #666;
}

.add-button:hover {
  border-color: #409eff;
  color: #409eff;
}
</style>
