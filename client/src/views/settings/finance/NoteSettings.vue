<template>
  <div class="note-settings-container">
    <!-- 收入项部分 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">{{ t('settings.noteSettings.incomeItems') }}</h3>
        <el-button type="primary" @click="handleAddIncome">
          {{ t('settings.noteSettings.addIncome') }}
        </el-button>
      </div>

      <draggable
        v-model="incomeItemsLocal"
        class="items-grid"
        item-key="id"
        @end="handleIncomeDragEnd"
      >
        <template #item="{ element: item }">
          <div class="grid-item">
            <el-icon class="drag-icon"><Menu /></el-icon>

            <!-- 编辑模式 -->
            <template v-if="editingItemId === item.id">
              <el-input
                v-model="editingName"
                size="small"
                maxlength="20"
                @keyup.enter="handleSaveEdit(item.id)"
              />
              <div class="item-actions">
                <el-button
                  link
                  type="success"
                  :icon="Check"
                  @click="handleSaveEdit(item.id)"
                />
                <el-button
                  link
                  type="danger"
                  :icon="Close"
                  @click="handleCancelEdit"
                />
              </div>
            </template>

            <!-- 查看模式 -->
            <template v-else>
              <span class="item-name">{{ item.name }}</span>
              <div class="item-actions">
                <el-button
                  link
                  type="primary"
                  :icon="Edit"
                  @click="handleEditItem(item)"
                />
                <el-button
                  link
                  type="danger"
                  :icon="Delete"
                  @click="handleDeleteItem(item)"
                />
              </div>
            </template>
          </div>
        </template>

        <!-- 新增项的输入框 -->
        <template #footer>
          <div v-if="showIncomeInput" class="grid-item editing">
            <el-input
              v-model="newIncomeName"
              :placeholder="t('settings.noteSettings.placeholders.incomeName')"
              maxlength="20"
              @keyup.enter="handleSaveNewIncome"
            />
            <div class="item-actions">
              <el-button
                link
                type="success"
                :icon="Check"
                @click="handleSaveNewIncome"
              />
              <el-button
                link
                type="danger"
                :icon="Close"
                @click="handleCancelNewIncome"
              />
            </div>
          </div>
        </template>
      </draggable>
    </div>

    <!-- 支出项部分 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">{{ t('settings.noteSettings.expenseItems') }}</h3>
        <el-button type="primary" @click="handleAddExpense">
          {{ t('settings.noteSettings.addExpense') }}
        </el-button>
      </div>

      <draggable
        v-model="expenseItemsLocal"
        class="items-grid"
        item-key="id"
        @end="handleExpenseDragEnd"
      >
        <template #item="{ element: item }">
          <div class="grid-item">
            <el-icon class="drag-icon"><Menu /></el-icon>

            <!-- 编辑模式 -->
            <template v-if="editingItemId === item.id">
              <el-input
                v-model="editingName"
                size="small"
                maxlength="20"
                @keyup.enter="handleSaveEdit(item.id)"
              />
              <div class="item-actions">
                <el-button
                  link
                  type="success"
                  :icon="Check"
                  @click="handleSaveEdit(item.id)"
                />
                <el-button
                  link
                  type="danger"
                  :icon="Close"
                  @click="handleCancelEdit"
                />
              </div>
            </template>

            <!-- 查看模式 -->
            <template v-else>
              <span class="item-name">{{ item.name }}</span>
              <div class="item-actions">
                <el-button
                  link
                  type="primary"
                  :icon="Edit"
                  @click="handleEditItem(item)"
                />
                <el-button
                  link
                  type="danger"
                  :icon="Delete"
                  @click="handleDeleteItem(item)"
                />
              </div>
            </template>
          </div>
        </template>

        <!-- 新增项的输入框 -->
        <template #footer>
          <div v-if="showExpenseInput" class="grid-item editing">
            <el-input
              v-model="newExpenseName"
              :placeholder="t('settings.noteSettings.placeholders.expenseName')"
              maxlength="20"
              @keyup.enter="handleSaveNewExpense"
            />
            <div class="item-actions">
              <el-button
                link
                type="success"
                :icon="Check"
                @click="handleSaveNewExpense"
              />
              <el-button
                link
                type="danger"
                :icon="Close"
                @click="handleCancelNewExpense"
              />
            </div>
          </div>
        </template>
      </draggable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Close, Menu, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import draggable from 'vuedraggable'
import {
  getAllCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  updateCategoriesOrder,
  type NoteCategoryDTO,
  type NoteCategoryType,
} from '@/api/noteCategory'

const { t } = useI18n()

interface NoteItem {
  id: number
  name: string
  type: 'income' | 'expense'
  displayOrder: number
}

const mapCategoryToNoteItem = (cat: NoteCategoryDTO): NoteItem => ({
  id: cat.id,
  name: cat.name,
  type: cat.type as 'income' | 'expense',
  displayOrder: cat.displayOrder,
})

// 用于拖拽的本地数组（可变）
const incomeItemsLocal = ref<NoteItem[]>([])
const expenseItemsLocal = ref<NoteItem[]>([])

// 控制输入框显示
const showIncomeInput = ref(false)
const showExpenseInput = ref(false)

// 新增项的名称
const newIncomeName = ref('')
const newExpenseName = ref('')

// 编辑状态
const editingItemId = ref<number | null>(null)
const editingName = ref('')
const editingType = ref<'income' | 'expense'>('income')

// 加载状态
const loading = ref(false)

// 加载分类数据
const loadCategories = async () => {
  try {
    loading.value = true
    const response = await getAllCategories()
    if (response.success) {
      incomeItemsLocal.value = response.data
        .filter((cat) => cat.type === 'income')
        .sort((a, b) => a.displayOrder - b.displayOrder)
        .map(mapCategoryToNoteItem)
      expenseItemsLocal.value = response.data
        .filter((cat) => cat.type === 'expense')
        .sort((a, b) => a.displayOrder - b.displayOrder)
        .map(mapCategoryToNoteItem)
    } else {
      ElMessage.error(response.message || t('settings.noteSettings.messages.loadFailed'))
    }
  } catch (error) {
    console.error('加载分类失败:', error)
    ElMessage.error(t('settings.noteSettings.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadCategories()
})

// 添加收入项
const handleAddIncome = () => {
  showIncomeInput.value = true
  newIncomeName.value = ''
}

// 保存新的收入项
const handleSaveNewIncome = async () => {
  if (!newIncomeName.value.trim()) {
    ElMessage.warning(t('settings.noteSettings.messages.incomeNameRequired'))
    return
  }

  try {
    const response = await createCategory({
      name: newIncomeName.value,
      type: 'income' as NoteCategoryType,
      displayOrder: incomeItemsLocal.value.length,
    })

    if (response.success) {
      // 重新加载分类数据
      await loadCategories()
      ElMessage.success(t('settings.noteSettings.messages.addSuccess'))
      showIncomeInput.value = false
      newIncomeName.value = ''
    } else {
      ElMessage.error(response.message || t('settings.noteSettings.messages.addFailed'))
    }
  } catch (error) {
    console.error('添加收入项失败:', error)
    ElMessage.error(t('settings.noteSettings.messages.addFailed'))
  }
}

// 取消新增收入项
const handleCancelNewIncome = () => {
  showIncomeInput.value = false
  newIncomeName.value = ''
}

// 添加支出项
const handleAddExpense = () => {
  showExpenseInput.value = true
  newExpenseName.value = ''
}

// 保存新的支出项
const handleSaveNewExpense = async () => {
  if (!newExpenseName.value.trim()) {
    ElMessage.warning(t('settings.noteSettings.messages.expenseNameRequired'))
    return
  }

  try {
    const response = await createCategory({
      name: newExpenseName.value,
      type: 'expense' as NoteCategoryType,
      displayOrder: expenseItemsLocal.value.length,
    })

    if (response.success) {
      // 重新加载分类数据
      await loadCategories()
      ElMessage.success(t('settings.noteSettings.messages.addSuccess'))
      showExpenseInput.value = false
      newExpenseName.value = ''
    } else {
      ElMessage.error(response.message || t('settings.noteSettings.messages.addFailed'))
    }
  } catch (error) {
    console.error('添加支出项失败:', error)
    ElMessage.error(t('settings.noteSettings.messages.addFailed'))
  }
}

// 取消新增支出项
const handleCancelNewExpense = () => {
  showExpenseInput.value = false
  newExpenseName.value = ''
}

// ==================== 编辑功能 ====================

// 开始编辑
const handleEditItem = (item: NoteItem) => {
  editingItemId.value = item.id
  editingName.value = item.name
  editingType.value = item.type
}

// 保存编辑
const handleSaveEdit = async (id: number) => {
  if (!editingName.value.trim()) {
    ElMessage.warning(t('settings.noteSettings.messages.categoryNameRequired'))
    return
  }

  try {
    const response = await updateCategory(id, {
      name: editingName.value,
      type: editingType.value as NoteCategoryType,
    })

    if (response.success) {
      await loadCategories()
      ElMessage.success(t('settings.noteSettings.messages.updateSuccess'))
      editingItemId.value = null
      editingName.value = ''
    } else {
      ElMessage.error(response.message || t('settings.noteSettings.messages.updateFailed'))
    }
  } catch (error) {
    console.error('修改分类失败:', error)
    ElMessage.error(t('settings.noteSettings.messages.updateFailed'))
  }
}

// 取消编辑
const handleCancelEdit = () => {
  editingItemId.value = null
  editingName.value = ''
}

// ==================== 删除功能 ====================

// 删除分类
const handleDeleteItem = async (item: NoteItem) => {
  try {
    await ElMessageBox.confirm(
      t('settings.noteSettings.messages.deleteConfirm', { name: item.name }),
      t('settings.common.deleteConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirmButton'),
        cancelButtonText: t('settings.common.cancelButton'),
        type: 'warning',
      },
    )

    const response = await deleteCategory(item.id)

    if (response.success) {
      await loadCategories()
      ElMessage.success(t('settings.common.deleteSuccess'))
    } else {
      ElMessage.error(response.message || t('settings.noteSettings.messages.deleteFailed'))
    }
  } catch (error) {
    // 用户取消删除
    if (error === 'cancel') {
      return
    }
    console.error('删除分类失败:', error)
    ElMessage.error(t('settings.noteSettings.messages.deleteFailed'))
  }
}

// ==================== 拖拽排序功能 ====================

// 收入项拖拽结束
const handleIncomeDragEnd = async (event: any) => {
  const { newIndex, oldIndex } = event

  if (newIndex === oldIndex) {
    return
  }

  try {
    // 获取当前收入项列表
    const items = incomeItemsLocal.value

    // 更新 displayOrder
    const updatedItems = items.map((item, index) => ({
      id: item.id,
      displayOrder: index,
    }))

    const response = await updateCategoriesOrder(updatedItems)

    if (response.success) {
      await loadCategories()
      ElMessage.success(t('settings.noteSettings.messages.sortSaved'))
    } else {
      ElMessage.error(response.message || t('settings.noteSettings.messages.sortSaveFailed'))
      await loadCategories() // 恢复原顺序
    }
  } catch (error) {
    console.error('保存排序失败:', error)
    ElMessage.error(t('settings.noteSettings.messages.sortSaveFailed'))
    await loadCategories() // 恢复原顺序
  }
}

// 支出项拖拽结束
const handleExpenseDragEnd = async (event: any) => {
  const { newIndex, oldIndex } = event

  if (newIndex === oldIndex) {
    return
  }

  try {
    // 获取当前支出项列表
    const items = expenseItemsLocal.value

    // 更新 displayOrder
    const updatedItems = items.map((item, index) => ({
      id: item.id,
      displayOrder: index,
    }))

    const response = await updateCategoriesOrder(updatedItems)

    if (response.success) {
      await loadCategories()
      ElMessage.success(t('settings.noteSettings.messages.sortSaved'))
    } else {
      ElMessage.error(response.message || t('settings.noteSettings.messages.sortSaveFailed'))
      await loadCategories() // 恢复原顺序
    }
  } catch (error) {
    console.error('保存排序失败:', error)
    ElMessage.error(t('settings.noteSettings.messages.sortSaveFailed'))
    await loadCategories() // 恢复原顺序
  }
}

</script>

<style scoped>
.note-settings-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e4e7ed;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.grid-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: move;
  transition: all 0.2s;
  min-height: 56px;
}

.grid-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.grid-item:hover .item-actions {
  opacity: 1;
}

.grid-item.editing {
  background: #fff;
  padding: 8px 12px;
  cursor: default;
}

.drag-icon {
  flex-shrink: 0;
  font-size: 16px;
  color: #909399;
}

.item-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.item-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.grid-item.editing .item-actions {
  opacity: 1;
}

:deep(.el-button.is-link) {
  padding: 4px;
  font-size: 16px;
}

:deep(.el-input__wrapper) {
  padding: 8px 12px;
}

.grid-item.editing :deep(.el-input) {
  flex: 1;
}
</style>
