/**
 * 管理端「调整会员等级」纯函数（P9）：时长模式提交体构造与订阅行展示计算。
 * 抽成纯函数便于单测；AdminSubscriptions.vue 只负责绑定表单与渲染。
 */

export type GrantDurationMode = 'PERIOD' | 'CUSTOM' | 'PERMANENT'

export const CUSTOM_DURATION_DAYS_MIN = 1
export const CUSTOM_DURATION_DAYS_MAX = 36500

export interface GrantPayloadInput {
  storeId: number
  packageId: number
  remark: string
  durationMode: GrantDurationMode
  durationDays?: number | null
  idempotencyKey?: string
}

export interface GrantPayload {
  storeId: number
  packageId: number
  remark: string
  durationDays?: number | null
  permanent?: boolean
  idempotencyKey?: string
}

/** 系统兜底套餐（isSystem）默认「长期有效」（对齐迁移脚本 2099 语义），其余按套餐周期。 */
export const defaultDurationMode = (isSystem: boolean): GrantDurationMode =>
  isSystem ? 'PERMANENT' : 'PERIOD'

/**
 * 构造人工开通提交体：CUSTOM 带 durationDays（null 表示未填，由表单校验拦截）；
 * PERMANENT 带 permanent=true；PERIOD 两者均不带（后端按套餐周期计算）。
 */
export const buildGrantPayload = (input: GrantPayloadInput): GrantPayload => {
  const payload: GrantPayload = {
    storeId: input.storeId,
    packageId: input.packageId,
    remark: input.remark.trim(),
    idempotencyKey: input.idempotencyKey || undefined,
  }
  if (input.durationMode === 'CUSTOM') {
    payload.durationDays = input.durationDays ?? null
  } else if (input.durationMode === 'PERMANENT') {
    payload.permanent = true
  }
  return payload
}

/** 剩余天数（向上取整，已过期/无效日期为 0）。endTime 形如 '2026-08-01T12:00:00'（本地时间解释）。 */
export const remainingDays = (
  endTime: string | null | undefined,
  now: number = Date.now(),
): number => {
  if (!endTime) {
    return 0
  }
  const end = new Date(endTime).getTime()
  if (!Number.isFinite(end)) {
    return 0
  }
  return Math.max(0, Math.ceil((end - now) / 86400000))
}

/**
 * 列表展示层：endTime 已过但 status 仍为 ACTIVE（后端仅惰性标记 EXPIRED，
 * 无人访问的门店长期显示假 ACTIVE）→ 前端按时间计算展示「已过期」。
 */
export const isEffectivelyExpired = (
  row: { status: string; endTime: string | null | undefined },
  now: number = Date.now(),
): boolean => {
  if (row.status !== 'ACTIVE' || !row.endTime) {
    return false
  }
  const end = new Date(row.endTime).getTime()
  return Number.isFinite(end) && end <= now
}
