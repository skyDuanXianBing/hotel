import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  getMySubscription,
  type BillingQuotaUsageView,
  type BillingSubscriptionView,
} from '@/api/billing'
import { useStoreStore } from '@/stores/store'

export interface UpgradeGuideContext {
  featureCode: string
  limit?: number | null
  used?: number | null
  message?: string
  /**
   * 402 触发原因（服务端 NeedUpgradeException.Reason）：
   * NO_SUBSCRIPTION（门店未开通套餐）时弹窗显示“先购买套餐”的专属文案；
   * NOT_INCLUDED / QUOTA_EXHAUSTED / CAPACITY_EXCEEDED 维持升级引导文案。
   */
  reason?: string | null
}

/**
 * 租户权益快照（来源 /billing/my-subscription）。
 *
 * 关键约束（红线）：拉取失败必须静默降级 —— loaded=false 时 hasFeature/hasQuota 一律
 * 放行（fail-open），路由守卫与菜单过滤不得因后端未上线/接口故障锁死前端；
 * 真正的拦截由后端 402 兜底，届时再走 upgradeDialog 引导。
 */
export const useEntitlementStore = defineStore('entitlement', () => {
  const storeStore = useStoreStore()

  const subscription = ref<BillingSubscriptionView | null>(null)
  /** 当前门店是否已成功拉取过权益（失败时保持 false → fail-open）。 */
  const loaded = ref(false)
  const loading = ref(false)
  const loadedStoreId = ref<number | null>(null)
  let requestSeq = 0

  // 402 升级引导弹窗状态
  const upgradeDialogVisible = ref(false)
  const upgradeContext = ref<UpgradeGuideContext | null>(null)

  const currentStoreId = computed(() => storeStore.currentStore?.id ?? null)

  const clear = () => {
    requestSeq += 1
    subscription.value = null
    loaded.value = false
    loading.value = false
    loadedStoreId.value = null
  }

  /**
   * 拉取当前门店权益快照；失败静默（不抛出、不 toast，billing API 已 suppressErrorToast）。
   * 同一门店重复调用走缓存，force=true 强制刷新。
   */
  const refresh = async (force = false): Promise<BillingSubscriptionView | null> => {
    if (!currentStoreId.value) {
      clear()
      return null
    }

    const requestedStoreId = currentStoreId.value
    if (!force && loaded.value && loadedStoreId.value === requestedStoreId) {
      return subscription.value
    }

    const seq = ++requestSeq
    loading.value = true
    try {
      const response = await getMySubscription()
      if (seq !== requestSeq || currentStoreId.value !== requestedStoreId) {
        return subscription.value
      }
      if (!response.success) {
        // 后端业务失败：视为未加载，fail-open
        loaded.value = false
        loadedStoreId.value = null
        return subscription.value
      }
      subscription.value = response.data ?? null
      loaded.value = true
      loadedStoreId.value = requestedStoreId
      return subscription.value
    } catch {
      if (seq === requestSeq) {
        loaded.value = false
        loadedStoreId.value = null
      }
      return subscription.value
    } finally {
      if (seq === requestSeq) {
        loading.value = false
      }
    }
  }

  /**
   * 是否拥有某项权益（BOOLEAN 解锁 / QUOTA、CAPACITY 含于套餐）。
   * 未成功加载（接口故障）时放行；已加载但无订阅或订阅不含该权益时拦截。
   */
  const hasFeature = (featureCode: string): boolean => {
    if (!loaded.value) {
      return true
    }
    if (!subscription.value) {
      return false
    }
    return subscription.value.entitlements.some((entry) => entry.featureCode === featureCode)
  }

  const quotaFor = (featureCode: string): BillingQuotaUsageView | null => {
    if (!loaded.value || !subscription.value) {
      return null
    }
    return subscription.value.quotas.find((quota) => quota.featureCode === featureCode) ?? null
  }

  /**
   * 配额是否仍有剩余（remaining 为 null = 不限）。未加载时放行。
   */
  const hasQuota = (featureCode: string): boolean => {
    if (!loaded.value) {
      return true
    }
    const quota = quotaFor(featureCode)
    if (!quota) {
      // 已加载但无此配额账：订阅不含该 QUOTA 权益
      return false
    }
    return quota.remaining === null || quota.remaining > 0
  }

  const openUpgradeGuide = (context: UpgradeGuideContext) => {
    upgradeContext.value = context
    upgradeDialogVisible.value = true
    // 后台静默刷新一次，让弹窗里的套餐对比/用量尽量新鲜；失败不影响弹窗
    void refresh(true)
  }

  const closeUpgradeGuide = () => {
    upgradeDialogVisible.value = false
  }

  watch(
    currentStoreId,
    (storeId, previousStoreId) => {
      if (storeId !== previousStoreId) {
        clear()
      }
    },
    { flush: 'sync' },
  )

  return {
    subscription,
    loaded,
    loading,
    loadedStoreId,
    upgradeDialogVisible,
    upgradeContext,
    refresh,
    clear,
    hasFeature,
    hasQuota,
    quotaFor,
    openUpgradeGuide,
    closeUpgradeGuide,
  }
})
