import { adminRequest } from '@/utils/adminRequest'
import type { ApiResponse } from '@/api/auth'
import type { SaasFeatureType, SaasPackagePeriod, SaasSubscriptionStatus } from '@/api/billing'

/**
 * 平台管理端接口（/api/admin/**，走 adminRequest 独立实例，仅带 adminToken）。
 * 与后端 AdminDtos / saas 实体 JSON 一一对应。
 */

export type SaasPackageStatus = 'ON_SHELF' | 'OFF_SHELF'
export type SaasQuotaResetCycle = 'MONTHLY' | 'NONE'

// ------------------------------------------------------------------
// 认证
// ------------------------------------------------------------------

export interface AdminLoginResponse {
  token: string
  username: string
  role: string
}

// ------------------------------------------------------------------
// 套餐 / 功能字典（实体直出）
// ------------------------------------------------------------------

export interface AdminPackage {
  id: number
  name: string
  version: number
  price: number
  period: SaasPackagePeriod
  status: SaasPackageStatus
  /** 系统兜底套餐（P9 契约）：禁止上架/下架操作；字段缺失按 false 容错 */
  isSystem?: boolean
  description: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface AdminFeature {
  id: number
  featureCode: string
  name: string
  type: SaasFeatureType
  unit: string | null
  description: string | null
  defaultResetCycle: SaasQuotaResetCycle | null
  createdAt: string | null
  updatedAt: string | null
}

export interface AdminPackageFeature {
  id: number
  packageId: number
  featureCode: string
  quotaLimit: number | null
}

export interface AdminPackageUpsertRequest {
  name: string
  version: number
  price: number
  period: SaasPackagePeriod
  description?: string | null
}

export interface AdminFeatureUpdateRequest {
  name: string
  type: SaasFeatureType
  unit?: string | null
  defaultResetCycle?: SaasQuotaResetCycle | null
  description?: string | null
}

export interface AdminPackageFeatureItem {
  featureCode: string
  quotaLimit: number | null
}

// ------------------------------------------------------------------
// 租户订阅
// ------------------------------------------------------------------

export interface AdminSubscriptionView {
  id: number
  storeId: number
  storeName: string | null
  packageId: number
  packageName: string
  pricePaid: number
  startTime: string | null
  endTime: string | null
  status: SaasSubscriptionStatus
  createdAt: string | null
}

export interface AdminPagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

// ------------------------------------------------------------------
// 配额调整 / 概览
// ------------------------------------------------------------------

export interface AdminQuotaUsage {
  featureCode: string
  totalQuota: number | null
  usedQuota: number
  remaining: number | null
  periodStart: string | null
  periodEnd: string | null
}

/** 门店搜索命中项（GET /stores/search）：前端展示为“名称 (#id)”。 */
export interface AdminStoreSearchItem {
  id: number
  name: string | null
}

export interface AdminPackageSubscriptionCount {
  packageName: string
  count: number
}

export interface AdminDashboardResponse {
  totalStores: number
  activeSubscriptions: number
  packageSubscriptionCounts: AdminPackageSubscriptionCount[]
  last30DaysOrderAmount: number
  aiQuotaUsedTotal: number | null
}

// ------------------------------------------------------------------
// API
// ------------------------------------------------------------------

export const adminLogin = async (
  username: string,
  password: string,
): Promise<ApiResponse<AdminLoginResponse>> => {
  return await adminRequest.post('/auth/login', { username, password })
}

/** 修改当前登录管理员密码（POST /auth/change-password；成功后客户端强制重新登录）。 */
export const changeAdminPassword = async (data: {
  oldPassword: string
  newPassword: string
}): Promise<ApiResponse<null>> => {
  return await adminRequest.post('/auth/change-password', data)
}

export const listAdminPackages = async (): Promise<ApiResponse<AdminPackage[]>> => {
  return await adminRequest.get('/packages')
}

export const createAdminPackage = async (
  data: AdminPackageUpsertRequest,
): Promise<ApiResponse<AdminPackage>> => {
  return await adminRequest.post('/packages', data)
}

export const updateAdminPackage = async (
  id: number,
  data: AdminPackageUpsertRequest,
): Promise<ApiResponse<AdminPackage>> => {
  return await adminRequest.put(`/packages/${id}`, data)
}

export const updateAdminPackageStatus = async (
  id: number,
  status: SaasPackageStatus,
): Promise<ApiResponse<AdminPackage>> => {
  return await adminRequest.put(`/packages/${id}/status`, { status })
}

export const listAdminPackageFeatures = async (
  id: number,
): Promise<ApiResponse<AdminPackageFeature[]>> => {
  return await adminRequest.get(`/packages/${id}/features`)
}

export const replaceAdminPackageFeatures = async (
  id: number,
  features: AdminPackageFeatureItem[],
): Promise<ApiResponse<AdminPackageFeature[]>> => {
  return await adminRequest.put(`/packages/${id}/features`, { features })
}

export const listAdminFeatures = async (): Promise<ApiResponse<AdminFeature[]>> => {
  return await adminRequest.get('/features')
}

export const updateAdminFeature = async (
  id: number,
  data: AdminFeatureUpdateRequest,
): Promise<ApiResponse<AdminFeature>> => {
  return await adminRequest.put(`/features/${id}`, data)
}

export const listAdminSubscriptions = async (params: {
  storeId?: number
  status?: SaasSubscriptionStatus
  page?: number
  size?: number
}): Promise<ApiResponse<AdminPagedResponse<AdminSubscriptionView>>> => {
  return await adminRequest.get('/subscriptions', { params })
}

/**
 * 人工开通/调整会员等级（P9）：remark 必填（落审计）；时长默认按套餐周期，
 * durationDays 自定义天数（1..36500）或 permanent=true 长期有效（至 2099-12-31）。
 */
export const grantAdminSubscription = async (data: {
  storeId: number
  packageId: number
  remark: string
  durationDays?: number | null
  permanent?: boolean
  idempotencyKey?: string
}): Promise<ApiResponse<AdminSubscriptionView>> => {
  return await adminRequest.post('/subscriptions', data)
}

export const cancelAdminSubscription = async (
  id: number,
): Promise<ApiResponse<AdminSubscriptionView>> => {
  return await adminRequest.post(`/subscriptions/${id}/cancel`)
}

export const adjustAdminQuota = async (data: {
  storeId: number
  featureCode: string
  delta: number
  remark?: string
}): Promise<ApiResponse<AdminQuotaUsage>> => {
  return await adminRequest.post('/quota/adjust', data)
}

/**
 * 当前配额用量只读查询（配额调整页“调整前先看用量”）。
 * 门店无有效订阅或订阅不含该 QUOTA 权益时 success=true 但 data=null，message 说明空态原因。
 */
export const getAdminQuotaUsage = async (params: {
  storeId: number
  featureCode: string
}): Promise<ApiResponse<AdminQuotaUsage | null>> => {
  return await adminRequest.get('/quota/usage', { params })
}

/** 门店选择器远程搜索：名称模糊 + ID 精确匹配，最多 20 条。keyword 为空返回前 20 家。 */
export const searchAdminStores = async (
  keyword?: string,
): Promise<ApiResponse<AdminStoreSearchItem[]>> => {
  return await adminRequest.get('/stores/search', { params: { keyword: keyword || undefined } })
}

export const getAdminDashboard = async (): Promise<ApiResponse<AdminDashboardResponse>> => {
  return await adminRequest.get('/dashboard')
}
