const REDACTED = '[redacted]'
const SENSITIVE_KEY_PARTS = [
  'authorization',
  'password',
  'secret',
  'testsupportkey',
  'token',
]

const JWT_PATTERN = /\beyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b/g
const BEARER_PATTERN = /\bBearer\s+[A-Za-z0-9._~-]+/gi
const NAMED_KEY_PATTERN = /\b(X-Test-Support-Key|CHANNEL_E2E_TEST_SUPPORT_KEY|TEST_SUPPORT_KEY)(\s*[:=]\s*)([^\s,;]+)/gi
const JSON_SENSITIVE_FIELD_PATTERN = /(\"[^\"]*(?:token|authorization|password|secret|test[^\"]*support[^\"]*key)[^\"]*\"\s*:\s*)\"[^\"]*\"/gi

function isSensitiveKey(key: string): boolean {
  const normalized = key.toLowerCase().replace(/[^a-z0-9]/g, '')
  return SENSITIVE_KEY_PARTS.some((part) => normalized.includes(part))
}

export function redactSensitiveText(value: string): string {
  return value
    .replace(JSON_SENSITIVE_FIELD_PATTERN, `$1"${REDACTED}"`)
    .replace(BEARER_PATTERN, `Bearer ${REDACTED}`)
    .replace(JWT_PATTERN, REDACTED)
    .replace(NAMED_KEY_PATTERN, (_match, name: string, separator: string) => {
      return `${name}${separator}${REDACTED}`
    })
}

export function redactForOutput(value: unknown): unknown {
  if (typeof value === 'string') {
    return redactSensitiveText(value)
  }
  if (Array.isArray(value)) {
    return value.map((item) => redactForOutput(item))
  }
  if (!value || typeof value !== 'object') {
    return value
  }
  const output: Record<string, unknown> = {}
  for (const [key, fieldValue] of Object.entries(value)) {
    output[key] = isSensitiveKey(key) ? REDACTED : redactForOutput(fieldValue)
  }
  return output
}
