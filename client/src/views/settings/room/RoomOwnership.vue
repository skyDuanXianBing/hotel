<template>
  <div class="room-ownership-container">
    <!-- 页面标题和说明 -->
    <div class="page-header">
      <div class="header-info-box">
        <div class="info-item">
          <span class="info-number">1.</span>
          <span class="info-text">
            {{ t('settingsStage4.roomOwnership.notices.dragSort') }}
          </span>
        </div>
        <div class="info-item">
          <span class="info-number">2.</span>
          <span class="info-text">
            {{ t('settingsStage4.roomOwnership.notices.refresh') }}
            <span class="warning-text">{{ t('settingsStage4.roomOwnership.notices.warning') }}</span>
          </span>
        </div>
      </div>
    </div>

    <!-- 房型房间列表 -->
    <div class="room-list-wrapper">
      <el-table :data="roomTypeList" border class="room-table" v-loading="loading">
        <el-table-column prop="name" :label="t('settingsStage4.roomSettings.columns.roomTypeName')" width="200" align="left">
          <template #default="{ row }">
            <div class="room-type-cell">
              <div class="room-type-name">{{ row.name }}</div>
              <div v-if="row.warning" class="room-type-warning">
                {{ row.warning }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="defaultPrice" :label="t('settingsStage4.roomOwnership.columns.defaultPrice')" width="150" align="left">
          <template #default="{ row }">
            <span>¥{{ formatPrice(row.defaultPrice) }}</span>
          </template>
        </el-table-column>

        <el-table-column :label="t('settingsStage4.roomSettings.columns.roomNumbers')" align="left">
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
      <el-button @click="handleCancel">{{ t('settings.common.cancel') }}</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">{{ t('settings.common.save') }}</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()
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
      ElMessage.error(t('settingsStage4.roomOwnership.messages.loadFailed'))
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
        warning = t('settingsStage4.roomOwnership.warningBoundChannel')
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
    ElMessage.error(t('settingsStage4.roomOwnership.messages.loadFailed'))
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
  ElMessage.success(t('settingsStage4.roomOwnership.messages.copySuccess', { roomNumber: room.roomNumber }))
}

// 取消
const handleCancel = () => {
  ElMessageBox.confirm(t('settingsStage4.roomOwnership.messages.cancelConfirm'), t('settingsStage4.roomOwnership.messages.cancelTitle'), {
    confirmButtonText: t('settings.common.confirmButton'),
    cancelButtonText: t('settings.common.cancelButton'),
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

    ElMessage.success(t('settingsStage4.roomOwnership.messages.saveSuccess'))

    // 延迟返回,让用户看到成功提示
    setTimeout(() => {
      router.back()
    }, 1000)
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(t('settingsStage4.roomOwnership.messages.saveFailed'))
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
  background: #ffffff;
}

/* 页面头部 */
.page-header {
  padding: 18px 20px 12px;
}

.header-info-box {
  min-height: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-sizing: border-box;
  background: #e8efff;
  border: 1px solid #7ea1ff;
  border-radius: 4px;
  padding: 8px 18px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 2px;
  line-height: 22px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-number {
  font-weight: 500;
  color: #3f7dff;
  margin-right: 0;
  flex-shrink: 0;
}

.info-text {
  color: #3f7dff;
  font-size: 14px;
}

.warning-text {
  color: #ff5f6d;
  font-weight: 500;
}

/* 房间列表 */
.room-list-wrapper {
  flex: 1;
  min-height: 0;
  padding: 8px 20px 12px;
  overflow: auto;
}

.room-table {
  width: 100%;
}

/* 房型单元格 */
.room-type-cell {
  min-height: 28px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.room-type-name {
  font-size: 14px;
  font-weight: 400;
  color: #3f3f3f;
  line-height: 20px;
}

.room-type-warning {
  font-size: 12px;
  color: #ff5f6d;
  line-height: 16px;
}

/* 房间号容器 */
.room-numbers-container {
  min-height: 28px;
  display: flex;
  align-items: center;
  padding: 0;
}

.room-tags-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0;
}

.room-tag {
  height: 24px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 8px;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #8a8f94;
  cursor: move;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease;
  user-select: none;
}

.room-tag:hover {
  background: #f7fbff;
  border-color: #7ea1ff;
}

.drag-icon {
  color: #8a8f94;
  font-size: 13px;
  cursor: grab;
}

.room-tag:active .drag-icon {
  cursor: grabbing;
}

.room-number {
  font-size: 14px;
  color: #8a8f94;
  line-height: 22px;
}

.copy-icon {
  color: #8a8f94;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.copy-icon:hover {
  color: #3f7dff;
}

/* 底部操作按钮 */
.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 20px;
  border-top: 1px solid #e6e8eb;
  background: #ffffff;
}

/* 表格样式优化 */
:deep(.el-table) {
  --el-table-border-color: #e6e8eb;
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #fafcff;
  font-size: 14px;
  color: #3f3f3f;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

:deep(.el-table th) {
  height: 36px;
  background: #f8fafc;
  color: #293241;
  font-weight: 600;
}

:deep(.el-table td) {
  height: 36px;
  padding: 0;
  border-bottom: 1px solid #e6e8eb;
}

:deep(.el-table .cell) {
  min-height: 36px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  line-height: 20px;
}

:deep(.el-table th.el-table__cell > .cell) {
  min-height: 34px;
  font-size: 14px;
}

:deep(.el-table .el-table__cell) {
  border-right: 1px solid #e6e8eb;
}

:deep(.el-table .el-table__cell:last-child) {
  border-right: none;
}

/* 拖拽占位符 */
.sortable-ghost {
  opacity: 0.5;
  background: #e8efff;
}
</style>
