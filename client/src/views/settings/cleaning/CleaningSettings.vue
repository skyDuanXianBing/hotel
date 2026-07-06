<template>
  <div class="cleaning-settings-container">
    <!-- 列表视图 -->
    <div v-if="!showConfigPage" class="stores-view">
      <!-- 顶部提示 -->
      <div class="notice-banner">
        <span>{{ t('settingsStage4.cleaningSettings.notices.storeProfileFirst') }}</span>
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
              {{ t('settingsStage4.cleaningSettings.actions.configure') }}
            </el-button>
            <div class="status-indicator">
              <span class="status-dot" :class="store.enabled ? 'enabled' : 'disabled'"></span>
              <span class="status-text">{{ store.enabled ? t('settingsStage4.cleaningSettings.status.enabled') : t('settingsStage4.cleaningSettings.status.disabled') }}</span>
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
          <span class="switch-label">{{ t('settingsStage4.cleaningSettings.status.enabled') }}</span>
          <el-switch v-model="currentConfig.enabled" />
        </div>
      </div>

      <!-- 任务时间标签页 -->
      <div v-if="activeTab === 'task-time'" class="tab-content">
        <!-- 顶部提示 -->
        <div class="info-notice">
          <span>{{ t('settingsStage4.cleaningSettings.notices.taskTime') }}</span>
        </div>

        <div class="action-bar">
          <el-button type="primary" @click="handleEdit">{{ t('settings.common.edit') }}</el-button>
        </div>

        <!-- 任务时间设置 -->
        <div class="settings-section">
          <h3 class="section-title">{{ t('settingsStage4.cleaningSettings.sections.taskTime') }}</h3>

          <div class="time-setting-item">
            <label class="setting-label">{{ t('settingsStage4.cleaningSettings.fields.stayRoom') }}</label>
            <div class="time-range">
              <el-time-select
                v-model="currentConfig.stayStartTime"
                :disabled="!isEditing"
                start="06:00"
                step="00:30"
                end="23:30"
                :placeholder="t('settingsStage4.cleaningSettings.placeholders.selectTime')"
              />
              <span class="time-separator">{{ t('settingsStage4.cleaningSettings.to') }}</span>
              <el-time-select
                v-model="currentConfig.stayEndTime"
                :disabled="!isEditing"
                start="06:00"
                step="00:30"
                end="23:30"
                :placeholder="t('settingsStage4.cleaningSettings.placeholders.selectTime')"
              />
            </div>
            <p class="setting-desc">{{ t('settingsStage4.cleaningSettings.descriptions.stayRoom') }}</p>
          </div>

          <div class="time-setting-item">
            <label class="setting-label">{{ t('settingsStage4.cleaningSettings.fields.checkout') }}</label>
            <div class="time-range">
              <el-time-select
                v-model="currentConfig.checkoutStartTime"
                :disabled="!isEditing"
                start="06:00"
                step="00:30"
                end="23:30"
                :placeholder="t('settingsStage4.cleaningSettings.placeholders.selectTime')"
              />
              <span class="time-separator">{{ t('settingsStage4.cleaningSettings.to') }}</span>
              <el-time-select
                v-model="currentConfig.checkoutEndTime"
                :disabled="!isEditing"
                start="06:00"
                step="00:30"
                end="23:30"
                :placeholder="t('settingsStage4.cleaningSettings.placeholders.selectTime')"
              />
            </div>
            <p class="setting-desc">{{ t('settingsStage4.cleaningSettings.descriptions.checkout') }}</p>
          </div>
        </div>

        <!-- 保洁任务生成设置 -->
        <div class="settings-section">
          <h3 class="section-title">{{ t('settingsStage4.cleaningSettings.sections.taskGeneration') }}</h3>

          <div class="switch-setting-item">
            <div class="switch-content">
              <label class="setting-label">{{ t('settingsStage4.cleaningSettings.fields.stay') }}</label>
              <p class="setting-desc">{{ t('settingsStage4.cleaningSettings.descriptions.autoStay') }}</p>
            </div>
            <el-switch v-model="currentConfig.autoStayTask" :disabled="!isEditing" />
          </div>

          <div class="switch-setting-item">
            <div class="switch-content">
              <label class="setting-label">{{ t('settingsStage4.cleaningSettings.fields.turnover') }}</label>
              <p class="setting-desc">{{ t('settingsStage4.cleaningSettings.descriptions.autoCheckout') }}</p>
            </div>
            <el-switch v-model="currentConfig.autoCheckoutTask" :disabled="!isEditing" />
          </div>
        </div>

        <!-- 编辑模式下的保存按钮 -->
        <div v-if="isEditing" class="edit-actions">
          <el-button @click="handleCancelEdit">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleSaveEdit">{{ t('settings.common.save') }}</el-button>
        </div>
      </div>

      <!-- 保洁员标签页 -->
      <div v-if="activeTab === 'cleaners'" class="tab-content">
        <div class="table-toolbar">
          <el-input
            v-model="cleanerSearchText"
            :placeholder="t('settingsStage4.cleaningSettings.placeholders.searchCleaner')"
            class="search-input-wide"
            clearable
          />
          <el-button type="primary" @click="handleAddCleaner">{{ t('settingsStage4.cleaningSettings.actions.addCleaner') }}</el-button>
        </div>

        <el-table
          :data="filteredCleaners"
          border
          stripe
          class="data-table"
        >
          <el-table-column prop="name" :label="t('settingsStage4.cleaningSettings.fields.name')" min-width="150" />
          <el-table-column prop="email" :label="t('settingsStage4.cleaningSettings.fields.email')" min-width="250" />
          <el-table-column :label="t('settingsStage4.accountList.columns.actions')" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="handleDeleteCleaner(row)">{{ t('settings.common.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 易耗品标签页 -->
      <div v-if="activeTab === 'supplies'" class="tab-content">
        <div class="table-toolbar">
          <div class="filter-group">
            <span class="filter-label">{{ t('settingsStage4.cleaningSettings.fields.roomType') }}:</span>
            <el-select v-model="selectedRoomType" :placeholder="t('settingsStage4.cleaningSettings.placeholders.selectRoomType')" style="width: 200px">
              <el-option :label="t('settingsStage4.cleaningSettings.all')" :value="''" />
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
          <el-table-column prop="roomType" :label="t('settingsStage4.cleaningSettings.fields.roomType')" min-width="150" />
          <el-table-column prop="supplies" :label="t('settingsStage4.cleaningSettings.fields.supplies')" min-width="200" />
          <el-table-column :label="t('settingsStage4.accountList.columns.actions')" width="150" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEditSupply(row)">{{ t('settings.common.edit') }}</el-button>
              <el-button link type="danger" @click="handleClearSupply(row)">{{ t('settingsStage4.cleaningSettings.actions.clear') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 添加保洁员对话框 -->
    <el-dialog
      v-model="showCleanerDialog"
      :title="t('settingsStage4.cleaningSettings.actions.addCleaner')"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="cleanerForm" label-width="80px">
        <el-form-item :label="t('settingsStage4.cleaningSettings.fields.name')">
          <el-input v-model="cleanerForm.name" :placeholder="t('settingsStage4.cleaningSettings.placeholders.name')" />
        </el-form-item>
        <el-form-item :label="t('settingsStage4.cleaningSettings.fields.email')">
          <el-input v-model="cleanerForm.email" :placeholder="t('settingsStage4.cleaningSettings.placeholders.email')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCleanerDialog = false">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleSaveCleaner">{{ t('settings.common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑易耗品对话框 -->
    <el-dialog
      v-model="showSupplyDialog"
      :title="t('settingsStage4.cleaningSettings.dialog.editSupply')"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form v-if="currentSupply" :model="currentSupply" label-width="80px">
        <el-form-item :label="t('settingsStage4.cleaningSettings.fields.roomType')">
          <el-input v-model="currentSupply.roomType" disabled />
        </el-form-item>
        <el-form-item :label="t('settingsStage4.cleaningSettings.fields.supplies')">
          <el-input
            v-model="currentSupply.supplies"
            type="textarea"
            :rows="4"
            :placeholder="t('settingsStage4.cleaningSettings.placeholders.supplies')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showSupplyDialog = false">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleSaveSupply">{{ t('settings.common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Minus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  getOrCreateCleaningConfig,
  updateCleaningConfig,
  getCleanersByStoreId,
  deleteCleaner,
  getCleaningSuppliesByUserId,
  createCleaningSupply,
  updateCleaningSupply,
  clearCleaningSupply,
  sendCleanerInvitation,
} from '@/api/cleaning'
import { getUserStores, type StoreDTO } from '@/api/store'
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
const { t } = useI18n()

const tabs = computed(() => [
  { key: 'task-time', label: t('settingsStage4.cleaningSettings.tabs.taskTime') },
  { key: 'cleaners', label: t('settingsStage4.cleaningSettings.tabs.cleaners') },
  { key: 'supplies', label: t('settingsStage4.cleaningSettings.tabs.supplies') },
])

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
const selectedRoomType = ref('')
const roomTypes = ref<string[]>([])
const supplies = ref<Supply[]>([])
const showSupplyDialog = ref(false)
const currentSupply = ref<Supply | null>(null)

// 加载门店列表
const loadStores = async () => {
  try {
    loading.value = true
    const response = await getUserStores()
    if (response.success && response.data) {
      // 将门店数据映射到本地 Store 接口
      stores.value = response.data.map((store: StoreDTO) => ({
        id: store.id,
        name: store.name,
        enabled: true, // 默认为启用状态,后续可以根据保洁配置的 enabled 字段来设置
      }))
    } else {
      ElMessage.error(response.message || t('settingsStage4.cleaningSettings.messages.loadStoresFailed'))
    }
  } catch (error) {
    console.error('加载门店列表失败:', error)
    ElMessage.error(t('settingsStage4.cleaningSettings.messages.loadStoresFailed'))
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
      roomTypes.value = roomTypeNames
    } else {
      ElMessage.error(response.message || t('settingsStage4.cleaningSettings.messages.loadRoomTypesFailed'))
    }
  } catch (error) {
    console.error('加载房型列表失败:', error)
    ElMessage.error(t('settingsStage4.cleaningSettings.messages.loadRoomTypesFailed'))
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
    ElMessage.error(t('settingsStage4.cleaningSettings.messages.loadConfigFailed'))
  } finally {
    loading.value = false
  }
}

// 加载保洁员列表
const loadCleaners = async (storeId: number) => {
  try {
    loading.value = true
    const response = await getCleanersByStoreId(storeId)
    if (response.success && response.data) {
      cleaners.value = response.data
    }
  } catch (error) {
    console.error('加载保洁员列表失败:', error)
    ElMessage.error(t('settingsStage4.cleaningSettings.messages.loadCleanersFailed'))
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
    ElMessage.error(t('settingsStage4.cleaningSettings.messages.loadSuppliesFailed'))
  } finally {
    loading.value = false
  }
}

const handleConfig = async (store: Store) => {
  currentStore.value = store
  // 同步到localStorage,以便请求拦截器能获取X-Store-Id
  localStorage.setItem('currentStore', JSON.stringify(store))
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
    ElMessage.error(t('settingsStage4.cleaningSettings.messages.configIdMissing'))
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
      ElMessage.success(t('settingsStage4.storeBasic.messages.saveSuccess'))
      isEditing.value = false
      configBackup.value = null
    } else {
      ElMessage.error(response.message || t('settingsStage4.storeBasic.messages.saveFailed'))
    }
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error(t('settingsStage4.storeBasic.messages.saveFailed'))
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
  ElMessageBox.confirm(t('settingsStage4.cleaningSettings.messages.deleteCleanerConfirm', { name: row.name }), t('settingsStage4.cleaningSettings.messages.deleteTitle'), {
    confirmButtonText: t('settings.common.confirm'),
    cancelButtonText: t('settings.common.cancel'),
    type: 'warning',
  })
    .then(async () => {
      try {
        loading.value = true
        const response = await deleteCleaner(row.id)
        if (response.success) {
          ElMessage.success(t('settingsStage4.cleaningSettings.messages.deleteSuccess'))
          // 重新加载保洁员列表
          if (currentStore.value) {
            await loadCleaners(currentStore.value.id)
          }
        } else {
          ElMessage.error(response.message || t('settingsStage4.cleaningSettings.messages.deleteFailed'))
        }
      } catch (error) {
        console.error('删除保洁员失败:', error)
        ElMessage.error((error as any)?.response?.data?.message || (error as any)?.message || t('settingsStage4.cleaningSettings.messages.deleteFailed'))
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
    ElMessage.warning(t('settingsStage4.cleaningSettings.messages.nameRequired'))
    return
  }
  if (!cleanerForm.email.trim()) {
    ElMessage.warning(t('settingsStage4.cleaningSettings.messages.emailRequired'))
    return
  }

  // 验证邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(cleanerForm.email.trim())) {
    ElMessage.warning(t('settingsStage4.cleaningSettings.messages.emailInvalid'))
    return
  }

  if (!currentStore.value) {
    ElMessage.error(t('settingsStage4.cleaningSettings.messages.storeNotSelected'))
    return
  }

  try {
    loading.value = true
    // 确保localStorage已同步当前门店信息(为请求拦截器提供X-Store-Id)
    localStorage.setItem('currentStore', JSON.stringify(currentStore.value))

    // userId和storeId由后端从请求上下文自动获取,无需前端传递
    const response = await sendCleanerInvitation({
      email: cleanerForm.email.trim(),
      name: cleanerForm.name.trim(),
    })

    if (response.success) {
      ElMessage.success(t('settingsStage4.cleaningSettings.messages.invitationSent'))
      showCleanerDialog.value = false
      cleanerForm.name = ''
      cleanerForm.email = ''
    } else {
      ElMessage.error(response.message || t('settingsStage4.cleaningSettings.messages.invitationFailed'))
    }
  } catch (error: any) {
    console.error('发送邀请失败:', error)
    ElMessage.error(error.response?.data?.message || error.message || t('settingsStage4.cleaningSettings.messages.invitationFailed'))
  } finally {
    loading.value = false
  }
}

// 易耗品相关函数
// 组合房型和易耗品数据 - 以房型为主,显示每个房型的易耗品配置
const roomTypeSupplies = computed(() => {
  // 为每个房型创建一条记录,包含其易耗品配置
  return roomTypes.value.map(roomTypeName => {
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
  if (!selectedRoomType.value) {
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
    ElMessage.warning(t('settingsStage4.cleaningSettings.messages.supplyNotConfigured'))
    return
  }

  ElMessageBox.confirm(t('settingsStage4.cleaningSettings.messages.clearSupplyConfirm', { roomType: row.roomType }), t('settingsStage4.cleaningSettings.messages.clearTitle'), {
    confirmButtonText: t('settings.common.confirm'),
    cancelButtonText: t('settings.common.cancel'),
    type: 'warning',
  })
    .then(async () => {
      try {
        loading.value = true
        const response = await clearCleaningSupply(row.id!)
        if (response.success) {
          ElMessage.success(t('settingsStage4.cleaningSettings.messages.clearSuccess'))
          // 重新加载易耗品列表
          await loadSupplies()
        } else {
          ElMessage.error(response.message || t('settingsStage4.cleaningSettings.messages.clearFailed'))
        }
      } catch (error) {
        console.error('清空易耗品失败:', error)
        ElMessage.error(t('settingsStage4.cleaningSettings.messages.clearFailed'))
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
        roomType: currentSupply.value.roomType,
        supplies: currentSupply.value.supplies,
      })
    } else {
      response = await createCleaningSupply({
        roomType: currentSupply.value.roomType,
        supplies: currentSupply.value.supplies,
      })
    }

    if (response.success) {
      ElMessage.success(t('settingsStage4.storeBasic.messages.saveSuccess'))
      showSupplyDialog.value = false
      currentSupply.value = null
      // 重新加载易耗品列表
      await loadSupplies()
    } else {
      ElMessage.error(response.message || t('settingsStage4.storeBasic.messages.saveFailed'))
    }
  } catch (error) {
    console.error('保存易耗品失败:', error)
    ElMessage.error(t('settingsStage4.storeBasic.messages.saveFailed'))
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
  background: #ffffff;
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
