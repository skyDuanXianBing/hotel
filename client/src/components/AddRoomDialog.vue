<template>
  <el-dialog
    v-model="dialogVisible"
    :title="t('stage6.components.addRoomDialog.title')"
    width="600px"
    @close="handleClose"
  >
    <div class="add-room-dialog">
      <!-- Selection summary -->
      <div class="room-stats">
        <span class="stats-text">
          {{ t('stage6.components.addRoomDialog.selectedOfTotal', { selected: selectedRooms.length, total: totalRooms }) }}
        </span>
      </div>

      <!-- Room selection area -->
      <div class="room-selection-area">
        <!-- Left: room type tree -->
        <div class="room-type-tree">
          <div class="tree-header">
            <el-checkbox 
              v-model="selectAll"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll"
            >
              {{ t('stage6.components.addRoomDialog.allRooms') }}
            </el-checkbox>
            <el-button 
              type="primary" 
              link 
              size="small" 
              @click="selectAllRooms"
              class="select-all-btn"
            >
              {{ t('stage6.common.actions.selectAll') }}
            </el-button>
            <el-button 
              type="primary" 
              link 
              size="small" 
              @click="clearAllSelection"
              class="clear-all-btn"
            >
              {{ t('stage6.common.actions.clear') }}
            </el-button>
          </div>
          
          <div class="tree-content">
            <div 
              v-for="roomType in roomTypesWithRooms" 
              :key="roomType.id"
              class="room-type-item"
            >
              <!-- Room type title -->
              <div class="room-type-header" @click="toggleRoomType(roomType.id)">
                <el-icon 
                  :class="expandedRoomTypes.includes(roomType.id) ? 'expanded' : 'collapsed'"
                  class="expand-icon"
                >
                  <ArrowRight />
                </el-icon>
                <el-checkbox 
                  :model-value="isRoomTypeSelected(roomType.id)"
                  :indeterminate="isRoomTypeIndeterminate(roomType.id)"
                  @change="(checked: boolean) => handleRoomTypeSelect(roomType.id, checked)"
                  @click.stop
                >
                  {{ roomType.name }}
                </el-checkbox>
              </div>

              <!-- Room list -->
              <div 
                v-if="expandedRoomTypes.includes(roomType.id)" 
                class="room-list"
              >
                <div 
                  v-for="room in roomType.rooms" 
                  :key="room.id"
                  class="room-item"
                >
                  <el-checkbox 
                    :model-value="selectedRooms.includes(room.id)"
                    @change="(checked: boolean) => handleRoomSelect(room.id, checked)"
                  >
                    {{ room.roomNumber }}
                  </el-checkbox>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: selected room list -->
        <div class="selected-rooms-panel">
          <div class="panel-header">
            {{ t('stage6.components.addRoomDialog.selectedRooms', { count: selectedRooms.length }) }}
          </div>
          <div class="selected-rooms-list">
            <div 
              v-for="roomId in selectedRooms"
              :key="roomId"
              class="selected-room-item"
            >
              <span>{{ getRoomDisplayName(roomId) }}</span>
              <el-button 
                type="text" 
                size="small" 
                @click="handleRoomSelect(roomId, false)"
                class="remove-btn"
              >
                {{ t('stage6.common.actions.remove') }}
              </el-button>
            </div>
            <div v-if="selectedRooms.length === 0" class="empty-state">
              {{ t('stage6.components.addRoomDialog.emptySelected') }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">{{ t('stage6.common.actions.cancel') }}</el-button>
        <el-button type="primary" @click="handleConfirm">{{ t('stage6.common.actions.confirm') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { request } from '@/utils/request'
import type { ApiResponse } from '@/types/room'

// Interface definitions
interface Room {
  id: number
  roomNumber: string
  roomType: {
    id: number
    name: string
  }
}

interface RoomType {
  id: number
  name: string
  rooms: Room[]
}

// Component props
interface Props {
  modelValue: boolean
  selectedRoomIds?: number[]
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', roomIds: number[]): void
}

const props = withDefaults(defineProps<Props>(), {
  selectedRoomIds: () => []
})

const emit = defineEmits<Emits>()
const { t } = useI18n()

// Reactive state
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const loading = ref(false)
const allRooms = ref<Room[]>([])
const selectedRooms = ref<number[]>([...props.selectedRoomIds])
const expandedRoomTypes = ref<number[]>([])

// Computed state
const roomTypesWithRooms = computed<RoomType[]>(() => {
  const roomTypeMap = new Map<number, RoomType>()
  
  allRooms.value.forEach(room => {
    const roomTypeId = room.roomType.id
    if (!roomTypeMap.has(roomTypeId)) {
      roomTypeMap.set(roomTypeId, {
        id: roomTypeId,
        name: room.roomType.name,
        rooms: []
      })
    }
    roomTypeMap.get(roomTypeId)!.rooms.push(room)
  })
  
  return Array.from(roomTypeMap.values())
})

const totalRooms = computed(() => allRooms.value.length)

const selectAll = computed({
  get: () => selectedRooms.value.length === totalRooms.value && totalRooms.value > 0,
  set: (value) => {
    // Handled by handleSelectAll
  }
})

const isIndeterminate = computed(() => {
  return selectedRooms.value.length > 0 && selectedRooms.value.length < totalRooms.value
})

// Load room data
const fetchRooms = async () => {
  try {
    loading.value = true
    
    const response: ApiResponse<Room[]> = await request.get('/rooms')
    
    if (response.success) {
      allRooms.value = response.data
    } else {
      ElMessage.error(response.message || t('stage6.components.addRoomDialog.loadFailed'))
      // Use demo data when the backend request fails
      allRooms.value = [
        {
          id: 1,
          roomNumber: 'a01',
          roomType: { id: 1, name: t('stage6.components.addRoomDialog.demoRoomTypes.queen') }
        },
        {
          id: 2,
          roomNumber: 'd01',
          roomType: { id: 2, name: t('stage6.components.addRoomDialog.demoRoomTypes.twin') }
        }
      ]
    }
    
    // Collapse all room types by default
    expandedRoomTypes.value = []
  } catch (error) {
    console.error('Failed to load room data:', error)
    // Use demo data on network errors
    allRooms.value = [
      {
        id: 1,
        roomNumber: 'a01',
        roomType: { id: 1, name: t('stage6.components.addRoomDialog.demoRoomTypes.queen') }
      },
      {
        id: 2,
        roomNumber: 'd01',
        roomType: { id: 2, name: t('stage6.components.addRoomDialog.demoRoomTypes.twin') }
      }
    ]
    expandedRoomTypes.value = []
  } finally {
    loading.value = false
  }
}

// Handle select all / clear all
const handleSelectAll = (checked: boolean) => {
  if (checked) {
    selectedRooms.value = allRooms.value.map(room => room.id)
  } else {
    selectedRooms.value = []
  }
}

// Select all rooms
const selectAllRooms = () => {
  selectedRooms.value = allRooms.value.map(room => room.id)
}

// Clear all selections
const clearAllSelection = () => {
  selectedRooms.value = []
}

// Handle room type selection
const handleRoomTypeSelect = (roomTypeId: number, checked: boolean) => {
  const roomType = roomTypesWithRooms.value.find(rt => rt.id === roomTypeId)
  if (!roomType) return
  
  const roomIds = roomType.rooms.map(room => room.id)
  
  if (checked) {
    // Add all rooms under this room type
    roomIds.forEach(roomId => {
      if (!selectedRooms.value.includes(roomId)) {
        selectedRooms.value.push(roomId)
      }
    })
  } else {
    // Remove all rooms under this room type
    selectedRooms.value = selectedRooms.value.filter(roomId => !roomIds.includes(roomId))
  }
}

// Handle single room selection
const handleRoomSelect = (roomId: number, checked: boolean) => {
  if (checked) {
    if (!selectedRooms.value.includes(roomId)) {
      selectedRooms.value.push(roomId)
    }
  } else {
    selectedRooms.value = selectedRooms.value.filter(id => id !== roomId)
  }
}

// Check whether a room type is fully selected
const isRoomTypeSelected = (roomTypeId: number): boolean => {
  const roomType = roomTypesWithRooms.value.find(rt => rt.id === roomTypeId)
  if (!roomType || roomType.rooms.length === 0) return false
  
  const roomIds = roomType.rooms.map(room => room.id)
  return roomIds.every(roomId => selectedRooms.value.includes(roomId))
}

// Check whether a room type is partially selected
const isRoomTypeIndeterminate = (roomTypeId: number): boolean => {
  const roomType = roomTypesWithRooms.value.find(rt => rt.id === roomTypeId)
  if (!roomType) return false
  
  const roomIds = roomType.rooms.map(room => room.id)
  const selectedCount = roomIds.filter(roomId => selectedRooms.value.includes(roomId)).length
  
  return selectedCount > 0 && selectedCount < roomIds.length
}

// Toggle room type expansion
const toggleRoomType = (roomTypeId: number) => {
  const index = expandedRoomTypes.value.indexOf(roomTypeId)
  if (index > -1) {
    expandedRoomTypes.value.splice(index, 1)
  } else {
    expandedRoomTypes.value.push(roomTypeId)
  }
}

// Get room display name
const getRoomDisplayName = (roomId: number): string => {
  const room = allRooms.value.find(r => r.id === roomId)
  return room
    ? `${room.roomType.name} - ${room.roomNumber}`
    : t('stage6.components.addRoomDialog.fallbackRoom', { id: roomId })
}

// Handle close
const handleClose = () => {
  dialogVisible.value = false
}

// Handle confirm
const handleConfirm = () => {
  emit('confirm', selectedRooms.value)
  dialogVisible.value = false
}

// Watch dialog open state
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    selectedRooms.value = [...props.selectedRoomIds]
    fetchRooms()
  }
})

// Watch externally provided selected rooms
watch(() => props.selectedRoomIds, (newValue) => {
  selectedRooms.value = [...newValue]
})

onMounted(() => {
  if (props.modelValue) {
    fetchRooms()
  }
})
</script>

<style scoped>
.add-room-dialog {
  padding: 0;
}

.room-stats {
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 20px;
  text-align: center;
}

.stats-text {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.room-selection-area {
  display: flex;
  gap: 20px;
  height: 400px;
}

.room-type-tree {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.tree-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.select-all-btn,
.clear-all-btn {
  margin-left: 8px;
  font-size: 12px;
}

.tree-content {
  height: calc(100% - 49px);
  overflow-y: auto;
  padding: 8px 0;
}

.room-type-item {
  margin-bottom: 8px;
}

.room-type-header {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.room-type-header:hover {
  background: #f5f7fa;
}

.expand-icon {
  margin-right: 8px;
  font-size: 12px;
  color: #909399;
  transition: transform 0.2s;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.room-list {
  padding-left: 32px;
  background: #fafafa;
}

.room-item {
  padding: 6px 16px;
}

.selected-rooms-panel {
  width: 200px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.panel-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.selected-rooms-list {
  height: calc(100% - 49px);
  overflow-y: auto;
  padding: 8px 0;
}

.selected-room-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  font-size: 13px;
}

.selected-room-item:hover {
  background: #f5f7fa;
}

.remove-btn {
  color: #f56c6c;
  padding: 0;
  font-size: 12px;
}

.remove-btn:hover {
  color: #f78989;
}

.empty-state {
  padding: 20px 16px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Scrollbar styles */
.tree-content::-webkit-scrollbar,
.selected-rooms-list::-webkit-scrollbar {
  width: 6px;
}

.tree-content::-webkit-scrollbar-track,
.selected-rooms-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.tree-content::-webkit-scrollbar-thumb,
.selected-rooms-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.tree-content::-webkit-scrollbar-thumb:hover,
.selected-rooms-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
