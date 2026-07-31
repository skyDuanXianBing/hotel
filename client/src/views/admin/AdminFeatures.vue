<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import {
  listAdminFeatures,
  updateAdminFeature,
  type AdminFeature,
  type SaasQuotaResetCycle,
} from '@/api/admin'
import type { SaasFeatureType } from '@/api/billing'
import { getAdminErrorMessage } from '@/utils/adminRequest'

const { t } = useI18n()

const loading = ref(true)
const features = ref<AdminFeature[]>([])

const dialogVisible = ref(false)
const submitting = ref(false)
const editingFeature = ref<AdminFeature | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  type: 'BOOLEAN' as SaasFeatureType,
  unit: '',
  defaultResetCycle: '' as '' | SaasQuotaResetCycle,
  description: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: t('admin.features.validation.nameRequired'), trigger: 'blur' }],
  type: [
    { required: true, message: t('admin.features.validation.typeRequired'), trigger: 'change' },
  ],
}

const formatDateTime = (value: string | null) =>
  value ? value.slice(0, 19).replace('T', ' ') : '-'

const loadFeatures = async () => {
  loading.value = true
  try {
    const response = await listAdminFeatures()
    if (!response.success) {
      throw new Error(response.message || t('admin.common.loadFailed'))
    }
    features.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error))
  } finally {
    loading.value = false
  }
}

const openEditDialog = (feature: AdminFeature) => {
  editingFeature.value = feature
  form.name = feature.name
  form.type = feature.type
  form.unit = feature.unit || ''
  form.defaultResetCycle = feature.defaultResetCycle || ''
  form.description = feature.description || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !editingFeature.value) {
    return
  }

  submitting.value = true
  try {
    const response = await updateAdminFeature(editingFeature.value.id, {
      name: form.name.trim(),
      type: form.type,
      unit: form.type === 'BOOLEAN' ? null : form.unit.trim() || null,
      defaultResetCycle: form.type === 'QUOTA' ? form.defaultResetCycle || null : null,
      description: form.description.trim() || null,
    })
    if (!response.success) {
      throw new Error(response.message || t('admin.common.saveFailed'))
    }
    ElMessage.success(response.message || t('admin.common.saveSuccess'))
    dialogVisible.value = false
    await loadFeatures()
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error, 'admin.common.saveFailed'))
  } finally {
    submitting.value = false
  }
}

onMounted(loadFeatures)
</script>

<template>
  <div v-loading="loading" class="admin-features">
    <div class="page-header">
      <h2>{{ t('admin.features.title') }}</h2>
      <p class="page-description">{{ t('admin.features.description') }}</p>
    </div>

    <el-card>
      <el-table :data="features" row-key="id">
        <el-table-column
          prop="featureCode"
          :label="t('admin.features.columns.featureCode')"
          min-width="170"
        >
          <template #default="{ row }">
            <span class="feature-code">{{ row.featureCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="t('admin.features.columns.name')" min-width="140" />
        <el-table-column :label="t('admin.features.columns.type')" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{
              t(`admin.enums.featureType.${row.type}`)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="unit"
          :label="t('admin.features.columns.unit')"
          width="90"
          align="center"
        >
          <template #default="{ row }">{{ row.unit || '-' }}</template>
        </el-table-column>
        <el-table-column
          :label="t('admin.features.columns.defaultResetCycle')"
          width="130"
          align="center"
        >
          <template #default="{ row }">
            {{ row.defaultResetCycle ? t(`admin.enums.resetCycle.${row.defaultResetCycle}`) : '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="description"
          :label="t('admin.features.columns.description')"
          min-width="180"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.features.columns.updatedAt')" width="150">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.common.actions')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">
              {{ t('admin.common.edit') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="t('admin.features.editTitle')" width="560px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item :label="t('admin.features.columns.featureCode')">
          <el-input :model-value="editingFeature?.featureCode" disabled />
          <p class="field-hint">{{ t('admin.features.codeReadonlyHint') }}</p>
        </el-form-item>
        <el-form-item :label="t('admin.features.form.name')" prop="name">
          <el-input v-model.trim="form.name" maxlength="64" :disabled="submitting" />
        </el-form-item>
        <!-- 类型+单位一行两列（修前三列挤在一行：类型显示“容..”、周期显示“请选...”）；
             单位仅 QUOTA/CAPACITY 有意义，BOOLEAN 隐藏；重置周期仅 QUOTA 需要（见下行显隐）。 -->
        <div class="form-row">
          <el-form-item :label="t('admin.features.form.type')" prop="type" class="form-col">
            <el-select v-model="form.type" :disabled="submitting" class="form-col-control">
              <el-option :label="t('admin.enums.featureType.BOOLEAN')" value="BOOLEAN" />
              <el-option :label="t('admin.enums.featureType.QUOTA')" value="QUOTA" />
              <el-option :label="t('admin.enums.featureType.CAPACITY')" value="CAPACITY" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.type !== 'BOOLEAN'" :label="t('admin.features.form.unit')" class="form-col">
            <el-input v-model.trim="form.unit" maxlength="16" :disabled="submitting" />
          </el-form-item>
        </div>
        <el-form-item v-if="form.type === 'QUOTA'" :label="t('admin.features.form.defaultResetCycle')">
          <el-select
            v-model="form.defaultResetCycle"
            clearable
            :disabled="submitting"
            class="form-col-control"
          >
            <el-option :label="t('admin.enums.resetCycle.MONTHLY')" value="MONTHLY" />
            <el-option :label="t('admin.enums.resetCycle.NONE')" value="NONE" />
          </el-select>
          <p class="field-hint">{{ t('admin.features.form.resetCycleQuotaHint') }}</p>
        </el-form-item>
        <el-form-item :label="t('admin.features.form.description')">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            :disabled="submitting"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="submitting" @click="dialogVisible = false">
          {{ t('admin.common.cancel') }}
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ t('admin.common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-features {
  min-height: 200px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 6px;
  color: #303133;
  font-size: 18px;
}

.page-description {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.feature-code {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-col {
  flex: 1;
  min-width: 0;
}

.form-col-control {
  width: 100%;
}

.field-hint {
  margin: 4px 0 0;
  color: #909399;
  font-size: 12px;
}
</style>
