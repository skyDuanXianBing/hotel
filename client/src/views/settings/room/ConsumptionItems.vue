<template>
  <div class="consumption-items-container">
    <!-- 提示信息 -->
    <el-alert
      :title="t('settingsStage4.consumptionItems.notice')"
      type="info"
      :closable="false"
      show-icon
      class="info-alert"
    />

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="tabs-container">
      <el-tab-pane :label="t('settingsStage4.consumptionItems.tabs.list')" name="list">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div></div>
          <el-button type="primary" @click="handleAdd">{{ t('settings.common.add') }}</el-button>
        </div>

        <!-- 表格 -->
        <el-table :data="consumptionItems" border class="items-table">
          <el-table-column prop="category" :label="t('settingsStage4.consumptionItems.columns.category')" min-width="120" />
          <el-table-column prop="name" :label="t('settingsStage4.consumptionItems.columns.name')" min-width="150" />
          <el-table-column prop="price" :label="t('settingsStage4.consumptionItems.columns.price')" min-width="120">
            <template #default="{ row }">
              ¥{{ row.price.toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('settings.common.status')" width="100" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.enabled"
                :active-text="t('settings.common.enabled')"
                @change="handleStatusChange(row)"
              />
            </template>
          </el-table-column>
          <el-table-column :label="t('settings.common.operation')" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEdit(row)">{{ t('settings.common.edit') }}</el-button>
              <el-button link type="danger" @click="handleDelete(row)">{{ t('settings.common.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <span class="pagination-info">{{ t('settingsStage4.common.itemsTotal', { count: total }) }}</span>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 25, 50, 100]"
            :total="total"
            layout="sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('settingsStage4.consumptionItems.tabs.category')" name="category">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div></div>
          <el-button type="primary" @click="handleAddCategory">{{ t('settingsStage4.consumptionItems.addCategory') }}</el-button>
        </div>

        <!-- 分类卡片 -->
        <div class="category-grid">
          <div
            v-for="category in categories"
            :key="category.id"
            class="category-card"
          >
            <div class="category-badge" :data-count="t('settingsStage4.consumptionItems.categoryDefault')"></div>
            <div class="category-icon">
              <el-icon size="24"><Menu /></el-icon>
            </div>
            <div class="category-name">{{ category.name }}</div>
            <div class="category-actions">
              <el-button link type="primary" @click="handleEditCategory(category)">
                {{ t('settings.common.edit') }}
              </el-button>
              <el-button link type="danger" @click="handleDeleteCategory(category)">
                {{ t('settings.common.delete') }}
              </el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('settingsStage4.consumptionItems.dialog.editItem') : t('settingsStage4.consumptionItems.dialog.addItem')"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item :label="t('settingsStage4.consumptionItems.fields.category')" prop="category" required>
          <el-select v-model="form.category" :placeholder="t('settingsStage4.consumptionItems.placeholders.selectCategory')" style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.name"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('settingsStage4.consumptionItems.fields.itemName')" prop="name" required>
          <el-input v-model="form.name" :placeholder="t('settingsStage4.consumptionItems.placeholders.itemName')" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item :label="t('settingsStage4.consumptionItems.fields.price')" prop="price" required>
          <el-input-number
            v-model="form.price"
            :min="0"
            :max="999999"
            :precision="2"
            :controls="false"
            style="width: 100%"
          >
            <template #prefix>¥</template>
          </el-input-number>
        </el-form-item>

        <el-form-item :label="t('settings.common.status')" prop="enabled">
          <el-switch v-model="form.enabled" :active-text="t('settings.common.enabled')" :inactive-text="t('settings.common.disabled')" />
        </el-form-item>

        <el-form-item :label="t('settingsStage4.consumptionItems.fields.description')" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            :placeholder="t('settingsStage4.consumptionItems.placeholders.description')"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancel">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleConfirm">{{ t('settings.common.confirmButton') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新增/编辑分类对话框 -->
    <el-dialog
      v-model="categoryDialogVisible"
      :title="isEditCategory ? t('settingsStage4.consumptionItems.dialog.editCategory') : t('settingsStage4.consumptionItems.dialog.addCategory')"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="categoryForm" :rules="categoryFormRules" ref="categoryFormRef" label-width="100px">
        <el-form-item :label="t('settingsStage4.consumptionItems.fields.categoryName')" prop="name" required>
          <el-input v-model="categoryForm.name" :placeholder="t('settingsStage4.consumptionItems.placeholders.categoryName')" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item :label="t('settingsStage4.consumptionItems.fields.description')" prop="description">
          <el-input
            v-model="categoryForm.description"
            type="textarea"
            :rows="3"
            :placeholder="t('settingsStage4.consumptionItems.placeholders.categoryDescription')"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelCategory">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleConfirmCategory">{{ t('settings.common.confirmButton') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Menu } from '@element-plus/icons-vue'
import { useStoreStore } from '@/stores/store'
import {
  getAllConsumptionItems,
  createConsumptionItem,
  updateConsumptionItem,
  deleteConsumptionItem,
  updateConsumptionItemEnabled,
  getAllConsumptionCategories,
  createConsumptionCategory,
  updateConsumptionCategory,
  deleteConsumptionCategory,
  type ConsumptionItemDTO,
  type ConsumptionCategoryDTO,
} from '@/api/consumptionItem'

const storeStore = useStoreStore()
const { t } = useI18n()

interface ConsumptionItem {
  id: number
  category: string
  name: string
  price: number
  enabled: boolean
  description?: string
}

interface ConsumptionItemForm {
  category: string
  name: string
  price: number
  enabled: boolean
  description: string
}

interface Category {
  id: number
  name: string
  count: number
  description?: string
}

interface CategoryForm {
  name: string
  description: string
}

const activeTab = ref('list')
const dialogVisible = ref(false)
const categoryDialogVisible = ref(false)
const isEditCategory = ref(false)
const categoryFormRef = ref<FormInstance>()
const currentEditCategoryId = ref<number | null>(null)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const currentEditId = ref<number | null>(null)

// 分页
const currentPage = ref(1)
const pageSize = ref(25)
const total = ref(2)

// 表单数据
const form = reactive<ConsumptionItemForm>({
  category: '',
  name: '',
  price: 0,
  enabled: true,
  description: '',
})

// 表单验证规则
const formRules: FormRules = {
  category: [{ required: true, message: t('settingsStage4.consumptionItems.validation.categoryRequired'), trigger: 'change' }],
  name: [{ required: true, message: t('settingsStage4.consumptionItems.validation.itemNameRequired'), trigger: 'blur' }],
  price: [{ required: true, message: t('settingsStage4.consumptionItems.validation.priceRequired'), trigger: 'blur' }],
}

// 分类表单数据
const categoryForm = reactive<CategoryForm>({
  name: '',
  description: '',
})

// 分类表单验证规则
const categoryFormRules: FormRules = {
  name: [{ required: true, message: t('settingsStage4.consumptionItems.validation.categoryNameRequired'), trigger: 'blur' }],
}

// 数据
const categories = ref<Category[]>([])
const consumptionItems = ref<ConsumptionItem[]>([])

// 加载消费项列表（门店级架构）
const loadConsumptionItems = async () => {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settingsStage4.consumptionItems.messages.selectStore'))
    consumptionItems.value = []
    return
  }

  try {
    const response = await getAllConsumptionItems()
    if (response.success) {
      consumptionItems.value = response.data.map((item: ConsumptionItemDTO) => ({
        id: item.id!,
        category: item.category,
        name: item.name,
        price: item.price,
        enabled: item.enabled,
        description: item.description,
      }))
      total.value = consumptionItems.value.length
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('加载消费项失败:', error)
    ElMessage.error(t('settingsStage4.consumptionItems.messages.loadItemsFailed'))
  }
}

// 加载分类列表（门店级架构）
const loadCategories = async () => {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settingsStage4.consumptionItems.messages.selectStore'))
    categories.value = []
    return
  }

  try {
    const response = await getAllConsumptionCategories()
    if (response.success) {
      categories.value = response.data.map((cat: ConsumptionCategoryDTO) => ({
        id: cat.id!,
        name: cat.name,
        count: cat.count || 0,
        description: cat.description,
      }))
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('加载分类失败:', error)
    ElMessage.error(t('settingsStage4.consumptionItems.messages.loadCategoriesFailed'))
  }
}

// 初始化数据
onMounted(() => {
  loadConsumptionItems()
  loadCategories()
})

const handleAdd = () => {
  isEdit.value = false
  currentEditId.value = null
  form.category = ''
  form.name = ''
  form.price = 0
  form.enabled = true
  form.description = ''
  dialogVisible.value = true
}

const handleEdit = (row: ConsumptionItem) => {
  isEdit.value = true
  currentEditId.value = row.id
  form.category = row.category
  form.name = row.name
  form.price = row.price
  form.enabled = row.enabled
  form.description = row.description || ''
  dialogVisible.value = true
}

const handleDelete = async (row: ConsumptionItem) => {
  try {
    await ElMessageBox.confirm(t('settingsStage4.consumptionItems.messages.deleteItemConfirm', { name: row.name }), t('settings.common.deleteConfirmTitle'), {
      confirmButtonText: t('settings.common.confirmButton'),
      cancelButtonText: t('settings.common.cancelButton'),
      type: 'warning',
    })

    const response = await deleteConsumptionItem(row.id)
    if (response.success) {
      ElMessage.success(t('settings.common.deleteSuccess'))
      await loadConsumptionItems()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(t('settings.common.deleteFailed'))
    }
  }
}

const handleStatusChange = async (row: ConsumptionItem) => {
  try {
    const response = await updateConsumptionItemEnabled(row.id, row.enabled)
    if (response.success) {
      ElMessage.success(row.enabled ? t('settings.common.enabled') : t('settings.common.disabled'))
    } else {
      ElMessage.error(response.message)
      // 恢复原状态
      row.enabled = !row.enabled
    }
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error(t('settingsStage4.consumptionItems.messages.statusUpdateFailed'))
    // 恢复原状态
    row.enabled = !row.enabled
  }
}

const handleCancel = () => {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

const handleConfirm = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    const itemData: ConsumptionItemDTO = {
      category: form.category,
      name: form.name,
      price: form.price,
      enabled: form.enabled,
      description: form.description,
    }

    let response
    if (isEdit.value && currentEditId.value) {
      // 编辑
      response = await updateConsumptionItem(currentEditId.value, itemData)
    } else {
      // 新增（storeId由后端自动注入）
      response = await createConsumptionItem(itemData)
    }

    if (response.success) {
      ElMessage.success(
        isEdit.value
          ? t('settingsStage4.consumptionItems.messages.editSuccess')
          : t('settingsStage4.consumptionItems.messages.addSuccess'),
      )
      dialogVisible.value = false
      formRef.value?.resetFields()
      await loadConsumptionItems()
      await loadCategories() // 刷新分类计数
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(t('settingsStage4.consumptionItems.messages.saveFailed'))
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  // 实际应用中,这里应该重新加载数据
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  // 实际应用中,这里应该重新加载数据
}

// 分类相关函数
const handleAddCategory = () => {
  isEditCategory.value = false
  currentEditCategoryId.value = null
  categoryForm.name = ''
  categoryForm.description = ''
  categoryDialogVisible.value = true
}

const handleEditCategory = (category: Category) => {
  isEditCategory.value = true
  currentEditCategoryId.value = category.id
  categoryForm.name = category.name
  categoryForm.description = category.description || ''
  categoryDialogVisible.value = true
}

const handleDeleteCategory = async (category: Category) => {
  try {
    await ElMessageBox.confirm(t('settingsStage4.consumptionItems.messages.deleteCategoryConfirm', { name: category.name }), t('settings.common.deleteConfirmTitle'), {
      confirmButtonText: t('settings.common.confirmButton'),
      cancelButtonText: t('settings.common.cancelButton'),
      type: 'warning',
    })

    const response = await deleteConsumptionCategory(category.id)
    if (response.success) {
      ElMessage.success(t('settings.common.deleteSuccess'))
      await loadCategories()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(t('settings.common.deleteFailed'))
    }
  }
}

const handleCancelCategory = () => {
  categoryDialogVisible.value = false
  categoryFormRef.value?.resetFields()
}

const handleConfirmCategory = async () => {
  if (!categoryFormRef.value) return

  try {
    const valid = await categoryFormRef.value.validate()
    if (!valid) return

    const categoryData: ConsumptionCategoryDTO = {
      name: categoryForm.name,
      description: categoryForm.description,
    }

    let response
    if (isEditCategory.value && currentEditCategoryId.value) {
      // 编辑
      response = await updateConsumptionCategory(currentEditCategoryId.value, categoryData)
    } else {
      // 新增（storeId由后端自动注入）
      response = await createConsumptionCategory(categoryData)
    }

    if (response.success) {
      ElMessage.success(
        isEditCategory.value
          ? t('settingsStage4.consumptionItems.messages.editSuccess')
          : t('settingsStage4.consumptionItems.messages.addSuccess'),
      )
      categoryDialogVisible.value = false
      categoryFormRef.value?.resetFields()
      await loadCategories()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(t('settingsStage4.consumptionItems.messages.saveFailed'))
  }
}
</script>

<style scoped>
.consumption-items-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.info-alert {
  margin-bottom: 20px;
}

.tabs-container {
  margin-top: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.items-table {
  margin-bottom: 16px;
}

.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
}

.pagination-info {
  font-size: 14px;
  color: #606266;
}

:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-input-number .el-input__inner) {
  text-align: left;
  padding-left: 30px;
}

:deep(.el-input-number .el-input__prefix) {
  left: 11px;
}

/* 分类卡片样式 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 16px;
}

.category-card {
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 24px 16px;
  background: #fff;
  transition: all 0.3s;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.category-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
}

.category-badge {
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 48px 48px 0;
  border-color: transparent #409eff transparent transparent;
}

.category-badge::after {
  content: attr(data-count);
  position: absolute;
  top: 4px;
  right: -44px;
  color: white;
  font-size: 12px;
  font-weight: 600;
  transform: rotate(45deg);
}

.category-icon {
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #ecf5ff;
}

.category-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  text-align: center;
}

.category-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
