<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  adjustAdminQuota,
  getAdminQuotaUsage,
  listAdminFeatures,
  type AdminFeature,
  type AdminQuotaUsage,
} from '@/api/admin'
import AdminStoreSelect from '@/components/admin/AdminStoreSelect.vue'
import { getAdminErrorMessage } from '@/utils/adminRequest'

const { t } = useI18n()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const features = ref<AdminFeature[]>([])
const result = ref<AdminQuotaUsage | null>(null)

// P4 修复：调整前展示当前用量（总额/已用/剩余/周期），避免“盲调”
const usage = ref<AdminQuotaUsage | null>(null)
const usageEmptyText = ref('')
const usageLoading = ref(false)
let usageSeq = 0

const form = reactive({
  storeId: undefined as number | undefined,
  featureCode: '',
  delta: undefined as number | undefined,
  remark: '',
})

// 配额调整仅对 QUOTA 型权益有意义（BOOLEAN 无账户、CAPACITY 实时 COUNT 不记账）
const quotaFeatures = computed(() => features.value.filter((feature) => feature.type === 'QUOTA'))

const rules: FormRules = {
  storeId: [
    { required: true, message: t('admin.quota.validation.storeIdRequired'), trigger: 'change' },
  ],
  featureCode: [
    { required: true, message: t('admin.quota.validation.featureRequired'), trigger: 'change' },
  ],
  delta: [
    { required: true, message: t('admin.quota.validation.deltaRequired'), trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (typeof value === 'number' && value === 0) {
          callback(new Error(t('admin.quota.validation.deltaNotZero')))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const formatDateTime = (value: string | null) =>
  value ? value.slice(0, 19).replace('T', ' ') : '-'

/**
 * 选定门店 + 功能后自动拉取当前用量；门店/功能切换时重新拉取。
 * 无有效订阅 / 订阅不含该配额权益时后端返回 data=null + message，展示友好空态。
 * usageSeq 防止慢响应覆盖新选择。
 * P10：查询前先清空旧用量与旧空态，避免慢响应期间展示陈旧数据。
 */
const loadUsage = async () => {
  const storeId = form.storeId
  const featureCode = form.featureCode
  if (storeId === undefined || !featureCode) {
    usageSeq += 1
    usage.value = null
    usageEmptyText.value = ''
    return
  }
  const seq = ++usageSeq
  usage.value = null
  usageEmptyText.value = ''
  usageLoading.value = true
  try {
    const response = await getAdminQuotaUsage({ storeId, featureCode })
    if (seq !== usageSeq) {
      return
    }
    if (!response.success) {
      throw new Error(response.message || t('admin.common.loadFailed'))
    }
    usage.value = response.data ?? null
    usageEmptyText.value = response.data ? '' : response.message || t('admin.quota.usageEmpty')
  } catch (error) {
    if (seq !== usageSeq) {
      return
    }
    usage.value = null
    usageEmptyText.value = ''
    ElMessage.error(getAdminErrorMessage(error))
  } finally {
    if (seq === usageSeq) {
      usageLoading.value = false
    }
  }
}

watch([() => form.storeId, () => form.featureCode], () => {
  // P10：门店/功能变化总是重新查询；上一选择的调整结果随之失效，避免陈旧结果残留
  result.value = null
  void loadUsage()
})

const loadFeatures = async () => {
  try {
    const response = await listAdminFeatures()
    if (response.success && Array.isArray(response.data)) {
      features.value = response.data
      if (!form.featureCode && quotaFeatures.value.length) {
        form.featureCode = quotaFeatures.value[0].featureCode
      }
    }
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error))
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || form.storeId === undefined || form.delta === undefined) {
    return
  }

  submitting.value = true
  result.value = null
  try {
    const response = await adjustAdminQuota({
      storeId: form.storeId,
      featureCode: form.featureCode,
      delta: form.delta,
      remark: form.remark.trim() || undefined,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || t('admin.common.saveFailed'))
    }
    result.value = response.data
    // 调整成功即刷新“当前用量”卡片（调整响应本就是最新用量视图）
    usage.value = response.data
    usageEmptyText.value = ''
    ElMessage.success(response.message || t('admin.quota.adjustSuccess'))
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error, 'admin.common.saveFailed'))
  } finally {
    submitting.value = false
  }
}

onMounted(loadFeatures)
</script>

<template>
  <div class="admin-quota">
    <div class="page-header">
      <h2>{{ t('admin.quota.title') }}</h2>
      <p class="page-description">{{ t('admin.quota.description') }}</p>
    </div>

    <div class="quota-panels">
      <el-card class="quota-form-card">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item :label="t('admin.quota.form.storeId')" prop="storeId">
            <AdminStoreSelect v-model="form.storeId" :disabled="submitting" />
          </el-form-item>
          <el-form-item :label="t('admin.quota.form.featureCode')" prop="featureCode">
            <el-select v-model="form.featureCode" :disabled="submitting" class="full-width">
              <el-option
                v-for="feature in quotaFeatures"
                :key="feature.featureCode"
                :value="feature.featureCode"
                :label="`${feature.name}（${feature.featureCode}）`"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('admin.quota.form.delta')" prop="delta">
            <el-input-number
              v-model="form.delta"
              :precision="0"
              controls-position="right"
              :disabled="submitting"
            />
            <p class="field-hint">{{ t('admin.quota.deltaHint') }}</p>
          </el-form-item>
          <el-form-item :label="t('admin.quota.form.remark')">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              :disabled="submitting"
            />
          </el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ t('admin.quota.submit') }}
          </el-button>
        </el-form>
      </el-card>

      <div class="quota-side">
        <el-card v-if="form.storeId !== undefined && form.featureCode" class="quota-usage-card">
          <template #header>
            <div class="usage-card-header">
              <span>{{ t('admin.quota.usageTitle') }}</span>
              <el-button
                link
                type="primary"
                :icon="Refresh"
                :loading="usageLoading"
                @click="loadUsage"
              >
                {{ t('admin.quota.refresh') }}
              </el-button>
            </div>
          </template>
          <div v-loading="usageLoading">
            <el-alert v-if="usageEmptyText" type="info" :title="usageEmptyText" :closable="false" />
            <el-descriptions v-else-if="usage" :column="1" border>
              <el-descriptions-item :label="t('admin.quota.result.featureCode')">
                <span class="feature-code">{{ usage.featureCode }}</span>
              </el-descriptions-item>
              <el-descriptions-item :label="t('admin.quota.result.totalQuota')">
                {{ usage.totalQuota === null ? t('admin.quota.unlimited') : usage.totalQuota }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('admin.quota.result.usedQuota')">
                {{ usage.usedQuota }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('admin.quota.result.remaining')">
                <span class="remaining-value">
                  {{ usage.remaining === null ? t('admin.quota.unlimited') : usage.remaining }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item :label="t('admin.quota.result.period')">
                {{ formatDateTime(usage.periodStart) }} ~ {{ formatDateTime(usage.periodEnd) }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>

        <el-card v-if="result" class="quota-result-card">
          <template #header>
            <span>{{ t('admin.quota.resultTitle') }}</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="t('admin.quota.result.featureCode')">
              <span class="feature-code">{{ result.featureCode }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('admin.quota.result.totalQuota')">
              {{ result.totalQuota === null ? t('admin.quota.unlimited') : result.totalQuota }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('admin.quota.result.usedQuota')">
              {{ result.usedQuota }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('admin.quota.result.remaining')">
              <span class="remaining-value">
                {{ result.remaining === null ? t('admin.quota.unlimited') : result.remaining }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('admin.quota.result.period')">
              {{ formatDateTime(result.periodStart) }} ~ {{ formatDateTime(result.periodEnd) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
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

.quota-panels {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(320px, 1fr);
  gap: 16px;
  align-items: start;
}

.quota-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: 900px) {
  .quota-panels {
    grid-template-columns: 1fr;
  }
}

.full-width {
  width: 100%;
}

.usage-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.field-hint {
  margin: 4px 0 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.feature-code {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
}

.remaining-value {
  color: #67c23a;
  font-weight: 700;
}
</style>
