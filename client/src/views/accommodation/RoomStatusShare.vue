<template>
  <div class="room-status-share">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-nav">
      <el-breadcrumb separator=">" class="breadcrumb">
        <el-breadcrumb-item>房态</el-breadcrumb-item>
        <el-breadcrumb-item>房态分享</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <!-- 可以添加筛选或其他功能 -->
      </div>
      
      <div class="toolbar-right">
        <el-button type="default" @click="openShareSettings">
          房态分享设置
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          新增分享链接
        </el-button>
      </div>
    </div>

    <!-- 分享列表表格 -->
    <div class="table-container" v-loading="loading">
      <el-table
        :data="shareList"
        stripe
        border
        class="share-table"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '500' }"
      >
        <el-table-column prop="shareTitle" label="分享标题" width="200" />
        <el-table-column prop="roomCount" label="房间数" width="100" align="center" />
        <el-table-column prop="roomNumbers" label="房间号" width="150" />
        
        <el-table-column label="房态分享链接" min-width="300">
          <template #default="{ row }">
            <div class="link-cell">
              <el-input
                :value="row.shareLink"
                readonly
                class="link-input"
              />
              <el-button 
                type="primary" 
                size="small" 
                @click="copyLink(row.shareLink)"
                class="copy-btn"
              >
                复制链接
              </el-button>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              size="small" 
              @click="editShare(row)"
            >
              编辑
            </el-button>
            <el-button 
              type="danger" 
              link 
              size="small" 
              @click="deleteShare(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 25, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="onPageSizeChange"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <!-- 新增/编辑分享对话框 -->
    <el-dialog
      :title="dialogMode === 'create' ? '新增分享链接' : '编辑分享链接'"
      v-model="showDialog"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="shareForm"
        :rules="formRules"
        label-width="80px"
        class="share-form"
      >
        <!-- 分享标题 -->
        <el-form-item label="分享标题" prop="shareTitle">
          <el-input v-model="shareForm.shareTitle" placeholder="请输入分享标题" />
        </el-form-item>
        
        <!-- 房态权限 -->
        <div class="form-section">
          <div class="section-title">房态权限</div>
          
          <!-- 查看房间状态开关 -->
          <div class="form-row">
            <span class="row-label">查看房间状态</span>
            <el-switch
              v-model="shareForm.viewRoomStatus"
              size="default"
            />
          </div>
          
          <!-- 查看提示 -->
          <div class="permission-hint">
            <span class="hint-text">仅在对应房态格子存在订单或关房时，系统会模糊显示</span>
          </div>

          <!-- 查看方式 -->
          <div class="form-row">
            <span class="row-label">查看</span>
            <el-radio-group v-model="shareForm.viewType" class="horizontal-radio-group">
              <el-radio value="normal">正常查看</el-radio>
              <el-radio value="blurred">模糊查看</el-radio>
              <el-tooltip content="仅在对应房态格子存在订单或关房时，系统会模糊显示" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-radio-group>
          </div>

          <!-- 开关房权限 -->
          <div class="form-row">
            <span class="row-label">开关房权限</span>
            <el-radio-group v-model="shareForm.queryMode" class="horizontal-radio-group">
              <el-radio value="enabled">启用</el-radio>
              <el-radio value="disabled">关闭</el-radio>
            </el-radio-group>
          </div>
        </div>

        <!-- 统计数据 -->
        <div class="form-section">
          <div class="section-title">统计数据</div>
          <div class="tag-group">
            <el-tag
              v-for="item in filterOptions"
              :key="item"
              :type="shareForm.filterItems.includes(item) ? 'primary' : 'info'"
              :effect="shareForm.filterItems.includes(item) ? 'dark' : 'plain'"
              @click="toggleFilterItem(item)"
              class="selectable-tag"
            >
              {{ item }}
            </el-tag>
          </div>
        </div>

        <!-- 订单数据 -->
        <div class="form-section">
          <div class="section-title">订单数据</div>
          <div class="tag-group">
            <el-tag
              v-for="item in orderOptions"
              :key="item"
              :type="shareForm.orderItems.includes(item) ? 'primary' : 'info'"
              :effect="shareForm.orderItems.includes(item) ? 'dark' : 'plain'"
              @click="toggleOrderItem(item)"
              class="selectable-tag"
            >
              {{ item }}
            </el-tag>
          </div>
        </div>

        <!-- 关联房间 -->
        <div class="form-section">
          <div class="section-title">关联房间</div>
          <el-button type="primary" link @click="addRoom">
            + 添加房间
          </el-button>
          
          <!-- 已选择的房间列表 -->
          <div v-if="shareForm.associatedRooms.length > 0" class="associated-rooms-list">
            <div class="rooms-summary">
              已选择 {{ shareForm.associatedRooms.length }} 间房间
            </div>
            <div class="rooms-tags">
              <el-tag
                v-for="room in getAssociatedRoomsDisplay()"
                :key="room.id"
                closable
                @close="removeAssociatedRoom(room.id)"
                class="room-tag"
              >
                {{ room.displayName }}
              </el-tag>
            </div>
          </div>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" @click="saveShare" :loading="saving">
            完成
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加房间弹窗 -->
    <AddRoomDialog
      v-model="showAddRoomDialog"
      :selected-room-ids="shareForm.associatedRooms"
      @confirm="handleRoomSelectionConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { request } from '@/utils/request'
import type { RoomStatusShare, RoomStatusShareListData, RoomStatusShareConfig, ApiResponse } from '@/types/room'
import AddRoomDialog from '@/components/AddRoomDialog.vue'
import { getRoomStatusShares, createRoomStatusShare, updateRoomStatusShare, deleteRoomStatusShare } from '@/api/roomStatusShare'

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const shareList = ref<RoomStatusShare[]>([])
const showDialog = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const currentEditId = ref<number | null>(null)
const showAddRoomDialog = ref(false)

// 分页数据
const pagination = reactive({
  page: 1,
  pageSize: 25,
  total: 0
})

// 表单数据
const shareForm = reactive<RoomStatusShareConfig>({
  shareTitle: '',
  viewRoomStatus: true, // 查看房间状态开关
  queryMethod: true, // true=查看房态，false=按时间查看
  viewType: 'normal',
  queryMode: 'enabled',
  filterItems: [],
  orderItems: [],
  associatedRooms: []
})

// 统计数据选项
const filterOptions = ['全部', '入住率', '入住间夜', '平均房费', '房费收入']

// 订单数据选项
const orderOptions = ['全部', '预订人姓名', '渠道企业', '房间信息', '房费', '订单金额', '订单备注']

// 表单验证规则
const formRules = {
  shareTitle: [
    { required: true, message: '请输入分享标题', trigger: 'blur' }
  ]
}

const formRef = ref()

// 获取分享列表
const fetchShareList = async () => {
  try {
    loading.value = true
    
    const response = await getRoomStatusShares(pagination.page, pagination.pageSize)
    
    if (response.success) {
      shareList.value = response.data.shares.map((item: any) => ({
        id: item.id,
        shareTitle: item.shareTitle,
        roomCount: item.roomCount,
        roomNumbers: item.roomNumbers,
        shareLink: item.shareLink,
        config: {
          shareTitle: item.shareTitle,
          viewRoomStatus: true,
          queryMethod: true,
          viewType: 'normal',
          queryMode: 'enabled',
          filterItems: ['全部'],
          orderItems: ['全部'],
          associatedRooms: []
        },
        createdAt: item.createdAt,
        updatedAt: item.updatedAt,
        isActive: item.isActive
      }))
      pagination.total = response.data.total
    } else {
      ElMessage.error(response.message || '获取分享列表失败')
    }
  } catch (error) {
    console.error('获取分享列表失败:', error)
    ElMessage.error('获取分享列表失败')
    
    // 添加模拟数据用于展示
    shareList.value = [
      {
        id: 1,
        shareTitle: '大床',
        roomCount: 1,
        roomNumbers: 'a01',
        shareLink: 'http://localhost:8091/share/abc123',
        config: {
          shareTitle: '大床',
          viewRoomStatus: true,
          queryMethod: true,
          viewType: 'normal',
          queryMode: 'enabled',
          filterItems: ['全部'],
          orderItems: ['全部'],
          associatedRooms: []
        },
        createdAt: '2025-01-01T00:00:00Z',
        updatedAt: '2025-01-01T00:00:00Z',
        isActive: true
      }
    ]
    pagination.total = 1
  } finally {
    loading.value = false
  }
}

// 复制链接
const copyLink = async (link: string) => {
  try {
    await navigator.clipboard.writeText(link)
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

// 切换筛选项目
const toggleFilterItem = (item: string) => {
  if (item === '全部') {
    // 点击全部时，如果当前是全选状态，则清空；否则选择所有
    const isAllSelected = shareForm.filterItems.length === filterOptions.length
    if (isAllSelected) {
      shareForm.filterItems = []
    } else {
      shareForm.filterItems = [...filterOptions]
    }
  } else {
    const index = shareForm.filterItems.indexOf(item)
    if (index > -1) {
      shareForm.filterItems.splice(index, 1)
      // 移除"全部"选项，因为不是全选状态了
      const allIndex = shareForm.filterItems.indexOf('全部')
      if (allIndex > -1) {
        shareForm.filterItems.splice(allIndex, 1)
      }
    } else {
      shareForm.filterItems.push(item)
      // 检查是否所有选项都被选中，如果是则添加"全部"
      const otherOptions = filterOptions.filter(opt => opt !== '全部')
      const selectedOthers = shareForm.filterItems.filter(opt => opt !== '全部')
      if (selectedOthers.length === otherOptions.length && !shareForm.filterItems.includes('全部')) {
        shareForm.filterItems.push('全部')
      }
    }
  }
}

// 切换订单项目
const toggleOrderItem = (item: string) => {
  if (item === '全部') {
    // 点击全部时，如果当前是全选状态，则清空；否则选择所有
    const isAllSelected = shareForm.orderItems.length === orderOptions.length
    if (isAllSelected) {
      shareForm.orderItems = []
    } else {
      shareForm.orderItems = [...orderOptions]
    }
  } else {
    const index = shareForm.orderItems.indexOf(item)
    if (index > -1) {
      shareForm.orderItems.splice(index, 1)
      // 移除"全部"选项，因为不是全选状态了
      const allIndex = shareForm.orderItems.indexOf('全部')
      if (allIndex > -1) {
        shareForm.orderItems.splice(allIndex, 1)
      }
    } else {
      shareForm.orderItems.push(item)
      // 检查是否所有选项都被选中，如果是则添加"全部"
      const otherOptions = orderOptions.filter(opt => opt !== '全部')
      const selectedOthers = shareForm.orderItems.filter(opt => opt !== '全部')
      if (selectedOthers.length === otherOptions.length && !shareForm.orderItems.includes('全部')) {
        shareForm.orderItems.push('全部')
      }
    }
  }
}

// 添加房间
const addRoom = () => {
  showAddRoomDialog.value = true
}

// 处理房间选择确认
const handleRoomSelectionConfirm = (roomIds: number[]) => {
  shareForm.associatedRooms = roomIds
  ElMessage.success(`已选择 ${roomIds.length} 间房间`)
}

// 移除关联房间
const removeAssociatedRoom = (roomId: number) => {
  shareForm.associatedRooms = shareForm.associatedRooms.filter(id => id !== roomId)
}

// 缓存房间数据
const roomsCache = ref<any[]>([])

// 获取房间数据并缓存
const fetchRoomsForDisplay = async () => {
  try {
    const response: ApiResponse<any[]> = await request.get('/rooms')
    if (response.success) {
      roomsCache.value = response.data
    }
  } catch (error) {
    console.error('获取房间数据失败:', error)
  }
}

// 获取关联房间显示信息
const getAssociatedRoomsDisplay = () => {
  return shareForm.associatedRooms.map(roomId => {
    const room = roomsCache.value.find(r => r.id === roomId)
    if (room) {
      return {
        id: roomId,
        displayName: `${room.roomType?.name || '未知房型'} - ${room.roomNumber}`
      }
    }
    return {
      id: roomId,
      displayName: `房间${roomId}`
    }
  })
}

// 编辑分享
const editShare = (share: RoomStatusShare) => {
  dialogMode.value = 'edit'
  currentEditId.value = share.id
  shareForm.shareTitle = share.shareTitle
  shareForm.viewRoomStatus = share.config.viewRoomStatus
  shareForm.queryMethod = share.config.queryMethod
  shareForm.viewType = share.config.viewType
  shareForm.queryMode = share.config.queryMode
  shareForm.filterItems = [...share.config.filterItems]
  shareForm.orderItems = [...share.config.orderItems]
  shareForm.associatedRooms = [...share.config.associatedRooms]
  showDialog.value = true
}

// 删除分享
const deleteShare = async (share: RoomStatusShare) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分享"${share.shareTitle}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    const response = await deleteRoomStatusShare(share.id)
    
    if (response.success) {
      ElMessage.success('删除成功')
      fetchShareList()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 打开创建对话框
const openCreateDialog = () => {
  dialogMode.value = 'create'
  currentEditId.value = null
  resetForm()
  showDialog.value = true
}

// 打开分享设置
const openShareSettings = () => {
  ElMessage.info('房态分享设置功能正在开发中...')
}

// 保存分享
const saveShare = async () => {
  try {
    const valid = await formRef.value?.validate()
    if (!valid) return

    saving.value = true

    // 准备请求数据
    const requestData = {
      shareTitle: shareForm.shareTitle,
      viewRoomStatus: shareForm.viewRoomStatus,
      queryMethod: shareForm.queryMethod,
      viewType: shareForm.viewType,
      queryMode: shareForm.queryMode,
      filterItems: shareForm.filterItems,
      orderItems: shareForm.orderItems,
      associatedRooms: shareForm.associatedRooms
    }

    let response
    
    if (dialogMode.value === 'create') {
      // 创建分享链接
      response = await createRoomStatusShare(requestData)
      if (response.success) {
        ElMessage.success('分享链接创建成功')
      } else {
        ElMessage.error(response.message || '创建失败')
        return
      }
    } else {
      // 更新分享链接
      response = await updateRoomStatusShare(currentEditId.value!, requestData)
      if (response.success) {
        ElMessage.success('分享链接更新成功')
      } else {
        ElMessage.error(response.message || '更新失败')
        return
      }
    }

    showDialog.value = false
    fetchShareList()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 重置表单
const resetForm = () => {
  shareForm.shareTitle = ''
  shareForm.viewRoomStatus = true
  shareForm.queryMethod = true
  shareForm.viewType = 'normal'
  shareForm.queryMode = 'enabled'
  shareForm.filterItems = []
  shareForm.orderItems = []
  shareForm.associatedRooms = []
  formRef.value?.clearValidate()
}

// 分页变化处理
const onPageChange = (page: number) => {
  pagination.page = page
  fetchShareList()
}

const onPageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchShareList()
}

// 组件挂载时获取数据
onMounted(() => {
  fetchShareList()
  fetchRoomsForDisplay()
})
</script>

<style scoped>
.room-status-share {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.breadcrumb-nav {
  margin-bottom: 20px;
}

.breadcrumb {
  background: white;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.share-table {
  width: 100%;
}

.link-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.link-input {
  flex: 1;
}

.copy-btn {
  flex-shrink: 0;
}

.pagination-container {
  padding: 16px 20px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 分享对话框样式 */
.share-form {
  max-height: 500px;
  overflow-y: auto;
}

.form-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.permission-hint {
  margin-bottom: 16px;
  padding: 8px 12px;
  background: #f0f9ff;
  border: 1px solid #e1f5fe;
  border-radius: 4px;
}

.hint-text {
  font-size: 12px;
  color: #666;
}

.form-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}

.form-row:last-child {
  margin-bottom: 0;
}

.row-label {
  min-width: 100px;
  font-size: 14px;
  color: #606266;
}

.radio-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.horizontal-radio-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.horizontal-radio-group :deep(.el-radio) {
  margin-right: 0;
}

.radio-desc {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.info-icon {
  margin-left: 8px;
  color: #909399;
  cursor: pointer;
  font-size: 14px;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selectable-tag {
  cursor: pointer;
  user-select: none;
  transition: all 0.3s;
}

.selectable-tag:hover {
  transform: scale(1.05);
}

/* 关联房间样式 */
.associated-rooms-list {
  margin-top: 16px;
  padding: 12px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.rooms-summary {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.rooms-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.room-tag {
  margin: 0;
}
</style>