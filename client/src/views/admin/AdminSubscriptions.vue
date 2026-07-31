<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  cancelAdminSubscription,
  grantAdminSubscription,
  listAdminPackages,
  listAdminSubscriptions,
  type AdminPackage,
  type AdminSubscriptionView,
} from '@/api/admin'
import AdminStoreSelect from '@/components/admin/AdminStoreSelect.vue'
import { getAdminErrorMessage } from '@/utils/adminRequest'
import {
  buildGrantPayload,
  CUSTOM_DURATION_DAYS_MAX,
  CUSTOM_DURATION_DAYS_MIN,
  defaultDurationMode,
  isEffectivelyExpired,
  remainingDays,
  type GrantDurationMode,
} from '@/utils/subscriptionAdjustment'
import { resolvePackageDisplayName } from '@/utils/saasDisplay'

const { t, te } = useI18n()

/**
 * 计费周期枚举文案：未知/空值回退为原始值或 '-'，避免动态 key 缺包时产生 i18n missing-key 警告
 * （验收 F5：admin.enums.period.undefined 控制台警告）。
 */
const periodLabel = (period: string | null | undefined) => {
  const key = `admin.enums.period.${period}`
  return period && te(key) ? t(key) : (period ?? '-')
}

const packageDisplayName = (name: string) => resolvePackageDisplayName(t, te, name)

const loading = ref(true)
const subscriptions = ref<AdminSubscriptionView[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filterStoreId = ref<number | undefined>(undefined)

// ---------------- 人工开通/调整会员等级 ----------------
const grantDialogVisible = ref(false)
const grantSubmitting = ref(false)
const grantFormRef = ref<FormInstance>()
const packages = ref<AdminPackage[]>([])
// 弹窗级幂等键：每次打开弹窗生成；提交失败弹窗保持打开，重试复用同键（服务端幂等重放，不重复人工订单）；
// 成功后关闭，重新打开生成新键。
const grantIdempotencyKey = ref('')
const grantForm = reactive({
  storeId: undefined as number | undefined,
  packageId: undefined as number | undefined,
  durationMode: 'PERIOD' as GrantDurationMode,
  durationDays: 30 as number,
  remark: '',
})

/** 选定门店的当前生效订阅（弹窗内自动查询）：null=当前无订阅。 */
const currentSubscription = ref<AdminSubscriptionView | null>(null)
const currentSubscriptionLoading = ref(false)
/** 查询失败时不谎报「无订阅」，单独提示运营先确认门店状态。 */
const currentSubscriptionFailed = ref(false)
let currentSubscriptionSeq = 0

/** 生成人工开通幂等键（随 grant 提交，服务端同 key 命中即幂等重放）。 */
const createIdempotencyKey = () =>
  typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`

/** 选定门店后查询其当前生效订阅；后端未支持 status 参数时按返回内容再过滤一次（防御）。 */
const loadCurrentSubscription = async (storeId: number) => {
  const seq = ++currentSubscriptionSeq
  currentSubscriptionLoading.value = true
  currentSubscriptionFailed.value = false
  try {
    const response = await listAdminSubscriptions({ storeId, status: 'ACTIVE', size: 1 })
    if (seq !== currentSubscriptionSeq) {
      return
    }
    const active = (response.data?.content ?? []).find((item) => item.status === 'ACTIVE')
    currentSubscription.value = active ?? null
  } catch {
    if (seq === currentSubscriptionSeq) {
      currentSubscription.value = null
      currentSubscriptionFailed.value = true
    }
  } finally {
    if (seq === currentSubscriptionSeq) {
      currentSubscriptionLoading.value = false
    }
  }
}

const currentRemainingDays = computed(() => remainingDays(currentSubscription.value?.endTime))

const selectedPackage = computed(
  () => packages.value.find((pkg) => pkg.id === grantForm.packageId) ?? null,
)

// 选中系统兜底套餐（isSystem）时默认「长期有效」（对齐迁移脚本 2099 语义）
watch(selectedPackage, (pkg) => {
  if (pkg) {
    grantForm.durationMode = defaultDurationMode(pkg.isSystem === true)
  }
})

// 弹窗内手动换选门店时重新查询（打开弹窗时的初始查询由 openGrantDialog 显式触发）
const handleGrantStoreChange = (storeId: number | undefined) => {
  currentSubscription.value = null
  currentSubscriptionFailed.value = false
  if (storeId !== undefined) {
    void loadCurrentSubscription(storeId)
  }
}

const grantRules: FormRules = {
  storeId: [
    {
      required: true,
      message: t('admin.subscriptions.validation.storeIdRequired'),
      trigger: 'change',
    },
  ],
  packageId: [
    {
      required: true,
      message: t('admin.subscriptions.validation.packageRequired'),
      trigger: 'change',
    },
  ],
  remark: [
    {
      validator: (_rule, value, callback) => {
        if (!String(value ?? '').trim()) {
          callback(new Error(t('admin.subscriptions.validation.remarkRequired')))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  durationDays: [
    {
      validator: (_rule, value, callback) => {
        if (grantForm.durationMode !== 'CUSTOM') {
          callback()
          return
        }
        const days = Number(value)
        if (
          !Number.isInteger(days) ||
          days < CUSTOM_DURATION_DAYS_MIN ||
          days > CUSTOM_DURATION_DAYS_MAX
        ) {
          callback(new Error(t('admin.subscriptions.validation.durationDaysInvalid')))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

const cancellingId = ref<number | null>(null)

const formatDateTime = (value: string | null) =>
  value ? value.slice(0, 19).replace('T', ' ') : '-'

const loadSubscriptions = async () => {
  loading.value = true
  try {
    const response = await listAdminSubscriptions({
      storeId: filterStoreId.value,
      page: page.value - 1, // 后端分页从 0 开始
      size: pageSize.value,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || t('admin.common.loadFailed'))
    }
    subscriptions.value = response.data.content || []
    total.value = response.data.totalElements
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  void loadSubscriptions()
}

const handlePageChange = (nextPage: number) => {
  page.value = nextPage
  void loadSubscriptions()
}

const openGrantDialog = async () => {
  grantForm.storeId = filterStoreId.value
  grantForm.packageId = undefined
  grantForm.durationMode = 'PERIOD'
  grantForm.durationDays = 30
  grantForm.remark = ''
  grantIdempotencyKey.value = createIdempotencyKey()
  currentSubscription.value = null
  currentSubscriptionFailed.value = false
  grantDialogVisible.value = true
  if (grantForm.storeId !== undefined) {
    void loadCurrentSubscription(grantForm.storeId)
  }
  if (!packages.value.length) {
    try {
      const response = await listAdminPackages()
      if (response.success && Array.isArray(response.data)) {
        packages.value = response.data
      }
    } catch (error) {
      ElMessage.error(getAdminErrorMessage(error))
    }
  }
}

const handleGrant = async () => {
  const valid = await grantFormRef.value?.validate().catch(() => false)
  if (!valid || grantForm.storeId === undefined || grantForm.packageId === undefined) {
    return
  }

  grantSubmitting.value = true
  try {
    const response = await grantAdminSubscription(
      buildGrantPayload({
        storeId: grantForm.storeId,
        packageId: grantForm.packageId,
        remark: grantForm.remark,
        durationMode: grantForm.durationMode,
        durationDays: grantForm.durationMode === 'CUSTOM' ? grantForm.durationDays : null,
        idempotencyKey: grantIdempotencyKey.value || undefined,
      }),
    )
    if (!response.success) {
      throw new Error(response.message || t('admin.common.saveFailed'))
    }
    ElMessage.success(response.message || t('admin.common.saveSuccess'))
    grantDialogVisible.value = false
    await loadSubscriptions()
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error, 'admin.common.saveFailed'))
  } finally {
    grantSubmitting.value = false
  }
}

const handleCancel = async (row: AdminSubscriptionView) => {
  try {
    await ElMessageBox.confirm(
      t('admin.subscriptions.cancelConfirm', {
        store: row.storeName || row.storeId,
        package: packageDisplayName(row.packageName),
      }),
      t('admin.subscriptions.cancelTitle'),
      {
        confirmButtonText: t('admin.common.confirm'),
        cancelButtonText: t('admin.common.cancel'),
        type: 'warning',
      },
    )
  } catch {
    return
  }

  cancellingId.value = row.id
  try {
    const response = await cancelAdminSubscription(row.id)
    if (!response.success) {
      throw new Error(response.message || t('admin.common.saveFailed'))
    }
    ElMessage.success(response.message || t('admin.common.saveSuccess'))
    await loadSubscriptions()
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error, 'admin.common.saveFailed'))
  } finally {
    cancellingId.value = null
  }
}

onMounted(loadSubscriptions)
</script>

<template>
  <div v-loading="loading" class="admin-subscriptions">
    <div class="page-header">
      <div>
        <h2>{{ t('admin.subscriptions.title') }}</h2>
        <p class="page-description">{{ t('admin.subscriptions.description') }}</p>
      </div>
      <div class="header-actions">
        <AdminStoreSelect
          v-model="filterStoreId"
          :placeholder="t('admin.subscriptions.filterStoreId')"
          class="store-filter"
        />
        <el-button :icon="Search" @click="handleSearch">{{ t('admin.common.search') }}</el-button>
        <el-button type="primary" :icon="Plus" @click="openGrantDialog">
          {{ t('admin.subscriptions.grant') }}
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="subscriptions" row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column :label="t('admin.subscriptions.columns.store')" min-width="140">
          <template #default="{ row }">
            <span>{{ row.storeName || '-' }}</span>
            <span class="store-id">(#{{ row.storeId }})</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('admin.subscriptions.columns.package')" min-width="110">
          <template #default="{ row }">{{ packageDisplayName(row.packageName) }}</template>
        </el-table-column>
        <el-table-column
          :label="t('admin.subscriptions.columns.pricePaid')"
          width="100"
          align="right"
        >
          <template #default="{ row }">${{ row.pricePaid }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.subscriptions.columns.startTime')" width="150">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.subscriptions.columns.endTime')" width="150">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column
          :label="t('admin.subscriptions.columns.status')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <!-- endTime 已过但后端惰性未标记 EXPIRED 的行，前端按时间计算展示「已过期」 -->
            <el-tag v-if="isEffectivelyExpired(row)" type="warning" effect="plain">
              {{ t('admin.enums.subscriptionStatus.EXPIRED') }}
            </el-tag>
            <el-tag
              v-else
              :type="
                row.status === 'ACTIVE' ? 'success' : row.status === 'EXPIRED' ? 'warning' : 'info'
              "
              effect="plain"
            >
              {{ t(`admin.enums.subscriptionStatus.${row.status}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('admin.common.actions')" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ACTIVE'"
              link
              type="danger"
              :loading="cancellingId === row.id"
              @click="handleCancel(row)"
            >
              {{ t('admin.subscriptions.cancel') }}
            </el-button>
            <span v-else class="no-action">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="grantDialogVisible"
      :title="t('admin.subscriptions.adjustTitle')"
      width="520px"
    >
      <el-alert
        type="info"
        :title="t('admin.subscriptions.grantHint')"
        :closable="false"
        class="grant-alert"
      />
      <el-form ref="grantFormRef" :model="grantForm" :rules="grantRules" label-position="top">
        <el-form-item :label="t('admin.subscriptions.form.storeId')" prop="storeId">
          <AdminStoreSelect
            v-model="grantForm.storeId"
            :disabled="grantSubmitting"
            @change="handleGrantStoreChange"
          />
        </el-form-item>

        <!-- 选定门店后的当前生效订阅：替换风险提示 + 事实信息（无订阅/查询失败均如实展示） -->
        <div
          v-if="grantForm.storeId !== undefined"
          v-loading="currentSubscriptionLoading"
          class="current-subscription"
        >
          <template v-if="currentSubscription">
            <el-alert
              type="warning"
              :title="
                t('admin.subscriptions.grantReplaceWarning', {
                  package: packageDisplayName(currentSubscription.packageName),
                  days: currentRemainingDays,
                  amount: currentSubscription.pricePaid,
                })
              "
              :closable="false"
              show-icon
            />
            <dl class="current-subscription-meta">
              <div>
                <dt>{{ t('admin.subscriptions.current.package') }}</dt>
                <dd>{{ packageDisplayName(currentSubscription.packageName) }}</dd>
              </div>
              <div>
                <dt>{{ t('admin.subscriptions.current.endTime') }}</dt>
                <dd>{{ formatDateTime(currentSubscription.endTime) }}</dd>
              </div>
              <div>
                <dt>{{ t('admin.subscriptions.current.pricePaid') }}</dt>
                <dd>${{ currentSubscription.pricePaid }}</dd>
              </div>
              <div>
                <dt>{{ t('admin.subscriptions.current.remainingDays') }}</dt>
                <dd>
                  {{
                    t('admin.subscriptions.current.remainingDaysValue', {
                      days: currentRemainingDays,
                    })
                  }}
                </dd>
              </div>
            </dl>
          </template>
          <el-alert
            v-else-if="currentSubscriptionFailed"
            type="warning"
            :title="t('admin.subscriptions.current.queryFailed')"
            :closable="false"
            show-icon
          />
          <el-alert
            v-else-if="!currentSubscriptionLoading"
            type="info"
            :title="t('admin.subscriptions.current.none')"
            :closable="false"
            show-icon
          />
        </div>

        <el-form-item :label="t('admin.subscriptions.form.package')" prop="packageId">
          <el-select
            v-model="grantForm.packageId"
            :disabled="grantSubmitting"
            class="package-select"
          >
            <el-option
              v-for="pkg in packages"
              :key="pkg.id"
              :value="pkg.id"
              :label="`${packageDisplayName(pkg.name)}（v${pkg.version} · $${pkg.price}/${periodLabel(pkg.period)}${pkg.status === 'OFF_SHELF' ? ' · ' + t('admin.enums.packageStatus.OFF_SHELF') : ''}${pkg.isSystem === true ? ' · ' + t('admin.subscriptions.systemPackageTag') : ''}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.subscriptions.form.durationMode')">
          <el-radio-group v-model="grantForm.durationMode" :disabled="grantSubmitting">
            <el-radio value="PERIOD">{{ t('admin.subscriptions.durationMode.period') }}</el-radio>
            <el-radio value="CUSTOM">{{ t('admin.subscriptions.durationMode.custom') }}</el-radio>
            <el-radio value="PERMANENT">
              {{ t('admin.subscriptions.durationMode.permanent') }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          v-if="grantForm.durationMode === 'CUSTOM'"
          :label="t('admin.subscriptions.form.durationDays')"
          prop="durationDays"
        >
          <el-input-number
            v-model="grantForm.durationDays"
            :min="CUSTOM_DURATION_DAYS_MIN"
            :max="CUSTOM_DURATION_DAYS_MAX"
            :precision="0"
            :disabled="grantSubmitting"
          />
        </el-form-item>
        <el-form-item :label="t('admin.subscriptions.form.remark')" prop="remark">
          <el-input
            v-model="grantForm.remark"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            :placeholder="t('admin.subscriptions.form.remarkPlaceholder')"
            :disabled="grantSubmitting"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="grantSubmitting" @click="grantDialogVisible = false">
          {{ t('admin.common.cancel') }}
        </el-button>
        <el-button type="primary" :loading="grantSubmitting" @click="handleGrant">
          {{ t('admin.common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-subscriptions {
  min-height: 200px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.store-filter {
  width: 240px;
}

.store-id {
  color: #909399;
  font-size: 12px;
}

.no-action {
  color: #c0c4cc;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.grant-alert {
  margin-bottom: 16px;
}

.package-select {
  width: 100%;
}

.current-subscription {
  display: flex;
  min-height: 32px;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.current-subscription-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 16px;
  margin: 0;
  padding: 8px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.current-subscription-meta > div {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.current-subscription-meta dt {
  flex-shrink: 0;
  color: #909399;
  font-size: 12px;
}

.current-subscription-meta dd {
  margin: 0;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}
</style>
