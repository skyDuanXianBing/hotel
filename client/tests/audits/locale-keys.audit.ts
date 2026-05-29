import en from '../../src/locales/messages/en'
import ja from '../../src/locales/messages/ja'
import zhCN from '../../src/locales/messages/zh-CN'
import zhTW from '../../src/locales/messages/zh-TW'

type LocaleMessages = Record<string, unknown>

interface LocaleEntry {
  name: string
  messages: LocaleMessages
}

const locales: LocaleEntry[] = [
  { name: 'en', messages: en },
  { name: 'ja', messages: ja },
  { name: 'zh-CN', messages: zhCN },
  { name: 'zh-TW', messages: zhTW },
]

const requiredKeys = [
  'channel.mapping.localPmsOnly',
  'channel.messages.channelWriteNotReady',
  'channel.managementData.standardRate',
  'channel.managementData.unmappedChannelRoom',
  'channel.managementData.localPmsData',
  'channel.managementData.rateModePms',
  'pages.roomStatusChannel.unavailableTitle',
  'pages.roomStatusChannel.unavailableDescription',
  'pages.roomStatusChannel.unavailableActionTip',
]

const flattenKeys = (value: unknown, prefix = ''): string[] => {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    return prefix ? [prefix] : []
  }

  const keys: string[] = []
  const entries = Object.entries(value as Record<string, unknown>)
  for (const [key, child] of entries) {
    const nextPrefix = prefix ? `${prefix}.${key}` : key
    keys.push(...flattenKeys(child, nextPrefix))
  }
  return keys
}

const getValueByPath = (messages: LocaleMessages, path: string): unknown => {
  const segments = path.split('.')
  let current: unknown = messages
  for (const segment of segments) {
    if (current === null || typeof current !== 'object') {
      return undefined
    }
    current = (current as Record<string, unknown>)[segment]
  }
  return current
}

const fail = (message: string): never => {
  console.error(`[locale-keys.audit] ${message}`)
  process.exit(1)
}

const assert = (condition: boolean, message: string): void => {
  if (!condition) {
    fail(message)
  }
}

const keySets = new Map<string, Set<string>>()
for (const locale of locales) {
  const keys = flattenKeys(locale.messages)
  assert(keys.length > 0, `${locale.name} has no flattened locale keys`)
  keySets.set(locale.name, new Set(keys))

  for (const key of requiredKeys) {
    const value = getValueByPath(locale.messages, key)
    assert(typeof value === 'string' && value.trim().length > 0, `${locale.name} is missing ${key}`)
  }
}

const baseline = keySets.get('en')
assert(Boolean(baseline), 'en locale baseline is unavailable')

for (const locale of locales) {
  if (locale.name === 'en') {
    continue
  }

  const current = keySets.get(locale.name)
  assert(Boolean(current), `${locale.name} locale key set is unavailable`)

  const missing = [...baseline!].filter((key) => !current!.has(key))
  const extra = [...current!].filter((key) => !baseline!.has(key))

  assert(
    missing.length === 0 && extra.length === 0,
    `${locale.name} locale key mismatch. Missing: ${missing.join(', ') || '-'}; Extra: ${
      extra.join(', ') || '-'
    }`,
  )
}

console.log(
  `[locale-keys.audit] ok: ${locales.length} locales, ${baseline!.size} flattened keys, required keys present`,
)
