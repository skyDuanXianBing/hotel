<template>
  <div class="cleaning-settings-container">
    <!-- 列表视图 -->
    <div v-if="!showConfigPage" class="stores-view">
      <!-- 顶部提示 -->
      <div class="notice-banner">
        <span>在配置之前,请先去设置门店资料</span>
      </div>

      <!-- 门店卡片网格 -->
      <div class="stores-grid">
        <div
          v-for="store in stores"
          :key="store.id"
          class="store-card"
        >
          <div class="card-header">
            <el-icon class="collapse-icon"><Minus /></el-icon>
            <h3 class="store-name">{{ store.name }}</h3>
          </div>
          <div class="card-content">
            <el-button
              type="primary"
              class="config-button"
              @click="handleConfig(store)"
            >
              配置
            </el-button>
            <div class="status-indicator">
              <span class="status-dot" :class="store.enabled ? 'enabled' : 'disabled'"></span>
              <span class="status-text">{{ store.enabled ? '开启' : '禁用' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 配置详情视图 -->
    <div v-else class="config-view">
      <!-- 标签页 -->
      <div class="tabs-header">
        <div class="tabs-nav">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            class="tab-item"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </div>
        </div>
        <div class="header-actions">
          <span class="switch-label">开启</span>
          <el-switch v-model="currentConfig.enabled" />
        </div>
      </div>

      <!-- 任务时间标签页 -->
      <div v-if="activeTab === 'task-time'" class="tab-content">
        <!-- 顶部提示 -->
        <div class="info-notice">
          <span>1.在配置之前,请先去设置门店详情资料。2.该配置系统自动生成保洁任务对任务有效时间。</span>
        </div>

        <div class="action-bar">
          <el-button type="primary" @click="handleEdit">编辑</el-button>
        </div>

        <!-- 任务时间设置 -->
        <div class="settings-section">
          <h3 class="section-title">任务时间设置</h3>

          <div class="time-setting-item">
            <label class="setting-label">续住房</label>
            <div class="time-range">
              <el-time-select
                v-model="currentConfig.stayStartTime"
                :disabled="!isEditing"
                start="06:00"
                step="00:30"
                end="23:30"
                placeholder="选择时间"
              />
              <span class="time-separator">至</span>
              <el-time-select
                v-model="currentConfig.stayEndTime"
                :disabled="!isEditing"
                start="06:00"
                step="00:30"
                end="23:30"
                placeholder="选择时间"
              />
            </div>
            <p class="setting-desc">从客人入住的第二天起,直到退房前一天,每天生成续住任务</p>
          </div>

          <div class="time-setting-item">
            <label class="setting-label">退房</label>
            <div class="time-range">
              <el-time-select
                v-model="currentConfig.checkoutStartTime"
                :disabled="!isEditing"
                start="06:00"
                step="00:30"
                end="23:30"
                placeholder="选择时间"
              />
              <span class="time-separator">至</span>
              <el-time-select
                v-model="currentConfig.checkoutEndTime"
                :disabled="!isEditing"
                start="06:00"
                step="00:30"
                end="23:30"
                placeholder="选择时间"
              />
            </div>
            <p class="setting-desc">在客人退房当天生成退房保洁任务</p>
          </div>
        </div>

        <!-- 保洁任务生成设置 -->
        <div class="settings-section">
          <h3 class="section-title">保洁任务生成设置</h3>

          <div class="switch-setting-item">
            <div class="switch-content">
              <label class="setting-label">续住</label>
              <p class="setting-desc">当开关打开时,系统会自动创建续住保洁任务</p>
            </div>
            <el-switch v-model="currentConfig.autoStayTask" :disabled="!isEditing" />
          </div>

          <div class="switch-setting-item">
            <div class="switch-content">
              <label class="setting-label">转退房</label>
              <p class="setting-desc">当开关打开时,系统会自动为转退房创建保洁任务</p>
            </div>
            <el-switch v-model="currentConfig.autoCheckoutTask" :disabled="!isEditing" />
          </div>
        </div>

        <!-- 编辑模式下的保存按钮 -->
        <div v-if="isEditing" class="edit-actions">
          <el-button @click="handleCancelEdit">取消</el-button>
          <el-button type="primary" @click="handleSaveEdit">保存</el-button>
        </div>
      </div>

      <!-- 保洁员标签页 -->
      <div v-if="activeTab === 'cleaners'" class="tab-content">
        <div class="table-toolbar">
          <el-input
            v-model="cleanerSearchText"
            placeholder="搜索姓名、邮箱"
            class="search-input-wide"
            clearable
          />
          <el-button type="primary" @click="handleAddCleaner">添加保洁员</el-button>
        </div>

        <el-table
          :data="filteredCleaners"
          border
          stripe
          class="data-table"
        >
          <el-table-column prop="name" label="姓名" min-width="150" />
          <el-table-column prop="email" label="邮箱" min-width="250" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="handleDeleteCleaner(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 易耗品标签页 -->
      <div v-if="activeTab === 'supplies'" class="tab-content">
        <div class="table-toolbar">
          <div class="filter-group">
            <span class="filter-label">房型:</span>
            <el-select v-model="selectedRoomType" placeholder="请选择房型" style="width: 200px">
              <el-option
                v-for="roomType in roomTypes"
                :key="roomType"
                :label="roomType"
                :value="roomType"
              />
            </el-select>
          </div>
        </div>

        <el-table :data="filteredSupplies" border stripe class="data-table">
          <el-table-column prop="roomType" label="房型" min-width="150" />
          <el-table-column prop="supplies" label="易耗品" min-width="200" />
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEditSupply(row)">编辑</el-button>
              <el-button link type="danger" @click="handleClearSupply(row)">清空</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 添加保洁员对话框 -->
    <el-dialog
      v-model="showCleanerDialog"
      title="添加保洁员"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="cleanerForm" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="cleanerForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="cleanerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCleanerDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSaveCleaner">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑易耗品对话框 -->
    <el-dialog
      v-model="showSupplyDialog"
      title="编辑易耗品"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form v-if="currentSupply" :model="currentSupply" label-width="80px">
        <el-form-item label="房型">
          <el-input v-model="currentSupply.roomType" disabled />
        </el-form-item>
        <el-form-item label="易耗品">
          <el-input
            v-model="currentSupply.supplies"
            type="textarea"
            :rows="4"
            placeholder="请输入易耗品信息,多个易耗品请用逗号分隔"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showSupplyDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSaveSupply">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Minus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getOrCreateCleaningConfig,
  updateCleaningConfig,
  getCleanersByUserIdAndStoreId,
  createCleaner,
  deleteCleaner,
  getCleaningSuppliesByUserId,
  createCleaningSupply,
  updateCleaningSupply,
  clearCleaningSupply,
} from '@/api/cleaning'
import { getAllStores, type StoreDTO } from '@/api/store'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'

interface Store {
  id: number
  name: string
  enabled: boolean
}

interface CleaningConfig {
  id?: number
  enabled: boolean
  stayStartTime: string
  stayEndTime: string
  checkoutStartTime: string
  checkoutEndTime: string
  autoStayTask: boolean
  autoCheckoutTask: boolean
}

interface Cleaner {
  id: number
  name: string
  email: string
}

interface Supply {
  id?: number
  roomType: string
  supplies: string
}

const loading = ref(false)
const showConfigPage = ref(false)
const currentStore = ref<Store | null>(null)
const activeTab = ref('task-time')
const isEditing = ref(false)

const tabs = [
  { key: 'task-time', label: '任务时间' },
  { key: 'cleaners', label: '保洁员' },
  { key: 'supplies', label: '易耗品' },
]

const stores = ref<Store[]>([])

const currentConfig = reactive<CleaningConfig>({
  enabled: true,
  stayStartTime: '12:00',
  stayEndTime: '15:00',
  checkoutStartTime: '10:00',
  checkoutEndTime: '16:00',
  autoStayTask: false,
  autoCheckoutTask: false,
})

// 备份配置用于取消编辑
const configBackup = ref<CleaningConfig | null>(null)

// 保洁员相关
const cleanerSearchText = ref('')
const cleaners = ref<Cleaner[]>([])
const showCleanerDialog = ref(false)
const cleanerForm = reactive({
  id: 0,
  name: '',
  email: '',
})

// 易耗品相关
const selectedRoomType = ref('全部')
const roomTypes = ref<string[]>(['全部'])
const supplies = ref<Supply[]>([])
const showSupplyDialog = ref(false)
const currentSupply = ref<Supply | null>(null)

// 加载门店列表
const loadStores = async () => {
  try {
    loading.value = true
    const response = await getAllStores()
    if (response.success && response.data) {
      // 将门店数据映射到本地 Store 接口
      stores.value = response.data.map((store: StoreDTO) => ({
        id: store.id,
        name: store.name,
        enabled: true, // 默认为启用状态,后续可以根据保洁配置的 enabled 字段来设置
      }))
    } else {
      ElMessage.error(response.message || '加载门店列表失败')
    }
  } catch (error) {
    console.error('加载门店列表失败:', error)
    ElMessage.error('加载门店列表失败')
  } finally {
    loading.value = false
  }
}

// 加载房型列表
const loadRoomTypes = async () => {
  try {
    loading.value = true
    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      const roomTypeNames = response.data.map((rt: RoomTypeDTO) => rt.name)
      roomTypes.value = ['全部', ...roomTypeNames]
    } else {
      ElMessage.error(response.message || '加载房型列表失败')
    }
  } catch (error) {
    console.error('加载房型列表失败:', error)
    ElMessage.error('加载房型列表失败')
  } finally {
    loading.value = false
  }
}

// 加载保洁配置
const loadCleaningConfig = async (storeId: number) => {
  try {
    loading.value = true
    const response = await getOrCreateCleaningConfig(1, storeId) // 用户ID=1
    if (response.success && response.data) {
      const data = response.data
      currentConfig.id = data.id
      currentConfig.enabled = data.enabled
      currentConfig.stayStartTime = data.stayStartTime
      currentConfig.stayEndTime = data.stayEndTime
      currentConfig.checkoutStartTime = data.checkoutStartTime
      currentConfig.checkoutEndTime = data.checkoutEndTime
      currentConfig.autoStayTask = data.autoStayTask
      currentConfig.autoCheckoutTask = data.autoCheckoutTask
    }
  } catch (error) {
    console.error('加载保洁配置失败:', error)
    ElMessage.error('加载保洁配置失败')
  } finally {
    loading.value = false
  }
}

// 加载保洁员列表
const loadCleaners = async (storeId: number) => {
  try {
    loading.value = true
    const response = await getCleanersByUserIdAndStoreId(1, storeId)
    if (response.success && response.data) {
      cleaners.value = response.data
    }
  } catch (error) {
    console.error('加载保洁员列表失败:', error)
    ElMessage.error('加载保洁员列表失败')
  } finally {
    loading.value = false
  }
}

// 加载易耗品列表
const loadSupplies = async () => {
  try {
    loading.value = true
    const response = await getCleaningSuppliesByUserId(1)
    if (response.success && response.data) {
      supplies.value = response.data
    }
  } catch (error) {
    console.error('加载易耗品列表失败:', error)
    ElMessage.error('加载易耗品列表失败')
  } finally {
    loading.value = false
  }
}

const handleConfig = async (store: Store) => {
  currentStore.value = store
  showConfigPage.value = true
  activeTab.value = 'task-time'
  isEditing.value = false

  // 加载数据
  await loadCleaningConfig(store.id)
  await loadCleaners(store.id)
  await loadSupplies()
}

const handleEdit = () => {
  // 备份当前配置
  configBackup.value = { ...currentConfig }
  isEditing.value = true
}

const handleCancelEdit = () => {
  // 恢复备份的配置
  if (configBackup.value) {
    Object.assign(currentConfig, configBackup.value)
  }
  isEditing.value = false
  configBackup.value = null
}

const handleSaveEdit = async () => {
  if (!currentConfig.id) {
    ElMessage.error('配置ID不存在')
    return
  }

  try {
    loading.value = true
    const response = await updateCleaningConfig(currentConfig.id, {
      enabled: currentConfig.enabled,
      stayStartTime: currentConfig.stayStartTime,
      stayEndTime: currentConfig.stayEndTime,
      checkoutStartTime: currentConfig.checkoutStartTime,
      checkoutEndTime: currentConfig.checkoutEndTime,
      autoStayTask: currentConfig.autoStayTask,
      autoCheckoutTask: currentConfig.autoCheckoutTask,
    })

    if (response.success) {
      ElMessage.success('保存成功')
      isEditing.value = false
      configBackup.value = null
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

// 保洁员相关函数
const filteredCleaners = computed(() => {
  if (!cleanerSearchText.value.trim()) {
    return cleaners.value
  }
  const searchText = cleanerSearchText.value.toLowerCase()
  return cleaners.value.filter(
    (cleaner) =>
      cleaner.name.toLowerCase().includes(searchText) ||
      cleaner.email.toLowerCase().includes(searchText)
  )
})

const handleAddCleaner = () => {
  cleanerForm.id = 0
  cleanerForm.name = ''
  cleanerForm.email = ''
  showCleanerDialog.value = true
}

const handleDeleteCleaner = (row: Cleaner) => {
  ElMessageBox.confirm(`确定要删除保洁员 "${row.name}" 吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        loading.value = true
        const response = await deleteCleaner(row.id)
        if (response.success) {
          ElMessage.success('删除成功')
          // 重新加载保洁员列表
          if (currentStore.value) {
            await loadCleaners(currentStore.value.id)
          }
        } else {
          ElMessage.error(response.message || '删除失败')
        }
      } catch (error) {
        console.error('删除保洁员失败:', error)
        ElMessage.error('删除失败')
      } finally {
        loading.value = false
      }
    })
    .catch(() => {
      // 用户取消
    })
}

const handleSaveCleaner = async () => {
  if (!cleanerForm.name.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (!cleanerForm.email.trim()) {
    ElMessage.warning('请输入邮箱')
    return
  }
  if (!currentStore.value) {
    ElMessage.error('未选择门店')
    return
  }

  try {
    loading.value = true
    const response = await createCleaner({
      userId: 1,
      storeId: currentStore.value.id,
      name: cleanerForm.name.trim(),
      email: cleanerForm.email.trim(),
    })

    if (response.success) {
      ElMessage.success('添加成功')
      showCleanerDialog.value = false
      // 重新加载保洁员列表
      await loadCleaners(currentStore.value.id)
    } else {
      ElMessage.error(response.message || '添加失败')
    }
  } catch (error) {
    console.error('添加保洁员失败:', error)
    ElMessage.error('添加失败')
  } finally {
    loading.value = false
  }
}

// 易耗品相关函数
// 组合房型和易耗品数据 - 以房型为主,显示每个房型的易耗品配置
const roomTypeSupplies = computed(() => {
  // 获取所有房型(排除"全部"选项)
  const allRoomTypes = roomTypes.value.filter(rt => rt !== '全部')

  // 为每个房型创建一条记录,包含其易耗品配置
  return allRoomTypes.map(roomTypeName => {
    // 查找该房型对应的易耗品配置
    const supply = supplies.value.find(s => s.roomType === roomTypeName)
    return {
      id: supply?.id,
      roomType: roomTypeName,
      supplies: supply?.supplies || '-'
    }
  })
})

const filteredSupplies = computed(() => {
  if (selectedRoomType.value === '全部') {
    return roomTypeSupplies.value
  }
  return roomTypeSupplies.value.filter((s) => s.roomType === selectedRoomType.value)
})

const handleEditSupply = (row: Supply) => {
  currentSupply.value = { ...row }
  showSupplyDialog.value = true
}

const handleClearSupply = (row: Supply) => {
  // 如果该房型没有易耗品配置,无需清空
  if (!row.id) {
    ElMessage.warning('该房型尚未配置易耗品')
    return
  }

  ElMessageBox.confirm(`确定要清空 "${row.roomType}" 的易耗品设置吗?`, '清空确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        loading.value = true
        const response = await clearCleaningSupply(row.id!)
        if (response.success) {
          ElMessage.success('清空成功')
          // 重新加载易耗品列表
          await loadSupplies()
        } else {
          ElMessage.error(response.message || '清空失败')
        }
      } catch (error) {
        console.error('清空易耗品失败:', error)
        ElMessage.error('清空失败')
      } finally {
        loading.value = false
      }
    })
    .catch(() => {
      // 用户取消
    })
}

const handleSaveSupply = async () => {
  if (!currentSupply.value) return

  try {
    loading.value = true
    let response

    // 如果有 id,则更新;否则创建新记录
    if (currentSupply.value.id) {
      response = await updateCleaningSupply(currentSupply.value.id, {
        userId: 1,
        roomType: currentSupply.value.roomType,
        supplies: currentSupply.value.supplies,
      })
    } else {
      response = await createCleaningSupply({
        userId: 1,
        roomType: currentSupply.value.roomType,
        supplies: currentSupply.value.supplies,
      })
    }

    if (response.success) {
      ElMessage.success('保存成功')
      showSupplyDialog.value = false
      currentSupply.value = null
      // 重新加载易耗品列表
      await loadSupplies()
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存易耗品失败:', error)
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

// 组件加载时获取门店列表和房型列表
onMounted(() => {
  loadStores()
  loadRoomTypes()
})
</script>

<style scoped>
.cleaning-settings-container {
  background: #f5f7fa;
  min-height: calc(100vh - 100px);
}

/* 列表视图样式 */
.stores-view {
  padding: 20px;
}

.notice-banner {
  background: #e8e8e8;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #333;
}

.stores-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.store-card {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.collapse-icon {
  font-size: 18px;
  color: #409eff;
}

.store-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin: 0;
  flex: 1;
}

.card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.config-button {
  flex: 1;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.enabled {
  background: #409eff;
}

.status-dot.disabled {
  background: #909399;
}

.status-text {
  font-size: 14px;
  color: #606266;
}

/* 配置视图样式 */
.config-view {
  background: white;
  min-height: calc(100vh - 100px);
}

.tabs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
}

.tabs-nav {
  display: flex;
  gap: 0;
}

.tab-item {
  padding: 16px 20px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.tab-item:hover {
  color: #409eff;
}

.tab-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 500;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.switch-label {
  font-size: 14px;
  color: #606266;
}

.tab-content {
  padding: 20px;
}

.info-notice {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #1890ff;
  line-height: 1.6;
}

.action-bar {
  margin-bottom: 24px;
  text-align: right;
}

.settings-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
}

.time-setting-item {
  margin-bottom: 24px;
}

.setting-label {
  display: block;
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
  font-weight: 500;
}

.time-range {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.time-separator {
  font-size: 14px;
  color: #909399;
}

.setting-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
  line-height: 1.5;
}

.switch-setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.switch-setting-item:last-child {
  border-bottom: none;
}

.switch-content {
  flex: 1;
}

.edit-actions {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e4e7ed;
  text-align: right;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.empty-content {
  padding: 60px 20px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

:deep(.el-time-select) {
  width: 150px;
}

/* 表格工具栏 */
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
}

.search-input-wide {
  flex: 1;
  max-width: 600px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* 数据表格 */
.data-table {
  width: 100%;
}

.data-table :deep(.el-table__header) {
  background-color: #f5f7fa;
}

/* 空状态 */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  background: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
}

.empty-text {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* 对话框 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
