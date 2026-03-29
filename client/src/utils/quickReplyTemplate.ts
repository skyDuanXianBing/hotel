export type QuickReplyTemplateKey =
  | 'property_name'
  | 'guest_name'
  | 'guest_phone'
  | 'checkin_date'
  | 'checkout_date'
  | 'room_type_name'
  | 'rate_plan_name'

export interface QuickReplyTemplateContext {
  propertyName?: string | null
  guestName?: string | null
  guestPhone?: string | null
  checkInDate?: string | null
  checkOutDate?: string | null
  roomTypeName?: string | null
  ratePlanName?: string | null
}

const normalizeValue = (value?: string | null) => {
  const trimmed = typeof value === 'string' ? value.trim() : ''
  return trimmed ? trimmed : null
}

const KEY_TO_VALUE = (context: QuickReplyTemplateContext) => ({
  property_name: normalizeValue(context.propertyName),
  guest_name: normalizeValue(context.guestName),
  guest_phone: normalizeValue(context.guestPhone),
  checkin_date: normalizeValue(context.checkInDate),
  checkout_date: normalizeValue(context.checkOutDate),
  room_type_name: normalizeValue(context.roomTypeName),
  rate_plan_name: normalizeValue(context.ratePlanName),
})

const LEGACY_LABEL_TO_KEY: Record<string, QuickReplyTemplateKey> = {
  'Property name': 'property_name',
  "Guest's name": 'guest_name',
  "Guest's phone number": 'guest_phone',
  'Check-in date': 'checkin_date',
  'Checkout date': 'checkout_date',
  'Room type name': 'room_type_name',
  'Rate plan name': 'rate_plan_name',
}

export interface RenderQuickReplyTemplateResult {
  rendered: string
  missingKeys: QuickReplyTemplateKey[]
}

/**
 * 渲染快捷回复模板：支持两套变量语法
 * 1) 新语法：{{guest_name}}
 * 2) 旧语法：{Guest's name}
 *
 * 缺失数据时保留占位符，并返回 missingKeys 便于 UI 提示。
 */
export const renderQuickReplyTemplate = (
  template: string,
  context: QuickReplyTemplateContext,
): RenderQuickReplyTemplateResult => {
  const source = template || ''
  if (!source) {
    return { rendered: '', missingKeys: [] }
  }

  const valueByKey = KEY_TO_VALUE(context)
  const missing = new Set<QuickReplyTemplateKey>()

  // 新语法：{{ key }}
  const moustacheRegex = /\{\{\s*([a-z0-9_]+)\s*\}\}/gi
  let rendered = source.replace(moustacheRegex, (rawToken, rawKey: string) => {
    const key = rawKey.toLowerCase() as QuickReplyTemplateKey
    const value = (valueByKey as Record<string, string | null>)[key]
    if (value) {
      return value
    }
    if (key in valueByKey) {
      missing.add(key)
    }
    return rawToken
  })

  // 旧语法：{Label}
  const legacyRegex = /\{([^{}]+)\}/g
  rendered = rendered.replace(legacyRegex, (rawToken, inner: string) => {
    const label = inner.trim()
    const mappedKey = LEGACY_LABEL_TO_KEY[label]
    if (!mappedKey) {
      return rawToken
    }

    const value = valueByKey[mappedKey]
    if (value) {
      return value
    }

    missing.add(mappedKey)
    return rawToken
  })

  return {
    rendered,
    missingKeys: Array.from(missing),
  }
}

export const formatMissingQuickReplyKeys = (missingKeys: QuickReplyTemplateKey[]) => {
  const labels: Record<QuickReplyTemplateKey, string> = {
    property_name: '房源名称',
    guest_name: '住客姓名',
    guest_phone: '住客电话',
    checkin_date: '入住日期',
    checkout_date: '退房日期',
    room_type_name: '房型名称',
    rate_plan_name: '价计划名称',
  }

  return missingKeys.map((key) => labels[key] || key).join('、')
}

