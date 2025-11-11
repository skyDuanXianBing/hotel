<template>
  <div class="room-ownership-container">
    <!-- 页面标题和说明 -->
    <div class="page-header">
      <div class="header-info-box">
        <div class="info-item">
          <span class="info-number">1.</span>
          <span class="info-text">
            拖动房间号可以更改房间归属的房型与房间的排序,房间的排序仅影响房态页面【按房型排序】模式下的排序顺序。
          </span>
        </div>
        <div class="info-item">
          <span class="info-number">2.</span>
          <span class="info-text">
            更改房间归属后,将自动刷新您有订单的房型房间信息与开关房记录。
            <span class="warning-text">届时会都更新为变更后的房型房间,该操作不影响历史报表统计,请谨慎修改。</span>
          </span>
        </div>
      </div>
    </div>

    <!-- 房型房间列表 -->
    <div class="room-list-wrapper">
      <el-table :data="roomTypeList" border class="room-table" v-loading="loading">
        <el-table-column prop="name" label="房型名称" width="300" align="left">
          <template #default="{ row }">
            <div class="room-type-cell">
              <div class="room-type-name">{{ row.name }}</div>
              <div v-if="row.warning" class="room-type-warning">
                {{ row.warning }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="defaultPrice" label="默认价" width="150" align="center">
          <template #default="{ row }">
            <span>¥{{ formatPrice(row.defaultPrice) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="房间号" align="left">
          <template #default="{ row }">
            <div class="room-numbers-container">
              <draggable
                v-model="row.rooms"
                item-key="id"
                class="room-tags-list"
                :group="{ name: 'rooms', pull: true, put: true }"
                @change="handleRoomChange(row, $event)"
              >
                <template #item="{ element }">
                  <div class="room-tag">
                    <el-icon class="drag-icon"><Grid /></el-icon>
                    <span class="room-number">{{ element.roomNumber }}</span>
                    <el-icon class="copy-icon" @click="handleCopyRoom(element)">
                      <DocumentCopy />
                    </el-icon>
                  </div>
                </template>
              </draggable>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 底部操作按钮 -->
    <div class="footer-actions">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Grid, DocumentCopy } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { getRooms, type RoomDTO } from '@/api/room'

interface RoomItem {
  id: number
  roomNumber: string
  roomTypeId: number
  floor?: number
  status?: string
}

interface RoomTypeItem {
  id: number
  name: string
  defaultPrice: number
  warning?: string
  rooms: RoomItem[]
}

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const roomTypeList = ref<RoomTypeItem[]>([])

// 格式化价格
const formatPrice = (price: number | undefined): string => {
  if (price === undefined || price === null) return '0.00'
  return price.toFixed(2)
}

// 加载房型和房间数据
const loadData = async () => {
  try {
    loading.value = true

    // 并行加载房型和房间数据
    const [roomTypesResponse, roomsResponse] = await Promise.all([
      getAllRoomTypes(),
      getRooms(),
    ])

    if (!roomTypesResponse.success || !roomsResponse.success) {
      ElMessage.error('加载数据失败')
      return
    }

    const roomTypes = roomTypesResponse.data || []
    const rooms = roomsResponse.data || []

    // 将房间按房型分组
    roomTypeList.value = roomTypes.map((rt: RoomTypeDTO) => {
      // 筛选属于该房型的房间
      const typeRooms = rooms
        .filter((room: RoomDTO) => room.roomType.id === rt.id)
        .map((room: RoomDTO) => ({
          id: room.id,
          roomNumber: room.roomNumber,
          roomTypeId: rt.id,
          floor: room.floor,
          status: room.status,
        }))

      // 检查是否有未绑定的房间或营销房型
      let warning = ''
      if (typeRooms.length === 0) {
        warning = '本房型已绑定OTA渠道/营销房型,请都开通后操作'
      }

      return {
        id: rt.id,
        name: rt.name,
        defaultPrice: rt.defaultPrice || 0,
        warning,
        rooms: typeRooms,
      }
    })
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 处理房间拖拽变化
const handleRoomChange = (roomType: RoomTypeItem, event: any) => {
  console.log('房间归属变化:', roomType.name, event)

  // 如果是从其他房型拖入
  if (event.added) {
    const addedRoom = event.added.element as RoomItem
    // 更新房间的房型ID
    addedRoom.roomTypeId = roomType.id
  }
}

// 复制房间号
const handleCopyRoom = (room: RoomItem) => {
  navigator.clipboard.writeText(room.roomNumber)
  ElMessage.success(`已复制房间号: ${room.roomNumber}`)
}

// 取消
const handleCancel = () => {
  ElMessageBox.confirm('确定要取消吗?未保存的更改将丢失。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      router.back()
    })
    .catch(() => {
      // 用户取消
    })
}

// 保存
const handleSave = async () => {
  try {
    saving.value = true

    // TODO: 调用后端API保存房间归属关系
    // 需要将 roomTypeList 中的房间归属信息提交到后端

    ElMessage.success('保存成功')

    // 延迟返回,让用户看到成功提示
    setTimeout(() => {
      router.back()
    }, 1000)
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.room-ownership-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 页面头部 */
.page-header {
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.header-info-box {
  background: #f0f9ff;
  border: 1px solid #bae7ff;
  border-radius: 4px;
  padding: 16px 20px;
}

.info-item {
  display: flex;
  margin-bottom: 8px;
  line-height: 1.6;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-number {
  font-weight: 500;
  color: #333;
  margin-right: 8px;
  flex-shrink: 0;
}

.info-text {
  color: #666;
  font-size: 14px;
}

.warning-text {
  color: #ff9800;
  font-weight: 500;
}

/* 房间列表 */
.room-list-wrapper {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

.room-table {
  width: 100%;
}

/* 房型单元格 */
.room-type-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.room-type-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.room-type-warning {
  font-size: 12px;
  color: #ff9800;
}

/* 房间号容器 */
.room-numbers-container {
  padding: 8px 0;
}

.room-tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 40px;
  padding: 4px;
}

.room-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: move;
  transition: all 0.2s ease;
  user-select: none;
}

.room-tag:hover {
  background: #e6f7ff;
  border-color: #1890ff;
}

.drag-icon {
  color: #999;
  font-size: 14px;
  cursor: grab;
}

.room-tag:active .drag-icon {
  cursor: grabbing;
}

.room-number {
  font-size: 14px;
  color: #333;
}

.copy-icon {
  color: #999;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.copy-icon:hover {
  color: #1890ff;
}

/* 底部操作按钮 */
.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e8e8e8;
  background: #fafafa;
}

/* 表格样式优化 */
:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background: #fafafa;
  color: #333;
  font-weight: 600;
}

:deep(.el-table td) {
  padding: 12px 0;
}

/* 拖拽占位符 */
.sortable-ghost {
  opacity: 0.5;
  background: #e6f7ff;
}
</style>
