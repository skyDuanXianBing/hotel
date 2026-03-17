<template>
  <div class="payment-methods" v-loading="loading">
    <el-alert type="info" :closable="false" class="info-alert">
      <template #default>
        <div class="info-content">
          <p>1. 收款方式按门店维度持久化保存，房态日历收款会实时联动这里的配置。</p>
          <p>2. 删除后会自动重排顺序，列表第一项会显示“默认”。</p>
        </div>
      </template>
    </el-alert>

    <div class="section-header">
      <h3 class="section-title">财务设置 - 收款方式</h3>
      <div class="header-actions">
        <el-checkbox v-model="selectAllMethods" @change="handleToggleSelectAll">全选</el-checkbox>
        <el-button type="danger" plain :disabled="selectedCount === 0" @click="handleDeleteSelected">
          删除选中（{{ selectedCount }}）
        </el-button>
        <el-button type="primary" @click="handleAddPaymentMethod">新增收款方式</el-button>
      </div>
    </div>

    <div class="payment-section">
      <h4 class="subsection-title">
        可用收款方式
        <span class="subtitle-desc">订单中收款/退款时可选项</span>
      </h4>

      <div class="payment-grid">
        <div v-for="method in availablePaymentMethods" :key="method.id" class="payment-card">
          <div v-if="method.isDefault && !method.isEditing" class="default-corner">默认</div>

          <div class="select-checkbox">
            <el-checkbox v-model="method.selected" @change="syncSelectAllState" />
          </div>

          <div class="drag-handle">
            <el-icon><Grid /></el-icon>
          </div>

          <div class="card-content">
            <el-input
              v-if="method.isEditing"
              v-model="method.name"
              placeholder="请输入收款方式名称"
              size="small"
              maxlength="50"
              @keyup.enter="handleSaveMethod(method)"
            />
            <span v-else class="method-name">{{ method.name }}</span>
          </div>

          <div class="card-actions">
            <template v-if="method.isEditing">
              <el-icon class="action-icon success" @click="handleSaveMethod(method)">
                <Check />
              </el-icon>
              <el-icon class="action-icon danger" @click="handleCancelEdit(method)">
                <Close />
              </el-icon>
            </template>
            <template v-else>
              <el-icon class="action-icon primary" @click="handleEditMethod(method)">
                <Edit />
              </el-icon>
              <el-icon class="action-icon danger" @click="handleDeleteMethod(method)">
                <Delete />
              </el-icon>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Grid, Check, Close, Delete, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createPaymentMethod,
  deletePaymentMethod,
  getAllPaymentMethods,
  updatePaymentMethod,
  type PaymentMethodDTO,
} from '@/api/paymentMethod'

interface PaymentMethodItem {
  id: number
  name: string
  enabled: boolean
  isDefault: boolean
  isEditing: boolean
  selected: boolean
  isNew: boolean
  originalName: string
}

const availablePaymentMethods = ref<PaymentMethodItem[]>([])
const selectAllMethods = ref(false)
const loading = ref(false)

const selectedCount = computed(() => availablePaymentMethods.value.filter((method) => method.selected).length)

const mapToViewModel = (items: PaymentMethodDTO[]): PaymentMethodItem[] => {
  return items.map((item, index) => ({
    id: item.id,
    name: item.name,
    enabled: item.enabled,
    isDefault: index === 0,
    isEditing: false,
    selected: false,
    isNew: false,
    originalName: item.name,
  }))
}

const syncSelectAllState = () => {
  const total = availablePaymentMethods.value.length
  if (!total) {
    selectAllMethods.value = false
    return
  }
  selectAllMethods.value = availablePaymentMethods.value.every((method) => method.selected)
}

const loadPaymentMethods = async () => {
  loading.value = true
  try {
    const response = await getAllPaymentMethods()
    if (!response.success) {
      ElMessage.error(response.message || '获取收款方式失败')
      return
    }
    availablePaymentMethods.value = mapToViewModel(response.data || [])
    syncSelectAllState()
  } catch (error) {
    console.error('加载收款方式失败:', error)
    ElMessage.error('加载收款方式失败')
  } finally {
    loading.value = false
  }
}

const handleToggleSelectAll = (checked: boolean) => {
  availablePaymentMethods.value.forEach((method) => {
    method.selected = checked
  })
}

const handleAddPaymentMethod = () => {
  const newMethod: PaymentMethodItem = {
    id: -Date.now(),
    name: '',
    enabled: true,
    isDefault: false,
    isEditing: true,
    selected: false,
    isNew: true,
    originalName: '',
  }
  availablePaymentMethods.value.push(newMethod)
  syncSelectAllState()
}

const handleEditMethod = (method: PaymentMethodItem) => {
  method.originalName = method.name
  method.isEditing = true
}

const handleSaveMethod = async (method: PaymentMethodItem) => {
  const normalizedName = method.name.trim()
  if (!normalizedName) {
    ElMessage.warning('请输入收款方式名称')
    return
  }

  try {
    if (method.isNew) {
      const response = await createPaymentMethod({
        name: normalizedName,
        enabled: method.enabled,
      })
      if (!response.success) {
        ElMessage.error(response.message || '新增收款方式失败')
        return
      }
      ElMessage.success('新增收款方式成功')
      await loadPaymentMethods()
      return
    }

    const response = await updatePaymentMethod(method.id, {
      name: normalizedName,
      enabled: method.enabled,
    })
    if (!response.success) {
      ElMessage.error(response.message || '更新收款方式失败')
      return
    }

    ElMessage.success('更新收款方式成功')
    await loadPaymentMethods()
  } catch (error) {
    console.error('保存收款方式失败:', error)
    ElMessage.error('保存收款方式失败')
  }
}

const handleCancelEdit = (method: PaymentMethodItem) => {
  if (method.isNew) {
    availablePaymentMethods.value = availablePaymentMethods.value.filter((item) => item.id !== method.id)
    syncSelectAllState()
    return
  }

  method.name = method.originalName
  method.isEditing = false
}

const handleDeleteMethod = async (method: PaymentMethodItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除收款方式“${method.name}”吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    if (method.isNew) {
      availablePaymentMethods.value = availablePaymentMethods.value.filter((item) => item.id !== method.id)
      syncSelectAllState()
      return
    }

    const response = await deletePaymentMethod(method.id)
    if (!response.success) {
      ElMessage.error(response.message || '删除失败')
      return
    }

    ElMessage.success('删除成功')
    await loadPaymentMethods()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除收款方式失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleDeleteSelected = async () => {
  if (selectedCount.value === 0) {
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedCount.value} 个收款方式吗？`, '批量删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const selectedMethods = availablePaymentMethods.value.filter((method) => method.selected)

    for (const method of selectedMethods) {
      if (method.isNew) {
        continue
      }
      const response = await deletePaymentMethod(method.id)
      if (!response.success) {
        throw new Error(response.message || `删除失败: ${method.name}`)
      }
    }

    availablePaymentMethods.value = availablePaymentMethods.value.filter(
      (method) => !(method.selected && method.isNew),
    )

    selectAllMethods.value = false
    ElMessage.success('批量删除成功')
    await loadPaymentMethods()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '批量删除失败')
    }
  }
}

onMounted(() => {
  loadPaymentMethods()
})
</script>

<style scoped>
.payment-methods {
  padding: 20px;
  background: white;
  min-height: 100%;
}

.info-alert {
  margin-bottom: 24px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
}

.info-content p {
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.payment-section {
  margin-bottom: 32px;
}

.subsection-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 16px;
}

.subtitle-desc {
  font-size: 13px;
  font-weight: normal;
  color: #999;
  margin-left: 8px;
}

.payment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.payment-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.select-checkbox {
  display: flex;
  align-items: center;
}

.payment-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.default-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 50px;
  height: 24px;
  background: #1890ff;
  color: white;
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  transform-origin: top right;
  transform: rotate(45deg) translate(14px, -14px);
  z-index: 1;
}

.drag-handle {
  display: flex;
  align-items: center;
  color: #999;
}

.card-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.method-name {
  font-size: 14px;
  color: #333;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-icon {
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-icon.primary {
  color: #409eff;
}

.action-icon.primary:hover {
  color: #66b1ff;
  transform: scale(1.1);
}

.action-icon.success {
  color: #52c41a;
}

.action-icon.success:hover {
  color: #73d13d;
  transform: scale(1.1);
}

.action-icon.danger {
  color: #ff4d4f;
}

.action-icon.danger:hover {
  color: #ff7875;
  transform: scale(1.1);
}
</style>
