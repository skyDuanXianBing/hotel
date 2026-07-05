<template>
  <div class="room-sort-container">
    <!-- 顶部提示信息 -->
    <el-alert
      :closable="false"
      type="info"
      show-icon
    >
      <template #default>
        {{ t('settingsStage4.roomSort.notice', { name: currentTabText }) }}
      </template>
    </el-alert>

    <!-- 标签页 -->
    <div class="content-area">
      <el-tabs v-model="activeTab" class="sort-tabs" @tab-change="handleTabChange">
        <!-- 房型排序 -->
        <el-tab-pane :label="t('settingsStage4.roomSort.tabs.roomType')" name="roomType">
          <div class="sort-grid">
            <div
              v-for="(item, index) in roomTypes"
              :key="item.id"
              class="sort-item"
              draggable="true"
              @dragstart="handleDragStart(index, 'roomType')"
              @dragover.prevent
              @drop="handleDrop(index, 'roomType')"
            >
              <el-icon class="drag-icon"><Menu /></el-icon>
              <span class="item-name">{{ item.name }}</span>
            </div>
          </div>
          <div v-if="isLoading" class="loading-text">{{ t('settings.common.loading') }}</div>
          <div v-else-if="roomTypes.length === 0" class="empty-text">{{ t('settingsStage4.roomSort.empty.roomType') }}</div>
        </el-tab-pane>

        <!-- 房间排序 -->
        <el-tab-pane :label="t('settingsStage4.roomSort.tabs.room')" name="room">
          <div class="sort-grid">
            <div
              v-for="(item, index) in rooms"
              :key="item.id"
              class="sort-item"
              draggable="true"
              @dragstart="handleDragStart(index, 'room')"
              @dragover.prevent
              @drop="handleDrop(index, 'room')"
            >
              <el-icon class="drag-icon"><Menu /></el-icon>
              <div class="room-item-content">
                <span class="room-number">{{ item.number }}</span>
                <span class="room-type">{{ item.type }}</span>
              </div>
            </div>
          </div>
          <div v-if="isLoading" class="loading-text">{{ t('settings.common.loading') }}</div>
          <div v-else-if="rooms.length === 0" class="empty-text">{{ t('settingsStage4.roomSort.empty.room') }}</div>
        </el-tab-pane>

        <!-- 分组排序 -->
        <el-tab-pane :label="t('settingsStage4.roomSort.tabs.group')" name="group">
          <div class="sort-grid">
            <div
              v-for="(item, index) in groups"
              :key="item.id"
              class="sort-item"
              draggable="true"
              @dragstart="handleDragStart(index, 'group')"
              @dragover.prevent
              @drop="handleDrop(index, 'group')"
            >
              <el-icon class="drag-icon"><Menu /></el-icon>
              <span class="item-name">{{ item.name }}</span>
            </div>
          </div>
          <div v-if="isLoading" class="loading-text">{{ t('settings.common.loading') }}</div>
          <div v-else-if="groups.length === 0" class="empty-text">{{ t('settingsStage4.roomSort.empty.group') }}</div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Menu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { getRooms, type RoomDTO } from '@/api/room'
import { getAllRoomGroups, type RoomGroupDTO } from '@/api/roomGroup'
import { getSortOrderMap, updateSortOrders } from '@/api/sortConfig'

const userStore = useUserStore()
const { t } = useI18n()

interface RoomType {
  id: number
  name: string
  sortOrder?: number
}

interface Room {
  id: number
  number: string
  type: string
  sortOrder?: number
}

interface Group {
  id: number
  name: string
  sortOrder?: number
}

const activeTab = ref('roomType')
const draggedIndex = ref<number>(-1)
const isLoading = ref(false)

// 房型数据
const roomTypes = ref<RoomType[]>([])

// 房间数据
const rooms = ref<Room[]>([])

// 分组数据
const groups = ref<Group[]>([])

/**
 * 加载房型数据
 */
const loadRoomTypes = async () => {
  if (!userStore.currentUser?.id) return

  try {
    isLoading.value = true
    const response = await getAllRoomTypes()
    if (response.success) {
      // 获取排序配置
      const sortMap = await getSortOrderMap(userStore.currentUser.id, 'ROOM_TYPE')

      // 将房型数据映射并应用排序
      let roomTypeList = response.data.map((rt: RoomTypeDTO) => ({
        id: rt.id!,
        name: rt.name,
        sortOrder: sortMap.data[rt.id!] ?? 999999,
      }))

      // 按sortOrder排序
      roomTypeList.sort((a, b) => a.sortOrder! - b.sortOrder!)
      roomTypes.value = roomTypeList
    }
  } catch (error) {
    console.error('加载房型失败:', error)
    ElMessage.error(t('settingsStage4.roomSort.messages.loadRoomTypesFailed'))
  } finally {
    isLoading.value = false
  }
}

/**
 * 加载房间数据
 */
const loadRooms = async () => {
  if (!userStore.currentUser?.id) return

  try {
    isLoading.value = true
    const response = await getRooms()
    if (response.success) {
      // 获取排序配置
      const sortMap = await getSortOrderMap(userStore.currentUser.id, 'ROOM')

      // 将房间数据映射并应用排序
      let roomList = response.data.map((r: RoomDTO) => ({
        id: r.id,
        number: r.roomNumber,
        type: r.roomType?.name || '',
        sortOrder: sortMap.data[r.id] ?? 999999,
      }))

      // 按sortOrder排序
      roomList.sort((a, b) => a.sortOrder! - b.sortOrder!)
      rooms.value = roomList
    }
  } catch (error) {
    console.error('加载房间失败:', error)
    ElMessage.error(t('settingsStage4.roomSort.messages.loadRoomsFailed'))
  } finally {
    isLoading.value = false
  }
}

/**
 * 加载分组数据
 */
const loadGroups = async () => {
  if (!userStore.currentUser?.id) return

  try {
    isLoading.value = true
    const response = await getAllRoomGroups()
    if (response.success) {
      // 获取排序配置
      const sortMap = await getSortOrderMap(userStore.currentUser.id, 'GROUP')

      // 将分组数据映射并应用排序
      let groupList = response.data.map((g: RoomGroupDTO) => ({
        id: g.id!,
        name: g.name,
        sortOrder: sortMap.data[g.id!] ?? 999999,
      }))

      // 按sortOrder排序
      groupList.sort((a, b) => a.sortOrder! - b.sortOrder!)
      groups.value = groupList
    }
  } catch (error) {
    console.error('加载分组失败:', error)
    ElMessage.error(t('settingsStage4.roomSort.messages.loadGroupsFailed'))
  } finally {
    isLoading.value = false
  }
}

/**
 * 保存排序配置
 */
const saveSortOrder = async (sortType: string, entityIds: number[]) => {
  if (!userStore.currentUser?.id) return

  try {
    await updateSortOrders(userStore.currentUser.id, {
      sortType,
      entityIds,
    })
  } catch (error) {
    console.error('保存排序失败:', error)
    ElMessage.error(t('settingsStage4.roomSort.messages.saveSortFailed'))
  }
}

// 当前标签页对应的文字
const currentTabText = computed(() => {
  switch (activeTab.value) {
    case 'roomType':
      return t('settingsStage4.roomSort.tabs.roomType')
    case 'room':
      return t('settingsStage4.roomSort.tabs.room')
    case 'group':
      return t('settingsStage4.roomSort.tabs.group')
    default:
      return t('settingsStage4.roomSort.tabs.default')
  }
})

const handleTabChange = (tabName: string) => {
  // 切换标签页时加载对应数据
  switch (tabName) {
    case 'roomType':
      if (roomTypes.value.length === 0) loadRoomTypes()
      break
    case 'room':
      if (rooms.value.length === 0) loadRooms()
      break
    case 'group':
      if (groups.value.length === 0) loadGroups()
      break
  }
}

onMounted(() => {
  // 默认加载房型数据
  loadRoomTypes()
})

const handleDragStart = (index: number, type: string) => {
  draggedIndex.value = index
}

const handleDrop = async (dropIndex: number, type: string) => {
  if (draggedIndex.value === -1 || draggedIndex.value === dropIndex) {
    return
  }

  let targetArray: any[] = []
  let arrayName = ''
  let sortType = ''

  switch (type) {
    case 'roomType':
      targetArray = roomTypes.value
      arrayName = t('settingsStage4.roomSort.entities.roomType')
      sortType = 'ROOM_TYPE'
      break
    case 'room':
      targetArray = rooms.value
      arrayName = t('settingsStage4.roomSort.entities.room')
      sortType = 'ROOM'
      break
    case 'group':
      targetArray = groups.value
      arrayName = t('settingsStage4.roomSort.entities.group')
      sortType = 'GROUP'
      break
  }

  // 执行拖拽排序
  const draggedItem = targetArray[draggedIndex.value]
  targetArray.splice(draggedIndex.value, 1)
  targetArray.splice(dropIndex, 0, draggedItem)

  // 保存排序到后端
  const entityIds = targetArray.map((item) => item.id)
  await saveSortOrder(sortType, entityIds)

  ElMessage.success(t('settingsStage4.roomSort.messages.sortUpdated', { name: arrayName }))
  draggedIndex.value = -1
}
</script>

<style scoped>
.room-sort-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.content-area {
  margin-top: 20px;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.sort-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.sort-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 16px;
  overflow: hidden;
  box-sizing: border-box;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: move;
  user-select: none;
  transition: all 0.2s;
}

.sort-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
  transform: translateY(-2px);
}

.sort-item:active {
  cursor: grabbing;
}

.drag-icon {
  flex-shrink: 0;
  font-size: 18px;
  color: #909399;
  cursor: grab;
}

.item-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-item-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.room-number {
  width: 100%;
  overflow: hidden;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-type {
  width: 100%;
  overflow: hidden;
  font-size: 12px;
  color: #909399;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-text {
  text-align: center;
  padding: 20px;
  color: #409eff;
  font-size: 14px;
}

.empty-text {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 14px;
}

/* 拖拽时的样式 */
.sort-item.dragging {
  opacity: 0.5;
}

:deep(.sort-tabs .el-tabs__header) {
  margin: 0 0 18px;
}

:deep(.sort-tabs .el-tabs__nav-wrap::after),
:deep(.sort-tabs .el-tabs__active-bar) {
  display: none;
}

:deep(.sort-tabs .el-tabs__nav) {
  display: flex;
  align-items: center;
  gap: 22px;
  float: none;
}

:deep(.sort-tabs .el-tabs__item) {
  height: 28px;
  padding: 0 13px !important;
  border-radius: 999px;
  color: #111111;
  font-size: 14px;
  font-weight: 600;
  line-height: 28px;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

:deep(.sort-tabs .el-tabs__item:hover) {
  background: #f5f5f5;
  color: #111111;
}

:deep(.sort-tabs .el-tabs__item.is-active),
:deep(.sort-tabs .el-tabs__item.is-active:hover) {
  background: #111111;
  color: #ffffff;
}
</style>
