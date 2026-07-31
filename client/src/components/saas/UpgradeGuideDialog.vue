<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { TopRight } from '@element-plus/icons-vue'
import { listBillingPackages, type BillingPackageView } from '@/api/billing'
import { useEntitlementStore } from '@/stores/entitlement'

/**
 * 全局 402 升级引导弹窗：request.ts 命中 402 时由 entitlement store 触发，
 * 挂载在 App.vue，展示受阻权益、当前用量与在售套餐对比，并跳转“我的套餐”。
 */
const router = useRouter()
const { t, te } = useI18n()
const entitlementStore = useEntitlementStore()

const packages = ref<BillingPackageView[]>([])
const packagesLoaded = ref(false)

const visible = computed({
  get: () => entitlementStore.upgradeDialogVisible,
  set: (value: boolean) => {
    if (!value) {
      entitlementStore.closeUpgradeGuide()
    }
  },
})

const context = computed(() => entitlementStore.upgradeContext)

const featureName = computed(() => {
  const code = context.value?.featureCode
  if (!code) {
    return ''
  }
  const key = `saasSubscription.featureNames.${code}`
  return te(key) ? t(key) : code
})

const hasUsage = computed(
  () => typeof context.value?.limit === 'number' && typeof context.value?.used === 'number',
)

/** 门店尚未开通任何套餐（402 reason=NO_SUBSCRIPTION）：显示“先购买套餐”的专属文案。 */
const isNoSubscription = computed(() => context.value?.reason === 'NO_SUBSCRIPTION')

const reasonText = computed(() => {
  if (isNoSubscription.value) {
    return t('saasSubscription.upgradeDialog.noSubscription')
  }
  return hasUsage.value
    ? t('saasSubscription.upgradeDialog.quotaExhausted', {
        feature: featureName.value,
        used: context.value?.used,
        limit: context.value?.limit,
      })
    : t('saasSubscription.upgradeDialog.featureLocked', { feature: featureName.value })
})

const packageIncludesFeature = (pkg: BillingPackageView) => {
  const code = context.value?.featureCode
  if (!code) {
    return false
  }
  return pkg.features.some((feature) => feature.featureCode === code)
}

/**
 * 套餐对比区的权益文案：布尔权益保持“包含/不含”；计次/容量权益展示该档具体额度
 * （如 10间 / 50间 / 不限），让用户能直接看出各档差异。
 */
const packageEntitlementLabel = (pkg: BillingPackageView) => {
  const code = context.value?.featureCode
  if (!code) {
    return ''
  }
  const feature = pkg.features.find((item) => item.featureCode === code)
  if (!feature) {
    return t('saasSubscription.upgradeDialog.excludesFeature')
  }
  if (feature.type !== 'QUOTA' && feature.type !== 'CAPACITY') {
    return t('saasSubscription.upgradeDialog.includesFeature')
  }
  if (feature.quotaLimit === null) {
    return t('saasSubscription.myPlan.unlimited')
  }
  return `${feature.quotaLimit}${feature.unit || ''}`
}

const loadPackages = async () => {
  if (packagesLoaded.value) {
    return
  }
  try {
    const response = await listBillingPackages()
    if (response.success && Array.isArray(response.data)) {
      packages.value = response.data
      packagesLoaded.value = true
    }
  } catch {
    // 套餐列表拉取失败不阻断引导：仍保留跳转“我的套餐”的入口
  }
}

watch(visible, (value) => {
  if (value) {
    void loadPackages()
  }
})

const goToPlans = () => {
  entitlementStore.closeUpgradeGuide()
  void router.push('/settings/package-settings')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('saasSubscription.upgradeDialog.title')"
    width="520px"
    append-to-body
  >
    <div class="upgrade-guide">
      <p v-if="context?.message" class="upgrade-message">{{ context.message }}</p>
      <p class="upgrade-reason">{{ reasonText }}</p>

      <div v-if="packages.length" class="upgrade-packages">
        <p class="upgrade-packages-title">{{ t('saasSubscription.upgradeDialog.compareTitle') }}</p>
        <ul class="upgrade-package-list">
          <li
            v-for="pkg in packages"
            :key="pkg.id"
            class="upgrade-package-item"
            :class="{ 'is-included': packageIncludesFeature(pkg) }"
          >
            <span class="package-name">{{ pkg.name }}</span>
            <span class="package-price">
              ¥{{ pkg.price }}/{{ t(`saasSubscription.periods.${pkg.period}`) }}
            </span>
            <span class="package-included">
              {{ packageEntitlementLabel(pkg) }}
            </span>
          </li>
        </ul>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{
        t('saasSubscription.upgradeDialog.later')
      }}</el-button>
      <el-button type="primary" :icon="TopRight" @click="goToPlans">
        {{ t('saasSubscription.upgradeDialog.viewPlans') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.upgrade-guide {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upgrade-message {
  margin: 0;
  color: #606266;
  font-size: 13px;
}

.upgrade-reason {
  margin: 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.upgrade-packages-title {
  margin: 0 0 8px;
  color: #909399;
  font-size: 12px;
}

.upgrade-package-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.upgrade-package-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  font-size: 13px;
}

.upgrade-package-item.is-included {
  border-color: #67c23a;
  background: #f0f9eb;
}

.package-name {
  flex: 1;
  font-weight: 600;
}

.package-price {
  color: #606266;
}

.package-included {
  color: #909399;
  font-size: 12px;
}

.upgrade-package-item.is-included .package-included {
  color: #67c23a;
}
</style>
