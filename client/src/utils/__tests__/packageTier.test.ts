import { describe, expect, test } from 'bun:test'
import { resolveCurrentPackagePrice, resolvePackageAction } from '../packageTier'

/**
 * P9 档位方向（用户点名的明显 bug）：有生效订阅时按价格比较——
 * 更高价「升级」可点 / 当前套餐禁用 / 更低或同价禁用「低于当前档位」；无订阅全部「立即购买」。
 */

describe('resolvePackageAction', () => {
  test('无生效订阅：全部 buy（currentPackageId/价格不产生影响）', () => {
    expect(
      resolvePackageAction(
        { id: 2, price: 999 },
        { hasActiveSubscription: false, currentPackageId: 1, currentPrice: 99 },
      ),
    ).toBe('buy')
    expect(
      resolvePackageAction(
        { id: 1, price: 99 },
        { hasActiveSubscription: false, currentPackageId: null, currentPrice: 0 },
      ),
    ).toBe('buy')
  })

  test('当前套餐 → current（禁用）', () => {
    expect(
      resolvePackageAction(
        { id: 1, price: 99 },
        { hasActiveSubscription: true, currentPackageId: 1, currentPrice: 99 },
      ),
    ).toBe('current')
  })

  test('目标价 > 当前价 → upgrade（可点）', () => {
    expect(
      resolvePackageAction(
        { id: 2, price: 199 },
        { hasActiveSubscription: true, currentPackageId: 1, currentPrice: 99 },
      ),
    ).toBe('upgrade')
  })

  test('目标价 < 当前价 → lowerTier（禁用）', () => {
    expect(
      resolvePackageAction(
        { id: 3, price: 49 },
        { hasActiveSubscription: true, currentPackageId: 1, currentPrice: 99 },
      ),
    ).toBe('lowerTier')
  })

  test('目标价 = 当前价但非当前套餐 → lowerTier（同价侧向不开放自助）', () => {
    expect(
      resolvePackageAction(
        { id: 4, price: 99 },
        { hasActiveSubscription: true, currentPackageId: 1, currentPrice: 99 },
      ),
    ).toBe('lowerTier')
  })

  test('兜底默认版（当前价 0）→ 任何在售套餐都是 upgrade', () => {
    expect(
      resolvePackageAction(
        { id: 5, price: 1 },
        { hasActiveSubscription: true, currentPackageId: 99, currentPrice: 0 },
      ),
    ).toBe('upgrade')
  })
})

describe('resolveCurrentPackagePrice', () => {
  test('当前套餐仍在售 → 取挂牌价（而非历史实付价）', () => {
    expect(
      resolveCurrentPackagePrice({
        currentPackageId: 1,
        pricePaid: 79,
        packages: [
          { id: 1, price: 99 },
          { id: 2, price: 199 },
        ],
      }),
    ).toBe(99)
  })

  test('当前套餐已下架 → 回落实付价', () => {
    expect(
      resolveCurrentPackagePrice({
        currentPackageId: 9,
        pricePaid: 129,
        packages: [{ id: 1, price: 99 }],
      }),
    ).toBe(129)
  })

  test('实付价缺失/为 0（兜底默认版、免费开通）→ 0', () => {
    expect(resolveCurrentPackagePrice({ currentPackageId: 9, pricePaid: 0, packages: [] })).toBe(0)
    expect(resolveCurrentPackagePrice({ currentPackageId: 9, pricePaid: null, packages: [] })).toBe(
      0,
    )
    expect(
      resolveCurrentPackagePrice({ currentPackageId: null, pricePaid: undefined, packages: [] }),
    ).toBe(0)
  })
})
