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
 * Render quick reply templates with two variable syntaxes:
 * 1) New syntax: {{guest_name}}
 * 2) Legacy syntax: {Guest's name}
 *
 * When data is missing, keep the placeholder and return missingKeys for UI hints.
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

  // New syntax: {{ key }}
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

  // Legacy syntax: {Label}
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
    property_name: 'Property name',
    guest_name: 'Guest name',
    guest_phone: 'Guest phone',
    checkin_date: 'Check-in date',
    checkout_date: 'Check-out date',
    room_type_name: 'Room type name',
    rate_plan_name: 'Rate plan name',
  }

  return missingKeys.map((key) => labels[key] || key).join('、')
}

export const formatMissingQuickReplyKeyLabels = (
  missingKeys: QuickReplyTemplateKey[],
  translate: (key: string) => string,
) => {
  const labelKeys: Record<QuickReplyTemplateKey, string> = {
    property_name: 'stage6.components.quickReplyTemplate.propertyName',
    guest_name: 'stage6.components.quickReplyTemplate.guestName',
    guest_phone: 'stage6.components.quickReplyTemplate.guestPhone',
    checkin_date: 'stage6.components.quickReplyTemplate.checkinDate',
    checkout_date: 'stage6.components.quickReplyTemplate.checkoutDate',
    room_type_name: 'stage6.components.quickReplyTemplate.roomTypeName',
    rate_plan_name: 'stage6.components.quickReplyTemplate.ratePlanName',
  }

  return missingKeys.map((key) => translate(labelKeys[key]) || key).join('、')
}
