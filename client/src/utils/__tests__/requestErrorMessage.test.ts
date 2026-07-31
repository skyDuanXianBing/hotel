import { describe, expect, mock, test } from 'bun:test'
import zhCN from '../../locales/messages/zh-CN'

/**
 * P9 修复回归：axios 原文错误（"Request failed with status code 500" / "Network Error" 等）
 * 不得直通用户；业务 message（4xx 等）照常透传。
 *
 * 依赖模块一律 mock.module（bun 无 vite 别名解析）；@/locales 用真实 zh-CN 消息包
 * 解析 dotted key——既验证映射选对了 key，也验证该 key 在消息包中真实存在。
 */

const resolveKey = (key: string): string => {
  let current: unknown = zhCN
  for (const segment of key.split('.')) {
    if (current === null || typeof current !== 'object') {
      return key
    }
    current = (current as Record<string, unknown>)[segment]
  }
  return typeof current === 'string' ? current : key
}

mock.module('@/locales', () => ({
  i18n: { global: { t: resolveKey } },
}))
mock.module('element-plus', () => ({
  ElMessage: { error: () => {}, success: () => {}, warning: () => {} },
}))
mock.module('@/stores/pinia', () => ({ pinia: {} }))
mock.module('@/utils/cleanerSession', () => ({
  CLEANER_STORE_KEY: 'cleaner_store',
  CLEANER_TOKEN_KEY: 'cleaner_token',
  PMS_CURRENT_STORE_KEY: 'pms_current_store',
  PMS_TOKEN_KEY: 'pms_token',
  clearAllLocalSessions: () => {},
}))
mock.module('@/utils/requestCancellation', () => ({
  isRequestCancellationError: () => false,
}))
mock.module('@/utils/adminSession', () => ({
  ADMIN_LOGIN_PATH: '/admin/login',
  clearAdminSession: () => {},
  getAdminToken: () => null,
  isAdminWorkspacePath: () => false,
}))
// publicRequest 以别名引用 request.ts：重定向到真实相对模块（与下方 import 同一实例）
mock.module('@/utils/request', () => import('../request'))

const { getAdminErrorMessage } = await import('../adminRequest')
const { getPublicErrorMessage, isStoreClosedError } = await import('../publicRequest')
const { classifyRequestFailure, resolveRequestFailureMessage, isUpgradeGuided, UPGRADE_GUIDE_SHOWN_KEY } =
  await import('../request')

// ------------------------------------------------------------------
// 构造各型 axios 错误（axios.isAxiosError 只认 isAxiosError=true）
// ------------------------------------------------------------------

const axiosError = (overrides: Record<string, unknown>) => ({
  isAxiosError: true,
  message: 'Request failed with status code 500',
  config: {},
  ...overrides,
})

const serverError500 = () =>
  axiosError({ response: { status: 500, data: {}, headers: {}, config: {} } })
const businessError400 = (message?: string) =>
  axiosError({
    message: 'Request failed with status code 400',
    response: { status: 400, data: message ? { message } : {}, headers: {}, config: {} },
  })
const networkError = () => axiosError({ message: 'Network Error' })
const timeoutError = () =>
  axiosError({ code: 'ECONNABORTED', message: 'timeout of 60000ms exceeded' })

// ------------------------------------------------------------------
// getAdminErrorMessage（管理端）
// ------------------------------------------------------------------

describe('getAdminErrorMessage', () => {
  test('业务 message（4xx）原样透传', () => {
    expect(getAdminErrorMessage(businessError400('该门店当前订阅不包含此配额权益'))).toBe(
      '该门店当前订阅不包含此配额权益',
    )
  })

  test('5xx 无业务 message → 服务不可用文案，非 axios 原文', () => {
    const message = getAdminErrorMessage(serverError500())
    expect(message).toBe(resolveKey('admin.common.serviceUnavailable'))
    expect(message).not.toContain('Request failed with status code')
  })

  test('无响应（断网/CORS）→ 网络异常文案', () => {
    expect(getAdminErrorMessage(networkError())).toBe(resolveKey('admin.common.networkError'))
  })

  test('超时 → 请求超时文案', () => {
    expect(getAdminErrorMessage(timeoutError())).toBe(resolveKey('admin.common.requestTimeout'))
  })

  test('4xx 无业务 message → fallbackKey 文案', () => {
    expect(getAdminErrorMessage(businessError400())).toBe(resolveKey('admin.common.loadFailed'))
    expect(getAdminErrorMessage(businessError400(), 'admin.common.saveFailed')).toBe(
      resolveKey('admin.common.saveFailed'),
    )
  })

  test('非 axios 的 Error 包装 axios 原文 → 拦截为服务不可用文案', () => {
    expect(getAdminErrorMessage(new Error('Request failed with status code 500'))).toBe(
      resolveKey('admin.common.serviceUnavailable'),
    )
    expect(getAdminErrorMessage(new Error('Network Error'))).toBe(
      resolveKey('admin.common.serviceUnavailable'),
    )
  })

  test('非 axios 的业务 Error → 原文透传', () => {
    expect(getAdminErrorMessage(new Error('调整量不能为 0'))).toBe('调整量不能为 0')
  })

  test('空错误 → fallbackKey 文案', () => {
    expect(getAdminErrorMessage(undefined)).toBe(resolveKey('admin.common.loadFailed'))
  })
})

// ------------------------------------------------------------------
// classifyRequestFailure / resolveRequestFailureMessage（租户侧 request.ts）
// ------------------------------------------------------------------

describe('classifyRequestFailure / resolveRequestFailureMessage', () => {
  test('归类：timeout / network / server / unknown', () => {
    expect(classifyRequestFailure(timeoutError())).toBe('timeout')
    expect(classifyRequestFailure(networkError())).toBe('network')
    expect(classifyRequestFailure(serverError500())).toBe('server')
    expect(classifyRequestFailure(businessError400())).toBe('unknown')
    expect(classifyRequestFailure(new Error('other'))).toBe('unknown')
  })

  test('映射到 stage6 本地化文案，永不返回 axios 原文', () => {
    expect(resolveRequestFailureMessage(timeoutError())).toBe(
      resolveKey('stage6.common.messages.requestTimeout'),
    )
    expect(resolveRequestFailureMessage(networkError())).toBe(
      resolveKey('stage6.common.messages.networkError'),
    )
    expect(resolveRequestFailureMessage(serverError500())).toBe(
      resolveKey('stage6.common.messages.serviceUnavailable'),
    )
    expect(resolveRequestFailureMessage(businessError400())).toBe(
      resolveKey('stage6.common.messages.requestFailed'),
    )
    expect(resolveRequestFailureMessage(serverError500())).not.toContain('Request failed')
  })
})

// ------------------------------------------------------------------
// getPublicErrorMessage（公共页 publicRequest.ts）
// ------------------------------------------------------------------

describe('getPublicErrorMessage', () => {
  const fallback = resolveKey('stage6.common.messages.requestFailed') // 以真实文案模拟调用方 t('xxxFailed')

  test('业务 message 透传', () => {
    expect(getPublicErrorMessage(businessError400('链接已过期'), fallback)).toBe('链接已过期')
  })

  test('5xx → 服务不可用文案；断网 → 网络异常文案；超时 → 超时文案', () => {
    expect(getPublicErrorMessage(serverError500(), fallback)).toBe(
      resolveKey('stage6.common.messages.serviceUnavailable'),
    )
    expect(getPublicErrorMessage(networkError(), fallback)).toBe(
      resolveKey('stage6.common.messages.networkError'),
    )
    expect(getPublicErrorMessage(timeoutError(), fallback)).toBe(
      resolveKey('stage6.common.messages.requestTimeout'),
    )
  })

  test('4xx 无业务 message → 用调用方操作级文案', () => {
    expect(getPublicErrorMessage(businessError400(), '上传失败')).toBe('上传失败')
  })

  test('调用方包装的业务 Error → 透传其 message', () => {
    expect(getPublicErrorMessage(new Error('保存失败'), fallback)).toBe('保存失败')
  })

  test('未知错误 → 调用方操作级文案', () => {
    expect(getPublicErrorMessage(null, fallback)).toBe(fallback)
  })
})

// ------------------------------------------------------------------
// isStoreClosedError（P9：公开独立站交易端点 403 = 门店暂停接单，切维护态）
// ------------------------------------------------------------------

describe('isStoreClosedError', () => {
  test('403（该店铺暂停接单）→ true', () => {
    const closed = axiosError({
      message: 'Request failed with status code 403',
      response: { status: 403, data: { message: '该店铺暂停接单' }, headers: {}, config: {} },
    })
    expect(isStoreClosedError(closed)).toBe(true)
  })

  test('其他状态码/非响应错误/空错误 → false', () => {
    expect(isStoreClosedError(businessError400('x'))).toBe(false)
    expect(isStoreClosedError(serverError500())).toBe(false)
    expect(isStoreClosedError(new Error('other'))).toBe(false)
    expect(isStoreClosedError(null)).toBe(false)
    expect(isStoreClosedError(undefined)).toBe(false)
  })
})

// ------------------------------------------------------------------
// isUpgradeGuided（P10：402 升级引导弹窗与调用方通用错误 toast 去重）
// ------------------------------------------------------------------

describe('isUpgradeGuided', () => {
  test('request.ts 已打标的 402 错误 → true（调用方跳过通用错误 toast）', () => {
    const guided = axiosError({
      message: 'Request failed with status code 402',
      response: {
        status: 402,
        data: { message: '已达上限', data: { featureCode: 'room_count' } },
        headers: {},
        config: {},
      },
    }) as Record<string, unknown>
    guided[UPGRADE_GUIDE_SHOWN_KEY] = true
    expect(isUpgradeGuided(guided)).toBe(true)
  })

  test('未打标的 402/其他错误/空错误 → false（照常 toast）', () => {
    const unmarked402 = axiosError({
      message: 'Request failed with status code 402',
      response: { status: 402, data: {}, headers: {}, config: {} },
    })
    expect(isUpgradeGuided(unmarked402)).toBe(false)
    expect(isUpgradeGuided(businessError400('x'))).toBe(false)
    expect(isUpgradeGuided(serverError500())).toBe(false)
    expect(isUpgradeGuided(new Error('other'))).toBe(false)
    expect(isUpgradeGuided('error')).toBe(false)
    expect(isUpgradeGuided(null)).toBe(false)
    expect(isUpgradeGuided(undefined)).toBe(false)
  })
})
