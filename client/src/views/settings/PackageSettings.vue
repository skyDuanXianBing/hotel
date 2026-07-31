<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Refresh } from '@element-plus/icons-vue'
import {
  listBillingPackages,
  subscribeBillingPackage,
  type BillingCapacityUsageView,
  type BillingEntitlementView,
  type BillingPackageView,
  type BillingQuotaUsageView,
} from '@/api/billing'
import { useEntitlementStore } from '@/stores/entitlement'
import {
  resolveCurrentPackagePrice,
  resolvePackageAction,
  type PackageActionKind,
} from '@/utils/packageTier'

const { t, te } = useI18n()
const entitlementStore = useEntitlementStore()

const loading = ref(true)
const packages = ref<BillingPackageView[]>([])
const subscribingPackageId = ref<number | null>(null)

/** 生成购买幂等键：随 subscribe 提交，服务端同 key 命中即幂等重放，防双击/重试重复下单。 */
const createIdempotencyKey = () =>
  typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`

// 一次购买尝试的幂等键：打开确认弹窗时生成；同套餐提交失败后保留供重试复用
// （覆盖“成功响应丢失后用户重试”——服务端命中同 key 直接返回原订阅，不重复下单）；
// 成功或取消确认后作废，下一次购买生成新键。
const subscribeAttemptKey = ref<string | null>(null)
const subscribeAttemptPackageId = ref<number | null>(null)

const resetSubscribeAttempt = () => {
  subscribeAttemptKey.value = null
  subscribeAttemptPackageId.value = null
}

const subscription = computed(() => entitlementStore.subscription)

const formatDateTime = (value: string | null | undefined) =>
  value ? value.slice(0, 19).replace('T', ' ') : '-'

const featureDisplayName = (featureCode: string, fallbackName?: string | null) => {
  if (fallbackName) {
    return fallbackName
  }
  const key = `saasSubscription.featureNames.${featureCode}`
  return te(key) ? t(key) : featureCode
}

const quotaFor = (featureCode: string): BillingQuotaUsageView | null =>
  subscription.value?.quotas.find((quota) => quota.featureCode === featureCode) ?? null

const capacityFor = (featureCode: string): BillingCapacityUsageView | null =>
  subscription.value?.capacityUsages?.find((usage) => usage.featureCode === featureCode) ?? null

interface EntitlementRow {
  featureCode: string
  name: string
  type: BillingEntitlementView['type']
  limit: number | null
  quota: BillingQuotaUsageView | null
  capacity: BillingCapacityUsageView | null
}

const entitlementRows = computed<EntitlementRow[]>(() => {
  if (!subscription.value) {
    return []
  }
  return subscription.value.entitlements.map((entitlement) => {
    const quota = quotaFor(entitlement.featureCode)
    const capacity = capacityFor(entitlement.featureCode)
    return {
      featureCode: entitlement.featureCode,
      name: featureDisplayName(entitlement.featureCode, quota?.name ?? capacity?.name),
      type: entitlement.type,
      limit: entitlement.limit,
      quota,
      capacity,
    }
  })
})

const quotaPercentage = (quota: BillingQuotaUsageView) => {
  if (quota.totalQuota === null || quota.totalQuota <= 0) {
    return 0
  }
  // 客服补偿会产生负 used（剩余>总额），展示层夹回 0，避免负百分比
  return Math.min(100, Math.round((Math.max(0, quota.usedQuota) / quota.totalQuota) * 100))
}

const quotaStatus = (quota: BillingQuotaUsageView): '' | 'warning' | 'exception' => {
  if (quota.remaining === 0) {
    return 'exception'
  }
  if (quota.totalQuota !== null && quota.remaining !== null && quota.totalQuota > 0) {
    return quota.remaining / quota.totalQuota <= 0.2 ? 'warning' : ''
  }
  return ''
}

/** 容量行文案：优先展示实时用量（当前 X / 上限 Y）；接口未返回用量时回退为仅展示上限。 */
const capacityLabel = (row: EntitlementRow) => {
  if (row.limit === null) {
    return t('saasSubscription.myPlan.unlimited')
  }
  if (row.capacity) {
    return t('saasSubscription.myPlan.capacityUsage', {
      used: row.capacity.used,
      limit: row.limit,
    })
  }
  return t('saasSubscription.myPlan.capacityLimit', { limit: row.limit })
}

const isCapacityExceeded = (row: EntitlementRow) =>
  row.limit !== null && row.capacity !== null && row.capacity.used > row.limit

/** 当前订阅对应的在售套餐（下架套餐不在列表中，返回 null）。 */
const currentListedPackage = computed(() => {
  const current = subscription.value
  if (!current) {
    return null
  }
  return packages.value.find((pkg) => pkg.id === current.packageId) ?? null
})

/** 实付 $0 且套餐挂牌价 > 0 → 管理端人工开通（区别于真实购买）。 */
const isManualGrant = computed(() => {
  const current = subscription.value
  const pkg = currentListedPackage.value
  return Boolean(current && pkg && Number(current.pricePaid) === 0 && Number(pkg.price) > 0)
})

const isCurrentPackage = (pkg: BillingPackageView) =>
  subscription.value?.status === 'ACTIVE' && subscription.value.packageId === pkg.id

/** 是否有生效中的订阅：无订阅时全部卡片「立即购买」（不涉档位方向）。 */
const hasActiveSubscription = computed(() => subscription.value?.status === 'ACTIVE')

/** 当前套餐参照价：在售同款取挂牌价，下架/系统兜底款回落实付价（见 packageTier）。 */
const currentPackagePrice = computed(() =>
  resolveCurrentPackagePrice({
    currentPackageId: subscription.value?.packageId ?? null,
    pricePaid: subscription.value?.pricePaid,
    packages: packages.value,
  }),
)

/** 档位方向（纯函数）：buy / upgrade / current / lowerTier。 */
const packageAction = (pkg: BillingPackageView): PackageActionKind =>
  resolvePackageAction(
    { id: pkg.id, price: Number(pkg.price) },
    {
      hasActiveSubscription: hasActiveSubscription.value,
      currentPackageId: hasActiveSubscription.value
        ? (subscription.value?.packageId ?? null)
        : null,
      currentPrice: currentPackagePrice.value,
    },
  )

/** 降级不开放自助：当前套餐与更低档位一律禁用。 */
const isSubscribeDisabled = (pkg: BillingPackageView) => {
  const action = packageAction(pkg)
  return action === 'current' || action === 'lowerTier'
}

const subscribeButtonLabel = (pkg: BillingPackageView) => {
  switch (packageAction(pkg)) {
    case 'current':
      return t('saasSubscription.myPlan.currentPackage')
    case 'lowerTier':
      return t('saasSubscription.myPlan.lowerTier')
    case 'upgrade':
      return t('saasSubscription.packages.upgradeAction')
    default:
      return t('saasSubscription.packages.buyAction')
  }
}

/** 系统兜底默认版不在售卖区展示（双保险：后端也会拦截上架）。 */
const visiblePackages = computed(() => packages.value.filter((pkg) => pkg.systemPackage !== true))

/** 存在被禁用的更低档位卡片时，在卡片区下方提示联系管理员调整。 */
const showLowerTierHint = computed(
  () =>
    hasActiveSubscription.value &&
    visiblePackages.value.some((pkg) => packageAction(pkg) === 'lowerTier'),
)

/** 当前订阅为系统兜底默认版：提示购买付费套餐解锁完整服务。 */
const isDefaultPackage = computed(() => subscription.value?.systemPackage === true)

const loadData = async () => {
  loading.value = true
  try {
    await entitlementStore.refresh(true)
    const response = await listBillingPackages()
    if (response.success && Array.isArray(response.data)) {
      packages.value = response.data
    }
  } catch {
    // 套餐列表失败时保留当前订阅卡片展示，列表区显示为空
  } finally {
    loading.value = false
  }
}

const handleSubscribe = async (pkg: BillingPackageView) => {
  const action = packageAction(pkg)
  if (action === 'current' || action === 'lowerTier' || subscribingPackageId.value !== null) {
    return
  }

  // 新的购买意图生成新幂等键；同套餐上次提交失败时复用旧键（重试由服务端幂等重放兜底）
  if (subscribeAttemptPackageId.value !== pkg.id || !subscribeAttemptKey.value) {
    subscribeAttemptKey.value = createIdempotencyKey()
    subscribeAttemptPackageId.value = pkg.id
  }
  const idempotencyKey = subscribeAttemptKey.value

  // 按购买方向告知后果：升档=立即替换不折算差价；同套餐重购=到期后续延；无订阅=原始购买文案。
  // 注：同套餐重购分支当前不可达（当前套餐按钮禁用，现状保留），仅为防御性补全。
  const current = subscription.value
  const confirmMessage =
    action === 'upgrade'
      ? t('saasSubscription.packages.upgradeConfirm', { name: pkg.name })
      : current?.status === 'ACTIVE' && current.packageId === pkg.id
        ? t('saasSubscription.packages.repurchaseConfirm', {
            end: formatDateTime(current.endTime),
          })
        : t('saasSubscription.packages.subscribeConfirm', {
            name: pkg.name,
            price: pkg.price,
            period: t(`saasSubscription.periods.${pkg.period}`),
          })

  try {
    await ElMessageBox.confirm(
      confirmMessage,
      t('saasSubscription.packages.subscribeConfirmTitle'),
      {
        confirmButtonText: t('saasSubscription.packages.subscribeConfirmButton'),
        cancelButtonText: t('saasSubscription.packages.subscribeCancel'),
        type: 'info',
      },
    )
  } catch {
    resetSubscribeAttempt() // 取消确认：意图作废，重开弹窗生成新键
    return
  }

  subscribingPackageId.value = pkg.id
  try {
    const response = await subscribeBillingPackage(pkg.id, idempotencyKey)
    if (!response.success) {
      throw new Error(response.message || t('saasSubscription.packages.subscribeFailed'))
    }
    ElMessage.success(t('saasSubscription.packages.subscribeSuccess', { name: pkg.name }))
    resetSubscribeAttempt() // 成交后键必须作废，否则未来再次购买同套餐会被幂等重放吞掉
    await loadData()
  } catch {
    // 保留尝试键：用户重试同套餐时复用；request.ts 已统一 toast 后端错误信息；402 会走全局升级引导
  } finally {
    subscribingPackageId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="package-settings">
    <div class="page-header">
      <h3>{{ t('saasSubscription.myPlan.title') }}</h3>
      <p class="page-description">{{ t('saasSubscription.myPlan.description') }}</p>
    </div>

    <div class="content-area">
      <el-card class="current-plan-card">
        <template #header>
          <div class="card-header">
            <span>{{ t('saasSubscription.myPlan.currentTitle') }}</span>
            <el-button link type="primary" :icon="Refresh" :loading="loading" @click="loadData">
              {{ t('saasSubscription.myPlan.refresh') }}
            </el-button>
          </div>
        </template>

        <template v-if="subscription">
          <el-alert
            v-if="isDefaultPackage"
            type="info"
            :title="t('saasSubscription.myPlan.defaultPackageNotice')"
            :closable="false"
            show-icon
            class="default-package-notice"
          />
          <div class="plan-summary">
            <div class="plan-name-row">
              <span class="plan-name">{{ subscription.packageName }}</span>
              <el-tag type="success" effect="plain">
                {{ t(`saasSubscription.subscriptionStatus.${subscription.status}`) }}
              </el-tag>
            </div>
            <div class="plan-meta">
              <span class="plan-price">
                {{ t('saasSubscription.myPlan.pricePaid', { price: subscription.pricePaid }) }}
              </span>
              <span v-if="isManualGrant" class="plan-grant-hint">
                · {{ t('saasSubscription.myPlan.manualGrantTag') }}
              </span>
              <span class="plan-dates">
                {{
                  t('saasSubscription.myPlan.validity', {
                    end: formatDateTime(subscription.endTime),
                  })
                }}
              </span>
            </div>
          </div>

          <el-divider />

          <div class="entitlement-list">
            <template v-for="row in entitlementRows" :key="row.featureCode">
              <!-- BOOLEAN：功能解锁 -->
              <div v-if="row.type === 'BOOLEAN'" class="entitlement-row">
                <el-icon class="entitlement-icon entitlement-icon--on"><CircleCheck /></el-icon>
                <span class="entitlement-name">{{ row.name }}</span>
                <span class="entitlement-value entitlement-value--ok">
                  {{ t('saasSubscription.myPlan.included') }}
                </span>
              </div>

              <!-- QUOTA：用量进度 -->
              <div
                v-else-if="row.type === 'QUOTA' && row.quota"
                class="entitlement-row entitlement-row--quota"
              >
                <div class="quota-head">
                  <span class="entitlement-name">{{ row.name }}</span>
                  <span
                    v-if="row.quota.totalQuota === null"
                    class="entitlement-value entitlement-value--ok"
                  >
                    {{ t('saasSubscription.myPlan.unlimited') }}
                  </span>
                  <span v-else class="entitlement-value">
                    {{
                      t('saasSubscription.myPlan.quotaUsage', {
                        used: Math.max(0, row.quota.usedQuota),
                        total: row.quota.totalQuota,
                      })
                    }}
                  </span>
                </div>
                <template v-if="row.quota.totalQuota !== null">
                  <el-progress
                    :percentage="quotaPercentage(row.quota)"
                    :status="quotaStatus(row.quota)"
                    :stroke-width="8"
                  />
                  <p class="quota-hint">
                    {{
                      t('saasSubscription.myPlan.quotaResetHint', {
                        remaining: row.quota.remaining ?? 0,
                        date: formatDateTime(row.quota.periodEnd),
                      })
                    }}
                  </p>
                </template>
              </div>

              <!-- CAPACITY：容量上限 + 实时用量（后端 my-subscription 实时 COUNT；无用量数据时回退为仅展示上限） -->
              <div v-else class="entitlement-row entitlement-row--capacity">
                <div class="capacity-line">
                  <el-icon class="entitlement-icon entitlement-icon--on"><CircleCheck /></el-icon>
                  <span class="entitlement-name">{{ row.name }}</span>
                  <span
                    class="entitlement-value"
                    :class="{ 'entitlement-value--exceeded': isCapacityExceeded(row) }"
                  >
                    {{ capacityLabel(row) }}
                  </span>
                </div>
                <p v-if="isCapacityExceeded(row)" class="capacity-exceeded-hint">
                  {{ t('saasSubscription.myPlan.capacityExceeded') }}
                </p>
              </div>
            </template>
          </div>
        </template>

        <el-empty v-else-if="!loading" :description="t('saasSubscription.myPlan.noSubscription')" />
      </el-card>

      <div v-if="visiblePackages.length" class="packages-section">
        <h4 class="section-title">{{ t('saasSubscription.packages.title') }}</h4>
        <div class="packages-grid">
          <el-card
            v-for="pkg in visiblePackages"
            :key="pkg.id"
            class="package-card"
            :class="{ 'is-current': isCurrentPackage(pkg) }"
          >
            <div class="package-card-head">
              <span class="package-name">{{ pkg.name }}</span>
              <el-tag v-if="isCurrentPackage(pkg)" type="success" size="small" effect="dark">
                {{ t('saasSubscription.myPlan.currentPackage') }}
              </el-tag>
            </div>
            <div class="package-price-row">
              <span class="package-price">${{ pkg.price }}</span>
              <span class="package-period">/{{ t(`saasSubscription.periods.${pkg.period}`) }}</span>
            </div>

            <!-- 权益事实只展示权益列表（唯一事实源）。不展示 pkg.description 自由文本：
                 管理端改额度后描述不会同步，同卡会出现「10 间房」vs「· 12间」的自相矛盾（验收 BUG2）。 -->

            <ul class="package-feature-list">
              <li
                v-for="feature in pkg.features"
                :key="feature.featureCode"
                class="package-feature-item"
              >
                <el-icon class="feature-check"><CircleCheck /></el-icon>
                <span>
                  {{ featureDisplayName(feature.featureCode, feature.name) }}
                  <template v-if="feature.type !== 'BOOLEAN'">
                    ·
                    {{
                      feature.quotaLimit === null
                        ? t('saasSubscription.myPlan.unlimited')
                        : `${feature.quotaLimit}${feature.unit || ''}`
                    }}
                  </template>
                </span>
              </li>
            </ul>

            <el-button
              class="package-action"
              :type="isSubscribeDisabled(pkg) ? 'info' : 'primary'"
              :plain="!isCurrentPackage(pkg)"
              :disabled="isSubscribeDisabled(pkg)"
              :loading="subscribingPackageId === pkg.id"
              @click="handleSubscribe(pkg)"
            >
              {{ subscribeButtonLabel(pkg) }}
            </el-button>
          </el-card>
        </div>
        <p v-if="showLowerTierHint" class="lower-tier-hint">
          {{ t('saasSubscription.myPlan.lowerTierHint') }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.package-settings {
  padding: 20px;
  height: 100%;
  overflow: auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h3 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 18px;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.content-area {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1180px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.plan-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.plan-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.plan-name {
  font-size: 20px;
  font-weight: 700;
}

.plan-meta {
  display: flex;
  align-items: baseline;
  gap: 16px;
  color: #606266;
  font-size: 13px;
}

.plan-price {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.entitlement-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.entitlement-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.entitlement-row--quota {
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
}

.quota-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.quota-hint {
  margin: 0;
  color: #909399;
  font-size: 12px;
}

.entitlement-icon--on {
  color: #67c23a;
}

.entitlement-name {
  font-weight: 600;
}

.entitlement-value {
  margin-left: auto;
  color: #606266;
  font-size: 13px;
}

.quota-head .entitlement-value {
  margin-left: 0;
}

.entitlement-value--ok {
  color: #67c23a;
}

.entitlement-row--capacity {
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
}

.capacity-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.entitlement-value--exceeded {
  color: #f56c6c;
  font-weight: 600;
}

.capacity-exceeded-hint {
  margin: 0;
  padding-left: 24px;
  color: #f56c6c;
  font-size: 12px;
}

.plan-grant-hint {
  color: #909399;
  font-size: 12px;
}

.section-title {
  margin: 0 0 12px;
  color: #333;
  font-size: 15px;
}

.packages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.package-card {
  border: 1px solid #ebeef5;
}

.package-card.is-current {
  border-color: #67c23a;
}

.package-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.package-name {
  font-size: 16px;
  font-weight: 700;
}

.package-price-row {
  margin-top: 8px;
}

.package-price {
  color: #f56c6c;
  font-size: 24px;
  font-weight: 700;
}

.package-period {
  color: #909399;
  font-size: 13px;
}

.package-feature-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 84px;
  margin: 12px 0;
  padding: 0;
  list-style: none;
}

.package-feature-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 13px;
}

.feature-check {
  color: #67c23a;
}

.package-action {
  width: 100%;
}

.default-package-notice {
  margin-bottom: 14px;
}

.lower-tier-hint {
  margin: 10px 2px 0;
  color: #909399;
  font-size: 12px;
}
</style>
