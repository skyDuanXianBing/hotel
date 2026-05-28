<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
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
  maxGuests?: number
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

type RoomTypeTab = 'ownership' | 'import' | 'add'

const { t } = useI18n()
const roomTypes = ref<RoomType[]>([])

const currentPage = ref(1)
const pageSize = ref(25)
const total = computed(() => roomTypes.value.length)
const loading = ref(false)

const activeTab = ref<RoomTypeTab>('ownership')
const tabs = [
  { key: 'ownership', label: 'settingsStage4.roomTypeManagement.tabs.ownership' },
  { key: 'import', label: 'settingsStage4.roomTypeManagement.tabs.importRoomTypes' },
  { key: 'add', label: 'settingsStage4.roomTypeManagement.tabs.addRoomType' },
] as const

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
        maxGuests: item.maxGuests,
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
      ElMessage.error(response.message || t('settingsStage4.roomTypeManagement.messages.loadFailed'))
    }
  } catch (error) {
    console.error('获取房型列表失败:', error)
    ElMessage.error(t('settingsStage4.roomTypeManagement.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

const createRoomTypeApi = async (roomTypeData: any) => {
  loading.value = true
  try {
    const response = (await request.post('/room-types', roomTypeData)) as any
    if (response.success) {
      ElMessage.success(response.message || t('settingsStage4.roomTypeManagement.messages.createSuccess'))
      await loadRoomTypes() // 重新加载数据
      return true
    } else {
      ElMessage.error(response.message || t('settingsStage4.roomTypeManagement.messages.createFailed'))
      return false
    }
  } catch (error) {
    console.error('创建房型失败:', error)
    ElMessage.error(t('settingsStage4.roomTypeManagement.messages.createFailed'))
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
      ElMessage.success(response.message || t('settingsStage4.roomTypeManagement.messages.deleteSuccess'))
      await loadRoomTypes() // 重新加载数据
      return true
    } else {
      ElMessage.error(response.message || t('settingsStage4.roomTypeManagement.messages.deleteFailed'))
      return false
    }
  } catch (error) {
    console.error('删除房型失败:', error)
    ElMessage.error(t('settingsStage4.roomTypeManagement.messages.deleteFailed'))
    return false
  } finally {
    loading.value = false
  }
}

const handleTabClick = (tab: RoomTypeTab) => {
  activeTab.value = tab
  if (tab === 'add') {
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
  activeTab.value = 'ownership'
  resetForm()
}

const handleAddRoomType = async () => {
  if (!newRoomTypeForm.value.name.trim()) {
    ElMessage.warning(t('settingsStage4.roomTypeManagement.messages.nameRequired'))
    return
  }
  if (!newRoomTypeForm.value.description.trim()) {
    ElMessage.warning(t('settingsStage4.roomTypeManagement.messages.shortNameRequired'))
    return
  }
  if (
    newRoomTypeForm.value.roomCreationType === 'sync' &&
    newRoomTypeForm.value.roomNumbers.length === 0
  ) {
    ElMessage.warning(t('settingsStage4.roomTypeManagement.messages.roomRequired'))
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
    const currentRoomType =
      editingBasicInfoRoomType.value ?? roomTypes.value.find((r) => r.id === data.id) ?? null
    if (!currentRoomType?.code) {
      ElMessage.error(t('settingsStage4.roomTypeManagement.messages.codeMissing'))
      return
    }
    if (currentRoomType.maxGuests == null) {
      ElMessage.error(t('settingsStage4.roomTypeManagement.messages.maxGuestsMissing'))
      return
    }

    const roomTypeData = {
      name: data.name,
      code: currentRoomType.code,
      description: data.shortName,
      totalRooms: data.roomCount,
      maxGuests: currentRoomType.maxGuests,
      defaultPrice: data.pricingType === 'fixed' ? data.fixedPrice : null,
      weekdayPrice: data.pricingType === 'flexible' ? data.weekdayPrice : null,
      weekendPrice: data.pricingType === 'flexible' ? data.weekendPrice : null,
    }

    const response = (await request.put(`/room-types/${data.id}`, roomTypeData)) as any
    if (response.success) {
      ElMessage.success(response.message || t('settingsStage4.roomTypeManagement.messages.updateSuccess'))
      // 重新加载数据以确保显示最新的后端数据
      await loadRoomTypes()
    } else {
      ElMessage.error(response.message || t('settingsStage4.roomTypeManagement.messages.updateFailed'))
    }
  } catch (error) {
    console.error('更新房型失败:', error)
    ElMessage.error(t('settingsStage4.roomTypeManagement.messages.updateFailed'))
  }
  handleBackFromBasicEditor()
}

const handleDeleteRoomType = (roomType: RoomType) => {
  ElMessageBox.confirm(
    t('settingsStage4.roomTypeManagement.messages.deleteConfirm', { name: roomType.name }),
    t('settings.common.deleteConfirmTitle'),
    {
      confirmButtonText: t('settings.common.confirmButton'),
      cancelButtonText: t('settings.common.cancelButton'),
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
  ElMessage.info(t('settingsStage4.roomTypeManagement.messages.viewRoomSort', { name: roomType.name }))
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
            :key="tab.key"
            :class="['tab-button', { active: activeTab === tab.key }]"
            @click="handleTabClick(tab.key)"
          >
            {{ t(tab.label) }}
          </button>
        </div>
      </div>

      <!-- 房间归属表格 -->
      <div v-if="activeTab === 'ownership'" class="table-container">
        <el-table
          :data="roomTypes"
          style="width: 100%"
          class="room-type-table"
          v-loading="loading"
          :element-loading-text="t('settingsStage4.roomTypeManagement.messages.loading')"
        >
          <el-table-column prop="name" :label="t('settingsStage4.roomTypeManagement.columns.roomTypeName')" width="180">
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

          <el-table-column prop="description" :label="t('settingsStage4.roomTypeManagement.columns.shortName')" width="180" />

          <el-table-column prop="defaultPrice" :label="t('settingsStage4.roomTypeManagement.columns.defaultWalkInRate')" width="120">
            <template #default="{ row }">
              ¥ {{ row.defaultPrice ? row.defaultPrice.toFixed(2) : t('settings.common.none') }}
            </template>
          </el-table-column>

          <el-table-column prop="roomCount" :label="t('settingsStage4.roomTypeManagement.columns.roomCount')" width="150">
            <template #default="{ row }">
              {{ row.roomCount || row.totalRooms || 0 }}
            </template>
          </el-table-column>

          <el-table-column prop="roomNumbers" :label="t('settingsStage4.roomTypeManagement.columns.roomNumbers')" width="150">
            <template #default="{ row }">
              {{ (row.roomNumbers || []).join(', ') }}
            </template>
          </el-table-column>

          <el-table-column prop="channel" :label="t('settingsStage4.roomTypeManagement.columns.listedChannels')" width="150" />

          <el-table-column :label="t('settings.common.actions')" width="350">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button link type="primary" @click="handleViewDetails(row)">
                  {{ t('settingsStage4.roomTypeManagement.actions.basicInfo') }}
                </el-button>
                <el-button link type="primary" @click="handleEditRoomType(row)">
                  {{ t('settingsStage4.common.details') }}
                </el-button>
                <el-button link type="primary" @click="handleRoomSchedule(row)">
                  {{ t('settingsStage4.roomTypeManagement.actions.roomSort') }}
                </el-button>
                <el-button link type="danger" @click="handleDeleteRoomType(row)">
                  {{ t('settings.common.delete') }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <div class="pagination-info">
            {{ t('settingsStage4.common.itemsTotal', { count: total }) }}
          </div>
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
      <div v-else-if="activeTab === 'import'" class="import-container">
        <div class="placeholder-content">
          <el-icon size="48" color="#d9d9d9"><Upload /></el-icon>
          <p>{{ t('settingsStage4.roomTypeManagement.messages.importInDevelopment') }}</p>
        </div>
      </div>

      <!-- 新增房型表单 -->
      <div v-else-if="activeTab === 'add'" class="form-container">
        <div class="form-content">
          <h3 class="form-title">{{ t('settingsStage4.roomTypeManagement.tabs.addRoomType') }}</h3>

          <el-form :model="newRoomTypeForm" label-width="100px" class="room-type-form">
            <!-- 房型名称 -->
            <el-form-item :label="t('settingsStage4.roomTypeManagement.fields.roomTypeName')" required>
              <div class="input-with-count">
                <el-input
                  v-model="newRoomTypeForm.name"
                  :placeholder="t('settingsStage4.roomTypeManagement.placeholders.input')"
                  maxlength="30"
                  show-word-limit
                />
                <span class="char-count">{{ nameCount }} / 30</span>
              </div>
            </el-form-item>

            <!-- 房型简称 -->
            <el-form-item :label="t('settingsStage4.roomTypeManagement.fields.shortName')" required>
              <div class="input-with-count">
                <el-input
                  v-model="newRoomTypeForm.description"
                  :placeholder="t('settingsStage4.roomTypeManagement.placeholders.input')"
                  maxlength="20"
                  show-word-limit
                />
                <span class="char-count">{{ descriptionCount }} / 20</span>
              </div>
            </el-form-item>

            <!-- 房间定价 -->
            <el-form-item :label="t('settingsStage4.roomTypeManagement.fields.roomPricing')">
              <div class="pricing-section">
                <el-radio-group v-model="newRoomTypeForm.pricingType" class="pricing-options">
                  <el-radio label="fixed">{{ t('settingsStage4.roomTypeManagement.options.fixedDailyPrice') }}</el-radio>
                  <el-radio label="flexible">{{ t('settingsStage4.roomTypeManagement.options.weekdayWeekendPrice') }}</el-radio>
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
                      <span class="price-label">{{ t('settingsStage4.roomTypeManagement.fields.weekdayPrice') }}</span>
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
                      <span class="price-label">{{ t('settingsStage4.roomTypeManagement.fields.weekendPrice') }}</span>
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
            <el-form-item :label="t('settingsStage4.roomTypeManagement.fields.roomInfo')">
              <div class="room-info-section">
                <el-radio-group
                  v-model="newRoomTypeForm.roomCreationType"
                  class="room-creation-options"
                >
                  <el-radio label="sync">{{ t('settingsStage4.roomTypeManagement.options.syncCreateRooms') }}</el-radio>
                  <el-radio label="later">{{ t('settingsStage4.roomTypeManagement.options.createLater') }}</el-radio>
                </el-radio-group>

                <div v-if="newRoomTypeForm.roomCreationType === 'sync'" class="room-creation-form">
                  <div class="room-prefix-section">
                    <span class="section-label">{{ t('settingsStage4.roomTypeManagement.fields.roomNumberPrefix') }}</span>
                    <el-input
                      v-model="newRoomTypeForm.roomPrefix"
                      :placeholder="t('settingsStage4.roomTypeManagement.placeholders.inputContent')"
                      class="prefix-input"
                    />
                  </div>

                  <div class="room-count-section">
                    <span class="section-label">{{ t('settingsStage4.roomTypeManagement.fields.roomCount') }}</span>
                    <el-input-number
                      v-model="newRoomTypeForm.roomCount"
                      :min="1"
                      class="count-input"
                    />
                    <span class="unit">{{ t('settingsStage4.roomTypeManagement.units.rooms') }}</span>
                  </div>

                  <div class="room-numbers-section">
                    <span class="section-label">{{ t('settingsStage4.roomTypeManagement.fields.roomNumbers') }}</span>
                    <div
                      v-if="newRoomTypeForm.roomNumbers.length === 0"
                      class="empty-room-hint"
                    >
                      {{ t('settingsStage4.roomTypeManagement.hints.roomDeleteImpact') }}
                    </div>
                    <div class="room-numbers-list">
                      <div
                        v-for="(_, index) in newRoomTypeForm.roomNumbers"
                        :key="index"
                        class="room-number-item"
                      >
                        <el-input
                          v-model="newRoomTypeForm.roomNumbers[index]"
                          :placeholder="t('settingsStage4.roomTypeManagement.placeholders.roomNumber')"
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
                      <el-icon><Plus /></el-icon> {{ t('settings.common.add') }}
                    </el-button>
                  </div>
                </div>
              </div>
            </el-form-item>
          </el-form>

          <!-- 底部操作按钮 -->
          <div class="form-footer">
            <el-button @click="handleCancel" class="cancel-btn">{{ t('settings.common.cancel') }}</el-button>
            <el-button type="primary" @click="handleAddRoomType" class="add-btn"
              >{{ t('settingsStage4.roomTypeManagement.actions.addRoomType') }}</el-button
            >
            <el-button type="primary" @click="handleAddRoomTypeWithDetails" class="add-detail-btn"
              >{{ t('settingsStage4.roomTypeManagement.actions.addAndComplete') }}</el-button
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
