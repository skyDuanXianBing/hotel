<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createAdminPackage,
  listAdminFeatures,
  listAdminPackageFeatures,
  listAdminPackages,
  replaceAdminPackageFeatures,
  updateAdminPackage,
  updateAdminPackageStatus,
  type AdminFeature,
  type AdminPackage,
  type AdminPackageFeatureItem,
} from '@/api/admin'
import { getAdminErrorMessage } from '@/utils/adminRequest'

const { t, te } = useI18n()

const loading = ref(true)
const packages = ref<AdminPackage[]>([])
const features = ref<AdminFeature[]>([])

/**
 * 计费周期枚举文案：未知/空值回退为原始值或 '-'，避免动态 key 缺包时产生 i18n missing-key 警告
 * （验收 F5：admin.enums.period.undefined 控制台警告）。
 */
const periodLabel = (period: string | null | undefined) => {
  const key = `admin.enums.period.${period}`
  return period && te(key) ? t(key) : (period ?? '-')
}

// ---------------- 新建 / 编辑套餐 ----------------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingPackage = ref<AdminPackage | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  version: 1,
  price: 0,
  period: 'MONTH' as 'MONTH' | 'YEAR',
  description: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: t('admin.packages.validation.nameRequired'), trigger: 'blur' }],
  version: [
    { required: true, message: t('admin.packages.validation.versionRequired'), trigger: 'blur' },
  ],
  price: [
    { required: true, message: t('admin.packages.validation.priceRequired'), trigger: 'blur' },
  ],
  period: [
    { required: true, message: t('admin.packages.validation.periodRequired'), trigger: 'change' },
  ],
}

const statusTogglingId = ref<number | null>(null)

// ---------------- 权益配置抽屉 ----------------
interface FeatureDraft {
  feature: AdminFeature
  checked: boolean
  unlimited: boolean
  quotaLimit: number
}

const drawerVisible = ref(false)
const drawerPackage = ref<AdminPackage | null>(null)
const drawerLoading = ref(false)
const drawerSaving = ref(false)
const featureDrafts = ref<FeatureDraft[]>([])

const formatDateTime = (value: string | null) =>
  value ? value.slice(0, 19).replace('T', ' ') : '-'

const loadAll = async () => {
  loading.value = true
  try {
    const [packagesResponse, featuresResponse] = await Promise.all([
      listAdminPackages(),
      listAdminFeatures(),
    ])
    if (!packagesResponse.success) {
      throw new Error(packagesResponse.message || t('admin.common.loadFailed'))
    }
    packages.value = Array.isArray(packagesResponse.data) ? packagesResponse.data : []
    if (featuresResponse.success && Array.isArray(featuresResponse.data)) {
      features.value = featuresResponse.data
    }
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error))
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  editingPackage.value = null
  form.name = ''
  form.version = 1
  form.price = 0
  form.period = 'MONTH'
  form.description = ''
  dialogVisible.value = true
}

const openEditDialog = (pkg: AdminPackage) => {
  dialogMode.value = 'edit'
  editingPackage.value = pkg
  form.name = pkg.name
  form.version = pkg.version
  form.price = pkg.price
  form.period = pkg.period
  form.description = pkg.description || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      version: form.version,
      price: form.price,
      period: form.period,
      description: form.description.trim() || null,
    }
    const response =
      dialogMode.value === 'create'
        ? await createAdminPackage(payload)
        : await updateAdminPackage(editingPackage.value!.id, payload)
    if (!response.success) {
      throw new Error(response.message || t('admin.common.saveFailed'))
    }
    ElMessage.success(response.message || t('admin.common.saveSuccess'))
    dialogVisible.value = false
    await loadAll()
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error, 'admin.common.saveFailed'))
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (pkg: AdminPackage) => {
  const nextStatus = pkg.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
  // 下架为零确认风险操作（P2）：新客不可购买，需显式确认；上架为非破坏性恢复操作，无需确认
  if (nextStatus === 'OFF_SHELF') {
    try {
      await ElMessageBox.confirm(
        t('admin.packages.offShelfConfirmMessage', { name: pkg.name }),
        t('admin.packages.offShelfConfirmTitle'),
        {
          confirmButtonText: t('admin.common.confirm'),
          cancelButtonText: t('admin.common.cancel'),
          type: 'warning',
        },
      )
    } catch {
      return
    }
  }
  statusTogglingId.value = pkg.id
  try {
    const response = await updateAdminPackageStatus(pkg.id, nextStatus)
    if (!response.success) {
      throw new Error(response.message || t('admin.common.saveFailed'))
    }
    ElMessage.success(response.message || t('admin.common.saveSuccess'))
    await loadAll()
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error, 'admin.common.saveFailed'))
  } finally {
    statusTogglingId.value = null
  }
}

const openFeaturesDrawer = async (pkg: AdminPackage) => {
  drawerPackage.value = pkg
  drawerVisible.value = true
  drawerLoading.value = true
  try {
    const response = await listAdminPackageFeatures(pkg.id)
    if (!response.success) {
      throw new Error(response.message || t('admin.common.loadFailed'))
    }
    const existing = new Map(
      (Array.isArray(response.data) ? response.data : []).map((item) => [item.featureCode, item]),
    )
    featureDrafts.value = features.value.map((feature) => {
      const current = existing.get(feature.featureCode)
      return {
        feature,
        checked: Boolean(current),
        unlimited: current ? current.quotaLimit === null : false,
        quotaLimit: current?.quotaLimit ?? 100,
      }
    })
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error))
    drawerVisible.value = false
  } finally {
    drawerLoading.value = false
  }
}

const needsQuotaLimit = (draft: FeatureDraft) =>
  draft.checked && draft.feature.type !== 'BOOLEAN' && !draft.unlimited

const drawerValid = computed(() =>
  featureDrafts.value.some(
    (draft) =>
      draft.checked &&
      (draft.feature.type === 'BOOLEAN' || draft.unlimited || draft.quotaLimit >= 0),
  ),
)

const handleSaveFeatures = async () => {
  if (!drawerPackage.value) {
    return
  }
  const items: AdminPackageFeatureItem[] = featureDrafts.value
    .filter((draft) => draft.checked)
    .map((draft) => ({
      featureCode: draft.feature.featureCode,
      quotaLimit: draft.feature.type === 'BOOLEAN' || draft.unlimited ? null : draft.quotaLimit,
    }))
  if (!items.length) {
    ElMessage.warning(t('admin.packages.features.emptyWarning'))
    return
  }

  drawerSaving.value = true
  try {
    const response = await replaceAdminPackageFeatures(drawerPackage.value.id, items)
    if (!response.success) {
      throw new Error(response.message || t('admin.common.saveFailed'))
    }
    ElMessage.success(response.message || t('admin.common.saveSuccess'))
    drawerVisible.value = false
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error, 'admin.common.saveFailed'))
  } finally {
    drawerSaving.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div v-loading="loading" class="admin-packages">
    <div class="page-header">
      <div>
        <h2>{{ t('admin.packages.title') }}</h2>
        <p class="page-description">{{ t('admin.packages.description') }}</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">
        {{ t('admin.packages.create') }}
      </el-button>
    </div>

    <el-card>
      <el-table :data="packages" row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" :label="t('admin.packages.columns.name')" min-width="120" />
        <el-table-column
          prop="version"
          :label="t('admin.packages.columns.version')"
          width="80"
          align="center"
        />
        <el-table-column :label="t('admin.packages.columns.price')" width="120">
          <template #default="{ row }"> ${{ row.price }}/{{ periodLabel(row.period) }} </template>
        </el-table-column>
        <el-table-column :label="t('admin.packages.columns.status')" width="100" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag :type="row.status === 'ON_SHELF' ? 'success' : 'info'" effect="plain">
                {{ t(`admin.enums.packageStatus.${row.status}`) }}
              </el-tag>
              <el-tag v-if="row.isSystem === true" type="warning" effect="plain">
                {{ t('admin.packages.systemTag') }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="description"
          :label="t('admin.packages.columns.description')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column :label="t('admin.packages.columns.createdAt')" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.common.actions')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">
              {{ t('admin.common.edit') }}
            </el-button>
            <el-button link type="primary" @click="openFeaturesDrawer(row)">
              {{ t('admin.packages.configureFeatures') }}
            </el-button>
            <!-- 系统兜底套餐（isSystem）：永不在售，隐藏上架/下架按钮（后端同样拦截） -->
            <el-button
              v-if="row.isSystem !== true"
              link
              :type="row.status === 'ON_SHELF' ? 'warning' : 'success'"
              :loading="statusTogglingId === row.id"
              @click="handleToggleStatus(row)"
            >
              {{
                row.status === 'ON_SHELF'
                  ? t('admin.packages.offShelf')
                  : t('admin.packages.onShelf')
              }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="
        dialogMode === 'create' ? t('admin.packages.createTitle') : t('admin.packages.editTitle')
      "
      width="520px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item :label="t('admin.packages.form.name')" prop="name">
          <el-input v-model.trim="form.name" maxlength="64" :disabled="submitting" />
        </el-form-item>
        <div class="form-row">
          <el-form-item :label="t('admin.packages.form.version')" prop="version">
            <el-input-number
              v-model="form.version"
              :min="1"
              :precision="0"
              :disabled="submitting"
            />
          </el-form-item>
          <el-form-item :label="t('admin.packages.form.price')" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" :disabled="submitting" />
          </el-form-item>
          <el-form-item :label="t('admin.packages.form.period')" prop="period">
            <el-select v-model="form.period" :disabled="submitting">
              <el-option :label="t('admin.enums.period.MONTH')" value="MONTH" />
              <el-option :label="t('admin.enums.period.YEAR')" value="YEAR" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item :label="t('admin.packages.form.description')">
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

    <el-drawer
      v-model="drawerVisible"
      :title="t('admin.packages.features.title', { name: drawerPackage?.name || '' })"
      size="440px"
    >
      <div v-loading="drawerLoading" class="features-drawer">
        <p class="features-hint">{{ t('admin.packages.features.hint') }}</p>
        <div v-for="draft in featureDrafts" :key="draft.feature.featureCode" class="feature-row">
          <el-checkbox v-model="draft.checked" class="feature-checkbox">
            <span class="feature-name">{{ draft.feature.name }}</span>
            <span class="feature-code">{{ draft.feature.featureCode }}</span>
          </el-checkbox>
          <el-tag size="small" effect="plain">{{
            t(`admin.enums.featureType.${draft.feature.type}`)
          }}</el-tag>
          <template v-if="draft.checked && draft.feature.type !== 'BOOLEAN'">
            <div class="feature-quota">
              <el-checkbox v-model="draft.unlimited" size="small">
                {{ t('admin.packages.features.unlimited') }}
              </el-checkbox>
              <el-input-number
                v-if="needsQuotaLimit(draft)"
                v-model="draft.quotaLimit"
                :min="0"
                :precision="0"
                size="small"
              />
              <span v-if="needsQuotaLimit(draft) && draft.feature.unit" class="feature-unit">
                {{ draft.feature.unit }}
              </span>
            </div>
          </template>
        </div>

        <div class="drawer-footer">
          <el-button :disabled="drawerSaving" @click="drawerVisible = false">
            {{ t('admin.common.cancel') }}
          </el-button>
          <el-button
            type="primary"
            :loading="drawerSaving"
            :disabled="!drawerValid"
            @click="handleSaveFeatures"
          >
            {{ t('admin.common.save') }}
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.admin-packages {
  min-height: 200px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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

.form-row {
  display: flex;
  gap: 16px;
}

.status-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.features-drawer {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100%;
}

.features-hint {
  margin: 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.feature-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.feature-row .el-tag {
  align-self: flex-start;
}

.feature-checkbox :deep(.el-checkbox__label) {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
}

.feature-name {
  font-weight: 600;
}

.feature-code {
  color: #909399;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 11px;
}

.feature-quota {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 24px;
}

.feature-unit {
  color: #909399;
  font-size: 12px;
}

.drawer-footer {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
