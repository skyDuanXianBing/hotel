import { NextFunction, Request, RequestHandler, Response } from 'express'
import chalk from 'chalk'

const MAX_LOGS = 500
const REDACTED_VALUE = '[redacted]'
const BINARY_CONTENT_KEY_NAMES = new Set([
  'attachment',
  'attachmentcontent',
  'base64',
  'filecontent',
  'imagebase64',
])
const SENSITIVE_KEY_PARTS = [
  'authorization',
  'password',
  'secret',
  'testsupportkey',
  'token',
]

export type LogType = 'MOCK_API' | 'WEBHOOK_SENT' | 'WEBHOOK_RECEIVED'

export interface LogEntry {
  timestamp: string
  type: LogType
  method: string
  path: string
  requestBody: unknown
  responseBody: unknown
  statusCode: number | null
}

/** 内存日志环形缓冲（最多保留 MAX_LOGS 条） */
const logs: LogEntry[] = []

/**
 * 类型 -> 终端颜色映射
 */
const typeColor: Record<LogType, (text: string) => string> = {
  MOCK_API: chalk.cyan,
  WEBHOOK_SENT: chalk.magenta,
  WEBHOOK_RECEIVED: chalk.green,
}

/**
 * 根据 HTTP 状态码上色
 */
function colorStatus(status: number | null) {
  if (status == null) return chalk.gray('-')
  if (status >= 500) return chalk.red(String(status))
  if (status >= 400) return chalk.yellow(String(status))
  if (status >= 300) return chalk.cyan(String(status))
  return chalk.green(String(status))
}

function isSensitiveKey(key: string): boolean {
  const normalizedKey = key.toLowerCase().replace(/[^a-z0-9]/g, '')

  for (const part of SENSITIVE_KEY_PARTS) {
    if (normalizedKey.includes(part)) {
      return true
    }
  }

  return false
}

function redactBinaryContent(key: string, value: unknown): unknown {
  const normalizedKey = key.toLowerCase().replace(/[^a-z0-9]/g, '')
  if (!BINARY_CONTENT_KEY_NAMES.has(normalizedKey)) {
    return undefined
  }
  if (normalizedKey === 'attachment' && typeof value !== 'string') {
    return undefined
  }
  if (typeof value === 'string') {
    return `[redacted binary:${value.length} chars]`
  }
  return REDACTED_VALUE
}

function redactSensitiveFields(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map((item) => redactSensitiveFields(item))
  }

  if (!value || typeof value !== 'object') {
    return value
  }

  const redacted: Record<string, unknown> = {}
  for (const [key, fieldValue] of Object.entries(value)) {
    const redactedBinary = redactBinaryContent(key, fieldValue)
    redacted[key] =
      redactedBinary !== undefined
        ? redactedBinary
        : isSensitiveKey(key)
          ? REDACTED_VALUE
          : redactSensitiveFields(fieldValue)
  }

  return redacted
}

/**
 * 安全克隆请求/响应体，避免循环引用与超大对象写入内存日志
 */
function safeClone(value: unknown): unknown {
  if (value === undefined || value === null) return value
  try {
    let normalizedValue = value
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
        try {
          normalizedValue = JSON.parse(trimmed)
        } catch (_err) {
          normalizedValue = value
        }
      }
    }
    const safeValue = redactSensitiveFields(normalizedValue)
    const text = typeof safeValue === 'string' ? safeValue : JSON.stringify(safeValue)
    if (text && text.length > 20000) {
      return typeof safeValue === 'string'
        ? `${safeValue.slice(0, 20000)}...[truncated]`
        : JSON.parse(text.slice(0, 20000) + '"')
    }
    return typeof safeValue === 'string' ? safeValue : JSON.parse(text)
  } catch (_err) {
    return String(value)
  }
}

/**
 * 记录一条请求/响应日志
 */
export function logRequest(
  type: LogType,
  method: string,
  requestPath: string,
  body: unknown,
  response: unknown,
  status: number | null,
): LogEntry {
  const entry = {
    timestamp: new Date().toISOString(),
    type,
    method: method ? String(method).toUpperCase() : 'GET',
    path: requestPath || '',
    requestBody: safeClone(body),
    responseBody: safeClone(response),
    statusCode: typeof status === 'number' ? status : null,
  }

  logs.push(entry)
  if (logs.length > MAX_LOGS) {
    logs.splice(0, logs.length - MAX_LOGS)
  }

  const color = typeColor[type] || chalk.white
  const tag = color(`[${type}]`)
  const line = `${chalk.gray(entry.timestamp)} ${tag} ${chalk.bold(entry.method)} ${entry.path} -> ${colorStatus(entry.statusCode)}`
  // eslint-disable-next-line no-console
  console.log(line)

  return entry
}

/**
 * 获取当前内存中的日志（按时间顺序）
 */
export function getLogs(): LogEntry[] {
  return logs.slice()
}

/**
 * 清空内存日志
 */
export function clearLogs(): void {
  logs.length = 0
}

/**
 * Express 中间件：自动记录所有进入 Mock API 的请求与响应
 * 通过劫持 res.send/res.json 捕获响应体。
 */
export const requestLogger: RequestHandler = (req: Request, res: Response, next: NextFunction) => {
  const originalSend = res.send.bind(res)
  const originalJson = res.json.bind(res)
  let capturedBody: unknown

  res.send = ((body?: unknown) => {
    if (capturedBody === undefined) {
      capturedBody = body
    }
    return originalSend(body as never)
  }) as typeof res.send
  res.json = ((body?: unknown) => {
    capturedBody = body
    return originalJson(body)
  }) as typeof res.json

  res.on('finish', () => {
    try {
      logRequest(
        'MOCK_API',
        req.method,
        req.originalUrl || req.url,
        req.body,
        capturedBody,
        res.statusCode,
      )
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error(chalk.red('[logger] failed to record request:'), err)
    }
  })

  next()
}
