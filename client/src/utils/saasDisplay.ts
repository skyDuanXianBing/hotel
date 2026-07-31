/**
 * SaaS 套餐/权益展示文案：优先前端 i18n，API 中文种子名仅作回退。
 * 管理端可新增 featureCode / 套餐名；缺 key 时回退到接口原值或 code。
 */

// Compatible with vue-i18n Composer `t` / `te` without pulling in Composer types.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Translate = (key: string, ...args: any[]) => string
type TranslateExists = (key: string) => boolean

/** 权益名：优先 saasSubscription.featureNames.${featureCode}。 */
export const resolveFeatureDisplayName = (
  t: Translate,
  te: TranslateExists,
  featureCode: string,
  fallbackName?: string | null,
): string => {
  const key = `saasSubscription.featureNames.${featureCode}`
  if (te(key)) {
    return t(key)
  }
  return fallbackName || featureCode
}

/** 权益单位：优先按 featureCode 取 i18n，再回退 API unit。 */
export const resolveFeatureUnit = (
  t: Translate,
  te: TranslateExists,
  featureCode: string,
  fallbackUnit?: string | null,
): string => {
  const key = `saasSubscription.featureUnits.${featureCode}`
  if (te(key)) {
    return t(key)
  }
  return fallbackUnit || ''
}

/** 额度+单位：有单位时用带空格模板，避免英文拼成 50times。 */
export const formatFeatureQuotaLimit = (
  t: Translate,
  te: TranslateExists,
  featureCode: string,
  limit: number,
  fallbackUnit?: string | null,
): string => {
  const unit = resolveFeatureUnit(t, te, featureCode, fallbackUnit)
  if (!unit) {
    return String(limit)
  }
  return t('saasSubscription.myPlan.quotaLimitWithUnit', { limit, unit })
}

/**
 * 套餐名：按 API 名精确匹配已知种子（标准版/豪华版/…）。
 * 管理端自定义名称无 key 时原样展示。
 */
export const resolvePackageDisplayName = (
  t: Translate,
  te: TranslateExists,
  packageName: string,
): string => {
  const key = `saasSubscription.packageNames.${packageName}`
  return te(key) ? t(key) : packageName
}
