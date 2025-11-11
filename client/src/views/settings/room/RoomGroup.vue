<template>
  <div class="room-group-container">
    <!-- 顶部提示信息 -->
    <el-alert
      :closable="false"
      type="info"
      show-icon
    >
      <template #default>
        设置分组后,可在日历房态页面按照分组进行筛选。例:可设置【杭州西湖区】分组,将所有位于西湖区的房间分配在该分组下
      </template>
    </el-alert>

    <!-- 主要内容区域 -->
    <div class="content-area">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="add-group-section" v-if="isEditing">
          <el-input
            v-model="newGroupName"
            placeholder="输入分组名称"
            style="width: 300px"
            maxlength="20"
            show-word-limit
          />
          <el-button type="primary" @click="handleAddGroup">+ 新增</el-button>
        </div>
        <div class="action-buttons">
          <el-button v-if="!isEditing" type="primary" @click="handleStartEdit">编辑</el-button>
          <template v-else>
            <el-button @click="handleCancelEdit">取消</el-button>
            <el-button type="primary" @click="handleConfirmEdit">保存</el-button>
          </template>
        </div>
      </div>

      <!-- 分组列表 -->
      <div class="groups-container">
        <div
          v-for="group in displayRoomGroups"
          :key="group.id"
          class="group-item"
        >
          <div class="group-header">
            <el-input
              v-if="isEditing"
              v-model="group.name"
              placeholder="分组名称"
              class="group-name-input"
            />
            <span v-else class="group-name">{{ group.name }}</span>
            <el-button
              v-if="isEditing"
              link
              type="danger"
              @click="handleDeleteGroup(group)"
            >
              删除分组
            </el-button>
          </div>
          <draggable
            v-if="isEditing"
            v-model="group.rooms"
            :group="{ name: 'rooms', pull: true, put: true }"
            item-key="id"
            class="group-rooms"
            :animation="200"
          >
            <template #item="{ element: room }">
              <el-tag
                closable
                size="large"
                class="room-tag draggable-item"
                @close="handleRemoveRoomFromGroup(group, room)"
              >
                <div class="room-tag-content">
                  <div class="room-number">{{ room.number }}</div>
                  <div class="room-type">{{ room.type }}</div>
                </div>
              </el-tag>
            </template>
          </draggable>
          <div v-else class="group-rooms">
            <el-tag
              v-for="room in group.rooms"
              :key="room.id"
              size="large"
              class="room-tag"
            >
              <div class="room-tag-content">
                <div class="room-number">{{ room.number }}</div>
                <div class="room-type">{{ room.type }}</div>
              </div>
            </el-tag>
          </div>
        </div>

        <!-- 未分组房间 -->
        <div class="ungrouped-section">
          <div class="ungrouped-header">
            <span class="ungrouped-title">未分组房间</span>
            <span v-if="!isEditing" class="ungrouped-hint">点击编辑才可拖拽房间至分组</span>
          </div>
          <draggable
            v-if="isEditing"
            v-model="ungroupedRoomsList"
            :group="{ name: 'rooms', pull: true, put: false }"
            item-key="id"
            class="ungrouped-rooms"
            :animation="200"
            :clone="cloneRoom"
          >
            <template #item="{ element: room }">
              <el-tag
                size="large"
                class="room-tag draggable-item"
              >
                <div class="room-tag-content">
                  <div class="room-number">{{ room.number }}</div>
                  <div class="room-type">{{ room.type }}</div>
                </div>
              </el-tag>
            </template>
          </draggable>
          <div v-else class="ungrouped-rooms">
            <el-tag
              v-for="room in displayUngroupedRooms"
              :key="room.id"
              size="large"
              class="room-tag"
            >
              <div class="room-tag-content">
                <div class="room-number">{{ room.number }}</div>
                <div class="room-type">{{ room.type }}</div>
              </div>
            </el-tag>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import draggable from 'vuedraggable'
import { useUserStore } from '@/stores/user'
import {
  getAllRoomGroups,
  createRoomGroup,
  updateRoomGroup,
  deleteRoomGroup,
  getGroupMembers,
  addRoomsToGroup,
  removeRoomsFromGroup,
  type RoomGroupDTO,
  type RoomGroupMemberDTO,
} from '@/api/roomGroup'
import { getRooms, type RoomDTO } from '@/api/room'

const userStore = useUserStore()

interface Room {
  id: number
  number: string
  type: string
}

interface RoomGroup {
  id: number
  name: string
  description?: string
  rooms: Room[]
}

// 编辑状态
const isEditing = ref(false)
const newGroupName = ref('')

// 所有房间数据
const allRooms = ref<Room[]>([])

// 房间分组数据(实际保存的)
const roomGroups = ref<RoomGroup[]>([])

// 编辑状态的分组数据(编辑时的临时数据)
const editRoomGroups = ref<RoomGroup[]>([])

// 分组成员映射表 (groupId -> roomIds[])
const groupMembersMap = ref<Map<number, number[]>>(new Map())

/**
 * 加载所有房间
 */
const loadRooms = async () => {
  try {
    const response = await getRooms()
    if (response.success) {
      allRooms.value = response.data.map((room: RoomDTO) => ({
        id: room.id,
        number: room.roomNumber,
        type: room.roomType?.name || '',
      }))
    }
  } catch (error) {
    console.error('加载房间列表失败:', error)
    ElMessage.error('加载房间列表失败')
  }
}

/**
 * 加载房间分组
 */
const loadRoomGroups = async () => {
  if (!userStore.currentUser?.id) return

  try {
    const response = await getAllRoomGroups(userStore.currentUser.id)
    if (response.success) {
      // 加载每个分组的成员
      const groups: RoomGroup[] = []
      for (const group of response.data) {
        const membersResponse = await getGroupMembers(group.id!)
        const roomIds = membersResponse.success
          ? membersResponse.data.map((m: RoomGroupMemberDTO) => m.roomId)
          : []

        groupMembersMap.value.set(group.id!, roomIds)

        const rooms = allRooms.value.filter((room) => roomIds.includes(room.id))

        groups.push({
          id: group.id!,
          name: group.name,
          description: group.description,
          rooms,
        })
      }
      roomGroups.value = groups
    }
  } catch (error) {
    console.error('加载房间分组失败:', error)
    ElMessage.error('加载房间分组失败')
  }
}

/**
 * 初始化页面数据
 */
const initData = async () => {
  await loadRooms()
  await loadRoomGroups()
}

onMounted(() => {
  initData()
})

// 显示用的分组数据(根据编辑状态决定显示哪个)
const displayRoomGroups = computed(() => {
  return isEditing.value ? editRoomGroups.value : roomGroups.value
})

// 计算未分组的房间(编辑模式)
const ungroupedRooms = computed(() => {
  const groupedRoomIds = new Set<number>()
  editRoomGroups.value.forEach((group) => {
    group.rooms.forEach((room) => {
      groupedRoomIds.add(room.id)
    })
  })
  return allRooms.value.filter((room) => !groupedRoomIds.has(room.id))
})

// 未分组房间列表(编辑模式,用于draggable的v-model)
const ungroupedRoomsList = ref<Room[]>([])

// 显示模式的未分组房间(非编辑模式)
const displayUngroupedRooms = computed(() => {
  const groupedRoomIds = new Set<number>()
  roomGroups.value.forEach((group) => {
    group.rooms.forEach((room) => {
      groupedRoomIds.add(room.id)
    })
  })
  return allRooms.value.filter((room) => !groupedRoomIds.has(room.id))
})

// 克隆房间对象(用于从未分组拖到分组时)
const cloneRoom = (room: Room): Room => {
  return { ...room }
}

// 开始编辑
const handleStartEdit = () => {
  // 深拷贝当前分组数据用于编辑
  editRoomGroups.value = JSON.parse(JSON.stringify(roomGroups.value))
  ungroupedRoomsList.value = [...ungroupedRooms.value]
  isEditing.value = true
}

const handleAddGroup = () => {
  if (!newGroupName.value.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }

  // 使用临时ID(负数)标识新建的分组,保存时会创建真实ID
  const newGroup: RoomGroup = {
    id: -Date.now(),
    name: newGroupName.value,
    rooms: [],
  }

  editRoomGroups.value.push(newGroup)
  newGroupName.value = ''
  ElMessage.success('添加成功')
}

const handleDeleteGroup = (group: RoomGroup) => {
  ElMessageBox.confirm(`确定要删除分组 "${group.name}" 吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      const index = editRoomGroups.value.findIndex(g => g.id === group.id)
      if (index !== -1) {
        editRoomGroups.value.splice(index, 1)
        ElMessage.success('删除成功')
      }
    })
    .catch(() => {
      // 用户取消删除
    })
}

const handleRemoveRoomFromGroup = (group: RoomGroup, room: Room) => {
  const roomIndex = group.rooms.findIndex((r) => r.id === room.id)
  if (roomIndex !== -1) {
    group.rooms.splice(roomIndex, 1)
  }
}

const handleCancelEdit = () => {
  isEditing.value = false
  editRoomGroups.value = []
  newGroupName.value = ''
}

const handleConfirmEdit = async () => {
  if (!userStore.currentUser?.id) {
    ElMessage.error('用户未登录')
    return
  }

  // 验证所有分组都有名称
  const hasEmptyName = editRoomGroups.value.some((group) => !group.name.trim())
  if (hasEmptyName) {
    ElMessage.warning('请为所有分组设置名称')
    return
  }

  try {
    // 1. 处理删除的分组
    const originalGroupIds = new Set(roomGroups.value.map((g) => g.id))
    const editGroupIds = new Set(editRoomGroups.value.filter((g) => g.id > 0).map((g) => g.id))
    const deletedGroupIds = Array.from(originalGroupIds).filter((id) => !editGroupIds.has(id))

    for (const groupId of deletedGroupIds) {
      await deleteRoomGroup(groupId)
    }

    // 2. 处理新建和更新的分组
    for (const group of editRoomGroups.value) {
      const roomIds = group.rooms.map((r) => r.id)

      if (group.id < 0) {
        // 新建分组
        const createResponse = await createRoomGroup(userStore.currentUser.id, {
          name: group.name,
          description: group.description,
        })
        if (createResponse.success && roomIds.length > 0) {
          // 添加房间到分组
          await addRoomsToGroup(createResponse.data.id!, { roomIds })
        }
      } else {
        // 更新分组
        const originalGroup = roomGroups.value.find((g) => g.id === group.id)
        if (originalGroup && originalGroup.name !== group.name) {
          await updateRoomGroup(group.id, {
            name: group.name,
            description: group.description,
          })
        }

        // 处理分组成员变化
        const originalRoomIds = groupMembersMap.value.get(group.id) || []
        const addedRoomIds = roomIds.filter((id) => !originalRoomIds.includes(id))
        const removedRoomIds = originalRoomIds.filter((id) => !roomIds.includes(id))

        if (addedRoomIds.length > 0) {
          await addRoomsToGroup(group.id, { roomIds: addedRoomIds })
        }
        if (removedRoomIds.length > 0) {
          await removeRoomsFromGroup(group.id, { roomIds: removedRoomIds })
        }
      }
    }

    ElMessage.success('保存成功')
    isEditing.value = false
    editRoomGroups.value = []
    newGroupName.value = ''
    // 重新加载数据
    await loadRoomGroups()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}
</script>

<style scoped>
.room-group-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.content-area {
  margin-top: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.add-group-section {
  display: flex;
  gap: 12px;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.groups-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-item {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
  background: #fff;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.group-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.group-name-input {
  width: 300px;
}

.group-rooms {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 40px;
  align-items: center;
}

.room-tag {
  border-radius: 4px;
  cursor: move;
}

.draggable-item {
  cursor: move;
}

.draggable-item:hover {
  opacity: 0.8;
}

.room-tag-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.room-number {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.room-type {
  font-size: 12px;
  color: #909399;
}

.ungrouped-section {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
  background: #f5f7fa;
}

.ungrouped-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.ungrouped-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.ungrouped-hint {
  font-size: 12px;
  color: #909399;
}

.ungrouped-rooms {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 60px;
}
</style>
