import { request } from '@/utils/request'
import type { ApiResponse } from '@/api/auth'

/**
 * 租户侧 SaaS 计费接口（/api/v1/billing，走 request 自动带 token + X-Store-Id）。
 * 与后端 SaasDtos 一一对应。
 */

export type SaasFeatureType = 'BOOLEAN' | 'QUOTA' | 'CAPACITY'
export type SaasPackagePeriod = 'MONTH' | 'YEAR'
export type SaasSubscriptionStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED'

export interface BillingPackageFeatureView {
  featureCode: string
  name: string
  type: SaasFeatureType | null
  unit: string | null
  quotaLimit: number | null
}

export interface BillingPackageView {
  id: number
  name: string
  version: number
  price: number
  period: SaasPackagePeriod
  description: string | null
  /** 系统兜底默认版（P9 契约）：永不上架售卖；字段缺失按 false 容错 */
  systemPackage?: boolean
  features: BillingPackageFeatureView[]
}

export interface BillingEntitlementView {
  featureCode: string
  type: SaasFeatureType
  limit: number | null
}

export interface BillingQuotaUsageView {
  featureCode: string
  name: string
  totalQuota: number | null
  usedQuota: number
  remaining: number | null
  periodStart: string | null
  periodEnd: string | null
}

export interface BillingCapacityUsageView {
  featureCode: string
  name: string
  limit: number | null
  used: number
}

export interface BillingSubscriptionView {
  id: number
  packageId: number
  packageName: string
  pricePaid: number
  startTime: string | null
  endTime: string | null
  status: SaasSubscriptionStatus
  /** 当前订阅为系统兜底默认版（P9 契约）：前端展示「默认保障套餐」提示；字段缺失按 false 容错 */
  systemPackage?: boolean
  entitlements: BillingEntitlementView[]
  quotas: BillingQuotaUsageView[]
  capacityUsages: BillingCapacityUsageView[]
}

/** 402 NeedUpgradeException 错误体 data 载荷。 */
export interface NeedUpgradePayload {
  featureCode: string
  limit: number | null
  used: number | null
}

/** 首期功能字典 featureCode（与后端种子数据一致，管理端可扩展新 code）。 */
export const SAAS_FEATURE_CODES = {
  INDEPENDENT_WEBSITE: 'independent_website',
  AI_WEBSITE_GEN: 'ai_website_gen',
  ROOM_COUNT: 'room_count',
} as const

/**
 * 在售套餐 + 权益明细。
 */
export const listBillingPackages = async (): Promise<ApiResponse<BillingPackageView[]>> => {
  return await request.get('/billing/packages')
}

/**
 * 当前订阅（套餐 + 权益快照 + 各 QUOTA 用量）。无订阅时 data 为 null。
 * suppressErrorToast：后台静默刷新，失败由调用方降级处理。
 */
export const getMySubscription = async (): Promise<ApiResponse<BillingSubscriptionView | null>> => {
  return await request.get('/billing/my-subscription', { suppressErrorToast: true })
}

/**
 * 直连购买：点击购买即成功（支付渠道后续接入）。
 * idempotencyKey：客户端按购买尝试生成，服务端同 key 命中即幂等重放（防双击/重试重复下单）。
 */
export const subscribeBillingPackage = async (
  packageId: number,
  idempotencyKey?: string,
): Promise<ApiResponse<BillingSubscriptionView>> => {
  return await request.post('/billing/subscribe', { packageId, idempotencyKey })
}
