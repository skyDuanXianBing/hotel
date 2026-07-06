<template>
  <div class="payment-methods" v-loading="loading">
    <el-alert type="info" :closable="false" class="info-alert">
      <template #default>
        <div class="info-content">
          <p>1. {{ t('settings.paymentMethods.tips.persistence') }}</p>
          <p>2. {{ t('settings.paymentMethods.tips.default') }}</p>
        </div>
      </template>
    </el-alert>

    <div class="section-header">
      <h3 class="section-title">{{ t('settings.paymentMethods.title') }}</h3>
      <div class="header-actions">
        <el-checkbox v-model="selectAllMethods" @change="handleToggleSelectAll">
          {{ t('settings.paymentMethods.selectAll') }}
        </el-checkbox>
        <el-button type="danger" plain :disabled="selectedCount === 0" @click="handleDeleteSelected">
          {{ t('settings.paymentMethods.deleteSelected', { count: selectedCount }) }}
        </el-button>
        <el-button type="primary" @click="handleAddPaymentMethod">
          {{ t('settings.paymentMethods.add') }}
        </el-button>
      </div>
    </div>

    <div class="payment-section">
      <h4 class="subsection-title">
        {{ t('settings.paymentMethods.available') }}
        <span class="subtitle-desc">{{ t('settings.paymentMethods.availableDesc') }}</span>
      </h4>

      <div class="payment-grid">
        <div v-for="method in availablePaymentMethods" :key="method.id" class="payment-card">
          <div v-if="method.isDefault && !method.isEditing" class="default-corner">
            {{ t('settings.common.default') }}
          </div>

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
              :placeholder="t('settings.paymentMethods.namePlaceholder')"
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
              <img
                class="action-image edit-image"
                :src="paymentMethodEditIcon"
                alt=""
                @click="handleEditMethod(method)"
              />
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
import { useI18n } from 'vue-i18n'
import { Grid, Check, Close, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createPaymentMethod,
  deletePaymentMethod,
  getAllPaymentMethods,
  updatePaymentMethod,
  type PaymentMethodDTO,
} from '@/api/paymentMethod'
import paymentMethodEditIcon from '@/assets/icons/payment-method-edit.png'

const { t } = useI18n()

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
      ElMessage.error(response.message || t('settings.paymentMethods.fetchFailed'))
      return
    }
    availablePaymentMethods.value = mapToViewModel(response.data || [])
    syncSelectAllState()
  } catch (error) {
    console.error('加载收款方式失败:', error)
    ElMessage.error(t('settings.paymentMethods.loadFailed'))
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
    ElMessage.warning(t('settings.paymentMethods.nameRequired'))
    return
  }

  try {
    if (method.isNew) {
      const response = await createPaymentMethod({
        name: normalizedName,
        enabled: method.enabled,
      })
      if (!response.success) {
        ElMessage.error(response.message || t('settings.paymentMethods.createFailed'))
        return
      }
      ElMessage.success(t('settings.paymentMethods.createSuccess'))
      await loadPaymentMethods()
      return
    }

    const response = await updatePaymentMethod(method.id, {
      name: normalizedName,
      enabled: method.enabled,
    })
    if (!response.success) {
      ElMessage.error(response.message || t('settings.paymentMethods.updateFailed'))
      return
    }

    ElMessage.success(t('settings.paymentMethods.updateSuccess'))
    await loadPaymentMethods()
  } catch (error) {
    console.error('保存收款方式失败:', error)
    ElMessage.error(t('settings.paymentMethods.saveFailed'))
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
    await ElMessageBox.confirm(
      t('settings.paymentMethods.deleteConfirm', { name: method.name }),
      t('settings.common.deleteConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirmButton'),
        cancelButtonText: t('settings.common.cancelButton'),
        type: 'warning',
      },
    )

    if (method.isNew) {
      availablePaymentMethods.value = availablePaymentMethods.value.filter((item) => item.id !== method.id)
      syncSelectAllState()
      return
    }

    const response = await deletePaymentMethod(method.id)
    if (!response.success) {
      ElMessage.error(response.message || t('settings.common.deleteFailed'))
      return
    }

    ElMessage.success(t('settings.common.deleteSuccess'))
    await loadPaymentMethods()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除收款方式失败:', error)
      ElMessage.error(t('settings.common.deleteFailed'))
    }
  }
}

const handleDeleteSelected = async () => {
  if (selectedCount.value === 0) {
    return
  }

  try {
    await ElMessageBox.confirm(
      t('settings.paymentMethods.batchDeleteConfirm', { count: selectedCount.value }),
      t('settings.common.batchDeleteConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirmButton'),
        cancelButtonText: t('settings.common.cancelButton'),
        type: 'warning',
      },
    )

    const selectedMethods = availablePaymentMethods.value.filter((method) => method.selected)

    for (const method of selectedMethods) {
      if (method.isNew) {
        continue
      }
      const response = await deletePaymentMethod(method.id)
      if (!response.success) {
        throw new Error(
          response.message ||
            t('settings.paymentMethods.batchDeleteItemFailed', { name: method.name }),
        )
      }
    }

    availablePaymentMethods.value = availablePaymentMethods.value.filter(
      (method) => !(method.selected && method.isNew),
    )

    selectAllMethods.value = false
    ElMessage.success(t('settings.common.batchDeleteSuccess'))
    await loadPaymentMethods()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || t('settings.common.batchDeleteFailed'))
    }
  }
}

onMounted(() => {
  loadPaymentMethods()
})
</script>

<style scoped>
.payment-methods {
  padding: 25px 24px 40px;
  background: #fff;
  min-height: 100%;
}

.info-alert {
  margin-bottom: 28px;
  padding: 7px 22px;
  background: rgba(89, 126, 247, 0.15);
  border: 1px solid rgba(89, 126, 247, 0.66);
  border-radius: 4px;
}

.info-alert :deep(.el-alert__content) {
  padding: 0;
}

.info-alert :deep(.el-alert__description),
.info-alert :deep(.el-alert__title) {
  color: #597ef7;
}

.info-content p {
  margin: 0;
  font-size: 16px;
  line-height: 30px;
  color: #597ef7;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e5e5;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions :deep(.el-button) {
  height: 30px;
  padding: 0 16px;
  border-radius: 4px;
}

.header-actions :deep(.el-button--danger.is-plain) {
  background: rgba(245, 63, 63, 0.12);
  border-color: transparent;
  color: #f56c6c;
}

.header-actions :deep(.el-button--danger.is-plain.is-disabled) {
  background: rgba(245, 63, 63, 0.12);
  border-color: transparent;
  color: rgba(245, 108, 108, 0.45);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 25px;
  color: #111;
  margin: 0;
}

.payment-section {
  margin-bottom: 32px;
}

.subsection-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
  color: #111;
  margin: 0 0 31px;
}

.subtitle-desc {
  font-size: 13px;
  font-weight: normal;
  color: #8c8c8c;
  margin-left: 16px;
}

.payment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 6px;
  align-items: start;
  justify-content: start;
}

.payment-card {
  position: relative;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  width: 100%;
  height: 68px;
  gap: 8px;
  padding: 0 16px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
  overflow: hidden;
}

.select-checkbox {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
}

.select-checkbox :deep(.el-checkbox) {
  height: 16px;
}

.select-checkbox :deep(.el-checkbox__inner) {
  width: 14px;
  height: 14px;
}

.payment-card:hover {
  border-color: #bfc7d9;
  box-shadow: 0 2px 8px rgba(17, 24, 39, 0.06);
}

.default-corner {
  position: absolute;
  top: 4px;
  right: 8px;
  height: 18px;
  padding: 0 6px;
  background: rgba(89, 126, 247, 0.12);
  color: #597ef7;
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  z-index: 1;
}

.drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 14px;
  color: #9b9b9b;
  font-size: 14px;
}

.card-content {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
}

.card-content :deep(.el-input) {
  width: 100%;
}

.method-name {
  max-width: 104px;
  font-size: 16px;
  line-height: 18px;
  color: #7a7a7a;
  word-break: break-word;
}

.card-actions {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 6px;
}

.action-icon {
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-image {
  display: block;
  width: 16px;
  height: 16px;
  cursor: pointer;
  object-fit: contain;
  transition: transform 0.2s ease;
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

.edit-image:hover,
.action-icon.danger:hover {
  transform: scale(1.1);
}

.action-icon.danger:hover {
  color: #ff7875;
}

@media (max-width: 768px) {
  .payment-methods {
    padding: 20px 16px 32px;
  }

  .section-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .payment-grid {
    grid-template-columns: 1fr;
  }

  .payment-card {
    width: 100%;
  }
}
</style>
