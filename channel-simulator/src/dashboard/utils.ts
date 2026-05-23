import type { JsonObject, LogEntry, ReadinessParts } from './types'

const LOG_PREVIEW_LIMIT = 1400

export function isObject(value: unknown): value is JsonObject {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

export function getEnvelopeData(value: unknown): unknown {
  if (isObject(value) && Object.prototype.hasOwnProperty.call(value, 'data')) {
    return value.data
  }
  return value
}

export function getText(source: unknown, keys: string[]): string {
  if (!isObject(source)) {
    return ''
  }
  for (const key of keys) {
    const value = source[key]
    if (value !== null && value !== undefined && String(value).trim()) {
      return String(value)
    }
  }
  return ''
}

export function buildReadinessParts(value: unknown): ReadinessParts {
  const data = getEnvelopeData(value)
  const response = isObject(data) ? data : {}
  const readinessEnvelope = isObject(response.readiness) ? response.readiness : {}
  const contextEnvelope = isObject(response.context) ? response.context : {}
  const readinessData = getEnvelopeData(readinessEnvelope)
  const contextData = getEnvelopeData(contextEnvelope)
  const sources = [response, contextData, readinessData]
  const missingRequirements = new Set<string>()
  const channels: unknown[] = []
  const roomTypes: unknown[] = []
  const rooms: unknown[] = []
  let suHotelId = ''
  let ready: boolean | null = null

  for (const source of sources) {
    for (const item of getArray(source, 'missingRequirements')) {
      missingRequirements.add(String(item))
    }
    for (const item of getArray(source, 'channels')) {
      channels.push(item)
    }
    for (const item of getArray(source, 'roomTypes')) {
      roomTypes.push(item)
    }
    for (const item of getArray(source, 'rooms')) {
      rooms.push(item)
    }
    if (!suHotelId) {
      suHotelId = getText(source, ['suHotelId', 'hotelId'])
    }
  }

  if (response.ready === true || response.ready === false) {
    ready = response.ready
  }

  return {
    ready,
    missingRequirements: Array.from(missingRequirements),
    channels,
    roomTypes,
    rooms,
    suHotelId,
  }
}

export function prettyJson(value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (typeof value === 'string') {
    return value
  }
  return JSON.stringify(value, null, 2)
}

export function labelForEntity(value: unknown, fallback: string): string {
  if (!isObject(value)) {
    return String(value || fallback)
  }
  const primary = getText(value, ['name', 'title', 'label', 'code', 'roomNumber', 'roomTypeName'])
  const id = getText(value, ['id', 'roomTypeId', 'roomId'])
  if (primary && id && primary !== id) {
    return `${primary} (${id})`
  }
  if (primary) {
    return primary
  }
  if (id) {
    return `${fallback} ${id}`
  }
  return fallback
}

export function valueForEntity(value: unknown, keys: string[]): string {
  const text = getText(value, keys)
  if (text) {
    return text
  }
  return ''
}

export function formatEntityList(items: unknown[], fallback: string): string {
  const labels: string[] = []
  for (const item of items) {
    labels.push(labelForEntity(item, fallback))
  }
  return labels.join(', ') || '-'
}

export function statusLabel(value: unknown): string {
  const text = String(value || '')
  const labels: Record<string, string> = {
    CREATED: '已创建',
    SENT: '已发送',
    FAILED: '失败',
    SUCCESS: '成功',
  }
  return labels[text] || text || '-'
}

export function formatLogPreview(log: LogEntry): string {
  const text = prettyJson({
    requestBody: log.requestBody,
    responseBody: log.responseBody,
  })
  if (text.length <= LOG_PREVIEW_LIMIT) {
    return text
  }
  return `${text.slice(0, LOG_PREVIEW_LIMIT)}\n...[已截断]`
}

export function formatDateTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

export function getRunSteps(value: unknown): unknown[] {
  const data = getEnvelopeData(value)
  if (isObject(data) && isObject(data.run)) {
    return getArray(data.run, 'steps')
  }
  return []
}

export function getLifecycleSteps(value: unknown): unknown[] {
  const data = getEnvelopeData(value)
  return getArray(data, 'steps')
}

export function getLogKey(log: LogEntry): string {
  return `${log.timestamp}-${log.path}`
}

function getArray(source: unknown, key: string): unknown[] {
  if (!isObject(source)) {
    return []
  }
  const value = source[key]
  if (Array.isArray(value)) {
    return value
  }
  return []
}
