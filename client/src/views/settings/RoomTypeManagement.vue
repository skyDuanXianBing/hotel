<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, View, Upload } from '@element-plus/icons-vue'
import RoomTypeEditor from './RoomTypeEditor.vue'
import RoomTypeBasicInfo from './RoomTypeBasicInfo.vue'
import { request } from '@/utils/request'

interface RoomInfo {
  id: number
  roomNumber: string
  status: string
  floor?: number
  notes?: string
}

interface RoomType {
  id: number
  name: string
  code: string
  description: string
  totalRooms: number
  createdAt?: string
  updatedAt?: string
  rooms?: RoomInfo[]
  defaultPrice?: number
  weekdayPrice?: number
  weekendPrice?: number
  roomCount?: number
  roomNumbers?: string[]
  channel?: string
  imageUrl?: string
}

interface NewRoomTypeForm {
  name: string
  description: string
  pricingType: 'fixed' | 'flexible'
  fixedPrice: number
  weekdayPrice: number
  weekendPrice: number
  roomCreationType: 'sync' | 'later'
  roomPrefix: string
  roomCount: number
  roomNumbers: string[]
}

const roomTypes = ref<RoomType[]>([])

const currentPage = ref(1)
const pageSize = ref(25)
const total = computed(() => roomTypes.value.length)
const loading = ref(false)

const activeTab = ref('房间归属')
const tabs = ['房间归属', '导入房型', '新增房型']

// 编辑房型状态
const isEditing = ref(false)
const editingRoomType = ref<RoomType | null>(null)

// 基础信息编辑状态
const isEditingBasicInfo = ref(false)
const editingBasicInfoRoomType = ref<RoomType | null>(null)

const newRoomTypeForm = ref<NewRoomTypeForm>({
  name: '',
  description: '',
  pricingType: 'fixed',
  fixedPrice: 0.0,
  weekdayPrice: 0.0,
  weekendPrice: 0.0,
  roomCreationType: 'sync',
  roomPrefix: '',
  roomCount: 1,
  roomNumbers: [],
})

// 监听房间数量变化,调整房间号数组长度
watch(
  () => newRoomTypeForm.value.roomCount,
  (newCount) => {
    const currentLength = newRoomTypeForm.value.roomNumbers.length

    if (newCount > currentLength) {
      // 增加房间号输入框
      const roomsToAdd = newCount - currentLength
      for (let i = 0; i < roomsToAdd; i++) {
        // 如果有前缀,自动生成房间号;否则添加空字符串
        if (newRoomTypeForm.value.roomPrefix) {
          const roomNumber = `${newRoomTypeForm.value.roomPrefix}${(currentLength + i + 1).toString().padStart(2, '0')}`
          newRoomTypeForm.value.roomNumbers.push(roomNumber)
        } else {
          newRoomTypeForm.value.roomNumbers.push('')
        }
      }
    } else if (newCount < currentLength) {
      // 减少房间号输入框
      newRoomTypeForm.value.roomNumbers = newRoomTypeForm.value.roomNumbers.slice(0, newCount)
    }
  },
)

// 监听房间号数组变化,同步更新房间数量
watch(
  () => newRoomTypeForm.value.roomNumbers.length,
  (newLength) => {
    newRoomTypeForm.value.roomCount = newLength
  },
)

// API调用相关方法
const loadRoomTypes = async () => {
  loading.value = true
  try {
    const response = (await request.get('/room-types/with-rooms')) as any
    if (response.success) {
      roomTypes.value = response.data.map((item: any) => ({
        id: item.id,
        name: item.name,
        code: item.code,
        description: item.description,
        totalRooms: item.totalRooms,
        createdAt: item.createdAt,
        updatedAt: item.updatedAt,
        rooms: item.rooms,
        defaultPrice: item.defaultPrice,
        weekdayPrice: item.weekdayPrice,
        weekendPrice: item.weekendPrice,
        roomCount: item.rooms?.length || 0,
        roomNumbers: item.rooms?.map((room: RoomInfo) => room.roomNumber) || [],
        channel: '-', // 临时默认值
      }))
    } else {
      ElMessage.error(response.message || '获取房型列表失败')
    }
  } catch (error) {
    console.error('获取房型列表失败:', error)
    ElMessage.error('获取房型列表失败')
  } finally {
    loading.value = false
  }
}

const createRoomTypeApi = async (roomTypeData: any) => {
  loading.value = true
  try {
    const response = (await request.post('/room-types', roomTypeData)) as any
    if (response.success) {
      ElMessage.success(response.message || '房型创建成功')
      await loadRoomTypes() // 重新加载数据
      return true
    } else {
      ElMessage.error(response.message || '房型创建失败')
      return false
    }
  } catch (error) {
    console.error('创建房型失败:', error)
    ElMessage.error('创建房型失败')
    return false
  } finally {
    loading.value = false
  }
}

const deleteRoomTypeApi = async (id: number) => {
  loading.value = true
  try {
    const response = (await request.delete(`/room-types/${id}`)) as any
    if (response.success) {
      ElMessage.success(response.message || '房型删除成功')
      await loadRoomTypes() // 重新加载数据
      return true
    } else {
      ElMessage.error(response.message || '房型删除失败')
      return false
    }
  } catch (error) {
    console.error('删除房型失败:', error)
    ElMessage.error('删除房型失败')
    return false
  } finally {
    loading.value = false
  }
}

const handleTabClick = (tab: string) => {
  activeTab.value = tab
  if (tab === '新增房型') {
    resetForm()
  }
}

const resetForm = () => {
  newRoomTypeForm.value = {
    name: '',
    description: '',
    pricingType: 'fixed',
    fixedPrice: 0.0,
    weekdayPrice: 0.0,
    weekendPrice: 0.0,
    roomCreationType: 'sync',
    roomPrefix: '',
    roomCount: 1,
    roomNumbers: [],
  }
}

const addNewRoom = () => {
  // 添加新的房间号输入框,如果有前缀则自动生成,否则为空
  if (newRoomTypeForm.value.roomPrefix) {
    const nextNumber = newRoomTypeForm.value.roomNumbers.length + 1
    const roomNumber = `${newRoomTypeForm.value.roomPrefix}${nextNumber.toString().padStart(2, '0')}`
    newRoomTypeForm.value.roomNumbers.push(roomNumber)
  } else {
    newRoomTypeForm.value.roomNumbers.push('')
  }
}

const removeRoom = (index: number) => {
  // 至少保留一个房间号输入框
  if (newRoomTypeForm.value.roomNumbers.length > 0) {
    newRoomTypeForm.value.roomNumbers.splice(index, 1)
  }
}

const handleCancel = () => {
  activeTab.value = '房间归属'
  resetForm()
}

const handleAddRoomType = async () => {
  if (!newRoomTypeForm.value.name.trim()) {
    ElMessage.warning('请输入房型名称')
    return
  }
  if (!newRoomTypeForm.value.description.trim()) {
    ElMessage.warning('请输入房型简称')
    return
  }
  if (
    newRoomTypeForm.value.roomCreationType === 'sync' &&
    newRoomTypeForm.value.roomNumbers.length === 0
  ) {
    ElMessage.warning('请至少添加一个房间')
    return
  }

  const roomTypeData = {
    name: newRoomTypeForm.value.name,
    code: newRoomTypeForm.value.name.substring(0, 3).toUpperCase(), // 简单生成代码
    description: newRoomTypeForm.value.description,
    totalRooms: newRoomTypeForm.value.roomCount,
    defaultPrice:
      newRoomTypeForm.value.pricingType === 'fixed' ? newRoomTypeForm.value.fixedPrice : null,
    weekdayPrice:
      newRoomTypeForm.value.pricingType === 'flexible' ? newRoomTypeForm.value.weekdayPrice : null,
    weekendPrice:
      newRoomTypeForm.value.pricingType === 'flexible' ? newRoomTypeForm.value.weekendPrice : null,
    roomNumbers:
      newRoomTypeForm.value.roomCreationType === 'sync' ? newRoomTypeForm.value.roomNumbers : [],
  }

  const success = await createRoomTypeApi(roomTypeData)
  if (success) {
    handleCancel()
  }
}

const handleAddRoomTypeWithDetails = async () => {
  await handleAddRoomType()
}

const handleEditRoomType = (roomType: RoomType) => {
  editingRoomType.value = roomType
  isEditing.value = true
}

// 从编辑器返回
const handleBackFromEditor = () => {
  isEditing.value = false
  editingRoomType.value = null
}

// 从基础信息编辑器返回
const handleBackFromBasicEditor = () => {
  isEditingBasicInfo.value = false
  editingBasicInfoRoomType.value = null
}

// 保存编辑的房型
const handleSaveRoomType = (data: any) => {
  const index = roomTypes.value.findIndex((r) => r.id === data.id)
  if (index !== -1) {
    // 更新房型数据
    roomTypes.value[index] = {
      ...roomTypes.value[index],
      name: data.externalName,
      description: data.description,
      defaultPrice: data.defaultPrice,
    }
  }
  handleBackFromEditor()
}

// 保存基础信息
const handleSaveBasicInfo = async (data: any) => {
  try {
    const roomTypeData = {
      name: data.name,
      code:
        roomTypes.value.find((r) => r.id === data.id)?.code ||
        data.name.substring(0, 3).toUpperCase(),
      description: data.shortName,
      totalRooms: data.roomCount,
      defaultPrice: data.pricingType === 'fixed' ? data.fixedPrice : null,
      weekdayPrice: data.pricingType === 'flexible' ? data.weekdayPrice : null,
      weekendPrice: data.pricingType === 'flexible' ? data.weekendPrice : null,
    }

    const response = (await request.put(`/room-types/${data.id}`, roomTypeData)) as any
    if (response.success) {
      ElMessage.success(response.message || '房型更新成功')
      // 重新加载数据以确保显示最新的后端数据
      await loadRoomTypes()
    } else {
      ElMessage.error(response.message || '房型更新失败')
    }
  } catch (error) {
    console.error('更新房型失败:', error)
    ElMessage.error('房型更新失败')
  }
  handleBackFromBasicEditor()
}

const handleDeleteRoomType = (roomType: RoomType) => {
  ElMessageBox.confirm(
    `确定要删除房型"${roomType.name}"吗？删除后该房型下的所有房间也将被删除。`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    },
  )
    .then(async () => {
      await deleteRoomTypeApi(roomType.id)
    })
    .catch(() => {
      // 用户取消删除
    })
}

const handleViewDetails = (roomType: RoomType) => {
  editingBasicInfoRoomType.value = roomType
  isEditingBasicInfo.value = true
}

const handleRoomSchedule = (roomType: RoomType) => {
  ElMessage.info(`查看房间排序：${roomType.name}`)
}

// 字符计数
const nameCount = computed(() => newRoomTypeForm.value.name.length)
const descriptionCount = computed(() => newRoomTypeForm.value.description.length)

// 组件挂载时加载数据
onMounted(() => {
  loadRoomTypes()
})
</script>

<template>
  <div class="room-type-management">
    <!-- 编辑房型页面 -->
    <RoomTypeEditor
      v-if="isEditing && editingRoomType"
      :room-type="editingRoomType"
      @back="handleBackFromEditor"
      @save="handleSaveRoomType"
    />

    <!-- 基础信息编辑页面 -->
    <RoomTypeBasicInfo
      v-else-if="isEditingBasicInfo && editingBasicInfoRoomType"
      :room-type="editingBasicInfoRoomType"
      @back="handleBackFromBasicEditor"
      @save="handleSaveBasicInfo"
    />

    <!-- 房型管理主页面 -->
    <div v-else class="main-content">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="tabs-container">
          <button
            v-for="tab in tabs"
            :key="tab"
            :class="['tab-button', { active: activeTab === tab }]"
            @click="handleTabClick(tab)"
          >
            {{ tab }}
          </button>
        </div>
      </div>

      <!-- 房间归属表格 -->
      <div v-if="activeTab === '房间归属'" class="table-container">
        <el-table
          :data="roomTypes"
          style="width: 100%"
          class="room-type-table"
          v-loading="loading"
          element-loading-text="加载房型数据中..."
        >
          <el-table-column prop="name" label="房型名称" width="180">
            <template #default="{ row }">
              <div class="room-type-cell">
                <div class="room-icon">
                  <img v-if="row.imageUrl" :src="row.imageUrl" alt="" />
                  <div v-else class="default-icon">🏠</div>
                </div>
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="description" label="简称" width="180" />

          <el-table-column prop="defaultPrice" label="默认门市价" width="120">
            <template #default="{ row }">
              ¥ {{ row.defaultPrice ? row.defaultPrice.toFixed(2) : '未设置' }}
            </template>
          </el-table-column>

          <el-table-column prop="roomCount" label="房间数" width="150">
            <template #default="{ row }">
              {{ row.roomCount || row.totalRooms || 0 }}
            </template>
          </el-table-column>

          <el-table-column prop="roomNumbers" label="房间号" width="150">
            <template #default="{ row }">
              {{ (row.roomNumbers || []).join(', ') }}
            </template>
          </el-table-column>

          <el-table-column prop="channel" label="上架渠道" width="150" />

          <el-table-column label="操作" width="350">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button link type="primary" @click="handleViewDetails(row)">
                  基础信息
                </el-button>
                <el-button link type="primary" @click="handleEditRoomType(row)"> 详情 </el-button>
                <el-button link type="primary" @click="handleRoomSchedule(row)">
                  房间排序
                </el-button>
                <el-button link type="danger" @click="handleDeleteRoomType(row)"> 删除 </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <div class="pagination-info">共 {{ total }} 条</div>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[25, 50, 100]"
            layout="sizes, prev, pager, next"
            @size-change="() => {}"
            @current-change="() => {}"
          />
        </div>
      </div>

      <!-- 导入房型 -->
      <div v-else-if="activeTab === '导入房型'" class="import-container">
        <div class="placeholder-content">
          <el-icon size="48" color="#d9d9d9"><Upload /></el-icon>
          <p>导入房型功能开发中...</p>
        </div>
      </div>

      <!-- 新增房型表单 -->
      <div v-else-if="activeTab === '新增房型'" class="form-container">
        <div class="form-content">
          <h3 class="form-title">新增房型</h3>

          <el-form :model="newRoomTypeForm" label-width="100px" class="room-type-form">
            <!-- 房型名称 -->
            <el-form-item label="房型名称" required>
              <div class="input-with-count">
                <el-input
                  v-model="newRoomTypeForm.name"
                  placeholder="请输入"
                  maxlength="30"
                  show-word-limit
                />
                <span class="char-count">{{ nameCount }} / 30</span>
              </div>
            </el-form-item>

            <!-- 房型简称 -->
            <el-form-item label="房型简称" required>
              <div class="input-with-count">
                <el-input
                  v-model="newRoomTypeForm.description"
                  placeholder="请输入"
                  maxlength="20"
                  show-word-limit
                />
                <span class="char-count">{{ descriptionCount }} / 20</span>
              </div>
            </el-form-item>

            <!-- 房间定价 -->
            <el-form-item label="房间定价">
              <div class="pricing-section">
                <el-radio-group v-model="newRoomTypeForm.pricingType" class="pricing-options">
                  <el-radio label="fixed">每日固定价</el-radio>
                  <el-radio label="flexible">区分平日、周末价</el-radio>
                </el-radio-group>

                <div class="price-inputs">
                  <div v-if="newRoomTypeForm.pricingType === 'fixed'" class="price-input">
                    <span class="currency">¥</span>
                    <el-input-number
                      v-model="newRoomTypeForm.fixedPrice"
                      :min="0"
                      :precision="2"
                      :step="0.01"
                      placeholder="0.00"
                      class="price-number"
                    />
                  </div>

                  <div v-else class="flexible-prices">
                    <div class="price-group">
                      <span class="price-label">平日价</span>
                      <span class="currency">¥</span>
                      <el-input-number
                        v-model="newRoomTypeForm.weekdayPrice"
                        :min="0"
                        :precision="2"
                        :step="0.01"
                        placeholder="0.00"
                        class="price-number"
                      />
                    </div>
                    <div class="price-group">
                      <span class="price-label">周末价</span>
                      <span class="currency">¥</span>
                      <el-input-number
                        v-model="newRoomTypeForm.weekendPrice"
                        :min="0"
                        :precision="2"
                        :step="0.01"
                        placeholder="0.00"
                        class="price-number"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </el-form-item>

            <!-- 房间信息 -->
            <el-form-item label="房间信息">
              <div class="room-info-section">
                <el-radio-group
                  v-model="newRoomTypeForm.roomCreationType"
                  class="room-creation-options"
                >
                  <el-radio label="sync">同步创建房间</el-radio>
                  <el-radio label="later">稍后统一创建</el-radio>
                </el-radio-group>

                <div v-if="newRoomTypeForm.roomCreationType === 'sync'" class="room-creation-form">
                  <div class="room-prefix-section">
                    <span class="section-label">房间号前缀</span>
                    <el-input
                      v-model="newRoomTypeForm.roomPrefix"
                      placeholder="请输入内容"
                      class="prefix-input"
                    />
                  </div>

                  <div class="room-count-section">
                    <span class="section-label">房间数量</span>
                    <el-input-number
                      v-model="newRoomTypeForm.roomCount"
                      :min="1"
                      class="count-input"
                    />
                    <span class="unit">间</span>
                  </div>

                  <div class="room-numbers-section">
                    <span class="section-label">房间号</span>
                    <div
                      v-if="newRoomTypeForm.roomNumbers.length === 0"
                      class="empty-room-hint"
                    >
                      房间删除后,将不对关联的订单也将无法操作"撤销退房"和"恢复预订"。
                    </div>
                    <div class="room-numbers-list">
                      <div
                        v-for="(_, index) in newRoomTypeForm.roomNumbers"
                        :key="index"
                        class="room-number-item"
                      >
                        <el-input
                          v-model="newRoomTypeForm.roomNumbers[index]"
                          placeholder="请输入房间号"
                          class="room-number-input"
                        />
                        <el-button
                          type="danger"
                          circle
                          size="small"
                          @click="removeRoom(index)"
                          class="remove-room-btn"
                        >
                          <el-icon><Delete /></el-icon>
                        </el-button>
                      </div>
                    </div>
                    <el-button type="primary" link @click="addNewRoom" class="add-room-btn">
                      <el-icon><Plus /></el-icon> 新增
                    </el-button>
                  </div>
                </div>
              </div>
            </el-form-item>
          </el-form>

          <!-- 底部操作按钮 -->
          <div class="form-footer">
            <el-button @click="handleCancel" class="cancel-btn">取消</el-button>
            <el-button type="primary" @click="handleAddRoomType" class="add-btn"
              >新增房型</el-button
            >
            <el-button type="primary" @click="handleAddRoomTypeWithDetails" class="add-detail-btn"
              >新增房型并完善信息</el-button
            >
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.room-type-management {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.main-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  background: white;
  border-bottom: 1px solid #e8e8e8;
}

.tabs-container {
  display: flex;
  padding: 0 20px;
}

.tab-button {
  padding: 16px 24px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
}

.tab-button:hover {
  color: #1890ff;
}

.tab-button.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
  background: #1890ff;
  color: white;
}

.table-container {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

.room-type-table {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
}

.room-type-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.room-icon {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.room-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-icon {
  font-size: 16px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-container {
  padding: 16px 20px;
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
}

.pagination-info {
  font-size: 14px;
  color: #666;
}

/* 导入房型占位 */
.import-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  gap: 16px;
}

/* 新增房型表单 */
.form-container {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

.form-content {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 8px;
  padding: 24px;
}

.form-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 24px 0;
}

.room-type-form {
  margin-bottom: 32px;
}

.room-type-form .el-form-item {
  margin-bottom: 24px;
}

.room-type-form .el-form-item__label {
  font-weight: 500;
  color: #333;
}

/* 输入框字符计数 */
.input-with-count {
  position: relative;
}

.char-count {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: #999;
  pointer-events: none;
}

/* 定价区域 */
.pricing-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pricing-options {
  display: flex;
  gap: 24px;
}

.price-inputs {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.price-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.flexible-prices {
  display: flex;
  gap: 24px;
}

.price-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-label {
  font-size: 14px;
  color: #666;
  min-width: 60px;
}

.currency {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.price-number {
  width: 120px;
}

/* 房间信息区域 */
.room-info-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.room-creation-options {
  display: flex;
  gap: 24px;
}

.room-creation-form {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.room-prefix-section,
.room-count-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-label {
  font-size: 14px;
  color: #333;
  min-width: 80px;
}

.prefix-input {
  width: 200px;
}

.count-input {
  width: 120px;
}

.unit {
  font-size: 14px;
  color: #666;
}

.room-numbers-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-room-hint {
  color: #909399;
  font-size: 12px;
  padding: 8px 0;
  line-height: 1.5;
}

.room-numbers-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.room-number-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.room-number-input {
  flex: 1;
  max-width: 300px;
}

.remove-room-btn {
  flex-shrink: 0;
}

.add-room-btn {
  align-self: flex-start;
  font-size: 14px;
  padding: 0;
}

/* 底部操作按钮 */
.form-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 24px;
  border-top: 1px solid #e8e8e8;
}

.cancel-btn {
  min-width: 100px;
}

.add-btn,
.add-detail-btn {
  min-width: 120px;
}

.add-detail-btn {
  background: #1890ff;
  border-color: #1890ff;
}
</style>
