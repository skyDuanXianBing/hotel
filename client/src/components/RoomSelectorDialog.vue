<template>
  <el-dialog
    v-model="visible"
    title="关联房间"
    width="800px"
    :before-close="handleClose"
    class="room-selector-dialog"
  >
    <div class="room-selector-content">
      <!-- 搜索框和统计信息 -->
      <div class="selector-header">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索房间号或房型"
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="selection-info">
          <span>全部</span>
          <span class="count">{{ totalRoomCount }}/{{ totalRoomCount }}</span>
          <el-divider direction="vertical" />
          <span>已选</span>
          <span class="count selected">({{ selectedRooms.length }})</span>
        </div>
      </div>

      <!-- 房间树形列表 -->
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
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
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

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const treeRef = ref<InstanceType<typeof ElTree>>()
const searchKeyword = ref('')
const selectedRooms = ref<number[]>([])

// 树形结构配置
const treeProps = {
  children: 'children',
  label: 'label',
}

// 构建树形数据
const buildTreeData = (rooms: RoomData[]): TreeNode[] => {
  // 按房型分组
  const roomTypeMap = new Map<number, RoomData[]>()

  rooms.forEach((room) => {
    const roomTypeId = room.roomType.id
    if (!roomTypeMap.has(roomTypeId)) {
      roomTypeMap.set(roomTypeId, [])
    }
    roomTypeMap.get(roomTypeId)!.push(room)
  })

  // 构建树形结构
  const treeData: TreeNode[] = []

  roomTypeMap.forEach((roomList, roomTypeId) => {
    const roomTypeName = roomList[0]?.roomType.name || '未知房型'
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

// 原始树形数据
const treeData = computed(() => buildTreeData(props.rooms))

// 过滤后的树形数据
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

// 总房间数
const totalRoomCount = computed(() => {
  return props.rooms.length
})

// 默认展开的节点
const defaultExpandedKeys = computed(() => {
  return treeData.value.map((node) => node.id)
})

// 处理选择变化
const handleCheckChange = () => {
  const checkedNodes = treeRef.value?.getCheckedNodes(false, false) || []
  selectedRooms.value = checkedNodes
    .filter((node: any) => node.isRoom)
    .map((node: any) => node.roomId)
}

// 确认选择
const handleConfirm = () => {
  emit('confirm', selectedRooms.value)
  handleClose()
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
}

// 监听对话框打开,初始化选中状态
watch(visible, (newVal) => {
  if (newVal) {
    selectedRooms.value = [...props.selectedRoomIds]

    // 延迟设置选中状态,确保树形结构已渲染
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

/* 房型节点样式 - 第一层节点 */
.room-tree :deep(.el-tree > .el-tree-node > .el-tree-node__content) {
  font-weight: 600;
  background-color: #f5f7fa;
}

.room-tree :deep(.el-tree > .el-tree-node > .el-tree-node__content:hover) {
  background-color: #e8edf5;
}

/* 房间节点样式 */
.room-tree :deep(.el-tree-node__children .el-tree-node__content) {
  padding-left: 40px;
}

.dialog-footer {
  text-align: right;
}
</style>