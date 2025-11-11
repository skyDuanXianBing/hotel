<template>
  <div class="supplies-container">
    <!-- 顶部提示信息 -->
    <div class="notice-banner">
      <span>您可以添加易耗品并与房型关联,对易耗品进行编辑或删除后,系统会将结果同步到引用该易耗品的房型中。</span>
    </div>

    <!-- 搜索和添加按钮 -->
    <div class="toolbar">
      <el-input
        v-model="searchText"
        placeholder=""
        class="search-input"
        clearable
      />
      <el-button type="primary" @click="handleAddSupply">添加易耗品</el-button>
    </div>

    <!-- 易耗品卡片网格 -->
    <div class="supplies-grid">
      <!-- 添加易耗品分类输入框 -->
      <div v-if="showSupplyInput" class="supply-card editing">
        <el-input
          v-model="newSupplyName"
          placeholder="请输入分类名称"
          @keyup.enter="handleSaveNewSupply"
        />
        <div class="card-actions">
          <el-button type="primary" link @click="handleSaveNewSupply">
            <el-icon><Check /></el-icon>
          </el-button>
          <el-button type="danger" link @click="handleCancelNewSupply">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 易耗品卡片列表 -->
      <div
        v-for="supply in filteredSupplies"
        :key="supply.id"
        class="supply-card"
      >
        <el-icon class="drag-icon"><Grid /></el-icon>
        <div class="supply-color-bar"></div>
        <span class="supply-name">{{ supply.roomType }}</span>
      </div>
    </div>

    <!-- 添加易耗品对话框 -->
    <el-dialog
      v-model="showSupplyDialog"
      title="添加易耗品"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="supplyForm" label-width="100px">
        <el-form-item label="房型">
          <el-select v-model="supplyForm.roomType" placeholder="请选择房型" style="width: 100%">
            <el-option
              v-for="roomType in availableRoomTypes"
              :key="roomType"
              :label="roomType"
              :value="roomType"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="易耗品">
          <el-input
            v-model="supplyForm.supplies"
            type="textarea"
            :rows="4"
            placeholder="请输入易耗品名称,多个易耗品请用逗号分隔"
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
import { Grid, Check, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getCleaningSuppliesByUserId,
  createCleaningSupply,
  updateCleaningSupply,
} from '@/api/cleaning'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'

interface SupplyItem {
  id: number
  roomType: string
  supplies: string
}

const loading = ref(false)
const searchText = ref('')
const showSupplyDialog = ref(false)
const showSupplyInput = ref(false)
const newSupplyName = ref('')

// 易耗品数据
const supplies = ref<SupplyItem[]>([])

// 可选房型
const availableRoomTypes = ref<string[]>([])

// 表单数据
const supplyForm = reactive({
  roomType: '',
  supplies: '',
})

// 加载房型列表
const loadRoomTypes = async () => {
  try {
    loading.value = true
    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      availableRoomTypes.value = response.data.map((rt: RoomTypeDTO) => rt.name)
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

// 加载易耗品列表
const loadSupplies = async () => {
  try {
    loading.value = true
    const response = await getCleaningSuppliesByUserId(1) // 默认用户ID为1
    if (response.success && response.data) {
      supplies.value = response.data
    } else {
      ElMessage.error(response.message || '加载易耗品列表失败')
    }
  } catch (error) {
    console.error('加载易耗品列表失败:', error)
    ElMessage.error('加载易耗品列表失败')
  } finally {
    loading.value = false
  }
}

// 过滤后的易耗品列表
const filteredSupplies = computed(() => {
  if (!searchText.value.trim()) {
    return supplies.value
  }
  const search = searchText.value.toLowerCase()
  return supplies.value.filter(
    (item) =>
      item.roomType.toLowerCase().includes(search) ||
      item.supplies.toLowerCase().includes(search)
  )
})

// 添加易耗品分类 - 切换为内联输入模式
const handleAddSupply = () => {
  showSupplyInput.value = true
  newSupplyName.value = ''
}

// 保存新增易耗品分类 - 内联卡片方式
const handleSaveNewSupply = async () => {
  if (!newSupplyName.value.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }

  try {
    loading.value = true
    const response = await createCleaningSupply({
      userId: 1,
      roomType: newSupplyName.value.trim(),
      supplies: '',
    })

    if (response.success) {
      ElMessage.success('新增分类成功')
      showSupplyInput.value = false
      newSupplyName.value = ''
      await loadSupplies()
    } else {
      ElMessage.error(response.message || '新增失败')
    }
  } catch (error) {
    console.error('新增失败:', error)
    ElMessage.error('新增失败')
  } finally {
    loading.value = false
  }
}

// 取消新增易耗品分类
const handleCancelNewSupply = () => {
  showSupplyInput.value = false
  newSupplyName.value = ''
}

// 保存易耗品 - 对话框方式(保留用于编辑)
const handleSaveSupply = async () => {
  if (!supplyForm.roomType.trim()) {
    ElMessage.warning('请选择房型')
    return
  }

  try {
    loading.value = true
    // 检查该房型是否已存在
    const existingItem = supplies.value.find(
      (item) => item.roomType === supplyForm.roomType
    )

    let response
    if (existingItem) {
      // 更新现有房型的易耗品
      response = await updateCleaningSupply(existingItem.id, {
        userId: 1,
        roomType: supplyForm.roomType,
        supplies: supplyForm.supplies,
      })
      if (response.success) {
        ElMessage.success('更新成功')
      }
    } else {
      // 添加新的房型易耗品
      response = await createCleaningSupply({
        userId: 1,
        roomType: supplyForm.roomType,
        supplies: supplyForm.supplies,
      })
      if (response.success) {
        ElMessage.success('添加成功')
      }
    }

    if (response.success) {
      showSupplyDialog.value = false
      await loadSupplies()
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRoomTypes()
  loadSupplies()
})
</script>

<style scoped>
.supplies-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

/* 顶部提示信息 */
.notice-banner {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #1890ff;
  line-height: 1.6;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

/* 易耗品卡片网格 */
.supplies-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.supply-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: move;
  transition: all 0.2s;
  min-height: 48px;
}

.supply-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.supply-card.editing {
  background: #fff;
  padding: 8px 12px;
  cursor: default;
}

.drag-icon {
  flex-shrink: 0;
  font-size: 18px;
  color: #909399;
  cursor: move;
}

.supply-color-bar {
  flex-shrink: 0;
  width: 4px;
  height: 24px;
  border-radius: 2px;
  background-color: #409eff;
}

.supply-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* 按钮样式调整 */
:deep(.el-button.is-link) {
  padding: 4px;
  font-size: 18px;
}

:deep(.el-input__wrapper) {
  padding: 8px 12px;
}

.supply-card.editing :deep(.el-input) {
  flex: 1;
}

/* 对话框 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
