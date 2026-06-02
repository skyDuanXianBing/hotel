<template>
  <div class="room-status-daily">
    <div class="main-content">
      <div class="header-toolbar">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('pages.roomStatusDaily.searchPlaceholder')"
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
        <div class="toolbar-actions">
          <el-button>{{ t('pages.roomStatusDaily.batchSettle') }}</el-button>
          <el-button>{{ t('pages.roomStatusDaily.batchOpenClose') }}</el-button>
        </div>
      </div>

      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-label">{{ t('pages.roomStatusDaily.stats.checkIn') }}</div>
          <div class="stat-value">{{ todayStats.checkIn }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">{{ t('pages.roomStatusDaily.stats.checkOut') }}</div>
          <div class="stat-value">{{ todayStats.checkOut }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">{{ t('pages.roomStatusDaily.stats.occupied') }}</div>
          <div class="stat-value">{{ todayStats.occupied }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">{{ t('pages.roomStatusDaily.stats.unassigned') }}</div>
          <div class="stat-value">{{ todayStats.unassigned }}</div>
        </div>
      </div>

      <div class="room-types-list">
        <div v-for="roomType in filteredRoomTypes" :key="roomType.id" class="room-type-section">
          <div class="room-type-header">
            <h3 class="room-type-title">
              {{ roomType.name }}
              ({{ t('pages.roomStatusDaily.roomCount', { count: roomType.totalRooms }) }})
            </h3>
          </div>

          <div class="rooms-grid">
            <div
              v-for="room in roomType.rooms"
              :key="room.id"
              class="room-card"
              :class="getRoomStatusClass(room)"
              @click="handleRoomClick(room)"
            >
              <div class="room-header">
                <span class="room-number">{{ room.roomNumber }}</span>
                <span v-if="room.quantity" class="room-quantity">{{ room.quantity }}</span>
                <el-icon v-if="room.hasIcon" class="room-icon">
                  <House />
                </el-icon>
              </div>

              <div class="room-type-name">{{ roomType.name }}</div>

              <div v-if="room.status === 'occupied'" class="guest-info">
                <div class="status-label">{{ room.statusLabel }}</div>
                <div class="guest-name">{{ room.guestName }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="sidebar-panel">
      <div class="panel-section">
        <h4 class="panel-title">{{ t('pages.roomStatusDaily.sidebar.dateSelection') }}</h4>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          :placeholder="t('pages.roomStatusDaily.sidebar.datePlaceholder')"
          format="YYYY/MM/DD"
          value-format="YYYY-MM-DD"
          style="width: 100%"
          @change="handleDateChange"
        />
      </div>

      <div class="panel-section">
        <div class="panel-header">
          <h4 class="panel-title">{{ t('pages.roomStatusDaily.sidebar.roomGroup') }}</h4>
          <el-button link @click="toggleRoomGroupCollapse">
            {{
              roomGroupCollapsed
                ? t('pages.roomStatusDaily.sidebar.expand')
                : t('pages.roomStatusDaily.sidebar.collapse')
            }}
          </el-button>
        </div>
        <div v-show="!roomGroupCollapsed" class="panel-content">
          <div class="group-stats">
            <div class="stat-row">
              <span>{{ t('pages.roomStatusDaily.sidebar.availableTotal') }}</span>
              <span class="stat-number">{{ totalAvailable }}/{{ totalRooms }}</span>
            </div>
          </div>
          <div class="group-item">
            <span>{{ t('pages.roomStatusDaily.sidebar.ungroupedRooms') }}</span>
            <span class="group-count">{{ unassignedCount }}/{{ totalRooms }}</span>
          </div>
        </div>
      </div>

      <div class="panel-section">
        <div class="panel-header">
          <h4 class="panel-title">{{ t('pages.roomStatusDaily.sidebar.roomTypeFilter') }}</h4>
          <el-button link @click="toggleRoomTypeCollapse">
            {{
              roomTypeCollapsed
                ? t('pages.roomStatusDaily.sidebar.expand')
                : t('pages.roomStatusDaily.sidebar.collapse')
            }}
          </el-button>
        </div>
        <div v-show="!roomTypeCollapsed" class="panel-content">
          <div class="filter-stats">
            <div class="stat-row">
              <span>{{ t('pages.roomStatusDaily.sidebar.availableTotal') }}</span>
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
            <span class="filter-count"
              >{{ roomType.availableRooms }}/{{ roomType.totalRooms }}</span
            >
          </div>

          <div class="add-section">
            <el-button class="add-button" @click="handleAddRoomType">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <div class="panel-section">
        <el-button style="width: 100%" @click="resetFilters">
          {{ t('pages.roomStatusDaily.sidebar.resetFilters') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { House, Plus, Search } from '@element-plus/icons-vue'
import { getStoreTodayYmd } from '@/utils/storeDateTime'

const { t } = useI18n()

const searchKeyword = ref('')
const selectedDate = ref(getStoreTodayYmd())
const selectedRoomTypeId = ref<number | null>(null)
const roomGroupCollapsed = ref(false)
const roomTypeCollapsed = ref(false)

const todayStats = ref({
  checkIn: 1,
  checkOut: 0,
  occupied: 0,
  unassigned: 0,
})

const roomTypes = ref([
  {
    id: 1,
    name: t('pages.roomStatusDaily.roomTypes.deluxeDouble'),
    totalRooms: 2,
    availableRooms: 1,
    rooms: [
      {
        id: 11,
        roomNumber: '11',
        quantity: 1,
        hasIcon: true,
        status: 'occupied',
        statusLabel: t('pages.roomStatusDaily.guest.reserved'),
        guestName: t('pages.roomStatusDaily.guest.directGuest'),
      },
      {
        id: 13,
        roomNumber: '13',
        quantity: 1,
        hasIcon: false,
        status: 'available',
        statusLabel: '',
        guestName: '',
      },
    ],
  },
  {
    id: 2,
    name: t('pages.roomStatusDaily.roomTypes.double'),
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
        guestName: '',
      },
    ],
  },
])

const totalRooms = computed(() => roomTypes.value.reduce((sum, rt) => sum + rt.totalRooms, 0))
const totalAvailable = computed(() =>
  roomTypes.value.reduce((sum, rt) => sum + rt.availableRooms, 0),
)
const unassignedCount = computed(() => roomTypes.value.reduce((sum, rt) => sum + rt.totalRooms, 0))

const filteredRoomTypes = computed(() => {
  if (selectedRoomTypeId.value) {
    return roomTypes.value.filter((rt) => rt.id === selectedRoomTypeId.value)
  }
  return roomTypes.value
})

const getRoomStatusClass = (room: { status: string }) => {
  if (room.status === 'occupied') return 'status-occupied'
  return 'status-available'
}

const handleRoomClick = (room: unknown) => {
  console.log('room clicked:', room)
}

const handleDateChange = (value: string) => {
  console.log('date changed:', value)
}

const toggleRoomGroupCollapse = () => {
  roomGroupCollapsed.value = !roomGroupCollapsed.value
}

const toggleRoomTypeCollapse = () => {
  roomTypeCollapsed.value = !roomTypeCollapsed.value
}

const selectRoomType = (id: number) => {
  selectedRoomTypeId.value = selectedRoomTypeId.value === id ? null : id
}

const handleAddRoomType = () => {
  console.log('add room type')
}

const resetFilters = () => {
  searchKeyword.value = ''
  selectedRoomTypeId.value = null
}

onMounted(() => {
  console.log('room status daily mounted')
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
