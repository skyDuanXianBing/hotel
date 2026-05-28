<template>
  <el-dialog
    v-model="visible"
    :title="t('stage6.components.roomSelector.title')"
    width="800px"
    :before-close="handleClose"
    class="room-selector-dialog"
  >
    <div class="room-selector-content">
      <!-- Search box and summary -->
      <div class="selector-header">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('stage6.components.roomSelector.searchPlaceholder')"
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="selection-info">
          <span>{{ t('stage6.common.labels.all') }}</span>
          <span class="count">{{ totalRoomCount }}/{{ totalRoomCount }}</span>
          <el-divider direction="vertical" />
          <span>{{ t('stage6.common.labels.selected') }}</span>
          <span class="count selected">({{ selectedRooms.length }})</span>
        </div>
      </div>

      <!-- Room tree list -->
      <div class="room-tree-container">
        <el-tree
          ref="treeRef"
          :data="filteredTreeData"
          :props="treeProps"
          show-checkbox
          node-key="id"
          :default-expanded-keys="defaultExpandedKeys"
          @check="handleCheckChange"
          class="room-tree"
        >
          <template #default="{ node, data }">
            <div class="tree-node-content">
              <span class="node-label">{{ node.label }}</span>
              <span v-if="!data.isRoom" class="node-count">
                {{ data.availableCount || 0 }}/{{ data.totalCount || 0 }}
              </span>
            </div>
          </template>
        </el-tree>
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
import { ref, computed, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import type { ElTree } from 'element-plus'

interface RoomData {
  id: number
  roomNumber: string
  roomType: {
    id: number
    name: string
  }
  status?: string
}

interface TreeNode {
  id: string
  label: string
  isRoom: boolean
  roomId?: number
  totalCount?: number
  availableCount?: number
  children?: TreeNode[]
}

interface Props {
  modelValue: boolean
  rooms?: RoomData[]
  selectedRoomIds?: number[]
}

interface Emits {
  (event: 'update:modelValue', value: boolean): void
  (event: 'confirm', roomIds: number[]): void
}

const props = withDefaults(defineProps<Props>(), {
  rooms: () => [],
  selectedRoomIds: () => [],
})

const emit = defineEmits<Emits>()
const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const treeRef = ref<InstanceType<typeof ElTree>>()
const searchKeyword = ref('')
const selectedRooms = ref<number[]>([])

// Tree configuration
const treeProps = {
  children: 'children',
  label: 'label',
}

// Build tree data
const buildTreeData = (rooms: RoomData[]): TreeNode[] => {
  // Group by room type.
  const roomTypeMap = new Map<number, RoomData[]>()

  rooms.forEach((room) => {
    const roomTypeId = room.roomType.id
    if (!roomTypeMap.has(roomTypeId)) {
      roomTypeMap.set(roomTypeId, [])
    }
    roomTypeMap.get(roomTypeId)!.push(room)
  })

  // Build the tree structure.
  const treeData: TreeNode[] = []

  roomTypeMap.forEach((roomList, roomTypeId) => {
    const roomTypeName = roomList[0]?.roomType.name || t('stage6.components.roomSelector.unknownRoomType')
    const children: TreeNode[] = roomList.map((room) => ({
      id: `room-${room.id}`,
      label: room.roomNumber,
      isRoom: true,
      roomId: room.id,
    }))

    treeData.push({
      id: `type-${roomTypeId}`,
      label: roomTypeName,
      isRoom: false,
      totalCount: children.length,
      availableCount: children.length,
      children,
    })
  })

  return treeData
}

// Original tree data
const treeData = computed(() => buildTreeData(props.rooms))

// Filtered tree data
const filteredTreeData = computed(() => {
  if (!searchKeyword.value.trim()) {
    return treeData.value
  }

  const keyword = searchKeyword.value.toLowerCase()

  return treeData.value
    .map((typeNode) => {
      const filteredChildren = typeNode.children?.filter((roomNode) =>
        roomNode.label.toLowerCase().includes(keyword)
      )

      if (filteredChildren && filteredChildren.length > 0) {
        return {
          ...typeNode,
          children: filteredChildren,
          availableCount: filteredChildren.length,
        }
      }

      return null
    })
    .filter(Boolean) as TreeNode[]
})

// Total room count
const totalRoomCount = computed(() => {
  return props.rooms.length
})

// Default expanded nodes
const defaultExpandedKeys = computed(() => {
  return treeData.value.map((node) => node.id)
})

// Handle selection changes
const handleCheckChange = () => {
  const checkedNodes = treeRef.value?.getCheckedNodes(false, false) || []
  selectedRooms.value = checkedNodes
    .filter((node: any) => node.isRoom)
    .map((node: any) => node.roomId)
}

// Confirm selection
const handleConfirm = () => {
  emit('confirm', selectedRooms.value)
  handleClose()
}

// Close the dialog
const handleClose = () => {
  visible.value = false
}

// Watch dialog opening and initialize selected state
watch(visible, (newVal) => {
  if (newVal) {
    selectedRooms.value = [...props.selectedRoomIds]

    // Delay selected state setup until the tree has rendered.
    setTimeout(() => {
      if (treeRef.value) {
        const checkedKeys = props.selectedRoomIds.map((id) => `room-${id}`)
        treeRef.value.setCheckedKeys(checkedKeys, false)
      }
    }, 100)
  }
})
</script>

<style scoped>
.room-selector-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.room-selector-content {
  min-height: 400px;
  max-height: 600px;
}

.selector-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.selection-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.selection-info .count {
  font-weight: 600;
  color: #333;
}

.selection-info .count.selected {
  color: #409eff;
}

.room-tree-container {
  max-height: 450px;
  overflow-y: auto;
  padding: 8px 0;
}

.tree-node-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  padding-right: 16px;
}

.tree-node-content .node-label {
  font-size: 14px;
}

.tree-node-content .node-count {
  font-size: 12px;
  color: #999;
}

.room-tree :deep(.el-tree-node__content) {
  height: 36px;
  padding: 0 8px;
}

.room-tree :deep(.el-tree-node__expand-icon) {
  font-size: 16px;
}

/* Room type node styles - first level */
.room-tree :deep(.el-tree > .el-tree-node > .el-tree-node__content) {
  font-weight: 600;
  background-color: #f5f7fa;
}

.room-tree :deep(.el-tree > .el-tree-node > .el-tree-node__content:hover) {
  background-color: #e8edf5;
}

/* Room node styles */
.room-tree :deep(.el-tree-node__children .el-tree-node__content) {
  padding-left: 40px;
}

.dialog-footer {
  text-align: right;
}
</style>
