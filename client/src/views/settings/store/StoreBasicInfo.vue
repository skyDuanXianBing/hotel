<template>
  <div class="store-basic-info-container">
    <!-- 标题和编辑按钮 -->
    <div class="page-header">
      <h2 class="page-title">{{ t('settingsStage4.storeBasic.title') }}</h2>
      <el-button type="primary" @click="handleEdit">{{ t('settings.common.edit') }}</el-button>
    </div>

    <!-- 信息展示卡片 -->
    <div class="info-card">
      <div class="info-grid">
        <!-- 第一行 -->
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.name') }}</span>
          <span class="info-value">{{ storeInfo.name }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.phone') }}</span>
          <span class="info-value">{{ storeInfo.phone }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.type') }}</span>
          <span class="info-value">{{ formatPropertyType(storeInfo.type) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.timezone') }}</span>
          <span class="info-value">{{ storeInfo.timezone }}</span>
        </div>

        <!-- 第二行 -->
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.storeId') }}</span>
          <span class="info-value">{{ storeInfo.id }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.manager') }}</span>
          <span class="info-value">{{ storeInfo.manager }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.ownerEmail') }}</span>
          <span class="info-value">{{ storeInfo.ownerEmail }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.createdAt') }}</span>
          <span class="info-value">{{ storeInfo.createdAt }}</span>
        </div>

        <!-- 第三行 -->
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.address') }}</span>
          <span class="info-value">{{ storeInfo.address }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.city') }}</span>
          <span class="info-value">{{ storeInfo.city }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.country') }}</span>
          <span class="info-value">{{ formatCountry(storeInfo.country) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('settingsStage4.storeBasic.fields.currency') }}</span>
          <span class="info-value">{{ storeInfo.currency }}</span>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="t('settingsStage4.storeBasic.dialog.editTitle')"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" :rules="formRules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.name')" prop="name">
              <el-input v-model="editForm.name" :placeholder="t('settingsStage4.storeBasic.placeholders.name')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.phone')" prop="phone">
              <el-input v-model="editForm.phone" :placeholder="t('settingsStage4.storeBasic.placeholders.phone')" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.type')" prop="type">
              <el-select v-model="editForm.type" :placeholder="t('settingsStage4.storeBasic.placeholders.type')" style="width: 100%">
                <el-option :label="t('settingsStage4.storeBasic.typeOptions.ryokan')" value="RYOKAN" />
                <el-option :label="t('settingsStage4.storeBasic.typeOptions.hotel')" value="HOTEL" />
                <el-option :label="t('settingsStage4.storeBasic.typeOptions.homestay')" value="HOMESTAY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.timezone')" prop="timezone">
              <el-select v-model="editForm.timezone" :placeholder="t('settingsStage4.storeBasic.placeholders.timezone')" style="width: 100%">
                <el-option label="Asia/Tokyo" value="Asia/Tokyo" />
                <el-option label="Asia/Shanghai" value="Asia/Shanghai" />
                <el-option label="Asia/Hong_Kong" value="Asia/Hong_Kong" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.manager')" prop="manager">
              <el-input v-model="editForm.manager" :placeholder="t('settingsStage4.storeBasic.placeholders.manager')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.ownerEmail')" prop="ownerEmail">
              <el-input v-model="editForm.ownerEmail" :placeholder="t('settingsStage4.storeBasic.placeholders.ownerEmail')" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="t('settingsStage4.storeBasic.fields.address')" prop="address">
          <el-input v-model="editForm.address" :placeholder="t('settingsStage4.storeBasic.placeholders.address')" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.city')" prop="city">
              <el-input v-model="editForm.city" :placeholder="t('settingsStage4.storeBasic.placeholders.city')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.country')" prop="country">
              <el-select v-model="editForm.country" :placeholder="t('settingsStage4.storeBasic.placeholders.country')" style="width: 100%">
                <el-option label="Japan" value="Japan" />
                <el-option label="China" value="China" />
                <el-option label="Korea" value="Korea" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="t('settingsStage4.storeBasic.fields.currency')" prop="currency">
          <el-select v-model="editForm.currency" :placeholder="t('settingsStage4.storeBasic.placeholders.currency')" style="width: 100%">
            <el-option label="JPY" value="JPY" />
            <el-option label="CNY" value="CNY" />
            <el-option label="USD" value="USD" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEdit">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleSaveEdit">{{ t('settings.common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getStoreById, updateStore, type StoreDTO } from '@/api/store'
import { useStoreStore } from '@/stores/store'
import {
  COUNTRY_OPTIONS,
  PROPERTY_TYPE_OPTIONS,
  getStoreOptionLabel,
} from '@/constants/storeOptions'

interface StoreInfo {
  id: string
  name: string
  phone: string
  type: string
  timezone: string
  manager: string
  ownerEmail: string
  createdAt: string
  address: string
  city: string
  country: string
  currency: string
}

const editDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const loading = ref(false)
const { t, locale } = useI18n()

// 使用 Pinia store 获取当前门店
const storeStore = useStoreStore()
const currentStoreId = computed(() => storeStore.currentStore?.id)

const storeInfo = ref<StoreInfo>({
  id: '',
  name: '',
  phone: '',
  type: '',
  timezone: '',
  manager: '',
  ownerEmail: '',
  createdAt: '',
  address: '',
  city: '',
  country: '',
  currency: '',
})

const formatPropertyType = (value?: string) => {
  return getStoreOptionLabel(PROPERTY_TYPE_OPTIONS, value, t)
}

const formatCountry = (value?: string) => {
  return getStoreOptionLabel(COUNTRY_OPTIONS, value, t)
}

const editForm = reactive<StoreInfo>({
  id: '',
  name: '',
  phone: '',
  type: '',
  timezone: '',
  manager: '',
  ownerEmail: '',
  createdAt: '',
  address: '',
  city: '',
  country: '',
  currency: '',
})

const formRules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('settingsStage4.storeBasic.validation.nameRequired'), trigger: 'blur' }],
  phone: [{ required: true, message: t('settingsStage4.storeBasic.validation.phoneRequired'), trigger: 'blur' }],
  type: [{ required: true, message: t('settingsStage4.storeBasic.validation.typeRequired'), trigger: 'change' }],
  timezone: [{ required: true, message: t('settingsStage4.storeBasic.validation.timezoneRequired'), trigger: 'change' }],
  manager: [{ required: true, message: t('settingsStage4.storeBasic.validation.managerRequired'), trigger: 'blur' }],
  ownerEmail: [
    { required: true, message: t('settingsStage4.storeBasic.validation.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('settingsStage4.storeBasic.validation.emailInvalid'), trigger: 'blur' },
  ],
  address: [{ required: true, message: t('settingsStage4.storeBasic.validation.addressRequired'), trigger: 'blur' }],
  city: [{ required: true, message: t('settingsStage4.storeBasic.validation.cityRequired'), trigger: 'blur' }],
  country: [{ required: true, message: t('settingsStage4.storeBasic.validation.countryRequired'), trigger: 'change' }],
  currency: [{ required: true, message: t('settingsStage4.storeBasic.validation.currencyRequired'), trigger: 'change' }],
}))

const handleEdit = () => {
  Object.assign(editForm, storeInfo.value)
  editDialogVisible.value = true
}

const handleCancelEdit = () => {
  editDialogVisible.value = false
}

// 加载门店信息
const loadStoreInfo = async () => {
  if (!currentStoreId.value) {
    ElMessage.warning(t('settingsStage4.storeBasic.messages.selectStore'))
    return
  }

  try {
    loading.value = true
    const response = await getStoreById(currentStoreId.value)
    if (response.success && response.data) {
      const data = response.data
      storeInfo.value = {
        id: data.id.toString(),
        name: data.name,
        phone: data.phone || '',
        type: data.type || '',
        timezone: data.timezone || '',
        manager: data.manager || '',
        ownerEmail: data.ownerEmail || '',
        createdAt: data.createdAt ? new Date(data.createdAt).toLocaleDateString(locale.value) : '',
        address: data.address || '',
        city: data.city || '',
        country: data.country || '',
        currency: data.currency || '',
      }
    } else {
      ElMessage.error(response.message || t('settingsStage4.storeBasic.messages.loadFailed'))
    }
  } catch (error) {
    console.error('加载门店信息失败:', error)
    ElMessage.error(t('settingsStage4.storeBasic.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

const handleSaveEdit = async () => {
  if (!currentStoreId.value) {
    ElMessage.warning(t('settingsStage4.storeBasic.messages.selectStore'))
    return
  }

  try {
    const valid = await formRef.value?.validate()
    if (valid) {
      loading.value = true
      const response = await updateStore(currentStoreId.value, {
        name: editForm.name,
        phone: editForm.phone,
        type: editForm.type,
        timezone: editForm.timezone,
        manager: editForm.manager,
        ownerEmail: editForm.ownerEmail,
        address: editForm.address,
        city: editForm.city,
        country: editForm.country,
        currency: editForm.currency,
      })

      if (response.success) {
        ElMessage.success(t('settingsStage4.storeBasic.messages.saveSuccess'))
        editDialogVisible.value = false
        // 重新加载数据
        await loadStoreInfo()
      } else {
        ElMessage.error(response.message || t('settingsStage4.storeBasic.messages.saveFailed'))
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(t('settingsStage4.storeBasic.messages.saveFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStoreInfo()
})
</script>

<style scoped>
.store-basic-info-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 100px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.info-card {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-label {
  font-size: 12px;
  color: #909399;
}

.info-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}
</style>
