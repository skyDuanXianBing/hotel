/**
 * 套餐档位方向计算（P9）：决定「我的套餐」在售卡片按钮的可用状态与文案方向。
 * 抽成纯函数便于单测；组件只负责把结果映射为 i18n 文案与禁用态。
 *
 * 规则（有生效订阅时按价格比较）：
 * - 无生效订阅：全部「立即购买」（buy）
 * - 目标套餐 = 当前套餐：「当前套餐」禁用（current）
 * - 目标价 > 当前参照价：「升级 / 切换」可点（upgrade）
 * - 目标价 <= 当前参照价且非当前：禁用，「低于当前档位」（lowerTier）
 */

export type PackageActionKind = 'buy' | 'upgrade' | 'current' | 'lowerTier'

export interface PackageTierContext {
  /** 是否有生效中的订阅（status=ACTIVE） */
  hasActiveSubscription: boolean
  /** 当前订阅的套餐 id（无订阅传 null） */
  currentPackageId: number | null
  /** 当前套餐参照价（在售同款取挂牌价，取不到用实付价，都没有按 0） */
  currentPrice: number
}

export interface PackageTierTarget {
  id: number
  price: number
}

export const resolvePackageAction = (
  target: PackageTierTarget,
  context: PackageTierContext,
): PackageActionKind => {
  if (!context.hasActiveSubscription || context.currentPackageId === null) {
    return 'buy'
  }
  if (target.id === context.currentPackageId) {
    return 'current'
  }
  return target.price > context.currentPrice ? 'upgrade' : 'lowerTier'
}

/**
 * 当前套餐参照价：优先在售同款挂牌价（管理端可能改价，以最新在售模板为准），
 * 已下架/系统兜底款回落实付价，最终兜底 0（兜底默认版场景：任何在售套餐都视为升级）。
 */
export const resolveCurrentPackagePrice = (args: {
  currentPackageId: number | null
  pricePaid: number | null | undefined
  packages: Array<{ id: number; price: number }>
}): number => {
  const listed = args.packages.find((pkg) => pkg.id === args.currentPackageId)
  if (listed) {
    return listed.price
  }
  const paid = Number(args.pricePaid)
  return Number.isFinite(paid) && paid > 0 ? paid : 0
}
