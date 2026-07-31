import { describe, expect, test } from 'bun:test'
import {
  buildGrantPayload,
  defaultDurationMode,
  isEffectivelyExpired,
  remainingDays,
} from '../subscriptionAdjustment'

/**
 * P9 管理端「调整会员等级」：时长模式提交体构造、剩余天数与「假 ACTIVE」过期展示计算。
 */

describe('buildGrantPayload', () => {
  const base = { storeId: 1, packageId: 2, remark: '  老板自家门店免充值开通旗舰版  ' }

  test('按套餐周期（默认）：不带 durationDays / permanent', () => {
    const payload = buildGrantPayload({ ...base, durationMode: 'PERIOD' })
    expect(payload).toEqual({
      storeId: 1,
      packageId: 2,
      remark: '老板自家门店免充值开通旗舰版',
      idempotencyKey: undefined,
    })
    expect('durationDays' in payload).toBe(false)
    expect('permanent' in payload).toBe(false)
  })

  test('自定义天数：带 durationDays', () => {
    const payload = buildGrantPayload({ ...base, durationMode: 'CUSTOM', durationDays: 90 })
    expect(payload.durationDays).toBe(90)
    expect('permanent' in payload).toBe(false)
  })

  test('长期有效：带 permanent=true', () => {
    const payload = buildGrantPayload({ ...base, durationMode: 'PERMANENT' })
    expect(payload.permanent).toBe(true)
    expect('durationDays' in payload).toBe(false)
  })

  test('remark 去空白；幂等键透传', () => {
    const payload = buildGrantPayload({ ...base, durationMode: 'PERIOD', idempotencyKey: 'k-1' })
    expect(payload.remark).toBe('老板自家门店免充值开通旗舰版')
    expect(payload.idempotencyKey).toBe('k-1')
  })
})

describe('defaultDurationMode', () => {
  test('系统兜底套餐默认长期有效，其余按套餐周期', () => {
    expect(defaultDurationMode(true)).toBe('PERMANENT')
    expect(defaultDurationMode(false)).toBe('PERIOD')
  })
})

describe('remainingDays', () => {
  const now = new Date('2026-07-29T00:00:00').getTime()

  test('未来到期：向上取整', () => {
    expect(remainingDays('2026-07-31T12:00:00', now)).toBe(3)
    expect(remainingDays('2026-07-29T00:00:01', now)).toBe(1)
  })

  test('已过期/空/非法日期 → 0', () => {
    expect(remainingDays('2026-07-28T23:59:59', now)).toBe(0)
    expect(remainingDays(null, now)).toBe(0)
    expect(remainingDays(undefined, now)).toBe(0)
    expect(remainingDays('not-a-date', now)).toBe(0)
  })
})

describe('isEffectivelyExpired（列表假 ACTIVE 展示修复）', () => {
  const now = new Date('2026-07-29T00:00:00').getTime()

  test('ACTIVE 且 endTime 已过 → true', () => {
    expect(isEffectivelyExpired({ status: 'ACTIVE', endTime: '2026-07-01T00:00:00' }, now)).toBe(
      true,
    )
  })

  test('ACTIVE 但未到期 → false', () => {
    expect(isEffectivelyExpired({ status: 'ACTIVE', endTime: '2026-08-01T00:00:00' }, now)).toBe(
      false,
    )
  })

  test('非 ACTIVE / 无 endTime / 非法日期 → false', () => {
    expect(isEffectivelyExpired({ status: 'EXPIRED', endTime: '2026-07-01T00:00:00' }, now)).toBe(
      false,
    )
    expect(isEffectivelyExpired({ status: 'CANCELLED', endTime: '2026-07-01T00:00:00' }, now)).toBe(
      false,
    )
    expect(isEffectivelyExpired({ status: 'ACTIVE', endTime: null }, now)).toBe(false)
    expect(isEffectivelyExpired({ status: 'ACTIVE', endTime: 'bad' }, now)).toBe(false)
  })
})
